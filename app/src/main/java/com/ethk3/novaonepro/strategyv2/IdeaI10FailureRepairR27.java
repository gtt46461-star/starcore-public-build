package com.ethk3.novaonepro.strategyv2;

import java.util.List;

/** R27-023 I10 失败修复族: 针对上一代冠军在 H_SELECT 的失败簇做局部结构修改. */
public final class IdeaI10FailureRepairR27 implements IdeaFamilyR27 {
    static final String[] KINDS = {"freq","gap","trans1","cross","crosscond","fdecay"};
    public String id() { return "I10"; }
    public String nameZh() { return "失败修复族"; }
    public String coreIdea() { return "针对上一代冠军在 H_SELECT 的失败簇做局部结构修改"; }
    static int gen(IdeaFamilyR27.Context ctx) { return ctx == null ? 0 : ctx.generation; }
    public PolicySpecR27 generate(StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        int tp = ctx != null && ctx.champion != null ? ctx.champion.position : IdeaFamilyR27.Grow.pos(r);
        PolicyAstR27.Node alt = IdeaFamilyR27.Grow.tree(KINDS, tp, 2 + r.nextInt(2), false, ctx, r);
        if (ctx == null || ctx.champion == null) return IdeaFamilyR27.Grow.spec(alt, tp, r, IdeaFamilyR27.meta(id(), "GENERATE", gen(ctx), r.state, "失败修复：尚无冠军，生成替代打分"));
        String cluster = ctx.failureCluster == null || ctx.failureCluster.isEmpty() ? "REGIME_LOW" : ctx.failureCluster;
        boolean low = cluster.endsWith("LOW");
        PolicyAstR27.Node champ = ctx.champion.ast;
        PolicyAstR27.Node gate = PolicyAstR27.leaf("regime", 128), thr = PolicyAstR27.constant(0.1);
        PolicyAstR27.Node ast = low ? PolicyAstR27.op("if", gate, thr, champ, alt) : PolicyAstR27.op("if", gate, thr, alt, champ);
        return ctx.champion.withAst(ast, IdeaFamilyR27.meta(id(), "REPAIR", gen(ctx), r.state, "失败簇" + cluster + "：在该状态下改用替代子打分", ctx.champion.policyHash()));
    }
    public PolicySpecR27 mutate(PolicySpecR27 p, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        return IdeaFamilyR27.Grow.swapLeaf(p, KINDS, id(), gen(ctx), ctx, r);
    }
}
