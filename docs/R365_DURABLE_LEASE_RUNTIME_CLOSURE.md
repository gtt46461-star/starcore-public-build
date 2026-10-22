# R3.6.5 持久化租约与 canonical 运行链闭环

## 现场证据到根因

| R3.6.4 现场证据 | 根因 | R3.6.5 关闭方式 | 验证 |
|---|---|---|---|
| `R36_DURABLE_CLAIM_FAILED_OR_EXPIRED` | RUNNING 行会被再次投递，执行 Owner 又与请求 attempt ID 相同 | 每次执行生成唯一 32 字符 Owner；活跃外部 Owner 只 DEFER | T43、T44 |
| claim 失败后下一次仍立即失败 | catch 路径无 Owner 围栏地 markRetry/清租约 | 所有状态转换核对 generation、requestHash、status、leaseOwner | Java 17 静态门 |
| `events=0`、紧密重复 attempt | 没有 next-attempt 时间 | 1–30 秒有界退避，`latestRunnable` 只返回到期任务 | T43 |
| 请求超过约 55 秒即失效 | deadline 被误用为预开奖业务有效期 | 只以 canonical 是否存在、目标是否已开奖和 durable 状态判断有效性 | T43 |
| 心跳 HEALTHY 但最近 attempt 失败 | heartbeat 新鲜度覆盖了失败事实 | `ANALYSIS_FAILED` 显式使 businessAlive=false；报告展示 durable 行 | 静态门 |
| 报告线程可能回填/建表 | `auditB12` 与统计读取调用 `ensure`/backfill | 诊断路径纯查询，缺表必须呈现 READ_ERROR | 静态门 |

## 唯一状态转换

`REQUESTED/RETRY -> RUNNING` 只能由成功 claim 的执行 Owner 完成。RUNNING 期间，checkpoint、历史哈希
绑定和 canonical 提交都要求同一 target、generation、requestHash 与 leaseOwner。Owner 丢失时立即
`R365_DURABLE_LEASE_FENCE_LOST`，旧执行不得继续写任何核心结果。

活跃外部 Owner 不属于失败；调度器只等待。lease 到期后，新 Owner 可以带完整审计原因接管。若 canonical
已经由 first-write-wins 提交，请求行只做一致性收敛，不重算模型。目标已开奖且没有 pre-draw canonical 时，
请求进入明确终态，禁止用开奖结果补算预测。

## DB_VERSION=36 的加法升级

R3.6.5 不提升数据库版本，也不删除用户数据。启动 Owner 使用幂等加列补充：

- `next_attempt_at`：重试到期时间；
- `repair_version`：防止覆盖安装 rearm 循环；
- `first_error_code`：保存第一个根因；
- `last_transition_at`：状态转换时间；
- runnable 索引：按状态、重试时间与请求时间选取任务。

覆盖安装桥只允许旧 repair_version、终态、未开奖、无 canonical 的 R3.6.4 行重置一次。任何 R3.6.5
终态、已开奖目标或已有 canonical 都不能被桥接器重置。

## 保持不变的红线

- 正式动作空间始终 `10*C(10,3)=1200`，固定 K3，阶段 9/8/7/6/5/4/3；
- `AnalysisSingleFlightR36` 是唯一分析并发 Owner；
- `DecisionGateEngine` 是唯一 REAL 授权 Owner；
- `SettlementEngine` 是唯一结算 Owner；
- canonical first-write-wins，不做开奖后重算；
- `MODEL_PROVEN=false`、`SHADOW_ONLY=true`、REAL 自动解锁为 false。

## 证明范围

T43 覆盖活跃 Owner 延后、陈旧接管、同 Owner 恢复、重试退避、开奖后阻断、旧终态一次 rearm 与新版本
防循环。T44 证明同一请求连续执行会得到不同 Owner，关闭 ABA 复用。全源码 Java 17 编译与 DEX 门确认
这些类和方法进入实际 APK。物理设备上的长期后台能力仍必须由 T29–T34 报告证明。
