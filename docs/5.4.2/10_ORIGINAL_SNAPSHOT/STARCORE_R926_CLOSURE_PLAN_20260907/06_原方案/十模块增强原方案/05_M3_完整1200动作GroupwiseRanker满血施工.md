# M3：完整1200动作 Groupwise Ranker（本轮最关键模型重构）

## R924根因
现有V50/V51训练代码把每期内部group size做成10（每位置Top3候选），但生产动作宇宙是1200。
这是训练和生产语义不一致，必须彻底重写。

## 正确训练数据
每一个target_issue构造完整：
position=1..10
每位置C(10,3)=120
总1200 action。

每一action字段：
- action_id/global_ordinal 0..1199
- position
- n1<n2<n3
- raw_action_p
- individual number Q
- third/fourth boundary
- entropy
- stability
- trend/shape/relation/teacher evidence
- regime
- atom summary
- mainline score
- candidate algorithm scores（研究列，不一定生产）
- hit_label（开奖后，仅训练label）

## 训练器
新增：
`training/r925_build_1200_action_dataset.py`
`training/r925_train_groupwise_ranker.py`
`training/r925_rank_metrics.py`
`V51ActionRankerArtifactR925.java`

## 模型家族
R0：RawActionP baseline
R1：线性pairwise
R2：listwise softmax / ListNet-like linear
R3：Lambda-style weighted pairwise（按rank顶部加权）
R4：浅层GBDT/forest可仅离线比较，若Android无法无第三方稳定推理则不得直接生产；可蒸馏为线性/小网络。
R5：小型MLP ranker，可导出Java数组。

## Groupwise Loss
不能把1200 action当独立样本。
每issue:
- 正样本：包含实际号码的固定K3动作（同位置共有C(9,2)=36个）
- 目标不是让所有36个同权；可以基于margin/稳定性构造soft relevance，但必须赛前特征定义。
建议：
1. listwise normalized relevance；
2. top-weighted loss，让Rank1/TopK更重要；
3. pairwise hard-negative：与最佳动作只差一个号码的近邻。

## Hard negative mining
重点采：
- 同位置只差一个号码；
- rawP很高但未中；
- entropy低却失败；
- teacher强却失败；
- cross-position支持高却失败。
这样ranker学习“为什么高分假象会错”。

## 指标
每issue输出：
- Rank1 hit rate
- Rank2/Rank3
- MRR
- NDCG@1/3/10
- oracle gap/regret
- top1 margin
- paired vs RawActionP
- block/regime stability
- MaxMiss
- latency

## 选择规则
Challenger必须满足：
- Rank1 > baseline Rank1，且置信下界非劣；
- Rank1 > Rank2/Rank3不是唯一条件；
- MRR/NDCG改善；
- paired improvement正；
- 4块至少3块非劣；
- Regime不崩；
- 资源符合一分钟实时预算。

## Java/Android parity
对固定100个issue × 1200 actions：
Python score、Java host score、Android score逐action比对。
必须：
- ordinal完全一致
- tie-break一致
- Rank1一致100%
- score误差≤定义epsilon
- artifact hash一致

## 完成定义
只有这个模块完成以后，才能说“训练出来的Action Ranker”和生产真正是同一个问题。
