# R34 学习、创造、进化与自然认证施工设计

## 0. 基线、范围与交付状态

本设计只以实际 APK 对应源码 `construction/r335-field-095`、提交 `273641e838e0b8dd842b42a67a674e184942a30a` 为源码基线；不以旧的 033 工程前态覆盖 095。检测材料为 `870cd99`、`89d1cac`、`fabedd0` 三个原报告包。文中源码路径以 `app/src/main/java/com/ethk3/novaonepro/` 为根。

交付分两层：

1. **B06 冷启重生修复已有可审阅实现**：`learning_payload/` 中三个完整 Java 类、八个原文件的精确集成补丁、原文件/后像哈希、纯 Java 与原生 SQLite 宿主验证。没有改动现有应用源码，没有安装 APK，也没有伪造自然样本。验收结果为 `PASS_LIMITED_HOST_SCOPE`，不等于 Android 编译或真机链路通过。
2. **B06–B10 其余工作为施工契约与工单**：明确在 095 哪个生产 owner 修改、写什么事实、由谁消费、哪些条件判失败。不能把这些设计文字当已运行功能，也不能把报告中已有类名当当前功能成功。

核心结论：目前能确证的是**大量对象保留在旧数值 ABI 阻断态、研究输入/输出/自然认证链缺少实际落库成果**，并不是源码里完全没有数值算法。R34 的第一任务是让已有真实 owner 形成可恢复、有界、可审计的运行链；再用相互隔离的开发、验证与自然样本检验是否存在稳定增益。能工程性保证持续研究和诚实计量，不能保证自然序列可预测，也不能保证最终获得高胜率资产。

## 1. 三份报告确认了什么

| 事实 | 证据位置 | 能得出的结论 | 不能据此得出的结论 |
|---|---|---|---|
| 三份 `STRATEGY_MATRICES.json` 均为 `SU2_MATRICES_V1`、`snapshots=[]` | 三个报告包同名文件 | 导出快照确为空，图表缺数据有事实基础 | 不能仅凭空 JSON 判断唯一原因是 UI |
| `naturalQuality=NOT_PROVEN`；`overallAcceptance=FAIL_RECORDED_METRICS` | `R30_REPORT.json` 顶层 | 当前报告没有自然质量证明 | 不能描述为“已高胜率、等待显示” |
| 证书、qualification、confirmation、evolution、主链决策/撤销数组为空 | R30 `chapters[0]`、`[1]`、`[7]` 的 `data` | 自然认证到主链没有足够成功产物 | 数组为空不证明所有历史任务从未启动 |
| calibration、modelCalls、runtimeCaches 为空；neuralTraining 为 null | R30 `chapters[5].data` | 本次报告缺真实训练与推理产出证据 | 不能把 null 改成 0 后宣称正常 |
| 24 个 family 首阻断为 `BLOCKED_WARM_NUMERICAL_ABI_PRESERVED`，12 个 expert 为 `BLOCKED_EXPERT_ABI_PRESERVED` | `chapters[11].capabilityRepairR32.rows` | 数值身份迁移形成成片堵塞 | 不等于这些算法统计上失败 |
| 20 个 matrix 为 `INPUT_UNAVAILABLE`；18 个 researchChart 为 `NO_RECORDED_OUTPUT`；29 个 legacyChart 为 `OUTPUT_UNKNOWN` | 同上 | 要逐项修生产链与证据读口 | 不能用相邻表总行数填充所有图 |
| independentFuture：activeCertificates=0、futureActs=0、futureHits=0、independentFutureIssues=0、policyObjects=0；evolutionAttempts=50 | `operatingSummary.independentFuture` | 尝试数 50 未产生相应自然产物 | 50 次尝试不等于 50 次有效进化 |
| Foundry、replay dataset、evolution 有 writer hold / dependency blocker | `operatingSummary.tasks` | 写入/依赖恢复是前置工作 | 不能靠添加更多搜索任务解决底层堵塞 |
| r27-atomnet 标为 RUNNING，但三报告 checkpoint 的 trained-through 均为 `202611011439` | 任务与 checkpoint 投影 | 状态名不能证明持续训练，需端点差分 | 不能因此断言完全未进行任何未提交计算 |

报告的 `inputArtifactBytesVerified=false`、`numericReplayExecuted=false` 必须保留。报告提出的修复方向是待验证假设，不是已验证数值根因。尤其“父状态保留”只证明库里记录存在，不证明旧权重字节可被当前 ABI 解码。

### 1.1 已找到明确代码断路

- `strategyv2/WarmCandidateR26.java` 对旧 ABI 候选写入 `BLOCKED_WARM_NUMERICAL_ABI_PRESERVED`；后续 `parent()` 只接收 `WAITING_WARM_FORK`、`EXPIRED_RETENTION` 且当前数值 ABI 的对象。
- `strategyv2/ExpertContinuationR26.java` 同样把旧对象放入 `BLOCKED_EXPERT_ABI_PRESERVED`，后续只接收当前 ABI 的 `WAITING_EXPERT_CONTINUATION`。
- 095 没有消费这两类阻断态的冷启处理器。`RepairDiagnosisR32` 提示使用“已验证的冷启重生事务”，但相关原研究 owner 中不存在该事务。
- `strategyv2/LiveShadowsR27.java` 的 `ABI_REBIRTH` 处理的是 `r27_live_shadow`；它不会自动消费 `r26_candidate` / `r26_expert_experiment`。同一词汇不能当跨表修复已经完成。

因此 B06 增加的是**旧 ABI 研究对象 → 新身份、零学习/零证据子对象**的实际桥接；它不是权重转换器，也不授予自然质量。

