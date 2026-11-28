# 211 检测报告覆盖、图表、矩阵与证据口径独立审计

## 1. 审计结论与保全边界

本审计以安装报告的 **source commit `5c7f1b36eaee6036df280e85835bb0fb4ce0b40d` / versionCode `577311211`** 为应用唯一基线。仓库 main `6e762807be3b36affccaeab230d44d540a16f8c8` 的旧应用源码不能用于覆盖此安装版；main 内最新 173 方案只作为继承核对材料。原件快照 SHA-256 为 `64bf0b335c6204de44da467026ce6d75b5218fbbb86bf8b5ff7cc340cce8b0a3`，快照字节数 5,060,665,344。报告未附原 SQLite/APK/训练二进制，故本轮完成的是报告取证、精确源码核对与候选显示合同验证，没有宣称在用户真机完成训练、自然资格或全库重放。

**当前软件有大量已实现的研究算法，但新运行闭环确实停滞。** 18 张研究图当前 source 的 committed frame、BORN 消费、非出生消费均为 0；20 注册矩阵当前原产物为 0；r27_policy、研究 proposal/consume 为 0。已有历史缓存、旧 R952 矩阵、算法实现和原报告必须保留，不能以清表、改成功标志或重写 UI 代替恢复合法输入、训练、生成与实际消费。

**本次报告导出本体成功不代表检测全部通过。** 96 项全登记，56 执行、40 READ_ERROR；44 指标没有完整 PASS 证据。报告只对有限已注册对象和规则形成可审计覆盖，不能保证未知产品缺陷已穷尽发现。这里把输入断链、下游无产出、显示缺项、证据不足分开，避免把一个上游原因重复计作 18 张图的 18 个新根因。

覆盖分母：82 能力行（24 族、21 思路、16 专家、20 矩阵、1 模型）+20 矩阵详细行+18 研究图+29 旧图+16 逻辑表=165 索引行；其中 20 矩阵是能力行的别名。因此 165 是索引覆盖数，不是 165 个互相独立能力。全部原对象详见 `coverage_full.json`；逐18图施工契约见 `charts_18_contracts.json`。


## 2. 核心事实和不能混用的分母

| 对象/分母 | 报告原值 | 准确解释 |
| --- | --- | --- |
| 96 检查 | 56 EXECUTED / 40 READ_ERROR | 完整登记不等于全部可读；40均有具体超时引用 |
| 67 投影来源 | 11 OK / 16 EMPTY / 40 unavailable | 与96检查不同分母，整体UNKNOWN_SOURCE_UNAVAILABLE |
| 44 指标 | 34不足、5记录未验、1 FAIL、1 BLOCKED、2 UNKNOWN类、1未部署 | 按各项scope/分母独立验收 |
| M07 | 1000ms / 73窗口 | 已超200ms目标；并非24h完整测量 |
| M23 | 81/82 | 可调用静态绑定；F23缺原教师，不是81条成功训练 |
| M31 | 1.0 / 16规则 | 行动字段齐全，不能代表诊断正确性100% |
| R26 45节 | 37 PARTIAL / 6 INSUFFICIENT / 1 OBSERVED / 1 NOT_RUN | 章节齐全与验收充分分开 |
| 研究测量原表 | 538行，其中517 INPUT_INVALIDATION | 仅其余21历史测量行；不是538研究成果 |
| 研究图 | 18 当前帧0；17有旧帧参考 | 不可用旧source替当前成功 |
| 20 canonical矩阵 | 全部retainedCount=0 | 旧r952 snapshot50/feedback48另存，必须保留 |
| 旧图 | 29注册；14缓存行存在 | 不是14幅数值有效图，更不是用户已有效8图的识别结果 |
| 逻辑表 | 8 READ_OK / 8 EMPTY_NO_ROWS | 不是8幅有效图；分页数非总分母 |
| 原子目录 | 25350定义；25220 DISCOVERED/130 REQUIRES_BINDING | 不是25350训练资产或新公式 |

## 3. 合并后的问题、原因、施工及验证

各 `CV` 项都给出证据、精确源文件、修复与正反例；`runtime_export_audit` 和 `strategy_pipeline_audit` 相同根因只需在总方案合并一次。


### CV01 96项完整登记但40项读取失败；UNKNOWN不得签为通过（P0）

类型：`CONFIRMED_OBSERVABILITY_FAILURE`。

**证据：**

- `R31_SEMANTIC_RECEIPT.json#/checks`：{"count": 96, "EXECUTED": 56, "READ_ERROR": 40}
- `R30_REPORT.json#/sourceHealthR341`：{"sourceCount": 67, "ok": 11, "empty": 16, "unavailable": 40}
- `REPORT_DETAILS.txt/check_result`：{"RSEC-009/010_budgetMs": 20000, "other_timeout_count": 38, "other_budgetMs": 1000}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/ReportCatalogCP4.java` → `export`（定位行 340）
- `app/src/main/java/com/ethk3/novaonepro/ReportReadContextCP4.java` → `runCheck / memo`
- `app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java` → `readCoverageR342`
- `app/src/main/java/com/ethk3/novaonepro/ReportCheckRegistryCP12.java` → `checks / run`

**原因与范围：**

- 注册覆盖完整与检查结果可用是两件事；67投影来源分母不能替代96检查分母。
- 共享180秒预算、单项1–20秒与失败依赖未记忆导致后续重复重算和1秒失败扇出。
- RSEC009当前scope min(e.target_issue)联表是已定位慢链之一；RSEC010 renderer hash、RSEC018 acceptanceWindow等栈独立，禁止把40项全归咎同一SQL。
- 每次runCheck创建自己的取消signal并finally清理；没有证据支持沿用旧signal为根因。

**施工：**

1. 按真实栈分别优化输入范围、必要索引/查询计划与任务级成功/失败依赖记忆；保持取消错误独立。
2. 为可恢复来源失败保留明确引用与原错误；不能捕获失败返回0/空集。
3. 保留96检查、原导出成员与完成/失败语义；执行预算整改不砍检查。

**验证：**

- 固定211快照fixture分别触发009、010、018，比较查询计划/读取量/耗时。
- 同依赖故障仅执行一次但各依赖检查正确UNKNOWN；下一次新报告重试。
- 取消、源错表、单检查timeout后其余合理预算内运行；导出仍可取消/重做。

**必须保留：** 全部96注册检查；原完整ZIP导出及取消协议；每检查局部signal与错误引用。


### CV02 44指标没有完整通过依据；0样本、静态覆盖与行动覆盖不得冒充验收（P0）

类型：`ACCEPTANCE_EVIDENCE_GAP`。

**证据：**

- `R30_REPORT.json#/metricResults`：{"INSUFFICIENT": 34, "RECORDED_UNVALIDATED": 5, "FAIL": 1, "BLOCKED": 1, "UNKNOWN_SOURCE_MISSING": 1, "NOT_DEPLOYED": 1, "UNKNOWN_STALE_SYNTHETIC_SOURCE": 1}
- `R30_REPORT.json#/metricResults/6`：{"id": "M07", "value": 1000.0, "sampleCount": 73, "target": "P95≤200ms；24h失败0"}
- `R30_REPORT.json#/metricResults/22`：{"id": "M23", "numerator": 81, "denominator": 82}
- `R30_REPORT.json#/metricResults/30`：{"id": "M31", "value": 1.0, "sampleCount": 16}
- `R26_REPORT.json#/sections`：{"PARTIAL": 37, "INSUFFICIENT": 6, "OBSERVED": 1, "NOT_RUN": 1}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricDefinitionsR29.java` → `currentR30 / r31Definitions`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR29.java` → `document / fromSnapshotR30`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java` → `build`

**原因与范围：**

- 34 INSUFFICIENT、5 RECORDED_UNVALIDATED及其余失败/未知状态必须分别保留。M07是已捕获73窗口的最大wait_p95，尚不是完整24h验收。
- M06/M22/M27/Q07均0样本的0；M23 81/82只表示可调用绑定；M31 16规则行动字段齐全只表示路由覆盖。
- M01安装身份VERIFIED_INSTALLATION与完整构建签名验收不足并不矛盾；M17本次导出成功与40检查失败也不矛盾。
- R26 45节全部存在，但37 PARTIAL、6 INSUFFICIENT、1 OBSERVED、1 NOT_RUN；UPGRADE只有1主finding及16规则不是未知软件缺陷穷尽检查。

**施工：**

1. 采用coverage_full.json每项独立cause/action/test，按当前版本、角色、时段、分母采集。
2. M07先修真正实时写瓶颈，其余缺测项按场景补证；不要把34缺测项等价为34个互不相关新根因。
3. 保留target与缺证状态，任何自然质量未达标继续真实标不足。

**验证：**

- 所有44 ID唯一且有状态、来源、修复、验收；无缺项/重复。
- 用0样本、混角色、旧source、同对象重复渲染等反例验证不得PASS。
- 修复后证据只能按当前source与指定范围升级，不用成功图表替代质量。

**必须保留：** 原44指标定义和门槛；UNKNOWN/NOT_DEPLOYED/RECORDED_UNVALIDATED分离；原失败窗口及负结果。


### CV03 静态82能力已实现不等于实训；F23原教师绑定真实缺失（P1）

类型：`INTERFACE_GAP_AND_RUNTIME_SCOPE_LIMIT`。

**证据：**

- `R30_REPORT.json#/objectIndex/capabilities`：{"count": 82, "FAMILY": 24, "IDEA": 21, "EXPERT": 16, "MATRIX": 20, "MODEL": 1, "callable": 81}
- `R30_REPORT.json#/objectIndex/capabilities/objects/22`：{"id": "F23", "callable": false, "executionBlockedReason": "F23_ORIGINAL_TEACHER_NOT_BOUND"}
- `DIAGNOSTIC_SCOPE.json#/tables/419`：{"table": "research_atom_spec_v2", "rows": 25350, "DISCOVERED": 25220, "REQUIRES_BINDING": 130}
- `DIAGNOSTIC_SCOPE.json#/tables/120`：{"table": "r26_task", "rows": 64, "BLOCKED": 45, "NEEDS_FIX": 2, "READY": 11, "RUNNING": 4, "YIELDED": 2}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java` → `build`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java` → `step`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java` → `build`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/CapabilityNeedDiagnosticsR335.java` → `capture`

