# FR-B00-04 冻结研究角色、单位和资格合同

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-11/FIX-17。

前驱：基线准备。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `construction/r30-repair/CONTRACTS.json`

动作：**ADD**。施工范围：完整新增文件。

问题：研究/确认/Future混用，工作游标被当日历期。

修改方式：固定角色、概率目标、单位、窗口、alpha连续性、热表与受保护引用合同。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B00-04。

### `construction/r30-repair/GATE_AUTHORITY.json`

动作：**ADD**。施工范围：完整新增文件。

问题：GateV2R29.ENABLED=false但报告可能把它当真实授权门禁。

修改方式：列出当前R27_GATE_V1真实签证链与R29_GATE_V2旁路评估，定义新合同启用的先决证据。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B00-04。

## 实施步骤

1. 冻结TRAIN/SELECT/ONLINE_RESEARCH/CONFIRM/FUTURE/SYNTHETIC语义；确认标签只展示，不回流共享优化。
2. 保留单位置固定K3；研究可多位置围号但最终动作不得改成多位置REAL。
3. 新资格合同若启用，只作用于重新登记的新实验；旧证据、旧alpha与旧证书合同不重写。

## 测试与反证

- 跨角色读写反证；同一policy、gateVersion、selector、sourceABI逐键一致。

## 验收

合同指纹和样本单位可复核；工程、研究与自然质量三个验收层独立。

## 恢复与回滚

维持原Gate继续保护，关闭新合同路由而非关闭研究。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