## 2. R952、R26/SU2、R27 到底谁负责什么

### 2.1 当前 095 真实所有权

| 族/owner | 输入与计算 | 真实产物 | 后续入口 | R34 决定 |
|---|---|---|---|---|
| R952 AST1/2 在线学习 | `ResearchDirectorR952.capturePredraw` → PIT；结算终态后 `onSettlementCompleted` → `ResearchOutboxR952`；`prepareTrainingLabel` 在 writer 外更新模型 | `r952_candidate_predraw`、`r952_artifact`、`r952_learning_cursor`、outbox ACK | `FrozenCandidateAdapterR958`、R967 evaluation、R929 certificate | 保留实际 owner，修等待原因/公平调度/按期进展；不得用 R27 产物代计 |
| R952 AST3 / CP12 开发训练 | `DevelopmentProductionCP12.prepare`；`ResearchTrainingHookRegistryR952.drainTrainingSlotCP11` 的 TRAIN_CANDIDATE / FREEZE_CANDIDATE | `research_training_protocol_v2`、输入 CAS、训练 checkpoint、开发 receipt、frozen candidate | R958/R967 独立 Future 与 `research_formal_observation_cp12` | 主动创建并训练可执行图候选；与旧在线 outbox 区分 |
| R26/SU2 家族与 expert | `FoundryCampaignR26`、`ExpertWorkshopR26`、`LivePoolR26`、`ModelLiveLoopSU2` | `r26_candidate`、checkpoint/paired experiment、SU2 live decision、qualification | `CertificateSU2`、`MainlineBridgeSU2`、`MainlineAdapterSU2` | B06 冷启修复后复用现有真实训练；不跨 ABI 继承样本 |
| R27 新研究池 | `RuntimeR27`、ReplayDataset、AtomNet、EvolutionLoop、LiveShadows | r27 policy/live shadow/frozen forecast、qualification inbox、e-process、certificate | `MainlineFreezeProofR31` → `MainlineAdapterR31` | 保留受控独立研究族；修数据与写入依赖，使用永久多重比较预算 |
| 统一消费 | `UnifiedResearchChoiceR932` | 每 target 的唯一选择、typed proof、rejection 原因 | `CertifiedPredictionR974` → `DecisionGateEngine` | 唯一 REAL 决策入口继续保留；不新增第二个出手 owner |
| 唯一结算 | `SettlementEngine` 的 R36 终态 | 单 target settlement + REAL/OOS 结果 | 终态后研究 hook | 研究方只消费已提交终态，不重新结算、不修改已发生结果 |

`r952_candidate.future_n` 包含在线训练角色的未来标签，不能直接当独立冻结认证样本；原 owner 已用 `SEALED_FUTURE_N_UNCHANGED` 区分。必须读取对应协议的正式观察表。

R952 的 `capturePredraw` 明确排除 `AST3:%`，所以 **AST3 的 outbox 为零本身是合法现象**；应查 CP12 训练协议、训练队列、开发 receipt 和冻结对象。相反，一个 AST1/2 NURSERY 候选在合法输入和持续新期均存在时仍没有 predraw、artifact、cursor，必须顺链找第一个缺失边，不得用 AST3 或 R27 活跃掩盖。

`ResearchDirectorR952.pulse` 已有 TRAIN / POST / MATRIX / RESEARCH / EVOLVE / IMPORT 六槽；TRAIN 内持久轮转 LEGACY_OUTBOX / TRAIN_CANDIDATE / FREEZE_CANDIDATE。`ResearchPredrawCaptureR923.captureTargetWithinR927` 调用 R952 capture 与 frozen capture。`ResearchSettlementHookRegistryR952.dispatchCompleted` 调用 R952 settle，随后 `StrategyRuntimeSU1.completed` 接 R26/SU2。这些是复用入口；不再制造互相竞争的全局循环。

R27 也已经有桥：`UnifiedResearchChoiceR932` 的 `R27:` 分支读取 `MainlineFreezeProofR31`，构造 `R31-R27-*` typed authority；R974 对应接 `MainlineAdapterR31`；它只重现已冻结 ACT，不重选位置和 K3。SU2 用 `CertificateSU2` 和 `MainlineAdapterSU2`；R952 frozen 用 R958 recipe 与 R929 的正式证书。多类证据生产者不等于多个 REAL Gate。

### 2.2 B06 必须落地的 owner 清单

增加统一**只读清单/投影**时，每个对象必须携带：

```text
producer_family = R952_ONLINE | R952_CP12 | R26_FAMILY | R26_EXPERT | R27_POLICY
producer_id, immutable_program_hash, numeric_rule_hash, lifecycle_state
source_table, source_primary_key, producing_task, expected_next_owner
last_input_issue, last_committed_issue, last_artifact_hash, next_due_issue
blocker_kind = INPUT | SCHEMA | WRITER | RESOURCE | PROOF | QUALITY | NONE
blocker_source_receipt, blocker_object_specific, observed_at, evidence_role
```

投影规则：只有真实原表存在才显示产物；不得创建镜像 r952_candidate 去代表 R27；不得把 r26_attempt 计入 r952 数值训练；不得把 CP12 开发训练样本计入独立 Future。按 `producer_family + producer_id + numeric_rule_hash` 联接图表与故障诊断。历史对象仍能看，但显式展示 `HISTORICAL_ABI`，不进入当前运行分母。

如果未来希望 R27 与 R952 共用一个新自然证书协议，必须另建经过统计复核的新协议及新身份，自零开始；本次不能把两族历史 Future 合并成一个更大的样本数。

## 3. 必须固定的统计与业务语义

