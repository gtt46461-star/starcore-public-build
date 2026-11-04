# R31 原子／分子写入协议：源码和真实 HOST 证据

施工卡 `R31-B02-C02` **仍为 IMPLEMENTING**。本模块完成165个原子评分及32个分子结果的有界 prepared 写入和完整发布；没有据此关闭整张卡、B05容量验收、Android集成、设备持续运行或自然质量验收。

- 方案原始基线：`5237b265a27fe5ad27fd842901c445d5f8c27749`。
- 本轮隔离分支基线：`241a93848851440fdfd21155ae400ff2354eceb2`。
- 分支：`construction/r31-atom-budget-protocol-20261104`，工作树 `/workspace/r31-atom-budget-protocol`。
- 生产提交：`4bd5d93e7ff31c5f6f5d9f0d2f8e848a6ba9bf40`，8个生产文件。
- 前置工具提交 `48a97416` 是已交给root的 `740e517e` 的等价cherry-pick。root已继承740时只需接生产及本轮测试提交。
- 本代理没有修改共享 `SchemaR27`、`SchemaDomainCoordinatorR926`、`RuntimeR27`、任务／资格／CSV协议、签名、版本及远端。

## 实际生产链

`AtomWorldR27.atomsStep` 保持120行 frame 分片。frame未完成不会评分；frame完成后评分写入失败，下次真正入口会重试评分，不会因内存cursor已推进而跳过。

原 `score` 的165项科学计算及 `molecules` 的32次AST、RNG和同机会评估循环保留。结果在writer外冻结并计算输入hash／manifest。每个结果pair使用固定一个part，包含primary、配对贡献、chunk marker；part身份不随另一个JVM的堆大小改变。

所有业务写入使用原 `ResearchWriteQueueR27.ImmutableBatch` 和真实 `DatabaseCoordinator.runIdempotentAtomicWriteR31`。源revision／pin、schema版本、owner／fence／session／boot租约、prepared header和完整manifest在原事务边界检查。只有最终发布事务能更新active指针和真实artifact进度。绑定超过 `min(256 KiB, maxHeap/512)` 的单结果会保留容量债务并拒绝，不会偷拆科学原子边界或删候选。

旧完整active保留到新结果集真正完整。三个visible views以及6个直接读者只消费完整发布集；旧表原件保留，derived能力及分子first-birth registry的原语义保留。`RuntimeR27.leafWeights` 的实际map只会整体装入165项完整集；新Runtime独立恢复分子消费者。原静态代数保留ABI，新链不再修改它。

已完成但被更新结果取代的run不能因旧receipt重放而冒充当前结果。提交后诊断故障不会重做业务；UNKNOWN在独立readback可用前仍为UNKNOWN。

## 迁移合同

`schema_contract.json/.sql` 保存最初提案，包含首轮实际SQLite发现的贡献view多一个尾括号错误，不能拿它启动新库。

当前精确合同为 `schema_contract_rev1_parse_fixed.json/.sql`，共29对象：6表、4索引、3view、16trigger。生产 `AtomPublicationStoreR31.DDL` 已逐条对照该合同；启动入口 `public static ensureSchemaR31(SQLiteDatabase)` 要求调用者已拥有迁移事务。业务只调用 `requireReadyR31`，按schema版本检查全部对象精确定义，不执行DDL。

root统一接入 `RESEARCH_R31_ATOM_PUBLICATION` rev1。这里实际验证了公开迁移入口的native SQLite旧库升级、重复运行、旧行/BLOB/user_version保全、实际view SELECT及同名错误view拒绝。**共享schema journal完整启动链和Android SQLite版本兼容仍由root联合验收。**

## 实测范围和原始证据

`runs/final-ci-contract/receipt.json` 是最终CI合同的源码／依赖／class／命令收据；`runs/final-targeted/` 和后续专项收据保留其他同源实跑。`SUMMARY.json` 汇总最终结果，不替代每次原始日志。

实际旧生产负对照保留：原子165项一次事务 **337 API SQL**；原32分子一次事务 **69 API SQL**，均真实超过64。失败分母和最初DDL／fixture／测试预期错误全部保留。

