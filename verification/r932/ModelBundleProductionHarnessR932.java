package com.ethk3.novaonepro;

import java.io.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;
import android.database.sqlite.SQLiteDatabase;

/** Synthetic cryptographic fixtures exercise production code; these are never model evidence. */
public final class ModelBundleProductionHarnessR932 {
    interface Work{void run()throws Exception;}
    static int assertions;
    static void check(boolean ok,String code){if(!ok)throw new AssertionError(code);assertions++;System.out.println(code+"=PASS");}
    static void reject(Work work,String code)throws Exception{try{work.run();}catch(Exception expected){check(true,code);return;}throw new AssertionError(code+" accepted");}
    static final String[] TYPES={"NUMBER","GATE","RANKER","CALIBRATION","PREDICTABILITY","POLICY"};
    static final Class<?>[] SEEDS={V51NumberModelArtifact.class,V51ExpertGateArtifact.class,V51ActionRankerArtifact.class,V51SelectedCalibrationArtifact.class,V51PredictabilityArtifact.class,V51PolicySurfaceArtifact.class};
    static final Class<?>[] PARAMS={ModelBundleParametersR932.Number.class,ModelBundleParametersR932.Gate.class,ModelBundleParametersR932.Ranker.class,ModelBundleParametersR932.Calibration.class,ModelBundleParametersR932.Predictability.class,ModelBundleParametersR932.Policy.class};
    static final class Fixture {
        final Map<String,byte[]> entries;final byte[] zip;final String hash;
        Fixture(double bias,KeyPair keys,String defect)throws Exception{
            String schema=V51ActionMetaFeatureEngine.featureManifestHash(),data=V51ChecksumEngine.sha256("SYNTHETIC_TEST_ONLY");
            String selector=new SelectionEvidenceContractR929.SelectionPolicyIdentity("fixture-ranker-"+bias,
                "V51_RUNTIME_RANK1_THEN_CERTIFIED_RESEARCH_MIXERS","CERTIFIED_RESEARCH_WEIGHT_CONTRACT","PREDRAW_STATE_DOMAIN_ONLY",
                "RANK_SCORE_DESC_RAW_P_DESC_POSITION_ASC_K3_LEXICOGRAPHIC",schema).hash;
            JSONObject cert=new JSONObject();cert.put("selection_policy_sha256",selector);cert.put("input_schema_sha256",schema);cert.put("data_sha256",data);
            for(String gate:new String[]{"host_parity_pass","android_parity_pass","sealed_evaluation_pass","resource_pass"})cert.put(gate,true);
            cert.put("code_sha256",SourceManifestR815.SOURCE_TREE_SHA256);
            cert.put("scope","SYNTHETIC_FIXTURE_NOT_REAL_MODEL_EVIDENCE");String certJson=cert.toString();
            entries=new LinkedHashMap<String,byte[]>();List<ModelBundleManagerR929.ArtifactEntry> list=new ArrayList<>();Map<String,String> hashes=new HashMap<>();
            for(int i=0;i<TYPES.length;i++){
                String type=TYPES[i];JSONObject params=new JSONObject();
                for(Field f:PARAMS[i].getDeclaredFields()){
                    if(f.getName().equals("TRAINED_MODEL")||f.getName().startsWith("SLOW_"))continue;
                    Object v=SEEDS[i].getField(f.getName()).get(null);
                    if(v.getClass().isArray())v=new JSONArray(v);params.put(f.getName(),v);
                }
                params.put("DATA_SHA256",data);
                if(type.equals("RANKER")){params.put("ARTIFACT_ID","fixture-ranker-"+bias);params.put("BIAS",bias);params.put("PROVEN",true);params.put("RANK_ORDER_PASSED",true);params.put("STABLE_FOLDS",5);}
                if(type.equals("CALIBRATION")){
                    selector=new SelectionEvidenceContractR929.SelectionPolicyIdentity(V51ChecksumEngine.sha256(hashes.get("NUMBER")+'|'+hashes.get("GATE")+'|'+hashes.get("RANKER")),
                        "V51_RUNTIME_RANK1_THEN_CERTIFIED_RESEARCH_MIXERS","CERTIFIED_RESEARCH_WEIGHT_CONTRACT","PREDRAW_STATE_DOMAIN_ONLY",
                        "RANK_SCORE_DESC_RAW_P_DESC_POSITION_ASC_K3_LEXICOGRAPHIC",schema).hash;
                    cert.put("selection_policy_sha256",selector);certJson=cert.toString();
                    params.put("INPUT_SCHEMA_SHA256",schema);params.put("SELECTION_POLICY_SHA256",selector);
                }
                if(type.equals("POLICY"))params.put("certificate_json",certJson);
                if(defect.equals("dimension")&&type.equals("NUMBER"))params.put("FEATURE_WEIGHT",new JSONArray(new double[]{1,2}));
                if(defect.equals("zero_scale")&&type.equals("RANKER"))params.getJSONArray("SCALE").put(0,0);
                JSONObject payload=new JSONObject();payload.put("revision",ModelBundleRuntimeR932.REVISION);payload.put("type",type);
                String stage=ModelBundleRuntimeR932.expectedStage(type),componentSchema=ModelBundleRuntimeR932.expectedSchema(type);
                if(defect.equals("semantic")&&type.equals("NUMBER"))componentSchema=V51ChecksumEngine.sha256("SAME_WIDTH_WRONG_SEMANTICS");
                payload.put("input_stage",stage);payload.put("schema_sha256",componentSchema);payload.put("trained_until_issue",defect.equals("future")?"999999":"100");
                payload.put("valid_until_ms",4102444800000L);payload.put("parameters",params);
                String path=type.toLowerCase(Locale.US)+".json";byte[] bytes=payload.toString().getBytes("UTF-8");entries.put(path,bytes);
                String sha=ModelBundleRuntimeR932.digest(bytes);hashes.put(type,sha);
                String parent=type.equals("CALIBRATION")?hashes.get("RANKER"):type.equals("POLICY")?hashes.get("CALIBRATION"):"";
                list.add(new ModelBundleManagerR929.ArtifactEntry(type,path,sha,bytes.length,stage,componentSchema,parent));
            }
            // Existing manifest convention also binds calibration to ranker input semantics.
            StringBuilder canonical=new StringBuilder(ModelBundleManagerR929.REVISION).append('|').append(selector).append('|').append(schema).append('|').append(V51ChecksumEngine.sha256(certJson));
            List<ModelBundleManagerR929.ArtifactEntry> sorted=new ArrayList<>(list);sorted.sort(Comparator.comparing(x->x.type));
            for(ModelBundleManagerR929.ArtifactEntry e:sorted)canonical.append('\n').append(e.canonical());
            hash=V51ChecksumEngine.sha256(canonical.toString());
            ModelBundleManagerR929.BundleManifest manifest=new ModelBundleManagerR929.BundleManifest(hash,selector,schema,V51ChecksumEngine.sha256(certJson),list);
            Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(keys.getPrivate());signer.update(hash.getBytes("UTF-8"));
            entries.put("manifest.json",ModelBundleManagerR929.toJson(manifest).getBytes("UTF-8"));entries.put("signature.bin",signer.sign());zip=zip(entries);
        }
    }
    static byte[] zip(Map<String,byte[]> entries)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(bytes)){
        for(Map.Entry<String,byte[]> e:entries.entrySet()){z.putNextEntry(new ZipEntry(e.getKey()));z.write(e.getValue());z.closeEntry();}}
        return bytes.toByteArray();}
    static void clearCache()throws Exception{Field cache=ModelBundleRuntimeR932.class.getDeclaredField("CACHE");cache.setAccessible(true);((Map<?,?>)cache.get(null)).clear();}
    public static void main(String[] args)throws Exception {
        File dir=Files.createTempDirectory("r932-model-fixture-").toFile();Field root=ModelBundleRuntimeR932.class.getDeclaredField("filesRoot");root.setAccessible(true);root.set(null,dir);
        KeyPairGenerator gen=KeyPairGenerator.getInstance("RSA");gen.initialize(2048);KeyPair key=gen.generateKeyPair(),wrong=gen.generateKeyPair();
        ModelBundleRuntimeR932.trustKey(key.getPublic().getEncoded());reject(()->ModelBundleRuntimeR932.trustKey(wrong.getPublic().getEncoded()),"TRUST_KEY_CANNOT_SILENTLY_CHANGE");
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try{
            DbStore store=new DbStore(db);Fixture a=new Fixture(7,key,""),b=new Fixture(11,key,"");
            check(ModelBundleRuntimeR932.importArchive(store,new ByteArrayInputStream(a.zip)).equals(a.hash),"SIGNED_COMPLETE_PACKAGE_STAGED");
            clearCache();
            try(ModelBundleRuntimeR932.Scope outer=ModelBundleRuntimeR932.open(store,"200",System.currentTimeMillis())){
                check(ModelBundleRuntimeR932.current().hash.equals(a.hash),"RESTART_VERIFIES_AND_LOADS_ALL_COMPONENTS");
                Method score=V51ActionRankerEngine.class.getDeclaredMethod("artifactScore",double[].class,double.class);score.setAccessible(true);
                check(((Double)score.invoke(null,new double[96],0.3d))==7d,"PRODUCTION_RANKER_READS_IMPORTED_PARAMETERS");
                ModelBundleRuntimeR932.importArchive(store,new ByteArrayInputStream(b.zip));
                check(ModelBundleRuntimeR932.ranker().BIAS==7d,"IMPORT_DOES_NOT_MUTATE_INFLIGHT_REQUEST");
                try(ModelBundleRuntimeR932.Scope retry=ModelBundleRuntimeR932.open(store,"200",System.currentTimeMillis())){
                    check(ModelBundleRuntimeR932.current().hash.equals(a.hash),"SAME_ISSUE_RETRY_PINNED");}
                try(ModelBundleRuntimeR932.Scope next=ModelBundleRuntimeR932.open(store,"201",System.currentTimeMillis())){
                    check(ModelBundleRuntimeR932.ranker().BIAS==11d,"NEXT_ISSUE_ATOMIC_GENERATION_SWITCH");}
                check(ModelBundleRuntimeR932.ranker().BIAS==7d,"NESTED_SCOPE_RESTORES_PRIOR_GENERATION");
            }
            check(ModelBundleRuntimeR932.ranker().BIAS==V51ActionRankerArtifact.BIAS,"REQUEST_SCOPE_CLEANUP");
            ModelBundleRuntimeR932.requestRollback(store,"FIXTURE");
            try(ModelBundleRuntimeR932.Scope restored=ModelBundleRuntimeR932.open(store,"202",System.currentTimeMillis())){
                check(ModelBundleRuntimeR932.current().hash.equals(a.hash),"ROLLBACK_NEXT_BOUNDARY_NO_CANONICAL_WRITES");
                check(store.coordinatorR36().runAtomicWrite("R939_UPGRADE_BINDING_REGRESSION",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase sql){
                    sql.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_BUNDLE+" SET code_hash=? WHERE state='ACTIVE'",new Object[]{"0".repeat(64)});
                    ResearchPlatformStoreR929.activateRuntimeBundle(sql,ModelBundleRuntimeR932.current(),"202",System.currentTimeMillis());
                }}),"SAME_BUNDLE_UPGRADE_REFRESH_COMMITS");
                try(android.database.Cursor c=db.rawQuery("SELECT code_hash FROM "+ResearchPlatformStoreR929.TABLE_BUNDLE+" WHERE state='ACTIVE'",null)){
                    check(c.moveToFirst()&&SourceManifestR815.SOURCE_TREE_SHA256.equals(c.getString(0)),"SAME_BUNDLE_UPGRADE_REBINDS_CURRENT_CODE");
                }
            }
            reject(()->ModelBundleRuntimeR932.importArchive(store,new ByteArrayInputStream(new Fixture(9,wrong,"").zip)),"WRONG_SIGNATURE_REJECTED");
            for(String fault:new String[]{"dimension","semantic","zero_scale"})reject(()->ModelBundleRuntimeR932.importArchive(store,new ByteArrayInputStream(new Fixture(9,key,fault).zip)),"SIGNED_"+fault.toUpperCase(Locale.US)+"_REJECTED");
            Map<String,byte[]> bad=new LinkedHashMap<>(a.entries);bad.remove("number.json");reject(()->ModelBundleRuntimeR932.importArchive(store,new ByteArrayInputStream(zip(bad))),"MISSING_COMPONENT_REJECTED");
            bad.put("../escape.json",new byte[]{1});reject(()->ModelBundleRuntimeR932.importArchive(store,new ByteArrayInputStream(zip(bad))),"PATH_TRAVERSAL_REJECTED");
            reject(()->ModelBundleRuntimeR932.importArchive(store,new ByteArrayInputStream(Arrays.copyOf(a.zip,a.zip.length/2))),"INTERRUPTED_IMPORT_REJECTED");
            reject(()->ModelBundleRuntimeR932.importArchive(store,new ByteArrayInputStream(Arrays.copyOf(a.zip,a.zip.length-12))),"MISSING_CENTRAL_DIRECTORY_REJECTED");
            check(store.getState(ModelBundleManagerR929.STATE_ACTIVE,"").equals(a.hash),"FAILED_IMPORT_PRESERVES_ACTIVE_POINTER");
            ModelBundleRuntimeR932.importArchive(store,new ByteArrayInputStream(a.zip));check(new File(dir,"model-bundles-r929/"+a.hash+"/manifest.json").isFile(),"RESTAGING_ACTIVE_BUNDLE_DOES_NOT_DELETE_IT");
            // A validly signed but future-trained bundle must never enter this target's computation.
            Fixture future=new Fixture(9,key,"future");ModelBundleRuntimeR932.importArchive(store,new ByteArrayInputStream(future.zip));
            try(ModelBundleRuntimeR932.Scope fallback=ModelBundleRuntimeR932.open(store,"203",System.currentTimeMillis())){
                check(!ModelBundleRuntimeR932.current().hash.equals(future.hash),"FUTURE_TRAINED_BUNDLE_FALLS_BACK");}
        } finally {db.shutdown();}

        System.out.println("R932_MODEL_BUNDLE_PRODUCTION_PASS|assertions="+assertions+"|scope=SYNTHETIC_CRYPTO_AND_REAL_SQLITE_NOT_DEVICE");
    }
}
