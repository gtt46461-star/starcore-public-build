# 创造、训练、反馈与资格闭环施工附约

## 1. 执行效力、应用底座与完整范围

本附约是本轮返修的**必须执行合同**，与 [完整执行指令](CODEX_EXECUTE.md)、[46 项问题卡](ISSUES.json)、[B00–B11 批次](BATCHES.json)、[逐项验收矩阵](ACCEPTANCE_MATRIX.json)、[创造与质量合同](contracts/CREATION_TRAINING_AND_QUALITY.md)及[运行链路与保全合同](contracts/PIPELINE_AND_PRESERVATION.md)一并执行。它细化现有方案的施工与验收，不另建第二套产品调度、模型队列或真实决策所有者。

应用底座固定为 **versionCode `577311211` / source commit `5c7f1b36eaee6036df280e85835bb0fb4ce0b40d`**，或经逐文件三方核验的合法后代。计划所在提交、分支或目录不自动成为应用底座。施工必须保留 211 的有效文件与逻辑；禁止将 main 旧应用目录、173 整文件后态或旧压缩包覆盖到 211。

必须依据全部 **46 张卡、12 个批次**实际施工。完整包的 **5 个候选补丁只覆盖 5 个局部问题**；补丁累计可应用、主机脚本通过或 APK 编译通过均不代表 46 项已完成。未提供候选补丁的问题仍须按卡完成必要实现、增量迁移、调用链接通、正反回归与实际交付。

每张卡必须登记以下有证据的处置之一：

1. 已实施修改，并逐层列明验证结果与原件。
2. 当前合法后代已经实现，提供精确源码、差异和回归证据后保留，避免重复重建。
3. 受到具体原件、设备或其他外部条件阻断，记录精确缺件、受阻路径、下一动作与仍可继续的独立工作；该状态不等于完成。

不得以“存在相应类”“表已创建”“有 heartbeat”“任务数增长”“没有现成补丁”代替处置。单纯计数为零是需要解释的现象，不能未经调用链核验就被写成根因。

如修复必须涉及卡外文件，应先说明必要调用链与关联问题，补齐文件/方法锚、增量差异及保留回归，再实施必要修改。不得借此扩大成无边界重写。继承的 1832 条需求和 299 张历史卡继续保留原状态和映射，不因本轮 46 卡施工而批量核销未验收要求。

## 2. 逐卡台账与 B00–B11 完整施工

### 2.1 台账必须能证明实际完成了什么

在计划交付记录中维护覆盖 46 卡、12 批次的执行台账。这是仓库文档，不要求给应用另加数据库状态机。每卡至少记录：

- 问题 ID、批次、原始证据、实际应用 source commit、当前合法输入条件。
- 涉及文件的 before/after SHA、原 owner 和方法、实际修改或保留理由、实现 commit。
- 正例、反例、真实输入输出、原件/回执引用和验证执行日志。
- 实现、主机、Android/迁移、现场持续运行、自然质量各层状态。
- 未完成的精确原因、仅受影响的路径、下一动作和仍可推进的工作。

每批记录上一批交付身份、本批 source/APK/签名/版本/计划绑定、原功能回归、可下载交付与回退条件。每批结束须对照 `ISSUES.json` 和 `ACCEPTANCE_MATRIX.json` 检查遗漏。B10 只汇总前批已经产生的证据，不能用总结文档代替 B01–B09 的实际实现。

`PLAN_DEFINED`、补丁可应用、抽取方法测试、主机测试、Android、设备及自然未来必须分开。`READ_ERROR`、`UNKNOWN`、真实 0、EMPTY、WAITING、NOT_RUN、NOT_PROVEN 不相互替换。不得通过修改检测报告状态制造产品完成。

### 2.2 各批不可被候选补丁替代的主体工作

下表是执行完整性检查，具体文件、方法、输入和负例仍以问题卡为准。

