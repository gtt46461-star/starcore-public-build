package com.ethk3.novaonepro;

/** Read-only concrete algorithm/formula catalog shown even before enough OOS exists to evaluate it. */
public final class ResearchStrategyCatalogR820 {
    public static final int FORMULA_COUNT = ResearchCandidateFormulaLibraryR3611.SEEDS.length;
    private ResearchStrategyCatalogR820() {}

    public static String compactStatus(String runState) {
        return "策略公式库=" + FORMULA_COUNT + "条（具体DSL字符已内置）"
                + "｜TinyMLP=96→64→32→100+10x10 Sinkhorn"
                + "｜在线动态迭代=FTRL"
                + "｜状态=" + safe(runState)
                + "｜有效性=尚待严格OOS证明";
    }

    public static String fullCatalog() {
        StringBuilder b = new StringBuilder(18000);
        b.append("【R900真实策略/方法/算法/公式字符目录】\n");
        b.append("边界=RESEARCH_ONLY/SHADOW_CHALLENGER；不是空泛描述，也不把未验证公式冒充高胜率。\n");
        b.append("正式动作空间：A={(p,S):p∈1..10,S⊂{1..10},|S|=3}，|A|=10*C(10,3)=1200。\n");
        b.append("动作基础分示意：Score(S)=Σ[n∈S]log(max(eps,Q[p,n])) + research_formula(features)，最终同一期1200动作组内排序。\n");
        b.append("TinyMLP：x[96]；h1=ReLU(W1*x+b1)[64]；h2=ReLU(W2*h1+b2)[32]；z=W3*h2+b3[100]；Q=log_domain_Sinkhorn(reshape10x10(z))。\n");
        b.append("Loss=L_ce+lambda2*ΣW²；L_ce=-Σ_p log(max(eps,Q[p,actual_p]))；Adam beta1=.9,beta2=.999,eps=1e-8,globalClip=5；lr∈{1e-4,3e-4,1e-3}。\n");
        b.append("FTRL在线更新：p=sigmoid(w·x)；g=(p-y)x；sigma=(sqrt(n+g²)-sqrt(n))/alpha；z+=g-sigma*w；n+=g²；")
                .append("w=0 if |z|<=l1 else -(z-sign(z)l1)/((beta+sqrt(n))/alpha+l2)；alpha=.05,beta=1,l1=.001,l2=.01。\n");
        b.append("严格时序：draw(t)→Settlement/OOS(t)→FTRL.update(t)→trainedUntil=t→earliestUse(t+1)，禁止未来回填学习。\n");
        b.append("研究晋级门：探索生成N>=12；walk-forward N>=120；SHADOW_CHALLENGER N>=240；Production仍需独立冻结未来样本证书，Research本身无REAL权限。\n");
        b.append("\n-- 60条可执行DSL公式 --\n");
        for (int i = 0; i < ResearchCandidateFormulaLibraryR3611.SEEDS.length; i++) {
            String[] s = ResearchCandidateFormulaLibraryR3611.SEEDS[i];
            b.append(s[0]).append('|').append(s[1]).append('|').append(s[2]).append('\n');
        }
        return b.toString();
    }

    private static String safe(String s) { return s == null || s.trim().length() == 0 ? "N/A" : s.trim(); }
}
