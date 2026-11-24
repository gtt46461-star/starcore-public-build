# 实际执行回执和72小时原件格式

第 6 版 R341-B08-04-A01 增加 rule6_index_artifact，格式见[新规则复算](field6/foundry.md)。A-B12-03b 增加 `mechanism6_index_artifact` 和同次运行的实际原件。[完整格式](field6/witness.md)

空模板在 `control/RECEIPT_TEMPLATE.json`。字段名是 `tools/verify_receipt.py` 的真实接口；本文件说明如何形成可核验的输入，不提供虚构的通过记录。

## 普通批次回执

`plan_manifest_sha256`填当前 `control/PLAN_SEAL.json`的manifest摘要；`source_before/source_after`填真实40位Git提交。两者都须在122应用底座之后且顺序可证；ENGINEERING回执不能只有版本元数据、测试文件或生成产物变化。已有真实实现只做重验时，`revalidation_only=true`且附原实施回执字节/摘要，仍须本次全部场景重新执行。

`issue_ids`列本批完整工单；`test_results`逐项列本批相应level全部验收ID。每个结果记录真实argv、整数退出码0、hash绑定原始日志和非空测量断言。断言支持eq、ge、gt、le、lt、in；null不变成0，布尔不冒充计数，NaN/Infinity不能通过。

`artifacts`每项为相对 `evidence-root` 的文件路径和真实SHA256；不允许绝对路径、越界、重复项、摘要不符。每条断言的 `evidence_artifact` 必须指向列出的原件。测试命令由施工者审阅后实际执行，验证器不执行回执中的命令。

验证器返回 `PASS_RECEIPT_STRUCTURE_AND_DIGESTS`，其含义只限于结构、身份、范围、数值断言和原件摘要通过。需审阅生产测试、输入真实性和业务语义后，才能在实际台账填写相应工程结果。APK构建/签名/安装另有证据，不能因该状态自动生成APK_VERIFIED。

## B12的DEVICE回执

本批DEVICE覆盖 `A-B12-02a` 真实覆盖安装以及 `A-B12-04a` 完整后台campaign。最终窗口必须至少 **259200秒真实墙钟**；24小时截点为中期证据，不能代替72小时最终窗口。窗口带时区，例如 `+08:00`；同APK、source、设备身份，不拼接换包前后。

顶层 `background_campaign_artifact` 指向hash绑定的JSON原件，字段为：

```text
source_commit, apk_sha256, device_identity, window_start, window_end, fixture=false,
preregistered_calendar_artifact, opportunity_ledger_artifact,
boot_segments_artifact, interventions_artifact, raw_events_artifact,
checkpoint_24h_artifact, coverage_evaluator_artifact
```

这些 `*_artifact` 均指向同回执artifacts中的文件。原始boot分段保存单调时钟与墙钟锚点，跨boot不能相减；interventions保存每次手动打开页面、装包、网络/电源操作的发生时间与目的，不删不利干预。

机会日历JSON含 `targets`数组；每项含唯一 `target_key`及实际预登记时点/窗口/来源。机会账本JSON含 `opportunities`数组；每项至少含相同 `target_key`、`terminal_state`、`reason`、`role`，以及B04定义的时点/输入/提交证据。两者键集合必须一一相等且非空。没有业务产物的目标也须保留为明确missing/失败状态，不能从账本消失。

`coverage_evaluator_artifact`包含实际B04/B05预登记SLO、逐目标评价、全部机会分母、成功样本分母和最终判定。结构完整不等于性能达标，验证器不会靠存在一个JSON就替评价器判定性能。晚到、被替代、未开始、不可用、期限失守等仍计入各自失败统计。

## NATURAL回执

必须 `fixture=false`，绑定真实设备/APK/源码和观察窗口，并有hash绑定的 `preregistration_artifact`、`future_calendar_artifact`、`natural_evaluator_artifact`。自然评价需实际B09冻结评价器结果；原V1资格和质量投影分别报告。

这里的用例可以验证系统如实产生QUALIFIED、REJECTED或样本不足；验证器结构通过不意味着候选已QUALIFIED。应用是否获得高品质资产，应读取真正的评价结果、所有条件和合法证书，不能把“完成评价”解释为“具有未来优势”。

## 本次复核采用第二版回执

