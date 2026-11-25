# 44项指标逐条覆盖表

基线：577311152 / eb8f82a8bda8db22f1f122095217132f07053bcb。原状态完全保留；施工归属使用原问题ID，不使用待拓扑重排的新编号。

逐项覆盖关系完成，不等于应用修复、设备验收或自然质量通过。

## 逐项处置

### M01 · 源码与安装身份

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** PLAN-SOURCE-04, RT21, RT23

**具体覆盖：** 安装manifest已验证，但指标缺同角色签名测量receipt；不能说已装错版本，也不能称M01已通过。

**验收方向：** 打通source/APK/签名/plan六身份与receipt；独立重算安装及构建hash，新版本由实际构建链生成。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M01]`；../evidence/METRICS_44_PROJECTED.json#M01

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：全部一致

### M02 · 功能保全覆盖

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** PLAN-SOURCE-01, RT21

**具体覆盖：** 1832要求/299卡原账有状态，但当前1770/296未检查、62/3待验证，逐行传承不等于功能回归通过。

**验收方向：** 保存所有原行与52/19列语义、原验收及hash；新增路径关联/实际回归receipt，未运行不得置VERIFIED。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M02]`；../evidence/METRICS_44_PROJECTED.json#M02

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：100%有记录；无凭空VERIFIED

### M03 · 准时预测持久化

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RT05, RT09

**具体覆盖：** 1440期request→durable指标缺测；当前单样本2804ms仅局部事实，不填入整体通过。

**验收方向：** 完整时间窗记录成功/缺失/失败、request→durable P95≤5000ms，trace绑定同target/epoch。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M03]`；../evidence/METRICS_44_PROJECTED.json#M03

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：P95≤5000ms

### M04 · 抓到后正式结算

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RT06, RT09

**具体覆盖：** observe_to_validate有8样本P95=88ms，而observe_to_settled为0有效/8缺测；不能用校验段代结算段。

**验收方向：** first-valid-observed→draw+settlement commit同boot单调receipt，P95≤1000ms，缺源/无冻结单列。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M04]`；../evidence/METRICS_44_PROJECTED.json#M04

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：P95≤1000ms

### M05 · 赛前有效覆盖

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RT05, RT06

**具体覆盖：** 要求截止前5秒合法冻结/全部应有期；当前deadline样本0且近30仅1覆盖。

**验收方向：** 完整机会分母保留断网/停机，计算deadline−freeze≥5000ms；健康支持场景≥99.5%目标及每个缺口可解释。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M05]`；../evidence/METRICS_44_PROJECTED.json#M05

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：目标100%；受支持健康场景≥99.5%

### M06 · 唯一正式结算

**原status：** `RECORDED_UNVALIDATED`；**处置：** 调查补证；**关联：** RT21

**具体覆盖：** value0/sample0仅空REAL逐期唯一性，无正式结算联合收据，未证幂等。

**验收方向：** 竞争同target、重复draw、重启与提交不确定注入；非法补冻结/REAL重复/结算重复均0且有非空输入。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M06]`；../evidence/METRICS_44_PROJECTED.json#M06

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

原评估器状态：`INSUFFICIENT_ZERO_SAMPLES`；值=0；样本=0。目标：均为0

### M07 · 实时写事务

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RT03, RT04, RT09

**具体覆盖：** 实时写事务P95/24h busy失败证据缺失，现场确有写背压和最长等待历史观测。

**验收方向：** 主PID同scope分开wait/hold/SQL/commit/返回；P95≤200ms与24h失败0按定义实测，超限保留原件。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M07]`；../evidence/METRICS_44_PROJECTED.json#M07

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：P95≤200ms；24h失败0

### M08 · 分库隔离

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RT03, PLAN-SOURCE-01

**具体覆盖：** 当前可为单库模式，缺同表双写/跨库持锁观测；不能为指标名称强制分库。

**验收方向：** 沿现owner验证研究写实时表/双owner0；若确启分库则outbox/inbox和无跨库持锁等待，证据注明拓扑。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M08]`；../evidence/METRICS_44_PROJECTED.json#M08

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：0

