# R75 施工范围与真实运行证据复核

本文件是对当前工作树、原始施工要求和用户上传 **R74** 报告的范围核对，不是 R75 安装后的验收证书。输入版本 `V57.21.2.0.74-R5.4.2.74-1013-O09`，快照 SHA256 `6eded8491d74af347080b90e90be6d7eab217ad0524c9eca95d3aaa268747ad1`。报告表下列计数来自完整元数据导出，不从页面文案或函数存在推导执行成功。

## 施工合同覆盖到哪里

权威原件为 `construction/authority/42_CORE_TASKS_AUTHORITY.json`、`30_ENHANCEMENT_TASKS_AUTHORITY.json`、`FULL_ACCEPTANCE_MATRIX.csv`、`FINAL_100_PERCENT_GATES.json`；综合细项为 `construction/r997/REQUIREMENTS_LEDGER.csv.gz` 与 `CARD_LEDGER.csv.gz`。原件中的旧 `NOT_STARTED`/`DESIGN_NOT_IMPLEMENTED` 是设计当时状态，不能直接当作当前代码未实现。

必须区分两套历史编号。最初已读取的本地 `docs/5.4.2/01_TASKS/tasks_batch1…7*.json` 是较早的七批任务；该套增强 30 卡 README 的“不新增第 8 批”只约束那一版方案。当前 checkout 是部分物化，不能用本地文件搜索推断完整仓库缺少第 8、9 批。随后对完整 Git index 核查，并从仓库 `gtt46461-star/eth-k3-v43-nova` 的固定原提交 `0dba7800a68f33dbe8617b71d1ce5f6318dfe16e` 实际读取 `construction_state/optimized299/` 下的 O09 交接、O08/O09 交付说明、HANDOFF 和逐条关闭修正表，确认后续 **O01–O09 施工方案及应用记录真实存在**，无需用户重传这些已在仓库的材料。远端返回的 blob SHA 与本地 index 一致，读取路径及身份见 `report_acceptance_r74_input.json.construction_plan_provenance`。

随后通过 Git blob `f65440cadbda5ce086c890f896e9892f98bf320d` 读取完整 `manifest.snapshot.json`（普通 file 接口对此大文件只返回空内容，不能据此当空方案）。manifest 的准确名称为：O01 固定基线与构建生成器；O02 运行期限与严格事务；O03 出号冻结与结算恢复；O04 原子矩阵数值闭环；O05 创造进化与 CAL 身份；O06 证书资格与真实主链；O07 检测报告与存储保留；O08 4200 分子有界导入；O09 全能力真实消费者。O08 有 10 个文件操作；O09 没有新文件操作，但有 BUILD/CONTRACTS/REGRESSION/RUNTIME/NATURAL 及整条原始要求的消费者验证，**零操作不等于空批次**。它们不是下方九个审查分类，也不能简单替换早期七批的编号。

该原 manifest 实际规划到 O12，另外 O10 检测与需求全量回归、O11 设备运行与性能、O12 签名发布与最终交接；当前交接的已应用范围只到 O09。这里保留这一规划事实，不把后续游标当作已经执行，也不据此扩大用户本次要求的范围。

上述固定提交保留的 `HANDOFF.json` 将 O01–O09 均记为 APPLIED，但 O01–O08 工程证据的有效状态是 STALE_SOURCE_REVERIFY、O09 是 FAILED，runtime 为 NOT_RUN、natural 为 NOT_PROVEN；其 latest_release 字段仍引用历史 release65，并明确 overall_1_9=NOT_CLOSED。这是**历史台账原值，不是 R75 的运行判定**。`O09_CURRENT_DELIVERY_HANDOFF.md` 保留 release60 并指向 release63；所读 release63/65 说明也明确完整 1–9 未闭合，不能把文件名“CURRENT”误当 R74/R75 最新身份。

`evidence/O09/BATCH_CLOSURE_592_REPAIRED.csv` 实际有 592 行、14 列：全部 `updated_whole_engineering_verification_stage=O09`，全部 `updated_implementation_status=NOT_RUN_ON_FINAL_COMPOSED_SOURCE`，并要求原完整条款在同一源码上的合取验证。592 是纠正过早整条关闭的子集，**不是新增要求、不是全部 1832 的当前失败数，也不是 592 项已修复通过**；局部卡收据仍只能证明对应组件。

