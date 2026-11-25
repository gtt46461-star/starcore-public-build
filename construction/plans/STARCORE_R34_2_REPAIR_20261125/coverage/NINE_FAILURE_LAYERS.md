# K3九层失败诊断：事实源、归因实验与主链接入

RSEC-066的九层诊断原件与施工接入；3个静态flag BLOCKED和6个summary UNAVAILABLE，不是九种已证算法缺陷。

**批次边界：** B06/B08/B09按实际冻结模型核原训练/资格合同，缺件在所属批次补；B11仅只读诊断与报告接入，不新增训练owner，不因静态Artifact false否定动态bundle。

原件统一来自 `REPORT_DETAILS.txt:72`，检查ID `RSEC-066`。本附录沿用 ROOT-02、RES20 等原问题ID，不使用重排后的施工问题编号。

| 原层 | 原状态 | 原静态诊断原因 |
|---|---|---|
| NUMBER 号码概率面 | BLOCKED | 静态 V51*Artifact.PROVEN（RANK另含RANK_ORDER_PASSED）为false，报告输出BLOCKED；不代表已读取当前动态模型原件。 |
| RANK 候选排序 | BLOCKED | 静态 V51*Artifact.PROVEN（RANK另含RANK_ORDER_PASSED）为false，报告输出BLOCKED；不代表已读取当前动态模型原件。 |
| THIRD 第三号码边界 | UNAVAILABLE | 旧runtime summary key未读到持久值，报告输出UNAVAILABLE；不能确定该机制运行错误或数值无效。 |
| WINDOW 窗口选择 | UNAVAILABLE | 旧runtime summary key未读到持久值，报告输出UNAVAILABLE；不能确定该机制运行错误或数值无效。 |
| REGIME 状态/漂移 | UNAVAILABLE | 旧runtime summary key未读到持久值，报告输出UNAVAILABLE；不能确定该机制运行错误或数值无效。 |
| MOTIF 局部模式 | UNAVAILABLE | 旧runtime summary key未读到持久值，报告输出UNAVAILABLE；不能确定该机制运行错误或数值无效。 |
| RELATION 跨位关系 | UNAVAILABLE | 旧runtime summary key未读到持久值，报告输出UNAVAILABLE；不能确定该机制运行错误或数值无效。 |
| CONSENSUS 多专家共识 | UNAVAILABLE | 旧runtime summary key未读到持久值，报告输出UNAVAILABLE；不能确定该机制运行错误或数值无效。 |
| CALIBRATION 选择后概率校准 | BLOCKED | 静态 V51*Artifact.PROVEN（RANK另含RANK_ORDER_PASSED）为false，报告输出BLOCKED；不代表已读取当前动态模型原件。 |

## 共用身份与职责合同

诊断过去某一期时，先固定该期canonical/compiled/冻结payload内的模型身份。`ModelBundleRuntimeR932.forEvidence(hash,target,at)` 内部按当前墙钟检查可用性；历史只读回放应复用已存在的 `forHistoricalEvidence(hash,target,frozenAt)`，按当时冻结时间检查，`ModuleAblationR932`已有该调用。不能以诊断时的current指针替代，也不能把今天过期认定为当年无效或借历史回放恢复今天REAL资格。源码存在动态bundle读取，静态种子PROVEN=false不能否定动态模型。原件取不到就保留UNKNOWN。

诊断输出至少包含：`target_issue / canonical_hash / frozen_bundle_hash / component_hashes / model_hash / source_commit / numerical_abi / certificate_domain / gate_version / evidence_role / input_hash / training_or_evaluation_receipt / settlement_hash / fact_status / hypothesis_id / experiment_id / confidence_kind / evidence_scope / first_missing_dependency`。

训练仍使用模型manifest绑定的原owner及协议；现代候选已有 `DevelopmentProductionCP12.prepare/prepareBatch`、`ResearchTrainingHookRegistryR952.drainTrainingSlotCP11` 和 `ResearchDirectorR952.commitPreparedCandidateCP5`，旧R26/SU沿原路径。它们是适配入口，不能据此认定所有上游模型共用一个训练器。

## 1. NUMBER — 号码概率面

**原状态**：BLOCKED；原confidence=1.0，仅为诊断输出。原字段Hash=`188a8c07b32305677574b8281e3fa23d00fb91928b496e47af2903fc7721746f`。