### M09 · 迁移一致性

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RT01, RT14, RT16, PLAN-SOURCE-01

**具体覆盖：** 迁移rowhash/watermark/中断恢复没有完整receipt，实际startup preserve与新表就绪存在阻断。

**验收方向：** 实际旧数据副本升级，主键/全行hash/事件水位及中断重入一致，无丢失/重复；原件缺失按资产隔离。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M09]`；../evidence/METRICS_44_PROJECTED.json#M09

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：全部一致；丢失/重复0

### M10 · 每期训练投递

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RES08, RES11, RT21

**具体覆盖：** 结果事件与训练inbox匹配缺测，历史25次POST_TRAIN不证明每期100%投递。

**验收方向：** 非空自然/注入序列逐event对账outbox→inbox→数值提交，重复消费0；消费者提交后才推进游标。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M10]`；../evidence/METRICS_44_PROJECTED.json#M10

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：100%；重复消费0

### M11 · 训练完成新鲜度

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RES08, RES11, RES29

**具体覆盖：** TaskLiveness中27训练任务源未知、总55 UNKNOWN，oldestBacklogIssues null不能用observed0替代。

**验收方向：** 按有ready输入任务计算真实期号积压，正常最老≤5期；unknown、依赖等待、无新输入分开。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M11]`；../evidence/METRICS_44_PROJECTED.json#M11

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：正常负载最老积压≤5期；超限必须FAIL

### M12 · 任务活性

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RT01, RT11, RES08, RES29

**具体覆盖：** 63任务摘要有stalled1/faulted2/unknown55，attempt或维护work_units不是连续5期提交。

**验收方向：** 有ready积压的任务每5期内真实产物/游标提交，否则FAIL；原owner恢复及依赖冷却可观察。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M12]`；../evidence/METRICS_44_PROJECTED.json#M12

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：0；等依赖/等数据单列

### M13 · 前后台连续性

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RT12, RT11

**具体覆盖：** 前台60min/息屏6h/72h设备窗口尚无receipt，权限YES不能证明持续性。

**验收方向：** 真实设备按场景记录全部投递和完成/缺口、重启代次；受支持场景不丢已投递任务且无重复owner。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M13]`；../evidence/METRICS_44_PROJECTED.json#M13

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：受支持场景不丢已投递任务；缺口全记录

### M14 · 进程稳定性

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RT12, RT13

**具体覆盖：** 无当前测试窗OOM/ANR/crash联合证据，历史crash2与旧stall不能算本版本复现。

**验收方向：** 同APK/source/boot日志与ApplicationExitInfo相互核对，完整测试窗三类计数0；系统杀/更新/用户stop单列。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M14]`；../evidence/METRICS_44_PROJECTED.json#M14

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：0

### M15 · 热库与总磁盘

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RT15, RT16, RT17, RT18

**具体覆盖：** draws5000已见，但storage指标receipt缺失；独立census已有10.97GB和保护原件信息。

**验收方向：** 接入现有census与配置预算，draws≤5000、owner预算和可恢复性完整；释放量以真实bytes/receipt证明。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M15]`；../evidence/METRICS_44_PROJECTED.json#M15

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：draws≤5000；未超配置总预算且可恢复

### M16 · 报告取消

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RT21, RT15

**具体覆盖：** 报告取消≤2000ms无测量，不能从一次成功导出或按钮存在推断。

**验收方向：** 在读取/压缩/写卷/交付阶段注入取消，测请求→owner停止/本job临时文件释放；其他作业pin和原件保留。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M16]`；../evidence/METRICS_44_PROJECTED.json#M16

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：≤2000ms

### M17 · 报告完整性

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RT20, RT23

**具体覆盖：** 本次57成员封包可核hash，但metric仍缺同角色完整性receipt；DIAGNOSTIC排除raw属声明范围。

