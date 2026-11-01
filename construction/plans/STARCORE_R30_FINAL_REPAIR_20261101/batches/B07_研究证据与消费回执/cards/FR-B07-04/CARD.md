# FR-B07-04 研究任务分片、进度和恢复

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-19。

前驱：B04, B06。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`

动作：**MODIFY**。施工范围：新增ResearchStudyTask及注册点。

问题：新分析不能阻塞实时链或无限重算整段。

修改方式：每片一个准备单元/组件，缓存计算状态与源hash；按typed progress恢复。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`ce5740d0d39e3e7f1ec6824614348713ee258535a92b8a46a600e4a423c71d2b`。共享合并顺序：FR-B02-05 → FR-B03-04 → FR-B04-05 → FR-B06-02 → FR-B07-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`

动作：**MODIFY**。施工范围：register/next。

问题：新增任务不得双Owner或独占worker。

修改方式：仅在schemaReady后注册一次，使用原研究线程池和writer，不创建第二自旋调度。

原方法锚点：ResearchPoolR27.register(Task t) L69–69；ResearchPoolR27.next() L123–134

基线SHA256：`e80ba04df767f5130b537c3aa70fa5f50a9c9f4cc1b5bd309ca13815b0a85f05`。共享合并顺序：FR-B03-03 → FR-B03-04 → FR-B03-05 → FR-B07-04 → FR-B08-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`

动作：**EXTEND_NEW_FILE**。施工范围：研究任务cursor接口。

问题：处方分析失败后需从最后提交点恢复。

修改方式：增加run工作游标和输入pin；提交cursor与产物同事务。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B03-03 → FR-B07-02 → FR-B07-04 → FR-B07-05 → FR-B08-05。

## 实施步骤

1. ReplayEvaluator当前selectTrace是整段API；大窗口必须做有界runner/缓存状态，不在每片重放整个历史。
2. 向existing研究分片预算接入，预算超限返回prepared RETRY而非while占住线程。

## 测试与反证

- 在任意组件/样本分片杀进程后与不中断产物一致；不得重复消耗确认标签。

## 验收

分析持续有进展，旧训练/预测不被饿死，内存与每片耗时可控。

## 恢复与回滚

撤新研究任务注册，已提交run/处方保留。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