**原Reason**：Strict OOS artifact proof is missing。

**原NextTest**：Train/freeze NumberQ; compare top-k recall and Sinkhorn parity.

**不能据此推定**

- 静态工程种子PROVEN=false不能否定该期动态bundle是否有已训练模型。
- 单一miss或10×10矩阵非空不能证明模型数值失败、预测优势或资格。
- 本报告confidence是静态诊断输出，不能作为算法根因的统计置信；读取失败、缺原件和实际失败必须分开。

**动态事实源**

- canonical中该期bundleHash/modelHash、target/base、历史cutoff及预测时点。
- 该冻结bundle的number/gate组件、训练协议/数据hash/参数hash、Result.status/artifactProven/projection.productionValid及10×10概率面。

**既有生产owner**

- `app/src/main/java/com/ethk3/novaonepro/V51PermutationNumberModelEngine.java:220` — `predict`。
- `app/src/main/java/com/ethk3/novaonepro/V51PermutationNumberModelEngine.java:166` — `Result.productionModelProven`。

**归因实验及所属批次**

1. 在B06/B08对应真实训练路径复核开发、校准与正式未来角色；B11只联查原回执。
2. 固定同目标同样本，对实际number模型、频率/均匀基线及去模型/去gate对照计算top-k与Brier，并验归一化、排列/Sinkhorn约束。
3. 数值相同则记录无贡献；有提升仍须通过该证书域原合同后才能发布替换。

**接入主链**

B11诊断/报告只读消费既有V51RuntimeOrchestrator→V51Repository的已提交冻结事实与SettlementEngine的exact结果。同一类型化事实由B08已有ResearchFeedbackCP12/研究owner独立于UI生成合法动作并交原Director消费；B11只联查这些原动作回执，不新增或触发入队。新模型训练、冻结、资格和发布仍在所属owner，下一期由原selector/DecisionGate读取，绝不回写当期预测或授权。

**原问题关系**

- ROOT-02：修正静态flag/summary关键词诊断，建立当前冻结模型事实源与类型化证据。
- RES20：可验证失败或研究假设进入真实action/child并取得结果回流；证据不足允许NO_ACTION。
- RES07：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES10：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES23：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES24：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。

**验收**

- 报告的target/bundle/组件/模型/证书域与canonical一致；换版本后不能沿用旧PROVEN或旧根因。
- 每层给出事实、假设、反证和未取证边界；缺输入/读失败保持明确状态；关键字和count不单独产生数值因果结论。
- 实际实验能由原冻结输入重放或由新增合法未来验证；研究创建与消费原键可对账，诊断不触发主链重算或改变REAL权限。

**保留**：保留成熟100号码/1200动作空间、工程SHADOW、现有窗口/状态/专家/排序/校准实现、真实旧训练与四证书域门槛；不将静态种子标志改true。

## 2. RANK — 候选排序

**原状态**：BLOCKED；原confidence=1.0，仅为诊断输出。原字段Hash=`04dd7351eac06dd82af4b9fffe376070b7f865e47b594e7a46fb63f9eb2fd84a`。

**原Reason**：Strict OOS artifact proof is missing。

**原NextTest**：Run same-issue groupwise OOS Rank1/MRR/NDCG/pairwise stability.

**不能据此推定**

- 静态ranker标志false不等于当前bundle排序器未训练。
- 候选按分数排了序、rank1命中过或排行榜好看，均不能证明学习排序优于固定基线。
- 本报告confidence是静态诊断输出，不能作为算法根因的统计置信；读取失败、缺原件和实际失败必须分开。

**动态事实源**

- 该target完整1200动作identity与顺序、rawActionP/rankScore/meta输入hash、实际ranker组件和selector身份。
- RankResult、rankOne与canonical选中动作的一致性；训练/选择/确认分段与样本数原件。

**既有生产owner**

- `app/src/main/java/com/ethk3/novaonepro/V51ActionRankerEngine.java:49` — `rankDetailed`。
- `app/src/main/java/com/ethk3/novaonepro/V51ActionRankerEngine.java:120` — `rankOne`。

**归因实验及所属批次**

