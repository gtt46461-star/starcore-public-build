# P1.03 增量：服务重启后从已提交开奖重建尾段恢复意图

本增量基于本轮已应用 P1—P5 的候选源码，修改 1 个生产文件、新增 1 个生产类；不修改 DbStore、迁移 owner、报告 owner 或算法资格门。完整后态源码位于 `full_source/`，精确改动位于 `P1_RUNTIME_INCREMENT.patch`，逐文件前后 SHA-256 位于 `FILES.json`。原仓库 app 工作树未改。

## 已确认的生产缺口

原 `AutoFetchService.reconcileResearchOwner` 每 5 秒控制 tick 只处理故障回执、迁移和研究恢复。尾段快速修复由 latest/history 响应触发；重启后未到达新的成功响应时，控制者没有独立恢复尾段请求的入口。因此数据库已保有最新开奖、但 12 期连续尾段不足时，已有 P1 历史页补丁仍可能没有被重新调度。

## 两个文件的实际修改

|文件|操作|代码作用|
|---|---|---|
|`AutoFetchService.java`|在 `reconcileResearchOwner` 的节流检查后、故障排空及迁移前调用新增 `reconcilePredrawTailRecoveryR32`|从实际数据库读最新 head 与最多 12 行轻量历史，用既有 `scheduleQuickHistorySyncR71` 请求同一 backfill lane；单独处理异常，避免尾段读失败使研究控制者退出|
|`PredrawTailRecoveryR32.java`|新增完整生产类|只以已提交 draws head 重建下一期目标，并用原 `R36AnalysisHistoryPolicyR364` 与 `TargetIssueResolver` 判定；无 durable head 也会请求历史；已有 12 连续期时停止请求|

耐久意图采用“从耐久业务状态重建”的方式：数据库已提交 head 是唯一目标来源，不另造可能与 head 不一致的待办目标表。每次 tick 重新计算最新目标；进程重启得到相同意图，head 前进自动合并到新目标。此处不使用非耐久遥测、旧回调参数或进程内缓存决定目标。backfill handler 未就绪、正在执行或处于冷却期时由原调度器合并，后续 tick 仍可重投。

本方法不执行网络 IO、不新增线程/写者、不直接冻结或结算、不补写过去的赛前预测。历史页仍经原 P1 `savePredrawSuffixR32` 的来源冲突检查、head 重读及至多 8 行分片，再由原 PIT/截止门决定能否正常冻结。

## 应用与本批验收

1. 在全量 P1—P5 候选后态执行 `python3 apply_runtime.py --repo <工程>`，全体源码 hash 预检通过后才执行同命令加 `--apply`。不同源码 hash 必须重新审查差分，禁止直接覆盖。
2. 用真实生产类编译新增判定器、`TailRecoveryProbe.java` 与完整 `AutoFetchService.java`；已记录 `HOST_PROBE.log` 的 9 项通过及 `AUTOFETCH_TYPECHECK.log` 的类型编译结果。
3. Android 本批集成例：在保留数据的测试安装中只保有 2 期连续尾段，阻止 latest 成功返回，重启服务；在正常历史接口可用时检查原 backfill lane 实际收到恢复请求并补齐 12 期。重复重启和重复 tick 后检查每期无重复业务写入。该例必须记录原机或模拟器实测，主机纯类测试不能代替。
4. 故障例：令历史不可用、迁移失败、handler 迟初始化及旧 head 与新 head 交错。不得创建第二个 backfill 线程，不得为过期目标生成决策；恢复可用后从最新已提交 head 继续。

## 真实完成范围

已完成并主机验证：从耐久开奖状态重建恢复意图、重启等价性、新目标合并、跨日、连续阈值、冲突和不修改输入；完整服务类与候选既有接口类型兼容。

未宣称完成：Android 线程实际调度、接口可用性、系统强制停止后的自动启动、锁屏长测、P95 时延、旧手机数据库集成结果；这些需本批装机记录。该增量也没有恢复已经永久遗失的 checkpoint 原件，没有证明 556 个历史缺件均可恢复，没有代替 P1.05 每期冻结/结算完整性和 P1.06 原件恢复/存储预算施工。已有诊断导出与 RAW 备份的严格缺件边界保持不变。不能用这 9 个测试签收全部 P1。
