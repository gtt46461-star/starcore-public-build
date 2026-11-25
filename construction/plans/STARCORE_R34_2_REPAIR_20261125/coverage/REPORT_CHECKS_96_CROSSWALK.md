# 96项报告检查逐条覆盖表

基线：577311152 / eb8f82a8bda8db22f1f122095217132f07053bcb。原状态完全保留；施工归属使用原问题ID，不使用待拓扑重排的新编号。

逐项覆盖关系完成，不等于应用修复、设备验收或自然质量通过。

## 逐项处置

### RSEC-001 · 【R975故障定位：先看阻断，再看原始表】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RT01, RT03, RES07, RES08, RES20

**具体覆盖：** 原项是运行证据总览OBSERVED；25次POST_TRAIN与0正式计划/校准等必须分别解释，阻断落到启动、写背压、训练登记与诊断行动。

**验收方向：** 逐stage提供同版本输入、提交、游标、产物和消费收据；维护次数不计数值训练，缺表不得记0。

**原件定位：** `REPORT_DETAILS.txt` 第14行；../evidence/REPORT_CHECKS_96.json#RSEC-001

证据成员：`report-text-6c2297d96051103bb257223b3971259cd43c2abbd58b30324eb5bcbe0894787c.txt`

### RSEC-002 · 【逐文件、方法、直接调用索引：编译字节码静态索引，不是真机覆盖率】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 不适用并说明；**关联：** RT20, RT23, PLAN-SOURCE-01

**具体覆盖：** 这是已安装APK中43,648,638字节源码调用索引的引用，原NOT_APPLICABLE表示非运行断言；不是每条调用已经执行。

**验收方向：** 保留asset/sourceCommit/APK/hash绑定；以真实调用trace补运行覆盖，不将索引方法数转为功能通过率。

**原件定位：** `REPORT_DETAILS.txt` 第15行；../evidence/REPORT_CHECKS_96.json#RSEC-002

### RSEC-003 · 【R967同一语句快照：研究登记、训练、校准与队列】

**原health：** `OBSERVED`；**原domain_outcome：** `READ_OK`；**处置：** 修复；**关联：** RES07, RES08, RES23, RES25, RES26

**具体覆盖：** 单SQL漏斗有21研究登记、25训练更新，但正式计划/门政策/正式looks/校准模型为0；修生产闭环，不改OBSERVED原状态。

**验收方向：** 每一阶段用独立对象主键及单位对账；训练流25与正式封存未来0并列，新结果具备固定协议、版本和消费回执。

**原件定位：** `REPORT_DETAILS.txt` 第16行；../evidence/REPORT_CHECKS_96.json#RSEC-003

### RSEC-004 · 【R974逐段执行收据：只读、各查询独立时点、不是整库事务快照】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES06, RES07, RES11, RES21

**具体覆盖：** 一个OBSERVING候选future_n25/numeric_updates25停在202611250258，未形成真实父子训练连续链。

**验收方向：** 旧产物哈希和25次更新保留，补历史开发任务/连续在线游标/真实父边；每段原件可重放而非只更新状态。

**原件定位：** `REPORT_DETAILS.txt` 第17行；../evidence/REPORT_CHECKS_96.json#RSEC-004

### RSEC-005 · 【R970历史算法原始档案与新种子去向】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES03, RES05, RT16

**具体覆盖：** 125条历史算法原档及新种子去向有记录，worker_status仍NOT_YET_RUN；旧未来证据明确不可迁入新身份。

**验收方向：** 125条逐原hash保全；可用旧程序作为新研究种子，旧ABI对象用独立新身份重训，缺原件资产隔离。

**原件定位：** `REPORT_DETAILS.txt` 第18行；../evidence/REPORT_CHECKS_96.json#RSEC-005

证据成员：`report-text-b3778da398672cc974265441bf4e0d5b783033be983f6d7d66ab888a5c432a31.txt`

### RSEC-006 · 【手机储存与5000期清理】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RT15, RT16, RT17, RT18

**具体覆盖：** draws5000已满足热表约束，但3.176GB快照、完整存储约10.97GB及未请求离线维护需独立治理。

**验收方向：** 先完整owner/引用/预算盘点，退役须可恢复证明；分别核验hot行、文件字节与真实释放，不能用PASSIVE完成称文件已缩。

**原件定位：** `REPORT_DETAILS.txt` 第19行；../evidence/REPORT_CHECKS_96.json#RSEC-006

### RSEC-007 · 【原子世界资产工具与神经学习】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 保留回归；**关联：** RES13, RES18, RES24

**具体覆盖：** 165原子、55次网络更新是已记录历史研究事实，productionAuthority=0及改善下界未证；保留而不冒称当前生产有效。

**验收方向：** 保留原学习状态；将逐原子贡献、分组神经输出与同目标对照消费挂接，未获独立证据保持观察。

**原件定位：** `REPORT_DETAILS.txt` 第20行；../evidence/REPORT_CHECKS_96.json#RSEC-007

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

证据成员：`report-text-f2c5607cf67fc3ed20e61c7026f402cc6536300f430096969a9a0e6befe3b39f.txt`

### RSEC-009 · 【0.当前总判定】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY`；**处置：** 调查补证；**关联：** RT20, RT21, RT12, RES30

**具体覆盖：** 总判定明确计划验收未建立、设备需重测、模型证明不足；当前运行APK存在不等于完整方案完成。

**验收方向：** 六层验收分别签收；工程/设备/自然质量状态不得互相代签，完整保留未完成条件。

**原件定位：** `REPORT_DETAILS.txt` 第21行；../evidence/REPORT_CHECKS_96.json#RSEC-009

### RSEC-010 · 【0A.R923全软件情况统一总览（先看这里）】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 调查补证；**关联：** RT06, RT11, RT22

**具体覆盖：** 该总览写HEALTHY及7/30，与RSEC052当前严格覆盖1/30并存；生产者、角色、过滤与时间范围需对齐。

**验收方向：** 逐字段给scope/epoch/watermark和SQL分母；同范围才比对，健康不得由单个347成功覆盖其他缺期。

**原件定位：** `REPORT_DETAILS.txt` 第22行；../evidence/REPORT_CHECKS_96.json#RSEC-010

证据成员：`report-text-4ffdbb3f1c861fc0fccc4bbce1c7dba5413582b3d400034000d367ceeef7cfdb.txt`

### RSEC-011 · 【分时读取的详细视图；不作为上方统一漏斗分母】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 调查补证；**关联：** RT22, RES08, RES23

**具体覆盖：** 标题已声明分时读取详细视图不能当统一漏斗分母；研究登记、维护任务与正式实验不是同一计数单位。

**验收方向：** 各查询保留时间和对象单位，去重映射到同一candidate/task/experiment；缺件原样UNKNOWN，不相加充产出。

**原件定位：** `REPORT_DETAILS.txt` 第23行；../evidence/REPORT_CHECKS_96.json#RSEC-011

证据成员：`report-text-3780e71a3fc4d2fd2519ce52c6bb941644265e68a04f3fd0fe6620e8c48ccbdd.txt`

### RSEC-012 · 【挑战者、证书与主链实际应用｜统一账本】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RT03, RES23, RES26, ROOT-01

**具体覆盖：** 8登记/0开始/0冻结、CP12_RESERVE_PROSPECTIVE_CAPTURE被写背压拒绝、0证书/0应用构成具体断点。

**验收方向：** 修原排空后完成真实前瞻捕获→固定未来→证书→精确消费，按证书域验权，未具资格保持SHADOW。

**原件定位：** `REPORT_DETAILS.txt` 第24行；../evidence/REPORT_CHECKS_96.json#RSEC-012

### RSEC-013 · 【GENESIS_NEW创生资产榜】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES21, RES23, RES24

**具体覆盖：** GENESIS_NEW已登记但N/H=0/0、CHILD_EVIDENCE_RESET_N0；名称、代数、程序长度不是进化质量。

**验收方向：** 保留原可执行程序和状态，推进新版本冻结/同期评价；只将独立验证改善记晋升，不继承旧N。

**原件定位：** `REPORT_DETAILS.txt` 第25行；../evidence/REPORT_CHECKS_96.json#RSEC-013

### RSEC-014 · 【活跃算法家族Future OOS榜】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES05, RES21, RES23

**具体覆盖：** 活跃算法为历史研究种子第41代而Future N=0；历史代数不能当当前41次验证改善。

**验收方向：** 保存父源与新身份，启动实际结构搜索/开发训练/合法冻结/未来结算；分列历史代数和已验证代际差。

**原件定位：** `REPORT_DETAILS.txt` 第26行；../evidence/REPORT_CHECKS_96.json#RSEC-014

### RSEC-015 · 【算法证据归档榜（淘汰不删除）】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 保留回归；**关联：** RES21

**具体覆盖：** NO_ARCHIVED_ALGORITHM_FAMILY是当前档案无样本，不是丢算法的证据；淘汰保留证据的合同应保全。

**验收方向：** 以真实淘汰样例验证状态迁移保留定义/父代/指标/首次原因；活跃榜和归档榜不可混计。

**原件定位：** `REPORT_DETAILS.txt` 第27行；../evidence/REPORT_CHECKS_96.json#RSEC-015

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-016 · 【240项底层因子/原子完整登记】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 保留回归；**关联：** RES13

**具体覆盖：** 240原子是完整静态登记，身份字符串原子与数值评分原子semanticTier不同，不能把全部登记当已学习特征。

