# STARCORE 天枢 V57.15.3-R3.6.9-812 检测报告后修复、深度学习与动态迭代检测报告

生成基线：用户补发的 `V57.15.3-R3.6.7 / R3.6.8-811` 物理机完整诊断报告

目标包：`com.ethk3.novaonepro`

版本：`V57.15.3-R3.6.9-812`

VersionCode：`577190821`
数据库：`binance_k3_nova_one_pro_v57.db / DB_VERSION=36 / R811_SCHEMA_REVISION=1`

## 1. 最终判定

| 判定项 | 结果 | 真实性边界 |
|---|---|---|
| 工程修复与集成 | `PASS` | 425 个 Java 源文件使用官方 Android 36 API、Java 17 完整编译 |
| APK 构建、签名、ABI | `PASS` | D8 9.2.23、zipalign、原签名证书连续性、239 个 Android 方法描述符检查均通过 |
| R3.6/R811 行为回归 | `PASS` | R3.6 宿主回归通过；R811 208 项断言通过 |
| 冻结生产策略零漂移 | `PASS` | 100 组输入 × 11 个字段逐字节一致，Golden SHA-256=`fd2660829c3d05df7d655e2996afb597145b16ad3b9775e7774c41bf7362fd15` |
| 深度学习工程能力 | `IMPLEMENTED_AND_BUILD_VERIFIED` | 训练器、Artifact、Android 推理、梯度/Sinkhorn 数值自检均存在且通过 |
| 动态迭代工程能力 | `IMPLEMENTED_AND_BUILD_VERIFIED` | 严格 OOS 后 FTRL、Artifact 恢复、延迟 OOS 补学、公式进化、分组实验、FDR 均已落地 |
| 模型效果证明 | `NOT_PROVEN_UNTRAINED` | 当前合法未来严格 OOS 样本仍不足，不能把工程能力写成命中率证明 |
| REAL 解锁 | `NO` | `MODEL_PROVEN=false`、`SHADOW_ONLY=true`、`REAL_BET_HARD_DISABLED=true` |
| 物理机运行证明 | `RETEST_REQUIRED` | 静态构建不能代替 T29–T34 真机复测；具体协议见 `R36912_PHYSICAL_DEVICE_RETEST_PROTOCOL.md` |

本次交付完成的是可编译、可执行、可持久化、可审计、可回滚的完整工程修复。任何“必定准确”“必定达到 45%”或“已经通过 24 小时真机测试”的表述都没有被伪造。

## 2. 补发诊断报告的根因闭环

| 原始证据 | 根因 | 已实施修复 | 当前工程验证 |
|---|---|---|---|
| `OutOfMemoryError`，648208 字节分配失败，256 MiB 上限 | 历史帧重复转换、1200 ActionRow 活对象、快照二次对象化、巨大报告字符串同时占用堆 | 冻结帧缓存；1200 动作原地校验；流式 GZIP 快照；Repository 直接接收编码 Header；低优先级 Research 按游标流式处理；16 MiB 分析启动余量；`largeHeap=true`；OOM 后只清理可重建缓存 | 官方 API 编译、宿主回归、D8、APK ABI 全通过；仍需真机证明 OOM=0 |
| `requestToCommitP95Ms=32040` | 热路径同时承担正式核心、1200 证据对象化及大块序列化 | 同步核心与延后证据继续分离；流式编码不再建立第二份 1200 对象图；O(n log n) 稳定原始数组排序；阶段内存/时延打点 | 生产字段零漂移；真机目标仍为 P95≤5000 ms |
| Service 存活但业务 `SERVICE_STALE/FETCH_STALE` | Service 心跳被误当业务心跳；灾难性线程退出可能留下内存 Owner；看门狗只有合作取消，无法释放已经死亡的执行线程 | 业务真值改读最新 fetch/analysis/settlement beat；每次分析绑定真实 Worker；仅在 Worker 物理死亡后原子回收；活 Worker 明确禁止替换；保留最新 pending；持久化 `WORKER_THREAD_DIED_BEFORE_COMMIT` 并重排唯一主链 | 新增死亡 Worker 宿主契约；并发最大值仍为 1；生产 Owner 未增加 |
| `oom=0` 与错误文本中的 OOM 矛盾 | 旧统计只读 `oom_count`，遗漏历史 detail/error 中的 OOM | 统计同时识别 `OutOfMemoryError` 历史文本；新失败写入明确 OOM 事件 | 报告不再把历史 OOM 错报为 0 |
| 诊断页闪退/卡顿风险 | 85k+ 完整报告在 UI 线程读库并构造，且预分配 128/256 KiB 连续字符数组 | 完整报告改为专用后台线程和独立 DbStore；UI 只接收最终不可变文本；页面关闭后不回写；报告 Builder 初始容量降为 32 KiB、按需增长 | Android 36 编译和 UI 宿主桩通过；真机旋转/返回键仍列入复测 |
| 55 个 canonical、0 settled、40 deferred | 生产者 OOM 阻断后续 canonical/settlement/OOS；Research 标签也无法闭环 | 结算 OOM 真实持久化；延后证据从 durable snapshot/cursor 恢复；Research 标签只从已认证 `COMPLETED + exact1200` 证据流式生成；每 tick 最多一组 | 不进行事后模型重算，`ModelInvocationDuringDeferred=0` 不变 |
| 深度学习与动态进化只有设计文本、`generated=0` | 缺少完整训练器、真实实验执行、持久 Artifact 与延迟 OOS 补学 | 增加 TinyMLP 训练器、Android Artifact 推理、公式 AST 进化、严格按期号的 1200 分组实验、Platt/Beta/Wilson/ECE/Brier、BH-FDR、持久 FTRL 和完整代码生成报告 | 数值自检与 120 期训练烟测通过；效果仍等待未来严格 OOS |

