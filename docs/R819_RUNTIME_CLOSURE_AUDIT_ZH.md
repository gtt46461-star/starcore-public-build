# STARCORE 天枢 V57.15.3-R3.6.16-819 运行闭环修复审计

## 交付判定

- Package：`com.ethk3.novaonepro`
- Version：`V57.15.3-R3.6.16-819`
- VersionCode：`577190828`
- Database：`binance_k3_nova_one_pro_v57.db`，DB_VERSION=36（不破坏升级）
- 发布姿态：`SHADOW_ONLY`
- MODEL_PROVEN：`false`
- REAL 自动解锁：`false`
- APK：原签名证书连续、ZipAlign、APK ZIP 完整性、Android 36/Java 17/D8 均通过

本次修复的是运行闭环，不伪造模型证明。REAL=0 在模型、Ranker、选择后校准和物理设备实时证书未通过时是正确的硬门行为，不允许通过降低门槛、追单、倍投、频率欠债或把 SHADOW 冒充 REAL 来“修复”。

## 从 R818 报告确认的根因

### P0：结算 Owner 只有线程内证明，没有进程级单飞

`SettlementCoordinatorR815` 原来使用 `ThreadLocal` 证明调用路径来自协调器，但 Fetch、Watchdog、Reconciler、Maintenance 的不同 HandlerThread 仍可同时进入结算。报告中的 12 个 `RUNNING`、结算长期不终态、SQLite 写竞争和 `SETTLEMENT_STALE` 与该缺陷一致。

修复：新增 `SettlementExecutionGateR819`，使用非阻塞进程级原子单飞门。任一时刻只允许一个线程进入 `SettlementEngine`，其余调用保留耐久任务并让出，不持锁等待、不抢 SQLite。Drain 遇到 BUSY 立即停止，避免同一期重复查询风暴。

### P0：旧版本历史任务污染当前“待结算”

报告显示任务共 1470，其中 `BACKLOG=1458`，但冻结 SHADOW 只有 12 条。当前版本的合法 seeder 已要求“冻结 SHADOW + 已有开奖”，因此大部分 1458 是旧版本宽泛历史 seeding 的遗留任务；这些任务没有开奖前 canonical，不可能生成严格 OOS，却一直污染 pending/健康判断。

修复：新增 `retireLegacyBacklogWithoutPredraw()`。每个低优先级维护切片最多处理 256 条，只处理：

- `task_class=BACKLOG`；
- 状态为 PENDING/RETRY_WAIT，或已过期 RUNNING；
- 不存在冻结 SHADOW observation。

这些行不删除，而是写为可审计的 `NO_PREDRAW_CANONICAL_TERMINAL`，终态码 `R819_LEGACY_BACKLOG_NO_PREDRAW`，并为每条生成 SHA-256 终态哈希。1458 条约需 6 个维护切片；正常情况下约一分钟完成隔离。合法冻结预测、开奖、严格 OOS 和 REAL 账本均不删除、不重算。

### P0：延后证据被“最近 5 秒有实时脉冲”永久饿死

R818 中 CORE 已提交 12 条，但 Deferred 仍全部 PENDING，100 号码面与 1200 动作表为 0。原因是低优先级写入要求 `isUnderPressure=false`；该条件既包含“真正有分析/结算在执行”，也包含“最近 5 秒出现实时脉冲”。抓取/协调持续脉冲时，系统可以永远没有 5 秒静默窗。

修复：增加 `hasActiveCriticalWork()`，低优先级维护只在分析或结算真正执行时让路，不再因单纯的最近脉冲永久饥饿。分析/结算运行中仍严格禁止 Deferred 抢占；空档按 1 或 4 个快照、每快照 120 动作分片恢复，正式动作宇宙仍为精确 1200，禁止删减或剪枝。

### P1：退出界面后的恢复链只在既有 Service 回调/Alarm/Job 上布防

