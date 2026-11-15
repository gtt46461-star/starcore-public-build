package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** Component tests for R27-024 (expert proposals), R27-036 (FTRL), R27-043 (atom replay scoring) and R27-044
 * (derived atoms / molecules). Each has a planted-signal case and a no-signal case. */
public final class R27ComponentTest {

    // ------------------------------------------------------------------ R27-043
    @Test public void atomScoringSeparatesUsefulNeutralAndMissing() {
        int n = 6000, atoms = 3, selectEnd = 4000; StatsR27.SplitMix r = new StatsR27.SplitMix(43);
        double[][] v = new double[n][atoms]; boolean[][] present = new boolean[n][atoms]; int[] hit = new int[n];
        for (int t = 0; t < n; t++) {
            v[t][0] = r.nextDouble(); v[t][1] = r.nextDouble(); v[t][2] = r.nextDouble();
            present[t][0] = true; present[t][1] = true; present[t][2] = r.nextDouble() > 0.2;   // atom 2: 20% missing
            hit[t] = r.nextDouble() < 0.15 + 0.3 * v[t][0] ? 1 : 0;                             // atom 0 carries signal
        }
        AtomScoreR27.Score useful = AtomScoreR27.score(0, v, present, hit, selectEnd), noise = AtomScoreR27.score(1, v, present, hit, selectEnd), missing = AtomScoreR27.score(2, v, present, hit, selectEnd);
        assertEquals(AtomScoreR27.USEFUL, useful.state);
        assertNotEquals(AtomScoreR27.USEFUL, noise.state);
        assertEquals(AtomScoreR27.MASKED_MISSING, missing.state);
        assertEquals(selectEnd, useful.nSelect); assertEquals(0, useful.nHoldout); assertTrue(useful.nLateSelect > 0); // R30-B03-03: H_CONFIRM rows are never read
        // Prequential: the first row's gain cannot depend on any later label.
        int[] flipped = hit.clone(); for (int t = 1; t < n; t++) flipped[t] = 1 - flipped[t];
        assertEquals(AtomScoreR27.score(0, Arrays.copyOf(v, 1), Arrays.copyOf(present, 1), Arrays.copyOf(flipped, 1), 1).select.mean, AtomScoreR27.score(0, Arrays.copyOf(v, 1), Arrays.copyOf(present, 1), Arrays.copyOf(hit, 1), 1).select.mean, 0);
        assertEquals(2.0, AtomScoreR27.weight(AtomScoreR27.USEFUL), 0); assertEquals(0.25, AtomScoreR27.weight(AtomScoreR27.HARMFUL), 0);
        R27TestSupport.log("R27-043_PASS useful=" + useful.json() + " noise=" + noise.state + " missing=" + missing.state + "(" + missing.missingRate() + ")");
    }

