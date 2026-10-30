package com.ethk3.novaonepro.strategyv2;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.ethk3.novaonepro.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** The actual coordinator/F01/scope/owner/schema path on real JDBC WAL files. Only Android platform IO is
 * supplied by functional HOST shims; no product coordinator, routing, claim, or schema behavior is replaced. */
public final class ScopedSchemaPendingR29HostTest {
    static int checks;
    static void check(String id,boolean ok){System.out.println("R29_SCOPED_SCHEMA_PENDING|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String causes(Throwable error){StringBuilder text=new StringBuilder();for(int i=0;error!=null&&i<20;i++,error=error.getCause())text.append(error.getClass().getName()).append(':').append(error.getMessage()).append(" <- ");return text.toString();}
    static void set(Object object,String field,Object value)throws Exception{Field f=object.getClass().getDeclaredField(field);f.setAccessible(true);f.set(object,value);}
    static Object allocate(Class<?> type)throws Exception{Class<?> u=Class.forName("sun.misc.Unsafe");Field f=u.getDeclaredField("theUnsafe");f.setAccessible(true);return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);}
    static boolean pending(ResearchSchemaCoordinatorR926.StepResult result)throws Exception{
        try{return ResearchSchemaCoordinatorR926.StepResult.class.getField("pending").getBoolean(result);}catch(NoSuchFieldException old){return false;}
    }
    static void seed(SQLiteDatabase root)throws Exception{
        ResearchSchemaOwnerR29HostTest.tx(root,()->{
            SchemaR27.ensure(root);ResearchEventBridgeR29.ensureRealtime(root);LegacyCapabilityAdapterR29.initialize(root,"SYNTHETIC_SCOPED_HOST");
            root.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,cursor) VALUES('synthetic-existing-task','P2','fixture',0)");
            root.execSQL("INSERT INTO r27_task_error VALUES(73,'synthetic-existing-task','SYNTHETIC_HISTORICAL_FAILURE','preserve existing evidence','SYNTHETIC_OLD_STACK',1)");
            long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001"),now=System.currentTimeMillis();
            for(int i=0;i<385;i++){
                Models.Draw draw=new Models.Draw();draw.issue=TargetIssueResolver.fromSequenceOrdinalSU1(first+i);draw.numbers=new int[10];for(int p=0;p<10;p++)draw.numbers[p]=(p+i)%10+1;draw.openTime=now-(386L-i)*60000;draw.fetchTime=draw.openTime+1000;
                String hash=DrawHashR36.sha256V2(draw);Object[]values=new Object[15];values[0]=draw.issue;values[1]=draw.openTime;values[2]=draw.fetchTime;for(int p=0;p<10;p++)values[3+p]=draw.numbers[p];values[13]=draw.fetchTime;values[14]=hash;
                root.execSQL("INSERT INTO draws(issue,open_time,fetch_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,first_seen_at,draw_hash_v2,draw_hash_version,dirty) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,2,0)",values);
                android.content.ContentValues canonical=ResearchSchemaOwnerR29HostTest.row(root,DbHelper.TABLE_R811_DRAW_CANONICAL,draw.issue);canonical.put("status","CONFIRMED_CANONICAL_DRAW");canonical.put("canonical_draw_hash",hash);root.insertOrThrow(DbHelper.TABLE_R811_DRAW_CANONICAL,null,canonical);
            }
        });
    }
    public static void main(String[]args)throws Exception{
        Path directory=Files.createTempDirectory("r29-real-scoped-schema-");Application app=new Application(directory.toFile());
        SQLiteDatabase root=null,rpReader=null,rootReader=null,preparation=null;ResearchPoolR27 pool=null;DatabaseCoordinator coordinator=null;
        try{
            root=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+app.getDatabasePath(AppConfig.DB_NAME)));preparation=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("unused-owned-schema.db")));
            ResearchSchemaOwnerR29HostTest.base(root);ResearchSchemaOwnerR29HostTest.base(preparation);ResearchSchemaOwnerR29HostTest.rootBaseAndOwnedCopy(root,preparation);preparation.close();preparation=null;seed(root);
            check("P1_ACTUAL_ROOT_HAS_MORE_THAN_THREE_CANONICAL_PAGES",n(root,"SELECT count(*) FROM "+DbHelper.TABLE_R811_DRAW_CANONICAL)==385);
            // Run the actual normal DCo constructor, including its original locks, atomics and executor.
            coordinator=DatabaseCoordinator.get(app);Method getHelper=DatabaseCoordinator.class.getDeclaredMethod("getHelper");getHelper.setAccessible(true);DbHelper helper=(DbHelper)getHelper.invoke(coordinator);SQLiteOpenHelper.bind(helper,root);
            // DbStore's unrelated Android service/startup constructor is bypassed; all its invoked methods are real.
            DbStore store=(DbStore)allocate(DbStore.class);set(store,"researchAppCP12",app);set(store,"researchInputRootCP11",new java.io.File(app.getFilesDir(),"research-input-v2"));set(store,"coordinator",coordinator);set(store,"helper",helper);set(store,"columnCache",new HashMap<>());
            check("P2_REAL_COORDINATOR_HELPER_AND_STORE_ARE_BOUND",store.coordinatorForResearchR952()==coordinator&&coordinator.readable()==root);
            pool=new ResearchPoolR27(store,"SYNTHETIC_SCOPED_HOST",2);rootReader=coordinator.realtimeReadOnlyR29();
            SQLiteDatabase before=ResearchRouteR29.openWorkerReader(app);ResearchPoolR27.Worker worker=pool.new Worker(before,rootReader,"synthetic-scoped-migration");String cutover;
            try{cutover=ResearchCutoverR29.run(store,worker);}finally{before.close();}
            check("P3_REAL_CUTOVER_PUBLISHED_WITH_CANONICAL_BOOTSTRAP_PENDING","ACTIVE_INDEPENDENT_RESEARCH_WRITER".equals(cutover));rpReader=ResearchRouteR29.openWorkerReader(app);check("P4_RP_HAS_NEVER_BEEN_CANONICAL_READY",!CriticalInputMirrorR29.canonicalReady(rpReader));
            DatabaseCoordinator owner=coordinator;long start=System.nanoTime(),deadline=start+60_000_000_000L;int calls=0,preCallbackPending=0,adapterPending=0;boolean ready=false,claimed=false,intermediate=false;
            while(System.nanoTime()<deadline&&!ready){
                ResearchSchemaCoordinatorR926.StepResult step;try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){step=ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);}calls++;
                boolean pending=pending(step);System.out.println("R29_SCOPED_SCHEMA_PENDING|STEP|"+calls+"|status="+step.status+"|entered="+step.enteredWriter+"|pending="+pending+"|units="+step.committedUnits+"|canonicalRows="+(ResearchInputMirrorR29.table(rpReader,CriticalInputMirrorR29.CANONICAL_INPUT)?n(rpReader,"SELECT count(*) FROM "+CriticalInputMirrorR29.CANONICAL_INPUT):0));
                if(!step.enteredWriter){if(!pending||step.cause!=null||step.committedUnits!=0)throw new AssertionError("PENDING_DID_NOT_SURVIVE_REAL_F01:"+step.detail,step.cause);preCallbackPending++;}
                if(step.cause!=null)throw new AssertionError("ACTUAL_SCOPED_SCHEMA_FAILURE:"+step.detail,step.cause);
                if(pending&&(step.ready||step.committedUnits!=0))throw new AssertionError("PENDING_INVENTED_SCHEMA_PROGRESS");
                if(!step.ready&&step.committedUnits>0)intermediate=true;ready=step.ready;
                if(!claimed){ResearchPoolR27.Outcome outcome;try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){
                    outcome=LegacyCapabilityAdapterR29.dispatch(()->{long advanced=owner.runAtomicWriteOrThrowF01("R26_TASK_CLAIM",db->{db.execSQL("UPDATE r26_task SET cursor=cursor+1 WHERE task_id='synthetic-existing-task'");return 1L;});return ResearchPoolR27.Outcome.progress(advanced,advanced);});
                }if(outcome.units==0){adapterPending++;check("CLAIM_PENDING_"+adapterPending+"_PRESERVES_CURSOR_AND_OLD_ERROR",outcome.reason.contains("COOPERATIVE_WRITER_BUSY")&&n(rpReader,"SELECT cursor FROM r26_task WHERE task_id='synthetic-existing-task'")==0&&n(rpReader,"SELECT count(*) FROM r27_task_error WHERE error_id=73")==1);}else claimed=true;}
            }
            check("P5_AT_LEAST_FOUR_REAL_PRECALLBACK_PENDING_RESULTS",preCallbackPending>=4);check("P6_ADAPTER_REAL_F01_PENDING_DOES_NOT_COMMIT_OR_QUARANTINE",adapterPending>=3&&claimed&&n(rpReader,"SELECT cursor FROM r26_task WHERE task_id='synthetic-existing-task'")==1&&n(rpReader,"SELECT count(*) FROM r27_task_error")==1);
            check("P7_REAL_SCOPED_SCHEMA_REACHES_READY_IN_60_SECONDS",ready&&intermediate&&CriticalInputMirrorR29.canonicalReady(rpReader));
            check("P8_BOOTSTRAP_DID_NOT_COMMIT_FAILED_SCHEMA_JOURNALS",n(rpReader,"SELECT count(*) FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" WHERE status='FAILED'")==0);
            ResearchRouteR29.write(store,"SYNTHETIC_SCOPED_ROLLBACK_FIXTURE",db->{db.execSQL("CREATE TABLE r29_scoped_callback_probe(k TEXT PRIMARY KEY)");return 0;});
            String researchPath=app.getDatabasePath(ResearchRouteR29.FILE_NAME).getPath();AtomicInteger injected=new AtomicInteger();
            SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(db.inTransaction()&&db.getPath().equals(researchPath)&&sql.equals("PRAGMA schema_version")){SQLiteDatabase.hostBeforeQuery=null;db.execSQL("INSERT INTO r29_scoped_callback_probe VALUES('busy-must-rollback')");injected.incrementAndGet();throw new IllegalStateException("CP3_DB_BUSY_SYNTHETIC_INSIDE_CALLBACK");}};
            ResearchSchemaCoordinatorR926.StepResult busy;try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){busy=ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);}finally{SQLiteDatabase.hostBeforeQuery=null;}
            check("P9_ENTERED_CALLBACK_BUSY_ROLLS_BACK_AND_RETURNS_PENDING",injected.get()==1&&busy.enteredWriter&&pending(busy)&&busy.cause==null&&busy.committedUnits==0&&!busy.ready&&n(rpReader,"SELECT count(*) FROM r29_scoped_callback_probe")==0);
            SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(db.inTransaction()&&db.getPath().equals(researchPath)&&sql.equals("PRAGMA schema_version")){SQLiteDatabase.hostBeforeQuery=null;db.execSQL("INSERT INTO r29_scoped_callback_probe VALUES('sql-error-must-rollback')");db.execSQL("INSERT INTO r29_missing_real_schema_table VALUES(1)");}};
            ResearchSchemaCoordinatorR926.StepResult failed;try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){failed=ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);}finally{SQLiteDatabase.hostBeforeQuery=null;}
            check("P10_REAL_SQLITE_MISSING_TABLE_IS_FAILURE_NOT_PENDING",failed.enteredWriter&&!pending(failed)&&failed.cause!=null&&failed.committedUnits==0&&failed.detail.contains("no such table")&&n(rpReader,"SELECT count(*) FROM r29_scoped_callback_probe")==0);
            try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){ResearchSchemaCoordinatorR926.StepResult stable=ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);check("P11_READY_NOOP_IS_NOT_PROGRESS_OR_PENDING",stable.ready&&!pending(stable)&&stable.cause==null&&stable.committedUnits==0);}
            boolean actualSqlFailure=false;try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){try{LegacyCapabilityAdapterR29.dispatch(()->{owner.runAtomicWriteOrThrowF01("R26_TASK_CLAIM",db->{db.execSQL("INSERT INTO r29_missing_claim_table VALUES(1)");return 0;});return ResearchPoolR27.Outcome.progress(1,1);});}catch(IllegalStateException expected){if(!causes(expected).contains("no such table"))throw expected;actualSqlFailure=true;}}
            check("P12_ADAPTER_DOES_NOT_SWALLOW_REAL_SQL_FAILURE",actualSqlFailure);
            long retired;try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){
                retired=owner.runAtomicWriteOrThrowF01("R26_TEST_DEFAULT_RETIRE",db->{
                    if(!StrategySchemaSU2.ready(db))StrategySchemaSU2.ensure(db);
                    return ShadowAdmissionR26.retireOne(db,TargetIssueResolver.sequenceOrdinalSU1("202610230001"),System.currentTimeMillis());
                });
            }
            check("P13_DEFAULT_SHADOW_RETIRE_READS_ROOT_CHOICE_WITHOUT_INVENTING_RP_AUTHORITY",retired==0&&n(root,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name='su2_mainline_choice'")==1&&n(rpReader,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name='su2_mainline_choice'")==0);
            System.out.println("R29_SCOPED_SCHEMA_PENDING|MEASURED|calls="+calls+"|preCallbackPending="+preCallbackPending+"|adapterPending="+adapterPending+"|milliseconds="+((System.nanoTime()-start)/1000000));
        }finally{
            SQLiteDatabase.hostBeforeQuery=null;if(pool!=null)pool.close();if(rpReader!=null)rpReader.close();if(rootReader!=null)rootReader.close();if(preparation!=null)preparation.close();if(root!=null)root.close();if(coordinator!=null){Field f=DatabaseCoordinator.class.getDeclaredField("backgroundWriter");f.setAccessible(true);((java.util.concurrent.ExecutorService)f.get(coordinator)).shutdownNow();}
            try(java.util.stream.Stream<Path> files=Files.walk(directory)){files.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}
        }
        System.out.println("R29_SCOPED_SCHEMA_PENDING|ALL_PASS|checks="+checks+"|REAL_COORDINATOR_F01_SCOPE_SCHEMA_WITH_HOST_PLATFORM_ONLY");
    }
}
