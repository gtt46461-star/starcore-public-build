# B06：现代策略的独立滚动 learner 与冻结发布

## 交付状态与边界

本批提供完整 Java 后态与可应用补丁。后台学习、独立冻结发布及消费适配均包含在本组 `after/` 中；按 `PATCHSET.json` 的 rolling 组完整合入16个文件。当前已完成源码层接线、44 项真实 SQLite 约束验证和 Java 语法检查；**Android 编译、设备执行、真实原数据库回放、吞吐/内存尾延迟及统计质量验收均为 NOT_RUN**。这份源码交付不是报告软件已修好，也不证明新版本具有高胜率。

本实现复用安装版 `DevelopmentTrainerCP9` 的六类真实数值内核。现代在线学习采用固定 cutoff 的滚动重训；同一策略的参数更新形成独立 learner version。它不伪称创造新的数学机制，不把历史数据计为正式未来数据，也不继承旧模型的资格证书。

## 一、原协议为何必须保留

原 `research_training_protocol_v2.candidate_id UNIQUE`、协议不可变触发器与 `r952_candidate.structural_hash UNIQUE` 保留。原候选 AST、初次开发训练协议、初次 learner cursor、`r967_numeric_training_run` 及已冻结模型均不因滚动训练被替换。

安装版 `ResearchDirectorR952.capturePredraw` 排除 AST3，是因为旧在线 `Model.update` 不支持其批训练协议。该排除条件保留；本批为现代模型增加真正的独立 owner，禁止把 AST3 塞进旧 update 调用冒充已支持。

滚动训练的 task `candidate_id` 使用新 `version_id`，实际策略身份仍通过 VERSION.candidate_id 外键指向原策略。即使旧版本发生输入缺口、版本失败或尚未发布被后继取代，原策略和既有冻结对象不被误判为质量失败。

## 二、生产调用链与确切文件

| 文件 | 实际修改与调用 |
|---|---|
| `r952/ResearchMigrationRegistryR952.java` | 在 core 的 `R343_TRAIN_INFRA_RECOVERY_V1` 后增加 `R343_ROLLING_LEARNER_V1`，同一迁移单元安装 learner 和 publisher schema；ready/早返回必须两者 installed |
| `r952/ResearchDirectorR952.java` | 已验证 COMPLETED settlement、hash、完成时间、原开奖内容的同一 `R952_STRICT_FUTURE_SETTLE` 事务中调用 `RollingLearnerR343.settled` |
| `r952/ResearchTrainingHookRegistryR952.java` | 持久化轮转扩成五槽：LEGACY_OUTBOX、TRAIN_CANDIDATE、FREEZE_CANDIDATE、R343_ROLLING_TRAIN、R343_ROLLING_FREEZE；空轮退出阈值同步改为5 |
| `r952/RollingLearnerR343.java` | 规划、原输入、特征分页、数值切片、checkpoint、版本 head CAS、回执、offer及清理的完整 owner |
| `r952/DevelopmentProductionCP12.java` | 仅开放现有 readSpecs 的包内访问；新增 rollingRowR343 调用原 dataAtoms 和 rawTrainingRowCP11，不复制一套公式 |
| `r952/HistoricalTrainingReceiptCP12.java` | 新增 originalForRollingR343 与原件哈希校验；沿用原 private extract/decode 的严格 PIT 验证 |
| `ResearchTaskQueueR967.java` | 新增精确两 kind 的 expireDependencyR343；仅处理滚动原件缺口或尚未发布版本被后继取代，保留旧 task 终态 |
| `r952/BoundAtomLifecycleR80.java` | 两个绑定父源查询改用同一精确冻结run resolver，保留原位置/资格/32个物化上限和正式义务排除 |
| `ReportDiagnosticEvidenceR64.java` | recentEvidence增加9个恢复/rolling/publication原始元数据表和精确调度/HOLD状态键；下次诊断可逐版本核验 |
| `ResearchCaptureQueueR946.java` | legacy raw-candidate恢复分支同步排除AST3，现代仍走独立frozen reservation/task |
| `DrawDependencyInputCP12.java` | 原有 retention owner 增加 learner 原输入范围引用；热历史仍最多5000行，不把 retained 输入当成第二历史流 |
| 配套 `r952/RollingFrozenPublisherR343.java` | 原始页回执、DONE checkpoint restore/finish校验、独立冻结发布/拒绝/未发布过期及 raw pin |
| 配套 `ResearchEvaluationRunR967.java`、`FrozenCandidateAdapterR958.java`、`GlobalResearchScheduleR932.java` | 通过精确冻结 run resolver 识别新 lineage；不再用 base 最新训练 RUN 否决原来已封存的窗口 |
| 配套 `DatabaseCoordinator.java` | runtime 补丁累积后态上登记全部新操作；不能覆盖 B02 的单 writer/fault backlog 修复 |

