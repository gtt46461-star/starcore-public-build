package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.List;

/** R27-025 mutation operators (docs/05 5.3). Every child records its operator; structural operators are flagged.
 * NO_OP detection is done by the evolution loop: a child whose 500-row behavior signature equals its parent's is
 * recorded as NO_OP, not counted and not replayed. */
public final class MutationOpsR27 {
    private MutationOpsR27() {}
    public static final String[] OPS = {"M_CONST", "M_WINDOW", "M_LEAF", "M_SUBTREE", "M_HOIST", "M_WRAP", "M_PRUNE", "M_FAMILY", "M_GATE", "M_SELECTOR", "M_UPDATER"};
    /** Default operator weights; the stagnation response lowers M_CONST/M_WINDOW when NO_OP is frequent (docs/05 5.8). */
    public static double[] defaultWeights() { return new double[] {1.5, 1.0, 1.5, 1.2, 0.5, 0.8, 0.6, 0.5, 0.6, 0.8, 0.5}; }
    public static boolean structural(String op) {
        switch (op) { case "M_CONST": case "M_WINDOW": case "M_SELECTOR": case "M_UPDATER": return false; default: return true; }
    }
    static final String[] ALL_KINDS = {"freq", "fdecay", "freqnb", "gap", "gaprank", "meangap", "trans1", "trans2", "cross", "crosscond", "repeat", "neighbor", "oddeven", "bigsmall", "sumbucket", "knn"};

    /** Measures, for pruning, the relative H_SELECT log-loss increase (fraction) of the pruned program (smaller = less useful). */
    public interface Contribution { double lossIncreaseWithout(PolicySpecR27 spec, int nodeIndex); }