**验收方向：** 逐原子保留类型、范围、时点和更新限制；实际准入/绑定/消费/边际增益另由产物验证。

**原件定位：** `REPORT_DETAILS.txt` 第28行；../evidence/REPORT_CHECKS_96.json#RSEC-016

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

证据成员：`report-text-f8d86637213f011595dc9d7c942b3c8799b544cf088f8a1ad42f9f4293713b50.txt`

### RSEC-017 · 【失败驱动的新原子提案】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES13, RES20

**具体覆盖：** NO_FAILURE_DRIVEN_ATOM_PROPOSAL记录失败驱动原子提案尚无样本。

**验收方向：** 失败原件→可反证假设→有类型的新原子程序→同目标对照→准入/拒绝，逐阶段能追溯原错误。

**原件定位：** `REPORT_DETAILS.txt` 第29行；../evidence/REPORT_CHECKS_96.json#RSEC-017

### RSEC-018 · 【0B.5.4.1新增检查与运行证据】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 调查补证；**关联：** RT09, RT12, RT20, RT22

**具体覆盖：** R925静态审计资产明确是历史资料；当前release另有1/16覆盖和冷/暖不足样本，不能把旧审计签当前版。

**验收方向：** 保存历史资产hash；当前版本重采分段性能/覆盖/设备矩阵并与旧范围分栏，构建标签不当验收。

**原件定位：** `REPORT_DETAILS.txt` 第30行；../evidence/REPORT_CHECKS_96.json#RSEC-018

证据成员：`report-text-0dc0610836890d004360350c8ed22951e52fec1026a636f7658bbf92b0190e13.txt`

### RSEC-019 · 【E05-A.模型准确率卡】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `ENGINEERING_IMPLEMENTED_REAL_DATA_AND_FUTURE_EVIDENCE_PENDING`；**处置：** 调查补证；**关联：** RES10, RES25, RES30, ROOT-02

**具体覆盖：** 模型准确率卡全部Future/结构/效果证明未建立，selected支持、ECE/Brier等N/A不等于0或失败命中率。

**验收方向：** 绑定当前真实NumberQ/Ranker/校准产物与冻结样本，补质量与稳定性证据，原评分不展示成已校准概率。

**原件定位：** `REPORT_DETAILS.txt` 第31行；../evidence/REPORT_CHECKS_96.json#RSEC-019

### RSEC-020 · 【E05-B.策略效果卡】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `ENGINEERING_IMPLEMENTED_REAL_DATA_AND_FUTURE_EVIDENCE_PENDING`；**处置：** 修复；**关联：** RES07, RES23, RES24, RES25, RES26

**具体覆盖：** 策略效果卡缺dataset/bundle/policy/stability/certificate等，GateFailures明确，15%/40%是该卡目标而非放行依据。

**验收方向：** 逐缺件修生产协议，独立未污染未来窗完成前不准REAL；证书域与gateVersion明确，不从其他域挑低门槛。

**原件定位：** `REPORT_DETAILS.txt` 第32行；../evidence/REPORT_CHECKS_96.json#RSEC-020

### RSEC-021 · 【E05-C.期级链路卡】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `ENGINEERING_IMPLEMENTED_REAL_DATA_AND_FUTURE_EVIDENCE_PENDING`；**处置：** 修复；**关联：** RT05, RT06, RES23, RES26

**具体覆盖：** 期级卡opportunity/available/eligible/started/frozen全0且bundle/policy N/A，声明的RequiredRoute未被真实收据贯通。

**验收方向：** 用同target生产正例与拒绝反例串联planned→frozen→SHADOW/REAL/终态，保留单DecisionGate/Settlement owner。

**原件定位：** `REPORT_DETAILS.txt` 第33行；../evidence/REPORT_CHECKS_96.json#RSEC-021

### RSEC-022 · 【E05-D.资源效率卡】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `ENGINEERING_IMPLEMENTED_REAL_DATA_AND_FUTURE_EVIDENCE_PENDING`；**处置：** 调查补证；**关联：** RT03, RT08, RT09, RES29

**具体覆盖：** 347资源原件存在，含1771ms执行、184ms锁等待及堆采样，但不是完整窗口资源达标证明。

**验收方向：** 保留25ms采样与进程GC差分语义；同scope记录排队/计算/锁/提交/取消，不将资源结果签成准确率。

**原件定位：** `REPORT_DETAILS.txt` 第34行；../evidence/REPORT_CHECKS_96.json#RSEC-022

证据成员：`report-text-2bd51e6b380f4ab348269b79cf35c254fadce2e88f1e5c97e00e527c97c87929.txt`

### RSEC-023 · 【旧算法观察桥接（独立历史口径）】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RT03, RES23, RES26, RT22

**具体覆盖：** 旧观察桥接显示同一前瞻reservation背压与0证书；不能将桥接重复渲染再算一条独立证据。

**验收方向：** 桥接保持只读、旧权重与未来证据不迁移；新捕获/证书消费按统一权威账验证并去重。

**原件定位：** `REPORT_DETAILS.txt` 第35行；../evidence/REPORT_CHECKS_96.json#RSEC-023

### RSEC-024 · 【0D.真实训练与逐期执行证据：fullblood】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES07, RES08, RES11, RES23

**具体覆盖：** fullblood输出是漏斗事实集合，POST_TRAIN25不抵消正式计划/验证/冻结为0的断点。

**验收方向：** 每类入口有真实任务、输入、数值/结构产物与后继消费；空阶段给具体依赖，不以页面已展示视为能力已运行。

**原件定位：** `REPORT_DETAILS.txt` 第36行；../evidence/REPORT_CHECKS_96.json#RSEC-024

### RSEC-025 · 【0D.真实训练与逐期执行证据：capability_trace】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 保留回归；**关联：** RES11, RES23

**具体覆盖：** capability_trace保留25次数值更新hash链，STRICT_FUTURE行同时写TRAIN_STREAM_V2_LABEL/SEALED_FUTURE_N_UNCHANGED。

**验收方向：** 重放验证更新幂等和模型连续性；保持训练标签与封存评价分离，并修最新数据追赶。

**原件定位：** `REPORT_DETAILS.txt` 第37行；../evidence/REPORT_CHECKS_96.json#RSEC-025

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-026 · 【0C.R940界面卡顿与闪退取证】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RT10, RT13

**具体覆盖：** 1507帧中7慢/4严重、最大176.004ms间隔为实测；崩溃摘要需按版本/时间分离。

**验收方向：** 关联慢帧到主线程/GC/IO/绑定，对成熟页面回归；旧版本crash不计当前复现，采当前设备日志。

**原件定位：** `REPORT_DETAILS.txt` 第38行；../evidence/REPORT_CHECKS_96.json#RSEC-026

### RSEC-027 · 【1.身份与不可变红线】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 保留回归；**关联：** PLAN-SOURCE-04

**具体覆盖：** 身份和fixed-K3/单位置/每期最多一个REAL为合同描述，原NOT_APPLICABLE不是运行失败。

**验收方向：** 保持包名、玩法、唯一门和唯一结算；新版本构建身份链真实更新，不只改标签。

**原件定位：** `REPORT_DETAILS.txt` 第39行；../evidence/REPORT_CHECKS_96.json#RSEC-027

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-028 · 【2.B01–B15工程状态】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 不适用并说明；**关联：** RT20, PLAN-SOURCE-01

**具体覆盖：** B01–B15仅历史名称，原文明确未完成最终验收；不能按批次名字或文件数量计完成。

**验收方向：** 保留历史标记并绑定152基线的当前工单/收据；以每层实证签收，不把历史标签删除或升格。

**原件定位：** `REPORT_DETAILS.txt` 第40行；../evidence/REPORT_CHECKS_96.json#RSEC-028

### RSEC-029 · 【3.唯一运行状态机】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 保留回归；**关联：** RT20, RT05

**具体覆盖：** 原NOT_APPLICABLE为静态状态机文本，写先结算再下期请求；实际BusinessReconciler先尝试下期持久请求的时效设计应保留。

**验收方向：** 按实际调用与并发owner校核流程文本；不得依此旧摘要改为等全部结算训练才预测，PIT/唯一冻结/结算不变。

**原件定位：** `REPORT_DETAILS.txt` 第41行；../evidence/REPORT_CHECKS_96.json#RSEC-029

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-030 · 【4.R3.6硬门】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY`；**处置：** 修复；**关联：** RT05, RT06, RT07, RT09

**具体覆盖：** 硬门已有K1覆盖1/30失败；并发1和单样本2804ms等局部PASS需各守范围，不能汇总抹平K1。

**验收方向：** 覆盖、时延样本、OOM、canonical不覆盖、OOS无模型调用分别验收；缺一期即有明确原因和终态。

**原件定位：** `REPORT_DETAILS.txt` 第42行；../evidence/REPORT_CHECKS_96.json#RSEC-030

### RSEC-031 · 【5.实际开奖分母预测覆盖与最近10期业务闭环】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RT05, RT06, RT07, RT22

**具体覆盖：** 最近有canonical的10条列表跳过缺失期，且formalActions=0/deferred=PENDING；不能当连续10期闭环。

**验收方向：** 用完整eligible期清单LEFT JOIN请求/冻结/结算/明细，旧canonical与当前release过滤明确；恢复延后证据。

**原件定位：** `REPORT_DETAILS.txt` 第43行；../evidence/REPORT_CHECKS_96.json#RSEC-031

### RSEC-032 · 【6.进程/Service/业务心跳】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RT01, RT11, RT12

**具体覆盖：** 引用真实heartbeat：服务alive而研究健康陈旧；进程、服务、业务和研究进度必须分层。

**验收方向：** 原owner恢复须实际slice提交/游标增长；attempt心跳不能刷新progress，设备场景补收据。

**原件定位：** `REPORT_DETAILS.txt` 第44行；../evidence/REPORT_CHECKS_96.json#RSEC-032

### RSEC-033 · 【7.R3.6运行健康账本】

**原health：** `OBSERVED`；**原domain_outcome：** `OK`；**处置：** 调查补证；**关联：** RT09, RT13

**具体覆盖：** 当前runtime账只有1个合法单调durable样本，P95=2804ms/maxConcurrency1/oom0不代表全天。

**验收方向：** 补同release足量事件及未测样本原因；历史累计与当前隔离，并发和崩溃用实际场景复测。

**原件定位：** `REPORT_DETAILS.txt` 第45行；../evidence/REPORT_CHECKS_96.json#RSEC-033

### RSEC-034 · 【8.严格OOS/Wilson/ECE/Brier/Coverage】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY`；**处置：** 调查补证；**关联：** RES23, RES25, RES30, ROOT-01

