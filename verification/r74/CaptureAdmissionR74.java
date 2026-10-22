package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.nio.file.*;

/** Public reservation owner, full installed SQLite schema, synthetic metadata only.
 * No capture, certificate, draw outcome or user trade is fabricated by the product. */
public final class CaptureAdmissionR74 {
    static int checks;
    static final String TARGET="202609230021", BASE="202609230020";
    static final String BUNDLE=ModelBundleRuntimeR932.current().hash;
    static final String SELECTOR=SelectionEvidenceContractR929.installedPolicy().hash;
    static void ok(boolean pass,String name){if(!pass)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static String h(String s){return V51ChecksumEngine.sha256(s);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String scan(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key='r74_capture_admission_scan'",null)){return c.moveToFirst()?c.getString(0):"";}}
    static String candidate(SQLiteDatabase db,int ordinal,boolean blocked,String rule){
        String learner="r74-learner-"+ordinal;
        CandidateAstR952 ast=new CandidateAstR952(CandidateAstR952.Operator.POSITION_FREQUENCY,1,20+ordinal);
        CandidateCompilerR952.Model model=CandidateCompilerR952.Model.fromParameters(300,CandidateCompilerR952.Model.initial().parameters());
        String artifact=h("r74-artifact-"+ordinal);
        String epoch=new ResearchSemanticsR967.Builder("R952_NUMERIC_RUN_ORIGIN_V1").text("learner",learner).text("first",TARGET).hash("ast",ast.structuralHash).hash("rule",rule).hash("initial",model.hash()).text("artifact",artifact).digest();
        String run=new ResearchSemanticsR967.Builder("R967_NUMERIC_RUN_V2").text("learner",learner).text("first",TARGET).hash("ast",ast.structuralHash).hash("rule",rule).hash("initial",model.hash()).text("artifact",artifact).text("split",ResearchEvaluationRunR967.SPLIT).hash("epoch",epoch).digest();
        String structure=ResearchEvaluationRunR967.evaluationHash(ast,model,rule,epoch),id=ResearchEvaluationRunR967.PREFIX+structure.substring(0,24),key="algorithm:"+id;
        db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,state,created_issue,may_create_real,created_at) VALUES(?,?,?,?,'VALIDATED',?,0,?)",new Object[]{learner,learner,ast.canonical,ast.structuralHash,TARGET,ordinal});
        db.execSQL("INSERT INTO "+ResearchEvaluationRunR967.RUN+" VALUES(?,?,?,?,?,?,300,?,?,?,?)",new Object[]{run,learner,TARGET,ast.structuralHash,rule,model.payload(),artifact,ResearchEvaluationRunR967.SPLIT,epoch,ordinal});
        db.execSQL("INSERT INTO "+ResearchEvaluationRunR967.EVAL+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,300)",new Object[]{id,learner,ast.canonical,model.payload(),structure,model.hash(),BASE,TARGET,artifact,ordinal,run,rule,epoch});
        if(blocked){
            ResearchMultiplicityR932.registerCandidate(db,key,"algorithm:R958_FIXED_POSITION_SOFTMAX",structure,TARGET);
            String study=CandidateCalibrationR932.study(db,key,structure,BUNDLE,SELECTOR);
            db.execSQL("INSERT INTO "+CandidateCalibrationR932.STUDY+" VALUES(?,?,0,0,'BLOCKED_INVALID_INPUT',?)",new Object[]{study,key,TARGET});
            db.execSQL("INSERT INTO r975_calibration_failure VALUES(?,?,'R932_CAL_FIXED_SUPPORT',1)",new Object[]{study,key});
        }
        return key;
    }
    static int reserve(SQLiteDatabase db,boolean commit){
        db.beginTransaction();try{int admitted=GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,TARGET,BUNDLE,SELECTOR);if(commit)db.setTransactionSuccessful();return admitted;}finally{db.endTransaction();}
    }
    public static void main(String[] a)throws Exception{
        String waiting;
        ok(TrainingContinuationR973.compatible("2b849d259828ad017b71cff6b39b8c4cadab01f33fee83112e6382d1b7847552"),"exact R73 development rule continues");
        ok(!TrainingContinuationR973.compatible(h("unreviewed-rule")),"unknown development rule rejected");
        try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
            db.beginTransaction();try{
                db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,1,1,2,3,?,1)",new Object[]{TARGET,BUNDLE,h("snapshot"),SELECTOR,h("champion")});
                for(int i=1;i<=32;i++)candidate(db,i,true,TrainingCodeIdentityR964.SHA256);
                waiting=candidate(db,33,false,TrainingCodeIdentityR964.SHA256);
                candidate(db,34,false,h("unreviewed-rule"));
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            ok(n(db,"SELECT COUNT(*) FROM sqlite_master WHERE type='trigger'")>1200,"full installed triggers retained");
            reserve(db,false);
            ok(scan(db).isEmpty(),"rolled back scan publishes no cursor or reservation");
            int first=reserve(db,true);System.out.println("ADMITTED_FIRST_PAGE="+first);
            ok(first==0,"first page skips blocked studies without fabricating samples");
        }
        // Fresh connection: progress may not depend on static process-local state.
        try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
            int second=reserve(db,true);System.out.println("ADMITTED_AFTER_REOPEN="+second);
            ok(second==1,"waiting candidate beyond first 32 is admitted after restart");
            ok(n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE candidate_key='"+waiting+"' AND state='RESERVED'")==1,"exact waiting candidate owns reservation");
            ok(n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2")==1,"unknown training rule remains ineligible");
            String cursor=scan(db);reserve(db,false);
            ok(cursor.equals(scan(db)),"cursor rolls back with reservation transaction");
            ok(reserve(db,true)==0,"repeat does not create duplicate reservation");
            ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.STUDY+" WHERE state='BLOCKED_INVALID_INPUT'")==32,"blocked studies not restarted or reclassified");
            ok(n(db,"SELECT COUNT(*) FROM r979_calibration_restart")==0,"low quality or invalid evidence is not reset");
            db.execSQL("UPDATE "+DbHelper.TABLE_APP_STATE+" SET state_value='not-a-valid-cursor' WHERE state_key='r74_capture_admission_scan'");
            ok(reserve(db,true)==0&&!scan(db).equals("not-a-valid-cursor"),"malformed optional cursor safely restarts bounded scan");
            db.beginTransaction();try{
                for(int i=35;i<=42;i++)candidate(db,i,false,TrainingCodeIdentityR964.SHA256);
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            int added=0;for(int i=0;i<4;i++)added+=reserve(db,true);
            ok(added==7,"free capacity filled across bounded pages");
            ok(n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE state IN('RESERVED','ACTIVE')")==8,"active capacity still exactly eight");
            ok(n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE candidate_key='"+waiting+"' AND state='RESERVED'")==1,"existing active candidate never evicted");
            for(int i=0;i<3;i++)ok(reserve(db,true)==0,"full capacity remains bounded attempt "+i);
            ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==0,"scheduler creates no observations");
            ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)==0,"scheduler creates no calibrated models");
            ok(n(db,"SELECT COUNT(*) FROM research_certificate_r929")==0,"scheduler creates no certificates");
            ok(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_TRADES)==0,"scheduler creates no user bets");
            try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){ok(c.moveToFirst()&&"ok".equals(c.getString(0)),"SQLite integrity");}
        }
        Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_RESERVATION_OWNER_FULL_SCHEMA_SYNTHETIC_METADATA_NOT_PHONE\"}\n");
    }
}
