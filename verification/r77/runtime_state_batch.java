package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Product DbStore/coordinator with real SQLite; only Android insert error transport is adapted. */
public final class runtime_state_batch {
    static int checks;
    static void check(boolean value,String name) {
        if(!value) throw new AssertionError(name);
        checks++; System.out.println("PASS|"+name);
    }
    static final String[][] TEXT_FIELDS={
        {"serviceStatus","service_status"},{"currentIssue","current_issue"},
        {"targetIssue","target_issue"},{"apiLine","api_line"},
        {"lastDecision","last_decision"},{"lastError","last_error"},
        {"backtestStatus","backtest_status"},{"backtestMode","backtest_mode"},
        {"backtestSummary","backtest_last_summary"},{"manualBacktestStatus","manual_backtest_status"},
        {"manualBacktestSummary","manual_backtest_summary"},{"learningSummary","learning_summary"},
        {"biasSummary","bias_summary"},{"auditSummary","audit_summary"},
        {"aiJudgeSummary","ai_judge_summary"},{"selfDiagnosisSummary","self_diagnosis_summary"},
        {"promotionSummary","promotion_summary"},{"aPlusSummary","a_plus_summary"},
        {"schedulerSummary","scheduler_summary"},{"lastSnapshotId","last_snapshot_id"},
        {"autoLearnStatus","auto_learn_status"},{"autoLearnTaskKey","auto_learn_task"},
        {"autoLearnSummary","auto_learn_summary"},{"runtimeProfileSummary","runtime_profile_summary"},
        {"teacherMatrixSummary","teacher_matrix_summary"}
    };
    static final class ObservedDb extends SQLiteDatabase {
        int begins,ends,successes,stateInserts,convertedErrors;
        boolean returnMinusOne;
        ObservedDb(String bridge,String database)throws Exception {super(bridge,database);}
        @Override public void beginTransaction(){begins++;super.beginTransaction();}
        @Override public void setTransactionSuccessful(){successes++;super.setTransactionSuccessful();}
        @Override public void endTransaction(){ends++;super.endTransaction();}
        @Override public long insertWithOnConflict(String table,String nullColumn,ContentValues values,int conflict){
            if(DbHelper.TABLE_APP_STATE.equals(table))stateInserts++;
            try{return super.insertWithOnConflict(table,nullColumn,values,conflict);}
            catch(RuntimeException failure){
                if(returnMinusOne&&String.valueOf(failure.getMessage()).contains("R77_INJECTED_STATE_INSERT")){
                    convertedErrors++;return -1L;
                }
                throw failure;
            }
        }
        void reset(){begins=ends=successes=stateInserts=convertedErrors=0;returnMinusOne=false;}
    }
    static Models.AppRuntimeState state(String token)throws Exception{
        Models.AppRuntimeState s=new Models.AppRuntimeState();
        for(String[] pair:TEXT_FIELDS)Models.AppRuntimeState.class.getField(pair[0]).set(s,pair[1]+"-"+token);
        s.countdownMs=55123L;s.nextOpenTime=1791895200000L;s.backtestProgress=27;s.manualBacktestProgress=43;
        return s;
    }
    static Map<String,String> expected(String token,boolean full){
        Map<String,String> expected=new LinkedHashMap<String,String>();
        String[] core={"service_status","current_issue","target_issue","api_line","last_decision","last_error"};
        for(String key:core)expected.put(key,key+"-"+token);
        expected.put("running_flag","1");expected.put("countdown_ms","55123");expected.put("next_open_time","1791895200000");
        if(full){
            for(String[] pair:TEXT_FIELDS)expected.put(pair[1],pair[1]+"-"+token);
            expected.put("backtest_reason","backtest_last_summary-"+token);
            expected.put("backtest_progress","27");expected.put("manual_backtest_progress","43");
        }
        return expected;
    }
    static void update(DbStore store,Models.AppRuntimeState state,boolean full){
        if(full)store.updateRuntimeState(state);
        else store.updateRuntimeState(state.serviceStatus,state.currentIssue,state.targetIssue,state.apiLine,
                state.countdownMs,state.nextOpenTime,state.lastDecision,state.lastError);
    }
    static Map<String,String> snapshot(SQLiteDatabase db,boolean timestamps){
        Map<String,String> rows=new LinkedHashMap<String,String>();
        try(Cursor c=db.rawQuery("SELECT state_key,state_value,updated_at FROM app_state ORDER BY state_key",null)){
            while(c.moveToNext())rows.put(c.getString(0),c.getString(1)+(timestamps?"|at="+c.getLong(2):""));
        }
        return rows;
    }
    static long commits(DbStore store)throws Exception{
        Field f=DatabaseCoordinator.class.getDeclaredField("commitCount");f.setAccessible(true);
        return f.getInt(store.coordinatorR36());
    }
    static void clear(ObservedDb db){db.execSQL("DROP TRIGGER IF EXISTS r77_runtime_state_fault");db.execSQL("DROP TABLE IF EXISTS r77_runtime_fault_counter");db.execSQL("DELETE FROM app_state");db.reset();}
    static void seed(ObservedDb db,Map<String,String> values){
        for(Map.Entry<String,String> e:values.entrySet())db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,12345)",new Object[]{e.getKey(),e.getValue()});
        db.reset();
    }
    static void equalValues(SQLiteDatabase db,Map<String,String> values,String name){check(snapshot(db,false).equals(values),name);}
    static void basicContracts(DbStore store,ObservedDb db)throws Exception{
        for(boolean full:new boolean[]{false,true}){
            clear(db);long before=commits(store);update(store,state("one"),full);
            String prefix=full?"FULL31":"CORE9";
            check(db.begins==1&&db.ends==1&&db.successes==1,prefix+"_ONE_SQLITE_TRANSACTION");
            check(commits(store)==before+1,prefix+"_ONE_COORDINATOR_WRITE");
            check(db.stateInserts==(full?31:9),prefix+"_EXACT_FIELD_COUNT");
            equalValues(db,expected("one",full),prefix+"_ALL_FIELDS_EXACT");
            check(!db.inTransaction(),prefix+"_TRANSACTION_RELEASED");
            try(Cursor c=db.rawQuery("SELECT COUNT(DISTINCT updated_at) FROM app_state",null)){
                check(c.moveToFirst()&&c.getInt(0)==1,prefix+"_ONE_OBSERVATION_TIMESTAMP");
            }
        }
        Map<String,String> before=snapshot(db,true);db.reset();long count=commits(store);
        store.updateRuntimeState((Models.AppRuntimeState)null);store.setStates(null);store.setStates(new LinkedHashMap<String,String>());
        check(db.begins==0&&db.stateInserts==0&&commits(store)==count,"NULL_AND_EMPTY_BATCHES_DO_NOT_WRITE");
        check(snapshot(db,true).equals(before),"NULL_STATE_PRESERVES_ALL_ROWS");
        clear(db);store.updateRuntimeState(null,null,null,null,-7L,Long.MAX_VALUE,null,null);
        Map<String,String> nullable=new LinkedHashMap<String,String>();
        for(String key:new String[]{"service_status","current_issue","target_issue","api_line","last_decision","last_error"})nullable.put(key,"");
        nullable.put("running_flag","1");nullable.put("countdown_ms","-7");nullable.put("next_open_time",String.valueOf(Long.MAX_VALUE));
        equalValues(db,nullable,"CORE_NULL_STRINGS_AND_LONG_VALUES_PRESERVED");
        store.updateRuntimeState(AppConfig.SERVICE_STATUS_STOPPED,"a","b","c",0L,0L,"d","e");
        check("0".equals(store.getState("running_flag","missing")),"STOPPED_FLAG_REMAINS_ZERO");
        clear(db);seed(db,expected("old",true));Models.AppRuntimeState empty=state("empty");
        empty.backtestSummary="";empty.manualBacktestSummary=null;empty.teacherMatrixSummary=null;empty.learningSummary=null;
        update(store,empty,true);Map<String,String> kept=expected("empty",true);
        kept.put("backtest_last_summary","backtest_last_summary-old");kept.put("backtest_reason","backtest_last_summary-old");
        kept.put("manual_backtest_summary","manual_backtest_summary-old");kept.put("teacher_matrix_summary","teacher_matrix_summary-old");kept.put("learning_summary","");
        equalValues(db,kept,"EMPTY_CONDITIONAL_SUMMARIES_AND_NULL_TEACHER_RETAIN_OLD_VALUES");
        empty.backtestSummary=null;empty.manualBacktestSummary="";empty.teacherMatrixSummary="";update(store,empty,true);
        kept.put("teacher_matrix_summary","");equalValues(db,kept,"NULL_BACKTEST_EMPTY_MANUAL_RETAIN_BUT_EXPLICIT_EMPTY_TEACHER_CLEARS");
        clear(db);Map<String,String> mixed=new LinkedHashMap<String,String>();mixed.put(null,"ignore-null-key");mixed.put("","ignore-empty-key");mixed.put("nullable",null);mixed.put("normal","value");store.setStates(mixed);
        Map<String,String> two=new LinkedHashMap<String,String>();two.put("nullable","");two.put("normal","value");
        equalValues(db,two,"BATCH_SKIPS_NULL_EMPTY_KEYS_AND_NORMALIZES_NULL_VALUES");
        check(db.stateInserts==2&&db.begins==1&&db.ends==1,"MIXED_KEY_BATCH_ONE_TRANSACTION_TWO_INSERTS");
    }
    static boolean contains(Throwable error,String fragment){
        for(Throwable x=error;x!=null;x=x.getCause())if(String.valueOf(x.getMessage()).contains(fragment))return true;
        return false;
    }
    static void directMinusOneContract(DbStore store,ObservedDb db)throws Exception{
        clear(db);seed(db,expected("old",true));Map<String,String> before=snapshot(db,true);
        db.execSQL("CREATE TRIGGER r77_runtime_state_fault BEFORE INSERT ON app_state WHEN NEW.state_key='last_error' BEGIN SELECT RAISE(ABORT,'R77_INJECTED_STATE_INSERT'); END");
        db.reset();db.returnMinusOne=true;Throwable caught=null;
        try{store.setStates(expected("new",true));}catch(RuntimeException failure){caught=failure;}
        System.out.println("OBSERVED|DIRECT_RETURN_MINUS_ONE|returned_errors="+db.convertedErrors
                +"|propagated="+(caught!=null)+"|partial_change="+!snapshot(db,true).equals(before));
        check(snapshot(db,true).equals(before),"DIRECT_RETURN_MINUS_ONE_ZERO_PARTIAL_ROWS");
        check(caught!=null&&contains(caught,"R77_STATE_BATCH_INSERT_FAILED"),"DIRECT_RETURN_MINUS_ONE_FAILURE_PROPAGATES");
        check(db.convertedErrors==1&&db.stateInserts>1&&db.stateInserts<31,"DIRECT_RETURN_MINUS_ONE_ACTUAL_MID_BATCH_SQL_FAULT");
        check(db.begins==1&&db.ends==1&&db.successes==0&&!db.inTransaction(),"DIRECT_RETURN_MINUS_ONE_ROLLBACK_RELEASED");
        db.execSQL("DROP TRIGGER r77_runtime_state_fault");db.reset();store.setStates(expected("new",true));
        equalValues(db,expected("new",true),"DIRECT_RETURN_MINUS_ONE_RETRY_COMPLETE");
    }
    static boolean telemetry(DbStore store,Map<String,String> values)throws Exception{
        // Reflection permits the same harness to compile against the old-HEAD
        // negative control, whose strict-state tests fail before this new API.
        try{return (Boolean)DbStore.class.getMethod("trySetTelemetryStatesR77",Map.class).invoke(store,values);}
        catch(InvocationTargetException wrapped){
            Throwable failure=wrapped.getCause();
            if(failure instanceof RuntimeException)throw (RuntimeException)failure;
            if(failure instanceof Error)throw (Error)failure;
            throw wrapped;
        }
    }
    static void telemetryContracts(DbStore store,ObservedDb db)throws Exception{
        clear(db);long before=commits(store);
        check(telemetry(store,null)&&telemetry(store,new LinkedHashMap<String,String>()),"OPTIONAL_TELEMETRY_EMPTY_IS_SUCCESSFUL_NO_OP");
        check(db.stateInserts==0&&db.begins==0&&commits(store)==before,"OPTIONAL_TELEMETRY_EMPTY_HAS_NO_WRITER");
        check(telemetry(store,expected("display",true)),"OPTIONAL_TELEMETRY_SUCCESS_RETURNS_TRUE");
        equalValues(db,expected("display",true),"OPTIONAL_TELEMETRY_SUCCESS_SAME_FIELDS_AS_STRICT_BATCH");
        check(db.begins==1&&db.ends==1&&db.successes==1&&commits(store)==before+1,"OPTIONAL_TELEMETRY_SUCCESS_ONE_ATOMIC_WRITE");
        for(boolean minusOne:new boolean[]{false,true}){
            clear(db);seed(db,expected("old",true));Map<String,String> original=snapshot(db,true);
            db.execSQL("CREATE TRIGGER r77_runtime_state_fault BEFORE INSERT ON app_state WHEN NEW.state_key='last_error' BEGIN SELECT RAISE(ABORT,'R77_INJECTED_STATE_INSERT'); END");
            db.reset();db.returnMinusOne=minusOne;String tag=minusOne?"OPTIONAL_TELEMETRY_MINUS_ONE":"OPTIONAL_TELEMETRY_SQL_THROW";
            check(!telemetry(store,expected("display",true)),tag+"_FAILURE_RETURNS_FALSE");
            check(snapshot(db,true).equals(original),tag+"_ENTIRE_BATCH_ROLLED_BACK");
            check(db.begins==1&&db.ends==1&&db.successes==0&&!db.inTransaction(),tag+"_INDEPENDENT_TRANSACTION_RELEASED");
            check(db.convertedErrors==(minusOne?1:0),tag+"_REAL_SQL_ERROR_TRANSPORT_OBSERVED");
            db.execSQL("DROP TRIGGER r77_runtime_state_fault");db.reset();
            check(telemetry(store,expected("display",true)),tag+"_RETRY_RETURNS_TRUE");
            equalValues(db,expected("display",true),tag+"_RETRY_ALL_FIELDS_COMMITTED");
        }
        clear(db);seed(db,expected("old",true));db.beginTransaction();
        try{
            db.execSQL("INSERT INTO app_state(state_key,state_value,updated_at) VALUES('outer-sentinel','safe-owner',12345)");
            Throwable rejected=null;
            try{telemetry(store,expected("display",true));}catch(IllegalStateException failure){rejected=failure;}
            check(rejected!=null&&contains(rejected,"R77_TELEMETRY_REQUIRES_INDEPENDENT_TRANSACTION"),"OPTIONAL_TELEMETRY_NESTED_CALL_EXPLICITLY_REJECTED");
            check(db.stateInserts==0,"OPTIONAL_TELEMETRY_NESTED_REJECTS_BEFORE_FIRST_BATCH_WRITE");
            check(db.inTransaction(),"OPTIONAL_TELEMETRY_REJECTION_PRESERVES_OUTER_OWNER_TRANSACTION");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        Map<String,String> owner=expected("old",true);owner.put("outer-sentinel","safe-owner");
        equalValues(db,owner,"OPTIONAL_TELEMETRY_REJECTED_BATCH_CANNOT_LEAK_INTO_OWNER_COMMIT");
    }
    static Map<String,String> sliceExpected(String token,String updatedAt){
        Map<String,String> values=new LinkedHashMap<String,String>();
        String prefix="background_slice_r77teacher_";
        values.put(prefix+"status","status-"+token);values.put(prefix+"processed","17");
        values.put(prefix+"pending","3");values.put(prefix+"cost_ms","45");
        values.put(prefix+"summary","summary-"+token);values.put(prefix+"updated_at",updatedAt);
        return values;
    }
    static void saveSlice(DbStore store,String token){
        store.saveBackgroundSliceState("r77teacher","status-"+token,17,3,45L,"summary-"+token);
    }
    static void secondSliceInsertFault(ObservedDb db){
        // The product helper uses HashMap, whose iteration order is not part of
        // its contract. Count actual SQLite inserts, then reject exactly number 2.
        db.execSQL("CREATE TEMP TABLE r77_runtime_fault_counter(n INTEGER NOT NULL)");
        db.execSQL("INSERT INTO r77_runtime_fault_counter(n) VALUES(0)");
        db.execSQL("CREATE TEMP TRIGGER r77_runtime_state_fault BEFORE INSERT ON app_state "
                +"WHEN NEW.state_key LIKE 'background_slice_r77teacher_%' BEGIN "
                +"UPDATE r77_runtime_fault_counter SET n=n+1; "
                +"SELECT CASE WHEN (SELECT n FROM r77_runtime_fault_counter)=2 "
                +"THEN RAISE(ABORT,'R77_INJECTED_STATE_INSERT') END; END");
    }
    static void backgroundSliceContracts(DbStore store,ObservedDb db)throws Exception{
        clear(db);long count=commits(store);
        store.saveBackgroundSliceState(null,"unused",1,2,3L,"unused");
        store.saveBackgroundSliceState("","unused",1,2,3L,"unused");
        check(db.stateInserts==0&&db.begins==0&&commits(store)==count,"BACKGROUND_SLICE_EMPTY_TASK_DOES_NOT_WRITE");
        for(boolean minusOne:new boolean[]{false,true}){
            clear(db);seed(db,sliceExpected("old","12345"));Map<String,String> original=snapshot(db,true);
            secondSliceInsertFault(db);db.reset();db.returnMinusOne=minusOne;count=commits(store);
            String tag=minusOne?"BACKGROUND_SLICE_MINUS_ONE":"BACKGROUND_SLICE_SQL_THROW";
            Throwable escaped=null;
            try{saveSlice(store,"new");}catch(RuntimeException failure){escaped=failure;}
            check(escaped==null,tag+"_OPTIONAL_DASHBOARD_FAILURE_DOES_NOT_ESCAPE");
            check(db.stateInserts==2,tag+"_REAL_SQL_FAULT_IS_EXACTLY_SECOND_FIELD");
            check(db.convertedErrors==(minusOne?1:0),tag+"_REAL_SQL_ERROR_TRANSPORT_OBSERVED");
            check(snapshot(db,true).equals(original),tag+"_ZERO_PARTIAL_VALUES_OR_TIMESTAMPS");
            check(db.begins==1&&db.ends==1&&db.successes==0&&!db.inTransaction(),tag+"_ONE_ROLLBACK_AND_RELEASE");
            check(commits(store)==count,tag+"_NO_SUCCESSFUL_WRITER_COMMIT");
            db.execSQL("DROP TRIGGER r77_runtime_state_fault");db.execSQL("DROP TABLE r77_runtime_fault_counter");db.reset();
            long started=System.currentTimeMillis();saveSlice(store,"new");long finished=System.currentTimeMillis();
            String updatedAt=store.getState("background_slice_r77teacher_updated_at","");
            long observed=Long.parseLong(updatedAt);
            check(observed>=started&&observed<=finished,tag+"_RETRY_ACTUAL_FRESH_TIMESTAMP");
            equalValues(db,sliceExpected("new",updatedAt),tag+"_RETRY_ALL_SIX_FIELDS_EXACT");
            check(db.stateInserts==6&&db.begins==1&&db.ends==1&&db.successes==1
                    &&commits(store)==count+1&&!db.inTransaction(),tag+"_RETRY_ONE_COMPLETE_ATOMIC_WRITE");
        }
        clear(db);store.saveBackgroundSliceState("r77teacher",null,-1,0,-9L,null);
        Map<String,String> nullable=sliceExpected("unused",store.getState("background_slice_r77teacher_updated_at",""));
        nullable.put("background_slice_r77teacher_status","");nullable.put("background_slice_r77teacher_summary","");
        nullable.put("background_slice_r77teacher_processed","-1");nullable.put("background_slice_r77teacher_pending","0");nullable.put("background_slice_r77teacher_cost_ms","0");
        equalValues(db,nullable,"BACKGROUND_SLICE_NULL_STRINGS_AND_NONNEGATIVE_COST_PRESERVED");
        clear(db);seed(db,sliceExpected("old","12345"));Map<String,String> original=snapshot(db,true);
        db.beginTransaction();
        try{
            db.execSQL("INSERT INTO app_state(state_key,state_value,updated_at) VALUES('outer-sentinel','teacher-result',12345)");
            Throwable rejected=null;
            try{saveSlice(store,"new");}catch(IllegalStateException failure){rejected=failure;}
            check(rejected!=null&&contains(rejected,"R77_TELEMETRY_REQUIRES_INDEPENDENT_TRANSACTION"),"BACKGROUND_SLICE_NESTED_CALL_EXPLICITLY_REJECTED");
            check(db.stateInserts==0,"BACKGROUND_SLICE_NESTED_REJECTS_BEFORE_FIRST_FIELD");
            check(db.inTransaction(),"BACKGROUND_SLICE_NESTED_REJECTION_PRESERVES_OWNER_TRANSACTION");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        original.put("outer-sentinel","teacher-result|at=12345");
        check(snapshot(db,true).equals(original),"BACKGROUND_SLICE_REJECTED_BATCH_CANNOT_LEAK_INTO_TEACHER_OWNER_COMMIT");
    }
    static void faultContract(DbStore store,ObservedDb db,boolean full,boolean minusOne,boolean nested)throws Exception{
        clear(db);seed(db,expected("old",true));Map<String,String> before=snapshot(db,true);
        db.execSQL("CREATE TRIGGER r77_runtime_state_fault BEFORE INSERT ON app_state WHEN NEW.state_key='last_error' BEGIN SELECT RAISE(ABORT,'R77_INJECTED_STATE_INSERT'); END");
        db.reset();db.returnMinusOne=minusOne;Models.AppRuntimeState changed=state("new");
        String tag=(full?"FULL31":"CORE9")+(minusOne?"_RETURN_MINUS_ONE":"_SQL_THROW")+(nested?"_NESTED":"_STANDALONE");
        Throwable caught=null;
        try{
            if(nested)store.coordinatorR36().runAtomicWriteOrThrowF01("R77_TEST_OUTER",outer->{
                outer.execSQL("INSERT INTO app_state(state_key,state_value,updated_at) VALUES('outer-sentinel','before-inner',12345)");
                update(store,changed,full);return Boolean.TRUE;
            });else update(store,changed,full);
        }catch(RuntimeException failure){caught=failure;}
        check(caught!=null,tag+"_FAILURE_PROPAGATES");
        check(contains(caught,minusOne?"R77_STATE_BATCH_INSERT_FAILED":"R77_INJECTED_STATE_INSERT"),tag+"_EXACT_CAUSE_PRESERVED");
        check(db.stateInserts==9,tag+"_FAULT_OCCURS_AFTER_EIGHT_REAL_INSERTS");
        check(db.convertedErrors==(minusOne?1:0),tag+"_ANDROID_ERROR_TRANSPORT_OBSERVED");
        check(db.begins==1&&db.ends==1&&db.successes==0&&!db.inTransaction(),tag+"_SINGLE_TRANSACTION_ROLLED_BACK");
        check(snapshot(db,true).equals(before),tag+"_ZERO_PARTIAL_ROWS_OR_TIMESTAMP_CHANGES");
        db.execSQL("DROP TRIGGER r77_runtime_state_fault");db.reset();
        if(nested)store.coordinatorR36().runAtomicWriteOrThrowF01("R77_TEST_OUTER_RETRY",outer->{
            outer.execSQL("INSERT INTO app_state(state_key,state_value,updated_at) VALUES('outer-sentinel','retry',12345)");
            update(store,changed,full);return Boolean.TRUE;
        });else update(store,changed,full);
        Map<String,String> repaired=expected("old",true);repaired.putAll(expected("new",full));if(nested)repaired.put("outer-sentinel","retry");
        equalValues(db,repaired,tag+"_RETRY_COMMITS_COMPLETE_OBSERVATION");
        check(db.begins==1&&db.ends==1&&db.successes==1&&!db.inTransaction(),tag+"_RETRY_ONE_TRANSACTION");
    }
    public static void main(String[] args)throws Exception{
        check(Math.abs(android.os.SystemClock.elapsedRealtimeNanos()-System.nanoTime())<1000000000L,
                "HOST_CLOCK_IS_REAL_MONOTONIC_NANOTIME_NOT_FAKE_STEPS");
        RealtimePressureGateR811.resetForTests();
        try(ObservedDb db=new ObservedDb(args[0],args[1])){
            DbStore store=HostProductR974.store(db);
            if(args.length>3&&"direct-minus-one".equals(args[3]))directMinusOneContract(store,db);
            else if(args.length>3&&"background-slice".equals(args[3]))backgroundSliceContracts(store,db);
            else{
                basicContracts(store,db);directMinusOneContract(store,db);
                for(boolean full:new boolean[]{false,true})for(boolean minusOne:new boolean[]{false,true})for(boolean nested:new boolean[]{false,true})faultContract(store,db,full,minusOne,nested);
                telemetryContracts(store,db);backgroundSliceContracts(store,db);
            }
        }
        String result="{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_DBSTORE_COORDINATOR_REAL_SQLITE_WITH_ANDROID_INSERT_ERROR_TRANSPORT_ADAPTER\",\"android_pool_runtime\":\"NOT_TESTED_HERE\"}\n";
        Files.writeString(Path.of(args[2]),result);System.out.println(result);
    }
}
