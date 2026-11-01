# FR-B01-04 统一44指标与12章事实对象

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-01。

前驱：B00。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricDefinitionsR29.java`

动作：**MODIFY**。施工范围：全部定义与版本适配。

问题：旧报告仍definitionVersion=R29.1和旧卡号。

修改方式：新增R30元数据适配层，旧原件只读保留，当前注册表唯一。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`167476e62079c1b1d2e04fb7a0014e64b60c0e8882d4b78cb6c75a2121f10d27`。共享合并顺序：FR-B01-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricEvaluatorR29.java`

动作：**MODIFY**。施工范围：全部评估方法。

问题：42项缺数与已有实测事实脱节。

修改方式：所有测量读取同一个捕获对象，缺失/失败/未部署分开，不用0替代null。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`af05db2d08e999cec9212a0e8fea6a1665bacf106be4508a657645516dc030ca`。共享合并顺序：FR-B01-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR29.java`

动作：**MODIFY**。施工范围：全部章节投影。

问题：12章结构齐全但未逐功能解释。

修改方式：增加同一事实的当前状态、窗口、分母、阻断链和修补卡；旧章节标签明确历史。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`78a535358cd67b6694de10c5550068a0b50a2e5759f795069f864f12be40da7c`。共享合并顺序：FR-B01-04。

## 实施步骤

1. 先构建统一MetricResult字段合同，再改投影，不能各章分别重算同一指标。
2. 把三态当前存活、窗口SLO、自然质量分列；Gate输入和影子摘要不同窗口必须列明。

## 测试与反证

- 同快照44总表/12章/图表相同指标值及分母一致；没有数据保持null。

## 验收

M32冲突为0，所有INCOMPLETE/UNKNOWN有具体取证动作。

## 恢复与回滚

回退投影层，不重写历史评价结果。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
