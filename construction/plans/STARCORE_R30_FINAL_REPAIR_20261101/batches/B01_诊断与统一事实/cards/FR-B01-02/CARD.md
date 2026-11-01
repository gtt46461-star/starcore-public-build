# FR-B01-02 导出诊断失败阶段和错误链

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-02。

前驱：B00。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java`

动作：**MODIFY**。施工范围：report。

问题：当前catch仅输出异常类，无法定位parse/diagnose/render阶段。

修改方式：拆阶段，输出最多12个本产品栈帧与输入hash；不在报告读取时建表或生成诊断。

原方法锚点：DiagnosisStoreSU2.report(SQLiteDatabase db) L77–88

基线SHA256：`6586acdf994f47749c8ec5b6c62a121c188abebe16bf3c5451b32de11b22e407`。共享合并顺序：FR-B01-02。

## 实施步骤

1. 应用report完整替换代码；读取原su2_diag_snapshot并保留不可变原件。
2. 报告JSON添加failureStage/errorChain/snapshotHash，文本显示首个产品栈；不输出凭据或整个模型payload。

## 测试与反证

- 注入读取、解析、诊断、MD渲染、JSON渲染错误；导出其他章节仍可继续。

## 验收

失败可指到具体阶段、文件方法与原输入，不再只有NullPointerException。

## 恢复与回滚

保留只读错误降级；原诊断输入不变。

## 关键代码

顺序补丁：`patches/B01_diagnostic_truth.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