**具体覆盖：** 1858/594累计严格shadow命中约31.97%，Wilson下界29.888%，合法校准样本0；展示PromotionMinOos120非所有域发证规则。

**验收方向：** 保留命中原件，按各冻结模型/证书域重采自然未来与校准；不能将累计shadow或120展示值签新策略。

**原件定位：** `REPORT_DETAILS.txt` 第46行；../evidence/REPORT_CHECKS_96.json#RSEC-034

### RSEC-035 · 【9.模型有效性与根因优先级】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY`；**处置：** 修复；**关联：** RT06, RT22, RES25, RES26

**具体覆盖：** 根因优先CALIBRATION_UNPROVEN与真实覆盖失败并存；当前artifact/数据范围决定哪些诊断适用。

**验收方向：** 优先处理阻断运行与覆盖，再补选择后概率/独立证书；根因排序引用同scope事实，不只看静态模型标志。

**原件定位：** `REPORT_DETAILS.txt` 第47行；../evidence/REPORT_CHECKS_96.json#RSEC-035

### RSEC-036 · 【10.唯一Owner】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 保留回归；**关联：** RT03, RT21

**具体覆盖：** NOT_APPLICABLE owner清单声明唯一分析/决策/结算/OOS/deferred，老R35写者只读；必须保留所有权。

**验收方向：** 竞争请求、重启和重复开奖验证无双写；新能力注册原调度链，不能并建第二主链。

**原件定位：** `REPORT_DETAILS.txt` 第48行；../evidence/REPORT_CHECKS_96.json#RSEC-036

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-037 · 【11.1200正式动作与实时/延后写分离】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY`；**处置：** 修复；**关联：** RT07

**具体覆盖：** 1200动作合同保持，但1632延后作业最老202611160380、游标最大700等说明全证据未完成。

**验收方向：** 原immutable snapshot分块补100/1200行，按identity、唯一键和最终数量回读；不裁剪动作宇宙。

**原件定位：** `REPORT_DETAILS.txt` 第49行；../evidence/REPORT_CHECKS_96.json#RSEC-037

### RSEC-038 · 【12.精确结算与OOS标签】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 保留回归；**关联：** RT06

**具体覆盖：** 最新346为NO_PREDRAW_CANONICAL_TERMINAL、OOS禁止模型重算，是正确拒绝语义而非应补造预测。

**验收方向：** 只用开奖前canonical与精确draw生成标签；已有义务幂等补结算，缺预测终态保持可见。

**原件定位：** `REPORT_DETAILS.txt` 第50行；../evidence/REPORT_CHECKS_96.json#RSEC-038

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-039 · 【13.REAL与SHADOW分账】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY`；**处置：** 保留回归；**关联：** RES26, RES30

**具体覆盖：** REAL/SHADOW分账与exact-gate已声明，质量仍NOT_PROVEN；不以无REAL为拆门理由。

**验收方向：** 真实/影子各自计盈亏与校准；合法证书正例可消费，缺证书/过期反例必须拒绝。

**原件定位：** `REPORT_DETAILS.txt` 第51行；../evidence/REPORT_CHECKS_96.json#RSEC-039

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-040 · 【14.真实出手机会漏斗（15%仅观察目标）】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 调查补证；**关联：** RT06, RES26, RES30

**具体覆盖：** 窗口30/120/1200均只有2机会样本、BET0、拒绝NO_QUALIFIED_MODEL_PROOF；15%是观察目标且禁止补频率。

**验收方向：** 修全机会分母和真实资格闭环；报告目标差、拒绝与样本区间，不能强制下注或继承虚假资格。

**原件定位：** `REPORT_DETAILS.txt` 第52行；../evidence/REPORT_CHECKS_96.json#RSEC-040

### RSEC-041 · 【15.后台恢复】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RT03, RT11, RT12

**具体覆盖：** 后台loop已ARMED但窗口关闭写入被背压拒绝、review1/22、deviceTimingVerified0；已武装不等于已执行。

**验收方向：** 修既有独立排空/研究owner，按screen-off/HOME/task-removed验证业务游标与有界恢复；尊重用户停止。

**原件定位：** `REPORT_DETAILS.txt` 第53行；../evidence/REPORT_CHECKS_96.json#RSEC-041

### RSEC-042 · 【16.物理设备验证矩阵】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 调查补证；**关联：** RT12, RT13

**具体覆盖：** 原NOT_APPLICABLE项是待测试矩阵，T29–T34均RETEST_REQUIRED，不能解读为设备无需验收。

**验收方向：** 真机前台/息屏/划掉任务/杀进程/主备网络/24h及72h逐场景收据，包含全部缺口及重复消费检查。

**原件定位：** `REPORT_DETAILS.txt` 第54行；../evidence/REPORT_CHECKS_96.json#RSEC-042

### RSEC-043 · 【17.历史链只读隔离】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 保留回归；**关联：** PLAN-SOURCE-01

**具体覆盖：** 旧runtime实例N/A，政策为只读观察且无预测/BET/结算owner；这是需保全的隔离合同。

**验收方向：** 旧功能读路径仍可用，升级不复活旧并行写者；固定输入与调用所有权回归。

**原件定位：** `REPORT_DETAILS.txt` 第55行；../evidence/REPORT_CHECKS_96.json#RSEC-043

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-044 · 【18A.B14–B15原始取证附录】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 不适用并说明；**关联：** RT20, RT23

**具体覆盖：** 本项仅B14–B15章节组合索引，实际检查在列出的子RSEC中，不应另算业务失败或通过。

**验收方向：** 组合children全部解析到96原检查及真实原件；修各子项不删索引、不双算证据。

**原件定位：** `REPORT_DETAILS.txt` 第56行；../evidence/REPORT_CHECKS_96.json#RSEC-044

### RSEC-048 · 【第1章 数据与期号】

**原health：** `PASS`；**原domain_outcome：** `PASS`；**处置：** 保留回归；**关联：** RT05

**具体覆盖：** PASS只证明base346→target347与resolver精确一致，不证明跨日/缺口全部正确。

**验收方向：** 保留该样例，增加边界期号/跨日/非法输入场景，仍通过唯一TargetIssueResolver。

**原件定位：** `REPORT_DETAILS.txt` 第57行；../evidence/REPORT_CHECKS_96.json#RSEC-048

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-052 · 【第2章 Service与业务运行】

**原health：** `FAIL`；**原domain_outcome：** `FAIL`；**处置：** 修复；**关联：** RT01, RT03, RT05, RT06, RT07, RT09, RT11

**具体覆盖：** 原FAIL直接为PREDRAW_COVERAGE_MISSING；服务alive、已有义务lag0、单样本时延PASS不抵消缺期。

**验收方向：** 完整机会对账，修历史/PIT后计算提交断点、启动与背压；新期合法freeze及old WAIT_DRAW/证据作业有明确结果。

**原件定位：** `REPORT_DETAILS.txt` 第58行；../evidence/REPORT_CHECKS_96.json#RSEC-052

### RSEC-053 · 【第3章 账本与首写冻结】

**原health：** `WARN`；**原domain_outcome：** `RETEST_REQUIRED`；**处置：** 修复；**关联：** RT07

**具体覆盖：** 当前核心issue/champion/selected/auth各1而number/full为0、deferred PENDING；dup=false仅当前观测。

**验收方向：** 补延后明细且保持首写不覆盖；注入重复提交/重启验证canonical、授权、结算幂等。

**原件定位：** `REPORT_DETAILS.txt` 第59行；../evidence/REPORT_CHECKS_96.json#RSEC-053

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-054 · 【第4章 100号码概率面】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `NOT_PROVEN`；**处置：** 修复；**关联：** RT07, RES25, ROOT-02

**具体覆盖：** 当前347完整身份已给，但0/100号码行、10位置有效单元全0、permutation MISSING；不能验收曲面。

**验收方向：** 从原冻结bytes补齐100单元、归一化/有限性/Sinkhorn行列误差与模型版本；缺原件仍UNKNOWN，质量另验。

**原件定位：** `REPORT_DETAILS.txt` 第60行；../evidence/REPORT_CHECKS_96.json#RSEC-054

### RSEC-055 · 【第5章 跨位置结构】

**原health：** `WARN`；**原domain_outcome：** `RETEST_REQUIRED`；**处置：** 调查补证；**关联：** RT07, RES18, ROOT-02

