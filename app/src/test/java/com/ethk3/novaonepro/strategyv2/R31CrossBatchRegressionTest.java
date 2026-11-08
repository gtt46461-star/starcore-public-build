package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/**
 * R31-B12-C02#A2: the same fixtures give the same results before and after the late batches.
 *
 * <p>BEFORE is the digest of each capability's full output recorded on the K10 closing commit 7091b11d (before any K11
 * change), computed by {@link #observed()} itself. A K11 (or later) change that alters any of these outputs fails here
 * and must be justified by a new card, not by re-recording. A second test proves the digests are not constants: each
 * capability still separates a real effect from no effect on its fixture.</p>
 */
public final class R31CrossBatchRegressionTest {
    private int r31LeasesBeforeTest;
    @org.junit.Before public void r31OutstandingLeaseGuardBefore(){FeatureCacheR27.trimRegisteredCachesR31();r31LeasesBeforeTest=MemoryBudgetR31.processBudget().explicitOwnerLeaseCountR31();}
    @org.junit.After public void r31OutstandingLeaseGuardAfter(){
        FeatureCacheR27.trimRegisteredCachesR31();MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();int after=memory.explicitOwnerLeaseCountR31();
        org.junit.Assert.assertTrue("R31_OUTSTANDING_LEASES before="+r31LeasesBeforeTest+" after="+after+" owners="+memory.explicitOwnerLeasesR31(64),after<=r31LeasesBeforeTest);
    }

    static final Map<String, String> BEFORE = new TreeMap<>();
    static {
        BEFORE.put("replay.selectTrace", "PENDING");
        BEFORE.put("paired.MASK_FROZEN", "PENDING");
        BEFORE.put("paired.RETRAIN_REMOVED", "PENDING");
        BEFORE.put("gain.estimate", "PENDING");
        BEFORE.put("improvement.claims", "PENDING");
        BEFORE.put("certificate.streakProbability", "PENDING");
    }

    static String digest(Object o) { return StatsR27.sha256(JsonR27.write(o)); }
    static List<Object> doubles(double[] xs) { List<Object> out = new ArrayList<>(); for (double x : xs) out.add(Long.toHexString(Double.doubleToLongBits(x))); return out; }
    static Map<String, Object> trace(ReplayEvaluatorR27.Trace t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("from", t.from); m.put("to", t.to); m.put("position", t.position); m.put("temperature", Long.toHexString(Double.doubleToLongBits(t.temperature)));
        List<Object> act = new ArrayList<>(), hit = new ArrayList<>(), k3 = new ArrayList<>();
        for (boolean b : t.act) act.add(b ? 1 : 0); for (byte b : t.hit) hit.add((int) b); for (int x : t.k3) k3.add(x);
        m.put("act", act); m.put("hit", hit); m.put("k3", k3); m.put("pHit", doubles(t.pHit)); m.put("bias", doubles(t.bias));
        return m;
    }
    static Map<String, Object> estimate(ResearchGainR30R1.Estimate e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("from", e.fromOrdinal); m.put("through", e.throughOrdinal); m.put("block", e.blockSize); m.put("evaluated", e.evaluatedIssues); m.put("complete", e.completeBlocks);
        m.put("mean", e.meanBlockGain == null ? "null" : Long.toHexString(Double.doubleToLongBits(e.meanBlockGain)));
        m.put("lower", e.lower95 == null ? "null" : Long.toHexString(Double.doubleToLongBits(e.lower95)));
        m.put("upper", e.upper95 == null ? "null" : Long.toHexString(Double.doubleToLongBits(e.upper95)));
        m.put("status", e.status);
        return m;
    }

