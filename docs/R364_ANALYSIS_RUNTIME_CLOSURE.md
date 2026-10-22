# R3.6.4 分析运行链闭环

## 根因与关闭方式

| 现场证据 | 确定根因 | R3.6.4 施工 | 验证 |
|---|---|---|---|
| fetch/settlement 有进度，analysis 永不提交 | 冻结层与 B04 层历史哈希序列化不同 | `V51PermutationFeatureEngine.historyHash` 成为唯一历史哈希 Owner | T39 |
| 新安装只有少量历史 | latest 默认 5 期，R36 要求至少 12 期且绕过旧补齐 | `<12` 自动拉取/保存 100 期；240 仅为最大冻结窗口 | T40 |
| B12 与 1200 全为 0 | 上游在 B04 已抛错，原子提交从未进入 | T41 从 B04 连续执行到号码面、K3 漏斗与 1200 Rank | T41 |
| 报告只有 `NEVER_COMMITTED` | runtime 查询只筛成功事件 | 独立读取最新任意 attempt，输出 type/status/target/detail | Java 17 门 |
| K7 有 fetch、章节 6 却为 FETCH_STALE | R36 与 B13 写不同心跳字段，stage 只做精确字符串匹配 | stage 前缀识别并合并 canonical/B13 双源最新时间 | 静态门 |
| 请求刚投递即 ANALYSIS_STALE | 只有 RUNNING 被视为有进度 | WAITING/REQUESTED/RUNNING 在有限超时内均为 active work | 静态门 |
| 覆盖安装仍复用旧失败请求 | old hash/deadline/attempts 已冻结在 durable row | 仅对三种已知旧错误、未开奖目标、无 Owner 执行一次 R3.6.4 rearm | T42 |

## 不变量

- 只允许 `AnalysisSingleFlightR36` 一个计算 Owner。
- 历史补齐在同一个串行分析线程执行，不创建救援预测线程。
- PIT 仅包含 `issue <= baseIssue`；目标期补齐期间已开奖则禁止提交。
- 正式动作始终精确 1200，不剪枝、不用 240 作为预测门槛。
- canonical 原子写与 first-write-wins 不变。
- `DecisionGateEngine` 唯一授权，`SettlementEngine` 唯一结算。
- `MODEL_PROVEN=false`、`SHADOW_ONLY=true`、REAL 自动解锁为 false。

## 主机证明与设备边界

T39 直接构造 12 期有效排列，冻结后使用同一批 DrawFrame 复算 B04 哈希并执行完整 PIT feature
build。T40 固化 5→bootstrap、12→usable、100→bootstrap size、240→max-not-minimum。T41 使用 24 期
历史连续执行 B04 窗口/图、B05 五类证据、号码模型、1200 动作、固定 K3 漏斗和 Ranker。
T42 固化升级 rearm 白名单，并证明任意未知运行错误不会被自动重置。

这些测试关闭确定性的代码/ABI 问题，但不能替代 Android 物理设备运行。设备报告必须证明实际
canonical 提交、延后证据完成、下一期开奖精确结算，以及 T29–T34。
