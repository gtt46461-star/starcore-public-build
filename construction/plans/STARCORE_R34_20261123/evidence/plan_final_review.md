# R34 最终方案独立复核

审查日期：2026-11-23。审查对象：`repo/construction/plans/STARCORE_R34_20261123` 的最终施工合同与局部代码材料；实际源码基线为 `273641e838e0b8dd842b42a67a674e184942a30a`（095）。本文件是独立审查意见，未修改根方案、应用源码或其他审查者的 payload。

## 1. 结论与适用范围

**当前复核范围内，没有发现必须阻止“R34 施工方案”交付的新增确定性阻断。** 初版的唯一 REAL 键、前后批生产者依赖、反馈投递与数值训练混算、在线/冻结身份、授权范围和旧要求承接等主要问题已写入明确合同。B06 参考代码此前找到的实际 source 误删和任务进度登记断口也已修正，并完成独立宿主复跑。

这个结论只允许把材料称为**已审查的施工方案及局部参考实现**。它不等于 66 项应用改动完成、整个 Android 编译通过、APK 发布、现场故障解除或自然质量达标。最终范围文件目前列出 13 批、66 个 R34 问题、66 个继承要求及 150 个文件卡；其中仍有 128 个 `METHOD_CONTRACT_REQUIRES_IMPLEMENTATION` 条目。文件类型与未完成状态已经公开，不能把文件卡数量解释成已修改源码数量。精确数量以封存后的 `FINAL_SCOPE.json` 为准。

复核机器记录在 `analyses/plan_final_review_facts.json`，其中保存所读关键文件的 SHA256。根方案仍在并行封装；本复核不替代最终 `verify_plan.py` 的封存字节、链接与远端回读校验。

## 2. 本次明确核过的关键合同

### 2.1 资格门确实是 ACT≥250，不是运行 120 期

095 的 `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java:16,34–43` 明确要求：自然 FUTURE ACT≥250、命中率≥34%、ECE≤0.04、最差 720 块命中率≥26%、最长连失≤6、ABI 语义等价、序贯资格通过，以及相对 I00 和 null control 的配对下界均大于 0。最终 `LEARNING_AND_QUALITY.md` 第 6 节与学习审计第 7 节已保持这些条件。120 期可以是工程观察检查点，不能代替 250 个已结算自然 ACT。

按源文件 `streakGatePassProbabilityR31` 同等递推，在 IID、p=0.34、250 ACT、最长连失≤6 的假设下，单独这一门的通过概率为 `0.004700246670406036`，即约 `0.470024667%`。最终方案写 `0.47002%` 与计算一致；它明确是单门敏感性，不是总体发证概率，也不是当前模型胜率估计。没有据此降低原门或重置窗口。

### 2.2 同一期 REAL 唯一性已修到正确维度

`PIPELINE_CONTRACT.md` 第 3 节和 `CODEX_EXECUTE.md` 第 4 节使用 `(gameCode,targetIssue)`，position 属于冻结载荷。不能使用 `(gameCode,targetIssue,position)` 来允许同一期不同位置分别真实出手。B10 的拒例必须保留不同位置、不同策略、不同进程同时竞争，而不能只测试同位置的重复请求。

研究多位置和多策略仍可存在；它们不会因此获得多条 REAL 授权。这一条是合同闭合，实际数据库唯一约束和最终调用路径仍须施工验证。

### 2.3 机会分母事前形成，故障不删除

`WORK_ORDERS.json` 的 `R34-B04-01.implementation` 已明确：`N_scheduled_enabled` 由用户事前启用日记与日历生成，事后网络失败、进程死亡、超时不得删除分母；100 个机会仅 5 次有效冻结，工程覆盖是 5%。`PIPELINE_CONTRACT.md` 第 6 节同时要求 scheduled / started / completed / timeout / unmeasured 与 withinSlo / scheduled。

这避免了旧报告“有成功样本便显得有运行”的问题。正常服务、用户事前关闭、事后服务不可用应分别展示；强停期间不能伪造赛前观察，也不能借状态分类把原定启用机会从历史删除。

### 2.4 当前 R952 的两种 owner 已分开

`LEARNING_AND_QUALITY.md` 第 2 节分别列出：