每次只选择**一个位置**和该位置的 **3 个互异数字**。不追期、不加倍、不把多个位置的并集合并成命中。真实命中为该目标期该位置的实际数字是否落在冻结 K3 内。选择、预测向量、阈值、模型、随机种子/状态、证书与 target 在开奖前绑定；结算只比对冻结对象。

在独立且均匀的 10 选 3 条件下，单次基线为 30%。这是条件模型下的数学基线，不是对实际数据机制的无条件断言。任何“高胜率”都必须明确是研究回放还是合法独立 Future；不能把回放最优者的 60% 当未来期望。

覆盖率必须同时给出：

- calendar opportunities：按预登记日历应被观测的期数；
- valid input opportunities：合法及时输入的期数；
- frozen opportunities：确实在截止前冻结的期数；
- settled observations：已完成权威结算且输入证明合格的期数；
- ACT、HIT、ABSTAIN、MISSED_DEADLINE、GAP、INVALID_INPUT；
- 业务覆盖率 `ACT / calendar opportunities`，诊断覆盖率 `ACT / valid input opportunities`，并列显示，不偷换分母。

最低 15%、目标 30% 是覆盖要求。不能用放宽证书、事后补冻结、删掉难期来达成。达不到时保留 `COVERAGE_UNMET`；质量不足时允许持续研究并保持主链 HOLD。覆盖优先级绝不能强制不合格资产变 REAL。

### 3.1 真正的逐期循环

```text
对每个合法 target t：
  读取最大可见 issue < t 的 PIT 快照；验证 fetch/open 时间、版本与输入 hash
  用截至 t-1 可用的状态计算概率、位置、K3、ACT/ABSTAIN
  截止前原子冻结：program/model/input/gate/certificate/target/概率向量
  等唯一 SettlementEngine 提交 t 的 COMPLETED 终态
  原子消费该终态：label_receipt + 各证据角色的合法观察 + 持久训练任务
  在 writer 外从 frozen features 与旧状态做真实数值更新
  CAS 原子提交：新状态/产物 + 数值结果 receipt + 训练 cursor + ACK
  后台按预算做失败分析、候选生成、开发比较、自然资格累积
```

**预测必须先于标签；评分使用更新前预测；训练标签必须最后进入模型。** 做历史回放也保持这一次序。未来冻结模型的认证样本不得回流训练该冻结模型或调节其阈值；在线研究流可以训练持续学习者，但不能同时冒充静态冻结验证集。

一个预登记的在线学习协议可以包含确定性逐期更新；它的身份绑定 updater、数据可见边界和调度。更改特征/损失/架构/阈值学习方法/ABI 会产生新协议身份与新的自然预算，不是旧证据的继续。R952 frozen 合同现有为固定模型，保持其现有隔离，不把 R27 的在线语义套过去。

**在线研究状态更新与自然认证对象须分开。** 在线实例在预登记 updater、特征、损失、优化器、学习率调度、PIT 边界和更新顺序不变时，合法逐期 SGD 只推进 `stateHash`/训练 cursor，不应每次清空在线协议观察账本；这些观察不能充作固定 frozen 的 sealed Future。新训练版本出现后，旧 R952/R958 frozen 实例保持其不可变 `modelHash`，继续原独立观察直至端点，避免“每次训练都终止实验、Future 永远凑不齐”。

R27 的 `SequentialQualificationR27.State` 按固定 policy/globalIndex 观察，并不在每次状态推进时自动新建试验。但 **095 证书没有因此获得任意滚动权重的通行权**：`MainlineModelBindingR31.capture` 绑定 `shadowBiasSha256`、`shadowThroughOrdinal` 和 AtomNet `publicationId/stateHash/modelHash/weightsHash`；`stillCommitted` 精确复核。若未来把连续自适应学习协议本身作为认证对象，需要新协议版本定义确定性的状态转换链、可预测的更新调度和逐期 model receipt，以新试验身份从零自然评价，并增加对应精确消费适配。不能放松现有 V1 的固定绑定、门槛和 typed proof。本次保守交付路线是：冻结已取得合法认证的版本供主链使用，在线研究实例独立持续训练。


## 4. B06：冷启重生完整实施工单

### 4.1 实现与调用点

新增三个类：

- `strategyv2/ColdRebirthContractR34.java`：确定性 lineage、预算、零继承检查与准确插入 SQL。
- `strategyv2/ColdRebirthSchemaR34.java`：2 表、3 个不可变触发器、2 个局部索引，准确枚举与校验。
- `strategyv2/ColdRebirthR34.java`：一次处理一个父对象，准备数值和文件在 writer 外，fence/CAS 后同一事务提交新 source、新子对象和不可变 receipt。

精确修改：`LifecycleSchemaR31.definitions/Definition` 由既有 MIGRATION owner 加入七对象；`RuntimeCoordinatorR26` 登记 `cold-rebirth-r34` P2 任务并分发；`TaskContinuationR26` 调用 handler；`ModelReplaySU2.build` 校验并从数值工厂参数副本移除 `_cold_*` 元数据，policy identity 仍保留该 lineage。否则其严格参数个数检查会拒绝冷启子对象，这是必须同批交付的第四个文件改动。此外，`ResearchTaskRegistryR30R1.ADDITIONAL` 登记 POLICY、training=false；birth 事务显式写原 `ResearchStoreR30R1.commitProgress`，以不可变出生事实计量，不把 birth 叫训练。`RuntimeCoordinatorR26` 的健康快照从当前主表读取待处理父对象数，缺 schema 返回 unknown，不写心跳伪造进展。`FoundryCampaignR26`、`WarmCandidateR26`、`RetentionR26` 同批加入 active expert 源与零训练后继输入的元数据保护。合计八个原文件。运行 owner 不自行执行 DDL。完整差异在 `learning_payload/patches/B06_cold_rebirth_integration.patch`，对 095 原提交 `git apply --check` 已通过。

