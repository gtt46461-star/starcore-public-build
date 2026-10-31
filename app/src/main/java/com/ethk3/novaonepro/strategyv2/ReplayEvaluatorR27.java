package com.ethk3.novaonepro.strategyv2;

import java.util.Arrays;

/** R27-015 prequential (walk-forward) evaluator, docs/03 3.3. One pass over rows: features(≤t−1) → predict →
 * record against draw t → online update with the rule fixed at birth. The temperature is the only quantity fitted,
 * and only on H_TRAIN rows. The identical code path produces live predictions (see {@link #predictNext}), so replay
 * and live outputs are bit-identical on the same inputs (H02). */
public final class ReplayEvaluatorR27 {
    private ReplayEvaluatorR27() {}
    static final double[] TEMPERATURES = {0.25, 0.5, 1, 2, 4, 8, 16};
    public static final int QUANTILE_WINDOW = 200;

    /** Per-row outputs of one pass. Rows outside [from,to) or unavailable rows are marked skipped. */
    public static final class Trace {
        public final int from, to, position; public final double temperature;
        public final boolean[] skipped, act; public final byte[] hit; public final int[] k3; public final double[] pHit, pActual, u;
        public final double[] bias = new double[10], lastProbs = new double[10];
        Trace(int n, int from, int to, int position, double temperature) {
            this.from = from; this.to = to; this.position = position; this.temperature = temperature;
            skipped = new boolean[n]; act = new boolean[n]; hit = new byte[n]; k3 = new int[n * 3]; pHit = new double[n]; pActual = new double[n]; u = new double[n];
            Arrays.fill(skipped, true);
        }
        /** Behavior signature row: -1 when not acting, else the encoded sorted K3 (for duplicate/NO_OP checks). */
        public int signature(int t) { return skipped[t] || !act[t] ? -1 : k3[t * 3] * 100 + k3[t * 3 + 1] * 10 + k3[t * 3 + 2]; }
    }

    // ---------------------------------------------------------------- R30-B03-01 PIT guard on every replay sample
    /** Simulated replay clock: one planned period per minute; the deadline of row t is its planned draw time. */
    static final long REPLAY_PERIOD_MS_R30 = 60_000L;
    /** Replay PIT violations waiting to be written to r30_pit_violation by the next research freeze batch. */
    static final java.util.concurrent.ConcurrentLinkedQueue<Object[]> PIT_VIOLATIONS_R30 = new java.util.concurrent.ConcurrentLinkedQueue<>();
    static final int PIT_QUEUE_CAP_R30 = 4096;
    /** Test-only counter-example: the replay sample with this ordinal claims a model trained through its target. */
    static volatile long pitInjectOrdinalR30 = Long.MIN_VALUE;
    private static final ThreadLocal<PitGuardR30.Freeze> PIT_FREEZE_R30 = new ThreadLocal<PitGuardR30.Freeze>() { @Override protected PitGuardR30.Freeze initialValue() { return new PitGuardR30.Freeze(); } };

    /** Returns true when replay sample t may be counted; otherwise the violation is queued and the sample is skipped. */
    static boolean pitReplaySampleR30(PolicySpecR27 spec, ReplayDatasetR27 data, int t, int[] k3, double[] p) {
        long target = data.ordinals[t], through = target - 1;
        if (target == pitInjectOrdinalR30) through = target;
        PitGuardR30.Freeze f = PIT_FREEZE_R30.get();
        f.replay = true; f.targetOrdinal = target; f.trainedThroughOrdinal = through; f.featureThroughOrdinal = target - 1;
        f.position = spec.position; f.k3 = k3; f.probabilities = p;
        // simulated clock: row with ordinal o is drawn at (o+1)·P (positive for ordinal 0); the model trained through
        // ordinal m is available once draw m exists, i.e. at (m+1)·P.
        f.deadlineAt = (target + 1) * REPLAY_PERIOD_MS_R30; f.frozenAt = f.deadlineAt - PitGuardR30.MIN_MARGIN_MS; f.preparedAt = f.frozenAt;
        f.modelAvailableAt = (Math.min(through, target - 1) + 1) * REPLAY_PERIOD_MS_R30; f.featureAvailableAt = f.modelAvailableAt;
        java.util.List<String> v = PitGuardR30.check(f);
        if (v.isEmpty()) return true;
        if (PIT_VIOLATIONS_R30.size() < PIT_QUEUE_CAP_R30)
            for (String code : v) PIT_VIOLATIONS_R30.add(new Object[] {spec.ast.toString().hashCode() + ":" + spec.position, target, code, "REPLAY"});
        return false;
    }

