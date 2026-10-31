package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R30 shuffled-sequence control uses the exact main evolution implementation, with recursive controls disabled.
 * Its frozen policy is compared outside evolution. Synthetic qualification/power is reported separately in Q08. */
public final class NullEvolutionR27 {
    public final List<SuccessiveHalvingR27.Candidate> population = new ArrayList<>();
    public PolicySpecR27 champion; public double championTau = 1;
    public int births, survivors;
    public long lastSeed;
    private String fullSnapshot;
    private final Map<String,Long> receipts = new LinkedHashMap<>();
    private Map<String,Object> lastGeneration = new LinkedHashMap<>();
    /** Explicit seed assets for synthetic benchmarks; ordinary controls retain their own restored assets. */
    public List<PolicySpecR27> seedAssets = Collections.emptyList();

    public int[][] labels(FeatureCacheR27 cache, SplitR27 split) { return ReplayEvaluatorR27.shuffledLabels(cache.data,split.trainFrom,split.selectTo,lastSeed); }

    public PolicySpecR27 generation(FeatureCacheR27 cache, SplitR27 split, IdeaFamilyR27[] families, int n, StatsR27.SplitMix rng, int gen, int popCap) {
        if (families != EvolutionLoopR27.FAMILIES) throw new IllegalArgumentException("R30_CONTROL_REQUIRES_MAIN_FAMILY_REGISTRY");
        lastSeed = rng.nextLong();
        ReplayDatasetR27 permuted = ReplayEvaluatorR27.shuffledDataset(cache.data,split.trainFrom,split.selectTo,lastSeed);
        FeatureCacheR27 controlCache = new FeatureCacheR27(permuted);
        EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.births = Math.max(1,n); cfg.minPerFamily = 0;
        cfg.nullControl = false; cfg.i00PerGeneration = 0; cfg.populationCap = popCap; cfg.seed = lastSeed;
        cfg.expertShare = n < 10 ? 0 : 0.10;
        ControlSink sink = new ControlSink();
        EvolutionLoopR27 loop = fullSnapshot == null ? new EvolutionLoopR27(cfg,controlCache,sink,() -> 0L) :
                EvolutionLoopR27.restore(fullSnapshot,cfg,controlCache,sink,() -> 0L);
        List<PolicySpecR27> seeds = new ArrayList<>(seedAssets);
        if (fullSnapshot == null) {
            for (SuccessiveHalvingR27.Candidate c : population) seeds.add(c.spec);
            if (champion != null) { seeds.add(champion); loop.champion = champion; loop.championTau = loop.context().temperature(champion); }
        }
        loop.legacySeeds = seeds;
        loop.runGeneration(); // same mutation/crossover/fusion, contract, dedup, halving, archive and all nine steps
        fullSnapshot = loop.snapshot(); champion = loop.champion;
        championTau = champion == null ? 1 : loop.context().temperature(champion);
        population.clear();
        for (EvolutionLoopR27.Member member : loop.population) {
            ReplayEvaluatorR27.Trace trace = member.trace == null ? loop.context().selectTrace(member.spec,member.tau) : member.trace;
            SuccessiveHalvingR27.Candidate c = new SuccessiveHalvingR27.Candidate(member.spec,trace,member.tau);
            c.select = ReplayScoreR27.of(SplitR27.SELECT,trace,loop.context().split.selectFrom,loop.context().split.selectTo); population.add(c);
        }
        survivors = population.size(); return champion;
    }
    public Map<String,Object> json() {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("mode","SYNTHETIC_SHUFFLED_TRAIN_SELECT_SEQUENCE");
        m.put("implementation","EvolutionLoopR27 all nine steps; recursive controls disabled"); m.put("receipts",new LinkedHashMap<>(receipts));
        Map<String,Object> logical = new LinkedHashMap<>(lastGeneration);
        for(String key:new String[]{"replayMs","candidateReplayP95Ms","startedAt","finishedAt"})logical.remove(key);
        m.put("acceptedBirths",(long)births); m.put("survivors",(long)survivors); m.put("lastGeneration",logical);
        m.put("naturalQualityEligible",false); return m;
    }
    public String snapshot() {
        if (fullSnapshot == null) return null;
        Map<String,Object> m = new LinkedHashMap<>(json()); m.put("loopSnapshot",fullSnapshot); return JsonR27.write(m);
    }
    @SuppressWarnings("unchecked") public void restoreFull(String text) {
        if (text == null) return; Map<String,Object> m = (Map<String,Object>)JsonR27.parse(text);
        fullSnapshot = (String)m.get("loopSnapshot"); births = (int)JsonR27.num(m.get("acceptedBirths")); survivors = (int)JsonR27.num(m.get("survivors"));
        receipts.clear(); for (Map.Entry<String,Object> e : ((Map<String,Object>)m.get("receipts")).entrySet()) receipts.put(e.getKey(),((Number)e.getValue()).longValue());
        lastGeneration = new LinkedHashMap<>((Map<String,Object>)m.get("lastGeneration"));
    }
    private void count(String event,long n) { receipts.put(event,receipts.getOrDefault(event,0L)+n); }
    EvolutionLoopR27.Sink countingSink() { return new ControlSink(); }
    private final class ControlSink implements EvolutionLoopR27.Sink {
        public void policy(PolicySpecR27 s,String state,String reason,int generation,String dataset) {
            count("policy:"+state,1); if ("BORN".equals(state)) { births++; count("bornOperator:"+s.meta.operator,1); }
        }
        public void lineage(String child,String parent,String role,String op,int generation) { count("lineage:"+role,1); }
        public void score(String policy,String dataset,ReplayScoreR27 score) { count("scores:"+score.split,1); }
        public void generation(Map<String,Object> record) { lastGeneration = new LinkedHashMap<>(record); survivors=(int)JsonR27.num(record.get("population")); count("completedGenerations",1); }
        public void promote(PolicySpecR27 s,double tau,int gen,String reason) { throw new IllegalStateException("R30_CONTROL_MUST_NEVER_PROMOTE"); }
        public void demote(String hash,String reason) { count("demotions",1); }
        public void expert(ExpertProposalsR27.Proposal proposal,int generation) { count("expertReceipts",1); if (proposal.spec != null) count("expertExecutableProposals",1); }
        public void ablation(int generation,String hash,List<Object[]> rows) { count("ablationRows",rows.size()); }
        public void holdoutLook(long block,String hash) { throw new IllegalStateException("R30_CONTROL_CONFIRM_DURING_EVOLUTION"); }
        public void behaviorDuplicate(BehaviorSignatureR30.Duplicate duplicate) { count("behaviorDuplicates:"+duplicate.stage,1); }
    }
}
