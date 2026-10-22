package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.PostSettlementResearchLoopR952;
/** Broken projection must retain evidence and ordering without starving an unrelated learner. */
public final class PostRetryHarnessR976 {
    static int checks;
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
    static long n(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){c.moveToFirst();return c.getLong(0);}}
    static String seed(SQLiteDatabase d,String who,String issue,boolean bad){
        String h=V51ChecksumEngine.sha256("SYNTHETIC_RETRY_"+who+issue);
        d.execSQL("INSERT INTO r952_candidate_predraw(target_issue,candidate_id,prediction_mask,position,snapshot_hash,structure_hash,model_hash,code_hash,data_hash,captured_at,settled,actual_number,hit,settlement_hash,label_hash,input_hash,evidence_role) VALUES(?,?,14,1,?,?,?,?,?,1,1,1,1,?,?,?,'ONLINE_TRAINING')",new Object[]{issue,who,h,h,h,h,h,h,h,h});
        d.execSQL("INSERT INTO r952_research_outbox(delivery_hash,target_issue,candidate_id,settlement_hash,label_hash,model_input_hash,event_type,state,created_at,updated_at) VALUES(?,?,?,?,?,?,'TRAIN','COMPLETED',1,1)",new Object[]{h,issue,who,h,h,h});
        String input=V51ChecksumEngine.sha256("R967_POST_ONLINE|"+h+'|'+h+'|'+h+'|'+h);
        String task=ResearchTaskQueueR967.enqueue(d,ResearchTaskQueueR967.POST_TRAIN,who,issue,h,bad?V51ChecksumEngine.sha256("BAD_HOST_INPUT"):input,h);
        ResearchTaskQueueR967.pin(d,task,"r952_candidate_predraw",issue);
        return task;
    }
    public static void main(String[] args)throws Exception{
        SQLiteDatabase d=new SQLiteDatabase(args[0]);
        try{
            DbStore s=HostProductR974.store(d);HostProductR974.schema(d,s);
            d.beginTransaction();String bad,good;
            try{
                bad=seed(d,"BROKEN_LEARNER","202609220100",true);
                seed(d,"BROKEN_LEARNER","202609220101",false);
                good=seed(d,"HEALTHY_LEARNER","202609220100",false);
                d.execSQL("UPDATE "+ResearchTaskQueueR967.TABLE+" SET updated_at=1 WHERE task_id=?",new Object[]{bad});
                d.setTransactionSuccessful();
            }finally{d.endTransaction();}
            check(!PostSettlementResearchLoopR952.drainOne(s),"BAD_INPUT_NOT_COMPLETED");
            check(n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE task_id='"+bad+"' AND status='YIELDED' AND generation=1 AND available_at>0 AND last_error LIKE '%R967_POST_INPUT_DRIFT%'")==1,"ROLLED_BACK_FAILURE_HAS_FENCED_RETRY");
            check(n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.PIN+" WHERE task_id='"+bad+"'")==1,"BROKEN_INPUT_PIN_PRESERVED");
            check(PostSettlementResearchLoopR952.drainOne(s),"UNRELATED_HEALTHY_LEARNER_CAN_ADVANCE");
            check(n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE task_id='"+good+"' AND status='COMPLETED'")==1,"ONLY_VALID_PROJECTION_COMPLETED");
            check(n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE candidate_id='BROKEN_LEARNER' AND target_issue='202609220101' AND status='PENDING'")==1,"SAME_LEARNER_ORDER_PRESERVED");
            check(n(d,"SELECT COUNT(*) FROM r952_knowledge")==1,"NO_FAKE_KNOWLEDGE_FOR_FAILED_TASK");
            d.beginTransaction();
            try{
                // Claim a retry, then prove an older failed attempt cannot rewrite it.
                d.execSQL("UPDATE "+ResearchTaskQueueR967.TABLE+" SET available_at=0 WHERE task_id=?",new Object[]{bad});
                ResearchTaskQueueR967.Lease lease=ResearchTaskQueueR967.claimFor(d,ResearchTaskQueueR967.POST_TRAIN,"BROKEN_LEARNER",System.currentTimeMillis());
                check(lease!=null&&lease.generation==2,"RETRY_GENERATION_MONOTONIC");
                check(!ResearchTaskQueueR967.deferRolledBack(d,lease,"HOST_STALE_ATTEMPT",1000),"CANNOT_DEFER_A_COMMITTED_LIVE_CLAIM");
                check(ResearchTaskQueueR967.owns(d,lease),"LIVE_LEASE_UNCHANGED");
                d.setTransactionSuccessful();
            }finally{d.endTransaction();}
            System.out.println("R976_POST_RETRY_PASS|checks="+checks+"|scope=REAL_SQLITE_SYNTHETIC_INPUT_NOT_DEVICE");
        }finally{d.shutdown();}
    }
}
