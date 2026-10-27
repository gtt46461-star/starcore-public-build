package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Concrete family dispatch. An unavailable mechanism fails explicitly; no generic fallback. */
public final class FamilyRegistryR26 {
    private FamilyRegistryR26() {}
    public static final List<String> EXECUTABLE=Collections.unmodifiableList(Arrays.asList("F01","F03","F04","F05","F06","F07","F08","F09","F10","F11","F12","F17","F19","F20","F21"));
    public static OnlineModelsSU2.Model build(String id){switch(id){
        case "F03":case "F04":case "F05":case "F06":case "F07":return new NeuralFamilyR26(id);
        case "F01":return new FamilyF01KernelR26();
        case "F08":return ModelReplaySU2.build("HMM",ModelEvolutionSU2.centralParams("HMM"));
        case "F09":return new FamilyF09KernelR26();
        case "F10":return ModelReplaySU2.build("BOCPD",ModelEvolutionSU2.centralParams("BOCPD"));
        case "F11":return new FamilyF11KernelR26(.02);
        case "F12":return new FamilyF12KernelR26();
        case "F17":return new PermutationModelR26(.02);
        case "F19":return new FamilyF19KernelR26(.02);
        case "F20":return ModelReplaySU2.build("REGMEM",ModelEvolutionSU2.centralParams("REGMEM"));
        case "F21":return ModelReplaySU2.build("RELBAYES",ModelEvolutionSU2.centralParams("RELBAYES"));
        default:throw new IllegalArgumentException("R26_FAMILY_IMPLEMENTATION_BLOCKED:"+id);
    }}
    public static String implementation(String id){switch(id){case "F03":case "F04":case "F05":case "F06":case "F07":return "NeuralFamilyR26:"+id;case "F01":return "FamilyF01KernelR26";case "F09":return "FamilyF09KernelR26";case "F11":return "FamilyF11KernelR26";case "F12":return "FamilyF12KernelR26";case "F17":return "PermutationModelR26";case "F19":return "FamilyF19KernelR26";case "F08":return "OnlineModelsSU2.Hmm";case "F10":return "OnlineModelsSU2.Bocpd";case "F20":return "OnlineModelsSU2.RegMem";case "F21":return "OnlineModelsSU2.RelBayes";default:return "";}}
    public static ModelGenomeSU2 genome(String id){if(!EXECUTABLE.contains(id))throw new IllegalArgumentException("R26_BLOCKED_FAMILY");return new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("R26"+id,Collections.singletonMap("abi","1"),1)),"STATIC",true,"MASS",.15,.2,false);}
}
