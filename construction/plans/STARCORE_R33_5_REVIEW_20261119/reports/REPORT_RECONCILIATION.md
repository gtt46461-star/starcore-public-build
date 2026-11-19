# 双报告问题与复验合同

报告 A 的任务号以 `e476ca61` 开头，报告 B 以 `d8ca0632` 开头。两份报告中的源码均为 `bb7b49bbcc9266bad548ec37e71403256fc06a4d`。两层 ZIP 的 CRC 已核验，内包大小和 SHA-256 与各自 index.json 相符。两份 index 都记录 `checksComplete=true`、`allChecksPassed=false`。B 将 A 识别为可比快照，`independentPeriodsAdded=1` 的计量单位是报告快照。策略独立未来开奖期在两份报告中均为 0。

[机器可读对照表](report-baseline.json) 给出 44 个指标和 165 个对象的两份原始状态、目标文件、方法、诊断来源及各自复验入口。165 个索引对象由 82 条能力、20 条矩阵、18 条研究图、29 条既有图表及 16 张逻辑表组成，同一矩阵可出现在不同分组。每个对象以 `(group,id)` 作为键。空值表示来源未测得该值。表中的目标符号取自报告源码版本，施工时在当前提交查找真实调用者、核对迁移或重命名后实施。

| 复验对象 | 原始证据与停点 | 应执行并保存的验收 |
|---|---|---|
| 抓取与冻结结算 | A 心跳为 FETCH_STALE，provider OPEN_TIME_STALE 持续 1,997,052ms。B 抓取成功后业务为 SETTLEMENT_STALE。A/B 的 settleDetail 均为 FROZEN_OBSERVATION_MISSING、frozenAt=0。pending=161 是心跳字段中的队列观测，需查队列定义再解释。 | 同期串起源时间、首次本地可见时间、冻结提交、抓到开奖、事务提交、decisionId。测试重复与乱序返回、错过赛前、重复结算、进程重启；过期记录按原因保留，禁止用开奖后数据补造冻结。按 M03 ≤5s 与 M04 ≤1s 分别计算 P95，带完整机会分母、缺失数、起止事件和样本范围。 |
| 1 秒调度、后台与磁盘 | A 调度 missed_tick=56/targetSlots=69；B 为 14054/21678。B 电池豁免已开启，24h/72h 仍为 NOT_RUN。两个进程样本的 DB/WAL 分别为 762028032/37112 与 922091520/8388608 字节。 | 调度使用单调时钟分别记录唤醒、请求、返回和持久化。按退避、前次任务超时等原因给出缺口。以同一设备生命周期分段执行前台、息屏、后台和重启，交付 24h/72h 连续时间线；按 DB、WAL、checkpoint、报告和归档记录峰值与回收后的物理大小，验证热历史 ≤5000 及恢复可读。 |
| Foundry 与旧教师 | 两份 foundry-su2 均停在 cursor=4119、steps=408，最近阻塞为 R26_CAMPAIGN_ABI_GUARD、ROLLED_BACK。M23 均 81/82，F23 的 implemented=true、callable=false。 | 回查 RuntimeCoordinatorR26.initialize → LegacyTeacherOwnerR26.register → FamilyRegistryR26.bindLegacyTeacher；校验真实 asset 字节、注册 binding、ABI/scope 和哈希，进入 FamilyRegistryR26.build("F23") 后核验 LegacyTeacherOwnerR26.buildBoundR31 构造的学生。合法教师必须能生产和消费，缺失教师仅隔离 F23。旧父代只读保全，数值不兼容时产生独立子代身份，事务提交后重启并读回游标。给出可重演的合法和非法各一条及实际消费回执。 |
| AtomNet 与归档 | materializedSamples 3150→3250，实训行和在线行均为 0，publicationReady=false、consumable=false。A 出现 R31_GC_REGISTER_PREPARED 拒绝，B 的独立归档任务出现 R27_ATOMNET_R325_ARCHIVE_ATTEMPT 拒绝。 | 固定同一 PIT 数据段，经 AtomNetRuntimeR30.step 保存十位置同 epoch、样本、optimizer、权重变更与提交水位。重启后回读模型，再由在线输入引用该模型哈希。单个归档故障按 publicationId 保留失败和重试时间，检查其它对象能继续。 |
| 回放、演化与知识消费 | 回放 cursor=3597847、steps=3 均未前进。A 最近记录为 R26_UNSLICED_COMPUTATION_CAPACITY，B 为 R27_REPLAY_KNOWLEDGE_RECOVERY 写入拒绝；演化 cursor=13、steps=4 等待 replay。两份均记录 50 次演化尝试，已注册假设、已验证研究动作和来源消费回执均为 0。 | 对同段数据实施有界计算，在计算后、事务中、提交后三处终止并恢复。对照不中断运行的输入身份、输出哈希与逻辑游标；证明同一观测恰好消费一次。子代的父代、变异或融合算子、淘汰理由与同目标配对结果可查，区间按 Q06 核验。 |
| 图表、逻辑表与报告语义 | T02/T03/T05/T07/T11/T12/T13/T15 在两份导出中所附首页均为 0 行。M32 为 UNKNOWN_SOURCE_MISSING，报告未记录冲突数不能推出跨源一致。 | 对照每张表自己的来源、角色、过滤、分页及 watermark，分别回放有数据、真实空集、源缺失、取消和翻页。合格样本通过实际生产者进入查询，UI 与独立来源在同范围可复算；矩阵消融只作开发证据，截图和渲染成功不代替消费。 |
| 授证、实盘与质量 | 两份均 independentFutureIssues=0、futureActs=0、activeCertificates=0、policyObjects=0、qualifiedExperiments=0，futureHitRate=null。Q02=NOT_DEPLOYED。 | 先以隔离正反例连通候选→冻结→资格→证书→唯一真实决策→结算→反馈，再以独立自然未来逐期验证 Q01–Q07，按原定义交付分母、频率、配对、区间和跨块稳定性。重放/合成样本只在开发及 Q08 的对应账本；自然未达时保留原状态与下一项采集动作。 |

