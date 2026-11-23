# B01：SchemaUnitPlanR34 保护迁移切片补件

## 交付状态与边界

本补件是 **可编译、已通过宿主协议测试与真实 SQLite 故障夹具的纯核心**，不是已接入 Android 的迁移实现。`SchemaUnitPlanR34.java` 未应用到 095；`productionAndroidOwnerIntegrated=false`。核心没有数据库连接、没有执行生产 DDL，也不会因为调用一个 `ready()` 就解开迁移保护。核心的 `certificateCurrent` 只检查已经建立的 **retention 单元证书** 是否仍匹配，不代表整个 R26/R27 schema、关键域原件、模型恢复或运行会话已完成。

基线固定为 095：`273641e838e0b8dd842b42a67a674e184942a30a`。58 个保护表名称与顺序逐字继承 `strategyv2/ResearchRetentionR30.PROTECTED_TABLES`；测试会对实际源码重新提取后逐项比较，增加或遗漏表都会失败。每表先一个 `INSTALL` 单元，再一个不允许 DDL 的 `VERIFY` 单元，共 116 个协议步骤。不是把全部 58 表放在一个短函数中就称为“切片”。

没有原始现场 DB，不能给出现场实际 source definition/guard definition 哈希，也不能确认58张表当时全部存在。本补件不填造这些哈希；测试中的 `fixture` 摘要明确只服务夹具。生产 `Plan` 要求旧 owner 提供每一个表的精确期望定义、存在策略和目标版本，不能拿当前任意 `sqlite_master` 内容计算摘要后反过来认可它。

## 095 真正的接入点

|现有文件/方法|本批施工必须完成的修改|不能省略的旧保护|
|---|---|---|
|`strategyv2/RuntimeCoordinatorR26.startupUpgradeSpecR31`（392行附近）、`initialize`|把 schema 安装/验证、checkpoint 启动原件保全、会话与 task 注册拆成独立可恢复阶段；恢复已有 durable cursor，按一次一单元调度。|会话 generation、owner fence、旧 running task 恢复、能力ABI失效，不能随 schema 命中缓存跳过。|
|`SchemaDomainCoordinatorR926.runUpgradeR31`（1672行起）|在旧 Upgrade journal 下增加版本化的单元子账本；已经 CLEANUP 的版本不再无条件执行包含全量保护循环的 transform。|VERIFY_SOURCE / TRANSFORM / VERIFY_TARGET 的 protected root 和定义保护要映射到可核验单元证据；禁止删除旧校验后只相信新账本。|
|`SchemaDomainCoordinatorR926.migrateR31RetentionProtection`（1022行起）|增加明确的“单表已准备输入 + 原件保护证明”入口，使用原 MIGRATION writer，不能另开写连接；该入口消费纯核心 Admission。|现有 critical stamp、完整定义、原件指纹同等约束，未知 attachment/SQL 必须阻断。|
|`strategyv2/ResearchRetentionStoreR30.ensureProtectedObjectsR31`（142行起）|从当前 for 循环中提取 **一个指定受保护source** 的安装与验证。公共 infra DDL 独立版本化；单表方法只准消费该表的不可变准备结果。|原 `requireArchiveDefinitionR31`、`requireGuardDefinitionsR31`、列的 NONE affinity、元数据约束及未知触发器拒绝。|
|`GapReferenceR31.expectedGuards` / `KeepArchiveRetirementR31.expectedGuards` / `KeepMismatchReferenceR31.expectedGuards`|继续从原 owner 组合完整目标guard；定义摘要要覆盖组合后的全部已知attachment。|本测试抽取的是基础 guard builder，不代表这些专用guard已经集成验证。不能用基础guard覆盖有退役/引用语义的实际表。|
|`ResearchRetentionStoreR30.preserveStartupCheckpoint`、`StartupCheckpointIdentityR31`|与 schema 单表循环分开，但必须在训练可覆盖 checkpoint 之前完成。每个checkpoint原件用原 hash 和scope保全；准备文件IO在writer外。|进程标记和完整原件保全回执同事务；不得因上次schema成功而跳过本次进程需要的snapshot。|
|`RuntimeSchemaR26.ensure/bindCapabilitySource`、`SchemaR27.ensure`|基础schema有自己的依赖顺序与单元计划；只有所需基础定义就绪才运行对应retention单元。|本补件没有替代base/R27 migration；数字ABI变更保留原模型，不转移旧自然样本。|

095 的 `RuntimeSchemaR26.ready` 只是有限表存在性检查，不足以生成本协议 DONE 证书。`DbHelper.ensureSchema` 又是另一条关键域启动入口：需要保持抓取/开奖能安全使用的关键表先就绪，其后研究迁移按域 `WAIT_SCHEMA`，不能把两条owner混成一个“大ensure”。

