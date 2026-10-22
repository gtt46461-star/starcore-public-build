# M5：Regime / 状态路由 / 分层稳定

## R924已有修复
AlgorithmRegimeEvidenceR923已经修复旧版固定1/3、2/3分桶导致“缺组=0命中”的问题，改成label-blind tertiles并要求至少2组、每组N≥12。

## R925进一步升级
### Regime必须来源于赛前状态
候选：
- volatility/entropy regime
- transition regime
- gap pressure regime
- trend lifecycle
- data freshness/source health（只作为运行域，不作为号码优势）
- expert disagreement
- change-point risk

### 两类Regime
1. Market/sequence Regime：用于策略泛化。
2. Runtime Regime：用于解释系统状态，绝不能被当成号码规律。

## 评估
每个候选必须输出：
- overall
- regime A/B/C
- represented groups
- min group N
- min hit rate
- min Wilson
- Brier/ECE per regime
- paired delta per regime

## 稳定性规则
- Regime support不足 → PAUSED_EVIDENCE，不PRUNE。
- 有充分support且某Regime长期Wilson上界低于baseline → QUALITY_PRUNE。
- 如果只在一个Regime有优势，可成为DOMAIN_CERTIFIED候选，不强求全域，但生产只在该Regime允许贡献。

## 价值
这样“专门在某状态有效”的策略不会因为全局平均被误杀，也不会因为小样本某状态100%就乱晋级。
