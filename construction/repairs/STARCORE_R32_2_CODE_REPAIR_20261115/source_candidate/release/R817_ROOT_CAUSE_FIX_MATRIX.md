# R817 根因—代码—验收闭环矩阵

| 优先级 | R816 设备证据 | 直接根因 | R817 施工 | 自动验证 | 物理机关闭条件 |
| --- | --- | --- | --- | --- | --- |
| P0 | OOM=3、lifetime OOM=9、闪退 | 完整 HTTP 正文复制进每条 draw，并由无 projection 历史查询反复装入 | `ApiParser` 单条 JSON；`DrawHistoryProjectionR817` 排除 raw；`DrawRawPayloadCompactorR817` 有界清理旧重复体 | 17 列无 raw 断言；Android 全编译；192 MiB 核心通过 | R817 epoch OOM=0；无 Java heap crash；compactor pending 单调下降 |
| P0 | provider FRESH，但 BusinessTruth `FETCH_STALE` | 同 issue 有效响应不刷新 heartbeat；旧 open time 永久压过新证据 | `recordValidatedFetchResponseR817`；`ProviderTimeReconcilerR817`；provider corroboration | current replaces stale / no regression / historical conservative 断言 | provider、DB、B13 的 issue/age 一致；fetch heartbeat 持续更新 |
| P0 | serviceAlive=1、businessAlive=0 | UI/main 与 Service 业务真值线程和写入节拍不一致 | 独立 liveness HandlerThread；一次事务批量 heartbeat；统一 R817 epoch | 编译与静态路径门 | 锁屏、HOME、划掉任务后 fetch/analysis/settlement 真正推进 |
| P0 | P95=6215ms、卡顿 | 启动播种500、结算大批、诊断、deferred 与分析同时争用 | 低优先级延迟启动；seed64；current/backlog 4+4；audit256；取消启动全诊断 | 96/240 核心 777/996ms 主机通过 | 目标机 current-release P95<=5000ms；无 ANR/持续 GC |
| P0 | 结算长期 PENDING/RUNNING | 大 backlog 和实时核心争用，当前任务缺少公平时间片 | 异步有界 settlement maintenance；唯一 coordinator/engine 不变 | owner scan 与回归通过 | 有合法 predraw canonical 的到期任务最终 terminal；重复重放幂等 |
| P1 | deferred pending=167、formalActions=0 | deferred 仅完全空闲运行且单轮过重 | 120×10、压力时1 job/空闲4 jobs、48MiB headroom、公平重排 | exact 1200、10 slice 断言 | pending 单调下降；目标期 number=100/action=1200；无模型重跑 |
| P1 | Research schema NOT_READY | schema/tick 错误依赖历史 backlog 全空 | schema 30s、tick15s、生产空闲即可运行 | N12 incubator/codegen 断言 | schema READY；候选/代码/实验表出现可审计行 |
| P1 | 生成代码不等于运行 | 没有开奖前 candidate consumer 和开奖后 label | `ResearchShadowChallengerRuntimeR817` + predraw 表 + settlement sidecar | predraw/future-live/REAL=0 静态与 77 断言 | 新未来期 predraw=1；开奖后 settled=1；旧期不回填预测 |
| P1 | FTRL NOT_STARTED/学而不用 | Artifact 更新后没有预测端 caller | `loadForTarget` + `predictResearch`；成熟度混合；完整审计字段 | 动态权重、上限、公式 fallback、0.59 混合断言 | trainedUntil < target；Artifact hash 对齐；下一期 predraw 标记 APPLIED_PIT_FTRL |
| P2 | MODEL_PROVEN=false | 25 个 OOS、无冻结模型/校准/独立证明 | 保持离线深度训练、冻结 Artifact、future-live 收集和晋级门 | REAL lock/owner boundary 通过 | 仅在原证明规则全部满足后讨论模型晋级；R817 不自动解锁 REAL |

## 不可通过“优化”破坏的合同

- 正式动作始终是 `10*C(10,3)=1200`，任何 deferred slice 都只是游标分片。
- SettlementEngine 是唯一生产结算 owner；研究 settlement 只能在其终态后读取结果。
- 动态 Artifact 的 `trainedUntilIssue` 必须严格早于目标期；future backfill learning 禁止。
- OOM 必须保留为 OOM，READ_ERROR 不能显示为零或 PASS。
- 任何设备失败、样本不足或模型未证明都维持 SHADOW_ONLY。

