package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.List;

/** R27-025 fusion F_STACK (docs/05 5.4): 2–5 archive members with pairwise prediction correlation &lt; 0.6, combined
 * by (a) weighted standardized scores with weights fitted on H_TRAIN, (b) a regime gate, or (c) rank voting whose
 * temperature is then fitted on H_TRAIN. The product is a new policy (new hash); members are unchanged and recorded
 * as parents (multi-parent lineage edges). */
public final class FusionOpsR27 {
    private FusionOpsR27() {}
    public static final double MAX_CORRELATION = 0.6;
    static final double[] WEIGHT_GRID = {0, 0.5, 1, 2};

    public static PolicySpecR27 fuse(List<PolicySpecR27> archive, EvalContextR27 ctx, StatsR27.SplitMix r, int gen) {
        if (archive.size() < 2) return null;
        PolicySpecR27 first = archive.get(r.nextInt(archive.size()));
        int tp = first.position; List<PolicySpecR27> members = new ArrayList<>(); members.add(first);
        int want = 2 + r.nextInt(4);
        for (int attempt = 0; attempt < 24 && members.size() < want; attempt++) {
            PolicySpecR27 c = archive.get(r.nextInt(archive.size())); boolean ok = true;
            for (PolicySpecR27 m : members) if (m.policyHash().equals(c.policyHash()) || Math.abs(ctx.correlation(m, c, tp)) >= MAX_CORRELATION) { ok = false; break; }
            if (ok) members.add(c);
        }
        if (members.size() < 2) return null;
        List<String> parents = new ArrayList<>(); for (PolicySpecR27 m : members) parents.add(m.policyHash());
        double mode = r.nextDouble(); PolicyAstR27.Node ast; String how;
        if (mode < 0.5) {
            double[] w = new double[members.size()]; java.util.Arrays.fill(w, 1);
            // coordinate search of weights on H_TRAIN log-loss
            for (int sweep = 0; sweep < 2; sweep++) for (int i = 0; i < w.length; i++) {
                double best = w[i], bestLoss = Double.POSITIVE_INFINITY;
                for (double g : WEIGHT_GRID) { w[i] = g; double l = ctx.trainLogLoss(weighted(members, w, first), 1.0); if (l < bestLoss) { bestLoss = l; best = g; } }
                w[i] = best;
            }
            ast = weighted(members, w, first).ast; how = "对数几率加权(权重在H_TRAIN拟合" + java.util.Arrays.toString(w) + ")";
        } else if (mode < 0.75) {
            ast = PolicyAstR27.op("if", PolicyAstR27.leaf("regime", 128), PolicyAstR27.constant(0.1), PolicyAstR27.op("zscore", members.get(0).ast), PolicyAstR27.op("zscore", members.get(1).ast));
            how = "状态门控(重复体制决定用哪个成员)";
        } else {
            PolicyAstR27.Node sum = PolicyAstR27.op("rank", members.get(0).ast);
            for (int i = 1; i < members.size(); i++) sum = PolicyAstR27.op("add", sum, PolicyAstR27.op("rank", members.get(i).ast));
            ast = sum; how = "投票+校准(温度在H_TRAIN拟合)";
        }
        PolicySpecR27.Meta m = new PolicySpecR27.Meta("I11", "F_STACK", parents, gen, r.state, "融合" + members.size() + "个低相关成员：" + how);
        return new PolicySpecR27(ast, tp, first.rule, first.threshold, first.rate, 1.0, true, first.lr, first.forget, m);
    }

    static PolicySpecR27 weighted(List<PolicySpecR27> members, double[] w, PolicySpecR27 first) {
        PolicyAstR27.Node sum = null;
        for (int i = 0; i < members.size(); i++) {
            if (w[i] == 0) continue;
            PolicyAstR27.Node term = PolicyAstR27.op("mul", PolicyAstR27.constant(w[i]), PolicyAstR27.op("zscore", members.get(i).ast));
            sum = sum == null ? term : PolicyAstR27.op("add", sum, term);
        }
        if (sum == null) sum = PolicyAstR27.op("zscore", members.get(0).ast);
        return new PolicySpecR27(sum, first.position, first.rule, first.threshold, first.rate, 1.0, true, first.lr, first.forget, null);
    }
}
