package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Read only a committed original terminal; project its selected-action status in a separate realtime
 * transaction. This bridge cannot create a canonical prediction, terminal task or OOS label. */
public final class TerminalProjectionR29 {
    private TerminalProjectionR29(){}
    public static final class Snapshot {
        public final Models.Draw draw;public final IssueClosureContractR906.Receipt receipt;
        public final String originalStatus,errorCode;
        Snapshot(Models.Draw draw,IssueClosureContractR906.Receipt receipt,String status,String error){this.draw=draw;this.receipt=receipt;originalStatus=status;errorCode=error;}
    }
    public static Snapshot capture(SQLiteDatabase realtime,String issue){
        if(realtime==null||!TargetIssueResolver.isValid(issue))return null;
        if(com.ethk3.novaonepro.strategyv2.ResearchRouteR29.isResearchDatabase(realtime))throw new IllegalStateException("R29_ROOT_TERMINAL_AUTHORITY_REQUIRED");
        String status,hash,error;long completed;
        try(Cursor c=realtime.rawQuery("SELECT status,settlement_hash,terminal_code,last_error,completed_at FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue=?",new String[]{issue})){
            if(!c.moveToFirst())return null;status=safe(c.getString(0));hash=safe(c.getString(1));error=safe(c.getString(2));if(error.isEmpty())error=safe(c.getString(3));completed=c.getLong(4);
        }
        if(!status.equals(SettlementTaskStoreR36.COMPLETED)&&!status.equals(SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL)&&!status.equals(SettlementTaskStoreR36.FAILED_TERMINAL)&&!status.equals(SettlementTaskStoreR36.INVALID_DRAW_TERMINAL))return null;
        if(completed<=0)throw new IllegalStateException("R29_REALTIME_TERMINAL_CLOCK_MISSING");
        Models.Draw draw=new Models.Draw();draw.issue=issue;draw.numbers=new int[10];
        try(Cursor c=realtime.rawQuery("SELECT n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,open_time,fetch_time FROM "+DbHelper.TABLE_DRAWS+" WHERE issue=? AND dirty=0",new String[]{issue})){
            if(!c.moveToFirst())return null;for(int i=0;i<10;i++)draw.numbers[i]=c.getInt(i);draw.openTime=c.getLong(10);draw.fetchTime=c.getLong(11);
        }
        if(!draw.isValid())return null;String predraw="",oos="";int actual=0,hit=-1;
        IssueClosureContractR906.Eligibility eligibility;IssueClosureContractR906.CoreStatus core;
        if(status.equals(SettlementTaskStoreR36.COMPLETED)){
            try(Cursor c=realtime.rawQuery("SELECT o.record_hash,o.settled,o.actual_number,o.hit_label,l.record_hash,l.strict_oos,o.pre_draw_frozen FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" o JOIN "+DbHelper.TABLE_R35_OOS_LABEL+" l ON l.target_issue=o.target_issue WHERE o.target_issue=?",new String[]{issue})){
                if(!c.moveToFirst()||c.getInt(1)!=1||c.getInt(5)!=1||c.getInt(6)!=1)return null;predraw=safe(c.getString(0));actual=c.getInt(2);hit=c.getInt(3);oos=safe(c.getString(4));
            }
            if(predraw.length()!=64||oos.length()!=64||!AppConfig.isValidNumber(actual)||(hit!=0&&hit!=1))return null;
            eligibility=IssueClosureContractR906.Eligibility.ELIGIBLE_PREDRAW;core=IssueClosureContractR906.CoreStatus.COMPLETED;
        }else if(status.equals(SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL)){eligibility=IssueClosureContractR906.Eligibility.EXCLUDED_NO_PREDRAW;core=IssueClosureContractR906.CoreStatus.EXCLUDED;}
        else{eligibility=IssueClosureContractR906.Eligibility.BLOCKED_DATA_ERROR;core=IssueClosureContractR906.CoreStatus.BLOCKED;}
        return new Snapshot(draw,new IssueClosureContractR906.Receipt(issue,DrawHashR36.sha256V2(draw),predraw,oos,hash,eligibility,core,actual,hit,completed),status,error);
    }
    public static void requireSame(SQLiteDatabase realtime,Snapshot captured){
        Snapshot current=capture(realtime,captured.receipt.targetIssue);
        if(current==null||!current.receipt.recordHash.equals(captured.receipt.recordHash)||!current.originalStatus.equals(captured.originalStatus))throw new IllegalStateException("R29_REALTIME_TERMINAL_CHANGED_RETRY");
    }
    /** Called by LegacyResearchScopeR29.realtimeWrite after all research locks have been released. */
    public static int projectSelected(SQLiteDatabase realtime,Snapshot captured){
        if(!realtime.inTransaction())throw new IllegalStateException("R29_SELECTED_PROJECTION_REALTIME_TRANSACTION");requireSame(realtime,captured);
        if(!captured.receipt.countsAsSettledPrediction())return 0;String result=captured.receipt.hitLabel==1?"HIT":"MISS";
        try(Cursor c=realtime.rawQuery("SELECT settled,result,settlement_hash FROM "+V51Repository.SELECTED+" WHERE target_issue=?",new String[]{captured.receipt.targetIssue})){
            if(!c.moveToFirst())return 0;
            if(c.getInt(0)==1){if(!result.equals(c.getString(1))||!originalSelectedHash(realtime,captured,c.getString(2)))throw new IllegalStateException("R29_SELECTED_TERMINAL_CONFLICT");return 0;}
        }
        realtime.execSQL("UPDATE "+V51Repository.SELECTED+" SET settled=1,result=?,settlement_hash=? WHERE target_issue=? AND settled=0",new Object[]{result,captured.receipt.oosHash,captured.receipt.targetIssue});
        try(Cursor c=realtime.rawQuery("SELECT changes()",null)){return c.moveToFirst()?c.getInt(0):0;}
    }
    private static boolean originalSelectedHash(SQLiteDatabase realtime,Snapshot captured,String hash){
        if(captured.receipt.oosHash.equals(hash))return true;
        try(Cursor exists=realtime.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{DbHelper.TABLE_B03_SETTLEMENT_EVENT})){
            if(!exists.moveToFirst())return false;
        }
        try(Cursor c=realtime.rawQuery("SELECT settlement_hash FROM "+DbHelper.TABLE_B03_SETTLEMENT_EVENT+" WHERE target_issue=?",new String[]{captured.receipt.targetIssue})){while(c.moveToNext())if(safe(hash).equals(c.getString(0)))return true;}
        return false;
    }
    private static String safe(String value){return value==null?"":value.trim();}
}
