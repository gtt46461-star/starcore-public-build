package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R27 research runtime: the single owner of the R27 research tasks on ResearchPoolR27 (docs/02 2.4: new system is
 * the Owner; the R26 coordinator keeps the legacy R26 tasks on its own thread). Tasks:
 * r27-replay-dataset, r27-evolution, r27-live, r27-atoms, r27-atomnet, r27-abi, r27-calibration, r27-chart-cache,
 * r27-legacy-export, r27-health. Every slice reports a WaitKindR27 reason through the pool. */
public final class RuntimeR27 implements AutoCloseable {
    public static final int DATASET_ROWS = 5000, REFRESH_NEW_ROWS = 360, LIVE_REBASE_ROWS = 6500;
    public static final String LOOP = "MAIN";
    final DbStore store; final ResearchPoolR27 pool;
    // Evolution dataset snapshot (rebound only between generations).
    volatile ReplayDatasetR27 evoData; volatile FeatureCacheR27 evoCache; volatile ReplayDatasetR27 pendingData; volatile FeatureCacheR27 pendingCache;
    volatile EvolutionLoopR27 evo;
    // Live dataset grows by appending; family streams continue incrementally.
    ReplayDatasetR27 liveData; FeatureCacheR27 liveCache;
    // Golden set and operator ABI (semantic ABI).
    volatile ReplayDatasetR27 golden; volatile FeatureCacheR27 goldenCache; volatile Map<String, String> operatorAbi; volatile String operatorAbiHash;
    // Atom replay frame and states.
    volatile AtomReplayR27.Frame frame; volatile String frameDataset = ""; int frameCursor; double[][] frameValues; boolean[][] framePresent; int[] frameHit, framePos, frameActual; String[] frameIssue;
    final Map<Integer, String> atomStates = new HashMap<>();
    volatile AtomNetTrainerR27 atomNet; volatile String atomNetDataset = "";

    private static volatile RuntimeR27 active;
    public static RuntimeR27 active() { return active; }

    public RuntimeR27(DbStore store, String session) {
        this.store = store;
        pool = new ResearchPoolR27(store, session, ResearchPoolR27.defaultThreads());
        pool.register(new DatasetTask()); pool.register(new LiveTask()); pool.register(new EvolutionTask()); pool.register(new AbiTask());
        pool.register(new AtomsTask()); pool.register(new AtomNetTask()); pool.register(new CalibrationTask()); pool.register(new ChartTask());
        pool.register(new LegacyExportTask()); pool.register(new HealthTask());
        active = this;
    }
    public void start() { pool.start(); }
    public ResearchPoolR27 pool() { return pool; }
    @Override public void close() { pool.close(); if (active == this) active = null; }

