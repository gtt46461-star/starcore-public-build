package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Research-local audited learning transactions. Root schema/events are never installed in RP.
 * The artifact, transaction receipt, local learned fact and pending Root notification share one transaction.
 * Root notification is a separate, input-CAS-checked transaction; only its acknowledgement exposes a new artifact. */
public final class LegacyLearningCommitR29 {
    private LegacyLearningCommitR29(){}
    public static final String TX="CREATE TABLE IF NOT EXISTS r29_research_tx_receipt(idempotency_key TEXT PRIMARY KEY,transaction_hash TEXT NOT NULL,operation TEXT NOT NULL,target_issue TEXT NOT NULL,committed_at INTEGER NOT NULL)";
    public static final String OUTBOX="CREATE TABLE IF NOT EXISTS r29_legacy_learned_outbox(learning_identity TEXT PRIMARY KEY,target_issue TEXT NOT NULL,source TEXT NOT NULL,pre_state_hash TEXT NOT NULL,post_state_hash TEXT NOT NULL,trained_until TEXT NOT NULL,detail TEXT NOT NULL,created_at INTEGER NOT NULL,acked_at INTEGER NOT NULL DEFAULT 0,delivery_status TEXT NOT NULL DEFAULT 'PENDING',delivery_error TEXT NOT NULL DEFAULT '')";
    private static final AtomicLong SAVEPOINT=new AtomicLong();
    public static void installResearch(SQLiteDatabase db){
        if(db==null||!ResearchRouteR29.isResearchDatabase(db))throw new IllegalStateException("R29_LEARNING_SCHEMA_REQUIRES_RESEARCH_OWNER");
        db.execSQL(TX);db.execSQL(OUTBOX);LegacyLearnedInputR29.installResearch(db);
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r29_research_tx_no_update BEFORE UPDATE ON r29_research_tx_receipt BEGIN SELECT RAISE(ABORT,'R29_RESEARCH_TX_RECEIPT_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r29_research_tx_no_delete BEFORE DELETE ON r29_research_tx_receipt BEGIN SELECT RAISE(ABORT,'R29_RESEARCH_TX_RECEIPT_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r29_learned_outbox_payload_immutable BEFORE UPDATE OF learning_identity,target_issue,source,pre_state_hash,post_state_hash,trained_until,detail,created_at ON r29_legacy_learned_outbox BEGIN SELECT RAISE(ABORT,'R29_LEARNED_OUTBOX_IMMUTABLE'); END");
    }
    public static <T>DatabaseCoordinator.TransactionResult<T> runResearchTransaction(SQLiteDatabase db,DatabaseCoordinator.TransactionContext tx,DatabaseCoordinator.TransactionCallable<T> task,T fallback){
        long started=TimeSourceR811.monoNowMs();boolean owns=!db.inTransaction(),saved=false;String point="r29_learning_"+SAVEPOINT.incrementAndGet();
        try{
            if(!tx.valid())throw new IllegalArgumentException("R29_RESEARCH_TX_CONTEXT");
            if(owns)db.beginTransactionNonExclusive();
            db.execSQL("SAVEPOINT "+point);saved=true;
            try(Cursor c=db.rawQuery("SELECT transaction_hash FROM r29_research_tx_receipt WHERE idempotency_key=?",new String[]{tx.idempotencyKey})){
                if(c.moveToFirst()){
                    if(!tx.transactionHash.equals(c.getString(0)))throw new IllegalStateException("R29_RESEARCH_TX_IDEMPOTENCY_CONFLICT");
                    db.execSQL("RELEASE "+point);saved=false;if(owns)db.setTransactionSuccessful();return DatabaseCoordinator.TransactionResult.ok(fallback,true,tx.transactionHash,TimeSourceR811.monoNowMs()-started);
                }
            }
            T value=task.call(db,tx);
            db.execSQL("INSERT INTO r29_research_tx_receipt VALUES(?,?,?,?,?)",new Object[]{tx.idempotencyKey,tx.transactionHash,tx.operation,tx.targetIssue,System.currentTimeMillis()});
            db.execSQL("RELEASE "+point);saved=false;if(owns)db.setTransactionSuccessful();return DatabaseCoordinator.TransactionResult.ok(value,false,tx.transactionHash,TimeSourceR811.monoNowMs()-started);
        }catch(Throwable failure){
            // Android SQLiteSession classifies a leading ROL as a whole-transaction abort.
            // The standard leading semicolon keeps ROLLBACK TO inside SQLite savepoint handling.
            if(saved)try{db.execSQL(";ROLLBACK TO "+point);db.execSQL("RELEASE "+point);}catch(Throwable rollback){failure.addSuppressed(rollback);throw new IllegalStateException("R29_RESEARCH_TX_ROLLBACK_FAILED",failure);}
            return DatabaseCoordinator.TransactionResult.fail(fallback,"R29_RESEARCH_TX_FAILED:"+failure,tx.transactionHash,TimeSourceR811.monoNowMs()-started,failure);
        }finally{if(owns&&db.inTransaction())db.endTransaction();}
    }
    public static void learnedWithin(SQLiteDatabase db,String issue,String source,String identity,String before,String after,String trained,String detail){
        if(!db.inTransaction()||!ResearchRouteR29.isResearchDatabase(db))throw new IllegalStateException("R29_RESEARCH_LEARNING_OWNER_TRANSACTION");
        LegacyLearnedInputR29.recordLocalWithin(db,issue,source,identity,before,after,trained,detail);
        String text=detail==null?"":detail.trim();if(text.length()>1000)text=text.substring(0,1000);
        try(Cursor c=db.rawQuery("SELECT target_issue,source,pre_state_hash,post_state_hash,trained_until,detail FROM r29_legacy_learned_outbox WHERE learning_identity=?",new String[]{identity})){
            if(c.moveToFirst()){if(!issue.equals(c.getString(0))||!source.equals(c.getString(1))||!before.equals(c.getString(2))||!after.equals(c.getString(3))||!trained.equals(c.getString(4))||!text.equals(c.getString(5)))throw new IllegalStateException("R29_RESEARCH_LEARNED_PAYLOAD_CONFLICT");return;}
        }
        db.execSQL("INSERT INTO r29_legacy_learned_outbox(learning_identity,target_issue,source,pre_state_hash,post_state_hash,trained_until,detail,created_at) VALUES(?,?,?,?,?,?,?,?)",new Object[]{identity,issue,source,before,after,trained,text,System.currentTimeMillis()});
    }
    public static String artifactVisibilitySql(SQLiteDatabase db,String alias){
        if(!ResearchRouteR29.isResearchDatabase(db))return "1=1";
        if(alias==null||!alias.matches("[a-zA-Z0-9_]+"))throw new IllegalArgumentException("R29_ARTIFACT_SQL_ALIAS");
        return "NOT EXISTS(SELECT 1 FROM r29_legacy_local_learned l WHERE l.source='ResearchAdaptiveLearnerStoreR811' AND l.target_issue="+alias+".trained_until_issue AND l.post_state_hash="+alias+".artifact_hash AND l.root_acked_at=0)";
    }
    public static boolean hasUnacknowledgedFtrl(SQLiteDatabase db){
        if(!ResearchRouteR29.isResearchDatabase(db))return false;
        try(Cursor c=db.rawQuery("SELECT 1 FROM r29_legacy_local_learned WHERE source='ResearchAdaptiveLearnerStoreR811' AND root_acked_at=0 LIMIT 1",null)){return c.moveToFirst();}
    }
    static final class Pending{String id,issue,source,before,after,trained,detail;}
    /** Only Root's original FTRL input and original event hook may acknowledge a research-local update. */
    static void deliverWithin(SQLiteDatabase root,Pending p){
        if(!root.inTransaction()||ResearchRouteR29.isResearchDatabase(root))throw new IllegalStateException("R29_LEARNED_DELIVERY_ROOT_OWNER");
        if(!"ResearchAdaptiveLearnerStoreR811".equals(p.source)||!ResearchAdaptiveLearnerStoreR811.confirmsRootLearningInput(root,p.issue,p.id,p.detail))throw new IllegalStateException("R29_LEARNED_SOURCE_INPUT_CONFLICT");
        RuntimeEventLedgerR926.Event expected=RuntimeEventLedgerR926.Event.of(p.issue,"","","LEARNED","LEARNED_EXACTLY_ONCE",RuntimeEventLedgerR926.RANK_LEARNED,"learning_identity="+p.id+"|pre_state_hash="+p.before+"|post_state_hash="+p.after+"|trained_until="+p.trained+"|detail="+p.detail,p.source,"LEARNED|"+p.issue+'|'+p.source+'|'+p.id);
        try(Cursor c=root.rawQuery("SELECT payload_hash FROM "+RuntimeEventLedgerR926.TABLE_EVENT+" WHERE dedup_key=?",new String[]{expected.dedupKey})){if(c.moveToFirst()&&!expected.payloadHash.equals(c.getString(0)))throw new IllegalStateException("R29_LEARNED_ROOT_RECEIPT_CONFLICT");}
        RuntimeEventHooksR926.learnedWithinTransaction(root,p.issue,p.source,p.id,p.before,p.after,p.trained,p.detail);
    }
    public static long drain(DbStore store){
        SQLiteDatabase root=LegacyResearchScopeR29.rawRealtime(store);if(!ResearchRouteR29.active(root))return 0;
        if(root.inTransaction()||ResearchRouteR29.writerHeldByCurrentThread())throw new IllegalStateException("R29_LEARNED_DELIVERY_CROSS_DOMAIN_TRANSACTION");
        List<Pending> rows=new ArrayList<>();
        try(Cursor c=ResearchRouteR29.reader(root).rawQuery("SELECT learning_identity,target_issue,source,pre_state_hash,post_state_hash,trained_until,detail FROM r29_legacy_learned_outbox WHERE acked_at=0 AND delivery_status='PENDING' ORDER BY created_at,learning_identity LIMIT 1",null)){
            while(c.moveToNext()){Pending p=new Pending();p.id=c.getString(0);p.issue=c.getString(1);p.source=c.getString(2);p.before=c.getString(3);p.after=c.getString(4);p.trained=c.getString(5);p.detail=c.getString(6);rows.add(p);}
        }
        long count=0;
        for(Pending p:rows){
            try{LegacyResearchScopeR29.realtimeWrite(store,"R29_RESEARCH_LEARNED_DELIVERY",db->{deliverWithin(db,p);return 0;});}
            catch(RuntimeException failure){
                if(containsMessage(failure,"SOURCE_INPUT_CONFLICT")||containsMessage(failure,"ROOT_RECEIPT_CONFLICT")){ResearchRouteR29.write(store,"R29_LEARNED_SOURCE_CONFLICT",db->{db.execSQL("UPDATE r29_legacy_learned_outbox SET delivery_status=?,delivery_error=? WHERE learning_identity=? AND acked_at=0",new Object[]{containsMessage(failure,"ROOT_RECEIPT_CONFLICT")?"ROOT_RECEIPT_CONFLICT":"SOURCE_CONFLICT",failure.toString(),p.id});return 0;});continue;}
                if(writerBusy(failure))continue;throw failure;
            }
            long ack=System.currentTimeMillis();ResearchRouteR29.write(store,"R29_RESEARCH_LEARNED_ACK",db->{db.execSQL("UPDATE r29_legacy_learned_outbox SET acked_at=CASE WHEN acked_at=0 THEN ? ELSE acked_at END,delivery_status='ACKNOWLEDGED',delivery_error='' WHERE learning_identity=?",new Object[]{ack,p.id});LegacyLearnedInputR29.acknowledgeLocalWithin(db,p.id,ack);return 0;});count++;
        }
        return count;
    }
    private static boolean writerBusy(Throwable failure){for(Throwable cause=failure;cause!=null;cause=cause.getCause()){String message=String.valueOf(cause.getMessage());if(cause instanceof android.database.sqlite.SQLiteDatabaseLockedException||message.contains("CP3_DB_BUSY")||message.contains("database is locked")||message.contains("SQLITE_BUSY"))return true;}return false;}

    private static boolean containsMessage(Throwable failure,String text){for(Throwable cause=failure;cause!=null;cause=cause.getCause())if(String.valueOf(cause.getMessage()).contains(text))return true;return false;}

}
