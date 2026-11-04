package com.ethk3.novaonepro.strategyv2;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Controlled engineering tests. Generated inputs and lifecycle receipts are not natural quality evidence. */
public final class R31ContinuationLifecycleTest {
    private final List<CooperativeContinuationR26<?>> workers = new ArrayList<>();
    private final AtomicInteger held = new AtomicInteger(), returned = new AtomicInteger(), bodyAliveOnReturn = new AtomicInteger();
    private final Map<String, Thread> threads = new ConcurrentHashMap<>();
    private volatile int capacity = 32;
    @Before public void installPermit() {
        CooperativeContinuationR26.installPermitProvider((owner, name) -> {
            for (;;) { int used = held.get(); if (used >= capacity) return null; if (held.compareAndSet(used, used + 1)) break; }
            AtomicBoolean closed = new AtomicBoolean();
            return () -> {
                if (closed.compareAndSet(false, true)) {
                    Thread thread = threads.get(name); if (thread != null && thread.isAlive()) bodyAliveOnReturn.incrementAndGet();
                    held.decrementAndGet(); returned.incrementAndGet();
                }
            };
        });
    }
    @After public void drainWorkers() {
        for (CooperativeContinuationR26<?> work : workers) work.close();
        for (CooperativeContinuationR26<?> work : workers) assertTrue("worker must really stop", work.awaitTermination(2000));
        CooperativeContinuationR26.reapTerminated(); assertEquals(0, bodyAliveOnReturn.get()); assertEquals(0, held.get());
    }
    private <T> CooperativeContinuationR26<T> work(String name, Callable<T> body, java.util.function.BooleanSupplier preempt) {
        CooperativeContinuationR26<T> work = new CooperativeContinuationR26<>(name, () -> { threads.put(name, Thread.currentThread()); return body.call(); }, preempt, null, System::nanoTime, name);
        workers.add(work); return work;
    }
    private static void ignoreInterruptUntil(CountDownLatch release) {
        for (;;) try { release.await(); return; } catch (InterruptedException ignored) { }
    }
    @Test public void pausedContinuationKeepsItsStackAndPermit() throws Exception {
        AtomicInteger steps = new AtomicInteger();
        CooperativeContinuationR26<Integer> work = work("r31-pause", () -> { for (int i = 0; i < 5; i++) { steps.incrementAndGet(); CooperativeContinuationR26.poll(); } return steps.get(); }, () -> true);
        assertEquals(CooperativeContinuationR26.State.PAUSED, work.state());
        CooperativeContinuationR26.Slice<Integer> slice = work.slice(10);
        assertFalse(slice.done); assertEquals(CooperativeContinuationR26.State.PAUSED, slice.state); assertEquals(1, steps.get()); assertEquals(1, held.get());
        for (int i = 0; i < 6 && !slice.done; i++) slice = work.slice(10);
        assertTrue(slice.done); assertEquals(Integer.valueOf(5), slice.result); assertTrue(work.awaitTermination(1000));
        assertEquals(CooperativeContinuationR26.State.TERMINATED, work.state()); assertEquals(1, returned.get());
    }
    @Test public void cancellationKeepsUncooperativeWorkerOwnedAndWaitPreservesInterrupt() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        CooperativeContinuationR26<Integer> work = work("r31-held-cancel", () -> { entered.countDown(); ignoreInterruptUntil(release); return 1; }, () -> false);
        ExecutorService controller = Executors.newSingleThreadExecutor();
        try {
            Future<?> running = controller.submit(() -> { try { work.slice(200); } catch (InterruptedException failure) { throw new AssertionError(failure); } });
            assertTrue(entered.await(1, TimeUnit.SECONDS)); work.close(); running.get(1, TimeUnit.SECONDS);
            assertEquals(CooperativeContinuationR26.State.CANCELLING, work.state()); assertFalse(work.isTerminated());
            assertFalse(work.awaitTermination(0)); assertFalse(work.awaitTermination(20)); assertEquals(1, held.get()); assertEquals(0, returned.get());
            Thread.currentThread().interrupt();
            try { assertFalse(CooperativeContinuationR26.awaitOwnerStopped("r31-held-cancel", 1000)); assertTrue(Thread.currentThread().isInterrupted()); }
            finally { Thread.interrupted(); }
            assertFalse(work.slice(1).done); assertEquals(1, threads.size());
        } finally { release.countDown(); controller.shutdownNow(); assertTrue(work.awaitTermination(1000)); }
        assertEquals(1, returned.get());
    }
    @Test public void hardTimeoutCannotResumeOrSpawnReplacementBeforeExit() throws Exception {
        capacity = 1; CountDownLatch release = new CountDownLatch(1); AtomicInteger calls = new AtomicInteger();
        CooperativeContinuationR26<Integer> work = work("r31-never-poll", () -> { calls.incrementAndGet(); ignoreInterruptUntil(release); return 3; }, () -> false);
        try {
            try { work.slice(1); fail("never-poll body must time out"); } catch (IllegalStateException expected) { assertEquals("R26_UNSLICED_COMPUTATION_CAPACITY", expected.getMessage()); }
            assertEquals(CooperativeContinuationR26.State.CANCELLING, work.state());
            long start = System.nanoTime(); assertFalse(work.slice(1).done); assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 250);
            try { work("r31-forbidden-replacement", () -> 4, () -> false); fail("live cancelled worker still owns the only permit"); } catch (CooperativeContinuationR26.CapacityUnavailable expected) { }
            assertEquals(1, calls.get()); assertEquals(1, held.get());
        } finally { release.countDown(); assertTrue(work.awaitTermination(1000)); }
        CooperativeContinuationR26<Integer> next = work("r31-after-termination", () -> 4, () -> false);
        assertEquals(Integer.valueOf(4), next.slice(10).result); assertTrue(next.awaitTermination(1000)); assertEquals(2, returned.get());
    }
    @Test public void concurrentSliceFailsPromptlyAndControllerInterruptCancels() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        CooperativeContinuationR26<Integer> work = work("r31-two-slices", () -> { entered.countDown(); ignoreInterruptUntil(release); return 1; }, () -> false);
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread controller = new Thread(() -> { try { work.slice(200); } catch (InterruptedException expected) { interrupted.set(true); } });
        controller.start();
        try {
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            try { work.slice(1); fail("second controller must not resume the same stack"); } catch (IllegalStateException expected) { assertEquals("R31_CONTINUATION_SLICE_IN_FLIGHT", expected.getMessage()); }
            controller.interrupt(); controller.join(1000); assertFalse(controller.isAlive()); assertTrue(interrupted.get());
            assertEquals(CooperativeContinuationR26.State.CANCELLING, work.state()); assertEquals(1, held.get());
        } finally { release.countDown(); controller.interrupt(); controller.join(1000); assertTrue(work.awaitTermination(1000)); }
    }
    @Test public void startupClockFailureStillTerminatesAndReturnsPermit() throws Exception {
        CooperativeContinuationR26<Integer> work = new CooperativeContinuationR26<>("r31-clock-failure", () -> 1, () -> false, null, () -> { throw new IllegalStateException("clock fixture"); }, "r31-clock-failure");
        workers.add(work); CooperativeContinuationR26.Slice<Integer> slice = work.slice(10);
        assertTrue(slice.done); assertNotNull(slice.error); assertTrue(work.awaitTermination(1000)); assertEquals(1, returned.get());
    }
    @Test public void writerCriticalRejectsPreparedYieldAndCriticalCloseIsIdempotent() throws Exception {
        CooperativeContinuationR26<Integer> work = work("r31-critical", () -> {
            CooperativeContinuationR26.Critical critical = CooperativeContinuationR26.critical();
            try { CooperativeContinuationR26.yieldPreparedWrite(); fail("writer must not park"); }
            catch (IllegalStateException expected) { assertEquals("R30_YIELD_INSIDE_WRITER", expected.getMessage()); }
            finally { critical.close(); critical.close(); }
            CooperativeContinuationR26.poll(); return 8;
        }, () -> true);
        assertFalse(work.slice(10).done); assertEquals(0, work.snapshot().criticalDepth);
        assertEquals(Integer.valueOf(8), work.slice(10).result); assertTrue(work.awaitTermination(1000));
    }
    @Test public void diagnosticsDistinguishPhaseFromDurableCheckpointAndBoundStack() throws Exception {
        CooperativeContinuationR26<Integer> work = work("r31-diagnostics", () -> {
            ExecutionBudgetR26.phase("numerical\nphase"); CooperativeContinuationR26.poll();
            CooperativeContinuationR26.recordCheckpoint("durable-attempt"); CooperativeContinuationR26.poll(); return 2;
        }, () -> true);
        work.slice(10); CooperativeContinuationR26.DiagnosticSnapshot phase = work.snapshot();
        assertEquals("numerical phase", phase.phase); assertEquals(0, phase.lastCheckpointNanos); assertEquals("NONE", phase.checkpoint); assertTrue(phase.lastPollNanos > 0); assertTrue(phase.stack.size() <= 16);
        work.slice(10); CooperativeContinuationR26.DiagnosticSnapshot checkpoint = work.snapshot();
        assertEquals("durable-attempt", checkpoint.checkpoint); assertTrue(checkpoint.lastCheckpointNanos > 0);
        assertEquals(Integer.valueOf(2), work.slice(10).result); assertTrue(work.awaitTermination(1000));
    }
    @Test public void helperWithoutContinuationActuallyExitsAfterFutureCancellation() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), exited = new CountDownLatch(1);
        AtomicBoolean preserved = new AtomicBoolean(); AtomicLong iterations = new AtomicLong();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            assertFalse(CooperativeContinuationR26.installed()); assertNull(ExecutionBudgetR26.current()); entered.countDown();
            try { for (;;) { iterations.incrementAndGet(); ExecutionBudgetR26.poll(); CooperativeContinuationR26.poll(); } }
            finally { preserved.set(Thread.currentThread().isInterrupted()); exited.countDown(); }
        });
        try {
            assertTrue(entered.await(1, TimeUnit.SECONDS)); assertTrue(future.cancel(true));
            assertTrue("real helper finally must execute", exited.await(1, TimeUnit.SECONDS));
            long stopped = iterations.get(); executor.shutdown(); assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
            assertTrue(preserved.get()); assertEquals(stopped, iterations.get());
        } finally { future.cancel(true); executor.shutdownNow(); assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS)); }
    }

}
