package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;

/** R31-B11-C01#A1-#A4, C02#A1-#A3, C03#A1-#A3, C04#A1-#A3: 44 metrics, 96 checks, layered objects, bounded export. */
public final class R31MetricReportTest {
    static String sha(String s) { return StatsR27.sha256(s); }
    static Map<String, Object> def(String id) { for (Map<String, Object> d : MetricDefinitionsR29.r31Definitions()) if (id.equals(d.get("id"))) return d; throw new AssertionError(id); }

    @Test public void all44MetricsAreDefinedComputedCollectedAndAcceptedWithoutFillingUnknowns() {
        List<Map<String, Object>> defs = MetricDefinitionsR29.r31Definitions(); assertEquals(44, defs.size());
        for (Map<String, Object> d : defs) {
            for (String k : new String[]{"owner", "collectionSource", "numerator", "denominator", "role", "window", "minimumSample", "missingPolicy", "cards", "requiredEvidence", "target"}) assertNotNull(d.get("id") + ":" + k, d.get(k));
            assertEquals("UNKNOWN_NOT_ZERO", d.get("missingPolicy")); assertEquals(MetricDefinitionsR29.R31_VERSION, d.get("version"));
            assertFalse(String.valueOf(d.get("requiredEvidence")).isEmpty());
        }
        Map<String, Object> missing = MetricEvaluatorR29.evaluateR31(def("M15"), null);
        assertEquals("UNKNOWN_SOURCE_MISSING", missing.get("status")); assertNull("unknown is never filled with 0", missing.get("value"));
    }
    @Test public void jsonAndHtmlOfOneSnapshotRowCarryTheSameFields() {
        Map<String, Object> row = MetricEvaluatorR29.evaluateR31(def("M04"), new MetricEvaluatorR29.MeasurementR31(sha("receipt"), "ENGINEERING_RUNTIME", 72, 0.986, true));
        @SuppressWarnings("unchecked") Map<String, Object> json = (Map<String, Object>) JsonR27.parse(JsonR27.write(row));
        String html = MetricEvaluatorR29.htmlRowR31(row);
        for (String k : new String[]{"id", "status", "value", "denominator"}) assertTrue(k + " in html: " + html, html.contains("data-" + k + "=\"" + String.valueOf(json.get(k)) + "\""));
        assertEquals("PASS", json.get("status"));
    }
    @Test public void everyMetricHasMissingZeroFailureAndSufficientFixturesAndNaturalShortageIsReported() {
        for (Map<String, Object> d : MetricDefinitionsR29.r31Definitions()) {
            String id = String.valueOf(d.get("id")); boolean quality = "QUALITY".equals(d.get("kind")); String role = quality ? "NATURAL_FUTURE" : "ENGINEERING_RUNTIME"; long min = ((Number) d.get("minimumSample")).longValue();
            assertEquals(id, "UNKNOWN_SOURCE_MISSING", MetricEvaluatorR29.evaluateR31(d, new MetricEvaluatorR29.MeasurementR31(null, role, 10, 1.0, true)).get("status"));
            assertEquals(id, "INSUFFICIENT_ZERO_SAMPLES", MetricEvaluatorR29.evaluateR31(d, new MetricEvaluatorR29.MeasurementR31(sha(id), role, 0, null, false)).get("status"));
            assertEquals(id, "FAIL", MetricEvaluatorR29.evaluateR31(d, new MetricEvaluatorR29.MeasurementR31(sha(id), role, min, .1, false)).get("status"));
            assertEquals(id, "PASS", MetricEvaluatorR29.evaluateR31(d, new MetricEvaluatorR29.MeasurementR31(sha(id), role, min, .9, true)).get("status"));
            if (quality) {
                assertEquals(id, "INSUFFICIENT_BELOW_MINIMUM_SAMPLE", MetricEvaluatorR29.evaluateR31(d, new MetricEvaluatorR29.MeasurementR31(sha(id), role, 72, .28, false)).get("status"));
                assertEquals("synthetic or engineering evidence never proves natural quality", "NOT_PROVEN_NOT_NATURAL_ROLE", MetricEvaluatorR29.evaluateR31(d, new MetricEvaluatorR29.MeasurementR31(sha(id), "SYNTHETIC", 5000, .9, true)).get("status"));
            }
        }
    }
    @Test public void crossSourceWatermarkConflictsAreDetectedAndLegalDifferentScopesAreNot() {
        List<MetricEvaluatorR29.SourceValueR31> fixture = Arrays.asList(
            new MetricEvaluatorR29.SourceValueR31("heartbeat.lastSettled", "AutoFetchService", "RUNTIME_HEARTBEAT", "HEARTBEAT", 1793664600000L, "202611020847", "202611020847"),
            new MetricEvaluatorR29.SourceValueR31("task.COMPLETED", "SettlementTaskStoreR36", "SETTLEMENT", "SETTLEMENT_TASKS", 1793664653944L, "202611030010", "202611030010"),
            new MetricEvaluatorR29.SourceValueR31("shadow.settled", "OosAfterDrawWriterR36", "SHADOW", "SETTLEMENT_TASKS", 1793664653944L, "202611030010", "202611030010"));
        Map<String, Object> r = MetricEvaluatorR29.crossSourceR31(fixture);
        assertEquals("the report that claimed 0 conflicts hides a stale/scope difference", "STALE_OR_SCOPE_CONFLICT", r.get("status"));
        assertEquals(false, r.get("settlementStoppedInferred")); assertEquals(3, ((List<?>) r.get("sources")).size());
        assertEquals("different roles/owners on the same scope and watermark agree", "CONSISTENT", MetricEvaluatorR29.crossSourceR31(fixture.subList(1, 3)).get("status"));
        List<MetricEvaluatorR29.SourceValueR31> contradiction = Arrays.asList(fixture.get(1), new MetricEvaluatorR29.SourceValueR31("RSEC-305", "R923", "REPORT", "SETTLEMENT_TASKS", 1793664653944L, "202611030010", "202611030009"));
        assertEquals("VALUE_CONFLICT_SAME_SCOPE_AND_WATERMARK", MetricEvaluatorR29.crossSourceR31(contradiction).get("status"));
        try { MetricEvaluatorR29.crossSourceR31(Collections.singletonList(new MetricEvaluatorR29.SourceValueR31("x", "", "r", "s", 1, "w", "v"))); fail(); } catch (IllegalArgumentException e) { assertEquals("R31_M32_SOURCE_FIELDS_REQUIRED:x", e.getMessage()); }
    }

