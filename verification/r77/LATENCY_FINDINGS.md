# R77 延迟排查：R76 手机报告的事实

输入是 2026-10-13 新报告，安装版 R5.4.2.76 / 577251035，发布哈希 f4c549365491c5f20e0a0e1370361dbc9442d94aae98a4e4be654df85baf92a3。当前进程起点 1791886582918（北京时间 18:16:22.918）。运行观察约 19:33:24，文件库存约 19:43:07；这些采样不是同一时间的单一事务。此取证之后已对 AnalysisRequestStoreR36 与 AutoFetchService 实施下述定点修改。

## 已确认的主要失效

`report-text-587f...` 的当前版本有效 WARM_MONOTONIC 共 70 条；另 1 条目标 617 是 REHYDRATED_UNMEASURED，旧请求早于本次进程，不能混进延迟分布。

| 阶段 | 中位数 | P95 | 最大 |
|---|---:|---:|---:|
| 请求到真正计算（queue_ms，包含元数据准备、失败重试等，不等同纯线程队列） | 9,018 ms | 29,767 ms | 54,317 ms |
| compute_ms | 997 ms | 11,574 ms | 18,277 ms |
| commit_ms | 65 ms | 5,157 ms | 7,880 ms |
| 请求到持久出号 | 12,221 ms | 41,346 ms | 55,453 ms |

58/70 条超过 5 秒。当前 76 个持久请求共被认领 128 次，71 个提交、5 个到期；51 个保留首次错误（含成功后重试修复的 46 个）。最后 120 条资源执行中，55 条未提交，其中 28 条租约栅栏丢失（HISTORY_PREPARE_ENTER 16、DONE 12）、10 条 HISTORY_BIND_FAILED、5 条 OperationCanceledException。没有证据支持“只是界面显示迟了”。

## 按期连接抓取、请求、出号、结算

`timeline.json/csv` 用 DIAGNOSTIC_SCOPE 中当前进程的 draws、fetch ledger、runtime event 和 settlement task 连接：本次 617–693 共 77 个新保存开奖。

- 49/77 在 draw open_time 后 5 秒内保存；4 个超过 30 秒：659（35.965 秒）、668（61.958 秒）、674（32.010 秒）、692（48.023 秒）。
- 保存开奖到下一目标请求：中位 57 ms，P95 1,066 ms；到本期开奖结算中位 216 ms，P95 1,227 ms。普通情况下保存后触发和结算并非主要 10–50 秒延迟来源。
- 下一目标 641、647、653、660、693 请求到期没有出号；669 没有请求。共 6 个本期开奖结算为 NO_PREDRAW_CANONICAL_TERMINAL，不能假称已影子预测结算。
- 692 开奖首次记录接收已晚 47.184 秒、48.023 秒保存；目标 693 请求时只剩约 10 秒，PIT 冻结 5.456 秒（线程 CPU 496 ms），随后 NUMBER_MODEL 被期限中断，最终缺预测。692 本期保存到结算还花 4.113 秒。
- 紧接着 693 在 +3.591 秒保存，目标 694 在 +6.107 秒完成持久出号（请求后约 2.38 秒）。说明正常计算本身可快，不是每期均需 30 秒。

### 网络证据边界

692 开奖分钟内 fetch ledger 第一个 request 直到 +45.308 秒。但运行观察还有 `fetchWriteError=COMPLETE:WRITE_REJECTED`；622、623、631、651、669、684 最早记录接收晚于保存开奖，证明这个 ledger 不能自动视为完整准确的网络首次到达证据。因此“45 秒网络线程没调度”仍需源码/注入验证；不能仅凭日志空白归咎上游，也不能说上游百分百已在 5 秒前发布。

## 指向代码的线索（待源码证实）

多处出现约 5 秒墙钟、低 CPU 的离散停顿：

- target 625 ACTION_BUILD 5,245 ms / CPU 282 ms，SELECTION_POLICY 5,196 / 414，DB_CANONICAL_WAIT 5,139 / 103。
- target 628 对应 5,436 / 336，5,233 / 479，5,148 / 121。
- target 691 PRE_HISTORY_METADATA 5,204 / CPU 19；后续正常模型 211 ms、选择 406 ms。
- target 692 首次 HISTORY_PREPARE_ENTER 栅栏丢失，10,451 ms 未提交；同一 durable attempt 重试后 compute 953 ms。

