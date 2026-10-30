package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Real coordinator/claim/schema code and the existing real SQLite event fixture. HOST-only.
 * Writer contention is explicit fault injection, not an Android latency measurement. */
public final class ColdStartFairnessR29HostTest {
    static int checks;
    static void check(String id,boolean ok){System.out.println("R29_COLD_START_FAIRNESS|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static String causes(Throwable e){StringBuilder out=new StringBuilder();for(int i=0;e!=null&&i<20;i++,e=e.getCause())out.append(e.getMessage()).append(';');return out.toString();}
    static void field(Object target,String name,Object value)throws Exception{Field f=target instanceof Class?((Class<?>)target).getDeclaredField(name):target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target instanceof Class?null:target,value);}
    static Object field(Object target,String name)throws Exception{Field f=target instanceof Class?((Class<?>)target).getDeclaredField(name):target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target instanceof Class?null:target);}
    static Object call(Object target,String name,Class<?>[] types,Object...args)throws Exception{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);try{return m.invoke(target,args);}catch(InvocationTargetException failure){Throwable cause=failure.getCause();if(cause instanceof Exception)throw (Exception)cause;if(cause instanceof Error)throw (Error)cause;throw failure;}}
    static RuntimeCoordinatorR26 runtime(EventYieldR29HostTest.Fixture f)throws Exception{
        RuntimeCoordinatorR26 r=(RuntimeCoordinatorR26)EventYieldR29HostTest.allocate(RuntimeCoordinatorR26.class);
        field(r,"store",f.store);field(r,"owner","SYNTHETIC_COLD_START");field(r,"signal",new Object());field(r,"r27",f.runtime);
        f.root.beginTransaction();try{
            LegacyCapabilityAdapterR29.initialize(f.root,"SYNTHETIC_COLD_START");
            f.root.execSQL("CREATE TABLE r26_task(task_id TEXT PRIMARY KEY,lane TEXT NOT NULL,mechanism TEXT NOT NULL,cursor INTEGER NOT NULL DEFAULT 0,fence INTEGER NOT NULL DEFAULT 0,lease_until INTEGER NOT NULL DEFAULT 0,owner TEXT,status TEXT NOT NULL DEFAULT 'READY',blocked_reason TEXT,next_at INTEGER NOT NULL DEFAULT 0,last_served INTEGER NOT NULL DEFAULT 0)");
            f.root.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,cursor,fence) VALUES('schema','P1','schema',5,7)");
            f.root.execSQL("INSERT INTO r26_task(task_id,lane,mechanism) VALUES('unready-numerical-task','P2','synthetic')");
            f.root.setTransactionSuccessful();
        }finally{f.root.endTransaction();}
        return r;
    }
    static Object claim(RuntimeCoordinatorR26 runtime)throws Exception{return call(runtime,"claim",new Class<?>[0]);}
    static long execute(RuntimeCoordinatorR26 runtime,long budget)throws Exception{return (Long)call(runtime,"execute",new Class<?>[]{RuntimeCoordinatorR26.Claim.class,BudgetTokenSU1.class},new RuntimeCoordinatorR26.Claim("schema","SYNTHETIC_COLD_START",8,5,"P1"),new BudgetTokenSU1(budget,3000000,()->false));}
    public static void main(String[]args)throws Exception{
        String mode=args.length==0?"":args[0];Object old=field(RuntimeCoordinatorR26.class,"active");
        try(EventYieldR29HostTest.Fixture f=new EventYieldR29HostTest.Fixture()){
            RuntimeCoordinatorR26 r=runtime(f);field(RuntimeCoordinatorR26.class,"active",r);
            if(mode.isEmpty()||mode.equals("--old-overlay-control")){
                AtomicInteger writerQueries=new AtomicInteger();SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(db==f.root&&db.inTransaction())writerQueries.incrementAndGet();};
                ResearchSchemaCoordinatorR926.StepResult out;try{out=ResearchSchemaCoordinatorR926.ensureOneStepResult(f.coordinator);}finally{SQLiteDatabase.hostBeforeQuery=null;}
                check("C1_OPTIONAL_OVERLAY_WAITS_OUTSIDE_ROOT_WRITER",out.pending&&!out.enteredWriter&&out.cause==null&&out.committedUnits==0&&writerQueries.get()==0&&out.status.equals("WAITING_FOR_STRATEGY_STORAGE"));
                if(!mode.isEmpty())return;
            }
            if(mode.isEmpty()||mode.equals("--old-migration-control")){
                RuntimeR27.MigrationTask task=f.runtime.new MigrationTask();ResearchPoolR27.Outcome out=task.step(f.worker);
                check("C2_MIGRATION_DOES_NOT_CREATE_STAGE_OR_ROOT_TRACKERS_BEFORE_DEPENDENCY",out.units==0&&out.reason.equals("DEPENDENCY:WAITING_SELF_CONTAINED_STRATEGY_STORAGE")&&field(task,"stage")==null&&EventYieldR29HostTest.n(f.root,"SELECT count(*) FROM sqlite_master WHERE name LIKE 'r29_source_%'")==0);
                if(!mode.isEmpty())return;
            }
            if(mode.isEmpty()){
                f.root.execSQL("UPDATE r26_task SET next_at=? WHERE task_id='schema'",new Object[]{System.currentTimeMillis()+60000});AtomicInteger admissions=new AtomicInteger();SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(db==f.root&&sql.startsWith("SELECT fence,cursor FROM r26_task"))admissions.incrementAndGet();};
                Object c;try{c=claim(r);}finally{SQLiteDatabase.hostBeforeQuery=null;}
                check("C3_COOLDOWN_PRESERVED_WITH_ONE_SCHEMA_ADMISSION",c==null&&admissions.get()==1);
                f.root.execSQL("UPDATE r26_task SET next_at=0,lease_until=? WHERE task_id='schema'",new Object[]{System.currentTimeMillis()+60000});check("C4_UNEXPIRED_LEASE_NOT_STOLEN",claim(r)==null);
                f.root.execSQL("UPDATE r26_task SET lease_until=0,status='QUARANTINED' WHERE task_id='schema'");check("C5_QUARANTINED_TASK_NOT_REVIVED",claim(r)==null);
                f.root.execSQL("UPDATE r26_task SET status='YIELDED' WHERE task_id='schema'");RuntimeCoordinatorR26.Claim admitted=(RuntimeCoordinatorR26.Claim)claim(r);
                check("C6_REAL_SCHEMA_CLAIM_FENCES_AND_RETAINS_CURSOR",admitted!=null&&admitted.task.equals("schema")&&admitted.cursor==5&&admitted.fence==8&&EventYieldR29HostTest.n(f.root,"SELECT fence FROM r26_task WHERE task_id='unready-numerical-task'")==0);
                f.root.execSQL("UPDATE r29_scheduler_owner SET session_id='REPLACED' WHERE singleton=1");boolean stale=false;try{claim(r);}catch(Exception e){stale=causes(e).contains("R29_WRONG_OR_STALE_TASK_DISPATCHER");}check("C7_STALE_SCHEDULER_OWNER_REJECTED",stale);f.root.execSQL("UPDATE r29_scheduler_owner SET session_id='SYNTHETIC_COLD_START' WHERE singleton=1");
                field(RuntimeCoordinatorR26.class,"active",null);check("C8_NATIVE_NO_RUNTIME_CAN_STILL_BOOTSTRAP",RuntimeCoordinatorR26.readyForResearchMigration());field(RuntimeCoordinatorR26.class,"active",r);
                java.util.List<Long> deadlines=new java.util.ArrayList<>();AtomicInteger slow=new AtomicInteger();
                SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(db==f.root&&db.inTransaction()&&sql.equals("PRAGMA schema_version"))try{
                    @SuppressWarnings("unchecked") ThreadLocal<Long> scope=(ThreadLocal<Long>)field(DatabaseCoordinator.class,"WRITE_DEADLINE_CP3");Long deadline=scope.get();if(deadline!=null)deadlines.add(deadline-TimeSourceR811.monoNowMs());
                    if(slow.getAndIncrement()==0)Thread.sleep(610);
                }catch(Exception e){throw new IllegalStateException(e);}};
                long units;try{units=execute(r,1000);}finally{SQLiteDatabase.hostBeforeQuery=null;}
                check("C9_REAL_COMMITTED_JOURNAL_UNITS_MATCH_RETURNED_PROGRESS",units>0&&units==EventYieldR29HostTest.n(f.root,"SELECT count(*) FROM su1_migration_unit_v2"));
                boolean bounded=!deadlines.isEmpty();for(long deadline:deadlines)bounded&=deadline<=250;
                check("C10_EVERY_NEW_UNIT_INHERITS_REMAINING_WRITER_BUDGET",bounded&&deadlines.stream().anyMatch(x->x>0&&x<200));
                CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
                Thread holder=new Thread(()->{try(DatabaseCoordinator.WriterGuardCP3 guard=DatabaseCoordinator.acquireWriterGuardCP3()){held.countDown();release.await(5,TimeUnit.SECONDS);}catch(Throwable e){failure.set(e);held.countDown();}});holder.start();if(!held.await(2,TimeUnit.SECONDS))throw new AssertionError("HOLDER_TIMEOUT");
                long before=EventYieldR29HostTest.n(f.root,"SELECT count(*) FROM su1_migration_unit_v2"),start=System.nanoTime(),busyUnits;
                try{busyUnits=execute(r,1000);}finally{release.countDown();holder.join(2000);}
                check("C11_BUSY_PENDING_RETURNS_ZERO_WITHOUT_INNER_RETRY",failure.get()==null&&busyUnits==0&&(System.nanoTime()-start)/1000000<650&&before==EventYieldR29HostTest.n(f.root,"SELECT count(*) FROM su1_migration_unit_v2"));
                check("C12_WRITER_SCOPE_RESTORED_AFTER_SCHEMA_SLICE",((ThreadLocal<?>)field(DatabaseCoordinator.class,"WRITE_DEADLINE_CP3")).get()==null);
                field(r,"strategyStorageReady",true);check("C13_COMMITTED_DEPENDENCY_OPENS_MIGRATION_GATE",RuntimeCoordinatorR26.readyForResearchMigration());
            }
            System.out.println("R29_COLD_START_FAIRNESS|ALL_PASS|checks="+checks+"|REAL_COORDINATOR_CLAIM_SCHEMA_HOST_ONLY");
        }finally{SQLiteDatabase.hostBeforeQuery=null;field(RuntimeCoordinatorR26.class,"active",old);}
    }
}
