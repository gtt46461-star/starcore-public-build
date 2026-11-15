package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.DrawDependencyInputCP12;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.TargetIssueResolver;
import com.ethk3.novaonepro.V51ChecksumEngine;

/** Immutable settlement linkage for the R952 research snapshot; never rewrites a predraw input. */
public final class FeedbackMatrixRegistryR952 {
    private FeedbackMatrixRegistryR952(){}
    public static String postDraw(SQLiteDatabase db,String issue,String settlementHash){
        if(db==null||!db.inTransaction())throw new IllegalStateException("R958_FEEDBACK_TRANSACTION_REQUIRED");
        if(!TargetIssueResolver.isValid(issue)||settlementHash==null||!settlementHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R958_FEEDBACK_IDENTITY");
        String snapshot;long frozen;
        try(Cursor c=db.rawQuery("SELECT snapshot_hash,feature_hash,payload_hash,state,created_at FROM r952_matrix_snapshot WHERE target_issue=?",new String[]{issue})){
            if(!c.moveToFirst())throw new IllegalStateException("R958_FEEDBACK_PREDRAW_MISSING");
            snapshot=c.getString(0);frozen=c.getLong(4);
            if(!"PREDRAW_FROZEN".equals(c.getString(3))||!snapshot.equals(V51ChecksumEngine.sha256("MATRIX1|"+issue+'|'+c.getString(1)+'|'+c.getString(2))))throw new IllegalStateException("R958_FEEDBACK_FROZEN_PAYLOAD_DRIFT");
        }
        long completed;
        try(Cursor c=db.rawQuery("SELECT settlement_hash,completed_at,status FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue=?",new String[]{issue})){
            if(!c.moveToFirst()||!"COMPLETED".equals(c.getString(2))||!settlementHash.equals(c.getString(0)))throw new IllegalStateException("R958_FEEDBACK_TERMINAL_REQUIRED");
            completed=c.getLong(1);
        }
        StringBuilder numbers=new StringBuilder();long opened;boolean[] seen=new boolean[11];
        try(Cursor c=db.rawQuery("SELECT n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,open_time,dirty FROM "+DrawDependencyInputCP12.SOURCE+" WHERE issue=?",new String[]{issue})){
            if(!c.moveToFirst()||c.getInt(11)!=0)throw new IllegalStateException("R958_FEEDBACK_TRUTH_MISSING");opened=c.getLong(10);
            for(int i=0;i<10;i++){int value=c.getInt(i);if(value<1||value>10||seen[value])throw new IllegalStateException("R958_FEEDBACK_TRUTH_INVALID");seen[value]=true;if(i>0)numbers.append(',');numbers.append(value);}
        }
        if(frozen<=0||opened<=frozen||completed<opened)throw new IllegalStateException("R958_FEEDBACK_TEMPORAL_PROOF");
        String next=TargetIssueResolver.next(issue).targetIssue;
        String label=V51ChecksumEngine.sha256("R958_MATRIX_LABEL|"+issue+'|'+numbers+'|'+settlementHash);
        String hash=V51ChecksumEngine.sha256("R958_MATRIX_FEEDBACK_V2|"+snapshot+'|'+label+'|'+next);
        db.execSQL("INSERT OR IGNORE INTO r952_matrix_feedback(target_issue,snapshot_hash,settlement_hash,label_hash,effective_from_issue,actual_numbers,feedback_hash,frozen_at,completed_at) VALUES(?,?,?,?,?,?,?,?,?)",
                new Object[]{issue,snapshot,settlementHash,label,next,numbers.toString(),hash,frozen,completed});
        try(Cursor c=db.rawQuery("SELECT feedback_hash,completed_at FROM r952_matrix_feedback WHERE target_issue=?",new String[]{issue})){
            if(!c.moveToFirst()||!hash.equals(c.getString(0))||completed!=c.getLong(1))throw new IllegalStateException("R958_FEEDBACK_REPLAY_DRIFT");
        }
        return hash;
    }
}
