package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B07-C04#A1-#A4: paired same-opportunity improvement claims. */
public final class R31VerifiedImprovementTest {
    private int r31LeasesBeforeTest;
    @org.junit.Before public void r31OutstandingLeaseGuardBefore(){FeatureCacheR27.trimRegisteredCachesR31();r31LeasesBeforeTest=MemoryBudgetR31.processBudget().explicitOwnerLeaseCountR31();}
    @org.junit.After public void r31OutstandingLeaseGuardAfter(){
        FeatureCacheR27.trimRegisteredCachesR31();MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();int after=memory.explicitOwnerLeaseCountR31();
        org.junit.Assert.assertTrue("R31_OUTSTANDING_LEASES before="+r31LeasesBeforeTest+" after="+after+" owners="+memory.explicitOwnerLeasesR31(64),after<=r31LeasesBeforeTest);
    }
    static final double Z = VerifiedImprovementR31.z(0.05);
    /** parent acts every period with hit rate .30; the child acts with `childRate` and hits with `childHit`. */
    static List<VerifiedImprovementR31.Opportunity> series(long from, long to, long seed, double childRate, double childHit, boolean futureLabel) {
        StatsR27.SplitMix r = new StatsR27.SplitMix(seed); List<VerifiedImprovementR31.Opportunity> out = new ArrayList<>();
        for (long t = from; t <= to; t++) {
            boolean ph = r.nextDouble() < .30, ca = r.nextDouble() < childRate, ch = r.nextDouble() < childHit;
            out.add(new VerifiedImprovementR31.Opportunity(t, futureLabel && t % 50 == 0 ? 10 * t + 5 : 10 * t, 10 * t + 5, true, ph, .3, ca, ch, .36));
        }
        return out;
    }
    static VerifiedImprovementR31.Evidence evidence(String experiment, String child, String segment, List<VerifiedImprovementR31.Opportunity> s) {
        return new VerifiedImprovementR31.Evidence(experiment, StatsR27.sha256("parent"), child, 1, 2000, segment, StatsR27.sha256("selection:" + experiment),
                StatsR27.sha256("frozen:" + child), StatsR27.sha256("outcomes:" + segment), .05, 4000, .001, s);
    }

