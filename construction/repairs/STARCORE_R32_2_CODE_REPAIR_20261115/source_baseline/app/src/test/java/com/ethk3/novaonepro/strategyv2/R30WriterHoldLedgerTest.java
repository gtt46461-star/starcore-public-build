package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;

/** R30-B01-01 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30WriterHoldLedgerTest {
    @Test public void ledgerPercentilesUseBucketUpperBounds() {
        WriterHoldLedgerR30.resetForTest();
        for (int i = 0; i < 95; i++) WriterHoldLedgerR30.record("R27_LIVE_SETTLE", false, 1, 3, false);
        for (int i = 0; i < 5; i++) WriterHoldLedgerR30.record("R27_LIVE_SETTLE", false, 1, 350, false);
        WriterHoldLedgerR30.Stat s = WriterHoldLedgerR30.snapshot().get(0);
        assertEquals(100, s.count());
        assertEquals(5, s.holdP(0.50));
        assertEquals(5, s.holdP(0.95));
        assertEquals(500, s.holdP(0.99));
        assertEquals(350, s.holdMax());
        assertEquals(5, s.overBudget());
        assertEquals(5, s.overHard());
    }

    @Test public void criticalWaitWithoutSamplesIsUnknownNotZero() {
        WriterHoldLedgerR30.resetForTest();
        WriterHoldLedgerR30.record("BACKGROUND", false, 900, 10, false);
        assertEquals(-1, WriterHoldLedgerR30.criticalWaitP95());
        WriterHoldLedgerR30.record("R914_DRAW_SAVE_ATOMIC", true, 40, 2, false);
        assertEquals(50, WriterHoldLedgerR30.criticalWaitP95());
        assertNull(WriterHoldLedgerR30.toMap(new WriterHoldLedgerR30.Stat("x", false)).get("holdP95"));
    }

    @Test public void criticalOperationsNeverCountAsOverBudget() {
        WriterHoldLedgerR30.resetForTest();
        WriterHoldLedgerR30.record("R914_DRAW_SAVE_ATOMIC", true, 0, 900, false);
        assertTrue(WriterHoldLedgerR30.offenders().isEmpty());
    }

    @Test public void offendersSortedByHardThenBudget() {
        WriterHoldLedgerR30.resetForTest();
        WriterHoldLedgerR30.record("A", false, 0, 60, false);
        WriterHoldLedgerR30.record("A", false, 0, 60, false);
        WriterHoldLedgerR30.record("B", false, 0, 250, false);
        List<WriterHoldLedgerR30.Stat> o = WriterHoldLedgerR30.offenders();
        assertEquals("B", o.get(0).operation);
        assertEquals("A", o.get(1).operation);
    }

    @Test public void ledgerIsThreadSafe() throws Exception {
        WriterHoldLedgerR30.resetForTest();
        final int threads = 8, per = 20000;
        final CountDownLatch go = new CountDownLatch(1), done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            final int id = t;
            new Thread(() -> {
                try { go.await(); for (int i = 0; i < per; i++) WriterHoldLedgerR30.record("OP" + (id % 2), id % 2 == 0, i % 7, i % 300, false); }
                catch (InterruptedException ignored) {} finally { done.countDown(); }
            }).start();
        }
        go.countDown(); done.await();
        long total = 0; for (WriterHoldLedgerR30.Stat s : WriterHoldLedgerR30.snapshot()) total += s.count();
        assertEquals((long) threads * per, total);
    }

    @Test public void operationNamesAreSanitizedAndBounded() {
        assertEquals("B|a_b_c", WriterHoldLedgerR30.normalize("a|b;c", false));
        assertEquals(66, WriterHoldLedgerR30.normalize(new String(new char[200]).replace('\0', 'x'), true).length());
    }
}
