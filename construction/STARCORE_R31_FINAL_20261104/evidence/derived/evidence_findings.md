# R31 检测报告证据复核与参考方案纠偏

本次复核依据报告原件、字段和逐行元数据，不把参考方案中的解释当成独立证据。可运行的重建脚本为 `summarize_evidence.py`；机器结论及完整清单与本说明同目录。复核没有运行手机App或生产模型。

## 已确认的事实

| 证据ID | 原始事实 | 定位 | 解释边界 |
|---|---|---|---|
| E001 | 报告源码提交5237b265a27fe5ad27fd842901c445d5f8c27749，APK SHA256 da42eaf3f6458bbf78ef256cfbe5667e29a18a51a94a02730a1d2f46b9c05749 | source-manifest.json#/sourceCommit、#/apkSha256 | 这是报告记录身份；APK二进制未导出，无法在本包重算安装包哈希或签名 |
| E002 | 60任务中14项QUARANTINED；lane=R27的13项全部QUARANTINED | R30_REPORT.json#/chapters/3/data/tasks | 11项为领取写失败，另外1项取消、1项学生缺失；不能写成仅11项R27隔离 |
| E003 | 11项R27 blocked_reason包含F01_WRITE_FAILED及R30_PREPARED_TASK_CLAIM | 同上按task_id筛选 | 外层异常只定位领取阶段；具体内层busy/owner/fence/其它SQL错误须从完整原因链证实，不能直接断言全部由250ms写锁超时导致 |
| E004 | r27-chart-cache为InterruptedException:R27_CANCELLED；r27-matrices-r1为MATRIX_ORIGINAL_STUDENT_UNAVAILABLE；incumbent-forecast为R26_UNSLICED_COMPUTATION_CAPACITY | 同上 | 三类应分别处理取消、依赖和不可协作计算，不能统一无限重试 |
| E005 | 研究心跳STALE，progressAge=45,165,527ms（约12.55小时）；restartRequest=26、actualStart=1 | runtime-observation-heartbeatSummary.txt:1 | 这是采样时的心跳记录，不能据重启请求数推断确实重启过26次 |
| E006 | 全部713表，556空表、157非空；research_row_revision_v2 316,363行、runtime_event 134,688行、action_space 85,859行、action_label 84,000行、chart_cache 72,458行 | DIAGNOSTIC_SCOPE.json#/tables；tables_713.csv | 行数不是占用字节，也不是运行效果；空表不是可删除的充分条件 |
| E007 | 55张r30_keep_*表全为0行 | 同上筛选表名前缀 | 可结合源码判定未来无界保留风险，但不能归因为本次已产生大量keep数据 |
| E008 | 磁盘普查total=1,293,614,271字节，DATABASE=606,564,438，REPORTS=673,227,433；报告快照592,556,032字节 | STORAGE_CENSUS.txt:1 | 是某时刻目录普查，约1.294GB/1.205GiB；用户所说两天30GB需连续观测确认，这份快照不等于30GB实测 |
| E009 | 快照PRAGMA freePageBytes=0，logicalPageBytes=592,556,032 | STORAGE_CENSUS.txt:2 | 明确是IMMUTABLE_REPORT_COPY，不能当作当前live库free-list、可回收空间或磁盘余量 |
| E010 | runtime liveDatabaseFileBytes=592,556,032、liveWalFileBytes=1,582,112；存储普查后续live DB=602,959,872，MAIN_WAL=959,992 | runtime-observation.json；STORAGE_CENSUS.txt | 两个采样时刻不同，不能把差异判为伪造或相加计算总量；更不能把磁盘30GB叫Java堆内存30GB |
| E011 | 44项指标：33 INSUFFICIENT、7 RECORDED_UNVALIDATED、2 PARTIAL、1 BLOCKED、1 NOT_DEPLOYED | R30_REPORT.json#/metricResults | 无一项状态为PASS；“31项不足”不准确；UNKNOWN/NULL不能填0 |
| E012 | 96条检查全部EXECUTED，其中4条domain_outcome=PASS、2 FAIL；其余为OBSERVED、N/A、不足、WATCH等 | REPORT_DETAILS.txt中check_result逐行；checks_96.csv | allChecks=96只是声明数量；必须读取逐项结果；EXECUTED与completes_contract=true不代表软件验收通过 |
| E013 | 总结INCOMPLETE、自然质量INSUFFICIENT、physicalDevice=NOT_RUN，12章齐全 | R30_REPORT.json顶层 | physicalDevice=NOT_RUN指对应验收证据未完成，不否认报告包含实际设备运行观测 |
| E014 | 最新结算任务202611030010为COMPLETED，73条SHADOW中72已结算、1未结算 | DIAGNOSTIC_SCOPE.json#/recentEvidence/25、/24；REPORT_DETAILS RSEC-305 | 心跳仍显示202611020847是旧业务心跳/口径不一致；不能写“所有结算停在该期” |
| E015 | 结算任务共5519：72 COMPLETED/LIVE_CURRENT、36无赛前排除/LIVE_CURRENT、4958无赛前排除/BACKLOG、453 PENDING/BACKLOG | DIAGNOSTIC_SCOPE.json#/recentEvidence/25 | 不能把历史种子缺预测算成实时漏预测，也不能把72/72已存在预测的结算率当全部在线期覆盖率 |
| E016 | 重算72条已结算SHADOW命中20次，27.7778%；Wilson双侧95%区间18.7603%–39.0464% | shadow_validation.json；原预测recentEvidence/24与draws recentEvidence/0 | 不是REAL胜率，不是新策略未来证据；样本小，不能证明优势；数值匹配不等于完整历史状态可重放 |
| E017 | 5000条历史开奖，202610300771至202611030010；相邻4999间隔全60000ms，无重复，全部1..10排列且原JSON一致 | draws_validation.json，原recentEvidence/0 | 仅内部一致性，不证明开奖源真伪、独立均匀性或哈希可预测性；可用于受限PIT研究回放 |
| E018 | rawDatabaseIncluded=false；installedApkIncluded=false；r26CheckpointBodiesIncluded=false | DIAGNOSTIC_SCOPE.json及source-manifest.json | 没有原SQLite页/全状态二进制；但并非没有原开奖数据，83组选定逐行元数据已经导出 |
| E019 | writerHardViolations=579；requestToCommitP95最近96=37,373ms；fetchWriteError=PAYLOAD:WRITE_REJECTED:BUSY_DEADLINE | R30_REPORT.json#/diagnosis/findings/0/evidence/value；RSEC-305；runtime-observation.json | 579是累计窗口指标，37.373s是分析请求至事务返回指标；不能当所有写锁单次持锁时间或远端开奖抓取时延 |
| E020 | crashSummary内有3条single-cleaner FORCE STOP与1条clear data；crashCount=0、oomCount=0；另有4498ms mainStall记录 | runtime-observation-crashSummary.txt | 没记录崩溃不证明未发生；mainStall栈在vivo统计广播路径，不能直接确认为自有业务ANR根因 |
| E021 | 48个解码成员的raw_bytes/raw_sha256全部匹配，manifest正文SHA256匹配 | ARCHIVE_MANIFEST.jsonl，archive_integrity.json | 原P*.bin编码块不在ZIP，不能验证其编码字节哈希；文件完整不等于软件功能完整 |
| E022 | r26_evolution=50行；r27_policy、r27_lineage=0行；r30r1_analysis_run=25行，其中21 COMMITTED | DIAGNOSTIC_SCOPE.json#/tables | 不能说从未产生任何进化尝试；也不能将50行等同50个高品质新算法；研究有部分运行痕迹但主产物链不完整 |

