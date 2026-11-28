# R34.4 总方案的策略与训练链独立复核

## 1. 结论与复核范围

本复核只判断方案是否能安全进入施工，以及所要求的修改与验收是否能落到 **211 的真实代码**。它不把方案完成、候选补丁可应用或主机分类测试当作 Android 应用已经修好，也不把历史训练完成当作自然未来质量通过。

应用底座固定为 `5c7f1b36eaee6036df280e85835bb0fb4ce0b40d`、versionCode `577311211`。核验树为 `/workspace/scratch/189d5130b5de/repo-installed`；本次未修改该树。计划树为 `/workspace/scratch/189d5130b5de/repo/construction/plans/STARCORE_R34_4_REPAIR_20261128`。

已逐项复核 `ISSUES.json` 中的 15 个 ST 问题及相关原件恢复问题、`BATCHES.json`、`contracts/CREATION_TRAINING_AND_QUALITY.md` 和 `contracts/PIPELINE_AND_PRESERVATION.md`。完整原始审计见 `strategy_pipeline.md`、`strategy_findings.json`；施工卡的补充见 **`final_strategy_supplemental.json`**，包含 16 卡。补充件不覆盖原审计记录，只为当前施工卡补文件、方法、修复动作和可执行验收，并纠正两处不够精确的原因表述。

**复核总评：以下 6 项方案必修已全部合入并独立核实关闭，当前版本可以进入受控增量施工。未发现新的阻断级方案矛盾。此结论只签收方案，当前产品的 Android、迁移、原件恢复、自然主链签收仍保持待验。**

## 2. 发现与关闭条件

|编号|原计划的具体风险|代码证据与实际含义|必修动作|最近复核状态|
|---|---|---|---|---|
|SR-01|B06 依赖 B05、旧 checkpoint 总闭包的措辞，可能把所有新训练锁在旧原件恢复后|现代基础工厂与 TRAIN 不需要旧 R27 pin 或不相关旧 parent checkpoint|B06 的工程前置仅 B00/B03；旧恢复只约束该任务自身原件。所有旧原件缺失时，合法新输入的独立基础实验仍能完成 DEV|CLOSED_PLAN_CHANGE_VERIFIED：B05/B06、025 及无旧原件正例已合入|
|SR-02|“缺原件沿 ColdRebirth 建独立子代”在当前实现不可执行|`ColdRebirthR34.prepare` 对已推进旧 cursor/stateReference 必须回读旧 state 并验 hash；缺原件会 hold|严格分开同根恢复、有原件的同谱系 ColdRebirth、无 parent 的独立新基础实验；不得删除旧证明门|CLOSED_PLAN_CHANGE_VERIFIED：B05、创造合同与 PIPELINE 三路径表均已同步|
|SR-03|把 Director 的唯一启动写成 COMPLETED 结算反馈，容易形成无预测→无结算→无训练→无预测循环|开发 bootstrap 有独立 canonical 历史入口；Rolling 的 COMPLETED 门则有真实意义，不能移除|显式列出独立开发调用链；加 REAL=0、证书=0、全 NO_PREDRAW 的正例。图也应画出历史直接进入开发路径|CLOSED_PLAN_CHANGE_VERIFIED：无 NO_PREDRAW 伪事件的正例与 A→G 开发入口均已合入|
|SR-04|部分卡只有一个值对象文件，严格“只动卡列文件”时实际 owner 不在施工白名单；大量 acceptance 只是重复 preserve|例如 typed rule 的 pair 尝试发生在 AtomPublicationStore/AtomWorld，而非纯 TypedGenerationRule；pin 重复投影发生在 RuntimeR27|合入 16 卡 supplemental 的 source_files_add、method_anchors、repair_append、acceptance_append，保留原审计历史|CLOSED_PLAN_CHANGE_VERIFIED：16 卡全部合入，60 个相关源码 SHA 锚一致|
|SR-05|pin load 被说成所有异常归 null；INVALID 长摘要被描述成同 epoch 修复|`loadCurrent` 仅合并指定状态，部分 schema/hash 异常继续抛出；LongScaleSummary 对非 ACTIVE head 按 fresh 创建新 epoch|使用 R344-027、029 的 cause_override；新增诊断不吞 fatal/integrity/cancel；不复活旧 epoch、不回写可知时间|CLOSED_PLAN_CHANGE_VERIFIED：027/029 当前 cause 已精确修正，原 raw 审计保留|
|SR-06|批次 DAG、工程验证与现场自然签收容易被混为一个总门|B09 自然主链需要 B04 的真实赛前/结算链；全部图表产出、全部旧 root 或自然达标本身不应阻止其他工程和训练|明确 B04 为 B09 自然现场验收前置，合成正负例只签 ENGINEERING_PATH_VERIFIED；各 WAITING 按自身输入局部生效|CLOSED_PLAN_CHANGE_VERIFIED：043 及 B08/B09 输入门已明确，12 批 DAG 无环|

