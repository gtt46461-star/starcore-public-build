# R34 三份原始检测报告取证审计

日期：2026-11-23（北京时间）。此文件是 R34 施工方案的原始证据审计附件，不是应用已修复的证明。

## 1. 核心结论

三份报告共同证明：**当前应用能抓到部分新开奖，能导出结构完整的诊断，但主要研究产出链和当前预测链没有持续推进。** 18 个研究图的当前来源产物和消费回执全为 0；20 个矩阵无可用输入/已生产产物；当前 R952 链只有 1 个 NURSERY 候选，没有训练产物、学习游标、事前预测或校准；R27 策略、自然未来机会与有效证书仍为 0。旧 R26 数据库里确有训练记录与 50 个候选，不能把“当前不推进”改写为“历史从未训练”。

主要证据链是：HTTP 请求与载荷身份不一致/写入争用 → 历史连续性不足 → 分析失败或停滞 → 新赛前冻结缺失 → 只有开奖无预测的终态增多 → 当前数值训练与独立评估没有输入。另一路为 writer-hold 故障积压 → Foundry 发布、重放数据集与进化依赖阻塞。两路可相互放大，但单凭报告不能证明所有根因的源码因果关系，必须与源码审计配对。

报告没有证明高胜率、高稳定、高品质已经实现，也没有足够新策略自然未来样本用于判断可达水平。修复运行闭环与验证策略质量必须分别验收。

## 2. 原件、版本和时间边界

| 简称 | 原始 ZIP 字节数 | SHA-256 | 内层文件数 |
|---|---:|---|---:|
| A / 870cd99 | 15,521,019 | 217bea3e9070e08dcc562be1602cd4f1c3c2aad7b9f675469955cc13b023496e | 49 |
| B / 89d1cac | 15,755,363 | a9e1b01fd620918ba3c455cb0073fe6b5f5c76358f61a8eefb1072455b01e930 | 52 |
| C / fabedd0 | 15,808,725 | 8a3eedd03b53693266f8a8744fc7b7e59b326c575d5f84c5741e4148ca26d0d2 | 52 |

A 的完整 SHA-256：`217bea3e9070e08dcc562be1602cd4f1c3c2aad7b9f675469955cc13b023496e`。三原件为用户此次上传文件；目录简称只用于本次解压审计。

- 三报告均为 `R33.5-B08-2026-11-22-577311095`，`versionCode=577311095`，`DB_VERSION=36`。
- `sourceCommit=273641e838e0b8dd842b42a67a674e184942a30a`。
- `apkSha256=0faab4a067efcd63c71bced7c4bbb8287e43f784bb636f6a0faa0dcb8e3d0b2a`。
- 报告不含原始 APK 或 raw SQLite；不能从这里直接重放整库、复算二进制模型权重或验证安装包签名。
- 来源：每份 `REPORT_DETAILS.txt` 第 2–8 行；`source-manifest.json#/sourceCommit,/apkSha256,/rawEvidenceIncluded,/omittedPayloads`。

| 观察项 | A | B | C |
|---|---|---|---|
| 运行采样北京时间 | 09:20:59.055 | 11:23:30.058 | 12:45:39.240 |
| R30 factsReadAt 北京时间 | 09:25:50.695 | 11:59:29.091 | 12:53:23.948 |
| 报告 latestIssue | 202611230080 | 202611230201 | 202611230288 |
| 核心预测/严格 OOS 最后期 | 202611230080 | 202611230119 | 202611230119 |
| 运行 pid | 3499 | 3499 | 3499 |

运行采样发生在数据库报告事实采集之前，B 两者差约 36 分钟。不得把两个时点的值强行视为同一个原子快照。来源：`runtime-observation.json#/sampleWallMs` 与 `R30_REPORT.json#/snapshotWatermark/factsReadAt`；预测水位来自 `REPORT_DETAILS.txt` 第 58/84/90 行。