## 必须修正的参考方案表述

1. “13个任务里11个隔离”应改成“13个R27任务全部隔离，其中11个为领取写失败”。全60项任务有14项隔离，额外1项为incumbent-forecast。
2. “31/44项不足”应改成33/44为INSUFFICIENT，其余状态逐项列明。图表与验收必须保持NULL/缺源/未部署语义。
3. “结算停在202611020847，比抓取落后约10小时”不能作当前全局结论。心跳与快照业务表不同步是实际问题，最新任务和SHADOW结算已到202611030010。验收应核对同一scope和采样时间，增加心跳—业务事实一致性检查。
4. “55张keep表导致本次数据库体积大”缺少本次数据依据：55表全0行。设计可致未来增长与当前增长来源是两项不同结论；逐表字节未测得，不能由行数线性推成每期0.8MB或两天30GB必由某表造成。
5. “所有算法/训练都没有跑”过强。r26_evolution有50行、r26_training_step有3047行、r30r1_analysis_run有25行、r27_molecule有64行、r30_atom_contribution有64行。可确定的是关键R27链隔离、policy/lineage缺失、独立质量证据缺失；是否每个学习器真的改权重、多少有效进步须按全状态与消费回执验证。
6. “报告无原始数据”过强。没有原数据库文件，但含5000条完整开奖元数据和73条冻结观察等选定逐行记录。模型checkpoint原字节缺失，则不能完整复原生产模型计算过程。
7. “96项检测完成”必须同时展示96项执行状态与各自业务结果。4个PASS中包含诊断Bundle/Owner契约等检查，不代表4大功能整体通过；更不能把全部EXECUTED渲染成绿灯。
8. “physicalDevice=NOT_RUN”不能被改写为没有手机运行证据。已有手机运行观测，但要求的真机验收协议、环境和收据未完成；工程自检、运行观测、自然质量验收必须分栏。
9. “哈希正常所以每期独立均匀/永远不可预测”“存在优势一定找到”“几天内必给答案”“合成无优势绝不误报”均不由此报告证明。应改成明确的统计假设、预登记实验、检出力和误报率区间，不能承诺100%质量结论。

## 现在可以复算、尚不能复算的范围

已复算：原文件完整性、表数/空表数/状态分布、96检查与44指标状态、5000开奖排列与连续性、72条SHADOW的命中标签、各自commit/deadline及history_max/target的记录级先后关系。

尚未复算：生产模型完整权重与优化器状态、每条history_hash对应的完整历史内容、APK/签名字节、原SQLite索引与实际页分布、任务错误的完整内层原因、72小时连续性能/存储曲线、OEM后台存活率、新算法真实未来质量。不得用本目录的复算结果替代这些证据。

## 施工验收需要补齐的采集

- 任务领取错误完整cause链、owner/fence/session/lease以及计算状态关联；分类为WAIT_BUSY、WAIT_DEPENDENCY、CANCELLED、STALE_FENCE、FATAL，不只保存外层字符串。
- 同一事件的单调时钟boot标识、首次观察、校验、落库提交、预测冻结、结算提交、后续研究提交时间；将进程心跳和已提交业务事实分开。
- 活库/报告快照/归档/备份/缓存/临时文件分别计字节，逐表原始数据与索引字节测量注明dbstat可用性或估算误差；采样覆盖预热、连续运行、反复导出和取消恢复。
- 每个模型的可恢复权重、优化器、随机数状态、数据游标、输入hash及独立消费回执；每个候选的出生、筛选、校准、冻结、未来验证、晋升与撤销原件。
- 报告每项状态绑定证据身份与时间范围；“健康”总标志不能掩盖研究STALE、慢分析或预测覆盖低；有结构化冲突则报告应明确CONFLICT，而不是只输出reportConsistencyConflicts=0。
