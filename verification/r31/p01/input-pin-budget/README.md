源变更失效协议：实际实现与证据

基线是 `de9a30834025c18ce291c241a722abe437717539`，工作分支为 `construction/r31-input-pin-budget-20261104`。这项工作补充 P01 / R31-B02-C02 的触发器连带写量与输入 CAS 协议，不代表整张卡或 G01 已闭环。方案原始基线仍为 `5237b265a27fe5ad27fd842901c445d5f8c27749`，未用 main 替换。

旧版真实复现见 `baseline-fanout.json`：1、16、256、2048 个受影响 pin 的一次 UPDATE 分别写入 1、16、256、2048 条 INPUT_INVALIDATION；含源行及旧 source clock 的 total_changes 为 3、18、258、2050。使用冻结的实际 013 Android 类及 SQLite JDBC。工程 pin 行用于测触发器放大量，不是训练或自然质量样本。

新实现每个源行变更只追加一条 source event，pin/window 插入时捕获独立 AUTOINCREMENT fence；旧 global source_revision 的两列及原六个触发器保持原样，继续用于准备/提交 CAS。新 fence 直接读取实际序列，不依赖两组触发器先后或猜测 generation+1。JDBC 原生 update listener 已记录 `[source_event, old_clock]` 和 `[old_clock, source_event]` 两种实际相反顺序，失效与回滚均正确。

有效性门同时检查旧 INPUT_INVALIDATION 与新 journal。所有五个原直接 consumer 已接齐：AtomNetRuntime、MatrixResearch 的提交前 guard；OnlineResearchWindow.find；ModelReplayStudy 的结算、summary、replay 分配；MatrixEvidence 的 temporal 来源读取。新 SQL 没有把“报告已物化”作为准入前提。模型、统计、数据角色、策略门槛及 5000 期选择没有修改。

HistoryImport 必须同版集成新的 COMPLETE 发布协议。准备阶段按最多 32 个精确 ordinal 写成员（History owner 实际每片 16），最终 INSERT/REPLACE 类事件与发布同事务；range 只是说明，不把范围中的洞当作成员。重复 event_key 使用 `INSERT SELECT WHERE NOT EXISTS`，避免 SQLite `INSERT OR IGNORE` 消耗 AUTOINCREMENT 序列导致假 fence 变化。发布后的成员和事件不可静默改写。

滚动删除保持原合同：完整 immutable pin 的原始 raw、hash、split 和正式角色能够实际恢复时，canonical 物化行删除不重启该 pin。首次恢复/重开/压力清理后在事务外解压核验，512KiB 声明预算内仅保存最多 8 个 DB ×128 个已验证元数据；事务内缺原始恢复 proof 时给 typed DEPENDENCY，避免在短 writer 中解压。未知或损坏原始快照拒绝，ONLINE window 没有原始 pin 恢复豁免。该 proof 不缓存源有效性；源 journal 和旧失效账本始终先判定。

迁移复用 Root 的 `SchemaDomainCoordinatorR926.migrateR31SourceJournal`，新 source domain rev1、24 个对象；SQLite user_version 保持原值，原 pin 编码 VERSION 也不变。helper 仅接受 EXACT 旧四个触发器、EXACT 新六个触发器或全空的显式安装；未知同名定义拒绝。R31 已存在而 evidence table 缺失也拒绝，不能创建一个空 journal 冒充原证据。

最终本模块测试：`runs/20261104T192757Z/RECEIPT.json`，所有来源测试前后字节一致。

- `InputPinJournalR31Host`：3631 项断言；1/16/256/2048 pin 的单次 UPDATE 恒 3 行（源 + 旧 clock + event）、新 INPUT_INVALIDATION 恒 0；同事务即时 invalid/revision/require/run gate、回滚、重开、metadata no-op、范围端点、精确 import 洞、已发布成员保护、重复 event 幂等、32/33 成员边界及真实逆序触发器。
- 原 `RepairInputPinR30HostTest` 原样通过；3000 原始输入、追加400与删除旧500后原 hash/角色可恢复、同事务纠正后 guard 全回滚、精确回改内容的新 pin 与旧 advice 区分、原两列/六触发器及 immutable pin 保留。
- `InputSourceMigrationR31Host`：46 项断言（含实际 user_version36 升级、重开和回滚保留）。执行实际 DbHelper.createBaseTables、migrateDrawColumns/AppState、实际 12 个 Critical runStep 回调，完整实际 Critical contract verifyObjects；旧 MATCH 及严格旧 pin-addition MATCH → 精确切换 → restamp → 物理重开。注入第二个新 draw trigger DDL 失败时，旧 trigger/stamp/journal 全回滚；无关 Critical trigger 篡改不能取得新 stamp；原 stamp 缺失不补造。仅 HOST Db 测试 adapter 去除 final 以便注入，原 schema 方法/断言未改，compiled SHA 已记录。
- 实际 consumer 有正反对照：有效工程模型 receipt 可被 summary 与 temporal matrix 消费；同事务源修正后两者立即拒绝；rollback 后 matrix 恢复。工程 receipt 不是模型训练、未来胜率或资格证据。

重跑命令：

```sh
python3 tools/r31/verify_input_pin_joint.py \
  --support-jar /tmp/r31-p02-014-actual-android-support/classes.jar \
  --shared-api-classes /workspace/r31-shared-migrations/verification/r31/p01/shared-migration-integration/20261104T184245Z/classes
```

工具 fresh 编译当前 8 个本模块生产文件，以及冻结的 Root Domain/Schema、Q、Evolution 共 13 个显式源码；其余 shared API 由指定固定类快照供给，来源、jar、命令、stderr、SQL trace 均有 hash。它不是完整 Android 构建。Root 必须将 Domain/Schema、History COMPLETE、其他新发布协议同版合并后，执行整编和身份门。

保留失败历史：`191546Z` 是 HOST fixture final/package-private 编译错误；修正的是测试适配。`192107Z` 所有测试退出0，但最终 Q 来源在运行中变化，收据严格为 FAILED/source drift；固定快照原始结果保留，不作为当前树通过。`191709Z` 是先前稳定源码的 3606+43 断言通过记录。

剩余边界：

- Android 全量构建、unexcluded suite、设备 SQLite/WAL、运行时延、24/72 小时、自然未来质量均未在此模块执行。
- Source/member journal 当前保全为 append-only；带引用保护的回收与磁盘字节准入尚未实现，不能将每个源行写量固定当作存储容量闭环。未发布成员的失败 job 回收仍需与 History/保留策略联验。
- 任意 SQL `INSERT OR REPLACE` 的 ONLINE 纠正语义没有全面证明。实际 canonical DbStore 路径是 CONFLICT_IGNORE + UPDATE；旧 HistoryImport 的 REPLACE 必须由新 COMPLETE 精确事件路径替换，不能单独交付本 helper 后宣称已接全。
- 原始 pin 解压/编码仍是有界但未单独新增完整 working-memory owner 的路径；新增 proof cache 只有元数据声明，不代表这些工作数组或 SQLite native 内存已覆盖。
- 不重置旧曝光、不删除旧 pin/失效账本、不创建自然合格策略；没有设备或自然质量 PASS。

源码提交为 `d88c848f16c6ad2fcde61c21a7c872b3443eb61a`。最后工具复验复制并逐类哈希固定 shared API 闭包，记录 runner SHA；本模块8生产源没有继续修改。192241Z 的3631+43稳定通过历史保持原样。
