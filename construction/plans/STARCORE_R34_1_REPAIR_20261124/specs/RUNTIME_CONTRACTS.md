# R34.1 运行链统一施工契约

本文件约束 B01–B05 的接口、数据库事务、错误分类和验收边界；状态是 **READY_FOR_IMPLEMENTATION / NOT_IMPLEMENTED**。方案结构自检不能证明 Android 编译、设备后台耐久或自然质量通过。本文件列出的初始预算是待测参数，调整必须附实际负载与结果。

## 1. 身份与版本兼容

应用底座必须为 `construction/r34-20261123@5562f9b19aa09f9f7d9e69ca085db56c537967fc`；现有 `main@4e1606e9c20c5f01a82cf495b8d39b2630c766f0` 是旧方案提交。新应用实现 commit 必须是该应用底座的后代。新方案上传 commit 与新应用实现 commit 是两个身份，不能互相填充。

新增构建输入 `STARCORE_R341_PLAN_ROOT`，保留 `STARCORE_R33_PLAN_ROOT` 原 R33.5 descriptor 以及 `STARCORE_R34_PLAN_ROOT` 原封存方案。新 descriptor 为 `control/PLAN_IDENTITY.json`：

| 字段 | 约定 |
|---|---|
| schema | `STARCORE_R341_PLAN_DESCRIPTOR_1` |
| planRevision | `R34.1` |
| application_baseline_commit | `5562f9b19aa09f9f7d9e69ca085db56c537967fc` |
| application_baseline_branch | `construction/r34-20261123` |
| repository | `gtt46461-star/starcore-public-build` |
| prior_plan_revision / prior_plan_commit | `R34` / `4e1606e9c20c5f01a82cf495b8d39b2630c766f0` |

`control/PLAN_SEAL.json` 持有 `manifest_sha256`。成员 manifest 排除其自身、seal、自检输出和交付ZIP，避免自引用；以实际封印工具定义的集合为准。应用内 `release/r341_plan_binding.json` 只能在方案上传后写真实 commit/seal；不可用占位 SHA 生成发布 APK。旧 R34 `identity` 内封印校验提取为新 `verify_seal`，版本命名校验仍属于各自版本分派，不能向旧校验器伪造 `R34-` 名称。

`advance_release_version.py` 保留锁和四文件原子 journal，R34.1 允许 B01–B12；版本号必须大于 manifest、现场设备、已发布版本及 577311122。封包器对旧033保留祖先证据，对现场122另验真实 APK 字节、包名、签名和版本；没有122原件只可交候选，状态 `COVER_INSTALL_UNVERIFIED`。构建密钥和授权密钥不进入仓库或日志。

## 2. 新增启动 journal 的完整接口

新增 `app/src/main/java/com/ethk3/novaonepro/StartupStageJournalR341.java`，B01 独立实现，禁止依赖 `DbStore`、SQLite 或 `DatabaseCoordinator`。调用点为 `AutoFetchService.initializeRuntimeAfterForegroundR32` / `reconcileResearchOwner` 和 `strategyv2.RuntimeCoordinatorR26.run` / `initialize` / `prepareStartupSchemaR34` / `registerSessionR34`。记录真实调用前后状态；没有执行的阶段不能造 END。

```java
// 所有方法均为 NEW；这里是施工接口合同，不是已编译实现。
StartupStageJournalR341 open(File appPrivateRoot, JournalLimits limits,
                             Clock wallAndMonotonic, RuntimeScope scope);
StageToken begin(String ownerGeneration, String attemptId, Stage stage,
                 String inputHash, Map<String,String> boundedContext);
AppendResult end(StageToken token, StageOutcome outcome,
                 CommitRef actualCommit, FailureSummary failure, long nextRetryMono);
AppendResult recordUncaught(StageToken token, Throwable error);
RecoveryResult recoverOpenAttempts(String currentBootId, String currentProcessEpoch);
JournalSnapshot snapshot();
ExportResult export(File destination);
```

`begin` 在被观察操作执行前由后台初始化线程同步写入小帧并 `force`；不能在主线程或实时提交热路径 fsync。帧包含 schemaVersion、sequence、CRC、release/sourceCommit、bootId/processEpoch、databaseIdentity（尚未打开时为 UNRESOLVED）、ownerGeneration、attemptId、stage、wallAt、monoAt、inputHash、outcome、reason、commitRef、causeChainHash、nextRetryMono。`StageToken` 只能被当前 owner 关闭一次；重试新 token 引用 parentAttempt，不覆盖首错。

