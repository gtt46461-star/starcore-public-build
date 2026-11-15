# STARCORE TIANSHU R816 完整修复与交付报告

## 1. 最终发布身份

| 项目 | 最终值 |
| --- | --- |
| applicationId | `com.ethk3.novaonepro` |
| versionName | `V57.15.3-R3.6.13-816` |
| versionCode | `577190825` |
| compile / min / target SDK | `36 / 21 / 28` |
| Java / Dex | Java 17 源码与字节码，D8 release Dex |
| 数据库 | `binance_k3_nova_one_pro_v57.db`，`DB_VERSION=36`，兼容性 schema revision `3` |
| 游戏与玩法 | `Binance1-PK10`、单位置、固定 `K=3`、每期 100 个号码概率、1200 个唯一正式动作 |
| 发布边界 | `SHADOW_ONLY=true`、`MODEL_PROVEN=false`、`REAL_BET_HARD_DISABLED=true` |

R816 是对设备报告中可复现运行链故障的代码级闭环修复。它不是通过降低门槛、删除 1200 动作、伪造历史预测或清空故障计数来让报告变绿；它修复生产链的实际输入、调度、结算和读取路径，并保留所有安全红线。

## 2. 为什么 R815 仍然卡顿、闪退、不出号、不结算

设备报告中的证据不是单一模型问题，而是四个上游运行问题叠加：

1. `Provider primary/backup ... OPEN_TIME_STALE` 的年龄约为八小时。供应商返回的不带时区 `openTime` 是 UTC wire time，旧解析器却使用手机默认时区。同一份响应在不同时区得到不同 epoch，导致最新期被误判为陈旧数据，业务循环因此停在 `DRAW_DATA_STALE`。
2. 最新有效历史实际可能具有连续尾段，但旧校验把更早的前缀缺口计入整个窗口。在完整 100/1200 输出已经计算后，输出合同仍以 `HISTORY_GAPPED` 拒绝提交，造成 `numbers=0|actions=0|rank1=0` 和重复重试。
3. 一秒级 watchdog tick 同时触发分析协调、结算扫描、延后 1200 动作落库和研究任务。它使大量 SQLite 查询、对象分配和排序在同一时刻竞争，和设备上的 `P95=19899ms`、`OOM=5`、UI 卡顿/闪退一致。
4. 后台维护只排历史 backlog，没有同时排当前 live settlement task；旧任务可长期留在 `RUNNING/PENDING/WAITING_DRAW`，使 SHADOW 账本持续 `settled=0`，严格 OOS 和统计自然无法推进。

同时，旧主界面每 4–7 秒逐键查询数百个 `app_state` 项，又放大了数据库锁竞争和字符串分配。以上故障链发生在模型证明之前，所以本次修复优先处理 runtime chain，且绝不以模型未训练为借口掩盖生产者失败。

## 3. 已实施的完整修复

### 3.1 数据时间和现有数据库自愈

- 新增 `ProviderTimeCodecR816.java`，所有供应商时间入口统一委托给它。
- 纯数字秒/毫秒 epoch 保持原语义；带 `Z` 或显式 offset 的 ISO 时间保留显式时区；无时区日期字符串始终严格按 UTC 解析。
- 解析使用 `Locale.US`、`setLenient(false)` 和完整字符串消费检查，拒绝模糊日期及半截解析。
- `ApiParser.java` 与 `BinanceGameContract.java` 不再各自实现时区相关解析。
- `DbStore.insertDrawInternal` 遇到“相同期号 + 相同十位号码”时，不再简单 `CONFLICT_IGNORE` 后结束；它只修复 `open_time/fetch_time/source/raw/hash` 等元数据。
- 元数据自愈严格保持业务身份、十位号码顺序和合法 `first_seen_at` 不变。只有新的时间明显更接近不可变首次接收时间时才替换错误的 open time；同一期不同号码仍是硬冲突。
- 因此安装 R816 时无需删库或清数据。下一次收到相同开奖响应即可修正旧 R815 的错误时间元数据。
- 历史读取与顺序判断以合法业务期号为主，不再让错误 open time 改变下一期、历史顺序或业务身份。

### 3.2 历史连续尾段和分析提交

