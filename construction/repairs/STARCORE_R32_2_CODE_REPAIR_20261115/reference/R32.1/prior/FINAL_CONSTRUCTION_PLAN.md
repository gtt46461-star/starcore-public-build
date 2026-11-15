# STARCORE R31.1—R31.4 统一最终施工方案

日期：2026-11-12（北京时间）。性质：同一R31方案的全量保全、真实源码返修及生产调用闭环实施合同。不是新起一套缩水方案，不宣称施工或APK已经完成。

## 1. 核对结论与统一规则

已逐项机器比较R31.1、R31.2和当前R31.4的56张原卡：title、issue、files、steps、acceptance、rollback完全相同；194条原验收文本全部保留。当前另有ENG-01/02两张真实修复卡，验收分层后为222项：LOCAL189、EMU8、DEVICE24、NATURAL1。PLAN_PRESERVATION_PROOF.json及ORIGINAL_TO_CURRENT_ACCEPTANCE.csv给出完整证据。文本保全通过不是源码实现通过。

31.1/.2/.3/.4主要是施工控制的连续修订：31.1纠偏接续，31.2顺序批与分层，31.3改为先施工后批末棘轮，31.4把21批合并为11批。31.2的三张临时工作流卡不再独立成卡，其CI/HOST、在制品身份和验收目的并入UF01/UF18；不能把失效的“开工前先全绿”再叠回流程。

优先级：最新用户要求 → 全量功能/数据/阈值保护合同 → 17分册的批次编号覆盖 → 16分册执行规则 → 14/15分册历史与未被替代目的。历史收据只是带源码身份的证据，不能取消需求。原acceptance_config.json不得改字节；功能冲突先列事实、采用依据、兼容方案与验证，不静默删功能。

统一底本为original_R31_4完整原包（打包时原样附带），保留全部56+2卡、47图、20矩阵、16逻辑表、44指标/96检查、40合并要求、1832旧要求/299旧卡、190个精确源码锚点与专题10—13。新20返修卡只作为原功能卡的实施补缺覆盖层，不替代这些原文。

## 2. 当前到底完成了什么

已做：快检与对应源码定位、跨版本功能保全核对、历史收据核对、关键生产链路审阅、部分运行修复原型、一次真实主机OS锁回归。未做成：当前版本全套工程通过、完整生产报告语义通过、原机670MB旧库恢复、24/72小时、自然优势证明。应用Java共1278文件，本轮聚焦123个原卡路径及实际关键调用链；不声称逐行穷尽审完1278文件或所有缺陷。

源码审阅已确认部分真实能力存在：ResearchPool的故障分类、owner/fence提交、隔离与半开恢复，进程内health的生产查询，以及历史训练与AtomNet部分发布/消费保护。它们要按原卡保留并在当前整链重验，不能因其他模块缺陷统称全空壳。相反，类和helper虽存在但生产入口仍旧或缺调用的项目，要明确作为未完成接线返修。

HANDOFF_BASE.json固定已保存源码检查点。检查点包含未验收改动，旧库切片和新报告辅助方法仍有明确阻塞；不能直接编包称成品。原APK577311020为本轮输入，577311021只是未交付候选身份。Codex必须检查最新分支差异，保留工作后继续，不从旧main或5237重做。

## 3. 快检的正确解释

- RUNNING/READY只证明进程与部分初始化，不证明抓取、预测、结算、训练正常。
- 本次报告request已FAILED且active/queued=0，失败发生在入队前；不是一直有后台队列等着。两次ACK也被同一锁拒绝。
- 同时有迁移ALTER及多线程等待SQLite连接，证明该瞬间数据库竞争；单次堆栈不能独自判永久死锁。
- 本次启动在目标期开奖之后，不能补造该期赛前预测。后续未来期必须证明恢复。
- uptime是设备单调时间，不是应用连续运行时长；crashCount=0不能抹去系统退出记录。job.lock存在/很旧不证明OS锁被占，禁止按mtime删锁。
- 剩余磁盘充足不等于预算协调锁可用。UNKNOWN、-1和无样本不写成0、胜率或满足P95。

## 4. 按31.4的11批落实，重开的是验收，保留的是功能

K01：全量保全、身份、真实状态与CI基础（UF01、UF18基础）。K02：原owner/fence、故障分类、内存与队列能力全保留并复验。K03：业务零DDL、严格迁移与短写者（UF02—04）。K04：真实抓取/冻结/结算、研究健康、预算；报告持久请求/ACK/发布阻塞在本批先修通（UF05—08、UF12；属必需前置依赖，登记scope_extensions，不等K10才恢复可诊断性）。K05：原历史回放/模型状态共同提交及PIT复验。K06：原全部家族、typed AST、创造、父子比较、完整工厂与真实失败假设接线（UF13—14）。K07：原矩阵依赖/融合和16表真实读者（UF16）。K08：原主链权威、资格和撤销（UF17）。K09：原引用闭包GC/CAS/WAL/缓存迁移及47图（UF09、UF15），必须保留原前置主链迁移顺序。K10：完整R31生产报告、全部对象与保存回读（UF10—11；UF06—09重新联合验收）。K11：原故障矩阵、完整当前CI、旧库/设备层与成品交付（UF18最终、UF19—20）。

旧K01—K11收据不覆盖、不改成新源码PASS。新执行收据放execution/Kxx/，引用原卡/222项和本轮UF项。每批报告真实源码diff、通过/失败、未完成与下一动作。不存在“先把所有功能都修好才准改第一批代码”的前置门。

## 5. 每张原卡的真正完成定义

每张卡必须有需求→实际生产入口→生产者→持久产物→消费者→反馈/撤销→检测字段→测试原件链。新增类、辅助函数、表/图存在、单个helper测试、线程RUNNING、代数增加均不能独自关闭应用功能。未部署、缺输入、缺产物、未消费、无效果与证据不足必须分别表示。

新helper静态未见调用时，先检查生产中是否已有等价实现；完整等价需逐条证明并绑定实际集成验收，避免重复造第三套状态。已有实现继续使用和完善，确实缺少的新合同或功能必须接线实施，不能只加说明、计数器或UNSUPPORTED文案代替。

原189项LOCAL仍逐项绑定；需要真实生产路径的验收不得绑定helper-only测试冒充。每卡保留实现、静态接线、集成执行、Android、原机及自然质量不同状态。新失败归属明确；本批新增失败必须修，旧失败先登记责任但不得无限结转；最终工程发布前原LOCAL及本轮工程检查清零，测试数不减、skip不增、编译失败阻断。

不因增加类别、数量或复杂度就认定算法更强。保留有效的历史训练快速筛劣、完整家族/专家、每期标签/反馈、动态进化/变异/融合、严格子代独立证据；合成实验与历史分数不转成未来/REAL胜率。达标策略可进入唯一主链，不达标保持理由，不强行生成号码/放低资格凑非0。阈值以原文件为准。

## 6. 完整检测报告必须能回答的问题

同一水位事实快照生成全部机器/人读产物。逐项回答：当前版本与签名；服务活着及业务推进；各阶段最近真实commit/时延分布；每期缺号/缺冻结/结算尾部；训练/选择/确认/未来各自样本；各家族/专家/策略/资产/模型状态；创造与失败反馈的真实路由；父子比较和资格/撤销；47图/20矩阵/16表每项生产、消费、效用与缺失；44指标与96检查的定义、输入、分母、scope、状态、证据；当前胜率是否有充分独立证据；每项不足对应源码/修复卡与可执行下一步。

文件生成成功、证据结构完整、检查都执行、功能健康、自然质量达标分开报告。M32不得固定0；没有输入就UNKNOWN。恢复诊断包不是完整报告。报告链要覆盖请求持久化、服务启动、快照、生成、打包、发布、ACK、UI取回、Provider、SAF、读回、取消与重启；每个断点有持久恢复策略。

## 7. 验证与交付

先局部真实故障复现和代码修复，再跑所属整类；每批末在稳定源码跑全量。无新修改/新失败不反复跑相同CI。当前工具链检查一次；本地缺SDK不冒称无法施工，使用既有签名/构建lane；不盲目重试代理。正式构建不能跳过身份门，调用索引须来自最终真实编译classes。

必须包含：严格DDL故障/重放、迁移期间关键写入且during>0、真实kill/restart、>5秒跨进程锁、提交与kick边界、ACK双失败重启、READY时grant竞争、孤儿job、读者跨清理期、R31生产报告语义、真实typed子代与完整工厂实验、实际47图/16表读者、真实API新抓取—赛前冻结—其后结算—训练反馈、原故障矩阵及原主链红线。

最终单一干净提交构建同包名同证书的递增版本release APK，并交付APK+ZIP+源码与资产+公开签名信息+SHA256+逐项验收回执。没有原机时继续工程工作，可以交付明确标注的工程候选，但不能宣称V2353A旧库、24/72小时或自然优势全部通过。平台force-stop限制及未知运行间隔如实呈现。任何“完美完成”须由原项目项和本轮故障验收共同支持，不能靠文档承诺。


## 8. 二十张真实返修项目卡


### UF01 唯一源码、修订合并、真实完成状态

原批次：K01；回开原卡：R31-B00-C01, R31-B00-C02, R31-B12-C05。


**源码文件**

- CURRENT_FINAL_PLAN.md
- construction/r31/NEXT_ACTION.json
- tools/r31/verify_baseline.py


**已定位事实/缺口**

- 旧入口仍写K02，旧批次收据只证明记录源码的LOCAL层；123路径存在不能证明58卡功能已部署。


**必须真实改代码**

1. 采用HANDOFF_BASE源码检查点；保留本地/远程未合入工作，不从5237或main覆盖重做。
2. 建立56+2卡、194原验收→222现验收、1832旧要求/299旧卡的保全映射；旧1832账本原先仅路径重叠索引，未完成语义核查，必须继续逐项落实生产能力/验收归属及缺口，不能宣称1832项已实现；31.1/.2/.3/.4功能统一，不复活被替代的前置构建流程。
3. 每项分列历史收据、当前生产入口、当前执行、设备/自然证据；一项辅助测试不能伪装整项运行。


**关门验收**

- 机器验证56卡六个功能字段跨修订完全一致；194原验收无孤项；阈值文件字节不变。
- 故意错源码、dirty、旧APK、旧结果或缺文件时必须拒绝当前PASS。


### UF02 业务路径零DDL，迁移只由唯一owner执行

原批次：K03；回开原卡：R31-B02-C01, R31-B02-C02。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/DbStore.java
- app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java
- app/src/main/java/com/ethk3/novaonepro/LegacySchemaCoordinatorR926.java


**已定位事实/缺口**

- saveDecisionInternal、saveTeacherMatrixSample、saveLearningCase等仍调用ensureRuntimeExtraTablesOnce，能在业务池同步执行迁移；独立连接只包住显式后台操作。


**必须真实改代码**

1. 拆分requireReadyReadOnly与advanceMigrationOwned；所有业务/报表/健康读取只能检查，不得建表、加列、补默认值或推进迁移日志。
2. 业务遇缺表/缺列返回有类型的SCHEMA_PENDING并向唯一owner投递幂等请求；选定DDL连接贯穿schema、日志、验证。
3. 扫描所有DDL入口及调用图，登记允许的owner操作；保留唯一写者和提交结果合同。


**关门验收**

- schema缺失/迁移中的每个业务入口执行零DDL、零迁移日志推进、零业务误写。
- 并发100次请求只保留一个迁移owner；旧owner/fence不能写。


### UF03 严格DDL、单元后置条件与失败自动重放

原批次：K03；回开原卡：R31-B02-C02, R31-B12-C01。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/DbStore.java
- app/src/main/java/com/ethk3/novaonepro/DbHelper.java
- app/src/main/java/com/ethk3/novaonepro/LegacySchemaCoordinatorR926.java
- app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java


**已定位事实/缺口**

- ensureLedgerIndexes与execQuiet吞CREATE/INDEX错误；当前列错误审计不能覆盖它们。
- MAIN_INDEXES可被写COMPLETED而缺索引，最终验证失败；incompleteRows>0又使completed单元无法重开，形成恢复死结。


**必须真实改代码**

1. 列出所有生产必需表、列、索引定义、默认值、约束与域归属，逐单元严格验证实际产物后才能COMPLETED。
2. 迁移禁止使用吞异常的execQuiet；失败保留第一条实际异常、SQL单元ID及完整cause chain。
3. 最终校验发现漂移时仅使负责单元失效并重放，不能只无限重试最终验证；已正确完成单元保持进度。
4. 旧READY必须经过当前审查，不能仅凭旧stamp跳过；第一条列/DDL失败即停止本单元后续SQL。


**关门验收**

- 逐个注入CREATE/ALTER/INDEX失败：失败必需操作绝不能COMPLETED或READY。
- 撤销故障后，无人工删journal/清数据，自动恢复READY；原行/列值和保护记录逐行不变。
- 旧READY且真实缺列/缺索引的旧库可修复；不仅是删除测试收据的空壳fixture。


### UF04 真正短迁移切片与关键写者进展

原批次：K03；回开原卡：R31-B02-C01, R31-B03-C01, R31-B05-C09。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java
- app/src/main/java/com/ethk3/novaonepro/DbHelper.java
- app/src/main/java/com/ethk3/novaonepro/DbStore.java
- app/src/main/java/com/ethk3/novaonepro/StorageCheckpointR31.java


**已定位事实/缺口**

- 当前RUNTIME_COLUMNS、FIELD、DYNAMIC_WINDOW、BASE、INDEXES仍含多表/多语句扫描；写者deadline只约束获取锁，不约束task.call。


**必须真实改代码**

1. 持久游标细分至有限DDL单元，避免用“一个表/一个功能组”冒充耗时有界；每单元记录语句数、耗时、锁持有和取消状态。
2. 切片之间释放写者并按原优先级允许当期draw/freeze/settle；大索引不能伪装短单元，须沿原B05-C09安全重建/校验/切换合同设计，保护活动pin、原件和新写入，单独可观测、可中断与可恢复，不把后台工作转到主线程。
3. 维持原P99≤50ms与>200ms缺陷披露要求；不可宣称任何不可分索引天然满足该目标。
4. 审计实际所有WAL checkpoint入口；StorageCheckpointR31静态未见生产调用，不能以策略helper替代真实入口的mode、deadline、结果与业务延迟验收。


**关门验收**

- 代表性约670MB有数据旧库下，迁移未结束期间必须观测多次关键写提交，during不得允许为0。
- 在单元前、部分autocommit DDL后、完成journal前杀进程，重开续做且不重复业务效果。
- 记录全分布、最坏20次及超标原因；不只展示一秒调度目标或锁获取时间。


### UF05 存储普查与准入临界区分离

原批次：K04；回开原卡：R31-B05-C01, R31-B05-C02, R31-B05-C11。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/StorageBudgetR31.java
- app/src/main/java/com/ethk3/novaonepro/StorageCensusSU2.java
- app/src/main/java/com/ethk3/novaonepro/ReportIoR31.java


**已定位事实/缺口**

- 5秒只限制获取锁；reserve/ensureAdditional/snapshot/recover仍在全局OS锁内遍历目录，无耗时上限。


**必须真实改代码**

1. 昂贵普查移出临界区，使用generation/fence/作用域核对后提交保守会计；过期、不完整、取消的普查明确重试或拒绝。
2. 控制元数据、bulk workspace、已发布报告分清预算，给必要ACK/取消/恢复元数据预留有界且预先计费的空间，不能无预算旁路。
3. 保留OS租约、跨PID总预算、原子写、有效pin和原文件保护；不要只增timeout。


**关门验收**

- 两个真实进程、慢/大目录、并发文件增长、崩溃owner、取消、磁盘不足下会计正确。
- 报告锁竞争不再使实时写入或报告请求不可恢复；未持锁不能改账本。


### UF06 最小请求先持久化，再采可选证据

原批次：K04→K10；回开原卡：R31-B05-C02, R31-B11-C03。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java
- app/src/main/java/com/ethk3/novaonepro/ReportRequestQueueCP12.java
- app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java


**已定位事实/缺口**

- requestExport仍先采集并写长观察，再写请求/FIFO；超出5秒占锁可再次导致从未入队。
- enqueue成功但kick失败仍进入统一requestFailure，造成后台有任务、界面FAILED。


**必须真实改代码**

1. 先原子持久化有界requestId/模式/身份/时间及可恢复提交意图，再补充可选观察；每个边界可幂等reconcile。
2. 区分未提交、已提交待服务、采集、生成、等待资源、失败、取消；瞬时竞争变成持久等待/重试，服务start失败不等于请求从未提交。
3. 可选观察失败记UNKNOWN/DEP/TIMEOUT，不虚构值；永久I/O故障有真实终态和可导出的恢复原因。


**关门验收**

- 在请求意图、请求文件、FIFO、kick前后逐点杀进程，重启不丢请求、不重复job。
- 存储锁被占超过5秒、FGS启动暂拒、观察器异常仍能恢复生成或明确失败，不停在假排队。


### UF07 ACK outbox、READY重连与同一报告恢复

原批次：K04→K10；回开原卡：R31-B11-C03, R31-B12-C01。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java
- app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java
- app/src/main/java/com/ethk3/novaonepro/CompleteReportFileR927.java


**已定位事实/缺口**

- transition先存READY再写ACK；两次ACK失败后FIFO已出队，服务终态+空队列直接返回，UI又不kick READY，只等ACK。


**必须真实改代码**

1. 为ACK建立持久待发状态/单调revision，并在启动与drain中独立于FIFO修复；不依赖仅16条的进程内失败缓存。
2. 取回已READY任务直接验证权威job、manifest、ZIP后恢复分享/保存，缺ACK不应永久遮蔽有效产物。
3. UI依据requestId/jobId/revision显示真实阶段、阻塞原因、最后进展和可执行恢复，不错误触发重做快照。


**关门验收**

- 故意让READY ACK及重试都失败，杀进程重开后恢复同一jobId、同一ZIP hash，备份次数不增加。
- 旧ACK不能覆盖更新状态；进程内缓存清空后恢复仍成立。


### UF08 发布可分享的原子边界与租约竞争

原批次：K04→K10；回开原卡：R31-B05-C02, R31-B11-C03。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java
- app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java
- app/src/main/java/com/ethk3/novaonepro/ReportIoR31.java


**已定位事实/缺口**

- READY ACK发生在workspace目录reservation释放前；UI grant独立预约job/share-until.json会与目录scope冲突。


**必须真实改代码**

