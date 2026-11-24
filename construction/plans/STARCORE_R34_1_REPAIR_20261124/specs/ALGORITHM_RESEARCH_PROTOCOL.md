# R34.1 算法创造、变异融合与自然质量研究协议

本协议规定可施工的算法实验和验收原件。它不保证彩票数据存在稳定、可交易的预测优势。源码中的家族、专家、思路和矩阵大多已声明；当前应先修复运行与消费链，再以严格对照判断哪些机制值得保留。保留负结果、无合格资产和样本不足均是合法结果。

## 1. 从源码AST到可执行公式

基线 `PolicyAstR27` 已有 `freq/fdecay/cross/crosscond/family/model_surface/set_intersection` 等叶、`add/sub/mul/div/max/min/mix/ema/softmax` 等节点；不能把“新增一个函数名字”冒充新算法。`parse` 已有导入预算，正式出生仍由 `PolicyContractR30` 约束≤64节点、≤8深度；导入允许的较大解析预算不能扩大出生资源上限。

直接可表示且可调用原求值器的公式例子：

```text
(mix 0.7 (crosscond 1) (model_surface 1))
(softmax 1.0 (add (log1p (crosscond 1)) (mul (c 0.2) (cross 2 1))))
(mix 0.5 (family 11) (family 20))
```

这些是研发候选，仍须类型/因果/数值/身份/单位置K3检查及训练。第二式的cross是位置2的一阶滞后，不能改成同一期已开奖值。`family` AST叶当前只允许源码集合 `[8,11,12,20,21]`；F01/F02/F09/F17的工厂对象不能直接偷塞成未允许的family叶。窗口参数从当前 `FeatureCacheR27.WINDOWS`、衰减从 `HALF_LIVES` 读取并冻结版本，不臆造合法整数。

每次创造至少保存：父公式/父状态、具体节点路径、算子、输入pin、seed、原子条件、学习参数、完整数值状态、canonical AST、行为探针输出、成本和出生/拒绝原因。解释文本由这些原件生成，不能先写解释再假定算法存在。

### 身份、去重和行为新颖

1. 语法身份：无损浮点序列化、节点参数顺序固定；不因空白、名称、日志顺序形成新策略。
2. 安全规范化：只用已验证的恒等变换；可对确实可交换且保持位级语义的二元节点按子hash排序。不得对浮点加法任意结合重排、忽略NaN/缺失传播或把有状态节点挪位。原AST数值输出对照必须通过后才承认为等价。
3. 行为指纹：在事前冻结的TRAIN探针上保存完整概率向量hash、K3/HOLD序列hash、支持掩码与成本；缺模型输入不能当均匀输出。以同目标行为距离和误差相关作为近重复提示，不能仅用新hash认定新颖。
4. 双层淘汰：精确重复拒绝；近重复只在相同目标/输入/语义范围内比较。不能因为历史少量负结果永久禁止所有新机制。保留结构差异和资源约束下的探索份额。

## 2. 真实学习目标与checkpoint

F13/F14的 `SymbolicFamilyR26.update` 已有数值学习协议；AtomNet按 `AtomNetTrainerR27` 的真实Adam/回放路径训练，不另写一份只用于报告的“学习器”。训练receipt需实际 `gradientNorm、parameterChange、loss、optimizerStep、before/afterHash`。非零梯度但参数不变、优化器步不前进或stateHash未提交要区分精度饱和、配置与持久化错误，不能自动记有效学习。

基线AtomNet配置包含batch64、预训练最多20epoch、patience3、学习率1e-3至1e-5、在线新样本+最多63回放、回放缓存2000、每720期开发检查。它们是**同提交现状**，不是本方案承诺最优超参数。调参只能在冻结TRAIN/SELECT协议里比较，选出后新contractHash；不能用正式未来命中再选学习率。

完整checkpoint包括权重、Adam moments/step、学习率阶段、RNG、回放样本键与顺序、十位置cursor、partialIssue、split/ABI/pin，不能只保存网络参数。连续运行和杀进程恢复的下一步梯度/状态/输出应在预先声明数值容差内一致。对一个固定可学习样例，梯度和参数变化应与参照计算方向一致；无信号数据可能不改善，不允许为验收强造下降损失。

十位置模型不代表每期下注十个位置。模型提供十位置概率面，策略合同仍选定一个位置K3；冻结后实际调用必须使用同 `publicationId/modelHash/featureABI/historyHash`。19行旧publication与存在模型文件均不证明此链已闭合。

## 3. 七组可以实际施工的研究路线

