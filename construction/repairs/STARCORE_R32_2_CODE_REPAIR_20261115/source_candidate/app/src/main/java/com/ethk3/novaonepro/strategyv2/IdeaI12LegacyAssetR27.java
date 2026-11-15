package com.ethk3.novaonepro.strategyv2;

import java.util.List;

/** R27-023 I12 旧资产族: 原多集合×多位置交叉交集等旧策略复现为程序并再拟合. */
public final class IdeaI12LegacyAssetR27 implements IdeaFamilyR27 {
    static final String[] KINDS = {"freq","freqnb","fdecay"};
    public String id() { return "I12"; }
    public String nameZh() { return "旧资产族"; }
    public String coreIdea() { return "原多集合×多位置交叉交集等旧策略复现为程序并再拟合"; }
    static int gen(IdeaFamilyR27.Context ctx) { return ctx == null ? 0 : ctx.generation; }
    public PolicySpecR27 generate(StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        if (ctx != null && !ctx.legacySeeds.isEmpty() && r.chance(0.5)) {
            PolicySpecR27 s = ctx.legacySeeds.get(r.nextInt(ctx.legacySeeds.size()));
            return s.withMeta(IdeaFamilyR27.meta(id(), "LEGACY_SEED", gen(ctx), r.state, "旧资产种子再拟合：" + (s.meta == null ? "" : s.meta.reason), s.policyHash()));
        }
        int tp = IdeaFamilyR27.Grow.pos(r), w1 = IdeaFamilyR27.Grow.window(r), w2 = IdeaFamilyR27.Grow.window(r), other = IdeaFamilyR27.Grow.pos(r);
        // Multi-set x multi-position cross intersection: agreement of hot ranks across windows and a second position.
        PolicyAstR27.Node ast = PolicyAstR27.op("add", PolicyAstR27.op("rank", PolicyAstR27.leaf("freq", tp, w1)),
                PolicyAstR27.op("add", PolicyAstR27.op("rank", PolicyAstR27.leaf("freq", tp, w2)), PolicyAstR27.op("rank", PolicyAstR27.leaf("freq", other, w1))));
        return IdeaFamilyR27.Grow.spec(ast, tp, r, IdeaFamilyR27.meta(id(), "GENERATE", gen(ctx), r.state, "旧策略复现：多窗口×多位置热号交集"));
    }
    public PolicySpecR27 mutate(PolicySpecR27 p, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        return IdeaFamilyR27.Grow.swapLeaf(p, KINDS, id(), gen(ctx), ctx, r);
    }
}
