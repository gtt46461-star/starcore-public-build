# B04 至 B08 研究证据、训练恢复与主链核查

本页随当前仓库修订一起使用。命令中的 `PLAN` 是已还原并核验的 `STARCORE_R33_5_20261118` 目录，`REVIEW` 是本页上一级的当前修订根目录，即准备结果中的 `current-review`，`OUT` 是本次独立输出目录。先执行原包 `verification/verify_package.py` 和当前 `verify_review.py`。本页脚本从同一原包读取原校验逻辑，直接读取一致性 SQLite 快照或哈希绑定的原始 JSON。结果归入对应批次的 POS、NEG、TRACE 原件，沿用原工单与资格协议。

## 执行链与当前源码身份

生产施工统一经 [replay/stage_checked.py](../replay/stage_checked.py) 按 P00→B08 顺序接收已提交的前序工程，安装原后态与 [production/OVERLAY.json](../production/OVERLAY.json) 中的完整修订。B04 的计数和恢复修复、B05 的父子配对修复、B07 的报告生产代码必须共同保留到 B08；随后执行本页对应验收与原批次要求。准备宿主工具、运行离线检查或生成单个 B05 文件，均不能代替这条安装链。

**当前阶段身份以 [production/SEMANTIC_RESULTS.json](../production/SEMANTIC_RESULTS.json) 为准。** [rebuild_overlay.py](../production/rebuild_overlay.py) 在每批累计完整源码上调用原 `build_semantic_inputs_r335.py::identity()` 和 033 的 R967 `build/write_outputs/verify_outputs`，同步生成 R82、依赖清单及两项已跟踪的 R967 输出。当前 B07/B08 的 `trainingRuleSha256` 为 `619fbc6ba89b6448e8eb663fd303c951b5fe7628ce0f91bfa6f18015ee629bac`；对应完整 `release/r82_training_invalidation.json` 的 SHA-256 为 `7f3a62b2176b1e7c03809db3ed5ff6d6389c3bb6e1929f102bf86c6116b6126c`。B04、B05/B06 的各自身份从同一结果文件的对应阶段读取，不能提前套用最终阶段值。`policyHash` 保持原值，旧资格兼容前身列表为空；实际候选提交前仍运行原 R967 `--check`，按新身份重新采集资格证据。

## 施工和签收

| 批次与工单 | 原代码和具体缺口 | 在本批完成的动作与合格条件 |
|---|---|---|
| B04／C03、RE16 | `AtomNetSessionR30.report()` 在首批训练后就把全部训练历史期计入；`TRAIN` 非零游标缺 shuffle 的 checkpoint 可被接受，恢复时重洗牌并从旧 offset 继续，导致下一批权重与正常恢复分歧。 | 安装当前完整 owner；按实际已访问训练位置去重历史期，无法还原的部分历程报告未知；拒绝非零游标缺 shuffle 和不可能的终端 TRAIN 游标。运行本页 B04 实际 owner 回归，继续取得同批 dataset→checkpoint→十位置发布→consumer 的真机原件。 |
| B05／RE24 | after_full 中 strategyv2/DiagnosisEvidenceR335.java 的 paired() 用内连接统计同时有 hit 的两臂，只检查 settled_at>0、frozen_at<target_open_time；旧库负冻结时间或开奖前结算可进入统计。两报告的 UPGRADE_DIAGNOSIS.json 中 R01、R10、R17 均记 NO_RECORDED_OUTPUT，无法据此推算独立提升。 | 对报告实际展示的每一对 childKey/parentKey，用其 throughExclusive 在同一快照执行下述 paired，复算窗口、均值、有效配对和两臂 ACT。窗口序号分为双臂已结算、至少一臂缺冻结、已双冻待结算，三数之和等于窗口长度。把异常期的原始行留在故障证据，停止将该窗口作为有效配对。报告现有 descriptiveOneSided95NormalLower 只作描述，缺观察、挑选父代、连续相关均不得据此签优质。 |
| B06／RE19 | 033 的 SequentialQualificationR27、CertificateGateR27、QualificationWindowR26 和实际 typed 路由已有实现。R27 的已结算 FUTURE ACT 与 SU2 的固定日历不是同一分母。 | 依 B06/C11_033源码逐项保全审查_R335.md 原序执行独立自然资格。FUTURE 的每期给出 ACT、ABSTAIN 或 MISSING 及源行。R27 保留 250 ACT、HR≥.34 和原序贯/稳定/校准/基准全门。SU2 保留 69120 期及各自校准窗。对同 APK 的合法 BET 在 B06 当批独立观察唯一 writer、证书源、授权、冻结、清算。证书过期、撤销、关闭及无证分别给原拒绝理由；授权关闭时验正常 SHADOW。宿主验收命令如下，不计作已获得自然证书。 |
| B07／研究处方回访 | B07/verify/check_improvement.py::compare_followup 校验首报→施工→复报，但不把实验的原始冻时/开奖时间与施工和复报夹紧。原测试自身可返回“本次试验测得改善”，即使所有 8 个开奖结果都在复报之后。B07/replay_r335/case_loop_r335.py 另外负责真正的 Git 改动和报告身份，保留其不同用途。 | 先运行原 case_loop 的 extract、followup，再在采用处方实验结论时执行下述 followup。原预登记可以早于施工；两臂实际预测要同时晚于施工及注册，开奖不晚于复报观察。非法或缺失时间不能签“测得改善”。显著结论限这个预登记比较；批量筛选、重选窗口、未留所有失败试验的情形保留探索状态。 |
| B08／D06、B08.2 | verification/mainline/check_trace.py::trace 已约束预测早于开奖与首次观测，但 OOS label 仅检查 settled_at>=first_seen_at。若源端提前可见，重算原有全部哈希后，早于 open_time 的标签仍能令原校验返回 PASS。 | 以原 B08.2 的同一个 trace manifest、证据根、候选 commit 与 APK SHA 执行 trace。先复跑原 INTEGRATED 原件联接，再逐机会验证 `settled_at >= max(open_time, first_seen_at)`。百期窗口在自己的 manifest 与同一冻结库上独立调用相同检查，使用另一份 `--window-bundle` 时也不能跳过。异常拒签并保全时钟、首次观测和事务时间；短联调与 B08.4 真机长测各自沿用原日历。 |

