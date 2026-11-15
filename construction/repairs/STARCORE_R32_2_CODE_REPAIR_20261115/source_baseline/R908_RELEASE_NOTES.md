# STARCORE_TIANSHU V57.17.2-R5.0.2-908

## 本版目标

P0：修复 R907 实机报告暴露的结算/OOS 积压、后台 Research IDLE、学习未启动与实时 P95 高压问题。
P1：把“公式变异”与“算法发明”分离，建立历史驱动的公式合成与 primitive-program algorithm genome 两条研究线。

## 不可变红线

- package=com.ethk3.novaonepro
- DB_VERSION=36，R906 additive schema revision=4
- single-position / fixed-K3 / max-one-REAL-per-issue
- SHADOW_ONLY=true / MODEL_PROVEN=false / REAL_BET_HARD_DISABLED=true
- Research/Algorithm invention 永远无 REAL 授权。

## R907 实机证据对应修复

- 192 个 ready frozen SHADOW：独立 settlement correctness looper，当前期优先，ready-predraw 次优先，历史 housekeeping 最后。
- Research state=IDLE：独立 15s self heartbeat + worker death repair。
- FTRL NOT_STARTED：ResearchOrchestratorR906 正式接入分阶段 Research pulse。
- 研究与结算锁竞争：Research settlement/coverage 从 Production SettlementExecutionGate 移除。
- P95 高压：Production analysis/settlement 与 Research/Deferred 分线程、分阶段、限预算。

## 真正创造能力

1. HistoricalStrategyInventionR908：raw history -> empirical atoms -> chronological walk-forward -> executable strategy/formula graphs。
2. AlgorithmGenomeR908：低层 primitive instruction genome，不以 Markov/forest/neural/bandit 为母模板。
3. 新算法必须同时拥有历史输入、持久状态、score program、settlement outcome feedback、update program。
4. structuralHash 只由真实执行程序决定，generation/parents 不产生假新颖性。
5. behaviorHash 覆盖多轮 score->outcome->update->score，学习机制不同可区分。
6. mechanismCell 做 quality-diversity 保留，避免种群坍缩到一堆近似公式。
7. 中文名由实际特征依赖、记忆、反馈、分流、耦合、压差、非线性机制自动生成。
8. 任何历史/开发命中率只用于排序，必须重新冻结并积累 future OOS 才能评价；不伪造高胜率。

## 物理设备边界

Host/构建可以证明代码合同、DB migration、模拟结算收敛、签名和 APK 身份；不能替代 360 期熄屏/杀进程/断网/24h 真机 soak，因此 PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED。
