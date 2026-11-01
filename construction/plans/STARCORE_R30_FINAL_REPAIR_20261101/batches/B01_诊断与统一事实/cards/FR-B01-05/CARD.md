# FR-B01-05 补齐逐图、逐矩阵、逐能力和前次报告对比

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-01/FIX-18。

前驱：B00。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`

动作：**MODIFY**。施工范围：rowsets/metrics。

问题：旧限量字段省略结果原因，逐对象统计不足。

修改方式：导出对象总量、显示量、遗漏量、完整索引与原件位置；大字段采用hash引用。

原方法锚点：ReportSnapshotR30.metrics(Query q,long at) L86–106；ReportSnapshotR30.rowsets(Query q) L107–137

基线SHA256：`2dfb38a9bcb05d76cf5b72432dacd6428697d1a152ecf4aef53682deb9010360`。共享合并顺序：FR-B01-05 → FR-B03-02 → FR-B05-01。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportDataR27.java`

动作：**MODIFY**。施工范围：load/chaptersR30。

问题：摘要缺前次jobId对比。

修改方式：读取上次成功摘要，按同版本/同对象/同角色比较，重复jobId去重。

原方法锚点：ReportDataR27.load(SQLiteDatabase db) L20–79；ReportDataR27.chaptersR30(SQLiteDatabase db) L86–86

基线SHA256：`3ed5d7386cd13fb0c1ba22df214c5a3cbb9f9812bda00cb0a1ac0c3467182e9d`。共享合并顺序：FR-B01-05 → FR-B07-03 → FR-B09-05。

### `construction/r30-repair/REPORT_OBJECT_SCHEMA.json`

动作：**ADD**。施工范围：完整新增文件。

问题：页面是否可开、算法是否有效混用。

修改方式：固定implemented/callable/inputAvailable/produced/consumed/effective六状态。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B01-05。

## 实施步骤

1. 29旧图、18新图、20矩阵、82能力分别逐对象输出，带代码与分析产物链接。
2. 96项检测执行率与产品通过率分开，别名缺陷按根因聚类。

## 测试与反证

- 重复B快照不当第三时段；跨进程累计计数不直接相减；部分展示不替代全分母。

## 验收

用户能从任何对象看到“现在行不行、缺什么、改哪里、如何验证”。

## 恢复与回滚

保留旧摘要及新schema版本，不丢原报告。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
