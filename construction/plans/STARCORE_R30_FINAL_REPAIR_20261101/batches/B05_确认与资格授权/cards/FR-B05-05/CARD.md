# FR-B05-05 唯一REAL、撤销与真实结算隔离

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-17。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java`

动作：**MODIFY**。施工范围：全部真实授权入口。

问题：新增研究/图表不能增加第二REAL路径。

修改方式：只消费当前有效证书、selector、冻结与源引用；所有首阻断和完整阻断导出。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`160d2203723ad2bb12c0165b98524bf400b2566861a0e86a978810fb6e4837d0`。共享合并顺序：FR-B05-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java`

动作：**MODIFY**。施工范围：全部撤销传播。

问题：0撤销记录不等于撤销功能已通过。

修改方式：对依赖失效建受影响图，撤销到Gate拒绝有真实时延。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`8e3c79e8a3b9e8ce3bdb1955069285d1a4a43654ff04a5d3a9ad919f41fd6377`。共享合并顺序：FR-B05-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/DecisionExplanationStoreR30.java`

动作：**MODIFY**。施工范围：全部解释落库。

问题：解释与原Gate允许/拒绝可能脱节。

修改方式：记录原Gate结果与各证据引用，不用解释对象自行授权。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`134f5991db92ae6ec7b4078edd55cad1a27389a28d841ce7e3834a3c09989a3c`。共享合并顺序：FR-B05-05。

### `app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java`

动作：**MODIFY**。施工范围：正式/影子统计边界。

问题：影子不能混入真实命中与资金。

修改方式：按原REAL唯一意向/结算键统计，影子模拟独立。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`38eb92ae71a0e09ee4bb7b3252a5c0ec2b51022e4dc5e5b18d8c0330ac85ecb0`。共享合并顺序：FR-B02-05 → FR-B05-05。

## 实施步骤

1. 先用明确夹具正对照贯通合法路径，再等自然资产达标，不能用夹具代替自然资产。
2. 一目标期单位置固定K3真实意向唯一；撤销不得靠APK回滚替代。

## 测试与反证

- 合法、过期、错ABI、错selector、无冻结、撤销、重复开奖/恢复。

## 验收

合法闭环、非法全拒绝、真实/影子分开、撤销目标≤5s。

## 恢复与回滚

撤销新资产引用，原合法结算不可改写。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
