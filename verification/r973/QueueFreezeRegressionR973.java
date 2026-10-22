package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Method;

/** Real SQLite and actual product methods; no phone/lifecycle or statistical proof claims. */
public final class QueueFreezeRegressionR973 {
    static int checks, failures;
    static void check(boolean value,String name) {
        checks++; if(!value)failures++;
        System.out.println((value?"PASS ":"FAIL ")+name);
    }
    static void tx(SQLiteDatabase db,Runnable work) {
        db.beginTransaction();try{work.run();db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    static String enqueue(SQLiteDatabase d,String candidate,String issue) {
        final String[] id={null};tx(d,()->id[0]=ResearchTaskQueueR967.enqueue(d,ResearchTaskQueueR967.POST_TRAIN,candidate,issue,"one","input","receipt"));return id[0];
    }
    static ResearchTaskQueueR967.Lease claim(SQLiteDatabase d,String candidate,long now) {
        final ResearchTaskQueueR967.Lease[] l={null};tx(d,()->l[0]=ResearchTaskQueueR967.claimFor(d,ResearchTaskQueueR967.POST_TRAIN,candidate,now));return l[0];
    }
    static void queue(String bridge) throws Exception {
        SQLiteDatabase d=new SQLiteDatabase(bridge);try {
            tx(d,()->ResearchTaskQueueR967.schema(d));long now=System.currentTimeMillis();
            String first=enqueue(d,"ordered","202609210001");
            enqueue(d,"ordered","202609210002");
            d.execSQL("UPDATE "+ResearchTaskQueueR967.TABLE+" SET status='WAIT_INPUT',available_at=? WHERE task_id=?",new Object[]{now+60000,first});
            check(!ResearchTaskQueueR967.hasReady(d,ResearchTaskQueueR967.POST_TRAIN,now),"READY_MATCHES_CLAIM_WHEN_PREDECESSOR_WAITS");
            check(claim(d,"ordered",now)==null,"WAIT_INPUT_PREDECESSOR_STILL_BLOCKS");
            d.execSQL("UPDATE "+ResearchTaskQueueR967.TABLE+" SET status='FAILED_TERMINAL' WHERE task_id=?",new Object[]{first});
            check(ResearchTaskQueueR967.hasReady(d,ResearchTaskQueueR967.POST_TRAIN,now),"TERMINAL_REVIEW_ALLOWS_INDEPENDENT_LATER_RECEIPT");
            ResearchTaskQueueR967.Lease second=claim(d,"ordered",now);
            check(second!=null&&second.issue.equals("202609210002"),"FAILED_REVIEW_DOES_NOT_BLOCK_NEW_POST_TRAIN");
            check(claim(d,"ordered",now)==null,"ACTIVE_LEASE_NOT_STOLEN");
            if(second!=null) {
                ResearchTaskQueueR967.Lease resumed=claim(d,"ordered",now+ResearchTaskQueueR967.LEASE_MS+1);
                check(resumed!=null&&resumed.generation>second.generation,"CRASH_LEASE_RECOVERY");
                tx(d,()->check(!ResearchTaskQueueR967.yield(d,second,"wrong","stale",0),"STALE_WORKER_CANNOT_WRITE"));
                if(resumed!=null)tx(d,()->ResearchTaskQueueR967.complete(d,resumed,"actual-receipt"));
            }
            try(Cursor c=d.rawQuery("SELECT status,result_hash FROM "+ResearchTaskQueueR967.TABLE+" WHERE task_id=?",new String[]{first})) {
                check(c.moveToFirst()&&"FAILED_TERMINAL".equals(c.getString(0))&&"".equals(c.getString(1)),"FAILED_TASK_NOT_RELABELED_SUCCESS");
            }
            String expired=enqueue(d,"expired","202609210003");
            d.execSQL("UPDATE "+ResearchTaskQueueR967.TABLE+" SET status='EXPIRED' WHERE task_id=?",new Object[]{expired});
            enqueue(d,"expired","202609210004");
            check(claim(d,"expired",now)!=null,"EXPIRED_REVIEW_DOES_NOT_BLOCK_NEW_POST_TRAIN");
        } finally {d.shutdown();}
    }
    static boolean eligible(SQLiteDatabase d,String target)throws Exception {
        Method m=FrozenCandidateAdapterR958.class.getDeclaredMethod("eligible",SQLiteDatabase.class,String.class);m.setAccessible(true);
        try(Cursor c=(Cursor)m.invoke(null,d,target)){return c.moveToFirst();}
    }
    static void freeze(String bridge)throws Exception {
        SQLiteDatabase d=new SQLiteDatabase(bridge);try {
            d.execSQL("CREATE TABLE r952_candidate(candidate_id TEXT,ast TEXT,structural_hash TEXT,state TEXT)");
            d.execSQL("CREATE TABLE r952_learning_cursor(consumer_id TEXT,trained_until_issue TEXT,artifact_hash TEXT)");
            d.execSQL("CREATE TABLE r952_artifact(artifact_hash TEXT,candidate_id TEXT,model_payload TEXT,payload_hash TEXT,numeric_updates INTEGER)");
            d.execSQL("CREATE TABLE "+ResearchEvaluationRunR967.RUN+"(learner_id TEXT,first_issue TEXT,rule_hash TEXT,run_hash TEXT,integrity_epoch TEXT,initial_updates INTEGER)");
            d.execSQL("CREATE TABLE "+ResearchEvaluationRunR967.EVAL+"(candidate_id TEXT,learner_id TEXT,run_hash TEXT,rule_hash TEXT,update_endpoint INTEGER)");
            d.execSQL("CREATE TABLE "+ResearchLifecycleR932.TABLE+"(candidate_key TEXT,state TEXT)");
            d.execSQL("CREATE TABLE r952_research_outbox(candidate_id TEXT,state TEXT)");
            d.execSQL("CREATE TABLE r952_candidate_predraw(candidate_id TEXT,settled INTEGER,proof_error TEXT)");
            d.execSQL("INSERT INTO r952_candidate VALUES('C','ast','hash','TRAINING')");
            d.execSQL("INSERT INTO r952_learning_cursor VALUES('R952:C','202609210239','artifact')");
            d.execSQL("INSERT INTO r952_artifact VALUES('artifact','C','payload','payload-hash',240)");
            d.execSQL("INSERT INTO "+ResearchEvaluationRunR967.RUN+" VALUES('C','202609210001',?,'run','epoch',120)",new Object[]{TrainingCodeIdentityR964.SHA256});
            check(eligible(d,"202609210240"),"COMMITTED_120_FRESH_UPDATES_ELIGIBLE");
            d.execSQL("INSERT INTO r952_research_outbox VALUES('C','PENDING')");
            check(eligible(d,"202609210240"),"PENDING_TRAINING_CANNOT_BLOCK_IMMUTABLE_CHECKPOINT");
            d.execSQL("DELETE FROM r952_research_outbox");
            d.execSQL("INSERT INTO r952_candidate_predraw VALUES('C',0,'')");
            check(eligible(d,"202609210240"),"UNSETTLED_FUTURE_CAPTURE_CANNOT_BLOCK_COMMITTED_CHECKPOINT");
            d.execSQL("DELETE FROM r952_candidate_predraw");
            check(!eligible(d,"202609210239"),"TRAINING_MUST_STRICTLY_PRECEDE_FREEZE");
            d.execSQL("UPDATE r952_artifact SET numeric_updates=239");
            check(!eligible(d,"202609210240"),"WARMSTART_UPDATES_NOT_COUNTED_AS_NEW_TRAINING");
            d.execSQL("UPDATE r952_artifact SET numeric_updates=240");
            d.execSQL("INSERT INTO "+ResearchEvaluationRunR967.EVAL+" VALUES('F','C','run',?,120)",new Object[]{TrainingCodeIdentityR964.SHA256});
            check(!eligible(d,"202609210240"),"DUPLICATE_ENDPOINT_NOT_REFROZEN");
            d.execSQL("DELETE FROM "+ResearchEvaluationRunR967.EVAL);
            d.execSQL("UPDATE r952_candidate SET state='QUARANTINED'");
            check(!eligible(d,"202609210240"),"QUARANTINE_STILL_BLOCKS");
            d.execSQL("UPDATE r952_candidate SET state='TRAINING'");
            d.execSQL("UPDATE "+ResearchEvaluationRunR967.RUN+" SET rule_hash='obsolete'");
            check(!eligible(d,"202609210240"),"INCOMPATIBLE_TRAINING_RULE_STILL_BLOCKS");
            String approved=ResearchSemanticsGeneratedR967.COMPATIBLE_PREDECESSOR_RULE_SHA256;
            check(approved.matches("[0-9a-f]{64}")&&!approved.equals(TrainingCodeIdentityR964.SHA256),"CURRENT_RULE_DIFFERS_FROM_EXACT_APPROVED_PREDECESSOR");
            d.execSQL("UPDATE "+ResearchEvaluationRunR967.RUN+" SET rule_hash=?",new Object[]{approved});
            check(eligible(d,"202609210240"),"R972_COMMITTED_TRAINING_CHECKPOINT_CONTINUES_AFTER_UPGRADE");
            d.execSQL("UPDATE r952_artifact SET numeric_updates=239");
            check(!eligible(d,"202609210240"),"CONTINUATION_DOES_NOT_INHERIT_WARMSTART_AS_FRESH_N");
            try(Cursor c=d.rawQuery("SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.EVAL,null)){check(c.moveToFirst()&&c.getInt(0)==0,"CONTINUATION_CHECK_CREATES_NO_FAKE_EVALUATION");}
            check(!TrainingContinuationR973.compatible("")&&!TrainingContinuationR973.compatible(null),"EMPTY_UNKNOWN_PREDECESSOR_REJECTED");
            boolean rejected=false;try{TrainingContinuationR973.predicate("r.rule_hash OR 1=1");}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"CONTINUATION_SQL_IDENTIFIER_INJECTION_REJECTED");
        } finally {d.shutdown();}
    }
    public static void main(String[] args)throws Exception {
        queue(args[0]);freeze(args[0]);
        System.out.println("R973 checks="+checks+" failures="+failures+"; DEVICE_NOT_RUN; NEW_FUTURE_EVIDENCE_NOT_RUN");
        if(failures>0)System.exit(1);
    }
}
