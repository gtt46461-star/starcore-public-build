# 策略创造、研究消费与实发资格的源码复核

本节细化 B06–B09，使用安装包 577311122 对应的 `5562f9b19aa09f9f7d9e69ca085db56c537967fc`。下文 Java 文件均在 `app/src/main/java/com/ethk3/novaonepro/strategyv2/`。现场计数来自同一份附件；下表中需要构造并发、不同数据集或特定角色才能触发的故障，是由源码识别的回归场景。报告没有逐行数据库原件，不能断言这些场景均已在该设备发生。

## 1. 找到一次运行中最早未完成的动作

| 阶段 | 附件能证明什么 | 施工现场应取的下一份原件 |
|---|---|---|
| 工厂与重生 | `DIAGNOSTIC_SCOPE.json.tables[table=r26_candidate/r26_expert_experiment/r26_foundry]` 显示 50 条候选为 BLOCKED_WARM_NUMERICAL_ABI_PRESERVED、12 条专家实验为 BLOCKED_EXPERT_ABI_PRESERVED、1 个工厂为 EXPIRED_RETENTION。 | 按单个父代查 campaign、保全状态、旧 ABI、曝光水位和原件。用 ColdRebirthR34 的原主键回查新子代，再核实 FoundryCampaignR26 的首个实际训练提交。50 和 12 属于不同对象，不能直接相加当作新策略数。 |
| 材料与训练 | `r30_atomnet_sample` 2150 行、`r31_atomnet_sample_stage` 790 行、`r31_atomnet_publication` 19 行状态 PUBLISHED；同包 `operatingSummary.atomNet` 中实际训练样本数、是否可用均为 null。 | 按同个 session 和 publication 查阶段、位置清单、权重、优化器更新及实际 forecast。材料化、验证、FILL 阶段分别计数。实训采用提交的更新及样本清单，不能用三个库存数相加或把 null 填成 0。 |
| 矩阵与选择 | `STRATEGY_MATRICES.json.matrixInventoryR34` 有 20 个登记对象、有留存快照的对象为 0。EVIDENCE_TEACHER 的一次回执为 BLOCKED_DEPENDENCY；另外 19 项此处为 NOT_RUN。 | 取同一角色、数据集、源版本、目标期的原矩阵和消费方。以无学生时可计算的 NUMBER_POSITION_COMPATIBILITY / CROSS_POSITION_FLOW 检查首产，再对学生误差等依赖矩阵逐件供给真实输入。 |
| 代际与独立未来 | `UPGRADE_DIAGNOSIS.json.capabilityRepairR32.operatingSummary.independentFuture` 记录 50 次 evolutionAttempts，0 个该口径 policyObjects，独立未来期、ACT、命中、合格实验、有效证书均为 0，命中率为空。 | 取原 generation manifest 中的父代、假设和子代；查 BORN、SELECT、独立注册、赛前冻结和结算的各自身份。用同一期 join 到决策和结算，不得把 R26 候选数改写为 R27 已出生或未来样本。 |

## 2. 源码核对后补入的批次细节

