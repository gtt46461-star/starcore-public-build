package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

/** Ordered, fenced training. Model, cursor, ACK and post-training intent share one commit. */
public final class ResearchOutboxR952 {
    private static final long LEASE_MS=45000L;
    private static final String PROCESS=java.util.UUID.randomUUID().toString();
    private static final java.util.concurrent.ConcurrentHashMap<String,Event> ACTIVE=
            new java.util.concurrent.ConcurrentHashMap<>();
    private ResearchOutboxR952(){}
    public static String enqueue(SQLiteDatabase db,String target,String candidate,String settlement,String label,String input) {
        if(!db.inTransaction())throw new IllegalStateException("R967_OUTBOX_TRANSACTION_REQUIRED");
        String delivery=RuntimeEventContractR952.deliveryHash(target,candidate,settlement,label);
        String modelInput=input==null?"":input;
        // Candidate/target is the existing online-label identity. Changed labels are never a retry.
        try(Cursor c=db.rawQuery("SELECT delivery_hash,settlement_hash,label_hash,model_input_hash FROM r952_research_outbox WHERE target_issue=? AND candidate_id=?",new String[]{target,candidate})) {
            boolean found=false;
            while(c.moveToNext()) {
                if(found||!delivery.equals(c.getString(0))||!settlement.equals(c.getString(1))||!label.equals(c.getString(2))||!modelInput.equals(c.getString(3)))
                    throw new IllegalStateException("R967_OUTBOX_NATURAL_KEY_DRIFT");
                found=true;
            }
            if(found)return delivery;
        }
        long now=System.currentTimeMillis();
        db.execSQL("INSERT INTO r952_research_outbox(delivery_hash,target_issue,candidate_id,settlement_hash,label_hash,model_input_hash,event_type,state,attempt_count,lease_until,last_error,created_at,updated_at) VALUES(?,?,?,?,?,?,'STRICT_FUTURE_LABEL','PENDING',0,0,'',?,?)",new Object[]{delivery,target,candidate,settlement,label,modelInput,now,now});
        return delivery;
    }
    static Event claim(SQLiteDatabase db,long now) {
        if(!db.inTransaction())throw new IllegalStateException("R967_OUTBOX_TRANSACTION_REQUIRED");
        reapExpiredOwned(db,now);
        String ready="(o.state='PENDING' OR (o.state='RETRYABLE_FAILED' AND (o.lease_until<=? OR o.lease_until>?))"
                +" OR (o.state='RUNNING' AND o.lease_owner<>?))";
        try(Cursor c=db.rawQuery("SELECT o.delivery_hash,o.target_issue,o.candidate_id,o.settlement_hash,o.label_hash,o.model_input_hash,o.attempt_count FROM r952_research_outbox o"
                +" WHERE "+ready+" AND NOT EXISTS(SELECT 1 FROM r952_research_outbox earlier WHERE earlier.candidate_id=o.candidate_id AND earlier.target_issue<o.target_issue AND earlier.state NOT IN ('COMPLETED','DEPENDENCY_RETIRED'))"
                +" ORDER BY o.updated_at,o.target_issue,o.candidate_id LIMIT 1",new String[]{Long.toString(now),Long.toString(now+60000L),PROCESS})) {
            if(!c.moveToFirst())return null;
            if(c.getLong(6)<0||c.getLong(6)>=Integer.MAX_VALUE)throw new IllegalStateException("R967_OUTBOX_GENERATION_EXHAUSTED");
            Event e=new Event(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getInt(6)+1,now+LEASE_MS);
            db.execSQL("UPDATE r952_research_outbox SET state='RUNNING',attempt_count=?,lease_until=?,lease_owner=?,last_error='',updated_at=? WHERE delivery_hash=? AND attempt_count=?"
                    +" AND (state='PENDING' OR (state='RETRYABLE_FAILED' AND (lease_until<=? OR lease_until>?)) OR (state='RUNNING' AND lease_owner<>?))",
                    new Object[]{e.attempt,e.lease,PROCESS,now,e.deliveryHash,e.attempt-1,now,now+60000L,PROCESS});
            if(changed(db)!=1)return null;
            ACTIVE.put(e.deliveryHash,e);
            RuntimeRecoveryJournalR952.record(db,e.deliveryHash,e.attempt,"RUNNING","");return e;
        }
    }
    static boolean owns(SQLiteDatabase db,Event e) {
        if(e==null||ACTIVE.get(e.deliveryHash)!=e||TimeSourceR811.monoNowMs()>=e.monotonicUntil)return false;
        try(Cursor c=db.rawQuery("SELECT state,attempt_count,lease_until,lease_owner FROM r952_research_outbox WHERE delivery_hash=?",new String[]{e.deliveryHash})) {
            return c.moveToFirst()&&"RUNNING".equals(c.getString(0))&&c.getInt(1)==e.attempt&&c.getLong(2)==e.lease&&PROCESS.equals(c.getString(3));
        }
    }
    private static void reapExpiredOwned(SQLiteDatabase db,long now) {
        long mono=TimeSourceR811.monoNowMs();
        try(Cursor c=db.rawQuery("SELECT delivery_hash,attempt_count,lease_until FROM r952_research_outbox WHERE state='RUNNING' AND lease_owner=?",new String[]{PROCESS})) {
            while(c.moveToNext()) {
                String id=c.getString(0);Event live=ACTIVE.get(id);
                if(live!=null&&live.attempt==c.getInt(1)&&live.lease==c.getLong(2)&&mono<live.monotonicUntil)continue;
                db.execSQL("UPDATE r952_research_outbox SET state='RETRYABLE_FAILED',lease_until=?,lease_owner='',last_error='F02_MONOTONIC_OR_ORPHAN_RECOVERY',updated_at=? WHERE delivery_hash=? AND state='RUNNING' AND attempt_count=? AND lease_owner=?",
                        new Object[]{now,now,id,c.getInt(1),PROCESS});
                if(live!=null)ACTIVE.remove(id,live);
            }
        }
    }

