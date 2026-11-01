# FR-B09-01 登记18个研究图的算法合同

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-18。

前驱：B08。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGraphSpecsR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：缺少图→分析算法→消费者→验收定义。

修改方式：R01–R18固定id/输入measure/consumer/verification，分3组每组6图。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B09-01。

## 实施步骤

1. 应用图合同补丁；每图对应研究数据合同和B08计算Owner。
2. 旧29图保留，18新图独立工作台；不把登记18个名字当18算法已运行。

## 测试与反证

- 18唯一id、3×6组、每图消费者和验证字段完整；未产出显示具体等待项。

## 验收

所有图都回答分析什么、怎样影响算法、如何验证收益。

## 恢复与回滚

隐藏新入口不影响旧29图与后台研究。

## 关键代码

顺序补丁：`patches/B09_research_graph_contracts.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGraphSpecsR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGraphSpecsR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Each view has a real analytical product and a named research consumer. No view itself mutates REAL state. */
public final class ResearchGraphSpecsR30R1 {
    public static final class Spec {
        public final String id,title,kind,measure,consumer,verification;public final int group;
        Spec(String id,String title,String kind,int group,String measure,String consumer,String verification){this.id=id;this.title=title;this.kind=kind;this.group=group;this.measure=measure;this.consumer=consumer;this.verification=verification;}
    }
    private static Spec s(String id,String title,String kind,int group,String measure,String consumer,String verification){return new Spec(id,title,kind,group,measure,consumer,verification);}
    public static final List<Spec> ALL=Collections.unmodifiableList(Arrays.asList(
      s("R01","多尺度结构状态与可用窗口","HEATMAP",0,"STRUCTURE_STATE","StructureGeneratorRegistryR30","固定窗口条件收益，禁止后验挑窗"),
      s("R02","结构预测与下一期落地误差","LINES",0,"STRUCTURE_FORECAST_ERROR","FailureProfileR30","事前结构概率与结算误差配对"),
      s("R03","号码位置兼容与跨位结构","GRAPH",0,"POSITION_COMPATIBILITY","IdeaI04CrossPositionR27","移除跨位输入的独立增益"),
      s("R04","多位置围号交集与缩减证据","STACK",0,"SET_INTERSECTION_SUPPORT","SetIntersectionPolicyR30","候选集合→单位置K3，支持不足弃权"),
      s("R05","失败簇→假设→可执行子代","GRAPH",0,"FAILURE_PRESCRIPTION","ExpertProposalsR27","实际子代hash与后续确认回执"),
      s("R06","原子条件边际贡献","BAR",0,"ATOM_PAIRED_GAIN","MutationOpsR27","同日历期分块配对及角色隔离"),
      s("R07","分子协同与冗余贡献","HEATMAP",1,"MOLECULE_SYNERGY","DerivedAtomR27","组合优于各单体且复杂度受控"),
      s("R08","思路族行为差异与机制覆盖","GRAPH",1,"BEHAVIOR_DIVERSITY","BirthGateR27","固定probe集行为hash及错误相关"),
      s("R09","专家处方落地与归因","BAR",1,"EXPERT_EXECUTABLE_CREDIT","ExpertProposalsR27","提案→AST→确认，文字不计产出"),
      s("R10","变异算子增益与计算成本","LINES",1,"OPERATOR_GAIN_COST","EvolutionLoopR27","研究算子预算改变及未来独立验证"),
      s("R11","父子同期配对收益与确认","LINE",1,"PARENT_CHILD_PAIRED","FrozenConfirmationR30","负结果保留，确认结果不回流进化选择"),
      s("R12","融合互补与共同失败区域","HEATMAP",1,"FUSION_COMPLEMENTARITY","FusionOpsR27","组合新身份新确认，不继承父证书"),
      s("R13","置信度失准与支持域风险","RELIABILITY",2,"CALIBRATION_SUPPORT","CalibrationRowsR27","prior-only拟合、域外拒绝、BSS并列"),
      s("R14","梯度→概率面→策略消费","GRAPH",2,"MODEL_CONSUMPTION_CHAIN","IdeaI08NeuralRepresentationR27","输入/梯度/发布水位及冻结消费逐键对应"),
      s("R15","状态漂移、遗忘与回放价值","LINES",2,"DRIFT_REPLAY_VALUE","AtomNetRuntimeR30","新旧模型同期评估及回滚对照"),
      s("R16","研究探索预算与有效产出","STACK",2,"RESEARCH_BUDGET","ResearchPoolR27","不忙等，按研究角色分配并保留探索份额"),
      s("R17","失败机制记忆与重复消耗","GRAPH",2,"FAILED_MECHANISM_MEMORY","EvolutionLoopR27","重复反证减少且有保护资产不被误淘汰"),
      s("R18","研究动作到主链效果全链","GRAPH",2,"END_TO_END_RECEIPTS","DecisionGateEngine","研究→新资产→资格→唯一REAL→结算，不跳门禁")
    ));
    private ResearchGraphSpecsR30R1(){}
    public static List<Spec> group(int group){if(group<0||group>2)throw new IllegalArgumentException("RESEARCH_GRAPH_GROUP");List<Spec>x=new ArrayList<>();for(Spec s:ALL)if(s.group==group)x.add(s);return Collections.unmodifiableList(x);}
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