- `HistoryIntegrityVerifierR815.Result` 新增 `contiguousSuffixAscending()`，从最新 base issue 向前精确寻找连续尾段。
- `V51RuntimeInputR36.freeze` 先过滤 `issue <= baseIssue`，再检查同一期冲突和 `maxIssue == baseIssue`；仅冻结最近连续尾段，且少于 12 期时在任何 100/1200 大分配之前硬失败。
- `R36AnalysisHistoryPolicyR364` 与 `AutoFetchService.prepareAnalysisHistoryR364` 使用同一连续尾段规则，不再由历史前缀中的无关旧缺口否决当前合法分析。
- `ProductionPredictionDispatcherR815` 仍要求冻结输入自身无缺口、输入哈希一致和唯一 owner；这不是放松 PIT 或完整性门槛。
- `PredictionOutputContractR815` 的 100 个号码、1200 个动作、rank 唯一性验证改用定长布尔数组，不再为每次验证创建大 `HashSet`。
- 保持精确 `10*C(10,3)=1200`；没有删除、剪枝、降级成 K4、动态 K 或缩小动作空间。

### 3.3 结算与严格 OOS 闭环

- `SettlementTaskStoreR36.seedHistorical` 不再用可修复的 provider open time 重复判定 PIT；唯一严格 OOS writer 仍以冻结 canonical、不可变 `first_seen_at` 和实际开奖执行最终验证。
- `SettlementBacklogAuditR3611` 先识别终态，再按 `first_seen_at` 分类 PIT，避免已经合法终结的任务被错误重算成待处理。
- `AutoFetchService.runSettlementBacklogMaintenanceR3611` 每轮同时执行 `SettlementCoordinatorR815.drainCurrent(store,12)` 和 `drainBacklog(store,12)`，当前任务和历史任务都能以有界批次推进。
- `SettlementEngine_ONLY`、`OosAfterDrawWriterR36` 和 first-write-wins 保持不变；任何缺少开奖前冻结 canonical 的旧期会留下明确终态，绝不在开奖后补做预测或伪造命中标签。
- `DbStore.RuntimeStats` 增加独立 SHADOW 预测、已结算、命中、未命中和命中率字段；主界面不再把 canonical 总数冒充已结算样本，也不把 SHADOW 混入 REAL 盈亏。

### 3.4 卡顿、SQLite 竞争与 OOM 修复

- 保留 1 秒轻量 heartbeat，但把分析 reconcile、当前结算、生产维护和研究任务分别限频到 3 秒、5 秒、10 秒和 60 秒。
- 研究 sidecar 仅在无实时分析压力、延后证据满足阈值且无 ready-to-settle 生产任务时运行；生产结算和证据闭环优先。
- `DeferredEvidenceWriterR36` 继续从耐久 snapshot 恢复，不调用模型；单动作块从 120 提高到 300，完整 1200 动作稳定分成 4 块，每 slice 最多 6 次有界写入。
- 任意实时压力都会让 deferred/research 主动 yield；没有把 `OutOfMemoryError` 吞成成功。当前 release 的 OOM 和延迟使用独立运行 epoch 计算，历史 lifetime 计数仍在报告中保留。
- `MainActivity` 的数百次逐键 `app_state` 查询改成 `DbStore.readUiStateSnapshotR816(map,2048,4096)` 单个有界 cursor；逐键读取只作为异常回退路径。
- 以上改变减少 UI 刷新、service watchdog、结算和证据落库对同一 SQLite 连接的争用，同时避免频繁大对象分配。

### 3.5 当前版本健康窗口与诚实诊断

- 新增 `RuntimeReleaseEpochR816.java`，首次打开 DB 时持久化 `r816_runtime_epoch_at`。
- `RuntimeHealthLedgerR36` 和 `ModelEffectivenessAnalyzerR35` 的 release gate 只统计 R816 epoch 后产生的 P95、并发和 OOM；旧版本 lifetime 事实继续显示，不能被删除或伪装成零。
- 错误读取仍是 `READ_ERROR`，不会显示成零样本 PASS。
- `MODEL_PROVEN=false`、`SHADOW_ONLY=true`、`REAL_BET_HARD_DISABLED=true` 均为编译期和运行期双重硬锁。

## 4. 升级后的确定行为

