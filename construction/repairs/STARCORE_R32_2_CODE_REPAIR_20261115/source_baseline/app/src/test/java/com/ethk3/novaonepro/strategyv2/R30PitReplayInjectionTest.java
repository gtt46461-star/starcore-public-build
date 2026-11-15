package com.ethk3.novaonepro.strategyv2;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/** R30-B03-01 counter-example: a replay sample that claims a model trained through its own target must be rejected
 * (left uncounted) and recorded; every other sample is unchanged. */
public final class R30PitReplayInjectionTest {
    // R31-ENG-02#A2: every test returns all explicit-owner memory leases it opened (production APIs only).
    private int r31LeasesBeforeTest;
    @org.junit.Before public void r31OutstandingLeaseGuardBefore(){FeatureCacheR27.trimRegisteredCachesR31();r31LeasesBeforeTest=MemoryBudgetR31.processBudget().explicitOwnerLeaseCountR31();}
    @org.junit.After public void r31OutstandingLeaseGuardAfter(){
        FeatureCacheR27.trimRegisteredCachesR31();MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();int after=memory.explicitOwnerLeaseCountR31();
        org.junit.Assert.assertTrue("R31_OUTSTANDING_LEASES before="+r31LeasesBeforeTest+" after="+after+" owners="+memory.explicitOwnerLeasesR31(64),after<=r31LeasesBeforeTest);
    }
    @After public void reset() { ReplayEvaluatorR27.pitInjectOrdinalR30 = Long.MIN_VALUE; ReplayEvaluatorR27.PIT_VIOLATIONS_R30.clear(); }

    @Test public void injectedTrainedThroughTargetIsRejectedAndRecorded() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(600, 5, new int[0]);
        PolicySpecR27 s = R27ReplayTest.spec(PolicyAstR27.op("zscore", PolicyAstR27.leaf("trans2")), 1);
        FeatureCacheR27 f = new FeatureCacheR27(d);
        ReplayEvaluatorR27.PIT_VIOLATIONS_R30.clear();
        ReplayEvaluatorR27.Trace clean = ReplayEvaluatorR27.run(s, f, d.draws, 1.0, 0, d.size());
        assertTrue("clean replay has no PIT violation", ReplayEvaluatorR27.PIT_VIOLATIONS_R30.isEmpty());
        int t = 300; assertFalse(clean.skipped[t]);
        ReplayEvaluatorR27.pitInjectOrdinalR30 = d.ordinals[t];
        ReplayEvaluatorR27.Trace injected = ReplayEvaluatorR27.run(s, f, d.draws, 1.0, 0, d.size());
        assertTrue("injected sample is not counted", injected.skipped[t]);
        Object[] v = ReplayEvaluatorR27.PIT_VIOLATIONS_R30.poll();
        assertNotNull("violation recorded", v);
        assertEquals(d.ordinals[t], ((Long) v[1]).longValue());
        assertTrue(String.valueOf(v[2]).startsWith("MODEL_SEES_TARGET"));
        for (int i = 0; i < d.size(); i++) if (i != t) {
            assertEquals(clean.skipped[i], injected.skipped[i]);
            assertEquals(clean.hit[i], injected.hit[i]);
            assertEquals(clean.act[i], injected.act[i]);
        }
    }
}
