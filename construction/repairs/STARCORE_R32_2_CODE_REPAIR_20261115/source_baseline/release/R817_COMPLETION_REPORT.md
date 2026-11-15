# STARCORE TIANSHU R817 完整修复与交付报告

## 1. 发布身份与不变红线

| 项目 | R817 最终值 |
| --- | --- |
| applicationId | `com.ethk3.novaonepro` |
| versionName / versionCode | `V57.15.3-R3.6.14-817` / `577190826` |
| 数据库 | `binance_k3_nova_one_pro_v57.db`，`DB_VERSION=36`，同版本增量 schema 修复 |
| 编译链 | Android API 36、Java 17、D8 release Dex |
| 游戏/玩法 | `Binance1-PK10`、单位置、固定 K3、每期精确 100 号码/1200 动作 |
| 安全边界 | `SHADOW_ONLY=true`、`MODEL_PROVEN=false`、`REAL_BET_HARD_DISABLED=true` |

R817 是针对 R816 物理机报告中仍存在的 OOM、卡顿、闪退、假 stale、不持续出号、结算 backlog、证据 backlog 和研究链“更新但未消费”所做的代码级升级。它没有删除动作、降低模型门槛、开奖后补预测、清空旧错误或解锁 REAL。

## 2. 本次确认的直接根因

### 2.1 OOM 与闪退：响应正文被按每一行重复保存和读取

- 多期开奖结果解析曾把完整 HTTP 响应赋给列表中的每一条 `Draw.rawJson`。若响应含 N 期，正文会在对象和 SQLite 中近似放大 N 倍。
- `DbStore` 的多条历史读取使用 `projection=null`，即使分析只需要 17 个结构化字段，也会把巨大的 `raw_json` 列重新装入每个历史对象。
- 这条放大链与设备记录 `OOM=3`、lifetime OOM=9、堆使用数百 MiB 和闪退吻合；它比单纯调整 Java heap 或缩减 1200 动作更上游。

### 2.2 业务循环假死：有效新响应没有刷新业务真值

- 同一期有效响应会被当成“没有新开奖”，旧路径未刷新 fetch receipt heartbeat。
- 已写入数据库的错误 open time 可能永久优先于新供应商时间，导致 provider 已显示 `FRESH`，B13/BusinessTruth 却仍显示 `FETCH_STALE/DRAW_DATA_STALE`。
- Service 存活和业务推进被不同线程、不同时间口径读取，产生 `truth=RUNNING` 与 `businessAlive=0` 的矛盾。

### 2.3 卡顿与 backlog：启动、结算、deferred、诊断同时争用

- 启动阶段曾同时播种 500 条结算任务、排大批 backlog、生成完整诊断、启动分析和 deferred，集中争用 SQLite、CPU 与堆。
- deferred 只有完全无实时需求时才运行，单轮工作仍偏大，长期产生 `pending=167`。
- 结算维护的大批扫描与实时分析并行，旧任务长期 `PENDING/RUNNING/WAITING_DRAW`，严格 OOS 和统计只能缓慢推进。

### 2.4 研究功能未形成真实闭环

- Research schema 和 tick 被 `deferred<=2`、settlement backlog=0 等无关条件长期阻塞，报告持续 `SCHEMA_NOT_READY`。
- 代码可生成公式/Java，但没有一个正式的开奖前候选执行、冻结、开奖后标签链。
- 原有轻量 FTRL 会在严格 OOS 后更新 Artifact，但预测端没有调用 `loadForTarget`，所以“学了却没用”。

## 3. R817 已实施修复

### 3.1 原始响应与历史内存闭环

- `ApiParser` 只为每条 draw 保存自身 JSON 对象；完整 HTTP 响应只保留一次运输审计副本。
- 新增 `DrawHistoryProjectionR817`，所有生产历史查询固定读取 17 个结构化字段，明确排除 `raw_json`。
- 新增 `DrawRawPayloadCompactorR817`，以小批次清理旧数据库中大于 8192 字节的重复 legacy payload；期号、十位号码、时间、来源、业务 hash、canonical、OOS 和 first-write 记录均不改变。
- deferred action chunk 从 300 降为 120，精确 1200 动作分成 10 个有界 slice；实时压力下每轮最多 1 个 job，空闲时最多 4 个 job。
- OOM 不会被伪装为 success；durable analysis lease 会释放为明确可重试状态，避免旧 owner 卡死。

### 3.2 Provider 时间、fetch heartbeat 与统一业务真值

- 新增 `ProviderTimeReconcilerR817`：当前已验证供应商时间可以替换明显陈旧的同一期时间；历史数据仍采用保守规则，不改变业务身份。
- 每一次已验证响应都会写 fetch receipt heartbeat，即使 issue 与上一期相同。
- 若 selected provider 是最近观测、同 issue 且 `FRESH`，B13 会用其 open time 对数据库遗留 stale 元数据做可审计 corroboration。
- Service liveness 使用独立 `HandlerThread`，不在 UI main looper 上执行数据库心跳；多项状态用一次事务批量写入。
- 新增 `RuntimeReleaseEpochR817`；current-release 指标和 lifetime 历史事实并列，旧错误不删除、R817 验收也不被旧版本永久污染。

### 3.3 启动、结算和 deferred 调度

- 启动维护移到低优先级串行线程并延迟执行，不再与首次 fetch/analysis 同时运行完整诊断。
- 启动结算播种由 500 限为 64；live current 和 backlog 每轮分别限为 4；审计上限由 500 限为 256。
- draw/watchdog 只调度有界 backlog 维护，不在高频调用点同步排大批任务。
- raw payload compaction 首轮 16 条、周期 8 条；deferred 以 48 MiB 可用 headroom 为最低门并执行公平调度。
- SettlementEngine、OOS writer、DecisionGate 和 single-flight owner 均未增加第二条生产写路径。

