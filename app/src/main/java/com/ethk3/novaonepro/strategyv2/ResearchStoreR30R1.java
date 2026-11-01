package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Typed facts are written by the original artifact transaction, never by a scheduler heartbeat. */
public final class ResearchStoreR30R1 {
    private ResearchStoreR30R1(){}
    public static final class Facts {
        public final long cursor,latest; public final Long pending,oldest,completed; public final String identity;
        public Facts(long cursor,long latest,Long pending,Long oldest,Long completed,String identity){
            if(cursor<0||latest<cursor||pending!=null&&pending<0||identity==null||identity.isEmpty())throw new IllegalArgumentException("INVALID_TYPED_COMMIT_FACTS");
            if(pending!=null&&pending==0&&oldest!=null)throw new IllegalArgumentException("EMPTY_PENDING_HAS_OLDEST");
            this.cursor=cursor;this.latest=latest;this.pending=pending;this.oldest=oldest;this.completed=completed;this.identity=identity;
        }
    }
    public static boolean ready(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('r30r1_analysis_run','r30r1_research_measure','r30r1_proposal','r30r1_consume','r30r1_chart_cache','r30r1_task_progress')",null)){return c.moveToFirst()&&c.getLong(0)==6;}}
    public static void appendOwnedProgress(ResearchWriteQueueR27.Batch batch,long calendar,Facts facts){RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();if(claim!=null)appendProgress(batch,claim.task,claim.owner,claim.fence,calendar,facts);}
    public static void appendProgress(ResearchWriteQueueR27.Batch batch,String task,String session,long fence,long calendar,Facts facts){
        // Progress first, then the original final source/deadline guard; both are in the same transaction.
        final com.ethk3.novaonepro.DatabaseCoordinator.WriteCallable<Integer> original=batch.after;
        batch.after=db->{commitProgress(db,task,session,fence,calendar<0?calendar(db):calendar,facts);return original==null?0:original.call(db);};
    }
    public static void commitProgress(SQLiteDatabase db,String task,String session,long fence,long calendar,Facts facts){
        if(!db.inTransaction())throw new IllegalStateException("TYPED_PROGRESS_REQUIRES_ARTIFACT_TRANSACTION");
        ResearchTaskRegistryR30R1.Entry entry=ResearchTaskRegistryR30R1.require(task);
        long now=System.currentTimeMillis();
        try(Cursor c=db.rawQuery("SELECT artifact_identity FROM r30r1_task_progress WHERE task_id=?",new String[]{task})){if(c.moveToFirst()&&facts.identity.equals(c.getString(0)))return;}
        db.execSQL("INSERT OR REPLACE INTO r30r1_task_progress(task_id,owner_version,work_unit,work_cursor,known_latest,pending,oldest_pending_ordinal,last_commit_clock_ordinal,completed_input_ordinal,fence,session_id,updated_at,artifact_identity) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{task,ResearchTaskRegistryR30R1.VERSION,entry.unit.name(),facts.cursor,facts.latest,facts.pending,facts.oldest,calendar<0?null:calendar,facts.completed,fence,session,now,facts.identity});
        if(task.startsWith("r27-"))db.execSQL("UPDATE r26_task SET cursor=?,last_progress=?,steps=steps+1,first_progress_at=CASE WHEN first_progress_at=0 THEN ? ELSE first_progress_at END WHERE task_id=? AND owner=? AND fence=?",new Object[]{facts.cursor,now,now,task,session,fence});
    }
    /** Exact owner adapters. Missing adapters stay UNKNOWN, instead of treating a receipt as a product. */
    public static void afterOwnedArtifact(SQLiteDatabase db,String operation,RuntimeCoordinatorR26.Claim claim){
        if(claim==null||!ResearchTaskRegistryR30R1.ENTRIES.containsKey(claim.task)||operation.contains("TASK_RECEIPT")||operation.contains("THREAD_CPU")||operation.contains("CLAIM")||operation.contains("WRITE_WAIT")||!ready(db))return;
        String task=claim.task;String query=null;
        if(task.equals("r27-live")&&operation.equals("R27_LIVE_SETTLE"))query="SELECT min(through_ordinal),group_concat(policy_hash||':'||through_ordinal,'|') FROM (SELECT policy_hash,through_ordinal FROM r27_live_shadow WHERE status IN ('ACTIVE','OBSERVE') ORDER BY policy_hash)";
        else if(task.equals("r27-atoms")&&operation.equals("R27_ATOM_SCORES"))query="SELECT coalesce(sum(n_select),0),group_concat(atom_id||':'||dataset_hash||':'||computed_at,'|') FROM (SELECT atom_id,dataset_hash,computed_at,n_select FROM r27_atom_score ORDER BY atom_id)";
        else if(task.equals("r27-abi")&&(operation.equals("R27_SEMANTIC_ABI")||operation.equals("R27_LEGACY_ABI_CONTINUITY")))query="SELECT count(*),group_concat(policy_hash||':'||fingerprint,'|') FROM (SELECT policy_hash,fingerprint FROM r27_semantic_fingerprint ORDER BY policy_hash,build)";
        else if(task.equals("r27-calibration")&&operation.equals("R27_CALIBRATION"))query="SELECT count(*),group_concat(policy_hash||':'||state||':'||updated_at,'|') FROM (SELECT policy_hash,state,updated_at FROM r27_calibration ORDER BY policy_hash)";
        else if(task.equals("r27-legacy-export")&&operation.startsWith("R27_LEGACY"))query="SELECT count(*),group_concat(source_id||':'||status||':'||policy_hash,'|') FROM (SELECT source_id,status,policy_hash FROM r27_legacy_export ORDER BY source_id)";

        else if(task.equals("r27-evolution")&&operation.startsWith("R27_EVOLUTION_STEP_"))query="SELECT generation*10+step,dataset_hash||'|'||rng_state||'|'||generation||'|'||step FROM r27_evolution_cursor WHERE loop='MAIN'";
        else if(task.equals("r27-replay-dataset")&&operation.equals("R27_REPLAY_DATASET"))query="SELECT to_issue,dataset_hash FROM r27_replay_dataset ORDER BY to_issue DESC LIMIT 1";
        else if(task.equals("live-model-training")&&operation.equals("R26_LIVE_LEARN_COMMIT"))query="SELECT min(through_ordinal),group_concat(policy_hash||':'||checkpoint_hash,'|') FROM (SELECT policy_hash,through_ordinal,checkpoint_hash FROM r26_live_model WHERE status='ACTIVE' ORDER BY policy_hash)";
        else if(task.equals("legacy-replay"))query="SELECT through_ordinal,checkpoint_hash FROM r26_legacy_runtime WHERE singleton=1";
        else if(task.startsWith("expert-"))query="SELECT coalesce(sum(cursor),0),group_concat(state_hash,'|') FROM (SELECT cursor,state_hash FROM r26_expert_experiment WHERE expert_id='"+task.substring(7)+"' ORDER BY experiment_id)";
        else if(task.equals("retention")&&operation.startsWith("R26_RETENTION_PHASE_")){RetentionR26.Checkpoint cp=RetentionR26.checkpoint(db);long cursor=cp.workCursor;commitProgress(db,task,claim.owner,claim.fence,calendar(db),new Facts(cursor,cursor,null,null,null,HashingSU1.text(cp.original)));return;}
        
        if(query==null)return;
        try(Cursor c=db.rawQuery(query,null)){
            if(!c.moveToFirst()||c.isNull(0)||c.isNull(1)||c.getLong(0)<0)return;long cursor=c.getLong(0),at=calendar(db);Long completed=null,pending=null,oldest=null;
            if(task.equals("r27-replay-dataset")){cursor=com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(c.getString(0));completed=cursor;pending=at<0?null:Math.max(0,at-cursor-RuntimeR27.REFRESH_NEW_ROWS+1);}
            else if(task.equals("live-model-training")||task.equals("legacy-replay")||task.equals("r27-live")){completed=cursor;pending=at<0?null:Math.max(0,at-cursor);if(pending!=null&&pending>0)oldest=cursor+1;}
            else if(task.equals("r27-atomnet")){AtomNetSessionR30 state=AtomNetStoreR30.restore(db);if(state==null)return;cursor=state.pretrainSamples+state.trainer.onlineSteps;completed=state.ready()?state.trainedThroughOrdinal():null;pending=null;}
            if(task.equals("r27-evolution"))pending=1L;
            long latest=(task.equals("live-model-training")||task.equals("legacy-replay")||task.equals("r27-live")||task.equals("r27-replay-dataset"))&&at>=0?Math.max(cursor,at):cursor;
            commitProgress(db,task,claim.owner,claim.fence,at,new Facts(cursor,latest,pending,oldest,completed,HashingSU1.text(c.getString(1))));
        }
    }
    static long calendar(SQLiteDatabase db){String issue=RuntimeR27.latestIssue(db);return issue==null?-1:com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(issue);}
}
