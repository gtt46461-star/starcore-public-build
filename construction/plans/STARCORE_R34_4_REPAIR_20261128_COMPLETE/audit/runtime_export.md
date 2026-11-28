# R34.4 运行、导出、存储专项源码核验

## 范围与结论边界

本文件只审计，不修改应用源码。报告job为 `1d352070785230116899039852ab39a3e48e3e79a048d8ee7135d69e214f8af3`。安装源码提交为 `5c7f1b36eaee6036df280e85835bb0fb4ce0b40d`、versionCode为577311211；main当时为 `6e762807be3b36affccaeab230d44d540a16f8c8`，其中应用源码明显较旧，不能直接套旧main声称已对应手机。本审计核的是安装提交工作树。applicationId为com.starcore.research.r13、targetSdk28、运行设备Android14，不把仅针对较新targetSdk的规则当已证根因。

报告成功导出成ZIP，96检查中56执行、40读取错误；原始SQLite/APK/模型字节未包含。没有原始5GB SQLite，未完成真实查询计划、Android编译或真机长跑；历史跨版本计数与主进程当次样本分开。

## 有效设计保全

- 唯一主链writer、single flight、赛前冻结PIT、正式结算唯一身份。
- ReportExportServiceCP4原request→snapshot→catalog→archive→verify→READY_TO_SHARE→SAF分享；96检查、章节、附件命名、index/manifest、SHA/CRC及语义回执。
- 归档完整性、检查完成、业务健康、自然质量分离；失败也可导出，不把READ_ERROR改PASS。
- 用户取消、进程死亡续作、share锁、failed唯一输入保护；补足恢复退役闭环，不清空原件。
- current-process/SQL snapshot/historical原件scope分离；未知不当0，旧异常不当当前。
- 已正常提交的FTRL学习和原始训练产物，不称所有训练从未运行。

## 问题概览

|编号|优先级|问题|证据强度|
|---|---|---|---|
|RT01|P0|报告覆盖率查询在全事件联表上超时|EXACT_STACK_AND_SOURCE; ACTUAL_QUERY_PLAN_PENDING|
|RT02|P0|共享诊断依赖失败后被多个检查反复重新构建|CONFIRMED_SOURCE_PATH|
|RT03|P0|报告共享180秒预算耗尽后压缩后续检查为1秒|CONFIRMED_SOURCE_AND_OUTPUT|
|RT04|P0|报告/保留快照占24.1GB，失败快照没有退役闭环|CONFIRMED_STORAGE; CONFIRMED_POTENTIAL_RETENTION_TRAP; PER_JOB_STATUS_PENDING|
|RT05|P0|draws已限5000，但热库大表仍持续膨胀|CONFIRMED_DATA; TABLE_POLICY_REQUIRES_OWNER_REVIEW|
|RT06|P0|写耗时故障队列被共享故障热配额卡住，反向抑制研究|CONFIRMED_CODE_CYCLE_AND_HISTORICAL_SNAPSHOT|
|RT07|P1|故障观测队列已经丢弃41次超限证据|CONFIRMED_RECORDED_EVIDENCE_GAP|
|RT08|P1|研究启动经历约53.7秒和5次尝试才就绪|CONFIRMED_CURRENT_STARTUP_SAMPLES|
|RT09|P0|1秒抓取目标未实现，调度和入库阶段均有长尾|CONFIRMED_CURRENT_SERVICE_WINDOW|
|RT10|P0|赛前冻结大量缺失且推理长尾|CONFIRMED_RECORDED_FAILURE_PATH|
|RT11|P1|后台权限具备，业务连续性和长期运行仍未验收|CONFIRMED_CAPABILITY_AND_EVIDENCE_GAP|
|RT12|P0|业务scope JSON被300字符展示截断器截坏|CONFIRMED_EXACT_DEFECT|
|RT13|P1|崩溃与退出历史跨版本混合，当前稳定性缺长期证据|CONFIRMED_SCOPE_BOUNDARY_AND_HISTORICAL_RISK|
|RT14|P0|checkpoint引用缺556个对象，不能验证恢复闭包|CONFIRMED_REFERENCE_AVAILABILITY_GAP|
|RT15|P1|启动观测日志自身容量被保护规则阻塞|CONFIRMED_OBSERVABILITY_CAPACITY|
|RT16|P1|报告是诊断包，不能独立重放全部SQLite/APK/训练原件|CONFIRMED_PROFILE_BOUNDARY|
|RT17|P1|写锁指标与历史硬超限范围混用，需分别治理|CONFIRMED_MEASUREMENT_SCOPE|
|RT18|P0|精确结算待办与延后证据债未闭环|CONFIRMED_EXPORTED_METADATA_JOIN_AND_RECOVERY_FILTER_GAP; DEFERRED_PER_ROW_CAUSES_PENDING|
|RT19|P1|冻结快照缺主writer单调时钟锚，历史原件不能投影为当时状态|CONFIRMED_EXACT_SOURCE_CONTRACT_AND_REPORT_BOUNDARY|

## RT01 报告覆盖率查询在全事件联表上超时

**P0；EXACT_STACK_AND_SOURCE; ACTUAL_QUERY_PLAN_PENDING。**

### 原始证据

- catalog-*.jsonl: check_result[id=RSEC-009].error_detail：RuntimeHealthLedgerR36.readCoverageR342:291，SQLITE_INTERRUPT；budgetMs=20000
- DIAGNOSTIC_SCOPE.json#/tables：v57_runtime_event_r926=861897，r341_runtime_event_scope=382966

### 原因

