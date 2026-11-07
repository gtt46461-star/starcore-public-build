package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.Map;
import org.junit.Test;

/** R31-B02-C01#A3: the writer report has per-operation count/waitP95/holdP95/max/overBudget/rolledBack,
 * and a soft-budget breach names the concrete owner file to fix. */
public final class R31WriterReportColumnsTest {
    private static void slowOwnerWrite() {
        WriterHoldLedgerR30.record("R31_REPORT_COLUMNS_SLOW", false, 7, 80, false, "COMMITTED");
        WriterHoldLedgerR30.record("R31_REPORT_COLUMNS_SLOW", false, 3, 12, true, "ROLLED_BACK");
    }
    @Test public void perOperationColumnsAndBudgetOwnerAreReported() {
        WriterHoldLedgerR30.resetForTest();
        slowOwnerWrite();
        WriterHoldLedgerR30.record("R31_REPORT_COLUMNS_FAST", false, 0, 1, false, "COMMITTED");
        Map<String, Object> slow = null, fast = null;
        for (WriterHoldLedgerR30.Stat stat : WriterHoldLedgerR30.snapshot()) {
            if (stat.operation.equals("R31_REPORT_COLUMNS_SLOW")) slow = WriterHoldLedgerR30.toMap(stat);
            if (stat.operation.equals("R31_REPORT_COLUMNS_FAST")) fast = WriterHoldLedgerR30.toMap(stat);
        }
        assertNotNull(slow); assertNotNull(fast);
        for (String column : new String[]{"count", "waitP95", "holdP95", "holdMax", "waitMax", "overBudget50ms", "overHard200ms", "rolledBack", "failures"})
            assertTrue("column " + column, slow.containsKey(column));
        assertEquals(2L, ((Number) slow.get("count")).longValue());
        assertEquals(80L, ((Number) slow.get("holdMax")).longValue());
        assertEquals(1L, ((Number) slow.get("overBudget50ms")).longValue());
        assertEquals(1L, ((Number) slow.get("rolledBack")).longValue());
        assertEquals(1L, ((Number) slow.get("failures")).longValue());
        String owner = String.valueOf(slow.get("overBudgetOwner"));
        assertTrue("budget breach names its owner file: " + owner, owner.startsWith("R31WriterReportColumnsTest.java#slowOwnerWrite"));
        assertNull("no owner without a breach", fast.get("overBudgetOwner"));
        WriterHoldLedgerR30.resetForTest();
    }
}
