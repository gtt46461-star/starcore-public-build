package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Durable P1/P2 side-effect outbox. Core settlement commits do not wait for Research or 1200-action materialization. */
public final class SettlementOutboxR906 {
    public static final String PENDING="PENDING",RUNNING="RUNNING",DONE="DONE",RETRY_WAIT="RETRY_WAIT";
    private SettlementOutboxR906(){}
    public static void enqueueWithinTransaction(SQLiteDatabase db,String type,String target,String subject,String payloadHash,int priority,long now){
        if(db==null||!db.inTransaction())throw new IllegalStateException("R906_OUTBOX_TX_REQUIRED");
        String event=V51ChecksumEngine.sha256("R906_OUTBOX|"+safe(type)+'|'+safe(target)+'|'+safe(subject)+'|'+safe(payloadHash));
        ContentValues cv=new ContentValues(); cv.put("event_id",event);cv.put("event_type",safe(type));cv.put("target_issue",safe(target));cv.put("subject_id",safe(subject));cv.put("payload_hash",safe(payloadHash));cv.put("status",PENDING);cv.put("priority",priority);cv.put("attempts",0);cv.put("next_attempt_at",0L);cv.put("lease_owner","");cv.put("lease_generation",0);cv.put("process_boot_id","");cv.put("lease_until",0L);cv.put("last_progress_at",now);cv.put("last_error_code","");cv.put("created_at",now);cv.put("updated_at",now);cv.put("completed_at",0L);
        db.insertWithOnConflict(DbHelper.TABLE_R906_SETTLEMENT_OUTBOX,null,cv,SQLiteDatabase.CONFLICT_IGNORE);
    }
    public static int pendingCount(DbStore store){if(store==null)return 0;Cursor c=null;try{c=store.readable().rawQuery("SELECT COUNT(*) FROM "+DbHelper.TABLE_R906_SETTLEMENT_OUTBOX+" WHERE status IN ('PENDING','RETRY_WAIT','RUNNING')",null);return c.moveToFirst()?c.getInt(0):0;}finally{if(c!=null)c.close();}}
    private static String safe(String x){return x==null?"":x.trim();}
}
