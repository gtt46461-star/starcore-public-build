package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;
import java.util.List;

/** Production outbox/inbox/model/settlement SQL on TWO SQLite databases. Not a claim that runtime cutover is done. */
public final class EventBridgeR29HostTest {
    static final String ISSUE = "202610300001";
    static final int[] NUMBERS = {1,2,3,4,5,6,7,8,9,10};
    static int checks;
    static void check(String name, boolean ok) {
        System.out.println("R29_EVENT|" + name + "|" + (ok ? "OK" : "FAILED"));
        if (!ok) throw new AssertionError(name); checks++;
    }
    static SQLiteDatabase open(boolean research) throws Exception {
        SQLiteDatabase db = new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"));
        db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER NOT NULL DEFAULT 0)");
        if (research) for (String sql : SchemaR27.DDL) db.execSQL(sql);
        else tx(db, () -> ResearchEventBridgeR29.ensureRealtime(db), false);
        return db;
    }
    interface Work { void run() throws Exception; }
    static void tx(SQLiteDatabase db, Work work, boolean kill) throws Exception {
        db.jdbc.setAutoCommit(false);
        try { work.run(); if (kill) throw new IllegalStateException("SIMULATED_KILL"); db.jdbc.commit(); }
        catch (Exception e) { db.jdbc.rollback(); throw e; }
        finally { db.jdbc.setAutoCommit(true); }
    }
    static void batch(SQLiteDatabase db, ResearchWriteQueueR27.Batch b, boolean kill) throws Exception {
        tx(db, () -> ResearchWriteQueueR27.execute(db,b,"EVENT_HOST"), kill);
    }
    static long count(SQLiteDatabase db, String sql) {
        try (Cursor c = db.rawQuery(sql,null)) { if (!c.moveToFirst()) return 0; return c.getLong(0); }
    }
    static void draw(SQLiteDatabase db, String issue) {
        db.execSQL("INSERT OR IGNORE INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,0)",new Object[]{issue,1,2,3,4,5,6,7,8,9,10});
    }
    static void publish(SQLiteDatabase realtime, String issue, boolean kill) throws Exception {
        tx(realtime, () -> { draw(realtime,issue);ResearchEventBridgeR29.appendCanonical(realtime,issue,true,100); }, kill);
    }
    static ModelArtifactR29 learned(String issue) {
        AtomNetTrainerR27 tr = new AtomNetTrainerR27(8,29);FtrlR27 f = new FtrlR27(7,.05,1,.0001,.001);
        double[] x={.1,.2,.3,.4,.5,.6,.7,.8};
        tr.online(new AtomNetTrainerR27.Sample(x,1,0,issue));f.update(java.util.Arrays.copyOf(x,7),1);
        return new ModelArtifactR29(tr,f,"EVENT_HOST_DATASET",issue);
    }
    public static void main(String[] args) throws Exception {
        SQLiteDatabase rt=open(false),research=open(true);
        try { publish(rt,ISSUE,true);throw new AssertionError("kill lost"); } catch (IllegalStateException expected) { }
        check("E1_DRAW_AND_OUTBOX_ROLL_BACK_TOGETHER",count(rt,"SELECT count(*) FROM draws")==0 && count(rt,"SELECT count(*) FROM r29_event_outbox")==0);
        publish(rt,ISSUE,false);
        tx(rt,()->ResearchEventBridgeR29.appendCanonical(rt,ISSUE,false,200),false);
        List<ResearchEventBridgeR29.Event> events=ResearchEventBridgeR29.pending(rt);
        check("E2_DUPLICATE_DRAW_ONE_IMMUTABLE_EVENT",events.size()==1 && count(rt,"SELECT count(*) FROM r29_event_outbox")==1);
        try { batch(research,ResearchEventBridgeR29.prepare(events,300),true);throw new AssertionError("kill lost"); } catch (IllegalStateException expected) { }
        check("E3_INBOX_WORK_AND_CURSOR_ROLL_BACK",count(research,"SELECT count(*) FROM r29_event_inbox")==0 && count(research,"SELECT count(*) FROM r29_learning_work")==0 && count(research,"SELECT count(*) FROM r29_event_cursor")==0 && ResearchEventBridgeR29.pending(rt).size()==1);
        batch(research,ResearchEventBridgeR29.prepare(events,400),false);
        batch(research,ResearchEventBridgeR29.prepare(ResearchEventBridgeR29.pending(rt),500),false); // ack lost
        check("E4_ACK_LOSS_REDELIVERY_IS_IDEMPOTENT",count(research,"SELECT count(*) FROM r29_event_inbox")==1 && count(research,"SELECT count(*) FROM r29_learning_work")==2 && count(research,"SELECT last_seq FROM r29_event_cursor")==events.get(0).seq);
        check("E5_DELIVERY_IS_NOT_TRAINING",count(research,"SELECT count(*) FROM r29_learning_receipt")==0 && count(research,"SELECT count(*) FROM r29_learning_work WHERE status='PENDING'")==2);
        ModelArtifactR29 state=learned(ISSUE);ResearchWriteQueueR27.Batch learning=new ResearchWriteQueueR27.Batch();
        String hash=AtomWorldR27.checkpoint(learning,state,600);
        ResearchEventBridgeR29.modelCommitted(learning,ISSUE,hash,1,600);
        try { batch(research,learning,true);throw new AssertionError("kill lost"); } catch (IllegalStateException expected) { }
        check("E6_MODEL_CURSOR_RECEIPT_AND_WORK_ROLL_BACK",count(research,"SELECT count(*) FROM r29_model_checkpoint")==0 && count(research,"SELECT count(*) FROM r29_learning_receipt")==0 && count(research,"SELECT count(*) FROM r29_learning_work WHERE status='COMMITTED'")==0);
        // Keep an unrelated realtime writer transaction open. This protocol's research commit uses only its own DB.
        rt.jdbc.setAutoCommit(false);rt.execSQL("UPDATE draws SET dirty=dirty WHERE issue=?",new Object[]{ISSUE});
        try { batch(research,learning,false); } finally { rt.jdbc.rollback();rt.jdbc.setAutoCommit(true); }
        batch(research,learning,false); // killed client retries the same completed unit
        check("E7_MODEL_RECEIPT_COMMITTED_ONCE_IN_RESEARCH_DB",count(research,"SELECT count(*) FROM r29_learning_receipt WHERE consumer='ATOMNET_ONLINE'")==1 && count(research,"SELECT count(*) FROM r29_learning_work WHERE status='COMMITTED'")==1);
        try (Cursor c=research.rawQuery("SELECT state_hash,artifact FROM r29_model_checkpoint",null)) {
            check("E8_RECEIPT_MATCHES_REAL_LEARNED_ARTIFACT",c.moveToFirst() && hash.equals(c.getString(0)) && java.util.Arrays.equals(state.encode(),c.getBlob(1)) && state.trainer.onlineSteps==1 && state.ftrl.updates==1);
        }
        tx(rt,()->ResearchEventBridgeR29.acknowledge(rt,events,700),false);
        tx(rt,()->ResearchEventBridgeR29.acknowledge(rt,events,800),false);
        check("E9_ACK_IS_RETRY_SAFE",ResearchEventBridgeR29.pending(rt).isEmpty() && count(rt,"SELECT acked_at FROM r29_event_outbox")==700);
        boolean rejected=false;
        try { rt.execSQL("UPDATE r29_event_outbox SET payload_hash='tampered'"); } catch(RuntimeException expected){rejected=true;}
        check("E10_OUTBOX_PAYLOAD_IS_IMMUTABLE",rejected);
        SQLiteDatabase conflict=open(true);
        conflict.execSQL("INSERT INTO r29_event_inbox VALUES(?,1,?,'wrong','NEW_CANONICAL_COMMIT',1)",new Object[]{events.get(0).id,ISSUE});
        rejected=false;try { batch(conflict,ResearchEventBridgeR29.prepare(events,900),false); } catch(IllegalStateException expected){rejected=true;}
        check("E11_CONFLICT_STOPS_BEFORE_WORK_OR_CURSOR",rejected && count(conflict,"SELECT count(*) FROM r29_learning_work")==0 && count(conflict,"SELECT count(*) FROM r29_event_cursor")==0);
        // Delivery can arrive after an already committed online update.
        SQLiteDatabase late=open(true);batch(late,learning,false);batch(late,ResearchEventBridgeR29.prepare(events,1000),false);
        check("E12_LEARNER_BEFORE_DELIVERY_RECONCILES_WITH_RECEIPT",count(late,"SELECT count(*) FROM r29_learning_work WHERE consumer='ATOMNET_ONLINE' AND status='COMMITTED'")==1);
        draw(research,ISSUE);
        research.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit,settled_at) VALUES('A',1,?,1,?,1,2,3,1,'FROZEN',1000,10000,-1,0)",new Object[]{ISSUE,StoreR27.pack(new double[10])});
        check("E13_PENDING_FREEZE_IS_NOT_COMPLETED",ResearchEventBridgeR29.reconcileSettlements(research,1100).isEmpty());
        long[] settled={0};ResearchWriteQueueR27.Batch settlement=LiveShadowsR27.settleBatch(research,1,settled);
        try { batch(research,settlement,true);throw new AssertionError("kill lost"); } catch(IllegalStateException expected){}
        check("E14_SETTLEMENT_AND_RECEIPT_ROLL_BACK_TOGETHER",count(research,"SELECT hit FROM r27_live_frozen WHERE policy_hash='A'")==-1 && count(research,"SELECT count(*) FROM r29_learning_receipt WHERE consumer='SHADOW_SETTLEMENT'")==0);
        batch(research,LiveShadowsR27.settleBatch(research,1,new long[1]),false);
        check("E15_SETTLEMENT_RECEIPT_REFERENCES_COMMITTED_ROWS",count(research,"SELECT hit FROM r27_live_frozen WHERE policy_hash='A'")==1 && count(research,"SELECT count(*) FROM r29_learning_receipt WHERE consumer='SHADOW_SETTLEMENT'")==1 && count(research,"SELECT count(*) FROM r29_learning_work WHERE status='COMMITTED'")==2);
        rejected=false;try { research.execSQL("DELETE FROM r29_learning_receipt"); } catch(RuntimeException expected){rejected=true;}
        check("E16_COMPLETION_EVIDENCE_CANNOT_BE_DELETED",rejected);
        SQLiteDatabase forged=open(true);ResearchWriteQueueR27.Batch noModel=new ResearchWriteQueueR27.Batch();
        ResearchEventBridgeR29.modelCommitted(noModel,ISSUE,hash,1,1200);
        rejected=false;try{batch(forged,noModel,false);}catch(IllegalStateException expected){rejected=true;}
        check("E17_RECEIPT_WITHOUT_REAL_MODEL_ROLLS_BACK",rejected&&count(forged,"SELECT count(*) FROM r29_learning_receipt")==0);
        ModelArtifactR29 other=learned(ISSUE);other.trainer.online(new AtomNetTrainerR27.Sample(new double[8],0,1,ISSUE));
        ResearchWriteQueueR27.Batch conflictingModel=new ResearchWriteQueueR27.Batch();String changedHash=AtomWorldR27.checkpoint(conflictingModel,other,1300);
        ResearchEventBridgeR29.modelCommitted(conflictingModel,ISSUE,changedHash,1,1300);
        rejected=false;try{batch(research,conflictingModel,false);}catch(IllegalStateException expected){rejected=true;}
        boolean original=false;try(Cursor c=research.rawQuery("SELECT state_hash FROM r29_model_checkpoint",null)){original=c.moveToFirst()&&hash.equals(c.getString(0));}
        check("E18_CONFLICTING_SECOND_MODEL_CANNOT_REWRITE_RECEIPT",rejected&&original);
        rt.jdbc.close();research.jdbc.close();conflict.jdbc.close();late.jdbc.close();forged.jdbc.close();
        System.out.println("R29_EVENT|ALL_PASS|checks="+checks+"|HOST_PROTOCOL_ONLY_RUNTIME_DB_CUTOVER_PENDING");
    }
}
