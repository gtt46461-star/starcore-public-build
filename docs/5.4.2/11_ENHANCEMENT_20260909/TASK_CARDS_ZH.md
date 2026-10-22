# 30张后六批增强施工卡

所有卡为DESIGN_NOT_IMPLEMENTED；原任务全部步骤继续有效。每卡必须提供调用者/消费者、DB/线程/Owner、前后SHA、真实状态和持久化、正常及至少两个可失败负例、重启/回退、命令退出码。文档不计施工完成。

## E02A · 第2批 · 端到端期限与资源隔离

原任务：B05 / B10；依赖：

源码：`app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java`, `app/src/main/java/com/ethk3/novaonepro/RealtimePressureGateR811.java`, `app/src/main/java/com/ethk3/novaonepro/BacklogConvergenceR925.java`

生产接线及实现合同：原请求经唯一SingleFlight到durable；各段单调钟deadline、全attempt、完整缓存键、连续FULL水位/洞/净吞吐；研究取消点目标100ms、DB分片50ms，资源上限同机预冻结

验收：

- 120期P95总延迟≤5000ms/compute≤3000ms
- 64苗圃和2216积压并发无洞不拖慢
- 阻塞SQL/取消/GC不删失败慢样本

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E02A/`

## E02B · 第2批 · 可信来源与时钟治理

原任务：B07 / B08；依赖：

源码：`app/src/main/java/com/ethk3/novaonepro/FetchLedgerR925.java`, `app/src/main/java/com/ethk3/novaonepro/ApiTransportGovernanceEngine.java`, `app/src/main/java/com/ethk3/novaonepro/RuntimeRecoveryCoordinatorR926.java`

生产接线及实现合同：provider→请求账本→规范化/隔离→outbox；source/request/payload hash与首次available_at、draw_at、clock_epoch；晚到更正追加版本不倒填赛前

验收：

- 503/脏200/乱序/冲突/时钟跳变有原因码
- 晚到不可成为历史赛前特征
- 用户停止不可复活

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E02B/`

## E02C · 第2批 · 可重放学习状态链

原任务：B09 / B10；依赖：E02B

源码：`app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchTinyDeepLearnerR909.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchAdaptiveLearnerStoreR811.java`

生产接线及实现合同：结算事件→学习outbox→旧状态→原子提交新状态和游标→下一期；幂等键game/issue/update_contract/label_version/input_hash；pre/post_state_hash和trained_until

验收：

- 每个边界kill重启/双worker/重复10次等于单次状态
- 当前期预测不被学习倒写
- 乱序/纠正标签按冻结规则重建或拒绝

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E02C/`

## E02D · 第2批 · 唯一Gate证据包

原任务：B11；依赖：E02C

源码：`app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java`, `app/src/main/java/com/ethk3/novaonepro/TargetIssueResolver.java`, `app/src/main/java/com/ethk3/novaonepro/ControlledCandidateOpportunityAuditR922.java`

生产接线及实现合同：最终选择/有效贡献→原唯一Gate→不可变REAL；绑定data/feature/model/state/selection/calibration/cert/budget hash和实际配置；受控未证明与MODEL_PROVEN分开

验收：

- 缺来源/失效证书不得旁路
- 每期仅单位置K3一个REAL
- 高分/频率不足不得触发补单

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E02D/`

## E03A · 第3批 · 统一PIT特征DAG

原任务：D01 / D02；依赖：E02B,E02C

源码：`app/src/main/java/com/ethk3/novaonepro/TrainingSnapshotExportR925.java`, `app/src/main/java/com/ethk3/novaonepro/GenesisAssetVmR915.java`, `app/src/main/java/com/ethk3/novaonepro/V51ActionMetaFeatureEngine.java`

生产接线及实现合同：raw过去窗口→typed图→专家/NumberQ→META1200→排序；节点semantic/type/unit/shape/parents/max_issue/available_at/mask/reason/cost/hash；label独立join

验收：

- 100期逐字段三端一致
- 同形状错语义/未来label/环被拒
- 合法0不判missing，legacy不填0升级

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E03A/`

## E03B · 第3批 · 嵌套时间验证与全试验登记

原任务：D03 / C06；依赖：E03A

源码：`training/r925/pipeline.py`, `training/r925/research_governance.py`, `app/src/main/java/com/ethk3/novaonepro/ResearchFamilyRegistryR925.java`

生产接线及实现合同：过去rolling OOF→TRAIN/TUNE/CAL/POLICY/SEALED/FUTURE；按标签可用性purge/embargo；数据内容hash/源码/超参/seed/族/预算全登记；sealed读取令牌先原子消费

验收：

- 改名/复制路径/拆族/归档不恢复sealed权限
- 上游变化下游失效
- 泄漏trap被阻断且全部失败试验保留

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E03B/`

