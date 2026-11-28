# 211 总工单终轮覆盖与职责复核

## 结论

**当前集合完整、原发现未丢失，也没有把施工计划冒充当前软件已修好；但有 5 组生产职责/证据合同需修正，另有 2 处继承与阶段边界需补清后再签方案自检。** 这是对生成方案的定向复核，不是新的应用源码修改或真机验收。

本轮读取了 `ISSUES.json`、`BATCHES.json`、`coverage/METRIC_RESPONSIBILITY.json`、`coverage/CHECK_RESPONSIBILITY.json`、`inheritance/173_ISSUE_CROSSWALK.json`，并核两批说明及精确 211 源文件。审查文件 SHA-256 与结构检查结果在 `final_coverage_review.json`。下列数组位置对应本次 46 问题/12 批/115 源锚点版本，重生成后请优先按 ID 定位。

## 已通过的集合、保全和真值检查

- 46 项总问题恰好保留 50 个原始 finding ID；本审计 CV01–CV13 每项恰好归并一次。
- 13 份 `original_audit_records` 与原 `coverage_findings.json` 逐对象完全相同；每项原 repair、verification、must_preserve 均进入对应总问题，未发现合并丢条目。
- 44 指标与 96 检查 ID 完整、唯一，无指向不存在问题的引用；96 原检查状态仍为 56 EXECUTED / 40 READ_ERROR。
- 46 项 `execution_status` 均为 `PLAN_DEFINED_PRODUCT_ACCEPTANCE_PENDING`，没有无依据 PRODUCT_PASS；当前零模型/零图产物没有被改成通过。
- 12 批依赖图有合法拓扑顺序，没有结构循环。
- 44 个 173 旧问题均保留。035/036/037 正确标为源修复已存在并继续核运行，没有再要求覆盖旧全文件。
- R06 的独立 producer、R11/R14/R18 正式只读、20 canonical 矩阵与旧 R952 50/48 分账、旧 8 图不能由表计数代替、538 中517输入失效的约束都已保留。

### CV 原项归并位置

|原项|总问题|原件/施工/验证/保全|
|---|---|---|
|CV01|R344-005|全部保留|
|CV02|R344-045|全部保留|
|CV03|R344-035|全部保留|
|CV04|R344-036|全部保留|
|CV05|R344-037|全部保留|
|CV06|R344-038|全部保留|
|CV07|R344-039|全部保留|
|CV08|R344-040|全部保留|
|CV09|R344-041|全部保留|
|CV10|R344-044|全部保留|
|CV11|R344-002|全部保留|
|CV12|R344-039|全部保留|
|CV13|R344-042|全部保留|

## 必修事项

### FCR01 · Q07 被分给创造/任务批次，遗漏真正资格与证书责任（P1）

**位置：** `coverage/METRIC_RESPONSIBILITY.json#/42`（`metric_id=Q07`）。

当前 `issues=[R344-032,R344-017]`、`production_batches=[B03,B07]`，但其 `receipt_required` 是 `ACTIVE 证书资产数`。B03 任务推进与 B07 结构创造只能生产前置，不能生成自然有效资格证据。

原报告 `R30_REPORT.json#/metricResults/42` 明确 Q07 为“同时满足 Q01/03/04/05 的独立资产数”，并明确 ACTIVE 数本身只有 RECORDED_UNVALIDATED 范围。应加 `R344-043/R344-044` 和 B09 为资格生产/最终核验责任；保留 B03/B07 作为前置。精确 owner 至少包含 `LiveShadowsR27.certify`、`CertificateGateR27.evaluate` 及逐资产对应 Q01/Q03/Q04/Q05 原件。

由此派生的 RSEC-003、013、014、308 业务职责也须更新。反例：只有 BORN/合法 AST、只有 ACTIVE 一行但不满足自然证据、或者父证移植时，Q07 不得计为有效产出。

### FCR02 · M18 和 M32 的收据要求回退到旧文字，违背当前已明确的缺证规则（P1）

**位置：** `coverage/METRIC_RESPONSIBILITY.json#/17/receipt_required`、`#/31/receipt_required`。

|指标|当前生成要求|为什么不足|应写的实际验收原件|
|---|---|---|---|
|M18|`r30_pit_violation 非注入行数`|原 `definition` 是“改当前/未来标签后既有 predraw 输出差异”；当前 `missingReason` 明说 PIT 拒绝记录不等于泄漏反证|固定全部过去可见输入、扰动当前和未来标签，保存同模型/同水位输入及既有预测输出差分；注入测试来源与自然产品记录分列|
|M32|`同快照两章同指标冲突数`|当前 `missingReason` 明说没有同角色、同水位独立源对照，不能重复渲染同一对象填0|独立原 SQL / 原回执与 JSON、HTML、UI 同 metric、source、role、watermark 对照，包含刻意错源/错值反例|

