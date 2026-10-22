package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;

/** P1 compact selected-action label. Training no longer waits for 1200 deferred action labels. */
public final class ResearchCompactLabelR906 {
    private ResearchCompactLabelR906(){}
    public static Result captureProduction(final DbStore store,final String target){
        if(store==null||!TargetIssueResolver.isValid(target))return Result.fail("INVALID_INPUT");
        Boolean ok=store.coordinatorR36().runWrite("R906_COMPACT_LABEL",new DatabaseCoordinator.WriteCallable<Boolean>(){
            public Boolean call(SQLiteDatabase db){return Boolean.valueOf(captureWithinWrite(db,target));}},Boolean.FALSE);
        return ok.booleanValue()?new Result(true,"COMMITTED",target):Result.fail("NOT_ELIGIBLE_OR_CONFLICT");
    }
    private static boolean captureWithinWrite(SQLiteDatabase db,String target){
        Cursor c=null;try{
            c=db.rawQuery("SELECT o.selected_hit_p,o.actual_number,o.hit_label,o.record_hash,o.prediction_created_at,o.settled_at,"
                    +"e.action_ordinal,e.raw_action_p_value,e.rank_score_value,e.third_margin_value,e.entropy_risk_value,e.stability_value,e.window_consensus_value,e.assignment_support_value,"
                    +"e.raw_action_p_status,e.rank_status,e.third_margin_status,e.entropy_risk_status,e.stability_status,e.window_consensus_status,e.assignment_support_status,e.record_hash,t.draw_hash,s.record_hash "
                    +"FROM "+DbHelper.TABLE_R35_OOS_LABEL+" o"
                    +" JOIN "+DbHelper.TABLE_R906_SELECTED_EVIDENCE+" e ON e.target_issue=o.target_issue"
                    +" JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t ON t.target_issue=o.target_issue"
                    +" JOIN "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s ON s.target_issue=o.target_issue "
                    +"WHERE o.target_issue=? AND o.strict_oos=1 AND t.status='COMPLETED'"
                    +" AND s.pre_draw_frozen=1 AND s.settled=1 LIMIT 1",new String[]{target});
            if(!c.moveToFirst())return false;for(int i=14;i<=20;i++)if(!"AVAILABLE".equals(safe(c.getString(i))))return false;
            String featureText=c.getDouble(7)+","+c.getDouble(8)+","+c.getDouble(9)+","+c.getDouble(10)+","+c.getDouble(11)+","+c.getDouble(12)+","+c.getDouble(13);
            byte[] blob=featureText.getBytes(StandardCharsets.UTF_8);String featureHash=V51ChecksumEngine.sha256("R906_COMPACT_FEATURE|"+featureText);String predraw=safe(c.getString(23));String drawHash=safe(c.getString(22));
            String outcomeHash=V51ChecksumEngine.sha256("R906_COMPACT_OUTCOME|"+target+'|'+featureHash+'|'+c.getInt(1)+'|'+c.getInt(2)+'|'+safe(c.getString(3)));
            Cursor old=null;try{old=db.rawQuery("SELECT outcome_hash FROM "+DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL+" WHERE asset_id='PRODUCTION_SELECTED' AND target_issue=? LIMIT 1",new String[]{target});if(old.moveToFirst())return outcomeHash.equals(safe(old.getString(0)));}finally{if(old!=null)old.close();}
            ContentValues v=new ContentValues();v.put("asset_id","PRODUCTION_SELECTED");v.put("target_issue",target);v.put("selected_ordinal",c.getInt(6));v.put("feature_schema_hash",AppConfig.R906_COMPACT_FEATURE_SCHEMA);v.put("feature_blob",blob);v.put("feature_hash",featureHash);v.put("predicted_probability",c.getDouble(0));v.put("actual_number",c.getInt(1));v.put("hit_label",c.getInt(2));v.put("predraw_hash",predraw);v.put("draw_hash",drawHash);v.put("outcome_hash",outcomeHash);v.put("frozen_at",c.getLong(4));v.put("settled_at",c.getLong(5));return db.insertOrThrow(DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL,null,v)>0L;
        }finally{if(c!=null)c.close();}
    }
    public static String nextAfter(DbStore store,String trainedUntil){if(store==null)return "";Cursor c=null;try{c=store.readable().rawQuery("SELECT target_issue FROM "+DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL+" WHERE asset_id='PRODUCTION_SELECTED' AND target_issue>? ORDER BY target_issue ASC LIMIT 1",new String[]{safe(trainedUntil)});return c.moveToFirst()?safe(c.getString(0)):"";}finally{if(c!=null)c.close();}}
    public static final class Result{public final boolean success;public final String status,targetIssue;Result(boolean ok,String s,String t){success=ok;status=safe(s);targetIssue=safe(t);}static Result fail(String s){return new Result(false,s,"");}}
    private static String safe(String x){return x==null?"":x.trim();}
}