1. 由B06/B08实际ranker训练owner按同issue分组完成开发与OOS比较，不将同一期1200动作当1200独立开奖。
2. 比较同目标Rank1、MRR、NDCG、pairwise和排序稳定性，并保留固定排序/去学习器对照。
3. 在B09核原证书/选择合同；B11展示当前冻结排序证据和缺件，不启动新训练。

**接入主链**

B11诊断/报告只读消费既有V51RuntimeOrchestrator→V51Repository的已提交冻结事实与SettlementEngine的exact结果。同一类型化事实由B08已有ResearchFeedbackCP12/研究owner独立于UI生成合法动作并交原Director消费；B11只联查这些原动作回执，不新增或触发入队。新模型训练、冻结、资格和发布仍在所属owner，下一期由原selector/DecisionGate读取，绝不回写当期预测或授权。

**原问题关系**

- ROOT-02：修正静态flag/summary关键词诊断，建立当前冻结模型事实源与类型化证据。
- RES20：可验证失败或研究假设进入真实action/child并取得结果回流；证据不足允许NO_ACTION。
- RES07：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES23：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES24：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。

**验收**

- 报告的target/bundle/组件/模型/证书域与canonical一致；换版本后不能沿用旧PROVEN或旧根因。
- 每层给出事实、假设、反证和未取证边界；缺输入/读失败保持明确状态；关键字和count不单独产生数值因果结论。
- 实际实验能由原冻结输入重放或由新增合法未来验证；研究创建与消费原键可对账，诊断不触发主链重算或改变REAL权限。

**保留**：保留成熟100号码/1200动作空间、工程SHADOW、现有窗口/状态/专家/排序/校准实现、真实旧训练与四证书域门槛；不将静态种子标志改true。

## 3. THIRD — 第三号码边界

**原状态**：UNAVAILABLE；原confidence=0.0，仅为诊断输出。原字段Hash=`6b79dd3575f6e6884abccc9ba90ac2200ea6630bf2c2af228348ceff0a35e0c9`。

**原Reason**：Third boundary decides whether final K3 loses its marginal candidate。

**原NextTest**：Audit rank3 vs rank4 margin, miss migration and after-draw counterfactual.

**不能据此推定**

- 旧v51_third_boundary_summary为空仅证明未取得该key值，不证明rank3选错。
- 开奖后替换rank3能命中属于事后反事实，不是该期真实命中或可立即改写的策略。
- 本报告confidence是静态诊断输出，不能作为算法根因的统计置信；读取失败、缺原件和实际失败必须分开。

**动态事实源**

- 该期冻结K3、合法第四边界候选、rank3/rank4概率或分数、thirdMargin、候选全集与exact模型身份。
- 同target authoritative settlement和实际号码；执行/数据/缺冻结先分流。

**既有生产owner**

- `app/src/main/java/com/ethk3/novaonepro/K3ActionSpaceEngine.java:247` — `validateDetailed`。
- `app/src/main/java/com/ethk3/novaonepro/K3ActionSpaceEngine.java:505` — `validFourthBoundary`。

**归因实验及所属批次**

1. 对合法冻结数据计算边界替换的遗漏迁移、支持与同目标反事实，只用于下一研究版本。
2. 通过RES20为边界规则变异建立独立action/child，走既有EVOLVE/TRAIN。
3. 父子同目标配对和独立确认后判断是否采用；固定K3不改成K4增加命中。

**接入主链**

B11诊断/报告只读消费既有V51RuntimeOrchestrator→V51Repository的已提交冻结事实与SettlementEngine的exact结果。同一类型化事实由B08已有ResearchFeedbackCP12/研究owner独立于UI生成合法动作并交原Director消费；B11只联查这些原动作回执，不新增或触发入队。新模型训练、冻结、资格和发布仍在所属owner，下一期由原selector/DecisionGate读取，绝不回写当期预测或授权。

**原问题关系**

- ROOT-02：修正静态flag/summary关键词诊断，建立当前冻结模型事实源与类型化证据。
- RES20：可验证失败或研究假设进入真实action/child并取得结果回流；证据不足允许NO_ACTION。
- RES21：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES24：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。

**验收**

- 报告的target/bundle/组件/模型/证书域与canonical一致；换版本后不能沿用旧PROVEN或旧根因。
- 每层给出事实、假设、反证和未取证边界；缺输入/读失败保持明确状态；关键字和count不单独产生数值因果结论。
- 实际实验能由原冻结输入重放或由新增合法未来验证；研究创建与消费原键可对账，诊断不触发主链重算或改变REAL权限。

