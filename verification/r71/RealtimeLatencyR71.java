package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.*;

/** Actual compiled owners and real SQLite; all network responses and health fixtures synthetic. */
public final class RealtimeLatencyR71 {
 static int checks;
 static void need(boolean b,String name){if(!b)throw new AssertionError(name);checks++;System.out.println("PASS|"+name);}
 static class MeasuredDb extends SQLiteDatabase {
  int reads;boolean capture;String lastSql="";
  MeasuredDb(String bridge,String file)throws Exception{super(bridge,file);}
  @Override public synchronized Cursor rawQuery(String sql,String[] args){
   if(capture){reads++;lastSql=sql;if(sql.toUpperCase(Locale.ROOT).contains("COUNT("))throw new AssertionError("AGGREGATE_ON_REALTIME_UI:"+sql);}
   return super.rawQuery(sql,args);
  }
 }
 static ServiceHeartbeatStore.BusinessTruthSnapshot truth(String canonical,String settled,String oos,long at){
  return new ServiceHeartbeatStore.BusinessTruthSnapshot("HEALTHY","","202610120781",canonical,settled,oos,0,0,0,0,200,"","",at);
 }
 public static void main(String[] args)throws Exception {
  Path dir=Path.of(args[1]);Files.createDirectories(dir);
  try(MeasuredDb db=new MeasuredDb(args[0],dir.resolve("ui.db").toString())){
   DbStore store=HostProductR974.store(db);
   db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
   db.beginTransaction();try{
    db.execSQL("INSERT INTO app_state VALUES('current_issue','202610120781',1)");
    db.execSQL("INSERT INTO app_state VALUES('target_issue','202610120782',1)");
    db.execSQL("INSERT INTO app_state VALUES('service_status','RUNNING',1)");
    db.execSQL("INSERT INTO app_state VALUES('teacher_matrix_sample_count','321',1)");
    db.execSQL("INSERT INTO app_state VALUES('analysis_summary',?,1)",new Object[]{"long-diagnostic".repeat(1000)});
    for(int i=0;i<5000;i++)db.execSQL("INSERT INTO app_state VALUES(?,?,100)",new Object[]{"unrelated_research_"+i,"payload".repeat(400)});
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   HashMap<String,String> map=new HashMap<>();map.put("last_error","stale-cached-error");db.capture=true;
   long began=System.nanoTime();store.readRealtimeUiStateR71(map);Models.AppRuntimeState state=store.getUiRuntimeStateR945(map);long elapsed=(System.nanoTime()-began)/1000000;
   db.capture=false;
   need("202610120782".equals(state.targetIssue)&&"202610120781".equals(state.currentIssue),"OLD_TIMESTAMP_LIVE_TARGET_SURVIVES_5000_NEWER_RESEARCH_KEYS");
   need(db.reads==1,"ONE_INDEXED_READ_NO_TEACHER_PROFILE_COUNTS_OR_PER_KEY_FALLBACK");
   need(map.size()==62&&!map.containsKey("unrelated_research_0"),"LIVE_UI_MEMORY_BOUND_INDEPENDENT_OF_TOTAL_APP_STATE");
   need(map.get("analysis_summary").length()==1024,"LONG_DISPLAY_VALUE_BOUNDED");
   need("".equals(map.get("last_error")),"DELETED_STATE_DOES_NOT_REUSE_CACHED_ERROR");
   try(Cursor c=db.rawQuery("EXPLAIN QUERY PLAN "+db.lastSql,new String[62])){boolean indexed=false;while(c.moveToNext())indexed|=c.getString(3).contains("INDEX");need(indexed,"LIVE_UI_QUERY_USES_KEY_INDEX");}
   System.out.println("MEASURED_HOST_UI_MS|"+elapsed+"|not_phone_latency");
  }
  long now=System.currentTimeMillis();
  need(truth("202610120782","202610120781","202610120781",now).currentClosureR71("202610120781","202610120782",now),"SAME_CURRENT_TARGET_SETTLED_FRESH_IS_HEALTHY");
  need(!truth("202610120779","202610120781","202610120781",now).currentClosureR71("202610120781","202610120782",now),"STALE_CANONICAL_NEVER_GREEN");
  need(!truth("202610120782","202610120780","202610120781",now).currentClosureR71("202610120781","202610120782",now),"UNSETTLED_CURRENT_NEVER_GREEN");
  need(!truth("202610120782","202610120781","202610120780",now).currentClosureR71("202610120781","202610120782",now),"UNSETTLED_OOS_NEVER_GREEN");
  need(!truth("202610120782","202610120781","202610120781",now-15001).currentClosureR71("202610120781","202610120782",now),"OLD_HEALTH_HEARTBEAT_NEVER_GREEN");
  need(!truth("202610120782","202610120781","202610120781",now+1).currentClosureR71("202610120781","202610120782",now),"CLOCK_REGRESSION_NEVER_GREEN");
  need(!truth("202610120782","202610120781","202610120781",now).currentClosureR71(null,null,now),"UNINITIALIZED_TARGET_NEVER_GREEN");
  ApiTransportGovernanceEngine engine=new ApiTransportGovernanceEngine(1000,100,32,16);
  ApiTransportGovernanceEngine.TransportAdapter ok=url->new ApiTransportGovernanceEngine.HttpResponse(true,200,"{\"info\":\"success\"}","",480);
  int rejected=0;
  for(String code:new String[]{"OPEN_TIME_STALE","ISSUE_BEHIND_EXPECTED","CLOCK_SKEW_QUARANTINED"})for(int i=0;i<10;i++){
   ApiTransportGovernanceEngine.TransportResult r=engine.execute(BinanceGameContract.RequestSpec.latest(5),ok,(line,body,at)->ApiTransportGovernanceEngine.PayloadDecision.reject(code,"fixture"));
   if(!r.success&&r.attempts==2)rejected++;
  }
  need(rejected==30,"THIRTY_OLD_PAYLOADS_REJECTED_AND_BACKUP_TRIED_WITHOUT_OUTAGE_BACKOFF");
  need(engine.execute(BinanceGameContract.RequestSpec.latest(5),ok,(l,b,t)->ApiTransportGovernanceEngine.PayloadDecision.accept()).success,"FIRST_FRESH_RESPONSE_AVAILABLE_AFTER_STALE_STREAK");
  need(engine.snapshot().primary.openUntilMs==0&&engine.snapshot().backup.openUntilMs==0,"RESPONSIVE_ENDPOINTS_NOT_CIRCUIT_OPEN");
  ApiTransportGovernanceEngine broken=new ApiTransportGovernanceEngine(1000,100,32,16);
  for(int i=0;i<5;i++)broken.execute(BinanceGameContract.RequestSpec.latest(5),url->new ApiTransportGovernanceEngine.HttpResponse(false,503,"","fixture-outage",5));
  need(broken.snapshot().primary.openUntilMs>0&&broken.snapshot().backup.openUntilMs>0,"REAL_TRANSPORT_OUTAGE_STILL_CIRCUIT_BREAKS");
  ApiTransportGovernanceEngine invalid=new ApiTransportGovernanceEngine(1000,100,32,16);
  for(int i=0;i<5;i++)invalid.execute(BinanceGameContract.RequestSpec.latest(5),ok,(l,b,t)->ApiTransportGovernanceEngine.PayloadDecision.reject("PARSE_OR_DRAW_INVALID","fixture"));
  need(invalid.snapshot().primary.openUntilMs>0,"MALFORMED_DRAW_STILL_CIRCUIT_BREAKS");
  need(TrainingContinuationR973.compatible("b343d46328e587f99eac719c38a495bc41334c9a6b34916e523ce7bc8df13e9e"),"EXACT_R70_NUMERICAL_TRAINING_CONTINUES");
  System.out.println("R71_REALTIME_REGRESSION_PASS|checks="+checks+"|HOST_SQLITE_SYNTHETIC_NOT_PHONE");
 }
}
