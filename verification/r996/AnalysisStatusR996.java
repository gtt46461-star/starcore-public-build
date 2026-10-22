package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
/** Queuing the next target must not mark the physical executing owner idle. */
public final class AnalysisStatusR996 {
 public static void main(String[] a)throws Exception{
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   DbStore store=HostProductR974.store(db);AutoFetchService service=(AutoFetchService)UiContinuityR996.U.allocateInstance(AutoFetchService.class);UiContinuityR996.set(service,"store",store);
   long now=System.currentTimeMillis();AnalysisSingleFlightR36.Request request=new AnalysisSingleFlightR36.Request("202610050554","202610050555",1,now,now+55000,"SYNTHETIC_OWNER_STATUS",V51ChecksumEngine.sha256("owner-status"),"");
   AnalysisSingleFlightR36.SubmitResult start=AnalysisSingleFlightR36.shared().submit(request);if(!start.startNow)throw new AssertionError("sole owner");
   try{start.lease.token.pulse("NUMBER_MODEL");UiContinuityR996.call(service,"setAnalysisState",new Class<?>[]{String.class,int.class,String.class},"WAITING",0,"Newer target queued");
    if(!"1".equals(store.getState("realtime_analysis_busy_v20","")))throw new AssertionError("Queued target erased active owner busy state");
    if(!"202610050555".equals(store.getState("analysis_execution_target","")))throw new AssertionError("actual target absent");
    if(!"NUMBER_MODEL".equals(store.getState("analysis_execution_stage","")))throw new AssertionError("actual stage absent");
   }finally{AnalysisSingleFlightR36.shared().releaseAndTakeNext(start.lease);}
   UiContinuityR996.call(service,"setAnalysisState",new Class<?>[]{String.class,int.class,String.class},"OK",100,"Owner finished");
   if(!"0".equals(store.getState("realtime_analysis_busy_v20",""))||!store.getState("analysis_execution_target","").isEmpty())throw new AssertionError("idle release state");
   System.out.println("ANALYSIS_STATUS_R996_PASS assertions=4 scope=ACTUAL_SERVICE_METHOD_ACTUAL_DAO_REAL_SQLITE_HOST");
  }
 }
}