**原件完整性校验：** ZIP 解压过程已校验 CRC；外层 `index.json` 记录的内层 ZIP SHA-256 与实得字节完全一致。A/B/C 的 `ARCHIVE_MANIFEST.jsonl` 共声明 48/51/51 个普通成员，逐个 raw_bytes、raw_sha256 全部匹配，0 缺失、0 不符。`ARCHIVE_MANIFEST.jsonl` 自身不在其普通成员清单内；本次额外保存其 SHA。结果见 `report_integrity.json`。这只证明收到的报告原件自洽，不证明应用业务健康。

## 3. 三时点增量对照

表中为同名表“保留的行数”，不是累计生命周期执行次数。若表存在淘汰/归档，行数不变不能独自证明完全没有新写入；本审计同时核对任务游标、发布与当前链产物。

| 对象/指标 | A | B | C | 含义 |
|---|---:|---:|---:|---|
| draws | 5,000 | 5,000 | 5,000 | 历史行数已受 5,000 限制，连续性仍不足 |
| r26_candidate | 50 | 50 | 50 | 旧研发候选存量 |
| r26_evolution | 50 | 50 | 50 | 旧进化尝试存量，不是有效提升 |
| r26_training_step | 3,453 | 3,453 | 3,453 | 旧训练行，不能与当前 R952 混算 |
| r26_statistic_training_step | 714 | 714 | 714 | 统计训练行 |
| r26_feedback | 2,958 | 3,053 | 3,114 | 部分反馈数据确有推进 |
| r26_progress | 4,097 | 4,097 | 4,097 | 研究进度存量 |
| r30_atomnet_sample | 2,150 | 2,150 | 2,150 | 材料样本数量不等于梯度训练或可消费模型 |
| r31_atomnet_publication | 19 | 19 | 19 | 发布记录存在，publishedModelRows 仍为 0 |
| r31_feedback_event / receipt | 176 / 88 | 176 / 88 | 176 / 88 | 新反馈路径无可见行数增量 |
| r27_policy / generation / lineage | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 没有正式策略/代际血缘产物 |
| r27_opportunity / certificate | 0 / 0 | 0 / 0 | 0 / 0 | 没有独立未来机会/证书 |
| r30_model_artifact / call / surface | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 模型发布、消费没有打通 |
| r31_model_publication / mainline_freeze | 0 / 0 | 0 / 0 | 0 / 0 | 主链发布、冻结未产出 |
| r31_evolution_op / publication | 0 / 0 | 0 / 0 | 0 / 0 | 没有当前算子执行发布原件 |
| r31_hypothesis_registry / improvement_registration / improvement_claim | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 研究假设和提升登记缺失 |
| r27_chart_cache | 115,831 | 116,324 | 116,463 | 图表缓存增长不代表图表有有效研究数据 |
| r31_source_event | 6,130 | 6,320 | 6,442 | 来源事件增长不代表训练和消费完成 |
| 影子预测/已结算 | 881 / 877 | 886 / 882 | 886 / 882 | A→B 仅增加 5，B→C 不再增加 |
| 延后证据待办 | 781 | 786 | 786 | 积压未收敛 |

来源：每份 `DIAGNOSTIC_SCOPE.json#/tables` 按 `table` 取 `rows`；影子计数与延后待办来自 `REPORT_DETAILS.txt` 第 84 行（RSEC-078）；完整结构化抽取在 `report_metrics.json`。

C 中 `DIAGNOSTIC_SCOPE.json#/recentEvidence` 的全量保留元数据可复算：区间 `202611230080 < issue <= 202611230288` 内实际保留开奖 156 条，同区间影子冻结只有 5 条。**不得直接把期号差 208 当作已抓到 208 条。** 5/156 也只是这个保留窗口内的记录比例，不是用户目标中的合格可出手频率或完整在线覆盖率。

## 4. 运行链实证及修复要求

### FA01：数据抓取身份错误会中断分析