    static String latestIssue(SQLiteDatabase ro) { try (Cursor c = ro.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1", null)) { return c.moveToFirst() ? c.getString(0) : null; } }
    static long latestOpen(SQLiteDatabase ro) { try (Cursor c = ro.rawQuery("SELECT open_time FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1", null)) { return c.moveToFirst() ? c.getLong(0) : 0; } }
    static void submit(ResearchPoolR27.Worker w, String op, ResearchWriteQueueR27.Batch b) throws InterruptedException {
        while (!w.writes().submit(op, b, w::cancelled)) { if (w.cancelled()) throw new InterruptedException("R27_CANCELLED"); Thread.sleep(50); }
    }

    // ------------------------------------------------------------------ r27-replay-dataset (R27-013/016)
    final class DatasetTask implements ResearchPoolR27.Task {
        public String id() { return "r27-replay-dataset"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            String latest = latestIssue(w.ro); if (latest == null) return ResearchPoolR27.Outcome.idle("WAITING_DRAWS", 0, 0);
            long latestOrd = TargetIssueResolver.sequenceOrdinalSU1(latest);
            ReplayDatasetR27 cur = pendingData != null ? pendingData : evoData;
            if (cur != null && latestOrd - cur.ordinals[cur.size() - 1] < REFRESH_NEW_ROWS) return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", cur.ordinals[cur.size() - 1], latestOrd);
            long t0 = System.currentTimeMillis();
            ReplayDatasetR27 d = StoreR27.loadDataset(w.ro, DATASET_ROWS);
            if (d.size() < 3 * SplitR27.BLOCK) return ResearchPoolR27.Outcome.idle("WAITING_DRAWS:rows=" + d.size(), d.size(), 3 * SplitR27.BLOCK);
            FeatureCacheR27 c = new FeatureCacheR27(d);
            SplitR27 sp = SplitR27.of(d, evo == null ? null : evo.ledger().degradedBlocks());
            ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
            b.add("INSERT OR IGNORE INTO r27_replay_dataset(dataset_hash,from_issue,to_issue,rows,segments,split,built_at,build_ms) VALUES(?,?,?,?,?,?,?,?)",
                    d.datasetHash, d.issues[0], d.issues[d.size() - 1], d.size(), d.segments(), sp.describe(), System.currentTimeMillis(), System.currentTimeMillis() - t0);
            submit(w, "REPLAY_DATASET", b);
            if (evoData == null) { evoData = d; evoCache = c; } else { pendingData = d; pendingCache = c; }
            return ResearchPoolR27.Outcome.progress(1, d.ordinals[d.size() - 1]);
        }
    }

    // ------------------------------------------------------------------ r27-evolution (R27-021..032)
    final class EvolutionTask implements ResearchPoolR27.Task {
        public String id() { return "r27-evolution"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            if (evoCache == null) return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-replay-dataset)", 0, -1);
            if (evo == null) evo = restoreOrCreate(w);
            if (evo.step == 1 && pendingCache != null) { evoData = pendingData; evoCache = pendingCache; pendingData = null; pendingCache = null; evo.rebind(evoCache); }
            evo.executor = pool.compute(); evo.serialWhile = ResearchWriteQueueR27::p0Writing; evo.leafWeight = leafWeights(); evo.derivedLeaves = derived;
            String birthIssue = evoData.issues[evoData.size() - 1];
            StoreR27.EvolutionSink sink = new StoreR27.EvolutionSink(LOOP, birthIssue);
            EvolutionLoopR27 loop = evo; loop.setSink(sink);
            int stepBefore = loop.step, genBefore = loop.generation;
            loop.step();
            ResearchWriteQueueR27.Batch b = sink.batch;
            b.add("INSERT OR REPLACE INTO r27_evolution_cursor(loop,generation,step,dataset_hash,rng_state,snapshot,updated_at) VALUES(?,?,?,?,?,?,?)", LOOP, loop.generation, loop.step, evoData.datasetHash, loop.rng.state, loop.snapshot(), System.currentTimeMillis());
            if (!sink.promotions.isEmpty()) {
                final List<Object[]> promos = sink.promotions;
                b.before = db -> { for (Object[] p : promos) { PolicySpecR27 s = (PolicySpecR27) p[0]; StoreR27.insertShadow(db, s, (Double) p[1], (String) p[3], semanticAbi(s), s.policyHash(), s.policyHash()); } return 0; };
            }
            submit(w, "EVOLUTION_STEP_" + EvolutionLoopR27.STEPS[stepBefore], b);
            if (stepBefore == 9 && genBefore % 50 == 49) derivedDue = true;
            return ResearchPoolR27.Outcome.progress(1, (long) loop.generation * 10 + loop.step);
        }
    }
    volatile boolean derivedDue; volatile List<PolicyAstR27.Node> derived = new ArrayList<>();

    EvolutionLoopR27 restoreOrCreate(ResearchPoolR27.Worker w) {
        EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.seed = System.currentTimeMillis();
        try (Cursor c = w.ro.rawQuery("SELECT snapshot,dataset_hash FROM r27_evolution_cursor WHERE loop=?", new String[] {LOOP})) {
            if (c.moveToFirst()) {
                try { return EvolutionLoopR27.restore(c.getString(0), cfg, evoCache, null, System::currentTimeMillis); }
                catch (RuntimeException mismatch) {
                    // Dataset changed mid-generation (e.g. reinstall): the generation restarts at step 1; nothing registered twice
                    // because registrations are keyed by policy hash.
                    android.util.Log.w("STARCORE_R27", "evolution cursor restart", mismatch);
                }
            }
        }
        EvolutionLoopR27 l = new EvolutionLoopR27(cfg, evoCache, null, System::currentTimeMillis);
        try (Cursor c = w.ro.rawQuery("SELECT max(generation)+1 FROM r27_generation WHERE loop=?", new String[] {LOOP})) { if (c.moveToFirst() && !c.isNull(0)) l.generation = c.getInt(0); }
        return l;
    }

    Map<String, Double> leafWeights() { synchronized (atomStates) { return AtomScoreR27.leafWeights(new HashMap<>(atomStates)); } }

    String semanticAbi(PolicySpecR27 s) { Map<String, String> abi = operatorAbi; return abi == null ? "" : SemanticAbiR27.policyAbi(s, abi); }

    // ------------------------------------------------------------------ r27-live (R27-008/032/040/041/042)
    final class LiveTask implements ResearchPoolR27.Task {
        public String id() { return "r27-live"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            String latest = latestIssue(w.ro); if (latest == null) return ResearchPoolR27.Outcome.idle("WAITING_DRAWS", 0, 0);
            long latestOrd = TargetIssueResolver.sequenceOrdinalSU1(latest), deadline = latestOpen(w.ro) + 60_000L;
            w.writes().setNextDeadline(deadline);
            long units = LiveShadowsR27.settle(RuntimeR27.this, w, latestOrd);
            ensureControls(w);
            units += LiveShadowsR27.freeze(RuntimeR27.this, w, latest, latestOrd, deadline);
            units += LiveShadowsR27.certify(RuntimeR27.this, w, latestOrd);
            return units > 0 ? ResearchPoolR27.Outcome.progress(units, latestOrd) : ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", latestOrd, latestOrd);
        }
    }

    /** Keeps one I00 and one shuffled-label control frozen every period next to the shadows (same-period negative controls). */
    void ensureControls(ResearchPoolR27.Worker w) throws Exception {
        EvolutionLoopR27 l = evo; if (l == null) return;
        Object[][] controls = {{"CONTROL_I00", l.i00Champion, l.i00Tau}, {"CONTROL_NULL", l.nullEvolution.champion, l.nullEvolution.championTau}};
        for (Object[] c : controls) {
            final PolicySpecR27 spec = (PolicySpecR27) c[1]; if (spec == null) continue; final String status = (String) c[0];
            boolean have; try (Cursor x = w.ro.rawQuery("SELECT 1 FROM r27_live_shadow WHERE status=?", new String[] {status})) { have = x.moveToFirst(); }
            if (have) continue;
            final double tau = (Double) c[2]; final String id = StatsR27.sha256(status + "|" + spec.policyHash());
            ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
            b.before = db -> { StoreR27.insertShadow(db, spec, tau, status + ":same-period negative control", semanticAbi(spec), id, spec.policyHash());
                db.execSQL("UPDATE r27_live_shadow SET status=? WHERE policy_hash=?", new Object[] {status, id}); db.execSQL("UPDATE r27_eprocess SET state='CONTROL_NOT_ELIGIBLE' WHERE policy_hash=?", new Object[] {id}); return 0; };
            submit(w, "LIVE_CONTROL", b);
        }
    }

    /** Live dataset + placeholder for the next target (family streams continue from the previous cache). */
    synchronized FeatureCacheR27 liveCacheWithPlaceholder(ResearchPoolR27.Worker w, long latestOrd) {
        ReplayDatasetR27 fresh = StoreR27.loadDataset(w.ro, DATASET_ROWS);
        if (liveData == null || liveData.size() > LIVE_REBASE_ROWS || fresh.size() == 0) { liveData = fresh; liveCache = null; }
        else {
            long last = liveData.ordinals[liveData.size() - 1];
            for (int i = 0; i < fresh.size(); i++) if (fresh.ordinals[i] > last) liveData = liveData.append(new ReplayDatasetR27.Row(fresh.issues[i], fresh.ordinals[i], fresh.draws[i]));
        }
        ReplayDatasetR27 withSlot = liveData.append(new ReplayDatasetR27.Row("PLACEHOLDER", latestOrd + 1, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}));
        FeatureCacheR27 c = new FeatureCacheR27(withSlot, 64);
        if (liveCache != null) { try { c.extendFrom(liveCache); } catch (RuntimeException rebuild) { /* streams rebuilt lazily */ } }
        liveCache = c;
        return c;
    }

