package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.FailureDispositionR31;
import com.ethk3.novaonepro.strategyv2.SchemaR27;
import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** Full production coordinator with JDBC adapters. No claim of Android timings or device process death. */
public final class R31WriteProtocolHost {
    private static DatabaseCoordinator coordinator;
    private static SQLiteDatabase db;
    private static ReentrantLock writer;
    private static void need(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void set(Object target,String name,Object value)throws Exception{Field field=DatabaseCoordinator.class.getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
    private static long scalar(Connection connection,String sql)throws Exception{try(Statement statement=connection.createStatement();ResultSet result=statement.executeQuery(sql)){need(result.next(),sql);return result.getLong(1);}}
    private static DatabaseCoordinator.WriteContext context(String operation){return DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,operation);}
    private static DatabaseCoordinator.CommitIdentityR31 identity(String key,String payload){return new DatabaseCoordinator.CommitIdentityR31(key,payload,"host-task","owner",1,"snapshot",0,"host-v1");}
    private static DatabaseCoordinator.CommitResultR31<Integer> increment(String key,String payload,AtomicInteger bodies){
        return coordinator.runIdempotentAtomicWriteR31(context("R31_HOST_INCREMENT"),identity(key,payload),tx->{
            need(tx.inTransaction()&&writer.isHeldByCurrentThread(),"same actual transaction and sole writer");
            tx.execSQL("UPDATE model SET samples=samples+1,settlements=settlements+1,parameter=parameter+7");bodies.incrementAndGet();return 9;
        },9);
    }
    private static void diagnostics(Connection connection)throws Exception{
        String[] phases={"AFTER_COMMIT","SCHEMA_LEAVE","TRANSACTION_TIMING","RECORD_SUCCESS","DB_BODY","WRITER_HOLD","WRITER_CONTINUATION_CLOSE","TRANSACTION_CONTINUATION_CLOSE","LOCK_WAIT"};
        AtomicInteger bodies=new AtomicInteger();
        for(String phase:phases){
            long old=scalar(connection,"SELECT samples FROM model");
            DatabaseCoordinator.diagnosticFaultR31.set(actual->{if(phase.equals(actual))throw new IllegalStateException("HOST_DIAGNOSTIC_"+phase);});
            DatabaseCoordinator.CommitResultR31<Integer> result;
            try{result=increment("diagnostic-"+phase,"payload-"+phase,bodies);}finally{DatabaseCoordinator.diagnosticFaultR31.remove();}
            need(result.outcome==DatabaseCoordinator.CommitOutcome.COMMITTED_DIAGNOSTIC_FAILED,phase+" must remain committed with red diagnostic: "+result.outcome);
            need(result.diagnosticFailure!=null&&result.value==9&&!writer.isLocked(),phase+" retains result and releases actual writer");
            need(scalar(connection,"SELECT samples FROM model")==old+1,phase+" increments once");
            need(increment("diagnostic-"+phase,"payload-"+phase,bodies).replay,phase+" durable replay");
            need(scalar(connection,"SELECT samples FROM model")==old+1,phase+" no duplicate after retry");
            Field current=TransactionSchemaCacheR941.class.getDeclaredField("CURRENT");current.setAccessible(true);need(((ThreadLocal<?>)current.get(null)).get()==null,phase+" leaves no schema ThreadLocal");
        }
        need(bodies.get()==phases.length,"diagnostic replay never re-enters body");
        System.out.println("R31_DIAGNOSTIC_HOST_PASS phases="+phases.length+" model_sample_settlement_once=true required_cleanup=true");
    }
    private static void unknownAndReceipt(Connection connection)throws Exception{
        AtomicInteger bodies=new AtomicInteger();int opens=SQLiteDatabase.independentOpens;
        db.failAfterCommitOnce=true;DatabaseCoordinator.CommitResultR31<Integer> committed=increment("end-unknown-commit","payload",bodies);
        need(committed.outcome==DatabaseCoordinator.CommitOutcome.COMMITTED&&committed.replay,"END exception confirmed by durable receipt");
        need(SQLiteDatabase.independentOpens>opens&&bodies.get()==1,"fresh independent reader and one body");
        need(increment("end-unknown-commit","payload",bodies).replay&&bodies.get()==1,"already committed never repeated");
        db.failBeforeCommitOnce=true;
        try{increment("end-unknown-rollback","payload",bodies);throw new AssertionError("rolled back END must reject");}
        catch(DatabaseCoordinator.WriteRejectedR31 expected){need(expected.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK,"fresh absent receipt after original transaction exit confirms rollback");}
        need(!increment("end-unknown-rollback","payload",bodies).replay,"rolled back step may redo");
        long old=scalar(connection,"SELECT samples FROM model");
        db.failAfterCommitOnce=true;SQLiteDatabase.rejectReadback=true;
        try{increment("end-unreadable","payload",bodies);throw new AssertionError("unavailable readback remains unknown");}
        catch(DatabaseCoordinator.WriteRejectedR31 expected){need(expected.commitOutcome==DatabaseCoordinator.CommitOutcome.UNKNOWN,"no false rollback on unavailable reader");need(FailureDispositionR31.classify(expected).requiresCommitResolution,"no scheduler blind replay");}
        finally{SQLiteDatabase.rejectReadback=false;}
        need(scalar(connection,"SELECT samples FROM model")==old+1,"unknown business may already have committed");
        need(increment("end-unreadable","payload",bodies).replay,"later receipt resolves without duplicate");
        try{increment("end-unreadable","different-payload",bodies);throw new AssertionError("same key different payload accepted");}
        catch(DatabaseCoordinator.WriteRejectedR31 expected){need("INTEGRITY".equals(expected.category),"payload mismatch classified integrity");}
        need(scalar(connection,"SELECT samples FROM model")==old+1,"conflict cannot mutate model");
        System.out.println("R31_UNKNOWN_RECEIPT_HOST_PASS independent_readback=true commit_ack_failure_once=true rollback_redo=true unavailable_unknown=true payload_conflict=true");
    }
    private static void compatibilityAndClassification(Connection connection)throws Exception{
        need(coordinator.runWriteOrThrowF01(context("R31_HOST_NULL"),tx->null)==null,"legitimate null success");
        need(coordinator.runAtomicWriteOrThrowF01(context("R31_HOST_ATOMIC_NULL"),tx->null)==null,"legitimate atomic null success");
        db.beginTransaction();
        try{
            coordinator.runAtomicWriteOrThrowF01(context("R31_HOST_NESTED"),tx->{need(tx.inTransaction(),"legacy nested body remains in caller transaction");tx.execSQL("UPDATE model SET samples=samples+99");return 1;});
            try{increment("nested-forbidden","p",new AtomicInteger());throw new AssertionError("durable success falsely promised in outer transaction");}
            catch(DatabaseCoordinator.WriteRejectedR31 expected){need("DEPENDENCY".equals(expected.category),"outer transaction explicit dependency");}
        }finally{db.endTransaction();}
        need(scalar(connection,"SELECT count(*) FROM r31_commit_receipt WHERE commit_key='nested-forbidden'")==0,"no nested durable receipt");
        try{coordinator.runAtomicWriteOrThrowF01(context("R31_HOST_CONSTRAINT"),tx->{tx.execSQL("INSERT INTO model VALUES(1,0,0,0)");return 0;});throw new AssertionError("constraint hidden");}
        catch(DatabaseCoordinator.WriteRejectedR31 expected){need("INTEGRITY".equals(expected.category),"native constraint classification");need(FailureDispositionR31.classify(expected).disposition==FailureDispositionR31.Disposition.FAULT,"constraint is not retry-busy");}
        Thread.currentThread().interrupt();
        try{coordinator.runAtomicWriteOrThrowF01(context("R31_HOST_CANCELLED"),tx->{throw new AssertionError("cancelled body executed");});throw new AssertionError("interrupt accepted");}
        catch(DatabaseCoordinator.WriteRejectedR31 expected){need("CANCELLED".equals(expected.category)&&Thread.currentThread().isInterrupted(),"interrupt preserved and classified cancelled");}
        finally{Thread.interrupted();}
        AtomicInteger ran=new AtomicInteger();Thread lockOwner=new Thread(()->{writer.lock();try{try{Thread.sleep(3000);}catch(InterruptedException e){Thread.currentThread().interrupt();}}finally{writer.unlock();}});
        lockOwner.start();while(!writer.isLocked())Thread.yield();
        try{coordinator.runAtomicWriteOrThrowF01(context("R31_HOST_BUSY"),tx->{ran.incrementAndGet();return 0;});throw new AssertionError("busy accepted");}
        catch(DatabaseCoordinator.WriteRejectedR31 expected){need("LOCK_BUSY".equals(expected.category),"actual sole lock wait classified busy");need(FailureDispositionR31.classify(expected).disposition==FailureDispositionR31.Disposition.RETRY_LATER,"busy not quarantined");}
        lockOwner.join();need(ran.get()==0&&!writer.isLocked(),"deadline body not run and writer finally released");
        coordinator.runAtomicWriteOrThrowF01(context("R31_HOST_AFTER_RELEASE"),tx->{ran.incrementAndGet();return 0;});need(ran.get()==1,"release resumes actual writer");
        long unknown=((Number)DatabaseCoordinator.writeProtocolSnapshotR31().get("unknownPriorityCount")).longValue();
        coordinator.runWriteOrThrowF01("UNREGISTERED_HOST_OPERATION",tx->null);
        need(((Number)DatabaseCoordinator.writeProtocolSnapshotR31().get("unknownPriorityCount")).longValue()==unknown+1,"unclassified operation observable");
        System.out.println("R31_COMPATIBILITY_HOST_PASS null=true legacy_outer_rollback=true nested_durable_rejected=true native_constraint=true interrupt=true lock_wait_retry=true unknown_priority_observable=true");
    }
    private static void processFatalBoundaries(Connection connection)throws Exception{
        for(String phase:new String[]{"AFTER_COMMIT","SCHEMA_LEAVE","TRANSACTION_TIMING","RECORD_SUCCESS","DB_BODY","WRITER_HOLD"}){
            NoClassDefFoundError original=new NoClassDefFoundError("HOST_NESTED_FATAL_"+phase);AtomicInteger bodies=new AtomicInteger();
            long old=scalar(connection,"SELECT samples FROM model");
            DatabaseCoordinator.diagnosticFaultR31.set(actual->{if(phase.equals(actual))throw new IllegalStateException("HOST_FATAL_DIAGNOSTIC_WRAPPER",original);});
            try{increment("fatal-diagnostic-"+phase,"payload",bodies);throw new AssertionError("nested linkage fatal swallowed at "+phase);}
            catch(NoClassDefFoundError actual){need(actual==original,"original nested linkage fatal identity at "+phase);}
            finally{DatabaseCoordinator.diagnosticFaultR31.remove();}
            need(!writer.isLocked()&&!db.inTransaction(),"fatal diagnostic still releases writer/transaction at "+phase);
            Field current=TransactionSchemaCacheR941.class.getDeclaredField("CURRENT");current.setAccessible(true);need(((ThreadLocal<?>)current.get(null)).get()==null,"fatal diagnostic still leaves schema scope at "+phase);
            need(scalar(connection,"SELECT samples FROM model")==old+1&&bodies.get()==1,"post-COMMIT fatal did not undo commit at "+phase);
            need(increment("fatal-diagnostic-"+phase,"payload",bodies).replay&&bodies.get()==1,"durable receipt survives propagated fatal at "+phase);
        }
        for(Error original:new Error[]{new NoClassDefFoundError("HOST_BODY_LINKAGE"),new OutOfMemoryError("HOST_BODY_SYNTHETIC_OOM")}){
            long old=scalar(connection,"SELECT samples FROM model");
            try{coordinator.runAtomicWriteOrThrowF01(context("R31_HOST_BODY_FATAL"),tx->{tx.execSQL("UPDATE model SET samples=samples+1");throw new DatabaseCoordinator.WriteRejectedR31("LOCK_BUSY","host","BODY",DatabaseCoordinator.CommitOutcome.ROLLED_BACK,new IllegalStateException("HOST_NESTED_BODY_WRAPPER",original));});throw new AssertionError("typed wrapper hid process fatal");}
            catch(Error actual){need(actual==original,"body propagates original nested process fatal");}
            need(scalar(connection,"SELECT samples FROM model")==old&&!writer.isLocked()&&!db.inTransaction(),"body fatal rolls back and releases writer");
        }
        DatabaseCoordinator.WriteRejectedR31 storage=new DatabaseCoordinator.WriteRejectedR31("DISK_FULL","host","BODY",DatabaseCoordinator.CommitOutcome.ROLLED_BACK,new IllegalStateException("SQLITE_FULL"));
        need(FailureDispositionR31.processFatalCause(storage)==null,"storage FATAL is not process fatal");
        System.out.println("R31_PROCESS_FATAL_BOUNDARY_HOST_PASS nested_diagnostic_phases=6 original_LinkageError_identity=true typed_body_LinkageError_and_synthetic_OOM_identity=true cleanup=true committed_receipt_replay=true storage_fatal_separate=true");
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");File path=new File(args[0]);
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+path.getPath())){
            db=new SQLiteDatabase(connection);for(String sql:SchemaR27.R31_WRITE_DDL)db.execSQL(sql);
            db.execSQL("CREATE TABLE IF NOT EXISTS model(id INTEGER PRIMARY KEY,samples INTEGER NOT NULL,settlements INTEGER NOT NULL,parameter INTEGER NOT NULL)");db.execSQL("INSERT OR IGNORE INTO model VALUES(1,0,0,0)");
            Field field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);Unsafe unsafe=(Unsafe)field.get(null);
            coordinator=(DatabaseCoordinator)unsafe.allocateInstance(DatabaseCoordinator.class);
            Field lock=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lock.setAccessible(true);writer=(ReentrantLock)lock.get(null);
            set(coordinator,"writeLock",writer);set(coordinator,"helper",new DbHelper(db));set(coordinator,"app",new android.app.Application(path));set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());
            if(args.length>1&&"halt-after-commit".equals(args[1])){
                DatabaseCoordinator.diagnosticFaultR31.set(phase->{if("AFTER_COMMIT".equals(phase))Runtime.getRuntime().halt(73);});
                increment("process-death-key","payload",new AtomicInteger());throw new AssertionError("halt hook did not run");
            }
            if(args.length>1&&"reopen-after-halt".equals(args[1])){
                AtomicInteger bodies=new AtomicInteger();need(increment("process-death-key","payload",bodies).replay,"new process replays durable receipt");
                need(bodies.get()==0&&scalar(connection,"SELECT samples FROM model")==1&&scalar(connection,"SELECT parameter FROM model")==7,"abrupt prior JVM death never duplicates model");
                System.out.println("R31_REAL_HOST_JVM_HALT_RECOVERY_PASS after_durable_commit_before_ack=true fresh_JVM_receipt_replay=true duplicate_model_update=0 scope=HOST_NOT_ANDROID_PROCESS_KILL");return;
            }
            diagnostics(connection);unknownAndReceipt(connection);compatibilityAndClassification(connection);processFatalBoundaries(connection);
            com.ethk3.novaonepro.strategyv2.R31QueueCancellationHost.verify(coordinator,db);
            need(scalar(connection,"SELECT settlements FROM model")==scalar(connection,"SELECT samples FROM model"),"settlement and sample advance atomically");
            need(scalar(connection,"SELECT parameter FROM model")==7*scalar(connection,"SELECT samples FROM model"),"parameters never duplicated independently");
            System.out.println("R31_WRITE_PROTOCOL_HOST_PASS scope=actual_production_sources_and_native_SQLite_HOST_adapters_NOT_Android_NOT_device_NOT_soak_NOT_natural_quality");
        }
    }
}
