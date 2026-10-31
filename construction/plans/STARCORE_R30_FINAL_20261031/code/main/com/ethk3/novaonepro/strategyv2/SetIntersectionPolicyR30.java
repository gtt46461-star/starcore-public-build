package com.ethk3.novaonepro.strategyv2;

/**
 * R30-B04-04 思路族 I14：集合交叉（现行“多集合×多位置交叉、固定 Rank1/K3”方法的可进化表达）。
 * 生成的打分程序：mix(α, freq(p, w1), freqnb(p, w2))，w1 < w2 取自 FeatureCacheR27.WINDOWS；两叶子类型同为 PROB（合同可通过）。
 * 它只是候选生成器：出生后与所有策略同样过 PolicyContractR30、观察、序贯资格与证书门，不享有任何豁免。
 */
public final class SetIntersectionPolicyR30 implements IdeaFamilyR27 {
    static final int[] W = {8, 32, 128, 512};
    public String id() { return "I14"; }
    public String nameZh() { return "集合交叉族"; }
    public String coreIdea() { return "同位置短窗频率与邻位置长窗频率的交叉"; }
    static int gen(IdeaFamilyR27.Context ctx) { return ctx == null ? 0 : ctx.generation; }

    public PolicySpecR27 generate(StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        int tp = IdeaFamilyR27.Grow.pos(r);
        int i = r.nextInt(W.length - 1), j = i + 1 + r.nextInt(W.length - 1 - i);
        double alpha = 0.2 + 0.6 * r.nextDouble();
        PolicyAstR27.Node ast = PolicyAstR27.op("mix", alpha, PolicyAstR27.leaf("freq", tp, W[i]), PolicyAstR27.leaf("freqnb", tp, W[j]));
        return IdeaFamilyR27.Grow.spec(ast, tp, r, IdeaFamilyR27.meta(id(), "GENERATE", gen(ctx), r.state,
                "集合交叉：第" + tp + "位 近" + W[i] + "期频率 × 邻位近" + W[j] + "期频率，α=" + String.format(java.util.Locale.ROOT, "%.2f", alpha)));
    }

    public PolicySpecR27 mutate(PolicySpecR27 p, StatsR27.SplitMix r, IdeaFamilyR27.Context ctx) {
        return IdeaFamilyR27.Grow.swapLeaf(p, new String[] {"freq", "freqnb", "fdecay"}, id(), gen(ctx), ctx, r);
    }
}
