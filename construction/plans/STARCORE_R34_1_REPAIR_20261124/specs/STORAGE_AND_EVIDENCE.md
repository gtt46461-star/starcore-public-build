# R34.1｜存储、原件、时间范围与报告事实契约

本规范约束B10、B11以及B12的证据交付。当前核验源码为`5562f9b19aa09f9f7d9e69ca085db56c537967fc`，安装版本码`577311122`。文中的新类、新表、字段和阈值均是施工设计；不代表当前APK已有实现。

## 1. 现场基线与不能混淆的口径

| 项目 | 122现场事实 | 施工含义 |
|---|---|---|
| 普通文件总量 | 4,344,863,006字节 | 这是磁盘文件长度，不是进程内存或allocated block总量 |
| DATABASE | 2,769,021,715字节 | 已包含旧备份1,336,406,139字节，不可重复相加 |
| REPORTS | 1,506,770,160字节 | 分开审计唯一报告原件、已验证副本、未完成工作目录 |
| 当前snapshotDB | 1,427,210,240字节 | 单次完整复制会提高峰值；压缩/报表必须预约临时空间 |
| 图表缓存 | 118,105行，122,822,656字节 | 属于可重算类别也要保留当前有效帧与调查固定帧 |
| writer_hold | 75,410行，51,617,792字节 | 先有原事件归档/可回放证明，再截短SQL热集 |
| 开奖热表 | 5,000行 | 行数不证明连续性、标签完整性或历史特征当时可用性 |
| 检查点审计 | 657根、677对象：103验证、556缺失、18未字节验证 | 缺失与尚未验证分别处理；574子集未知，闭包不成立 |
| 观测完整性 | 目录census未截断；dbstat细表有1024项上限 | 不得由目录完整推断全部表空间明细完整 |

以上来自输入ZIP成员`STORAGE_CENSUS.txt`、`source-manifest.json`、`r32-checkpoint-observations.jsonl`。当前报告约8MiB的检查点读取预算不能支持对未扫描对象的健康结论。

## 2. 预算模型：保护性负担和新增增长分开

源码`StorageBudgetR31.Config.production()`设置`capacityDriven=true`，`reserveOnceR31`与`ensureAdditionalOnceR31`只有在非capacityDriven模式才强制总量20/30GiB常量。因此本次不能仅修改两个常量就宣称限制生效，也不能切换成1.5GiB总量门禁后封死已存在4.34GB的安装。

### 2.1 分类与测量

每个物理文件只有一个storageCategory；硬链接或多个逻辑引用不能当多个物理副本重复计量。优先使用已核验的文件身份与目录边界；无法证明同一物理对象时保守记录不确定性。`StorageCensusSU2.measure`继续在SQL锁外遍历，保留取消、符号链接和目录项上限检查。

```text
MeasuredStorage = {
  censusId, capturedAt, directoryComplete, dbstatComplete,
  logicalFileBytes, allocatedBytes: integer|null,
  categoryBytes, protectedDebtBytes, recomputableBytes,
  pendingReservationBytes, physicalFreeBytes, measurementErrors
}
```

`protectedDebtBytes`只是保留历史原件的会计分类，不是删除清单，不从真实磁盘总量中扣掉后假装空间空闲。SQLite/WAL、暂存副本、报告分卷、模型文件、压缩candidate及旧backup都进入实际物理测量。未知allocated保留`null`；使用logical估计时字段名称和说明必须明确。

### 2.2 初始工程策略与准入计算

以下是明确可编码、可版本化的初始工作集政策，不是现场已达到的数字：

| 工作集类别 | 上限 |
|---|---:|
| 可重算图表缓存 | 128MiB |
| 未发布报告中可重算的普通派生工作集 | 256MiB |
| 可重建未发布模型/检查点 | 512MiB |
| 可重建诊断索引及派生统计 | 256MiB |
| 其他可重算工作集 | 384MiB |
| 合计 | 1.5GiB |

唯一报告、唯一恢复备份、冻结输入、模型原件、未来观察、签名/资格原证据与UNKNOWN对象不进入自动驱逐池。类别不明时不能把对象强行标为缓存以满足预算。调整这些数值需增加`policyRevision`及测量依据，不改变保护类别。

普通写入要求写后物理空闲至少`256MiB + 64MiB`，其中后64MiB为关键事实/结算应急余量；关键写入最多消耗该64MiB，仍需保持256MiB底线。不能把“critical”作为无限空间豁免。对数据库写入，B02统一writer在取得写锁前调用B10新增admission接口；使用已登记操作的最大增量/页预算并在提交后记录DB/WAL实际变化，超出预算的可选大写先分页。不能持有SQLite写锁再扫描磁盘或等待回收。

