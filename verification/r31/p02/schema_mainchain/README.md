# P02 主链业务 schema 热点修复与实际 native SQLite 证据

当前完整执行：[runs/20261104T112716Z/receipt.json](runs/20261104T112716Z/receipt.json)。状态 `HOST_MAINCHAIN_NAMED_PATHS_PASS_NOT_ANDROID_NOT_FULL_B02`。没有运行 Gradle、Android、真实用户数据库或清数据；没有 commit/push。P03 仍未施工，原准备与反例证据保持原样。

本 agent 仅修改 `DbStore.java`、`LegacySchemaCoordinatorR926.java`。root 统一拥有并整合 DbHelper wrapper、SchemaDomain/manifest revision 2、AutoFetch control；task_runtime 拥有 DatabaseCoordinator 只读 schema API、失效 API 和迁移 operation registry。本测试 fresh 编译了这批真实生产源码的显式闭包，不使用 DbStore/Legacy 的简化替身。

## 行为

* `saveShadowCandidates → saveMainChainCandidateSnapshot` 原主链 ensure 改为真实只读结构检查。saveShadowCandidates 后半段原先还调用 lazy Legacy initializer，也改为只读，避免第一处移除后仍在业务写入时 DDL。
* 只读合同检查 28 个唯一表的必要列及 18 个已有 DbHelper 主链索引，缺失抛出 `SchemaPendingR31(MAIN_CHAIN_CLOSED_LOOP, reason)`，不临场建表/补列。业务不补 defaults、不从报告/诊断/DbStore 构造触发迁移。
* Legacy 新增 `LEGACY_MAIN_CHAIN_R31_V1`。旧 READY stamp 只有新 step 完成且真实结构仍 ready 才能早返；owner 通过 root 的严格 DbHelper additive wrapper 补齐原主链和全部 16 张因子表，包括 promotion 的 factor 字段及 schema_fix_version。失败不得 stamp READY，process-fatal 不被吞成普通失败。
* `requiredMainChainObjectsR31()` 返回 46 个去重对象名，供 root 纳入 Legacy contract 并升级 revision 2；schema drift 可使旧完成步骤进入真实重试。
* `requestLegacySchemaMigrationR31()` 由独立后台 control 调用。它只读检查并通过现有 `submitBackgroundWrite("R31_LEGACY_SCHEMA_PREPARE")` 排迁移；真实缺失时先调用协调器的 `invalidateLegacyRuntimeSchemaR31`，防止旧内存 READY 永远跳过修复。跨 DbStore 以同一 coordinator 保持 single-flight；Future 不向产品调用者暴露，callback finally 记录实际退出，后续 control 只在正常 Future 完成，或取消且 callback 已退出时回收 flight。未进入 callback 的普通 admission 失败也能在 Future 正常完成后再请求；取消本身不代表运行者退出。
* 旧 `ensureMainChainDefaultState` 在新 mainchain journal 事务内执行；两个原状态表的五个值均实读校验，不能靠旧 helper 吞错当初始化成功。后续正常 control 不重复写 defaults。

## 测试揭示并修复的原业务错误

原 `candidate_audit` 的 `audit_key` 是 `NOT NULL UNIQUE`，但主链 snapshot writer 从未提供它。实际 native SQLite 轨迹显示 snapshot/fullscan 先插入，audit 插入失败，然后整笔 snapshot 事务 ROLLBACK；原 catch(Throwable) 吞掉失败，saveShadowCandidates 继续独立 SHADOW COMMIT。这不是成功保存 snapshot。

限定在该 sidecar 路径，新增 `audit_key = "MCCA_" + existing snapshot_key`，只绑定本次真实候选，不生成任何预测冻结、资格、收益或模型效果证明。各 insert 必须返回真实成功 rowid；snapshot 和 reject-bucket 写失败向上传递并回滚，提交失败也不再返回已回滚的 saved 计数。旧有效 snapshot、factor、diagnostic 行不改写。

原失败证据保留于 `runs/20261104T112314Z`，包括 native SQL 的实际 ROLLBACK，未删除后只留通过轮。更早 `111617Z` 的 fixture 缺少原 diagnostic_log.log_key，属于 harness NOT NULL 错误，该轮明确为失败；修复的是 fixture 必填 key，没有放松生产约束。

## 最新实际执行

|检查|真实结果|
|---|---|
|空库调用实际 public saveShadowCandidates|SCHEMA_PENDING；3 个 API 调用、DDL=0，schema object 数未变|
|owner additive migration / 第二次 ensure|新 step 真实 journal COMPLETED；第二次 36 个 API 调用、DDL=0；原 snapshot BLOB `000102FEFF`、因子 `ORIGINAL_FACTOR:37`、原诊断保留|
|ready 实际 saveShadowCandidates|snapshot/fullscan/audit/reject/SHADOW 真写入；80 个 API 调用、DDL=0；80 含只读 readiness 和写入，不能当作 64-op writer 验收|
|audit 写入注入异常|原异常到达调用方，snapshot 事务 rollback，旧行原样保留；38 个 API 调用、DDL=0|
|匹配 READY stamp 但无新 mainchain step|实际执行新 initializer 并 journal 完成，不再跳过|
|移除 index、注入 owner index DDL 失败，再重试|域与 step 明确 FAILED；再次执行恢复缺失 index、attempt 增加；原数据不丢|
|两个 DbStore 共用 coordinator 的异步迁移|一个请求被接受，另一个不重复排队；control 线程 DDL=0；旧内存 READY 被失效，后台 owner 真修复；default keys 初始化一次；后续 control 32 个 API 调用、DDL=0 且没有 INSERT/UPDATE|

见 [mainchain-native.log](runs/20261104T112716Z/mainchain-native.log) 与 [SQL_API_CALLS.tsv](runs/20261104T112716Z/SQL_API_CALLS.tsv)。源文件/runner/harness 摘要、命令、exit code、原日志摘要与完整源码副本都在该轮目录。`source_changed_during_run=[]`。

## 明确未关闭的门

这是 named-path host 证据。SQLite 由 JDBC 调用真实 native SQLite，平台连接、clock、Looper 与构造由 host adapters 替代；Unsafe 只跳过 Android 构造并注入连接，不替换执行的生产 DbStore/Legacy/DbHelper 方法。Critical 前置对象由对应真实 source schema 方法建立，不用同名空壳冒充 trigger/view；没有声称完整 Android startup/backup migration 验证。

迁移 owner 的实测轨迹仍很大：主链迁移 1498 次 DDL，首次完整 Legacy 初始化 3567 次 DDL。top40 sidecar 也可能约 160 次 INSERT。这些仍是全 B02 owner staging/迁移分片债务；移除业务 DDL 不等于整体 writer op/bytes/hold 预算通过。没有 Android writer hold、实时延迟、24h/72h 或自然质量 PASS，也未证明所有其它 DbStore hot ensure 已被移除。

重跑：

```bash
python3 verification/r31/p02/schema_mainchain/run_native_host.py
```

该 runner 只向隔离 `/tmp` 编译和新建 native fixture DB，证据写入本目录；不运行 Gradle、不安装 APK、不连接用户 DB。