## 3. 内存与运行时修复的具体代码

### 3.1 分析热路径

- `V51RuntimeInputR36`：冻结后缓存帧，不再为同一期历史重复转换。
- `V51ActionRankerEngine`：改为原始索引稳定归并排序，保持完全相同的比较器与 tie-break，避免装箱和临时对象洪峰。
- `K3ActionSpaceEngine`：单次遍历同时验证 1200 基数、ordinal、身份唯一性与 Hash。
- `DeferredEvidenceSnapshotR36`：新增 `Header`、`EncodedSnapshot` 与 `encodeRuntimeResult`，把 100 号码、1200 动作、排列和证明直接写入版本化 GZIP DataStream。
- `CanonicalObservationWriterR36`、`V51Repository`：原子 canonical 提交只携带轻量 Header 和已编码快照，不再解码回 1200 个 ActionRow。
- `V51RuntimeOrchestrator`：阶段化释放临时引用，记录每阶段堆余量；OOM 时保留真实失败并清理可重建缓存。
- `RuntimeMemoryGuardR811`：低于 24 MiB 开始回收，回收后不足 16 MiB 则用可持久化 `MemoryPressureException` 重试，不再冒险运行至 `<1% heap free`。

### 3.2 唯一 Owner 与死亡线程恢复

`AnalysisSingleFlightR36` 新增真实 Worker 绑定和死亡回收：

1. `Lease.bindCurrentThread()` 把执行租约绑定到实际分析线程。
2. `reapDeadWorker()` 只接受 `worker != null && !worker.isAlive()`，或 HandlerThread 已物理死亡且任务从未开始绑定。
3. 活线程返回 `WORKER_ALIVE`，禁止启动第二执行 Owner。
4. 回收时先把 active 从 1 原子降为 0，再取出唯一 latest pending。
5. `AutoFetchService` 重建唯一分析 HandlerThread，持久化失败、重新声明 durable lease，再提交 pending/latest。

这不是并发“救援链”，而是对已经不可能继续执行的死亡线程进行安全回收；`AnalysisMaxConcurrency=1` 红线保持不变。

## 4. 深度学习：真实模块、算法与公式

### 4.1 文件与执行边界

- 离线训练：`training/r811_autonomous_research.py`
- Android 推理与数值参考：`ResearchModelCandidateRuntimeR811.java`
- Artifact 编解码：`ResearchArtifactCodecR811.java`
- 候选生成：`StrategyChallengerFactoryEngine.java`
- 完整候选代码生成：`CandidateCodeSynthesisEngine.java`
- 完整检测报告：`LiveDiagnosticReportEngine.java`

大网络反向传播只允许离线执行；Android 只加载冻结 Artifact 推理，并运行轻量 FTRL。Research 无 Production Owner、无动态 Dex 加载、无 REAL 权限。

### 4.2 TinyMLP 结构

