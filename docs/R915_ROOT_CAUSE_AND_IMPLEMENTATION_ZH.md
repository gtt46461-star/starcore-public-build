# R915 问题根因与代码施工结果

## 1. 后台退出后不运行

### 根因

- 服务存活和“业务真的继续推进”混为一个健康状态；服务对象还在不代表开奖抓取、预测、结算三条业务游标在前进。
- 任务划走后的恢复只覆盖部分入口，Alarm、Job、Boot 在“服务看似健康”时没有强制执行业务对账。
- 后台被回收后可能残留 RUNNING 租约；旧数据中的 `lease_until/next_attempt_at` 允许 NULL，比较表达式不成立，任务永久不可 claim。

### 修改

- `AutoFetchService`：`START_STICKY`；`onTaskRemoved` 重新布置 Alarm/Job 并触发最新期分析；预测、结算、Research 使用独立预算。
- `BootReceiver`、`InternalWatchdogReceiverR815`、`ServiceWatchdogJobService`、`RecoveryCoordinatorR815`：无论进程新建还是服务表面存活，都执行轻量业务 reconcile；记录触发源和恢复真值。
- `PredictionAvailabilityR815`、`PredictionOutputContractR815`：区分服务状态、目标期、赛前输出合同和业务新鲜度。
- Android 明确“强行停止”仍是系统边界：强停后 Alarm/Job/广播都会被冻结，必须用户手动再次启动。

## 2. 一百期只结算三四期

### 根因

- 抓取、下一期预测和当前期结算曾共享一条调度路径；预测失败或耗时会阻断已开奖的结算。
- 任务 claim 对 NULL 租约不安全；崩溃后存在永远卡住的 RUNNING。
- 当期与历史欠账没有明确公平调度；只盯最新期会饿死 backlog，只回补历史又会错过当前期。
- OOS、REAL 和任务终态分散提交，异常时可能出现“有开奖但任务没终态”或“交易结了但统计没结”的半状态。
- Research sidecar 失败可能反向污染生产结算。

### 修改

- `SettlementTaskStoreR36`：每个抓取到的有效 issue 均有 durable task；`COALESCE(lease_until,0)` 与 `COALESCE(next_attempt_at,0)` 恢复旧 NULL；claim/续租/终态条件化更新。
- `BusinessReconcilerR36`：当前期快速通道与下一期预测通道拆开；当前与 backlog 公平收敛。
- `SettlementCoordinatorR815`：有界即时重试当前精确任务，未完成仍由持久队列继续。
- `SettlementEngine`：在唯一事务里完成严格 OOS、REAL Trade 结果与任务 terminal；重复执行变成幂等读取，不重复计数。
- `ResearchPredrawSettlementPumpR908`、`SettlementPostCommitSidecarR909`：算法与新资产读取权威开奖逐项结算，但完全位于生产 commit 之后并失败开放。
- `RuntimeTruthSnapshotR906`：分别显示“赛前冻结可结算分母”和“全部开奖任务分母”，不再把少量已完成任务伪装成 100% 覆盖。

## 3. 根本不出手

### 根因

- 先前 release flags 同时存在全局 SHADOW/HARD_DISABLED，导致即使动作满足条件也不可能写 REAL。
- 候选概率口径混杂；未校准的 rawP 可能被当作 calibration 后概率，或授权签名使用模糊前缀比较。
- 真实交易落库和 canonical observation 不是一个原子决定，容易出现授权/记录不一致。

### 修改

- `V51BuildManifest`、`AppConfig`、构建身份：切换到 `CONTROLLED_CANDIDATE_MODEL_NOT_PROVEN`；明确 `MODEL_PROVEN=false` 与 `REALTIME_PROVEN=false`，不伪造模型证明。
- `V51CandidateRealGateR915`：检查精确 target、原始概率阈值、动作稳定性、唯一未结 REAL、PIT、授权哈希和完整动作合同；拒绝伪校准输入。
- `DecisionGateEngine`、`V51AuthorizationCore`：仅接受完整相等签名，不能前缀/模糊通过。
- `CanonicalObservationWriterR36`、`V51Repository`：canonical 与真实 Trade 在单一写事务内落库；一期开仓最多一条。
- `CanonicalDecisionViewAdapterR36`、`DiagnosticStateRendererR906`：界面显示真实 BET/HOLD 和精确拒绝码。

## 4. “为什么中/不中”只剩一小段字符

### 根因

