# R34 issue_data 运行域二次审查

审查输入：`analyses/plan_build/issue_data.py` 当前文件；APK 095 前态 `273641e838e0b8dd842b42a67a674e184942a30a`。范围 B00–B05、B10、B12。只出意见，未修改方案生成数据。

## 必须更正或补齐

|位置（批次内从 1 编号）|发现|具体修正|
|---|---|---|
|B03 / 2|`TaskContinuationR31` 不是 095 现存类或方法。|锚点改为 `strategyv2/TaskContinuationR26.step/compute`；若拟新增类，必须另标 ADD，不能作现存owner。|
|B01 / 1|文件列表没有实际 writer hold 队列owner。`RuntimeFaultEvidenceR31` 存在，但不负责该内存积压队列。|补 `strategyv2/WriterHoldFaultsR31.java`，精确锚点 `deferResearch`、`drainControlSliceR32`、`drainOne`、`drainBatchR33`。095 堵塞的准入在 `DatabaseCoordinator.runWrite`。|
|B01 / 1|“BUSINESS、RECOVERY、MIGRATION三类”是拟议逻辑分类，095 `WriteClass` 实际有 9 类。|保留 9 类的原关键域语义，明确受限恢复映射到 `RESEARCH_CONTROL`，故障耐久化映射到既有 `TELEMETRY`；如要增加 enum，需列所有准入、deadline、quota调用者迁移。不能让任意业务自报 RECOVERY。|
|B01 / 3|`runUpgrade` 未精确指向真实owner；没有列 `SchemaDomainCoordinatorR926`、`ResearchRetentionStoreR30`、`ResearchRetentionR30`。|用 `SchemaDomainCoordinatorR926.runUpgradeR31/migrateR31RetentionProtection`、`strategyv2/ResearchRetentionStoreR30.ensureProtectedObjectsR31`、`RuntimeCoordinatorR26.startupUpgradeSpecR31/initialize`。`DbHelper.ensureSchema` 是另一启动迁移路径，须独立说明，不能只改它就声称修复报告栈。|
|B03 / 1、5|Prepared结果存储之外，durable analysis请求、singleflight和canonical提交owner未列。|补 `CanonicalObservationWriterR36.insertWithinTransaction`、`AnalysisRequestStoreR36.claimDetailed/markRetry/markCanonicalCommittedWithinTransaction/latestRunnable`、`AnalysisSingleFlightR36.submit/releaseAndTakeNext/reapDeadWorker`。复用原 fence/deadline/不可变target，不能另造平行调度器。|
|B04 / 4|独立结算队列的实际owner缺失。|补 `SettlementTaskStoreR36.enqueueConfirmedWithinWrite/claim/markTerminalWithinTransaction/markRetry/recoverOrphanedRunningLeasesOnProcessStart`。`DeferredEvidenceWriterR36` 不能替代该主队列。|
|B04 / 5|“冻结和标签形成反馈”可能使现有标签outbox被错误改为依赖freeze。|补 `SettlementTaskStoreR36.enqueueConfirmedWithinWrite` 与 `strategyv2/RuntimeSchemaR26.feedbackWithinTransaction/reconcileFeedbackR31`。095 在开奖事务直接产生训练标签，无冻结仍应产生；有冻结才形成额外的策略预测误差/盈亏反馈。二者必须分别幂等。|
|B01 / 2|“fatal Error继续上抛”与同时把 OOM 列为普通资源重试存在含混，当前代码确实把 OutOfMemory 识别为 transient。|明确哪些现有异常分类保持、哪些变更需要 Android 验证；不得 catch(Throwable) 后一律吞掉。候选补丁仅修精确 DEPENDENCY/ADMISSION/ROLLED_BACK backlog，不声称已解决所有 Error 分类。|
|B12 / 3、4|`tools/r34/verify_plan.py`、`tools/r34/check_acceptance.py` 不在 095 前态。|明确标 ADD，并区分“本计划包校验工具”与“应用未来构建/真机验收工具”；不能将本次工具通过写成App验收通过。|

## 文件解析约束

本范围其他既有文件 basenames 都能在 095 找到。`DatabaseCoordinator.java`、`DbStore.java`、`RuntimeCoordinatorR26.java` 在工具shim里还有同名文件，`AndroidManifest.xml` 在 main/androidTest均有：生成文件合同必须限定 `app/src/main/`，不能依赖第一个同名搜索结果。`RuntimeFaultEvidenceR31`、`WriterHoldFaultsR31`、`ResearchRetentionStoreR30`、`RuntimeCoordinatorR26`、`MainlineChoiceStoreR31`、`MainlineAdapterR31` 均在 `strategyv2/` 包。

## 不应纠正为错误的内容

- B00 的 `plan_descriptor/verify_plan/verify_manifest` 确在旧工具中存在；`r334PlanRoot/generateR334PlanIdentity/requiredIdentity` 是现有Gradle锚点，虽然名称带 R334，不能据此判定虚构。
- B02 的 `complete/bindRawResultR82/bindParsedResultR82`、`DbStore.saveDraw` 都是现有方法；身份问题需保留原始body，不能“全项目去trim”破坏其他非身份规范化。
- B10 的现有选择机制可接 `MainlineChoiceStoreR31.appendChoice/effectiveChoice/authorizedChoice/revoke` 与 `MainlineAdapterR31.prepare/verifyFrozen`。新增promotion事件应接入原owner，不需要新建第二套真实授权入口。
- B03/B12的“caught与uncaught分开”是正确要求。报告中的 deferred栈经 `CrashRecorder.recordCaught` 记录，单凭该栈不能断言线程死亡。

具体源证据和原报告定位见同目录 `runtime_audit.md`；建议为上述owner逐一补进修改文件合同，保持整体66工单数量不变即可。
