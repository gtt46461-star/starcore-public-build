package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import java.util.*;
import com.ethk3.novaonepro.TargetIssueResolver;

/** One kind per slice on the original research pool. No live owner waits for a tensor. */
public final class MatrixResearchR30R1 {
    private MatrixResearchR30R1(){}
    public static final String PANEL="matrix-r30-r1";
    static List<String> labels(String prefix,int n){List<String> out=new ArrayList<>();for(int i=1;i<=n;i++)out.add(prefix+i);return out;}
    static String dependency(String kind){switch(kind){
        case "EVIDENCE_TEACHER":case "TEACHER_DIVERSITY":return "ORIGINAL_TEACHER_SAME_TARGET_FROZEN_PROBE_AND_ABI_REQUIRED";
        case "COUNTERFACTUAL":case "CAUSAL_SUPPORT":return "EXACT_PAIRED_COMPONENT_REMOVAL_WITH_CHILD_HASH_REQUIRED";
        case "REVERSE_REASONING":case "FORWARD_REASONING":return "EXECUTABLE_PREMISE_TRACE_AND_FORWARD_VERIFICATION_REQUIRED";
        case "INVARIANCE":return "PREREGISTERED_SEMANTIC_PRESERVING_TRANSFORM_REQUIRED";
        case "TEMPORAL_PRECEDENCE":return "AVAILABLE_AT_AND_FROZEN_AT_SOURCE_RECEIPTS_REQUIRED";
        case "ACTION_UTILITY":return "PREREGISTERED_RESEARCH_ACTION_SETTLEMENT_REQUIRED_FORMAL_FUTURE_READ_ONLY";
        case "CONTRADICTION":return "IDENTICAL_TARGET_ROLE_DENOMINATOR_CLAIMS_REQUIRED";
        case "COUNTER_EVIDENCE":return "ORIGINAL_NEGATIVE_RESEARCH_RECEIPT_REQUIRED";
        case "EVOLUTION_CREDIT":return "ATOMIC_PROPOSAL_CHILD_AND_PER_OPERATOR_CPU_RECEIPT_REQUIRED";
        case "RESOURCE_EFFICIENCY":return "PER_TASK_CPU_MEMORY_QUEUE_AND_ARTIFACT_COMMIT_JOIN_REQUIRED";
        default:return "LEGAL_TYPED_SOURCE_NOT_AVAILABLE:"+kind;
    }}
    /** R31-B08-C01: per-kind state of one round. Only PRODUCED appends a tensor; every other state keeps the last valid one. */
    public enum StatusR31{PRODUCED,WAITING_STUDENT,WAITING_SOURCE,INSUFFICIENT,INVALID}
    public static final List<String> STUDENT_KINDS_R31=Collections.unmodifiableList(Arrays.asList("STUDENT_ERROR","FEEDBACK","RESIDUAL_MECHANISM"));
    // Only these products are independent of the current policy/target set.
    // Reverse reasoning can run without a student, but an available teacher's
    // position changes its conditional matrix and must reopen the durable key.
    public static final List<String> SOURCE_ONLY_KINDS_R31=Collections.unmodifiableList(Arrays.asList("NUMBER_POSITION_COMPATIBILITY","CROSS_POSITION_FLOW"));
    public static final class OutcomeR31{
        public final StatusR31 status;public final MatrixTensorR30R1.Tensor tensor;public final String reason;
        OutcomeR31(StatusR31 status,MatrixTensorR30R1.Tensor tensor,String reason){this.status=status;this.tensor=tensor;this.reason=reason==null?"":reason;}
    }
    public interface ProducerR31{MatrixTensorR30R1.Tensor produce()throws Exception;}
    /** Dependency key: dataset, source revision, kind, policy set and measured version. An unchanged key is never re-run. */
    public static String dependencyKeyR31(String datasetHash,long revision,String kind,String policySetHash,long measuredVersion,String sourceCommit){
        return StatsR27.sha256("R31_MATRIX_DEPENDENCY_V1|"+datasetHash+"|"+revision+"|"+kind+"|"+policySetHash+"|"+measuredVersion+"|"+sourceCommit);
    }
    /** Runs one kind in isolation: a missing student, an empty window or an invalid tensor only affects this kind. */
    public static OutcomeR31 produceR31(String kind,boolean studentAvailable,String datasetHash,long revision,ProducerR31 producer)throws Exception{
        if(STUDENT_KINDS_R31.contains(kind)&&!studentAvailable)return new OutcomeR31(StatusR31.WAITING_STUDENT,null,"MATRIX_ORIGINAL_STUDENT_UNAVAILABLE");
        MatrixTensorR30R1.Tensor t;
        try{t=producer.produce();}
        catch(IllegalArgumentException|IllegalStateException|ArithmeticException|IndexOutOfBoundsException invalid){
            Error fatal=FailureDispositionR31.processFatalCause(invalid);if(fatal!=null)throw fatal;
            if(isStorageFaultR31(invalid))throw invalid; // database integrity stays on the B01/B02 fault path
            String m=String.valueOf(invalid.getMessage());
            if(m.contains("MATRIX_ORIGINAL_STUDENT_UNAVAILABLE"))return new OutcomeR31(StatusR31.WAITING_STUDENT,null,m);
            if(m.contains("MATRIX_SELECT_EMPTY"))return new OutcomeR31(StatusR31.INSUFFICIENT,null,m);
            // Scheduling controls must reach the original owner. Persisting one
            // as INVALID would mark this dependency key decided until inputs change.
            if(FailureDispositionR31.classify(invalid).disposition!=FailureDispositionR31.Disposition.FAULT)throw invalid;
            return new OutcomeR31(StatusR31.INVALID,null,invalid.getClass().getSimpleName()+":"+m);
        }
        if(t==null)return new OutcomeR31(StatusR31.WAITING_SOURCE,null,dependency(kind));
        if(!t.datasetHash.equals(datasetHash)||t.revision!=revision)return new OutcomeR31(StatusR31.INVALID,null,"MATRIX_SOURCE_IDENTITY_CHANGED");
        if(t.presentCells==0)return new OutcomeR31(StatusR31.INSUFFICIENT,null,"MATRIX_NO_SUPPORTED_CELL");
        return new OutcomeR31(StatusR31.PRODUCED,t,"");
    }
    static boolean isStorageFaultR31(Throwable t){IdentityHashMap<Throwable,Boolean> seen=new IdentityHashMap<>();for(Throwable c=t;c!=null&&seen.size()<16&&seen.put(c,Boolean.TRUE)==null;c=c.getCause()){String n=c.getClass().getName();if(n.startsWith("android.database.sqlite.")||n.equals("java.sql.SQLException"))return true;}return false;}
    /** Receipt of one kind; a non-PRODUCED state carries the previous valid tensor hash forward, flagged as stale. */
    public static Map<String,Object> receiptR31(String matrixId,String dependencyKey,String datasetHash,long revision,OutcomeR31 o,Map<?,?> previous,long costNs){
        Map<String,Object> r=new LinkedHashMap<>();r.put("matrixId",matrixId);r.put("inputIdentity",dependencyKey);r.put("datasetHash",datasetHash);r.put("sourceRevision",revision);r.put("role","SELECT");
        r.put("producerVersion",MatrixTensorR30R1.VERSION);r.put("status",o.status==StatusR31.PRODUCED?"PRODUCED":o.status==StatusR31.WAITING_SOURCE?"BLOCKED_DEPENDENCY":o.status.name());r.put("statusR31",o.status.name());
        r.put("reason",o.status==StatusR31.PRODUCED?null:o.reason);r.put("matrixHash",o.tensor==null?null:o.tensor.hash);
        Object lastValid=o.tensor!=null?o.tensor.hash:previous==null?null:previous.get("matrixHash")!=null?previous.get("matrixHash"):previous.get("lastValidMatrixHash");
        Object lastValidRevision=o.tensor!=null?(Object)revision:previous==null?null:previous.get("matrixHash")!=null?previous.get("sourceRevision"):previous.get("lastValidSourceRevision");
        r.put("lastValidMatrixHash",lastValid);r.put("lastValidSourceRevision",lastValidRevision);r.put("lastValidIsStale",o.tensor==null&&lastValid!=null);
        r.put("costNs",costNs);r.put("qualityEffective",null);return r;
    }
    /** True when this kind was already decided for the same dependency key (no repeated waiting job). */
    public static boolean decidedR31(Map<?,?> previous,String dependencyKey){return previous!=null&&dependencyKey.equals(previous.get("inputIdentity"));}

