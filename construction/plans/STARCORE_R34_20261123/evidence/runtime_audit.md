# R34 运行主链、数据库与并发根因审计

审计日期：2026-11-23。应用源码基线 `273641e838e0b8dd842b42a67a674e184942a30a`；三个报告均来自 `R33.5-B08-2026-11-22-577311095`。源码相对路径以下均从 `app/src/main/java/com/ethk3/novaonepro/` 起算。**当前只有检测报告及源码，没有本次真机原始SQLite、未截断HTTP原始体、系统trace或新APK执行结果。** 不把证据中的共享写入阻断，扩写成每个策略对象均已数值复演的根因。

## 一、核心结论与精确证据

1. **当前主链停顿是报告实证，后台线程是否终止不能由caught crash字段推定。** 最新 `reports/fabedd0/runtime-observation-heartbeatSummary.txt:1` 同时记录 `truth=RUNNING,alive=1`、fetch `202611230285`、analysis/settlement `202611230119`、`business=ANALYSIS_STALE`、pending=162、`research state=STALE`、restartRequest=53 / actualStart=22 / generation=22 / progress=0。`R30_REPORT.json#/snapshotWatermark` 的快照draw head为288；285是稍早服务观测，不应视为同一原子采样。`AutoFetchService.java:3694-3704,4043-4063` 已有in-flight状态和finally释放，不能仅看到ANALYSIS_RUNNING就断言忘记清理flag。

2. **writer准入背压被Deferred旧分类误判，已能给高置信修复。** `DatabaseCoordinator.java:916-919` 仅在research/non-reentrant/无age slot且 `WriterHoldFaultsR31.deferResearch()` 时抛 typed `DEPENDENCY/ADMISSION/ROLLED_BACK`。`WriterHoldFaultsR31.java:17-18,234-235` 的队列上限32、背压阈值24。`DeferredEvidenceWriterR36.java:33-40` 只把BUSY/OOM/yield等当暂态，未识别这个已有正常调度状态；`:200-207` 因而主动 `CrashRecorder.recordCaught(...,"F01_DEFERRED_MATERIALIZE")`，`:541-547` 又允许按普通attempts耗尽终止。报告 `runtime-observation-crashSummary.txt:10-41` 正好是这条调用链。`AutoFetchService.java:4420-4423` 另有catch/finally。**准确结论是“被捕获的背压异常计入crash记录，任务分类错误”，不能称“这份记录证明线程已崩溃退出”。**

3. **故障处理自身仍受研究背压闸口，构成可达的反馈阻塞。** `DeferredEvidenceWriterR36.java:534-578` 通过字符串 `R36_DEFERRED_FAILURE` 更新retry状态，命中 `DatabaseCoordinator.java:68` 的 `R36_DEFERRED* -> RESEARCH_DATA`；因此试图记录背压失败的事务也可能被同一闸口拒绝。更新仍有 lease_owner CAS，应保留。修复将失败控制回执、原有恢复查询列为 `RESEARCH_CONTROL`，继续走唯一writer和deadline，绝不把业务数据任意升级成critical。

4. **原始HTTP响应hash的语义不一致是源码确证、可复现缺陷。** `FetchLedgerR925.java:119-123` 对完整body算hash；`:406,412` 计算`safe(raw.rawJson)`，而`:508` 的safe会trim。首尾空格/CRLF等合法JSON空白足以让同一响应的ledger hash与绑定hash不同。真实失败响应体未包含在报告中，所以不能断言每次现场mismatch都由空白造成。载荷测试已使用原095 `V51ChecksumEngine` 在宿主复现旧表达式不相等，并验证R34精确body hash相等。

5. **“receipt未入库”与“payload真的冲突”被混成一个异常。** `FetchLedgerR925.java:137-141,222-225` 写失败只记状态并返回；pending begin/response暂留内存。`:434-435` 用 `!moveToFirst() || hash不等` 同时报 `HTTP_REQUEST_PAYLOAD_IDENTITY_MISMATCH`；并未在draw原子事务补齐同一request的原始pending receipt。报告三份 `runtime-observation.json#/fetchWriteError` 都为 `PAYLOAD:WRITE_REJECTED:BUSY_DEADLINE`，报告存在draw_save_error mismatch。R34必须区分缺原件/等待持久化/真实冲突；只有内存仍持有exact request/response且hash、源和时钟身份一致，才可在draw事务内提交receipt；没有原件不得用issue猜request、不得改hash为凑齐。

6. **writer deadline主要约束等锁，不会自动把长事务切成200ms。** `DatabaseCoordinator.java:897-900,925-940` 把deadline用于锁等待；`:949-963` task.call进入后没有通用切片或取消SQL。这并非SQLite可强行安全中断任意DDL的证明。报告存在BUSY，同时最新thread stack `runtime-observation-threadStacks.txt:391-421` 显示研究线程在持有原writer/atomic callable时扫描保护归档定义。修复须把调用体改为可恢复小单元，不能简单拉长等锁时间或在循环外加“预算=200ms”字样。