readCoverageR342 先写事件表 JOIN scope，再 min(target_issue)。原件明确卡在此查询；未提供实际 EXPLAIN，不能断言手机必定用了哪个索引。当前安装范围选择可从已有 r341_observation_release 索引驱动，避免优化器优先遍历庞大事件表。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java`
- `app/src/main/java/com/ethk3/novaonepro/RuntimeObservationScopeR341.java`

### 保全

- 原 min 语义、游戏/发行版/源码/数据库过滤、合法12位期号、30期分母
- 完整报告96项及只读快照

### 施工处理

1. readCoverageR342:278–333 的第一条SQL以 scope s CROSS JOIN event e ON e.event_id=s.event_id 定向从当前scope索引取外层；见已写候选patch。
2. 记录查询实际行数和 EXPLAIN；后续30期相关 EXISTS 也按 event_id/target_issue 与scope键验证执行计划，不把当前范围强裁成最新任意N行。

### 验收

- 候选SQL在50000事件、100组scope夹具返回值逐一等价；此项已运行。
- 在原5GB快照上复测 RSEC009；当前安装范围完整保留，超时从原始原因引用可追溯；目标设备性能另验。

## RT02 共享诊断依赖失败后被多个检查反复重新构建

**P0；CONFIRMED_SOURCE_PATH。**

### 原始证据

- ReportCheckRegistryCP12.java:105 model-analysis；RSEC009/030/033/034/035/037/039共同调用model(ctx)
- ReportReadContextCP4.java:170 memo只在factory.create成功后put；catalog的多项超时栈重复model/dashboard

### 原因

同一报告快照内只缓存成功值，昂贵依赖失败后下游每次重建。不是缓存了旧的取消信号；runCheck:122每项创建新signal并在finally清除。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/ReportReadContextCP4.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportCheckRegistryCP12.java`
- `app/src/main/java/com/ethk3/novaonepro/ResearchDashboardR924.java`
- `app/src/main/java/com/ethk3/novaonepro/ModelEffectivenessAnalyzerR35.java`

### 保全

- 新旧scope隔离、取消传播、UNKNOWN不转0、不可变快照
- runCheck在原owner线程同步执行；不启动额外共享连接SQL工作线程

### 施工处理

1. 引入只在本次ReportReadContext生命期有效的依赖结果：键(snapshotSha,sourceCommit,dependencyName,parameterHash,definitionVersion)，状态 SUCCESS/SOURCE_UNAVAILABLE/CANCELLED，失败带原errorRef与attempt耗时。
2. 把model/runtime/dashboard按共同事实拆成可独立取数的依赖；一次失败仅使真正依赖该事实的检查输出DEPENDENCY_UNAVAILABLE，不把别的检查短路。
3. 允许该依赖在新的显式重试/新报告context重算；全局用户取消不得负缓存为普通失败。旧失败对象不得跨快照复用。

### 验收

- 同一失败依赖在同次报告只执行一次；每个原检查仍逐项有ID、状态及真实依赖错误，不能把全部40项都强绑同一依赖。
- 一项取消后必须等当前runner实际返回、所有Cursor关闭再开始下一项；若未来引入Future，cancel不等于已退出，未确认退出必须终止该reader并重开只读连接。

## RT03 报告共享180秒预算耗尽后压缩后续检查为1秒

**P0；CONFIRMED_SOURCE_AND_OUTPUT。**

### 原始证据

- ReportCatalogCP4.java:340：max(1000,min(20000,180000-spent))
- R31_SEMANTIC_RECEIPT.json#/checkAssessmentR342/execution：56 EXECUTED、40 READ_ERROR；RSEC009/010为20000ms，其余38失败项为1000ms

### 原因

早期慢查询/渲染消耗共享预算，后续原本可能正常的检查被降到1秒，造成检查覆盖级联失败。根因包含SQL、渲染/哈希和累计调度，不能把全部40项都归为同一SQL。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/ReportCatalogCP4.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportReadContextCP4.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportPhaseBudgetR31.java`

### 保全

- 每项明确超时/取消、总阶段上限、同连接同步执行、96项数量
- 原index/archive/semantic receipt/share接口及中文报告章节

### 施工处理

1. 把全局预算改为“本轮工作时间片+可恢复进度”，每项或依赖有独立budget；全局时间片耗尽时checkpoint到job并PAUSED续跑，不将未运行项伪装READ_ERROR。
2. 昂贵查询用稳定主键分页与已验证部分产物断点；检查取消后关闭本项renderer，不损坏已完成附件。
3. 必须先做RT01/RT02，再校准预算；禁止仅增大超时或将失败强写PASS。

### 验收

- 连续3次导出、导出中取消再恢复、杀diagnostics进程后恢复、低空间/慢盘/共享读取同时发生均可拿到有效ZIP或明确可恢复失败。
- 完整报告96项全部保留，SOURCE_EMPTY/UNAVAILABLE/FAIL/PASS分别统计；一项耗时不能压缩无依赖项预算。

## RT04 报告/保留快照占24.1GB，失败快照没有退役闭环

**P0；CONFIRMED_STORAGE; CONFIRMED_POTENTIAL_RETENTION_TRAP; PER_JOB_STATUS_PENDING。**

### 原始证据

- STORAGE_CENSUS.txt第1行：total=31180341650，REPORTS=24546120422，REPORT_OR_RETENTION_SNAPSHOT=24109711360；列出5份4.56–5.06GB快照
- ReportPublicationCP12.java:316：FAILED且snapshot.db存在直接continue；ReportCleanupR31:12/30仅收READY_TO_SHARE

### 原因

保护唯一失败输入是有效规则，但缺少“失败快照恢复成已验证产物→安全退役”的状态转移，重复创建新请求可累积整库副本。此次清单未含每份job正文，不能把5份全部判定为FAILED或泄漏；当前活跃快照本来就应存在。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportCleanupR31.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportBudgetSU2.java`

### 保全

- 只读冻结快照、唯一原件保护、活动share锁、已发布ZIP、校验和、原失败回执
- 禁止按目录年龄、大小或新报告成功就删除不同hash旧快照

### 施工处理

1. 先追加只读job清单：jobId、stage、snapshotHash/bytes、原request profile、publication proof、active readers、cleanupReason；作为独立小附件，不能阻塞导出。
2. READY_TO_SHARE继续现有verifyPublishedZip+semantic receipt+profile核验，独占share lease后cleanRaw，只清临时workspace；清理失败不得降级READY。
3. FAILED的DIAGNOSTIC任务复用同一快照生成完整所请求范围的诊断包并保留失败历史，验证ZIP/manifest/语义回执后转READY并清理；不得生成空壳ZIP作为替代。
4. FAILED的FULL_RAW必须验证替代产物含同hash同字节snapshot及所请求原件；尚未获得替代证明就显示具体保护原因与恢复动作，不无限静默保留。
5. 新请求优先恢复明确匹配同job/同profile的失败任务，不用相近时间或同版本替代snapshot identity；使用相同owner状态机，不新建平行清理器。

### 验收

- 构造5份不同hash失败任务：恢复与清理只处理有精确替代证明者；其余保留且界面列明原因。
- 连续3次正常导出后临时快照占用回落，分享链接仍可读取、逐成员CRC/SHA校验通过；取消/进程死亡任一边界不丢唯一原件。