综合台账有 **1,832 条要求、299 张文件卡**。要求来源为用户指令 647、蓝图 665、问题矩阵 26、测试目录 87、历史要求 343、策略 JSON 17、施工序列 47；语义分类为计划制品 530、精确支持 860、等价替代 426、历史参考 16。文件卡计划处置为 MODIFY 201、ADD 79、KEEP 11、DELETE 8，均不等于本轮已经执行的变更数。

以下是按台账 `module` 汇总的审查分类，**不是重新编号的施工批次**，总和为 1,832：

| 审查范围 | 要求数 | 原 module |
|---|---:|---|
| 期号、历史、PIT、时钟、结算 | 184 | ISSUE/HISTORY/TARGET/PIT/CLOCK/SETTLE |
| 原子数学、类型图、准入 | 206 | ATOM/ATOM_MATH/AST/ADMISSION |
| 矩阵 | 229 | MATRIX |
| 训练、校准、冻结、赛前、Future | 153 | TRAIN/CAL/FREEZE/PREDRAW/FUTURE |
| 创造、研究、进化、退役 | 125 | CREATOR/RESEARCH/EVOLUTION/RETIRE |
| 证书、主链、门控、连挂、状态 | 199 | CERT/MAINLINE/GATE/STREAK/STATE |
| 后台、Owner、接口、队列、线程、恢复、资源、存储、schema、后处理 | 169 | BACKGROUND/OWNER/INTERFACE/QUEUE/THREAD/RECOVERY/PERF/MEMORY/RETENTION/SCHEMA/POST |
| 界面、报告、原因 | 72 | UI/REPORT/REASON |
| 文档、根约束、调用、序列、施工卡、补丁、测试、构建 | 495 | DOC/ROOT/CALL/SEQUENCE/CARD/PATCH/TEST/BUILD |

R997 台账当前状态仍是要求 `已修改待验证=62、未检查=1770`，文件卡 `已修改待验证=3、未检查=296`，没有整条已验证完成。**“未检查”是语义及运行证据未核实，不能改写成“未施工”或“已完成”。** 本轮局部测试不能把关联的复合父要求整体勾为通过。

## 原子、矩阵、深度学习和进化实际在哪里

以下源码以 `app/src/main/java/com/ethk3/novaonepro/` 为根。列出的是可核查的实现与入口；右侧单独列出上传报告实际执行证据。

