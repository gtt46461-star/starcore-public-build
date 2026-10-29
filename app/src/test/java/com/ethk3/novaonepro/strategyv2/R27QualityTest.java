package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import com.ethk3.novaonepro.CertificateBundleServiceR929;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import org.junit.Test;

/** docs/12 H10, H11, H13, H14, H15 (Java half; the SQL half is tools/r27/test_r27_migration.py), H16, H17 and the
 * WaitKind receipts (R27-001/002). */
public final class R27QualityTest {
    static PolicySpecR27 spec(PolicyAstR27.Node ast, int position) { return new PolicySpecR27(ast, position, PolicySpecR27.Rule.QUANTILE, 0, 0.2, 1.0, false, 0.01, 0.01, null); }
    static PolicyAstR27.Node z(PolicyAstR27.Node n) { return PolicyAstR27.op("zscore", n); }

    static List<PolicySpecR27> policies() {
        List<PolicySpecR27> l = new ArrayList<>();
        l.add(spec(z(PolicyAstR27.leaf("fdecay", 1, 32)), 1));                                                      // uses fdecay
        l.add(spec(PolicyAstR27.op("add", z(PolicyAstR27.leaf("freq", 2, 128)), z(PolicyAstR27.leaf("fdecay", 3, 8))), 2)); // uses fdecay
        l.add(spec(z(PolicyAstR27.leaf("freq", 1, 32)), 1));
        l.add(spec(z(PolicyAstR27.leaf("gap", 4)), 4));
        l.add(spec(PolicyAstR27.op("mul", PolicyAstR27.constant(0.5), z(PolicyAstR27.leaf("trans1"))), 5));
        l.add(spec(z(PolicyAstR27.leaf("cross", 2, 1)), 7));
        return l;
    }
    static boolean usesFdecay(PolicySpecR27 s) { return SemanticAbiR27.operators(s).contains("fdecay"); }

    // ------------------------------------------------------------------ H10 / H11 semantic ABI
    @Test public void h10UiOnlyChangeIsEquivalent() throws Exception {
        ReplayDatasetR27 g = R27TestSupport.golden();
        FeatureCacheR27 before = new FeatureCacheR27(g);
        Map<String, String> abiBefore = OperatorGoldenR27.fingerprints(before);
        Map<String, SemanticAbiR27.Fingerprint> fp = new LinkedHashMap<>();
        for (PolicySpecR27 s : policies()) fp.put(s.policyHash(), SemanticAbiR27.fingerprint(s, 1.0, before));
        // A "new build" that changed only UI/report/scheduling code: a fresh process recomputes everything from
        // scratch; the build identity / source hash is not an input of the fingerprint.
        FeatureCacheR27 after = new FeatureCacheR27(SemanticAbiR27.readGolden(new java.io.FileInputStream(new File(R27TestSupport.root(), "app/src/main/assets/" + SemanticAbiR27.GOLDEN_ASSET)), true));
        assertEquals(abiBefore, OperatorGoldenR27.fingerprints(after));
        int eq = 0;
        for (PolicySpecR27 s : policies()) {
            SemanticAbiR27.Result r = SemanticAbiR27.compare(fp.get(s.policyHash()), SemanticAbiR27.fingerprint(s, 1.0, after));
            assertEquals(SemanticAbiR27.EQUIVALENT, r.result); eq++;
            // The persisted rows blob round-trips (what r27_semantic_fingerprint stores).
            SemanticAbiR27.Fingerprint f = fp.get(s.policyHash());
            assertEquals(SemanticAbiR27.EQUIVALENT, SemanticAbiR27.compare(SemanticAbiR27.Fingerprint.fromBlob(f.digest, f.rowsBlob(), f.issues), f).result);
        }
        R27TestSupport.log("H10_PASS policies=" + eq + " allEquivalent=true operatorAbi=" + SemanticAbiR27.operatorSetHash(abiBefore).substring(0, 16));
    }

