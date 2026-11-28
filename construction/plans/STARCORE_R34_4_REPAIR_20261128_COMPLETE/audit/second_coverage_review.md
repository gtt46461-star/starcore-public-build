# 第二轮覆盖与验收语义复核

审阅对象：已发布完整方案 `7947177eb26a5508e8c34c1159cb179ccbe15a89`；应用底座 `5c7f1b36eaee6036df280e85835bb0fb4ce0b40d`。本轮只读复核施工与验收语义，不重做已完成的包字节、相对链接或数量校验，不运行产品、Android、原库或自然质量测试，不改 publication、应用或远端。

## 结论

确认 **1 个需要补充的生产责任/验收合同缺口**：20 个矩阵对象虽然都在清单中，但当前 R344-038、M25 与 B08 没有将其拆成真正的 **18 普通 R930 typed + 1 配对 COUNTERFACTUAL + 1 SU2 checkpoint** 三条生产链。该缺口应合入既有 038 卡，不扩大成新的根因工单，也不重建有效矩阵实现。

原 50 发现归并为 46 卡未发现施工、保全或验收语义丢失。B08 与 B09 当前分层合同没有循环依赖。1832 需求、299 旧卡、826 表明确只是保留/清点与待验入口，没有被当作已完成产品成果。

建议的机器可读增补在 `audit/second_coverage_amendments.json`，审阅结果在 `audit/second_coverage_review.json`。本报告外另保存了 13 个按精确 211 commit 读取并与原 1407 基线 SHA/size 匹配的必要源文件及 SOURCE_RECEIPT；这不等于重新校验了完整源树。

## CV2-01：20 矩阵的生产、消费和验收分路遗漏

### 当前计划的位置

- `ISSUES.json` 的 R344-038 / `repair[0]` 只说先恢复合法 dataset，再逐 20 类型生产；`source_files` 只有 MatrixResearchR30R1、MatrixRegistryR30、MatrixSnapshotFeedbackR930。
- `coverage/METRIC_RESPONSIBILITY.json` 的 M25 只列上述文件及长档案输入文件，证明字段仍是泛化的 SIGNED_HASHED_MEASUREMENT_RECEIPT。
- `batches/B08.md` 施工步骤 2 统一表述为 20 矩阵 ABI/consumer 接线。
- `coverage/FULL_REPORT_COVERAGE.json` 的 SU2 对象保留了原报告分类观察：`owner_observations` 指向 `r27-matrices-r1`。这是原报告内容，不能反过来当作 SU2 的源码生产者证明；原报告仍应保留。

### 精确 211 源码证明

|对象路由|生产与持久化|消费/读回|本轮须补的验收|
|---|---|---|---|
|18 个普通 R930 typed|MatrixResearchR30R1.step / produceR31 / build → MatrixTensorR30R1.append|各 kind 的 typedConsumers；原 matrixObjectIndex|逐 SOURCE_ONLY/STUDENT/EVIDENCE 的可运行与等待，不由统一 dataset 回执代替|
|R930:COUNTERFACTUAL|ResearchStudyTaskR30R1 的已完成 RETRAIN_REMOVED → ResearchStoreR30R1.paired → MatrixTensorR30R1.append|原 R06 / ready/consume 与实际测得 child|完整配对、真实 blocks、parent/child/role/revision 对齐；MASK_FROZEN 或未完成 replay 不能代替|
|SU2:LEARNED_FACTOR_TEACHER_CHECKPOINT|LegacyGenerationR26 的已完成 campaign/F15 committee → MatrixSnapshotSU2.capture → MatrixStoreSU2.prepareR31 → 原 ModelPublication 提交及 project → markAvailable|MatrixStoreSU2.maintain / MatrixSnapshotSU2.support；su2_matrix_snapshot 与原报告|原 genome/checkpoint、factors/state、提交后可见、同 genome 已冻结动作消费；缺历史保持缺证，不补造消费|

`MatrixRegistryR30.all` 明确只注册 19 个 R930 加 1 个 SU2。`MatrixResearchR30R1.java` 第 144–145 行进一步只选 namespace=R930 且排除 COUNTERFACTUAL，所以该任务实际覆盖 18 个，不是 20 个。`ResearchStudyTaskR30R1.java` 第 57–59 行仅将已完成的 RETRAIN_REMOVED 送入配对生产；`ResearchStoreR30R1.java` 第 80–92 行按真实 blocks 构建 COUNTERFACTUAL。`LegacyGenerationR26.java` 第 65–87、101 行和 `MatrixStoreSU2.java` 第 21–25、32–65 行证明 SU2 完全不同的 campaign/发布/消费条件。

若施工人员只恢复 r27-matrices-r1 的 dataset 并见到 18 个正常输出，就会漏验另两路；若为了凑满 20 将另两项也放入 MatrixResearch，会破坏已有独立合同。这个问题是当前计划责任表述不够精确，不是声称以上源码缺实现。

### 最小施工方案补充

