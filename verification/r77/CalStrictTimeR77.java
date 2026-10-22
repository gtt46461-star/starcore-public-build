package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;

/** Controlled SQLite fixtures; actual CAL owner, never fake certificate/REAL receipts. */
public final class CalStrictTimeR77 {
    static int checks;
    static void ok(boolean v,String message){if(!v)throw new AssertionError(message);checks++;}
    static String h(String x){return V51ChecksumEngine.sha256(x);}
    static long n(SQLiteDatabase db,String q){try(Cursor c=db.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String text(SQLiteDatabase db,String q){try(Cursor c=db.rawQuery(q,null)){return c.moveToFirst()?c.getString(0):"";}}
    static void init(SQLiteDatabase db){
        db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,dirty INTEGER)");
        db.execSQL("CREATE TABLE draw_dependency_input_cp12(_id INTEGER,issue TEXT PRIMARY KEY,open_time INTEGER,fetch_time INTEGER,source_line TEXT,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,dirty_reason TEXT,first_seen_at INTEGER,draw_hash_version INTEGER,draw_hash_v2 TEXT,input_hash TEXT)");
        db.execSQL("CREATE TABLE research_candidate_cal_observation_r932(study_hash TEXT,target_issue TEXT,candidate_key TEXT,structure_hash TEXT,bundle_hash TEXT,selection_hash TEXT,raw_score REAL,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,prediction_hash TEXT,frozen_at INTEGER,hit INTEGER DEFAULT -1,PRIMARY KEY(study_hash,target_issue))");
        db.execSQL("CREATE TABLE research_candidate_cal_study_r932(study_hash TEXT PRIMARY KEY,candidate_key TEXT,captured_n INTEGER,settled_n INTEGER,state TEXT,first_issue TEXT)");
        db.execSQL("CREATE TABLE research_capture_reservation_v2(reservation_id TEXT PRIMARY KEY,study_hash TEXT,state TEXT,last_reason TEXT,generation INTEGER)");
        db.execSQL("CREATE TABLE r975_calibration_failure(study_hash TEXT PRIMARY KEY,candidate_key TEXT,reason TEXT,recorded_at INTEGER)");
        db.execSQL("CREATE TABLE r979_calibration_head(base_study TEXT PRIMARY KEY,current_study TEXT,updated_at INTEGER)");
        db.execSQL("CREATE TABLE r979_calibration_restart(new_study TEXT PRIMARY KEY,base_study TEXT,previous_study TEXT,candidate_key TEXT,first_issue TEXT,reason TEXT,created_at INTEGER)");
    }
    static void observation(SQLiteDatabase db,String study,String key,String issue,long at){
        db.execSQL("INSERT INTO research_candidate_cal_study_r932 VALUES(?,?,1,0,'COLLECTING',?)",new Object[]{study,key,issue});
        db.execSQL("INSERT INTO research_candidate_cal_observation_r932 VALUES(?,?,?,?,?,?,.35,1,1,2,3,?,?,-1)",new Object[]{study,issue,key,h("structure"),h("bundle"),h("selector"),h(study+issue),at});
        db.execSQL("INSERT INTO research_capture_reservation_v2 VALUES(?,?,'ACTIVE','',0)",new Object[]{h(study),study});
    }
    static String fitFailure(SQLiteDatabase db,String study){
        try{CandidateCalibrationR932.loadFit(db,study);return "";}catch(IllegalStateException e){return e.getMessage();}
    }
    static void retained(SQLiteDatabase db,Models.Draw draw,boolean malformedNumbers,boolean damagedHash){
        Object[] row=new Object[21];row[0]=1;row[1]=draw.issue;row[2]=draw.openTime;row[3]=draw.openTime+1;row[4]="fixture";
        for(int i=0;i<10;i++)row[5+i]=malformedNumbers?1:draw.numbers[i];
        row[15]=0;row[16]="";row[17]=draw.openTime+1;row[18]=2;row[19]=DrawHashR36.sha256V2(draw);
        StringBuilder b=new StringBuilder("CP12_DRAW_DEPENDENCY_INPUT_V1");
        for(int i=0;i<20;i++){String v=String.valueOf(row[i]);b.append('|').append(v.length()).append(':').append(v);}
        row[20]=damagedHash?h("damaged-retained-input"):h(b.toString());
        db.execSQL("INSERT INTO draw_dependency_input_cp12 VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",row);
    }
    public static void main(String[] a)throws Exception{
        try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
            init(db);Models.Draw draw=new Models.Draw();draw.issue="202610130651";draw.openTime=1791888663000L;
            draw.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};
            String valid=h("valid"),equal=h("equal"),late=h("late"),zero=h("zero");
            db.beginTransaction();try{
                observation(db,valid,"algorithm:test-valid",draw.issue,draw.openTime-1);
                observation(db,equal,"algorithm:test-equal",draw.issue,draw.openTime);
                observation(db,late,"algorithm:test-late",draw.issue,draw.openTime+2976);
                observation(db,zero,"algorithm:test-zero",draw.issue,0);
                CandidateCalibrationR932.settle(db,draw);db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            ok(n(db,"SELECT COUNT(*) FROM research_candidate_cal_observation_r932 WHERE hit=-2")==3,"equal/late/zero capture times rejected instead of counted as CAL labels");
            ok(n(db,"SELECT COUNT(*) FROM research_candidate_cal_observation_r932")==4,"original observation rows preserved");
            ok(n(db,"SELECT settled_n FROM research_candidate_cal_study_r932 WHERE study_hash='"+valid+"'")==1,"strictly earlier capture gets one valid settlement");
            ok(n(db,"SELECT SUM(settled_n) FROM research_candidate_cal_study_r932")==1,"invalid captures do not increase denominator");
            ok(n(db,"SELECT COUNT(*) FROM research_candidate_cal_study_r932 WHERE state='BLOCKED_INVALID_INPUT'")==3,"invalid studies blocked");
            ok(n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE state='FAILED_GAP' AND generation=1")==3,"invalid reservations release future capacity with generation advance");
            db.beginTransaction();try{CandidateCalibrationR932.settle(db,draw);db.setTransactionSuccessful();}finally{db.endTransaction();}
            ok(n(db,"SELECT SUM(settled_n) FROM research_candidate_cal_study_r932")==1,"settlement replay idempotent including terminal invalid observations");
            String next;
            db.beginTransaction();try{next=CandidateCalibrationR932.restartAfterGap(db,h("base"),late,"algorithm:test-late","202610130652");db.setTransactionSuccessful();}finally{db.endTransaction();}
            ok(!late.equals(next),"late study can restart only under a fresh prospective identity");
            ok(n(db,"SELECT COUNT(*) FROM r979_calibration_restart")==1,"restart has one durable lineage row");
            ok(n(db,"SELECT COUNT(*) FROM research_candidate_cal_observation_r932 WHERE study_hash='"+next+"'")==0,"restart cannot inherit any invalid labels");
            db.beginTransaction();try{
                String replay=CandidateCalibrationR932.restartAfterGap(db,h("base"),next,"algorithm:test-late","202610130652");
                ok(next.equals(replay),"new study replay preserves its identity");
                String quality=h("quality");
                db.execSQL("INSERT INTO research_candidate_cal_study_r932 VALUES(?, 'algorithm:quality',241,241,'BLOCKED_INVALID_INPUT','202610130001')",new Object[]{quality});
                db.execSQL("INSERT INTO r975_calibration_failure VALUES(?,'algorithm:quality','CALIBRATION_QUALITY_BLOCKED',1)",new Object[]{quality});
                ok(quality.equals(CandidateCalibrationR932.restartAfterGap(db,h("quality-base"),quality,"algorithm:quality","202610130652")),"quality failure cannot start a retry experiment");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            // An old READY row created before upgrade must also fail the fit owner.
            db.execSQL("INSERT INTO draws VALUES(?,?,0)",new Object[]{draw.issue,draw.openTime});
            db.execSQL("UPDATE research_candidate_cal_study_r932 SET state='READY' WHERE study_hash=?",new Object[]{late});
            boolean rejected=false;try{CandidateCalibrationR932.loadFit(db,late);}catch(IllegalStateException e){rejected="R77_CAL_STRICT_TIME_REJECTED".equals(e.getMessage());}
            ok(rejected,"fit owner rechecks legacy labels against authoritative draw time");
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            CandidateCalibrationR932.fitOne(store);
            ok("BLOCKED_INVALID_INPUT".equals(text(db,"SELECT state FROM research_candidate_cal_study_r932 WHERE study_hash='"+late+"'")),"production fit owner blocks invalid old READY instead of retrying it indefinitely");
            CandidateCalibrationR932.fitOne(store);
            ok(n(db,"SELECT COUNT(*) FROM research_candidate_cal_study_r932 WHERE state='READY'")==0,"repeated production fit pass has no poisoned READY head");
            // Single-row READY fixtures reach fixed-support validation only after
            // authoritative retained-time validation succeeds; they cannot fit a model.
            db.execSQL("UPDATE research_candidate_cal_study_r932 SET state='READY' WHERE study_hash=?",new Object[]{valid});
            db.execSQL("DELETE FROM draws");retained(db,draw,false,false);
            ok("R932_CAL_FIXED_SUPPORT".equals(fitFailure(db,valid)),"valid retained input authenticates time after hot eviction");
            db.execSQL("INSERT INTO draws VALUES(?,?,1)",new Object[]{draw.issue,draw.openTime});
            ok("R77_CAL_DRAW_TIME_UNAVAILABLE".equals(fitFailure(db,valid)),"dirty hot source cannot fall back to valid retained copy");
            db.execSQL("DELETE FROM draws");db.execSQL("DELETE FROM draw_dependency_input_cp12");retained(db,draw,false,true);
            ok("R77_CAL_RETAINED_INPUT_INVALID".equals(fitFailure(db,valid)),"damaged retained input hash cannot authenticate time");
            db.execSQL("DELETE FROM draw_dependency_input_cp12");retained(db,draw,true,false);
            ok("R77_CAL_RETAINED_INPUT_INVALID".equals(fitFailure(db,valid)),"hashed but invalid retained permutation cannot authenticate time");
            CandidateCalibrationR932.fitOne(store);
            ok("R77_CAL_RETAINED_INPUT_INVALID".equals(text(db,"SELECT reason FROM r975_calibration_failure WHERE study_hash='"+valid+"'")),"retained corruption gets a durable production failure receipt");
            db.beginTransaction();try{
                String recovered=CandidateCalibrationR932.restartAfterGap(db,h("valid-base"),valid,"algorithm:test-valid","202610130652");
                ok(!valid.equals(recovered),"corrupt input releases a new prospective study without reusing labels");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            String missing=h("missing");db.beginTransaction();try{observation(db,missing,"algorithm:test-missing","202610130650",draw.openTime-1);db.execSQL("UPDATE research_candidate_cal_study_r932 SET state='READY' WHERE study_hash=?",new Object[]{missing});db.setTransactionSuccessful();}finally{db.endTransaction();}
            CandidateCalibrationR932.fitOne(store);
            ok("R77_CAL_DRAW_TIME_UNAVAILABLE".equals(text(db,"SELECT reason FROM r975_calibration_failure WHERE study_hash='"+missing+"'")),"missing original input cannot block all subsequent READY studies");
            db.beginTransaction();try{
                ok(!missing.equals(CandidateCalibrationR932.restartAfterGap(db,h("missing-base"),missing,"algorithm:test-missing","202610130652")),"unavailable historical input permits a clean prospective restart");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_CAL_OWNER_CONTROLLED_SQLITE_TIME_FENCES_NOT_PHONE_QUALITY\"}\n");
        }
    }
}
