# R34.1 学习、研究、资格与主链接口合同

**设计基线：** `construction/r34-20261123@5562f9b19aa09f9f7d9e69ca085db56c537967fc`。本文件为 B06–B09 施工规范；尚未修改、编译或运行应用。现场附件未提供完整 SQLite 原件，报告事实与逐对象根因复演必须分开。方法名按同提交源码核对，内部类入口在下表注明。

## 1. 不可混淆的四个结果

| 结果 | 必需原件 | 不能替代它的东西 |
|---|---|---|
| 创造并出生 | 可解析、通过类型与因果校验的 AST/数值结构；独立 policy identity；父节点变化；训练/行为探针；原子出生回执 | 改名字、复制模板、生成文字建议、计数增加 |
| 真实学习 | 合法时点输入；真实梯度/损失/参数差；完整优化器 checkpoint；游标与状态同提交 | 样本入库、stage/归档成功、RUNNING、历史 cursor |
| 实际研究消费 | 原始frame/matrix→typed proposal→真实子代或预算/回放动作→commit receipt | 显示图表、写 ACK、登记消费者类 |
| 自然质量合格 | 事前注册、独立自然机会、原始冻结/结算、全门槛、合法证书及唯一决策链 | 历史回放、人工阳性例、250次ACT、漂亮曲线、父证书 |

`ENGINEERING_PASS`、`DEVICE_PASS`、`NATURAL_QUALITY_PASS` 是正交状态，不能继承。每工单 `acceptance.level` 指明所需轴；缺设备/自然证据不妨碍工程封板，但不能填写对应轴已通过。所有正反夹具必须在独立数据库/文件根 `test-r341-bxx`，标 `runScope=ENGINEERING_ONLY, productionWritable=false`，使用不同 identity namespace；生产导入器拒绝这些原件。

## 2. 跨层身份与时点

所有新 payload 至少带以下字段，按契约版本规范序列化后 SHA-256；不能依默认 Map 遍历顺序、格式化小数或运行时地址生成 hash。原有身份格式不能被新规范悄然重写。

| 字段组 | 必填内容 | 核验方 |
|---|---|---|
| provenance | sourceCommit、contractVersion、sourceRowKeys、originalHash、sourceRevision、sessionId、owner/fence | 生产者与提交 guard |
| input | datasetHash、role、first/through ordinal、segmentId、inputHash、availableAt、labelKnownAt、featureThrough | input pin / causal evaluator |
| executable | policyHash、canonicalAst、parametersHash、numericalABI、featureABI、optimizerABI、modelHash | BirthGate / model provider |
| target | targetIssue、position、decisionRule、K3 mask、preparedAt、frozenAt、deadlineAt | 唯一冻结 owner |
| learning | sampleKey、epoch、positionCursor、optimizerStep、before/afterStateHash、gradientNorm、parameterChange、loss、checkpointHash | 训练提交 owner |
| research | runId、view/kind、measureKey、proposalId、actionKind、parentHashes、childHash/effectHash、sourceArtifactHash | 消费事务 |
| qualification | experimentId、globalAlphaIndex、gateVersion、exposureThrough、calendarStart、registeredModelHash、comparatorIdentities | 独立资格 owner |

必要顺序是 `特征所含事实可见 ≤ preparedAt ≤ frozenAt < deadlineAt`，训练样本的标签必须在权威开奖/结算可见之后更新。不能把导入历史数据的时间推断为原历史赛前可见时间。正式未来实验的模型必须为注册前已存在的冻结模型；若采用在线学习程序，程序/状态更新规则、每期可见输入和模型演进也必须事先固定，不能事后选版本。当前实现优先沿 `FutureModelRegistrationR34` 的固定模型绑定落地，不新开隐含在线授证通道。

单位置 K3 为固定 `position∈[1,10]` 和三个不同的 `[1,10]` 号码。交集不足只能按原合同 `HOLD`，不能补成其他位置/号码。统计的均匀无信息对照为该位置十类中选三类，基准命中率 0.30；它是合同对照，不能据此保证真实规律存在。

## 3. 状态机、事务与幂等

这些为逻辑状态；存放在既有版本化 payload/原状态表，不直接给旧枚举增加未知值。施工先对照既有 DDL，采用既有索引、guard、publication transaction，避免另建互相竞争的表。确实需新列/索引时必须由当批在 B03 建立的迁移 owner 追加版本化迁移单和 schema hash；不得在热路径临时建表。