### SR-01：拆掉的是错误的全局前置，不是任何原件或质量门

`RuntimeCoordinatorR26.runModernContinuity`（1028–1044）进入 `ResearchLearningContinuityR912.pulseWithinR927`（260–284），后者的 lane 13（357–362）调用现有 `ResearchDirectorR952.pulse`。Director 的 IMPORT 槽经 `ModernSeedFactoryCP12` 调用 `ModernFamilyFactoryCP12.maintain`；其 165–192 行只从合法已确认 canonical draw 取得源 issue，并依工厂的实际原子/分子/特征输入准备候选。此处没有 R27 原 pin、全部旧 checkpoint、REAL 或证书前置。

独立新实验必须使用新候选/协议/输入身份，计数从零开始。不继承旧权重、样本数、表现、冻结或资格；不重置旧工厂游标冒充新创造。对应旧 pin/cursor、旧 root、失败任务和曝光账本继续保留并显示具体缺因。

真正必须保留的门仍在：原任务续跑只能使用其精确原输入；教师/绑定模型必须有真实原件；学习家族需要已验证材料及足量合法历史；未知提交先回读；资源准入及唯一 writer 保留。

### SR-02：当前 ColdRebirth 并非缺原件时的万能启动器

`ColdRebirthR34.prepare`（102–118）先核 parent identity；当旧 cursor 已推进或已有 stateReference 时，必须有 stateReference，读取其 checkpoint 并校验原状态 hash。`step`（208–240）对缺原件保持对应 parent 的等待。虽然 coldGenome 不继承暖权重，它仍依赖旧 parent 原件证明来建立合法谱系。

因此“完全独立新基础实验”必须经现有 `ModernFamilyFactoryCP12 → Director` 路径，不能伪造一个 ColdRebirth receipt，也不能让新候选冒充缺原件父代的后续代。此分流是在保留现有证明合同的前提下恢复进展。

### SR-03：无 REAL 与无 COMPLETED 是两个不同场景

`RollingLearnerR343.settled`（63–74）需要权威开奖与真实 `COMPLETED` 结算来源，不要求 REAL。没有赛前预测时，可能只有 `NO_PREDRAW_CANONICAL_TERMINAL`；该状态不能被当成 COMPLETED，也不能为了训练创建假 rolling 事件。

必须实际覆盖的启动反例为：`REAL=0`、`certificate=0`、旧期全部 `NO_PREDRAW_CANONICAL_TERMINAL`，但提供足够可信 canonical 历史和已验证 catalog。在同一原 owner/预算体系中，独立固定图和一个无需教师的学习内核应能完成开发回执；此时 rolling_event 保持零完全合法。后续有真实冻结与合法 COMPLETED 来源时，才自然进入既有 RollingLearner。