在施工试验前运行以下回归：

```bash
python3 -B "$REVIEW/research/test_research.py" "$PLAN"
python3 -B "$PLAN/B06/host/run_host.py" --out "$OUT/b06-qualification-host"
```

在相应原件产生后，分别执行以下命令。变量取自现场证据；OUT 为本次输出目录。paired 的通过只表明原始行可复算，要将输出与被验报告对照。任何输入缺失都记在所属批次与目标期下。

```bash
python3 -B "$REVIEW/research/check_research.py" paired "$SNAPSHOT_DB" "$CHILD_KEY" "$PARENT_KEY" "$THROUGH_EXCLUSIVE"
python3 -B "$REVIEW/research/check_research.py" followup "$PLAN" "$CHAIN_JSON"
python3 -B "$REVIEW/research/check_research.py" trace "$PLAN" "$TRACE_JSON" "$EVIDENCE_ROOT" "$SOURCE_COMMIT" "$APK_SHA256"
```

CHAIN_JSON 使用原 B07/verify/check_improvement.py::compare_followup 的 firstCase、work、nextCase 引用合同。若施工后还没有新的预登记实验，维持缺证结论。两个原报告只作施工前现场指标，不能用作本轮施工后的复报。SQLite 的窗口测量仅针对原候选本地序号，由独立日历与冻结记录负责业务机会分母；行数为零时均值为空。报告来源缺少该一致性库时，填写待采集原件，不补造数据。

## B04 实际训练计数与恢复回归

当前 [AtomNetSessionR30.java](../production/sources/B04/app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java) 的完整文件 SHA-256 为 `f61d69c3e879a8aff104b39a0efeaea47b0ae8852170d59db2be12d0fecc18d6`。`independentTrainingCalendarIssues` 只计 PRETRAIN 实际访问过的历史 TRAIN 期：首轮未完成时对已访问 shuffle 前缀的 `sampleIndex / 10` 去重；完成一轮后最多为 `ceil(trainCount / 10)`，后续 epoch 不重复增加。未访问为 0，部分历程与 phase、epoch、offset 或 shuffle 无法相互核实时为未知。SELECT 的开发验证和 ONLINE 更新不叠加进这个计数字段；字段附带的 scope 明确这些历史训练期不是独立未来证据。

可复现计数夹具包含 1,600 个 TRAIN 位置、160 个历史期；首批 64 个位置实际访问 55 个历史期，旧 owner 报 160，当前 owner 报 55。恢复反例构造的是摘要正确、语义不完整的 `TRAIN/offset=64/order=null` checkpoint：旧 owner 接受并造成随后实际梯度权重分歧，当前 owner 拒绝。新增边界覆盖 `trainCount=69`、有效 checkpoint 恢复、后续 epoch 的不一致部分计数和 online 更新不扩大历史 TRAIN 分母。完整源码、实际 Java 测试与前后回执见 [b04/README.md](b04/README.md)、[legacy-result.json](b04/legacy-result.json) 和 [fixed-result.json](b04/fixed-result.json)。

使用包含编译器的 JDK17，并让 `java`、`javac` 指向该 JDK 后执行：

```bash
python3 -B "$REVIEW/research/b04/run_review.py" "$PLAN" \
  "$REVIEW/research/b04/AtomNetSessionR30.java" --mode fixed \
  > "$OUT/b04-current-owner.json"
```

这个入口重新编译实际十二个数值 owner，记录每个输入 SHA，并运行恢复回归与原数值探针。当前回执为 **17,763 项检查、166 个 work slice、60 个梯度 slice、102 个评估 slice、20 个 epoch、10 个在线位置更新**；数值探针的 `pretrainSamples=3200` 是重复训练访问数，不能解读为 3,200 个独立期。权重 SHA-256 为 `5b17ecac4f369f0071031aa07e156232b5894be03363202146fa55308242705b`。这些计数属于确定输入的宿主数值执行；数据库边界使用公开的抛错或类型接缝，Android 数据库发布、真实 consumer、目标机长时恢复与自然质量仍由原批次现场验收完成。