    @Test public void everyChartMatrixAndCapabilityHasItsOwnEntry() {
        List<Map<String, Object>> entries = ReportObjectIndexR30.objectEntriesR31(Collections.<String, ReportObjectIndexR30.ObjectFactsR31>emptyMap());
        int charts = 0, matrices = 0, capabilities = 0; Set<String> ids = new HashSet<>();
        for (Map<String, Object> e : entries) { assertTrue(ids.add(String.valueOf(e.get("id")))); String k = String.valueOf(e.get("kind")); if (k.equals("CHART")) charts++; else if (k.equals("MATRIX")) matrices++; else capabilities++; }
        assertEquals(47, charts); assertEquals(20, matrices); assertEquals(CapabilityRegistryR30.all().size(), capabilities);
        for (Map<String, Object> e : entries) { assertEquals("unknown facts stay unknown", "UNKNOWN", e.get("ran")); assertEquals("NOT_PASS", e.get("status")); }
    }
    @Test public void runningWithoutCommitEmptyFrameAndConsumedWithoutAdvantageAreDistinct() {
        Map<String, Object> running = ReportObjectIndexR30.layersR31(new ReportObjectIndexR30.ObjectFactsR31("TASK:x", "TASK", "R31-B01-C03", true, true, false, null, null, null, true));
        assertEquals("RUNNING_NO_COMMIT", running.get("ran")); assertEquals("NO", running.get("produced"));
        Map<String, Object> empty = ReportObjectIndexR30.layersR31(new ReportObjectIndexR30.ObjectFactsR31("CHART:R03", "CHART", "R31-B08-C04", true, false, true, false, null, null, true));
        assertEquals("EMPTY", empty.get("produced"));
        Map<String, Object> noGain = ReportObjectIndexR30.layersR31(new ReportObjectIndexR30.ObjectFactsR31("MATRIX:R930:CROSS_POSITION_FLOW", "MATRIX", "R31-B08-C01", true, false, true, true, true, false, true));
        assertEquals("YES", noGain.get("consumed")); assertEquals("NO_MEASURED_ADVANTAGE", noGain.get("effective")); assertEquals("NOT_PASS", noGain.get("status"));
        Map<String, Object> formal = ReportObjectIndexR30.layersR31(new ReportObjectIndexR30.ObjectFactsR31("CHART:R11", "CHART", "R31-B08-C04", true, false, true, true, null, null, false));
        assertEquals("NOT_APPLICABLE", formal.get("consumed")); assertEquals("PASS", formal.get("status"));
    }
    @Test public void everyFailureTracesToACardAndRuleCoverageIsNotDefectCompleteness() {
        for (Map<String, Object> e : ReportObjectIndexR30.objectEntriesR31(Collections.<String, ReportObjectIndexR30.ObjectFactsR31>emptyMap())) {
            assertTrue(String.valueOf(e.get("card")).startsWith("R31-B")); assertEquals("SEE_CARD:" + e.get("card"), e.get("nextAction"));
        }
        assertEquals("RULE_COVERAGE_IS_NOT_DEFECT_COMPLETENESS", ReportObjectIndexR30.DIAGNOSIS_SCOPE_R31);
    }

