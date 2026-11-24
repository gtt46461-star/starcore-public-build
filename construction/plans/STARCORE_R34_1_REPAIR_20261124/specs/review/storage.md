# 存储、导出和实时并行施工补充

适用应用底座 `5562f9b19aa09f9f7d9e69ca085db56c537967fc`。以下入口均在 `app/src/main/java/com/ethk3/novaonepro/` 下。沿用原工单和签收编号，场景是该票新增的必测断言。测试数据建于隔离安装或副本，收据初始为待执行。

## 现场数字限定了可行方案

| 来源 | 确认的事实 | 验收应回答的问题 |
|---|---|---|
| `STORAGE_CENSUS.txt` | 同一次采样的文件长度 4,344,863,006 B，分配块 4,358,670,848 B。现场主库 1,432,539,136 B，本次报告私有快照 1,427,210,240 B。数据库类别含 1,336,406,139 B 旧备份。 | 导出前、快照封存后、发布后、清理后各自实占多少？单次采样无法推出两天增长速度或导出结束后仍留有这份快照。 |
| `index.json` 与 `source-manifest.json` | 一个已导出的内层 ZIP 共 11,719,107 B。报告配置为 COMPLETE_DIAGNOSTIC_R68，未包含原始数据库等对象；该任务 `resourceObservationB01.state=NO_REJECTION_OBSERVATION_IN_THIS_JOB`。 | 本例属于已交付的诊断报告。重建导出失败、争用和空间不足须用独立故障注入或后续现场原件。 |
| `runtime-observation-crashSummary.txt` | 累计 crashCount=2、oomCount=0；记录的最近异常在 F01_DEFERRED_MATERIALIZE，故障积压下的写入准入回滚。 | 保留应用进程/诊断进程各自的运行身份、堆/PSS与错误类型。磁盘占用、Java 已用堆、PSS、系统可用内存分别取样。 |

## R341-B10-01 / A-B10-01a、b：让现有 1.43 GB 数据库可以合法导出

原规范“普通报告单次增长 128 MiB、未发布报告工作区 256 MiB”如果按整项任务一刀切，会在快照创建之前拦截同一现场。现有 `ReportExportServiceCP4.drain → reserveDisk → ReportNativeCP4.backup` 先做事务一致快照，然后才分页读、创建归档及分卷。提高压缩率或减小交付卷无法减少这一步的预约。修订这两条限制的作用域如下。

1. 128 MiB 用于普通派生数据一次增量准入，256 MiB 用于并存的可重算普通工作集。为诊断和明确请求的 FULL_RAW 的一致性快照设**一个物理预约**，按原来源主库与 WAL 的字节数、页开销、真实可用空间及同卷其他租约批准；工作区内快照不能又作为普通派生字节重复计算。已有完整快照只核销其已占实物。同步修改 `StorageBudgetR31.reserveOnceR31/ensureAdditionalOnceR31`、本批拟新增的 `StoragePolicyR341`、`ReportExportServiceCP4.reserveDisk/ActiveControl.beforeBackupChunk/settleBackupChunkR31`。目录分类以写入意图和对象为准，同一路径下的快照、可重算附件、归档、可下载卷应落在同一物理总账的不同子账。
2. `ReportPhaseBudgetR31.archiveFuture` 与 `ReportExportServiceCP4.ActiveControl.reserveArchiveWorkspaceR335` 按实际并存的归档、ZIP、分卷分别预约峰值。FULL_RAW 交付物可大于 256 MiB。通过已有 `ReportPublicationCP12.prepareBoundedZipR30` 生成带顺序、总份数、字节和哈希的卷。已验证的旧卷、恢复中的临时快照、其他活动预约及原规范保留的结算余量均参与可用空间核对。只在具备剩余物理空间时接新任务。取消或崩溃后凭旧 owner 租约、文件身份及现存字节对账，不能把残留文件同时当已回收和待重写。
3. 若无法安置新快照，报告 UI 在落盘前显示阶段、需要/可用/已预约字节、可重试条件，并保留 requestId。若存在此前已验证的报告，允许读取该报告并显示其原采样时间。新的报告必须拿到自己的采样证据；不能默认改交一份旧报告或改成不同配置。持久记录同一请求的状态，由既有调度器退避，避免同一故障叠加多个快照。
4. 生产入口验收至少四组：以 122 实测尺寸的可读 SQLite 库及 WAL、足够剩余空间，普通导出与 FULL_RAW 先后走 `requestExport` 至发布/回读；可用空间少于下个阶段峰值，验证只拒绝该增量且仍可结算；两个并发请求+进程在原生复制中死亡，恢复后没有双重费用或双份未登记快照；已核验快照可复用与损坏快照须重取分别执行。大库必须有实际分配页和可验内容，稀疏占位或仅模拟大小无法签收回读。保存阶段峰值、预约流水、宿主实际分配块、报告配置和 ZIP 成员比对。

