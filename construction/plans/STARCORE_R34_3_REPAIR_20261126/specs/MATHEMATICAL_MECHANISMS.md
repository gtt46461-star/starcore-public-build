# 已安装源码的 24 + 15 家族数学机制清单

源码 commit：`4d1de9d83692635b5c5ce39144fece1c8a31c8ac`。对象：已安装版本 173 / R34.2 源码。本清单不把计划编号 R34.3 当作已完成的新 APK。

**结论：数学实现存在，但家族标签、实现可执行、真实训练成功、有效资产出生、独立前瞻质量、主链真实消费是六件不同的事。下面只对照实现，不以源码存在冒充本次运行已完成。**

## 1. 核对规则与主要发现

- 不是“完全没有数学代码”：已安装源码中可找到以上确定实现。但 dispatch 存在不证明当前版本已成功训练、生产多种有效资产或进入真实主链。
- 39 个家族名不是39个互不相同的数学优化器；应分别计数图结构、kernel、学习参数、统计更新、候选身份与独立质量资格。
- 现代7类固定规则不训练模型权重；TREE 学结构；LOGISTIC 是 OVR BCE；LINEAR/HYBRID 共用 softmax kernel；BANDIT/EXPERT/ENSEMBLE 共用 Hedge。
- 基础工厂 family cursor 的 variant 范围0–39。15*40只是此基础路径的上限，不是无限新机制产量；从parent继续创造走另一真实入口，仍受去重、训练和资格约束。
- F23 有明确教师依赖，F19 的默认绑定为 Tensor；误用旧类或无教师时宣称可用都会使施工/验收对不上。
- 每个方法的状态更新必须证明前后模型哈希、准确预测前输入哈希、原始标签序号、checkpoint、消费身份；图表非空、计数增长和训练损失下降都不能单独证明高胜率。
- 已安装源码的 modern 实时数值更新仍缺通用 owner：AST3 不进 legacy generic predraw，modern update 明确拒绝。新增 rolling 方案必须在原 owner 的副本/冻结身份、CAS 与真实 receipt 约束下验收。

所有公式是所列函数的直接转写。数字在公式中按实现的内部索引描述；实际 draw 标签是 1–10，数组类别索引是 0–9。完整逐段原文、行号、文件 SHA-256 保存在配套 JSON 的 evidence/sources 中。

## 2. F01–F24 总览

| 家族 | 实际默认实现 | 数学作用 | 更新种类 |
|---|---|---|---|
| F01 | FamilyF01KernelR26(lags=3,cross=true) | 跨位置多滞后稀疏线性排列模型 | GRADIENT_PROXIMAL |
| F02 | FamilyF02KernelR26(graph=0,factors=true) | 相邻位置成对势排列链 | GRADIENT |
| F03 | NeuralFamilyR26("F03",graphVariant=0) | 三元超图神经特征 | GRADIENT_AUTODIFF |
| F04 | NeuralFamilyR26("F04",graphVariant=0) | 两层因果膨胀卷积 | GRADIENT_AUTODIFF |
| F05 | NeuralFamilyR26("F05",graphVariant=0) | 因果窗口注意力 | GRADIENT_AUTODIFF |
| F06 | NeuralFamilyR26("F06",graphVariant=0) | 门控状态空间递推 | GRADIENT_AUTODIFF |
| F07 | NeuralFamilyR26("F07",graphVariant=0) | 学习边消息传递 GRU | GRADIENT_AUTODIFF |
| F08 | FamilyF08KernelR26 -> OnlineModelsSU2.Hmm | 过滤式隐马尔可夫混合 | STATISTICAL_POSTERIOR |
| F09 | FamilyF09KernelR26 | 带持续时间的隐藏状态混合 | GRADIENT_AND_EXPECTED_COUNTS |
| F10 | OnlineModelsSU2.Bocpd(hazard=.005,alpha=1,R=160) | 截断运行长度贝叶斯变点 | BAYESIAN_COUNTS |
| F11 | FamilyF11KernelR26(lr=.02) | 可学习距离的有限情节检索 | GRADIENT_AND_MEMORY |
| F12 | FamilyF12KernelR26 | 分层上下文类别计数 | HIERARCHICAL_COUNTS |
| F13 | SymbolicFamilyR26("F13",formulaVariant=0) | 符号乘积特征 | GRADIENT |
| F14 | SymbolicFamilyR26("F14",formulaVariant=0) | 带在线状态的符号宏 | GRADIENT_AND_EMA |
| F15 | SetFamilyR26("F15") | 完整 1200 动作风险读出 | GRADIENT_COMPOSITE |
| F16 | SetFamilyR26("F16") | 双分支与七层支持集风险读出 | GRADIENT_COMPOSITE |
| F17 | PermutationModelR26(lr=.02) | 跨位置 lag-1 精确排列模型 | GRADIENT |
| F18 | FamilyF18KernelR26 | 已用数字集合的掩码自回归 | GRADIENT_AUTOREGRESSIVE |
| F19 | FamilyF19TensorR26(.02,true,false) | CP 张量因子化精确排列 | GRADIENT_ADAGRAD |
| F20 | FamilyF20KernelR26(slots=16,retrieval=MATCH) | 因果状态片段检索 | STATISTICAL_RETRIEVAL |
| F21 | FamilyF21KernelR26 -> OnlineModelsSU2.RelBayes | 折扣关系贝叶斯统计 | STATISTICAL_BETA_COUNTS |
| F22 | ResidualFamilyR26 | 冻结父模型后的门控残差 | GRADIENT_STAGED_PARENT |
| F23 | LegacyTeacherOwnerR26.buildBoundR31 -> DistillationFamilyR26 | 明确绑定旧 VM 教师的蒸馏学生 | GRADIENT_DISTILLATION_CONDITIONAL |
| F24 | PortfolioFamilyR26 | 三个独立模型的上下文软路由 | GRADIENT_COMPOSITE |

## 3. F01–F24 逐项绑定

### F01 跨位置多滞后稀疏线性排列模型

**实现：** `FamilyF01KernelR26(lags=3,cross=true)`。

**输入：** 此前最多 3 期的完整十位置开奖；预测和梯度先读取旧 ring，再写入新标签。

**状态：** bias[10][10]、w[lags*10][1000]、ring、时钟；write/read 保存完整状态。

**实际公式：**

- s[p,n]=bias[p,n]+(1/10)Σ(l,q) w[l,q,p,n,draw[t-l,q]-1]。
- 使用 UNARY_PERMUTATION_HEAD：合法排列联合 NLL，g[p,n]=marginal[p,n]-1{y[p]=n}。

**更新：** 学习率 .02；每个 lag/source-position 组使用 L2 组范数，近端缩放 max(0,1-.02*.0005/||w_group||)。诊断损失包含 .0005Σ||w_group||，保持同一预测前上下文。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：lags/cross 的变体改变图或边；仅改变学习率、窗口、种子不能计作新增独立机制。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S011 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF01KernelR26.java:5–34`

### F02 相邻位置成对势排列链

**实现：** `FamilyF02KernelR26(graph=0,factors=true)`。

**输入：** 上一期同位置数字，以及预声明的位置图顺序。

**状态：** unary[10][10]、context[100][10]、pair[9][10][10]、previous、t。

**实际公式：**

- 能量 = Σ位置(unary + 同位置 lag-1 context) + Σ图中相邻边 pair。
- PermutationChainR26 在不重复数字约束下求节点、边边际和 logZ；损失 logZ-真实排列能量。

**更新：** 节点/上下文梯度为节点边际减真实值；边梯度为边边际减真实边。SGD .02，参数裁剪 [-8,8]，先暂存再提交。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：不能用 unary ExactPermutation 的边际替代此成对势模型；默认图与 graph=1/2 变体需分别标明。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S012 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF02KernelR26.java:5–33`；S013 `app/src/main/java/com/ethk3/novaonepro/strategyv2/PermutationChainR26.java:1–18`

### F03 三元超图神经特征

**实现：** `NeuralFamilyR26("F03",graphVariant=0)`。

**输入：** 已到达历史 lag 特征 (digit-5.5)/4.5、位置/数字嵌入；递归图仅使用预测前状态。

**状态：** 4096 个权重、同形 momentum、40 维 recurrent、8×10 ring、时钟；完整版本化 codec。

**实际公式：**

