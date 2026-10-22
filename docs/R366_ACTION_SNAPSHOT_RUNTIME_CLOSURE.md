# R3.6.6 动作快照与 canonical 提交闭环

## 已确认的故障链

| 现场/源码证据 | 确定根因 | R3.6.6 修复 | 回归 |
|---|---|---|---|
| `FAILED_TERMINAL`、`R36_ACTION_INVALID` | 快照错误要求 `n4==0` | n4 由 `K3ActionSpaceEngine.validFourthBoundary` 唯一校验 | T45 全1200行 |
| 生成器 `bestExcluded()` 返回 1–10 | n4 是边界证据，不是第4个投注号 | 固定 K3 身份仍仅为 n1/n2/n3，结算语义不变 | T28/T45 |
| canonical 前编码失败、账本全零 | 快照位于计算完成与原子提交之间 | 对实际运行结果执行 fromRuntimeResult→encode→decode | T45 |
| 生产哈希与快照临时哈希算法不同 | 修 n4 后会继续哈希失败 | 两端共用 canonicalPayloadRow 和 actionSpaceHashFromPayloadBody | T45 哈希相等 |
| 顶层显示 WAITING、底层 ANALYSIS_FAILED | 等待状态覆盖失败真相 | 明确失败优先于 WAITING_CANONICAL | Java17 静态门 |
| Finding 读取另一目标期 | B13 最近事件期可能与报告目标不同 | 失败树优先读取报告 `target_issue` | 诊断一致性门 |

## 动作语义

正式玩法始终是单位置固定 K3：动作身份为 `(position,n1,n2,n3)`，共有
`10*C(10,3)=1200` 个。`n4` 只记录前三号码之外的最高边界候选，用于 margin、诊断和延后证据；它不参与
投注身份，不进入结算命中判断，也不构成 K4。

共享验证合同要求：n1<n2<n3 且均在 1–10；n4 在 1–10 且与前三个号码不同；rawActionP 在 0–1；
rank 必须形成 1–1200 的完整排列；fingerprint、actionHash、recordHash 均为 64 位 SHA-256。

## 原子提交前完整回归

T45 不再只验证“数组长度为1200”。它使用 24 期合法 PIT 历史执行特征、状态、号码面、共识、固定 K3
枚举、漏斗、meta 和排名，随后构造真实 `V51RuntimeResult`，生成包含 100 NumberRow 和 1200 ActionRow
的版本化 GZIP 快照，再解码并复算动作宇宙哈希。任一 n4、rank、概率、指纹、记录哈希、重复身份或哈希
漂移都会使构建失败。

## 覆盖安装桥

R3.6.6 可对 repair_version 为 R3.6.5 的 EXPIRED/FAILED_TERMINAL 行执行一次重置，但仅限目标尚未开奖、
无 canonical、无活动内存 Owner。R3.6.6 自身终态不会循环 rearm；已开奖目标仍禁止事后补算。

## 不变红线

DecisionGateEngine 和 SettlementEngine 的唯一 Owner 不变；SHADOW 与 REAL 统计分离；MODEL_PROVEN=false；
REAL 自动解锁关闭；1200 正式动作不得删除或剪枝；诊断线程只读。