```text
availableForClass = physicalFreeBytes - floorForClass - otherLiveReservations
allow = requestedMaximumGrowth <= availableForClass
        AND (criticalBoundedWrite OR verifiedSnapshotLease OR categoryProjectedBytes <= categoryLimit)
```

`DEBT`表示历史/当前可重算工作集超过目标：停止该类别新增可选大任务并触发有界回收；关键结算和现有运行不因历史保护负担直接停机。128MiB 限普通派生数据的单次增量。SQLite 一致性快照先按 DB、WAL、开销、其他活动预约和结算余量单独申请同卷临时租约。分卷发生在快照之后。空间不足保留原件并可重试，具体计算与故障用例见 [存储与导出复核](review/storage.md)。

每个可增长任务使用现有`reserve→beforeChunk/ensureAdditional→reconcile→close`协议。预约不能替代实际测量；任务取消只退还未使用增长额度，已落盘文件必须按其身份另行处理。重启时仅收回已死亡OS租约的剩余额度。过期时间不能独立证明所有者死亡。

### 2.3 新接口

`StoragePolicyR341.evaluate(Census, Reservations, WriteIntent)`输入operation/category/criticality/protectionClass/maximumGrowth/sourceScope；输出`ALLOW|DEFER|REJECT`、policyHash、reason、remainingGrowth。调用点是`StorageBudgetR31.reserveOnceR31`与`ensureAdditionalOnceR31`；数据库准入由B02的统一writer接入同一接口，不复制第二套锁策略。

`StoragePolicyR341.reconcile(Reservation, ActualWrite)`保存估计/实际差额及持有者状态；无副作用地计算下一次准入。所有记录追加或CAS更新同一reservation；同一个reservation不能被两个任务重复消费。

## 3. 原件分类、清理与托管

| 分类 | 必需证明 | 自动操作 |
|---|---|---|
| PROTECTED_ORIGINAL | 唯一证据、有效冻结依赖、恢复原件、签名/资格原件或人工固定 | 不自动删除 |
| VERIFIED_DUPLICATE | 内容hash、字节数一致；替代件仍可读；引用与reader lease完备 | 可回收冗余副本，保存回执 |
| RECOMPUTABLE | 有确定输入/算法版本/重算入口；不包含唯一时间原件 | 可按预算分页回收，保留最新有效及固定对象 |
| UNKNOWN | 尚不能证明属于前三类 | 按保护处理，继续调查 |

现有`ReportPublicationCP12.pruneOldReports`按最近份数/7天、`enforceQuota`按总量/8份触发`deletePublished`。共享锁只能证明此刻没有读者，不能证明报告不是唯一故障证据。B10必须让自动清理先核保护分类；当前施工不授权删除旧唯一证据或旧签名。

清理事务为`CANDIDATE→CLAIMED→RECHECKED→UNLINKED→RECEIPTED`：claim必须CAS、短事务；hash/文件I/O在SQL锁外；unlink前再验证引用、pin、替代件可读及文件身份。未成功unlink不得记reclaimedBytes；unlink后回执失败重试不能重新扣预算。物理释放数由完整目录测量或对应对象实际allocated差值提供；SQL删行数不是物理释放量。

`ReportCleanupR31.runDue`保留“报告发布”和“工作区清理”独立状态。FULL_RAW私有数据库只有确切同hash/bytes的snapshot进入已验证交付件才能清掉；DIAGNOSTIC摘要/路径索引不能当原件托管证明。取消未发布任务时也必须检查它是否含唯一恢复原件，不能泛删整目录。

writer_hold等历史事件采用既有归档原件协议：冻结页→写暂存件→校验内容hash/计数→发布→短事务裁剪热表。SQL每页≤500，锁时预算沿用B02；缺归档证明则保持原事件。需要更长历史时读归档，避免SQL热表无限增长。

唯一旧备份只能在恢复验证、保管状态与用户授权处置齐全时退役。主动删除按钮仍需保留现有明确操作含义；本方案不安排自动按年龄退役原件，也不安排把同一份原件无限复制来假装保护。

## 4. 检查点恢复与GC隔离

`RecoveryLedgerR341`为新组件，使用以下状态：

