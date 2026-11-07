package com.ethk3.novaonepro.strategyv2;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.concurrent.atomic.*;

/** R31-B01-C04#A3a: at least 10000 claim/cancel cycles keep threads, compute permits and owned
 * continuations bounded, and every count returns to zero after cancellation. Engineering stress only. */
public final class R31ContinuationStressTest {
    private final AtomicInteger held = new AtomicInteger(), maxHeld = new AtomicInteger(), returned = new AtomicInteger();
    private static final int CAPACITY = 4;
    @Before public void installPermit() {
        CooperativeContinuationR26.installPermitProvider((owner, name) -> {
            for (;;) { int used = held.get(); if (used >= CAPACITY) return null; if (held.compareAndSet(used, used + 1)) { maxHeld.accumulateAndGet(used + 1, Math::max); break; } }
            AtomicBoolean closed = new AtomicBoolean();
            return () -> { if (closed.compareAndSet(false, true)) { held.decrementAndGet(); returned.incrementAndGet(); } };
        });
    }
    @Test public void tenThousandClaimCancelCyclesStayBoundedAndDrainToZero() throws Exception {
        String owner = "r31-stress-owner";
        int baseline = Thread.activeCount(), maxThreads = baseline, cycles = 10_000;
        for (int i = 0; i < cycles; i++) {
            CooperativeContinuationR26<Integer> work = new CooperativeContinuationR26<>("r31-stress-" + i,
                    () -> { for (;;) CooperativeContinuationR26.poll(); }, () -> true, null, System::nanoTime, owner);
            work.slice(5);
            work.close();
            assertTrue("cancelled continuation must really exit, cycle " + i, work.awaitTermination(2000));
            maxThreads = Math.max(maxThreads, Thread.activeCount());
            if (i % 1000 == 999) {
                CooperativeContinuationR26.reapTerminated();
                assertTrue("owned continuations bounded at cycle " + i, CooperativeContinuationR26.snapshots(owner).size() <= CAPACITY);
            }
        }
        CooperativeContinuationR26.reapTerminated();
        assertTrue("thread count bounded: baseline=" + baseline + " max=" + maxThreads, maxThreads <= baseline + 2 * CAPACITY);
        assertTrue("permits never exceeded capacity", maxHeld.get() <= CAPACITY);
        assertEquals("every permit returned after cancellation", 0, held.get());
        assertEquals("one permit per cycle, returned once", cycles, returned.get());
        assertTrue("no owned continuation remains", CooperativeContinuationR26.snapshots(owner).isEmpty());
        assertTrue("owner fully stopped", CooperativeContinuationR26.awaitOwnerStopped(owner, 0));
    }
}
