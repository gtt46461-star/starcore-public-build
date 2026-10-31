package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.List;

/** R27-030 shuffled-label negative-control evolution (docs/05 5.7). Same generators, gate, replay and halving as the
 * main loop with ~10% of its births; H_TRAIN/H_SELECT labels are permuted within segments with a new seed every
 * generation. Its champion is then scored on the real H_HOLDOUT next to the main champion. It must always run. */
public final class NullEvolutionR27 {
    public final List<SuccessiveHalvingR27.Candidate> population = new ArrayList<>();
    public PolicySpecR27 champion; public double championTau = 1;
    public int births, survivors;
    /** Seed of the latest label permutation (persisted so a resumed process scores the control identically). */
    public long lastSeed;

    public int[][] labels(FeatureCacheR27 cache, SplitR27 split) { return ReplayEvaluatorR27.shuffledLabels(cache.data, split.trainFrom, split.selectTo, lastSeed); }

    /** One control generation. Returns the control champion (may be null when nothing survives). */
    public PolicySpecR27 generation(FeatureCacheR27 cache, SplitR27 split, IdeaFamilyR27[] families, int n, StatsR27.SplitMix rng, int gen, int popCap) {
        lastSeed = rng.nextLong();
        int[][] shuffled = labels(cache, split);
        EvalContextR27 ctx = new EvalContextR27(cache, split, shuffled);
        BirthGateR27 gate = new BirthGateR27(cache, split.trainTo);
        IdeaFamilyR27.Context ic = new IdeaFamilyR27.Context(); ic.generation = gen; ic.champion = champion;
        List<SuccessiveHalvingR27.Candidate> cands = new ArrayList<>();
        // Population is re-scored on this generation's new label permutation.
        for (SuccessiveHalvingR27.Candidate c : population) { double tau = ctx.temperature(c.spec); cands.add(new SuccessiveHalvingR27.Candidate(c.spec, ctx.selectTrace(c.spec, tau), tau)); }
        for (int i = 0; i < n; i++) {
            IdeaFamilyR27 f = families[1 + rng.nextInt(families.length - 1)];
            PolicySpecR27 s = f.generate(rng, ic);
            if (s == null || !gate.check(s).ok) continue;
            births++;
            double tau = ctx.temperature(s); cands.add(new SuccessiveHalvingR27.Candidate(s, ctx.selectTrace(s, tau), tau));
        }
        SuccessiveHalvingR27.Result r = SuccessiveHalvingR27.run(cands, split);
        population.clear();
        r.survivors.sort((a, b) -> Double.compare(b.select.pairedUMean, a.select.pairedUMean));
        for (int i = 0; i < Math.min(popCap, r.survivors.size()); i++) population.add(r.survivors.get(i));
        survivors = population.size();
        if (!population.isEmpty()) { champion = population.get(0).spec; championTau = population.get(0).temperature; }
        return champion;
    }
}
