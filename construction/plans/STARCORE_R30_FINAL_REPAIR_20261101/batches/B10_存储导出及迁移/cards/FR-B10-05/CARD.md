# FR-B10-05 存储与并发故障矩阵

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-20/FIX-22。

前驱：B00, B02, B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `construction/r30-repair/FAULT_MATRIX.json`

动作：**ADD**。施工范围：完整新增文件。

问题：旧CI14格不自动覆盖新研究表/消费链。

修改方式：扩展BUSY、FULL、CANCEL、KILL、CORRUPT、SOURCE_CHANGE×新关键阶段，逐格真实回执。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B10-05。

### `app/src/androidTest/java/com/ethk3/novaonepro/R30FaultMatrixTest.java`

动作：**MODIFY**。施工范围：新增研究链与存储阶段故障用例。

问题：新增处方/消费/快照生命周期尚无Android注入验证。

修改方式：保留原14格，新增场景逐格验证无半发布、无重复学习、无错授证。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`c6c771aa6516c4fa003238835d1f5fa99b2399eb62e3a64a296eb6f5b7281bf7`。共享合并顺序：FR-B10-05。

## 实施步骤

1. 故障注入只针对测试DB/测试设备夹具，不把控制信号写入自然资格。
2. 故障后重启再跑，比较原artifact/行数/hash/游标和真实意向。

## 测试与反证

- 每格有触发证据而非仅“测试调用成功”；自动审查未通过不得标PASS。

## 验收

新旧事务不变量、恢复与错误解释均有证据。

## 恢复与回滚

关闭注入标志，恢复隔离测试原件；不触及生产自然成绩。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
