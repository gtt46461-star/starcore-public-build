package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Q08 synthetic experiments only. Uses the actual evolution and qualification engines. Each work unit is one
 * power trial, one native evolution step, or one frozen FUTURE replay; state is serializable between units. */
public final class NullPipelineSimulationR30 {
    public static final int POWER_REPS=20, POWER_PERIODS=5000, NULL_TRIALS=3, WARM_ROWS=3000, FUTURE_ROWS=6000;
    public static final double[] EDGES={0.34,0.37,0.40};
    private int powerRate, powerRep, trial;
    private long powerRng=7;
    private final List<Map<String,Object>> power=new ArrayList<>(), nullRuns=new ArrayList<>();
    private String loopSnapshot, auditSnapshot;
    private transient EvolutionLoopR27 loop;
    private transient NullEvolutionR27 audit;
    private final String sourceCommit;
    public NullPipelineSimulationR30() { this("HOST_UNBOUND"); }
    public NullPipelineSimulationR30(String sourceCommit) { this.sourceCommit=sourceCommit; }
    public String sourceCommit() { return sourceCommit; }

    public boolean complete() { return powerRate==EDGES.length && trial==NULL_TRIALS; }
    public boolean cheapWorkPending() { return powerRate<EDGES.length; }
    public boolean step() {
        if(complete())return false;
        if(powerRate<EDGES.length){powerTrial();return true;}
        EvolutionLoopR27 engine=engine();
        if(engine.generation<3){engine.step();loopSnapshot=engine.snapshot();auditSnapshot=JsonR27.write(audit.json());return true;}
        qualifyFrozen(engine);trial++;loop=null;audit=null;loopSnapshot=null;auditSnapshot=null;return true;
    }
    private void powerTrial() {
        double edge=EDGES[powerRate];StatsR27.SplitMix rng=new StatsR27.SplitMix(0);rng.state=powerRng;
        SequentialQualificationR27.State state=new SequentialQualificationR27.State("Q08_POWER_"+powerRate+"_"+powerRep,1,1);
        for(long t=1;t<=POWER_PERIODS&&SequentialQualificationR27.ACCUMULATING.equals(state.state);t++)state.observe(t,true,rng.nextDouble()<edge);
        Map<String,Object> row=new LinkedHashMap<>();row.put("trueHitProbability",edge);row.put("rep",(long)powerRep);row.put("periods",state.n);
        row.put("state",state.state);row.put("detected",SequentialQualificationR27.QUALIFIED.equals(state.state));power.add(row);powerRng=rng.state;
        if(++powerRep==POWER_REPS){powerRate++;powerRep=0;powerRng=7;}
    }
    static PolicySpecR27 neutralSeed() {
        return new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32)),1,PolicySpecR27.Rule.QUANTILE,0,0.15,0.05,true,0,0,
                IdeaFamilyR27.meta("I12","LEGACY_SEED",0,1,"Q08 synthetic neutral calibrated seed; all ordinary birth/selection gates apply"));
    }
    private ReplayDatasetR27 warm() {
        ReplayDatasetR27 raw=ReplayDatasetR27.synthetic(WARM_ROWS,8200+trial,new int[0]);SplitR27 split=SplitR27.of(raw,null);
        return ReplayEvaluatorR27.shuffledDataset(raw,split.trainFrom,split.selectTo,9100+trial);
    }
    private EvolutionLoopR27 engine() {
        if(loop!=null)return loop;
        audit=new NullEvolutionR27();if(auditSnapshot!=null)audit.restoreFull(auditSnapshot);
        EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.births=40;cfg.minPerFamily=1;cfg.nullControl=false;cfg.i00PerGeneration=0;cfg.seed=9300+trial;
        FeatureCacheR27 cache=new FeatureCacheR27(warm());
        loop=loopSnapshot==null?new EvolutionLoopR27(cfg,cache,audit.countingSink(),()->0L):EvolutionLoopR27.restore(loopSnapshot,cfg,cache,audit.countingSink(),()->0L);
        loop.legacySeeds=Collections.singletonList(neutralSeed());return loop;
    }
    private void qualifyFrozen(EvolutionLoopR27 engine) {
        Map<String,Object> row=new LinkedHashMap<>();row.put("trial",(long)trial);row.put("mode","SYNTHETIC_RANDOM_PERMUTATION_ONLY");
        row.put("pipeline",audit.json());row.put("naturalQualityEligible",false);
        if(engine.population.isEmpty()){
            row.put("status","NO_ELIGIBLE_CANDIDATE");row.put("falseSequentialQualification",false);row.put("futurePeriods",0L);nullRuns.add(row);return;
        }
        EvolutionLoopR27.Member selected=engine.population.get(0);
        PolicySpecR27 frozen=PolicySpecR27.parse(selected.spec.fullJson());double tau=selected.tau;
        ReplayDatasetR27 warm=engine.context().data,raw=ReplayDatasetR27.synthetic(WARM_ROWS+FUTURE_ROWS,8200+trial,new int[0]);
        int[][] joined=raw.draws.clone();for(int t=0;t<WARM_ROWS;t++)joined[t]=warm.draws[t].clone();ReplayDatasetR27 future=raw.withDraws(joined);
        ReplayEvaluatorR27.Trace trace=ReplayEvaluatorR27.run(frozen,new FeatureCacheR27(future),future.draws,tau,WARM_ROWS,future.size());
        SequentialQualificationR27.State state=new SequentialQualificationR27.State("Q08_NULL_"+trial,trial+1,SelectorPolicyR30.registeredRate(frozen));
        for(int t=WARM_ROWS;t<future.size()&&SequentialQualificationR27.ACCUMULATING.equals(state.state);t++)state.observe(future.ordinals[t],!trace.skipped[t]&&trace.act[t],trace.hit[t]>0);
        row.put("status","MEASURED");row.put("policyHash",frozen.policyHash());row.put("temperature",tau);row.put("frozenThrough",warm.ordinals[WARM_ROWS-1]);
        row.put("futureFrom",future.ordinals[WARM_ROWS]);row.put("futurePeriods",state.n);row.put("qualification",state.json());
        row.put("falseSequentialQualification",SequentialQualificationR27.QUALIFIED.equals(state.state));
        row.put("endpoint","SEQUENTIAL_EVIDENCE; controls never receive a REAL certificate");nullRuns.add(row);
    }
    public Map<String,Object> summary() {
        Map<String,Object> out=new LinkedHashMap<>();out.put("metric","Q08");out.put("status",complete()?"MEASURED":"RUNNING");
        out.put("sourceCommit",sourceCommit);
        out.put("evidenceLevel","SYNTHETIC_MECHANISM_TEST");out.put("naturalQualityEligible",false);out.put("mainAlphaBudgetSpent",false);
        out.put("powerIndex","index 1 in each independent synthetic experiment; no shared real budget");
        List<Object> groups=new ArrayList<>();
        for(double edge:EDGES){
            List<Long> detected=new ArrayList<>();int reps=0,censored=0,notBetter=0;
            for(Map<String,Object> r:power)if(((Number)r.get("trueHitProbability")).doubleValue()==edge){reps++;if(Boolean.TRUE.equals(r.get("detected")))detected.add(((Number)r.get("periods")).longValue());
                else if(SequentialQualificationR27.NOT_BETTER.equals(r.get("state")))notBetter++;else censored++;}
            Collections.sort(detected);Map<String,Object> g=new LinkedHashMap<>();g.put("trueHitProbability",edge);g.put("experiments",(long)reps);g.put("detected",(long)detected.size());
            g.put("notBetter",(long)notBetter);g.put("censoredAt5000",(long)censored);g.put("detectionPeriods",detected);
            g.put("medianDetectedPeriods",detected.isEmpty()?null:detected.get((detected.size()-1)/2));g.put("p95DetectedPeriods",detected.isEmpty()?null:detected.get(Math.min(detected.size()-1,(int)Math.ceil(detected.size()*0.95)-1)));groups.add(g);
        }
        out.put("power",groups);out.put("fullPipelineTrials",new ArrayList<>(nullRuns));long eligible=0,falseQualified=0;
        for(Map<String,Object> row:nullRuns){if("MEASURED".equals(row.get("status")))eligible++;if(Boolean.TRUE.equals(row.get("falseSequentialQualification")))falseQualified++;}
        out.put("eligibleNullCandidates",eligible);out.put("falseSequentialQualifications",eligible==0?null:falseQualified);
        out.put("nullEvidenceStatus",eligible==0?"INSUFFICIENT":"MEASURED");return out;
    }
    public String snapshot() {
        Map<String,Object> m=new LinkedHashMap<>();m.put("version",1L);m.put("powerRate",(long)powerRate);m.put("powerRep",(long)powerRep);m.put("powerRng",powerRng);
        m.put("sourceCommit",sourceCommit);
        m.put("trial",(long)trial);m.put("power",power);m.put("nullRuns",nullRuns);m.put("loopSnapshot",loopSnapshot);m.put("auditSnapshot",auditSnapshot);return JsonR27.write(m);
    }
    @SuppressWarnings("unchecked") public static NullPipelineSimulationR30 restore(String text) {
        if(text==null)return new NullPipelineSimulationR30();Map<String,Object> m=(Map<String,Object>)JsonR27.parse(text);
        NullPipelineSimulationR30 state=new NullPipelineSimulationR30(m.get("sourceCommit") instanceof String ? (String)m.get("sourceCommit") : "HOST_UNBOUND");
        if(JsonR27.num(m.get("version"))!=1)throw new IllegalArgumentException("R30_Q08_CHECKPOINT_VERSION");
        state.powerRate=(int)JsonR27.num(m.get("powerRate"));state.powerRep=(int)JsonR27.num(m.get("powerRep"));state.powerRng=((Number)m.get("powerRng")).longValue();state.trial=(int)JsonR27.num(m.get("trial"));
        for(Object row:(List<Object>)m.get("power"))state.power.add((Map<String,Object>)row);for(Object row:(List<Object>)m.get("nullRuns"))state.nullRuns.add((Map<String,Object>)row);
        state.loopSnapshot=(String)m.get("loopSnapshot");state.auditSnapshot=(String)m.get("auditSnapshot");return state;
    }
}