| 对象 | 允许顺序 | 原子边界与幂等键 |
|---|---|---|
| 冷启子代 | PRESERVED_PARENT → PREPARED → COMMITTED / WAITING / QUARANTINED | 复用 `r34_cold_rebirth` 主键 `(source_kind,source_campaign,source_id,target_abi)`；child+lineage+receipt+初始状态同提交；snapshotHash是该次已提交子代身份的一部分 |
| 训练步 | INPUT_READY → COMPUTED → STAGED → COMMITTED | `candidate/model epoch + sample/step + ABI`；旧stateHash CAS；state、optimizer、cursor、receipt同提交 |
| 模型发布 | 完整十位置COMMITTED → VERIFIED → PUBLISHED | publicationId 固定完整 manifest；先校验stage，再交换发布头；旧合法模型保持可读 |
| 研究提案 | READY → CLAIMED(fence) → APPLIED / REJECTED / RETRY | `sourceArtifactHash + actionKind + parentIdentity + parameterHash`；复杂计算在事务外，实际动作和消费回执同提交 |
| 非出生动作 | 计划 → 实际预算/回放/剪枝变更 → effect receipt | 复用 `r33_research_applied`，必须有beforeHash/afterHash与实际使用者；重复receipt不能再更新权重 |
| 独立实验 | REGISTERED → ACCUMULATING → QUALIFIED / REJECTED / SUSPENDED | 原全局alpha index永久消耗；opportunity `(experiment,target)` 唯一；序贯状态与结算账/cursor同提交 |

写入一律经过 B02 公平准入与既有短事务；读取/梯度/回放/大对象编码不占写锁。`owner/fence`、源revision、旧stateHash任一不符则不得提交。事务已提交后进程死亡，重入返回原结果；不得把重试变成新身份。单对象坏数据隔离并继续轮转，`CAPACITY_WAIT`/`WAITING_SOURCE`不是成功也不是所有对象的永久异常。

保留原父代、旧checkpoint和所有曝光/独立证据。重生的 `requireFreshCounts(cursor,train,confirm,future,certificates)` 必须全零；旧父代本身不清零。退休只退出活跃调度与热索引，引用仍存在的原件不能删除；B10 负责验证后归档与保留策略。

## 4. 角色与反馈边界

| 角色 | 可做 | 不可做 |
|---|---|---|
| TRAIN | 梯度、可学习参数、冻结行为探针 | 当独立确认/未来成绩 |
| SELECT | 开发择优、消融、固定预算选择、开发校准 | 再冒充未见数据；直接授证 |
| ONLINE_RESEARCH | 用已可见公开事实驱动后续新研究 | 改写原冻结或将该事实算成新身份独立未来 |
| CONFIRM / FUTURE | 按预先冻结合同评分、资格、配对核验 | 回调同实验阈值/参数/位置/预算；使用当期标签事后换模型 |
| SYNTHETIC / ENGINEERING_ONLY | 工程正反例、误报/功效模拟 | 写自然证据、生产证书或REAL |

反馈不是永久封锁公开历史事实。正式实验结果可在**结算之后**投影成后续新开发任务的公开输入，但必须建立新 `selectionBoundary`、新dataset revision、暴露清单与新子代身份，且这些已曝光样本不可再成为该子代的独立未来证据。原实验模型、阈值、校准和未来战绩保持不变。`ResearchFeedbackCP12.prepareFinding/commitFinding` 已有 action/input-boundary结构，应在其原事务上落实此隔离，不另起绕过边界的反馈写路径。

不能把 `CONFIRM` 简单改名字为 `SELECT` 消除泄漏；需要显式关闭/冻结旧实验，并在新实验谱系中记录已暴露范围。各任务读身份和可见时点，而不是相信字符串角色就代表独立。

## 5. 精确源代码接口与新增模块

全部源路径前缀为 `app/src/main/java/com/ethk3/novaonepro/`；未标主包者均在 `strategyv2/`。

| 现有接口 | 本轮施工职责 |
|---|---|
| `ColdRebirthR34.prepare/publish/step`；`ColdRebirthContractR34.sourceKey/campaign/requireSnapshot/requireFreshCounts` | 父代保全、新identity零继承、owner/fence提交、逐父代重试 |
| `FoundryCampaignR26.capture/step/publishBirthPreparedR31/publishFinishPreparedR31`；`WarmCandidateR26.step/authenticate` | 正常源→真实训练→有界提交与恢复；出生与质量分开 |
| `SymbolicFamilyR26.update/write/read/lastTrainingStep` | F13/F14实际数值训练及完整状态 |
| `PolicyAstR27.parse/type/rangeAndCausality/evalScopedR31`；`PolicyContractR30.violations` | 可执行AST、类型、时点、资源与K3合同 |
| `AtomNetRuntimeR30.step/forecast`；`AtomNetSessionR30.work/encode/decode/consumableR31` | 十位置学习和完整epoch |
| `AtomNetPublicationR31.Prepared.verifyPrepared/stagingBatches/publicationBatch` | 模型stage、验证、原子发布；方法位于Prepared内部类 |
| `ModelSurfaceProviderR30.predict/validate`；`ModelSurfaceStoreR30.surface/calls` | 同模型概率面与真实调用 |
| `RuntimeR27.DatasetTask.step`；`RuntimeR27.publishDatasetInputR335` | 既有数据owner；仅在不可变pin提交后暴露cache |
| `ResearchInputPinR30R1.require/requireRun`；`ResearchInputContextR33.acquire` | 固定角色/版本/水位，不拿latest覆盖旧输入 |
| `ResearchObservatoryR30R1.step`；`MatrixResearchR30R1.step` | 一视图/一矩阵kind的有界真实生产与轮转 |
| `ResearchActionPolicyR33.Source.requireAction`；`ResearchConsumptionStoreR31.completeWithin` | 源合同到出生/非出生动作；前者在Source内部类 |
| `EvolutionLoopR27.stepInsideEvolution/snapshot/restore`；`FusionOpsR27.fuse/weighted` | 真实代际和同目标双亲组合 |
| `FrozenConfirmationStoreR30.freeze/register`；`QualificationInboxR29.step/consume` | 独立确认、永久alpha、按完整机会日历推进 |
| `MainlineFreezeProofR31.publishWithinFreeze/allows`；`MainlineAdapterR31.prepare/verifyFrozen` | 合法证书接原冻结动作；不能重算改号 |
| 主包 `DecisionGateEngine.decideV51B12`、`SettlementEngine.settleV51ByDraw` | 唯一授权与唯一结算，不被研究模块取代 |

