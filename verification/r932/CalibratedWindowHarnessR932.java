package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;

public final class CalibratedWindowHarnessR932 {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println(label+"=PASS");}
    static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static void freeze(SQLiteDatabase db,String issue,String bundle,String structure,boolean second,String other,int index) {
        db.beginTransaction();try {
            String champion=V51ChecksumEngine.sha256(issue+"CHAMPION");
            db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{issue,bundle,bundle,bundle,1,2,3,4,champion,1});
            FixedResearchWindowR932.freezePair(db,"algorithm:cal",structure,issue,V51ChecksumEngine.sha256(issue+"CANDIDATE"),1,1,2,3,1.0);
            if(second)FixedResearchWindowR932.freezePair(db,"algorithm:validation-negative-control",other,issue,V51ChecksumEngine.sha256(issue+"CONTROL"),1,index<120?1:2,index<120?2:3,index<120?3:4,1.0);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,1,2,3,4,5,6,7,8,9,10,0)",new Object[]{issue});
    }
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try {
            DbStore store=new DbStore(db);FixedResearchWindowR932.schema(db);
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER)");
            String bundle=V51ChecksumEngine.sha256("CAL_SYNTHETIC_BUNDLE"),structure=V51ChecksumEngine.sha256("CAL_SYNTHETIC_STRUCTURE"),other=V51ChecksumEngine.sha256("CAL_SYNTHETIC_CONTROL"),issue="202609100001";
            db.beginTransaction();try {
                ResearchMultiplicityR932.registerCandidate(db,"algorithm:cal","family:cal",structure,issue);
                ResearchMultiplicityR932.registerCandidate(db,"algorithm:validation-negative-control","family:control",other,issue);
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            for(int i=0;i<241;i++){freeze(db,issue,bundle,structure,true,other,i);issue=TargetIssueResolver.next(issue).targetIssue;if(i%8==7)FixedResearchWindowR932.maintain(store);}
            for(int i=0;i<32;i++)FixedResearchWindowR932.maintain(store);
            check(scalar(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW).equals("0"),"CALIBRATION_241_ROWS_NEVER_COUNT_AS_PROMOTION_WINDOW");
            JSONObject fit=new JSONObject(scalar(db,"SELECT model_json FROM "+CandidateCalibrationR932.MODEL+" WHERE candidate_key='algorithm:cal'"));
            JSONObject control=new JSONObject(scalar(db,"SELECT model_json FROM "+CandidateCalibrationR932.MODEL+" WHERE candidate_key='algorithm:validation-negative-control'"));
            check(fit.getDouble("bias")==control.getDouble("bias")&&fit.getDouble("weight")==control.getDouble("weight")&&fit.getDouble("validation_brier")<control.getDouble("validation_brier"),"VALIDATION_LABELS_CHANGE_METRICS_WITHOUT_CHANGING_FIT");
            for(int i=0;i<120;i++){freeze(db,issue,bundle,structure,false,other,i);issue=TargetIssueResolver.next(issue).targetIssue;if(i%8==7)FixedResearchWindowR932.maintain(store);}
            for(int i=0;i<16;i++)FixedResearchWindowR932.maintain(store);
            check(scalar(db,"SELECT state FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_no=1").equals("PASS"),"NEW_120_CALIBRATED_PAIRED_WINDOW_PASSES_SYNTHETIC_ENDPOINT");
            check(scalar(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.PAIR+" WHERE selected_hit_p>0.9 AND length(calibration_hash)=64").equals("120"),"FUTURE_PROBABILITIES_BIND_EARLIER_FROZEN_CALIBRATION");
            for(int i=0;i<240;i++){freeze(db,issue,bundle,structure,false,other,i);issue=TargetIssueResolver.next(issue).targetIssue;if(i%8==7)FixedResearchWindowR932.maintain(store);}
            for(int i=0;i<31;i++)FixedResearchWindowR932.maintain(store);
            check(scalar(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW+" WHERE state='PASS'").equals("2")&&scalar(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.PAIR).equals("360"),"AUTOMATIC_NEXT_240_IS_INDEPENDENT_OF_FIRST_120_AND_CAL");
            String count=scalar(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS);
            db.beginTransaction();try{ResearchMultiplicityR932.registerCandidate(db,"algorithm:renamed","family:cal",structure,issue);db.setTransactionSuccessful();}finally{db.endTransaction();}
            check(count.equals(scalar(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)),"RENAMING_PRESERVES_HYPOTHESIS_AND_ALPHA");
            boolean rejected=false;db.beginTransaction();try{ResearchMultiplicityR932.registerLook(db,"algorithm:renamed","RENAMED_WINDOW",1,issue,issue);}catch(IllegalStateException expected){rejected=true;}finally{db.endTransaction();}
            check(rejected,"RENAMED_CANDIDATE_CANNOT_REPEAT_CONSUMED_LOOK");
            rejected=false;db.beginTransaction();try{ResearchMultiplicityR932.registerCandidate(db,"algorithm:refamily","other-family",structure,issue);}catch(IllegalStateException expected){rejected=true;}finally{db.endTransaction();}
            check(rejected,"STATISTICAL_FAMILY_CANNOT_BE_REASSIGNED");
            rejected=false;try{db.execSQL("DELETE FROM "+ResearchMultiplicityR932.HYPOTHESIS);}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"ARCHIVE_CANNOT_DELETE_REGISTERED_DENOMINATOR");
            double total=Double.parseDouble(scalar(db,"SELECT SUM(alpha) FROM "+ResearchMultiplicityR932.FAMILY));
            check(total<.05&&scalar(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.LOOK+" WHERE state='PASS'").equals("2"),"GLOBAL_FAMILY_ALPHA_CONSERVED_AND_TWO_TERMINAL_LOOKS_DURABLE");
            freeze(db,issue,bundle,structure,false,other,0);issue=TargetIssueResolver.next(issue).targetIssue;
            AtomCaptureR932.schema(db);String atom=GenesisSchemaRegistryR915.atoms().get(0).id;
            db.execSQL("INSERT INTO "+AtomCaptureR932.QUALITY+" VALUES(?,1,0,0,0,0,0,?,'QUARANTINED','SYNTHETIC_PIT_FAILURE')",new Object[]{atom,issue});
            freeze(db,issue,bundle,structure,false,other,1);
            check(scalar(db,"SELECT state FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_no=3").equals("PAUSED"),"ATOM_DEPENDENCY_CHANGE_PAUSES_ACTIVE_WINDOW_WITHOUT_RESETTING_LOOK");
            System.out.println("R932_CALIBRATED_WINDOW_PASS|scope=SYNTHETIC_REAL_SQLITE_241_CAL_PLUS_120_PLUS_240_NOT_DEVICE_OR_REAL_FUTURE");
        }finally{db.shutdown();}
    }
}