## B05 应用源码的修改

B05 新编译后态由本目录 b05_pair_source.py 从 SHA-256 为 31fdbec19d5ae3aba21a727bda2b6cc06f14931b73c3683bfb545cfa68c3563a 的封存完整源码生成。对应安装路径为 app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisEvidenceR335.java，修订后 SHA-256 为 640bd8c8c0d45d8ff4a2b9f6ed1dcb0117c7bec619470dc025c4e43a8f293249。每个输入/输出都按完整字节比对。需要审阅后态时可执行：

```bash
python3 -B "$REVIEW/research/b05_pair_source.py" "$PLAN" "$OUT/b05-candidate"
python3 -B "$REVIEW/research/test_b05_java.py" "$PLAN"
```

新 pairWindow() 在同一冻结快照上逐序号检查两臂的行数、类型、冻结时间、开盘时间、结算状态。已有单臂的坏记录同样进入检查。发现重复、零冻结、开盘前结算、时点冲突或有结算无标签时，paired() 标记该父子对 UNKNOWN_INVALID_PAIR_ORIGINAL，记录首个错误序号，其它父子对仍可读。合法窗口保留旧配对原件 SHA 及均值计算，新增 lineageWindowOrdinals、ordinalsMissingOneOrBothFreezes、ordinalsPendingSettlement，并与旧 pairedOpportunities 核和。独立未来资格分母仍按原协议自己登记。新扫描每对最多 5000 个本地序号、最多六组，不在主链写事务执行。

Java 回归从生成的完整 owner 重新编译，使用封存 C10 的已编译依赖、Android JDBC 适配器及 SQLite 驱动，在实际 paired() 入口验证正常、空窗、缺冻、待结算、反向时间、非法重复与相同旧原件摘要。本测试提供方法级宿主证据；设备运算成本、长时行为随该批实际 APK 另验。

正式安装通过仓库修订的批次入口。DiagnosisEvidenceR335 属于 NUMERIC，新的 B05–B08 实际后态必须与各阶段 release/r82_training_invalidation.json、原 r967 计算的 training_rule_sha256 和 r815 源码身份同步核对，继续执行原失效与新未来采集合同。B07 回访和 B08 验签属于离线证据检查，执行本页脚本即可加严签收；两个离线检查均无需修改 Android 授权或资格算法。

`b05_pair_source.py::generate_identity(plan, stage, output_dir)` 保留的是上一版仅有 B05 配对修订时的身份生成与前态核对用途。其固定 B05/B07 R82 结果不包含本轮 B04 和 B07 生产修改，不能作为当前安装目标。当前安装器先识别该旧有效前态，再以 `production/OVERLAY.json` 覆盖为完整累计源码重新生成的 R82/R967 后态；签收只采用本页开头所指的 `production/SEMANTIC_RESULTS.json` 与候选实际文件树复验结果。

修订后的完整文件随仓库提供。[DiagnosisEvidenceR335.java](after_full/app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisEvidenceR335.java)。安装器通过生成器和前后 SHA 采用相同字节，保留到 B08。

## 短链与百期窗口的共同时间约束

`check_research.py::settlement_times()` 由短链 `trace` 和 [runtime/check_window.py](../runtime/check_window.py) 共同调用。两条路径各自使用自己的 manifest 所绑定的冻结数据库，拒绝 WAL、SHM、journal 残留，检查前后 SHA，并核对每条可联接标签的 `settled_at >= max(open_time, first_seen_at)`。晚到的任务 `completed_at` 不能替代标签自身的时间约束；百期 bundle 与 B08.2 短链 bundle 不同也须独立通过。

长窗保留完整未过滤日历及原启用日记。120 个日历期中，100 个启用机会全部闭环且满足时延、另外 20 个机会被原件明确证明停用时，工程窗口可为 PASS：20 个停用期只从启用机会分母排除，不能计为训练、成功结算、命中或自然资格样本。停用期的缺链留在 `explicitlyDisabledTraceFailures`，所有原 trace 失败仍留在 `originalTraceFailures`；启用或无法证明全程停用的缺链仍阻断通过。任何已有标签原件仍接受严格时间检查。分母细则与现场命令见 [runtime/README.md](../runtime/README.md)。

[SETTLEMENT_WINDOW_RESULT.json](../runtime/SETTLEMENT_WINDOW_RESULT.json) 留存 5 个新增窗口用例、8 个研究用例及 3 个既有运行用例的真实通过输出和脚本 SHA。新增用例分别覆盖完整百期通过、重算全部原件哈希后的早标签拒绝、sidecar 拒绝、100 启用加 20 明确停用通过，以及下一机会开始后 1 秒才停用仍按第 101 个启用机会验缺链。它们证明校验器的正反行为，不产生真实设备观察或策略质量结论。