- 自己的位置/数字嵌入与另外两个位置嵌入乘积形成三元项，经过 tanh 后接 100 个 logit。默认邻居为 p+1、p+2。
- UNARY_PERMUTATION_HEAD；对每个 logit 的反传种子为 marginal-onehot。

**更新：** AutoDiffR26 反传；全局梯度范数裁剪 5；momentum=.9*m+.1*g；`w=clip[-6,6](w-.03*m)`。递归梯度是单步截断，不能描述为无限长 BPTT。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：F03–F07 共享执行器和优化器，但 switch 确实选择不同因果图；需以图身份、checkpoint 和实际步骤证明各图运行。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S014 `app/src/main/java/com/ethk3/novaonepro/strategyv2/NeuralFamilyR26.java:5–58`

### F04 两层因果膨胀卷积

**实现：** `NeuralFamilyR26("F04",graphVariant=0)`。

**输入：** 已到达历史 lag 特征 (digit-5.5)/4.5、位置/数字嵌入；递归图仅使用预测前状态。

**状态：** 4096 个权重、同形 momentum、40 维 recurrent、8×10 ring、时钟；完整版本化 codec。

**实际公式：**

- 每层 3 个 tap，默认膨胀间隔 1、2；残差 tanh，最后的隐藏向量接 100 个 logit。
- UNARY_PERMUTATION_HEAD；对每个 logit 的反传种子为 marginal-onehot。

**更新：** AutoDiffR26 反传；全局梯度范数裁剪 5；momentum=.9*m+.1*g；`w=clip[-6,6](w-.03*m)`。递归梯度是单步截断，不能描述为无限长 BPTT。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：F03–F07 共享执行器和优化器，但 switch 确实选择不同因果图；需以图身份、checkpoint 和实际步骤证明各图运行。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S014 `app/src/main/java/com/ethk3/novaonepro/strategyv2/NeuralFamilyR26.java:5–58`

### F05 因果窗口注意力

**实现：** `NeuralFamilyR26("F05",graphVariant=0)`。

**输入：** 已到达历史 lag 特征 (digit-5.5)/4.5、位置/数字嵌入；递归图仅使用预测前状态。

**状态：** 4096 个权重、同形 momentum、40 维 recurrent、8×10 ring、时钟；完整版本化 codec。

**实际公式：**

- 4 维 Q/K/V；注意力权重 softmax(0.5·Q·K)，加权 V 后 tanh，输出 100 个 logit。
- UNARY_PERMUTATION_HEAD；对每个 logit 的反传种子为 marginal-onehot。

**更新：** AutoDiffR26 反传；全局梯度范数裁剪 5；momentum=.9*m+.1*g；`w=clip[-6,6](w-.03*m)`。递归梯度是单步截断，不能描述为无限长 BPTT。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：F03–F07 共享执行器和优化器，但 switch 确实选择不同因果图；需以图身份、checkpoint 和实际步骤证明各图运行。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S014 `app/src/main/java/com/ethk3/novaonepro/strategyv2/NeuralFamilyR26.java:5–58`

### F06 门控状态空间递推

**实现：** `NeuralFamilyR26("F06",graphVariant=0)`。

**输入：** 已到达历史 lag 特征 (digit-5.5)/4.5、位置/数字嵌入；递归图仅使用预测前状态。

**状态：** 4096 个权重、同形 momentum、40 维 recurrent、8×10 ring、时钟；完整版本化 codec。

**实际公式：**

- h[k]=sigmoid(affine610(x))*previousState[k]+tanh(affine630(x))*x[k]，输出 100 个 logit。
- UNARY_PERMUTATION_HEAD；对每个 logit 的反传种子为 marginal-onehot。

**更新：** AutoDiffR26 反传；全局梯度范数裁剪 5；momentum=.9*m+.1*g；`w=clip[-6,6](w-.03*m)`。递归梯度是单步截断，不能描述为无限长 BPTT。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：F03–F07 共享执行器和优化器，但 switch 确实选择不同因果图；需以图身份、checkpoint 和实际步骤证明各图运行。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S014 `app/src/main/java/com/ethk3/novaonepro/strategyv2/NeuralFamilyR26.java:5–58`

### F07 学习边消息传递 GRU

**实现：** `NeuralFamilyR26("F07",graphVariant=0)`。

**输入：** 已到达历史 lag 特征 (digit-5.5)/4.5、位置/数字嵌入；递归图仅使用预测前状态。

**状态：** 4096 个权重、同形 momentum、40 维 recurrent、8×10 ring、时钟；完整版本化 codec。

**实际公式：**

- softmax 学习边权汇聚历史隐藏状态；reset/update 为 sigmoid；h=update*old+(1-update)*tanh(candidate)，输出 100 个 logit。
- UNARY_PERMUTATION_HEAD；对每个 logit 的反传种子为 marginal-onehot。

**更新：** AutoDiffR26 反传；全局梯度范数裁剪 5；momentum=.9*m+.1*g；`w=clip[-6,6](w-.03*m)`。递归梯度是单步截断，不能描述为无限长 BPTT。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：F03–F07 共享执行器和优化器，但 switch 确实选择不同因果图；需以图身份、checkpoint 和实际步骤证明各图运行。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S014 `app/src/main/java/com/ethk3/novaonepro/strategyv2/NeuralFamilyR26.java:5–58`

### F08 过滤式隐马尔可夫混合

**实现：** `FamilyF08KernelR26 -> OnlineModelsSU2.Hmm`。

**输入：** 上一时刻状态后验 a 与新到达完整开奖；预测使用转移后的先验 pi。

**状态：** 固定 A、emission E、后验 a、updates、历史计数是否完整标记。默认 K=3, stay=.995, lr=.01, seed=1, temper=.6。

**实际公式：**

- pi[j]=Σ_i a[i]A[i,j]；p[position,digit]=Σ_k pi[k]E[k,position,digit]。
- a_new[k] ∝ pi[k] exp(.6 Σ_position log E[k,position,y])。
- E_new[k,p,n]=(1-.01*a_new[k])*E[k,p,n]+.01*a_new[k]*1{y[p]=n}。

**更新：** 后验加权类别频率松弛；A 固定。StatisticalTrainingDiagnostics 明确 NOT_SGD，delta 只量 emission。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：不能把固定转移矩阵说成已训练转移参数，也不能把统计增量记录为梯度优化成功。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S015 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF08KernelR26.java:9–42`；S016 `app/src/main/java/com/ethk3/novaonepro/strategyv2/OnlineModelsSU2.java:167–202`；S017 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelEvolutionSU2.java:29–33`

### F09 带持续时间的隐藏状态混合

**实现：** `FamilyF09KernelR26`。

**输入：** 预测前 K=3、D=32 的状态/持续时间概率，随后到达排列标签。

**状态：** 3×100 emission logits、3×32 prior/post、exit/stay 计数和时钟。

**实际公式：**

- 每个隐藏状态使用 UNARY_PERMUTATION_HEAD；总体输出按状态与持续时间先验混合。
- hazard[k,d]=exit/(exit+stay)，终末 duration=31 时 hazard=1；观测联合排列似然形成 gamma。

**更新：** emission 梯度 gamma[k]*(marginal_k-onehot)+1e-5*logit，SGD .03、clip8；exit/stay 是期望计数更新，随后推进 duration prior。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：区分 emission 数值梯度与 duration 期望计数；不能把两种更新合计冒充独立训练步。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S018 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF09KernelR26.java:5–31`

### F10 截断运行长度贝叶斯变点

**实现：** `OnlineModelsSU2.Bocpd(hazard=.005,alpha=1,R=160)`。

**输入：** 每个位置的旧 run-length posterior 与每个运行长度的类别计数。

**状态：** r[10*R]、C[10*R*10]；每位置独立 Dirichlet 类别模型。

**实际公式：**

- p[position,k]=Σ_r posterior[r]*(C[r,k]+alpha)/(Σ_j C[r,j]+10*alpha)。
- cp=hazard*Σ_r posterior[r]*likelihood[r]；growth[r+1]=(1-hazard)*posterior[r]*likelihood[r]；末端 R-1 合并截断尾部并归一化。

**更新：** 后验递推和计数移位后追加真实标签；无 SGD 参数更新。centralParams 取网格中间索引。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：不同 hazard/alpha/R 是该机制的参数变体，不是新算法种类。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S019 `app/src/main/java/com/ethk3/novaonepro/strategyv2/OnlineModelsSU2.java:116–164`；S020 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelEvolutionSU2.java:29–33`；S005 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelReplaySU2.java:130–167`

### F11 可学习距离的有限情节检索

**实现：** `FamilyF11KernelR26(lr=.02)`。

**输入：** previous 完整十位置开奖作为检索键；最多 256 个历史键→下一期标签情节。

**状态：** metric[10]、episode keys/labels、previous、FIFO 游标、lifetime。

**实际公式：**

- a[e]=softmax_e(-Σ_q metric[q]*1{key[e,q] != previous[q]})。
- p[p,n]=.01+.9Σ_e a[e]*1{label[e,p]=n}；无情节时均匀 .1。

**更新：** 对检索权重归一化求导，最小化边际 NLL + .5e-4||metric||²；lr .02，metric∈[.001,8]；随后 FIFO 插入 previous→draw。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：插入记忆与学习 metric 是两个不同状态变化；不能只以新增情节声称发生了梯度学习。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S021 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF11KernelR26.java:5–37`