    @Test public void h11ChangedFdecayChangesOnlyItsPolicies() throws Exception {
        ReplayDatasetR27 g = R27TestSupport.golden();
        FeatureCacheR27 before = new FeatureCacheR27(g);
        Map<String, String> abiBefore = OperatorGoldenR27.fingerprints(before);
        Map<String, SemanticAbiR27.Fingerprint> fp = new LinkedHashMap<>();
        for (PolicySpecR27 s : policies()) fp.put(s.policyHash(), SemanticAbiR27.fingerprint(s, 1.0, before));
        FeatureCacheR27 after = new FeatureCacheR27(g);
        // The changed formula: the exponential decay uses twice the half-life.
        after.override("fdecay", (f, tp, a, b) -> f.decay(a, b * 2));
        Map<String, String> abiAfter = OperatorGoldenR27.fingerprints(after);
        List<String> changedOps = new ArrayList<>();
        for (String k : abiBefore.keySet()) if (!abiBefore.get(k).equals(abiAfter.get(k))) changedOps.add(k);
        assertEquals(Collections.singletonList("fdecay"), changedOps);
        int changed = 0, equivalent = 0; StringBuilder first = new StringBuilder();
        for (PolicySpecR27 s : policies()) {
            SemanticAbiR27.Result r = SemanticAbiR27.compare(fp.get(s.policyHash()), SemanticAbiR27.fingerprint(s, 1.0, after));
            if (usesFdecay(s)) {
                assertEquals(SemanticAbiR27.CHANGED, r.result);
                // First difference: recompute the first row where the leaf values differ and check the report names it.
                double[] a = before.leaf("fdecay", s.position, s.position == 1 ? 1 : 3, s.position == 1 ? 32 : 8), b = after.leaf("fdecay", s.position, s.position == 1 ? 1 : 3, s.position == 1 ? 32 : 8);
                int t0 = -1; for (int t = 0; t < g.size() && t0 < 0; t++) for (int k = 0; k < 10; k++) { double x = a[t * 10 + k], y = b[t * 10 + k]; if (!(Double.isNaN(x) && Double.isNaN(y)) && Double.doubleToLongBits(x) != Double.doubleToLongBits(y)) { t0 = t; break; } }
                assertTrue(t0 >= 0);
                assertEquals("probabilities", r.firstDiffField);
                assertEquals(g.issues[t0], r.firstDiffIssue);
                first.append(s.policyHash(), 0, 8).append('@').append(r.firstDiffIssue).append('/').append(r.firstDiffField).append(' ');
                changed++;
            } else { assertEquals(SemanticAbiR27.EQUIVALENT, r.result); equivalent++; }
        }
        assertEquals(2, changed); assertEquals(4, equivalent);
        R27TestSupport.log("H11_PASS changedOperators=" + changedOps + " changedPolicies=" + changed + " equivalentPolicies=" + equivalent + " firstDiff=" + first.toString().trim());
    }

    // ------------------------------------------------------------------ H13 calibration rows
    @Test public void h13LateRowsAreExcludedNotFatal() {
        StatsR27.SplitMix r = new StatsR27.SplitMix(13);
        List<CalibrationRowsR27.Row> rows = new ArrayList<>(); int late = 0, gap = 0;
        for (int i = 0; i < 400; i++) {
            String src = i < 150 ? "REPLAY" : "FUTURE"; double score = r.nextDouble() * 2 - 1; int hit = r.nextDouble() < 0.3 + 0.1 * score ? 1 : 0;
            long open = 1_000_000L + i * 60_000L, frozen = open - 5_000;
            boolean isLate = src.equals("FUTURE") && r.nextDouble() < 0.05, isGap = r.nextDouble() < 0.02;
            if (isLate) { frozen = open + 1_000; late++; }
            if (isGap) gap++;
            rows.add(new CalibrationRowsR27.Row("I" + i, src, frozen, open, score, isGap ? -1 : hit, 1 + i % 10, !isGap, true));
        }
        CalibrationRowsR27.Result res = CalibrationRowsR27.fit(rows);
        assertTrue(late > 0);
        assertEquals("FITTED", res.state);
        assertEquals(late, res.excludedLate); assertEquals(gap, res.excludedGap);
        assertEquals(120, res.fitRows); assertEquals(120, res.validationRows);
        assertTrue(Double.isFinite(res.ece) && Double.isFinite(res.a));
        // Too few rows: still a specific reason, never a blanket BLOCKED_INVALID_INPUT.
        CalibrationRowsR27.Result few = CalibrationRowsR27.fit(rows.subList(0, 100));
        assertEquals("COLLECTING", few.state); assertTrue(few.reason.startsWith(CalibrationRowsR27.INSUFFICIENT_ROWS)); assertFalse(few.reason.contains("BLOCKED_INVALID_INPUT"));
        R27TestSupport.log("H13_PASS injectedLate=" + late + " excludedLate=" + res.excludedLate + " excludedGap=" + res.excludedGap + " state=" + res.state + " ece=" + res.ece + " fitSource=" + res.fitSource);
    }