## 与施工总包逐项对照

方案 `evidence/original/` 内的两份外层 ZIP 与另行取得的原始报告逐字节 SHA-256 相同。逐项读取 `control/两份报告原处方与施工索引.csv` 中的原件地址，对每个对象重算 UTF-8 规范 JSON 的 SHA-256（排序键，紧凑分隔符，保留中文）。旧 S4 按原合同将 finding 与解释组成有序数组。203 项 × 两份报告共 406 条对照全部一致。另按 `control/两报告诊断字段原文索引.csv` 的 JSON Pointer 逐项读回字符串，2474 处全部一致，包括空字符串。以上核验对象为原始报告的追溯索引。

| 工作段 | 应关联原方案的主批次 |
|---|---|
| 报告、归档、热库和有界恢复 | B01 管理存储及证据，B07 消费并核验报告 |
| 调度、赛前冻结、唯一结算、每期投递、后台连续性 | B02，B08 承接真机与发布回执 |
| Foundry 的 ABI、演化与回放 | B03 |
| 十位置实训、恢复、归档后消费 | B04 |
| 矩阵、研究图、消费回执与成对研究 | B05 |
| 独立未来、有效证书、主链合法动作与反馈 | B06，与 B02 的同一决策和结算身份核对 |
| 表格、指标、对象状态与下期可复验报告 | B07，B08 承接安装与环境签收 |

## 报告源码声明复核

[165 个对象的 Java 方法、行号与工单](REPORT_METHOD_BINDINGS.json) 将每个对象接到注册的批次和工单。用 javac 语法树解析 033 原件及应用 B08 后的源码，并分别核验输入文件 SHA-256。这 165 项共引用 12 种生产入口，两个版本中均找到对应声明。82 条含 implementationClass 的能力记录，其声明类型在两个版本的编译输出中均可定位。

38 条矩阵记录里的 `<init>` 指 `MatrixSnapshotFeedbackR930.FrozenMatrix` 构造函数（两版均第 108 行）。14 条图表记录里的 `build` 指 `ChartCacheR27.build(SQLiteDatabase,RuntimeR27)`（第 57 行）。`ResearchObservatoryR30R1.step` 的入口由 033 第 77 行移到 B08 第 136 行。F23 的 `FamilyRegistryR26.bindLegacyTeacher` 确有声明（第 10 行）：它校验 ABI、scope 及绑定格式；资源读取、绑定字节与模型构造还需以上游注册者和实际 adapter 的记录证明。方法声明与类文件能定位；后续应按报告回执核验输入、调用、消费与效果。

## 每次签收的证据

记录施工提交、包 SHA、方法入口、输入夹具及哈希、实际执行命令、退出码、原始账本或导出位置。业务任务状态写 RUNNING 时，另外核验提交游标和输出身份是否发生有效变化。对反例检查隔离、去重、保全旧证据及恢复控制路径。设备与自然样本状态单列，工程测试通过后继续保留待测项。两个快照的累计量仅在共同定义、进程窗口与独立样本可比时计算变化率。

## 从解压后的方案复验原索引

运行环境为 Python 3 标准库。从本目录执行下列命令，参数指向同时包含 `control/` 与 `evidence/original/` 的方案目录。

```bash
python3 verify_dual_reports.py /path/to/STARCORE_R33_5_20261118
```

脚本校验两份封存报告的外包和内包摘要，读取原始 JSON，重算 406 条记录摘要，并从原始树枚举 2474 处字段，与 CSV 做双向全集及逐值比较。成功时退出码为 0，输出 `comparisonRows=203`、`recordDigests=406`、`fieldPointers=2474`。当前版本已实测单条摘要替换、遗漏字段、重复字段三种负例，均退出 1。脚本只读已解压方案，不附带设备原件。