原代码的 `Application.onCreate()` 完全不重新布防恢复。如果 OEM 在任务移除后杀进程，恢复依赖此前已成功登记的 Alarm/Job；在进程被系统重新创建但 Service 尚未启动的窗口，没有新的轻量恢复布防。

修复：`StarcoreApplication` 在用户未明确停用服务时，仅重新登记轻量 Alarm/Job 恢复；不打开 SQLite、不联网、不预测、不结算、不训练、不产生 BET。原有前台服务、WakeLock、BOOT/LOCKED_BOOT/USER_UNLOCKED/MY_PACKAGE_REPLACED、onTaskRemoved、onDestroy 与 JobScheduler 链保留。

Android 的“强行停止应用”和部分厂商自启动限制可以阻断所有广播/Alarm/Job；应用代码无法绕过系统强停。因此物理设备必须允许通知、后台运行、自启动和忽略电池优化，并执行 T29–T34。

## 为什么其他指标为 0

- `shadowSettled=0`、`strictOos=0`：是结算链未闭合的直接结果；修复后只能从真实开奖与既有开奖前冻结 canonical 生成标签，禁止补造历史预测。
- `Research Run=NOT_STARTED`：Research 正在等待严格 OOS 数据；OOS=0 时不启动训练/晋级是正确行为。
- `SelectedHitP=未生成`、ECE/Brier=N/A：模型与选择后校准 artifact 未训练/未证明，不允许把工程分数伪装成概率。
- `REAL=0`：MODEL_PROVEN=false、REALTIME_PROVEN=false 时由 `DecisionGateEngine_ONLY` 硬锁；不是 UI 漏显示。
- 旧期无 pre-draw canonical：会审计终态，不会在开奖后重跑模型，也不会计入严格 OOS 样本。

## 验证结果

- Android 36 / Java 17 全量编译：PASS（468 Java 源文件，1993 class）
- R36 Host Contracts：PASS
- R811 Host Contracts：PASS（227 assertions）
- R815 Host Contracts：PASS（131 assertions）
- R816 Runtime Closure：PASS（14 assertions）
- R817/R819 Runtime Research Closure：PASS（83 assertions，含结算单飞）
- 1200 动作空间：PASS
- 主机核心性能：96 history 约 0.9 秒；240 history 约 1.45 秒
- 192 MiB 低堆测试：PASS
- D8 Release：PASS
- 原 APK 签名证书连续性：PASS
- ZipAlign / APK ZIP 完整性：PASS
- APK SHA-256：`89548ea17065cd8b8c274160ea0993c4d401bfd14f641a4ee3eea92e4425b3b0`

主机性能不等于物理设备 P95；T29–T34 与 24 小时 soak 仍为 `RETEST_REQUIRED`。安装后不得把一次 UI 截图、静态构建、极小样本或 0 行统计宣称为设备/模型证明。

## 安装与首次验证

1. 不要卸载旧版；直接覆盖安装，以保留 DB 与冻结账本。
2. 安装后打开一次，允许通知、后台运行、自启动，并关闭该应用的电池优化。
3. 保持网络和屏幕开启至少 2 分钟。旧遗留 backlog 应按每批最多 256 条下降，约一分钟后不再污染非终态 pending。
4. 等待至少一个“安装后新产生的开奖前冻结 SHADOW”完成开奖；应看到 task 终态、shadowSettled 增加、strictOos 增加。
5. 查看 Deferred：应从 PENDING 逐步变为完成，并出现精确 100 number / 1200 action；不得少于 1200。
6. 执行 T29 屏幕开启 120 分钟、T30 熄屏 120 分钟、T31 划掉任务 30 分钟、T32 杀进程矩阵、T33 主/备网络、T34 24 小时 soak。
7. 只有设备 P95<=5000ms、严格 OOS 样本达到门槛、冻结 artifact/校准/策略证书全部通过后，才可另行评审 REAL；本包不会自动解锁。

