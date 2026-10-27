package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import java.util.Arrays;

/** Runs production retention SQL on SQLite; this is not an Android latency test. */
public final class WarmRetention {
    static long value(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){c.moveToFirst();return c.getLong(0);}}
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    static void candidate(SQLiteDatabase db,String id,String campaign,String text,int cursor,byte[] state,String status){
        db.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,cursor,state,status,operation) VALUES(?,?,?,?,?,?,'SYNTHETIC_RETENTION')",new Object[]{campaign,id,text,cursor,state,status});
    }
    public static void main(String[] args)throws Exception{
        try(SQLiteDatabase db=new SQLiteDatabase(args[0])){
            for(String ddl:RuntimeSchemaR26.DDL)db.execSQL(ddl);
            String abi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;byte[] learned={1,2,3};
            for(String id:new String[]{"expired","old","running"})db.execSQL("INSERT INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first) VALUES(?,'source',127,X'',?,1,?,0)",new Object[]{id,id.equals("running")?"RUNNING":"EXPIRED_RETENTION",id.equals("old")?"old-abi":abi});
            candidate(db,"trained","expired","cold",3,learned,"READY");candidate(db,"seeded","expired","_warm_kernel=seed",0,learned,"READY");
            candidate(db,"untrained","expired","cold",0,learned,"READY");candidate(db,"empty","expired","cold",3,new byte[0],"READY");candidate(db,"missing","expired","cold",0,null,"READY");
            candidate(db,"corrupt","expired","cold",1,learned,"BLOCKED_WARM_CHECKPOINT_PRESERVED");candidate(db,"obsolete","old","cold",3,learned,"READY");candidate(db,"active","running","cold",3,learned,"READY");
            try{WarmCandidateR26.preserveExpired(db);throw new AssertionError("Unfenced writer accepted");}catch(IllegalStateException expected){}
            db.beginTransaction();try{WarmCandidateR26.preserveExpired(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            check(value(db,"SELECT count(*) FROM r26_candidate")==5);check(value(db,"SELECT count(*) FROM r26_candidate WHERE status='WAITING_WARM_FORK'")==2);
            check(value(db,"SELECT count(*) FROM r26_candidate WHERE status='BLOCKED_WARM_CHECKPOINT_PRESERVED'")==1);check(value(db,"SELECT count(*) FROM r26_candidate WHERE status='BLOCKED_WARM_NUMERICAL_ABI_PRESERVED'")==1);check(value(db,"SELECT count(*) FROM r26_candidate WHERE policy_hash='active' AND status='READY'")==1);
            try(Cursor c=db.rawQuery("SELECT state FROM r26_candidate",null)){while(c.moveToNext())check(Arrays.equals(learned,c.getBlob(0)));}
            db.beginTransaction();try{WarmCandidateR26.preserveExpired(db);db.setTransactionSuccessful();}finally{db.endTransaction();}check(value(db,"SELECT count(*) FROM r26_candidate")==5);
            System.out.println("R26_WARM_RETENTION_PASS|cases=8|unchangedLearnedBytes=true|idempotent=true|scope=HOST_SQLITE_PRODUCTION_SQL_NOT_DEVICE");
        }
    }
}
