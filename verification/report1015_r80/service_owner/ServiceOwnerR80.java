package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.util.*;
public final class ServiceOwnerR80 {
 static int checks;static void ok(boolean b,String n){if(!b)throw new AssertionError(n);checks++;System.out.println("PASS|"+n);}
 static long count(SQLiteDatabase d,String table){try(Cursor c=d.rawQuery("SELECT count(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 public static void main(String[] a)throws Exception{
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   DbStore store=HostProductR974.store(db);java.lang.reflect.Field shared=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");shared.setAccessible(true);HostProductR974.set(store.coordinatorR36(),"writeLock",shared.get(null));Models.Draw draw=new Models.Draw();draw.issue="202610150661";draw.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};draw.openTime=System.currentTimeMillis()+(a[2].equals("expiry")?-59000:0);draw.fetchTime=draw.openTime+1;
   db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{draw.issue,draw.openTime,draw.fetchTime,draw.fetchTime});
   long deadline=TargetIssueResolver.predrawDeadlineWallR20261008(draw);String target=TargetIssueResolver.next(draw.issue).targetIssue;List<AnalysisSingleFlightR36.Request> callbacks=new ArrayList<>();BusinessReconcilerR36 reconciler=new BusinessReconcilerR36();
   if(a[2].equals("integrity")){
    db.execSQL("CREATE TRIGGER r80_integrity_fault BEFORE INSERT ON "+DbHelper.TABLE_R36_ANALYSIS_REQUEST+" BEGIN SELECT RAISE(ABORT,'R80_HASH_MISMATCH'); END");
    boolean thrown=false;try{reconciler.reconcile(store,draw,"R80_INTEGRITY",callbacks::add);}catch(IllegalStateException e){System.out.println("EXPECTED|"+e.getMessage());thrown=!BusinessReconcilerR36.retryableRequestContention(e);}
    ok(thrown,"integrity_failure_propagates_from_actual_reconciler");ok(callbacks.isEmpty(),"integrity_does_not_submit_analysis");ok(count(db,DbHelper.TABLE_R36_ANALYSIS_REQUEST)==0,"integrity_rolls_back_request");
   }else{
    java.util.concurrent.CountDownLatch held=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
    Thread holder=null;if(a[2].equals("lock")){holder=new Thread(()->{try(DatabaseCoordinator.WriterGuardCP3 guard=DatabaseCoordinator.acquireWriterGuardCP3()){held.countDown();release.await();}catch(Exception e){throw new RuntimeException(e);}});holder.start();held.await();}
    try(DatabaseCoordinator.WriterDeadlineScopeCP3 scope=DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs()+(holder==null?-1:150))){
     BusinessReconcilerR36.ReconcileResult r=reconciler.reconcile(store,draw,"R80_CONTENTION",callbacks::add);
     ok(r.status.startsWith("ANALYSIS_RETRY_DB_BUSY"),"actual_reconciler_reports_deferred_contention");ok(r.targetIssue.equals(target),"deferred_target_unchanged");ok(callbacks.isEmpty(),"failed_write_not_submitted");ok(count(db,DbHelper.TABLE_R36_ANALYSIS_REQUEST)==0,"failed_write_not_durable");
    }
    finally{release.countDown();if(holder!=null)holder.join();}
    if(a[2].equals("expiry"))Thread.sleep(Math.max(0,deadline-System.currentTimeMillis()+50));
    BusinessReconcilerR36.ReconcileResult retry=reconciler.reconcile(store,draw,"R80_RETRY",callbacks::add);ok(retry.targetIssue.equals(target),"retry_same_target");
    if(a[2].equals("expiry")){ok(retry.status.startsWith("TARGET_EXPIRED_WAIT_LATEST"),"expired_retry_not_renewed");ok(callbacks.isEmpty(),"expired_retry_not_submitted");ok(count(db,DbHelper.TABLE_R36_ANALYSIS_REQUEST)==0,"expired_retry_no_request");}
    else{ok(retry.status.startsWith("ANALYSIS_REQUESTED"),"retry_recovers_at_actual_reconciler");ok(callbacks.size()==1,"retry_submits_exactly_one_request");ok(callbacks.get(0).deadlineAt==deadline,"retry_keeps_fixed_draw_deadline");ok(callbacks.get(0).targetIssue.equals(target),"submitted_same_target");ok(count(db,DbHelper.TABLE_R36_ANALYSIS_REQUEST)==1,"one_durable_request");}
   }
   System.out.println("SERVICE_OWNER_PASS|mode="+a[2]+"|checks="+checks+"|scope=HOST_ACTUAL_RECONCILER_REAL_SQLITE_NOT_ANDROID_LIFECYCLE");
  }
 }
}