    // ------------------------------------------------------------------ H14 matrix consistency
    @Test public void h14HundredSurfacesTwelveHundredActions() {
        StatsR27.SplitMix r = new StatsR27.SplitMix(14); double worst = 0;
        for (int i = 0; i < 100; i++) {
            double[] p = new double[10]; double s = 0; for (int k = 0; k < 10; k++) { p[k] = r.nextDouble() + 1e-3; s += p[k]; } for (int k = 0; k < 10; k++) p[k] /= s;
            double[][] surf = MatrixConsistencyR27.surface(1 + i % 10, p);
            double e = MatrixConsistencyR27.error(surf, MatrixConsistencyR27.actions(surf)); worst = Math.max(worst, e);
            int[] top = MatrixConsistencyR27.rank3(surf, 1 + i % 10);
            for (int k = 0; k < 10; k++) if (k + 1 != top[0] && k + 1 != top[1] && k + 1 != top[2]) assertTrue(p[k] <= p[top[2] - 1]);
        }
        assertTrue("max error " + worst, worst < MatrixConsistencyR27.TOLERANCE);
        // A corrupted action is detected.
        double[][] surf = MatrixConsistencyR27.surface(1, new double[] {.1, .1, .1, .1, .1, .1, .1, .1, .1, .1}); double[] a = MatrixConsistencyR27.actions(surf); a[17] += 1e-6;
        assertTrue(MatrixConsistencyR27.error(surf, a) > MatrixConsistencyR27.TOLERANCE);
        R27TestSupport.log("H14_PASS surfaces=100 actions=1200 maxError=" + worst);
    }

    // ------------------------------------------------------------------ H15 main-chain red lines (Java half)
    static final String[][] MAINLINE_OWNERS = {
        {"app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java", "edd7edb47b087dd3053ca98540461e1c4a4b7031c5314036e5a1356a5f688d91"},
        {"app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java", "a978b689f0c6d5eafcab5279bf18794e7e573f6f2cf59a74985a55f22822693f"},
        {"app/src/main/java/com/ethk3/novaonepro/V51DecisionGateCore.java", "63341a9d65087cff33d441a7ced63232c5f6b7d5680113d6fed3afe522f7cce4"},
        {"app/src/main/java/com/ethk3/novaonepro/CertificateBundleServiceR929.java", "7e07152d224e4342e4a7f95e200f52fd6b12c5bde2022c19393437c440b436e2"},
        {"app/src/main/java/com/ethk3/novaonepro/AppConfig.java", "76600928570ce1dfb2425304202af3ae7b6d6045f4519c3b0ef0df76472b31e9"},
        {"app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterSU2.java", "e3625ddf0815343d54ac19daba96a78f9ded2a7d837f05ece321fb60f447a0a9"},
        {"app/src/main/java/com/ethk3/novaonepro/DbStore.java", "9edd4e0f593eeee607138543869df8a3e2e8d47a862988f154cb2d632ce9c78a"},
    };