新封印执行 `STARCORE_R341_EXECUTION_RECEIPT_2`。第一版回执按原 Git 版本、原封印留档。新版本的接受条件由本节和本目录的校验器共同定义。空模板始终保留 `template_only=true`。正式提交填写 `template_only=false`、本批全部 `issue_ids`、当前封印和分层状态。未运行、阻塞、失败、自然待积累均可保存，给出 `reason`。验证器输出 `RECORDED_NONPASS` 并返回退出码 2。只有明确提交 `status=PASS` 才进入实际签收检查。不得用模板、非通过记录或另一层的结果更新通过状态。

### 工程证据的产生顺序

1. 取本批 `ACCEPTANCE_MATRIX.json` 当前层的完整用例。每项直接采用其中的 issue、harness、method、argv 与 `required_review_ids`。本次运行只依赖已实现的当前批与前序批。工具使用最终 source 提交中的 Git blob；测量前应完成对应源码和测试提交。
2. 将经审阅的度量名称、算子、预期、可选的 review ID 固定在对应测试源码。Python 或 Java 可以复用现有测试文件，单行注释格式为 `STARCORE_EXPECT <完整case ID> {"某个度量":{"operator":"ge","expected":1,"review_id":"该case实际关联ID"}}`。也可引用同一源码提交内的 JSON，顶层键为 case ID，值为该用例的度量字典。对运行时才确定的对照值，可用 `expected_pointer` 代替固定的 `expected`，由断言列出 `expected_artifact` 指向独立输入的原件。施工审阅需核实该对照的来源与测试覆盖的业务条件。源码中定义的每项测量均需出现在结果里。
3. 按合同 argv 运行；将占位路径替换为实际源码与输出路径。若复用已有等价测试，将映射作为当前源码中受审阅的 JSON 提交，`run.equivalent_mapping_path` 指向它；映射以 case ID 为键，值含 `case_contract_sha256/path/method/command_argv`。映射必须实际选择此用例，并记录对应测试文件 blob。现场命令按设备/自然合同记录实际 argv。
4. 每个 case 输出独立的 `execution_artifact`，保存运行器真实输出。基本字段见模板中的 `runner_original`。 `case_contract_sha256` 为该用例整条矩阵记录经 Python `json.dumps(case,ensure_ascii=False,sort_keys=True,separators=(",",":")).encode()` 后的 SHA-256。执行环境、引用日志、工单生产 owner 路径和实测字段均绑定到此 case。工程用例的 `runner_path/runner_method/runner_blob_sha` 取自真正执行的测试文件。若同一设备运行产出多个 case，可共用完整原始日志，每个 case 仍有自己的执行记录。实际断言列表必须与回执一致。
5. 每条断言的 `evidence_artifact/json_pointer` 指向独立 JSON 原件的实测字段。保留数据库导出、仪器运行记录、原始文本日志；若需要从文本、SQLite 或二进制规范化 JSON，收录原件、转换程序提交和输入摘要并现场复算。JSON Pointer 支持转义和数组。未读取到字段会失败；测试确实要求 SQL NULL 时使用 `operator=is_null`、`actual=null`、`expected=null` 并读取真实存在的字段。没有观测和测出零必须分别登记。每个额外 `required_review_ids` 至少由一个具体且通过的断言的 `review_id` 承担，不允许用其他工单的 ID 代签。
6. 本批实际修改的可执行产品文件至少一个命中本批的 NEW/MODIFY 文件卡；每个 case 原件必须关联所属工单在当前 Git 提交内真实存在的生产 owner。继续逐卡检查改动、已有实现、VERIFY_ONLY 的实际行为和原件。若以同一封印下的原实施回执重验，提供 `original_implementation_receipt_artifact` 与 SHA-256；按原实施回执所在目录摆放其相对原件，验证器递归重核原实施批次、提交祖先和真实签收，拒绝无限重验链。历史封印不能充当新封印的实施签字。

### 现场和自然证据的补充字段