## RT05 draws已限5000，但热库大表仍持续膨胀

**P0；CONFIRMED_DATA; TABLE_POLICY_REQUIRES_OWNER_REVIEW。**

### 原始证据

- DIAGNOSTIC_SCOPE.json#/tables：draws=5000；research_row_revision_v2=2608991；r27_chart_cache=332670；v57_fetch_ledger_r925=202698
- STORAGE_CENSUS.txt第2行 measuredBytesByOwnerTable：deferred_evidence_job=1180975104、runtime_event=1160331264、k3_action_space=952934400、chart_cache=424329216、row_revision=256090112；freelist_count=0

### 原因

5000期限制只覆盖draws，派生动作/事件/修订/图表与待写原件没有同等生命周期终点；freelist_count=0说明不能靠一次VACUUM凭空缩小当前有效页。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/DataRetentionGovernorR900.java`
- `app/src/main/java/com/ethk3/novaonepro/RetentionTableRegistryCP12.java`
- `app/src/main/java/com/ethk3/novaonepro/RetentionRevisionSchemaCP12.java`
- `app/src/main/java/com/ethk3/novaonepro/RetentionArchiveProtocolCP12.java`
- `app/src/main/java/com/ethk3/novaonepro/StorageMaintenanceServiceR82.java`
- `app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java`

### 保全

- 待结算/训练引用、checkpoint/CAS闭包、未确认迁移备份、真实出手账本、原始失败证据

### 施工处理

1. 逐表补充owner、immutable/mutable、期号字段、pin规则、可重建依据和归档证明；按5000期滚动窗口管理可替代派生数据，长期模型只保留必要原始模型及压缩统计。
2. 对k3动作空间避免每期重复存同一静态1200组合：组合表一次存、每期保留明确候选与必要分数/引用；先证明回放重建等价，再退役旧行。
3. deferred jobs只在产物提交并readback后退休原payload；runtime event和row revision在消费ACK及快照pin释放后分批归档。
4. 逻辑退役后再在现有离线维护窗口压缩；按预检空间/校验/原子替换/恢复证明执行，不在实时writer中全库VACUUM。

### 验收

- 5000期后继续运行，所有表/文件按其声明预算进入稳定区间；新建事件量与回收量均有实测。
- 保留回放、当前待结算、活跃训练和报告导出；故障注入回滚不丢历史。

## RT06 写耗时故障队列被共享故障热配额卡住，反向抑制研究

**P0；CONFIRMED_CODE_CYCLE_AND_HISTORICAL_SNAPSHOT。**

### 原始证据

- source-manifest.json#/mainWriterFaultsR342/historicalOriginalJson：pending=32，drainFailures=35，drainAttempts=35，drainStatus=BLOCKED_STORAGE，faultArchiveState=BLOCKED_PROTECTED，ONLY_MUTABLE_OR_UNKNOWN_FAULT_PRODUCERS_REMAIN
- WriterHoldFaultsR31.java:204 requireQuota计算整个r31_runtime_fault；ResearchPoolR27常量2000条/8MiB；RuntimeFaultOriginalStoreR335:73/117只归档WRITER_HOLD+WRITER_UNLOCKED

### 原因

故障热表被其他mutable/未知producer占用时，故障归档器不能退休这些行，新writer故障又无法落库；内存队列≥24触发deferResearch。DatabaseCoordinator:1023仅有公平槽豁免；ColdRebirth/提交队列前置gate仍可能挡住进度。

证据边界：投影本身 observationState=UNKNOWN、SNAPSHOT_CLOCK_ANCHOR_UNAVAILABLE；这些是快照中hash验证历史行，不能称现在主进程仍是相同32条。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`
- `app/src/main/java/com/ethk3/novaonepro/RuntimeFaultOriginalStoreR335.java`
- `app/src/main/java/com/ethk3/novaonepro/RuntimeFaultArchiveB01.java`
- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java`

### 保全

- 单writer、commit identity、原故障字节/hash、pending outbox、P0 freeze优先、未证明不可删

### 施工处理

1. 把WRITER_HOLD已封存原件写入独立有界durable spool/CAS并持有locator收据，不再与可变故障聚合表争同一热配额；通过原有RESEARCH_CONTROL通道落index，不另开writer。
2. 可变producer增加seal/rotate合同：旧版本按revision/hash封存并readback后生成retire permit，新occurrence写新版本，不能覆盖未归档原件。
3. 设置storage recovery控制槽与研究计算/持久化公平槽；明确压力和“正在进行且即将截止的freeze”不同，修复前置gate，避免准入前饿死。
4. 先恢复durable inflight旧批次，核commit回执；只在原件或CAS读回一致后减pending/释放内存，绝不能清空队列伪恢复。

### 验收

- 混合2000条可变/不可变故障+已有inflight+重启夹具中drain能真实前进、pending降到24以下，研究能产出新artifact。
- 不重放已提交业务、不丢故障原件；存储满时保留明确INCOMPLETE，P0调度不被诊断写锁长期阻塞。

## RT07 故障观测队列已经丢弃41次超限证据

**P1；CONFIRMED_RECORDED_EVIDENCE_GAP。**

### 原始证据

- source-manifest.json#/mainWriterFaultsR342/historicalOriginalJson：observedOver200ms=73，capacityLoss=41，droppedFirstSequence=33，droppedLastSequence=73，state=INCOMPLETE_CAPACITY_LOSS

### 原因

内存队列最大32，持久化排空不能前进时后续fault observations被拒，统计不再完整；这是故障观测证据损失，不代表业务事务本身丢了41次。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldLedgerR30.java`
- `app/src/main/java/com/ethk3/novaonepro/RuntimeFaultOriginalStoreR335.java`

### 保全

- 显式capacityLoss和不完整状态
- 有界内存，不为保全观测无限扩容

### 施工处理

1. 与RT06同修：有界预留spool提供durable first-write；保留不可恢复观测的序号区间与loss计数，不回填伪异常。
2. 把业务成功与诊断持久化失败独立，不让已commit业务重跑；故障重复按producer/revision/hash保全原件后聚合。

### 验收

- 压力/低磁盘注入可复现观测失效且UI/报告标INCOMPLETE；恢复后新观测不持续丢失。

## RT08 研究启动经历约53.7秒和5次尝试才就绪

**P1；CONFIRMED_CURRENT_STARTUP_SAMPLES。**

