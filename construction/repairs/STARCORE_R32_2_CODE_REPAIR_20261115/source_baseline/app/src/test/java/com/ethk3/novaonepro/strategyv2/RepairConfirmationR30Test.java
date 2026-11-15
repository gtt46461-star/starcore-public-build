package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public final class RepairConfirmationR30Test {
    @Test public void negativeAndInsufficientAreCompleteOutcomesInsteadOfRegistrationFailures(){
        assertEquals("PENDING_COMPUTATION",ConfirmationViewR30R1.state("PENDING",null,false,0));
        assertEquals("COMPLETED_WITH_INSUFFICIENT_EVIDENCE",ConfirmationViewR30R1.state("DONE","INSUFFICIENT",false,0));
        assertEquals("COMPLETED_NO_VERIFIED_IMPROVEMENT",ConfirmationViewR30R1.state("DONE","COMPLETED",false,0));
        assertEquals("VERIFIED_WAITING_REGISTRATION",ConfirmationViewR30R1.state("DONE","COMPLETED",true,0));
        assertEquals("VERIFIED_AND_REGISTERED",ConfirmationViewR30R1.state("DONE","COMPLETED",true,100));
        assertTrue(ConfirmationViewR30R1.state("DONE","COMPLETED",false,100).startsWith("DATA_CONFLICT"));
        assertEquals("UNKNOWN_OR_FAILED_COMPUTATION",ConfirmationViewR30R1.state("FAILED",null,false,0));
    }
    @Test public void originalGateThresholdsAreImmutableAndV2HasNoAuthority(){
        assertEquals("R27_GATE_V1",CertificateGateR27.contract().get("activeGate"));assertEquals(250,CertificateGateR27.MIN_ACTS);assertEquals(.34,CertificateGateR27.MIN_HR,0);assertEquals(6,CertificateGateR27.MAX_LOSS_STREAK);assertFalse(GateV2R29.ENABLED);
        GateV2R29.Evidence e=new GateV2R29.Evidence();e.policyHash="fixture";Map<?,?> provenance=(Map<?,?>)GateV2R29.evaluate(e).sections.get("provenance");assertEquals(false,provenance.get("enabled"));assertTrue(provenance.get("authority").toString().contains("REPORT_ONLY"));
    }
    @Test public void completeInputHashIncludesPairedControlsAndProbabilitiesAndCannotDriftBeforeRecording(){
        GateV2R29.Evidence e=new GateV2R29.Evidence();e.policyHash="fixture";e.p=new double[]{.3};e.y=new int[]{1};e.actOrdinal=new long[]{100};String first=GateV2R29.inputHash(e);e.p[0]=.31;assertNotEquals(first,GateV2R29.inputHash(e));
        GateV2R29.Verdict before=GateV2R29.evaluate(e);e.pairedLcbVsI00=.01;
        try{GateV2R29.record(new ResearchWriteQueueR27.Batch(),e,before);fail("stale V2 input silently recorded");}catch(IllegalStateException expected){}
    }
    @Test public void aPositiveResultForAnotherPacketCannotCompleteThisOne(){
        EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.nullControl=false;EvolutionLoopR27 loop=new EvolutionLoopR27(cfg,new FeatureCacheR27(ReplayDatasetR27.synthetic(3000,41,null)),null,()->1L);loop.champion=NullPipelineSimulationR30.neutralSeed();loop.i00Champion=loop.champion;
        FrozenConfirmationR30.Task task=FrozenConfirmationR30.freeze(loop,1000);assertNotNull(task);Map<String,Object> forged=new LinkedHashMap<>();forged.put("taskKey","another-packet");forged.put("datasetHash",task.datasetHash);forged.put("holdoutBlock",task.block);forged.put("verified",true);forged.put("status","COMPLETED");
        try{FrozenConfirmationStoreR30.complete(new ResearchWriteQueueR27.Batch(),task,new FrozenConfirmationR30.Result(forged));fail("foreign result accepted");}catch(IllegalArgumentException expected){}
    }
}
