package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Locale;

/** Executes the production resource journal, budgets, cache and slice policy with real SQLite. */
public final class R928RuntimeRegressionHarness {
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println(name+"=PASS");}
    static String scalar(SQLiteDatabase db,String sql){Cursor c=db.rawQuery(sql,null);try{return c.moveToFirst()?c.getString(0):"";}finally{c.close();}}
    static AnalysisSingleFlightR36.Lease lease(int n){
        long now=System.currentTimeMillis();String base=String.format(Locale.US,"20260910%04d",n),target=String.format(Locale.US,"20260910%04d",n+1);
        AnalysisSingleFlightR36.Request r=new AnalysisSingleFlightR36.Request(base,target,n+1,now,now+55000L,"TEST",V51ChecksumEngine.sha256("request"+n),"");
        RuntimeObservabilityR925.registerRequest(r,TimeSourceR811.monoNowMs());return AnalysisSingleFlightR36.Lease.acquire(r);
    }
    static void checkProgress(boolean ok){if(!ok)throw new AssertionError("CURSOR_FAILED_TO_ADVANCE");}
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);DbStore store=new DbStore(db);
        try{
            RuntimeResourceAuditR927.ensure(db);RuntimeResourceAuditR927.ensure(db);
            android.os.Debug.count=7;android.os.Debug.time=10;
            AnalysisSingleFlightR36.Lease first=lease(1);
            RuntimeResourceAuditR927.Scope scope=RuntimeResourceAuditR927.begin(store,first);
            RuntimeResourceAuditR927.lockWait(19);RuntimeResourceAuditR927.lockWait(23);RuntimeResourceAuditR927.dbBody(77,true);
            RuntimeResourceAuditR927.stageBegin("FEATURES_SUB");RuntimeResourceAuditR927.stageEnd("FEATURES_SUB",30,true);
            android.os.Debug.count=10;android.os.Debug.time=24;
            first.token.markCanonicalCommitted();RuntimeResourceAuditR927.finish(store,scope,true,"DONE");
            check("42".equals(scalar(db,"SELECT lock_wait_ms FROM "+RuntimeResourceAuditR927.TABLE)),"LOCK_WAIT_ACCUMULATED_PER_EXECUTION");
            check("23".equals(scalar(db,"SELECT lock_wait_max_ms FROM "+RuntimeResourceAuditR927.TABLE)),"LOCK_WAIT_MAX_SEPARATE_FROM_TOTAL");
            check("3".equals(scalar(db,"SELECT gc_count FROM "+RuntimeResourceAuditR927.TABLE))&&"14".equals(scalar(db,"SELECT gc_time_ms FROM "+RuntimeResourceAuditR927.TABLE)),"GC_PROCESS_COUNTER_DELTAS_PERSISTED");
            check(Long.parseLong(scalar(db,"SELECT heap_peak_bytes FROM "+RuntimeResourceAuditR927.TABLE))>0,"HEAP_SAMPLED_PEAK_PERSISTED");
            check("1".equals(scalar(db,"SELECT db_slice_overruns FROM "+RuntimeResourceAuditR927.TABLE)),"SLOW_DB_SLICE_NOT_DISCARDED");
            check(RuntimeResourceAuditR927.report(store).contains("FEATURES_SUB:30/900"),"RAW_STAGE_AND_FROZEN_BUDGET_VISIBLE");
            check(RuntimeResourceAuditR927.delta(-1,9)==-1&&RuntimeResourceAuditR927.delta(9,2)==-1,"UNAVAILABLE_AND_RESET_GC_ARE_NOT_ZERO");
            AnalysisSingleFlightR36.Lease second=lease(2);scope=RuntimeResourceAuditR927.begin(store,second);
            RuntimeResourceAuditR927.stageBegin("FEATURES_SUB");scope.active.get("FEATURES_SUB")[0]=TimeSourceR811.monoNowMs()-901;
            boolean failed=false;try{second.token.checkpoint("FEATURE_LOOP");}catch(RuntimeResourceAuditR927.BudgetExceeded expected){failed=true;}
            check(!failed,"SLOW_STAGE_RETAINS_COMPUTED_PREDRAW_ACTION");
            RuntimeResourceAuditR927.stageEnd("FEATURES_SUB",901,true);
            check(scope.stages.toString().contains("OVER_BUDGET"),"SLOW_STAGE_REMAINS_VISIBLE_AS_SLO_FAILURE");
            RuntimeResourceAuditR927.beginRecovery();RuntimeResourceAuditR927.checkpoint("FAILURE_JOURNAL_WRITE");
            RuntimeResourceAuditR927.finish(store,scope,false,"B05_TIMEOUT");
            check("1".equals(scalar(db,"SELECT COUNT(*) FROM "+RuntimeResourceAuditR927.TABLE+" WHERE status='NOT_COMMITTED' AND reason LIKE '%B05_TIMEOUT%'")),"TIMEOUT_KEEPS_DURABLE_FAILURE_AND_RESOURCES");
            AnalysisSingleFlightR36.Lease third=lease(3);scope=RuntimeResourceAuditR927.begin(store,third);
            RuntimeResourceAuditR927.stageBegin("DB_CANONICAL_WAIT");third.token.markCanonicalCommitted();
            RuntimeResourceAuditR927.stageEnd("DB_CANONICAL_WAIT",999,true);
            RuntimeResourceAuditR927.finish(store,scope,true,"SLOW_COMMIT");
            check("2".equals(scalar(db,"SELECT COUNT(*) FROM "+RuntimeResourceAuditR927.TABLE+" WHERE status='CANONICAL_COMMITTED'")),"DURABLE_COMMIT_NOT_REVERSED_BY_LATE_TELEMETRY");
            db.execSQL("INSERT INTO "+RuntimeResourceAuditR927.TABLE+"(execution_id,attempt_id,target_issue,process_id,release_code,started_at,status,budget_hash) VALUES('dead','dead','202609100999','old-process',1,1,'STARTED','budget')");
            java.lang.reflect.Field reconciled=RuntimeResourceAuditR927.class.getDeclaredField("priorProcessReconciled");reconciled.setAccessible(true);reconciled.setBoolean(null,false);
            AnalysisSingleFlightR36.Lease fourth=lease(4);scope=RuntimeResourceAuditR927.begin(store,fourth);RuntimeResourceAuditR927.finish(store,scope,false,"TEST");
            check("PROCESS_INTERRUPTED".equals(scalar(db,"SELECT status FROM "+RuntimeResourceAuditR927.TABLE+" WHERE execution_id='dead'")),"PROCESS_DEATH_MARKED_UNKNOWN_WITHOUT_FAKE_ZERO");
            AnalysisSingleFlightR36.Lease aged=lease(8);
            RuntimeObservabilityR925.registerRequest(aged.request,TimeSourceR811.monoNowMs()-40000L);
            scope=RuntimeResourceAuditR927.begin(store,aged);
            RuntimeResourceAuditR927.checkpoint("DB_LOCK_ACQUIRED");
            check(RuntimeResourceAuditR927.remainingMs()>10000L,"OLD_SLO_ORIGIN_DOES_NOT_POISON_RETRY");
            RuntimeResourceAuditR927.finish(store,scope,false,"RETRY_TEST");
            long now=System.currentTimeMillis();
            AnalysisSingleFlightR36.Request expired=new AnalysisSingleFlightR36.Request("202609100010","202609100011",12,now-60000,now-1,"TEST",V51ChecksumEngine.sha256("expired"),"");
            scope=RuntimeResourceAuditR927.begin(store,AnalysisSingleFlightR36.Lease.acquire(expired));
            failed=false;try{RuntimeResourceAuditR927.checkpoint("PRE_COMMIT");}catch(RuntimeResourceAuditR927.BudgetExceeded expected){failed=true;}
            check(failed,"ACTUAL_DRAW_DEADLINE_STILL_REJECTS_COMMIT");
            RuntimeResourceAuditR927.finish(store,scope,false,"EXPIRED_TEST");
            check(new PredictionGenerationCoverageR923.Window(30,30,0,0,30,0,0,0).canonical().contains("realPerDraw=0.00%|minimum15=NOT_MET"),"ZERO_PREDICTIONS_IS_ZERO_FREQUENCY_NOT_NO_SAMPLE");
            check(new PredictionGenerationCoverageR923.Window(30,30,30,30,30,30,5,25).canonical().contains("nonREALShadowCoverage=100.00%"),"NON_REAL_DRAW_DENOMINATOR_REQUIRES_ALL_SHADOW");
            V51Models.PositionAction[] exportActions=new V51Models.PositionAction[1200];int ao=0;
            for(int pos=1;pos<=10;pos++)for(int aa=1;aa<=8;aa++)for(int bb=aa+1;bb<=9;bb++)for(int cc=bb+1;cc<=10;cc++)
                exportActions[ao++]=new V51Models.PositionAction(pos,aa,bb,cc,0,.3,0,0,0,0,0,new double[96],"",ao,"");
            int[] draw={1,2,3,4,5,6,7,8,9,10};
            TrainingSnapshotExportR925.validateActions(exportActions,draw);
            check(true,"EXPORT_ACCEPTS_1200_ACTIONS_AND_LEGITIMATE_ZERO_FEATURES");
            V51Models.PositionAction saved=exportActions[0];exportActions[0]=exportActions[1];
            failed=false;try{TrainingSnapshotExportR925.validateActions(exportActions,draw);}catch(java.io.IOException expected){failed=true;}
            check(failed,"EXPORT_REJECTS_DUPLICATE_ORDINAL");exportActions[0]=saved;
            exportActions[0].metaFeatures[0]=Double.NaN;
            failed=false;try{TrainingSnapshotExportR925.validateActions(exportActions,draw);}catch(java.io.IOException expected){failed=true;}
            check(failed,"EXPORT_REJECTS_NONFINITE_INPUT");
            V51Models.DrawFrame[] history=new V51Models.DrawFrame[150];
            for(int i=0;i<history.length;i++)history[i]=new V51Models.DrawFrame(String.format(Locale.US,"20260910%04d",i),new int[]{1,2,3,4,5,6,7,8,9,10},i+1);
            RollingWindowCache cache=RollingWindowCache.shared();cache.clear();
            RollingWindowCache.SnapshotSet before=cache.getOrBuild(history);check(before==cache.getOrBuild(history),"IDENTICAL_COMPLETE_HISTORY_REUSES_CACHE");
            history[0]=new V51Models.DrawFrame(history[0].issue,new int[]{2,1,3,4,5,6,7,8,9,10},1);
            RollingWindowCache.SnapshotSet after=cache.getOrBuild(history);
            check(before!=after&&!before.checksum.equals(after.checksum),"CHANGE_OUTSIDE_LAST_96_INVALIDATES_LONG_WINDOW_CACHE");
            ResearchWorkBudgetR927.Scope research=ResearchWorkBudgetR927.enter(2000);
            try{RealtimePressureGateR811.beginAnalysis();failed=false;try{ResearchWorkBudgetR927.checkpoint();}catch(ResearchWorkBudgetR927.Yield expected){failed=true;}check(failed,"RESEARCH_YIELDS_TO_EXISTING_PRODUCTION_OWNER");}
            finally{RealtimePressureGateR811.endAnalysis();ResearchWorkBudgetR927.leave(research);}
            research=ResearchWorkBudgetR927.enter(1);Thread.sleep(3);failed=false;try{ResearchWorkBudgetR927.checkpoint();}catch(ResearchWorkBudgetR927.Yield expected){failed=true;}finally{ResearchWorkBudgetR927.leave(research);}check(failed,"RESEARCH_EXPIRED_MONOTONIC_BUDGET_REJECTS_WORK");
            check(!DeferredEvidenceWriterR36.shouldYieldSlice(0,100,true)&&!DeferredEvidenceWriterR36.shouldYieldSlice(1,49,false)&&DeferredEvidenceWriterR36.shouldYieldSlice(1,50,false)&&DeferredEvidenceWriterR36.shouldYieldSlice(1,1,true),"FULL_SLICE_50MS_OR_REALTIME_BOUNDARY");
            int cursor=0,slices=0;while(cursor<1200){int start=cursor;while(cursor<1200){cursor++;if(DeferredEvidenceWriterR36.shouldYieldSlice(cursor-start,(cursor-start)*11L,false))break;}slices++;}
            check(cursor==1200&&slices==240,"ADAPTIVE_CURSOR_REACHES_1200_WITHOUT_ORDINAL_GAPS");
            research=ResearchWorkBudgetR927.enter(500);
            try {
                failed=false;try{BudgetedQueryR927.query(db,"BLOCK_UNTIL_CANCEL",null,10);}catch(IllegalStateException expected){failed=expected.getMessage().contains("SQL_NATIVE_CANCEL_SIGNAL");}
                check(failed,"BLOCKED_SQL_RECEIVES_NATIVE_CANCELLATION_SIGNAL");
                Cursor quick=BudgetedQueryR927.query(db,"SELECT 1",null,10);quick.close();Thread.sleep(25);
                check(!db.lastSignal.isCanceled(),"CLOSED_CURSOR_CANCELS_PENDING_TIMEOUT");
            }finally{ResearchWorkBudgetR927.leave(research);}
            ApiTransportGovernanceEngine.EndpointState endpoint=new ApiTransportGovernanceEngine.EndpointState(BinanceGameContract.primary());
            endpoint.failure(1000);endpoint.failure(1000);long reopen=endpoint.snapshot().openUntilMs;
            check(reopen>=6000&&reopen<=7250&&!endpoint.allow(1001),"NETWORK_BACKOFF_EXPONENTIAL_JITTER_BOUNDED");
            check(endpoint.ready(reopen)&&endpoint.allow(reopen)&&!endpoint.allow(reopen),"CIRCUIT_HALF_OPEN_ALLOWS_ONLY_ONE_PROBE");
            endpoint.success(3);check(endpoint.allow(reopen),"CIRCUIT_SUCCESS_REOPENS_NORMAL_REQUESTS");
            ApiTransportGovernanceEngine.TokenBucket bucket=new ApiTransportGovernanceEngine.TokenBucket(2);long tokenNow=System.nanoTime()/1000000L;
            check(bucket.tryAcquire(tokenNow)&&bucket.tryAcquire(tokenNow)&&!bucket.tryAcquire(tokenNow)&&!bucket.tryAcquire(tokenNow-3600000L),"PROVIDER_LIMIT_NOT_BYPASSED_BY_BACKWARD_CLOCK");
            RuntimeEventLedgerR926.ensureSchema(db);
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,dirty INTEGER DEFAULT 0)");
            db.execSQL("INSERT INTO draws VALUES('202609100050',100,0)");
            db.execSQL("CREATE TABLE v57_fetch_ledger_r925(request_id TEXT PRIMARY KEY,http_code INTEGER,received_at INTEGER,payload_hash TEXT,source_line TEXT,provider TEXT,latest_issue TEXT,first_available_at INTEGER DEFAULT 0,draw_at INTEGER,clock_epoch TEXT,label_version TEXT,status TEXT,error_code TEXT)");
            String payload=V51ChecksumEngine.sha256("payload");
            db.execSQL("INSERT INTO v57_fetch_ledger_r925(request_id,http_code,received_at,payload_hash,status) VALUES('good',200,200,?,'HTTP_OK_BODY_RECEIVED')",new Object[]{payload});
            FetchLedgerR925.payload(store,"good","PRIMARY","202609100050",true,"");
            check("BUSINESS_ACCEPTED".equals(scalar(db,"SELECT status FROM v57_fetch_ledger_r925 WHERE request_id='good'"))&&"1".equals(scalar(db,"SELECT COUNT(*) FROM v57_runtime_event_r926 WHERE source='FetchLedgerR925'")),"PRODUCTION_PAYLOAD_AND_SOURCE_EVENT_COMMIT_TOGETHER");
            db.execSQL("INSERT INTO v57_fetch_ledger_r925(request_id,http_code,received_at,payload_hash,status) VALUES('fault',200,250,?,'HTTP_OK_BODY_RECEIVED')",new Object[]{payload});
            db.execSQL("CREATE TRIGGER fault_event BEFORE INSERT ON v57_runtime_event_r926 BEGIN SELECT RAISE(ABORT,'INJECT_EVENT_FAILURE'); END");
            FetchLedgerR925.payload(store,"fault","PRIMARY","202609100050",true,"");
            check("HTTP_OK_BODY_RECEIVED".equals(scalar(db,"SELECT status FROM v57_fetch_ledger_r925 WHERE request_id='fault'")),"EVENT_FAILURE_ROLLS_BACK_PAYLOAD_ACCEPTANCE");
            db.execSQL("DROP TRIGGER fault_event");FetchLedgerR925.payload(store,"fault","PRIMARY","202609100050",true,"duplicate");
            check("200".equals(scalar(db,"SELECT first_available_at FROM v57_fetch_ledger_r925 WHERE request_id='fault'")),"DUPLICATE_RECOVERS_ORIGINAL_PAYLOAD_AVAILABILITY");
            java.util.ArrayList<DeferredEvidenceSnapshotR36.NumberRow> numbers=new java.util.ArrayList<DeferredEvidenceSnapshotR36.NumberRow>();
            for(int pos=1;pos<=10;pos++)for(int n=1;n<=10;n++)numbers.add(new DeferredEvidenceSnapshotR36.NumberRow(pos,n,.1,.1,n,"state","domain",V51ChecksumEngine.sha256("expert")));
            java.util.ArrayList<DeferredEvidenceSnapshotR36.ActionRow> actions=new java.util.ArrayList<DeferredEvidenceSnapshotR36.ActionRow>();
            for(int pos=1;pos<=10;pos++)for(int n1=1;n1<=8;n1++)for(int n2=n1+1;n2<=9;n2++)for(int n3=n2+1;n3<=10;n3++){
                String id=pos+":"+n1+":"+n2+":"+n3;actions.add(new DeferredEvidenceSnapshotR36.ActionRow(pos,n1,n2,n3,0,.3,.4,.3,actions.size()+1,id,"",V51ChecksumEngine.sha256(id),V51ChecksumEngine.sha256("record"+id)));
            }
            DeferredEvidenceSnapshotR36.Snapshot snap=new DeferredEvidenceSnapshotR36.Snapshot("202609100080","202609100079","history","bundle","contract","action","record","", "",0,0,0,true,false,"",numbers,actions,new java.util.ArrayList<String>(),new java.util.ArrayList<RuntimeTraceR36.Span>());
            V51Repository.ensure(db);
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue TEXT PRIMARY KEY,status TEXT,lease_owner TEXT,lease_until INTEGER,attempts INTEGER,next_attempt_at INTEGER,updated_at INTEGER,core_materialized INTEGER,next_action_ordinal INTEGER,core_next_ordinal INTEGER,cursor_version INTEGER,number_rows INTEGER,action_rows INTEGER,last_stage TEXT)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue,status,lease_owner,next_action_ordinal) VALUES('202609100080','RUNNING','owner',0)");
            java.lang.reflect.Method core=DeferredEvidenceWriterR36.class.getDeclaredMethod("materializeCore",DbStore.class,DeferredEvidenceSnapshotR36.Snapshot.class,String.class,String.class,int.class);core.setAccessible(true);
            RealtimePressureGateR811.beginAnalysis();int coreCursor;
            try{coreCursor=(Integer)core.invoke(null,store,snap,"hash","owner",0);}finally{RealtimePressureGateR811.endAnalysis();}
            check(coreCursor==1&&"1".equals(scalar(db,"SELECT core_next_ordinal FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB)),"PRODUCTION_CORE_SLICE_YIELDS_AND_COMMITS_CURSOR");
            while(coreCursor<121){db.execSQL("UPDATE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" SET status='RUNNING',lease_owner='owner'");int next=(Integer)core.invoke(null,store,snap,"hash","owner",coreCursor);checkProgress(next>coreCursor);coreCursor=next;}
            check("100".equals(scalar(db,"SELECT COUNT(*) FROM "+V51Repository.NUMBER))&&"1".equals(scalar(db,"SELECT core_materialized FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB)),"PRODUCTION_CORE_CURSOR_REACHES_ALL_121_UNITS");
            db.execSQL("UPDATE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" SET status='RUNNING',lease_owner='owner'");
            java.lang.reflect.Method materialize=DeferredEvidenceWriterR36.class.getDeclaredMethod("materializeActionChunk",DbStore.class,DeferredEvidenceSnapshotR36.Snapshot.class,String.class,String.class,int.class,int.class,int.class);materialize.setAccessible(true);
            RealtimePressureGateR811.beginAnalysis();int advanced;
            try{advanced=(Integer)materialize.invoke(null,store,snap,"hash","owner",0,120,0);}finally{RealtimePressureGateR811.endAnalysis();}
            check(advanced==1&&"1".equals(scalar(db,"SELECT next_action_ordinal FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB)),"PRODUCTION_SLICE_YIELDS_AFTER_ONE_ROW_AND_COMMITS_CURSOR");
            while(advanced<1200){db.execSQL("UPDATE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" SET status='RUNNING',lease_owner='owner'");int next=(Integer)materialize.invoke(null,store,snap,"hash","owner",advanced,Math.min(1200,advanced+120),advanced/120);checkProgress(next>advanced);advanced=next;}
            check("1200".equals(scalar(db,"SELECT COUNT(*) FROM "+V51Repository.ACTION_SPACE))&&"1200".equals(scalar(db,"SELECT next_action_ordinal FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB)),"PRODUCTION_SQL_ALL_1200_ROWS_AND_CURSOR_MATCH");
            db.execSQL("DELETE FROM "+V51Repository.ACTION_SPACE+" WHERE position=1 AND n1=1 AND n2=2 AND n3=3");
            failed=false;try{materialize.invoke(null,store,snap,"hash","wrong-owner",0,1,0);}catch(java.lang.reflect.InvocationTargetException expected){failed=true;}
            check(failed&&"1199".equals(scalar(db,"SELECT COUNT(*) FROM "+V51Repository.ACTION_SPACE)),"LOST_LEASE_ROLLS_BACK_SLICE_ROW_AND_CURSOR");
            System.out.println("R928_RUNTIME_REGRESSION_PASS");
        }finally{db.shutdown();}
    }
}