A、B 的 `draw_save_error` 直接记录 `HTTP_REQUEST_PAYLOAD_IDENTITY_MISMATCH`；B 的 RSEC-052 把该错误列为 analysisError。操作为 `R914_DRAW_SAVE_ATOMIC`，`category=BUG`、`phase=BODY`、`commitOutcome=ROLLED_BACK`。这不是“网络慢”可以完整解释的问题。

- 原始定位：A/B `DIAGNOSTIC_SCOPE.json#/researchSchedulerState` 中 `key=draw_save_error`；B `REPORT_DETAILS.txt` 第 58 行。
- 施工验收必须包括同请求重复响应、重试、并发最新/历史抓取、主备源交错、响应顺序反转，逐一验证 request identity 绑定到实际 payload。
- 错误绑定应失败并保留原因，不能为了通过测试删除身份校验。

### FA02：历史条数达标但连续尾部只有 1

C 的 RSEC-052 明确为 `analysisError=java.lang.IllegalStateException:R31_DEPENDENCY_HISTORY_BOOTSTRAP_PENDING|contiguous=1`。5,000 条历史总数与可用于当前 PIT 的连续尾部长度不是同一指标。报告同时显示历史回填近期错误字段已经清空，但分析仍受该依赖阻塞，不能把字段空串认定为恢复。

- 原始定位：C `REPORT_DETAILS.txt` 第 58 行；C `DIAGNOSTIC_SCOPE.json#/researchSchedulerState` 的 `draw_save_error` 与 `draw_history_backfill_error`。
- 必须分离实时尾部与旧历史保留，明确缺口集合、连续尾部、待补页、可开始研究的输入水位。
- 需要覆盖乱序、缺页、重复、跨日期和滚动淘汰后连续性；回填只能补真实开奖，不得补造过去冻结。

### FA03：故障台账积压挡住业务写入

三个报告的 Foundry 均 `BLOCKED`，原因 `R33_WAIT_WRITER_HOLD_FAULT_BACKLOG`；重放数据集为 `DEPENDENCY(DEPENDENCY(WRITER_HOLD_FAULT_BACKLOG))`；进化再依赖重放数据集；R963_CREATION_INGEST 与 AtomNet 归档也遭 `ADMISSION/DEPENDENCY` 拒绝。须核查故障排空与普通业务共享写者是否形成等待环路。

- 原始定位：`UPGRADE_DIAGNOSIS.json#/capabilityRepairR32/operatingSummary/tasks`，三份一致。
- `foundry-su2`：cursor=4119、steps=408；`r27-replay-dataset`：cursor=3597847、steps=3；`r27-evolution`：cursor=13、steps=4；三时点不变。
- 修复需要有界排空预算、明确故障来源/重试类别、保留原件与幂等回执；不能直接清空故障记录或放宽证书门。

### FA04：心跳活着，工作水位不动

C `runtime-observation-heartbeatSummary.txt` 第 1 行：`truth=RUNNING|alive=1`，但 `business=ANALYSIS_STALE`、`research state=STALE`、`progressAge=9990128`、`attemptAge=40`。尝试不断发生，但有效研究进度约 2.78 小时没有更新。另有 `restartRequest=53/actualStart=22/generation=22/progress=0`，不能用反复重启次数作为恢复证明。

- 活性必须以目标期、提交游标、模型身份、消费回执的增量证明。
- `WAIT_NEW_DATA`、`WAIT_DEPENDENCY`、`YIELD_RESOURCE`、`RETRY_BACKOFF`、`BROKEN` 应分别展示，不能全算 RUNNING。

### FA05：一秒调度目标没有实现

`runtime-observation.json#/fetchCadenceR31` 中三个采样为：

| 指标 | A | B | C |
|---|---:|---:|---:|
| targetPeriodMs | 1,000 | 1,000 | 1,000 |
| targetSlots | 74 | 7,322 | 12,362 |
| wakes | 22 | 316 | 370 |
| missed_tick | 52 | 7,007 | 11,993 |
| NORMAL_OR_PREVIOUS_WORK_OVERRUN | 52 | 6,906 | 11,892 |
| HTTP_FAILURE_OR_429_BACKOFF | 未记录 | 101 | 101 |

