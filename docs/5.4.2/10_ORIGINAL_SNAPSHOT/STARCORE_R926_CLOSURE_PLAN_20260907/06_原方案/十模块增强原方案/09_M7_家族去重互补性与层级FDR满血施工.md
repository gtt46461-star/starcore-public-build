# M7：Family去重、互补性、层级FDR

## 问题
153算法/1852公式中大量变体高度相关。
如果每个都当独立假设：
- 计算浪费；
- FDR惩罚大；
- 同质候选挤掉真正新方向。

## 三重去重
1. Structural：structural_hash完全相同 → 合并。
2. Behavioral：固定PIT probe集上的1200排序/selected输出hash相同或高度一致 → 合并。
3. Statistical：Future OOS逐期残差相关>0.95，且没有明显互补Regime → 同一family。

## Family Champion
每family只保留：
- 1个Champion
- 2~3个active challenger
其他进入archive但证据不删除。

## 互补性
新候选必须至少满足之一：
- 对Champion错误集合有显著互补；
- 在某Regime改善；
- 降低Brier/ECE；
- 降低MaxMiss；
- 提升paired HR。
单纯结构不同但输出相同，不算novelty。

## Hierarchical FDR
第一级：family-level p/q；
第二级：只有family通过探索门后才在族内做候选校正。
FDR报告必须给：
- hypotheses_total
- family_count
- family_q
- within_family_q
- final_q
而不是只显示一个100%。

## 防止“为了过FDR而少报候选”
候选注册后不可删除历史；停止运行只改变active状态，hypothesis ledger仍保留，防止选择性报告。