输入为 96 维开奖前特征，输出为 10×10 位置—号码概率面：

\[
h_1=\operatorname{ReLU}(W_1x+b_1),\quad W_1\in\mathbb{R}^{64\times96}
\]

\[
h_2=\operatorname{ReLU}(W_2h_1+b_2),\quad W_2\in\mathbb{R}^{32\times64}
\]

\[
z=W_3h_2+b_3,\quad W_3\in\mathbb{R}^{100\times32}
\]

将 `z` 重排为 10×10，使用 log-domain Sinkhorn 交替行/列归一化得到双随机矩阵 `Q`。损失为排列交叉熵加 L2：

\[
L=-\sum_{p=1}^{10}\log\max(\epsilon,Q_{p,y_p})+\lambda_2\sum_k\lVert W_k\rVert_2^2
\]

优化器使用 Adam：

\[
m_t=0.9m_{t-1}+0.1g_t,\quad v_t=0.999v_{t-1}+0.001g_t^2
\]

\[
\theta_t=\theta_{t-1}-\eta\frac{\hat m_t}{\sqrt{\hat v_t}+10^{-8}}
\]

全局梯度范数裁剪为 5.0；Xavier Uniform 初始化；固定 seed；不 shuffle。超参候选为 `lr∈{1e-4,3e-4,1e-3}`、`λ2∈{0,1e-6,1e-5,1e-4}`。

### 4.3 防泄漏训练协议

- 全部 issue 严格按时间升序。
- 最后 20% 为 sealed final，训练、超参选择和 early stopping 均不读取。
- 前 80% 再按 80/20 划分 train/validation。
- 均值与标准差只由 train 段估计；sealed final 不参与归一化拟合。
- early stopping 只看 validation。
- 训练输出含 Dataset SHA-256、Artifact SHA-256、训练截止期号、特征 Schema Hash 与 `sealed_final_consumed=false`。

数值自检结果：

- 确定性 Artifact SHA-256：`642327b904a2a29e6f603ac46cd11dc353d6d8b27b1026e1ce203b0b5a34df11`
- 有限差分梯度相对误差：`1.1039630321008076e-9`
- Sinkhorn 最大行误差：`4.440892098500626e-16`
- Sinkhorn 最大列误差：`2.220446049250313e-16`
- 120 期合成数据完整训练烟测：`PASS`，且 `sealed_final_consumed=false`

这些结果证明算法实现和数值链正确，不证明真实预测命中率。

## 5. 动态迭代、策略创造与严格实验

### 5.1 策略公式创造

`ResearchStrategyDslEngine` 提供白名单 AST：`FEATURE/CONST/ADD/SUB/MUL/DIV/ABS/MIN/MAX/CLIP/SIGMOID`。`StrategyChallengerFactoryEngine` 从六个种子公式开始，执行确定性 mutation/crossover，保存：

- `FormulaId`
- `CanonicalAST` 与人类可读公式
- 父公式 ID、generation、遗传算子
- complexity、feature dependencies、seed
- PIT 审计、Dataset Hash、Candidate Hash

系统还生成 G0–G5 Expert Gate、Platt/Temperature 校准、TinyMLP、Attention 与 FTRL 候选。任一候选引用 `DecisionGateEngine`、`SettlementEngine` 或 REAL 解锁符号时静态拒绝。

### 5.2 1200 动作严格分组实验

`ResearchActionOutcomeMaterializerR811` 仅从冻结且完整的 `1200` 动作快照逐行产生 label；`ResearchExperimentRunnerR811` 每期只保留两个标量 winner，不把 1200 行装进内存。实验包含：

- 按 issue 分组的四段 walk-forward；
- 独立 frozen test；
- leave-one-feature-out ablation；
- Platt 选择后校准；
- 四块稳定性、regime stress；
- `EligibleN / SelectedN / HitN` 明确分母与分子；
- Wilson 下界、Jeffreys Beta 下界、ECE、Brier、Coverage CV、最长连错、P95 延迟、RAM；
- 候选间 Benjamini–Hochberg FDR，`q=0.05`。

报告不再把 `N=0`、读取错误或工程存在写成效果证明。

### 5.3 严格 OOS 后动态 FTRL

每个目标期的顺序固定为：

`draw(t) → SettlementEngine(t) → strictOos(t) → exact1200Labels(t) → FTRL.update(t) → Artifact(trainedUntil=t) → 最早 t+1 使用`