**原因与范围：**

- F23原teacher未绑定是具体可调用缺口，其余81只有当前报告进程静态入口存在。
- 原子catalog25350是定义/发现；不能算25350训练原子、有效策略或新公式。
- 保留候选状态BLOCKED_WARM_NUMERICAL_ABI_PRESERVED说明旧数值ABI断链；不能用可调用=true绕过旧权重身份。

**施工：**

1. 按原教师hash/角色/ABI接入F23；教师缺失保持BLOCKED，不虚拟教师。
2. 先修输入/ABI后沿原候选生成独立子代，分别记录参数变化、游标提交、产物、消费、独立效果。
3. 不新增空壳族凑覆盖，不覆盖改写旧父代身份。

**验证：**

- F23合法原教师正例；无teacher/错source/错ABI反例。
- 全部82项各有静态与运行分层结果；原子定义数不得替代绑定/回放数。

**必须保留：** 24族/21思路/16专家/20矩阵/AtomNet既有实现；旧数值权重和原ABI封存；真实拒绝码。


### CV04 18研究图当前来源产物全零；先恢复合法数据，再分producer验证（P0）

类型：`CONFIRMED_DEPENDENCY_BLOCK_WITH_MULTIPLE_OWNERS`。

**证据：**

- `R30_REPORT.json#/objectIndex/researchCharts/objects`：{"count": 18, "each_currentSourceProducedFrames": 0, "each_actualSourceConsumptionReceipts": 0, "each_actualSourceNonBornActionReceipts": 0, "historical_latest_view_references": 17}
- `R30_REPORT.json#/objectIndex/researchCharts/objects/*/verifiedFaultTraceR33`：{"shared_owner": "r27-observatory-r1", "state": "BLOCKED", "reason": "DEPENDENCY:DEPENDENCY(DEPENDENCY(IMMUTABLE_REPLAY_DATASET))", "scope": "SHARED_OWNER_TASK_NOT_OBJECT_ROOT_CAUSE"}
- `DIAGNOSTIC_SCOPE.json#/tables/252`：{"research_measure": 538, "INPUT_INVALIDATION": 517, "other_measure_rows": 21}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java` → `step`（定位行 232,244,248,269）
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputContextR33.java` → `acquire`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStudyTaskR30R1.java` → `step`（定位行 41）
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java` → `paired`
- `app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchRepositoryR30R1.java` → `readFrame`（定位行 68,99）

**原因与范围：**

- observatory在evoData==null直接等待IMMUTABLE_REPLAY_DATASET，因此17图及其产物链无法推进；具体输入失效/恢复原因应与策略链原owner证据合并。
- R06由ResearchStudyTask写，不在observatory循环；共享owner阻断只能作线索，不能冒充R06独立故障证据。
- 538测量行中517是输入失效事件；旧17图参考和28缓存行不能证明当前source产出。
- 现代码已有真正结构/配对/协同/融合/漂移/模型对照等实现，不能以0运行成果推断全部算法不存在。

**施工：**

1. 先恢复有效不可变dataset/pin/revision与原重放任务，保留输入/角色/ABI/hash验证。
2. 按charts_18_contracts.json逐图producer、原表、consumer及反例施工；R06单独验证切片/恢复。
3. R11/R14/R18为FORMAL_READ_ONLY，优化消费标N/A；不能强制他们有BORN回执。
4. 可为只读formal/预算图单列独立输入诊断，但新增依赖合同需精确来源；不得绕过合法原件生成假点。

**验证：**

- 从原dataset建链后逐18图当前source frame/run/hash有独立原件，负值/不足/缺来源正确保留。
- 输入revision变化和错ABI拒绝旧帧，已成功同identity不重复计算，下一新水位能推进。
- R06合法原子组件配对续跑，无法仅凭observatoryREADY签收。

**必须保留：** 原17历史帧及其HISTORICAL_ABI标识；原算法/固定块/角色隔离；每视图有界查询/错误隔离。


### CV05 研究建议到真实动作回执断链，不能靠显示或计数补完成（P0）

类型：`DOWNSTREAM_EXECUTION_EVIDENCE_ABSENT`。

**证据：**

- `DIAGNOSTIC_SCOPE.json#/tables/251`：{"table": "r30r1_proposal", "rows": 0}
- `DIAGNOSTIC_SCOPE.json#/tables/250`：{"table": "r30r1_consume", "rows": 0}
- `DIAGNOSTIC_SCOPE.json#/tables/332`：{"table": "r33_research_input_head", "rows": 0}
- `DIAGNOSTIC_SCOPE.json#/tables/331`：{"table": "r33_research_applied", "rows": 0}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java` → `read / stageBoundR31 / stage`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java` → `stageResearchHypotheses`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchConsumptionStoreR31.java` → `completeWithin`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchResultContractR31.java` → `consumptionKindOf / validateForConsumption`

**原因与范围：**

- 当前先缺有效研究输入/原产物，proposal和consume也为0；这些是上游停工的下游症状，不再单列18个新根因。
- 非BORN预算/剪枝/窗口调整与BORN新子代需要不同原回执；0不等于消费绑定代码全不存在。

**施工：**

1. 用原frame/run/contractHash/source/revision/role精确消费；出生与原childPolicy同事务完成回执。
2. 非出生动作须有真实before/after和下一轮行为影响；FORMAL只读来源不得作优化输入。
3. 对没有改善的合法动作如实记录负结果，不通过重试多记产量。

**验证：**

- 实际建议改变合法子代或有界预算后回读same-source原件；重复执行幂等。
- 错hash/错role/错parent/无实际变化/孤立frame均拒绝；CONFIRM/FUTURE不得回流训练。

**必须保留：** 原主链唯一owner；真实事务消费与反泄漏门禁；BORN/non-BORN分账。


### CV06 20注册矩阵未产出；既有R952与长档案不得被删或混计（P1）

类型：`DOWNSTREAM_TYPED_MATRIX_DEPENDENCY`。

**证据：**

- `R30_REPORT.json#/objectIndex/matrices/objects`：{"count": 20, "each_retainedCount": 0, "each_inputAvailable": false, "each_produced": false}
- `DIAGNOSTIC_SCOPE.json#/tables/368`：{"r952_matrix_snapshot": 50}
- `DIAGNOSTIC_SCOPE.json#/tables/367`：{"r952_matrix_feedback": 48}
- `DIAGNOSTIC_SCOPE.json#/tables/501`：{"research_matrix_checkpoint_v2": 49}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java` → `step / produceR31 / build`（定位行 12,42,99,142）
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryR30.java` → `all / Definition.json`
- `app/src/main/java/com/ethk3/novaonepro/MatrixSnapshotFeedbackR930.java` → `settle`

**原因与范围：**

- 20 canonical typed矩阵与旧R952在线矩阵是不同来源/合同，不能宣称所有矩阵为空或用R952数量填20类型通过。
- MatrixResearch.step也在evoData缺失时等待；student依赖和source-only类型不同，需按类型分解。

**施工：**

1. 先恢复合法dataset，再逐20类型按dependency/维度/source/revision生产；有学生需求的类型保持明确等待。
2. 保留R95250份冻结/48份反馈及49checkpoint，不能清表重建。
3. 过期原输入缺失应记录EXPIRED/WITHOUT_PROJECTION，不能事后造历史矩阵。

**验证：**

- 20类型逐一正确维度和原键，source-only不错误等学生；学生型无学生明确不足。
- 错角色/目标/ABI/维度/重复提交拒绝或幂等，旧在线矩阵回归保持。

**必须保留：** R952旧在线原逻辑与反馈；20矩阵完整注册；历史矩阵checkpoint及过期原因。


### CV07 历史图“有缓存行”不等于有真实可用图；既有8图应保全（P1）

类型：`DISPLAY_EVIDENCE_SEMANTICS_LIMIT`。

**证据：**

- `R30_REPORT.json#/objectIndex/legacyCharts`：{"registered": 29, "produced_true_cache_present": 14, "other_produced_unknown": 15}
- `R30_REPORT.json#/objectIndex/researchCharts`：{"historical_references": 17, "current_source_frames_all": 0}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java` → `build legacyCharts`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java` → `panel / build`
- `app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java` → `cached / snapshotPanelR30`（定位行 88,116,179）
- `app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchRepositoryR30R1.java` → `readFrame / panel`（定位行 68,139）

**原因与范围：**

- legacy produced=stored!=null是缓存存在语义；ChartCache可以写points为空且INSUFFICIENT的panel。14缓存记录不等于14个有效可视图。
- 报告不能独立确定用户肉眼可用8图的具体id；16逻辑表中8可读也不是这8图。
- 现UI已有hash/size/schema/source校验、历史标识和单图故障隔离，重写会破坏有效保全。

**施工：**

1. 先在真机逐页捕获既有8图id/样式/数据源/交互作为回归基线。
2. 报告侧必要时增量区分cachePresent/payloadValid/finitePointCount/currentSource/rendered，不替换现图注册布局。
3. 历史帧继续显示带来源标识；新图只有真实原件时画点。

**验证：**

- 全部29旧图入口保留；已有效8图截图/翻页/点击无退化。
- 坏hash/超限/空点/历史source分别独立状态，一图坏不能拖垮整页或报告。

**必须保留：** 旧图id和布局；已有8图真实交互与数据；HISTORICAL_FRAME/HISTORICAL_ABI与原hash验证。


### CV08 R18成功结算仍无条件显示REAL缺证的投影缺陷（P1）

类型：`SOURCE_CONFIRMED_LATENT_PROJECTION_DEFECT`。

**证据：**