| 能力及原要求示例 | 当前真实实现与调用 | R74 报告事实与缺口 |
|---|---|---|
| 原子世界；USR-05-006、USR-12-001、USR-13-001 | `r952/AtomCatalogImportCP12` 导入，`r952/AtomSpecCP4`/`AtomEvaluatorCP4`/`TypedAtomMoleculeGraphR930` 解析与计算，`r952/AtomAdmissionCP12` 质量准入；`r952/DevelopmentProductionCP12.readSpecs/dataAtoms` 接入训练特征 | `research_atom_spec_v2=25350`，共 28 family；25220 DISCOVERED、130 REQUIRES_BINDING。catalog COMPLETE；`research_atom_admission_cp12=0`、`research_atom_binding_v2=0`。目录已导入，尚不能声称所有原子已完成质量准入或带来预测增益。 |
| 分子与公式；USR-05-007、USR-14-001 | `r952/MoleculeSeedPageCP12`、`CandidateAstR952`、`CandidateCompilerR952`、`ModernFamilyFactoryCP12.prepare`；实际 typed AST/数学图编译，不以 family 名称代替计算 | `research_molecule_import_cp12` 和 checkpoint 均 COMPLETE，expected/next=4200。`research_family_creation_cp12=11` 且全部 PROPOSED；15 family cursor 的 next_variant 全为 0。尚未证明这些提案已变成完成训练的正式冻结候选。 |
| 20 类矩阵；USR-05-008、USR-15-001、BPR-0112 | `r952/MatrixProjectionCP12` 实现转移、条件转移、共现、频率、遗漏、原子目标/状态、候选窗口/误差/相似/多样性、父子、漂移、置信、时延、教师、规则健康、冻结、失败、残差；Director MATRIX 槽进入 `MatrixSnapshotBusR952.drainOneCP4`，通过 `MatrixSourceRouterCP12` 取真实源，checkpoint/ACK 事务发布 | 旧 `r952_matrix_snapshot=463`、`r952_matrix_feedback=421`，另 `research_matrix_regime5_cp12=330`；现代 `research_matrix_spec_cp12`、checkpoint、terminal 均为 0。不能说完全没有矩阵，也不能把旧 snapshot 数冒充现代 20 类全部运行。 |
| 现代数值训练/深度学习；USR-05-012、USR-05-013 | Director TRAIN 槽 → `ResearchTrainingHookRegistryR952.drainTrainingSlotCP11` → `DevelopmentProductionCP12.prepareBatch` → `DevelopmentTrainerCP9`；MLP D-32-16-10 有实际反向梯度与 Adam，linear/OVR 用 SGD，另有树、固定图、专家组合；批量 32、最多 20 epoch、独立 120 行 validation、训练归一化与 checkpoint 续跑 | `research_training_input_v2`、protocol、feature cursor、development receipt、artifact identity 均为 0。源码存在且有主机测试入口，R74 手机上的现代训练完成链尚未建立。旧 `ResearchTinyDeepLearnerR909.updateOne` 明确返回 `RETIRED_CP12_MODERN_TRAINING_OWNER`，不能把该旧入口当现代训练已完成。 |
| 已经真实执行的旧在线学习 | `ResearchDirectorR952.prepareTrainingLabel/commitTrainingLabel` 与 `CandidateCompilerR952.Model`，结算后从冻结输入生成更新并原子提交 artifact/cursor | `r952_candidate` 唯一 `R952-C-c41f6a28c10408b48b88` 为 AST1 RECENCY_BLEND；463 赛前记录、421 已结算；`r952_artifact=413`，payload 含实际 `R974_RESIDUAL_MLP_10_8_10_V1` 权重，training_kind=LEGACY_ONLINE。对应 outbox 413 COMPLETED、1 RUNNING、7 PENDING。由此可确认有真实数值学习，也可确认尚有未消费任务；不能概括成全无训练。 |
| 定向变异、融合、创造；USR-05-028、USR-18-001、HIST-E04D-01 | `ResearchFeedbackCP12` → `r952/EvolutionControllerR952.drainActionCP12` → `MutationGraphCP12.propose`；最多 32 次有界 typed 搜索，包含窗口/lag/原子/权重/阈值/子树/结构/交叉/双专家融合/新 family，编译及结构、语义去重；子代重新训练/冻结/校准 | `research_action_v2=11` 全 PROPOSED，action result binding=0、`r975_evolution_review=0`。BANDIT/EXPERT/ENSEMBLE family cursor 的 last_reason=WAIT_FROZEN_TEACHER。缺合法冻结教师不能伪造其预测；变异成功只证明产生不同合法图，不能保证下一代必胜过上一代。 |
| 正式主链与 REAL；USR-05-020、BPR-0322 | 冻结候选 → `CandidateCalibrationR932` → `FixedResearchWindowR932`/`FixedWindowCertificatePublisherR932` → `CertificateBundleServiceR929` → `V51RuntimeOrchestrator` 的 `CertifiedPredictionR974.prepare/evaluate` → 唯一 `DecisionGateEngine` → `SettlementEngine`；正式评估与开发训练样本分开 | 574 个校准 study：388 COLLECTING、186 BLOCKED_INVALID_INPUT；累计 settled_n=7094，但单 study 最大只有 166，不能拿跨候选总数凑 241。186 failure 全为 R969_CAL_EVALUATION_SCHEDULE_GAP；cal_model、calibration_quality、fixed_pair/window、certificate、mainline_contribution 全 0。现证据首先说明连续捕获/校准链未完成，不能直接下结论“最佳公式胜率差”。ACTIVE 的 runtime bundle 仅是六组件快照，不能冒充已有合格研究候选真实贡献。 |

## 本轮证据能验收到哪一层

当前调度入口是 `AutoFetchService` 单研究 callback → `ResearchLearningContinuityR912` → Director 的 TRAIN/POST/MATRIX/RESEARCH/EVOLVE/IMPORT 六槽。R75 已修改抓取到期仍给旧期时的择源、研究与维护 callback 隔离、轮盘剩余预算准入、满预约容量早返、非法负分候选隔离、revision 孤儿清理有界扫描等具体阻塞；相关 `verification/r75` 用例验证的是这些限定行为。

最终生产源 `452854cc8a82933d5547e1b81789beb22dbe4c49` 的正常 strict release 构建及签名/资产/生成身份核验已通过，51项 unit 零失败，lint 仅2个 Hint，详见 `evidence/final_build_manifest.json`。最终主机字节码回归索引见 `ACCEPTANCE.md`：这些限定检查已完成，不再记作“等待最终classes”。Android CI `38359778200` 已结束：GitHub 步骤回执确认编译及 Android 滚动/回滚、独立运行验证成功，仅验收文件上传失败；整个 workflow 仍为 FAILURE。日志下载接口连续失败，原始 Android 收据尚未恢复，具体上传失败原因未取得，见 `evidence/final_android_ci_step_receipt.json`。用户手机、自然资格和完整1–9验收并未因此通过。用户随后同意继续准备现场验收包，本次提供已核验身份的 R75 APK 供手机验收，见 `FIELD_ACCEPTANCE.md`；最终业务交付仍未通过。

