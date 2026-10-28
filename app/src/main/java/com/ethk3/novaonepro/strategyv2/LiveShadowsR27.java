package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R27 live shadow pool: FUTURE evidence only (docs/01 1.4). Predictions for the next target are prepared right
 * after the previous draw and frozen with one small write before the deadline (R27-008); a freeze that cannot be
 * written before the deadline is recorded as DEADLINE_MISSED and never back-filled. Settled rows feed the e-process
 * (R27-040), certificates (R27-041) and the mainline choice (R27-042). Same-period negative controls (I00 champion,
 * shuffled-label champion) are frozen exactly like shadows. */
public final class LiveShadowsR27 {
    private LiveShadowsR27() {}
    static final String LIVE_STATES = "('ACTIVE','CONTROL_I00','CONTROL_NULL')";

    static final class Shadow { String id, status; PolicySpecR27 spec; double tau; }
    static List<Shadow> shadows(SQLiteDatabase db, String statuses) {
        List<Shadow> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT policy_hash,spec_json,temperature,status FROM r27_live_shadow WHERE status IN " + statuses + " ORDER BY global_index", null)) {
            while (c.moveToNext()) { Shadow s = new Shadow(); s.id = c.getString(0); s.spec = PolicySpecR27.parse(c.getString(1)); s.tau = c.getDouble(2); s.status = c.getString(3); out.add(s); }
        }
        return out;
    }

    // ---------------------------------------------------------------- settle
    static long settle(RuntimeR27 rt, ResearchPoolR27.Worker w, long latestOrd) throws Exception {
        List<Object[]> rows = new ArrayList<>();
        try (Cursor c = w.ro.rawQuery("SELECT policy_hash,target_ordinal,target_issue,position,n1,n2,n3,act,status FROM r27_live_frozen WHERE hit=-1 AND target_ordinal<=? ORDER BY target_ordinal,policy_hash LIMIT 640", new String[] {String.valueOf(latestOrd)})) {
            while (c.moveToNext()) rows.add(new Object[] {c.getString(0), c.getLong(1), c.getString(2), c.getInt(3), c.getInt(4), c.getInt(5), c.getInt(6), c.getInt(7), c.getString(8)});
        }
        if (rows.isEmpty()) return 0;
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); Map<String, SequentialQualificationR27.State> states = new LinkedHashMap<>(); Map<String, Long> before = new LinkedHashMap<>();
        long now = System.currentTimeMillis(); int settled = 0;
        for (Object[] r : rows) {
            String id = (String) r[0]; long ord = (Long) r[1]; int pos = (Integer) r[3];
            int actual = -1;
            try (Cursor d = w.ro.rawQuery("SELECT n" + pos + " FROM draws WHERE issue=? AND dirty=0", new String[] {(String) r[2]})) { if (d.moveToFirst()) actual = d.getInt(0); }
            if (actual < 1) {
                if (ord < latestOrd - 2) { b.add("UPDATE r27_live_frozen SET hit=-2,settled_at=? WHERE policy_hash=? AND target_ordinal=? AND hit=-1", now, id, ord); settled++; }
                continue; // missing draw: no outcome is invented; the e-process sees nothing for it
            }
            boolean frozen = "FROZEN".equals(r[8]), act = frozen && (Integer) r[7] == 1;
            boolean hit = frozen && (actual == (Integer) r[4] || actual == (Integer) r[5] || actual == (Integer) r[6]);
            b.add("UPDATE r27_live_frozen SET hit=?,settled_at=? WHERE policy_hash=? AND target_ordinal=? AND hit=-1", hit ? 1 : 0, now, id, ord);
            SequentialQualificationR27.State s = states.get(id);
            if (s == null) { s = StoreR27.readEprocess(w.ro, id); if (s == null) continue; states.put(id, s); before.put(id, s.lastOrdinal); }
            s.observe(ord, act, hit); settled++;
        }
        for (Map.Entry<String, SequentialQualificationR27.State> e : states.entrySet()) StoreR27.writeEprocess(b, e.getValue(), before.get(e.getKey()));
        for (Map.Entry<String, SequentialQualificationR27.State> e : states.entrySet())
            if (SequentialQualificationR27.NOT_BETTER.equals(e.getValue().state))
                b.add("UPDATE r27_live_shadow SET status='RETIRED_TO_ARCHIVE',reason='SEQUENTIAL_TEST_NOT_BETTER_THAN_TARGET',updated_at=? WHERE policy_hash=? AND status='ACTIVE'", now, e.getKey());
        b.add("UPDATE r27_opportunity SET settled=(SELECT count(*) FROM r27_live_frozen f WHERE f.target_ordinal=r27_opportunity.target_ordinal AND f.hit>=0) WHERE target_ordinal<=?", latestOrd);
        RuntimeR27.submit(w, "LIVE_SETTLE", b);
        return settled;
    }

    // ---------------------------------------------------------------- freeze
    static long freeze(RuntimeR27 rt, ResearchPoolR27.Worker w, String latestIssue, long latestOrd, long deadline) throws Exception {
        long target = latestOrd + 1; String targetIssue = TargetIssueResolver.fromSequenceOrdinalSU1(target);
        List<Shadow> all = shadows(w.ro, LIVE_STATES), todo = new ArrayList<>();
        for (Shadow s : all) try (Cursor c = w.ro.rawQuery("SELECT 1 FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?", new String[] {s.id, String.valueOf(target)})) { if (!c.moveToFirst()) todo.add(s); }
        if (todo.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); int frozen = 0, acted = 0;
        if (now < deadline - 500) {
            FeatureCacheR27 cache = rt.liveCacheWithPlaceholder(w, latestOrd);
            for (Shadow s : todo) {
                double[] out = ReplayEvaluatorR27.predictNext(s.spec, cache, s.tau);
                boolean ok = Double.isFinite(out[0]);
                double[] p = new double[10]; if (ok) System.arraycopy(out, 0, p, 0, 10);
                int n1 = ok ? (int) out[10] + 1 : 0, n2 = ok ? (int) out[11] + 1 : 0, n3 = ok ? (int) out[12] + 1 : 0, act = ok && out[13] == 1 ? 1 : 0;
                b.add("INSERT OR IGNORE INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                        s.id, target, targetIssue, s.spec.position, StoreR27.pack(p), n1, n2, n3, act, ok ? "FROZEN" : "SKIPPED_GAP", System.currentTimeMillis(), deadline);
                if (ok) frozen++; acted += act;
            }
            final long dl = deadline;
            b.before = db -> System.currentTimeMillis() < dl ? 0 : -1; // never freeze after the deadline
            b.add("INSERT OR REPLACE INTO r27_opportunity(target_ordinal,target_issue,scheduled,frozen,acted,settled,deadline_missed,recorded_at) VALUES(?,?,?,?,?,0,0,?)", target, targetIssue, todo.size(), frozen, acted, System.currentTimeMillis());
            try { while (!w.writes().submit("LIVE_FREEZE", b, w::cancelled, true)) { if (w.cancelled()) return 0; Thread.sleep(20); } return todo.size(); }
            catch (IllegalStateException late) { if (!String.valueOf(late.getMessage()).contains("R27_WRITE_PRECONDITION") && !(late.getCause() != null && String.valueOf(late.getCause().getMessage()).contains("R27_WRITE_PRECONDITION"))) throw late; }
            b = new ResearchWriteQueueR27.Batch();
        }
        // Deadline missed: recorded, counted in the opportunity denominator, never back-filled.
        for (Shadow s : todo) b.add("INSERT OR IGNORE INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,?,?,?,0,0,0,0,'DEADLINE_MISSED',?,?)",
                s.id, target, targetIssue, s.spec.position, StoreR27.pack(new double[10]), System.currentTimeMillis(), deadline);
        b.add("INSERT OR REPLACE INTO r27_opportunity(target_ordinal,target_issue,scheduled,frozen,acted,settled,deadline_missed,recorded_at) VALUES(?,?,?,0,0,0,?,?)", target, targetIssue, todo.size(), todo.size(), System.currentTimeMillis());
        while (!w.writes().submit("LIVE_DEADLINE_MISSED", b, w::cancelled, true)) { if (w.cancelled()) return 0; Thread.sleep(20); }
        return todo.size();
    }

    // ---------------------------------------------------------------- certificates and mainline choice
    static long certify(RuntimeR27 rt, ResearchPoolR27.Worker w, long latestOrd) throws Exception {
        List<CertificateGateR27.Evidence> active = new ArrayList<>(); List<Object[]> nearest = new ArrayList<>();
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis();
        String i00 = controlId(w.ro, "CONTROL_I00"), nul = controlId(w.ro, "CONTROL_NULL");
        for (Shadow s : shadows(w.ro, "('ACTIVE')")) {
            SequentialQualificationR27.State st = StoreR27.readEprocess(w.ro, s.id); if (st == null) continue;
            CertificateGateR27.Evidence e = new CertificateGateR27.Evidence(); e.policyHash = s.id; e.sequentialState = st.state; e.acts = st.acts; e.hits = st.hits; e.logE = st.logUp;
            e.abiEquivalent = true; e.ece = ece(w.ro, s.id); e.calibrationQuality = CertificateGateR27.calibrationQuality(e.ece);
            double[] blocks = blocks(w.ro, s.id); e.worstBlock = blocks[0]; e.maxLosingStreak = (int) blocks[1];
            e.pairedLcbVsI00 = paired(w.ro, s.id, i00); e.pairedLcbVsNull = paired(w.ro, s.id, nul);
            CertificateGateR27.Verdict v = CertificateGateR27.evaluate(e);
            Map<String, Object> g = new LinkedHashMap<>(v.gates); g.put("failing", new ArrayList<Object>(v.failing));
            String digest = StatsR27.sha256(s.id + "|" + st.n + "|" + st.acts + "|" + st.hits + "|" + Double.toHexString(st.logUp));
            b.add("INSERT OR REPLACE INTO r27_certificate(policy_hash,status,gates_json,evidence_digest,issued_at,expires_at) VALUES(?,?,?,?,?,?)", s.id, v.active ? "ACTIVE" : "NOT_ISSUED", JsonR27.write(g), digest, now, now + CertificateGateR27.VALID_MS);
            if (v.active) active.add(e); else nearest.add(new Object[] {s.id, v.failing.size(), v.failing, st.progress()});
        }
        String choice = CertificateGateR27.choose(active);
        nearest.sort((x, y) -> (Integer) x[1] != (Integer) y[1] ? (Integer) x[1] - (Integer) y[1] : Double.compare((Double) y[3], (Double) x[3]));
        StringBuilder near = new StringBuilder();
        for (int i = 0; i < Math.min(3, nearest.size()); i++) near.append(i == 0 ? "" : "；").append(((String) nearest.get(i)[0]).substring(0, 10)).append(" 差：").append(nearest.get(i)[2]);
        Map<String, Object> m = new LinkedHashMap<>(); m.put("qualified", (long) active.size()); m.put("choice", choice == null ? "PRODUCTION_FALLBACK|NO_VALID_CERTIFICATE" : choice); m.put("nearest", near.toString()); m.put("at", now);
        b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('mainline','choice',?,?)", JsonR27.write(m), now);
        RuntimeR27.submit(w, "LIVE_CERTIFY", b);
        return 0; // bookkeeping only; progress is reported by settle/freeze
    }

    static String controlId(SQLiteDatabase db, String status) { try (Cursor c = db.rawQuery("SELECT policy_hash FROM r27_live_shadow WHERE status=? ORDER BY promoted_at DESC LIMIT 1", new String[] {status})) { return c.moveToFirst() ? c.getString(0) : null; } }

    static double ece(SQLiteDatabase db, String id) { try (Cursor c = db.rawQuery("SELECT ece FROM r27_calibration WHERE policy_hash=? AND state='FITTED'", new String[] {id})) { return c.moveToFirst() && !c.isNull(0) ? c.getDouble(0) : Double.NaN; } }

    /** {worst 720-ordinal block hit rate (blocks with ≥ 20 acts), longest losing streak in the latest 720 ordinals}. */
    static double[] blocks(SQLiteDatabase db, String id) {
        Map<Long, int[]> bl = new LinkedHashMap<>(); List<long[]> acts = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT target_ordinal,hit FROM r27_live_frozen WHERE policy_hash=? AND status='FROZEN' AND act=1 AND hit>=0 ORDER BY target_ordinal", new String[] {id})) {
            while (c.moveToNext()) { long o = c.getLong(0); int h = c.getInt(1); int[] a = bl.computeIfAbsent(Math.floorDiv(o, 720L), k -> new int[2]); a[0]++; a[1] += h; acts.add(new long[] {o, h}); }
        }
        double worst = Double.NaN; for (int[] a : bl.values()) if (a[0] >= 20) worst = Double.isNaN(worst) ? a[1] / (double) a[0] : Math.min(worst, a[1] / (double) a[0]);
        long last = acts.isEmpty() ? 0 : acts.get(acts.size() - 1)[0]; int streak = 0, max = 0;
        for (long[] r : acts) { if (last - r[0] >= 720) continue; if (r[1] == 0) { streak++; max = Math.max(max, streak); } else streak = 0; }
        return new double[] {worst, max};
    }

    /** One-sided 95% lower bound of mean(u_a − u_b) over target ordinals both have settled (FUTURE only). */
    static double paired(SQLiteDatabase db, String a, String b) {
        if (b == null) return Double.NaN;
        StatsR27.Moments m = new StatsR27.Moments();
        try (Cursor c = db.rawQuery("SELECT x.act,x.hit,x.status,y.act,y.hit,y.status FROM r27_live_frozen x JOIN r27_live_frozen y ON y.target_ordinal=x.target_ordinal AND y.policy_hash=? WHERE x.policy_hash=? AND x.hit>=0 AND y.hit>=0", new String[] {b, a})) {
            while (c.moveToNext()) {
                double ua = "FROZEN".equals(c.getString(2)) && c.getInt(0) == 1 ? c.getInt(1) - StatsR27.BASE : 0, ub = "FROZEN".equals(c.getString(5)) && c.getInt(3) == 1 ? c.getInt(4) - StatsR27.BASE : 0;
                m.add(ua - ub);
            }
        }
        return m.n < 2 ? Double.NaN : m.lcb(StatsR27.Z95_ONE_SIDED);
    }

    // ---------------------------------------------------------------- semantic ABI for shadows (R27-011)
    static long semanticCheck(RuntimeR27 rt, ResearchPoolR27.Worker w) throws Exception {
        String build = BuildConfig.SOURCE_COMMIT_R26; long units = 0;
        for (Shadow s : shadows(w.ro, LIVE_STATES)) {
            boolean have; try (Cursor c = w.ro.rawQuery("SELECT 1 FROM r27_semantic_fingerprint WHERE policy_hash=? AND build=?", new String[] {s.id, build})) { have = c.moveToFirst(); }
            if (have) continue;
            SemanticAbiR27.Fingerprint now = SemanticAbiR27.fingerprint(s.spec, s.tau, rt.goldenCache);
            String priorDigest = null; byte[] priorRows = null; String priorBuild = null;
            try (Cursor c = w.ro.rawQuery("SELECT fingerprint,rows_blob,build FROM r27_semantic_fingerprint WHERE policy_hash=? ORDER BY computed_at DESC LIMIT 1", new String[] {s.id})) { if (c.moveToFirst()) { priorDigest = c.getString(0); priorRows = c.getBlob(1); priorBuild = c.getString(2); } }
            ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long t = System.currentTimeMillis();
            b.add("INSERT OR IGNORE INTO r27_semantic_fingerprint(policy_hash,build,fingerprint,operator_abi,rows_blob,computed_at) VALUES(?,?,?,?,?,?)", s.id, build, now.digest, rt.operatorAbiHash == null ? "" : rt.operatorAbiHash, now.rowsBlob(), t);
            if (priorDigest != null) {
                SemanticAbiR27.Result r = SemanticAbiR27.compare(SemanticAbiR27.Fingerprint.fromBlob(priorDigest, priorRows, rt.golden.issues), now);
                b.add("INSERT OR IGNORE INTO r27_abi_continuity(policy_hash,old_build,new_build,result,first_diff_issue,first_diff_field,checked_at) VALUES(?,?,?,?,?,?,?)", s.id, priorBuild, build, r.result, r.firstDiffIssue, r.firstDiffField, t);
                if (SemanticAbiR27.CHANGED.equals(r.result)) {
                    // ABI_REBIRTH: old evidence frozen (status), child inherits spec/temperature, FUTURE restarts with a new index.
                    final String child = StatsR27.sha256(s.id + "|ABI_REBIRTH|" + build); final Shadow sh = s;
                    b.add("UPDATE r27_live_shadow SET status='ABI_CHANGED_EVIDENCE_FROZEN',reason=?,updated_at=? WHERE policy_hash=?", "first diff " + r.firstDiffIssue + " " + r.firstDiffField, t, s.id);
                    b.add("INSERT OR IGNORE INTO r27_lineage(child_hash,parent_hash,role,operator,generation,created_at,origin) VALUES(?,?,'ABI_REBIRTH','ABI_REBIRTH',-1,?,'R27_LIVE')", child, s.id, t);
                    b.before = db -> { StoreR27.insertShadow(db, sh.spec, sh.tau, "ABI_REBIRTH from " + sh.id, rt.semanticAbi(sh.spec), child, sh.spec.policyHash());
                        if (!"ACTIVE".equals(sh.status)) db.execSQL("UPDATE r27_live_shadow SET status=? WHERE policy_hash=?", new Object[] {sh.status, child}); return 0; };
                }
            }
            RuntimeR27.submit(w, "SEMANTIC_ABI", b); units++;
        }
        return units;
    }

    // ---------------------------------------------------------------- calibration (R27-037/038)
    static long lastCalibration;
    static ResearchPoolR27.Outcome calibrate(RuntimeR27 rt, ResearchPoolR27.Worker w) throws Exception {
        if (System.currentTimeMillis() - lastCalibration < 10 * 60_000L) return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", 0, 0);
        EvolutionLoopR27 evo = rt.evo; if (evo == null) return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-evolution)", 0, -1);
        EvalContextR27 ctx = evo.context(); ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long n = 0;
        for (Shadow s : shadows(w.ro, "('ACTIVE')")) {
            List<CalibrationRowsR27.Row> rows = new ArrayList<>();
            ReplayEvaluatorR27.Trace tr = ctx.selectTrace(s.spec, s.tau);
            for (int t = ctx.split.selectFrom; t < ctx.split.selectTo; t++) if (!tr.skipped[t])
                rows.add(new CalibrationRowsR27.Row(ctx.data.issues[t], "REPLAY", 0, 0, tr.pHit[t], tr.hit[t], s.spec.position, true, true));
            try (Cursor c = w.ro.rawQuery("SELECT target_issue,probabilities,n1,n2,n3,hit,frozen_at,deadline_at,status FROM r27_live_frozen WHERE policy_hash=? AND hit>=-2 ORDER BY target_ordinal", new String[] {s.id})) {
                while (c.moveToNext()) {
                    double[] p = StoreR27.unpack(c.getBlob(1)); boolean frozen = "FROZEN".equals(c.getString(8));
                    double score = frozen ? p[c.getInt(2) - 1] + p[c.getInt(3) - 1] + p[c.getInt(4) - 1] : 0;
                    rows.add(new CalibrationRowsR27.Row(c.getString(0), "FUTURE", frozen ? c.getLong(6) : 0, c.getLong(7), score, frozen ? c.getInt(5) : -1, s.spec.position, frozen && c.getInt(5) >= 0, true));
                }
            }
            CalibrationRowsR27.Result r = CalibrationRowsR27.fit(rows);
            b.add("INSERT OR REPLACE INTO r27_calibration(policy_hash,method,fit_source,fit_rows,validation_rows,excluded_late,excluded_gap,a,b,ece,ece_by_stratum,state,reason,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    s.id, r.method, r.fitSource, r.fitRows, r.validationRows, r.excludedLate, r.excludedGap, f(r.a), f(r.b), f(r.ece), JsonR27.write(r.eceByStratum), r.state, r.reason, System.currentTimeMillis());
            n++;
        }
        if (n > 0) RuntimeR27.submit(w, "CALIBRATION", b);
        lastCalibration = System.currentTimeMillis();
        return n > 0 ? ResearchPoolR27.Outcome.progress(n, lastCalibration / 1000) : ResearchPoolR27.Outcome.idle("NO_NEW_EVENT:no live shadow", 0, 0);
    }
    static Object f(double d) { return Double.isFinite(d) ? (Object) d : null; }
}
