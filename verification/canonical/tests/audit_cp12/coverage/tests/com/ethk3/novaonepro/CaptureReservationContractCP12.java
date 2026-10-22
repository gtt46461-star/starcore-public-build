package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.nio.file.*;import java.util.*;
/** Actual scheduler and calibration SQL exercised on SQLite; numeric inference is a separate scope. */
public final class CaptureReservationContractCP12 {
 static int n;static final String B=V51ChecksumEngine.sha256("champion"),S=V51ChecksumEngine.sha256("selector");
 static void ok(boolean b){if(!b)throw new AssertionError("check "+n);n++;}
 static long count(SQLiteDatabase db,String t,String where){try(Cursor c=db.rawQuery("SELECT COUNT(*)FROM "+t+(where.isEmpty()?"":" WHERE "+where),null)){c.moveToFirst();return c.getLong(0);}}
 static String value(SQLiteDatabase db,String sql,String arg){try(Cursor c=db.rawQuery(sql,new String[]{arg})){return c.moveToFirst()?c.getString(0):"";}}
 static void tx(SQLiteDatabase db,Runnable r,boolean commit){db.beginTransaction();try{r.run();if(commit)db.setTransactionSuccessful();}finally{db.endTransaction();}}
 static void champion(SQLiteDatabase db,String target,String bundle){db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?, ?,1,1,2,3,?,1)",new Object[]{target,bundle,V51ChecksumEngine.sha256("input:"+target),S,V51ChecksumEngine.sha256("champion:"+target+bundle)});}
 static String candidate(SQLiteDatabase db,int i,String prefix){String id=prefix+String.format("%024x",i),learner="learner:"+i,ast=V51ChecksumEngine.sha256("AST:"+i),structure=V51ChecksumEngine.sha256("freeze:"+i),run=V51ChecksumEngine.sha256("run:"+i),epoch=V51ChecksumEngine.sha256("epoch:"+i);
  db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,state,created_issue,may_create_real,created_at)VALUES(?,?,'AST2',?,'VALIDATED','202609230001',0,?)",new Object[]{learner,learner,ast,i});
  db.execSQL("INSERT INTO "+ResearchEvaluationRunR967.RUN+" VALUES(?,?,'202609230001',?,?,'{}',300,'artifact',?,?,?)",new Object[]{run,learner,ast,TrainingCodeIdentityR964.SHA256,ResearchEvaluationRunR967.SPLIT,epoch,i});
  db.execSQL("INSERT INTO "+ResearchEvaluationRunR967.EVAL+" VALUES(?,?, 'AST2','{}',?,?,'202609220144','202609230001','artifact',?,?,?,?,300)",new Object[]{id,learner,structure,V51ChecksumEngine.sha256("model:"+i),i,run,TrainingCodeIdentityR964.SHA256,epoch});return id;
 }
 static void capture(SQLiteDatabase db,GlobalResearchScheduleR932.CaptureReservationCP11 r,String target,String prediction,double score){CandidateCalibrationR932.captureOrPredict(db,r.key,r.freeze,B,S,target,prediction,score,1,1,2,3);}
 public static void main(String[] args)throws Exception{
  final boolean expectedQualified=args.length>3&&args[3].equals("qualified");
  SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
  try{
   tx(db,()->CandidateCalibrationR932.qualitySchemaCP12(db),true);
   ok(CandidateCalibrationR932.inputStateCP12("READY")==CandidateCalibrationR932.CalibrationInputState.READY_TO_FIT);
   for(int i=1;i<=9;i++)candidate(db,i,ResearchEvaluationRunR967.PREFIX);
   String t="202609230001";champion(db,t,B);
   tx(db,()->ok(GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,t,B,S)==8),false);
   ok(count(db,GlobalResearchScheduleR932.RESERVATION_CP11,"")==0);ok(count(db,CandidateCalibrationR932.STUDY,"")==0);ok(count(db,ResearchMultiplicityR932.RESEARCH,"")==0);
   tx(db,()->ok(GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,t,B,S)==8),true);
   ok(GlobalResearchScheduleR932.dueReservationsCP11(db,t).size()==8);ok(count(db,GlobalResearchScheduleR932.RESERVATION_CP11,"")==8);
   tx(db,()->ok(GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,t,B,S)==0),true);ok(count(db,CandidateCalibrationR932.STUDY,"")==8);
   GlobalResearchScheduleR932.CaptureReservationCP11 r=GlobalResearchScheduleR932.dueReservationsCP11(db,t).get(0);String pred=V51ChecksumEngine.sha256("predict:"+t);
   tx(db,()->capture(db,r,t,pred,.5),false);ok(count(db,CandidateCalibrationR932.OBS,"")==0);ok(GlobalResearchScheduleR932.dueReservationsCP11(db,t).get(0).generation==0);
   tx(db,()->capture(db,r,t,pred,.5),true);ok(count(db,CandidateCalibrationR932.OBS,"")==1);ok("ACTIVE".equals(value(db,"SELECT state FROM research_capture_reservation_v2 WHERE reservation_id=?",r.id)));
   tx(db,()->capture(db,r,t,pred,.5),true);ok(count(db,CandidateCalibrationR932.OBS,"")==1);ok("1".equals(value(db,"SELECT generation FROM research_capture_reservation_v2 WHERE reservation_id=?",r.id)));
   tx(db,()->{try{capture(db,r,t,V51ChecksumEngine.sha256("different"),.5);throw new AssertionError();}catch(IllegalStateException e){ok(e.getMessage().equals("R932_CAL_OBSERVATION_DRIFT"));}},false);
   tx(db,()->{try{GlobalResearchScheduleR932.completeReservationCaptureCP11(db,r,r.key,r.freeze,r.study,t,1);throw new AssertionError();}catch(IllegalStateException e){ok(e.getMessage().equals("CAPTURE_RESERVATION_CAS_LOST"));}},false);
   GlobalResearchScheduleR932.CaptureReservationCP11 held=GlobalResearchScheduleR932.dueReservationsCP11(db,t).get(0);
   tx(db,()->GlobalResearchScheduleR932.holdReservationCP12(db,held,t,"MEMORY_PRESSURE"),true);
   ok("MEMORY_PRESSURE".equals(value(db,"SELECT last_reason FROM research_capture_reservation_v2 WHERE reservation_id=?",held.id)));
   String gap="202609230003";champion(db,gap,B);
   tx(db,()->GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,gap,B,S),false);ok(count(db,"research_capture_gap_v2","")==0);ok("COLLECTING".equals(value(db,"SELECT state FROM "+CandidateCalibrationR932.STUDY+" WHERE study_hash=?",r.study)));
   tx(db,()->GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,gap,B,S),true);ok(count(db,"research_capture_gap_v2","")==8);ok("FAILED_GAP".equals(value(db,"SELECT state FROM research_capture_reservation_v2 WHERE reservation_id=?",r.id)));ok("BLOCKED_INVALID_INPUT".equals(value(db,"SELECT state FROM "+CandidateCalibrationR932.STUDY+" WHERE study_hash=?",r.study)));ok(count(db,CandidateCalibrationR932.OBS,"")==1);
   ok("MEMORY_PRESSURE".equals(value(db,"SELECT reason FROM research_capture_gap_v2 WHERE request_ref=?",held.id)));
   List<GlobalResearchScheduleR932.CaptureReservationCP11> due=GlobalResearchScheduleR932.dueReservationsCP11(db,gap);ok(due.size()==8);GlobalResearchScheduleR932.CaptureReservationCP11 current=due.get(0);ok(!current.study.equals(r.study));ok("0".equals(value(db,"SELECT captured_n FROM "+CandidateCalibrationR932.STUDY+" WHERE study_hash=?",current.study)));
   // 241 actual prospective observations. Each committed capture alone advances the next issue.
   String target=gap;
   for(int i=0;i<241;i++){
    final String issue=target,hash=V51ChecksumEngine.sha256("p:"+i);final GlobalResearchScheduleR932.CaptureReservationCP11 reservation=GlobalResearchScheduleR932.reservationForStudyCP11(db,current.study);
    tx(db,()->capture(db,reservation,issue,hash,.25+.002*(Integer.parseInt(issue.substring(8))%100)),true);
    ok(Integer.toString(i+1).equals(value(db,"SELECT captured_n FROM "+CandidateCalibrationR932.STUDY+" WHERE study_hash=?",current.study)));target=ResearchEvaluationRunR967.nextEvaluation(current.key,target);
   }
   ok("COMPLETE".equals(value(db,"SELECT state FROM research_capture_reservation_v2 WHERE reservation_id=?",current.id)));ok("241".equals(value(db,"SELECT captured_n FROM "+CandidateCalibrationR932.STUDY+" WHERE study_hash=?",current.study)));ok(count(db,CandidateCalibrationR932.MODEL,"")==0);ok(count(db,FixedResearchWindowR932.WINDOW,"")==0);
   String settledIssue=gap;
   for(int i=0;i<241;i++){
    Models.Draw draw=new Models.Draw();draw.issue=settledIssue;draw.openTime=1000+i;draw.fetchTime=2000+i;
    draw.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};
    if(expectedQualified||i<121&&i%10>=3)draw.numbers=new int[]{4,2,3,1,5,6,7,8,9,10};
    tx(db,()->CandidateCalibrationR932.settle(db,draw),true);settledIssue=ResearchEvaluationRunR967.nextEvaluation(current.key,settledIssue);
   }
   ok("241".equals(value(db,"SELECT settled_n FROM "+CandidateCalibrationR932.STUDY+" WHERE study_hash=?",current.study)));
   ok("READY".equals(value(db,"SELECT state FROM "+CandidateCalibrationR932.STUDY+" WHERE study_hash=?",current.study)));
   CandidateCalibrationR932.FitSnapshot fit=CandidateCalibrationR932.loadFit(db,current.study);ok(fit!=null&&!db.inTransaction());
   String fitted=CandidateCalibrationR932.fitModel(fit);org.json.JSONObject calibrated=new org.json.JSONObject(fitted);
   int support=0;double sumY=0,ps=0;for(int i=0;i<10;i++){support+=calibrated.getJSONArray("validation_bin_counts").getInt(i);sumY+=calibrated.getJSONArray("validation_bin_sum_y").getDouble(i);ps+=calibrated.getJSONArray("validation_bin_sum_p").getDouble(i);}
   ok(support==120&&sumY>=0&&sumY<=120&&ps>0&&ps<120);ok(calibrated.getDouble("validation_baseline_brier")>=0);
   tx(db,()->{try{CandidateCalibrationR932.publishFit(db,fit,fitted+" ");throw new AssertionError();}catch(IllegalStateException e){ok("CALIBRATION_UNPREPARED_MODEL".equals(e.getMessage()));}},false);
   tx(db,()->ok(CandidateCalibrationR932.publishFit(db,fit,fitted)),false);ok(count(db,CandidateCalibrationR932.MODEL,"")==0);
   tx(db,()->ok(CandidateCalibrationR932.publishFit(db,fit,fitted)),true);ok(count(db,CandidateCalibrationR932.MODEL,"")==1);ok("FROZEN".equals(value(db,"SELECT state FROM "+CandidateCalibrationR932.STUDY+" WHERE study_hash=?",current.study)));
   tx(db,()->ok(!CandidateCalibrationR932.publishFit(db,fit,fitted)),true);
   CandidateCalibrationR932.Qualification quality=CandidateCalibrationR932.qualificationForStudyCP12(db,current.study);
   ok(expectedQualified?quality.qualified()&&quality.state==CandidateCalibrationR932.QualificationState.CALIBRATION_QUALIFIED&&quality.reason.isEmpty():!quality.qualified()&&quality.state==CandidateCalibrationR932.QualificationState.FITTED_BUT_NOT_QUALIFIED&&quality.reason.equals("CALIBRATION_QUALITY_BLOCKED"));
   ok(count(db,CandidateCalibrationR932.QUALITY_CP12,"")==1);
   final String nextAfterCalibration=target;
   tx(db,()->{CandidateCalibrationR932.Probability probability=CandidateCalibrationR932.captureOrPredict(db,current.key,current.freeze,B,S,nextAfterCalibration,V51ChecksumEngine.sha256("postfit"),.4,1,1,2,3);ok(expectedQualified?probability!=null&&Double.isFinite(probability.value)&&probability.value>=0&&probability.value<=1:probability==null);},true);
   tx(db,()->{try{db.execSQL("DELETE FROM "+CandidateCalibrationR932.QUALITY_CP12+" WHERE study_hash=?",new Object[]{current.study});throw new AssertionError();}catch(IllegalStateException refused){ok(refused.getMessage().contains("CALIBRATION_QUALITY_PERMANENT"));}},false);
   tx(db,()->{try{db.execSQL("INSERT OR REPLACE INTO "+CandidateCalibrationR932.QUALITY_CP12+" SELECT * FROM "+CandidateCalibrationR932.QUALITY_CP12+" WHERE study_hash=?",new Object[]{current.study});throw new AssertionError();}catch(IllegalStateException refused){ok(refused.getMessage().contains("CALIBRATION_QUALITY_REPLACE_FORBIDDEN"));}},false);
   tx(db,()->{try{db.execSQL("UPDATE "+CandidateCalibrationR932.QUALITY_CP12+" SET reason='CHANGED' WHERE study_hash=?",new Object[]{current.study});throw new AssertionError();}catch(IllegalStateException refused){ok(refused.getMessage().contains("CALIBRATION_QUALITY_IMMUTABLE"));}},false);

   // A legacy/corrupt model payload is rejected before JSON decoding or a large CursorWindow.
   final String tooLarge=String.join("",Collections.nCopies(65537,"x"));
   tx(db,()->{try{db.execSQL("UPDATE "+CandidateCalibrationR932.MODEL+" SET model_json=?,model_hash=? WHERE study_hash=?",new Object[]{tooLarge,V51ChecksumEngine.sha256(tooLarge),current.study});throw new AssertionError();}catch(IllegalStateException immutable){ok(immutable.getMessage().contains("F05_CAL_MODEL_IMMUTABLE"));}},false);
   final String oversizedBundle=V51ChecksumEngine.sha256("EXPLICIT_OVERSIZED_CALIBRATION_FIXTURE");
   final String oversizedStudy=CandidateCalibrationR932.study(db,current.key,current.freeze,oversizedBundle,S);
   tx(db,()->{db.execSQL("INSERT INTO "+CandidateCalibrationR932.MODEL+" VALUES(?,?,'202609230001','202609230001',?,?,1)",new Object[]{oversizedStudy,current.key,tooLarge,V51ChecksumEngine.sha256(tooLarge)});try{CandidateCalibrationR932.qualificationForStudyCP12(db,oversizedStudy);throw new AssertionError();}catch(IllegalStateException bounded){ok("R932_CAL_MODEL_BOUND".equals(bounded.getMessage()));}try{CandidateCalibrationR932.captureOrPredict(db,current.key,current.freeze,oversizedBundle,S,nextAfterCalibration,V51ChecksumEngine.sha256("oversized"),.4,1,1,2,3);throw new AssertionError();}catch(IllegalStateException bounded){ok("R932_CAL_MODEL_BOUND".equals(bounded.getMessage()));}},false);
   tx(db,()->{try{CandidateCalibrationR932.fitOne(db);throw new AssertionError();}catch(IllegalStateException writer){ok("R932_CAL_FIT_REQUIRES_NO_WRITER".equals(writer.getMessage()));}},false);
   ok(CandidateCalibrationR932.qualificationForStudyCP12(db,current.study).qualified()==expectedQualified);
   ok(count(db,FixedResearchWindowR932.WINDOW,"")==0);ok(count(db,DbHelper.TABLE_TRADES,"")==0);
   // A real new dependency retires an ACTIVE reservation without rewriting or copying old evidence.
   String changed="202609230004";String nextBundle=V51ChecksumEngine.sha256("new_bundle");champion(db,changed,nextBundle);
   tx(db,()->GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,changed,nextBundle,S),true);ok(count(db,GlobalResearchScheduleR932.RESERVATION_CP11,"state='RETIRED'")==7);ok(count(db,CandidateCalibrationR932.OBS,"")==242);ok(GlobalResearchScheduleR932.dueReservationsCP11(db,changed).size()==8);
   // No real champion: fail before writing any reservation.
   long before=count(db,GlobalResearchScheduleR932.RESERVATION_CP11,"");tx(db,()->{try{GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,"202609230005",B,S);throw new AssertionError();}catch(IllegalStateException e){ok(e.getMessage().equals("CAPTURE_CHAMPION_MISSING_OR_AMBIGUOUS"));}},false);ok(before==count(db,GlobalResearchScheduleR932.RESERVATION_CP11,""));
   ok("202609240001".equals(ResearchEvaluationRunR967.nextEvaluation(ResearchEvaluationRunR967.PREFIX,"202609231440")));ok("202609240002".equals(ResearchEvaluationRunR967.nextEvaluation(ResearchEvaluationRunR967.LEGACY_PREFIX,"202609231440")));
   try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){ok(!c.moveToFirst());}
   Files.writeString(Path.of(args[2]),"{\"status\":\"PASS\",\"assertions\":"+n+",\"full_capture_n\":241,\"actual_scheduler_and_calibration_sql\":true,\"scope\":\"Host SQLite actual reservation and calibration methods, including rollback/replay/generation/capacity/gaps/dependency/241 completion + settle READY + actual 160-epoch fit outside writer + model publication fence + typedqualification/"+(expectedQualified?"QUALIFIED":"FITTED_BUT_NOT_QUALIFIED")+" probabilityguard + immutablequality UPDATE/DELETE/REPLACE + actual65537-byte payload preflight beforeparse/prediction + fixture writer-fit rejection. Frozen identity metadata is a fixture; full numerical inference and Android lifecycle are separate scopes.\"}\n");
  }finally{db.shutdown();}
 }
}
