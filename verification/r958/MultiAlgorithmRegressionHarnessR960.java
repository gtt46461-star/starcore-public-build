package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Real owner calls, synthetic outcomes. Never counts these runs as phone or live evidence. */
public final class MultiAlgorithmRegressionHarnessR960 {
    static int checks;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try {
            DbStore s=DevelopmentTrainingHarnessR959.init(db);Random random=new Random(960);String base="202609190001";
            CertificateBundleServiceR929.Evaluation below=CertificateBundleServiceR929.evaluate(new CertificateBundleServiceR929.EvidenceInput(true,true,true,120,120,0,.3399,.31,.85,.22,.03,.01,.20,true,true,V51ChecksumEngine.sha256("ENTRY_BELOW")));
            CertificateBundleServiceR929.Evaluation at=CertificateBundleServiceR929.evaluate(new CertificateBundleServiceR929.EvidenceInput(true,true,true,120,120,0,.34,.31,.85,.22,.03,.01,.20,true,true,V51ChecksumEngine.sha256("ENTRY_AT")));
            check(below.level==CertificateBundleServiceR929.Level.L3_EVIDENCE_READY,"BELOW_34_PERCENT_CANNOT_BECOME_CHALLENGER");
            check(at.level==CertificateBundleServiceR929.Level.L4_CHALLENGER&&at.weightCap==0,"34_PERCENT_BOUNDARY_MAY_ENTER_ZERO_WEIGHT_CHALLENGER_ONLY");
            check(ResearchRuntimeSqlGateR911.verifyAndPublish(s).passed,"SQL_GATE_INITIAL_REAL_PREPARATION");
            s.setState("r911_research_sql_gate_at","CACHE_SENTINEL");
            check(ResearchRuntimeSqlGateR911.verifyAndPublish(s).passed&&"CACHE_SENTINEL".equals(s.getState("r911_research_sql_gate_at","")),"UNCHANGED_SCHEMA_AVOIDS_REPEATED_PREPARE_AND_STATE_WRITES");
            db.execSQL("ALTER TABLE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" RENAME TO fixture_hidden_genome");
            check(!ResearchRuntimeSqlGateR911.verifyAndPublish(s).passed,"DDL_INVALIDATES_PREVIOUS_SQL_PASS");
            db.execSQL("ALTER TABLE fixture_hidden_genome RENAME TO "+DbHelper.TABLE_R908_ALGORITHM_GENOME);
            check(ResearchRuntimeSqlGateR911.verifyAndPublish(s).passed,"REPAIRED_SCHEMA_IS_REVERIFIED");
            db.beginTransaction();try {for(int i=0;i<320;i++){DevelopmentTrainingHarnessR959.draw(db,base,random);if(i<319)base=TargetIssueResolver.next(base).targetIssue;}db.setTransactionSuccessful();}finally{db.endTransaction();}
            for(int i=0;i<9;i++){
                check(DirectedResearchCreationR932.maintain(s,"algorithm").startsWith("CREATED_N0|"),"REAL_CREATION:"+i);
                if(i<8){base=TargetIssueResolver.next(base).targetIssue;DevelopmentTrainingHarnessR959.draw(db,base,random);}
            }
            check(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==9,"NINE_DISTINCT_EXECUTABLE_ALGORITHMS");
            for(int i=0;i<27;i++){
                s=new DbStore(db);String result=ResearchDynamicBootstrapR922.pulse(s,1000).status;
                if(result.startsWith("ERROR"))throw new AssertionError(result);
            }
            check(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key GLOB 'r959_development_job_*'")==9,"ALL_NINE_GET_INDEPENDENT_DEVELOPMENT_JOBS");
            int progressed=0;
            try(Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key GLOB 'r959_development_job_*'",null)){
                while(c.moveToNext()){JSONObject j=new JSONObject(c.getString(0));if(j.getInt("n")>0&&j.getInt("n")<240)progressed++;}
            }
            check(progressed==9,"ALL_NINE_ACTUALLY_TRAIN_BEFORE_ANY_ONE_FINISHES");
            check(!ResearchDynamicBootstrapR922.hasUnservedTraining(s),"PERIOD_SERVE_REQUIRES_REAL_NUMERIC_CHECKPOINTS");
            // A parent that cannot load its own data may fail; other parents must continue.
            String failing="";try(Cursor c=db.rawQuery("SELECT genome_id FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" ORDER BY genome_id LIMIT 1",null)){c.moveToFirst();failing=c.getString(0);}
            String inputKey="r959_development_input_"+failing,original=s.getState(inputKey,"");s.setState(inputKey,"corrupt");
            long before=n(db,"SELECT SUM(CAST(json_extract(state_value,'$.n') AS INTEGER)) FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key GLOB 'r959_development_job_*'");
            boolean failed=false;for(int i=0;i<9;i++){String result=ResearchDynamicBootstrapR922.pulse(s,1000).status;if(result.contains("R959_DEV_DATASET_HASH"))failed=true;}
            check(failed&&n(db,"SELECT SUM(CAST(json_extract(state_value,'$.n') AS INTEGER)) FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key GLOB 'r959_development_job_*'")>before,"ONE_BAD_DATASET_CANNOT_STARVE_OTHER_TRAINERS");s.setState(inputKey,original);
            // Keep an old recovery cursor. Current evolution must execute for the real latest draw.
            s.setState("r947_evolution_issue","202609190010");s.setState("r947_evolution_stage","1");
            db.execSQL("CREATE TRIGGER fixture_evolution_commit_failure BEFORE INSERT ON "+DbHelper.TABLE_APP_STATE+" WHEN NEW.state_key='r960_evolution_stage_"+base+"' BEGIN SELECT RAISE(ABORT,'INJECTED_EVOLUTION_COMMIT_FAILURE'); END");
            boolean evolutionFailed=false;try{ResearchEvolutionHeartbeatR947.pulseCurrent(s,1000);}catch(IllegalStateException expected){evolutionFailed=expected.getMessage().contains("COMMIT_FAILED");}
            check(evolutionFailed&&s.getState("r960_evolution_stage_"+base,"0").equals("0"),"FAILED_EVOLUTION_CHECKPOINT_CANNOT_REPORT_STAGE_COMPLETE");
            db.execSQL("DROP TRIGGER fixture_evolution_commit_failure");
            for(int retry=0;retry<12;retry++){ResearchEvolutionHeartbeatR947.Result r=ResearchEvolutionHeartbeatR947.pulseCurrent(s,1000);if(r.stage>=4)break;}
            check(s.getState("r960_evolution_stage_"+base,"0").equals("4"),"CURRENT_EVOLUTION_FINISHES_WITH_OLD_BACKLOG_CURSOR");
            check(s.getState("r947_evolution_issue","").equals("202609190010"),"OLD_REVIEW_DEBT_NOT_FABRICATED_AS_COMPLETE");
            // Nine algorithms traverse real proxy checks, nursery promotion and full predictions.
            for(int period=0;period<8;period++){
                String target=TargetIssueResolver.next(base).targetIssue;
                DeferredEvidenceSnapshotR36.Snapshot surface=ReportedCaptureHarnessR945.snapshot(base,target);
                byte[] blob=DeferredEvidenceSnapshotR36.encode(surface);
                db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue,job_type,status,created_at,updated_at,record_hash,snapshot_blob,snapshot_hash) VALUES(?,?,?,0,0,?,?,?)",new Object[]{target,"R960_SYNTHETIC","PENDING",surface.recordHash,blob,DeferredEvidenceSnapshotR36.sha256(blob)});
                for(int retry=0;retry<45;retry++){
                    try{ResearchPredrawCaptureR923.captureTarget(s,target,1200);}catch(ResearchWorkBudgetR927.Yield yielded){}
                    if(ResearchCaptureQueueR946.complete(s,target))break;
                }
                check(ResearchCaptureQueueR946.complete(s,target),"ALL_ADMITTED_ALGORITHMS_CAPTURE:"+period);
                DevelopmentTrainingHarnessR959.draw(db,target,random);AlgorithmInventionLabR908.settleDraw(s,s.getDrawByIssue(target));
                AlgorithmInventionLabR908.settleDraw(s,s.getDrawByIssue(target));
                // One production lifecycle pulse, rather than directly promoting individual rows.
                if(period==2)db.execSQL("CREATE TRIGGER fixture_one_lifecycle_failure BEFORE UPDATE ON "+ResearchLifecycleR932.TABLE+" WHEN OLD.candidate_key='algorithm:"+failing+"' BEGIN SELECT RAISE(ABORT,'INJECTED_ONE_CANDIDATE_REVIEW_FAILURE'); END");
                int reviewed=ResearchLifecycleR932.maintain(s);
                if(period==2){
                    check(reviewed>=8,"ONE_BAD_CANDIDATE_CANNOT_ROLL_BACK_EIGHT_LIFECYCLE_COMMITS");
                    check(n(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key LIKE 'algorithm:%' AND state IN ('IDEA','NURSERY')")==1,"FAILED_CANDIDATE_ALONE_REMAINS_RETRYABLE");
                    db.execSQL("DROP TRIGGER fixture_one_lifecycle_failure");
                    reviewed+=ResearchLifecycleR932.maintain(s);
                    check(reviewed>=9,"REPAIRED_CANDIDATE_RETRIES_WITHOUT_WAITING_ANOTHER_DRAW");
                    check(n(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key LIKE 'algorithm:%' AND state IN ('IDEA','NURSERY')")==0,"NO_ALGORITHM_STARVED_IN_NURSERY_AFTER_THIRD_PROXY");
                }
                base=target;
            }
            check(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE state_updates>0 AND future_n>0")==9,"ALL_NINE_FUTURE_AND_ONLINE_UPDATES_NONZERO");
            check(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE state_updates<>future_n")==0,"DUPLICATE_SETTLEMENT_DOES_NOT_DOUBLE_TRAIN");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"SHORT_SYNTHETIC_RUN_DOES_NOT_INVENT_CERTIFICATES");
            // More than one lifecycle page must remain pending, then converge without
            // relabeling a partial 24-candidate pulse as the complete population.
            db.beginTransaction();try{
                for(int i=0;i<55;i++)ResearchLifecycleR932.enrollR952(db,"r952:R962-PAGE-"+i,V51ChecksumEngine.sha256("PAGE-"+i),base);
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            check(ResearchLifecycleR932.hasPendingPeriodReview(s),"NEW_COHORT_REQUIRES_ACTUAL_PERIOD_REVIEWS");
            ResearchLifecycleR932.maintain(s);
            check(ResearchLifecycleR932.hasPendingPeriodReview(s),"ONE_BOUNDED_PAGE_CANNOT_COMPLETE_55_CANDIDATES");
            for(int retry=0;retry<8&&ResearchLifecycleR932.hasPendingPeriodReview(s);retry++)ResearchLifecycleR932.maintain(new DbStore(db));
            check(!ResearchLifecycleR932.hasPendingPeriodReview(s),"MULTIPAGE_LIFECYCLE_RESUMES_ACROSS_REOPEN_AND_CONVERGES");
            check(!ResearchEvolutionHeartbeatR947.terminalReview("LIFECYCLE_PENDING|assessed=24"),"PARTIAL_LIFECYCLE_CANNOT_COMPLETE_EVOLUTION_STAGE");
            long pendingAtoms=n(db,"SELECT COUNT(*) FROM "+AtomicToolWorldR943.CONTEXT+" WHERE hit=-1");
            check(pendingAtoms>0,"ACTUAL_CAPTURE_CREATED_FROZEN_ATOM_LABELS");
            db.execSQL("CREATE TRIGGER fixture_atom_chunk_failure BEFORE INSERT ON "+AtomicToolWorldR943.QUALITY+" WHEN NEW.atom_index>=100 BEGIN SELECT RAISE(ABORT,'INJECTED_ATOM_CHUNK_FAILURE'); END");
            long atomRows=n(db,"SELECT COUNT(*) FROM "+AtomicToolWorldR943.QUALITY);
            check(AtomicToolWorldR943.maintain(s).contains("FAILED")&&n(db,"SELECT COUNT(*) FROM "+AtomicToolWorldR943.QUALITY)==atomRows&&n(db,"SELECT COUNT(*) FROM "+AtomicToolWorldR943.CONTEXT+" WHERE hit=-1")==pendingAtoms,"ATOMIC_CHUNK_FAILURE_ROLLS_BACK_ENTIRE_LABEL");
            db.execSQL("DROP TRIGGER fixture_atom_chunk_failure");
            for(int i=0;i<pendingAtoms;i++)check(AtomicToolWorldR943.maintain(s).startsWith("LEARNED|"),"ATOMIC_BACKLOG_REAL_NUMERIC_UPDATE:"+i);
            check(n(db,"SELECT MIN(n) FROM "+AtomicToolWorldR943.QUALITY)==pendingAtoms&&n(db,"SELECT MAX(n) FROM "+AtomicToolWorldR943.QUALITY)==pendingAtoms&&n(db,"SELECT COUNT(*) FROM "+AtomicToolWorldR943.QUALITY)==165,"ALL_165_ATOMS_UPDATE_ON_EVERY_LABEL");
            check(n(db,"SELECT updates FROM "+AtomicToolWorldR943.MODEL)==pendingAtoms&&AtomicToolWorldR943.maintain(s).startsWith("WAIT_"),"ATOMIC_NETWORK_CATCHUP_AND_NO_DUPLICATE_TRAINING");
            long now=TimeSourceR811.monoNowMs();AnalysisSingleFlightR36 flight=AnalysisSingleFlightR36.shared();flight.resetForTests();
            AnalysisSingleFlightR36.SubmitResult submit=flight.submit(new AnalysisSingleFlightR36.Request(base,TargetIssueResolver.next(base).targetIssue,1,System.currentTimeMillis(),System.currentTimeMillis()+5000,"CLOCK_DOMAIN",V51ChecksumEngine.sha256("clock"),""));
            RuntimeResourceAuditR927.Scope resource=new RuntimeResourceAuditR927.Scope(submit.lease,false);
            check(Math.abs(submit.lease.acquiredMonoAt-now)<1000,"OWNER_DISPATCH_USES_ELAPSED_REALTIME_CLOCK");
            check(resource.deadline()-TimeSourceR811.monoNowMs()>3000&&resource.deadline()-TimeSourceR811.monoNowMs()<5500,"REQUEST_DEADLINE_EXCLUDES_UPTIME_OFFSET");flight.releaseAndTakeNext(submit.lease);
            System.out.println("R960_MULTI_ALGORITHM_PASS|checks="+checks+"|scope=ACTUAL_OWNERS_SYNTHETIC_NOT_PHONE_NOT_EFFECT");
        }finally{db.shutdown();}
    }
}