    // ------------------------------------------------------------------ r27-abi (R27-010/011/012)
    final class AbiTask implements ResearchPoolR27.Task {
        public String id() { return "r27-abi"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            ensureGolden();
            long units = LegacyContinuityR27.step(store, w.ro, golden, operatorAbiHash, w.writes());
            units += LiveShadowsR27.semanticCheck(RuntimeR27.this, w);
            return units > 0 ? ResearchPoolR27.Outcome.progress(units, units) : ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", 0, 0);
        }
    }
    synchronized void ensureGolden() throws Exception {
        if (golden != null) return;
        try (InputStream in = store.contextForR26().getAssets().open(SemanticAbiR27.GOLDEN_ASSET)) { golden = SemanticAbiR27.readGolden(in, true); }
        goldenCache = new FeatureCacheR27(golden, 64);
        operatorAbi = OperatorGoldenR27.fingerprints(goldenCache);
        operatorAbiHash = SemanticAbiR27.operatorSetHash(operatorAbi);
        store.setState("r27_operator_abi", operatorAbiHash);
    }

    // ------------------------------------------------------------------ r27-atoms (R27-043/044) and r27-atomnet (R27-033/034/036)
    final class AtomsTask implements ResearchPoolR27.Task {
        public String id() { return "r27-atoms"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception { return AtomWorldR27.atomsStep(RuntimeR27.this, w); }
    }
    final class AtomNetTask implements ResearchPoolR27.Task {
        public String id() { return "r27-atomnet"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception { return AtomWorldR27.atomNetStep(RuntimeR27.this, w); }
    }

