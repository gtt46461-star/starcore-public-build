package com.ethk3.novaonepro;
import org.json.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class ModuleCalibrationHarnessR932 {
    static void check(boolean pass,String name){if(!pass)throw new AssertionError(name);System.out.println(name+"=PASS");}
    static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static String issue(int n){return String.format(java.util.Locale.US,"20260910%04d",n);}
    static JSONObject result(String identity,int n,int y,int z)throws Exception {
        JSONObject r=new JSONObject();r.put("state","REEXECUTED");r.put("calibration_identity",identity);r.put("target_issue",issue(n));r.put("source_hash",V51ChecksumEngine.sha256("fixture"+n));r.put("original_rank_score",.4);r.put("selected_rank_score",.5);r.put("original_hit",y);r.put("counterfactual_hit",z);return r;
    }
    static void enrich(SQLiteDatabase db,JSONObject r)throws Exception {db.beginTransaction();try{ModuleCalibrationR932.enrich(db,r);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
            ModuleCalibrationR932.schema(db);String identity=V51ChecksumEngine.sha256("SYNTHETIC_MODULE");
            JSONObject first=result(identity,1,1,1);enrich(db,first);check(first.isNull("delta_brier"),"INSUFFICIENT_MODULE_CAL_IS_UNKNOWN_NOT_FAKE_PROBABILITY");
            db.beginTransaction();try{for(int i=2;i<=241;i++){
                String source=V51ChecksumEngine.sha256("fixture"+i),hash=ModuleCalibrationR932.record(identity,issue(i),source,.4,.5,1,1);
                db.execSQL("INSERT INTO "+ModuleCalibrationR932.OBS+" VALUES(?,?,?,?,?,?,?,?)",new Object[]{identity,issue(i),source,.4,.5,1,1,hash});
            }db.setTransactionSuccessful();}finally{db.endTransaction();}
            db.execSQL("CREATE TRIGGER fixture_module_cal_abort BEFORE INSERT ON "+ModuleCalibrationR932.MODEL+" BEGIN SELECT RAISE(ABORT,'INJECTED'); END");
            boolean rejected=false;try{enrich(db,result(identity,242,0,1));}catch(RuntimeException expected){rejected=true;}
            check(rejected&&scalar(db,"SELECT COUNT(*) FROM "+ModuleCalibrationR932.MODEL).equals("0")&&scalar(db,"SELECT COUNT(*) FROM "+ModuleCalibrationR932.OBS).equals("241"),"CAL_MODEL_AND_CURRENT_OBSERVATION_ROLL_BACK_TOGETHER");
            db.execSQL("DROP TRIGGER fixture_module_cal_abort");JSONObject a=result(identity,242,0,1);enrich(db,a);
            check(!a.isNull("delta_brier")&&a.getDouble("delta_brier")<0&&a.getString("calibration_validation_until").equals(issue(241)),"PAST_ONLY_MODULE_CAL_PRODUCES_PAIRED_BRIER_DELTA");
            JSONObject model=new JSONObject(scalar(db,"SELECT model_json FROM "+ModuleCalibrationR932.MODEL));
            check(model.getString("fit_last").equals(issue(120))&&model.getString("embargo_issue").equals(issue(121))&&model.getString("validation_first").equals(issue(122)),"FIXED_120_TRAIN_ONE_EMBARGO_120_HOLDOUT");
            JSONObject b=result(identity,243,1,0);enrich(db,b);
            check(a.getString("module_calibration_hash").equals(b.getString("module_calibration_hash"))&&a.getDouble("original_module_cal_p")==b.getDouble("original_module_cal_p")&&a.getDouble("counterfactual_module_cal_p")==b.getDouble("counterfactual_module_cal_p"),"CURRENT_LABELS_CANNOT_CHANGE_FROZEN_MODELS_OR_PROBABILITIES");
            check(b.getDouble("delta_brier")>0,"PAIRED_DELTA_USES_EACH_ACTIONS_OWN_LABEL");
            enrich(db,b);check(scalar(db,"SELECT COUNT(*) FROM "+ModuleCalibrationR932.OBS).equals("243"),"RETRY_CANNOT_DUPLICATE_CALIBRATION_SUPPORT");
            rejected=false;try{enrich(db,result(identity,243,0,0));}catch(IllegalStateException expected){rejected=true;}check(rejected,"RETRY_LABEL_DRIFT_REJECTED");
            JSONObject changed=result(V51ChecksumEngine.sha256("DIFFERENT_SELECTOR_OR_MODULE"),244,0,1);enrich(db,changed);check(changed.isNull("delta_brier"),"CHANGED_SELECTOR_OR_MODULE_CANNOT_REUSE_CALIBRATION");
            JSONObject old=result(identity,120,1,1);enrich(db,old);check(old.isNull("delta_brier"),"OLD_CASE_CANNOT_USE_LATER_CALIBRATION");
            db.execSQL("UPDATE "+ModuleCalibrationR932.MODEL+" SET model_hash='tampered'");rejected=false;try{enrich(db,result(identity,245,1,1));}catch(IllegalStateException expected){rejected=true;}check(rejected,"MODEL_HASH_TAMPERING_BLOCKS_BRIER_OUTPUT");
            System.out.println("R932_MODULE_CALIBRATION_PASS|scope=SYNTHETIC_REAL_SQLITE_POSTMORTEM_NOT_DEVICE_NOT_FUTURE");
        }finally{db.shutdown();}
    }
}
