# FR-B09-05 47图与报告、研究动作的联合验收

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-18/FIX-22。

前驱：B08。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportChartsR30.java`

动作：**MODIFY**。施工范围：panels/evolution/capabilities。

问题：原2新图仅登记/配对概览，不能覆盖全部研究用途。

修改方式：联结工作台18图索引，分别报告静态、运行、消费、质量。

原方法锚点：ReportChartsR30.evolution(ReportR27.R30Input in) L14–40；ReportChartsR30.capabilities(ReportR27.R30Input in) L41–54；ReportChartsR30.panels(ReportR27.R30Input in) L55–55

基线SHA256：`d07cbf0279db9be9bab9800644cf678fe6282cf56379abd4eb3dec36210ef575`。共享合并顺序：FR-B09-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportDataR27.java`

动作：**MODIFY**。施工范围：load/chaptersR30。

问题：缺少逐图源对象与产物/动作链接。

修改方式：导出47图schema与每图最新frame/run、显示点数/完整行数、处方消费及收益证据。

原方法锚点：ReportDataR27.load(SQLiteDatabase db) L20–79；ReportDataR27.chaptersR30(SQLiteDatabase db) L86–86

基线SHA256：`3ed5d7386cd13fb0c1ba22df214c5a3cbb9f9812bda00cb0a1ac0c3467182e9d`。共享合并顺序：FR-B01-05 → FR-B07-03 → FR-B09-05。

### `construction/r30-repair/CHART_ACCEPTANCE_47.json`

动作：**ADD**。施工范围：完整新增文件。

问题：截图通过不等算法有效。

修改方式：逐图4状态+来源数据+动作消费+对照结果，保存真实Android截图索引。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B09-05。

## 实施步骤

1. 运行47×空/不足/有效分析/读错四状态；完整机读分析不限于展示120点。
2. 每图至少能指向真实计算产物与消费者；未有效可通过展示工程，但不能关闭质量验收。

## 测试与反证

- 图/报告/DB同快照分母一致；重复图不重复计样本；缺分析禁止演示线替代。

## 验收

用户能逐图看明白是否有用以及为什么，工程和收益两种结论分开。

## 恢复与回滚

保留旧可读图和所有原分析结果。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