| 状态 | 含义 | 对依赖者 |
|---|---|---|
| MISSING | 预期位置无对象 | 隔离该谱系，允许无依赖的新谱系继续 |
| AVAILABLE_UNVERIFIED | 有字节但预算/权限等使核验未完成 | 不作为已验证祖先 |
| VERIFIED | hash、大小、对象格式、引用身份均核验 | 可由既有资格流程继续消费，恢复本身不授予资格 |
| MISMATCH | 找到的对象不匹配预期 | 另存冲突观察，禁止覆盖原身份 |
| UNRECOVERABLE | 已知授权源探测完仍无原件 | 保留历史负债，后续有新intake可重开恢复 |

建议新增表`r341_recovery_ledger`，主键`(object_hash, attempt_id)`，字段包括lineage、state、sourceLocationRef、observedAt、expected/actual bytes/hash、reason、evidenceRef、nextRetryAt。`object_hash`与`attempt_id`非空，hash按格式校验；索引`(state,nextRetryAt)`及`(lineage,observedAt)`。通过B03既有按对象schema/migration入口安装，不运行一次性全库DDL。每次恢复页≤32对象；读取最多8MiB一个slice，未完成留游标并退避，不让扫描占据实时写锁。

`CheckpointRecoveryR335.step`读取已授权备份/intake，执行temp流式复制、hash/格式校验、fsync、atomic publish，再由短事务调用`registerVerified`并写恢复账本。同objectHash重试幂等，正在被另一所有者恢复时等待租约；冲突字节不覆盖。找不到原件不能补造文件或复制相似模型。

GC沿用`CheckpointGcReachabilityR324`局部闭合分量证明。未知子集不代表零子对象；claim后出现新pin/ref必须阻止unlink。某个历史分量缺失不应阻断另一个已证明无根、无pin且可删副本的回收。工程签收需证明恢复/隔离/GC机制和真实负债清单准确，不要求找回所有不可能取得的556个对象。它们依赖的旧资格仍保持不成立，新候选不得继承旧future计数/证书。

## 5. 长历史连续性、冻结引用与时间可用性

`HistoryContinuityR341.inspect`按现有`TargetIssueResolver`后继规则遍历有序开奖页；不能用整数加1跨日，也不能只验证页内而忽略前后页衔接。输出每段first/through、count、缺口范围、重复/冲突、dirty来源与分页cursor。

当前`research_long_head_r80`的`INVALID_SOURCE`、through`202611230191`及`SOURCE_GAP:202611230247:202611230257`保留为旧epoch事实。补取须经过B04唯一开奖事实owner接纳，记录sourceHash/receivedAt/acceptedAt；缺权威来源时从可证明的连续段建立新epoch，窗口不足的长原子等待，短窗独立研究继续。

`LongScaleSummaryR80.prepare/commit`对token/generation/epoch做CAS，生成新snapshot和真实availableAt。恢复模式为`RECOVERED_AVAILABLE_NOW`；旧availableAt和旧INVALID_SOURCE不被回写。`LongSummaryBindingsR80.historical`仍要求源当时已可用，旧frozen仍引用原epoch。修好历史标签只能用于当前以后或明确离线研究，不能制造过去的预报/未来样本。B08新研究dataset与新freeze使用新epoch，不静默修改旧样本。

## 6. 物理压缩与崩溃恢复

复用`StorageMaintenanceServiceR82`/`StorageMaintenanceFileR82`。压缩不在Activity启动或实时出号事务执行；先取得maintenance lease，排空/拒绝新的不兼容读写，登记`MAINTENANCE_PAUSE`及受影响机会。既有backup未完成处置时不覆盖它，再次请求返回`BACKUP_CUSTODY_PENDING`。

峰值预约包含当前源、candidate、旧backup、WAL/sidecar及回执余量。`journal→candidate→fsync→关闭句柄→rename/swap→目录fsync→重开校验→完成回执`任一步失败都可从journal恢复；不能同时删除新旧库。验收覆盖swap前后杀进程、候选写失败、磁盘不足，检查integrity、关键表计数、保护原件hash、权限和新自然目标的恢复推进。成功压缩不自动授权删除旧backup。

## 7. 统一ScopeEnvelope

所有报告指标、图表、工单回查附以下身份：

```json
{
  "scopeId": "sha256(canonical scope fields)",
  "installedVersionCode": 577311122,
  "sourceCommit": "5562f9b19aa09f9f7d9e69ca085db56c537967fc",
  "planRevision": "R34",
  "processEpoch": "observed process identity",
  "bootId": "observed boot identity",
  "role": "MAIN_CANONICAL|SHADOW|REPLAY|SYNTHETIC",
  "windowKind": "CURRENT_PROCESS|CURRENT_INSTALL|HISTORICAL",
  "targetFrom": "full issue string or null",
  "targetThrough": "full issue string or null",
  "snapshotAt": "SQL freeze time",
  "runtimeObservedAt": "runtime sampling time",
  "fileObservedAt": "file census time",
  "sourceSnapshotHash": "hash or null",
  "identityStatus": "VERIFIED|UNVERIFIED_IDENTITY"
}
```

