package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class R341BackgroundTestCases {
    private static final String BASE = "202611240024";
    private static final String TARGET = "202611240025";
    private R341BackgroundTestCases() {}

    public static void durableRequestAndSettlement(Context app) {
        DbStore store = new DbStore(app);
        try {
            Models.Draw base = new Models.Draw();
            base.issue = BASE;
            base.openTime = System.currentTimeMillis() - 1000L;
            base.fetchTime = base.openTime + 500L;
            for (int i = 0; i < 10; i++) base.numbers[i] = i + 1;
            assertTrue(store.saveDraw(base) > 0L);
            BusinessReconcilerR36 reconciler = new BusinessReconcilerR36();
            AtomicLong generation = new AtomicLong();
            BusinessReconcilerR36.ReconcileResult first = reconciler.reconcile(store, store.getDrawByIssue(BASE), "ENGINEERING_NO_UI", request -> {
                assertEquals(TARGET, request.targetIssue);
                generation.set(request.generation);
                throw new DatabaseCoordinator.WriteRejectedR31("LOCK_BUSY", "R341_NATIVE_RECONCILE",
                        "ADMISSION", DatabaseCoordinator.CommitOutcome.ROLLED_BACK,
                        new IllegalStateException("DATABASE IS LOCKED"));
            });
            assertTrue(first.status, first.status.startsWith("ANALYSIS_RETRY_DB_BUSY"));
            assertTrue(generation.get() > 0L);
            reconciler.reconcile(store, store.getDrawByIssue(BASE), "ENGINEERING_RECONCILE_RETRY", request -> {
                assertEquals(generation.get(), request.generation);
                assertEquals(base.openTime + 60000L, request.deadlineAt);
            });
            try (Cursor row = store.readable().rawQuery("SELECT count(*) FROM "
                    + DbHelper.TABLE_R36_ANALYSIS_REQUEST + " WHERE target_issue=?", new String[]{TARGET})) {
                assertTrue(row.moveToFirst());
                assertEquals(1L, row.getLong(0));
            }
        } finally { store.close(); }
    }

    public static void generationRejectsCancelledOwner(Context app) throws Exception {
        AtomicLong elapsed = new AtomicLong(50_000L);
        ServiceLoopTicketR341 ticket = new ServiceLoopTicketR341(elapsed::get);
        Queue queue = new Queue();
        AtomicInteger calls = new AtomicInteger();
        ticket.reset(1);
        queue.reject = true;
        assertEquals(ServiceLoopTicketR341.Result.REJECTED, ticket.request(1, queue, queue, 0L, true, calls::incrementAndGet));
        queue.reject = false;
        assertEquals(ServiceLoopTicketR341.Result.POSTED, ticket.request(1, queue, queue, 100L, true, calls::incrementAndGet));
        Runnable old = queue.callbacks.get(0);
        for (int i = 0; i < 100; i++) {
            assertEquals(ServiceLoopTicketR341.Result.RETAINED, ticket.request(1, queue, queue, 1000L, true, calls::incrementAndGet));
        }
        assertTrue(ticket.snapshot().contains("dueElapsedMs=50100"));
        ticket.reset(2);
        HandlerThread owner = new HandlerThread("r341-test-service-owner");
        owner.start();
        try {
            CountDownLatch invoked = new CountDownLatch(1);
            assertEquals(ServiceLoopTicketR341.Result.POSTED, ticket.request(2, new Handler(owner.getLooper()), 0L, true, () -> {
                calls.incrementAndGet();
                invoked.countDown();
            }));
            old.run();
            assertTrue(invoked.await(2L, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
            assertEquals(ServiceLoopTicketR341.Result.STALE, ticket.request(1, queue, queue, 0L, true, calls::incrementAndGet));
        } finally { owner.quitSafely(); }
    }

    public static void userStopBlocksDelivery(Context app) {
        ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
        heartbeat.setUserEnabled(false);
        AtomicInteger calls = new AtomicInteger();
        RecoveryCoordinatorR815.Result out = RecoveryCoordinatorR815.request(app, "ENGINEERING_USER_STOP", 0L,
                100_000L, "TEST_BOOT", (context, reason) -> { calls.incrementAndGet(); return true; });
        assertFalse(out.accepted);
        assertEquals("USER_DISABLED_FAIL_CLOSED", out.status);
        assertEquals(0, calls.get());
        assertFalse(RecoveryCoordinatorR815.evidence(app).businessActuallyRestored());
    }

    public static void undeliveredThirdAttemptAndNewBoot(Context app) {
        ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
        heartbeat.setUserEnabled(true);
        try {
            AtomicInteger dispatched = new AtomicInteger();
            RecoveryCoordinatorR815.Delivery source = (context, reason) -> dispatched.incrementAndGet() != 3;
            RecoveryCoordinatorR815.Result first = RecoveryCoordinatorR815.request(app, "TEST_JOB", 0L, 100_000L, "BOOT_A", source);
            assertTrue(first.accepted);
            for (int i = 0; i < 10; i++) {
                RecoveryCoordinatorR815.Result merged = RecoveryCoordinatorR815.request(app, "TEST_ALARM", 0L, 101_000L + i, "BOOT_A", source);
                assertEquals("COALESCED_AWAITING_PROGRESS", merged.status);
                assertEquals(first.generation, merged.generation);
            }
            assertTrue(RecoveryCoordinatorR815.request(app, "TEST_JOB", 0L, 130_000L, "BOOT_A", source).accepted);
            RecoveryCoordinatorR815.Result lost = RecoveryCoordinatorR815.request(app, "TEST_JOB", 0L, 160_000L, "BOOT_A", source);
            assertEquals(3, lost.attempts);
            assertFalse(lost.accepted);
            assertEquals("START_DEFERRED_OR_FAILED", lost.status);
            assertEquals(190_000L, lost.nextDueElapsedMs);
            assertEquals("DELIVERY_RETRY_PENDING", RecoveryCoordinatorR815.request(app, "TEST_ALARM", 0L,
                    161_000L, "BOOT_A", source).status);
            assertEquals("RECOVERY_RATE_LIMITED", RecoveryCoordinatorR815.request(app, "TEST_JOB", 0L,
                    190_000L, "BOOT_A", source).status);
            assertFalse(RecoveryCoordinatorR815.evidence(app).businessActuallyRestored());
            RecoveryCoordinatorR815.Result reboot = RecoveryCoordinatorR815.request(app, "TEST_ALARM", 0L, 100L, "BOOT_B", source);
            assertTrue(reboot.accepted);
            assertEquals(1, reboot.attempts);
            assertTrue(reboot.generation > first.generation);
            assertEquals(4, dispatched.get());
        } finally { heartbeat.setUserEnabled(false); }
    }

    public static void noReconcileFromDisabledUiLifecycle(Context app) throws Exception {
        ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
        heartbeat.setUserEnabled(false);
        long before = RecoveryCoordinatorR815.currentGeneration(app);
        for (int i = 0; i < 100; i++) LifecycleHealthR963.keepService(app, "engineering_ui_exit");
        CountDownLatch drained = new CountDownLatch(1);
        assertTrue(LifecycleHealthR963.post(app, drained::countDown));
        assertTrue(drained.await(2L, TimeUnit.SECONDS));
        assertEquals(before, RecoveryCoordinatorR815.currentGeneration(app));
        assertFalse(heartbeat.isUserEnabled());
    }

    public static void stoppedStateDoesNotRepresentBusinessProgress(Context app) {
        ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
        heartbeat.setUserEnabled(false);
        heartbeat.markStop("ENGINEERING_STOPPED");
        heartbeat.markServiceLoopsR341("fetch=none", "watchdog=none", "settlement=none", "research=none");
        RecoveryCoordinatorR815.markServiceCompleted(app);
        assertFalse(heartbeat.isUserEnabled());
        assertEquals(RecoveryCoordinatorR815.State.IDLE, RecoveryCoordinatorR815.evidence(app).state);
        assertFalse(RecoveryCoordinatorR815.evidence(app).businessActuallyRestored());
    }

    private static final class Queue implements ServiceLoopTicketR341.Queue {
        final List<Runnable> callbacks = new ArrayList<>();
        boolean reject;
        @Override public boolean post(Runnable callback, long delayMs) {
            return !reject && callbacks.add(callback);
        }
        @Override public void remove(Runnable callback) { callbacks.remove(callback); }
    }
}