    static ReportSnapshotR30.AttachmentsR31.Item item(File dir, String name, String content, String metric) throws IOException {
        File f = new File(dir, name); Files.write(f.toPath(), content.getBytes("UTF-8")); return new ReportSnapshotR30.AttachmentsR31.Item(name, f.length(), StatsR27.sha256(content.getBytes("UTF-8")), metric);
    }
    @Test public void anInsufficientBudgetListsOmittedItemsAndLeavesTheirMetricsUnknown() throws Exception {
        File dir = Files.createTempDirectory("r31-attach").toFile();
        List<ReportSnapshotR30.AttachmentsR31.Item> items = Arrays.asList(item(dir, "a.tsv", "0123456789", "M17"), item(dir, "b.db", "0123456789012345678901234567890123456789", "M09"));
        Map<String, Object> plan = ReportSnapshotR30.AttachmentsR31.plan(items, 20);
        assertEquals(1, ((List<?>) plan.get("included")).size()); assertEquals(1, ((List<?>) plan.get("omitted")).size());
        assertEquals("UNKNOWN_ATTACHMENT_NOT_INCLUDED", ((Map<?, ?>) plan.get("metricsUnknownBecauseOmitted")).get("M09")); assertEquals(false, plan.get("referenceOnlyIsCompleteRawPackage"));
        Map<String, Object> rows = ReportSnapshotR30.AttachmentsR31.rowset("RSEC-321", 900, 200, 65536, "ALL_ALGORITHMS");
        assertEquals(700L, rows.get("omitted")); assertEquals(false, rows.get("shownIsDenominator"));
    }
    @Test public void includedAttachmentsVerifyByHashSizeAndPath() throws Exception {
        File dir = Files.createTempDirectory("r31-verify").toFile(); ReportSnapshotR30.AttachmentsR31.Item ok = item(dir, "frame.json", "{\"points\":[1,2]}", "M33");
        assertEquals("VERIFIED", ReportSnapshotR30.AttachmentsR31.verify(dir, ok));
        Files.write(new File(dir, "frame.json").toPath(), "{\"points\":[1,3]}".getBytes("UTF-8"));
        assertEquals("HASH_MISMATCH_NOT_RECOMPUTABLE", ReportSnapshotR30.AttachmentsR31.verify(dir, ok));
        assertEquals("MISSING_NOT_RECOMPUTABLE", ReportSnapshotR30.AttachmentsR31.verify(dir, new ReportSnapshotR30.AttachmentsR31.Item("gone.json", 3, ok.sha256, null)));
        assertEquals("REJECTED_PATH", ReportSnapshotR30.AttachmentsR31.verify(dir, new ReportSnapshotR30.AttachmentsR31.Item("../escape.json", 3, ok.sha256, null)));
        Files.write(new File(dir, "frame.json").toPath(), "{\"points\":[1,2]}x".getBytes("UTF-8"));
        assertEquals("SIZE_MISMATCH_NOT_RECOMPUTABLE", ReportSnapshotR30.AttachmentsR31.verify(dir, ok));
    }
    @Test public void cancelKillOrCorruptionDuringExportNeverHarmsTheLiveDatabaseOrOriginals() throws Exception {
        File live = Files.createTempDirectory("r31-live").toFile(), out = Files.createTempDirectory("r31-export").toFile();
        StringBuilder big = new StringBuilder(); for (int i = 0; i < 200_000; i++) big.append(i % 10);
        ReportSnapshotR30.AttachmentsR31.Item original = item(live, "main.db", big.toString(), "M09"); byte[] before = Files.readAllBytes(new File(live, "main.db").toPath());
        int[] polls = {0};
        assertEquals("CANCELLED_NO_PARTIAL_EXPORT", ReportSnapshotR30.AttachmentsR31.export(new File(live, "main.db"), out, original, () -> ++polls[0] > 1));
        assertFalse(new File(out, "main.db").exists()); assertFalse(new File(out, "main.db.part").exists());
        Files.write(new File(out, "main.db.part").toPath(), "half".getBytes("UTF-8")); // a killed export leaves a part file
        assertEquals(1, ReportSnapshotR30.AttachmentsR31.recover(out)); assertFalse(new File(out, "main.db.part").exists());
        ReportSnapshotR30.AttachmentsR31.Item wrong = new ReportSnapshotR30.AttachmentsR31.Item("main.db", original.bytes, sha("other"), "M09");
        assertEquals("SOURCE_CHANGED_OR_CORRUPT_NOT_EXPORTED", ReportSnapshotR30.AttachmentsR31.export(new File(live, "main.db"), out, wrong, null));
        assertFalse(new File(out, "main.db").exists());
        assertEquals("EXPORTED", ReportSnapshotR30.AttachmentsR31.export(new File(live, "main.db"), out, original, null));
        assertEquals("VERIFIED", ReportSnapshotR30.AttachmentsR31.verify(out, original));
        assertArrayEquals("the live original is only read", before, Files.readAllBytes(new File(live, "main.db").toPath()));
    }

