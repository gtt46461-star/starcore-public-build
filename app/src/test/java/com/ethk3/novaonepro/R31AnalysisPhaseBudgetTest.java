package com.ethk3.novaonepro;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public final class R31AnalysisPhaseBudgetTest {
    @Test public void pollCadenceAnchorsToStartCountsLateSlotsAndNeverReplaysMissedTicks(){
        AutoFetchService.PollCadenceR31 cadence=new AutoFetchService.PollCadenceR31();cadence.beginSession(100);cadence.wake(100);
        assertEquals(700,cadence.nextDelay(400,1000,"NORMAL",10));
        cadence.wake(1100);assertEquals(10,cadence.nextDelay(4200,1000,"OVERRUN",10));cadence.wake(4210);
        assertEquals(2L,cadence.snapshot().get("missed_tick"));assertEquals(890,cadence.nextDelay(4210,1000,"NORMAL",10));
    }
    @Test public void backoffAndExplicitWakePreserveMissedDenominatorAndInactiveTime(){
        AutoFetchService.PollCadenceR31 cadence=new AutoFetchService.PollCadenceR31();cadence.beginSession(100);cadence.wake(100);cadence.wake(500);
        assertEquals(1L,cadence.snapshot().get("earlyExplicitWakes"));assertEquals(8000,cadence.nextDelay(600,8000,"HTTP_429",10));cadence.wake(8600);
        assertEquals(7L,cadence.snapshot().get("missed_tick"));cadence.endSession(8700);cadence.beginSession(100000);cadence.wake(100000);
        assertEquals(7L,cadence.snapshot().get("missed_tick"));assertEquals(91300L,cadence.snapshot().get("excludedInactiveMs"));
        assertFalse(ServiceHeartbeatStore.researchProgressAcceptableR31("UNKNOWN"));assertTrue(ServiceHeartbeatStore.researchProgressAcceptableR31("PROGRESS_PENDING"));assertTrue(ServiceHeartbeatStore.researchProgressAcceptableR31("IDLE_WAITING_INPUT"));
    }
    @Test public void pulsesAndRepeatedPhaseEntryCannotExtendBudget(){
        AtomicLong now=new AtomicLong(100);AnalysisSingleFlightR36.PhaseBudgetR31 budget=new AnalysisSingleFlightR36.PhaseBudgetR31(2000,now::get);
        budget.begin(AnalysisSingleFlightR36.PhaseR31.PREPARE,100);now.set(180);budget.begin(AnalysisSingleFlightR36.PhaseR31.PREPARE,1000);now.set(201);
        assertEquals("PHASE_BUDGET_PREPARE",budget.exceeded());
    }
    @Test public void fixedPredrawDeadlineDominatesEachPhaseAllowance(){
        AtomicLong now=new AtomicLong(100);AnalysisSingleFlightR36.PhaseBudgetR31 budget=new AnalysisSingleFlightR36.PhaseBudgetR31(150,now::get);
        budget.begin(AnalysisSingleFlightR36.PhaseR31.INFER,3000);now.set(149);assertEquals("",budget.exceeded());now.set(150);assertEquals("FIXED_PREDRAW_DEADLINE",budget.exceeded());
    }
    @Test public void committedPublicationCannotBeCancelledOrReclassifiedByLateWork(){
        AtomicLong now=new AtomicLong(100);AnalysisSingleFlightR36.PhaseBudgetR31 budget=new AnalysisSingleFlightR36.PhaseBudgetR31(150,now::get);
        budget.begin(AnalysisSingleFlightR36.PhaseR31.COMMIT,40);now.set(120);budget.published();now.set(1000);budget.begin(AnalysisSingleFlightR36.PhaseR31.INFER,1);
        assertEquals("",budget.exceeded());assertEquals("PUBLISHED",budget.snapshot().get("phase"));assertEquals(true,budget.snapshot().get("committed"));
    }
    @Test public void phaseRegressionIsRejectedAndSourceChoiceIsHonest(){
        AtomicLong now=new AtomicLong(100);AnalysisSingleFlightR36.PhaseBudgetR31 budget=new AnalysisSingleFlightR36.PhaseBudgetR31(2000,now::get);
        budget.begin(AnalysisSingleFlightR36.PhaseR31.INFER,300);
        try{budget.begin(AnalysisSingleFlightR36.PhaseR31.PREPARE,100);fail("phase moved backwards");}catch(IllegalStateException expected){assertEquals("R31_ANALYSIS_PHASE_REGRESSION",expected.getMessage());}
        Map<String,Object> snapshot=budget.snapshot();assertEquals("FULL_REGISTERED_RUNTIME",snapshot.get("sourceChoice"));assertEquals("UNSUPPORTED_UNTIL_PREREGISTERED_QUALIFIED_IMPLEMENTATION",snapshot.get("fastFallback"));
        try{snapshot.put("committed",true);fail("mutable evidence snapshot");}catch(UnsupportedOperationException expected){}
    }
}
