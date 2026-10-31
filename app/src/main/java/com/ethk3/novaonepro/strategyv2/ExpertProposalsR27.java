package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** R27-024 the 16 R26 experts as proposers (docs/04 4.4). Each proposal names the failure cluster, hypothesis,
 * structural change, expected mechanism and refutation condition. Audit experts (E01/E14/E15/E16) produce
 * receipts, not births. Proposals take at most 25% of a generation's birth quota (the loop enforces ≤10%). */
public final class ExpertProposalsR27 {
    private ExpertProposalsR27() {}
    public static final String[][] EXPERTS = {
        {"E01", "证据与泄漏审计"}, {"E02", "旧资产复现"}, {"E03", "状态窗口发现"}, {"E04", "多尺度关系发现"}, {"E05", "位置交互发现"}, {"E06", "号码组结构发现"},
        {"E07", "表征学习"}, {"E08", "程序与公式合成"}, {"E09", "残差失败修复"}, {"E10", "互补融合"}, {"E11", "学习状态迁移"}, {"E12", "校准与门控"},
        {"E13", "稳定性与漂移"}, {"E14", "资源与编译"}, {"E15", "资格与主链"}, {"E16", "诊断与复现实验"}};

    public static final class Proposal {
        public final String expert, failureCluster, hypothesis, structuralChange, mechanism, refutation; public final PolicySpecR27 spec;
        Proposal(String e, String c, String h, String s, String m, String r, PolicySpecR27 spec) { expert = e; failureCluster = c; hypothesis = h; structuralChange = s; mechanism = m; refutation = r; this.spec = spec; }
        public String json() {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("expert", expert); m.put("failureCluster", failureCluster); m.put("hypothesis", hypothesis); m.put("structuralChange", structuralChange);
            m.put("mechanism", mechanism); m.put("refutation", refutation); m.put("policyHash", spec == null ? null : spec.policyHash());
            return JsonR27.write(m);
        }
    }

