# 检测报告提出的升级方向逐条施工表

适用采样至 202611240604。表内保留本作业的逐对象原文、来源和首个工程签收入口；完整输入、生产/消费状态、反证、案例标识及其他责任用例见 [能力诊断对照](evidence/field3/directions.json)。正文中的“预期”是待检验假设。

## 策略、模型、方法和运行能力

| 对象 | 报告提出的升级动作 | 回查位置 | 首个工程用例 |
|---|---|---|---|
| capabilities:F01 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：稀疏跨位置滞后组。固定目标期，切掉跨位置边并保留本位置，检验组收缩后独立 SELECT 的概率损失、K3 动作和成本。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F02 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：排列链相邻因子。将同一冻结图的相邻成对因子归零，保留一元项，检查合法排列边际和同期差。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F03 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：类型化三元消息。屏蔽一种三元消息通路，保持参数预算，核验真实梯度、十位置输出及差值。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F04 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：膨胀时间卷积。分别屏蔽长滞后卷积层与短滞后层，对同一序列做断点重置和未来输入扰动测试。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F05 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：时间注意力。对过去可见行比较已学权重和均匀权重，延迟新特征一步并验证未来标签不改变旧输出。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F06 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：选择性状态空间模型。分别冻结选择门与重置时序状态，跟踪梯度、代价及跨断期后的恢复值。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F07 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：图结构 GRU。分别切图边和循环门，保持训练窗口及资源相同，对照独立期概率损失和行为变化。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F08 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：因果 HMM 过滤。按原图变体切除状态转移记忆，比较过滤概率与重启前后状态。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F09 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：显式时长半马尔可夫。固定发射分布，进行移除时长项的对照，按停留时长分层检查校准及稀疏支持。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F10 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：在线变点。记录每次变点判断的可见数据末期，与冻结危害率的对照计算延迟、误报和换窗后损失。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F11 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：仅历史片段检索。对每个被选邻居验证结束期早于目标，固定或打乱距离权重比较 SELECT 概率损失。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F12 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：关系后缀自动机。比较有支持的状态分裂与退回父状态，列最小支持、失败片段和跨期预测差。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F13 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：类型化符号公式。固定公式树逐叶消融，保留函数类型、系数和合法滞后，记录真正改变 K3 的子树。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F14 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：共享状态宏。用相同单头特征比较共享宏启用与关闭，验证十位置共用状态重启后可复演。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F15 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：单路集合风险。固定概率面比较风险评分与简单 top-3，记录 1200 个候选动作中最终一个位置和三个号码。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F16 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：双路集合路由。比较原软路由、左右单路和固定等权，用同机会出手集合检查风险校准收益。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F17 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：跨位置滞后一的排列读出。打乱源位置或切滞后项，验证输出仍是合法边际且选出的三号码可复算。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F18 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：掩码自回归。保持已用号码约束，比较移除顺序依赖的较简模型；验证重复号码拒绝和边际归一。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F19 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：CP 关系张量。按现有变体移除位置或状态模式，保持 AdaGrad 累积量可恢复，在相同训练步比较十位置损失。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F20 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：因果状态片段。比较匹配检索、错配检索与过期记忆，核对检索只用旧片段和覆盖变化。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F21 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：折扣关系统计。冻结折扣因子并打乱源位置，与边际频率配对，报告有效支持和真实增量更新。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F22 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：冻结父代后的残差学习。固定父参数和分段边界，比较父单体、关闭残差门和子代，子代证据从零开始。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:F23 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：教师蒸馏。先由 FamilyRegistryR26.bindLegacyTeacher 校验 asset、teacher、state 和 ABI，再对同输入比较学生与去教师学生。缺教师只挂起 F23。 | FamilyRegistryR26.build(F23)与教师绑定回执 | R341-B06-02-A01 |
| capabilities:F24 | 先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行：三模型组合。对 F01、F09、F11 各做单体和等权对照，再比较学习路由，记录权重、选择范围和同期净差。 | FoundryCampaignR26.step：当前家族训练、checkpoint和消费者 | R341-B06-02-A01 |
| capabilities:E01 | 逐条扰动 t 或 t 以后的标签，赛前输入哈希和当期动作应恒定；审计回执关联违规原件。 | 对照本行专家实现和审计回执，以对应违规或故障复核审计结果 | R341-B06-04-A01 |
| capabilities:E02 | 同一旧资产恢复模型和状态并重放，记录首次分歧，兼容声明通过后才可提出 warm 子代。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E03 | 在 DEV 登记短期窗口的出现与失效，SELECT 作同机会去窗口对照，未来只验证。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E04 | 固定不同尺度的特征终点，删除长窗复验，分开报告滞后与信号增量。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E05 | 切除跨位边并保持单位置边际，只有改变实际选择或损失才记录贡献。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E06 | 记录号码组围号及其合法支持，取消组结构重放，区分更窄覆盖和真实增益。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E07 | 将表示学习提案绑定可消费权重哈希，对相同输入移除表示分支，记预测差。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E08 | 产出可执行的类型化公式，拒绝改名与同构；完成首步数值训练再计有效出生。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E09 | 把具体失败簇绑定到父规则和拟替换节点，测试修复在独立段是否过度专门化。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E10 | 选择同时期且残差互补的双亲，缺父代时留原因，融合后与两个单亲配对。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E11 | 核对继承的参数与优化器 ABI，错版本只允许合规冷启，子代不得继承父胜率。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E12 | 从冻结概率重算校准和门控，与固定阈值比较覆盖、ECE、Brier 及机会损失。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E13 | 固定漂移触发点，检查分块损失、最差块和错误报警，并提出降权或退役的依据。 | ExpertProposalsR27：提案与下游出生的关联 | R341-B06-04-A01 |
| capabilities:E14 | 按原资源与编译审计职责记录真实 CPU、内存和可执行 ABI；审计回执单独计数。 | 对照本行专家实现和审计回执，以对应违规或故障复核审计结果 | R341-B06-04-A01 |
| capabilities:E15 | 审计从资格原件到主链的冻结键，列撤销竞态和拒绝原因；只有 DecisionGate 可签 REAL。 | 对照本行专家实现和审计回执，以对应违规或故障复核审计结果 | R341-B06-04-A01 |
| capabilities:E16 | 冻结输入和复现实验版本，给首个阻塞、反例与保全原件；审计记录无需制造一个新子代。 | 对照本行专家实现和审计回执，以对应违规或故障复核审计结果 | R341-B06-04-A01 |
| capabilities:I00 | 预注册随机对照的种子和机会集，记录误报及配对下界，仅作负对照。 | 按登记的同期对照、冻结键和独立期检查比较分母 | R341-B09-05-A02 |
| capabilities:I01 | 比较短长窗频率与同位置平滑频率，固定收缩规则后只在新窗口计分。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I02 | 只用上次出现以前的遗漏序列，和无周期项的规则同机会比较。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I03 | 冻结转移阶数，稀疏转移退回低阶，检验退回前后概率损失。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I04 | 保留目标位置，打乱其他位置或切跨位边，检验非边际信息。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I05 | 在同一概率面上开关排列约束，检查输出是否改变 K3 及校准。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I06 | 固定状态切换和失效条件，按切换前后独立窗口核对延迟和误率。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I07 | 历史候选片段的结束期先于 t，去掉相似检索后比较同目标损失。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I08 | 只接受已发布的同 ABI 权重面，按 model_hash 找到调用后预测，与去表示模型对照。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I09 | 记录类型化 AST、可用特征与滞后，等价表达式规范化去重，逐子树消融。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I10 | 按时间冻结失败簇，对同一失败机制比较原父代和指定修复子树。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I11 | 先验证双亲在同一期有互补误差，再与优良单亲和等权融合比较。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I12 | 对旧资产重放冻结输入，核对语义 ABI 和输出，再登记新子代独立证据。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I13 | 预注册出手分数阈值；分别报告全机会、共同出手、动作并集的质量与频率。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I14 | 记录围号两个来源位置、号码集合、交集与投影规则；用单来源对照复验固定位置 K3。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I15 | 只用两个截止于 t 以前的窗口，冻结差分符号与窗口长度，比较去差分基线。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I16 | 以预登记的位置轮换顺序和固定位置作配对，分解覆盖变化与真实收益。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I17 | 设置遗漏回补触发与失效期限，在相同遗漏长度桶对照简单频率，记录稀疏支持。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I18 | 拆分邻号与原位转移贡献，打乱邻接关系后重新冻结并比较。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I19 | 只用已揭晓期组合奇偶大小状态，在同支持数下比较去状态与平滑边际。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:I20 | 把条件发生、交集产生及不足而 HOLD 分别记账；条件在 t 以后出现不得改变已冻结结果。 | EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证 | R341-B06-03-A01 |
| capabilities:ATOMNET | 核对十位置同一epoch、optimizer、非零更新及权重哈希。完成一轮后回读发布的模型，再追踪在线预测的模型引用。 | AtomNetRuntimeR30.step → AtomNetPublicationR31 → ModelSurfaceStoreR30 的发布/消费 | R341-B07-02-A01 |
| capabilities:MATRIX:R930:EVIDENCE_TEACHER | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:STUDENT_ERROR | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:FORWARD_REASONING | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:REVERSE_REASONING | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:COUNTERFACTUAL | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:FEEDBACK | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:CONTRADICTION | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:TEACHER_DIVERSITY | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:TEMPORAL_PRECEDENCE | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:INVARIANCE | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:CAUSAL_SUPPORT | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:COUNTER_EVIDENCE | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:RESIDUAL_MECHANISM | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:NUMBER_POSITION_COMPATIBILITY | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:CROSS_POSITION_FLOW | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:ACTION_UTILITY | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:ALGORITHM_COMPLEMENTARITY | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:EVOLUTION_CREDIT | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:R930:RESOURCE_EFFICIENCY | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| capabilities:MATRIX:SU2:LEARNED_FACTOR_TEACHER_CHECKPOINT | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |

