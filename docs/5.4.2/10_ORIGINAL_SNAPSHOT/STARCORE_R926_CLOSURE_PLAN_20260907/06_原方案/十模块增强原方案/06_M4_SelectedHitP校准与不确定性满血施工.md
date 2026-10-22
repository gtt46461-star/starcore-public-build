# M4：SelectedHitP Calibration + 不确定性

## 原则
最终要校准的是“已经被固定Ranker选中的Rank1动作命中概率”，不是任意action概率。

## 数据顺序
1. 用更早fold训练NumberQ。
2. 更后fold生成1200 rank。
3. 固定Ranker选择Rank1。
4. 只在更后cross-fitted Rank1上训练Calibration。
5. 再更后的Policy只能消费cross-fitted calibrated probability。
6. Sealed final一次性验证。

## 模型
C0 Beta calibration/logistic
C1 isotonic（样本足够）
C2 regime-conditioned beta calibration
C3 shrinkage calibration（小Regime向全局收缩）
最终按OOS Brier/ECE/NLL与稳定性选Champion。

## 不确定性
每个selected action除了p，还输出：
- reliability_bin_n
- wilson interval
- calibration support
- regime support
- model disagreement
- conformal/nonconformity score（可作为拒绝条件，不伪装为概率）

## 指标
- Brier
- ECE
- log loss
- reliability bins
- subgroup ECE
- calibration slope/intercept
- p区间命中率
- drift后的校准变化

## 失败闭环
若raw score高但calibration长期下修：
将此类issue写入FailureCluster，交给M8定向创造，而不是简单抬阈值。

## 生产门
SelectedHitP没有有效artifact时：
- UI必须显示N/A/NOT_PROVEN；
- DecisionGate不得把rawScore当calibrated probability。
