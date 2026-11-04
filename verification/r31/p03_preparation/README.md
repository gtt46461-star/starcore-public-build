# R31 P03 / B06 只读准备

状态：`READ_ONLY_PREPARATION`。固定报告/计划基线为 `5237b265a27fe5ad27fd842901c445d5f8c27749`；本次检查时分支为 `construction/r31-final-20261104`，已提交源码 HEAD 为 `844a78488f396189c2db98591782b7bb214799d2`（P01）。精确文件摘要见 `source_identity.json`。

本目录首轮为源码发现、拟议方法合同、额外文件范围及测试准备。root 后续授权在隔离 `/tmp` 执行真实反例，结果见 [repro/README.md](repro/README.md)：已运行当前真实生产方法的 JDK 数值恢复及 publication 反例，完整发布合同仍为 baseline FAIL。没有修改生产源码或 tools，没有运行 Gradle 或 Android 设备测试，没有 commit/push。P03 施工须等 root 明确通知 P02 gate；新合同仍为 `PROPOSED_NOT_IMPLEMENTED`，不得把 host 反例复现或数值恢复单项通过用于关闭 B06、设备、自然质量或 72h 验收。

已读计划 README、docs 00–05/12、B06 三卡、cards/matrix/shared-conflicts/acceptance_config，以及本报告引用的真实源码和测试。190 个打包源码不代表完整工程；没有把包完整性 1205 PASS 当成源码、运行或能力 PASS，也没有声称逐一审阅完整工程的全部 Java 文件。

## 1. 原报告证明的边界

`report_excerpt.json` 从原始 R30 报告逐字段提取，并绑定原文件 SHA-256。原报告中十位置 checkpoint 的 owner 为 `ATOMNET_TEN_R30_REPAIR1`，state hash 为 `554ed996766a7f0c82c60bdd6e6774b4b554c4b8ef7c897fba8a43f33e239700`。缓存记录有 2080 个材料化样本、`cachePeriod=232`、`phase=TRAIN`、`displayPhase=MATERIALIZE`，但训练样本/在线样本/训练毫秒均为 0，`publicationReady=false`，权重训练水位为空。第 6 章 `modelCalls`、`calibration` 均为空。

该记录足以证明已持久材料化进度，不能证明产生梯度、完整发布、真实预测消费或自然质量提升。报告没有包含可独立重放该 state hash 的完整原始模型 bytes；迁移验收需要真实设备/备份中的原 bytes，不可用新建 fixture 替代原 checkpoint。任务隔离及领取写异常也不能据此推断为“历史训练从未实现”。

## 2. 已有能力与明确缺口

### 2.1 历史输入、角色与 revision

* `RuntimeR27:18,133–228` 已有 `DATASET_ROWS=5000`、360 新期刷新、持久输入 pin、source revision 检查、事务成功后发布及代边界采用 pending cache。必须保留这条链。P01 的 `submit(..., ownerInputSnapshot)`、owner/fence、线程终止和 writer receipt 已有真实实现，不重写或旁路。
* `ResearchInputPinR30R1:9–44` 将完整冻结行压缩存入 `r30r1_analysis_run`，以 dataset/revision 定位；源数据更正/范围内补录使原身份 invalid，普通尾部追加和原始热尾删除不使冻结副本失效。现有原副本必须保留。它没有统一 pin 配额/生命周期、显式 reader ABI、known_at/角色合同 envelope；不可把“有 pin”写成“预算闭环已完成”。
* `ReplayDatasetR27:58–119` 已排序、拒绝重复 ordinal、保留 gap segment 和数值副本。旧 hash 绑定 issue/数值/segment，但不显式绑定每个 ordinal、known_at、source revision、role 或 reader ABI。`ordinalOf` 的 Unix epochday 基准与生产 `TargetIssueResolver` 的 2020 基准不同；不可混用、不可用 issue 字符串相减算缺期。保留现 hash 以维护既有模型身份，新增版本化 job/input envelope 绑定规范日历、每行 ordinal/availability/gap 和角色。
* `StoreR27.loadDataset:27–39` 读取最多 5000 条干净热尾后，仍可由验证过的 history import 追加更早数据，最多 10000 行。不能以“5000 保留”删除已有有效导入能力；热尾保留与作业冻结总行数要分开计费。
* `FrozenConfirmationStoreR30.freeze/context/complete/register` 已冻结输入、packet 和 trace，register 的重试不重新领取 global index，`generationReport` 只补报告，不把确认反馈给搜索。应增加统一 pin 引用/预算，保持 EXPOSED_HISTORY、INDEPENDENT_CONFIRM、UNIQUE_FUTURE 隔离；未知历史可见时间只能标 UNKNOWN/EXPOSED_HISTORY，不能补造“当时可见”。确认解压读取还应在既有格式约束下补充显式上限/取消点，不能用失去原数据的截断代替失败。