报告明确该口径为当前 Service 实例单调时钟调度器，不是 HTTP 完成或设备 SLO。不能将 missed_tick 直接称为漏开多少期，但它清楚显示调度被长任务/前次超时压住。应拆开唤醒、请求开始、响应、验证、事务提交各时间，并使慢网络任务不占用节拍协调线程。

### FA06：分析耗时超标，样本数也不足

A `analysisP95=5754ms`，仅 1 次测量；B/C `analysisP95=36776ms`，仅 6 次测量，阈值 5000ms。这个失败是真实记录，但 6 次不是稳定分位数长期验收。

- 定位：各 `REPORT_DETAILS.txt` 第 58/80/84/90 行。
- 拆分排队、读输入、计算、写等待、写事务、发布耗时；真实期主链必须使用已经准备好且 PIT 合法的输入/模型。
- 每批需要有可复算的计时样本，不以最后一次变快替代 24/72 小时稳定性验收。

### FA07：没有事前预测的开奖被终态记录，不能冒充正常预测结算

C `v57_settlement_task_r36` 共 6,201 行，分布：`NO_PREDRAW_CANONICAL_TERMINAL=5157`、`COMPLETED=882`、`PENDING=161`、`RETRY_WAIT=1`。最新期 `202611230288` 的原因 `FROZEN_OBSERVATION_MISSING`。这是审计终态，不是对应期有过合法预测。最新严格 OOS 仍停在 `202611230119`。

- 定位：C `DIAGNOSTIC_SCOPE.json#/recentEvidence[table=v57_settlement_task_r36]` 全量行；`REPORT_DETAILS.txt` 第 84/90 行。
- 需给每个在线预期目标期一个事前终态：合法冻结、明确跳过、输入缺失、服务不在线/截止已过；同时统计预期机会分母。
- 补救不能把开奖结果反写成过去的冻结，也不能把 NO_PREDRAW 从故障分母删除后宣称全覆盖。

### FA08：延后证据积压与饥饿

延后证据从 781 到 786 后不再下降，C 显示 `YIELD_REALTIME`、`YIELD_ACTIVE_ANALYSIS`，最老期 `202611140228`，新期 `202611230119`。说明仅有“实时优先”而无可验证进展保障会长期饿死后续。

- 定位：C `REPORT_DETAILS.txt` 第 90 行（RSEC-305），`runtime-observation-crashSummary.txt` 第 12–41 行。
- 需要每任务/每片预算、公平调度与提交游标；实时路径失败不能永久占用优先权。

### FA09：crashCount 混合了已捕获异常

B/C `runtime-observation-crashSummary.txt` 第 1 行 `crashCount=1`，第 10–41 行为 `F01_DEFERRED_MATERIALIZE` 的 writer-hold 依赖异常。仅凭该栈**不能断言线程或进程退出**。

本次交叉阅读源码确认：`source095/app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java` 的 `writeOneInternal` 在 `catch(Throwable)` 中调用 `CrashRecorder.recordCaught` 后返回失败结果；`AutoFetchService` 延后任务外层另有 `catch(Throwable)`。因此此处应记作“捕获到的工作失败”，线程终止未证。也不能反过来由此证明不存在其他真实崩溃。

- 必须拆分 caught operation failure、uncaught Java crash、OOM、OS exit、ANR、受控重启。依赖等待不能累加为用户可见崩溃数。
- `freeMemory=0` 与这个异常不能证明 OOM；报告的 `oomCount=0`，OS 历史中有较早 low memory 退出，二者范围不同。

### FA10：后台授权与后台连续运行不是同一件事

运行授权为 `CAPABILITIES_READY_DEVICE_SOAK_NOT_VERIFIED`，`continuous24h=NOT_RUN`、`continuous72h=NOT_RUN`；电池优化豁免、精确闹钟等当前授权已具备。不能把权限状态作为退出界面后持续运行的验收。