|批次|候选覆盖的局部|必须完成的其余主体|保留边界|
|---|---|---|---|
|B00|无|核准 211/合法后代；保全旧 49 个相同修复和 15 个后续变化；建立卡/需求映射及现有构建签名、语义身份基线|禁止使用 main 旧应用整树，禁止改写历史封印|
|B01|C01 scope、C02 覆盖 SQL|独立预算、typed 依赖、同快照续跑、freeze 时钟来源、超时/取消/连续导出，保留 96 检查和 44 指标及原格式|检查失败仍可导出，不增加业务全 PASS 前置|
|B02|无|报告工作副本恢复与回收、总盘预算、共享故障热配额、可变生产者原件封存/排空、启动 journal|不清队列、不删唯一原件；磁盘不足明确记录受阻资源|
|B03|C03 精确重试分类|prepared 公平准入与同一 writer 服务、已提交 schema 单元恢复、claim→prepare→admission→commit 的真实证据|活动结算、freeze、截止、fence 和容量仍受保护|
|B04|无|抓取/推理/落库长尾、下一期赛前窗口、163 个旧 LIVE_RETRY 的逐项分类、延后证据 ACK、后台/内存/缓存正确性|无冻结不伪结算；工程回归与 24/72 小时现场验收分开|
|B05|无|逐 pin/root 恢复、typed load、精确投影快路、合法 ColdRebirth、长期摘要新 epoch|某旧缺件不锁独立 AST3 DEV，不复活旧失效 epoch|
|B06|C04 精确第五 RULE 资格|推进已有恢复继任、特征→数值→tape→receipt→artifact、各已启用内核恢复与首次合法冻结|旧恢复需原件；无 REAL/noPredraw 的独立 DEV 仍可推进|
|B07|无|既有 RollingLearner/FrozenPublisher、结构变异/交叉/融合、typed rule、F23 教师依赖与真实反馈消费|缺教师/供体只等待对应路径，不锁其他基础家族|
|B08|C05 R18 精确投影|18 新图、20 注册矩阵、29 旧图、16 逻辑表及真实消费；AtomNet 原模型、对象 CPU、R11 数值显示|工程 READY 与 PENDING_B09 分开，正式只读图不回流优化|
|B09|无|独立未来/校准/永久账本、工厂能力与自然质量分别验证、证书到唯一主链的实际消费|工程夹具不授予自然质量；自然消费需 B04 的真实主链条件|
|B10|无|逐项汇总 96/44/165/826 及 1832/299 继承状态，回到生产原件和真实分母|不删项、不把未知改零、不批量改 PASS|
|B11|无|真实源码、签名 APK、覆盖安装、数据保留、同 APK 重复导出、版本/源/计划闭合及可下载交付|不得用旧 APK 改名；外部设备缺失须明确未验|

按已满足工程前置的工作持续推进到 B11。某旧 root、教师、自然窗口或现场设备暂缺，只暂停其依赖任务和相应签收，不能停掉所有可独立完成的工程；也不能省略被阻断项后声称全部完成。

## 3. 三条研究路径、独立 DEV 与首次冻结

### 3.1 三条路径分别修复、分别计量

|路径|合法续跑需要什么|缺件时如何处理|不得混用的证据|
|---|---|---|---|
|R26 旧家族/检查点|对应原 state、模型/优化器、ABI、输入与身份；已推进 parent 的 ColdRebirth 仍须回读原 state 并验 hash|保留并隔离具体旧 parent/root；具原件者按原 ColdRebirth；无需 parent 的新基础实验另走现代工厂|旧权重、样本、分数、冻结或资格不得移给独立新实验|
|R27 固定回放/进化|同一 expectedPin、原输入分片、source/fence、cursor/RNG/曝光账本及一致 metadata|仅相应旧实验等待；原 pin 无法恢复时不得重捕活数据并冒用原 dataset hash|旧数据集记录数不能替代 policy/replay score/generation 的实际产物|
|AST3 / R952 现代开发/发布|自身 canonical 历史、catalog/特征、协议、ABI、合法截止和资源准入；恢复旧 TRAIN 另需精确五元组与原 `.scdev`|缺旧任务原件只阻其恢复；足量新合法输入可建立无关联的新基础实验|特征行、旧 AST1 在线产物和任务数不能替代现代开发模型/回执|

不能因为全部旧 checkpoint 尚未恢复，就禁止独立基础模型训练；不能删除 ColdRebirth 的旧 parent 证明来伪造谱系。新基础实验必须有独立 ID、新输入/协议、从零开始的计数，不继承旧质量或资格，不重置旧游标来伪造新创造。

### 3.2 无 REAL、无证书、无赛前预测时仍有开发入口

