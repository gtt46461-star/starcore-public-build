package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Native SQLite + the registered Runtime Turn restore, on isolated synthetic data. No quality claim. */
@RunWith(AndroidJUnit4.class)
public final class R29EvolutionDatasetPinDeviceTest {
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static RuntimeR27.EvolutionTask task(RuntimeR27 rt)throws Exception{Field f=RuntimeR27.class.getDeclaredField("evolutionTask");f.setAccessible(true);return(RuntimeR27.EvolutionTask)f.get(rt);}
    static PolicySpecR27 policy(int window){return new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,window)),1,PolicySpecR27.Rule.QUANTILE,0,.2,1,false,.01,.01,null);}
    static List<Models.Draw> draws(long first,int from,int count,long opened){
        StatsR27.SplitMix rng=new StatsR27.SplitMix(29001031L+from);List<Models.Draw> out=new ArrayList<>();
        for(int i=from;i<from+count;i++){Models.Draw d=new Models.Draw();d.issue=TargetIssueResolver.fromSequenceOrdinalSU1(first+i);d.numbers=ReplayDatasetR27.randomPermutation(rng);d.openTime=opened+i*60000L;d.fetchTime=d.openTime+1000;d.sourceLine="SYNTHETIC_NATIVE_EVOLUTION_PIN";out.add(d);}return out;
    }
    static EvolutionLoopR27 midGeneration(ReplayDatasetR27 data){
        EvolutionLoopR27 loop=new EvolutionLoopR27(new EvolutionLoopR27.Config(),new FeatureCacheR27(data),null,()->123L);
        loop.step(); // Actual DATASET phase; the remainder is an explicit small mid-generation fixture.
        PolicySpecR27 parent=policy(64),candidate=policy(128);EvolutionLoopR27.Member member=new EvolutionLoopR27.Member();member.spec=parent;member.tau=1;member.island="FREQ";loop.population.add(member);
        loop.archive.put(3,new EliteArchiveR27.Elite(parent,.125,1));loop.addPendingForTest(candidate);loop.live.add(parent);loop.liveTau.put(parent.policyHash(),1.0);
        loop.champion=parent;loop.previousChampion=parent;loop.i00Champion=parent;loop.nullEvolution.champion=candidate;
        loop.ledger().restore(8,StatsR27.sha256("SYNTHETIC_ALREADY_EXPOSED"));loop.ledger().restoreControl(9,StatsR27.sha256("SYNTHETIC_CONTROL_EXPOSED"));
        loop.generation=7;loop.step=3;loop.rng.state=0x1234_5678_9abc_def0L;return loop;
    }
    static void save(DbStore store,EvolutionLoopR27 loop,String snapshot,ReplayDatasetR27 data)throws Exception{
        EvolutionDatasetPinR29.Prepared pin=EvolutionDatasetPinR29.prepare(data);ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();EvolutionDatasetPinR29.append(batch,pin);
        batch.add("INSERT OR REPLACE INTO r27_evolution_cursor(loop,generation,step,dataset_hash,rng_state,snapshot,updated_at) VALUES('MAIN',?,?,?,?,?,?)",loop.generation,loop.step,data.datasetHash,loop.rng.state,snapshot,System.currentTimeMillis());
        batch.add(RuntimeR27.EVOLUTION_CHECKSUM_DDL);batch.add("INSERT OR REPLACE INTO r29_evolution_checkpoint_checksum VALUES('MAIN',?,?,?,?,?)",StatsR27.sha256(snapshot),data.datasetHash,loop.generation,loop.step,loop.rng.state);
        ResearchRouteR29.write(store,"SYNTHETIC_EVOLUTION_PIN_CHECKPOINT",db->{ResearchWriteQueueR27.execute(db,batch,"SYNTHETIC_EVOLUTION_PIN_CHECKPOINT");return 0;});
    }
    static ReplayDatasetR27 replaceHotTail(DbStore store,long first,int from,long opened)throws Exception{
        List<Models.Draw> fresh=draws(first,from,1000,opened);
        ResearchRouteR29.write(store,"SYNTHETIC_RAW_TAIL_REPLACEMENT",db->{
            for(Models.Draw d:fresh){Object[] a=new Object[15];a[0]=d.issue;a[1]=d.openTime;a[2]=d.fetchTime;a[3]=d.sourceLine;for(int p=0;p<10;p++)a[4+p]=d.numbers[p];a[14]=0;
                db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,source_line,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",a);}
            db.execSQL("DELETE FROM draws WHERE issue IN(SELECT issue FROM draws ORDER BY issue LIMIT 200)");return 0;
        });
        return StoreR27.loadDataset(ResearchRouteR29.reader(store.readable()),RuntimeR27.DATASET_ROWS);
    }
    @Test public void nativeRestartUsesPinnedInputAndContinuesExactTurn()throws Exception{
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertNull(DatabaseCoordinator.peek());
        R26RuntimeDeviceTest.Sandbox sandbox=new R26RuntimeDeviceTest.Sandbox(target);DbStore store=null;RuntimeR27 rt=null;ResearchPoolR27 migrationPool=null;SQLiteDatabase root=null,ro=null;
        Map<String,Object> receipt=new LinkedHashMap<>();receipt.put("scope","API34_SYNTHETIC_NATIVE_RUNTIME_TURN_NOT_NATURAL_NOT_PHYSICAL_PHONE");
        try{
            store=new DbStore(sandbox);
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_EVOLUTION_PIN_SCHEMA",db->{RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);return 0;});
            long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001"),opened=System.currentTimeMillis()-60000L*5201;assertEquals(4200,store.saveDraws(draws(first,0,4200,opened)));
            root=SQLiteDatabase.openDatabase(store.readable().getPath(),null,SQLiteDatabase.OPEN_READONLY);migrationPool=new ResearchPoolR27(store,"SYNTHETIC_EVOLUTION_PIN_MIGRATION",2);SQLiteDatabase initial=ResearchRouteR29.openWorkerReader(sandbox);
            try{assertEquals("ACTIVE_INDEPENDENT_RESEARCH_WRITER",ResearchCutoverR29.run(store,migrationPool.new Worker(initial,root,"pin-migration")));}finally{initial.close();}migrationPool.close();migrationPool=null;
            ro=ResearchRouteR29.openWorkerReader(sandbox);ReplayDatasetR27 original=StoreR27.loadDataset(ro,RuntimeR27.DATASET_ROWS);assertEquals(4200,original.size());
            EvolutionLoopR27 before=midGeneration(original);String saved=before.snapshot();long savedRng=before.rng.state;save(store,before,saved,original);
            rt=new RuntimeR27(store,"SYNTHETIC_BEFORE_PIN_RESTART");rt.evoData=original;rt.evoCache=new FeatureCacheR27(original);rt.evo=before;
            ReplayDatasetR27 current=replaceHotTail(store,first,4200,opened);assertEquals(5000,current.size());assertNotEquals(original.datasetHash,current.datasetHash);assertEquals(0,n(ro,"SELECT count(*) FROM draws WHERE issue='"+original.issues[0]+"'"));
            receipt.put("hotTailAdvancedAndExpired",true);receipt.put("originalRows",4200);receipt.put("newRows",1000);receipt.put("currentRows",5000);
            rt.close();rt=null;ro.close();ro=null;root.close();root=null;store.close();store=null;
            store=new DbStore(sandbox);root=SQLiteDatabase.openDatabase(store.readable().getPath(),null,SQLiteDatabase.OPEN_READONLY);ro=ResearchRouteR29.openWorkerReader(sandbox);
            current=StoreR27.loadDataset(ro,RuntimeR27.DATASET_ROWS);assertEquals(5000,current.size());rt=new RuntimeR27(store,"SYNTHETIC_AFTER_PIN_RESTART");rt.evoData=current;rt.evoCache=new FeatureCacheR27(current);
            RuntimeR27.EvolutionTask evolution=task(rt);ResearchPoolR27.Worker worker=rt.pool.new Worker(ro,root,"pin-restore");RuntimeR27.EvolutionTask.Turn restored=evolution.new Turn(worker);
            assertNotNull(restored.pinned);assertEquals(original.datasetHash,restored.pinned.datasetHash);
            ReplayDatasetR27 pinned=EvolutionDatasetPinR29.decode(restored.pinned);assertEquals(4200,pinned.size());assertArrayEquals(original.ordinals,pinned.ordinals);assertArrayEquals(original.issues,pinned.issues);
            EvolutionLoopR27 exact=RuntimeR27.restoreExact(restored.saved,new EvolutionLoopR27.Config(),new FeatureCacheR27(pinned),()->123L);
            assertEquals(saved,exact.snapshot());assertEquals(savedRng,exact.rng.state);assertEquals(before.ledger().snapshot(),exact.ledger().snapshot());assertEquals(before.ledger().controlSnapshot(),exact.ledger().controlSnapshot());receipt.put("exactCheckpointAndLedgerRestored",true);
            StoreR27.EvolutionSink expectedSink=new StoreR27.EvolutionSink(RuntimeR27.LOOP,original.issues[original.size()-1]);exact.setSink(expectedSink);exact.leafWeight=Collections.emptyMap();exact.derivedLeaves=Collections.emptyList();exact.step();String expected=exact.snapshot();
            long deadline=System.nanoTime()+60_000_000_000L;ResearchPoolR27.Outcome outcome=null;int slices=0;
            while(System.nanoTime()<deadline){outcome=evolution.step(worker);slices++;if(outcome.units>0)break;assertEquals("COOPERATIVE_PRIVATE_EVOLUTION_TURN",outcome.reason);}
            assertNotNull(outcome);assertEquals("actual registered Turn must finish within the original 60s bound",1,outcome.units);assertNotNull(rt.evo);assertEquals(expected,rt.evo.snapshot());assertEquals(4,rt.evo.step);assertEquals(7,rt.evo.generation);assertEquals(original.datasetHash,rt.evoData.datasetHash);assertEquals(current.datasetHash,rt.pendingData.datasetHash);
            assertEquals(expected,text(ro,"SELECT snapshot FROM r27_evolution_cursor WHERE loop='MAIN'"));assertEquals(1,n(ro,"SELECT count(*) FROM "+EvolutionDatasetPinR29.META+" WHERE dataset_hash='"+original.datasetHash+"'"));receipt.put("actualRuntimeTurnResumedPinned4200",true);receipt.put("nextGateSnapshotBitIdentical",true);receipt.put("current5000WaitsForGenerationBoundary",true);receipt.put("cooperativeSlices",slices);
            rt.close();rt=null;
            // At the actual generation boundary the exact old checkpoint is restored first, then the new
            // 5000-row input is bound before the DATASET step. It must not run another old-data generation.
            EvolutionLoopR27 boundary=EvolutionLoopR27.restore(saved,new EvolutionLoopR27.Config(),new FeatureCacheR27(original),null,()->123L);boundary.step=1;String boundarySnapshot=boundary.snapshot();save(store,boundary,boundarySnapshot,original);
            rt=new RuntimeR27(store,"SYNTHETIC_PIN_GENERATION_BOUNDARY");rt.evoData=current;rt.evoCache=new FeatureCacheR27(current);RuntimeR27.EvolutionTask boundaryTask=task(rt);ResearchPoolR27.Worker boundaryWorker=rt.pool.new Worker(ro,root,"pin-boundary");
            deadline=System.nanoTime()+60_000_000_000L;ResearchPoolR27.Outcome boundaryOutcome=null;
            while(System.nanoTime()<deadline){boundaryOutcome=boundaryTask.step(boundaryWorker);if(boundaryOutcome.units>0)break;assertEquals("COOPERATIVE_PRIVATE_EVOLUTION_TURN",boundaryOutcome.reason);}
            assertNotNull(boundaryOutcome);assertEquals(1,boundaryOutcome.units);assertNotNull(rt.evo);assertEquals(2,rt.evo.step);assertEquals(current.datasetHash,rt.evo.context().data.datasetHash);assertEquals(current.datasetHash,rt.evoData.datasetHash);assertNull("the consumed future input cannot be queued for another boundary",rt.pendingData);assertEquals(current.datasetHash,text(ro,"SELECT dataset_hash FROM r27_evolution_cursor WHERE loop='MAIN'"));receipt.put("boundaryRebindBeforeDatasetStep",true);rt.close();rt=null;
            // A genuine legacy checkpoint with no pin must wait rather than replace old state with a new loop.
            ReplayDatasetR27 missing=ReplayDatasetR27.synthetic(4200,999,null);EvolutionLoopR27 legacy=midGeneration(missing);String legacySnapshot=legacy.snapshot();final String missingHash=missing.datasetHash;final long legacyRng=legacy.rng.state;
            ResearchRouteR29.write(store,"SYNTHETIC_LEGACY_PIN_MISSING",db->{db.execSQL("DELETE FROM r29_evolution_checkpoint_checksum WHERE loop='MAIN'");db.execSQL("UPDATE r27_evolution_cursor SET generation=7,step=3,dataset_hash=?,rng_state=?,snapshot=?,updated_at=? WHERE loop='MAIN'",new Object[]{missingHash,legacyRng,legacySnapshot,System.currentTimeMillis()});return 0;});
            rt=new RuntimeR27(store,"SYNTHETIC_MISSING_LEGACY_PIN");rt.evoData=current;rt.evoCache=new FeatureCacheR27(current);ResearchPoolR27.Worker legacyWorker=rt.pool.new Worker(ro,root,"legacy-pin");ResearchPoolR27.Outcome waiting=task(rt).step(legacyWorker);assertEquals(0,waiting.units);assertTrue(waiting.reason,waiting.reason.startsWith("WAITING_IMMUTABLE_EVOLUTION_DATASET_PIN:"));assertNull(rt.evo);assertEquals(legacySnapshot,text(ro,"SELECT snapshot FROM r27_evolution_cursor WHERE loop='MAIN'"));receipt.put("missingLegacyPinWaitsWithoutReplacingCursor",true);rt.close();rt=null;
            // V1's hash excludes absolute ordinal offsets; matching it cannot prove a legacy input.
            ReplayDatasetR27 unproven=ReplayDatasetR27.synthetic(5000,7777,null);assertNull(EvolutionDatasetPinR29.read(ro,unproven.datasetHash));List<ReplayDatasetR27.Row> shiftedRows=new ArrayList<>();for(int i=0;i<unproven.size();i++)shiftedRows.add(new ReplayDatasetR27.Row(unproven.issues[i],unproven.ordinals[i]+10,unproven.draws[i]));ReplayDatasetR27 shifted=ReplayDatasetR27.of(shiftedRows);assertEquals(unproven.datasetHash,shifted.datasetHash);
            EvolutionLoopR27 sameHashLegacy=midGeneration(unproven);String sameHashSnapshot=sameHashLegacy.snapshot();final String currentHash=unproven.datasetHash;final long sameHashRng=sameHashLegacy.rng.state;
            ResearchRouteR29.write(store,"SYNTHETIC_LEGACY_PIN_HASH_COLLISION",db->{db.execSQL("UPDATE r27_evolution_cursor SET dataset_hash=?,rng_state=?,snapshot=?,updated_at=? WHERE loop='MAIN'",new Object[]{currentHash,sameHashRng,sameHashSnapshot,System.currentTimeMillis()});return 0;});
            rt=new RuntimeR27(store,"SYNTHETIC_UNPROVEN_V1_HASH");rt.evoData=shifted;rt.evoCache=new FeatureCacheR27(shifted);ResearchPoolR27.Outcome sameHashWait=task(rt).step(rt.pool.new Worker(ro,root,"same-hash-unproven"));assertEquals(0,sameHashWait.units);assertTrue(sameHashWait.reason,sameHashWait.reason.startsWith("WAITING_IMMUTABLE_EVOLUTION_DATASET_PIN:"));assertNull(rt.evo);assertEquals(sameHashSnapshot,text(ro,"SELECT snapshot FROM r27_evolution_cursor WHERE loop='MAIN'"));receipt.put("unprovenSameV1HashCannotResetOrResumeCursor",true);rt.close();rt=null;
            // Corrupt JSON with an existing verified pin remains a failure, never a fresh-loop fallback.
            final String originalHash=original.datasetHash;
            ResearchRouteR29.write(store,"SYNTHETIC_BAD_CHECKPOINT_JSON",db->{db.execSQL("UPDATE r27_evolution_cursor SET dataset_hash=?,snapshot='{broken',updated_at=? WHERE loop='MAIN'",new Object[]{originalHash,System.currentTimeMillis()});return 0;});
            rt=new RuntimeR27(store,"SYNTHETIC_CORRUPT_CHECKPOINT");rt.evoData=current;rt.evoCache=new FeatureCacheR27(current);RuntimeR27.EvolutionTask.Turn bad=task(rt).new Turn(rt.pool.new Worker(ro,root,"bad-pin"));boolean failed=false;try{bad.compute();}catch(RuntimeException expectedFailure){failed=true;}assertTrue("corrupt checkpoint was silently replaced",failed);assertNull(rt.evo);assertEquals("{broken",text(ro,"SELECT snapshot FROM r27_evolution_cursor WHERE loop='MAIN'"));receipt.put("badSnapshotRemainsFailureAndUnchanged",true);
            assertTrue(n(ro,"SELECT max(length(chunk_blob)) FROM "+EvolutionDatasetPinR29.CHUNKS)<=128*1024);receipt.put("nativePinChunksAtMost128KiB",true);receipt.put("status","PASS_ANDROID_ENGINEERING_SUBSET");
        }finally{
            if(rt!=null)rt.close();if(migrationPool!=null)migrationPool.close();if(ro!=null)ro.close();if(root!=null)root.close();if(store!=null)store.close();
            try(FileOutputStream out=new FileOutputStream(new File(target.getFilesDir(),"r29-evolution-dataset-pin-device.json"))){out.write(JsonR27.write(receipt).getBytes(java.nio.charset.StandardCharsets.UTF_8));}R26RuntimeDeviceTest.remove(sandbox.root);
        }
    }
}
