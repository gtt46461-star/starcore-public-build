package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/** Pending B05-01 integration tests; not executed or counted as passing B03 evidence. */
public final class R30EvolutionConfirmIntegrationTest {
    private static EvolutionLoopR27 evolution(HoldoutLedgerR27.ConfirmPersistence persistence, R27BirthGateTest.Recorder sink) {
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(3000, 42, new int[0]);
        EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.nullControl = false;
        EvolutionLoopR27 loop = new EvolutionLoopR27(cfg, new FeatureCacheR27(data), sink, () -> 0L);
        assertTrue(loop.context().split.holdoutAvailable);
        loop.champion = R27ReplayTest.spec(PolicyAstR27.op("zscore", PolicyAstR27.leaf("trans1")), 2);
        loop.championTau = 1;
        loop.ledger().setConfirmPersistence(persistence);
        return loop;
    }

    @Test public void productionEvolutionMustCommitReservationBeforeAnyConfirmScore() {
        R27BirthGateTest.Recorder sink = new R27BirthGateTest.Recorder();
        EvolutionLoopR27 loop = evolution(read -> { throw new IllegalStateException("INJECTED_RESERVATION_FAILURE"); }, sink);
        try { loop.stepHoldout(); fail("evolution read confirmation despite failed reservation"); }
        catch (IllegalStateException e) { assertEquals("INJECTED_RESERVATION_FAILURE", e.getMessage()); }
        assertTrue("nothing published after reservation failure", sink.events.isEmpty());
    }

    @Test public void productionEvolutionRecordsTheConfirmRead() {
        AtomicInteger reserved = new AtomicInteger();
        R27BirthGateTest.Recorder sink = new R27BirthGateTest.Recorder();
        EvolutionLoopR27 loop = evolution(read -> reserved.incrementAndGet(), sink);
        loop.stepHoldout();
        SplitR27 split = loop.context().split;
        assertEquals(1, reserved.get());
        assertTrue(loop.ledger().confirmed(loop.champion.policyHash(), loop.context().data.datasetHash, split.holdoutBlock));
        assertTrue(sink.events.stream().anyMatch(e -> e.startsWith("S|")));
    }

}
