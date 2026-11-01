package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Test APK only: actual original writer and production batches on isolated Android SQLite.
 * Known planted data exercises mechanisms; it is never natural evidence or a REAL certificate. */
@RunWith(AndroidJUnit4.class)
public final class RepairResearchFaultAndroidTest {
    static final String LOOP="ANDROID_REPAIR_FAULT_ONLY";
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){assertTrue(sql,c.moveToFirst());return c.getLong(0);}}
    static long count(DbStore s,String table){return n(s.readable(),"SELECT count(*) FROM "+table);}
    static void write(DbStore s,String op,ResearchWriteQueueR27.Batch b){
        s.coordinatorForResearchR952().runAtomicWriteOrThrowF01(op,db->{ResearchWriteQueueR27.execute(db,b,op);return 0;});
    }
    static boolean message(Throwable t,String text){for(Throwable x=t;x!=null;x=x.getCause()){if(String.valueOf(x.getMessage()).contains(text))return true;if(x==x.getCause())break;}return false;}
    static boolean caused(Throwable t,Class<?> type){for(Throwable x=t;x!=null;x=x.getCause()){if(type.isInstance(x))return true;if(x==x.getCause())break;}return false;}
    static final class Fixture {
        final EvalContextR27 ctx;final PairedReplayRunnerR30R1 runner;final ResearchWriteQueueR27.Batch analysis;final DbStore store;
        Fixture(DbStore s){
            store=s;s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_REPAIR_FAULT_SCHEMA",db->{RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);db.execSQL("CREATE TABLE IF NOT EXISTS r30_repair_fault_disk(id INTEGER PRIMARY KEY,payload BLOB)");return 0;});
            ReplayDatasetR27 raw=ReplayDatasetR27.synthetic(6000,841,new int[0]).planted(.85,842);List<ReplayDatasetR27.Row> rows=new ArrayList<>();long start=TargetIssueResolver.sequenceOrdinalSU1("202610240001");
            for(int i=0;i<raw.size();i++)rows.add(new ReplayDatasetR27.Row(TargetIssueResolver.fromSequenceOrdinalSU1(start+i),start+i,raw.draws[i]));
            ReplayDatasetR27 data=ReplayDatasetR27.of(rows);ctx=new EvalContextR27(new FeatureCacheR27(data),SplitR27.of(data,null),null);
            if(count(s,"draws")==0){Models.Draw d=new Models.Draw();d.issue=data.issues[0];d.openTime=1;d.fetchTime=2;d.sourceLine="SYNTHETIC_REPAIR_FAULT_ONLY";System.arraycopy(data.draws[0],0,d.numbers,0,10);assertEquals(1,s.saveDraws(Collections.singletonList(d)));}
            long revision=SchemaR27.liveSourceRevision(s.readable());ResearchWriteQueueR27.Batch pin=new ResearchWriteQueueR27.Batch();ResearchInputPinR30R1.append(pin,data,revision,1);write(s,"R30_REPAIR_FAULT_INPUT",pin);
            PolicySpecR27 parent=new PolicySpecR27(PolicyAstR27.op("add",PolicyAstR27.op("mul",PolicyAstR27.constant(5),PolicyAstR27.leaf("randu",843)),PolicyAstR27.op("zscore",PolicyAstR27.leaf("cross",2,1))),1,PolicySpecR27.Rule.ALWAYS,0,1,1,false,0,0,IdeaFamilyR27.meta("I09","ANDROID_REGISTERED_SYNTHETIC",0,844,"KNOWN_SIGNAL_NOT_NATURAL_QUALITY"));
            ResearchContractR30R1 contract=new ResearchContractR30R1(data.datasetHash,parent.policyHash(),FrozenConfirmationR30.currentSemantics(),BuildConfig.SOURCE_COMMIT_R26,StatsR27.sha256("ANDROID_REPAIR_FAULT_REGISTERED_V1"),ResearchContractR30R1.Role.SELECT,data.ordinals[ctx.split.selectFrom],data.ordinals[ctx.split.selectTo-1],0,ResearchInputPinR30R1.revision(s.readable(),data.datasetHash));
            runner=new PairedReplayRunnerR30R1(contract,parent,"root/0",ctx,1,0,64);while(!runner.complete())runner.advance(ctx,256);
            assertEquals("actual production replay must produce the tested pruning proposal",ResearchAdviceR30R1.Action.PRUNE_COMPONENT,runner.advice().action);
            analysis=ResearchStoreR30R1.paired(runner,10);
        }
        ResearchWriteQueueR27.Batch consumption(){
            EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.births=1;cfg.minPerFamily=0;cfg.expertShare=0;cfg.nullControl=false;
            StoreR27.EvolutionSink sink=new StoreR27.EvolutionSink(LOOP,ctx.data.issues[ctx.data.size()-1]);EvolutionLoopR27 loop=new EvolutionLoopR27(cfg,ctx.cache,sink,()->1);
            loop.step=2;loop.directedResearch=ResearchStoreR30R1.ready(store.readable(),ctx,0,4);assertEquals(1,loop.directedResearch.size());loop.step();loop.step();
            assertEquals("original birth gate", "BORN",loop.pendingState(runner.without.policyHash()));loop.commitResearchAdmissions(sink.batch,11);
            sink.batch.add("INSERT OR REPLACE INTO r27_evolution_cursor VALUES(?,?,?,?,?,?,?)",LOOP,loop.generation,loop.step,ctx.data.datasetHash,0,loop.snapshot(),11);return sink.batch;
        }
    }
    static void absent(DbStore s,String stage){
        assertEquals(0,count(s,"r30r1_consume"));assertEquals(0,count(s,"r27_policy"));assertEquals(0,count(s,"r27_evolution_cursor"));
        assertEquals(stage.equals("analysis")?0:2,count(s,"r30r1_analysis_run")-n(s.readable(),"SELECT count(*) FROM r30r1_analysis_run WHERE status='INPUT_DATASET_PINNED'"));
        assertEquals(stage.equals("analysis")?0:1,count(s,"r30r1_proposal"));
        if(stage.equals("analysis")){assertEquals(0,count(s,"research_matrix_snapshot_r930"));assertEquals(0,n(s.readable(),"SELECT count(*) FROM r30r1_research_measure WHERE kind!='INPUT_INVALIDATION'"));assertEquals(0,count(s,"r30r1_chart_cache"));}
        assertEquals(0,count(s,"r27_certificate"));assertEquals(0,count(s,"r27_holdout_exposure"));
    }
    static void complete(DbStore s,String stage){
        assertEquals(1,count(s,"r30r1_proposal"));assertEquals(1,count(s,"r30r1_chart_cache"));
        assertEquals(stage.equals("analysis")?0:1,count(s,"r30r1_consume"));assertEquals(stage.equals("analysis")?0:1,count(s,"r27_policy"));assertEquals(stage.equals("analysis")?0:1,count(s,"r27_evolution_cursor"));
        assertEquals(0,count(s,"r27_certificate"));assertEquals(0,count(s,"r27_holdout_exposure"));
    }
    static void busy(DbStore s,ResearchWriteQueueR27.Batch batch)throws Exception{
        CountDownLatch locked=new CountDownLatch(1),release=new CountDownLatch(1);ExecutorService executor=Executors.newSingleThreadExecutor();
        Future<?> holder=executor.submit(()->s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_REPAIR_FAULT_HOLD_ORIGINAL_WRITER",db->{db.execSQL("INSERT INTO r30_repair_fault_disk VALUES(7,zeroblob(1))");locked.countDown();if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("HOLDER_TIMEOUT");return 0;}));
        try{assertTrue(locked.await(10,TimeUnit.SECONDS));long start=SystemClock.elapsedRealtime();assertFalse("actual original writer deadline must requeue",new ResearchWriteQueueR27(s).submit("R30_REPAIR_BUSY",batch,()->false));long elapsed=SystemClock.elapsedRealtime()-start;assertTrue("bounded original writer admission: "+elapsed,elapsed<1500);}
        finally{release.countDown();try{holder.get(10,TimeUnit.SECONDS);}finally{executor.shutdownNow();}}
    }
    static void full(DbStore s,ResearchWriteQueueR27.Batch batch){
        long[] original={0},bytes={0};
        s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_REPAIR_FAULT_FULL_ARM",db->{long pages=n(db,"PRAGMA page_count");original[0]=n(db,"PRAGMA max_page_count");try(Cursor c=db.rawQuery("PRAGMA max_page_count="+pages,null)){assertTrue(c.moveToFirst());assertEquals(pages,c.getLong(0));}bytes[0]=(pages+1)*n(db,"PRAGMA page_size");assertTrue(bytes[0]<=128L*1024*1024);return 0;});
        int ops=batch.ops.size();batch.add("INSERT INTO r30_repair_fault_disk VALUES(8,zeroblob(?))",bytes[0]);
        try{write(s,"R30_REPAIR_FAULT_FULL",batch);fail("actual SQLITE_FULL required");}
        catch(RuntimeException failure){assertTrue("original Android SQLiteFullException retained: "+failure,caused(failure,SQLiteFullException.class));}
        finally{batch.ops.remove(ops);s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_REPAIR_FAULT_FULL_RESTORE",db->{try(Cursor c=db.rawQuery("PRAGMA max_page_count="+original[0],null)){assertTrue(c.moveToFirst());assertEquals(original[0],c.getLong(0));}return 0;});}
        assertEquals(0,n(s.readable(),"SELECT count(*) FROM r30_repair_fault_disk WHERE id=8"));
    }
    static void marker(String text){System.out.println(text);Bundle b=new Bundle();b.putString("stream",text+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,b);}
    @Test public void faultCell()throws Exception{
        assertTrue(InstrumentationRegistry.getInstrumentation() instanceof R30FaultAndroidRunner);Bundle args=InstrumentationRegistry.getArguments();String stage=args.getString("repairStage"),fault=args.getString("repairFault"),phase=args.getString("r30Phase","run");
        assertTrue(Arrays.asList("analysis","consume").contains(stage));assertTrue(Arrays.asList("BUSY","FULL","CANCEL","KILL","CORRUPT","SOURCE_CHANGE").contains(fault));
        DbStore s=new DbStore(ApplicationProvider.getApplicationContext());
        try{
            boolean recover=phase.equals("recover");if(recover){absent(s,stage);marker("R30_REPAIR_KILL_RECOVERY|"+stage+"|ACTUAL_REOPEN_NO_HALF_CHAIN");}
            Fixture f=new Fixture(s);if(stage.equals("consume")&&!recover)write(s,"R30_REPAIR_FAULT_ANALYSIS_SEED",f.analysis);
            ResearchWriteQueueR27.Batch batch=stage.equals("analysis")?f.analysis:f.consumption();boolean blocked=false;
            if(fault.equals("KILL")&&!recover){
                batch.guardAfter(db->{assertTrue(db.inTransaction());assertEquals(1,n(db,"SELECT count(*) FROM r30r1_proposal"));if(stage.equals("consume"))assertEquals(1,n(db,"SELECT count(*) FROM r30r1_consume"));Log.i("R30_REPAIR_KILL_POINT",stage+"|ORIGINAL_BATCH_BEFORE_COMMIT|pid="+android.os.Process.myPid());android.os.Process.killProcess(android.os.Process.myPid());throw new AssertionError("SIGKILL returned");});
                write(s,"R30_REPAIR_FAULT_KILL",batch);fail("real process death required");
            }else if(fault.equals("BUSY")){busy(s,batch);absent(s,stage);}
            else if(fault.equals("FULL")){full(s,batch);absent(s,stage);}
            else if(fault.equals("CANCEL")){assertFalse(new ResearchWriteQueueR27(s).submit("R30_REPAIR_CANCEL",batch,()->true));absent(s,stage);}
            else if(fault.equals("SOURCE_CHANGE")){
                s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_REPAIR_FAULT_REAL_SOURCE_CORRECTION",db->{db.execSQL("UPDATE draws SET n1=n2,n2=n1 WHERE issue=?",new Object[]{f.ctx.data.issues[0]});return 0;});
                assertTrue(ResearchInputPinR30R1.invalid(s.readable(),f.ctx.data.datasetHash));
                try{write(s,"R30_REPAIR_SOURCE_GUARD",batch);fail("stale input must fail");}catch(RuntimeException failure){assertTrue("real production stale-source guard",ResearchStoreR30R1.stale(failure));}absent(s,stage);blocked=true;
            }else if(fault.equals("CORRUPT")){
                if(stage.equals("analysis")){
                    try{ResearchStoreR30R1.paired(f.runner.contract,f.runner.gain().estimate(),f.runner.advice(),f.runner.parent.fullJson(),f.runner.without.policyHash(),10);fail("mismatched exact measured child must fail");}catch(IllegalStateException expected){assertEquals("ANALYSIS_CANDIDATE_HASH",expected.getMessage());}absent(s,stage);
                }else{
                    s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_REPAIR_FAULT_CORRUPT_PAYLOAD",db->{db.execSQL("UPDATE r30r1_proposal SET payload_json=payload_json||' '");return 0;});
                    try{write(s,"R30_REPAIR_CORRUPT_GUARD",batch);fail("payload hash must fail");}catch(RuntimeException expected){assertTrue(expected.toString(),message(expected,"R27_WRITE_PRECONDITION"));}absent(s,stage);blocked=true;
                }
            }
            if(!blocked){write(s,"R30_REPAIR_FAULT_RETRY",batch);complete(s,stage);if(stage.equals("analysis")){write(s,"R30_REPAIR_FAULT_IDEMPOTENT_RETRY",batch);complete(s,stage);}else{try{write(s,"R30_REPAIR_FAULT_REPEATED_CONSUME",batch);fail("duplicate consumption required rejection");}catch(RuntimeException expected){assertTrue(ResearchStoreR30R1.stale(expected));}complete(s,stage);}}
            marker("R30_REPAIR_FAULT_CELL|"+stage+'|'+fault+"|ACTUAL_ANDROID_SQLITE_ORIGINAL_WRITER|"+(blocked?"BLOCKED_REQUIRES_VALID_ORIGINAL_SOURCE":"RECOVERED_EXACTLY_ONCE")+"|NO_HALF_CHAIN_NO_REAL_NO_FORMAL_LABELS|SYNTHETIC_ONLY|PASS");
        }finally{s.close();}
    }
}
