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

/** Production methods, native SQLite and controlled clock/fault adapters. No device or quality assertion. */
public final class R31TaskRuntimeHost {
    static SQLiteDatabase db;static DatabaseCoordinator coordinator;static DbStore store;static ResearchPoolR27 pool;
    static ReentrantLock writer;static AtomicInteger assertions=new AtomicInteger();static Method record,claimPrepared;
    static void need(boolean condition,String code){assertions.incrementAndGet();if(!condition)throw new AssertionError(code);}
    static void field(Object target,String name,Object value)throws Exception{Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
    static long n(String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static String text(String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getString(0);}}
    interface Tx<T>{T call()throws Exception;}
    static <T>T tx(Tx<T> action)throws Exception{db.beginTransaction();try{T result=action.call();db.setTransactionSuccessful();return result;}finally{db.endTransaction();}}
    static void add(String id){db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,status,registered_at) VALUES(?,'R27',?,'READY',1)",new Object[]{id,id});}
    static RuntimeCoordinatorR26.Claim claim(String task,String owner)throws Exception{return tx(()->RuntimeCoordinatorR26.claimWithinR31(db,task,owner,"R27",null));}
    static void receipt(RuntimeCoordinatorR26.Claim claim,Throwable failure,long units)throws Exception{record.invoke(pool,claim.task,new ResearchPoolR27.Outcome(units,units>0?null:"WAITING_NEW_DRAW",claim.cursor,-1),failure,null,claim,"HOST_INJECTED",1L);}
    static void staleAndClock()throws Exception{
        add("host-fenced");RuntimeCoordinatorR26.Claim old=claim("host-fenced","owner-A");need(old!=null,"initial claim");
        db.execSQL("UPDATE r26_task SET lease_until=-86400000 WHERE task_id='host-fenced'");
        tx(()->{RuntimeCoordinatorR26.requireFence(db,old);return 0;});need(claim("host-fenced","owner-B")==null,"wall deadline rollback cannot grant owner-B");
        db.execSQL("UPDATE r26_task SET lease_until=9223372036854775807 WHERE task_id='host-fenced'");
        android.os.SystemClock.offset+=31_000;
        RuntimeCoordinatorR26.Claim fresh=claim("host-fenced","owner-B");need(fresh!=null&&fresh.fence>old.fence,"elapsed expiry ignores far future wall deadline");
        receipt(old,new IllegalStateException("HOST_OLD_FAILURE"),0);receipt(old,null,1);
        need(text("SELECT owner FROM r26_task WHERE task_id='host-fenced'").equals("owner-B"),"old failure/success cannot overwrite owner");
        need(text("SELECT status FROM r26_task WHERE task_id='host-fenced'").equals("RUNNING"),"old receipt cannot isolate new owner");
        need(n("SELECT error_count FROM r26_task WHERE task_id='host-fenced'")==0,"old receipt cannot consume new fault budget");
        tx(()->{RuntimeCoordinatorR26.requireFence(db,fresh);return 0;});
        db.execSQL("UPDATE r31_task_lease SET boot_id='OLD_BOOT' WHERE task_id='host-fenced'");
        boolean rejected=false;try{tx(()->{RuntimeCoordinatorR26.requireFence(db,fresh);return 0;});}catch(IllegalStateException expected){rejected=expected.getMessage().startsWith("R26_STALE_FENCE");}
        need(rejected,"cross-boot elapsed values never authorize old claim");
        need(old.nextCommitChunkR31()==0&&old.nextCommitChunkR31()==1,"chunk sequence increments once per logical submit");
        RuntimeCoordinatorR26.Claim next=new RuntimeCoordinatorR26.Claim(old.task,old.owner,old.fence+1,old.cursor,"R27");next.inheritCommitChunksR31(old);need(next.nextCommitChunkR31()==2,"continuation renew inherits logical sequence");
        add("host-health-cas");RuntimeCoordinatorR26.Claim health=claim("host-health-cas","owner-A");
        db.execSQL("INSERT INTO r30r1_task_progress(task_id,session_id,fence,artifact_identity) VALUES('host-health-cas','owner-A',?,'HOST_CURRENT_ARTIFACT')",new Object[]{health.fence});
        need(!pool.requestTaskRecoveryR31(health.task,health.fence,"HOST_OLDER_ARTIFACT","HOST_STALL"),"health observation cannot revoke a task that published a newer artifact");
        need(pool.requestTaskRecoveryR31(health.task,health.fence,"HOST_CURRENT_ARTIFACT","HOST_STALL"),"matching health owner fence and artifact permit one controlled revoke");
        need(!pool.requestTaskRecoveryR31(health.task,health.fence,"HOST_CURRENT_ARTIFACT","HOST_LATE_OBSERVATION"),"old control observation cannot revoke new fence");
        db.execSQL("UPDATE r26_task SET owner='owner-B',fence=?,status='CANCELLING' WHERE task_id='host-health-cas'",new Object[]{health.fence+2});
        Method drained=ResearchPoolR27.class.getDeclaredMethod("finishCancelledR31",RuntimeCoordinatorR26.Claim.class);drained.setAccessible(true);drained.invoke(pool,health);
        need(text("SELECT status FROM r26_task WHERE task_id='host-health-cas'").equals("CANCELLING"),"late old-owner drain receipt cannot ready a newer owner");
        System.out.println("R31_TASK_FENCE_NATIVE_HOST_PASS late_success_and_failure_ignored=true wall_deadline_plus_minus_24h_ignored=true boot_mismatch_rejected=true");
    }
    static void restartR31(String task){db.execSQL("UPDATE r31_task_lease SET boot_id='OLD_BOOT' WHERE task_id=?",new Object[]{task});}
    static void publishR31(RuntimeCoordinatorR26.Claim claim,long seq)throws Exception{
        tx(()->{RuntimeCoordinatorR26.requireFence(db,claim);
            db.execSQL("INSERT INTO host_artifact(task_id,seq,fence) VALUES(?,?,?)",new Object[]{claim.task,seq,claim.fence});
            db.execSQL("UPDATE r26_task SET cursor=? WHERE task_id=? AND owner=? AND fence=? AND cursor=?",new Object[]{seq,claim.task,claim.owner,claim.fence,seq-1});
            need(n("SELECT changes()")==1,"fenced cursor CAS advances exactly once");return 0;});
    }
    /** R31-B01-C02#A2: process death after claim, before commit and after commit before receipt. */
    static void killPointsPublishExactlyOnce()throws Exception{
        db.execSQL("CREATE TABLE host_artifact(task_id TEXT,seq INTEGER,fence INTEGER,PRIMARY KEY(task_id,seq))");
        add("host-kill");String task="host-kill";
        RuntimeCoordinatorR26.Claim a=claim(task,"owner-A");need(a!=null&&a.cursor==0,"initial claim at durable cursor 0");
        restartR31(task);
        RuntimeCoordinatorR26.Claim b=claim(task,"owner-B");need(b!=null&&b.fence>a.fence&&b.cursor==0,"death after claim: recovery starts from durable cursor");
        boolean staleRejected=false;try{publishR31(a,1);}catch(IllegalStateException expected){staleRejected=expected.getMessage().startsWith("R26_STALE_FENCE");}
        need(staleRejected,"pre-restart claim cannot publish");
        publishR31(b,b.cursor+1);
        boolean killed=false;
        try{tx(()->{RuntimeCoordinatorR26.requireFence(db,b);
            db.execSQL("INSERT INTO host_artifact(task_id,seq,fence) VALUES(?,?,?)",new Object[]{task,2,b.fence});
            db.execSQL("UPDATE r26_task SET cursor=2 WHERE task_id=? AND owner=? AND fence=?",new Object[]{task,b.owner,b.fence});
            throw new IllegalStateException("HOST_KILL_BEFORE_COMMIT");});}
        catch(IllegalStateException expected){killed="HOST_KILL_BEFORE_COMMIT".equals(expected.getMessage());}
        need(killed&&n("SELECT cursor FROM r26_task WHERE task_id='host-kill'")==1&&n("SELECT count(*) FROM host_artifact WHERE task_id='host-kill'")==1,"death before commit leaves nothing durable");
        restartR31(task);
        RuntimeCoordinatorR26.Claim c=claim(task,"owner-A");need(c!=null&&c.fence>b.fence&&c.cursor==1,"death before commit repeats only the uncommitted step");
        publishR31(c,c.cursor+1);
        restartR31(task);
        RuntimeCoordinatorR26.Claim d=claim(task,"owner-B");need(d!=null&&d.fence>c.fence&&d.cursor==2,"death after commit before receipt does not repeat the committed step");
        receipt(c,null,1);
        need(text("SELECT owner FROM r26_task WHERE task_id='host-kill'").equals("owner-B")&&text("SELECT status FROM r26_task WHERE task_id='host-kill'").equals("RUNNING"),"late receipt of the killed owner is ignored");
        publishR31(d,d.cursor+1);
        need(n("SELECT count(*) FROM host_artifact WHERE task_id='host-kill'")==3&&n("SELECT count(DISTINCT seq) FROM host_artifact WHERE task_id='host-kill'")==3
                &&n("SELECT max(seq) FROM host_artifact WHERE task_id='host-kill'")==3&&n("SELECT cursor FROM r26_task WHERE task_id='host-kill'")==3,"artifact and cursor published exactly once per step");
        need(n("SELECT error_count FROM r26_task WHERE task_id='host-kill'")==0,"process deaths consume no fault budget");
        System.out.println("R31_KILL_POINT_NATIVE_HOST_PASS after_claim=true before_commit=true after_commit_before_receipt=true artifacts=3 cursor=3 duplicates=0 scope=HOST_RESTART_SIMULATED_BY_BOOT_EPOCH");
    }
    /** R31-B01-C02#A4: an owner change makes the old prepared claim terminal (STALE_OWNER), never an endless retry. */
    static void ownerChangeIsTerminalForPrepared()throws Exception{
        String task="host-owner-change";add(task);
        RuntimeCoordinatorR26.Claim a=claim(task,"owner-A");need(a!=null,"owner-A claim");
        restartR31(task);RuntimeCoordinatorR26.Claim b=claim(task,"owner-B");need(b!=null&&b.fence>a.fence,"owner-B takes over");
        for(int i=0;i<50;i++){
            IllegalStateException stale=null;
            try{tx(()->RuntimeCoordinatorR26.claimWithinR31(db,task,"owner-A","R27",a));}catch(IllegalStateException expected){stale=expected;}
            need(stale!=null&&FailureDispositionR31.classify(stale).disposition==FailureDispositionR31.Disposition.STALE_OWNER,"old prepared claim is STALE_OWNER "+i);
            need(!ResearchWriteQueueR27.writerBusy(stale),"stale owner is not a busy retry "+i);
            receipt(a,stale,0);
            need(tx(()->RuntimeCoordinatorR26.claimWithinR31(db,task,"owner-A","R27",null))==null,"fresh claim waits while the new owner holds the lease "+i);
        }
        need(text("SELECT owner FROM r26_task WHERE task_id='host-owner-change'").equals("owner-B")&&text("SELECT status FROM r26_task WHERE task_id='host-owner-change'").equals("RUNNING"),"new owner keeps RUNNING");
        need(n("SELECT fence FROM r26_task WHERE task_id='host-owner-change'")==b.fence,"stale attempts never advance the fence");
        need(n("SELECT error_count FROM r26_task WHERE task_id='host-owner-change'")==0,"stale attempts consume no fault budget");
        System.out.println("R31_OWNER_CHANGE_NATIVE_HOST_PASS stale_prepared_terminal=true attempts=50 fence_stable=true faults=0");
    }
    static void classificationsAndRecovery()throws Exception{
        class Typed extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure{
            final String category,outcome;Typed(String category,String outcome){super("HOST_TYPED");this.category=category;this.outcome=outcome;}
            public String failureCategory(){return category;}public String operation(){return "HOST";}public String phase(){return "BODY";}public String commitOutcomeName(){return outcome;}
        }
        for(String category:new String[]{"LOCK_BUSY","TEMP_TIMEOUT"})need(FailureDispositionR31.classify(new Typed(category,"ROLLED_BACK")).disposition==FailureDispositionR31.Disposition.RETRY_LATER,"transient "+category);
        need(FailureDispositionR31.classify(new Typed("LOCK_BUSY","UNKNOWN")).requiresCommitResolution,"unknown commit dominates busy cause");
        need(FailureDispositionR31.classify(new Typed("BUG","COMMITTED_DIAGNOSTIC_FAILED")).alreadyCommitted,"diagnostic failure does not redo committed work");
        for(String category:new String[]{"DISK_FULL","DB_CORRUPT"})need(FailureDispositionR31.classify(new Typed(category,"ROLLED_BACK")).disposition==FailureDispositionR31.Disposition.FATAL,"fatal storage is not busy");
        add("host-cancel");for(int i=0;i<100;i++){android.os.SystemClock.offset+=1100;RuntimeCoordinatorR26.Claim claim=claim("host-cancel","owner-A");need(claim!=null,"cancel claim "+i);receipt(claim,new InterruptedException("HOST_USER_CANCEL"),0);}
        need(n("SELECT error_count FROM r26_task WHERE task_id='host-cancel'")==0,"100 cancellations consume zero fault budget");
        need(!text("SELECT status FROM r26_task WHERE task_id='host-cancel'").equals("QUARANTINED"),"cancellation never permanently isolates");
        add("host-fault");
        for(int i=0;i<6;i++){android.os.SystemClock.offset+=1100;RuntimeCoordinatorR26.Claim claim=claim("host-fault","owner-A");need(claim!=null,"bug claim "+i);receipt(claim,new IllegalStateException("HOST_REPEAT_BUG"),0);}
        need(text("SELECT status FROM r26_task WHERE task_id='host-fault'").equals("QUARANTINED"),"genuine sixth fault quarantines");
        need(n("SELECT level FROM r31_task_control WHERE task_id='host-fault'")==1,"first finite quarantine level");
        for(int level=2;level<=6;level++){
            android.os.SystemClock.offset+=2000;db.execSQL("UPDATE r31_task_control SET quarantine_deadline_elapsed_ms=0 WHERE task_id='host-fault'");
            ResearchPoolR27.recoverOneR31(store,"owner-A");RuntimeCoordinatorR26.Claim probe=claim("host-fault","owner-A");need(probe!=null,"unique half-open probe "+level);receipt(probe,new IllegalStateException("HOST_REPEAT_BUG"),0);
        }
        need(text("SELECT status FROM r26_task WHERE task_id='host-fault'").equals("NEEDS_FIX"),"six same-root rounds require code repair");
        add("host-progress");RuntimeCoordinatorR26.Claim progress=claim("host-progress","owner-A");
        db.execSQL("INSERT INTO r31_task_control(task_id,level,fault_rounds,half_open_owner,half_open_fence) VALUES('host-progress',3,3,'owner-A',?)",new Object[]{progress.fence});
        receipt(progress,null,1);need(n("SELECT level FROM r31_task_control WHERE task_id='host-progress'")==3,"work units alone do not claim recovered artifact");
        android.os.SystemClock.offset+=1100;progress=claim("host-progress","owner-A");
        db.execSQL("INSERT INTO r30r1_task_progress(task_id,session_id,fence,artifact_identity) VALUES('host-progress','owner-A',?,'HOST_CHECKPOINT_HASH')",new Object[]{progress.fence});
        receipt(progress,null,1);need(n("SELECT level FROM r31_task_control WHERE task_id='host-progress'")==0,"real same-fence artifact resets recovery level");
        add("host-recovery-a");add("host-recovery-b");
        for(String id:new String[]{"host-recovery-a","host-recovery-b"}){db.execSQL("UPDATE r26_task SET status='QUARANTINED' WHERE task_id=?",new Object[]{id});db.execSQL("INSERT INTO r31_task_control(task_id,level,quarantine_boot_id,quarantine_deadline_elapsed_ms) VALUES(?,1,?,0)",new Object[]{id,IssueTimelineWriterR30.bootId()});}
        android.os.SystemClock.offset+=2000;ResearchPoolR27.recoverOneR31(store,"owner-A");ResearchPoolR27.recoverOneR31(store,"owner-A");
        need(n("SELECT count(*) FROM r26_task WHERE task_id LIKE 'host-recovery-%' AND status='READY'")==1,"global half-open limiter admits one per second");
        System.out.println("R31_TASK_RECOVERY_NATIVE_HOST_PASS cancel100_fault0=true finite_quarantine=true one_probe_per_second=true same_root_six_rounds_NEEDS_FIX=true actual_artifact_required=true");
    }
    static void busyThirteen()throws Exception{
        for(int i=0;i<13;i++)add("host-busy-"+i);
        CountDownLatch locked=new CountDownLatch(1);Thread owner=new Thread(()->{writer.lock();locked.countDown();try{try{Thread.sleep(3000);}catch(InterruptedException ignored){}}finally{writer.unlock();}});owner.start();locked.await();
        ExecutorService clients=Executors.newFixedThreadPool(13);List<Future<?>> results=new ArrayList<>();
        for(int i=0;i<13;i++){String id="host-busy-"+i;results.add(clients.submit(()->{try{claimPrepared.invoke(pool,id,null);throw new AssertionError("lock held but claim succeeded");}catch(InvocationTargetException expected){need(FailureDispositionR31.classify(expected.getCause()).disposition==FailureDispositionR31.Disposition.RETRY_LATER,"actual claim busy classified retry");try{record.invoke(pool,id,null,expected.getCause(),null,null,"CLAIM",0L);}catch(Exception failed){throw new RuntimeException(failed);}}catch(Exception failure){throw new RuntimeException(failure);}}));}
        for(Future<?> result:results)result.get();clients.shutdown();owner.join();
        need(n("SELECT sum(error_count) FROM r26_task WHERE task_id LIKE 'host-busy-%'")==0,"13 real lock-rejected claims add no faults");
        need(n("SELECT count(*) FROM r26_task WHERE task_id LIKE 'host-busy-%' AND status='QUARANTINED'")==0,"13 real busy claims do not quarantine");
        for(int i=0;i<13;i++)need(claim("host-busy-"+i,"owner-A")!=null,"lock released task resumes "+i);
        System.out.println("R31_THIRTEEN_ACTUAL_CLAIMS_LOCK3S_NATIVE_HOST_PASS faults=0 quarantined=0 resumed=13 scope=HOST_WRITER_LOCK_NOT_ANDROID_SLO");
    }
    static void helperTermination()throws Exception{
        RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("host-helper","owner-A",99,0,"R27");
        MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();long beforeLeases=((Number)memory.snapshot().get("activeLeaseCount")).longValue(),beforeBytes=((Number)memory.snapshot().get("declaredJavaWorkingBytes")).longValue();
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<Future<?>> future=new AtomicReference<>();
        ResearchPoolR27.Worker worker=pool.new Worker(db,"HOST_HELPER_OWNER");
        PreparedTaskR30R1 prepared=new PreparedTaskR30R1(new ResearchPoolR27.Task(){public String id(){return "host-helper";}public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker ignored){future.set(pool.compute().submit(()->{started.countDown();while(release.getCount()>0)try{release.await(5,TimeUnit.MILLISECONDS);}catch(InterruptedException deliberatelyUncooperative){};}));return ResearchPoolR27.Outcome.progress(1,1);}},worker,claim);
        prepared.slice(200);need(started.await(1,TimeUnit.SECONDS),"helper actual body started");
        prepared.close();need(future.get().isCancelled(),"future cancel accepted");
        need(!prepared.isTerminated(),"cancelled future and exited parent do not prove helper termination");
        need(((Number)ResearchPoolR27.computeBudgetSnapshotR31().get("activeHelpers")).intValue()==1,"uncooperative helper still owned and budgeted");
        need(((Number)memory.snapshot().get("activeLeaseCount")).longValue()>=beforeLeases+3,"prepared descriptor and two helper working/descriptor leases retained after Future cancel");
        need(((Number)memory.snapshot().get("declaredJavaWorkingBytes")).longValue()>=beforeBytes+ResearchPoolR27.HELPER_REPLAY_WORKING_ESTIMATE_R31,"actual admitted replay working estimate remains owned until body finally");
        need(!prepared.awaitTermination(25),"bounded helper drain returns pending");
        long closeStarted=System.nanoTime();pool.close();need((System.nanoTime()-closeStarted)/1_000_000<100,"close requests return without waiting for uncooperative helper");
        need(!pool.resourcesTerminatedR31(),"pool cannot declare terminated while owned helper body is alive");
        release.countDown();need(prepared.awaitTermination(1000),"actual helper exit releases prepared ownership");
        ResearchPoolR27.clearTaskHelperCancellationR31(claim);need(((Number)ResearchPoolR27.computeBudgetSnapshotR31().get("activeHelpers")).intValue()==0,"actual finally clears active helper");
        need(((Number)memory.snapshot().get("activeLeaseCount")).longValue()==beforeLeases,"actual finally releases exactly the registered prepared/helper leases");
        need(((Number)memory.snapshot().get("declaredJavaWorkingBytes")).longValue()==beforeBytes,"declared bytes decrease after actual resource release, not cancellation alone");
        pool.close();need(pool.awaitResourcesTerminatedR31(2000),"pool controller releases only after actual resource exit");
        System.out.println("R31_HELPER_TERMINATION_HOST_PASS cancelled_future_not_exit=true parent_exit_not_group_exit=true permit_retained_until_real_finally=true bounded_drain=true");
    }
    static void actualPoolLoopRetainsHelperOwner()throws Exception{
        ResearchPoolR27 loopPool=new ResearchPoolR27(store,"owner-B",1);AtomicInteger starts=new AtomicInteger();
        CountDownLatch helperStarted=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<Future<?>> helper=new AtomicReference<>();
        loopPool.register(new ResearchPoolR27.Task(){public String id(){return "host-loop-retain";}public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker worker){
            if(starts.incrementAndGet()==1)helper.set(loopPool.compute().submit(()->{helperStarted.countDown();while(release.getCount()>0)try{release.await(5,TimeUnit.MILLISECONDS);}catch(InterruptedException deliberatelyUncooperative){};}));
            return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT:HOST_HELPER_GUARD",0,0);
        }});
        loopPool.start();need(helperStarted.await(2,TimeUnit.SECONDS),"actual pool loop created parent and owned helper");
        Thread.sleep(1300);need(starts.get()==1,"actual scheduler does not replace parent after 50ms cleanup budget while helper lives");
        ResearchPoolR27.retry(store,"host-loop-retain");need(helper.get().isCancelled(),"manual retry signals old helper cancellation");
        Thread.sleep(100);need(starts.get()==1,"manual retry cannot start replacement during uncooperative helper");
        need(!text("SELECT status FROM r26_task WHERE task_id='host-loop-retain'").equals("READY"),"old live helper retains draining state");
        release.countDown();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(starts.get()<2&&System.nanoTime()<end)Thread.sleep(10);
        need(starts.get()>=2,"actual task resumes after real helper termination");
        loopPool.close();need(loopPool.awaitResourcesTerminatedR31(2000),"actual worker and read connection drain");
        System.out.println("R31_ACTUAL_POOL_LOOP_HELPER_RACE_HOST_PASS no_replacement_after_parent_done=true manual_retry_waits_for_helper=true resumes_after_actual_exit=true");
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");File file=new File(args[0]);
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+file)){
            db=new SQLiteDatabase(connection);
            boolean nullStringRejected=false;
            try(Cursor ignored=db.rawQuery("SELECT ? IS NULL",new String[]{null})){
                throw new AssertionError("HOST rawQuery accepted a null String binding rejected by Android");
            }catch(IllegalArgumentException expected){nullStringRejected=expected.getMessage().contains("index 1 is null");}
            need(nullStringRejected,"Android bindString rejects null selectionArgs");
            for(String owner:new String[]{"","null"})try(Cursor row=db.rawQuery("SELECT ? IS NULL",new String[]{owner})){
                need(row.moveToFirst()&&row.getInt(0)==0,"non-null owner strings must not become SQL NULL");
            }
            db.execSQL("CREATE TABLE r26_task(task_id TEXT PRIMARY KEY,lane TEXT,mechanism TEXT,status TEXT,owner TEXT,fence INTEGER DEFAULT 0,cursor INTEGER DEFAULT 0,lease_until INTEGER DEFAULT 0,last_served INTEGER DEFAULT 0,error_window_start INTEGER DEFAULT 0,error_count INTEGER DEFAULT 0,wait_kind TEXT DEFAULT '',wait_detail TEXT DEFAULT '',blocked_reason TEXT,wait_since INTEGER DEFAULT 0,registered_at INTEGER DEFAULT 1)");
            db.execSQL("CREATE TABLE r26_session(session_id TEXT PRIMARY KEY,user_enabled INTEGER,state TEXT)");db.execSQL("INSERT INTO r26_session VALUES('owner-A',1,'RUNNING'),('owner-B',1,'RUNNING')");
            db.execSQL("CREATE TABLE r27_task_error(error_id INTEGER PRIMARY KEY,task_id TEXT,error_class TEXT,message TEXT,stack TEXT,recorded_at INTEGER)");
            db.execSQL("CREATE TABLE r30r1_task_progress(task_id TEXT PRIMARY KEY,session_id TEXT,fence INTEGER,artifact_identity TEXT)");
            for(String name:new String[]{"r30r1_analysis_run","r30r1_research_measure","r30r1_proposal","r30r1_consume","r30r1_chart_cache"})db.execSQL("CREATE TABLE "+name+"(id INTEGER)");
            db.execSQL("INSERT INTO r26_task(task_id,lane,status,error_count) VALUES('host-inherited-quarantine','R27','QUARANTINED',6)");
            tx(()->{SchemaR27.ensureR31Observations(db);SchemaR27.ensureR31WriteProtocol(db);SchemaR27.ensureR31WriteProtocol(db);return 0;});
            need(n("SELECT count(*) FROM r31_task_control WHERE task_id='host-inherited-quarantine' AND level=1")==1,"upgrade schedules exactly one finite inherited quarantine recovery");
            need(n("SELECT error_count FROM r26_task WHERE task_id='host-inherited-quarantine'")==6,"upgrade preserves original fault history");
            need(text("SELECT status FROM r26_task WHERE task_id='host-inherited-quarantine'").equals("QUARANTINED"),"migration does not bulk-release old quarantines");
            // Remove only this isolated host fixture after checking preservation, before clock-driven recovery cases.
            db.execSQL("DELETE FROM r31_task_control WHERE task_id='host-inherited-quarantine'");db.execSQL("DELETE FROM r26_task WHERE task_id='host-inherited-quarantine'");
            Field unsafeField=Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Unsafe unsafe=(Unsafe)unsafeField.get(null);coordinator=(DatabaseCoordinator)unsafe.allocateInstance(DatabaseCoordinator.class);
            Field lock=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lock.setAccessible(true);writer=(ReentrantLock)lock.get(null);
            field(coordinator,"writeLock",writer);field(coordinator,"helper",new DbHelper(db));field(coordinator,"app",new android.app.Application(file));field(coordinator,"waitingWriters",new AtomicInteger());field(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());
            store=new DbStore(coordinator,db,new android.app.Application(file));pool=new ResearchPoolR27(store,"owner-A",2);
            record=Arrays.stream(ResearchPoolR27.class.getDeclaredMethods()).filter(method->method.getName().equals("record")).findFirst().orElseThrow();record.setAccessible(true);
            claimPrepared=Arrays.stream(ResearchPoolR27.class.getDeclaredMethods()).filter(method->method.getName().equals("claimPrepared")).findFirst().orElseThrow();claimPrepared.setAccessible(true);
            staleAndClock();killPointsPublishExactlyOnce();ownerChangeIsTerminalForPrepared();classificationsAndRecovery();busyThirteen();actualPoolLoopRetainsHelperOwner();helperTermination();
            System.out.println("R31_TASK_RUNTIME_HOST_PASS assertions="+assertions+" scope=ACTUAL_PRODUCTION_METHODS_NATIVE_SQLITE_WITH_HOST_FAULT_CLOCK_PLATFORM_ADAPTERS_NOT_ANDROID_NOT_DEVICE_NOT_24H_NOT_72H_NOT_QUALITY");
        }
    }
}
