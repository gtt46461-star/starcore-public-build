# B01–B15 工程闭环摘要

逐文件 114 条原方案与实际行数、字节、方法、引用、决策点和 SHA-256 的对应关系，见 `R361_B01_B15_CONTRACT_TRACEABILITY.csv`。本文件只列每批主闭环与验收点。

| 批次 | 主闭环 | 唯一 Owner/核心 | 本次关键验证 |
|---|---|---|---|
| B01 | 身份、期号、输入合同 | AppConfig / TargetIssueResolver | 包名、版本、跨日 1440→0001 |
| B02 | Schema 与冻结字段合同 | DbHelper / SchemaContractRegistry | DB_VERSION=36、迁移/表字段编译 |
| B03 | 单写者与原子事务 | DatabaseCoordinator | single writer、幂等、回滚 |
| B04 | PIT 输入与快照 | PointInTimeSnapshot / V51RuntimeInputR36 | 目标期开奖前冻结 |
| B05 | 窗口/状态/轨迹 | V51Window* / V51State* | 窗口证书与无标签泄漏 |
| B06 | Motif/Relation/Permutation | V51Motif* / RelationGraphVerifier | 预开奖边界、跨块证书 |
| B07 | Service、恢复与守护 | AutoFetchService / BusinessReconcilerR36 | 恢复只启动并 reconcile |
| B08 | 证据隔离与延后物化 | DeferredEvidenceWriterR36 | 模型调用=0、版本化 gzip+SHA |
| B09 | 号码模型与专家门 | V51NumberModelArtifact / ExpertGate | 工程与模型证明分离 |
| B10 | 1200 动作、K3 漏斗、Ranker | K3ActionSpaceEngine / K3FunnelOrchestrator | 1200 唯一、9→8→7→6→5→4→3 |
| B11 | 选择后校准/风险证书 | SelectedHitCertification / SelectivePolicy | Wilson/ECE/Brier/OOS 硬门 |
| B12 | 决策授权与账本一致性 | DecisionGateEngine_ONLY | 单期最多一个 REAL，当前硬锁 |
| B13 | 后台恢复、发布与回滚 | ServiceWatchdog / Artifact Promotion/Rollback | 原子晋级、失败关闭、设备待复测 |
| B14 | 失败树、消融、Owner、改进 | LiveResearchDiagnosisOrchestrator | 14 阶段、Finding DAG、确定性 Hash |
| B15 | 完整报告、UI、复制、发布 | LiveDiagnosticReportEngine / V57 report | 18 章、全文/摘要/JSON/分段、DB 持久化 |

工程门全部通过不等于模型效果与真机 24 小时已经通过。B09/B11 的冻结模型证书和 B13 的物理设备矩阵仍然是独立发布证据，当前不能解锁 REAL。
