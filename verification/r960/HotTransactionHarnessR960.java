package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Method;
/** Loads the actual DatabaseCoordinator class, with only Android/SQLite host adapters. */
public final class HotTransactionHarnessR960 {
 static int checks;
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
 static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 public static void main(String[] args)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
   db.execSQL("CREATE TABLE "+DbHelper.TABLE_B03_TRANSACTION_AUDIT+"(idempotency_key TEXT PRIMARY KEY,operation TEXT,game_code TEXT,target_issue TEXT,transaction_hash TEXT,status TEXT,error_detail TEXT,created_at INTEGER)");
   db.execSQL("CREATE TABLE fixture_work(value INTEGER)");long schema=n(db,"PRAGMA schema_version");
   Method m=DatabaseCoordinator.class.getDeclaredMethod("runExistingR36Transaction",SQLiteDatabase.class,DatabaseCoordinator.TransactionContext.class,DatabaseCoordinator.TransactionCallable.class,Object.class);m.setAccessible(true);
   DatabaseCoordinator.TransactionContext tx=new DatabaseCoordinator.TransactionContext("R36_ANALYSIS_CREATE","202609190001",V51ChecksumEngine.sha256("hot-transaction"));
   DatabaseCoordinator.TransactionCallable<Integer> task=(d,t)->{d.execSQL("INSERT INTO fixture_work VALUES(1)");return 1;};
   DatabaseCoordinator.TransactionResult<?> r=(DatabaseCoordinator.TransactionResult<?>)m.invoke(null,db,tx,task,0);
   check(r.success&&!r.replay&&n(db,"SELECT COUNT(*) FROM fixture_work")==1,"ACTUAL_CRITICAL_TRANSACTION_COMMITS");
   check(n(db,"PRAGMA schema_version")==schema&&n(db,"SELECT COUNT(*) FROM sqlite_master WHERE type='table'")==2,"NO_HOT_PATH_SCHEMA_CREATION_OR_MIGRATION");
   r=(DatabaseCoordinator.TransactionResult<?>)m.invoke(null,db,tx,task,0);
   check(r.success&&r.replay&&n(db,"SELECT COUNT(*) FROM fixture_work")==1,"ACTUAL_OWNER_REPLAY_DOES_NOT_DOUBLE_WRITE");
   DatabaseCoordinator.TransactionCallable<Integer> failing=(d,t)->{d.execSQL("INSERT INTO fixture_work VALUES(2)");throw new IllegalStateException("INJECTED_BUSINESS_FAILURE");};
   r=(DatabaseCoordinator.TransactionResult<?>)m.invoke(null,db,new DatabaseCoordinator.TransactionContext("R36_ANALYSIS_CLAIM","202609190002",V51ChecksumEngine.sha256("hot-failed")),failing,0);
   check(!r.success&&n(db,"SELECT COUNT(*) FROM fixture_work")==1,"ACTUAL_OWNER_ROLLS_BACK_FAILED_BUSINESS_WRITE");
   db.execSQL("CREATE TRIGGER fixture_audit_failure BEFORE INSERT ON "+DbHelper.TABLE_B03_TRANSACTION_AUDIT+" BEGIN SELECT RAISE(ABORT,'INJECTED_AUDIT_FAILURE'); END");
   r=(DatabaseCoordinator.TransactionResult<?>)m.invoke(null,db,new DatabaseCoordinator.TransactionContext("R36_ANALYSIS_CLAIM","202609190003",V51ChecksumEngine.sha256("hot-audit-failed")),task,0);
   check(!r.success&&n(db,"SELECT COUNT(*) FROM fixture_work")==1,"AUDIT_FAILURE_CANNOT_COMMIT_UNAUDITED_BUSINESS_WRITE");
   System.out.println("R960_HOT_TRANSACTION_PASS|checks="+checks+"|scope=ACTUAL_COORDINATOR_REAL_SQLITE_NOT_ANDROID_LATENCY");
  }finally{db.shutdown();}
 }
}
