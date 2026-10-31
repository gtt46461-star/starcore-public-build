package com.ethk3.novaonepro.strategyv2;

import java.util.LinkedHashMap;
import java.util.Map;

/** One dataset snapshot + split + label view for a generation (docs/05 5.6 step 1). Labels may be shuffled within
 * H_TRAIN/H_SELECT for the null-evolution control; H_HOLDOUT labels are always real. */
public final class EvalContextR27 {
    public final ReplayDatasetR27 data; public final FeatureCacheR27 cache; public final SplitR27 split; public final int[][] labels;
    private final Map<String, double[]> scoreMemo = new LinkedHashMap<String, double[]>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, double[]> e) { return size() > 96; }
    };

    public EvalContextR27(FeatureCacheR27 cache, SplitR27 split, int[][] labels) {
        this.cache = cache; this.data = cache.data; this.split = split; this.labels = labels == null ? cache.data.draws : labels;
    }

    public double[] scores(PolicySpecR27 s) {
        String k = data.datasetHash + "|" + s.ast.toString() + "@" + s.position; // R30-B03-06: a new dataset never hits old scores
        synchronized (scoreMemo) { double[] v = scoreMemo.get(k); if (v != null) return v; }
        double[] v = PolicyAstR27.eval(s.ast, cache, s.position); // outside the lock: parallel replay threads
        synchronized (scoreMemo) { scoreMemo.put(k, v); }
        return v;
    }
    public double temperature(PolicySpecR27 s) { return ReplayEvaluatorR27.fitTemperature(s, scores(s), labels, split.trainFrom, split.trainTo); }

    /** Pass through H_TRAIN and H_SELECT only (candidates never touch the sealed block). */
    public ReplayEvaluatorR27.Trace selectTrace(PolicySpecR27 s, double tau) {
        return ReplayEvaluatorR27.evaluateTrace(s, this, SplitR27.SELECT, tau, null);
    }
    /** Pass that also records H_HOLDOUT — only the generation's champion and its challengers may call this, through the ledger. */
    public ReplayEvaluatorR27.Trace holdoutTrace(PolicySpecR27 s, double tau) {
        return holdoutTrace(s, tau, null);
    }
    public ReplayEvaluatorR27.Trace holdoutTrace(PolicySpecR27 s, double tau, HoldoutLedgerR27 ledger) {
        return ReplayEvaluatorR27.evaluateTrace(s, this, SplitR27.CONFIRM, tau, ledger);
    }

    /** H_TRAIN mean log-loss at the policy's position with temperature tau (fusion weight fitting). */
    public double trainLogLoss(PolicySpecR27 s, double tau) {
        double[] sc = scores(s), z = new double[10], p = new double[10]; double loss = 0; int n = 0;
        for (int t = split.trainFrom; t < split.trainTo; t++) {
            if (!ReplayEvaluatorR27.standardize(sc, t, z)) continue;
            ReplayEvaluatorR27.softmax(z, tau, null, p);
            loss -= Math.log(Math.max(1e-12, p[labels[t][s.position - 1] - 1])); n++;
        }
        return n == 0 ? Double.POSITIVE_INFINITY : loss / n;
    }

    /** Pearson correlation of standardized score rows of a and b at position tp over the last 500 H_SELECT rows. */
    public double correlation(PolicySpecR27 a, PolicySpecR27 b, int tp) {
        double[] x = PolicyAstR27.eval(a.ast, cache, tp), y = PolicyAstR27.eval(b.ast, cache, tp), za = new double[10], zb = new double[10];
        double sxy = 0, sxx = 0, syy = 0; int n = 0;
        for (int t = Math.max(split.selectFrom, split.selectTo - 500); t < split.selectTo; t++) {
            if (!ReplayEvaluatorR27.standardize(x, t, za) || !ReplayEvaluatorR27.standardize(y, t, zb)) continue;
            for (int j = 0; j < 10; j++) { sxy += za[j] * zb[j]; sxx += za[j] * za[j]; syy += zb[j] * zb[j]; } n++;
        }
        return n == 0 || sxx == 0 || syy == 0 ? 0 : sxy / Math.sqrt(sxx * syy);
    }
}