- 定位：每份 `runtime-observation-backgroundRuntimeCapabilityR31.txt` 第 1 行。
- 需要前台/息屏/切后台/任务划走/网络切换/被系统杀死后的实际进度与恢复矩阵；强制停止后不能伪称自动运行，也不能补造离线期间的预测。

## 5. 创造—训练—评估—应用链缺口

### FA11：当前研发链只有登记，没有数值训练产物

C `r952_candidate` 唯一对象：

```text
candidate_id=R952-C-c41f6a28c10408b48b88
ast=AST1(RECENCY_BLEND,POSITION=1,LOOKBACK=120,K=3)
state=NURSERY
future_n=0
created_issue=202611230080
may_create_real=0
```

`r952_runtime_event` 仅一条 REGISTERED；`r952_candidate_funnel` 仅一条 REGISTERED。`r952_learning_cursor`、`r952_artifact`、`r952_research_outbox`、`r952_candidate_predraw`、`r958_frozen_candidate`、`r958_frozen_predraw`、训练协议/训练输入/特征游标等均为空。这能直接证实当前链没有走通；不能用旧 R26 3,453 训练行来签收这一链。

- 定位：C `DIAGNOSTIC_SCOPE.json#/recentEvidence` 上述同名表；`REPORT_DETAILS.txt` 第 16/17 行。
- 每条候选必须绑定输入身份、模型状态、更新次数、训练截止、下一步任务、重启续接原件。登记成功只是起点。

### FA12：既有家族不是全部可执行

能力表共 82 对象：81 个 callable=true，1 个 false；F23 明确 `F23_ORIGINAL_TEACHER_NOT_BOUND`。训练需求诊断把 F01/F23 关联到 `BLOCKED_WARM_NUMERICAL_ABI_PRESERVED`。声明可调用仍不是实际训练完成。

- 定位：`R30_REPORT.json#/objectIndex/capabilities/objects`；`UPGRADE_DIAGNOSIS.json#/capabilityRepairR32/capabilityNeedDiagnosticsR335/needResults` 中 `TRAINING_FAMILY_EXECUTION`。
- 必须逐家族执行输入/输出契约测试，缺教师/旧 ABI 明确禁用或迁移为新独立子代；不能把旧权重套到不兼容数值实现后沿用旧成绩。

### FA13：AtomNet 材料、训练、发布、消费脱节

AtomNet 任务标为 RUNNING，但三次 cursor=3250、steps=56 一致；operatingSummary 中 `trainingSamplesProcessed`、`completeTrainedThroughOrdinal`、`publicationReady`、`consumable` 都为 null。19 个 publication 记录不等于可消费模型：`publishedModelRows=0`，模型调用/概率面/发布表均为 0。

- 定位：`UPGRADE_DIAGNOSIS.json#/capabilityRepairR32/operatingSummary`；`R30_REPORT.json#/objectIndex/capabilities/objects[id=ATOMNET]` 的 produced=false/consumed=false。
- 需要同 publicationId 的样本输入 → 梯度更新 → optimizer/RNG/cursor → 完整十位置模型 → immutable hash → 主链冻结引用，任一段缺失不计完整发布。
- 归档任务有 6 ARCHIVED/4 CLAIMED，但报告没有孤立归档错误对象；不能仅凭归档 BLOCKED 推断其阻断所有训练。源码需确认真实依赖。

### FA14：矩阵和研究图为无产物状态

20 个矩阵全部 `inputAvailable=false`、`produced=false`；18 个研究图全部 `produced=false`、`consumed=false`、`currentSourceProducedFrames=0`、`actualSourceConsumptionReceipts=0`。这不是只有图形渲染问题，源计算产物与消费证据均未产生。