    @Test public void checkIdsEqualTheReal96AndEveryMetricHasIndependentEvidence() throws Exception {
        CheckMetricCrosswalkR31.validateCoverage();
        String raw = new String(Files.readAllBytes(Paths.get("../construction/STARCORE_R31_FINAL_20261104/evidence/derived/checks_96.json")), "UTF-8");
        Set<String> real = new TreeSet<>(); for (Object o : (List<?>) JsonR27.parse(raw)) real.add(String.valueOf(((Map<?, ?>) o).get("id")));
        Set<String> mine = new TreeSet<>(); Map<String, String> outcome = new HashMap<>(); for (CheckMetricCrosswalkR31.Check c : CheckMetricCrosswalkR31.checks()) { mine.add(c.id); outcome.put(c.id, c.domainOutcome); }
        assertEquals(96, real.size()); assertEquals(real, mine);
        for (Object o : (List<?>) JsonR27.parse(raw)) { Map<?, ?> r = (Map<?, ?>) o; assertEquals("domain outcome kept apart from execution", r.get("domain_outcome"), outcome.get(r.get("id"))); }
        assertEquals(44, CheckMetricCrosswalkR31.definitions().size());
        for (CheckMetricCrosswalkR31.Metric m : CheckMetricCrosswalkR31.definitions()) assertFalse(m.id, m.requiredEvidence.isEmpty());
        for (String uncovered : new String[]{"M08", "M16", "M25", "M29", "M34", "Q04", "Q08"}) assertTrue(uncovered, CheckMetricCrosswalkR31.uncoveredConditions(uncovered).get(0).startsWith("NO_SUPPORTING_CHECK"));
    }
    @Test public void aStaticCheckPassNeverMakesQualityOrDeviceMetricsPass() {
        for (String id : new String[]{"Q01", "Q06", "M13", "M14", "M33", "M34"}) assertNotEquals(id, "PASS", CheckMetricCrosswalkR31.metricStatusFromChecks(id, false));
        assertEquals("a structure-only PASS supports a subcondition only", "STRUCTURE_SUBCONDITION_ONLY", CheckMetricCrosswalkR31.metricStatusFromChecks("M01", false));
        assertEquals("FAIL_OR_BLOCKED_BY_CHECK", CheckMetricCrosswalkR31.metricStatusFromChecks("M31", true));
        assertEquals("PASS", CheckMetricCrosswalkR31.metricStatusFromChecks("Q06", true));
    }
    @Test public void completeMetadataDoesNotMeanTheProductPassed() {
        Map<String, String> results = new HashMap<>(); for (CheckMetricCrosswalkR31.Metric m : CheckMetricCrosswalkR31.definitions()) results.put(m.id, CheckMetricCrosswalkR31.metricStatusFromChecks(m.id, false));
        Map<String, Object> c = CheckMetricCrosswalkR31.coverage(results);
        assertEquals(false, c.get("productAllPassed")); assertEquals(96L, c.get("checksExecuted"));
        Map<String, String> allBytes = new HashMap<>(); for (CheckMetricCrosswalkR31.Metric m : CheckMetricCrosswalkR31.definitions()) allBytes.put(m.id, "SUPPORTED_NOT_PROVEN");
        Map<String, Object> full = CheckMetricCrosswalkR31.coverage(allBytes);
        assertEquals("100% metadata coverage", 1.0, (Double) full.get("reportCoverage"), 0); assertEquals(false, full.get("productAllPassed")); assertEquals(0.0, (Double) full.get("passRate"), 0);
    }
}