7. **迁移owner有journal，却仍包含整段重进和不分页扫描。** `SchemaDomainCoordinatorR926.java:1679-1683` 检测CLEANUP完成后仍调用完整spec.transform；`strategyv2/RuntimeCoordinatorR26.java:392-410` 把RuntimeSchema、SchemaR27、全保护表、warm publication、session更新和任务注册合到同一个startup transform。`ResearchRetentionStoreR30.java:145-167` 遍历所有保护表，逐表验证table_info/trigger/SQL，末尾`:176-204` 还有启动checkpoint保留。`:515` 初始化dependency每500ms重试。报告最新栈正位于`:151`的归档定义检查。这证明当前启动路径有长事务/重试风险；单一栈不能量化该函数贡献了所有延迟。R34需将“版本迁移”和“每次session注册”拆开，不能只判断`ready()`就跳过原件保护。

8. **creation错误传播有另一处类别丢失。** `DirectedResearchCreationR932.java:35-46` 捕获的是body内异常；writer在body前超时而返回fallback时，`failure[0]`仍null，随后包装成`R963_CREATION_INGEST_COMMIT_FAILED|WRITER_INTERRUPTED`，可能丢掉LOCK_BUSY和commit outcome。改用现有 `runAtomicWriteOrThrowF01` 保留typed语义。`:51-72` ingest游标和聚合本已同事务，必须保留；数值创造逻辑不在这项补丁中更改。

9. **部分基础闭环机制已经存在，应修通而非造第二条链。** `SettlementTaskStoreR36.java:126` 已在原draw结算任务入队事务生产 `RuntimeSchemaR26.feedbackWithinTransaction`；后者`:133-141` 按issue及draw source hash写draw label事件并拒绝冲突。它不应被“必须先有正式BET”卡住。`SettlementEngine.java:175-178` 无冻结原件会转 `NO_PREDRAW_CANONICAL_TERMINAL`，这应作为漏期/不可计自然样本实证保留，不能删掉或补造历史赛前冻结来让图表看起来有数。

## 二、因果链与不可绕过的约束

现场相容的链条是：长写事务/启动重复保护扫描 → fetch receipt或draw等锁超时 → current draw/analysis推进不稳定；长writer观察进入故障队列 →研究准入背压 → Deferred failure回执也走研究数据通道、错误被计crash/可能终止 →候选与训练/回放owner等待 →模型/未来冻结/证书为零 →图表空。链条中“错误分类、hash不一致、准入规则、扫描路径”有代码证据；具体每次等待的时间归因、每个模型是否有数值bug仍需同版本trace与原件验证。

必须保持：单数据库唯一writer；开奖与receipt/事件原子提交；同一target只有一个有效canonical owner；固定单位置K3；冻结只能用当时可见输入，不能事后补造；未知commit先查同幂等key回执；CAS校验owner/fence；diagnostic failure不能把已提交业务body再跑一次；回补历史只进入REPLAY/HISTORICAL；无有效证书时SHADOW可以有数据而REAL仍不放行。

## 三、B01—B05施工工单与方法合同

### B01-运行01：拆开schema迁移和每次session启动

涉及 `SchemaDomainCoordinatorR926.runUpgradeR31`、`RuntimeCoordinatorR26.startupUpgradeSpecR31/initialize`、`RuntimeSchemaR26.ensure`、`SchemaR27.ensure`、`ResearchRetentionStoreR30.ensureProtectedObjectsR31/preserveStartupCheckpoint`、`StartupCheckpointIdentityR31`。

- 将原startup transform拆成不可互相覆盖的`schema_install`、`protected_source_<table>`、`startup_checkpoint`、`session_register`单元。每个单元记录 migrationId、unitId、expectedDefinitionHash、beforeSchemaVersion、afterSchemaVersion、owner、committedAt和验证结果。单元提交后只在definition/version发生变化时重新审计；不得把上一版CLEANUP自动赋给R34。
- read准备阶段读取精确SQL定义/列/trigger，持有有界immutable plan；进入唯一writer后对`schema_version`、受影响源身份、journal版本重新核对。不一致退回PREPARE；不会边验证边默默修补未知trigger。
- 每次session只做当前owner/session/task的有界登记和lease invalidation。已有单元完整验证后，不再在每次重建worker时执行全保护表DDL和全表内容hash。
- 新装/升级仍逐表生成必要表和保护trigger；每个保护单元先验证旧原件、事务安装元数据、commit后独立验证。未验证单元保持对应能力BLOCKED，不阻断已完备的抓取/冻结/结算critical域。
- 验收：真实095 schema fixture +现有大表形状；新装、095覆盖升级、已经完成后10次重启；每次断电注入在PREPARE/DDL/COMMIT/ACK；旧原件hash不变；完成后的重复启动没有DDL写放大；critical写P95、最长hold记录实际值。
- 这里是涉及原件和启动协议的重构，现有局部载荷未冒充完成。禁止用`if (RuntimeSchemaR26.ready(db)) return`跳过验证。