### F12 分层上下文类别计数

**实现：** `FamilyF12KernelR26`。

**输入：** 历史关系符号 floorMod(draw[0]-draw[1],10) 的 previous/older。

**状态：** 111 个上下文的 count[111][10][10]、support[111] 与 previous/older。

**实际公式：**

- root=(count+1)/(support+10)；child=(count+10*parentProbability)/(support+10)。
- 只有一阶 support>=20 且二阶 support>=4 时使用 deep=11+older*10+previous，否则回退上层。

**更新：** 真实到达标签更新 root、previous、deep 计数，然后推进关系上下文；无梯度优化。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：计数回退是定义好的推断逻辑；缺少支持时不能把回退计为新策略产生。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S022 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF12KernelR26.java:5–36`

### F13 符号乘积特征

**实现：** `SymbolicFamilyR26("F13",formulaVariant=0)`。

**输入：** Lag(delay,offset)=(pastDigit-5.5)/4.5；三个历史开奖。

**状态：** 100×5 权重与 momentum、3 期 ring；F14 另有十位置 ema。

**实际公式：**

- 特征=[1,Lag(1,0),Lag(2,0),Lag(1,0)*Lag(1,1),第四项]；第四项为 Lag(1,3)*Lag(2,5)。
- s[p,n]=Σ_j W[p,n,j]*feature[p,j]；UNARY_PERMUTATION_HEAD。

**更新：** momentum=.9*m+.1*g，lr .03，clip8；F14 另用 ema[p]=.9*ema[p]+.1*(draw[p]-draw[p+1])/9 更新宏状态。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：formula=0/1/2 为显式符号图变体；权重微调与符号结构变异要分开计数。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S023 `app/src/main/java/com/ethk3/novaonepro/strategyv2/SymbolicFamilyR26.java:5–51`

### F14 带在线状态的符号宏

**实现：** `SymbolicFamilyR26("F14",formulaVariant=0)`。

**输入：** Lag(delay,offset)=(pastDigit-5.5)/4.5；三个历史开奖。

**状态：** 100×5 权重与 momentum、3 期 ring；F14 另有十位置 ema。

**实际公式：**

- 特征=[1,Lag(1,0),Lag(2,0),Lag(1,0)*Lag(1,1),第四项]；第四项为 Macro(relation*Lag(3,2))，Macro=tanh(body+ema[p])。
- s[p,n]=Σ_j W[p,n,j]*feature[p,j]；UNARY_PERMUTATION_HEAD。

**更新：** momentum=.9*m+.1*g，lr .03，clip8；F14 另用 ema[p]=.9*ema[p]+.1*(draw[p]-draw[p+1])/9 更新宏状态。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：formula=0/1/2 为显式符号图变体；权重微调与符号结构变异要分开计数。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S023 `app/src/main/java/com/ethk3/novaonepro/strategyv2/SymbolicFamilyR26.java:5–51`

### F15 完整 1200 动作风险读出

**实现：** `SetFamilyR26("F15")`。

**输入：** F01 十位置边际；F16 另含 F11 边际，实际标签用于 miss 风险监督。

**状态：** risk[10][57]、router[2]、F01 状态；F16 加 F11 状态；时钟。

**实际公式：**

- 枚举 10*C(10,3)=1200 动作；q=p[a]+p[b]+p[c]；risk=σ(intercept+3 singleton+3 pair+massCoefficient*q)。
- utility=q-.05*risk。

**更新：** risk 用 1200 动作上的二元 miss 梯度，步长 .1、clip8；F16 router 对平均边际 NLL 做 momentum .9/.1、lr .03；底层分支各自真实 update。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：utility 与 q 分开保留；utility 不是校准命中概率。此类未实现 TrainingDiagnosticsR26，不能自动声称已有顶层梯度见证。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S024 `app/src/main/java/com/ethk3/novaonepro/strategyv2/SetFamilyR26.java:3–22`

### F16 双分支与七层支持集风险读出

**实现：** `SetFamilyR26("F16")`。

**输入：** F01 十位置边际；F16 另含 F11 边际，实际标签用于 miss 风险监督。

**状态：** risk[10][57]、router[2]、F01 状态；F16 加 F11 状态；时钟。

**实际公式：**

- 枚举 10*C(10,3)=1200 动作；q=p[a]+p[b]+p[c]；risk=σ(intercept+3 singleton+3 pair+massCoefficient*q)。
- utility=q-.05*risk+.01*support；support 在宽度 [10,9,8,7,6,5,3] 的 sigmoid 软入选得分上求平均 log。F16 预测表为 σ(router[0])*F01+(1-σ(router[0]))*F11。

**更新：** risk 用 1200 动作上的二元 miss 梯度，步长 .1、clip8；F16 router 对平均边际 NLL 做 momentum .9/.1、lr .03；底层分支各自真实 update。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：utility 与 q 分开保留；utility 不是校准命中概率。此类未实现 TrainingDiagnosticsR26，不能自动声称已有顶层梯度见证。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S024 `app/src/main/java/com/ethk3/novaonepro/strategyv2/SetFamilyR26.java:3–22`

### F17 跨位置 lag-1 精确排列模型

**实现：** `PermutationModelR26(lr=.02)`。

**输入：** previous 的全部十位置数字。

**状态：** bias[10][10]、weights[100][10][10]、previous、updates、lastLoss。

**实际公式：**

- s[p,n]=bias[p,n]+(1/10)Σ_q weights[p*10+n,q,previous[q]-1]；UNARY_PERMUTATION_HEAD。
- g_bias=marginal-onehot+1e-4*bias；g_activeWeight=(marginal-onehot)/10+1e-4*activeWeight。

**更新：** lr .02，clip[-12,12]；L2 仅对本步 bias 与活跃 lag 坐标计入损失和更新；同预测前上下文对比 before/after。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：与 F01 共享联合读出；F17 是 lag-1 活跃坐标 L2，不能把其读出另包装成全新联合数学原理。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S025 `app/src/main/java/com/ethk3/novaonepro/strategyv2/PermutationModelR26.java:5–40`

### F18 已用数字集合的掩码自回归

**实现：** `FamilyF18KernelR26`。

**输入：** previous 同位置数字；联合生成的 used-set mask。训练沿真实目标前缀；推断对所有集合传播概率。

**状态：** bias[10][10]、context[100][10]、prefix[10][10]、previous、t。

**实际公式：**

- 对未用 n：score=bias[p,n]+lagContext+Σ_used prefix[n,used]/p；在未用数字上 softmax。
- mass[0]=1；mass[mask∪{n}]+=mass[mask]*conditional(mask,n)；累积得到位置边际，状态数 1024。
- 训练损失 -Σ_p log conditional(truePreviousMask,y[p])，梯度是每一步 masked probability-onehot。

**更新：** SGD .03，bias/context/prefix 参数 clip8，全部暂存完成后提交。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：这是真实 masked 自回归条件分解，不等于 F01 的 unary 联合排列分布。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S026 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF18KernelR26.java:3–25`

### F19 CP 张量因子化精确排列

**实现：** `FamilyF19TensorR26(.02,true,false)`。

**输入：** L=3 的完整历史开奖；position mode 打开，state mode 关闭。

**状态：** R=3；U(位置)、V(数字)、A(源位置)、B(滞后数字)、C(lag)、可选 D(state)；每因子的 AdaGrad 累积器、ring 和时钟。

**实际公式：**

