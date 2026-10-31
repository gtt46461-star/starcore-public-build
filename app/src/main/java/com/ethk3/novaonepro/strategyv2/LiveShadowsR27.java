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
    /** R27.4: observers are the current best evolved candidates frozen every period next to the shadows. They have no
     * e-process, global index or certificate, so they never spend α and never reach the mainline; they only make every
     * strategy's real per-period record visible (docs/01: FUTURE evidence is collected, qualification is unchanged). */
    static final String OBSERVE_STATES = "('" + RuntimeR27.OBSERVE + "')";

    /** R29 r29_policy_defaults.freeze_margin_ms: a FUTURE freeze must be committed at least 5 s before the draw. */
    public static final long FREEZE_MARGIN_MS = 5000;

    static final class Shadow { String id, status; PolicySpecR27 spec; double tau; }
    static List<Shadow> shadows(SQLiteDatabase db, String statuses) {
        List<Shadow> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT policy_hash,spec_json,temperature,status FROM r27_live_shadow WHERE status IN " + statuses + " ORDER BY global_index", null)) {
            while (c.moveToNext()) { Shadow s = new Shadow(); s.id = c.getString(0); s.spec = PolicySpecR27.parse(c.getString(1)); s.tau = c.getDouble(2); s.status = c.getString(3); out.add(s); }
        }
        return out;
    }

    // ---------------------------------------------------------------- settle
    /** R29-010: records outcomes only. Settleable rows (a clean draw exists) are paged separately from rows still
     * waiting for their draw, so an early missing draw never fills the LIMIT; a missing draw is PENDING_DRAW
     * (r29_pending_draw, retried every pass) and the row stays settleable when the backfill brings it. Rows the R27
     * loop had written as hit=-2 are settled again from their original freeze. The e-process is fed exclusively by
     * QualificationInboxR29 in contiguous order (docs/12 §3). */
    static final String SETTLEABLE = "SELECT * FROM (SELECT f.policy_hash,f.target_ordinal,f.target_issue,f.position,f.n1,f.n2,f.n3,f.act,f.status,"
            + "(SELECT CASE f.position WHEN 1 THEN d.n1 WHEN 2 THEN d.n2 WHEN 3 THEN d.n3 WHEN 4 THEN d.n4 WHEN 5 THEN d.n5 WHEN 6 THEN d.n6 WHEN 7 THEN d.n7 WHEN 8 THEN d.n8 WHEN 9 THEN d.n9 WHEN 10 THEN d.n10 END FROM draws d WHERE d.issue=f.target_issue AND d.dirty=0 LIMIT 1) AS actual"
            + " FROM r27_live_frozen f WHERE f.hit IN(-1,-2) AND f.target_ordinal<=?) WHERE actual IS NOT NULL ORDER BY target_ordinal,policy_hash LIMIT 640";
    static final int SETTLE_CHUNK_R30 = 160;

    static long settle(RuntimeR27 rt, ResearchPoolR27.Worker w, long latestOrd) throws Exception {
        long[] settled = new long[1];
        // R30-B01-04: 每片 ≤160 行一个事务；汇总只更新本片涉及的期号。每条 UPDATE 都带 hit IN(-1,-2)，片间幂等。
        for (ResearchWriteQueueR27.Batch chunk : settleChunks(w.ro, latestOrd, settled, SETTLE_CHUNK_R30))
            RuntimeR27.submit(w, "LIVE_SETTLE", chunk);
        return settled[0];
    }

    /** 兼容旧调用与旧测试：把所有片合成一个 Batch（内容与旧版相同，只是汇总语句改为按期号）。 */
    static ResearchWriteQueueR27.Batch settleBatch(SQLiteDatabase ro, long latestOrd, long[] settled) {
        ResearchWriteQueueR27.Batch all = new ResearchWriteQueueR27.Batch();
        for (ResearchWriteQueueR27.Batch chunk : settleChunks(ro, latestOrd, settled, Integer.MAX_VALUE)) all.ops.addAll(chunk.ops);
        return all;
    }

    static List<ResearchWriteQueueR27.Batch> settleChunks(SQLiteDatabase ro, long latestOrd, long[] settled, int perChunk) {
        List<Object[]> rows = new ArrayList<>();
        try (Cursor c = ro.rawQuery(SETTLEABLE, new String[] {String.valueOf(latestOrd)})) {
            while (c.moveToNext()) rows.add(new Object[] {c.getString(0), c.getLong(1), c.getString(2), c.getInt(3), c.getInt(4), c.getInt(5), c.getInt(6), c.getInt(7), c.getString(8), c.getInt(9)});
        }
        List<ResearchWriteQueueR27.Batch> out = new ArrayList<>();
        ResearchWriteQueueR27.Batch b = QualificationInboxR29.pendingDraws(ro, latestOrd); // 缺期记账只放第一片
        java.util.TreeSet<Long> ords = new java.util.TreeSet<>();
        long now = System.currentTimeMillis(); int inChunk = 0;
        for (Object[] r : rows) {
            String id = (String) r[0]; long ord = (Long) r[1]; int actual = (Integer) r[9];
            if (actual < 1) continue; // malformed draw value: never invent an outcome
            boolean frozen = "FROZEN".equals(r[8]);
            boolean hit = frozen && (actual == (Integer) r[4] || actual == (Integer) r[5] || actual == (Integer) r[6]);
            b.add("UPDATE r27_live_frozen SET hit=?,settled_at=? WHERE policy_hash=? AND target_ordinal=? AND hit IN(-1,-2)", hit ? 1 : 0, now, id, ord);
            ords.add(ord); settled[0]++;
            if (++inChunk >= perChunk) { closeSettleChunk(b, ords); out.add(b); b = new ResearchWriteQueueR27.Batch(); ords = new java.util.TreeSet<>(); inChunk = 0; }
        }
        if (inChunk > 0 || !b.isEmpty()) { closeSettleChunk(b, ords); out.add(b); }
        return out;
    }

    private static void closeSettleChunk(ResearchWriteQueueR27.Batch b, java.util.Set<Long> ords) {
        for (Long o : ords)
            b.add("UPDATE r27_opportunity SET settled=(SELECT count(*) FROM r27_live_frozen f WHERE f.target_ordinal=? AND f.hit>=0) WHERE target_ordinal=?", o, o);
    }

    /** R29-035: contiguous qualification consumption after settlement (single owner of e-process updates). */
    static long qualify(RuntimeR27 rt, final ResearchPoolR27.Worker w, long latestOrd) throws Exception {
        QualificationInboxR29.Pass p = QualificationInboxR29.consume(w.ro, (op, batch) -> w.writes().submit(op, batch, w::cancelled), LIVE_STATES, latestOrd);
        return p.consumed + p.rebuilt;
    }

    // ---------------------------------------------------------------- R30-B03-01 PIT violations
    static final String PIT_DDL_R30 = "CREATE TABLE IF NOT EXISTS r30_pit_violation(policy_hash TEXT,target_ordinal INTEGER,code TEXT,source TEXT,recorded_at INTEGER,PRIMARY KEY(policy_hash,target_ordinal,code))";
    static void pitViolationsR30(ResearchWriteQueueR27.Batch b, String policy, long target, List<String> codes, String source) {
        b.add(PIT_DDL_R30);
        for (String code : codes) b.add("INSERT OR IGNORE INTO r30_pit_violation(policy_hash,target_ordinal,code,source,recorded_at) VALUES(?,?,?,?,?)", policy, target, code, source, System.currentTimeMillis());
    }
    /** Replay samples rejected by the guard are recorded with the next research freeze batch (bounded per batch). */
    static void drainReplayPitViolationsR30(ResearchWriteQueueR27.Batch b) {
        Object[] v; int n = 0; boolean ddl = false;
        while (n++ < 256 && (v = ReplayEvaluatorR27.PIT_VIOLATIONS_R30.poll()) != null) {
            if (!ddl) { b.add(PIT_DDL_R30); ddl = true; }
            b.add("INSERT OR IGNORE INTO r30_pit_violation(policy_hash,target_ordinal,code,source,recorded_at) VALUES(?,?,?,?,?)", v[0], v[1], v[2], v[3], System.currentTimeMillis());
        }
    }

    // ---------------------------------------------------------------- freeze
    static long freeze(RuntimeR27 rt, ResearchPoolR27.Worker w, String latestIssue, long latestOrd, long deadline) throws Exception {
        return freeze(rt, w, latestIssue, latestOrd, deadline, LIVE_STATES, true);
    }
    static long freeze(RuntimeR27 rt, ResearchPoolR27.Worker w, String latestIssue, long latestOrd, long deadline, String statuses, boolean opportunity) throws Exception {
        long target = latestOrd + 1; String targetIssue = TargetIssueResolver.fromSequenceOrdinalSU1(target);
        List<Shadow> all = shadows(w.ro, statuses), todo = new ArrayList<>();
        for (Shadow s : all) try (Cursor c = w.ro.rawQuery("SELECT 1 FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?", new String[] {s.id, String.valueOf(target)})) { if (!c.moveToFirst()) todo.add(s); }
        if (todo.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); int frozen = 0, acted = 0;
        if (now < deadline - FREEZE_MARGIN_MS) {
            final long sourceRevision=SchemaR27.liveSourceRevision(w.ro);
            final long sourceSchema=SchemaR27.liveSourceSchemaVersion(w.ro);
            if (sourceRevision<0 || sourceSchema<0 || !SchemaR27.liveSourceTriggersValid(w.ro)) {
                recordSourceWait(w,todo,target,"SOURCE_REVISION_UNAVAILABLE"); return 0;
            }
            FeatureCacheR27 cache = rt.liveCacheWithPlaceholder(w, latestOrd);
            if (cache == null) { recordSourceWait(w,todo,target,"SOURCE_HEAD_CHANGED_OR_INVALID"); return 0; }
            if (sourceRevision!=SchemaR27.liveSourceRevision(w.ro) || sourceSchema!=SchemaR27.liveSourceSchemaVersion(w.ro)) {
                recordSourceWait(w,todo,target,"SOURCE_CHANGED_DURING_PREPARATION"); return 0;
            }
            boolean needsModel=false;for(Shadow shadow:todo)needsModel|=shadow.spec.ast.modelPositions!=0;
            ModelSurfaceStoreR30.Prepared modelPrepared=needsModel?ModelSurfaceStoreR30.prepareLive(rt,w,cache,deadline):null;
            for (Shadow s : todo) {
                double[] out = ReplayEvaluatorR27.predictNext(s.spec, cache, s.tau);
                boolean ok = Double.isFinite(out[0]);
                double[] p = new double[10]; if (ok) System.arraycopy(out, 0, p, 0, 10);
                int n1 = ok ? (int) out[10] + 1 : 0, n2 = ok ? (int) out[11] + 1 : 0, n3 = ok ? (int) out[12] + 1 : 0, act = ok && out[13] == 1 ? 1 : 0;
                if (ok) { // R30-B03-01: a freeze that fails the PIT guard is not written; the violation is.
                    PitGuardR30.Freeze f = new PitGuardR30.Freeze();
                    long prepared = System.currentTimeMillis();
                    f.targetOrdinal = target; f.trainedThroughOrdinal = latestOrd; f.featureThroughOrdinal = latestOrd; f.position = s.spec.position;
                    f.k3 = new int[] {n1, n2, n3}; f.probabilities = p; f.modelAvailableAt = now; f.featureAvailableAt = now;
                    f.preparedAt = prepared; f.frozenAt = prepared; f.deadlineAt = deadline;
                    List<String> violations = PitGuardR30.check(f);
                    if (!violations.isEmpty()) { pitViolationsR30(b, s.id, target, violations, "LIVE");ModelSurfaceStoreR30.calls(b,s.id,s.spec,target,cache,modelPrepared,false,"POLICY_PIT:"+String.join(",",violations));continue; }
                }
                b.add("INSERT OR IGNORE INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,?,?,?,?,?,?,?,?,CAST(strftime('%s','now') AS INTEGER)*1000+CAST(substr(strftime('%f','now'),4,3) AS INTEGER),?)",
                        s.id, target, targetIssue, s.spec.position, StoreR27.pack(p), n1, n2, n3, act, ok ? "FROZEN" : "SKIPPED_GAP", deadline);
                ModelSurfaceStoreR30.calls(b,s.id,s.spec,target,cache,modelPrepared,ok,"POLICY_PREDICTION_UNAVAILABLE");
                if (ok) frozen++; acted += act;
            }
            // R30-B02-04: predictions above are computed on this worker thread; the batch carries only INSERTs and the
            // deadline checks, so the writer transaction stays a short freeze write.
            drainReplayPitViolationsR30(b);
            final long dl = deadline;
            b.before = db -> {
                if (sourceRevision!=SchemaR27.liveSourceRevision(db) || sourceSchema!=SchemaR27.liveSourceSchemaVersion(db)
                        || !sameDrawHead(db, latestIssue, dl)) throw new IllegalStateException("R30_FREEZE_SOURCE_CHANGED");
                return System.currentTimeMillis() < dl - FREEZE_MARGIN_MS ? 0 : -1;
            };
            b.after = b.before; // Both the source and the original 5 s margin must still hold before commit.
            if (opportunity) b.add("INSERT OR REPLACE INTO r27_opportunity(target_ordinal,target_issue,scheduled,frozen,acted,settled,deadline_missed,recorded_at) VALUES(?,?,?,?,?,0,0,?)", target, targetIssue, todo.size(), frozen, acted, System.currentTimeMillis());
            try { while (!w.writes().submit(opportunity ? "LIVE_FREEZE" : "OBSERVE_FREEZE", b, w::cancelled, true)) { if (w.cancelled()) return 0; Thread.sleep(20); } return todo.size(); }
            catch (IllegalStateException late) {
                if (causedBy(late, "R30_FREEZE_SOURCE_CHANGED")) {
                    recordSourceWait(w,todo,target,"SOURCE_CHANGED_BEFORE_COMMIT"); return 0;
                }
                if (!causedBy(late, "R27_WRITE_PRECONDITION")) throw late;
            }
            b = new ResearchWriteQueueR27.Batch();
        }
        // Deadline missed: recorded, counted in the opportunity denominator, never back-filled.
        for (Shadow s : todo) b.add("INSERT OR IGNORE INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,?,?,?,0,0,0,0,'DEADLINE_MISSED',?,?)",
                s.id, target, targetIssue, s.spec.position, StoreR27.pack(new double[10]), System.currentTimeMillis(), deadline);
        if (opportunity) b.add("INSERT OR REPLACE INTO r27_opportunity(target_ordinal,target_issue,scheduled,frozen,acted,settled,deadline_missed,recorded_at) VALUES(?,?,?,0,0,0,?,?)", target, targetIssue, todo.size(), todo.size(), System.currentTimeMillis());
        while (!w.writes().submit("LIVE_DEADLINE_MISSED", b, w::cancelled, true)) { if (w.cancelled()) return 0; Thread.sleep(20); }
        return todo.size();
    }

    /** A rejected attempt remains a retryable, explicit gap; it never creates a post-result forecast. */
    private static void recordSourceWait(ResearchPoolR27.Worker w,List<Shadow> pending,long target,String reason) throws Exception {
        ResearchWriteQueueR27.Batch wait=new ResearchWriteQueueR27.Batch();wait.add(PIT_DDL_R30);
        long now=System.currentTimeMillis();
        for (Shadow s:pending) {wait.add("INSERT OR IGNORE INTO r30_pit_violation(policy_hash,target_ordinal,code,source,recorded_at) VALUES(?,?,?,?,?)",s.id,target,reason,"LIVE_WAIT",now);ModelSurfaceStoreR30.calls(wait,s.id,s.spec,target,null,null,false,reason);}
        while (!w.writes().submit("LIVE_SOURCE_WAIT",wait,w::cancelled)) { if(w.cancelled()) return;Thread.sleep(20); }
    }

    /** The sole writer verifies the original draw and scheduled clock immediately before publication. */
    static boolean sameDrawHead(SQLiteDatabase db,String issue,long deadline) {
        try (Cursor c=db.rawQuery("SELECT issue,open_time FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)) {
            if (!c.moveToFirst()) return false;
            long open=c.getLong(1);
            return issue.equals(c.getString(0)) && open>0 && open<=Long.MAX_VALUE-60_000L && open+60_000L==deadline;
        }
    }
    private static boolean causedBy(Throwable failure,String marker) {
        for (Throwable t=failure;t!=null;t=t.getCause()) {
            if (String.valueOf(t.getMessage()).contains(marker)) return true;
            if (t.getCause()==t) break;
        }
        return false;
    }

    // ---------------------------------------------------------------- certificates and mainline choice
    static long certify(RuntimeR27 rt, ResearchPoolR27.Worker w, long latestOrd) throws Exception {
        List<CertificateGateR27.Evidence> active = new ArrayList<>(); List<Object[]> nearest = new ArrayList<>();
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis();
        String i00 = controlId(w.ro, "CONTROL_I00"), nul = controlId(w.ro, "CONTROL_NULL");
        String deployed = null;
        try (Cursor c = w.ro.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'", null)) {
            if (c.moveToFirst()) { Object ch = ((Map<?, ?>) JsonR27.parse(c.getString(0))).get("choice"); deployed = ch instanceof String ? (String) ch : null; }
        } catch (RuntimeException none) { deployed = null; }
        for (Shadow s : shadows(w.ro, "('ACTIVE')")) {
            SequentialQualificationR27.State st = StoreR27.readEprocess(w.ro, s.id); if (st == null) continue;
            CertificateGateR27.Evidence e = new CertificateGateR27.Evidence(); e.policyHash = s.id; e.sequentialState = st.state; e.acts = st.acts; e.hits = st.hits; e.logE = st.logUp;
            e.abiEquivalent = true; e.ece = ece(w.ro, s.id); e.calibrationQuality = CertificateGateR27.calibrationQuality(e.ece);
            double[] blocks = blocks(w.ro, s.id); e.worstBlock = blocks[0]; e.maxLosingStreak = (int) blocks[1];
            e.pairedLcbVsI00 = paired(w.ro, s.id, i00); e.pairedLcbVsNull = paired(w.ro, s.id, nul);
            CertificateGateR27.Verdict v = CertificateGateR27.evaluate(e);
            // R29-035: an isolated result conflict blocks the certificate until it is resolved (never overwritten).
            List<String> failing = new ArrayList<>(v.failing); boolean issued = v.active;
            if (QualificationInboxR29.blockedForCertificate(w.ro, s.id)) { issued = false; failing.add("QUALIFICATION_GAP_OR_CONFLICT"); }
            Map<String, Object> g = new LinkedHashMap<>(v.gates); g.put("failing", new ArrayList<Object>(failing));
            String digest = StatsR27.sha256(s.id + "|" + st.n + "|" + st.acts + "|" + st.hits + "|" + Double.toHexString(st.logUp));
            b.add("INSERT OR REPLACE INTO r27_certificate(policy_hash,status,gates_json,evidence_digest,issued_at,expires_at) VALUES(?,?,?,?,?,?)", s.id, issued ? "ACTIVE" : "NOT_ISSUED", JsonR27.write(g), digest, now, now + CertificateGateR27.VALID_MS);
            // R29-036: R29_GATE_V2 evaluated beside the unchanged R27 gate and recorded separately (report only).
            GateV2R29.Evidence v2 = GateV2R29.evidence(w.ro, s.id, latestOrd); v2.pairedLcbVsI00 = e.pairedLcbVsI00; v2.pairedLcbVsNull = e.pairedLcbVsNull; v2.deployed = s.id.equals(deployed);
            GateV2R29.record(b, v2, GateV2R29.evaluate(v2));
            if (issued) active.add(e); else nearest.add(new Object[] {s.id, failing.size(), failing, st.progress()});
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
