# FR-B06-04 原子/分子目录到可执行贡献闭环

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-13。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomWorldR27.java`

动作：**MODIFY**。施工范围：atomsStep/derivedStep。

问题：25350目录原子/176评分/46465分子属于不同集合。

修改方式：用namespace与compiled/inputAvailable/consumed区分，不强行要求数量相等。

原方法锚点：AtomWorldR27.atomsStep(RuntimeR27 rt,ResearchPoolR27.Worker w) L19–39

基线SHA256：`64e3ffeb3e3861f85898b0d6cb1987ddc514fb93aede70af657ec1043b6ffcce`。共享合并顺序：FR-B06-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomScoreR27.java`

动作：**MODIFY**。施工范围：贡献评分。

问题：研究段得分与未来有效性混淆。

修改方式：同日历期/同目标的加入移除配对；输出分块区间与成本，角色明示。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`ffa0ebbfe2cf9a23a0fb8633972caf39cbea31096f2987e186a7e350f87b8ac1`。共享合并顺序：FR-B06-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/DerivedAtomR27.java`

动作：**MODIFY**。施工范围：生成和评分。

问题：组合数量不等协同增益。

修改方式：对分子与单体同窗比较，测冗余与复杂度；新生与旧受保护引用分开。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`14c80bb47e46c309a53e82e280f5523b5313ee753388a6cb15a4e2313394964e`。共享合并顺序：FR-B06-04。

## 实施步骤

1. 保留历史快速筛劣；无效对象停止新优先采样但旧依赖不删除。
2. 新组件必须通过类型/PIT/shape/预算合同，不任意数学式当策略。

## 测试与反证

- 有益/无效/有害/缺失/重复组件；移除对照的目标一致。

## 验收

原子贡献实际改变下一代研究输入，且有新子代和验证回执。

## 恢复与回滚

回退采样分布，原目录与模型依赖不丢。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
