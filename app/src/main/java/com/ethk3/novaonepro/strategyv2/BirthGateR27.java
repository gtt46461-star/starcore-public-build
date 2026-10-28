package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** R27-022 executable-birth gate (docs/04 4.1/4.5). Every rejection reason is one of {@link Reason}; nothing is
 * reported as "unknown". The behavior probe is a fixed 500-row window so signatures are comparable within a dataset. */
public final class BirthGateR27 {
    public enum Reason { TYPE, RANGE, CAUSAL, COST, OUTPUT, DUPLICATE, TOO_COMPLEX }
    public static final int MAX_DEPTH = 6, MAX_NODES = 40, MAX_CONSTANTS = 12, PROBE = 500;
    public static final double DUPLICATE_JACCARD = 0.95, MAX_PREDICT_MS = 5.0;
    public static final long MAX_STATE_BYTES = 4L << 20;

    public static final class Verdict {
        public final boolean ok; public final Reason reason; public final String detail; public final int[] signature;
        Verdict(boolean ok, Reason r, String detail, int[] sig) { this.ok = ok; this.reason = r; this.detail = detail; this.signature = sig; }
        public String code() { return ok ? "ACCEPTED" : "BIRTH_REJECTED(" + reason + ")"; }
    }

    private final FeatureCacheR27 cache; private final int probeFrom, probeTo; private int maxDepth = MAX_DEPTH; private double costLimitMs = MAX_PREDICT_MS;
    /** Per-period prediction cost limit (5 ms by default; tests lower it to exercise COST). */
    public void setCostLimitMs(double ms) { costLimitMs = ms; }
    private final Map<String, int[]> signatures = new HashMap<>();
    private final Map<Long, String> exact = new HashMap<>();

    public BirthGateR27(FeatureCacheR27 cache, int probeEnd) {
        this.cache = cache; this.probeTo = Math.max(1, Math.min(probeEnd, cache.data.size())); this.probeFrom = Math.max(0, probeTo - PROBE);
    }
    /** Stagnation response (docs/05 5.8): a temporary lower complexity cap. */
    public void setMaxDepth(int d) { maxDepth = Math.max(2, Math.min(MAX_DEPTH, d)); }
    public int maxDepth() { return maxDepth; }

    public Verdict check(PolicySpecR27 spec) {
        PolicyAstR27.Node ast = spec.ast;
        if (ast.depth() > maxDepth || ast.size() > MAX_NODES || ast.constants() > MAX_CONSTANTS)
            return reject(Reason.TOO_COMPLEX, "depth=" + ast.depth() + ",nodes=" + ast.size() + ",constants=" + ast.constants());
        try { PolicyAstR27.type(ast); } catch (IllegalArgumentException e) { return reject(Reason.TYPE, e.getMessage()); }
        String rc = PolicyAstR27.rangeAndCausality(ast);
        if (rc != null) return reject(rc.startsWith("CAUSAL") ? Reason.CAUSAL : Reason.RANGE, rc);
        if (spec.position < 1 || spec.position > 10 || !(spec.threshold >= 0 && spec.threshold <= 0.5) || !(spec.rate >= 0 && spec.rate <= 1)
                || !(spec.temperature >= 0.05 && spec.temperature <= 20) || !(spec.lr >= 0 && spec.lr <= 0.5) || !(spec.forget >= 0 && spec.forget <= 0.5))
            return reject(Reason.RANGE, "selector/readout/updater");
        long state = 160L;
        for (PolicyAstR27.Node n : PolicyAstR27.nodes(ast)) if (n.op.equals("family")) state += 64L << 10; else if (n.op.equals("ema")) state += 80;
        if (state > MAX_STATE_BYTES) return reject(Reason.COST, "stateBytes=" + state);
        long started = System.nanoTime(); double[] s;
        try { s = PolicyAstR27.eval(ast, cache, spec.position); }
        catch (RuntimeException e) { return reject(Reason.RANGE, "EVAL:" + e.getClass().getSimpleName() + ":" + e.getMessage()); }
        double perPeriodMs = (System.nanoTime() - started) / 1e6 / Math.max(1, cache.data.size());
        if (perPeriodMs > costLimitMs) return reject(Reason.COST, "predictMsPerPeriod=" + perPeriodMs);
        ReplayEvaluatorR27.Trace tr = ReplayEvaluatorR27.run(spec, s, cache.data, cache.data.draws, spec.temperature, probeFrom, probeTo);
        int available = 0; double[] z = new double[10], p = new double[10];
        for (int t = probeFrom; t < probeTo; t++) {
            if (tr.skipped[t]) continue;
            available++;
            if (!ReplayEvaluatorR27.standardize(s, t, z)) return reject(Reason.OUTPUT, "NONFINITE_SCORE@" + t);
            ReplayEvaluatorR27.softmax(z, spec.temperature, null, p);
            double sum = 0; for (double x : p) { if (!Double.isFinite(x) || x < 0) return reject(Reason.OUTPUT, "PROBABILITY@" + t); sum += x; }
            if (Math.abs(sum - 1) > 1e-9) return reject(Reason.OUTPUT, "SUM@" + t + "=" + sum);
            int a = tr.k3[t * 3], b = tr.k3[t * 3 + 1], c = tr.k3[t * 3 + 2];
            if (a == b || b == c || a == c || a < 0 || c > 9) return reject(Reason.OUTPUT, "K3_NOT_DISTINCT@" + t);
        }
        if (available < (probeTo - probeFrom) / 2) return reject(Reason.OUTPUT, "AVAILABLE_ROWS=" + available);
        int[] sig = signature(tr);
        return new Verdict(true, null, "predictMsPerPeriod=" + String.format(java.util.Locale.ROOT, "%.4f", perPeriodMs), sig);
    }

    /** Duplicate test against registered behavior (exact hash first, then Jaccard ≥ 0.95 against registered set). */
    public String duplicateOf(int[] sig) {
        long h = Arrays.hashCode(sig) * 31L + sig.length;
        String same = exact.get(h);
        if (same != null && Arrays.equals(signatures.get(same), sig)) return same;
        for (Map.Entry<String, int[]> e : signatures.entrySet()) if (jaccard(sig, e.getValue()) >= DUPLICATE_JACCARD) return e.getKey();
        return null;
    }
    public void register(String policyHash, int[] sig) { signatures.put(policyHash, sig); exact.put(Arrays.hashCode(sig) * 31L + sig.length, policyHash); }
    public void unregister(String policyHash) { signatures.remove(policyHash); }
    public int registered() { return signatures.size(); }
    public int[] signatureOf(String policyHash) { return signatures.get(policyHash); }

    /** Sorted (row, K3) codes for acting rows in the probe. */
    int[] signature(ReplayEvaluatorR27.Trace tr) {
        List<Integer> out = new ArrayList<>();
        for (int t = probeFrom; t < probeTo; t++) { int s = tr.signature(t); if (s >= 0) out.add((t - probeFrom) * 1000 + s); }
        int[] a = new int[out.size()]; for (int i = 0; i < a.length; i++) a[i] = out.get(i); return a;
    }

    public static double jaccard(int[] a, int[] b) {
        if (a.length == 0 && b.length == 0) return 1;
        int i = 0, j = 0, inter = 0;
        while (i < a.length && j < b.length) { if (a[i] == b[j]) { inter++; i++; j++; } else if (a[i] < b[j]) i++; else j++; }
        return inter / (double) (a.length + b.length - inter);
    }

    private static Verdict reject(Reason r, String d) { return new Verdict(false, r, d, null); }
}