1. 定义验证完成、stream/reader关闭、workspace owner释放、发布READY/ACK和分享控制元数据的明确顺序。
2. 或采用经授权的独立发布后控制作用域；保留预算、文件hash、发布不可变性与reader pin，不靠跳过准入解决。


**关门验收**

- 在READY ACK后刻意暂停owner，立即执行UI grant及Provider读取/保存，必须成功或持久等待后自动成功。
- 并发取消、删除、分享、重启不损坏已验证ZIP与真实数据库。


### UF09 证据生命周期生产接线、孤儿恢复与有界清理

原批次：K04/K09；回开原卡：R31-B05-C03, R31-B05-C04, R31-B05-C05, R31-B05-C06, R31-B05-C07, R31-B05-C08, R31-B05-C10, R31-B05-C11, R31-B12-C01。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java
- app/src/main/java/com/ethk3/novaonepro/ReportBudgetSU2.java
- app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java
- app/src/main/java/com/ethk3/novaonepro/CompleteReportFileR927.java
- app/src/main/java/com/ethk3/novaonepro/EvidenceReachabilityR31.java
- app/src/main/java/com/ethk3/novaonepro/StorageGcR31.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/KeepMigrationR31.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java


**已定位事实/缺口**

- 无关缺失/损坏job.json可使latestFullReport整次失败；先删ACK后遇读者租约会让遗留目录以后永远无法回收。
- 旧可选观察/快检使用cached线程池；当前有界原型未做忽略中断的超时长期测试。
- KeepMigration/引用闭包GC/事件有界等新接口有实现，但静态未见完整生产Store、root/edge/pin与append接线；CAS缩页和部分revision/cache保护已有真实调用，须保留而非整体重写。


**必须真实改代码**

1. 保留可疑原件，以类型化警告隔离孤儿/坏job，继续选择有效报告。
2. 逐项把keep迁移、引用闭包GC、CAS陈旧页恢复、事件有界聚合及旧库重建接入原合同允许的实际owner/显式维护入口；静态未见调用的工具类不能算完成。必须先完成P05权威迁移再做P06清理，保留原件、活跃pin、迁移收据和新写入。
3. 保留可恢复cleanup tombstone，直到所有生成目录真实退役；读者关闭后续清理必须能发现它。
4. 报告观察器/快检工作者及队列有上界，超时不退出的读者继续占槽，容量不足记未知而非无限开线程；取消不得吞掉。


**关门验收**

- 有效READY旁放空目录、坏retention job和不可读无关journal，仍能取回有效报告。
- 持有读者跨过过期时间，清理不删原件；读者释放后下一轮能完整清理。
- 读者忽略中断并超过timeout，重复请求后线程/队列仍有界，完整报告不会被可选观察永久阻断。


### UF10 R31指标、对象状态和真实跨源冲突进入生产报告

原批次：K10；回开原卡：R31-B11-C01, R31-B11-C02, R31-B11-C04。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR27.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR29.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricEvaluatorR29.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckMetricCrosswalkR31.java


**已定位事实/缺口**

- 生产R30Input仍调用evaluateR30；evaluateR31/crossSourceR31主要为测试调用，M32固定0/44。
- ReportSnapshot仍用旧build，新的六层对象状态辅助入口未接真实生产采集。


**必须真实改代码**

1. 生产快照内接R31定义、collector、crosswalk及对象原六状态：DEFINED/IMPLEMENTED/RAN/PRODUCED/CONSUMED/EFFECTIVE（定义/实现/实际运行提交/产出/消费/有效）；输入与部署另列附加字段；每项绑定身份、role、scope、水位、时间、原件。
2. M32比较真实同scope/同水位事实；不同role/epoch区分不可比与矛盾；缺源为UNKNOWN，不能固定0。
3. 单一事实快照派生JSON、HTML/Markdown和图表；44指标、96检查、47图、20矩阵、16表及能力集合逐项完整解释。


**关门验收**

- 通过ReportR29.fromSnapshotR30到document到真实ZIP验证，不只调用helper。
- 注入真实跨源矛盾、合法不同scope、零样本、缺源和有效样本；最终打包报告正确反映状态/分母/证据，不宣称全部健康。


### UF11 完整报告语义、附件闭包及保存回读

原批次：K10；回开原卡：R31-B11-C03, R31-B09-C04。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java
- app/src/main/java/com/ethk3/novaonepro/ReportArchiveVerifierCP12.java
- app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java
- app/src/main/java/com/ethk3/novaonepro/DiagnosticReportProviderR923.java
- app/src/main/java/com/ethk3/novaonepro/CompleteReportFileR927.java


**已定位事实/缺口**

- 现有新增联测主要验证READY、Provider字节hash；没有验证ZIP内完整章节、对象、数据契约与可复算原件闭包。


**必须真实改代码**

1. 生成同快照JSON与人读报告，逐项检查12章、44/96及全部对象、附件、失败/未知原因、修复卡；含运行、训练、家族、进化、模型资格与实盘状态。
2. 验证ZIP解压/CRC、分卷重建hash、大小、路径、索引和必须成员；预算不足列出精确缺失，不伪装完整。
3. 将生成、发布、分享Provider、SAF文件/目录保存、保存后回读分别验收；恢复诊断包明确不是完整报告。


**关门验收**

- 缺关键章/损坏附件/错水位/错jobId/同名旧报告必须被最终验收抓到。
- 真实Service生成后，经Provider与SAF落文件回读，hash和报告语义一致；证据完整、检查执行完整、业务健康三项独立。


### UF12 抓取—下一期冻结—结算—训练事件闭环

原批次：K04/K05；回开原卡：R31-B03-C01, R31-B03-C02, R31-B03-C03, R31-B06-C01, R31-B06-C02, R31-B06-C03。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java
- app/src/main/java/com/ethk3/novaonepro/AnalysisRequestStoreR36.java
- app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java
- app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java
- app/src/main/java/com/ethk3/novaonepro/OosAfterDrawWriterR36.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java


**已定位事实/缺口**

- 快检有抓取成功样本但业务等待canonical，分析/结算显示旧时间；当前进程在本期开奖后启动，该期缺冻结不能反向伪造。


**必须真实改代码**

1. 每期跟踪HTTP/解析/draw_commit/next_freeze_commit/settle_commit/label/feedback/research_commit各独立时间和水位，旧回补不挡当期。
2. 唯一赛前冻结、同owner deadline不刷新、迟到结果只入研究；合法NO_BET说明原因但不能冒充有K3预测。
3. 结算outbox幂等，训练标签与研究反馈不因无REAL/无冻结永久饥饿；历史回放与模型状态共同提交，5000期滚动及pin保护。
4. 倒计时依据单调时钟投影与合法开奖时间；数据陈旧与倒计时单独显示。


**关门验收**

- 真实生产服务观测新抓取、新未来冻结、随后对应真实开奖结算、标签反馈及研究提交，不能只看心跳。
- 错序/重复/修正/缺号/网络超时/重启/回补下无重复结算、无未来泄漏。
- 原P95≤1秒结算、≤5秒冻结按真实子阶段分布验收，源晚到单列。


### UF13 研究进展、可执行失败假设与regime接入

原批次：K06；回开原卡：R31-B04-C01, R31-B07-C06。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDirectedResearchR26.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchHypothesisR31.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/RegimeHypothesisR31.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java


**已定位事实/缺口**

- FailureDirectedResearch仍先用F08/F17选子代，再追加假设metadata，component为空；dispatch/recordOutcome及Regime辅助逻辑未发现生产调用。


**必须真实改代码**

1. 按真实失败证据在birth之前路由typed可执行adapter；有component证据才使用component假设；不支持的机制明确UNSUPPORTED。
2. 连接实际Regime检测/窗口/路由/结构构建并持久化状态，保留原父代对照、fence、游标事务、家族与专家能力。
3. 研究健康用已提交cursor/产物/反馈衡量，attempt、RUNNING、owner代数不当成进展。
4. 子代独立训练/选择/确认/未来证据；outcome credit幂等写入，不继承父证书。
5. recoverOneR31的恢复控制异常必须分类并独立记录原因/次数，不得吞RuntimeException后显示健康；保留已有生产owner/fence、隔离、历史训练及AtomNet消费保护。


**关门验收**

- 六种受控失败经真实select→birth→recordWithin产生对应AST/谱系或UNSUPPORTED；不能只比dispatch字符串。
- kill/restart后cursor、credit、产物一致；空转线程不能显示研究推进。


### UF14 完整创造工厂实验而非四程序演示

原批次：K06；回开原卡：R31-B07-C01, R31-B07-C02, R31-B07-C03, R31-B07-C04, R31-B07-C05。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchCapabilityExperimentR31.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/BirthGateR27.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java


**已定位事实/缺口**

- 当前candidates固定4程序，PRODUCTION_STAGES没有驱动实际family/expert/proposal/进化编排，不能证明完整工厂能力。


**必须真实改代码**

1. 保留四程序测试为内核测试；另建驱动真实工厂的可复现adapter，使用隔离SYNTHETIC命名空间、预算、seed与真实搜索路径。
2. 记录所有候选/淘汰/失败、搜索暴露、训练—选择—确认—资格门及成本；失败试验不能标COMPLETE。
3. 保留所有有效家族、typed AST、变异/融合/淘汰和父子比较；历史快速训练筛劣继续可用。
4. 家族kernel、Foundry遍历、AST出生门、CreationBudget和FrozenConfirmation已有生产实现，先核对新接口与这些实现的等价合同；FamilyRegistry新接口或VerifiedImprovement helper未调用既不能算新合同完成，也不能误判原创造能力全不存在；以同机会父子、冻结确认、未来证据的生产回执关门。


**关门验收**

- null、注入信号/禁用信号、漂移、缺口实验经过完整工厂，给出trial级不确定性及完整搜索分母。
- 合成检出力不计自然胜率，不保证每代或每次变异必然更强。


### UF15 图表新鲜度、ABI、取消和健康口径

原批次：K09；回开原卡：R31-B09-C01, R31-B09-C02, R31-B09-C03, R31-B09-C04, R31-B09-C05。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java
- app/src/main/java/com/ethk3/novaonepro/ChartRegistryR31.java


**已定位事实/缺口**

- 缓存READY直接透传；updated_at仅展示，points列表被当ABI；全快照缺普通panel字节上限；取消被RuntimeException吞掉。
- 每次缓存刷新又完整capture报告，健康文案仍用旧CPU/代数指标。


**必须真实改代码**

1. 读取前检查字节、schema/ABI、hash、source watermark；根据时效独立判STALE/UNKNOWN，不能信缓存READY。
2. 各panel错误有类型、相互隔离；取消向上传播；缓存由已有事实快照投影，避免图刷新触发完整报告。
3. 分别显示服务活着、研究提交推进、资格/自然质量，47稳定ID与全部功能保留。


**关门验收**

- 陈旧合法READY、错ABI/hash、超大快照、取消、单图损坏均经真实repository/load/render验证。
- 线程RUNNING但无commit不得显示研究健康；一个坏图不拖垮其他面板。


### UF16 16逻辑表实际读者与T09对象数值投影

原批次：K07；回开原卡：R31-B08-C01, R31-B08-C02, R31-B08-C03, R31-B08-C04, R31-B08-C05。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAdaptersR30R1.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchMeasureProjectionR31.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStudyTaskR30R1.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchResultContractR31.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java


**已定位事实/缺口**

- T09仍读顶层mean_gain，产品原件可能为null；正确frame.points投影及page接口主要是测试调用；导出SQL合同不等于表格能用。
- 矩阵按kind状态/依赖及source→proposal→BORN同事务消费已有真实实现；新usages/withoutSourceRun及统一validateForConsumption/数值效果合同缺生产调用；部分family/selector/calibrator为UNSUPPORTED，未见fusion adapter，不能仅以通用helper通过关门。


**必须真实改代码**

1. 接入有界、只读的真实产品读者/渲染及报告路径，执行16个逻辑查询、筛选与分页。
2. T09按对象展示gain、区间、样本、node cost和payload hash；CPU未知保持未知，role/dataset/source过滤严格。
3. 矩阵产物的生产、消费与实验效果各有回执，不把统计表行数当学习。
4. 将统一结果校验、真实消费效果/去源对照、完整source身份贯穿实际矩阵/研究/提案/出生/报告；不能仅输出Spec元数据。
5. 按原卡落实family、selector、calibrator、fusion等应支持的真实适配；原要求未实现时UNSUPPORTED必须记未完成。缺学生/rt.evo按实际依赖kind隔离等待，已有可运行kind继续真实提交。


**关门验收**

- 顶层mean_gain=null但frame.points有值时，真实界面/报告呈现正确对象值。
- 16视图全过当前schema、分页、缺源、缺产品、错role/source和只读不改业务库测试。


### UF17 主链资格、真实选择与撤销闭环

原批次：K08；回开原卡：R31-B10-C01, R31-B10-C02, R31-B10-C03。


**源码文件**

- app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationWindowR26.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java
- app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceContractR31.java
- app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java


**已定位事实/缺口**

- 新快检没有足够新冻结/结算/未来样本，不能据此判定当前资格与高胜率；不能用工程修复强行主链非零。
- typed双写及授权读取确已接入，但switchToTypedRead/revoke未见生产调用，默认LEGACY_READ；model_state_hash当前传semantic ABI hash，不是实际模型状态发布回执；新风险合同及机会分母仍缺生产编排。


**必须真实改代码**

1. 原数值门槛字节不变，typed权威选择、缓存投影、唯一DecisionGate及撤销/过期生效期闭环。
2. 达标策略真实进入主链且可追溯；不达标如实显示原因和缺证据，SHADOW/合成/回放不计REAL。
3. 保留全部有效历史训练和高质量策略资产，GC按引用闭包，不为清理删除活跃证据。
4. 落实原子双写→逐行对账→受控typed切换→启动恢复→真实事件撤销和资格过期的完整链；冻结绑定实际model publication receipt/state hash、choice、contract，ABI身份与模型状态不可混用。
5. 实现新风险合同真实预注册/持久化/工厂验证/激活及eligible/ACT/ABSTAIN/MISSED_INPUT/MISSED_DEADLINE分母；未经验证的新合同不授权。


**关门验收**

- 合法资格正例可走真实Decision/Trade/结算路径；过期/撤销/篡改/旧build/旧source反例全拒绝。
- 生产实际执行typed切换与撤销，默认LEGACY_READ不能靠测试单独调用switch算迁移完成；新资格风险合同须接真实注册/校验/激活编排。
- K3单位置、无马丁/追期/加倍、PIT、原收益与主链红线负例持续通过。


### UF18 统一CI、同次运行结果与低消耗施工

原批次：K01基础/K11总验；回开原卡：R31-B00-C01, R31-B12-C02, R31-B12-C04, R31-B12-C05。


**源码文件**

- .github/workflows/r31-gate.yml
- .github/workflows/r31-verification.yml
- .github/workflows/r31-build-r27.yml
- tools/r31/verification_receipt.py
- construction/r31/HOST_SUITE.json


**已定位事实/缺口**

- post-K11 ||true及复用摘要仍可能把同SHA旧绿结果算新运行；只比较source_head不足。
- 新锁/Android34测试已加入部分签名路径，但未统一注册到完整HOST/Android验收集合。


**必须真实改代码**

1. 每次run创建唯一invocationId、清理/归档旧摘要，验证进程exit、source/tree/控制输入、runId、开始时间、实跑任务与完整项集合。
2. 复用原close的编译/测试数/跳过/覆盖/已知失败清偿验证器，post-K11不能仅检查0失败。
3. 新锁测试和真实Android34报告/实时链测试进入统一注册集合；API29/35原矩阵继续；过期重复历史门单独标历史，不拿其错误代替当前结论。
4. 检查工具链一次；缺SDK则使用已能运行的CI，无代理盲重试。先改业务代码、针对性测试，批末一次全量；同源码同失败不重复烧额度。
5. 签名构建工作树必须干净，测试输出放临时目录；最后用真实编译classes重冻调用索引，再正式构建。


**关门验收**

- 同SHA上次绿、本次前置失败或无JUnit/跳过/漏suite时，必须红且不能消费旧摘要。
- 原整文件保护和窄schema块复核负例保持；不能批量改期望hash掩盖业务变化。
- 每份APK签名与源码身份和同次CI记录互证，旧APK改名/重压缩不算新交付。


### UF19 旧库、后台、故障与真机时长分层

原批次：K11；回开原卡：R31-B04-C02, R31-B05-C09, R31-B12-C01, R31-B12-C02, R31-B12-C03。


**源码文件**

- tools/r31/collect_device_receipts.py
- tools/r31/accept_device_receipts.py
- tools/r31/run_r31q_android_delivery.sh


**已定位事实/缺口**

- 用户670MB旧库未取得，快检只是一瞬间；当前测试允许during=0且没有真正进程杀重启，不能证明旧库恢复。


**必须真实改代码**

1. 代表性有数据旧库、真实迁移并发业务、报告生成/取消/分享/保存同时测试，原数据逐行对照。
2. 杀主/诊断进程、磁盘满、错序开奖、超时、缓存坏、时钟回拨、系统回收逐点注入；保留所有失败，不让前置失败遮住后续场景。
3. 正常系统条件下前后台/熄屏实测；force-stop未知区间如实记，不保证平台不允许的自动复活。
4. DEVICE24项和24/72小时无设备则NOT_RUN并给明确采集协议，继续源码工作，不能借缺设备停工或伪记PASS。
5. 旧库恢复证明必须有真实采集/重建/验证/recordRestoreProofR31调用与回执，不以证明API存在替代旧库数据保全、恢复与备用文件删除验收。


**关门验收**

- API29/34/35各自输出当前源码真实结果；原机验证另列。
- 完整报告导出后能回答真实进展、性能分布、研究状态和剩余失败；无新样本不显示虚假胜率/P95。


### UF20 同签名成品APK与可核对交付

原批次：K11；回开原卡：R31-B12-C05。


**源码文件**

- release/release_manifest.json
- tools/r31/package_release.py
- release/R31_DELIVERY.json


**已定位事实/缺口**

- 当前只有未完成检查点和历史APK声明，没有可以认定本轮全部验收通过的成品。


**必须真实改代码**

