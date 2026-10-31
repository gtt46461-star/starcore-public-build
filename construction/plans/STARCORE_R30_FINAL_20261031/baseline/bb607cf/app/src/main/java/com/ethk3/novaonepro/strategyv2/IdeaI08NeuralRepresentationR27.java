package com.ethk3.novaonepro.strategyv2;

import java.util.List;

/** R27-023 I08 神经表示族: 让网络/家族模型自己学特征. */
public final class IdeaI08NeuralRepresentationR27 implements IdeaFamilyR27 {
    static final String[] KINDS = {"family"};
    public String id() { return "I08"; }
    public String nameZh() { return "神经表示族"; }
    public String coreIdea() { return "让网络/家族模型自己学特征"; }
    static int gen(IdeaFamilyR27.Context ctx) { return ctx == null ? 0 : ctx.generation; }
    public PolicySpecR27 generate(StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        int tp = IdeaFamilyR27.Grow.pos(r);
        PolicyAstR27.Node ast = PolicyAstR27.op("zscore", IdeaFamilyR27.Grow.leaf("family", tp, r));
        if (r.chance(0.4)) ast = PolicyAstR27.op("ema", IdeaFamilyR27.Grow.round(0.1 + 0.8 * r.nextDouble()), ast);
        return IdeaFamilyR27.Grow.spec(ast, tp, r, IdeaFamilyR27.meta(id(), "GENERATE", gen(ctx), r.state, "神经/家族表示：R26 在线家族核输出作为打分"));
    }
    public PolicySpecR27 mutate(PolicySpecR27 p, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        return IdeaFamilyR27.Grow.swapLeaf(p, KINDS, id(), gen(ctx), ctx, r);
    }
}