**具体覆盖：** relation/permutation/transition均NO_RUNTIME_SAMPLE，不能由代码存在判跨位机制已运行，也不能断言实现不存在。

**验收方向：** 补同target冻结关系图、置换/转移矩阵与实际消费；对照/消融验证语义，延后证据只重放原快照。

**原件定位：** `REPORT_DETAILS.txt` 第61行；../evidence/REPORT_CHECKS_96.json#RSEC-055

### RSEC-056 · 【第6章 固定K3漏斗】

**原health：** `WARN`；**原domain_outcome：** `RETEST_REQUIRED`；**处置：** 调查补证；**关联：** RT07, ROOT-02

**具体覆盖：** 静态9→8→7→6→5→4→3漏斗存在，runtime无样本。

**验收方向：** 捕获同target逐层候选/第三边界及最终固定K3证据；不把静态枚举或最终3号当全漏斗已验证。

**原件定位：** `REPORT_DETAILS.txt` 第62行；../evidence/REPORT_CHECKS_96.json#RSEC-056

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-057 · 【第7章 1200正式动作空间】

**原health：** `WARN`；**原domain_outcome：** `RETEST_REQUIRED`；**处置：** 修复；**关联：** RT07

**具体覆盖：** 当前CANONICAL_COMMITTED与deferred PENDING并存，原文明确未验收全部1200动作。

**验收方向：** 游标分片至完整1200唯一动作、100单元和proof，核对相同record/contract/hash，重启不重算或删动作。

**原件定位：** `REPORT_DETAILS.txt` 第63行；../evidence/REPORT_CHECKS_96.json#RSEC-057

### RSEC-058 · 【第8章 动作Ranker】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `NOT_PROVEN`；**处置：** 调查补证；**关联：** RES10, RES23, ROOT-02

**具体覆盖：** 动作Ranker为NO_RUNTIME_SAMPLE，没有当前同target1200排序与产物消费证据。

**验收方向：** 冻结完整动作组、ranker版本及排序结果，做顺序稳定、Rank1/MRR/NDCG/配对对照；静态PROVEN标志不能替代。

**原件定位：** `REPORT_DETAILS.txt` 第64行；../evidence/REPORT_CHECKS_96.json#RSEC-058

### RSEC-059 · 【第9章 选择后校准与严格OOS】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `WAIT_CALIBRATION_INPUT`；**处置：** 修复；**关联：** RT06, RES23, RES25, RES26

**具体覆盖：** 合法校准不足、strictOOS累计1858与覆盖1/30不同角色；calibration是独立训练/验证链。

**验收方向：** 补fit结束<validate开始、选择条件一致的冻结校准与未来质量；不把raw分数改名为概率。

**原件定位：** `REPORT_DETAILS.txt` 第65行；../evidence/REPORT_CHECKS_96.json#RSEC-059

### RSEC-060 · 【第10章 频率与REAL硬门】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `NOT_PROVEN`；**处置：** 调查补证；**关联：** RT21, RES26, RES30

**具体覆盖：** REAL合同total0/dup0的PASS_EXACT_CONTROLLED_LEDGER为无数据局部检查，原项仍NOT_PROVEN。

**验收方向：** 用合法证书正例及失效/重复/过期反例运行唯一Gate和结算；不把空账零违规当全面通过。

**原件定位：** `REPORT_DETAILS.txt` 第66行；../evidence/REPORT_CHECKS_96.json#RSEC-060

### RSEC-061 · 【第11章 失败树与反证】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `NOT_PROVEN`；**处置：** 修复；**关联：** ROOT-02, RES20, RT22

**具体覆盖：** 失败树复制B14模型未证与runtime活跃摘要；必须把当前证据、假设与模型绑定，不能从总标签推具体原因。

**验收方向：** 每条失败层提供正证/反证/适用模型与验证动作，已确认与待证分列。

**原件定位：** `REPORT_DETAILS.txt` 第67行；../evidence/REPORT_CHECKS_96.json#RSEC-061

### RSEC-062 · 【B14动态故障诊断】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `ENGINEERING_DIAGNOSIS_ACTIVE_REAL_REMAINS_GATED`；**处置：** 修复；**关联：** ROOT-02, RES20, RT02

**具体覆盖：** 动态诊断的ACTIVATE/已安装不证明cause→action→后果已发生；错误分类应消费typed cause。

**验收方向：** 真实原因驱动原owner修复，记录动作前后同scope状态与失败回退；不能只产生描述文本。

**原件定位：** `REPORT_DETAILS.txt` 第68行；../evidence/REPORT_CHECKS_96.json#RSEC-062

### RSEC-063 · 【确认事实/假设分离】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `ENGINEERING_DIAGNOSIS_ACTIVE_REAL_REMAINS_GATED`；**处置：** 调查补证；**关联：** ROOT-02, RT22, RT23

**具体覆盖：** 同一B14正文被用作事实/假设分离章节，需核验每条confidence与正反证是否来自实际当前原件。

**验收方向：** 保留原结论，标明确认/源码机制/待复现；无法取得原字节就给明确采集项，禁止将哈希当原始原因。

**原件定位：** `REPORT_DETAILS.txt` 第69行；../evidence/REPORT_CHECKS_96.json#RSEC-063

### RSEC-064 · 【因果故障图】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `ENGINEERING_DIAGNOSIS_ACTIVE_REAL_REMAINS_GATED`；**处置：** 调查补证；**关联：** RT02, RT03, RT20, ROOT-02

**具体覆盖：** 因果图与通用模型依赖不等于现场故障因果；writer排空不可反等研究owner/全局GC。

**验收方向：** 区分证据依赖与施工依赖，验证有向无环及每边实际调用/数据条件；不假设556缺件导致已通过PREPARE的保存失败。

**原件定位：** `REPORT_DETAILS.txt` 第70行；../evidence/REPORT_CHECKS_96.json#RSEC-064

### RSEC-065 · 【修复优先级】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `ENGINEERING_DIAGNOSIS_ACTIVE_REAL_REMAINS_GATED`；**处置：** 修复；**关联：** RT01, RT02, RT03, RT05, RES20

**具体覆盖：** 修复优先级必须先解除可证工程阻断，再训练/校准，不能用静态CALIBRATION优先遮住启动/背压/缺预测。

**验收方向：** B02局部排空→B03原启动恢复→覆盖→研究质量，所有动作有结果与终态，真正BUG保留断路。

**原件定位：** `REPORT_DETAILS.txt` 第71行；../evidence/REPORT_CHECKS_96.json#RSEC-065

### RSEC-066 · 【K3失败九类拆解】

**原health：** `FAIL`；**原domain_outcome：** `BLOCKED`；**处置：** 修复；**关联：** ROOT-02, RES20, RES25

**具体覆盖：** 原FAIL九层拆解以静态Number/Rank PROVEN=false为confidence1，THIRD等无运行原件；当前准确归因不足。

**验收方向：** NUMBER/RANK/THIRD/FUNNEL/RELATION/CALIBRATION/POLICY/SAMPLE/DRIFT逐层绑定当前模型和冻结数据，反例中不误判根因。

**原件定位：** `REPORT_DETAILS.txt` 第72行；../evidence/REPORT_CHECKS_96.json#RSEC-066

### RSEC-067 · 【第12章 消融与冲突】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 调查补证；**关联：** RES18, RES22, RES24, ROOT-02

**具体覆盖：** 原OBSERVED记录NO_MEASURED_ABLATION和各模块UNAVAILABLE，这是缺测事实而非已证明模块无用。

**验收方向：** 保留功能，补同target开关消融及配对deltaHit/dBrier/dCoverage/dECE、区间与预算，失败结果也留存。

**原件定位：** `REPORT_DETAILS.txt` 第73行；../evidence/REPORT_CHECKS_96.json#RSEC-067

### RSEC-068 · 【策略贡献/冲突/冗余/消融】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `NO_MEASURED_ABLATION`；**处置：** 调查补证；**关联：** RES18, RES22, RES24

**具体覆盖：** 与RSEC067共用消融正文，NOT_PROVEN不应被“审计已生成”提升为贡献已验证。

**验收方向：** 逐模块共用唯一冻结对照原件，不重复计样本；有产物/消费者/边际贡献才计完成。

**原件定位：** `REPORT_DETAILS.txt` 第74行；../evidence/REPORT_CHECKS_96.json#RSEC-068

### RSEC-069 · 【公式/算法/概率审计】

**原health：** `WARN`；**原domain_outcome：** `WATCH`；**处置：** 调查补证；**关联：** RT07, RT21, RES25, ROOT-02

**具体覆盖：** 11审计条款中组合数/K=3/分片是合同；parity NOT_RUN、Number/Rank/SelectedHitP未证、strictOOS/Sinkhorn/PIT证据缺失需补。

**验收方向：** 保持1200/K3，分别执行冻结parity、归一化误差、PIT扰动、rank质量及校准；8个warning不能由静态常量消除。

**原件定位：** `REPORT_DETAILS.txt` 第75行；../evidence/REPORT_CHECKS_96.json#RSEC-069

### RSEC-070 · 【第13章 代码Owner完整性】

**原health：** `WARN`；**原domain_outcome：** `WARN`；**处置：** 调查补证；**关联：** RT21, PLAN-SOURCE-01, ROOT-02

**具体覆盖：** 17个owner审计为11PASS/5WATCH/1NA，静态ownerConflict0不证明所有动态路径无双写。

**验收方向：** 保留11条所有权合同，补PIT/Number/Rank/SelectedHitP/Policy及source governance证据，运行并发/恢复反例。

