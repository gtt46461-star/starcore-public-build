package com.ethk3.novaonepro;

import android.content.Context;
import java.io.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.zip.*;
import org.json.JSONObject;

/** Single bundle loader. A scope pins all six components for the complete production request. */
public final class ModelBundleRuntimeR932 {
    public static final String REVISION="R932_RUNTIME_PARAMETERS_V1";
    private static volatile File filesRoot;
    private static final ThreadLocal<Snapshot> CURRENT=new ThreadLocal<Snapshot>();
    private static final ThreadLocal<Scope> REQUEST=new ThreadLocal<Scope>();
    private static final LinkedHashMap<String,Snapshot> CACHE=new LinkedHashMap<String,Snapshot>();
    private static final Snapshot SEED=new Snapshot();
    private ModelBundleRuntimeR932() {}
    static void configure(Context context){if(context!=null)filesRoot=context.getFilesDir();}

    public static final class Snapshot {
        public final String hash,trainedUntil,selectionHash;
        public final long expiresAt,payloadBytes;
        public final Map<String,Domain> domains;
        public final Map<String,String> artifactHashes;
        public final ModelBundleParametersR932.Number number;
        public final ModelBundleParametersR932.Gate gate;
        public final ModelBundleParametersR932.Ranker ranker;
        public final ModelBundleParametersR932.Calibration calibration;
        public final ModelBundleParametersR932.Predictability predictability;
        public final ModelBundleParametersR932.Policy policy;
        private Snapshot(){
            domains=Collections.emptyMap();artifactHashes=Collections.emptyMap();payloadBytes=0;
            hash=V51ArtifactBundleManifest.BUNDLE_HASH;trainedUntil="";selectionHash="";expiresAt=Long.MAX_VALUE;
            number=new ModelBundleParametersR932.Number(null);gate=new ModelBundleParametersR932.Gate(null);
            ranker=new ModelBundleParametersR932.Ranker(null);calibration=new ModelBundleParametersR932.Calibration(null);
            predictability=new ModelBundleParametersR932.Predictability(null);policy=new ModelBundleParametersR932.Policy(null);
        }
        Snapshot(ModelBundleManagerR929.BundleManifest manifest,Map<String,byte[]> bytes) throws Exception {
            hash=manifest.bundleHash;selectionHash=manifest.selectionPolicyHash;
            long measured=0;for(byte[] value:bytes.values())measured+=value.length;payloadBytes=measured;
            Map<String,JSONObject> p=new HashMap<String,JSONObject>();Map<String,String> trainingEnds=new HashMap<String,String>();String last=null;long expiry=Long.MAX_VALUE;
            for(ModelBundleManagerR929.ArtifactEntry entry:manifest.artifacts){
                JSONObject r=new JSONObject(new String(bytes.get(entry.path),"UTF-8"));
                if(!REVISION.equals(r.getString("revision"))||!entry.type.equals(r.getString("type"))
                        ||!entry.inputStage.equals(r.getString("input_stage"))||!entry.schemaHash.equals(r.getString("schema_sha256")))
                    throw new IllegalArgumentException("R932_ARTIFACT_SEMANTICS:"+entry.type);
                boolean deep=r.getJSONObject("parameters").has("TRAINED_MODEL")&&(entry.type.equals("NUMBER")||entry.type.equals("GATE"));
                String stage=expectedStage(entry.type,deep);
                if(!stage.equals(entry.inputStage)||!expectedSchema(entry.type,deep).equals(entry.schemaHash))throw new IllegalArgumentException("R932_INPUT_STAGE:"+entry.type);
                String until=r.getString("trained_until_issue");if(!until.matches("[0-9]+"))throw new IllegalArgumentException("R932_TRAINING_END");
                trainingEnds.put(entry.type,until);if(last==null||IssueOrderR932.compare(until,last)>0)last=until;
                long valid=r.getLong("valid_until_ms");if(valid<=0)throw new IllegalArgumentException("R932_EXPIRY");expiry=Math.min(expiry,valid);
                p.put(entry.type,r.getJSONObject("parameters"));
            }
            trainedUntil=last;expiresAt=expiry;
            if(IssueOrderR932.compare(trainingEnds.get("NUMBER"),trainingEnds.get("RANKER"))>0
                ||IssueOrderR932.compare(trainingEnds.get("GATE"),trainingEnds.get("RANKER"))>0
                ||IssueOrderR932.compare(trainingEnds.get("RANKER"),trainingEnds.get("CALIBRATION"))>0
                ||IssueOrderR932.compare(trainingEnds.get("PREDICTABILITY"),trainingEnds.get("CALIBRATION"))>0
                ||IssueOrderR932.compare(trainingEnds.get("CALIBRATION"),trainingEnds.get("POLICY"))>0)
                throw new IllegalArgumentException("R932_TRAINING_STAGE_ORDER");
            Map<String,String> hashes=new LinkedHashMap<String,String>();for(ModelBundleManagerR929.ArtifactEntry e:manifest.artifacts)hashes.put(e.type,e.sha256);
            artifactHashes=Collections.unmodifiableMap(hashes);
            JSONObject frozenSelection=p.get("POLICY").optJSONObject("frozen_selection");
            String consumer="V51_RUNTIME_RANK1_THEN_CERTIFIED_RESEARCH_MIXERS",weight="CERTIFIED_RESEARCH_WEIGHT_CONTRACT",regime="PREDRAW_STATE_DOMAIN_ONLY";
            if(frozenSelection!=null){
                String lane=frozenSelection.getString("consumer"),candidate=frozenSelection.getString("candidate_id");double influence=frozenSelection.getDouble("weight");
                if(!Arrays.asList("GENESIS_NEW_CERTIFIED","INVENTED_ALGORITHM_CERTIFIED","FORMULA_DSL_ACTIVE_SHADOW").contains(lane)||candidate.length()==0||!Double.isFinite(influence)||influence<=0||influence>.30)
                    throw new IllegalArgumentException("R932_FROZEN_SELECTOR");
                consumer=lane+":"+candidate;weight=Double.toHexString(influence);regime=DomainRouterR929.installedSchemaHash();
            }
            String expectedSelector=new SelectionEvidenceContractR929.SelectionPolicyIdentity(selectorModelIdentity(hashes),consumer,weight,regime,
                "RANK_SCORE_DESC_RAW_P_DESC_POSITION_ASC_K3_LEXICOGRAPHIC",manifest.inputSchemaHash).hash;
            if(!selectionHash.equals(expectedSelector))throw new IllegalArgumentException("R932_SELECTOR_NOT_BOUND_TO_RANKER_BYTES");
            number=new ModelBundleParametersR932.Number(p.get("NUMBER"));gate=new ModelBundleParametersR932.Gate(p.get("GATE"));
            ranker=new ModelBundleParametersR932.Ranker(p.get("RANKER"));calibration=new ModelBundleParametersR932.Calibration(p.get("CALIBRATION"));
            predictability=new ModelBundleParametersR932.Predictability(p.get("PREDICTABILITY"));policy=new ModelBundleParametersR932.Policy(p.get("POLICY"));
            if(!manifest.inputSchemaHash.equals(V51ActionMetaFeatureEngine.featureManifestHash())
                    ||!manifest.inputSchemaHash.equals(calibration.INPUT_SCHEMA_SHA256)
                    ||!manifest.selectionPolicyHash.equals(calibration.SELECTION_POLICY_SHA256))
                throw new IllegalArgumentException("R932_SCHEMA_OR_SELECTOR_SPLIT");
            if(predictability.TRAINED_MODEL!=null&&!manifest.selectionPolicyHash.equals(predictability.SELECTION_POLICY_SHA256))throw new IllegalArgumentException("R932_PREDICTABILITY_SELECTOR_DRIFT");
            String upstreamData=number.DATA_SHA256,data=calibration.DATA_SHA256;
            if(!upstreamData.matches("[0-9a-f]{64}")||!data.matches("[0-9a-f]{64}")||!upstreamData.equals(gate.DATA_SHA256)||!upstreamData.equals(ranker.DATA_SHA256)
                    ||!data.equals(predictability.DATA_SHA256)||!data.equals(policy.DATA_SHA256))
                throw new IllegalArgumentException("R932_DATA_GENERATION_SPLIT");
            if(!upstreamData.equals(data)) {
                JSONObject frozen=new JSONObject(p.get("POLICY").getString("certificate_json")).optJSONObject("frozen_upstream");
                if(frozen==null||!upstreamData.equals(frozen.getString("data_sha256"))||!data.equals(frozen.getString("downstream_data_sha256"))
                        ||!frozen.getString("parent_bundle_sha256").matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R932_DATA_GENERATION_SPLIT");
                JSONObject parents=frozen.getJSONObject("artifacts");
                if(parents.length()!=3)throw new IllegalArgumentException("R932_FROZEN_UPSTREAM_CARDINALITY");
                for(String type:Arrays.asList("NUMBER","GATE","RANKER"))if(!hashes.get(type).equals(parents.getString(type)))throw new IllegalArgumentException("R932_FROZEN_UPSTREAM_BYTES_CHANGED");
            }
            if(number.TEMPERATURE<=0||number.TEMPERATURE>100||calibration.CALIBRATION_HITS>calibration.CALIBRATION_SUPPORT_N
                    ||calibration.OOS_ECE<0||calibration.OOS_ECE>1||calibration.OOS_BRIER<0||calibration.OOS_BRIER>1)
                throw new IllegalArgumentException("R932_PARAMETER_RANGE");
            // A signed package carries the certificate bytes, not just an unverifiable arbitrary hash.
            String certificate=p.get("POLICY").getString("certificate_json");
            if(!manifest.certificateHash.equals(V51ChecksumEngine.sha256(certificate)))
                throw new IllegalArgumentException("R932_CERTIFICATE_HASH");
            JSONObject cert=new JSONObject(certificate);
            if(!manifest.selectionPolicyHash.equals(cert.getString("selection_policy_sha256"))
                    ||!manifest.inputSchemaHash.equals(cert.getString("input_schema_sha256"))
                    ||!data.equals(cert.getString("data_sha256"))||!SourceManifestR815.SOURCE_TREE_SHA256.equals(cert.getString("code_sha256")))throw new IllegalArgumentException("R932_CERTIFICATE_BINDING");
            if((number.PROVEN||gate.PROVEN||ranker.PROVEN||calibration.PROVEN||predictability.PROVEN||policy.MODEL_GATE_PASSED)
                    &&(!cert.getBoolean("host_parity_pass")||!cert.getBoolean("android_parity_pass")
                    ||!cert.getBoolean("sealed_evaluation_pass")||!cert.getBoolean("resource_pass")))
                throw new IllegalArgumentException("R932_PROOF_REQUIRED");
            Map<String,Domain> domainMap=new LinkedHashMap<String,Domain>();
            org.json.JSONArray rows=p.get("POLICY").optJSONArray("domains");
            if(rows!=null){if(rows.length()>128)throw new IllegalArgumentException("R932_DOMAIN_COUNT");
                for(int i=0;i<rows.length();i++){Domain d=new Domain(rows.getJSONObject(i),selectionHash,manifest.inputSchemaHash);
                    if(domainMap.put(d.evidence.domainId,d)!=null)throw new IllegalArgumentException("R932_DOMAIN_DUPLICATE");}}
            domains=Collections.unmodifiableMap(domainMap);
        }
    }
    public static final class Domain {
        public final DomainRouterR929.DomainEvidence evidence;
        public final double hitRate,wilsonLower,blockMinimum;
        Domain(JSONObject row,String selector,String featureSchema)throws Exception{
            if(!"MARKET".equals(row.getString("namespace"))||!selector.equals(row.getString("selection_policy_sha256"))
                    ||!featureSchema.equals(row.getString("feature_schema_sha256"))
                    ||!DomainRouterR929.installedSchemaHash().equals(row.getString("domain_schema_sha256")))
                throw new IllegalArgumentException("R932_DOMAIN_BINDING");
            String hash=V51ChecksumEngine.sha256(row.toString());
            evidence=new DomainRouterR929.DomainEvidence(row.getString("domain_id"),row.getString("domain_schema_sha256"),hash,
                row.getInt("calibration_n"),row.getInt("future_n"),row.getBoolean("active"),row.getDouble("ece"),row.getDouble("paired_delta"));
            hitRate=row.getDouble("hit_rate");wilsonLower=row.getDouble("wilson_lower");blockMinimum=row.getDouble("block_minimum");
            for(double x:new double[]{hitRate,wilsonLower,blockMinimum})if(!Double.isFinite(x)||x<0||x>1)throw new IllegalArgumentException("R932_DOMAIN_METRIC");
            if(wilsonLower>hitRate)throw new IllegalArgumentException("R932_DOMAIN_INTERVAL");
        }
    }
    public static String expectedStage(String type,boolean deep){return deep?"BASE_EXPERTS_PLUS_PREDRAW_STATE":expectedStage(type);}
    public static String expectedSchema(String type,boolean deep){return deep?V51ChecksumEngine.sha256(UpstreamModelMathR932.CONTRACT+"|"+type+"|"+Arrays.toString(V51ExpertGateArtifact.EXPERT_IDS)+"|confidence,change_point_risk,remaining_life,posterior|NUMBER1049_GATE68"):expectedSchema(type);}
    public static String expectedStage(String type){
        if("NUMBER".equals(type))return "RAW_HISTORY_NUMBER_FEATURES";
        if("GATE".equals(type))return "FROZEN_EXPERT_LOGITS";
        if("RANKER".equals(type))return "ACTION_META96_AFTER_NUMBER_GATE";
        if("CALIBRATION".equals(type))return "SELECTED_ACTION16";
        if("PREDICTABILITY".equals(type))return "PREDRAW_PREDICTABILITY12";
        if("POLICY".equals(type))return "CALIBRATED_SELECTED_ACTION";
        throw new IllegalArgumentException("R932_TYPE");
    }
    public static String expectedSchema(String type){
        if("NUMBER".equals(type))return V51TrajectoryFeatureEngine.schemaHash();
        if("GATE".equals(type))return V51ChecksumEngine.sha256("R932_GATE|"+Arrays.toString(V51ExpertGateArtifact.EXPERT_IDS)+"|"+Arrays.toString(V51StateModelArtifact.IDS)+"|TRUST_UNCERTAINTY_SIGNAL_HEALTH_V51");
        if("RANKER".equals(type)||"CALIBRATION".equals(type))return V51ActionMetaFeatureEngine.featureManifestHash();
        if("PREDICTABILITY".equals(type))return V51ChecksumEngine.sha256("R932_PREDICTABILITY12|stateConfidence,oneMinusChangePoint,effectiveConsensus,signalStrength,rankRetention,thirdRobustness,rankMarginDiv0.12,thirdMarginDiv0.08,relationReliability,motifReliability,teacherReliability,oneMinusFalsifier|CLAMP01_V51|RELIABILITY=EXPERT_TRUST_TIMES_ONE_MINUS_UNCERTAINTY_R932");
        if("POLICY".equals(type))return V51ChecksumEngine.sha256("R932_POLICY_CELL_V1|domainPrefix,position,lifecycle,selectedP,predictability,rankMargin,thirdMargin,consensus,uncertainty,falsifier,drift");
        throw new IllegalArgumentException("R932_SCHEMA_TYPE");
    }
    public static final class Scope implements AutoCloseable {
        private final Snapshot prior;private final Scope parent;private boolean closed;
        public final String targetIssue;public final long capturedAt;private final DbStore store;
        private Scope(Snapshot snapshot){this(snapshot,"",0);}
        private Scope(Snapshot snapshot,String target,long at){this(snapshot,target,at,null);}
        private Scope(Snapshot snapshot,String target,long at,DbStore source){store=source;prior=CURRENT.get();parent=REQUEST.get();targetIssue=target;capturedAt=at;CURRENT.set(snapshot);REQUEST.set(this);}
        public void close(){if(!closed){closed=true;if(prior==null)CURRENT.remove();else CURRENT.set(prior);if(parent==null)REQUEST.remove();else REQUEST.set(parent);}}
    }
    static Scope use(Snapshot snapshot){return new Scope(snapshot);}
    public static DbStore currentStore(){Scope s=REQUEST.get();return s==null?null:s.store;}
    public static String currentTarget(){Scope scope=REQUEST.get();return scope==null?"":scope.targetIssue;}
    public static long capturedAt(){Scope scope=REQUEST.get();return scope==null?0:scope.capturedAt;}
    public static Snapshot current(){Snapshot s=CURRENT.get();return s==null?SEED:s;}
    private static String selectorModelIdentity(Map<String,String> hashes){return V51ChecksumEngine.sha256(hashes.get("NUMBER")+'|'+hashes.get("GATE")+'|'+hashes.get("RANKER"));}
    public static String rankerIdentity(){return current().artifactHashes.isEmpty()?V51ActionRankerArtifact.ARTIFACT_ID:selectorModelIdentity(current().artifactHashes);}
    public static ModelBundleParametersR932.Number number(){return current().number;}
    public static ModelBundleParametersR932.Gate gate(){return current().gate;}
    public static ModelBundleParametersR932.Ranker ranker(){return current().ranker;}
    public static ModelBundleParametersR932.Calibration calibration(){return current().calibration;}
    public static ModelBundleParametersR932.Predictability predictability(){return current().predictability;}
    public static ModelBundleParametersR932.Policy policy(){return current().policy;}

