# R34.3 图表/消费/检测证据精确补丁

基线：`4d1de9d83692635b5c5ce39144fece1c8a31c8ac`。`after/` 是完整替换/新增文件，`charts_evidence.diff` 为同一内容的 unified diff，`manifest.json` 给每文件前后 SHA-256。先校验基线文件 hash 与 `git apply --check`，再应用；不把其他版本的文件整段覆盖到此基线。

## 修改范围

1. `ReportSnapshotR30.TABLES` 登记 `r33_research_input_head`、`r33_research_applied`，使真正元数据 reader 能区分“缺表”“存在0行”。
2. `ReportInlineBudgetR343` 给图表身份索引保留16 KiB、研究帧索引32 KiB、当前head32 KiB、非出生动作32 KiB；普通payload最多656 KiB，总分配仍768 KiB。
3. 当前源已提交帧计数使用 `r30r1_chart_cache JOIN r30r1_analysis_run`，限定 `view_id/current source/COMMITTED`。R06独立研究任务没有input-head，R11/R14/R18头状态为 `FORMAL_READ_ONLY`，均不能再被旧的 `head.state=PRODUCED` 条件漏掉。提交帧行数不是有限数值、质量或绘图通过证明。
4. `ReportResearchConsumptionR343` 在已有 lifecycle 只读范围内读出生与非出生原件，输出分开的完整计数或已验证下界。BORN原始 `measure -> run -> consume -> proposal -> actual policy` join不省略，复验哈希、源/数据集/修订/ABI/角色、实际child、parent、目标、ordinal与已存在typed合同的consumeKey。非BORN复用既有 `ResearchActionPolicyR33` 身份/时间合同。没有任何消费计数自动变成品质、独立未来优势或REAL权限。
5. 状态修正：当前0保持false；没有运行回执则ran保持UNKNOWN；历史hash仅保留历史元数据；未知消费不标成未消费；正式只读与旧纯展示图优化消费为N/A。

`r33_research_applied` 的旧 `researchConsumed:<view>` raw fact 为兼容保留，但只在对象中显示成 `currentSourceNonBornActionRows`，明确不是出生计数。`actualSourceConsumptionReceipts` 改为经原件join验证的BORN图源消费；`actualSourceNonBornActionReceipts` 分列。typed-generation-rule 的BORN消费没有R01–R18 source-view合同，作为 `unvalidatedTypedRuleRowsOutsideChartViews` 原始计数单列，交该owner独立验证，不随意归到某张图。

## 编码预算的单位

SQL把每个原值编码为 `T:`/`I:`/`R:` 等ASCII标签、ASCII十六进制、ASCII分隔符。原值即使含中文，编码结果仍是ASCII，因此当前查询中的 `length(encoded)` 与该编码串的UTF-8字节数相等。768 KiB预算是**SQL编码后rowset载荷字节数**，不是JSON原始字符数、原数据库大小或整个报告ZIP大小。

本补丁还把 `CUT_V1:\n` 的8字节计入该rowset预算。分配小于8字节时返回空编码串，并依据 `Cell.inlineEncodedByteBudget` 与真实SQL total报告预算截断；不会为了写截断标记再越出总预算。单cell的 `maxCell` 仍按 `length(CAST(value AS BLOB))` 判断原UTF-8字节，超过则保留引用。完整原件遵循原报告的独立附件机制。

消费验证器的上限另以**字符数**标注：每类最多512行、每字段262144字符、每类累计2 Mi字符。它不宣称这些是字节上限。SQL原始view-key聚合先将每key限制到96字符并保留oversize标记，DISTINCT最多65项；超过64项或出现oversize都使计数PARTIAL，避免先把无界group_concat装进CursorWindow。投影输出本身经现有lifecycle的64 KiB UTF-8预算。

## 已执行检查

- `ReportInlineBudgetR343` 实际Java类独立编译/运行，128种调用交错，总分配≤768 KiB，四个索引预留不被普通payload抢占，负预算拒绝。
- `ReportResearchConsumptionR343` 实际新类及原 `JsonR27`、`StatsR27`、`ResearchActionPolicyR33`、`ResearchResultContractR31` 编译通过。平台SQLite接口、当前ABI提供器、PolicySpec等使用明确标记的编译fixture；这不是完整Android编译。
- 实际非BORN验证器15项合同/负例/partial-zero检查通过；fraction/NaN/infinity/long overflow不得被 `longValue()` 截断接受。
- 从上述实际Java类导出原SQL，在主机SQLite验证：空表/真实BORN外键join、BORN/nonBORN不混计、R06及formal3无head仍按当前COMMITTED frame计数、旧source不计。30万字符view-key和超过64个key均在进入Cursor前有界并明确partial。
- SQLite中文输入/ASCII hex/CUT envelope预算边界已验证，实际编码UTF-8字节不超过给定预算。
- `git apply --check` 对安装源码基线执行，结果见 `verification/verification.json`。

复跑可用 `python verification/run_host_receipts.py`。脚本默认相对本审计目录找到 `source-installed`，若放入正式仓库后应由施工脚本显式传入相同基线的源码路径。脚本的 `syntax_stubs` 仅用于主机验证，不能复制进app，也不能包装成真机验收。

## 必须继续做的真机与集成验收

`R343ChartEvidenceTest`、`R343ResearchConsumptionTest` 已提供完整JUnit测试源码；本环境无Android SDK，完整app编译、真实PolicySpec BORN测试、Android SQLite与全部47图渲染均未执行。

真机验收必须先解决启动/DDL/writer与不可变研究输入、进化学生、现代在线owner的问题。之后每张图按 `chart_source_mapping_63.json` 核对 producer、实际表、筛选口径、当前/历史source、原件hash、finite point count与页面渲染。报告原件明确没有数值时显示原因，不能用模拟数据或累计活动计数伪装收益。R11/R14/R18不走优化反馈，真实出手仍由原有质量/资格/主链合同决定。

原报告中的 `SOURCE_UNAVAILABLE` 三表需要原迁移owner恢复，本补丁不在reader创建空表。`coveredSettled=0` 另由 `coverage_patch` 修复双hash域误比较（CH-12 / RUN-019）。冻结writer TTL范围由 `runtime_patch` 解决（CH-09）。
