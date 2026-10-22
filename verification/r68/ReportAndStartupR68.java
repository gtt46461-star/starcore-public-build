package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.zip.*;import org.json.*;
/** Actual product code, real SQLite and native archive codec; synthetic input, not phone evidence. */
public final class ReportAndStartupR68 {
 static int checks;static final ReportNativeCP4.Control YES=()->true;
 static void need(boolean b,String why){if(!b)throw new AssertionError(why);checks++;System.out.println("PASS|"+why);}
 interface Work{void run()throws Exception;}
 static void reject(Work action,String why)throws Exception{try{action.run();throw new AssertionError(why);}catch(IOException expected){checks++;System.out.println("PASS|"+why);}}
 static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
 static JSONObject table(JSONArray a,String name)throws Exception{for(int i=0;i<a.length();i++)if(name.equals(a.getJSONObject(i).getString("table")))return a.getJSONObject(i);throw new AssertionError(name);}
 public static void main(String[] args)throws Exception{
  need(TrainingContinuationR973.compatible("a0d0c0be9e9e1bf0ea99130a2962ea7f591cb53de7d5ff686b51e9efd7fc5f12"),"EXACT_R67_LEARNERS_CONTINUE");
  Path root=Path.of(args[1]);Files.createDirectories(root);Path database=root.resolve("fixture.db");
  try(SQLiteDatabase db=new SQLiteDatabase(args[0],database.toString())){
   RuntimeEventLedgerR926.ensureSchema(db);
   for(int i=1;i<=120;i++)RuntimeEventLedgerR926.appendWithinTransaction(db,RuntimeEventLedgerR926.Event.of("20261012"+String.format(Locale.ROOT,"%04d",i),"epoch","attempt","ANALYSIS_STARTED","STARTED",40,"payload","fixture","key-"+i));
   long original=scalar(db,"SELECT COUNT(*) FROM "+RuntimeEventLedgerR926.TABLE_EVENT);
   need(RuntimeEventLedgerR926.reconcile(db,500)==0,"HEALTHY_STARTUP_DOES_NOT_REPLAY_120_ISSUES");
   db.execSQL("DELETE FROM "+RuntimeEventLedgerR926.TABLE_PROJECTION+" WHERE target_issue='202610120001'");
   db.execSQL("UPDATE "+RuntimeEventLedgerR926.TABLE_PROJECTION+" SET event_watermark=0 WHERE target_issue='202610120002'");
   db.execSQL("UPDATE "+RuntimeEventLedgerR926.TABLE_PROJECTION+" SET event_watermark=999999 WHERE target_issue='202610120003'");
   need(RuntimeEventLedgerR926.reconcile(db,500)==3,"MISSING_STALE_AND_FUTURE_WATERMARKS_REPAIRED");
   need(RuntimeEventLedgerR926.reconcile(db,500)==0,"RECOVERY_IDEMPOTENT");
   need(scalar(db,"SELECT COUNT(*) FROM "+RuntimeEventLedgerR926.TABLE_EVENT)==original,"RECONCILIATION_DOES_NOT_FABRICATE_EVENTS");
   db.execSQL("DELETE FROM "+RuntimeEventLedgerR926.TABLE_PROJECTION+" WHERE target_issue='202610120004'");
   db.beginTransaction();try{need(RuntimeEventLedgerR926.reconcile(db,500)==1,"RECOVERY_INSIDE_OWNER_TRANSACTION");}finally{db.endTransaction();}
   need(scalar(db,"SELECT COUNT(*) FROM "+RuntimeEventLedgerR926.TABLE_PROJECTION+" WHERE target_issue='202610120004'")==0,"OWNER_ROLLBACK_RETAINS_RETRYABLE_GAP");
   need(RuntimeEventLedgerR926.reconcile(db,500)==1,"ROLLED_BACK_RECOVERY_CAN_RETRY");
   db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
   db.execSQL("INSERT INTO app_state VALUES('private_credential','SECRET_NOT_EXPORTED',1)");
   db.execSQL("CREATE TABLE r952_candidate(candidate_id TEXT PRIMARY KEY,state TEXT,ast TEXT,new_diagnostic_field TEXT,model_binary BLOB)");
   db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,numbers TEXT,open_time INTEGER)");
   db.execSQL("CREATE TABLE derived_training_payload(issue TEXT PRIMARY KEY,payload BLOB)");
   db.beginTransaction();try{
    for(int i=0;i<1025;i++)db.execSQL("INSERT INTO r952_candidate VALUES(?,'OBSERVING',?,?,zeroblob(1024))",new Object[]{"candidate-"+i,"公式研究完整长文本".repeat(1000)+"END-"+i,"new-field-"+i});
    for(int i=0;i<5000;i++)db.execSQL("INSERT INTO draws VALUES(?,'1,2,3,4,5,6,7,8,9,10',?)",new Object[]{String.valueOf(202600000000L+i),i});
    for(int i=0;i<64;i++)db.execSQL("INSERT INTO derived_training_payload VALUES(?,zeroblob(1048576))",new Object[]{"issue-"+i});
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
  }
  String before=ReportArchiveCP4.hash(database.toFile(),YES);File job=root.resolve("job").toFile();job.mkdir();File scope;
  try(SQLiteDatabase db=SQLiteDatabase.openDatabase(database.toString(),null,SQLiteDatabase.OPEN_READONLY)){
   scope=ReportDiagnosticEvidenceR64.write(db,job,before,YES);
   JSONObject result=new JSONObject(Files.readString(scope.toPath()));JSONArray evidence=result.getJSONArray("recentEvidence");
   JSONObject candidates=table(evidence,"r952_candidate");JSONArray rows=candidates.getJSONArray("rows");
   need(rows.length()==1025&&candidates.getLong("exportedRows")==1025,"ALL_ALGORITHMS_NO_ROW_SAMPLING");
   need(rows.getJSONObject(1024).getString("ast").endsWith("END-1024"),"LONG_FORMULA_TEXT_NOT_TRUNCATED");
   need("new-field-1024".equals(rows.getJSONObject(1024).getString("new_diagnostic_field")),"NEW_METADATA_COLUMNS_NOT_SILENTLY_DROPPED");
   need(rows.getJSONObject(0).getString("model_binary").contains("bytes=1024"),"BINARY_BOUNDARY_EXPLICIT_WITH_SIZE");
   need(table(evidence,"draws").getJSONArray("rows").length()==5000,"ALL_5000_DRAW_ROWS_INCLUDED");
   need(table(result.getJSONArray("tables"),"derived_training_payload").getLong("rows")==64,"OMITTED_RAW_PAYLOAD_TABLE_STILL_COUNTED");
   need("MISSING_SCHEMA".equals(table(evidence,"research_matrix_spec_cp12").getString("status")),"ABSENT_MATRIX_REPORTED_NOT_EMPTY_SUCCESS");
   need(!Files.readString(scope.toPath()).contains("SECRET_NOT_EXPORTED"),"UNRELATED_CREDENTIAL_VALUES_EXCLUDED");
  }
  need(before.equals(ReportArchiveCP4.hash(database.toFile(),YES)),"DIAGNOSTICS_READ_ONLY_SOURCE_UNCHANGED");
  Files.writeString(root.resolve("model.dat"),"model contents");Files.createSymbolicLink(root.resolve("model-link"),root.resolve("model.dat"));
  File inventory=ReportDiagnosticEvidenceR64.writeFileInventory(root.toFile(),job,YES);String inventoryText=Files.readString(inventory.toPath());
  need(inventoryText.contains("CURRENT_EXPORT_TEMPORARIES")&&inventoryText.contains("LINK_NOT_FOLLOWED")&&inventoryText.contains("model.dat"),"FILE_INVENTORY_NO_SELF_RECURSION_OR_SYMLINK_FOLLOW");
  need(!inventoryText.contains("model contents"),"FILE_INVENTORY_DOES_NOT_DUPLICATE_CONTENTS");
  ReportNativeCP4.loadForDiagnosticProcess("test:diagnostics","test");String id="8".repeat(64);File archive=new File(job,"archive-1");archive.mkdir();
  try(ReportArchiveCP4 writer=new ReportArchiveCP4(archive,id)){writer.add(scope.getName(),scope,4L<<20,YES);writer.add(inventory.getName(),inventory,4L<<20,YES);writer.finish(true,true,"SYNTHETIC_R68_SIZE_AND_CLOSURE_FIXTURE");}
  ReportArchiveVerifierCP12.Receipt proof=ReportArchiveVerifierCP12.verify(archive,id,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),YES,null);
  ReportJobJournalCP12.Job state=new ReportJobJournalCP12.Job(id,"7".repeat(64),ReleaseManifestR926.MANIFEST_SHA256,"FULL_REPORT",System.currentTimeMillis());
  state.stage="READY_TO_SHARE";state.publishedDir="archive-1";state.manifestSha=proof.manifestSha256;state.evidenceComplete=true;state.checksComplete=true;state.allChecksPassed=false;
  ReportPublicationCP12.prepareSingleZip(job,archive,id,proof.manifestSha256,true,true,false,YES);
  ReportJobJournalCP12.writeAtomic(new File(job,"job.json"),state.json().toString());
  JSONObject pointer=new JSONObject().put("jobId",id).put("relativeDir","archive-1").put("manifestSha256",state.manifestSha)
   .put("deliveryIndexSha256",ReportArchiveCP4.hash(new File(job,"delivery/index.json"),YES));
  ReportJobJournalCP12.writeAtomic(new File(job,"published.json"),pointer.toString());
  File zip=new File(job,"delivery/STARCORE_FULL_REPORT.zip");long zipBytes=zip.length(),scopeBytes=scope.length();
  try(ZipFile z=new ZipFile(zip)){
   need(z.size()==3&&z.getEntry("snapshot.db")==null,"ONE_STANDARD_ZIP_ALL_DECLARED_MEMBERS_NO_RAW_DATABASE");
   try(InputStream in=z.getInputStream(z.getEntry(scope.getName()))){need(Arrays.equals(in.readAllBytes(),Files.readAllBytes(scope.toPath())),"ZIP_METADATA_BYTE_FOR_BYTE_LOSSLESS");}
  }
  need(zipBytes<scopeBytes/10,"REPEATED_TEXT_COMPRESSES_WITHOUT_TRUNCATION");
  need(ReportPublicationCP12.verifyPublishedZip(job,YES),"SEALED_ZIP_VERIFIED_BEFORE_CLEANUP");
  Files.writeString(new File(job,"snapshot.db").toPath(),"temporary snapshot");ReportPublicationCP12.cleanRaw(job,"archive-1");
  need(!new File(job,"snapshot.db").exists()&&!new File(archive,"P000001.bin").exists()&&zip.exists(),"REDUNDANT_PRIVATE_SNAPSHOT_AND_PARTS_REMOVED");
  need(ReportPublicationCP12.verifyPublishedZip(job,YES),"RESUME_FROM_VERIFIED_ZIP_AFTER_PART_REMOVAL");
  Files.writeString(new File(job,"snapshot.db").toPath(),"must not remove before validation");
  try(RandomAccessFile out=new RandomAccessFile(zip,"rw")){out.seek(0);out.write(0);}
  reject(()->ReportPublicationCP12.cleanRaw(job,"archive-1"),"CORRUPTED_DELIVERY_BLOCKS_CLEANUP");
  need(new File(job,"snapshot.db").exists(),"FAILED_VERIFICATION_PRESERVES_RECOVERY_INPUT");
  File published=root.resolve("published").toFile();new File(published,"jobs").mkdirs();File moved=new File(published,"jobs/"+id);need(job.renameTo(moved),"CORRUPT_OLD_JOB_FIXTURE_MOVED");
  ReportPublicationCP12.pruneTerminal(published,"6".repeat(64));
  need(new File(moved,"cleanup-pending.json").exists()&&new File(moved,"snapshot.db").exists(),"CORRUPT_OLD_DELIVERY_DOES_NOT_BLOCK_NEW_REPORT_OR_DELETE_RECOVERY");
  File legacy=root.resolve("legacy").toFile();File legacyArchive=new File(legacy,"archive-1");legacyArchive.mkdirs();Files.writeString(new File(legacyArchive,"P000001.bin").toPath(),"legacy part");
  ReportPublicationCP12.cleanRaw(legacy,"archive-1");need(new File(legacyArchive,"P000001.bin").isFile(),"LEGACY_MULTIPART_REMAINS_COMPLETE");
  System.out.println("R68_PASS|checks="+checks+"|sourceDatabaseBytes="+database.toFile().length()+"|metadataBytes="+scopeBytes+"|zipBytes="+zipBytes+"|HOST_SYNTHETIC_NOT_DEVICE");
 }
}