## E03C · 第3批 · 深度表征和模型族公平比较

原任务：D04 / D05；依赖：E03B,E02A

源码：`app/src/main/java/com/ethk3/novaonepro/DeepCandidateRuntimeR925.java`, `app/src/main/java/com/ethk3/novaonepro/V51ExpertGateEngine.java`, `app/src/main/java/com/ethk3/novaonepro/V51ActionRankerEngine.java`, `training/r925/deep_candidates.py`

生产接线及实现合同：D0小MLP/D1残差/D2快慢/D3蒸馏、9专家与R0–R5同fold/同成本；多尺度/矩阵交互/OOF残差为登记消融轴；teacher仅用合法过去；完整1200评分与固定tie-break

验收：

- 1200唯一动作360正标签仅一issue
- 100期rank1一致且abs1e-8/rel1e-6
- 无增益选简单基线，不可部署记录失败

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E03C/`

## E03D · 第3批 · 选择后校准与联合质量门

原任务：D06 / D07 / C03；依赖：E03C,E02D

源码：`app/src/main/java/com/ethk3/novaonepro/V51SelectedActionCalibrationEngine.java`, `app/src/main/java/com/ethk3/novaonepro/PromotionWindowR925.java`, `training/r925/pipeline.py`

生产接线及实现合同：最终consumer/selection/混合权重冻结后CAL；固定箱support/区间，Brier/NLL对champion、常数0.3和简单模型配对；同Future联合HR.40/频率.15/ECE.04

验收：

- raw score换尺度不得伪造概率
- 选择权重变化旧cal拒绝
- 零出手/低支持/缩分母不能确认效果

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E03D/`

## E03E · 第3批 · 漂移与不确定性分解

原任务：D07 / D08；依赖：E03D

源码：`app/src/main/java/com/ethk3/novaonepro/AlgorithmRegimeEvidenceR923.java`, `app/src/main/java/com/ethk3/novaonepro/V51StateTrajectoryEngine.java`, `app/src/main/java/com/ethk3/novaonepro/CertifiedResearchMixerR915.java`

生产接线及实现合同：过去特征/mask/残差/校准监测→域支持→PAUSE/新观察；TUNE冻结阈值/参考窗/误报预算/冷却；设备故障与game regime分离；分歧/扰动/域支持分列

验收：

- 突变/渐变/短噪声/断网有不同状态
- 未知域0新贡献
- conformal仅诊断，不扩大K或保证单期

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E03E/`

## E03F · 第3批 · 双记忆持续学习

原任务：D04 / D08 / D10；依赖：E03B,E03E,E02C

源码：`app/src/main/java/com/ethk3/novaonepro/ResearchTinyDeepLearnerR909.java`, `app/src/main/java/com/ethk3/novaonepro/ModelChampionChallengerRegistry.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchArtifactCodecR811.java`

生产接线及实现合同：已结游标→近期+长期replay→有界更新→checkpoint→下一期state；预冻结学习率/梯度/步数/seed/初始化/回退；静态与adaptive contract分别评价

验收：

- A→B→A报告学习和遗忘
- 重启10次state hash一致
- 坏权重/当前label/人工改规则拒绝旧证书

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E03F/`

## E03G · 第3批 · 证书依赖闭包与原子激活

原任务：C01 / C06 / D10；依赖：E03D,E03F

源码：`app/src/main/java/com/ethk3/novaonepro/ResearchCertificateR906.java`, `app/src/main/java/com/ethk3/novaonepro/AlgorithmProductionGovernorR915.java`, `app/src/main/java/com/ethk3/novaonepro/CertifiedResearchMixerR915.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchMainlineGovernorR903.java`

生产接线及实现合同：一合同验证三消费者，绑定原子图/model/policy/cal/domain/trial/selection/code；验证完整包后下一期换指针；撤销向依赖闭包传播

验收：

- 三消费者同向量授权一致
- 错签名/路径/缺依赖/中途kill拒绝或原子恢复
- 当前期不可半旧半新

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E03G/`

## E04A · 第4批 · 原子因子分子类型图

原任务：C10 / C11 / C12；依赖：E03A,E03G

源码：`app/src/main/java/com/ethk3/novaonepro/GenesisSchemaRegistryR915.java`, `app/src/main/java/com/ethk3/novaonepro/GenesisAssetVmR915.java`, `app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeVmR908.java`, `app/src/main/java/com/ethk3/novaonepro/AtomQualityLedgerR925.java`

生产接线及实现合同：保留240项语义；原子→typed因子表达式→分子子图/有界状态→策略VM；声明类型/单位/时态/mask/来源/成本/父hash；规范化结构与CSE

验收：

- 全生产原子有消费者与逐项耗时
- 未来依赖/环/无界/NaN/非法类型拒绝
- 新分子可反解全部父原子

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E04A/`

