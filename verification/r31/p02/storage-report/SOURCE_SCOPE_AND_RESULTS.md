# P02 B05 存储/报告交接：真实源码与证据边界

本交接源基线 `5237b265a27fe5ad27fd842901c445d5f8c27749`，继承已通过 P01 的 `844a78488f396189c2db98591782b7bb214799d2`。分支 `construction/r31-final-20261104`。本 agent 不提交、不推送、不执行本阶段 Gradle；root 统一构建、提交及交付。正式全 App、签名、APK、真机、24h/72h 状态以 root 后续真实结果为准，这里没有这些 PASS。

最终冻结点是 `final-freeze/source-freeze.json`。16 项与全部 sink host 通过后，仅 `ReportIoR31`、`ReportJobJournalCP12`、`ReportExportServiceCP4` 增加恢复 ZIP 的 UI 旗标取消，三源重新真实 javac，并独立执行该目标测试。不是把修改前报告当作全 App 最终验证。

## 实际修改

- `StorageBudgetR31`：跨 PID admission.lock + 持久 SHA256 binary ledger + 每 reservation 的 OS 生命周期锁；最大64 active/64 receipts，bounded metadata。owner/fence/generation 检查；取消、容量、缺测、stale 有共享 ClassifiedFailure 分类。不能仅按墙钟 expiry 释放活跃 holder。真实退出后完成/恢复 reservation，保留物理文件。保留 2GiB 常态、4GiB transient、256MiB free floor。缺实际 allocated blocks 的生产准入拒绝，未覆盖 producer 不被写成受统一治理。
- `StorageCensusSU2` / `DirectoryCursorR31`：HOST/API26+ DirectoryStream 深度优先流式枚举，不先 listFiles 分配整目录；200000 entry/depth/cancel 上限，API21–25 显式 unsupported。每个文件长度在总量、类别和 scope 中使用同一次观察。Android Os.stat st_blocks*512 独立度量文件与目录，HOST 不支持显示 UNKNOWN。副本 PRAGMA/dbstat 与 live 文件、free space、heap 分开；dbstat 可取消、不支持不写零。
- `StorageLedgerR31`：固定288点和720小时槽；boot/monotonic/wall/PID/阶段、缺测、regular/allocated/free/reserved 独立记录。root 已在 HealthTask 事务外接 sampleIfDue；此调用源码接通不等于已在设备运行。
- `ReportIoR31`：显式 try-with-resources 线程作用域只用于把既有同步回调路由给当前 report owner，跨 PID 权威仍是文件 ledger/lock。报告区外 metadata、独立 legacy stream 和恢复作业真实申请有界 grant；已知 metadata 在 AtomicFile 替换旧版本前全量准入，每写入检查已准入子区间。作用域和实际 stream/FD 关闭后释放。HOST fixture 注册显式标识，不是 Android 证据。
- `ReportPhaseBudgetR31` / `ReportExportServiceCP4` / `ReportAttachmentClosureCP12`：256MiB 是有界 initial metadata envelope，绝非原件 worstcase。复制前按冻结 descriptor 的确切 bytes/hash、已经存在的固定副本及缺失 copy bytes 请求下一阶段；压缩前按确切 member bytes/count 请求 archive、ZIP、分卷并存的保守上界。申请失败明确停止，不截断后宣称完整。临时 grant 的扩展仍受总容量/free floor约束。
- `ReportNativeCP4` / `report_native_cp4.c`：保留原入口、source READONLY、100页分片和4/32MiB。每 backup chunk 写前 callback；按完整尚未分配的固定快照而不是本次100页估算最后提交/缓存刷盘。JNI callback 异常真实关闭 SQLite/文件后返回。没有改 vendor SQLite/Zstd 数学。
- `ReportArchiveCP4` / `ReportArchiveVerifierCP12` / `ReportPublicationCP12`：archive/manifest、ZIP、分卷、legacy manifest segment 的实际 sink 接准入；原 CRC、SHA、完整验证/发布顺序和旧入口保留。quota 增加全 report root/临时部分的观察，不把移动目录算释放。
- `ReportAttachmentClosureCP12` / `ReportCatalogCP4` / `ReportDiagnosticEvidenceR64` / `ReportMemberPlanCP12` / `ReportReadContextCP4` / `ReportTextCP12`：冻结 body/计划/refs、catalog每页、诊断输出、外排run/merge/dedup、renderer spool/manifest均实际接 sink。仓库没有 FileExternalSort 文件；真实外排 owner 是 ReportMemberPlanCP12。
- `ReportJobJournalCP12` / `ReportTaskControlR69`：报告 metadata 的原子写前准入；恢复 ZIP 独立 grant、活跃 writer share pin、原件/CRC/SHA；可选 BooleanSupplier UI取消兼容旧API，输入/输出 chunk、回读、发布前检查。已实际收到完整新增观测后，full observation refs 固定上限24，单个 spill固定8MiB，超限是明确失败而非静默裁剪。
- `ReportCleanupR31` / `ReportBudgetSU2` / `ReportPublicationCP12` / `ReportInputsLeaseCP12` / `DiagnosticReportProviderR923`：READY 与 CLEANUP_PENDING 独立；同 owner 在自身 input/native/stream scopes 退出后重试，OS alarm 可在无新导出时触发下一轮。坏记录隔离，有界 backoff；FULL_RAW 必须有确切 snapshot member + 完整 delivery hash 后才能清理。唯一未验证 snapshot/原件不按 TTL 删除；活跃 share / recovery PFD（到实际close callback）保护。报告作用域以外 P06 GC、mainline/choice 等未启用。
- `ReportExportServiceCP4`：backup32MiB/decoder4MiB显式接每PID MemoryBudget，summary读取上限1MiB；释放在实际 scope 退出后。已验证 READY 的 ack/cleanup callback 失败不会冒充未提交重建。collectObservation 实接 fetchCadenceJsonR31 和 observeRuntimeR31，记录 prefs 读取起止/PID，注明不是SQL watermark或跨PID强一致，也不是设备持续运行证书。

