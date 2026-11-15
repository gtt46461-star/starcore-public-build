package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Four capture denominators. A FROZEN terminal is committed in the prediction's transaction. */
public final class ResearchCaptureLedgerR932 {
    public static final String TABLE="research_capture_opportunity_r932",EVENT="research_capture_event_r932";
    private ResearchCaptureLedgerR932() {}
    public static void schema(SQLiteDatabase db) {
        if(TransactionSchemaCacheR941.readyCommitted(db,"ResearchCaptureLedgerR932"))return;
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,system_available INTEGER NOT NULL,eligible INTEGER NOT NULL,selected INTEGER NOT NULL,execution_started INTEGER NOT NULL,frozen INTEGER NOT NULL,full1200 INTEGER NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,record_hash TEXT NOT NULL,cost_ms INTEGER NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(target_issue,candidate_key))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_capture_candidate ON "+TABLE+"(candidate_key,target_issue)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+EVENT+"(event_hash TEXT PRIMARY KEY,target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,record_hash TEXT NOT NULL,at_ms INTEGER NOT NULL)");
        TransactionSchemaCacheR941.markCommitted(db,"ResearchCaptureLedgerR932");
    }
    /** The roster transaction owns a new page: bulk insertion never overwrites later capture truth. */
    static void selectPage(SQLiteDatabase db,String target,int start,int end) {
        if(!db.inTransaction()||start<0||end<start||end-start>32)throw new IllegalArgumentException("R945_CAPTURE_PAGE");
        final String reason="R959_FROZEN_ADMISSION_PAGE";long now=System.currentTimeMillis();
        db.execSQL("INSERT OR IGNORE INTO "+TABLE+" SELECT target_issue,candidate_key,1,1,selected,0,0,0,CASE WHEN selected=1 THEN 'SELECTED' ELSE 'NOT_SELECTED' END,?,'',0,? FROM "+GlobalResearchScheduleR932.TASK+" WHERE target_issue=? AND queue_rank>=? AND queue_rank<?",new Object[]{reason,now,target,start,end});
        java.util.List<Object> args=new java.util.ArrayList<Object>();StringBuilder slots=new StringBuilder();
        try(Cursor c=db.rawQuery("SELECT candidate_key,selected FROM "+GlobalResearchScheduleR932.TASK+" WHERE target_issue=? AND queue_rank>=? AND queue_rank<? ORDER BY queue_rank",new String[]{target,String.valueOf(start),String.valueOf(end)})){
            while(c.moveToNext()){
                String key=c.getString(0),status=c.getInt(1)==1?"SELECTED":"NOT_SELECTED",hash=V51ChecksumEngine.sha256(target+'|'+key+'|'+status+'|'+reason+'|');
                if(slots.length()>0)slots.append(',');slots.append("(?,?,?,?,?,?,?)");
                java.util.Collections.addAll(args,hash,target,key,status,reason,"",Long.valueOf(now));
            }
        }
        if(slots.length()>0)db.execSQL("INSERT OR IGNORE INTO "+EVENT+" VALUES"+slots,args.toArray());
    }
    public static void record(DbStore store,final String target,final String candidate,final String status,final String reason,final long cost) {
        boolean ok=store.coordinatorR36().runAtomicWrite("R932_CAPTURE_STAGE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){record(db,target,candidate,status,reason,"",cost,false);}});
        if(!ok)throw new IllegalStateException("R932_CAPTURE_STAGE_COMMIT_FAILED");
    }
    /**
     * Persist one bounded owner-dispatch receipt for an already admitted SELECTED cohort before
     * expensive shared surface construction.  This only advances the honest attempted denominator:
     * it never creates roster membership, never freezes output and never satisfies completion gates.
     */
    public static int markDispatched(final DbStore store,final String target,final java.util.List<String> candidateKeys,final String owner) {
        if(store==null||target==null||target.length()==0||candidateKeys==null||candidateKeys.isEmpty())return 0;
        final java.util.ArrayList<String> keys=new java.util.ArrayList<String>();
        for(String key:candidateKeys){
            if(key!=null&&key.length()>0&&!keys.contains(key)){keys.add(key);if(keys.size()>=96)break;}
        }
        if(keys.isEmpty())return 0;
        Integer out=store.coordinatorR36().runAtomicWrite("R932_OWNER_DISPATCH",new DatabaseCoordinator.WriteCallable<Integer>(){public Integer call(SQLiteDatabase db){
            schema(db);int changed=0;
            for(String key:keys){
                int selected=0,frozen=0,started=0;
                try(Cursor c=db.rawQuery("SELECT selected,frozen,execution_started FROM "+TABLE+" WHERE target_issue=? AND candidate_key=?",new String[]{target,key})){
                    if(!c.moveToFirst())continue;selected=c.getInt(0);frozen=c.getInt(1);started=c.getInt(2);
                }
                if(selected!=1||frozen==1||started==1)continue;
                record(db,target,key,"EXECUTION_DISPATCHED","OWNER_ACCEPTED_BATCH_NOT_FROZEN|"+(owner==null?"":owner),"",0,false);
                changed++;
            }
            return Integer.valueOf(changed);
        }},Integer.valueOf(0));
        return out==null?0:out.intValue();
    }
    public static void record(SQLiteDatabase db,String target,String candidate,String status,String reason,String hash,long cost,boolean full1200) {
        if(target==null||target.length()==0||candidate==null||candidate.length()==0)throw new IllegalArgumentException("R932_CAPTURE_ID");
        boolean frozen="FROZEN".equals(status);
        if(frozen&&(!db.inTransaction()||hash==null||!hash.matches("[0-9a-f]{64}")))throw new IllegalArgumentException("R932_FROZEN_REQUIRES_TRANSACTION_AND_HASH");
        schema(db);long now=System.currentTimeMillis();
        try(Cursor c=db.rawQuery("SELECT frozen,record_hash FROM "+TABLE+" WHERE target_issue=? AND candidate_key=?",new String[]{target,candidate})) {
            if(c.moveToFirst()&&c.getInt(0)==1) {
                if(frozen&&!hash.equals(c.getString(1)))throw new IllegalStateException("R932_CAPTURE_FROZEN_CONFLICT");
                return;
            }
        }
        int available=status.startsWith("SYSTEM_")?0:1;
        int eligible="INELIGIBLE".equals(status)?0:1;
        int selected="NOT_SELECTED".equals(status)||"INELIGIBLE".equals(status)?0:1;
        int executed=frozen||status.startsWith("EXECUTION_")?1:0;
        db.execSQL("INSERT OR IGNORE INTO "+TABLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{target,candidate,available,eligible,selected,executed,0,0,status,reason,"",cost,now});
        db.execSQL("UPDATE "+TABLE+" SET system_available=MAX(system_available,?),eligible=MAX(eligible,?),selected=MAX(selected,?),execution_started=MAX(execution_started,?),frozen=?,full1200=?,status=?,reason=?,record_hash=?,cost_ms=MAX(cost_ms,?),updated_at=? WHERE target_issue=? AND candidate_key=? AND frozen=0",new Object[]{available,eligible,selected,executed,frozen?1:0,frozen&&full1200?1:0,status,reason,hash,Math.max(0,cost),now,target,candidate});
        String event=V51ChecksumEngine.sha256(target+'|'+candidate+'|'+status+'|'+reason+'|'+hash);
        db.execSQL("INSERT OR IGNORE INTO "+EVENT+" VALUES(?,?,?,?,?,?,?)",new Object[]{event,target,candidate,status,reason,hash,now});
    }
}
