package com.ethk3.novaonepro.strategyv2;

import java.util.List;

/** R27-023 I05 排列一致族: 一期10个号互斥，联合考虑全排列. */
public final class IdeaI05PermutationConsistencyR27 implements IdeaFamilyR27 {
    static final String[] KINDS = {"family","freq","crosscond"};
    public String id() { return "I05"; }
    public String nameZh() { return "排列一致族"; }
    public String coreIdea() { return "一期10个号互斥，联合考虑全排列"; }
    static int gen(IdeaFamilyR27.Context ctx) { return ctx == null ? 0 : ctx.generation; }
    public PolicySpecR27 generate(StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        int tp = IdeaFamilyR27.Grow.pos(r);
        PolicyAstR27.Node fam = PolicyAstR27.op("zscore", IdeaFamilyR27.Grow.leaf("family", tp, r));
        PolicyAstR27.Node other = IdeaFamilyR27.Grow.tree(KINDS, tp, 2 + r.nextInt(2), false, ctx, r);
        PolicyAstR27.Node ast = PolicyAstR27.op("mix", IdeaFamilyR27.Grow.round(0.5 + 0.5 * r.nextDouble()), fam, other);
        return IdeaFamilyR27.Grow.spec(ast, tp, r, IdeaFamilyR27.meta(id(), "GENERATE", gen(ctx), r.state, "排列一致：精确排列核家族边际为主，频率修正为辅"));
    }
    public PolicySpecR27 mutate(PolicySpecR27 p, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        return IdeaFamilyR27.Grow.swapLeaf(p, KINDS, id(), gen(ctx), ctx, r);
    }
}
