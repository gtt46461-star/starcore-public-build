# 图表、研究产物和检测报告的同源验收

适用工单为 R341-B08-02、B08-04、B11-01、B11-02、B11-04。源码基线为 `5562f9b19aa09f9f7d9e69ca085db56c537967fc`。对象逐项对应 `specs/RESEARCH_OBJECT_CONTRACTS.json` 的 29 个旧图、18 个研究图、16 个逻辑表。文件基准目录为 `app/src/main/java/com/ethk3/novaonepro/`。研究生产和报告类在该目录的 `strategyv2/` 下。

## 本次导出给出的边界

在 `R30_REPORT.json#/objectIndex` 中，18 个研究图的当前源帧数与报告计得的消费数逐项为 0。29 个旧图的 `produced`、`consumed` 均为 null，不能据此判定真机这 29 个图全空。16 个逻辑表页都执行了有界读取；其 `totalMatchingRows` 与第一页数据如下。T16 的 913 条决策解释中也包含拒绝解释，不能累计成策略资产或 REAL 出手。

| 视图 | 本报告同一 SQL 语句的总行数 / 首页面展示行数 | 施工核对的原件 |
|---|---|---|
| T01 / T08 / T14 / T16 | 29/8、24/8、20/8、913/8 | 分别回查分析运行、专家提案、任务进度、决策解释的稳定键 |
| T04 / T06 / T09 / T10 | 3/3、1/1、4/4、1/1 | 按 kind、run、role、revision 复读失败处方、分子协同、算子衡量、融合记录；逐项确认是否属于本版 |
| T02 / T03 / T05 / T07 | 各 0/0 | 可见原子评分、合法冻结、配对增益、行为去重 |
| T11 / T12 / T13 / T15 | 各 0/0 | 对应模型面、校准缓存、冻结确认、提案与消费。凭各自可读原表诊断前驱 |

导出页由 `ReportSnapshotR30.capture` 对各表调用 `ResearchTableAdaptersR30R1.readPageR31(...,0,8,...)`。手机表页默认 20 行。比较同快照的相同范围及业务键；不比较包含读取时刻、耗时的整份回执 hash。

## 文件、真实路径与改法

| 工单 | 当前正常路径 | 施工和验收 |
|---|---|---|
| B08-02 | `ResearchObservatoryR30R1.step → stored/compute → productR335 / appendHeadR335` 写分析运行、测量、缓存与源状态；R06 经独立配对产线，step 中主动跳过 R06 | 对 18 个 ID 逐项用注册的 measure 及对应产线验产品。记录 dataset、input/contractHash、source、role、ABI、revision、目标区间、单位、支持数和原件。采用已有 `classifyFrameR335` 的判别，对 R06 单独验证配对均值、区间、componentPath 和反事实身份；正式链 R11/R14/R18 以可复核配对或业务原件展示。每次切片只处理本图。一个源损坏须能在下一次轮转访问其余图。 |
| B08-04 | `ResearchConsumptionStoreR31.completeWithin`、出生/非出生实际动作账，`ResearchResultContractR31.consumptionKindOf` | 出生和非出生分别验证来源运行、产品 hash、动作前后实体与消费键。去重用同一个动作身份，不能把联合查询复制造成的行数作为消费。R11/R14/R18 的“优化消费”标不适用；正式链是否存在完整原件独立输出。 |
| B11-01、02 | `ReportSnapshotR30.rowsets` 构造 `researchProduced:<id>`、`researchConsumed:<id>`、`repairFrameIndex`；`ReportObjectIndexR30.build/buildProductionR31/layersR31` 输出事实及层级 | 当前计数把本版缓存行计作产物，消费查询按图、measure_key 计数且没有 run.source_commit 条件；索引取最近帧的规则也与 UI 优先当前源的规则不同。统一当前、历史作用域，要求同一 run、版本、语义、角色及原件身份。分别输出持久诊断帧数、有支持的产品数、已验证动作数、正式只读原件数和缺失原因。消费回执绑定出生或 `r33_research_applied`，同一 consumeKey 仅计一次。用已有生产判据及原件验证器，避免在报表快照中逐条无界解析所有大 JSON；无法完成核验时输出待核验或覆盖范围。 |
| B11-01、04 | `ChartsActivityR83.load → ChartsRepositoryR27.Page.read` 按 29 个 ID 分发至 headline、14 个缓存图、2 个报告快照图、12 个 R83 图；`ChartCacheR27.build/safe/count` 是缓存生产者 | `count` 目前捕获 RuntimeException 即返回 0，修成异常由 panel 的隔离层转为带 SQL 来源的读取错误。对组合面板保留已成功的子测量，标 PARTIAL 和失读字段；取消单独传播。`ChartsRepositoryR83.actionCandles/hitTrend` 的 SQLiteException 当前可被吞掉而派生空图，应区分原表缺失、SQL 错、窗口内成功查询 0 行。 `ChartsActivityR83.load` 目前用 points/edges/matrixRows 非空累计有绘图数据；改为验证有限值与所需支持、真实结构边或矩阵有效单元。状态类文本可正常显示，另列状态，未知点对象不计有值。 |
| B11-02 | `ChartRegistryR31` 的 R83 role、`ChartsRepositoryR83.loadPanel/actionCandles/hitTrend` 的真实 SQL 与 `ReportObjectIndexR30` 的展示索引 | `r83_action_candles` 现注册为 REAL，实际按 ExplorationLaneR83 × R35 影子结算推导模拟资金。统一标探索影子模拟。两条 R35 命中趋势当前被归入 DRAW_HISTORY，按真实来源分别标全部已结算影子或行动子集。按原有冻结及观察身份验证唯一配对；重复或歧义单独报数。正式动作与结算逐笔只认主链唯一决策/权威结算。旧榜、事后最优曲线、简称“实盘”的研究榜使用实际角色和区间。 |
| B11-04 | `ResearchWorkbenchActivityR30R1.load → ResearchWorkbenchRepositoryR30R1.Page.pin/read → readFrame`；`ResearchTablesActivityR31.load → ResearchTableAdaptersR30R1.readPageR31` | 工作台按组一次固定这 6 个图的 frame ID；`readFrame` 核验 payload、原件及摘要，缺新帧时可读历史。保留每图逐次取消、超时和旧 epoch 屏蔽。历史原件可读时标历史、截至范围，另列当前阻断。表页 COUNT 与有界 SELECT 同一 SQL 语句取数；大行阻塞、超限、取消、过滤条件、越界 offset 均单列。分页跨请求变化时重置或提示重读；不承诺同一活库的不同读取时刻产生相同统计。 |

