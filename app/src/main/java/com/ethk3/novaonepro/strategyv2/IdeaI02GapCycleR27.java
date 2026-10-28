package com.ethk3.novaonepro.strategyv2;

import java.util.List;

/** R27-023 I02 遗漏周期族: 号码间隔是否有周期. */
public final class IdeaI02GapCycleR27 implements IdeaFamilyR27 {
    static final String[] KINDS = {"gap","gaprank","meangap"};
    public String id() { return "I02"; }
    public String nameZh() { return "遗漏周期族"; }
    public String coreIdea() { return "号码间隔是否有周期"; }
    static int gen(IdeaFamilyR27.Context ctx) { return ctx == null ? 0 : ctx.generation; }
    public PolicySpecR27 generate(StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        int tp = IdeaFamilyR27.Grow.pos(r);
        PolicyAstR27.Node ast = IdeaFamilyR27.Grow.tree(KINDS, tp, 1 + r.nextInt(ctx == null ? 4 : Math.max(1, Math.min(4, ctx.maxDepth - 1))), r.chance(0.5), ctx, r);
        return IdeaFamilyR27.Grow.spec(ast, tp, r, IdeaFamilyR27.meta(id(), "GENERATE", gen(ctx), r.state, "号码间隔是否有周期"));
    }
    public PolicySpecR27 mutate(PolicySpecR27 p, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        return IdeaFamilyR27.Grow.swapLeaf(p, KINDS, id(), gen(ctx), ctx, r);
    }
}