| 图 ID | 本应支持的研究 |
|---|---|
| R01 | 多尺度结构状态与可用窗口 |
| R02 | 结构预测与下一期落地误差 |
| R03 | 号码位置兼容与跨位结构 |
| R04 | 多位置围号交集与缩减证据 |
| R05 | 失败簇到假设到可执行子代 |
| R06 | 原子条件边际贡献 |
| R07 | 分子协同与冗余贡献 |
| R08 | 思路族行为差异与机制覆盖 |
| R09 | 专家处方落地与归因 |
| R10 | 变异算子增益与计算成本 |
| R11 | 父子同期配对与独立确认 |
| R12 | 融合互补与共同失败区域 |
| R13 | 置信度失准与支持域风险 |
| R14 | 梯度到概率面到策略消费 |
| R15 | 漂移、遗忘与回放价值 |
| R16 | 研究探索预算与有效产出 |
| R17 | 失败机制记忆与重复消耗 |
| R18 | 研究处方到主链效果全链 |

来源：每份 `R30_REPORT.json#/objectIndex/researchCharts/objects` 与 `.../matrices/objects`。R34 必须逐图定义生产算法、持久化产物、consumer、下一轮实际行动和同源对照。表格/图形只是这些原件的投影。

### FA15：未来资格和主链应用没有证据

`commonMetrics` 三次均为 policyObjects=0、futurePeriods=0、independentFutureIssues=0、futureActs=0、activeCertificates=0、qualifiedExperiments=0。当前正式校准研究、冻结版本、固定窗口、证书表也是 0；这不是“证书已达标但主链不用”的现有实证，而是上游还没产出可授证依据。

- 定位：`R30_REPORT.json#/chapters/0/commonMetrics`；`REPORT_DETAILS.txt` 第 16/17 行。
- 质量门需要自然未来、同机会基线、预登记选择规则、独立分母、校准和稳定性；不得以历史训练、旧影子总命中或人工把阈值降下来替代。
- 旧生产影子 C 的命中 250/882≈28.3447%，报告 Wilson 下界约25.4693%；这个对象与新候选的未来资格不是同一命名空间。它也不支持已实现用户要求的高胜率。

### FA16：进化、变异、融合、提升登记没有闭环

有 50 个旧 evolution 尝试，但 r27_generation/lineage、r31_evolution_op/publication/value、r31_improvement_registration/claim、r31_hypothesis_registry 全为 0。SU2 来源进一步报告 GENERATION/MATRICES/ATOMS/TEACHERS 证据不足，OPERATORS/REPAIR 因 `R335_EVOLUTION_CHECKPOINT_MISSING` 为 UNKNOWN，不能当“算子成功率0%”或“已完成进化”。

- 定位：`DIAGNOSTIC_SCOPE.json#/tables`；`UPGRADE_DIAGNOSIS.json#/sourceEvidenceR335`。
- 每次创造必须有父代原件、结构与行为去重、算子、随机种子、训练输入、算力成本、子代模型、同机会配对评估及保留/淘汰原因。可以不产生合格子代，但必须留下真实失败/未改善结论。
- “持续创造无上限”应指不永久封顶候选总历史；活动任务、内存、模型保留和并行数必须有预算。旧候选淘汰记录不能擦除以重置多重试验支出。

### FA17：缺失检查点与重建身份必须隔离

三份 `source-manifest.json#/r26CheckpointAuditR32` 相同：referenceRoots=657，observedObjects=677，verifiedObjects=103，missingObjects=556，byteVerificationNotRun=18，unknownChildSets=574，sourceClosureVerified=false。缺失包括什么必须逐项读取 `r32-checkpoint-observations.jsonl`，不能从 556 推定全部现役模型损坏。

- 原始模型缺失不能被空壳 checkpoint 或复制 hash 伪修复；旧证据保持隔离。
- 能以真实输入可复现重建的，必须新 run/new model identity、记录重建来源，禁止继承旧自然资格；不可复原项保持明确缺失。

## 6. 报告本身的能力和缺陷

### FA18：检测完整不等于缺陷完整或产品通过