接线已在上述完整 Java 中实现，不需要施工者寻找注释位置补空方法。禁止另建 Executor、另一套租约 owner 或直接 SQLite 写通道。构建时必须重跑仓库的实际源码身份生成流程，不能手填相同 source/code SHA。

## 三、五张 learner 表与事务合同

| 表 | 身份与作用 |
|---|---|
| `research_rolling_event_r343` | 每个已接收 COMPLETED issue 唯一；精确 draw/settlement/completedAt 的不可变回执。重复相同原件 no-op，冲突失败 |
| `research_rolling_version_r343` | 同一原策略、predecessor、固定 cutoff、input_first_issue、AST/manifest/input/protocol/code身份；每candidate最多一个 FEATURES/TRAINING版本 |
| `research_rolling_head_r343` | 同策略最近完成的开发模型版本和through_issue。只在完成数值训练的同一事务里对 predecessor CAS；规划等待不提前推进through |
| `research_rolling_feature_r343` | 每版本按row_index连续存储原始 PIT 特征、actual、raw/source/row哈希及原始source JSON；active期间禁止更新/替换/删除 |
| `research_rolling_offer_r343` | 训练完成后不可变发布请求。其兼容状态字面值 `NEEDS_INDEPENDENT_PUBLICATION_ADAPTER` 不表示适配器缺失；真实消费状态由配套独立publication/reject表决定 |

VERSION 状态为 `FEATURES → TRAINING → COMPLETE`。确定性输入/身份错误只使该版本 `FAILED`。固定 cutoff 缺少合法过去 PIT 见证时首先 WAIT，保持原cursor；更晚的真实结算已到达后，可形成 `DEPENDENCY_GAP` 终态与原 queue 的 EXPIRED 回执，让后继 cutoff 重新尝试。这个转换既不把不存在的原件补成数据，也不宣布策略质量失败。

终态模型/receipt/hash/prefix/checkpoint身份禁止改写；raw payload只能按保留合同清空，不可恢复成另一些字节。EVENT、OFFER、VERSION与feature防REPLACE，核心回执永久。每个版本按唯一candidate/cutoff、协议哈希及predecessor绑定。

## 四、每期输入、训练和恢复

