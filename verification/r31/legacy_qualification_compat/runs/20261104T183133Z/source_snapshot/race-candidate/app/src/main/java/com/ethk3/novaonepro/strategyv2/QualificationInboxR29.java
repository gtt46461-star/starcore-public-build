package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.TargetIssueResolver;
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
    // MAX_STEP remains the logical window. Physical commits reserve space for
    // the existing fence, source/CAS reads, typed progress and durable receipt.
    static final int RECEIPTS_PER_COMMIT_R31=16, PENDING_PER_COMMIT_R31=8;
    static final int PROTOCOL_SQL_RESERVE_R31=32;
    static final long PROTOCOL_BIND_RESERVE_R31=8192;
    static void requireBudgetR31(ResearchWriteQueueR27.Batch batch,int guardSql) {
        requireBudgetR31(batch,guardSql,0);
    }
    static long bindBytesR31(Object... args) {
        long bytes=0;for(Object value:args)bytes+=value instanceof byte[]?((byte[])value).length:value instanceof String?((String)value).getBytes(java.nio.charset.StandardCharsets.UTF_8).length:8;
        return bytes;
    }
    static void requireBudgetR31(ResearchWriteQueueR27.Batch batch,int guardSql,long guardBindBytes) {
        long bytes=PROTOCOL_BIND_RESERVE_R31+guardBindBytes;
        for(ResearchWriteQueueR27.Op op:batch.ops)if(op.args!=null)for(Object value:op.args)
            bytes+=bindBytesR31(value);
        if(batch.ops.size()+guardSql+PROTOCOL_SQL_RESERVE_R31>64
                ||bytes>Math.min(262144L,Runtime.getRuntime().maxMemory()/512L))
            throw dependencyR31("PREPARED_WRITE_BUDGET","RESOURCE_WAIT:R31_LIVE_PREPARED_WRITE_BUDGET");
    }
    private static final class PreparedDependencyR31 extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        private final String phase;
        PreparedDependencyR31(String phase,String detail){super(detail);this.phase=phase;}
        public String failureCategory(){return "DEPENDENCY";}
        public String operation(){return "R31_LIVE_QUALIFICATION";}
        public String phase(){return phase;}
        public String commitOutcomeName(){return "NOT_SUBMITTED";}
    }
    private static PreparedDependencyR31 dependencyR31(String phase,String detail) {
        return new PreparedDependencyR31(phase,detail);
    }
    static String preparedInputR31(ResearchWriteQueueR27.Batch batch) {
        List<Object> operations=new ArrayList<>();
        for(ResearchWriteQueueR27.Op op:batch.ops){List<Object> row=new ArrayList<>();row.add(op.sql);if(op.args!=null)java.util.Collections.addAll(row,op.args);operations.add(row);}
        return HashingSU1.text(JsonR27.write(operations));
    }
    static void requireSchemaR31(SQLiteDatabase db) {
        DatabaseCoordinator.requireSchemaObjectsR31(db,"R31_LIVE_QUALIFICATION",new String[][]{
                {"draws","issue,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10"},
                {"r27_live_shadow","policy_hash,status,global_index,reason,updated_at,through_ordinal"},
                {"r27_live_frozen","policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,hit,frozen_at,deadline_at,settled_at"},
                {"r27_opportunity","target_ordinal,settled"},
                {"r27_eprocess","policy_hash,global_index,alpha,target_rate,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,updated_at"},
                {"r29_qual_receipt","experiment_id,target_ordinal,target_issue,act,hit,u,digest,state_after,consumed_at"},
                {"r29_qual_cursor","experiment_id,blocked_ordinal,blocked_reason,consumed_total,cas_retries,updated_at"},
                {"r29_qual_calendar","experiment_id,first_ordinal,registered_at,origin"},
                {"r29_qual_conflict","experiment_id,target_ordinal,kind,receipt_digest,observed_digest,detail,detected_at,resolved_at"},
                {"r29_qual_rebuild","experiment_id,old_state_json,old_certificate,reason,rebuilt_at"},
                {"r27_certificate","policy_hash,status,gates_json,evidence_digest,issued_at,expires_at,gate_version"},
                {"r27_chart_cache","panel_id,key,payload_json,updated_at"},
                {"r26_evolution","attempt_id,policy_hash,prospective_qualified"},
                {"r29_pending_draw","target_ordinal,target_issue,rows,act_rows,first_seen_at,last_checked_at,checks,resolved_at"}},
                new String[][]{{"index","r27_live_frozen_order"}});
    }
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
        try (Cursor c = db.rawQuery("SELECT s.policy_hash,coalesce(nullif(s.base_policy,''),s.policy_hash) FROM r27_live_shadow s JOIN r27_eprocess e ON e.policy_hash=s.policy_hash WHERE e.global_index>0 AND s.status IN ('ACTIVE','ACTIVE_REPAIR1','OBSERVE','OBSERVE_RETIRED','OBSERVE_REPAIR1','OBSERVE_REPAIR1_RETIRED') AND e.state NOT IN (?,?) ORDER BY e.global_index,s.policy_hash",
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
    public static final class Pass { public long consumed, casRetries, conflicts, rebuilt; public final Map<String, String> blocked = new LinkedHashMap<>();
        public final Map<String,String> dependencies=new LinkedHashMap<>(); }

    /** Consumes every live experiment as far as its contiguous resolvable prefix allows. */
    public static Pass consume(SQLiteDatabase ro, Commit sink, String statuses, long latestOrdinal) throws Exception {
        requireSchemaR31(ro);
        Pass pass = new Pass();
        List<String> experiments = new ArrayList<>();
        try (Cursor c = ro.rawQuery("SELECT s.policy_hash FROM r27_live_shadow s JOIN r27_eprocess e ON e.policy_hash=s.policy_hash WHERE s.status IN " + statuses + " ORDER BY s.global_index", null)) {
            while (c.moveToNext()) experiments.add(c.getString(0));
        }
        for (String id : experiments) {
            CooperativeContinuationR26.poll();
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
                requireBudgetR31(cal,0);
                if (!sink.commit("R29_QUAL_CALENDAR", cal)) { pass.casRetries++; continue; }
            }
            if(needsRebuild(ro,id))try {
                if(rebuild(ro,sink,id))pass.rebuilt++;else{pass.casRetries++;continue;}
            }catch(LegacyCompatibilityPendingR31 pending){
                pass.blocked.put(id,pending.reason);pass.dependencies.put(id,pending.reason);
                if(pending.committedProgress)pass.rebuilt++;
                continue;
            }
            // Preserve the original 64 logical windows of MAX_STEP opportunities;
            // only the physical prefix transaction is smaller.
            int physicalPasses=64*((MAX_STEP+RECEIPTS_PER_COMMIT_R31-1)/RECEIPTS_PER_COMMIT_R31);
            for (int guard = 0; guard < physicalPasses; guard++) {
                CooperativeContinuationR26.poll();
                SequentialQualificationR27.State s = StoreR27.readEprocess(ro, id); if (s == null) break;
                final long expectLast = s.lastOrdinal, expectN = s.n;
                Plan p = plan(s, rows(ro, id, s.lastOrdinal, RECEIPTS_PER_COMMIT_R31 + 1), latestOrdinal, RECEIPTS_PER_COMMIT_R31, firstOrdinal);
                if (p.consumed.isEmpty() && !BLOCK_CONFLICT.equals(p.block) && sameCursor(ro, id, p)) { pass.blocked.put(id, p.block); break; }
                ResearchWriteQueueR27.Batch b = step(id, s, p, expectLast, expectN);
                appendReplayMarkerR31(ro,b,id,p,s.lastOrdinal);
                requireBudgetR31(b,2,bindBytesR31(id,id,String.valueOf(expectLast)));
                boolean ok;
                try { ok = sink.commit("R29_QUAL_CONSUME", b); }
                catch (IllegalStateException cas) { if (!precondition(cas)) throw cas; ok = false; }
                if (!ok) { pass.casRetries++; break; }
                pass.consumed += p.consumed.size();
                if (BLOCK_CONFLICT.equals(p.block)) pass.conflicts++;
                if (!BLOCK_NONE.equals(p.block) || p.consumed.size() < RECEIPTS_PER_COMMIT_R31) { pass.blocked.put(id, p.block); break; }
            }
            markQualifiedLinksR31(ro,sink,id);
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
        if(needsRebuild(db,id))return true;
        try (Cursor c = db.rawQuery("SELECT blocked_reason FROM r29_qual_cursor WHERE experiment_id=?", new String[] {id})) {
            if (!c.moveToFirst()) return true;
            String reason = c.getString(0);
            return !BLOCK_NONE.equals(reason) && !BLOCK_NOT_DUE.equals(reason);
        }
    }

    /** Builds the single transaction for one plan. */
    static ResearchWriteQueueR27.Batch step(final String id, final SequentialQualificationR27.State s, final Plan p, final long expectLast, final long expectN) {
        final long now = System.currentTimeMillis();
        final List<Consumed> prefix=java.util.Collections.unmodifiableList(new ArrayList<>(p.consumed));
        if(prefix.size()>RECEIPTS_PER_COMMIT_R31)throw new IllegalArgumentException("R31_QUALIFICATION_PHYSICAL_PREFIX_TOO_LARGE");
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
        b.guardBefore(db->{
            try(Cursor current=db.rawQuery("SELECT last_ordinal,n FROM r27_eprocess WHERE policy_hash=?",new String[]{id})) {
                if(!current.moveToFirst()||current.getLong(0)!=expectLast||current.getLong(1)!=expectN)return -1;
            }
            if(!prefix.isEmpty()) {
                List<Row> current=rows(db,id,expectLast,prefix.size());if(current.size()!=prefix.size())return -1;
                for(int i=0;i<prefix.size();i++) {
                    Row before=prefix.get(i).row,after=current.get(i);
                    if(before.ordinal!=after.ordinal||!before.issue.equals(after.issue)||before.registeredAct()!=after.registeredAct())return -1;
                    if(before.registeredAct()&&(before.n1!=after.n1||before.n2!=after.n2||before.n3!=after.n3||before.hit!=after.hit||before.actual!=after.actual))return -1;
                }
            }
            return 0;
        });
        if (p.consumed.isEmpty() && !BLOCK_CONFLICT.equals(p.block)) {
            // Only refresh the visible block reason; no state change.
            b.add("INSERT OR IGNORE INTO r29_qual_cursor(experiment_id,blocked_ordinal,blocked_reason,updated_at) VALUES(?,?,?,?)", id, p.blockedOrdinal, p.block, now);
            b.add("UPDATE r29_qual_cursor SET blocked_ordinal=?,blocked_reason=?,updated_at=? WHERE experiment_id=? AND (blocked_ordinal<>? OR blocked_reason<>?)", p.blockedOrdinal, p.block, now, id, p.blockedOrdinal, p.block);
            requireBudgetR31(b,1,bindBytesR31(id));return b;
        }
            for (Consumed x : prefix)
                b.add("INSERT INTO r29_qual_receipt(experiment_id,target_ordinal,target_issue,act,hit,u,digest,state_after,consumed_at) VALUES(?,?,?,?,?,?,?,?,?)",
                        id,x.row.ordinal,x.row.issue,x.act?1:0,x.act?(x.hit?1:0):-1,x.u,x.digest,x.stateAfter,now);
            b.add("UPDATE r27_eprocess SET log_e_up=?,log_e_down=?,n=?,acts=?,hits=?,sum_u=?,sum_u2=?,last_ordinal=?,state=?,updated_at=? WHERE policy_hash=? AND last_ordinal=? AND n=?",
                    s.logUp,s.logDown,s.n,s.acts,s.hits,s.sumU,s.sumU2,s.lastOrdinal,s.state,now,id,expectLast,expectN);
            if (SequentialQualificationR27.NOT_BETTER.equals(s.state))
                b.add("UPDATE r27_live_shadow SET status='RETIRED_TO_ARCHIVE',reason='SEQUENTIAL_TEST_NOT_BETTER_THAN_TARGET',updated_at=? WHERE policy_hash=? AND status IN ('ACTIVE','ACTIVE_REPAIR1')",now,id);
            if (BLOCK_CONFLICT.equals(p.block))
                b.add("INSERT OR IGNORE INTO r29_qual_conflict(experiment_id,target_ordinal,kind,receipt_digest,observed_digest,detail,detected_at) VALUES(?,?,'RESULT_CONFLICT',NULL,?,?,?)",id,p.blockedOrdinal,p.conflictDigest,p.conflictDetail,now);
            b.add("INSERT OR IGNORE INTO r29_qual_cursor(experiment_id,blocked_ordinal,blocked_reason,updated_at) VALUES(?,?,?,?)",id,p.blockedOrdinal,p.block,now);
            b.add("UPDATE r29_qual_cursor SET blocked_ordinal=?,blocked_reason=?,consumed_total=consumed_total+?,updated_at=? WHERE experiment_id=?",p.blockedOrdinal,p.block,prefix.size(),now,id);
        requireBudgetR31(b,2,bindBytesR31(id,id,String.valueOf(expectLast)));return b;
    }

    private static void appendReplayMarkerR31(SQLiteDatabase ro,ResearchWriteQueueR27.Batch b,String id,Plan p,long through) {
        if(p.consumed.isEmpty())return;
        String key=id+"|"+p.consumed.get(0).row.ordinal;
        String original=QualificationGapRepairR30R1.text(ro,"SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",QualificationGapRepairR30R1.PANEL,key);
        if(original==null)return;
        try {
            Map<String,Object> receipt=new LinkedHashMap<>(MetricEvaluatorR29.map(JsonR27.parse(original)));
            if(!"ORIGINAL_RESTORED_REPLAY_PENDING".equals(receipt.get("status")))return;
            long now=System.currentTimeMillis();receipt.put("status","ORIGINAL_RESTORED_CONTIGUOUS_PREFIX_ADVANCED");receipt.put("replayedThroughOrdinal",through);receipt.put("replayedAt",now);
            b.add("UPDATE r27_chart_cache SET payload_json=?,updated_at=? WHERE panel_id=? AND key=? AND payload_json=?",JsonR27.write(receipt),now,QualificationGapRepairR30R1.PANEL,key,original);
        } catch(RuntimeException invalid){QualificationGapRepairR30R1.observeFailure(invalid);}
    }

    private static void markQualifiedLinksR31(SQLiteDatabase ro,Commit sink,String id)throws Exception {
        for(;;) {
            CooperativeContinuationR26.poll();SequentialQualificationR27.State s=StoreR27.readEprocess(ro,id);
            if(s==null||!SequentialQualificationR27.QUALIFIED.equals(s.state))return;
            List<String> attempts=new ArrayList<>();
            try(Cursor c=ro.rawQuery("SELECT attempt_id FROM r26_evolution WHERE policy_hash=? AND attempt_id LIKE 'R27:%' AND coalesce(prospective_qualified,0)<>1 ORDER BY attempt_id LIMIT 16",new String[]{id})){while(c.moveToNext())attempts.add(c.getString(0));}
            if(attempts.isEmpty())return;
            final long expected=s.n,last=s.lastOrdinal;ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
            b.guardBefore(db->{try(Cursor c=db.rawQuery("SELECT n,last_ordinal,state FROM r27_eprocess WHERE policy_hash=?",new String[]{id})){return c.moveToFirst()&&c.getLong(0)==expected&&c.getLong(1)==last&&SequentialQualificationR27.QUALIFIED.equals(c.getString(2))?0:-1;}});
            for(String attempt:attempts)b.add("UPDATE r26_evolution SET prospective_qualified=1 WHERE attempt_id=? AND policy_hash=? AND coalesce(prospective_qualified,0)<>1",attempt,id);
            requireBudgetR31(b,1,bindBytesR31(id));
            try{if(!sink.commit("R29_QUALIFIED_LINK",b))return;}catch(IllegalStateException changed){if(!precondition(changed))throw changed;return;}
        }
    }

    static boolean sameCursor(SQLiteDatabase ro, String id, Plan p) {
        try (Cursor c = ro.rawQuery("SELECT blocked_ordinal,blocked_reason FROM r29_qual_cursor WHERE experiment_id=?", new String[] {id})) {
            return c.moveToFirst() && c.getLong(0) == p.blockedOrdinal && p.block.equals(c.getString(1));
        }
    }

    static boolean hasOpenConflict(SQLiteDatabase ro, String id) {
        try (Cursor c = ro.rawQuery("SELECT 1 FROM r29_qual_conflict WHERE experiment_id=? AND resolved_at=0 LIMIT 1", new String[] {id})) { return c.moveToFirst(); }
    }

    /** Legacy state is evidence, including when inconsistent. Never erase receipts or reset its exposure. */
    static boolean needsRebuild(SQLiteDatabase ro, String id) {
        long n,last;
        try(Cursor c=ro.rawQuery("SELECT n,last_ordinal FROM r27_eprocess WHERE policy_hash=?",new String[]{id})){if(!c.moveToFirst())return false;n=c.getLong(0);last=c.getLong(1);}
        Object[] receipt=legacyReceiptSummaryR31(ro,id);long count=((Number)receipt[0]).longValue();
        if(n!=count||n<0)return true;
        if(n==0)return last!=-1;
        long start=calendarStart(ro,id);
        if(receipt[1]==null||receipt[2]==null||((Number)receipt[2]).longValue()!=last
                ||last-((Number)receipt[1]).longValue()+1!=n||start<0||((Number)receipt[1]).longValue()!=start)return true;
        Object[] cursor=legacyCursorR31(ro,id);return cursor==null||((Number)cursor[2]).longValue()!=n||String.valueOf(cursor[1]).startsWith("LEGACY_");
    }

    private static final String LEGACY_VERSION_R31="R31_APPEND_ONLY_QUALIFICATION_COMPAT_V1";
    private static final String LEGACY_STATE_SQL_R31="SELECT global_index,alpha,target_rate,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,updated_at FROM r27_eprocess WHERE policy_hash=?";
    private static final class LegacyCompatibilityPendingR31 extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        final String reason;final boolean committedProgress;
        LegacyCompatibilityPendingR31(String reason,boolean progress){super("DEPENDENCY:"+LEGACY_VERSION_R31+":"+reason);this.reason=reason;committedProgress=progress;}
        public String failureCategory(){return "DEPENDENCY";}public String operation(){return "R29_QUAL_LEGACY_COMPAT";}
        public String phase(){return "LEGACY_RECEIPT_COMPATIBILITY";}public String commitOutcomeName(){return committedProgress?"COMMITTED":"NOT_SUBMITTED";}
    }
    private static Object[] legacyRecordR31(SQLiteDatabase db,String sql,String...args) {
        try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())return null;Object[] row=new Object[c.getColumnCount()];
            for(int i=0;i<row.length;i++)row[i]=c.isNull(i)?null:c.getType(i)==Cursor.FIELD_TYPE_INTEGER?Long.valueOf(c.getLong(i)):c.getType(i)==Cursor.FIELD_TYPE_FLOAT?Double.valueOf(c.getDouble(i)):c.getString(i);
            return row;
        }
    }
    private static Object[] legacyReceiptSummaryR31(SQLiteDatabase db,String id){return legacyRecordR31(db,"SELECT count(*),min(target_ordinal),max(target_ordinal) FROM r29_qual_receipt WHERE experiment_id=?",id);}
    private static Object[] legacyCursorR31(SQLiteDatabase db,String id){return legacyRecordR31(db,"SELECT blocked_ordinal,blocked_reason,consumed_total,cas_retries,updated_at FROM r29_qual_cursor WHERE experiment_id=?",id);}
    private static Object[] legacyCalendarR31(SQLiteDatabase db,String id){return legacyRecordR31(db,"SELECT first_ordinal,registered_at,origin FROM r29_qual_calendar WHERE experiment_id=?",id);}
    private static Object[] legacyCertificateR31(SQLiteDatabase db,String id){return legacyRecordR31(db,"SELECT status,evidence_digest,issued_at,expires_at,gate_version FROM r27_certificate WHERE policy_hash=?",id);}
    private static Object[] legacyAuditR31(SQLiteDatabase db,String id){return legacyRecordR31(db,"SELECT reason,rebuilt_at FROM r29_qual_rebuild WHERE experiment_id=? ORDER BY rebuilt_at DESC LIMIT 1",id);}
    private static boolean sameRecordR31(Object[] a,Object[] b){return java.util.Arrays.deepEquals(a,b);}
    private static final class LegacySourceR31 {
        final Row row;final int position;final byte[] probabilities;final long frozen,deadline,open;final Object[] receipt;
        LegacySourceR31(Cursor c){row=new Row(c.getLong(0),c.getString(1),c.getString(2),c.getInt(3),c.getInt(4),c.getInt(5),c.getInt(6),c.getInt(7),c.isNull(8)?-1:c.getInt(8));position=c.getInt(9);probabilities=c.getBlob(10);frozen=c.getLong(11);deadline=c.getLong(12);open=c.getLong(13);
            receipt=c.isNull(14)?null:new Object[]{c.getString(14),c.getLong(15),c.getLong(16),c.getDouble(17),c.getString(18),c.getString(19),c.getLong(20)};}
    }
    private static final String LEGACY_SOURCE_SQL_R31="SELECT f.target_ordinal,f.target_issue,f.status,f.act,f.n1,f.n2,f.n3,f.hit,"
            +"(SELECT CASE f.position WHEN 1 THEN d.n1 WHEN 2 THEN d.n2 WHEN 3 THEN d.n3 WHEN 4 THEN d.n4 WHEN 5 THEN d.n5 WHEN 6 THEN d.n6 WHEN 7 THEN d.n7 WHEN 8 THEN d.n8 WHEN 9 THEN d.n9 WHEN 10 THEN d.n10 END FROM draws d WHERE d.issue=f.target_issue AND d.dirty=0 LIMIT 1),"
            +"f.position,f.probabilities,f.frozen_at,f.deadline_at,coalesce((SELECT open_time FROM draws d WHERE d.issue=f.target_issue AND d.dirty=0 LIMIT 1),-1),r.target_issue,r.act,r.hit,r.u,r.digest,r.state_after,r.consumed_at"
            +" FROM r27_live_frozen f LEFT JOIN r29_qual_receipt r ON r.experiment_id=f.policy_hash AND r.target_ordinal=f.target_ordinal WHERE f.policy_hash=? AND f.target_ordinal>? AND f.target_ordinal<=? ORDER BY f.target_ordinal LIMIT "+RECEIPTS_PER_COMMIT_R31;
    private static List<LegacySourceR31> legacySourcesR31(SQLiteDatabase db,String id,long after,long through){List<LegacySourceR31> rows=new ArrayList<>();try(Cursor c=db.rawQuery(LEGACY_SOURCE_SQL_R31,new String[]{id,Long.toString(after),Long.toString(through)})){while(c.moveToNext())rows.add(new LegacySourceR31(c));}return rows;}
    private static boolean legalLegacySourceR31(LegacySourceR31 source) {
        Row r=source.row;
        if(!TargetIssueResolver.isValid(r.issue)||TargetIssueResolver.sequenceOrdinalSU1(r.issue)!=r.ordinal)return false;
        if(!r.registeredAct())return r.act==0&&("FROZEN".equals(r.status)||"SKIPPED_GAP".equals(r.status)||"DEADLINE_MISSED".equals(r.status)||"ABSTAIN_SET_SUPPORT".equals(r.status));
        if(source.position<1||source.position>10||r.n1<1||r.n1>10||r.n2<1||r.n2>10||r.n3<1||r.n3>10||r.n1==r.n2||r.n1==r.n3||r.n2==r.n3
                ||source.frozen<=0||source.deadline-source.frozen<LiveShadowsR27.FREEZE_MARGIN_MS||source.open<source.deadline||source.probabilities==null||source.probabilities.length!=80)return false;
        double sum=0;for(double p:StoreR27.unpack(source.probabilities)){if(!Double.isFinite(p)||p<0||p>1)return false;sum+=p;}return Math.abs(sum-1)<=1e-9;
    }
    private static final class LegacyProofR31 {String reason;final List<Consumed> missing=new ArrayList<>();final List<LegacySourceR31> missingSources=new ArrayList<>();long missingCount;}
    private static LegacyProofR31 legacyProofR31(SQLiteDatabase ro,String id,Object[] state,Object[] calendar,Object[] receipt,Object[] cursor) {
        LegacyProofR31 proof=new LegacyProofR31();
        for(int i=0;i<state.length;i++)if(i!=11&&!(state[i] instanceof Number)){proof.reason="LEGACY_STATE_NOT_NUMERIC";return proof;}
        long n=((Number)state[5]).longValue(),last=((Number)state[10]).longValue(),index=((Number)state[0]).longValue();double rate=((Number)state[2]).doubleValue();
        if(calendar==null||!"REGISTERED_BEFORE_FUTURE".equals(calendar[2])){proof.reason="LEGACY_REGISTRATION_BOUNDARY_UNPROVEN";return proof;}
        long first=((Number)calendar[0]).longValue();
        if(n<=0||first<0||last<first||last-first+1!=n){proof.reason="LEGACY_EXPOSURE_RANGE_NONCONTIGUOUS";return proof;}
        if(index<1||!Double.isFinite(rate)||rate<=0||rate>1){proof.reason="LEGACY_ALPHA_OR_RATE_UNPROVEN";return proof;}
        if(((Number)receipt[0]).longValue()>n||receipt[1]!=null&&(((Number)receipt[1]).longValue()<first||((Number)receipt[2]).longValue()>last)){proof.reason="LEGACY_RECEIPT_OUTSIDE_EXPOSURE";return proof;}
        if(cursor!=null&&(((Number)cursor[2]).longValue()<0||((Number)cursor[2]).longValue()>n)){proof.reason="LEGACY_CURSOR_EXCEEDS_EXPOSURE";return proof;}
        try{DatabaseCoordinator.requireSchemaObjectsR31(ro,"R31_LEGACY_QUALIFICATION_PROOF",new String[][]{{"draws","open_time"}},new String[0][]);}
        catch(DatabaseCoordinator.SchemaPendingR31 pending){proof.reason="LEGACY_ORIGINAL_TIMING_SCHEMA_PENDING";return proof;}
        SequentialQualificationR27.State replay=new SequentialQualificationR27.State(id,index,rate);
        long after=first-1;
        while(after<last) {
            CooperativeContinuationR26.poll();List<LegacySourceR31> rows=legacySourcesR31(ro,id,after,last);
            if(rows.isEmpty()){proof.reason="LEGACY_ORIGINAL_PREFIX_UNAVAILABLE";return proof;}
            for(LegacySourceR31 source:rows) {
                CooperativeContinuationR26.poll();Row row=source.row;
                if(row.ordinal!=after+1||!legalLegacySourceR31(source)||row.registeredAct()&&source.frozen<=((Number)calendar[1]).longValue()){proof.reason="LEGACY_ORIGINAL_PREFIX_OR_PIT_UNPROVEN";return proof;}
                boolean act=row.registeredAct(),hit=act&&row.hit==1;
                if(act&&(row.actual<1||row.hit<0||row.hit>1||(row.actual==row.n1||row.actual==row.n2||row.actual==row.n3)!=hit)){proof.reason="LEGACY_ACT_RESULT_UNRESOLVED_OR_CONFLICTING";return proof;}
                double u=act?(hit?1:0)-StatsR27.BASE:0;String stateAfter=replay.observe(row.ordinal,act,hit),digest=digest(id,row);
                if(source.receipt!=null) {
                    Object[] expected={row.issue,act?1L:0L,act?(hit?1L:0L):-1L,u,digest,stateAfter,source.receipt[6]};
                    if(!sameRecordR31(expected,source.receipt)){proof.reason="LEGACY_EXISTING_RECEIPT_CONFLICT";return proof;}
                }else {proof.missingCount++;if(proof.missing.size()<RECEIPTS_PER_COMMIT_R31){proof.missing.add(new Consumed(row,act,hit,u,digest,stateAfter));proof.missingSources.add(source);}}
                after=row.ordinal;
            }
        }
        Object[] recomputed={index,replay.alpha,rate,replay.logUp,replay.logDown,replay.n,replay.acts,replay.hits,replay.sumU,replay.sumU2,replay.lastOrdinal,replay.state,state[12]};
        if(!sameRecordR31(state,recomputed))proof.reason="LEGACY_EPROCESS_NOT_EQUAL_ORIGINAL_CONTIGUOUS_REPLAY";
        return proof;
    }

    /** Append evidence only after a complete numerical proof. Progress is the existing receipt keyset,
     * with an immutable versioned audit in the existing ledger; no cache is an authority or new schema. */
    static boolean rebuild(SQLiteDatabase ro,Commit sink,final String id)throws Exception {
        CooperativeContinuationR26.poll();if(!needsRebuild(ro,id))return true;
        final Object[] state=legacyRecordR31(ro,LEGACY_STATE_SQL_R31,id);if(state==null)return false;
        final Object[] calendar=legacyCalendarR31(ro,id),receipt=legacyReceiptSummaryR31(ro,id),cursor=legacyCursorR31(ro,id),certificate=legacyCertificateR31(ro,id),audit=legacyAuditR31(ro,id);
        final long revision=SchemaR27.liveSourceRevision(ro),now=System.currentTimeMillis();
        LegacyProofR31 proof=legacyProofR31(ro,id,state,calendar,receipt,cursor);
        if(SchemaR27.liveSourceRevision(ro)!=revision)throw new LegacyCompatibilityPendingR31("LEGACY_SOURCE_CHANGED_DURING_PROOF",false);
        final boolean verified=proof.reason==null,complete=verified&&proof.missingCount==proof.missing.size();
        final String reason=verified?(complete?BLOCK_NONE:"LEGACY_RECEIPT_EVIDENCE_RESTORING"):proof.reason;
        final String auditKey=LEGACY_VERSION_R31+"|"+HashingSU1.text(JsonR27.write(java.util.Arrays.asList(java.util.Arrays.asList(state),calendar==null?null:java.util.Arrays.asList(calendar))));
        final boolean newAudit=audit==null||!auditKey.equals(audit[0]);
        long ordinal=verified&&!proof.missing.isEmpty()?proof.missing.get(0).row.ordinal:state[10] instanceof Number?((Number)state[10]).longValue():-1;
        if(!verified&&!newAudit&&cursor!=null&&reason.equals(cursor[1])&&(certificate==null||!"ACTIVE".equals(certificate[0])))throw new LegacyCompatibilityPendingR31(reason,false);
        Map<String,Object> original=new LinkedHashMap<>();original.put("version",LEGACY_VERSION_R31);original.put("eprocessColumns","global_index,alpha,target_rate,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,updated_at");original.put("originalEprocess",java.util.Arrays.asList(state));original.put("originalCalendar",calendar==null?null:java.util.Arrays.asList(calendar));original.put("originalCursor",cursor==null?null:java.util.Arrays.asList(cursor));original.put("originalReceiptSummary",java.util.Arrays.asList(receipt));original.put("sourceRevisionAtProof",revision);original.put("proofVerdict",verified?"EXACT_ORIGINAL_PREFIX_NUMERICAL_EQUALITY":reason);original.put("action","PRESERVE_ALL_EXISTING_EXPOSURE_AND_RECEIPTS_APPEND_ONLY");
        String certificateJson=null;if(certificate!=null){Map<String,Object> cert=new LinkedHashMap<>();cert.put("originalMetadata",java.util.Arrays.asList(certificate));String gates=QualificationGapRepairR30R1.text(ro,"SELECT gates_json FROM r27_certificate WHERE policy_hash=?",id);cert.put("originalGatesSha256",gates==null?null:HashingSU1.text(gates));cert.put("gatesBytes","PRESERVED_IN_ORIGINAL_CERTIFICATE_ROW");certificateJson=JsonR27.write(cert);}
        final List<Consumed> missing=java.util.Collections.unmodifiableList(new ArrayList<>(proof.missing));
        final List<LegacySourceR31> sources=java.util.Collections.unmodifiableList(new ArrayList<>(proof.missingSources));
        StringBuilder slots=new StringBuilder();List<String> guardArguments=new ArrayList<>();guardArguments.add(id);
        for(Consumed input:missing){if(slots.length()>0)slots.append(',');slots.append('?');guardArguments.add(Long.toString(input.row.ordinal));}
        final String guardSql=LEGACY_SOURCE_SQL_R31.substring(0,LEGACY_SOURCE_SQL_R31.lastIndexOf(" WHERE f.policy_hash=?"))+" WHERE f.policy_hash=? AND f.target_ordinal IN ("+slots+") ORDER BY f.target_ordinal";
        final String[] guardArgs=guardArguments.toArray(new String[0]);ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
        b.guardBefore(db->{
            if(!sameRecordR31(state,legacyRecordR31(db,LEGACY_STATE_SQL_R31,id))||!sameRecordR31(calendar,legacyCalendarR31(db,id))||!sameRecordR31(receipt,legacyReceiptSummaryR31(db,id))
                    ||!sameRecordR31(cursor,legacyCursorR31(db,id))||!sameRecordR31(certificate,legacyCertificateR31(db,id))||!sameRecordR31(audit,legacyAuditR31(db,id))||SchemaR27.liveSourceRevision(db)!=revision)return -1;
            if(verified&&!missing.isEmpty())try(Cursor c=db.rawQuery(guardSql,guardArgs)) {
                int i=0;while(c.moveToNext()) {
                    if(i>=sources.size())return -1;LegacySourceR31 expected=sources.get(i++),current=new LegacySourceR31(c);Row a=expected.row,z=current.row;
                    if(a.ordinal!=z.ordinal||!a.issue.equals(z.issue)||!a.status.equals(z.status)||a.act!=z.act||a.n1!=z.n1||a.n2!=z.n2||a.n3!=z.n3||a.hit!=z.hit||a.actual!=z.actual
                            ||expected.position!=current.position||expected.frozen!=current.frozen||expected.deadline!=current.deadline||expected.open!=current.open||!java.util.Arrays.equals(expected.probabilities,current.probabilities)||current.receipt!=null)return -1;
                }if(i!=sources.size())return -1;
            }
            return 0;
        });
        if(newAudit)b.add("INSERT INTO r29_qual_rebuild(experiment_id,old_state_json,old_certificate,reason,rebuilt_at) VALUES(?,?,?,?,?)",id,JsonR27.write(original),certificateJson,auditKey,audit==null?now:Math.max(now,((Number)audit[1]).longValue()+1));
        if(verified)for(Consumed input:missing)b.add("INSERT INTO r29_qual_receipt(experiment_id,target_ordinal,target_issue,act,hit,u,digest,state_after,consumed_at) VALUES(?,?,?,?,?,?,?,?,?)",id,input.row.ordinal,input.row.issue,input.act?1:0,input.act?(input.hit?1:0):-1,input.u,input.digest,input.stateAfter,now);
        if(certificate!=null&&"ACTIVE".equals(certificate[0]))b.add("UPDATE r27_certificate SET status='REVOKED_LEGACY_EXPOSURE' WHERE policy_hash=? AND status='ACTIVE' AND evidence_digest=? AND issued_at=?",id,certificate[1],certificate[2]);
        b.add("INSERT OR IGNORE INTO r29_qual_cursor(experiment_id,blocked_ordinal,blocked_reason,updated_at) VALUES(?,?,?,?)",id,ordinal,reason,now);
        if(complete)b.add("UPDATE r29_qual_cursor SET blocked_ordinal=-1,blocked_reason=?,consumed_total=?,updated_at=? WHERE experiment_id=?",BLOCK_NONE,state[5],now,id);
        else b.add("UPDATE r29_qual_cursor SET blocked_ordinal=?,blocked_reason=?,updated_at=? WHERE experiment_id=?",ordinal,reason,now,id);
        requireBudgetR31(b,7+(verified&&!missing.isEmpty()?1:0),bindBytesR31(id)*7+(verified&&!missing.isEmpty()?bindBytesR31((Object[])guardArgs):0));
        try{if(!sink.commit("R29_QUAL_LEGACY_COMPAT",b))return false;}catch(IllegalStateException changed){if(!precondition(changed))throw changed;return false;}
        if(!complete)throw new LegacyCompatibilityPendingR31(reason,true);
        return true;
    }

    static boolean precondition(Throwable t) {
        for (Throwable x = t; x != null; x = x.getCause()) { if (String.valueOf(x.getMessage()).contains("R27_WRITE_PRECONDITION")) return true; if (x.getCause() == x) break; }
        return false;
    }

    // ---------------------------------------------------------------- pending draws (separate page from settlement)
    /** Records frozen periods whose draw is still missing (PENDING_DRAW with retry bookkeeping) and resolves those whose
     * draw has arrived. Pending rows stay hit=-1 and remain settleable whenever the backfill brings the draw. */
    public static ResearchWriteQueueR27.Batch pendingDraws(SQLiteDatabase ro,long latestOrdinal) {
        List<ResearchWriteQueueR27.Batch> chunks=pendingDrawChunksR31(ro,latestOrdinal);
        return chunks.isEmpty()?new ResearchWriteQueueR27.Batch():chunks.get(0);
    }
    static final class PendingR31 {
        final long ordinal,rows,acts,checked,checks,first,resolved;final String issue;final boolean exists,draw;
        PendingR31(long ordinal,String issue,long rows,long acts,boolean exists,long checked,long checks,long first,long resolved,boolean draw){
            this.ordinal=ordinal;this.issue=issue;this.rows=rows;this.acts=acts;this.exists=exists;this.checked=checked;this.checks=checks;this.first=first;this.resolved=resolved;this.draw=draw;
        }
    }
    private static List<PendingR31> missingPendingR31(SQLiteDatabase ro,long latest,long now) {
        List<PendingR31> selected=new ArrayList<>();
        try(Cursor c=ro.rawQuery("SELECT f.target_ordinal,f.target_issue,count(*),sum(CASE WHEN f.status='FROZEN' AND f.act=1 THEN 1 ELSE 0 END),p.target_issue,p.last_checked_at,p.checks,p.first_seen_at,p.resolved_at FROM r27_live_frozen f LEFT JOIN r29_pending_draw p ON p.target_ordinal=f.target_ordinal"
                +" WHERE f.hit IN(-1,-2) AND f.target_ordinal<? AND NOT EXISTS(SELECT 1 FROM draws d WHERE d.issue=f.target_issue AND d.dirty=0) GROUP BY f.target_ordinal,f.target_issue ORDER BY f.target_ordinal DESC LIMIT 128",new String[]{Long.toString(latest)})) {
            while(c.moveToNext()) {
                CooperativeContinuationR26.poll();boolean exists=!c.isNull(4);
                if(exists&&!c.getString(1).equals(c.getString(4)))throw dependencyR31("PENDING_DRAW_IDENTITY","R31_PENDING_DRAW_ORDINAL_IDENTITY_CONFLICT");
                long checked=exists?c.getLong(5):0,checks=exists?c.getLong(6):0,first=exists?c.getLong(7):0,resolved=exists?c.getLong(8):0;
                if(exists&&resolved==0&&(stale(checks,first,now)||now-checked<nextCheckDelayMs(checks)))continue;
                selected.add(new PendingR31(c.getLong(0),c.getString(1),c.getLong(2),c.getLong(3),exists,checked,checks,first,resolved,false));
            }
        }return selected;
    }
    private static List<PendingR31> resolvedPendingR31(SQLiteDatabase ro,long after,long upper,int limit) {
        List<PendingR31> selected=new ArrayList<>();
        try(Cursor c=ro.rawQuery("SELECT target_ordinal,target_issue,rows,act_rows,last_checked_at,checks,first_seen_at,resolved_at FROM r29_pending_draw p WHERE resolved_at=0 AND target_ordinal>? AND target_ordinal<=? AND EXISTS(SELECT 1 FROM draws d WHERE d.issue=p.target_issue AND d.dirty=0) ORDER BY target_ordinal LIMIT "+limit,new String[]{Long.toString(after),Long.toString(upper)})) {
            while(c.moveToNext())selected.add(new PendingR31(c.getLong(0),c.getString(1),c.getLong(2),c.getLong(3),true,c.getLong(4),c.getLong(5),c.getLong(6),c.getLong(7),true));
        }return selected;
    }
    private static ResearchWriteQueueR27.Batch pendingChunkR31(List<PendingR31> selected,long now) {
        final List<PendingR31> inputs=java.util.Collections.unmodifiableList(new ArrayList<>(selected));
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
        if(inputs.isEmpty())return b;
        b.guardBefore(db->{
            for(PendingR31 input:inputs)try(Cursor c=db.rawQuery("SELECT p.target_issue,p.last_checked_at,p.checks,p.first_seen_at,p.resolved_at,EXISTS(SELECT 1 FROM draws d WHERE d.issue=? AND d.dirty=0) FROM (SELECT 1) seed LEFT JOIN r29_pending_draw p ON p.target_ordinal=?",new String[]{input.issue,Long.toString(input.ordinal)})) {
                if(!c.moveToFirst()||(!c.isNull(0))!=input.exists||(c.getInt(5)!=0)!=input.draw)return -1;
                if(input.exists&&(!input.issue.equals(c.getString(0))||input.checked!=c.getLong(1)||input.checks!=c.getLong(2)||input.first!=c.getLong(3)||input.resolved!=c.getLong(4)))return -1;
            }return 0;
        });
        long guardBindBytes=0;
        for(PendingR31 input:inputs) {
            guardBindBytes+=bindBytesR31(input.issue,Long.toString(input.ordinal));
            if(input.draw)b.add("UPDATE r29_pending_draw SET resolved_at=? WHERE target_ordinal=? AND target_issue=? AND resolved_at=0",now,input.ordinal,input.issue);
            else {
                b.add("INSERT OR IGNORE INTO r29_pending_draw(target_ordinal,target_issue,rows,act_rows,first_seen_at,last_checked_at) VALUES(?,?,?,?,?,?)",input.ordinal,input.issue,input.rows,input.acts,now,now);
                b.add("UPDATE r29_pending_draw SET rows=?,act_rows=?,last_checked_at=?,checks=checks+1,resolved_at=0 WHERE target_ordinal=? AND target_issue=? AND checks=? AND last_checked_at=? AND resolved_at=?",input.rows,input.acts,now,input.ordinal,input.issue,input.checks,input.exists?input.checked:now,input.resolved);
            }
        }requireBudgetR31(b,inputs.size(),guardBindBytes);return b;
    }
    /** Bounded compatibility page; production drains resolution pages below without a global UPDATE. */
    static List<ResearchWriteQueueR27.Batch> pendingDrawChunksR31(SQLiteDatabase ro,long latest) {
        requireSchemaR31(ro);long now=System.currentTimeMillis();List<ResearchWriteQueueR27.Batch> out=new ArrayList<>();
        List<PendingR31> missing=missingPendingR31(ro,latest,now);
        for(int i=0;i<missing.size();i+=PENDING_PER_COMMIT_R31)out.add(pendingChunkR31(missing.subList(i,Math.min(missing.size(),i+PENDING_PER_COMMIT_R31)),now));
        List<PendingR31> resolved=resolvedPendingR31(ro,Long.MIN_VALUE,Long.MAX_VALUE,128);
        for(int i=0;i<resolved.size();i+=PENDING_PER_COMMIT_R31)out.add(pendingChunkR31(resolved.subList(i,Math.min(resolved.size(),i+PENDING_PER_COMMIT_R31)),now));
        return out;
    }
    static void reconcilePendingDrawsR31(SQLiteDatabase ro,Commit sink,long latest)throws Exception {
        requireSchemaR31(ro);long now=System.currentTimeMillis();List<PendingR31> missing=missingPendingR31(ro,latest,now);
        for(int i=0;i<missing.size();i+=PENDING_PER_COMMIT_R31) {
            CooperativeContinuationR26.poll();ResearchWriteQueueR27.Batch b=pendingChunkR31(missing.subList(i,Math.min(missing.size(),i+PENDING_PER_COMMIT_R31)),now);
            try{if(!sink.commit("LIVE_PENDING_DRAW",b))return;}catch(IllegalStateException changed){if(!precondition(changed))throw changed;return;}
        }
        long upper=Long.MIN_VALUE;
        try(Cursor c=ro.rawQuery("SELECT max(target_ordinal) FROM r29_pending_draw WHERE resolved_at=0",null)){if(c.moveToFirst()&&!c.isNull(0))upper=c.getLong(0);}
        long after=Long.MIN_VALUE;
        while(after<upper) {
            CooperativeContinuationR26.poll();List<PendingR31> rows=resolvedPendingR31(ro,after,upper,PENDING_PER_COMMIT_R31);if(rows.isEmpty())return;
            ResearchWriteQueueR27.Batch b=pendingChunkR31(rows,System.currentTimeMillis());
            try{if(!sink.commit("LIVE_PENDING_RESOLVE",b))return;}catch(IllegalStateException changed){if(!precondition(changed))throw changed;return;}
            after=rows.get(rows.size()-1).ordinal;
        }
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
