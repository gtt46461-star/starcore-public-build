# R32.1 方案独立审查与代码交付核定

审查源：施工分支 `7424eeae6354eb6599dd8498e123c059bd410c90` 中 `construction/repairs/STARCORE_R32_REPAIR_20261115/`。此报告审查旧 R32.1 的交付边界，不把旧包局限转述为当前 95 个功能未实现。

## 核定结论

用户关于“没有按每批、每问题、每文件把完整代码交付”的指责成立。旧包明确交付五份候选局部补丁，`patches/README.md` 逐批列了施工者仍需完成的工作；不是完整后态代码包。其 112 个唯一声明写路径中 17 个有候选差分，其余 95 个需从现有工程读取原代码并继续判断。无差分不能证明源码没有该功能，部分历史修复已经存在。

|批次|声明写路径|含候选差分|无候选差分|
|---|---:|---:|---:|
|P1|25|7|18|
|P2|36|3|33|
|P3|18|3|15|
|P4|18|3|15|
|P5|15|1|14|

## 已有实现不能误判为缺失

已直接复核以下 032 基线源码：

- `DbStore.java:336` 的 `ensureRuntimeExtraTablesOnce` 仅调用只读 `requireRuntimeReadyReadOnlyR31`，不能再沿用旧卡断言它会在业务入口偷偷建表。
- `DbStore.java:738` 的 `execMigrationDdlOnlyR31` 检查 `LegacyDdlR31.owned(db)`；非迁移所有者只读检查。`ensureLedgerIndexes` 在约 2027 行也检查同一 owner。
- `LegacySchemaCoordinatorR926.java:46` 已对旧 COMPLETED 单元重新审计，捕获 `LegacyDdlR31.Pending` 后 `invalidateStepR31`。其他异常是否也正确重开仍需具体负例，不应抹去现有实现。

因此新包应明确分成：本轮实际新增/修改代码、已有源码保全、仍待行为证明的设计余项。把完整源码打包是交付改进，不自动等于这些余项已全部实现。

## 批次冲突与闭环

- 旧账本声明的单文件写入 owner 没有跨批重复；`verify_plan.py` 和 `verify_continuation.py` 本次均实际通过结构核验。
- 这些脚本读取手填 prerequisites、reads/writes、验收消费者与字段非空。`verify_continuation.py` 的 `productionCases=18` 意味着 18 份用例描述与方法锚点映射通过，并不意味着 18 个生产入口用例实际执行通过。
- 原 R31.2 先要求全量绿化再做业务，R31.3 改为批末棘轮，R31.4 合并为 11 批。R32.1 已重新分配 34 条验收到最终真实消费者，结构上没有发现后向签收引用；业务闭环仍须由本批真实入口测试证明。
- P1 的冻结/结算/训练事件应只签到耐久事件与游标；P2 签到实际优化器更新与发布/消费；P3 签到证书和唯一正式出手。不能以 P1 写了事件就签 P2 训练，也不能等待 P3 自然证书才关 P1。
- P1 的缺件恢复和无引用副本删除必须保全旧证书/未结算/在研窗口/导出租约，不需要等待 P3 才能恢复已知原件；若要修改权威引用格式或删除旧根，必须把所有读写者随同变更放进同一个责任批。

## 可核查的交付缺口和修正

1. **完整代码缺口。** 对每个涉及路径提供唯一完整后态源码、基线/后态 SHA-256、精确差分和明确动作。保全文件注明 `PRESERVE_REFERENCE`。禁止为了制造 diff 改动已正确实现。
2. **逐卡文件漏项。** 新打包器原先只取 writes，会漏 `EvidenceBlockR26.java` 与 `ResearchTableAdaptersR30R1.java` 等只读依赖；现已要求纳入每卡 files 与 traceOnly，并在缺文档/源码时失败，不能 silent continue。
3. **“已核验”范围。** 旧 RESULTS 写明宿主类型编译、66 项断言、SQL/配额局部检查；Gradle APK、Android runtime 未运行，自然质量未证明。应保留这些原字，不写成完整 APK/真机通过。
4. **逐文件保全证明。** 无差分文件须由具体源码锚点和业务回执判断已实现/余项，不能把旧 diagnosis 全部复制当当前事实。
5. **逐批应用与防漂移。** 所有文件先检查当前 SHA 再写；出现与前态/后态均不同的用户改动时拒绝覆盖。应用回执只证明字节，不得签业务。
6. **可重放的包自检。** 从完整基线 ZIP 在临时目录逐批应用 diff，再逆向恢复；独立通过实际应用工具复演并校验幂等。所有输出字节必须与完整候选 ZIP 一致。
7. **高胜率等质量。** 无自然未来样本时给原始分母与未证明状态。工程替换代码不提供未来必胜保证；达标资格进入唯一现有 DecisionGate，SHADOW 和 REAL 分账。

