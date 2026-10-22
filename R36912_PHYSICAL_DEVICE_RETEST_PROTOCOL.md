# R3.6.9-812 物理设备复测协议（T29–T34）

## 1. 测试包与前置条件

- APK：`Starcore-Tianshu-V57.15.3-R3.6.9-812-release.apk`
- 期望包名：`com.ethk3.novaonepro`
- VersionCode：`577190821`
- SHA-256：`1312d9f1c28848acfa45c546afd36b27d2228934ad8594c94eec03166616a253`
- 模式必须为：`MODEL_PROVEN=false / SHADOW_ONLY=true / REAL_BET_HARD_DISABLED=true`
- 禁止用旧 APK 的报告判定新 APK；升级安装后必须确认版本号并重新产生完整报告。

建议先保留旧数据库备份，再使用 `adb install -r` 升级安装。不要清除数据，否则无法验证 DB36 同版本修复、旧失败任务 rearm 和历史 deferred 恢复。

## 2. 每个矩阵共同采集项

每次开始、每 10 期、结束时复制应用完整诊断报告，并记录：

- 最新 draw / canonical / settled / strict OOS 期号及 lag；
- Service、fetch、analysis、settlement 四类 heartbeat age；
- durable analysis request 的 status/generation/attempt/owner/lease；
- `requestToCommit` P50/P95/P99 与每阶段 span；
- `maxConcurrency`、OOM 次数和 OOM 文本回溯数；
- canonical first-write-wins、1200 action、100 number；
- deferred pending/cursor/modelInvocation；
- exact settlement/OOS model call；
- Research sidecar、FTRL trainedUntil、Artifact Hash；
- APK 版本、报告版本、完整正文 Hash。

建议同时采集 `adb logcat`。任何闪退都必须保留首次异常栈，不能只用重启后的空日志。

## 3. T29：亮屏 120 期

1. 前台保持应用可见并运行 120 个连续目标期。
2. 每期必须恰好一个 canonical；同一期不得出现第二分析 Owner。
3. 开奖后 exact settlement 和 strict OOS 必须按冻结 pre-draw 记录闭合。
4. 延后证据允许稍后完成，但最终每期必须达到 100/1200，且 model invocation=0。

通过条件：

- eligible coverage 为 120/120；
- `analysisMaxConcurrency≤1`；
- `OOM=0`；
- `requestToCommitP95≤5000 ms`；
- canonical/settlement/OOS 无重复、无事后重算；
- 无 Activity/Service 闪退。

## 4. T30：熄屏 120 期

1. 启动服务后锁屏，保持设备非充电与充电两种子场景。
2. 运行 120 期；中途只通过 adb/通知观察，不唤醒应用 UI。
3. 结束后解锁，复制完整报告。

通过条件除 T29 外，还要求：Service 存活不能被当作业务通过；fetch/analysis/settlement heartbeat 均持续推进，Alarm/Job 不能产生第二 Owner。

## 5. T31：划掉任务 30 期

1. 服务运行中从最近任务划掉 Activity。
2. 不手动重新打开应用，连续观察 30 期。
3. 重新进入后读取完整报告。

通过条件：`onTaskRemoved` 只执行 fetch+reconcile；业务 heartbeat 推进；无重复 canonical、BET 或 settlement；分析并发仍为 1。

## 6. T32：进程恢复矩阵

分别执行并保留独立报告：

- 系统回收进程后 Alarm 恢复；
- 系统回收进程后 JobScheduler 恢复；
- 设备重启后的 BootReceiver 恢复；
- 分析 Worker 在 canonical 前异常退出；
- 分析 Worker 在 canonical 后退出。

关键判定：

- 只有 Worker 物理死亡时允许 `DEAD_WORKER_REAPED`；
- 活 Worker 必须显示 `WORKER_ALIVE` 并禁止 HandlerThread 替换；
- 死亡前未提交的 durable request 被真实标为 retry/error，并由唯一主链重排；
- canonical 已提交后不得重新跑模型；
- `restartRequest`、`actualStart`、generation 分账，不以请求次数冒充进程启动次数。

## 7. T33：网络主备与断网恢复

覆盖：主线路成功、主线路失败转备用、完全断网、本地历史分析、网络恢复。

通过条件：

- 错误和 URL 均脱敏；
- 完全断网时不产生伪造 draw；
- 网络恢复后精确补齐，不跳过或重复 target；
- fetch stale 会触发唯一 fetch 主链恢复；
- OOS 始终只使用冻结 canonical 与 exact draw。

## 8. T34：24 小时 soak

保持正常真实环境运行 24 小时，包含至少一次亮屏、熄屏、HOME、划掉任务、断网恢复和充电状态变化。

最终通过条件：

- `OOM=0`，没有被旧统计漏掉的 OOM 文本；
- `requestToCommitP95≤5000 ms`，P99 无持续恶化；
- service/fetch/analysis/settlement heartbeat 无超过阈值且未恢复的 stale；
- deferred backlog 最终回落，不单调增长；
- 数据库无重复 Owner、重复 canonical、重复 settlement；
- strict OOS 覆盖分母来自可枚举 eligible target；
- Research Artifact 的 `trainedUntilIssue` 严格单调，晚到历史显示 `BACKFILL_LEARNING_FORBIDDEN`；
- 生成的公式和模型仍为 Research/SHADOW，无 Production mutation、无 REAL 解锁。

## 9. 模型证明的独立门

T29–T34 通过只证明运行稳定，不自动证明模型有效。模型晋级至少还需：

- 未来冻结后 live OOS；
- `SelectedN≥480`；
- HitRate≥0.45；
- Wilson lower 95%≥0.40；
- Jeffreys Beta lower 95%≥0.40；
- Coverage≥0.15；
- ECE≤0.04；
- 至少 3/4 块稳定；
- BH-FDR 通过；
- Artifact/Data/Code/Feature Schema Hash 与推理 parity 完整。

任一项未通过时继续 `SHADOW_ONLY`，不得降低门槛、用频率欠债补覆盖或自动解锁 REAL。
