package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.nio.file.*;import java.io.*;
public final class RuntimeRegressionR67 {
 static int checks;
 static void need(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS|"+name);}
 static Models.Draw draw(){Models.Draw d=new Models.Draw();d.issue="202610120001";d.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};d.openTime=1791763260000L;d.sourceLine="fixture-confirmed";return d;}
 static AtomCatalogImportCP12.Page page(Path asset,AtomCatalogImportCP12.ImportCursor c)throws Exception{
  try(InputStream in=Files.newInputStream(asset)){in.skipNBytes(c.offset);return AtomCatalogImportCP12.preparePageCP12(in,c,System.nanoTime()+5_000_000_000L);}
 }
 static int apply(SQLiteDatabase db,AtomCatalogImportCP12.Page p){db.beginTransaction();try{int n=AtomCatalogImportCP12.applyPageCP12(db,p,System.currentTimeMillis());db.setTransactionSuccessful();return n;}finally{db.endTransaction();}}
 public static void main(String[] args)throws Exception{
  need(TrainingContinuationR973.compatible("04dcbeb86d5a92e3ea600425f8ee21b682e922d36945994ead611e9cefd377f8"),"R66_EXISTING_NUMERIC_STREAM_CONTINUES");
  Models.Draw a=draw(),b=draw();need(DrawPollingReplayR67.alreadyDurable(a,b),"IDENTICAL_CONFIRMED_POLL_READ_ONLY");
  b.numbers=new int[]{2,1,3,4,5,6,7,8,9,10};need(!DrawPollingReplayR67.alreadyDurable(a,b),"CONFLICT_STILL_REACHES_CANONICAL_WRITER");
  b=draw();b.openTime++;need(!DrawPollingReplayR67.alreadyDurable(a,b),"TIME_REPAIR_STILL_WRITTEN");
  b=draw();b.issue="202610120002";need(!DrawPollingReplayR67.alreadyDurable(a,b),"NEW_PERIOD_STILL_WRITTEN");
  b=draw();b.dirty=1;need(!DrawPollingReplayR67.alreadyDurable(a,b),"DIRTY_INPUT_NOT_REUSED");
  a.dirty=1;b=draw();need(!DrawPollingReplayR67.alreadyDurable(a,b),"DIRTY_SAVED_ROW_NOT_REUSED");
  need(!DrawPollingReplayR67.alreadyDurable(null,b),"MISSING_DRAW_NOT_REUSED");
  CountdownProjectionR971.Anchor old=CountdownProjectionR971.Anchor.capture("202610120002",1,"fixture",1060000,1000000,1000);
  need(old.remaining("202610120002",1,"fixture",21000)==40000,"MONOTONIC_COUNTDOWN_ADVANCES");
  need(old.remaining("202610120002",1,"fixture",62000)==0,"EXPIRED_DRAW_NOT_RENEWED");
  need(old.remaining("202610120003",1,"fixture",62000)==CountdownProjectionR971.STALE_CLOCK,"OLD_TARGET_CANNOT_DRIVE_NEW_PERIOD");
  CountdownProjectionR971.Anchor next=CountdownProjectionR971.Anchor.capture("202610120003",2,"fixture",1120000,1061000,62000);
  need(next.remaining("202610120003",2,"fixture",62000)==59000,"NEXT_DURABLE_DRAW_RESTARTS_COUNTDOWN");
  Path root=Path.of(args[1]);Files.createDirectories(root);
  try(SQLiteDatabase db=new SQLiteDatabase(args[0],root.resolve("atom-pages.db").toString())){
   db.execSQL("CREATE TABLE research_row_revision_v2(source_table TEXT,source_rowid INTEGER,PRIMARY KEY(source_table,source_rowid))");
   db.execSQL("CREATE TABLE research_atom_spec_v2(atom_id TEXT PRIMARY KEY,family TEXT,spec_version TEXT,params_json TEXT,position INTEGER,output_type TEXT,min_history INTEGER,spec_hash TEXT,missing_policy TEXT,state TEXT)");
   db.beginTransaction();try{AtomCatalogImportCP12.schemaCP12(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
   Path asset=Path.of(args[2],"app/src/main/assets",AtomCatalogPinnedCP12.ASSET);
   AtomCatalogImportCP12.Page p=page(asset,AtomCatalogImportCP12.readCursorCP12(db));need(p.rows.size()==96,"BOUNDED_96_ROW_PINNED_PAGE");
   need(apply(db,p)==96,"FIRST_PAGE_COMMITTED");need(apply(db,p)==96,"REPLAY_DOES_NOT_DOUBLE_IMPORT");
   AtomCatalogImportCP12.Page q=page(asset,AtomCatalogImportCP12.readCursorCP12(db));need(apply(db,q)==192,"RESUME_EXACT_PREFIX_AND_OFFSET");
   boolean stale=false;try{apply(db,p);}catch(IllegalStateException expected){stale=true;}need(stale,"STALE_PAGE_CAS_REJECTED");
   try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM research_atom_spec_v2",null)){need(c.moveToFirst()&&c.getInt(0)==192,"REJECTED_REPLAY_DOES_NOT_MUTATE_REGISTRY");}
   need(!AtomCatalogImportCP12.completeCP12(db),"PARTIAL_IMPORT_NOT_MARKED_COMPLETE");
   DbStore store=HostProductR974.store(db);ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(1000);RealtimePressureGateR811.beginAnalysis();
   try{boolean yielded=false;try{store.coordinatorR36().runAtomicWriteOrThrowF01("CP12_SCOPE_ADMISSION_FIXTURE",d->{d.execSQL("DELETE FROM research_atom_spec_v2");return true;});}catch(ResearchWorkBudgetR927.Yield expected){yielded=true;}need(yielded,"CP12_RESEARCH_WRITER_PREEMPTED_BEFORE_MUTATION");}
   finally{RealtimePressureGateR811.endAnalysis();ResearchWorkBudgetR927.leave(scope);}
   need(!ResearchWorkBudgetR927.isActive(),"RESEARCH_SCOPE_RESTORED");
  }
  System.out.println("R67_REGRESSION_PASS|checks="+checks+"|HOST_SYNTHETIC_NOT_DEVICE");
 }
}