沿 211 已有调用链推进独立开发：

`RuntimeCoordinatorR26.runModernContinuity → ResearchLearningContinuityR912 → ResearchDirectorR952 IMPORT/TRAIN → ModernFamilyFactoryCP12 / DevelopmentProductionCP12`

在家族自身输入与资源合法时，该链不等待 REAL、证书、全体旧 root、完整 18 图或全部长期摘要。无教师的基础家族与需要教师/绑定输入的家族分别排队，不能由后者阻塞前者。

必须实际验证启动场景：`REAL=0`、`certificate=0`、旧期全部为 `NO_PREDRAW_CANONICAL_TERMINAL`、无关旧 checkpoint 全缺失，但提供足量合法 canonical 历史和已验证 catalog。独立 fixed graph 和至少一个无需教师的学习内核应能完成开发回执。无 `COMPLETED` 时 rolling_event 保持 0；不得制造结算、历史预测或虚假反馈事件。

### 3.3 首次 FREEZE_CANDIDATE 不等待 rolling

211 的 `ResearchTrainingHookRegistryR952` 已有五个持久轮转子槽：`LEGACY_OUTBOX`、`TRAIN_CANDIDATE`、`FREEZE_CANDIDATE`、既有 rolling TRAIN、`R343_ROLLING_FREEZE`。继续沿用这些 owner、预算与租约，不另建调度器。

首次 `FREEZE_CANDIDATE` 沿原 `FrozenCandidateAdapterR958.prepareDevelopmentFreezeCP8` / `commitDevelopmentFreezeCP8`，按照其自身开发产物、原件、目标期、cutoff 和冻结条件推进。不得额外要求先有 rolling event/version、REAL 或自然证书。

`R343_ROLLING_FREEZE` 依据实际滚动版本，是不同的现有路径。首次开发冻结与滚动冻结互不等待，但均保留不可变模型、合法时间边界及原资格门。**冻结完成本身不授予证书或 REAL。**

## 4. 真正完成历史开发训练

### 4.1 每段计算和提交必须可回读

先推进已有 `attempt0` 恢复继任，逐段定位预算、子槽选择、claim、输入解码、feature page、数值 checkpoint、commit、development receipt 和 FREEZE 入队。继续保持原六槽/五子槽、训练预留、prepared 身份、owner/fence、commitKey/payloadHash、cursor 与原件校验。

诊断快照已保留 1100 条特征，说明部分计算确实发生；这些特征不能替代完整训练成功。每份完成模型至少闭合下列身份：

`candidate/AST → kernel/feature ABI → input manifest/训练与验证边界 → feature prefix → normalizer/数值 checkpoint → 验证 tape → development receipt → model artifact → 后续冻结任务`

每个箭头均须由同源原件、真实提交与回读证明，不仅记录名称。等待时允许没有产物；禁止为了填满漏斗造 artifact、梯度、模型 hash、pair 数或回执。已知回滚可按精确合同重试；未知提交须先回查权威回执，不能重复数值学习或游标推进。

C03 只解决精确分类错配；它不代替 B02 故障原件排空或 B03 公平准入。只允许精确 backlog 原因、`ADMISSION`、`ROLLED_BACK` 及受审旧/新类别进入对应等待。其他资源错误、完整性错误、COMMITTED/UNKNOWN 不扩大重试。公平服务票据应绑定 task/owner/fence/input/payload/阶段，仍由同一个协调器检查活动结算、freeze、截止、容量、取消与存储条件。

### 4.2 六个实际 ModelKind 必须分别验算

211 的 15 个 family 标签映射到下列 6 个实际 ModelKind。不得用标签数量冒充数学机制数量。**固定图加一个学习内核是首次贯通里程碑；最终仍须逐个覆盖每个已启用内核的真实计算、数值检查和中断恢复。**不得关闭未通过的既有内核来缩小验收。

