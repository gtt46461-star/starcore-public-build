package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class PolicyAstCooperationR34Test {
    @Test public void mixYieldsAfterAllocationAndResumesTheSameVector() throws Exception {
        ReplayDatasetR27 rows = ReplayDatasetR27.synthetic(5000, 27, null);
        AtomicBoolean inputReady = new AtomicBoolean();
        AtomicInteger preemptions = new AtomicInteger();
        try (FeatureCacheR27 cache = new FeatureCacheR27(rows)) {
            cache.override("bigsmall", (ignored, position, a, b) -> {
                double[] values = new double[rows.size() * 10];
                for (int i = 0; i < values.length; i++) values[i] = input(i);
                inputReady.set(true);
                return values;
            });
            PolicyAstR27.Node expression = PolicyAstR27.op("mix", 0.75,
                    PolicyAstR27.constant(1.25), PolicyAstR27.leaf("bigsmall", 1, 8));
            try (CooperativeContinuationR26<double[]> job = new CooperativeContinuationR26<>(
                    "ast-preemption-test", () -> PolicyAstR27.eval(expression, cache, 1),
                    () -> {
                        if (!inputReady.get() || preemptions.get() == 2) return false;
                        preemptions.incrementAndGet();
                        return true;
                    },
                    () -> {}, System::nanoTime)) {
                int pauses = 0;
                double[] result = null;
                for (int slice = 0; slice < 100; slice++) {
                    CooperativeContinuationR26.Slice<double[]> next = job.slice(200);
                    if (next.error != null) throw new AssertionError(next.error);
                    if (next.done) { result = next.result; break; }
                    if (inputReady.get()) pauses++;
                }
                assertEquals(2, preemptions.get());
                assertTrue("The scheduler must be able to reschedule again after allocating the result", pauses >= 2);
                assertNotNull(result);
                assertEquals(50000, result.length);
                for (int i = 0; i < result.length; i++) {
                    double expected = 0.75 * 1.25 + 0.25 * input(i);
                    assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(result[i]));
                }
            }
        }
    }

    private static double input(int i) {
        return i % 27 == 0 ? Double.NaN : (i % 23 - 11) * 0.125;
    }
}