- AST1/2：predraw → label → ResearchOutbox → artifact / LearningCursor。
- AST3/CP12：开发训练与独立冻结 → 正式 R958/R967 自然证据 owner。

因此，AST3 的 AST1/2 outbox 为零不能单独判其训练失败，也不能用 R27 的 policy/certificate 行数替 R952 签收。统一图表只能投影各族原件身份及状态，不能另外制造镜像表计数作为运行证明。

### 2.5 B04 投递和 B07 数值消费已分开

`R34-B04-05` 只验不可变 label / feedback 事件、投递游标与 delivery receipt；label 不以 BET 或证书存在为前提。`R34-B07-*` 才验证实际模型状态、训练 cursor、checkpoint 和 numeric-consumption receipt 的事务一致性。B04 的 100 次投递不等于 B07 已完成 100 次数值训练。

同一 feedbackId 必须贯穿两段；未知提交结果先查耐久 receipt，再决定是否重试，不能重做参数更新。前序验收没有再要求尚未实施的后序 learner 来证明成功。

### 2.6 迁移补件诚实限定为纯核心

`payload/runtime/supplement/INTEGRATION.md:5–9,38–42,69–92` 明确 `productionAndroidOwnerIntegrated=false`；58 个保护表来自实际 095 注册表，116 个 fixture unit 是 INSTALL / VERIFY 协议步骤，不能称为 58 张生产表已迁移。

补件没有虚构现场 DB 指纹，也没有把 `originalContentRoot` 参数当成已实现的低成本原件保全。原 `SchemaDomainCoordinatorR926`、`ResearchRetentionStoreR30`、启动 checkpoint 和 writer 的实际接线、全量指纹等价替换、schema cookie / owner fence 复验仍是 B01 必做门。这里边界清楚，未发现以纯函数代替 Android 迁移完成的表述。

## 3. 初版 15 项 IR 的闭合情况

“合同闭合”表示修正已落在方案与工单中；除明确列出的宿主局部代码验证外，均不代表应用实际行为通过。

| 原审查项 | 最终观察 | 状态与剩余门 |
|---|---|---|
| IR01 REAL 唯一键 | 明确 game + target；position 为载荷 | 合同闭合；B10 实际唯一约束与跨位置并发负例待做 |
| IR02 机器生产者倒依赖 | CODEX 第 2 节要求 B06/B07/B08 当批完成各自生产者与消费者，B11 负责汇总呈现与剩余覆盖 | 合同闭合；不能在前批手工 INSERT 成功 receipt |
| IR03 投递/数值训练混算 | B04-05 与 B07 分工明确 | 合同闭合；实际数值重放仍待 B07 |
| IR04 故障删机会分母 | B04-01 与 PIPELINE 明确事前启用分母 | 合同闭合；实际启用原件与日历 join 待做 |
| IR05 在线学习/冻结窗口 | ONLINE 状态更新推进 stateHash；已登记 updater 不变时不重置协议观察；静态冻结实例禁止原位换权重 | 合同闭合；REAL 默认消费独立冻结已验证实例，不宣称旧 V1 接受任意滚动权重 |
| IR06 性能测量口径 | nearest-rank 仅用于成功有限时延，完整分母和 withinSlo 同报；生成源已显式加入 minimum_completed_samples / observation_window | 合同闭合；最小 completedN、窗口和实机资源预算须在测试前冻结，未校准不得宣称 SLO |
| IR07 bootstrap 恢复环 | 关键 schema / 故障 spool 与研究迁移拆开，补件列实际旧 owner 接入点 | 合同闭合；Android owner 接线与空库/半迁移/hold 场景仍待做 |
| IR08 旧 66 项细节缺口 | 全 66 旧 ID 精确映射，补 147 条测试合同，双向 mandatory 已校正 | 合同闭合；66 拟新增 case 未实现，现存测试未在当前 R34 应用复跑 |
| IR09 F23 教师 | B06-02 明确 LegacyTeacherOwner / artifact / PIT / ABI，缺件 WAIT_TEACHER，新替代教师新身份 | 合同闭合；冷启不等于自动修好原教师 |
| IR10 S5 缺父/满槽 | B06-05 与 RE23 继承合同保留 WAIT_PARENT / WAIT_CAPACITY 和同案续收 | 合同闭合；必须以独立 freeze / settle 原件形成 ACK |
| IR11 正负控制实验 | B07-05 指定合法 1–10 排列负控、预登记种子/次数/候选规模与可学习 PIT 正控 | 合同闭合；实验必须绑定预登记总错误预算，fixture 证书不得进入生产 |
| IR12 同机会基线 | 最终生成源已明写 game + target + position + role、固定 K3 / 纯过去滚动频率、事前配对与缺失处理；3/9.9 盈亏平衡保持 | 合同闭合；同日历不能替代位置/角色同一性，详见第 7 节回读记录 |
| IR13 构建身份 | 构建环境注入 sourceCommit，构建后记录 APK hash；每批 R967/R82；独立输出目录 | 合同闭合；不以手填 hash 或旧调用索引绕过 |
| IR14 有限资源与长期保留 | 活动池/队列预算、raw TTL、归档及保留债明确；birth pending 上限 16 | 合同闭合；手机实际 RSS / WAL / 文件增长和耗尽状态待实测 |
| IR15 工程/真机/自然状态 | ACCEPTANCE 分 IMPLEMENTED / BUILD / BEHAVIOR / DEVICE_PENDING / QUALITY_PENDING | 合同闭合；自然不足不假签，不阻止已满足工程依赖的后续施工 |

