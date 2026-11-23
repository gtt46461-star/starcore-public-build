package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class QualificationCalendarBoundaryR34Test {
    private static SequentialQualificationR27.State state() {
        return new SequentialQualificationR27.State("registered", 1, 1.0);
    }

    private static QualificationInboxR29.Row settled(long ordinal) {
        return new QualificationInboxR29.Row(ordinal, Long.toString(ordinal), "FROZEN", 1, 1, 2, 3, 1, 2);
    }

    @Test public void resolvedHistoryBeforeRegistrationCannotEnterFutureEvidence() {
        List<QualificationInboxR29.Row> history = new ArrayList<>();
        for (long ordinal = 1000; ordinal < 1500; ordinal++) history.add(settled(ordinal));
        SequentialQualificationR27.State s = state();
        QualificationInboxR29.Plan p = QualificationInboxR29.plan(s, history, 1499, 500, 1500);
        assertEquals(0, s.n);
        assertEquals(0, s.acts);
        assertEquals(0, s.hits);
        assertEquals(-1, s.lastOrdinal);
        assertTrue(p.consumed.isEmpty());
        assertEquals(QualificationInboxR29.BLOCK_NOT_DUE, p.block);
        assertEquals(1500, p.blockedOrdinal);
    }

    @Test public void oldRowsCannotHideFirstEligibleReceiptOrCauseDuplicatesOnReplay() {
        List<QualificationInboxR29.Row> rows = new ArrayList<>();
        for (long ordinal = 1000; ordinal <= 1501; ordinal++) rows.add(settled(ordinal));
        SequentialQualificationR27.State s = state();
        QualificationInboxR29.Plan p = QualificationInboxR29.plan(s, rows, 1501, 16, 1500);
        assertEquals(2, p.consumed.size());
        assertEquals(1500, p.consumed.get(0).row.ordinal);
        assertEquals(1501, s.lastOrdinal);
        assertEquals(2, s.n);
        assertTrue(QualificationInboxR29.plan(s, rows, 1501, 16, 1500).consumed.isEmpty());
        assertEquals(2, s.n);
    }

    @Test public void missingFirstRegisteredOpportunityStopsAtThatExactPeriod() {
        SequentialQualificationR27.State s = state();
        QualificationInboxR29.Plan p = QualificationInboxR29.plan(s, Arrays.asList(settled(1499), settled(1501)), 1501, 16, 1500);
        assertEquals(QualificationInboxR29.BLOCK_MISSING_OPPORTUNITY, p.block);
        assertEquals(1500, p.blockedOrdinal);
        assertEquals(0, s.n);
        p = QualificationInboxR29.plan(s, Collections.emptyList(), 1499, 16, 1500);
        assertEquals(QualificationInboxR29.BLOCK_NOT_DUE, p.block);
        assertEquals(1500, p.blockedOrdinal);
    }

    @Test public void registeredButUnsettledActRemainsPending() {
        SequentialQualificationR27.State s = state();
        QualificationInboxR29.Row pending = new QualificationInboxR29.Row(1500, "1500", "FROZEN", 1, 1, 2, 3, -1, 2);
        QualificationInboxR29.Plan p = QualificationInboxR29.plan(s, Arrays.asList(settled(1499), pending, settled(1501)), 1501, 16, 1500);
        assertEquals(QualificationInboxR29.BLOCK_ACT_PENDING_SETTLE, p.block);
        assertEquals(1500, p.blockedOrdinal);
        assertEquals(0, s.n);
    }
}