### 2.2 已有精确数值 checkpoint，应复用

* `ModelArtifactR29.encode/decode:38–104` 已持久完整 NN 权重及 Adam 槽、rollback net、dropout/采样 RNG、学习率、online/check counters、next epoch receipt、replay 原样本及 event、FTRL 超参和 z/n/w。`TOLERANCE=0`；旧 CP04 缺失 receipt cursor 明确要求持久记录恢复，不默认归零。只保存权重会破坏现有能力。
* `AtomNetSessionR30.encode/decode:140–171` 的外层 1/2/3 已保存 phase、shuffle permutation、epoch/offset/step、early-stop bestNet、统计矩、position cursor、replay policy/evidence，以及上述完整内层。新水位适合放在十位置外层或版本化作业 envelope；不必为了卡片文件列表而重写通用内层 ABI。
* `CheckpointArtifactsR26:55–105` 是已投产 CAS：prepare lease → fsync 临时文件 → 校验 → 同目录 rename → directory fsync；in-flight lease 保护到引用提交，resolve 兼容旧内联 bytes；现有 `ChunkedCheckpointR26` 可存流式大状态及子块闭包。单文件 32MiB、根 256MiB/8192 文件与 app 总盘预算不同。十位置 decoder 允许到 64MiB，不能因 CAS 单文件上限而拒绝原本合法的旧状态并重训；大状态应复用分块通道或持久资源等待，原 bytes 保留。
* `TrainingCasCP12` 是已有输入 CAS，有 `.scdev/.scbind` 格式限制、128MiB 根配额和 8MiB 单输入上限，持有文件锁至引用提交，支持明确 storage wait。它不是任意模型 BLOB 仓库。若复用其输入通道，必须使用 `DevelopmentInputCP9` 的真实格式/绑定，不能把当前 gzip JSON 改个后缀塞入。数值 checkpoint 直接复用 `CheckpointArtifactsR26`，不再建立独立 `ReplayCheckpointR31` 存储系统。
* `FoundryCampaignR26:22–109` 已捕获含 opened/available 时间的 4096 行固定快照，按真实模型数值训练，单片一般 <=16 样本；lastGood 模型/校准状态在事务外编码，CAS 文件准备在事务外，事务内校验 source/cursor/parent、提交引用和 receipt。应修复出生与终结写入分段，而不是重造其 checkpoint。
* `ReplayWorkspaceR26:15–72` 已按 64 标签提交 RAW/CAL 模型输出块、全数值状态及 next cursor；key 已绑定 source/model/training-rule ABI，验证 chunk 连续和 shape/hash，预测先于学习。`LegacyGenerationR26.compute` 正在真实使用它。不能报告成“没有增量回放”。
* `LegacyGenerationR26` 的 P01 cooperative helper/cancel/真正终止 owner 已更新，勿覆盖。成员输出已有 durable chunk；但 `ModelReplaySU2.evaluate`、generation judge/committee/mutation 等成员之后的纯计算阶段仍可能重算。需要显式子阶段/输出/RNG 恢复证据，不能把成员块等同于整个 generation 已满足每个断点恢复。

### 2.3 十位置 partial 被当成可发布模型，且重启改变模型身份

1. `AtomNetSessionR30.online:108–116` 一次只训练一个位置；位置 1–9 后仍为 ONLINE，`partialIssue` 是本期、`nextPosition` 前进，而 completeIssue 仍是上一期。
2. `trainedThrough:119` 返回 partial，`trainedThroughOrdinal:120` 取输入/在线已观测最大值，`ready:121` 只判断 phase=ONLINE；`report:134–136` 据此标可发布。
3. `AtomNetRuntimeR30.step:62–64` 每片成功即 `publishAtomStateR30(next)`；`RuntimeR27:40` 编码此时的当前 net，故位置 1–9 的模型直接进入发布对象。
4. 真实消费链为 `AtomPublicationR30 → ModelSurfaceStoreR30.prepareLive → LiveShadowsR27.predict/calls → FeatureCacheR27 / I08`；`AtomNetRuntimeR30.forecast` 也直接用当前 `state.trainer.net` 和 FTRL。没有证明 MainlineBridge 直接读取该 publication，本准备不把它列为直接消费者。
5. `RuntimeR27.publishAtomStateR30` 每次含恢复时都取当前 wall time。`FrozenModelRef` 的 modelHash 绑定 availableAt，故同一权重重启后可得到不同 modelHash。发布时间/hash 必须由已提交 receipt 恢复，不可每次重建。
6. `ResearchProvenanceGraphR30R1.gradient:12–15` 也只看 ready，把当前 partial net 写为 `NEURAL_GRADIENT_PUBLICATION`，publicationAvailableAt 却为空。梯度提交与完整发布必须是两类事实。

