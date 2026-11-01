# FR-B04-01 神经预处理、梯度、发布水位分列

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-07。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java`

动作：**MODIFY**。施工范围：Samples/work/report。

问题：TRAIN标签掩盖仍在MATERIALIZE；trainedThrough可只是输入边界。

修改方式：增加displayPhase、inputBoundary、weightsTrainedThrough/publicationReady和明确计数scope；不破坏旧codec字段。

原方法锚点：AtomNetSessionR30$Samples.get(int sample) L11–11；AtomNetSessionR30.work(Samples samples) L40–70；AtomNetSessionR30.report() L110–120

基线SHA256：`bea4b2c349b9f2342dbfdcbf3890c25896204c663378536b22efa32d67c5f35a`。共享合并顺序：FR-B04-01 → FR-B04-02 → FR-B04-03。

## 实施步骤

1. 应用B04补丁中的Session hunk；旧trainedThrough保留兼容但标明旧语义。
2. 训练是否进步同时记录数据、梯度、验证、发布四个阶段；不能凭样本准备数显示变强。

## 测试与反证

- 初始缓存→首梯度→首epoch→ONLINE全过程；旧checkpoint恢复不失位置游标。

## 验收

梯度0时不得显示权重训练完成；每期10位置的独立样本与重复访问分开。

## 恢复与回滚

恢复旧显示适配器和兼容codec，保留新字段版本。

## 关键代码

顺序补丁：`patches/B04_neural_prefetch_and_truth.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
