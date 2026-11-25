package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Map;

public final class R342BackgroundTestCases {
    private R342BackgroundTestCases() {}

    public static void pollTiming(Context ignored) {
        AutoFetchService.PollCadenceR31 cadence = new AutoFetchService.PollCadenceR31();
        cadence.beginSession(100L);
        cadence.wake(100L);
        cadence.finishRoundR342("202611250031", "DRAW_INGEST_RETURNED", 2150, 11, 15, 20, 8);
        assertEquals("PREVIOUS_network", cadence.overrunReasonR342());
        assertEquals(1L, cadence.nextDelay(2304, 1000, cadence.overrunReasonR342(), 1L));
        cadence.wake(2305L);
        cadence.finishRoundR342("", "NETWORK_UNAVAILABLE", -1, -1, -1, -1, -1);
        Map<?,?> state = cadence.snapshot();
        assertEquals(1L, state.get("missed_tick"));
        Map<?,?> history = (Map<?,?>)state.get("recentRoundPhases");
        Map<?,?> network = (Map<?,?>)history.get("network");
        assertEquals(2150L, network.get("p99Ms"));
        assertEquals(1L, network.get("missing"));
        assertEquals(1L, ((Map<?,?>)state.get("roundOutcomes")).get("NETWORK_UNAVAILABLE"));
        ApiClient.LatestRoundR342 request = new ApiClient.LatestRoundR342(100L);
        assertEquals(1L, request.remaining(request.deadline - 1L));
        assertEquals(0L, request.remaining(request.deadline));
    }

    public static void measuredWindow(Context ignored) {
        RuntimeHealthLedgerR36.BusinessTruth result = new RuntimeHealthLedgerR36.BusinessTruth();
        RuntimeHealthLedgerR36.applyPerformanceR80(result,
                new RuntimeHealthLedgerR36.Metrics("OK",1,2804L,"SUCCESS","",0L,"",2,1));
        assertEquals("INSUFFICIENT_MEASURED_WINDOW", result.performanceState);
        assertEquals(1, result.performanceUnmeasured);
        RuntimeHealthLedgerR36.applyPerformanceR80(result,
                new RuntimeHealthLedgerR36.Metrics("OK",29,7000L,"SUCCESS","",0L,"",30,1));
        assertEquals("FAIL", result.performanceState);
        RuntimeHealthLedgerR36.applyPerformanceR80(result,
                new RuntimeHealthLedgerR36.Metrics("OK",30,2804L,"SUCCESS","",0L,"",30,0));
        assertEquals("PASS", result.performanceState);
        assertFalse(RuntimeHealthLedgerR36.measuredDurableR80(0L,"SUCCESS","HISTORICAL_RECONSTRUCTION"));
    }

    public static void frameAndWriter(Context app) {
        FrameMetricsR906 frames = new FrameMetricsR906();
        for (int i = 0; i < 99; i++) frames.recordFrameGapR342(17_000_000L,true,false);
        frames.recordFrameGapR342(176_000_000L,true,true);
        String measured = frames.summary();
        assertTrue(measured, measured.contains("maxMs=176.000"));
        assertTrue(measured, measured.contains("recentGapSamples=100|recentGapP95Ms=17.000|recentGapP99Ms=17.000"));
        assertTrue(measured, measured.contains("slowFrameWithRecentBind=1"));
        DbStore store = new DbStore(app);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                try {
                    store.coordinatorR36().runWriteOrThrowF01("R36_HEALTH", db -> {
                        fail("The UI executed a database write");
                        return null;
                    });
                    fail("Main writer call was admitted");
                } catch (IllegalStateException refused) {
                    assertTrue(refused.getMessage().contains("CP3_DB_WRITE_ON_MAIN_THREAD"));
                }
            });
        } finally {
            store.close();
        }
    }

    public static void ownerProjection(Context ignored) throws Exception {
        RuntimeObservationScopeR341 owner = RuntimeObservationScopeR341.capture(
                "75d0e5db-0e59-4ee7-949c-878cb29e0aed", null, RuntimeObservationScopeR341.SYSTEM);
        String original = new org.json.JSONObject(owner.exportEnvelope()).toString();
        org.json.JSONObject old = new org.json.JSONObject(original);
        old.put("processEpoch","retired-process");
        assertEquals("USER_DISABLED", BackgroundRuntimeContractR922.observedOwnerR342(
                false,"RUNNING",original,original,owner));
        assertEquals("OWNER_STARTING", BackgroundRuntimeContractR922.observedOwnerR342(
                true,"STARTING",original,original,owner));
        assertEquals("BUSINESS_PROJECTION_SCOPE_MISMATCH", BackgroundRuntimeContractR922.observedOwnerR342(
                true,"RUNNING",original,old.toString(),owner));
        assertEquals("CURRENT_OWNER_BUSINESS_PROJECTION_OBSERVED", BackgroundRuntimeContractR922.observedOwnerR342(
                true,"RUNNING",original,original,owner));
    }

    public static void crashIndex(Context app) {
        SharedPreferences prefs = app.getSharedPreferences("v20_crash_snapshot", Context.MODE_PRIVATE);
        assertTrue(prefs.edit().putInt("crash_count",2).putLong("at",1L).commit());
        CrashRecorder.recordCaught(app, new IllegalStateException("R342_CAUGHT_FIXTURE"), "R342_CRASH_FIXTURE");
        String summary = CrashRecorder.detailedSummary(app);
        assertTrue(summary, summary.contains("r342CurrentReleaseRecordedEvents10m=1|scope=BOUNDED_APP_EVENTS_SAME_BOOT"));
        assertTrue(summary, summary.contains("R342_CRASH_FIXTURE"));
        assertTrue(summary, summary.contains("stackPrefixSha256"));
        assertTrue(summary, summary.contains(BuildConfig.SOURCE_COMMIT_R26));
        assertTrue(summary, summary.contains("crashCount=3"));
    }
}
