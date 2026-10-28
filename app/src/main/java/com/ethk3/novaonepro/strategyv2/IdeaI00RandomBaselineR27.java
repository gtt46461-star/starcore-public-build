package com.ethk3.novaonepro.strategyv2;

import java.util.List;

/** R27-023 I00 随机基线族: 对照组：与冠军同出手率的随机选号（负对照，必须保留）. */
public final class IdeaI00RandomBaselineR27 implements IdeaFamilyR27 {
    static final String[] KINDS = {};
    public String id() { return "I00"; }
    public String nameZh() { return "随机基线族"; }
    public String coreIdea() { return "对照组：与冠军同出手率的随机选号（负对照，必须保留）"; }
    static int gen(IdeaFamilyR27.Context ctx) { return ctx == null ? 0 : ctx.generation; }
    public PolicySpecR27 generate(StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        double rate = StatsR27.clamp(ctx == null ? 0.15 : ctx.championActRate, 0.01, 0.6);
        PolicyAstR27.Node ast = PolicyAstR27.leaf("randu", r.nextInt(1 << 20));
        return new PolicySpecR27(ast, 1 + r.nextInt(10), PolicySpecR27.Rule.RANDOM, 0, IdeaFamilyR27.Grow.round(rate), 1.0, false, 0, 0,
                IdeaFamilyR27.meta(id(), "GENERATE", ctx == null ? 0 : ctx.generation, r.state, "随机基线：出手率与冠军一致(" + rate + ")"));
    }
    public PolicySpecR27 mutate(PolicySpecR27 p, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        return null; // the control never evolves
    }
}
