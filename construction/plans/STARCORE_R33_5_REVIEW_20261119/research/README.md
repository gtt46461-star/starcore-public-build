# B05 至 B08 研究证据与主链核查

本页随仓库修订一起使用。命令中的 PLAN 是两份分包合并后的 STARCORE_R33_5_20261118 目录，REVIEW 是本页目录。先执行原包 verification/verify_package.py。本页的脚本从同一原包读取原校验逻辑，直接读取一致性 SQLite 快照或哈希绑定的原始 JSON。结果归入对应批次的 POS、NEG、TRACE 原件，不另设批次，不授予真实交易权限。

## 施工和签收

| 批次与工单 | 原代码和具体缺口 | 在本批完成的动作与合格条件 |
|---|---|---|
| B05／RE24 | after_full 中 strategyv2/DiagnosisEvidenceR335.java 的 paired() 用内连接统计同时有 hit 的两臂，只检查 settled_at>0、frozen_at<target_open_time；旧库负冻结时间或开奖前结算可进入统计。两报告的 UPGRADE_DIAGNOSIS.json 中 R01、R10、R17 均记 NO_RECORDED_OUTPUT，无法据此推算独立提升。 | 对报告实际展示的每一对 childKey/parentKey，用其 throughExclusive 在同一快照执行下述 paired，复算窗口、均值、有效配对和两臂 ACT。窗口序号分为双臂已结算、至少一臂缺冻结、已双冻待结算，三数之和等于窗口长度。把异常期的原始行留在故障证据，停止将该窗口作为有效配对。报告现有 descriptiveOneSided95NormalLower 只作描述，缺观察、挑选父代、连续相关均不得据此签优质。 |
| B06／RE19 | 033 的 SequentialQualificationR27、CertificateGateR27、QualificationWindowR26 和实际 typed 路由已有实现。R27 的已结算 FUTURE ACT 与 SU2 的固定日历不是同一分母。 | 依 B06/C11_033源码逐项保全审查_R335.md 原序执行独立自然资格。FUTURE 的每期给出 ACT、ABSTAIN 或 MISSING 及源行。R27 保留 250 ACT、HR≥.34 和原序贯/稳定/校准/基准全门。SU2 保留 69120 期及各自校准窗。对同 APK 的合法 BET 在 B06 当批独立观察唯一 writer、证书源、授权、冻结、清算。证书过期、撤销、关闭及无证分别给原拒绝理由；授权关闭时验正常 SHADOW。宿主验收命令如下，不计作已获得自然证书。 |
| B07／研究处方回访 | B07/verify/check_improvement.py::compare_followup 校验首报→施工→复报，但不把实验的原始冻时/开奖时间与施工和复报夹紧。原测试自身可返回“本次试验测得改善”，即使所有 8 个开奖结果都在复报之后。B07/replay_r335/case_loop_r335.py 另外负责真正的 Git 改动和报告身份，保留其不同用途。 | 先运行原 case_loop 的 extract、followup，再在采用处方实验结论时执行下述 followup。原预登记可以早于施工；两臂实际预测要同时晚于施工及注册，开奖不晚于复报观察。非法或缺失时间不能签“测得改善”。显著结论限这个预登记比较；批量筛选、重选窗口、未留所有失败试验的情形保留探索状态。 |
| B08／D06、B08.2 | verification/mainline/check_trace.py::trace 已约束预测早于开奖与首次观测，但 OOS label 仅检查 settled_at>=first_seen_at。若源端提前可见，重算原有全部哈希后，早于 open_time 的标签仍能令原校验返回 PASS。 | 以原 B08.2 的同一个 trace manifest、证据根、候选 commit 与 APK SHA 执行 trace。先复跑原 INTEGRATED 原件联接，再逐机会验证 settled_at 不早于 open_time 和 first_seen_at 两者。发现异常应拒签 B08.2，保全源时钟、看见时刻和实际事务时间供收集者解释。该短联调与 B08.4 真机长测各自沿用原日历。 |

在施工试验前运行以下回归：

```bash
python3 -B "$REVIEW/test_research.py" "$PLAN"
python3 -B "$PLAN/B06/host/run_host.py" --out "$OUT/b06-qualification-host"
```

在相应原件产生后，分别执行以下命令。变量取自现场证据；OUT 为本次输出目录。paired 的通过只表明原始行可复算，要将输出与被验报告对照。任何输入缺失都记在所属批次与目标期下。

