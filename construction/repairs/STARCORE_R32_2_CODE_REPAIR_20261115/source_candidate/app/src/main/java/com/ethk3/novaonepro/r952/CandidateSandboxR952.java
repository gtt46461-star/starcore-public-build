package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.AppConfig;
/** Pure bounded candidate execution. The caller owns the frozen context lifetime. */
public final class CandidateSandboxR952 {
    public static final long MAX_COST_NS=50_000_000L;
    private CandidateSandboxR952(){}
    public static int execute(CandidateCompilerR952.Program p,PITSnapshotContractR952 pit){
        return execute(p,pit,CandidateCompilerR952.Model.initial()).mask;
    }
    public static Prediction execute(CandidateCompilerR952.Program p,PITSnapshotContractR952 pit,CandidateCompilerR952.Model model){
        if(p==null||p.ast.formatVersion==3)throw new IllegalArgumentException("AST3_CONTEXT_REQUIRED");
        return execute(p,pit,model,null);
    }
    public static Prediction execute(CandidateCompilerR952.Program p,PITSnapshotContractR952 pit,
            CandidateCompilerR952.Model model,FrozenGraphContextCP4 context){
        if(p==null||pit==null||model==null)throw new IllegalArgumentException("R958_SANDBOX_INPUT");
        if(p.ast.formatVersion==3){
            if(context==null)throw new IllegalArgumentException("AST3_CONTEXT_REQUIRED");
            context.requireOpen();
            if(model.isLegacy()||!model.hash().equals(context.modelHash)
                ||!model.matchesManifest(p.manifest.hash,context.normalizer.hash))
                throw new IllegalStateException("MODEL_FEATURE_HASH_MISMATCH");
        }else if(!model.isLegacy())throw new IllegalArgumentException("MODEL_AST_MISMATCH");
        long start=System.nanoTime();WorldFeatureComputationR974.Result computed=p.compute(pit,context);
        double[] features=computed.values();
        if(features.length!=model.inputDimension())throw new IllegalStateException("MODEL_FEATURE_DIMENSION_MISMATCH");
        double[] probability=model.probabilities(features);int mask=CandidateCompilerR952.top3(probability);
        if(System.nanoTime()-start>MAX_COST_NS)throw new IllegalStateException("R952_SANDBOX_COST");
        if(AppConfig.countK3MaskNumbers(mask)!=3)throw new IllegalStateException("R952_SANDBOX_FIXED_K3");
        return new Prediction(mask,CandidateCompilerR952.encode(features),computed,probability);
    }
    public static final class Prediction {
        public final int mask;public final String features;public final WorldFeatureComputationR974.Result computation;
        private final double[] probabilities;
        Prediction(int m,String f,WorldFeatureComputationR974.Result c,double[] p){mask=m;features=f;computation=c;probabilities=p.clone();}
        public double[] probabilities(){return probabilities.clone();}
    }
    public static void rejectReal(){throw new SecurityException("R952_RESEARCH_MAY_NOT_CREATE_REAL");}
}