优先检查：AnalysisRequestStoreR36 租约续租/存活判定、V51RuntimeOrchestrator 的 HISTORY_PREPARE 栅栏、DbWriteCoordinator 的同步等待/超时、SharedPreferences 同步 commit 是否与 Android 异步持久化队列串行、取消信号生命周期。资源统计里 R36_COMPUTE 的 CP12EX1 独占 span 很小，不能与另一个聚合 inclusive compute_ms 相加。

目前不能从快照指定阻塞线程或调用栈；不要直接以 5 秒形状断言唯一锁死因。

## 界面和内存的证据

本次 frameSummary 47,441 帧，slow33 25、severe50 8、max233.696ms；truthBinds795，平均1.046ms、最大27.922ms。说明这次已观测的 UI 绑定大多快，不等于后台及时。当前堆峰值为分执行采样、最后一次约209MB；没有新增 OOM 证据。crashCount172 的 lastAt 1791806924296 早于 R76，本次安装退出属于 installPackageLI；不能把旧异常计为新版本崩溃。

## 可复现材料

- `analyze.py`：重新从本地原报告生成表格与统计；对不同 snapshot 时点、未测量样本、负的 receipt_to_db 显式处理。
- `summary.json`：分布与输入 SHA-256。
- `timeline.json/csv`：逐期时钟链路。
- `resources.json`：120 条资源行及源行号、墙钟/CPU span、完整 reason 文本。

源码恢复后要以“合法租约在写入压力下不被误判失效，旧租约仍不允许提交”“已有开奖迅速触发且保存最新倒计时”“网络请求调度不被报告/研究同步等待拖住”作为产品回归目标。上述还不是修复完成或设备性能验收。

## 实际代码修改与开发回归

- AutoFetchService：成功持久 claim 后记录本次续租时间；HISTORY_PREPARE/BOOTSTRAP 的 checkpoint 与 RuntimeOrchestrator 统一 10 秒续租合并。每次仍执行取消/期限 checkpoint；最终 canonical 前仍强制 SQL CAS 检查。
- AnalysisRequestStoreR36：pulse 和 bindHistoryHash 改用现有 runWriteOrThrowF01，使数据库执行失败保留本次调用原因；真正 owner/generation/hash/status 不匹配仍返回 false/拒绝。
- 最初 16 条开发回归通过，包括实际 SQLite 行更新、10 秒合并、强制续租、错 owner、SQLite 注入异常保留、相同历史幂等、冲突历史拒绝、取消仍被检查、从不制造 canonical。
- 使用精确 HEAD 旧 AutoFetchService 的负对照确实失败于多余历史续租；使用精确 HEAD 旧 AnalysisRequestStoreR36 的负对照确实失败于吞掉 SQLite 原因。
- 当前是实际修改源码的开发 overlay + R75 依赖/Android host adapter 验证，明确不是最终 R77 APK 字节码验收。最终构建后需无产品覆盖重新运行 lease_history_run.py。
- 本次 203 条 DBTX75：BEGIN 最大 4.065 ms、BODY 最大 19.062 ms、END 最大 18.518 ms；已测 R932 事务不是上述五秒等待来源。完整表 report-dbtx75.json。

## R77 后续已修改：退出 owner 不再空等 45 秒

如果工作线程已退出，而 catch 中 markRetry/markExpired 恰逢数据库失败，R76 会清掉内存 owner，但持久行仍 RUNNING，新的 claim 只能等原 45 秒租约。新增恢复 ticket 在原 analysisHandler 上每 250 ms 有界重试精确 owner CAS；始终固定 requestId/owner/generation/hash 和原 wall/monotonic deadline。成功释放后按原 retryAt 唤醒已有管线，不额外并发执行分析。

- 同一退出 owner 最多一个 pending callback；仍存活 owner 禁止恢复；CAS mismatch 即结束；stop/destroy 撤销 callback。
- transition 返回 null、WRITE_COORDINATION_FAILED 或抛出异常可重试；重试释放不增加 attempts。
- release 已成功而后续 wake 抛错时只重试 wake，不重复释放；原 deadline 已过则只尝试到期标记，不重开窗口。
- 共 34 条实际服务/Store/真实 SQLite adapter 回归通过，覆盖上述行为。已换完整当前开发源码依赖重新执行，未覆盖任何被测产品类。产物 complete-development-*；仍不是最终 APK 或手机运行验收。

