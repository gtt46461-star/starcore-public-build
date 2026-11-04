这是一份未闭环的源码预算审计。R31-B02-C02 保持 IMPLEMENTING；本目录只新增静态证据，不新增 PASS，不修改 production/tools/tests，也没有运行构建、SQLite/Java fixture、长测或设备实验。

捕获时间与每份源文件 SHA 见 source_manifest.json；精确逐字片段见 source_excerpts.json。HEAD 只用于追溯，当前工作区含其他 owner 在途修改，不能用 HEAD 或本报告冒充012最终源码/APK身份。全部计数区分 SQL语句、受影响行、字节和时间；静态最大值/下界不是设备实测。

合同：每批初始64 ops；bind字节上限 min(262144, Runtime.maxMemory()/512)，64MiB heap 时为128KiB。writer hold p99目标50ms，每次超过200ms必须有故障回执。card要求 owner 分片/staging/manifest、最终短事务发布，旧 ACTIVE 或完整新 ACTIVE 两选一；callback、触发器连带写量、进度和幂等 receipt 不能漏算。P01唯一前置是P00，P03不是新开工依赖。

| 可达 owner | 精确静态公式/缺口 | 最小施工边界 |
|---|---|---|
| CSV HistoryImport | N+1DDL，冲突另2；65行66语句；legacy submit放行DDL | P01先修startup readiness、有限片、完整import集合发布和真实appendOlder消费 |
| HoldLedgerFlush | K INSERT+DDL+PRUNE；65类67语句；512是原始normal key阈值，双critical/background overflow可增加至514 | P01独立冻结JSON/窗口、短片、同输入重试、提交后推进 |
| Timeline flush | 最多64 eligible事件×2 DML=128，锁内JSON解码/编码 | P01独立按全部成本pack，保留原时钟与payload CAS |
| Live settle | S<=160 UPDATE+distinct ordinal汇总+第一片pending | P01独立拆pending、实际成本有限结算片；不改变结算结果 |
| Qualification | callback-only ops=0但C<=256 receipt+3基础状态写=259，另条件写 | P01真实短prefix+receipt/state/cursor同CAS，不能重置曝光 |
| Pending draws | 2*P+R，P<=128，最多257语句；resolved UPDATE行数另计 | P01 bounded keyset，保留缺期与回补 |
| Atom score | 实际registry165eligible，2*165+1=331显式ops | P01原owner score-set staging+完整pointer，leafWeights及报告同版消费 |
| Molecules | 32*2=64显式ops，单幂等receipt使至少65 | P01原owner完整generation staging+最终derived发布，不能少做研究 |
| Cache rebuild | 64INSERT+1receipt至少65 | P01独立预留全部成本、同批事件直到commit resolved |
| AtomNet samples | 每期10sample，最多12期，120sample+checkpoint；100ms可能提前截断 | P01有限sample staging现在开工，最终状态/样本cursor与P03 CAS共同集成 |
| AtomNet state | in182，参数6571，单net157720B；bestNet+current net初始至少315440B；之后replay更大 | P01原artifact/staging协议现在实现，P03同版接实际restore/model publication/CAS |
| Evolution | 3*B+parent edges+cursor+条件产物，B>=22已>64；B是实际写入BORN/REJECTED，非把128出生全假定成功 | P01 EvolutionSink+Runtime原owner staging及全部实际读者，P03联合完整snapshot/cursor/RNG/exposure CAS |
| Legacy generation | 现代24member population至少72DML，再HOF、committee、cursor；锁内encode/identity解析 | P01预制身份/字节+staging协议，P03 generation状态及实际读者共同发布 |
| Foundry birth/train | 23家族现代bound identity至少69proposal DML+foundry，再central/children；训练有多member诊断 | P01出生staging和完整campaign选择，P03 candidate checkpoint/consumption同CAS |
| Source invalidation | 一个draw/history写可触发任意多受影响pin的INSERT SELECT | P01修真实revision/fence+bounded materialization+所有读者即时拒绝失效；禁止删保护触发器 |

不能把大路径全部留成“后序处理”。最小同版 staging 范围如下，都是原库、原 coordinator、原 owner，不能另建数据库/队列：