**原件定位：** `REPORT_DETAILS.txt` 第76行；../evidence/REPORT_CHECKS_96.json#RSEC-070

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-071 · 【代码Owner与完整性审计】

**原health：** `WARN`；**原domain_outcome：** `WATCH`；**处置：** 调查补证；**关联：** RT21, PLAN-SOURCE-01

**具体覆盖：** 此项与070共正文但不同检查入口；保留各原ID，静态owner通过与实际执行质量分开。

**验收方向：** 一表一owner索引与生产调用收据对照，旧只读入口不变，变更前后文件卡/哈希/方法去向完整。

**原件定位：** `REPORT_DETAILS.txt` 第77行；../evidence/REPORT_CHECKS_96.json#RSEC-071

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-072 · 【第14章 Challenger施工与验证】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES07, RES10, RES23, RES25, RES26

**具体覆盖：** Challenger改进对象已有计划，但DatasetHash unavailable、Binding BLOCKED、ProductionMutation0；计划存在不等于训练完成。

**验收方向：** 每个Number/Rank/校准挑战者真正绑定输入/代码/产物/协议，经未来验证再精确消费，旧冠军继续。

**原件定位：** `REPORT_DETAILS.txt` 第78行；../evidence/REPORT_CHECKS_96.json#RSEC-072

### RSEC-073 · 【改进Challenger计划】

**原health：** `NOT_PROVEN`；**原domain_outcome：** `REAL_LOCKED_SHADOW_ONLY`；**处置：** 修复；**关联：** RES07, RES23, RES24, RES25

**具体覆盖：** 与072同计划正文、原NOT_PROVEN；冻结OOS与时间顺序训练、数据hash和验证条件仍未落实。

**验收方向：** 从计划转换为原调度器真实任务及产物，验收数值变化/可恢复/独立评价，不能只修改计划状态。

**原件定位：** `REPORT_DETAILS.txt` 第79行；../evidence/REPORT_CHECKS_96.json#RSEC-073

### RSEC-074 · 【第15章 Finding完整施工账本】

**原health：** `FAIL`；**原domain_outcome：** `FAIL`；**处置：** 修复；**关联：** RT05, RT06, RT07, RT21, RES25, RES26, ROOT-02

**具体覆盖：** 原FAIL汇总包含14个子Finding：仅runtime为已观察FAIL，其余未证/重测/警告/观察/数据PASS分别承接，详见subfindings。

**验收方向：** 每个子项独立状态与证据，不因总FAIL把DATA PASS改失败，也不因计划已生成把模型未证改通过。

**原件定位：** `REPORT_DETAILS.txt` 第80行；../evidence/REPORT_CHECKS_96.json#RSEC-074

#### RSEC-074 的14个原子Finding

| 原ID | 原状态 | 处置 | 原问题关联 | 覆盖与验收 |
|---|---|---|---|---|
| B14-F020-RUNTIME | FAIL | 修复 | RT05, RT06, RT03 | 前瞻覆盖缺失已被当前运行观察确认；修恢复/历史/PIT后提交断点，完整机会有成功或明确缺失终态 |
| B14-F040-NUMBER_SURFACE | NOT_PROVEN | 调查补证 | RT07, RES10, ROOT-02 | 号码面工程存在但当前0/100落地与模型质量未证须分开；补同身份100单元/模型冻结/数值parity和独立质量 |
| B14-F080-RANKER | NOT_PROVEN | 调查补证 | RES10, RES23, ROOT-02 | Ranker当前产物和1200同组排序质量未证；补真实模型消费、Rank1/MRR/NDCG及冻结排序反例 |
| B14-F090-CALIBRATION | NOT_PROVEN | 修复 | RES25 | 选择后校准和固定独立验证缺件；fitEnd<validateStart、同ACT条件、ECE/Brier及可见版本 |
| B14-F100-FREQUENCY | NOT_PROVEN | 调查补证 | RES26, RES30 | 空REAL账通过局部合同不证明有效频率；先资格后真实消费，完整分母、不补下注配额 |
| B14-F110-FAILURE_TREE | NOT_PROVEN | 修复 | ROOT-02, RES20 | NUMBER/BLOCKED由静态PROVEN和关键词不足以准确归因；九层当前证据、typed cause、反证与可验证动作闭环 |
| B14-F030-LEDGER | RETEST_REQUIRED | 修复 | RT07 | 核心已有而deferred待续写，不能覆写canonical；补全证据并在并发/恢复中验证首写和幂等 |
| B14-F050-CROSS_POSITION | RETEST_REQUIRED | 调查补证 | RT07, RES18 | 跨位relation/permutation/transition缺运行样本；同target冻结结构与实际消费对账、保留原件 |
| B14-F060-K3_FUNNEL | RETEST_REQUIRED | 调查补证 | RT07 | 固定K3合同存在但逐层运行证据缺失；保存9→3候选边界，最终K=3、完整1200不变 |
| B14-F070-ACTION_SPACE | RETEST_REQUIRED | 修复 | RT07 | 公式1200而当前持久行0/deferred PENDING；分片完成1200唯一动作、游标/原件hash回读 |
| B14-F130-CODE_OWNER | WARN | 调查补证 | RT21, PLAN-SOURCE-01 | owner静态WATCH且无显式冲突，不代表动态无双写；生产调用/同表owner与故障并发反例验证 |
| B14-F120-ABLATION | OBSERVED | 调查补证 | RES18, RES22, RES24 | 消融审计已生成但NO_MEASURED_ABLATION，不能称贡献成立；同target开关对照及配对增益/代价/区间原件 |
| B14-F140-IMPROVEMENT | OBSERVED | 修复 | RES07, RES23, RES25 | 计划已生成且未改生产参数，但binding/input/quality未齐；原调度器真实执行计划产物与独立评价后合法消费 |
| B14-F010-DATA | PASS | 保留回归 | RT05 | 346→347精确下一期已解析，此PASS必须保留；同样例及跨日/非法/缺口边界回归，保留唯一resolver |

### RSEC-075 · 【第16章 根因依赖DAG】

**原health：** `PASS`；**原domain_outcome：** `PASS`；**处置：** 保留回归；**关联：** RT20, RT21

**具体覆盖：** PASS证明列出的诊断依赖边可解析，不证明所有根因已复现或节点问题修好。

**验收方向：** 保留有向无环/唯一节点/引用测试；新施工依赖需独立校核，writer排空不依赖被阻断研究owner。

**原件定位：** `REPORT_DETAILS.txt` 第81行；../evidence/REPORT_CHECKS_96.json#RSEC-075

### RSEC-076 · 【第17章 身份、玩法与REAL不可变红线】

**原health：** `PASS`；**原domain_outcome：** `PASS`；**处置：** 保留回归；**关联：** PLAN-SOURCE-04

**具体覆盖：** 原PASS限于身份、玩法、唯一owner与禁止项合同；MODEL_PROVEN=false仍然有效。

**验收方向：** 固定K3、单位置、每期最多一REAL及无追单/马丁/补频率回归；不解除唯一证书门。

**原件定位：** `REPORT_DETAILS.txt` 第82行；../evidence/REPORT_CHECKS_96.json#RSEC-076

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-077 · 【第18章 诊断编排确定性与完整性校验】

**原health：** `PASS`；**原domain_outcome：** `PASS`；**处置：** 保留回归；**关联：** RT20, RT23

**具体覆盖：** bundleValid/fixedOrder/uniqueIds/hash/acyclic/readOnly通过，是诊断包完整性而非设备运行效果。

**验收方向：** 保持确定性/引用hash/只读和损坏反例；设备、模型、自然质量另有receipt。

**原件定位：** `REPORT_DETAILS.txt` 第83行；../evidence/REPORT_CHECKS_96.json#RSEC-077

### RSEC-078 · 【第19章 R811结算、证据、SHADOW与Research统一真值】

**原health：** `FAIL`；**原domain_outcome：** `FAIL`；**处置：** 修复；**关联：** RT01, RT03, RT05, RT06, RT07, RT11

**具体覆盖：** 原FAIL含核心PASS/full PENDING、近30缺覆盖、5旧WAIT_DRAW、研究窗口背压和FTRL/发明链局部状态。

**验收方向：** 分开工程已有成果和未运行链，逐旧WAIT_DRAW查精确draw；恢复capture/settlement/learn/deferred闭环并保持原件。

**原件定位：** `REPORT_DETAILS.txt` 第84行；../evidence/REPORT_CHECKS_96.json#RSEC-078

### RSEC-079 · 【自主策略发现/深度学习/动态进化】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES05, RES07, RES08, RES20, RES21, RES22

**具体覆盖：** 原定义要求原始历史→原语/公式/程序搜索→冻结→OOS，但算法发明NOT_STARTED、捕获/结算0，维护review不抵充。

**验收方向：** 实际生成具有独立状态/score/update/failure域的可执行结构，训练→变异/融合→配对评价→合格消费完整留证。

**原件定位：** `REPORT_DETAILS.txt` 第85行；../evidence/REPORT_CHECKS_96.json#RSEC-079

### RSEC-301 · 【真实性边界】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 保留回归；**关联：** RT20, RT22, RT23

**具体覆盖：** 真实性边界是只读诊断且事实/假设/构建合同/设备证明分开；不是业务功能测量。

**验收方向：** 所有新增诊断继续给范围和证据等级，不写运行副作用、不将推断升级为实证。

**原件定位：** `REPORT_DETAILS.txt` 第86行；../evidence/REPORT_CHECKS_96.json#RSEC-301

