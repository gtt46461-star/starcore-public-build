package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R27-044 derived atoms and molecules (docs/07 7.1-3, 7.2). Every 50 generations the most frequent depth-≤3
 * subtree among surviving programs whose ablation (replacing it by a constant) costs H_SELECT same-period paired Brier is registered
 * as a DERIVED atom (identity = canonical subtree hash). Molecules are 2–4-leaf typed combinations (depth ≤ 2) of
 * USEFUL/NEUTRAL leaf kinds, sampled by H_TRAIN mutual information with the hit label; 32 per generation are
 * replayed like policies and survivors become generator leaves. Both are composed only of PIT leaves, and the
 * leakage test is run on each before registration. */
public final class DerivedAtomR27 {
    private DerivedAtomR27() {}

    public static final class Candidate { public final PolicyAstR27.Node subtree; public final int frequency; public double contribution; public AtomScoreR27.Contribution contributionStats; public String reason = "";
        Candidate(PolicyAstR27.Node s, int f) { subtree = s; frequency = f; }
        public String id() { return "DERIVED:" + StatsR27.sha256(subtree.toString()).substring(0, 16); } }

    /** Ranked non-leaf subtrees of depth ≤ 3 by frequency among the given programs. */
    public static List<Candidate> frequentSubtrees(List<PolicySpecR27> programs, int limit) {
        Map<String, Integer> freq = new HashMap<>(); Map<String, PolicyAstR27.Node> node = new HashMap<>();
        for (PolicySpecR27 p : programs) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (PolicyAstR27.Node n : PolicyAstR27.nodes(p.ast)) {
                if (n.leaf() || n.depth() > 3 || n.size() < 2) continue;
                String k = n.toString(); if (!seen.add(k)) continue;
                freq.merge(k, 1, Integer::sum); node.put(k, n);
            }
        }
        List<Candidate> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : freq.entrySet()) if (e.getValue() >= 2) out.add(new Candidate(node.get(e.getKey()), e.getValue()));
        out.sort((a, b) -> a.frequency != b.frequency ? b.frequency - a.frequency : a.subtree.toString().compareTo(b.subtree.toString()));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    /** Mean H_SELECT paired Brier gain for the original action, with the subtree replaced by zero in the other arm.
     * Per-period clustering and the one-sided lower bound are available through pairedContribution(). */
    public static double contribution(PolicyAstR27.Node sub,List<PolicySpecR27> programs,EvalContextR27 ctx){return pairedContribution(sub,programs,ctx).mean();}
    /** Same original K3 and binary event in both arms; each period contributes once after averaging its policies. */
    public static AtomScoreR27.Contribution pairedContribution(PolicyAstR27.Node sub,List<PolicySpecR27> programs,EvalContextR27 ctx){
        double[] sum=new double[ctx.data.size()];int[] counts=new int[sum.length];long comparisons=0;String key=sub.toString();
        for(PolicySpecR27 p:programs){
            List<PolicyAstR27.Node> nodes=PolicyAstR27.nodes(p.ast);int at=-1;for(int i=0;i<nodes.size();i++)if(nodes.get(i).toString().equals(key)){at=i;break;}
            if(at<0)continue;
            PolicySpecR27 without=p.withAst(PolicyAstR27.replace(p.ast,at,PolicyAstR27.constant(0)),null);
            try{
                ReplayEvaluatorR27.Trace full=ReplayEvaluatorR27.selectTraceWithProbabilities(p,ctx,ctx.temperature(p));
                ReplayEvaluatorR27.Trace removed=ReplayEvaluatorR27.selectTraceWithProbabilities(without,ctx,ctx.temperature(without));
                for(int t=ctx.split.selectFrom;t<ctx.split.selectTo;t++)if(!full.skipped[t]&&!removed.skipped[t]){
                    double probability=0;for(int k=0;k<3;k++)probability+=removed.probabilityRows[t][full.k3[t*3+k]];
                    sum[t]+=AtomScoreR27.pairedBrier(full.pHit[t],probability,full.hit[t]);counts[t]++;comparisons++;
                }
            }catch(IllegalArgumentException unsupported){/* No estimate for a structurally invalid counterfactual. */}
        }
        AtomScoreR27.Contribution result=new AtomScoreR27.Contribution();result.comparisons=comparisons;
        for(int t=ctx.split.selectFrom;t<ctx.split.selectTo;t++)if(counts[t]>0)result.gains.add(sum[t]/counts[t]);
        return result;
    }
    /** Preserved historical utility diagnostic. Production contribution is paired Brier above. */
    public static double legacyUtilityContribution(PolicyAstR27.Node sub, List<PolicySpecR27> programs, EvalContextR27 ctx) {
        StatsR27.Moments m = new StatsR27.Moments(); String key = sub.toString();
        for (PolicySpecR27 p : programs) {
            List<PolicyAstR27.Node> all = PolicyAstR27.nodes(p.ast); int at = -1;
            for (int i = 0; i < all.size(); i++) if (all.get(i).toString().equals(key)) { at = i; break; }
            if (at < 0) continue;
            try {
                double tau = ctx.temperature(p);
                double base = ReplayScoreR27.of(SplitR27.SELECT, ctx.selectTrace(p, tau), ctx.split.selectFrom, ctx.split.selectTo).pairedUMean;
                PolicySpecR27 ab = p.withAst(PolicyAstR27.replace(p.ast, at, PolicyAstR27.constant(0)), null);
                double without = ReplayScoreR27.of(SplitR27.SELECT, ctx.selectTrace(ab, ctx.temperature(ab)), ctx.split.selectFrom, ctx.split.selectTo).pairedUMean;
                m.add(base - without);
            } catch (RuntimeException e) { /* an illegal ablated program contributes no estimate */ }
        }
        return m.n == 0 ? Double.NaN : m.mean;
    }

    /** PIT leakage check of a candidate leaf/subtree: perturbing draws from row t on must not change its row-t values. */
    public static boolean leakFree(PolicyAstR27.Node sub, ReplayDatasetR27 d, int tp, int[] rows, long seed) {
        double[] base = PolicyAstR27.eval(sub, new FeatureCacheR27(d, 16), tp);
        StatsR27.SplitMix r = new StatsR27.SplitMix(seed);
        for (int t : rows) {
            int[][] x = new int[d.size()][]; for (int i = 0; i < d.size(); i++) x[i] = i < t ? d.draws[i] : ReplayDatasetR27.randomPermutation(r);
            double[] v = PolicyAstR27.eval(sub, new FeatureCacheR27(d.withDraws(x), 16), tp);
            for (int k = 0; k <= t * 10 + 9 && k < v.length; k++) if (Double.doubleToLongBits(v[k]) != Double.doubleToLongBits(base[k])) return false;
        }
        return true;
    }

    /** A random molecule (2–4 leaves, depth ≤ 2) over the given kinds. */
    public static PolicyAstR27.Node molecule(String[] kinds, int tp, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        int k = 2 + r.nextInt(3); PolicyAstR27.Node[] a = new PolicyAstR27.Node[k];
        for (int i = 0; i < k; i++) a[i] = PolicyAstR27.op("zscore", IdeaFamilyR27.Grow.leaf(IdeaFamilyR27.Grow.pick(kinds, ctx, r), tp, r));
        // Combination depth ≤ 2 over the atoms (docs/07 7.2): (a∘b), ((a∘b)∘c), ((a∘b)∘(c∘d)).
        PolicyAstR27.Node left = combine(a[0], a[1], r);
        return k == 2 ? left : combine(left, k == 3 ? a[2] : combine(a[2], a[3], r), r);
    }
    static PolicyAstR27.Node combine(PolicyAstR27.Node x, PolicyAstR27.Node y, StatsR27.SplitMix r) {
        String[] ops = {"mul", "sub", "add", "max"}; return PolicyAstR27.op(ops[r.nextInt(ops.length)], x, y);
    }

    /** Empirical mutual information (bits) between a leaf's 5-bin value at the target and the K3-membership label on H_TRAIN. */
    public static double mutualInformation(String kind, int a, int b, FeatureCacheR27 f, int tp, int from, int to) {
        double[] v = f.leaf(kind, tp, a, b); double[][] joint = new double[5][2]; double n = 0;
        for (int t = from; t < to; t++) {
            int y = f.data.draws[t][tp - 1] - 1; double x = v[t * 10 + y];
            if (Double.isNaN(x)) continue;
            double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY; for (int j = 0; j < 10; j++) { lo = Math.min(lo, v[t * 10 + j]); hi = Math.max(hi, v[t * 10 + j]); }
            for (int j = 0; j < 10; j++) { int bin = hi > lo ? Math.min(4, (int) ((v[t * 10 + j] - lo) / (hi - lo) * 5)) : 2; joint[bin][j == y ? 1 : 0]++; n++; }
        }
        if (n == 0) return 0;
        double mi = 0, py1 = 0; for (double[] row : joint) py1 += row[1]; py1 /= n;
        for (double[] row : joint) { double px = (row[0] + row[1]) / n; for (int c = 0; c < 2; c++) { double pxy = row[c] / n, py = c == 1 ? py1 : 1 - py1; if (pxy > 0) mi += pxy * Math.log(pxy / (px * py)) / Math.log(2); } }
        return mi;
    }

    public static Map<String, Object> json(Candidate c) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("id", c.id()); m.put("definition", c.subtree.toString()); m.put("frequency", (long) c.frequency);
        m.put("pairedBrierR30",c.contributionStats==null?null:c.contributionStats.json()); m.put("contribution", Double.isFinite(c.contribution) ? (Object) c.contribution : null); m.put("reason", c.reason); return m;
    }
}