原 `BrierContributionR30HostTest` 的3000行、seed601、原split、poisoned CONFIRM labels、paired mean／单侧LCB、报告、失败保留内存和重试断言保留。新的实际旧／新生产输出对照检查全部165评分字段、全部配对period/count/mean/LCB/source、32分子registry字段；两份完整科学projection字节SHA相同。仅排除的墙钟列另有跨进程原始computedAt／recordedAt恢复断言。

测试还实际覆盖：第一／中间SQL故障、END前回滚、已提交但ACK异常、UNKNOWN后独立readback、提交后诊断异常、取消、中断事务、source correction、owner transfer、前代CAS、superseded重放、每个中间part旧完整值保全、真正atomsStep失败恢复、view全文报告、schema定义不匹配。

两种实际JVM退出：提交并释放writer后 `halt(73)`；原生事务中已写两行、尚未提交时 `halt(74)`。另一个64 MiB JVM重新打开真实数据库，只看到73已提交part；只执行92缺失pair，73已有receipt不重做。两个完整结果集恰有334个receipt、330个part，原始时间及creator/fence保留。它们是 **HOST进程退出／原生SQLite恢复**，不是Android杀进程、真机后台、Doze或24/72小时证明。事务中halt时那个尝试的完整hold未知，原记录保留UNKNOWN；后继进程独立数据检查证明该事务回滚。

每个可观测事务记录API SQL、参数字节、原生 `changes()/total_changes()`、回滚／提交、实际writer ledger hold/wait和原生事务耗时。trigger字段是依据精确DDL和实际DML调用计算的保守执行上界，包含stage和最终指针／run guard；不是不存在的JDBC内部SQL trace。预算断言使用API调用加该上界。回滚尝试的native total_changes也保留，不能把它当已提交业务数量。

`RAW_SQL_ARCHIVES.json` 及最终runner收据保留每份无损SQL gzip的原文件字节数、原SHA、压缩SHA和逐字roundtrip验证。所有失败／中断SQL仍可恢复，没有删失败样本。归档压缩只是仓库证据存储处理，不是B05应用数据回收证据。

HOST适配只提供真实JDK单调时钟／PID及JDBC SQLite物理连接、事务、原生变化计数；DbStore adapter把reader和唯一真实coordinator接到工程fixture。业务score、source guard、fence、prepared queue、receipt、发布和报告来自实际生产源码。它不替代Android系统服务、主线程或真机行为。固定支持jar：`8dd37c4c7f21e09fc1413e7c2b992538b4d66f833d07c754d41ca65df2dc5da0`，来源root ba35真实完整构建。初期012支持jar收据也保留，最终收据明确区分。

## 未完成及下一步

- root接入共享schema后，使用同版实际完整app类重跑该CI入口、Android编译／安装和必要仪器测试。代理没有跑全局Gradle，也没有制作或交付APK。
- `ReportSnapshotR30` 已接原子complete views；History owner的published-count scalar未冻结，本提交没有用假API／stub把未完成导入计为已完成。待其提交后单独联改同一次报告SELECT。
- 每SCORES run增长165 primary、165 contribution、165 chunk和167 receipt；每MOLECULES run为32／32／32／34。所有者是本生产helper。`total_bind_bytes` 是绑定payload，不是物理DB／WAL容量。B05必须将新表接入统一磁盘写前准入、引用／生命周期、容量债务和后续受控回收；active／活动输入／必要历史贡献原件受保护。本提交不宣称物理释放或磁盘预算通过。
- 其他producer预算／模型checkpoint同版工作仍各有owner，不因本模块完成而关闭B02-C02。
- 早期HOST曾观测122ms hold，原记录保留；最终固定快照的HOST统计不能选择性替代设备SLO。24小时／72小时、Android锁竞争／OS恢复／设备内存磁盘／时钟／后台验收仍为未运行或由root独立证明。
- 没有任何自然未来质量、资格晋升、REAL收益或代际增强结论。工程fixture和开发集保全不能证明自然高胜率。
