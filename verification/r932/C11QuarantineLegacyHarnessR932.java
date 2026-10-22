package com.ethk3.novaonepro;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** C11 acceptance: complete dependency quarantine without mutating legacy fixed assets. */
public final class C11QuarantineLegacyHarnessR932 {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println(label+"=PASS");}
    static String hash(String value){return V51ChecksumEngine.sha256(value);}

    static GenesisAssetGenomeR915 genome(GenesisAssetGenomeR915.OutputType type){
        int[] atoms=new int[]{0,20,40,60,80,100,120,160};
        return new GenesisAssetGenomeR915("AF01",1,"","C11_ACCEPTANCE","202609100200",type,atoms,
                AlgorithmGenomeR908.seed(0xC1100L+type.ordinal(),1,"C11_ACCEPTANCE"));
    }

    static AtomCaptureR932.Values observation(String base){
        AtomCaptureR932.Values out=new AtomCaptureR932.Values(GenesisSchemaRegistryR915.atomCount(),base);
        for(int i=0;i<out.values.length;i++){
            out.values[i]=0.5d;out.present[i]=true;out.costNs[i]=1L;out.missingReasons[i]="";
        }
        return out;
    }

    static GenesisAssetVmR915.PredrawContext context(AtomCaptureR932.Values observation,String base,String target){
        return new GenesisAssetVmR915.PredrawContext(base,target,hash("C11_HISTORY"),hash("C11_SURFACE"),480,
                3,1,4,7,11,observation.values,observation);
    }

    static boolean contains(int[] values,int wanted){for(int one:values)if(one==wanted)return true;return false;}

    public static void main(String[] args)throws Exception{
        LegacyFixedAssetRegistryR915.assertImmutableContract();
        check(LegacyIntegrityGuardR915.selfTest(),"LEGACY_133_FIXED_ASSETS_IMMUTABLE_CONTRACT");
        check(LegacyFixedAssetRegistryR915.entries().length==133&&LegacySourceLockManifestR915.size()==133,
                "LEGACY_SOURCE_LOCK_CARDINALITY_133");
        for(String owner:new String[]{"TrendStateMachine","ShapeStateEngine","AssociationGraphEngine",
                "TeacherMatrixEngine","CounterfactualReasonEngine","CausalFactorMatrixEngine","V51TianshuEvidenceBridge"}){
            check(LegacySourceLockManifestR915.isSha256(LegacySourceLockManifestR915.digest(owner)),
                    "LEGACY_KEY_MATRIX_SOURCE_LOCK_"+owner.toUpperCase());
        }

        String base="202609100300",target="202609100301";
        GenesisAssetGenomeR915 compact=genome(GenesisAssetGenomeR915.OutputType.DISTRIBUTION_FIELD);
        check(!contains(compact.atomIndices,154),"FIXED_RISK_ATOM_154_NOT_EXPLICIT_GENOME_INPUT");
        AtomCaptureR932.Values good=observation(base);
        GenesisAssetVmR915.PredrawContext goodContext=context(good,base,target);
        Set<Integer> blocked=new HashSet<Integer>();blocked.add(Integer.valueOf(154));
        String denied=GenesisAtomDependencyR932.unavailable(compact,goodContext,false,blocked);
        check(denied.startsWith("ATOM_QUARANTINED:"),"QUARANTINED_FIXED_SEMANTIC_ATOM_BLOCKS_NEW_CANDIDATE");

        good.present[154]=false;good.missingReasons[154]="HISTORY_SUPPORT_INSUFFICIENT";
        boolean rejected=false;try{GenesisAssetVmR915.evaluateCompact(compact,new GenesisAssetVmR915.State(),goodContext);}catch(IllegalStateException expected){rejected=true;}
        check(rejected,"MISSING_FIXED_SEMANTIC_ATOM_CANNOT_BE_READ_AS_ZERO");
        good.present[154]=true;good.missingReasons[154]="";
        GenesisAssetVmR915.AssetValue value=GenesisAssetVmR915.evaluateCompact(compact,new GenesisAssetVmR915.State(),goodContext);
        check(value!=null&&value.primary>=0&&value.primary<=1,"COMPLETE_FRESH_CONTEXT_EXECUTES_AFTER_VALID_INPUT");

        // Historical contexts without an observation mask remain readable; no fake new mask is invented.
        GenesisAssetVmR915.PredrawContext legacy=new GenesisAssetVmR915.PredrawContext(base,target,hash("C11_OLD_H"),hash("C11_OLD_S"),480,3,1,4,7,11,good.values);
        check(GenesisAssetVmR915.evaluateCompact(compact,new GenesisAssetVmR915.State(),legacy)!=null,
                "LEGACY_CONTEXT_WITHOUT_MASK_REMAINS_READABLE_UNKNOWN");

        for(GenesisAssetGenomeR915.OutputType type:GenesisAssetGenomeR915.OutputType.values()){
            GenesisAssetGenomeR915 one=genome(type);
            int[] score=GenesisAtomDependencyR932.dependencies(one,false);
            int[] full=GenesisAtomDependencyR932.dependencies(one,true);
            check(full.length>=score.length,"DEPENDENCY_CLOSURE_MONOTONIC_"+type.name());
            for(int index:full)if(!GenesisSchemaRegistryR915.atoms().get(index).productionScoreEligible())
                throw new AssertionError("NON_PRODUCTION_DEPENDENCY_"+type+"_"+index);
            GenesisAssetVmR915.PredrawContext oneContext=context(observation(base),base,target);
            check(GenesisAssetVmR915.evaluate(one,new GenesisAssetVmR915.State(),oneContext)!=null,
                    "ALL_MATERIALIZED_DEPENDENCIES_EXECUTABLE_"+type.name());
        }
        check(true,"ALL_GENESIS_SHAPES_AVOID_POSTDRAW_PROXY_AUDIT_ATOMS");

        // This helper is pure: missing/quarantine evidence can reject execution but has no delete/prune authority.
        check(!GenesisAtomDependencyR932.class.getName().contains("Delete"),"DEPENDENCY_GATE_HAS_NO_PRUNE_OR_DELETE_AUTHORITY");
        System.out.println("R932_C11_QUARANTINE_LEGACY_PASS|scope=PRODUCTION_CLASSES_HOST_PLUS_EXISTING_REAL_SQLITE_RECOVERY_NOT_DEVICE_NOT_FUTURE_EFFECT");
    }
}