**验收方向：** registered/expected/exported/omitted逐项对账，默认≤20MiB或明确分卷，损坏/截断拒绝，RAW与诊断分别签收。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M17]`；../evidence/METRICS_44_PROJECTED.json#M17

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：全量索引100%；默认ZIP≤20MiB或明确分卷

### M18 · PIT泄漏反证

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RT21, RES23

**具体覆盖：** pitViolations0与PIT拒绝记录不能代替未来标签扰动反证。

**验收方向：** 固定事前输入，改当前/未来标签不改变已冻结输出；真泄漏反例被硬挡，测试注入与生产源分列。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M18]`；../evidence/METRICS_44_PROJECTED.json#M18

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：0

### M19 · 训练分割污染

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RES23, RES24, RT21

**具体覆盖：** CONFIRM未污染缺Holdout真实账，0确认reservations不是隔离生效证明。

**验收方向：** 生成/调参/原子权重不得读取封存确认标签；非法复用被记账拒绝，重启后仍遵守角色与预算。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M19]`；../evidence/METRICS_44_PROJECTED.json#M19

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：0

### M20 · 全状态续训恢复

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RT01, RT16, RES10, RES11

**具体覆盖：** 有旧AtomNet checkpoint元数据但缺全状态重启续训对照，缺原件与startup保存亦未闭环。

**验收方向：** 同权重/优化器/RNG/cursor/输入hash恢复，按预登记容差与不中断训练等价且不重学；缺源不得伪恢复。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M20]`；../evidence/METRICS_44_PROJECTED.json#M20

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：按已登记浮点容差一致；不重复学习

### M21 · 策略完整合同

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RES05, RES06, RES07, PLAN-SOURCE-02

**具体覆盖：** policyObjects0/birthRejects空，不能证明出生策略必要字段和可执行图完整。

**验收方向：** 合法结构真正出生并可执行/恢复，缺字段/空壳/非法图被BirthGate拒绝；产物具输入输出/状态/hash。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M21]`；../evidence/METRICS_44_PROJECTED.json#M21

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：100%；空壳拒绝

### M22 · 结构与行为去重

**原status：** `RECORDED_UNVALIDATED`；**处置：** 调查补证；**关联：** RES05, RES21, PLAN-SOURCE-02

**具体覆盖：** 结构/行为/参数去重value0/sample0为未验证空账。

**验收方向：** 同构/交换律/重命名/仅参数变体反例不计新机制；结构与行为hash可重算，真实新结构计数与谱系一致。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M22]`；../evidence/METRICS_44_PROJECTED.json#M22

原评估器状态：`INSUFFICIENT_ZERO_SAMPLES`；值=0；样本=0。目标：计数与ledger一致

### M23 · 族专家覆盖

**原status：** `BLOCKED`；**处置：** 修复；**关联：** RES28, RES04, RES29, RT21

**具体覆盖：** 81/82静态可调用唯一缺F23教师绑定；代码绑定不代表输入可用、执行产物、消费或效果。

**验收方向：** 局部修F23真实teacher owner绑定；82对象逐项producer/consumer/ready/status，非空生产执行与产物验收分列。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M23]`；../evidence/METRICS_44_PROJECTED.json#M23

原评估器状态：`MEASURED_REQUIRES_VALIDATION`；值=81；样本=82。目标：全部注册与可执行；没有证据不标有效

### M24 · 模型实际消费

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RES10, RES26, RT21

**具体覆盖：** expectedCalls0/matched0/ratio null，没有模型调用与冻结行匹配证据。

**验收方向：** I08按实际AST位置消费合法trainedThrough模型，调用与冻结model/artifact identity100%匹配或明确BLOCKED，不伪造call。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M24]`；../evidence/METRICS_44_PROJECTED.json#M24

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：100%匹配或明确BLOCKED；不得伪造调用

### M25 · 矩阵接线

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RES15, RES16, RES17

**具体覆盖：** 20矩阵原生与SU2消费行0，另25个R952快照不能抵这20矩阵。

**验收方向：** 逐matrix producer→typed schema/watermark→consumer→结果receipt；17过期输入修保留/服务时限，未产出给原因。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M25]`；../evidence/METRICS_44_PROJECTED.json#M25

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：全部可追溯；未产出有明确原因

### M26 · 校准时间隔离

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RES25

**具体覆盖：** 校准列表空，无法验证fitEnd<validateStart及apply版本可见。

**验收方向：** 冻结校准训练区间/版本和验证区间，强制时间隔离/选择条件一致；零模型明确缺输入或阻断，不默认通过。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M26]`；../evidence/METRICS_44_PROJECTED.json#M26

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：100%；零模型需可定位原因