    // ------------------------------------------------------------------ r27-calibration (R27-037/038)
    final class CalibrationTask implements ResearchPoolR27.Task {
        public String id() { return "r27-calibration"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception { return LiveShadowsR27.calibrate(RuntimeR27.this, w); }
    }

    // ------------------------------------------------------------------ r27-chart-cache (R27-049)
    final class ChartTask implements ResearchPoolR27.Task {
        long lastBuilt;
        public String id() { return "r27-chart-cache"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            if (System.currentTimeMillis() - lastBuilt < 30_000) return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", 0, 0);
            ResearchWriteQueueR27.Batch b = ChartCacheR27.build(w.ro, RuntimeR27.this);
            submit(w, "CHART_CACHE", b); lastBuilt = System.currentTimeMillis();
            return ResearchPoolR27.Outcome.progress(b.ops.size(), lastBuilt / 1000);
        }
    }

    // ------------------------------------------------------------------ r27-legacy-export (R27-019/020)
    final class LegacyExportTask implements ResearchPoolR27.Task {
        public String id() { return "r27-legacy-export"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception { return LegacyPolicyExportR27.step(RuntimeR27.this, w); }
    }

    // ------------------------------------------------------------------ r27-health (R27-006/048)
    final class HealthTask implements ResearchPoolR27.Task {
        long last, lastSize; boolean assetsCached; final Map<String, Long> fetchFlushed = new HashMap<>();
        public String id() { return "r27-health"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            if (System.currentTimeMillis() - last < 60_000) return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", 0, 0);
            last = System.currentTimeMillis(); w.writes().flush();
            ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
            if (!assetsCached) {
                // R27-053/054: build identity, card registry, source audit and R26→R27 merge status packaged as assets.
                for (String name : new String[] {"build-identity", "r27_cards", "source-audit", "merge-status"}) {
                    String payload;
                    try (InputStream in = store.contextForR26().getAssets().open("r27/" + name + ".json")) {
                        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream(); byte[] chunk = new byte[8192]; int k; while ((k = in.read(chunk)) > 0) buf.write(chunk, 0, k);
                        payload = buf.toString("UTF-8"); JsonR27.parse(payload);
                    } catch (Exception missing) { Map<String, Object> m = new LinkedHashMap<>(); m.put("status", "INSUFFICIENT"); m.put("reason", "asset r27/" + name + ".json unavailable: " + missing.getClass().getSimpleName()); payload = JsonR27.write(m); }
                    b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('asset',?,?,?)", name, payload, last);
                }
                Map<String, Object> runtime = new LinkedHashMap<>(); runtime.put("sourceCommit", BuildConfig.SOURCE_COMMIT_R26); runtime.put("versionCode", (long) BuildConfig.VERSION_CODE); runtime.put("versionName", BuildConfig.VERSION_NAME);
                b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('asset','runtime-identity',?,?)", JsonR27.write(runtime), last);
                assetsCached = true;
            }
            // R27-048: fetch write-rejection causes since process start (absolute counts per process; delta-added).
            for (Map.Entry<String, long[]> e : com.ethk3.novaonepro.FetchLedgerR925.rejectCountsR27().entrySet()) {
                long prev = fetchFlushed.getOrDefault(e.getKey(), 0L), delta = e.getValue()[0] - prev;
                if (delta <= 0) continue;
                b.add("INSERT OR IGNORE INTO r27_fetch_reject(reason,count,last_issue,last_at) VALUES(?,0,NULL,?)", e.getKey(), e.getValue()[1]);
                b.add("UPDATE r27_fetch_reject SET count=count+?,last_at=? WHERE reason=?", delta, e.getValue()[1], e.getKey());
                fetchFlushed.put(e.getKey(), e.getValue()[0]);
            }
            // R27-047: database size and the largest tables (measurement every hour). Archival of old-chain tables is
            // gated on the legacy switch being off after the observation period; history stays queryable.
            if (last - lastSize > 3_600_000L) { lastSize = last; b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('retention','db_size',?,?)", JsonR27.write(dbSize(w.ro)), last); }
            // wait_since bookkeeping for alarms is maintained by receipts; here we only stamp alarms for the report.
            List<WaitKindR27.Health> h = health(w.ro, last); int stuck = 0, never = 0;
            for (WaitKindR27.Health x : h) { if (x.stuck) stuck++; if (x.neverRun) never++; }
            Map<String, Object> m = new LinkedHashMap<>(); m.put("stuck", (long) stuck); m.put("neverRun", (long) never); m.put("checkedAt", last);
            b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('health','summary',?,?)", JsonR27.write(m), last);
            submit(w, "HEALTH", b);
            return ResearchPoolR27.Outcome.progress(1, last / 1000);
        }
    }

