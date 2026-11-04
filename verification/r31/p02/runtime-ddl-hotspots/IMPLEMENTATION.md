# B02-C02 指定业务 DDL 热点修复

本次生产修改仅涉及 `DatabaseCoordinator`、`V51Repository`、`CertificateRevocationR30`、`DeferredEvidenceWriterR35` 与同链的 `RuntimeHealthLedgerR35`。保留既有启动/手动迁移入口，没有新增迁移表或算法修改，也未修改冻结的任务健康与内存实现。

- `DatabaseCoordinator.SchemaPendingR31` 实现现有 ClassifiedFailure 合同：DEPENDENCY / SCHEMA_READINESS / NOT_SUBMITTED，不消费任务故障预算。`requireSchemaObjectsR31` 只读取 sqlite_master 的真实对象类型与 PRAGMA table_info 的实际列，校验固定内部合同，最多 128 个对象；不创建、修复或清理表。
- `runExistingTransaction` 去掉每次执行的 B03/R35 ensure。先验证本包装层实际需要的事务回执列，再做原幂等、业务和审计；缺迁移时不进入 body，也不补造审计表。失败保留原 failureCause。P01 写入优先级、提交结果和异常协议保持原接口。
- `V51Repository` 两个预测/结算 callback 及 owner 结算前置检查使用纯 readiness。原 `ensure()` 仍供已存在的 Critical 启动和手动迁移使用。SchemaPending 原异常传播到持久 job owner，不能以普通失败报告掩盖成可重做的业务故障。
- 撤销入口在没有任何 R27/SU2 certificate 表时直接返回，不创建授权。已有 certificate 表时，必须先验证撤销表、所用列、防旧证书重发触发器、源修订触发器和索引；缺少任何必要对象均为依赖等待。通过后继续原事务内撤销、原证据归档及禁止旧证据复活。SchemaR27/SU2 原迁移入口继续执行 ensure。
- R35 job 的读取和状态更新改为纯 readiness；读取缺源不再报 pending=0。更新使用既有 coordinator 的原子写接口，已有 R35 行只改状态、计数、错误和更新时间，保留原 record_hash/job_type 及 snapshot_blob/hash/lease。bundle 不匹配或现有行属于 R36/未知协议时拒绝为 stale owner，保持 R36 实际可领取的 PENDING/RETRY_WAIT/RUNNING 状态。缺迁移不受理新 job、不写成成功，已有 RETRY 行继续保留。R35 旧入口无法在进程重启后重建缺失的内存结果这一限制仍明确存在；实际 R36 durable snapshot owner 不被替换。
- 同链 RuntimeHealthLedger 的 record/snapshot 去掉 ensure；缺 schema 的 snapshot 明确为 SCHEMA_PENDING，诊断失败不反过来要求重做已提交业务。
- 按 root 合同显式登记 `R31_FETCH_BEGIN_ASYNC=TELEMETRY`、`R31_LEGACY_SCHEMA_PREPARE=MIGRATION`，保留已有 R31_FETCH_TIMING 和所有其它优先级。
- `invalidateLegacyRuntimeSchemaR31(reason)` 仅失效旧 Legacy ready flag 并写有界 SCHEMA_PENDING 原因；不发 SQL，不改变 Critical readiness，使已有异步迁移 owner 能修复真实检测到的 schema 漂移。

`run_runtime_ddl_host.py` 编译真实当前生产方法，使用 native SQLite/JDBC 与明确的 HOST 平台适配器。SQL_API_CALLS 记录实际 Java SQLite API 调用；最终驱动将验证断言自身的查询标为 ASSERTION_READ，避免将其计入业务 SQL。它不是 Android SQLite trace，也不覆盖全应用或 native 生成 SQL。

最终局部证据为 96 个断言、17 个有真实 SQL 的业务区间、261 次生产业务 API 调用、DDL0；断言读取已另标 ASSERTION_READ。覆盖缺表/缺触发器拒绝、实际预测 callback、结算与重复结算、原冻结 bundle 保全、持久 job 身份及快照/租约保全、旧 R35 不覆盖 R36 协议、无 SQL 的 Legacy cache 失效。原撤销四种源变更与实际下一次权威读取拒绝均通过。原 P01 writer Java 驱动、真实 HOST JVM halt 后新 JVM 回执恢复及 10 个单测也通过。旧 writer 的 Java 驱动/断言未改，仅配置支撑类和同版 Java compiler/runtime，并将 transport 耦合依赖从实际源码编译。

第一次大 sourcepath javac 触发 JDK17 编译器 NPE；随后指定实际源码闭包的 scoped javac 和 native 回归通过。后续出现旧 DbStore/Legacy API 闭包缺方法、支撑 class major65 与 JDK17 不匹配、旧 RawHttpResult 缺 transport 字段等具体编译错误。最终使用 root 指定的实际主链支撑目录 `/tmp/r31-mainchain-native-v1t3j3gw/production`（其 14 源收据在 schema_mainchain/runs/20261104T112716Z）、当前 transport 源码和已安装 Java21 compiler/runtime，通过当前 12 个显式生产源码编译及上述回归。历史错误摘要另存，最终 production-javac.log 是通过日志。支撑闭包与基线 numerical classes 的范围均在收据明确标注；这仍不是整个 Android 工程最终构建通过。

最终冻结收据见 FROZEN_SOURCES.json、RECEIPT.json 与 WRITER_REGRESSION_RECEIPT.json。5 个修改生产源与 native 编译收据 SHA 一致，DatabaseCoordinator 同时与最终 writer 回归一致。root 全 Android 联合构建/APK仍待其记录。本目录不宣称全部应用运行期 DDL 已消除。DbStore 主链、平台/实时选择缓存失效路径、其它静态审计候选和设备 SQL trace 另有 owner/验收项。真机、24/72 小时与自然策略质量仍为 NOT_RUN / NOT_PROVEN。