### RSEC-302 · 【报告哈希与完整结束标志】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 保留回归；**关联：** RT20, RT23

**具体覆盖：** 报告身份由每成员/分卷manifest SHA256绑定，旧字符计数不是完整性依据。

**验收方向：** 正常/损坏/取消导出验证manifest、part和member完整；DIAGNOSTIC与RAW范围不混淆。

**原件定位：** `REPORT_DETAILS.txt` 第87行；../evidence/REPORT_CHECKS_96.json#RSEC-302

### RSEC-303 · 【END_OF_LIVE_DIAGNOSTIC_REPORT】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 不适用并说明；**关联：** RT20

**具体覆盖：** END_OF_LIVE标记由96注册与manifest footer表达，不是一个运行模块或额外失败。

**验收方向：** 检查footer/精确记录数和96ID全到，不能仅凭出现END字符串接受截断包。

**原件定位：** `REPORT_DETAILS.txt` 第88行；../evidence/REPORT_CHECKS_96.json#RSEC-303

### RSEC-304 · 【18B.R922运行闭环/影子结算/Research/REAL硬门唯一真值】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 不适用并说明；**关联：** RT20, RT22

**具体覆盖：** 本项是305–310组合目录，不额外产生运行、训练或质量样本。

**验收方向：** 各子章节保留独立分母/身份并可定位原件；组合渲染不双计证据。

**原件定位：** `REPORT_DETAILS.txt` 第89行；../evidence/REPORT_CHECKS_96.json#RSEC-304

### RSEC-305 · 【R923运行闭环唯一真值】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RT05, RT06, RT07, RT11, RT12

**具体覆盖：** 终态4840/5000、开放160、预测义务1858/1858和无预测排除2982是不同分母，deferred1632且研究结算写失败。

**验收方向：** 所有开放/无预测/既有预测逐类对账；后台能力与持续验收分开，核心结算不被研究失败回滚。

**原件定位：** `REPORT_DETAILS.txt` 第90行；../evidence/REPORT_CHECKS_96.json#RSEC-305

### RSEC-306 · 【R923严格Future OOS统计】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 保留回归；**关联：** RT06, RES25, RES30

**具体覆盖：** 已有1863 shadow/1858结算/594命中真实可保留；99.73%只为已建预测结算率、校准样本0。

**验收方向：** 命中÷已结算、时间排序、SHADOW/REAL分账不变；独立报告全开奖覆盖和模型版本质量。

**原件定位：** `REPORT_DETAILS.txt` 第91行；../evidence/REPORT_CHECKS_96.json#RSEC-306

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-307 · 【R923深度学习・动态迭代・算法创造唯一真值】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES07, RES08, RES11, RES23, RES24, RES26

**具体覆盖：** 14阶段漏斗Created1/Registered0/Training0/PostTrain25/Calibration0/Validation0/Predraw0/Future0，准确揭示业务前后段断开。

**验收方向：** 14阶段逐对象身份、输入/输出/原因/下一触发完整；训练更新25不代替封存未来或真实应用。

**原件定位：** `REPORT_DETAILS.txt` 第92行；../evidence/REPORT_CHECKS_96.json#RSEC-307

证据成员：`report-text-93d16cc73d6fa4daa74df267b7222c755f37836b7121fe5e4e5ae2d80889aa2f.txt`

### RSEC-308 · 【证书化主链贡献】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES26, ROOT-01

**具体覆盖：** NO_CERTIFIED_CONTRIBUTION_PRODUCTION_FALLBACK_100_PERCENT记录当前无证书化贡献，不等于旧主链应删除。

**验收方向：** 合法证书正例经选择器/DecisionGate产生真实权重与可核消费；无证书继续合法旧SHADOW，质量未达不强推。

**原件定位：** `REPORT_DETAILS.txt` 第93行；../evidence/REPORT_CHECKS_96.json#RSEC-308

### RSEC-309 · 【R923逐期预测主链贡献审计】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES23, RES26, RT22

**具体覆盖：** 重复researchDetail用于逐期贡献视图；正式future与贡献为0须明确，不计为两份独立实验。

**验收方向：** 每target绑定当前exact model/certificate/selector/bundle和实际影响；退出/拒绝原因可复核。

**原件定位：** `REPORT_DETAILS.txt` 第94行；../evidence/REPORT_CHECKS_96.json#RSEC-309

证据成员：`report-text-93d16cc73d6fa4daa74df267b7222c755f37836b7121fe5e4e5ae2d80889aa2f.txt`

### RSEC-310 · 【R923真实出手统计与硬门唯一真值】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 保留回归；**关联：** RES26, RES30, RT21

**具体覆盖：** 已结算REAL0、胜率N/A及NO_QUALIFIED_MODEL_PROOF是准确门控结果；空账dup0不能证明真实链已验收。

**验收方向：** 保留门控与分账，补合法/非法证书集成反例；频率目标不能转成REAL配额。

**原件定位：** `REPORT_DETAILS.txt` 第95行；../evidence/REPORT_CHECKS_96.json#RSEC-310

### RSEC-311 · 【R922为什么中/不中/不出手真值】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 不适用并说明；**关联：** RT20

**具体覆盖：** 此项仅why-card312–315组合索引，不是新结算或因果证据。

**验收方向：** 保留用户解释入口，子卡引用同target冻结/开奖事实而非新计算。

**原件定位：** `REPORT_DETAILS.txt` 第96行；../evidence/REPORT_CHECKS_96.json#RSEC-311

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-312 · 【为什么中】

**原health：** `OBSERVED`；**原domain_outcome：** `AVAILABLE`；**处置：** 保留回归；**关联：** ROOT-02

**具体覆盖：** 330期实际03属于冻结03/06/10，可验证命中；教师/趋势/关系描述是当期信息，不自动证明因果机制。

**验收方向：** 保存冻结3号、实际draw与raw未校准标记；解释词句可追源，新增因果归因须消融/反证。

**原件定位：** `REPORT_DETAILS.txt` 第97行；../evidence/REPORT_CHECKS_96.json#RSEC-312

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-313 · 【为什么不中】

**原health：** `OBSERVED`；**原domain_outcome：** `AVAILABLE`；**处置：** 保留回归；**关联：** RES20, ROOT-02

**具体覆盖：** 331期实际04不属于冻结03/06/10；已有失败解释和raw标记应保留，未中本身不是软件bug。

**验收方向：** 用真实失败推动可反证假设及新子代实验，不删未中记录、不事后重算预测解释。

**原件定位：** `REPORT_DETAILS.txt` 第98行；../evidence/REPORT_CHECKS_96.json#RSEC-313

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-314 · 【为什么不出手】

**原health：** `OBSERVED`；**原domain_outcome：** `AVAILABLE`；**处置：** 保留回归；**关联：** RES26

**具体覆盖：** 347的SHADOW拒绝是MODEL_NOT_PROVEN/NO_QUALIFIED_MODEL_PROOF，当前证书不足时正确。

**验收方向：** 保持下期独立评估和不追单；将拒绝原因关联缺失具体证书字段/当前版本，不以开关虚假解锁。

**原件定位：** `REPORT_DETAILS.txt` 第99行；../evidence/REPORT_CHECKS_96.json#RSEC-314

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-315 · 【逐期结算统计】

**原health：** `OBSERVED`；**原domain_outcome：** `AVAILABLE`；**处置：** 调查补证；**关联：** RT06, RT22

**具体覆盖：** 4840终态/160待闭环/1858预测已结算/2982无预测排除，和另一5旧WAIT_DRAW有不同集合范围。

**验收方向：** 给完整target集合与scope，分别核对当前热期、既有义务、旧缺源；无预测排除不从覆盖分母消失。

**原件定位：** `REPORT_DETAILS.txt` 第100行；../evidence/REPORT_CHECKS_96.json#RSEC-315

### RSEC-316 · 【18C.创造候选接入预测主链审计】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES23, RES25, RES26, ROOT-01

**具体覆盖：** 当前347/331贡献审计preflight已执行但NO_VALID_CERTIFICATE、applied0/weight0；实际消费者权限链缺件。

**验收方向：** 修正有效证书当前bundle/issued_before_capture/positive_weight/支持exact consumer；正例可用、失效反例拒绝。

**原件定位：** `REPORT_DETAILS.txt` 第101行；../evidence/REPORT_CHECKS_96.json#RSEC-316

### RSEC-317 · 【18C2.算法数量、归档原因、晋升淘汰与完整清单】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** ROOT-01, RES21, RES23, RES24

**具体覆盖：** 生命周期已有1算法/4创生/4方法且9NURSERY/1OBSERVING；文字120/360等权重阶梯与实际证书协议需统一。

**验收方向：** 三个登记体系分开；文案从当前域协议生成，真实晋升使用固定未来配对/证书，代数和创建数不当改善。

**原件定位：** `REPORT_DETAILS.txt` 第102行；../evidence/REPORT_CHECKS_96.json#RSEC-317

### RSEC-318 · 【18.报告完整性】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 不适用并说明；**关联：** RT20, RT23

**具体覆盖：** 报告完整性参考SCREPORT成员/分卷hash，原NOT_APPLICABLE不表示导出损坏。

**验收方向：** 保留完整hash链与范围声明；M17补独立验收收据，字符数不能代签。

**原件定位：** `REPORT_DETAILS.txt` 第103行；../evidence/REPORT_CHECKS_96.json#RSEC-318

### RSEC-319 · 【19.完整报告SHA256】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 不适用并说明；**关联：** RT20

**具体覆盖：** 完整报告SHA是最终manifestSha256，不另有运行检查或预测效果。