旧字段不要删，可改名为 `original_measurement_source` 保全。新增真正可施工的 `current_acceptance_receipt`，并带原缺证规则。不能因生成器复制旧定义就让返修再次回到已被报告拒绝的验收方式。

### FCR03 · minimum_sample 只有数字，丢失登记的单位、角色和充分性限制（P1）

**位置：** `coverage/METRIC_RESPONSIBILITY.json#/40`（Q05）、`#/42`（Q07）、`#/43`（Q08）；建议全44项统一携带原定义中的适用字段。

- Q05 的 `200` 是校准报告支持量，原 `minimumSampleScope` 明说不改逐资产 V1 至少 250 ACT 及 ECE≤0.04 的资格条件。当前 JSON 没有这段，不能让施工把 200 当资格门槛。
- Q07 的 `1` 是至少一份被检查的资产资格原件，不是一条 ACTIVE 行即达标，仍需逐资产 Q01/Q03/Q04/Q05。
- Q08 的 `120` 是六臂 × 20 seeds 原 trial 完整性，不是 120 个自然期；还需完整当前 source 原件、每臂种子、误授证/功效区间及原登记合同，且 `naturalQualityEligible=false`。
- 工程指标的 `minimum_sample=1` 也不是完整 24h/72h、覆盖率或 P95 验收的替代。

从 `FULL_REPORT_COVERAGE.json#/metrics/*/r31Definition` 继承 `target/role/window/sampleUnit/denominator/minimumSampleScope/requiredEvidence/naturalQualityEligible/missingPolicy`；按实际采集接口引用当前可执行测试，不改原门槛。

另：M08/M09 的 `B11`、M13 的 `B10` 来自旧指标合同，而本轮 B11 是交付、B10 是汇总。保留原文字时需写明旧命名空间，避免误指当前批次触发分库或把设备测试推迟到汇总。

### FCR04 · 部分指标有读取文件，却缺实际生产者及其分层验收（P1）

**位置：** `ISSUES.json#/issues/35`（R344-036）；指标 M20 `#/19`、M24 `#/23`、M26 `#/25`、Q05 `#/40`、Q06 `#/41`；`SOURCE_ANCHORS.json`。

|范围|当前缺口|可直接绑定的211真实方法/owner|建议责任|
|---|---|---|---|
|M24 / R14 / AtomNet十位置|总36工单标题有 publication，但文件集合只见研究观测/消费与现代rolling，没有独立AtomNet实际训练→模型发布→概率面/调用链|`AtomNetRuntimeR30.step/forecast`、`AtomNetPublicationR31.prepare`、`AtomNetStoreR30.publicationModelR31`、`ModelSurfaceStoreR30.surface/calls`；只读统计 `ModelConsumptionCensusR30R1.report`|在现R344-036补独立producer子任务：当前phase/pin/样本→optimizer→非null model_hash→surface→matched call，不以现代rolling替AtomNet|
|M20|只派 B05 旧原件与ABI恢复，遗漏本计划实际重点改动的 CP9 和 rolling续训|`DevelopmentTrainerCP9.encode/decode`、`RollingLearnerR343` checkpoint/cursor、原 `ModelArtifactR29`|加现R344-031/034对应B06/B07恢复等价回执，保持各ABI独立|
|Q05/M26|列的是证书门/合成工厂，但receipt指实际校准评估|`CalibrationRowsR27.fit/fitRolling`、`CalibrationR30.evaluate`，以及冻结校准绑定/角色记录|B09明确真实校准输入/截止/模型hash的生产与独立验收，不用合成工厂替自然校准|
|Q06|只列创造、typed规则和合成工厂；自然父子同机会claim只出现在B08显示工单|`ImprovementStoreR31` 原自然注册/机会/成本/claim、`VerifiedImprovementR31.record`|B09增加自然pair原件职责；B08只读投影不能签自然提升；开发配对和CONFIRM与自然改善分账|

AtomNet 当前原报告还有可核的具体事实：`lifecycleEvidenceR31.atomNetPublicationCapacityR33.value` 中 PUBLISHED 19、publishedModelRows 0，active remaining 4084；`r34_atomnet_optimizer_step=0`、`r30_model_artifact/surface/call=0`。不能武断说容量已满，也不能把19 publication当19已训练模型。这不是要求凭现表0臆造新根因，而是补齐上游修复后必须实际运行验收的生产职责。

**不要求为了列表完整而改所有源文件。** 建议分 `producer_files`、`readback_files`、`candidate_edit_files`：只读/保全验收锚点可加入前两类，真正修改仍受具体已证实问题限制。