|ModelKind|应保留的真实计算|必须核验的状态/产物|
|---|---|---|
|FIXED_GRAPH|执行类型化 PIT 图，得到合法概率和验证结果；无需伪造数值优化|图/输入身份、逐样本前向与验证 tape、完成回执；优化步为 0 可以合法|
|SOFTMAX_LINEAR|`z=Wx+b`，`p(k)=exp(z_k)/Σexp(z_j)`，按真实标签优化交叉熵|训练集标准化、实际权重/偏置、有限值、维度、checkpoint/restore；LINEAR/HYBRID 的特征差别据实记录|
|OVR_LOGISTIC|逐类逻辑函数和原实现的概率输出合同|逐类权重更新、输出校验、数值状态及恢复一致性，不能换成固定图或改名替代|
|HISTOGRAM_TREE|训练样本分位候选、Gini/切分、叶子概率及原容量约束|TREE_QUANTILES/TREE_SCAN、真实切分结构、叶子状态、深度/容量与恢复；不虚构神经梯度|
|MLP_D32_16_10|现有隐藏层、激活、反向传播与 Adam 更新|权重/偏置、Adam 一阶/二阶矩、step、有限值与 checkpoint/restore；不把线性模型改名 NEURAL|
|EXPERT_HEDGE|真实冻结教师概率与原 Hedge 权重更新；211 使用对数权重增量 `0.05*log(max(1e-15,p_expert(y)))`|教师 freeze/model/manifest 身份、同目标对齐、专家权重与恢复；BANDIT/EXPERT/ENSEMBLE 共享内核的配置差异分别记录|

教师或绑定输入不足时，对应现场路径保持 WAITING；使用隔离合法工程夹具验证其实现，夹具产物不能写入用户自然质量账本。禁止占位均匀教师、假模型或伪造原件让缺依赖路径通过。

### 4.3 历史快训的效率与时间隔离同时保留

保留现有 CP9 训练区间、120 个验证样本、embargo、仅训练集拟合标准化、有限步预算及停止规则。当前 Dataset 使用 300–1200 个训练样本；验证 early-stop 属于开发使用，不能再被称为独立确认未来。

按家族实际 `requiredHistory`、合法连续来源与固定 cutoff 开工。热历史不超过 5000 行不等于 5000 连续期；不填零、不跳缺口冒充长窗，也不要求短窗家族先等满最长窗。长期摘要失效后沿现有新 epoch 逻辑恢复，保留旧 epoch 与已有 journal/snapshot，可追加真实迁移记录，不能回写过去可知时间。

解码、特征和数值计算继续放在原有 writer 事务外；提交保持有界小事务。重任务可跨多期切片，不强令每次开奖完成整个 epoch。参数收敛后 hash 可能不变；验收验证真实计算与提交，不能强迫制造参数差异。

## 5. 类型化创造、变异、交叉、融合与持续研究

### 5.1 继续使用现有可执行结构生产链

沿现有 `MutationGraphCP12`、`EvolutionControllerR952`、`ResearchDirectorR952`、`CreationChangeKindR343`、`TypedGenerationRuleR342` 与失败研究 action 链实现持续生产。持久化 exact proposal，绑定父代、输入 roster、冻结教师和 seed；恢复同一 proposal 不得换父代或重采一个更高分结果。

合法 child 必须经过类型化编译、固定原始输入与协议、TRAIN、开发筛选和独立冻结。结构生成限制在既有有界 typed graph 内，不执行任意生成 Java，不通过修改 ABI 标签假装兼容旧模型。

|产物分类|必须提供的真实区别|不得记为该类的情况|
|---|---|---|
|参数/输入迭代|同结构下的窗口、系数、学习率、种子、来源等具体变化|不能直接计为新机制|
|结构变异|合法算子/拓扑/依赖改变、改变位置、父子身份与可执行图|改名、改 generation、同构重排、复制父代|
|交叉|两个父代兼容子图被实际组合、类型检查与两侧来源|只复制一侧、只改参数标签|
|融合|原合同要求数量的真实父模型/子策略及其冻结输出被实际消费|重复同一预测包装成多个专家、旧证书背书新模型|
|机制扩展|输入变换、预测函数、更新规则、目标或结构家族的实质差别，完成实现与独立验证|仅 family 数或 AST hash 不同|
|NO_OP/拒绝|实际尝试、具体原因和受影响路径|未知尝试数填零、伪造 child 充成功计数|

至少有一个真实结构改变的 child 沿现有队列完成开发评分，并能回读原父引用与改变位置；对既有变异、交叉、融合分别执行合法工程正例和必要拒绝例。开发效果可以为负，允许失败与淘汰。不得为了“每代更强”修改评分、挑选未登记窗口或丢弃失败分母。

