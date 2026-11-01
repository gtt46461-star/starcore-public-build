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
            v=new double[rows.size()][cols.size()];n=new long[rows.size()][cols.size()];
            for(int t=from;t<to;t++)if(!trace.skipped[t]){
                double[] probabilities=trace.probabilityRows[t];if(probabilities==null)continue;
                int condition=trace.pHit[t]<.30?0:trace.pHit[t]<.45?1:2,actual=d.draws[t][policy.position-1]-1;
                if("STUDENT_ERROR".equals(kind)){v[0][trace.hit[t]==1?0:1]++;n[0][0]++;n[0][1]++;double loss=0;for(int j=0;j<10;j++){double e=probabilities[j]-(j==actual?1:0);loss+=.5*e*e;}v[0][2]+=loss;n[0][2]++;}
                else if("FEEDBACK".equals(kind)){v[condition][trace.hit[t]==1?0:1]++;n[condition][0]++;n[condition][1]++;}
                else for(int j=0;j<10;j++){v[condition][j]+=(j==actual?1:0)-probabilities[j];n[condition][j]++;}
            }
            for(int r=0;r<v.length;r++)for(int c=0;c<v[r].length;c++)if(n[r][c]>0)v[r][c]/=n[r][c];
            more.put("estimator","STUDENT_ERROR".equals(kind)?"SAME_TARGET_HIT_MISS_AND_NORMALIZED_TEN_CLASS_BRIER":"FEEDBACK".equals(kind)?"PREDRAW_CONFIDENCE_CONDITIONED_K3_ERROR":"PREDRAW_CONFIDENCE_CONDITIONED_TEN_CLASS_RESIDUAL");more.put("position",policy.position);more.put("conditionScope","FROZEN_BEFORE_LABEL_FROM_CAUSAL_REPLAY");
        }else return null;
        return new MatrixTensorR30R1.Tensor(kind,"SELECT",d.datasetHash,policy==null?StatsR27.sha256("SOURCE_ONLY"):policy.policyHash(),through,target,revision,rows,cols,v,n,more);
    }
    static ResearchPoolR27.Outcome step(RuntimeR27 rt,ResearchPoolR27.Worker w)throws Exception{
        EvolutionLoopR27 evolution=rt.evo;if(evolution==null)return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-evolution)",0,-1);
        EvalContextR27 ctx=evolution.context();long revision=SchemaR27.liveSourceRevision(w.ro);if(revision<0)return ResearchPoolR27.Outcome.idle("DEPENDENCY(SOURCE_REVISION)",0,-1);
        PolicySpecR27 policy=evolution.population.isEmpty()?null:evolution.population.get(0).spec;
        String input=StatsR27.sha256(ctx.data.datasetHash+"|"+revision+"|"+(policy==null?"NO_STUDENT":policy.policyHash()));
        for(MatrixRegistryR30.Definition def:MatrixRegistryR30.all())if(def.namespace.equals("R930")&&!def.kind.equals("COUNTERFACTUAL")){
            try(Cursor c=w.ro.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{PANEL,def.kind})){if(c.moveToFirst()){Object saved=JsonR27.parse(c.getString(0));if(saved instanceof Map&&input.equals(((Map<?,?>)saved).get("inputIdentity")))continue;}}
            long started=System.nanoTime();MatrixTensorR30R1.Tensor tensor=build(def.kind,ctx,policy,revision);long now=System.currentTimeMillis();Map<String,Object> receipt=new LinkedHashMap<>();receipt.put("matrixId",def.id);receipt.put("inputIdentity",input);receipt.put("datasetHash",ctx.data.datasetHash);receipt.put("sourceRevision",revision);receipt.put("role","SELECT");receipt.put("producerVersion",MatrixTensorR30R1.VERSION);receipt.put("status",tensor==null?"BLOCKED_DEPENDENCY":"PRODUCED");receipt.put("reason",tensor==null?dependency(def.kind):null);receipt.put("matrixHash",tensor==null?null:tensor.hash);receipt.put("costNs",System.nanoTime()-started);receipt.put("qualityEffective",null);
            ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();if(tensor!=null)MatrixTensorR30R1.append(b,tensor,now);
            b.add("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,?,?)",PANEL,def.kind,JsonR27.write(receipt),now);
            b.guardBefore(db->{if(SchemaR27.liveSourceRevision(db)!=revision)throw new ResearchStoreR30R1.StaleResearch();return 0;});
            if(tensor!=null)ResearchStoreR30R1.appendOwnedProgress(b,-1,new ResearchStoreR30R1.Facts(tensor.throughOrdinal,tensor.throughOrdinal,null,null,tensor.throughOrdinal,tensor.hash));
            try{RuntimeR27.submit(w,"MATRIX_TYPED",b);}catch(RuntimeException sourceChanged){if(ResearchStoreR30R1.stale(sourceChanged))return ResearchPoolR27.Outcome.idle("DEPENDENCY(MATRIX_SOURCE_CHANGED_RETRY)",0,-1);throw sourceChanged;}
            return tensor==null?ResearchPoolR27.Outcome.idle("DEPENDENCY("+dependency(def.kind)+")",0,-1):ResearchPoolR27.Outcome.progress(1,tensor.throughOrdinal);
        }
        return ResearchPoolR27.Outcome.idle("WAITING_NEW_RESEARCH_INPUT",0,-1);
    }
}
