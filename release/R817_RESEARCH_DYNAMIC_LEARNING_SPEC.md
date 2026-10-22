# R817 深度学习、动态迭代与策略创造运行规范

## 1. 三条互相隔离的研究通道

### A. 公式/策略 DSL 通道

- 候选由 failure distribution、third-boundary、窗口、关系、共识和校准证据生成。
- DSL 只允许白名单特征与有界算子；禁止反射、I/O、网络、数据库写、BET 和生产 owner。
- 当前 APK 使用通用 DSL runtime 对完整 1200 动作开奖前评分；生成 Java 是可审计、可离线编译的完整源码，不做动态加载。
- 12–119 个 strict OOS 只允许 `EXPLORATORY_COLLECT_ONLY` 公式；learned expert/model 至少需要 120 个合法样本。

### B. 冻结深度模型通道

- 输入 `x[96]`；隐藏层 `64 ReLU -> 32 ReLU`；输出 `z[100]`；reshape 10×10 后执行 log-domain Sinkhorn。
- 损失：`L = -sum_p log(max(eps,Q[p,actual_p])) + lambda2*sum(W^2)`。
- 优化：Adam `(0.9,0.999,1e-8)`，全局梯度裁剪 5；候选学习率 `1e-4/3e-4/1e-3`，L2 `0/1e-6/1e-5/1e-4`。
- 数据严格 chronological train -> validation -> sealed final；不 shuffle；early stop 只看 validation；sealed final 不参与训练。
- 大网络训练只在离线工具 `training/r811_autonomous_research.py` 运行。Android 只接受完整 shape、schema、dataset、trained-until 和 SHA-256 验证后的 frozen Artifact 推断。

### C. 在线轻量 FTRL 通道

- 特征固定为：`raw_action_p, rank_score, third_margin, entropy, stability, effective_consensus, selected_hit_p`。
- 参数：`alpha=0.05, beta=1, l1=0.001, l2=0.01`。
- 更新公式：
  - `p = sigmoid(w·x + bias)`；
  - `g_i = (p-y)x_i`；
  - `sigma_i=(sqrt(n_i+g_i^2)-sqrt(n_i))/alpha`；
  - `z_i += g_i-sigma_i*w_i`，`n_i += g_i^2`；
  - 若 `|z_i|<=l1`，`w_i=0`，否则 `w_i=-(z_i-sign(z_i)l1)/((beta+sqrt(n_i))/alpha+l2)`。
- 只有 strict OOS 且 `frozenAt < drawOpenTime` 的结算行能更新；相同期重放幂等；晚到的旧期不能在新 Artifact 之后补学。

## 2. 动态消费而不是“只更新不使用”

R817 在每个未来目标期开始时读取最新合法 FTRL Artifact：

```text
require artifact.trainedUntilIssue < targetIssue
w = 0.35 * min(120, updates) / 120
p_shadow = (1-w) * p_formula + w * p_ftrl
```

- 无 prior Artifact 时 `w=0`，完全使用公式概率。
- 12 次更新时 `w=0.035`；120 次及以后封顶 `w=0.35`。
- 每条 predraw 行保存 formulaP、adaptiveP、Artifact hash、trainedUntil、updates、weight、input hash 和 record hash。
- 这使在线学习真实影响下一期 SHADOW 选择，同时防止小样本过度摆动和同期开奖泄漏。

## 3. 开奖前/开奖后顺序

```text
fetch draw(t)
-> production SettlementEngine settles t
-> strict OOS label(t)
-> FTRL update(t), artifact.trainedUntil=t
-> resolve target(t+1)
-> production canonical(t+1)
-> research exact-1200 predraw(t+1), may consume artifact(t)
-> first-write freeze
-> after draw(t+1), label only; no model call
```

## 4. 晋级规则与权限边界

- 公式可在 12 个样本时进入 incubator，但这不是有效性证明。
- learned candidate 的 screening、future-live 和最终 proof 使用更高独立样本门；生产证明仍要求 `Nselected>=480`、hit/ECE/Brier/Wilson/Beta/coverage/block/FDR/future-live 全部通过。
- Research candidate 只能进入 SHADOW challenger；Production mutation=0，REAL authority=0。
- `DecisionGateEngine_ONLY`、`SettlementEngine_ONLY`、`AnalysisSingleFlightR36_ONLY`、`TargetIssueResolver_ONLY` 不变。
- 本规范实现的是可执行研究基础设施，不宣称彩票随机过程可被保证预测，也不把 25 个样本渲染成模型证明。