### 4.2 单位与原子性

幂等键为 `(source_kind, source_campaign, source_id, target_abi)`。同一父对象在同一目标 ABI 最多一个出生 receipt。子 campaign 基于协议 + 父身份 + 新 ABI + 新快照 hash 构造；候选 policy 使用原 `ModelGenomeSU2.identityR26` 生成，明确去掉全部 `_warm_*`、旧 `_birth_*`、旧 `_cold_*`，重新绑定 fresh snapshot 与 cold lineage。不是手改一个字符串假装新身份。

父对象字节、原状态、旧 checkpoint、训练/验证命中和原证书保持原样。新候选 `cursor=0`、`state=NULL`、`state_hash=''`、`checkpoint_hash=''`、开发/验证计数均 0。expert 用当前 `ExpertExperimentR26.spec` 构造新的两个 `LiveStateR26`，验证两侧 samples=0，再序列化；不解码旧 pair。其后仍由 `ExpertWorkshopR26` 做真实配对训练。ABI 重生的类别是 `ABI_MIGRATION_NOT_QUALITY_IMPROVEMENT`；零训练 source 续生为 `UNTRAINED_SOURCE_RESTART_NOT_QUALITY_IMPROVEMENT`。

同一 writer 事务按如下顺序提交：校验 owner fence → receipt 去重 → 父快照 CAS → 最新数据 generation/保留边界 → source → 新对象/合法身份 → receipt → 清理该父重试记录 → 再校验 fence。任意失败回滚全部数据库写入；文件 CAS 准备在事务前，未引用文件由既有维护 owner 回收。

**源到期且尚未训练的恢复也已实现。** 旧 ABI 父的首次重生不修改父对象。之后若这个新 child 仍为零训练、零 checkpoint，且 source 到期/缺失或 ABI 已变，handler 会把该 child 当成新的 parent，重新捕获合法 source、产生新身份和第二个不可变 lineage receipt。它只在成功提交新后继的同一事务中，将旧零训练 child 的运行状态标为 `COLD_UNTRAINED_SUPERSEDED`；旧哈希、数据、证据和原 birth receipt 不改。每个 parent/target ABI 最多一个后继。已经训练的对象仍走既有 warm continuation，不能借此清零自然或训练账本。

清理保护覆盖三处真实 owner：WarmCandidate 的 prepare/action=3 删除、Foundry 的候选/source 清理、Retention 的无源 expert 删除。没有后继收据的零训练 cold child 元数据暂不删除；存在 RUNNING expert 的 source 不得按“无 candidate”误删。原 5000 窗口 TTL 仍清空过期 raw snapshot，不无限保留原始数据；新后继提交后旧元数据保护可释放。不可变 birth receipt 只证明出生，不能被报告为已训练。实际 admission 只计算当前 ABI 且 source 有效的可训练 child，避免 16 个 snapshot 同时过期后把续生容量永久堵死。

局部 poison 不堵整个队列：只为该父对象写失败原因与 30 秒起、最高 30 分钟的退避，其他 due 父可继续。资源/写入/任务取消走原 owner 资源等待路径，不能被计为“质量淘汰”。保留旧对象不意味着永远重复挑同一毒性输入。

### 4.3 资源边界

每 slice 最多 1 个新子对象；最多 16 个当前 ABI/source 有效的可训练 reborn child；候选与 expert 交替挑选；source 使用现有最近最多 4096 条合法保留数据，最少 64 条，ordinal span 小于 5000；snapshot 最大 176 KiB。候选准备预留 2 MiB，expert 编码 workspace 预留 32 MiB；数值模型另走既有模型预算。工作量有界，不宣称序列化一定在固定毫秒内完成，仍需合作取消与 Android 真机测量。

新专家两模型的构建/编码/关闭必须纳入内存测试，特别是“左侧已创建，右侧因资源失败”的半构造路径。此项是 **B06 启用前必须完成的 Pair 生命周期修复**：B06-01/02 必须显式修改 `ExpertExperimentR26.Pair` 与相关 `LiveStateR26` 构造器的失败释放，并加入“左成功、右失败”的资源负例；当前参考实现未补改这两个原构造器，因此此门仍未通过。正常构造成功后的临时 pair，本实现会在 finally 关闭两侧模型；不能把工作集 reservation 当实测 PSS。

### 4.4 R967 / R82 语义与迁移

加入类和改动调用方必须进入 `release/research_dependencies_r967.json` 的准确路径/角色。冷启身份、数值构造与注册逻辑属于数值/运行契约，不能标成 PRESENTATION 逃避规则改变；纯报告文件可按其实际用途标 PROVENANCE。保持 R81 固定 baseline 原字节，按 `scripts/r967_semantic_identity.py:invalidation_r82` 要求，列齐所有非 provenance 的 controlled_changes 与 new_role_reviews；新规则 `predecessors=[]`，不继承旧 qualification。

最终由生成工具计算新 training-rule、source provenance、call index 和受控文件清单，再执行对应 `--check`。禁止编辑生成哈希让旧证书“过关”。B06 后像以原 095 为审阅参照；在累计 B01–B05 分支上应合并小补丁并重新生成清单，不可覆盖整个旧后像文件。

### 4.5 已执行与必须继续执行的验收