现有创造合同已明确这一点。主链图应从“合法开奖与固定输入水位”另连“开发训练”，并注明足够合法历史与自身预算，以免图形把反馈环误读为唯一启动入口。

### SR-04：可施工内容应到实际 owner 和具体反例

补充件覆盖所有 ST 卡以及 R344-025。重点如下：

|施工卡|补齐的实际位置|具体可签收结果|
|---|---|---|
|015/016|ResearchWriteQueueR27、DatabaseCoordinator、WriteBudgetR341、RuntimeR27.DatasetTask|精确旧/新分类可重试；同 prepared 身份只提交一次；prepared 小批次可进入原公平槽，但不能越过活动结算/freeze/fence|
|026/027|ResearchInputPinR30R1、RuntimeR27、ReplayKnowledgeStoreR31|精确同 pin 恢复；typed 原因保留传播语义；已有精确投影才可省掉纯重复 metadata 写，缺投影仍真实提交|
|030/031|TrainingRecoveryR343、Director、训练 hook、现代 continuity 与工厂|五元组只扩大一条已证实原任务；已有 attempt0 继任被实际 claim/prepare/commit；固定图前向和学习内核数值阶段分别签收|
|032/033|EvolutionControllerR952、MutationGraphCP12、AtomPublicationStore、AtomWorld、ResearchContext、ResearchConsumptionStore|持久 exact proposal 不换父代/种子；pair 尝试真实计量；typed rule 更晚不同 dataset 的两个子图实际开发评分|
|034/043|现有 RollingLearner/Publisher、Director/hook、LiveTask、CertificateGate/MainlineAdapter|重复结算不二次学习；冻结窗不被学习头覆盖；资格工程正负例与自然主链证据分别签收|

每项补充不仅有成功例，也有错误身份、无原件、输入修改、源修正、晚标签、未知提交、重启和重复回执的反例。没有要求为“产出率”伪造梯度、模型 hash、artifact、pair 数、训练回执或证书。

typed rule 尤其需要避免改错层：`TypedGenerationRuleR342` 是纯类型与规范对象；供体最多 8 个、pair 上限 28 的准备循环在 `AtomPublicationStoreR31.prepareRuleR342`（54–84）。写尝试回执应由既有生产 owner 完成，不能在纯对象里加数据库写者。没有合法供体时该可选分支继续返回无规则，不能堵住原子、分子或无教师基础家族。

### SR-05：两处原因需要纠正到实际分支

`ResearchInputPinR30R1.loadCurrent`（41–47）对 malformed hash、`invalid=true`、找不到 pin 及 `pin.require` 的 StaleResearch 返回 null；schema 异常和部分 decode/hash 异常仍可传播。typed outcome 必须兼容旧 API，并保留取消、fatal、schema 和 integrity 的既有处置。不能为了增加诊断把原本的完整性错误静默变成“暂无数据”。

`LongScaleSummaryR80.prepare`（118–152）在 old 不存在或状态不是 ACTIVE 时设置 fresh，并建立新 epoch；`commit`（164–179）按源 token 与 before-fence 提交。旧 gap 的源修复可以改善后续新 epoch 的输入，不能把旧 INVALID_SOURCE 直接重新标 ACTIVE，也不能改写其过去的 available_at。旧 epoch 及已有 journal/snapshot 不改写；允许按照当前代码向 journal 追加真实的 epoch 切换记录。

## 3. 已经正确、应维持的设计

