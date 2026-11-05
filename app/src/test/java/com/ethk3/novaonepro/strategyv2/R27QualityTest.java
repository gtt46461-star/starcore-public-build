package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import com.ethk3.novaonepro.CertificateBundleServiceR929;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.RealtimePressureGateR811;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
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
        try (FeatureCacheR27 before = new FeatureCacheR27(g)) {
        Map<String, String> abiBefore = OperatorGoldenR27.fingerprints(before);
        Map<String, SemanticAbiR27.Fingerprint> fp = new LinkedHashMap<>();
        for (PolicySpecR27 s : policies()) fp.put(s.policyHash(), SemanticAbiR27.fingerprint(s, 1.0, before));
        // A "new build" that changed only UI/report/scheduling code: a fresh process recomputes everything from
        // scratch; the build identity / source hash is not an input of the fingerprint.
        ReplayDatasetR27 reread;
        try (java.io.InputStream input = new java.io.FileInputStream(new File(R27TestSupport.root(), "app/src/main/assets/" + SemanticAbiR27.GOLDEN_ASSET))) {
            reread = SemanticAbiR27.readGolden(input, true);
        }
        try (FeatureCacheR27 after = new FeatureCacheR27(reread)) {
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
        }
    }

    @Test public void h11ChangedFdecayChangesOnlyItsPolicies() throws Exception {
        ReplayDatasetR27 g = R27TestSupport.golden();
        try (FeatureCacheR27 before = new FeatureCacheR27(g)) {
        Map<String, String> abiBefore = OperatorGoldenR27.fingerprints(before);
        Map<String, SemanticAbiR27.Fingerprint> fp = new LinkedHashMap<>();
        for (PolicySpecR27 s : policies()) fp.put(s.policyHash(), SemanticAbiR27.fingerprint(s, 1.0, before));
        try (FeatureCacheR27 after = new FeatureCacheR27(g)) {
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
        }
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

    /** R31-B03-C03 moves this one research producer to confirmed-draw ingress. Reconstruct
     * its exact former bytes solely for the original H15 owner hash; no PnL/Gate/REAL code
     * is excluded from comparison and the expected pre-R31 hash remains unchanged. */
    static String restoreR31FeedbackExtractionForH15(String source) {
        String oldCall="                                            com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26.feedbackWithinTransaction(db, draw);\n";
        assertFalse("Core settlement must no longer depend on research feedback",source.contains("RuntimeSchemaR26.feedbackWithinTransaction("));
        String anchor="                                            RuntimeEventHooksR926.settledWithinTransaction(\n"
                +"                                                    db, draw.issue, settlementHash, terminalStatus, claim.drawHash);\n"
                +"                                            SettlementReport report = new SettlementReport();\n";
        assertEquals("Only the exact registered feedback-extraction site is allowed",1,source.split(java.util.regex.Pattern.quote(anchor),-1).length-1);
        return source.replace(anchor,anchor.replace("                                            SettlementReport report",oldCall+"                                            SettlementReport report"));
    }

    /** Each permitted schema/sidecar block is independently pinned; the rest of DbStore
     * still must reproduce the original 9edd4e0f whole-file hash. The baseline is the
     * immutable plan fixture, not a replacement production implementation. */
    static String restoreR31DbSchemaSidecarsForH15(String source,String baseline) {
        assertEquals("Original DbStore fixture must itself retain the old owner identity",
                "9edd4e0f593eeee607138543869df8a3e2e8d47a862988f154cb2d632ce9c78a",
                StatsR27.sha256(baseline.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String[][] blocks={
            {"    private static final java.util.Map<DatabaseCoordinator,LegacySchemaFlightR31> LEGACY_SCHEMA_FLIGHTS_R31=","    public java.io.InputStream openResearchAssetCP12(","7e9b5721810883f8776e5d1dd22339bdf983669e12bcf034b7c7e7bbcdb772f1","INSERTION"},
            {"    private void ensureRuntimeExtraTablesOnce(final SQLiteDatabase db) {","    private void verifyLegacyRuntimeTablesR363(","6b3f167e263c8bc81ec53ae5746431e3137c8b9077770e7e94f39ca240001cc5","REPLACE"},
            {"    private void ensureMainChainClosedLoopReady(SQLiteDatabase db) {","    private void ensureMainChainColumns(","97c5e3abe28683190a2c92ba10413490faa5ed9b16b8c87e78e221fe8b3c7158","REPLACE"},
            {"    public int saveMainChainCandidateSnapshot(","    public int saveRealtimeTop40Candidates(","e83ca46b42aa0b524437a15d4a5dea2ba0f1648eebfe9938bda1cd1a87f74419","REPLACE"},
            {"    private void recordMainChainRejectBucket(","    private void putFinalVersionFallback(","5ae257ee60cfc081b1baa1ca2296b007e21ef21b31f8723afce780261f476b52","REPLACE"},
            // R31 P01: startup-only R929 runtime-bundle schema prerequisite; no main-chain decision, payout or gate bytes.
            {"        // The first bundle capture can precede asynchronous research-schema initialization.\n","        // Establish the R816 diagnostic boundary","f97ec7d2dcb9ac6902b4bbcc485e8b9d970b6f8095b15efe42ad996834b182e9","INSERTION_INLINE"}
        };
        for(String[] block:blocks) {
            int start=source.indexOf(block[0]),end=source.indexOf(block[1],start+block[0].length());
            assertTrue("Approved block start/end must exist: "+block[0],start>=0&&end>start);
            assertEquals("Approved block start must be unique",start,source.lastIndexOf(block[0]));
            assertEquals("Only exact reviewed schema/sidecar bytes are allowed: "+block[0],block[2],
                    StatsR27.sha256(source.substring(start,end).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            String original="INSERTION_INLINE".equals(block[3])?"":"\n"; // the inserted flight occupies the original blank separator
            if(!"INSERTION".equals(block[3])&&!"INSERTION_INLINE".equals(block[3])) {
                int oldStart=baseline.indexOf(block[0]),oldEnd=baseline.indexOf(block[1],oldStart+block[0].length());
                assertTrue("Original block must exist",oldStart>=0&&oldEnd>oldStart);
                original=baseline.substring(oldStart,oldEnd);
            }
            source=source.substring(0,start)+original+source.substring(end);
        }
        for(String method:new String[]{"mainChainClosedLoopColumnDefs","mainChainFactorColumnDefs"}) {
            String signature="    static String[] "+method+"() {";
            assertEquals("Only package-static exposure, never a column definition change",1,source.split(java.util.regex.Pattern.quote(signature),-1).length-1);
            source=source.replace(signature,"    private String[] "+method+"() {");
        }
        String oldShadow="            SQLiteDatabase db = writable();\n            ensureRuntimeExtraTablesOnce(db);\n\n            db.beginTransaction();";
        String newShadow="            SQLiteDatabase db = writable();\n"
                +"            // The snapshot and SHADOW path share the same read-only contract.\n"
                +"            // A first business call must not run the lazy Legacy DDL initializer.\n"
                +"            ensureMainChainClosedLoopReady(db);\n\n            db.beginTransaction();";
        assertEquals("Only the four-argument SHADOW readiness call is replaced",1,source.split(java.util.regex.Pattern.quote(newShadow),-1).length-1);
        return source.replace(newShadow,oldShadow);
    }

    /** The two literal-body alternatives are disjoint. Possessive repetition preserves the
     * matched language/output while avoiding one recursive regex frame per source character. */
    static String stripJavaStringsR31(String source) {
        return source.replaceAll("\"(?:[^\"\\\\]|\\\\.)*+\"", "\"\"");
    }

    @Test public void h15MainlineRedLines() throws Exception {
        File root = R27TestSupport.root();
        // 1. Original bytes match after the registered R30 wrappers and exact R31 research extraction.
        for (String[] f : MAINLINE_OWNERS) {
            byte[] bytes=Files.readAllBytes(new File(root,f[0]).toPath());
            if(f[0].endsWith("/DecisionGateEngine.java")) {
                String source=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
                assertEquals("Only two registered production entry wrappers",2,source.split("R30_READONLY_BEGIN:",-1).length-1);
                source=source.replaceAll("(?s)    // R30_READONLY_BEGIN:[^\\n]*\\n.*?    // R30_READONLY_END\\n\\n", "");
                source=source.replace("private V51Models.AuthorizedDecision decideV51B12OriginalR30(","public V51Models.AuthorizedDecision decideV51B12(")
                        .replace("private V50Models.AuthorizedDecision decideV50OriginalR30(","public V50Models.AuthorizedDecision decideV50(");
                bytes=source.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            if(f[0].endsWith("/SettlementEngine.java")) {
                String source=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
                assertEquals("One registered source-revocation delegator",1,source.split("R30_REVOCATION_BEGIN",-1).length-1);
                source=source.replaceAll("(?s)    // R30_REVOCATION_BEGIN\\n.*?    // R30_REVOCATION_END\\n\\n", "");
                String normalized=restoreR31FeedbackExtractionForH15(source);
                bytes=normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                // Negative controls: extraction cannot excuse a formula/Gate change or add a
                // feedback dependency back into the settlement transaction.
                String changedPnl=source.replace("double pnl = hit ? AppConfig.WIN_PNL_K3 : AppConfig.MISS_PNL_K3;","double pnl = hit ? AppConfig.WIN_PNL_K3 + 1 : AppConfig.MISS_PNL_K3;");
                assertNotEquals(source,changedPnl);
                assertNotEquals("REAL payout mutation must still fail the original owner hash",f[1],StatsR27.sha256(restoreR31FeedbackExtractionForH15(changedPnl).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                String changedGate=source.replace("!DecisionGateEngine.b12K3(n1, n2, n3)","false /* illegal K3 gate bypass */");
                assertNotEquals(source,changedGate);
                assertNotEquals("K3 authorization mutation must still fail the original owner hash",f[1],StatsR27.sha256(restoreR31FeedbackExtractionForH15(changedGate).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                final String core=source;
                assertThrows(AssertionError.class,()->restoreR31FeedbackExtractionForH15(core+"\ncom.ethk3.novaonepro.strategyv2.RuntimeSchemaR26.feedbackWithinTransaction(db, draw);\n"));
            }
            if(f[0].endsWith("/DbStore.java")) {
                String source=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
                String baseline=new String(Files.readAllBytes(new File(root,"construction/STARCORE_R31_FINAL_20261104/baseline_source/"+f[0]).toPath()),java.nio.charset.StandardCharsets.UTF_8);
                bytes=restoreR31DbSchemaSidecarsForH15(source,baseline).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                String illegalPnl=source.replace("dcv.put(\"pnl\", t.pnl);","dcv.put(\"pnl\", t.pnl + 1);");
                assertNotEquals(source,illegalPnl);
                assertNotEquals("DbStore REAL payout outside the registered blocks stays protected",f[1],
                        StatsR27.sha256(restoreR31DbSchemaSidecarsForH15(illegalPnl,baseline).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                String illegalSidecar=source.replace("cv.put(\"audit_key\",\"MCCA_\"+snapshotKey);","cv.put(\"audit_key\",\"FORGED_\"+snapshotKey);");
                assertNotEquals(source,illegalSidecar);
                assertThrows("Pinning a block does not permit arbitrary new side effects",AssertionError.class,
                        ()->restoreR31DbSchemaSidecarsForH15(illegalSidecar,baseline));
            }
            assertEquals("original main-chain body changed: "+f[0],f[1],StatsR27.sha256(bytes));
        }
        // 2. No R27 class can create a REAL decision, settle, or size a stake: no reference to the owners' entry points.
        String[] forbidden = {"DecisionGateEngine", "SettlementEngine", "V51DecisionGateCore", "insertTrade", "insertBet", "placeBet", "stake", "multiplier", "martingale", "chase"};
        File dir = new File(root, "app/src/main/java/com/ethk3/novaonepro/strategyv2"); int scanned = 0;
        for (File f : Objects.requireNonNull(dir.listFiles())) {
            if (!f.getName().contains("R27")) continue; scanned++;
            String src = stripJavaStringsR31(new String(Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "")); // code only: comments and UI text removed
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
        R27TestSupport.log("H15_JAVA_PASS originalOwnerHashesProtected=" + MAINLINE_OWNERS.length + " registeredR31FeedbackExtraction=1 pinnedDbSchemaSidecars=true payoutK3AndUnregisteredSideEffectMutationsRejected=true r27FilesScanned=" + scanned + " noCertificateDenied=" + none.reason);
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
        for (String op : new String[] {"R925_FETCH_BEGIN", "R925_FETCH_PAYLOAD", "R925_FETCH_COMPLETE", "R914_DRAW_SAVE_ATOMIC"}) {
            assertTrue(op + " stays critical", (Boolean) crit.invoke(null, op));
            assertFalse(op + " is not background", (Boolean) bg.invoke(null, op));
        }
        // R31-B02-C01: catch-up history is a background slice; current-draw ingest remains critical.
        assertFalse((Boolean) crit.invoke(null, "R914_DRAW_HISTORY_BATCH"));
        assertTrue((Boolean) bg.invoke(null, "R914_DRAW_HISTORY_BATCH"));
        assertEquals(DatabaseCoordinator.WriteClass.RESEARCH_DATA,
                DatabaseCoordinator.OPERATION_REGISTRY_R31.get("R914_DRAW_HISTORY_BATCH"));
        for (DatabaseCoordinator.WriteClass kind : DatabaseCoordinator.WriteClass.values()) {
            boolean expected = kind == DatabaseCoordinator.WriteClass.DRAW_INGEST
                    || kind == DatabaseCoordinator.WriteClass.LIVE_FREEZE
                    || kind == DatabaseCoordinator.WriteClass.SETTLEMENT;
            assertEquals(kind.name(), expected, DatabaseCoordinator.WriteContext.of(kind, "QUALITY_TYPED_ROUTE").critical());
        }
        R27TestSupport.log("R31_WRITER_PRIORITY_PASS current_draw_critical=true history_background=true");
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
        AtomicInteger attempts = new AtomicInteger(), committed = new AtomicInteger();
        ResearchWriteQueueR27 queue = new ResearchWriteQueueR27((operation, batch) -> {
            if (attempts.getAndIncrement() == 0) throw (IllegalStateException) busy;
            committed.incrementAndGet();
        }, () -> false);
        ResearchWriteQueueR27.Batch batch = new ResearchWriteQueueR27.Batch().add("UPDATE host_fixture SET n=n+1");
        assertFalse("busy submit yields the same prepared work back to its owner", queue.submit("QUALITY_BUSY", batch, () -> false));
        assertEquals(0, committed.get());
        assertEquals(1L, ((Number) queue.waitSnapshotR30().get("rejectedOrRequeued")).longValue());
        assertTrue("retry after the writer is available runs once", queue.submit("QUALITY_BUSY", batch, () -> false));
        assertEquals(2, attempts.get()); assertEquals(1, committed.get());
        for (Throwable retry : new Throwable[] {busy, afterAcquire}) {
            FailureDispositionR31.Decision decision = FailureDispositionR31.classify(retry);
            assertEquals(FailureDispositionR31.Disposition.RETRY_LATER, decision.disposition);
            assertFalse("a lock wait must not consume task fault budget", decision.consumesFaultBudget());
        }
        ResearchWriteQueueR27 faulty = new ResearchWriteQueueR27((operation, value) -> {
            throw (IllegalStateException) real;
        }, () -> false);
        try { faulty.submit("QUALITY_REAL_FAULT", batch, () -> false); fail("real errors must reach the owner"); }
        catch (IllegalStateException actual) { assertSame(real, actual); }
        // Recovery now preserves history and admits one fenced half-open probe, rather than a bulk SQL reset.
        // Native SQLite behavior is exercised separately by R31TaskRuntimeHost.classificationsAndRecovery().
        File dir = new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro/strategyv2");
        String coordinator = new String(Files.readAllBytes(new File(dir, "RuntimeCoordinatorR26.java").toPath()), "UTF-8").replaceAll("\\s+", "");
        String pool = new String(Files.readAllBytes(new File(dir, "ResearchPoolR27.java").toPath()), "UTF-8").replaceAll("\\s+", "");
        String schema = new String(Files.readAllBytes(new File(dir, "SchemaR27.java").toPath()), "UTF-8");
        assertTrue("R26 dispatch retains the shared recovery controller", coordinator.contains("ResearchPoolR27.recoverOneR31(store,owner);"));
        assertTrue("half-open recovery retains its CAS rate limiter", pool.contains("RECOVERY_POLL_R31.compareAndSet(last,mono)"));
        assertTrue("recovery only replaces the selected prior owner/fence", pool.contains("WHEREtask_id=?ANDownerIS?ANDfence=?ANDstatus='QUARANTINED'"));
        assertTrue("upgrade schedules recovery without erasing inherited faults", schema.contains("INHERITED_PRE_R31_QUARANTINE"));
        R27TestSupport.log("R31_BUSY_WRITER_PASS queue_sink_and_typed_budget=true native_recovery_separate=true");
    }

    @Test public void r27ComputeHelpersRunAtBackgroundPriorityAndNarrowDuringP0() throws Exception {
        File dir = new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro/strategyv2");
        String source = new String(Files.readAllBytes(new File(dir, "ResearchPoolR27.java").toPath()), "UTF-8");
        ResearchPoolR27 pool = new ResearchPoolR27(null, "quality-helper-" + UUID.randomUUID(), 2);
        ExecutorService replayExecutor = Executors.newSingleThreadExecutor(r -> {
            throw new AssertionError("P0 replay must not submit a helper");
        });
        try {
            assertTrue("the actual helper executor remains bounded", pool.compute() instanceof ThreadPoolExecutor);
            ThreadPoolExecutor helper = (ThreadPoolExecutor) pool.compute();
            assertEquals(2, helper.getMaximumPoolSize());
            assertTrue(helper.getQueue().remainingCapacity() > 0 && helper.getQueue().remainingCapacity() < Integer.MAX_VALUE);
            AtomicBoolean ran = new AtomicBoolean();
            Thread worker = helper.getThreadFactory().newThread(() -> ran.set(true));
            assertTrue(worker.isDaemon()); worker.start(); worker.join(1000);
            assertFalse("the real factory wrapper exits", worker.isAlive()); assertTrue(ran.get());
            int declaration = source.indexOf("class " + helper.getClass().getSimpleName());
            assertTrue("check the factory actually returned by the production pool", declaration > 0);
            int priority = source.indexOf("setThreadPriority(BACKGROUND_PRIORITY)", declaration);
            int body = source.indexOf("r.run()", declaration);
            assertTrue("the helper requests background priority before its body", priority > declaration && body > priority);
            assertEquals(android.os.Process.THREAD_PRIORITY_BACKGROUND + 1, ResearchPoolR27.BACKGROUND_PRIORITY);
            // The JVM verifies the request hook, not Android's actual OS scheduling priority.
            long now = System.currentTimeMillis();
            pool.writes().setNextDeadline(now + 1000);
            assertTrue("upcoming freezes narrow the actual pool", pool.narrow(now));
            pool.writes().setNextDeadline(0);
            pool.setThermalLimit(1, "QUALITY_THERMAL"); assertTrue(pool.narrow(now));
            pool.setThermalLimit(2, "NORMAL");
            RealtimePressureGateR811.beginAnalysis();
            try {
                assertTrue("P0 narrows the actual pool", pool.narrow(now));
                EvolutionLoopR27 loop = R30EvolutionConfirmIntegrationTest.evolution();
                try (FeatureCacheR27 ownedCache = loop.context().cache; EvolutionLoopR27 ownedLoop = loop) {
                EvolutionLoopR27.Pending pending = new EvolutionLoopR27.Pending();
                pending.spec = loop.champion; pending.state = "BORN";
                java.lang.reflect.Field field = EvolutionLoopR27.class.getDeclaredField("pending");
                field.setAccessible(true); field.set(loop, new ArrayList<>(Collections.singletonList(pending)));
                loop.executor = replayExecutor; loop.serialWhile = () -> pool.narrow(System.currentTimeMillis());
                loop.stepReplay();
                assertNotNull("a real SELECT replay completed serially", pending.trace);
                assertEquals(1, loop.candidateReplayMs.size());
                }
            } finally { RealtimePressureGateR811.endAnalysis(); }
            String runtime = new String(Files.readAllBytes(new File(dir, "RuntimeR27.java").toPath()), "UTF-8").replaceAll("\\s+", "");
            assertTrue("runtime connects evolution to the pressure guard", runtime.contains("loop.serialWhile=()->pool.narrow(System.currentTimeMillis());"));
        } finally {
            replayExecutor.shutdownNow(); assertTrue(replayExecutor.awaitTermination(1, TimeUnit.SECONDS));
            pool.close(); assertTrue("test leaves no pool resources owned", pool.awaitResourcesTerminatedR31(1000));
        }
        R27TestSupport.log("R31_RESEARCH_PRIORITY_PASS bounded_factory_and_P0_serial_replay=true OS_PRIORITY_DEVICE_REQUIRED");
    }

    // ------------------------------------------------------------------ R27.3 third device report (3cc41ff, fresh install)
    @Test public void r27BackgroundWritersNeverQueueAheadOfCriticalWriters() throws Exception {
        AtomicBoolean p0 = new AtomicBoolean(true); AtomicInteger writes = new AtomicInteger();
        ResearchWriteQueueR27 queue = new ResearchWriteQueueR27((operation, batch) -> writes.incrementAndGet(), p0::get);
        ResearchWriteQueueR27.Batch batch = new ResearchWriteQueueR27.Batch().add("UPDATE host_fixture SET n=n+1");
        assertFalse("P0 pressure requeues without entering the write sink", queue.submit("QUALITY_P0", batch, () -> false));
        assertEquals(0, writes.get());
        p0.set(false);
        assertTrue("the retained write advances once after P0 releases", queue.submit("QUALITY_P0", batch, () -> false));
        assertEquals(1, writes.get());
        // Audit the actual typed admission hook as well. Native lock/transaction behavior is separately
        // exercised by R31WriteProtocolHost; these JVM checks do not claim Android latency or OS fairness.
        String src = new String(Files.readAllBytes(new File(R27TestSupport.root(), "app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java").toPath()), "UTF-8").replaceAll("\\s+", "");
        int gate = src.indexOf("if(background&&!reentrant&&criticalBusinessWaitersR914.get()>0&&(!serviceSlot||FREEZE_WAITERS_R31.get()>0))");
        int lock = src.indexOf("if(writeLock.tryLock(", gate);
        int deadline = src.lastIndexOf("if(left<=0)thrownewIllegalStateException(\"CP3_DB_BUSY_DEADLINE\");", gate);
        assertTrue("critical admission and expiry checks precede the sole writer lock", deadline >= 0 && gate > deadline && lock > gate);
        assertTrue("a bounded aged research slot cannot preempt a freeze",
                src.contains("FREEZE_WAITERS_R31.get()==0&&now-LAST_RESEARCH_SERVICE_R31.get()>=5000L"));
        assertEquals(5000, ((Number) DatabaseCoordinator.writeProtocolSnapshotR31().get("researchServiceIntervalMs")).intValue());
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
        // R30-B05-01 moves all actual looks outside evolution; exercise that production evaluator directly.
        EvolutionLoopR27 loop = R30EvolutionConfirmIntegrationTest.evolution();
        try (FeatureCacheR27 ownedCache = loop.context().cache; EvolutionLoopR27 ownedLoop = loop) {
        loop.cfg.nullControl = true;
        loop.i00Champion = loop.champion; loop.i00Tau = 1;
        loop.nullEvolution.champion = loop.champion; loop.nullEvolution.championTau = 1; loop.nullEvolution.lastSeed = 17;
        loop.stepHoldout();
        FrozenConfirmationR30.evaluate(loop.pendingConfirmation,loop.context(),loop.ledger(),new R30EvolutionConfirmIntegrationTest.MemoryEvidence());
        long block = loop.context().split.holdoutBlock;
        assertEquals("only the main candidate spends the main budget",1,loop.ledger().exposures(block));
        assertEquals("both frozen controls keep their own experiment identities",2,loop.ledger().controlExposures(block));
        assertFalse(loop.ledger().degraded(block));
        }
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

    // ------------------------------------------------------------------ R27.4 observers: every strategy runs every period
    @Test public void r274ObserversRecordEveryPeriodButNeverQualify() throws Exception {
        String root = R27TestSupport.root().getPath() + "/app/src/main/java/com/ethk3/novaonepro/";
        String rt = new String(Files.readAllBytes(new File(root + "strategyv2/RuntimeR27.java").toPath()), "UTF-8");
        String live = new String(Files.readAllBytes(new File(root + "strategyv2/LiveShadowsR27.java").toPath()), "UTF-8");
        int i = rt.indexOf("void refreshObservers("); assertTrue(i >= 0);
        String body = rt.substring(i, rt.indexOf("submit(w, \"OBSERVERS\", b);", i));
        // No e-process row, no α index, no certificate, no promotion: observers only collect FUTURE rows.
        assertFalse(body.contains("r27_eprocess")); assertFalse(body.contains("insertShadow")); assertFalse(body.contains("r27_certificate"));
        assertTrue(body.contains("VALUES(?,?,?,?,0,?"));
        assertTrue(rt.contains("LiveShadowsR27.freeze(RuntimeR27.this, w, latest, latestOrd, deadline, LiveShadowsR27.OBSERVE_STATES, false)"));
        assertTrue(rt.contains("if (stepBefore == 9) refreshObservers(w, loop);"));
        // Certification, calibration and the mainline intent still read ACTIVE shadows only.
        assertTrue(live.contains("for (Shadow s : shadows(w.ro, \"('ACTIVE','ACTIVE_REPAIR1')\"))"));
        assertEquals(LiveShadowsR27.LIVE_STATES, "('ACTIVE','ACTIVE_REPAIR1','CONTROL_I00','CONTROL_NULL')");
        assertFalse(MainlineBridgeSU2.R27_INTENT_SQL.contains("OBSERVE"));
        assertFalse(LiveShadowsR27.LIVE_STATES.contains("OBSERVE"));
        assertNotEquals(RuntimeR27.observerId("p"), "p");
        R27TestSupport.log("R27_4_OBSERVERS_PASS cap=" + RuntimeR27.OBSERVE_CAP);
    }

    @Test public void r274ReportCarriesObserversWithoutChangingTheConclusion() {
        ReportR27.Input in = new ReportR27.Input(); in.observersTracking = 24;
        Map<String, Object> o = new LinkedHashMap<>(); o.put("observer", "x"); o.put("periods", 100L); o.put("hits", 31L); in.observers.add(o);
        Map<String, Object> ch = ReportR27.truth(in);
        assertEquals("INSUFFICIENT", ch.get("status")); assertEquals(ReportR27.T_INSUFFICIENT, ch.get("conclusionTemplate"));
        @SuppressWarnings("unchecked") Map<String, Object> obs = (Map<String, Object>) ch.get("observers");
        assertEquals(24L, obs.get("tracking")); assertEquals(100L, obs.get("settledPeriodsTotal")); assertEquals(0.31, (Double) obs.get("pooledHitRate"), 1e-12);
        R27TestSupport.log("R27_4_REPORT_OBSERVERS_PASS");
    }

    @Test public void r274RealtimeChainOutranksResearchThreads() throws Exception {
        String root = R27TestSupport.root().getPath() + "/app/src/main/java/com/ethk3/novaonepro/";
        String svc = new String(Files.readAllBytes(new File(root + "AutoFetchService.java").toPath()), "UTF-8");
        String cont = new String(Files.readAllBytes(new File(root + "strategyv2/CooperativeContinuationR26.java").toPath()), "UTF-8");
        assertTrue(svc.contains("new HandlerThread(\"eth_k3_analysis_channel\", Process.THREAD_PRIORITY_FOREGROUND)"));
        assertTrue(svc.contains("new HandlerThread(\"eth_k3_fetch_channel\", Process.THREAD_PRIORITY_FOREGROUND)"));
        int i = cont.indexOf("private void run(){"); assertTrue(i >= 0);
        assertTrue(cont.substring(i, cont.indexOf("CURRENT.set(this);", i)).contains("threadStartHook"));
        assertFalse("continuations stay pure Java (host verifiers compile them without android.jar)", cont.contains("android."));
        String rt = new String(Files.readAllBytes(new File(root + "strategyv2/RuntimeR27.java").toPath()), "UTF-8");
        assertTrue(rt.contains("CooperativeContinuationR26.threadStartHook = () -> android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);"));
        R27TestSupport.log("R27_4_PRIORITY_PASS");
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