以下 **NEW** 文件只增加纯合同/桥接能力，不新增后台owner或REAL权限。接口参数中的DTO均为该新类的不可变嵌套类型，施工时需一并实现，不能留下未定义占位类型。

1. **`FormulaIdentityR341.java`**：`canonical(PolicyAstR27.Node, Abi) -> byte[]`；`fingerprint(Node, Probe) -> Behavior`；`diff(Node, Node) -> List<NodeEdit>`。Abi含numerical/feature/canonical版本；Probe含固定TRAIN行键、输入hash、targetPosition；Behavior含概率/动作hash与支持数；NodeEdit含path、op、before/after subtreeHash。调用点为 `StructuralProposalR26.add` 与 `BirthGateR27.checkCandidate`。浮点安全规范化规则和对照见算法协议。
2. **`AssetQualityVerdictR341.java`**：`evaluate(V1Verdict, CalendarProof, PairedProof, Scope) -> QualityVerdict`；`serialize(QualityVerdict) -> byte[]`。V1Verdict引用原 `CertificateGateR27.Verdict`；CalendarProof含完整机会/缺口/覆盖，PairedProof含同目标父子/控制原件与界；输出各Q项状态/原因/引用。只供质量报告和验收，不自行授证、不增加V1首用门槛；未观察到部署后REAL频率不得压住有效证书自动进入既定选择。
3. **`QualityExperimentR341.java`**：离线 `run(Spec, Seed)`、`resume(Checkpoint)`、`summarize(List<Trial>)`。Spec冻结source/config、seed集、完整搜索预算、null/植入机制、次数与统计精度；Trial保留全部搜索/失败/超时；Checkpoint含原搜索状态/随机状态/alpha分配。命令入口NEW `tools/quality_experiment_r341`，Android生产构建不得自动执行该runner。

## 6. 检验矩阵与可复演材料

每工单至少包含正常路径、否定路径和中断恢复。测试材料必须给输入生成版本/seed、期望或参照计算、调用的真实源码入口、数据库与文件根、生产隔离证明、实际输出、退出码、source/config哈希。只验证正则存在、代码类名存在或模拟一段独立玩具逻辑，不构成本工单工程通过。

数值验证选择预先固定的可学习样例和无信号样例；先验证参考计算与真实优化器，再验证事务崩溃恢复。允许真实零梯度和没有改进；不能设“所有数据都必须高胜率”的不可能工程门槛。跨设备浮点对比必须声明ABI与容差，不能测试失败后扩大容差。

字段完整性、18图/20矩阵/165对象覆盖以及源码路径锚点检查，只验证本方案可追踪性，不能当Android功能通过。真实设备持续后台、当前源码Q08完成及自然高品质资产均保留待验证；每种等待有单独状态，不把工程实现未完成藏在“等未来”后面。

## 7. 首次应用与频率分母禁止成环

部署前只可评估预登记的 `FUTURE_ACT_RATE=独立未来ACT/预登记未来机会`；部署后另报 `REAL_ACT_RATE=实际REAL/部署机会`。后者首次部署前未观察，不能成为首次部署条件。有效V1证书只要处于原有效期且PIT、模型、校准、撤销等原依赖满足，应自动进入既定选择。额外Q项用于质量透明度，不静默修改V1资格或卡住首次应用；若要改变授权判据，必须新协议版本及新前瞻实验。

矩阵typed消费者并非缺失：`MatrixRegistryR30.typedConsumers` 已列 `ResearchContextR30R1` 的假设/互补/校准/剪枝/残差/位置/跨位/行动/权重入口，以及原ResearchPool和ModelReplay入口。本轮复用这些接口，取消冗余新router；旧algorithmConsumer诊断字段不能代替typed调用核查。

实际发证以 `LiveShadowsR27.certify` 为准。核对数学判定、实发门、额外质量三份原件和同一资格窗口。覆盖与收益等实发条件由部署前的独立未来记录计算；部署后的 REAL 频率另行观测。详见[资格边界与回归](review/algorithm.md)。

追加结构在对应实施批次通过 B03 建立的迁移接口递增版本并自验。B03 的签收使用该批已声明的结构和任务合同；后批不能倒改已发布的迁移 hash。
