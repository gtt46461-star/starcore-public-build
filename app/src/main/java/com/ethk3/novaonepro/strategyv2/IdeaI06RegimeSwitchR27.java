package com.ethk3.novaonepro.strategyv2;

import java.util.List;

/** R27-023 I06 状态切换族: 体制不同则规律不同：状态门控两个子打分. */
public final class IdeaI06RegimeSwitchR27 implements IdeaFamilyR27 {
    static final String[] KINDS = {"freq","gap","trans1","cross","fdecay"};
    public String id() { return "I06"; }
    public String nameZh() { return "状态切换族"; }
    public String coreIdea() { return "体制不同则规律不同：状态门控两个子打分"; }
    static int gen(IdeaFamilyR27.Context ctx) { return ctx == null ? 0 : ctx.generation; }
    public PolicySpecR27 generate(StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        int tp = IdeaFamilyR27.Grow.pos(r);
        PolicyAstR27.Node gate = PolicyAstR27.leaf("regime", IdeaFamilyR27.Grow.window(r));
        PolicyAstR27.Node a = IdeaFamilyR27.Grow.tree(KINDS, tp, 2 + r.nextInt(2), false, ctx, r), b = IdeaFamilyR27.Grow.tree(KINDS, tp, 2 + r.nextInt(2), false, ctx, r);
        PolicyAstR27.Node ast = PolicyAstR27.op("if", gate, PolicyAstR27.constant(IdeaFamilyR27.Grow.round(0.05 + 0.15 * r.nextDouble())), a, b);
        return IdeaFamilyR27.Grow.spec(ast, tp, r, IdeaFamilyR27.meta(id(), "GENERATE", gen(ctx), r.state, "状态门控：重复体制高/低时使用不同子打分"));
    }
    public PolicySpecR27 mutate(PolicySpecR27 p, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        List<PolicyAstR27.Node> all = PolicyAstR27.nodes(p.ast);
        for (int i = 0; i < all.size(); i++) if (all.get(i).op.equals("if")) {
            PolicyAstR27.Node n = all.get(i);
            PolicyAstR27.Node moved = new PolicyAstR27.Node("if", null, 0, PolicyAstR27.leaf("regime", IdeaFamilyR27.Grow.window(r)), PolicyAstR27.constant(IdeaFamilyR27.Grow.round(0.05 + 0.15 * r.nextDouble())), n.kids[2], n.kids[3]);
            return p.withAst(PolicyAstR27.replace(p.ast, i, moved), IdeaFamilyR27.meta(id(), "M_GATE", gen(ctx), r.state, "门控窗口/阈值移动", p.policyHash()));
        }
        return null;
    }
}
