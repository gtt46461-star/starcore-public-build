package com.ethk3.novaonepro;

import java.util.*;
import org.json.*;

/** Captures upstream tensors from the existing predraw computation, never by inverting META96. */
public final class RawTrainingInputR932 {
    public static final String PREFIX="R932_UPSTREAM_RAW=";
    public static final String HASH_PREFIX="R932_UPSTREAM_SHA256=";
    public static final String REVISION="R932_FROZEN_RAW_UPSTREAM_V1";
    private RawTrainingInputR932(){}
    static void append(List<String> proofs,V51RuntimeResult result){
        if(result==null||result.features==null||result.numberModel==null||result.states==null)return;
        try {
            JSONObject root=new JSONObject();root.put("revision",REVISION);root.put("target_issue",result.context.targetIssue);
            root.put("base_issue",result.context.baseIssue);root.put("history_hash",result.context.historyHash);
            root.put("model_bundle_hash",result.context.artifactBundleHash);root.put("created_at",result.context.createdAt);
            root.put("number_schema_sha256",V51TrajectoryFeatureEngine.schemaHash());
            root.put("meta_schema_sha256",V51ActionMetaFeatureEngine.featureManifestHash());
            root.put("domain_schema_sha256",DomainRouterR929.installedSchemaHash());
            JSONArray numbers=new JSONArray(),states=new JSONArray(),experts=new JSONArray(),ids=new JSONArray(),versions=new JSONArray(),missing=new JSONArray();
            boolean ready=true;
            if(result.features.numberFeatures.length!=10||result.states.length!=10)throw new IllegalArgumentException("R932_UPSTREAM_POSITIONS");
            for(int p=0;p<10;p++){
                JSONArray position=new JSONArray();if(result.features.numberFeatures[p].length!=10)throw new IllegalArgumentException("R932_UPSTREAM_NUMBERS");
                for(int n=0;n<10;n++)position.put(vector(result.features.numberFeatures[p][n],18));numbers.put(position);
                V51Models.StateTrajectorySnapshot state=result.states[p];
                if(state==null||!result.context.targetIssue.equals(state.targetIssue))throw new IllegalArgumentException("R932_UPSTREAM_STATE_TARGET");
                states.put(vector(new double[]{state.confidence,state.changePointRisk,state.remainingLife,state.posterior},4));
            }
            root.put("state_fields",new JSONArray(Arrays.asList("confidence","change_point_risk","remaining_life","posterior")));
            String[] expected=V51ExpertGateArtifact.EXPERT_IDS;
            for(String id:expected){
                V51Models.ExpertEvidence found=null;
                for(V51Models.ExpertEvidence e:result.numberModel.experts)if(e!=null&&id.equals(e.expertId)){if(found!=null)throw new IllegalArgumentException("R932_DUPLICATE_EXPERT");found=e;}
                ids.put(id);versions.put(found==null?JSONObject.NULL:found.artifactId);
                if(found==null){ready=false;experts.put(JSONObject.NULL);missing.put("MISSING_EXPERT:"+id);continue;}
                JSONArray positions=new JSONArray();
                if(found.rawScore.length!=10)throw new IllegalArgumentException("R932_EXPERT_POSITIONS");
                for(double[] row:found.rawScore)positions.put(vector(row,10));experts.put(positions);
            }
            if(result.selected!=null&&result.selected.predictability!=null) {
                V51Models.PredictabilityEvidence p=result.selected.predictability;
                root.put("predictability_missing_reason",p.missingReason);
                if(p.inputFeatures.length==12&&(p.missingReason.length()==0||p.missingReason.equals("PREDICTABILITY_SELECTION_POLICY_CHANGED")))root.put("predictability_inputs",vector(p.inputFeatures,12));
                V51Models.PositionAction a=result.selected.action;
                root.put("selected_ordinal",K3ActionSpaceEngine.globalOrdinal(a.position,a.n1,a.n2,a.n3));
                root.put("selected_prediction_hash",a.checksum);
                root.put("selected_rank_score",a.rankScore);
                root.put("selection_policy_hash",result.selected.selectionPolicyHash);
                root.put("predictability_schema_hash",ModelBundleRuntimeR932.expectedSchema("PREDICTABILITY"));
            }
            try{root.put("module_replay",FrozenModuleInputR932.capture(result));}catch(Exception unavailable){root.put("module_replay_missing_reason",unavailable.getMessage());}
            root.put("number_features",numbers);root.put("states",states);root.put("expert_ids",ids);root.put("expert_versions",versions);
            root.put("expert_logits",experts);root.put("missing_reasons",missing);root.put("ready",ready);
            String payload=root.toString();proofs.add(PREFIX+payload);proofs.add(HASH_PREFIX+V51ChecksumEngine.sha256(payload));
        }catch(Exception e){proofs.add("R932_UPSTREAM_UNAVAILABLE="+e.getClass().getSimpleName()+":"+e.getMessage());}
    }
    private static JSONArray vector(double[] values,int size)throws Exception{
        if(values==null||values.length!=size)throw new IllegalArgumentException("R932_UPSTREAM_DIMENSION");
        JSONArray out=new JSONArray();for(double v:values){if(!Double.isFinite(v))throw new IllegalArgumentException("R932_UPSTREAM_NONFINITE");out.put(v);}return out;
    }
    public static boolean ready(List<String> proofs){
        if(proofs==null)return false;String payload=null,hash=null;
        for(String s:proofs){if(s.startsWith(PREFIX)){if(payload!=null)return false;payload=s.substring(PREFIX.length());}
            if(s.startsWith(HASH_PREFIX)){if(hash!=null)return false;hash=s.substring(HASH_PREFIX.length());}}
        if(payload==null||hash==null||!hash.equals(V51ChecksumEngine.sha256(payload)))return false;
        try{JSONObject root=new JSONObject(payload);return REVISION.equals(root.getString("revision"))&&root.getBoolean("ready");}
        catch(Exception e){return false;}
    }
}