    public static List<Proposal> propose(String expert, IdeaFamilyR27.Context ctx, StatsR27.SplitMix r) {
        PolicySpecR27 champ = ctx.champion; int g = ctx.generation;
        String cluster = ctx.failureCluster == null || ctx.failureCluster.isEmpty() ? "NONE" : ctx.failureCluster;
        int tp = champ == null ? IdeaFamilyR27.Grow.pos(r) : champ.position;
        String refute = "H_HOLDOUT 配对下界 ≤ 0 或不优于 I00/打乱标签对照 即判否";
        PolicySpecR27 base = champ != null ? champ : IdeaFamilyR27.Grow.spec(IdeaFamilyR27.Grow.tree(MutationOpsR27.ALL_KINDS, tp, 2, true, ctx, r), tp, r, null);
        PolicyAstR27.Node ast; String change, hyp, mech;
        switch (expert) {
            case "E02":
                if (ctx.legacySeeds.isEmpty()) return audit(expert, cluster, "旧资产种子为空：缺少原状态时记UNKNOWN，不捏造复现");
                PolicySpecR27 seed = ctx.legacySeeds.get(r.nextInt(ctx.legacySeeds.size()));
                return one(expert, cluster, "旧高分若可复现，应在回放上保持", "导入旧资产程序", "原执行器逐期一致", refute, seed.withMeta(meta(expert, g, r, "旧资产复现", seed)));
            case "E03": ast = PolicyAstR27.op("if", PolicyAstR27.leaf("regime", 512), PolicyAstR27.constant(0.1), base.ast, PolicyAstR27.op("zscore", PolicyAstR27.leaf("trans1"))); hyp = "全局弱但特定重复体制下有结构"; change = "加入状态窗口门控"; mech = "体制内条件分布不同"; break;
            case "E04": ast = PolicyAstR27.op("sub", PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", tp, 32)), PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", tp, 2048))); hyp = "短长尺度意见相反时短期偏离回归"; change = "短长窗口差分"; mech = "多尺度回归"; break;
            case "E05": ast = PolicyAstR27.op("add", PolicyAstR27.op("zscore", base.ast), PolicyAstR27.op("zscore", PolicyAstR27.leaf("crosscond", 1 + (tp % 10)))); hyp = "相邻位置上期号码影响本位"; change = "加入跨位置条件转移项"; mech = "跨位置依赖"; break;
            case "E06": ast = PolicyAstR27.op("rank", base.ast); hyp = "三元组丢失源于分数尺度不稳"; change = "排名化整体分数"; mech = "集合评分稳健化"; break;
            case "E07": ast = PolicyAstR27.op("mix", 0.5, PolicyAstR27.op("zscore", base.ast), PolicyAstR27.op("zscore", PolicyAstR27.leaf("family", 20))); hyp = "现有特征不能区分成败"; change = "加入片段检索家族表征"; mech = "F20表征"; break;
            case "E08": ast = IdeaFamilyR27.Grow.tree(MutationOpsR27.ALL_KINDS, tp, 4, true, ctx, r); hyp = "参数变异无进展需结构重写"; change = "重写完整Typed-AST"; mech = "符号结构搜索"; break;
            case "E09": ast = PolicyAstR27.op("if", PolicyAstR27.leaf("regime", 128), PolicyAstR27.constant(0.1), base.ast, PolicyAstR27.op("neg", PolicyAstR27.op("zscore", base.ast))); hyp = "特定状态反复失误"; change = "失败状态下反向残差分支"; mech = "定向残差修复"; break;
            case "E10":
                if (ctx.archive.size() < 2) return audit(expert, cluster, "档案成员不足2个，无法互补融合");
                PolicySpecR27 a = ctx.archive.get(r.nextInt(ctx.archive.size())), b = ctx.archive.get(r.nextInt(ctx.archive.size()));
                ast = PolicyAstR27.op("mix", 0.5, PolicyAstR27.op("zscore", a.ast), PolicyAstR27.op("zscore", b.ast)); hyp = "低相关专家互补"; change = "两成员等权融合"; mech = "互补降低共同失误"; base = a; break;
            case "E11": return one(expert, cluster, "子代从零学习过慢", "继承父代更新器并加大学习率", "状态迁移", refute,
                    base.withUpdater(Math.min(0.2, base.lr * 2 + 0.005), base.forget, meta(expert, g, r, "学习状态迁移：学习率加倍", base)));
            case "E12": return one(expert, cluster, "高分高频窗口反而低命中", "提高出手阈值", "选择后校准", refute,
                    base.withSelector(base.position, PolicySpecR27.Rule.MARGIN, Math.min(0.3, base.threshold + 0.03), 0, meta(expert, g, r, "校准与门控：阈值+0.03", base)));
            case "E13": ast = PolicyAstR27.op("ema", 0.3, base.ast); hyp = "最差块/连挂退化来自分数抖动"; change = "分数指数平滑"; mech = "稳定性"; break;
            default: return audit(expert, cluster, expert.equals("E01") ? "出生门已强制因果滞后≥1与PIT泄漏测试；无越界程序" : expert.equals("E14") ? "成本门：单期预测≤5ms、状态≤4MiB" : expert.equals("E15") ? "资格只由FUTURE序贯检验给出" : "任务健康由wait_kind枚举诊断");
        }
        PolicySpecR27 spec = base.withAst(ast, meta(expert, g, r, hyp + "；" + change, base));
        return one(expert, cluster, hyp, change, mech, refute, spec);
    }

    static PolicySpecR27.Meta meta(String expert, int g, StatsR27.SplitMix r, String reason, PolicySpecR27 parent) {
        return new PolicySpecR27.Meta("EXPERT:" + expert, "EXPERT_PROPOSAL", parent == null || parent.meta == null && parent.ast == null ? Collections.<String>emptyList() : Collections.singletonList(parent.policyHash()), g, r.state, "专家" + expert + "提案：" + reason);
    }
    static List<Proposal> one(String e, String c, String h, String s, String m, String refute, PolicySpecR27 spec) {
        List<String> violations;
        try { violations = PolicyContractR30.violations(spec); }
        catch (RuntimeException malformed) { return audit(e, c, "R30_CONTRACT:MALFORMED_PROGRAM:" + malformed.getClass().getSimpleName()); }
        if (!violations.isEmpty()) return audit(e, c, "R30_CONTRACT:" + violations);
        List<Proposal> l = new ArrayList<>(); l.add(new Proposal(e, c, h, s, m, refute, spec)); return l;
    }
    static List<Proposal> audit(String e, String c, String note) { List<Proposal> l = new ArrayList<>(); l.add(new Proposal(e, c, note, "无结构改动(审计回执)", "审计", "—", null)); return l; }
}
