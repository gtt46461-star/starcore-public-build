package com.ethk3.novaonepro.strategyv2;

import java.util.List;

/** R27-023 I11 组合互补族: 多个弱且不相关策略的加权/门控组合. */
public final class IdeaI11ComplementaryEnsembleR27 implements IdeaFamilyR27 {
    static final String[] KINDS = {"freq","gap","trans1","cross"};
    public String id() { return "I11"; }
    public String nameZh() { return "组合互补族"; }
    public String coreIdea() { return "多个弱且不相关策略的加权/门控组合"; }
    static int gen(IdeaFamilyR27.Context ctx) { return ctx == null ? 0 : ctx.generation; }
    public PolicySpecR27 generate(StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        int tp = IdeaFamilyR27.Grow.pos(r);
        if (ctx != null && ctx.archive.size() >= 2) {
            PolicySpecR27 a = ctx.archive.get(r.nextInt(ctx.archive.size())), b = ctx.archive.get(r.nextInt(ctx.archive.size()));
            if (!a.policyHash().equals(b.policyHash())) {
                PolicyAstR27.Node ast = PolicyAstR27.op("mix", IdeaFamilyR27.Grow.round(0.3 + 0.4 * r.nextDouble()), PolicyAstR27.op("zscore", a.ast), PolicyAstR27.op("zscore", b.ast));
                return a.withAst(ast, IdeaFamilyR27.meta(id(), "F_STACK", gen(ctx), r.state, "互补组合：档案中两个成员的标准化加权", a.policyHash(), b.policyHash()));
            }
        }
        PolicyAstR27.Node ast = PolicyAstR27.op("add", IdeaFamilyR27.Grow.tree(KINDS, tp, 2, true, ctx, r), IdeaFamilyR27.Grow.tree(KINDS, tp, 2, true, ctx, r));
        return IdeaFamilyR27.Grow.spec(ast, tp, r, IdeaFamilyR27.meta(id(), "GENERATE", gen(ctx), r.state, "互补组合：两个不同信息源子打分相加"));
    }
    public PolicySpecR27 mutate(PolicySpecR27 p, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        return IdeaFamilyR27.Grow.swapLeaf(p, KINDS, id(), gen(ctx), ctx, r);
    }
}