### 3.4 可执行研究、深度学习与动态迭代

- Research schema 每 30 秒独立确保，research tick 每 15 秒在生产空闲时运行，不再等待整个 deferred/settlement 历史 backlog 归零。
- 新增 `v57_research_predraw_r817` 和 `ResearchShadowChallengerRuntimeR817`：
  - 从已生成且可运行的公式候选中选一个；
  - 在生产 canonical 已提交、开奖前，对完整 1200 动作进行有界 DSL 评分；
  - 首写冻结候选、目标期、位置、K3、ordinal、公式概率、FTRL 概率、Artifact hash、trained-until、权重和输入 hash；
  - 只在唯一生产 Settlement owner 终态后读取实际开奖并写 future-live 标签；开奖后不调用模型。
- 当前严格 OOS 达到 12 期即可生成 `EXPLORATORY_COLLECT_ONLY` 的公式 incubator；120 期前不会生成需训练的 learned expert/model，也不会升级为模型证明。
- FTRL 更新严格遵循 `draw(t) -> settlement(t) -> strict OOS(t) -> update(t) -> earliest use(t+1)`；`trainedUntil >= target` 的 Artifact 永远拒绝。
- 下一期开奖前预测会实际读取 prior FTRL Artifact。动态混合公式为：
  - `w = 0.35 * min(120, updates) / 120`；
  - `p_shadow = (1-w) * p_formula + w * p_ftrl`；
  - `0 <= w <= 0.35`，早期样本不会覆盖稳定公式，成熟后仍保留 65% 公式基线。
- 深度网络 `96 -> 64 ReLU -> 32 ReLU -> 100 -> log-domain Sinkhorn 10x10`、Adam/梯度裁剪、时间顺序 train/validation/sealed-final 和 frozen Artifact 推断均保留。大网络反向传播明确为离线训练，Android 仅执行哈希验证后的冻结推断和轻量 FTRL，避免再次制造 OOM。
- 生成 Java 是完整可审计源码；当前 APK 运行受限 DSL/通用冻结模型 runtime，不做动态 class loading，不修改 Production owner。

## 4. 构建与自动验证事实

正式构建已通过：

- 466 个 Java 源文件、387450 行；Android API 36 / Java 17 全量编译生成 1985 个 class。
- R36 契约回归通过；R811 227 条、R815 131 条、R816 14 条、R817 77 条断言通过。
- R817 专项覆盖：raw payload 排除、provider 自愈、120×10 deferred、N12 incubator、完整 Java codegen、开奖前 FTRL 消费和 REAL 锁。
- 96/240 期完整 100-number/1200-action 主机性能分别为 777ms/996ms；192 MiB 低堆 96 期为 670ms。以上是主机证据，不冒充目标手机 P95。
- D8、AAPT2、APK ZIP、zipalign、v1/v2/v3 签名和原签名证书连续性全部通过。

| 构建项 | 最终值 |
| --- | --- |
| APK | `Starcore-Tianshu-V57.15.3-R3.6.14-817-release.apk` |
| APK bytes | `3261294` |
| APK SHA-256 | `af07de05526aa20dc0c62f8ea3468e6cd42081cbdf5e0b5adbdf9298a4b1a7bd` |
| Dex entries SHA-256 | `7e7d1203451421042c0dd52a6a6ff23d1ff1afb0ba73fe1639eeab49fce307de` |
| Source tree SHA-256 | `297a6088a0ba1b7e54e5b8c8a1c01aea1b4a6203808ddababf935b2db731c2dd` |
| Resource manifest SHA-256 | `5c894ce6cfb35d6c6e9630be10c842d79c474f1b0413d28ee83ad228d3ce4b63` |
| Toolchain SHA-256 | `48cb4f1da92d7112c54d349209a89db0635be60d3c573dc5947f0dc9d063320f` |
| Signing certificate SHA-256 | `ae7e1b4a5a71f1f299d4a0b2472c6a36513873514e78190d42d6cfba3340116f` |

## 5. 升级安装后的确定行为

1. 用同签名 R817 APK 覆盖安装；不要卸载或清空数据库。
2. 首次启动会建立 R817 健康 epoch，并以小批次修复 legacy raw payload 和 stale metadata。
3. 有效响应即刷新 fetch truth；精确下一期只由 TargetIssueResolver 创建，分析只由一个 durable single-flight owner 执行。
4. 实时核心先首写 SHADOW canonical，完整 100/1200 证据随后从 durable snapshot 分 10 块补齐，不重跑开奖后模型。
5. 当前和历史结算任务以有界批次继续推进；只有合法开奖前 canonical 才能产生严格 OOS。
6. 研究公式在未来期开奖前冻结，开奖后唯一标注；已有 FTRL 只影响更晚的目标期，所有动态结果仍为 SHADOW。

## 6. 真实性边界

- 本次已经完成源码、主机、Android 36、D8、APK、签名和低内存验证；尚未在用户目标手机执行 T29–T34，因此 `PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED`。
- 当前报告只有 25 个 strict OOS，模型 Artifact、校准、Wilson/ECE/Brier 和独立 future-live 证书未达到证明规则，因此 `MODEL_PROVEN=false`。
- 不能把“功能已实现”描述成“预测有效性已证明”，也不能承诺彩票/随机结果命中。
- REAL、K4、dynamic K、多位置 REAL、追单、倍投、martingale 和 frequency debt 均保持硬禁止。

