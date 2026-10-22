# R816 文件级施工清单

总包内的 `SOURCE_ASSETS/` 是完整工程树，包含每个源码、资源、Manifest、Gradle、验证、训练、工具和报告文件的完整内容；本清单只指出本轮与 R816 运行闭环直接相关的变化，不代表总包只含这些文件。

## 新增的 R816 生产文件

- `app/src/main/java/com/ethk3/novaonepro/ProviderTimeCodecR816.java`
  - 无时区供应商日期固定按 UTC 解析。
  - 支持秒/毫秒 epoch、`Z`、RFC-822 offset、ISO offset 和毫秒/纳秒文本。
  - 提供只修元数据且以 `first_seen_at` 为锚的安全 open-time 选择器。
- `app/src/main/java/com/ethk3/novaonepro/RuntimeReleaseEpochR816.java`
  - 创建并读取 R816 耐久健康统计边界。
  - 只隔离 release 验收窗口，不删除 lifetime 故障事实。
- `app/src/main/java/com/ethk3/novaonepro/ReleaseIdentityR816.java`
  - 唯一 R816 发布身份、版本、DB、玩法和 SHADOW 红线。

## 现有生产文件的 R816 关键修改

- `ApiParser.java`、`BinanceGameContract.java`
  - 删除分散的本地时区解析路径，统一委托 `ProviderTimeCodecR816.parse`。
- `DbStore.java`
  - 在 DB 初始化时建立 R816 epoch。
  - 对“相同期号 + 相同十位号码”执行有约束的 metadata-only 自愈。
  - 保留业务身份与 first-write/first-seen 语义。
  - 以一个有界 cursor 批量读取 UI state。
  - 汇总独立 SHADOW predicted/settled/hit/miss 统计。
- `HistoryIntegrityVerifierR815.java`
  - 新增从最新期向前计算精确连续尾段的方法。
- `V51RuntimeInputR36.java`
  - 先执行冲突/base/连续尾段检查，再做重量计算和对象转换。
  - 只冻结最近连续合法窗口；不足 12 期明确失败。
- `R36AnalysisHistoryPolicyR364.java`、`AutoFetchService.java`
  - 分析准备与冻结使用同一连续尾段规则。
  - 3/5/10/60 秒拆分分析、结算、生产维护和研究节拍。
  - 每轮同时排空 live current settlement 与 historical backlog。
  - 生产闭环完成且无实时压力时才运行 research sidecar。
  - OOM 继续作为 OOM 记账，不被普通失败恢复状态掩盖。
- `PredictionOutputContractR815.java`
  - 用定长布尔数组验证 100/1200 唯一性和 rank，减少每期临时集合分配。
- `SettlementTaskStoreR36.java`
  - 历史任务播种不再由可修复的 provider open time 提前否决 PIT。
- `SettlementBacklogAuditR3611.java`
  - 终态优先；PIT 分类使用不可变 first-seen。
- `SettlementCoordinatorR815.java`
  - 增加有界 `drainCurrent`；唯一可执行结算 owner 不变。
- `DeferredEvidenceWriterR36.java`、`AppConfig.java`
  - 保留精确 1200 动作；每块 300、完整四块、每 slice 最多六次写入。
- `RuntimeHealthLedgerR36.java`、`ModelEffectivenessAnalyzerR35.java`
  - current-release P95/OOM gate 只读取 R816 epoch 后事件。
  - lifetime 指标继续单独输出，READ_ERROR 不转成 PASS。
- `Models.java`、`MainActivity.java`
  - 新增独立 SHADOW 结算统计字段与界面文案。
  - 正常 UI 刷新只执行一次批量 app_state cursor。
- `ReleaseIdentityR815.java`
  - 保留源码/二进制兼容 facade，所有值委托唯一 `ReleaseIdentityR816`。
- `BuildIdentity.java`、`V51BuildManifest.java`、`V51AutoFetchBridge.java`、`LiveDiagnosticReportEngine.java`、`LiveResearchDiagnosisOrchestrator.java`、`V57CompleteDiagnosticReportEngine.java`
  - 统一显示和校验 R816 身份；安全边界不变。
- `app/build.gradle`、`release/release_identity.properties`、`app/src/main/res/values/strings.xml`
  - 版本更新为 `V57.15.3-R3.6.13-816` / `577190825`，release API key 门禁与 SHADOW 文案同步。

## R815 完整基础施工仍保留

以下不是空壳兼容件，而是 R815 已实现并由 R816 继续使用的完整生产模块：

- `BuildProvenanceR815.java`
- `CriticalPathErrorLedgerR815.java`
- `CrossPositionPrimitiveCacheR815.java`
- `DatasetIdentityGateR815.java`
- `DiagnosticSnapshotR815.java`
- `InternalWatchdogReceiverR815.java`
- `IssueFreshnessResolverR815.java`
- `LongTextStoreR815.java`
- `PredictionAvailabilityR815.java`
- `ProductionPredictionDispatcherR815.java`
- `RecoveryCoordinatorR815.java`
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
- `ShadowObservationSidecarR815.java`
- `SourceManifestR815.java`

## 新增的完整验证和发布入口

- `verification/r816/R816RuntimeClosureHarness.java`
  - 14 个确定性断言，覆盖时区、元数据自愈、历史尾段、fail-fast、四块证据和 SHADOW/REAL 锁。
- `verification/r816/R816CorePerformanceHarness.java`
  - 在 96/240 期两档执行真实 100-number/1200-action 生产核心，检查有限性、唯一性和 rank 合同。
- `scripts/r816_generate_provenance.sh`
  - R816 溯源入口，完整调用兼容实现，不是空脚本。
- `scripts/r816_behavior_gate.sh`
  - R816 全量静态、官方 Android 编译和主机回归入口。
- `scripts/r816_build_release.sh`
  - R816 Java17/D8/AAPT2/zipalign/签名 release 构建入口。
- `release/R816_COMPLETION_REPORT.md`
- `release/R816_CHANGED_FILES.md`
- `release/R816_ROOT_CAUSE_FIX_MATRIX.md`
- `release/R816_PHYSICAL_DEVICE_RETEST.md`

## 明确未做的危险修改

- 未改 package、数据库文件名、游戏、固定 K3、单位置或 max-one-REAL-per-issue。
- 未删除 100/1200 正式宇宙，未加入 K4、dynamic K、追单、倍投、martingale 或 frequency debt。
- 未添加第二个预测、REAL、结算、OOS 或 deferred owner。
- 未在开奖后调用模型生成缺失的历史预测。
- 未把旧 OOM/P95/失败账本从数据库删除。
- 未写入私钥、签名密码或真实 API key。
- 未把主机构建结果伪装成物理机 24 小时或模型有效性证明。
