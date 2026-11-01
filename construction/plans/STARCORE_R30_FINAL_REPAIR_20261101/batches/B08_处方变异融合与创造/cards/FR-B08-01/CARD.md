# FR-B08-01 原子剔除研究→精确子代的完整示范链

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-14。

前驱：B07。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/DirectedResearchPolicyR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：按统计名字随意改AST无法证明测过同一个子代。

修改方式：预先生成明确counterfactual AST；只支持声明的add/mix/sub移除；hash完全一致才物化。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B08-01。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedComponentStudyR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：不同K3二元命中事件直接比较Brier会混目标。

修改方式：在H_SELECT对同位置同真实号码做归一化十分类Brier配对，产出处方和原始块。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B08-01。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`

动作：**MODIFY**。施工范围：stepBirth/stepGate/snapshot/restore。

问题：新处方可能绕过出生门或确认反馈回流。

修改方式：只接受研究角色处方入待生队列，仍跑合同/行为/预算gate；消费receipt与pending/游标同提交。

原方法锚点：EvolutionLoopR27.stepBirth() L160–201；EvolutionLoopR27.stepGate() L203–232；EvolutionLoopR27.snapshot() L487–519；EvolutionLoopR27.restore(String json,Config cfg,FeatureCacheR27 cache,Sink sink,Clock clock) L521–563

基线SHA256：`1c574ba07ac91efd900d29ed1ff02fbe659b9edf149e9d3de29ed36a4fd80580`。共享合并顺序：FR-B08-01 → FR-B08-03 → FR-B08-05。

## 实施步骤

1. 应用B08两新增类；先在任务外准备反事实，再按B07有界runner执行。
2. 正式确认只读展示，不进入stepBirth选择；每个处方有原parent、component和measured child hash。
3. 不能把样例整段evaluate直接放实时回调；接分片/预算后再启用全量。

## 测试与反证

- 测过子代A但消费B必须拒绝；父代AST不变；非法移除/常量空壳拒绝；事务回滚不标CONSUMED。

## 验收

至少一条真实“测量→处方→新AST→出生门→配对确认”链可复算；无收益仍如实失败。

## 恢复与回滚

关闭新处方operator，保留原创造器、父代和历史筛劣。

## 关键代码

顺序补丁：`patches/B08_measured_counterfactual_operator.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/DirectedResearchPolicyR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedComponentStudyR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/strategyv2/DirectedResearchPolicyR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Exact counterfactual candidate. It must be measured before any prescription may materialize it. */
public final class DirectedResearchPolicyR30R1 {
    private DirectedResearchPolicyR30R1(){}
    public static PolicySpecR27 withoutChild(PolicySpecR27 original,String path,int generation){
        if(path==null||!path.matches("root(?:/[0-9]+)+"))throw new IllegalArgumentException("PRUNING_PATH_REQUIRES_CHILD");
        PolicySpecR27 p=PolicySpecR27.parse(original.fullJson());String[] parts=path.split("/");
        PolicyAstR27.Node parent=p.ast;for(int i=1;i<parts.length-1;i++){int slot=Integer.parseInt(parts[i]);if(slot>=parent.kids.length)throw new IllegalArgumentException("PRUNING_PATH");parent=parent.kids[slot];}
        int slot=Integer.parseInt(parts[parts.length-1]);
        if(parent.kids.length!=2||slot>1||!(parent.op.equals("add")||parent.op.equals("mix")||parent.op.equals("sub")))
            throw new IllegalArgumentException("PRUNING_REQUIRES_DECLARED_ADDITIVE_COUNTERFACTUAL");
        PolicyAstR27.Node replacement=parent.kids[1-slot];
        if(parent.op.equals("sub")&&slot==0)replacement=PolicyAstR27.op("neg",replacement);
        List<PolicyAstR27.Node> nodes=PolicyAstR27.nodes(p.ast);int parentIndex=-1;
        for(int i=0;i<nodes.size();i++)if(nodes.get(i)==parent){parentIndex=i;break;}
        if(parentIndex<0)throw new IllegalStateException("PRUNING_PARENT_NOT_FOUND");
        PolicyAstR27.Node ast=PolicyAstR27.replace(p.ast,parentIndex,replacement);
        PolicyAstR27.type(ast);String bad=PolicyAstR27.rangeAndCausality(ast);if(bad!=null)throw new IllegalArgumentException(bad);
        PolicySpecR27 result=p.withAst(ast,new PolicySpecR27.Meta(p.meta==null?"I09":p.meta.idea,"MEASURED_PRUNE",Collections.singletonList(p.policyHash()),generation,0,"COUNTERFACTUAL_PREPARED:"+path));
        if(result.policyHash().equals(p.policyHash()))throw new IllegalArgumentException("PRUNING_NO_CHANGE");return result;
    }
    public static PolicySpecR27 materialize(ResearchAdviceR30R1 advice,ResearchContractR30R1 source,
            PolicySpecR27 original,String measuredCounterfactualJson,String measuredCounterfactualHash,int generation,long target,long preparedAt){
        source.requireAdvice(target,preparedAt);
        if(advice.action!=ResearchAdviceR30R1.Action.PRUNE_COMPONENT||!advice.parentPolicy.equals(original.policyHash())
                ||!advice.evidenceKey.equals(source.key("PAIRED_COMPONENT_ADVICE_V1|"+advice.analysisContractHash)))throw new IllegalStateException("PRUNING_EVIDENCE_IDENTITY");
        PolicySpecR27 child=PolicySpecR27.parse(measuredCounterfactualJson);
        PolicySpecR27 expected=withoutChild(original,advice.componentPath,generation);
        if(!child.policyHash().equals(ResearchContractR30R1.sha(measuredCounterfactualHash))||!child.policyHash().equals(expected.policyHash()))
            throw new IllegalStateException("UNMEASURED_COUNTERFACTUAL_SUBSTITUTION");
        return child.withMeta(new PolicySpecR27.Meta(child.meta==null?"I09":child.meta.idea,"MEASURED_PRUNE",Collections.singletonList(original.policyHash()),generation,0,"PROPOSAL:"+advice.proposalId+";"+advice.reason));
    }
}

