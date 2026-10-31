package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** R30-B00-05: empty sources are INSUFFICIENT with null values; every metric names its R30 measurement source. */
public final class R30MetricEvaluatorTest {
    private static Map<String, Object> snapshot() { Map<String, Object> s = new LinkedHashMap<>(); s.put("timeRange", null); return s; }

    @Test public void emptyReportIsInsufficientNeverZeroOrPass() {
        List<Map<String, Object>> rows = MetricEvaluatorR29.evaluate(new HashMap<String, MetricEvaluatorR29.Probe>(), snapshot());
        assertEquals(44, rows.size());
        for (Map<String, Object> r : rows) {
            String id = (String) r.get("metricId");
            assertEquals(id, "Q02".equals(id) ? "NOT_DEPLOYED" : "INSUFFICIENT", r.get("status"));
            assertNull(id, r.get("value"));
            assertNotEquals(id, "PASS", r.get("status"));
        }
    }

    @Test public void zeroSampleProbeIsInsufficientButFailStaysFail() {
        Map<String, MetricEvaluatorR29.Probe> probes = new HashMap<>();
        probes.put("M07", new MetricEvaluatorR29.Probe("PARTIAL", "no r30_writer_hold rows", "CRITICAL_WAIT", null, null, 0.0, 0, Collections.<String>emptyList()));
        probes.put("M03", new MetricEvaluatorR29.Probe("FAIL", "identity drift", "TIMELINE", null, null, null, 0, new ArrayList<String>()));
        Map<String, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> r : MetricEvaluatorR29.evaluate(probes, snapshot())) byId.put((String) r.get("metricId"), r);
        assertEquals("INSUFFICIENT", byId.get("M07").get("status"));
        assertNull(byId.get("M07").get("value"));
        assertEquals("PARTIAL", byId.get("M07").get("probeStatus"));
        assertEquals("FAIL", byId.get("M03").get("status"));
    }

    @Test public void everyMetricHasMeasurementSource() {
        for (Map<String, Object> r : MetricEvaluatorR29.evaluate(new HashMap<String, MetricEvaluatorR29.Probe>(), snapshot())) {
            Object m = r.get("measurementSource");
            assertTrue(r.get("metricId") + " missing measurementSource", m instanceof String && !((String) m).trim().isEmpty());
        }
    }
}