    static MatrixTensorR30R1.Tensor build(String kind,EvalContextR27 ctx,PolicySpecR27 policy,long revision){
        int from=Math.max(ctx.split.selectFrom,ctx.split.selectTo-256),to=ctx.split.selectTo;
        if(from>=to)throw new IllegalArgumentException("MATRIX_SELECT_EMPTY");
        ReplayDatasetR27 d=ctx.data;String through=d.issues[to-1],target=TargetIssueResolver.next(through).targetIssue;
        double[][] v;long[][] n;List<String> rows,cols;Map<String,Object> more=new LinkedHashMap<>();
        more.put("fromOrdinal",d.ordinals[from]);more.put("throughOrdinal",d.ordinals[to-1]);more.put("calendarIssues",(long)(to-from));more.put("formalRowsRead",0L);
        if("NUMBER_POSITION_COMPATIBILITY".equals(kind)){
            rows=labels("number",10);cols=labels("position",10);v=new double[10][10];n=new long[10][10];
            for(int t=from;t<to;t++)for(int p=0;p<10;p++)v[d.draws[t][p]-1][p]++;
            for(int a=0;a<10;a++)for(int p=0;p<10;p++){n[a][p]=to-from;v[a][p]=(v[a][p]+1)/(to-from+10.0);}
            more.put("estimator","LAPLACE_STRICT_PAST_NUMBER_POSITION_FREQUENCY");
        }else if("CROSS_POSITION_FLOW".equals(kind)){
            rows=labels("targetPosition",10);cols=new ArrayList<>();int[] lags={1,2,5,10};for(int source=1;source<=10;source++)for(int lag:lags)cols.add("sourcePosition"+source+"@lag"+lag);
            v=new double[10][40];n=new long[10][40];for(int p=0;p<10;p++)for(int source=0;source<10;source++)for(int k=0;k<lags.length;k++){
                int c=source*4+k;for(int t=from;t<to;t++){int previous=t-lags[k];if(previous<0||d.ordinals[t]-d.ordinals[previous]!=lags[k])continue;n[p][c]++;if(d.draws[previous][source]==d.draws[t][p])v[p][c]++;}
                if(n[p][c]>0)v[p][c]=v[p][c]/n[p][c]-.1;
            }more.put("estimator","DIRECTED_LAG_SAME_CLASS_EXCESS_OVER_0.10_ASSOCIATION_ONLY");
        }else if("STUDENT_ERROR".equals(kind)||"FEEDBACK".equals(kind)||"RESIDUAL_MECHANISM".equals(kind)){
            if(policy==null)throw new IllegalArgumentException("MATRIX_ORIGINAL_STUDENT_UNAVAILABLE");
            ReplayEvaluatorR27.Trace trace=ReplayEvaluatorR27.selectTraceWithProbabilities(policy,ctx,ctx.temperature(policy));
            rows="STUDENT_ERROR".equals(kind)?Collections.singletonList(policy.policyHash()):Arrays.asList("LOW_PREDRAW_CONFIDENCE","MEDIUM_PREDRAW_CONFIDENCE","HIGH_PREDRAW_CONFIDENCE");
            cols="STUDENT_ERROR".equals(kind)?Arrays.asList("K3_HIT","K3_MISS","TEN_CLASS_BRIER"):"FEEDBACK".equals(kind)?Arrays.asList("K3_HIT","K3_MISS"):labels("actualNumber",10);
            double[] confidenceSum=new double[3];long[] confidenceN=new long[3];
            v=new double[rows.size()][cols.size()];n=new long[rows.size()][cols.size()];
            for(int t=from;t<to;t++)if(!trace.skipped[t]){
                double[] probabilities=trace.probabilityRows[t];if(probabilities==null)continue;
                int condition=trace.pHit[t]<.30?0:trace.pHit[t]<.45?1:2,actual=d.draws[t][policy.position-1]-1;confidenceSum[condition]+=trace.pHit[t];confidenceN[condition]++;
                if("STUDENT_ERROR".equals(kind)){v[0][trace.hit[t]==1?0:1]++;n[0][0]++;n[0][1]++;double loss=0;for(int j=0;j<10;j++){double e=probabilities[j]-(j==actual?1:0);loss+=.5*e*e;}v[0][2]+=loss;n[0][2]++;}
                else if("FEEDBACK".equals(kind)){v[condition][trace.hit[t]==1?0:1]++;n[condition][0]++;n[condition][1]++;}
                else for(int j=0;j<10;j++){v[condition][j]+=(j==actual?1:0)-probabilities[j];n[condition][j]++;}
            }
            for(int r=0;r<v.length;r++)for(int c=0;c<v[r].length;c++)if(n[r][c]>0)v[r][c]/=n[r][c];
            more.put("estimator","STUDENT_ERROR".equals(kind)?"SAME_TARGET_HIT_MISS_AND_NORMALIZED_TEN_CLASS_BRIER":"FEEDBACK".equals(kind)?"PREDRAW_CONFIDENCE_CONDITIONED_K3_ERROR":"PREDRAW_CONFIDENCE_CONDITIONED_TEN_CLASS_RESIDUAL");List<Object> confidenceMeans=new ArrayList<>();for(int bin=0;bin<3;bin++)confidenceMeans.add(confidenceN[bin]==0?null:confidenceSum[bin]/confidenceN[bin]);more.put("confidenceBinMeanProbability",confidenceMeans);more.put("position",policy.position);more.put("conditionScope","FROZEN_BEFORE_LABEL_FROM_CAUSAL_REPLAY");
        }else return null;
        // R31-B08-C02: unit, axes, support, calendar span, role and semantics travel with the tensor; association is
        // never an intervention and a tensor alone grants no main-chain permission.
        more.put("unitR31","NUMBER_POSITION_COMPATIBILITY".equals(kind)?"LAPLACE_PROBABILITY":"CROSS_POSITION_FLOW".equals(kind)?"EXCESS_SAME_CLASS_RATE_OVER_0.10":"STUDENT_ERROR".equals(kind)?"RATE_AND_HALF_BRIER":"PER_CONDITION_RATE_OR_RESIDUAL");
        more.put("axesR31",Arrays.asList(rows.size()+"_ROWS",cols.size()+"_COLUMNS"));more.put("supportScopeR31","PER_CELL_SUPPORT_COUNT");more.put("calendarSpanR31",(long)(to-from));
        more.put("semanticsR31",STUDENT_KINDS_R31.contains(kind)?"STUDENT_DIAGNOSTIC_FROZEN_BEFORE_LABEL":"OBSERVATIONAL_ASSOCIATION_NOT_INTERVENTION");
        more.put("consumersR31",MatrixRegistryR30.typedConsumers(kind));more.put("pairedAblationR31","NOT_RUN_UNTIL_A_CONSUMER_ABLATION_RECEIPT");more.put("mainChainPermissionR31",false);
        return new MatrixTensorR30R1.Tensor(kind,"SELECT",d.datasetHash,policy==null?StatsR27.sha256("SOURCE_ONLY"):policy.policyHash(),through,target,revision,rows,cols,v,n,more);
    }
    static ResearchPoolR27.Outcome step(RuntimeR27 rt,ResearchPoolR27.Worker w)throws Exception{
        EvolutionLoopR27 evolution=rt.evo;if(evolution==null)return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-evolution)",0,-1);
        EvalContextR27 ctx=evolution.context();if(ResearchInputPinR30R1.invalid(w.ro,ctx.data.datasetHash))return ResearchPoolR27.Outcome.idle("DEPENDENCY(CORRECTED_DATASET)",0,-1);long revision=ResearchInputPinR30R1.revision(w.ro,ctx.data.datasetHash);if(revision<0)return ResearchPoolR27.Outcome.idle("DEPENDENCY(SOURCE_REVISION)",0,-1);
        PolicySpecR27 policy=evolution.population.isEmpty()?null:evolution.population.get(0).spec;
        List<PolicySpecR27> policies=new ArrayList<>();for(EvolutionLoopR27.Member member:new ArrayList<>(evolution.population)){policies.add(member.spec);if(policies.size()==8)break;}for(PolicySpecR27 p:evolution.archive.members())if(policies.size()<12)policies.add(p);StringBuilder policyIdentity=new StringBuilder();for(PolicySpecR27 p:policies)policyIdentity.append(p.policyHash());
        long measuredVersion=0;try(Cursor q=w.ro.rawQuery("SELECT coalesce(max(rowid),0) FROM r30r1_research_measure WHERE kind IN ('ATOM_PAIRED_GAIN','OPERATOR_GAIN_COST','MODEL_PAIR_FROZEN','MODEL_PAIR_SETTLED')",null)){if(q.moveToFirst())measuredVersion=q.getLong(0);}
        String baseInput=ctx.data.datasetHash+"|"+revision+"|"+com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26;

