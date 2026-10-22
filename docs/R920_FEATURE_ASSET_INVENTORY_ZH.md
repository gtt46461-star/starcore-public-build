# R920 功能、算法与资产清单

## 项目规模与实质性审计

- Java：523 个，402,989 行，16,361,413 个 UTF-8 字符。
- 空文件：0；注释壳：0；无 Java 类型：0；未解释的短具体 Owner：0；TODO/FIXME：0。
- 分类：245 个实质 Owner、125 个聚焦组件、45 个数据/合同类型、30 个窄适配/工具、15 个接口、
  44 个枚举、6 个兼容门面、7 个 fail-closed 未训练工件、6 个异常合同。
- 不能用“每个文件必须几千行”作为质量门：例如接口、枚举和不可变 DTO 几十行就是完整职责；把它们
  灌到上千行会制造重复和分叉。审计要求是具体 Owner 有真实行为、状态/DB/调用入口/测试证据。

## 关键实质 Owner 规模

| 文件 | 行数 | 职责 |
| --- | ---: | --- |
| `MainActivity.java` | 9376 | 首页、真值、交互、启动保护和轻/重刷新 |
| `SettlementEngine.java` | 3045 | 唯一原子结算 Owner |
| `AssetGenesisEngineR915.java` | 840 | 新资产搜索、冻结、结算、证据、生命周期 |
| `AlgorithmGenomeR908.java` | 777 | 状态/评分/更新程序、结构变异、中文机制命名 |
| `AlgorithmInventionLabR908.java` | 684 | 失败驱动搜索、行为去重、赛前捕获、结算与持久化 |
| `RuntimeTruthSnapshotR906.java` | 698 | 同一数据库快照、16 状态框、完整研究/REAL 真值 |
| `GenesisAtomicWorldR920.java` | 797 | 240 具名原子的 PIT 时间/拓扑/统计/风险/动作语义与缓存 |
| `GenesisAssetVmR915.java` | 523 | typed 资产程序执行、状态和多形状输出 |
| `AlgorithmEvolutionLifecycleR912.java` | 458 | Future OOS 多指标保留、淘汰、制证、回滚 |
| `GenesisAssetGenomeR915.java` | 423 | 15 器官资产生命体合同 |
| `ResearchTinyDeepLearnerR909.java` | 288 | 7→16→8→1 前向与严格结算后在线更新 |
| `CertifiedResearchMixerR915.java` | 222 | 已证书新增资产限幅 8% 贡献，禁止直接 REAL |

## 既有固定资产

趋势、形态、状态、走势、号码属性、号码关联、位置关联、多号码多位置交叉交集、量子/因子组合、
Teacher、推理、反推、智能研究、因果与反哺矩阵仍在 Legacy 固定路径；Genesis 只读使用，不能自动
改写、删除、淘汰或替换。133 个锁定 Owner 的逐文件哈希审计随包提供。

## 新增 Genesis 资产层

- 240 个 L0 原子，分 A0–A11 十二组。
- 72 个搜索导航家族，覆盖趋势/轨迹、形态/母题、状态/区制、号码/位置关系、Teacher/推理、机制/
  反证、概率/风险/稳定、动作几何和元研究/运行。
- 205 个原子允许赛前计算；20 个 A9 开奖后证据只能用于生命周期；其余按阶段和合同使用。
- AssetGenome 包含 15 个器官，并能输出 scalar/vector/matrix/graph/tensor/sequence/state/residual/
  evidence/risk 等语义；候选必须真实执行、冻结、结算和被否证，不能只有中文名称。

## 算法、深度学习与动态进化

- AlgorithmGenome 拥有独立状态、score program、settlement update program、条件分流、非线性、
  交互、校准/风险、失效条件和资源合同。
- TinyDeep 是实际 7→16→8→1 网络，不把“深度学习”当 UI 字样；每个标签只在结算后更新，
  `trainedUntil < targetIssue`。
- FailureVector 有七类结构性变异意图；每 24 个已结算 issue 可形成一代时间片，最多 8 active，
  行为等价去重，子代从全新 Future OOS 开始。
- 每个算法的中文名和机制说明由可执行结构生成；结构/行为双哈希阻止改名和微调冒充新家族。
- 制证是多指标联合门；无证书不影响主链，撤证后贡献归零，研究无直接 REAL 权限。

## 成品 APK 资产

- 最终公开资源：275 项；Dex R 字段：275 项；偏差 0。
- 源码 R 引用：86 项，全部存在。
- 首页启动绑定：45；动态解释行绑定：6；全部类型匹配。
- 关键运行类：28/28；最终 Dex 包含赛前 Asset/Algorithm/TinyDeep/语义原子接线和开奖后 Algorithm/Asset/
  Teacher 接线。
- APK SHA-256 以 `08_CHECKSUMS/SHA256SUMS.txt` 为最终总包真值。