96 检测全部执行，查询错误=0；最新 health 分布为 OBSERVED 43、NOT_APPLICABLE 16、NOT_PROVEN 20、WARN 7、PASS 4、FAIL 6。既不是 96 项全部通过，也不能把所有非 FAIL 当通过。R30 的 44 个工程/质量指标中没有 verifiedMetricIds；工程 M23 BLOCKED，其他多数 INSUFFICIENT/UNKNOWN。

`diagnosis.expectedRules=16`、actionableRules=16、actionCoverage=1.0 只表示这16条规则都有动作说明。`capabilityRepairR32` 覆盖165个索引对象，`coverageGaps=[]` 只证明索引覆盖，不证明发现了所有能力缺失。

- 定位：`REPORT_DETAILS.txt` 第 4–8 行及逐项 JSON；`R31_SEMANTIC_RECEIPT.json#/checks`；`R30_REPORT.json#/diagnosis,/evidenceAssessmentR33`。
- `reportConsistencyConflicts=0` 是既有检查器没发现它定义的冲突，不是应用所有语义完全一致的证明。

### FA19：已有升级方向，但尚未形成能自行验证的研发处方闭环

8 个 capabilityNeedDiagnostics 结果中 1 个 `EXISTING_CAPABILITY_BLOCKED`、7 个 `EXISTING_CAPABILITY_DECLARED_RUNTIME_UNVERIFIED`；`candidateCapabilityProposalsR33=[]`。检测可定位既有 owner、要求正负对照和消费回执，这比纯统计有用。但检查范围主要是声明签名与已有索引，不能据没有 proposal 就断言“没有任何欠缺能力”。

当前诊断 16 条规则的 action.currentPlan 仍引用 `STARCORE_R31_UNIFIED_FINAL_20261112` 和 UF/FR 卡号，版本为 R33.5 的运行报告存在施工指针未更新的风险。

- 定位：`UPGRADE_DIAGNOSIS.json#/capabilityRepairR32/capabilityNeedDiagnosticsR335`；`R30_REPORT.json#/diagnosis/findings/*/action`。
- R34 应把每条检测产物转成：实际故障原键 → 实际 owner → 最小复现 → 修复/实验提案 → 批次卡 → positive/negative验收 → 新报告回读。对因果仅推测的归因必须标为 HYPOTHESIS。
- 没有数据时也要给出“卡在谁、缺什么、谁负责生产、何时再试、验收怎样失败”，不能只显示无数据或 RUNNING。

### FA20：计数、生命周期和生产角色命名容易串账

同份报告有旧 R26训练、R27 formal、R952当前链、旧 V57影子、R31模型发布等多命名空间。`evolutionAttempts=50` 与 `policyObjects=0` 可以同时为真；`publishedPublicationRows=19` 与 `publishedModelRows=0` 也可同时为真；CORE=PASS 与 businessAlive=0 并不表示业务完成。

R34 报告需每个数字带 role/namespace、source、保留范围、watermark、样本单位和是否属于质量分母。日期后缀、checkpoint sequence、draw issue、report snapshot count不能互换。

### FA21：总存储仍大且缓存/审计量持续增长

运行快照主库：A 1,404,346,368 字节，B 1,410,441,216 字节，C 1,410,981,888 字节；只是主 SQLite 文件，不是 RAM，也不是应用全部磁盘。最后 raw snapshotBytes=1,411,264,512。5,000期历史并未把总库压到几百MB。

C 有794表、1,545,244行；最大行数表包括 research_row_revision_v2=617225、v57_runtime_event_r926=260793、v57_k3_action_space_1200=120480、r27_chart_cache=116463、v57_research_action_label_r811=114000、r30_writer_hold=75410。表行数不是字节占比，不能据此直接计算谁占最多空间。

