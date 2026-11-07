package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import org.junit.Test;

/** R31-B06-C02#A3a: a missing, incomplete, non-device or below-gate performance receipt never raises the family
 * budget; throughput, peak memory and realtime latency are all required fields. Gate values here are fixtures. */
public final class R31ReplayPerformanceGateTest {
    private final ReplayCheckpointR31.PerformanceGate gate = new ReplayCheckpointR31.PerformanceGate(500, 96L << 20, 1000);
    private static List<?> reasons(Map<String, Object> d) { return (List<?>) d.get("reasons"); }
    @Test public void missingOrIncompleteReceiptKeepsBudget() {
        Map<String, Object> missing = gate.decide(null);
        assertEquals("KEEP_FAMILY_BUDGET", missing.get("decision")); assertTrue(reasons(missing).contains("RECEIPT_MISSING"));
        Map<String, Object> partial = gate.decide(new ReplayCheckpointR31.PerformanceReceipt(Double.NaN, 0, -1, "", ""));
        assertEquals("KEEP_FAMILY_BUDGET", partial.get("decision"));
        for (String f : new String[]{"cpuSamplesPerSecond", "peakHeapBytes", "realtimeP95Ms", "measuredOn", "receiptId"})
            assertTrue(f, reasons(partial).contains("FIELD_MISSING:" + f));
    }
    @Test public void hostMeasurementOrAnyMissedGateKeepsBudget() {
        assertTrue(reasons(gate.decide(new ReplayCheckpointR31.PerformanceReceipt(900, 64L << 20, 400, "HOST", "r1"))).contains("NOT_DEVICE_MEASURED:HOST"));
        assertTrue(reasons(gate.decide(new ReplayCheckpointR31.PerformanceReceipt(100, 64L << 20, 400, "DEVICE", "r2"))).contains("THROUGHPUT_BELOW_GATE"));
        assertTrue(reasons(gate.decide(new ReplayCheckpointR31.PerformanceReceipt(900, 128L << 20, 400, "DEVICE", "r3"))).contains("PEAK_HEAP_ABOVE_GATE"));
        assertTrue(reasons(gate.decide(new ReplayCheckpointR31.PerformanceReceipt(900, 64L << 20, 1500, "DEVICE", "r4"))).contains("REALTIME_P95_ABOVE_GATE"));
    }
    @Test public void onlyACompleteDeviceReceiptMeetingEveryGateAllowsIncrease() {
        Map<String, Object> ok = gate.decide(new ReplayCheckpointR31.PerformanceReceipt(900, 64L << 20, 400, "DEVICE", "device-receipt-1"));
        assertEquals("ALLOW_FAMILY_BUDGET_INCREASE", ok.get("decision"));
        assertTrue(reasons(ok).isEmpty()); assertEquals("device-receipt-1", ok.get("receiptId"));
    }
}