### FCR05 · 96 检查映射把“导出可读”与“业务生产”混为一组（P1）

**位置：** `coverage/CHECK_RESPONSIBILITY.json#/3`（RSEC-004）、`#/24`（RSEC-026）、`#/78`（RSEC-307）、`#/80`（RSEC-309）。

|检查|当前映射|精确范围/缺失责任|
|---|---|---|
|RSEC-004 逐段执行收据|只有 M20 → B05 旧检查点|`ResearchStageProgressR973.writeSnapshotCP4` 实际读训练、outbox、队列、子代谱系、冻结、校准、证书、原REAL；需另关联B06/B07/B09及R344-023的业务原件生产|
|RSEC-026 卡顿与闪退|只有 M33 图表|原evidence含frame jank和crashSummary，除图表回归还应有M14 / R344-021当前版本稳定性归属|
|RSEC-307/309 深度学习/创造/主链贡献|只有 M17及B01导出问题|READ_ERROR归B01正确，但`RuntimeTruthSnapshotR906.researchDetail`读取真实业务链；恢复可读后，零训练/无贡献由B06/B07/B09原producer继续负责，不能导出恢复即关全部问题|

不用改原96注册表或原 `CheckMetricCrosswalkR31` 来凑新的映射。保全它作为 `original_metric_crosswalk`，在施工映射新增 `read_path_issues` 与 `producer_issues/verification_owner`，避免超时影响标记充当全部业务职责。

RSEC-044、304、311 已核确为 `COMPOSITION_INDEX` / `NOT_APPLICABLE`，当前没有metrics而归汇总合理；它们可引用子检查，不能为填表强行新增生产义务。

## 继承与阶段边界的两处收紧

### FCR06 · 173旧038读层已经修正，应标保留而非笼统继续（P2）

**位置：** `inheritance/173_ISSUE_CROSSWALK.json#/37`，旧ID `R343-038`。

当前是 `CONTINUE_REAL_CONSUMPTION`，虽没有明令重写，但遗漏了已经进入211的改进：`ReportObjectIndexR30.java:29` 现明确将 `actualSourceConsumptionReceipts=bornExact`、`actualSourceNonBornActionReceipts=nonBornExact`；`ReportResearchConsumptionR343` 有独立 BORN_SQL/ACTION_SQL、原proposal/child/role/hash验证、有界扫描和UNKNOWN语义。

建议处置改为“SOURCE_FIX_PRESENT_RUNTIME_CONSUMPTION_PENDING”，原因明确分为两层：读层计数修复保留；真实proposal/consume仍0由R344-037继续修。与035/036/037一样，不能重新覆盖旧实现。

### FCR07 · B08/B09无结构循环，但工程门与自然后验应显式分开（P2）

**位置：** `BATCHES.json#/batches/8`、`#/batches/9` 与 R344-036 acceptance。

现依赖是 B09→B08，**未发现JSON结构循环**。批说明也已经写“外部原件不足只阻止对应验收，不可停止全部工程”，且R11/R14/R18已标FORMAL_READ_ONLY，这些做法正确。应把这层语义直接写进机器可执行退出条件，避免“47图消费回归”被误读成先获得自然REAL才能让B09开始。

建议明确：

1. **B08工程退出**：原有图保全、各图真实来源/缺失分类、隔离工程fixture成功与拒绝路径、合法研究动作消费、hash/分页/取消验证通过。不存在自然原件时显示真实EMPTY/WAITING；R11/R14/R18消费N/A。
2. **B09自然生产/资格**：沿原owner生成或继续收集实际CONFIRM/FUTURE/证书/REAL；缺资格保持不出手。
3. **B09后回填B08业务验收**：用真正新原件验证R11/R14/R18显示和归因，不重算HOLDOUT、不回流训练，不把fixture写进自然分母。

每对象CPU是现声明UNKNOWN的功能补齐，不能为了B08退出强造CPU；它也不能成为修复原真实主链的未声明全局前置。

B08 purpose 中“旧8类可读数据及有效图表”建议改成“16逻辑表现8表可读与用户既有有效图表分别保留”；这不是新增事实错误，但会消除“8表=8图”的歧义。

## 虚假当前成果与闭环判断

本次没有发现将当前40 READ_ERROR改为通过、把81/82说成81项成功训练、把旧R952矩阵填成20canonical产物、把19publication说成19模型、把纯Java补丁测试当Android真机通过的表述。R18候选实际12用例及其验证范围也得到保留。

以上必修事项是**职责与验收合同的缺口**。修正不需要另起整套软件、改指标门槛或删除已有源文件。生成器更新后需重跑集合/引用校验，并逐条确认上述指针已反映新的生产owner和证据要求；不能仅增加一句“见审计”而保持机器可执行职责仍旧不完整。