    @Test public void h15MainlineRedLines() throws Exception {
        File root = R27TestSupport.root();
        // 1. The REAL / settlement owners are byte-identical to the 1713d11 baseline (rules unchanged, not tightened, not relaxed).
        for (String[] f : MAINLINE_OWNERS) assertEquals("main-chain owner changed: " + f[0], f[1], StatsR27.sha256(Files.readAllBytes(new File(root, f[0]).toPath())));
        // 2. No R27 class can create a REAL decision, settle, or size a stake: no reference to the owners' entry points.
        String[] forbidden = {"DecisionGateEngine", "SettlementEngine", "V51DecisionGateCore", "insertTrade", "insertBet", "placeBet", "stake", "multiplier", "martingale", "chase"};
        File dir = new File(root, "app/src/main/java/com/ethk3/novaonepro/strategyv2"); int scanned = 0;
        for (File f : Objects.requireNonNull(dir.listFiles())) {
            if (!f.getName().contains("R27")) continue; scanned++;
            String src = new String(Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "").replaceAll("\"(?:[^\"\\\\]|\\\\.)*\"", "\"\""); // code only: comments and UI text removed
            for (String w : forbidden) assertFalse(f.getName() + " references " + w, src.contains(w));
        }
        // 3. Certificate service of the existing chain: no certificate / expired certificate are denied; never mayCreateReal.
        CertificateBundleServiceR929.CertificateService svc = new CertificateBundleServiceR929.CertificateService();
        String h = com.ethk3.novaonepro.PitFeatureDagR929.sha256("x");
        CertificateBundleServiceR929.CandidateKey key = new CertificateBundleServiceR929.CandidateKey(CertificateBundleServiceR929.Consumer.values()[0], "ns", "family", "R27", h);
        CertificateBundleServiceR929.Authorization none = svc.authorize(key, "202610280100", h, h, h, h, 0.05);
        assertFalse(none.authorized); assertFalse(none.mayCreateReal);
        // 4. R27 certificate gates: no evidence / missing gate → not ACTIVE; numeric gates exactly the current ones.
        assertEquals(0.34, CertificateGateR27.MIN_HR, 0); assertEquals(250, CertificateGateR27.MIN_ACTS); assertEquals(0.04, CertificateGateR27.MAX_ECE, 0);
        assertEquals(0.26, CertificateGateR27.MIN_WORST_BLOCK, 0); assertEquals(6, CertificateGateR27.MAX_LOSS_STREAK);
        CertificateGateR27.Evidence e = new CertificateGateR27.Evidence(); e.policyHash = "p";
        assertFalse(CertificateGateR27.evaluate(e).active);
        e.sequentialState = SequentialQualificationR27.QUALIFIED; e.acts = 300; e.hits = 105; e.ece = 0.03; e.worstBlock = 0.3; e.maxLosingStreak = 5; e.abiEquivalent = true; e.pairedLcbVsI00 = 0.01; e.pairedLcbVsNull = 0.01;
        assertTrue(CertificateGateR27.evaluate(e).active);
        e.hits = 101; assertFalse("33.7% < 34%", CertificateGateR27.evaluate(e).active); e.hits = 105;
        e.maxLosingStreak = 7; assertFalse(CertificateGateR27.evaluate(e).active); e.maxLosingStreak = 5;
        e.abiEquivalent = false; assertFalse("semantic ABI changed → certificate not valid", CertificateGateR27.evaluate(e).active); e.abiEquivalent = true;
        e.sequentialState = SequentialQualificationR27.ACCUMULATING; assertFalse("REPLAY/HOLDOUT never substitute FUTURE evidence", CertificateGateR27.evaluate(e).active);
        assertNull("no ACTIVE certificate → PRODUCTION_FALLBACK", CertificateGateR27.choose(Collections.<CertificateGateR27.Evidence>emptyList()));
        assertTrue(CertificateGateR27.VALID_MS > 0);
        R27TestSupport.log("H15_JAVA_PASS ownersUnchanged=" + MAINLINE_OWNERS.length + " r27FilesScanned=" + scanned + " noCertificateDenied=" + none.reason);
    }

