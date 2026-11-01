package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import java.sql.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;
/** Actual production coordinator and sole lock with native SQLite deferred-COMMIT failure. HOST, not Android. */
public final class RepairCommitFailureR30HostTest {
    static void need(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static void set(Object target,String name,Object value)throws Exception{Field field=DatabaseCoordinator.class.getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
    static long count(Connection connection)throws Exception{try(Statement s=connection.createStatement();ResultSet r=s.executeQuery("SELECT count(*) FROM child")){return r.getLong(1);}}
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
            System.out.println("REPAIR_REAL_NATIVE_COMMIT_FAILURE_HOST_PASS actual_production_coordinator_and_sole_writer=1 deferred_foreign_key_at_COMMIT=1 no_false_success=1 rollback_rows=0 healthy_rows=1 failed_attempt_retained=1 telemetry_after_unlock=1 scope=HOST_Android_adapters_NOT_phone_or_natural");
        }
    }
}
