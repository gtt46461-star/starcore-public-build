package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/** Actual evolution phase, frozen confirmation, and once-only evidence reuse. */
public final class R30EvolutionConfirmIntegrationTest {
    static final class MemoryEvidence implements FrozenConfirmationR30.Evidence {
        final Map<String,ReplayEvaluatorR27.Trace> traces = new HashMap<>();
        public ReplayEvaluatorR27.Trace load(FrozenConfirmationR30.Task task, FrozenConfirmationR30.Policy p) { return traces.get(p.experimentId+"|"+task.datasetHash+"|"+task.block); }
        public void save(FrozenConfirmationR30.Task task, FrozenConfirmationR30.Policy p, ReplayEvaluatorR27.Trace trace) { traces.put(p.experimentId+"|"+task.datasetHash+"|"+task.block,trace); }
    }
    static EvolutionLoopR27 evolution() {
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(3000,42,new int[0]);
        EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.nullControl = false; cfg.births = 24; cfg.minPerFamily = 1;
        EvolutionLoopR27 loop = new EvolutionLoopR27(cfg,new FeatureCacheR27(data),new R27BirthGateTest.Recorder(),() -> 0L);
        assertTrue(loop.context().split.holdoutAvailable);
        loop.champion = new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32)),1,PolicySpecR27.Rule.MARGIN,0.01,0,1,false,0.02,0.01,
                IdeaFamilyR27.meta("I01","BIRTH",0,1,"confirmation fixture"));
        loop.championTau = 1; return loop;
    }

    @Test public void actualEvolutionCanRunWithUnreadableConfirmLabels() throws Exception {
        EvolutionLoopR27 loop = evolution(); EvalContextR27 ctx = loop.context(); int[][] labels = ctx.data.draws.clone();
        for (int t = ctx.split.selectTo; t < labels.length; t++) labels[t] = null;
        Field f = EvolutionLoopR27.class.getDeclaredField("ctx"); f.setAccessible(true);
        f.set(loop,new EvalContextR27(ctx.cache,ctx.split,labels));
        AtomicInteger reserved = new AtomicInteger(); loop.ledger().setConfirmPersistence(r -> reserved.incrementAndGet());
        loop.runGeneration();
        assertEquals(0,reserved.get()); assertEquals(0,loop.ledger().exposures(ctx.split.holdoutBlock));
        assertNotNull(loop.pendingConfirmation); assertEquals("FROZEN_PENDING_EXTERNAL",((Map<?,?>)loop.lastRecord.get("holdout")).get("status"));
    }

    @Test public void roleAndLowLevelReadsAreRejectedBeforeAnyReservationDuringEvolution() {
        EvolutionLoopR27 loop = evolution(); EvalContextR27 ctx = loop.context(); AtomicInteger reserved = new AtomicInteger();
        loop.ledger().setConfirmPersistence(r -> reserved.incrementAndGet()); Integer old = ReplayEvaluatorR27.enterEvolutionR30(ctx.split.selectTo);
        try {
            assertEquals("R30_CONFIRM_DURING_EVOLUTION",assertThrows(IllegalStateException.class,() -> ctx.holdoutTrace(loop.champion,1,loop.ledger())).getMessage());
            assertEquals("R30_CONFIRM_DURING_EVOLUTION",assertThrows(IllegalStateException.class,() -> ReplayEvaluatorR27.run(loop.champion,ctx.cache,ctx.labels,1,0,ctx.split.holdoutTo)).getMessage());
            assertEquals(0,reserved.get());
        } finally { ReplayEvaluatorR27.leaveEvolutionR30(old); }
    }

    @Test public void productionFreezeDoesNotReadAndIndependentStageCommitsBeforeScoring() {
        EvolutionLoopR27 loop = evolution();
        loop.ledger().setConfirmPersistence(r -> { throw new IllegalStateException("INJECTED_RESERVATION_FAILURE"); });
        loop.stepHoldout(); assertNotNull(loop.pendingConfirmation); MemoryEvidence evidence = new MemoryEvidence();
        assertEquals("INJECTED_RESERVATION_FAILURE",assertThrows(IllegalStateException.class,() ->
                FrozenConfirmationR30.evaluate(loop.pendingConfirmation,loop.context(),loop.ledger(),evidence)).getMessage());
        assertTrue(evidence.traces.isEmpty());
        AtomicInteger reserved = new AtomicInteger(); loop.ledger().setConfirmPersistence(r -> reserved.incrementAndGet());
        FrozenConfirmationR30.Result result = FrozenConfirmationR30.evaluate(loop.pendingConfirmation,loop.context(),loop.ledger(),new FrozenConfirmationR30.Evidence() {
            public ReplayEvaluatorR27.Trace load(FrozenConfirmationR30.Task t, FrozenConfirmationR30.Policy p) { return null; }
            public void save(FrozenConfirmationR30.Task t, FrozenConfirmationR30.Policy p, ReplayEvaluatorR27.Trace trace) {
                assertEquals(1,reserved.get()); assertTrue(loop.ledger().confirmed(p.experimentId,t.datasetHash,t.block)); assertEquals(t.to,trace.to);
            }
        });
        assertEquals(1,reserved.get()); assertTrue(result.report.get("championHoldout") instanceof Map);
    }

    @Test public void frozenParametersAndControlsCannotBeChangedByLaterEvolution() {
        EvolutionLoopR27 loop = evolution(); loop.stepHoldout(); FrozenConfirmationR30.Task task = loop.pendingConfirmation;
        String text = task.json(), spec = task.champion.spec().canonical();
        loop.champion.ast.kids[0].ints[0] = 10; loop.championTau = 7;
        PolicySpecR27 borrowed = task.champion.spec(); borrowed.ast.kids[0].ints[0] = 9;
        assertEquals(text,task.json()); assertEquals(spec,task.champion.spec().canonical());
        assertEquals(text,FrozenConfirmationR30.Task.parse(text).json()); assertEquals(1,task.champion.temperature,0);
    }

    @Test public void cachedConfirmationIsReusableButConsumedEvidenceCannotBeReopened() {
        EvolutionLoopR27 loop = evolution(); loop.stepHoldout(); MemoryEvidence evidence = new MemoryEvidence();
        AtomicInteger reserved = new AtomicInteger(); loop.ledger().setConfirmPersistence(r -> reserved.incrementAndGet());
        FrozenConfirmationR30.Result a = FrozenConfirmationR30.evaluate(loop.pendingConfirmation,loop.context(),loop.ledger(),evidence);
        EvalContextR27 ctx = loop.context(); int[][] labels = ctx.labels.clone();
        for (int t = ctx.split.selectTo; t < labels.length; t++) labels[t] = null;
        EvalContextR27 poisoned = new EvalContextR27(ctx.cache,ctx.split,labels);
        FrozenConfirmationR30.Result b = FrozenConfirmationR30.evaluate(loop.pendingConfirmation,poisoned,loop.ledger(),evidence);
        assertEquals(a.json(),b.json()); assertEquals(1,reserved.get()); evidence.traces.clear();
        assertTrue(assertThrows(IllegalStateException.class,() -> FrozenConfirmationR30.evaluate(loop.pendingConfirmation,poisoned,loop.ledger(),evidence)).getMessage().startsWith(HoldoutLedgerR27.CONFIRM_REUSED));
    }

    @Test public void identicalControlProgramsKeepTheirOwnFrozenLabelViewAndBudget() {
        EvolutionLoopR27 loop = evolution(); loop.cfg.nullControl = true; loop.i00Champion = loop.champion; loop.i00Tau = 1;
        loop.nullEvolution.champion = loop.champion; loop.nullEvolution.championTau = 1; loop.nullEvolution.lastSeed = 17;
        loop.stepHoldout(); FrozenConfirmationR30.Task task = loop.pendingConfirmation;
        MemoryEvidence evidence = new MemoryEvidence(); FrozenConfirmationR30.Result result = FrozenConfirmationR30.evaluate(task,loop.context(),loop.ledger(),evidence);
        assertFalse(result.verified); assertEquals(3,evidence.traces.size()); assertEquals(1,loop.ledger().exposures(task.block)); assertEquals(2,loop.ledger().controlExposures(task.block));
        assertNotEquals(task.champion.experimentId,task.i00.experimentId); assertNotEquals(task.i00.experimentId,task.nullPolicy.experimentId);
    }
}
