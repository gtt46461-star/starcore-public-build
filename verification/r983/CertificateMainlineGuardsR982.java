package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Field;import java.util.concurrent.locks.ReentrantLock;
public final class CertificateMainlineGuardsR982 {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static long count(SQLiteDatabase db,String target){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM research_consumer_choice_r932 WHERE target_issue=?",new String[]{target})){c.moveToFirst();return c.getLong(0);}}
 public static void main(String[] a)throws Exception{SQLiteDatabase db=new SQLiteDatabase(a[0],a[1]);try{
  DbStore store=HostProductR974.store(db);Field global=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");global.setAccessible(true);ReentrantLock lock=(ReentrantLock)global.get(null);Field local=DatabaseCoordinator.class.getDeclaredField("writeLock");local.setAccessible(true);local.set(store.coordinatorR36(),lock);
  ResearchPlatformStoreR929.ensureSchema(store);String target="202609230361";db.execSQL("DELETE FROM research_consumer_choice_r932 WHERE target_issue=?",new Object[]{target});
  String id,structure,certificate;try(Cursor c=db.rawQuery("SELECT candidate_id,structural_hash,certificate_hash FROM research_certificate_r929 WHERE state='ACTIVE'",null)){ok(c.moveToFirst(),"Missing certificate");id=c.getString(0);structure=c.getString(1);certificate=c.getString(2);}
  try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(store,target,System.currentTimeMillis())){
   ok(ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,id,structure,target,.05).authorized,"Preflight fixture not qualified");
   if(a[2].equals("revoke"))db.beforeNextBegin=()->{ok(lock.isHeldByCurrentThread(),"Hook outside actual writer lock");ok(ResearchPlatformStoreR929.revoke(store,certificate,"TEST_ONLY_REVOKED_BETWEEN_PREPARE_COMMIT"),"Revocation owner failed");};
   else if(a[2].equals("draw"))db.beforeNextBegin=()->{ok(lock.isHeldByCurrentThread(),"Hook outside actual writer lock");db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,open_time,fetch_time) VALUES(?,1,2,3,4,5,6,7,8,9,10,?,?)",new Object[]{target,System.currentTimeMillis(),System.currentTimeMillis()});};
   boolean frozen=UnifiedResearchChoiceR932.freeze(store,target);
   if(a[2].equals("repeat")){
    ok(frozen,"Qualified freeze failed");ok(count(db,target)==1,"Missing unique choice");UnifiedResearchChoiceR932.Evidence selected=UnifiedResearchChoiceR932.selected(store,target,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM);ok(selected!=null&&id.equals(selected.id)&&certificate.equals(selected.certificate),"Exact consumer identity mismatch");
    ok(UnifiedResearchChoiceR932.freeze(store,target)&&count(db,target)==1,"Duplicate callback changed choice count");
    ok(ResearchPlatformStoreR929.revoke(store,certificate,"TEST_ONLY_REVOKED_AFTER_FREEZE"),"Revoke failed");ok(UnifiedResearchChoiceR932.selected(store,target,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM)==null,"Revoked frozen choice still consumed");
   }else{
    ok(!frozen,"Race accepted stale authorization");ok(count(db,target)==0,"Rejected race committed choice");ok(UnifiedResearchChoiceR932.selected(store,target,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM)==null,"Rejected race consumer returned evidence");String reason=store.getState("r932_choice_freeze_status","");ok(reason.startsWith(a[2].equals("revoke")?"R932_CHOICE_PREPARED_CERTIFICATE_CHANGED:":"R932_CHOICE_POSTDRAW_REJECTED"),"Lost original typed blocker: "+reason);System.out.println("BLOCKER="+reason);
   }
   ok(!db.inTransaction()&&!lock.isLocked(),"Transaction or owner leaked");
  }
  System.out.println("MAINLINE_GUARDS_PASS mode="+a[2]+" checks="+checks+" exactCertificate=YES scope=synthetic_host_no_REAL");
 }finally{db.shutdown();}}
}