- DEVICE/NATURAL 添加 `apk_artifact`（实际APK字节）和 `apk_identity_member`（B01写入该APK的唯一assets成员）。校验器核对内含 `sourceCommit/planSha256/versionCode`、APK SHA-256、设备与观察窗。每个 runner 原件同时写入模板中的全部 `field_runner_extra` 字段。实际签名与覆盖安装输出仍按B12保留。
- B05自己的连续后台用例和B12最终后台用例都检查实际72小时。campaign增加 `installed_version_code`，24小时截点原件记录同一起点和实际截止时间。7个原件保留不同的路径。覆盖评估原件回写 `verdict/unattended/opportunity_count/calendar_sha256/ledger_sha256`。本机操作、打开页面、断网、重启等干预逐次留证。只有预登记的无人干预场景真正满足约定才可给 `unattended=true`。B05工程签收依其工程合同独立完成。
- NATURAL 三类原件均保留相同 `candidate_hash/protocol_hash`。预注册原件给出 `freeze_at`；日历的 `targets` 每行给出唯一 `target_key` 和带时区的 `opportunity_at`；评估器原件给出 `source_commit/apk_sha256/fixture/preregistration_sha256/calendar_sha256/opportunity_count/future_act_count/verdict`。这些是对原生状态的导出映射；需保留原始机会、冻单、结算及原评价器结果供重放。没有候选注册时标记自然未观察并说明原因；已注册但当前机会为零时可如实返回 `NATURAL_PENDING`。未达门槛或证书撤销均保留原 verdict。
- 自然结果声称 `QUALIFIED` 时另外填写 `v1_pass/evaluated_at/certificate_artifact/v1_metrics`。指标含 `hr/ece/worst720Block/maxLosingStreak/pairedLcbVsI00/pairedLcbVsNull/abi_equivalent/sequential_pass`。证书原件包含 `candidate_hash/protocol_hash/issued_at/expires_at`。认证条件、证书时效见B09，今后升级协议须重新封存。评估结果另列 `math_verdict=PASS|FAIL|NOT_EVALUATED`、`issuance_status=ACTIVE|NOT_ISSUED|REVOKED|NOT_EVALUATED`、`quality_projection_status`。状态、支持数与这三项一起进入 `natural_evaluator_outcome`。质量投影在新窗口可为 `NOT_OBSERVED`。
- 有效 ACT 大于零时，`future_act_ledger_artifact` 导出逐份独立未来的已结算原件，保留 `candidate_hash/protocol_hash/source_commit/role=PREREGISTERED_FUTURE/fixture=false` 和 `settled_acts` 数组。每行含已登记的唯一 `target_key`、`freeze_return_at/draw_at/settled_at` 和布尔 `hit`；写明冻结成功返回早于开奖且原始结算属于同一机会。B09可以完成验证系统“真实待证”的工程或结构核查。B12最终自然用例遇有效 ACT 少于250或评价仍在等待时，验证器保存实际结果并返回 `RECORDED_NONPASS`、退出码2。自然通过与统计优势分栏记录。
- 验证实发证书时读取其持久结果 `status=ACTIVE` 和 `issuance`。其中记录 `issued/model_binding/calibration_binding/qualification_binding/source_revoked/qualification_gap_or_conflict/failing` 及 `registeredFutureR34` 的原始统计。三个 binding 均需 BOUND，来源未撤销，无资格缺口或冲突，failing 为空；独立机会中已出手并结算的份数占比至少15%，按 `hits×winNetProfit−(acts−hits)×missLoss` 复算的 `netRecordedPayoff` 须为正。核对原生 `opportunities/acts/hits/coveragePass/expectedPayoffPositive`、证书摘要与有效期，保存真实表行及其产生时的绑定原件。部署后的 REAL ACT 和额外质量投影各自观察，首次发证可在它们待观察时完成。

### 执行命令

从本目录运行 `python3 tools/test_receipt_guards.py` 复核签收工具。提交回执时实际运行：

```bash
python3 tools/verify_receipt.py --plan . --receipt "$receipt" \
  --evidence-root "$evidence_root" --repo "$app_repo" --level "$scope"
```

退出码 0 表示用例范围、Git身份、测量原件及结构检查通过。生产测试是否实际覆盖所有业务边界、原件来源是否可信、候选未来表现和设备环境是否可复现，须独立复核运行与数据。

### 逐期复算与截点边界

