# FR-B10-04 覆盖安装、备份和迁移并发控制

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-20/FIX-21。

前驱：B00, B02, B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/DbHelper.java`

动作：**MODIFY**。施工范围：升级/恢复入口。

问题：新备份8.23GB与旧备份可共同占满磁盘。

修改方式：避免每次打开重复备份，记录已验证恢复点；不同签名/来源路径不得混用。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`5deaa12e31fd8fd97766fd91572cd4664dba13c31660cf41584ea0a051949a49`。共享合并顺序：FR-B10-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/SchemaR27.java`

动作：**MODIFY**。施工范围：ensure/readiness。

问题：旧Root/研究/legacy初始化互相等待会拖慢首启。

修改方式：每个域唯一Owner与依赖有向无环；报告critical/research/legacy的实际阶段与失败。

原方法锚点：SchemaR27.ensure(SQLiteDatabase db) L73–98

基线SHA256：`a9be5a2b116922f478554fff3e0d990ff53e433d278e0bd94118458a2d0cc305`。共享合并顺序：FR-B03-01 → FR-B10-04。

### `construction/r30-repair/B11_CUTOVER_DECISION.json`

动作：**ADD**。施工范围：完整新增文件。

问题：大库不等原R30条件分库已触发。

修改方式：只在原三项真机条件全满足时采用原B11方案，否则NOT_REQUIRED。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B10-04。

## 实施步骤

1. 现有用户数据必须覆盖保全；不盲目把当前缺schema通过readonly创建空表“修好”。
2. 原R30分库三条件需24h+头部R27写者+切片后仍失败，本次未证实就不启动切库。

## 测试与反证

- 旧库覆盖/失败回退/二次启动/备份空间不足/schema错ABI/并发读lease。

## 验收

启动/迁移可恢复且没有反复大副本；原签名口径安装。

## 恢复与回滚

恢复已验证旧库与路由，保留新原件等待复查。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
