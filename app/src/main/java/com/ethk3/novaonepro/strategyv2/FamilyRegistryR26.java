package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Concrete family dispatch. An unavailable mechanism fails explicitly; no generic fallback. */
public final class FamilyRegistryR26 {
    private FamilyRegistryR26() {}
    private static final java.util.concurrent.CopyOnWriteArrayList<String> AVAILABLE=new java.util.concurrent.CopyOnWriteArrayList<>(Arrays.asList("F01","F02","F03","F04","F05","F06","F07","F08","F09","F10","F11","F12","F13","F14","F15","F16","F17","F18","F19","F20","F21","F22","F24"));
    public static final List<String> EXECUTABLE=Collections.unmodifiableList(AVAILABLE);
    private static volatile Map<String,String> teacherBinding=Collections.emptyMap();
    public static synchronized void bindLegacyTeacher(Map<String,String> binding,ModelReplaySU2.Adapter adapter){if(binding==null||!binding.containsKey("teacher")||!binding.containsKey("state")||adapter==null)throw new IllegalArgumentException("F23_BINDING");teacherBinding=Collections.unmodifiableMap(new TreeMap<>(binding));ModelReplaySU2.ADAPTERS.put("R26F23",adapter);AVAILABLE.addIfAbsent("F23");}
    public static OnlineModelsSU2.Model build(String id){switch(id){
        case "F03":case "F04":case "F05":case "F06":case "F07":return new NeuralFamilyR26(id);
        case "F02":return new FamilyF02KernelR26();
        case "F18":return new FamilyF18KernelR26();
        case "F13":case "F14":return new SymbolicFamilyR26(id);
        case "F24":return new PortfolioFamilyR26();
        case "F15":case "F16":return new SetFamilyR26(id);
        case "F23":{ModelReplaySU2.Adapter adapter=ModelReplaySU2.ADAPTERS.get("R26F23");if(adapter==null)throw new IllegalArgumentException("F23_ORIGINAL_TEACHER_NOT_BOUND");return adapter.build(teacherBinding);}
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
        case "F21":return ModelReplaySU2.build("RELBAYES",ModelEvolutionSU2.centralParams("RELBAYES"));
        default:throw new IllegalArgumentException("R26_FAMILY_IMPLEMENTATION_BLOCKED:"+id);
    }}
    public static String implementation(String id){switch(id){case "F23":return teacherBinding.isEmpty()?"":"DistillationFamilyR26:original-VM-preference-projection";case "F22":return "ResidualFamilyR26";case "F15":case "F16":return "SetFamilyR26:"+id;case "F13":case "F14":return "SymbolicFamilyR26:"+id;case "F24":return "PortfolioFamilyR26";case "F02":return "FamilyF02KernelR26";case "F18":return "FamilyF18KernelR26";case "F03":case "F04":case "F05":case "F06":case "F07":return "NeuralFamilyR26:"+id;case "F01":return "FamilyF01KernelR26";case "F09":return "FamilyF09KernelR26";case "F11":return "FamilyF11KernelR26";case "F12":return "FamilyF12KernelR26";case "F17":return "PermutationModelR26";case "F19":return "FamilyF19TensorR26:exact-cp-adagrad";case "F08":return "FamilyF08KernelR26:causal-filter-router";case "F10":return "OnlineModelsSU2.Bocpd";case "F20":return "FamilyF20KernelR26:causal-state-fragment-retrieval";case "F21":return "OnlineModelsSU2.RelBayes";default:return "";}}
    public static ModelGenomeSU2 genome(String id){if(!EXECUTABLE.contains(id))throw new IllegalArgumentException("R26_BLOCKED_FAMILY");Map<String,String> parameters=id.equals("F23")?teacherBinding:Collections.singletonMap("abi","1");if(id.equals("F19")){parameters=new TreeMap<>(parameters);parameters.put("tensor","2");parameters.put("position","true");parameters.put("state","NONE");parameters.put("optimizer","ADAGRAD_V1");parameters.put("control","NONE");}if(id.equals("F20")){parameters=new TreeMap<>(parameters);parameters.put("fragments","1");parameters.put("slots","16");parameters.put("retrieval","MATCH");}return new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("R26"+id,parameters,1)),"STATIC",true,"MASS",.15,.2,false);}
}