**验收方向：** 写完所有成员/part且核验后生成唯一manifest身份，读取相同字节可重算。

**原件定位：** `REPORT_DETAILS.txt` 第104行；../evidence/REPORT_CHECKS_96.json#RSEC-319

### RSEC-320 · 【END_OF_COMPLETE_REPORT】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 不适用并说明；**关联：** RT20

**具体覆盖：** 完整报告END由footer hash和精确记录数约束，属于封包协议引用。

**验收方向：** 截断、漏记录、错footer必须被检测；正常包不因缺旧文本END重复失败。

**原件定位：** `REPORT_DETAILS.txt` 第105行；../evidence/REPORT_CHECKS_96.json#RSEC-320

### RSEC-321 · 【20.全部创造算法与研究方法，含归档】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 保留回归；**关联：** RES05, RES21, RES23

**具体覆盖：** 全状态清单存在第41代历史研究种子、Future0；需保存旧定义/父代，不将其当当前41代改善。

**验收方向：** 全量未截断、历史/新证据分栏；新结构和真实代际变化按独立产物/评价验收。

**原件定位：** `REPORT_DETAILS.txt` 第106行；../evidence/REPORT_CHECKS_96.json#RSEC-321

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-322 · 【全部创造算法中文清单｜全状态｜无数量截断】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 保留回归；**关联：** RES05, RES21

**具体覆盖：** 全部算法清单与321的算法部分同源，单算法IDEA_WAIT_FUTURE_OOS且开发/未来为0。

**验收方向：** 旧源码/程序/父hash保留并可查；生成/训练/冻结有新收据前不提升生命周期。

**原件定位：** `REPORT_DETAILS.txt` 第107行；../evidence/REPORT_CHECKS_96.json#RSEC-322

成熟功能保留参考：RT24（保全卡，不另算软件故障）。

### RSEC-323 · 【全部研究方法｜含归档｜无数量截断】

**原health：** `OBSERVED`；**原domain_outcome：** `OBSERVED`；**处置：** 修复；**关联：** RES05, RES20, RES23, RES24

**具体覆盖：** 4个typed expression方法已有PARITY_PASS/exec1，但冻结/结算/质量未知；可执行不等于已未来验证。

**验收方向：** 保留合法表达式与parity，实际输入绑定/捕获/结算/配对完成后才发证；常量调权与新机制分开计数。

**原件定位：** `REPORT_DETAILS.txt` 第108行；../evidence/REPORT_CHECKS_96.json#RSEC-323

### RSEC-324 · 【21.全部资产、晋升、淘汰证据原始表】

**原health：** `NOT_APPLICABLE`；**原domain_outcome：** `N/A`；**处置：** 不适用并说明；**关联：** RT23, RT20

**具体覆盖：** DIAGNOSTIC包含96检查和全表schema/count，但raw_database_exported=0，不等同全原始表数据导出。

**验收方向：** 保留完整诊断模式；数据库/APK/模型原件需要RAW复现时另取且核hash，不伪造已附原始字节。

**原件定位：** `REPORT_DETAILS.txt` 第109行；../evidence/REPORT_CHECKS_96.json#RSEC-324






<!-- CURRENT_MAPPING_BEGIN -->
## 本轮施工编号对应

原问题ID用于保留审计来源；本表给出最终施工编号。保留项/参考项链接只说明责任，不表示它被判为新故障。