- 页面曾把运行状态摘要当成解释内容；旧 Teacher/矩阵证据仍在工程里，但没有从只读桥汇合到当前界面。
- 状态框渲染对缺字段用空字符串，导致动作稳定性、号码风险、原始概率、形态等视觉上为空。

### 修改

- `LegacyPredrawContextBridgeR915`：只读聚合教师后验、正向推理、反推矩阵、因果口径、反哺契约、形态/趋势/号码/位置关联以及新研究贡献。
- `WhyTruthSectionRendererR906`：命中和未命中均给出赛前证据、开奖后结算事实和各矩阵作用，禁止用模型名词冒充因果证明。
- `RuntimeTruthSnapshotR906`：恢复 16 项真值，包括原始概率、动作稳定性、号码风险、形态、窗口共识、跨位支持、校准、深度学习、算法迭代和 DecisionGate。
- `MainActivity`：数据库读取放后台、latest-wins generation fence、防止旧结果覆盖新结果。
- `activity_main.xml`：只保留一个根 ScrollView，避免嵌套滚动竞争；长内容按状态框打开查看。

## 5. 深度学习、动态迭代和创造只有空壳

### 根因

- 旧候选主要是表达式树和参数搜索，缺少独立持久状态和开奖结果后的更新程序。
- 每个算法重复构建 1200×特征观察，移动端预算耗尽，造成所有候选长期只冻结/结算一条。
- 覆盖率、配对主链增益和 FDR 计算存在口径错误：覆盖率硬编码、配对 delta 使用非配对总体、负增益被算作新增信息、FDR 不是检验 q 值。
- 中文名没有绑定真实结构；结构不同但行为近似的候选可能换名占满 active 池。
- Legacy 与新层边界不够强，用户无法确认原有趋势、形态和矩阵是否仍在。

### 修改

- `AlgorithmGenomeR908/AlgorithmGenomeVmR908`：算法必须同时拥有 State、ScoreProgram、SettlementUpdateProgram；赛前 score 不可写状态，结算后 update 必须读取 outcome 并改变状态。
- `ResearchGenomeContractR915`：16 器官完整合同；解析、序列化、证据、PIT、晋级边界形成一个哈希身份。
- `AlgorithmInventionLabR908`：同一期 1200 动作观察只构建一次并让最多 8 个 active genome 只读共享；子代 Future OOS 清零；结构与执行行为双重去重。
- `AlgorithmEvidencePolicyR912/AlgorithmEvolutionLifecycleR912`：N≥240、paired≥120、Wilson、Block、Regime、Brier、ECE、Coverage、MaxMiss、FDR 一起决定保留/晋级/淘汰。
- `AlgorithmProductionGovernorR915`：只有 16 器官合同完整且证据通过的 challenger 才能影响 Production，仍无 REAL 权限。
- `GenesisSchemaRegistryR915`：240 个 PIT 原子与 72 个家族；所有原子带 datatype/leakage/updateTiming。
- `GenesisAssetGenomeR915/GenesisAssetVmR915`：15 器官，支持 scalar/vector/matrix/graph/tensor/sequence/state-machine/distribution/prototype/risk/stability/uncertainty 等真实输出形状；有独立状态与 exactly-once 更新。
- `AssetGenesisEngineR915`：FailureVector→NeedPlan→生成/变异→赛前冻结→结算→证据→证书/淘汰；最多 8 个 active，避免候选稀释到每个只结算一次。
- `CertifiedResearchMixerR915`：有证书资产最多 8% 影响且可撤证归零。
- `LegacyFixedAssetRegistryR915/LegacySourceLockManifestR915/LegacyIntegrityGuardR915`：133 个 Owner 四重锁；Genesis 对 Legacy 的 mutation/prune/replace 路径为 0。
- 新原子提案：写入独立 `research_new_atom_proposal_v41`；完整验证来源、类型、PIT、泄漏、资源、消费家族和不可约证据。数据库提案只能待签名实现，不能热改 240 原子的历史语义。

## 6. 为什么不承诺固定 70%–80%

软件可以扩大算法搜索空间、减少重复错误、自动淘汰、提高增量信息和校准，但随机性或弱可预测环境中不存在仅靠代码即可保证的胜率。R915 把“越来越好”定义为可测的进化方向：同类失败复发下降、垃圾淘汰更快、增量/互补提高、校准改善、覆盖提高、资源和结算延迟下降。任何 70%–80% 结果只能在足量、严格时间顺序、未参与搜索的 Future OOS 上由多指标证书证明。