```

`app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedComponentStudyR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

/** Complete H_SELECT analytical computation. Scheduling and the existing atomic writer remain external owners. */
public final class PairedComponentStudyR30R1 {
    public static final class Product {
        public final PolicySpecR27 counterfactual;public final ResearchGainR30R1.Estimate gain;public final ResearchAdviceR30R1 advice;
        Product(PolicySpecR27 p,ResearchGainR30R1.Estimate e,ResearchAdviceR30R1 a){counterfactual=p;gain=e;advice=a;}
    }
    private PairedComponentStudyR30R1(){}
    public static Product evaluate(ResearchContractR30R1 contract,PolicySpecR27 parent,String path,
            EvalContextR27 context,double frozenTemperature,int generation,int blockPeriods,long target,long preparedAt){
        contract.requireAdvice(target,preparedAt);
        if(contract.role!=ResearchContractR30R1.Role.SELECT||!contract.policyHash.equals(parent.policyHash())||!contract.datasetHash.equals(context.data.datasetHash))throw new IllegalArgumentException("STUDY_ROLE_OR_DATASET");
        int from=context.split.selectFrom,to=context.split.selectTo;
        if(from>=to||contract.fromOrdinal!=context.data.ordinals[from]||contract.throughOrdinal!=context.data.ordinals[to-1])throw new IllegalArgumentException("STUDY_REGISTERED_WINDOW");
        PolicySpecR27 without=DirectedResearchPolicyR30R1.withoutChild(parent,path,generation);
        ReplayEvaluatorR27.Trace with=ReplayEvaluatorR27.selectTraceWithProbabilities(parent,context,frozenTemperature);
        ReplayEvaluatorR27.Trace removed=ReplayEvaluatorR27.selectTraceWithProbabilities(without,context,frozenTemperature);
        ResearchGainR30R1 gain=new ResearchGainR30R1(contract.fromOrdinal,contract.throughOrdinal,blockPeriods,4,200);
        // Multiclass Brier pairs compare the same outcome even when the two policies choose different K3 sets.
        for(int t=from;t<to;t++){
            if(with.skipped[t]||removed.skipped[t]||with.probabilityRows[t]==null||removed.probabilityRows[t]==null)continue;
            double a=0,b=0;int actual=context.data.draws[t][parent.position-1]-1;
            for(int k=0;k<10;k++){double y=k==actual?1:0;a+=Math.pow(with.probabilityRows[t][k]-y,2);b+=Math.pow(removed.probabilityRows[t][k]-y,2);}
            // Divide by two: normalized 10-class Brier is bounded in [0,1]. Candidate=with component.
            gain.add(context.data.ordinals[t],b/2,a/2);
        }
        ResearchGainR30R1.Estimate e=gain.estimate();return new Product(without,e,ResearchAdviceR30R1.component(contract,path,e,target,preparedAt));
    }
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
