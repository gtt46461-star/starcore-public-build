package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;

/** Derived post-ACK scheduling; does not create a second settlement or training owner. */
public final class PostSettlementResearchLoopR952 {
    private PostSettlementResearchLoopR952(){}
    static String enqueueCommitted(SQLiteDatabase db,ResearchOutboxR952.Event e) {
        String input=V51ChecksumEngine.sha256("R967_POST_ONLINE|"+e.deliveryHash+'|'+e.labelHash+'|'+e.settlementHash+'|'+e.modelInputHash);
        String id=ResearchTaskQueueR967.enqueue(db,ResearchTaskQueueR967.POST_TRAIN,e.candidateId,e.targetIssue,e.deliveryHash,input,e.deliveryHash);
        // Completed intent replay must not re-pin historical inputs.
        try(Cursor c=db.rawQuery("SELECT status FROM "+ResearchTaskQueueR967.TABLE+" WHERE task_id=?",new String[]{id})) {c.moveToFirst();if(ResearchTaskQueueR967.terminal(c.getString(0)))return id;}
        for(String table:new String[]{"r952_research_outbox","r952_candidate_predraw",DbHelper.TABLE_DRAWS,DbHelper.TABLE_R36_SETTLEMENT_TASK})ResearchTaskQueueR967.pin(db,id,table,e.targetIssue);
        return id;
    }
    /** Bounded index repair from actual completed ONLINE receipts; never invent labels. */
    static int rehydrate(SQLiteDatabase db,int limit) {
        ArrayList<ResearchOutboxR952.Event> rows=new ArrayList<ResearchOutboxR952.Event>();
        try(Cursor c=db.rawQuery("SELECT o.delivery_hash,o.target_issue,o.candidate_id,o.settlement_hash,o.label_hash,o.model_input_hash FROM r952_research_outbox o JOIN r952_candidate_predraw p ON p.target_issue=o.target_issue AND p.candidate_id=o.candidate_id WHERE o.state='COMPLETED' AND p.settled=1 AND p.evidence_role IN ('ONLINE_TRAINING','TRAIN_STREAM_V2') AND p.label_hash=o.label_hash AND p.settlement_hash=o.settlement_hash AND NOT EXISTS(SELECT 1 FROM "+ResearchTaskQueueR967.TABLE+" t WHERE t.kind='POST_TRAIN' AND t.authority_ref=o.delivery_hash) ORDER BY o.target_issue,o.candidate_id LIMIT "+Math.max(1,Math.min(16,limit)),null)) {
            while(c.moveToNext())rows.add(new ResearchOutboxR952.Event(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),0,0));
        }
        int inserted=0;
        for(ResearchOutboxR952.Event e:rows){try{enqueueCommitted(db,e);inserted++;}catch(ResearchTaskQueueR967.Capacity full){break;}}
        return inserted;
    }
    static String projectOnline(SQLiteDatabase db,ResearchTaskQueueR967.Lease task) {
        try(Cursor c=db.rawQuery("SELECT o.label_hash,o.settlement_hash,o.model_input_hash,p.hit,p.evidence_role,p.model_hash FROM r952_research_outbox o JOIN r952_candidate_predraw p ON p.target_issue=o.target_issue AND p.candidate_id=o.candidate_id WHERE o.delivery_hash=? AND o.state='COMPLETED' AND o.target_issue=? AND o.candidate_id=? AND p.settled=1 AND p.label_hash=o.label_hash AND p.settlement_hash=o.settlement_hash",new String[]{task.authorityRef,task.issue,task.candidate})) {
            if(!c.moveToFirst())throw new IllegalStateException("R967_POST_AUTHORITY_OR_INPUT_MISSING");
            if(ResearchEvaluationRunR967.ROLE.equals(c.getString(4)))ResearchEvaluationRunR967.verifyRecordedInput(db,task.candidate,task.issue,c.getString(2),c.getString(5));
            else if(!"ONLINE_TRAINING".equals(c.getString(4)))throw new IllegalStateException("R967_SEALED_LABEL_FEEDBACK_FORBIDDEN");
            int hit=c.getInt(3);if(hit!=0&&hit!=1)throw new IllegalStateException("R967_POST_HIT_INVALID");
            String expected=V51ChecksumEngine.sha256("R967_POST_ONLINE|"+task.authorityRef+'|'+c.getString(0)+'|'+c.getString(1)+'|'+c.getString(2));
            if(!expected.equals(task.inputHash))throw new IllegalStateException("R967_POST_INPUT_DRIFT");
            String payload="label="+c.getString(0)+"|settlement="+c.getString(1)+"|hit="+hit;
            KnowledgeLedgerR952.put(db,task.issue,task.candidate,"STRICT_FUTURE",payload);
            return V51ChecksumEngine.sha256(payload);
        }
    }
    public static boolean drainOne(final DbStore store) {
        if(store==null)return false;
        // R975: a budget/cancellation between two transactions used to commit RUNNING
        // without a result, stranding the issue until its 45 s lease expired. This task
        // is a small immutable projection, NOT model training: claim, verify, projection
        // and completion now share one rollback boundary. A yield rolls back the claim
        // as well. Existing crashed RUNNING leases remain protected until lease expiry.
        final String[] failure=new String[]{""};
        final ResearchTaskQueueR967.Lease[] failedClaim=new ResearchTaskQueueR967.Lease[1];
        Boolean completed=store.coordinatorForResearchR952().runAtomicWrite("R975_POST_PROJECT_ATOMIC",new DatabaseCoordinator.WriteCallable<Boolean>() {
            public Boolean call(SQLiteDatabase db) {
                ResearchMigrationRegistryR952.ensure(db);
                ResearchTaskQueueR967.Lease task=ResearchTaskQueueR967.claim(db,ResearchTaskQueueR967.POST_TRAIN,System.currentTimeMillis());
                if(task==null){rehydrate(db,1);task=ResearchTaskQueueR967.claim(db,ResearchTaskQueueR967.POST_TRAIN,System.currentTimeMillis());}
                if(task==null)return Boolean.FALSE;
                try {
                    ResearchWorkBudgetR927.checkpoint();
                    String projection=projectOnline(db,task);
                    String result=V51ChecksumEngine.sha256("R971_POST_RESULT_V2|"+projection+"|EVOLUTION_COALESCED_BY_DIRECTOR");
                    ResearchWorkBudgetR927.checkpoint();
                    ResearchTaskQueueR967.complete(db,task,result);
                    return Boolean.TRUE;
                } catch (ResearchWorkBudgetR927.Yield yield) {throw yield;}
                  catch (RuntimeResourceAuditR927.BudgetExceeded deadline) {throw deadline;}
                  catch (RuntimeException error) {
                    failedClaim[0]=task;
                    failure[0]=task.id+'|'+error.getClass().getSimpleName()+":"+String.valueOf(error.getMessage());
                    throw error; // rollback the claim and any partial projection
                }
            }
        },null);
        if(completed!=null)return completed.booleanValue();
        if(failedClaim[0]!=null) {
            // A permanently broken oldest projection must not win every next poll.
            // The original transaction has already rolled back; fence a scheduling-only
            // retry receipt against its PRE-claim generation, never mark it completed.
            store.coordinatorForResearchR952().runAtomicWrite("R976_POST_RETRY_BACKOFF",new DatabaseCoordinator.WriteRunnable(){
                public void run(SQLiteDatabase db){
                    ResearchTaskQueueR967.deferRolledBack(db,failedClaim[0],failure[0],1000L);
                }
            });
        }
        // This is diagnostic state only; it grants neither completion nor new evidence.
        store.setState("r967_post_training_status","PENDING_RETRY|"+(failure[0].isEmpty()?"COORDINATOR_DEFERRED":failure[0]));
        return false;
    }
    /** Legacy call is a bounded drain, not an untracked second execution of global evolution. */
    public static int run(DbStore store,String issue){return drainOne(store)?1:0;}
}
