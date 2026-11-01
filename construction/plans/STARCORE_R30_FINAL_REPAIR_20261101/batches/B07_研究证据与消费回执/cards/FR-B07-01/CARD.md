# FR-B07-01 机器可读研究证据和配对贡献核心

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-13/FIX-14。

前驱：B04, B06。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGainR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：多位置/多策略可能虚增独立样本，重复选窗造高分。

修改方式：日历期聚类、预先固定块、缺口/尾块披露；分析合同hash含块长/最小样本/统计法。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B07-01。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchAdviceR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：图表只给统计，没有可执行处方。

修改方式：只有研究角色与完整支持的负贡献才生成PRUNE_COMPONENT；正贡献仅KEEP_FOR_CONFIRMATION。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B07-01。

## 实施步骤

1. 应用B07核心补丁；区间为研究描述用途，不能授REAL证书。
2. 计算with组件与without组件的同结果多分类Brier差，不能比较各自不同K3事件的二元Brier当成同一目标。

## 测试与反证

- 重复期、错窗口、正式角色、缺块、尾块、零/正/负增益；更改分析参数必须改变合同身份。

## 验收

真实分析结果能形成受约束处方；未知不出伪处方。

## 恢复与回滚

停新处方产生，保留所有已提交研究原件。

## 关键代码

顺序补丁：`patches/B07_paired_research_receipts.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGainR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchAdviceR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGainR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Fixed, predeclared calendar blocks. Descriptive research evidence, never a certificate test. */
public final class ResearchGainR30R1 {
    private final long from, to; private final int blockSize,minimumBlocks,minimumIssues;
    private final TreeMap<Long,Double> differences=new TreeMap<>();
    public ResearchGainR30R1(long from,long to,int blockSize,int minBlocks,int minIssues){
        if(from<0||to<from||blockSize<1||minBlocks<2||minIssues<1)throw new IllegalArgumentException("GAIN_WINDOW");
        this.from=from;this.to=to;this.blockSize=blockSize;minimumBlocks=minBlocks;minimumIssues=minIssues;
    }
    public void add(long issueOrdinal,double baselineLoss,double candidateLoss){
        if(issueOrdinal<from||issueOrdinal>to||!Double.isFinite(baselineLoss)||!Double.isFinite(candidateLoss)
                ||baselineLoss<0||baselineLoss>1||candidateLoss<0||candidateLoss>1)throw new IllegalArgumentException("GAIN_OBSERVATION");
        // One calendar issue, already clustered over positions/components. Never count 100 cells as 100 issues.
        if(differences.putIfAbsent(issueOrdinal,baselineLoss-candidateLoss)!=null)
            throw new IllegalStateException("DUPLICATE_CALENDAR_ISSUE");
    }
    public static final class Estimate {
        public final long fromOrdinal,throughOrdinal;
        public final int blockSize,observedCalendarIssues,evaluatedIssues,completeBlocks,incompleteBlocks,incompleteFullBlocks,excludedTailIssues,missingIssues;
        public final Double meanBlockGain,lower95,upper95;
        public final String analysisContractHash;
        public final String status="DESCRIPTIVE_RESEARCH_ONLY";
        Estimate(long from,long to,int size,int minBlocks,int minIssues,int n,int evaluated,int blocks,int incomplete,int incompleteFull,int tail,int missing,Double mean,Double lower,Double upper){fromOrdinal=from;throughOrdinal=to;blockSize=size;observedCalendarIssues=n;evaluatedIssues=evaluated;completeBlocks=blocks;incompleteBlocks=incomplete;incompleteFullBlocks=incompleteFull;excludedTailIssues=tail;missingIssues=missing;meanBlockGain=mean;lower95=lower;upper95=upper;analysisContractHash=ResearchContractR30R1.hash("PAIRED_BLOCK_V1|"+from+'|'+to+'|'+size+'|'+minBlocks+'|'+minIssues+"|TAIL=EXCLUDE_FIXED_REMAINDER|LOSS=MULTICLASS_BRIER_HALF|STAT=MEAN_BLOCK_T95_DESCRIPTIVE");}
        public boolean supported(){return lower95!=null&&upper95!=null;}
    }
    public Estimate estimate(){
        List<Double> means=new ArrayList<>();int incomplete=0,incompleteFull=0,tail=0,completeIssues=0;
        for(long start=from;start<=to;){
            long remaining=to-start+1;int expected=(int)Math.min((long)blockSize,remaining);
            double sum=0;int n=0;for(int j=0;j<expected;j++){Double d=differences.get(start+j);if(d!=null){sum+=d;n++;}}
            // Partial final blocks are disclosed and excluded using the registered rule, never their score.
            if(expected==blockSize&&n==expected){means.add(sum/n);completeIssues+=n;}else {incomplete++;if(expected==blockSize)incompleteFull++;else tail=expected;}
            if(remaining<=blockSize)break;start+=blockSize;
        }
        long planned=to-from+1;int missing=(int)Math.min(Integer.MAX_VALUE,planned-differences.size());
        if(means.size()<minimumBlocks||completeIssues<minimumIssues)
            return new Estimate(from,to,blockSize,minimumBlocks,minimumIssues,differences.size(),completeIssues,means.size(),incomplete,incompleteFull,tail,missing,null,null,null);
        double mean=0;for(double x:means)mean+=x;mean/=means.size();double ss=0;for(double x:means)ss+=(x-mean)*(x-mean);
        double se=Math.sqrt(ss/(means.size()-1)/means.size()),t=critical95(means.size()-1);
        return new Estimate(from,to,blockSize,minimumBlocks,minimumIssues,differences.size(),completeIssues,means.size(),incomplete,incompleteFull,tail,missing,mean,mean-t*se,mean+t*se);
    }
    static double critical95(int df){double[] t={0,12.706,4.303,3.182,2.776,2.571,2.447,2.365,2.306,2.262,2.228,2.201,2.179,2.160,2.145,2.131,2.120,2.110,2.101,2.093,2.086,2.080,2.074,2.069,2.064,2.060,2.056,2.052,2.048,2.045,2.042};if(df<1)throw new IllegalArgumentException("DF");return df<t.length?t[df]:2.042;}
}

```

