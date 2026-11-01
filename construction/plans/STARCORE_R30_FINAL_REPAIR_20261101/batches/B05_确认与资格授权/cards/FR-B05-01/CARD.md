# FR-B05-01 确认负结果与不足结果准确展示

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-10。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ConfirmationViewR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：DONE/verified0可能是正常未胜对照，不能按登记故障处理。

修改方式：区分未计算、完成但证据不足、正常负结果、已验证待注册、已注册。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B05-01。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`

动作：**MODIFY**。施工范围：rowsets confirmation。

问题：只导出verified看不到真实reason。

修改方式：限量导出result_json及其hash/引用，展示状态/原因与配对下界。

原方法锚点：ReportSnapshotR30.rowsets(Query q) L107–137

基线SHA256：`2dfb38a9bcb05d76cf5b72432dacd6428697d1a152ecf4aef53682deb9010360`。共享合并顺序：FR-B01-05 → FR-B03-02 → FR-B05-01。

## 实施步骤

1. 应用B05补丁；只对verified=1且待登记的任务检查注册推进。
2. 现有两条DONE0先读原result.reason，若负结果如实保留，不改为通过。

## 测试与反证

- 正常负结果、INSUFFICIENT、正结果等待slot、注册完成和旧代重复证据。

## 验收

负结果不会被强改verified，也不会被无故反复重新确认。

## 恢复与回滚

回退展示，不重算消耗过的确认预算。

## 关键代码

顺序补丁：`patches/B05_confirmation_negative_evidence.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/ConfirmationViewR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/strategyv2/ConfirmationViewR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

/** verified=false is not necessarily a malfunction: a completed negative control comparison is valid evidence. */
public final class ConfirmationViewR30R1 {
    private ConfirmationViewR30R1(){}
    public static String state(String taskState,String resultStatus,boolean verified,long registeredAt){
        if(!"DONE".equals(taskState))return "PENDING_COMPUTATION";
        if(!"COMPLETED".equals(resultStatus))return "COMPLETED_WITH_INSUFFICIENT_EVIDENCE";
        if(!verified)return "COMPLETED_NO_VERIFIED_IMPROVEMENT";
        return registeredAt>0?"VERIFIED_AND_REGISTERED":"VERIFIED_WAITING_REGISTRATION";
    }
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