以上JSON只是当前输入身份示意；新APK必须使用实际新版本/commit，不能继续硬编码122。**versionCode尾号122、图表targetIssue尾号122、历史窗口到某一期122是三个不同字段。** 不得因为字符串都出现122就连接成同一当前对象。当前进程10:11:43启动、约10:17:04运行采样、后续报告捕获、SQL冻结137各自时间保留。

跨scope统计必须显式声明连接条件与范围。历史03:05:56的crash不计为当前进程crash；旧FTRL的13LEARNED不计新候选训练；旧合成Q08不成为自然完成；shadow不计主路canonical。旧诊断原文不改写，但新增投影必须显示`STALE_UNRECHECKED`，不是沿用历史LIVE。

## 8. 源状态、数值和界面三态

```text
SourceObservation = {
  sourceId, queryId, scopeId,
  state: READBACK|OBSERVED_EMPTY|SOURCE_UNAVAILABLE|TIMED_OUT|CANCELLED|PARTIAL|STALE,
  value: number|null, rowsReturned, totalRows: integer|null,
  totalKnown, truncated, reasonCode, errorRef, observedAt
}
```

`ReportSourceStateR341`把底层结果合并到displayState，但不丢reason：

| displayState | 条件 | 示例 |
|---|---|---|
| HAS_DATA | 至少有合法当前数据 | 真实0可显示0；部分成功同时标PARTIAL及缺源 |
| EMPTY | 查询成功且当前查询范围确实零行 | 显示等待哪一生产步骤，不猜全表总数 |
| UNAVAILABLE | 缺表/读取失败/超时/UNKNOWN/失配/只剩旧帧 | 不画0线；显示具体原因和source scope |

`fromSchemaPending=null`保持未知。`ReportReadContextCP4.scalarLong`现有严格类型/null校验不应改坏。计数只有成功COUNT结果才可为0。分页32条/120点限制不是全量分母；`totalKnown=false`时`totalRows=null`。页为空只对该页/过滤条件作结论。

错误聚合遍历所有子source；按`queryId+scopeId+errorCode`去重但保留occurrences，不通过外层catch是否触发计算错误数。本次3个no such table必须出现在顶层sourceHealth及可点击明细。`evidenceCoverageComplete`、`checksComplete`、`sourceHealth`、`allChecksPassed`独立；96项都有状态可true，健康仍可FAIL/UNKNOWN。

图表页面仅订阅服务快照，`ChartPageReadR34`保留逐图deadline与页面取消，单图超时不清空其他图。18研究图/20矩阵的当前帧0是生产问题，29旧图UNKNOWN是证据状态；16逻辑页8页空不等于8张底表空。对象与根因/工单是多对多关系，不把165索引对象当165独立故障。

## 9. 工单生效回查与报告发布范围

`RepairEffectLedgerR341`新增安装回执及追加式观察。建议`r341_repair_install`主键`install_identity_hash`，`r341_repair_effect`主键`(issue_id, source_commit, process_epoch, scenario_id, evidence_hash)`；引用真实build identity、effectStart时间/目标期、input/output hash及producer/consumer。静态工单元数据来自封存计划资产，报告读器无权写PASS；生产者只追加观察，CI工程回执与device现场回执分开。

工程通过、现场待验、自然待验是独立字段。旧进程成功不能关闭新安装故障；修复生效点以前的累计值不能满足本次验收。错误签收通过追加REVOKED及原因撤销，不能删除原观察。

报告交付范围为：

| 模式 | 可声明 | 不能声明 |
|---|---|---|
| DIAGNOSTIC | 已声明语义目录/检查项完整；实际附带文本/索引hash可复验 | 原DB/APK/模型/归档二进制已完整提供，或可独立重算全部模型 |
| FULL_RAW | 只有ZIP实际成员中存在的确切hash/bytes原件可声明完整 | 仅有路径引用/外部manifest就当字节已打包 |

`ReportSemanticVerifierR31`校验成员与声明；FULL_RAW缺件则拒绝发布，不静默降级。报告首页列snapshot/source/runtime各自时点、未包含原件和不能验证项目。manifest先列成员hash、外层ZIP最后计算；manifest自身hash放外置seal/回执，防自引用。原始122ZIP保持不变，新说明以伴随证据形式加入仓库。
