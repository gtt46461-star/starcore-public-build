# 《算法创造方案 V4.1》审查与落码映射

## 方案结论

原方案的主方向正确：冻结 Legacy、只新增 Genesis、资产 15 器官、算法 16 器官、Future OOS、证书化有界影响、撤证回滚。施工中补了四个原方案未写到代码级的关键边界：

1. 新原子不能靠数据库热激活，否则会改变 PIT 历史语义；合法提案必须停在 `AWAITING_SIGNED_SCHEMA`，进入下一同签名构建后才可执行和收集 Future OOS。
2. 移动端不能让数百候选同时逐个构建 1200×240 上下文；active 上限为 8，同一期观察共享，只对入选动作物化高维输出。
3. FDR 必须以明确 null 和候选家族规模计算 q 值；不能用 Wilson 的补数伪装。
4. 编译 PASS 不是交付完成；源码实质性、SQLite 崩溃回放、APK 签名/身份、以及实体机 200 期覆盖必须分开验收。

## Stage 逐项落码

| 方案阶段 | 实现 Owner | 完成口径 |
| --- | --- | --- |
| Stage 0 Legacy Freeze | `LegacyFixedAssetRegistryR915`、`LegacySourceLockManifestR915`、`LegacyIntegrityGuardR915`、`LegacyPredrawContextBridgeR915` | 133 个锁；131 与基线逐字节相同；2 个唯一控制 Owner 明示修复并重锁；mutation/write/evolution 违规为 0 |
| Stage 1 Truth/Capture/Settlement | `AutoFetchService`、`SettlementTaskStoreR36`、`BusinessReconcilerR36`、`SettlementCoordinatorR815`、`SettlementEngine` | 每个抓取 issue 任务化；NULL 租约恢复；当前/欠账公平；OOS/REAL/terminal 原子提交 |
| Stage 2 Atomic World | `GenesisSchemaRegistryR915`、`AssetGenesisEngineR915` | 12 组 240 原子、72 家族、类型/PIT/泄漏/更新时间；新原子独立只增提案账本 |
| Stage 3 New Asset Genesis | `GenesisAssetGenomeR915`、`GenesisAssetVmR915`、`AssetGenesisEngineR915` | 15 器官、真实多形状输出、状态、赛前冻结、exactly-once 更新、证据、证书、淘汰 |
| Stage 4 Mechanism Discovery | `FailureVector`、`NeedPlan`、行为探针、中文命名器 | 由真实 OOS 失败选家族/结构，结构哈希和 40 步执行行为哈希去重，中文名来自机制 |
| Stage 5 Full Algorithm Lifeform | `AlgorithmGenomeR908`、`AlgorithmGenomeVmR908`、`ResearchGenomeContractR915` | State/Score/Update 与 16 器官合同；不是表达式或参数就冒充算法 |
| Stage 6 Dynamic Evolution | `AlgorithmInventionLabR908`、`AlgorithmEvolutionLifecycleR912`、`AssetGenesisEngineR915` | 24 个 settled issue 一批；变异/交叉/容量淘汰；子代 N=0；最多 8 active；垃圾自动剪除 |
| Stage 7 Certificate/Mainline | `AlgorithmEvidencePolicyR912`、`AlgorithmProductionGovernorR915`、`CertifiedResearchMixerR915` | N、paired、FDR、多指标全部通过；新资产≤8%；Research 无 REAL 权限；撤证贡献归零 |
| Stage 8 Runtime Stability | 看门狗组、实时压力门、共享观察/compact evaluate | P0 优先、Research yield、sidecar 失败开放、避免 1200×240×候选重复分配 |
| Stage 9 Truth UI | `RuntimeTruthSnapshotR906`、`ModelLearningActivity`、解释 Renderer | 16 状态框、中文名、每项证据、Legacy 四重锁、新原子提案、贡献与拒绝原因 |

## AssetGenome 15 器官

| 器官 | 真实实现 |
| --- | --- |
| AtomicProvenance | 原子索引、PIT 来源和结构哈希 |
| AtomContract | numeric/type/leakage/updateTiming 合同 |
| CompositionGraph | 内部程序和非线性/分流/交互结构 |
| OutputSemantics | 输出类型、形状、中文机制摘要和值哈希 |
| PersistentState | 独立状态槽、更新次数和训练截止期 |
| UpdateTiming | 赛前只读、开奖后更新、严格 issue 顺序 |
| QualityModel | primary/confidence/stability/risk |
| FailureModel | creationReason 与失败向量 |
| Falsification | Wilson/块/域/校准/配对/FDR/连挂 |
| ResourceProfile | active≤8、预算、compact 扫描、P0 yield |
| Lineage | generation、parentIds、mutation |
| EvidenceLifecycle | N/H/HR、Brier/ECE/Coverage 等持久证据 |
| PromotionState | IDEA/OBSERVING/CERTIFIED/PRUNED/REVOKED |
| Explainability | 结构驱动中文名和机制说明 |
| LegacyBoundary | namespace=GENESIS_NEW、mayAuthorizeReal=0、Legacy 不可写 |

## ResearchGenome 16 器官

Observation、Representation、PersistentState、Inference、ActionScorer、Selectivity/HOLD、Uncertainty、Calibration、SettlementUpdate、FailureModel、Falsification、Resource、Evidence、Lineage、Explainability、Certificate 均进入 canonical contract 和 contract hash。缺失合同、证据不足、trainedUntil 不在 target 之前、行为克隆或越权标志都会被生产 Governor 拒绝。

## 中文命名不是包装

名字由状态记忆、条件分流、跨位迁移、遗漏压差、非线性、反馈更新、输出语义和 generation 组成，例如“玄策·迁移记忆分流非线性算法·第09代”。结构哈希不同但执行行为等价的候选仍会按 behavior hash 合并；只改参数或名字不能形成新家族。

## 证据门

晋级至少需要：Future OOS N≥240、paired mainline N≥120、命中率和 Wilson 下界、Coverage、BlockMin、RegimeMin、Brier、ECE、MaxMiss、paired delta、增量信息、冗余和 FDR 同时通过。训练、历史回放或后验挑选都不计 Future OOS。

## 尚需实体机证明的项目

- 连续 200 个自然未来期赛前冻结覆盖率 ≥98%；
- 可结算冻结的结算覆盖率 ≥98%，duplicate update=0；
- 普通返回桌面/划走任务后的业务游标持续前进；
- P95、OOM、电量、DB 增长和厂商省电策略影响；
- 真正产生证书前，UI 必须显示“未证明”，不能把代码能力写成已达到 70%–80%。

