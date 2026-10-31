# R30-B02-04 事务体审查（准备在事务外、冻结在短事务内）

基线 bb607cf 静态审查结果（行号为施工时位置）：

| 路径 | 事务外（准备） | 事务体内（冻结） | 结论 |
|---|---|---|---|
| 正式主链 `V51RuntimeOrchestrator` → `V51Repository.commitCanonicalBundleVerifiedR368` → `DatabaseCoordinator.runCanonicalTransactionR36` | 特征、状态、号码模型、共识、漏斗、动作、选择、授权、`PredictionOutputContractR815` 校验、`DeferredEvidenceSnapshotR36.encodeRuntimeResult` 全部在 `DB_CANONICAL_WAIT` 之前完成 | `assertPreDrawR36`（同一目标期/赛前校验）+ 写 issue/champion/selected/授权/交易/审计/canonical/deferred job 行；调用到的非写方法仅 `K3ActionSpaceEngine.globalOrdinal`（索引算术）与 `CertifiedPredictionR974.beforeInsert`（校验） | 符合，无模型/特征调用 |
| 研究观察冻结 `LiveShadowsR27.freeze` | `ReplayEvaluatorR27.predictNext` 在 worker 线程逐策略计算 | Batch 只含 `INSERT OR IGNORE r27_live_frozen` / `r27_opportunity` 与 before/after 截止检查 | 符合 |

grep 规则（审查清单）：`runCanonicalTransactionR36` 回调体与 `ResearchWriteQueueR27.Batch` 的 before/after 中不得出现 `predict`、`Model`、`Feature`、`Engine.compute`、`train` 调用。

时间线：B02-01 已在 `runCanonicalTransactionR36` 包装中写 NEXT_PREPARED（进入事务前取时刻）与 NEXT_FROZEN。prepare_to_frozen P95≤200ms 与 M07 是否变差只能由真机 r30_issue_timeline / r30_writer_hold 判定：NOT_RUN。
