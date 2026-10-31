package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public final class R30StructureRegistryTest {
    @Test public void sevenRegisteredFamiliesEachProduceFiveHundredCompleteContracts() {
        assertEquals(21, EvolutionLoopR27.FAMILIES.length); assertEquals(20, IslandModelR27.ISLANDS.length);
        assertEquals(6, StructureGeneratorRegistryR30.ALL.length);
        StatsR27.SplitMix rng = new StatsR27.SplitMix(306); IdeaFamilyR27.Context ctx = new IdeaFamilyR27.Context();
        Map<String,Integer> complete = new TreeMap<>();
        for (int f = 14; f <= 20; f++) {
            IdeaFamilyR27 family = EvolutionLoopR27.FAMILIES[f]; assertEquals(String.format(Locale.ROOT,"I%02d",f), family.id());
            assertEquals(f - 1, IslandModelR27.index(family.id()));
            for (int i = 0; i < 500; i++) {
                PolicySpecR27 s = family.propose(rng, ctx); assertTrue(family.id(), PolicyContractR30.violations(s).isEmpty());
                complete.merge(family.id(), 1, Integer::sum);
                if (i < 50) {
                    PolicySpecR27 mutated = family.mutate(s, rng, ctx);
                    if (mutated != null) assertTrue(family.id(), PolicyContractR30.violations(mutated).isEmpty());
                }
            }
        }
        assertEquals(7, complete.size()); R27TestSupport.log("R30_NEW_FAMILY_CONTRACT " + complete);
    }

    @Test public void realMainBirthLoopProducesEveryNewFamily() {
        Map<String,Integer> births = new TreeMap<>();
        R27BirthGateTest.Recorder sink = new R27BirthGateTest.Recorder() {
            @Override public void policy(PolicySpecR27 s, String state, String reason, int generation, String dataset) {
                super.policy(s,state,reason,generation,dataset);
                if ("BORN".equals(state)) births.merge(s.meta.idea, 1, Integer::sum);
            }
        };
        EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.births = 80; cfg.minPerFamily = 3; cfg.nullControl = false; cfg.i00PerGeneration = 0; cfg.seed = 307;
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(1800, 9, new int[0]);
        EvolutionLoopR27 loop = new EvolutionLoopR27(cfg, new FeatureCacheR27(data), sink, () -> 0L);
        loop.step(); loop.step(); loop.step(); // actual DATASET -> BIRTH -> GATE, not manual pending injection
        for (int f = 14; f <= 20; f++) {
            String id = String.format(Locale.ROOT,"I%02d",f); assertTrue(id + " absent from actual births: " + births, births.getOrDefault(id, 0) > 0);
        }
        R27TestSupport.log("R30_NEW_FAMILY_ACTUAL_BIRTHS " + births);
    }

    @Test public void intersectionPositionSelectionCannotUseConfirmOrFutureRows() {
        ReplayDatasetR27 a = ReplayDatasetR27.synthetic(3000, 39, new int[0]); SplitR27 split = SplitR27.of(a, null);
        assertTrue(split.holdoutAvailable);
        List<ReplayDatasetR27.Row> rows = new ArrayList<>();
        for (int t = 0; t < a.size(); t++) {
            int[] draw = a.draws[t].clone();
            if (t >= split.selectTo) for (int p = 0; p < 10; p++) draw[p] = 11 - draw[p];
            rows.add(new ReplayDatasetR27.Row(a.issues[t], a.ordinals[t], draw));
        }
        ReplayDatasetR27 b = ReplayDatasetR27.of(rows);
        IdeaFamilyR27.Context ca = new IdeaFamilyR27.Context(), cb = new IdeaFamilyR27.Context();
        ca.evaluation = new EvalContextR27(new FeatureCacheR27(a), split, null);
        cb.evaluation = new EvalContextR27(new FeatureCacheR27(b), SplitR27.of(b, null), null);
        IdeaFamilyR27 family = new StructureGeneratorRegistryR30.ConditionalIntersection();
        assertEquals(family.propose(new StatsR27.SplitMix(7),ca).canonical(), family.propose(new StatsR27.SplitMix(7),cb).canonical());
    }
}