校准失败反馈修复已完成最终字节码局部主机验证，见 `evidence/final_mainline_calrepair_dirichlet_ledger.json`：33项捕获主链、119项当前校准修复（另2项R74旧基线复现）及13项Dirichlet调用通过。实际产品 `ResearchFeedbackCP12` → `CalibrationRepairR75` → EVOLVE/Director 从合成的不合格校准诊断生成不同 typed AST3，跨 JVM 登记 NURSERY 子代、不可变训练输入/协议和 TRAIN 任务；开发数据截止严格早于原 CAL，诊断选择边界单独绑定。测试还覆盖写锁重试、父版本变化、非法截止、ACK 归档后幂等；原 241 条校准观测不改，未产生正式观察、Future 继承、证书或 REAL。追加 `evidence/final_training_repair.json` 的 17 项验证通过：真实 TRAIN/FREEZE Owner 经 115 个短脉冲计算 600 行特征（480 train、120 validation），生成唯一 artifact、development receipt 和 FIXED_GRAPH 冻结模型；trainedUntil=`202609230421`，早于原 CAL fitFirst=`202609230442`，新冻结 createdIssue=`202609230683`。所以该合成主机修复链已到新子开发训练/验证及冻结，不停留在排队；它仍不能证明新子的独立校准/正式 Future/自然增益或全部模型族完成，也不能把合成诊断当作用户报告已经出现的质量判定。

普通现代训练也已在最终字节码完成：实际报告AST和历史经公共Owner、checkpoint重启，计算1320行特征（1200 train、120 validation），得到真实MLP_D32_16_10 artifact/receipt并冻结；另66项预算、10项outbox和11个普通family协议字节一致性检查通过，见 `evidence/final_training_manifest.json`。这补充证明代码会执行数值训练及冻结，输入在独立主机库，不能改写上表R74手机零完成记录，更不能推导自然预测性能或所有family均完成。

现代矩阵新增 `r952/MatrixRegistrationR75`，由 `MatrixSnapshotBusR952.drainOneCP4` **持久交替 registration-first 与 consumption-first**：发现 quantum 有界，每页最多 8 个 spec；5 个持久阶段从历史默认分区、实际 exclusive 时延、冻结模型/原子/教师、共同 Future 同伴与谱系发现定义，数值输入仍由既有 SourceRouter/Projection/ACK Owner 产生。登记和消费分别保留进度，不能只把登记放在每次已耗尽预算的尾部。首次默认历史窗口的真实下界与 spec 同事务固定，有限尾窗须与全历史投影数值一致，旧 manifest 的原始源选择保持。

矩阵 **551 项最终字节码主机检查已通过**（393 公平/预算/游标、57 尾窗/bootstrap/指标等价、101 源权限与负例隔离），见 `evidence/final_matrix_result.json`、`final_matrix_class_identity.json` 及对应日志。实际 SourceRouter→自动 spec→MATRIX Owner→90 个 SQLite checkpoint→`MatrixEvidenceReadCP12` 消费覆盖 18 类拉取源；Freeze/Failure 仍由原事件源处理，canonical 76 项最终复跑亦通过。另 `evidence/final_matrix_owner_summary.json` 的 67 项使用本轮最终公共 TRAIN/FREEZE Owner 产出的NEURAL和维修子代数据库副本继续运行，未手插矩阵源，跨 JVM 恢复并保持 CAL/Future/证书/REAL 业务计数不变。最终正例不编译生产源码、无产品overlay，运行前后身份一致；旧开发overlay收据仍保留并明确区分。**用户手机及自然资格仍待验**，不是手机全部 20 类已经闭环。

同轮还实测定位 `MatrixSourceRouterCP12` 的 lag 前置检查：旧相关 `COUNT(prior.issue<d.issue)` 对每个候选重复累加，空 regime 在大历史表退化为 O(N²)。改为一次独立查询 lag-th clean issue 的索引阈值，再沿用原相邻期连续性/PIT/源证明；`evidence/matrix_lag_sql.json` 保留 5000/100000 行精确 SQL、查询计划和每规模 63 个真值比较，包含 dirty、lag 大于历史及稀疏源。100000 行旧全空扫描明确按 VM 步数中断，不能假称完成。成功登记 spec 不等于已有合法输入，缺赛前绑定、冻结教师或共同正式 Future 的类型仍必须 WAIT；上表 R74 的零计数保持原始事实，不被新代码或主机 fixture 改写。