FTRL-Proximal 公式：

\[
p=\sigma(w^Tx),\quad g_i=(p-y)x_i
\]

\[
\sigma_i=\frac{\sqrt{n_i+g_i^2}-\sqrt{n_i}}{\alpha},\quad
z_i\leftarrow z_i+g_i-\sigma_iw_i,\quad n_i\leftarrow n_i+g_i^2
\]

\[
w_i=
\begin{cases}
0,&|z_i|\le\lambda_1\\
-\dfrac{z_i-\operatorname{sign}(z_i)\lambda_1}
{(\beta+\sqrt{n_i})/\alpha+\lambda_2},&\text{otherwise}
\end{cases}
\]

参数：`α=0.05, β=1, λ1=0.001, λ2=0.01`。每次更新把 `z/n/meta/params` 完整编码、Hash 校验并写入 `v57_research_artifact_r811`。Artifact 可恢复；同一期幂等；已经存在更新期号更大的 Artifact 时，晚到历史严禁回填学习。

本次额外补齐了标签/OOS 到达顺序竞争：即使 1200 标签先完成、严格 OOS 稍后才写入，`ResearchSettlementSidecarR811.pendingLearningTarget()` 仍会找到最早缺失 Artifact 的合法期号并闭合学习，不再漏掉动态更新。

## 6. 完整检测报告写入内容

应用内 B14–B15 V10 完整报告新增并完整输出：

- Production Champion 真实状态；
- 失败分布与正/反证据；
- 持久化研究假设；
- 每个新公式的 AST、父代、代数、mutation/crossover、复杂度、依赖、PIT 与 Dataset Hash；
- 候选链和搜索淘汰统计；
- Top Challenger 的 Eligible/Selected/Hit、Wilson/Beta/ECE/Brier/FDR/块/Regime/延迟/RAM；
- 每个候选的完整 Generated Java、PATCH、manifest、fixtures 和 Bundle Hash，不截断；
- TinyMLP、Sinkhorn、Adam、FTRL 的精确公式、文件名和训练边界；
- 最新 Artifact Hash、训练截止期号与状态；
- `ResearchObserved`、`ResearchTarget45`、`ModelProof45` 三者分开，禁止混写。

若当前尚无合法候选，报告会输出完整的“未训练设计参考代码”，并明确标记 `DESIGN_ONLY_BUILD_REQUIRED_UNTRAINED`，不会伪造已训练模型。

## 7. 发布验证证据

| 验证 | 结果 |
|---|---|
| 官方 Android 36 / Java 17 完整编译 | `PASS`，425 Java，1851 class |
| R3.6 回归 | `PASS` |
| R811 Host Contracts | `PASS`，208 assertions |
| B01–B15 源码完整性 | `PASS`，425 文件、114/114 合同、0 hard failure、0 review |
| R811 文件处置清单 | `PASS`，165/165 行 |
| 生产策略 parity | `PASS`，100×11 字段逐字节相同 |
| 神经网络梯度/Sinkhorn 自检 | `PASS` |
| 120 期训练烟测 | `PASS` |
| 源码密钥扫描 | `PASS`，明文 API key=0、签名口令=0、私钥文件=0 |
| APK zipalign/签名/证书连续性 | `PASS` |
| APK ABI | `PASS`，239 descriptors，forbidden=0 |
| APK 标识 | `com.ethk3.novaonepro / 577190821 / V57.15.3-R3.6.9-812` |
| APK SHA-256 | `1312d9f1c28848acfa45c546afd36b27d2228934ad8594c94eec03166616a253` |

## 8. 不能静态宣称完成的项目

以下项目必须安装本次 APK 后由物理设备重新产生证据：

- T29 屏幕点亮 120 期；
- T30 熄屏 120 期；
- T31 划掉任务 30 期；
- T32 进程杀死/重启矩阵；
- T33 主/备网络和断网恢复；
- T34 24 小时 soak；
- 真机 `requestToCommitP95≤5000 ms`、`OOM=0`、`DeferredPending→0`；
- 未来严格 OOS 的命中、Wilson、Beta、ECE、Brier、Coverage 与 FDR。

在这些证据完成前，正确发布姿态仍是 `SHADOW_ONLY`。这不是缺少代码，而是防止拿静态构建冒充物理运行和模型有效性证明。
