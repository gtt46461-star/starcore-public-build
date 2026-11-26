package com.ethk3.novaonepro;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ReportSchemaSourceWaitTest {
    @Test public void readsACompletedOwnerReply() throws Exception {
        ThreadPoolExecutor executor = ReportSchemaSourceProviderR335.newOwnerCallExecutor();
        try {
            assertEquals("available", ReportSchemaSourceProviderR335.awaitOwnerCall(executor,
                    () -> "available", () -> { }, 5000));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test public void expiredCallCannotTrapExportOrQueueAnotherOwnerRequest() throws Exception {
        ThreadPoolExecutor executor = ReportSchemaSourceProviderR335.newOwnerCallExecutor();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            TimeoutException failure = assertThrows(TimeoutException.class,
                    () -> ReportSchemaSourceProviderR335.awaitOwnerCall(executor, () -> {
                        entered.countDown();
                        awaitRelease(release);
                        return "late";
                    }, () -> { }, 1000));
            assertEquals("REGISTERED_SOURCE_OWNER_CALL_TIMED_OUT", failure.getMessage());
            assertEquals(0L, entered.getCount());
            assertThrows(RejectedExecutionException.class,
                    () -> ReportSchemaSourceProviderR335.awaitOwnerCall(executor, () -> {
                        throw new AssertionError("Second owner request was accepted");
                    }, () -> { }, 5000));
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test public void observesExportCancellationDuringAnOutstandingOwnerCall() throws Exception {
        ThreadPoolExecutor executor = ReportSchemaSourceProviderR335.newOwnerCallExecutor();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        IOException cancelled = new IOException("REPORT_CANCELLED");
        try {
            IOException failure = assertThrows(IOException.class,
                    () -> ReportSchemaSourceProviderR335.awaitOwnerCall(executor, () -> {
                        entered.countDown();
                        awaitRelease(release);
                        return "late";
                    }, () -> {
                        if (entered.getCount() == 0) throw cancelled;
                    }, 5000));
            assertSame(cancelled, failure);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static void awaitRelease(CountDownLatch release) {
        while (true) {
            try {
                release.await();
                return;
            } catch (InterruptedException ignored) {
                // A synchronous Binder call may keep running after the exporting thread cancels its wait.
            }
        }
    }
}
