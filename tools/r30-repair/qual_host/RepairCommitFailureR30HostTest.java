package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import java.sql.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;
/** Actual production coordinator and sole lock with native SQLite FULL and deferred-COMMIT failures. HOST, not Android. */
public final class RepairCommitFailureR30HostTest {
    static void need(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static void set(Object target,String name,Object value)throws Exception{Field field=DatabaseCoordinator.class.getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
    static long scalar(Connection connection,String sql)throws Exception{try(Statement s=connection.createStatement();ResultSet r=s.executeQuery(sql)){need(r.next(),sql);return r.getLong(1);}}
    static long count(Connection connection)throws Exception{return scalar(connection,"SELECT count(*) FROM child");}
    static void atomicFailures(DatabaseCoordinator coordinator,SQLiteDatabase db,Connection connection,ReentrantLock lock)throws Exception{
        try{
            coordinator.runAtomicWriteOrThrowF01("R30_REPAIR_HOST_ATOMIC_DEFERRED_COMMIT",database->{
                need(lock.isHeldByCurrentThread()&&database.inTransaction(),"atomic COMMIT uses original writer and transaction");
                database.execSQL("INSERT INTO child VALUES(2,999)");return 1;
            });
            throw new AssertionError("atomic deferred foreign-key COMMIT must fail");
        }catch(IllegalStateException failure){
            need(failure.getCause() instanceof android.database.sqlite.SQLiteException
                    &&failure.getCause().getMessage().contains("FOREIGN KEY constraint failed"),"successful body must retain real COMMIT failure: "+failure);
        }
        need(count(connection)==1&&!db.inTransaction()&&!lock.isLocked(),"failed atomic COMMIT rolls back and releases original writer");
        coordinator.runAtomicWriteOrThrowF01("R30_REPAIR_HOST_FULL_SCHEMA",database->{database.execSQL("CREATE TABLE full_fixture(id INTEGER PRIMARY KEY,payload BLOB)");return 0;});
        final long[] originalLimit={0};final SQLException[] originalFull={null};
        try{
            coordinator.runAtomicWriteOrThrowF01("R30_REPAIR_HOST_ACTUAL_FULL",database->{
                need(lock.isHeldByCurrentThread()&&database.inTransaction(),"FULL stays on original writer and transaction");
                long pages=scalar(connection,"PRAGMA page_count"),pageSize=scalar(connection,"PRAGMA page_size");
                originalLimit[0]=scalar(connection,"PRAGMA max_page_count");
                need(scalar(connection,"PRAGMA max_page_count="+pages)==pages,"FULL limit pinned to original writer connection");
                database.execSQL("INSERT INTO full_fixture VALUES(1,zeroblob(1))");
                // Native JDBC exposes the real SQLite error code; the HOST Android adapter erases it.
                try(PreparedStatement insert=connection.prepareStatement("INSERT INTO full_fixture VALUES(2,zeroblob(?))")){
                    insert.setLong(1,(pages+1)*pageSize);
                    try{insert.execute();}catch(SQLException full){originalFull[0]=full;throw full;}
                }
                throw new AssertionError("native SQLITE_FULL must fail within original transaction");
            });
            throw new AssertionError("original coordinator must report SQLITE_FULL");
        }catch(IllegalStateException failure){
            need(originalFull[0]!=null&&originalFull[0].getErrorCode()==13,"actual native body failure must be SQLITE_FULL");
            need(failure.getCause()==originalFull[0],"rollback cleanup must not replace original SQLITE_FULL: "+failure.getCause());
            boolean rollbackCleanup=false;
            for(Throwable cleanup:originalFull[0].getSuppressed())if(String.valueOf(cleanup.getMessage()).contains("no transaction is active"))rollbackCleanup=true;
            need(rollbackCleanup,"native FULL auto-rollback cleanup remains suppressed on original failure");
        }finally{
            if(originalLimit[0]>0)coordinator.runAtomicWriteOrThrowF01("R30_REPAIR_HOST_FULL_RESTORE",database->{need(scalar(connection,"PRAGMA max_page_count="+originalLimit[0])==originalLimit[0],"original FULL capacity restored");return 0;});
        }
        need(scalar(connection,"SELECT count(*) FROM full_fixture")==0&&!db.inTransaction()&&!lock.isLocked(),"FULL leaves no half batch or writer ownership");
        AtomicInteger successfulRetries=new AtomicInteger();
        DatabaseCoordinator.WriteCallable<Integer> retry=database->{
            need(lock.isHeldByCurrentThread()&&database.inTransaction(),"retry retains original writer transaction");
            if(scalar(connection,"SELECT count(*) FROM full_fixture")!=0)throw new IllegalStateException("HOST_FULL_RETRY_ALREADY_APPLIED");
            database.execSQL("INSERT INTO full_fixture VALUES(1,zeroblob(1))");successfulRetries.incrementAndGet();return 1;
        };
        need(coordinator.runAtomicWriteOrThrowF01("R30_REPAIR_HOST_FULL_RETRY",retry)==1,"healthy FULL retry commits");
        try{coordinator.runAtomicWriteOrThrowF01("R30_REPAIR_HOST_FULL_DUPLICATE",retry);throw new AssertionError("repeated retry must retain original guard");}
        catch(IllegalStateException failure){need(failure.getCause()!=null&&"HOST_FULL_RETRY_ALREADY_APPLIED".equals(failure.getCause().getMessage()),"original retry guard remains the failure");}
        need(successfulRetries.get()==1&&scalar(connection,"SELECT count(*) FROM full_fixture")==1&&!lock.isLocked(),"FULL recovery commits once and duplicate guard leaves writer released");
        System.out.println("REPAIR_ATOMIC_NATIVE_FULL_HOST_PASS actual_production_coordinator_and_sole_writer=1 native_full_error13_retained_as_original_cause=1 auto_rollback_cleanup_suppressed=1 real_atomic_COMMIT_failure_retained=1 no_half_batch=1 retry_commits_once=1 duplicate_guard_preserved=1 scope=HOST_NOT_Android_exception_type_or_phone_or_natural");
    }
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
        boolean baseline=args.length>0&&args[0].equals("baseline");Class.forName("org.sqlite.JDBC");
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){
            try(Statement s=connection.createStatement()){s.execute("PRAGMA foreign_keys=ON");s.execute("CREATE TABLE parent(id INTEGER PRIMARY KEY)");s.execute("CREATE TABLE child(id INTEGER PRIMARY KEY,parent_id INTEGER REFERENCES parent(id) DEFERRABLE INITIALLY DEFERRED)");}
            SQLiteDatabase db=new SQLiteDatabase(connection);Field uf=Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);Unsafe unsafe=(Unsafe)uf.get(null);DatabaseCoordinator coordinator=(DatabaseCoordinator)unsafe.allocateInstance(DatabaseCoordinator.class);
            Field lf=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lf.setAccessible(true);ReentrantLock lock=(ReentrantLock)lf.get(null);set(coordinator,"writeLock",lock);set(coordinator,"helper",new DbHelper(db));set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());
            com.ethk3.novaonepro.strategyv2.WriterHoldLedgerR30.observedLock=lock;
            Method core=DatabaseCoordinator.class.getDeclaredMethod("runSettlementCoreTransactionR911",String.class,String.class,DatabaseCoordinator.TransactionCallable.class,Object.class);core.setAccessible(true);
            DatabaseCoordinator.TransactionCallable<Boolean> work=(database,context)->{database.execSQL("INSERT INTO child VALUES(1,7)");return true;};
            DatabaseCoordinator.TransactionResult<Boolean> failed=(DatabaseCoordinator.TransactionResult<Boolean>)core.invoke(coordinator,"202611010001","SYNTHETIC_COMMIT_FAILURE_123456789",work,Boolean.FALSE);
            need(count(connection)==0,"native deferred foreign-key failure rolled back actual row");need(!lock.isHeldByCurrentThread()&&!lock.isLocked(),"sole writer released");
            if(baseline){need(failed.success,"baseline false-success reproduction missing");need(com.ethk3.novaonepro.strategyv2.WriterHoldLedgerR30.observedWhileLocked>0,"baseline lock ordering reproduction missing");System.out.println("BASELINE_REAL_NATIVE_COMMIT_FAILURE_REPRODUCED false_success=true committed_rows=0 telemetry_called_while_writer_locked=true");return;}
            need(!failed.success,"COMMIT failure must not report success");need(com.ethk3.novaonepro.strategyv2.WriterHoldLedgerR30.observedFailures>0,"actual failed write remains a failure");need(com.ethk3.novaonepro.strategyv2.WriterHoldLedgerR30.observedWhileLocked==0,"telemetry runs only after original writer release");
            try(Statement s=connection.createStatement()){s.execute("INSERT INTO parent VALUES(7)");}
            DatabaseCoordinator.TransactionResult<Boolean> good=(DatabaseCoordinator.TransactionResult<Boolean>)core.invoke(coordinator,"202611010001","SYNTHETIC_COMMIT_SUCCESS_123456789",work,Boolean.FALSE);need(good.success&&count(connection)==1,"healthy original core commit still succeeds");need(com.ethk3.novaonepro.strategyv2.WriterHoldLedgerR30.observedWhileLocked==0,"healthy write telemetry outside writer too");
            atomicFailures(coordinator,db,connection,lock);
            System.out.println("REPAIR_REAL_NATIVE_COMMIT_FAILURE_HOST_PASS actual_production_coordinator_and_sole_writer=1 deferred_foreign_key_at_COMMIT=1 no_false_success=1 rollback_rows=0 healthy_rows=1 failed_attempt_retained=1 telemetry_after_unlock=1 scope=HOST_Android_adapters_NOT_phone_or_natural");
        }
    }
}