存储采用单 writer 有界 framed spool，帧长度+CRC保证尾部半帧可截断；轮换生成新临时文件，`force` 后原子替换，并同步目录元数据。首轮预算总量256KiB、每帧≤2KiB、保留最近两个attempt的阶段与首个错误、未闭合阶段；另预留4KiB emergency槽。删去旧非首错摘要时记录 omittedCount 与最早保留序号。落盘失败不能返回“已记录”；返回 `AppendResult{durable=false, reason}`，emergency仍失败则下一次导出显示 `OBSERVABILITY_DEGRADED` 及最后已落盘序号。日志中的hash与boundedContext不得包含密钥、授权头或原始隐私设备标识。

启动阶段最少包括 OWNER_BEGIN、CHECKPOINT_PREPARE、CHECKPOINT_PRESERVE、RUNTIME_BASE、RETENTION_PLAN、RETENTION_UNIT、CHECKPOINT_POST_MIGRATION、UPGRADE_TRANSFORM、SESSION_REGISTER、OWNER_READY、OWNER_STOP。`preserveAfterMigrationR34` 的真实 owner 是 `ResearchRetentionStoreR30`，由 `RuntimeCoordinatorR26.prepareStartupSchemaR34` 调用，不能错改到 `StartupCheckpointIdentityR31`。

| Outcome | 条件与后续 |
|---|---|
| COMMITTED / VERIFIED | 真实提交receipt或只读核验完成；必须有对应证据引用 |
| WAIT_RESOURCE | 锁、内存、空间、受控预算不足；留 nextDue，可取消、可恢复，不计确定性BUG |
| WAIT_DEPENDENCY | schema/source尚未满足；列精确依赖owner，不无限热循环 |
| MISSING_SOURCE | 必需原件缺失；保留expectedHash与受影响owner，不能随机重建冒充恢复 |
| CONFLICT / BUG | identity、schema、确定性异常；同fingerprint有限重试后隔离相关owner |
| INTERRUPTED | BEGIN无END且原进程已结束；不倒推为成功或业务rollback |
| FATAL | 无法安全继续的实际异常；保留cause，不能吞成RUNNING |

最早停点决策：无 OWNER_BEGIN 检查开关/服务与owner创建；有 OWNER_BEGIN 无 RETENTION_BEGIN 检查checkpoint或前置折返；retention unit停住查锁/空间/源proof；迁移完成无SESSION_COMMIT查控制写和升级transform；session已有但cold任务缺查注册manifest；任务已有但表缺查 lifecycle ready/claim。当前报告无法唯一定位最早停点，施工必须根据新收据选择分支。

## 3. 其他 NEW 类的输入输出与调用点

下列类位于主包 `app/src/main/java/com/ethk3/novaonepro/`，只有最后一个在 `strategyv2/`。均由所属批次完整实现，不留空函数或未来默认成功适配。

| NEW 类 / 方法 | 输入 → 输出 | 实际调用点与失败语义 |
|---|---|---|
| RuntimeObservationScopeR341.capture / validateJoin / exportEnvelope | identity、clock、可选SQL snapshot ref → 不可变Scope；两个Scope → JoinResult | ServiceHeartbeatStore、RuntimeEventLedger；不同库/未定义跨boot比较拒绝，旧行LEGACY_UNSCOPED |
| RuntimeScenarioRecorderR341.captureInputEnvelope / exportFixture / redact | request/history/阶段时间及scope → bounded fixture ref | AnalysisRequestStoreR36、AutoFetchService；无完整原始输入标STRUCTURAL_RECONSTRUCTION；只出隔离重放数据 |
| WriteBudgetR341.forLane / canAdmit / beforeStatement / finish | WriteContext、固定deadline、rows/bytes/statements、age → Permit或ResourceWait；Permit+resolved outcome+actualCost →账目 | DatabaseCoordinator.runWrite与原事务入口；持锁前准入，语句间协作让步，提交确认后更新服务时间 |
| RuntimeReadSnapshotR341.publishAfterCommit / read / refreshAsync / invalidate | 已确认draw或progress token、scope → immutable snapshot；异步refresh返回generation | AutoFetchService控制tick、MainActivity轻读；旧epoch回调丢弃，忙为STALE/RESOURCE_WAIT，不变为0 |
| AnalysisInputEnvelopeR341.create / verify / encode / decode | ordered history/revisions、base/target、requestHash、fence、固定deadline → canonical bytes+hash | prepareAnalysisHistoryR364 / bindHistoryHash / runAnalysisRequestR36；身份变化需新合法attempt，旧输入不改 |
| strategyv2.CheckpointDependencyR341.resolve / affectedOwners / export | 受保护source refs与task依赖manifest → READY/WAIT_SOURCE/UNVERIFIED/CONFLICT +影响owner集合 | prepareStartupCheckpointR31 / initialize / claim；不生成替代权重，不取消真实强保护依赖 |

