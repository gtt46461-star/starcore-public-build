# 10.29 真机检测报告分析与修复（R27.1）

**报告来源**：用户上传的 `10.29____.zip`，job 20c8e1b1…。设备 V2353A（vivo，Android 14）。

## 0. 先说结论：这份报告测的不是 R27

| 字段 | 报告值 | 含义 |
|---|---|---|
| sourceCommit | `1713d114…` | R26 基线源码 |
| apkHash | `a335594e9c4a2ed5da6c4f3ce9087597eb1f40a3d75967c1c9815d04e8dcb60a` | CI 产物里 `baseline-apk/` 的 **R26 对照包** |
| 版本 | R26-2026.10.28-reportfix2 / 577271029 | 不是 R27（577271030） |

**原因是我打包的问题**：CI 下载包里同时放了 R27 包和用于升级/回滚测试的 R26 对照包，用户装成了对照包。

**R27.1 的修复**：下载产物 `R27-APK` 里只放 R27 的 release / debug 两个包；对照包移到单独的 `R27-test-evidence-NOT-FOR-INSTALL`。

这份报告里暴露的后台问题，大部分在 R27 里同样存在，或者被 R27 放大了，所以全部按 R27 修复。

## 1. “后台不运行”的根因（按影响大小排序）

### 1.1 全新安装后，研究存储迁移要两个多小时

| 现象 | 报告证据 |
|---|---|
| 47 个研究任务里有 46 个停在等待存储 | `BLOCKED / WAITING_SELF_CONTAINED_STRATEGY_STORAGE`，`cursor=0`、`steps=0` |
| 只有 `schema` 任务在动，且很慢 | 17 分钟走了 35 步 |
| 研究库一直没就绪 | `research=INITIALIZING`，`researchSchemaReady=false` |

**原因**：

- 存储迁移约有 265 个单元，代码每个调度轮只执行 1 个。
- R26 基线在实时压力（`realtimePressure=true`）下，会把整个研究会话让出去，所以平均约 30 秒才走一步。
- 用户因签名不同重装，等于从零开始。按这个速度，研究要 **2 小时以上** 才能开始。

**修复（R27.1，`RuntimeCoordinatorR26.execute("schema")`）**：

- 每个时间片连续执行迁移单元，最长 200 毫秒，每个单元仍是独立的短事务。
- R27 已经去掉了“实时压力时整段让出”，现在只在资源受限或取消时让出。
- 预期全新安装在几分钟内研究就绪。CI 模拟器新增“全新安装”阶段，在第 1/2/3/5/10/15 分钟各拍一次快照来测量这个时间。

### 1.2 只开了 3 期：抓取写入被拒，整期丢失

| 现象 | 报告证据 |
|---|---|
| 启动后 10 期里只有 3 期出了预测 | `K1_Coverage=3/10` |
| 漏掉的期没有分析请求 | `NO_ANALYSIS_REQUEST` |
| 抓取写入被拒 | `fetchWriteError=BEGIN:WRITE_REJECTED`，`FETCH_STALE` |
| 历史回补失败 | `R914_DRAW_HISTORY_BATCH … CP3_DB_BUSY_DEADLINE`，`HTTP_REQUEST_RECEIPT_NOT_DURABLE` |
| 数据库写锁等待接近上限 | `dbWaitMaxMs=1980`，上限是 2000 |

**因果链**：

1. 抓取开始时要写一条“请求回执”。写锁被别的事务占用超过 2 秒，这条回执就被拒绝。
2. 开奖返回后，因为没有持久回执，无法绑定到正式开奖（`HTTP_REQUEST_RECEIPT_NOT_DURABLE`），这一期没写进去。
3. 分析请求只给“最新一期之后的下一期”建。等下一轮抓取把几期一起补进来时，只有最后一期会出预测，前面几期全部变成 `NO_ANALYSIS_REQUEST`。

**修复（R27.1）**：