### M27 · 资格预算连续

**原status：** `RECORDED_UNVALIDATED`；**处置：** 调查补证；**关联：** RES23, RES24, RES30

**具体覆盖：** 资格冲突0/sample0并未证明永久index、不可回填、重启预算连续。

**验收方向：** 跨重启/重复投递验证假设索引唯一、alpha支出不重置、旧observations不回填新窗，完整原件可复算。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M27]`；../evidence/METRICS_44_PROJECTED.json#M27

原评估器状态：`INSUFFICIENT_ZERO_SAMPLES`；值=0；样本=0。目标：全部通过

### M28 · 主链闭环

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RES26, RT21, ROOT-01

**具体覆盖：** decisions空/unsafeMismatch0，不足以证明certificate→projection→intent→REAL→settlement贯通。

**验收方向：** 同生产入口合法正例真实用证书与模型并结算；过期/错bundle/错域/未确认selector全拒绝，SHADOW不转REAL。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M28]`；../evidence/METRICS_44_PROJECTED.json#M28

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：合法正对照贯通；非法/过期全部拦截

### M29 · 确认队列连续

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RES23, RES24, RES29

**具体覆盖：** confirmation队列无原件，不能证明锁定候选不会因榜单变化被挤出。

**验收方向：** 锁定后固定协议与样本预算，榜单重排/新候选/重启不驱逐已确认义务；每次取消必须有合法原因和收据。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M29]`；../evidence/METRICS_44_PROJECTED.json#M29

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：0

### M30 · 主链选择器身份

**原status：** `INSUFFICIENT`；**处置：** 修复；**关联：** RES26, RES24

**具体覆盖：** selector决策为空，未确认meta-policy禁止REAL尚缺生产正负对照。

**验收方向：** 多成员择优整体identity/版本/冻结评价绑定；selector未确认不REAL，已确认新模型合法选择可追溯。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M30]`；../evidence/METRICS_44_PROJECTED.json#M30

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：未确认组合不能REAL

### M31 · 诊断可追溯

**原status：** `RECORDED_UNVALIDATED`；**处置：** 调查补证；**关联：** ROOT-02, RES20, RT20, PLAN-SOURCE-03

**具体覆盖：** 16/16行动字段覆盖值1.0仅结构，不能代表诊断正确或全缺陷已修；现修复生效账覆盖不足。

**验收方向：** 每缺陷证据强度/源码/卡号/动作/回读/效果齐全；原状态保留，多次修复和失败都可追，不把首次登记当关闭。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M31]`；../evidence/METRICS_44_PROJECTED.json#M31

原评估器状态：`MEASURED_REQUIRES_VALIDATION`；值=1.0；样本=16。目标：100%或UNKNOWN并给采集动作

### M32 · 报告口径一致

**原status：** `UNKNOWN_SOURCE_MISSING`；**处置：** 修复；**关联：** RT04, RT22, RT20

**具体覆盖：** 无同角色同水位独立源，原UNKNOWN_SOURCE_MISSING；同对象重复渲染不能填一致差0。

**验收方向：** 用同metric/scope独立原账重算与JSON/HTML/UI比对；诊断PID零队列及7/30对1/30必须显式不可比或校正合同。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M32]`；../evidence/METRICS_44_PROJECTED.json#M32

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=0。目标：0

### M33 · 图表回归

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RT10, RES18

**具体覆盖：** 图表入口/排行榜/谱系/BSS/空态无设备截图回归；18研究视图不能用模拟数据充实。

**验收方向：** 保留成熟图表与筛选入口，真实产物/无样本/缺源/失败场景均可打开、口径一致并达交互性能要求。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M33]`；../evidence/METRICS_44_PROJECTED.json#M33

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：全部场景可打开且口径一致

### M34 · 签名覆盖安装

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** PLAN-SOURCE-04, RT01, RT12

**具体覆盖：** 当前安装已识别不等于新版本覆盖安装完成；缺首启/二启及保库receipt。

**验收方向：** 同包同签名版本递增构建，真实覆盖安装后首启/二启无崩溃、数据库/原件保留、migration续跑可验。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M34]`；../evidence/METRICS_44_PROJECTED.json#M34

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：成功；首启/二启无崩溃