最小修复是同时维护“可续进的最新训练 checkpoint”和“上一次完整发布的不可变数值状态/引用”。只把 `ready` 改为 partial 为空会暂停已有完整模型的预测，而且不能恢复上次完整 FTRL；只改报告字段也无法阻止消费 partial。训练中保留旧完整发布，位置 10 的状态、complete 水位、publication receipt 同一次成功提交后才更新共享模型。独立发布对象须拥有全套预测所需 NN/FTRL 或其完整可验证引用，forecast 不能再混用新 partial 的 FTRL 与旧完整 NN。

### 2.4 初次训练截止不能由 holdout 输入尾端推导

`AtomNetRuntimeR30:21` 以原 holdout 最后一行构造 observedThrough；`AtomNetSessionR30:79` 在 FILL 完成时把 observedThrough 复制成 completeIssue。实际 NN 梯度读 TRAIN、模型选择/风险检查读 SELECT，FTRL 读 TRAIN+SELECT，FILL 也只读 TRAIN+SELECT；原 formal holdout 禁止进入优化器。

新增 input boundary 与实际训练/完整截止后，要保留独立 online admission baseline（原输入尾端），避免把降低“真实训练截止”误变为允许补学原 holdout。对旧 checkpoint，保留原完整 bytes 和原 online cursor，按可信证据推导水位；无法证明的完整发布标未知，不伪造过去的发布或重置 optimizer/RNG。partial 原状态必须按 nextPosition 继续，第 10 位置完成后能形成可证明的新发布；旧预测仍按原 modelHash/bytes 可回放。

## 3. B02 owner staging 债务：P01 的 writer 协议没有自动解决它

|真实入口|现状|需要的增量合同|
|---|---|---|
|AtomNetRuntime materialize:31–42|一片最多 12 期 × 10 = 120 条样本 SQL，之后还有 checkpoint/进度/epoch/provenance；64 训练样本不是 64 写操作|按实际操作数和 bind bytes 预留元数据开销；分段后材料化游标/receipt 必须和对应行同提交，期间不得跳过/重复样本|
|AtomNetStore checkpoint:76–87|完整数值 BLOB 放入 writer bind；尚无 expected previous state CAS；archive 只覆盖外层1/2，不覆盖当前3|事务外 CAS 准备；事务内验证父 state/input/fence，写轻引用+cursor+事件receipt；首次迁移先精确保留外层3原 bytes|
|RuntimeR27 EvolutionTask + EvolutionLoopR27.birth/validate|默认 births=128，单步 sink 可产生 policy/lineage/score 大批，最后再全 snapshot/RNG cursor|owner 准备不可变结果，持久 staged chunks 和完成清单；final commit 后才替换可见 generation/cursor，队列不能任意切开原子语义|
|FoundryCampaign birth:49–52、finish:109|出生 proposal 循环与终结 saveGenome/top3 在 writer 内，identity ledger 还有隐藏 SQL/编码|事务外预编码 DTO；owner chunk receipt，最后可见 publish/finalize 同提交；保持父系/全部候选|
|LegacyGeneration final commit:60–66 / ModelStoreSU2:25–44|population/HOF/committee/stage/matrix 循环；saveEvolution 在 writer 内调用 ModelStateCodecSU2.encode；saveGenome 可能执行多条 SQL|预编码 state/canonical/lineage，按实际 SQL 工作量和 bytes 分段，最终 generation 指针原子发布；不能删除候选凑预算|
|FrozenConfirmationStore register|batch before callback 内可 INSERT SHADOW，普通 batch opCount 看不见|计入该 owner 的真实 op/bytes/hold receipt；确认 reservation/result 不可拆坏或重复分配|

起始预算必须采用 acceptance_config：write ops 64、bind bytes `min(262144,maxMemory/512)`；训练 <=64 样本、回放 <=256 行还受 P02 时间/内存 admission 约束，均不是测量结果。writer hold p99 50ms / >200ms fault receipt 必须用目标设备证据核对。若某合法原子单元超预算，返回有原因的资源等待/保护债务，不能偷偷放宽预算或将其算 PASS。