    public static PolicySpecR27 mutate(PolicySpecR27 parent, String op, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx, Contribution contribution) {
        PolicyAstR27.Node ast = parent.ast; List<PolicyAstR27.Node> all = PolicyAstR27.nodes(ast);
        String idea = parent.meta == null ? "I09" : parent.meta.idea; int gen = ctx == null ? 0 : ctx.generation;
        PolicySpecR27.Meta m = new PolicySpecR27.Meta(idea, op, java.util.Collections.singletonList(parent.policyHash()), gen, r.state, reason(op));
        switch (op) {
            case "M_CONST": {
                List<Integer> idx = indices(all, n -> PolicyAstR27.hasParam(n.op));
                if (idx.isEmpty()) return null;
                int i = idx.get(r.nextInt(idx.size())); PolicyAstR27.Node n = all.get(i);
                double c = n.param + (0.1 * Math.abs(n.param) + 0.01) * r.gaussian();
                if (n.op.equals("mix") || n.op.equals("ema")) c = StatsR27.clamp(c, 0, 1);
                return parent.withAst(PolicyAstR27.replace(ast, i, PolicyAstR27.withParam(n, IdeaFamilyR27.Grow.round(c))), m);
            }
            case "M_WINDOW": {
                List<Integer> idx = indices(all, n -> windowed(n) >= 0);
                if (idx.isEmpty()) return null;
                int i = idx.get(r.nextInt(idx.size())); PolicyAstR27.Node n = all.get(i); int slot = windowed(n);
                int[] ints = n.ints.clone(); int w = java.util.Arrays.binarySearch(FeatureCacheR27.WINDOWS, ints[slot]);
                int nw = Math.max(0, Math.min(FeatureCacheR27.WINDOWS.length - 1, w + (r.chance(0.5) ? 1 : -1)));
                ints[slot] = FeatureCacheR27.WINDOWS[nw];
                return parent.withAst(PolicyAstR27.replace(ast, i, PolicyAstR27.withInts(n, ints)), m);
            }
            case "M_LEAF": {
                List<Integer> idx = indices(all, n -> n.leaf() && !n.op.equals("c"));
                if (idx.isEmpty()) return null;
                int i = idx.get(r.nextInt(idx.size()));
                return parent.withAst(PolicyAstR27.replace(ast, i, IdeaFamilyR27.Grow.leaf(IdeaFamilyR27.Grow.pick(ALL_KINDS, ctx, r), parent.position, r)), m);
            }
            case "M_SUBTREE": {
                int i = r.nextInt(all.size());
                return parent.withAst(PolicyAstR27.replace(ast, i, IdeaFamilyR27.Grow.tree(ALL_KINDS, parent.position, 1 + r.nextInt(3), false, ctx, r)), m);
            }
            case "M_HOIST": {
                List<Integer> idx = indices(all, n -> !n.leaf());
                if (idx.size() < 2) return null;
                int i = idx.get(1 + r.nextInt(idx.size() - 1));
                return parent.withAst(all.get(i), m);
            }
            case "M_WRAP": {
                int i = r.nextInt(all.size()); PolicyAstR27.Node n = all.get(i); double u = r.nextDouble();
                PolicyAstR27.Node w = u < 0.25 ? PolicyAstR27.op("sigmoid", n) : u < 0.5 ? PolicyAstR27.op("rank", n) : u < 0.75 ? PolicyAstR27.op("ema", IdeaFamilyR27.Grow.round(0.1 + 0.8 * r.nextDouble()), n)
                        : PolicyAstR27.op("if", PolicyAstR27.leaf("regime", IdeaFamilyR27.Grow.window(r)), PolicyAstR27.constant(0.1), n, PolicyAstR27.op("zscore", IdeaFamilyR27.Grow.leaf("freq", parent.position, r)));
                return parent.withAst(PolicyAstR27.replace(ast, i, w), m);
            }
            case "M_PRUNE": {
                List<Integer> idx = indices(all, n -> n.kids.length >= 2);
                if (idx.isEmpty()) return null;
                int best = -1; double least = Double.POSITIVE_INFINITY; int kid = 0;
                for (int i : idx) {
                    for (int k = 0; k < all.get(i).kids.length && k < 2; k++) {
                        // removing child k keeps the other child
                        double inc = contribution == null ? r.nextDouble() : contribution.lossIncreaseWithout(parent.withAst(PolicyAstR27.replace(ast, i, all.get(i).kids[1 - k]), null), i);
                        if (inc < least) { least = inc; best = i; kid = 1 - k; }
                    }
                }
                // Only branches contributing < 1% (relative H_SELECT log-loss increase) are pruned.
                if (best < 0 || (contribution != null && least >= 0.01)) return null;
                return parent.withAst(PolicyAstR27.replace(ast, best, all.get(best).kids[kid]), m);
            }
            case "M_FAMILY": {
                List<Integer> idx = indices(all, n -> n.leaf() && !n.op.equals("c"));
                if (idx.isEmpty()) return null;
                int i = idx.get(r.nextInt(idx.size()));
                return parent.withAst(PolicyAstR27.replace(ast, i, IdeaFamilyR27.Grow.leaf("family", parent.position, r)), m);
            }
            case "M_GATE": {
                if (ast.op.equals("if")) return parent.withAst(ast.kids[2], m); // remove gate
                PolicyAstR27.Node alt = IdeaFamilyR27.Grow.tree(ALL_KINDS, parent.position, 2, false, ctx, r);
                return parent.withAst(PolicyAstR27.op("if", PolicyAstR27.leaf("regime", IdeaFamilyR27.Grow.window(r)), PolicyAstR27.constant(IdeaFamilyR27.Grow.round(0.05 + 0.15 * r.nextDouble())), ast, alt), m);
            }
            case "M_SELECTOR": {
                int pos = r.chance(0.2) ? IdeaFamilyR27.Grow.pos(r) : parent.position;
                if (parent.rule == PolicySpecR27.Rule.QUANTILE || parent.rule == PolicySpecR27.Rule.RANDOM)
                    return parent.withSelector(pos, PolicySpecR27.Rule.QUANTILE, 0, IdeaFamilyR27.Grow.round(StatsR27.clamp(parent.rate * Math.exp(0.4 * r.gaussian()), 0.02, 0.5)), m);
                double th = IdeaFamilyR27.Grow.round(StatsR27.clamp(parent.threshold + 0.03 * r.gaussian(), 0, 0.3));
                return parent.withSelector(pos, parent.rule, th, parent.rate, m);
            }
            case "M_UPDATER": {
                double lr = IdeaFamilyR27.Grow.round(StatsR27.clamp(parent.lr * Math.exp(0.5 * r.gaussian()), 0, 0.2));
                double fg = IdeaFamilyR27.Grow.round(StatsR27.clamp(parent.forget * Math.exp(0.5 * r.gaussian()), 0, 0.2));
                return parent.withUpdater(lr, fg, m);
            }
            default: throw new IllegalArgumentException("R27_UNKNOWN_MUTATION:" + op);
        }
    }

    static String reason(String op) {
        switch (op) {
            case "M_CONST": return "常数高斯扰动"; case "M_WINDOW": return "窗口换到相邻尺度"; case "M_LEAF": return "替换叶子"; case "M_SUBTREE": return "随机子树替换";
            case "M_HOIST": return "用子树替换整棵树(简化)"; case "M_WRAP": return "子树外包一层变换"; case "M_PRUNE": return "删除消融贡献最小的分支"; case "M_FAMILY": return "叶子替换为家族模型调用";
            case "M_GATE": return "加/删状态门控"; case "M_SELECTOR": return "只改出手阈值/位置"; case "M_UPDATER": return "改在线学习率/遗忘率"; default: return op;
        }
    }

    static int windowed(PolicyAstR27.Node n) {
        switch (n.op) { case "freq": case "freqnb": case "meangap": case "oddeven": case "bigsmall": return 1; case "regime": return 0; default: return -1; }
    }
    interface Pred { boolean test(PolicyAstR27.Node n); }
    static List<Integer> indices(List<PolicyAstR27.Node> all, Pred p) { List<Integer> out = new ArrayList<>(); for (int i = 0; i < all.size(); i++) if (p.test(all.get(i))) out.add(i); return out; }

    public static String pick(double[] weights, StatsR27.SplitMix r) {
        double s = 0; for (double w : weights) s += w; double u = r.nextDouble() * s;
        for (int i = 0; i < weights.length; i++) { u -= weights[i]; if (u <= 0) return OPS[i]; }
        return OPS[OPS.length - 1];
    }
}
