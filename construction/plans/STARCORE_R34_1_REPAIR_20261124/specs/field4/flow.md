# 关闭界面后，服务和研发是否持续推进

适用源码 `5562f9b19aa09f9f7d9e69ca085db56c537967fc`、安装版 577311122 和第 2 次报告 `4bef989d…`。这份报告的后台能力观察明确标注 24/72 小时均待测。以下前两项分别是可复现的源码故障分支和已观察到的统计语义差异。当前证据不足以把 95 个赛前缺口全部归给任何一项。

## 首次服务启动投递失败会堵住后续启动

`AutoFetchService.startWork` 从主线程投递到 runtimeGuardHandler 或 startupInitHandler 时，先置 `startWorkDispatchQueuedR3610=true`。122 源码只有已投递的 Runnable 会清除此标记，`owner.post(...)` 返回 false 时也直接返回。这样的服务实例即使继续收到 START 也可卡在“已经排队”，而首次实际启动、抓取和研发循环从未开始。运行中的 fetch/watchdog 重排故障与此分支分别验收。

B05 的队列令牌按 service 代际和唯一投递号持有。记录待投、Android 受理、实际进入、取消/拒绝四种状态。在投递失败、回调被移除及 Looper 正在退出时，只允许对应令牌释放待投并在既有可运行控制入口有限重排；成功投递但未开始时，以服务状态和可取消令牌对账。停止、旧回调迟到或停止后再启不得清掉新代令牌。用不创建 Activity 的真实服务入口先令 post 返回 false，再恢复队列并发送重复 START；从服务端回读一次实际启动、合法抓取/冻结/结算。将“受理后取消”“旧回调晚于新代”也注入同一用例，记录首次中断处与重新唤醒者。首批用既有真实基础影子证明服务启动，不要求尚待后批施工的创造模块先通过。

## 22 路维护工作的一次回顾不能刷新数值学习水位

`DIAGNOSTIC_SCOPE.json.researchSchedulerState` 的 `r912_research_continuity_status` 与 `r908_research_tick_status` 同时记录：`PERIOD_PENDING|REVIEW_ONLY|watermark=202611240604|taskWatermark=202611240601|reviewedLanes=2/22|lane=11|outcome=WAIT_FORMAL_FUTURE|numericEvidence=OWNER_RECEIPTS_ONLY`。同份 heartbeat 保留 `learningProgress=R959_PERIOD_PROGRESS`、`PROGRESS_FRESH` 和 `RESEARCH_UNKNOWN:HEALTH_PROBE_NOT_YET_SAMPLED`。采集时点有差异，不能断定这一行恰好触发了那次心跳。

`ResearchLearningContinuityR912.pulseWithinR927` 把完成本路回顾的 `terminalReviewOutcome` 作为 Result.success 返回。该成功可以只代表完成维护任务。异步服务 `AutoFetchService.postAutonomousResearchTickR908` 收到 success 后直接调用 `ServiceHeartbeatStore.markLearningPump("R959_PERIOD_PROGRESS", ...)`。因此各层要保留独立的启动代际、时间、最近输入与实际数值产物。兼容旧心跳但把这一路显示为维护复查进度；只有出生、实训提交、研究产物或应用等**对应 owner 自己**提交的唯一原件可以更新该阶段的水位。维护 TRUE、探针 UNKNOWN、实时正常的组合不得让零出生研发显示健康。旧版仅有成功字符串的历史保持计数和来源，升级后不能补造模型更新。

启动 B01 的工程场景中主动收齐 22 路维护回顾、保持实训提交为零；这时维护能推进，学习状态显示等待/未证实。接着回放同一已提交产物两次，再注入后续不同且有真实可比输入的产物，只有最后才向前推进一次。记录维护任务的创建期、实际服务期和数值 owner 所用训练到期。当前 R26 owner 已占用共享表时保留旧 R908 让行，恢复只唤醒权威 owner；禁止为了出现学习数字开启第二个写者。本批验证水位语义，不依赖后续批次产出盈利资产。

## 72 小时无人干预需重建过程和期末结果

第 3 版 `tools/verify_receipt.py:campaign` 会核对时长、日历、24 小时截面和摘要，但 boot 和人工介入两个原件只校验已声明。原单测的正例在一个 72 小时窗口中只有 4 个目标，boot 与 interventions 仅有 kind 字段，也能通过该辅助函数。新增 `tools/background4.py:check_background4` 在旧校验之后执行。B05 的 DEVICE 验收和 B12 的 DEVICE 复验共用已登记的同一场景与合同。工程用例 `A-B12-04b` 负责验证采集器和错证据拒绝，样例不取得设备资格。

