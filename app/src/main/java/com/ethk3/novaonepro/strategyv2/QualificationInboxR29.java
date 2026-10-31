package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R29-010/035/036 (docs/12 §3): contiguous qualification consumer. Settlement (LiveShadowsR27.settle) only records
 * outcomes; this class is the single owner of e-process updates. For each live experiment it walks the experiment's
 * registered opportunities (its r27_live_frozen rows) in target-ordinal order starting right after
 * last_ordinal, which therefore means "last contiguously consumed ordinal" and never "largest ordinal seen":
 *   - a non-ACT opportunity (no ACT decision, SKIPPED_GAP, DEADLINE_MISSED) scores u=0 by the pre-registered rule and
 *     is consumed without waiting for its draw;
 *   - an ACT opportunity whose result is not settled yet stops the walk (kept pending, never turned into non-ACT);
 *   - an ACT opportunity whose stored hit disagrees with the draw is a conflict: isolated, never overwritten.
 * Each step commits, in one transaction, the unique (experiment_id,target_ordinal) receipts, the new e-process state
 * and therefore the cursor, guarded by a compare-and-set on (last_ordinal,n): a replayed or concurrent step commits
 * nothing, so a period can never be counted twice (plain INSERT on the receipt key backs this up structurally).
 * Results arriving in the order 1,3,2 thus yield exactly the state of 1,2,3. */