- `R30_REPORT.json#/objectIndex/researchCharts/objects/17`：{"id": "R18", "currentSourceProducedFrames": 0, "current_real_success_path_observed": false}
- `source:ResearchProvenanceGraphR30R1.java:76`：{"unconditional_node": "REAL缺证", "read_attributedRealSettlement": false}
- `source:ResearchObservatoryR30R1.java:121`：{"row_fields": ["realAuthorization", "attributedRealSettlement", "attributionReason"]}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchProvenanceGraphR30R1.java` → `project`（定位行 76）
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java` → `stored(R18)`（定位行 103,121）
- `app/src/main/java/com/ethk3/novaonepro/ResearchMainlineContributionAuditR922.java` → `exactRealAttributionR34`（定位行 95,133）

**原因与范围：**

- 上游精确SQL已读取policy/model/authorization/trade及权威终态，显示层却无条件添加REAL缺证，没有任何成功边。
- 这是可达成功路径的潜在显示错误，当前没有成功原件，不能解释为何当前未产出/未训练。
- nonnull receipt含待结算和终态未知；不能简单判断nonnull就展示成功。

**施工：**

1. 应用R18_exact_receipt_projection.candidate.patch前以5c7f基线复核，仅更改project显示。
2. 保持exactRealAttributionR34原SQL、赛前时钟及终态hash验证；只按匹配原键呈现SETTLED_EXACT/WAITING/UNKNOWN/MISSING。
3. 成功与亏损都是真实结算；单次成功不代表自然质量达标。

**验证：**

- git apply --check在5c7f通过；精确候选helper源Java运行12个正负合同用例通过。
- 施工还需集成Android/SQLite真实原件测试：合法成功、缺证、错授权/错实验、待结算、终态未知、不合法时间。
- 图表只读，无新授权/结算写入；旧图和导出不变。

**必须保留：** 原exactRealAttributionR34安全校验；唯一DecisionGate/SettlementEngine授权；formalReadOnly。


### CV09 R10/R17每对象CPU原件缺失；不能以全池成本代替（P2）

类型：`FEATURE_EVIDENCE_GAP_NOT_CURRENT_STALL_ROOT`。

**证据：**

- `source:ResearchObservatoryR30R1.java:81`：{"R10": "perObjectCpuNs=null", "costScope": "AST_NODE_COMPLEXITY_ONLY_CPU_UNKNOWN_NOT_POOL_CPU"}
- `source:ResearchObservatoryR30R1.java:83`：{"R17": "perObjectCpuNs=null", "costReason": "NO_OBJECT_CPU_RECEIPT_WHOLE_POOL_CPU_NOT_SUBSTITUTED"}
- `source:ResearchObservatoryR30R1.java:262-264`：{"frame_cost": "System.nanoTime elapsed; frame production, not child operator CPU"}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java` → `stored(R10/R17) / step`（定位行 81,83,262）
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java` → `researchCosts`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ImprovementStoreR31.java` → `appendFreezeCost / outerReceipt`

**原因与范围：**

- 现显示准确承认对象CPU未知，已有AST复杂度/全任务CPU但它们的范围不等于每个算子真实CPU。
- 原自然父子观察器r31_improvement_cost有独立calling-thread CPU合同；不可拿来冒充SELECT算子成本。

**施工：**

1. 在真实算子执行边界增量记录operation/parent/child/source/input/slice及calling-thread CPU，与wall等待分离。
2. slice累加只算已提交独立工作，重试幂等；失败成本保留。
3. 收集完成前继续展示AST复杂度并标CPU_UNKNOWN，先修P0链路。

**验证：**

- CPU忙计算与故意阻塞sleep区分；多个对象并发不得拿整池CPU均摊。
- 重试/切片/失败各自原件可复算；无原件不做收益/CPU排序承诺。

**必须保留：** 原AST复杂度与配对增益；25%探索下限；原自然观察器成本独立口径。


### CV10 自然质量无通过依据，旧源小合成不代表当前完整工厂检出力（P0）

类型：`QUALITY_VALIDATION_NOT_COMPLETED`。

**证据：**

- `R30_REPORT.json#/metricResults/36`：{"Q01": "INSUFFICIENT"}
- `R30_REPORT.json#/metricResults/37`：{"Q02": "NOT_DEPLOYED"}
- `R30_REPORT.json#/metricResults/42`：{"Q07": 0, "sampleCount": 0}
- `R30_REPORT.json#/metricResults/43/observations/factoryCapabilityExperiment/payload`：{"Q08": "UNKNOWN_STALE_SYNTHETIC_SOURCE", "oldSource": "ab2695b1b7e764fe4fd057183d2007f35a720a63", "plannedTrials": 120, "completedTrials": 0, "scheduler": "WAIT_REALTIME_WINDOW"}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FactoryCapabilityStoreR31.java` → `advance / advanceIsolated`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FactoryCapabilityExperimentR31.java` → `protocol / summary`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/SequentialQualificationR27.java` → `qualification`
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchResultContractR31.java` → `effectiveness`

**原因与范围：**

- 有效证书/REAL缺失与工程闭环未完成相符，不能靠图表数量或训练次数推出可用优势。
- 完整工厂试验当前未完成，来源旧；旧19/20或20/20小合成只属各自实验范围。
- REGISTRY_READ_FAILED/UNKNOWN_REGISTRY_UNAVAILABLE_OR_CORRUPT不能当作已无风险。

**施工：**

1. 先完成工程同池正负对照，再按当前施工source登记原完整工厂六臂×20种子=120轮（NULL_UNIFORM/NULL_TIME_SHUFFLED/SIGNAL/SIGNAL_DISABLED/DRIFT/GAPS）与功效区间。
2. 保持自然FUTURE与合成完全分账，继续独立未来确认和正确预算。
3. Q01–Q08逐项按原规则报告真实不足，禁止补配额/选择好窗口。

**验证：**

- 错source、未完成trial、单轮高命中、未登记假设不得签收。
- 原六臂完整工厂的误授证/检出力与额外34/37/40%目标概率对照分别带CI、全分母；原signalStrength=.30是植入参数，不能当命中率；自然资格独立。

**必须保留：** 全部自然质量门槛；合成与自然来源隔离；失败轮与负结果。


### CV11 173旧方案三项读层修复已进入211，不能重新覆盖回退（P0）

类型：`PRESERVATION_AND_BASELINE_GUARD`。

**证据：**

- `source-manifest.json#/sourceCommit`："5c7f1b36eaee6036df280e85835bb0fb4ce0b40d"
- `repo-main`："6e762807be3b36affccaeab230d44d540a16f8c8"
- `DIAGNOSTIC_SCOPE.json#/tables/331`："r33_research_applied=0, no longer null"
- `DIAGNOSTIC_SCOPE.json#/tables/332`："r33_research_input_head=0, no longer null"
- `R30_REPORT.json#/objectIndex/boundedPayloads`：{"chartObjectIndex": "14/14", "repairFrameIndex": "17/17", "omitted": 0}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java` → `TABLES / capture`（定位行 18,337,358）
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportInlineBudgetR343.java` → `<init> / take`

**原因与范围：**

- main旧app源不能直接作211应用施工底座；当前计划173仍需逐项继承但必须重新核current code。
- R343-035白名单已修，036元数据预算已隔离，037已用原committed cache/run计数；当前零产物是运行问题，不是这三处旧读层bug继续存在。
- 有界payload摘要有意少量展示；researchFrames2/28、runs4/30、measure1/538不得当原表只有2/4/1。

**施工：**

1. 只在5c7f精确底座应用最小补丁；给旧项记录IMPLEMENTED/REVERIFY_RUNTIME，不再复制旧全文件。
2. 保留完整成员和索引专额；进一步优化慢源不删除检查/附件。
3. 通过现原件检索确认大payload总分母和省略状态，原ZIP本体不改。

**验证：**

- 施工前base commit/file hash验证；错误base拒绝应用。
- 035明确0不回null；036各元数据索引全覆盖；037R06/formal current frame计数回归。
- 升级导出可取消/成功/重做；原报告格式、成员及hash验证保留。

**必须保留：** 211已成功的035/036/037修复；原已可导出的报告；173其余有效条目和8旧图。


### CV12 16逻辑表的空集和可读不能错算成图表成功或底层全部缺失（P1）

类型：`TABLE_READING_SCOPE_AND_ACCEPTANCE`。

**证据：**

- `R30_REPORT.json#/researchTables`：{"READ_OK": 8, "EMPTY_NO_ROWS": 8, "counts": {"T01": 30, "T02": 0, "T03": 0, "T04": 3, "T05": 0, "T06": 1, "T07": 0, "T08": 24, "T09": 4, "T10": 1, "T11": 0, "T12": 0, "T13": 0, "T14": 20, "T15": 0, "T16": 3376}}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAdaptersR30R1.java` → `query / readPageR31`（定位行 6,42,145）
- `app/src/main/java/com/ethk3/novaonepro/ResearchTablesActivityR31.java` → `asynchronous page load`

**原因与范围：**

- T01=30历史analysis run；T08=24专家提案；T16=3376解释均不等于当前有效资产。
- T02/T03/T05/T07/T11/T12/T13/T15为明示空集，不必重写查询去制造数据。
- 现页面/总数同SQL、取消、分页与跨请求水位说明已存在；未知来源/超限不得降级EMPTY。

**施工：**

1. 按每逻辑表对应的真实生产者恢复源，保留查询/分页/错误处理。
2. 空表从source/role/filter/读时点定位；不做无差别清表或补零数据。

**验证：**

- 16表当前与新source总分母/翻页一致；table缺失、取消、超限、源变化各给不同状态。
- T15真实proposal/consume联表正例与悬空提案反例；T16有解释无REAL不计闭环。

**必须保留：** 原16逻辑查询与同SQL分母；有界分页/取消/来源版本提示；历史原件。


### CV13 R11只画verified布尔值，原父子配对收益和区间未投影（P2）

类型：`SOURCE_CONFIRMED_DISPLAY_CONTENT_GAP`。

**证据：**

- `source:ResearchObservatoryR30R1.java:85`：{"point.value": "verified 0/1", "numeric_result": "stored in row.result, not point lower/upper"}
- `source:FrozenConfirmationR30.java:113-132`：{"original_fields": ["pairedLcbVsPrevious", "pairedLcbVsI00", "pairedLcbVsNull", "pairedImprovementR31"]}
- `source:ResearchWorkbenchRepositoryR30R1.java:145-153`：{"consumed_fields": ["value", "lower", "upper", "n", "details"], "row.result_displayed": false}

**源码：**

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java` → `stored(R11)`（定位行 84,85）
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationR30.java` → `evaluate`（定位行 113,132）
- `app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchRepositoryR30R1.java` → `panel`（定位行 145,153）
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ImprovementStoreR31.java` → `outerReceipt`

