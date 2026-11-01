# FR-B08-05 预算、失败记忆与端到端信用

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-14/FIX-19。

前驱：B07。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`

动作：**MODIFY**。施工范围：任务资源统计与分派。

问题：CPU占用高而有效产出为0无法指导优化。

修改方式：按独立机制有效出生/完成确认/成本记录研究预算，不把忙等计能力。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`e80ba04df767f5130b537c3aa70fa5f50a9c9f4cc1b5bd309ca13815b0a85f05`。共享合并顺序：FR-B03-03 → FR-B03-04 → FR-B03-05 → FR-B07-04 → FR-B08-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`

动作：**MODIFY**。施工范围：opWeights/familySurvivors/研究提案接口。

问题：反复失败机制可能持续换名消耗。

修改方式：行为簇与反证记忆影响新研究预算，探索份额固定且保护有效资产。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`1c574ba07ac91efd900d29ed1ff02fbe659b9edf149e9d3de29ed36a4fd80580`。共享合并顺序：FR-B08-01 → FR-B08-03 → FR-B08-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`

动作：**EXTEND_NEW_FILE**。施工范围：预算/反证/端到端视图。

问题：贡献不能追到最终主链效果。

修改方式：R16–R18联结analysis→proposal→child→confirmation→experiment→certificate→REAL→settlement。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B03-03 → FR-B07-02 → FR-B07-04 → FR-B07-05 → FR-B08-05。

## 实施步骤

1. 所有统计分母有namespace/role/window；探索成本与正式质量收益分别报告。
2. “一代比一代强”落成冠军晋升合同，探索子代允许失败，生产冠军不能随意被劣质父代替换。

## 测试与反证

- 反证重复机制降额而新机制仍获探索；没有证书时端到端图明确停在资格。

## 验收

预算变化有消费回执，质量产出和失败都能解释，不承诺自然优势必出现。

## 恢复与回滚

回退预算版本，保留每一份反证和资产引用。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