`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchAdviceR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

/** A research prescription requires measured evidence and never confers live eligibility. */
public final class ResearchAdviceR30R1 {
    public enum Action { NONE, PRUNE_COMPONENT, KEEP_FOR_CONFIRMATION }
    public final String proposalId,evidenceKey,parentPolicy,componentPath,reason,analysisContractHash;
    public final Action action;
    private ResearchAdviceR30R1(ResearchContractR30R1 c,String component,ResearchGainR30R1.Estimate e,Action action,String reason){
        analysisContractHash=e.analysisContractHash;evidenceKey=c.key("PAIRED_COMPONENT_ADVICE_V1|"+analysisContractHash);parentPolicy=c.policyHash;componentPath=component;this.action=action;this.reason=reason;
        proposalId=ResearchContractR30R1.hash(evidenceKey+'|'+component+'|'+action.name());
    }
    public static ResearchAdviceR30R1 component(ResearchContractR30R1 c,String component,ResearchGainR30R1.Estimate e,long target,long preparedAt){
        c.requireAdvice(target,preparedAt);if(component==null||!component.matches("root(?:/[0-9]+)*"))throw new IllegalArgumentException("COMPONENT_PATH");
        if(e.fromOrdinal!=c.fromOrdinal||e.throughOrdinal!=c.throughOrdinal)throw new IllegalArgumentException("GAIN_CONTRACT_WINDOW_MISMATCH");
        // estimate compares candidate-with-component to otherwise-identical baseline-without-component.
        if(!e.supported()||e.missingIssues>0||e.incompleteFullBlocks>0)return new ResearchAdviceR30R1(c,component,e,Action.NONE,"INSUFFICIENT_OR_INCOMPLETE_PAIRED_RESEARCH");
        if(e.upper95<0)return new ResearchAdviceR30R1(c,component,e,Action.PRUNE_COMPONENT,"COMPONENT_HARMFUL_IN_REGISTERED_RESEARCH_BLOCKS");
        if(e.lower95>0)return new ResearchAdviceR30R1(c,component,e,Action.KEEP_FOR_CONFIRMATION,"RESEARCH_GAIN_ONLY_REQUIRES_NEW_INDEPENDENT_CONFIRMATION");
        return new ResearchAdviceR30R1(c,component,e,Action.NONE,"PAIRED_RESEARCH_INTERVAL_CONTAINS_ZERO");
    }
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
