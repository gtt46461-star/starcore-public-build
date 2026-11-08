package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B08-C05#A1/#A3/#A4: one measure-kind enumeration for producers and the 16 logical views; object projection. */
public final class R31ResearchMeasureTablesTest {
    /** The T09 filter as it was before R31 (kept here as the regression fixture). */
    static final String PRE_R31_T09 = "WHERE m.kind IN ('OPERATOR_COST','PARENT_CHILD_GAIN','RESEARCH_BUDGET')";

    static String r10Payload(Object topValue, String costScope, Object nodeCost, Object cpu) {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("label", "M_LEAF/nodes=7"); point.put("value", 0.0125); point.put("n", 256L); point.put("lower", -0.002); point.put("upper", 0.027);
        point.put("parentHash", StatsR27.sha256("parent")); point.put("childHash", StatsR27.sha256("child")); point.put("operator", "M_LEAF");
        point.put("nodeCost", nodeCost); point.put("perObjectCpuNs", cpu); point.put("costScope", costScope);
        Map<String, Object> frame = new LinkedHashMap<>(); frame.put("role", "SELECT"); frame.put("mean_gain", topValue); frame.put("points", Collections.singletonList(point));
        Map<String, Object> source = new LinkedHashMap<>(); source.put("viewId", "R10"); source.put("inputHash", StatsR27.sha256("frame")); source.put("frame", frame);
        return JsonR27.write(source);
    }

    @Test public void producersAndT09ShareTheActualR10Kind() {
        Set<ResearchMeasureKindR31> viewKinds = EnumSet.noneOf(ResearchMeasureKindR31.class);
        for (ResearchGraphSpecsR30R1.Spec s : ResearchGraphSpecsR30R1.ALL) { assertEquals(s.measure, s.measureKind.name()); viewKinds.add(s.measureKind); }
        assertEquals(18, viewKinds.size());
        assertEquals(ResearchMeasureKindR31.OPERATOR_GAIN_COST, ResearchMeasureKindR31.fromViewId("R10"));
        String t09 = ResearchTableAdaptersR30R1.query("T09");
        assertFalse("regression fixture: the old filter misses what R10 writes", PRE_R31_T09.contains("'" + ResearchMeasureKindR31.fromViewId("R10").name() + "'"));
        assertTrue("T09 reads the kind R10 actually writes", t09.contains("'OPERATOR_GAIN_COST'"));
        assertTrue("units are a separate column", t09.contains(" AS unit,") && t09.contains(ResearchMeasureKindR31.OPERATOR_GAIN_COST.unit));
        assertTrue("historical names are labelled, not current", t09.contains("ELSE 'HISTORICAL' END AS tier"));
        assertEquals(16, ResearchTableAdaptersR30R1.ids().size());
        for (Map<String, Object> c : ResearchTableAdaptersR30R1.contracts()) { assertEquals(false, c.get("newPhysicalTable")); assertEquals(200L, c.get("maxPage")); }
    }

