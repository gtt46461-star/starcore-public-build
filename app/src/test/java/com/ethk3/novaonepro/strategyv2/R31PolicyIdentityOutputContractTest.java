package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B07-C02#A2/#A4: a lawful policy keeps canonical text, hash and per-sample predictions through its own
 * serialization; changing the updater or selector produces a different identity while meta never does. Every
 * evaluated sample is a finite normalized 10-way distribution and either acts with three distinct numbers at the
 * single target position (the top three of that distribution) or explicitly abstains; rows never read later labels. */
public final class R31PolicyIdentityOutputContractTest {
    static PolicyAstR27.Node z(PolicyAstR27.Node n) { return PolicyAstR27.op("zscore", n); }
    static List<PolicySpecR27> policies() {
        PolicyAstR27.Node freq = z(PolicyAstR27.leaf("freq", 2, 512)), gap = z(PolicyAstR27.leaf("gap", 4));
        PolicyAstR27.Node mix = PolicyAstR27.op("mix", 0.3, freq, gap);
        return Arrays.asList(
            new PolicySpecR27(freq, 2, PolicySpecR27.Rule.ALWAYS, 0, 0, 1, false, 0.01, 0.001, null),
            new PolicySpecR27(gap, 4, PolicySpecR27.Rule.MARGIN, 0.02, 0, 1.3, false, 0.02, 0.002, null),
            new PolicySpecR27(mix, 7, PolicySpecR27.Rule.QUANTILE, 0, 0.3, 0.8, false, 0.005, 0.01, null),
            new PolicySpecR27(mix, 9, PolicySpecR27.Rule.RANDOM, 0, 0.4, 1, false, 0.01, 0.0, null));
    }
    static EvalContextR27 context(ReplayDatasetR27 d) { return new EvalContextR27(new FeatureCacheR27(d), SplitR27.of(d, null), null); }
    @Test public void canonicalHashAndPredictionsSurviveSerializationAndBehaviorChangesChangeIdentity() {
        try (EvalContextR27 ctx = context(ReplayDatasetR27.synthetic(2400, 31, new int[0]))) { identity(ctx); }
    }
    private static void identity(EvalContextR27 ctx) {
        Set<String> hashes = new HashSet<>();
        for (PolicySpecR27 s : policies()) {
            PolicySpecR27 back = PolicySpecR27.parse(s.fullJson());
            assertEquals(s.canonical(), back.canonical()); assertEquals(s.policyHash(), back.policyHash());
            ReplayEvaluatorR27.Trace a = ReplayEvaluatorR27.selectTraceWithProbabilities(s, ctx, s.temperature), b = ReplayEvaluatorR27.selectTraceWithProbabilities(back, ctx, back.temperature);
            assertArrayEquals(a.skipped, b.skipped); assertArrayEquals(a.act, b.act); assertArrayEquals(a.k3, b.k3); assertArrayEquals(a.pHit, b.pHit, 0); assertArrayEquals(a.hit, b.hit);
            for (int t = 0; t < a.skipped.length; t++) if (!a.skipped[t]) assertArrayEquals("sample " + t, a.probabilityRows[t], b.probabilityRows[t], 0);
            assertTrue(hashes.add(s.policyHash()));
            PolicySpecR27.Meta meta = IdeaFamilyR27.meta("I02", "RENAMED", 3, 99, "different lineage only");
            assertEquals("meta never changes identity", s.policyHash(), new PolicySpecR27(s.ast, s.position, s.rule, s.threshold, s.rate, s.temperature, s.fitTemperature, s.lr, s.forget, meta).policyHash());
            assertNotEquals("updater learning rate is identity", s.policyHash(), s.withUpdater(s.lr * 2, s.forget, null).policyHash());
            assertNotEquals("updater forgetting is identity", s.policyHash(), s.withUpdater(s.lr, s.forget + 0.003, null).policyHash());
            assertNotEquals("selector position is identity", s.policyHash(), s.withSelector(s.position % 10 + 1, s.rule, s.threshold, s.rate, null).policyHash());
            PolicySpecR27.Rule other = s.rule == PolicySpecR27.Rule.ALWAYS ? PolicySpecR27.Rule.MARGIN : PolicySpecR27.Rule.ALWAYS;
            assertNotEquals("selector rule is identity", s.policyHash(), s.withSelector(s.position, other, s.threshold, s.rate, null).policyHash());
            assertNotEquals("selector threshold/rate is identity", s.policyHash(), s.withSelector(s.position, s.rule, s.threshold + 0.01, s.rate + 0.05, null).policyHash());
        }
    }
    @Test public void outputsAreFiniteNormalizedSinglePositionK3OrAbstainAndIgnoreLaterLabels() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(2400, 47, new int[0]);
        try (EvalContextR27 ctx = context(d)) { outputs(d, ctx); }
    }
    private static void outputs(ReplayDatasetR27 d, EvalContextR27 ctx) {
        int cut = ctx.split.selectFrom + (ctx.split.selectTo - ctx.split.selectFrom) / 2;
        int[][] later = d.draws.clone(); for (int t = cut; t < later.length; t++) later[t] = d.draws[t % 7].clone();
        try (EvalContextR27 changed = context(d.withDraws(later))) { outputs(ctx, changed, cut); }
    }
    private static void outputs(EvalContextR27 ctx, EvalContextR27 changed, int cut) {
        for (PolicySpecR27 s : policies()) {
            ReplayEvaluatorR27.Trace tr = ReplayEvaluatorR27.selectTraceWithProbabilities(s, ctx, s.temperature), alt = ReplayEvaluatorR27.selectTraceWithProbabilities(s, changed, s.temperature);
            assertEquals(s.position, tr.position);
            int samples = 0, acted = 0, abstained = 0;
            for (int t = ctx.split.selectFrom; t < ctx.split.selectTo; t++) {
                if (tr.skipped[t]) { assertFalse("a skipped sample never acts", tr.act[t]); continue; }
                samples++;
                double[] p = tr.probabilityRows[t]; assertEquals(10, p.length);
                double sum = 0; for (double v : p) { assertTrue(s.policyHash() + "@" + t, Double.isFinite(v) && v >= 0 && v <= 1); sum += v; }
                assertEquals("normalized at " + t, 1.0, sum, 1e-9);
                int a = tr.k3[t * 3], b = tr.k3[t * 3 + 1], c = tr.k3[t * 3 + 2];
                assertTrue("three distinct numbers at " + t, a >= 0 && a < b && b < c && c <= 9);
                double third = Math.min(p[a], Math.min(p[b], p[c]));
                for (int k = 0; k < 10; k++) if (k != a && k != b && k != c) assertTrue("K3 is the top three at " + t, p[k] <= third);
                if (tr.act[t]) acted++; else { abstained++; assertEquals("abstention earns nothing", 0, tr.u[t], 0); }
                if (t < cut) {
                    assertArrayEquals("row " + t + " cannot read labels at or after " + cut, p, alt.probabilityRows[t], 0);
                    assertEquals(tr.act[t], alt.act[t]);
                }
            }
            assertTrue(s.rule + " evaluated samples", samples > 100);
            assertEquals(samples, acted + abstained);
            if (s.rule == PolicySpecR27.Rule.ALWAYS) assertEquals(0, abstained);
        }
    }
}
