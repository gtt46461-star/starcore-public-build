package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Real SQLite transaction/failure tests; the fixture prediction table is never production data. */
public final class CaptureTransactionHarnessR932 {
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println(name+"=PASS");}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){c.moveToFirst();return c.getLong(0);}}
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try {
            ResearchCaptureLedgerR932.schema(db);db.execSQL("CREATE TABLE fixture_prediction(target TEXT PRIMARY KEY,hash TEXT NOT NULL)");
            String h=V51ChecksumEngine.sha256("SYNTHETIC_CAPTURE");
            for(int failure=0;failure<3;failure++) {
                db.beginTransaction();try {
                    if(failure==0)throw new IllegalStateException("BEFORE_FREEZE");
                    db.execSQL("INSERT INTO fixture_prediction VALUES('200',?)",new Object[]{h});
                    if(failure==1)throw new IllegalStateException("AFTER_PREDICTION");
                    ResearchCaptureLedgerR932.record(db,"200","algorithm:fixture","FROZEN","TEST",h,1,true);
                    throw new IllegalStateException("AFTER_TERMINAL_BEFORE_COMMIT");
                } catch(IllegalStateException expected) {} finally{db.endTransaction();}
                check(count(db,"SELECT COUNT(*) FROM fixture_prediction")==0&&count(db,"SELECT COUNT(*) FROM "+ResearchCaptureLedgerR932.TABLE)==0&&count(db,"SELECT COUNT(*) FROM "+ResearchCaptureLedgerR932.EVENT)==0,"FAILURE_"+failure+"_ATOMIC_ROLLBACK");
            }
            db.beginTransaction();try{db.execSQL("INSERT INTO fixture_prediction VALUES('200',?)",new Object[]{h});ResearchCaptureLedgerR932.record(db,"200","algorithm:fixture","FROZEN","TEST",h,1,true);db.setTransactionSuccessful();}finally{db.endTransaction();}
            ResearchCaptureLedgerR932.record(db,"200","algorithm:fixture","EXECUTION_NO_OUTPUT","LATE_FAILURE","",2,false);
            check(count(db,"SELECT COUNT(*) FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE frozen=1 AND full1200=1")==1,"LATE_FAILURE_CANNOT_OVERWRITE_FROZEN");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchCaptureLedgerR932.EVENT)==1,"RETRY_NO_DUPLICATE_TERMINAL_EVENT");
            db.beginTransaction();boolean rejected=false;try{ResearchCaptureLedgerR932.record(db,"200","algorithm:fixture","FROZEN","TEST",V51ChecksumEngine.sha256("changed"),0,true);}catch(IllegalStateException expected){rejected=true;}finally{db.endTransaction();}
            check(rejected,"SAME_ISSUE_DIFFERENT_FREEZE_REJECTED");
            ResearchCaptureLedgerR932.record(db,"201","algorithm:fixture","NOT_SELECTED","FAMILY_QUOTA","",0,false);
            ResearchCaptureLedgerR932.record(db,"202","algorithm:fixture","SYSTEM_BUDGET_YIELD","P0","",0,false);
            check(count(db,"SELECT COUNT(*) FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE eligible=1")==3,"SYSTEM_YIELD_REMAINS_IN_ELIGIBLE_DENOMINATOR");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE execution_started=1")==1,"UNSCHEDULED_AND_SYSTEM_YIELD_NOT_EXECUTION_FAILURE");
            System.out.println("R932_CAPTURE_TRANSACTION_PASS|scope=REAL_SQLITE_SYNTHETIC_PREDICTIONS");
        }finally{db.shutdown();}
    }
}
