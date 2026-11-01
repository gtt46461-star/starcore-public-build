# FR-B07-03 16逻辑研究表适配现有来源

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-12/FIX-13/FIX-14。

前驱：B04, B06。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `construction/r30-repair/RESEARCH_TABLE_CONTRACTS.json`

动作：**ADD**。施工范围：完整新增文件。

问题：不能新增16套互相矛盾的数据库真相。

修改方式：映射T01–T16到既有特征/预测/回执表和6个新物化表，固定字段/公式/单位/Owner。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B07-03。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportDataR27.java`

动作：**MODIFY**。施工范围：query/load。

问题：当前统计查询与机器研究输入未区分。

修改方式：提供索引化只读视图，限量展示仍能链接完整机读来源。

原方法锚点：ReportDataR27.load(SQLiteDatabase db) L20–79；ReportDataR27.query(String id) L96–115

基线SHA256：`3ed5d7386cd13fb0c1ba22df214c5a3cbb9f9812bda00cb0a1ac0c3467182e9d`。共享合并顺序：FR-B01-05 → FR-B07-03 → FR-B09-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`

动作：**MODIFY**。施工范围：受保护引用与新表保留登记。

问题：分析源被清理后无法复算或破坏未完成任务。

修改方式：把run/proposal/consume依赖加入保护图，未消费与正式证据引用不得删。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`5d3b1a97012b91001b57dde9aba14da807669ffa74db68ffad9d5f1b16c5fa27`。共享合并顺序：FR-B07-03 → FR-B10-02。

## 实施步骤

1. 每个逻辑表有生产者、消费者、更新事件、角色、空值和验收。
2. 不从UI图上抄数字训练；训练器读取原机读向量/概率/误差与hash。

## 测试与反证

- 随机抽取表中一行可追到输入与输出；清理未完成研究源必须被保护。

## 验收

16表均有明确用途和运行责任，统计展示与算法数据同源。

## 恢复与回滚

停新视图，保留原数据源与引用保护。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