**原因与范围：**

- 现算法已做配对确认，问题是展示contract丢了数值与原结果详情，不是没有配对算法。
- R11登记为LINE/父子同期配对收益与确认，显示只有verified0/1会掩盖负贡献幅度和不确定性。
- 历史CONFIRM配对与自然未来改善有不同资格范围；二者不得混为自然有效。

**施工：**

1. 从同原result_json精确投影paired收益/已保存区间与父子hash、日历范围、证据角色；缺少字段保持UNKNOWN。
2. 保留verified作为独立状态；不把布尔1作为收益，不重跑已使用holdout，不向训练回流。
3. 详情包含原结果hash/实验id，必要增量保留独立自然pair claim入口。

**验证：**

- 原结果正/负/跨零/无父代四例显示相应真实数值和未知；verified不覆盖收益。
- 旧无区间结果不虚构上下界；历史CONFIRM不得显示NATURAL_FUTURE通过。

**必须保留：** 原FrozenConfirmation算法与所有负结果；只读CONFIRM合同；原自然父子观察器分账。

## 4. 18 张研究图的真实生产与消费路径

共同已证实状态：18 图 `currentSourceProducedFrames=0`，BORN/non-BORN 当前消费均为 0。除 R06 外，生产主入口是 `ResearchObservatoryR30R1.step`；R06 使用 `ResearchStudyTaskR30R1.step`。三张正式图 R11/R14/R18 是 `FORMAL_READ_ONLY`，不得拿正式确认/未来结果训练，也不得要求每图都有 BORN 消费。所有图数据恢复后继续经 `ResearchWorkbenchRepositoryR30R1.readFrame` 原 hash/contract/来源校验，旧帧保留明确历史标识。

表中“consumer”是静态设计路由；真实执行必须用原消费回执确认，不由类名推断成功。
| 图 | 现存生产算法/输入 | 原消费者/角色 | 最小施工与验收 |
| --- | --- | --- | --- |
| R01 多尺度结构状态与可用窗口 | StructureStudyR30R1.compute；固定多窗口/十位置合法SELECT数据；预先登记结构目标 | StructureGeneratorRegistryR30 | 先恢复同源不可变replay数据；保留原结构统计和固定窗口；验收：同输入复算数值/支持期相同；未来标签扰动不得改过去结构 |
| R02 结构预测与下一期落地误差 | StructureStudyR30R1.compute；TRAIN拟合、SELECT下一期真实标签、固定64期块 | FailureProfileR30 | 恢复结构预测原件与下一期匹配，不用预测自身当实际值；验收：预测/实际逐目标对应；乱序/错期与不足整块不能补齐 |
| R03 号码位置兼容与跨位结构 | ResearchObservatoryR30R1.compute(R03)；NUMBER_POSITION_COMPATIBILITY/CROSS_POSITION_FLOW矩阵；号码位置维度与滞后窗口 | IdeaI04CrossPositionR27 | 恢复来源专属矩阵及原measurement，保留关联而非因果口径；验收：P1-P10维度/置换一致；同一期未来信息或错维度拒绝 |
| R04 多位置围号交集与缩减证据 | ResearchObservatoryR30R1.compute(R04)；短32/长128窗、各位置K3集合、同SELECT目标 | SetIntersectionPolicyR30 | 恢复原交集/不足3弃权，不为展示凑足K3；验收：确切集合与命中可复算；<3时弃权、随机基线0.30分列 |
| R05 失败簇→假设→可执行子代 | ResearchObservatoryR30R1.compute(R05)；真实失败父代、FailureProfile、合法windowed leaf与子代AST | ExpertProposalsR27 | 先真实出生与回放，再输出具体leaf-window修改假设；原谱系保留；验收：childHash确为AST变化；文本处方/同hash不能计产出；不宣称全新数学族 |
| R06 原子条件边际贡献 | ResearchStudyTaskR30R1.step / ResearchStoreR30R1.paired；原父代、dataset/revision；合法组件路径与配对回放 | MutationOpsR27 | 单独追原研究study owner；保留8父代轮转/256行切片及续跑；验收：原子有/无同目标配对；RETRAIN_REMOVED与MASK_FROZEN分开，不以共享observatory说明根因 |
| R07 分子协同与冗余贡献 | ResearchObservatoryR30R1.compute(R07)；同父代none/A/B/A+B四臂；TRAIN-only拟合与SELECT | DerivedAtomR27 | 恢复四臂共同数据和分块增益，不拼异期收益；验收：协同=L_A+L_B-L_AB-L_none复算；缺任何臂保持不足 |
| R08 思路族行为差异与机制覆盖 | ResearchObservatoryR30R1.compute(R08)；BirthGate固定TRAIN探针行为签名 | BirthGateR27 | 恢复真实族候选与固定probe，不读CONFIRM/FUTURE来去重；验收：签名/Jaccard确定；改将来标签不改变TRAIN签名；不同名字同机制可被发现 |
| R09 专家处方落地与归因 | ResearchObservatoryR30R1.stored(R09)；expert proposal→真实child→lineage父代，同dataset | ExpertProposalsR27 | 恢复处方执行子代链，不把24提案当24成功策略；验收：真实外键配对proper loss；悬空提案/不同父代无法获得收益归因 |
| R10 变异算子增益与计算成本 | ResearchObservatoryR30R1.stored(R10)；原父子同目标SELECT、operator、AST nodeCost | EvolutionLoopR27 | 保留配对增益；实际逐对象CPU目前缺省，应追加同操作原计时后才展示CPU收益比；验收：父子/损失配对复算；没有对象CPU则UNKNOWN，禁止用全池CPU顶替 |
| R11 父子同期配对收益与确认 | ResearchObservatoryR30R1.stored(R11)；r30_frozen_confirm 原任务/注册实验/result_json | FrozenConfirmationR30（只读展示）；FORMAL_READ_ONLY | 先恢复独立确认生产；只读呈现原结果与证据；验收：同任务/独立日历可复算；不要求BORN消费，不把确认反馈用于再训练 |
| R12 融合互补与共同失败区域 | ResearchObservatoryR30R1.compute(R12)；两原父代同目标残差相关/共同失败与独立融合子代 | FusionOpsR27 | 恢复合法父代和真实融合AST，保留原互补度计算；验收：两个父代/融合均完整；相关不足、相同父代或复制证书不能成为有效融合 |
| R13 置信度失准与支持域风险 | ResearchObservatoryR30R1.compute(R13)；SELECT K3概率/实际，箱计数≥20与Brier | CalibrationRowsR27 | 恢复冻结概率与正确标签，不在图表中拟合正式校准；验收：概率箱支持/计数/Brier可复算；空箱保留未知，未来拟合拒绝 |
| R14 梯度→概率面→策略消费 | ResearchObservatoryR30R1.stored(R14) / ResearchProvenanceGraphR30R1.project；原optimizer step/publication/stateHash→model→surface→call→freeze | IdeaI08NeuralRepresentationR27（此图只读）；FORMAL_READ_ONLY | 修真实训练/发布/消费路径；保留所有精确原键和历史缺梯度UNKNOWN；验收：任一model/weights/target/position/probability/frozenAt错键断边；不以同一期替代 |
| R15 状态漂移、遗忘与回放价值 | ResearchObservatoryR30R1.compute(R15) / ModelReplayStudyR30R1；固定256窗输入JS与旧/新模型同机会回放 | AtomNetRuntimeR30 | 恢复原模型与配对study；.05只告警，不以漂移直接认定改进；验收：JS重算、模型同机会对比；全池/异期误差不能代替配对遗忘 |
| R16 研究探索预算与有效产出 | ResearchObservatoryR30R1.stored(R16)；真实task CPU、本会话范围、task progress、consume | ResearchPoolR27 | 恢复真实进度回执与预算动作；保留25%探索下限；验收：任务CPU/游标/实际子代独立分母；出生数不等于有效资产，不虚拟CPU |
| R17 失败机制记忆与重复消耗 | ResearchObservatoryR30R1.stored(R17)；当前源同dataset的原失败配对上下界及parent/run | EvolutionLoopR27 | 恢复真实负贡献记忆和有界剪枝；保留探索且不永久禁整族；验收：upper95<0等原条件才剪枝；对象CPU缺省保持未知，缺证不奖惩 |
| R18 研究动作到主链效果全链 | ResearchObservatoryR30R1.stored(R18) / ResearchProvenanceGraphR30R1.project；run→proposal→consume child→confirmation→experiment→精确REAL授权/结算 | DecisionGateEngine / SettlementEngine（图表只读）；FORMAL_READ_ONLY | 先恢复真实链；另修project无条件“REAL缺证”分支，匹配成功/待结算/未知/缺证分别画；验收：合法SETTLED_EXACT才显示结算边；无回执/错实验/错授权/终态未知不能显示成功 |

### 4.1 已有算法应保留的具体语义

- R01/R02 保留固定结构窗口、TRAIN 拟合和 SELECT 独立误差；R07 保留四臂配对协同公式 `L_A + L_B - L_AB - L_none` 及固定 64 期块。数值缺省或区间跨零如实呈现。
- R05 现方法是从真实失败父代产生具体窗口叶子修改假设，有实际 AST/childHash 才是可执行子代。它不是自动发明全新数学理论的证明。
- R06 保留 8 父代轮转、合法组件路径、256 行配对切片与续跑；`RETRAIN_REMOVED` 和 `MASK_FROZEN` 分别表达不同反事实，不能混计。
- R08 的固定 TRAIN 探针和 Jaccard/行为签名用于机制差异，不得偷读 CONFIRM/FUTURE。R12 保留两父代同目标残差相关与共同失败范围，融合子代采用新身份新确认。
- R13 图上不重新拟合正式校准；支持不足和空箱不能补成可靠概率。R15 的 JS 阈值只用于告警，已有 `ModelReplayStudyR30R1` 同机会模型对照应继续保留。
- R14 原模型/权重/概率面/调用/冻结每一键均精确连接。原梯度或参数变动存在但无 matched call 时，完整消费链仍不足；不能按同一期随意连边。
- R16 全任务 CPU 是任务范围，R10/R17 的每对象 CPU 目前缺原件；两者不得替换。25% 探索下限继续保留。
- R11 原 `FrozenConfirmationR30.evaluate` 已保存配对结果，但当前 UI 只画 verified；仅补原结果投影，禁止重开 HOLDOUT 或回流训练。R18 只读原 REAL 链，候选补丁不创建交易/授权。


