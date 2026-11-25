# R34.1 第6版62工单与152源码对照

核验源码：`eb8f82a8bda8db22f1f122095217132f07053bcb`。这是只读源码/报告审计，不是应用修改或验收通过。

295文件卡对应294个不同实际路径且均存在；按卡统计127个与122相同、122个修改、46个新增。F238与F247因旧工具显式替代映射共用verify_integrated_chain.py；保留两张卡身份，只保留一个实际文件修改owner。不得按122整文件后态覆盖152。

|旧工单|状态|本次发现|剩余工作|
|---|---|---|---|
|R341-B01-01 先打通 R34.1 构建身份、旧方案封印与当前安装基线的兼容桥|STATIC_IMPLEMENTATION_RETAIN|R34.1构建分派、版本推进和嵌入身份已写入152源码，报告有VERIFIED_INSTALLATION；原方案仍把122当开工底座。|以eb8f82a8为本轮源码底座；保留旧身份/封印，先扩展R34.2明确series与新绑定，再递增实际最高versionCode；不能重新套用122后态。|
|R341-B01-02 固定版本、进程、数据库与观测水位，停止把跨版本累计当当前故障|PARTIAL_FIELD_EVIDENCE|新身份信封与进程/数据库标识已存在，但报告含运行观测、不可变SQL快照和后续独立生命周期读取，不能合并为同一时间窗口。|保留身份结构；逐结论强制绑定source/APK/process/database/role/时间水位，禁止累计旧日志计当前故障；补同版本测量原件。|
|R341-B01-03 建立不依赖 SQLite 的启动阶段首错收据与可复现分支树|STATIC_IMPLEMENTATION_FIELD_BLOCKED|数据库外StartupStageJournal已真实接服务和研究启动；研究仍记录WAIT_DEPENDENCY/迁移等待。|沿已记录首停点排查；不能只再加启动入口；保留有界文件journal并验证同输入重启恢复。|
|R341-B01-04 把线程活跃、真实提交、恢复效果拆成可检验的业务进度|FIELD_REQUIREMENT_NOT_MET|服务心跳不能解释最新开奖与结算水位差；当前报告仍显示赛前覆盖缺失和结算停滞。|以canonical/settlement实际commit receipt与最长未推进期数驱动恢复；恢复请求后必须见业务提交才签恢复有效。|
|R341-B01-05 建立逐目标全分母与可重放故障输入，为后续修复提供真实基线|PARTIAL_FIELD_EVIDENCE|runtime-opportunity-cohort已导出逐目标观察，但完整设备24/72h场景尚无回执。|保留全机会日历/原始输入信封，复演失败目标而非只看成功期；后续所有SLO统一完整目标分母。|
|R341-B02-01 将共享写者改成有界切片和可解释的截止期协议|FIELD_REQUIREMENT_NOT_MET|WriteBudget已提供行/字节/DDL准入与时长测量，现场仍出现WRITER_HOLD_FAULT_BACKLOG拒绝；接口存在不能证明所有写者有界。|对实际超时operation逐段测wait/begin/sql/commit/diagnostic，准备在锁外并分片提交；控制排水和实时提交不受研究故障反向阻塞。|
|R341-B02-02 让故障积压拥有独立可持续的持久化通道，不把研究永久封死|FIELD_REQUIREMENT_NOT_MET|故障积压成为研究数据与部分赛前捕获的共同拒绝原因；独立排水没有令现场依赖恢复。|审计requireReady允许通道、排水操作自身再入与持久化收据；保证控制通道持续推进且失败不递归生成同类积压。|
|R341-B02-03 在实时优先前提下验证研究、迁移和控制写者的最大饥饿时间|FIELD_REQUIREMENT_NOT_MET|数据集任务被WRITER_HOLD_FAULT_BACKLOG阻塞，进化任务继续等待数据集；调度有运行标记但业务未服务。|保留单写者，按就绪时间/截止期有界公平分配；明列真实冻结饱和与研究饥饿，验证控制、迁移、数据集在负载下的实际提交间隔。|
|R341-B02-04 切断控制循环与UI读对SQLite连接池的阻塞传播|PARTIAL_FIELD_EVIDENCE|RuntimeReadSnapshot已接入控制读；用户仍报告慢与卡顿，单份报告不足以归因某一UI读。|保留异步只读快照；用线程栈和实际队列/数据库连接耗时定位剩余阻塞，UI主线程不得执行重查询/模型计算。|
|R341-B03-01 按真实首停点修复研究启动链，保留可恢复迁移的单元提交|FIELD_REQUIREMENT_NOT_MET|启动阶段取证已存在，但仍未走到当前研究结构与会话完全就绪。|按journal首停阶段续迁移；检查点、schema单元和session注册分别持久化，保留已提交单元；不通过删除父代/证据来清障。|
|R341-B03-02 安装并核验R34缺失结构，把DDL存在变成当前数据库真实就绪|FIELD_REQUIREMENT_NOT_MET|当前快照缺r34_cold_rebirth、r34_atomnet_optimizer_step、r34_atomnet_unavailable_label；源码有对应DDL。|修迁移调度/ready定义，使DDL属于唯一schema owner且真正落入当前DB；用表/索引/版本核验真实就绪后再claim依赖任务。|
|R341-B03-03 让当前session的任务注册、claim与可执行依赖闭合|FIELD_REQUIREMENT_NOT_MET|任务注册、claim/execute框架存在；foundry与replay-dataset阻塞、evolution依赖链阻塞。|给当前epoch任务列出必需schema/input/owner fence；可执行就绪任务要实际获得切片，旧任务原因不得替代当前状态。|
|R341-B03-04 隔离缺失checkpoint与保护源故障，防止一个孤儿让全部研究停摆|FIELD_REQUIREMENT_NOT_MET|677检查点观察对象有556缺原件；整个研究前置被保护源/迁移等共享依赖牵连。|逐引用计算受影响owner；隔离缺原件谱系，允许无该依赖的新根/合法历史训练继续；禁止将缺原件改成空模型或伪恢复。|
|R341-B04-01 联合迁移LegacyPredraw结构所有权并移除冻结、结算热路径DDL|STATIC_IMPLEMENTATION_RETAIN|LegacyPredraw critical结构所有权和热路径改造已在152对应文件中，不能再按122旧说明盲删ensureSchema。|先核当前migration contract与freeze/settle实际热调用；只修仍在热路径的DDL并复验两处共享表，保留已完成迁移。|
|R341-B04-02 冻结一次PIT历史输入并把hash冲突拆成可定位的身份错误|PARTIAL_FIELD_EVIDENCE|AnalysisInputEnvelope已实现持久绑定并被分析owner读取；现场仍无足够当前同scope成功预测。|保留同请求不可变PIT输入；复演同键换hash、重试、重启、晚到历史等，解析每个失败目标第一条输入冲突。|
|R341-B04-03 以固定截止期调度single-flight，减少无效排队并保留被替代目标事实|FIELD_REQUIREMENT_NOT_MET|最近覆盖观察严重不足，过期/排队/缺冻结未闭合；single-flight代码存在但不能宣称实时已恢复。|给每个目标固定deadline和唯一终态，截止前提交；跳过已过期计算并保留其失败机会，队列退出必须续排当前目标。|
|R341-B04-04 保持canonical与核心结算原子闭合，延后证据不得反向改变赛前事实|FIELD_REQUIREMENT_NOT_MET|最新开奖346但结算/OOS仍331；影子每期有效冻结与结算未满足。|保持canonical与核心结算原子/幂等；保留晚到开奖对已冻结目标结算，事后不能补造赛前冻结；基础影子不能被无证书停掉。|
|R341-B04-05 用有界游标消化789项deferred积压，给每项真实完成状态|FIELD_REQUIREMENT_NOT_MET|v57_deferred_evidence_job共有1863，PENDING1573、RETRY_WAIT48、RUNNING11，非完成1632。|按owner/fence与小分页游标排水；区分可重试/缺件/终态，核心开奖结算先提交，deferred不得反向改变赛前事实。|
|R341-B04-06 保留并回归原响应身份、落库收据、重复乱序与历史连续性|STATIC_IMPLEMENTATION_RETAIN|RawHttpIdentity、FetchLedger原始响应身份、落库收据和连续性框架存在，是必须保留的成熟边界。|用重复、乱序、冲突响应、断期重放回归；保留原始响应与首次可见时间，历史补齐只能用于相应角色。|
|R341-B05-01 让服务独立闭合抓取、请求、计算、结算与续排，不依赖Activity补启动|FIELD_REQUIREMENT_NOT_MET|ServiceLoopTicket与独立服务循环已经写入，但当前业务推进/赛前覆盖与用户后台现象仍不满足要求。|不新增平行引擎；修服务循环首个丢票/阻塞点；锁屏关闭Activity后跟踪fetch→request→freeze→settle的同目标链。|
|R341-B05-02 把恢复验收改为后续业务提交，并防止无效重启风暴|FIELD_REQUIREMENT_NOT_MET|恢复入口存在，但无当前版本完整恢复后业务提交的有效证明。|恢复租约、退避、防风暴保留；以恢复前后的canonical/settlement水位差和延迟验收，不以onStartCommand计成功。|
|R341-B05-03 完成后台场景与网络、时钟、OS恢复的分层验收|DEVICE_UNVERIFIED|报告physicalDevice为NOT_RUN_OR_RECEIPT_UNAVAILABLE；后台能力摘要不能替代实际前后台/断网/重启场景。|保持OS权限/前台服务合规路径；执行同APK场景矩阵和全机会日历，单列强制停止等平台边界；设备验收不假装100%永驻。|
|R341-B05-04 验证界面只是观察者，页面打开、关闭与刷新不改变业务推进|FIELD_REQUIREMENT_NOT_MET|用户报告只有打开界面才计算；已有MainActivity只读快照改造不够证明页面已与业务完全解耦。|同输入在有/无Activity两组核对业务水位，移除依赖UI恢复的必要路径；允许UI发显式请求但服务必须自足。|
|R341-B06-01 保全旧 ABI 父代并完成可重入冷启出生|FIELD_REQUIREMENT_NOT_MET|旧ABI父代仍保全阻塞，当前冷启表缺失，不能形成可重入出生链。|保留父代原字节/证据；先完成表ready，再用当前PIT历史创独立新子代、数值状态和出生收据，重试不重复。|
|R341-B06-02 让工厂候选产生真实数值训练与可续算状态|FIELD_REQUIREMENT_NOT_MET|SymbolicFamily实训与codec已实现，报告却未形成正式工厂的开发训练/发布链；旧FTRL/R952少量训练不等于新工厂运行。|沿工厂首个候选把实际优化/状态、游标、checkpoint与出生原子关联；重启下一步等价；不可学习规则以回放验收。|
|R341-B06-03 形成可执行、可去重、可消融的公式与结构子代|FIELD_REQUIREMENT_NOT_MET|FormulaIdentity、BirthGate和结构/参数区分已有实质代码；r27_policy仍0。|保留规范化与真实AST去重；让生产工厂得到合法数据、出生并首评；结构改变需固定参数消融，行为差不自动当新机理。|
|R341-B06-04 区分实验专家与审计专家并恢复真实教师绑定|FIELD_REQUIREMENT_NOT_MET|实验专家与审计专家注册已有；专家历史提案不能证明新合法教师绑定与训练被当前版本消费。|每专家固定角色、原教师ABI与PIT输入；缺教师仅隔离该实验；以双模型数值学习与实际消费收据验收。|
|R341-B06-05 设置可持续多样性、资源预算与非破坏淘汰|FIELD_REQUIREMENT_NOT_MET|资源/非破坏淘汰实现已存在，现场仍未形成持续多机制出生和有用代际。|在原预算owner内保留无父本探索、失败编辑、互补融合的真实调度；有界活跃池、累计无终身数量上限、无用淘汰保留证据。|
|R341-B07-01 冻结因果样本并对齐十位置完整训练轮|FIELD_REQUIREMENT_NOT_MET|缺r34_atomnet_unavailable_label，十位置完整轮游标与可消费状态为未知；不能把RUNNING/steps56当十位置训练完成。|先修结构；按期/位置/版本冻结因果样本并为不可用标签落明确原因；完整轮才提升发布水位。|
|R341-B07-02 证明优化器真实更新并支持完整状态恢复|FIELD_REQUIREMENT_NOT_MET|缺r34_atomnet_optimizer_step，无法验证当前优化器数值变化；codec/训练类存在不等于设备实训成功。|同输入记录梯度范数、参数差、optimizer/RNG/游标完整状态；连续与恢复后下一步一致；失败不能增成功计数。|
|R341-B07-03 打通发布模型到同 hash 概率面和冻结消费|FIELD_REQUIREMENT_NOT_MET|r31_model_publication、item、part均0；无同hash发布模型被赛前概率面消费的证据。|修完整模型原子发布与容量前置；概率面/冻结行写所用hash、训练水位、ABI并校验，不以最新指针代替固定版本。|
|R341-B07-04 使结算反馈产生幂等学习和下一轮研究输入|FIELD_REQUIREMENT_NOT_MET|正式反馈输入/动作/产物表为0；R952有25次训练结算，不能借其分母给R27闭环。|每策略版本独立feedback outbox与幂等消费；结算→学习→下一轮研究要有具体关联键，防重投/重启重复更新。|
|R341-B07-05 隔离归档故障并建立训练有用进度判据|FIELD_REQUIREMENT_NOT_MET|归档任务受写者DEPENDENCY阻塞，训练可用性部分生命周期读未知。|把坏归档对象与学习任务预算隔离，保留原件；训练有用进度用样本/数值/发布/消费，归档失败不可全局锁研究。|
|R341-B08-01 建立可续建、不可变的研究数据集和输入 pin|FIELD_REQUIREMENT_NOT_MET|r27-replay-dataset阻塞于writer backlog，evolution由它连带阻塞。|增量不可变数据集分片、明确可用时间/PIT pin；同scope续建复用已提交片，输入修改只失效受影响对象。|
|R341-B08-02 逐图生产18种真实产物并保留只读图语义|FIELD_REQUIREMENT_NOT_MET|研究图表大量无有效当前产物；底层spec存在和旧累计测量不能当18图已工作。|按18对象合同逐个补真实输入→数值support→产物→消费者，缺源/等待/真正空集分别显示；只读图不强制生成策略。|
|R341-B08-03 为20矩阵配置逐对象输入、shape与实际用途|FIELD_REQUIREMENT_NOT_MET|矩阵注册数量/旧R952快照不能证明20个R30矩阵各自的生产消费。|按轴/shape/mask/support/role/input hash逐矩阵实算；变化必须追至typed action或只读解释，禁用输入验证实际贡献。|
|R341-B08-04 原子闭合提案到出生或非出生动作的消费|SOURCE_GAP_AND_FIELD_REQUIREMENT_NOT_MET|第6版新增规则归纳/发布/迁移消费合同未找到对应生产链；当前提案路径主要调用静态family并消费现有研究框架。|在原ResearchContext/IdeaFamily与产物owner新增有类型生成规则的归纳、存储、消费者、恢复和禁用对照；保留原子/分子既有能力。|
|R341-B08-05 使失败驱动变异和代际迭代可完成可复演|FIELD_REQUIREMENT_NOT_MET|结构变异/回放代码已存在但r27_generation0，无完整一代或下一轮实调度证据。|失败原件指到节点→合法结构修改→同预算回放/实训→选择/淘汰→下一轮；分列已运行代与独立证实改善，不能要求每代必涨。|
|R341-B08-06 验证同目标双亲融合与组件协同|FIELD_REQUIREMENT_NOT_MET|融合与有效父本的代码/测试有实现，现场没有正式谱系和配对实验完成证据。|共同目标比较A、B、A+B、等权、最强单亲及逐亲消融；零权重/不可达父本不得算双亲贡献；子代重新独立确认。|
|R341-B08-07 以真实动作成效更新下一轮探索预算|FIELD_REQUIREMENT_NOT_MET|实际研究动作成效/改进与下一代资源分配没有现场闭环。|每个动作记录唯一终态及实际收益/不确定性/成本，下一轮预算决定真实dispatch；保留最小探索和负结果。|
|R341-B09-01 冻结独立确认及新未来实验身份|FIELD_REQUIREMENT_NOT_MET|r30_frozen_confirm0、正式独立未来0；R952的STRICT_FUTURE不是R27资格实验。|每个候选固定程序/数据角色/确认与未来起点；新标签只进其预登记账，不向旧确认/多资产串借。|
|R341-B09-02 按全机会日历闭合事前冻结与权威结算|FIELD_REQUIREMENT_NOT_MET|r27_live_frozen和r27_live_shadow均0，无正式全机会资格日历闭环。|未来登记先于机会；每机会冻结/HOLD/缺失/结算完整计数，补历史不能伪造未来；资格推进与实时收据一致。|
|R341-B09-03 落实质量合同全门槛及统计不确定性|NATURAL_NOT_PROVEN|有效证书0、独立未来ACT0，自然胜率/稳定性/校准尚不能估计。|保留原V1门槛与序贯统计预算；训练/开发改进和自然资格分别签收，不降低门槛或用偶然高分保证高胜率。|
|R341-B09-04 将合法证书接入唯一实时决策并支持撤销|NATURAL_NOT_PROVEN|当前REAL0与证书0相符，关闭真实入口本身是正确保护；达标即应用的生产正例仍需验证。|保留唯一DecisionGate/SettlementEngine、K3/PIT、撤销；隔离工程正例贯通合法证书，真实自然达标再应用，基础影子持续。|
|R341-B09-05 用同提交全工厂模拟检验误报与功效|CURRENT_SOURCE_VALIDATION_PENDING|Q08原注册实验源不是当前源码，且样本不足，不能签152完整工厂误报/功效。|同提交完整搜索/选择/停止管线做多独立空信号与植入信号试验；保存每次种子、输入、结果、区间；合成不计自然。|
|R341-B10-01 给磁盘增长加真实预算，并保留已存在的保护性历史负担|FIELD_REQUIREMENT_NOT_MET|报告SQL快照3,176,525,824字节，存储预算状态与历史负担仍影响写入，尚不能证明长期总盘受控。|以当前实测总盘与受保护存量设增量预算；关键写入和排水保留额度，锁外测量，准入/实际字节对账；不清库降体积。|
|R341-B10-02 按可恢复性管理报告、缓存、审计尾部和旧备份|FIELD_VALIDATION_PENDING|报告、缓存、审计尾部治理代码已存在；当前诊断不能证明所有清理对象可恢复及增量稳定。|分类可重算缓存/原始证据/报告副本；删除前验证归档哈希和恢复路径，取消/导出/清理竞争不阻塞实时主链。|
|R341-B10-03 检查点原件恢复、缺失谱系隔离和GC闭包分别验收|FIELD_REQUIREMENT_NOT_MET|检查点677对象中556缺失、18字节验证未跑、574子集未知；不能称恢复或GC闭包完成。|逐对象恢复账和依赖隔离，合法可用原件单独验证；闭包未知不得物理删除，缺件不得填空原件。|
|R341-B10-04 修复5000条历史仍断档的长周期源和时间可用性|FIELD_REQUIREMENT_NOT_MET|保留5000行不自动代表连续历史或每个来源PIT时间可用。|按目标日历段检测断期和首次可见，授权来源补齐并记录延迟到达；旧未来实验不倒灌，历史开发可使用新完整段。|
|R341-B10-05 让物理压缩和恢复在有界维护窗口安全完成|FIELD_VALIDATION_PENDING|维护/压缩/恢复路径已写，现场还有维护写受拒线索；不能把压缩文件存在当恢复可用。|保留备份/临时文件原子交换及可恢复校验；维护有时限、可取消并不占实时写锁；存量不可按新小额度一刀拒绝。|
|R341-B11-01 源读取失败必须贯通到总览，未知值保持未知|STATIC_IMPLEMENTATION_RETAIN|sourceHealth已明确3不可用、13空、11可读，总览UNKNOWN_SOURCE_UNAVAILABLE，这比吞缺表为0正确。|保留并扩展源状态传播；每一图/指标/总览区分unknown/empty/failed，不把所有checks已执行解读业务健康。|
|R341-B11-02 把源码、安装、进程、历史窗口和主影子角色写进每项结论|PARTIAL_FIELD_EVIDENCE|安装身份可查，但44指标多数无同版本同角色测量，跨后续生命周期读取有独立水位。|统一每结论作用域与相同分母，分别输出旧历史/current process/natural，拒绝来源不匹配的质量或性能合并。|
|R341-B11-03 旧诊断归档，当前故障分级并指向可复核工单|PARTIAL_FIELD_EVIDENCE|165对象诊断能给方向与owner，但多项根因明确只是共享owner观察，未逐对象输入重放。|保留诊断对象表；每项挂本轮问题/文件/复现步骤，原因分已证明与假设，不能批量把同一writer原因当165种算法已定因。|
|R341-B11-04 前后台页面使用相同事实，三态展示保留细分原因|FIELD_VALIDATION_PENDING|页面源读三态实现存在，用户仍见无数据；source查询并未得到完整生产数据。|先修数据生产再修展示，前后台读同事实；按当前数据库/窗口/角色显示等待原因与最近消费，补截图与同源回查。|
|R341-B11-05 每张工单记录真实修复生效点和后续回查结果|SOURCE_REQUIREMENT_NOT_MET|RepairEffectLedger仅把2个操作映射到2项工单，PRIMARY KEY(install_id,issue_id)+INSERT OR IGNORE只存首次观察，无法62工单后续回查。|扩展版本化工单→生产证据注册；追加观测序列与独立断言，保留首生效投影；发布收据不可直接标结算反馈成功。|
|R341-B11-06 报告发布同时说明语义覆盖和原始字节覆盖|STATIC_IMPLEMENTATION_RETAIN|本次完整57成员ZIP已成功导出，清楚声明DIAGNOSTIC并排除原SQLite/APK/模型字节；不可再称本次导出失败。|保留单ZIP、校验、语义/原字节双覆盖；完善重复导出取消/清理边界；需要全重演时获取受授权原件，不能静默宣称已有。|
|R341-B12-01 复验R34.1构建身份与R33/R34封存方案继承|PARTIAL_FIELD_EVIDENCE|152安装源/APK摘要绑定可见；新方案R34.2需新身份桥，现有R34.1/旧封印验证须保留。|工程源码、嵌入身份、APK摘要、计划封印一致，篡改/错祖先反例拒绝；未取得APK不能独立回验安装字节。|
|R341-B12-02 从真实122安装验证同包同签名高版本覆盖|DEVICE_UNVERIFIED|152版本与签名摘要有报告值，原APK未附，不能重做真实122→152或152→新版本覆盖证明。|同包名com.starcore.research.r13/原签名，在当前已安装基线取证后覆盖；版本提升取实际max+1，保留数据库与成熟功能。|
|R341-B12-03 逐批工程签收和最终集成使用同一完整台账|INTEGRATION_ORIGINALS_PENDING|295文件卡对应294个不同实际路径均存在、136命名测试+10分派入口有路由，仅代表实现可见；不等于152全案工程或持续多根三代成功。|62工单/152入口逐项绑当前证据，特别F6新规则和同服务多根三代不能以旧单元用例代签；保留本轮待施工状态。|
|R341-B12-04 真机后台24/72小时连续性使用全机会分母|DEVICE_UNVERIFIED|当前physicalDevice未运行或回执不可用，无法签24/72h连续后台。|最终同APK24/72h完整日历；息屏/无UI/断网恢复，缺期和失败完整保留；层级与工程验收分开。|
|R341-B12-05 工程交付与高胜率稳定性自然资格分轨签收|NATURAL_NOT_PROVEN|自然质量NOT_PROVEN，没有独立合格资产；不能承诺代码完工即产生高胜率。|工程全链可交付，DEVICE/NATURAL独立待观测；自然逐资产依据预登记实验签收，达标后真实主链自动采用。|
|R341-B12-06 交付可下载APK、精确源码和完整证据包并实际回下载核验|DELIVERY_REVALIDATION_PENDING|本轮附件可读取为诊断ZIP，不包含应用源码总包或安装APK；方案更新不能当APK交付。|以后施工交付精确源码、可回下载APK、签名、逐批证据与hash；本次只交审计/方案并真实提交仓库，不冒称应用已修。|

152个原验收入口：136个有命名方法，10个B12通过`--case`分派，6个DEVICE/NATURAL为外部原件协议。所有入口保留；存在源文件/方法不代表已运行或通过。