        for(MatrixRegistryR30.Definition def:MatrixRegistryR30.all())if(def.namespace.equals("R930")&&!def.kind.equals("COUNTERFACTUAL")){
            boolean sourceOnly=SOURCE_ONLY_KINDS_R31.contains(def.kind);
            long measured=Arrays.asList("CAUSAL_SUPPORT","COUNTER_EVIDENCE","EVOLUTION_CREDIT","TEMPORAL_PRECEDENCE").contains(def.kind)?measuredVersion:def.kind.equals("RESOURCE_EFFICIENCY")?ResearchStoreR30R1.calendar(w.ro):0;
            String input=dependencyKeyR31(ctx.data.datasetHash,revision,def.kind,sourceOnly?"SOURCE_ONLY":StatsR27.sha256(policyIdentity.toString()),measured,com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26);
            Map<?,?> previous=null;
            try(Cursor c=w.ro.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{PANEL,def.kind})){if(c.moveToFirst()){Object saved=JsonR27.parse(c.getString(0));if(saved instanceof Map)previous=(Map<?,?>)saved;}}
            if(decidedR31(previous,input))continue; // R31-B08-C01#A2: unchanged dependency -> no new waiting job
            long started=System.nanoTime();final PolicySpecR27 student=sourceOnly?null:policy;
            OutcomeR31 outcome=produceR31(def.kind,policy!=null,ctx.data.datasetHash,revision,()->{MatrixTensorR30R1.Tensor t=build(def.kind,ctx,student,revision);if(t==null)t=MatrixEvidenceR30R1.compute(def.kind,ctx,policies,revision);if(t==null)t=MatrixEvidenceR30R1.stored(def.kind,w.ro,ctx,revision,rt);return t;});
            MatrixTensorR30R1.Tensor tensor=outcome.tensor;long now=System.currentTimeMillis();Map<String,Object> receipt=receiptR31(def.id,input,ctx.data.datasetHash,revision,outcome,previous,System.nanoTime()-started);
            ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();if(tensor!=null)MatrixTensorR30R1.append(b,tensor,now);
            b.add("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,?,?)",PANEL,def.kind,JsonR27.write(receipt),now);
            final MatrixTensorR30R1.Tensor preparedTensor=tensor;
            b.guardBefore(db->{ResearchInputPinR30R1.require(db,ctx.data.datasetHash,revision);if(preparedTensor!=null&&"ONLINE_RESEARCH".equals(preparedTensor.role)){Object parsed=JsonR27.parse(preparedTensor.payload);Object raw=((Map<?,?>)((Map<?,?>)parsed).get("details")).get("sourceRun");if(!(raw instanceof String))throw new IllegalStateException("MATRIX_ONLINE_SOURCE_REFERENCE");ResearchInputPinR30R1.requireRun(db,(String)raw);}return 0;});
            if(tensor!=null)ResearchStoreR30R1.appendOwnedProgress(b,-1,new ResearchStoreR30R1.Facts(tensor.throughOrdinal,tensor.throughOrdinal,null,null,tensor.throughOrdinal,tensor.hash));
            try{RuntimeR27.submit(w,"MATRIX_TYPED",b);}catch(RuntimeException sourceChanged){if(ResearchStoreR30R1.stale(sourceChanged))return ResearchPoolR27.Outcome.idle("DEPENDENCY(MATRIX_SOURCE_CHANGED_RETRY)",0,-1);throw sourceChanged;}
            // A non-produced kind records its state and lets the round continue with the next kind on the next slice.
            return tensor==null?ResearchPoolR27.Outcome.progress(1,0):ResearchPoolR27.Outcome.progress(1,tensor.throughOrdinal);
        }
        return ResearchPoolR27.Outcome.idle("WAITING_NEW_RESEARCH_INPUT",0,-1);
    }
}