规范 AST hash 用于身份和字节去重，不能单独证明数学不等价。若使用行为探针或消融，输入与规则须在开发范围预登记，不偷看确认/未来结果选择子代。

### 5.2 typed rule 的实际尝试和迁移必须闭合

`AtomPublicationStoreR31.prepareRuleR342` 最多读取 8 个供体，至多尝试 28 个 pair。供体 proof、实际 pair 尝试、拒绝原因、规则和迁移得分由现有生产/消费 owner 持久化；纯 `TypedGenerationRuleR342` 值对象不新增数据库写者。

`ResearchContextR30R1` 的迁移绑定 discovery/target dataset、revision、from/through、rule 和 child，只允许严格更晚且不同的 dataset。合法两供体归纳后，应能追溯两个合法结构子代及其真实开发分数。同 dataset、时间重叠、无 proof、类型不符、same-shape 等反例不得通过。

没有供体、没有尝试、尝试全拒绝是不同状态。`DONOR_PAIR_ATTEMPT_NOT_PERSISTED` 必须保持未知，不能写 0。归纳是可选分支；缺合法供体不得堵住原子、分子、基础进化和其他就绪 data-only 家族。

### 5.3 持续性来自实际反馈，不来自重置计数

每族 40 个 foundation variant 是有限种子集合。长期持续性应由真实新输入、失败研究 action、合法结构提议和反馈处方推动；不得清空 variant 游标、改 family 名称或重复编号制造持续创造。

当没有合法新结构时记录 NO_OP 和原因；当存在已就绪工作时，原调度应按有界预算实际处理，不能长期仅更新 heartbeat。对研究建议，记录它究竟改变了哪个 child、哪项合法预算或哪个真实 action，并由相同 source/owner 的原件回读证明消费。

## 6. 滚动反馈、独立未来与主链消费

`RollingLearnerR343` 只消费自己的开发模型与合法 `COMPLETED` 结算来源，REAL 可以为 0。相同 issue/hash/receipt 重放不二次学习，异 hash 同 issue 按原合同拒绝。只有 `NO_PREDRAW_CANONICAL_TERMINAL` 时不生成假 rolling 事件，独立 DEV、首次合法冻结与其他已就绪研究仍继续。

滚动学习头、独立冻结版本、旧冻结的未来窗口分别保留。新标签可以训练新学习头，不能反改旧冻结模型、旧预测或已登记未来评价。新版本应能回读 model/source epoch/cutoff 与发布回执；缺绑定原件只暂停对应版本，不把全部原策略隔离。

历史快训、开发验证、SELECT/CONFIRM、自然 Future 与 REAL 分账。保留曝光账本、永久 alpha 索引、预登记窗口、固定校准、机会分母及原资格门。多个候选的大量搜索会使最高开发分偏高，不得凭历史最高命中、单轮高分、换 ID、换版本或复用旧证书绕过确认。

合成工厂能力验证、工程正负例只能签收相应工程范围。自然样本不足或真实表现不达标时保持 `NOT_PROVEN`，继续合法训练/影子，不降低门槛求出手，也不承诺自然必然达标、每代必强或盈利。

自然证据达到既有资格合同后，原发布与唯一决策门必须在下一合法赛前边界消费同一 frozen/model/protocol/certificate 身份，并由真实精确结算反向引用。不能只在排行榜、证书文件或“可调用”清单停留。资格撤销、模型改变或原件缺失继续执行原失效规则。

## 7. 依赖与分层验收不得形成循环等待

### 7.1 必须维持的工程与运行依赖

|对象|实际前置|不得额外增加的全局前置|
|---|---|---|
|B06 独立开发|B00/B03 工程就绪，以及该候选自身合法输入/协议/资源|B05 全部旧 pin/root 恢复、REAL、证书或全部图表有数据|
|B07 持续学习与创造|B00/B06；每条操作自己的开发模型/COMPLETED/教师/供体条件|所有教师、所有 R27 donor 同时完备才让其他路径运行|
|首次 development freeze|自身开发回执/模型/目标期/截止及原冻结条件|先有 rolling event/version 或自然证书|
|rolling freeze|实际滚动版本与自身原件/发布条件|要求旧首次冻结窗口结束后才允许新学习头训练|
|B08 工程图表|读取/投影/反例/UI 保全及合法研究动作的工程输入|R11/R14/R18 先产生 B09 自然证据才准工程退出|
|B09 自然主链|B04 的 PIT、唯一 owner、合法截止和精确结算工程条件，加该候选真实质量证据|先跑满全部 72 小时或恢复全部旧 root 才能做隔离工程测试|
|B10/B11|前批实际工程与交付证据及明确待验状态|自然策略必须取得好成绩才能交付已经完成的软件工程|