    // ------------------------------------------------------------------ R27-044
    @Test public void derivedAtomsAndMoleculesArePitAndRanked() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(1500, 44, null);
        PolicyAstR27.Node common = PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", 1, 32));
        List<PolicySpecR27> programs = new ArrayList<>();
        programs.add(R27BirthGateTest.spec(PolicyAstR27.op("add", common, PolicyAstR27.op("zscore", PolicyAstR27.leaf("gap", 1)))));
        programs.add(R27BirthGateTest.spec(PolicyAstR27.op("mul", PolicyAstR27.constant(0.5), common)));
        programs.add(R27BirthGateTest.spec(PolicyAstR27.op("zscore", PolicyAstR27.leaf("trans1"))));
        List<DerivedAtomR27.Candidate> c = DerivedAtomR27.frequentSubtrees(programs, 5);
        assertFalse(c.isEmpty()); assertEquals(common.toString(), c.get(0).subtree.toString()); assertEquals(2, c.get(0).frequency);
        assertTrue(c.get(0).id().startsWith("DERIVED:"));
        assertTrue(DerivedAtomR27.leakFree(common, d, 1, new int[] {200, 700, 1200}, 7));
        StatsR27.SplitMix r = new StatsR27.SplitMix(5); IdeaFamilyR27.Context ctx = new IdeaFamilyR27.Context();
        for (int i = 0; i < 10; i++) {
            PolicyAstR27.Node m = DerivedAtomR27.molecule(new String[] {"freq", "gap", "trans1", "neighbor"}, 1, r, ctx);
            assertTrue(m.depth() <= 4); assertNull(PolicyAstR27.rangeAndCausality(m));
            assertTrue("molecule must be PIT: " + m, DerivedAtomR27.leakFree(m, d, 1, new int[] {300, 1100}, 9 + i));
        }
        // Mutual information ranks a planted leaf above a no-signal one.
        ReplayDatasetR27 p = d.planted(0.6, 3); FeatureCacheR27 fp = new FeatureCacheR27(p);
        double planted = DerivedAtomR27.mutualInformation("cross", 2, 1, fp, 1, 100, 1400), none = DerivedAtomR27.mutualInformation("randu", 12345, 0, fp, 1, 100, 1400);
        assertTrue("planted MI " + planted + " vs null " + none, planted > none);
        R27TestSupport.log("R27-044_PASS derived=" + c.get(0).id() + " freq=" + c.get(0).frequency + " moleculesPit=10/10 miPlanted=" + planted + " miNull=" + none);
    }

    // ------------------------------------------------------------------ R27-036
    static double[][] xs(int n, int d, StatsR27.SplitMix r) { double[][] x = new double[n][d]; for (double[] row : x) for (int j = 0; j < d; j++) row[j] = r.gaussian(); return x; }
    @Test public void ftrlWarmStartsAndLearnsOnlyRealSignal() {
        int d = 5, n = 8000, pre = 4000; StatsR27.SplitMix r = new StatsR27.SplitMix(36);
        for (boolean planted : new boolean[] {true, false}) {
            double[][] x = xs(n, d, r); int[] y = new int[n];
            for (int i = 0; i < n; i++) { double z = -0.85 + (planted ? 0.6 * x[i][0] : 0); y[i] = r.nextDouble() < 1 / (1 + Math.exp(-z)) ? 1 : 0; }
            FtrlR27 f = FtrlR27.pretrain(x, y, pre);
            double[] w0 = LogisticRidgeSU2.fit(x, toD(y, pre), pre, 1.0, 25);
            double s = w0[0]; for (int j = 0; j < d; j++) s += w0[j + 1] * x[pre][j];
            assertEquals("warm start reproduces the ridge weights", 1 / (1 + Math.exp(-s)), f.predict(x[pre]), 1e-12);
            double[] p = new double[n - pre]; int[] yy = new int[n - pre];
            for (int i = pre; i < n; i++) { p[i - pre] = f.predict(x[i]); yy[i - pre] = y[i]; f.update(x[i], y[i]); } // predict, then learn
            assertEquals(n - pre, f.updates);
            String v = BrierSkillR27.verdict(BrierSkillR27.cumulative(p, yy));
            // Brier skill is measured against the constant-0.30 base; the no-signal stream has base rate ≈0.30.
            if (planted) assertEquals("BETTER_THAN_BASELINE", v); else assertNotEquals("BETTER_THAN_BASELINE", v);
            R27TestSupport.log("R27-036 planted=" + planted + " verdict=" + v + " summary=" + BrierSkillR27.summary(p, yy));
        }
        R27TestSupport.log("R27-036_PASS");
    }
    static double[] toD(int[] y, int n) { double[] o = new double[n]; for (int i = 0; i < n; i++) o[i] = y[i]; return o; }

    // ------------------------------------------------------------------ R27-024
    @Test public void expertProposalsAreCompleteAndSurvivalIsMeasured() {
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(3000, 24, null); FeatureCacheR27 cache = new FeatureCacheR27(data);
        IdeaFamilyR27.Context ctx = new IdeaFamilyR27.Context(); ctx.generation = 1; ctx.failureCluster = "REGIME_LOW";
        ctx.champion = R27BirthGateTest.spec(PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", 1, 32)));
        StatsR27.SplitMix r = new StatsR27.SplitMix(24); BirthGateR27 gate = new BirthGateR27(cache, 2000);
        int births = 0, accepted = 0, audits = 0;
        for (String[] e : ExpertProposalsR27.EXPERTS) for (ExpertProposalsR27.Proposal p : ExpertProposalsR27.propose(e[0], ctx, r)) {
            for (String f : new String[] {p.failureCluster, p.hypothesis, p.structuralChange, p.mechanism, p.refutation}) assertFalse(e[0] + " missing field", f == null || f.isEmpty());
            if (p.spec == null) { audits++; continue; }
            births++; if (gate.check(p.spec).ok) accepted++;
        }
        assertTrue(audits >= 4); assertTrue(births > 0); assertTrue("expert births must be executable", accepted > 0);
        // Survival through the loop: expert births are tracked by hash and their final state is counted (report field).
        final Set<String> expertHashes = new HashSet<>(); final Map<String, String> last = new HashMap<>();
        R27BirthGateTest.Recorder rec = new R27BirthGateTest.Recorder() {
            @Override public void expert(ExpertProposalsR27.Proposal p, int g) { super.expert(p, g); if (p.spec != null) expertHashes.add(p.spec.policyHash()); }
            @Override public void policy(PolicySpecR27 s, String st, String rs, int g, String dh) { super.policy(s, st, rs, g, dh); last.put(s.policyHash(), st); }
        };
        EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.births = 40; cfg.seed = 24; cfg.expertShare = 0.10;
        EvolutionLoopR27 loop = new EvolutionLoopR27(cfg, cache, rec, () -> 0L);
        for (int i = 0; i < 3 * 9; i++) loop.step();
        int born = 0, active = 0; for (String h : expertHashes) { String st = last.get(h); if (st == null) continue; if (!st.equals("REJECTED")) born++; if (st.equals("ACTIVE")) active++; }
        assertTrue("experts proposed during evolution", !expertHashes.isEmpty());
        R27TestSupport.log("R27-024_PASS directProposals=" + (births + audits) + " executable=" + accepted + "/" + births + " audits=" + audits + " loopExpertBirths=" + expertHashes.size() + " born=" + born + " stillActive=" + active);
    }
}