## R341-B10-02 / A-B10-02a、b：失去索引后优先恢复，校验前不清唯一证据

`ReportPublicationCP12.verifyPublishedZip` 遇缺少 delivery/index.json 或不识别的格式会返回 false。`cleanRawOwned` 当前只在 true 分支删除 archive 内的 P 分片，之后仍执行 `cleanChildren` 清理快照和原始工作区。`requireRawCoverageR31` 单独读成员清单，不能证明替代交付物仍可读取。另一个入口 `pruneTerminal` 已保护失败时唯一的 snapshot.db，`ReportBudgetSU2.abandoned` 在本底座返回 false。两项保护都要有回归。

- 在 `ReportPublicationCP12.cleanRawOwned/requireRawCoverageR31/pruneTerminal` 及 `ReportCleanupR31.runDue` 统一核验“候选对象、替代件、实际读回结果”。完整可识别 ZIP 检查参与保护的成员及 hash/bytes；旧格式走既有 `ReportArchiveVerifierCP12.verify` 验证其真实分卷及成员。index 丢失或未知格式进入保护待修；可从可验证原件重建索引，再执行同样回读。任何不确定结果保留快照、分片和任务目录，显示具体失败原因，后续健康任务继续。
- `ReportPublicationCP12.jobPhysicalBytes` 当前调用 `StorageBudgetR31.scopeBytes`，后者只返回 regular-file length。清理回执的 `beforePhysicalBytes/afterPhysicalBytes` 应改为有明确定义的 logical 与 allocated 两组读数，并标注测量时点与未知状态；实际回收量按同一文件系统的分配块另计。稀疏文件、DB 自由页与压缩后文件分别覆盖。
- 在同一份有快照的 READY 任务上逐次移去索引、破坏 ZIP 内容、改成未知格式，并执行正常 cleanup owner。断言原始对象 hash 保留、已发行状态独立、cleanup 为可重试并有原因。修复原件后仅清一次。再放入旧格式可验证包、损坏 job.json、失败的唯一快照、仍持有分享/取证租约的目录，分项核对保留/清理结果。维护前后保存相对路径、大小、块数、哈希、重试游标和删除理由。

## R341-B11-06 / A-B11-06a、b：导出全过程包含后台竞争和手机保存路径

- `ReportExportServiceCP4.ActiveControl.shouldContinue` 的实时压力分支当前仅对非 FULL_REPORT 模式生效；DIAGNOSTIC 与 FULL_RAW 都走 FULL_REPORT 模式。生产 owner 在已有 journal 和压力租约上，为可分段的索引、压缩和读回登记有界让行点。已封存快照保持同一身份，恢复前复核页/成员边界，释放过期资源票；不在持数据库写锁或单个不可续跑的 native 调用中虚报暂停成功。若备份尚未完成，使用 SQLite 支持的一致性恢复方式，无法保证同一读视图就显式作废本次未封存输入、同请求重新采样；不得混合两个采样时点。对持续压力记录报告的等待年龄、允许进度窗口和终态，避免只把饥饿从出号移给导出。
- 经 `runSafRequest/runSafTreeRequest/verifySavedTreeParts` 和 `ReportSafCopyCP12.copy/verifyReadback` 走实际 UI 保存路径。本地 READY、文档提供方写完、目的地逐卷与 index 回读成功分别记账；持久化权限丢失、ENOSPC、目标短写、损坏、取消、进程死亡分别生成状态。有问题的目的地继续可选新的保存位置并复用已核验的源任务，不重新采集同份报告。报告正文分清“成员目录已读、设备上原件存在、ZIP 实际含该字节”。
- 工程场景同时从服务运行冻结/结算与发起完整导出，跨两个压力窗及一次进程重启，保留按 targetId 计算的全部机会、冻结截止、结算耗时和每个导出阶段的 bytes/duration/park/resume；以同种负载且不导出的基线对照。诊断单卷或 FULL_RAW 多卷均做手机保存回读；目的地故障时源 ZIP 的 hash 不变。工程夹具独立签收，本机 24/72 h 的后台影响仍在 B12 按真实 APK 判定。若释放原件稍后失败，不能反写已核验的发布/保存结果。
