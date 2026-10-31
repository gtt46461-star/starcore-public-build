package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** R30-B04-01 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30PolicyContractTest {
    static PolicySpecR27 policy(PolicyAstR27.Node ast, PolicySpecR27.Rule rule, PolicySpecR27.Meta meta) {
        return new PolicySpecR27(ast, 3, rule, 0.02, 0.15, 1.0, true, 0.01, 0.001, meta);
    }
    static final PolicySpecR27.Meta META = new PolicySpecR27.Meta("I01", "BIRTH", Arrays.asList("p0"), 1, 7L, "频率×遗漏组合");

    @Test public void completePolicyPasses() {
        PolicyAstR27.Node ast = PolicyAstR27.op("add", PolicyAstR27.leaf("freq", 3, 32), PolicyAstR27.leaf("freqnb", 3, 128));
        List<String> v = PolicyContractR30.violations(policy(ast, PolicySpecR27.Rule.MARGIN, META));
        assertTrue(v.toString(), v.isEmpty());
    }
    @Test public void emptyShellsRejected() {
        assertTrue(PolicyContractR30.violations(policy(PolicyAstR27.constant(1), PolicySpecR27.Rule.MARGIN, META)).contains("PROGRAM_CONSTANT_ONLY"));
        assertTrue(PolicyContractR30.violations(policy(PolicyAstR27.leaf("freq", 3, 32), PolicySpecR27.Rule.MARGIN, null)).contains("PROVENANCE_MISSING"));
        assertTrue(PolicyContractR30.violations(policy(PolicyAstR27.leaf("freq", 3, 33), PolicySpecR27.Rule.MARGIN, META)).get(0).startsWith("PROGRAM_RANGE"));
        assertTrue(PolicyContractR30.violations(new PolicySpecR27(PolicyAstR27.leaf("freq", 3, 32), 11, PolicySpecR27.Rule.ALWAYS, 0, 0, 1, true, 0.01, 0, META)).contains("SELECTOR_POSITION:11"));
        assertTrue(PolicyContractR30.controlOnly(policy(PolicyAstR27.leaf("freq", 3, 32), PolicySpecR27.Rule.RANDOM, META)));
    }

    @Test public void mainBirthIntakeRejectsShellsAndPreservesReasonsAcrossRestart() {
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(1500, 17, new int[0]);
        R27BirthGateTest.Recorder sink = new R27BirthGateTest.Recorder();
        EvolutionLoopR27.Config config = new EvolutionLoopR27.Config(); config.nullControl = false;
        EvolutionLoopR27 loop = new EvolutionLoopR27(config, new FeatureCacheR27(data), sink, () -> 0L);
        loop.stepDataset();
        loop.addPendingForTest(null);
        loop.addPendingForTest(policy(null, PolicySpecR27.Rule.MARGIN, META));
        loop.addPendingForTest(policy(PolicyAstR27.leaf("freq", 3, 32), null, META));
        loop.addPendingForTest(policy(PolicyAstR27.constant(1), PolicySpecR27.Rule.MARGIN, META));
        loop.addPendingForTest(policy(PolicyAstR27.leaf("freq", 3, 32), PolicySpecR27.Rule.MARGIN, null));
        loop.addPendingForTest(policy(PolicyAstR27.leaf("freq"), PolicySpecR27.Rule.MARGIN, META));
        loop.addPendingForTest(policy(PolicyAstR27.leaf("freq", 3, 32), PolicySpecR27.Rule.RANDOM, META));
        loop.stepGate(); loop.stepRecord();
        assertEquals(7L, ((Number) loop.lastRecord.get("rejected")).longValue());
        assertTrue(sink.events.stream().noneMatch(e -> e.endsWith("|BORN")));
        @SuppressWarnings("unchecked") java.util.Map<String, Object> reasons = (java.util.Map<String, Object>) loop.lastRecord.get("rejectReasons");
        assertEquals(7, reasons.values().stream().mapToInt(x -> ((Number)x).intValue()).sum());
        assertTrue(reasons.keySet().stream().allMatch(x -> x.startsWith("R30_CONTRACT:")));
        EvolutionLoopR27 restored = EvolutionLoopR27.restore(loop.snapshot(), config, new FeatureCacheR27(data), sink, () -> 0L);
        restored.stepRecord();
        assertEquals(JsonR27.write(reasons), JsonR27.write(restored.lastRecord.get("rejectReasons")));
        R27TestSupport.log("R30_CONTRACT_INTAKE rejected=7 born=0 reasons=" + reasons);
    }

    @Test public void completeCandidateIsBornAndExplicitControlRemainsExecutable() {
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(1500, 19, new int[0]);
        FeatureCacheR27 cache = new FeatureCacheR27(data);
        R27BirthGateTest.Recorder sink = new R27BirthGateTest.Recorder();
        EvolutionLoopR27.Config config = new EvolutionLoopR27.Config(); config.nullControl = false;
        EvolutionLoopR27 loop = new EvolutionLoopR27(config, cache, sink, () -> 0L);
        PolicySpecR27 complete = policy(PolicyAstR27.leaf("freq", 3, 32), PolicySpecR27.Rule.MARGIN, META);
        loop.stepDataset(); loop.addPendingForTest(complete); loop.stepGate();
        assertEquals("BORN", loop.pendingState(complete.policyHash()));
        assertTrue(sink.events.contains("P|" + complete.policyHash() + "|BORN"));
        PolicySpecR27 control = new IdeaI00RandomBaselineR27().generate(new StatsR27.SplitMix(1), new IdeaFamilyR27.Context());
        BirthGateR27 gate = new BirthGateR27(cache, loop.context().split.trainTo);
        assertTrue(gate.check(control).ok);
        assertFalse(gate.checkCandidate(control).ok);
    }
}