## 矩阵

| 对象 | 报告提出的升级动作 | 回查位置 | 首个工程用例 |
|---|---|---|---|
| matrices:R930:EVIDENCE_TEACHER | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:STUDENT_ERROR | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:FORWARD_REASONING | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:REVERSE_REASONING | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:COUNTERFACTUAL | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:FEEDBACK | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:CONTRADICTION | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:TEACHER_DIVERSITY | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:TEMPORAL_PRECEDENCE | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:INVARIANCE | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:CAUSAL_SUPPORT | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:COUNTER_EVIDENCE | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:RESIDUAL_MECHANISM | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:NUMBER_POSITION_COMPATIBILITY | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:CROSS_POSITION_FLOW | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:ACTION_UTILITY | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:ALGORITHM_COMPLEMENTARITY | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:EVOLUTION_CREDIT | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:R930:RESOURCE_EFFICIENCY | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |
| matrices:SU2:LEARNED_FACTOR_TEACHER_CHECKPOINT | 写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。 | 按本行producerMethod/algorithmConsumer比较原始矩阵与消融 | R341-B08-03-A02 |

## 研究图

| 对象 | 报告提出的升级动作 | 回查位置 | 首个工程用例 |
|---|---|---|---|
| researchCharts:R01 | 按冻结窗口读结构状态与下一期目标，比较固定窗口与移除状态输入的子代，保留所选窗口和配对差。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R02 | 追回该期赛前结构概率，按相同目标绑定开奖误差，将失败簇交给FailureProfile，缺冻结时列未观察期。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R03 | 查九个同伴位置的事前输入和掩码，生成跨位对照与消融，登记同一目标的子代变化。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R04 | 回读各位置候选集合、交集及支持数，保持单位置K3。交集不足记弃权，用相同期的单集合为对照。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R05 | 沿失败簇、专家处方、AST、子代逐键追踪，记录首个缺失键，确认出生提案被真实执行。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R06 | 按相同行为窗口对照保留和移除该原子的候选，读取实际PRUNE_COMPONENT操作及其子代。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R07 | 检查两个合法组件与同角色父代，缺任一即列组件ID，齐备后比A、B、A+B并计成本。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R08 | 在事前冻结的探针集比较行为哈希和误差相关，复核BirthGate的去重或保留回执。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R09 | 将专家处方绑定到实际出生的AST和子代并复核后续支持，只有文本时定位首个未执行环节。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R10 | 比较同计算预算的变异算子、时间成本与同机会子代收益，回读下一轮实际分配。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R11 | 只读核对父子同一目标冻结和确认原件，显示负差与区间，标明缺配对原因。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A03 |
| researchCharts:R12 | 核实两个父代同目标与共同失败域，读新融合子代独立身份，用最佳单体与固定组合交叉比较。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R13 | 按冻结前固定校准器复核支持域、加权误差和BSS，列出域外弃权及受影响的研究候选。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R14 | 只读串联已提交权重、十位置概率面、调用模型哈希和冻结引用，列出第一条不匹配。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A03 |
| researchCharts:R15 | 按同机会比较无回放、固定回放和候选回放模型的遗忘与代价，记录后续回放选择。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R16 | 复核每类计算预算、CPU和唯一有效子代数，比较下一轮真实发放额度，重试去重。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R17 | 用失败机制身份匹配重复消耗，验证预算调整及活跃模型保护，保留负例。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A01 |
| researchCharts:R18 | 只读串联处方、子代、证书、唯一决策和结算；有拒绝时给出拒绝者和对应源行。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | R341-B08-02-A03 |