**保留**：保留成熟100号码/1200动作空间、工程SHADOW、现有窗口/状态/专家/排序/校准实现、真实旧训练与四证书域门槛；不将静态种子标志改true。

## 4. WINDOW — 窗口选择

**原状态**：UNAVAILABLE；原confidence=0.0，仅为诊断输出。原字段Hash=`fb3a48fa1bc2d5e28442c917c82f59400f6fb92e839b0127032d1fab2102cfed`。

**原Reason**：Window choice can overfit local repetition or react too slowly。

**原NextTest**：Compare 12/24/48/96/360 survival and PIT-stable window selection.

**不能据此推定**

- b05_window_summary为空不等于全部窗口计算未运行。
- 短期高分或事后挑最优窗口不能证明该窗口选择规则有效。
- 本报告confidence是静态诊断输出，不能作为算法根因的统计置信；读取失败、缺原件和实际失败必须分开。

**动态事实源**

- 冻结WindowFeatureEngine.B05WindowEvidenceBundle/校验hash与所选窗口、历史区间、gap、availability。
- 其输入状态、number模型和最终动作的exact引用；实际窗口支持长度须匹配协议。

**既有生产owner**

- `app/src/main/java/com/ethk3/novaonepro/WindowFeatureEngine.java:529` — `buildB05Evidence`。

**归因实验及所属批次**

1. 在B06/B08按连续合法开发区间对12/24/48/96/360等原合同窗口做分块生存与稳定性对照。
2. 窗口选择只使用当时可见数据；另设未参与选择的确认段，与固定窗口基线比较。
3. 一个窗口缺输入局部等待，不阻断无此依赖策略；draws热表仍不超过5000。

**接入主链**

B11诊断/报告只读消费既有V51RuntimeOrchestrator→V51Repository的已提交冻结事实与SettlementEngine的exact结果。同一类型化事实由B08已有ResearchFeedbackCP12/研究owner独立于UI生成合法动作并交原Director消费；B11只联查这些原动作回执，不新增或触发入队。新模型训练、冻结、资格和发布仍在所属owner，下一期由原selector/DecisionGate读取，绝不回写当期预测或授权。

**原问题关系**

- ROOT-02：修正静态flag/summary关键词诊断，建立当前冻结模型事实源与类型化证据。
- RES20：可验证失败或研究假设进入真实action/child并取得结果回流；证据不足允许NO_ACTION。
- RES09：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES18：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES24：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。

**验收**

- 报告的target/bundle/组件/模型/证书域与canonical一致；换版本后不能沿用旧PROVEN或旧根因。
- 每层给出事实、假设、反证和未取证边界；缺输入/读失败保持明确状态；关键字和count不单独产生数值因果结论。
- 实际实验能由原冻结输入重放或由新增合法未来验证；研究创建与消费原键可对账，诊断不触发主链重算或改变REAL权限。

**保留**：保留成熟100号码/1200动作空间、工程SHADOW、现有窗口/状态/专家/排序/校准实现、真实旧训练与四证书域门槛；不将静态种子标志改true。

## 5. REGIME — 状态/漂移

**原状态**：UNAVAILABLE；原confidence=0.0，仅为诊断输出。原字段Hash=`5c21f76c86346f97ba4c37d6cb76d5b36db86f656d68be3f9a833416efda04b0`。

**原Reason**：Regime mismatch can invalidate an otherwise strong motif/expert。

**原NextTest**：Blockwise regime transition replay; inspect CUSUM/BOCPD/drift flags.

**不能据此推定**

- summary含DRIFT/RISK不是数值根因证据；空summary也不能证明状态引擎不存在。
- 不同位置不同target的drift记录不能合成同一期十位置状态。
- 本报告confidence是静态诊断输出，不能作为算法根因的统计置信；读取失败、缺原件和实际失败必须分开。

**动态事实源**

- 冻结state trajectory、B05 drift bundle、所属位置/目标/输入hash和当时使用的drift policy hash。
- FrozenDriftPolicy.observe输出、DomainRouter决策与provider健康原件；TUNE artifact缺失和运行健康暂停分别列示。

