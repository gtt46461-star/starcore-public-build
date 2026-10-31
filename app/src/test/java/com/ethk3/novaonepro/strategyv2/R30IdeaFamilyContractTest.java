package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public final class R30IdeaFamilyContractTest {
    @Test public void thirteenFamiliesHaveCompleteProposalsAndRealBirthRecords() {
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(1500, 77, new int[0]);
        Map<String,Integer> born = new TreeMap<>();
        R27BirthGateTest.Recorder sink = new R27BirthGateTest.Recorder() {
            @Override public void policy(PolicySpecR27 s, String state, String reason, int generation, String dataset) {
                super.policy(s, state, reason, generation, dataset);
                if ("BORN".equals(state)) born.merge(s.meta.idea, 1, Integer::sum);
            }
        };
        EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.nullControl = false;
        EvolutionLoopR27 loop = new EvolutionLoopR27(cfg, new FeatureCacheR27(data), sink, () -> 0L);
        loop.stepDataset(); IdeaFamilyR27.Context ctx = loop.ideaContext(); StatsR27.SplitMix rng = new StatsR27.SplitMix(301);
        Map<String,Integer> complete = new TreeMap<>();
        for (int f = 1; f <= 13; f++) {
            IdeaFamilyR27 family = EvolutionLoopR27.FAMILIES[f];
            for (int i = 0; i < 100; i++) {
                PolicySpecR27 s = family.propose(rng, ctx);
                assertTrue(family.id(), PolicyContractR30.violations(s).isEmpty());
                assertTrue(s.meta.reason.contains(family.coreIdea()));
                complete.merge(family.id(), 1, Integer::sum);
                if (i < 20) loop.addPendingForTest(s);
            }
        }
        loop.stepGate();
        assertEquals(13, complete.size());
        for (int f = 1; f <= 13; f++) {
            String id = String.format(java.util.Locale.ROOT, "I%02d", f);
            assertEquals(100, complete.get(id).intValue()); assertTrue(id + " has no real birth", born.getOrDefault(id, 0) > 0);
        }
        R27TestSupport.log("R30_FAMILY_CONTRACT complete=" + complete + " actualBirths=" + born);
    }

    @Test public void expertsPublishCompletePoliciesOrExplicitAuditReceipts() {
        StatsR27.SplitMix rng = new StatsR27.SplitMix(302); IdeaFamilyR27.Context ctx = new IdeaFamilyR27.Context();
        PolicySpecR27 a = new IdeaI01FrequencyReversionR27().propose(rng, ctx);
        PolicySpecR27 b = new IdeaI02GapCycleR27().propose(rng, ctx);
        ctx.champion = a; ctx.archive = Arrays.asList(a, b); ctx.legacySeeds = Collections.singletonList(a);
        Map<String,Integer> executable = new TreeMap<>(), audits = new TreeMap<>();
        for (String[] expert : ExpertProposalsR27.EXPERTS) for (int i = 0; i < 100; i++) {
            for (ExpertProposalsR27.Proposal proposal : ExpertProposalsR27.propose(expert[0], ctx, rng)) {
                if (proposal.spec == null) { assertFalse(proposal.hypothesis.isEmpty()); audits.merge(expert[0], 1, Integer::sum); }
                else { assertTrue(proposal.expert, PolicyContractR30.violations(proposal.spec).isEmpty()); executable.merge(expert[0], 1, Integer::sum); }
            }
        }
        assertEquals(12, executable.size()); assertEquals(4, audits.size());
        for (int n : executable.values()) assertEquals(100, n);
        R27TestSupport.log("R30_EXPERT_CONTRACT executable=" + executable + " audits=" + audits);
        PolicySpecR27 bad = new PolicySpecR27(null, 1, PolicySpecR27.Rule.ALWAYS, 0, 0, 1, false, 0, 0, null);
        ExpertProposalsR27.Proposal rejected = ExpertProposalsR27.one("E08", "fixture", "h", "s", "m", "r", bad).get(0);
        assertNull(rejected.spec); assertTrue(rejected.hypothesis.startsWith("R30_CONTRACT:"));
    }
}
