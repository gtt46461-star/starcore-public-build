package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** R29-010/035 HOST test on a real SQLite engine (sqlite-jdbc) with the production classes and SQL
 * (LiveShadowsR27.settleBatch, QualificationInboxR29, SchemaR27.DDL). Run by tools/r29/run_qual_host.sh.
 *   Q1 in-order arrival equals the reference e-process exactly;
 *   Q2 shuffled/late arrival (1,3 before 2), duplicate passes and killed transactions equal the in-order state;
 *      invariants hold after every pass: receipts == n, unique, contiguous prefix of registered opportunities;
 *   Q3 a late draw for a row the R27 loop had marked hit=-2 is settled from the original freeze and counted;
 *      an e-process advanced without receipts is rebuilt (old state logged, certificate revoked) to the same state;
 *   Q4 a stored hit that disagrees with the draw is isolated as a conflict, never consumed, never duplicated;
 *   Q5 rows still waiting for their draw do not fill the settlement page; they are recorded as PENDING_DRAW;
 *   Q6 a stale concurrent step commits nothing (no double count);
 *   Q8 R29_GATE_V2 evidence from the database: missed (never-frozen) periods stay in the FUTURE_ACT_RATE denominator,
 *      pending ACT is not in the hit rate, the frozen probability is the validation score; recorded report-only;
 *   Q7 the report diagnosis distinguishes "not migrated" (null) from a measured 0 and shows the open conflict.
 * Prints one line per check; exit code 0 only when all pass. Synthetic data only checks mechanics, not quality. */
public final class QualInboxR29HostTest {
    static final List<String> FAIL = new ArrayList<>();
    static void check(String name, boolean ok, String detail) {
        System.out.println("R29_QUAL|" + name + "|" + (ok ? "OK" : "FAILED") + "|" + detail);
        if (!ok) FAIL.add(name);
    }
    static final class Killed extends RuntimeException { Killed() { super("SIMULATED_PROCESS_DEATH"); } }

    static SQLiteDatabase open() throws Exception {
        Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
        SQLiteDatabase db = new SQLiteDatabase(c);
        db.execSQL("CREATE TABLE draws(_id INTEGER PRIMARY KEY,issue TEXT NOT NULL,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_draws_issue ON draws(issue)");
        db.execSQL("CREATE TABLE r26_evolution(attempt_id TEXT PRIMARY KEY,policy_hash TEXT,prospective_qualified INTEGER DEFAULT 0)");
        for (String s : SchemaR27.DDL) try { db.execSQL(s); } catch (RuntimeException otherSchema) { /* index/trigger on a table owned elsewhere */ }
        return db;
    }

    /** One writer transaction like DatabaseCoordinator: before-callback, then ops, then commit; kill → rollback. */
    static QualificationInboxR29.Commit sink(final SQLiteDatabase db, final double kill, final Random rng) {
        return (op, b) -> {
            db.jdbc.setAutoCommit(false);
            try {
                if (b.before != null) { Integer r = b.before.call(db); if (r != null && r < 0) throw new IllegalStateException("R27_WRITE_PRECONDITION:" + op); }
                for (ResearchWriteQueueR27.Op o : b.ops) db.execSQL(o.sql, o.args);
                if (rng.nextDouble() < kill) throw new Killed();
                db.jdbc.commit(); return true;
            } catch (Exception e) { db.jdbc.rollback(); throw e; }
            finally { db.jdbc.setAutoCommit(true); }
        };
    }

