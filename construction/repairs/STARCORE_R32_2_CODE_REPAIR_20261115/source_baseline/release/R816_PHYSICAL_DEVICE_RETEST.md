# R816 物理设备复测规程

## 1. 安装前

1. 保存当前 APK 版本、完整诊断报告、数据库导出和故障时段 logcat。
2. 核对安装包 SHA-256：`602c937773078cb11040d123ca290468c5c71d7e98712832240fca64d751650b`。
3. 采用覆盖升级安装，必须保留 `com.ethk3.novaonepro` 的现有数据；不要卸载、不要清除应用数据。
4. 若 Android 拒绝覆盖，先保存“签名不匹配/版本降级/空间不足”的原始错误，不能以清数据绕过。

## 2. 首次启动 15 分钟

每 1 分钟记录一次完整诊断和界面统计，至少验证：

- `Version=V57.15.3-R3.6.13-816`，`VersionCode=577190825`。
- `r816_runtime_epoch_at > 0`。
- primary/backup provider issue 合法；最新 draw age 不再固定多出约八小时。
- service、fetch、analysis、settlement heartbeat 分层更新。
- durable analysis 不反复停在 `HISTORY_GAPPED`；若连续历史确实少于 12 期，错误必须是明确的 `R816_HISTORY_*_LT_12`，不能分配完 1200 后才失败。
- 同一 target 的 max concurrency 始终为 1。
- 界面可以操作、滚动和切换，无 ANR、闪退、黑屏或持续 GC 抖动。

## 3. 新一期完整闭环

必须等待“安装后尚未开奖”的未来目标期，按时间顺序保存以下证据：

1. 开奖前：base issue、target issue、history hash、canonical commit timestamp。
2. canonical：issue=1、number=100、action=1200、rank1=1、authorization 为 SHADOW 且 BET=0。
3. 开奖后：draw 业务身份和 first_seen_at；settlement task 进入唯一终态。
4. OOS：只读取冻结 canonical + actual draw，model invocation=0。
5. SHADOW：predicted 增 1；结算后 settled 增 1 且 pending 减 1；hit/miss 合计等于 settled。
6. deferred：从 durable snapshot 产生完整 100/1200；重复 drain 不产生重复行。

旧期若没有合法开奖前 canonical，只能记录 `FROZEN_OBSERVATION_MISSING` 等明确终态，不能用它验证“出号”或命中率。

## 4. T29–T34 运行矩阵

| 测试 | 步骤 | 必须记录 | 通过条件 |
| --- | --- | --- | --- |
| T29 Screen-on 120 分钟 | 前台/后台正常使用，保持亮屏 | 每 5 分钟 heartbeat、P95、heap、OOM、pending、最新 issue | 无闪退/ANR；业务 heartbeat 连续；maxConcurrency<=1；current OOM=0 |
| T30 Screen-off 120 分钟 | 锁屏并保持网络 | 锁屏前后 fetch/analysis/settlement 时间戳 | 解锁后不是仅 service 活着；业务阶段有真实推进，无重复 target |
| T31 Task-removed 30 分钟 | HOME 后从最近任务划掉 | recovery generation、trigger/start/process counters | 单 active lease；服务恢复并继续最后阶段，不生成并行 owner |
| T32 Process-kill matrix | 正常杀进程、系统回收、重启设备各一次 | boot/job/alarm/watchdog 来源与 generation | 每次最多一个 active runtime；耐久 request/task 恢复，DB quick_check 正常 |
| T33 主备网络 | Wi-Fi→蜂窝→断网→恢复，分别模拟主源失败 | primary/backup issue、age、错误码、selected line | 不能把 HTTP 成功当新开奖；恢复后精确下一期且不重复 canonical |
| T34 24h soak | 连续运行 24 小时，跨多个期开奖 | 全量诊断、DB、logcat、每期闭环表 | current OOM=0；P95<=5000ms；并发<=1；无 ANR/崩溃；合法未来期闭环推进 |

## 5. 失败采集

任何一项失败时，不要重复点击或清空数据。立即保存：

- 完整未截断诊断报告和 JSON evidence；
- `adb logcat -d`（包含崩溃前后至少 10 分钟）；
- 应用数据库及 `-wal`/`-shm`（先正常停止应用或 checkpoint）；
- `dumpsys activity services com.ethk3.novaonepro`；
- `dumpsys jobscheduler`、`dumpsys alarm` 中对应 package 片段；
- 设备型号、Android 版本、时区、电池优化、网络类型、安装/启动时间；
- 失败目标期的 analysis request、canonical、settlement task、OOS、deferred snapshot 原始行。

## 6. 判定边界

- T29–T34 全部实际通过前，`PHYSICAL_DEVICE_RUNTIME` 必须保持 `RETEST_REQUIRED`。
- 未来 live strict OOS 少于 120 时，不显示为模型效果证明。
- 即使运行矩阵通过，只要冻结模型 artifact、校准和严格 OOS 证书未通过，`MODEL_PROVEN` 仍为 false、REAL 仍为锁定。