另有两处最终发现已回读确认修正：`CODEX_EXECUTE.md:3` 不再将本次方案交付扩大为应用施工发布授权；`LEARNING_AND_QUALITY.md:28–30,63` 和 PIPELINE 第 3 节不再把每次已登记在线权重更新等同于新 protocol / 清空自然账本。

## 4. 旧 66 项承接：原 44 空入口如何处理

### 4.1 原缺口必须留在记录里

第一次读取最终 crosswalk 时，66 个旧 ID 都已映射，但 **44 项 `current_test_entry` 为空**。仅有批次标题与保留描述不能满足 IR08。因此不能写“原来已全部有可执行回归入口”。

本次新增 `analyses/legacy_test_contracts.json`，现已并入根方案 `R335_CROSSWALK.json` 和对应工单。补件逐旧 ID 给出具体正例、拒例、断言、拟新增方法与执行方式，同时保留可定位的现存部分入口：

| 项目 | 独立回读结果 |
|---|---:|
| 旧要求总数、不同旧 ID 数 | 66 / 66 |
| 原始旧 ID 集合与最终集合差异 | 0 |
| 全部测试合同条目 | 147 |
| `EXISTING_ENTRY_REQUIRES_REVALIDATION` 引用 | 81 |
| 这些引用涉及实际 095 文件 | 21 |
| 不重复的文件 + 方法组合 | 29 |
| 路径、hash、方法定位缺失 | 0 |
| `NEW_TEST_REQUIRED` 独立 case 合同 | 66 |
| 本轮新增回归实现代码 | 0 |
| 本轮现存 Android / Gradle 回归执行 | 未运行 |

81 是跨旧要求复用的测试方法引用数，不能称为 81 个独立新测试；21 个文件存在，也不证明它们覆盖全部新增拒例。所有现存入口的范围明确为部分支持，后续必须对当前实际 R34 提交重验。

拟新增统一入口是 `tools/r34/test_legacy_r335.py --case <旧ID>`，每 ID 独立 `test_r335_*` 方法；需要实际 Android owner 的 case 同时列拟新增 AndroidJUnit 类/方法。它们是施工合同，没有伪装成现在已经可运行的文件。已有断言足够时应复用，不为凑 66 数量重复实现。

### 4.2 双向映射检查

已独立核对 `crosswalk.new_issue_ids`、`WORK_ORDERS.legacy_requirements` 与 `mandatory_assertions`；当前未发现漏边。两处明确修正为：

- `R335-RT06` 新增反向 owner `R34-B04-03`，与其中无冻结终态/坏首记录断言一致。
- `R335-RE12` 新增反向 owner `R34-B08-04`，与负贡献方向断言一致。

原 44 项缺口的最终状态是 **“具体测试合同补齐，代码实现与当前版本执行仍待施工”**，不能写成“44 项测试已通过”。

## 5. B06 冷启参考代码的独立生命周期复核

复核对象是 `analyses/learning_payload` 中三个新类、八个 095 原文件集成 delta / 后像。独立复跑命令为：

