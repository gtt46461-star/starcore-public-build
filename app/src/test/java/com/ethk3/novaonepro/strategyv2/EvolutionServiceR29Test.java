package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import java.util.concurrent.atomic.*;
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
}