### B01-运行02：给故障drain保留服务机会并让控制回执能够落盘

涉及 `DatabaseCoordinator.runWrite`、`WriterHoldFaultsR31.observe/drainBatchR33/deferResearch`、`RuntimeFaultArchiveB01`、`RuntimeFaultOriginalStoreR335`、`AutoFetchService.drainWriterFaultControlR334`。

- 继续唯一writer；critical freeze靠deadline优先；单个有界故障drain按原控制定时器获得机会，不依赖foundry/健康研究任务先运行。
- drain必须提交事件原件+幂等回执，独立read-back相等后才从内存队列释放；失败/未知提交保留事件。已提交事件不能再次作为新故障无限增长，应独立记录drain服务耗时/递归诊断数。
- protected fault配额满：先逐页拷贝原件到现有CAS、回读hash验证、才允许移动hot row；严禁丢原件/重置fault counters/清表。排空还需有可观察的oldestAge、eligibleRows、archivedVerified、pendingBytes和拒绝类别。
- 本载荷已修正Deferred失败/恢复操作的RESEARCH_CONTROL类别，并用窄分类恢复旧误终止任务。待施工配额与drain持续压力测试：故障到达率大于排空率应出现可解释退化，不全业务永久停摆。

### B01-运行03：重试分类和crash指标分离

涉及 `DeferredEvidenceWriterR36`、`FailureDispositionR31`、`CrashRecorder`、`ServiceHeartbeatStore`。

- 背压明确 `DEPENDENCY+ADMISSION+ROLLED_BACK` 才能不耗业务失败budget，原snapshot/hash/cursor不变，清lease仅匹配当前lease_owner。COMPLETE/UNKNOWN/END类结果不得走同一盲重试。
- crash报告分 `caughtFaultCount`、`uncaughtTerminationCount`、`systemExitHistory`、`processRestartCount`，每条带是否实际传播、boot/session/PID；继续保留原crash历史并标LEGACY_MIXED，不改旧事实。
- 验收需注入未知提交，先读同key receipt再决定重放；真正snapshot hash mismatch保留终态；错误处理写也BUSY时lease到期后能有界恢复。

### B02-运行04：统一HTTP身份与receipt→draw原子性

涉及 `FetchLedgerR925`、`ApiClient.rawSuccess/executeGoverned`、`DbStore.saveDraw`、`AutoFetchService`接收路径。局部patch已接线。

- transport body只按收到的UTF-8字符串精确hash；解析用trim可以，但不能改变identity。
- exact pending response补写和draw canonical event在同一外层事务完成。receipt hash已非空而不等，立即完整性冲突，不能UPDATE为新hash；缺pending且DB缺row，返回DEPENDENCY。
- requestId必须来自原transport对象，不能从最新issue/当前thread最后一个request推测跨线程结果。现实现ThreadLocal作为同步adapter路径条件有效；如未来改异步adapter，需把requestId作为RawHttpResult字段随对象传递。
- 故障测试：前后空白JSON；两次相同issue不同payload；complete和payload同时BUSY；排队128条溢出；process death before receipt。无原件时仅新抓取真实响应，不能回造first_available_at。

### B02-运行05：历史连续性与时钟/轮询分层

涉及 `PredrawTailRecoveryR32`、`R36AnalysisHistoryPolicyR364`、`TargetIssueResolver`、`ApiClient`、`AutoFetchService`既有fetch in-flight控制与现有catch-up controller。

- 1秒轮询为调度目标，记录计划启动/实际启动/首字节/EOF/parse/commit全部同boot单调时钟。网络故障用受控退避和服务状态，不把失败计成功抓取。
- 最新lane只保留一次有效request in-flight；history lane单独有界分页和公平预算，不因一直“latest有新号”永远抢不到时间。draw raw来源完整可查。
- 历史窗上限5000条，跨日issue用官方序号解析，不能简单数值减1。缺口表记录范围与来源；缺尾部输入时当前预测记SKIP_INPUT_GAP，恢复后从下一期开始自然冻结。

### B03-运行06：canonical单owner和持久队列推进

涉及 `AutoFetchService.submitAnalysisRequestR36/runAnalysisRequestR36`、`AnalysisRequestStoreR36`、`AnalysisSingleFlightR36`、`V51RuntimeOrchestrator`、`BusinessReconcilerR36`。