    // ------------------------------------------------------------------ H16 AtomNet planted vs null
    // docs/06 6.2/6.3 shape: 7 group inputs; history pre-training on ~5000 periods x 10 positions, early stop on
    // H_SELECT, then FUTURE samples scored before their label is learned (predict, then one online step).
    static final int H16_DIM = 7, H16_TRAIN = 40_000, H16_SELECT = 5_000, H16_FUTURE = 3_000;
    static List<AtomNetTrainerR27.Sample> samples(int n, boolean planted, long seed) {
        StatsR27.SplitMix r = new StatsR27.SplitMix(seed); List<AtomNetTrainerR27.Sample> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double[] x = new double[H16_DIM]; for (int k = 0; k < H16_DIM; k++) x[k] = r.nextDouble() * 2 - 1;
            double p = planted ? 0.3 + 0.1 * Math.signum(x[0]) : 0.3;
            out.add(new AtomNetTrainerR27.Sample(x, r.nextDouble() < p ? 1 : 0, r.nextInt(10), "S" + i));
        }
        return out;
    }
    static Map<String, Object> bss(boolean planted, long seed) {
        List<AtomNetTrainerR27.Sample> all = samples(H16_TRAIN + H16_SELECT + H16_FUTURE, planted, seed);
        AtomNetTrainerR27 t = new AtomNetTrainerR27(H16_DIM, seed);
        t.pretrain(all.subList(0, H16_TRAIN), all.subList(H16_TRAIN, H16_TRAIN + H16_SELECT));
        List<AtomNetTrainerR27.Sample> fut = all.subList(H16_TRAIN + H16_SELECT, all.size()); double[] p = new double[fut.size()]; int[] y = new int[fut.size()];
        for (int i = 0; i < p.length; i++) { p[i] = t.net.predict(fut.get(i).x); y[i] = fut.get(i).y; t.online(fut.get(i)); } // predict before learning the label
        return BrierSkillR27.summary(p, y);
    }
    @Test public void h16AtomNetFindsSignalAndDoesNotInventOne() {
        StringBuilder log = new StringBuilder();
        for (long seed = 1; seed <= 3; seed++) {
            Map<String, Object> pl = bss(true, seed), nu = bss(false, seed);
            assertEquals("planted seed " + seed + ": " + pl, "BETTER_THAN_BASELINE", pl.get("verdict")); assertTrue((Double) pl.get("bandLow") > 0);
            assertEquals("null band must contain 0, seed " + seed + ": " + nu, "NOT_DISTINGUISHABLE_FROM_BASELINE", nu.get("verdict"));
            log.append(" seed").append(seed).append(":planted[bss=").append(pl.get("bss")).append(",low=").append(pl.get("bandLow")).append("] null[bss=").append(nu.get("bss")).append(",low=").append(nu.get("bandLow")).append(",high=").append(nu.get("bandHigh")).append(']');
        }
        assertEquals("INSUFFICIENT", BrierSkillR27.verdict(BrierSkillR27.cumulative(new double[150], new int[150])));
        R27TestSupport.log("H16_PASS" + log);
    }

    // ------------------------------------------------------------------ H17 report on empty data
    @Test public void h17EmptyReportIsInsufficientWithoutPass() {
        ReportR27.Input in = new ReportR27.Input();
        List<Map<String, Object>> ch = Arrays.asList(ReportR27.truth(in), ReportR27.evolution(in), ReportR27.runtime(in), ReportR27.upgrade(in));
        for (Map<String, Object> c : ch) assertEquals(c.get("sectionId") + "", "INSUFFICIENT", c.get("status"));
        assertEquals(ReportR27.T_INSUFFICIENT, ch.get(0).get("conclusionTemplate"));
        String md = ReportR27.markdown(ch), json = JsonR27.write(ch);
        assertFalse(md.contains("PASS")); assertFalse(json.contains("PASS"));
        // The other two templates are chosen by data only.
        ReportR27.Input q = new ReportR27.Input(); ReportR27.Shadow s = new ReportR27.Shadow(); s.id = "a"; s.status = "ACTIVE"; s.n = 900; s.acts = 300; s.hits = 110; s.state = SequentialQualificationR27.QUALIFIED; q.shadows.add(s);
        assertEquals(ReportR27.T_QUALIFIED, ReportR27.truth(q).get("conclusionTemplate"));
        s.state = SequentialQualificationR27.ACCUMULATING; s.hits = 90;
        Map<String, Object> none = ReportR27.truth(q); assertEquals(ReportR27.T_NONE, none.get("conclusionTemplate"));
        assertFalse(JsonR27.write(none).contains("PASS"));
        R27TestSupport.log("H17_PASS chapters=" + ch.size() + " template=" + ch.get(0).get("conclusionTemplate") + " otherTemplates=QUALIFIED,NONE");
    }

    // ------------------------------------------------------------------ R27.1 continuation quantum (emulator run 40790353126)
    @Test public void continuationSlicesUseAnAcceptedQuantum() throws Exception {
        try (CooperativeContinuationR26<Long> c = new CooperativeContinuationR26<>("t", () -> 7L, () -> false, () -> { }, () -> 0L)) {
            CooperativeContinuationR26.Slice<Long> s = null;
            for (int i = 0; i < 50 && (s == null || !s.done); i++) s = c.slice(CooperativeContinuationR26.CONTINUATION_SLICE_MS);
            assertTrue(s.done); assertEquals(7L, (long) s.result);
        }
        try (CooperativeContinuationR26<Long> c = new CooperativeContinuationR26<>("t", () -> 7L, () -> false, () -> { }, () -> 0L)) {
            try { c.slice(CooperativeContinuationR26.SLICE_MS); fail("SLICE_MS is a scheduler budget, not a continuation quantum"); }
            catch (IllegalArgumentException expected) { assertEquals("R26_CONTINUATION_QUANTUM", expected.getMessage()); }
        }
        File dir = new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro");
        java.util.List<File> all = new ArrayList<>(); java.util.Deque<File> q = new java.util.ArrayDeque<>(); q.add(dir);
        while (!q.isEmpty()) { File f = q.poll(); File[] k = f.listFiles(); if (k == null) continue; for (File x : k) if (x.isDirectory()) q.add(x); else if (x.getName().endsWith(".java")) all.add(x); }
        int sites = 0;
        for (File f : all) {
            String src = new String(Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\.slice\\(([^)]*)\\)").matcher(src);
            while (m.find()) {
                String arg = m.group(1).trim(); if (arg.isEmpty() || arg.startsWith("long ")) continue;
                sites++;
                assertFalse(f.getName() + " passes SLICE_MS to slice()", arg.endsWith("SLICE_MS") && !arg.endsWith("CONTINUATION_SLICE_MS"));
                if (arg.matches("[0-9]+")) assertTrue(f.getName() + " slice(" + arg + ") > 200", Long.parseLong(arg) <= 200);
            }
        }
        assertTrue(sites >= 3);
        R27TestSupport.log("R27_1_CONTINUATION_QUANTUM_PASS callSites=" + sites);
    }

    // ------------------------------------------------------------------ R27.1 writer priority (10.29 device report)
    @Test public void r27ResearchWritesYieldToCriticalBusiness() throws Exception {
        Class<?> dc = Class.forName("com.ethk3.novaonepro.DatabaseCoordinator");
        java.lang.reflect.Method bg = dc.getDeclaredMethod("isBackgroundOperationR909", String.class), crit = dc.getDeclaredMethod("isCriticalBusinessOperationR914", String.class);
        bg.setAccessible(true); crit.setAccessible(true);
        for (String op : new String[] {"R27_EVOLUTION_STEP", "R27_PRIORITY_CLAIM", "R27_WRITE_WAIT", "R27_LIVE_FREEZE", "R26_STRATEGY_STORAGE_UNIT"})
            assertTrue(op + " must be background (yields to critical writers)", (Boolean) bg.invoke(null, op));
        for (String op : new String[] {"R925_FETCH_BEGIN", "R925_FETCH_PAYLOAD", "R925_FETCH_COMPLETE", "R914_DRAW_HISTORY_BATCH", "R914_DRAW_SAVE_ATOMIC"}) {
            assertTrue(op + " stays critical", (Boolean) crit.invoke(null, op));
            assertFalse(op + " is not background", (Boolean) bg.invoke(null, op));
        }
        R27TestSupport.log("R27_1_WRITER_PRIORITY_PASS");
    }

    // ------------------------------------------------------------------ R27.2 second device report (3b84648, V2353A)
    @Test public void r27BusyWriterIsAWaitNotATaskError() throws Exception {
        Throwable busy = new IllegalStateException("F01_WRITE_FAILED|operation=R27_EVOLUTION_STEP_HOLDOUT", new IllegalStateException("CP3_DB_BUSY_DEADLINE"));
        Throwable afterAcquire = new IllegalStateException("R26_STRATEGY_SCHEMA", new IllegalStateException("F01_WRITE_FAILED|operation=R26_STRATEGY_STORAGE_UNIT", new IllegalStateException("CP3_DB_BUSY_DEADLINE_AFTER_ACQUIRE")));
        Throwable real = new IllegalStateException("F01_WRITE_FAILED|operation=R27_LIVE_SETTLE", new RuntimeException("UNIQUE constraint failed: r27_live_shadow.id"));
        assertTrue(ResearchWriteQueueR27.writerBusy(busy));
        assertTrue(ResearchWriteQueueR27.writerBusy(afterAcquire));
        assertFalse(ResearchWriteQueueR27.writerBusy(real));
        assertFalse(ResearchWriteQueueR27.writerBusy(new IllegalStateException("R27_WRITE_PRECONDITION:LIVE_FREEZE")));
        File dir = new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro/strategyv2");
        String queue = new String(Files.readAllBytes(new File(dir, "ResearchWriteQueueR27.java").toPath()), "UTF-8");
        assertTrue("busy writer re-queues instead of throwing", queue.contains("if (!writerBusy(failure)) throw failure;"));
        String coordinator = new String(Files.readAllBytes(new File(dir, "RuntimeCoordinatorR26.java").toPath()), "UTF-8");
        assertTrue("R26 slice: busy writer is cooperative", coordinator.contains("ResearchWriteQueueR27.writerBusy(failure))reason=\"COOPERATIVE_WRITER_BUSY\""));
        assertTrue("startup releases busy-only quarantines", coordinator.contains("message LIKE 'F01_WRITE_FAILED%' AND stack LIKE '%CP3_DB_BUSY_DEADLINE%'"));
        R27TestSupport.log("R27_2_BUSY_WRITER_PASS");
    }

    @Test public void r27ComputeHelpersRunAtBackgroundPriorityAndNarrowDuringP0() throws Exception {
        File dir = new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro/strategyv2");
        String pool = new String(Files.readAllBytes(new File(dir, "ResearchPoolR27.java").toPath()), "UTF-8");
        int factory = pool.indexOf("\"starcore-r27-compute\""); assertTrue(factory > 0);
        String line = pool.substring(pool.lastIndexOf("compute = new ThreadPoolExecutor", factory), factory);
        assertTrue("compute helpers set the background priority", line.contains("setThreadPriority(BACKGROUND_PRIORITY)"));
        assertEquals(android.os.Process.THREAD_PRIORITY_BACKGROUND + 1, ResearchPoolR27.BACKGROUND_PRIORITY);
        assertTrue("one-worker floor also while P0 runs", pool.contains("narrow(System.currentTimeMillis()) && active.get() >= 1"));
        assertTrue(pool.contains("return writes.freezeWindow(now) || ResearchWriteQueueR27.p0Writing();"));
        String evo = new String(Files.readAllBytes(new File(dir, "EvolutionLoopR27.java").toPath()), "UTF-8");
        assertTrue("replay is serial while P0 runs", evo.contains("if (executor == null || (serialWhile != null && serialWhile.getAsBoolean()))"));
        String rt = new String(Files.readAllBytes(new File(dir, "RuntimeR27.java").toPath()), "UTF-8");
        assertTrue(rt.contains("evo.serialWhile = ResearchWriteQueueR27::p0Writing;"));
        R27TestSupport.log("R27_2_RESEARCH_PRIORITY_PASS");
    }

    // ------------------------------------------------------------------ R27.3 third device report (3cc41ff, fresh install)
    @Test public void r27BackgroundWritersNeverQueueAheadOfCriticalWriters() throws Exception {
        String src = new String(Files.readAllBytes(new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java").toPath()), "UTF-8");
        int gate = src.indexOf("if (backgroundR909 && !reentrantWriterR914 && criticalBusinessWaitersR914.get() > 0) {");
        int lock = src.indexOf("if (writeLock.tryLock(Math.min(backgroundR909 ? 10L : 50L, remainingCP3), TimeUnit.MILLISECONDS)) {");
        assertTrue("background writers wait outside the writer queue while a critical writer waits", gate > 0 && lock > gate && lock - gate < 800);
        assertTrue(src.indexOf("CP3_DB_BUSY_DEADLINE\")") < gate);
        String report = new String(Files.readAllBytes(new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java").toPath()), "UTF-8");
        assertTrue("report carries app thread stacks", report.contains("collectObservation(root,id,observation,\"threadStacks\",ReportExportServiceCP4::threadStacksR27)"));
        R27TestSupport.log("R27_3_CRITICAL_FIRST_WRITER_PASS");
    }

    @Test public void r27ControlLooksDoNotSpendTheHoldoutBudget() throws Exception {
        HoldoutLedgerR27 l = new HoldoutLedgerR27();
        for (int g = 0; g < 500; g++) { l.lookControl(7, "i00-" + g); l.lookControl(7, "null-" + g); }
        assertFalse("regenerated controls alone never degrade a block", l.degraded(7));
        assertEquals(1000, l.controlExposures(7));
        for (int i = 0; i <= HoldoutLedgerR27.MAX_DISTINCT_LOOKS; i++) l.look(7, "champion-" + i);
        assertTrue("main-line looks still spend the budget", l.degraded(7));
        String evo = new String(Files.readAllBytes(new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java").toPath()), "UTF-8");
        assertTrue(evo.contains("look(i00Champion, i00Tau, null, true)"));
        assertTrue(evo.contains("nullEvolution.labels(ctx.cache, sp), true)"));
        String frame = new String(Files.readAllBytes(new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro/strategyv2/EventFrameBuilderSU1.java").toPath()), "UTF-8");
        assertTrue("SU1 frames use the newest contiguous run", frame.contains("pit=pit.newestContiguous();pit.requireConsecutive();"));
        R27TestSupport.log("R27_3_HOLDOUT_BUDGET_PASS");
    }

    @Test public void r27SettlementNeverRunsTheWholeResearchMigration() throws Exception {
        String reg = new String(Files.readAllBytes(new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro/r952/ResearchMigrationRegistryR952.java").toPath()), "UTF-8");
        int e = reg.indexOf("public static void ensure(SQLiteDatabase db) {");
        int gate = reg.indexOf("SettlementExecutionGateR819.ownedByCurrentThread()", e);
        int loop = reg.indexOf("while (!advanceR64(db))", e);
        assertTrue("settlement thread is checked before the full migration loop", e > 0 && gate > e && loop > gate);
        assertTrue(reg.substring(gate, loop).contains("R952_SCHEMA_PENDING_DEFERRED_FROM_SETTLEMENT"));
        Class<?> g = Class.forName("com.ethk3.novaonepro.SettlementExecutionGateR819");
        java.lang.reflect.Method enter = g.getDeclaredMethod("tryEnter"), exit = g.getDeclaredMethod("exit"), owned = g.getDeclaredMethod("ownedByCurrentThread");
        assertFalse((Boolean) owned.invoke(null));
        assertTrue((Boolean) enter.invoke(null));
        try {
            assertTrue((Boolean) owned.invoke(null));
            final boolean[] other = {true};
            Thread t = new Thread(() -> { try { other[0] = (Boolean) owned.invoke(null); } catch (Exception x) { throw new RuntimeException(x); } });
            t.start(); t.join();
            assertFalse("another thread does not own the gate", other[0]);
        } finally { exit.invoke(null); }
        R27TestSupport.log("R27_3_SETTLEMENT_SCHEMA_DEFERRAL_PASS");
    }

    // ------------------------------------------------------------------ R27-005 no session-level CRITICAL_DEADLINE yield
    @Test public void r27005ResearchSessionNoLongerYieldsToDeadlines() throws Exception {
        String src = new String(Files.readAllBytes(new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java").toPath()), "UTF-8");
        int i = src.indexOf("private boolean preempt()"); assertTrue(i >= 0);
        String preempt = src.substring(i, src.indexOf('}', i) + 1);
        // 1713d11: preempt() also returned true while SettlementExecutionGateR819 or RealtimePressureGateR811 had critical
        // work, which parked the whole research session as BLOCKED:CRITICAL_DEADLINE. R27: only cancel/interrupt/resource.
        assertEquals("private boolean preempt(){return cancelled||Thread.currentThread().isInterrupted()||resourceBlocked!=null;}", preempt);
        assertFalse(src.contains("RealtimePressureGateR811"));
        R27TestSupport.log("R27-005_PASS preempt=" + preempt);
    }

    // ------------------------------------------------------------------ R27-001/002 WaitKind receipts
    @Test public void waitKindsAreSpecific() {
        assertEquals(WaitKindR27.PROGRESS, WaitKindR27.classify(3, null, null, 0, 10, null, false).kind);
        assertEquals(WaitKindR27.NO_NEW_EVENT, WaitKindR27.classify(0, null, null, 10, 10, null, false).kind);
        assertEquals(WaitKindR27.DEPENDENCY, WaitKindR27.classify(0, "WAITING_TEACHER", null, 0, 10, null, false).kind);
        assertEquals(WaitKindR27.DEADLINE_MISSED, WaitKindR27.classify(0, "FROZEN_AFTER_OPEN", null, 0, 10, null, false).kind);
        assertEquals(WaitKindR27.RESOURCE, WaitKindR27.classify(0, null, null, 0, 10, "THERMAL", false).kind);
        WaitKindR27.Wait err = WaitKindR27.classify(0, null, new IllegalStateException("boom"), 0, 10, null, false);
        assertEquals(WaitKindR27.ERROR, err.kind); assertTrue(err.detail.contains("IllegalStateException:boom"));
        assertEquals(WaitKindR27.DISABLED_BY_USER, WaitKindR27.classify(5, null, null, 0, 10, null, true).kind);
        long[] w = {0, 0}; long now = 1_800_000_000_000L;
        for (int i = 0; i < 6; i++) w = WaitKindR27.recordError(w[0], w[1], now + i * 1000);
        assertTrue(WaitKindR27.quarantine(w[1]));
        assertTrue(WaitKindR27.health("t", WaitKindR27.DEPENDENCY, "x", now - WaitKindR27.STUCK_MS, now - 1, 5, 5, now).stuck);
        assertTrue(WaitKindR27.health("t", "", "", 0, now - WaitKindR27.NEVER_RUN_MS, 0, 0, now).neverRun);
        assertFalse(WaitKindR27.kinds().contains("CRITICAL_DEADLINE"));
        R27TestSupport.log("WAITKIND_PASS kinds=" + WaitKindR27.kinds());
    }
}