| 路线与现有入口 | 具体子代操作 | 必须保留的同机会对照 | 失败结论与消费者 |
|---|---|---|---|
| F13/F14、E08、I09 符号/数值公式 | 替换合法子树、剪掉冗余原子、改变允许滞后/窗口；TRAIN拟合真实数值参数 | 父公式、移除该子树、仅参数变体、等预算结构变体 | 无行为差或开发增益不足则淘汰；`StructuralProposalR26`→`BirthGateR27` |
| F01/F17、E05/E06、I04/I14/I20 跨位与交集 | 选择事前跨位边、mask、合法集合交集；固定目标位置；不足3个合法候选HOLD | 去跨位、本位置、单集合；按全部机会比较收益和覆盖 | 不得用更少ACT制造虚高命中；实际交给跨位/交集策略 |
| F04–F12、F20/F21 时间/状态 | 替换合法窗口、状态记忆、注意力、相似检索；保留断期segment | 去长窗、均匀注意力、无记忆、无检索与等预算简单模型 | 复杂度没贡献则降阶；延迟/成本超预算正常拒绝 |
| F22/E09/I10、R02/R05/R17 失败驱动 | 同目标误差聚类→父节点定位→可反证假设→实际AST变异 | 原父代、无失败提示、随机合法节点变异 | 已见失败修复仅开发结论；新子代需独立未来，失败记忆回下一轮 |
| E10/E11、R06/R07/R12 变异与融合 | 同目标双亲交叉、固定/学习融合、组件A/B/A+B；权重TRAIN拟合 | 最佳单亲、固定等权、去一个组件、同资源独立搜索 | 缺双亲/组件明确等待；无协同不计有效改进 |
| F23、教师/学生、20矩阵 | 使用原teacher/state/ABI和同目标冻结探针；残差或互补矩阵产生typed action | 无教师、打乱教师但保持时点、无矩阵消费者、单教师基线 | 报告进程静态绑定≠owner绑定；相关性不能冒称因果 |
| 校准/回放/预算 | 只在开发角色拟合校准；固定/可学习回放；effect去重后调整下轮真实预算 | 固定校准、无回放、固定回放、固定预算、同CPU预算 | 正式未来只读；输出真实allocation/dispatch和负结果 |

这七组覆盖报告明确方向，不能宣称穷尽所有未来算法。全部24家族、16专家、21思路及矩阵的对象级输入/输出/消费者/失败合同见 `RESEARCH_OBJECT_CONTRACTS.json`；同一矩阵在两个目录出现仍是同一底层对象。

## 4. 配对评估、消融与代际信用

对每个候选、父代与控制，冻结相同目标期日历、同一位置语义、同一输入可见边界，保存每期概率、ACT/HOLD、K3、损失、结果、成本。不能只抽“双方都命中”的子集。ACT-only命中率与覆盖、每机会收益分别报告：

\[
\widehat{HR}=H/A\quad(A>0),\qquad Coverage=A/N,\qquad Payoff/N=[H W-(A-H)L]/N.
\]

其中N为预注册全机会，A为合法且已结算ACT，H为命中；缺冻结/缺结算/冲突单列并阻断相关资格完整性，不能偷偷排除降低分母。W/L读取实际合同；未观测费用明确列为未计，不能把记录收益当保证净利润。单次模型预测可用十类Brier/对数损失评估，决策质量还需覆盖、K3效果和成本。

开发消融使用相同时间块和计算预算，保存子代减父代/控制的配对差、样本支持和区间。时间依赖的数据不能不加说明套独立样本标准误；使用预先固定时间块或原批准统计方法。正式资格的配对界沿现有统计合同执行，不能选一个看起来显著的方法替代它。

一代 `ENGINEERING_GENERATION_COMPLETE` 需要所有预算内proposal都有终态和原件，包括拒绝/重复/失败。`VERIFIED_IMPROVEMENT` 必须有独立证据，不能由一代完成、产出多少公式或训练损失下降推出。研发信用按真实增量/成本与不确定性更新，来源回执去重，保留探索下限；下一轮实际dispatch必须与allocation对账。

## 5. 18研究图、20矩阵与165索引对象

18图逐项有输入、输出、数值support、consumer和失败状态；`R11`父子确认、`R14`模型消费链、`R18`主链全链是只读核验，不能为了“都消费”让它们授证或训练。R08行为探针为TRAIN固定输入，其余研究测量按SELECT/合法ONLINE_RESEARCH边界；R11正式结果不回流同实验。