1. 完成本轮所有工程闭环后，在单一干净提交冻结身份和实际编译调用清单，再构建release APK。
2. 保持包com.starcore.research.r13、既有d8d8cee7…证书，核对现机版本后递增versionCode，覆盖安装保留数据。
3. 交付release.apk、可直接解压ZIP、完整源码归档或精确仓库SHA、方案、diff、资产、公开签名指纹、SHA256及完整验收结果；不公开密钥或凭证。
4. 提供用户能下载的文件，不只给Actions工件页；同时准确声明设备/自然证据边界。


**关门验收**

- 安装/覆盖/启动/核心闭环/完整报告保存实跑；包、签名、ABI、版本、嵌入source和外部receipt一致。
- 未完成或失败项逐项列出；不能把“方案写完/源码提交/测试helper通过”写成“软件全部完美完成”。


## 9. 最新源码逐卡落实情况与未闭环清单

核对源码：`762de13d1b02e4e670e785d1bb7489cca5a0f9e0`；分支：`construction/r31-final-20261104`。原机APK源码另列，不能把源码原型当已安装修复。

原卡路径均与上述提交字节核对一致。以下58行是覆盖索引；有事实审阅的卡仍需按验收关门，其他卡明确列待深入核实。当前没有新完整关门的卡，不代表源码毫无实现。

历史K11记录622项单测、0失败/错误/跳过，绑定历史源码`63fae8a4c1cb4a38a991293d1b48a97b5e6d8e84`。当前仅保留带文件hash的OS锁HOST五场景PASS；它不证明完整报告、Android、原机或自然质量。

| 原卡 | 批次 | 当前核验状态 | 返修去向 |
|---|---|---|---|
| R31-B00-C01 固定报告对应源码和安装身份，继承1832要求与299历史卡 | K01 | 已审具体接点，须返修/复验 | UF01, UF18 |
| R31-B00-C02 采集完整原因链和实际提交回执，纠正隔离数量口径 | K01 | 已审具体接点，须返修/复验 | UF01 |
| R31-B01-C01 统一瞬时等待、取消、依赖和真实故障分类 | K02 | 已审具体接点，须返修/复验 | 原卡继续核实实施 |
| R31-B01-C02 领取、提交、回执全部使用同一owner/fence并消除旧prepared状态 | K02 | 已审具体接点，须返修/复验 | 原卡继续核实实施 |
| R31-B01-C03 有限隔离、半开探测与任务级恢复 | K03 | 已审具体接点，须返修/复验 | 原卡继续核实实施 |
| R31-B01-C04 可中断计算与关闭流程有界，禁止超时后并发替身 | K03 | 已审具体接点，须返修/复验 | 原卡继续核实实施 |
| R31-B02-C01 写优先级显式化，消除R30操作名漏分 | K03 | 已审具体接点，须返修/复验 | UF02, UF04 |
| R31-B02-C02 数值计算和DDL退出短事务，分片在owner层完成 | K03 | 已审具体接点，须返修/复验 | UF02, UF03 |
| R31-B02-C03 区分实际提交与提交后诊断失败，防重复训练/结算 | K03 | 已审具体接点，须返修/复验 | 原卡继续核实实施 |
| R31-B03-C01 抓取与当期入库优先，历史回补不挡下一期准备 | K04 | 已审具体接点，须返修/复验 | UF04, UF12 |
| R31-B03-C02 冻结只有一个权威提交，快通道不能覆盖已冻结记录 | K04 | 已审具体接点，须返修/复验 | UF12 |
| R31-B03-C03 结算、反馈与训练事件解耦，漏冻结不能让研究饿死 | K04 | 已审具体接点，须返修/复验 | UF12 |
| R31-B04-C01 进程内研究健康与服务心跳分离，恢复精确到任务或池 | K04 | 已审具体接点，须返修/复验 | UF13 |
| R31-B04-C02 后台能力按实际系统限制验收，保留主动会话持续研究 | K04 | 已审具体接点，须返修/复验 | UF19 |
| R31-B05-C01 连续存储普查与准确口径 | K04 | 已审具体接点，须返修/复验 | UF05 |
| R31-B05-C02 所有增长来源写前预算准入 | K04 | 已审具体接点，须返修/复验 | UF05, UF06, UF08 |
| R31-B05-C03 报告当前作业清理闭环与全物理配额 | K04 | 已审具体接点，须返修/复验 | UF09 |
| R31-B05-C04 keep保护机制迁为有界证据生命周期 | K09 | 已审具体接点，须返修/复验 | UF09 |
| R31-B05-C05 有效资产根引用闭包GC | K09 | 已审具体接点，须返修/复验 | UF09 |
| R31-B05-C06 CAS归档STALE_PAGE有界恢复 | K09 | 已审具体接点，须返修/复验 | UF09 |
| R31-B05-C07 事件、revision与高基数诊断表有界化 | K09 | 已审具体接点，须返修/复验 | UF09 |
| R31-B05-C08 业务混用chart cache先迁权威再清理 | K09 | 已审具体接点，须返修/复验 | UF09 |
| R31-B05-C09 WAL与可重用页回收不阻塞实时链 | K09 | 已审具体接点，须返修/复验 | UF04, UF19 |
| R31-B05-C10 旧库重建与备份删除恢复证明 | K09 | 已审具体接点，须返修/复验 | UF09 |
| R31-B05-C11 Java堆/native/PSS与缓存队列有界治理 | K04 | 已审具体接点，须返修/复验 | UF05, UF09 |
| R31-B06-C01 保留历史训练并统一冻结输入、角色和数据修订 | K05 | 已审具体接点，须返修/复验 | UF12 |
| R31-B06-C02 回放增量化与模型、优化器、游标共同提交 | K05 | 已审具体接点，须返修/复验 | UF12 |
| R31-B06-C03 AtomNet 十位置完整训练水位与真实发布消费证明 | K05 | 已审具体接点，须返修/复验 | UF12 |
| R31-B07-C01 保留真实家族与算子并建立执行归属清单 | K06 | 已审具体接点，须返修/复验 | UF14 |
| R31-B07-C02 有限 typed AST、前置解析预算与因果行为去重 | K06 | 已审具体接点，须返修/复验 | UF14 |
| R31-B07-C03 以真实增益和成本驱动的有限创造预算 | K06 | 已审具体接点，须返修/复验 | UF14 |
| R31-B07-C04 父子同机会比较、嵌套时间验证和诚实代际提升 | K06 | 已审具体接点，须返修/复验 | UF14 |
| R31-B07-C05 完整工厂检出力、无信号误报与自然数据证据分开验收 | K06 | 已审具体接点，须返修/复验 | UF14 |
| R31-B07-C06 失败归因、短期状态与多位置结构的实质能力升级 | K06 | 已审具体接点，须返修/复验 | UF13 |
| R31-B08-C01 矩阵缺学生依赖隔离与按 kind 持续推进 | K07 | 已审具体接点，须返修/复验 | UF16 |
| R31-B08-C02 研究矩阵到公式的 typed 来源与消费闭环 | K07 | 已审具体接点，须返修/复验 | UF16 |
| R31-B08-C03 扩展配对子树、家族和融合研究且不混用置信区间 | K07 | 已审具体接点，须返修/复验 | UF16 |
| R31-B08-C04 研究产物与消费者接成可验证数值合同 | K07 | 已审具体接点，须返修/复验 | UF16 |
| R31-B08-C05 修复T09枚举错配并验收16个逻辑研究表 | K07 | 已审具体接点，须返修/复验 | UF16 |
| R31-B09-C01 以稳定ID统一47个实际面板合同 | K09 | 已审具体接点，须返修/复验 | UF15 |
| R31-B09-C02 隔离慢图读取与生命周期取消 | K09 | 已审具体接点，须返修/复验 | UF15 |
| R31-B09-C03 缓存投影限额与业务真值隔离 | K09 | 已审具体接点，须返修/复验 | UF15 |
| R31-B09-C04 前向榜、校准图、资金图统一证据口径 | K09 | 已审具体接点，须返修/复验 | UF11, UF15 |
| R31-B09-C05 47图与16逻辑表设备回归证据 | K09 | 已审具体接点，须返修/复验 | UF15 |
| R31-B10-C01 主链选择从图表缓存迁移到唯一 typed 权威表 | K08 | 已审具体接点，须返修/复验 | UF17 |
| R31-B10-C02 审计资格功效与连续败绩风险并版本化新合同 | K08 | 已审具体接点，须返修/复验 | UF17 |
| R31-B10-C03 合理出手频率、唯一未来证据与撤销闭环 | K08 | 已审具体接点，须返修/复验 | UF17 |
| R31-B11-C01 逐项接齐44指标及设备证据导入 | K10 | 已审具体接点，须返修/复验 | UF10 |
| R31-B11-C02 报告区分已存在、已运行、已消费与有效 | K10 | 已审具体接点，须返修/复验 | UF10 |
| R31-B11-C03 有界报告与可取回证据附件 | K10 | 已审具体接点，须返修/复验 | UF06, UF07, UF08, UF11 |
| R31-B11-C04 建立96检查与44指标的显式交叉账 | K10 | 已审具体接点，须返修/复验 | UF10 |
| R31-B12-C01 覆盖安装与迁移可恢复性联合验收 | K11 | 已审具体接点，须返修/复验 | UF03, UF07, UF09, UF19 |
| R31-B12-C02 统一CI、故障注入与跨批次不可回归测试 | K11 | 已审具体接点，须返修/复验 | UF18, UF19 |
| R31-B12-C03 24h初测、72h全链与长期容量验证 | K11 | 已审具体接点，须返修/复验 | UF19 |
| R31-B12-C04 受控退役与大型文件渐进拆分 | K11 | 已审具体接点，须返修/复验 | UF18 |
| R31-B12-C05 唯一成品发布收据和APK压缩包交付 | K11 | 已审具体接点，须返修/复验 | UF01, UF18, UF20 |
| R31-ENG-01 修复启动Certificate关键域指纹失配（随B02-C02同批） | K03 | 已审具体接点，须返修/复验 | 原卡继续核实实施 |
| R31-ENG-02 内存准入在制品与既有研究测试相容（不加堆、不改阈值） | K02 | 已审具体接点，须返修/复验 | 原卡继续核实实施 |

### R31-B00-C01 固定报告对应源码和安装身份，继承1832要求与299历史卡

历史声明：307615ed6181e4f1234bae27064beeb02089f553；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/build.gradle`, `tools/r31/verify_baseline.py`。

事实记录 1（root_completion_audit_rows.json，保留完整机器证据）：

- 已实现：app/build.gradle从同一release_manifest/identity核对版本、包及schema，生成SOURCE_COMMIT/SOURCE_TREE/DIRTY/PLAN_MANIFEST字段；release拒绝dirty。

- 已实现：verify_baseline.identity使用全git status和产品输入集合；已将123个原卡源码路径与远程762de13d检查点逐字节核对。

- 未闭环/待验证：旧审计器只检查app/src/main的dirty，不能证明构建配置、工具与元数据全部干净；身份工具和生产报告口径需统一。

- 未闭环/待验证：当前检查点仍有未完成接线及故障验收；原机APK源码为5e05e668，与本轮代码检查点分开。

- 源码依据：`app/build.gradle` expectedIdentity / r31BuildIdentity / requestedRelease L21。

- 源码依据：`tools/r31/verify_baseline.py` identity L265。

如何闭环：按本卡事实缺口及UF01,UF18修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B00-C02 采集完整原因链和实际提交回执，纠正隔离数量口径

历史声明：307615ed6181e4f1234bae27064beeb02089f553；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/WaitKindR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/SchemaR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：ResearchPoolR27.record在实际任务循环后记录任务回执；observeAttemptR31用独立TELEMETRY写事务保存attempt/CPU/工作量/receipt失败。

- 已实现：有2000样本/8MiB故障上界、首末cause/stack、非递归失败计数；诊断失败不回滚已发布业务事务。

- 未闭环/待验证：报告最终输出能否逐项消费这些实际事实，需随UF10/UF11修生产快照/文档链并联测；本卡报告fixture/错误注入尚未在本检查点重跑。

- 未闭环/待验证：ResearchPoolR27.recoverOneR31:359捕获RuntimeException后静默返回，没有该次恢复控制失败的独立cause/次数；任务旧行能表达隔离但不能解释本次恢复为何失败，应补有界非递归控制失败事实。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java` l, o, o, p, :, 2, 1, 0, ,,  , r, e, c, o, r, d, :, 2, 5, 4, -, 2, 7, 4, ,,  , o, b, s, e, r, v, e, A, t, t, e, m, p, t, R, 3, 1, :, 4, 4, 7, -, 4, 7, 6, ,,  , p, e, r, s, i, s, t, F, a, u, l, t, R, 3, 1, :, 4, 8, 6, 起 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeFaultEvidenceR31.java` c, a, p, t, u, r, e, /, c, l, e, a, n 。

如何闭环：按本卡事实缺口及UF01修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B01-C01 统一瞬时等待、取消、依赖和真实故障分类

历史声明：187194e7d31baf78178d6428e570397b84d01d75；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/WaitKindR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java`, `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDispositionR31.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：WaitKindR27.classify与ResearchPoolR27.loop/record已生产接FailureDispositionR31；仅FAULT消耗fault预算，busy退避、取消/stale/fatal有独立处理。

- 已实现：分类器有16层identity防环cause walk；磁盘满/损坏与提交不确定/已提交诊断失败分开。

- 未闭环/待验证：当前检查点的锁3秒/磁盘满/取消/四入口同根异常全套仍须重跑；不能凭分类器存在认为设备锁争用已解决。

- 未闭环/待验证：迁移与连接池资源拥堵是上游实际阻塞，必须修UF02-04，而非把一切异常改成RETRY并隐藏真实故障。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDispositionR31.java` c, l, a, s, s, i, f, y, :, 3, 1, 起, ,,  , r, o, o, t, C, a, t, e, g, o, r, y, R, 3, 1, :, 6, 2, 起, ,,  , c, a, t, e, g, o, r, y, :, 1, 3, 0, 起 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/WaitKindR27.java` c, l, a, s, s, i, f, y, :, 2, 8, 起 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java` l, o, o, p, :, 2, 0, 6, -, 2, 1, 8, ,,  , r, e, c, o, r, d, :, 2, 6, 2, -, 2, 7, 3 。

如何闭环：按本卡事实缺口及原卡要求修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B01-C02 领取、提交、回执全部使用同一owner/fence并消除旧prepared状态

历史声明：187194e7d31baf78178d6428e570397b84d01d75；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/PreparedTaskR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：claimWithinR31领取采用task+旧owner+fence+状态CAS；requireFence联查session、boot和elapsed租约；任务成功/失败回执也按owner/fence且检查affected_rows=1。

- 已实现：失效prepared发取消并等待实际termination；有效busy保留prepared，ClaimAttempt区分等待与领取。

- 未闭环/待验证：本次未重跑旧owner晚到回执、claim/提交/回执三处真kill及时间跳变；应绑定当前源码执行原反例，不能沿用历史收据当当前通过。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java` r, e, q, u, i, r, e, F, e, n, c, e, :, 3, 3, 5, ,,  , C, L, A, I, M, _, C, A, S, _, R, 3, 1, /, c, l, a, i, m, W, i, t, h, i, n, R, 3, 1, :, 3, 3, 7, -, 3, 6, 4, ,,  , r, e, l, e, a, s, e, L, e, a, s, e, R, 3, 1, :, 3, 6, 6 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java` c, l, a, i, m, P, r, e, p, a, r, e, d, :, 2, 2, 8, -, 2, 3, 4, ,,  , T, A, S, K, _, R, E, C, E, I, P, T, _, C, A, S, _, R, 3, 1, :, 2, 5, 3, ,,  , r, e, c, o, r, d, :, 2, 5, 9, -, 2, 7, 3, ,,  , r, e, t, r, y, :, 3, 0, 2, 起 。

如何闭环：按本卡事实缺口及原卡要求修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B01-C03 有限隔离、半开探测与任务级恢复

