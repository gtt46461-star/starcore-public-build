# FR-B06-06 校准、概率目标和支持域

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-15。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/CalibrationRowsR27.java`

动作：**MODIFY**。施工范围：拟合与评估取行。

问题：两项ECE/BSS未达标，rolling必须严格早于被评价标签。

修改方式：prior-only校准，冻结拟合截止与支持范围；超支持域明确拒绝。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`0ad8ee0f426b6cf064b5c875dce3ca78930779f21b0708d7d84b3a04363b8461`。共享合并顺序：FR-B06-06 → FR-B08-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/CalibrationR30.java`

动作：**MODIFY**。施工范围：全部指标计算。

问题：等宽/等质量ECE及BSS目标可能混淆。

修改方式：并列样本分母、支持箱覆盖、BSS参考与区间；二元K3和十分类分开。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`5e8dd87f1536d4e3e2bea135d6cd0b51ec5518960408cd40ba285c4d157a175a`。共享合并顺序：FR-B06-06。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java`

动作：**MODIFY**。施工范围：calibrate。

问题：校准拟合存在不等Q05通过。

修改方式：输出原概率与校准后概率对照；只用合法旧标签，不能追调正式确认段。

原方法锚点：LiveShadowsR27.calibrate(RuntimeR27 rt,ResearchPoolR27.Worker w) L312–349

基线SHA256：`0d150194fbb8d0758ef7501c7657eb5fdcaf02bbc3a7f96a39c1802451f04188`。共享合并顺序：FR-B05-03 → FR-B05-04 → FR-B06-06。

## 实施步骤

1. 先修数据/目标/时序，再比较Platt/收缩等开发方案；复杂校准器不自动更好。
2. Q05仍按原R30门槛，不能靠换基率或删困难样本变成通过。

## 测试与反证

- 完美/失准/小样本/域外/迟到标签/重复期；原始两策略可复算前后proper score。

## 验收

时间违规0；达标才标校准合格，校准不等预测优势。

## 恢复与回滚

恢复最后合法校准器或拒绝缺支持动作，保留原概率。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