1. 自然日历按同一期的 `target_key` 分别固定开放时刻 `opportunity_at` 与严格截止 `deadline_at`，预注册原件固定权威开奖来源 `draw_source_id`。评价器另引 `frozen_predictions_artifact` 与 `authoritative_draws_artifact`。冻结导出含同 candidate/protocol/source、自然角色及 `fixture=false`；`predictions` 每行给出唯一 target、真实冻结事务成功返回时刻 `freeze_return_at`、1–10 的 `position` 与三个不同号码 `numbers`。开奖导出含相同 `source_id`、`fixture=false`、`draws`，每行给出唯一 target、权威 `draw_at` 与按位置排列的10个不同号码。在登记窗口开放之后、截止和真实开奖之前完成冻结才有效。逐期用冻结位置从该期权威结果取值，与冻结号码集合计算命中，再同结算原行逐一对照。须保留原始来源标识和提交返回证据；无法恢复独立截止、权威开奖时刻或赛前返回时，请登记待证。
2. 72小时 campaign 中的首24小时按半开放的机会窗 `[start,start+24h)` 切分；截点原件的 `window_end` 必须精确为带时区起点加86400秒。导出可以稍后完成，`exported_at` 另记。首日截点含与整轮相同的APK、设备、版本、源码，绑定整轮 `calendar_sha256/raw_events_sha256`。日历所有目标给出 `opportunity_at`。原始事件提供可从原日志复算的 `opportunity_events`，每项含 `target_key/observed_at/sequence/role/terminal_state/reason`，`sequence` 在同一目标同一时刻消除歧义。截点的 `opportunities` 恰好覆盖首日预登记目标，按当时可观察的最后一条事件形成；无事件则截至边界写明 `NOT_OBSERVED_AS_OF_CUTOFF`。不得把之后的结算归入首日快照。保留原始日志与适配器，休眠、重启和人工干预照常记账。
3. 质量状态为 `PASS/FAIL/NOT_OBSERVED/INSUFFICIENT/PENDING/BLOCKED`。综合评估声称 PASS 时，`quality_projection_artifact.components` 列出 Q01–Q08 的 `status/evidence_artifact`，每个对象保留报告 metric ID 和实际源码。Q01–Q07 沿用预登记候选、协议和自然观察口径。Q08 是独立的合成功效验证，仅与工厂协议、配置和同版源码关联，不能加入自然 ACT、自然命中、候选证书或主链 REAL。

| 条目 | 签收读回 |
|---|---|
| Q01、Q02 | 自然已结算 ACT 至少250份，命中率≥34%，37%为研发优先目标。部署前单列 `future_act_rate`。部署后 Q02 从 `deployed_opportunities` 的唯一目标逐期计算真实出手频率，登记 `real_act_rate`、原始分母、目标≥15%及采样支持；未部署为待观察。Q02 的部署后结果不阻止首次合法发证。 |
| Q03、Q04 | Q03 用合规原序贯状态、预分配的 `allocated_alpha` 和实测 `log_e_up` 复查 logE≥log(1/α)。Q04 的 `window_rule_hash` 与事前登记匹配，同时报告原固定720块、最差块及最长连错和漂移。V1 的最差块≥26%、最大连败≤6在证书核验中继续生效。 |
| Q05 | 提交 `CalibrationRowsR27.fitRolling` 的逐期 `samples`，保留 `Evaluation.json()` 的 ordinal、position、原概率与校准概率、标签、fitHash、fitRows、fitThroughOrdinal、supportLo/Hi、supported、fitRole、evaluationRole。按 ordinal/position 原顺序处理同分预测，使用120条先前拟合记录、1期 embargo 与 FUTURE_READ_ONLY。逐行用自身拟合区间判支持。N≥200（滚动上限5000）、十等质量箱加权 ECE≤0.04、受支持样本占比≥90%才签 Q05。按校准概率稳定排序，前9箱每箱 floor(N/10)，末箱收余数。另列十等宽箱 ECE、Brier、常数0.30同标签 baselineBrier、BSS、逐行支持范围与固定64期块的 BSS 区间。少于4个完整块时区间如实为空。`values` 的字段是 `support/eceEqualMass/eceEqualWidth/brier/baselineBrier/bss/supportCoverage`，区间在 `calibratedBssBlockInterval95`。等宽 ECE 和 BSS 属于并列诊断，不能替换等质量 ECE 或设置新的 V1 门槛。 |
| Q06、Q07 | Q06 以同机会父子收益配对的单侧95%下界>0判为验证改善，其他对照原件按各自协议保存。Q07 独立核查资产的 Q01/03/04/05 与原 V1、有效证书，再计 `independently_qualified_assets` 和 `qualified_candidate_hashes`；一次报告至少检查1份资产是观测支持，单独 ACTIVE 行数仍待验证。主链消费另查真实决策原件。 |
| Q08 | `factory` 取原 `R31_FULL_FACTORY_EXPERIMENT_2_FIXED_FUTURE` 快照，预注册的 `factory_protocol_hash` 与原字段完整摘要、每条原件和当前源码相符。NULL_UNIFORM、NULL_TIME_SHUFFLED、SIGNAL、SIGNAL_DISABLED、DRIFT、GAPS 各20个 seed（710000–710019），120条一一齐备、无失败或重复。每臂复算双侧 Wilson 95% 区间；旧协议合并两种NULL与SIGNAL_DISABLED的60个名义试验（同seed有相同流，分组误报另按本版复算），Clopper–Pearson 单侧95%误授上限≤.15；SIGNAL 的20份单侧95%检出下限≥.70。另附同版 `NullPipelineSimulationR30` 的 `power_trials`、`power_intervals`、`power_source_commit`，在真实植入概率34/37/40%下各20次、至多5000期，依据每次原记录复算各强度检出率和Wilson区间。这60条另计，不能替代120个工厂试验。其他研究协议单独登记，禁止冒用这份注册的签收。 |

