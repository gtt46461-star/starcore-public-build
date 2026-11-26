# B06：滚动学习版本到独立正式冻结的可施工补丁

基线：`4d1de9d83692635b5c5ce39144fece1c8a31c8ac`。本目录属于 R34.3-173 修复方案；APK 仍沿用既有 R34.2-B01~B12 安装/签名/版本系列。本文及完整 after 源码是施工材料，不是已部署或设备验收证明。

## 交付与依赖

`../rolling_after/` 包含 5 个完整最终文件，`rolling-publication.patch` 是它们相对于安装基线的精确差分，`patch_manifest.json` 是 before/after SHA-256。必须与 `../research_patch/rolling_candidate/` 同批合并；不能单独应用后启动功能。后者负责 `RollingLearnerR343` 的事件、版本、原件、数值训练、canonical 修订保护、schema、五个 TRAIN lane、清理、BoundAtom 父源接线和恢复捕获条件。`../runtime_patch/` 的 DatabaseCoordinator 已累计登记本批所有 writer 操作，不要再覆盖成旧登记表。

本包 5 个文件：

| 文件 | 完整修改 |
|---|---|
| `r952/RollingFrozenPublisherR343.java` | 新的独立发布 owner、3 张不可变元数据表、原队列调度、逐页原件核验、CP9 DONE checkpoint 重放、发布事务、未发布版本超替回执 |
| `ResearchEvaluationRunR967.java` | 独立冻结身份生成；按 frozen 自身 run hash 精确解析；统一元数据资格谓词；窄 lifecycle 接口；mayCapture/consumerCompatible 保持旧窗 |
| `FrozenCandidateAdapterR958.java` | 正式捕获 SQL 及每学习器/全局活动冻结容量计算改用统一资格谓词；原训练头选择保持 |
| `GlobalResearchScheduleR932.java` | 已预约窗口续存核对和新校准预约候选扫描均改用精确冻结身份；原241连续捕获、容量、champion、来源和缺口规则保持 |
| `ResearchFunnelR967.java` | 原报告只读漏斗加入12个滚动事件/训练/原件/发布/拒绝/超替/等待观察量；缺表仍显示 UNKNOWN，不将 COMPLETE 说成合格 |

## 必须解决的源码事实

安装版 `ResearchEvaluationRunR967.mayCapture` 以 `current(db, learner, target)` 等于 frozen.run 为前提；`FrozenCandidateAdapterR958.capture`、`GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11` 两条查询也以最新 RUN 过滤。直接给同一 learner 新增 RUN 会改变它们的筛选结果：原来已经冻结、校准或正在采样的旧对象会失去资格，甚至被预约 owner 记为退役。相反，只创建一个没有 RUN 的新 frozen 又无法通过这些 JOIN。

因此本补丁不写 `r967_numeric_training_run`。新发布写既有不可变 `r967_frozen_evaluation_v2` 加独立 `research_rolling_frozen_publication_r343`。精确 resolver 首先按 `run_hash+learner` 查询原 RUN；未找到时才查询独立 publication 并与 EVAL 的 frozen_id、run、learner、first、rule、epoch、model、artifact 逐项相等。原 Run 构造器继续重算 origin/run identity；原 Frozen 构造器继续核对 AST/model/evaluation hash。

旧 RUN 的 `current()`、训练输入的 `requireTrain/bindInput/verifyRecordedInput`、旧算法训练捕获与历史 ABI 均保持原语义。评估边使用冻结对象自己的 lineage；兼容规则仍必须在构建审核的精确 hash 白名单中，父候选退役、依赖失效、错误 epoch、错 AST、未来起点不符仍拒绝。没有放宽旧协议 `candidate_id UNIQUE`、候选 `structural_hash UNIQUE` 或任何旧记录不可变触发器。