`RuntimeScope` / `ProgressReceipt` / `CommitRef` 等值对象优先做上述类的不可变内部类型，避免新增平行owner。所需JSON字段必须有版本号和缺失值语义；新增列/表由唯一迁移owner安装，读路径禁止CREATE。

## 4. 写者、进度、重试与幂等

`runWrite` 是唯一串行writer。锁外完成网络、文件扫描、decode/hash、history envelope及大对象准备；锁内只做输入身份、fence/CAS和必要领域写。`WriteBudgetR341.canAdmit` 在拿写锁前执行；它在B02已完整执行当前预算与deadline，不是NOOP。后续存储治理可以在该边界加growth额度适配，在 `finish` 后按已解析outcome入账；不得持写锁做census或等GC，B02也不等待后批存储策略才可验收。

初始切片配置：研究/故障/迁移目标持锁≤100ms，协作切片边界200ms，每片≤64行且≤64KiB，DDL每片一个对象；fault仍≤4条并服从绑定数上限。大于预算的单条SQL必须减少数据或另案改写，Java计时器不能保证中止已执行SQL；不能由第二线程强制endTransaction。固定canonical截止期不得延长。

| CommitOutcome | cursor / 下一步 |
|---|---|
| COMMITTED | 检查receipt，前移一次cursor；可发业务进度 |
| COMMITTED_DIAGNOSTIC_FAILED | 业务已提交，只重试诊断；不得重复业务 |
| ROLLED_BACK | cursor不动；按WAIT/依赖/确定性故障分类决定重试 |
| UNKNOWN | 保留owner/fence，先resolveCommitReceiptR31读回；明确前不并发重做、不算成功 |

幂等键至少绑定领域unitId、inputHash、generation/fence与语义版本。相同键不同payload为CONFLICT；不能用INSERT IGNORE掩盖内容不一致。事务内cursor和对应产物同提交；诊断outbox独立重发，不修改原事实。

`ProgressReceipt` 字段为 scope、lane、unitId、priorCursor、newCursor、commitId/outcome、committedAt、inputHash、outputHash。只发真实新增产物或cursor的提交。heartbeat/claim/START/维护扫描只能算activity。恢复有baseline token：REQUESTED→CONTROL_ACCEPTED→AWAITING_PROGRESS→RECOVERED或NO_PROGRESS/RESOURCE_WAIT，重复请求不重置失败年龄。

公平性沿用既有保留slot再加真实age；fault/migration现有约1s、research/control约5s尝试不能被描述为“本来毫无公平”。实时pressure必须有target/fence/expiresAt，过期失败不持续占用最高优先级。只有存在可用窗口且无临近freeze时，工程负载目标是ready研究30秒内至少提交一片；持续饱和要报SATURATED，不能无条件保证。attempt、rollback和同cursor重复写不更新lastCommitted。

## 5. LegacyPredraw 联合迁移合同及拒例

确切问题：`LegacyPredrawContextBridgeR915.freezeWithinTransaction` 第48行和 `settleDraw` 第211行都调用ensureSchema；现场在ensureSchema第263行卡于连接池。DDL现仅由 `DbHelper.ensureR906ResearchSchema` 安装，criticalContract不包含两表。

不可拆分补丁的对象是 `v57_legacy_predraw_context_r915`、`v57_legacy_postsettlement_context_r919`、`idx_r919_legacy_post_status`：

1. 先将原DDL权威定义及必要列/索引检查纳入critical owner，原表列和历史数据不变；research owner改为核验已安装对象。
2. 把两表和索引加到 `SchemaDomainCoordinatorR926.criticalContract`，设计明确的兼容增量迁移step。新增成员改变fingerprint，旧 `runStep` 同revision/step的COMPLETED可能跳过，必须新stepId和可核expected-before/after差异。
3. 核 `DatabaseCoordinator.ensureCriticalSchemaSingleFlightR64` 的MATCH快速路径、fresh路径、备份迁移、`prepareRepairIfDrift`路径及重入。仅允许这三对象的已知增量；其他critical漂移仍fail closed。修改revision必须经原release manifest生成器，不能手填Java常量或重写旧计划封印。
4. 完整ready receipt持久后才发布criticalSchemaReady。随后删除freeze/settle两个业务事务中的ensureSchema，改无DDL ready guard；未就绪返回明确DEPENDENCY并由迁移owner处理。

必测拒例：旧stamp MATCH但LegacyPredraw缺一表；旧R906_SCHEMA为COMPLETED；同名表错列；未知critical列漂移；迁移中途进程死亡；UNKNOWN需解析；fresh未启动R26也应能准备关键结构；热路径SQL trace不得再有CREATE/ALTER。单纯删除两处DDL不足以构成安全补丁。