- h[k]=Σ_(lag,q) A[q,k]*B[pastDigit,k]*C[lag,k]；s[p,n]=Σ_k U[p,k]*V[n,k]*h[k]。
- UNARY_PERMUTATION_HEAD；梯度通过所有启用因子反传，未启用 D 不更新。

**更新：** a_new=a+g²；`θ_new=clip[-3,3](θ-.02*g/(sqrt(a_new)+1e-8)-.02*1e-5*θ)`；暂存并检查 finite。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：默认 F19 dispatch 是 FamilyF19TensorR26，旧 FamilyF19KernelR26 不能当当前默认实现。默认 state=NONE，不能宣称默认启用了状态因子。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S027 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF19TensorR26.java:5–74`

### F20 因果状态片段检索

**实现：** `FamilyF20KernelR26(slots=16,retrieval=MATCH)`。

**输入：** 最近 8 个标签的十位置频率与最新开奖 onehot 形成 200 维签名。

**状态：** 全局类别频率、最多16个片段 signature/expert、每片段更新/使用时钟、recent[8][10]。

**实际公式：**

- distance=Σ_i(signature_i-query_i)²/20；有效最近片段距离<=.35 时 blend=.9*updates/(updates+8)*exp(-3*distance)。
- p=(1-blend)*normalizedGlobal+blend*normalizedFragment；历史不足或无有效片段时使用全局表。

**更新：** 8 期后允许片段；距离>.18 时分配；signature 使用 1/min(64,updates+1) 的 EMA；片段计数*.999 后加 onehot；global*.97 后加 onehot。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：真实默认窗口是8，不是32/128/512/2048；计数/记忆更新不能包装成 SGD。MATCH/NONE/MISMATCH/EXPIRED 是检索控制或消融。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S028 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF20KernelR26.java:13–58`；S029 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF20KernelR26.java:84–111`

### F21 折扣关系贝叶斯统计

**实现：** `FamilyF21KernelR26 -> OnlineModelsSU2.RelBayes`。

**输入：** 默认 lags=(1,2)；各源位置预测 src、src+1、src-1、mirror 四种数字关系。

**状态：** 成功 s、暴露 n、历史 recent、时钟；decay=.995,kappa=100,positiveOnly=true,temp=1。

**实际公式：**

- rho=(s+.1*kappa)/(n+kappa)；w=max(0,log(rho/.1)/temp)；按被预测数字累加 w 后逐行 softmax。
- s_new=.995*s+1{relationCorrect}；n_new=.995*n+1。

**更新：** 仅有足够 lag 上下文后更新统计计数；StatisticalTrainingDiagnostics 参数变化是 Beta posterior means。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：无 SGD；warmup 仅推进上下文时不能加为有效关系训练次数。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S030 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF21KernelR26.java:5–35`；S031 `app/src/main/java/com/ethk3/novaonepro/strategyv2/OnlineModelsSU2.java:400–448`；S005 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelReplaySU2.java:130–167`

### F22 冻结父模型后的门控残差

**实现：** `ResidualFamilyR26`。

**输入：** 新建 F01 的预测前上下文，previous 与 drift；先128步 bootstrap。

**状态：** 新 F01 parent、residual[100][3]、gate[2]、drift[10]、previous、frozenParentHash、时钟。

**实际公式：**

- f[p]=[1,(previous[p]-5.5)/4.5,drift[p]]；g=σ(gate[0]+gate[1]*mean(abs(drift)))。
- bootstrap 后 s=parent.scores+g*residual*f；UNARY_PERMUTATION_HEAD。
- drift_new=.9*drift+.1*(draw-previous)/9。

**更新：** 前128步只训练新 F01；之后冻结 parent 参数哈希，只 observeOnly 推上下文；residual SGD .02、gate SGD .01，clip8。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：父模型是当前新建 F01，不是继承某个旧赢家的质量；父证据不能传给残差子代。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S032 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResidualFamilyR26.java:3–35`

### F23 明确绑定旧 VM 教师的蒸馏学生

**实现：** `LegacyTeacherOwnerR26.buildBoundR31 -> DistillationFamilyR26`。

**输入：** 明确哈希绑定的旧 VM 教师偏好投影（非已校准命中概率）+previous 同位置上下文+新标签。

**状态：** bias[10][10]、context[100][10]、previous、时钟、教师身份；teacher/asset/state 哈希与资源租约。

**实际公式：**

- s=bias+同位置 lag context；UNARY_PERMUTATION_HEAD。
- 有合法双随机教师边际时 q=.65*onehot+.35*teacher；否则该步 alpha=0，只用真实标签。
- loss=logZ-Σ_(p,n) q[p,n]*s[p,n]；g=marginal-q。

**更新：** SGD .02，clip8；提交前再次核对教师身份；teacher 缺绑定时不创建 F23。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：F23 最初不在 AVAILABLE；bindLegacyTeacher 成功后才加入。不能把未绑定教师的家族当作已运行，也不能把教师偏好当胜率标签。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S033 `app/src/main/java/com/ethk3/novaonepro/LegacyTeacherOwnerR26.java:45–47`；S034 `app/src/main/java/com/ethk3/novaonepro/strategyv2/DistillationFamilyR26.java:5–48`

### F24 三个独立模型的上下文软路由

**实现：** `PortfolioFamilyR26`。

**输入：** F01、F09、F11 的各自预测；过去输出熵与过去损失作为门控上下文。

**状态：** 三个完整模型状态、gate[3][3]、momentum[3][3]、entropy、previousLoss、t。

**实际公式：**

- x=[1,entropy,clip(previousLoss-2.302585,-2,2)]；w=softmax(gate*x)；p=Σ_m w[m]*p_m。
- 门控 logit 梯度为十位置平均 -w[m]*(p_m[y]/p[y]-1)。

**更新：** 门控 momentum .9/.1、lr .04、clip8；三个成员随后各自 update。

**评估与消费：** 见 R26_COMMON 和 R26_MAINLINE_COMMON。

- 边界：组合具有独立成员状态，但不是三份新的底层算法。类本身没有 TrainingDiagnosticsR26 顶层见证接口。

**源码证据：** S001 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:6–39`；S035 `app/src/main/java/com/ethk3/novaonepro/strategyv2/PortfolioFamilyR26.java:3–16`

## 4. 15 个 modern 家族总览

| 家族 | 实际 ModelKind | 更新种类 | 核心区别 |
|---|---|---|---|
| FREQUENCY | CandidateCompilerR952.Modern.FIXED_GRAPH | FIXED_NUMERICAL_RULE | 窗口频率固定规则 |
| OMISSION | CandidateCompilerR952.Modern.FIXED_GRAPH | FIXED_NUMERICAL_RULE | 有界遗漏固定规则 |
| MARKOV | CandidateCompilerR952.Modern.FIXED_GRAPH | FIXED_NUMERICAL_RULE | 同位置一阶平滑转移 |
| TRANSITION | CandidateCompilerR952.Modern.FIXED_GRAPH | FIXED_NUMERICAL_RULE | 相邻源位置到目标位置的跨位置转移 |
| STATISTICAL | CandidateCompilerR952.Modern.FIXED_GRAPH | FIXED_NUMERICAL_RULE | 频率与熵加权遗漏组合 |
| RULE | CandidateCompilerR952.Modern.FIXED_GRAPH | FIXED_NUMERICAL_RULE | 频率阈值条件公式 |
| REGIME | CandidateCompilerR952.Modern.FIXED_GRAPH | FIXED_NUMERICAL_RULE | 五种因果状态混合固定规则 |
| LINEAR | CandidateCompilerR952.Modern.SOFTMAX_LINEAR | GRADIENT_SGD | 多类别线性 softmax |
| LOGISTIC | CandidateCompilerR952.Modern.OVR_LOGISTIC | GRADIENT_SGD | 十个 one-vs-rest logistic |
| TREE | CandidateCompilerR952.Modern.HISTOGRAM_TREE | SUPERVISED_TREE_SPLITS | 有界直方图决策树 |
| NEURAL | CandidateCompilerR952.Modern.MLP_D32_16_10 | GRADIENT_ADAM | 两隐藏层 ReLU 网络 |
| HYBRID | CandidateCompilerR952.Modern.SOFTMAX_LINEAR | GRADIENT_SGD_WITH_INTERACTION | 增加显式交互项的线性 softmax |
| BANDIT | CandidateCompilerR952.Modern.EXPERT_HEDGE | EXPONENTIATED_EXPERT_WEIGHTS | 双冻结教师的 Hedge 混合（标签 BANDIT） |
| EXPERT | CandidateCompilerR952.Modern.EXPERT_HEDGE | EXPONENTIATED_EXPERT_WEIGHTS | 双冻结教师的 Hedge 混合 |
| ENSEMBLE | CandidateCompilerR952.Modern.EXPERT_HEDGE | EXPONENTIATED_EXPERT_WEIGHTS | 三冻结教师的 Hedge 混合 |

## 5. 15 个 modern 家族逐项绑定

**固定图归一化约定：** 本清单 N(v) 指 TypedOperationsCP4.NORMALIZE_POSITIVE：全10项 presence=true 且 v≥0 时 (v_i+1e-9)/(Σv+1e-8)；缺任一项则全向量保持缺失。FIXED_GRAPH inference 再执行 (x_i+1e-9)/Σ_j(x_j+1e-9)。

**证据：** S041 `app/src/main/java/com/ethk3/novaonepro/r952/TypedOperationsCP4.java:41–64`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`

