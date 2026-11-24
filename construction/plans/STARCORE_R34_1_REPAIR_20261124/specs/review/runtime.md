# 122 版后台出号与结算的补充施工条款

应用源码为 `construction/r34-20261123@5562f9b19aa09f9f7d9e69ca085db56c537967fc`。本节细化 B01、B04、B05、B10 原工单。类名均位于 `app/src/main/java/com/ethk3/novaonepro/`，其中 `IssueTimelineWriterR30` 在 `strategyv2/`。所有下列故障注入与真机验收均待实施，源码暴露的边界不能单凭本次现场快照判定为唯一根因。

## 现场基线与证据的使用方式

- `runtime-observation-heartbeatSummary.txt` 的同次采样里，抓取已经到 136，分析和结算成功水位仍为 133，136 的明细是 `NO_PREDRAW_CANONICAL_TERMINAL/FROZEN_OBSERVATION_MISSING`。此时心跳年龄是 331ms。当前 122 的 132–137 六个已开奖目标只有 133 留下 canonical，138 尚未成熟。恢复请求 56、实际启动 25、进展字段 0 是保留状态，不能推断本进程重启 25 次。影子、canonical 和 REAL 分角色计数。
- `runtime-observation-backgroundRuntimeCapabilityR31.txt` 记录 runtimeSdk=34、targetSdk=28，采样时服务、通知、闹钟及电池豁免可用，24h 和 72h 均为 NOT_RUN。“用户重新开界面后恢复”是现场反馈。源码中 Activity 新建约 1.8 秒后 reconcile，退到后台的生命周期也可能触发 reconcile；仅凭当时的心跳无法判明是唤醒、排队、锁争用或 UI 补救触发了进展。
- `R30_REPORT.json.objectIndex.boundedPayloads` 中 r31FetchTiming 总数 16384、内联 18；timeline 物理表 0 行；timelineEventRows 在 r27_chart_cache 中有 1488 行、内联 8。一个期的多阶段、多角色和重复 HTTP 请求都可能产生多行。先按 request/attempt/role/issue/boot/watermark 追踪，再计算指标。

## 对应工单与正常入口

### R341-B05-01 · 服务回调保活与实时请求

`AutoFetchService.fetchLoop` 通过 finally 调用 `scheduleNextFetchLoop`。当前顺序为计算 delay、写节拍遥测、移除原 callback、调用 postDelayed，返回值未受理。遥测异常、Looper 正在退出或 post 返回 false 时，finally 仍可能没有排上下一次。另一处 `ensureAnalysisWatchdog` 每次都移除并按当前时间延后 1000ms，频繁调用有推迟已到期回调的可能。当前 `ensureRuntimeLoopsAliveV17` 还会同步读写业务状态；`runAnalysisWatchdogR36` 用 wall 间隔节流并回调 `postAnalysisForLatest`。这些真实入口都需纳入修改。

按 service-generation、loop、epoch 保存一个可核对的绝对 elapsed due，加 posted/running/fence。已有更早预约保持，改期仅向前；回调先验证 generation。post(false)/抛错显式回到待调度，交另一个已有 service control 入口处理，不能由同一退出中的 Handler 原地自旋；业务工作或遥测失败均须留下后续尝试。线程仍活但持锁时只做有界取消/等待，物理死亡才复建 worker。watchdog 不在自身线程同步执行阻塞的 SQL。重复 START、时钟回拨和 UI 退出不得重置 due。断网后沿用有界退避，在服务代际内注册并注销与设备 API 兼容的网络恢复回调，在线事件只唤醒既有单在途抓取；重启后由 last durable draw 重建任务，断开回调的竞态由 fence 拦截。

