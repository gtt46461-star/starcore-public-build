package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Concrete family dispatch. An unavailable mechanism fails explicitly; no generic fallback. */
public final class FamilyRegistryR26 {
    private FamilyRegistryR26() {}
    private static final java.util.concurrent.CopyOnWriteArrayList<String> AVAILABLE=new java.util.concurrent.CopyOnWriteArrayList<>(Arrays.asList("F01","F02","F03","F04","F05","F06","F07","F08","F09","F10","F11","F12","F13","F14","F15","F16","F17","F18","F19","F20","F21","F22","F24"));
    public static final List<String> EXECUTABLE=Collections.unmodifiableList(AVAILABLE);
    private static final class Teacher {final Map<String,String> binding;final ModelReplaySU2.Adapter adapter;Teacher(Map<String,String> b,ModelReplaySU2.Adapter a){binding=Collections.unmodifiableMap(new TreeMap<>(b));adapter=a;}}
    private static volatile Teacher teacher;
    public static synchronized void bindLegacyTeacher(Map<String,String> binding,ModelReplaySU2.Adapter adapter){
        if(binding==null||adapter==null||!"1".equals(binding.get("abi"))||!"LEGACY_REPORTED_PREFERENCE_PROJECTION_V1".equals(binding.get("scope")))throw new IllegalArgumentException("F23_BINDING_ABI_OR_SCOPE");
        for(String key:new String[]{"asset","teacher","state"})if(binding.get(key)==null||!binding.get(key).matches("[0-9a-f]{64}"))throw new IllegalArgumentException("F23_BINDING_HASH:"+key);
        FeatureCacheR27.registerFamilyConstructorBudgetR31(23,DistillationFamilyR26.constructorEnvelopeR31(),"F23_STUDENT_BIAS_CONTEXT_REF_AND_DIAGNOSTICS;SHARED_VM_OWNER_SEPARATELY_ADMITTED");
        Teacher prepared=new Teacher(binding,adapter);ModelReplaySU2.ADAPTERS.put("R26F23",adapter);teacher=prepared;AVAILABLE.addIfAbsent("F23");
    }
    public static Map<String,String> legacyBinding(){Teacher snapshot=teacher;return snapshot==null?Collections.emptyMap():snapshot.binding;}
    public static OnlineModelsSU2.Model build(String id){switch(id){
        case "F03":case "F04":case "F05":case "F06":case "F07":return new NeuralFamilyR26(id);
        case "F02":return new FamilyF02KernelR26();
        case "F18":return new FamilyF18KernelR26();
        case "F13":case "F14":return new SymbolicFamilyR26(id);
        case "F24":return new PortfolioFamilyR26();
        case "F15":case "F16":return new SetFamilyR26(id);
        case "F23":{Teacher snapshot=teacher;if(snapshot==null)throw new IllegalArgumentException("F23_ORIGINAL_TEACHER_NOT_BOUND");return snapshot.adapter.build(snapshot.binding);}
        case "F22":return new ResidualFamilyR26();
        case "F01":return new FamilyF01KernelR26();
        case "F08":return new FamilyF08KernelR26();
        case "F09":return new FamilyF09KernelR26();
        case "F10":return ModelReplaySU2.build("BOCPD",ModelEvolutionSU2.centralParams("BOCPD"));
        case "F11":return new FamilyF11KernelR26(.02);
        case "F12":return new FamilyF12KernelR26();
        case "F17":return new PermutationModelR26(.02);
        case "F19":return new FamilyF19TensorR26(.02,true,false);
        case "F20":return new FamilyF20KernelR26();
        case "F21":return new FamilyF21KernelR26();
        default:throw new IllegalArgumentException("R26_FAMILY_IMPLEMENTATION_BLOCKED:"+id);
    }}
    public static String implementation(String id){switch(id){case "F23":return legacyBinding().isEmpty()?"":"DistillationFamilyR26:original-VM-preference-projection";case "F22":return "ResidualFamilyR26";case "F15":case "F16":return "SetFamilyR26:"+id;case "F13":case "F14":return "SymbolicFamilyR26:"+id;case "F24":return "PortfolioFamilyR26";case "F02":return "FamilyF02KernelR26";case "F18":return "FamilyF18KernelR26";case "F03":case "F04":case "F05":case "F06":case "F07":return "NeuralFamilyR26:"+id;case "F01":return "FamilyF01KernelR26";case "F09":return "FamilyF09KernelR26";case "F11":return "FamilyF11KernelR26";case "F12":return "FamilyF12KernelR26";case "F17":return "PermutationModelR26";case "F19":return "FamilyF19TensorR26:exact-cp-adagrad";case "F08":return "FamilyF08KernelR26:causal-filter-router";case "F10":return "OnlineModelsSU2.Bocpd";case "F20":return "FamilyF20KernelR26:causal-state-fragment-retrieval";case "F21":return "FamilyF21KernelR26:discounted-relation-statistics";default:return "";}}
    public static ModelGenomeSU2 genome(String id){if(!EXECUTABLE.contains(id))throw new IllegalArgumentException("R26_BLOCKED_FAMILY");Map<String,String> parameters=id.equals("F23")?legacyBinding():Collections.singletonMap("abi","1");if(id.equals("F19")){parameters=new TreeMap<>(parameters);parameters.put("tensor","2");parameters.put("position","true");parameters.put("state","NONE");parameters.put("optimizer","ADAGRAD_V1");parameters.put("control","NONE");}if(id.equals("F20")){parameters=new TreeMap<>(parameters);parameters.put("fragments","1");parameters.put("slots","16");parameters.put("retrieval","MATCH");}return new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("R26"+id,parameters,1)),"STATIC",true,"MASS",.15,.2,false);}
    /** R31-B07-C01: one acceptance record per family on one causal stream (predict before update, serialize, restore
     * into a fresh instance, continue bit-exactly). A registration or an implementation name is never an effective strategy. */
    public static final List<String> ALL_FAMILIES_R31;
    static{List<String> all=new ArrayList<>();for(int i=1;i<=24;i++)all.add(String.format(Locale.ROOT,"F%02d",i));ALL_FAMILIES_R31=Collections.unmodifiableList(all);}
    public enum AcceptanceStatusR31{EXECUTED,DEPENDENCY_NOT_READY,FAILED}
    public static final class AcceptanceR31{
        public final String id,reason,stateSha256;public final AcceptanceStatusR31 status;public final int steps,stateBytes;public final long costNanos;
        AcceptanceR31(String id,AcceptanceStatusR31 status,String reason,int steps,int stateBytes,long costNanos,String stateSha256){this.id=id;this.status=status;this.reason=reason==null?"":reason;this.steps=steps;this.stateBytes=stateBytes;this.costNanos=costNanos;this.stateSha256=stateSha256==null?"":stateSha256;}
        public boolean executed(){return status==AcceptanceStatusR31.EXECUTED;}
    }
    public static final class CapabilityR31{
        public final String id,inputAbi,implementation,tasks,stateSerialization,owner,cost,dependency;public final AcceptanceR31 acceptance;
        CapabilityR31(String id,String inputAbi,String implementation,String tasks,String stateSerialization,String owner,String cost,String dependency,AcceptanceR31 acceptance){this.id=id;this.inputAbi=inputAbi;this.implementation=implementation;this.tasks=tasks;this.stateSerialization=stateSerialization;this.owner=owner;this.cost=cost;this.dependency=dependency;this.acceptance=acceptance;}
        public Map<String,Object> json(){Map<String,Object> r=new LinkedHashMap<>();r.put("id",id);r.put("inputAbi",inputAbi);r.put("implementation",implementation);r.put("tasks",tasks);r.put("stateSerialization",stateSerialization);r.put("owner",owner);r.put("cost",cost);r.put("dependency",dependency);r.put("acceptance",acceptance.status.name());r.put("acceptanceReason",acceptance.reason);r.put("steps",(long)acceptance.steps);r.put("stateBytes",(long)acceptance.stateBytes);r.put("costNanos",acceptance.costNanos);r.put("stateSha256",acceptance.stateSha256);return r;}
    }
    static String costClassR31(String id){switch(id){case "F03":case "F04":case "F05":case "F06":case "F07":case "F19":case "F23":return "NEURAL_OR_TENSOR";case "F10":case "F20":case "F21":return "STATE_RETRIEVAL";case "F15":case "F16":case "F24":return "SET_OR_PORTFOLIO";default:return "KERNEL";}}
    public static String dependencyR31(String id){if(!ALL_FAMILIES_R31.contains(id))throw new IllegalArgumentException("R31_UNKNOWN_FAMILY:"+id);if(id.equals("F23")&&(legacyBinding().isEmpty()||!ModelReplaySU2.ADAPTERS.containsKey("R26F23")))return "DEPENDENCY_NOT_READY:F23_ORIGINAL_TEACHER_NOT_BOUND";return EXECUTABLE.contains(id)?"":"DEPENDENCY_NOT_READY:FAMILY_NOT_REGISTERED";}
    private static long[] stepR31(OnlineModelsSU2.Model model,int[] draw,float[] state){
        float[][] p=new float[10][10];model.predict(p,state);long[] bits=new long[100];
        for(int q=0;q<10;q++)for(int k=0;k<10;k++){if(!Float.isFinite(p[q][k]))throw new IllegalStateException("R31_FAMILY_NONFINITE_OUTPUT");bits[q*10+k]=Float.floatToIntBits(p[q][k]);}
        model.update(draw);return bits;
    }
    /** Executes the family on `causal` (rows are complete 10-position draws, oldest first). Half the stream trains,
     * the state is serialized and restored into a fresh instance, and both must continue identically on the rest. */
    public static AcceptanceR31 acceptR31(String id,int[][] causal){
        String dependency=dependencyR31(id);
        if(!dependency.isEmpty())return new AcceptanceR31(id,AcceptanceStatusR31.DEPENDENCY_NOT_READY,dependency,0,0,0,"");
        if(causal==null||causal.length<4)throw new IllegalArgumentException("R31_ACCEPTANCE_STREAM_TOO_SHORT");
        long started=System.nanoTime();int split=causal.length/2,steps=0;OnlineModelsSU2.Model original=null,restored=null;
        try{
            original=build(id);float[] state=new float[Math.max(1,original.stateWidth())];
            for(int t=0;t<split;t++){stepR31(original,causal[t],state);steps++;}
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();original.write(new java.io.DataOutputStream(bytes));byte[] encoded=bytes.toByteArray();
            restored=build(id);java.io.DataInputStream in=new java.io.DataInputStream(new java.io.ByteArrayInputStream(encoded));restored.read(in);
            if(in.available()!=0)return new AcceptanceR31(id,AcceptanceStatusR31.FAILED,"TRAILING_STATE_BYTES",steps,encoded.length,System.nanoTime()-started,"");
            float[] restoredState=state.clone();
            for(int t=split;t<causal.length;t++){if(!Arrays.equals(stepR31(original,causal[t],state),stepR31(restored,causal[t],restoredState)))return new AcceptanceR31(id,AcceptanceStatusR31.FAILED,"RESTORE_DIVERGED_AT_"+t,steps,encoded.length,System.nanoTime()-started,"");steps++;}
            return new AcceptanceR31(id,AcceptanceStatusR31.EXECUTED,"",steps,encoded.length,System.nanoTime()-started,StatsR27.sha256(encoded));
        }catch(RuntimeException|java.io.IOException failure){return new AcceptanceR31(id,AcceptanceStatusR31.FAILED,failure.getClass().getSimpleName()+":"+failure.getMessage(),steps,0,System.nanoTime()-started,"");}
        finally{closeR31(original);closeR31(restored);}
    }
    /** Acceptance instances own memory leases; they are released when the acceptance ends. */
    static void closeR31(OnlineModelsSU2.Model model){if(model instanceof AutoCloseable)try{((AutoCloseable)model).close();}catch(Exception closeFailure){throw new IllegalStateException("R31_ACCEPTANCE_MODEL_CLOSE",closeFailure);}}
    public static CapabilityR31 capabilityR31(String id,AcceptanceR31 acceptance){
        if(acceptance==null||!acceptance.id.equals(id))throw new IllegalArgumentException("R31_ACCEPTANCE_FOR_OTHER_FAMILY");
        String abi=id.equals("F23")?"SU2_DRAW_10_POSITION_V1+LEGACY_REPORTED_PREFERENCE_PROJECTION_V1":"SU2_DRAW_10_POSITION_V1";
        return new CapabilityR31(id,abi,implementation(id),"PREDICT_10x10;UPDATE;SERIALIZE;RESTORE","OnlineModelsSU2.Model.write/read","R26_REPLAY_OWNER_FENCED",costClassR31(id),dependencyR31(id),acceptance);
    }
    public static List<CapabilityR31> capabilitiesR31(int[][] causal){List<CapabilityR31> out=new ArrayList<>();for(String id:ALL_FAMILIES_R31)out.add(capabilityR31(id,acceptR31(id,causal)));return Collections.unmodifiableList(out);}
    /** Only executed acceptances count; registered, declared or dependency-blocked families are not effective strategies. */
    public static int effectiveCountR31(Collection<CapabilityR31> rows){int n=0;for(CapabilityR31 row:rows)if(row.acceptance.executed()&&row.dependency.isEmpty())n++;return n;}
}
