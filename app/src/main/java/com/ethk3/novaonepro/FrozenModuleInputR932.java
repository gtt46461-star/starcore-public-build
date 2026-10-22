package com.ethk3.novaonepro;
import org.json.*;
import java.util.*;

/** Exact predraw state needed by counterfactual replay. Missing historical input is not imputed. */
public final class FrozenModuleInputR932 {
    static final String REVISION="R932_FROZEN_MODULE_INPUT_V1";
    private FrozenModuleInputR932(){}
    static JSONObject capture(V51RuntimeResult result)throws Exception {
        if(result.consensus==null||result.actions==null||result.actions.length!=1200||result.contributionAuditR922==null)throw new IllegalArgumentException("FROZEN_MODULE_MAINLINE_CONTEXT_UNAVAILABLE");
        JSONObject root=new JSONObject();root.put("revision",REVISION);root.put("source_sha256",SourceManifestR815.SOURCE_TREE_SHA256);
        root.put("global_features",new JSONArray(result.features.globalFeatures));root.put("feature_checksum",result.features.checksum);
        root.put("history_rows",result.context.historyRows);root.put("cohort_id",result.context.cohortId);
        JSONArray states=new JSONArray(),quality=new JSONArray(),health=new JSONArray(),reliability=new JSONArray();
        for(int p=0;p<10;p++){
            V51Models.StateTrajectorySnapshot s=result.states[p];JSONObject metadata=new JSONObject();metadata.put("prototype",s.prototypeId);metadata.put("domain",s.domainId);metadata.put("lifecycle",s.lifecycle.name());metadata.put("run_length",s.runLength);metadata.put("checksum",s.checksum);states.put(metadata);
            reliability.put(result.consensus[p].historicalReliability);
        }
        for(int i=0;i<9;i++) {
            V51Models.ExpertEvidence e=result.numberModel.experts[i];JSONObject q=new JSONObject();q.put("trust",new JSONArray(e.trust));q.put("uncertainty",new JSONArray(e.uncertainty));q.put("signal",new JSONArray(e.signalStrength));q.put("checksum",e.checksum);quality.put(q);
            V51ExpertHealthRegistry.Snapshot h=result.numberModel.gate.health[i];JSONObject value=new JSONObject();value.put("state",h.state.name());value.put("sample",h.sample);value.put("consecutive_bad",h.consecutiveBad);value.put("recovery_good",h.recoveryGood);value.put("reliability",h.reliability);value.put("recent_reliability",h.recentReliability);value.put("ece",h.calibrationError);value.put("rank_regret",h.rankRegret);value.put("weight",h.weightMultiplier);value.put("reason",h.reason);value.put("updated_at",h.updatedAt);health.put(value);
        }
        root.put("state_metadata",states);root.put("expert_quality",quality);root.put("gate_health",health);root.put("consensus_reliability",reliability);
        root.put("number_q",new JSONArray(result.numberModel.matrix.probability));root.put("gate_weights",new JSONArray(result.numberModel.gateWeight));
        root.put("consumer_applied",result.contributionAuditR922.applied);root.put("consumer_lane",result.contributionAuditR922.lane);root.put("consumer_id",result.contributionAuditR922.candidateId);root.put("consumer_weight",result.contributionAuditR922.influenceWeight);
        if(result.contributionAuditR922.applied){if(result.frozenConsumerRecipe.length()==0)throw new IllegalArgumentException("FROZEN_CONSUMER_RECIPE_MISSING");root.put("consumer_recipe",result.frozenConsumerRecipe);}
        double[] score=new double[1200];for(V51Models.PositionAction action:result.actions)score[K3ActionSpaceEngine.globalOrdinal(action.position,action.n1,action.n2,action.n3)]=action.rankScore;
        V51Models.PositionAction champion=null;for(V51Models.PositionAction action:result.actions)if(action.rank==1){if(champion!=null)throw new IllegalArgumentException("DUPLICATE_RANK_ONE");champion=action;}
        if(champion==null)throw new IllegalArgumentException("MISSING_RANK_ONE");
        root.put("final_rank_scores",new JSONArray(score));root.put("selected_ordinal",K3ActionSpaceEngine.globalOrdinal(champion.position,champion.n1,champion.n2,champion.n3));
        ModelBundleParametersR932.Number model=ModelBundleRuntimeR932.number();
        if(model.TRAINED_MODEL!=null&&"D2".equals(model.TRAINED_MODEL.kind)) {
            double[] input=UpstreamModelMathR932.features(UpstreamModelMathR932.stateValues(result.states),UpstreamModelMathR932.expertValues(result.numberModel.experts));
            root.put("past_slow_memory",new JSONArray(SlowStateR932.frozenPast(ModelBundleRuntimeR932.currentStore(),ModelBundleRuntimeR932.current().artifactHashes.get("NUMBER"),result.context.targetIssue,input)));
        }
        return root;
    }
    static final class Input {
        final JSONObject raw,replay;
        final V51Models.PermutationFeatureSnapshot features;
        final V51Models.StateTrajectorySnapshot[] states;
        final V51Models.ExpertEvidence[] experts;
        final V51ExpertHealthRegistry.Snapshot[] health;
        final double[] reliability,pastSlow,finalScores;
        final double[][] q,gate;
        final String bundle,target;
        final boolean consumerApplied;
        final FrozenConsumerRecipeR932.Recipe consumer;
        final int selectedOrdinal;
        Input(JSONObject raw)throws Exception {
            this.raw=raw;replay=raw.getJSONObject("module_replay");target=raw.getString("target_issue");bundle=raw.getString("model_bundle_hash");
            requireSame("REVISION",REVISION,replay.getString("revision"));
            requireSame("SOURCE",SourceManifestR815.SOURCE_TREE_SHA256,replay.getString("source_sha256"));
            requireSame("NUMBER_SCHEMA",V51TrajectoryFeatureEngine.schemaHash(),raw.getString("number_schema_sha256"));
            requireSame("META_SCHEMA",V51ActionMetaFeatureEngine.featureManifestHash(),raw.getString("meta_schema_sha256"));
            requireSame("DOMAIN_SCHEMA",DomainRouterR929.installedSchemaHash(),raw.getString("domain_schema_sha256"));
            if(!TargetIssueResolver.exact(raw.getString("base_issue"),target).valid)
                throw new IllegalArgumentException("R932_MODULE_REPLAY_PIT_TARGET");
            V51Models.PredictionContext context=new V51Models.PredictionContext(raw.getString("base_issue"),target,raw.getString("base_issue"),raw.getString("history_hash"),replay.getString("cohort_id"),bundle,raw.getLong("created_at"),replay.getInt("history_rows"));
            double[][][] number=new double[10][][];for(int p=0;p<10;p++)number[p]=TrainedModelMathR932.matrix(raw.getJSONArray("number_features").getJSONArray(p),10,18);
            features=new V51Models.PermutationFeatureSnapshot(context,TrainedModelMathR932.vector(replay.getJSONArray("global_features"),8,false),number,replay.getString("feature_checksum"));
            String[] stateFields={"confidence","change_point_risk","remaining_life","posterior"};
            JSONArray fields=raw.getJSONArray("state_fields");if(fields.length()!=4)throw new IllegalArgumentException("R932_MODULE_STATE_FIELDS");
            for(int i=0;i<4;i++)if(!fields.getString(i).equals(stateFields[i]))throw new IllegalArgumentException("R932_MODULE_STATE_ORDER");
            double[][] state=TrainedModelMathR932.matrix(raw.getJSONArray("states"),10,4);states=new V51Models.StateTrajectorySnapshot[10];
            for(int p=0;p<10;p++){JSONObject meta=replay.getJSONArray("state_metadata").getJSONObject(p);states[p]=new V51Models.StateTrajectorySnapshot(target,p+1,meta.getString("prototype"),meta.getString("domain"),V51Enums.Lifecycle.valueOf(meta.getString("lifecycle")),meta.getInt("run_length"),state[p][3],state[p][2],state[p][1],state[p][0],meta.getString("checksum"));}
            experts=new V51Models.ExpertEvidence[9];health=new V51ExpertHealthRegistry.Snapshot[9];
            for(int e=0;e<9;e++) {
                String id=raw.getJSONArray("expert_ids").getString(e);if(!id.equals(V51ExpertGateArtifact.EXPERT_IDS[e]))throw new IllegalArgumentException("R932_MODULE_EXPERT_ORDER");
                JSONObject quality=replay.getJSONArray("expert_quality").getJSONObject(e),h=replay.getJSONArray("gate_health").getJSONObject(e);
                experts[e]=new V51Models.ExpertEvidence(id,raw.getJSONArray("expert_versions").getString(e),TrainedModelMathR932.matrix(raw.getJSONArray("expert_logits").getJSONArray(e),10,10),TrainedModelMathR932.vector(quality.getJSONArray("trust"),10,false),TrainedModelMathR932.vector(quality.getJSONArray("uncertainty"),10,false),TrainedModelMathR932.vector(quality.getJSONArray("signal"),10,false),new String[0],new String[0],quality.getString("checksum"));
                health[e]=new V51ExpertHealthRegistry.Snapshot(id,V51ExpertHealthRegistry.HealthState.valueOf(h.getString("state")),h.getInt("sample"),h.getInt("consecutive_bad"),h.getInt("recovery_good"),h.getDouble("reliability"),h.getDouble("recent_reliability"),h.getDouble("ece"),h.getDouble("rank_regret"),h.getDouble("weight"),h.getString("reason"),h.getLong("updated_at"));
            }
            reliability=TrainedModelMathR932.vector(replay.getJSONArray("consensus_reliability"),10,false);pastSlow=replay.has("past_slow_memory")?TrainedModelMathR932.vector(replay.getJSONArray("past_slow_memory"),1049,false):null;
            q=TrainedModelMathR932.matrix(replay.getJSONArray("number_q"),10,10);gate=TrainedModelMathR932.matrix(replay.getJSONArray("gate_weights"),10,9);finalScores=TrainedModelMathR932.vector(replay.getJSONArray("final_rank_scores"),1200,false);consumerApplied=replay.getBoolean("consumer_applied");selectedOrdinal=replay.getInt("selected_ordinal");
            consumer=consumerApplied?FrozenConsumerRecipeR932.decode(replay.getString("consumer_recipe"),target,replay.getString("consumer_lane"),replay.getString("consumer_id"),replay.getDouble("consumer_weight")):null;
        }
    }
    private static void requireSame(String field,String expected,String actual) {
        if(!expected.equals(actual))throw new IllegalArgumentException("R932_MODULE_REPLAY_"+field+"|expected="+expected+"|actual="+actual);
    }
    static Input decode(List<String> proofs)throws Exception {
        if(!RawTrainingInputR932.ready(proofs))throw new IllegalArgumentException("R932_MODULE_RAW_HASH");
        for(String proof:proofs)if(proof.startsWith(RawTrainingInputR932.PREFIX))return new Input(new JSONObject(proof.substring(RawTrainingInputR932.PREFIX.length())));
        throw new IllegalArgumentException("R932_MODULE_RAW_MISSING");
    }
}