## R77 后续已修改：HTTP 开始不等完整状态快照

runFetchChannelOnce 原先在 HTTP 前调用 updateRuntimeState(FETCHING)，后者读取回测/学习/Decision 状态，并经 DbStore 独立 setState 持久写约 30 个字段。FETCHING/RUNNING 每圈切换让相同 stateKey 节流失效。现 HTTP 前仅更新内存状态，网络尝试结束的原有成功/失败路径再发布完整状态；独立 countdown owner 不变。DbStore 的快照批量写由另一修复负责。

实际 runFetchChannelOnce → ApiClient 测试在 Android SQL adapter 边界阻塞 updateRuntimeState 的数据库访问，并拦截 HTTPS 返回合成 503，证明网络 admission 不依赖该状态快照。新码 4 PASS；精确 HEAD 旧 AutoFetchService 同一测试未通过 admission 断言。测试没有真实联网，不宣称服务端固定 5 秒发布。

4 个迟到期的已记录接收→保存分别 81/75/28/839 ms；659/668/674 的本次成功 HTTP latency 为 496/502/784–813 ms，692 latency 字段缺失。前段迟到主要已发生在记录接收之前。report-slow-fetch-windows.json 保留这四段请求记录；其 ledger 缺写问题仍意味着空白区不能自动当作网络没有运行。

## R77 后续已修改：fetch Handler 不承载展示和回测心跳

- Decision 后置状态/通知刷新合并为一个可撤销 Runnable，投递到现有 runtimeGuardHandler。
- 回测 heartbeat 的 start、执行后 re-post、stop 均使用同一 runtimeGuardHandler。原有 stopWork 和部分启动失败 reset 仍清空该队列；没有新增线程。
- scheduleNextFetchLoop 只读 clock/内存 busy/pending/realtimePriority 信号，不再为了安排下一次 HTTP 先查询 SQL。数据合法性及 exact target 的校验位置未移动。
- 扩展后的实际 Service → ApiClient 测试 15 PASS，覆盖 guard 队列未执行时仍可建立本地 HTTP、heartbeat 执行后的 re-post 归属、stop/restart 无残留、同一 provider issue/openTime/numbers 保存及 clock target 更新。精确 HEAD 旧服务在同一测试失败于仍把展示放 fetch queue。产物 fetch-scheduling-development-* / fetch-scheduling-negative-*。
- 释放恢复额外核验：若目标已开奖或旧 token 已被新目标替代，即使原 deadline 尚未到也只 EXPIRED，不转 RETRY_WAIT。

这些是受控故障与真实 SQLite 业务路径的开发回归，未替代手机运行与最终 APK 字节码复跑。审计 FetchLedger 仍同步保存权威接收事实，没有改成丢弃或异步补造时间戳。

## 原子状态批次失败与业务结果隔离

DbStore.setStates 现为严格原子批次并抛出失败。已审查服务调用点，把任务移除尾部展示、新开奖展示、已完成 research capture 的 summary、教师 pump summary、低内存恢复展示 5 处改用 trySetTelemetryStatesR77；这些必须在权威事务外，失败整批回滚但不阻断后继调度。权威 receipt/cursor 写入未替换。updateRuntimeState 的持久展示失败现在保留当次内存状态/clock 广播。

新增 2 项实际服务故障回归：在 newDrawState 的 app_state insert 注入失败，确认精确开奖仍保存且后续 realtime dispatch 继续；在 service_status 插入注入原子批次失败，确认当次 ACTION_STATE_CHANGED 广播仍发送。fetch-scheduling-development 总计 15 PASS。

## Gradle bootstrap 复跑

当前准备构建产出的完整 Gradle release classes（未最终提交/build）已直接运行相同 fixture：lease 34 PASS、fetch 15 PASS，产品类与测试 adapter 无碰撞，前后产品 class SHA 一致。没有 R75 产品依赖或测试产品类覆盖。封存最终构建后仍需以 --final-bytecode 重新执行。