另已全源复核其余引用：StagedResearchRoster 经 consumerCompatible 进入新 resolver；BoundAtomLifecycle 的两条旧 RUN 查询在配套研究补丁改用同一 resolver；ResearchCaptureQueue 的旧训练恢复分支补上与 ResearchDirector 相同的 AST3 排除，现代捕获仍经正式 frozen reservation/任务路径。RuntimeChainDiagnosis 原训练头观察保留其原含义，新滚动状态在新漏斗观察项中单列。

## 数据与所有权

### 三张新表

1. `research_rolling_frozen_publication_r343`：version 主键、frozen/run/artifact 唯一，记录初始未来期、AST/rule/epoch、model/normalizer/manifest/protocol/input/训练receipt/checkpoint/feature-prefix、完整发布 JSON/hash。模型 payload 仍在既有 EVAL 表，不另复制进原训练 RUN。
2. `research_rolling_publication_page_r343`：每页最多32行原件验证的前后 prefix、索引边界和完整身份 receipt。常规420行版本需要14页；时间预算缩短页面时页数至多1320。页面不可更新、不可替换、不可删除。
3. `research_rolling_publication_reject_r343`：终态拒绝或 `SUPERSEDED_UNPUBLISHED` 的不可变原始回执。后者明确 `statisticalFailure=false`，不是低胜率、不是训练完成、不是正式评价结果。

publication 与 reject 设互斥触发器，同一 version 不得既发布又拒绝；原件 pin 不能因为第二种结果而错误释放已发布对象。三表只由既有迁移 owner 建立，报告/结算 hot path 不做 DDL。

### 唯一队列与真实调用

研究代理已将原 `ResearchTrainingHookRegistryR952` TRAIN 子槽扩至5个 owner：legacy outbox、初次训练、初次冻结、rolling learner、rolling freeze。最后一项实际调用 `RollingFrozenPublisherR343.drainOne(store, endNs)`，内部使用原 `ResearchTaskQueueR967` 的 `R343_ROLLING_FREEZE`，没有第二任务系统或新线程。

任务自然键固定为 kind、versionId、next(cutoff)、protocolHash；input 和 authorityRef 分别固定原 inputHash 与 versionId。这个任务期是调度身份。真正发布未来起点在准备时按当前最新有效 draw 的 next 计算，并在 writer 内再次核对且禁止已开奖。如果期间有新 draw，原任务有原因地等待，用同一版本重新准备未来起点，不改写任务身份或补历史预测。

每个 base learner 最多一个未终态发布任务，全局最多32个。原4096 payload 任务和22控制任务上限继续有效。claim/wait 是原 RESEARCH_CONTROL 类；plan/原件页/发布/拒绝/超替均为 RESEARCH_DATA，不能绕过 writer fault 背压。

## 发布前逐项真实验证

1. 先投影 byte length，模型≤262144、AST≤32768、训练receipt≤8192、协议≤65536、checkpoint≤262144、input≤524288。超界或原件缺失不能生成发布成功回执。
2. OFFER、VERSION、base AST、原 TRAIN 任务 COMPLETED/result/input/authority 必须一致。版本必须 COMPLETE，next_index 必须 trainN+120。
3. 每次最多核验32条 feature，校验 source_json 的原始 hash、由原 training task id/protocol/input/索引/角色/issue/actual/source/raw/payload 重算的 feature hash，以及有序 prefix。deadline 缩短后落不可变小页回执并让出，不每次从第0行重复大 JSON。
4. final 阶段核对完整 canonical protocol/训练receipt、AST/model/manifest/normalizer/weights/seed/各分割期/样本数/代码身份、canonical range hash，且 formalFutureCount=0、realAuthority=false、newStrategyCount=0。
5. 重读有 hash 的 SCDEV1 输入，核对原始范围与 train/embargo/120 validation 分割，逐行核对特征标签和输入开奖位置一致；另通过研究代理的 verifySourceR343 重新验证 retained/live draw 的 dirty、真实开奖原件、canonical 范围，并记录 source epoch，提交时 CAS epoch。
6. 用 finalModel.modern 作为形状构造 CP9 Dataset。CP9 shape identity 绑定 kind/family/dimensions/seed/cutoff/manifest/graph/expert/缺失策略/N，不含最终权重或normalizer。恢复原 DONE checkpoint，核对优化步骤、epochs/selectedEpoch、CE/Brier/有序预测hash，再 `finish(dataset, state, originalReceiptHash)` 必须得到逐字节相同 payload 和 model hash。
7. 这是 **DONE checkpoint/codec 的确定性重放**。它不是从初始随机状态重新训练一次；不能把本文说成已完成从头独立复算或设备数值验收。施工回归应补连续执行与多次断点恢复一致性。
8. 原源闭包/teacher/summary 在准备和提交分别使用既有 guard。所有统计门、真未来证明和资格政策保留；历史验证好看也不授予 REAL。