`saveApiResultDraws` 成功后 `handleLatestDrawAfterSave` 调用 `postAnalysisForLatest → BusinessReconcilerR36.reconcile`；reconcile 持久化下一目标，亦排本期结算并读取状态。保持真实已提交 draw 为恢复依据。service 内对“开奖已落库、通知尚未发”重启进行对账，并覆盖 latest、相同最新期去重响应、历史补录、provider stale 四条路径；每期有且仅有一个逻辑请求与结算任务。实时计算/长 SQL 在各自已有 owner 上执行。不能把 post 成功、重复网包或一次 reconcile 当作实际冻结/结算。

工程反例沿用 B05-01-T01/T02。从启动后不创建 Activity，给 watchdog 持续投递早于 1 秒的 ensure；分别模拟遥测抛错、Handler 拒绝、UI/系统重复投递、断网后在线、中途进程死亡、旧 callback 晚到及时间回拨。输出每次计划/实际/下一 due、代际、handler return 和最终同一目标回执。健康恢复后的下一合法目标必须冻结，只有一个活动执行权；主动关闭后迟到事件不能重启服务。

### R341-B05-02 · 修正真正持有恢复状态的类

`InternalWatchdogReceiverR815.onReceive` 和 `ServiceWatchdogJobService.onStartJob` 调 `RecoveryCoordinatorR815.request`。R815 的限流窗口为 wall 时钟十分钟、最多三次。requestedGeneration=0 取 current+1，可能绕过“同代 15 秒内合并”。第三次处于 FETCH_SETTLEMENT_ONLY 时，即使 BootReceiver.reconcileAutoFetchService 返回 false，Result 亦可返回 accepted=true/SAFE_DEGRADED。Job 目前按 !accepted 决定重试。另一个同名相近的 `RuntimeRecoveryCoordinatorR926.record/recordMessage` 只记异常。现场确实出现 RECOVERY_RATE_LIMITED；上述第三次失败是否发生尚无原件。

在 R815.request/markServiceStarted/markBusinessRestored 中分开“控制请求已送达/合并到有主请求、资源等待、降级、限流、需要重试”。Job 和 Alarm 按 disposition/nextDue 行动，第三次投递失败不会成为成功；成批触发不消耗同一根因的多个 restart 名额。区分 in-process reconcile 与真实 onCreate，不将“已运行服务后来健康”计作实际重新启动。冷却和请求年龄在同 boot 用 elapsed，保留 wall 供人读；重启重新对照合法待办。旧 preferences 无 boot/token 的证据标 LEGACY_UNSCOPED，沿用用户开关；不得迁成恢复成功。

在 `ServiceHeartbeatStore.noteResearchRecoveryRequestR31/markResearchHealthR31` 补作用域和证据身份。现有比较接受 ordinal>=baseline 且 hash 不同。施工时用同 lane、数据集、业务游标和已提交事务回执判有因果的进展；同 ordinal 异 hash 认作身份歧义/修订单独处理。owner 重建后的第一次采样保留旧 recoveryId 与原基线，建立跨代映射，再判断更晚的真实提交。规范化投递结果写入 B01 进度模型，R926 继续保留原异常。B05-02-T01/T02 注入前两次成功/第三次投递失败，Job/Alarm 同时到达，wall 回拨，旧 artifact 重读，同 ordinal 改 hash，当前 lane 真正提交。预计只有最后的合格提交关闭一次恢复；未送达状态可在冷却/新输入后再试。

### R341-B04-04 · 每期影子要有单独的失败语义

`AutoFetchService.handleLatestDrawAfterSave` 仅在 isNew 时将 `BaselineShadowR34.freeze` postAtFrontOfQueue，源码未检查此处布尔返回值。freeze 的输入不满足条件时直接返回。首次事务先写 RETURN_NOT_OBSERVED，第二次以真实调用返回时间升级为 FROZEN；`settleWithin` 对返回时刻无证据的行最终写 UNVERIFIED_PIT。这些分支需要归入正式的影子逐期分母。为每个期记录期望、投递受理、输入缺失或冲突、首次事务结果、真实成功返回及最终标签。目标仍合法时，可由持久的 base 重新派发缺失的首次写入，沿用唯一键及相同输入；已有 RETURN_NOT_OBSERVED 的行需凭当时真实返回证据处理。首次事务返回到持久证明之间崩溃的，不能事后拿重启时刻补盖“赛前”。到截止期固化对应失败类，继续下一期。S0、候选 shadow、canonical、唯一主链 REAL 分别统计。按 B04-04-T02 对 handler 拒绝、首次写后崩溃、迟到标签、同 hash 重放和跨日做对照；要求无重复 hit，无泄露，每个应有影子的目标均有可查归属。