    @Test public void pagesAreBoundAndBounded() {
        ResearchTableAdaptersR30R1.Page p = ResearchTableAdaptersR30R1.page("T01", Collections.singletonMap("dataset_hash", "abc"), 40, 20);
        assertTrue(p.sql.endsWith(" WHERE dataset_hash=? LIMIT ? OFFSET ?")); assertArrayEquals(new String[]{"abc", "20", "40"}, p.args);
        for (Runnable bad : Arrays.<Runnable>asList(() -> ResearchTableAdaptersR30R1.page("T03", Collections.singletonMap("dataset_hash", "x"), 0, 10),
                () -> ResearchTableAdaptersR30R1.page("T01", Collections.singletonMap("role; DROP TABLE r26_task", "x"), 0, 10),
                () -> ResearchTableAdaptersR30R1.page("T01", null, 0, 201), () -> ResearchTableAdaptersR30R1.page("T01", null, -1, 10), () -> ResearchTableAdaptersR30R1.page("T99", null, 0, 10)))
            try { bad.run(); fail("unbound or unbounded page accepted"); } catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().startsWith("R31_RESEARCH_TABLE_")); }
    }

    @Test public void unknownKindsAndWrongUnitsAreRefusedNotPlottedAsZero() {
        for (String bad : new String[]{"OPERATOR_GAIN_COSTS", "", null}) try { ResearchMeasureKindR31.parseCanonical(bad); fail(bad); } catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().startsWith("R31_RESEARCH_KIND_UNKNOWN")); }
        try { ResearchMeasureKindR31.parseCanonical("OPERATOR_COST"); fail(); } catch (IllegalArgumentException expected) { assertEquals("R31_RESEARCH_KIND_HISTORICAL:OPERATOR_COST", expected.getMessage()); }
        assertEquals("HISTORICAL:OPERATOR_COST->OPERATOR_GAIN_COST", ResearchMeasureKindR31.normalizeHistoricalKind("OPERATOR_COST"));
        assertEquals("CURRENT:OPERATOR_GAIN_COST", ResearchMeasureKindR31.normalizeHistoricalKind("OPERATOR_GAIN_COST"));
        try { ResearchMeasureProjectionR31.operatorGainCost("OPERATOR_GAIN_COST", r10Payload(null, "POOL_CPU_NS", 7L, null)); fail("wrong cost unit"); }
        catch (IllegalArgumentException expected) { assertEquals("R31_PROJECTION_UNIT_MISMATCH:costScope=POOL_CPU_NS", expected.getMessage()); }
        try { ResearchMeasureProjectionR31.operatorGainCost("OPERATOR_GAIN_COST", r10Payload(null, ResearchMeasureProjectionR31.NODE_COST_SCOPE, 0L, null)); fail("zero node cost"); }
        catch (IllegalArgumentException expected) { assertEquals("R31_PROJECTION_NODE_COST", expected.getMessage()); }
        try { ResearchMeasureProjectionR31.projectObjects("ATOM_PAIRED_GAIN", r10Payload(null, ResearchMeasureProjectionR31.NODE_COST_SCOPE, 7L, null)); fail("view/kind mismatch"); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().startsWith("R31_PROJECTION_VIEW_KIND_MISMATCH")); }
        try { ResearchMeasureProjectionR31.projectObjects("NOT_A_KIND", r10Payload(null, ResearchMeasureProjectionR31.NODE_COST_SCOPE, 7L, null)); fail(); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().startsWith("R31_RESEARCH_KIND_UNKNOWN")); }
    }

    @Test public void r10ObjectValuesComeFromFramePointsAndUnknownCpuStaysUnknown() {
        String payload = r10Payload(null, ResearchMeasureProjectionR31.NODE_COST_SCOPE, 7L, null);
        List<ResearchMeasureProjectionR31.ObjectRow> rows = ResearchMeasureProjectionR31.operatorGainCost("OPERATOR_GAIN_COST", payload);
        assertEquals(1, rows.size()); ResearchMeasureProjectionR31.ObjectRow r = rows.get(0);
        assertEquals("object value, not the null top-level mean_gain", 0.0125, r.value, 0); assertEquals(-0.002, r.lower, 0); assertEquals(0.027, r.upper, 0); assertEquals(256, r.n);
        assertEquals(Long.valueOf(7), r.nodeCost); assertEquals("UNKNOWN", r.perObjectCpuNs); assertEquals("M_LEAF", r.operator);
        assertEquals(StatsR27.sha256(payload), r.originalSha256); assertEquals(ResearchMeasureKindR31.OPERATOR_GAIN_COST.unit, r.json().get("unit"));
        Map<String, Object> p = new LinkedHashMap<>(); p.put("label", "x"); p.put("value", null); p.put("n", 0L);
        Map<String, Object> frame = new LinkedHashMap<>(); frame.put("points", Collections.singletonList(p)); Map<String, Object> src = new LinkedHashMap<>(); src.put("viewId", "R06"); src.put("frame", frame);
        assertNull("a missing value stays null, never 0", ResearchMeasureProjectionR31.projectObjects("ATOM_PAIRED_GAIN", JsonR27.write(src)).get(0).value);
    }
}
