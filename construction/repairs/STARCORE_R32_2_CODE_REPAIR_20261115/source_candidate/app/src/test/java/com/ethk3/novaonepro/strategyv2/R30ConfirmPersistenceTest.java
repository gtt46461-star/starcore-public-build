package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public final class R30ConfirmPersistenceTest {
    @Test public void writerFailureDoesNotAuthorizeOrConsumeRead() {
        HoldoutLedgerR27 ledger = new HoldoutLedgerR27();
        ledger.setConfirmPersistence(read -> { throw new IllegalStateException("writer failed"); });
        try { ledger.confirm("p", "d", 7, 2, 8); fail("read authorized after write failure"); }
        catch (IllegalStateException e) { assertEquals("writer failed", e.getMessage()); }
        assertFalse(ledger.confirmed("p", "d", 7));
        ledger.setConfirmPersistence(read -> {});
        ledger.confirm("p", "d", 7, 2, 8);
        assertTrue(ledger.confirmed("p", "d", 7));
        assertTrue(ledger.drainUnsavedConfirmReads().isEmpty());
    }

    @Test public void pendingWriteDoesNotHoldLedgerMonitorAndRejectsConcurrentReuse() throws Exception {
        HoldoutLedgerR27 ledger = new HoldoutLedgerR27();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        ledger.setConfirmPersistence(read -> {
            entered.countDown();
            try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("timeout"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> ledger.confirm("p", "d", 7, 2, 8));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            Future<Boolean> second = pool.submit(() -> {
                try { ledger.confirm("p", "d", 7, 2, 8); return false; }
                catch (IllegalStateException e) { return e.getMessage().startsWith(HoldoutLedgerR27.CONFIRM_REUSED); }
            });
            assertTrue(second.get(3, TimeUnit.SECONDS));
            release.countDown(); first.get(5, TimeUnit.SECONDS);
            assertTrue(ledger.confirmed("p", "d", 7));
        } finally { release.countDown(); pool.shutdownNow(); }
    }

    @Test public void controlsStayOutsideMainExposureBudgetAfterRestore() {
        HoldoutLedgerR27 ledger = new HoldoutLedgerR27();
        ledger.confirm("control", "d", 7, 2, 8, true);
        assertEquals(0, ledger.exposures(7)); assertEquals(1, ledger.controlExposures(7));
        HoldoutLedgerR27 restored = new HoldoutLedgerR27();
        restored.restoreConfirm("control", "d", 7, 2, 8, 1, true);
        assertEquals(0, restored.exposures(7)); assertEquals(1, restored.controlExposures(7));
    }
}