## 历史图表

| 对象 | 报告提出的升级动作 | 回查位置 | 首个工程用例 |
|---|---|---|---|
| legacyCharts:status | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p01_three_scores | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p02_generation_holdout | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p03_evolution_events | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p04_seven_counters | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p05_lineage | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p06_idea_survival | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p07_funnel | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p08_bss | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p09_rolling200 | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p10_train_holdout_loss | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p11_eprocess | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p12_mainline_weight | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p13_runtime_health | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p14_atom_scores | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p15_paired_improvement_r30 | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:p16_capability_coverage_r30 | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_algorithm_all | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_algorithm_rank | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_best_curve | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_action_candles | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_all_hit | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_act_hit | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_number_candles | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_number_line | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_omission_heat | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_hot_cold | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_calibration | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |
| legacyCharts:r83_brier20 | 对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。 | ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费 | A-B11-01b |

## 研究逻辑表

| 对象 | 报告提出的升级动作 | 回查位置 | 首个工程用例 |
|---|---|---|---|
| logicalTables:T01 | 按 dataset_hash 和 role 连接训练运行水位，逐期拒绝晚到标签，报告第一处可见时间冲突。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T02 | 核查 r31_atom_score_visible 已提交的评分版本与 n_select，缺失率过高的原子暂停供给，回放同一输入。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T03 | 用 target_ordinal 对齐赛前 frozen_at、deadline_at 与开奖结算。按策略列缺失冻结和逾期，并保留只读未来角色。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T04 | 把失败窗口绑定到具体 parent_hash、输出和误差，再登记可复验的修复操作，追踪下一次出生。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T05 | 在同一 SELECT 原始期比较保留或移除原子的增益与下界，无配对支持的原子返回等样本。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T06 | 从同一父程序提供两个合法加法组件，分别消融A、B及组合，记录配对分母、协同差和成本。 | PairedComponentStudyR30R1：同目标两个组件/父代 | A-B11-04a |
| logicalTables:T07 | 固定 dataset_hash 与 probe_set_hash，重演父子行为；同一签名合并来源，保留去重和释放槽的记录。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T08 | 把专家、提案和出生后的可执行 policy_hash 对齐，统计真正被接纳、拒绝和待处理的提案。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T09 | 按 role、kind、tier 读取 frame.points 和 unit，在同预算下比较算子净增益，再调整下一代份额。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T10 | 提供同目标同角色的两个已冻结父策略，分别与子代、移除一方和均匀组合比较，写独立子代证据。 | PairedComponentStudyR30R1：同目标两个组件/父代 | A-B11-04a |
| logicalTables:T11 | 追踪权重版本到逐位置 model_surface、训练水位和真实预测的同一模型哈希；追查缺失的位置与发布事务。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T12 | 返回构成校准缓存的冻结概率和揭晓结果，按同机会重算分箱、ECE与支持数，错误缓存应失效。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T13 | 核对锁定块、策略哈希与 registration_experiment_id；未通过确认和登记失败分别计数，保留未来统计角色。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T14 | 从最老待办查进展、lane及阻因，比较重启前后已提交游标，修复有输入而永久得不到服务的任务。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |
| logicalTables:T15 | 以 proposal_id 关联 consume.receipt_hash、child_policy_hash 和独立验证，给出未消费的具体阻因。 | ResearchObservatoryR30R1：处方、child与消费引用 | A-B11-04a |
| logicalTables:T16 | 从决策说明追到同一期权威冻结、trades 与结算，分别列真实出手和HOLD，缺结算保持未观察。 | ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者 | A-B11-04a |

