package com.ethk3.novaonepro.strategyv2;

import java.util.List;

/** R27-023 I07 相似片段检索族: 历史上类似局面之后发生什么. */
public final class IdeaI07FragmentRetrievalR27 implements IdeaFamilyR27 {
    static final String[] KINDS = {"knn","trans1"};
    public String id() { return "I07"; }
    public String nameZh() { return "相似片段检索族"; }
    public String coreIdea() { return "历史上类似局面之后发生什么"; }
    static int gen(IdeaFamilyR27.Context ctx) { return ctx == null ? 0 : ctx.generation; }
    public PolicySpecR27 generate(StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        int tp = IdeaFamilyR27.Grow.pos(r);
        PolicyAstR27.Node ast = r.chance(0.6) ? PolicyAstR27.op("zscore", IdeaFamilyR27.Grow.leaf("knn", tp, r))
                : PolicyAstR27.op("add", PolicyAstR27.op("zscore", IdeaFamilyR27.Grow.leaf("knn", tp, r)), PolicyAstR27.op("zscore", IdeaFamilyR27.Grow.leaf("knn", tp, r)));
        return IdeaFamilyR27.Grow.spec(ast, tp, r, IdeaFamilyR27.meta(id(), "GENERATE", gen(ctx), r.state, "相似片段检索：最近W期本位序列的k近邻下一期分布"));
    }
    public PolicySpecR27 mutate(PolicySpecR27 p, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        return IdeaFamilyR27.Grow.swapLeaf(p, KINDS, id(), gen(ctx), ctx, r);
    }
}
