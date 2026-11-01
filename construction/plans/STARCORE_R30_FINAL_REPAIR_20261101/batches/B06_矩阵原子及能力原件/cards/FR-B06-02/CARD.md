# FR-B06-02 恢复矩阵唯一schema与真实生产入口

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-12。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/ResearchEvolutionStoreR930.java`

动作：**MODIFY**。施工范围：ensureSchema/putSnapshot。

问题：源代码有建表/存储，但全树未找到外部直接静态生产调用。

修改方式：补明确的schemaOwner及生产调度入口，不在报告中补空表；putSnapshot通过现有writer。

原方法锚点：ResearchEvolutionStoreR930.ensureSchema(final DbStore store) L41–57；ResearchEvolutionStoreR930.putSnapshot(final DbStore store,final MatrixSnapshotFeedbackR930.CommonSnapshot snapshot,final long nowMs) L211–264

基线SHA256：`078a9b99c445a6e414b9e1e7358556ab0b3dbe42195e4268422ed207b00f45db`。共享合并顺序：FR-B06-02。

### `app/src/main/java/com/ethk3/novaonepro/MatrixSnapshotFeedbackR930.java`

动作：**MODIFY**。施工范围：CommonSnapshot/FrozenMatrix/CaptureRecord。

问题：19类型张量不能用一种统计数组换名生成。

修改方式：每kind维度/输入/PIT/hash/可用性合同；无合法来源保持BLOCKED。

原方法锚点：MatrixSnapshotFeedbackR930$FrozenMatrix.<init>(MatrixKind kind,String targetIssue,String maxSourceIssue,long frozenAtMs,String inputStage,String graphHash,String inputHash,String modelHash,List<MatrixCell> cells) L108–145；MatrixSnapshotFeedbackR930$FrozenMatrix.cell(int row,int column) L147–147；MatrixSnapshotFeedbackR930$FrozenMatrix.valuesWithMissingAsZero() L149–155；MatrixSnapshotFeedbackR930$FrozenMatrix.presentCells() L157–163；MatrixSnapshotFeedbackR930$CaptureRecord.<init>(CaptureStage stage,String targetIssue,String maxSourceIssue,long frozenAtMs,String freezeHash,String inputHash,int expectedRows,int capturedRows,int missingRows,long costNs,String payloadHash) L180–204；MatrixSnapshotFeedbackR930$CaptureRecord.complete() L206–206；MatrixSnapshotFeedbackR930$CaptureRecord.coverage() L207–207；MatrixSnapshotFeedbackR930$CommonSnapshot.<init>(String targetIssue,String maxSourceIssue,long frozenAtMs,String graphHash,String inputHash,String bundleHash,CaptureBundle capture,List<FrozenMatrix> matrices,Set<MatrixKind> requiredKinds) L270–325；MatrixSnapshotFeedbackR930$CommonSnapshot.require(MatrixKind kind) L327–331

基线SHA256：`a9eb46502a37701aa9f152b9e5614f94b74484537080cfd7b529fc1bcffb81fb`。共享合并顺序：FR-B06-02 → FR-B06-03。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`

动作：**MODIFY**。施工范围：新增MatrixProductionTask及注册点。

问题：旧矩阵无持续生产Owner。

修改方式：按事件类型注册有界生产任务，实时预测不等大矩阵；依赖缺失有具体原因。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`ce5740d0d39e3e7f1ec6824614348713ee258535a92b8a46a600e4a423c71d2b`。共享合并顺序：FR-B02-05 → FR-B03-04 → FR-B04-05 → FR-B06-02 → FR-B07-04。

## 实施步骤

1. 参考20矩阵计算合同清单逐类接原源；动态反射可能不在静态索引中，需运行回执证明实际调用。
2. 输入、计算锁外，快照与cursor短事务提交；不双写原Root与独立库。

## 测试与反证

- 每kind合法/缺依赖/错时点/恢复场景；源revision变化不发布过期张量。

## 验收

每活跃kind有真实生产回执，未启用kind有可定位依赖；不空造20行。

## 恢复与回滚

停新任务，原源/注册/模型引用保持可恢复。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
