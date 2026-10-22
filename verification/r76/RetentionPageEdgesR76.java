package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import java.io.*;import java.nio.file.*;import org.json.JSONObject;
/** Fixed WAL read view under a concurrent writer, plus true oversized-cell fallback. */
public final class RetentionPageEdgesR76 {
 static int checks,backups;static void ok(boolean b,String why){if(!b)throw new AssertionError(why);checks++;System.out.println("PASS "+why);}
 static long number(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
 static String value(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getString(0);}}
 public static void main(String[] a)throws Exception{
  ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics","host");File source=new File(a[1]),dest=new File(a[2]),root=new File(a[3]);root.mkdirs();
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],source.getPath())){
   long old=number(db,"SELECT generation FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=1");
   int[] calls={0};boolean[] wrote={false};ReportNativeCP4.retentionPage(source,dest,"diagnostic_log",new long[]{1},32L<<20,1<<20,()->{
    ++calls[0];if(!wrote[0]&&new File(dest.getPath()+"-journal").isFile()){wrote[0]=true;db.execSQL("UPDATE diagnostic_log SET detail='concurrent new value' WHERE rowid=1");}return true;
   });
   ok(wrote[0],"concurrent writer executed after private destination transaction started (source already pinned)");
   try(SQLiteDatabase copy=new SQLiteDatabase(a[0],dest.getPath())){
    ok(value(copy,"SELECT detail FROM diagnostic_log WHERE rowid=1").equals("original"),"page retains original cell despite concurrent live update");
    ok(number(copy,"SELECT generation FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=1")==old,"copied generation matches same pinned old row");
   }
   ok(number(db,"SELECT generation FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=1")>old,"real live generation advanced independently");
   db.execSQL("UPDATE diagnostic_log SET detail=zeroblob(67108864) WHERE rowid=1");
   RetentionArchiveProtocolCP12.Page page=DataRetentionGovernorR900.prepareArchivePageCP12(db,"diagnostic_log");
   ReportNativeCP4.Control yes=new ReportNativeCP4.Control(){public boolean shouldContinue(){return true;}public void onBackupProgress(long a,long b,int c){backups++;}};
   try(ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(root)){
    try(RetentionSnapshotCacheR73.Lease cache=RetentionSnapshotCacheR73.acquire(journal,source,page,yes,1800000000000L,Long.MAX_VALUE/4)){
     ok(backups>0,"64MiB cell exceeding SQL copy budget falls back to existing streaming backup");
     File meta=new File(journal.root(),"retention-snapshot-r73/snapshot.json");
     ok(new JSONObject(ReportJobJournalCP12.readAtomic(meta,4096)).getBoolean("fullFallback"),"fallback is explicitly recorded");
     File job=new File(root,"large-job");job.mkdirs();
     RetentionArchiveProtocolCP12.Receipt receipt=RetentionArchiveIoCP12.runVerified(root,page,job,cache.file,cache.verified,yes);
     ok(receipt.byteCount>(64L<<20),"all 64MiB cell bytes stream into independently verified SCROW");
     ok(number(db,"SELECT length(detail) FROM diagnostic_log WHERE rowid=1")==67108864L,"source large cell is never truncated or pruned by snapshot worker");
    }
    RetentionSnapshotCacheR73.discard(journal);
   }
  }
  Files.writeString(Path.of(a[4]),new JSONObject().put("status","PASS").put("checks",checks).put("backupCallbacks",backups).toString()+"\n");
 }
}