## 5. 16 逻辑表逐项覆盖

证据为 `R30_REPORT.json#/researchTables`；完整筛选条件、读时段、页大小和来源回执存于 `coverage_full.json`。

| ID | 状态 | 总匹配行 | 本页 | 原查询（保留） |
| --- | --- | --- | --- | --- |
| T01 | READ_OK | 30 | 8 | SELECT run_id,dataset_hash,policy_hash,role,source_commit,source_revision,from_ordinal,through_ordinal,contract_hash,status,summary_json FROM r30r1_analysis_run ORDER BY recorded_at DESC,run_id |
| T02 | EMPTY_NO_ROWS | 0 | 0 | SELECT atom_id,dataset_hash,n_select,missing_rate,state,definition,computed_at FROM r31_atom_score_visible ORDER BY atom_id |
| T03 | EMPTY_NO_ROWS | 0 | 0 | SELECT policy_hash,target_issue,target_ordinal,position,probabilities,frozen_at,deadline_at,hit,settled_at,'FUTURE_READ_ONLY' AS role FROM r27_live_frozen ORDER BY target_ordinal DESC,policy_hash |
| T04 | READ_OK | 3 | 3 | SELECT run_id,measure_key,kind,parent_hash,from_ordinal,to_ordinal,payload_json FROM r30r1_research_measure WHERE kind IN ('FAILURE_PRESCRIPTION','FAILURE_MEMORY','STRUCTURE_FORECAST_ERROR') ORDER BY run_id,measure_key |
| T05 | EMPTY_NO_ROWS | 0 | 0 | SELECT run_id,measure_key,parent_hash,from_ordinal,to_ordinal,evaluated_calendar_issues,mean_gain,lower95,upper95,payload_json FROM r30r1_research_measure WHERE kind='ATOM_PAIRED_GAIN' ORDER BY run_id,measure_key |
| T06 | READ_OK | 1 | 1 | SELECT run_id,measure_key,parent_hash,payload_json FROM r30r1_research_measure WHERE kind='MOLECULE_SYNERGY' ORDER BY run_id,measure_key |
| T07 | EMPTY_NO_ROWS | 0 | 0 | SELECT dataset_hash,probe_set_hash,signature,kept_policy,dropped_policy,stage,recorded_at FROM r30_behavior_dup ORDER BY recorded_at DESC,kept_policy,dropped_policy |
| T08 | READ_OK | 24 | 8 | SELECT generation,expert,proposal_json,policy_hash,recorded_at FROM r27_expert_proposal ORDER BY generation DESC,expert,policy_hash |
| T09 | READ_OK | 4 | 4 | SELECT r.run_id,r.role,r.policy_hash,m.measure_key,m.kind,CASE m.kind "+unitCases(ResearchMeasureKindR31.OPERATOR_GAIN_COST,ResearchMeasureKindR31.PARENT_CHILD_PAIRED,ResearchMeasureKindR31.RESEARCH_BUDGET)+" ELSE 'HISTORICAL_UNIT_UNVERIFIED' END AS unit,CASE WHEN m.kind IN ("+ResearchMeasureKindR31.sqlList(ResearchMeasureKindR31.OPERATOR_GAIN_COST,ResearchMeasureKindR31.PARENT_CHILD_PAIRED,ResearchMeasureKindR31.RESEARCH_BUDGET)+") THEN 'CURRENT' ELSE 'HISTORICAL' END AS tier,m.mean_gain,m.payload_json FROM r30r1_analysis_run r JOIN r30r1_research_measure m ON m.run_id=r.run_id WHERE m.kind IN ("+ResearchMeasureKindR31.sqlList(ResearchMeasureKindR31.OPERATOR_GAIN_COST,ResearchMeasureKindR31.PARENT_CHILD_PAIRED,ResearchMeasureKindR31.RESEARCH_BUDGET)+",'OPERATOR_COST','PARENT_CHILD_GAIN') ORDER BY r.recorded_at DESC,m.measure_key,r.run_id |
| T10 | READ_OK | 1 | 1 | SELECT run_id,measure_key,parent_hash,payload_json FROM r30r1_research_measure WHERE kind='FUSION_COMPLEMENTARITY' ORDER BY run_id,measure_key |
| T11 | EMPTY_NO_ROWS | 0 | 0 | SELECT target_ordinal,position,model_hash,history_hash,probabilities,probability_hash,trained_through_ordinal,model_available_at,feature_through_ordinal,frozen_at,deadline_at FROM r30_model_surface ORDER BY target_ordinal DESC,position,model_hash |
| T12 | EMPTY_NO_ROWS | 0 | 0 | SELECT key AS policy_hash,payload_json,updated_at FROM r27_chart_cache WHERE panel_id='calibration-r30' ORDER BY key |
| T13 | EMPTY_NO_ROWS | 0 | 0 | SELECT task_key,policy_hash,dataset_hash,block_id,state,verified,registration_state,registration_experiment_id,packet_json,result_json,'FORMAL_READ_ONLY' AS role FROM r30_frozen_confirm ORDER BY created_at DESC,task_key |
| T14 | READ_OK | 20 | 8 | SELECT p.*,t.lane,t.status,t.blocked_reason FROM r30r1_task_progress p LEFT JOIN r26_task t ON t.task_id=p.task_id ORDER BY p.task_id |
| T15 | EMPTY_NO_ROWS | 0 | 0 | SELECT p.proposal_id,p.run_id,p.parent_hash,p.kind,p.payload_json,p.state,p.reason,c.child_policy_hash,c.source_generation,c.consumer_version,c.receipt_hash,c.consumed_at FROM r30r1_proposal p LEFT JOIN r30r1_consume c ON c.proposal_id=p.proposal_id ORDER BY p.created_at DESC,p.proposal_id |
| T16 | READ_OK | 3376 | 8 | SELECT issue,decision_key,entry_point,original_allow,original_reason,certificate_active,items_json,source_json,recorded_at,'FORMAL_READ_ONLY' AS role FROM r30_decision_explain ORDER BY recorded_at DESC,issue,decision_key,entry_point |

## 6. 全部 44 指标逐项原因、施工和验收

每行原定义、目标、measurementSource、missingReason、receipt 哈希、repairCardIds 和 r31 定义/评价完整保存在 `coverage_full.json#/metrics`。下表为逐项可执行摘要，不能把缺证自动改为 PASS。

