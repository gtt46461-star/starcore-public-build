# FR-B00-02 逐文件动作与跨文件所有权登记

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-22。

前驱：基线准备。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `construction/r30-repair/FILE_OWNERS.json`

动作：**ADD**。施工范围：完整新增文件。

问题：共享Runtime/Schema/报告文件有并发覆盖风险。

修改方式：每个文件登记卡、方法范围、前驱、schemaOwner、读写Owner及验证出口。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B00-02。

### `construction/r30-repair/METHOD_CHANGE_MAP.json`

动作：**ADD**。施工范围：完整新增文件。

问题：泛称改引擎不能准确施工。

修改方式：对齐包内每卡file_action，带原方法行号和blob SHA；新增方法明确ADD。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B00-02。

## 实施步骤

1. 全1201Java文件已作语法/调用扫描；对关键链做语义审查并登记，不将静态未告警当成无缺陷。
2. RuntimeR27、SchemaR27、EvolutionLoopR27由每批一个集成Owner串行修改；其余无共享文件的叶子实现才可并行。

## 测试与反证

- 所有MODIFY目标存在；所有方法锚点可定位；重复文件写者在同批有唯一合并顺序。

## 验收

60卡所有源文件与方法可定位，新增文件不伪装成已有实现。

## 恢复与回滚

恢复本卡登记元数据，不覆盖其他卡源码。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