```bash
python3 analyses/learning_payload/tools/r34/verify_cold_rebirth.py \
  --source-root /workspace/scratch/ca36fc3f9d7b/source095 \
  --output /workspace/scratch/ca36fc3f9d7b/analyses/independent_cold_rebirth_verification
```

实际输出：`PASS_LIMITED_HOST_SCOPE`，44 项 SQLite / 跨语言 / 进程恢复检查通过；Java 纯合同日志为 33 个断言。原始回执见 `analyses/independent_cold_rebirth_verification/cold_rebirth_verification.json`。这里不包含整个 handler 的 Android 类型检查或调度执行。

### 5.1 曾经找到的确定阻断及修正

**原始 095 的 source prune 会删除仍被 RUNNING expert 引用的 cold source。** `FoundryCampaignR26.java:149–156` 原 SQL 排除 candidate 引用，却没有排除 active expert；随后 `RetentionR26.java:107` 又会删除无源、零 cursor、空 checkpoint 的 experiment。birth receipt 仍保留，原 `done()` 会阻止再生成，形成“出生一次后再也不训练”的死端。

该路径已用实际原 SQL 在 native SQLite 复现，记录为 `analyses/b06_cold_source_prune_repro.json`，结果是 source=0、child=0、birth receipt=1。最新补丁以 `ColdRebirthContractR34.pruneSourceSql()` 保护 active expert 引用；它不无限 pin raw，5000 期 TTL 仍可清源字节。

**typed registry / progress 初始缺失。** 新调度 task 单独注册不足以让旧报告识别；旧自动产物进度 hook 只处理原 ENTRIES。最新补丁将 task 加入 ADDITIONAL，保持原 57 项统计口径，并在出生事务内显式 `ResearchStoreR30R1.commitProgress`；unit=POLICY、training=false，健康 pending 从真实父队列读取，避免把出生数冒充训练量。

### 5.2 过期零训练对象的恢复链已补齐

最新实现的 `pick` 同时处理旧 ABI preserved 父和 birth 可定位的过期/缺源零训练 child。后者以旧 child 作为新 parent，生成新的 source、identity 和 receipt，而不是改旧 receipt。

事务内顺序为：核 fence / parent 原状态与 cursor / 当前 ABI / source generation / source 保留范围 → 插入新 source / child / receipt → 对旧零训练 child 做 `COLD_UNTRAINED_SUPERSEDED` CAS → 提交 typed progress。新后继未提交时，WarmCandidate、Foundry、Retention 三处清理均保护该零训练元数据；后继一旦存在，旧可淘汰元数据不被无限保留。原旧 ABI 父的字节、hash、指标与证书没有被迁移覆盖。

`requireParent` 再核原状态、stateHash、checkpoint、cursor、ABI、canonical/spec 与 sourceThrough；数值 owner 在准备期间已推进时不能以旧父提交新生。source 元数据已缺失的 cold child 可从不可变 birth receipt 取得 ABI / through；非 cold 原父缺来源不能编造暴露边界，保留局部等待。

pending 仅统计当前 ABI、有效 source、可实际训练的 child。16 个 source 同时过期时 runnable pending 归零，允许受限地生出后继；不会被 16 个不可运行旧项永久占满。新的 birth 仍一次一项，总可训练 pending 上限 16。

### 5.3 未完成的验证不可省略

当前未发现新增明确代码阻断，但实际 Android owner 仍须验证：完整编译、schema 安装后调度恢复、Foundry / ExpertWorkshop 真正推进 child cursor、模型发布和消费、kill / artifact / fence / 内存不足路径。只 birth 不算训练；只 Java parse 不算 typecheck；宿主 SQL 通过不算设备运行通过。

Expert Pair 左成员已构造而右成员失败的半构造释放仍是未交付的资源修复。最终交叉审查发现 payload 曾把它称为“B02 前置”，但当前 R34 B02 实为抓取，所读文件卡尚未含 ExpertExperimentR26。已要求根方案将其明确并入 B06-01/02，增加真实 Pair owner 文件和右侧构造失败注入负例；B06 birth 启用前必须完成，不能因本次冷启合同通过而消失，也不能称为 B02 已实现。该项要求施工合同有明确归属，不要求本轮把参考 payload 冒充完整应用修复。

## 6. FA01—FA22 是否仍有明显遗漏

