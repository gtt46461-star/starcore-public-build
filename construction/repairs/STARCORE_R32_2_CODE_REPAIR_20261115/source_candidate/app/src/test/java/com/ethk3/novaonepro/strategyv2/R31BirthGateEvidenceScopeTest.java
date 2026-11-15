package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import org.junit.Test;

/** R31-B07-C02#A3: an identical behavior signature from another position, another probe input hash or another probe
 * window/role is not the same evidence; copied registrations keep their full reference but stay in their own scope. */
public final class R31BirthGateEvidenceScopeTest {
    private int r31LeasesBeforeTest;
    @org.junit.Before public void r31OutstandingLeaseGuardBefore(){FeatureCacheR27.trimRegisteredCachesR31();r31LeasesBeforeTest=MemoryBudgetR31.processBudget().explicitOwnerLeaseCountR31();}
    @org.junit.After public void r31OutstandingLeaseGuardAfter(){
        FeatureCacheR27.trimRegisteredCachesR31();MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();int after=memory.explicitOwnerLeaseCountR31();
        org.junit.Assert.assertTrue("R31_OUTSTANDING_LEASES before="+r31LeasesBeforeTest+" after="+after+" owners="+memory.explicitOwnerLeasesR31(64),after<=r31LeasesBeforeTest);
    }
    private static final int[] SIG = {11, 205, 3307, 4410, 5123};
    @Test public void signaturesAreDuplicatesOnlyWithinTheSameInputRoleAndPosition() {
        try (FeatureCacheR27 a = new FeatureCacheR27(ReplayDatasetR27.synthetic(700, 9101, null));
             FeatureCacheR27 b = new FeatureCacheR27(ReplayDatasetR27.synthetic(700, 9102, null))) {
            assertNotEquals(a.data.datasetHash, b.data.datasetHash);
            BirthGateR27 gateA = new BirthGateR27(a, 700);
            gateA.register("policy-1", SIG.clone(), 3);
            assertEquals("same input, role and position is the same evidence", "policy-1", gateA.duplicateOf(SIG.clone(), 3));
            assertNull("another position is different evidence", gateA.duplicateOf(SIG.clone(), 4));
            BirthGateR27 sameInput = new BirthGateR27(a, 700);
            sameInput.copyValidatedRegistrationsR30(gateA);
            assertEquals("a copy over the same input keeps detecting the duplicate", "policy-1", sameInput.duplicateOf(SIG.clone(), 3));
            BirthGateR27 otherInput = new BirthGateR27(b, 700);
            otherInput.copyValidatedRegistrationsR30(gateA);
            assertNull("an identical signature over another input hash is not the same evidence", otherInput.duplicateOf(SIG.clone(), 3));
            assertFalse(otherInput.sameTarget("policy-1", 3));
            assertArrayEquals("the copied formal evidence keeps its complete reference", SIG, otherInput.signatureOf("policy-1"));
            assertEquals(1, otherInput.registered());
            BirthGateR27 otherWindow = new BirthGateR27(a, 650);
            otherWindow.copyValidatedRegistrationsR30(gateA);
            assertNotEquals(gateA.evidenceScopeR31(), otherWindow.evidenceScopeR31());
            assertNull("another probe window is not the same evidence", otherWindow.duplicateOf(SIG.clone(), 3));
            otherInput.register("policy-2", SIG.clone(), 3);
            assertEquals("a registration in the new scope is found there", "policy-2", otherInput.duplicateOf(SIG.clone(), 3));
            otherInput.unregister("policy-2");
            assertNull(otherInput.duplicateOf(SIG.clone(), 3));
        }
    }
}
