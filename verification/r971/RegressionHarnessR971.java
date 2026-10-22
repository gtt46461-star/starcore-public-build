package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.LegacyTrainingStreamMigrationR971;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Runs product classes against real SQLite; not Android lifecycle or live win-rate evidence. */
public final class RegressionHarnessR971 {
 static int checks=0;
 interface Op {void run();}
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
 static void tx(SQLiteDatabase d,Op op){d.beginTransaction();try{op.run();d.setTransactionSuccessful();}finally{d.endTransaction();}}
 static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static String text(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getString(0):"";}}
 static String sha(byte[] bytes)throws Exception{return LosslessReportArchiveR971.hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
 static String digestFile(File f)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];for(int z;(z=in.read(b))>=0;)md.update(b,0,z);}return LosslessReportArchiveR971.hex(md.digest());}
 static String tableDigest(SQLiteDatabase db,String q)throws Exception{
  MessageDigest md=MessageDigest.getInstance("SHA-256");try(Cursor c=db.rawQuery(q,null)){while(c.moveToNext())for(int i=0;i<c.getColumnCount();i++){String v=c.isNull(i)?"<NULL>":c.getString(i);md.update((v.length()+":"+v+"|").getBytes(StandardCharsets.UTF_8));}}
  return LosslessReportArchiveR971.hex(md.digest());
 }
 static void countdown(){
  check(CountdownProjectionR971.remaining(60000,1000,1000)==60000,"MONOTONIC_ANCHOR");
  check(CountdownProjectionR971.remaining(60000,1000,41000)==20000,"STILL_TICKS_AFTER_OLD_6_5_SECOND_EXPIRY");
  check(CountdownProjectionR971.remaining(60000,1000,61000)==0,"AT_DEADLINE_ZERO");
  check(CountdownProjectionR971.remaining(60000,1000,180000)==0,"DOES_NOT_INVENT_NEW_PERIOD_AFTER_STALL");
  check(CountdownProjectionR971.remaining(60000,1000,999)==0,"BACKWARDS_CLOCK_FAILS_CLOSED");
  check(CountdownProjectionR971.atRead(90000,100000)==0,"STALE_SNAPSHOT_NOT_RESURRECTED");
  check(CountdownProjectionR971.atRead(9999999,100000)==60000,"LARGE_WALL_CLOCK_SKEW_BOUNDED");
 }
 static void maintenance(String bridge)throws Exception{
  final SQLiteDatabase d=new SQLiteDatabase(bridge);
  try{
   d.execSQL("CREATE TABLE "+DbHelper.TABLE_APP_STATE+"(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
   tx(d,()->ResearchLearningContinuityR912.ensureDesired(d,"202609200001",false,true));
   long before=n(d,"SELECT total_changes()");
   for(int z=0;z<10;z++)tx(d,()->ResearchLearningContinuityR912.ensureDesired(d,"202609200001",false,true));
   check(n(d,"SELECT total_changes()")==before,"TEN_NOOP_PULSES_ZERO_ROW_REWRITES");
   Set<String> served=new HashSet<>();int hot=0;
   final ResearchTaskQueueR967.Lease[] lease={null};
   // R971 backlog repair reserves every second READY turn for lane 13. Forty-four turns
   // therefore contain at least one complete 22-lane round-robin plus bounded hot repeats.
   for(int z=0;z<44;z++){
    tx(d,()->lease[0]=ResearchLearningContinuityR912.claimNext(d,System.currentTimeMillis(),true));
    check(lease[0]!=null,"READY_TURN_"+z);
    served.add(lease[0].candidate);if("lane:13".equals(lease[0].candidate))hot++;
    if(z%2==1)check("lane:13".equals(lease[0].candidate),"RESERVED_TRAINING_TURN_"+z);
    tx(d,()->ResearchTaskQueueR967.yield(d,lease[0],lease[0].cursor,"RESOURCE_YIELD",0));
   }
   check(served.size()==22,"WEIGHTED_SCHEDULER_ALL_22_LANES_SERVED_WITHIN_44_TURNS");
   check(hot>=22&&hot<=24,"TRAINING_EXTRA_TURNS_BOUNDED_NO_OTHER_LANE_STARVATION");
  }finally{d.shutdown();}
 }
 static void actualReportMigration(String bridge,String original,Path out)throws Exception{
  Path copied=out.resolve("actual_report_migration.db");Files.copy(Paths.get(original),copied,StandardCopyOption.REPLACE_EXISTING);
  final SQLiteDatabase d=new SQLiteDatabase(bridge,copied.toString());
  try{
   d.execSQL("CREATE TABLE IF NOT EXISTS r952_recovery_journal(delivery_hash TEXT,attempt INTEGER,state TEXT,error_code TEXT,event_hash TEXT UNIQUE,created_at INTEGER)");
   check(n(d,"SELECT COUNT(*) FROM research_training_input_r967 WHERE role='TRAIN_STREAM'")==96,"REAL_REPORT_96_LEGACY_INPUT_RECEIPTS");
   String beforePred=tableDigest(d,"SELECT target_issue,candidate_id,prediction_mask,position,snapshot_hash,structure_hash,model_hash,code_hash,data_hash,captured_at,settled,actual_number,hit,settlement_hash,label_hash,frozen_features,input_hash,evidence_role FROM r952_candidate_predraw ORDER BY target_issue,candidate_id");
   String beforeModel=tableDigest(d,"SELECT * FROM r952_artifact ORDER BY artifact_hash");
   String beforeCursor=tableDigest(d,"SELECT * FROM r952_learning_cursor ORDER BY consumer_id");
   long completed=n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='COMPLETED'");
   final int[] result={0};
   d.beginTransaction();try{result[0]=LegacyTrainingStreamMigrationR971.retireBatch(d,32,Long.MAX_VALUE/2);check(result[0]==32,"LEGACY_BATCH_IS_BOUNDED_AT_32");}finally{d.endTransaction();}
   check(n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='DEPENDENCY_RETIRED'")==0,"MIGRATION_ROLLBACK_LEAVES_OUTBOX_UNCHANGED");
   check(n(d,"SELECT COUNT(*) FROM r952_recovery_journal")==0,"MIGRATION_ROLLBACK_LEAVES_JOURNAL_UNCHANGED");
   // Keep one valid active lease and deliberately corrupt another receipt. Neither may retire.
   String guarded=text(d,"SELECT o.delivery_hash FROM r952_research_outbox o JOIN r952_candidate_predraw p ON o.target_issue=p.target_issue AND o.candidate_id=p.candidate_id WHERE p.evidence_role='TRAIN_STREAM' ORDER BY o.delivery_hash LIMIT 1");
   String invalid=text(d,"SELECT o.delivery_hash FROM r952_research_outbox o JOIN r952_candidate_predraw p ON o.target_issue=p.target_issue AND o.candidate_id=p.candidate_id WHERE p.evidence_role='TRAIN_STREAM' AND o.delivery_hash<>'"+guarded+"' ORDER BY o.delivery_hash LIMIT 1");
   String oldInput=text(d,"SELECT model_input_hash FROM r952_research_outbox WHERE delivery_hash='"+invalid+"'");
   d.execSQL("UPDATE r952_research_outbox SET state='RUNNING',lease_until=? WHERE delivery_hash=?",new Object[]{Long.MAX_VALUE-1,guarded});
   d.execSQL("UPDATE r952_research_outbox SET model_input_hash='corrupted' WHERE delivery_hash=?",new Object[]{invalid});
   int retired=0;for(int z=0;z<5;z++){tx(d,()->result[0]=LegacyTrainingStreamMigrationR971.retireBatch(d,32,Long.MAX_VALUE/2));retired+=result[0];}
   check(retired==82,"MATCHING_82_RETIRED_OTHER_TWO_GUARDED");
   check(text(d,"SELECT state FROM r952_research_outbox WHERE delivery_hash='"+guarded+"'").equals("RUNNING"),"LIVE_LEASE_NOT_STOLEN");
   check(!text(d,"SELECT state FROM r952_research_outbox WHERE delivery_hash='"+invalid+"'").equals("DEPENDENCY_RETIRED"),"MISMATCHED_RECEIPT_NOT_LAUNDERED");
   d.execSQL("UPDATE r952_research_outbox SET lease_until=0 WHERE delivery_hash=?",new Object[]{guarded});
   d.execSQL("UPDATE r952_research_outbox SET model_input_hash=? WHERE delivery_hash=?",new Object[]{oldInput,invalid});
   tx(d,()->result[0]=LegacyTrainingStreamMigrationR971.retireBatch(d,32,Long.MAX_VALUE/2));
   check(result[0]==2&&n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='DEPENDENCY_RETIRED'")==84,"ALL_84_VALID_LEGACY_DELIVERIES_RETIRED_EXPLICITLY");
   check(n(d,"SELECT COUNT(*) FROM r952_candidate_predraw WHERE evidence_role='TRAIN_STREAM' AND settled=0 AND proof_error='R958_STRICT_FUTURE_PROOF_REJECTED'")==12,"TWELVE_EXISTING_STRICT_PROOF_REJECTIONS_PRESERVED");
   check(n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='COMPLETED'")==completed,"NO_FAKE_TRAINING_COMPLETION");
   check(beforePred.equals(tableDigest(d,"SELECT target_issue,candidate_id,prediction_mask,position,snapshot_hash,structure_hash,model_hash,code_hash,data_hash,captured_at,settled,actual_number,hit,settlement_hash,label_hash,frozen_features,input_hash,evidence_role FROM r952_candidate_predraw ORDER BY target_issue,candidate_id")),"ALL_ORIGINAL_PREDRAW_EVIDENCE_BYTES_RETAINED");
   check(beforeModel.equals(tableDigest(d,"SELECT * FROM r952_artifact ORDER BY artifact_hash")),"ALL_1235_MODEL_ARTIFACTS_UNCHANGED");
   check(beforeCursor.equals(tableDigest(d,"SELECT * FROM r952_learning_cursor ORDER BY consumer_id")),"ALL_LEARNER_TRAINING_CURSORS_UNCHANGED");
   tx(d,()->result[0]=LegacyTrainingStreamMigrationR971.retireBatch(d,32,Long.MAX_VALUE/2));check(result[0]==0,"MIGRATION_IDEMPOTENT_ON_RESTART");
  }finally{d.shutdown();}
  final SQLiteDatabase empty=new SQLiteDatabase(bridge);try{tx(empty,()->check(LegacyTrainingStreamMigrationR971.retireBatch(empty,32,1)==0,"FRESH_INSTALL_WITHOUT_LEGACY_TABLES_NOOP"));}finally{empty.shutdown();}
 }
 static void archive(Path out,String hugeReport)throws Exception{
  File dir=out.resolve("archive_test").toFile();dir.mkdirs();
  final String text="完整准确报告：中文、emoji 🧪，制表\t换行\n零号\u0000末尾\n";
  LosslessReportArchiveR971.Result r=LosslessReportArchiveR971.create(dir,w->w.write(text));
  check(r.bodySha256.equals(sha(text.getBytes(StandardCharsets.UTF_8))),"UNICODE_EXACT_UTF8_BODY_HASH");
  check(r.archiveSha256.equals(digestFile(r.file)),"ARCHIVE_FILENAME_SHA_MATCHES_ACTUAL_FILE");
  try(ZipFile z=new ZipFile(r.file)){
   byte[] decoded=z.getInputStream(z.getEntry("report.txt")).readAllBytes();
   check(r.reportSha256.equals(sha(decoded))&&r.reportBytes==decoded.length,"DECODED_REPORT_HASH_AND_SIZE_EXACT");
   check(new String(decoded,StandardCharsets.UTF_8).startsWith(text),"UNICODE_BODY_LOSSLESS_ROUNDTRIP");
   check(z.getEntry("manifest.json")!=null&&z.size()==2,"FINISHED_ARCHIVE_HAS_REPORT_AND_MANIFEST");
  }
  int count=dir.list().length;boolean failed=false;
  try{LosslessReportArchiveR971.create(dir,w->{w.write("partial");throw new IOException("simulated source failure");});}catch(IOException expected){failed=true;}
  check(failed&&count==dir.list().length,"SOURCE_FAILURE_NEVER_PUBLISHES_PARTIAL_COMPLETE_ARCHIVE");
  long begin=System.nanoTime();
  LosslessReportArchiveR971.Result full=LosslessReportArchiveR971.create(dir,w->{try(Reader in=new InputStreamReader(new FileInputStream(hugeReport),StandardCharsets.UTF_8)){char[] b=new char[32768];for(int n;(n=in.read(b))>=0;)w.write(b,0,n);}});
  double elapsed=(System.nanoTime()-begin)/1e9;
  check(full.bodyBytes==new File(hugeReport).length(),"REAL_353MB_REPORT_EVERY_BYTE_PRESERVED");
  check(full.bodySha256.equals(digestFile(new File(hugeReport))),"REAL_REPORT_INPUT_HASH_MATCHES_STREAMED_BODY_HASH");
  try(ZipFile z=new ZipFile(full.file);InputStream in=z.getInputStream(z.getEntry("report.txt"))){MessageDigest md=MessageDigest.getInstance("SHA-256");long got=0;byte[] b=new byte[65536];for(int n;(n=in.read(b))>=0;){got+=n;md.update(b,0,n);}check(got==full.reportBytes&&LosslessReportArchiveR971.hex(md.digest()).equals(full.reportSha256),"REAL_353MB_REPORT_DECOMPRESSED_HASH_AND_LENGTH_VERIFIED");}
  String metrics="{\"input_bytes\":"+full.bodyBytes+",\"archive_bytes\":"+full.file.length()+",\"host_elapsed_seconds\":"+elapsed+",\"phone_time_measured\":false,\"archive_sha256\":\""+full.archiveSha256+"\",\"input_sha256\":\""+full.bodySha256+"\"}";
  Files.writeString(out.resolve("archive_metrics.json"),metrics);System.out.println("ARCHIVE_METRICS "+metrics);
 }
 public static void main(String[] args)throws Exception{Path out=Paths.get(args[3]);Files.createDirectories(out);countdown();maintenance(args[0]);actualReportMigration(args[0],args[1],out);archive(out,args[2]);System.out.println("R971_SCOPED_REGRESSION_PASS checks="+checks+"; actual_report_readonly_source=YES; Android_device=NOT_RUN; complete_91_plan=NOT_PROVEN");}
}