`B08_ENGINEERING_READY` 只表示对应工程验证通过；正式只读图尚缺 B09 自然原件时保留 `PENDING_B09`，后续回填。不能让“B09 依赖图表工程，而图表工程又要求 B09 自然成功”成为循环。

工程依赖就绪不代表绕过现场输入；现场等待也不代表工程不可继续。所有局部等待必须注明真实缺件和对应消费者，禁止隐式升级为全应用停止。

### 7.2 每层独立签收

|层级|所需证据|不能替代它的结果|
|---|---|---|
|方案/源锚|对象、方法、依赖、保全、源 SHA 与完整范围|类存在或文档生成不能证明应用运行|
|实现/主机|实际代码、合法隔离输入、数学/身份合同、正反例和执行日志|`git apply --check` 或抽取方法测试不能代签 Android/SQLite 全流程|
|Android/迁移|精确 APK、真实 runner、schema/事务/恢复/签名和覆盖安装|主机模拟不能代替用户数据库及设备|
|现场连续|同版本真实数据的冻结、结算、训练、消费、导出及完整机会分母|heartbeat、任务数、模型行数、图表缓存不能代替真实产物|
|自然质量|预登记角色/窗口、独立 Future、校准、证书及实际消费|历史高分、开发 early-stop 或合成工厂试验不能代替自然优势|

工程夹具的合法正例标明 `ENGINEERING_PATH_VERIFIED`，不得混入自然样本。没有执行的层级保持 `NOT_RUN`，自然尚未证明保持 `NOT_PROVEN`。不能用一个 PASS 覆盖全部层级。

## 8. 十个必须贯通并留证的场景

以下是施工必须执行的验收合同，文件存在本身不表示场景已通过。复用已有测试和夹具，只为缺失的关键风险补充测试；不为数量重复相同检查。

|编号|场景|必须观察到的正向结果|必须拒绝或保留的边界|
|---|---|---|---|
|E01|零出手启动|REAL=0、证书=0、全 NO_PREDRAW，但合法历史/catalog 足够：独立 DEV 完成，首次冻结按自身门推进|rolling_event 不造数，不把无预测终态改成 COMPLETED|
|E02|全无关旧原件缺失|旧 checkpoint/pin 缺失时旧对象受保护，无父代新基础实验仍完成开发|不继承旧权重、分数、样本、资格；不删除 ColdRebirth 原证明|
|E03|精确资源恢复|旧/新 backlog 分类在精确 cause+ADMISSION+ROLLED_BACK 下保持同 prepared，最终只提交一次|UNKNOWN 先回读；错 phase/cause、已提交和非白名单错误不泛化重试|
|E04|故障背压真实解除|混合可变/不可直接归档故障的配额场景按原件封存和回执合同排空，合法 prepared 小批次进入同 writer|不能只测试分类补丁；不能删队列、越过活动 freeze/settlement 或虚构物理资源|
|E05|真实训练与重启|所有已启用内核在合法工程输入下有真实模型/状态；同输入/种子/身份的中断恢复与连续过程一致|固定图不造优化步；错 ABI/hash/损坏状态不得续训成功|
|E06|合法结构子代|真实结构变化 child 有 exact proposal、typed compile、父引用、TRAIN 与开发分数，交叉/融合实际组合或消费父信息|参数、同构、复制父代分别分类；不强迫收益为正|
|E07|教师/供体局部不足|缺教师仅使 F23/Hedge/fusion 等待；无 donor 不造 pair 数；data-only、原子、分子继续前进|不塞均匀教师，不把未知写 0，不把可选分支升级为总门|
|E08|重复结算与冻结不变|无 REAL 的合法 COMPLETED 能驱动 rolling，同 receipt 重放不二次学习，旧冻结与未来窗保持不变|异 hash 同 issue、修改旧预测、用新模型反算旧未来结果不得通过|
|E09|资格拒绝与自然消费|工程正例证明门禁/发布/消费接口；自然达标后出现同身份唯一主链决策与精确结算|高开发分无 Future、晚冻结、错模型/校准、移植证书均拒绝；工程不冒充自然|
|E10|修复后仍可导出|研究/训练/资格等待与诊断失败并存时，报告仍由原 owner 导出；重复、取消后重试与原件保护可复核|不得把研究完成或所有检查 PASS 加为导出前置，不改外部 catalog 枚举掩盖内部新状态|