1. **B06-01，保全了父代但 SQL 看不见它。** `ColdRebirthR34.ownerAbi` 取工厂 ABI 或已有重生记录；对原工厂和后备重生记录均不可读的普通保全候选，`ownerAbi <> ?` 的值可以是 SQL NULL。分别核算已出生、能被 pick 选中、延期、身份待查，末项给出要恢复的原件和下一次触发条件。训练状态与曝光无法核实的父代保全原行，其他可运行父代继续。用含过期、有相同 ABI、不同 ABI 和 NULL ABI 的真实 schema 验证队列守恒。
2. **B07-01，两条训练入口共用一个验证边界。** `AtomNetSessionR30.work` 的 FILL 会把 `trainCount + selectCount` 末段送入回放池；`AtomNetTrainerR27.pretrain` 也会将 select 样本入池。`online` 从池中抽样训练，而 `EVAL_ONLINE_DEVELOPMENT_VALIDATION` 仍读取原 SELECT 窗口。这样可能使开发验证、模型回滚或开发增益使用已经参与该模型训练的记录。将保留作验证的 SELECT 排除在两个入口的回放与梯度之外；如新实验确需重用已曝光数据，另建边界、登记样本并使用后续未训练验证块。兼容旧 checkpoint 时保留旧权重和原证据，来源不足的旧分数标为待核。用实际采样数据验证集合交集为零，并在相同 seed 下核对崩溃恢复的下一步抽样顺序。这个缺口涉及开发验证，附件没有给出正式 FUTURE 标签泄漏证据。
3. **B08-01，进化长期被阻塞时缓存如何退场。** `RuntimeR27.publishDatasetInputR335` 可在 A 活跃时把 B 放入 pending，下一次把 C 赋给同一字段。进化线程也可已选中 B 在事务外计算。制定引用交接顺序，在同步段内选定并持有引用，替换只退休旧缓存，最后读者退出后释放。用 A/B/C、提交取消和提交后死亡检查活跃租约、可重读的数据 pin 和下次续建游标；先复现所有权问题再决定复用哪一层现有租约。不得在赋值处直接 close 正被计算的 B。
4. **B08-03，TEMPORAL_PRECEDENCE 有两个水位。** `MatrixEvidenceR30R1.stored` 的 MODEL_PAIR_FROZEN 查询按全库最新 ordinal 取一条，经 validRunSql 验证，其输出携带这条源的 dataset/revision；`MatrixResearchR30R1.produceR31` 随后按当前请求的 dataset/revision 核验。若 A 的合法行较旧，更新的 B 行可能令 A 被判源身份改变；`decidedR31` 把相同依赖键视为已处理。让 SQL 先约束 A 的合法输入范围再排序，并使用同范围实际原件身份唤醒重试。没有同源行时保留等待。如在线窗口本来就采用独立 pin，应先持久化该 pin 与研究请求之间的合法映射，不能删除外层身份检查。夹具交错写入 A/B 并保留 A 已有原件，实际产物必须引用 A 的原冻结与时间差。
5. **B08-04，落实现场唯一 P50 升级处方 S4。** `UPGRADE_DIAGNOSIS.json.findings` 将 S4 标为旧诊断。截至 3616141 的留存文本写有 7229 次重拟合、已证实 0 条；`sourceEvidenceR335.REFIT` 为 UNKNOWN_LEGACY_CHECKPOINT_NO_MEASURED_REFIT_ORIGINAL，`LEDGER.state` 为 NO_S5_CASE。当前报告应并列展示历史数、可回查水位、重启后新实测次数。复用 `ContMatSU2.update/refit` 的时序与最近 40 个已结算块；源码从 t≥2000 且 t 能被 500 整除时尝试重拟合。每半至少 200 个支持单元、方向一致、全位置两半 |z| 均≥2.5 或单位置均≥3.5，才可留下该模式的 VERIFIED 原件。对没有通过的模式记录各半 n/k/z；保持 S4 检测。满足既有 S5 条件时从 `DiagnosisStoreSU2.maintain`、`DiagnosisShadowReceiptR335.attempt/readback` 得到影子、谱系和实际基因组；槽满等原因保留可恢复状态。后续影子评估仍须按原协议等待真实样本；诊断建议中的 600 次影子出手属于效果复看口径，与正式 V1 门槛各自记账。
6. **B09-01，已确认的对照不能随下次选择改变。** `FrozenConfirmationStoreR30` 固定确认 packet；但 `LiveShadowsR27.certify` 通过 `controlId` 按 promoted_at 取该类最新对照，`paired` 对两条账上已结算的重合 ordinal 做 INNER JOIN。登记未来实验时固化适用的 I00/NULL 对照身份或预先排定且不可事后改变的换届规则，对缺少对照的机会单列缺口。输出实验原始表、控制原始表、预登记全机会和实际配对范围。历史实验对照原件不足时保留待证状态；修改统计检验或门槛应另立版本并重新前瞻登记。
7. **B09-02，部分重试会改变汇总分母。** `LiveShadowsR27.freeze` 从 todo 排除已冻结对象；正常及 deadline missed 两个分支又以本切片 todo.size 执行 INSERT OR REPLACE 到 `r27_opportunity`。同一目标的第二个子集可以覆盖第一次累计的 scheduled/frozen/acted/settled。以冻结时已登记的实验集合为当期基准，在同一短事务从各实验的已有原行重建汇总或作受唯一键保护的增量。完整性等式须区分准时 ACT、准时 HOLD、明确弃权、输入故障、迟到、无原件、未决和未登记；未决及异常均各有原因，不混入合法 HOLD。正式资格按每个实验的 `r29_qual_calendar` 与消费原件核验，不以 UI 汇总授证。
8. **B09-03，实际证书有三层结果。** `CertificateGateR27.evaluate` 判 V1 数学条件。随后 `LiveShadowsR27.certify` 取 `futureSummary`，覆盖不足 15% 或记录净收益非正都会把 issued 改为 false；模型、校准、资格原件、来源撤销和缺口也影响 issued。最后，研发质量报告给出独立区间、频率目标、质量与稳定性判据。按数学门、实发状态和额外资产质量保存三份独立结果。部署后 REAL 频率只在真实部署机会产生后展示。有效证书正常参加原选择，获选后通过唯一冻结和决策进入主链；多个证书按原规则择一，缺合格证书保持生产侧合法终态。

## 3. 工程回放、设备观察与自然效果

B06–B09 各使用本批自己的独立测试库。B08 的 SELECT 产物可从隔离原件开始，B09 的原始注册/冻结/结算可用当前批夹具，均无需未来批次才能签工程收据。回归命令、源码身份、输入 seed、全量断言、交易前后原行与返回状态随本批收据保存。重复执行后并排比较身份及计数，可以发现重复出生、重复结算和伪造的消费增长。

设备升级后，在同一 session 采两个连续水位，逐项给出新提交、等待条件或失败原件：真实子代首次提交、首个 optimizer step、十位置模型首次被消费、首个有支持的矩阵、首个消费动作、独立实验首个赛前机会。测试正例使用隔离身份；现场没有合法自然证书时仍可验证影子业务进度。只有预登记自然账才能给出未来胜率、覆盖、稳定性与真实收益，零 ACT 时胜率为空。有足够样本仍未达门槛，应登记本次拒绝并反馈给下一独立研究实验。