    /** Testable actual owner boundary; any later failure rolls all of this transaction back. */
    static boolean consume(SQLiteDatabase db,Event e,ResearchTrainingHookRegistryR952.PreparedBatch prepared) {
        if(!db.inTransaction())throw new IllegalStateException("R967_OUTBOX_TRANSACTION_REQUIRED");
        if(!owns(db,e))return false;
        ResearchTrainingHookRegistryR952.commit(db,e,prepared);
        db.execSQL("UPDATE r952_research_outbox SET state='COMPLETED',lease_until=0,lease_owner='',updated_at=? WHERE delivery_hash=? AND state='RUNNING' AND attempt_count=? AND lease_until=?",new Object[]{System.currentTimeMillis(),e.deliveryHash,e.attempt,e.lease});
        if(changed(db)!=1)throw new IllegalStateException("R958_TRAIN_COMPLETE_FENCE");
        PostSettlementResearchLoopR952.enqueueCommitted(db,e);
        RuntimeRecoveryJournalR952.record(db,e.deliveryHash,e.attempt,"COMPLETED","");
        return true;
    }
    static boolean release(SQLiteDatabase db,Event e,String reason,long delayMs) {
        if(!db.inTransaction())throw new IllegalStateException("R967_OUTBOX_TRANSACTION_REQUIRED");
        long now=System.currentTimeMillis();
        db.execSQL("UPDATE r952_research_outbox SET state='RETRYABLE_FAILED',lease_until=?,lease_owner='',last_error=?,updated_at=? WHERE delivery_hash=? AND state='RUNNING' AND attempt_count=? AND lease_until=?",new Object[]{now+Math.max(0,Math.min(60000,delayMs)),reason,now,e.deliveryHash,e.attempt,e.lease});
        boolean changed=changed(db)==1;
        if(changed)ACTIVE.remove(e.deliveryHash,e);
        if(changed)RuntimeRecoveryJournalR952.record(db,e.deliveryHash,e.attempt,"RETRYABLE_FAILED",reason);
        return changed;
    }
    public static boolean drainOne(final DbStore store) {
        if(store==null)return false;
        // Return value means one TRAINING commit, never a post-training review. The Director
        // drains persisted post intents independently, including when no training labels remain.
        final Event claimed=store.coordinatorForResearchR952().runAtomicWrite("R952_TRAIN_CLAIM",new DatabaseCoordinator.WriteCallable<Event>() {
            public Event call(SQLiteDatabase db){ResearchMigrationRegistryR952.ensure(db);long now=System.currentTimeMillis();LegacyTrainingStreamMigrationR971.retireBatch(db,32,now);return claim(db,now);}
        },null);
        if(claimed==null)return false;
        final RuntimeException[] failure=new RuntimeException[1];
        try {
            ResearchWorkBudgetR927.checkpoint();
            final ResearchTrainingHookRegistryR952.PreparedBatch prepared=ResearchTrainingHookRegistryR952.prepare(store.readable(),claimed);
            Boolean done=store.coordinatorForResearchR952().runAtomicWrite("R952_TRAIN_CONSUME",new DatabaseCoordinator.WriteCallable<Boolean>() {
                public Boolean call(SQLiteDatabase db) {
                    try{return Boolean.valueOf(consume(db,claimed,prepared));}
                    catch(RuntimeException e){failure[0]=e;throw e;}
                }
            },null);
            if(Boolean.TRUE.equals(done)){ACTIVE.remove(claimed.deliveryHash,claimed);return true;}
            if(Boolean.FALSE.equals(done))return false; // Superseded lease, not algorithm failure.
            if(failure[0]==null)failure[0]=new IllegalStateException("R967_COORDINATOR_CONSUME_DEFERRED");
        }catch(RuntimeException e){failure[0]=e;}
        final RuntimeException cause=failure[0];
        final boolean yielded=cause instanceof ResearchWorkBudgetR927.Yield||cause instanceof RuntimeResourceAuditR927.BudgetExceeded||cause instanceof ResearchTaskQueueR967.Capacity;
        final String reason=(yielded?"RESOURCE_YIELD|":"RETRYABLE_INFRASTRUCTURE|")+cause.getClass().getSimpleName()+":"+String.valueOf(cause.getMessage());
        boolean recorded=ResearchWorkBudgetR927.finishLeaseReceiptR66(() -> store.coordinatorForResearchR952().runAtomicWrite("R952_TRAIN_RELEASE",new DatabaseCoordinator.WriteRunnable() {
            public void run(SQLiteDatabase db){release(db,claimed,reason,yielded?250L:1000L*Math.min(60,claimed.attempt));}
        }));
        // Failure to release is safe: the committed lease expires and can be reclaimed.
        if(!recorded)store.setState("r967_training_retry_status","LEASE_RECOVERY_REQUIRED|"+claimed.deliveryHash);
        else store.setState("r967_training_retry_status",reason);
        // attempt_count is a fencing generation/operational metric, NEVER quality evidence.
        return false;
    }
    private static int changed(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){return c.moveToFirst()?c.getInt(0):0;}}
    public static final class Event {
        public final String deliveryHash,targetIssue,candidateId,settlementHash,labelHash,modelInputHash;
        public final int attempt;
        public final long lease,monotonicUntil;
        Event(String d,String t,String c,String s,String l,String m,int a,long fence){deliveryHash=d;targetIssue=t;candidateId=c;settlementHash=s;labelHash=l;modelInputHash=m;attempt=a;lease=fence;monotonicUntil=TimeSourceR811.monoNowMs()+LEASE_MS;}
    }
}
