package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import java.io.*;import java.nio.file.*;import java.util.UUID;
/** Publishes real independently verified SCROW evidence before allowing live-tail deletion. */
public final class RetentionHeadroomR996 {
 static void prepareAndPublish(SQLiteDatabase db,File files)throws Exception{
  db.setForeignKeyConstraintsEnabled(true);RetentionArchiveProtocolCP12.Page page=DataRetentionGovernorR900.prepareArchivePageCP12(db,"draws");if(page==null)throw new AssertionError("draw headroom not prepared before full tail");
  File requests=new File(files,"retention-requests-cp12");requests.mkdirs();Files.writeString(new File(requests,page.requestId+".json").toPath(),page.requestJson);File job=new File(files,"jobs/"+page.requestId);job.mkdirs();File snapshot=new File(job,"snapshot.db");
  ReportNativeCP4.loadForDiagnosticProcess("fixture:diagnostics","fixture");ReportNativeCP4.backup(new File(db.getPath()),snapshot,32L<<20,()->true);
  RetentionArchiveProtocolCP12.Receipt made=RetentionArchiveIoCP12.run(files,page,job,snapshot,ReportArchiveCP4.hash(snapshot,()->true),()->true);File replies=new File(files,"retention-replies-cp12");replies.mkdirs();File reply=new File(replies,page.requestId+".receipt");RetentionArchiveIoCP12.writeReceipt(reply,made);RetentionArchiveProtocolCP12.Receipt verified=RetentionArchiveIoCP12.validatePublishedProof(files,reply,made);
  int progress;do{db.beginTransaction();try{progress=DataRetentionGovernorR900.publishAndPruneCP12(db,verified,UUID.randomUUID().toString(),TimeSourceR811.monoNowMs()+2000);db.setTransactionSuccessful();}finally{db.endTransaction();}}while(progress==-2);
  System.out.println("REAL_HEADROOM_PROOF rows="+verified.members.size()+" archiveBytes="+verified.byteCount+" archiveHash="+verified.archiveHash+" pruneResult="+progress);
 }
 public static void main(String[] a)throws Exception{
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   DbStore store=HostProductR974.store(db);if(store.getDrawCount()!=4872)throw new AssertionError("boundary fixture");prepareAndPublish(db,new File(a[2]));
   if(store.getDrawCount()!=4872)throw new AssertionError("prospective preparation deleted live tail");
   if(DataRetentionGovernorR900.prepareArchivePageCP12(db,"draws")!=null)throw new AssertionError("duplicate archive queued while 128 proofs available");
   System.out.println("RETENTION_HEADROOM_R996_PASS assertions=3 liveDraws=4872 scope=REAL_NATIVE_SCROW_REAL_SQLITE_ACTUAL_GOVERNOR_NO_DEVICE");
  }
 }
}