    // ---------------------------------------------------------------- R30-B03-06 unified evaluation entry
    /** The single replay scoring entry: one walk-forward pass and the score of {@code role} (H_TRAIN, H_SELECT or
     * H_CONFIRM). H_CONFIRM is read only through {@link HoldoutLedgerR27#confirm} (once per candidate and dataset hash). */
    public static ReplayScoreR27 evaluate(PolicySpecR27 spec, EvalContextR27 ctx, String role, double tau, HoldoutLedgerR27 ledger) {
        SplitR27 sp = ctx.split; int from, to;
        if (SplitR27.TRAIN.equals(role)) { from = sp.trainFrom; to = sp.trainTo; }
        else if (SplitR27.SELECT.equals(role)) { from = sp.selectFrom; to = sp.selectTo; }
        else if (SplitR27.CONFIRM.equals(role) || SplitR27.HOLDOUT.equals(role)) {
            if (!sp.holdoutAvailable) throw new IllegalStateException("R30_CONFIRM_UNAVAILABLE");
            if (ledger == null) throw new IllegalStateException("R30_CONFIRM_WITHOUT_LEDGER");
            ledger.confirm(spec.policyHash(), ctx.data.datasetHash, sp.holdoutBlock, sp.holdoutFrom, sp.holdoutTo);
            from = sp.holdoutFrom; to = sp.holdoutTo;
        } else throw new IllegalArgumentException("R30_UNKNOWN_ROLE:" + role);
        Trace tr = run(spec, ctx.scores(spec), ctx.data, ctx.labels, tau, sp.trainFrom, to);
        return ReplayScoreR27.of(ReplayScoreR27.storedSplit(role), tr, from, to);
    }

    public static double[] scores(PolicySpecR27 spec, FeatureCacheR27 f) { return PolicyAstR27.eval(spec.ast, f, spec.position); }

    /** Fits the softmax temperature on H_TRAIN rows by grid search on log-loss (bias held at zero). */
    public static double fitTemperature(PolicySpecR27 spec, double[] scores, int[][] labels, int trainFrom, int trainTo) {
        if (!spec.fitTemperature) return spec.temperature;
        double best = spec.temperature, bestLoss = Double.POSITIVE_INFINITY; double[] z = new double[10], p = new double[10];
        for (double mult : TEMPERATURES) {
            double tau = spec.temperature * mult, loss = 0; int n = 0;
            for (int t = trainFrom; t < trainTo; t++) {
                if (!standardize(scores, t, z)) continue;
                softmax(z, tau, null, p);
                loss -= Math.log(Math.max(1e-12, p[labels[t][spec.position - 1] - 1])); n++;
            }
            if (n > 0 && loss / n < bestLoss - 1e-12) { bestLoss = loss / n; best = tau; }
        }
        return best;
    }

    /** Full pass: rows [0,to) are processed online; outputs are recorded for rows [from,to). */
    public static Trace run(PolicySpecR27 spec, FeatureCacheR27 f, int[][] labels, double temperature, int from, int to) {
        return run(spec, scores(spec, f), f.data, labels, temperature, from, to);
    }

    public static Trace run(PolicySpecR27 spec, double[] scores, ReplayDatasetR27 data, int[][] labels, double temperature, int from, int to) {
        int tp = spec.position - 1; Trace tr = new Trace(data.size(), from, to, spec.position, temperature);
        double[] z = new double[10], p = new double[10]; int[] order = new int[10];
        long gate = spec.rule == PolicySpecR27.Rule.RANDOM ? spec.ast.toString().hashCode() * 0x9E3779B97F4A7C15L : 0;
        double[] ring = new double[QUANTILE_WINDOW]; int filled = 0, head = 0;
        for (int t = 0; t < to; t++) {
            if (!standardize(scores, t, z)) continue;
            softmax(z, temperature, tr.bias, p);
            int y = labels[t][tp] - 1;
            if (t >= from) {
                top3(p, order);
                int a = order[0], b = order[1], c = order[2];
                double mass = p[a] + p[b] + p[c];
                boolean act;
                switch (spec.rule) {
                    case ALWAYS: act = true; break;
                    case RANDOM: act = unit(gate ^ (data.ordinals[t] * 0xBF58476D1CE4E5B9L)) < spec.rate; break;
                    case QUANTILE: {
                        int below = 0; for (int i = 0; i < filled; i++) if (ring[i] < mass) below++;
                        act = filled >= 50 && below >= (1 - spec.rate) * filled;
                        break;
                    }
                    default: act = mass - StatsR27.BASE >= spec.threshold; break;
                }
                int[] s = {a, b, c}; Arrays.sort(s);
                // R30-B03-01: a sample failing the PIT guard stays skipped (not counted); the online state still advances.
                if (pitReplaySampleR30(spec, data, t, new int[] {s[0] + 1, s[1] + 1, s[2] + 1}, p)) {
                tr.skipped[t] = false; tr.act[t] = act; tr.k3[t * 3] = s[0]; tr.k3[t * 3 + 1] = s[1]; tr.k3[t * 3 + 2] = s[2];
                boolean h = y == a || y == b || y == c; tr.hit[t] = (byte) (h ? 1 : 0);
                tr.pHit[t] = Math.min(1, mass); tr.pActual[t] = p[y];
                tr.u[t] = act ? (h ? 1 : 0) - StatsR27.BASE : 0;
                if (t == to - 1) System.arraycopy(p, 0, tr.lastProbs, 0, 10);
                }
            }
            if (spec.rule == PolicySpecR27.Rule.QUANTILE) { double[] q = new double[10]; System.arraycopy(p, 0, q, 0, 10); int[] o = new int[10]; top3(q, o); ring[head] = q[o[0]] + q[o[1]] + q[o[2]]; head = (head + 1) % QUANTILE_WINDOW; filled = Math.min(QUANTILE_WINDOW, filled + 1); }
            // Online update: identical in every segment and in live operation (registered rule).
            for (int k = 0; k < 10; k++) tr.bias[k] = tr.bias[k] * (1 - spec.forget) + spec.lr * ((k == y ? 1 : 0) - p[k]);
        }
        return tr;
    }

