# 两份总方案合同的独立交叉复核

复核对象：`contracts/CREATION_TRAINING_AND_QUALITY.md`、`contracts/PIPELINE_AND_PRESERVATION.md`（2026-11-28 初稿）。基线为安装版 577311211 / 5c7f1b36eaee6036df280e85835bb0fb4ce0b40d。此文件是复核意见；父代理更新后的文档应再次核对，不能把初稿复核当修改后已经通过。

## 必须改正或明确的三处

|编号|位置|发现|正确依据及处理|
|---|---|---|---|
|CC01|CREATION 第1节 TREE 行|阶段名称写成 `TREE_QUANTILES / TREE_TRAIN`|`r952/DevelopmentTrainerCP9.java:16` 的枚举是 `TREE_QUANTILES / TREE_SCAN`；124行以后实际也走这两个阶段。仅改阶段名，不修改训练器。|
|CC02|CREATION 第5节“每次真实结算”|容易被施工理解为“必须先有 REAL 交易才能训练”，会造成没有合格策略→没有REAL→不训练的循环等待|`r952/RollingLearnerR343.settled:62–75` 要求合法 draw 和权威 `r36_settlement_task COMPLETED` 的同一 settlementHash/completedAt，不要求 REAL trade。`ResearchDirectorR952:169` 在原结算研究路径投递。写成“每个权威开奖已完成结算事件，包括无 REAL 出手的期”；自然REAL质量仍单列。|
|CC03|CREATION 第6节 R18 三态|列精确已结算、待结算、身份未知，但仍需保留不存在原回执或错键的缺证状态|原 `exactRealAttributionR34` 返回三种存在回执的 state；receipt==null/错实验/错授权是独立缺证。候选投影已覆盖四类显示与反例。|

## 经源码或报告核对吻合的部分

- 211 精确应用底座、main 仅保存方案及不得整文件倒灌的边界正确。
- 现代 family 名称与数值内核并非一一对应；现有 fixed graph、linear/logistic、tree、MLP、Hedge 不能因现场为零而重写。
- CP9 训练样本 300–1200，加 120 验证、一个日历期间隔，训练区间标准化正确。32 行梯度批次适用于需要梯度的模型，`epoch==19` 或 patience≥3 停止；fixed graph/Hedge/tree 各按其实际阶段完成，不要求它们伪造神经梯度。
- R26 candidate/evolution 各50条、R27 replay_dataset3且policy/replay_score/generation等0的口径正确。三个 pipeline 不相互冒充成果。
- 20 canonical矩阵0和旧R952 snapshot50/feedback48的区分正确；保护旧图和旧矩阵合理。
- 原173继承计划确有30%目标/15%最低目标，不是本轮另改门槛；当前报告Q02实际登记目标15%，需在验收表保留两个范围和真实来源，不能合并成当前已达目标。
- 173旧图表035/036/037在211已出现修复代码和报告读层改进，继续保留，不重开旧全文件补丁。
- R11/R14/R18必须明确FORMAL_READ_ONLY。当前合同说图表支持实际决定，仍不能使这些正式图结果进入训练或筛选；CV04/05已给此约束。

## 总方案仍需覆盖的补充

1. R11 的原确认数值已有，但UI只取verified0/1，原result中的配对收益/下界和角色未投影。按CV13小改原结果显示，不重开HOLDOUT，不向训练回流。
2. R10/R17 每对象CPU原件缺失，以UNKNOWN和AST复杂度如实显示；另补CPU原件时不能用整个任务池CPU替代。此项不成为核心停工修复前置。
3. Q08原完整工厂是六臂×20seed=120轮：`NULL_UNIFORM`、`NULL_TIME_SHUFFLED`、`SIGNAL`、`SIGNAL_DISABLED`、`DRIFT`、`GAPS`。`FactoryCapabilityExperimentR31.protocol` 的 `.30 signalStrength` 是植入参数，不是模型命中率。34/37/40%目标概率对照、原六臂创造工厂和自然未来资格必须分别验收。报告旧源ab2695…、完成0/120，不能用旧19/20或20/20小合成签当前工程有效。
4. 既有8张有效图的具体ID不能从本报告直接确定：14旧缓存行、17历史研究帧、8可读逻辑表是不同统计。施工前需真机逐图基线，不能擅自选择8个ID当成用户原图。
