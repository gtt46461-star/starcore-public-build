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
        public Map<String,Object> json(){Map<String,Object> m=new LinkedHashMap<>();m.put("id",id);m.put("namespace",namespace);m.put("kind",kind);m.put("producerClass",producerClass);m.put("producerMethod",producerMethod);m.put("consumerClass",consumerClass);m.put("consumerMethods",consumerMethods);m.put("sourceTable",table);m.put("outputScope",outputScope);m.put("algorithmConsumer",algorithmConsumer);return m;}
    }
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