20矩阵含19个R930对象及SU2教师checkpoint。下面是同提交真实producer的shape，均须同时校验轴标签、support mask、role、target、availableAt和artifactHash；legacy FrozenMatrix的统一位置×号码ABI与typed矩阵分开，不混用。

| typed kind | 实际shape/轴 | 实际生产与消费 |
|---|---|---|
| EVIDENCE_TEACHER | P×10，1≤P≤4，同位置归档策略×number1..10 | MatrixEvidence.compute → ResearchContext.premiseHypothesis；原归档策略为研究teacher，不能冒充F23原VM |
| STUDENT_ERROR | 1×3，policy×HIT/MISS/TEN_CLASS_BRIER | MatrixResearch.build → calibrationHypothesis |
| FORWARD_REASONING | P×3，策略×TYPE_PIT_VALID/AVAILABLE/NORMALIZED | MatrixEvidence.compute → premiseHypothesis |
| REVERSE_REASONING | 10×10，targetNumber×previousSourceNumber，lag=1 | MatrixEvidence.compute → premiseHypothesis |
| COUNTERFACTUAL | 1×B，componentPath×block:fromOrdinal，1≤B≤64 | ResearchStore.paired → ready/consume；专属组件移除原件 |
| FEEDBACK | 3×2，预报置信档×HIT/MISS | MatrixResearch.build → calibrationHypothesis |
| CONTRADICTION | P×3，策略×CONTRACT_CONTRADICTION/MISSING_PREMISE/NONNORMALIZED | MatrixEvidence.compute → premiseHypothesis输入拒绝 |
| TEACHER_DIVERSITY | P×P，2≤P≤4，同位置策略两两预测JS距离 | MatrixEvidence.compute → fusionParents |
| TEMPORAL_PRECEDENCE | 2×2，CURRENT/PREVIOUS_MODEL×AVAILABLE_BEFORE_FREEZE/FREEZE_BEFORE_DEADLINE毫秒 | MatrixEvidence.stored → ModelReplayStudy.settle/summary，ONLINE_RESEARCH |
| INVARIANCE | E×3，合法add/mul二元交换策略×概率匹配/决策匹配/最大误差 | MatrixEvidence.compute → premiseHypothesis |
| CAUSAL_SUPPORT | R×3，最多8个组件原件×ROLE_PROVED/EXACT_CHILD/COMPLETE_BLOCKS | MatrixEvidence.stored → measuredPrune |
| COUNTER_EVIDENCE | R×B，R≤8、B≤16，负组件×REGISTERED_BLOCK_SLOT | MatrixEvidence.stored → measuredPrune |
| RESIDUAL_MECHANISM | 3×10，预报置信档×actualNumber | MatrixResearch.build → residualHypothesis |
| NUMBER_POSITION_COMPATIBILITY | 10×10，number×position | MatrixResearch.build → position；不等学生 |
| CROSS_POSITION_FLOW | 10×40，targetPosition×sourcePosition×lag[1,2,5,10] | MatrixResearch.build → cross；AST cross叶仅lag1..3，不能直接灌lag5/10 |
| ACTION_UTILITY | 2×3，DECLARED_SKIP/ACT×反事实HIT/MISS/研究单位效用 | MatrixEvidence.compute → actionHypothesis；单位效用不是REAL利润 |
| ALGORITHM_COMPLEMENTARITY | P×P，2≤P≤4，1−共同失败/任一失败 | MatrixEvidence.compute → fusionParents |
| EVOLUTION_CREDIT | O×C，原OPERATOR_GAIN_COST中的算子×child | MatrixEvidence.stored → weights/familyCredit/appendBudgetReceipt；没有对象CPU则保持未知 |
| RESOURCE_EFFICIENCY | T×4，T≤64，task×CPU_MS/QUANTA/MEMORY_UNKNOWN/QUEUE_UNKNOWN | MatrixEvidence.stored → ResearchPool.refreshResourceBudget/recordResourceUse；后两列未测不能补零 |
| SU2 checkpoint | factorCount×10位置权重+原stream；codec限定factorCount≤1200、各blob≤262144字节 | MatrixSnapshotSU2.capture → support / MatrixStoreSU2.maintain |

`MatrixRegistryR30.typedConsumers` 已声明实际研究消费者。报告旧目录的 `algorithmConsumer=PERSISTENCE_AND_DIAGNOSTICS` 只描述legacy路径，不能推出typed消费者不存在。本轮修复生产/输入绑定/实际use→annotate→stage→commit，沿既有消费入口添加校验与回执，取消重复router。原假设筛选达到支持/信号要求才会使用矩阵，合法无信号时不应强迫产生子代或假消费。