另已核实原子定义与质量资格的混淆：旧 `EvolutionControllerR952.registry`/`MutationGraphCP12.usable` 排除了 DISCOVERED，报告全部普通原子恰处于该状态，导致研究没有可选变异定义；`ModernFamilyFactoryCP12` 和 compiler 原本允许这类定义参与开发。R75 的调整只允许已验 hash、非 binding、非 RETIRED 的 DISCOVERED 定义生成研究提案，不改原子 state，不赋予合格资格。`usable` 是 private，生产调用 `propose` 只有研究 Evolution 和新 CalibrationRepair；Director 仍仅登记 NURSERY、future_n=0、may_create_real=0 与训练任务，正式消费者仍须通过绑定级 `AtomAdmissionCP12` 和证书。此边界已只读复核，上述校准修复收据同时验证 DISCOVERED 不晋级、无绑定 OOS 资格仍拒绝；手机持续运行和实际效果仍待验收。

独立事务观测已复核：`DatabaseCoordinator` 保留原 own/external transaction 语义；`RuntimeResourceAuditR927` 新 DBTX75 buffer 有 128 条/96 字符操作名上限，只在旧 stage 完成后使用剩余空间追加。36 项最终字节码真实主机 SQLite 测试包含 begin/commit 争用、rollback、caller-owned transaction、异常不替换原结果和真实矩阵消费者的 ordinal/receipt/proof 不变，证据在 `evidence/final_bundle_result.json` 与 `final_bundle_run.log`。这不等于 Android 冷启动时延已达标。

归档开发证据在 `retention_evidence/manifest.json`，百万行旧查询扫描失败复现保留。追加最终复跑 `evidence/final_retention_manifest.json`：锁定源提交 `452854cc8a82933d5547e1b81789beb22dbe4c49` 的 strict release Java 字节码，20 项 revision 游标/并发/回滚/有界扫描及 42 项 SQLite/native 归档回归全部通过。只编译原 fixture/Android适配器，所有前置classpath均无产品类碰撞，两轮前后产品字节码 hash 不变；使用两个独立新数据库，未覆盖开发记录。报告中的 `STALE_PAGE_RELEASE_CHANGED` 是跨版本的历史请求失效记录，随后有新请求文件；它本身不能被说成当前一直卡死，也不能被说成归档已成功。报告 `research_evidence_archive_v2=0`，仍缺升级后手机归档落地与空间回收证据。

全部 6 FAIL、8 WARN、20 NOT_PROVEN 的逐检查映射见 `report_acceptance_r74_input.json`。最终必须保留以下未完成验收边界：

已有 r71 运行8组、r72领域9组和certificate→DecisionGate→trade→settlement 5组合成主链在最终字节码回归通过，见 `evidence/final_regression_manifest.json`。这里的REAL交易/结算是隔离测试库内的合成资格场景，用于验证真实Owner调用和拒绝边界；没有成为用户数据库的证书、自然样本或手机真实出手，不得以此关闭下一条自然主链义务。

- 同一最终安装包在手机持续自然开奖，记录源结果实际可用时刻、首抓、落库、结算、分析完成、倒计时显示；主机模拟源不能证明真实服务一定在剩余 55 秒发布。
- 现代候选实际完成 input/protocol/checkpoint/training artifact/freeze；中断、重启后持续进度；20 类矩阵真实生产和消费回执；逐期任务收敛不能由一次轮盘测试推断。
- 相同冻结候选独立连续校准、固定正式 Future 窗口、质量/稳定性合格证书、正权重主链贡献、真实决策及其自然结算。不得补造历史赛前样本，不能以总训练量或总世代数替代。
- 屏幕开关、HOME/任务移除、后台恢复、覆盖安装与迁移、原签名/版本/源码包同源、低空间和真实回退，及 1,832 条复合要求/299 卡的逐条适用验收。当前没有这些完整新版本现场证据。

`FINAL_100_PERCENT_GATES.json` 明确适用项存在 FAIL 或 NOT_RUN 就不能标为 100%。编译通过、unit/gate 通过、目录与表已存在、旧历史收据，都不能替代整套业务验收或“每代一定更强”的效果证据。
