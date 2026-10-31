package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;

/** R30-B03-06: same policy + same dataset scores identically twice; a new dataset does not reuse cached scores;
 * H_CONFIRM goes through the ledger once. */
public final class R30UnifiedEvaluatorTest {
    static PolicySpecR27 spec() { return R27ReplayTest.spec(PolicyAstR27.op("zscore", PolicyAstR27.leaf("trans1")), 2); }

    @Test public void deterministicAndDatasetKeyed() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(2600, 4, new int[0]);
        EvalContextR27 ctx = new EvalContextR27(new FeatureCacheR27(d), SplitR27.of(d, null), null);
        ReplayScoreR27 a = ReplayEvaluatorR27.evaluate(spec(), ctx, SplitR27.SELECT, 1.0, null), b = ReplayEvaluatorR27.evaluate(spec(), ctx, SplitR27.SELECT, 1.0, null);
        assertEquals(a.nAct, b.nAct); assertEquals(a.hits, b.hits); assertEquals(Double.doubleToLongBits(a.brier), Double.doubleToLongBits(b.brier));
        double[] s1 = ctx.scores(spec());
        ReplayDatasetR27 d2 = ReplayDatasetR27.synthetic(2600, 5, new int[0]);
        EvalContextR27 ctx2 = new EvalContextR27(new FeatureCacheR27(d2), SplitR27.of(d2, null), null);
        assertNotEquals(d.datasetHash, d2.datasetHash);
        assertNotSame(s1, ctx2.scores(spec()));
    }

    @Test public void confirmOnlyThroughLedgerOnce() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(3000, 6, new int[0]);
        EvalContextR27 ctx = new EvalContextR27(new FeatureCacheR27(d), SplitR27.of(d, null), null);
        if (!ctx.split.holdoutAvailable) return;
        try { ReplayEvaluatorR27.evaluate(spec(), ctx, SplitR27.CONFIRM, 1.0, null); fail("confirm without ledger"); } catch (IllegalStateException expected) { }
        HoldoutLedgerR27 l = new HoldoutLedgerR27();
        ReplayScoreR27 c = ReplayEvaluatorR27.evaluate(spec(), ctx, SplitR27.CONFIRM, 1.0, l);
        assertEquals(SplitR27.HOLDOUT, c.split);
        try { ReplayEvaluatorR27.evaluate(spec(), ctx, SplitR27.CONFIRM, 1.0, l); fail("second confirm"); }
        catch (IllegalStateException e) { assertTrue(e.getMessage().startsWith(HoldoutLedgerR27.CONFIRM_REUSED)); }
    }
}