已执行：纯 Java 合同 33 个断言；使用原始 095 DDL + 新实际 SQL 的原生 SQLite 44 项检查，包括：可重放身份、旧对象不改、样本不继承、先失败回滚、真实另进程中途退出回滚、重复不可变 receipt 被拒绝、expert 默认零计数、重开连接持久化，以及真实清理 SQL 的 active expert source 保护、TTL 后保元数据不保 raw、缺源零样本保留、同 ABI 单后继与 16 个 source 全部过期恢复容量。参见 JSON 与 log。另用实际 JDK compiler 对 3 个新类与 8 个后像做语法 parse，共 11 文件通过；这不等于 Android 类型检查。编译使用环境 JDK 21 的 compiler，`-source 17 -target 17`；该环境缺 `ct.sym`，不能使用 `--release 17`，也不能宣称验证 Android API。

必须在施工分支继续做：完整 Android 编译；真实 MIGRATION 路径冷/热升级；真 SQLiteDatabase writer/fence；model 参数与 Hash 完整验证；任务取消、内存不足、磁盘满、快照 generation 改变；成功 birth 后 Foundry/ExpertWorkshop 真实 cursor 前进且状态可跨进程恢复。运行前后按 child 主键联接，不能只报告总量增加。

## 5. B07：真实训练与反馈，不用计数充数

### 5.1 改动清单

| 工作单 | 原 owner | 必须产出 | 失败分类 |
|---|---|---|---|
| L07-01 | `ResearchDirectorR952.capturePredraw` / `ResearchPredrawCaptureR923` | 按 candidate、target 缺边清单；模型/输入 CAS 成功的冻结 | 没有未来窗口、输入过迟、数据缺失分别记录 |
| L07-02 | `ResearchOutboxR952.consume` / `commitTrainingLabel` | 原子 artifact + cursor + ACK + POST intent；before/after 数值 witness | CAS 失效重算；写失败等待；非有限数值才是算法数值故障 |
| L07-03 | `DevelopmentProductionCP12` / `commitCandidateBatchCP11` | AST3 真特征块、数值 batch、validation receipt、FREEZE 队列 | 队列等待不叫训练；已准备未提交不叫进展 |
| L07-04 | `FoundryCampaignR26.step` / `ExpertWorkshopR26.step` | reborn child 实际样本处理与新 checkpoint | source 到期走原保留协议，不重放已泄漏样本当 Future |
| L07-05 | `AtomNetSessionR30` / `RuntimeR27` | trained-through、epoch/cursor、权重 bytes hash、loss 记录的连续差分 | RUNNING 且 durable artifact 不动达到预算窗口时标 STALLED |
| L07-06 | 原 POST / feedback owner | 明确失败簇与建议动作、关联具体 frozen observation | “未执行”“资源不足”“质量差”三类不可混淆 |

095 已有的可复用实现：Foundry 每样本先预测后评分再 update，序列化完成才提交 cursor；单 slice 最多 16 样本/约 160ms 计算预算。Expert 既有配对两侧协议与约 8 样本/150ms 单元。R952 已有在 writer 外 `model.update`、writer 内 prepared fingerprint/CAS、artifact+cursor+ACK 同事务。R34 应修通这些事实链并补缺失 witness，不能再另建计数器循环。

### 5.2 数值 witness 契约

新诊断 receipt 需要绑定原实际 artifact；若新增表，唯一写者是对应模型提交 owner，仍通过同一个 DatabaseCoordinator。不能由 UI 或导出程序回填。

```sql
-- 设计 DDL；施工时纳入既有 schema owner 及准确依赖清单。
CREATE TABLE r34_training_witness(
  artifact_hash TEXT PRIMARY KEY,
  producer_family TEXT NOT NULL,
  producer_id TEXT NOT NULL,
  numeric_rule_hash TEXT NOT NULL,
  source_receipt_hash TEXT NOT NULL,
  old_state_hash TEXT NOT NULL,
  new_state_hash TEXT NOT NULL,
  first_issue TEXT NOT NULL,
  last_issue TEXT NOT NULL,
  unique_labels INTEGER NOT NULL CHECK(unique_labels>0),
  model_updates_before INTEGER NOT NULL,
  model_updates_after INTEGER NOT NULL,
  loss_before_sum REAL,
  loss_before_count INTEGER NOT NULL,
  finite_output INTEGER NOT NULL CHECK(finite_output IN(0,1)),
  update_kind TEXT NOT NULL,
  parameter_change_norm REAL,
  state_payload_bytes INTEGER NOT NULL CHECK(state_payload_bytes>=0),
  numerical_duration_nanos INTEGER NOT NULL CHECK(numerical_duration_nanos>=0),
  created_at INTEGER NOT NULL
);
```

`parameter_change_norm=0` 不是必然失败：零梯度、饱和、固定图等可以合法无参数改变。`numeric_updates` 增加也不是充分证据：必须有真实调用、输入 fingerprint、数值前态、后态/产物与收据一致。固定图 `update_kind=FIXED_GRAPH_NO_TRAINABLE_PARAMETERS`，绝不能伪报梯度训练。loss 用更新前概率，训练损失与独立验证损失分别记录。

审核SQL示例：

```sql
SELECT c.candidate_id, substr(c.ast,1,5) AS ast_kind,
       count(DISTINCT a.artifact_hash) AS artifacts,
       max(a.numeric_updates) AS max_updates,
       l.trained_until_issue, l.artifact_hash
FROM r952_candidate c
LEFT JOIN r952_artifact a ON a.candidate_id=c.candidate_id
LEFT JOIN r952_learning_cursor l ON l.consumer_id=('R952:'||c.candidate_id)
GROUP BY c.candidate_id, ast_kind, l.trained_until_issue, l.artifact_hash;
```

