package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import java.util.concurrent.atomic.*;
import com.ethk3.novaonepro.AtomReplayR27;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real pure numerical work must release the one-worker admission slot between quanta. Synthetic engineering,
 * not device latency or predictive quality. The same access gate is used by RuntimeR27's production consumers. */
public final class EvolutionServiceR29Test {
    @Test public void privateTurnCannotOverlapAnExistingReaderOrExposeHalfComputedState(){
        RuntimeR27.EvolutionAccess access=new RuntimeR27.EvolutionAccess();
        try(RuntimeR27.EvolutionAccess.Lease reader=access.read()){assertNotNull(reader);assertFalse("an active numerical reader must drain first",access.begin());}
        assertTrue(access.begin());assertNull("dependent tasks must wait without reading a partial turn",access.read());
        access.publish();try(RuntimeR27.EvolutionAccess.Lease reader=access.read()){assertNotNull(reader);}
    }
    static PolicySpecR27 policy(){return new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,128)),1,PolicySpecR27.Rule.QUANTILE,0,.2,1,false,.01,.01,null);}
    @Test public void actualReplayYieldsItsOnlySlotAndResumesIdentically()throws Exception{
        ReplayDatasetR27 d=ReplayDatasetR27.synthetic(5000,29,new int[]{900,2500});PolicySpecR27 spec=policy();double[] scores=ReplayEvaluatorR27.scores(spec,new FeatureCacheR27(d));
        ReplayEvaluatorR27.Trace expected=ReplayEvaluatorR27.run(spec,scores,d,d.draws,1,0,d.size());
        AtomicInteger active=new AtomicInteger(),completed=new AtomicInteger();AtomicBoolean narrow=new AtomicBoolean(true);String owner="R29_SINGLE_SLOT_"+UUID.randomUUID();
        try(CooperativeContinuationR26<ReplayEvaluatorR27.Trace> work=new CooperativeContinuationR26<>("R29-test-replay",()->{ReplayEvaluatorR27.Trace out=null;for(int i=0;i<8;i++)out=ReplayEvaluatorR27.run(spec,scores,d,d.draws,1,0,d.size());completed.incrementAndGet();return out;},narrow::get,null,System::nanoTime,owner)){
            assertTrue(ResearchSlotsR29.tryEnter(active,1));CooperativeContinuationR26.Slice<ReplayEvaluatorR27.Trace> slice;
            try{slice=work.slice(1);}finally{active.decrementAndGet();}
            assertFalse("narrow-window preemption must park at the actual evaluator poll",slice.done);assertEquals(0,completed.get());
            assertNotEquals(WaitKindR27.PROGRESS,WaitKindR27.classify(0,"COOPERATIVE_PRIVATE_EVOLUTION_TURN",null,0,-1,null,false).kind);
            for(int i=0;i<8;i++){assertTrue("legacy service must be admitted while evolution is parked",ResearchSlotsR29.tryEnter(active,1));active.decrementAndGet();}
            narrow.set(false);int slices=0;while(!slice.done&&slices++<10000){assertTrue(ResearchSlotsR29.tryEnter(active,1));try{slice=work.slice(1);}finally{active.decrementAndGet();}}
            assertTrue(slice.done);assertNull(slice.error);assertEquals(1,completed.get());assertArrayEquals(expected.pHit,slice.result.pHit,0);assertArrayEquals(expected.u,slice.result.u,0);assertArrayEquals(expected.k3,slice.result.k3);assertArrayEquals(expected.bias,slice.result.bias,0);
        }
        assertTrue(CooperativeContinuationR26.awaitOwnerStopped(owner,2000));
    }
    @Test public void stoppingAParkedNumericalTurnDrainsBeforeResourcesCanClose()throws Exception{
        AtomicBoolean preempt=new AtomicBoolean(true);String owner="R29_STOP_"+UUID.randomUUID();
        CooperativeContinuationR26<Integer> work=new CooperativeContinuationR26<>("R29-test-stop",()->{for(int i=0;i<1000;i++)CooperativeContinuationR26.poll();return 1;},preempt::get,null,System::nanoTime,owner);
        assertFalse(work.slice(1).done);work.close();assertTrue("owner must drain before its database readers are closed",CooperativeContinuationR26.awaitOwnerStopped(owner,2000));assertTrue(work.isDone());
    }
    @Test public void completeCheckpointParksAndResumesWithoutChangingAnyBytes()throws Exception{
        ReplayDatasetR27 d=ReplayDatasetR27.synthetic(1800,29,new int[0]);FeatureCacheR27 cache=new FeatureCacheR27(d);
        EvolutionLoopR27 loop=new EvolutionLoopR27(new EvolutionLoopR27.Config(),cache,null,()->123L);PolicySpecR27 spec=policy();
        for(int i=0;i<512;i++){
            EvolutionLoopR27.Member m=new EvolutionLoopR27.Member();m.spec=spec;m.tau=1;m.island="FREQ";loop.population.add(m);
            loop.addPendingForTest(spec);loop.archive.put(i,new EliteArchiveR27.Elite(spec,.1,1));
            loop.ledger().restore(i,"policy-"+i);loop.ledger().restoreControl(i,"control-"+i);
        }
        loop.live.add(spec);loop.liveTau.put(spec.policyHash(),1.0);String expected=loop.snapshot();
        AtomicBoolean narrow=new AtomicBoolean(true);String owner="R29_CHECKPOINT_"+UUID.randomUUID();
        try(CooperativeContinuationR26<String> work=new CooperativeContinuationR26<>("R29-test-checkpoint",loop::snapshot,narrow::get,null,System::nanoTime,owner)){
            CooperativeContinuationR26.Slice<String> slice=work.slice(1);assertFalse("checkpoint preparation must poll before completing its population/archive/receipt collections",slice.done);
            narrow.set(false);int n=0;while(!slice.done&&n++<10000)slice=work.slice(1);
            assertTrue(slice.done);assertNull(slice.error);assertEquals(expected,slice.result);
            assertEquals(JsonR27.write(JsonR27.parse(expected)),JsonR27.write(JsonR27.parseCooperative(slice.result)));
        }
        assertTrue(CooperativeContinuationR26.awaitOwnerStopped(owner,2000));
    }
    @Test public void aCheckpointWithoutPendingCandidatesStillHasAComputeQuantum()throws Exception{
        EvolutionLoopR27 loop=new EvolutionLoopR27(new EvolutionLoopR27.Config(),new FeatureCacheR27(ReplayDatasetR27.synthetic(1800,29,new int[0])),null,()->123L);
        for(int i=0;i<256;i++){EvolutionLoopR27.Member member=new EvolutionLoopR27.Member();member.spec=policy();member.tau=1;member.island="FREQ";loop.population.add(member);}
        String owner="R29_NO_PENDING_"+UUID.randomUUID();
        try(CooperativeContinuationR26<String> work=new CooperativeContinuationR26<>("R29-test-population-checkpoint",loop::snapshot,()->true,null,System::nanoTime,owner)){
            assertFalse("population and final JSON serialization cannot depend on a nonempty pending list to poll",work.slice(1).done);
        }
        assertTrue(CooperativeContinuationR26.awaitOwnerStopped(owner,2000));
    }
    @Test public void cancelledPrivateTurnDrainsThenDiscardsPartialLoopAndFamilyCache()throws Exception{
        RuntimeR27 rt=new RuntimeR27(null,"R29_ABORT_"+UUID.randomUUID());
        ReplayDatasetR27 d=ReplayDatasetR27.synthetic(1800,29,new int[0]);rt.evoData=d;rt.evoCache=new FeatureCacheR27(d);
        EvolutionLoopR27 committed=new EvolutionLoopR27(new EvolutionLoopR27.Config(),rt.evoCache,null,()->123L);rt.evo=committed;
        String saved=committed.snapshot();FeatureCacheR27 oldCache=rt.evoCache;
        RuntimeR27.EvolutionTask task=rt.new EvolutionTask();ResearchPoolR27.Worker worker=rt.pool.new Worker(null,null,"abort");
        java.lang.reflect.Field gateField=RuntimeR27.class.getDeclaredField("evolutionAccess");gateField.setAccessible(true);RuntimeR27.EvolutionAccess gate=(RuntimeR27.EvolutionAccess)gateField.get(rt);assertTrue(gate.begin());
        RuntimeR27.EvolutionTask.Turn pending=task.new Turn(worker);rt.evo=null;
        AtomicBoolean release=new AtomicBoolean();
        String owner=rt.pool.session()+":R29_EVOLUTION";
        pending.work=new CooperativeContinuationR26<>("R29-test-unsliced",()->{pending.loop=committed;committed.generation=999;while(!release.get())Thread.onSpinWait();CooperativeContinuationR26.poll();return pending;},()->false,null,System::nanoTime,owner);
        java.lang.reflect.Field turnField=task.getClass().getDeclaredField("turn");turnField.setAccessible(true);turnField.set(task,pending);
        try{
            try{task.step(worker);fail("the original unsliced capacity failure must remain visible");}catch(IllegalStateException capacity){assertEquals("R26_UNSLICED_COMPUTATION_CAPACITY",capacity.getMessage());}
            assertNull("the cancelled helper is still mutating private state",gate.read());assertFalse(pending.work.isDone());
            ResearchPoolR27.Outcome draining=task.step(worker);assertEquals(0,draining.units);assertEquals("COOPERATIVE_CANCELLED_EVOLUTION_DRAIN",draining.reason);
            release.set(true);assertTrue(CooperativeContinuationR26.awaitOwnerStopped(owner,2000));
            ResearchPoolR27.Outcome restored=task.step(worker);assertEquals(0,restored.units);assertEquals("COOPERATIVE_EVOLUTION_RESTORE_COMMITTED_CURSOR",restored.reason);
            assertNull(rt.evo);assertNotSame("a cancelled family update must not be reused",oldCache,rt.evoCache);assertNull(turnField.get(task));
            try(RuntimeR27.EvolutionAccess.Lease reader=gate.read()){assertNotNull(reader);}
            EvolutionLoopR27 recovered=EvolutionLoopR27.restore(saved,new EvolutionLoopR27.Config(),rt.evoCache,null,()->123L);assertEquals("discarded work cannot advance the durable checkpoint",saved,recovered.snapshot());
        }finally{release.set(true);pending.work.close();assertTrue(CooperativeContinuationR26.awaitOwnerStopped(owner,2000));rt.close();}
    }
    @Test public void actualAtomChunkYieldsItsOnlySlotAndResumesIdentically()throws Exception{
        ReplayDatasetR27 d=ReplayDatasetR27.synthetic(900,29,new int[0]);AtomReplayR27.Frame expected=AtomReplayR27.build(d,600,720);
        AtomicBoolean narrow=new AtomicBoolean(true);String owner="R29_ATOM_CHUNK_"+UUID.randomUUID();
        try(CooperativeContinuationR26<AtomReplayR27.Frame> work=new CooperativeContinuationR26<>("R29-test-atoms",()->AtomReplayR27.build(d,600,720),narrow::get,null,System::nanoTime,owner)){
            CooperativeContinuationR26.Slice<AtomReplayR27.Frame> slice=work.slice(1);assertFalse("the actual 120-row atom chunk must release a service quantum",slice.done);
            narrow.set(false);int n=0;while(!slice.done&&n++<10000)slice=work.slice(1);assertTrue(slice.done);assertNull(slice.error);
            for(int i=0;i<expected.rows();i++){assertArrayEquals(expected.values[i],slice.result.values[i],0);assertArrayEquals(expected.present[i],slice.result.present[i]);assertArrayEquals(expected.k3[i],slice.result.k3[i]);}
            assertArrayEquals(expected.hit,slice.result.hit);assertArrayEquals(expected.actual,slice.result.actual);assertArrayEquals(expected.position,slice.result.position);
        }
        assertTrue(CooperativeContinuationR26.awaitOwnerStopped(owner,2000));
    }
}
