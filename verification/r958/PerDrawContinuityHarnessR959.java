package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Random;
import org.json.JSONObject;

/** Verifies actual per-draw dispatcher and persistent stage truth, not device performance. */
public final class PerDrawContinuityHarnessR959 {
    static int checks;
    static void check(boolean x,String name){if(!x)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static JSONObject period(String issue,long mask,int next)throws Exception {JSONObject p=new JSONObject();p.put("issue",issue);p.put("mask",mask);p.put("attempted",mask);p.put("next",next);p.put("started",System.currentTimeMillis());p.put("spent_ms",0);return p;}
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try {
            DbStore s=DevelopmentTrainingHarnessR959.init(db);Random r=new Random(15959);String issue="202609190001";
            db.beginTransaction();try{for(int i=0;i<120;i++){DevelopmentTrainingHarnessR959.draw(db,issue,r);if(i<119)issue=TargetIssueResolver.next(issue).targetIssue;}db.setTransactionSuccessful();}finally{db.endTransaction();}
            db.beginTransaction();try{DirectedResearchCreationR932.schema(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            long all=(1L<<ResearchLearningContinuityR912.CONTINUITY_LANES)-1;
            String key="r959_research_period_"+issue;
            AnalysisSingleFlightR36 flight=AnalysisSingleFlightR36.shared();flight.resetForTests();
            RealtimePressureGateR811.beginAnalysis();
            AnalysisSingleFlightR36.SubmitResult admitted=flight.submit(new AnalysisSingleFlightR36.Request(issue,TargetIssueResolver.next(issue).targetIssue,1,System.currentTimeMillis(),System.currentTimeMillis()+55000,"PRIORITY_TEST",V51ChecksumEngine.sha256("priority"),""));
            RealtimePressureGateR811.endAnalysis();
            check(admitted.startNow&&RealtimePressureGateR811.hasActiveCriticalWork(),"REQUEST_SCOPE_EXIT_RETAINS_ADMITTED_OWNER_PRESSURE");
            flight.releaseAndTakeNext(admitted.lease);
            check(!RealtimePressureGateR811.hasActiveCriticalWork(),"OWNER_RELEASE_BALANCES_REQUEST_AND_EXECUTION_REFERENCES");
            RealtimePressureGateR811.beginAnalysis();
            check(ResearchLearningContinuityR912.pulse(s,1000).status.contains("P0_PRESSURE"),"PREDICTION_PREEMPTS_RESEARCH");
            check(s.getState(key,"").isEmpty(),"PREEMPTION_DOES_NOT_MARK_PERIOD_DONE");RealtimePressureGateR811.endAnalysis();
            // Actual atomic-world stage; no unseen lanes may be reported completed after this test.
            s.setState(key,period(issue,all^(1L<<14),14).toString());
            check(ResearchLearningContinuityR912.needsContinuation(s),"PENDING_PERIOD_REQUESTS_CONTINUATION");
            String pending=s.getState(key,"");
            db.execSQL("CREATE TRIGGER fail_period_progress BEFORE INSERT ON "+DbHelper.TABLE_APP_STATE+" WHEN NEW.state_key='"+key+"' BEGIN SELECT RAISE(ABORT,'INJECTED_PERIOD_PROGRESS_FAILURE'); END");
            check(ResearchLearningContinuityR912.pulse(s,1000).status.contains("COMMIT_FAILED"),"PERIOD_PROGRESS_COMMIT_FAILURE_VISIBLE");
            check(s.getState(key,"").equals(pending),"FAILED_PROGRESS_TRANSACTION_CANNOT_MARK_DONE");
            check(s.getState("r962_research_next_lane","0").equals("0"),"FAILED_SCHEDULING_CLAIM_ROLLS_BACK_GLOBAL_CURSOR");
            db.execSQL("DROP TRIGGER fail_period_progress");
            ResearchLearningContinuityR912.Result first=ResearchLearningContinuityR912.pulse(s,1000);
            JSONObject saved=new JSONObject(s.getState(key,""));
            check(saved.getLong("mask")==all&&saved.getString("lane_14").startsWith("ATOMIC_REVIEWED|"),"ACTUAL_ATOMIC_STAGE_RESULT_DURABLE");
            check(!ResearchLearningContinuityR912.needsContinuation(new DbStore(db)),"RESTART_RETAINS_COMPLETED_PERIOD");
            // An actual failing production lookup remains pending and is retried after repair.
            s.setState(key,period(issue,all^(1L<<18),18).toString());
            db.execSQL("ALTER TABLE "+DirectedResearchCreationR932.ATTEMPT+" RENAME TO fixture_held_attempts");
            ResearchLearningContinuityR912.pulse(s,1000);JSONObject failed=new JSONObject(s.getState(key,""));
            check((failed.getLong("mask")&(1L<<18))==0&&failed.getString("lane_18").contains("ERROR"),"FAILED_TRAINING_STAGE_REMAINS_PENDING");
            db.execSQL("ALTER TABLE fixture_held_attempts RENAME TO "+DirectedResearchCreationR932.ATTEMPT);
            ResearchLearningContinuityR912.pulse(new DbStore(db),1000);
            check(new JSONObject(s.getState(key,"")).getLong("mask")==all,"REPAIRED_STAGE_RETRIES_WITHOUT_WAITING_ANOTHER_DRAW");
            check(s.getState("r962_research_next_lane","").equals("19"),"SUCCESSFUL_STAGE_LEAVES_DURABLE_NEXT_TURN");
            JSONObject expired=period(issue,0,0);expired.put("started",System.currentTimeMillis()-46000);s.setState(key,expired.toString());
            check(!ResearchLearningContinuityR912.needsContinuation(s)&&ResearchLearningContinuityR912.pulse(s,1000).status.startsWith("PERIOD_DEADLINE_INCOMPLETE"),"NEAR_NEXT_DRAW_STOP_IS_REPORTED_INCOMPLETE");
            String next=TargetIssueResolver.next(issue).targetIssue;DevelopmentTrainingHarnessR959.draw(db,next,r);
            check(ResearchLearningContinuityR912.needsContinuation(s),"NEW_DRAW_REOPENS_ALL_REQUIRED_STAGES");
            check(new JSONObject(s.getState(key,"")).getLong("mask")==0,"NEW_DRAW_DOES_NOT_REWRITE_MISSED_OLDER_PERIOD");
            db.execSQL("CREATE TRIGGER fail_after_lane19 BEFORE INSERT ON "+DbHelper.TABLE_APP_STATE+" WHEN NEW.state_key='r962_research_next_lane' AND NEW.state_value='21' BEGIN SELECT RAISE(ABORT,'STOP_AFTER_FAIR_FIRST_TURN'); END");
            ResearchLearningContinuityR912.pulse(new DbStore(db),1000);
            JSONObject fair=new JSONObject(s.getState("r959_research_period_"+next,""));
            check((fair.getLong("attempted")&(1L<<19))!=0,"NEXT_DRAW_SERVES_PERSISTED_TAIL_LANE_BEFORE_RESTARTING_AT_ZERO");
            check((fair.getLong("attempted")&1)==0,"OLD_PERIOD_COMPLETION_NOT_COPIED_TO_NEW_PERIOD");
            db.execSQL("DROP TRIGGER fail_after_lane19");
            // Run the full real dispatcher over this new period and retain all returned stage outcomes.
            JSONObject p=null;
            for(int i=0;i<90;i++){
                ResearchLearningContinuityR912.pulse(s,1000);
                String value=s.getState("r959_research_period_"+next,"");if(!value.isEmpty())p=new JSONObject(value);
                if(p!=null&&p.getLong("mask")==all)break;
            }
            check(p!=null&&p.getLong("attempted")==all,"EVERY_REQUIRED_LANE_ACTUALLY_ATTEMPTED_THIS_PERIOD");
            for(int i=0;i<ResearchLearningContinuityR912.CONTINUITY_LANES;i++)System.out.println("LANE "+i+" "+p.optString("lane_"+i,"NOT_ATTEMPTED"));
            check(p.getLong("mask")==all,"ALL_LANES_REVIEWED_AND_EACH_PARENT_NUMERICALLY_SERVED_HOST");
            String report=com.ethk3.novaonepro.r952.FullbloodDiagnosticsProviderR952.render(s);
            String actualTraining=p.optString("lane_18","");
            check(!actualTraining.isEmpty()&&!ResearchDynamicBootstrapR922.hasUnservedTraining(s),"NO_UNSERVED_PARENT_WHEN_PERIOD_MARKED_COMPLETE");
            // A generator need not create an algorithm in every period. The report must expose
            // its actual durable result, including IDLE when no registered parent needs work.
            check(report.contains("completedLanes=22/22")&&report.contains("PERIOD_LANE|18|complete=1|detail="+actualTraining)
                    &&report.contains("BOUNDED_PERIOD_STAGES_NOT_ALL_BACKLOG_OR_EFFECT"),"REPORT_EXPOSES_ACTUAL_LANES_WITH_COMPLETION_SCOPE");
            p.put("mask",1L<<30);s.setState("r959_research_period_"+next,p.toString());
            report=com.ethk3.novaonepro.r952.FullbloodDiagnosticsProviderR952.render(s);
            check(report.contains("periodEvidence=UNKNOWN:IllegalArgumentException")&&!report.contains("completedLanes=22/22"),"CORRUPT_PERIOD_CANNOT_REPORT_VERIFIED_COMPLETION");
            System.out.println("R959_PER_DRAW_CONTINUITY_PASS|checks="+checks+"|scope=SYNTHETIC_PRODUCTION_DISPATCHER_NOT_PHONE");
        }finally{db.shutdown();}
    }
}