```bash
python3 -B "$REVIEW/check_research.py" paired "$SNAPSHOT_DB" "$CHILD_KEY" "$PARENT_KEY" "$THROUGH_EXCLUSIVE"
python3 -B "$REVIEW/check_research.py" followup "$PLAN" "$CHAIN_JSON"
python3 -B "$REVIEW/check_research.py" trace "$PLAN" "$TRACE_JSON" "$EVIDENCE_ROOT" "$SOURCE_COMMIT" "$APK_SHA256"
```

CHAIN_JSON 使用原 B07/verify/check_improvement.py::compare_followup 的 firstCase、work、nextCase 引用合同。若施工后还没有新的预登记实验，维持缺证结论。两个原报告只作施工前现场指标，不能用作本轮施工后的复报。SQLite 的窗口测量仅针对原候选本地序号，由独立日历与冻结记录负责业务机会分母；行数为零时均值为空。报告来源缺少该一致性库时，填写待采集原件，不补造数据。

## B05 应用源码的修改

B05 新编译后态由本目录 b05_pair_source.py 从 SHA-256 为 31fdbec19d5ae3aba21a727bda2b6cc06f14931b73c3683bfb545cfa68c3563a 的封存完整源码生成。对应安装路径为 app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisEvidenceR335.java，修订后 SHA-256 为 640bd8c8c0d45d8ff4a2b9f6ed1dcb0117c7bec619470dc025c4e43a8f293249。每个输入/输出都按完整字节比对。需要审阅后态时可执行：

```bash
python3 -B "$REVIEW/b05_pair_source.py" "$PLAN" "$OUT/b05-candidate"
python3 -B "$REVIEW/test_b05_java.py" "$PLAN"
```

新 pairWindow() 在同一冻结快照上逐序号检查两臂的行数、类型、冻结时间、开盘时间、结算状态。已有单臂的坏记录同样进入检查。发现重复、零冻结、开盘前结算、时点冲突或有结算无标签时，paired() 标记该父子对 UNKNOWN_INVALID_PAIR_ORIGINAL，记录首个错误序号，其它父子对仍可读。合法窗口保留旧配对原件 SHA 及均值计算，新增 lineageWindowOrdinals、ordinalsMissingOneOrBothFreezes、ordinalsPendingSettlement，并与旧 pairedOpportunities 核和。独立未来资格分母仍按原协议自己登记。新扫描每对最多 5000 个本地序号、最多六组，不在主链写事务执行。

Java 回归从生成的完整 owner 重新编译，使用封存 C10 的已编译依赖、Android JDBC 适配器及 SQLite 驱动，在实际 paired() 入口验证正常、空窗、缺冻、待结算、反向时间、非法重复与相同旧原件摘要。本测试提供方法级宿主证据；设备运算成本、长时行为随该批实际 APK 另验。

正式安装通过仓库修订的批次入口。DiagnosisEvidenceR335 属于 NUMERIC，新的 B05–B08 实际后态必须与各阶段 release/r82_training_invalidation.json、原 r967 计算的 training_rule_sha256 和 r815 源码身份同步核对，继续执行原失效与新未来采集合同。B07 回访和 B08 验签属于离线证据检查，执行本页脚本即可加严签收；两个离线检查均无需修改 Android 授权或资格算法。

配套的 generate_identity(plan, stage, output_dir) 生成两份阶段 R82 全文件中的适用一份。B05/B06 完整 R82 文件 SHA-256 为 4cd79138d64f8a48499a8a08ad7294bae300c44dde8c5bc351add01e6dee4563，对应新 training_rule_sha256 为 c2a0e780ccb17094a7177f37ea89b39aa67776de84ee32f7a60df2b23f49e9ba。B07/B08 分别为 b58acd80066dea55828d5f86852e21cc2bac0144c7d527a6b5dbd0e3526be7ad 与 608a816d468acbac49987d4db072dbed3a12b4202a3db53a1273f81f2ef45941。两种身份均使用原 build_semantic_inputs_r335.py::identity() 和 033 的 r967_semantic_identity.py::build() 在完整受控源码集重算；policy_hash 沿用原协议，兼容旧规则列表为空。固定字节生成只用于复现该合成结果，最终候选仍由原构建工具从实际文件树复验。

修订后的完整文件随仓库提供。[DiagnosisEvidenceR335.java](after_full/app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisEvidenceR335.java)。安装器通过生成器和前后 SHA 采用相同字节，保留到 B08。