    static final class World {
        final int n; final String id = "EXP_A"; final int[][] draws; final Object[][] frozen; // {status, act, pos, n1, n2, n3}
        World(int n, long seed) {
            this.n = n; Random r = new Random(seed); draws = new int[n + 1][]; frozen = new Object[n + 1][];
            for (int o = 1; o <= n; o++) {
                List<Integer> p = new ArrayList<>(); for (int i = 1; i <= 10; i++) p.add(i); Collections.shuffle(p, r);
                draws[o] = new int[10]; for (int i = 0; i < 10; i++) draws[o][i] = p.get(i);
                List<Integer> q = new ArrayList<>(p); Collections.shuffle(q, r);
                double x = r.nextDouble(); String st = x < 0.9 ? "FROZEN" : x < 0.95 ? "SKIPPED_GAP" : "DEADLINE_MISSED";
                int act = "FROZEN".equals(st) && r.nextDouble() < 0.35 ? 1 : 0;
                frozen[o] = new Object[] {st, act, 1 + r.nextInt(10), q.get(0), q.get(1), q.get(2)};
            }
        }
        static String issue(long o) { return "I" + (100000 + o); }
        boolean act(int o) { return "FROZEN".equals(frozen[o][0]) && (Integer) frozen[o][1] == 1; }
        boolean hit(int o) { int a = draws[o][(Integer) frozen[o][2] - 1]; return a == (Integer) frozen[o][3] || a == (Integer) frozen[o][4] || a == (Integer) frozen[o][5]; }
        SequentialQualificationR27.State reference() {
            SequentialQualificationR27.State s = new SequentialQualificationR27.State(id, 1, 0.15);
            for (int o = 1; o <= n; o++) s.observe(o, act(o), act(o) && hit(o));
            return s;
        }
        void register(SQLiteDatabase db, int hitInit) {
            db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,through_ordinal,base_policy,semantic_abi) VALUES(?,'{}',1,x'',1,'ACTIVE','test',0,0,-1,'',  '')", new Object[] {id});
            SequentialQualificationR27.State s = new SequentialQualificationR27.State(id, 1, 0.15);
            db.execSQL("INSERT INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,target_rate,updated_at) VALUES(?,1,?,0,0,0,0,0,0,0,-1,'ACCUMULATING',0.15,0)", new Object[] {id, s.alpha});
            for (int o = 1; o <= n; o++) freezeRow(db, o, hitInit);
        }
        void freezeRow(SQLiteDatabase db, int o, int hitInit) {
            Object[] f = frozen[o]; boolean fr = "FROZEN".equals(f[0]);
            db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    new Object[] {id, (long) o, issue(o), f[2], StoreR27.pack(new double[] {.1, .1, .1, .1, .1, .1, .1, .1, .1, .1}), fr ? f[3] : 0, fr ? f[4] : 0, fr ? f[5] : 0, f[1], f[0], (long) o * 1000, (long) o * 1000 + 500, hitInit});
        }
        void draw(SQLiteDatabase db, int o) {
            int[] d = draws[o];
            db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(?,?,?,?,?,?,?,?,?,?,?,0)", new Object[] {issue(o), d[0], d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9]});
        }
    }

    static long scalar(SQLiteDatabase db, String sql, String... a) { try (Cursor c = db.rawQuery(sql, a)) { return c.moveToFirst() ? c.getLong(0) : -999; } }

    /** settle + consume, as RuntimeR27.LiveTask does; a killed transaction leaves the database as it was. */
    static void pass(SQLiteDatabase db, QualificationInboxR29.Commit sink, long latest) throws Exception {
        try { long[] k = new long[1]; sink.commit("LIVE_SETTLE", LiveShadowsR27.settleBatch(db, latest, k)); } catch (Killed died) { }
        try { QualificationInboxR29.consume(db, sink, LiveShadowsR27.LIVE_STATES, latest); } catch (Killed died) { }
    }

    static String invariants(SQLiteDatabase db, String id) {
        long n = scalar(db, "SELECT n FROM r27_eprocess WHERE policy_hash=?", id), last = scalar(db, "SELECT last_ordinal FROM r27_eprocess WHERE policy_hash=?", id);
        long receipts = scalar(db, "SELECT count(*) FROM r29_qual_receipt WHERE experiment_id=?", id);
        long distinct = scalar(db, "SELECT count(DISTINCT target_ordinal) FROM r29_qual_receipt WHERE experiment_id=?", id);
        long prefix = scalar(db, "SELECT count(*) FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal<=?", id, String.valueOf(last));
        long maxReceipt = scalar(db, "SELECT coalesce(max(target_ordinal),-1) FROM r29_qual_receipt WHERE experiment_id=?", id);
        if (n != receipts || receipts != distinct || prefix != n || maxReceipt != last) return "n=" + n + " receipts=" + receipts + " distinct=" + distinct + " prefix=" + prefix + " last=" + last + " maxReceipt=" + maxReceipt;
        return null;
    }

    static String same(SequentialQualificationR27.State a, SequentialQualificationR27.State b) {
        boolean ok = a.n == b.n && a.acts == b.acts && a.hits == b.hits && a.lastOrdinal == b.lastOrdinal && a.state.equals(b.state)
                && Math.abs(a.logUp - b.logUp) < 1e-12 && Math.abs(a.logDown - b.logDown) < 1e-12 && Math.abs(a.sumU - b.sumU) < 1e-12 && Math.abs(a.sumU2 - b.sumU2) < 1e-12;
        return ok ? null : "db=" + a.json() + " ref=" + b.json();
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        final int N = 240;
        // Q1 in order
        {
            World w = new World(N, 7); SQLiteDatabase db = open(); w.register(db, -1); QualificationInboxR29.Commit s = sink(db, 0, new Random(1));
            for (int o = 1; o <= N; o++) { w.draw(db, o); pass(db, s, o); }
            String d = same(StoreR27.readEprocess(db, w.id), w.reference()), inv = invariants(db, w.id);
            check("Q1_IN_ORDER_EQUALS_REFERENCE", d == null && inv == null, d != null ? d : inv != null ? inv : "n=" + N + " acts=" + w.reference().acts + " hits=" + w.reference().hits);
        }
        // Q2 shuffled + late + duplicate + killed
        for (long seed = 11; seed <= 15; seed++) {
            World w = new World(N, seed); SQLiteDatabase db = open(); w.register(db, -1); Random r = new Random(seed * 31);
            QualificationInboxR29.Commit s = sink(db, 0.25, r);
            Map<Integer, Integer> due = new LinkedHashMap<>(); // ordinal -> tick at which its draw arrives
            for (int o = 1; o <= N; o++) { double x = r.nextDouble(); due.put(o, x < 0.6 ? o : x < 0.9 ? o + 1 + r.nextInt(8) : N + 5 /* long outage */); }
            due.put(1, 1); due.put(3, 3); due.put(2, 4); // explicit 1,3 then 2
            String broken = null; long latest = 0;
            for (int t = 1; t <= N + 10; t++) {
                for (Map.Entry<Integer, Integer> e : due.entrySet()) if (e.getValue() == t) w.draw(db, e.getKey());
                latest = Math.min(t, N);
                pass(db, s, latest); pass(db, s, latest); // duplicate delivery of the same events
                String inv = invariants(db, w.id); if (inv != null && broken == null) broken = "t=" + t + " " + inv;
            }
            QualificationInboxR29.Commit clean = sink(db, 0, r);
            for (int k = 0; k < 20; k++) pass(db, clean, latest);
            String d = same(StoreR27.readEprocess(db, w.id), w.reference());
            long settledRows = scalar(db, "SELECT count(*) FROM r27_live_frozen WHERE hit>=0");
            check("Q2_OUT_OF_ORDER_EQUALS_IN_ORDER_seed" + seed, d == null && broken == null && settledRows == N, d != null ? d : broken != null ? broken : "settled=" + settledRows + " receipts=" + scalar(db, "SELECT count(*) FROM r29_qual_receipt"));
        }
        // Q3 legacy hit=-2 rows + e-process advanced without receipts → rebuild
        {
            World w = new World(60, 21); SQLiteDatabase db = open(); w.register(db, -1); QualificationInboxR29.Commit s = sink(db, 0, new Random(3));
            for (int o = 1; o <= 60; o++) if (o % 7 != 0) w.draw(db, o);
            db.execSQL("UPDATE r27_live_frozen SET hit=-2 WHERE target_ordinal%7=0");
            // the R27 loop had fed observe() in max-ordinal order, skipping the hit=-2 periods
            SequentialQualificationR27.State legacy = new SequentialQualificationR27.State(w.id, 1, 0.15);
            for (int o = 1; o <= 60; o++) if (o % 7 != 0) legacy.observe(o, w.act(o), w.act(o) && w.hit(o));
            db.execSQL("UPDATE r27_eprocess SET log_e_up=?,log_e_down=?,n=?,acts=?,hits=?,sum_u=?,sum_u2=?,last_ordinal=?,state=? WHERE policy_hash=?",
                    new Object[] {legacy.logUp, legacy.logDown, legacy.n, legacy.acts, legacy.hits, legacy.sumU, legacy.sumU2, legacy.lastOrdinal, legacy.state, w.id});
            db.execSQL("INSERT INTO r27_certificate(policy_hash,status,gates_json,evidence_digest,issued_at,expires_at) VALUES(?,'ACTIVE','{}','old',0,1)", new Object[] {w.id});
            pass(db, s, 60);
            long rebuilt = scalar(db, "SELECT count(*) FROM r29_qual_rebuild WHERE experiment_id=?", w.id);
            String cert = null; try (Cursor c = db.rawQuery("SELECT status FROM r27_certificate WHERE policy_hash=?", new String[] {w.id})) { if (c.moveToFirst()) cert = c.getString(0); }
            long blockedAt = scalar(db, "SELECT last_ordinal FROM r27_eprocess WHERE policy_hash=?", w.id);
            long pendingDraw = scalar(db, "SELECT count(*) FROM r29_pending_draw WHERE resolved_at=0");
            for (int o = 7; o <= 60; o += 7) w.draw(db, o); // the backfill finally brings the missing draws
            for (int k = 0; k < 5; k++) pass(db, s, 60);
            String d = same(StoreR27.readEprocess(db, w.id), w.reference());
            long minus2 = scalar(db, "SELECT count(*) FROM r27_live_frozen WHERE hit=-2");
            long resolved = scalar(db, "SELECT count(*) FROM r29_pending_draw WHERE resolved_at>0");
            boolean actBlocks = false; for (int o = 7; o <= 60; o += 7) if (w.act(o)) { actBlocks = blockedAt < o; break; }
            check("Q3_LATE_HIT_MINUS2_RECOVERED_AND_REBUILT", rebuilt == 1 && "REVOKED_REBUILD".equals(cert) && d == null && minus2 == 0 && pendingDraw > 0 && resolved == pendingDraw,
                    "rebuildLog=" + rebuilt + " cert=" + cert + " pendingDraw=" + pendingDraw + " resolved=" + resolved + " blockedAtLastOrdinal=" + blockedAt + " actBlocked=" + actBlocks + (d == null ? "" : " " + d));
        }
        // Q4 conflict isolation
        {
            World w = new World(40, 31); SQLiteDatabase db = open(); w.register(db, -1); QualificationInboxR29.Commit s = sink(db, 0, new Random(5));
            int target = -1; for (int o = 10; o <= 40; o++) if (w.act(o)) { target = o; break; }
            for (int o = 1; o <= 40; o++) w.draw(db, o);
            long[] k = new long[1]; s.commit("LIVE_SETTLE", LiveShadowsR27.settleBatch(db, 40, k));
            int pos = (Integer) w.frozen[target][2]; int stored = w.hit(target) ? 1 : 0;
            // a correction of the source result after settlement: stored hit no longer matches
            Object[] f = w.frozen[target]; int other = 1;
            if (stored == 1) { while (other == (Integer) f[3] || other == (Integer) f[4] || other == (Integer) f[5]) other++; } else other = (Integer) f[3];
            db.execSQL("UPDATE draws SET n" + pos + "=? WHERE issue=?", new Object[] {other, World.issue(target)});
            for (int i = 0; i < 3; i++) pass(db, s, 40);
            long conflicts = scalar(db, "SELECT count(*) FROM r29_qual_conflict WHERE experiment_id=? AND target_ordinal=?", w.id, String.valueOf(target));
            long last = scalar(db, "SELECT last_ordinal FROM r27_eprocess WHERE policy_hash=?", w.id);
            long receiptAt = scalar(db, "SELECT count(*) FROM r29_qual_receipt WHERE experiment_id=? AND target_ordinal>=?", w.id, String.valueOf(target));
            long storedHit = scalar(db, "SELECT hit FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?", w.id, String.valueOf(target));
            check("Q4_CONFLICT_ISOLATED", conflicts == 1 && last < target && receiptAt == 0 && storedHit == stored && QualificationInboxR29.hasOpenConflict(db, w.id) && invariants(db, w.id) == null,
                    "target=" + target + " conflicts=" + conflicts + " last=" + last + " receiptsAtOrAfter=" + receiptAt + " storedHitKept=" + (storedHit == stored));
        }
        // Q5 pending draws do not fill the settlement page
        {
            World w = new World(800, 41); SQLiteDatabase db = open(); w.register(db, -1);
            for (int o = 701; o <= 800; o++) w.draw(db, o);
            long[] k = new long[1]; ResearchWriteQueueR27.Batch b = LiveShadowsR27.settleBatch(db, 800, k);
            sink(db, 0, new Random(1)).commit("LIVE_SETTLE", b);
            long settled = scalar(db, "SELECT count(*) FROM r27_live_frozen WHERE hit>=0"), pending = scalar(db, "SELECT count(*) FROM r29_pending_draw WHERE resolved_at=0");
            check("Q5_PENDING_DRAWS_SEPARATE_PAGE", settled == 100 && pending > 0 && k[0] == 100, "settled=" + settled + " pendingDrawRows=" + pending);
        }
        // Q6 stale concurrent step commits nothing
        {
            World w = new World(30, 51); SQLiteDatabase db = open(); w.register(db, -1); QualificationInboxR29.Commit s = sink(db, 0, new Random(9));
            for (int o = 1; o <= 30; o++) w.draw(db, o);
            long[] k = new long[1]; s.commit("LIVE_SETTLE", LiveShadowsR27.settleBatch(db, 30, k));
            SequentialQualificationR27.State a = StoreR27.readEprocess(db, w.id), b2 = StoreR27.readEprocess(db, w.id);
            List<QualificationInboxR29.Row> rows = QualificationInboxR29.rows(db, w.id, -1, 300);
            QualificationInboxR29.Plan p1 = QualificationInboxR29.plan(a, rows, 30, 256), p2 = QualificationInboxR29.plan(b2, rows, 30, 256);
            boolean first = s.commit("A", QualificationInboxR29.step(w.id, a, p1, -1, 0)); boolean second;
            try { second = s.commit("B", QualificationInboxR29.step(w.id, b2, p2, -1, 0)); } catch (IllegalStateException cas) { second = !QualificationInboxR29.precondition(cas); }
            check("Q6_STALE_STEP_COMMITS_NOTHING", first && !second && invariants(db, w.id) == null && same(StoreR27.readEprocess(db, w.id), w.reference()) == null,
                    "first=" + first + " second=" + second + " receipts=" + scalar(db, "SELECT count(*) FROM r29_qual_receipt"));
        }
        // Q7 report diagnosis: null before migration, measured values after
        {
            SQLiteDatabase bare = new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"));
            Map<String, Object> none = QualificationInboxR29.diagnostics(bare);
            World w = new World(40, 31); SQLiteDatabase db = open(); w.register(db, -1); QualificationInboxR29.Commit s = sink(db, 0, new Random(5));
            for (int o = 1; o <= 40; o++) if (o != 20) w.draw(db, o);
            for (int i = 0; i < 3; i++) pass(db, s, 40);
            Map<String, Object> d = QualificationInboxR29.diagnostics(db);
            Map<String, MetricEvaluatorR29.Probe> probes = ReportR29.qualificationProbes(d);
            boolean ok = none.get("receipts") == null && none.get("pendingDrawOpen") == null && ReportR29.qualificationProbes(none).isEmpty()
                    && Long.valueOf(1).equals(d.get("pendingDrawOpen")) && Long.valueOf(0).equals(d.get("receiptStateMismatch")) && Long.valueOf(0).equals(d.get("legacyHitMinus2Remaining"))
                    && probes.containsKey("M27") && "PARTIAL".equals(probes.get("M27").status);
            check("Q7_REPORT_DIAGNOSIS", ok, "beforeMigration=" + none.get("receipts") + " after=" + d);
        }
        // Q8 GATE_V2 evidence
        {
            World w = new World(120, 61); SQLiteDatabase db = open(); w.register(db, -1); QualificationInboxR29.Commit s = sink(db, 0, new Random(7));
            db.execSQL("DELETE FROM r27_live_frozen WHERE target_ordinal BETWEEN 50 AND 59"); // app was not running: no rows
            for (int o = 1; o <= 120; o++) if (o != 100) w.draw(db, o);
            for (int i = 0; i < 3; i++) pass(db, s, 120);
            GateV2R29.Evidence e = GateV2R29.evidence(db, w.id, 120);
            long acts = 0, settled = 0, hits = 0; for (int o = 1; o <= 120; o++) if ((o < 50 || o > 59) && w.act(o)) { acts++; if (o != 100) { settled++; if (w.hit(o)) hits++; } }
            GateV2R29.Verdict v = GateV2R29.evaluate(e); ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); GateV2R29.record(b, e, v); s.commit("V2", b);
            boolean ok = e.opportunities == 120 && e.actDecisions == acts && e.settledActs == settled && e.hits == hits && e.pendingActs == (w.act(100) ? 1 : 0)
                    && Math.abs(e.p[0] - 0.3) < 1e-12 && !v.wouldIssue && scalar(db, "SELECT count(*) FROM r29_gate_v2 WHERE would_issue=0") == 1
                    && "NOT_DEPLOYED".equals(((Map<?, ?>) v.sections.get("frequency")).get("Q02"));
            check("Q8_GATE_V2_EVIDENCE", ok, "opportunities=" + e.opportunities + " actDecisions=" + e.actDecisions + "/" + acts + " settled=" + e.settledActs + " pending=" + e.pendingActs + " failing=" + v.failing);
        }
        System.out.println("R29_QUAL|SUMMARY|" + (FAIL.isEmpty() ? "OK" : "FAILED " + FAIL));
        System.exit(FAIL.isEmpty() ? 0 : 1);
    }
}