| 指标 | 当前/数值/样本 | 原因 | 施工 | 验收 |
| --- | --- | --- | --- | --- |
| M01 源码与安装身份 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 报告安装源身份已核；完整构建/签名/包哈希验收原件缺失 | 追加同一APK构建manifest、签名和安装回读；不换成main旧源 | 逐项比对source/version/APK/signature；错commit或错签名拒收 |
| M02 功能保全覆盖 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 保全与Android生命周期回归缺原件 | 以211文件/入口/页面为底板生成保全差异，保留历史8图与全部报告96检查 | 实际安装逐入口回归及导出；出现删除/不可达即失败 |
| M03 准时预测持久化 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 逐期request_to_frozen/settled_to_frozen原件不足 | 先修实时写阻塞与任务调度，再采同一期完整timeline | 按原期号时钟算P95≤5000ms；遗漏期进入分母 |
| M04 抓到后正式结算 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | observe_to_settled原件不足 | 恢复唯一结算owner及幂等任务，保存抓到至提交区间 | 按有效开奖逐期P95≤1000ms；乱序重发不重复结算 |
| M05 赛前有效覆盖 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 赛前冻结覆盖证据不足 | 修预测冻结时序，记录未冻结和不足提前量的真实原因 | 完整机会日历下受支持健康场景≥99.5%；不能事后回填 |
| M06 唯一正式结算 | RECORDED_UNVALIDATED / 0 / 0 | 0样本的原REAL唯一性计数不能证明结算账唯一 | 联查原始授权、trade、终态结算唯一键 | 正例重复/乱序投递仅一次；负例冲突必须被拒绝 |
| M07 实时写事务 | FAIL / 1000.0 / 73 | 73个已捕获窗口中最大wait_p95=1000ms超200ms目标，非完整24h | 按实时写等待/持锁/队列排空分别优化并保留真实故障记录 | 相同负载独立24h窗口P95≤200ms且失败0；不删坏窗口 |
| M08 分库隔离 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 同表双写者与分库阶段状态缺独立原件 | 核owner唯一性和现阶段模式；仅在已批准迁移阶段执行分库 | 双owner冲突反例、原件fence；不得凭单库仍存在自动FAIL |
| M09 迁移一致性 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 迁移比对未测，不等于已证实数据损坏 | 在实际迁移发生时保存计数、内容hash和恢复点 | 原/目标逐分区一致，错行/缺行检测；未迁移标N/A有证据 |
| M10 每期训练投递 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | outbox投递、接收、消费连续性缺同机会原件 | 沿每期开奖outbox原键恢复任务投递和连续游标 | 漏期、重复、进程死后补投验证100%且重复消费0 |
| M11 训练完成新鲜度 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 任务训练完成新鲜度缺原件 | 分别记已投递/实际完成/当前依赖等待游标 | 正常负载最老积压≤5期；超限FAIL，未知不可填0 |
| M12 任务活性 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 任务活性缺完整时序测量 | 区分READY/RUNNING/有效推进/等待依赖，复核停滞任务 | 有CPU无提交、状态RUNNING无进度应检出；合法等待单列 |
| M13 前后台连续性 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 前后台场景回归未证明 | 保存提交后任务原件，恢复后按owner/fence接续 | 锁屏/切后台/进程重启不丢已投递任务；缺口有解释 |
| M14 进程稳定性 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 未附完整崩溃ANR观测窗口 | 采ApplicationExitInfo/logcat与版本/时间窗口 | 热启动冷启动/取消报告场景无崩溃；缺日志保持未知 |
| M15 热库与总磁盘 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | draws=5000仅满足热开奖上限；库约5.06GB不是总预算通过证明 | 按库/归档/缓存/临时文件分开治理，先保全可恢复原件 | 总预算内且恢复复算一致；不得只删draws宣称空间问题修好 |
| M16 报告取消 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 报告取消延时缺实测原件 | 保留任务取消协议，逐SQL/大payload/ZIP阶段验证释放 | 取消到资源释放≤2000ms，下一次导出可成功 |
| M17 报告完整性 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 本次ZIP完整可读但40检查READ_ERROR，完整索引不等于检查全通过 | 保留96检查/原成员与有界附件，修慢依赖不缩覆盖 | ZIP索引100%/hash有效；≤20MiB或明确分卷；失败可定位可重导 |
| M18 PIT泄漏反证 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | PIT拒绝计数不是标签泄漏反证 | 在隔离验证环境扰动当前/未来标签，保留过去输入固定 | 过去预测不变；把注入测试来源与产品自然数据分列 |
| M19 训练分割污染 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 分割隔离没有当前版本验收原件 | 保留TRAIN/SELECT/CONFIRM/FUTURE角色合同和holdout使用账 | 重复确认/跨角色样本注入拒绝；不能松门槛让管道变绿 |
| M20 全状态续训恢复 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 全状态续训恢复一致性未证明 | 复核权重、optimizer、rng、游标、ABI与pending提交共同恢复 | 中断/重复恢复与连续基线在登记容差内且不重复学习 |
| M21 策略完整合同 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 出生策略合同覆盖未证明 | 保留BirthGate验证，真实生成合法AST和输入绑定 | 空壳、非法节点、错ABI拒绝；合法完整独立子代入池 |
| M22 结构与行为去重 | RECORDED_UNVALIDATED / 0 / 0 | 去重账为0且样本0，不能证明无重复 | 以原结构hash/参数/行为签名/ledger独立复算 | 相同重试不增出生数，结构不同但行为相同可被识别 |
| M23 族专家覆盖 | BLOCKED / 81 / 82 | 81/82是静态可调用绑定，F23教师未绑定；不是81条训练成功 | 按原教师来源/ABI绑定F23，其余按真实artifact与消费分层验收 | 静态可调用、输入可用、运行、产物、消费、效果分别测试 |
| M24 模型实际消费 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 模型call与冻结原键匹配率缺原件 | 按publication/modelHash/probabilityHash/目标/位置接回原消费链 | 错模型/错概率面/错epoch不匹配；合法冻结精确引用 |
| M25 矩阵接线 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 20注册矩阵无原产物，但旧R952矩阵50/48独立存在 | 修不可变输入与逐类型producer，保留R952及其原反馈 | 20类型逐一正确来源/维度/角色/目标/hash；缺学生分支不造矩阵 |
| M26 校准时间隔离 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 校准样本和时间隔离未验收 | 拟合只用合法训练/校准分区，并保留模型cutoff | 未来label/同确认期拟合被拒；零模型显式原因 |
| M27 资格预算连续 | RECORDED_UNVALIDATED / 0 / 0 | 冲突0但样本0，不代表预算与连续游标通过 | 保全资格全局序号、预算消耗与连续原件 | 重启预算不重置；重复消费不加预算；缺期不跳过 |
| M28 主链闭环 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 自然无主链不能替代工程合法正例贯通 | 在隔离工程fixture完整跑原DecisionGate/SettlementEngine | 合法正例闭环，非法/过期/无赛前冻结全部拒绝；不能转自然资格 |
| M29 确认队列连续 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 确认队列持续性未测 | 保留独立确认队列优先级与已登记样本 | 压力和恢复下无被挤出；不得重用SELECT或重启确认 |
| M30 主链选择器身份 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 解释表3376行不是主链选择器已确认 | 以同decision/selector证据查实际REAL授权 | selector未确认却REAL必须为0；解释文本不能授权 |
| M31 诊断可追溯 | RECORDED_UNVALIDATED / 1.0 / 16 | 1.0=16规则具行动字段，非全软件100%正确 | 保留16规则并补按每个真实故障独立复演的结果 | 错误诊断反例可被识别；每UNKNOWN给采集动作 |
| M32 报告口径一致 | UNKNOWN_SOURCE_MISSING / UNKNOWN / null / 0 | 缺同角色同水位独立源对照 | 以独立原SQL/导出/UI同范围对照，区别不同读时点 | 同对象重复渲染不能当独立对照；刻意错源应发现不一致 |
| M33 图表回归 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 没有当前修复Android截图回归原件 | 保留既有图表注册/位置/样式，逐页恢复新增真实数据 | 既有8图视觉基线+全部29历史/18研究/16逻辑表可打开；缺证不画假点 |
| M34 签名覆盖安装 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 覆盖安装/首启二启无崩溃未验收 | 按可授权签名路径做覆盖安装与回滚保全 | 签名不匹配如实失败；原数据保留，首启二启可导出 |
| M35 失效模型撤销 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 证书失效到决策拒绝延时未测 | 按同证书撤销事件与DecisionGate读回测量 | 目标≤5000ms；旧证书不能继续REAL；不混APK回滚时间 |
| M36 同池对照 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 合成对照与完整工厂实验当前源未完成 | 分别完整跑同池随机/打乱/正控制及创造工厂，封存原件 | 有可重算多轮分母和区间；合成不计自然性能 |
| Q01 自然ACT命中 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 自然未来ACT命中没有足量已登记样本 | 工程链恢复后继续真实独立FUTURE收集 | 逐策略原分母/区间；未达34%不得宣称达标，优先目标37% |
| Q02 真实频率 | NOT_DEPLOYED / UNKNOWN / null / UNKNOWN / null | 无原REAL行和有效ACTIVE证书，未部署 | 仅按自然资格准入恢复真实授权；独立报告ACT率与REAL率 | 目标15%不能补配额；无有效证书须保持不授权 |
| Q03 真实统计优势 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 序贯优势未有合规原件 | 保全假设/alpha_i/预算/连续机会后评价e值 | e≥1/alpha_i且全部假设成立；不重启预算挑好段 |
| Q04 跨块稳定 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 跨块稳定缺事前规则与同角色块原件 | 先登记窗口/块再持续收集，所有好坏块一起报告 | 事后选窗反例拒绝；足量同策略同角色块独立评价 |
| Q05 校准与BSS | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 自然校准/BSS及支持范围不足 | 按独立未来计算ECE/BSS与区间、样本与箱支持 | n≥200、支持质量≥90%、ECE目标≤.04；不强造5非空箱 |
| Q06 进化提升 | INSUFFICIENT / UNKNOWN / null / UNKNOWN / null | 父子配对进化提升尚无验证原件 | 同日历目标配对父子，登记块规则与损失 | 有效区间下界>0才记改善；无差/负差如实保留 |
| Q07 有效资产产量 | RECORDED_UNVALIDATED / 0 / 0 | 有效证书0样本0，记录而未验收 | 沿资格恢复真实有效资产，只统计满足逐资产条件的证书 | 目标≥1不能伪造/补配额；旧证书/仅BORN不算有效产出 |
| Q08 空信号与优势检出力 | UNKNOWN_STALE_SYNTHETIC_SOURCE / UNKNOWN / null / UNKNOWN / null | 合成工厂来源旧ab2695…；120计划0完成 | 当前修复源重跑原六臂×20种子工厂；另测34/37/40%目标概率对照，不混植入强度与命中率 | 逐轮种子/分母/误授证CI/功效CI完整；旧小样本不得签当前源 |

## 7. 全部 96 检查逐项覆盖

每项完整错误原件定位、证据路径、error_detail SHA、受影响指标与源方法保存在 `coverage_full.json#/checks`。READ_ERROR 的具体原因必须分别追溯，不能按表面相同超时代码就合并同根因。

