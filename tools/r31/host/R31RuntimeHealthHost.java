package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.File;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** Real production query/control, native SQLite; isolated controlled facts are never natural strategy evidence. */
public final class R31RuntimeHealthHost {
    static final String OWNER="health-owner",BOOT=IssueTimelineWriterR30.bootId(),ISSUE="202611040100";
    static final long LATEST=TargetIssueResolver.sequenceOrdinalSU1(ISSUE);
    static int checks;static TraceDb db;static Unsafe unsafe;static DbStore store;static DatabaseCoordinator coordinator;
    static class TraceDb extends SQLiteDatabase {
        final AtomicInteger reads=new AtomicInteger();volatile String queryThread;volatile Error healthFailure;
        TraceDb(Connection connection){super(connection);}
        @Override public Cursor rawQuery(String sql,String[] args){reads.incrementAndGet();if(sql.equals(RuntimeCoordinatorR26.HEALTH_QUERY_R31)){queryThread=Thread.currentThread().getName();if(healthFailure!=null)throw healthFailure;}return super.rawQuery(sql,args);}
    }
    static void need(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void field(Object target,String name,Object value)throws Exception{Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
    static Object value(Object target,String name)throws Exception{Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}
    static String scalar(String sql){try(Cursor rows=db.rawQuery(sql,null)){need(rows.moveToFirst(),sql);return rows.getString(0);}}
    static Map<String,Object> task(RuntimeCoordinatorR26.RuntimeHealthSnapshot snapshot,String id){for(Map<String,Object> row:snapshot.tasksImmutable)if(id.equals(row.get("taskId")))return row;throw new AssertionError("missing task "+id);}
    static RuntimeCoordinatorR26.RuntimeHealthSnapshot capture(){return capture(null);}
    static RuntimeCoordinatorR26.RuntimeHealthSnapshot capture(String resource){int before=db.reads.get();RuntimeCoordinatorR26.RuntimeHealthSnapshot observed=RuntimeCoordinatorR26.captureHealthSnapshotR31(db,OWNER,BOOT,System.currentTimeMillis(),android.os.SystemClock.elapsedRealtime(),"ALIVE_OWNER_RUNNING",resource);need(db.reads.get()==before+1,"health capture must use one bounded read, no per-task reads");return observed;}
    static void reset(){
        db.execSQL("DELETE FROM r26_task");db.execSQL("DELETE FROM r30r1_task_progress");db.execSQL("DELETE FROM r31_task_control");db.execSQL("DELETE FROM r31_task_lease");db.execSQL("DELETE FROM draws");db.execSQL("INSERT INTO draws(issue,dirty) VALUES(?,0)",new Object[]{ISSUE});
        for(ResearchTaskRegistryR30R1.Entry entry:ResearchTaskRegistryR30R1.all().values()){
            db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,status,owner,fence) VALUES(?,?,?,'READY',?,7)",new Object[]{entry.id,entry.id.startsWith("r27-")?"R27":"P2",entry.id,OWNER});
            db.execSQL("INSERT INTO r30r1_task_progress(task_id,session_id,fence,owner_version,work_unit,work_cursor,known_latest,pending,last_commit_clock_ordinal,completed_input_ordinal,artifact_identity,updated_at) VALUES(?,?,7,?,?,?, ?,0,?,?,?,1)",new Object[]{entry.id,OWNER,ResearchTaskRegistryR30R1.VERSION,entry.unit.name(),entry.unit==TaskProgressR30R1.Unit.ISSUE?LATEST:123,LATEST,LATEST,LATEST,"HOST_FIXTURE_ARTIFACT:"+entry.id});
        }
    }
    static void facts()throws Exception{
        reset();RuntimeCoordinatorR26.RuntimeHealthSnapshot snapshot=capture();need(snapshot.tasksImmutable.size()==60,"all 57 original and 3 additional owners retained");need(snapshot.pendingKnown&&snapshot.pending==0,"known idle inputs are zero");need(snapshot.researchHealth.equals("IDLE_WAITING_INPUT"),"all current fixture owners idle");
        need(snapshot.lastArtifactHash!=null&&!snapshot.lastArtifactHash.contains("r27-health"),"time ticks do not supply model/artifact freshness");
        try{snapshot.asMap().put("pending",123);throw new AssertionError("mutable DTO");}catch(UnsupportedOperationException expected){checks++;}
        try{snapshot.tasksImmutable.clear();throw new AssertionError("mutable task list");}catch(UnsupportedOperationException expected){checks++;}
        try{snapshot.tasksImmutable.get(0).put("state","PASS");throw new AssertionError("mutable task fact");}catch(UnsupportedOperationException expected){checks++;}
        db.execSQL("UPDATE r26_task SET status='QUARANTINED' WHERE task_id LIKE 'r27-%'");
        for(int tick=0;tick<2;tick++){snapshot=capture();need(snapshot.quarantined==13&&snapshot.researchHealth.equals("DEGRADED"),"13 isolated R27 tasks degrade independently of healthy owner heartbeat tick="+tick);}
        reset();db.execSQL("UPDATE r30r1_task_progress SET pending=NULL WHERE task_id='kernel'");snapshot=capture();need(!snapshot.pendingKnown&&snapshot.pending==null&&snapshot.unknownTaskCount==1,"NULL pending is unknown, never manufactured zero");need(task(snapshot,"kernel").get("state").equals("UNKNOWN"),"NULL owner pending explicit unknown");
        db.execSQL("UPDATE r30r1_task_progress SET owner_version='OLD_ABI',pending=0 WHERE task_id='kernel'");snapshot=capture();need(task(snapshot,"kernel").get("sourceFailure").equals("NO_CURRENT_TYPED_PROGRESS_OR_WORK_UNIT_MISMATCH"),"old ABI does not become current progress");
        reset();db.execSQL("UPDATE r30r1_task_progress SET pending=3,last_commit_clock_ordinal=?,artifact_identity='HOST_OLD_KERNEL' WHERE task_id='kernel'",new Object[]{LATEST-6});db.execSQL("UPDATE r30r1_task_progress SET updated_at=9999999,artifact_identity='HOST_NEW_OTHER_TASK' WHERE task_id='feedback'");snapshot=capture();need(snapshot.stalled==1&&snapshot.researchHealth.equals("STALLED"),"fresh other task cannot hide stalled pending kernel");need(task(snapshot,"kernel").get("state").equals("STALLED"),"per-task committed clock lag >= five periods");
        db.execSQL("UPDATE r26_task SET wait_kind='DEPENDENCY',blocked_reason='DEPENDENCY:HOST_MISSING_INPUT' WHERE task_id='kernel'");snapshot=capture();need(snapshot.stalled==0&&task(snapshot,"kernel").get("state").equals("WAITING_DEPENDENCY"),"dependency has distinct wait semantics");
        db.execSQL("UPDATE r26_task SET wait_kind='RESOURCE',blocked_reason='MEMORY_PRESSURE' WHERE task_id='kernel'");snapshot=capture();need(snapshot.stalled==0&&task(snapshot,"kernel").get("state").equals("RESOURCE_WAIT"),"resource pause is not failed research");
        db.execSQL("UPDATE r26_task SET status='NEEDS_FIX' WHERE task_id='kernel'");snapshot=capture();need(snapshot.needsFix==1&&snapshot.researchHealth.equals("DEGRADED"),"needs-fix outranks resource wait");
        reset();db.execSQL("UPDATE r30r1_task_progress SET last_commit_clock_ordinal=? WHERE task_id='kernel'",new Object[]{LATEST-1});snapshot=capture();need(task(snapshot,"kernel").get("pending")==null,"stale idle zero is not current input evidence");
        db.execSQL("UPDATE r30r1_task_progress SET pending=0,last_commit_clock_ordinal=?,completed_input_ordinal=? WHERE task_id='r27-live'",new Object[]{LATEST-6,LATEST-6});snapshot=capture();need(((Number)task(snapshot,"r27-live").get("pending")).longValue()==6&&task(snapshot,"r27-live").get("state").equals("STALLED"),"live pending derived from real completed input watermark");
        reset();db.execSQL("UPDATE r30r1_task_progress SET last_commit_clock_ordinal=?,work_cursor=? WHERE task_id='r27-replay-dataset'",new Object[]{LATEST-5,LATEST-5});snapshot=capture();need(task(snapshot,"r27-replay-dataset").get("state").equals("IDLE_WAITING_INPUT"),"dataset refresh 360 threshold not false stall at five");
        db.execSQL("UPDATE r30r1_task_progress SET last_commit_clock_ordinal=? WHERE task_id='kernel'",new Object[]{LATEST+1});snapshot=capture();need(task(snapshot,"kernel").get("state").equals("UNKNOWN")&&task(snapshot,"kernel").get("sourceFailure").equals("COMMIT_CLOCK_AHEAD_OF_SOURCE"),"future artifact clock rejected as unknown");
        reset();snapshot=capture("MEMORY_EMERGENCY:HOST_CONTROLLED");need(snapshot.researchHealth.equals("RESOURCE_PAUSED"),"resource pause has explicit reason");
        db.execSQL("DELETE FROM draws");snapshot=capture();need(snapshot.latestSourceOrdinal==null&&snapshot.pending==null&&snapshot.researchHealth.equals("UNKNOWN"),"missing source cannot be a healthy zero backlog");
        db.execSQL("ALTER TABLE draws RENAME TO host_absent_draw_source");snapshot=capture();need(snapshot.researchHealth.equals("UNKNOWN")&&snapshot.reason.startsWith("HEALTH_SOURCE_UNAVAILABLE"),"missing source table reports query failure separately");db.execSQL("ALTER TABLE host_absent_draw_source RENAME TO draws");
        reset();db.execSQL("UPDATE r26_task SET blocked_reason=? WHERE task_id='kernel'",new Object[]{"x".repeat(10000)});snapshot=capture();need(task(snapshot,"kernel").get("blockedReason").toString().length()<=256,"text fields bounded in SQL before health DTO materialization");
        for(int i=0;i<70;i++)db.execSQL("INSERT INTO r26_task(task_id,status,owner) VALUES(?,'READY',?)",new Object[]{"zzz-host-unregistered-"+i,OWNER});snapshot=capture();need(snapshot.tasksImmutable.size()==128&&snapshot.researchHealth.equals("UNKNOWN")&&snapshot.reason.equals("TASK_CATALOG_EXCEEDS_128_ROW_BOUND"),"catalog overflow is explicit unknown and cannot bypass row byte estimate");
        System.out.println("R31_HEALTH_SQL_NATIVE_HOST_PASS singleQuery=true quarantine13=DEGRADED unknownNotZero=true perTaskStall=true dependencyResourceDistinct=true cappedRows=128");
    }
    static RuntimeCoordinatorR26 coordinator(CountDownLatch stop)throws Exception{
        RuntimeCoordinatorR26 runtime=(RuntimeCoordinatorR26)unsafe.allocateInstance(RuntimeCoordinatorR26.class);Thread thread=new Thread(()->{try{stop.await();}catch(InterruptedException ignored){}},"HOST_ALIVE_OWNER_THREAD");thread.setDaemon(true);thread.start();
        field(runtime,"store",store);field(runtime,"owner",OWNER);field(runtime,"signal",new Object());field(runtime,"worker",thread);field(runtime,"memoryBudgetR31",MemoryBudgetR31.processBudget());field(runtime,"memoryObservationR31",MemoryBudgetR31.processBudget().snapshot());field(runtime,"healthProbePendingR31",new AtomicBoolean());field(runtime,"nextTaskRecoveryR31",new HashMap<String,Long>());field(runtime,"lastStalledArtifactR31",new HashMap<String,String>());field(runtime,"lastRecoveryArtifactR31",new HashMap<String,String>());field(runtime,"drainingTasksR31",new HashMap<String,RuntimeCoordinatorR26.Claim>());return runtime;
    }
    static void probe(RuntimeCoordinatorR26 runtime)throws Exception{
        runtime.reconcileResearchHealthR31("HOST_CONTROL_OBSERVATION");long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        AtomicBoolean pending=(AtomicBoolean)value(runtime,"healthProbePendingR31");while(pending.get()&&System.nanoTime()<end)Thread.sleep(2);need(!pending.get(),"bounded actual health controller probe completes");
        Thread.sleep(5);need(runtime.healthSnapshotR31().reason.indexOf("HEALTH_SOURCE_UNAVAILABLE")<0,"controller actual SQL read succeeds");
    }
    static void control()throws Exception{
        reset();Map<String,Object> absent=RuntimeCoordinatorR26.activeHealthObservationR31();need(absent.get("reason").equals("MAIN_RUNTIME_OWNER_NOT_IN_THIS_PID")&&absent.get("scope").equals("PER_PID"),"diagnostic PID absent owner is unknown, not main process death");
        CountDownLatch stop=new CountDownLatch(1);RuntimeCoordinatorR26 runtime=coordinator(stop);int before=db.reads.get();need(runtime.healthSnapshotR31().researchHealth.equals("UNKNOWN"),"unsampled cache getter unknown");need(db.reads.get()==before,"cache getter does not query");probe(runtime);before=db.reads.get();runtime.healthSnapshotR31();need(db.reads.get()==before,"sampled cache getter does not query");need(!Thread.currentThread().getName().equals(db.queryThread),"SQL health control runs outside caller/UI thread");
        db.execSQL("UPDATE r26_task SET status='RUNNING' WHERE task_id='incumbent-forecast'");db.execSQL("UPDATE r30r1_task_progress SET pending=3,last_commit_clock_ordinal=?,artifact_identity='HOST_CONTROL_OLD_ARTIFACT' WHERE task_id='incumbent-forecast'",new Object[]{LATEST-6});
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger bodies=new AtomicInteger();
        CooperativeContinuationR26<Long> work=new CooperativeContinuationR26<>("starcore-r26-incumbent-forecast",()->{bodies.incrementAndGet();entered.countDown();while(release.getCount()>0)try{release.await(5,TimeUnit.MILLISECONDS);}catch(InterruptedException intentionallyUncooperative){}return 1L;},()->false,()->{},System::nanoTime,OWNER);
        ExecutorService dispatcher=Executors.newSingleThreadExecutor();Future<?> slice=dispatcher.submit(()->{try{return work.slice(200);}catch(Exception error){throw new RuntimeException(error);}});
        try{
            need(entered.await(1,TimeUnit.SECONDS),"actual old owner calculation entered");probe(runtime);need(scalar("SELECT status FROM r26_task WHERE task_id='incumbent-forecast'").equals("RUNNING"),"first stalled observation only wakes existing owner");
            android.os.SystemClock.offset+=5000;probe(runtime);need(scalar("SELECT status FROM r26_task WHERE task_id='incumbent-forecast'").equals("CANCELLING"),"second unchanged observation revokes only observed generation");need(scalar("SELECT fence FROM r26_task WHERE task_id='incumbent-forecast'").equals("8"),"revoke advances fence once");need(!CooperativeContinuationR26.isTaskTerminatedR31(OWNER,"starcore-r26-incumbent-forecast"),"cancel request never means actual termination");
            probe(runtime);need(scalar("SELECT status FROM r26_task WHERE task_id='incumbent-forecast'").equals("CANCELLING")&&bodies.get()==1,"no replacement or READY while old thread remains alive");need(scalar("SELECT error_count FROM r26_task WHERE task_id='incumbent-forecast'").equals("0"),"controlled recovery consumes zero fault budget");
            release.countDown();need(work.awaitTermination(1000),"actual old thread exit observed");probe(runtime);need(scalar("SELECT status FROM r26_task WHERE task_id='incumbent-forecast'").equals("READY"),"only actual named thread termination permits resume");
            android.os.SystemClock.offset+=31000;probe(runtime);probe(runtime);need(scalar("SELECT fence FROM r26_task WHERE task_id='incumbent-forecast'").equals("8"),"same durable chunk is woken without repeated restart starvation");
            RuntimeCoordinatorR26.RuntimeHealthSnapshot snapshot=runtime.healthSnapshotR31();Map<?,?> control=(Map<?,?>)snapshot.asMap().get("control");need(control.get("result").toString().contains("NOT_RECOVERED_UNTIL_NEW_DURABLE_ARTIFACT"),"control wake never claims model recovery");need(control.containsKey("memory")&&control.containsKey("executor"),"actual memory budget and bounded executor consumed by production control");
            db.execSQL("UPDATE r30r1_task_progress SET pending=3,last_commit_clock_ordinal=?,artifact_identity='HOST_R27_STALLED_WITHOUT_POOL' WHERE task_id='r27-evolution'",new Object[]{LATEST-6});probe(runtime);probe(runtime);need(scalar("SELECT status FROM r26_task WHERE task_id='r27-evolution'").equals("READY"),"missing R27 pool cannot be recovered with wrong R26 worker identity");
            field(runtime,"resourceBlocked","MEMORY_EMERGENCY:HOST_CONTROLLED");db.execSQL("UPDATE r30r1_task_progress SET pending=4,last_commit_clock_ordinal=?,artifact_identity='HOST_RESOURCE_PAUSED' WHERE task_id='kernel'",new Object[]{LATEST-6});probe(runtime);probe(runtime);need(scalar("SELECT fence FROM r26_task WHERE task_id='kernel'").equals("7"),"resource-paused work never causes recovery revoke churn");
            android.os.SystemClock.offset+=11000;need(!runtime.healthSnapshotR31().pendingKnown,"older than two ticks is stale with unknown aggregate pending");
            field(runtime,"resourceBlocked",null);db.healthFailure=new LinkageError("HOST_HEALTH_FATAL_INJECTION");probe(runtime);need(runtime.healthSnapshotR31().reason.startsWith("HEALTH_CONTROL_FATAL")&&runtime.healthSnapshotR31().researchHealth.equals("UNKNOWN"),"executor-captured fatal cannot leave a previously healthy cache or fabricate task fault/recovery");db.healthFailure=null;
            // R31-B04-C01#A2: 20 repeated start/recovery requests while recovery is in progress keep one owner and one pool generation.
            String epoch=runtime.healthSnapshotR31().ownerEpoch,generation=runtime.healthSnapshotR31().poolGeneration;
            for(int i=0;i<20;i++){runtime.reconcileResearchHealthR31("HOST_REPEATED_START_COMMAND_"+i);android.os.SystemClock.offset+=250;}
            Thread.sleep(200);
            need(epoch.equals(runtime.healthSnapshotR31().ownerEpoch)&&generation.equals(runtime.healthSnapshotR31().poolGeneration),"20 repeated start requests keep one owner epoch and one pool generation");
            need(scalar("SELECT count(*) FROM r26_session").equals("1")&&scalar("SELECT count(DISTINCT owner) FROM r26_task WHERE owner IS NOT NULL").compareTo("1")<=0,"repeated start requests never create a second session owner");
            field(runtime,"cancelled",true);int stoppedReads=db.reads.get();runtime.reconcileResearchHealthR31("HOST_LATE_REQUEST_AFTER_OWNER_STOP");Thread.sleep(10);need(db.reads.get()==stoppedReads&&!((AtomicBoolean)value(runtime,"healthProbePendingR31")).get(),"stopped owner cannot enqueue a late health query against a released store");
        }finally{release.countDown();work.close();work.awaitTermination(1000);slice.get(2,TimeUnit.SECONDS);dispatcher.shutdownNow();dispatcher.awaitTermination(1,TimeUnit.SECONDS);stop.countDown();((Thread)value(runtime,"worker")).join(1000);}
        System.out.println("R31_HEALTH_CONTROL_NATIVE_HOST_PASS cachedGetterNoQuery=true async=true twoObservations=true actualThreadDrain=true sameOwnerFenceAdvanced=true resourcePauseNoRevoke=true");
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");File file=new File(args[0]);Field unsafeField=Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);unsafe=(Unsafe)unsafeField.get(null);
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+file)){
            db=new TraceDb(connection);db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER)");
            db.execSQL("CREATE TABLE r26_task(task_id TEXT PRIMARY KEY,lane TEXT,mechanism TEXT,status TEXT,owner TEXT,fence INTEGER DEFAULT 0,cursor INTEGER DEFAULT 0,lease_until INTEGER DEFAULT 0,last_served INTEGER DEFAULT 0,error_window_start INTEGER DEFAULT 0,error_count INTEGER DEFAULT 0,wait_kind TEXT DEFAULT '',wait_detail TEXT DEFAULT '',blocked_reason TEXT,wait_since INTEGER DEFAULT 0,registered_at INTEGER DEFAULT 1)");
            db.execSQL("CREATE TABLE r26_session(session_id TEXT PRIMARY KEY,user_enabled INTEGER,state TEXT)");db.execSQL("INSERT INTO r26_session VALUES(?,1,'RUNNING')",new Object[]{OWNER});
            db.execSQL("CREATE TABLE r30r1_task_progress(task_id TEXT PRIMARY KEY,session_id TEXT,fence INTEGER,owner_version TEXT,work_unit TEXT,work_cursor INTEGER,known_latest INTEGER,pending INTEGER,last_commit_clock_ordinal INTEGER,completed_input_ordinal INTEGER,artifact_identity TEXT,updated_at INTEGER)");
            db.beginTransaction();try{SchemaR27.ensureR31Observations(db);SchemaR27.ensureR31WriteProtocol(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            coordinator=(DatabaseCoordinator)unsafe.allocateInstance(DatabaseCoordinator.class);Field writeLock=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");writeLock.setAccessible(true);field(coordinator,"writeLock",(ReentrantLock)writeLock.get(null));field(coordinator,"helper",new DbHelper(db));field(coordinator,"app",new android.app.Application(file));field(coordinator,"waitingWriters",new AtomicInteger());field(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());store=new DbStore(coordinator,db,new android.app.Application(file));
            facts();control();System.out.println("R31_RUNTIME_HEALTH_HOST_PASS assertions="+checks+" scope=ACTUAL_PRODUCTION_QUERY_AND_CONTROL_NATIVE_SQLITE_HOST_FIXTURES_NOT_ANDROID_NOT_DEVICE_NOT_SOAK_NOT_NATURAL_QUALITY");
        }
    }
}