### M35 · 失效模型撤销

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RES26, ROOT-01

**具体覆盖：** revocationEvents0没有“撤销→不再采纳”时延样本。

**验收方向：** 证书过期/撤销/ABI不兼容的事件发布到主链缓存/版本指针失效≤5000ms，下一合法调用必须拒绝旧模型；非APK回滚。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M35]`；../evidence/METRICS_44_PROJECTED.json#M35

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：目标≤5000ms；不含APK回滚

### M36 · 同池对照

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RES30, RES24

**具体覆盖：** 原同池合成对照与全创造工厂功效验收分离，当前各arm完成0，不能用模拟单次成功证明优势。

**验收方向：** 固定预算的合法正/负对照经同搜索/停止/选择管线，多轮收据与区间可重算；明确SYNTHETIC不计自然质量。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=M36]`；../evidence/METRICS_44_PROJECTED.json#M36

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：有可复算输出；合成不计真实质量

### Q01 · 自然ACT命中

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RES23, RES24, RES30

**具体覆盖：** 自然注册后ACT质量未证；累计1858 shadow不替新模型事前冻结的FUTURE ACT。

**验收方向：** 对应R27域至少250已结算ACT及完整现门合同，命中率目标≥34%/优先37%只按自然样本声明；其他域不强套阈值。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=Q01]`；../evidence/METRICS_44_PROJECTED.json#Q01

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：≥34%，优先≥37%；未达不得宣称达标

### Q02 · 真实频率

**原status：** `NOT_DEPLOYED`；**处置：** 调查补证；**关联：** RES26, RES30, RT06

**具体覆盖：** NOT_DEPLOYED因0REAL/0ACTIVE证书，部署前FUTURE_ACT_RATE与部署后REAL频率不同。

**验收方向：** 资格先成立，部署后全部应有期作分母，目标≥15%不补配额；未部署如实保留状态。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=Q02]`；../evidence/METRICS_44_PROJECTED.json#Q02

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：目标≥15%；质量支持扩大；不得补配额

### Q03 · 真实统计优势

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RES23, RES24, RES30

**具体覆盖：** 没有当前事前登记序贯资格与证书证据，普通Wilson或一次高命中不能代替e过程。

**验收方向：** 固定p0/alpha_i与可选停止协议，e≥1/alpha或logE门由同角色原件重算，假设预算跨重启不清零。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=Q03]`；../evidence/METRICS_44_PROJECTED.json#Q03

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：e≥1/alpha，假设与预算成立

### Q04 · 跨块稳定

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RES24, RES25, RES30

**具体覆盖：** 无预登记同策略日历块质量；事后从展示子集选好块会污染稳定性证明。

**验收方向：** 先冻结块规则，再逐块报告命中/区间/回撤/漂移及最差块；缺块和停机全记，不挑窗口。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=Q04]`；../evidence/METRICS_44_PROJECTED.json#Q04

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：窗口规则事前冻结；不得事后选好块

### Q05 · 校准与BSS

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RES25, RES23

**具体覆盖：** 没有固定校准器同目标同ACT条件验证；合法校准样本0。

**验收方向：** 至少200样本、支持箱覆盖质量≥90%，全样本加权ECE目标≤0.04；并报BSS/区间/支持范围，不硬凑5非空箱。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=Q05]`；../evidence/METRICS_44_PROJECTED.json#Q05

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：ECE目标≤0.04；样本≥200且支持箱覆盖质量≥90%；不要求5个非空箱；BSS、区间、支持范围如实报告

### Q06 · 进化提升

**原status：** `INSUFFICIENT`；**处置：** 调查补证；**关联：** RES21, RES22, RES24, RES30

**具体覆盖：** 父子同期配对/确认/演化原件为空，代数/出生数不是代际提升。

**验收方向：** 事前冻结父子版本与同target机会，配对收益差有效单侧区间下界>0才计改善；失败保留，不承诺每个子代更强。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=Q06]`；../evidence/METRICS_44_PROJECTED.json#Q06

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：有效区间下界>0才计验证改善

