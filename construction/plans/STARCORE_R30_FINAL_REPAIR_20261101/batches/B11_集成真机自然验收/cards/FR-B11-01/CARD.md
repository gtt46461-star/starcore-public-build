# FR-B11-01 全源码编译、既有回归与47图Android验证

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-21/FIX-22。

前驱：B05, B09, B10。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/build.gradle`

动作：**MODIFY**。施工范围：构建/校验任务与原身份保护。

问题：主机纯Java测试不等Android/NDK/ABI集成通过。

修改方式：接新回归与lint；原Java17/SDK36/NDK/JNI/签名身份保护不绕过。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`3ec9832e98ff3196b3d8790b2bd13856d255c42c307d7366848549628734bd5c`。共享合并顺序：FR-B11-01。

### `app/src/androidTest/java/com/ethk3/novaonepro/R30ChartsAndroidTest.java`

动作：**MODIFY**。施工范围：47图及工作台生命周期。

问题：原48截图不能证明新增18图可用。

修改方式：保留旧图场景，新工作台三组各4数据状态，截图与frame hash对应。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`23a5c11561337ddff95be0135b65d26bf4beb41b4c47392007e4cc74b24ad605`。共享合并顺序：FR-B11-01。

### `construction/r30-repair/INTEGRATION_RECEIPT.json`

动作：**ADD**。施工范围：完整新增文件。

问题：parse-only容易被误称编译完成。

修改方式：分别记录git apply、全源parse、HOST、Android compile、instrumentation、device、natural结果。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B11-01。

## 实施步骤

1. 所有60卡代码合并后跑完整构建与旧回归，解决跨文件类型/迁移冲突。
2. 不得把本包11补丁直接称已完成60卡；未预写部分必须按对应卡真实实现。

## 测试与反证

- 原17项Android回归、旧故障矩阵、模型/资格/保全测试、新47图与工作台生命周期。

## 验收

实际Android编译及回归有当前commit证据；新增错误0，旧基线明确。

## 恢复与回滚

回退对应未通过卡，保持上一已验证检查点可安装。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