1. 实时结算按已验证原件记录 event。进程恢复若发现更晚的原始 COMPLETED settlement，`catchUpOne` 补入最新真实水位；它不为无结算原件的历史伪造事件。没有逐期实时交付的停机区间仍由SCDEV1原历史样本按真实连续期号进入新cutoff训练，不能把补水位数量当作训练步数。
2. TRAIN中的滚动槽持久化交替分配“规划”与“训练”，避免一个等待绑定原件的版本抢占所有新候选规划。总active最多4，含外部原始输入的版本最多1，留出数据自足族的推进空间。候选等待原因落入HEAD.last_reason，按updated_at公平重试。
3. 首次历史开发训练仍使用安装版最多1200训练样本合同。后继滚动版本使用**最近300训练样本 + 1期隔离 + 120验证样本**，另满足manifest.requiredHistory至少20。使用原连续后缀选择器，缺口拒绝，不要求不必要的整段5000历史。
4. 每个版本捕获SCDEV1固定字节、协议和cutoff，单input≤512KiB；事务提交核对原draw generation、canonical/raw source epoch与原策略/HEAD身份。训练过程中输入不随最新数据漂移。
5. 特征owner每页最多32行、256KiB暂存，保存真实dataAtoms/typed graph计算结果。绑定、教师、矩阵及长摘要来自原 `HistoricalTrainingReceiptCP12.extract/decode`；当前teacher不能重算过去标签。
6. 特征完成后复用原CP9 Dataset、normalizer、模型算法、优化器和检查点。300训练样本不因多个epoch变成更多独立样本。FIXED_GRAPH没有优化器步数时记 `STATE_REFRESH`；学习模型记 `NUMERICAL_UPDATE`。HISTOGRAM_TREE在当前CP9的optimizerSteps也可为0，其真实树拟合/节点/模型哈希须单独核验，不能据此认定没有训练。
7. 每个数值切片按task lease、checkpoint hash/generation、feature prefix CAS提交。重启从持久checkpoint及exact queue cursor恢复，不用重新更新原策略旧RUN。complete时同事务写模型/开发receipt、HEAD predecessor CAS、原task COMPLETED和不可变offer。
8. 完成前重验协议原输入范围：读取真实source Draw、dirty、openTime、十数排列，核对canonical V1或V2；rangeHash包含真实issue/openTime/V2身份。任何draw/canonical/retained输入变更都会推进epoch。提交再次CAS epoch。正常新未来行只影响并发CAS，不能把完整旧训练范围当作新数据重写。

开发receipt明确包含 AST、protocol、input、modelkind、manifest、normalizer、weights、训练/验证边界、selectedEpoch、optimizerSteps、CE、Brier、predictionHash、codeRevision、canonicalRangeHash，以及 `newStrategyCount=0`、`formalFutureCount=0`、`realAuthority=false`。

## 五、独立冻结与主链边界

配套publisher有自己的不可变publication/page/reject表。它读取COMPLETE+offer和已完成的原训练task，分页校验原始feature/source/prefix，验证SCDEV1 split/actual标签，恢复CP9 DONE checkpoint并执行finish，要求模型payload、hash、normalizer、validation结果和receipt完全相同。这个检查是**checkpoint与codec恢复重放**，不是宣称已独立从头再次训练。

新frozen取得独立run/epoch/source_artifact、真正未来firstIssue。base训练current()保持原语义；旧冻结对象按自己的精确run解析，不能被新后台learner头打断。旧证书不可传给新模型。每个新frozen仍须独立走CAL、固定未来窗口、paired comparison、质量门和主链资格；发布开发模型本身不授予REAL权限。

容量满时保持已有合格旧窗。后续已完成且尚未发布的旧offer可由唯一发布owner形成 `SUPERSEDED_UNPUBLISHED` 永久回执后释放raw；已正式发布对象不可使用这条路径清掉。此容量策略用于持续滚动，不能误报“被取代版本通过质量门”。

## 六、原件保留与预算

工作raw合计上限64MiB；当前版本/未消费offer/活跃冻结依赖的原件先pin。清理仍在已有TRAIN owner的有界单元执行：每次最多64条feature或一个版本的input/checkpoint。compact身份/结果/拒绝/原件页回执保留，报表必须区分完整raw可用与仅有摘要哈希。

DrawDependency只引用active版本及COMPLETE尚待发布/拒绝版本的 `[input_first_issue,cutoff_issue]` 范围。热表5000行上限不变；已有保留owner把有依赖的原始行移入原retained表。不存在的旧原件仍WAIT，不放宽PIT。

MODEL内存按实际阶段计费；滚动input解码2MiB与数值工作区分生命周期，300样本降低常驻Dataset。publisher也必须先释放SCDEV解码对象再恢复数值工作区。正式通过仍需在128MiB低堆及目标设备实测，不以静态预算代替测量。

## 七、施工与验收顺序

本目录补丁的before是：安装版173 + B05 research_core。因此 `ResearchTaskQueueR967`、`ResearchTrainingHookRegistryR952`、`ResearchMigrationRegistryR952` 的后态完整包含B05修复；不能从173覆盖回去。其余本目录旧文件before为173。发布文件同属 rolling 组；逐文件before/after以本组 `FILES.json` 和总 `PATCHSET.json` 为准。DatabaseCoordinator操作登记随前置runtime组交付。

