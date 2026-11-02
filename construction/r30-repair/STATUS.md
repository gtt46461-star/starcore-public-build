# R30 返修003实施与验收记录

已发布 `577311003 / R30-REPAIR1-2026.11.01-003`，原R27证书。[下载APK](https://github.com/gtt46461-star/starcore-public-build/releases/download/r30-repair1-577311003-63ad6291711d/STARCORE-R30-REPAIR1-EXISTING_R27-63ad6291711d.apk) · [发布页](https://github.com/gtt46461-star/starcore-public-build/releases/tag/r30-repair1-577311003-63ad6291711d)。

应用/测试同源 `63ad6291711db607fd604443b4f50492882f09e8`；全CI run41365268452。实际HOST单元测试347项、XML96份，失败/错误/跳过均为0；原全部门禁及13组repair native通过，原51项lint基线保留、无新增。

Android原17项及工作台1项、26故障和三代升级均为模拟器证据。47图四场景188个目标场景真实采集，并检查存在图形的原生绘制成功；已实际查看R03/R12及记录的其他抽样，共9个列明目标：READY-R03.png、READY-R12.png、READY-R02.png、READY-R01.png、EMPTY-R04.png、INSUFFICIENT-R13.png、READ_ERROR-R08.png、READY-28.png、READY-01.png。人工范围以该记录为准，旧图缺ABI的读取失败和研究合法不足状态保留。R03实际100节点成功绘制，R12已有真实pair按1×N热图绘制。研究12故障格为9恢复和3预期阻断。三代基线各3步，共9步新APK覆盖→回退→再升级，原draw/model/预算只按各自实际回执记录。

002原重复截图假绿和正确定位后R03绘图失败假绿均保留人工否决历史；002最终推荐已撤回，003使用新产品源码与新APK实证。

60卡、12批次逐项记录实现、保留Owner适配、HOST、Android直接/交叉范围与缺证；不按提交、测试或截图数推完工百分比。真机V2353A72小时、原约26GB库性能/空间、自然Q01-Q07和主链效果均未证明，额外专门Android场景缺证仍保留。

Q08仍为历史commit `1342b77b0c3ffeb73e68d892fee93987dc4a4129`上的20个完整独立合成试验，源哈希快照含355个文件；0假资格、Wilson95上限0.161125158。003增加显示预算、draw观察和R12已有pair的轴说明；不是最终整源重新20轮，也不扩大为40独立样本。

详见[逐卡证据](ALL60_EVIDENCE.json)、[最终范围](FINAL.json)、[交付身份](DELIVERY.json)。

| 卡 | 实现或Owner适配范围 |
| --- | --- |
| FR-B00-01 固定运行源码、方案和签名身份 | 基线、source tree、方案hash、既有包名/版本与公开R27证书身份已登记。 |
| FR-B00-02 逐文件动作与跨文件所有权登记 | 60卡文件动作、原方法锚点与同文件串行集成Owner登记存在；SUBCARDS保留追加及补登记事实。 |
| FR-B00-03 保全历史训练和已有效功能 | 原writer、唯一REAL/结算、PIT、模型/优化器/RNG、旧29图及覆盖安装保护规则已登记。 |
| FR-B00-04 冻结研究角色、单位和资格合同 | TRAIN/SELECT/ONLINE_RESEARCH/CONFIRM/FUTURE/SYNTHETIC、日历期聚类、原V1权威与V2旁路合同已冻结。 |
| FR-B01-01 修复诊断空探针空指针 | 空有效增益探针按缺数据处理，保留NA，修复基线可复现NPE且不改统计公式。 |
| FR-B01-02 导出诊断失败阶段和错误链 | 读取/解析/诊断/MD/JSON分阶段失败、输入hash及有界产品栈链已接入只读导出。 |
| FR-B01-03 修正诊断指标映射 | writerHard指向M07，M11保持训练积压口径；诊断动作优先指向当前FR返修卡。 |
| FR-B01-04 统一44指标与12章事实对象 | 不可变R30Input一次捕获当前44指标，12章和图共用事实；缺失、失败、负结果与分母分开。 |
| FR-B01-05 补齐逐图、逐矩阵、逐能力和前次报告对比 | 47图、20矩阵、82能力完整索引和六状态存在；成功发布摘要按jobId/版本/角色比较。 |
| FR-B02-01 防止写锁遥测旋转丢数与失败丢窗口 | record/rotate短内存互斥、单飞pending窗口和成功后清除已实现；开口桶以区间/空数值表达。 |
| FR-B02-02 定位并优化FTRL事务内慢点 | FTRL事务分段定长计时与事务外只读EXPLAIN已实现，核心artifact/学习回执原子性保留。 |
| FR-B02-03 移出旧结算解释与保留重计算 | 旧解释锁外准备、短事务复核；R26可恢复维护阶段和原充分统计折叠已接入原Owner。 |
| FR-B02-04 实时事件身份和时间线校正 | LIVE/BACKFILL/REPLAY与boot/process/单调时钟身份分开，保留原表并以原cache命名空间保存新事件。 |
| FR-B02-05 关键落库、结算和下一期冻结的幂等贯通 | 原协调器修复真正COMMIT失败仍报告成功，锁释放后再发遥测；原durable请求/结算Owner保持。 |
| FR-B02-06 跨文件批次原子性与组合守卫 | 原Batch组合前后守卫和提交信封接入，source/cancel/CAS失败整批回滚。 |
| FR-B03-01 唯一schemaOwner增加研究物化表 | SchemaR27唯一writer增加六个R1物化表；恢复13个原R930声明表的实际增量另有披露。 |
| FR-B03-02 57任务分清工作单位与日历新鲜度 | 工作phase、日历freshness、artifact提交与typed pending分开；未知源不记健康。 |
| FR-B03-03 任务提交与真实进度原子记录 | 产物和typed progress同事务，失败不发布内存完成；旧调度游标为兼容保留。 |
| FR-B03-04 有界重试和状态发布协议 | 有界PreparedTask重试、提交后状态发布与旧守卫兼容已实现。 |
| FR-B03-05 公平持续运行与息屏任务恢复 | 原池/原R26调度使用typed进度有限boost，保留任务lane、后台优先级和恢复游标。 |
| FR-B04-01 神经预处理、梯度、发布水位分列 | 预处理、梯度、epoch、发布水位分列；十位置样本和日历期数分开。 |
| FR-B04-02 单批64样本预取与输入完整性 | 64样本单cursor预取保留原更新顺序，形状/缺行/NaN/错position拒绝。 |
| FR-B04-03 完整优化器续训及标签隔离 | 完整Adam/FTRL/RNG/replay/位置状态外层V3续训，原V1/V2字节归档和正式标签隔离保留。 |
| FR-B04-04 真实10×10概率面准备和发布 | 真实网络十位置面只在成功训练提交后准备/发布，source/model/ABI/target引用不可变。 |
| FR-B04-05 I08与策略冻结真实消费闭环 | I08多模型叶子、冻结和完整消费census绑定原模型hash/位置/来源，缺失明确BLOCKED。 |
| FR-B05-01 确认负结果与不足结果准确展示 | 确认负结果、不足、待slot和注册状态独立展示；未verified不强改成功。 |
| FR-B05-02 确认原件、曝光预算与唯一注册 | 原件、预留预算、结果、注册及冻结轨迹恢复使用原权威，重启不重新读取正式标签。 |
| FR-B05-03 资格缺期4实验逐项恢复或封段 | 逐实验64行归档扫描、原件恢复/封段/未知/冲突分列；原alpha/globalIndex/e-process不变。 |
| FR-B05-04 对齐报告旁路门禁与实际签证合同 | 原R27_GATE_V1继续签证，GateV2只读would_issue；正式分母仅连续已消费前缀。 |
| FR-B05-05 唯一REAL、撤销与真实结算隔离 | 唯一DecisionGateEngine/SettlementEngine源码保留；原合法授权、过期/错源/撤销拒绝与解释旁路复验。 |
| FR-B06-01 矩阵静态接线与运行效果分列 | 静态binding与implemented/callable/input/produced/consumed/effective分开，未知Boolean不拆箱/补0。 |
| FR-B06-02 恢复矩阵唯一schema与真实生产入口 | 原Schema owner恢复表并按kind真实维度/role/source/hash生产张量，缺依赖保留BLOCKED。 |
| FR-B06-03 矩阵真实消费与反事实效果验证 | 实际矩阵消费引用、原SU2提交后availability和graded cursor CAS、配对反事实张量接入。 |
| FR-B06-04 原子/分子目录到可执行贡献闭环 | 原子/分子配对移除证据和真实精确子代输入、Brier/消费回执接入原出生门。 |
| FR-B06-05 82能力与原教师F23恢复 | 保留82静态目录，运行事实在Store/报告层独立；F23原teacher资产/state/ABI绑定原子发布。 |
| FR-B06-06 校准、概率目标和支持域 | 仅H_SELECT REPLAY拟合、FUTURE只评价；ACT支持域、同窗raw/calibrated proper score和日历块分开。 |
| FR-B07-01 机器可读研究证据和配对贡献核心 | 按日历期聚类的配对gain、缺块/尾块披露、描述性区间和合同身份形成受约束处方。 |
| FR-B07-02 分析、处方、消费和图缓存同事务 | 分析/measure/proposal/frame同事务，真实BORN子代/consume/cursor同提交；错误源/child全拒绝。 |
| FR-B07-03 16逻辑研究表适配现有来源 | 16逻辑适配器复用现有来源与六R1表，原retention保护集合接入run/measure/proposal/consume。 |
| FR-B07-04 研究任务分片、进度和恢复 | 原池一次256样本双臂runner、bias/loss/cursor/source pin持久化并真实HOST JVM重启对齐。 |
| FR-B07-05 反证控制与失败机制记忆 | 完整Q08机制协议固定20独立重复，候选行为去重和失败记忆保持合成/正式隔离。 |
| FR-B08-01 原子剔除研究→精确子代的完整示范链 | 生产双臂runner→负组件gain→精确child→原BirthGate→consume/cursor链已有HOST合成机制。 |
| FR-B08-02 短期结构、跨位与集合交集研究 | 固定8/32/128/512多尺度结构、因果预序proper score与真实硬集合交集接入；不足3明确弃权。 |
| FR-B08-03 专家、家族、变异和融合的贡献反馈 | 专家/家族/变异/同目标融合使用实际配对贡献，同行为兄弟去重、谱系和消费原子保存。 |
| FR-B08-04 校准、模型消费与漂移研究 | 预登记ONLINE_RESEARCH同输入冻结旧新概率，输入JS与proper损失退化分开；回放分配规则有显式门槛。 |
| FR-B08-05 预算、失败记忆与端到端信用 | 预算/失败记忆研究与实际measure消费引用接入原池，端到端图按原授权键披露缺口。 |
| FR-B09-01 登记18个研究图的算法合同 | 18唯一R01-R18及3×6组的measure/consumer/verification合同存在。 |
| FR-B09-02 有界图帧、张量切片与单快照读取 | 003工作台有界帧/实际100节点图与R12已有pair1×N轴已接入；完整机读值和旧64/研究120合同分开。 |
| FR-B09-03 手机研究工作台与生命周期 | 003修复真实R03的64/100节点冲突，原64与研究120分开；实际drawCount/noFailure、生命周期与9幅人工抽样有回执。 |
| FR-B09-04 旧29图语义与真实资金纠偏 | 003保留旧29图语义/原Owner；实际采集及有图时native draw断言通过，人工抽样包含旧READY-01/28。 |
| FR-B09-05 47图与报告、研究动作的联合验收 | 003全47图四场景188目标真实采集，有图原生绘制成功检查；父实际人工查看9个列明目标，合法不足/读取失败状态保留。 |
| FR-B10-01 真实空间归因与预算 | 物理普查保留旧分类并细分主库/备份/快照/归档/模型、Owner/时间/截断；root alias漏计已复现修正。 |
| FR-B10-02 5000热期与引用安全归档 | 原retention机制和CP12归档Owner保留；新增研究保护集合与分页维护接入，不删无复验原件。 |
| FR-B10-03 导出快照生命周期、取消与重建 | 原导出取消/lease Owner保留；报告单快照总字节预算、完整行省略索引与成功摘要复建接入。 |
| FR-B10-04 覆盖安装、备份和迁移并发控制 | 原DbHelper迁移/唯一writer保留，R1 schema按Owner增量；原B11分库触发三条件显式未知，未启用。 |
| FR-B10-05 存储与并发故障矩阵 | 原14格保持；新增analysis/consume×BUSY/FULL/CANCEL/KILL/CORRUPT/SOURCE_CHANGE共12Android格及发布校验。 |
| FR-B11-01 全源码编译、既有回归与47图Android验证 | 003应用/测试同source63，主CI6job成功；实际347 HOST单测/96XML、13native与原全部gate、17+1模拟器回归/26故障/三代9步升级通过。 |
| FR-B11-02 V2353A前台、息屏与72小时实测 | V2353A设备运行记录模板与缺证原因存在，旧健康Owner保留并由typed进度/时间线报告补足事实。 |
| FR-B11-03 自然高质量资产与主链效果独立验收 | Q01-Q08与主链效果独立记录；Q01-Q07缺自然数据保持NOT_PROVEN/null，Q08完整20次合成机制有回执。 |
| FR-B11-04 最终源码、APK、方案与证据交付 | 003公开APK已发布，实际HTTP200/字节/hash/包版本/原R27证书与同源CI独立校验；新图9幅人工样本及全188自动采集/绘图边界有据。 |
