# Runtime / charts 独立复核结论

基线：`4d1de9d83692635b5c5ce39144fece1c8a31c8ac`，已安装 `577311173`。复核对象为本轮原始 DIAGNOSTIC 报告、源码，以及 R34.3 封印方案的 runtime / prepared_storage / coverage / charts / delivery 补丁；新增必修项为 R343-002 / B01 的语义构建身份补足，候选位于相邻 `semantic_support/`。

## 必修遗漏

新增 12 个主 Java 叶子缺少原 R967 闭集分类和当前 R82 精确差异 proof 的施工工具。原门在 B02 就会拒绝构建，不能由四文件版本升级自动解决。见 `semantic_support/REVIEW_AND_TESTS.md`、逐路径 `ROLE_REVIEW.json` 和两个可直接应用的完整工具源文件。此修法保留原 closed manifest、原验证器、原两份不可变 R81、旧 SU1 reviewed_inputs 与统计/REAL 门。

## 对既有 runtime/storage 修法的复核

- Startup TEMP 空间：新 `StartupTempSchemaR343` 只接受四个属于原启动链的精确建表定义。原 checkpoint 判定遗漏合法 `r31q_ddl_cache_flush` 导致启动关门，补丁补齐该定义并将持久表读取限定为 `main`。未知 TEMP 对象、同名异构与影子对象仍失败；没有全名通配放行。
- 持续启动：`AutoFetchService` 保持原所有者，新增持久半开探测；`StartupStageJournalR341` 按 source/dependency 限制三次探测、五分钟冷却，文件锁中先持久预约再执行，旋转保留必要的最近成功/失败与探测上下文。避免一个旧失败状态永久阻止后续启动；不证明现场队列已经排空。
- 主写故障与耗时：区分 CONTROL tick 和真正 main-writer attempt，补充 phase/timing；把不可识别的 `RESOURCE_WAIT` 改为原可识别 `RESOURCE_QUEUE`。原 outbox、receipt、readback 和独占 writer 路径保留。当前报告 P95 的少量样本不足以把其全部耗时归因到某一阶段，方案先补测量是必要的。
- 主写快照：`WriterFaultSnapshotViewR343` 保留已封存哈希行，但生产没有独立单调时间 pin 时必须标为 UNKNOWN；不把当前导出时间或历史 digest 当成实时健康证明。
- canonical 存储：新增的 admission 计算包含原子文件替换可能同时占用的两个镜像及目录空间，并按既有预算粒度计入；最终原 StorageBudget 仍测量现实容量，不能通过估算免除检查。旧缓存身份与来源关系保留，增加 save phase 观测。
- coverage：`CoverageSettlementR343` 修正只读诊断时关联结算证据的哈希域，不能回写补造已有 freeze/terminal。报告中 8/30 有 predraw terminal、其余 22 缺失仍作为缺失；不是只改显示即可宣告链路完成。

## 对既有 charts/report 修法的复核

- 原源表中八个非空表不等同于已证明 UI 只有八个成功渲染面板。方案现在逐层区分 producer、input、frame、consumer、apply、render、export 的证据，未把所有空图归为一个数据库故障。
- `ReportSnapshotR30` 纳入真实 input head / applied 来源；`ReportInlineBudgetR343` 为图表对象索引、repair frame、head/action 分别预留 16/32/32/32 KiB，并将普通部分限制在总 768 KiB 的预算内，防止关键索引被前面普通表占满。
- R06 与 R11/R14/R18 的正式只读数据来源由已提交、当前 source 的研究 frame JOIN analysis_run 证明；没有把完整研究消费资格凭空授予只读看板。
- `ReportResearchConsumptionR343` 已与原 producer 的 receipt 字段对读。BORN 使用自己的 measure/run/consume/proposal/policy 关系；其他研究路径使用真实 `r33_research_applied` 关系，两者不可混记。最多 512 行、单 cell 262144 字符、每 kind 总 2 Mi 字符，超限/缺证据的零值保持 UNKNOWN；经过验证的正数只表示可证实下界。
- 当前 frame、历史完整性、真实 consumed/applied 与不可适用的 consumer 分开呈现。没有新增第二个主链 owner、第二套结算 ledger 或通过图表显示授予 REAL 资格。

## 证据界限

以上是源码与候选代码合同的复核，加上原 R967 主机执行验证。没有原 SQLite 全库、APK 原体和 Android 实机环境，就不能据此声称数据库原样回放、APK 构建安装、前后台恢复、24/72 小时持续跑数、实际 P95 改善、全部面板真实渲染、自然策略质量和 REAL 主链出手已完成。本次 DIAGNOSTIC 中相应缺证据必须继续作为未通过/待验，不能因为新增字段或主机 PASS 被关闭。