### 原始证据

- runtime-observation-r341ResearchStartupJournal.txt 当前source 5c7f1b36... 32帧、5个attempt；wall1795790318232→1795790371936=53704ms；4 WAIT_DEPENDENCY、2 WAIT_RESOURCE
- startup-failure原件：R34_STARTUP_CHECKPOINT/READY、R34_RETENTION_BOOTSTRAP、R31_UPGRADE_INFRA发生CP3_DB_BUSY_DEADLINE，phase WAIT/BEFORE_SQL；当前最后OWNER_READY VERIFIED

### 原因

研究启动依赖迁移/retention/bootstrap写批次与高优先级writer竞争。当前最后已就绪，不能说启动永久失败；主要是脆弱长尾和多次重试。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RetentionMigrationR34.java`
- `app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java`
- `app/src/main/java/com/ethk3/novaonepro/WriteBudgetR341.java`
- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`

### 保全

- 域迁移事务、DDL逐单元收据、重入fence、已完成阶段复用、关键主链先就绪

### 施工处理

1. 按现有prepareStartupSchemaR34阶段收据跳过已提交单元，依赖尚未变更采用可恢复退避而不是重跑整初始化。
2. DDL/初始化数据在事务外准备，保留小批次；写槽统计等候与实际SQL分离，确保迁移fair turn真的能穿过前置gate。

### 验收

- 旧5GB库冷启、覆盖安装、中断每个迁移单元再启均只提交一次；记录主链ready与research ready各自P95。

## RT09 1秒抓取目标未实现，调度和入库阶段均有长尾

**P0；CONFIRMED_CURRENT_SERVICE_WINDOW。**

### 原始证据

- runtime-observation-fetchCadenceR31.txt：targetSlots=87,wakes=23,missed_tick=64；slotDelay p95=11655/max=36055ms；ingest p95=3597ms；network p95=1837ms；19成功/3轮失败/1HTTP或payload失败

### 原因

原件能区分等待slot、网络和ingest；当前目标调度1秒不等于1秒获得新开奖。具体网络失败码与429是否真实发生须读取对应fetch ledger，不从统一scheduleReason推断。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`
- `app/src/main/java/com/ethk3/novaonepro/FetchLedgerR925.java`
- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/IssueTimelineR30.java`

### 保全

- 防重入、网络退避、开奖验证、同issue幂等、服务器约束

### 施工处理

1. 复用现有单轮抓取，网络回调与短durable ingest脱离研究重计算/大日志；每槽记录scheduled/wake/network/validated/committed单调时钟。
2. 将准备好的payload以短原子写落库并发布事件；过期目标与当前下一期分队列，不能让历史补课排在实时冻结前。
3. 有真实429时尊重退避并记外部限制；平稳正常网络区间独立验收1秒调度。

### 验收

- 至少连续1440期对每个目标槽给分母与missing原因；正常网络调度指标和端到端落库指标分别满足目标；断网恢复不造赛前历史预测。

## RT10 赛前冻结大量缺失且推理长尾

**P0；CONFIRMED_RECORDED_FAILURE_PATH。**

### 原始证据

- runtime-observation-heartbeatSummary.txt：NO_PREDRAW_CANONICAL_TERMINAL/FROZEN_OBSERVATION_MISSING；pending=163
- report-text-d92029f4dd7be646b8a52e03ce9712b89679bcaccaf08f14eed422f843cdff49.txt：target202611270879 elapsed37651ms、R36_COMPUTE36683/3000、ACTION_STABILITY_DONE后B05_TIMEOUT；880有R31_PHASE_BUDGET_INFER
- R30_REPORT.json#/metricResults[metricId=M04]：observe_to_validate n=8,p95=85ms；request_to_frozen n=0,missing=8，observe_to_settled n=0,missing=8
- runtime-opportunity-cohort.json#/requestedTargets=4、canonicalSuccesses=3、drawnEligibleTargets=119、drawnWithPredrawCanonical=2、drawnWithoutPredrawCanonical=117、truncatedOlderTargets=true；这是当前发行版事件界定的有界目标区间，不是服务enabled全时段分母

### 原因

已记录的部分推理在有效时限前未形成durable frozen，后续按原规则进入无赛前终态；但当前发行版cohort已有3次canonical成功，不能写成冻结从未形成。当前代码fastFallback=UNSUPPORTED_UNTIL_PREREGISTERED_QUALIFIED_IMPLEMENTATION，不能临时假装存在快速模型。163条结算待办及2899条延后证据债另见RT18，不能全部归因于实时推理。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java`
- `app/src/main/java/com/ethk3/novaonepro/RuntimeResourceAuditR927.java`
- `app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java`
- `app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java`
- `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`

### 保全

- PIT、deadline、single flight、冻结与结算hash域、唯一正式意向
- 已有3次canonical成功及其不可变原件；正确的无赛前终态排除语义

### 施工处理

1. 研究训练在后台生产已验证可加载model bundle；实时路径只读当时有效冻结模型和有界feature，移出候选搜索/多模型训练/稳定性回放。
2. 对ACTION_BUILD_FUNNEL_META_RANK/ACTION_STABILITY具体调用分段计时，先修最重stage，不盲目提高总deadline。
3. 可用快速fallback须预注册/验证/版本固定且在deadline前可用；无可用模型时明确NO_VALID_MODEL并维持shadow观察的真实失败状态。

### 验收

- 未来120+真实期按服务enabled区间、可预测时窗和实际请求分别列完整分母；每次合法predraw以原冻结hash按同issue结算，缺失有真实原因；不得补造已开奖期的frozen。

### 边界

M04八个观察点、当前发行版有界cohort和跨历史SQL任务总量是不同统计总体；117/119不能直接当完整服务在线机会失败率，3次canonical成功也不能证明长期合格。

## RT11 后台权限具备，业务连续性和长期运行仍未验收

**P1；CONFIRMED_CAPABILITY_AND_EVIDENCE_GAP。**

### 原始证据

- runtime-observation-backgroundRuntimeCapabilityR31.txt：foregroundService=YES,batteryExemption=YES,exactAlarm=YES,backgroundRestricted=NO,ownerState=OWNER_PROJECTION_MISSING；continuous24h/72h=NOT_RUN
- heartbeat：restartRequest=68,actualStart=37，RESEARCH_STALE与learningProgress新鲜并存

### 原因

权限能力、服务心跳、业务产物进展是三类证据；当前有服务运行但owner投影缺失、scope被截断（RT12）、业务链仍缺冻结/研究产物，不能用增加自启动次数代替修复。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`
- `app/src/main/java/com/ethk3/novaonepro/BackgroundRuntimeCapabilityR811.java`
- `app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java`
- `app/src/main/java/com/ethk3/novaonepro/RuntimeObservationScopeR341.java`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`

### 保全

- 单owner租约、前台服务用户开关、重启fence、任务持久化、系统不可保证force-stop自动恢复的真实边界

### 施工处理

1. 先修RT12，再让健康判据以相同owner/boot/release/database和产物commit watermark判定，不用UI打开事件驱动研究。
2. Service持续调度已有owner，Activity只读投影；onResume不得新增第二套计算。
3. 后台控制请求需回读业务进度才算恢复；无进度按明确依赖原因修复，禁止无限重启。

### 验收

- 目标vivo Android14熄屏/切后台/断网恢复/进程杀死及升级安装逐场景测试；连续24h和72h分别出真实机会分母、冻结/结算/训练进度，不以心跳达标代替。

## RT12 业务scope JSON被300字符展示截断器截坏

**P0；CONFIRMED_EXACT_DEFECT。**

### 原始证据

- runtime-observation.json#/r341BusinessScope 长度300，末尾databaseIdentity:"35129b31，无闭合JSON
- ServiceHeartbeatStore.java:501 `this.observationScope=safe(observationScope)`；safe:1616–1619截300

### 原因

结构化身份字符串错误地使用UI短文本清洗函数，丢失完整databaseIdentity、sample clocks与scopeKind，影响后续同源合并和健康判定。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java`
- `app/src/main/java/com/ethk3/novaonepro/RuntimeObservationScopeR341.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`