## 8 项能力需求

| 能力 | 分类 | 生产者 → 消费者 | 原报告动作与正反例 |
|---|---|---|---|
| 家族构建、训练和状态恢复 | EXISTING_CAPABILITY_BLOCKED | 构建已注册家族 → Foundry按预算训练并提交状态 | 沿原candidate和owner/fence查训练输入；缺绑定补接口，数值ABI不匹配保全旧父代并生成独立子代。；正例：读取真实权重变化、cursor提交及重启恢复相等原件；反例：错ABI、缺教师、只有RUNNING无提交不得算训练完成 |
| 十位置实训到模型面消费 | EXISTING_CAPABILITY_DECLARED_RUNTIME_UNVERIFIED | 梯度训练并发布模型 → 冻结时同模型概率面消费 | 按publicationId采集样本、梯度、optimizer、模型哈希、发布与消费；归档失败独立隔离。；正例：参数重算与重启一致；在线冻结引用同modelHash；反例：材料样本增加但training=0；只文件无消费；错epoch不得通过 |
| 研究图原件到真实研究动作 | EXISTING_CAPABILITY_DECLARED_RUNTIME_UNVERIFIED | 有界研究分析 → 子代或非出生动作写入原事务回执 | 由frame/run查实际consumer，缺消费边先接线；不得以建议或图形代替执行。；正例：同来源动作与下一轮预算/子代身份一致；反例：孤立frame、重试重复计产出、来源hash不符时拒绝 |
| 独立确认与自然未来资格 | EXISTING_CAPABILITY_DECLARED_RUNTIME_UNVERIFIED | 固定确认合同和结果 → 唯一资格owner按连续日历消费 | 独立未来为0时沿冻结/开奖/资格缺口原键定位；工程连通不提升自然资格。；正例：同机会原始冻结、开奖、分母、区间、频率及证书可复算；反例：重放混自然、偷看未来、漏期、旧证书借用均拒绝 |
| 唯一真实决策到权威结算 | EXISTING_CAPABILITY_DECLARED_RUNTIME_UNVERIFIED | 唯一DecisionGate决策 → SettlementEngine幂等结算 | 按同decisionId/目标期追冻结、trades和结算；错过赛前保留原因，不补造冻结。；正例：重复乱序开奖只结一次，原决策与结算逐键一致；反例：FROZEN_OBSERVATION_MISSING不能靠开奖后回填消除 |
| 逻辑表来源与有界页验真 | EXISTING_CAPABILITY_DECLARED_RUNTIME_UNVERIFIED | 只读原表查询 → 逐表语义审计 | 空首页分别核真空集、来源缺失、过滤和分页；完整分母不从展示条数推算。；正例：相同范围UI/SQL可复算并能翻页；反例：缺表、截断、取消、跨水位不得报告正常空集 |
| 同水位原始任务与故障追踪 | EXISTING_CAPABILITY_DECLARED_RUNTIME_UNVERIFIED | 捕获原始任务/故障行 → 对象诊断只读绑定 | 从同水位event_id/owner/fence取得真正输入与独立复演；共享owner只作定位。；正例：报告对象能回到同一原行和原operation；反例：错owner/fence、篡改hash、旧样本、共享任务不能称对象根因 |
| 有限需求目录与缺能力提案 | EXISTING_CAPABILITY_DECLARED_RUNTIME_UNVERIFIED | 核需求与实际生产入口 → 独立候选提案数组，不改固定165索引 | 所有约定owner均可核且入口全缺才提新增；部分缺失补绑定；读取失败保持未知。；正例：缺类/缺方法/完整绑定/调用受阻四类明确分流；反例：LinkageError、来源缺失、重复对象不得成为NEW_CAPABILITY_PROPOSAL |

