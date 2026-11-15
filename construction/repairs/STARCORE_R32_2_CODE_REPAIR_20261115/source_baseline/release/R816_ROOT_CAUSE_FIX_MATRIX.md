# R816 根因—代码—验收闭环矩阵

| 优先级 | 设备证据 | 直接根因 | 修改文件/方法 | 修复后合同 | 已完成自动验证 | 仍需设备验证 |
| --- | --- | --- | --- | --- | --- | --- |
| P0 | `OPEN_TIME_STALE`，约八小时 | 无时区 UTC 字符串按设备默认时区解析 | `ProviderTimeCodecR816#parseText`；`ApiParser`；`BinanceGameContract` | 同一 wire value 在任意手机时区产生同一 epoch | Shanghai/Tokyo 默认时区一致、Z/offset/epoch parity、自愈选择器断言 | 主/备源最新 draw age 和 source selection |
| P0 | `DRAW_DATA_STALE`、业务不活 | 错误 open time 已存库且冲突忽略不更新 | `DbStore#insertDrawInternal`、`repairDrawMetadataR816` | 相同业务身份仅修元数据；不同号码硬冲突；first-seen 不变 | metadata self-heal/first-seen 安全断言 | 覆盖安装后同一期再次获取是否修正 |
| P0 | `R815_OUTPUT_CONTRACT_FAILED|HISTORY_GAPPED` | 旧前缀 gap 污染当前连续尾段 | `HistoryIntegrityVerifierR815#contiguousSuffixAscending`；`V51RuntimeInputR36#freeze`；`prepareAnalysisHistoryR364` | 最新 base 向前连续至少 12 期即可冻结；冲突/base 不符仍硬失败 | 最近连续尾段、旧缺口隔离、少于 12 期 fail-fast | 新一目标期 canonical 100/1200 提交 |
| P0 | P95 19.899 秒、OOM 5、卡顿/闪退 | 1 秒 tick 同时执行重型生产和研究任务 | `AutoFetchService#runAnalysisWatchdogR36` | heartbeat 轻量；分析/结算/维护/研究 3/5/10/60 秒；压力下 yield | 512 MiB 96/240 历史核心 harness；静态调度门 | screen-on/off 120 分钟、24h soak，current-release OOM=0/P95<=5s |
| P0 | settled=0、任务长期 RUNNING/PENDING | 只排 backlog，未排 live current | `runSettlementBacklogMaintenanceR3611`；`SettlementCoordinatorR815#drainCurrent` | 每轮 current + backlog 都以 12 条上限排空 | 静态唯一调用链和回归门 | 新开奖后 task terminal、settled、OOS 增长 |
| P1 | deferred pending=166 | 小 chunk 且 watchdog 争用 | `DeferredEvidenceWriterR36`；`AppConfig.R36_DEFERRED_ACTION_CHUNK` | snapshot-only、无模型调用、300×4 完整 1200、压力 yield | 4-chunk 和 exact1200 断言 | pending 单调下降并最终 full=1200 |
| P1 | UI 卡顿、SQLite 压力 | 每次刷新执行数百个 SELECT | `DbStore#readUiStateSnapshotR816`；`MainActivity#loadDashboardState` | 单个有界 cursor，旧逐键逻辑仅异常回退 | 官方编译、静态路径门 | 滚动/切换页面无 ANR，cursor/heap 稳定 |
| P1 | 统计看似永远失败 | 新 release 被旧 P95/OOM 永久污染 | `RuntimeReleaseEpochR816`；`RuntimeHealthLedgerR36`；`ModelEffectivenessAnalyzerR35` | current release 与 lifetime 并列；旧事实不删、当前验收可重新测 | epoch/read-error/current-scope 门 | R816 epoch 后 current metrics 连续观察 |
| P1 | “有预测、无结算统计”难辨 | UI 未独立展示 SHADOW 生命周期 | `Models.RuntimeStats`；`DbStore`；`MainActivity` | predicted/settled/pending/hit/miss 分开；REAL 不混入 | 编译和字段/查询门 | 与 raw DB 逐项交叉计数 |

## 关闭条件

### 运行链关闭

- 最新 provider draw 的 issue 合法且 draw age 位于业务允许窗口。
- base/target 精确相邻；同一 target 至多一个 RUNNING owner。
- 最新未来 live 目标期产生 canonical=1、number=100、action=1200、rank1=1。
- current-release request-to-commit P95 `<=5000ms`，max concurrency `<=1`，OOM `=0`。

### 结算链关闭

- 每个拥有开奖前 canonical 的到期开奖期最终为 SETTLED 或有明确不可重试 terminal 原因。
- settlement task 不长期停留 RUNNING/PENDING；相同 draw 重放保持幂等。
- strict OOS writer 的 modelInvocation 永远为 0。
- SHADOW predicted = settled + pending；hit + miss = settled（只对可结算观察）。

### 证据链关闭

- canonical CORE 首写后不变。
- deferred snapshot hash 校验通过；每期 number=100、action=1200，pending 最终归零。
- 没有因修复性能而删减动作宇宙或重跑模型。

### 安全边界关闭

- REAL BET 行数保持 0。
- `MODEL_PROVEN=false` 时 DecisionGate 永远拒绝 REAL。
- 频率、追单、倍投、dynamic K、多位置 REAL 均不存在。

## 回滚原则

- 发生设备异常时先导出 DB、诊断全文和 logcat，再回滚 APK；不要清空数据库来隐藏问题。
- 回滚只回退 active runtime，不删除 canonical、settlement、OOS、critical error 或 lifetime health 记录。
- deferred 可暂停并从持久 snapshot 继续；不得重新计算开奖后的模型输出。
- 任何物理机失败都保持 SHADOW_ONLY，绝不通过降低门槛解锁 REAL。