## E04B · 第4批 · 全矩阵共同快照与反哺

原任务：C09 / C10 / C11；依赖：E04A

源码：`app/src/main/java/com/ethk3/novaonepro/TeacherMatrixEngine.java`, `app/src/main/java/com/ethk3/novaonepro/CausalFactorMatrixEngine.java`, `app/src/main/java/com/ethk3/novaonepro/RuleHealthMatrixEngine.java`, `app/src/main/java/com/ethk3/novaonepro/DynamicFreezeMatrixEngine.java`

生产接线及实现合同：调用图补齐全部趋势/形态/关系/教师/因果反推/冻结矩阵入口；同snapshot输出score_kind/domain/support/cost/status；消融→repair→新证书→未来贡献

验收：

- 旧入口与固定fixture逐一回归
- 不同快照混合拒绝
- 真实关闭矩阵重跑有结果，负对照不得凭名称获权

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E04B/`

## E04C · 第4批 · 真实消融与最小修复

原任务：C08 / C09；依赖：E04B

源码：`app/src/main/java/com/ethk3/novaonepro/ResearchAblationR815.java`, `app/src/main/java/com/ethk3/novaonepro/FeatureAblationAttributionEngine.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchFailureHypothesisEngineR811.java`, `app/src/main/java/com/ethk3/novaonepro/LifecycleAuditR925.java`

生产接线及实现合同：已结失败按采集/执行/选位/边界/校准/域/冲突/随机残差归因；真实模块干预→目的/证伪条件/预算→最小child→新Future；计算干预不冒充机制因果

验收：

- 去FTRL/TinyDeep/Teacher/Trend/Shape/Relation/Invented真实重执行
- 父200样本child仍0
- 失败/pending保留，历史hash不变

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E04C/`

## E04D · 第4批 · 多样性和残差互补搜索

原任务：C05 / C07 / C12；依赖：E04A,E04C,E03B

源码：`app/src/main/java/com/ethk3/novaonepro/ResearchNovelFormulaSynthesizerR903.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchMethodGraphSynthesizerR903.java`, `app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchFamilyRegistryR925.java`

生产接线及实现合同：参数/结构/新拓扑分别登记可执行DSL和变异；结构/行为/过去OOF残差去重；增益/稳定/互补/成本/复杂度Pareto；8/24/64公平调度，所有生成计假设预算

验收：

- 同结构改名不占双active
- 同seed可复建
- 代理证据不晋级，无信号可全部失败，预算耗尽不追求凑通过

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E04D/`

## E04E · 第4批 · 公平配对与渐进晋级

原任务：C02 / C04 / C08；依赖：E04D,E03G

源码：`app/src/main/java/com/ethk3/novaonepro/PromotionWindowR925.java`, `app/src/main/java/com/ethk3/novaonepro/CaptureOpportunityR925.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchPredrawCaptureR923.java`, `app/src/main/java/com/ethk3/novaonepro/AlgorithmEvolutionLifecycleR912.java`

生产接线及实现合同：共同机会→两者赛前冻结→固定champion配对；保留candidate-only/champion-only/both-missing及敏感性；原FWER、L4、新120/240/240/240/360窗、0/5/12/20/30%及更严格旧cap

验收：

- 冻结前后/事务中断分母不丢
- 变更基线/域/选择旧窗失效
- 只保留赢家/重复look/失败窗复用拒绝

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E04E/`

## E04F · 第4批 · 频率质量可行前沿

原任务：D09 / C07；依赖：E04E,E03D

源码：`app/src/main/java/com/ethk3/novaonepro/V51PredictabilityArtifact.java`, `app/src/main/java/com/ethk3/novaonepro/V51PolicySurfaceArtifact.java`, `app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java`, `app/src/main/java/com/ethk3/novaonepro/ControlledCandidateOpportunityAuditR922.java`

生产接线及实现合同：POLICY选一条冻结政策→SEALED/FUTURE→原Gate；计划/合法可用/选中/REAL分母分列；机会不足归因数据/信号/支持/资源，不降低质量

验收：

