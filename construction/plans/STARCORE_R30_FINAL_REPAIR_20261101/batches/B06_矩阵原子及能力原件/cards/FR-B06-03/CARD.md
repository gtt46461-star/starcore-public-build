# FR-B06-03 矩阵真实消费与反事实效果验证

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-12/FIX-13。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/MatrixSnapshotFeedbackR930.java`

动作：**MODIFY**。施工范围：settle/FeedbackState。

问题：存储/展示不等算法消费。

修改方式：对教师、误差、跨位、动作效用等建立具体消费与变化记录；只影响合法下一期研究。

原方法锚点：MatrixSnapshotFeedbackR930.settle(CommonSnapshot snapshot,SettlementLabel label,String effectiveFromIssue) L406–452；MatrixSnapshotFeedbackR930$FeedbackState.<init>(String stateId,String trainedUntilIssue,int updates,double[][] fast,double[][] slow) L462–476；MatrixSnapshotFeedbackR930$FeedbackState.initial(String stateId) L478–481；MatrixSnapshotFeedbackR930$FeedbackState.update(FeedbackDelta delta,double fastRate,double slowRate) L483–502；MatrixSnapshotFeedbackR930$FeedbackState.correction(int row,int column,String targetIssue) L504–510

基线SHA256：`a9eb46502a37701aa9f152b9e5614f94b74484537080cfd7b529fc1bcffb81fb`。共享合并顺序：FR-B06-02 → FR-B06-03。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixSnapshotSU2.java`

动作：**MODIFY**。施工范围：capture/support。

问题：因子/教师checkpoint与日历期不同。

修改方式：保留checkpoint序列身份、inputThrough和可用时间，不伪装目标期。

原方法锚点：MatrixSnapshotSU2$Factor.support(AtomStreamSU2 stream,int position,int mask) L16–24；MatrixSnapshotSU2.capture(CommitteeSU2 committee,ModelReplaySU2.Library lib,long through) L39–65；MatrixSnapshotSU2.support(List<Factor> factors,AtomStreamSU2 stream,int position,int mask) L71–71

基线SHA256：`11c7ac43bcf44fe7c4068139438cbc7ddd304ad5f08fc3b33649882ecba762a7`。共享合并顺序：FR-B06-03。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixStoreSU2.java`

动作：**MODIFY**。施工范围：save/maintain/report。

问题：SU2快照0时可能没有可用genome或未运行。

修改方式：逐项解释缺genome/缺训练/缺source；生产→support消费→结算评分可联结。

原方法锚点：MatrixStoreSU2.save(SQLiteDatabase db,String key,long through,MatrixSnapshotSU2.Data data,long now) L13–17；MatrixStoreSU2.maintain(DbStore store,String target) L19–54；MatrixStoreSU2.report(SQLiteDatabase db) L57–73

基线SHA256：`0c54a6e6e0de3ec6f4ec6a2b292bf75731641a2bed7aac4c431d876d7c28fb3a`。共享合并顺序：FR-B06-03。

## 实施步骤

1. 各矩阵输出映射T04/T05/T06/T09等实际研究动作，不能全部只写报告。
2. 因果/反事实矩阵只给有对照的研究证据，相关性不标因果已证明。

## 测试与反证

- 移除各矩阵的配对对照，实际消费者引用和策略变更回执；负结果保留。

## 验收

产出/消费/变化/增益四层齐全或明确未知，未证收益不标effective。

## 恢复与回滚

撤新消费路由，原矩阵数据和旧有效算法保留。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
