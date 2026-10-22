package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.io.*;import java.nio.file.*;import java.util.*;import org.json.JSONObject;
/** Real source capture with an explicitly late authoritative draw clock. */
public final class BoundRejectionReviewR80 {
 static int checks;static void ok(boolean b,String s){if(!b)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
 static long n(SQLiteDatabase d,String s){return BoundChainReviewR80.n(d,s);}static void tx(SQLiteDatabase d,Runnable r){BoundChainReviewR80.tx(d,r);}
 public static void main(String[] a)throws Exception{
  File dbFile=new File(a[1]),root=new File(a[5]);root.mkdirs();ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics","host");
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){db.setForeignKeyConstraintsEnabled(true);
   if(a.length>6){File[] fs=new File(root,"retention-replies-cp12").listFiles((dir,name)->name.endsWith(".receipt"));ok(fs!=null&&fs.length==1,"restart finds terminal rejection archive");for(File f:fs){RetentionArchiveProtocolCP12.Receipt r=RetentionArchiveIoCP12.readReceipt(f);RetentionArchiveIoCP12.validatePublishedProof(root,f,r);RetentionArchiveIoCP12.verifyPreparedFile(RetentionArchiveProtocolCP12.casFile(root,r.archiveHash),r.page,r.snapshotHash,()->true);ok(n(db,"SELECT COUNT(*) FROM research_evidence_locator_v2 WHERE archive_id='"+r.archiveId+"'")==2,"restart preserves both terminal originals and verified archive bytes");}write(a[4]);return;}
   DbStore store=BoundChainReviewR80.store(db,a[2],a[3]);String target=BoundChainReviewR80.issue(1405);String table=BoundAtomLifecycleR80.SOURCE;
   long cal=n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS),formal=n(db,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW);
   tx(db,()->BoundChainReviewR80.draw(db,1404,System.currentTimeMillis()-2,true));
   int captured=FrozenCandidateAdapterR958.captureBoundSourcesR80(store,MainlineCaptureR75.snapshot(BoundChainReviewR80.issue(1404),target,ModelBundleRuntimeR932.current().hash));ok(captured==2,"two original sources captured by actual owner");
   long at=n(db,"SELECT MIN(captured_at)-1 FROM "+table+" WHERE target_issue='"+target+"'");Models.Draw[] draw=new Models.Draw[1];
   tx(db,()->{draw[0]=BoundChainReviewR80.draw(db,1405,at,false);BoundChainReviewR80.terminal(db,draw[0]);DrawDependencyInputCP12.preserveIfReferencedWithinWrite(db,n(db,"SELECT rowid FROM draws WHERE issue='"+target+"'"),target);});
   ok(n(db,"SELECT COUNT(*) FROM "+DrawDependencyInputCP12.TABLE+" WHERE issue='"+target+"'")==1,"unresolved late source pins exact draw");
   tx(db,()->BoundChainReviewR80.settleLabels(db,draw[0]));
   try(Cursor c=db.rawQuery("SELECT record_hash,proof_error,settlement_hash,draw_hash,actual,draw_open_at,draw_seen_at,authority_completed_at,settled_at,authority_hash,settled,hit,label_hash FROM "+table+" WHERE target_issue=?",new String[]{target})){int rows=0;while(c.moveToNext()){String expected=V51ChecksumEngine.sha256("R80_BOUND_REJECTION_1|"+c.getString(0)+'|'+c.getString(1)+'|'+c.getString(2)+'|'+c.getString(3)+'|'+c.getInt(4)+'|'+c.getLong(5)+'|'+c.getLong(6)+'|'+c.getLong(7)+'|'+c.getLong(8));ok(expected.equals(c.getString(9))&&c.getString(1).equals("BOUND_STRICT_TIME_REJECTED")&&c.getInt(10)==0&&c.getInt(11)==-1&&c.getString(12).isEmpty(),"terminal rejection authority independently hash verified without label");rows++;}ok(rows==2,"both source rejections terminal");}
   boolean blocked=false;db.beginTransaction();try{db.execSQL("UPDATE "+table+" SET proof_error='' WHERE target_issue=?",new Object[]{target});}catch(RuntimeException expected){blocked=true;}finally{db.endTransaction();}ok(blocked,"terminal rejection cannot be rewritten");
   tx(db,()->{db.execSQL("DELETE FROM draws WHERE issue=?",new Object[]{target});DrawDependencyInputCP12.releaseConsumedWithinWrite(db,32);});ok(n(db,"SELECT COUNT(*) FROM "+DrawDependencyInputCP12.TABLE+" WHERE issue='"+target+"'")==0,"terminal rejection releases retained draw pin");
   ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==cal&&n(db,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW)==formal,"rejection adds no calibration or formal Future");
   tx(db,()->{String issue=TargetIssueResolver.next(target).targetIssue;for(int i=0;i<5000;i++){db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10)VALUES(?,1,2,2,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{issue});issue=TargetIssueResolver.next(issue).targetIssue;}});
   String cutoff=BoundChainReviewR80.text(db,"SELECT issue FROM draws ORDER BY issue DESC LIMIT 1 OFFSET 4999");List<RetentionArchiveProtocolCP12.RowVersion> versions=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT s.rowid,COALESCE(v.generation,0) FROM "+table+" s LEFT JOIN research_row_revision_v2 v ON v.source_table=? AND v.source_rowid=s.rowid WHERE target_issue=?",new String[]{table,target})){while(c.moveToNext())versions.add(new RetentionArchiveProtocolCP12.RowVersion(c.getLong(0),c.getLong(1)));}
   RetentionArchiveProtocolCP12.Page page=new RetentionArchiveProtocolCP12.Page(table,cutoff,1,DataRetentionGovernorR900.schemaHashCP12(db,table),ReleaseManifestR926.MANIFEST_SHA256,RetentionTableRegistryCP12.hashForTable(table),versions);
   RetentionArchiveProtocolCP12.Receipt r=BoundRetentionReviewR80.archive(db,dbFile,root,page);ok(BoundRetentionReviewR80.prune(db,r,false)==2,"verified rejection archive permits prune before rollback");ok(n(db,"SELECT COUNT(*) FROM "+table+" WHERE target_issue='"+target+"'")==2,"rollback preserves rejected originals");ok(BoundRetentionReviewR80.prune(db,r,true)==2&&BoundRetentionReviewR80.prune(db,r,true)==0,"terminal rejection CAS prune idempotent");ok(n(db,"SELECT COUNT(*) FROM research_evidence_locator_v2 WHERE archive_id='"+r.archiveId+"'")==2,"archive retains both immutable rejection originals");write(a[4]);
  }
 }
 static void write(String path)throws Exception{Files.writeString(Path.of(path),new JSONObject().put("status","PASS").put("checks",checks).put("scope","ACTUAL_CAPTURE_TERMINAL_REJECTION_NATIVE_CAS_SYNTHETIC_HOST").toString()+"\n");}
}
