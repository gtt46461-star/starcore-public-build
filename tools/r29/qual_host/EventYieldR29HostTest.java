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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Actual event task, writer admission and pool selection, on separate real SQLite Root/RP files.
 * A synthetic host test, not Android connection-pool, physical-device or future-quality acceptance. */
public final class EventYieldR29HostTest {
    static int checks;
    static void check(String id,boolean ok){System.out.println("R29_EVENT_YIELD|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static void set(Object object,String field,Object value)throws Exception{Field f=object.getClass().getDeclaredField(field);f.setAccessible(true);f.set(object,value);}
    static Object get(Object object,String field)throws Exception{Field f=object.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(object);}
    static Object allocate(Class<?> type)throws Exception{Class<?> u=Class.forName("sun.misc.Unsafe");Field f=u.getDeclaredField("theUnsafe");f.setAccessible(true);return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);}
    static void tx(SQLiteDatabase db,ResearchWriteQueueR27.Batch batch){db.beginTransaction();try{ResearchWriteQueueR27.execute(db,batch,"SYNTHETIC_EVENT_YIELD");db.setTransactionSuccessful();}catch(Exception e){throw e instanceof RuntimeException?(RuntimeException)e:new IllegalStateException(e);}finally{db.endTransaction();}}
    static final class Fixture implements AutoCloseable {
        final Path directory;final SQLiteDatabase root,research;final DbStore store;final DatabaseCoordinator coordinator;
        final RuntimeR27 runtime;final RuntimeR27.EventTask event;final ResearchPoolR27.Worker worker;
        final AtomicInteger attempts=new AtomicInteger(),failWrites=new AtomicInteger();final AtomicBoolean pressure=new AtomicBoolean(),sqlFailure=new AtomicBoolean();
        Fixture()throws Exception{
            directory=Files.createTempDirectory("r29-event-yield-");Application app=new Application(directory.toFile());
            root=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+app.getDatabasePath(AppConfig.DB_NAME)));
            research=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("research.db")));
            for(SQLiteDatabase db:new SQLiteDatabase[]{root,research}){
                db.execSQL("PRAGMA journal_mode=WAL");db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER DEFAULT 0)");
                db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER NOT NULL DEFAULT 0)");
                db.beginTransaction();try{SchemaR27.ensure(db);ResearchEventBridgeR29.ensureRealtime(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            }
            root.beginTransaction();try{root.execSQL("INSERT INTO draws VALUES('202610300001',1,2,3,4,5,6,7,8,9,10,0)");ResearchEventBridgeR29.appendCanonical(root,"202610300001",true,100);root.setTransactionSuccessful();}finally{root.endTransaction();}
            coordinator=DatabaseCoordinator.get(app);Method helperMethod=DatabaseCoordinator.class.getDeclaredMethod("getHelper");helperMethod.setAccessible(true);DbHelper helper=(DbHelper)helperMethod.invoke(coordinator);SQLiteOpenHelper.bind(helper,root);
            store=(DbStore)allocate(DbStore.class);set(store,"researchAppCP12",app);set(store,"researchInputRootCP11",new java.io.File(app.getFilesDir(),"research-input-v2"));set(store,"coordinator",coordinator);set(store,"helper",helper);set(store,"columnCache",new HashMap<>());
            runtime=new RuntimeR27(store,"SYNTHETIC_EVENT_YIELD_"+UUID.randomUUID());
            ResearchWriteQueueR27 writes=new ResearchWriteQueueR27((operation,batch)->{attempts.incrementAndGet();if(failWrites.getAndUpdate(x->Math.max(0,x-1))>0)throw new IllegalStateException("CP3_DB_BUSY_SYNTHETIC_EVENT");if(sqlFailure.get())throw new android.database.sqlite.SQLiteException("no such table: synthetic-missing");tx(research,batch);},pressure::get);
            set(runtime.pool,"writes",writes);worker=runtime.pool.new Worker(research,root,"synthetic-single-worker");worker.serviceWorker=true;event=runtime.new EventTask();
            research.execSQL("CREATE TABLE event_retry_probe(k TEXT PRIMARY KEY)");
        }
        ResearchPoolR27.Outcome boundedTurn()throws Exception{
            ExecutorService executor=Executors.newSingleThreadExecutor();Future<ResearchPoolR27.Outcome> future=executor.submit(()->event.step(worker));
            try{return future.get(1500,TimeUnit.MILLISECONDS);}catch(TimeoutException e){future.cancel(true);throw new AssertionError("EVENT_MUST_RELEASE_SOLE_WORKER_AFTER_ONE_BUSY_ATTEMPT",e);}finally{executor.shutdownNow();executor.awaitTermination(2,TimeUnit.SECONDS);}
        }
        public void close()throws Exception{runtime.close();root.close();research.close();((ExecutorService)get(coordinator,"backgroundWriter")).shutdownNow();try(java.util.stream.Stream<Path>files=Files.walk(directory)){files.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}}
    }
    @SuppressWarnings("unchecked")
    public static void main(String[]args)throws Exception{
        try(Fixture f=new Fixture()){
            f.failWrites.set(1000);long start=System.nanoTime();ResearchPoolR27.Outcome out=f.boundedTurn();
            check("Y1_ONE_BUSY_EVENT_ATTEMPT_RETURNS_ZERO_PROGRESS",out.units==0&&out.cursor==0&&out.reason.contains("COOPERATIVE_WRITER_BUSY")&&f.attempts.get()==1&&(System.nanoTime()-start)/1000000<1500);
            check("Y2_BUSY_EVENT_DOES_NOT_ACK_OR_FABRICATE_LEARNING",n(f.root,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0")==0&&n(f.research,"SELECT count(*) FROM r29_event_inbox")==0&&n(f.research,"SELECT count(*) FROM r29_learning_receipt")==0&&f.event.lastDrain==0);
            check("Y3_BUSY_ATTEMPT_URGENT_CLOCK_IS_NOT_DURABLE_PROGRESS",f.event.lastServiceAttempt>0&&!f.event.urgent(f.worker,f.event.lastServiceAttempt+999)&&f.event.urgent(f.worker,f.event.lastServiceAttempt+1000));
            List<ResearchPoolR27.Task> tasks=(List<ResearchPoolR27.Task>)get(f.runtime.pool,"tasks");Map<String,Boolean> running=(Map<String,Boolean>)get(f.runtime.pool,"running");
            tasks.clear();running.clear();f.runtime.pool.register(f.event);ResearchPoolR27.Task migration=f.runtime.new MigrationTask();f.runtime.pool.register(migration);
            Method next=ResearchPoolR27.class.getDeclaredMethod("next",ResearchPoolR27.Worker.class);next.setAccessible(true);
            check("Y4_ACTUAL_BOOTSTRAP_SCHEDULER_SELECTS_EVENT",next.invoke(f.runtime.pool,f.worker)==f.event);running.put(f.event.id(),false);
            Map<String,Long> notBefore=(Map<String,Long>)get(f.runtime.pool,"notBefore");notBefore.put(f.event.id(),System.currentTimeMillis()+50);Thread.sleep(75);
            check("Y5_AFTER_50MS_BACKOFF_ACTUAL_MIGRATION_GETS_NEXT_TURN",next.invoke(f.runtime.pool,f.worker)==migration);
            running.put(migration.id(),false);tasks.clear();running.clear();f.runtime.pool.register(f.event);
            ResearchPoolR27.Task dispatcher=new ResearchPoolR27.Task(){public String id(){return "synthetic-enabled-dispatcher";}public boolean serviceDispatcher(){return true;}public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w){return ResearchPoolR27.Outcome.idle("SYNTHETIC",0,0);}};
            f.runtime.pool.register(dispatcher);f.event.lastServiceAttempt=System.currentTimeMillis()-75;
            check("Y6_BUSY_EVENT_NOT_PERPETUALLY_URGENT_AFTER_RECEIPT_OVERHEAD",next.invoke(f.runtime.pool,f.worker)==dispatcher);
            f.failWrites.set(1000);ResearchPoolR27.Outcome sameThread=f.event.step(f.worker);check("Y7A_DIRECT_EVENT_ALSO_RETURNS_BUSY_WITHOUT_PROGRESS",sameThread.units==0);
            f.failWrites.set(2);int before=f.attempts.get();RuntimeR27.submit(f.worker,"ORDINARY_RETRY_AFTER_EVENT",new ResearchWriteQueueR27.Batch().add("INSERT INTO event_retry_probe VALUES('ordinary')"));
            check("Y7_EVENT_THREAD_SCOPE_DOES_NOT_LEAK_TO_NUMERICAL_SUBMIT",f.attempts.get()==before+3&&n(f.research,"SELECT count(*) FROM event_retry_probe")==1);
            f.pressure.set(true);before=f.attempts.get();start=System.nanoTime();out=f.boundedTurn();
            check("Y8_REAL_200MS_PRESSURE_GATE_RETURNS_WORKER_WITHOUT_WRITER_ENTRY",out.units==0&&f.attempts.get()==before&&(System.nanoTime()-start)/1000000<1500);f.pressure.set(false);
            f.sqlFailure.set(true);boolean rejected=false;try{f.event.step(f.worker);}catch(android.database.sqlite.SQLiteException expected){rejected=expected.getMessage().contains("no such table");}
            check("Y9_REAL_SQL_FAILURE_IS_NOT_CONVERTED_TO_BUSY",rejected);f.sqlFailure.set(false);
            f.failWrites.set(1);before=f.attempts.get();RuntimeR27.submit(f.worker,"AFTER_SQL_FAILURE",new ResearchWriteQueueR27.Batch().add("INSERT INTO event_retry_probe VALUES('after-sql-error')"));
            check("Y10_EVENT_SCOPE_RESTORED_ON_REAL_FAILURE",n(f.research,"SELECT count(*) FROM event_retry_probe")==2&&f.attempts.get()==before+2);
        }
        try(Fixture f=new Fixture()){
            CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<Throwable> holderError=new AtomicReference<>();
            Thread holder=new Thread(()->{try(DatabaseCoordinator.WriterGuardCP3 guard=DatabaseCoordinator.acquireWriterGuardCP3()){held.countDown();if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("HOLDER_TIMEOUT");}catch(Throwable e){holderError.set(e);held.countDown();}},"synthetic-root-writer-held");holder.start();if(!held.await(2,TimeUnit.SECONDS))throw new AssertionError("ROOT_GUARD_NOT_ACQUIRED");
            try{ResearchPoolR27.Outcome out=f.boundedTurn();
                check("Y11_RESEARCH_COMMIT_SURVIVES_BUSY_ROOT_ACK_WITHOUT_FAKE_COMPLETION",out.units==0&&n(f.research,"SELECT count(*) FROM r29_event_inbox")==1&&n(f.research,"SELECT count(*) FROM r29_learning_work")==2&&n(f.research,"SELECT count(*) FROM r29_learning_receipt")==0&&n(f.root,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0")==0);
            }finally{release.countDown();holder.join(2000);}if(holderError.get()!=null)throw new AssertionError("ROOT_GUARD_FAILED",holderError.get());
            ResearchPoolR27.Outcome replay=f.event.step(f.worker);
            check("Y12_ACK_LOSS_RETRY_USES_REAL_TASK_AND_COMMITS_ONLY_ONCE",replay.units==1&&n(f.research,"SELECT count(*) FROM r29_event_inbox")==1&&n(f.research,"SELECT count(*) FROM r29_learning_work")==2&&n(f.research,"SELECT count(*) FROM r29_learning_receipt")==0&&n(f.root,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0")==1&&f.event.lastDrain>0);
            check("Y13_ACK_MATCHES_ORIGINAL_ROOT_IDENTITY",ResearchEventBridgeR29.pending(f.root).isEmpty()&&n(f.root,"SELECT count(*) FROM draws")==1);
        }
        try(Fixture f=new Fixture()){
            // A genuine durable stage copy exists before its optional status publication becomes writer-busy.
            Path stagePath=f.directory.resolve("durable-stage.db");long copied;
            try(SQLiteDatabase stage=SQLiteDatabase.openOrCreateDatabase(stagePath.toFile(),null)){
                for(String ddl:MigrationCopyR29.DESTINATION_DDL)stage.execSQL(ddl);
                List<MigrationCopyR29.Table> tables=Collections.singletonList(MigrationCopyR29.tables(f.root).get(0));
                f.root.beginTransaction();try{MigrationCopyR29.trackSource(f.root,tables.get(0));f.root.setTransactionSuccessful();}finally{f.root.endTransaction();}
                stage.beginTransaction();try{MigrationCopyR29.register(stage,tables);stage.setTransactionSuccessful();}finally{stage.endTransaction();}
                for(int i=0;i<10&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==0;i++){
                    MigrationCopyR29.step(f.root,stage,tables,body->{stage.beginTransaction();try{body.run(stage);stage.setTransactionSuccessful();}finally{stage.endTransaction();}},System.currentTimeMillis());
                }
                copied=n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'");
            }
            check("Y14_ACTUAL_STAGE_VERIFIED_RECEIPT_IS_DURABLE_BEFORE_STATUS",copied==1);
            RuntimeR27.MigrationTask migration=f.runtime.new MigrationTask();Method publish=RuntimeR27.MigrationTask.class.getDeclaredMethod("publish",ResearchPoolR27.Worker.class,Map.class,long.class);publish.setAccessible(true);
            f.failWrites.set(1000);int before=f.attempts.get();long start=System.nanoTime();boolean busy=false;
            try{publish.invoke(migration,f.worker,Collections.singletonMap("state","SYNTHETIC_VERIFIED_STAGE"),123L);}catch(InvocationTargetException expected){busy=ResearchWriteQueueR27.writerBusy(expected.getCause());}
            check("Y15_REAL_MIGRATION_STATUS_SINGLE_BUSY_ATTEMPT_YIELDS",busy&&f.attempts.get()==before+1&&(System.nanoTime()-start)/1000000<1500&&(Long)get(migration,"reported")==0L);
            try(SQLiteDatabase reopened=SQLiteDatabase.openDatabase(stagePath.toString(),null,SQLiteDatabase.OPEN_READONLY)){
                check("Y16_BUSY_STATUS_DOES_NOT_ROLL_BACK_PREVIOUS_DURABLE_STAGE_RESULT",n(reopened,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==copied);
            }
            f.failWrites.set(2);before=f.attempts.get();RuntimeR27.submit(f.worker,"AFTER_MIGRATION_STATUS_BUSY",new ResearchWriteQueueR27.Batch().add("INSERT INTO event_retry_probe VALUES('after-migration')"));
            check("Y17_MIGRATION_STATUS_SCOPE_DOES_NOT_LEAK_TO_NUMERICAL_RETRY",f.attempts.get()==before+3&&n(f.research,"SELECT count(*) FROM event_retry_probe")==1);
            publish.invoke(migration,f.worker,Collections.singletonMap("state","SYNTHETIC_VERIFIED_STAGE"),456L);
            check("Y18_REPORTED_TIME_ADVANCES_ONLY_AFTER_REAL_STATUS_COMMIT",(Long)get(migration,"reported")==456L&&n(f.research,"SELECT count(*) FROM r27_chart_cache WHERE panel_id='r29' AND key='research-migration' AND updated_at=456")==1);
        }
        try(Fixture f=new Fixture()){
            RuntimeR27.MigrationTask migration=f.runtime.new MigrationTask();AtomicInteger storageStops=new AtomicInteger();
            // The HOST shim lacks Android's setMaximumSize; bind an already prepared stage file without
            // replacing any executed product step/catch/publication behavior.
            Path stagePath=f.directory.resolve("storage-branch-stage.db");SQLiteDatabase stageDb=SQLiteDatabase.openOrCreateDatabase(stagePath.toFile(),null);
            for(String ddl:MigrationCopyR29.DESTINATION_DDL)stageDb.execSQL(ddl);
            List<MigrationCopyR29.Table> tables=Collections.singletonList(MigrationCopyR29.tables(f.root).get(0));
            stageDb.beginTransaction();try{MigrationCopyR29.register(stageDb,tables);stageDb.setTransactionSuccessful();}finally{stageDb.endTransaction();}
            ResearchDatabaseR29 stage=(ResearchDatabaseR29)allocate(ResearchDatabaseR29.class);set(stage,"destination",stageDb);set(stage,"file",stagePath.toFile());set(stage,"tables",tables);set(stage,"prepared",true);set(stage,"references",1);set(migration,"stage",stage);
            // Explicit HOST fault at the framework query boundary drives the actual low-storage catch.
            // It does not claim the host filesystem, or an Android volume, was physically filled.
            SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(db==stageDb&&sql.equals("PRAGMA page_count")){storageStops.incrementAndGet();throw new ResearchDatabaseR29.StorageBlocked("R29_MIGRATION_STAGE_128M_BUDGET_REACHED");}};
            try{
                f.failWrites.set(1000);int before=f.attempts.get();long start=System.nanoTime();ResearchPoolR27.Outcome out=migration.step(f.worker);
                check("Y19_ACTUAL_LOW_STORAGE_CATCH_BUSY_DIAGNOSTIC_REMAINS_RESOURCE_IDLE",storageStops.get()==1&&out.units==0&&out.reason.startsWith("RESOURCE_LOW_STORAGE:MIGRATION_ONLY:")&&f.attempts.get()==before+1&&(System.nanoTime()-start)/1000000<1500&&(Long)get(migration,"reported")==0L);
                f.failWrites.set(0);f.sqlFailure.set(true);boolean rejected=false;try{migration.step(f.worker);}catch(android.database.sqlite.SQLiteException expected){rejected=expected.getMessage().contains("no such table");}
                check("Y20_ACTUAL_LOW_STORAGE_CATCH_DOES_NOT_HIDE_TRUE_STATUS_SQL_FAILURE",rejected&&storageStops.get()==2&&(Long)get(migration,"reported")==0L&&n(f.research,"SELECT count(*) FROM r27_chart_cache WHERE panel_id='r29' AND key='research-migration'")==0);
                f.sqlFailure.set(false);f.failWrites.set(1);before=f.attempts.get();RuntimeR27.submit(f.worker,"AFTER_STORAGE_STATUS",new ResearchWriteQueueR27.Batch().add("INSERT INTO event_retry_probe VALUES('after-storage-status')"));
                check("Y21_LOW_STORAGE_STATUS_RESTORES_SCOPE_FOR_ORDINARY_RETRY",f.attempts.get()==before+2&&n(f.research,"SELECT count(*) FROM event_retry_probe")==1);
            }finally{SQLiteDatabase.hostBeforeQuery=null;migration.close();}
        }
        System.out.println("R29_EVENT_YIELD|ALL_PASS|checks="+checks+"|ACTUAL_EVENT_TASK_WRITER_AND_SCHEDULER_REAL_SQLITE_HOST_ONLY");
    }
}
