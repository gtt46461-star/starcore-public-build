package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;

/** Separate original and counterfactual score calibration from strictly earlier replay cases.
 * These postmortem estimates never become production calibration or Future promotion evidence. */
public final class ModuleCalibrationR932 {
    static final String OBS="research_module_cal_observation_r932",MODEL="research_module_cal_model_r932";
    private ModuleCalibrationR932(){}
    static void schema(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS "+OBS+"(identity_hash TEXT NOT NULL,target_issue TEXT NOT NULL,source_hash TEXT NOT NULL,original_score REAL NOT NULL,counter_score REAL NOT NULL,original_hit INTEGER NOT NULL CHECK(original_hit IN (0,1)),counter_hit INTEGER NOT NULL CHECK(counter_hit IN (0,1)),record_hash TEXT NOT NULL,PRIMARY KEY(identity_hash,target_issue))");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+MODEL+"(identity_hash TEXT PRIMARY KEY,validation_until TEXT NOT NULL,model_json TEXT NOT NULL,model_hash TEXT NOT NULL,created_at INTEGER NOT NULL)");
    }
    static String identity(FrozenModuleInputR932.Input input,ModuleReplayR932.Module module){
        FrozenConsumerRecipeR932.Recipe r=input.consumer;
        String selector=r==null?"NO_CONSUMER":r.lane+'|'+r.id+'|'+r.structure+'|'+Double.toHexString(r.weight);
        return V51ChecksumEngine.sha256("R932_MODULE_CAL_120_1_120|"+SourceManifestR815.SOURCE_TREE_SHA256+'|'+input.bundle+'|'+selector+'|'+module.name()+'|'+V51ActionMetaFeatureEngine.featureManifestHash());
    }
    static void enrich(SQLiteDatabase db,JSONObject result)throws JSONException {
        if(!db.inTransaction())throw new IllegalStateException("R932_MODULE_CAL_TRANSACTION");
        if(!"REEXECUTED".equals(result.optString("state")))return;
        String identity=result.getString("calibration_identity"),target=result.getString("target_issue");
        JSONObject model=load(db,identity,target);
        if(model==null)model=fitEarlier(db,identity,target);
        result.put("delta_brier",JSONObject.NULL);result.put("brier_status","WAIT_EARLIER_MODULE_CAL_120_1_120");
        if(model!=null){
            String hash=V51ChecksumEngine.sha256(model.toString());result.put("module_calibration_hash",hash);result.put("calibration_validation_until",model.getString("validation_last"));
            JSONObject original=model.getJSONObject("original"),counter=model.getJSONObject("counter");
            if(original.getDouble("validation_brier")<=.21&&counter.getDouble("validation_brier")<=.21&&original.getDouble("validation_ece")<=.04&&counter.getDouble("validation_ece")<=.04){
                double p=ResearchLogisticCalibrationR932.predict(original,result.getDouble("original_rank_score")),q=ResearchLogisticCalibrationR932.predict(counter,result.getDouble("selected_rank_score"));
                double a=p-result.getInt("original_hit"),b=q-result.getInt("counterfactual_hit");
                result.put("original_module_cal_p",p);result.put("counterfactual_module_cal_p",q);result.put("delta_brier",b*b-a*a);
                result.put("brier_status","EARLIER_MODULE_CAL_VALIDATED_POSTMORTEM_ONLY; NEGATIVE_DELTA_IS_IMPROVEMENT");
            }else result.put("brier_status","MODULE_CAL_HOLDOUT_GATE_FAILED");
        }
        // Persist after fitting, so the current label cannot enter either fit or holdout.
        String source=result.getString("source_hash");double a=result.getDouble("original_rank_score"),b=result.getDouble("selected_rank_score");int y=result.getInt("original_hit"),z=result.getInt("counterfactual_hit");
        String hash=record(identity,target,source,a,b,y,z);
        db.execSQL("INSERT OR IGNORE INTO "+OBS+" VALUES(?,?,?,?,?,?,?,?)",new Object[]{identity,target,source,a,b,y,z,hash});
        try(Cursor c=db.rawQuery("SELECT record_hash FROM "+OBS+" WHERE identity_hash=? AND target_issue=?",new String[]{identity,target})){if(!c.moveToFirst()||!hash.equals(c.getString(0)))throw new IllegalStateException("R932_MODULE_CAL_OBSERVATION_DRIFT");}
    }
    static String record(String identity,String target,String source,double a,double b,int y,int z){
        return V51ChecksumEngine.sha256(identity+'|'+target+'|'+source+'|'+Double.toHexString(a)+'|'+Double.toHexString(b)+'|'+y+'|'+z);
    }
    private static JSONObject load(SQLiteDatabase db,String identity,String target)throws JSONException {
        try(Cursor c=db.rawQuery("SELECT validation_until,model_json,model_hash FROM "+MODEL+" WHERE identity_hash=?",new String[]{identity})){
            if(!c.moveToFirst())return null;
            if(!V51ChecksumEngine.sha256(c.getString(1)).equals(c.getString(2)))throw new IllegalStateException("R932_MODULE_CAL_MODEL_HASH");
            if(target.compareTo(c.getString(0))<=0)return null;
            JSONObject model=new JSONObject(c.getString(1));if(!identity.equals(model.getString("identity_hash"))||!c.getString(0).equals(model.getString("validation_last")))throw new IllegalStateException("R932_MODULE_CAL_MODEL_IDENTITY");return model;
        }
    }
    private static JSONObject fitEarlier(SQLiteDatabase db,String identity,String target)throws JSONException {
        double[] a=new double[241],b=new double[241],y=new double[241],z=new double[241];String[] issues=new String[241];int n=0;StringBuilder evidence=new StringBuilder(identity);
        try(Cursor c=db.rawQuery("SELECT target_issue,source_hash,original_score,counter_score,original_hit,counter_hit,record_hash FROM "+OBS+" WHERE identity_hash=? AND target_issue<? ORDER BY target_issue LIMIT 241",new String[]{identity,target})){
            while(c.moveToNext()){
                ResearchWorkBudgetR927.checkpoint();issues[n]=c.getString(0);a[n]=c.getDouble(2);b[n]=c.getDouble(3);y[n]=c.getInt(4);z[n]=c.getInt(5);
                String hash=record(identity,issues[n],c.getString(1),a[n],b[n],(int)y[n],(int)z[n]);if(!hash.equals(c.getString(6)))throw new IllegalStateException("R932_MODULE_CAL_SOURCE_HASH");evidence.append('|').append(hash);n++;
            }
        }
        if(n<241)return null;
        JSONObject original=ResearchLogisticCalibrationR932.fit(a,y),counter=ResearchLogisticCalibrationR932.fit(b,z);
        ResearchLogisticCalibrationR932.validate(original,a,y);ResearchLogisticCalibrationR932.validate(counter,b,z);
        JSONObject model=new JSONObject();model.put("revision","R932_MODULE_PAIRED_CAL_V1");model.put("identity_hash",identity);model.put("fit_first",issues[0]);model.put("fit_last",issues[119]);model.put("embargo_issue",issues[120]);model.put("validation_first",issues[121]);model.put("validation_last",issues[240]);
        model.put("original",original);model.put("counter",counter);model.put("evidence_hash",V51ChecksumEngine.sha256(evidence.toString()));model.put("scope","POSTMORTEM_ONLY_NOT_PRODUCTION_NOT_FUTURE");
        String text=model.toString();db.execSQL("INSERT OR IGNORE INTO "+MODEL+" VALUES(?,?,?,?,?)",new Object[]{identity,issues[240],text,V51ChecksumEngine.sha256(text),System.currentTimeMillis()});
        return load(db,identity,target);
    }
}
