package com.ethk3.novaonepro.strategyv2;

import java.util.List;

/** R27-023 I13 出手选择族: 不改预测，只改何时出手、出哪个位置. */
public final class IdeaI13SelectorOnlyR27 implements IdeaFamilyR27 {
    static final String[] KINDS = {"freq","fdecay"};
    public String id() { return "I13"; }
    public String nameZh() { return "出手选择族"; }
    public String coreIdea() { return "不改预测，只改何时出手、出哪个位置"; }
    static int gen(IdeaFamilyR27.Context ctx) { return ctx == null ? 0 : ctx.generation; }
    public PolicySpecR27 generate(StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        PolicySpecR27 base = ctx != null && !ctx.archive.isEmpty() ? ctx.archive.get(r.nextInt(ctx.archive.size())) : null;
        if (base == null) {
            int tp = IdeaFamilyR27.Grow.pos(r);
            PolicySpecR27 seed = IdeaFamilyR27.Grow.spec(IdeaFamilyR27.Grow.tree(KINDS, tp, 2, true, ctx, r), tp, r, IdeaFamilyR27.meta(id(), "GENERATE", gen(ctx), r.state, "出手选择：基础打分"));
            return SelectorPolicyR30.combine(seed, seed.position, seed.rule, seed.threshold, seed.rate, ctx == null ? null : ctx.evaluation, seed.meta);
        }
        return selector(base, r, ctx);
    }
    public PolicySpecR27 mutate(PolicySpecR27 p, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        return selector(p, r, ctx);
    }
    static PolicySpecR27 selector(PolicySpecR27 base, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        int pos = r.chance(0.3) ? IdeaFamilyR27.Grow.pos(r) : base.position;
        if (r.chance(0.5)) {
            double rate = IdeaFamilyR27.Grow.round(StatsR27.clamp((base.rate > 0 ? base.rate : 0.15) * Math.exp(0.5 * r.gaussian()), 0.02, 0.5));
            return SelectorPolicyR30.combine(base, pos, PolicySpecR27.Rule.QUANTILE, 0, rate, ctx == null ? null : ctx.evaluation, IdeaFamilyR27.meta("I13", "M_SELECTOR", ctx == null ? 0 : ctx.generation, r.state, "只改出手规则：置信度分位出手率→" + rate + "，位置" + base.position + "→" + pos, base.policyHash()));
        }
        double th = IdeaFamilyR27.Grow.round(StatsR27.clamp(base.threshold + 0.03 * r.gaussian(), 0, 0.3));
        return SelectorPolicyR30.combine(base, pos, PolicySpecR27.Rule.MARGIN, th, 0, ctx == null ? null : ctx.evaluation, IdeaFamilyR27.meta("I13", "M_SELECTOR", ctx == null ? 0 : ctx.generation, r.state, "只改出手阈值/位置：阈值" + base.threshold + "→" + th + "，位置" + base.position + "→" + pos, base.policyHash()));
    }
}