**既有生产owner**

- `app/src/main/java/com/ethk3/novaonepro/V51StateTrajectoryEngine.java:36` — `buildAudited`。
- `app/src/main/java/com/ethk3/novaonepro/DistributionDriftEngine.java:1120` — `buildB05Evidence`。
- `app/src/main/java/com/ethk3/novaonepro/FrozenDriftPolicyR82.java:33` — `observe`。

**归因实验及所属批次**

1. 按预声明状态转换块回放识别延迟、误报和稳定性，TUNE数据与正式未来分离。
2. 比较有/无drift策略的同目标父子，保存政策阈值、冷却和支持域；不将经验超限预算称理论误报保证。
3. 漂移只触发下一模型研究/回放，沿既有DomainRouter局部fallback/PAUSE；不得修改已冻结观察。

**接入主链**

B11诊断/报告只读消费既有V51RuntimeOrchestrator→V51Repository的已提交冻结事实与SettlementEngine的exact结果。同一类型化事实由B08已有ResearchFeedbackCP12/研究owner独立于UI生成合法动作并交原Director消费；B11只联查这些原动作回执，不新增或触发入队。新模型训练、冻结、资格和发布仍在所属owner，下一期由原selector/DecisionGate读取，绝不回写当期预测或授权。

**原问题关系**

- ROOT-02：修正静态flag/summary关键词诊断，建立当前冻结模型事实源与类型化证据。
- RES20：可验证失败或研究假设进入真实action/child并取得结果回流；证据不足允许NO_ACTION。
- RES18：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES24：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES25：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。

**验收**

- 报告的target/bundle/组件/模型/证书域与canonical一致；换版本后不能沿用旧PROVEN或旧根因。
- 每层给出事实、假设、反证和未取证边界；缺输入/读失败保持明确状态；关键字和count不单独产生数值因果结论。
- 实际实验能由原冻结输入重放或由新增合法未来验证；研究创建与消费原键可对账，诊断不触发主链重算或改变REAL权限。

**保留**：保留成熟100号码/1200动作空间、工程SHADOW、现有窗口/状态/专家/排序/校准实现、真实旧训练与四证书域门槛；不将静态种子标志改true。

## 6. MOTIF — 局部模式

**原状态**：UNAVAILABLE；原confidence=0.0，仅为诊断输出。原字段Hash=`32e23403c5b53c02ba73ab0509c51dd7bef16b695d1f80a76ba87818f1267b0d`。

**原Reason**：Motif may be sparse, stale or duplicated across templates。

**原NextTest**：Require support, recent validity and negative evidence before promotion.

**不能据此推定**

- b06_motif_summary空不证明当前Motif expert无计算；模式条数不能代表可执行创新。
- 少量重复出现或模板名不同不能证明稳定规律或新机制。
- 本报告confidence是静态诊断输出，不能作为算法根因的统计置信；读取失败、缺原件和实际失败必须分开。

**动态事实源**

- 冻结Motif expert输入历史、window、位置/号码、模式身份、scoreSurface、支持/负例和语义指纹。
- 该expert的gate/consensus贡献、完整模型身份与最终动作/结算。

**既有生产owner**

- `app/src/main/java/com/ethk3/novaonepro/V51MotifNumberExpert.java:9` — `evaluate`。
- `app/src/main/java/com/ethk3/novaonepro/V51MotifMatrixProfileEngine.java:15` — `scoreSurface`。

**归因实验及所属批次**

1. 按类型和行为指纹去重模式；对稀疏/失效模式保留support与反例而不是强行晋级。
2. 跨块比较原模式、去模式、简单基线；新结构走真实AST/graph、input protocol和训练。
3. 新生成规则在B07按TypedGenerationRule规格由既有生成器/owner归纳与消费，B08继续反馈与代际验证；B11只读取产物，不新增NumberQ或规则训练owner。

**接入主链**

B11诊断/报告只读消费既有V51RuntimeOrchestrator→V51Repository的已提交冻结事实与SettlementEngine的exact结果。同一类型化事实由B08已有ResearchFeedbackCP12/研究owner独立于UI生成合法动作并交原Director消费；B11只联查这些原动作回执，不新增或触发入队。新模型训练、冻结、资格和发布仍在所属owner，下一期由原selector/DecisionGate读取，绝不回写当期预测或授权。

