# R815 文件级施工清单

本清单以 `work/target` 基线与最终 R815 工作树的文件级比较生成；总包内保留每个文件的完整内容，不提供占位实现。

## 现有文件已修改

- `app/build.gradle` — 从 release identity 读取全部发布字段，并对 release API key 做硬失败门禁。
- `app/src/main/AndroidManifest.xml` — 新增非导出内部看门狗接收器。
- `app/src/main/res/values/strings.xml` — R815 页面、诊断、SHADOW 边界文案。
- `app/src/main/java/com/ethk3/novaonepro/AppConfig.java` — R815 数据库、历史、SHADOW/模型/REAL 硬红线。
- `AutoFetchService.java`、`BootReceiver.java`、`BusinessReconcilerR35.java`、`BusinessReconcilerR36.java`、`DiagnosticObservationEngine.java` — 改为通过结算协调器和恢复世代执行。
- `BuildIdentity.java`、`V51BuildManifest.java`、`V57CompleteDiagnosticReportEngine.java`、`LiveDiagnosticReportEngine.java`、`LiveResearchDiagnosisOrchestrator.java`、`MainActivity.java` — R815 身份与可见报告版本一致。
- `DataGuard.java`、`DrawCanonicalizerR811.java`、`DrawHashR36.java`、`DbStore.java`、`DbHelper.java`、`DatabaseCoordinator.java`、`OosAfterDrawWriterR36.java` — 业务期号、V2 业务哈希、first-seen、DB36 R815 迁移与 OOS 边界。
- `DiagnosisActivity.java`、`StarcoreApplication.java` — 长报告存储、诊断快照、运行时溯源。
- `SettlementEngine.java`、`SettlementTaskStoreR36.java`、`V51Repository.java`、`V51AutoFetchBridge.java`、`V51RuntimeOrchestrator.java` — 唯一预测入口、唯一可执行结算协调入口、1200 动作正式输出门禁。

## 新增完整生产模块

- `BuildProvenanceR815.java`
- `CriticalPathErrorLedgerR815.java`
- `CrossPositionPrimitiveCacheR815.java`
- `DatasetIdentityGateR815.java`
- `DiagnosticSnapshotR815.java`
- `HistoryIntegrityVerifierR815.java`
- `InternalWatchdogReceiverR815.java`
- `IssueFreshnessResolverR815.java`
- `LongTextStoreR815.java`
- `PredictionAvailabilityR815.java`
- `PredictionOutputContractR815.java`
- `ProductionPredictionDispatcherR815.java`
- `RecoveryCoordinatorR815.java`
- `ReleaseIdentityR815.java`
- `ResearchAblationR815.java`
- `ResearchCandidateExecutorRegistryR815.java`
- `ResearchFailureAttributionR815.java`
- `ResearchFeatureMaterializerR815.java`
- `ResearchFeatureSchemaR815.java`
- `ResearchFeatureVectorR815.java`
- `ResearchPromotionVerifierR815.java`
- `ResearchRepositoryR815.java`
- `ResearchSchedulerR815.java`
- `RuntimeOwnerAuditR815.java`
- `SchemaUpgradeCoordinatorR815.java`
- `SettlementCoordinatorR815.java`
- `ShadowObservationSidecarR815.java`
- `SourceManifestR815.java`

## 新增或扩展的验证支撑

- `verification/r815/R815HostContractHarness.java` — 131 条 R815 断言，覆盖 V2 哈希、跨日滚动、12 期历史、冲突、100/1013 primitive、96 特征、数据集绑定、SHADOW 与期级消融。
- `verification/r811/R811HostContractHarness.java` — 身份断言更新到 R815。
- `verification/r36-stubs/android/content/Intent.java`、`ComponentName.java` — 补齐显式 component/package 主机替身行为。
- `verification/r36-stubs/android/content/pm/ApplicationInfo.java`、`PackageInfo.java`、`PackageManager.java`、`Signature.java`、`SigningInfo.java` — 补齐安装产物指纹校验所需 API 36 主机替身。
- `verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java` — R815 的无密钥构建替身。

## 新增发布与自检文件

- `release/release_identity.properties`
- `release/R815_COMPLETION_REPORT.md`
- `release/R815_CHANGED_FILES.md`
- `scripts/r815_generate_provenance.sh`
- `scripts/r815_behavior_gate.sh`
- `scripts/r815_build_release.sh`

交付时另外生成 `BUILD_EVIDENCE/RELEASE_MANIFEST.json`、APK SHA-256、官方 API 36 编译日志、主机契约日志、D8、签名、zipalign 和 APK 结构校验日志。它们是构建产物，不是占位文件。
