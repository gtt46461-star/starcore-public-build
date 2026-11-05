package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** R27-025 crossover (docs/05 5.4): X_SUBTREE swaps a type-compatible subtree; X_INPUT keeps A's model with B's
 * feature parameters (source positions/windows) and target position. Parent weights may be inherited by the loop;
 * parent scores never are — children run their own replay. */
public final class CrossoverOpsR27 {
    private CrossoverOpsR27() {}

    public static PolicySpecR27 subtree(PolicySpecR27 a, PolicySpecR27 b, StatsR27.SplitMix r, int gen) {
        return subtree(a,b,r,gen,null);
    }
    public static PolicySpecR27 subtree(PolicySpecR27 a, PolicySpecR27 b, StatsR27.SplitMix r, int gen, EvalContextR27 ctx) {
        FailureProfileR30 profile = ctx == null ? FailureProfileR30.missing(a) : ctx.failureProfile(a);
        FailureProfileR30 donorProfile = ctx == null ? FailureProfileR30.missing(b) : ctx.failureProfile(b);
        List<PolicyAstR27.Node> na = PolicyAstR27.nodes(a.ast), nb = PolicyAstR27.nodes(b.ast);
        List<Integer> targets = profile.prioritize(na,MutationOpsR27.indices(na,n -> !"c".equals(n.op)));
        for (int attempt = 0; attempt < 8; attempt++) {
            int i = targets.get(r.nextInt(targets.size())); PolicyAstR27.Node donor = nb.get(r.nextInt(nb.size()));
            PolicyAstR27.Type want, got;
            try { want = PolicyAstR27.type(na.get(i)); got = PolicyAstR27.type(donor); } catch (IllegalArgumentException e) { continue; }
            if (want != got && want != PolicyAstR27.Type.SCORE && got != PolicyAstR27.Type.SCORE) continue;
            PolicySpecR27 child = a.withAst(PolicyAstR27.replace(a.ast, i, donor), meta(a, b, "X_SUBTREE", r, gen, "子树交叉：A的第" + i + "节点换成B的子树；" + profile.reason() + "；donor_" + donorProfile.reason()));
            if (FailureProfileR30.legal(child)) return child;
        }
        return null;
    }

    public static PolicySpecR27 input(PolicySpecR27 a, PolicySpecR27 b, StatsR27.SplitMix r, int gen) {
        return input(a,b,r,gen,null);
    }
    public static PolicySpecR27 input(PolicySpecR27 a, PolicySpecR27 b, StatsR27.SplitMix r, int gen, EvalContextR27 ctx) {
        FailureProfileR30 profile = ctx == null ? FailureProfileR30.missing(a) : ctx.failureProfile(a);
        FailureProfileR30 donorProfile = ctx == null ? FailureProfileR30.missing(b) : ctx.failureProfile(b);
        List<PolicyAstR27.Node> leavesB = new ArrayList<>();
        for (PolicyAstR27.Node n : PolicyAstR27.nodes(b.ast)) if (n.leaf() && !n.op.equals("c") && n.ints.length > 0) leavesB.add(n);
        PolicyAstR27.Node ast = a.ast; List<PolicyAstR27.Node> na = PolicyAstR27.nodes(ast);
        List<Integer> targets = profile.prioritize(na,MutationOpsR27.indices(na,n -> n.leaf() && !"c".equals(n.op)));
        for (int i : targets) {
            PolicyAstR27.Node n = na.get(i);
            if (!n.leaf() || n.op.equals("c")) continue;
            for (PolicyAstR27.Node m : leavesB) if (m.op.equals(n.op) && !Arrays.equals(m.ints, n.ints)) { ast = PolicyAstR27.replace(ast, i, m); break; }
        }
        PolicySpecR27 child = new PolicySpecR27(ast, b.position, a.rule, a.threshold, a.rate, a.temperature, a.fitTemperature, a.lr, a.forget, null);
        child = child.withMeta(meta(a, b, "X_INPUT", r, gen, "输入交叉：A的模型+B的特征参数与目标位置；" + profile.reason() + "；donor_" + donorProfile.reason()));
        return FailureProfileR30.legal(child) ? child : null;
    }

    static PolicySpecR27.Meta meta(PolicySpecR27 a, PolicySpecR27 b, String op, StatsR27.SplitMix r, int gen, String reason) {
        return new PolicySpecR27.Meta(a.meta == null ? "I09" : a.meta.idea, op, Arrays.asList(a.policyHash(), b.policyHash()), gen, r.state, reason);
    }
}