- draw提交时产生下一target intent；analysis入口只消费已提交draw head，claim带owner/fence和输入cutoff。结果未commit不得先广播CANONICAL_COMMITTED。
- 新target到达时停止旧过期compute，旧owner只能在真实停止/lease被fenced后替换；不可仅清boolean就开第二线程。latestRunnable从持久任务恢复，不依赖UI countdown触发。
- 只对当前canonical critical工作保留短事务；100/1200物化、图表、模型训练使用后置outbox。所有postCanonical side-effect记录自身commitOutcome，不能使canonical重算。
- 验收：旧target持续compute时收到新draw，deadline前能明确FREEZE或SKIP；两owner竞争只能一个publish；提交后进程death重开不能重复BET/训练label。

### B04-运行07：冻结与结算闭环

涉及 `CanonicalObservationWriterR36` / `PreparedCanonicalStoreR335`、`OosAfterDrawWriterR36`、`SettlementEngine`、`SettlementTaskStoreR36`、`EssentialSettlementFeedbackR923`、`RuntimeSchemaR26.feedbackWithinTransaction`。

- 每个自然target必须有真实冻结或带原因SKIP；有效冻结tuple包含target、inputThrough<target、policy/checkpoint/source hash、freezeWall/mono、deadline和K3 action。
- draw到账先结算当前freeze，旧pending用age公平slice排空。结算事务写freeze匹配结果、settlement receipt和feedback outbox，幂等key不可只用“目前最新期”。
- 缺冻结允许终结为NO_PREDRAW并纳入漏期分母；不能拿历史重算填自然FUTURE，不能把hit=-1当miss或计入命中率。
- draw label训练事件不依赖BET和证书；model consumer checkpoint与consumption receipt同事务推进。断电后未确认effect可重放，已有effect不重复更新参数。
- 验收：重复开奖、修正draw hash、晚到开奖、过期lease、commit ACK丢失、无冻结、每期SKIP，逐项检查原件/hash/分母。SHADOW和REAL严格分账。

### B05-运行08：原件闭合、资源与报告负担

涉及 `ResearchRetentionStoreR30`、`CheckpointRecoveryR335`、`KeepMigrationStoreR31`、`RuntimeFaultOriginalStoreR335`、报告snapshot owner、`StorageBudgetR31`。

- 三报告 source-manifest 的snapshot/原件范围是compact，不含raw SQLite。missingObjects=556/677是实际可用性缺口，不是可自动再生对象的许可；每个引用须区分存在/丢失/未验证/ABI不兼容。
- 可从已校验相同hash原件恢复；丢失不可恢复时保留不可用父代和失败链，创建新的冷启动候选，不能移植旧自然证据或伪造checkpoint。
- 空间账必须覆盖热DB、WAL、报告快照、临时ZIP、keep/CAS、APK内置asset、模型与历史重复体，不能只限制draw5000就声称全机有界。报告任务只允许一个活动job、预留峰值空间、关闭/取消后清临时文件；成功导出的用户原包保留。
- 存储测试以已有1.4GB级结构和长时间高频write进行：退出/重启、同时report、低磁盘、旧对象不可解引用；GC必须具备mark闭合证据，否则不删原件。

## 四、独立验收、依赖与回滚

- B01的局部合同可以独立验收：writer背压不会假crash、控制回执能提交、故障原件不丢；不要求B09已有高胜率策略才能签B01。B02独立验收实际receipt-draw身份与事务；B03独立验收owner/调度；B04独立验收冻结/结算/feedback一一对应；B05独立验收受保护原件和预算。
- 每批签收区分 `HOST_CONTRACT_PASS`、`ANDROID_COMPILE_PASS`、`APK_INSTALLED`、`DEVICE_RUNTIME_PASS`、`NATURAL_QUALITY_STATUS`。本次payload只达到第一项与补丁前态校验，后四项未运行；不得给整软件运行证据盖章。
- 生产schema仅加法迁移，失败保留原表/旧journal/快照。回滚app前检查DB forward-read contract；不能把旧APK直接覆盖新DB作为无条件回滚。失配时降级仅safe read/SHADOW停发REAL。
- 受控replay与synthetic fixture必须独立source role，严禁生成自然/真实样本。任何“无策略符合统计资格”是有效负结果，工程合格与可获利是两件独立验收事实。

## 五、已交付的代码与验证

见 `runtime_payload/README.md`、`PAYLOAD_MANIFEST.json`、分批patch及完整后态。6份文件：`DatabaseCoordinator`、`DeferredEvidenceWriterR36`、`FetchLedgerR925`、`DirectedResearchCreationR932`和两份已接线helper。对095精确前态应用检查通过；15项Java断言和3项SQLite合同通过；完整Android编译和真机测试尚未进行。源码095保持未修改。schema分页迁移/完整app闭环是上述明确工单，未用一个helper或宿主fixture冒充已实现。