### 保全

- safe对其他展示字符串的原有限长行为；完整scope的原schema和字段；跨进程边界

### 施工处理

1. 只替换BusinessTruth.observationScope赋值为完整字符串（null→空、允许trim），见候选patch；解析/验证继续由scope consumer处理。
2. 结构化原件如需限长必须作为独立附件保存原字节并标UNAVAILABLE，不能输出貌似JSON的截断片段。

### 验收

- 433字符有效scope在赋值/序列化/导出后全部字段一致；原报告300字符反例确定不可JSON解析。
- candidate patch apply --check已过；尚未Android编译或真机验收，不标软件已修好。

### 边界

完整scope保全不能生成缺失的冻结时点单调时钟锚；main writer快照投影的独立问题与施工合同见RT19。

## RT13 崩溃与退出历史跨版本混合，当前稳定性缺长期证据

**P1；CONFIRMED_SCOPE_BOUNDARY_AND_HISTORICAL_RISK。**

### 原始证据

- runtime-observation-crashSummary.txt：crashCount7319，oomCount0；当前release10分钟记录事件0，索引64项均historicalReleaseEntries；diagnostics进程有3次系统low memory退出
- 同文件mainStall4970ms属于版本577311204，安装包是577311211

### 原因

7319是累计异常记录，不能称211发生7319次崩溃；但diagnostics曾低内存退出、长窗口没验收仍为真实可靠性风险。Java OOM0也不等于系统不会因低内存杀进程。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java`
- `app/src/main/java/com/ethk3/novaonepro/RuntimeResourceAuditR927.java`
- `app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java`

### 保全

- 按boot/release/process分类原异常；流式报告与有界observer线程池；已有低内存退出记录

### 施工处理

1. 导出保持流式、限内存，重入时复用已验证snapshot/分段产物；所有heavy native阶段close前一阶段连接后执行。
2. 新崩溃/退出回执带release/source/process/window；旧故障维持历史展示，不参与当前10min分子。

### 验收

- 大库导出+后台研究同时运行、杀diagnostics后恢复、连续运行真机RSS/PSS/native/heap各自曲线；没有回执不称零崩溃。

## RT14 checkpoint引用缺556个对象，不能验证恢复闭包

**P0；CONFIRMED_REFERENCE_AVAILABILITY_GAP。**

### 原始证据

- source-manifest.json#/r26CheckpointAuditR32：roots658,observed678,verified103,missing556,notRun19,unknownChildSets575,sourceClosureVerified=false

### 原因

检查点引用存在不代表模型状态可恢复。缺失是报告在其固定根目录看到缺失；19个超预算未验不等于丢失，575未知子集合也不能当作垃圾回收已闭合。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/CheckpointReportAuditR32.java`
- `app/src/main/java/com/ethk3/novaonepro/CheckpointAuditContinuationR343.java`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointRecoveryR335.java`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointArtifactsR26.java`

### 保全

- 原checkpoint元数据、原state/model/optimizer/seed/source身份、未验证对象保护、GC禁止

### 施工处理

1. 按缺失对象逐一定位blob hash、root、owner、stage及源安装，恢复已有精确原件；确实无法恢复的旧模型标不可用并从保全合法数据重新训练新版本。
2. 按rootSetHash+snapshotHash持久化分页验进度；每页当前字节验hash，过去页验证不可直接证明当前字节未变。
3. 发布完整root closure且测试恢复后再允许训练继续或旧对象退役。

### 验收

- 缺失/损坏/预算未验三类均可追踪；未闭合模型不参与实时主链；同seed同数据中断恢复与连续训练状态一致。

## RT15 启动观测日志自身容量被保护规则阻塞

**P1；CONFIRMED_OBSERVABILITY_CAPACITY。**

### 原始证据

- runtime-observation-r341ResearchStartupJournal.txt：state=OBSERVABILITY_DEGRADED；emergency=R341_JOURNAL_PROTECTED_CAPACITY
- source-manifest.json#/startupFailureOriginalsR342：referenced10,verifiedAndAttached6,pending4

### 原因