## 4. 最小实施顺序与跨 owner 接口

1. root 关闭 P02 后，固定当时 integrated HEAD，先向 P01/B02 owner 合并输入 snapshot、expected parent、业务幂等 identity 和真实 op/bytes 计费合同；不自行改 DatabaseCoordinator/ResearchWriteQueue/RuntimeCoordinator 等 owner 文件。
2. 在现有 pin 和 checkpoint 通道外增加版本化输入/角色 envelope、受保护引用和 admission wait；保留旧 datasetHash、5000 热尾和进行中完整快照。
3. 先补 checkpoint/输入/发布根的可达性，再启用新 CAS 引用，保持旧内联读取和未知格式 fail-closed。P03 只增加保护与恢复引用；不启用新 GC，不提前执行 P06 回收。
4. 外层 codec 增加 complete/partial/published/consumed 事实（见 method_contracts.json）；事务内一并提交 position event、数值引用、nextPosition、完整发布 receipt；事务外只发布已提交对象。恢复从 receipt 的固定 availableAt/hash 还原。
5. 修正所有真实消费与证明读取者，再做 owner staging。成员回放复用 ReplayWorkspace；只补尚未覆盖的 post-member 阶段，不变更数值顺序、RNG 消耗、早停、replay allocation、FTRL、校准、gate/act/K3。
6. 先运行 exact numerical/gap/PIT/role/migration/故障测试，联合 P01/P02 操作/bytes/fence 验证后，再交给主任务跑 fresh build 与 Android/长期设备门。施工前后保留原 bytes/hash 与恢复清单。

## 5. 必须额外纳入的文件

详表见 `scope.csv`，区分直接必需、共享 owner、采用特定存储方案才必需、既有组件仅复用。

* 三卡没有列出实际入口 `AtomNetRuntimeR30` / `AtomNetStoreR30`，它们必须纳入，否则训练批大小、持久 cursor、restore、forecast 都不会改变。
* `CheckpointReferencesR26.hashes` 目前只枚举 7 类 r26 state 和 replay chunks，不含 r29_model_checkpoint 或 r30_model_artifact。新 ref 若没有根，`ArtifactMaintenanceR26` 现有 60s grace 后能删掉；`ReportAttachmentClosureCP12` 复用同一 root census，备份也会漏。P03 必须先增加 root，P06 owner 接手完整回收规则。若新设计把 checkpoint manifest 放入 r26_checkpoint_artifact，还须扩展其 metadata 保护查询；不应凭空创建重复 manifest。
* `CapabilityRegistryStoreR30` 直接对 r29 artifact 调用 Session.decode；`ReportSnapshotR30` 硬编码旧 owner。改 owner/ref 后必须适配，不能把 CAS 引用误报损坏、漏掉合法旧模型或只改 cache。
* 发布证明涉及 `ModelSurfaceProviderR30` / `ModelSurfaceStoreR30` / `ResearchProvenanceGraphR30R1` / `ModelConsumptionCensusR30R1`；`LiveShadowsR27` 需验证实际 receipt/hash 联接。`AtomNetRuntime.forecast` 要使用同一已发布对象的完整 NN/FTRL。
* 若 r30_model_artifact.weights 也迁移 CAS，`ModelReplayStudyR30R1.previous` 和 provenance 的直接 raw byte 读取必须一起适配；这是条件范围，不要求无故搬迁当前可在预算内的权重 BLOB。r29 数值 checkpoint 的大 BLOB 与 publication weights 要分别核算。
* `ReplayWorkspaceR26`、`ModelStoreSU2`、`ResearchInputPinR30R1` 是必要的恢复/staging/pin 连接点；DDL 由 schema owner 统一增量登记，使用现有 migration framework，禁止 destructive recreate。

## 6. 验证状态

`test_plan.md` 按现有真实测试、需新增断言、主机/Android 真实性边界列出。后续实际执行的独立反例 harness 及范围见 `repro/README.md`。现有 R30 十位置单测明确接受 partial trainedThrough，不能未经修订就拿旧绿灯证明 C03。原主机端到端 restart harness 使用 Unsafe 构造 runtime，P01 后缺有效 currentClaim；端到端验证必须接入真实 claim/writer，不能为跑绿旁路新 fence。此次仅调用数值/codec/publication/surface 方法的隔离反例未声称执行领取或 writer。

本文档与 JSON/CSV 的格式、路径存在性可以在本目录内校验；这不等于代码编译或任何业务/设备验收。
