package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;

/** Original prospective champion probability; absence is never repaired using its result. */
public final class ChampionProbabilityEvidenceR82 {
    public static final String TABLE="research_champion_probability_r82",REVISION="R82_ORIGINAL_RANK1_CALIBRATED_CHAMPION_V1";
    private ChampionProbabilityEvidenceR82(){}
    public static void schema(SQLiteDatabase db){
        if(db==null||!db.inTransaction())throw new IllegalStateException("R82_CHAMPION_SCHEMA_OWNER");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(target_issue TEXT NOT NULL,bundle_hash TEXT NOT NULL,champion_prediction_hash TEXT NOT NULL,input_hash TEXT NOT NULL,selection_hash TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,probability REAL,calibration_hash TEXT NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,proof_json TEXT NOT NULL,proof_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,PRIMARY KEY(target_issue,bundle_hash),FOREIGN KEY(target_issue,bundle_hash) REFERENCES research_fixed_champion_r932(target_issue,bundle_hash))");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r82_champion_p_no_update BEFORE UPDATE ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R82_CHAMPION_PROBABILITY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r82_champion_p_no_replace BEFORE INSERT ON "+TABLE+" WHEN EXISTS(SELECT 1 FROM "+TABLE+" x WHERE x.target_issue=NEW.target_issue AND x.bundle_hash=NEW.bundle_hash) BEGIN SELECT RAISE(ABORT,'R82_CHAMPION_PROBABILITY_REPLACE'); END");
    }
    public static boolean installed(SQLiteDatabase db){
        String[] objects={TABLE,"r82_champion_p_no_update","r82_champion_p_no_replace","r82_champion_probability_revision_insert","r82_champion_probability_revision_delete","r82_champion_probability_archive_guard","r82_calibration_pin_"+TABLE,"r82_calibration_pin_"+FixedResearchWindowR932.CHAMPION};
        for(String object:objects)try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE name=? AND type IN('table','trigger')",new String[]{object})){if(!c.moveToFirst())return false;}return true;
    }
    static void freeze(SQLiteDatabase db,DeferredEvidenceSnapshotR36.Snapshot snapshot,V51Models.PositionAction selected,String bundle,String selector,String prediction){
        if(!db.inTransaction())throw new IllegalStateException("R82_CHAMPION_FREEZE_TX");
        if(!TargetIssueResolver.isValid(snapshot.targetIssue)||!DecisionGateEngine.b12K3(selected.n1,selected.n2,selected.n3))throw new IllegalArgumentException("R82_CHAMPION_FREEZE_IDENTITY");
        try(Cursor drawn=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? AND dirty=0 LIMIT 1",new String[]{snapshot.targetIssue})){if(drawn.moveToFirst())throw new IllegalStateException("R82_CHAMPION_POSTDRAW_REJECTED");}
        String raw=null,rawHash=null;for(String s:snapshot.proofs){if(s.startsWith(RawTrainingInputR932.PREFIX)){if(raw!=null)throw new IllegalStateException("R82_CHAMPION_RAW_DUPLICATE");raw=s.substring(RawTrainingInputR932.PREFIX.length());}if(s.startsWith(RawTrainingInputR932.HASH_PREFIX)){if(rawHash!=null)throw new IllegalStateException("R82_CHAMPION_RAW_HASH_DUPLICATE");rawHash=s.substring(RawTrainingInputR932.HASH_PREFIX.length());}}
        Double probability=null;String reason="ORIGINAL_CALIBRATED_PROBABILITY_MISSING",calibration="";
        try{
            ModelBundleRuntimeR932.Snapshot model=ModelBundleRuntimeR932.current();ModelBundleParametersR932.Calibration cal=model.calibration;
            if(raw==null||rawHash==null||!rawHash.equals(V51ChecksumEngine.sha256(raw)))reason="ORIGINAL_RAW_INPUT_HASH_MISSING_OR_INVALID";
            else{
                try{JSONObject proof=new JSONObject(raw);DeferredEvidenceSnapshotR36.ActionRow rankOne=null;for(DeferredEvidenceSnapshotR36.ActionRow row:snapshot.actions)if(row.rank==1){if(rankOne!=null)throw new IllegalStateException("R82_CHAMPION_RANK1_AMBIGUOUS");rankOne=row;}
                calibration=model.artifactHashes.get("CALIBRATION");if(calibration==null)calibration="";
                boolean identity=RawTrainingInputR932.REVISION.equals(proof.getString("revision"))&&snapshot.targetIssue.equals(proof.getString("target_issue"))&&snapshot.baseIssue.equals(proof.getString("base_issue"))&&bundle.equals(proof.getString("model_bundle_hash"))&&selector.equals(proof.getString("selection_policy_hash"))&&V51ActionMetaFeatureEngine.featureManifestHash().equals(proof.getString("meta_schema_sha256"))&&bundle.equals(model.hash)&&selector.equals(model.selectionHash)&&selector.equals(cal.SELECTION_POLICY_SHA256)&&V51ActionMetaFeatureEngine.featureManifestHash().equals(cal.INPUT_SCHEMA_SHA256)&&rankOne!=null&&rankOne.position==selected.position&&rankOne.n1==selected.n1&&rankOne.n2==selected.n2&&rankOne.n3==selected.n3&&proof.getInt("selected_ordinal")==K3ActionSpaceEngine.globalOrdinal(selected.position,selected.n1,selected.n2,selected.n3)&&selected.checksum.equals(proof.getString("selected_prediction_hash"));
                if(!identity)reason="ORIGINAL_SELECTOR_ACTION_SCHEMA_BUNDLE_MISMATCH";
                else if(!cal.PROVEN||cal.CALIBRATION_SUPPORT_N<120||!Double.isFinite(cal.OOS_ECE)||cal.OOS_ECE<0||cal.OOS_ECE>.04||!calibration.matches("[0-9a-f]{64}"))reason="ORIGINAL_CALIBRATION_NOT_QUALIFIED";
                else if(!Double.isFinite(rankOne.selectedHitP)||rankOne.selectedHitP<0||rankOne.selectedHitP>1)reason="ORIGINAL_SELECTED_HIT_P_MISSING";
                else{probability=rankOne.selectedHitP;reason="";}
                }catch(JSONException malformedOriginal){reason="ORIGINAL_RAW_INPUT_REQUIRED_FIELDS_MISSING";}
            }
            JSONObject out=new JSONObject();out.put("revision",REVISION);out.put("target_issue",snapshot.targetIssue);out.put("bundle_hash",bundle);out.put("input_hash",snapshot.recordHash);out.put("selection_hash",selector);out.put("champion_prediction_hash",prediction);out.put("position",selected.position);out.put("n1",selected.n1);out.put("n2",selected.n2);out.put("n3",selected.n3);out.put("probability",probability==null?JSONObject.NULL:probability);out.put("calibration_hash",calibration);out.put("raw_input_proof_hash",rawHash==null?"":rawHash);out.put("original_calibration_support_n",cal.CALIBRATION_SUPPORT_N);out.put("original_calibration_ece",cal.OOS_ECE);out.put("original_calibration_proven",cal.PROVEN);out.put("original_calibration_input_schema",cal.INPUT_SCHEMA_SHA256);out.put("original_calibration_selection_hash",cal.SELECTION_POLICY_SHA256);out.put("status",probability==null?"MISSING":"PROVEN");out.put("reason",reason);out.put("may_authorize_real",false);String json=out.toString();
            db.execSQL("INSERT INTO "+TABLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{snapshot.targetIssue,bundle,prediction,snapshot.recordHash,selector,selected.position,selected.n1,selected.n2,selected.n3,probability,calibration,probability==null?"MISSING":"PROVEN",reason,json,V51ChecksumEngine.sha256(json),System.currentTimeMillis()});
        }catch(JSONException malformed){throw new IllegalStateException("R82_CHAMPION_PROOF_JSON",malformed);}
    }
    /** Column projection is explicitly stable for the calibration reader and tests. */
    static double verifiedProbability(Cursor c,int start,String target,String bundle,String prediction,String input,String selector,int position,int n1,int n2,int n3,long openTime){
        // p,calibration,status,proof_json,proof_hash,frozen_at; NULL/old rows remain unknown.
        if(c.isNull(start)||c.isNull(start+2)||!"PROVEN".equals(c.getString(start+2)))return Double.NaN;
        double p=c.getDouble(start);String json=c.getString(start+3);if(!Double.isFinite(p)||p<0||p>1||json==null||json.length()>16384||!V51ChecksumEngine.sha256(json).equals(c.getString(start+4))||c.getLong(start+5)<=0||c.getLong(start+5)>=openTime)return Double.NaN;
        try{JSONObject v=new JSONObject(json);if(!REVISION.equals(v.getString("revision"))||!target.equals(v.getString("target_issue"))||!bundle.equals(v.getString("bundle_hash"))||!prediction.equals(v.getString("champion_prediction_hash"))||!input.equals(v.getString("input_hash"))||!selector.equals(v.getString("selection_hash"))||position!=v.getInt("position")||n1!=v.getInt("n1")||n2!=v.getInt("n2")||n3!=v.getInt("n3")||!"PROVEN".equals(v.getString("status"))||!c.getString(start+1).equals(v.getString("calibration_hash"))||!v.getString("calibration_hash").matches("[0-9a-f]{64}")||!v.getString("raw_input_proof_hash").matches("[0-9a-f]{64}")||!v.getBoolean("original_calibration_proven")||v.getInt("original_calibration_support_n")<120||!Double.isFinite(v.getDouble("original_calibration_ece"))||v.getDouble("original_calibration_ece")<0||v.getDouble("original_calibration_ece")>.04||!selector.equals(v.getString("original_calibration_selection_hash"))||!V51ActionMetaFeatureEngine.featureManifestHash().equals(v.getString("original_calibration_input_schema"))||Double.compare(p,v.getDouble("probability"))!=0)return Double.NaN;return p;}catch(JSONException malformed){return Double.NaN;}
    }
    public static String activeCalibrationPin(String issueExpression){return "EXISTS(SELECT 1 FROM research_candidate_cal_observation_r932 o JOIN research_candidate_cal_study_r932 s ON s.study_hash=o.study_hash WHERE o.target_issue="+issueExpression+" AND s.state IN('COLLECTING','READY'))";}
    public static String archiveEligible(String table){return "length("+table+".target_issue)=12 AND "+table+".target_issue<? AND NOT ("+activeCalibrationPin(table+".target_issue")+") AND NOT EXISTS(SELECT 1 FROM research_fixed_window_r932 w WHERE w.champion_hash="+table+".bundle_hash AND w.state IN('OPEN','PAUSED') AND "+table+".target_issue BETWEEN w.first_issue AND w.last_issue)";}
}
