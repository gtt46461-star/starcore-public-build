package com.ethk3.novaonepro.strategyv2;

import java.util.Map;
import java.util.TreeMap;

/** R27-010 per-operator golden tests (docs/08 8.3-4). Each operator/leaf/family/readout/selector is exercised by a
 * fixed tiny program on the golden set; the operator fingerprint is the SHA-256 of its outputs' IEEE-754 bits.
 * The semantic ABI = this map. Code that does not change these computations (UI, report, scheduling) cannot change
 * any entry; intentionally changing one operator changes exactly that entry and the policies that use it. */
public final class OperatorGoldenR27 {
    private OperatorGoldenR27() {}

    public static Map<String, String> fingerprints(FeatureCacheR27 golden) {
        Map<String, String> out = new TreeMap<>(); int tp = 3;
        PolicyAstR27.Node f = PolicyAstR27.leaf("freq", 3, 32), g = PolicyAstR27.leaf("gap", 3);
        for (String kind : PolicyAstR27.LEAVES.keySet()) {
            if (kind.equals("family")) { for (int fam : PolicyAstR27.FAMILY_LEAVES) out.put("family:" + fam, hash(PolicyAstR27.eval(PolicyAstR27.leaf("family", fam), golden, tp))); continue; }
            out.put(kind, hash(PolicyAstR27.eval(sample(kind), golden, tp)));
        }
        out.put("c", hash(PolicyAstR27.eval(PolicyAstR27.constant(0.125), golden, tp)));
        for (String u : new String[] {"neg", "abs", "log1p", "sqrt", "sigmoid", "tanh", "rank", "zscore", "lagdiff"}) out.put(u, hash(PolicyAstR27.eval(PolicyAstR27.op(u, f), golden, tp)));
        for (String b : new String[] {"add", "sub", "mul", "div", "max", "min"}) out.put(b, hash(PolicyAstR27.eval(PolicyAstR27.op(b, f, g), golden, tp)));
        out.put("softmax", hash(PolicyAstR27.eval(PolicyAstR27.op("softmax", 2.0, f), golden, tp)));
        out.put("ema", hash(PolicyAstR27.eval(PolicyAstR27.op("ema", 0.3, f), golden, tp)));
        out.put("mix", hash(PolicyAstR27.eval(PolicyAstR27.op("mix", 0.25, f, g), golden, tp)));
        out.put("if", hash(PolicyAstR27.eval(PolicyAstR27.op("if", f, g, f, g), golden, tp)));
        // Readout, selectors and updater through the evaluator on a fixed program.
        for (PolicySpecR27.Rule r : PolicySpecR27.Rule.values()) {
            PolicySpecR27 s = new PolicySpecR27(PolicyAstR27.op("zscore", f), tp, r, 0.02, 0.2, 1.0, false, 0.01, 0.01, null);
            ReplayEvaluatorR27.Trace t = ReplayEvaluatorR27.run(s, golden, golden.data.draws, 1.0, 0, golden.data.size());
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < golden.data.size(); i++) b.append(t.signature(i)).append(',').append(Long.toHexString(Double.doubleToLongBits(t.pHit[i]))).append(';');
            out.put("selector:" + r, StatsR27.sha256(b.toString()));
            if (r == PolicySpecR27.Rule.ALWAYS) { out.put("readout:softmax", StatsR27.sha256(b.toString())); StringBuilder u = new StringBuilder(); for (double x : t.bias) u.append(Long.toHexString(Double.doubleToLongBits(x))); out.put("updater:bias_sgd", StatsR27.sha256(u.toString())); }
        }
        return out;
    }

    static PolicyAstR27.Node sample(String kind) {
        switch (kind) {
            case "freq": case "freqnb": case "meangap": case "oddeven": case "bigsmall": return PolicyAstR27.leaf(kind, 2, 32);
            case "fdecay": return PolicyAstR27.leaf(kind, 2, 32);
            case "gap": case "gaprank": case "crosscond": case "neighbor": return PolicyAstR27.leaf(kind, 2);
            case "cross": return PolicyAstR27.leaf(kind, 2, 1);
            case "regime": return PolicyAstR27.leaf(kind, 32);
            case "knn": return PolicyAstR27.leaf(kind, 3, 20);
            case "randu": return PolicyAstR27.leaf(kind, 12345);
            default: return PolicyAstR27.leaf(kind);
        }
    }

    static String hash(double[] v) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(v.length * 8);
        for (double x : v) bb.putLong(Double.doubleToLongBits(x));
        return StatsR27.sha256(bb.array());
    }
}