    @Test public void futureLabelsAreDetectedAndCannotForgeAnImprovement() {
        VerifiedImprovementR31.Claim leaked = VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(), evidence("e1", StatsR27.sha256("c1"), "CONFIRM:1", series(1, 2000, 3, .4, .9, true)), Z);
        assertEquals(VerifiedImprovementR31.Status.REJECTED, leaked.status);
        assertTrue(leaked.reasons.toString(), leaked.reasons.get(0).startsWith("FUTURE_LABEL_USED:"));
        assertFalse(VerifiedImprovementR31.isEligibleForChampionReplacement(leaked));
        // without the future label the same child has no edge (hit .30): no improvement can be claimed
        VerifiedImprovementR31.Claim honest = VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(), evidence("e2", StatsR27.sha256("c2"), "CONFIRM:2", series(1, 2000, 4, .4, .30, false)), Z);
        assertEquals(VerifiedImprovementR31.Status.NOT_IMPROVED, honest.status);
        assertFalse(VerifiedImprovementR31.isEligibleForChampionReplacement(honest));
        VerifiedImprovementR31.Claim real = VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(), evidence("e3", StatsR27.sha256("c3"), "CONFIRM:3", series(1, 2000, 5, .5, .55, false)), Z);
        assertEquals(real.reasons.toString(), VerifiedImprovementR31.Status.VERIFIED, real.status);
        assertTrue(VerifiedImprovementR31.isEligibleForChampionReplacement(real));
    }

    @Test public void differentActCountsArePairedAndChildWinCherryPickingIsRejected() {
        List<VerifiedImprovementR31.Opportunity> s = series(1, 2000, 6, .2, .5, false);
        VerifiedImprovementR31.Claim c = VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(), evidence("e4", StatsR27.sha256("c4"), "CONFIRM:4", s), Z);
        assertEquals(2000, c.opportunities); assertEquals(2000, c.parentActs); assertTrue(c.childActs > 300 && c.childActs < 500);
        assertEquals("frequencies are reported separately", 1.0, c.parentFrequency, 0); assertEquals(c.childActs / 2000.0, c.childFrequency, 0);
        assertTrue(Double.isFinite(c.parentBrier) && Double.isFinite(c.childBrier) && Double.isFinite(c.childHitRate));
        List<VerifiedImprovementR31.Opportunity> wins = new ArrayList<>();
        for (VerifiedImprovementR31.Opportunity o : s) { double pu = (o.parentHit ? 1 : 0) - .3, cu = o.childAct ? (o.childHit ? 1 : 0) - .3 : 0; if (cu >= pu) wins.add(o); }
        VerifiedImprovementR31.Claim picked = VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(), evidence("e5", StatsR27.sha256("c5"), "CONFIRM:5", wins), Z);
        assertEquals(VerifiedImprovementR31.Status.REJECTED, picked.status);
        assertTrue(picked.reasons.toString(), picked.reasons.toString().contains("SELECTED_OR_MISSING_OPPORTUNITIES"));
        List<VerifiedImprovementR31.Opportunity> repeated = new ArrayList<>(s); repeated.add(s.get(10));
        assertTrue(VerifiedImprovementR31.compareContract(evidence("e6", "c", "s", repeated)).toString().contains("OPPORTUNITY_REPEATED:11"));
    }

    @Test public void reRunRenameOrRestartOnOneSegmentAddsNoIndependentEvidence() {
        VerifiedImprovementR31.Ledger ledger = new VerifiedImprovementR31.Ledger();
        List<VerifiedImprovementR31.Opportunity> s = series(1, 2000, 7, .5, .55, false);
        VerifiedImprovementR31.Claim first = VerifiedImprovementR31.record(ledger, evidence("e7", StatsR27.sha256("c7"), "CONFIRM:BLOCK9", s), Z);
        assertEquals(VerifiedImprovementR31.Status.VERIFIED, first.status);
        VerifiedImprovementR31.Claim rerun = VerifiedImprovementR31.record(ledger, evidence("e7", StatsR27.sha256("c7"), "CONFIRM:BLOCK9", s), Z);
        VerifiedImprovementR31.Claim renamed = VerifiedImprovementR31.record(ledger, evidence("e8-renamed", StatsR27.sha256("c7-renamed"), "CONFIRM:BLOCK9", s), Z);
        VerifiedImprovementR31.Ledger restarted = VerifiedImprovementR31.Ledger.restore(ledger.json());
        VerifiedImprovementR31.Claim afterRestart = VerifiedImprovementR31.record(restarted, evidence("e9", StatsR27.sha256("c9"), "CONFIRM:BLOCK9", s), Z);
        for (VerifiedImprovementR31.Claim c : Arrays.asList(rerun, renamed, afterRestart)) {
            assertEquals(VerifiedImprovementR31.Status.PROVISIONAL, c.status);
            assertEquals("SEGMENT_ALREADY_CONSUMED_BY:e7", c.reasons.get(0));
            assertFalse("no new error budget on a consumed segment", VerifiedImprovementR31.isEligibleForChampionReplacement(c));
        }
        assertEquals("e7", restarted.firstExperiment("CONFIRM:BLOCK9"));
        assertEquals(VerifiedImprovementR31.Status.VERIFIED, VerifiedImprovementR31.record(restarted, evidence("e10", StatsR27.sha256("c10"), "CONFIRM:BLOCK10", s), Z).status);
    }

    @Test public void claimsAreTraceableAndMissingKeysAreOnlyProvisional() {
        List<VerifiedImprovementR31.Opportunity> s = series(1, 2000, 8, .5, .55, false);
        VerifiedImprovementR31.Claim c = VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(), evidence("e11", StatsR27.sha256("c11"), "CONFIRM:11", s), Z);
        Map<String, Object> trace = c.trace();
        for (String k : new String[]{"experimentId", "parentHash", "childHash", "windowFrom", "windowTo", "segmentKey", "selectionProcessHash", "frozenPredictionsHash", "outcomesHash", "metric", "errorBudget", "costUnits", "lowerBound", "netImprovement"})
            assertNotNull(k, trace.get(k));
        String[] keys = {"experimentId", "parentHash", "childHash", "segmentKey", "selectionProcessHash", "frozenPredictionsHash", "outcomesHash"};
        for (int i = 0; i < keys.length; i++) {
            String[] v = {"e12", StatsR27.sha256("parent"), StatsR27.sha256("c12"), "CONFIRM:12", "sel", "frozen", "outcomes"}; v[i] = "";
            VerifiedImprovementR31.Evidence e = new VerifiedImprovementR31.Evidence(v[0], v[1], v[2], 1, 2000, v[3], v[4], v[5], v[6], .05, 4000, .001, s);
            VerifiedImprovementR31.Claim missing = VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(), e, Z);
            assertEquals(keys[i], VerifiedImprovementR31.Status.PROVISIONAL, missing.status);
            assertEquals("MISSING_" + keys[i], missing.reasons.get(0));
        }
        VerifiedImprovementR31.Claim noBudget = VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(), new VerifiedImprovementR31.Evidence("e13", "p", "c", 1, 2000, "S", "a", "b", "d", 0, 4000, .001, s), Z);
        assertEquals(VerifiedImprovementR31.Status.PROVISIONAL, noBudget.status); assertEquals("MISSING_ERROR_BUDGET", noBudget.reasons.get(0));
    }

    @Test public void evolutionSuccessionReceiptIsPairedDevelopmentEvidenceOnly() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(2400, 17, new int[]{700});
        try (FeatureCacheR27 cache = new FeatureCacheR27(d); EvalContextR27 ctx = new EvalContextR27(cache, SplitR27.of(d, null), null)) {
            PolicySpecR27 parent = new PolicySpecR27(PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", 2, 512)), 3, PolicySpecR27.Rule.ALWAYS, 0, 0, 1, false, .01, .001, null);
            PolicySpecR27 child = parent.withSelector(3, PolicySpecR27.Rule.QUANTILE, 0, .3, null);
            Map<String, Object> r = VerifiedImprovementR31.developmentReceipt(parent.policyHash(), child.policyHash(), ctx.selectTrace(parent, 1), ctx.selectTrace(child, 1), ctx.split.selectFrom, ctx.split.selectTo);
            assertEquals("PROVISIONAL", r.get("status")); assertEquals("DEVELOPMENT_WINDOW_NOT_INDEPENDENT_EVIDENCE", r.get("reason")); assertEquals(false, r.get("championReplacedByThisReceipt"));
            assertEquals((long) (ctx.split.selectTo - ctx.split.selectFrom), r.get("opportunities"));
            assertTrue("different act counts on one opportunity sequence", ((Long) r.get("parentActs")) > ((Long) r.get("childActs")));
        }
    }
}
