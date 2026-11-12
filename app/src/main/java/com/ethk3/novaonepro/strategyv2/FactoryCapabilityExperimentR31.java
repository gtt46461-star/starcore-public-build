package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Resumable isolated experiment using the actual factory (family/expert birth,
 * mutation/fusion, birth gate, selection, independent confirmation and unchanged
 * certificate gates). The old four-program kernel experiment remains separate.
 * One step is checkpointed by the existing fenced HEALTH writer. */
public final class FactoryCapabilityExperimentR31 implements AutoCloseable {
    public static final String VERSION="R31_FULL_FACTORY_EXPERIMENT_1",NAMESPACE="SYNTHETIC_FULL_FACTORY_R31";
    public static final int REPLICATES=20,WARM=3000,FUTURE=6000,GENERATIONS=3;
    private final String source;
    private int trial;private long computeWallNanos,stepStarted;
    private String loopState;
    private Ledger ledger=new Ledger();
    private final List<Map<String,Object>> results=new ArrayList<>();
    private Map<String,Object> pendingArtifact;
    private transient EvolutionLoopR27 loop;private transient FeatureCacheR27 cache;
    private transient ReplayDatasetR27 full;
    private final Object lifecycle=new Object();private boolean running,retired;
    public FactoryCapabilityExperimentR31(String source){if(source==null||source.isEmpty())throw new IllegalArgumentException("FACTORY_SOURCE");this.source=source;}
    public String source(){return source;}
    public static Map<String,Object> protocol(){
        Map<String,Object> p=new LinkedHashMap<>();p.put("version",VERSION);p.put("namespace",NAMESPACE);p.put("replicatesPerArm",REPLICATES);p.put("warmRows",WARM);p.put("futureRows",FUTURE);p.put("generations",GENERATIONS);p.put("seedBase",710000L);p.put("signalStrength",.30);p.put("signalPosition",3);p.put("farUpper95Budget",.15);p.put("powerLower95Floor",.70);
        List<String> arms=new ArrayList<>();for(ResearchCapabilityExperimentR31.Arm arm:ResearchCapabilityExperimentR31.Arm.values())arms.add(arm.name());p.put("arms",arms);
        EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();p.put("births",cfg.births);p.put("familyMinimum",cfg.minPerFamily);p.put("populationCap",cfg.populationCap);p.put("recursiveNullControl",cfg.nullControl);p.put("i00PerGeneration",cfg.i00PerGeneration);p.put("creationCostUnits",cfg.creationCostUnits);p.put("creationSliceUnits",cfg.creationSliceUnits);
        List<String> families=new ArrayList<>();for(IdeaFamilyR27 f:EvolutionLoopR27.FAMILIES)families.add(f.getClass().getName());p.put("factoryFamilies",families);p.put("semantics",FrozenConfirmationR30.currentSemantics());p.put("certificateContract",CertificateGateR27.contract());p.put("confirmationRule","ORIGINAL_FROZEN_CONFIRMATION_OUTSIDE_EVOLUTION");p.put("realPermission",false);return Collections.unmodifiableMap(p);
    }
    public static String protocolHash(){return StatsR27.sha256(JsonR27.write(protocol()));}
    public boolean complete(){return trial==REPLICATES*ResearchCapabilityExperimentR31.Arm.values().length;}
    private ResearchCapabilityExperimentR31.Arm arm(){return ResearchCapabilityExperimentR31.Arm.values()[trial/REPLICATES];}
    private long seed(){return 710000L+trial%REPLICATES;}
    private void ensureEngine(){
        if(loop!=null)return;
        int[][] draws=ResearchCapabilityExperimentR31.generateStream(arm(),seed(),WARM+FUTURE,3,.30);
        full=ResearchCapabilityExperimentR31.dataset(arm(),draws);
        List<ReplayDatasetR27.Row> warm=new ArrayList<>();for(int i=0;i<WARM;i++)warm.add(new ReplayDatasetR27.Row(full.issues[i],full.ordinals[i],full.draws[i]));
        cache=new FeatureCacheR27(ReplayDatasetR27.of(warm));EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.seed=seed();
        loop=loopState==null?new EvolutionLoopR27(cfg,cache,ledger,()->0L):EvolutionLoopR27.restore(loopState,cfg,cache,ledger,()->0L);
        loop.sourceCommit=source; // Only synthetic stores receive this runner's outputs.
    }
    public void step(){
        synchronized(lifecycle){if(retired)throw new java.util.concurrent.CancellationException("FACTORY_RUNNER_CLOSED");if(running)throw new IllegalStateException("FACTORY_STEP_IN_FLIGHT");running=true;}
        long began=System.nanoTime();stepStarted=began;int startedTrial=trial;pendingArtifact=null;
        try{
            if(complete())return;
            ensureEngine();
            if(loop.generation<GENERATIONS){loop.step();loopState=loop.snapshot();return;}
            finish();
        }catch(RuntimeException failed){
            FailureDispositionR31.Disposition disposition=FailureDispositionR31.classify(failed).disposition;
            if(disposition==FailureDispositionR31.Disposition.CANCELLED||disposition==FailureDispositionR31.Disposition.DEPENDENCY||disposition==FailureDispositionR31.Disposition.STALE_OWNER)throw failed;
            Map<String,Object> row=baseResult();row.put("status","FAILED");row.put("fault",failed.getClass().getName()+":"+String.valueOf(failed.getMessage()));record(row);
        }finally{if(trial==startedTrial)computeWallNanos+=Math.max(0,System.nanoTime()-began);boolean release;synchronized(lifecycle){running=false;release=retired;}if(release)releaseEngine();}
    }
    private Map<String,Object> baseResult(){Map<String,Object> r=new LinkedHashMap<>();r.put("protocolHash",protocolHash());r.put("sourceCommit",source);r.put("namespace",NAMESPACE);r.put("arm",arm().name());r.put("seed",seed());r.put("realPermission",false);r.put("naturalQualityEligible",false);return r;}
    private void finish(){
        Map<String,Object> row=baseResult();
        if(loop.champion==null||loop.pendingConfirmation==null){row.put("status","NOT_DETECTED");row.put("reason","NO_ELIGIBLE_FACTORY_CHAMPION_OR_FROZEN_CONFIRMATION");record(row);return;}
        FrozenConfirmationR30.Task frozen=loop.pendingConfirmation;
        Map<String,ReplayEvaluatorR27.Trace> traces=new HashMap<>();
        FrozenConfirmationR30.Result confirm=FrozenConfirmationR30.evaluate(frozen,loop.context(),loop.ledger(),new FrozenConfirmationR30.Evidence(){
            public ReplayEvaluatorR27.Trace load(FrozenConfirmationR30.Task task,FrozenConfirmationR30.Policy policy){return traces.get(policy.experimentId);}
            public void save(FrozenConfirmationR30.Task task,FrozenConfirmationR30.Policy policy,ReplayEvaluatorR27.Trace trace){traces.put(policy.experimentId,trace);}
        });
        row.put("confirmation",confirm.report);row.put("selectedHash",frozen.champion.policyHash);
        if(!confirm.verified){row.put("status","NOT_DETECTED");row.put("reason","ORIGINAL_INDEPENDENT_CONFIRMATION_NOT_VERIFIED");record(row);return;}
        try(FeatureCacheR27 future=new FeatureCacheR27(full)){
            ReplayEvaluatorR27.Trace actual=ReplayEvaluatorR27.run(frozen.champion.spec(),future,full.draws,frozen.champion.temperature,WARM,full.size());
            SequentialQualificationR27.State sequential=new SequentialQualificationR27.State(NAMESPACE+":"+protocolHash()+":"+trial,1,SelectorPolicyR30.registeredRate(frozen.champion.spec()));
            int evaluatedTo=WARM;for(int i=WARM;i<full.size()&&SequentialQualificationR27.ACCUMULATING.equals(sequential.state);i++){sequential.observe(full.ordinals[i],!actual.skipped[i]&&actual.act[i],actual.hit[i]>0);evaluatedTo=i+1;}
            ReplayScoreR27 score=ReplayScoreR27.of("SYNTHETIC_FUTURE",actual,WARM,evaluatedTo);row.put("futureFrom",full.ordinals[WARM]);row.put("futureThrough",full.ordinals[evaluatedTo-1]);
            CertificateGateR27.Evidence evidence=new CertificateGateR27.Evidence();evidence.policyHash=frozen.champion.policyHash;evidence.sequentialState=sequential.state;evidence.acts=sequential.acts;evidence.hits=sequential.hits;evidence.logE=sequential.logUp;evidence.ece=score.ece;evidence.calibrationQuality=CertificateGateR27.calibrationQuality(score.ece);evidence.worstBlock=score.worstBlockHr;evidence.maxLosingStreak=score.maxLosingStreak;evidence.abiEquivalent=frozen.researchSemanticHash.equals(FrozenConfirmationR30.currentSemantics());
            evidence.pairedLcbVsI00=paired(actual,frozen.i00,future,evaluatedTo);evidence.pairedLcbVsNull=paired(actual,frozen.nullPolicy,future,evaluatedTo);
            CertificateGateR27.Verdict verdict=CertificateGateR27.evaluate(evidence);row.put("qualification",sequential.json());row.put("certificateGates",verdict.gates);row.put("certificateFailures",verdict.failing);row.put("status",verdict.active?"DETECTED":"NOT_DETECTED");row.put("futureScore",score.json());
        }
        record(row);
    }
    private double paired(ReplayEvaluatorR27.Trace actual,FrozenConfirmationR30.Policy control,FeatureCacheR27 future,int to){
        if(control==null)return Double.NaN;
        if(control.shuffled){
            ReplayDatasetR27 shuffled=ReplayEvaluatorR27.shuffledDataset(full,loop.context().split.trainFrom,loop.context().split.selectTo,control.labelSeed);
            try(FeatureCacheR27 originalControl=new FeatureCacheR27(shuffled)){ReplayEvaluatorR27.Trace other=ReplayEvaluatorR27.run(control.spec(),originalControl,shuffled.draws,control.temperature,WARM,to);return ReplayScoreR27.paired(actual,other,WARM,to).lcb(StatsR27.Z95_ONE_SIDED);}
        }
        ReplayEvaluatorR27.Trace other=ReplayEvaluatorR27.run(control.spec(),future,full.draws,control.temperature,WARM,to);return ReplayScoreR27.paired(actual,other,WARM,to).lcb(StatsR27.Z95_ONE_SIDED);
    }
    private void record(Map<String,Object> row){
        String text=JsonR27.write(ledger.json());row.put("candidateLedgerSha256",StatsR27.sha256(text));row.put("candidates",ledger.policies.size());row.put("searchEvents",ledger.events.size());row.put("computeWallNanos",computeWallNanos+Math.max(0,System.nanoTime()-stepStarted));row.put("costScope","ACTUAL_HOST_OR_DEVICE_WORKING_THREAD_WALL_TIME; NOT_PROCESS_CPU");
        pendingArtifact=new LinkedHashMap<>(row);pendingArtifact.put("candidateLedger",ledger.json());results.add(row);trial++;loopState=null;releaseEngine();ledger=new Ledger();computeWallNanos=0;
    }
    public Map<String,Object> pendingArtifact(){return pendingArtifact;}
    public Map<String,Object> summary(){
        Map<String,Object> m=new LinkedHashMap<>();m.put("version",VERSION);m.put("namespace",NAMESPACE);m.put("sourceCommit",source);m.put("protocol",protocol());m.put("protocolHash",protocolHash());m.put("plannedTrials",REPLICATES*ResearchCapabilityExperimentR31.Arm.values().length);m.put("completedTrials",results.size());
        long failures=0;for(Map<String,Object> row:results)if("FAILED".equals(row.get("status")))failures++;
        m.put("failedTrials",failures);m.put("status",failures>0?"FAULTED":complete()?"COMPLETE_MEASURED_REQUIRES_REGISTERED_POWER_ASSESSMENT":"RUNNING_INCOMPLETE");m.put("trialResults",new ArrayList<>(results));
        List<Object> groups=new ArrayList<>();for(ResearchCapabilityExperimentR31.Arm a:ResearchCapabilityExperimentR31.Arm.values()){
            long n=0,detected=0,faults=0;for(Map<String,Object> row:results)if(a.name().equals(row.get("arm"))){n++;if("DETECTED".equals(row.get("status")))detected++;if("FAILED".equals(row.get("status")))faults++;}
            Map<String,Object> group=new LinkedHashMap<>();group.put("arm",a.name());group.put("completed",n);group.put("failures",faults);group.put("detected",detected);group.put("independentTrialInterval",NullPipelineSimulationR30.binomial(detected,n));groups.add(group);
        }m.put("arms",groups);
        long nullN=0,falseAlarms=0,signalN=0,detected=0;
        for(Map<String,Object> row:results){String a=String.valueOf(row.get("arm"));if(a.equals("NULL_UNIFORM")||a.equals("NULL_TIME_SHUFFLED")||a.equals("SIGNAL_DISABLED")){nullN++;if("DETECTED".equals(row.get("status")))falseAlarms++;}if(a.equals("SIGNAL")){signalN++;if("DETECTED".equals(row.get("status")))detected++;}}
        Double far=nullN==0?null:ResearchCapabilityExperimentR31.clopperPearsonUpper(falseAlarms,nullN,.05),power=signalN==0?null:ResearchCapabilityExperimentR31.clopperPearsonLower(detected,signalN,.05);
        m.put("falseAlarmUpper95",far);m.put("signalPowerLower95",power);m.put("meetsRegisteredExperiment",complete()&&failures==0&&far!=null&&power!=null&&far<=.15&&power>=.70);
        m.put("currentGeneration",loop==null?null:loop.generation);m.put("currentStep",loop==null?null:loop.step);m.put("realPermission",false);m.put("naturalQuality","NOT_PROVEN_SYNTHETIC_ONLY");return m;
    }
    public String snapshot(){Map<String,Object> m=new LinkedHashMap<>();m.put("version",VERSION);m.put("source",source);m.put("protocolHash",protocolHash());m.put("trial",trial);m.put("computeWallNanos",computeWallNanos);m.put("loopState",loopState);m.put("ledger",ledger.json());m.put("results",results);m.put("pendingArtifact",pendingArtifact);return JsonR27.write(m);}
    @SuppressWarnings("unchecked") public static FactoryCapabilityExperimentR31 restore(String text){
        Map<String,Object> m=(Map<String,Object>)JsonR27.parse(text);if(!VERSION.equals(m.get("version"))||!protocolHash().equals(m.get("protocolHash")))throw new IllegalArgumentException("FACTORY_PROTOCOL_CHANGED");
        FactoryCapabilityExperimentR31 x=new FactoryCapabilityExperimentR31((String)m.get("source"));x.trial=((Number)m.get("trial")).intValue();if(x.trial<0||x.trial>REPLICATES*ResearchCapabilityExperimentR31.Arm.values().length)throw new IllegalArgumentException("FACTORY_CURSOR");x.computeWallNanos=((Number)m.get("computeWallNanos")).longValue();x.loopState=(String)m.get("loopState");x.ledger=Ledger.restore((Map<String,Object>)m.get("ledger"));for(Object row:(List<?>)m.get("results"))x.results.add((Map<String,Object>)row);x.pendingArtifact=(Map<String,Object>)m.get("pendingArtifact");if(x.results.size()!=x.trial)throw new IllegalArgumentException("FACTORY_TRIAL_JOURNAL");return x;
    }
    private void releaseEngine(){try{if(loop!=null)loop.close();}finally{loop=null;if(cache!=null)cache.close();cache=null;full=null;}}
    @Override public void close(){boolean release;synchronized(lifecycle){retired=true;release=!running;}if(release)releaseEngine();}
    /** All proposals, rejections, scores and lineage are retained as originals. */
    private static final class Ledger implements EvolutionLoopR27.Sink {
        final Map<String,Object> policies=new LinkedHashMap<>();final List<Object> events=new ArrayList<>();
        void event(String kind,Object data){if(events.size()>=20000)throw new IllegalStateException("FACTORY_SEARCH_LEDGER_BOUND");Map<String,Object> e=new LinkedHashMap<>();e.put("kind",kind);e.put("data",data);events.add(e);}
        public void policy(PolicySpecR27 s,String state,String reason,int generation,String dataset){policies.put(s.policyHash(),s.fullJson());event("POLICY",Arrays.asList(s.policyHash(),state,reason,generation,dataset));}
        public void lineage(String child,String parent,String role,String op,int generation){event("LINEAGE",Arrays.asList(child,parent,role,op,generation));}
        public void score(String policy,String dataset,ReplayScoreR27 score){event("SCORE",Arrays.asList(policy,dataset,score.json()));}
        public void generation(Map<String,Object> r){event("GENERATION",new LinkedHashMap<>(r));}
        public void promote(PolicySpecR27 s,double tau,int generation,String reason){throw new IllegalStateException("SYNTHETIC_CANNOT_PROMOTE_REAL");}
        public void demote(String policy,String reason){event("DEMOTE_SYNTHETIC_MEMBER",Arrays.asList(policy,reason));}
        public void expert(ExpertProposalsR27.Proposal p,int generation){event("EXPERT",Arrays.asList(generation,p.json()));}
        public void ablation(int generation,String champion,List<Object[]> rows){event("ABLATION",Arrays.asList(generation,champion,rows));}
        public void holdoutLook(long block,String policy){throw new IllegalStateException("FACTORY_CONFIRMATION_INSIDE_EVOLUTION");}
        public void behaviorDuplicate(BehaviorSignatureR30.Duplicate d){event("BEHAVIOR_DUPLICATE",Arrays.asList(d.datasetHash,d.probeSetHash,d.signature,d.keptPolicy,d.droppedPolicy,d.stage));}
        Map<String,Object> json(){Map<String,Object> m=new LinkedHashMap<>();m.put("policies",policies);m.put("events",events);return m;}
        @SuppressWarnings("unchecked") static Ledger restore(Map<String,Object> m){Ledger l=new Ledger();l.policies.putAll((Map<String,Object>)m.get("policies"));l.events.addAll((List<?>)m.get("events"));return l;}
    }
}
