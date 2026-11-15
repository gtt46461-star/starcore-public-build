package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class RepairNullBudgetR30Test {
 @Test public void zeroEventsHaveAWideNonzeroUpperBoundAndDoNotProveZeroRisk(){Map<String,Object> n=NullPipelineSimulationR30.binomial(0,2);assertEquals(2L,n.get("independentReplicates"));assertTrue(((Number)n.get("upper95")).doubleValue()>.5);assertTrue(((Number)n.get("oneSided95ZeroEventUpper")).doubleValue()>.7);assertNull(NullPipelineSimulationR30.binomial(0,0).get("upper95"));}
 @Test public void productionBudgetAndRestoreNeverSilentlyBecomeTwoTests(){NullPipelineSimulationR30 r=new NullPipelineSimulationR30(RepairPairedResearchR30Test.SOURCE);assertEquals(20,r.summary().get("plannedNullTrials"));assertEquals(0,r.summary().get("completedNullTrials"));assertEquals(JsonR27.write(r.summary()),JsonR27.write(NullPipelineSimulationR30.restore(r.snapshot()).summary()));assertFalse(r.complete());assertEquals(false,r.summary().get("mainAlphaBudgetSpent"));}
}
