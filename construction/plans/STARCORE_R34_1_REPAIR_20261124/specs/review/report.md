# 检测原件复核与九层 K3 诊断施工要求

本次附件与原 R34.1 方案登记的是同一份检测包，外层 SHA-256 为 `2b984d2b249f54e4363f7c02c5d2470e7b9393e6f3994b24e25bcead3e44c37d`。内包 53 项，独立复算的报告正文、升级诊断和归档清单哈希见配套覆盖索引。设备为 V2353A、运行 API 34，安装版本为 R34-B12-2026-11-24-577311122，源码提交为 `5562f9b19aa09f9f7d9e69ca085db56c537967fc`。该作业与上一个成功快照的源码或定义不匹配，报告注明可比独立时段增量为 0。施工后的业务改善须用新安装的同角色、同指标版本、匹配的机会日历验收。

## 逐项闭合方式

| 检测入口 | 本次读数 | 施工后用何种原件复查 |
|---|---|---|
| R31_SEMANTIC_RECEIPT 与 catalog 的 check_result | 96 项都执行。其中 4 PASS、4 FAIL、7 WARN、21 NOT_PROVEN、44 OBSERVED、16 N/A | 按注册 ID 对读语义回执与目录结果，给每项列责任工单与实际原件。RSEC-052、074、078 中赛前覆盖缺失共享业务事件，按角色、目标、版本去重。 |
| R30_REPORT.metricResults 与 diagnosis.findings | 44 项中 34 INSUFFICIENT，5 RECORDED_UNVALIDATED。另有 M05 持久观察缺失、M23 BLOCKED、M32 来源缺失、Q02 未部署、Q08 合成来源过期。16 条诊断规则仅 WRITER_HARD 标记 DETECTED，8859 为快照内累计记录。 | 固定 metricId、观测期、分母、级别与数据来源。M23 的 81/82 是当前报告进程对代码入口的观察，受阻的 F23 为 F23_ORIGINAL_TEACHER_NOT_BOUND，须按真实候选及教师身份追运行侧消费。 |
| fieldFollowupR34.sources | 9 项。2项读回，3项新表不存在，4项表可查但行集为空。最新开奖 202611240137，R34 S0 的133–137五期已结算，5期全部未命中，138已冻结。 | 分别带查询状态、表/列、窗口、角色、实际分母验收。旧影子严格统计为254/887，当前主链对已开奖132–137有效冻结仅133；独立未来、合格实验、有效证书均为0。四组数值分别展示。 |
| capabilityRepairR32 | 165个对象分别有 caseKey、occurrenceId 与首个已知缺口。8项能力需求中1项已声明并有运行阻塞，7项声明存在、运行链待证。 | 以 group+id 对接既有对象契约，取真实来源、实际动作、前后哈希及所属批次的工程用例。165个索引含20对矩阵别名，避免重复统计。无输入时保留缺口、可恢复条件及下一次采集位置。 |
| runtime-observation 与 catalog 的 RSEC-052 | 1000ms 的当前单调调度目标共有316个槽、200次唤醒、117次 missed tick，其中103次上轮超时/正常调度、13次 HTTP 失败或退避、1次会话启动。24/72小时未实测。两次成功返回的 P95 为1370ms，已漏失目标另计。 | 用起讫期、session/boot、实际抓取、开奖落库、赛前冻结和结算的时间戳对账。早唤醒、短样本与网络退避分别记；线程唤醒次数不能替代 HTTP 频率和全机会业务 SLO。 |

## RSEC-066 暴露的具体问题

在上述提交的 `com/ethk3/novaonepro` 包内，`K3FailureDiagnosisEngine.diagnose` 与 `diagnoseSnapshotCP4` 读取三个 V51 工程产物的编译常量。三个产物当前都声明 `DATA_SHA256=UNTRAINED`、`PROVEN=false`；动作排序另有 `RANK_ORDER_PASSED=false`，选择后校准的支持数为零。另六层读取字符串，`stateLayer` 对非空且没有特定风险词的文本默认判为 HEALTHY。报告读取链中的 `LiveResearchDiagnosisOrchestrator.collectRanker`、`FormulaAlgorithmAuditEngine.audit`、`CodeOwnershipCompletenessAuditEngine.audit` 及快照对应入口也使用静态标志。新模型即使在 R27/R34 内成功发布，这些读法仍会显示旧标志；任意无风险词的字符串则可能产生健康假象。

| 模块 | 源码文件及已核 Git blob | 在本批直接交付的约束 |
|---|---|---|
| 九层诊断 | K3FailureDiagnosisEngine.java · `70244ff822a12b7bd5f93e4f53b6831075bc768b` | 实时和快照走同一只读纯评估，结构化证据关联同模型、目标、窗口、角色；任意文本、旧标签、错模型或来源失败取 UNKNOWN/BLOCKED 并列明原因。 |
| 排名与审计消费 | LiveResearchDiagnosisOrchestrator.java · `37e910085de1781e2274e757683d39dbd587e62f`；FormulaAlgorithmAuditEngine.java · `d3cf878f344c7264f3a30ad94827b336656a9c8b`；CodeOwnershipCompletenessAuditEngine.java · `d9ee48812d4e5ccf8c8cd438e4cea84865be5c83` | 将 RSEC-058、066、069、070、071 对同模型的证明字段对齐。保留编译种子身份；在线模型按来源另行评估、独立展示。 |
| 三个编译占位 | V51NumberModelArtifact.java · `a183fb93db91e32d06147eb295ce436d0f9b96b6`；V51ActionRankerArtifact.java · `8d1259904e20fd72ca77566093c527f3f448275d`；V51SelectedCalibrationArtifact.java · `8cb47fc98467c457805bade6d69a57021e73bcaa` | 保持占位产物如实记录。禁止通过手改 PROVEN、RANK_ORDER_PASSED、支持数或写摘要字符串使检查变绿。 |

按以下数据顺序落实，每项随本批的 ENGINEERING 用例使用隔离数据验收。自然优势、上线资格和真机持续运行继续使用其各自的独立原件。

1. **B07** 在概率面与正式动作账本写入点绑定被冻结的数据、模型、选择策略和目标期。保存同一赛前排序、确定性平手处理与唯一完整 1200 动作，开奖后按期聚合 top-k recall、Rank1/MRR/NDCG、成对顺序。如延后证据尚未齐全，返回待核状态，核心冻结、结算照常进行。
2. **B08** 在第三/四名替换、固定窗口、状态切换、模式支持、跨位边和专家留一六个试验中分别保存当时可用输入、消融定义、结果、失败原因、实际动作与重试键。逐层的源状态键、原始 finding hash 和验收要求见覆盖索引的 k3_layers，允许反证显示“无改善”。
3. **B09** 对同一冻结选择规则和独立时间窗计算选择后校准及分组区间。正式 ACT 按唯一自然目标计数。重复同一期动作、未来标签或其他模型的校准原件必须被拒绝。固定对照、统计预算和门槛承接现有 B09 合同。
4. **B11** 验收现场构造的正、负、未知三类读取。相同原件在实时页和冻结报告给出同类结论，旧种子、新模型、S0 和 REAL 独立定位。96个已执行检查、165个对象可追溯、12章完整分别属于文档覆盖证据。新版本签收需查实际原件和模型消费。