初版 `analyses/plan_independent_review.md` 已逐 FA 给出对应工单。本次按最终主题重新核对，没有发现某个 FA 被整体删去：来源/连续尾部在 B02；writer/故障/迁移在 B01；分析水位/后台恢复/延后饥饿在 B03；漏冻结/终态/反馈/时延在 B04；旧原件/存储预算在 B05；家族/教师/冷启在 B06；真实数值训练/发布/消费在 B07；失败研究/变异/融合/淘汰在 B08；未来资格在 B09；唯一应用与结算在 B10；图表/矩阵/报告真值和诊断在 B11；同 APK 耐久及全案验收在 B12。

报告证据边界也仍保留：`F01_DEFERRED_MATERIALIZE` 是 recordCaught 记录的已捕获异常，不能据栈断言后台线程退出；5 次冻结需按真实存储机会区间计算，不能直接用 issue 数字相减当样本数；旧 R26 / shadow 的历史产出不能算成当前 R952 训练；DB 1.4GB 不是 RAM；96 checks 被运行不是 96 checks 全通过；图表 producer 类存在不是 runtime produced=true。

因此，“整体链严重停滞”有证据支持，“从来没有任何模块运行过”不受证据支持。最终施工设计沿实际缺口推进，没有依赖后一种过度结论。

## 7. 最后增补的回读记录与后续门

1. **同位置/角色基线已补**：独立回读 `analyses/plan_build/issue_data.py:76` 与 `doc_texts.py:236`，已加入 game + target + position + role，固定 K3 和纯过去滚动频率基线，以及事前配对集合 / 缺失处理。最终重建后应在 B07-05 和 LEARNING 主文保留，不允许只取双方成功或命中的交集。
2. **性能预登记已具体化**：独立回读 `analyses/plan_build/build_plan.py:179`，预算待校准项已加入 `minimum_completed_samples` 与 `observation_window`。没有实机测量的 writer hold、CPU 量子、活动 learner、backlog、RSS 等不得填伪测量值；B00 必须在正式性能协议执行前校准并封存，目标保持用户要求。
3. **测试入口验证器已补**：独立回读 `analyses/root_payload/tools/r34/verify_plan.py:105–111`，`--source` 会核 EXISTING_ENTRY 的文件、观测 hash、方法定位；新增合同允许不存在，但保持 NEW_TEST_REQUIRED。独立复核另已对 21 个实际文件做同类核验。
4. **交付 seal 外部锚点已补**：独立回读 `analyses/root_payload/tools/r34/check_acceptance.py:30,41–44,72–73`，新增必需 `--plan-manifest` 并拒绝与 receipt 自洽但不匹配受信 manifest 的 seal。输入 manifest 仍须来自已复核 Git commit；任何无外部信任锚的 JSON 一致性都不能自证真实性。工具保持 `application_runtime_accepted=false`、`natural_advantage_proven=false`，不会把 SDK 身份验证升级成运行/自然资格。
5. **旧测试执行账本**：147 条合同存在不算复跑；每条必须连到当前 sourceCommit、工具 hash、具体输入与真实 owner 输出。拟新增测试不得以字符串匹配或手工插入成功 receipt 冒充回归。

上述生成源 / 工具源已复核，根任务还需最后重建封包以使其进入实际交付文件。Pair 资源项必须明确挂到 B06 施工 owner。完成方案封存与远端原件回读后，可以交付施工方案；实际工程、设备和自然结果继续按各自门禁保留未完成状态。

## 8. 对根任务的交接

本独立审查产物：

- `analyses/plan_final_review.md`：本复核意见。
- `analyses/plan_final_review_facts.json`：当前结构/入口/双向映射核验、局部复跑结果与关键文件 hash。
- `analyses/legacy_test_contracts.json`：已并入根方案的 66 旧要求测试合同补件。
- `analyses/independent_cold_rebirth_verification/`：独立宿主复跑原日志。
- `analyses/b06_cold_source_prune_repro.json`：原 source prune 断口的可复现记录。

建议最终对用户清楚说明：三份原报告与完整方案已经提交到哪里；哪些问题由报告与源码确证；哪些方案只是待施工合同；哪些局部代码实际通过何种测试；没有生成或验证新的完整 R34 APK，也没有证明未来高胜率。该区分正是避免再把“库里有方案、有 APK”误当软件已经运行起来的关键。