报告165是82能力、18图、20矩阵、29旧图、16逻辑表的索引行数。29旧图目前输出未知，16逻辑表的分页不代表全部数据；对象appendix保留原观察状态、source reference、source hash、现有consumer和拟施工合同，不能把165当独立根因或已修复数。

## 6. Q01–Q08自然质量合同

| 项目 | 可接受判据 | 必须单列的限制 |
|---|---|---|
| Q01 命中 | 独立自然ACT命中率≥34%；研发优先目标≥37% | ≥37%不是当前V1新增硬门槛；区间和支持不足不得说已稳定 |
| Q02 频率 | 部署前预登记FUTURE_ACT_RATE≥15%，报告30%目标；部署后另计REAL_ACT_RATE | 数学裁决由 `evaluate` 产生，`LiveShadowsR27.certify` 核查未来 ACT 覆盖≥15%及净收益等条件后决定签发；资产质量另列，REAL部署后分母不能成为首次应用条件 |
| Q03 序贯 | 保留原alpha分配与永久index；e-value达到原阈值 | R27族内控制不扩展成整个软件所有家族的保证 |
| Q04 稳定 | 原合同固定720块最差≥26%、最长连错≤6；窗口事先冻结 | 不后选漂亮块；报告单门槛敏感性和联合功效，不能私改 |
| Q05 校准 | ECE≤0.04、等质量 ECE 按当前 CalibrationR30 算法复算；校准支持≥200且逐行先前拟合支持域覆盖率≥90%，并列等宽 ECE、BSS、区间与范围 | 校准支持200不是资产所需未来ACT250；样本/分箱缺失保持未知 |
| Q06 净增益 | 同目标父子与负控制的独立配对差，按原计划有效下界>0 | 不用开发消融替独立配对；父子ACT不同必须对齐全机会 |
| Q07 资产 | 合法身份、ABI、全部门槛、证书及实际主链消费 | 250ACT必要不充分；零资产可是真实研究结果 |
| Q08 误报/功效 | 原 Q08 按登记的 6 个臂各 20 个种子跑完整工厂，核对 120 对键、原注册零信号合并误报上界与 SIGNAL 功效下界；另报 NullPipelineSimulationR30 的 34%、37%、40% 强度扫描结果 | 旧源120计划0完成无效；人工positive不能替自然优势 |

V1源代码还要求与I00和shuffled control的配对LCB>0、语义ABI一致、序贯合格，证书有效期24小时。只可按原合同授V1证书；新增资产质量投影不改变历史V1 contractHash，更不能自行授REAL。证书到期或证据失效，下一决策应拒绝；已经合法冻结的真实结果必须仍可追溯结算。

最长连错≤6是较强稳定性约束，可能显著降低给定命中率下通过联合门槛的机会。用 `CertificateGateR27.streakGatePassProbabilityR31` 展示iid假设下的单门槛敏感性，不将它解释成设备真实概率。若目标不一致，应提出新的前瞻统计合同和功效研究；不能在运行中放宽现合同让结果“达标”。

## 7. 完整工厂模拟及停止规则

模拟必须包含候选创造、筛选、调参、失败记忆、预算更新、确认、未来序贯与模拟证书，才能检测搜索带来的误报；只跑固定冠军不覆盖选择过程。先登记null与植入机制、seed集、样本长度、trial数、全工厂预算、误报上界精度、功效目标和终止规则。

零次误报也不等于误报率为0。N次独立trial零误报时，单侧95%上界可按 `1-0.05^(1/N)` 报告；N=120约为2.47%，不能因此宣称验证任意更小alpha。存在依赖trial时不能套该独立式，应按预注册仿真设计处理。原 Q08 的 120 条为六臂合计，每臂 20 个种子。区间分母遵循注册的臂和组合，使用原协议的三个零假设臂及同一批完成的 trial；不得把其他臂或额外强度扫描并进误报分母。扩展实验另行预注册，具体trial数由所需上界精度/功效决定，并受离线计算预算约束；未完成清楚标INCOMPLETE。

每次运行固定当前source/config hash，所有失败、超时、被门槛拒绝都保留，不删种子重跑至显著。结果不达标则保持算法研究未达质量目标，工程链仍可完成。后续新版本另开实验，不改旧实验的“计划”和分母。

**选择不成环：** 原V1证书有效、PIT/模型/校准依赖满足时自动进入原选择规则。Q02的部署后REAL频率及其他综合研究目标尚未观察，不得成为额外首用门槛。改变V1资格逻辑必须新协议版本+新实验，不重写旧证书。