| ID | 检查标题 | 执行/健康 | 原reason或含义 | 源码方法 |
| --- | --- | --- | --- | --- |
| RSEC-001 | 【R975故障定位：先看阻断，再看原始表】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/RuntimeChainDiagnosisR975.java → write |
| RSEC-002 | 【逐文件、方法、直接调用索引：编译字节码静态索引，不是真机覆盖率】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/RuntimeChainDiagnosisR975.java → writeSourceIndex |
| RSEC-003 | 【R967同一语句快照：研究登记、训练、校准与队列】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/ResearchFunnelR967.java → capture + appendMoleculeCheckpointCP12 |
| RSEC-004 | 【R974逐段执行收据：只读、各查询独立时点、不是整库事务快照】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/ResearchStageProgressR973.java → write |
| RSEC-005 | 【R970历史算法原始档案与新种子去向】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/HistoricalGenomeImportR967.java → writeReport |
| RSEC-006 | 【手机储存与5000期清理】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/StorageReclaimR943.java → summary |
| RSEC-007 | 【原子世界资产工具与神经学习】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/AtomicToolWorldR943.java → report |
| RSEC-009 | 【0.当前总判定】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-009\|budgetMs=20000 | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → overall |
| RSEC-010 | 【0A.R923全软件情况统一总览（先看这里）】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-010\|budgetMs=20000 | app/src/main/java/com/ethk3/novaonepro/UnifiedSoftwareStatusR923.java → capture |
| RSEC-011 | 【分时读取的详细视图；不作为上方统一漏斗分母】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-011\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/ResearchDashboardR924.java → capture |
| RSEC-012 | 【挑战者、证书与主链实际应用｜统一账本】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/ResearchPipelineStatusR941.java → capture |
| RSEC-013 | 【GENESIS_NEW创生资产榜】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → appendGenesisAssets |
| RSEC-014 | 【活跃算法家族Future OOS榜】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → appendAlgorithms |
| RSEC-015 | 【算法证据归档榜（淘汰不删除）】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → appendArchivedAlgorithms |
| RSEC-016 | 【240项底层因子/原子完整登记】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → appendAtomRegistry |
| RSEC-017 | 【失败驱动的新原子提案】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → appendAtomProposals |
| RSEC-018 | 【0B.5.4.1新增检查与运行证据】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-018\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/SoftwareAuditR925.java → report |
| RSEC-019 | 【E05-A.模型准确率卡】 | EXECUTED / NOT_PROVEN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/Batch34DiagnosticCardsR930.java → render |
| RSEC-020 | 【E05-B.策略效果卡】 | EXECUTED / NOT_PROVEN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/Batch34DiagnosticCardsR930.java → render |
| RSEC-021 | 【E05-C.期级链路卡】 | EXECUTED / NOT_PROVEN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/Batch34DiagnosticCardsR930.java → render |
| RSEC-022 | 【E05-D.资源效率卡】 | EXECUTED / NOT_PROVEN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/Batch34DiagnosticCardsR930.java → render |
| RSEC-023 | 【旧算法观察桥接（独立历史口径）】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/Batch34ChallengerCertificateBridgeR930.java → report |
| RSEC-024 | 【0D.真实训练与逐期执行证据：fullblood】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/r952/FullbloodDiagnosticsProviderR952.java → render |
| RSEC-025 | 【0D.真实训练与逐期执行证据：capability_trace】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/r952/CapabilityTracePageR952.java → render |
| RSEC-026 | 【0C.R940界面卡顿与闪退取证】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → generateArtifact |
| RSEC-027 | 【1.身份与不可变红线】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → identity |
| RSEC-028 | 【2.B01–B15工程状态】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → batchStatus |
| RSEC-029 | 【3.唯一运行状态机】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → stateMachine |
| RSEC-030 | 【4.R3.6硬门】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-030\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → hardGates |
| RSEC-031 | 【5.实际开奖分母预测覆盖与最近10期业务闭环】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → recentClosure |
| RSEC-032 | 【6.进程/Service/业务心跳】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → generateArtifact |
| RSEC-033 | 【7.R3.6运行健康账本】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-033\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/ModelEffectivenessAnalyzerR35.java → readRuntime |
| RSEC-034 | 【8.严格OOS/Wilson/ECE/Brier/Coverage】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-034\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → oos |
| RSEC-035 | 【9.模型有效性与根因优先级】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-035\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/ModelEffectivenessAnalyzerR35.java → analyze |
| RSEC-036 | 【10.唯一Owner】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → owners |
| RSEC-037 | 【11.1200正式动作与实时/延后写分离】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-037\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → actionContract |
| RSEC-038 | 【12.精确结算与OOS标签】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → settlement |
| RSEC-039 | 【13.REAL与SHADOW分账】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-039\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → realShadow |
| RSEC-040 | 【14.真实出手机会漏斗（15%仅观察目标）】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-040\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → frequency |
| RSEC-041 | 【15.后台恢复】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → recovery |
| RSEC-042 | 【16.物理设备验证矩阵】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → deviceMatrix |
| RSEC-043 | 【17.历史链只读隔离】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → legacyReadOnly |
| RSEC-044 | 【18A.B14–B15原始取证附录】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → generateArtifact |
| RSEC-048 | 【第1章 数据与期号】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-048\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectData |
| RSEC-052 | 【第2章 Service与业务运行】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-052\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectRuntime |
| RSEC-053 | 【第3章 账本与首写冻结】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-053\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectLedger |
| RSEC-054 | 【第4章 100号码概率面】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-054\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectNumberSurface |
| RSEC-055 | 【第5章 跨位置结构】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-055\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectCrossPosition |
| RSEC-056 | 【第6章 固定K3漏斗】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-056\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectK3Funnel |
| RSEC-057 | 【第7章 1200正式动作空间】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-057\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectActionSpace |
| RSEC-058 | 【第8章 动作Ranker】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-058\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectRanker |
| RSEC-059 | 【第9章 选择后校准与严格OOS】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-059\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectCalibration |
| RSEC-060 | 【第10章 频率与REAL硬门】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-060\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectFrequency |
| RSEC-061 | 【第11章 失败树与反证】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-061\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectFailureTree |
| RSEC-062 | 【B14动态故障诊断】 | EXECUTED / NOT_PROVEN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57FailureDiagnosisEngine.java → render |
| RSEC-063 | 【确认事实/假设分离】 | EXECUTED / NOT_PROVEN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57FailureDiagnosisEngine.java → render |
| RSEC-064 | 【因果故障图】 | EXECUTED / NOT_PROVEN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57FailureDiagnosisEngine.java → render |
| RSEC-065 | 【修复优先级】 | EXECUTED / NOT_PROVEN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57FailureDiagnosisEngine.java → render |
| RSEC-066 | 【K3失败九类拆解】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-066\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java → render |
| RSEC-067 | 【第12章 消融与冲突】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-067\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectAblation |
| RSEC-068 | 【策略贡献/冲突/冗余/消融】 | EXECUTED / NOT_PROVEN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/StrategyConflictAblationEngine.java → render |
| RSEC-069 | 【公式/算法/概率审计】 | EXECUTED / WARN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/FormulaAlgorithmAuditEngine.java → render |
| RSEC-070 | 【第13章 代码Owner完整性】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-070\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectCodeOwner |
| RSEC-071 | 【代码Owner与完整性审计】 | EXECUTED / WARN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/CodeOwnershipCompletenessAuditEngine.java → render |
| RSEC-072 | 【第14章 Challenger施工与验证】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-072\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java → collectImprovement |
| RSEC-073 | 【改进Challenger计划】 | EXECUTED / NOT_PROVEN | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/ImprovementChallengerPlanEngine.java → render |
| RSEC-074 | 【第15章 Finding完整施工账本】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-074\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java → findingChapter |
| RSEC-075 | 【第16章 根因依赖DAG】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-075\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java → edgeChapter |
| RSEC-076 | 【第17章 身份、玩法与REAL不可变红线】 | EXECUTED / PASS | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java → redlineChapter |
| RSEC-077 | 【第18章 诊断编排确定性与完整性校验】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-077\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java → validationChapter |
| RSEC-078 | 【第19章 R811结算、证据、SHADOW与Research统一真值】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-078\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java → r811TruthChapter |
| RSEC-079 | 【自主策略发现/深度学习/动态进化】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-079\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java → r811ResearchTruth |
| RSEC-301 | 【真实性边界】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java → buildBody |
| RSEC-302 | 【报告哈希与完整结束标志】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java → buildInternal |
| RSEC-303 | 【END_OF_LIVE_DIAGNOSTIC_REPORT】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/LiveDiagnosticReportEngine.java → renderChapter |
| RSEC-304 | 【18B.R922运行闭环/影子结算/Research/REAL硬门唯一真值】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → generateArtifact |
| RSEC-305 | 【R923运行闭环唯一真值】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-305\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → pipelineDetail |
| RSEC-306 | 【R923严格Future OOS统计】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-306\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → shadowDetail |
| RSEC-307 | 【R923深度学习・动态迭代・算法创造唯一真值】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-307\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → researchDetail |
| RSEC-308 | 【证书化主链贡献】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → appendContributions |
| RSEC-309 | 【R923逐期预测主链贡献审计】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-309\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → researchDetail |
| RSEC-310 | 【R923真实出手统计与硬门唯一真值】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-310\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → realDetail |
| RSEC-311 | 【R922为什么中/不中/不出手真值】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → renderWhyCards |
| RSEC-312 | 【为什么中】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-312\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → buildTruthCards |
| RSEC-313 | 【为什么不中】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-313\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → buildTruthCards |
| RSEC-314 | 【为什么不出手】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-314\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → buildTruthCards |
| RSEC-315 | 【逐期结算统计】 | READ_ERROR / UNKNOWN | ReportReadFailureCP4:REPORT_CHECK_TIMEOUT\|RSEC-315\|budgetMs=1000 | app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java → buildTruthCards |
| RSEC-316 | 【18C.创造候选接入预测主链审计】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/ResearchMainlineContributionAuditR922.java → recentDetail |
| RSEC-317 | 【18C2.算法数量、归档原因、晋升淘汰与完整清单】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/AlgorithmLifecycleReportR927.java → capture |
| RSEC-318 | 【18.报告完整性】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → generateArtifact |
| RSEC-319 | 【19.完整报告SHA256】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → <init> |
| RSEC-320 | 【END_OF_COMPLETE_REPORT】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java → <init> |
| RSEC-321 | 【20.全部创造算法与研究方法，含归档】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/AlgorithmLifecycleReportR927.java → writeDetails |
| RSEC-322 | 【全部创造算法中文清单｜全状态｜无数量截断】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/AlgorithmLifecycleReportR927.java → allAlgorithms |
| RSEC-323 | 【全部研究方法｜含归档｜无数量截断】 | EXECUTED / OBSERVED | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/AlgorithmLifecycleReportR927.java → allResearchMethods |
| RSEC-324 | 【21.全部资产、晋升、淘汰证据原始表】 | EXECUTED / NOT_APPLICABLE | EXACT_SOURCE_SNAPSHOT_EVALUATOR | app/src/main/java/com/ethk3/novaonepro/FullResearchInventoryR923.java → writeToStrict |

## 8. 82 能力、20 矩阵、29 旧图全覆盖清单

下表的 callable/produced/consumed 是原报告对象字段，null 保持未知。20 矩阵能力行与独立矩阵表重复描述同一注册对象，不双计产量。