持续保存各项目原始数据、水位、配置与可复算的输出。质量观察尚不足时，合法证书仍按原发行路径处理。

## 逐对象方向的回执

读取封印矩阵的 `field3_directions`。同一工程用例内每个方向提交两个来自实际生产路径的观测，一份有效输入与一份错源、缺证、反例或对照。合法拒绝和无改善均可按原场景判定。将 `direction_results` 放在 run 执行原件，每行含 `key, contract_sha256, positive_metric, negative_metric`。两项名称引用本次提交中已按源码 oracle 验证的 assertions。每项 JSON 指针指向业务测量字段，其父记录带 `direction_key, contract_sha256, polarity, case_id, source_commit`。两个 polarity 用 `positive`、`negative`。一份日志可包含多条不同对象记录，每个对象都需保存自己的输入、输出和失败测量。用例无法复现时写待办与缺项。

## B12 三个连续公式的工程回执

第 3 版的 A-B12-03b 在执行原件中增加 `learning_trace_index_artifact`。索引、三轮训练到研究的原件、两次出生、父子公式、模型状态、权重字节和同一构建的 APK 均加入回执的 `artifacts`。正式回执校验器在核对源码提交、方案摘要及 APK 后复算这条链。字段、采集顺序与独立复算方法见[三代连续运行实例](field3/chain.md)。

## 第 4 版新增必验原件

- 按每个工程用例的 `inherited_path_probes` 提供源码和实际载荷能对上的生产者、消费者及正反测量。[字段和适用范围](field4/owners.md)
- B12-03b 的 `continuity_index_artifact` 与既有三代索引同 APK、同提交，并从源码中的固定场景重算连续运行。[持续生产及复验](field4/continuity.md)
- B09-04-A01 读取 `mainline_campaign`，用受控授权证明正常选择到实盘交易账和结算全过程。[主链收据与用例](field4/gate.md)
- B05/B12 的 DEVICE 背景观察同时提交期历、业务账、boot、干预、观察器及原事件。[后台验收](field4/flow.md)
- B09-05-A02 增加 `q08_component_artifact`。第 4 版自然质量投影 PASS 时还会从原件复算 Q06 与 Q08。[父子独立配对](field4/q06.md) · [按同种子校验误报](field4/quality.md)

## 第 5 版新增字段

- Android 工程用例提交当次有界沙箱、运行器参数、实际进入与结束日志及 JUnit 原件。[Android 回执](field5/dispatch.md)
- B12 的 `continuity_index_artifact` 与 `learning_trace_index_artifact` 来自同一次运行。联合核对注册的三代目标、实际事务和跨进程消费。[联合验收](field5/trace.md)
- 自然质量 PASS 时，Q04 的 `window_rule_hash` 和预登记值按既有分块公式确定，由当期独立 ACT 结算原件重算。Q02、Q07 在宣称实际应用时从正式交易与结算快照回读。未部署或观察不足的状态保留，首次合法发证走原 V1 门。[稳定性](field5/stability.md) · [正式交易核对](field5/real.md)
- APK 前缀回执索引及命令见[逐批交付](field5/apk.md)。B01 即记录旧方案与当前方案各自的构建字段。