## 9. 原有功能、报告导出与最终交付

保留唯一 REAL/结算 owner、下一期分析请求与当期结算的既有次序、单位置固定 K=3、PIT 截止、SHADOW/REAL 分账、原冻结/校准/资格合同。历史回放、研究动作总数和影子覆盖不扩大真实出手范围。

报告继续由既有 diagnostics/`ReportExportServiceCP4` 路径处理。保留 96 检查、44 指标、附件/索引、单 ZIP/分卷、取消、重试、SAF 分享和语义验收。诊断失败可以真实记录，但必须保留有效 ZIP 或明确可恢复失败的导出能力；不得增加“所有研究完成”“全部检查通过”或“自然达标”前置。连续导出、导出中取消后重试、进程死亡恢复、低空间、慢盘和单检查超时都应按原合同签收。

不删库、不清失败队列、不把旧 `FAILED_TERMINAL` 改成成功、不补造过去预测，不把 0/空/null/UNKNOWN 相互替换。原件回收必须先有合法替代、hash 和回执，未闭合 root 保持保护。物理磁盘耗尽时明确暂停原因，不能承诺越过物理容量的实时保证。

每批沿既有构建/签名/版本/当前语义身份工具生成真实产物，保留历史封印，增量绑定本轮计划；版本号高于实际已发布和安装最高值。按实际能力完成保留数据覆盖安装，并验证同 APK 的重复导出。若设备、签名或原件确实不具备，应明确标记对应未验层并继续其他可完成工作，不能虚报已安装或已验证。

最终交付必须包括逐批源码差异、实际构建与测试日志、APK SHA/签名/版本/下载位置、覆盖安装与数据保留证据、训练/冻结/结算/消费原件、导出结果、回退条件、未完成现场或自然窗口的具体状态。46 卡逐项回填，旧要求保留未证实状态。**完工是实际生产与消费可追溯、报告可靠、原功能保全；不是把报表改为全绿。**

## 10. 精确 211 代码依据

以下为关键调用与机制锚，均对应 `5c7f1b36eaee6036df280e85835bb0fb4ce0b40d`。具体修改前仍须用本包 `SOURCE_ANCHORS.json` 及实际合法后代差异核对。

- [RuntimeCoordinatorR26](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java)：`runModernContinuity`，现代独立调度入口。
- [ResearchLearningContinuityR912](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/ResearchLearningContinuityR912.java)：`pulseWithinR927` 与 lane 13 的现有 Director 调用。
- [ModernFamilyFactoryCP12](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java)：`prepare/maintain`、15 family 与实际 ModelKind、独立基础种子。
- [ResearchTrainingHookRegistryR952](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/r952/ResearchTrainingHookRegistryR952.java)：`drainTrainingSlotCP11`、`drainTrainingKindR65`、`advanceTrainingSlotCP11`；首次冻结与 rolling 冻结分别服务。
- [DevelopmentProductionCP12](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java)及[DevelopmentTrainerCP9](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java)：固定协议、特征前缀、数值内核、验证与恢复。
- [ColdRebirthR34](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdRebirthR34.java)：`prepare/step` 的原 parent/state/hash 前置，不能以缺原件强行冷重生。
- [AtomPublicationStoreR31](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomPublicationStoreR31.java)及[ResearchContextR30R1](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java)：typed donor/pair、规则发布及更晚不同 dataset 的消费。
- [RollingLearnerR343](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/r952/RollingLearnerR343.java)：`settled/planOne/trainSlice` 的真实结算、开发模型与版本条件。

本附约列出的场景与交付要求须在施工时实际执行。它不声明应用已修复，不替代 Android/现场验收，也不授予任何自然质量或交易资格。
