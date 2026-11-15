# R817 物理设备复测规程

## 1. 安装

1. 先导出 R816 完整诊断、DB、`-wal/-shm` 和故障 logcat。
2. 校验 APK SHA-256：`af07de05526aa20dc0c62f8ea3468e6cd42081cbdf5e0b5adbdf9298a4b1a7bd`。
3. 对 `com.ethk3.novaonepro` 执行同签名覆盖安装；不要卸载、不要清数据。
4. 核对 `versionName=V57.15.3-R3.6.14-817`、`versionCode=577190826`。

## 2. 首次启动 30 分钟

每分钟保存一次诊断摘要，每 5 分钟保存一次完整报告，检查：

- `r817_runtime_epoch_at>0`；旧 lifetime OOM 仍可见，R817 current OOM 从真实零开始。
- provider selected FRESH 时，B13 不再因同一期数据库遗留时间持续显示 stale。
- fetch receipt 即使未换期也持续刷新；Service/Fetch/Analysis/Settlement 分层心跳一致。
- raw payload compactor pending 只下降，不阻塞实时分析，不改变 draw 业务 hash/号码。
- 启动期间无 ANR、闪退、黑屏和持续 GC；analysis maxConcurrency<=1。
- 当前期 canonical 先提交；deferred 按 120 行 slice 补齐，不缩减 1200 动作。

## 3. 至少两个未来期闭环

对安装后尚未开奖的目标期保存：

1. 开奖前 production canonical：issue=1、champion=1、selected<=1、auth=1、BET=0。
2. deferred 最终：number=100、formal action=1200、ordinal 0..1199 唯一。
3. research predraw：candidate/target/position/K3/ordinal、formulaP、adaptiveP、artifact hash、trainedUntil、weight、frozenAt。
4. 若 adaptive Artifact 存在，必须 `trainedUntil < targetIssue`；否则状态应明确为 formula-only。
5. 开奖后：唯一 Settlement terminal；strict OOS modelInvocation=0；research 只写 outcome，不重跑预测。
6. SHADOW predicted = settled + pending，hit + miss = settled；REAL 指标保持 0。

没有合法开奖前 canonical 的旧期只能得到明确 terminal 原因，不能补造预测或命中。

## 4. T29–T34

| 测试 | 操作 | 通过条件 |
| --- | --- | --- |
| T29 Screen-on 120 分钟 | 前台/后台切换、浏览报告 | 无 ANR/闪退；current OOM=0；P95<=5000ms；业务心跳连续 |
| T30 Screen-off 120 分钟 | 锁屏并保持网络 | 解锁后 fetch/analysis/settlement 有真实推进，不是只有 Service alive |
| T31 Task-removed 30 分钟 | HOME 后划掉任务 | 单 recovery lease、单 runtime generation；durable request/task 恢复 |
| T32 Process-kill matrix | 系统回收、强停后手动启动、重启设备 | DB quick_check 正常；不重复 canonical/settlement；owner<=1 |
| T33 Network primary/backup | Wi-Fi、蜂窝、断网、恢复、主源失败 | HTTP 成功不冒充新期；主备切换后 issue/time/heartbeat 一致 |
| T34 24h soak | 连续跨多个开奖周期 | current OOM=0；无 crash/ANR；P95<=5000；结算和 deferred backlog 可收敛 |

## 5. 失败时必须采集

- 未截断完整诊断和 JSON evidence；
- crash/ANR 前后至少 10 分钟 logcat；
- DB、`-wal/-shm`、`PRAGMA quick_check`；
- `dumpsys activity services`、jobscheduler、alarm、proc/meminfo；
- 失败目标期的 draw、analysis request、canonical、deferred、settlement、OOS、research predraw/artifact 原始行；
- 设备型号、Android、时区、电池优化、网络和操作时间线。

T29–T34 实际完成前，`PHYSICAL_DEVICE_RUNTIME` 必须保持 `RETEST_REQUIRED`。模型证明与设备运行证明相互独立；任何一项失败都保持 SHADOW_ONLY。