必须先完成：B02 writer/checkpoint修复、B05四任务精确复训及partial目录分族就绪，然后应用本B06 rolling组全部后态。生成source身份并完成Android构建、迁移升级、强杀恢复、实时结算竞争、前向窗口不被中断及主链授权回归后，才可标记此批施工完成。

| 验收 | 当前证据与剩余门 |
|---|---|
| 核心错误分类 | 纯Java25断言PASS；不是Android运行 |
| 四旧任务保留终态及精确继任门 | 实际DDL/queue SQL 11断言PASS；原CAS包恢复尚待设备 |
| learner不可变/REPLACE/回滚/CAS/epoch | 实际production DDL和HEAD SQL，44断言PASS；SQLite synthetic，不等于生产owner调用已验证 |
| 发布数值重放 | 六kernel、60项结果/拒绝合同及62,136次真实CP9转移PASS；未改CP9和原文提取Model/Modern/codec配测试平台绑定，不是Android/数据库owner测试 |
| 绑定父源SQL | 两个实际Java SQL构造器编译并在SQLite prepare通过，绑定参数4/3；完整资格语义由配套publisher fixture验证 |
| Java语法 | 全部本批源码parse通过；不等于类型/Android构建 |
| 诊断类型检查 | 全源+仓库历史Android host stubs运行，环境缺API导致整体不能编译；用于查出新增接口错误，不能作Android PASS |
| Android assemble/unit/instrumentation | NOT_RUN，需完整SDK/原设备 |
| 每事务边界强杀/队列恢复/原件恢复 | NOT_RUN，必须真实数据库副本与CAS |
| 每个现代族本次在线状态变化 | NOT_RUN；FIXED/learned/teacher实际原件资格各自计数 |
| 旧frozen窗口/证书不中断 | 必须比对旧对象逐列、旧待结算样本仍可读、所有SQL消费者一致 |
| 新模型真实主链出手 | 仅当未来质量/校准/风险/授权各门自然通过；不得用测试数据、重置门槛或老证书制造成功 |

施工方必须回传源码commit、APK SHA、安装包版本、migration/schema attestation、实际checkpoint恢复与吞吐原件、新报告快照。未完成门保持NOT_RUN/WAIT/FAIL的真实状态，不得用“代码写了”“有队列行”“任务完成计数”替代真实进展。

## 八、本次续接自检追加修正（候选代码审查，不是安装版新增报告事实）

1. **已知等待不再变成writer异常。** 安装版`DatabaseCoordinator.runAtomicWrite(..., fallback)`会捕获回调异常并返回fallback；旧候选代码在事务内抛`Wait`，外层catch无法接到，任务仍会占有45秒租约。现对冻结容量满、未来target变化、source epoch变化、滚动工作预算不足，在原writer事务内按原lease/cursor调用`ResearchTaskQueueR967.yield`；不写模型、publication、quality或REAL。真实完整性错误、失效CAS、未知异常仍失败回滚，不能统统视为WAIT。
2. **回收不会被待训任务永久挤掉。** 原TRAIN owner的计划轮先给一个有界清理机会；清理最多扫描16个版本、删除64条feature或清一个终态payload，不新增线程/owner。`r343_rolling_cleanup_after`保存扫描游标，跳过有效冻结pin并继续扫描后继版本；到尾回绕。当前HEAD模型、活动版本、未发布offer和有效冻结原件仍保留。
3. **全局payload队列满时提前返回。** 在同writer中、版本或新任务插入之前，按原队列的4096 payload边界预检，22个MAINTENANCE控制槽仍保留。不给正常背压制造约束/回滚故障。
4. **回归范围。** 新增Java方法抽取+真实SQLite桥接回归，51项断言通过，其中包含旧候选六种正常等待触发异常、旧固定16行扫描无法越过pin的反例；修正后精确等待码、lease释放、cursor不变、无新冻结、真实错误不降级、扫描回绕与保护原件均通过。平台Cursor/协调器外框、冻结元数据/codec/资格判定使用测试适配，不能标Android/真机/自然质量PASS。