### FREQUENCY 窗口频率固定规则

**实现：** `CandidateCompilerR952.Modern.FIXED_GRAPH`。

**输入：** 固定图使用截至预测时可用的 atom/matrix 数据与缺失 mask；缺失不会凭空变成观测值。

**状态：** AST3 不可变图、feature manifest、矩阵引用、source/hash；模型 weights 长度0。窗口统计属于输入状态，不是模型训练权重。

**实际公式：**

- f[k]=窗口内 digit=k 的频数 / count；output=N(f)。

**更新：** DevelopmentTrainerCP9 TRAIN 遍历训练行但跳过 gradient/update；随后120条验证。每次重算图读取已推进的 PIT 统计，不能把 rowsVisited 当数值权重训练步。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：window/position 变体不等于独立新机制。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S037 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:134–161`；S039 `app/src/main/java/com/ethk3/novaonepro/r952/AtomEvaluatorCP4.java:37–71`；S040 `app/src/main/java/com/ethk3/novaonepro/r952/AtomWindowCP4.java:59–94`；S041 `app/src/main/java/com/ethk3/novaonepro/r952/TypedOperationsCP4.java:41–64`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`

### OMISSION 有界遗漏固定规则

**实现：** `CandidateCompilerR952.Modern.FIXED_GRAPH`。

**输入：** 固定图使用截至预测时可用的 atom/matrix 数据与缺失 mask；缺失不会凭空变成观测值。

**状态：** AST3 不可变图、feature manifest、矩阵引用、source/hash；模型 weights 长度0。窗口统计属于输入状态，不是模型训练权重。

**实际公式：**

- o[k]=log(1+abs(min(omission[k],w)))；output=N(o)。

**更新：** DevelopmentTrainerCP9 TRAIN 遍历训练行但跳过 gradient/update；随后120条验证。每次重算图读取已推进的 PIT 统计，不能把 rowsVisited 当数值权重训练步。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：遗漏是确定统计变换，无权重学习。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S037 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:134–161`；S039 `app/src/main/java/com/ethk3/novaonepro/r952/AtomEvaluatorCP4.java:37–71`；S046 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:296–314`；S041 `app/src/main/java/com/ethk3/novaonepro/r952/TypedOperationsCP4.java:41–64`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`

### MARKOV 同位置一阶平滑转移

**实现：** `CandidateCompilerR952.Modern.FIXED_GRAPH`。

**输入：** 固定图使用截至预测时可用的 atom/matrix 数据与缺失 mask；缺失不会凭空变成观测值。

**状态：** AST3 不可变图、feature manifest、矩阵引用、source/hash；模型 weights 长度0。窗口统计属于输入状态，不是模型训练权重。

**实际公式：**

- p[k]=(count(latestDigit→k)+.5)/(Σ_j count(latestDigit→j)+5)；output=N(p)。

**更新：** DevelopmentTrainerCP9 TRAIN 遍历训练行但跳过 gradient/update；随后120条验证。每次重算图读取已推进的 PIT 统计，不能把 rowsVisited 当数值权重训练步。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：factory 指定 order=1 与 alpha=.5；不能把该默认规则写成任意阶马尔可夫搜索。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S037 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:134–161`；S039 `app/src/main/java/com/ethk3/novaonepro/r952/AtomEvaluatorCP4.java:37–71`；S040 `app/src/main/java/com/ethk3/novaonepro/r952/AtomWindowCP4.java:59–94`；S046 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:296–314`；S041 `app/src/main/java/com/ethk3/novaonepro/r952/TypedOperationsCP4.java:41–64`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`

### TRANSITION 相邻源位置到目标位置的跨位置转移

**实现：** `CandidateCompilerR952.Modern.FIXED_GRAPH`。

**输入：** 固定图使用截至预测时可用的 atom/matrix 数据与缺失 mask；缺失不会凭空变成观测值。

**状态：** AST3 不可变图、feature manifest、矩阵引用、source/hash；模型 weights 长度0。窗口统计属于输入状态，不是模型训练权重。

**实际公式：**

- sourcePosition=position%10+1，lag=1；TransitionMatrix 的 latestSmoothedRow 对应预测时源位置最新数字的行。
- 每行使用 .5 平滑；output=N(crossTransitionRow)。

**更新：** DevelopmentTrainerCP9 TRAIN 遍历训练行但跳过 gradient/update；随后120条验证。每次重算图读取已推进的 PIT 统计，不能把 rowsVisited 当数值权重训练步。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：占位 atom 节点绑定到 TransitionMatrix 后实际由矩阵投影取值，不能误报为普通 frequency 图。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S037 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:134–161`；S046 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:296–314`；S047 `app/src/main/java/com/ethk3/novaonepro/r952/MatrixBindingsCP12.java:23–35`；S048 `app/src/main/java/com/ethk3/novaonepro/r952/MatrixProjectionCP12.java:141–150`；S041 `app/src/main/java/com/ethk3/novaonepro/r952/TypedOperationsCP4.java:41–64`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`

### STATISTICAL 频率与熵加权遗漏组合

**实现：** `CandidateCompilerR952.Modern.FIXED_GRAPH`。

**输入：** 固定图使用截至预测时可用的 atom/matrix 数据与缺失 mask；缺失不会凭空变成观测值。

**状态：** AST3 不可变图、feature manifest、矩阵引用、source/hash；模型 weights 长度0。窗口统计属于输入状态，不是模型训练权重。

**实际公式：**

- H=-Σ_k f[k]log f[k]；raw=f+.25*(H/log10)*o；output=N(raw)，o 为有界遗漏 log1p 变换。

**更新：** DevelopmentTrainerCP9 TRAIN 遍历训练行但跳过 gradient/update；随后120条验证。每次重算图读取已推进的 PIT 统计，不能把 rowsVisited 当数值权重训练步。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：.25 为固定系数，不是已训练融合权重。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S037 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:134–161`；S039 `app/src/main/java/com/ethk3/novaonepro/r952/AtomEvaluatorCP4.java:37–71`；S046 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:296–314`；S041 `app/src/main/java/com/ethk3/novaonepro/r952/TypedOperationsCP4.java:41–64`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`

### RULE 频率阈值条件公式

**实现：** `CandidateCompilerR952.Modern.FIXED_GRAPH`。

**输入：** 固定图使用截至预测时可用的 atom/matrix 数据与缺失 mask；缺失不会凭空变成观测值。

**状态：** AST3 不可变图、feature manifest、矩阵引用、source/hash；模型 weights 长度0。窗口统计属于输入状态，不是模型训练权重。

**实际公式：**

- raw[k]= f[k] if f[k]>=.1 else log1p(abs(min(omission[k],w)))；output=N(raw)。

**更新：** DevelopmentTrainerCP9 TRAIN 遍历训练行但跳过 gradient/update；随后120条验证。每次重算图读取已推进的 PIT 统计，不能把 rowsVisited 当数值权重训练步。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：阈值 .1 固定；家族名 RULE 不意味着运行时能自动发明任意公式。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S037 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:134–161`；S039 `app/src/main/java/com/ethk3/novaonepro/r952/AtomEvaluatorCP4.java:37–71`；S041 `app/src/main/java/com/ethk3/novaonepro/r952/TypedOperationsCP4.java:41–64`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`

### REGIME 五种因果状态混合固定规则

**实现：** `CandidateCompilerR952.Modern.FIXED_GRAPH`。

