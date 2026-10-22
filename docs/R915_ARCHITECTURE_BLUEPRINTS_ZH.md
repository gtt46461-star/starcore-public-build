# R915 项目架构与唯一主链图纸

## 软件运行职责

```mermaid
flowchart TD
    A[开奖源与本地历史] --> B[AutoFetchService]
    B --> C[赛前唯一预测主链]
    B --> D[每期开奖持久任务]
    D --> E[唯一 SettlementEngine]
    E --> F[OOS/真实交易/任务终态原子提交]
    F --> G[Research 结算侧路]
    G --> H[状态更新/证据/进化/淘汰]
```

实时出号和结算是 P0；Research 遇到实时压力必须 yield。研究侧路失败不能回滚已完成的生产结算。

## 唯一预测与出手主链

```mermaid
flowchart TD
    A[历史开奖 PIT] --> B[Legacy 原有资产与专家]
    B --> C[完整 1200 动作面]
    C --> D[证书化新资产 ≤8%]
    D --> E[证书化发明算法 Governor]
    E --> F[唯一动作编译合同]
    F --> G[唯一 DecisionGate]
    G -->|BET| H[原子 Trade Writer]
    G -->|HOLD| I[拒绝码与 16 项真值]
```

- Legacy 路径先完整计算，新层无证书或异常时贡献严格为 0。
- 新资产 Mixer 和算法 Governor 只调整评分，不拥有 REAL 权限。
- 只有 `DecisionGateEngine` 能产生授权决定，只有现有原子交易写入链能落 REAL。

## Legacy 与 Genesis 永久隔离

```mermaid
flowchart TD
    A[LEGACY_FIXED 133 Owner] --> B[只读输出桥]
    B --> C[既有主链]
    D[240 L0 原子] --> E[GENESIS_NEW AssetGenome 15器官]
    E --> F[ResearchGenome 16器官]
    F --> G[Future OOS 证书]
    G --> H[限幅增量贡献]
    C --> I[Production 动作面]
    H --> I
```

Genesis 禁止对 Legacy 做 REWRITE、MUTATE、CROSSOVER、PRUNE、REPLACE、RENAME、SCHEMA_CHANGE 或固定权重覆盖。133 个锁定 Owner 中 131 个与基线逐字节相同；仅唯一控制 Owner `DecisionGateEngine` 与 `SettlementEngine` 因本次 P0 修复明示修改并以新交付哈希重锁。它们不是被 Genesis 进化的分析资产。

## 每期开奖的赛前与结算时序

```mermaid
sequenceDiagram
    participant S as 抓取服务
    participant P as 预测主链
    participant DB as SQLite
    participant X as 结算引擎
    participant R as Research
    S->>P: draw(t) 后计算 target(t+1)
    P->>DB: 冻结 Production/算法/新资产赛前记录
    S->>DB: draw(t+1) 到达并创建 durable task
    X->>DB: 原子 claim 当前任务
    X->>DB: OOS + REAL + terminal 同事务
    X-->>R: post-commit 结算侧路
    R->>DB: 状态更新与证据（失败开放）
```

任何 target 的开奖结果都不能写回它自己的赛前输入。没有冻结记录时只能明确排除，不能开奖后重算出一条“预测”。

## 算法与资产创造闭环

```mermaid
flowchart TD
    A[真实 Future OOS 失败] --> B[FailureVector/NeedPlan]
    B --> C[新资产构造或算法结构变异]
    C --> D[结构哈希 + 真实行为探针去重]
    D --> E[下一期赛前冻结]
    E --> F[开奖后状态更新]
    F --> G[多指标证据与反证]
    G -->|有效| H[保留/交叉/证书]
    G -->|无效| I[淘汰/撤证/容量释放]
    H --> A
    I --> A
```

这不是改几个参数：候选拥有自己的观察、表示、持久状态、推断、1200 动作评分、选择/HOLD、不确定度、校准、结算更新、失败模型、反证、资源、证据、血统、解释和证书。