该查询仅对应 R952 在线 cursor；AST3 必须继续查 `research_training_protocol_v2` 与 CP12 开发 receipt/frozen，不能以 l 为空判失败。冷启的 SQL 核查用不可变 receipt 精确 join child：

```sql
SELECT b.source_kind,b.source_id,b.target_abi,b.child_id,
       c.cursor,c.status,c.state_hash,c.checkpoint_hash,c.dev_n,c.val_n
FROM r34_cold_rebirth b
LEFT JOIN r26_candidate c
  ON b.source_kind='CANDIDATE'
 AND c.campaign_id=b.child_campaign AND c.policy_hash=b.child_id;
```

### 5.3 样本角色与反馈

输入 manifest 包括每条 issue 的 event/open/fetch/available 时间、source_revision、max_visible_issue、缺失掩码、训练与验证边界；`max_visible_issue < target` 是硬条件。数据更正触发旧观察撤销与相关证书复核，不能替换原冻结输入后仍声称同一实验。

CP12 原有训练窗 300–1200、验证 120、embargo 与注册期顺序约束应保留；它是开发筛选合同，不是自然证书。缺乏足够历史时 WAIT，禁止拼接未来。历史保留不足时报告实际可用跨度；不能用重复样本把 64 变 4096。

反馈最少分：概率过度自信、K3 边界错误、覆盖率不足、单一块退化、缺失输入、上下文切换、教师共错、资源违约。每类携带分母、样本区间、版本、可复核观察主键；没有原观察就 `NO_OBJECT_SPECIFIC_EVIDENCE`。反馈只提出新实验，不直接改已认证模型。

## 6. B08：策略、算法、公式如何真正创造

### 6.1 创造的最小定义

一个新候选至少具有可执行、受类型约束的程序，以及能对其假设作比较的实验：

```text
hypothesis_id, parent_ids, failure_cluster_receipts
candidate_program_hash, semantic_hash, numeric_rule_hash
typed_diff, rationale, falsifier, control_program_hash, ablation_program_hash
allowed_inputs + max_lookback + lag_constraints
train_protocol_hash, selection_boundary, planned_validation
resource_budget, creation_seed, novelty_kind
```

“换名字”“修改备注”“seed 随机不同”“增加 generation 计数”“对同样预测再取一个 hash”不计结构创造。超参数搜索可以是真实搜索，但单独标为 PARAMETER_SEARCH，不夸称发明算法。新公式只有编译并实际执行、有输入/输出/失败/性能证据后才从 PROPOSED 进入 EXECUTED。

建议复用既有 `CandidateAstR952` / `MoleculeCompilerR952` / `EvolutionControllerR952` / `DevelopmentProductionCP12`，以及 R26 family 的实际模型实现，不建一个不接原训练 owner 的“智能创造服务”。每条 proposal 经编译、资源检查、去重后，必须原子提交 candidate + training protocol + training task；不能只生成字符串后停止。

### 6.2 有理由的搜索空间

| 方向 | 合法模型/修改 | 需要验证的机制 | 反证与控制 |
|---|---|---|---|
| 概率基线 | 带平滑的单位置频率、滚动/指数衰减计数 | 条件分布是否有可持续偏离 | 均匀与固定频率基线，同期评分 |
| 多尺度 | 有缺失掩码的 8/32/128/512/2048/5000 过去尺度 | 长短尺度互补是否真实 | 拿掉长尺度，记录每尺度实际可用数 |
| 状态 | HMM / 显式持续期 / change point | 状态持久性与变化是否比独立噪声解释更好 | 不使用未来平滑标签、事后阶段标注 |
| 交互 | 稀疏边/超边/匹配结构 | 跨位置约束是否改善单位置概率 | 保持唯一主动作；去掉交互做消融 |
| 公式 | typed lagged feature AST、受界状态宏 | 可解释的过去输入关系是否存在 | 禁用 lag=0、issue-id 记忆、NaN/无穷/无界递归 |
| 集合读出 | 合法 Top3 / set head / soft branches | 改善 K3 命中而非只优化辅助 loss | 辅助 retention 不得当真实命中 |
| 残差 | 冻结父模型 + 有界残差 | 修复已确定失败簇且不伤其补集 | 父本、残差子本、去残差对照同期 |
| 融合 | 同期预测概率的校准加权或 contextual router | 互补且有质量的成员是否提高组合 | 原父1/父2/简单均值/新融合四路共同观察 |
| 教师 | 原件 hash 绑定的固定教师蒸馏 | 在资源更小前提下保持或提高开发效果 | 相同学生仅真实标签对照，不继承教师样本和证书 |
| 资源优化 | 缩小模型/特征缓存/确定性推理 | 是否在时限内产生合法冻结 | 不能将速度提升计为胜率提升 |

R26 的 E03–E14 实际已有这些配对实验 spec，可直接用于机制对照。其 labels 使用已曝光历史，输出只进入研究方向，不能直接签自然证书。优先补齐被阻断机制的真实执行、产物与 paired loss，不再增加没有输入的家族编号。

### 6.3 演化、淘汰、融合的约束