## 实际运行结果

`all-report-sinks-host/receipt.json` 保留完整源 hash、编译 class hash、明确依赖 classpath。依赖包含旧于最终整合的 P02 API DTO classpath，只证明这个源码子集可编译/host执行；不是最终Android全工程门。

- 新13项 + 旧3项 Census JUnit：16，0失败。含真实 overlap/free-space/protected debt/overshoot/closed-owner、共享失败分类、ring有界、corrupt metadata保全。
- 独立 JVM + OS lock：live holder不得回收、合计和重叠拒绝、SIGKILL 后才释放并保留原文件；真实跨 PID share reader 退出前不得删除恢复副本。
- 原生产 JNI：backup逐chunk、写前预算拒绝、JNI中途异常后目的 DB/journal真实关闭清理、source hash不变；真实归档/ZIP/CRC/恢复DB sha/逻辑行一致；READY/cleanup重试与缺raw成员保护。
- 新 sink 合同：真实 descriptor body copy、catalog实际FilePages、外排run/merge、renderer、inventory、metadata、恢复ZIP消费准入；拒绝不改原件；下一阶段扩容受真实cap拒绝；真实writer pin与作用域退出。未声称 catalog 的96业务检查在 Android 跑过。
- 原 R30 大 ZIP→20MiB parts、重组/tamper/offset、small ZIP 和 durable native cancel：PASS；本轮实际取消32ms，HOST结果。
- 最后 UI旗标恢复ZIP目标：已真实输出至少64KiB、输入/输出FD存活时才翻 Boolean，未interrupt，实际退出4ms；FD、grant、scope结束，无半ZIP/成功receipt，原始合法SQLite/hash保留，同job重试CRC/SHA通过。最终日志见 final-freeze/recovery-ui-cancel-final.log（精确时间以该原始日志为准）。

## 真实发现及纠正

`superseded-defect/` 保留最初100页估算不足日志和被替换C源码（重建后 SHA与原build receipt精确相同），以及同一原native库在最终 Linux st_blocks driver 下再次失败：实际1912832 >155648。原因是 SQLite 最后提交把先前 dirty cache 一起落盘。最终实现按全快照未分配 bytes+header/journal margin准入，原生真实回归通过。

早期 runner 把 SQLite backup 后的规范化 header 与 mutable原DB要求字节完全相等，是测试假设错误；已保留日志。最终对冻结snapshot恢复做确切SHA，对source做不变SHA，对source/restored合法SQLite做 integrity_check 和全逻辑行一致，未放松归档完整性。

## 未取得与未覆盖

见 GROWTH_SOURCE_COVERAGE.md。没有真实 Android OS getter/PFD/Doze/熄屏/低内存/低磁盘/进程杀后恢复的运行回执，没有24h/72h或原30GB材料。两项Android instrumentation定义已新增，执行NOT_RUN。外部SAF provider的物理可用空间UNKNOWN，仅有预期bytes、stream close、确切回读合同。全PID native/PSS峰值、所有native owner和跨PID总内存额度未闭环。

这里不关闭完整B05-C01/C02/C11，不代表13批56卡闭环，不代表自然未来胜率或15%出手目标达标。B05-C03的本owner源码/host链已完成；全App、设备与时长门仍必须由真实后续证据接续。