内存预算按实际读取长度预留，而非固定占用2MiB。SCDEV1保留的输入先 close，再申请数值恢复预算；feature页使用512KiB预算。研究 producer 的每期滚动窗口固定300 train + 1 embargo + 120 validation，requiredHistory另加；初次历史快训仍保持原最大1200策略。128MiB heap设备 MODEL4MiB 的真实高水位与任务吞吐必须实测，超预算继续等待资源，不能给假完成。

## 发布提交的原子边

同一个 coordinator writer 事务内依次：lease ownership → VERSION/OFFER/hash/generation/prefix CAS → future target与未开奖 → canonical source epoch → source guard → 每学习器2/全局32活动frozen上限 → INSERT EVAL → INSERT publication → 原 lifecycle NURSERY → summary/matrix/atom-study注册 → 原 FreezeMatrix 队列 → 原 publication task COMPLETE。

FreezeMatrix 的 components 精确保持原5键 `ast/model/features/development/protocol`。新 publication hash 保存在独立不可变表；向原精确键合同塞入第6键会被现有 API 拒绝，已通过专项回归防止此问题。

任何失败都回滚整组写入；不得先完成任务再补 EVAL、page或矩阵记录。无容量、deadline、目标推进、资源/typed数据库基础设施错误进入有原因的等待；真正身份/原件不一致形成独立拒绝回执。不会把旧候选标成低胜率，也不会替换旧模型、旧校准、旧证书。

## 容量、超替与旧窗保留

如果每个已完成后台版本都永久 pin 原件，而每学习器只有2个活动正式冻结槽，很快会填满64MiB rolling working budget，滚动训练最终停机。因此唯一 publication owner 可对 **尚未正式发布且已有更新 COMPLETE HEAD** 的旧版本登记 `SUPERSEDED_UNPUBLISHED`；如果该版本有任务，必须持有同一任务 lease 并以 `EXPIRED+receipt` 原子终结。无任务的老 offer 也一次仅处理一条，使用同一 owner。完整原始训练结果/receipt身份仍保留，只有 raw payload 清理由原 retention owner 按 pin 释放。

已经有 publication 的版本绝不走该路径，继续拥有自己的 formal model/run/normalizer、校准窗口和统计证据。处于活动 lifecycle 的 published version 保持原件pin。原模型全局持久配额同样不被突破：容量满时暂停新发布并保留原因，不能擅删不可变正式模型。系统可持续刷新后台 HEAD，不等于承诺无限存储或每个数值版本都获得独立正式试验名额。

## 已执行与未执行验证

可复现入口（目录均为本机实际绝对路径，输出在计划外新目录）：

```bash
python3 -B "$PLAN_ROOT/tools/run_host_checks.py" \
  --plan "$PLAN_ROOT" --source-repo "$APP_REPO" \
  --out "$EVIDENCE_ROOT/host-checks"
```