    /** Live prediction for the row after the dataset. The caller appends a placeholder row so that the feature
     * cache has a slot; by the PIT invariant its content cannot influence the result. Returns the 10 probabilities
     * at the target position in [0..9], then K3 (3 ints as doubles), then act (0/1). */
    public static double[] predictNext(PolicySpecR27 spec, FeatureCacheR27 withPlaceholder, double temperature) {
        int last = withPlaceholder.data.size() - 1;
        int[][] labels = withPlaceholder.data.draws;
        Trace tr = run(spec, withPlaceholder, labels, temperature, last, last + 1);
        double[] out = new double[14];
        if (tr.skipped[last]) { Arrays.fill(out, Double.NaN); return out; }
        System.arraycopy(tr.lastProbs, 0, out, 0, 10);
        out[10] = tr.k3[last * 3]; out[11] = tr.k3[last * 3 + 1]; out[12] = tr.k3[last * 3 + 2]; out[13] = tr.act[last] ? 1 : 0;
        return out;
    }

    static boolean standardize(double[] s, int t, double[] z) {
        int o = t * 10; double m = 0;
        for (int j = 0; j < 10; j++) { double x = s[o + j]; if (Double.isNaN(x) || Double.isInfinite(x)) return false; m += x; }
        m /= 10; double v = 0;
        for (int j = 0; j < 10; j++) { double d = s[o + j] - m; v += d * d; }
        double sd = Math.sqrt(v / 10);
        for (int j = 0; j < 10; j++) z[j] = sd < 1e-12 ? 0 : (s[o + j] - m) / sd;
        return true;
    }

    static void softmax(double[] z, double tau, double[] bias, double[] p) {
        double mx = Double.NEGATIVE_INFINITY;
        for (int j = 0; j < 10; j++) { p[j] = tau * z[j] + (bias == null ? 0 : bias[j]); mx = Math.max(mx, p[j]); }
        double sum = 0;
        for (int j = 0; j < 10; j++) { p[j] = Math.exp(p[j] - mx); sum += p[j]; }
        for (int j = 0; j < 10; j++) p[j] /= sum;
    }

    /** Stable top-3 (ties → lower number). */
    static void top3(double[] p, int[] order) {
        for (int i = 0; i < 10; i++) order[i] = i;
        for (int i = 0; i < 3; i++) {
            int best = i;
            for (int j = i + 1; j < 10; j++) if (p[order[j]] > p[order[best]] || (p[order[j]] == p[order[best]] && order[j] < order[best])) best = j;
            int x = order[i]; order[i] = order[best]; order[best] = x;
        }
    }

    static double unit(long z) { z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L; z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL; z ^= z >>> 31; return (z >>> 11) * 0x1.0p-53; }

    /** Labels permuted within each segment over [from,to) (shuffled-label control, docs/05 5.7). Rows elsewhere keep real labels. */
    public static int[][] shuffledLabels(ReplayDatasetR27 d, int from, int to, long seed) {
        int[][] l = new int[d.size()][];
        for (int t = 0; t < d.size(); t++) l[t] = d.draws[t];
        StatsR27.SplitMix rng = new StatsR27.SplitMix(seed);
        int s = from;
        while (s < to) {
            int e = s; while (e < to && d.segment[e] == d.segment[s]) e++;
            for (int i = e - 1; i > s; i--) { int j = s + rng.nextInt(i - s + 1); int[] x = l[i]; l[i] = l[j]; l[j] = x; }
            s = e;
        }
        return l;
    }
}