public final class QualificationInboxR29 {
    private QualificationInboxR29() {}
    public static final int MAX_STEP = 256;
    public static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS r29_qual_receipt(experiment_id TEXT NOT NULL,target_ordinal INTEGER NOT NULL,target_issue TEXT NOT NULL,act INTEGER NOT NULL,hit INTEGER NOT NULL,u REAL NOT NULL,digest TEXT NOT NULL,state_after TEXT NOT NULL,consumed_at INTEGER NOT NULL,PRIMARY KEY(experiment_id,target_ordinal))",
        "CREATE TABLE IF NOT EXISTS r29_qual_conflict(experiment_id TEXT NOT NULL,target_ordinal INTEGER NOT NULL,kind TEXT NOT NULL,receipt_digest TEXT,observed_digest TEXT NOT NULL,detail TEXT NOT NULL,detected_at INTEGER NOT NULL,resolved_at INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(experiment_id,target_ordinal,observed_digest))",
        "CREATE TABLE IF NOT EXISTS r29_qual_rebuild(experiment_id TEXT NOT NULL,old_state_json TEXT NOT NULL,old_certificate TEXT,reason TEXT NOT NULL,rebuilt_at INTEGER NOT NULL,PRIMARY KEY(experiment_id,rebuilt_at))",
        "CREATE TABLE IF NOT EXISTS r29_qual_cursor(experiment_id TEXT PRIMARY KEY,blocked_ordinal INTEGER NOT NULL,blocked_reason TEXT NOT NULL,consumed_total INTEGER NOT NULL DEFAULT 0,cas_retries INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r29_pending_draw(target_ordinal INTEGER PRIMARY KEY,target_issue TEXT NOT NULL,rows INTEGER NOT NULL,act_rows INTEGER NOT NULL,first_seen_at INTEGER NOT NULL,last_checked_at INTEGER NOT NULL,checks INTEGER NOT NULL DEFAULT 0,resolved_at INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r29_qual_calendar(experiment_id TEXT PRIMARY KEY,first_ordinal INTEGER NOT NULL,registered_at INTEGER NOT NULL,origin TEXT NOT NULL)",
    };

    public static final String BLOCK_NONE = "NONE", BLOCK_ACT_PENDING_DRAW = "ACT_PENDING_DRAW", BLOCK_ACT_PENDING_SETTLE = "ACT_PENDING_SETTLE",
            BLOCK_CONFLICT = "CONFLICT", BLOCK_NOT_DUE = "NOT_DUE", BLOCK_MISSING_OPPORTUNITY = "MISSING_OPPORTUNITY";

    /** R30-B07-01: membership comes from durable FUTURE registrations, never the current H_SELECT rank.
     * Unknown nonterminal states stay protected; controls have no positive, eligible registration here.
     * Values are base-policy identities so exploration cannot spend another slot on the same policy. */
    public static Map<String,String> confirmationQueueR30(SQLiteDatabase db) {
        Map<String,String> pending = new LinkedHashMap<>();
        try (Cursor c = db.rawQuery("SELECT s.policy_hash,coalesce(nullif(s.base_policy,''),s.policy_hash) FROM r27_live_shadow s JOIN r27_eprocess e ON e.policy_hash=s.policy_hash WHERE e.global_index>0 AND s.status IN ('ACTIVE','OBSERVE','OBSERVE_RETIRED') AND e.state NOT IN (?,?) ORDER BY e.global_index,s.policy_hash",
                new String[]{SequentialQualificationR27.QUALIFIED,SequentialQualificationR27.NOT_BETTER})) {
            while (c.moveToNext()) pending.put(c.getString(0),c.getString(1));
        }
        return pending;
    }

    /** One registered opportunity of an experiment as stored (status/act/numbers frozen before the deadline). */
    public static final class Row {
        public final long ordinal; public final String issue, status; public final int act, n1, n2, n3, hit, actual;
        /** @param hit stored settlement (-2/-1 = not settled), @param actual drawn number at the frozen position, &lt;1 = no clean draw */
        public Row(long ordinal, String issue, String status, int act, int n1, int n2, int n3, int hit, int actual) {
            this.ordinal = ordinal; this.issue = issue; this.status = status; this.act = act; this.n1 = n1; this.n2 = n2; this.n3 = n3; this.hit = hit; this.actual = actual;
        }
        public boolean registeredAct() { return "FROZEN".equals(status) && act == 1; }
    }
    public static final class Consumed { public final Row row; public final boolean act, hit; public final double u; public final String digest, stateAfter;
        Consumed(Row row, boolean act, boolean hit, double u, String digest, String stateAfter) { this.row = row; this.act = act; this.hit = hit; this.u = u; this.digest = digest; this.stateAfter = stateAfter; } }
    public static final class Plan {
        public final List<Consumed> consumed = new ArrayList<>(); public String block = BLOCK_NONE; public long blockedOrdinal = -1; public String conflictDetail;
        public String conflictDigest;
    }

    /** Scoring-relevant facts only: non-ACT periods score 0 whatever the draw, so a late draw never changes their digest. */
    public static String digest(String experiment, Row r) {
        boolean act = r.registeredAct();
        return StatsR27.sha256("R29_QUAL|" + experiment + "|" + r.ordinal + "|" + r.issue + "|" + (act ? "ACT|" + r.n1 + "," + r.n2 + "," + r.n3 + "|" + r.actual + "|" + r.hit : "NO_ACT"));
    }

    /** Pure decision: applies rows (ascending, all &gt; s.lastOrdinal) to s in place while they are resolvable. */
    public static Plan plan(SequentialQualificationR27.State s, List<Row> rows, long latestOrdinal, int max) {
        return plan(s, rows, latestOrdinal, max, -1);
    }
    static Plan plan(SequentialQualificationR27.State s, List<Row> rows, long latestOrdinal, int max, long firstOrdinal) {
        Plan p = new Plan();
        long expected = s.lastOrdinal >= 0 ? s.lastOrdinal + 1 : firstOrdinal;
        for (Row r : rows) {
            if (p.consumed.size() >= max) break;
            if (r.ordinal <= s.lastOrdinal) continue; // already consumed (defensive; the query starts after the cursor)
            if (expected >= 0 && expected <= latestOrdinal && r.ordinal != expected) {
                p.block = BLOCK_MISSING_OPPORTUNITY; p.blockedOrdinal = expected; break;
            }
            if (r.ordinal > latestOrdinal) { p.block = BLOCK_NOT_DUE; p.blockedOrdinal = r.ordinal; break; }
            boolean act = r.registeredAct(), hit = false;
            if (act) {
                if (r.hit < 0) { p.block = r.actual < 1 ? BLOCK_ACT_PENDING_DRAW : BLOCK_ACT_PENDING_SETTLE; p.blockedOrdinal = r.ordinal; break; }
                boolean recomputed = r.actual == r.n1 || r.actual == r.n2 || r.actual == r.n3;
                if (r.actual < 1 || r.hit > 1 || recomputed != (r.hit == 1)) {
                    p.block = BLOCK_CONFLICT; p.blockedOrdinal = r.ordinal; p.conflictDigest = digest(s.policyHash, r);
                    p.conflictDetail = "stored hit=" + r.hit + " draw=" + r.actual + " frozen=" + r.n1 + "," + r.n2 + "," + r.n3;
                    break;
                }
                hit = r.hit == 1;
            }
            double u = act ? (hit ? 1 : 0) - StatsR27.BASE : 0;
            String after = s.observe(r.ordinal, act, hit);
            p.consumed.add(new Consumed(r, act, hit, u, digest(s.policyHash, r), after));
            expected = r.ordinal + 1;
        }
        if (BLOCK_NONE.equals(p.block) && p.consumed.size() < max && expected >= 0 && expected <= latestOrdinal) {
            p.block = BLOCK_MISSING_OPPORTUNITY; p.blockedOrdinal = expected;
        }
        return p;
    }

    /** Transactional sink (Android: ResearchWriteQueueR27 via the single writer; tests: a JDBC transaction). */
    public interface Commit { boolean commit(String operation, ResearchWriteQueueR27.Batch batch) throws Exception; }

    static final String ROWS = "SELECT f.target_ordinal,f.target_issue,f.status,f.act,f.n1,f.n2,f.n3,f.hit,"
            + "(SELECT CASE f.position WHEN 1 THEN d.n1 WHEN 2 THEN d.n2 WHEN 3 THEN d.n3 WHEN 4 THEN d.n4 WHEN 5 THEN d.n5 WHEN 6 THEN d.n6 WHEN 7 THEN d.n7 WHEN 8 THEN d.n8 WHEN 9 THEN d.n9 WHEN 10 THEN d.n10 END FROM draws d WHERE d.issue=f.target_issue AND d.dirty=0 LIMIT 1)"
            + " FROM r27_live_frozen f WHERE f.policy_hash=? AND f.target_ordinal>? ORDER BY f.target_ordinal LIMIT ";

    public static List<Row> rows(SQLiteDatabase ro, String experiment, long after, int limit) {
        List<Row> out = new ArrayList<>();
        try (Cursor c = ro.rawQuery(ROWS + limit, new String[] {experiment, String.valueOf(after)})) {
            while (c.moveToNext()) out.add(new Row(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3), c.getInt(4), c.getInt(5), c.getInt(6), c.getInt(7), c.isNull(8) ? -1 : c.getInt(8)));
        }
        return out;
    }

    /** Result of one consumer pass (report/diagnostics). */
    public static final class Pass { public long consumed, casRetries, conflicts, rebuilt; public final Map<String, String> blocked = new LinkedHashMap<>(); }

    /** Consumes every live experiment as far as its contiguous resolvable prefix allows. */
    public static Pass consume(SQLiteDatabase ro, Commit sink, String statuses, long latestOrdinal) throws Exception {
        Pass pass = new Pass();
        List<String> experiments = new ArrayList<>();
        try (Cursor c = ro.rawQuery("SELECT s.policy_hash FROM r27_live_shadow s JOIN r27_eprocess e ON e.policy_hash=s.policy_hash WHERE s.status IN " + statuses + " ORDER BY s.global_index", null)) {
            while (c.moveToNext()) experiments.add(c.getString(0));
        }
        for (String id : experiments) {
            if (hasOpenConflict(ro, id)) { pass.blocked.put(id, BLOCK_CONFLICT); continue; }
            long firstOrdinal = calendarStart(ro, id);
            if (firstOrdinal < 0) {
                // An old installation has no registration boundary. Preserve that uncertainty explicitly;
                // the first original freeze is a conservative lower bound, never a fabricated prediction.
                try (Cursor c = ro.rawQuery("SELECT min(target_ordinal) FROM r27_live_frozen WHERE policy_hash=?", new String[] {id})) {
                    if (c.moveToFirst() && !c.isNull(0)) firstOrdinal = c.getLong(0);
                }
                if (firstOrdinal < 0) { pass.blocked.put(id, "REGISTRATION_BOUNDARY_UNKNOWN"); continue; }
                ResearchWriteQueueR27.Batch cal = new ResearchWriteQueueR27.Batch().add(
                        "INSERT OR IGNORE INTO r29_qual_calendar VALUES(?,?,?,?)", id, firstOrdinal, System.currentTimeMillis(), "LEGACY_FIRST_FREEZE_LOWER_BOUND");
                if (!sink.commit("R29_QUAL_CALENDAR", cal)) { pass.casRetries++; continue; }
            }
            if (needsRebuild(ro, id)) { if (rebuild(ro, sink, id)) pass.rebuilt++; else { pass.casRetries++; continue; } }
            for (int guard = 0; guard < 64; guard++) {
                SequentialQualificationR27.State s = StoreR27.readEprocess(ro, id); if (s == null) break;
                final long expectLast = s.lastOrdinal, expectN = s.n;
                Plan p = plan(s, rows(ro, id, s.lastOrdinal, MAX_STEP + 1), latestOrdinal, MAX_STEP, firstOrdinal);
                if (p.consumed.isEmpty() && !BLOCK_CONFLICT.equals(p.block) && sameCursor(ro, id, p)) { pass.blocked.put(id, p.block); break; }
                ResearchWriteQueueR27.Batch b = step(id, s, p, expectLast, expectN);
                boolean ok;
                try { ok = sink.commit("R29_QUAL_CONSUME", b); }
                catch (IllegalStateException cas) { if (!precondition(cas)) throw cas; ok = false; }
                if (!ok) { pass.casRetries++; break; }
                pass.consumed += p.consumed.size();
                if (BLOCK_CONFLICT.equals(p.block)) pass.conflicts++;
                if (!BLOCK_NONE.equals(p.block) || p.consumed.size() < MAX_STEP) { pass.blocked.put(id, p.block); break; }
            }
        }
        return pass;
    }

    static long calendarStart(SQLiteDatabase db, String id) {
        try (Cursor c = db.rawQuery("SELECT first_ordinal FROM r29_qual_calendar WHERE experiment_id=?", new String[] {id})) {
            return c.moveToFirst() ? c.getLong(0) : -1;
        }
    }
    static boolean blockedForCertificate(SQLiteDatabase db, String id) {
        if (hasOpenConflict(db, id)) return true;
        try (Cursor c = db.rawQuery("SELECT blocked_reason FROM r29_qual_cursor WHERE experiment_id=?", new String[] {id})) {
            if (!c.moveToFirst()) return true;
            String reason = c.getString(0);
            return !BLOCK_NONE.equals(reason) && !BLOCK_NOT_DUE.equals(reason);
        }
    }

    /** Builds the single transaction for one plan. */
    static ResearchWriteQueueR27.Batch step(final String id, final SequentialQualificationR27.State s, final Plan p, final long expectLast, final long expectN) {
        final long now = System.currentTimeMillis();
        if (p.consumed.isEmpty() && !BLOCK_CONFLICT.equals(p.block)) {
            // Only refresh the visible block reason; no state change.
            ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
            b.add("INSERT OR IGNORE INTO r29_qual_cursor(experiment_id,blocked_ordinal,blocked_reason,updated_at) VALUES(?,?,?,?)", id, p.blockedOrdinal, p.block, now);
            b.add("UPDATE r29_qual_cursor SET blocked_ordinal=?,blocked_reason=?,updated_at=? WHERE experiment_id=? AND (blocked_ordinal<>? OR blocked_reason<>?)", p.blockedOrdinal, p.block, now, id, p.blockedOrdinal, p.block);
            return b;
        }
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
        b.before = db -> {
            try (Cursor c = db.rawQuery("SELECT last_ordinal,n FROM r27_eprocess WHERE policy_hash=?", new String[] {id})) {
                if (!c.moveToFirst() || c.getLong(0) != expectLast || c.getLong(1) != expectN) return -1; // another step won: commit nothing
            }
            for (Consumed x : p.consumed)
                db.execSQL("INSERT INTO r29_qual_receipt(experiment_id,target_ordinal,target_issue,act,hit,u,digest,state_after,consumed_at) VALUES(?,?,?,?,?,?,?,?,?)",
                        new Object[] {id, x.row.ordinal, x.row.issue, x.act ? 1 : 0, x.act ? (x.hit ? 1 : 0) : -1, x.u, x.digest, x.stateAfter, now});
            db.execSQL("UPDATE r27_eprocess SET log_e_up=?,log_e_down=?,n=?,acts=?,hits=?,sum_u=?,sum_u2=?,last_ordinal=?,state=?,updated_at=? WHERE policy_hash=?",
                    new Object[] {s.logUp, s.logDown, s.n, s.acts, s.hits, s.sumU, s.sumU2, s.lastOrdinal, s.state, now, id});
            if (SequentialQualificationR27.QUALIFIED.equals(s.state)) db.execSQL(StoreR27.R26_EVOLUTION_QUALIFIED, new Object[] {id});
            if (SequentialQualificationR27.NOT_BETTER.equals(s.state))
                db.execSQL("UPDATE r27_live_shadow SET status='RETIRED_TO_ARCHIVE',reason='SEQUENTIAL_TEST_NOT_BETTER_THAN_TARGET',updated_at=? WHERE policy_hash=? AND status='ACTIVE'", new Object[] {now, id});
            if (BLOCK_CONFLICT.equals(p.block))
                db.execSQL("INSERT OR IGNORE INTO r29_qual_conflict(experiment_id,target_ordinal,kind,receipt_digest,observed_digest,detail,detected_at) VALUES(?,?,'RESULT_CONFLICT',NULL,?,?,?)",
                        new Object[] {id, p.blockedOrdinal, p.conflictDigest, p.conflictDetail, now});
            db.execSQL("INSERT OR IGNORE INTO r29_qual_cursor(experiment_id,blocked_ordinal,blocked_reason,updated_at) VALUES(?,?,?,?)", new Object[] {id, p.blockedOrdinal, p.block, now});
            db.execSQL("UPDATE r29_qual_cursor SET blocked_ordinal=?,blocked_reason=?,consumed_total=consumed_total+?,updated_at=? WHERE experiment_id=?", new Object[] {p.blockedOrdinal, p.block, p.consumed.size(), now, id});
            return 0;
        };
        return b;
    }

    static boolean sameCursor(SQLiteDatabase ro, String id, Plan p) {
        try (Cursor c = ro.rawQuery("SELECT blocked_ordinal,blocked_reason FROM r29_qual_cursor WHERE experiment_id=?", new String[] {id})) {
            return c.moveToFirst() && c.getLong(0) == p.blockedOrdinal && p.block.equals(c.getString(1));
        }
    }

    static boolean hasOpenConflict(SQLiteDatabase ro, String id) {
        try (Cursor c = ro.rawQuery("SELECT 1 FROM r29_qual_conflict WHERE experiment_id=? AND resolved_at=0 LIMIT 1", new String[] {id})) { return c.moveToFirst(); }
    }

    /** An experiment fed by the R27 settle loop (state advanced, no receipt) may have skipped late or out-of-order periods:
     * its e-process is rebuilt from the frozen rows in order. The old state and certificate are kept in r29_qual_rebuild;
     * nothing about history is edited silently, and the certificate gate re-evaluates from the rebuilt state. */
    static boolean needsRebuild(SQLiteDatabase ro, String id) {
        long n = 0, receipts = 0;
        try (Cursor c = ro.rawQuery("SELECT n FROM r27_eprocess WHERE policy_hash=?", new String[] {id})) { if (c.moveToFirst()) n = c.getLong(0); }
        if (n == 0) return false;
        try (Cursor c = ro.rawQuery("SELECT count(*),min(target_ordinal),max(target_ordinal) FROM r29_qual_receipt WHERE experiment_id=?", new String[] {id})) {
            if (c.moveToFirst()) {
                receipts = c.getLong(0);
                long start = calendarStart(ro, id);
                if (receipts > 0 && (c.getLong(2) - c.getLong(1) + 1 != receipts || (start >= 0 && c.getLong(1) != start))) return true;
            }
        }
        return receipts != n;
    }

    static boolean rebuild(SQLiteDatabase ro, Commit sink, final String id) throws Exception {
        final SequentialQualificationR27.State old = StoreR27.readEprocess(ro, id); if (old == null) return false;
        String cert = null;
        try (Cursor c = ro.rawQuery("SELECT status||'|'||evidence_digest FROM r27_certificate WHERE policy_hash=?", new String[] {id})) { if (c.moveToFirst()) cert = c.getString(0); }
        final String oldJson = JsonR27.write(old.json()), oldCert = cert; final long expectLast = old.lastOrdinal, expectN = old.n; final long now = System.currentTimeMillis();
        final String keepState = SequentialQualificationR27.QUALIFIED.equals(old.state) || SequentialQualificationR27.NOT_BETTER.equals(old.state) ? SequentialQualificationR27.ACCUMULATING : old.state;
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
        b.before = db -> {
            try (Cursor c = db.rawQuery("SELECT last_ordinal,n FROM r27_eprocess WHERE policy_hash=?", new String[] {id})) {
                if (!c.moveToFirst() || c.getLong(0) != expectLast || c.getLong(1) != expectN) return -1;
            }
            db.execSQL("INSERT INTO r29_qual_rebuild(experiment_id,old_state_json,old_certificate,reason,rebuilt_at) VALUES(?,?,?,?,?)",
                    new Object[] {id, oldJson, oldCert, "R29_CONTIGUOUS_REPLAY: e-process advanced by the R27 settle loop without receipts (late hit=-2 / out-of-order skips possible)", now});
            db.execSQL("DELETE FROM r29_qual_receipt WHERE experiment_id=?", new Object[] {id});
            db.execSQL("UPDATE r27_eprocess SET log_e_up=0,log_e_down=0,n=0,acts=0,hits=0,sum_u=0,sum_u2=0,last_ordinal=-1,state=?,updated_at=? WHERE policy_hash=?", new Object[] {keepState, now, id});
            // A certificate issued on the old state is revoked until the rebuilt state earns it again.
            db.execSQL("UPDATE r27_certificate SET status='REVOKED_REBUILD' WHERE policy_hash=? AND status='ACTIVE'", new Object[] {id});
            return 0;
        };
        try { return sink.commit("R29_QUAL_REBUILD", b); }
        catch (IllegalStateException cas) { if (!precondition(cas)) throw cas; return false; }
    }

    static boolean precondition(Throwable t) {
        for (Throwable x = t; x != null; x = x.getCause()) { if (String.valueOf(x.getMessage()).contains("R27_WRITE_PRECONDITION")) return true; if (x.getCause() == x) break; }
        return false;
    }

    // ---------------------------------------------------------------- pending draws (separate page from settlement)
    /** Records frozen periods whose draw is still missing (PENDING_DRAW with retry bookkeeping) and resolves those whose
     * draw has arrived. Pending rows stay hit=-1 and remain settleable whenever the backfill brings the draw. */
    public static ResearchWriteQueueR27.Batch pendingDraws(SQLiteDatabase ro, long latestOrdinal) {
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis();
        Map<Long, long[]> known = new LinkedHashMap<>(); // ordinal -> {last_checked_at, resolved_at, checks, first_seen_at}
        try (Cursor c = ro.rawQuery("SELECT target_ordinal,last_checked_at,resolved_at,checks,first_seen_at FROM r29_pending_draw WHERE resolved_at=0 OR target_ordinal>=?", new String[] {String.valueOf(latestOrdinal - 2880)})) {
            while (c.moveToNext()) known.put(c.getLong(0), new long[] {c.getLong(1), c.getLong(2), c.getLong(3), c.getLong(4)});
        }
        boolean open = false;
        try (Cursor c = ro.rawQuery("SELECT f.target_ordinal,f.target_issue,count(*),sum(CASE WHEN f.status='FROZEN' AND f.act=1 THEN 1 ELSE 0 END) FROM r27_live_frozen f"
                + " WHERE f.hit IN(-1,-2) AND f.target_ordinal<? AND NOT EXISTS(SELECT 1 FROM draws d WHERE d.issue=f.target_issue AND d.dirty=0)"
                + " GROUP BY f.target_ordinal,f.target_issue ORDER BY f.target_ordinal DESC LIMIT 128", new String[] {String.valueOf(latestOrdinal)})) {
            while (c.moveToNext()) {
                long ord = c.getLong(0); long[] k = known.get(ord);
                // R30-B02-03: recheck backoff 1,2,4,...,300 s from the existing checks count (next_check_at =
                // last_checked_at + nextCheckDelayMs(checks)); a STALE row (>2880 checks or >48 h open) is no longer
                // rechecked but is kept, stays settleable and is listed in the report. No row is deleted.
                if (k != null && k[1] == 0 && (stale(k[2], k[3], now) || now - k[0] < nextCheckDelayMs(k[2]))) continue;
                b.add("INSERT OR IGNORE INTO r29_pending_draw(target_ordinal,target_issue,rows,act_rows,first_seen_at,last_checked_at) VALUES(?,?,?,?,?,?)", ord, c.getString(1), c.getLong(2), c.getLong(3), now, now);
                b.add("UPDATE r29_pending_draw SET rows=?,act_rows=?,last_checked_at=?,checks=checks+1,resolved_at=0 WHERE target_ordinal=?", c.getLong(2), c.getLong(3), now, ord);
            }
        }
        for (long[] k : known.values()) if (k[1] == 0) open = true;
        if (open) b.add("UPDATE r29_pending_draw SET resolved_at=? WHERE resolved_at=0 AND EXISTS(SELECT 1 FROM draws d WHERE d.issue=r29_pending_draw.target_issue AND d.dirty=0)", now);
        return b;
    }
    public static final long PENDING_RECHECK_MS = 30_000L;
    /** R30-B02-03 recheck backoff: 1 s doubling per check, capped at 300 s. */
    public static final long PENDING_BACKOFF_CAP_MS = 300_000L, PENDING_STALE_CHECKS = 2880, PENDING_STALE_AGE_MS = 48L * 3600_000L;
    public static long nextCheckDelayMs(long checks) { return checks <= 0 ? 1000L : Math.min(PENDING_BACKOFF_CAP_MS, 1000L << Math.min(checks - 1, 9)); }
    public static boolean stale(long checks, long firstSeenAt, long now) { return checks > PENDING_STALE_CHECKS || (firstSeenAt > 0 && now - firstSeenAt > PENDING_STALE_AGE_MS); }

    // ---------------------------------------------------------------- report (R29-039)
    /** Read-only diagnosis for ReportR29: null = table not migrated yet (never shown as 0). */
    public static Map<String, Object> diagnostics(SQLiteDatabase db) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pendingDrawOpen", count(db, "SELECT count(*) FROM r29_pending_draw WHERE resolved_at=0"));
        m.put("pendingDrawOpenActRows", count(db, "SELECT coalesce(sum(act_rows),0) FROM r29_pending_draw WHERE resolved_at=0"));
        m.put("pendingDrawOldestOrdinal", count(db, "SELECT min(target_ordinal) FROM r29_pending_draw WHERE resolved_at=0"));
        m.put("pendingDrawResolved", count(db, "SELECT count(*) FROM r29_pending_draw WHERE resolved_at>0"));
        m.put("pendingDrawStale", count(db, "SELECT count(*) FROM r29_pending_draw WHERE resolved_at=0 AND (checks>" + PENDING_STALE_CHECKS + " OR first_seen_at<" + (System.currentTimeMillis() - PENDING_STALE_AGE_MS) + ")"));
        m.put("pendingDrawStaleIssues", text(db, "SELECT group_concat(target_issue) FROM (SELECT target_issue FROM r29_pending_draw WHERE resolved_at=0 AND (checks>" + PENDING_STALE_CHECKS + " OR first_seen_at<" + (System.currentTimeMillis() - PENDING_STALE_AGE_MS) + ") ORDER BY target_ordinal LIMIT 50)"));
        m.put("legacyHitMinus2Remaining", count(db, "SELECT count(*) FROM r27_live_frozen WHERE hit=-2"));
        m.put("receipts", count(db, "SELECT count(*) FROM r29_qual_receipt"));
        m.put("receiptActs", count(db, "SELECT count(*) FROM r29_qual_receipt WHERE act=1"));
        m.put("conflictsOpen", count(db, "SELECT count(*) FROM r29_qual_conflict WHERE resolved_at=0"));
        m.put("rebuilds", count(db, "SELECT count(*) FROM r29_qual_rebuild"));
        m.put("registrationBoundariesInferred", count(db, "SELECT count(*) FROM r29_qual_calendar WHERE origin='LEGACY_FIRST_FREEZE_LOWER_BOUND'"));
        m.put("missingOpportunityExperiments", count(db, "SELECT count(*) FROM r29_qual_cursor WHERE blocked_reason='MISSING_OPPORTUNITY'"));
        m.put("receiptStateMismatch", count(db, "SELECT count(*) FROM r27_eprocess e WHERE EXISTS(SELECT 1 FROM r29_qual_receipt r WHERE r.experiment_id=e.policy_hash) AND e.n<>(SELECT count(*) FROM r29_qual_receipt r WHERE r.experiment_id=e.policy_hash)"));
        Map<String, Object> blocked = new LinkedHashMap<>();
        try (Cursor c = db.rawQuery("SELECT blocked_reason,count(*),min(CASE WHEN blocked_ordinal>=0 THEN blocked_ordinal END) FROM r29_qual_cursor GROUP BY blocked_reason ORDER BY blocked_reason", null)) {
            while (c.moveToNext()) { Map<String, Object> x = new LinkedHashMap<>(); x.put("experiments", c.getLong(1)); x.put("oldestOrdinal", c.isNull(2) ? null : (Object) c.getLong(2)); blocked.put(c.getString(0), x); }
            m.put("cursorByBlockReason", blocked);
        } catch (RuntimeException schemaPending) { m.put("cursorByBlockReason", null); }
        m.put("gateV2Evaluated", count(db, "SELECT count(*) FROM r29_gate_v2"));
        m.put("gateV2WouldIssue", count(db, "SELECT count(*) FROM r29_gate_v2 WHERE would_issue=1"));
        try (Cursor c = db.rawQuery("SELECT policy_hash,failing,evaluated_through FROM r29_gate_v2 ORDER BY would_issue DESC,length(failing),policy_hash LIMIT 5", null)) {
            List<Object> near = new ArrayList<>(); while (c.moveToNext()) { Map<String, Object> x = new LinkedHashMap<>(); x.put("policy", c.getString(0)); x.put("failing", c.getString(1)); x.put("through", c.getLong(2)); near.add(x); }
            m.put("gateV2Nearest", near);
        } catch (RuntimeException schemaPending) { m.put("gateV2Nearest", null); }
        m.put("rule", "e-process advances only over the contiguous resolvable prefix; ACT with missing draw stays pending; conflicts are isolated");
        return m;
    }
    static String text(SQLiteDatabase db, String sql) {
        try (Cursor c = db.rawQuery(sql, null)) { return c.moveToFirst() && !c.isNull(0) ? c.getString(0) : null; }
        catch (RuntimeException schemaPending) { return null; }
    }
    static Object count(SQLiteDatabase db, String sql) {
        try (Cursor c = db.rawQuery(sql, null)) { return c.moveToFirst() && !c.isNull(0) ? (Object) c.getLong(0) : null; }
        catch (RuntimeException schemaPending) { return null; }
    }
}