**输入：** 固定图使用截至预测时可用的 atom/matrix 数据与缺失 mask；缺失不会凭空变成观测值。

**状态：** AST3 不可变图、feature manifest、矩阵引用、source/hash；模型 weights 长度0。窗口统计属于输入状态，不是模型训练权重。

**实际公式：**

- h=H(current)/log10，j=JS(current(.5 prior),previousWindow(.5 prior))，c=lag1 digitCorrelation（ZERO_VARIANCE→0）。
- r=softmax([2(1-h),2h,2max(0,c),2max(0,-c),4j])。
- vectors=[frequency20,frequency_w,Markov_w,omission_w,crossTransition_w]；output=N(Σ_i r[i]*N(vectors[i]))。

**更新：** DevelopmentTrainerCP9 TRAIN 遍历训练行但跳过 gradient/update；随后120条验证。每次重算图读取已推进的 PIT 统计，不能把 rowsVisited 当数值权重训练步。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：这些 regime gate 是确定特征计算，不是另外训练了五个状态模型。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S037 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:134–161`；S039 `app/src/main/java/com/ethk3/novaonepro/r952/AtomEvaluatorCP4.java:37–71`；S046 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:296–314`；S041 `app/src/main/java/com/ethk3/novaonepro/r952/TypedOperationsCP4.java:41–64`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`

### LINEAR 多类别线性 softmax

**实现：** `CandidateCompilerR952.Modern.SOFTMAX_LINEAR`。

**输入：** 已验证父图的标量特征 + 短/中/长期 bank；≤32 标量，输入维度 d=2F（值和缺失 mask）。train-only z-score。

**状态：** 不可变 AST3/manifest、TRAIN 统计 mean/std、模型参数、训练收据/截止期；训练 checkpoint 另持有梯度累积、优化器状态、验证 tape。

**实际公式：**

- z=W*x+b；p=softmax(z)；L=-log p[y]；∂L/∂z=p-onehot。

**更新：** batch≤32；g=平均样本梯度+1e-4*W（仅权重不含bias）；全局范数裁剪1；SGD lr .01。TRAIN 中计算 mean/std，验证集不更新标准化。最多20 epoch，验证 CE 改善需>1e-6，patience=3。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：LINEAR 与 HYBRID 共用本 kernel；需要用特征图差异证明 HYBRID 差别。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S038 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:71–100`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`；S043 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:265–288`；S044 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java:106–163`

### LOGISTIC 十个 one-vs-rest logistic

**实现：** `CandidateCompilerR952.Modern.OVR_LOGISTIC`。

**输入：** 已验证父图的标量特征 + 短/中/长期 bank；≤32 标量，输入维度 d=2F（值和缺失 mask）。train-only z-score。

**状态：** 不可变 AST3/manifest、TRAIN 统计 mean/std、模型参数、训练收据/截止期；训练 checkpoint 另持有梯度累积、优化器状态、验证 tape。

**实际公式：**

- s[k]=sigmoid(W[k]*x+b[k])；推断先将 s 裁剪 [1e-12,1-1e-12]，再 p=s/Σs。
- 训练 L=Σ_k(max(z[k],0)-1{k=y}*z[k]+log1p(exp(-abs(z[k]))))；梯度 sigmoid(z)-onehot。

**更新：** batch≤32；g=平均样本梯度+1e-4*W（仅权重不含bias）；全局范数裁剪1；SGD lr .01。TRAIN 中计算 mean/std，验证集不更新标准化。最多20 epoch，验证 CE 改善需>1e-6，patience=3。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：训练是十分类 one-vs-rest BCE，不能错写成与 LINEAR 相同的 softmax CE。验证仍使用归一化预测的 CE/Brier/top3。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S038 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:71–100`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`；S043 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:265–288`；S044 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java:106–163`

### TREE 有界直方图决策树

**实现：** `CandidateCompilerR952.Modern.HISTOGRAM_TREE`。

**输入：** 已验证父图的标量特征 + 短/中/长期 bank；≤32 标量，输入维度 d=2F（值和缺失 mask）。train-only z-score。

**状态：** 不可变 AST3/manifest、TRAIN 统计 mean/std、模型参数、训练收据/截止期；训练 checkpoint 另持有梯度累积、优化器状态、验证 tape。；treeFeature/cut/left/right/leafP 保存树，不用 gradient weights。

**实际公式：**

- 每个特征取 TRAIN 的15个分位候选切点；gain=Gini(parent)-(nL*Gini(L)+nR*Gini(R))/(nL+nR)。
- 深度≤4，节点≤31，节点至少40样本才分裂，左右叶各≥20，gain>1e-12；叶 p[k]=(count[k]+.5)/(n+5)。

**更新：** TRAIN 扫描计数与分裂；单轮树训练后120条验证；不得把零 weight 长度当没有训练树。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：候选切点只来自 TRAIN；树的结构学习与梯度更新次数要分别记账。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S038 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:71–100`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`；S044 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java:106–163`；S045 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java:165–199`

### NEURAL 两隐藏层 ReLU 网络

**实现：** `CandidateCompilerR952.Modern.MLP_D32_16_10`。

**输入：** 已验证父图的标量特征 + 短/中/长期 bank；≤32 标量，输入维度 d=2F（值和缺失 mask）。train-only z-score。

**状态：** 不可变 AST3/manifest、TRAIN 统计 mean/std、模型参数、训练收据/截止期；训练 checkpoint 另持有梯度累积、优化器状态、验证 tape。

**实际公式：**

- h1=ReLU(W1*x+b1)（32维）；h2=ReLU(W2*h1+b2)（16维）；p=softmax(W3*h2+b3)（10类）。
- L=-log p[y]；输出梯度 p-onehot，经 ReLU 激活条件反传。

**更新：** Xavier uniform 初始化；batch≤32、权重 L2=1e-4、梯度范数clip1；Adam lr .001、β1=.9、β2=.999、ε=1e-8，含偏差校正。与 learned 公共验证/早停相同。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：该现代 NEURAL 是 MLP，不等于 F03–F07 五种时序神经图。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S038 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:71–100`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`；S043 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:265–288`；S044 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java:106–163`

### HYBRID 增加显式交互项的线性 softmax

**实现：** `CandidateCompilerR952.Modern.SOFTMAX_LINEAR`。

**输入：** 已验证父图的标量特征 + 短/中/长期 bank；≤32 标量，输入维度 d=2F（值和缺失 mask）。train-only z-score。

**状态：** 不可变 AST3/manifest、TRAIN 统计 mean/std、模型参数、训练收据/截止期；训练 checkpoint 另持有梯度累积、优化器状态、验证 tape。

**实际公式：**

- 先保留最多32个标量父特征和 bank；另加 firstNumericFeature*frequency(digit=variant%10,window=50)。
- 在新特征向量上 z=W*x+b，p=softmax(z)，L=-log p[y]。

**更新：** batch≤32；g=平均样本梯度+1e-4*W（仅权重不含bias）；全局范数裁剪1；SGD lr .01。TRAIN 中计算 mean/std，验证集不更新标准化。最多20 epoch，验证 CE 改善需>1e-6，patience=3。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：与 LINEAR 的优化器/网络相同；实际区别是 AST 的乘积交互节点。仅换 family 标签不能算新机制。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S038 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:71–100`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`；S043 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:265–288`；S044 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java:106–163`

### BANDIT 双冻结教师的 Hedge 混合（标签 BANDIT）

**实现：** `CandidateCompilerR952.Modern.EXPERT_HEDGE`。

**输入：** 精确冻结教师的十类概率及身份；教师不在本步骤重新训练。

**状态：** 教师 freezeHash/modelHash/featureManifestHash/position + 2 个学生 logWeight；教师原模型只读。

**实际公式：**

- 需要 2 个同位置、不同 freezeHash 的精确冻结教师；w=softmax(logWeights)；p=Σ_e w[e]*teacherP[e]。
- logWeight[e] += .05*log(max(1e-15,teacherP[e,y]))；各权重减去最大值稳定化。

**更新：** 真实标签上进行一遍乘法权重/Hedge 更新，再120条验证；非 SGD，不在此步骤训练教师。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：BANDIT/EXPERT/ENSEMBLE 当前共享同一 EXPERT_HEDGE kernel；不能宣称 BANDIT 已实现独立的探索臂选择或 bandit reward 更新。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S037 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:134–161`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`；S044 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java:106–163`

### EXPERT 双冻结教师的 Hedge 混合

