package com.ethk3.novaonepro.r952;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.json.JSONObject;
/** Actual original-source chain -> real JNI SCROW CAS -> unique archive/prune owner.
 * The appended draws are synthetic age fixtures, never predictions or qualification. */
public final class BoundRetentionReviewR80 {
 static int checks;static void ok(boolean b,String m){if(!b)throw new AssertionError(m);checks++;System.out.println("PASS "+m);}
 static long n(SQLiteDatabase db,String q){try(Cursor c=db.rawQuery(q,null)){if(!c.moveToFirst())throw new AssertionError(q);return c.getLong(0);}}
 static String hash(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))>=0)d.update(b,0,n);}StringBuilder hex=new StringBuilder();for(byte b:d.digest())hex.append(String.format("%02x",b&255));return hex.toString();}
 static void tx(SQLiteDatabase db,Runnable r){db.beginTransaction();try{r.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
 public static int prune(SQLiteDatabase db,RetentionArchiveProtocolCP12.Receipt p,boolean commit){db.beginTransaction();try{int n=DataRetentionGovernorR900.publishAndPruneCP12(db,p,"BOUND_RETENTION_REVIEW_R80",Long.MAX_VALUE);if(commit)db.setTransactionSuccessful();return n;}finally{db.endTransaction();}}
 static void denied(SQLiteDatabase db,String sql){boolean denied=false;db.beginTransaction();try{db.execSQL(sql);}catch(RuntimeException e){denied=true;}finally{db.endTransaction();}ok(denied,"unpermitted delete rejected");}
 public static RetentionArchiveProtocolCP12.Receipt archive(SQLiteDatabase db,File source,File root,RetentionArchiveProtocolCP12.Page p)throws Exception{
  File job=new File(root,p.table+"-"+p.requestId);job.mkdirs();File snapshot=new File(job,"snapshot.db");ReportNativeCP4.backup(source,snapshot,32L<<20,()->true);
  RetentionArchiveProtocolCP12.Receipt receipt=RetentionArchiveIoCP12.run(root,p,job,snapshot,hash(snapshot),()->true);
  File cas=RetentionArchiveProtocolCP12.casFile(root,receipt.archiveHash);ok(hash(cas).equals(receipt.archiveHash),"original archive bytes independently hash verified "+p.table);
  File request=new File(root,"retention-requests-cp12/"+p.requestId+".json"),reply=new File(root,"retention-replies-cp12/"+p.requestId+".receipt");request.getParentFile().mkdirs();ReportJobJournalCP12.writeAtomic(request,p.requestJson);RetentionArchiveIoCP12.writeReceipt(reply,receipt);
  ok(RetentionArchiveIoCP12.validatePublishedProof(root,reply,receipt).manifestHash.equals(receipt.manifestHash),"durable published receipt readback "+p.table);
  try(RandomAccessFile io=new RandomAccessFile(cas,"rw")){long offset=io.length()-1;io.seek(offset);int old=io.read();io.seek(offset);io.write(old^1);boolean rejected=false;try{RetentionArchiveIoCP12.verifyPreparedFile(cas,p,receipt.snapshotHash,()->true);}catch(IOException expected){rejected=true;}finally{io.seek(offset);io.write(old);}ok(rejected,"archive tamper fails verified proof "+p.table);}
  return RetentionArchiveIoCP12.validatePublishedProof(root,reply,receipt);
 }
 public static void main(String[] a)throws Exception{
  File source=new File(a[1]),root=new File(a[2]);root.mkdirs();ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics","host");
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],source.getPath())){db.setForeignKeyConstraintsEnabled(true);
   if(a.length>4&&a[4].equals("resume")){File[] receipts=new File(root,"retention-replies-cp12").listFiles((dir,name)->name.endsWith(".receipt"));ok(receipts!=null&&receipts.length==3,"restart discovers all durable original archives");for(File f:receipts){RetentionArchiveProtocolCP12.Receipt r=RetentionArchiveIoCP12.readReceipt(f);ok(RetentionArchiveIoCP12.validatePublishedProof(root,f,r).archiveHash.equals(r.archiveHash),"restart verifies unchanged archived original bytes");if(!r.page.table.equals("draws"))ok(n(db,"SELECT COUNT(*) FROM research_evidence_locator_v2 WHERE archive_id='"+r.archiveId+"'")==r.members.size(),"restart preserves complete original audit locators");}Files.writeString(Path.of(a[3]),new JSONObject().put("status","PASS").put("checks",checks).put("scope","SEPARATE_JVM_ORIGINAL_BOUND_CAS_RESTART").toString()+"\n");return;}
   BoundAtomLifecycleR80.Cohort cohort=BoundAtomLifecycleR80.activeR80(db);CandidateCompilerR952.Program program=CandidateCompilerR952.compile(cohort.ast,cohort.ast.graphSpec.manifest,BoundReceiptOracleReviewR80.specs(db,cohort));
   String target;long drawAt;try(Cursor row=db.rawQuery("SELECT i.target_issue,d.open_time FROM "+BoundAtomLifecycleR80.INPUT+" i JOIN draws d ON d.issue=i.target_issue WHERE i.available=1 ORDER BY i.target_issue LIMIT 1",null)){ok(row.moveToFirst(),"original receipt selected");target=row.getString(0);drawAt=row.getLong(1);}
   String original=BoundAtomLifecycleR80.extractOriginalR80(db,program,target,drawAt);
   String liveTarget=TargetIssueResolver.next(cohort.selectionIssue).targetIssue;long liveReadAt=System.currentTimeMillis();String liveProof=BoundAtomLifecycleR80.prepareInputsR80(db,cohort,program,liveTarget,liveReadAt).sourceProof;
   // Append exactly5000 age-only draws. No source predictions or Future are inferred.
   String[] issue={TargetIssueResolver.next(target).targetIssue};try(Cursor r=db.rawQuery("SELECT issue FROM draws ORDER BY issue DESC LIMIT 1",null)){r.moveToFirst();issue[0]=TargetIssueResolver.next(r.getString(0)).targetIssue;}
   if(n(db,"SELECT COUNT(*) FROM draws")<5000)tx(db,()->{for(int i=0;i<5000;i++){db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,1,2,2,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{issue[0]});issue[0]=TargetIssueResolver.next(issue[0]).targetIssue;}});
   String sourceIssue;long rowid,generation;try(Cursor r=db.rawQuery("SELECT s.target_issue,d.rowid,COALESCE((SELECT generation FROM research_row_revision_v2 v WHERE v.source_table='draws' AND v.source_rowid=d.rowid),0) FROM "+BoundAtomLifecycleR80.SOURCE+" s JOIN draws d ON d.issue=s.target_issue ORDER BY s.target_issue LIMIT 1",null)){ok(r.moveToFirst(),"original settled source draw selected");sourceIssue=r.getString(0);rowid=r.getLong(1);generation=r.getLong(2);}
   String cutoff;try(Cursor r=db.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999",null)){r.moveToFirst();cutoff=r.getString(0);}
   RetentionArchiveProtocolCP12.Page rawPage=new RetentionArchiveProtocolCP12.Page("draws",cutoff,1,DataRetentionGovernorR900.schemaHashCP12(db,"draws"),ReleaseManifestR926.MANIFEST_SHA256,RetentionTableRegistryCP12.hashForTable("draws"),Collections.singletonList(new RetentionArchiveProtocolCP12.RowVersion(rowid,generation)));
   RetentionArchiveProtocolCP12.Receipt raw=archive(db,source,root,rawPage);ok(prune(db,raw,true)==1,"settled original raw draw pruned by verified CAS owner");ok(n(db,"SELECT COUNT(*) FROM draws WHERE issue='"+sourceIssue+"'")==0,"raw draw absent");
   ok(original.equals(BoundAtomLifecycleR80.extractOriginalR80(db,program,target,drawAt)),"exact original replay unchanged after raw draw eviction");
   for(String table:new String[]{BoundAtomLifecycleR80.SOURCE,BoundAtomLifecycleR80.INPUT}){
    denied(db,"DELETE FROM "+table+" WHERE rowid IN(SELECT rowid FROM "+table+" LIMIT 1)");
    tx(db,()->db.execSQL("UPDATE "+BoundAtomLifecycleR80.COHORT+" SET state='PROPOSED' WHERE cohort_hash=?",new Object[]{cohort.hash}));try{ok(DataRetentionGovernorR900.prepareArchivePageCP12(db,table)==null,"registration source remains protected "+table);}finally{tx(db,()->db.execSQL("UPDATE "+BoundAtomLifecycleR80.COHORT+" SET state='REGISTERED' WHERE cohort_hash=?",new Object[]{cohort.hash}));}
    db.beginTransaction();try{ResearchTaskQueueR967.enqueue(db,"TRAIN_CANDIDATE",cohort.child,liveTarget,"ROLLBACK_ONLY_RETENTION_GUARD",V51ChecksumEngine.sha256("negative-guard-input"),"NEGATIVE_GUARD_ONLY");RetentionTableRegistryCP12.Rule rule=RetentionTableRegistryCP12.require(table);try(Cursor count=db.rawQuery("SELECT COUNT(*) FROM "+table+" WHERE "+rule.eligibleSql,rule.args(cutoff,1))){ok(count.moveToFirst()&&count.getLong(0)==0,"unfinished child training guards original evidence "+table);}}finally{db.endTransaction();}
    RetentionArchiveProtocolCP12.Page page=DataRetentionGovernorR900.prepareArchivePageCP12(db,table);ok(page!=null&&!page.rows.isEmpty(),"old completed evidence eligible "+table);
    if(table.equals(BoundAtomLifecycleR80.INPUT)){try(Cursor r=db.rawQuery("SELECT i.rowid,COALESCE((SELECT generation FROM research_row_revision_v2 v WHERE v.source_table=? AND v.source_rowid=i.rowid),0) FROM "+table+" i WHERE i.target_issue=? AND available=1",new String[]{table,target})){ok(r.moveToFirst(),"archive selects consumed available original feature receipt");page=new RetentionArchiveProtocolCP12.Page(table,cutoff,1,DataRetentionGovernorR900.schemaHashCP12(db,table),ReleaseManifestR926.MANIFEST_SHA256,RetentionTableRegistryCP12.hashForTable(table),Collections.singletonList(new RetentionArchiveProtocolCP12.RowVersion(r.getLong(0),r.getLong(1))));}}
    RetentionArchiveProtocolCP12.Receipt receipt=archive(db,source,root,page);long before=n(db,"SELECT COUNT(*) FROM "+table);ok(prune(db,receipt,false)>0,"verified archive permits bounded deletion before rollback "+table);ok(n(db,"SELECT COUNT(*) FROM "+table)==before,"rollback preserves original rows "+table);
    ok(prune(db,receipt,true)>0,"originals leave hot SQLite only after verified CAS "+table);ok(prune(db,receipt,true)==0,"archive replay idempotent "+table);
    ok(n(db,"SELECT COUNT(*) FROM research_evidence_locator_v2 WHERE archive_id='"+receipt.archiveId+"'")==page.rows.size(),"audit locators retain original archived evidence "+table);
    db.beginTransaction();try{ok(!DataRetentionGovernorR900.summarizeArchiveWithinWriteCP12(db,receipt.archiveId,Long.MAX_VALUE),"audit-backed archive cannot expire "+table);}finally{db.endTransaction();}
   }
   ok(liveProof.equals(BoundAtomLifecycleR80.prepareInputsR80(db,cohort,program,liveTarget,liveReadAt).sourceProof),"recent live bound window unchanged after archival of older originals");
   ok(n(db,"SELECT COUNT(*) FROM research_retention_permit_v2")==0,"no leaked archive permits");
   Files.writeString(Path.of(a[3]),new JSONObject().put("status","PASS").put("checks",checks).put("scope","ACTUAL_PRODUCT_ORIGINAL_BOUND_SOURCE_REAL_JNI_CAS_SYNTHETIC_HOST").toString()+"\n");
  }
 }
}