**原问题关系**

- ROOT-02：修正静态flag/summary关键词诊断，建立当前冻结模型事实源与类型化证据。
- RES20：可验证失败或研究假设进入真实action/child并取得结果回流；证据不足允许NO_ACTION。
- RES05：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES13：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES18：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES24：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。

**验收**

- 报告的target/bundle/组件/模型/证书域与canonical一致；换版本后不能沿用旧PROVEN或旧根因。
- 每层给出事实、假设、反证和未取证边界；缺输入/读失败保持明确状态；关键字和count不单独产生数值因果结论。
- 实际实验能由原冻结输入重放或由新增合法未来验证；研究创建与消费原键可对账，诊断不触发主链重算或改变REAL权限。

**保留**：保留成熟100号码/1200动作空间、工程SHADOW、现有窗口/状态/专家/排序/校准实现、真实旧训练与四证书域门槛；不将静态种子标志改true。

## 7. RELATION — 跨位关系

**原状态**：UNAVAILABLE；原confidence=0.0，仅为诊断输出。原字段Hash=`ed64e2c478c02f484a80404a6928a808cd31024f665f2a266dc1e88e6c34e1ac`。

**原Reason**：Cross-position relation can be spurious or leak contemporaneous information。

**原NextTest**：Verify relation uses only pre-draw history and survives block ablation.

**不能据此推定**

- 旧关系summary无值不等于关系模型没有实现。
- 同期开出的其他位相关、全数据相关系数或关键词提示均不能证明可开奖前使用的因果关系。
- 本报告confidence是静态诊断输出，不能作为算法根因的统计置信；读取失败、缺原件和实际失败必须分开。

**动态事实源**

- 冻结关系expert/tensor来源位置、目标位置、lag、cutoff、输入hash、可用时刻与支持域。
- 该关系经过哪个原模型组件/graph节点进入最终概率和动作；精确消费回执及结算。

**既有生产owner**

- `app/src/main/java/com/ethk3/novaonepro/V51RelationGraphNumberExpert.java:9` — `evaluate`。
- `app/src/main/java/com/ethk3/novaonepro/V51RelationGraphVerifier.java:19` — `stable`。

**归因实验及所属批次**

1. 先验证所有关系输入在target前可用，禁止同期开奖后他位信息泄漏。
2. 按预声明时段/状态分块消融关系并评估稳定性，复用V51RelationGraphVerifier原校验；全搜索登记避免挑最高相关。
3. 只有可执行、可消费的新子代才能计为研究产物，证据不足记录待证而非因果结论。

**接入主链**

B11诊断/报告只读消费既有V51RuntimeOrchestrator→V51Repository的已提交冻结事实与SettlementEngine的exact结果。同一类型化事实由B08已有ResearchFeedbackCP12/研究owner独立于UI生成合法动作并交原Director消费；B11只联查这些原动作回执，不新增或触发入队。新模型训练、冻结、资格和发布仍在所属owner，下一期由原selector/DecisionGate读取，绝不回写当期预测或授权。

**原问题关系**

- ROOT-02：修正静态flag/summary关键词诊断，建立当前冻结模型事实源与类型化证据。
- RES20：可验证失败或研究假设进入真实action/child并取得结果回流；证据不足允许NO_ACTION。
- RES13：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES15：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES18：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES24：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。

**验收**

- 报告的target/bundle/组件/模型/证书域与canonical一致；换版本后不能沿用旧PROVEN或旧根因。
- 每层给出事实、假设、反证和未取证边界；缺输入/读失败保持明确状态；关键字和count不单独产生数值因果结论。
- 实际实验能由原冻结输入重放或由新增合法未来验证；研究创建与消费原键可对账，诊断不触发主链重算或改变REAL权限。

**保留**：保留成熟100号码/1200动作空间、工程SHADOW、现有窗口/状态/专家/排序/校准实现、真实旧训练与四证书域门槛；不将静态种子标志改true。

## 8. CONSENSUS — 多专家共识

**原状态**：UNAVAILABLE；原confidence=0.0，仅为诊断输出。原字段Hash=`ee3932ea4105789fae103d999748483cf24d5b906f8af0c3e863266986fdc346`。

