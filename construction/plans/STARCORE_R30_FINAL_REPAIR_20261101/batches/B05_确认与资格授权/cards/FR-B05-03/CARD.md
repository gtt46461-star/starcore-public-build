# FR-B05-03 资格缺期4实验逐项恢复或封段

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-06。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationInboxR29.java`

动作：**MODIFY**。施工范围：plan/推进与重建。

问题：连续前缀停在MISSING_OPPORTUNITY，等待新数据不能越过。

修改方式：导出逐实验缺口、原边界与恢复来源；仅恢复原始不可变predraw；无原件依原合同保守退役/封段。

原方法锚点：QualificationInboxR29.plan(SequentialQualificationR27.State s,List<Row> rows,long latestOrdinal,int max) L72–74；QualificationInboxR29.plan(SequentialQualificationR27.State s,List<Row> rows,long latestOrdinal,int max,long firstOrdinal) L75–105

基线SHA256：`31182b8628be76939303fe626485000891f933040b83b1e66865438ab02b15cd`。共享合并顺序：FR-B05-03。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java`

动作：**MODIFY**。施工范围：实时冻结/结算。

问题：所有日历机会必须有明确终态。

修改方式：原冻结/合法事前弃权/故障缺失分开，不能赛后补SKIP。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`0d150194fbb8d0758ef7501c7657eb5fdcaf02bbc3a7f96a39c1802451f04188`。共享合并顺序：FR-B05-03 → FR-B05-04 → FR-B06-06。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/SequentialQualificationR27.java`

动作：**MODIFY**。施工范围：预算/累计状态。

问题：重新登记可能错误重置历史alpha。

修改方式：原e-process和预算永久连续；新实验身份与退役原因可追。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`a5634e3c2ef81cd11307d304ec86b7bf384488c71935180dd87b3aed81f29a9c`。共享合并顺序：FR-B05-03。

## 实施步骤

1. 4实验含2控制和2ACTIVE逐项排查，不能把0 pendingDraw当成无缺口。
2. 同期开奖去重；恢复原件后重算连续前缀；无原件不伪造历史贡献。

## 测试与反证

- 真原件恢复、确认缺原件、ACT缺开奖、冲突、重新启动；重建前后alpha一致。

## 验收

每个阻断有具体处置且合法链持续推进；未处置不自动跳过。

## 恢复与回滚

封存受影响链并保留证据，其余健康研究不受影响。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