### Q07 · 有效资产产量

**原status：** `RECORDED_UNVALIDATED`；**处置：** 调查补证；**关联：** RES26, RES30

**具体覆盖：** 有效证书0/sample0如实记录，评估器还指出非NATURAL角色；不能补配额造有效资产。

**验收方向：** 逐独立资产同时完成Q01/Q03/Q04/Q05，证书域/版本/消费匹配；0如实保留，互补性需独立证据。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=Q07]`；../evidence/METRICS_44_PROJECTED.json#Q07

原评估器状态：`NOT_PROVEN_NOT_NATURAL_ROLE`；值=0；样本=0。目标：目标≥1，继续寻找多个互补资产；0必须如实

### Q08 · 空信号与优势检出力

**原status：** `UNKNOWN_STALE_SYNTHETIC_SOURCE`；**处置：** 调查补证；**关联：** RES30, PLAN-SOURCE-04

**具体覆盖：** UNKNOWN_STALE_SYNTHETIC_SOURCE说明原注册不是当前源码，旧实验不能签当前版本。

**验收方向：** 当前source注册6臂×20种子完整搜索/选择/停止实验，报告空信号误授证CI及34/37/40%信号功效；120组合与原对照可复算。

**原件定位：** `R30_REPORT.json#/metricResults[metricId=Q08]`；../evidence/METRICS_44_PROJECTED.json#Q08

原评估器状态：`UNKNOWN_SOURCE_MISSING`；值=None；样本=None。目标：报告误授证率及CI、34/37/40%信号功效，不以一次模拟签收






<!-- CURRENT_MAPPING_BEGIN -->
## 本轮施工编号对应

原问题ID用于保留审计来源；本表给出最终施工编号。保留项/参考项链接只说明责任，不表示它被判为新故障。