|原项|当前施工问题|批次|
|---|---|---|
|RSEC-001|[R342-004](../issues/R342-004.md), [R342-007](../issues/R342-007.md), [R342-010](../issues/R342-010.md), [R342-022](../issues/R342-022.md), [R342-039](../issues/R342-039.md)|B02, B03, B06, B08|
|RSEC-002|[R342-001](../issues/R342-001.md), [R342-053](../issues/R342-053.md), [R342-057](../issues/R342-057.md)|B01, B11, B12|
|RSEC-003|[R342-010](../issues/R342-010.md), [R342-022](../issues/R342-022.md), [R342-033](../issues/R342-033.md), [R342-043](../issues/R342-043.md), [R342-044](../issues/R342-044.md)|B03, B06, B08, B09|
|RSEC-004|[R342-022](../issues/R342-022.md), [R342-027](../issues/R342-027.md), [R342-032](../issues/R342-032.md), [R342-040](../issues/R342-040.md)|B06, B07, B08|
|RSEC-005|[R342-024](../issues/R342-024.md), [R342-025](../issues/R342-025.md), [R342-046](../issues/R342-046.md)|B07, B10|
|RSEC-006|[R342-045](../issues/R342-045.md), [R342-046](../issues/R342-046.md), [R342-047](../issues/R342-047.md), [R342-048](../issues/R342-048.md)|B10|
|RSEC-007|[R342-028](../issues/R342-028.md), [R342-034](../issues/R342-034.md), [R342-050](../issues/R342-050.md)|B07, B08, B11|
|RSEC-009|[R342-018](../issues/R342-018.md), [R342-053](../issues/R342-053.md), [R342-054](../issues/R342-054.md), [R342-056](../issues/R342-056.md)|B05, B11, B12|
|RSEC-010|[R342-008](../issues/R342-008.md), [R342-013](../issues/R342-013.md), [R342-055](../issues/R342-055.md)|B03, B04, B11|
|RSEC-011|[R342-010](../issues/R342-010.md), [R342-033](../issues/R342-033.md), [R342-055](../issues/R342-055.md)|B03, B08, B11|
|RSEC-012|[R342-004](../issues/R342-004.md), [R342-033](../issues/R342-033.md), [R342-044](../issues/R342-044.md), [R342-051](../issues/R342-051.md)|B02, B08, B09, B11|
|RSEC-013|[R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-040](../issues/R342-040.md)|B08|
|RSEC-014|[R342-025](../issues/R342-025.md), [R342-033](../issues/R342-033.md), [R342-040](../issues/R342-040.md)|B07, B08|
|RSEC-015|[R342-040](../issues/R342-040.md)|B08|
|RSEC-016|[R342-028](../issues/R342-028.md)|B07|
|RSEC-017|[R342-028](../issues/R342-028.md), [R342-039](../issues/R342-039.md)|B07, B08|
|RSEC-018|[R342-016](../issues/R342-016.md), [R342-018](../issues/R342-018.md), [R342-053](../issues/R342-053.md), [R342-055](../issues/R342-055.md)|B05, B11|
|RSEC-019|[R342-031](../issues/R342-031.md), [R342-043](../issues/R342-043.md), [R342-052](../issues/R342-052.md), [R342-056](../issues/R342-056.md)|B08, B09, B11, B12|
|RSEC-020|[R342-022](../issues/R342-022.md), [R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-043](../issues/R342-043.md), [R342-044](../issues/R342-044.md)|B06, B08, B09|
|RSEC-021|[R342-012](../issues/R342-012.md), [R342-013](../issues/R342-013.md), [R342-033](../issues/R342-033.md), [R342-044](../issues/R342-044.md)|B04, B08, B09|
|RSEC-022|[R342-004](../issues/R342-004.md), [R342-011](../issues/R342-011.md), [R342-015](../issues/R342-015.md), [R342-016](../issues/R342-016.md)|B02, B03, B05|
|RSEC-023|[R342-004](../issues/R342-004.md), [R342-033](../issues/R342-033.md), [R342-044](../issues/R342-044.md), [R342-055](../issues/R342-055.md)|B02, B08, B09, B11|
|RSEC-024|[R342-010](../issues/R342-010.md), [R342-022](../issues/R342-022.md), [R342-032](../issues/R342-032.md), [R342-033](../issues/R342-033.md)|B03, B06, B08|
|RSEC-025|[R342-032](../issues/R342-032.md), [R342-033](../issues/R342-033.md)|B08|
|RSEC-026|[R342-017](../issues/R342-017.md), [R342-019](../issues/R342-019.md)|B05|
|RSEC-027|[R342-002](../issues/R342-002.md)|B01|
|RSEC-028|[R342-001](../issues/R342-001.md), [R342-053](../issues/R342-053.md)|B01, B11|
|RSEC-029|[R342-012](../issues/R342-012.md), [R342-053](../issues/R342-053.md)|B04, B11|
|RSEC-030|[R342-012](../issues/R342-012.md), [R342-013](../issues/R342-013.md), [R342-014](../issues/R342-014.md), [R342-016](../issues/R342-016.md)|B04, B05|
|RSEC-031|[R342-012](../issues/R342-012.md), [R342-013](../issues/R342-013.md), [R342-014](../issues/R342-014.md), [R342-055](../issues/R342-055.md)|B04, B11|
|RSEC-032|[R342-007](../issues/R342-007.md), [R342-008](../issues/R342-008.md), [R342-018](../issues/R342-018.md)|B03, B05|
|RSEC-033|[R342-016](../issues/R342-016.md), [R342-019](../issues/R342-019.md)|B05|
|RSEC-034|[R342-033](../issues/R342-033.md), [R342-043](../issues/R342-043.md), [R342-051](../issues/R342-051.md), [R342-056](../issues/R342-056.md)|B08, B09, B11, B12|
|RSEC-035|[R342-013](../issues/R342-013.md), [R342-043](../issues/R342-043.md), [R342-044](../issues/R342-044.md), [R342-055](../issues/R342-055.md)|B04, B09, B11|
|RSEC-036|[R342-004](../issues/R342-004.md), [R342-054](../issues/R342-054.md)|B02, B11|
|RSEC-037|[R342-014](../issues/R342-014.md)|B04|
|RSEC-038|[R342-013](../issues/R342-013.md)|B04|
|RSEC-039|[R342-044](../issues/R342-044.md), [R342-056](../issues/R342-056.md)|B09, B12|
|RSEC-040|[R342-013](../issues/R342-013.md), [R342-044](../issues/R342-044.md), [R342-056](../issues/R342-056.md)|B04, B09, B12|
|RSEC-041|[R342-004](../issues/R342-004.md), [R342-008](../issues/R342-008.md), [R342-018](../issues/R342-018.md)|B02, B03, B05|
|RSEC-042|[R342-018](../issues/R342-018.md), [R342-019](../issues/R342-019.md)|B05|
|RSEC-043|[R342-001](../issues/R342-001.md)|B01|
|RSEC-044|[R342-053](../issues/R342-053.md), [R342-057](../issues/R342-057.md)|B11, B12|
|RSEC-048|[R342-012](../issues/R342-012.md)|B04|
|RSEC-052|[R342-004](../issues/R342-004.md), [R342-007](../issues/R342-007.md), [R342-008](../issues/R342-008.md), [R342-012](../issues/R342-012.md), [R342-013](../issues/R342-013.md), [R342-014](../issues/R342-014.md), [R342-016](../issues/R342-016.md)|B02, B03, B04, B05|
|RSEC-053|[R342-014](../issues/R342-014.md)|B04|
|RSEC-054|[R342-014](../issues/R342-014.md), [R342-043](../issues/R342-043.md), [R342-052](../issues/R342-052.md)|B04, B09, B11|
|RSEC-055|[R342-014](../issues/R342-014.md), [R342-050](../issues/R342-050.md), [R342-052](../issues/R342-052.md)|B04, B11|
|RSEC-056|[R342-014](../issues/R342-014.md), [R342-052](../issues/R342-052.md)|B04, B11|
|RSEC-057|[R342-014](../issues/R342-014.md)|B04|
|RSEC-058|[R342-031](../issues/R342-031.md), [R342-033](../issues/R342-033.md), [R342-052](../issues/R342-052.md)|B08, B11|
|RSEC-059|[R342-013](../issues/R342-013.md), [R342-033](../issues/R342-033.md), [R342-043](../issues/R342-043.md), [R342-044](../issues/R342-044.md)|B04, B08, B09|
|RSEC-060|[R342-044](../issues/R342-044.md), [R342-054](../issues/R342-054.md), [R342-056](../issues/R342-056.md)|B09, B11, B12|
|RSEC-061|[R342-039](../issues/R342-039.md), [R342-052](../issues/R342-052.md), [R342-055](../issues/R342-055.md)|B08, B11|
|RSEC-062|[R342-003](../issues/R342-003.md), [R342-039](../issues/R342-039.md), [R342-052](../issues/R342-052.md)|B02, B08, B11|
|RSEC-063|[R342-052](../issues/R342-052.md), [R342-055](../issues/R342-055.md), [R342-057](../issues/R342-057.md)|B11, B12|
|RSEC-064|[R342-003](../issues/R342-003.md), [R342-004](../issues/R342-004.md), [R342-052](../issues/R342-052.md), [R342-053](../issues/R342-053.md)|B02, B11|
|RSEC-065|[R342-003](../issues/R342-003.md), [R342-004](../issues/R342-004.md), [R342-007](../issues/R342-007.md), [R342-012](../issues/R342-012.md), [R342-039](../issues/R342-039.md)|B02, B03, B04, B08|
|RSEC-066|[R342-039](../issues/R342-039.md), [R342-043](../issues/R342-043.md), [R342-052](../issues/R342-052.md)|B08, B09, B11|
|RSEC-067|[R342-034](../issues/R342-034.md), [R342-041](../issues/R342-041.md), [R342-050](../issues/R342-050.md), [R342-052](../issues/R342-052.md)|B08, B11|
|RSEC-068|[R342-034](../issues/R342-034.md), [R342-041](../issues/R342-041.md), [R342-050](../issues/R342-050.md)|B08, B11|
|RSEC-069|[R342-014](../issues/R342-014.md), [R342-043](../issues/R342-043.md), [R342-052](../issues/R342-052.md), [R342-054](../issues/R342-054.md)|B04, B09, B11|
|RSEC-070|[R342-001](../issues/R342-001.md), [R342-052](../issues/R342-052.md), [R342-054](../issues/R342-054.md)|B01, B11|
|RSEC-071|[R342-001](../issues/R342-001.md), [R342-054](../issues/R342-054.md)|B01, B11|
|RSEC-072|[R342-022](../issues/R342-022.md), [R342-031](../issues/R342-031.md), [R342-033](../issues/R342-033.md), [R342-043](../issues/R342-043.md), [R342-044](../issues/R342-044.md)|B06, B08, B09|
|RSEC-073|[R342-022](../issues/R342-022.md), [R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-043](../issues/R342-043.md)|B06, B08, B09|
|RSEC-074|[R342-012](../issues/R342-012.md), [R342-013](../issues/R342-013.md), [R342-014](../issues/R342-014.md), [R342-043](../issues/R342-043.md), [R342-044](../issues/R342-044.md), [R342-052](../issues/R342-052.md), [R342-054](../issues/R342-054.md)|B04, B09, B11|
|RSEC-075|[R342-053](../issues/R342-053.md), [R342-054](../issues/R342-054.md)|B11|
|RSEC-076|[R342-002](../issues/R342-002.md)|B01|
|RSEC-077|[R342-053](../issues/R342-053.md), [R342-057](../issues/R342-057.md)|B11, B12|
|RSEC-078|[R342-004](../issues/R342-004.md), [R342-007](../issues/R342-007.md), [R342-008](../issues/R342-008.md), [R342-012](../issues/R342-012.md), [R342-013](../issues/R342-013.md), [R342-014](../issues/R342-014.md)|B02, B03, B04|
|RSEC-079|[R342-010](../issues/R342-010.md), [R342-022](../issues/R342-022.md), [R342-025](../issues/R342-025.md), [R342-039](../issues/R342-039.md), [R342-040](../issues/R342-040.md), [R342-041](../issues/R342-041.md)|B03, B06, B07, B08|
|RSEC-301|[R342-053](../issues/R342-053.md), [R342-055](../issues/R342-055.md), [R342-057](../issues/R342-057.md)|B11, B12|
|RSEC-302|[R342-053](../issues/R342-053.md), [R342-057](../issues/R342-057.md)|B11, B12|
|RSEC-303|[R342-053](../issues/R342-053.md)|B11|
|RSEC-304|[R342-053](../issues/R342-053.md), [R342-055](../issues/R342-055.md)|B11|
|RSEC-305|[R342-008](../issues/R342-008.md), [R342-012](../issues/R342-012.md), [R342-013](../issues/R342-013.md), [R342-014](../issues/R342-014.md), [R342-018](../issues/R342-018.md)|B03, B04, B05|
|RSEC-306|[R342-013](../issues/R342-013.md), [R342-043](../issues/R342-043.md), [R342-056](../issues/R342-056.md)|B04, B09, B12|
|RSEC-307|[R342-010](../issues/R342-010.md), [R342-022](../issues/R342-022.md), [R342-032](../issues/R342-032.md), [R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-044](../issues/R342-044.md)|B03, B06, B08, B09|
|RSEC-308|[R342-044](../issues/R342-044.md), [R342-051](../issues/R342-051.md)|B09, B11|
|RSEC-309|[R342-033](../issues/R342-033.md), [R342-044](../issues/R342-044.md), [R342-055](../issues/R342-055.md)|B08, B09, B11|
|RSEC-310|[R342-044](../issues/R342-044.md), [R342-054](../issues/R342-054.md), [R342-056](../issues/R342-056.md)|B09, B11, B12|
|RSEC-311|[R342-053](../issues/R342-053.md)|B11|
|RSEC-312|[R342-052](../issues/R342-052.md)|B11|
|RSEC-313|[R342-039](../issues/R342-039.md), [R342-052](../issues/R342-052.md)|B08, B11|
|RSEC-314|[R342-044](../issues/R342-044.md)|B09|
|RSEC-315|[R342-013](../issues/R342-013.md), [R342-055](../issues/R342-055.md)|B04, B11|
|RSEC-316|[R342-033](../issues/R342-033.md), [R342-043](../issues/R342-043.md), [R342-044](../issues/R342-044.md), [R342-051](../issues/R342-051.md)|B08, B09, B11|
|RSEC-317|[R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-040](../issues/R342-040.md), [R342-051](../issues/R342-051.md)|B08, B11|
|RSEC-318|[R342-053](../issues/R342-053.md), [R342-057](../issues/R342-057.md)|B11, B12|
|RSEC-319|[R342-053](../issues/R342-053.md)|B11|
|RSEC-320|[R342-053](../issues/R342-053.md)|B11|
|RSEC-321|[R342-025](../issues/R342-025.md), [R342-033](../issues/R342-033.md), [R342-040](../issues/R342-040.md)|B07, B08|
|RSEC-322|[R342-025](../issues/R342-025.md), [R342-040](../issues/R342-040.md)|B07, B08|
|RSEC-323|[R342-025](../issues/R342-025.md), [R342-033](../issues/R342-033.md), [R342-034](../issues/R342-034.md), [R342-039](../issues/R342-039.md)|B07, B08|
|RSEC-324|[R342-053](../issues/R342-053.md), [R342-057](../issues/R342-057.md)|B11, B12|

<!-- CURRENT_MAPPING_END -->