- 同窗HR≥.40/频率≥.15/ECE≤.04才点值达标
- 联合置信不足保持不足
- 零机会/连错不追单加倍补债

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E04F/`

## E04G · 第4批 · 贡献图与隔离恢复

原任务：C04 / C08 / C10 / C11；依赖：E04B,E04C,E04E

源码：`app/src/main/java/com/ethk3/novaonepro/AtomQualityLedgerR925.java`, `app/src/main/java/com/ethk3/novaonepro/CertifiedResearchMixerR915.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchMainlineContributionAuditR922.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchLearningContinuityR912.java`

生产接线及实现合同：构件→候选→证书→选择→结算→贡献/失败→新研究；记录真实权重/排序变化/依赖hash；ACTIVE/WATCH/SHADOW_ONLY/QUARANTINED，恢复需新证据

验收：

- 隔离原子后代下一期贡献归零
- 撤权重启projection一致
- 无证据不能显示0而实际参与排序

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E04G/`

## E05A · 第5批 · 四能力页与旧功能可达

原任务：U01 / U02；依赖：E04G

源码：`app/src/main/java/com/ethk3/novaonepro/MainActivity.java`, `app/src/main/java/com/ethk3/novaonepro/ModelLearningActivity.java`, `app/src/main/java/com/ethk3/novaonepro/ResearchDashboardR924.java`, `app/src/main/java/com/ethk3/novaonepro/OperationalDashboardR925.java`

生产接线及实现合同：四独立入口同一只读watermark，展示正在做什么/支持/改善/主链贡献/阻断；原矩阵页全部保留

验收：

- 5秒可找证据与原因
- phone/sw600dp/最大字体/旋转/长名单通过
- 空样本不默认绿灯100%

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E05A/`

## E05B · 第5批 · 胜率品质与稳定证据面板

原任务：U01 / U03；依赖：E04F

源码：`app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java`, `app/src/main/java/com/ethk3/novaonepro/MainActivity.java`, `app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java`

生产接线及实现合同：HR/配对delta/Brier/NLL/ECE/coverage/域支持与时延/掉期/重复/ANR分列；点值、区间、n、分母、假设、窗口、版本可钻取

验收：

- 180选中与1200计划不混
- 未知/低支持/依赖假设不足明确不足
- 历史连错不冒充未来上限

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E05B/`

## E05C · 第5批 · 赛前赛后解释边界

原任务：U03；依赖：E04C

源码：`app/src/main/java/com/ethk3/novaonepro/FeatureAblationAttributionEngine.java`, `app/src/main/java/com/ethk3/novaonepro/WhyTruthSectionRendererR906.java`, `app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java`

生产接线及实现合同：赛前理由→赛后追加诊断→未来修复意图三时态；关联真实消融和冻结hash；区分计算敏感性/关联/假说

验收：

- 未开奖/无赛前/待结/缺失/失败都有fixture
- 后验替换特征不改历史hash
- 重复诊断不双学

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E05C/`

## E05D · 第5批 · 一致报告与实际导出

原任务：U04；依赖：E05A,E05B,E05C

源码：`app/src/main/java/com/ethk3/novaonepro/V57CompleteDiagnosticReportEngine.java`, `app/src/main/java/com/ethk3/novaonepro/DiagnosticTextExportR923.java`, `app/src/main/java/com/ethk3/novaonepro/DiagnosticReportProviderR923.java`, `app/src/main/java/com/ethk3/novaonepro/TrainingSnapshotExportR925.java`

生产接线及实现合同：同snapshot_id/watermark生成TXT/JSON/CSV，含42任务30卡、全部试验、图/证书/贡献/故障；分章节错误标PARTIAL；原始凭据不进报告

验收：

- 三格式逐期一致
- 手机长报告真分享打开和hash复算
- 删章/假PASS/空间不足/进程中断不可误当完整

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E05D/`

## E06A · 第6批 · 属性变形和故障回归

原任务：Q01；依赖：E05D,E04G

源码：`scripts/r926_migration_fault_harness.py`, `scripts/r926_event_ledger_harness.py`, `verification/r925/R925PolicyHarness.java`

生产接线及实现合同：真实Owner/SQLite边界执行→kill/并发/重放/乱序/分片/变形断言，永久保留B06先durable outbox再投影；每卡正常+至少两负例

验收：