**原Reason**：Consensus can become correlated-vote double counting。

**原NextTest**：Measure expert diversity, regret and leave-one-expert-out delta.

**不能据此推定**

- summary为空不证明投票或expert gate没有运行。
- 专家数量、共识一致或换名后的重复票不能当独立证据与质量提升。
- 本报告confidence是静态诊断输出，不能作为算法根因的统计置信；读取失败、缺原件和实际失败必须分开。

**动态事实源**

- 冻结experts、各自版本/输出、gate weights、Consensus.evidence/checksum及共同target。
- 候选/父代来源关系、共同失败区域、实际排序和最终动作的exact消费。

**既有生产owner**

- `app/src/main/java/com/ethk3/novaonepro/V51EffectiveConsensusEngine.java:52` — `evaluate`。

**归因实验及所属批次**

1. 测量同目标专家行为相关性、重复来源和共同失败；leave-one-expert-out区分各方真实贡献。
2. 合法双父融合另建child；比较父A、父B、简单组合与融合子代，不能仅复制相同预测再加权。
3. 未达B09原资格时保留成熟父模型和合法SHADOW，B11只显示可信共识证据。

**接入主链**

B11诊断/报告只读消费既有V51RuntimeOrchestrator→V51Repository的已提交冻结事实与SettlementEngine的exact结果。同一类型化事实由B08已有ResearchFeedbackCP12/研究owner独立于UI生成合法动作并交原Director消费；B11只联查这些原动作回执，不新增或触发入队。新模型训练、冻结、资格和发布仍在所属owner，下一期由原selector/DecisionGate读取，绝不回写当期预测或授权。

**原问题关系**

- ROOT-02：修正静态flag/summary关键词诊断，建立当前冻结模型事实源与类型化证据。
- RES20：可验证失败或研究假设进入真实action/child并取得结果回流；证据不足允许NO_ACTION。
- RES22：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES24：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES26：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。

**验收**

- 报告的target/bundle/组件/模型/证书域与canonical一致；换版本后不能沿用旧PROVEN或旧根因。
- 每层给出事实、假设、反证和未取证边界；缺输入/读失败保持明确状态；关键字和count不单独产生数值因果结论。
- 实际实验能由原冻结输入重放或由新增合法未来验证；研究创建与消费原键可对账，诊断不触发主链重算或改变REAL权限。

**保留**：保留成熟100号码/1200动作空间、工程SHADOW、现有窗口/状态/专家/排序/校准实现、真实旧训练与四证书域门槛；不将静态种子标志改true。

## 9. CALIBRATION — 选择后概率校准

**原状态**：BLOCKED；原confidence=1.0，仅为诊断输出。原字段Hash=`b56a0c0e94baee871985a8f23a4d725e5905ac9a1d3733cb744df123eea93f51`。

**原Reason**：Strict OOS artifact proof is missing。

**原NextTest**：Use selection-conditioned pre-draw OOS ECE/Brier/Wilson/subgroup certificates.

**不能据此推定**

- 静态SelectedCalibrationArtifact.PROVEN=false不能替代当前bundle.calibration或该证书域校准的实际状态。
- 输出0到1概率、提高选中概率或单次命中不等于经过校准，也不能据此提高REAL权限。
- 本报告confidence是静态诊断输出，不能作为算法根因的统计置信；读取失败、缺原件和实际失败必须分开。

**动态事实源**

- canonical选中条件、实际calibration artifact版本/输入及模型/选择器身份；原始概率与校准后概率。
- 所属证书域原拟合/隔离/验证范围与角色、事前捕获原件、支持域、Brier/ECE/分组及覆盖；不得把FrozenDriftPolicy的earlier-TUNE误当全部概率校准协议。

**既有生产owner**

- `app/src/main/java/com/ethk3/novaonepro/V51SelectedActionCalibrationEngine.java:72` — `calibrateDetailed`。

**归因实验及所属批次**

1. B06/B08形成实际数值模型；B09沿各域原协议拟合和验证。CandidateCalibrationR932须eligibleEvaluation及事前预约采集的FIT120/EMBARGO1/VALIDATION120，版本化模型绑定R969_STATIC_DISJOINT_CALIBRATION；FrozenDriftPolicyR82才用其earlier-TUNE。不能用历史TUNE代替正式校准观察；B11不现场拟合。
2. 同目标比较校准前后Brier/ECE、覆盖与区间，固定选择条件；有多个校准器需登记全选择过程。
3. 通过时绑定精确artifact和支持域再消费；失败/缺件保留原概率与合法fallback，不改静态PROVEN常量。