**实现：** `CandidateCompilerR952.Modern.EXPERT_HEDGE`。

**输入：** 精确冻结教师的十类概率及身份；教师不在本步骤重新训练。

**状态：** 教师 freezeHash/modelHash/featureManifestHash/position + 2 个学生 logWeight；教师原模型只读。

**实际公式：**

- 需要 2 个同位置、不同 freezeHash 的精确冻结教师；w=softmax(logWeights)；p=Σ_e w[e]*teacherP[e]。
- logWeight[e] += .05*log(max(1e-15,teacherP[e,y]))；各权重减去最大值稳定化。

**更新：** 真实标签上进行一遍乘法权重/Hedge 更新，再120条验证；非 SGD，不在此步骤训练教师。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：BANDIT/EXPERT/ENSEMBLE 当前共享同一 EXPERT_HEDGE kernel；不能宣称 BANDIT 已实现独立的探索臂选择或 bandit reward 更新。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S037 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:134–161`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`；S044 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java:106–163`

### ENSEMBLE 三冻结教师的 Hedge 混合

**实现：** `CandidateCompilerR952.Modern.EXPERT_HEDGE`。

**输入：** 精确冻结教师的十类概率及身份；教师不在本步骤重新训练。

**状态：** 教师 freezeHash/modelHash/featureManifestHash/position + 3 个学生 logWeight；教师原模型只读。

**实际公式：**

- 需要 3 个同位置、不同 freezeHash 的精确冻结教师；w=softmax(logWeights)；p=Σ_e w[e]*teacherP[e]。
- logWeight[e] += .05*log(max(1e-15,teacherP[e,y]))；各权重减去最大值稳定化。

**更新：** 真实标签上进行一遍乘法权重/Hedge 更新，再120条验证；非 SGD，不在此步骤训练教师。

**评估与消费：** 见 MODERN_COMMON 和 MODERN_MAINLINE_COMMON。

- 边界：BANDIT/EXPERT/ENSEMBLE 当前共享同一 EXPERT_HEDGE kernel；不能宣称 BANDIT 已实现独立的探索臂选择或 bandit reward 更新。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S037 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:134–161`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`；S044 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java:106–163`

## 6. 共同数学、训练、评估、主链绑定

### UNARY_PERMUTATION_HEAD

P(π|history)=exp(Σ_p s[p,π_p])/Z，π 遍历十数字合法排列；L=logZ-Σ_p s[p,y_p]；∂L/∂s[p,n]=marginal[p,n]-1{y_p=n}。精确子集动态规划 O(n·2^n)。K3 概率为同一位置三个互斥数字边际之和。

**boundary：** 只对 unary permutation energy 适用；F02 成对势链和 F18 used-set 自回归分别用自己的联合模型。

**源码证据：** S002 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ExactPermutationR26.java:5–25`

### R26_COMMON

1. FamilyRegistryR26.genome -> ModelGenomeSU2
2. ModelLiveSU2.Hub.model -> ModelReplaySU2.build -> 实际 F kernel
3. FoundryCampaignR26.step: live.decide/pendingTable -> label settle -> hub.update -> 完整checkpoint序列化 -> 带fence/CAS事务发布
4. 历史开发/选择按 3/5 划分；前32条不累计质量指标；HistoryRankR32 只选 RESEARCH 候选
5. FamilyRegistryR26.acceptR31 是 predict-before-update + 序列化/恢复一致性验收，不能证明胜率或独立前瞻质量

**qualityBoundary：** 训练 checkpoint、家族 EXECUTED、历史 dev/select 排名都不是 REAL 授权。数据缺口需 reset 或明确 BLOCKED。

**源码证据：** S003 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java:40–88`；S004 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelLiveSU2.java:20–80`；S005 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelReplaySU2.java:130–167`；S006 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java:163–210`；S007 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java:287–318`

### R26_MAINLINE_COMMON

1. LiveStateR26.learn -> decide/settle/hub.update 维护可变研究状态
2. MainlineBridgeSU2.mainline intent 读取 MainlineChoiceStoreR31.authorizedChoice
3. UnifiedResearchChoiceR932 精确 certificate/typed proof 绑定 -> CertifiedPredictionR974 -> DecisionGateEngine -> DbStore 插入前重验

**boundary：** F 家族预测/训练类没有直接 REAL writer；必须展示 policy/model/certificate/target 与实际出手 receipt 的同一身份链。

**源码证据：** S008 `app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveStateR26.java:60–65`；S009 `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineBridgeSU2.java:129–155`；S010 `app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java:150–162`；S058 `app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java:263–310`；S060 `app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java:11304–11326`；S059 `app/src/main/java/com/ethk3/novaonepro/DbStore.java:5375–5396`

### MODERN_COMMON

1. ModernFamilyFactoryCP12.prepare/newFamilyFromParentCP12 编译 AST3
2. DevelopmentProductionCP12.prepareBatch 准备有界 PIT 数据集
3. ResearchTrainingHookRegistryR952.prepareNumericsCP9 -> DevelopmentTrainerCP9.step
4. ResearchDirectorR952.commitCandidateBatchCP11 提交 checkpoint/receipt 并入队 FREEZE_CANDIDATE
5. FrozenCandidateAdapterR958.commitDevelopmentFreezeCP8 -> ResearchEvaluationRunR967.registerDevelopmentCP8
6. 后续精确冻结 captureOneCP12 -> CandidateCalibrationR932 / FixedResearchWindowR932

**trainingSplit：** TRAIN 300–1200行、VALIDATION 120行；TRAIN/VALIDATION 之间源码要求跳过一个 target（embargo）。TRAIN-only Welford mean/std；缺失保留 mask；每个特征全缺失则拒绝。

**evaluation：** 120条验证计算平均 CE、平均每类 Brier 与稳定排序 top3 hit；这是开发验证，不代替自然前瞻 CAL/QA 与证书。

**baselineOnlineGap：** 已安装源码的 ResearchDirector generic predraw 两查询排除 AST3，CandidateCompiler.Model.update 对 modern 抛 CP4_BATCH_TRAINING_OWNER_REQUIRED。开发批训练实现不能用来证明现代持续实时数值训练已闭环。此方案的新增 rolling owner 必须另验收。

**源码证据：** S036 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java:33–57`；S049 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java:20–52`；S051 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java:198–259`；S050 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchTrainingHookRegistryR952.java:53–76`；S044 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java:106–163`；S052 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java:420–463`；S053 `app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java:197–228`；S054 `app/src/main/java/com/ethk3/novaonepro/ResearchEvaluationRunR967.java:156–174`；S055 `app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java:697–768`；S062 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java:11–40`；S061 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:414–417`

### MODERN_MAINLINE_COMMON

1. 精确不可变冻结模型 + 校准/固定窗口质量证据
2. CertificateBundleServiceR929/平台证书原 owner
3. UnifiedResearchChoiceR932 选择获准 exact frozen candidate
4. CertifiedPredictionR974.prepare 重算相同模型动作与概率
5. DecisionGateEngine 授权，DbStore.beforeInsert 重验后才可能真实插入

**boundary：** 证书服务本身 MAY_CREATE_REAL=false，数值模型也没有出手权限；质量通过与真实动作还需原始消费证据。

**源码证据：** S056 `app/src/main/java/com/ethk3/novaonepro/CertificateBundleServiceR929.java:22–37`；S057 `app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java:111–145`；S058 `app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java:263–310`；S060 `app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java:11304–11326`；S059 `app/src/main/java/com/ethk3/novaonepro/DbStore.java:5375–5396`

### MODERN_FIXED_NORMALIZATION

本清单 N(v) 指 TypedOperationsCP4.NORMALIZE_POSITIVE：全10项 presence=true 且 v≥0 时 (v_i+1e-9)/(Σv+1e-8)；缺任一项则全向量保持缺失。FIXED_GRAPH inference 再执行 (x_i+1e-9)/Σ_j(x_j+1e-9)。

**boundary：** 两次平滑是源码实际行为；不能把 N 简化成允许负数、允许缺失值的任意 softmax。

**源码证据：** S041 `app/src/main/java/com/ethk3/novaonepro/r952/TypedOperationsCP4.java:41–64`；S042 `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java:234–253`

## 7. 本清单对应的施工验收

