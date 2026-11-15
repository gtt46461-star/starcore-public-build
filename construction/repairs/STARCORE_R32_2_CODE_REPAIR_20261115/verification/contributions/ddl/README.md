# P1.01：已完成迁移单元的严格定义漂移回执

实际修改一个文件：`app/src/main/java/com/ethk3/novaonepro/LegacySchemaCoordinatorR926.java`。后态完整文件见 `full/`，增量见 `P1_DDL_AUDIT.patch`，输入是应用既有 P1–P5 后的源码。

## 已实现的边界

`advanceUnitsR31` 先跳过 COMPLETED，再逐单元只读复核。旧码只捕获 `LegacyDdlR31.Pending`（缺对象），同名错误索引/触发器抛 `LEGACY_DDL_DEFINITION_DRIFT` 时，旧 COMPLETED 行不会失效，报错只能停在总审计。

新码对非致命 RuntimeException 使**对应单元**失效为 REPAIR_REQUIRED/POSTCONDITION_DRIFT，记录真实 cause chain，重抛原异常。失效回执本身失败则作为 suppressed exception 保留，原错误仍在；致命错误照原协议抛出。下次 owner 会进入该具体单元并保留失败，不会把它当已完成或重跑其他完成单元。

这是精确定位和失败恢复状态修复，**没有自动重建错误定义索引**。当前严格DDL明确不赋予通用DROP权限。错误对象重建需要逐个登记对象、原定义、新定义、读者、事务切换与长锁预算；不可以为“全绿”盲删索引或业务数据。

## 不应重复实现的历史项目

当前032源码的 `DbStore.ensureRuntimeExtraTablesOnce` 已只调用只读 `requireRuntimeReadyReadOnlyR31`，业务缺schema投递唯一迁移owner；`execQuiet` 及 `ensureLedgerIndexes` 已进入 `LegacyDdlR31` 的 sticky failure 和对象定义校验；分片每次最多一条实际DDL。它们必须保全并复验，不能把旧ledger的怀疑直接当本轮尚无代码。

## 本次验证

`verification_complete/RESULTS.json`：真实 Native SQLite + 真实 `LegacyDdlR31`、`SchemaDomainCoordinatorR926`、`LegacySchemaCoordinatorR926` 执行 12 个断言通过。反例把已完成的索引替换为同名错误列定义：实际原源码失败于“responsible old complete row rearmed”，新源码通过。检查原异常、单元状态、精确错误、坏定义未被悄悄删除、原业务值、重复拒绝，以及修正此对象后可恢复完成。

原有 `StrictMigrationR31Host` 的61条断言也通过，包含真实子JVM终止/恢复；与新12条合计73条。

Android、真机、耗时预算和错误定义重建尚未验证；不能将本次状态变化描述为这些问题全部解决。

复验参数见 `run_test.py`。依赖提供SDK36、同源全量支持classes、sqlite-jdbc和slf4j；`tests/CompletedDriftR32Host.java` 是实际回归入口。SLF4J只是日志依赖，无provider警告不影响SQLite断言。
