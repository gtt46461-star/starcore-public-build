package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Inventory of existing matrix objects: nineteen R930 contracts and the SU2 learned factor/teacher checkpoint.
 * This does not invent a twentieth R930 enum, manufacture observations, migrate tables, or declare efficacy. */
public final class MatrixRegistryR30 {
    private MatrixRegistryR30(){}
    public static final String SU2_ID="SU2:LEARNED_FACTOR_TEACHER_CHECKPOINT";
    private static final String ROOT="com.ethk3.novaonepro.",STRATEGY=ROOT+"strategyv2.";
    private static final String[] NATIVE={"EVIDENCE_TEACHER","STUDENT_ERROR","FORWARD_REASONING","REVERSE_REASONING","COUNTERFACTUAL","FEEDBACK","CONTRADICTION","TEACHER_DIVERSITY","TEMPORAL_PRECEDENCE","INVARIANCE","CAUSAL_SUPPORT","COUNTER_EVIDENCE","RESIDUAL_MECHANISM","NUMBER_POSITION_COMPATIBILITY","CROSS_POSITION_FLOW","ACTION_UTILITY","ALGORITHM_COMPLEMENTARITY","EVOLUTION_CREDIT","RESOURCE_EFFICIENCY"};
    public static final class Definition {
        public final String id,namespace,kind,producerClass,producerMethod,consumerClass,table,outputScope,algorithmConsumer;
        public final List<String> consumerMethods;
        private Definition(String id,String ns,String kind,String producer,String method,String consumer,List<String> methods,String table,String scope,String algorithm){
            this.id=id;namespace=ns;this.kind=kind;producerClass=producer;producerMethod=method;consumerClass=consumer;consumerMethods=Collections.unmodifiableList(new ArrayList<>(methods));this.table=table;outputScope=scope;algorithmConsumer=algorithm;
        }
        public Map<String,Object> json(){Map<String,Object> m=new LinkedHashMap<>();m.put("id",id);m.put("namespace",namespace);m.put("kind",kind);m.put("producerClass",producerClass);m.put("producerMethod",producerMethod);m.put("consumerClass",consumerClass);m.put("consumerMethods",consumerMethods);m.put("sourceTable",table);m.put("outputScope",outputScope);m.put("algorithmConsumer",algorithmConsumer);if(namespace.equals("R930")){m.put("typedContract",MatrixTensorR30R1.contract(kind));m.put("typedProducer","RuntimeR27.MatrixProductionTask / MatrixResearchR30R1.build + MatrixEvidenceR30R1.compute/stored");m.put("typedMachineSource","r30r1_research_measure / MATRIX_<kind>");m.put("typedConsumers",typedConsumers(kind));m.put("consumerActivation","REQUIRES_ACTUAL_ROLE_SOURCE_ABI_AND_INPUTS; STATIC_LINK_IS_NOT_RUNTIME_CONSUMPTION");m.put("legacyShape","POSITION_10_NUMBER_10");m.put("productionEvent","NEW_IMMUTABLE_SELECT_DATASET_OR_STUDENT");}return m;}
    }
    static java.util.List<String> typedConsumers(String kind){switch(kind){case "EVIDENCE_TEACHER":case "FORWARD_REASONING":case "REVERSE_REASONING":case "CONTRADICTION":case "INVARIANCE":return java.util.Collections.singletonList(STRATEGY+"ResearchContextR30R1.premiseHypothesis");case "TEACHER_DIVERSITY":case "ALGORITHM_COMPLEMENTARITY":return java.util.Collections.singletonList(STRATEGY+"ResearchContextR30R1.fusionParents / FusionOpsR27.fuse");case "STUDENT_ERROR":case "FEEDBACK":return java.util.Collections.singletonList(STRATEGY+"ResearchContextR30R1.calibrationHypothesis");case "COUNTERFACTUAL":return java.util.Collections.singletonList(STRATEGY+"ResearchStoreR30R1.ready/consume");case "CAUSAL_SUPPORT":case "COUNTER_EVIDENCE":return java.util.Collections.singletonList(STRATEGY+"ResearchContextR30R1.measuredPrune");case "RESIDUAL_MECHANISM":return java.util.Collections.singletonList(STRATEGY+"ResearchContextR30R1.residualHypothesis");case "NUMBER_POSITION_COMPATIBILITY":return java.util.Collections.singletonList(STRATEGY+"ResearchContextR30R1.position");case "CROSS_POSITION_FLOW":return java.util.Collections.singletonList(STRATEGY+"ResearchContextR30R1.cross");case "ACTION_UTILITY":return java.util.Collections.singletonList(STRATEGY+"ResearchContextR30R1.actionHypothesis");case "EVOLUTION_CREDIT":return java.util.Collections.singletonList(STRATEGY+"ResearchContextR30R1.weights/familyCredit/appendBudgetReceipt");case "RESOURCE_EFFICIENCY":return java.util.Collections.singletonList(STRATEGY+"ResearchPoolR27.refreshResourceBudget/recordResourceUse");case "TEMPORAL_PRECEDENCE":return java.util.Collections.singletonList(STRATEGY+"ModelReplayStudyR30R1.settle/summary");default:throw new IllegalArgumentException(kind);}}
    private static final List<Definition> DEFINITIONS;
    static{
        List<Definition> rows=new ArrayList<>();
        for(String kind:NATIVE)rows.add(new Definition("R930:"+kind,"R930",kind,ROOT+"MatrixSnapshotFeedbackR930$FrozenMatrix","<init>",ROOT+"ResearchEvolutionStoreR930",Collections.singletonList("putSnapshot"),"research_matrix_snapshot_r930","COMMON_PIT_MATRIX_CONTRACT",kind.equals("EVIDENCE_TEACHER")||kind.equals("FORWARD_REASONING")?ROOT+"MatrixSnapshotFeedbackR930.settle":"PERSISTENCE_AND_DIAGNOSTICS"));
        rows.add(new Definition(SU2_ID,"SU2","FACTOR_TEACHER_CHECKPOINT",STRATEGY+"MatrixSnapshotSU2","capture",STRATEGY+"MatrixStoreSU2",Arrays.asList("save","maintain","report"),"su2_matrix_snapshot","EXISTING_FACTOR_AND_TEACHER_MATRIX_BUNDLE",STRATEGY+"MatrixSnapshotSU2.support / MatrixStoreSU2.maintain"));
        DEFINITIONS=Collections.unmodifiableList(rows);
    }
    public static List<Definition> all(){return DEFINITIONS;}
    public static Definition require(String id){for(Definition d:DEFINITIONS)if(d.id.equals(id))return d;throw new IllegalArgumentException("R30_UNKNOWN_MATRIX:"+id);}
    /** Check links without initialising model classes or running training inside a report read. */
    public static boolean declared(String className,String method){
        try{Class<?> type=Class.forName(className,false,MatrixRegistryR30.class.getClassLoader());if(method.equals("<init>"))return type.getDeclaredConstructors().length>0;for(java.lang.reflect.Method m:type.getDeclaredMethods())if(m.getName().equals(method))return true;return false;}
        catch(ReflectiveOperationException|LinkageError missing){return false;}
    }
}
