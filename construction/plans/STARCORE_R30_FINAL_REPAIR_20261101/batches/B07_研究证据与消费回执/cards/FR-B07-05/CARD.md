# FR-B07-05 反证控制与失败机制记忆

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-11/FIX-14。

前驱：B04, B06。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/NullPipelineSimulationR30.java`

动作：**MODIFY**。施工范围：step/summary/控制统计。

问题：2个全管线零误授证不能证明误授证率很低。

修改方式：扩大独立重复，记录分母、CI、预算和检出功效；与自然角色隔离。

原方法锚点：NullPipelineSimulationR30.step() L27–33；NullPipelineSimulationR30.summary() L76–94

基线SHA256：`b1ad335fc3a52a1e167f1b25af09529ce5b4556ae2cab9b57ad9132452f8510c`。共享合并顺序：FR-B07-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/NullEvolutionR27.java`

动作：**MODIFY**。施工范围：控制流水线。

问题：控制计算量/筛选预算必须同池公平。

修改方式：使用同创造/选择流程与固定预算，明确合成不能授REAL。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`3c29e1be14827a021bfc59f8c9d2d0213418c29b113f600e0b63b033bc73cd3f`。共享合并顺序：FR-B07-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`

动作：**EXTEND_NEW_FILE**。施工范围：反证/失败记忆视图。

问题：同一失败机制可能反复换名消耗资源。

修改方式：按机制/输入/角色/预算版本存反证，后续提案引用，不永久删除整个家族。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B03-03 → FR-B07-02 → FR-B07-04 → FR-B07-05 → FR-B08-05。

## 实施步骤

1. 空信号、标签打乱、已知34/37/40%合成优势分别检验机制。
2. 研究预算和正式alpha记录独立，不能因控制输赢选择性停止或隐藏失败。

## 测试与反证

- 候选换名不绕过行为去重；合成结果不能进入自然样本和证书表。

## 验收

Q08报告有效分母和不确定性；失败记忆实际减少重复消耗且保留可复验新机制。

## 恢复与回滚

停止控制任务不影响真实观察；原模拟输出全部保留。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