## 生产者输入与不可变计划

`Plan(databaseId, migrationId, schemaRevision, definitionRevision, definitions)` 的每项输入必须有明确来源：

- `databaseId`：与数据库原件绑定的耐久UUID/restore epoch，不能用进程UUID；恢复另一份DB后必须重核身份。
- `schemaRevision`：目标版本化 schema owner 的实际revision；不是随意把 `PRAGMA user_version` 改为34。测试中的34只是fixture数字。
- `definitionRevision`：原builder组合规则版本，任何会影响guard/归档列/owner语义的修改都必须变化。
- `sourceDefinitionHash`：该表期望的完整列、PK/约束、索引与相关owner契约的规范序列化摘要。
- `protectionDefinitionHash`：目标archive定义、全部专用guard、owner索引schemaHash与相关已知attachment的规范序列化摘要。不是仅触发器名列表。
- `sourceExpected`：若原版本owner明确允许该表尚不存在，仍须给该表一个经过验证的ABSENT契约；不能把58表注册表过滤成当前存在的表。后续创建该表使schema cookie/定义计划改变，旧absence证书随即失效。

计划哈希包含DB身份、版本和全部58表合同。缺表、额外未知表、目标版本变化，都不能复用旧计划cursor。新计划必须显式创建新run，保留旧ledger。

## 有界工作与持锁范围

`Cost`/`Budget` 对 **一个表** 的DDL条数、列数、attachment数和定义字节数设硬上限。超过上限抛 `UNIT_REQUIRES_SMALLER_OWNER_SLICE`，施工方必须进一步拆该owner的列/guard准备步骤。不能增大上限让所有表重新进入一个事务，也不能返回READY。提供的宿主预算不是实机性能承诺；DDL数量有界不等于 SQLite 执行时间已达标，必须测实际 hold p95/p99。

不允许单元内扫描/拷贝全部原始行、hash大BLOB、创建checkpoint文件或跑训练。准备工作按一致只读快照分块进行；writer中只核可廉价复验的输入revision、schema cookie、owner fence与原件证明，再安装一个表的元数据并写回执。**095没有为全部保护表提供可直接替代全行指纹的通用内容版本计数器**；旧全量 `protectedTableFingerprints` 的等价保全设计仍是集成必做项。不能把核心中的 `originalContentRoot` 字段当作已实现的低成本原件证明。它必须来自旧owner认可的完整原件/版本证明，并在writer准入时验证没有变化；若当前表缺可用证明，明确 WAIT_PREPARED_PROOF，不能填固定hash。

一个表需要多次DDL时，要保持旧guard有效，最后一个短事务才切换保护索引的schemaHash并验证新guard全量存在。新保护尚未完成时，破坏性更新/退役必须继续被原owner挡住；不能先取消旧trigger再等待下一片。

## cursor、事务与 ACK 协议

`Cursor` 必须从 `r34_schema_unit_run` 的耐久行读取，包含 planHash/runId、INSTALL或VERIFY阶段、nextIndex、revision、ownerEpoch/fence、auditRound/auditSchemaCookie。每个调度tick只准备当前一个unit，不允许跳过坏表推进总cursor。

1. 在writer外按当前unit取得一致 `View`、准确 `Cost` 和不可变DDL。调用 `prepare` 形成Ticket。Ticket的commitKey由run/phase/unit/auditRound确定；同一未决单元不会因重试改key。
2. 请求原 `DatabaseCoordinator.WriteClass.MIGRATION` writer并开启唯一事务。先查相同commitKey的receipt：已经提交则读回原receipt，不再执行DDL；同key不同payload立即完整性失败。
3. 在事务内从durable cursor和当前schema重新核身份。调用 `beforeWrite`，拿到不能直接构造的Admission；任何旧owner/fence、deadline过期、schema变更或原件输入变化都在变更前拒绝。schema_version不同要重新准备，不能把旧DDL盲重放。
4. 原owner执行一表的精确DDL/guard校验。生成Effect，调用 `receiptPayload(Admission, Effect)`。它检查精确目标定义和原件root相同；VERIFY阶段禁止任何DDL，schema cookie必须不变。
5. **同一事务** INSERT不可变receipt，并CAS更新run cursor。CAS条件至少包含runId、planHash、revision、phase、nextIndex、ownerEpoch、ownerFence、auditRound；影响行数必须是1，否则整事务回滚。不能先提交DDL、再另一次事务写cursor。
6. writer事务结束以后，以独立读取连接的一致快照同时读receipt和cursor，构造ReadBack；只有 `resolve` 返回 `ACKNOWLEDGED`，内存调度才使用下一cursor。`ReceiptPayload`对象、`setTransactionSuccessful`、返回true、heartbeat都不是ACK。

