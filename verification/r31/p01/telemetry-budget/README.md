这批只修两个已确认可达的生产热点。基线3246ccf8eb6f7b2f7b9d0fe77fc18a70ab958056；工作树/workspace/r31-writer-budgets，分支construction/r31-writer-budgets-20261104。没有改主验收工作树、版本、应用身份、签名、数据库版本或全局施工账本；没有推送或生成APK。

HoldLedgerFlushR30在60秒轮换点关闭原始窗口，保留其完整Stat集合；JSON、分位数和SQL参数都在writer外准备。每片按64 SQL操作及min(262144,maxMemory/512)字节准入，预留12条协议SQL以及含owner/fence/readiness/receipt的保守字节额度。实际r27-health owner有两次真实fence查询；本轮真实最大59 SQL调用。所有同一窗口的行沿用同一start/end，不把每片伪装成更短统计窗口。每行INSERT OR IGNORE后比对全部原始列，已有不同内容不得覆盖；幂等receipt与业务在同一事务，COMMITTED/独立readback确证后才推进cursor。SQL失败、提交后异常、UNKNOWN均保留原prepared片，统计未重取样。

时间线生产flush在writer外一次读原事件存储panel及原始payload，保留其全部身份、来源、stage wall/mono及未知字段；事务只做exact payload CAS、唯一存储验证及receipt，不重新编码JSON。并发新增原stage令整片回滚，保留原queued observation wall/mono/boot并重备；已有durable observation只验证，保留最早已提交值。原样本对象只在确证提交后从队列compare-remove。同event的不同stage不能同片引用相同旧payload，后续stage留下一片验证，避免持续CAS回滚。

业务路径没有CREATE。readiness由现有SchemaR27表/列提供，缺schema在轮换统计之前返回DEPENDENCY(SCHEMA_PENDING)，不现场建表。原7天统计清理保留，但改为在writer外按既有主键排序准备最多52条精确key删除，一条最多一行；只清理原策略已过期且时钟窗口顺序明确的数据，不删除未来/逆序时钟窗口。所有修改仍在原唯一coordinator和数据库中。

真实结果见SUMMARY.json及runs/final-cast-bound-source：

- 旧64事件生产flush：单事务320 SQL调用（含原事件读取及128条DML）；旧65操作统计：单事务67调用且包含业务CREATE，均真实重现。
- 当前22个native SQLite生产场景、4285项HOST断言，75笔事务尝试，其中68提交、7回滚；每笔含协议实际最多59调用、245206 bind字节、业务DDL0。
- 两种真实JVM堆配置512MiB/64MiB，上限分别262144/131072字节；每笔严格比较实际cap。
- 原生产WriterHoldLedger逐attempt测得本次HOST最大写锁持有79ms；这不是Android p99/24h/72h证据。原样保留每笔事务duration及原生产ledger的hold/wait增量。
- 故障覆盖首片及中间片SQL失败、COMMIT前rollback、COMMIT后ack异常、独立readback不可用后receipt重放、postcommit诊断、source payload竞争、并发新增、已有durable时钟、缺schema、超大单事件容量债务和160行真实过期清理。
- committed-write-count-postchecks.json把原始SQL调用与实际commit/rollback关联：标准64事件恰64个已提交原事件UPDATE；原已有durable则63；统计65个操作恰65个已提交行INSERT，130个原样本及两个直方图分母均保全；过期删除恰160行。
- 原RepairTimelineR30HostTest不改断言，全部原回归执行成功。所有测试明确使用工程fixture，不能当真实开奖、训练效果或自然策略质量。

复现命令（既有JDK/SDK及合法完整Android classes支持jar）：

    source /workspace/starcore-environment/activate.sh
    python tools/r31/run_telemetry_budget_host.py --output verification/r31/p01/telemetry-budget/runs/reproduce-001 --support-jar /tmp/r31-p02-012-actual-android-support/classes.jar

runner在/tmp编译实际coordinator及本批生产源码，HOST SQLite adapter只映射原SQL到真实JDBC SQLite并记录调用/注入异常，不手写替代CAS。支持jar SHA892e5be7feb3609755c6746ea080644fd19427a90f8d9ab8493c964af1f01fa8；源SHA、编译class SHA、JDBC/Android jar、HOST shim/test SHA在receipt。命令每个子进程60秒截止，并发producer真实join后才继续；适配器与fixture不进入APK。

所有先前失败都保留在runs及historical-outcomes.json：平台shim编译依赖缺失、同event双stage的实际生产CAS缺陷、最后计时fixture误清missing-schema输入。失败日志未改名为成功；最终结果仅绑定final-cast-bound-source的实际源码SHA。原失败receipt有旧runner留下的IN_PROGRESS字段，historical-outcomes以其真实非零exitCode明确标为FAILED，不把已终止失败伪报仍在运行。

边界仍明确：

- 本分支未执行Gradle全量/Android仪器/物理设备/24h/72h，未生成含该修改的APK。必须由root同版合入两个生产文件再联合构建；它们共享内部准备协议，不能只取一个文件。
- 单个原始事件连同完整CAS参数超预算时，保留原事件与observation，报告容量债务，其他可容纳事件继续；不截断JSON、不删证据、不超预算。缺源保持待办。
- 原进程内未flush观察和直方图仍有进程死亡后UNKNOWN边界；没有实现或声称持久telemetry outbox。既有256观察队列及omitted计数未改成无界，也未把原可能省略的观察声称保全。
- Hold同窗口多操作可在相邻短事务陆续可见，closed-window pending行在flushStateR31；pendingRows只表示已关闭窗口，不包括仍开放的live accumulator。报告不能据此声称全局分母完整。
- 历史package-private flushCommittedWithin保留供原单事务兼容测试；生产flush不再调用。其历史调用不能用本轮新路径证据冒充全工程运行期DDL0/全部callback合规。
- 复用的r31_commit_receipt增长/回收、其他真实writer producer（HistoryImport/AtomWorld/Evolution/AtomNet等）、完整staging/GC与Android/WAL集成仍归后续原owner；本模块不删除共享receipt，也不另拆数据库。

R31-B02-C02不得关闭。本目录只支持上述两条生产路径的真实写前预算与故障保全证据，不支持整卡闭环、整包工程完成或自然质量达标。

最终自审另修正了Android rawQuery字符串绑定的SQLite affinity边界：length(...)<=?在无列affinity表达式中可能按存储类型比较，现为<=CAST(? AS INTEGER)。新增HOST适配器只观察真实query返回文本字节，不改变SQL结果。64MiB堆实测source原件最大40586字节，明确不把200KiB超cap原件读入Java JSON；所有原payload/时钟保全及22场景重新按最终源码执行。

最终完整分母的HOST hold目标仍未达标：75尝试（含7次rollback）nearest-rank p99=79ms；68次提交p99=61ms；共2次>50ms，0次>200ms。SUMMARY逐case/attempt保留分类。不能删掉故障/冷启动样本，也不能改用旧run的35ms来写目标通过；此项继续开放，Android实际性能仍未执行。
