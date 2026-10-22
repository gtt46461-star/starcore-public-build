# R915 变更文件施工台账

精确行数、字符数、Git 增删行、SHA-256 和 524 个 Java 的实质性分类见自动生成的 `R915_ALL_CHANGED_FILE_METRICS.csv` 与 `R915_JAVA_FILE_SUBSTANCE_AUDIT.csv`。本表说明每个代码/资源/验证文件为什么改、改成什么；没有用“编译通过”代替功能结果。

| 文件 | 问题/责任 | 本次实质修改 |
| --- | --- | --- |
| `app/build.gradle` | 旧版本身份 | versionName/versionCode 升级到 R915，保持 applicationId 和覆盖安装身份 |
| `AlgorithmEvidencePolicyR912.java` | 证据门和 FDR 口径不足 | N240、paired120、多指标晋级/淘汰与保守多重检验 q 值 |
| `AlgorithmEvolutionLifecycleR912.java` | 生命周期缺完整合同/FDR 持久化 | 每次证据刷新保存 q 值和 16 器官 canonical contract |
| `AlgorithmInventionLabR908.java` | 算法只结算极少样本、观察重复构建 | 最多 8 active；1200 动作观察一次构建共享；子代 N0；Future OOS 冻结 |
| `AppConfig.java` | 全局 SHADOW/REAL 硬锁与发布身份不一致 | 受控候选配置，仍保持模型未证明边界 |
| `AutoFetchService.java` | 退出后业务不推进、预测阻塞结算、研究抢预算 | task removal 重布置；每期开奖任务；当前结算即时重试；P0/Research 独立预算；新资产/算法逐期冻结 |
| `BootReceiver.java` | 服务存在时跳过业务恢复 | 启动服务同时强制业务 reconcile |
| `BuildIdentity.java` | 版本显示旧 | R915 身份统一 |
| `BusinessReconcilerR36.java` | 当前结算与下一期预测耦合 | 分离结算/预测 lane，欠账公平收敛 |
| `CanonicalDecisionViewAdapterR36.java` | UI 不能区分受控 BET 与 HOLD | 输出真实动作、拒绝码和授权真值 |
| `CanonicalObservationWriterR36.java` | canonical 与 REAL 写入可能分裂 | 在原子写事务内固化动作和真实 Trade |
| `DbHelper.java` | 新字段/表和覆盖升级缺失 | 挂接 R915 Schema、additive column migration、保留 DB_VERSION |
| `DbStore.java` | 恢复/任务/真值访问缺 Owner 接口 | 加入 R915 持久化协调访问，不新建第二数据库 Owner |
| `DecisionGateEngine.java` | 全局不出手、授权边界不精确 | 唯一 Gate 接入精确受控候选；完整哈希校验；仍无 Research 直接授权 |
| `DiagnosticStateRendererR906.java` | 原始概率/稳定性/风险等空白 | 16 项真值完整渲染与不可用原因 |
| `InternalWatchdogReceiverR815.java` | Alarm 只拉服务不记业务恢复 | 恢复触发真值与 reconcile |
| `MainActivity.java` | 主线程读库和旧结果覆盖导致卡顿 | 后台读取、latest-wins fence、非空 loading/error 真值 |
| `ModelLearningActivity.java` | 创造页无中文机制/证据/Legacy 完整真值 | 中文资产/算法逐项指标、四重锁、新原子提案、真实晋级/淘汰原因 |
| `PredictionAvailabilityR815.java` | 服务存活冒充预测可用 | 按目标期、PIT、输出合同判断业务可用 |
| `PredictionOutputContractR815.java` | 输出缺完整目标/来源校验 | 预测存在性与赛前合法性合同 |
| `RecoveryCoordinatorR815.java` | 恢复入口分散 | 统一 Alarm/Job/Boot/task-removal 恢复协调 |
| `ReleaseIdentityR817.java` | 研究/生产版本身份旧 | R915 版本和发布模式一致 |
| `ResearchMainlineGovernorR903.java` | 研究可能被误认为直接主链 | 证书边界与无直接 REAL 权限 |
| `ResearchPredrawSettlementPumpR908.java` | 发明算法/资产开奖后未逐项结算 | 读取权威 draw，算法和新资产 exactly-once fanout |
| `ResearchPromotionVerifierR815.java` | 晋级只看局部指标 | 加入合同、Future OOS、多指标和时序门 |
| `ResearchShadowChallengerRuntimeR817.java` | challenger 状态/证据不连续 | 赛前冻结、结算后的生命周期衔接 |
| `ResearchTinyDeepLearnerR909.java` | 深度学习更新口径不透明 | 只从已结算标签在线更新，记录 updates/trainedUntil |
| `RuntimeTruthSnapshotR906.java` | 结算分母失真、状态框缺失 | 两个分母、16 状态框、Legacy/Genesis/算法/提案真值 |
| `ServiceWatchdogJobService.java` | “服务健康”时不执行业务对账 | 周期业务 reconcile 并记录状态 |
| `SettlementCoordinatorR815.java` | 当前任务等待下一调度、侧路耦合 | 有界即时重试与生产优先，sidecar 后置 |
| `SettlementEngine.java` | OOS/REAL/terminal 半提交 | 唯一事务原子结算，幂等终态，缺赛前记录明确排除 |
| `SettlementPostCommitSidecarR909.java` | Research 异常可能污染生产结算 | 生产 commit 后失败开放；资产逐期结算 |
| `SettlementTaskStoreR36.java` | NULL 租约/退避导致任务永久卡死 | `COALESCE` 原子 claim、崩溃租约回收、明确 terminal |
| `V51ArtifactRollbackEngine.java` | 新研究撤证回滚边界不完整 | 新层撤证归零并回到 Legacy baseline |
| `V51AuthorizationCore.java` | 授权签名可能模糊接受 | 完整哈希相等和受控候选签名 |
| `V51BuildManifest.java` | 全局硬锁导致永不出手 | CONTROLLED_CANDIDATE；MODEL/REALTIME_PROVEN 仍 false，禁止虚假声明 |
| `V51Repository.java` | 真实 Trade 非原子/唯一性不足 | 一期最多一个未结 REAL 与 canonical 同事务 |
| `V51RuntimeOrchestrator.java` | 新资产/算法未统合进唯一主链 | Legacy 1200 动作完成后依次 Mixer/Governor，再进入唯一 Gate |
| `WhyTruthSectionRendererR906.java` | 解释只有短状态字符 | Teacher/正推/反推/因果/反哺和赛前/结算证据长解释 |
| `activity_main.xml` | 嵌套滚动与大段同步渲染卡顿 | 单一根 ScrollView 与清晰状态区 |
| `strings.xml` | 版本和按钮文案旧 | R915 真值/创造/诊断文案 |
| `release_identity.properties` | 交付身份分散 | 应用、版本、SDK、DB、发布模式唯一事实表 |
| `R912AlgorithmEvidenceHarness.java` | 旧测试不覆盖新 q 值门 | 新证据策略回归 |
| `R914RuntimeContractHarness.java` | 基线回归不兼容 R915 受控门 | 保留 R914 合同并验证 R915 身份 |
| `AlgorithmProductionGovernorR915.java` | 无算法证书化生产消费 Owner | 16 器官+N240+paired120+FDR 门；评分影响；无 Trade/REAL API |
| `AssetGenesisEngineR915.java` | 新资产创造/结算/证据/原子扩展缺闭环 | Failure 驱动生成、8 active、逐期 freeze/settle、证书/淘汰、新原子只增提案账本 |
| `CertifiedResearchMixerR915.java` | 新资产如何影响主链不明确 | 仅有效证书，Production top64 评估，最大 8%，贡献逐期审计和撤证 |
| `GenesisAssetGenomeR915.java` | 资产只是名词/单数值 | 15 器官、多输出家族、结构/行为身份、变异/血统、机制中文名 |
| `GenesisAssetVmR915.java` | 资产无真实执行/状态/高维输出 | 240 PIT 上下文、compact 扫描、高维物化、赛前只读、exactly-once 更新、40 步行为探针 |
| `GenesisSchemaRegistryR915.java` | 原子/家族和泄漏合同未落码 | 12×20=240 原子、72 家族、类型组合和新原子静态验证 |
| `LegacyFixedAssetRegistryR915.java` | 原有资产是否被屏蔽不可证 | 登记 133 Owner、输出消费者和不可变权限 |
| `LegacyIntegrityGuardR915.java` | 无运行时 Legacy 违规熔断 | source/semantic/DB/behavior 四重锁、mutation/write/evolution 计数与失败停 Genesis |
| `LegacyPredrawContextBridgeR915.java` | 原矩阵证据没有出现在新 UI | 只读汇合 Teacher/趋势/形态/关联/矩阵，不写旧状态 |
| `LegacySourceLockManifestR915.java` | “旧文件未动”无法逐字节证明 | 133 个当前交付 SHA-256；静态门逐文件重算 |
| `ResearchGenomeContractR915.java` | 复杂公式冒充顶级算法 | 16 器官 canonical/parse/hash、证据与生产资格合同 |
| `V51CandidateRealGateR915.java` | 解除硬锁后缺安全受控入口 | rawP/稳定性/PIT/唯一仓位/时间/签名精确门和拒绝原因 |
| `r915_apk_acceptance.sh` | 只编译不验证 APK | applicationId/version/signature continuity/Dex 类/provider key/zipalign/integrity |
| `r915_behavior_gate.sh` | 无一键全合同验收 | 串联源码、SQLite、历史回归、编译、REAL 边界、实质性审计 |
| `r915_build_release.sh` | 无可复现覆盖安装构建 | 从原签名基座构建 Java17/D8、校验核心类、zipalign、同证书签名 |
| `r915_compile_host.sh` | Gradle 环境不足时无法全量验证 | Android stub + Java17 编译 524 文件并跑全部 host harness |
| `r915_real_boundary_gate.sh` | 受控 REAL 可能绕过唯一 Gate | 静态扫描授权/Trade API 和旧模糊签名回归 |
| `R915RuntimeContractHarness.java` | 缺 R915 可执行合同证明 | REAL 门、133/240/72、15/16 器官、状态更新、中文名、行为进化、FDR |
| `r915_source_substance_audit.py` | 文件数/行数可被空壳欺骗 | 524 文件逐个类型/成员/字符/哈希/空壳审计、变更指标、133 基线比较 |
| `r915_sqlite_closed_loop.py` | 编译不能证明崩溃/幂等/证书 | NULL 租约、事务回滚重放、OOS/REAL/terminal、资产结算、算法/资产证书、新原子隔离 |
| `r915_static_contract.py` | 结构红线可能被回归破坏 | 唯一 Owner、后台、结算、REAL、133 哈希、240/72、15/16、UI 真值静态门 |

文档文件说明架构、根因、V4.1 映射和限制；自动生成审计文件覆盖上述每个文件的精确数量指标。任何后续源码变化都必须重新运行 `scripts/r915_behavior_gate.sh` 和 APK 验收，旧审计不能复用。