保留 46 卡、20 个原 ID、原 native R930、SU2 和 R952；在 038/B08/M25 上补三路生产者、消费与读回合同，新增 5 个已核 211 源锚：MatrixTensorR30R1、LegacyGenerationR26、MatrixSnapshotSU2、MatrixStoreSU2、ModelStoreSU2，同时把已有 ResearchStudyTask、ResearchStore、ModelPublication 源锚关联到本卡。源锚用于定位和保全，不代表必须修改所有文件。

正反例至少包括：18 普通类型的不同输入门；已完成与未完成配对、RETRAIN_REMOVED 与 MASK_FROZEN、错 parent/child/source；SU2 campaign 未完成、缺 F15、重复发布、提交前故障、已冻结 action 与无 action、history 缺口及 source generation 改变。逐 ID 记录原 hash/role/source/revision/schema/watermark，不以计数 20、缓存存在或 retainedCount 签消费/有效。

新增合同继续遵守工程与现场分层：依赖未来合法 action 的消费可以 PENDING，工程正负例可先验。不得让 SU2、COUNTERFACTUAL 或正式图表的自然数据成为 B09 工程开工的全局前置。

## 已自检排除：不重开一个实际已存在的桥接

阅读 MatrixResearch 和 ReportObjectIndex 的中间步骤曾产生候选假设：typed 矩阵只写 research_measure，而索引只读 native 表，可能一直显示 0。继续读取完整 `MatrixTensorR30R1.append` 后，该假设被明确反证：第 50–53 行已在同一个 ResearchWriteQueue.Batch 写入 analysis_run、research_measure 与 research_matrix_snapshot_r930。ReportSnapshotR30 第 287–292 行和 ReportObjectIndexR30 第 31–35 行可以读原 native 桥接。

**该假设已撤回，不列为缺陷，不增加索引重写、表重建或补行任务。** 本次只把已有同事务桥接及三处 hash/role/revision 一致性作为保全与回归项。这个反证同时说明不能只看某一步 INSERT 或某个总数就认定生产/读取断链。

## 50 → 46 的语义继承

逐项对比每个 original_audit_records 的 repair/construction_actions、acceptance/verification/preserve_and_acceptance、preserve/must_preserve，再人工核全部不逐字相同项。未发现丢失。差异是已审定的三类澄清：

- R344-001 随授权更新当前发布措辞，仍要求 211 或经核后代独立施工，禁止用旧 main 应用树覆盖。
- R344-025 将 root closure 约束限定在自身旧模型的恢复/使用/退役；保留原件和 GC 保护，同时让不依赖坏 root 的新实验继续。
- R344-028 区分精确原件 ColdRebirth 与完全独立的新基础实验；没有删除旧 state/hash/ABI 检查，也不继承旧质量身份。

## 全量集合的实际覆盖含义

|集合|本次语义判断|
|---|---|
|96 检查|已有 read_path 与 producer 分离；三个 COMPOSITION_ONLY 不应虚造生产者。M17-only 报告/静态登记检查 producer 为空，不自动形成业务缺陷；仍逐检查验真实输出，不能整体签 PASS。|
|44 指标|原 role/window/target/样本支持含义保留。M25 存在本报告的具体分路缺口；泛 proof 标记须结合原定义与卡，不把每个泛标记人为扩大成一个新根因。|
|165 对象|18 图有逐 producer/input/consumer；29 旧图及 16 表有真实源与 039 回归；82 能力按静态/输入/运行/产物/消费/效果分层，矩阵 alias 不当额外生产者。20 矩阵需补三路合同。|
|826 表|清点原表/行数/状态/证据位置，不等于 826 个新功能。空表没有被自动判为源码缺陷；现有 B02 预算与 B10 下一动作合同保留。未找到额外可证漏修表。|
|1832 需求 / 299 卡|保留 RETAINED_CONTRACT_NOT_AUTO_ACCEPTED；002 与 B10 要求逐项真实 owner/原件或保持未检查，没有已完成声明。执行时仍须逐项核销，不能批量写完成。|

兄弟 strategy 审计 S2-ST02 发现的 AST3 现代冻结/校准/固定窗/证书路径与 R27 路径不同，会影响部分 metric/check 责任映射；交由同一发现统一合入，避免重复根因。本审计未声称独立重核了那条完整源码路径。runtime 审计提出的 B01/B02 fault-drain 工程门拆分同样应保留最终验收但不互相作开工前置。

## B08 / B09 与验收门

B08 明确 ENGINEERING_READY；R11/R14/R18 等自然原件缺失保持 PENDING_B09，不阻 B09 工程。B09 自然消费才要求 B04 PIT、唯一 owner、合法截止和精确结算的工程回归，不等 72 小时或全部旧原件恢复。当前范围内没有发现另一个不可执行硬门。矩阵补充不得改变这一点。

本轮结论为方案审阅。全部产品施工、Android、原库性能、现场运行与自然质量验收继续待真实执行。