1. 使用同签名 APK 覆盖安装，不卸载、不清数据。
2. 启动后建立 R816 健康 epoch；历史 R815 故障仍可在 lifetime 诊断中看到，但不会永久污染 R816 验收窗口。
3. 下一次 API 获取相同期时，错误时间元数据会在业务身份完全一致的条件下自愈。
4. 当最新 base issue 拥有至少 12 期连续合法尾段时，只创建一个 durable analysis request，由唯一 single-flight owner 计算并提交完整 100/1200 SHADOW canonical。
5. 当前结算和历史 backlog 按有界批次持续排空；已冻结开奖前 canonical 的期次在开奖后产生严格 OOS 标签。
6. 延后证据从 snapshot 分块补齐，不重跑开奖后模型；UI 显示独立 predicted/settled/pending/hit/miss。
7. 已经过期开奖但从未有开奖前 canonical 的旧期不会“补出号”或伪造结算。这些期会保持明确 terminal 原因；可验证的统计必须从 R816 安装后的未来 live SHADOW 样本开始累计。

## 5. 自动化验证与构建事实

正式构建已通过：

- 官方 Android API 36 全量 Java 17 编译：460 个 Java 源文件，1970 个 class 文件。
- R36 回归契约：通过。
- R811 回归契约：208 条断言通过。
- R815 兼容契约：131 条断言通过。
- R816 runtime closure：14 条断言通过，覆盖 UTC/offset/epoch、跨设备时区一致性、现有元数据自愈、连续尾段、少于 12 期 fail-fast、四块 1200 证据和 REAL 锁。
- R816 100/1200 性能 harness：96 期与 240 期两档都在 512 MiB 进程中通过；验证 100 个有限概率、1200 个唯一动作、唯一 rank=1 和禁止 BET。
- D8 release Dex、AAPT2 Manifest/resource 重编、zipalign、APK ZIP 完整性全部通过。
- APK v1/v2/v3 签名验证通过，签名证书连续性通过。
- 静态 owner/红线/敏感信息门禁通过。

最终产物：

| 项目 | 值 |
| --- | --- |
| APK | `Starcore-Tianshu-V57.15.3-R3.6.13-816-release.apk` |
| APK bytes | `3,257,198` |
| APK SHA-256 | `602c937773078cb11040d123ca290468c5c71d7e98712832240fca64d751650b` |
| DEX entries SHA-256 | `5e2910fb152e59080f50b2fc1f11c82ec818223ca461ceafd8867e00f6f98977` |
| Source tree SHA-256 | `64da6994bfd570f2ff59c00e272ae35858677fd4abf2b7ec20efe2b981d464e8` |
| Resource manifest SHA-256 | `5c894ce6cfb35d6c6e9630be10c842d79c474f1b0413d28ee83ad228d3ce4b63` |
| Toolchain SHA-256 | `48cb4f1da92d7112c54d349209a89db0635be60d3c573dc5947f0dc9d063320f` |
| Signing certificate SHA-256 | `ae7e1b4a5a71f1f299d4a0b2472c6a36513873514e78190d42d6cfba3340116f` |

原始日志和机器可读 manifest 均在总包的 `BUILD_EVIDENCE/` 中。

## 6. 真实边界

- 静态/主机构建无法替代真实手机上的 screen-on、screen-off、划掉任务、杀进程、主备网络和 24 小时运行。因此 `PHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED`。
- 本次构建没有离线训练出可证明模型，严格未来 live OOS 样本仍需从零合法积累。因此 `MODEL_PROVEN=false`，不能宣称命中率、ECE、Brier 或 Wilson 达标。
- REAL 自动解锁、追单、倍投、频率欠债、dynamic K、多位置 REAL 均被禁止。
- 只有执行 `R816_PHYSICAL_DEVICE_RETEST.md` 并保存真实 DB/logcat/报告后，才能把对应设备项从 `RETEST_REQUIRED` 改成观测结果。

## 7. 可复现构建

```bash
cd <SOURCE_ASSETS 根目录>
export DUOQU_API_KEY='<已配置的真实密钥>'
./scripts/r816_behavior_gate.sh
./scripts/r816_build_release.sh
```

签名私钥、PKCS#12、密码和 API 密钥不属于可公开交付源码，未写入源码树、日志或总包。总包中的 APK 已完成正式签名；构建脚本支持在受控环境中使用受信任基线的同签名材料重新构建。