### R341-B04-06 / R341-B10-02 · 让新期的计时证据持续可写

`FetchLedgerR925.persistTraceR31` 在表满且 request_id 为新值时直接返回，递增丢弃计数。transport、parse、validated 对同一被拒请求都可能再次尝试计数，dropped 不能充当丢失请求的精确数量。单独调高上限只会推迟下一次饱和。保留当前 16384 行原件，区分“唯一请求缺失”和“阶段写入失败次数”。在 B04，仅凭同身份的实际返回回执判定时延；遭容量拒绝或队列淘汰时继续业务，输出明确的计时覆盖缺口，并证明历史/网络请求不能伪装实时请求。B10 接入 `DataRetentionGovernorR900.prepareArchivePageCP12/publishAndPruneCP12` 的表适配，或与其同等验证的独立计时页归档，低水位前提前准备滚动空间。写入校验和、计数、首尾键及归档可读性后按原件身份 CAS 裁小页；保护未闭合请求及仍被证书、故障/导出钉住的原件。写盘失败/无空间时停止裁剪，可见降级，不能阻塞下一期提交。恢复后接纳新 request，拒绝静默丢失。B04 的局部验收不依赖 B10 完成归档。B10-02 的满表 fixture 必须覆盖中途崩溃重入、磁盘不足与解除保护后的新增。报告分别列 eligible/observed/missing、按唯一 request 去重的拒绝与原件位置。

### R341-B05-03 · 去掉任意放宽性能阈值的空间

健康网络下每 1000ms 计划一次、同时只允许一个 HTTP 在途。分别导出计划槽数、实际 HTTP 开始数、错过槽数、退避/离线/挂起槽数和原因；回到前台的发现时间不能代替后台工作时间。以游戏正式期号从相邻确认开奖及权威期历构造应有目标，跨日 1440→0001；丢失整段进程的期间也保留分母。待开奖的右端目标单列，已开奖的漏期留在原窗口。每期按角色记录冻结、已开奖后结算、缺失及原因，正常授权且数据源健康的设备段目标按每期影子验收。

记录权威发布时间、首次真实收到、开奖事务成功返回、下一目标冻结事务成功返回、本期结算事务成功返回及目标开奖边界。源发布时间可信时逐期检验开奖至落库与到结算各 ≤1000ms；否则该源端指标为 NOT_MEASURABLE，另列同 boot 的“收包→落库”“落库→结算”。下一期正式预测按本期可信开奖→下一期赛前冻结返回 ≤5000ms，并且严格早于目标开奖；另列落库→冻结的应用内时延及距离开奖的余量。未知时间不能按 0，不能以窗口均值/P95 遮掉逐期超时，不把事后离线重演算入发布表现。报告各指标给 n/N、未测 n、超标期号、P50/P95/max 与原因。以同 boot/可信时钟做差；回拨或墙钟偏差超过已申明的校时误差时保留 CLOCK_UNCERTAIN。同一安装的连续 72h 试验事前固定标准、设备与权限，并在起点后 24h 固定累计截面；持续时间内不打开页面补救，结束后只读导出。以 B05-03-T01 的 999/1000/1001ms、4999/5000/5001ms、相邻 boot、源未披露发布时间、整段服务缺失和最后一期未开奖覆盖边界。真机证据另附 B05-03-T02。
