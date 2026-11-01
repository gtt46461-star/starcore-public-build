package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class RepairTaskProgressR30Test {
    @Test public void phaseCursorCannotBecomeCalendarLag(){
        TaskProgressR30R1 phase=new TaskProgressR30R1(TaskProgressR30R1.Unit.PHASE,18460,18461,1,-1,1195);
        assertEquals(TaskProgressR30R1.State.STALLED,phase.state(1200,false,true));assertNull(phase.oldestLag(1200));
        TaskProgressR30R1 idle=new TaskProgressR30R1(TaskProgressR30R1.Unit.PHASE,18460,18460,0,-1,-1);
        assertEquals(TaskProgressR30R1.State.IDLE_WAITING_INPUT,idle.state(1200,false,false));
        assertEquals(Long.valueOf(7),new TaskProgressR30R1(TaskProgressR30R1.Unit.ISSUE,1192,1200,8,1193,1199).oldestLag(1200));
    }
    @SuppressWarnings("unchecked") @Test public void all57OwnersAppearAndMissingFactsCannotProduceZeroM12(){
        Map<String,Object> result=TaskProgressR30R1.summarize(Collections.emptyList(),Collections.emptyList(),1200);
        assertEquals(59,((List<?>)result.get("tasks")).size());Map<String,Object> summary=(Map<String,Object>)result.get("summary");
        assertEquals(59L,summary.get("unknown"));assertEquals(57,summary.get("originalEntrances"));assertEquals(2,summary.get("additionalRepairTasks"));assertNull(summary.get("m12"));assertNull(summary.get("oldestBacklogIssues"));
        assertEquals(TaskProgressR30R1.Unit.PHASE,ResearchTaskRegistryR30R1.require("r27-evolution").unit);
        assertEquals(TaskProgressR30R1.Unit.POSITION_SAMPLE,ResearchTaskRegistryR30R1.require("r27-atomnet").unit);
        assertEquals(TaskProgressR30R1.Unit.TIME_TICK,ResearchTaskRegistryR30R1.require("r27-health").unit);
    }
    @Test public void uncommittedEvolutionDraftDoesNotAdvanceOriginalRngOrPending(){
        EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.nullControl=false;
        EvolutionLoopR27 original=new EvolutionLoopR27(cfg,new FeatureCacheR27(ReplayDatasetR27.synthetic(3000,71,null)),null,()->100L);
        original.addPendingForTest(NullPipelineSimulationR30.neutralSeed());String committed=original.snapshot();
        EvolutionLoopR27 draft=original.preparedForkR30();assertEquals(committed,draft.snapshot());
        draft.rng.nextLong();draft.step();assertNotEquals(committed,draft.snapshot());assertEquals(committed,original.snapshot());
        EvolutionLoopR27 retry=original.preparedForkR30();retry.rng.nextLong();retry.step();assertEquals(draft.snapshot(),retry.snapshot());
    }
    @Test public void busyPreparedWriteRetainsStackAndYieldsOtherWork()throws Exception{
        AtomicInteger prepared=new AtomicInteger(),attempts=new AtomicInteger(),published=new AtomicInteger();
        CooperativeContinuationR26<Integer> work=new CooperativeContinuationR26<>("repair-busy-fixture",()->{
            prepared.incrementAndGet();CommitEnvelopeR30R1 e=new CommitEnvelopeR30R1(ResearchContractR30R1.hash("fixture"),4,8,new byte[]{9});
            while(e.attempt((id,f,r,state)->attempts.incrementAndGet()<3?CommitEnvelopeR30R1.Outcome.RETRY_LATER:CommitEnvelopeR30R1.Outcome.COMMITTED)!=CommitEnvelopeR30R1.Outcome.COMMITTED)CooperativeContinuationR26.yieldPreparedWrite();
            assertArrayEquals(new byte[]{9},e.publishedState());published.incrementAndGet();return 1;
        },()->false,null,System::nanoTime);
        try{assertFalse(work.slice(200).done);assertEquals(0,published.get());assertEquals(1,attempts.get());
            AtomicInteger other=new AtomicInteger();other.incrementAndGet();assertFalse(work.slice(200).done);assertEquals(1,other.get());
            assertTrue(work.slice(200).done);assertEquals(1,prepared.get());assertEquals(3,attempts.get());assertEquals(1,published.get());
        }finally{work.close();}
    }
}