**本交付不包含候选应用patch。** 原因是该变化同时触及指纹、迁移journal跳过规则和旧stamp兼容，未经实际Android迁移调用链验证不能安全提供“可直接应用即完成”的补丁。精确联合工单、前后条件和拒例已在B04-01定义，施工阶段须在上述组合通过后提交。不能把此限制改写为“问题已修复”。

## 6. 赛前输入、收据与旧R34抓取契约继承

| 原工单 | R34.1承接 | 必须保留的行为 |
|---|---|---|
| R34-B02-01 | B04-06 | RawHttpIdentityR34已存在不trim逻辑；原body的hash不可变；如有transport bytes hash须分域，不与解码字符串hash混同 |
| R34-B02-02 | B04-06 / B02-01 | 收据缺行不是payload冲突；同pending原件可在原writer幂等恢复，无原件WAIT_DURABLE_BINDING |
| R34-B02-03 | B04-06 / B04-04 | game+issue与完整排列幂等；重复100次一draw/一label；同键异结果隔离；迟到不回退最新watermark |
| R34-B02-04 | B04-02 / B04-06 | 最新摄取和补洞独立，gap可定位；MIN12/PREFERRED100/MAX240政策保留；short窗口合法可推进，long-window仍WAIT_HISTORY |
| R34-B02-05 | B01-02 / B05-01 / B05-03 | 健康网络1秒目标节拍、单在途、有界超时；sourcePublishedAt/receivedWall/receivedElapsed/commitElapsed/bootId齐备；无可信源发布时间标NOT_MEASURABLE |

`AnalysisInputEnvelopeR341` 编码必须确定性，history按issue及明确revision排序，只含≤base数据；同attempt不能重新读取补齐后的新集合来覆盖historyHash。源更正必须旧attempt终态+新generation，目标仍未开奖且deadline合法；已有canonical永不重写。`AnalysisSingleFlightR36` 的现存入口是 `submit` / `releaseAndTakeNext`，Token有阶段与checkpoint；不能凭空修改不存在的claim/release方法。

核心canonical与必要身份、授权、选中动作证据保持同事务；大的可派生准备移锁外，必要安全证据不能删。deferred沿现有snapshotHash、cursorVersion、coreCursor、nextActionOrdinal分片，FULL要求全部1200动作完整唯一且hash验证；1200动作仍属于一个target。无赛前预测不得开奖后补造，S0 shadow不能计为REAL。

## 7. 精确跨批修改范围

| 文件 | B01 | B02 | B03 | B04 | B05 |
|---|---|---|---|---|---|
| DatabaseCoordinator | outcome/观测字段 | writer预算、slot公平、读阻塞边界 | 复用既有迁移lane | critical迁移及canonical/deferred边界 | 只消费状态，不重写writer |
| AutoFetchService | 初始化取证/作用域 | fault控制tick、压力缓存 | R26 owner依赖状态 | history/request/deferred | service-owned loops、nextDue与恢复 |
| RuntimeCoordinatorR26 | 阶段journal | claim等待与age | 初始化/保留迁移/session/task/fence | 不新增第二owner | owner恢复消费真实进度 |
| MainActivity | 不负责业务 | singleflight读及stale | 无 | 无 | UI生命周期不驱动业务 |
| DbHelper / SchemaDomainCoordinator | 保持原结构 | 只预算观测 | 研究lifecycle由原owner | LegacyPredraw联合critical迁移 | 无 |

各批补丁基于上一批实际commit，不能分别从5562整文件覆盖共享类。出现相同方法跨批修改时先保留前批合同，再叠加当前改变。逐批Android构建能力由B01身份桥准备；B12只复验和封包，不能把B01必需构建前置拖到B12。

## 8. 本批验收与证据层

每个验收条目有固定ID、`level` 和初始 `execution_status=NOT_RUN`。ENGINEERING需要真实生产owner/SQL或数值类的可重复行为测试；源码grep、JSON字段存在、计划自检都不能直接签该层PASS。DEVICE需要该APK、具体设备/OS/权限、完整时段日志和业务commit；NATURAL需要真实未来独立样本与质量门，不能由上述两层代替。

工程试验可用结构重建fixture但必须显式标识，不能声称拥有并重放完整现场数据库。模拟输出不能进入生产canonical、future、REAL、用户图表或质量计数。每份执行收据绑定精确sourceCommit/sourceTree、工具/输入/log SHA256、命令、实际assertions和失败输出。

后台DEVICE场景包括息屏、退页面、任务移除、系统回收、断网恢复、重启和覆盖升级。24h/72h必须分别连续，禁止靠人工打开Activity补救后继续计无人干预时长；所有预计目标有明确终态，漏期不能从分母删。主动force-stop受OS限制如实记录。当前现场权限已准备好，仅约5.5分钟当前进程样本，24/72h仍NOT_RUN；本方案本身没有改变这些事实。