## 存档能力体检与后续触发

| 能力 | 原状态 | 原方向 | 原验法 |
|---|---|---|---|
| 信息探针（P50） | 无数据 | — | — |
| 延续/转向矩阵（P49） | 检测中 | 保持检测，不出手 | 影子 600 次出手 |
| 阶段层（P49） | 无数据 | — | — |
| 区制记忆（P48） | 检测中 | 保持检测 | 下界 >0 持续 4 段 |
| 在线校准（P36） | 无数据 | — | — |
| 裁判与选择（P38/P40） | 无数据 | — | — |
| 委员会冠军（P37） | 无数据 | — | — |
| 进化与算子（P21/P41） | 无数据 | — | — |
| 研究记忆与原子（P44/P43） | 无数据 | — | — |
| 教师矩阵与原子蒸馏（P45/P47） | 无数据 | — | — |
| 主链 IPM（P30/P03） | 无数据 | — | — |
| 存储与内存（P31–P34/P50） | 无数据 | 保留规则照常执行 | 下一次普查 |

原文未给出升级动作的位置保留“—”。工程补充见 [原子与模型](specs/field3/learning.md) 和 [研发对照](specs/field3/research.md)。

## 诊断规则和指标的行动与反证

| 诊断项 | 观察/状态 | 行动或下次采集 | 反证或指标目标 |
|---|---|---|---|
| WRITER_HARD | DETECTED | 在目标设备运行实际写者并收集 WriterHoldLedgerR30.record 的窗口、operation、count 和 hold/wait 分位数 | 同一范围原始来源恢复一致且对应回归通过 |
| TASK_STALLED | UNKNOWN | 采集 r30r1_task_progress 的 pending、last_commit_clock_ordinal、work_unit、artifact_identity 和当前真实期号；补齐被限量展示省略的任务，依赖等待与等待新期分别记录 | 同一范围原始来源恢复一致且对应回归通过 |
| RECEIPT_STATE | CLEAR_IN_CAPTURED_SCOPE | 导出 ACTIVE 正永久序号实验的全部 receipt、eprocess、cursor 及原始冻结；缺表先记录 SchemaR27.ensure 的实际失败 | 同一范围原始来源恢复一致且对应回归通过 |
| QUAL_CONFLICT | CLEAR_IN_CAPTURED_SCOPE | 采集 r29_qual_conflict、draws、原始来源和对应冻结记录及其水位 | 同一范围原始来源恢复一致且对应回归通过 |
| UNSAFE_DECISION | CLEAR_IN_CAPTURED_SCOPE | 采集 r30_decision_explain、r30_decision_mismatch、完整冻结/证书/intent/REAL 与来源代号 | 同一范围原始来源恢复一致且对应回归通过 |
| PIT_REJECTIONS | CLEAR_IN_CAPTURED_SCOPE | 采集守卫的真实 code/source 和原冻结输入；运行当前及未来标签扰动反证，明确运行环境与夹具范围 | 对应原始配对数据和独立验证回执，不能仅凭行数 |
| CONFIRM_EVIDENCE | INSUFFICIENT | 导出 r30_frozen_confirm 与 r30_confirm_read 的冻结候选/数据hash、预约和完成状态 | 对应原始配对数据和独立验证回执，不能仅凭行数 |
| MODEL_STATE | RECORDED_UNVALIDATED | 经原诊断导出 checkpoint 元数据与解码结果、实际训练任务状态；完整模型仅保留在原受保护产物内 | 对应原始配对数据和独立验证回执，不能仅凭行数 |
| MODEL_CONSUMER | INSUFFICIENT | 采集 r30_model_call、r30_model_surface、模型元数据和原冻结引用的同一期配对 | 对应原始配对数据和独立验证回执，不能仅凭行数 |
| CALIBRATION | INSUFFICIENT | 采集原校准行与 calibration-r30 同数据集缓存的 validationRows/fitEnd/validateStart/支持范围；缺失仍为 UNKNOWN/INSUFFICIENT | 对应原始配对数据和独立验证回执，不能仅凭行数 |
| NATIVE_MATRIX | INSUFFICIENT | 采集20对象清单与原矩阵来源表的 count/latest issue/hash，缺来源保持 null | 对应原始配对数据和独立验证回执，不能仅凭行数 |
| SU2_MATRIX | INSUFFICIENT | 采集 su2_matrix_snapshot 的真实数量、checkpoint_sequence、updated_at 与20对象注册来源 | 对应原始配对数据和独立验证回执，不能仅凭行数 |
| ABI_CONTINUITY | INSUFFICIENT | 采集当前源版本对应的 ABI 检查行、完整checkpoint解码和中断恢复差异 | 对应原始配对数据和独立验证回执，不能仅凭行数 |
| ABLATION | INSUFFICIENT | 采集原消融行和父子同一期机会配对明细、样本数与单侧95%区间；禁止选好窗口 | 对应原始配对数据和独立验证回执，不能仅凭行数 |
| NATURAL_SAMPLE | INSUFFICIENT | 在目标设备采集事前登记到自然FUTURE结算的原始冻结、ACT命中、时间块、校准和序贯证据 | 对应原始配对数据和独立验证回执，不能仅凭行数 |
| SNAPSHOT_CAPTURE | CLEAR_IN_CAPTURED_SCOPE | 保留真实异常和当前 schema_version/来源表元数据，启动后重新导出同一快照；不可读值保持未知 | 同一范围原始来源恢复一致且对应回归通过 |
| M01 源码与安装身份 | INSUFFICIENT | 采集 构建身份：release_manifest.json + APK 签名指纹 + 源码提交（tools/r29/package_receipt.py）；按 [FR-B00-01, FR-B11-04] 验证，禁止用编译通过代替 | 全部一致 |
| M02 功能保全覆盖 | INSUFFICIENT | 采集 保全清单 1832/299（legacy_count.py）+ 生命周期回归 androidTest；按 [FR-B00-03, FR-B11-01] 验证，禁止用编译通过代替 | 100%有记录；无凭空VERIFIED |
| M03 准时预测持久化 | INSUFFICIENT | 采集 r30_issue_timeline：request_to_frozen 与 settled_to_frozen 段 P95（IssueTimelineR30.summarize）；按 [FR-B02-04, FR-B02-05] 验证，禁止用编译通过代替 | P95≤5000ms |
| M04 抓到后正式结算 | INSUFFICIENT | 采集 r30_issue_timeline：observe_to_settled 段 P95；按 [FR-B02-03, FR-B02-04, FR-B02-05] 验证，禁止用编译通过代替 | P95≤1000ms |
| M05 赛前有效覆盖 | INSUFFICIENT | 采集 r30_issue_timeline：NEXT_DEADLINE−NEXT_FROZEN ≥5000ms 的期数占比；按 [FR-B02-04, FR-B02-05] 验证，禁止用编译通过代替 | 目标100%；受支持健康场景≥99.5% |
| M06 唯一正式结算 | RECORDED_UNVALIDATED | 采集 结算表唯一键冲突计数 + 同 issue REAL 意向计数（SQL 见 docs/90）；按 [FR-B02-05, FR-B02-06, FR-B05-05] 验证，禁止用编译通过代替 | 均为0 |
| M07 实时写事务 | INSUFFICIENT | 采集 r30_writer_hold critical=1 的 wait_p95 最大值；24h 失败数=sum(failures)；按 [FR-B02-01, FR-B02-02, FR-B02-03, FR-B02-06] 验证，禁止用编译通过代替 | P95≤200ms；24h失败0 |
| M08 分库隔离 | INSUFFICIENT | 采集 单库模式：SCHEDULER_OWNERS.json 中同表双写者数；B11 启用后：日志 R30_RESEARCH_TABLE_FROZEN 次数；按 [FR-B00-02, FR-B10-04] 验证，禁止用编译通过代替 | 0 |
| M09 迁移一致性 | INSUFFICIENT | 采集 r30_copy_log COUNT_MISMATCH 行数 + B11 ContentHash 不一致数；按 [FR-B10-04] 验证，禁止用编译通过代替 | 全部一致；丢失/重复0 |
| M10 每期训练投递 | INSUFFICIENT | 采集 每期训练投递：r952_research_outbox 状态分布 + QualificationInboxR29 连续游标；按 [FR-B02-05, FR-B03-03] 验证，禁止用编译通过代替 | 100%；重复消费0 |
| M11 训练完成新鲜度 | INSUFFICIENT | 采集 TaskLivenessR30：最老积压期数（pending 最大任务）；按 [FR-B03-02, FR-B04-01] 验证，禁止用编译通过代替 | 正常负载最老积压≤5期；超限必须FAIL |
| M12 任务活性 | INSUFFICIENT | 采集 TaskLivenessR30.summarize.m12（null=有UNKNOWN）；按 [FR-B03-02, FR-B03-03, FR-B03-04, FR-B03-05] 验证，禁止用编译通过代替 | 0；等依赖/等数据单列 |
| M13 前后台连续性 | INSUFFICIENT | 采集 B10 场景：前后台切换前后已投递任务数差；按 [FR-B03-05, FR-B11-02] 验证，禁止用编译通过代替 | 受支持场景不丢已投递任务；缺口全记录 |
| M14 进程稳定性 | INSUFFICIENT | 采集 进程崩溃/ANR 计数（logcat + ApplicationExitInfo）；按 [FR-B11-02] 验证，禁止用编译通过代替 | 0 |
| M15 热库与总磁盘 | INSUFFICIENT | 采集 draws 热库行数 + 应用数据目录总字节；按 [FR-B10-01, FR-B10-02] 验证，禁止用编译通过代替 | draws≤5000；未超配置总预算且可恢复 |
| M16 报告取消 | INSUFFICIENT | 采集 ReportTaskControlR69 取消到释放耗时；按 [FR-B10-03] 验证，禁止用编译通过代替 | ≤2000ms |
| M17 报告完整性 | INSUFFICIENT | 采集 报告索引覆盖率 + ZIP 大小/分卷；按 [FR-B01-05, FR-B10-03] 验证，禁止用编译通过代替 | 全量索引100%；默认ZIP≤20MiB或明确分卷 |
| M18 PIT泄漏反证 | INSUFFICIENT | 采集 r30_pit_violation 非注入行数；按 [FR-B00-04, FR-B04-03] 验证，禁止用编译通过代替 | 0 |
| M19 训练分割污染 | INSUFFICIENT | 采集 HoldoutLedgerR27 R30_CONFIRM_REUSED 计数；按 [FR-B00-04, FR-B05-02] 验证，禁止用编译通过代替 | 0 |
| M20 全状态续训恢复 | INSUFFICIENT | 采集 ModelArtifactR29 中断/恢复一致性测试结果；按 [FR-B04-03] 验证，禁止用编译通过代替 | 按已登记浮点容差一致；不重复学习 |
| M21 策略完整合同 | INSUFFICIENT | 采集 BirthGateR27 R30_CONTRACT: 拒绝码分布；入池策略合同违规=0；按 [FR-B06-04, FR-B08-01] 验证，禁止用编译通过代替 | 100%；空壳拒绝 |
| M22 结构与行为去重 | RECORDED_UNVALIDATED | 采集 r30_behavior_dup 行数与报告去重计数一致；按 [FR-B08-01, FR-B08-03] 验证，禁止用编译通过代替 | 计数与ledger一致 |
| M23 族专家覆盖 | BLOCKED | 采集 CapabilityRegistryR30 对象可执行率；按 [FR-B06-05] 验证，禁止用编译通过代替 | 全部注册与可执行；没有证据不标有效 |
| M24 模型实际消费 | INSUFFICIENT | 采集 r30_model_call 与冻结行匹配率；按 [FR-B04-05] 验证，禁止用编译通过代替 | 100%匹配或明确BLOCKED；不得伪造调用 |
| M25 矩阵接线 | INSUFFICIENT | 采集 MatrixRegistryR30 20 项可追溯率；按 [FR-B06-01, FR-B06-02, FR-B06-03] 验证，禁止用编译通过代替 | 全部可追溯；未产出有明确原因 |
| M26 校准时间隔离 | INSUFFICIENT | 采集 校准样本时间隔离检查通过率；按 [FR-B06-06] 验证，禁止用编译通过代替 | 100%；零模型需可定位原因 |
| M27 资格预算连续 | RECORDED_UNVALIDATED | 采集 SequentialQualificationR27 全局序号连续、无重置；按 [FR-B05-03, FR-B05-04] 验证，禁止用编译通过代替 | 全部通过 |
| M28 主链闭环 | INSUFFICIENT | 采集 androidTest 正对照贯通 + 非法/过期拦截；按 [FR-B05-05, FR-B11-01] 验证，禁止用编译通过代替 | 合法正对照贯通；非法/过期全部拦截 |
| M29 确认队列连续 | INSUFFICIENT | 采集 确认队列被挤出次数；按 [FR-B05-01, FR-B05-02] 验证，禁止用编译通过代替 | 0 |
| M30 主链选择器身份 | INSUFFICIENT | 采集 r30_decision_explain：selectorConfirmed=FAIL 时 REAL 次数（必须0）；按 [FR-B05-04, FR-B05-05] 验证，禁止用编译通过代替 | 未确认组合不能REAL |
| M31 诊断可追溯 | RECORDED_UNVALIDATED | 采集 DiagnosisRulesR30 规则行动覆盖率；按 [FR-B01-01, FR-B01-02, FR-B01-03] 验证，禁止用编译通过代替 | 100%或UNKNOWN并给采集动作 |
| M32 报告口径一致 | UNKNOWN_SOURCE_MISSING | 采集 同快照两章同指标冲突数；按 [FR-B01-04, FR-B01-05, FR-B09-05] 验证，禁止用编译通过代替 | 0 |
| M33 图表回归 | INSUFFICIENT | 采集 图表页面 androidTest 截图通过数；按 [FR-B09-03, FR-B09-04, FR-B09-05] 验证，禁止用编译通过代替 | 全部场景可打开且口径一致 |
| M34 签名覆盖安装 | INSUFFICIENT | 采集 覆盖安装首启/二启无崩溃（两证书）；按 [FR-B10-04, FR-B11-04] 验证，禁止用编译通过代替 | 成功；首启/二启无崩溃 |
| M35 失效模型撤销 | INSUFFICIENT | 采集 证书撤销到 DecisionGate 拒绝的耗时；按 [FR-B05-05] 验证，禁止用编译通过代替 | 目标≤5000ms；不含APK回滚 |
| M36 同池对照 | INSUFFICIENT | 采集 NullEvolutionR27 同池对照输出可复算；按 [FR-B07-05, FR-B08-05] 验证，禁止用编译通过代替 | 有可复算输出；合成不计真实质量 |
| Q01 自然ACT命中 | INSUFFICIENT | 采集 真机 FUTURE ACT 命中率（事前登记策略）；按 [FR-B11-03] 验证，禁止用编译通过代替 | ≥34%，优先≥37%；未达不得宣称达标 |
| Q02 真实频率 | NOT_DEPLOYED | 采集 真机实际出手频率（不补配额）；按 [FR-B11-03] 验证，禁止用编译通过代替 | 目标≥15%；质量支持扩大；不得补配额 |
| Q03 真实统计优势 | INSUFFICIENT | 采集 SequentialQualificationR27 logEUp ≥ log(1/α_i)；按 [FR-B05-04, FR-B11-03] 验证，禁止用编译通过代替 | e≥1/alpha，假设与预算成立 |
| Q04 跨块稳定 | INSUFFICIENT | 采集 事前冻结窗口规则下各块命中率；按 [FR-B11-03] 验证，禁止用编译通过代替 | 窗口规则事前冻结；不得事后选好块 |
| Q05 校准与BSS | INSUFFICIENT | 采集 CalibrationR30：等质量ECE、BSS、支持覆盖（与等宽ECE并列）；按 [FR-B06-06, FR-B11-03] 验证，禁止用编译通过代替 | ECE目标≤0.04；样本≥200且支持箱覆盖质量≥90%；不要求5个非空箱；BSS、区间、支持范围如实报告 |
| Q06 进化提升 | INSUFFICIENT | 采集 父子配对差单侧95%下界；按 [FR-B05-01, FR-B08-01, FR-B11-03] 验证，禁止用编译通过代替 | 有效区间下界>0才计验证改善 |
| Q07 有效资产产量 | RECORDED_UNVALIDATED | 采集 ACTIVE 证书资产数；按 [FR-B11-03] 验证，禁止用编译通过代替 | 目标≥1，继续寻找多个互补资产；0必须如实 |
| Q08 空信号与优势检出力 | UNKNOWN_STALE_SYNTHETIC_SOURCE | 采集 R30SequentialNullSimTest 空信号/功效 + NullEvolutionR27；按 [FR-B07-05, FR-B11-03] 验证，禁止用编译通过代替 | 报告误授证率及CI、34/37/40%信号功效，不以一次模拟签收 |