若明确 `ROLLED_BACK` 且无receipt，保持同unit，按资源等待退避；等待次数不能消耗业务失败额度。若 `UNKNOWN` 且未读到receipt，返回 `WAIT_DURABLE_RECEIPT`，禁止重复执行。恢复owner应先取得新fence、确认旧事务已结束，再读commitKey和旧cursor，才能把缺receipt判成可重试；不能仅凭超时猜rollback。若报告 `COMMITTED` 但读不到receipt，属于 `COMMITTED_RECEIPT_MISSING`，必须诊断，不得显示成功。

提交之后、ACK之前崩溃时，原receipt和cursor都存在；恢复直接消费这一原件。若读回时另一个合法owner已经推进后续cursor，核心要求 `RELOAD_DURABLE_CURSOR`；重新载入权威状态，不让旧owner写回旧指针。

SQL文件仅定义账本。INSERT/UPDATE必须由现有writer适配器按上述条件执行；SQL本身不声称能约束所有Java调用者。租约接管需要同事务递增fence和revision，不能只改内存owner。

## 验证阶段与重启快路径

INSTALL阶段完成58表后进入VERIFY，按一次一个表验证真实定义，不再跑全表保护安装循环。VERIFY第一单元绑定auditSchemaCookie；任何期间的外部DDL使cookie变化，必须CAS执行 `restartVerification` 提案，从第0表重新验证，auditRound递增，保留之前回执。该方法返回的是待提交提案，不能直接当作耐久状态。

只有58个VERIFY receipt全部在同一schema cookie下完成，才产生DONE。普通重启可使用“DB原件身份 + planHash/目标版本 + DONE耐久cursor + 相同schema cookie”的有限查询检查该retention证书，仍应执行现有关键域和启动原件的独立检查。发生任何schema变化、restore epoch变化或目标定义变化，证书失效，重新有界验证。禁止把“表都存在”或静态boolean当替代条件。SQLite schema cookie属于本应用受控迁移协议中的变化探测信号，不作为对恶意数据库篡改的密码学证明。

## 已运行的测试与不能扩张的结论

运行命令：

```bash
python analyses/runtime_payload/supplement/tests/verify_schema_unit_plan.py --baseline source095
```

本次通过21个Java协议断言、实际58表注册表匹配、116个fixture unit的完整INSTALL/VERIFY推进，并用真实SQLite完成6个故障场景：

|故障|真实检查|
|---|---|
|子进程在DDL/receipt/cursor写完但COMMIT前 `os._exit`|重开DB：DDL回滚、cursor未推进、receipt不存在、原BLOB未改变。|
|子进程COMMIT后、ACK前退出|重开DB：仅一receipt和一次cursor推进；重试读回不重复安装；原guard在删除source时保存完整BLOB及SQLite storage class。|
|同名trigger具有错误SQL|安装前拒绝；不DROP、不自动覆盖，不推进cursor。|
|过期owner fence|不能创建archive或推进cursor。|
|真实第二SQLite writer持锁|首次实际 `database is locked`；释放后同unit成功，原件不丢。|
|准备后schema cookie被其他DDL改变|在任何unit变更前拒绝，要求重新准备。|

SQLite夹具的source表DDL来自095 `RuntimeSchemaR26.FEEDBACK`；archiveSQL和基础guardSQL由从095 `ResearchRetentionStoreR30` **原样提取**的Java builder编译生成，未重新手写一套SQL来证明自己。夹具适配器仍不是Android生产适配器；它不覆盖所有58张表的专用guard组合、完整checkpoint保全、原全域指纹、实机并发、报告导出或APK升级。宿主使用Java21编译模块、`-source 17 -target 17`，没有全Android类编译。

## 施工验收与回滚门

完成旧owner接线后，B01至少再交付：58表全部真实定义manifest及owner来源；逐单元崩溃/重启回执；已有旧guard+专用退役guard的迁移负例；checkpoint保全独立回执；两次普通重启无全量DDL/全行hash持writer的trace；关键抓取并发下hold p95/p99；未知定义篡改时WAIT_SCHEMA但不丢原件；Android编译、同签名升级、真机结果。缺任何旧保护的等价替换，整批保持OPEN。

回滚只停用新调度并保留原ledger与原件。未知schema状态禁止直接安装旧APK并开放写入；只有旧版本兼容性和保护定义验证通过才恢复旧owner。不能删除R34 receipt或恢复旧cursor来伪装“从没迁移”。
