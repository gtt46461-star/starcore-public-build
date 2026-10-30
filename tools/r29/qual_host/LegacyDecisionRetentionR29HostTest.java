package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;
import java.util.*;

/** Real SQLite fault injection across Root/RP ownership. Synthetic engineering, never natural-quality evidence. */
public final class LegacyDecisionRetentionR29HostTest {
    static int checks;static void check(String name,boolean ok){System.out.println("R29_DECISION_RETENTION|"+name+"|"+(ok?"OK":"FAIL"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static SQLiteDatabase db()throws Exception{return new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"));}
    static void schema(SQLiteDatabase db){for(String ddl:StrategySchemaSU2.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS su2_live_decision(")||ddl.startsWith("CREATE TABLE IF NOT EXISTS su2_live_daily("))db.execSQL(ddl);}
    static void decision(SQLiteDatabase root,String policy,long ordinal,Integer hit,int act){root.execSQL("INSERT INTO su2_live_decision VALUES(?,?,1,7,?,0.25,0.4,1,2,?,?)",new Object[]{policy,ordinal,act,hit,hit==null?null:3});}
    static void tx(SQLiteDatabase db,Runnable task,boolean kill){db.beginTransactionNonExclusive();try{task.run();if(kill)throw new IllegalStateException("INJECTED_PROCESS_KILL");db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static boolean fails(Runnable work,String reason){try{work.run();return false;}catch(RuntimeException expected){return String.valueOf(expected.getMessage()).contains(reason);}}
    static void count(SQLiteDatabase rp,List<LegacyDecisionRetentionR29.Row> rows){tx(rp,()->LegacyDecisionRetentionR29.countWithin(rp,rows,100),false);}
    static void frozen(SQLiteDatabase root,String policy,long ordinal,String checkpoint){root.execSQL("INSERT INTO r26_frozen VALUES(?,?,?,?,'state','source',1,1,2,?)",new Object[]{policy,ordinal,ordinal-1,checkpoint,new byte[400]});}
    static void incumbent(SQLiteDatabase root,long ordinal){root.execSQL("INSERT INTO r26_incumbent_frozen VALUES(?,?,'source',1,'abi',1,2,?,'payload')",new Object[]{ordinal,ordinal-1,new byte[400]});}
    static void feedback(SQLiteDatabase root,String id,long ordinal){root.execSQL("INSERT INTO r26_feedback VALUES(?,?,?,'202610300001,1,2,3,4,5,6,7,8,9,10',1)",new Object[]{id,ordinal,StatsR27.sha256(id)});}
    public static void main(String[]args)throws Exception{
        try(SQLiteDatabase root=db();SQLiteDatabase rp=db()){
            schema(root);schema(rp);decision(root,"a",1,1,1);decision(root,"a",2,0,1);decision(root,"a",3,1,0);decision(root,"pending",4,null,1);decision(root,"new",20000,1,1);
            List<LegacyDecisionRetentionR29.Row> rows=LegacyDecisionRetentionR29.capture(root,20000);
            check("D1_ONLY_SETTLED_EXPIRED_SOURCE_ROWS_ARE_CAPTURED",rows.size()==3);
            check("D2_KILLED_RESEARCH_COUNT_ROLLS_BACK_RECEIPTS_AND_TOTALS",fails(()->tx(rp,()->LegacyDecisionRetentionR29.countWithin(rp,rows,100),true),"INJECTED")&&n(rp,"SELECT count(*) FROM su2_live_daily")==0&&!LegacyDecisionRetentionR29.table(rp,LegacyDecisionRetentionR29.RECEIPT));
            count(rp,rows);check("D3_PERIODS_ACTIONS_AND_HITS_USE_ORIGINAL_DENOMINATORS",n(rp,"SELECT periods FROM su2_live_daily")==3&&n(rp,"SELECT actions FROM su2_live_daily")==2&&n(rp,"SELECT hits FROM su2_live_daily")==1&&n(root,"SELECT count(*) FROM su2_live_decision")==5);
            count(rp,rows);check("D4_ROOT_WRITE_FAILURE_AND_REDELIVERY_NEVER_RECOUNT",n(rp,"SELECT periods FROM su2_live_daily")==3&&n(rp,"SELECT count(*) FROM "+LegacyDecisionRetentionR29.RECEIPT)==3);
            check("D5_KILLED_ROOT_DELETE_LEAVES_AUTHORITY_AND_COUNTED_RECEIPT",fails(()->tx(root,()->LegacyDecisionRetentionR29.deleteCountedWithin(root,rows,20000),true),"INJECTED")&&n(root,"SELECT count(*) FROM su2_live_decision WHERE genome_key='a'")==3&&n(rp,"SELECT periods FROM su2_live_daily")==3);
            tx(root,()->LegacyDecisionRetentionR29.deleteCountedWithin(root,rows,20000),false);
            List<LegacyDecisionRetentionR29.Row> restored=LegacyDecisionRetentionR29.pending(rp);
            check("D6_LOST_ACK_RECOVERS_COMPLETE_TYPED_RECEIPT_AFTER_ROOT_IS_GONE",restored.size()==3&&restored.get(0).hash.equals(rows.get(0).hash));
            tx(root,()->LegacyDecisionRetentionR29.deleteCountedWithin(root,restored,20000),false);tx(rp,()->LegacyDecisionRetentionR29.acknowledgeWithin(rp,restored,200),false);
            check("D7_ACK_RETRY_DOES_NOT_CHANGE_TOTALS_OR_UNSETTLED_ROWS",LegacyDecisionRetentionR29.pending(rp).isEmpty()&&n(rp,"SELECT periods FROM su2_live_daily")==3&&n(root,"SELECT count(*) FROM su2_live_decision WHERE genome_key='pending' AND hit IS NULL")==1&&n(root,"SELECT count(*) FROM su2_live_decision WHERE genome_key='new'")==1);
            decision(root,"a",1,1,1);List<LegacyDecisionRetentionR29.Row> replay=LegacyDecisionRetentionR29.capture(root,20000);count(rp,replay);tx(root,()->LegacyDecisionRetentionR29.deleteCountedWithin(root,replay,20000),false);
            check("D8_IDENTICAL_REINSERT_CANNOT_INFLATE_LIFETIME_DAILY_TOTAL",n(rp,"SELECT periods FROM su2_live_daily")==3);
            check("D9_COUNTED_RECEIPT_PAYLOAD_CANNOT_BE_REWRITTEN",fails(()->rp.execSQL("UPDATE "+LegacyDecisionRetentionR29.RECEIPT+" SET row_hash='changed'"),"IMMUTABLE"));
        }
        try(SQLiteDatabase root=db();SQLiteDatabase rp=db()){
            schema(root);schema(rp);decision(root,"a",1,1,1);decision(root,"a",2,0,1);List<LegacyDecisionRetentionR29.Row> rows=LegacyDecisionRetentionR29.capture(root,20000);count(rp,rows);root.execSQL("UPDATE su2_live_decision SET settled_at=999 WHERE sequence_ordinal=2");
            check("D10_FULL_ROW_CAS_CONFLICT_ROLLS_BACK_EVERY_ROOT_DELETE",fails(()->tx(root,()->LegacyDecisionRetentionR29.deleteCountedWithin(root,rows,20000),false),"CAS_CONFLICT")&&n(root,"SELECT count(*) FROM su2_live_decision")==2&&n(rp,"SELECT periods FROM su2_live_daily")==2);
            List<LegacyDecisionRetentionR29.Row> revised=LegacyDecisionRetentionR29.capture(root,20000);check("D11_DIFFERENT_PAYLOAD_FOR_COUNTED_KEY_FAILS_CLOSED",fails(()->count(rp,revised),"COUNTED_ROW_CONFLICT")&&n(rp,"SELECT periods FROM su2_live_daily")==2);
        }
        try(SQLiteDatabase root=db()){
            schema(root);for(int i=0;i<100;i++)decision(root,"p"+i,1,1,1);check("D12_CAPTURE_IS_STRICTLY_BOUNDED_TO_SIXTY_ROWS",LegacyDecisionRetentionR29.capture(root,20000).size()==60);
        }
        try(SQLiteDatabase root=db();SQLiteDatabase rp=db()){
            schema(root);schema(rp);root.execSQL(RuntimeSchemaR26.FEEDBACK);for(String ddl:RuntimeSchemaR26.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS r26_frozen(")||ddl.startsWith("CREATE TABLE IF NOT EXISTS r26_incumbent_frozen("))root.execSQL(ddl);
            root.execSQL("CREATE TABLE su2_mainline_binding(target_issue INTEGER PRIMARY KEY,genome_key TEXT)");rp.execSQL("CREATE TABLE r26_live_model(checkpoint_hash TEXT)");rp.execSQL("INSERT INTO r26_live_model VALUES('protected-checkpoint')");rp.execSQL("CREATE TABLE r26_consumption(event_id TEXT,checkpoint_hash TEXT)");rp.execSQL("INSERT INTO r26_consumption VALUES('referenced-feedback','another-checkpoint')");
            decision(root,"settled",1,1,1);frozen(root,"settled",1,"old");decision(root,"unsettled",2,null,1);frozen(root,"unsettled",2,"old2");decision(root,"referenced",3,1,1);frozen(root,"referenced",3,"protected-checkpoint");decision(root,"bound",4,1,1);frozen(root,"bound",4,"old4");root.execSQL("INSERT INTO su2_mainline_binding VALUES(4,'bound')");frozen(root,"unproven",5,"no-decision");
            decision(root,IncumbentLoopSU2.KEY,7,1,1);incumbent(root,7);decision(root,IncumbentLoopSU2.KEY,8,null,1);incumbent(root,8);
            feedback(root,"old-feedback",11);feedback(root,"pending-feedback",8);feedback(root,"referenced-feedback",12);
            LegacyDecisionRetentionR29.AuthorityBatch capture=LegacyDecisionRetentionR29.authorityCapture(root,rp,20000);
            check("D13_AUTHORITY_CAPTURE_DOES_NOT_MUTATE_EITHER_OWNER",n(root,"SELECT count(*) FROM r26_frozen")==5&&n(root,"SELECT count(*) FROM r26_feedback")==3);
            tx(root,()->LegacyDecisionRetentionR29.pruneAuthoritiesWithin(root,capture,20000),false);
            check("D14_OLD_SETTLED_UNREFERENCED_FROZEN_AUTHORITIES_RETIRE",n(root,"SELECT count(*) FROM r26_frozen WHERE policy_hash='settled'")==0&&n(root,"SELECT count(*) FROM r26_incumbent_frozen WHERE target_ordinal=7")==0);
            check("D15_UNRESOLVED_REFERENCED_BOUND_AND_UNPROVEN_FREEZES_SURVIVE",n(root,"SELECT count(*) FROM r26_frozen")==4&&n(root,"SELECT count(*) FROM r26_incumbent_frozen WHERE target_ordinal=8")==1);
            check("D16_ROOT_FEEDBACK_RESPECTS_PENDING_AND_CONTEXT_REFERENCES",n(root,"SELECT count(*) FROM r26_feedback WHERE event_id='old-feedback'")==0&&n(root,"SELECT count(*) FROM r26_feedback WHERE event_id IN ('pending-feedback','referenced-feedback')")==2);
        }
        try(SQLiteDatabase root=db();SQLiteDatabase rp=db()){
            schema(root);schema(rp);root.execSQL(RuntimeSchemaR26.FEEDBACK);root.execSQL(ResearchEventBridgeR29.OUTBOX);rp.execSQL("CREATE TABLE r29_event_inbox(event_id TEXT PRIMARY KEY)");
            String issue="202610300001",id=PolicyIdentityR26.sha256("R26_SETTLED|"+issue),source=StatsR27.sha256("valid terminal source"),payload=issue+",1,2,3,4,5,6,7,8,9,10";long ordinal=com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(issue),current=ordinal+20000;
            root.execSQL("INSERT INTO r26_feedback VALUES(?,?,?,?,1)",new Object[]{id,ordinal,source,payload});ResearchEventBridgeR29.Event delivery=ResearchEventBridgeR29.terminalFeedback(id,ordinal,source,payload,1);
            root.execSQL("INSERT INTO r29_event_outbox(event_id,issue,kind,payload_json,payload_hash,origin,committed_at,acked_at) VALUES(?,?,?,?,?,?,1,0)",new Object[]{delivery.id,issue,delivery.kind,delivery.payload,delivery.hash,delivery.origin});
            LegacyDecisionRetentionR29.AuthorityBatch first=LegacyDecisionRetentionR29.authorityCapture(root,rp,current);tx(root,()->LegacyDecisionRetentionR29.pruneAuthoritiesWithin(root,first,current),false);
            check("D17_UNDELIVERED_TERMINAL_AUTHORITY_CANNOT_BE_PRUNED",n(root,"SELECT count(*) FROM r26_feedback")==1);
            rp.execSQL("INSERT INTO r29_event_inbox VALUES(?)",new Object[]{delivery.id});LegacyDecisionRetentionR29.AuthorityBatch unacked=LegacyDecisionRetentionR29.authorityCapture(root,rp,current);tx(root,()->LegacyDecisionRetentionR29.pruneAuthoritiesWithin(root,unacked,current),false);
            check("D18_LOST_TERMINAL_ACK_RETAINS_ROOT_UNTIL_ACK_REPAIRS",n(root,"SELECT count(*) FROM r26_feedback")==1);
            root.execSQL("UPDATE r29_event_outbox SET acked_at=99");LegacyDecisionRetentionR29.AuthorityBatch complete=LegacyDecisionRetentionR29.authorityCapture(root,rp,current);tx(root,()->LegacyDecisionRetentionR29.pruneAuthoritiesWithin(root,complete,current),false);
            check("D19_ACKNOWLEDGED_UNREFERENCED_TERMINAL_RETIRES_AT_ORIGINAL_CUTOFF",n(root,"SELECT count(*) FROM r26_feedback")==0);
        }
        System.out.println("R29_DECISION_RETENTION|ALL_PASS|checks="+checks+"|HOST_SQL_ONLY_ANDROID_RETENTION_PENDING");
    }
}