1. 在现有 campaign 主索引新增 `observer_artifact`。日历、期末账、启动区间、干预、原始业务事件、评价器和观察流均沿用回执的设备、APK、版本、源码和窗口身份。日历增加事前的 `registered_at/clock_error_ms` 和邻近的 `before_window/after_window`，均用 12 位正式期键、时间与每个 UTC 日期 0001–1440 的序号检查。按 TargetIssueResolver 的源时钟和每期 60000ms 推得事前期历，不从 UTC 零点反推具体开奖时刻；网络晚到仍保留该期机会。夹在两个快照之间的漏期和跨日报告均需纳入。每期可共用批量导出的只读原件，不要求在实时库内增加 72 小时的文件队列。
2. 由独立的只读设备观察器记录 `records[{event_id,kind,observed_at,boot_id,elapsed_ms}]` 和观测完整性 `complete`。boot 段的 `segments` 引用同 boot 的 `start_event_id/end_event_id` 并列出校时误差。墙钟和 elapsed 比较仅发生在同 boot 的已校准段。墙钟不可靠时声明不确定区间，保留缺口并继续实测。投影的 `observer_event_ids` 须包含窗内全部干预，启动段、干预和评价输出绑定 `observer_sha256`。正常系统后台恢复/有观测覆盖的重启与需要用户重新打开页面分开。强行停止后的恢复可在独立负例验证，不能并入无人干预通过的窗口。观察器未知、中断无记录或出现人工启动，均可提交失败/待观察回执。
3. 期末账从截面结束前最后业务事件复算；窗内事件带 `boot_id/elapsed_ms/sequence/service_commit/producer`。将同一事件的观测时间、角色、终态和原因、生产者、boot 与期末逐行核对。没有事件的期保留 `NOT_OBSERVED_AS_OF_CUTOFF` 并用真实窗末时刻。标为服务提交时，需交同一期、角色、时点和设备身份的业务提交原件。观察结束后回补的预测或结算均保留其实际时点，不允许回填上一个截面。
4. 评价输出增加 `per_target_shadow` 及 `service_commit_targets/raw_events_sha256`。逐目标提供独立观测的输入条件，健康机会须回读当前预测影子的服务赛前冻结，以及已开奖时同一冻结的权威结算。逐期用本期的预登记机会起点校验 `opportunity_at ≤ freeze.returned_at < deadline_at`，跨到上期的冻结不得计入。成熟期的 `draw_artifact` 取本机收到的权威开奖原件，以 `target_key/provider_id/draw_id`、同次设备和 APK 标识定位，`origin=AUTHORITATIVE_DRAW_INGRESS`、`state=DRAW_RECEIVED`。结算复用同一个 `draw_id/draw_at`，顺序为预定开奖边界 ≤ 权威实际开奖 ≤ 设备收到 ≤ 结算事务返回 < 窗末。提前开奖、错期、源时间不可核或先结算后收到均需保留异常，不能签为健康期成功。右端未开奖须由相同权威采集入口交付 `state=NOT_YET_OBSERVED, as_of=window_end` 的当时读数。网络/数据源/系统限制要有同一期及时间范围的外部输入证据，不能靠服务沉默自判。即便其余角色有数据，缺少主预测影子时仍不能填写该目标的已覆盖。参考工具核对连续性和业务原件；开奖与落库的 1 秒、出号的 5 秒和其余原 SLO 继续由 B04/B05 的实测时钟与源评价器独立核对。
5. 详见参考工具及其正反用例。业务原件按 `<field>_artifact` 关联；若为批量导出的 JSON，再用 `<field>_record_index` 取对应行。已有存储/采集接口优先。工程签收检查原始提取器可回读、投影完整、重启可恢复；现场报告无此观测链时标明缺证。校验器无法证明外部设备日志产生的真实性，仍须留存只读采集器、原始设备日志、生产 SQLite/commit 回执与同机安装身份，供复验。

B05-03 在本批工作树建立 `tools/r341/device_campaign.py` 及 `release/r341_device_campaign.schema.json`，使用本批安装包完成所需观察。B12 在原路径扩展、用最终包重新观察；首次签收可独立安排。
