package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import com.ethk3.novaonepro.LiveResearchDiagnosisOrchestrator.*;
/** Actual snapshot action collector over read-only real SQLite plus report classification boundaries. */
public final class diagnostic_classification {
 static int checks;
 static void check(boolean b,String key){if(!b)throw new AssertionError(key);checks++;System.out.println("PASS|"+key);}
 static StageArtifact collect(SQLiteDatabase db)throws Exception {
  ReportReadContextCP4 report=new ReportReadContextCP4(db,"R77_FIXTURE",String.join("",Collections.nCopies(64,"a")),1791891204173L,new ReportReadContextCP4.RuntimeObservationCP4("{}"),new ReportReadContextCP4.SourceManifestCP4("{}"),new CancellationSignal(),4L*1024*1024);
  Class<?> c=Class.forName("com.ethk3.novaonepro.LiveResearchDiagnosisOrchestrator$DiagnosisContext");
  Constructor<?> ctor=c.getDeclaredConstructors()[0];ctor.setAccessible(true);
  Object ctx=ctor.newInstance(1791891204173L,"202610130694",null,null);
  Method m=LiveResearchDiagnosisOrchestrator.class.getDeclaredMethod("collectActionSpaceSnapshotCP4",ReportReadContextCP4.class,ReportReadContextCP4.class,c);m.setAccessible(true);
  try{return (StageArtifact)m.invoke(new LiveResearchDiagnosisOrchestrator(),report,report,ctx);}finally{report.close();}
 }
 static void action(String bridge,Path dir,String name,Status expected)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(bridge,dir.resolve(name+".db").toString());
  try {
   StageArtifact stage=collect(db);check(stage.status==expected,name+"_ACTUAL_COLLECTOR_STATUS_"+stage.status);
   if(expected==Status.RETEST_REQUIRED)check(stage.detail.contains("未完成证据待续写"),name+"_PENDING_NOT_COMPLETION_CLAIM");
  }finally{db.shutdown();}
 }
 static RuntimeHealthLedgerR36.BusinessTruth truth(String drawn,String predictedThrough,String settled,String oos,boolean alive)throws Exception {
  RuntimeHealthLedgerR36.BusinessTruth t=new RuntimeHealthLedgerR36.BusinessTruth();t.serviceAlive=true;t.businessAlive=alive;t.latestDrawIssue=drawn;t.latestCanonicalIssue="202610130694";t.latestSettledIssue=settled;t.latestOosIssue=oos;
  Field field=t.getClass().getDeclaredField("expectedSettledIssue");field.set(t,predictedThrough);
  Method classify=RuntimeHealthLedgerR36.class.getDeclaredMethod("classifyBusinessR77",t.getClass(),int.class);classify.setAccessible(true);classify.invoke(null,t,71);return t;
 }
 static String crashes(long now,long last,int total)throws Exception {
  Method m=CrashRecorder.class.getDeclaredMethod("recentCrashCountR77",long.class,long.class,int.class);m.setAccessible(true);return (String)m.invoke(null,now,last,total);
 }
 @SuppressWarnings("unchecked") static Finding finding(RuntimeHealthLedgerR36.BusinessTruth truth)throws Exception {
  ArrayList<Metric> m=new ArrayList<Metric>();m.add(new Metric("businessAlive","1","fixture","report",true));m.add(new Metric("rootCause",truth.rootCause,"fixture","report",true));
  m.add(new Metric("requestToCommitP95","41346","ms","report",true));
  StageArtifact a=new StageArtifact(Stage.RUNTIME,Status.FAIL,"owner","metric","summary","","",1,m,Collections.<Evidence>emptyList());
  Method derive=LiveResearchDiagnosisOrchestrator.class.getDeclaredMethod("deriveFindings",List.class);derive.setAccessible(true);
  return ((List<Finding>)derive.invoke(new LiveResearchDiagnosisOrchestrator(),Collections.singletonList(a))).get(0);
 }
 public static void main(String[] args)throws Exception {
  Path dir=Path.of(args[1]);
  action(args[0],dir,"partial_pending",Status.RETEST_REQUIRED);
  action(args[0],dir,"partial_running",Status.RETEST_REQUIRED);
  action(args[0],dir,"partial_retry",Status.RETEST_REQUIRED);
  action(args[0],dir,"partial_completed",Status.FAIL);
  action(args[0],dir,"empty_completed",Status.FAIL);
  action(args[0],dir,"partial_orphan",Status.FAIL);
  action(args[0],dir,"overflow_pending",Status.FAIL);
  action(args[0],dir,"full_completed",Status.PASS);
  action(args[0],dir,"empty_requested",Status.RETEST_REQUIRED);
  SQLiteDatabase bad=new SQLiteDatabase(args[0],dir.resolve("missing_table.db").toString());
  try{boolean rejected=false;try{collect(bad);}catch(InvocationTargetException e){rejected=e.getCause() instanceof ReportReadContextCP4.ReportReadFailureCP4;}check(rejected,"MISSING_ACTION_TABLE_EXPLICIT_SNAPSHOT_READ_FAILURE");}finally{bad.shutdown();}
  for(String variant:new String[]{"health_missing_predraw","health_existing_unsettled"}){
   SQLiteDatabase healthDb=new SQLiteDatabase(args[0],dir.resolve(variant+".db").toString());
   ReportReadContextCP4 report=new ReportReadContextCP4(healthDb,"R77_HEALTH_FIXTURE",String.join("",Collections.nCopies(64,"a")),1791891204173L,new ReportReadContextCP4.RuntimeObservationCP4("{}"),new ReportReadContextCP4.SourceManifestCP4("{}"),new CancellationSignal(),4L*1024*1024);
   try{
    RuntimeHealthLedgerR36.BusinessTruth observed=RuntimeHealthLedgerR36.businessTruthSnapshotCP4(report,report);
    String expected=variant.equals("health_missing_predraw")?"PREDRAW_COVERAGE_MISSING":"SETTLEMENT_OOS";
    check(expected.equals(observed.rootCause),variant+"_ACTUAL_SNAPSHOT_SQL_"+observed.canonical());
    check(observed.analysisP95Ms==41346L,variant+"_SLOW_ANALYSIS_METRIC_NOT_LOST");
   }finally{report.close();healthDb.shutdown();}
  }
  RuntimeHealthLedgerR36.BusinessTruth missing=truth("202610130693","202610130692","202610130692","202610130692",true);
  check("PREDRAW_COVERAGE_MISSING".equals(missing.rootCause)&&"FAIL".equals(missing.status),"PHONE_693_NO_PREDRAW_REMAINS_FAILURE");
  check(missing.drawToSettledLag==0&&missing.settledToOosLag==0,"EXCLUDED_693_NOT_FALSE_EXISTING_SETTLEMENT_BACKLOG");
  check(missing.canonical().contains("expectedSettled=202610130692")&&missing.canonical().contains("latestDrawPredrawMissing=1"),"OBLIGATION_AND_MISSING_COVERAGE_VISIBLE");
  Finding f=finding(missing);check(f.cause.contains("PREDRAW_COVERAGE_MISSING")&&f.cause.contains("41346")&&f.impact.contains("业务链异常"),"COVERAGE_AND_REAL_SLA_FAILURE_PRESERVED_IN_FINDING");
  check("SETTLEMENT_OOS".equals(truth("202610130693","202610130693","202610130692","202610130692",true).rootCause),"EXISTING_PREDRAW_UNSETTLED_REMAINS_SETTLEMENT_FAILURE");
  check("SETTLEMENT_OOS".equals(truth("202610130693","202610130693","202610130693","202610130692",true).rootCause),"MISSING_OOS_REMAINS_FAILURE");
  check("RUNTIME_CHAIN".equals(truth("202610130693","202610130693","202610130693","202610130693",false).rootCause),"DEAD_BUSINESS_REMAINS_FAILURE");
  check("HEALTHY".equals(truth("202610130693","202610130693","202610130693","202610130693",true).status),"COMPLETED_EXISTING_CHAIN_HEALTHY");
  check("0".equals(crashes(1791891204173L,1791806924296L,172)),"PHONE_OCT12_CRASH_NOT_CURRENT_TEN_MINUTE_EVENT");
  check("UNKNOWN".equals(crashes(1000000L,400000L,2)),"EXACT_TEN_MINUTE_BOUNDARY_NOT_ASSUMED_EMPTY");
  check("0".equals(crashes(1000001L,400000L,2)),"OLDER_THAN_TEN_MINUTES_EMPTY");
  check("UNKNOWN".equals(crashes(1000000L,999999L,2)),"FIXED_BUCKET_NOT_MISLABELED_EXACT_ROLLING_COUNT");
  check("UNKNOWN_CLOCK_ROLLBACK".equals(crashes(999999L,1000000L,2)),"CLOCK_ROLLBACK_NOT_FALSE_ZERO");
  check("0".equals(crashes(1000000L,0L,0)),"NO_RECORDED_EVENTS_ZERO");
  check("UNKNOWN".equals(crashes(1000000L,0L,172)),"MISSING_TIMESTAMP_WITH_HISTORY_UNKNOWN");
  String summary="{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"READ_ONLY_REAL_SQLITE_ACTION_COLLECTOR_AND_REPORT_CLASSIFICATION_FIXTURES_NOT_DEVICE\"}\n";
  Files.writeString(Path.of(args[2]),summary);System.out.print(summary);
 }
}
