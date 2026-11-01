# FR-B05-02 确认原件、曝光预算与唯一注册

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-10/FIX-11。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationR30.java`

动作：**MODIFY**。施工范围：evaluate/read。

问题：同块重复查询或语义改变不能重复授证。

修改方式：保持不可变包、原控制与readout；身份不符拒绝；结果仅由外部确认器产生。

原方法锚点：FrozenConfirmationR30.evaluate(Task task,EvalContextR27 ctx,HoldoutLedgerR27 ledger,Evidence evidence) L98–127；FrozenConfirmationR30.read(Task task,Policy policy,EvalContextR27 ctx,HoldoutLedgerR27 ledger,Evidence evidence) L129–145

基线SHA256：`b54600ee0d4e9974befa4eef61451f54c7185524462dbe284940fa576c107c27`。共享合并顺序：FR-B05-02。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationStoreR30.java`

动作：**MODIFY**。施工范围：freeze/complete/nextRegistration/register/generationReport。

问题：正结果注册/代际引用必须幂等，旧结果可能复用错代。

修改方式：确认完成/正结果注册各一步，原task_key唯一；旧代结果仅标历史，不计新改善。

原方法锚点：FrozenConfirmationStoreR30.freeze(ResearchWriteQueueR27.Batch batch,FrozenConfirmationR30.Task task,ReplayDatasetR27 data) L28–35；FrozenConfirmationStoreR30.complete(ResearchWriteQueueR27.Batch batch,FrozenConfirmationR30.Task task,FrozenConfirmationR30.Result result) L120–129；FrozenConfirmationStoreR30.nextRegistration(SQLiteDatabase ro) L130–134；FrozenConfirmationStoreR30.register(FrozenConfirmationR30.Task task,String abi,int cap) L135–148；FrozenConfirmationStoreR30.generationReport(ResearchWriteQueueR27.Batch batch,SQLiteDatabase ro,int generation,FrozenConfirmationR30.Task task,FrozenConfirmationR30.Result result) L151–176

基线SHA256：`6484b21c77c1f7872e0cc87ca5d8d56abb3150fe20e3abba9fdb2a796cdff4f5`。共享合并顺序：FR-B05-02。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ConfirmReadStoreR30.java`

动作：**MODIFY**。施工范围：全部确认预留/恢复方法。

问题：预留后死亡不等于允许再看。

修改方式：预留/结果恢复有明确缺失终态，未保留原outcome不能重看。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`dfe1f66ccb37d069629e363490b9064fe9b70a61f18346f06cf7cad1f98e3a26`。共享合并顺序：FR-B05-02。

## 实施步骤

1. 确认与进化选择完全隔离；不让图表筛选结果回流H_CONFIRM。
2. 新选择器组合必须新确认，父证书不能继承。

## 测试与反证

- 预留后杀进程、结果落库前后杀进程、错dataset/seed/generation、重复登记。

## 验收

曝光不重复、有效正结果可登记、负结果完整保留。

## 恢复与回滚

冻结新晋升，保留原confirm ledger与原包。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