journal封存/归档保留没有及时形成可释放闭包，观察记录容量不足；不能为了记录完整再阻塞业务owner，也不能覆盖未导出的唯一失败原件。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/StartupStageJournalR341.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java`

### 保全

- startup token BEGIN/END配对、原异常hash、未闭合token、用户取消语义

### 施工处理

1. 已闭合token按段封存成hash校验附件并写locator/retire receipt；未闭合token保留在当前日志。
2. 保留最小emergency槽及丢失区间记录；journal写失败独立观测状态，不把业务已成功阶段回退。
3. 将4个pending引用在下次导出前补附件回读或明确原因，而不是把referenced数当attached。

### 验收

- 循环故障/反复升级后journal仍可追加；所有已封存段有完整替代原件；已成功业务不会因观测故障被重复执行。

## RT16 报告是诊断包，不能独立重放全部SQLite/APK/训练原件

**P1；CONFIRMED_PROFILE_BOUNDARY。**

### 原始证据

- outer_index.json：evidenceComplete=true,checksComplete=false,allChecksPassed=false,expected/exportedVolumes=1
- source-manifest.json：rawEvidenceIncluded=false,installedApkIncluded=false；reproductionR342.state=ORIGINAL_BYTES_NOT_INCLUDED；r31MissingAttachmentCopyBytes=14305413

### 原因

包已成功导出且文件完整，但其profile排除原始数据库/APK/model/training，完整诊断目录不等于全原件可复现。属于证据边界，不可据此指控CRC损坏。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/ReportSemanticVerifierR31.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`

### 保全

- 默认易传输的DIAGNOSTIC模式、原件单独完整备份入口、固定manifest及SHA验证

### 施工处理

1. 在同一UI/报告同时显示：归档完整性、检查完成率、业务健康、原件覆盖率四个独立结论。
2. 原始大库验性能/重放需用既有FULL_RAW或目标设备测试提供证据，不能在这份19.7MB内假称存在5GB数据库。
3. 继续对ZIP及各附件SHA/CRC做独立校验，保留完整排除清单和精确源码身份。

### 验收

- 诊断模式不会因未请求的原件缺失阻塞导出；明确请求FULL_RAW时任何缺件导致真实INCOMPLETE而不是强标完整。

## RT17 写锁指标与历史硬超限范围混用，需分别治理

**P1；CONFIRMED_MEASUREMENT_SCOPE。**

### 原始证据

- R30_REPORT.json#/metricResults[M07]：FAIL,value1000,samples73；两个窗口R925_FETCH_PAYLOAD hold_max452/wait_max985，R925_FETCH_COMPLETE hold_max821/wait_max1269
- 同文件#/diagnosis/findings[id=WRITER_HARD]/evidence.value=25179；ReportSnapshotR30:282对r30_writer_hold无scope的sum(over_hard)

### 原因

M07当前展示是当前捕获critical窗口的最大wait_p95=1000，并不是单条SQL恰持锁1000ms；25179是累计全表硬超限，不可套给当前安装。writer分析要按operation/source/session分开等候、SQL、commit、diagnostic耗时。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricEvaluatorR29.java`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldLedgerR30.java`
- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`

### 保全

- 原始直方图、failure与commitOutcome区分、真实超限不能抹零

### 施工处理

1. 保持旧累计值作为historical，不覆盖；新增当前release/source/process窗口投影并明确scope。
2. 按两种fetch写operation分别用实际SQL trace拆长事务，编码/排序/哈希/统计置于事务外，P0只执行必要原子落库。
3. 把wait_p95与hold_p95单独列，不以改变阈值让指标通过。

### 验收

- 同scope当前窗口可重算并与原件一致；既定短写目标在目标设备测试；每条“已commit但诊断失败”不得执行第二次业务。

## RT18 精确结算待办与延后证据债未闭环

**P0；CONFIRMED_EXPORTED_METADATA_JOIN_AND_RECOVERY_FILTER_GAP; DEFERRED_PER_ROW_CAUSES_PENDING。**

### 原始证据

- DIAGNOSTIC_SCOPE.json#/tables/822：v57_settlement_task_r36共10928，COMPLETED3308、NO_PREDRAW_CANONICAL_TERMINAL7457、PENDING162、RETRY_WAIT1；合计163非终态
- DIAGNOSTIC_SCOPE.json#/tables/765：v57_deferred_evidence_job共3099，COMPLETED200、PENDING1241、RETRY_WAIT1541、RUNNING117；合计2899非终态
- DIAGNOSTIC_SCOPE.json#/recentEvidence/25、/24、/22、/0均complete=true；逐target联查163非终态全部LIVE_RETRY、全部无当前shadow_observation行、全部有相同draw_hash的CONFIRMED_CANONICAL_DRAW；162无5000热区actual draw，仅202611270878实际开奖仍在
- DIAGNOSTIC_SCOPE.json#/recentEvidence/25/rows/3432：target202611160335，RETRY_WAIT，TRANSIENT_DB_BUSY，last_error=R914_FENCED_ORPHAN_RECOVERED:R914_SUPERVISOR_R912_CONTINUOUS_CORRECTNESS；其余162 PENDING的failure_class/last_error为空
- 运行脚本runtime_export_candidates/check_settlement_evidence.py所得settlement_evidence_result.json记录逐行路径；这些是整个SQL快照历史任务，不是本安装版本新产生全部数量

### 原因

结算监督器的已冻结补课只选存在immutable shadow及实际draw的任务；无冻结排除器只选BACKLOG。163条实际待办全为LIVE_RETRY且没有该shadow行，落在两种后台筛选之外；全源码未找到drainCurrent调用点，只有定义。热开奖表已淘汰162个对应实际draw而canonical hash仍保留，恢复不能把hash当号码。延后证据现有hash冻结读取、租约、core/action游标、短事务和catchup都已存在，2899非终态说明实际执行/准入/恢复仍有欠账；分组证据不足以断言1541全为同一异常或117都为失效租约。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java`
- `app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java`
- `app/src/main/java/com/ethk3/novaonepro/SettlementRecoverySupervisorR906.java`
- `app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java`
- `app/src/main/java/com/ethk3/novaonepro/OosAfterDrawWriterR36.java`
- `app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java`
- `app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceSnapshotR36.java`

### 保全

- SettlementEngine核心结算与研究/延后证据侧车隔离；MODEL_INVOCATIONS=0；正式结算唯一owner及draw_hash/lease_generation/process_boot_id栅栏
- 真实NO_PREDRAW终态不计预测胜负；原COMPLETED与历史7457无赛前终态不可批量重置
- DeferredEvidenceWriter原snapshot/hash、100 number/1200 action基数、cursor、唯一leaseOwner、幂等事务和结果读回

### 施工处理

