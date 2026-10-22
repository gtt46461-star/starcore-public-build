package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/** Runs the production mailbox with a delayed pump and an expired queued request. */
public final class RecoveryDeadlineR999 {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        // Initialize the host framework's main-thread identity before warmup.
        android.os.Looper.getMainLooper();
        CountDownLatch warm = new CountDownLatch(1);
        new Thread(() -> { RecoveryDispatchR965.warmUpOffMain(); warm.countDown(); }, "startup-health").start();
        require(warm.await(2, TimeUnit.SECONDS), "WARMUP");
        CountDownLatch workerEntered = new CountDownLatch(1), releaseWork = new CountDownLatch(1);
        CountDownLatch pumpInFinish = new CountDownLatch(1), releasePump = new CountDownLatch(1);
        RecoveryDispatchR965.submit(() -> {
            workerEntered.countDown();
            try { releaseWork.await(3, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, () -> {
            pumpInFinish.countDown();
            try { releasePump.await(3, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, 1000);
        require(workerEntered.await(2, TimeUnit.SECONDS), "WORKER_ENTERED");
        AtomicInteger expiredRuns = new AtomicInteger(), finishes = new AtomicInteger();
        CountDownLatch queuedFinished = new CountDownLatch(1);
        Runnable queuedWork = expiredRuns::incrementAndGet;
        RecoveryDispatchR965.submit(queuedWork, () -> { finishes.incrementAndGet(); queuedFinished.countDown(); }, 1500);
        Field mailboxField = RecoveryDispatchR965.class.getDeclaredField("MAILBOX");
        mailboxField.setAccessible(true);
        AtomicReferenceArray<?> mailbox = (AtomicReferenceArray<?>) mailboxField.get(null);
        Object queued = null;
        long findUntil = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (queued == null && System.nanoTime() < findUntil) {
            for (int i = 0; i < mailbox.length(); i++) {
                Object entry = mailbox.get(i);
                if (entry == null) continue;
                Field work = entry.getClass().getDeclaredField("work"); work.setAccessible(true);
                Field claimed = entry.getClass().getDeclaredField("claimed"); claimed.setAccessible(true);
                if (work.get(entry) == queuedWork && ((AtomicBoolean) claimed.get(entry)).get()) queued = entry;
            }
            Thread.yield();
        }
        require(queued != null, "PRODUCTION_REQUEST_WAS_QUEUED");
        require(pumpInFinish.await(2, TimeUnit.SECONDS), "PUMP_DELAYED_BY_OTHER_COMPLETION");
        Field deadline = queued.getClass().getDeclaredField("deadline"); deadline.setAccessible(true);
        long until = deadline.getLong(queued);
        while (android.os.SystemClock.elapsedRealtime() <= until) Thread.sleep(1);
        releaseWork.countDown();
        try {
            require(queuedFinished.await(2, TimeUnit.SECONDS), "EXPIRED_REQUEST_FINISHED");
            require(expiredRuns.get() == 0, "EXPIRED_QUEUED_WORK_MUST_NOT_EXECUTE");
            require(finishes.get() == 1, "EXPIRED_COMPLETION_ONCE");
        } finally { releasePump.countDown(); }
        CountDownLatch resumed = new CountDownLatch(1);
        RecoveryDispatchR965.submit(resumed::countDown, () -> {}, 1000);
        require(resumed.await(2, TimeUnit.SECONDS), "WORKER_RESUMES_AFTER_EXPIRY");
        System.out.println("PASS EXPIRED_QUEUED_WORK_RECHECK|finish=1|work=0|resumed=true|HOST_FRAMEWORK_ADAPTER_NOT_DEVICE");
    }
}
