package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;

/** D2 predraw-only EWMA. Capture t-1 memory and advance cursor exactly once with no label input. */
public final class SlowStateR932 {
    public static final String HISTORY="research_slow_history_r932";
    private SlowStateR932() {}
    /** Existing research worker replays only immutable past input tensors, never settled labels. */
    public static int replayPastInputs(DbStore store) {
        String bundle=store.getState(ModelBundleManagerR929.STATE_ACTIVE,"");
        String target=store.getState("r932_bundle_capture_issue","");
        if(bundle.length()!=64||!TargetIssueResolver.isValid(target))return 0;
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(bundle,target,System.currentTimeMillis())){
            ModelBundleParametersR932.Number model=ModelBundleRuntimeR932.number();
            if(model.TRAINED_MODEL==null||!model.TRAINED_MODEL.kind.equals("D2"))return 0;
            String hash=ModelBundleRuntimeR932.current().artifactHashes.get("NUMBER"),last=model.SLOW_CHECKPOINT_ISSUE;
            boolean ok=store.coordinatorR36().runAtomicWrite("R932_D2_REPLAY_SCHEMA",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){schema(db);}});
            if(!ok)throw new IllegalStateException("R932_D2_REPLAY_SCHEMA_FAILED");
            try(Cursor c=store.readable().rawQuery("SELECT last_issue FROM research_slow_state_r932 WHERE model_hash=?",new String[]{hash})){if(c.moveToFirst())last=c.getString(0);}
            int advanced=0;
            while(advanced<8){
                ResearchWorkBudgetR927.checkpoint();String next=TargetIssueResolver.next(last).targetIssue;
                if(next.compareTo(target)>=0)break;
                byte[] blob=null;String digest="";
                try(Cursor c=BudgetedQueryR927.query(store.readable(),"SELECT snapshot_blob,snapshot_hash FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" WHERE target_issue=? AND snapshot_blob IS NOT NULL LIMIT 1",new String[]{next})){
                    if(c.moveToFirst()){blob=c.getBlob(0);digest=c.getString(1);}
                }
                if(blob==null){store.setState("r932_d2_replay_status","WAIT_FROZEN_RAW_INPUT:"+next);return advanced;}
                if(!digest.equals(DeferredEvidenceSnapshotR36.sha256(blob)))throw new IllegalStateException("R932_D2_REPLAY_SNAPSHOT_HASH");
                DeferredEvidenceSnapshotR36.Snapshot snapshot=DeferredEvidenceSnapshotR36.decode(blob);
                double[] input=replayInput(snapshot.proofs,next,snapshot.baseIssue);
                capture(store,hash,next,input,model.SLOW_INITIAL,model.SLOW_CHECKPOINT_ISSUE);last=next;advanced++;
            }
            store.setState("r932_d2_replay_status","PAST_INPUT_ONLY_CURSOR:"+last+"|advanced="+advanced);return advanced;
        }catch(ResearchWorkBudgetR927.Yield yielded){throw yielded;}
        catch(Exception error){store.setState("r932_d2_replay_status","WAIT_INPUT_CONTRACT:"+error.getMessage());return 0;}
    }
    static double[] replayInput(java.util.List<String> proofs,String target,String base)throws Exception {
        if(!RawTrainingInputR932.ready(proofs)||!TargetIssueResolver.exact(base,target).valid)throw new IllegalArgumentException("R932_D2_REPLAY_RAW_PIT");
        String payload=null;for(String proof:proofs)if(proof.startsWith(RawTrainingInputR932.PREFIX))payload=proof.substring(RawTrainingInputR932.PREFIX.length());
        JSONObject raw=new JSONObject(payload);
        if(!target.equals(raw.getString("target_issue"))||!base.equals(raw.getString("base_issue"))||!V51ActionMetaFeatureEngine.featureManifestHash().equals(raw.getString("meta_schema_sha256"))||!DomainRouterR929.installedSchemaHash().equals(raw.getString("domain_schema_sha256")))throw new IllegalArgumentException("R932_D2_REPLAY_SCHEMA_DRIFT");
        JSONArray fields=raw.getJSONArray("state_fields");String[] names={"confidence","change_point_risk","remaining_life","posterior"};
        if(fields.length()!=4)throw new IllegalArgumentException("R932_D2_STATE_ORDER");
        for(int i=0;i<4;i++)if(!names[i].equals(fields.getString(i)))throw new IllegalArgumentException("R932_D2_STATE_ORDER");
        JSONArray ids=raw.getJSONArray("expert_ids");String[] expected=V51ExpertGateArtifact.EXPERT_IDS;
        if(ids.length()!=expected.length)throw new IllegalArgumentException("R932_D2_EXPERT_ORDER");
        double[][][] experts=new double[9][][];
        for(int i=0;i<9;i++){if(!expected[i].equals(ids.getString(i)))throw new IllegalArgumentException("R932_D2_EXPERT_ORDER");experts[i]=TrainedModelMathR932.matrix(raw.getJSONArray("expert_logits").getJSONArray(i),10,10);}
        return UpstreamModelMathR932.features(TrainedModelMathR932.matrix(raw.getJSONArray("states"),10,4),experts);
    }
    private static void schema(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS research_slow_state_r932(model_hash TEXT PRIMARY KEY,last_issue TEXT NOT NULL,input_hash TEXT NOT NULL,past_memory TEXT NOT NULL,next_memory TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+HISTORY+"(model_hash TEXT NOT NULL,target_issue TEXT NOT NULL,input_hash TEXT NOT NULL,past_memory TEXT NOT NULL,next_memory TEXT NOT NULL,record_hash TEXT NOT NULL,PRIMARY KEY(model_hash,target_issue))");
    }
    /** Read-only exact past state for same-input replay; no rewind of the canonical D2 cursor. */
    static double[] frozenPast(DbStore store,String model,String target,double[] input) {
        String inputHash=V51ChecksumEngine.sha256(encode(input));
        try(Cursor c=store.readable().rawQuery("SELECT input_hash,past_memory,next_memory,record_hash FROM "+HISTORY+" WHERE model_hash=? AND target_issue=?",new String[]{model,target})) {
            if(!c.moveToFirst())throw new IllegalStateException("R932_D2_FROZEN_PAST_UNAVAILABLE");
            String record=V51ChecksumEngine.sha256(model+'|'+target+'|'+inputHash+'|'+c.getString(1)+'|'+c.getString(2));
            if(!inputHash.equals(c.getString(0))||!record.equals(c.getString(3)))throw new IllegalStateException("R932_D2_FROZEN_PAST_DRIFT");
            return decode(c.getString(1),input.length);
        }
    }
    public static double[] capture(final DbStore store,final String modelHash,final String target,
            final double[] input,final double[] initial,final String checkpointIssue) {
        if(store==null||input==null||input.length!=UpstreamModelMathR932.NUMBER_INPUT||initial==null||initial.length!=input.length)
            throw new IllegalArgumentException("R932_D2_STATE_INPUT");
        final String inputHash=V51ChecksumEngine.sha256(encode(input));
        double[] result=store.coordinatorR36().runAtomicWrite("R932_D2_CAPTURE_ADVANCE",new DatabaseCoordinator.WriteCallable<double[]>(){public double[] call(SQLiteDatabase db){
            schema(db);
            String last=checkpointIssue;double[] past=initial.clone();
            try(Cursor c=db.rawQuery("SELECT last_issue,input_hash,past_memory,next_memory FROM research_slow_state_r932 WHERE model_hash=?",new String[]{modelHash})) {
                if(c.moveToFirst()) {
                    last=c.getString(0);
                    if(target.equals(last)){if(!inputHash.equals(c.getString(1)))throw new IllegalStateException("R932_D2_SAME_ISSUE_INPUT_DRIFT");return decode(c.getString(2),input.length);}
                    past=decode(c.getString(3),input.length);
                }
            }
            if(last==null||!TargetIssueResolver.isValid(last)||!TargetIssueResolver.next(last).targetIssue.equals(target))
                throw new IllegalStateException("R932_D2_GAP_REPLAY_REQUIRED");
            double[] next=new double[input.length];for(int i=0;i<next.length;i++)next[i]=.95*past[i]+.05*TrainedModelMathR932.finite(input[i]);
            String previous=encode(past),advanced=encode(next),record=V51ChecksumEngine.sha256(modelHash+'|'+target+'|'+inputHash+'|'+previous+'|'+advanced);
            db.execSQL("INSERT INTO "+HISTORY+" VALUES(?,?,?,?,?,?)",new Object[]{modelHash,target,inputHash,previous,advanced,record});
            db.execSQL("INSERT OR REPLACE INTO research_slow_state_r932 VALUES(?,?,?,?,?)",new Object[]{modelHash,target,inputHash,previous,advanced});return past;
        }},null);
        if(result==null)throw new IllegalStateException("R932_D2_STATE_COMMIT_FAILED");return result;
    }
    private static String encode(double[] x){StringBuilder s=new StringBuilder();for(double v:x){if(s.length()>0)s.append(',');s.append(Double.toHexString(TrainedModelMathR932.finite(v)));}return s.toString();}
    private static double[] decode(String text,int length){String[] parts=text.split(",",-1);if(parts.length!=length)throw new IllegalStateException("R932_D2_STATE_DIMENSION");double[] out=new double[length];for(int i=0;i<length;i++)out[i]=TrainedModelMathR932.finite(Double.valueOf(parts[i]));return out;}
}