    public static Scope forEvidence(String hash,String target,long at) {
        Snapshot s=SEED.hash.equals(hash)?SEED:load(hash);requireUsable(s,target,System.currentTimeMillis());
        return new Scope(s,target,at);
    }
    static Scope forHistoricalEvidence(String hash,String target,long frozenAt) {
        if(frozenAt<=0)throw new IllegalArgumentException("R932_HISTORICAL_CAPTURE_TIME");
        Snapshot snapshot=SEED.hash.equals(hash)?SEED:load(hash);requireUsable(snapshot,target,frozenAt);
        return new Scope(snapshot,target,frozenAt);
    }
    public static Scope open(final DbStore store,final String target,long now){
        if(store==null||filesRoot==null)return new Scope(SEED,target,now,store);
        final Map<String,String> pointersCP3 = readPointersCP3(store);
        final String fixedIssue=pointersCP3.get("r932_bundle_capture_issue");
        final String fixedHash=pointersCP3.get("r932_bundle_capture_hash");
        if(TargetIssueResolver.isValid(fixedIssue)
                && TargetIssueResolver.compare(target,fixedIssue)<0)
            throw new IllegalStateException("CP3_BUNDLE_TARGET_SUPERSEDED");
        final String oldActive=pointersCP3.get(ModelBundleManagerR929.STATE_ACTIVE);
        final String staged=pointersCP3.get(ModelBundleManagerR929.STATE_STAGED);
        final boolean reset="1".equals(pointersCP3.get("r932_bundle_reset_pending"));
        boolean same=target.equals(fixedIssue);
        String chosen=same?fixedHash:reset?"":staged.length()==64?staged:oldActive;
        Snapshot snapshot;
        try {snapshot=chosen.length()==64?load(chosen):SEED;requireUsable(snapshot,target,System.currentTimeMillis());}
        catch(RuntimeException failure){
            if(same)throw failure; // Never reroll an already fixed issue with another model.
            store.setState("r932_model_last_rejection","DISABLED|"+failure.getMessage());
            String previous=pointersCP3.get(ModelBundleManagerR929.STATE_PREVIOUS);
            try{snapshot=previous.length()==64&&!previous.equals(chosen)?load(previous):SEED;requireUsable(snapshot,target,System.currentTimeMillis());}
            catch(RuntimeException unavailable){snapshot=SEED;}
            chosen=snapshot==SEED?"":snapshot.hash;
        }
        if(!same){
            final String selected=chosen;
            final Snapshot frozenSnapshot=snapshot;
            final boolean committed;
            try {
                committed=store.coordinatorR36().runAtomicWriteOrThrowF01("R932_BUNDLE_CAPTURE",new DatabaseCoordinator.WriteCallable<Boolean>(){
                public Boolean call(android.database.sqlite.SQLiteDatabase db){
                    if(!fixedIssue.equals(state(db,"r932_bundle_capture_issue"))
                            || !fixedHash.equals(state(db,"r932_bundle_capture_hash"))
                            || reset != "1".equals(state(db,"r932_bundle_reset_pending"))
                            || !oldActive.equals(state(db,ModelBundleManagerR929.STATE_ACTIVE))
                            || !staged.equals(state(db,ModelBundleManagerR929.STATE_STAGED)))
                        throw new IllegalStateException("R932_BUNDLE_POINTER_CAS_LOST");
                    Map<String,String> values=new LinkedHashMap<String,String>();
                    if(!oldActive.equals(selected))values.put(ModelBundleManagerR929.STATE_PREVIOUS,oldActive);
                    values.put(ModelBundleManagerR929.STATE_ACTIVE,selected);values.put(ModelBundleManagerR929.STATE_STAGED,"");
                    values.put(ModelBundleManagerR929.STATE_ACTIVATED_ISSUE,target);
                    values.put("r932_bundle_reset_pending","0");values.put("r932_bundle_capture_issue",target);values.put("r932_bundle_capture_hash",selected);
                    values.put(ModelBundleManagerR929.STATE_STATUS,selected.length()==64?"ACTIVE_AT_ISSUE_BOUNDARY":"SEED_BUNDLE_ACTIVE");
                    ResearchPlatformStoreR929.activateRuntimeBundle(db,frozenSnapshot,target,now);
                    DbStore.putStatesR36(db,values);
                    return Boolean.TRUE;
                }});
            } catch (RuntimeException failure) {
                // Keep lock deadline, CAS and integrity causes available to retry/diagnosis.
                throw new IllegalStateException("R932_BUNDLE_CAPTURE_COMMIT_FAILED", failure);
            }
            if(!committed)throw new IllegalStateException("R932_BUNDLE_CAPTURE_COMMIT_FAILED");
        }
        return new Scope(snapshot,target,now,store);
    }
    private static Map<String,String> readPointersCP3(DbStore store) {
        String[] keys = {"r932_bundle_capture_issue", "r932_bundle_capture_hash",
                ModelBundleManagerR929.STATE_ACTIVE, ModelBundleManagerR929.STATE_STAGED,
                "r932_bundle_reset_pending", ModelBundleManagerR929.STATE_PREVIOUS};
        Map<String,String> values = new LinkedHashMap<String,String>();
        for (String key : keys) values.put(key, "");
        try (android.database.Cursor cursor = store.readable().rawQuery(
                "SELECT state_key,state_value FROM " + DbHelper.TABLE_APP_STATE
                + " WHERE state_key IN (?,?,?,?,?,?)", keys)) {
            while (cursor.moveToNext())
                values.put(cursor.getString(0), cursor.isNull(1) ? "" : cursor.getString(1));
        }
        return Collections.unmodifiableMap(values);
    }
    private static String state(android.database.sqlite.SQLiteDatabase db,String key){
        try(android.database.Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=?",new String[]{key})){
            return c.moveToFirst()&&!c.isNull(0)?c.getString(0):"";
        }
    }
    public static void requestRollback(final DbStore store,final String reason){
        if(store==null)throw new IllegalArgumentException("CP3_ROLLBACK_STORE_REQUIRED");
        final Map<String,String> beforeCP3=readPointersCP3(store);
        final String previous=beforeCP3.get(ModelBundleManagerR929.STATE_PREVIOUS);
        if(previous.length()==64)load(previous);
        boolean committed=store.coordinatorR36().runAtomicWrite("CP3_BUNDLE_ROLLBACK_REQUEST",
                new DatabaseCoordinator.WriteRunnable(){public void run(android.database.sqlite.SQLiteDatabase db){
                    for(Map.Entry<String,String> entry:beforeCP3.entrySet())
                        if(!entry.getValue().equals(state(db,entry.getKey())))
                            throw new IllegalStateException("CP3_BUNDLE_ROLLBACK_CAS_LOST");
                    Map<String,String> values=new LinkedHashMap<String,String>();
                    values.put(ModelBundleManagerR929.STATE_STAGED,previous);
                    values.put("r932_bundle_reset_pending",previous.length()==64?"0":"1");
                    values.put(ModelBundleManagerR929.STATE_STATUS,"ROLLBACK_PENDING|"+reason);
                    DbStore.putStatesR36(db,values);
                }});
        if(!committed)throw new IllegalStateException("CP3_BUNDLE_ROLLBACK_COMMIT_FAILED");
    }
    public static String status(DbStore store){
        if(store==null)return "数据库尚未就绪";
        return "状态="+store.getState(ModelBundleManagerR929.STATE_STATUS,"使用内置完整模型组")
            +"\n当前="+store.getState(ModelBundleManagerR929.STATE_ACTIVE,"内置")
            +"\n待激活="+store.getState(ModelBundleManagerR929.STATE_STAGED,"无")
            +"\n固定期="+store.getState("r932_bundle_capture_issue","尚无")
            +"\n最近拒绝="+store.getState("r932_model_last_rejection","无");
    }
    private static void requireUsable(Snapshot s,String target,long now){
        if(s==SEED)return;
        if(now>=s.expiresAt)throw new IllegalStateException("R932_BUNDLE_EXPIRED");
        if(IssueOrderR932.compare(s.trainedUntil,target)>=0)throw new IllegalStateException("R932_BUNDLE_FUTURE_TRAINED");
    }
    private static File root(){if(filesRoot==null)throw new IllegalStateException("R932_FILES_UNAVAILABLE");return filesRoot;}
    static PublicKey trustedKey() throws Exception {
        File key=new File(root(),"trusted-model-key-r932.der");
        try(InputStream in=new FileInputStream(key)){return decodeKey(ModelBundleManagerR929.readBounded(in,8192));}
    }
    static PublicKey decodeKey(byte[] encoded) throws Exception {
        PublicKey key=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
        if(((java.security.interfaces.RSAPublicKey)key).getModulus().bitLength()<2048)throw new IllegalArgumentException("R932_RSA_KEY_TOO_SMALL");return key;
    }
    public static synchronized String trustKey(byte[] encoded) throws Exception {
        decodeKey(encoded);File key=new File(root(),"trusted-model-key-r932.der");
        if(key.exists())try(InputStream in=new FileInputStream(key)){
            if(!Arrays.equals(encoded,ModelBundleManagerR929.readBounded(in,8192)))throw new IllegalStateException("R932_TRUST_KEY_ALREADY_PINNED");
            return digest(encoded);
        }
        File tmp=new File(root(),"trusted-model-key-r932.tmp");
        try(FileOutputStream out=new FileOutputStream(tmp)){out.write(encoded);out.getFD().sync();}
        if(!tmp.renameTo(key))throw new IOException("R932_TRUST_KEY_RENAME");return digest(encoded);
    }
    public static String importArchive(DbStore store,InputStream source) throws Exception {
        File spool=File.createTempFile("model-import-", ".zip",root());
        try {
            long count=0;byte[] block=new byte[8192];int n;
            try(FileOutputStream out=new FileOutputStream(spool)){
                while((n=source.read(block))!=-1){count+=n;if(count>ModelBundleManagerR929.MAX_BUNDLE_BYTES+1024*1024)
                    throw new IOException("R932_ARCHIVE_COMPRESSED_LIMIT");out.write(block,0,n);}
            }
            // ZipInputStream checks entry CRCs but accepts a missing central directory. Require both.
            try(ZipFile central=new ZipFile(spool)){
                if(central.size()!=8)throw new IllegalArgumentException("R932_ARCHIVE_ENTRY_COUNT");
            }
            try(InputStream in=new FileInputStream(spool)){return importVerifiedStream(store,in);}
        }finally{if(!spool.delete()&&spool.exists())spool.deleteOnExit();}
    }
    private static String importVerifiedStream(DbStore store,InputStream source) throws Exception {
        Map<String,byte[]> entries=new LinkedHashMap<String,byte[]>();long total=0;
        try(ZipInputStream zip=new ZipInputStream(source)){
            ZipEntry e;while((e=zip.getNextEntry())!=null){String name=e.getName();
                if(e.isDirectory())continue;
                if(entries.size()>=8||entries.containsKey(name)||name.startsWith("/")||name.contains("\\")||name.contains(":"))
                    throw new IllegalArgumentException("R932_ARCHIVE_ENTRY");
                for(String part:name.split("/",-1))if(part.length()==0||part.equals(".")||part.equals(".."))throw new IllegalArgumentException("R932_ARCHIVE_PATH");
                long limit=name.equals("manifest.json")?65536:name.equals("signature.bin")?8192:ModelBundleManagerR929.MAX_ARTIFACT_BYTES;
                byte[] bytes=ModelBundleManagerR929.readBounded(zip,Math.min(limit,ModelBundleManagerR929.MAX_BUNDLE_BYTES+73728-total));
                total+=bytes.length;entries.put(name,bytes);
            }
        }
        byte[] manifestBytes=entries.remove("manifest.json"),signature=entries.remove("signature.bin");
        if(manifestBytes==null||signature==null)throw new IllegalArgumentException("R932_ARCHIVE_METADATA_MISSING");
        ModelBundleManagerR929.BundleManifest manifest=ModelBundleManagerR929.parse(new String(manifestBytes,"UTF-8"));
        PublicKey key=trustedKey();ModelBundleManagerR929.verifyBytes(manifest,entries);ModelBundleManagerR929.verifySignature(manifest,signature,key);
        Snapshot snapshot=new Snapshot(manifest,entries);
        if(snapshot.expiresAt<=System.currentTimeMillis())throw new IllegalArgumentException("R932_EXPIRED_IMPORT");
        ModelBundleManagerR929.stageToDisk(root(),manifest,entries,signature,key);
        cache(snapshot);
        final String verifiedHashCP3=manifest.bundleHash;
        if(!store.coordinatorR36().runAtomicWrite("CP3_BUNDLE_STAGE_VERIFIED",
                new DatabaseCoordinator.WriteRunnable(){public void run(android.database.sqlite.SQLiteDatabase db){
                    Map<String,String> values=new LinkedHashMap<String,String>();
                    values.put(ModelBundleManagerR929.STATE_STAGED,verifiedHashCP3);
                    values.put(ModelBundleManagerR929.STATE_STATUS,"STAGED_VERIFIED");
                    values.put("r932_bundle_reset_pending","0");
                    DbStore.putStatesR36(db,values);
                }}))throw new IllegalStateException("CP3_BUNDLE_STAGE_COMMIT_FAILED");
        return manifest.bundleHash;
    }
    private static Snapshot load(String hash){
        if(!hash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R932_BUNDLE_HASH");
        Snapshot cached; synchronized(CACHE){cached=CACHE.get(hash);}if(cached!=null)return cached;
        try {
            File dir=new File(root(),"model-bundles-r929/"+hash);
            byte[] manifestBytes=read(new File(dir,"manifest.json"),65536),signature=read(new File(dir,"signature.bin"),8192);
            ModelBundleManagerR929.BundleManifest m=ModelBundleManagerR929.parse(new String(manifestBytes,"UTF-8"));
            if(!hash.equals(m.bundleHash))throw new IllegalArgumentException("R932_DIRECTORY_HASH");
            ModelBundleManagerR929.verifyDisk(dir,m,signature,trustedKey());Map<String,byte[]> files=new LinkedHashMap<String,byte[]>();
            for(ModelBundleManagerR929.ArtifactEntry e:m.artifacts)files.put(e.path,read(new File(dir,e.path),e.size));
            Snapshot snapshot=new Snapshot(m,files);cache(snapshot);return snapshot;
        }catch(Exception e){throw new IllegalStateException("R932_BUNDLE_LOAD:"+hash,e);}
    }
    private static byte[] read(File file,long limit)throws Exception{try(InputStream in=new FileInputStream(file)){return ModelBundleManagerR929.readBounded(in,limit);}}
    private static void cache(Snapshot s){synchronized(CACHE){CACHE.put(s.hash,s);while(CACHE.size()>2)CACHE.remove(CACHE.keySet().iterator().next());}}
    public static String digest(byte[] bytes)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder();for(byte b:h)out.append(String.format(Locale.US,"%02x",b&255));return out.toString();}
    private static final class IssueOrderR932 {static int compare(String a,String b){if(!a.matches("[0-9]+")||!b.matches("[0-9]+"))throw new IllegalArgumentException("R932_NUMERIC_ISSUE");return new java.math.BigInteger(a).compareTo(new java.math.BigInteger(b));}}
}
