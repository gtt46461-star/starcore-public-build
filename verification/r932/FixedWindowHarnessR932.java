package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class FixedWindowHarnessR932 {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println(label+"=PASS");}
    static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try {
            DbStore store=new DbStore(db);FixedResearchWindowR932.schema(db);
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER)");
            String hash=V51ChecksumEngine.sha256("SYNTHETIC_FIXED_WINDOW"),first="202609100001",issue=first;
            db.beginTransaction();try{ResearchMultiplicityR932.registerCandidate(db,"algorithm:fixture","algorithm:fixture-family",hash,first);db.setTransactionSuccessful();}finally{db.endTransaction();}
            for(int i=0;i<120;i++) {
                String pred=V51ChecksumEngine.sha256(issue+"BASE"),candidate=V51ChecksumEngine.sha256(issue+"CANDIDATE");
                db.beginTransaction();try {
                    db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{issue,hash,hash,hash,1,2,3,4,pred,1});
                    FixedResearchWindowR932.freezePair(db,"algorithm:fixture",hash,issue,candidate,1,1,2,3);
                    FixedResearchWindowR932.freezePair(db,"algorithm:fixture",hash,issue,candidate,1,1,2,3);
                    db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,1,2,3,4,5,6,7,8,9,10,0)",new Object[]{issue});
                if(i==0){FixedResearchWindowR932.maintain(store);check(scalar(db,"SELECT state FROM "+FixedResearchWindowR932.WINDOW).equals("OPEN"),"EARLY_WIN_DOES_NOT_CLOSE_WINDOW");}
                issue=TargetIssueResolver.next(issue).targetIssue;
            }
            check(scalar(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.PAIR).equals("120"),"DUPLICATE_FREEZE_COUNTS_ONE_PAIR_PER_ISSUE");
            for(int i=0;i<16;i++)FixedResearchWindowR932.maintain(store);
            check(scalar(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.PAIR+" WHERE candidate_hit=1 AND champion_hit=0").equals("120"),"FIXED_CHAMPION_EXACT_PAIRED_LABELS");
            check(scalar(db,"SELECT state FROM "+FixedResearchWindowR932.WINDOW).equals("FAIL_PROBABILITY_CONTRACT"),"RAW_SCORES_CANNOT_PASS_PROBABILITY_WINDOW");
            String result=scalar(db,"SELECT result_hash FROM "+FixedResearchWindowR932.WINDOW);FixedResearchWindowR932.maintain(store);
            check(result.equals(scalar(db,"SELECT result_hash FROM "+FixedResearchWindowR932.WINDOW)),"CLOSED_ENDPOINT_NOT_REEVALUATED");
            boolean rejected=false;db.beginTransaction();try{FixedResearchWindowR932.freezePair(db,"algorithm:fixture",hash,first,hash,1,1,2,3);}catch(IllegalStateException expected){rejected=true;}finally{db.endTransaction();}
            check(rejected,"POSTDRAW_NEW_PAIR_REJECTED");
            rejected=false;db.beginTransaction();try{FixedResearchWindowR932.register(db,"algorithm:fixture",hash,hash,hash,issue,120,1);}catch(IllegalStateException expected){rejected=true;}finally{db.endTransaction();}
            check(rejected,"FAILED_WINDOW_CANNOT_RESET_SAME_TRIAL");
            System.out.println("R932_FIXED_WINDOW_PASS|scope=SYNTHETIC_PREDRAW_REAL_SQLITE_120_ISSUES");
        }finally{db.shutdown();}
    }
}
