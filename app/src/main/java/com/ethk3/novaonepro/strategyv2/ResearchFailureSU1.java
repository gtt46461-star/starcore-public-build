package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;

/** Exact contract observations. A code is not an inferred causal explanation for a losing prediction. */
public final class ResearchFailureSU1 {
    public enum Category { MISSING_INPUT,PIT_TIMING,STATE_INTEGRITY,DEPENDENCY,RANKING_EVIDENCE,CALIBRATION,COVERAGE,RESOURCE,REDUNDANT_PROPOSAL,NUMERIC,UNKNOWN }
    public final Category category;
    public final String code;
    public final boolean rejectCandidate,resourceOrRepeatedFailure;
    private ResearchFailureSU1(Category category,String code,boolean reject,boolean resource){this.category=category;this.code=code;rejectCandidate=reject;resourceOrRepeatedFailure=resource;}
    private static boolean one(String code,String... values){for(String value:values)if(value.equals(code))return true;return false;}
    public static ResearchFailureSU1 code(String raw){
        String code=raw==null?"UNKNOWN":raw.substring(0,Math.min(256,raw.length()));
        if(one(code,"SU1_BUDGET_EXHAUSTED","BUDGET_SKIP"))return new ResearchFailureSU1(Category.RESOURCE,code,false,true);
        if(one(code,"SU1_PREEMPTED"))return new ResearchFailureSU1(Category.RESOURCE,code,false,false);
        if(one(code,"SEMANTIC_DUPLICATE","REDUNDANT_PROPOSAL"))return new ResearchFailureSU1(Category.REDUNDANT_PROPOSAL,code,true,true);
        if(one(code,"SU1_PROGRAM_COST","SU1_COMPILED_RESOURCE_BUDGET","SU1_MEMORY_BUDGET"))return new ResearchFailureSU1(Category.RESOURCE,code,true,true);
        if(one(code,"SU1_GRAPH_COMPLETE_WINDOW_REQUIRED","SU1_REPLAY_CURSOR","SU1_INSUFFICIENT_HISTORY"))return new ResearchFailureSU1(Category.MISSING_INPUT,code,true,false);
        if(one(code,"SU1_FITTED_MODEL_TRAINING_BOUNDARY","SU1_GRAPH_FITTED_TRAINING_BOUNDARY","SU1_MODEL_TRAINED_ON_TARGET","SU1_GRAPH_TRAINED_ON_TARGET","SU1_IMPORT_WAIT_POSTTRAIN_LOCAL_HISTORY"))return new ResearchFailureSU1(Category.PIT_TIMING,code,true,false);
        if(one(code,"SU1_FITTED_MODEL_DEPENDENCY_MISSING","SU1_GRAPH_MODEL_DEPENDENCY_MISSING","SU1_MODEL_PROGRAM_BINDING","SU1_GRAPH_PROGRAM_BINDING"))return new ResearchFailureSU1(Category.DEPENDENCY,code,true,false);
        if(one(code,"SU1_NONFINITE_SCORE","SU1_NONFINITE_FITNESS","SU1_GRAPH_NONFINITE_EVENT","SU1_GRAPH_NONFINITE_EDGE","SU1_GRAPH_NONFINITE_ATTENTION","SU1_GRAPH_NONFINITE_VALUE","SU1_GRAPH_NONFINITE_STATE","SU1_GRAPH_NONFINITE_LOGIT","SU1_GRAPH_NONFINITE_GRADIENT","SU1_GRAPH_NONFINITE_LOSS"))return new ResearchFailureSU1(Category.NUMERIC,code,true,false);
        if(one(code,"SU1_CHECKPOINT_CORRUPT","SU1_MEMORY_CORRUPT","SU1_GRAPH_SCHEMA_OR_MEMORY","SU1_MODEL_MANIFEST_NONCANONICAL_OR_UNKNOWN_FIELDS"))return new ResearchFailureSU1(Category.STATE_INTEGRITY,code,false,false);
        if(one(code,"SU1_CALIBRATION_NOT_VALIDATED","CALIBRATION_UNKNOWN","CALIBRATION_MISSING"))return new ResearchFailureSU1(Category.CALIBRATION,code,false,false);
        if(one(code,"GATE_SKIP","COVERAGE_INSUFFICIENT","BOTH_MISSING"))return new ResearchFailureSU1(Category.COVERAGE,code,false,false);
        if(one(code,"PAIRED_IMPROVEMENT_NOT_ESTABLISHED","SCOUT_UPPER_BELOW025"))return new ResearchFailureSU1(Category.RANKING_EVIDENCE,code,code.equals("SCOUT_UPPER_BELOW025"),false);
        return new ResearchFailureSU1(Category.UNKNOWN,code,false,false);
    }
    public static ResearchFailureSU1 observe(Throwable failure){
        Set<Throwable> visited=Collections.newSetFromMap(new IdentityHashMap<>());for(Throwable t=failure;t!=null&&visited.size()<8&&visited.add(t);t=t.getCause()){
            ResearchFailureSU1 diagnostic=code(t.getMessage());if(diagnostic.category!=Category.UNKNOWN)return diagnostic;
        }return code(failure==null?null:failure.getMessage());
    }
    public double schedulingPenalty(){
        switch(category){case RESOURCE:case REDUNDANT_PROPOSAL:return 1;case NUMERIC:return 2;case DEPENDENCY:return .5;case PIT_TIMING:return .25;case MISSING_INPUT:return .1;default:return 0;}
    }
    public String payload(String dataset,String context,String program){
        Map<String,Object> m=new TreeMap<>();m.put("protocol","SU1_FAILURE_OBSERVATION_V1");m.put("category",category.name());m.put("code",code);m.put("rejectCandidate",rejectCandidate);m.put("resourceOrRepeatedFailure",resourceOrRepeatedFailure);m.put("dataset",HashingSU1.sha(dataset));m.put("context",context);m.put("program",HashingSU1.sha(program));m.put("knowledge","OBSERVED_CONTRACT_OR_DESCRIPTIVE_EVIDENCE_NOT_CAUSAL_ATTRIBUTION");return StrictCanonicalJson.writeContext(m);
    }
}