**接入主链**

B11诊断/报告只读消费既有V51RuntimeOrchestrator→V51Repository的已提交冻结事实与SettlementEngine的exact结果。同一类型化事实由B08已有ResearchFeedbackCP12/研究owner独立于UI生成合法动作并交原Director消费；B11只联查这些原动作回执，不新增或触发入队。新模型训练、冻结、资格和发布仍在所属owner，下一期由原selector/DecisionGate读取，绝不回写当期预测或授权。

**原问题关系**

- ROOT-02：修正静态flag/summary关键词诊断，建立当前冻结模型事实源与类型化证据。
- RES20：可验证失败或研究假设进入真实action/child并取得结果回流；证据不足允许NO_ACTION。
- RES23：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES24：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES25：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。
- RES26：前置输入、数值生产或配对/资格消费依赖，按该问题原合同实施。

**验收**

- 报告的target/bundle/组件/模型/证书域与canonical一致；换版本后不能沿用旧PROVEN或旧根因。
- 每层给出事实、假设、反证和未取证边界；缺输入/读失败保持明确状态；关键字和count不单独产生数值因果结论。
- 实际实验能由原冻结输入重放或由新增合法未来验证；研究创建与消费原键可对账，诊断不触发主链重算或改变REAL权限。

**保留**：保留成熟100号码/1200动作空间、工程SHADOW、现有窗口/状态/专家/排序/校准实现、真实旧训练与四证书域门槛；不将静态种子标志改true。



<!-- CURRENT_MAPPING_BEGIN -->
## 本轮施工编号对应

原问题ID用于保留审计来源；本表给出最终施工编号。保留项/参考项链接只说明责任，不表示它被判为新故障。

|原项|当前施工问题|批次|
|---|---|---|
|NUMBER|[R342-022](../issues/R342-022.md), [R342-031](../issues/R342-031.md), [R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-039](../issues/R342-039.md), [R342-052](../issues/R342-052.md)|B06, B08, B11|
|RANK|[R342-022](../issues/R342-022.md), [R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-039](../issues/R342-039.md), [R342-052](../issues/R342-052.md)|B06, B08, B11|
|THIRD|[R342-034](../issues/R342-034.md), [R342-039](../issues/R342-039.md), [R342-040](../issues/R342-040.md), [R342-052](../issues/R342-052.md)|B08, B11|
|WINDOW|[R342-021](../issues/R342-021.md), [R342-034](../issues/R342-034.md), [R342-039](../issues/R342-039.md), [R342-050](../issues/R342-050.md), [R342-052](../issues/R342-052.md)|B06, B08, B11|
|REGIME|[R342-034](../issues/R342-034.md), [R342-039](../issues/R342-039.md), [R342-043](../issues/R342-043.md), [R342-050](../issues/R342-050.md), [R342-052](../issues/R342-052.md)|B08, B09, B11|
|MOTIF|[R342-025](../issues/R342-025.md), [R342-028](../issues/R342-028.md), [R342-034](../issues/R342-034.md), [R342-039](../issues/R342-039.md), [R342-050](../issues/R342-050.md), [R342-052](../issues/R342-052.md)|B07, B08, B11|
|RELATION|[R342-028](../issues/R342-028.md), [R342-034](../issues/R342-034.md), [R342-035](../issues/R342-035.md), [R342-039](../issues/R342-039.md), [R342-050](../issues/R342-050.md), [R342-052](../issues/R342-052.md)|B07, B08, B11|
|CONSENSUS|[R342-034](../issues/R342-034.md), [R342-039](../issues/R342-039.md), [R342-041](../issues/R342-041.md), [R342-044](../issues/R342-044.md), [R342-052](../issues/R342-052.md)|B08, B09, B11|
|CALIBRATION|[R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-039](../issues/R342-039.md), [R342-043](../issues/R342-043.md), [R342-044](../issues/R342-044.md), [R342-052](../issues/R342-052.md)|B08, B09, B11|

<!-- CURRENT_MAPPING_END -->