- 注册前做语义去重；结构变化再以固定只读探针集产生行为 fingerprint。相同行为/同模型应避免占多个独立自然检验预算；不同 seed 仍计一个真实多重尝试身份。
- 父母必须有可读原件与训练边界；一方缺失就 WAIT/REJECT_PARENT，不从描述文字捏造教师。
- 融合权重只由开发流学习。观察自然测试后改权重，必须新候选、新模型、新冻结端点；旧自然证据不能迁移。
- 两个都差的模型低相关，不等于高质量；必须比较 joint failure、left-only/right-only success、概率 loss 与同一日历收益差。
- 开发筛选采用预先定义的目标和约束：校准/适当评分、固定窗稳定、覆盖、成本。Pareto 结构用于保存不同权衡；不把不同统计协议拼成一个任意总分。
- ACTIVE 工作集有界，终身候选数无需硬上限；归档保留身份、父子关系、输入/数值证据摘要和判定。全局 alpha index 永不重用。
- `RESOURCE_RETIRED`、`DEPENDENCY_RETIRED`、`NUMERIC_INVALID`、`QUALITY_PRUNED` 含义分别保留；基础设施故障不能删除为“低质量”，质量差也不能无限伪装资源等待。
- 动态迭代由新证据触发：新开发块完成、旧模型可证退化、输入语义改变、固定资源问题。纯定时每分钟无意义随机变异不得成为主要创造机制。

## 7. B09：自然样本认证与稳定质量

### 7.1 复用与补强边界

R27 复用 `QualificationInboxR29` 的连续、幂等观察 owner，`SequentialQualificationR27` 的永久 global index/alpha，`CertificateGateR27` 原 V1。证书必须绑定实际程序、状态/模型、特征 ABI、阈值/校准、证据 digest、对照与签发/过期时点。

R952/CP12 保持原 R958/R967 frozen 与 R929 正式证书 owner，不能用 R27 e-process 数值作为无协议转换的替代物。统一报表可展示各族各自证明状态，但协议名/窗口/分母必须并列。

**跨族多重比较不能漏算。** R27 的 `ALPHA_ROOT=0.05` 与永久 index 只覆盖它实际登记的试验域；R952/CP12、R26/SU2 的另一个 0.05 不能直接并在一起宣称系统总错误率仍为 0.05。B09 的跨族 trial key 固定为 `(producer_family, protocol_hash, experiment_id)`，不可复用，预先分配总预算且各域分配之和不得超过系统预算。如果旧证书字段不支持跨域分配，须标“各协议独立证明，系统级合并错误率尚未证明”；新增合并协议需新版本与从零样本，不伪改原 Gate。

对于频繁查看结果，普通固定样本区间不足以支撑任意时刻停止。Howard 等给出 time-uniform confidence sequence 的方法论；当前 R27 已使用逐时 e-process，是正确复用方向。大量候选反复挑选最优者会使有限验证集过拟合，Cawley/Talbot 说明这种选择偏差并不限于某种模型。因此自然期、全局尝试预算和预登记边界必须独立于开发搜索。[S1][S2]

R27 现有上行使用 `u_t = ACT_t * (hit_t - 0.30)`，非 ACT 为 0；参数依赖过去观测。顺序资格证明、34% 点估计门槛、校准、稳定性和 paired controls 是不同条件；不能用其中一个替代其他。错过冻结不能补 ACT；GAP 的处理必须与原 inbox 日历协议一致，单独报告数据可用性，不可删除以改善覆盖或 streak。

### 7.2 现有“连续亏损不超过 6”的实际难度

095 的 `CertificateGateR27` V1 要求：自然 ACT≥250、命中率≥34%、ECE≤0.04、最差 720 块≥26%、最长连失≤6、语义等价、相对 I00 与 shuffled control 的 paired lower bound>0，并有 sequential qualification。

直接用仓库已有 `streakGatePassProbabilityR31` 同等递推可得，在**假设每次 ACT 独立同分布**、250 次 ACT 时，仅“最长连失≤6”这一项通过概率为：

| 假设真实命中率 | 单独 streak gate 通过概率 |
|---:|---:|
| 30% | 0.04698% |
| 34% | 0.47002% |
| 40% | 4.92972% |
| 50% | 37.30951% |
| 60% | 78.48339% |

这是数学习题式敏感性分析，不是对当前模型的胜率估计，也不是总体发证概率。它说明“稍优于 30%”与“长期最长连失仅 6”是非常不同的要求。不能据此偷偷删除 streak、重置开始日期、改写旧证书或保证短期一定通过。若未来要研究其他稳定性门槛，应以新版本协议预登记并让新样本从零开始；V1 保持原合同。

15% 覆盖下拿到 250 次 ACT 至少需要约 1667 个日历机会；30% 覆盖约 834 个机会，且未计缺失、失效、检验阈值等更多要求。时间和质量均不能以“装好后一两天必过”承诺。

### 7.3 自然晋级工单

1. 注册时冻结身份、评价角色、起始机会、控制策略、alpha index 与目标覆盖；同一对象不能反复注销重登获得较宽松预算。
2. 每期全量记录 ACT 或 ABSTAIN；独立累计 counter，不借用训练样本。
3. 对照同期开窗、同时间证明，比较 common calendar 的 `u_A-u_B`；未出手记 0，不能仅取两者都命中的子集。
4. 校准拟合、阈值拟合与认证验证分开；自然端点若用于选择新的校准器，后者是新对象。
5. 签发事务重新核对全部 digest、来源 revision、revocation、当前规则；ACTIVE 字段不能替代实际消费时再验证。
6. 失效和漂移只让主链暂停/降级或创建新研究对象；历史样本原字节保留，不能修改来恢复资格。

## 8. B10：唯一主链和实际应用验收

R34 不新增第二个决策引擎。`UnifiedResearchChoiceR932` 负责选择可被当前消费端重新验证的候选；`CertifiedPredictionR974` 绑定可执行 recipe/typed proof；`DecisionGateEngine` 是唯一 REAL BET 创建者；`SettlementEngine` 是唯一结算者。

