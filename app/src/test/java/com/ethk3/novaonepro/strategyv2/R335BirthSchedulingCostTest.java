package com.ethk3.novaonepro.strategyv2;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class R335BirthSchedulingCostTest {
    private static final int ROWS = 128;
    private static final long PAUSE_MS = (long) (ROWS * BirthGateR27.MAX_PREDICT_MS) + 350;

    @Test public void sameBirthRetainsSignatureAcrossSchedulerPauseAndChargesRunningDelay() throws Exception {
        BirthGateR27.Verdict uninterrupted = evaluate(false, false);
        assertTrue(uninterrupted.detail, uninterrupted.ok);
        BirthGateR27.Verdict delayed = evaluate(false, true);
        assertEquals(delayed.detail, BirthGateR27.Reason.COST, delayed.reason);
        BirthGateR27.Verdict parked = evaluate(true, false);
        System.out.println("R335_BIRTH_GATE baseline=" + uninterrupted.detail + " activeDelay=" + delayed.detail
                + " scheduledPause=" + parked.detail + " forcedPauseMs=" + PAUSE_MS);
        assertTrue(parked.detail, parked.ok);
        assertArrayEquals(uninterrupted.signature, parked.signature);
    }

    private static BirthGateR27.Verdict evaluate(boolean suspend, boolean slow) throws Exception {
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(ROWS, 21, null);
        AtomicBoolean entered = new AtomicBoolean();
        try (FeatureCacheR27 cache = new FeatureCacheR27(data)) {
            cache.override("freq", (f, position, src, window) -> {
                if (suspend) {
                    entered.set(true);
                    CooperativeContinuationR26.yieldPreparedWrite();
                }
                if (slow) {
                    try { Thread.sleep(PAUSE_MS); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                }
                return f.freq(src, window);
            });
            BirthGateR27 gate = new BirthGateR27(cache, ROWS);
            PolicySpecR27 policy = new PolicySpecR27(PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", 1, 32)),
                    1, PolicySpecR27.Rule.QUANTILE, 0, 0.2, 1.0, false, 0.01, 0.01, null);
            if (!suspend) return gate.check(policy);
            try (CooperativeContinuationR26<BirthGateR27.Verdict> work = new CooperativeContinuationR26<>(
                    "r335-birth-test", () -> gate.check(policy), () -> false, null, System::nanoTime)) {
                CooperativeContinuationR26.Slice<BirthGateR27.Verdict> slice = work.slice(200);
                while (!entered.get() && !slice.done) slice = work.slice(200);
                assertTrue("leaf did not run", entered.get());
                assertFalse("leaf did not park", slice.done);
                Thread.sleep(PAUSE_MS);
                for (int i = 0; i < 100 && !slice.done; i++) slice = work.slice(200);
                if (slice.error != null) throw new AssertionError(slice.error);
                assertTrue("birth did not complete", slice.done);
                return slice.result;
            }
        }
    }
}
