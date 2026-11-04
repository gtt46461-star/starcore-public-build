package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.strategyv2.FailureDispositionR31;
import com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30;
import com.ethk3.novaonepro.strategyv2.ResearchPoolR27;
import com.ethk3.novaonepro.strategyv2.RuntimeCoordinatorR26;
import com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26;
import com.ethk3.novaonepro.strategyv2.SchemaR27;
import com.ethk3.novaonepro.strategyv2.StatsR27;
import com.ethk3.novaonepro.strategyv2.WaitKindR27;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual Android SQLite and original production protocol. All rows are isolated engineering fixtures. */
@RunWith(AndroidJUnit4.class)
public final class R31OwnerFenceProtocolInstrumentedTest {
    private static final String PROBE="r31_test_owner_fence_probe";
    private static DatabaseCoordinator.WriteContext context(DatabaseCoordinator.WriteClass kind,String name){
        return DatabaseCoordinator.WriteContext.of(kind,name);
    }
    private static DbStore open() throws Exception {
        Context app=ApplicationProvider.getApplicationContext();
        // Refuse before opening the owner if someone invokes this with the normal application runner.
        assertTrue("Explicit R30FaultAndroidRunner sandbox is required",
                app.getDatabasePath(AppConfig.DB_NAME).getCanonicalPath().contains("/r30-instrumentation/"));
        DbStore store=new DbStore(app);
        boolean initialized=false;
        try{
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    context(DatabaseCoordinator.WriteClass.MIGRATION,"R31_TEST_OWNER_SCHEMA"),db->{
                        RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);
                        db.execSQL("CREATE TABLE IF NOT EXISTS "+PROBE+"(id TEXT PRIMARY KEY,value INTEGER NOT NULL)");return 0;
                    });
            initialized=true;return store;
        }finally{if(!initialized)store.close();}
    }
    private static long number(SQLiteDatabase db,String sql,String... args){
        try(Cursor rows=db.rawQuery(sql,args)){assertTrue(sql,rows.moveToFirst());return rows.getLong(0);}
    }
    private static Object invoke(Method method,Object owner,Object... args) throws Exception {
        try{return method.invoke(owner,args);}
        catch(InvocationTargetException wrapped){Throwable cause=wrapped.getCause();if(cause instanceof Error)throw (Error)cause;if(cause instanceof Exception)throw (Exception)cause;throw new AssertionError(cause);}
    }
    private static RuntimeCoordinatorR26.Claim claim(DbStore store,String task,String owner) throws Exception {
        Method original=RuntimeCoordinatorR26.class.getDeclaredMethod("claimWithinR31",SQLiteDatabase.class,
                String.class,String.class,String.class,RuntimeCoordinatorR26.Claim.class);
        original.setAccessible(true);
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                context(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_TEST_ORIGINAL_CLAIM"),
                db->(RuntimeCoordinatorR26.Claim)invoke(original,null,db,task,owner,"R27",null));
    }
    private static <T>T bound(RuntimeCoordinatorR26.Claim claim,Callable<T> body) throws Exception {
        Method bind=RuntimeCoordinatorR26.class.getDeclaredMethod("bindBackgroundClaim",RuntimeCoordinatorR26.Claim.class);
        Method clear=RuntimeCoordinatorR26.class.getDeclaredMethod("unbindBackgroundClaimR31");
        bind.setAccessible(true);clear.setAccessible(true);invoke(bind,null,claim);
        try{return body.call();}finally{invoke(clear,null);}
    }
    private static void receipt(ResearchPoolR27 pool,RuntimeCoordinatorR26.Claim claim,Throwable failure,long units) throws Exception {
        Class<?> budget=Class.forName(ResearchPoolR27.class.getName()+"$ResourceBudget");
        Method original=ResearchPoolR27.class.getDeclaredMethod("record",String.class,ResearchPoolR27.Outcome.class,
                Throwable.class,budget,RuntimeCoordinatorR26.Claim.class,String.class,long.class);
        original.setAccessible(true);
        invoke(original,pool,claim.task,new ResearchPoolR27.Outcome(units,units>0?null:"WAITING_NEW_DRAW",claim.cursor,-1),
                failure,null,claim,"ANDROID_ORIGINAL_RECEIPT_INJECTED_NOT_NATURAL",1L);
    }
    private static void state(DbStore store,RuntimeCoordinatorR26.Claim claim,String expectedStatus,String expectedWaitKind){
        try(Cursor row=store.readable().rawQuery("SELECT owner,fence,status,error_count,wait_kind FROM r26_task WHERE task_id=?",new String[]{claim.task})){
            assertTrue(row.moveToFirst());assertEquals(claim.owner,row.getString(0));assertEquals(claim.fence,row.getLong(1));
            assertEquals(expectedStatus,row.getString(2));assertEquals("Late receipts cannot consume the new owner's fault budget",0,row.getLong(3));
            if(expectedWaitKind!=null)assertEquals("Late receipts cannot replace the current owner's actual resource wait",expectedWaitKind,row.getString(4));
        }
    }
    private static String probe(DbStore store){
        String key=UUID.randomUUID().toString();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                context(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_TEST_OWNER_PROBE"),
                db->{db.execSQL("INSERT INTO "+PROBE+" VALUES(?,0)",new Object[]{key});return 0;});
        return key;
    }
    private static DatabaseCoordinator.CommitIdentityR31 identity(String key,String task,String owner,long fence){
        return new DatabaseCoordinator.CommitIdentityR31(key,StatsR27.sha256("increment:"+key),task,owner,fence,
                StatsR27.sha256("IMMUTABLE_ENGINEERING_INPUT:"+key),0,"R31_ANDROID_OWNER_PROTOCOL_V1");
    }
    private static void joinExited(Thread thread,long millis) throws InterruptedException {
        if(thread==null)return;thread.join(millis);assertFalse("Actual thread exit was not observed: "+thread.getName(),thread.isAlive());
    }

    @Test public void staleOwnerReceiptsCannotReplaceNewClaimAndCommitIsIdempotent() throws Exception {
        DbStore store=open();ResearchPoolR27 oldPool=null,newPool=null;
        try{
            String task="r27-owner-fence-test-"+UUID.randomUUID(),oldOwner="R31_TEST_OLD_"+UUID.randomUUID(),
                    newOwner="R31_TEST_NEW_"+UUID.randomUUID(),key=probe(store);
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    context(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_TEST_SESSION_FIXTURE"),db->{
                        long wall=System.currentTimeMillis();String boot=IssueTimelineWriterR30.bootId();
                        for(String owner:new String[]{oldOwner,newOwner})db.execSQL(
                                "INSERT INTO r26_session VALUES(?,1,?,'RUNNING',?,0)",new Object[]{owner,boot,wall});
                        return 0;
                    });
            AtomicInteger unexpectedComputes=new AtomicInteger();
            ResearchPoolR27.Task registered=new ResearchPoolR27.Task(){
                @Override public String id(){return task;}
                @Override public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker worker){
                    unexpectedComputes.incrementAndGet();throw new AssertionError("Protocol-only pool was not kept resource-paused");
                }
            };
            oldPool=new ResearchPoolR27(store,oldOwner,1);newPool=new ResearchPoolR27(store,newOwner,1);
            // Production start performs the durable registration and owns real read connections.
            // This deliberate test pause prevents numerical work; it is not a production eligibility rule.
            for(ResearchPoolR27 pool:new ResearchPoolR27[]{oldPool,newPool}){
                pool.setResource("INSTRUMENTED_PROTOCOL_CONTROL_ONLY");pool.register(registered);pool.start();
            }
            assertEquals(1,number(store.readable(),"SELECT count(*) FROM r26_task WHERE task_id=? AND lane='R27' AND registered_at>0",task));
            assertEquals("A newly registered task must exercise the original SQL NULL owner path",1,
                    number(store.readable(),"SELECT count(*) FROM r26_task WHERE task_id=? AND owner IS NULL",task));
            RuntimeCoordinatorR26.Claim old=claim(store,task,oldOwner);assertNotNull(old);
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    context(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_TEST_OLD_FENCE_VALID"),
                    db->{RuntimeCoordinatorR26.requireFence(db,old);return 0;});
            ResearchPoolR27.retry(store,task); // original recovery revokes the old fence; no handwritten claim CAS
            RuntimeCoordinatorR26.Claim fresh=claim(store,task,newOwner);assertNotNull(fresh);assertTrue(fresh.fence>old.fence);
            receipt(oldPool,old,new IllegalStateException("INJECTED_LATE_OLD_FAILURE"),0);receipt(oldPool,old,null,1);
            state(store,fresh,"RUNNING",null);
            AtomicInteger businessBodies=new AtomicInteger(),faults=new AtomicInteger();
            DatabaseCoordinator.CommitIdentityR31 staleId=identity(key,task,old.owner,old.fence);
            try{
                bound(old,()->store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(
                        context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_STALE_OWNER_COMMIT"),staleId,
                        db->{businessBodies.incrementAndGet();db.execSQL("UPDATE "+PROBE+" SET value=value+1 WHERE id=?",new Object[]{key});return 1;},0));
                fail("A stale bound owner reached a production writer body");
            }catch(DatabaseCoordinator.WriteRejectedR31 expected){
                assertEquals(FailureDispositionR31.Disposition.STALE_OWNER,FailureDispositionR31.classify(expected).disposition);
            }
            assertEquals(0,businessBodies.get());assertEquals(0,number(store.readable(),"SELECT value FROM "+PROBE+" WHERE id=?",key));
            DatabaseCoordinator.CommitIdentityR31 currentId=identity(key,task,fresh.owner,fresh.fence);
            DatabaseCoordinator.diagnosticFaultR31.set(phase->{if("AFTER_COMMIT".equals(phase)){faults.incrementAndGet();throw new IllegalStateException("INJECTED_OWNER_POSTCOMMIT_DIAGNOSTIC");}});
            DatabaseCoordinator.CommitResultR31<Integer> committed;
            try{
                committed=bound(fresh,()->store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(
                        context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_CURRENT_OWNER_COMMIT"),currentId,
                        db->{RuntimeCoordinatorR26.requireFence(db,fresh);businessBodies.incrementAndGet();db.execSQL("UPDATE "+PROBE+" SET value=value+1 WHERE id=?",new Object[]{key});return 1;},0));
            }finally{DatabaseCoordinator.diagnosticFaultR31.remove();}
            assertFalse(committed.replay);assertEquals(DatabaseCoordinator.CommitOutcome.COMMITTED_DIAGNOSTIC_FAILED,committed.outcome);
            assertNotNull(committed.diagnosticFailure);assertEquals(1,faults.get());
            for(int i=0;i<100;i++)assertTrue(bound(fresh,()->store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(
                    context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_CURRENT_OWNER_COMMIT"),currentId,
                    db->{businessBodies.incrementAndGet();throw new AssertionError("Duplicate input re-entered the business body");},0)).replay);
            assertEquals(1,businessBodies.get());assertEquals(1,number(store.readable(),"SELECT value FROM "+PROBE+" WHERE id=?",key));
            assertEquals(1,number(store.readable(),"SELECT count(*) FROM r31_commit_receipt WHERE commit_key=? AND owner_epoch=? AND fence=?",key,fresh.owner,String.valueOf(fresh.fence)));
            receipt(newPool,fresh,null,1);state(store,fresh,"BLOCKED",WaitKindR27.RESOURCE);
            receipt(oldPool,old,new IllegalStateException("INJECTED_LATE_FAILURE_AFTER_NEW_RECEIPT"),0);receipt(oldPool,old,null,1);
            state(store,fresh,"BLOCKED",WaitKindR27.RESOURCE);assertEquals(0,unexpectedComputes.get());
            assertEquals(DatabaseCoordinator.CommitOutcome.COMMITTED,store.coordinatorForResearchR952().resolveCommitReceiptR31(currentId));
        }finally{
            DatabaseCoordinator.diagnosticFaultR31.remove();
            if(oldPool!=null)oldPool.close();if(newPool!=null)newPool.close();
            boolean oldExited=oldPool==null||oldPool.awaitResourcesTerminatedR31(6000);
            boolean newExited=newPool==null||newPool.awaitResourcesTerminatedR31(6000);
            if(oldExited&&newExited)store.close();
            assertTrue("Old pool resources did not actually terminate",oldExited);assertTrue("New pool resources did not actually terminate",newExited);
        }
        System.out.println("R31_ANDROID|OWNER_FENCE_ORIGINAL_REGISTER_CLAIM_RECOVERY_RECEIPT|STALE_SUCCESS_FAILURE_REJECTED|NEW_OWNER_ONE_COMMIT_100_REPLAYS|POSTCOMMIT_DIAGNOSTIC_SEPARATE|BOTH_POOLS_ACTUALLY_DRAINED|ENGINEERING_NOT_TRAINING_OR_QUALITY|PASS");
    }

    @Test public void independentSqliteWriterContentionReleasesBeforeSameInputRetry() throws Exception {
        DbStore store=open();final String key,path;
        try{key=probe(store);path=store.readable().getPath();}
        catch(RuntimeException|Error failed){store.close();throw failed;}
        DatabaseCoordinator.CommitIdentityR31 id=identity(key,"R31_TEST_CONNECTION_CONTENTION","R31_TEST_CONNECTION_OWNER",1);
        CountDownLatch locked=new CountDownLatch(1),release=new CountDownLatch(1),admitted=new CountDownLatch(1),bodyEntered=new CountDownLatch(1);
        AtomicReference<Throwable> holderFailure=new AtomicReference<>(),contenderFailure=new AtomicReference<>();
        AtomicReference<DatabaseCoordinator.CommitResultR31<Integer>> first=new AtomicReference<>();
        AtomicInteger bodies=new AtomicInteger(),attempts=new AtomicInteger();
        Thread holder=new Thread(()->{
            try(SQLiteDatabase separate=SQLiteDatabase.openDatabase(path,null,SQLiteDatabase.OPEN_READWRITE)){
                separate.beginTransaction();
                try{
                    separate.execSQL("UPDATE "+PROBE+" SET value=value WHERE id=?",new Object[]{key});locked.countDown();
                    if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("Controller did not release the real SQLite writer");
                }finally{separate.endTransaction();}
            }catch(Throwable failed){holderFailure.set(failed);locked.countDown();}
        },"R31_ANDROID_EXTERNAL_SQLITE_WRITER");
        Thread contender=new Thread(()->{
            DatabaseCoordinator.diagnosticFaultR31.set(phase->{if("LOCK_WAIT".equals(phase))admitted.countDown();});
            try{
                attempts.incrementAndGet();
                first.set(store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(
                        context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_REAL_SQLITE_CONTENTION"),id,
                        db->{bodyEntered.countDown();bodies.incrementAndGet();db.execSQL("UPDATE "+PROBE+" SET value=value+1 WHERE id=?",new Object[]{key});return 1;},0));
            }catch(Throwable failed){contenderFailure.set(failed);}finally{DatabaseCoordinator.diagnosticFaultR31.remove();}
        },"R31_ANDROID_PRODUCTION_WRITER_CONTENDER");
        try{
            holder.start();assertTrue("Independent SQLite transaction did not acquire its writer",locked.await(3,TimeUnit.SECONDS));
            assertNull(holderFailure.get());contender.start();
            assertTrue("Production coordinator admission was not observed",admitted.await(3,TimeUnit.SECONDS));
            assertFalse("Business body ran while the separate SQLite writer was held",bodyEntered.await(150,TimeUnit.MILLISECONDS));
            release.countDown();joinExited(holder,7000);joinExited(contender,7000);assertNull(holderFailure.get());
            if(contenderFailure.get()!=null){
                assertTrue("Only an explicit retryable SQLite rejection is acceptable",contenderFailure.get() instanceof DatabaseCoordinator.WriteRejectedR31);
                DatabaseCoordinator.WriteRejectedR31 rejected=(DatabaseCoordinator.WriteRejectedR31)contenderFailure.get();
                assertEquals("A Java admission timeout is not a SQLite-connection contention result","BEGIN",rejected.phase);
                assertEquals(FailureDispositionR31.Disposition.RETRY_LATER,FailureDispositionR31.classify(rejected).disposition);
                assertEquals(DatabaseCoordinator.CommitOutcome.ROLLED_BACK,store.coordinatorForResearchR952().resolveCommitReceiptR31(id));
                attempts.incrementAndGet();first.set(store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(
                        context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_REAL_SQLITE_CONTENTION"),id,
                        db->{bodies.incrementAndGet();db.execSQL("UPDATE "+PROBE+" SET value=value+1 WHERE id=?",new Object[]{key});return 1;},0));
            }
            assertNotNull(first.get());assertEquals(DatabaseCoordinator.CommitOutcome.COMMITTED,first.get().outcome);
            assertEquals(1,bodies.get());assertEquals(1,number(store.readable(),"SELECT value FROM "+PROBE+" WHERE id=?",key));
            assertEquals(1,number(store.readable(),"SELECT count(*) FROM r31_commit_receipt WHERE commit_key=?",key));
            assertTrue(store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(
                    context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_REAL_SQLITE_CONTENTION"),id,
                    db->{throw new AssertionError("Resolved/replayed SQLite attempt repeated its business effect");},0).replay);
        }finally{
            release.countDown();
            if(holder.isAlive())holder.join(7000);if(contender.isAlive())contender.join(7000);
            boolean exited=!holder.isAlive()&&!contender.isAlive();if(exited)store.close();
            assertTrue("SQLite resources remain owned by a live test thread; no early close/release",exited);
        }
        System.out.println("R31_ANDROID|TWO_REAL_SQLITE_DATABASE_CONNECTIONS|BOUNDED_WRITER_CONTENTION|ACTUAL_HOLDER_AND_CONTENDER_EXIT|SAME_INPUT_ATTEMPTS="+attempts.get()+"|ONE_EFFECT_ONE_RECEIPT|ENGINEERING_NOT_DEVICE_SLO_OR_QUALITY|PASS");
    }
}