统一runner会复制随包host_validation夹具，并从完整after提取实际SQL构造器与DDL，在真实宿主SQLite执行`rolling-publication-sql`；`all-java-syntax`会编译随包ParseChangedJava后解析累积after。原记录保留在control/host_logs，新一轮结果和范围见SELF_CHECK与control/revision2_host。它们复现latest RUN丢旧窗/新独立对象，验证旧窗与新对象共存、错身份/边界/缺表/输入拒绝、原RUN行数/旧frozen字节不变、immutable/replace/页大小/互斥结果和FreezeMatrix五键ABI。

`rolling-six-kernels`另执行6类真实CP9数值kernel宿主回归：300训练样本，62136个实际状态转移、60个核心结果/负例断言（把循环上限检查计入时共62196）。使用未修改CP9和从生产源码原文提取的Modern/Model/codec段，finalModel.modern重建Dataset后restore/finish得到相同payload/hash/normalizer，改验证标签被拒绝。该测试不包含Android或数据库owner，也不证明预测质量。TREE可以在真实树训练发生时optimizerSteps=0，不能仅凭该字段判断是否训练。

研究代理另执行整源+旧Android host stub 的诊断类型检查：它曾发现 publisher 跨包调用 lifecycle 的真实错误，现已改为同包窄 bridge；重验未再发现这4文件新增符号/类型错误。整体仍有旧stub缺失Android API导致的错误，**不能把该诊断写成完整构建通过**。

未执行：真实 Android SDK Gradle/APK编译、设备SQLite事务故障注入、128MiB内存/快慢设备时延、当前4.3GB库迁移、自然连续300/120/241/正式Future/多重检验/主链REAL证据。用户报告没有这些新代码的运行结果。只允许标候选实现+宿主验证通过，整体 B06 必须等待下列门。

## 施工必须逐条补齐的设备门

1. 同173基线、同应用/签名覆盖安装，迁移逐单元恢复、schema可重入，旧表原始行/唯一约束/触发器逐列一致。
2. 初次 CP12 数值成功、后续至少3个结算cutoff产生独立滚动版本；固定图 STATE_REFRESH 不记优化策略出生。每任务完整状态和原因可读。
3. 在每一页验证前/后、checkpoint恢复后、EVAL/publication/lifecycle/matrix/queue提交前/后强杀；只有完整commit可见，重复投递无重复 frozen、receipt或正式观测。
4. 参数化覆盖6种模型kernel、全部合法原件类型、错hash/错seed/错normalizer/错模型/错train-label/缺source_json；缺原件不以当前teacher回填。
5. 输入范围内 dirty、draw修订、canonical更正、retained source替换与准备/提交竞争均阻止新发布；范围以外正常新draw只导致短暂重验或新未来起点。
6. old RUN / old frozen / old calibration / old window / old certificate 逐字节前后相等；旧已封存窗口仍从自身next期继续。新版本 CAL/Future 从0独立积累。
7. 通过原 multiplicity、241校准捕获、固定正式窗口、配对对照、资格、真实主链应用receipt逐边验证。没有合格证据时继续原已合格模型或 abstain，不能降低统计门强行出手。
8. 一直有新HEAD但正式容量满：旧未发布offer产生 SUPERSEDED receipt，已发布窗不退役，raw cleanup收敛、64MiB不突破；停止新HEAD时最新待发布版本原件保持。
9. 每base最多1个未终态发布、全局32，其他候选不会被同一base无限任务占满；5lane无空轮次提前退出。
10. 后台屏灭/Doze/断网/重启、writer backlog与FAULT drain竞争、24/72h连续运行、P50/P95/最大训练片段和发布writer时长、实际MODEL预算高水位均留原始证据。Android后台不保证1秒硬实时。
11. 对照报告12项新漏斗和原COUNT进行独立SQL复算：COMPLETED数值、发布、等待、拒绝、超替、真未来、授权、实际出手严格分开。
12. 最终原始报告/manifest/日志/截图及版本身份绑定随同施工交付；NOT_RUN/UNKNOWN仍可见，不以静态声明或host fixture替代用户规模设备证明。