- 定位：`runtime-observation.json#/liveDatabaseFileBytes,/liveWalFileBytes`；`source-manifest.json#/snapshotBytes`；`DIAGNOSTIC_SCOPE.json#/tables`。
- `STORAGE_MAINTENANCE_R82 phase=NEVER_REQUESTED`；retention为 `RETENTION_IO_RETRY:STALE_PAGE_RELEASE_CHANGED`；SU2 10个存储预算分类的 measuredBytes/At 均 null。应统一活跃库、WAL、缓存、checkpoint、备份、报告、归档的实测字节账。
- 清理需 pin/引用闭包与原件保护，不能以削表计数破坏未结算/未验收证据。可以移除可重建缓存，但须明确与不可替代学习状态不同。

### FA22：导出成功不能替代可复现材料齐全

本次三个 ZIP 都可读且内部校验通过，说明“当前报告完全无法导出”不是本次材料支持的结论。报告明确省略 installed APK、raw SQLite、模型/训练/保留二进制；source-manifest 保留了附件缺口元数据。R34 可以继续用轻量诊断做日常报告，但出现模型恢复/数值差异时必须提供受控原件取证路径，否则只能保持 UNKNOWN。

## 7. 不能从现有报告推出的结论

1. 不可保证未来达到34%、40%或任何固定胜率；更不能通过放宽证书、回填冻结、训练复用未来样本制造达标。
2. 不能说“所有模块从未执行”。旧训练和反馈确有存量与部分增量。
3. 不能把50次 evolution、19个 publication、11万 chart cache当成50个有效新算法、19个可用模型或研究图已工作。
4. 不能说该 caught 栈证明进程崩溃；也不能说本次oomCount=0证明长期无OOM。
5. 不能把3份报告当3个独立开奖样本；previousReportComparison的计数本就标为成功报告快照，不是质量样本。
6. 不能由0条PIT violation证明无泄漏，因为相关路径可能尚未执行。
7. 不能由文件齐全、声明签名存在、coverage=100%、sourceErrors=[]证明运行闭环完成。
8. 不能说所有截图都空白已复现，本次没有屏幕截图。可以证明研究图数据源和消费回执为0。
9. 不能把报告主库字节当成手机内存；需要单独RAM/PSS/RSS与磁盘指标。
10. 不能把缺失556个对象当作授权删除其他证据，或当作556个不同根因；要逐对象依赖图核对。

## 8. 对 R34 方案的验收约束

- 每个批次必须在该批已施工代码上自足验收，不能让“本批完成”依赖后面尚未实现功能。
- 工程验收先证明可执行、幂等、持续、资源有界、证据可复算；自然质量单独标注 NEED_FUTURE/INSUFFICIENT，不能让不可控制的高胜率承诺变成假完成。
- 每批一个明确 owner，关联现有源码文件/方法、需变更的契约、输入输出、负例、迁移/回滚与APK版本；禁止仅加表、仅加报告、仅改版本号。
- 先解除抓取/历史/写队列/业务调度阻塞，再打通最小真实候选的一次训练—冻结—结算—反馈，之后逐族扩展与进化；不以补几个显示数字作为图表修复。
- 自然主链策略升级必须由确有资格的同模型证书、事前冻结、单次真实决策与幂等结算闭环证明；无合格策略时输出明确拒绝原因。
- 最终真机证据必须包含当前包版本、当前源码提交、设备/Android版本、连续运行时段、任务水位增量、延迟分布、存储趋势及异常分类。无该证据只能签工程完成，不能签真机长期通过。

## 9. 附件与复核方法

- `report_metrics.json`：对三份报告的可机读抽取，含794表计数、核心对象、任务状态、原件hash、44指标、96检查状态和165对象的缺口/入口。
- `report_integrity.json`：ZIP内层与150个普通成员校验结果。
- `check_evidence/{报告简称}/RSEC-*.txt`：保留关键检查的完整 evidence 字段和原始行号，避免在超长JSON单行里查找。
- `build_report_metrics.py`：本次抽取脚本，只读原始解压报告，可重生成结构化结果，不改应用数据库。

全部事实应回到 `reports/{870cd99,89d1cac,fabedd0}/` 中原始成员或用户原始ZIP。复制到仓库时可以改根目录，但须保留报告简称到原始ZIP SHA-256 的映射；源文件内容不应被改写。