1. SettlementRecoverySupervisorR906:42–73增加有界的全类别分类机会，复用现有owner；SettlementTaskStoreR36:1238–1303的BACKLOG过滤不能仅粗暴移除，先为LIVE_RETRY核对真实开奖确认、原冻结/归档引用、正式REAL义务、活动owner及lease。只有已证无原冻结且无未处理正式义务，才以独立不可变审计回执进入NO_PREDRAW排除；缺原件则显式SOURCE_MISSING待恢复。
2. 存在可恢复的原赛前冻结者，以原hash/原deadline证据恢复；所需实际draw从精确原始记录/归档读回，重算DrawHashR36并与canonical/task一致后，交SettlementCoordinatorR815:215与SettlementEngine:109–208原单owner执行。不得开奖后新算预测冒充原冻结。
3. 对2899延后任务导出target/snapshotHash/bytes/status/leaseOwner/leaseUntil/attempts/coreCursor/actionCursor/lastStage/lastError原件，按真实原因分类；RUNNING仅在lease过期或有同owner退出证明后重领，先核已提交事务游标再续，不能全表重置。
4. 保留DeferredEvidenceWriterR36:105–143的已开奖catchup、:229–309带租约hash读取、:312–445游标短事务、:461–573精确完成校验；使其有界调度机会在实时分析owner真正空闲时可达，并与RT06/队列公平修复配合。资源等待继续可重试，hash/基数损坏必须隔离不可伪完成。
5. 完成证据物化后先读回精确基数/每行hash/事件，再按存储owner原件引用合同归档；不得为减1.18GB表体积提前清除未完成snapshot_blob。

### 验收

- 原报告元数据联查脚本已运行，结算四状态和=10928、延后四状态和=3099；非终态分别163/2899，单项原路径可追溯。
- 施工后每个旧任务有独立分类和原件依据：完成、真实无赛前排除、原件缺失待恢复或合法重试；163不得无证清零，历史结果和质量分母不得变造。
- 重启/租约过期/事务返回不确定测试保证原游标不回退、同一target不双结算、100/1200与hash不被放宽；新增冻结与当前结算不因补课受阻。
- 观测连续窗口内非终态净减少和最老欠账年龄；不存在资源可用却同游标无限重试的静默状态。

### 边界

当前shadow热表无行不证明历史从未冻结，归档原件/其他正式义务需逐项核验；162实际draw未在热表也不证明所有归档丢失。COMPLETED3308的terminal_code为STRICT_ACTION_OOS_UNCALIBRATED，任务终态成功不能当概率已校准或正式资格通过。

## RT19 冻结快照缺主writer单调时钟锚，历史原件不能投影为当时状态

**P1；CONFIRMED_EXACT_SOURCE_CONTRACT_AND_REPORT_BOUNDARY。**

### 原始证据

- source-manifest.json#/mainWriterFaultsR342：observationState=UNKNOWN、reason=SNAPSHOT_CLOCK_ANCHOR_UNAVAILABLE、observerPid=16735、producerPid=null
- source-manifest.json#/mainWriterFaultsR342/historicalOriginalJson解码：producerPid=12219、processEpoch=c6696087f9f24e612944b156、sampleElapsedMs=330631073、validUntilElapsedMs=330643073；原字符串SHA256=643688bee90aee131e95026aa72f0169fea8fee7217e33dd71beb0679ac534fa
- 该投影明确historicalEvidenceScope=HASH_VERIFIED_ROW_FROM_IMMUTABLE_SQL_SNAPSHOT; NOT_CURRENT_MAIN_STATE，snapshotSha256=64bf0b335c6204de44da467026ce6d75b5218fbbb86bf8b5ff7cc340cce8b0a3
- MainWriterFaultProjectionR342.fromSnapshot:63–68明确传anchor=null；WriterFaultSnapshotViewR343:14–16说明当前exporter没有独立SQL pin时钟回执

### 原因

现有报告job仅记录snapshotWallMs，未独立记录真正SQLite读快照pin的单调时钟区间及生产者身份。WriterFaultSnapshotViewR343因此正确保留原始hash已核验历史行并返回UNKNOWN。RT12修复JSON截断只保全身份字符串，不会生成缺失的pin锚；用旧样本自己的sampleElapsed或导出时刻作为锚会循环自证/跨进程误判。

### 实际文件

- `app/src/main/java/com/ethk3/novaonepro/MainWriterFaultProjectionR342.java`
- `app/src/main/java/com/ethk3/novaonepro/WriterFaultSnapshotViewR343.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportNativeCP4.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java`
- `app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java`

### 保全

- 旧173方案R343041所保留的历史证据/当时证据/当前在线状态严格区分；UNKNOWN不改0/PASS
- WriterFaultSnapshotViewR343原hash、source、release、database、boot、TTL和pin区间全覆盖校验；报告后台native独立owner、无实时writer长锁
- 旧报告无锚时仍可导出原件和明确UNKNOWN，不能为了新附件缺失阻断正常导出

### 施工处理

1. 沿ReportExportServiceCP4:362–368、copyPinnedSnapshot:979–989与ReportNativeCP4.backup桥，在实际BEGIN+schema读取固定SQLite水位的窄区间独立取elapsedRealtime同域before/after；不要使用整个5GB复制时长，也不从sample本身推算。JNI/native回调需绑定snapshot capture ID并在最终hash可得后以原子sidecar绑定snapshotSha/bytes、source/release/database/boot、diagnostics采集PID/epoch以及主writer生产PID/epoch证据及其取得方式。
2. 将已验证sidecar作为可选Anchor传入fromSnapshot，校验旧样本sampled<=earliestPinMono且validUntil>=latestPinMono、时钟同boot同域、完整身份和回执hash；失败保留UNKNOWN及具体原因，不拉长12秒TTL掩盖陈旧。
3. 绑定成功最多输出OBSERVED_AS_OF_SNAPSHOT_INTERVAL；当前在线main健康另走validate的actualProducer+actualEpoch实时证据，不能把冻结SQL行当当前main内存。原先无锚的快照不可事后补造，应继续UNKNOWN或真正新采集快照。
4. 侧车读取/旧job兼容走现有manifest/附件路径，保持压缩包与share接口；新增采样失败只影响该投影状态，不影响用户拿到诊断包。

### 验收

- 空锚、错误snapshot/source/database/boot、进程epoch不匹配、过期样本、逆序pin及中途换owner均保持明确UNKNOWN；原historicalOriginal和SHA照常导出。
- 同一boot同一clock、独立pin区间完全在样本TTL内且身份匹配，才得到AS_OF_SNAPSHOT；延后导出和主进程重启不能升级成LIVE_FRESH。
- 大库复制超过12秒仍只用实际pin窄区间验证；取消/进程死亡不发布半个锚；重复导出保留旧SHA与原receipt。
- RT12与RT19分别回归：有效完整scope不自动使本项已验收。尚未实现native/Android补丁或真机验证。

