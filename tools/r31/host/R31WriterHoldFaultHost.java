package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** Real delayed transactions and production receipt delivery; injected counter fixture, no quality claim. */
public final class R31WriterHoldFaultHost {
    static int checks;
    static DatabaseCoordinator coordinator;
    static SQLiteDatabase db;
    static DbStore store;
    static ReentrantLock writer;
    static void need(boolean value,String reason) { checks++; if(!value)throw new AssertionError(reason); }
    static void set(Object target,String field,Object value)throws Exception {
        Field f=DatabaseCoordinator.class.getDeclaredField(field);f.setAccessible(true);f.set(target,value);
    }
    static long scalar(String sql,String...args) {try(Cursor c=db.rawQuery(sql,args)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static long observation(String key){return ((Number)WriterHoldFaultsR31.snapshot().get(key)).longValue();}
    static DatabaseCoordinator.CommitIdentityR31 identity(String key) {
        return new DatabaseCoordinator.CommitIdentityR31(key,"payload:"+key,"injected-counter","host-owner",1,"same-original-input",0,"engineering-counter-v1");
    }
    static DatabaseCoordinator.CommitResultR31<Integer> slow(String key,AtomicInteger calls) {
        return coordinator.runIdempotentAtomicWriteR31(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"INJECTED_SLOW_COUNTER"),identity(key),tx->{
            need(writer.isHeldByCurrentThread()&&tx.inTransaction(),"actual sole writer and native transaction");
            tx.execSQL("UPDATE injected_counter SET n=n+1");calls.incrementAndGet();Thread.sleep(230);
            return 7;
        },7);
    }
    static void drainAll() {
        for(int n=0;n<WriterHoldFaultsR31.MAX_PENDING+8&&observation("pending")>0;n++)
            need(WriterHoldFaultsR31.drainOne(store),"actual durable drain: "+WriterHoldFaultsR31.snapshot());
        need(observation("pending")==0,"all currently observed faults persisted");
    }
    static void basic()throws Exception {
        AtomicInteger calls=new AtomicInteger();
        DatabaseCoordinator.CommitResultR31<Integer> result=slow("slow-original",calls);
        need(result.outcome==DatabaseCoordinator.CommitOutcome.COMMITTED,"original counter commit succeeded");
        need(!writer.isLocked()&&!db.inTransaction(),"actual unlock/transaction exit before queue inspection");
        need(observation("pending")>0&&scalar("SELECT count(*) FROM r31_runtime_fault")==0,"observation does no nested SQL before independent drain");
        need(scalar("SELECT n FROM injected_counter")==1&&calls.get()==1,"business ran exactly once");
        db.beforeSuccessful=()->{throw new IllegalStateException("INJECTED_FAULT_RECEIPT_ROLLBACK");};
        try{need(!WriterHoldFaultsR31.drainOne(store),"diagnostic transaction can fail independently");}
        finally{db.beforeSuccessful=null;}
        need(observation("pending")>0&&scalar("SELECT count(*) FROM r31_runtime_fault")==0,"rollback retains real event and no half receipt");
        need(slow("slow-original",calls).replay&&calls.get()==1,"diagnostic failure never repeats original business");
        DatabaseCoordinator.diagnosticFaultR31.set(phase->{if("AFTER_COMMIT".equals(phase))throw new IllegalStateException("INJECTED_FAULT_RECEIPT_AFTER_COMMIT");});
        try{need(WriterHoldFaultsR31.drainOne(store),"durable diagnostic receipt survives its own callback failure");}
        finally{DatabaseCoordinator.diagnosticFaultR31.remove();}
        need("COMMITTED_DIAGNOSTIC_FAILED".equals(WriterHoldFaultsR31.snapshot().get("lastDrainCommitOutcome")),"actual diagnostic commit outcome preserved");
        drainAll();
        need(scalar("SELECT count(*) FROM r31_runtime_fault WHERE operation='INJECTED_SLOW_COUNTER' AND category='WRITER_HOLD'")==1,"one precise fault for original measured hold");
        try(Cursor c=db.rawQuery("SELECT cause_chain_json,commit_outcome,stack FROM r31_runtime_fault WHERE operation='INJECTED_SLOW_COUNTER'",null)){
            need(c.moveToFirst(),"actual persisted row");
            Map<?,?> details=(Map<?,?>)JsonR27.parse(c.getString(0));
            need(((Number)details.get("writerHoldMs")).longValue()>200,"real measured writer interval exceeds threshold");
            need("UNKNOWN_NOT_INFERRED_FROM_DURATION".equals(details.get("rootCause")),"no fabricated delay attribution");
            need("NOT_CAPTURED_NO_BUSINESS_THROWABLE".equals(c.getString(2)),"no invented exception stack");
            need("COMMITTED".equals(c.getString(1)),"business outcome kept independent of later receipt failure");
        }
        need(!WriterHoldFaultsR31.drainOne(store),"empty delivery cannot duplicate receipt");
        need(observation("capacityLoss")==0&&observation("declaredQueueBytes")==0,"leases released only after actual durable delivery");
        need(scalar("SELECT n FROM injected_counter")==1&&calls.get()==1,"no duplicate final business effect");
    }
    static void schema()throws Exception {
        AtomicInteger calls=new AtomicInteger();slow("schema-slow",calls);
        coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"INJECTED_REMOVE_DIAGNOSTIC_SCHEMA"),tx->{tx.execSQL("DROP TABLE r31_runtime_fault");return 0;});
        long pending=observation("pending");need(!WriterHoldFaultsR31.drainOne(store),"missing schema does not synthesize a receipt or execute DDL");
        need(observation("pending")==pending&&calls.get()==1,"missing-source event remains queued");
        coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R31_OBSERVATION_SCHEMA_REPAIR"),tx->{SchemaDomainCoordinatorR926.migrateR31Observations(tx);return 0;});
        drainAll();need(scalar("SELECT count(*) FROM r31_runtime_fault WHERE operation='INJECTED_SLOW_COUNTER'")==1,"real migration restores independent delivery");
        need(scalar("SELECT n FROM injected_counter")==1,"schema repair did not retrain counter fixture");
    }
    static void backpressure()throws Exception {
        AtomicInteger calls=new AtomicInteger();
        for(int n=0;n<WriterHoldFaultsR31.RESEARCH_BACKPRESSURE_AT;n++)slow("pressure-"+n,calls);
        need(WriterHoldFaultsR31.deferResearch(),"real outstanding receipt queue defers lower priority writes");
        int prior=calls.get();
        try{slow("must-wait",calls);throw new AssertionError("research ran despite actual pending evidence debt");}
        catch(DatabaseCoordinator.WriteRejectedR31 expected){need("DEPENDENCY".equals(expected.category),"backpressure is a recoverable dependency");}
        need(calls.get()==prior&&observation("capacityLoss")==0,"refused body did not run or lose observations");
        coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.DRAW_INGEST,"INJECTED_CRITICAL_SENTINEL"),tx->{tx.execSQL("INSERT INTO critical_sentinel VALUES(1)");return 1;});
        need(scalar("SELECT count(*) FROM critical_sentinel")==1,"Critical is still admitted");
        need(WriterHoldFaultsR31.drainOne(store)&&!WriterHoldFaultsR31.deferResearch(),"actual durable delivery is the recovery condition");
        slow("resumed",calls);need(calls.get()==prior+1,"research really resumes without resetting task or exposure");
        drainAll();need(observation("capacityLoss")==0,"no dropped fault receipts under controlled research pressure");
        need(scalar("SELECT count(*) FROM r31_runtime_fault WHERE operation='INJECTED_SLOW_COUNTER'")==calls.get(),"each measured injected hold has a real independent fault row");
    }
    public static void main(String[]args)throws Exception {
        Class.forName("org.sqlite.JDBC");File file=new File(args[1]);
        try(Connection jdbc=DriverManager.getConnection("jdbc:sqlite:"+file.getPath())){
            db=new SQLiteDatabase(jdbc);
            Field unsafeField=Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
            coordinator=(DatabaseCoordinator)((Unsafe)unsafeField.get(null)).allocateInstance(DatabaseCoordinator.class);
            Field lock=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lock.setAccessible(true);writer=(ReentrantLock)lock.get(null);
            android.app.Application context=new android.app.Application(file);
            set(coordinator,"writeLock",writer);set(coordinator,"helper",new DbHelper(db));set(coordinator,"app",context);
            set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());
            store=new DbStore(coordinator,db,context);
            coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R31_HOLD_FAULT_REAL_MIGRATION"),tx->{
                SchemaDomainCoordinatorR926.migrateR31Observations(tx);SchemaDomainCoordinatorR926.migrateR31WriteProtocol(tx);
                tx.execSQL("CREATE TABLE injected_counter(n INTEGER NOT NULL)");tx.execSQL("INSERT INTO injected_counter VALUES(0)");
                tx.execSQL("CREATE TABLE critical_sentinel(id INTEGER PRIMARY KEY)");return 0;
            });
            drainAll();
            if("basic".equals(args[0]))basic();else if("schema".equals(args[0]))schema();else if("backpressure".equals(args[0]))backpressure();else throw new IllegalArgumentException(args[0]);
            need(!writer.isLocked()&&!db.inTransaction(),"final real resources released");
            System.out.println("R31_WRITER_HOLD_FAULT_HOST_PASS|mode="+args[0]+"|assertions="+checks+"|actualMonotonicSleeps=true|probeSubstituted=false|quality=NOT_EVALUATED|device=NOT_RUN|snapshot="+JsonR27.write(WriterHoldFaultsR31.snapshot()));
        }
    }
}
