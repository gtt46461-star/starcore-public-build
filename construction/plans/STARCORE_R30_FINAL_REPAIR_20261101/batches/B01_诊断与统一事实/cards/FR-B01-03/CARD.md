# FR-B01-03 修正诊断指标映射

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-01。

前驱：B00。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisRulesR30.java`

动作：**MODIFY**。施工范围：rules。

问题：WRITER_HARD挂在M11训练新鲜度，实际是写事务治理。

修改方式：映射M07及后台写预算子项；M11只测最老训练积压；统计规则不得与授证绑定。

原方法锚点：DiagnosisRulesR30.rules() L25–124

基线SHA256：`0f4897401e2013603520431092a98bb5a5e5bf2018c7a5d398313f366100766f`。共享合并顺序：FR-B01-03。

## 实施步骤

1. 应用单行映射补丁并逐条对照44指标元数据。
2. 对16条现有规则和新来源错误规则分别报覆盖，不用16条100%表示所有缺陷均覆盖。

## 测试与反证

- 错误M11映射的回归输入应失败；writerHard>0同时定位operation及M07。

## 验收

每条诊断指向正确指标、卡、代码和验证动作。

## 恢复与回滚

恢复诊断投影版本，保留原测量。

## 关键代码

顺序补丁：`patches/B01_diagnostic_truth.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisRulesR30.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