1. 对每个实际启用的图/模型，冻结 kernel、AST、feature manifest、训练规则、参数与状态身份；F23 额外冻结教师身份。当前不可用家族记依赖阻塞，并给准确原因为何不能进入训练。
2. 历史训练收据必须分别保存数值梯度、树结构、统计后验、固定规则评估四种过程。固定图零参数更新是正确语义，不应伪造优化器步数。
3. 每次候选产生需要 parent、操作、前后 graph/model hash、去重判据、真实输入与发布事务；参数改变和新图结构分栏，不能刷变体数量满足“多种数学机制”。
4. 现代持续训练要在新增 rolling owner 上验收自然到达标签、精确 PIT、不可变旧冻结、后继训练状态、checkpoint CAS、重复事件幂等与训练/结算收据。已有开发批训练与旧 legacy 更新不能顶替。
5. 资格使用独立自然前瞻 CAL/QA。进入主链应证明 exact policy/model/certificate/target/action/Trade receipt 一致；不得因目标要求“高胜率”降低原始质量门槛。
6. 每张研究图只消费上述原始生产/训练/评估/结算事实。无事实时显示缺依赖或未知，不能补样本、复制历史 hash 或混用不同 source scope。

## 8. 证据文件哈希

| 源码文件 | SHA-256 |
|---|---|
| `app/src/main/java/com/ethk3/novaonepro/CertificateBundleServiceR929.java` | `7e07152d224e4342e4a7f95e200f52fd6b12c5bde2022c19393437c440b436e2` |
| `app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java` | `c3f325f57adcfac98cf2c6b497bac1821c7d4e46fd2aa856c8f790fc28832470` |
| `app/src/main/java/com/ethk3/novaonepro/DbStore.java` | `bb2f1db7760d53189d5cfd058fe6bf0041dc7d0b68769a0c2fa391ee6949408d` |
| `app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java` | `160d2203723ad2bb12c0165b98524bf400b2566861a0e86a978810fb6e4837d0` |
| `app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java` | `b91a8f71ec8b1543867eb6a260a63d53cfd157bc31fba97aac548577211cb0a6` |
| `app/src/main/java/com/ethk3/novaonepro/LegacyTeacherOwnerR26.java` | `b6461d719a80beecab6a0258e550dd8146db07d4d284644753d8f9ef758135a9` |
| `app/src/main/java/com/ethk3/novaonepro/ResearchEvaluationRunR967.java` | `7e05211ba09a5748663e805d9ec5228666e8932bea8bff861502f3fd5a72b5f2` |
| `app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java` | `d8df77be1b214d8f6d3af9d5615bd82a6a44f228a8f3cd3806a1619eeb7da75d` |
| `app/src/main/java/com/ethk3/novaonepro/r952/AtomEvaluatorCP4.java` | `a53ff9573353c01f12febeb988c0a81be7adbb94b8fdf37d4459298513b6f82d` |
| `app/src/main/java/com/ethk3/novaonepro/r952/AtomWindowCP4.java` | `81ace6410904da52df6f7529f45e23e36c8d528cb4fe16262c9b1e0402d14dc8` |
| `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java` | `200c062dae290825d78cf32db3a7a7c76c93b2b71bb86d5857212130de3d7d2f` |
| `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java` | `384cad2bdc5231b08070600c4a22e7584ba627d87d93322f1f9bbe9546e3bc5c` |
| `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java` | `96c9b80b9b5510c555492301ac467da65bd0593c04101bb0b9bf99142e4e1ee7` |
| `app/src/main/java/com/ethk3/novaonepro/r952/MatrixBindingsCP12.java` | `e129ecdca0e5cee31fddfa8c46f7cc10b057f408c3ff0cfbfdca1967b6c34fb6` |
| `app/src/main/java/com/ethk3/novaonepro/r952/MatrixProjectionCP12.java` | `4efa68e599f51a5aa33ba9ef9d3895d941ec15377c37e054767e32585c58a5a9` |
| `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java` | `52ce918ba368a3c9600fd03b0604473cd19760fdb4a1d9890affa58b131b3a35` |
| `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java` | `f256335b6cbadd49c575c1d01207a1da8616155b3d35ca8fec7492a96a1cf5eb` |
| `app/src/main/java/com/ethk3/novaonepro/r952/ResearchTrainingHookRegistryR952.java` | `2c96882963d15d6c8c171977d4b8ac2aee7329a7dafa801043e0e7e961840323` |
| `app/src/main/java/com/ethk3/novaonepro/r952/TypedOperationsCP4.java` | `8f724d4e3e6024bb2365cca267b78601c5b4e69ee92611e797be90d5722f7e52` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/DistillationFamilyR26.java` | `cd0836470252c4fbdac7930b3336b55910da253c21fe34a4290fb68b58afe6df` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ExactPermutationR26.java` | `5e56dd7e6d6be8f1bb9bac79786a564a8be6c9c64d4d6e67c5b8947116486c32` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF01KernelR26.java` | `cfd8eef79aff282f6d8e65b07ead7ce0501cb7acc5b229271fd1d37ba38a1cd9` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF02KernelR26.java` | `37ead506a23d34bfdcad01561de55d929e8c1fed0cdba692347c90d54e161598` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF08KernelR26.java` | `f252406df34e635a2d603fd7d11352e681e9812d8944c1c36d27a32ef720d11d` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF09KernelR26.java` | `c9543417437cb136183032dc8c68bc4076e5ae317bde4d7b639de1d2f63c2c9e` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF11KernelR26.java` | `4aa9c582122de14f3f518c8d5cdeb78dc6ec4ac44423a8b2b7ddfef46aec6eca` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF12KernelR26.java` | `6c5804014ac7d39bbc746db94ee66e3b27a53d1e905106a1407f4d804c89b180` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF18KernelR26.java` | `54e345020ae5e048c54794729050de33491189671cee0814657e84c7084b9043` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF19TensorR26.java` | `bf89f5f9233d8d02c35218d157fbf9206a2e03d5d3913020c2768e3cb7308a7e` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF20KernelR26.java` | `970cdb2ec1ca148f0bb4309a7a87484333ad8495b6afce965f5f2659c4dc6ab7` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF21KernelR26.java` | `66d0d24473a08b74d279c9bbcde8f2034db79771fe270f3a23dfea960b000b42` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java` | `cdeb4d130e40fb26d448ad5b7a980ed07215876d33e5c6d344cef4de0a371eeb` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java` | `c773d651a77661830d21c13a63764f12695d000f21d31b104bae9901d65c0cb9` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveStateR26.java` | `1084470c6f5293b80426e6cdb47a1f41d14ef3ddff699b2803948cef23c20546` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineBridgeSU2.java` | `915704b12522440b7696886affb69e3a2fefce5b7cd13608a5e94a00e2c76032` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelEvolutionSU2.java` | `acc11a888c0ed34fc3525095c41de115b6849a16f9dfd1ee340150313768c1f8` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelLiveSU2.java` | `dbcbe4690034872c9c951f2f18fd86c6c89c5ff4aba0cfd9c0a62f5bb4363575` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelReplaySU2.java` | `940f22cc0fb67095a0979f16c2b432a8584d6ad502dc554a0e952466543f2464` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/NeuralFamilyR26.java` | `f8b285f4a91d33a7747213247a76295b27cc8dec050727e1a4a385455ddb46b1` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/OnlineModelsSU2.java` | `1e9888bcf36b6e4e75a45bdca238fe4be0a22c78a7b773eea036d79df063eb8e` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/PermutationChainR26.java` | `d01b5ea316cc77528a932ea457fb362bdc0d049a9fef0e2175dbccffef4bf545` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/PermutationModelR26.java` | `deb7ba3f7f8091d9bf5006076dedb135d110607a1b7c87c7881dca7fb13892ee` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/PortfolioFamilyR26.java` | `606a967092666201ada000f19365f846a5f4229fc346b8be121c2013ae7a93c9` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResidualFamilyR26.java` | `56f5f120dca2c6b61feb5c626b1d5abb4c2dfc0ecbe7763014a1fc9a0604289e` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/SetFamilyR26.java` | `0929466e3c83920e93c6770801afd5cc20e0fafa661504c4db44ca5bb81f9710` |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/SymbolicFamilyR26.java` | `af5f71d6745ab85425484255b44d9d6262d728c37e168438075a6ff203786110` |

校验范围：39 个唯一家族 ID、所有引用文件与行范围存在、所有引用带整文件 SHA-256。该清单未运行 Android 应用，也不签署本次运行质量或真实出手验收。