1. **不重造 173/211 已实现的 owner。** 当前方案保留六个 Director 槽、五个训练子槽、训练预留、RollingLearnerR343、RollingFrozenPublisherR343、MutationGraphCP12、CreationChangeKindR343 与旧 AST1 在线产物。当前 5 条旧失败、1 条恢复继任的现场事实已经纳入，未把旧四例修复写成仍全无实现。
2. **数学机制按实际内核区分。** 15 family 标签对应固定图与 5 个学习内核，共 6 种 ModelKind；参数/窗口/来源变化不自动计成新机制。FIXED_GRAPH 的零梯度是合法实现，验收看真实 PIT 前向与 tape；树用 TREE_QUANTILES/TREE_SCAN，神经网络保留真实 checkpoint/restore 数值状态。
3. **训练、开发验证、确认未来分开。** 300–1200 训练行、120 验证行和 embargo 的开发过程不能替代独立未来。多次尝试、永久 alpha/曝光、预登记、冻结、校准与资格保留。工程修复不承诺每代必强或长期高胜率。
4. **保留唯一实时与结算路径。** 下一期分析请求先于当期精确结算的既有次序保留；REAL 不被研究动作总数或 SHADOW 分母替代；新的训练、图表或报告代码不能成为第二个真实决策 owner。
5. **报告与原件保全边界明确。** 检测报告维持原 diagnostics/ReportExportServiceCP4 路径；未要求因训练修复重建导出、改旧封印、改签名、删库或清故障队列。本文没有对报告实现作候选修改。

## 4. 验证结论的边界

本次策略审计的两个窄候选补丁均已在精确 211 树 `git apply --check` 通过，但未应用到应用树。writerBusy 的真实方法抽取在 Java 源文件启动器上执行了 30 例：原版 1 个预期失败、候选 0 失败。验证级别仅为 `SOURCE_EXTRACTED_CLASSIFIER`，不等于完整 JUnit、Android、native SQLite、设备或故障恢复测试。

报告未提供可供本审计恢复的完整数据库、原 APK、原 `.scdev` 与模型/checkpoint CAS 字节。因此“现场原 pin 能否还原”“第 5 条训练原件能否验证”“用户设备迁移后能否连续运行”“自然未来是否达标”等仍需施工后的真实签收。它们必须保持 NOT_RUN / NOT_PROVEN 或具体等待原因，不能从本方案补成成功。

## 5. 合入后复核记录

已对合入后的 `ISSUES.json` 逐卡验证：16 卡的新增文件、动作、验收、方法锚和完整 supplemental 快照全部一致；027/029 当前原因采用精确修订；所有卡保留 `original_audit_records`。未修改原审计来掩盖修订。

独立验证结果写入 `final_strategy_merge_verification.json`：

- 46 个问题、12 个批次，批次依赖无环。
- 16 个补充卡，合入缺项 0。
- 这 16 卡涉及 60 个源码文件，全部存在且 SHA-256 与精确 211 树及计划 SOURCE_ANCHORS 一致；检查时全计划共 136 个源锚。该数字是读取时的计划规模，不是应用修改文件数。
- B06 依赖集合精确为 B00/B03；B07 为 B00/B06。
- B05 保留同 pin/同 parent 的证明门，同时允许完全独立新基础实验；B06 明确自身输入门。
- 开发合同明确 REAL=0、证书=0、全 NO_PREDRAW 时独立开发仍可完成，rolling 不制造事件；PIPELINE 图已增加 A→G 入口。
- R26 三路径表已同步“有原 state 的 ColdRebirth / 缺原件旧父隔离 / 独立工厂新实验”。
- B08 以 ENGINEERING_READY 与 PENDING_B09 区分工程可用和待自然证据；B09 自然主链签收要求 B04 的 PIT、唯一 owner、合法截止与精确结算工程回归，但不要求全部 72 小时长测或全部旧 root 恢复后才能做工程验证。

**SR-01–SR-06 均关闭为 `CLOSED_PLAN_CHANGE_VERIFIED`。方案复核通过；没有把产品验收或自然质量状态改为 PASS。**

上述核验是计划结构、源锚与方案因果合同检查，不新增候选补丁运行结论。相关文件的读取时 hash 保存在验证 JSON；若施工方再变更本文涉及的门禁、owner、源身份或验收定义，应重审相应项，而不是直接沿用本次签收。
