package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.Collections;
import org.junit.Test;

public final class R30SelectorPolicyTest {
    static PolicySpecR27 base() {
        return new PolicySpecR27(PolicyAstR27.op("zscore", PolicyAstR27.leaf("trans1")), 2,
                PolicySpecR27.Rule.MARGIN, 0.02, 0, 1.1, true, 0.012, 0.001,
                IdeaFamilyR27.meta("I13", "M_SELECTOR", 1, 5, "selection fixture"));
    }
    static EvalContextR27 context(boolean poisonConfirm) {
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(3000, 37, new int[0]); SplitR27 split = SplitR27.of(data, null);
        int[][] labels = new int[data.size()][];
        for (int t = 0; t < labels.length; t++) labels[t] = poisonConfirm && t >= split.selectTo ? new int[10] : data.draws[t].clone();
        return new EvalContextR27(new FeatureCacheR27(data), split, labels);
    }
    @Test public void selectorPreservesScoringAndUpdaterAndCannotReadConfirmLabels() {
        PolicySpecR27 base = base(); EvalContextR27 plain = context(false), poisoned = context(true);
        PolicySpecR27 a = SelectorPolicyR30.combine(base, 4, PolicySpecR27.Rule.MARGIN, 0.03, 0, plain, base.meta);
        PolicySpecR27 b = SelectorPolicyR30.combine(base, 4, PolicySpecR27.Rule.MARGIN, 0.03, 0, poisoned, base.meta);
        assertEquals(a.canonical(), b.canonical()); assertEquals(a.meta.reason, b.meta.reason);
        assertSame(base.ast, a.ast); assertEquals(base.temperature, a.temperature, 0); assertEquals(base.fitTemperature, a.fitTemperature);
        assertEquals(base.lr, a.lr, 0); assertEquals(base.forget, a.forget, 0); assertEquals(4, a.position);
        ReplayScoreR27 measured = ReplayEvaluatorR27.evaluate(a, plain, SplitR27.SELECT, plain.temperature(a), null);
        assertEquals(Math.max(0.01, measured.actRate()), a.rate, 0);
        assertTrue(a.meta.reason.contains("@H_SELECT")); assertTrue(a.meta.reason.contains("acts=" + measured.nAct));
        assertTrue(PolicyContractR30.violations(a).isEmpty());
    }
    @Test public void quantileExecutionRateStaysFixedAndActualFamilyUsesTheStudy() {
        PolicySpecR27 base = base(); EvalContextR27 eval = context(true);
        PolicySpecR27 q = SelectorPolicyR30.combine(base, 2, PolicySpecR27.Rule.QUANTILE, 0, 0.2, eval, base.meta);
        assertEquals(0.2, q.rate, 0); assertTrue(q.meta.reason.contains("@H_SELECT"));
        IdeaFamilyR27.Context family = new IdeaFamilyR27.Context(); family.evaluation = eval; family.archive = Collections.singletonList(base);
        PolicySpecR27 actual = new IdeaI13SelectorOnlyR27().propose(new StatsR27.SplitMix(55), family);
        assertSame(base.ast, actual.ast); assertTrue(actual.meta.reason.contains("@H_SELECT"));
        assertFalse(actual.meta.reason.contains("NOT_RUN")); assertTrue(PolicyContractR30.violations(actual).isEmpty());
    }
    @Test public void missingStudyIsExplicitAndLegacyMarginFallbackIsPreserved() {
        PolicySpecR27 base = base();
        PolicySpecR27 unmeasured = SelectorPolicyR30.combine(base, 2, PolicySpecR27.Rule.MARGIN, 0.02, 0, null, base.meta);
        assertTrue(unmeasured.meta.reason.contains("NOT_RUN@H_SELECT")); assertEquals(0, unmeasured.rate, 0);
        assertEquals(0.15, SelectorPolicyR30.registeredRate(unmeasured), 0);
    }
}
