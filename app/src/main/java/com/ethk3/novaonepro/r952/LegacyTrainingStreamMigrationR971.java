package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;

/** Explicit R968 TRAIN_STREAM retirement, not conversion into V2 evidence or a model update.
 * Older releases emitted TRAIN_STREAM rows that the V2 consumer deliberately rejects. Leaving
 * those rows RETRYABLE forever blocks the chronological learner and prospective enrollment.
 * Only matching original run/input/predraw receipts are retired; damaged provenance stays blocked.
 */
public final class LegacyTrainingStreamMigrationR971 {
    private LegacyTrainingStreamMigrationR971() {}
    private static boolean exists(SQLiteDatabase db,String name){
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}
    }
    public static int retireBatch(SQLiteDatabase db,int max,long now) {
        if(db==null||!db.inTransaction())throw new IllegalStateException("R971_MIGRATION_TRANSACTION_REQUIRED");
        if(!exists(db,"research_training_run_r967")||!exists(db,"research_training_input_r967"))return 0;
        // Most current releases have no trainable legacy receipt. An indexed existence check
        // avoids rejoining every historical input before EACH numerical update. A matching old
        // record still takes the original provenance-checked retirement path; no blanket ACK.
        try(Cursor c=db.rawQuery("SELECT 1 FROM r952_candidate_predraw p WHERE p.evidence_role='TRAIN_STREAM' AND ((p.settled=0 AND p.proof_error='') OR EXISTS(SELECT 1 FROM r952_research_outbox o WHERE o.candidate_id=p.candidate_id AND o.target_issue=p.target_issue AND o.state NOT IN ('COMPLETED','DEPENDENCY_RETIRED'))) LIMIT 1",null)) {
            if(!c.moveToFirst())return 0;
        }
        int limit=Math.max(1,Math.min(32,max));
        String joined=" FROM r952_candidate_predraw p JOIN research_training_input_r967 i ON i.learner_id=p.candidate_id AND i.target_issue=p.target_issue"
                +" JOIN research_training_run_r967 r ON r.run_hash=i.run_hash AND r.learner_id=p.candidate_id"
                +" JOIN r952_candidate c ON c.candidate_id=p.candidate_id AND c.structural_hash=p.structure_hash";
        String receipt="p.evidence_role='TRAIN_STREAM' AND i.role='TRAIN_STREAM' AND i.input_hash=p.input_hash AND i.model_hash=p.model_hash AND r.start_issue<=p.target_issue";
        ArrayList<String[]> events=new ArrayList<String[]>();
        try(Cursor c=db.rawQuery("SELECT o.delivery_hash,o.target_issue,o.candidate_id,o.settlement_hash,o.label_hash,o.model_input_hash,o.attempt_count,i.run_hash,p.frozen_features,p.snapshot_hash,p.position,p.prediction_mask,p.actual_number"
                +joined+" JOIN r952_research_outbox o ON o.candidate_id=p.candidate_id AND o.target_issue=p.target_issue"
                +" WHERE "+receipt+" AND p.settled=1 AND o.label_hash=p.label_hash AND o.settlement_hash=p.settlement_hash AND o.model_input_hash=p.input_hash"
                +" AND (o.state IN ('PENDING','RETRYABLE_FAILED') OR (o.state='RUNNING' AND o.lease_until<=?)) ORDER BY o.target_issue,o.candidate_id LIMIT "+limit,new String[]{Long.toString(now)})) {
            while(c.moveToNext()){String[] row=new String[13];for(int i=0;i<row.length;i++)row[i]=c.getString(i);events.add(row);}
        }
        int retired=0;
        for(String[] e:events){
            ResearchWorkBudgetR927.checkpoint();
            if(!valid(e))continue; // Do not launder mismatched receipts by relabeling them.
            String reason="R971_LEGACY_TRAIN_STREAM_RETIRED_NOT_TRAINED|run="+e[7]+"|input="+e[5];
            db.execSQL("UPDATE r952_research_outbox SET state='DEPENDENCY_RETIRED',lease_until=0,last_error=?,updated_at=? WHERE delivery_hash=? AND attempt_count=? AND (state IN ('PENDING','RETRYABLE_FAILED') OR (state='RUNNING' AND lease_until<=?))",new Object[]{reason,now,e[0],Integer.parseInt(e[6]),now});
            if(changed(db)==1){RuntimeRecoveryJournalR952.record(db,e[0],Integer.parseInt(e[6]),"DEPENDENCY_RETIRED",reason);retired++;}
        }
        // A never-settled legacy capture otherwise also blocks the new prospective run.
        ArrayList<String[]> captures=new ArrayList<String[]>();
        try(Cursor c=db.rawQuery("SELECT p.candidate_id,p.target_issue,p.frozen_features,p.snapshot_hash,p.input_hash,i.run_hash"+joined+" WHERE "+receipt+" AND p.settled=0 AND p.proof_error='' ORDER BY p.target_issue,p.candidate_id LIMIT "+limit,null)){
            while(c.moveToNext())captures.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5)});
        }
        for(String[] c:captures){
            ResearchWorkBudgetR927.checkpoint();
            if(!TargetIssueResolver.isValid(c[1])||!hash(c[5])||!V51ChecksumEngine.sha256("R958_INPUT|"+c[2]+'|'+c[3]).equals(c[4]))continue;
            db.execSQL("UPDATE r952_candidate_predraw SET proof_error='R971_LEGACY_TRAIN_STREAM_RETIRED_WITHOUT_LABEL_OR_TRAINING' WHERE candidate_id=? AND target_issue=? AND settled=0 AND evidence_role='TRAIN_STREAM' AND proof_error=''",new Object[]{c[0],c[1]});
        }
        return retired;
    }
    private static boolean valid(String[] e){
        try{
            int attempt=Integer.parseInt(e[6]),pos=Integer.parseInt(e[10]),mask=Integer.parseInt(e[11]),actual=Integer.parseInt(e[12]);
            return attempt>=0&&TargetIssueResolver.isValid(e[1])&&hash(e[7])&&pos>=1&&pos<=10&&actual>=1&&actual<=10&&AppConfig.countK3MaskNumbers(mask)==3
                    &&RuntimeEventContractR952.deliveryHash(e[1],e[2],e[3],e[4]).equals(e[0])
                    &&V51ChecksumEngine.sha256("R958_INPUT|"+e[8]+'|'+e[9]).equals(e[5])
                    &&V51ChecksumEngine.sha256("R958_LABEL|"+e[1]+'|'+e[2]+'|'+mask+'|'+pos+'|'+actual+'|'+e[3]+'|'+e[9]).equals(e[4]);
        }catch(RuntimeException invalid){return false;}
    }
    private static boolean hash(String x){return x!=null&&x.matches("[0-9a-f]{64}");}
    private static int changed(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){return c.moveToFirst()?c.getInt(0):0;}}
}
