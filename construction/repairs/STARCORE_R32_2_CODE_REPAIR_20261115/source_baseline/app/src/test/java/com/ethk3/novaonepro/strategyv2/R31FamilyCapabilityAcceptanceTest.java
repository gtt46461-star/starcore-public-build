package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import org.junit.Test;

/** R31-B07-C01#A1/#A3/#A4: every family row carries its input ABI, implementation, tasks, state serialization, owner,
 * cost class, dependency and an executed acceptance; a row is either executed or names why it did not run. The
 * compatibility numeric entries of the SU2 live loop only forward to the fenced R26 owner. */
public final class R31FamilyCapabilityAcceptanceTest {
    private int r31LeasesBeforeTest;
    @org.junit.Before public void r31OutstandingLeaseGuardBefore(){FeatureCacheR27.trimRegisteredCachesR31();r31LeasesBeforeTest=MemoryBudgetR31.processBudget().explicitOwnerLeaseCountR31();}
    @org.junit.After public void r31OutstandingLeaseGuardAfter(){
        FeatureCacheR27.trimRegisteredCachesR31();MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();int after=memory.explicitOwnerLeaseCountR31();
        org.junit.Assert.assertTrue("R31_OUTSTANDING_LEASES before="+r31LeasesBeforeTest+" after="+after+" owners="+memory.explicitOwnerLeasesR31(64),after<=r31LeasesBeforeTest);
    }
    static int[][] stream(int n, long seed) {
        Random r = new Random(seed); int[][] d = new int[n][];
        for (int t = 0; t < n; t++) { int[] v = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}; for (int k = 9; k > 0; k--) { int j = r.nextInt(k + 1), x = v[k]; v[k] = v[j]; v[j] = x; } d[t] = v; }
        return d;
    }
    @Test public void everyFamilyRowIsExecutedOrNamesItsDependency() {
        List<FamilyRegistryR26.CapabilityR31> rows = FamilyRegistryR26.capabilitiesR31(stream(48, 7));
        assertEquals(24, rows.size());
        Map<String, String> red = new TreeMap<>(); Set<String> ids = new HashSet<>();
        for (FamilyRegistryR26.CapabilityR31 row : rows) {
            assertTrue(ids.add(row.id));
            for (String field : new String[]{row.inputAbi, row.tasks, row.stateSerialization, row.owner, row.cost}) assertFalse(row.id, field.isEmpty());
            Map<String, Object> json = row.json();
            for (String key : new String[]{"inputAbi", "implementation", "tasks", "stateSerialization", "owner", "cost", "dependency", "acceptance", "costNanos"}) assertTrue(row.id + ":" + key, json.containsKey(key));
            if (row.acceptance.executed()) {
                assertEquals(row.id, "", row.dependency);
                assertFalse(row.id + " executed with an implementation", row.implementation.isEmpty());
                assertEquals(row.id, 48, row.acceptance.steps);
                assertTrue(row.id, row.acceptance.stateBytes > 0 && row.acceptance.stateSha256.matches("[0-9a-f]{64}") && row.acceptance.costNanos > 0);
            } else {
                assertFalse("a non-executed family must name its reason: " + row.id, row.acceptance.reason.isEmpty());
                if (row.acceptance.status == FamilyRegistryR26.AcceptanceStatusR31.FAILED) red.put(row.id, row.acceptance.reason);
                else assertEquals(row.id, row.dependency, row.acceptance.reason);
            }
        }
        assertEquals("families that executed but failed acceptance are red", Collections.emptyMap(), red);
        int effective = FamilyRegistryR26.effectiveCountR31(rows);
        if (FamilyRegistryR26.legacyBinding().isEmpty()) {
            FamilyRegistryR26.CapabilityR31 f23 = rows.get(22);
            assertEquals("F23", f23.id);
            assertEquals(FamilyRegistryR26.AcceptanceStatusR31.DEPENDENCY_NOT_READY, f23.acceptance.status);
            assertEquals("DEPENDENCY_NOT_READY:F23_ORIGINAL_TEACHER_NOT_BOUND", f23.acceptance.reason);
            assertEquals("", f23.implementation);
            assertEquals("registered rows are not effective strategies", 23, effective);
        } else assertEquals(24, effective);
    }
    @Test public void acceptanceIsDeterministicPerCausalStream() {
        for (String id : new String[]{"F01", "F10", "F17", "F19"}) {
            FamilyRegistryR26.AcceptanceR31 a = FamilyRegistryR26.acceptR31(id, stream(40, 11)), b = FamilyRegistryR26.acceptR31(id, stream(40, 11));
            assertTrue(id, a.executed() && b.executed());
            assertEquals(id + " same stream gives the same restored state", a.stateSha256, b.stateSha256);
        }
        try { FamilyRegistryR26.acceptR31("F99", stream(8, 1)); fail("unknown family accepted"); }
        catch (IllegalArgumentException expected) { assertEquals("R31_UNKNOWN_FAMILY:F99", expected.getMessage()); }
        try { FamilyRegistryR26.capabilityR31("F02", FamilyRegistryR26.acceptR31("F01", stream(8, 1))); fail("acceptance reused for another family"); }
        catch (IllegalArgumentException expected) { assertEquals("R31_ACCEPTANCE_FOR_OTHER_FAMILY", expected.getMessage()); }
    }
    @Test public void compatibilityNumericEntriesOnlyForwardToTheFencedOwner() throws Exception {
        Set<Thread> before = new HashSet<>(Thread.getAllStackTraces().keySet());
        try { ModelLiveLoopSU2.ensure(null, null, 1L); fail("legacy ensure built a second runtime"); }
        catch (IllegalStateException expected) { assertEquals("R31_LEGACY_NUMERIC_ENTRY_FORWARDED:ensure->R26_REPLAY_OWNER_FENCED", expected.getMessage()); }
        try { ModelLiveLoopSU2.checkpoint(null, null, 1L); fail("legacy checkpoint wrote a second model state"); }
        catch (IllegalStateException expected) { assertEquals("R31_LEGACY_NUMERIC_ENTRY_FORWARDED:checkpoint->R26_REPLAY_OWNER_FENCED", expected.getMessage()); }
        Set<Thread> after = new HashSet<>(Thread.getAllStackTraces().keySet()); after.removeAll(before); after.removeIf(t -> !t.isAlive());
        assertEquals("no numeric thread was started by a compatibility entry", Collections.emptySet(), after);
        assertEquals("R26_REPLAY_OWNER_FENCED", ModelLiveLoopSU2.LEGACY_NUMERIC_OWNER_R31);
        for (FamilyRegistryR26.CapabilityR31 row : FamilyRegistryR26.capabilitiesR31(stream(8, 3))) assertEquals(row.id, ModelLiveLoopSU2.LEGACY_NUMERIC_OWNER_R31, row.owner);
    }
}