- **R27 研究写入降为后台级**：所有 `R27_*` 写入归为后台，抓取、开奖、分析这些关键写入在等待时，研究写入先让路（`DatabaseCoordinator.isBackgroundOperationR909`）。R27 新增的线程池和批量写入原来不在这个名单里，会和抓取平级抢锁，这是 R27 引入的退化。HOST 测试 `r27ResearchWritesYieldToCriticalBusiness` 锁定了这个行为。
- **抓取回执写入重试**：`FetchLedgerR925.begin` 最多重试 3 次，每次 2 秒，用 `INSERT OR IGNORE` 按同一个 request_id 防重复。写锁偶尔忙，不再直接丢一整期。

### 1.3 vivo 系统强制停止应用

| 现象 | 报告证据 |
|---|---|
| 应用被系统强停过一次 | `[FORCE STOP] stop com.starcore.research.r13 due to stop by com.vivo.sps` |
| 电池限制未关闭 | `后台系统能力=SETUP_REQUIRED_BATTERY_RESTRICTED｜电池无限制=NO` |

**这一项代码无法绕过**，需要在手机上设置：

1. 设置 → 电池 → 后台耗电管理 → 星核创研 → **允许后台高耗电**。
2. 设置 → 应用与权限 → 权限管理 → **自启动** → 打开星核创研。
3. 最近任务里把星核创研**下拉锁定**，不要一键清理。
4. 通知权限保持开启：前台服务要靠常驻通知才能存活。

### 1.4 CI 模拟器又查出一个 R27 自己引入的问题（已修，commit d17d4bb）

范围：EMULATOR（CI 模拟器 API 34），不是真机。

| 现象 | 证据 |
|---|---|
| 16 个专家任务全部被隔离，不再运行 | `r27_task_error`：`R26_CONTINUATION_QUANTUM`，状态 `QUARANTINED` |

**原因**：R27 给续算切片传了 1000 毫秒，但切片只允许 1～200 毫秒，所以每个专家任务第一次运行就报错，被隔离。

**修复**：

- 新常量 `CONTINUATION_SLICE_MS = 200`，三个调用点都改用它。
- 启动时把只因这个错误被隔离的任务放回 `READY`，旧安装升级后不用重装。
- HOST 测试 `continuationSlicesUseAnAcceptedQuantum` 扫描全部 `slice(...)` 调用点，防止再犯。

**同一轮模拟器的其他结果**：

- 升级、回滚、再升级都能安装运行。
- 全新安装 1 分钟内研究存储就绪：265 个单元，等待存储的任务 0 个。1.1 的修复有效。
- **仍未解决**：全新安装 15 分钟里开奖数一直是 499，冻结预测只有 1 期。根因还没查清。新一轮 CI 在模拟器日志里打印抓取账本、抓取拒绝、相关 app_state 和 logcat，用来定位。

## 2. 其他观察（不是后台根因）

| 项目 | 报告值 | 说明 |
|---|---|---|
| 单期分析耗时 | 请求→提交 P95 = 17.2 秒，目标 ≤5 秒 | 热路径里 `R36_COMPUTE` 5.2 秒、`SELECTION_POLICY` 3.3 秒、`ACTION_BUILD` 1.7 秒。1 分钟一期来得及；但写锁竞争时会挤占抓取。R27.1 没有改主链算法，规则一个字没动。 |
| 超时过期 | 1 期 `B05_TIMEOUT … over_ms=260999` | 同上。 |
| REAL | 0 次 | 没有合格的模型证书（`GATE_NO_QUALIFIED_MODEL_PROOF`），这是主链规则的正确行为，不是故障。 |

## 3. 下一份报告请这样采集

1. 先卸载旧包，装 `R27-APK` 里的 **STARCORE-R27-2026.10.28-release.apk**（内置秘钥，不用手填）。
2. 按 1.3 完成四项手机设置。
3. 打开应用后连续运行至少 **2 小时**，期间不要强行停止。
4. 导出完整检测报告。

看这几个字段判断是否修好：

| 字段 | 期望 |
|---|---|
| `sourceCommit` | 是 R27 的提交，不是 1713d114 |
| `research` | `READY` |
| `WAITING_SELF_CONTAINED_STRATEGY_STORAGE` 的任务数 | 0 |
| `K1_Coverage` | 接近 100% |
| R27-01～04 章 | 有数据 |
