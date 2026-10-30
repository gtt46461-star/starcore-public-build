package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Real bridge delivery on independent SQLite files; historical ACK repair is not model training. */
public final class AcknowledgedRecoveryR29HostTest {
    static int checks;
    static void check(String id,boolean ok){System.out.println("R29_ACK_RECOVERY|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static long n(SQLiteDatabase db,String sql){return EventYieldR29HostTest.n(db,sql);}
    static String value(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static final class Fixture implements AutoCloseable {
        final EventYieldR29HostTest.Fixture base=new EventYieldR29HostTest.Fixture();
        final SQLiteDatabase root=base.root,rp;final ResearchPoolR27.Worker worker;
        final AtomicBoolean rollback=new AtomicBoolean();
        final AtomicInteger repairWrites=new AtomicInteger();
        final String schema=StatsR27.sha256("ACK_REPAIR_SCHEMA"),source=StatsR27.sha256("ACK_REPAIR_SOURCE");
        final long epoch=14;
        Fixture()throws Exception{
            Path file=Paths.get(root.getPath()).getParent().resolve(ResearchRouteR29.FILE_NAME);rp=SQLiteDatabase.openOrCreateDatabase(file.toFile(),null);
            rp.execSQL("PRAGMA journal_mode=WAL");rp.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER DEFAULT 0)");
            rp.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER NOT NULL DEFAULT 0)");
            rp.beginTransaction();try{SchemaR27.ensure(rp);rp.execSQL(ResearchRouteR29.OWNER_DDL);rp.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY',?,?,?,1)",new Object[]{schema,source,epoch});rp.setTransactionSuccessful();}finally{rp.endTransaction();}
            root.beginTransaction();try{
                root.execSQL(ResearchRouteR29.ROUTE_DDL);root.execSQL(ResearchCutoverR29.PUBLICATION);
                root.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE',?,?,?,?,1)",new Object[]{ResearchRouteR29.FILE_NAME,schema,source,epoch});
                root.execSQL("INSERT INTO r29_migration_publication VALUES(1,?,'synthetic.candidate.db','PUBLISHED','synthetic.baseline.db',?,?,'',1)",new Object[]{epoch,StatsR27.sha256("SEALED_PROOF"),source});root.setTransactionSuccessful();
            }finally{root.endTransaction();}
            ResearchWriteQueueR27 writes=new ResearchWriteQueueR27((op,batch)->{
                if(op.equals("EVENT_ACK_RECONCILIATION"))repairWrites.incrementAndGet();
                rp.beginTransaction();try{
                    ResearchWriteQueueR27.execute(rp,batch,op);
                    if(rollback.getAndSet(false))rp.execSQL("INSERT INTO deliberately_missing_ack_repair_table VALUES(1)");
                    rp.setTransactionSuccessful();
                }catch(Exception failure){throw failure instanceof RuntimeException?(RuntimeException)failure:new IllegalStateException(failure);}
                finally{rp.endTransaction();}
            },()->false);
            EventYieldR29HostTest.set(base.runtime.pool,"writes",writes);worker=base.runtime.pool.new Worker(rp,root,"actual-ack-repair-worker");
        }
        void seed(int acknowledged,int pending)throws Exception{
            root.beginTransaction();try{
                int[] numbers={1,2,3,4,5,6,7,8,9,10};
                for(int i=2;i<=acknowledged+pending;i++)ResearchEventBridgeR29.append(root,ResearchEventBridgeR29.Event.draw(0,String.format(Locale.ROOT,"20261030%04d",i),numbers,"SYNTHETIC_ROOT",100+i));
                root.execSQL("UPDATE r29_event_outbox SET acked_at=700 WHERE seq<=?",new Object[]{acknowledged});root.setTransactionSuccessful();
            }finally{root.endTransaction();}
        }
        ResearchPoolR27.Outcome deliver()throws Exception{return ResearchEventBridgeR29.deliver(worker);}
        public void close()throws Exception{rp.close();base.close();}
    }
    static long repairedCursor(SQLiteDatabase db){return n(db,"SELECT coalesce((SELECT last_seq FROM r29_event_cursor WHERE source='ROOT_ACK_RECONCILE'),0)");}
    static String inboxIdentity(SQLiteDatabase rp){StringBuilder text=new StringBuilder();try(Cursor c=rp.rawQuery("SELECT event_id,source_seq,issue,payload_hash,origin,applied_at FROM r29_event_inbox ORDER BY source_seq",null)){while(c.moveToNext())for(int i=0;i<6;i++)text.append('|').append(c.getString(i));}return StatsR27.sha256(text.toString());}
    static void legacy64(boolean oldControl)throws Exception{
        try(Fixture f=new Fixture()){
            f.seed(64,0);
            // This immutable pre-existing receipt is a synthetic fixture of an earlier real learner commit.
            // The repair may use it, but must neither create nor replace one.
            f.rp.execSQL("INSERT INTO r29_learning_receipt VALUES('ATOMNET_ONLINE','202610300001',?,3,123)",new Object[]{StatsR27.sha256("EXISTING_COMMITTED_MODEL")});
            ResearchPoolR27.Outcome first=f.deliver();
            check("A1_LEGACY_ACK_PAGE_REPAIRS_REAL_RP_INBOX",n(f.rp,"SELECT count(*) FROM r29_event_inbox")==32&&first.units==32);
            if(oldControl)return;
            check("A2_PAGE_IS_BOUNDED_AND_PREFIX_IS_DURABLE",repairedCursor(f.rp)==32&&n(f.rp,"SELECT count(*) FROM r29_learning_work")==64);
            check("A3_ONLY_EXISTING_REAL_RECEIPT_COMPLETES_WORK",n(f.rp,"SELECT count(*) FROM r29_learning_receipt")==1&&n(f.rp,"SELECT count(*) FROM r29_learning_work WHERE status='COMMITTED'")==1);
            ResearchPoolR27.Outcome second=f.deliver();
            check("A4_SECOND_PAGE_REPAIRS_ALL_LEGACY64_EXACTLY_ONCE",second.units==32&&n(f.rp,"SELECT count(*) FROM r29_event_inbox")==64&&repairedCursor(f.rp)==64&&n(f.rp,"SELECT count(*) FROM r29_learning_work")==128);
            check("A5_ROOT_ACK_TIMESTAMPS_AND_MODEL_RECEIPT_UNCHANGED",n(f.root,"SELECT count(*) FROM r29_event_outbox WHERE acked_at=700")==64&&n(f.rp,"SELECT count(*) FROM r29_learning_receipt")==1&&n(f.rp,"SELECT count(*) FROM r29_learning_work WHERE status='PENDING'")==127);
            String before=inboxIdentity(f.rp);
            try(SQLiteDatabase reopened=SQLiteDatabase.openDatabase(f.rp.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
                ResearchPoolR27.Worker worker=f.base.runtime.pool.new Worker(reopened,f.root,"reopened-ack-repair-worker");
                ResearchPoolR27.Outcome again=ResearchEventBridgeR29.deliver(worker);
                check("A6_REOPEN_RETRY_PRESERVES_PREFIX_IDENTITIES_AND_APPLIED_TIMES",again.units==0&&repairedCursor(reopened)==64&&before.equals(inboxIdentity(reopened)));
            }
            check("A7_REPAIR_NEVER_REWRITES_ROOT_PAYLOAD_OR_ACK",n(f.root,"SELECT count(*) FROM r29_event_outbox")==64&&n(f.root,"SELECT count(*) FROM r29_event_outbox WHERE acked_at=700")==64);
        }
    }
    static void fairness()throws Exception{
        try(Fixture f=new Fixture()){
            f.seed(64,5);ResearchPoolR27.Outcome out=f.deliver();
            check("A8_REPAIR_PAGE_DOES_NOT_STARVE_NORMAL_PENDING_DELIVERY",out.units==37&&n(f.rp,"SELECT count(*) FROM r29_event_inbox")==37&&n(f.root,"SELECT count(*) FROM r29_event_outbox WHERE seq>64 AND acked_at>0")==5&&repairedCursor(f.rp)==32);
            check("A9_EXISTING_ACK_TIMESTAMPS_SURVIVE_NORMAL_DELIVERY",n(f.root,"SELECT count(*) FROM r29_event_outbox WHERE seq<=64 AND acked_at=700")==64);
        }
        try(Fixture f=new Fixture()){
            f.seed(0,33);f.deliver();f.deliver();f.deliver();
            check("A17_FRESH_NORMAL_ACKS_DO_NOT_CREATE_ENDLESS_REPAIR_WORK",n(f.root,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0")==33&&n(f.rp,"SELECT count(*) FROM r29_event_inbox")==33&&f.repairWrites.get()==1&&n(f.rp,"SELECT last_seq FROM r29_event_cursor WHERE source='ROOT_ACK_RECONCILE_BOUNDARY'")==0&&repairedCursor(f.rp)==0);
        }
    }
    static void conflictAndRollback()throws Exception{
        try(Fixture f=new Fixture()){
            f.seed(32,0);String id,issue,hash,origin;long seq;
            try(Cursor c=f.root.rawQuery("SELECT event_id,seq,issue,payload_hash,origin FROM r29_event_outbox ORDER BY seq LIMIT 1",null)){c.moveToFirst();id=c.getString(0);seq=c.getLong(1);issue=c.getString(2);hash=c.getString(3);origin=c.getString(4);}
            f.rp.execSQL("INSERT INTO r29_event_inbox VALUES(?,?,?,?,?,1)",new Object[]{id,seq,issue,hash,origin});
            for(String column:new String[]{"source_seq","issue","origin"}){
                Object wrong=column.equals("source_seq")?999L:column.equals("issue")?"202610300099":"FORGED_ORIGIN";
                f.rp.execSQL("UPDATE r29_event_inbox SET "+column+"=? WHERE event_id=?",new Object[]{wrong,id});
                boolean rejected=false;try{f.deliver();}catch(IllegalStateException expected){rejected=expected.getMessage().contains("R29_INBOX_IDENTITY_CONFLICT");}
                check("A10_FULL_IDENTITY_CONFLICT_"+column+"_ROLLS_BACK_PREFIX",rejected&&repairedCursor(f.rp)==0&&n(f.rp,"SELECT count(*) FROM r29_event_inbox")==1&&n(f.rp,"SELECT count(*) FROM r29_learning_work")==0);
                f.rp.execSQL("UPDATE r29_event_inbox SET source_seq=?,issue=?,origin=? WHERE event_id=?",new Object[]{seq,issue,origin,id});
            }
            ResearchPoolR27.Outcome good=f.deliver();
            check("A11_EXISTING_VALID_INBOX_IS_NOT_COUNTED_AS_NEW_REPAIR",good.units==31&&n(f.rp,"SELECT count(*) FROM r29_event_inbox")==32&&repairedCursor(f.rp)==32);
        }
        try(Fixture f=new Fixture()){
            f.seed(32,0);f.rollback.set(true);boolean rejected=false;
            try{f.deliver();}catch(android.database.sqlite.SQLiteException expected){rejected=expected.getMessage().contains("deliberately_missing_ack_repair_table");}
            check("A12_REAL_SQL_FAILURE_ROLLS_BACK_INBOX_WORK_AND_REPAIR_CURSOR",rejected&&n(f.rp,"SELECT count(*) FROM r29_event_inbox")==0&&n(f.rp,"SELECT count(*) FROM r29_learning_work")==0&&repairedCursor(f.rp)==0&&n(f.root,"SELECT count(*) FROM r29_event_outbox WHERE acked_at=700")==32);
            check("A13_RETRY_AFTER_ROLLBACK_COMMITS_ONLY_MISSING_PAGE",f.deliver().units==32&&n(f.rp,"SELECT count(*) FROM r29_event_inbox")==32&&repairedCursor(f.rp)==32);
        }
    }
    static void failClosed()throws Exception{
        try(Fixture f=new Fixture()){
            f.seed(32,0);f.root.execSQL("DROP TRIGGER r29_event_payload_immutable");f.root.execSQL("UPDATE r29_event_outbox SET payload_json='{}' WHERE seq=1");boolean rejected=false;
            try{f.deliver();}catch(IllegalArgumentException expected){rejected=expected.getMessage().contains("R29_EVENT_CHECKSUM_OR_IDENTITY");}
            check("A14_CORRUPT_ROOT_PAYLOAD_IS_NOT_REPAIRED_OR_SKIPPED",rejected&&n(f.rp,"SELECT count(*) FROM r29_event_inbox")==0&&repairedCursor(f.rp)==0);
        }
        try(Fixture f=new Fixture()){
            f.seed(32,0);f.rp.execSQL("UPDATE r29_research_owner SET source_root=?",new Object[]{StatsR27.sha256("WRONG_OWNER")});boolean rejected=false;
            try{f.deliver();}catch(IllegalStateException expected){rejected=expected.getMessage().contains("OWNER_MISMATCH");}
            check("A15_WRONG_ACTIVE_OWNER_CANNOT_CONSUME_ACK_HISTORY",rejected&&n(f.rp,"SELECT count(*) FROM r29_event_inbox")==0&&repairedCursor(f.rp)==0);
        }
        try(Fixture f=new Fixture()){
            f.seed(32,0);f.root.execSQL("DELETE FROM r29_database_route");
            check("A16_NOT_ACTIVE_NEVER_CLAIMS_LEGACY_ACK_RECOVERY",f.deliver().units==0&&n(f.rp,"SELECT count(*) FROM r29_event_inbox")==0&&repairedCursor(f.rp)==0);
        }
    }
    public static void main(String[]args)throws Exception{
        boolean old=args.length>0&&args[0].equals("--old-84-control");legacy64(old);if(old)return;
        fairness();conflictAndRollback();failClosed();
        System.out.println("R29_ACK_RECOVERY|ALL_PASS|checks="+checks+"|ACTUAL_TWO_FILE_EVENT_RECOVERY_HOST_NOT_ANDROID_ACCEPTANCE");
    }
}