|原项|当前施工问题|批次|
|---|---|---|
|M01|[R342-002](../issues/R342-002.md), [R342-054](../issues/R342-054.md), [R342-057](../issues/R342-057.md)|B01, B11, B12|
|M02|[R342-001](../issues/R342-001.md), [R342-054](../issues/R342-054.md)|B01, B11|
|M03|[R342-012](../issues/R342-012.md), [R342-016](../issues/R342-016.md)|B04, B05|
|M04|[R342-013](../issues/R342-013.md), [R342-016](../issues/R342-016.md)|B04, B05|
|M05|[R342-012](../issues/R342-012.md), [R342-013](../issues/R342-013.md)|B04|
|M06|[R342-054](../issues/R342-054.md)|B11|
|M07|[R342-004](../issues/R342-004.md), [R342-005](../issues/R342-005.md), [R342-016](../issues/R342-016.md)|B02, B05|
|M08|[R342-001](../issues/R342-001.md), [R342-004](../issues/R342-004.md)|B01, B02|
|M09|[R342-001](../issues/R342-001.md), [R342-007](../issues/R342-007.md), [R342-009](../issues/R342-009.md), [R342-046](../issues/R342-046.md)|B01, B03, B10|
|M10|[R342-010](../issues/R342-010.md), [R342-032](../issues/R342-032.md), [R342-054](../issues/R342-054.md)|B03, B08, B11|
|M11|[R342-010](../issues/R342-010.md), [R342-011](../issues/R342-011.md), [R342-032](../issues/R342-032.md)|B03, B08|
|M12|[R342-007](../issues/R342-007.md), [R342-008](../issues/R342-008.md), [R342-010](../issues/R342-010.md), [R342-011](../issues/R342-011.md)|B03|
|M13|[R342-008](../issues/R342-008.md), [R342-018](../issues/R342-018.md)|B03, B05|
|M14|[R342-018](../issues/R342-018.md), [R342-019](../issues/R342-019.md)|B05|
|M15|[R342-045](../issues/R342-045.md), [R342-046](../issues/R342-046.md), [R342-047](../issues/R342-047.md), [R342-048](../issues/R342-048.md)|B10|
|M16|[R342-045](../issues/R342-045.md), [R342-054](../issues/R342-054.md)|B10, B11|
|M17|[R342-053](../issues/R342-053.md), [R342-057](../issues/R342-057.md)|B11, B12|
|M18|[R342-033](../issues/R342-033.md), [R342-054](../issues/R342-054.md)|B08, B11|
|M19|[R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-054](../issues/R342-054.md)|B08, B11|
|M20|[R342-007](../issues/R342-007.md), [R342-031](../issues/R342-031.md), [R342-032](../issues/R342-032.md), [R342-046](../issues/R342-046.md)|B03, B08, B10|
|M21|[R342-022](../issues/R342-022.md), [R342-025](../issues/R342-025.md), [R342-026](../issues/R342-026.md), [R342-027](../issues/R342-027.md)|B06, B07|
|M22|[R342-025](../issues/R342-025.md), [R342-026](../issues/R342-026.md), [R342-040](../issues/R342-040.md)|B07, B08|
|M23|[R342-011](../issues/R342-011.md), [R342-020](../issues/R342-020.md), [R342-030](../issues/R342-030.md), [R342-054](../issues/R342-054.md)|B03, B06, B07, B11|
|M24|[R342-031](../issues/R342-031.md), [R342-044](../issues/R342-044.md), [R342-054](../issues/R342-054.md)|B08, B09, B11|
|M25|[R342-035](../issues/R342-035.md), [R342-036](../issues/R342-036.md), [R342-037](../issues/R342-037.md)|B08|
|M26|[R342-043](../issues/R342-043.md)|B09|
|M27|[R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-056](../issues/R342-056.md)|B08, B12|
|M28|[R342-044](../issues/R342-044.md), [R342-051](../issues/R342-051.md), [R342-054](../issues/R342-054.md)|B09, B11|
|M29|[R342-011](../issues/R342-011.md), [R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md)|B03, B08|
|M30|[R342-034](../issues/R342-034.md), [R342-044](../issues/R342-044.md)|B08, B09|
|M31|[R342-039](../issues/R342-039.md), [R342-049](../issues/R342-049.md), [R342-052](../issues/R342-052.md), [R342-053](../issues/R342-053.md)|B08, B11|
|M32|[R342-005](../issues/R342-005.md), [R342-053](../issues/R342-053.md), [R342-055](../issues/R342-055.md)|B02, B11|
|M33|[R342-017](../issues/R342-017.md), [R342-050](../issues/R342-050.md)|B05, B11|
|M34|[R342-002](../issues/R342-002.md), [R342-007](../issues/R342-007.md), [R342-018](../issues/R342-018.md)|B01, B03, B05|
|M35|[R342-044](../issues/R342-044.md), [R342-051](../issues/R342-051.md)|B09, B11|
|M36|[R342-034](../issues/R342-034.md), [R342-056](../issues/R342-056.md)|B08, B12|
|Q01|[R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-056](../issues/R342-056.md)|B08, B12|
|Q02|[R342-013](../issues/R342-013.md), [R342-044](../issues/R342-044.md), [R342-056](../issues/R342-056.md)|B04, B09, B12|
|Q03|[R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-056](../issues/R342-056.md)|B08, B12|
|Q04|[R342-034](../issues/R342-034.md), [R342-043](../issues/R342-043.md), [R342-056](../issues/R342-056.md)|B08, B09, B12|
|Q05|[R342-033](../issues/R342-033.md), [R342-043](../issues/R342-043.md)|B08, B09|
|Q06|[R342-034](../issues/R342-034.md), [R342-040](../issues/R342-040.md), [R342-041](../issues/R342-041.md), [R342-056](../issues/R342-056.md)|B08, B12|
|Q07|[R342-044](../issues/R342-044.md), [R342-056](../issues/R342-056.md)|B09, B12|
|Q08|[R342-002](../issues/R342-002.md), [R342-056](../issues/R342-056.md)|B01, B12|

<!-- CURRENT_MAPPING_END -->