    public static List<WaitKindR27.Health> health(SQLiteDatabase db, long now) {
        List<WaitKindR27.Health> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT task_id,wait_kind,wait_detail,wait_since,registered_at,cursor,last_progress FROM r26_task", null)) {
            while (c.moveToNext()) out.add(WaitKindR27.health(c.getString(0), c.getString(1), c.getString(2), c.getLong(3), c.getLong(4), c.getLong(5), c.getLong(6), now));
        } catch (RuntimeException schemaPending) { }
        return out;
    }

    static Map<String, Object> dbSize(SQLiteDatabase db) {
        Map<String, Object> m = new LinkedHashMap<>(); long pages = 0, size = 0;
        try (Cursor c = db.rawQuery("PRAGMA page_count", null)) { if (c.moveToFirst()) pages = c.getLong(0); }
        try (Cursor c = db.rawQuery("PRAGMA page_size", null)) { if (c.moveToFirst()) size = c.getLong(0); }
        m.put("bytes", pages * size); m.put("targetBytes", 1L << 30);
        String legacy = "1"; try (Cursor c = db.rawQuery("SELECT state_value FROM app_state WHERE state_key=?", new String[] {com.ethk3.novaonepro.ResearchLearningContinuityR912.R27_LEGACY_SWITCH})) { if (c.moveToFirst()) legacy = c.getString(0); } catch (RuntimeException e) { }
        m.put("legacyResearchEnabled", legacy); m.put("archival", "1".equals(legacy) ? "PENDING_LEGACY_RETIREMENT(R27-046: parallel observation ≥1440 periods first)" : "ELIGIBLE");
        Map<String, Object> top = new LinkedHashMap<>();
        for (String t : new String[] {"research_evolution_event_v41", "r974_model_lineage", "r967_numeric_training_run", "atomic_tool_context_r943", "research_atom_context_r932", "r26_training_step", "r26_progress", "r27_policy", "r27_live_frozen", "r27_replay_score"})
            try (Cursor c = db.rawQuery("SELECT count(*) FROM " + t, null)) { if (c.moveToFirst()) top.put(t, c.getLong(0)); } catch (RuntimeException absent) { }
        m.put("rows", top); return m;
    }
}
