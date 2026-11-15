package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** One immutable business classification per exact draw. This is the settlement denominator owner in R906. */
public final class IssueClosureLedgerR906 {
    private IssueClosureLedgerR906() {}

    public static IssueClosureContractR906.Receipt recordWithinTransaction(
            SQLiteDatabase db, Models.Draw draw, OosAfterDrawWriterR36.LabelResult oos,
            String realHash, String settlementHash, String previousTaskStatus, long now) {
        if (db == null || !db.inTransaction() || draw == null || !draw.isValid())
            throw new IllegalStateException("R906_CLOSURE_TX_REQUIRED");
        String target=safe(draw.issue), drawHash=DrawHashR36.sha256V2(draw);
        String predraw=oos==null?"":safe(oos.canonicalHash);
        IssueClosureContractR906.Eligibility eligibility;
        IssueClosureContractR906.CoreStatus status;
        String code="";
        int hit=-1, actual=0;
        String oosHash="";
        if (oos != null && oos.success) {
            eligibility=IssueClosureContractR906.Eligibility.ELIGIBLE_PREDRAW;
            status=IssueClosureContractR906.CoreStatus.COMPLETED;
            hit=oos.hitLabel; oosHash=safe(oos.recordHash);
            actual=actualFromOos(db,target);
        } else if (oos != null && "FROZEN_OBSERVATION_MISSING".equals(oos.status)) {
            eligibility=IssueClosureContractR906.Eligibility.EXCLUDED_NO_PREDRAW;
            status=IssueClosureContractR906.CoreStatus.EXCLUDED;
            code=oos.status;
        } else {
            eligibility=IssueClosureContractR906.Eligibility.BLOCKED_DATA_ERROR;
            status=IssueClosureContractR906.CoreStatus.BLOCKED;
            code=oos==null?"OOS_NULL":safe(oos.status);
        }
        long completedAt=status==IssueClosureContractR906.CoreStatus.COMPLETED||status==IssueClosureContractR906.CoreStatus.EXCLUDED?now:0L;
        IssueClosureContractR906.Receipt typed = new IssueClosureContractR906.Receipt(
                target, drawHash, predraw, oosHash, safe(settlementHash), eligibility, status, actual, hit, completedAt);
        String recordHash=typed.recordHash;
        verifyFirstWriteCompatible(db,target,drawHash,predraw,eligibility,status,oosHash,recordHash);
        ContentValues cv=new ContentValues();
        cv.put("target_issue",target); cv.put("draw_hash",drawHash); cv.put("predraw_record_hash",predraw);
        cv.put("eligibility",eligibility.name()); cv.put("core_status",status.name()); cv.put("actual_number",actual);
        cv.put("hit_label",hit); cv.put("oos_record_hash",oosHash); cv.put("real_record_hash",safe(realHash));
        cv.put("settlement_hash",safe(settlementHash)); cv.put("previous_task_status",safe(previousTaskStatus));
        cv.put("exclusion_or_error_code",code); cv.put("classified_at",now);
        cv.put("completed_at",completedAt);
        cv.put("record_hash",recordHash);
        db.insertWithOnConflict(DbHelper.TABLE_R906_ISSUE_CLOSURE,null,cv,SQLiteDatabase.CONFLICT_REPLACE);
        return readReceipt(db,target);
    }

    public static IssueClosureContractR906.Receipt read(DbStore store,String target){
        if(store==null) return null; return readReceipt(store.readable(),safe(target));
    }

    public static boolean businessClosed(DbStore store,String target){
        IssueClosureContractR906.Receipt r=read(store,target);
        return r!=null && (r.status==IssueClosureContractR906.CoreStatus.COMPLETED || r.status==IssueClosureContractR906.CoreStatus.EXCLUDED);
    }

    public static boolean countsAsSettledPrediction(DbStore store,String target){
        IssueClosureContractR906.Receipt r=read(store,target); return r!=null && r.countsAsSettledPrediction();
    }

    public static String errorCode(DbStore store,String target){
        if(store==null)return ""; Cursor c=null; try{
            c=store.readable().rawQuery("SELECT exclusion_or_error_code FROM "+DbHelper.TABLE_R906_ISSUE_CLOSURE+" WHERE target_issue=? LIMIT 1",new String[]{safe(target)});
            return c.moveToFirst()?safe(c.getString(0)):"";
        } finally {if(c!=null)c.close();}
    }