| 能力ID | kind | callable | produced | consumed | 具体阻断/来源范围 |
| --- | --- | --- | --- | --- | --- |
| F01 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F02 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F03 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F04 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F05 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F06 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F07 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F08 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F09 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F10 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F11 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F12 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F13 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F14 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F15 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F16 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F17 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F18 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F19 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F20 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F21 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F22 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| F23 | FAMILY | false | UNKNOWN / null | UNKNOWN / null | F23_ORIGINAL_TEACHER_NOT_BOUND |
| F24 | FAMILY | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E01 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E02 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E03 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E04 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E05 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E06 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E07 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E08 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E09 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E10 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E11 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E12 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E13 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E14 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E15 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| E16 | EXPERT | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I00 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I01 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I02 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I03 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I04 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I05 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I06 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I07 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I08 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I09 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I10 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I11 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I12 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I13 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I14 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I15 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I16 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I17 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I18 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I19 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| I20 | IDEA | true | UNKNOWN / null | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| ATOMNET | MODEL | true | false | false | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:EVIDENCE_TEACHER | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:STUDENT_ERROR | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:FORWARD_REASONING | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:REVERSE_REASONING | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:COUNTERFACTUAL | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:FEEDBACK | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:CONTRADICTION | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:TEACHER_DIVERSITY | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:TEMPORAL_PRECEDENCE | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:INVARIANCE | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:CAUSAL_SUPPORT | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:COUNTER_EVIDENCE | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:RESIDUAL_MECHANISM | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:NUMBER_POSITION_COMPATIBILITY | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:CROSS_POSITION_FLOW | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:ACTION_UTILITY | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:ALGORITHM_COMPLEMENTARITY | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:EVOLUTION_CREDIT | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:R930:RESOURCE_EFFICIENCY | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |
| MATRIX:SU2:LEARNED_FACTOR_TEACHER_CHECKPOINT | MATRIX | true | false | UNKNOWN / null | RUNTIME_INPUT_ARTIFACT_AND_CONSUMPTION_RECEIPT_REQUIRED |


### 20 注册矩阵

| 矩阵ID | 来源表 | 原产物数 | 输入/产出 | producer → consumer |
| --- | --- | --- | --- | --- |
| R930:EVIDENCE_TEACHER | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → com.ethk3.novaonepro.MatrixSnapshotFeedbackR930.settle |
| R930:STUDENT_ERROR | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:FORWARD_REASONING | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → com.ethk3.novaonepro.MatrixSnapshotFeedbackR930.settle |
| R930:REVERSE_REASONING | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:COUNTERFACTUAL | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:FEEDBACK | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:CONTRADICTION | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:TEACHER_DIVERSITY | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:TEMPORAL_PRECEDENCE | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:INVARIANCE | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:CAUSAL_SUPPORT | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:COUNTER_EVIDENCE | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:RESIDUAL_MECHANISM | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:NUMBER_POSITION_COMPATIBILITY | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:CROSS_POSITION_FLOW | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:ACTION_UTILITY | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:ALGORITHM_COMPLEMENTARITY | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:EVOLUTION_CREDIT | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| R930:RESOURCE_EFFICIENCY | research_matrix_snapshot_r930 | 0 | false / false | com.ethk3.novaonepro.MatrixSnapshotFeedbackR930$FrozenMatrix.<init> → PERSISTENCE_AND_DIAGNOSTICS |
| SU2:LEARNED_FACTOR_TEACHER_CHECKPOINT | su2_matrix_snapshot | 0 | false / false | com.ethk3.novaonepro.strategyv2.MatrixSnapshotSU2.capture → com.ethk3.novaonepro.strategyv2.MatrixSnapshotSU2.support / MatrixStoreSU2.maintain |


### 29 旧图

| ID | 标题 | 缓存存在型produced | 原source |
| --- | --- | --- | --- |
| status | 研究状态/同报告快照 | UNKNOWN / null | ChartsRepositoryR27.headlineSnapshotR30；commonMetrics |
| p01_three_scores | 三类成绩并列 | true | ChartCacheR27.threeScores；replay_score/live_frozen/eprocess |
| p02_generation_holdout | 每代冠军留出：主/打乱/随机 | true | ChartCacheR27.generationHoldout；r27_generation |
| p03_evolution_events | 每代进化事件 | true | ChartCacheR27.evolutionEvents；r27_generation |
| p04_seven_counters | 进化七计数累计 | true | ChartCacheR27.sevenCounters；generation/r26_evolution |
| p05_lineage | 父子谱系最近60条 | true | ChartCacheR27.lineage；lineage/policy/replay |
| p06_idea_survival | 思路族存活率 | true | ChartCacheR27.ideaSurvival；r27_policy |
| p07_funnel | 淘汰漏斗 | true | ChartCacheR27.funnel；generation/shadow/eprocess/certificate |
| p08_bss | 深度学习A：累计BSS及区间 | true | ChartCacheR27.bss；brier_pair FUTURE ATOMNET |
| p09_rolling200 | 深度学习B：滚动200 Brier | true | ChartCacheR27.rolling；r27_brier_pair |
| p10_train_holdout_loss | 深度学习C：训练/留出损失 | true | ChartCacheR27.trainHoldout；r27_atomnet_epoch |
| p11_eprocess | 序贯进度 | true | ChartCacheR27.eprocess；r27_eprocess |
| p12_mainline_weight | 主链算法参与权重 | true | ChartCacheR27.mainline；ResearchMainlineContributionAuditR922 |
| p13_runtime_health | 研究运行健康 | true | ChartCacheR27.health；CPU/task/generation/wait/opportunity |
| p14_atom_scores | 原子回放评分分布 | true | ChartCacheR27.atoms；r27_atom_score |
| p15_paired_improvement_r30 | 逐代配对提升/独立确认 | UNKNOWN / null | ReportChartsR30.evolution；同报告快照 |
| p16_capability_coverage_r30 | 能力覆盖与阻塞 | UNKNOWN / null | ReportChartsR30.capabilities；82对象注册 |
| r83_algorithm_all | 全部算法逐期实盘榜 | UNKNOWN / null | ChartsRepositoryR83.r27AlgorithmBoard |
| r83_algorithm_rank | 算法实盘排行榜 | UNKNOWN / null | ChartsRepositoryR83.algorithmBoard |
| r83_best_curve | 最佳算法累计命中 | UNKNOWN / null | ChartsRepositoryR83.bestAlgorithmCurve |
| r83_action_candles | 出手资金K线 | UNKNOWN / null | ChartsRepositoryR83.actionCandles；exploration与shadow join |
| r83_all_hit | 全部预测命中率走势 | UNKNOWN / null | ChartsRepositoryR83.hitTrend(false) |
| r83_act_hit | 出手命中率走势 | UNKNOWN / null | ChartsRepositoryR83.hitTrend(true) |
| r83_number_candles | 当前所选位置号码K线 | UNKNOWN / null | ChartsRepositoryR83.numberCandles；最多600开奖 |
| r83_number_line | 当前所选位置号码轨迹 | UNKNOWN / null | ChartsRepositoryR83.numberLine；draws |
| r83_omission_heat | 号码遗漏热力图 | UNKNOWN / null | ChartsRepositoryR83.omissionHeat；draws |
| r83_hot_cold | 当前所选位置冷热号 | UNKNOWN / null | ChartsRepositoryR83.hotCold；draws |
| r83_calibration | 预测概率校准 | UNKNOWN / null | ChartsRepositoryR83.calibration；旧概率来源 |
| r83_brier20 | 短期波动参考：滚动20期Brier | UNKNOWN / null | ChartsRepositoryR83.networkBrier（R27加载时改名） |

## 9. 173 → 211 继承自检

| 旧项 | 当前判断 | 本次原件 | 处理 |
| --- | --- | --- | --- |
| R343-031 | STILL_APPLIES | 20注册canonical矩阵retainedCount=0；r952_matrix_snapshot=50/feedback=48独立保留 | 保留旧在线矩阵，修逐类型新来源与生产链 |
| R343-035 | SOURCE_IMPLEMENTED_CURRENT_RUNTIME_EMPTY | r33_research_input_head及r33_research_applied已进TABLES，本报告为明确0而非null | 不得重复按漏白名单修；定位实际产出0 |
| R343-036 | SOURCE_IMPLEMENTED_METADATA_COVERAGE_OBSERVED | ReportInlineBudgetR343保留专额，chartObjectIndex14/14、repairFrameIndex17/17、omission0 | 保留预算隔离与索引；大payload有界截断不得误为无原件 |
| R343-037 | SOURCE_IMPLEMENTED_CURRENT_SOURCE_COUNT_VALID | ReportSnapshotR30按chart_cache JOIN COMMITTED analysis_run/current source统计，18图仍各0 | 不得回退为只数PRODUCED head；修输入和计算，不改计数掩盖停工 |

应用施工仅允许在精确 211 底座作增量修改。只修已有确凿问题，不用旧版本整文件覆盖新代码；任何非必要删除、重命名注册 ID、改写原角色、放宽出生/资格门槛均应在差异审查中拒绝。报告本体不可回写，原 ZIP 及 hash 保留为回归证据。

## 10. 本审计可交付验证与剩余施工门槛

- 已解析全 96 检查、44 指标、165 索引行、826 表目录与 45 R26 节；每项标明原始来源，交付 `coverage_full.json`。
- 已与 source commit `5c7f…` 核实际生产/消费方法，18 图逐条给出真实修复动作及反例，交付 `charts_18_contracts.json`。具体根因与父代理策略/运行审计合并，避免重复报 18 个独立根因。
- R18 候选显示补丁 `R18_exact_receipt_projection.candidate.patch` 在该源码 `git apply --check` 通过。精确候选 helper 抽取的 `java r18_candidate/R18ProjectionContractHarness.java` 共 12 个正负用例通过；这只验证显示合同，不等于完整 Android 编译、SQLite 校验或真机结算已通过。
- 新增对 R11 的修复只要求投影已经保存的配对原件，不擅自改统计公式/重做确认。R10/R17 实际逐对象 CPU 是分开登记的功能补齐，不能变成核心停工修复的阻塞依赖。
- 真机仍须完成：合法输入恢复、真实参数/策略变化、持久化消费、独立确认、原数据覆盖安装、既有8图视觉回归、完整报告导出/取消/再次导出、相同source质量与工厂实验。完成之前保持未验收状态，不填写“全部修复/全面通过”。

