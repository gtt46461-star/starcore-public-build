package com.ethk3.novaonepro.r952;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.V51ChecksumEngine;

public final class ResearchIntentLedgerR952 {
    private ResearchIntentLedgerR952() {}
    public static String put(SQLiteDatabase db,String problem,String parents,String deficit,String metrics,String operators,int budget,String createdIssue){
        String canonical="R952_INTENT_V1|"+safe(problem)+'|'+safe(parents)+'|'+safe(deficit)+'|'+safe(metrics)+'|'+safe(operators)+'|'+Math.max(1,budget)+'|'+safe(createdIssue);
        String hash=V51ChecksumEngine.sha256(canonical),id="INTENT-"+hash.substring(0,20);
        db.execSQL("INSERT OR IGNORE INTO r952_intent(intent_id,problem_type,parent_candidates,evidence_deficit,target_metrics,allowed_operators,budget,created_issue,intent_hash,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                new Object[]{id,safe(problem),safe(parents),safe(deficit),safe(metrics),safe(operators),Integer.valueOf(Math.max(1,budget)),safe(createdIssue),hash,Long.valueOf(System.currentTimeMillis())});
        return id;
    }
    private static String safe(String v){return v==null?"":v.trim();}
}