### 边界

本次历史样本内容包括pending32等数据可用于审计历史时点与代码路径，但无法证明报告观察时主writer仍堵塞或仍在线。没有真实pin回执不能从其他较晚cohort.sampleMonoAt回推原冻结时点。

## 可实施代码合同与额外防回归要求

### 1. 报告依赖：限定同快照，保留同步owner

将ReportReadContextCP4.memo改为typed dependency result，键包括snapshotSha、sourceCommit、definitionVersion、依赖名和参数hash，只在本次ReportReadContext生命周期内有效，不能改static或跨新报告复用。状态SUCCESS/SOURCE_UNAVAILABLE/CANCELLED，失败记录稳定分类、errorRef、首次耗时、错误文本附件引用；不得缓存旧CancellationSignal/异常线程状态。全局用户取消不能负缓存为普通失败。将ModelEffectivenessAnalyzerR35.analyzeSnapshotCP4按共同事实拆分，coverage子事实失败不能抹掉已有模型/数据集事实。ReportCheckRegistryCP12为每项只声明真实依赖，所有96项都要保留自己的结果记录。

runCheck目前在报告owner线程同步调用runner；此项返回后finally清理，再进入下一项，不存在cancel Future后继续共享连接的现成缺陷。最小返修保留此事实。若未来做异步，必须等worker实际退出且cursor关闭；Future.cancel(true)不能作为SQLite已停止的证明，无法确认就终止本reader并重新打开只读连接。

### 2. 超时：独立预算加持久进度，不把其余检查压成1秒

移除180秒共享扣减导致的后续1000ms预算；每检查/依赖按实测配置独立预算，仍保留已有阶段上限、有进度才续租。全局工作片耗尽时保存已验证的检查结果和未开始集合，进入已有PAUSED路径。下次按原snapshotSha、源码、registry hash、定义hash、产物hash恢复，以原96项顺序装配catalog，同ID仅一个正式结果；未开始状态不能写READ_ERROR。

小型check-progress记录建议由现有job owner管理，包含jobId、snapshotSha、registryHash、checkId、definitionHash、dependencyKeys、state、resultFile/resultSha、attempts、duration、cancellationAck。result文件先完整写/同步/验证后才推进progress。单个来源失败可产生明确失败结果并继续独立项。禁止仅增大超时，必须先修RT01/RT02。

### 3. 快照释放：精确替代证明，恢复原请求

ReportPublicationCP12.pruneTerminal中FAILED+snapshot.db不能改为直接delete，应登记“恢复待办”。先导出小型job状态附件：jobId、stage、snapshotHash/bytes、原request profile、publication proof、active readers、cleanupReason。该可选附件失败不能阻塞核心导出。

DIAGNOSTIC失败任务复用原快照恢复成完整所请求范围的诊断包，保留原失败日志，验证ZIP/manifest/语义回执后通过已有READY清理规则释放workspace。FULL_RAW必须真的包含原数据库字节或有另一个精确同hash同字节持久副本及读取校验；不得用后来不同hash的新报告替代。清理时核验记录至少绑定：旧job、旧snapshotSha/bytes、profile、replacementJob、replacementManifestSha、semanticReceiptSha、requestedCoverageVerified、rawSnapshotCoveredWhenRequired、activeReaders=0、许可状态。

取得share排他锁后重新核job revision/hash/profile，持久化retire intent，只清许可对象，测量regular/allocated实际减少量，写完成收据。中断后按同许可幂等续作。最终ZIP、index/manifest、发布回执、分享控制保留。READY不会因cleanup失败降级FAILED。没有证明的保护对象必须列出具体恢复方法，不能无限静默保留。

### 4. 故障观测：原件可恢复，公平槽真正可达

WriterHoldFaultsR31已解锁事件有稳定payload/hash；先进入有界durable spool，借既有RESEARCH_CONTROL通道建立冷原件locator，而不是与永不封存的mutable故障共享全表热配额。mutable生产者采用“旧revision封存，新revision接后续occurrence”；旧原件hash与收据读回一致才允许retire。不能直接把deferResearch改false或扩大MAX_PENDING遮住堵塞。

公平准入必须穿过ResearchWriteQueueR27.submitAttemptR31前置critical gate和DatabaseCoordinator.runWrite两层。真正临近截止的freeze仍优先；笼统realtimePressure或过期积压不应永远排除已预处理完的后台小写。每槽使用原单writer短事务，计算留事务外，commit/readback完成才推进owner游标。

## 候选补丁与真实验证结果

- runtime_export_candidates/coverage_scope_join.patch：仅改coverage第一条SQL驱动顺序，原过滤/min语义保留；对精确211源码git apply --check通过。
- check_coverage_join.py：SQLite3.53.1、50,000事件、100组scope返回等价；EXPLAIN候选先scope release索引，再event_id主键。不是Android SQLite或5GB真机性能证明。
- heartbeat_scope_identity.patch：仅改完整scope字符串赋值，不改其他展示safe；git apply --check通过。
- check_scope_identity.py：原件300字符非法JSON复核、候选源码合同、5类字符串输入往返；合成433字符scope保全。没有运行Java/Android。
- check_settlement_evidence.py：对原DIAGNOSTIC_SCOPE完整元数据做精确target联查，163结算非终态全为LIVE_RETRY、全无当前shadow行、162无实际draw热行；原文件hash和逐行JSON路径写入settlement_evidence_result.json。只读证据复核，没有改业务任务。
- 其他修改属于逐文件施工合同，不假称应用修复已编译、APK已生成。

## 专项自检

19项均有原始证据、确定程度、实际文件、保全、修复和验收；全部source_files在精确211工作树存在。未将全部40超时归为同一SQL，RSEC010有renderer/hash、RSEC018有observability SQL。未将历史7319异常当本版本崩溃，未将历史32故障队列当现在状态。未将1秒调度目标当网络端到端SLO，未将draws5000当全库容量已受控。没有无条件清空数据库/受保护原件方案。Android编译、覆盖迁移、完整报告连续导出、目标设备24/72小时、自然未来样本与质量均待施工和真机验证。
