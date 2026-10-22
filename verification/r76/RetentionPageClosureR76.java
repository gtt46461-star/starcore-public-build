package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;import java.nio.file.*;import java.util.*;import org.json.JSONObject;

/** Actual product/native retention, complete installed schema, synthetic old logs and draws. */
public final class RetentionPageClosureR76 {
 static int checks,backupCallbacks;static final long NOW=1800000000000L;
 static final ReportNativeCP4.Control YES=new ReportNativeCP4.Control(){public boolean shouldContinue(){return true;}public void onBackupProgress(long a,long b,int c){backupCallbacks++;}};
 interface Work{void run()throws Exception;}
 static void ok(boolean v,String why){if(!v)throw new AssertionError(why);checks++;System.out.println("PASS "+why);}
 static void reject(Work w,String reason)throws Exception {try{w.run();throw new AssertionError("accepted "+reason);}catch(IOException e){ok(e.getMessage().contains(reason),"reject "+reason);}}
 static long number(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
 static RetentionArchiveProtocolCP12.Page page(SQLiteDatabase db){return DataRetentionGovernorR900.prepareArchivePageCP12(db,"diagnostic_log");}
 static RetentionArchiveProtocolCP12.Receipt archive(File root,File job,RetentionArchiveProtocolCP12.Page p,RetentionSnapshotCacheR73.Lease cache)throws Exception {
  RetentionArchiveProtocolCP12.Receipt receipt=RetentionArchiveIoCP12.runVerified(root,p,job,cache.file,cache.verified,YES);
  File request=new File(root,"retention-requests-cp12/"+p.requestId+".json");ReportJobJournalCP12.writeAtomic(request,p.requestJson);
  File reply=new File(root,"retention-replies-cp12/"+p.requestId+".receipt");RetentionArchiveIoCP12.writeReceipt(reply,receipt);
  ok(RetentionArchiveIoCP12.validatePublishedProof(root,reply,receipt).manifestHash.equals(receipt.manifestHash),"published immutable proof readback");
  return receipt;
 }
 static int prune(SQLiteDatabase db,RetentionArchiveProtocolCP12.Receipt r)throws Exception {
  int result=-2,turns=0;long before=number(db,"SELECT COUNT(*) FROM diagnostic_log");
  db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('retention_cp12_inflight_request',?,1)",new Object[]{r.page.requestId});
  while(result==-2&&turns++<300){db.beginTransaction();try{result=DataRetentionGovernorR900.publishAndPruneCP12(db,r,"R73_HOST",TimeSourceR811.monoNowMs()+2);db.setTransactionSuccessful();}finally{db.endTransaction();}}
  ok(result>=0,"bounded publication/prune completes after restartable slices");
  ok(number(db,"SELECT COUNT(*) FROM research_retention_permit_v2")==0,"no deletion permits leak");
  return (int)(before-number(db,"SELECT COUNT(*) FROM diagnostic_log"));
 }
 public static void main(String[] args)throws Exception {
  File source=new File(args[1]),root=new File(args[2]);root.mkdirs();File job=new File(root,"page-job");job.mkdirs();
  RealtimePressureGateR811.resetForTests();ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics","host");
  try(SQLiteDatabase db=new SQLiteDatabase(args[0],source.getPath());ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(root)){
   ok(number(db,"SELECT COUNT(*) FROM sqlite_master WHERE type='trigger'")>=1300,"full production trigger schema retained");
   db.setForeignKeyConstraintsEnabled(true);
   RetentionArchiveProtocolCP12.Page first=page(db);ok(first.rows.size()==128,"first eligible 128-row page");
   try(RetentionSnapshotCacheR73.Lease cache=RetentionSnapshotCacheR73.acquire(journal,source,first,YES,NOW,Long.MAX_VALUE/4)){
    ok(!cache.reused&&backupCallbacks==0,"first page makes selected-row native snapshot without whole-DB backup");
    reject(()->RetentionSnapshotCacheR73.discard(journal),"RETENTION_SNAPSHOT_BUSY");
    RetentionArchiveProtocolCP12.Receipt receipt=archive(root,job,first,cache);
    ok(receipt.byteCount>(8L<<20),"large cell streamed into exact archive");
    int beforeRecovery=backupCallbacks;
    ok(RetentionArchiveIoCP12.recoverPublishedReceipt(root,first,YES).manifestHash.equals(receipt.manifestHash),"receipt-before-ack crash recovers original verified bytes");
    ok(beforeRecovery==backupCallbacks,"receipt recovery performs no database copy");
    File cas=RetentionArchiveProtocolCP12.casFile(root,receipt.archiveHash);long last=cas.length()-1;int original;
    try(RandomAccessFile io=new RandomAccessFile(cas,"rw")){io.seek(last);original=io.read();io.seek(last);io.write(original^1);}
    reject(()->RetentionArchiveIoCP12.recoverPublishedReceipt(root,first,YES),"ARCHIVE_STREAM_HASH");
    try(RandomAccessFile io=new RandomAccessFile(cas,"rw")){io.seek(last);io.write(original);}
    ok(prune(db,receipt)==128,"128 independently verified rows deleted");
    ok(prune(db,receipt)==0,"duplicate proof cannot delete twice");
   }
   RetentionArchiveProtocolCP12.Page second=page(db);int copies=backupCallbacks;String stableHash;
   try(RetentionSnapshotCacheR73.Lease cache=RetentionSnapshotCacheR73.acquire(journal,source,second,YES,NOW+1,Long.MAX_VALUE/4)){
    stableHash=cache.verified.sha256;
    ok(!cache.reused&&copies==backupCallbacks,"different page takes a new small snapshot with zero whole-DB backup calls");
    RetentionArchiveProtocolCP12.Receipt receipt=archive(root,job,second,cache);
    long changed=second.rows.get(0).rowId;
    db.execSQL("UPDATE diagnostic_log SET detail='changed after archival' WHERE rowid=?",new Object[]{changed});
    ok(prune(db,receipt)==127,"changed live generation survives stale archive proof");
    ok(number(db,"SELECT COUNT(*) FROM diagnostic_log WHERE rowid="+changed)==1,"changed row remains live");
   }
   RetentionArchiveProtocolCP12.Page third=page(db);copies=backupCallbacks;
   try(RetentionSnapshotCacheR73.Lease cache=RetentionSnapshotCacheR73.acquire(journal,source,third,YES,NOW+2,Long.MAX_VALUE/4)){
    ok(!cache.reused&&backupCallbacks==copies,"changed requested generation refreshes page cache");
    ok(cache.file.length()<1024*1024,"small requested page is below 1MiB despite unrelated history");
    ok(!cache.verified.sha256.equals(stableHash),"refreshed snapshot has new real content hash");
   }
   try(RetentionSnapshotCacheR73.Lease cache=RetentionSnapshotCacheR73.acquire(journal,source,third,YES,NOW+2,Long.MAX_VALUE/4)){
    ok(cache.reused,"identical requested page reuses immutable snapshot");
   }
   copies=backupCallbacks;
   reject(()->RetentionSnapshotCacheR73.acquire(journal,source,third,()->false,NOW+3,Long.MAX_VALUE/4),"ARCHIVE_CANCELLED");
   ok(copies==backupCallbacks,"cancellation does not trigger another backup");
   File cacheRoot=new File(journal.root(),"retention-snapshot-r73"),snapshot=new File(cacheRoot,"snapshot.db"),meta=new File(cacheRoot,"snapshot.json");
   long oldModified=snapshot.lastModified();try(RandomAccessFile io=new RandomAccessFile(snapshot,"rw")){io.seek(io.length()-1);int b=io.read();io.seek(io.length()-1);io.write(b^1);}snapshot.setLastModified(oldModified+2000);
   copies=backupCallbacks;
   try(RetentionSnapshotCacheR73.Lease cache=RetentionSnapshotCacheR73.acquire(journal,source,third,YES,NOW+4,Long.MAX_VALUE/4)){
    ok(!cache.reused&&backupCallbacks==copies,"changed private file fails hash and is rebuilt");
   }
   JSONObject metadata=new JSONObject(ReportJobJournalCP12.readAtomic(meta,4096));metadata.put("uses",256);ReportJobJournalCP12.writeAtomic(meta,metadata.toString());copies=backupCallbacks;
   try(RetentionSnapshotCacheR73.Lease cache=RetentionSnapshotCacheR73.acquire(journal,source,third,YES,NOW+5,Long.MAX_VALUE/4)){
    ok(!cache.reused&&backupCallbacks==copies,"bounded use count forces replacement");
   }
   copies=backupCallbacks;
   try(RetentionSnapshotCacheR73.Lease cache=RetentionSnapshotCacheR73.acquire(journal,source,third,YES,NOW+600006,Long.MAX_VALUE/4)){
    ok(!cache.reused&&backupCallbacks==copies,"age limit forces replacement");
   }
   copies=backupCallbacks;
   try(RetentionSnapshotCacheR73.Lease cache=RetentionSnapshotCacheR73.acquire(journal,source,third,YES,NOW+4,Long.MAX_VALUE/4)){
    ok(!cache.reused&&backupCallbacks==copies,"wall clock rollback cannot extend cache age");
   }
   long n=number(db,"SELECT COUNT(*) FROM diagnostic_log");
   reject(()->RetentionSnapshotCacheR73.acquire(journal,source,third,YES,NOW+5,0),"REPORT_DISK_BUDGET");
   ok(n==number(db,"SELECT COUNT(*) FROM diagnostic_log"),"disk refusal never prunes rows");
   db.execSQL("UPDATE diagnostic_log SET detail='changed before copy' WHERE rowid=?",new Object[]{third.rows.get(0).rowId});
   RetentionSnapshotCacheR73.discard(journal);
   reject(()->RetentionSnapshotCacheR73.acquire(journal,source,third,YES,NOW+6,Long.MAX_VALUE/4),"STALE_PAGE");
   ok(!snapshot.exists()&&!meta.exists(),"stale request leaves no accepted snapshot metadata");
   RetentionArchiveProtocolCP12.Page last=page(db);
   try(RetentionSnapshotCacheR73.Lease cache=RetentionSnapshotCacheR73.acquire(journal,source,last,YES,NOW+7,Long.MAX_VALUE/4)){
    ok(prune(db,archive(root,job,last,cache))==128,"cleanup continues after stale-page refresh");
   }
   int archives=new File(root,"retention-cas-cp12").list().length;
   RetentionSnapshotCacheR73.discard(journal);
   ok(!snapshot.exists()&&!meta.exists(),"full export can reclaim optional snapshot copy");
   ok(new File(root,"retention-cas-cp12").list().length==archives,"snapshot disposal preserves all immutable archives");
   RetentionArchiveProtocolCP12.Page remaining=page(db);
   for(String stage:new String[]{"com.ethk3.novaonepro.ReportArchiveCP4", "com.ethk3.novaonepro.RetentionSnapshotRowsCP7$VerifiedSnapshot"}){
    final boolean[] cancelled={false};
    ReportNativeCP4.Control duringProof=()->{for(StackTraceElement frame:Thread.currentThread().getStackTrace())if(frame.getClassName().equals(stage)){cancelled[0]=true;return false;}return true;};
    reject(()->RetentionSnapshotCacheR73.acquire(journal,source,remaining,duringProof,NOW+8,Long.MAX_VALUE/4),"CANCELLED");
    ok(cancelled[0],"cancellation reached actual post-copy proof stage "+stage);
    ok(!snapshot.exists()&&!meta.exists(),"post-copy cancellation promptly removes unpublished snapshot "+stage);
    ok(number(db,"SELECT COUNT(*) FROM diagnostic_log")==17,"post-copy cancellation never prunes live rows");
   }
   ok(number(db,"SELECT COUNT(*) FROM draws WHERE dirty=0")==5000,"draw tail remains exactly 5000");
   ok(number(db,"SELECT COUNT(*) FROM diagnostic_log")==17,"383 eligible rows removed while 17 remain");
   ok(number(db,"SELECT COUNT(*) FROM research_evidence_archive_v2")==3,"three real archive receipts published");
  }
  Files.writeString(Path.of(args[3]),new JSONObject().put("status","PASS").put("checks",checks).put("scope","ACTUAL_PRODUCT_NATIVE_SQLITE_FULL_SCHEMA_SYNTHETIC_INPUT_NOT_PHONE").toString()+"\n");
 }
}