## 批内隔离复演与签收

1. B08 在隔离库用同输入运行 18 图原生产者，给每一图有输入、缺输入及损坏原件三组情况。有支持的真零保留为零，负收益保留原值，全部缺值不可报产出。对 R16 核对实测任务 CPU，对 R06 核对真正配对测量；图类型允许结构边时须回到相应原始节点边。15 个可用于研究动作的视图分别证实正式输入角色、成功或拒绝，3 个正式只读视图验证禁止反向驱动训练。原工单 `R341-B08-02-A01/A02/A03`、`R341-B08-04-A01` 当批出具结果。
2. 针对旧 SQL 的最小反例：同一 R05 在 CUR 写入一条 `usableResearchProduct=false` 的诊断帧，OLD 有另一个 run 且有 `RESEARCH_SOURCE_CONSUMPTION`。在隔离 SQLite 运行底座 `ReportSnapshotR30.rowsets` 同型两条计数，原结果为已产 1、已消费 1；复核口径必须是 CUR 已存帧 1、有值产品 0、当前有效消费 0。再注入当前有效动作，重放两次，当前有效计数恰为 1。绝不将夹具回执写入自然质量账。
3. B11 从注册表枚举完整 29+18+16 个 ID，无重复或漏项。分别在单一固定快照对照正常 UI 入口、报告导出和源查询，归档每行的 ID、source、role/ABI、窗口、输入键、sourceHash、total/shown、有限值或有效边/单元数、状态及失败原因。页面与报告的当前事实不一致则 B11 不签。29 个旧图中 4 个直接使用开奖序列的 R83 图须检验真实 draws，不从“无图缓存”推断空。
4. 给某图注入缺列和读取超时，另图保留真值；给同一底座植入“只有影子命中、REAL 为 0”、多个 role 同一期、缓存有点全 null、古版本缓存坏哈希、页面关闭后旧结果才返回、第一页 8 行但总量大于 8、越界页空但总数大于 0、无可读总数等输入。`A-B11-01b/02b/04a/04b` 应给出相应原因与分母。打开和关闭图表前后，冻结、结算、出生与研究消费业务行数不因页面额外增加。
5. 初次升级保留旧帧及原运行原件，将其作为有明确版本的历史记录读取；新聚合加口径版本，可从固定原件重建并核对。回滚仅停用新派生读法或展示，不改旧冻结与结算原件。设备验收另收正常后台生产后再开页面的同版本检验；自然没有某类产物时，保留等待原因、原始水位、再次触发条件。工程夹具合格与自然资产质量各自记录。
