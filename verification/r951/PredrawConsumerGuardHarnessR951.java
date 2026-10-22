package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
public final class PredrawConsumerGuardHarnessR951 {
    private static void require(boolean ok,String code){if(!ok)throw new IllegalStateException("R951_FAIL:"+code);}
    public static void main(String[] args)throws Exception{
        require(args.length>0,"BRIDGE");
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try{
            DbStore store=new DbStore(db);
            String target="202609140001";
            String input="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
            String payload="[]";
            String payloadHash=V51ChecksumEngine.sha256(payload);
            db.execSQL("CREATE TABLE "+GlobalResearchScheduleR932.PLAN+"(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,roster_hash TEXT NOT NULL,created_at INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE "+StagedResearchRosterR943.TABLE+"(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,payload TEXT NOT NULL,payload_hash TEXT NOT NULL,cursor INTEGER NOT NULL,created_at INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE "+StagedResearchRosterR943.MANIFEST+"(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,payload_hash TEXT NOT NULL,candidate_count INTEGER NOT NULL,created_at INTEGER NOT NULL)");
            db.execSQL("INSERT INTO "+StagedResearchRosterR943.TABLE+" VALUES(?,?,?,?,?,?)",new Object[]{target,input,payload,payloadHash,32,1});
            db.execSQL("INSERT INTO "+StagedResearchRosterR943.MANIFEST+" VALUES(?,?,?,?,?)",new Object[]{target,input,payloadHash,64,1});
            GlobalResearchScheduleR932.prepareConsumer(store,target,input);
            try(Cursor c=db.rawQuery("SELECT cursor FROM "+StagedResearchRosterR943.TABLE+" WHERE target_issue=?",new String[]{target})){
                require(c.moveToFirst()&&c.getInt(0)==32,"PARTIAL_PREFIX_ADVANCED_BY_CONSUMER");
            }
            db.execSQL("INSERT INTO "+GlobalResearchScheduleR932.PLAN+" VALUES(?,?,?,?)",new Object[]{"202609140002",input,"r",1});
            GlobalResearchScheduleR932.prepareConsumer(store,"202609140002",input);
            boolean drift=false;
            try{GlobalResearchScheduleR932.prepareConsumer(store,"202609140002","bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");}
            catch(IllegalStateException expected){drift=expected.getMessage()!=null&&expected.getMessage().contains("R951_ROSTER_INPUT_DRIFT");}
            require(drift,"FINAL_PLAN_DRIFT_NOT_REJECTED");
            System.out.println("R951_PREDRAW_CONSUMER_GUARD_PASS|partial_cursor=32|consumer_advance=0|plan_fast_path=PASS|input_drift=REJECT");
        }finally{db.shutdown();}
    }
}