- 禁用关键校验对应测试必失败
- 零测试/跳过不可PASS
- 新接口有真实消费者、状态变化和重启证据

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E06A/`

## E06B · 第6批 · 误晋级和泄漏对抗

原任务：Q01；依赖：E04E

源码：`training/r925/research_governance.py`, `verification/r925/verify_training.py`, `training/r925/pipeline.py`

生产接线及实现合同：独立噪声/相关候选/时间依赖/突变/泄漏trap→完整选择和证书；轻量治理≥10000模拟并报告FWER及95%区间；完整训练另列有界种子

验收：

- 重复look/改名/拆族/复制行/复用sealed被识别
- 故意bug能暴露
- 假设不成立不可凭模拟称任意依赖有效

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E06B/`

## E06C · 第6批 · 三端全状态Host准备

原任务：Q01；依赖：E03F

源码：`verification/r925/verify_parity.py`, `verification/r925/DeepParityHarness.java`, `verification/r925/RankerParityHarness.java`

生产接线及实现合同：冻结100期raw/mask/Q/Gate/Sinkhorn/META1200/rank/selection/cal/state向量；abs1e-8/rel1e-6、rank1完全一致；极值/错语义/重启向量

验收：

- Python/Host符合合同
- 错hash/非有限/schema负例拒绝
- Android栏NOT_RUN，Host不能关闭D11

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E06C/`

## E06D · 第6批 · 源码APK方案与包同源门

原任务：Q01；依赖：E06A,E06B,E06C

源码：`scripts/r926_build_release.sh`, `scripts/r925_build_total.sh`, `scripts/r926_apk_acceptance.sh`

生产接线及实现合同：source/tree/build_input/plan/APK/cert/CI/package绑定；当前展开源码及全部原资产继承；分片顺序/尺寸/逐片和总hash；同commit CI后预发行

验收：

- 缺片/旧APK/旧计划/输入漂移拒绝
- 干净环境可重建
- 任一必需门未过不得发布完成版

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E06D/`

## E07A · 第7批 · 真机覆盖与连续运行

原任务：Q02；依赖：E06D

源码：`app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`, `app/src/main/java/com/ethk3/novaonepro/RuntimeRecoveryCoordinatorR926.java`, `app/src/main/java/com/ethk3/novaonepro/MainActivity.java`

生产接线及实现合同：同签名覆盖→历史hash→冷启动10次→亮屏120/息屏120/划掉30→网络/授权/用户停止/重启→24h；记录OS/设备/热/内存/权限

验收：

- 正常可运行场景掉期/重复/崩溃/ANR/OOM均0
- 停止不复活
- 5秒SLO/FULL连续水位与旧账门通过

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E07A/`

## E07B · 第7批 · 真实Future联合效果

原任务：Q03；依赖：E07A,E04F

源码：`training/r925/pipeline.py`, `app/src/main/java/com/ethk3/novaonepro/PromotionWindowR925.java`, `app/src/main/java/com/ethk3/novaonepro/ModelEffectivenessAnalyzerR35.java`

生产接线及实现合同：真实数据全部阶段冻结后新Future；原10000期首轮规划加实际出手率/依赖/效应/alpha的预注册样本和最大日历；共同验证HR/coverage/ECE/相对基线/域稳定

验收：

- 真实合格候选至少一名且有合法贡献才关闭创造效果
- 旧sealed/模拟/后补赛前不得证明
- 未达FAIL_EFFECT，低支持不足

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E07B/`

## E07C · 第7批 · Android三端全状态一致

原任务：D11；依赖：E07A,E06C

源码：`verification/r925/verify_parity.py`, `verification/r925/GateParityHarness.java`, `app/src/main/java/com/ethk3/novaonepro/DeepCandidateRuntimeR925.java`

生产接线及实现合同：同冻结100期向量真Android运行，设备/APK/输入hash和退出码；全1200/rank1/prob/mask/schema/在线state及热切换撤权重启

验收：

- 原数值容差与rank1=100%全过
- 假设备输出/错schema/hash被拒
- Host输出不可复制到Android栏

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E07C/`

## E07D · 第7批 · 实际恢复与最终5.4.2发布

原任务：Q04；依赖：E07A,E07B,E07C

源码：`scripts/r925_build_total.sh`, `scripts/r926_build_release.sh`, `release/release_manifest.json`

生产接线及实现合同：42任务+30卡+Q01-Q04/D11→真实备份/模型/历史恢复→源码APK计划证据一致→5.4.2/933/577250933；安装回退与源码回退区分

验收：

- 所有必需门有真实证据
- 单总包可核查重建并实际恢复
- 任何设备/Future未过只能RC，不虚报100%

失败/回退：拒绝/隔离无效新增贡献；保留上一完整合法baseline和历史证据；不修改过去预测/结算，按当前唯一Owner和冻结恢复合同重试。不得降门槛凑通过。

证据：`evidence/5.4.2/enhancement/E07D/`