历史声明：37828f5f6e639e065b63f8c4fba92a579666377e；原验收分层：{"LOCAL": 3, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/WaitKindR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/SchemaR27.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：生产loop和RuntimeCoordinator.run调用recoverOneR31；分级隔离30秒/2分钟/10分钟/30分钟，6轮同根因NEEDS_FIX。

- 已实现：恢复检查boot/elapsed、旧计算仍活则跳过；全局和持久时钟限速每秒1个半开，成功仅在新artifact身份提交后复位。

- 未闭环/待验证：recoverOneR31的控制失败被catch后不保存恢复失败cause/计数；需补有界观测，避免只知道隔离未恢复却不知道恢复控制失败。

- 未闭环/待验证：13任务混合故障、半开唯一性、当前24小时无无声停摆尚未执行；依赖恢复与真实新artifact必须联测。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java` l, o, o, p, :, 1, 7, 4, ,,  , r, e, c, o, r, d, F, a, u, l, t, B, u, d, g, e, t, R, 3, 1, :, 2, 7, 6, -, 2, 9, 4, ,,  , r, e, c, o, r, d, D, u, r, a, b, l, e, R, e, c, o, v, e, r, y, R, 3, 1, :, 2, 9, 5, -, 3, 0, 0, ,,  , r, e, c, o, v, e, r, O, n, e, R, 3, 1, :, 3, 3, 9, -, 3, 5, 9 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java` r, u, n, :, 4, 8, 0, ,,  , c, l, a, i, m, W, i, t, h, i, n, R, 3, 1, :, 3, 5, 9, 将, h, a, l, f, _, o, p, e, n, _, f, e, n, c, e, 绑, 定, 真, 实, 领, 取 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDispositionR31.java` q, u, a, r, a, n, t, i, n, e, M, i, l, l, i, s 。

如何闭环：按本卡事实缺口及原卡要求修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B01-C04 可中断计算与关闭流程有界，禁止超时后并发替身

历史声明：37828f5f6e639e065b63f8c4fba92a579666377e；原验收分层：{"LOCAL": 4, "EMU": 1, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/CooperativeContinuationR26.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/PreparedTaskR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/TaskContinuationR26.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/IncumbentLoopSU2.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/XiIncumbentSU2.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：PreparedTask持有CooperativeContinuation，PAUSED/RUNNING/CANCELLING/TERMINATED与awaitTermination存在；超时先close，资源permit保留到Thread.isAlive=false。

- 已实现：pool.close异步取消、10秒控制等待和后续reaper；tryFinishResources仅在worker/helper/continuation都真正退出后关闭只读连接。

- 未闭环/待验证：每条实际长计算/编码/模型循环的poll覆盖、忽略中断任务不产生替身和线程/连接峰值，尚未在本次完整压力场景复验；需要保留DRAINING而不是超时后假TERMINATED。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/PreparedTaskR30R1.java` s, l, i, c, e, :, 2, 0, 起, ,,  , i, s, T, e, r, m, i, n, a, t, e, d, /, a, w, a, i, t, T, e, r, m, i, n, a, t, i, o, n, :, 3, 1, -, 3, 9, ,,  , c, l, o, s, e, :, 4, 1 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/CooperativeContinuationR26.java` s, l, i, c, e, :, 1, 8, 5, -, 2, 0, 2, ,,  , r, u, n, :, 2, 0, 4, -, 2, 2, 0, ,,  , c, h, e, c, k, p, o, i, n, t, /, p, o, l, l, :, 2, 3, 0, -, 2, 4, 2, ,,  , c, l, o, s, e, :, 2, 7, 2 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java` c, l, o, s, e, :, 3, 8, 3, -, 3, 9, 4, ,,  , D, R, A, I, N, E, R, :, 3, 9, 6, -, 4, 0, 0, ,,  , t, r, y, F, i, n, i, s, h, R, e, s, o, u, r, c, e, s, R, 3, 1, :, 4, 0, 1, -, 4, 0, 9 。

如何闭环：按本卡事实缺口及原卡要求修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B02-C01 写优先级显式化，消除R30操作名漏分

历史声明：37828f5f6e639e065b63f8c4fba92a579666377e；原验收分层：{"LOCAL": 3, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已有WriteClass/WriteContext和显式operation注册表；R30_PREPARED_TASK_CLAIM归RESEARCH_CONTROL，历史批次归RESEARCH_DATA，Legacy迁移归MIGRATION。

- 已实现：runWrite对后台队外让关键写，并提供每5秒研究服务机会；实际持锁时间交WriterHoldLedger/WriterHoldFaults记录。

- 未闭环/待验证：250ms/2000ms入锁期限不约束task.call体；当前迁移单元仍可连续执行大量ALTER/CREATE，不能从deadline推导持锁满足P99≤50ms/硬目标≤200ms。

- 未闭环/待验证：未重跑当前源码60分钟全负载竞争、每operation分布、1000次真实机会；历史收据不能自动继承。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java` WriteClass / WriteContext / operationRegistryR31 L27。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java` runWrite / recordUnlockedWriterR30 L830。

如何闭环：按本卡事实缺口及UF02,UF04修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B02-C02 数值计算和DDL退出短事务，分片在owner层完成

历史声明：37828f5f6e639e065b63f8c4fba92a579666377e；原验收分层：{"LOCAL": 2, "EMU": 1, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`, `app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java`, `app/src/main/java/com/ethk3/novaonepro/DbStore.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：ResearchWriteQueueR27.ImmutableBatch提交拒绝DDL，提交使用同一DatabaseCoordinator及持久幂等回执。

- 已实现：retentionStepR30已移除每phase的ensureProtected，改为只读就绪/缺schema依赖检查。

- 已实现：已有SlicedInitializerR31、R31Q21_RUNTIME_/MAIN_逐单元日志及原后台队列续跑，专用DDL连接贯穿迁移、journal和校验。

- 未闭环/待验证：saveDecisionInternal:3713、saveTeacherMatrixSample:7286、saveLearningCase:8983等业务方法仍可在传入业务连接上推进DDL，绕过专用迁移连接；尚未满足运行期零DDL/唯一迁移入口。

- 未闭环/待验证：RUNTIME_COLUMNS内又调用FIELD_COLUMNS；DYNAMIC_WINDOW遍历多表/索引；MAIN_BASE调用整个createBaseTables；单元数有限不等于单次持锁时长有界。

- 未闭环/待验证：未取得当前改动下Android真实SQLite的WAL、长reader、旧库迁移并发时延收据。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java` submit(ImmutableBatch) L154。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java` retentionStepR30 L631。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DbStore.java` advanceLegacyRuntimeSchemaR31 / ensureRuntimeExtraTablesOnce L334。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java` dedicatedDdlTaskR31Q / submitBackgroundWrite L1119。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DbStore.java` saveDecisionInternal / saveTeacherMatrixSample / saveLearningCase L3698。

如何闭环：按本卡事实缺口及UF02,UF03修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B02-C03 区分实际提交与提交后诊断失败，防重复训练/结算

历史声明：37828f5f6e639e065b63f8c4fba92a579666377e；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/SchemaR27.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：DatabaseCoordinator有ROLLED_BACK/COMMITTED/COMMITTED_DIAGNOSTIC_FAILED/UNKNOWN及诊断失败隔离。

- 已实现：runIdempotentAtomicWriteR31在业务事务内插入commit_key+payload_hash；UNKNOWN使用独立只读连接查receipt，payload冲突拒绝。

- 已实现：ImmutableBatch提交已接入上述幂等入口。

- 未闭环/待验证：本次仅核查通用实现和ImmutableBatch接线，未逐个证明所有非幂等生产调用都携带稳定输入/输出identity；不能以框架存在宣布全调用闭环。

- 未闭环/待验证：未执行当前源码commit完成返回前kill、afterCommit/timing失败、模型参数/样本计数/结算计数仅更新一次的集成测试。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java` CommitOutcome / diagnosticR31 L29。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java` runIdempotentAtomicWriteR31 / resolveCommitReceiptR31 L1034。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java` submit(ImmutableBatch) L162。

如何闭环：按本卡事实缺口及原卡要求修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B03-C01 抓取与当期入库优先，历史回补不挡下一期准备

历史声明：958fbb7172ffe100971eef352df627dc5536cc6f；原验收分层：{"LOCAL": 3, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`, `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`, `app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：runFetchChannelOnce保留FETCH_R30单飞；最新draw校验后saveApiResultDraws的durable回调优先分发当前期，历史后置。

- 已实现：后台控制节拍申请Legacy迁移；显式写优先级将当前入库、冻结和结算与历史批次分级。

- 未闭环/待验证：迁移回调仍可长时间持有唯一写者；独立DDL连接不能排除应用写锁、SQLite文件锁及I/O争用，当前改动尚不足证明下一期准备及时。

- 未闭环/待验证：未执行当前源码1000次分母完整测量、乱序/重复/晚到/断网恢复/HTTP卡住矩阵；手机旧版本快检不等于新版结果。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java` runFetchChannelOnce L2725。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java` saveApiResultDraws durable callback L2844。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java` reconcileResearchOwner L2429。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java` operationRegistryR31 / runWrite L60。

如何闭环：按本卡事实缺口及UF04,UF12修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B03-C02 冻结只有一个权威提交，快通道不能覆盖已冻结记录

历史声明：958fbb7172ffe100971eef352df627dc5536cc6f；原验收分层：{"LOCAL": 4, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java`, `app/src/main/java/com/ethk3/novaonepro/AnalysisRequestStoreR36.java`, `app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java`, `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`, `app/src/main/java/com/ethk3/novaonepro/OosAfterDrawWriterR36.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：AnalysisSingleFlight.submit严格active=1，超时只请求协作取消；reapDeadWorker要求worker/handler已确证死亡。

- 已实现：CanonicalObservationWriterR36在事务内先查target hash；精确重放幂等，不一致R36_CANONICAL_INTEGRITY_CONFLICT；冻结事实含commit/deadline及history/snapshot hash。

- 未闭环/待验证：源码关键防线存在；未执行当前源码fast/full竞争、边界±1ms、时钟跳、model更新、活owner超时的跨Analysis/DecisionGate/Settlement联测。

- 未闭环/待验证：原手机WAITING_CANONICAL及旧分析时间戳只证明快检时无新canonical凭据，不能据此推断缺少哪些策略资格或伪造补预测。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java` submit / cancelIfNoProgress / reapDeadWorker L41。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java` insertWithinTransaction L11。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/AnalysisRequestStoreR36.java` fixedDeadlineF01 L482。

如何闭环：按本卡事实缺口及UF12修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B03-C03 结算、反馈与训练事件解耦，漏冻结不能让研究饿死

历史声明：958fbb7172ffe100971eef352df627dc5536cc6f；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java`, `app/src/main/java/com/ethk3/novaonepro/OosAfterDrawWriterR36.java`, `app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeSchemaR26.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：SettlementTaskStoreR36为已确认draw调用兼容feedback入库；无赛前canonical有明确排除终态。

- 已实现：RuntimeSchemaR26分别生产DRAW_LABEL_REVEALED和经过原始canonical/赛前时间校验的FROZEN_RESULT；反馈receipt有consumer_version/event_id维度。

- 未闭环/待验证：本次确认事件区分与关键接点，未逐消费者追完参数/checkpoint/receipt原子提交及全部五类独立水位报告。

- 未闭环/待验证：未跑删某期冻结后draw训练继续、重复事件100次、消费者kill恢复、研究失败不回滚核心结算的当前源码联测。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java` enqueueConfirmedWithinWrite / markTerminalWithinTransaction L126。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeSchemaR26.java` feedbackWithinTransaction / typedDrawEventsR31 / typedFrozenEventsR31 L134。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeSchemaR26.java` consumeFrozenEffectsR31 L192。

如何闭环：按本卡事实缺口及UF12修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B04-C01 进程内研究健康与服务心跳分离，恢复精确到任务或池

历史声明：958fbb7172ffe100971eef352df627dc5536cc6f；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`, `app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`, `app/src/main/java/com/ethk3/novaonepro/BootReceiver.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：RuntimeCoordinator.captureHealthSnapshotR31用有界128任务真实typed进度/提交水位/产物身份判定研究健康，attempt和owner心跳不当进展。

- 已实现：AutoFetchService.reconcileResearchOwner真实调用owner.reconcileResearchHealthR31，ReportSnapshot亦纳入activeHealthObservation；有异步有界health executor和stale/UNKNOWN表示。

- 未闭环/待验证：health只读查询本身仍需SQLite连接，用户栈显示controller等待池；调度/查询有界不等于底层连接获取有硬上限。先修迁移/连接争用并做持续无artifact的真实状态与恢复联测。

- 未闭环/待验证：最终报告44指标/新对象状态生产接线仍未闭环，不能用此已接健康字段替代全报告能力验收。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java` H, E, A, L, T, H, _, Q, U, E, R, Y, _, R, 3, 1, :, 2, 0, 0, ,,  , c, a, p, t, u, r, e, H, e, a, l, t, h, S, n, a, p, s, h, o, t, R, 3, 1, :, 2, 0, 3, -, 2, 5, 3, ,,  , h, e, a, l, t, h, S, n, a, p, s, h, o, t, R, 3, 1, :, 2, 7, 2, -, 2, 8, 4, ,,  , r, e, c, o, n, c, i, l, e, R, e, s, e, a, r, c, h, H, e, a, l, t, h, R, 3, 1, :, 2, 8, 5, -, 2, 9, 5 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java` r, e, c, o, n, c, i, l, e, R, e, s, e, a, r, c, h, O, w, n, e, r, :, 2, 4, 3, 0, -, 2, 4, 4, 0 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java` c, a, p, t, u, r, e, :, 9, 2 。

如何闭环：按本卡事实缺口及UF13修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B04-C02 后台能力按实际系统限制验收，保留主动会话持续研究

历史声明：958fbb7172ffe100971eef352df627dc5536cc6f；原验收分层：{"DEVICE": 3, "LOCAL": 2}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/AndroidManifest.xml`, `app/src/main/java/com/ethk3/novaonepro/BootReceiver.java`, `app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimePowerLeaseR26.java`, `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：RuntimePowerLeaseR26按明确连续会话持有最长60秒wake lease、15秒续租，close幂等释放，renew与数值进展分开。

- 已实现：AutoFetchService.onDestroy区分userRequestedStop；onTaskRemoved将恢复/抓取/分析重整投递工作线程，正常滑掉与用户stop行为不同。

- 未闭环/待验证：尚无当前V2353A亮/熄屏、省电、Doze、断网重连、普通kill、force-stop/重启72小时原件；源码有重启安排不能证明OEM允许执行。

- 未闭环/待验证：source中已有restart的吞异常分支仍需退出事实、限制原因与恢复结果验证；force-stop停止属平台边界，不能承诺自动复活或补造旧预测。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimePowerLeaseR26.java` k, e, e, p, A, l, i, v, e, :, 2, 5, ,,  , c, l, o, s, e, :, 3, 1 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java` o, n, D, e, s, t, r, o, y, :, 1, 8, 7, 4, -, 1, 9, 0, 8, ,,  , o, n, T, a, s, k, R, e, m, o, v, e, d, :, 1, 9, 3, 7, -, 1, 9, 5, 7, ,,  , u, s, e, r,  , s, t, o, p,  , f, l, a, g, s, :, 1, 7, 3, 5, /, 1, 8, 0, 5 。

如何闭环：按本卡事实缺口及UF19修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B05-C01 连续存储普查与准确口径

历史声明：958fbb7172ffe100971eef352df627dc5536cc6f；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/StorageCensusSU2.java`, `app/src/main/java/com/ethk3/novaonepro/StorageLedgerR31.java`。

事实记录 1（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已有固定类别目录普查，常规文件长度与分配块独立记录；有288个五分钟点与720小时槽。

- 已实现：报告准入和owner释放后均调用强制采样，采样回执标注非原子观测及磁盘并非内存。

- 未闭环/待验证：StorageBudget.snapshot仍在全局准入锁内做目录普查；StorageLedger.sample先普查，再调用snapshot重复普查。五秒仅约束获取锁，未限制持锁测量耗时。

- 未闭环/待验证：protected/reclaimable仍明确为null；此处应保持UNKNOWN，不能据历史关门宣称所有owner长期增长和回收债务已现场验证。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageLedgerR31.java` POINTS/HOURS/INTERVAL_MS L11。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageLedgerR31.java` sample L17。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageLedgerR31.java` Point.map L54。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageBudgetR31.java` snapshot L179。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java` drain finally L389。

如何闭环：按本卡事实缺口及UF05修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B05-C02 所有增长来源写前预算准入

历史声明：958fbb7172ffe100971eef352df627dc5536cc6f；原验收分层：{"LOCAL": 3, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionR30.java`, `app/src/main/java/com/ethk3/novaonepro/StorageBudgetR31.java`。

事实记录 1（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已有跨PID OS锁、持久ledger、活跃owner lifetime lease、scope排他、写前beforeChunk与实际停止后finish/recovery。

- 已实现：新增单一monotonic五秒获取锁期限与中断保留，没有无锁写入。

- 未闭环/待验证：锁内reserve/recover/ensureAdditional/snapshot执行目录普查或scope遍历，获取等待有界不等于临界区耗时有界。

- 未闭环/待验证：REPORT请求可选观察文件与失败ACK仍依赖同一bulk admission，争用超过期限时请求和失败回执同时失败。

- 未闭环/待验证：全部增长源接入属于卡要求；此次仅审报告路径，未验证所有DB/CAS/checkpoint/cache/log生产写路径覆盖，不得记全面完成。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageBudgetR31.java` locked L66。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageBudgetR31.java` reserve L94。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageBudgetR31.java` Reservation.beforeChunk L124。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportIoR31.java` beforeAtomic L66。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java` requestFailure L164。

如何闭环：按本卡事实缺口及UF05,UF06,UF08修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B05-C03 报告当前作业清理闭环与全物理配额

历史声明：958fbb7172ffe100971eef352df627dc5536cc6f；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`, `app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java`, `app/src/main/java/com/ethk3/novaonepro/ReportBudgetSU2.java`。

事实记录 1（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已有发布后独立cleanup记录、有界每轮处理数量、指数退避、公开ZIP覆盖验证及读者锁保护；全物理量与delivery量分开。

- 未闭环/待验证：pruneRequestMetadata先删request和ACK，随后才尝试删除读者保护的recovery/saf-parts；若被读者阻挡，后续retirableRecovery又依赖已删ACK，残留失去可发现的退休依据。

- 未闭环/待验证：latestFullReport遇任一无效job.json直接中止；create先建目录再写journal，崩溃孤儿可遮住有效报告；abandoned始终false无法自动消除此障碍。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportCleanupR31.java` runDue L30。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java` pruneRequestMetadata L245。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportBudgetSU2.java` retirableRecovery L73。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java` latestFullReport L114。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java` create L128。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportBudgetSU2.java` abandoned L67。

如何闭环：按本卡事实缺口及UF09修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B05-C04 keep保护机制迁为有界证据生命周期

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionR30.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：ResearchRetentionStoreR30.ensureProtected已通过SchemaDomainCoordinator迁移owner；保留archive定义、guard定义验证和启动checkpoint保护。

- 已实现：retentionStepR30日常phase只检查保护schema就绪，缺失返回SCHEMA_PENDING。

- 未闭环/待验证：此审计只覆盖DDL/迁移接点；未逐表证明原件hash、consumer迁移、引用登记及旧keep有界缩减；不能把ensure入口存在算整张生命周期卡完成。

- 未闭环/待验证：未执行本次版本ST-08各阶段kill与真实旧库数据保全。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java` ensureProtected / ensureProtectedObjectsR31 / preserveStartupCheckpoint L114。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java` startupUpgradeSpecR31 / retentionStepR30 L369。

事实记录 3（lifecycle_completion_audit_rows.json，保留完整机器证据）：

- 已实现：ResearchRetentionR30已有逐表TablePolicyR31及默认拒删；KeepMigrationR31实现VERIFY_HASH→REGISTER_REFERENCE→MIGRATE→SWITCH状态机接口。

- 已实现：ResearchRetentionStoreR30已迁至SchemaDomainCoordinator安装并精确校验旧keep/guard定义，保留原件保护。

- 未闭环/待验证：KeepMigrationR31在生产源码仅有类声明及注释引用，未见SQLite Store实现或生产step调用；实际guardsR31仍把删除/清payload复制到immutable keep表，尚未接有界迁移退出路径。

- 未闭环/待验证：tableDigest、rowIds/maxId每步取全列表，现有抽象不能仅凭PAGE_ROWS=64宣称耗时有界。

- 未闭环/待验证：逐表producer/consumer/key仍为前缀推导及DECLARED_PRIMARY_KEY文案，需对真实读者、主键、bytes/ref/expiry落实适配与持久回执。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/KeepMigrationR31.java` Store, tableDigest, step, dropDecision L15,21,30,42。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java` ensureProtectedObjectsR31, guardsR31, step L25,117,214。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionR30.java` TablePolicyR31, POLICIES_R31, deletableByAgeR31 L20,30,33。

如何闭环：按本卡事实缺口及UF09修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B05-C05 有效资产根引用闭包GC

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/EvidenceReachabilityR31.java`, `app/src/main/java/com/ethk3/novaonepro/StorageGcR31.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`。

事实记录 2（lifecycle_completion_audit_rows.json，保留完整机器证据）：

- 已实现：EvidenceReachabilityR31具有RootKind、mark/unreachable/protectedBudget；StorageGcR31有claim、unlink、recover及LIVE/DELETING/DELETED协议。

- 已实现：原ResearchRetentionStoreR30.cleanFiles仍给允许删除的cache/retired duplicate写前后回执。

- 未闭环/待验证：全生产源码搜索EvidenceReachabilityR31/StorageGcR31仅定义及二者内部引用，未见真实数据库Graph/Store适配、调度或根/边/pin写入接线。

- 未闭环/待验证：因此算法级GC存在不等于实际资产引用闭包GC完成；需把当前/回滚模型、待结算冻结、任务、验证、分享与迁移根接到同一writer并实测pin竞态/杀进程恢复。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/EvidenceReachabilityR31.java` Graph, mark, unreachable, protectedBudget L16,29,38,47。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageGcR31.java` Store, claim, unlink, recover L17,29,36,44。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java` cleanFiles, step L193,214。

如何闭环：按本卡事实缺口及UF09修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B05-C06 CAS归档STALE_PAGE有界恢复

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/DataRetentionGovernorR900.java`, `app/src/main/java/com/ethk3/novaonepro/RetentionArchiveIoCP12.java`。

事实记录 2（lifecycle_completion_audit_rows.json，保留完整机器证据）：

- 已实现：DataRetentionGovernorR900.trimOwnedCP12实际调用StalePageR31.onStale/onSuccess，prepareArchivePageCP12使用per-table cap/cooldown；保留严格generation/schema/hash与INFLIGHT协议。

- 已实现：publishAndPruneCP12在writer里复核row generation，持久member/prune cursor，压力/时间到让出。

- 未闭环/待验证：这部分可确认有生产接线，不能判定完全未实施；本轮未执行同页100次变化/稳定后prune/进程重启及临时bytes上界。

- 未闭环/待验证：归档IO仍走ReportExportServiceCP4，报告请求/ACK/发布恢复缺口可能阻断实际归档；需和UF06—09联合故障验证，不放松原permit证明。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DataRetentionGovernorR900.java` trimOwnedCP12, prepareArchivePageCP12, publishAndPruneCP12, retryFailedRequestWithinWriteCP12, StalePageR31 L54,82,127,146。

如何闭环：按本卡事实缺口及UF09修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B05-C07 事件、revision与高基数诊断表有界化

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 3, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/RuntimeEventLedgerR926.java`, `app/src/main/java/com/ethk3/novaonepro/DataRetentionGovernorR900.java`, `app/src/main/java/com/ethk3/novaonepro/RetentionTableRegistryCP12.java`。

事实记录 2（lifecycle_completion_audit_rows.json，保留完整机器证据）：

- 已实现：DataRetentionGovernorR900有真实sweepOrphanRevisionIndexCP12持久pass/generation游标、64行页/2048预算及live row复核。

- 已实现：RuntimeEventLedgerR926增加EventBoundR31内存聚合器，限定诊断行/bytes并保留business事件。

- 未闭环/待验证：EventBoundR31在生产全app搜索仅定义，真实appendWithinTransaction未调用它；源头诊断聚合未证实生效，内存辅助器也不是持久业务事件生命周期。

- 未闭环/待验证：本轮未复核所有RetentionTableRegistryCP12逐表consumer/physical bytes/归档政策及24h总上界；需补真实ledger接线及报告度量，不能只报删行数。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DataRetentionGovernorR900.java` sweepOrphanRevisionIndexCP12, revisionSequenceR81 L348,359,375,388。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/RuntimeEventLedgerR926.java` appendWithinTransaction, EventBoundR31.diagnostic L287,306,313。

如何闭环：按本卡事实缺口及UF09修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B05-C08 业务混用chart cache先迁权威再清理

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineBridgeSU2.java`。

事实记录 2（lifecycle_completion_audit_rows.json，保留完整机器证据）：

- 已实现：ChartCacheR27.classR31/displayPanelsR31将DISPLAY与BUSINESS_STATE/CHECKPOINT/TIMELINE分开，未知默认BUSINESS_STATE。

- 已实现：ResearchRetentionStoreR30.visualCacheDeleteSql实际使用displayPanelsR31白名单，不把mainline/choice纳入通用显示缓存删除。

- 已实现：MainlineBridgeSU2读授权已委托MainlineChoiceStoreR31.authorizedChoice。

- 未闭环/待验证：生产搜索未见switchToTypedRead调用，默认模式仍LEGACY_READ；不能宣称已完成typed权威切换后可清旧choice。

- 未闭环/待验证：白名单保护部分真实接线已存在，仍须补全逐panel全部动态/兼容消费者与每panel key/bytes/TTL、同PIT输入清理前后决策及重启证据。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java` classR31, displayPanelsR31 L20,22,30。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java` visualCacheDeleteSql, step L209,211,220。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java` authorizedChoice, switchToTypedRead L113,125。

如何闭环：按本卡事实缺口及UF09修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B05-C09 WAL与可重用页回收不阻塞实时链

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 3, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/StorageMaintenanceServiceR82.java`, `app/src/main/java/com/ethk3/novaonepro/StorageCheckpointR31.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：StorageCheckpointR31.round实现实时窗口避让、PASSIVE、BUSY指数退避、owner取消reader及条件TRUNCATE；状态输出区分BUSY与corruption。

- 已实现：实际源树另有DatabaseCoordinator、StorageReclaimR943、StorageHealthR811、DatabaseCompactionSU2、SchemaUpgradeCoordinatorR815的直接checkpoint入口。

- 未闭环/待验证：全app/src/main/java rg检索StorageCheckpointR31仅命中自身声明，未见生产构造/调用；策略类存在不能证明实际入口遵守该策略。

- 未闭环/待验证：未逐入口审清是否只在受控离线维护中使用，未跑长reader/实时冻结窗口/旧库WAL恢复的当前版本Android测试。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageCheckpointR31.java` round / json L28。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java` maintenance checkpoint L382。

如何闭环：按本卡事实缺口及UF04,UF19修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B05-C10 旧库重建与备份删除恢复证明

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/StorageMaintenanceFileR82.java`, `app/src/main/java/com/ethk3/novaonepro/SourceReadLeaseR82.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：StorageMaintenanceFileR82.rebuildAndSwap保留原库、检查空间及native proof，journal/rename采用fsync和摘要。

- 已实现：启动recoverIfNeeded持独占SourceReadLease恢复；deleteVerifiedBackup要求绑定active SHA的多项restore proof，拒绝仅quick_check/mtime/size。

- 未闭环/待验证：本次未审nativeRebuild内部rowid/模型/引用闭包等价验证，也未证明restore proof真实生产链完整；字符串字段检查本身不是恢复成功证据。

- 未闭环/待验证：无用户670MB真实旧库，未做每rename/journal点kill、实际新库重启模型加载和待结算核验。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageMaintenanceFileR82.java` recoverIfNeeded / rebuildAndSwap L13。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageMaintenanceFileR82.java` restoreProofRefusalR31 / deleteVerifiedBackup L114。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java` constructor L419。

事实记录 3（lifecycle_completion_audit_rows.json，保留完整机器证据）：

- 已实现：StorageMaintenanceFileR82有原库/候选/备份hash、swap journal、fsync与recover，deleteVerifiedBackup已新增RESTORE_PROOF_KEYS_R31并拒绝仅quick_check/mtime/size。

- 已实现：recordRestoreProofR31可把新库恢复proof写回备份receipt并绑定当前active hash。

- 未闭环/待验证：recordRestoreProofR31生产源码只有定义，未见实际模型加载/rowid/BLOB/待结算/引用闭包/启动恢复检查采集与调用链；没有proof时备份正确保留，但闭环无法自行完成。

- 未闭环/待验证：proof部分字段只检查非空或PASS前缀，不等于实际执行过检查；需由受信同源码验证器产出原件、逐行对照及真实重启证明。

- 未闭环/待验证：本轮未取得原机670MB库，未执行每个rename/journal点真实中断恢复。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/StorageMaintenanceFileR82.java` recover, rebuildAndSwap, restoreProofRefusalR31, recordRestoreProofR31, deleteVerifiedBackup L78,92,114,116。

如何闭环：按本卡事实缺口及UF09修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B05-C11 Java堆/native/PSS与缓存队列有界治理

历史声明：958fbb7172ffe100971eef352df627dc5536cc6f；原验收分层：{"LOCAL": 5, "DEVICE": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java`, `app/src/main/java/com/ethk3/novaonepro/ChartsActivityR83.java`, `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ExecutionBudgetR26.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/BoundedResearchExecutorR31.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/SharedFeatureStoreR31.java`。

事实记录 1（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：报告REQUESTS队列为单worker+2排队；可选observer最多2个实际worker且无排队，超时后若未退出仍占slot。

- 已实现：快速诊断reader池最多3个worker；本地失败fallback最多16个4096字符原因。

- 未闭环/待验证：当前Android observer测试仅在容量饱和时发12个额外调用，finally随即释放阻塞任务，未覆盖超时后忽略interrupt的真实线程。

- 未闭环/待验证：生产report request仍在可选采样之后才durable enqueue，进程死亡或其输出写失败仍可失去请求。

- 未闭环/待验证：此行仅审报告/诊断的资源边界，未重审ResearchPool、写队列、图表、模型全部内存卡范围。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java` REQUESTS L24。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java` OBSERVERS_R31Q L195。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java` collectObservation L212。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportRequestStatusR31.java` FAILURES L9。

- 源码依据：`app/src/androidTest/java/com/ethk3/novaonepro/R31QDeliveryInstrumentedTest.java` blockedOptionalObserversStayBoundedAndDoNotBlockTheNextRequest L71。

如何闭环：按本卡事实缺口及UF05,UF09修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B06-C01 保留历史训练并统一冻结输入、角色和数据修订

历史声明：3a66c047bbbee3fa4a6e962db0c7dede0dc9acd1；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayDatasetR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationStoreR30.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：RuntimeR27.DatasetTask真实加载5000期数据，source revision双检，注册pin/事务提交后再发布cache；恢复先取持久pin。

- 已实现：EvolutionTask按边界切换合法输入，FrozenConfirmationStore的独立确认注册/消费路径仍存在；历史训练并非被删除。

- 未闭环/待验证：本次未完整执行缺期/修订/补期/跨日期/滚动尾窗及pin预算故障矩阵；需证实当前业务阻塞解除后既有生产任务实际提交，不可用文件存在或旧hash替代。

- 未闭环/待验证：不同历史输入格式的known_at/source revision/role边界需对照原卡全面复验；本轮只读确认主链实现存在，不声称已覆盖所有恢复分支。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java` D, a, t, a, s, e, t, T, a, s, k, ., s, t, e, p, :, 1, 7, 6, -, 2, 2, 6, ,,  , E, v, o, l, u, t, i, o, n, T, a, s, k, :, 2, 4, 9, -, 3, 1, 1, ,,  , f, r, o, z, e, n,  , c, o, n, f, i, r, m, a, t, i, o, n, :, 3, 2, 2, -, 3, 5, 9 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayDatasetR27.java` h, a, s, h, :, 9, 9, 起, ,,  , g, a, p, s, /, s, e, g, m, e, n, t, s, :, 3, 9, -, 4, 5 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java` c, a, p, t, u, r, e, :, 2, 2, ,,  , s, t, e, p, :, 2, 8, 起 。

如何闭环：按本卡事实缺口及UF12修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B06-C02 回放增量化与模型、优化器、游标共同提交

历史声明：3a66c047bbbee3fa4a6e962db0c7dede0dc9acd1；原验收分层：{"LOCAL": 4, "DEVICE": 2}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/LegacyGenerationR26.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelReplaySU2.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelArtifactR29.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayCheckpointR31.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：FoundryCampaign.step确实先decide再settle/update，单样本完整序列化后才推进cursor；事务内以旧cursor/checkpoint hash CAS同时写state、进度、训练/消费回执。

- 已实现：CheckpointArtifactsR26.prepareLease在事务外写校验内容；LegacyGeneration已用ModelPublicationR31.stageAndPublish及ReplayWorkspace；不是每次重启都毫无数值checkpoint。

- 已实现：ReplayCheckpointR31独立类只有自身定义，无生产引用；不能以helper单测关闭原卡，但现有替代链也应按等价完整性核实，不能因此声称全无回放。

- 未闭环/待验证：当前源码尚无此轮真kill/restart与不中断黄金模型的模型/Adam/RNG/游标/act/K3/hash完整对照；原机吞吐/峰值/实时P95门也未测。

- 未闭环/待验证：需明确每条生产实现对原卡所有恢复、输入修订和GC合同的等价覆盖；未被生产调用的ReplayCheckpoint helper不能计作运行完成。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java` s, t, e, p, :, 2, 8, -, 1, 3, 8, （, s, a, m, p, l, e, L, i, m, i, t, 、, l, a, s, t, G, o, o, d, 编, 码, 、, R, 2, 6, _, C, A, N, D, I, D, A, T, E, _, S, T, E, P,  , C, A, S, ） 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/LegacyGenerationR26.java` s, t, e, p, :, 2, 1, 起, ,,  , c, o, m, p, u, t, e, :, 7, 6, 起, ,,  , M, o, d, e, l, P, u, b, l, i, c, a, t, i, o, n, 调, 用, :, 6, 8 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayCheckpointR31.java` p, r, e, p, a, r, e, /, c, o, m, m, i, t, R, e, f, e, r, e, n, c, e, /, r, e, s, t, o, r, e, ；, r, g, 全, 生, 产, 只, 有, 该, 类, 自, 身, 定, 义 。

如何闭环：按本卡事实缺口及UF12修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B06-C03 AtomNet 十位置完整训练水位与真实发布消费证明

历史声明：3a66c047bbbee3fa4a6e962db0c7dede0dc9acd1；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomWorldR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelArtifactR29.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：AtomNetSession.online每次一个position，nextPosition与完整期/部分期独立；consumableR31要求ONLINE且partialIssue为空。

- 已实现：AtomNetRuntime.step复制committed状态后计算，staging/校验/提交publication后才rt.publishAtomStateR31；forecast禁止部分期，Runtime.publish部分期保留上次完整model引用。

- 已实现：ModelSurfaceStoreR30消费rt.atomPublicationR30模型；所以完整十位置消费保护有真实生产接线。

- 未闭环/待验证：当前未执行位置1..9每处真kill、位置10事务失败、非零梯度变化与实际消费者同hash证据联测；完整报告还要展示输入/partial/complete/published/consumed各自水位。

- 未闭环/待验证：publicationReady=ready与consumable分字段已存在，读取端不得继续把ready或训练观察水位解释为完整可消费模型；需查全UI/报告消费者。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java` o, n, l, i, n, e, :, 1, 0, 8, -, 1, 1, 7, ,,  , c, o, n, s, u, m, a, b, l, e, R, 3, 1, :, 1, 2, 3, ,,  , r, e, p, o, r, t, :, 1, 2, 5, -, 1, 4, 7 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java` r, e, s, t, o, r, e, /, s, t, e, p, :, 1, 2, -, 7, 9, ,,  , f, o, r, e, c, a, s, t, :, 1, 0, 6, 起 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java` p, u, b, l, i, s, h, A, t, o, m, S, t, a, t, e, R, 3, 1, :, 4, 3, -, 5, 7 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceStoreR30.java` c, a, p, t, u, r, e, /, b, i, n, d, :, 5, 0, 起 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetStoreR30.java` r, e, s, t, o, r, e, :, 8, 1, 起, ,,  , p, u, b, l, i, c, a, t, i, o, n, M, o, d, e, l, R, 3, 1, :, 8, 9, 起, ,,  , c, h, e, c, k, p, o, i, n, t, R, e, f, e, r, e, n, c, e, R, 3, 1, :, 1, 0, 1, 起 。

如何闭环：按本卡事实缺口及UF12修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B07-C01 保留真实家族与算子并建立执行归属清单

历史声明：f30ad7cc4c82dc22173dd0255afb58f08e61bc56；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelFoundryJobSU2.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelLiveLoopSU2.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertWorkshopR26.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：FamilyRegistryR26默认23可用家族有具体build/kernel dispatch；F23不在默认可用列表，必须绑定真实teacher/ABI/hash后才能加入，无随机教师fallback。

- 已实现：FoundryCampaignR26.step真实遍历EXECUTABLE生成训练种子；ModelReplaySU2.build路由具体模型；RuntimeCoordinator注册F01..F24及E01..E16，专家通过TaskContinuation生产入口执行。

- 已实现：新增acceptR31/capabilitiesR31提供因果predict→update→serialize/restore验收辅助接口，但全生产搜索未发现调用；注册EXECUTABLE与有当前执行证据是不同状态。

- 未闭环/待验证：当前源码无本轮23默认族全量因果执行/恢复/成本原件及F23真实teacher正反例；acceptR31辅助方法不能替代实际训练提交和消费者证据。

- 未闭环/待验证：生产r26_capability登记EXECUTABLE基于实现名非空；最终报告必须结合last_progress/训练产物/消费/依赖事实展示，不能把注册状态当运行或有效策略。

- 未闭环/待验证：尚未逐核所有23kernel数值细节及专家独占owner双入口反例，需按原卡逐族补当前验收，不能为表全绿填虚值。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java` A, V, A, I, L, A, B, L, E, /, E, X, E, C, U, T, A, B, L, E, :, 6, -, 7, ,,  , b, i, n, d, L, e, g, a, c, y, T, e, a, c, h, e, r, :, 1, 0, -, 1, 5, ,,  , b, u, i, l, d, :, 1, 7, -, 3, 7, ,,  , a, c, c, e, p, t, R, 3, 1, :, 6, 4, -, 8, 1, ,,  , c, a, p, a, b, i, l, i, t, i, e, s, R, 3, 1, :, 8, 8 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java` s, t, e, p, :, 4, 1, -, 4, 9, 真, 实, 家, 族, s, e, e, d, s, /, 机, 制, c, h, i, l, d, r, e, n, /, 失, 败, 研, 究, 提, 案 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelReplaySU2.java` b, u, i, l, d, :, 1, 2, 0, -, 1, 4, 4, 具, 体, F, x, x, 分, 派, 及, 参, 数, 合, 同 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java` i, n, i, t, i, a, l, i, z, e, :, 3, 8, 6, 注, 册, 家, 族, /, 专, 家, ,,  , r, e, g, i, s, t, e, r, C, a, p, a, b, i, l, i, t, y, :, 4, 1, 2, 按, i, m, p, l, e, m, e, n, t, a, t, i, o, n, 登, 记, ,,  , e, x, e, c, u, t, e, :, 5, 8, 7,  , e, x, p, e, r, t, 任, 务, 入, 口 。

如何闭环：按本卡事实缺口及UF14修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B07-C02 有限 typed AST、前置解析预算与因果行为去重

历史声明：f30ad7cc4c82dc22173dd0255afb58f08e61bc56；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/PolicyAstR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/PolicySpecR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/BirthGateR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：PolicyAstR27.parse在字节扫描及每次递归前计费节点/深度，IMPORT上限64KiB/64层/4096节点；finiteParameter拒绝NaN/Infinity，类型/arity/因果合同存在。

- 已实现：EvolutionLoop.stepGate对所有pending候选先candidateContract、hash去重、gate.check和行为签名重复判定；不同位置/输入scope分离。

- 已实现：PolicySpecR27.executable/canonical/policyHash包含selector/readout/updater参数，withSelector/withUpdater产生对应执行身份；正式出生复杂度由BirthGate更严门约束。

- 未闭环/待验证：本轮未重跑超限/深递归/未知op/arity/非有限数、所有旧策略逐样本hash与行为兼容、改selector/updater身份、无未来标签及K3输出反例；不能由源代码限制存在替代当前回归。

- 未闭环/待验证：尚须按全局调用图核实兼容导入/恢复/结构生成/变异交叉融合没有绕过正式出生门；本轮确认主Evolution入口，不声称全1278文件入口已穷尽。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/PolicyAstR27.java` P, a, r, s, e, B, u, d, g, e, t, /, p, a, r, s, e, :, 6, 4, -, 1, 1, 0, ,,  , t, y, p, e, /, a, r, i, t, y, :, 1, 3, 6, 起, ,,  , r, a, n, g, e, A, n, d, C, a, u, s, a, l, i, t, y 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/PolicySpecR27.java` w, i, t, h, S, e, l, e, c, t, o, r, /, w, i, t, h, U, p, d, a, t, e, r, :, 4, 1, -, 4, 2, ,,  , e, x, e, c, u, t, a, b, l, e, /, c, a, n, o, n, i, c, a, l, /, p, o, l, i, c, y, H, a, s, h, :, 5, 7, -, 7, 0 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/BirthGateR27.java` c, a, n, d, i, d, a, t, e, C, o, n, t, r, a, c, t, /, c, h, e, c, k, C, a, n, d, i, d, a, t, e, :, 4, 5, -, 6, 0, ,,  , c, h, e, c, k, :, 6, 3, 起, ,,  , d, u, p, l, i, c, a, t, e, O, f, /, r, e, g, i, s, t, e, r, :, 1, 0, 9, -, 1, 2, 0 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java` s, t, e, p, G, a, t, e, :, 2, 5, 1, -, 2, 8, 9, 正, 式, 候, 选, 统, 一, 门, 及, 谱, 系 。

如何闭环：按本卡事实缺口及UF14修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B07-C03 以真实增益和成本驱动的有限创造预算

历史声明：f30ad7cc4c82dc22173dd0255afb58f08e61bc56；原验收分层：{"LOCAL": 4, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelEvolutionSU2.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：EvolutionLoop.stepBirth实际按CreationBudget.allocate给每个主思路族最小探索并分配变异/交叉/融合/专家提案；stepReplay用costUnits×rows准入并明确记录预算DEFERRED。

- 已实现：CreationBudget.creditOperator按op+父子hash+window幂等，低支持回先验、权重保留25%探索；stepHalving实际同SELECT父子效用差减成本记credit。

- 已实现：RNG、creationBudget、creditedExperiments等进入EvolutionLoop.snapshot/restore；淘汰路径释放trace/cache；ResearchContext.weights/familyCredit/appendBudgetReceipt也接入真实出生与同事务stageResearchHypotheses。

- 未闭环/待验证：这里costUnits是程序节点数×评估行数的确定性预算，不是逐候选实测CPU/峰值；必须在报告区分估计、实测和SLO证据，不能称已证明实时不劣化。

- 未闭环/待验证：本轮尚未做各族轮次不饥饿、重复credit、昂贵低增益候选、重启seed/分配/credit一致、退役真实内存下降的当前全套验证。

- 未闭环/待验证：失败机制/专家真实执行路由缺口仍按UF13、完整工厂检出力缺口按UF14返修；现有出生数和漏斗不能证明自然质量改善。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java` s, t, a, g, e, R, e, s, e, a, r, c, h, H, y, p, o, t, h, e, s, e, s, :, 8, 8, ,,  , s, t, e, p, B, i, r, t, h, :, 1, 9, 9, -, 2, 4, 7, ,,  , s, t, e, p, R, e, p, l, a, y, O, w, n, e, d, R, 3, 1, :, 3, 1, 6, -, 3, 3, 3, ,,  , s, t, e, p, H, a, l, v, i, n, g, :, 4, 1, 6, -, 4, 4, 1, ,,  , s, n, a, p, s, h, o, t, /, r, e, s, t, o, r, e, :, 6, 6, 8, -, 7, 6, 1 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/CreationBudgetR31.java` a, l, l, o, c, a, t, e, :, 5, 7, -, 8, 7, ,,  , a, d, m, i, t, :, 9, 5, -, 1, 0, 1, ,,  , c, r, e, d, i, t, O, p, e, r, a, t, o, r, /, w, e, i, g, h, t, e, d, :, 1, 0, 5, -, 1, 3, 2, ,,  , j, s, o, n, /, r, e, s, t, o, r, e, :, 1, 4, 2, 起 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java` w, e, i, g, h, t, s, /, f, a, m, i, l, y, C, r, e, d, i, t, /, a, p, p, e, n, d, B, u, d, g, e, t, R, e, c, e, i, p, t, :, 1, 9, -, 2, 1 。

如何闭环：按本卡事实缺口及UF14修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B07-C04 父子同机会比较、嵌套时间验证和诚实代际提升

历史声明：f30ad7cc4c82dc22173dd0255afb58f08e61bc56；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationStoreR30.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedReplayRunnerR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/VerifiedImprovementR31.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：EvolutionLoop.stepHoldout把候选及对照冻结为外部确认；RuntimeR27独立处理FrozenConfirmation，不把确认分数送回同一个搜索循环。

- 已实现：FrozenConfirmationR30.evaluate对同窗口父子、I00及null做配对比较；FrozenConfirmationStore.complete/register/generationReport有不可变task/result identity与verified才登记未来观察的生产链。

- 已实现：EvolutionLoop.stepRecord实际输出VerifiedImprovementR31.developmentReceipt，明确开发证据至多PROVISIONAL；PairedReplayRunner经ResearchStudyTask真实用于受控反事实配对研究。

- 已实现：新版VerifiedImprovementR31.record/compareContract/isEligibleForChampionReplacement全局生产搜索无外部调用，不能凭其helper测试声称新改善合同全部进入生产；既有FrozenConfirmation链需按等价完整性核对。

- 未闭环/待验证：需核实生产改善声明逐项绑定独立segment消费、完整机会分母、frozen/outcome/selection哈希、error budget及cost；新helper未接线不可当完成，若旧确认链等价满足则保留并补证据，缺项才改真实链路。

- 未闭环/待验证：本轮尚未执行未来标签泄漏、只挑子代赢的期、更名/重启重复确认不增独立证据、改善反查关键原件缺失必须PROVISIONAL等实际生产集成反例。

- 未闭环/待验证：正式自然期数据与未来资格不能靠历史开发paired improvement替代；不能承诺每代必然更强或为主链非0降低门槛。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java` s, t, e, p, H, o, l, d, o, u, t, :, 5, 1, 1, -, 5, 2, 0, ,,  , s, t, e, p, R, e, c, o, r, d, :, 5, 2, 2, -, 5, 4, 4, ,,  , s, t, e, p, P, r, o, m, o, t, e, :, 5, 4, 6, -, 5, 4, 8 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java` 冻, 结, 外, 部, 确, 认, 处, 理, :, 3, 2, 2, -, 3, 5, 9 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationR30.java` e, v, a, l, u, a, t, e, /, p, a, i, r, e, d, 验, 证, :, 1, 0, 9, -, 1, 2, 4, ,,  , c, o, n, f, i, r, m,  , e, v, i, d, e, n, c, e,  , l, e, d, g, e, r, :, 1, 3, 2, -, 1, 4, 2 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationStoreR30.java` c, o, m, p, l, e, t, e, :, 1, 2, 0, -, 1, 3, 1, ,,  , r, e, g, i, s, t, e, r, :, 1, 3, 8, -, 1, 5, 6, ,,  , g, e, n, e, r, a, t, i, o, n, R, e, p, o, r, t, :, 1, 6, 0, -, 1, 8, 4 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/VerifiedImprovementR31.java` r, e, c, o, r, d, /, c, o, m, p, a, r, e, C, o, n, t, r, a, c, t, /, i, s, E, l, i, g, i, b, l, e, 为, 辅, 助, 接, 口, ；, d, e, v, e, l, o, p, m, e, n, t, R, e, c, e, i, p, t, 有, E, v, o, l, u, t, i, o, n, L, o, o, p, 生, 产, 调, 用 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStudyTaskR30R1.java` s, t, e, p, :, 4, 4, -, 5, 2, 实, 际, n, e, w, /, r, e, s, t, o, r, e,  , P, a, i, r, e, d, R, e, p, l, a, y, R, u, n, n, e, r, 、, a, d, v, a, n, c, e, 及, 持, 久, 研, 究, 结, 果 。

如何闭环：按本卡事实缺口及UF14修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B07-C05 完整工厂检出力、无信号误报与自然数据证据分开验收

历史声明：f30ad7cc4c82dc22173dd0255afb58f08e61bc56；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/SequentialQualificationR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchCapabilityExperimentR31.java`。

事实记录 1（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：预注册seed/样本/预算、流生成、trial失败区分、完整性及误报/功效区间汇总已实现；当前实验候选固定为4程序，管线使用生产数值内核，但并未驱动完整创造工厂编排。

- 未闭环/待验证：须使用隔离命名空间的确定性适配器驱动真实家族/专家/提案/训练/筛选/确认/资格全流程，登记全部搜索暴露和成本；保留当前4程序为内核测试，不称完整工厂证据。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchCapabilityExperimentR31.java` register / candidates / PRODUCTION_STAGES / runPipeline / summarize L4,4,-,4。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java` stepSwitchR31 / stepBirth L1,6,9,-。

如何闭环：按本卡事实缺口及UF14修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B07-C06 失败归因、短期状态与多位置结构的实质能力升级

历史声明：f30ad7cc4c82dc22173dd0255afb58f08e61bc56；原验收分层：{"LOCAL": 4, "NATURAL": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDirectedResearchR26.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/DirectedResearchPolicyR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchHypothesisR31.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/RegimeHypothesisR31.java`。

事实记录 1（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：原失败研究的来源校验、父结构对照、子代登记和游标事务存在；新增typed hypothesis可以描述机制并有dispatch/credit辅助逻辑。现生产select仍先按F08/F17产生结构变体，再把hypothesis附为metadata。

- 未闭环/待验证：typed机制必须在真实birth之前决定可执行adapter或UNSUPPORTED；component证据、regime持久状态、真实子代谱系及幂等outcome credit须接入生产。六类失败的helper字符串测试不能关闭实际路由。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDirectedResearchR26.java` select / hypothesisR31 / recordWithin L1,4,-,4。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchHypothesisR31.java` fromFailure / Registry.dispatch / Registry.recordOutcome L6,1,-,1。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RegimeHypothesisR31.java` proposeWindow / routePastState / toSinglePositionK3 。

如何闭环：按本卡事实缺口及UF13修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B08-C01 矩阵缺学生依赖隔离与按 kind 持续推进

历史声明：56952133aace3b388a7d24b21cb0e7987709a65b；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryR30.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：MatrixResearchR30R1已实际实现PRODUCED/WAITING_STUDENT/WAITING_SOURCE/INSUFFICIENT/INVALID；缺学生或非法数值只影响当前kind，SQLite完整性错误仍向上抛。

- 已实现：dependencyKeyR31含dataset/revision/kind/policy set/measured version/source commit；相同key已决定则跳过；非PRODUCED保留lastValidMatrixHash并标stale，不写全零替代。

- 已实现：RuntimeR27注册MatrixProductionTask并真实调用step；step每轮选一个未决定kind，提交状态/必要tensor后返回，后续slice继续下一个kind；source-only kind不要求population里有student。

- 未闭环/待验证：MatrixResearchR30R1.step:112仍以rt.evo/context为整个任务前置；没有evolution上下文时source-only同样DEPENDENCY(r27-evolution)，尚未彻底拆独立输入依赖。

- 未闭环/待验证：kind决定状态与dependency key保存在r27_chart_cache而非独立typed状态表；这是生产实现而非helper-only，但清缓存后会重新决定，需要与UF16缓存生命周期合同核对。

- 未闭环/待验证：未运行缺学生时source-only持续产出、依赖不变100次无重复等待、学生发布唤醒、NaN/维度/source修订等测试；不记PASS。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java` produceR31 / dependencyKeyR31 / receiptR31 L25。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java` step L111。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java` constructor / MatrixProductionTask.step L70。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryR30.java` Definition / dependencyClassR31 / typedConsumers L12。

如何闭环：按本卡事实缺口及UF16修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B08-C02 研究矩阵到公式的 typed 来源与消费闭环

历史声明：56952133aace3b388a7d24b21cb0e7987709a65b；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixEvidenceR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：ResearchContext.read直接读原r30r1_analysis_run/research_measure，严格SELECT、COMMITTED、dataset/revision/source/ABI及frame hash，不依赖图cache作为消费权威。

- 已实现：已有window/cross/molecule/position/fusion/校准/残差/动作等实际假设构造函数，annotate把source runs带入候选；RuntimeR27生产读取context，EvolutionLoop.stageResearchHypotheses与出生batch登记proposal。

- 已实现：ResearchStore.consume生产路径在同出生batch校验合法source、候选hash、实际BORN child并登记consume及source consumption measure；EvolutionLoop区分受研究影响出生与配对收益存活计数。

- 已实现：MatrixResearch及MatrixEvidence为tensor记录role、unit、axes、support、calendar span和关联/干预语义，tensor不直接授主链权限。

- 未闭环/待验证：recordUsageR31记录在generation内List；usagesR31()全生产无外部读取，withoutSourceRunR31()全生产无调用。现有proposal→BORN消费链真实存在，但新增完整参数before/after、预算、实验级回执及同预算禁用sourceRun消融未证明接入。

- 未闭环/待验证：ResearchStore.ready当前显式只路由PRUNE_COMPONENT；其他context假设通过stage/出生链实现，不能凭统一typedConsumers名字宣称每一数值产品动作合同均已满足。

- 未闭环/待验证：未执行逐source raw→run→proposal→child→配对结果反查、删UI缓存不影响权威、source修订拒绝和同预算消融；不声称研究提高胜率。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java` read / stage / annotate L9。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java` EvolutionTask.step L282。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java` currentSource / ready / consume L96。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java` stageResearchHypotheses / stepBirth / stepRecord L88。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java` build L103。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java` recordUsageR31 / usagesR31 / withoutSourceRunR31 L38。

如何闭环：按本卡事实缺口及UF16修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B08-C03 扩展配对子树、家族和融合研究且不混用置信区间

历史声明：56952133aace3b388a7d24b21cb0e7987709a65b；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStudyTaskR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedReplayRunnerR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGainR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStudyCheckpointR31.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：RuntimeR27真实注册ResearchStudyTask并委托ResearchStudyTaskR30R1.step；每代轮转8父代，避免永久只研究排序前8。

- 已实现：组件清单显式标SUPPORTED/UNSUPPORTED_PAIR；MASK_FROZEN与RETRAIN_REMOVED分别执行、分别保存protocol/effect，只有RETRAIN_REMOVED进入paired→proposal路径。

- 已实现：PairedReplayRunner约束SELECT及窗口、每片≤256样本、两边bias/state分离；snapshot/restore保存并验证两边状态、cursor、contract、counterfactual及hash。

- 已实现：ResearchGain按唯一日历期和固定完整块给DESCRIPTIVE区间，支持不足保持未知，拒绝直接认证。

- 已实现：生产step已有实际typed checkpoint双写：优先读analysis_run、兼容旧cache，同batch写typed run与cache并guard source revision。

- 未闭环/待验证：componentsR31:19仅root/0、root/1；family、selector、calibrator在24-26明确UNSUPPORTED_PAIR，未见fusion成员adapter。诚实标边界已实现，但原卡扩展family/fusion/selector/calibrator能力未完整实现。

- 未闭环/待验证：ResearchStudyCheckpointR31无生产外部调用；其通用迁移/authority helper不能当生产持久迁移完成收据。实际Study.step另有typed双写，不能误写checkpoint完全未迁。

- 未闭环/待验证：未测清cache前迁移覆盖、中断恢复两侧参数等价、protocol分栏及持续覆盖；不记PASS。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java` constructor / ResearchStudyTask.step L70。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStudyTaskR30R1.java` rotationR31 / componentsR31 / step L17。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedReplayRunnerR30R1.java` arm / advance / snapshot / restore L19。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGainR30R1.java` add / estimate / blocks / critical95 L17。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStudyCheckpointR31.java` Authority / migrate / put / get L13。

如何闭环：按本卡事实缺口及UF16修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B08-C04 研究产物与消费者接成可验证数值合同

历史声明：56952133aace3b388a7d24b21cb0e7987709a65b；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGraphSpecsR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchResultContractR31.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：ResearchGraphSpecs已为18视图接共享measure枚举、role、producerVersion、consumerKind和required receipts；R11/R14/R18明确FORMAL_READ_ONLY。

- 已实现：ResearchResultContractR31实现ProductState、身份校验、行动receipt字段、TRAIN/SELECT限定、late label/parent/ABI/revision/重复consume/无实际变化拒绝及同预算effectiveness判定。

- 已实现：ResearchObservatory生产已由RuntimeR27注册，product真实写原始analysis_run/research_measure和可重建chart投影；正式只读视图不授主链权限。

- 已实现：原ResearchStore PRUNE及context→出生消费链实际存在，不能误写全部消费者都未实现。

- 未闭环/待验证：全app/src/main/java检索validateForConsumption只命中其定义；未见new ResearchResultContractR31.Result或effectiveness生产调用。现有Spec对该类仅引用枚举/required字段/consumptionKind元数据。新统一数值合同未成为生产消费闸门。

- 未闭环/待验证：Observatory.step:78仍整体要求rt.evo；缺独立演化上下文时所有图都等待，未完全拆为每产品最小输入依赖。现有数值计算/原件生产真实存在。

- 未闭环/待验证：未逐R01/02/03/04/07/08/09/10/12/13/15/16/17验证实际consumer动作及10类receipt、错误角色/ABI/revision/parent/late label拒绝和同预算消融；源码helper测试不能代替生产闭环。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGraphSpecsR30R1.java` Spec / ALL L7。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchResultContractR31.java` Result / validateIdentity / validateForConsumption / effectiveness L34。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java` product / step L72。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java` constructor / ObservatoryTask.step L70。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java` consume L112。

如何闭环：按本卡事实缺口及UF16修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B08-C05 修复T09枚举错配并验收16个逻辑研究表

历史声明：56952133aace3b388a7d24b21cb0e7987709a65b；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAdaptersR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGraphSpecsR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchMeasureKindR31.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchMeasureProjectionR31.java`。

事实记录 1（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：T01-T16逻辑SQL、绑定分页接口、共享kind以及T09正确OPERATOR_GAIN_COST枚举已实现；ResearchMeasureProjectionR31已能提取frame.points对象数值。当前T09查询仍选择顶层mean_gain，分页和对象投影未发现实际产品读者调用。

- 未闭环/待验证：接入真实只读读者及界面/报告，T09顶层null但frame有值时显示正确对象gain/区间/n/nodeCost/hash；16表按当前schema实际执行、角色/来源筛选、分页与缺数据状态均需验证。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAdaptersR30R1.java` SQL initialization / page / contracts L7,-,2,3。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchMeasureProjectionR31.java` projectObjects / operatorGainCost L4,2,-,6。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java` capture L8,6。

如何闭环：按本卡事实缺口及UF16修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B09-C01 以稳定ID统一47个实际面板合同

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java`, `app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR83.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java`, `app/src/main/java/com/ethk3/novaonepro/ChartRegistryR31.java`。

事实记录 1（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：稳定图ID路由、普通缓存帧字符上限、部分坏JSON隔离、500点展示截取和缓存分类已有实现；当前缓存READY仍直接透传，updated_at主要供显示，snapshot包解析缺同等大小前置约束。不能据此认定47图均已正确运行。

- 未闭环/待验证：真实repository/load/render链需补齐schema/ABI/hash/来源水位/新鲜度，取消传播、快照大小限制和图间隔离；缓存投影不得在刷新时重复全量报告采集；研究健康须依真实commit而非线程/代数。47图现版逐项渲染和同源数值回归未实跑。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java` load / cached / snapshotR30 / headline L3,0,-,4。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java` classR31 / build / safe / health L2,0,-,3。

如何闭环：按本卡事实缺口及UF15修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B09-C02 隔离慢图读取与生命周期取消

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 2, "EMU": 2, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/ChartsActivityR83.java`, `app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR83.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchActivityR30R1.java`。

事实记录 1（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：稳定图ID路由、普通缓存帧字符上限、部分坏JSON隔离、500点展示截取和缓存分类已有实现；当前缓存READY仍直接透传，updated_at主要供显示，snapshot包解析缺同等大小前置约束。不能据此认定47图均已正确运行。

- 未闭环/待验证：真实repository/load/render链需补齐schema/ABI/hash/来源水位/新鲜度，取消传播、快照大小限制和图间隔离；缓存投影不得在刷新时重复全量报告采集；研究健康须依真实commit而非线程/代数。47图现版逐项渲染和同源数值回归未实跑。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java` load / cached / snapshotR30 / headline L3,0,-,4。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java` classR31 / build / safe / health L2,0,-,3。

如何闭环：按本卡事实缺口及UF15修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B09-C03 缓存投影限额与业务真值隔离

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java`, `app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchFrameBoundR30R1.java`。

事实记录 1（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：稳定图ID路由、普通缓存帧字符上限、部分坏JSON隔离、500点展示截取和缓存分类已有实现；当前缓存READY仍直接透传，updated_at主要供显示，snapshot包解析缺同等大小前置约束。不能据此认定47图均已正确运行。

- 未闭环/待验证：真实repository/load/render链需补齐schema/ABI/hash/来源水位/新鲜度，取消传播、快照大小限制和图间隔离；缓存投影不得在刷新时重复全量报告采集；研究健康须依真实commit而非线程/代数。47图现版逐项渲染和同源数值回归未实跑。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java` load / cached / snapshotR30 / headline L3,0,-,4。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java` classR31 / build / safe / health L2,0,-,3。

如何闭环：按本卡事实缺口及UF15修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B09-C04 前向榜、校准图、资金图统一证据口径

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR83.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java`。

事实记录 1（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：稳定图ID路由、普通缓存帧字符上限、部分坏JSON隔离、500点展示截取和缓存分类已有实现；当前缓存READY仍直接透传，updated_at主要供显示，snapshot包解析缺同等大小前置约束。不能据此认定47图均已正确运行。

- 未闭环/待验证：真实repository/load/render链需补齐schema/ABI/hash/来源水位/新鲜度，取消传播、快照大小限制和图间隔离；缓存投影不得在刷新时重复全量报告采集；研究健康须依真实commit而非线程/代数。47图现版逐项渲染和同源数值回归未实跑。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java` load / cached / snapshotR30 / headline L3,0,-,4。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java` classR31 / build / safe / health L2,0,-,3。

如何闭环：按本卡事实缺口及UF11,UF15修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B09-C05 47图与16逻辑表设备回归证据

历史声明：4246c5f4149443fdcadadbf5695690508b4cb423；原验收分层：{"LOCAL": 2, "EMU": 1, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/ChartsActivityR83.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchActivityR30R1.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java`。

事实记录 1（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：稳定图ID路由、普通缓存帧字符上限、部分坏JSON隔离、500点展示截取和缓存分类已有实现；当前缓存READY仍直接透传，updated_at主要供显示，snapshot包解析缺同等大小前置约束。不能据此认定47图均已正确运行。

- 未闭环/待验证：真实repository/load/render链需补齐schema/ABI/hash/来源水位/新鲜度，取消传播、快照大小限制和图间隔离；缓存投影不得在刷新时重复全量报告采集；研究健康须依真实commit而非线程/代数。47图现版逐项渲染和同源数值回归未实跑。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java` load / cached / snapshotR30 / headline L3,0,-,4。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java` classR31 / build / safe / health L2,0,-,3。

如何闭环：按本卡事实缺口及UF15修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B10-C01 主链选择从图表缓存迁移到唯一 typed 权威表

历史声明：4a7cc33a389975eaad218b44015573ed58d87f29；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineBridgeSU2.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterSU2.java`。

事实记录 1（lifecycle_completion_audit_rows.json，保留完整机器证据）：

- 已实现：MainlineChoiceStoreR31实际DDL、历史/冲突/fence保护已实现；LiveShadowsR27.certify在同一batch.after中双写旧choice与appendChoice。

- 已实现：MainlineBridgeSU2.r27Intent/projectionR30实际读authorizedChoice，DIVERGED阻断而不是OR任选。

- 未闭环/待验证：switchToTypedRead和MainlineChoiceStoreR31.revoke在全生产源码仅定义，未见受控切换及typed撤销事件消费者；默认LEGACY_READ，尚未完成唯一typed权威启用闭环。

- 未闭环/待验证：effectiveChoice只接now并核认证书，不接目标ordinal；有效期序号、实际model publish receipt/state hash与冻结choice_id的绑定仍需真实路径核查和补齐，不能只存字段。

- 未闭环/待验证：当前写入modelState用SEMANTIC_ABI字符串hash，不是已验证实际模型状态发布回执；需要按原卡合同接到真实模型身份。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java` certify, batch.after L285,296。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java` appendChoice, effectiveChoice, revoke, switchToTypedRead, authorizedChoice L58,79,89,113。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineBridgeSU2.java` projectionR30, r27Intent L118,165。

如何闭环：按本卡事实缺口及UF17修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B10-C02 审计资格功效与连续败绩风险并版本化新合同

历史声明：4a7cc33a389975eaad218b44015573ed58d87f29；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/GateV2R29.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/SequentialQualificationR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceContractR31.java`。

事实记录 1（lifecycle_completion_audit_rows.json，保留完整机器证据）：

- 已实现：CertificateGateR27保持V1阈值并增加精确iid最长连败DP及敏感性方法；GateV2R29仍报告用途，LiveShadows实际record v2结果。

- 已实现：EvidenceContractR31实现register/validateDesign/activate/authorityFor接口，要求工厂误报与功效Summary。

- 未闭环/待验证：EvidenceContractR31在全生产源码仅定义，无真实预注册/持久注册/完整工厂验证/新实验合同启用调用；报告当前gateAuthority仅旧contract和report-only标志，不能把新合同API当生效。

- 未闭环/待验证：完整工厂Summary自身当前仅固定四程序实验（另审阅已证实），不能据其替代真实多候选搜索误报/功效验证；未验证新合同必须保持无授权。

- 未闭环/待验证：本轮未运行DP边界/V1重放/alpha不重置等当前验收。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java` contract, streakGatePassProbabilityR31, streakGateSensitivityR31 L62,76。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceContractR31.java` register, validateDesign, activate, authorityFor L30,43,58,63。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java` certify L268,275。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java` capture L83。

如何闭环：按本卡事实缺口及UF17修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B10-C03 合理出手频率、唯一未来证据与撤销闭环

历史声明：4a7cc33a389975eaad218b44015573ed58d87f29；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/PolicySpecR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationInboxR29.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineBridgeSU2.java`。

事实记录 1（lifecycle_completion_audit_rows.json，保留完整机器证据）：

- 已实现：LiveShadowsR27已有未来冻结、迟到DEADLINE_MISSED、结算、资格与发证路径；Bridge用有效证书构造R27意图。

- 已实现：CertificateRevocationR30已有draw修订trigger与revokeWithin，在原writer事务中撤销证书，effectiveChoice拒绝失效证书。

- 未闭环/待验证：typed choice撤销方法未接入生产事件；当前R27冻结INSERT仅policy/target/probabilities/numbers/act/time等字段，choice/contract/实际model publish receipt的完整冻结绑定需核实补齐。

- 未闭环/待验证：EvidenceContractR31.Opportunities/frequencyReport无外部生产调用，原卡要求eligible/ACT/ABSTAIN/MISSED_INPUT/MISSED_DEADLINE分母不能靠辅助对象存在判完成。

- 未闭环/待验证：未运行合法测试证书经唯一DecisionGate→真实冻结→开奖→结算→反馈→新模型→下次预测的全链；自然合格不保证。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java` freeze, settleBatch, qualify, certify L187,213,285。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java` sourceTrigger, revokeWithin, statementParts L66,86,137。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java` effectiveChoice, revoke L79,89。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterSU2.java` prepare L23。

如何闭环：按本卡事实缺口及UF17修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B11-C01 逐项接齐44指标及设备证据导入

历史声明：64058996c0d715de5f70da3e693042ac5932c4aa；原验收分层：{"LOCAL": 4}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricDefinitionsR29.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricEvaluatorR29.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`。

事实记录 1（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已有R31版本指标定义、receipt形状MeasurementR31、R31 evaluator及同scope/watermark跨源比较helper。

- 已实现：完整报告当前仍建立R30Input并执行evaluateR30；新helper的存在不等于生产指标已切换。

- 未闭环/待验证：R30Input构造器仍调用evaluateR30；其M32仍写observed 0。evaluateR31/crossSourceR31生产搜索只有定义，未接capture导出链。

- 未闭环/待验证：R31 evaluator仅接收caller传入receiptHash/value/met；实际测量源、内容hash/身份/窗口/角色验证必须由生产采集链闭合，不能以64hex字符串视已验真。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricDefinitionsR29.java` r31Definitions L20。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricEvaluatorR29.java` evaluateR31 L173。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricEvaluatorR29.java` crossSourceR31 L198。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR27.java` R30Input constructor L182。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricEvaluatorR29.java` currentMeasurement case M32 L119。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java` drain report chapters L325。

事实记录 2（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已实现44项R31定义、检查交叉账及独立评估辅助方法；现行生产报告仍由R30Input调用evaluateR30并使用currentR30定义。存在代码与历史绑定，不等于R31生产采集已完成。

- 未闭环/待验证：需把R31定义、真实collector、96检查证据范围和跨源同scope/同水位比较接入实际快照及最终ZIP；M32不得硬编码零冲突；缺源不得补0。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR27.java` R30Input constructor L1,7,7,-。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricDefinitionsR29.java` r31Definitions L2,0,-,3。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricEvaluatorR29.java` evaluateR30 / evaluateR31 / currentMeasurement L4,9,-,6。

- 源码依据：`app/src/test/java/com/ethk3/novaonepro/strategyv2/R31MetricReportTest.java` jsonAndHtmlOfOneSnapshotRowCarryTheSameFields / crossSourceWatermarkConflictsAreDetectedAndLegalDifferentScopesAreNot L2,5,-,5。

如何闭环：按本卡事实缺口及UF10修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B11-C02 报告区分已存在、已运行、已消费与有效

历史声明：64058996c0d715de5f70da3e693042ac5932c4aa；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR29.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java`。

事实记录 1（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：旧生产索引已列29旧图、18研究图、矩阵/能力元数据，并明确存在与效果分开。

- 已实现：新增ObjectFactsR31/layersR31/objectEntriesR31可表达六层状态及NOT_APPLICABLE。

- 未闭环/待验证：ReportSnapshot.capture仍调用旧build；新版objectEntriesR31/layersR31没有生产capture调用。旧state默认consumed/effective=null且artifactReference静态指向snapshot.db。

- 未闭环/待验证：不能据新helper单测或对象清单总数认定每个生产对象实际运行、提交、消费、有效均有来源证据。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java` capture L97。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java` state L9。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java` layersR31 L53。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java` objectEntriesR31 L65。

事实记录 2（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：旧生产索引已逐项列出29旧图、18研究图、20矩阵和82能力；新增六层状态与对象条目辅助方法也已实现。旧生产build仍沿用部分静态绑定及行数，并未调用新六层入口。

- 未闭环/待验证：各对象的真实输入、最近commit、有效数值、消费、独立效果及部署事实尚须接入同水位报告；不适用消费必须与未知分开。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java` capture L9,7。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java` state / build / layersR31 / objectEntriesR31 L9,-,3,6。

如何闭环：按本卡事实缺口及UF10修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B11-C03 有界报告与可取回证据附件

历史声明：64058996c0d715de5f70da3e693042ac5932c4aa；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportChartsR30.java`。

事实记录 1（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已有单只读数据库快照、12章JSON/Markdown共享同一次章节对象、ZIP/分卷hash、provider与SAF输出回读代码。

- 已实现：已有AttachmentsR31的有界规划/流式hash/验证helper；默认不附snapshot.db，raw选项才附。

- 未闭环/待验证：最小请求没有先持久化；可选观察文件失败时请求尚未入FIFO。kick失败发生在入队后却统一写FAILED，有队列中后续仍可运行的终态歧义。

- 未闭环/待验证：READY journal先写再ACK；ACK及一次retry失败后，空FIFO启动与UI READY recover均不修ACK，完整ZIP可永远藏在旧回执后。

- 未闭环/待验证：READY ACK暴露早于workspace reservation释放；UI grant的独立job/share-until admission可因scope overlap失败。

- 未闭环/待验证：默认无snapshot.db，但生产rowsetIndexes/fullEvidence和对象artifactReference仍指snapshot.db；AttachmentsR31只见定义/测试，未接真实included/retained/exportable清单和重算依赖闭包。

- 未闭环/待验证：Query.rows用group_concat聚合后再substr，不等于流式物化；超大输入下内存/SQLite聚合边界仍需真实压力验证。

- 未闭环/待验证：新Android完整Service测试仅校验READY和交付文件hash，不解析ZIP必需成员、12章语义、raw边界、指标/对象引用闭包，也不走SAF。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java` requestExport L59。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java` requestExport L91。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java` transition L133。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java` drain L213。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/CompleteReportFileR927.java` recover L36。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java` publish L428。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java` grant L105。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java` Query.rows L40。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java` capture rowsetIndexes L73。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java` AttachmentsR31 L298。

- 源码依据：`app/src/androidTest/java/com/ethk3/novaonepro/R31QDeliveryInstrumentedTest.java` fullReportReachesPublishedZipAndProviderReadback L114。

如何闭环：按本卡事实缺口及UF06,UF07,UF08,UF11修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B11-C04 建立96检查与44指标的显式交叉账

历史声明：64058996c0d715de5f70da3e693042ac5932c4aa；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR29.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricDefinitionsR29.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckMetricCrosswalkR31.java`。

事实记录 1（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：CheckMetricCrosswalkR31列96条静态check与44个产品指标关系，明确EXECUTED不是domain PASS、结构检查不能替代设备或质量证据。

- 未闭环/待验证：生产ReportR29.chapters/document未导出该交叉账；CheckMetricCrosswalk生产引用仅来自未接生产的r31Definitions。

- 未闭环/待验证：静态CHECKS含历史EXECUTED/outcome，不能当本次Service同快照真实96结果；必须与本次ReportCatalog检查结果按id/source/window关联。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckMetricCrosswalkR31.java` class provenance/comment L6。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckMetricCrosswalkR31.java` CHECKS L34。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricDefinitionsR29.java` r31Definitions L23。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR29.java` document L52。

事实记录 2（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已实现44项R31定义、检查交叉账及独立评估辅助方法；现行生产报告仍由R30Input调用evaluateR30并使用currentR30定义。存在代码与历史绑定，不等于R31生产采集已完成。

- 未闭环/待验证：需把R31定义、真实collector、96检查证据范围和跨源同scope/同水位比较接入实际快照及最终ZIP；M32不得硬编码零冲突；缺源不得补0。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR27.java` R30Input constructor L1,7,7,-。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricDefinitionsR29.java` r31Definitions L2,0,-,3。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricEvaluatorR29.java` evaluateR30 / evaluateR31 / currentMeasurement L4,9,-,6。

- 源码依据：`app/src/test/java/com/ethk3/novaonepro/strategyv2/R31MetricReportTest.java` jsonAndHtmlOfOneSnapshotRowCarryTheSameFields / crossSourceWatermarkConflictsAreDetectedAndLegalDifferentScopesAreNot L2,5,-,5。

如何闭环：按本卡事实缺口及UF10修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B12-C01 覆盖安装与迁移可恢复性联合验收

历史声明：63fae8a4c1cb4a38a991293d1b48a97b5e6d8e84；原验收分层：{"LOCAL": 2, "EMU": 2}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java`, `tools/r31/upgrade_matrix.py`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已有PREPARE/VERIFY_SOURCE/TRANSFORM/VERIFY_TARGET/PUBLISH/CLEANUP阶段、hash、attempt与重入接口；RuntimeCoordinator启动schema使用此入口。

- 已实现：Legacy新增逐单元journal、同handle校验及AUDITED_RUNTIME_STEP_R31Q21，旧LEGACY_RUNTIME/MAIN_CHAIN完成标记不能跳过新runtime审计。

- 已实现：upgrade_matrix明确区分HOST/SIMULATED，真实600MB/30GB、旧APK读新库、Android覆盖kill标NOT_RUN。

- 未闭环/待验证：MAIN_INDEXES内CREATE INDEX普通失败被吞，单元可误标COMPLETED；最终MAIN_CHAIN校验FAILED后存在incomplete row，prepareRepairIfDrift不重置INDEXES，故去除外部故障也可能永久重试最终校验。

- 未闭环/待验证：运行期index helper也吞错误且audit只跟踪column/probe；未纳入最终合同的index丢失可被错误READY永久跳过。

- 未闭环/待验证：当前Sliced新路径未完成真实旧库、实际进程kill及覆盖安装回滚/前滚验证；现有旧host无权替代Android结论。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java` runUpgradeR31 / upgradeStateR31 L1053。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/LegacySchemaCoordinatorR926.java` advanceUnitsR31 / ensure L19。

- 源码依据：`tools/r31/upgrade_matrix.py` SCENARIOS / NOT_RUN / run_upgrade_matrix L21。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DbHelper.java` createMainChainClosedLoopIndexes / execQuiet L6107。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java` prepareRepairIfDrift L934。

事实记录 2（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：报告journal、只读快照与发布身份提供可恢复基础；迁移部分由独立源审负责，本行只核报告恢复边界。

- 未闭环/待验证：READY ACK丢失后恢复入口只轮询ACK；无关损坏job可中断latestFullReport。

- 未闭环/待验证：旧库升级/杀进程/低磁盘联合矩阵本次未执行，历史LOCAL关门不能替代安装020或当前树的设备证明。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java` latestFullReport L114。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/CompleteReportFileR927.java` recover L34。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java` drain post-publication failure L374。

如何闭环：按本卡事实缺口及UF03,UF07,UF09,UF19修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B12-C02 统一CI、故障注入与跨批次不可回归测试

历史声明：63fae8a4c1cb4a38a991293d1b48a97b5e6d8e84；原验收分层：{"LOCAL": 3, "EMU": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`.github/workflows/r31-verification.yml`, `tools/r31/fault_injection_manifest.json`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：R31QDeliveryInstrumentedTest已有真实Android schema probe失败拒绝、旧READY新审计marker、迁移与关键写探针、完整报告provider回读、真实联网未来链测试入口。

- 已实现：run_r31q_android_delivery.sh按测试数及关键标志检查结果。

- 未闭环/待验证：名为resumes的测试未kill/restart；允许现成READY数据库，WHILE_PENDING=0也能通过，不能证明迁移期间关键写实际前进。

- 未闭环/待验证：旧READY测试只删除新日志，不能覆盖曾吞错导致真实缺column/index；probe测试用关闭数据库，不模拟真实busy/CREATE/INDEX失败。

- 未闭环/待验证：本次只读审计未运行任何当前源码测试；已有测试入口不能记PASS。

- 源码依据：`app/src/androidTest/java/com/ethk3/novaonepro/R31QDeliveryInstrumentedTest.java` firstSchemaProbeFailureStopsRemainingColumns L32。

- 源码依据：`app/src/androidTest/java/com/ethk3/novaonepro/R31QDeliveryInstrumentedTest.java` oldReadyStampCannotSkipTheNewAuditedMigrationPass L54。

- 源码依据：`app/src/androidTest/java/com/ethk3/novaonepro/R31QDeliveryInstrumentedTest.java` legacyMigrationResumesAndAllowsCriticalCommitsBetweenUnits L89。

- 源码依据：`tools/r31/run_r31q_android_delivery.sh` instrumentation result validation L24。

事实记录 2（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：统一workflow与host/Android分层存在；最新测试源码新增真实main/diagnostics Service+provider、真实live fetch/freeze/settle断言。

- 已实现：host锁测试使用两个JVM真实OS锁、短暂争用、五秒失败、中断与ledger不变断言。

- 未闭环/待验证：本次未执行最新源码测试；历史K11收据不能覆盖当前未提交或较晚修订。

- 未闭环/待验证：完整Service Android用例未覆盖admission争用/失败ACK重启/READY grant竞争/ZIP语义/SAF；observer用例不覆盖忽略中断。

- 未闭环/待验证：live测试以真实网络六分钟窗口为条件，代码存在不是自然闭环或长期连续性已验证。

- 源码依据：`.github/workflows/r31-verification.yml` instrumentation job L209。

- 源码依据：`tools/r31/host/R31QLockRegressionHost.java` main L30。

- 源码依据：`app/src/androidTest/java/com/ethk3/novaonepro/R31QDeliveryInstrumentedTest.java` fullReportReachesPublishedZipAndProviderReadback L114。

- 源码依据：`app/src/androidTest/java/com/ethk3/novaonepro/R31QDeliveryInstrumentedTest.java` liveServiceFetchesFreezesAndSettlesNewFutureObservations L148。

- 源码依据：`tools/r31/run_r31q_android_delivery.sh` install/instrument L8。

事实记录 3（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已有注册HOST、API29/35仪器矩阵、历史LOCAL绑定和构建摘要检查；新Q21锁与Android34测试存在，但完整统一验收注册及post-K11同次运行证明仍有缺口。

- 未闭环/待验证：统一注册新锁及Android34实际导出/实时闭环；清旧摘要，要求exit=0及runId/invocationId/source/tree/输入和实跑集合；post-K11复用完整close验收，不仅检查0失败。

- 源码依据：`.github/workflows/r31-verification.yml` Android matrix / instrumentation invocation L2,1,7,,。

- 源码依据：`.github/workflows/r31-gate.yml` gate-build / summary acceptance L5,9,,,7。

- 源码依据：`construction/STARCORE_R31_FINAL_20261104/tools/r31_gate.py` build / close acceptance L3,1,3,-。

- 源码依据：`app/src/test/java/com/ethk3/novaonepro/strategyv2/R27QualityTest.java` schema block guard / mutation negatives L1,6,4,-。

如何闭环：按本卡事实缺口及UF18,UF19修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B12-C03 24h初测、72h全链与长期容量验证

历史声明：63fae8a4c1cb4a38a991293d1b48a97b5e6d8e84；原验收分层：{"DEVICE": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`tools/r31/collect_device_receipts.py`, `tools/r31/accept_device_receipts.py`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：collect_device_receipts.py有collect/merge_windows/verify_scope；accept_device_receipts.py有evaluate_gate/write_verdict，具备按source/apk及窗口输出门结果的工具。

- 未闭环/待验证：未执行当前源码目标手机24h/72h；快检、boot uptime、线程alive、旧时间戳均不能替代连续业务推进证据。

- 未闭环/待验证：phone 577311020旧版本的运行反例应保留；不得自动转移成当前R31Q21验收结果，也不能将未知质量、未知训练推进写0/PASS。

- 源码依据：`tools/r31/collect_device_receipts.py` collect / merge_windows / verify_scope L72。

- 源码依据：`tools/r31/accept_device_receipts.py` evaluate_gate / write_verdict L37。

事实记录 2（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：存在按boot/source/hash采集与验收设备收据的脚本设计；历史K11和release声明均将设备层与工程层分开。

- 未闭环/待验证：24h/72h、V2353A、30GB原库和自然质量均不能由本次只读源审或6分钟模拟器用例证明；历史交付remainingLimits明确NOT_RUN/NOT_PROVEN。

- 源码依据：`release/R31_DELIVERY.json` layers.L4_PHYSICAL_DEVICE_24H_72H L40。

- 源码依据：`app/src/androidTest/java/com/ethk3/novaonepro/R31QDeliveryInstrumentedTest.java` liveServiceFetchesFreezesAndSettlesNewFutureObservations L155。

如何闭环：按本卡事实缺口及UF19修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B12-C04 受控退役与大型文件渐进拆分

历史声明：63fae8a4c1cb4a38a991293d1b48a97b5e6d8e84；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`tools/r31/retirement_manifest.json`, `app/src/main/java/com/ethk3/novaonepro/DbStore.java`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：DbHelper.dropAll已硬拒绝破坏性reset；原计划对DbStore方法选择明确为覆盖后另开精确卡，不授权整库重写。

- 未闭环/待验证：本审计未核验整份退役清单和动态SQL/反射消费者，不能确认B12-C04整体完成。

- 未闭环/待验证：不得因DbStore大或schema迁移慢而删除旧方法/表、DROP keep、清空手机库过关。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/DbHelper.java` dropAll L6854。

- 源码依据：`construction/STARCORE_R31_FINAL_20261104/ledgers/cards.json` R31-B12-C04 L1。

事实记录 3（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已有注册HOST、API29/35仪器矩阵、历史LOCAL绑定和构建摘要检查；新Q21锁与Android34测试存在，但完整统一验收注册及post-K11同次运行证明仍有缺口。

- 未闭环/待验证：统一注册新锁及Android34实际导出/实时闭环；清旧摘要，要求exit=0及runId/invocationId/source/tree/输入和实跑集合；post-K11复用完整close验收，不仅检查0失败。

- 源码依据：`.github/workflows/r31-verification.yml` Android matrix / instrumentation invocation L2,1,7,,。

- 源码依据：`.github/workflows/r31-gate.yml` gate-build / summary acceptance L5,9,,,7。

- 源码依据：`construction/STARCORE_R31_FINAL_20261104/tools/r31_gate.py` build / close acceptance L3,1,3,-。

- 源码依据：`app/src/test/java/com/ethk3/novaonepro/strategyv2/R27QualityTest.java` schema block guard / mutation negatives L1,6,4,-。

事实记录 4（lifecycle_completion_audit_rows.json，保留完整机器证据）：

- 已实现：tools/r31/retirement_manifest.json存在实际静态DROP/重建及DbStore方法覆盖清单，approved_drops为空，规则保留无证不删；retirement_inventory.py可生成/检查。

- 已实现：当前大DbStore仍保留，未发现以本卡名义全面删除旧能力的授权。

- 未闭环/待验证：静态清单不等于运行owner/读写覆盖、动态SQL/反射/持久引用和等价性证据齐备；未逐条执行其全部验证。

- 未闭环/待验证：最新迁移源码变化后清单/原文件窄块红线需重新生成核对，禁止通过批量改hash掩盖业务变更；具体提取方法仍必须另列精确卡，不准借“重构”无证删除。

- 源码依据：`tools/r31/retirement_manifest.json` approved_drops, rebuild_in_place, drop_table_sites, dbstore 。

- 源码依据：`tools/r31/retirement_inventory.py` manifest generation, check L134,150,153。

如何闭环：按本卡事实缺口及UF18修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-B12-C05 唯一成品发布收据和APK压缩包交付

历史声明：63fae8a4c1cb4a38a991293d1b48a97b5e6d8e84；原验收分层：{"LOCAL": 3, "DEVICE": 1}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`tools/r31/package_release.py`, `release/R31_DELIVERY.json`。

事实记录 1（migration_plan_audit_rows.json，保留完整机器证据）：

- 已实现：本次已定位同源码生产实现、测试入口和未闭环范围；未执行构建、签名、安装、打包或交付动作。

- 未闭环/待验证：当前工作树修改后的源码不能凭旧历史收据或已安装577311020旧APK认定完成；必须由release owner验证实际编译身份、同包名同签名递增版本。

- 源码依据：`construction/STARCORE_R31_FINAL_20261104/ledgers/cards.json` R31-B12-C05 L1。

事实记录 2（report_plan_audit_rows.json，保留完整机器证据）：

- 已实现：release/R31_DELIVERY记录旧016的source/APK/signature和分层限制；R31Q_020_DELIVERY另记录020 source/hash/signer。

- 未闭环/待验证：020收据source=5e05e668，不是当前审阅HEAD及staged修复；其scope仍physical device NOT_RUN/natural quality NOT_PROVEN。

- 未闭环/待验证：本次无构建、安装或产物签名/字节校验，不能把当前修复源码称用户已收到或安装完成。

- 源码依据：`release/R31Q_020_DELIVERY.json` sourceCommit/identity/apk L3。

- 源码依据：`release/R31Q_020_DELIVERY.json` scope L18。

- 源码依据：`release/R31_DELIVERY.json` layers L34。

事实记录 3（research_plan_audit_rows.json，保留完整机器证据）：

- 已实现：已有注册HOST、API29/35仪器矩阵、历史LOCAL绑定和构建摘要检查；新Q21锁与Android34测试存在，但完整统一验收注册及post-K11同次运行证明仍有缺口。

- 未闭环/待验证：统一注册新锁及Android34实际导出/实时闭环；清旧摘要，要求exit=0及runId/invocationId/source/tree/输入和实跑集合；post-K11复用完整close验收，不仅检查0失败。

- 源码依据：`.github/workflows/r31-verification.yml` Android matrix / instrumentation invocation L2,1,7,,。

- 源码依据：`.github/workflows/r31-gate.yml` gate-build / summary acceptance L5,9,,,7。

- 源码依据：`construction/STARCORE_R31_FINAL_20261104/tools/r31_gate.py` build / close acceptance L3,1,3,-。

- 源码依据：`app/src/test/java/com/ethk3/novaonepro/strategyv2/R27QualityTest.java` schema block guard / mutation negatives L1,6,4,-。

如何闭环：按本卡事实缺口及UF01,UF18,UF20修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-ENG-01 修复启动Certificate关键域指纹失配（随B02-C02同批）

历史声明：37828f5f6e639e065b63f8c4fba92a579666377e；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java`。

事实记录 1（root_completion_audit_rows.json，保留完整机器证据）：

- 已实现：CertificateRevocation.ensure经SchemaDomainCoordinator迁移owner执行；sourceTrigger定义规范化、安装前后精确检查均存在。

- 已实现：migrateR31CertificateRevocationProtection检查原critical可信状态、相关trigger delta与无关core不变，受控变更后重新stamp，未见无条件信任旁路。

- 未闭环/待验证：当前源码的新库、真实R30旧库和篡改拒绝三类启动顺序未重新执行；不能沿用历史HOST/EMU PASS宣称原机fingerprint故障已全修。

- 未闭环/待验证：整体迁移仍有UF02—04的同步DDL、严格后置条件及有界切片缺口；应区分本修复已有实现与整个启动链仍不闭合。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java` ensure / criticalSourceTriggersR31 / requireSourceTriggerDefinitionsR31 L20。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java` migrateR31CertificateRevocationProtection L535。

如何闭环：按本卡事实缺口及原卡要求修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。

### R31-ENG-02 内存准入在制品与既有研究测试相容（不加堆、不改阈值）

历史声明：187194e7d31baf78178d6428e570397b84d01d75；原验收分层：{"LOCAL": 3}。

当前结论：SOURCE_FINDINGS_REQUIRE_REPAIR_OR_REVALIDATION；当前整卡执行：NOT_COMPLETED。

实际源码：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/BoundedResearchExecutorR31.java`, `app/src/main/java/com/ethk3/novaonepro/strategyv2/SharedFeatureStoreR31.java`。

事实记录 1（runtime_completion_audit_rows.json，保留完整机器证据）：

- 已实现：MemoryBudgetR31真实区分声明工作集与实测Java/native/PSS，按owner/fence维护lease、上限/压力/取消、幂等release，不是直接增堆。

- 已实现：PreparedTask、pool helper和BoundedResearchExecutor真实申请/close lease；SharedFeatureStore有共享reader、私有backing生命周期和回收队列。

- 未闭环/待验证：本次未以原512MiB重跑11项故障类单独/全量及outstanding lease=0守卫；当前压力环境/OOM恢复不能由历史LOCAL绿推断。

- 未闭环/待验证：unscoped executor默认只申报512描述符+1工作字节，代码明确该值不是实际大载荷；需继续审计大工作调用是否都用显式owner/working estimate，未审全不能称全局内存有界。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java` t, r, y, A, c, q, u, i, r, e, :, 1, 1, 4, 起, ,,  , g, r, o, w, /, r, e, l, e, a, s, e, ,,  , r, e, c, o, v, e, r, A, g, e, d, O, p, t, i, o, n, a, l, H, e, a, p, R, 3, 1, （, 独, 立, 控, 制, ，, 不, 作, 准, 入, 绕, 过, ） 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/PreparedTaskR30R1.java` c, o, n, s, t, r, u, c, t, o, r, :, 1, 1, -, 1, 7, ,,  , i, s, T, e, r, m, i, n, a, t, e, d, /, a, w, a, i, t, T, e, r, m, i, n, a, t, i, o, n, :, 3, 1, -, 3, 9 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/BoundedResearchExecutorR31.java` p, r, e, p, a, r, e, :, 3, 6, 起, ,,  , O, w, n, e, d, T, a, s, k, ., r, u, n, /, c, a, n, c, e, l, /, r, e, l, e, a, s, e, :, 2, 8, -, 3, 3 。

- 源码依据：`app/src/main/java/com/ethk3/novaonepro/strategyv2/SharedFeatureStoreR31.java` R, e, a, d, e, r, ., c, l, o, s, e, :, 6, 5, ,,  , d, r, o, p, /, r, e, a, p, W, o, r, k, i, n, g, :, 1, 0, 8, /, 1, 1, 8, ,,  , c, l, o, s, e, :, 1, 6, 8 。

如何闭环：按本卡事实缺口及原卡要求修改实际生产者/消费者/持久恢复链；执行原验收及新增故障/集成验收。
