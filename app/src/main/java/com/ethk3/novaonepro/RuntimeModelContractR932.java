package com.ethk3.novaonepro;
import java.lang.reflect.Field;
import org.json.*;

/** Export the installed APK's exact model schemas and static fallback parameter shapes. */
public final class RuntimeModelContractR932 {
    private RuntimeModelContractR932() {}
    public static String json()throws Exception {
        String[] types={"NUMBER","GATE","RANKER","CALIBRATION","PREDICTABILITY","POLICY"};
        Class<?>[] seeds={V51NumberModelArtifact.class,V51ExpertGateArtifact.class,V51ActionRankerArtifact.class,V51SelectedCalibrationArtifact.class,V51PredictabilityArtifact.class,V51PolicySurfaceArtifact.class};
        Class<?>[] params={ModelBundleParametersR932.Number.class,ModelBundleParametersR932.Gate.class,ModelBundleParametersR932.Ranker.class,ModelBundleParametersR932.Calibration.class,ModelBundleParametersR932.Predictability.class,ModelBundleParametersR932.Policy.class};
        JSONObject root=new JSONObject();root.put("revision",ModelBundleRuntimeR932.REVISION);root.put("manifest_revision",ModelBundleManagerR929.REVISION);
        root.put("application_id",ReleaseManifestR926.APPLICATION_ID);root.put("version_code",ReleaseManifestR926.VERSION_CODE);root.put("game",V51Repository.GAME_CODE);
        root.put("source_sha256",SourceManifestR815.SOURCE_TREE_SHA256);root.put("meta_schema_sha256",V51ActionMetaFeatureEngine.featureManifestHash());
        root.put("domain_schema_sha256",DomainRouterR929.installedSchemaHash());root.put("tie_break","RANK_SCORE_DESC_RAW_P_DESC_POSITION_ASC_K3_LEXICOGRAPHIC");
        JSONObject components=new JSONObject();
        for(int i=0;i<types.length;i++) {
            JSONObject component=new JSONObject(),values=new JSONObject();
            for(Field f:params[i].getDeclaredFields()) {
                if(f.getName().equals("TRAINED_MODEL")||f.getName().startsWith("SLOW_"))continue;
                Object value=seeds[i].getField(f.getName()).get(null);values.put(f.getName(),value.getClass().isArray()?new JSONArray(value):value);
            }
            component.put("parameters",values);component.put("input_stage",ModelBundleRuntimeR932.expectedStage(types[i]));component.put("schema_sha256",ModelBundleRuntimeR932.expectedSchema(types[i]));
            if(i<2){component.put("deep_input_stage",ModelBundleRuntimeR932.expectedStage(types[i],true));component.put("deep_schema_sha256",ModelBundleRuntimeR932.expectedSchema(types[i],true));}
            components.put(types[i],component);
        }
        root.put("components",components);return root.toString();
    }
}