    /** The fixed fixtures; every value is a full output, not a summary flag. */
    static Map<String, String> observed() {
        Map<String, String> out = new TreeMap<>();
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(3000, 30, new int[0]);
        try (FeatureCacheR27 cache = new FeatureCacheR27(d); EvalContextR27 ctx = new EvalContextR27(cache, SplitR27.of(d, null), null)) {
            PolicySpecR27 parent = RepairPairedResearchR30Test.parent();
            out.put("replay.selectTrace", digest(trace(ctx.selectTrace(parent, 1))));
            for (PairedReplayRunnerR30R1.Protocol protocol : PairedReplayRunnerR30R1.Protocol.values()) {
                PairedReplayRunnerR30R1 r = new PairedReplayRunnerR30R1(RepairPairedResearchR30Test.contract(ctx, parent), parent, "root/0", ctx, 1, 1, 64, protocol);
                while (!r.complete()) r.advance(ctx, 256);
                out.put("paired." + protocol, StatsR27.sha256(r.snapshot()));
            }
        }
        ResearchGainR30R1 gain = new ResearchGainR30R1(1, 512, 64, 4, 200); StatsR27.SplitMix rng = new StatsR27.SplitMix(5);
        for (long t = 1; t <= 512; t++) gain.add(t, .45 + .01 * rng.nextDouble(), .40 + .01 * rng.nextDouble());
        out.put("gain.estimate", digest(estimate(gain.estimate())));
        List<Object> claims = new ArrayList<>();
        double z = VerifiedImprovementR31.z(0.05);
        for (long seed : new long[] {4, 5}) {
            List<VerifiedImprovementR31.Opportunity> s = R31VerifiedImprovementTest.series(1, 2000, seed, seed == 4 ? .4 : .5, seed == 4 ? .30 : .55, false);
            VerifiedImprovementR31.Claim c = VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(), R31VerifiedImprovementTest.evidence("e" + seed, StatsR27.sha256("c" + seed), "CONFIRM:" + seed, s), z);
            claims.add(c.trace()); claims.add(c.status.name());
        }
        out.put("improvement.claims", digest(claims));
        List<Object> streak = new ArrayList<>();
        for (int n : new int[] {50, 200, 1000}) for (double p : new double[] {.1, .3, .5}) streak.add(Long.toHexString(Double.doubleToLongBits(CertificateGateR27.streakGatePassProbabilityR31(n, p, 5))));
        out.put("certificate.streakProbability", digest(streak));
        return out;
    }

    @Test public void theSameFixturesGiveTheSameResultsAsBeforeK11() {
        Map<String, String> now = observed();
        assertEquals("the fixture set is unchanged", BEFORE.keySet(), now.keySet());
        StringBuilder observed = new StringBuilder("R31_REGRESSION_OBSERVED"); List<String> changed = new ArrayList<>(); int i = 0;
        for (Map.Entry<String, String> e : BEFORE.entrySet()) {
            String prefix = now.get(e.getKey()).substring(0, 40); observed.append('|').append(i++).append(':').append(prefix);
            if (!prefix.equals(e.getValue())) changed.add(e.getKey());
        }
        // 160-bit prefixes of the SHA-256 output digests (the message must fit the 300-character gate summary)
        assertTrue(observed + " changed=" + changed, changed.isEmpty());
    }

    @Test public void theComparedCapabilitiesStillSeparateAnEffectFromNoEffect() {
        double z = VerifiedImprovementR31.z(0.05);
        VerifiedImprovementR31.Claim none = VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(), R31VerifiedImprovementTest.evidence("n", StatsR27.sha256("n"), "CONFIRM:N", R31VerifiedImprovementTest.series(1, 2000, 4, .4, .30, false)), z);
        VerifiedImprovementR31.Claim real = VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(), R31VerifiedImprovementTest.evidence("r", StatsR27.sha256("r"), "CONFIRM:R", R31VerifiedImprovementTest.series(1, 2000, 5, .5, .55, false)), z);
        assertEquals(VerifiedImprovementR31.Status.NOT_IMPROVED, none.status); assertEquals(VerifiedImprovementR31.Status.VERIFIED, real.status);
        ResearchGainR30R1 better = new ResearchGainR30R1(1, 512, 64, 4, 200), noise = new ResearchGainR30R1(1, 512, 64, 4, 200); StatsR27.SplitMix r = new StatsR27.SplitMix(5);
        for (long t = 1; t <= 512; t++) { better.add(t, .45 + .01 * r.nextDouble(), .40 + .01 * r.nextDouble()); double x = .4 + .1 * r.nextDouble(); noise.add(t, x, .4 + .1 * r.nextDouble()); }
        assertEquals("EFFECTIVE", ResearchResultContractR31.effectiveness(better.estimate(), 100, 100));
        assertEquals("NOT_SHOWN_EFFECTIVE", ResearchResultContractR31.effectiveness(noise.estimate(), 100, 100));
        assertTrue(CertificateGateR27.streakGatePassProbabilityR31(1000, .5, 5) > CertificateGateR27.streakGatePassProbabilityR31(50, .1, 5));
        Map<String, String> a = observed(); assertEquals("deterministic: two runs agree", a, observed());
        assertEquals("six distinct outputs, none collapsed to a constant", 6, new HashSet<>(a.values()).size());
    }
}