必须把以下链逐键导出，而非只显示“证书 ACTIVE 1”：

```text
producer_family + producer_id
→ immutable policy/program + model hash + rule/feature/calibration hash
→ natural qualification receipt + certificate + expiry/revocation check
→ target choice_id + frozen input/model/probability vector + ACT
→ R974 proof hash + exact position + 3-number mask
→ DecisionGate decision_id / rejection reason
→ authoritative settlement_id + actual target/position + HIT or MISS
```

核心验收：候选 B 被选时，REAL 预测确实来自 B 的被冻结状态；排第一的不是 UI 重排后不同 K3；失效证书、补写冻结、错 target、状态 hash 改变都不能通过。一个 target 最多一条 REAL；研究方没有写真实结果的权限。没有合格对象时让原因可读，而非制造预测装作系统已实现收益。

当前 `UnifiedResearchChoiceR932` 先检查正式 R929 证书，再 SU1，再 SU2/R27 fallback。应记录这个确定性优先级和跳过原因；R27 活跃但被更高优先级对象选中不算“未接入”。若要改变跨族选择规则，按新选择协议审查；禁止隐藏地按近几期命中排名改变真实选手。

## 9. 图表/报告与算法链的共同验收

每张图均附 `producer_family, producer_id, rule_hash, source table/key, window, n, generation`。训练图画真实产物的更新/损失/样本与耗时；创造图画执行后的 parent→child 和 novelty_kind；进化图画共同窗口配对差及淘汰原因；可靠度图用同一冻结版本的实际预测和标签，空箱为 NA。各版本不可拼成平滑“越来越好”曲线。

空态最少区分：`WAIT_INPUT`、`WAIT_WRITER`、`WAIT_TRAINING`、`WAIT_FUTURE`、`INPUT_INVALID`、`STALE_VERSION`、`NO_MATCHING_OWNER`。图表应给出第一个缺失上游事实、负责 owner、最后一次成功产物、下一触发条件。页面加载失败不能抹掉其他面板；若原件无法验证，显示 UNAVAILABLE 及原因，不画默认 0 或随机数据。

新增诊断必须能回答：哪个对象、哪一期、哪个原件、哪个任务、在哪一事务失败；当前有无待提交产物；重试会消费何种已授权事实；它和共享 writer 报错是否只是相关而不是已证实因果。报告导出只读，不得为了“有数据”运行训练或写研究证据。

## 10. 逐批验收矩阵

| 批次 | 最少可复核成功证据 | 必须拒绝的反例 | 未满足时状态 |
|---|---|---|---|
| B06 | 旧父原样、新 child 哈希/ABI/零证据、同一事务 receipt；R952/R26/R27 owner 清单 | 重试产生两个 child；旧权重/样本/证书继承；无源对象只登记计数 | ENGINEERING_BLOCKED |
| B07 | 真实标签→数值准备→状态产物→cursor/ACK 的每键一致；重启恢复后不重训已提交标签 | 只+1；先训练后预测；未来标签；未提交状态算进展；AST3 查错 outbox | TRAINING_NOT_PROVEN |
| B08 | 新 typed program 实际编译/执行/训练，控制与消融同窗，有结果/失败原因 | 随机 rename 算算法；共享旧证据；只看 best-of-many；低相关即晋级 | RESEARCH_ONLY |
| B09 | 独立 Future、永久试验预算、既有证书合同全条件通过且签发复核 | 回放抵自然；删掉难期；同窗选模型再认证；先写 ACTIVE | NATURAL_QUALITY_NOT_PROVEN |
| B10 | 唯一选择→typed proof→唯一 REAL→唯一终态逐键一致 | 第二 Gate；主链使用另一模型；不同位置合并命中；追期加倍 | HOLD_WITH_EXACT_REASON |

工程验收与自然质量验收分开。人工或合成 fixture 可以证明状态机、CAS、回滚、数学恒等式；不能证明当前自然市场/开奖数据中存在长期优势。新系统的合格结束状态可以是“全部 owner 正常运行，持续产生合规研究与失败分析，尚无达到全部质量门槛的策略”。这是可验证结论，不应用承诺高胜率替代。

## 11. 一手研究来源与使用边界

- **[S1] Howard, Ramdas, McAuliffe, Sekhon (2021)**，*Time-uniform, nonparametric, nonasymptotic confidence sequences*，Annals of Statistics。https://arxiv.org/abs/1810.08240 。支持任意时间持续查看所需的时间一致统计推断；不自动验证本仓库实现所有数学条件。
- **[S2] Cawley & Talbot (2010)**，*On Over-fitting in Model Selection and Subsequent Selection Bias in Performance Evaluation*，JMLR 11:2079–2107。https://jmlr.org/papers/v11/cawley10a.html 。支持将开发选择与最后评估隔离、约束多重搜索选择偏差；不说明某种神经模型必然提高命中率。
- **[S3] Han, Huang & Wang (2024)**，*Model Assessment and Selection under Temporal Distribution Shift*，ICML/PMLR 235。https://proceedings.mlr.press/v235/han24b.html 。支持变化环境下滚动评估与候选配对比较的研究方向；这里是工程设计推论，不声称仓库已经实现该论文算法或其理论保证。
- **[S4] Hyndman & Athanasopoulos**，*Forecasting: Principles and Practice* 的时间序列交叉验证。https://otexts.com/fpp3/tscv.html 。训练只使用测试时点之前可获得的信息，采用滚动预测起点；本任务的 PIT 额外加入 fetch/available 时间与修正版本约束。

上述文献只支撑评价方法，不构成“开奖序列可预测”的证据。
