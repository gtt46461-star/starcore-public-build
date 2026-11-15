# R817 文件级施工清单

总包的 `SOURCE_ASSETS/` 含完整工程；以下仅列 R817 直接新增或修改的关键文件。

## 新增生产文件

- `DrawHistoryProjectionR817.java`：固定历史投影，排除 `raw_json`。
- `DrawRawPayloadCompactorR817.java`：小批次清理旧超大重复 payload，保留业务身份和证据。
- `ProviderTimeReconcilerR817.java`：当前同一期 stale 时间安全自愈。
- `RuntimeReleaseEpochR817.java`：R817 current-release 健康统计边界。
- `ReleaseIdentityR817.java`：唯一 R817 身份与红线。
- `ResearchShadowChallengerRuntimeR817.java`：exact-1200 开奖前 Research 执行、FTRL 消费、冻结和开奖后标签。

## 关键修改

- `ApiParser.java`：每条 draw 只保存自身 JSON，完整响应不再 N 倍复制。
- `DbStore.java`：所有生产历史查询使用 R817 projection；R817 metadata reconcile/epoch。
- `AutoFetchService.java`：独立 liveness 线程、批量 heartbeat、同 issue fetch receipt、provider corroboration、有界启动/结算/deferred/compaction/research 调度、OOM lease 释放。
- `AppConfig.java`：deferred chunk=120，R817 identity。
- `DbHelper.java`：R817 predraw 表及公式/FTRL 审计字段；同 DB_VERSION=36 的幂等增量修复。
- `R811SchemaIntegrityVerifier.java`：Research 完整性要求 R817 predraw 表。
- `V51RuntimeOrchestrator.java`：production canonical 提交后触发 Research predraw，不影响 production 成功状态。
- `SettlementCoordinatorR815.java`：唯一生产结算终态后触发 Research label。
- `ResearchAdaptiveLearnerStoreR811.java`：FTRL 状态批量持久化；供 R817 严格 prior-target 消费。
- `AutonomousResearchEvolutionEngine.java`：schema/tick 与无关历史 backlog 解耦，生产压力下仍 yield。
- `StrategyChallengerFactoryEngine.java`：N12–119 仅公式 incubator；N120 前禁止 learned candidate。
- `CandidateCodeSynthesisEngine.java`：区分可运行 DSL 源码和必须离线 Artifact 的模型源码。
- `LiveDiagnosticReportEngine.java`：R817 runtime/memory/research/predraw/FTRL 真值与 V13 报告。
- `RuntimeHealthLedgerR36.java`、`ModelEffectivenessAnalyzerR35.java`：R817 current-release scope。
- `BuildIdentity.java`、`V51BuildManifest.java`、`V51AutoFetchBridge.java`、`MainActivity.java`、`V57CompleteDiagnosticReportEngine.java`、`LiveResearchDiagnosisOrchestrator.java`：统一 R817 身份和诊断边界。
- `ReleaseIdentityR815.java`、`ReleaseIdentityR816.java`、`RuntimeReleaseEpochR816.java`：兼容 facade 委托 R817 唯一身份/epoch。
- `app/build.gradle`、`release/release_identity.properties`、BuildConfig stub：版本升级到 817/577190826。

## 验证与构建入口

- `verification/r817/R817RuntimeResearchClosureHarness.java`：77 项专项断言。
- `verification/r811/R811HostContractHarness.java`：N12 incubator 与 codegen 状态回归。
- `verification/r816/R816RuntimeClosureHarness.java`：120×10 deferred 合同。
- `scripts/r817_behavior_gate.sh`：R817 官方编译、全回归、性能和低堆门。
- `scripts/r817_build_release.sh`：Java17/D8/AAPT2/zipalign/同证书签名构建。
- `scripts/r817_generate_provenance.sh`：R817 源码/资源/工具链溯源。

## 明确没有做

- 未清空数据库或删除 lifetime OOM/失败证据。
- 未删减 100/1200、未改为 K4/dynamic K、未增加多位置 REAL。
- 未增加第二个分析、BET、settlement、OOS 或 deferred owner。
- 未在开奖后补预测，未让研究 Artifact 写入 production decision。
- 未把私钥、密码或真实 API key 放入源码或总包。
- 未把主机验证伪装成物理设备或模型效果证明。