## 本次实际执行

- `python3 verify/verify_plan.py`：PASS，5 个结构反例拒绝；未带 `--repo`，不冒领补丁回环执行。
- `python3 verify/verify_continuation.py`：PASS，222 条映射、18 份用例描述、34 条归属迁移、4 个元数据反例拒绝。
- `python3 package_work/test_apply_batch.py`：9 项 PASS，包含只读检查、顺序、损坏、源漂移、路径/软链接逃逸、中断后续写与故障回滚。
- `strict_audit.py`：已生成 `CODE_COVERAGE_AUDIT.json`，范围仅旧 R32.1 交付，未把无 diff 判作源码未实现。
- 新包 `verify_code_package.py`：验证范围为完整代码、文档、哈希、依赖、差分正逆应用、实际应用工具幂等，不授予 Android 或自然质量通过。待新包生成后执行。

## 112 个声明写路径逐项交付状态

以下只说明旧 R32.1 是否含差分；每行 cards 已映射到原问题，供新包核对全文件交付。

|批次|问题卡|文件|旧包代码交付|
|---|---|---|---|
|P5|P5.01|`.github/workflows/r31-current-live.yml`|既有源码保全，本轮余项未核定|
|P5|P5.01|`.github/workflows/r31-publish-release.yml`|既有源码保全，本轮余项未核定|
|P5|P5.01|`.github/workflows/r31-report-publish.yml`|既有源码保全，本轮余项未核定|
|P5|P5.01|`.github/workflows/r31-verification.yml`|既有源码保全，本轮余项未核定|
|P5|P5.01|`app/build.gradle`|既有源码保全，本轮余项未核定|
|P1|P1.03,P1.04,P1.05|`app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`|有候选局部差分，余项需核定|
|P1|P1.05|`app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java`|既有源码保全，本轮余项未核定|
|P3|P3.02|`app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java`|既有源码保全，本轮余项未核定|
|P4|P4.02|`app/src/main/java/com/ethk3/novaonepro/ChartRegistryR31.java`|既有源码保全，本轮余项未核定|
|P4|P4.02|`app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java`|既有源码保全，本轮余项未核定|
|P4|P4.02|`app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR83.java`|既有源码保全，本轮余项未核定|
|P1|P1.03,P1.04|`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`|有候选局部差分，余项需核定|
|P1|P1.01|`app/src/main/java/com/ethk3/novaonepro/DbHelper.java`|既有源码保全，本轮余项未核定|
|P1|P1.01,P1.03|`app/src/main/java/com/ethk3/novaonepro/DbStore.java`|有候选局部差分，余项需核定|
|P3|P3.02|`app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java`|既有源码保全，本轮余项未核定|
|P1|P1.03|`app/src/main/java/com/ethk3/novaonepro/HistoryIntegrityVerifierR815.java`|既有源码保全，本轮余项未核定|
|P1|P1.01|`app/src/main/java/com/ethk3/novaonepro/LegacyDdlR31.java`|既有源码保全，本轮余项未核定|
|P1|P1.01|`app/src/main/java/com/ethk3/novaonepro/LegacySchemaCoordinatorR926.java`|既有源码保全，本轮余项未核定|
|P1|P1.04|`app/src/main/java/com/ethk3/novaonepro/LifecycleHealthR963.java`|既有源码保全，本轮余项未核定|
|P1|P1.02|`app/src/main/java/com/ethk3/novaonepro/LifecycleSchemaR31.java`|有候选局部差分，余项需核定|
|P2|P2.05|`app/src/main/java/com/ethk3/novaonepro/MatrixSnapshotFeedbackR930.java`|既有源码保全，本轮余项未核定|
|P1|P1.03|`app/src/main/java/com/ethk3/novaonepro/R36AnalysisHistoryPolicyR364.java`|有候选局部差分，余项需核定|
|P1|P1.04|`app/src/main/java/com/ethk3/novaonepro/RealtimePressureGateR811.java`|既有源码保全，本轮余项未核定|
|P4|P4.03|`app/src/main/java/com/ethk3/novaonepro/ReportArchiveVerifierCP12.java`|既有源码保全，本轮余项未核定|
|P4|P4.03|`app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java`|既有源码保全，本轮余项未核定|
|P1|P1.06|`app/src/main/java/com/ethk3/novaonepro/ReportCleanupR31.java`|既有源码保全，本轮余项未核定|
|P4|P4.01,P4.03|`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`|有候选局部差分，余项需核定|
|P4|P4.03|`app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java`|既有源码保全，本轮余项未核定|
|P4|P4.03|`app/src/main/java/com/ethk3/novaonepro/ReportJsonWriterR31.java`|既有源码保全，本轮余项未核定|
|P4|P4.03|`app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java`|既有源码保全，本轮余项未核定|
|P4|P4.01|`app/src/main/java/com/ethk3/novaonepro/ReportSemanticVerifierR31.java`|既有源码保全，本轮余项未核定|
|P2|P2.05|`app/src/main/java/com/ethk3/novaonepro/ResearchEvolutionStoreR930.java`|既有源码保全，本轮余项未核定|
|P4|P4.02|`app/src/main/java/com/ethk3/novaonepro/ResearchTablesActivityR31.java`|既有源码保全，本轮余项未核定|
|P1|P1.01|`app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java`|既有源码保全，本轮余项未核定|
|P1|P1.05|`app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java`|既有源码保全，本轮余项未核定|
|P1|P1.05|`app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java`|既有源码保全，本轮余项未核定|
|P1|P1.06|`app/src/main/java/com/ethk3/novaonepro/StorageBudgetR31.java`|既有源码保全，本轮余项未核定|
|P1|P1.03|`app/src/main/java/com/ethk3/novaonepro/TargetIssueResolver.java`|既有源码保全，本轮余项未核定|
|P3|P3.02|`app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java`|有候选局部差分，余项需核定|
|P2|P2.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetPublicationR31.java`|既有源码保全，本轮余项未核定|
|P2|P2.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java`|既有源码保全，本轮余项未核定|
|P2|P2.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java`|既有源码保全，本轮余项未核定|
|P2|P2.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetTrainerR27.java`|既有源码保全，本轮余项未核定|
|P2|P2.05|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomWorldR27.java`|既有源码保全，本轮余项未核定|
|P2|P2.04|`app/src/main/java/com/ethk3/novaonepro/strategyv2/BirthGateR27.java`|既有源码保全，本轮余项未核定|
|P3|P3.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java`|有候选局部差分，余项需核定|
|P3|P3.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java`|既有源码保全，本轮余项未核定|
|P3|P3.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateSU2.java`|既有源码保全，本轮余项未核定|
|P1|P1.06|`app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointArtifactsR26.java`|既有源码保全，本轮余项未核定|
|P1|P1.06|`app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcStoreR31.java`|既有源码保全，本轮余项未核定|
|P2|P2.06|`app/src/main/java/com/ethk3/novaonepro/strategyv2/CreationBudgetR31.java`|既有源码保全，本轮余项未核定|
|P2|P2.05|`app/src/main/java/com/ethk3/novaonepro/strategyv2/DerivedAtomR27.java`|既有源码保全，本轮余项未核定|
|P4|P4.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java`|既有源码保全，本轮余项未核定|
|P2|P2.06|`app/src/main/java/com/ethk3/novaonepro/strategyv2/EliteArchiveR27.java`|既有源码保全，本轮余项未核定|
|P3|P3.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceContractR31.java`|既有源码保全，本轮余项未核定|
|P2|P2.04,P2.06|`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`|既有源码保全，本轮余项未核定|
|P2|P2.04|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertProposalsR27.java`|既有源码保全，本轮余项未核定|
|P2|P2.06|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FactoryCapabilityExperimentR31.java`|既有源码保全，本轮余项未核定|
|P2|P2.04|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDirectedResearchR26.java`|既有源码保全，本轮余项未核定|
|P2|P2.03,P2.06|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java`|有候选局部差分，余项需核定|
|P3|P3.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/GateV2R29.java`|既有源码保全，本轮余项未核定|
|P1|P1.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/HypothesisStoreR31.java`|有候选局部差分，余项需核定|
|P2|P2.04|`app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaFamilyR27.java`|既有源码保全，本轮余项未核定|
|P2|P2.06|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ImprovementStoreR31.java`|既有源码保全，本轮余项未核定|
|P1|P1.06|`app/src/main/java/com/ethk3/novaonepro/strategyv2/KeepMigrationStoreR31.java`|既有源码保全，本轮余项未核定|
|P3|P3.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java`|既有源码保全，本轮余项未核定|
|P3|P3.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterR31.java`|既有源码保全，本轮余项未核定|
|P3|P3.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineBridgeSU2.java`|有候选局部差分，余项需核定|
|P3|P3.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java`|既有源码保全，本轮余项未核定|
|P3|P3.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineFreezeProofR31.java`|既有源码保全，本轮余项未核定|
|P3|P3.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineModelBindingR31.java`|既有源码保全，本轮余项未核定|
|P2|P2.05|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryR30.java`|既有源码保全，本轮余项未核定|
|P4|P4.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryStoreR30.java`|既有源码保全，本轮余项未核定|
|P2|P2.05|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java`|既有源码保全，本轮余项未核定|
|P1|P1.06|`app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java`|既有源码保全，本轮余项未核定|
|P2|P2.06|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelEvolutionSU2.java`|既有源码保全，本轮余项未核定|
|P2|P2.03|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelGenomeSU2.java`|既有源码保全，本轮余项未核定|
|P2|P2.03|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelPublicationR31.java`|既有源码保全，本轮余项未核定|
|P2|P2.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceStoreR30.java`|既有源码保全，本轮余项未核定|
|P2|P2.05|`app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedComponentStudyR30R1.java`|既有源码保全，本轮余项未核定|
|P3|P3.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationGapRepairR30R1.java`|既有源码保全，本轮余项未核定|
|P3|P3.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationInboxR29.java`|既有源码保全，本轮余项未核定|
|P3|P3.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationWindowR26.java`|既有源码保全，本轮余项未核定|
|P2|P2.04|`app/src/main/java/com/ethk3/novaonepro/strategyv2/RegimeHypothesisR31.java`|既有源码保全，本轮余项未核定|
|P4|P4.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/RepairDiagnosisR32.java`|有候选局部差分，余项需核定|
|P2|P2.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayKnowledgeStoreR31.java`|既有源码保全，本轮余项未核定|
|P4|P4.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java`|既有源码保全，本轮余项未核定|
|P4|P4.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR27.java`|既有源码保全，本轮余项未核定|
|P4|P4.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR29.java`|有候选局部差分，余项需核定|
|P4|P4.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`|既有源码保全，本轮余项未核定|
|P2|P2.04|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchHypothesisR31.java`|既有源码保全，本轮余项未核定|
|P2|P2.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java`|既有源码保全，本轮余项未核定|
|P2|P2.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`|既有源码保全，本轮余项未核定|
|P2|P2.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java`|既有源码保全，本轮余项未核定|
|P1|P1.04|`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`|既有源码保全，本轮余项未核定|
|P2|P2.01,P2.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`|有候选局部差分，余项需核定|
|P3|P3.01|`app/src/main/java/com/ethk3/novaonepro/strategyv2/SequentialQualificationR27.java`|既有源码保全，本轮余项未核定|
|P2|P2.04|`app/src/main/java/com/ethk3/novaonepro/strategyv2/SetIntersectionPolicyR30.java`|既有源码保全，本轮余项未核定|
|P2|P2.03,P2.05|`app/src/main/java/com/ethk3/novaonepro/strategyv2/StructuralProposalR26.java`|有候选局部差分，余项需核定|
|P2|P2.02|`app/src/main/java/com/ethk3/novaonepro/strategyv2/TrainingStepStoreR26.java`|既有源码保全，本轮余项未核定|
|P2|P2.03|`app/src/main/java/com/ethk3/novaonepro/strategyv2/WarmCandidateR26.java`|既有源码保全，本轮余项未核定|
|P1|P1.04|`app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`|有候选局部差分，余项需核定|
|P5|P5.01|`release/R31_REPORT_PUBLISH_REQUEST.json`|既有源码保全，本轮余项未核定|
|P5|P5.01|`release/release_identity.properties`|既有源码保全，本轮余项未核定|
|P5|P5.01|`tools/r31/package_release.py`|有候选局部差分，余项需核定|
|P5|P5.01|`tools/r31/publish_report_package.py`|既有源码保全，本轮余项未核定|
|P5|P5.01|`tools/r31/run_current_acceptance.py`|既有源码保全，本轮余项未核定|
|P5|P5.01|`tools/r31/run_emulator_upgrade_matrix.py`|既有源码保全，本轮余项未核定|
|P5|P5.01|`tools/r31/run_final_runtime.sh`|既有源码保全，本轮余项未核定|
|P5|P5.01|`tools/r31/run_phone_report_android.sh`|既有源码保全，本轮余项未核定|
|P5|P5.01|`tools/r31/test_package_release.py`|既有源码保全，本轮余项未核定|
|P5|P5.01|`tools/r31/upgrade_matrix.py`|既有源码保全，本轮余项未核定|