    public static SettlementCoverageTruthR906 coverage(DbStore store){
        if(store==null) return new SettlementCoverageTruthR906(0,0,0,0,0);
        SQLiteDatabase db=store.readable();
        int eligible=scalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION
                +" s WHERE s.pre_draw_frozen=1 AND EXISTS(SELECT 1 FROM "+DrawDependencyInputCP12.SOURCE
                +" d WHERE d.issue=s.target_issue AND d.dirty=0)");
        int settled=scalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION
                +" s WHERE s.pre_draw_frozen=1 AND s.settled=1 AND EXISTS(SELECT 1 FROM "
                +DbHelper.TABLE_R35_OOS_LABEL+" o WHERE o.target_issue=s.target_issue AND o.strict_oos=1)");
        int excluded=scalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK
                +" WHERE status='NO_PREDRAW_CANONICAL_TERMINAL'");
        int blocked=scalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK
                +" WHERE status IN ('FAILED_TERMINAL','INVALID_DRAW_TERMINAL')");
        int taskTotal=scalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK
                +" t WHERE EXISTS(SELECT 1 FROM "+DrawDependencyInputCP12.SOURCE
                +" d WHERE d.issue=t.target_issue AND d.dirty=0)");
        int taskTerminal=scalar(db,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK
                +" t WHERE t.status IN ('COMPLETED','NO_PREDRAW_CANONICAL_TERMINAL',"
                +"'FAILED_TERMINAL','INVALID_DRAW_TERMINAL') AND EXISTS(SELECT 1 FROM "
                +DrawDependencyInputCP12.SOURCE+" d WHERE d.issue=t.target_issue AND d.dirty=0)");
        return new SettlementCoverageTruthR906(
                eligible,settled,excluded,blocked,Math.max(0,eligible-settled),
                taskTotal,taskTerminal,Math.max(0,taskTotal-taskTerminal));
    }

    private static int scalar(SQLiteDatabase db,String sql){Cursor c=null;try{c=db.rawQuery(sql,null);return c.moveToFirst()?c.getInt(0):0;}finally{if(c!=null)c.close();}}

    private static IssueClosureContractR906.Receipt readReceipt(SQLiteDatabase db,String target){
        Cursor c=null; try{
            c=db.rawQuery("SELECT eligibility,core_status,draw_hash,predraw_record_hash,oos_record_hash,real_record_hash,settlement_hash,exclusion_or_error_code,classified_at,completed_at,record_hash,actual_number,hit_label FROM "+DbHelper.TABLE_R906_ISSUE_CLOSURE+" WHERE target_issue=? LIMIT 1",new String[]{target});
            if(!c.moveToFirst()) return null;
            IssueClosureContractR906.Receipt r = new IssueClosureContractR906.Receipt(
                    target, c.getString(2), c.getString(3), c.getString(4), c.getString(6),
                    IssueClosureContractR906.Eligibility.valueOf(c.getString(0)),
                    IssueClosureContractR906.CoreStatus.valueOf(c.getString(1)),
                    c.getInt(11), c.getInt(12), c.getLong(9));
            if (!r.recordHash.equals(safe(c.getString(10))))
                throw new IllegalStateException("R906_CLOSURE_RECEIPT_HASH_MISMATCH");
            return r;
        } finally {if(c!=null)c.close();}
    }

    private static void verifyFirstWriteCompatible(SQLiteDatabase db,String target,String drawHash,String predraw,
            IssueClosureContractR906.Eligibility eligibility,IssueClosureContractR906.CoreStatus status,String oosHash,String recordHash){
        Cursor c=null; try{
            c=db.rawQuery("SELECT draw_hash,predraw_record_hash,eligibility,core_status,oos_record_hash,record_hash FROM "+DbHelper.TABLE_R906_ISSUE_CLOSURE+" WHERE target_issue=? LIMIT 1",new String[]{target});
            if(!c.moveToFirst())return;
            if(!drawHash.equals(safe(c.getString(0)))) throw new IllegalStateException("R906_CLOSURE_DRAW_FIRST_WRITE_CONFLICT");
            String oldPred=safe(c.getString(1)); if(oldPred.length()>0 && predraw.length()>0 && !oldPred.equals(predraw))throw new IllegalStateException("R906_CLOSURE_PREDRAW_FIRST_WRITE_CONFLICT");
            String oldOos=safe(c.getString(4)); if(oldOos.length()>0 && oosHash.length()>0 && !oldOos.equals(oosHash))throw new IllegalStateException("R906_CLOSURE_OOS_FIRST_WRITE_CONFLICT");
            // Exact replay is accepted. A prior EXCLUDED may be upgraded only when immutable predraw evidence proves eligibility.
            if(recordHash.equals(safe(c.getString(5))))return;
            if(IssueClosureContractR906.CoreStatus.COMPLETED.name().equals(safe(c.getString(3))) && status!=IssueClosureContractR906.CoreStatus.COMPLETED)
                throw new IllegalStateException("R906_CLOSURE_COMPLETED_REGRESSION");
        } finally {if(c!=null)c.close();}
    }

    private static int actualFromOos(SQLiteDatabase db,String target){Cursor c=null;try{c=db.rawQuery("SELECT actual_number FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE target_issue=? LIMIT 1",new String[]{target});return c.moveToFirst()?c.getInt(0):0;}finally{if(c!=null)c.close();}}
    private static String safe(String x){return x==null?"":x.trim();}
}
