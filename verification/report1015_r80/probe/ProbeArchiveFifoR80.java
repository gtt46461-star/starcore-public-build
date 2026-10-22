package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.json.JSONObject;
/** Real JNI/CAS owner contract with explicitly synthetic terminal retention receipts. */
public final class ProbeArchiveFifoR80 {
 static int checks;
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
 static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
 static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getString(0);}}
 static String hash(String s){return V51ChecksumEngine.sha256(s);}
 static String fileHash(File f)throws Exception{return ReportArchiveCP4.hash(f,()->true);}
 static void tx(SQLiteDatabase db,Runnable r){db.beginTransaction();try{r.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
 static int prune(SQLiteDatabase db,RetentionArchiveProtocolCP12.Receipt p,boolean commit){db.beginTransaction();try{int result=DataRetentionGovernorR900.publishAndPruneCP12(db,p,"PROBE_FIFO_R80",Long.MAX_VALUE);if(commit)db.setTransactionSuccessful();return result;}finally{db.endTransaction();}}
 static RetentionArchiveProtocolCP12.Receipt archive(File source,File root,RetentionArchiveProtocolCP12.Page page)throws Exception {
  File job=new File(root,page.table+"-"+page.requestId);job.mkdirs();File snapshot=new File(job,"snapshot.db");ReportNativeCP4.backup(source,snapshot,32L<<20,()->true);
  RetentionArchiveProtocolCP12.Receipt receipt=RetentionArchiveIoCP12.run(root,page,job,snapshot,fileHash(snapshot),()->true);
  File cas=RetentionArchiveProtocolCP12.casFile(root,receipt.archiveHash);ok(fileHash(cas).equals(receipt.archiveHash),"independent exact CAS hash "+page.table);
  File request=new File(root,"retention-requests-cp12/"+page.requestId+".json"),reply=new File(root,"retention-replies-cp12/"+page.requestId+".receipt");request.getParentFile().mkdirs();ReportJobJournalCP12.writeAtomic(request,page.requestJson);RetentionArchiveIoCP12.writeReceipt(reply,receipt);
  ok(RetentionArchiveIoCP12.validatePublishedProof(root,reply,receipt).manifestHash.equals(receipt.manifestHash),"durable exact archive proof "+page.table);return receipt;
 }
 public static void main(String[] a)throws Exception {
  File dbFile=new File(a[1]),root=new File(a[2]);root.mkdirs();ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics","host");RealtimePressureGateR811.resetForTests();
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],dbFile.getPath())){db.setForeignKeyConstraintsEnabled(true);
   if(a.length>4){File[] replies=new File(root,"retention-replies-cp12").listFiles((dir,name)->name.endsWith(".receipt"));ok(replies!=null&&replies.length==4,"restart discovers all four original probe archive groups");for(File reply:replies){RetentionArchiveProtocolCP12.Receipt r=RetentionArchiveIoCP12.readReceipt(reply);ok(fileHash(RetentionArchiveProtocolCP12.casFile(root,r.archiveHash)).equals(r.archiveHash),"restart exact original body hash");ok(n(db,"SELECT COUNT(*) FROM research_evidence_locator_v2 WHERE archive_id='"+r.archiveId+"'")==r.members.size(),"restart complete original audit locators");} }
   else {
    String[] inputHashes=new String[2],forecastHashes=new String[2],labelHashes={hash("fifo label A"),hash("fifo label B")};
    tx(db,()->{for(int i=0;i<2;i++){String anchor="20261013"+(i==0?"0001":"0003"),target="20261013"+(i==0?"0002":"0004");String input=i==0?"{}":"{\"diagnosticLabel\":\""+labelHashes[0]+"\"}";inputHashes[i]=hash(input);forecastHashes[i]=hash("fifo forecast "+i);
      db.execSQL("INSERT INTO r80_probe_input VALUES(?,?,?,?,?,?,1)",new Object[]{anchor,MultiHorizonProbeR80.PROTOCOL_HASH,hash("fifo source"),hash("fifo model"),input,inputHashes[i]});
      db.execSQL("INSERT INTO r80_probe_forecast VALUES(?,1,?,?,?,?,?,?,?,1)",new Object[]{anchor,target,MultiHorizonProbeR80.ROLE,MultiHorizonProbeR80.PROTOCOL_HASH,inputHashes[i],hash("fifo model"),forecastHashes[i],"{}"});
      db.execSQL("INSERT INTO r80_probe_label VALUES(?,?,'REJECTED','SYNTHETIC_RETENTION_ONLY',?,2,?,?,1)",new Object[]{forecastHashes[i],target,hash("fifo settlement"),"{}",labelHashes[i]});
      db.execSQL("INSERT INTO r80_probe_opportunity VALUES(?, ?,?,'FROZEN','SYNTHETIC_RETENTION_ONLY','NOT_RUN',1,'{}',?)",new Object[]{anchor,target,MultiHorizonProbeR80.PROTOCOL_HASH,hash("fifo opportunity "+i)});
    }db.execSQL("INSERT INTO r80_probe_diagnostic_ref VALUES(?,?)",new Object[]{inputHashes[1],labelHashes[0]});});
    boolean blocked=false;try{db.execSQL("DELETE FROM r80_probe_diagnostic_ref");}catch(RuntimeException e){blocked=true;}ok(blocked,"live immutable input protects diagnostic reference");
    String[] issue={TargetIssueResolver.next(text(db,"SELECT issue FROM draws ORDER BY issue DESC LIMIT 1")).targetIssue};tx(db,()->{for(int i=0;i<5000;i++){db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,1,2,2,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{issue[0]});issue[0]=TargetIssueResolver.next(issue[0]).targetIssue;}});
    for(String table:new String[]{MultiHorizonProbeR80.FORECAST,MultiHorizonProbeR80.INPUT,MultiHorizonProbeR80.LABEL,MultiHorizonProbeR80.OPPORTUNITY}) {
      if(table.equals(MultiHorizonProbeR80.INPUT)){String eligible=MultiHorizonProbeR80.archiveEligibleR80(MultiHorizonProbeR80.LABEL,"l","'999999999999'");ok(n(db,"SELECT COUNT(*) FROM r80_probe_label l WHERE l.label_hash='"+labelHashes[0]+"' AND "+eligible)==0,"diagnostic predecessor label waits until consuming input archived");}
      RetentionArchiveProtocolCP12.Page page=DataRetentionGovernorR900.prepareArchivePageCP12(db,table);ok(page!=null&&page.rows.size()==2,"FIFO dependency release selects exactly two terminal originals "+table);
      RetentionArchiveProtocolCP12.Receipt receipt=archive(dbFile,root,page);long before=n(db,"SELECT COUNT(*) FROM "+table);ok(prune(db,receipt,false)==2,"verified CAS permits original release before rollback "+table);ok(n(db,"SELECT COUNT(*) FROM "+table)==before,"rollback restores entire original dependency chain "+table);
      ok(prune(db,receipt,true)==2,"verified CAS removes only released original rows "+table);ok(prune(db,receipt,true)==0,"duplicate archive proof idempotent "+table);
      ok(n(db,"SELECT COUNT(*) FROM research_evidence_locator_v2 WHERE archive_id='"+receipt.archiveId+"'")==2,"two exact original audit locators "+table);
      try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){ok(!c.moveToFirst(),"foreign key integrity after "+table);}
    }
    ok(n(db,"SELECT COUNT(*) FROM r80_probe_diagnostic_ref")==0,"input CAS release removes derived reference without deadlock");ok(n(db,"SELECT COUNT(*) FROM r80_probe_forecast")==4,"genuine pending original forecasts never archived");ok(n(db,"SELECT COUNT(*) FROM research_retention_permit_v2")==0,"no leaked deletion authority");
   }
  }
  Files.writeString(Path.of(a[3]),new JSONObject().put("status","PASS").put("checks",checks).put("scope","ACTUAL_PRODUCT_JNI_CAS_FIFO_SYNTHETIC_RETENTION_NOT_EFFICACY").toString()+"\n");
 }
}