1. AtomWorld score/molecule：AtomWorldR27准备真实331/64ops结果；RuntimeR27原claim提交稳定run(input/sourceRevision/operator)+chunk_seq；SchemaR27和SchemaDomain唯一迁移owner登记专用staging/manifest/pointer；score/derived实际读者与报告查询必须按完整published run取数；RuntimeR27内存atomStates/derived只在最终确证提交后替换。只加staging表而现消费者仍读部分写入不算完成。
2. Evolution：StoreR27.EvolutionSink的policy/lineage/score/generation/ablation/admission等产物，RuntimeR27.EvolutionTask及EvolutionLoopR27.snapshot/restore共同登记同一prepared run。不能先把LIVE/policy/admission公开再提交cursor；既有读者必须过滤未发布run或读取持久完整版本。最终短事务校验全chunk/hash/source/owner/oldcursor并绑定完整population/pending/RNG/exposure snapshot。P01先实现并实测这一协议和真实owner分片；与P03完整checkpoint CAS联调后才能关闭相应债务，不把P03设成P01开工前置。
3. AtomNet：AtomNetRuntimeR30材料化、AtomNetStoreR30.dataset/checkpoint/restore、AtomNetSessionR30和ModelArtifactR29完整状态、RuntimeR27 atom publication、ModelSurfaceProvider/Store实际消费一起整合。复用现CheckpointArtifactsR26/ChunkedCheckpointR26/CheckpointManifestR26；新格式若是reference/manifest，所有旧blob直接decode读者必须同版识别。prepare时完成字节/完整性和必要受保护引用，写入预算必须含manifest/receipt/progress，最终CAS同时绑定artifact、sample/cachePeriod、through/cursor与消费证据。分片blob但先推进cursor或只保留weights均不合格。
4. Legacy/Foundry：ModelStoreSU2将encode/canonical/hash结果事务外准备；LegacyGeneration generation/status及Foundry candidate cursor/checkpoint hash原CAS保持；人口/HOF/committee/stage/matrix/candidate selector与checkpoint/diagnostics消费按完整manifest切换。出生staging可P01独立接真实producer/selector，模型恢复语义与P03联合；不能以联调需要为由现在只写inventory。

还有未完成的基础协议：ResearchWriteQueueR27现overInitialBudget只记inventory；immutable路径仅禁止显式DDL，legacy仍放行；before/after只是引用，payloadHash只记录有无callback，真实写量/可变捕获不在hash；ResearchStore自动typed progress可加两写，DatabaseCoordinator另加receipt；触发器可能增多行。须迁移真实owner并纳入短CAS guard成本，再按owner逐个启用真实写前准入；盲目对旧大batch全抛错会停研究，机械切SQL会破坏原子模型，两者均非施工完成。

原有有效实现需要保留：事务外大部分训练/预测计算、Prepared私有learner、已有源/PIT/5秒freeze guard、唯一资格/e-process消费、事务receipt和owner fence。当前ChartCache build最多17显式ops，JSON bind是否超限需要实际测量，不能静态写成已观测。AtomNet epoch receipts在work入口clear且decode不继承旧列表，本审计不支持“lifetime receipts无界”说法。

范围例外必须有调用证据：CoordinatorTxR30含DDL，但只找到示例注释/定义而未见生产new调用；legacyAtomNetStep保留为旧实现，当前atomNetStep直接dispatch AtomNetRuntimeR30，不能把旧方法当当前违规量。R29FoldBack/TableCopier为启动条件迁移路径，必须单独标真实migration window；不能用“名字含Schema/Registry”永久豁免任意业务DDL。

experiments_not_run.json定义10组必须执行的真实production实验，全部NOT_RUN，command/result/evidence均为空。没有记录本轮模型训练结果、Android WAL时延、物理设备24/72小时或自然质量证据。本目录不是验收回执；B02C02、all-runtimeDDL0、64-op/bind staging、chunk杀进程与Android并发验收均不可关闭。


补充两条同样已确认可达的静态路径，见WB18/WB19：LIVE/OBSERVE_FREEZE每policy有1冻结行、每matched model位置2写及每policy去重的artifact写，另可drain256条PIT；3个10位置model policy可形成66显式ops。更早的字节超限是2个policy各bind同一157720B模型，至少315440B，INSERT OR IGNORE并不免除bind成本。必须按实际coherent policy/model bundle切片、原deadline前最终发布，不能变造赛前时间。LIVE_CERTIFY每active实验2写，32实验64显式ops+after choice1+receipt1=66；不要求存在自然合格证书，NOT_ISSUED同样写入。证书证据可P01独立短片，但主链当前choice缓存权威必须保留到P05完成正式迁移。两项本轮均未实跑，未声称当前设备已存在相应自然策略/模型。
