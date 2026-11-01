# FR-B08-04 校准、模型消费与漂移研究

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-08/FIX-09/FIX-15。

前驱：B07。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/CalibrationRowsR27.java`

动作：**MODIFY**。施工范围：研究支持域输出。

问题：单一ECE无法驱动有针对性的研究。

修改方式：输出按状态/置信度的失准与域外分布，收缩/重训处方限研究角色。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`0ad8ee0f426b6cf064b5c875dce3ca78930779f21b0708d7d84b3a04363b8461`。共享合并顺序：FR-B06-06 → FR-B08-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java`

动作：**MODIFY**。施工范围：在线/回放状态报告。

问题：训练忙碌不等有效学习，遗忘可能损害旧能力。

修改方式：记录梯度→概率面→冻结调用及新旧状态分块得分；同源对照后才调研究回放/遗忘。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`562ade6660d38a48a5dc831d550e8606c9befa6566168db404fabb86e7c054ba`。共享合并顺序：FR-B04-04 → FR-B08-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceStoreR30.java`

动作：**MODIFY**。施工范围：消费链证据。

问题：模型有checkpoint但没有实际调用时图仍可能好看。

修改方式：R14完整展示准备/训练/发布/消费/冻结/结算断点，不补调用。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`4a7605681eb69bfb4369bcfc1ede418de5fec9d13104dc54a3d25ced75c4938f`。共享合并顺序：FR-B04-04 → FR-B04-05 → FR-B08-04。

## 实施步骤

1. 实现R13–R15；漂移由输入变化与proper score变化分开判断，不凭连输几期直接重置模型。
2. 正式确认/资格数据不能被工作台交互拿去调参。

## 测试与反证

- 输入漂移无性能退化、性能退化无输入漂移、缺支持域、旧能力回退、错模型hash。

## 验收

有证据驱动的合法研究处方，原有效模型不会因噪声被随意清空。

## 恢复与回滚

恢复最后合法研究/模型配置，保留完整优化器状态。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
