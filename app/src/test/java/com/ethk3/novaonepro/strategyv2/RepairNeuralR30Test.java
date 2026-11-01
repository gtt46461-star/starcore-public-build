package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public final class RepairNeuralR30Test {
    static AtomNetSessionR30 fixture(){return new AtomNetSessionR30(HashingSU1.text("fixture"),"000000000999",999,12,80,40,20);}
    @Test public void preparedSamplesAreNotGradientOrPublishedWeights(){
        AtomNetSessionR30 s=fixture();s.materializedSamples=30;
        Map<String,Object> initial=s.report();assertEquals("MATERIALIZE",initial.get("displayPhase"));assertEquals(0L,initial.get("primaryTrainingSamplesCommitted"));assertNull(initial.get("weightsTrainedThrough"));assertEquals(false,initial.get("publicationReady"));
        s.cachePeriod=38;s.materializedSamples=140;s.work(R30TenPositionTrainingTest::sample);
        assertEquals(64L,s.report().get("primaryTrainingSamplesCommitted"));assertNull(s.report().get("weightsTrainedThrough"));
    }
    @Test public void allOptimizationAndRiskValidationExcludeOriginalConfirmRows(){
        AtomNetSessionR30 s=fixture();AtomNetSessionR30.Samples researchOnly=index->{if(index>=120)throw new AssertionError("FORMAL_CONFIRM_SAMPLE_READ:"+index);return R30TenPositionTrainingTest.sample(index);};
        int steps=0;while(!s.ready()){s.work(researchOnly);assertTrue(++steps<1000);}
        assertEquals(120,s.trainer.buffer.size());s.trainer.sinceCheck=10L*AtomNetTrainerR27.CHECK_EVERY-10;
        for(int p=1;p<=10;p++)s.online(new AtomNetTrainerR27.Sample(AtomNetR27.positionedInput(new double[]{.2,.7},p),0,p-1,AtomNetSessionR30.eventKey("000000001000",p)),"000000001000",1000,p);
        assertEquals("EVAL_ONLINE_DEVELOPMENT_VALIDATION",s.phase);while(!s.ready())s.work(researchOnly);
        assertEquals("DEVELOPMENT_VALIDATION_FROM_ORIGINAL_SELECT_ONLY",s.report().get("validationRole"));
        byte[] before=s.trainer.net.encode();for(ResearchContractR30R1.Role forbidden:new ResearchContractR30R1.Role[]{ResearchContractR30R1.Role.CONFIRM,ResearchContractR30R1.Role.FUTURE,ResearchContractR30R1.Role.SYNTHETIC})try{s.trainer.checkDevelopment(Collections.singletonList(R30TenPositionTrainingTest.sample(1)),forbidden);fail(forbidden.name());}catch(IllegalArgumentException expected){}
        assertArrayEquals(before,s.trainer.net.encode());
    }
    @Test public void oldOuterRestoresFullNumericStateAndNamesUnknownValidationOrigin()throws Exception{
        AtomNetSessionR30 old=fixture();old.work(R30TenPositionTrainingTest::sample);byte[] v1;try(java.io.InputStream in=getClass().getResourceAsStream("/r30-repair-legacy-v1.bin")){assertNotNull(in);v1=in.readAllBytes();}byte[] original=v1.clone();assertEquals(0x30A70001,java.nio.ByteBuffer.wrap(v1).getInt());
        AtomNetSessionR30 restored=AtomNetSessionR30.decode(v1,StatsR27.sha256(v1));assertArrayEquals(old.trainer.net.encode(),restored.trainer.net.encode());assertEquals(old.trainer.rng.state,restored.trainer.rng.state);assertEquals(old.ftrl.updates,restored.ftrl.updates);assertEquals(old.offset,restored.offset);assertTrue(restored.legacyValidationOriginUnknown);assertTrue(restored.needsValidationRoleMigration);assertArrayEquals(original,v1);
        restored.work(R30TenPositionTrainingTest::sample);assertFalse(restored.needsValidationRoleMigration);assertTrue(restored.legacyValidationOriginUnknown);
    }
    @Test public void everyPhaseBuildsACheckpointWithUnknownPendingAllowed(){AtomNetSessionR30 s=fixture();s.cachePeriod=38;s.materializedSamples=140;int n=0;do{AtomNetStoreR30.checkpoint(new ResearchWriteQueueR27.Batch(),s,1);s.work(R30TenPositionTrainingTest::sample);assertTrue(++n<1000);}while(!s.ready());AtomNetStoreR30.checkpoint(new ResearchWriteQueueR27.Batch(),s,2);}
    @Test public void modelDescriptorBindsTheExactAvailabilityAndTrainingBoundary(){
        byte[] weights=new AtomNetR27(12,7).encode();ModelSurfaceProviderR30.FrozenModelRef a=new ModelSurfaceProviderR30.FrozenModelRef(weights,99,10000),b=new ModelSurfaceProviderR30.FrozenModelRef(weights,99,10001),c=new ModelSurfaceProviderR30.FrozenModelRef(weights,98,10000);
        assertNotEquals(a.modelHash,b.modelHash);assertNotEquals(a.modelHash,c.modelHash);assertEquals(a.weightsHash,b.weightsHash);assertEquals(a.modelHash,ModelSurfaceProviderR30.FrozenModelRef.archived(a.modelHash,weights,99,10000).modelHash);
        try{ModelSurfaceProviderR30.FrozenModelRef.archived(a.modelHash,weights,98,10000);fail("wrong source boundary accepted");}catch(ModelSurfaceProviderR30.Blocked expected){}
    }
    @Test public void preregisteredOnlineWindowCannotConsumeOldOrAlreadyObservedInput(){
        OnlineResearchWindowR30R1.Window w=new OnlineResearchWindowR30R1.Window("fixture",100,200,5000);w.require(100,5001);
        for(long[] invalid:new long[][]{{99,9000},{201,9000},{100,5000},{100,4999}})try{w.require(invalid[0],invalid[1]);fail("outside declared role/time");}catch(IllegalStateException expected){}
    }
}
