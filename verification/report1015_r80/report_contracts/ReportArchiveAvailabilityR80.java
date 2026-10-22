package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import java.io.*;import java.nio.file.*;import org.json.*;import java.lang.reflect.*;
public class ReportArchiveAvailabilityR80 {
 static int n;static void ok(boolean b,String s){if(!b)throw new AssertionError(s);n++;System.out.println("PASS "+s);}
 public static void main(String[] args)throws Exception{
 File dir=new File(args[1]);dir.mkdirs();File cas=new File(dir,"retention-cas-cp12");cas.mkdirs();File payload=new File(dir,"fixture.txt");Files.writeString(payload.toPath(),"synthetic archive bytes");String h=ReportArchiveCP4.hash(payload);File archive=new File(cas,h+".scrow");Files.copy(payload.toPath(),archive.toPath());File report=new File(dir,"availability.jsonl");
 try(SQLiteDatabase db=new SQLiteDatabase(args[0],new File(dir,"fixture.db").getPath())){
  JSONObject missing=ReportAttachmentClosureCP12.observeR80Archives(db,dir,report,()->true);ok(missing.getString("status").equals("MISSING_SCHEMA"),"missing archive schema unavailable");
  db.execSQL("CREATE TABLE research_evidence_archive_v2(archive_id TEXT,source_table TEXT,archive_hash TEXT,relative_path TEXT,row_count INTEGER,state TEXT)");db.execSQL("CREATE TABLE research_evidence_locator_v2(archive_id TEXT)");db.execSQL("INSERT INTO research_evidence_archive_v2 VALUES('synthetic','r80_probe_input',?,?,7,'VERIFIED')",new Object[]{h,"retention-cas-cp12/"+h+".scrow"});
  JSONObject valid=ReportAttachmentClosureCP12.observeR80Archives(db,dir,report,()->true);ok(valid.getLong("verifiedPages")==1,"archive bytes independently hash verified");ok(!new JSONObject(Files.readString(report.toPath())).getBoolean("bodyIncludedInCompact"),"compact metadata never claims archived body included");
  Files.writeString(archive.toPath(),"corruption");JSONObject bad=ReportAttachmentClosureCP12.observeR80Archives(db,dir,report,()->true);ok(bad.getString("status").equals("ARCHIVE_EVIDENCE_UNAVAILABLE"),"corrupt archive prevents availability success");ok(Files.readString(report.toPath()).contains("CORRUPT_HASH_MISMATCH"),"corrupt hash typed in stream");
  Files.delete(archive.toPath());ReportAttachmentClosureCP12.observeR80Archives(db,dir,report,()->true);ok(Files.readString(report.toPath()).contains("MISSING_ARCHIVE"),"missing archive typed in stream");
  db.execSQL("UPDATE research_evidence_archive_v2 SET relative_path='../unsafe'");ReportAttachmentClosureCP12.observeR80Archives(db,dir,report,()->true);ok(Files.readString(report.toPath()).contains("INVALID_REGISTRY_IDENTITY"),"invalid registry identity cannot access arbitrary path");
  db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");db.makeReadOnly();
  try(ReportReadContextCP4 ctx=new ReportReadContextCP4(db,"synthetic",h,1,new ReportReadContextCP4.RuntimeObservationCP4("{}"),new ReportReadContextCP4.SourceManifestCP4("{}"),new android.os.CancellationSignal(),3L<<20)){
   K3FailureDiagnosisEngine engine=new K3FailureDiagnosisEngine();for(String key:new String[]{"v51_third_boundary_summary","b05_window_summary","b05_state_summary","b06_motif_summary","b06_relation_summary","v51_consensus_summary"})ok(engine.stateLayerSnapshotCP4(ctx,ctx,K3FailureDiagnosisEngine.Layer.THIRD,key,"why","test").state==K3FailureDiagnosisEngine.State.UNAVAILABLE,"absent persisted key remains unavailable "+key);
   Class<?> c=Class.forName("com.ethk3.novaonepro.LiveResearchDiagnosisOrchestrator$DiagnosisContext");Constructor<?> ctor=c.getDeclaredConstructors()[0];ctor.setAccessible(true);Object dc=ctor.newInstance(1L,"",null,null);Object o=new LiveResearchDiagnosisOrchestrator();
   for(String method:new String[]{"collectNumberSurfaceSnapshotCP4","collectCrossPositionSnapshotCP4"}){Method m=o.getClass().getMethod(method,ReportReadContextCP4.class,ReportReadContextCP4.class,c);LiveResearchDiagnosisOrchestrator.StageArtifact a=(LiveResearchDiagnosisOrchestrator.StageArtifact)m.invoke(o,ctx,ctx,dc);ok(a.detail.contains("NO_RUNTIME_SAMPLE"),"legacy diagnostic does not invent runtime sample "+method);}
  }
 }
 System.out.println("PASS ARCHIVE_AND_LEGACY_CONSUMER assertions="+n+" scope=synthetic_registry_not_native_archive_decode");
 }
}
