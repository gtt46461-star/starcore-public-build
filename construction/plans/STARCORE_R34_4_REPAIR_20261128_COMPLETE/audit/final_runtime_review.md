# 总施工方案最终独立复核：运行、导出、存储与批次依赖

复核对象：`repo/construction/plans/STARCORE_R34_4_REPAIR_20261128` 的 `ISSUES.json`、`BATCHES.json`、`SOURCE_ANCHORS.json`、问题/批次卡及三份 contracts。核验底座仍为已安装 211 提交 `5c7f1b36eaee6036df280e85835bb0fb4ce0b40d`。

本次仅审阅和读取源码，不修改生成 JSON、应用 Java/C 源码、数据库或远端仓库。本文是设计与封包复核，不是 Android 构建、迁移、真机运行或自然质量验收。

## 1. 结论

**运行与导出核心设计可继续施工，不需要推翻。163 条结算待办的分类边界、快照原件保全、原报告格式、96 项检查及独立诊断 owner 均已正确保留。** 本次识别的原 catalog 枚举兼容和开发训练启动循环歧义，已在合同中修正并重新读回确认。

封板前尚需完成下列生成/封包核验：实际 native pin 文件加入 RT19/B01 白名单与源码锚；解除现代 TRAIN 对 B05 全部旧原件恢复的整体阻断；将已实际产生的候选补丁、复核脚本及原始结果拷入仓库并建立可运行路径。主代理已确认正在处理这些事项，本文不提前把“准备修改”写成“生成物已验证”。

### 结构检查的实际结果

|检查|已观察结果|
|---|---|
|主问题/原始问题|46 个主问题；50 个原始 ID 均唯一，合并未丢原始记录|
|批次|B00–B11 共 12 个；所有问题均指向存在的批次|
|文件锚集合|审阅时 115 个唯一文件；与全部问题的 source_files 并集一致|
|问题卡|46 个独立 Markdown 问题卡齐全|
|依赖图|DFS 拓扑检查通过，无结构循环、无不存在的依赖 ID|
|完成状态|46 项均为 PLAN_DEFINED_PRODUCT_ACCEPTANCE_PENDING，没有冒签产品已通过|
|数据归因|RT10 已保留当前 cohort 的 3 次 canonical 成功；RT18/RT19独立成项，没有再把全部欠账归于推理|

新增 native 文件后文件数应据实际集合重新计算，不可继续硬写 115；若同时补充其他真实施工文件，同样由生成器统一导出。

## 2. 必修与封板项

### FR01：实际 native pin 源文件必须进入施工锚和白名单

**状态：缺口已定位，主代理确认已纳入生成输入；本文第二次读回时生成 JSON 仍为旧 115 文件版本，待封板重生成核验。**

R344-008 / RT19 要求在实际 SQLite 水位被固定时取得独立单调时钟区间。原 source_files 仅有 Java 文件，主合同又要求只改卡片列出的文件，这不足以实施该功能。

精确 211 中实际位置为：

- `app/src/main/cpp/report_native_cp4.c` 的 `cp4_backup_impl` 从第 50 行开始。
- 第 63 行执行 `PRAGMA query_only=ON;BEGIN;PRAGMA schema_version;`；第 65 行核验 `SQLITE_TXN_READ`。应在这一窄区间捕获 pin 证明。
- 第 398–402 行为 `Java_com_ethk3_novaonepro_ReportNativeCP4_backup0` JNI 桥。
- 原文件 36,546 字节，SHA256 为 `4a0919b59e2ddb898a41e40a0406db601a41957973bb9fedd7ae0c537c9ddcc1`。
- 同文件第 31 行 `now_ms()` 使用 `CLOCK_MONOTONIC`；主 writer 样本使用 Android `elapsedRealtime`。不得不经时钟域验证就把这两种值混用。

最小落地要求：将该 C 文件加入 RT19、B01、源码锚和文件矩阵；在实际 pin 边界走与 writer 同域的 Java 回调或明确验证过的同域 clock，保留原 memory/backup/cancel/磁盘准入行为。`ReportNativeCP4.sourceControlR31` 的包装层也必须转发新增回调，不能只在外层 Control 实现而在 wrapper 丢掉。

新增采样为可选诊断能力，失败保持 UNKNOWN，不能因为缺新 sidecar 把原本能完成的报告备份变成失败。pin 边界不写大文件、不做整库 hash、不阻塞实时 writer；最终完成后再绑定 snapshot SHA/bytes 并原子保存回执。只修改此 native 接口与对应 Java consumer，不改 vendored SQLite/Zstd。

**关闭条件：** 生成物含精确 C 文件锚；候选/施工差异可定位此 pin 点；JNI/Java 同域测试覆盖休眠、取消、长复制和旧 job 无锚兼容。最后这些运行测试属于施工后验收，不能用本次只读审阅代替。

### FR02：保留外部 catalog 枚举，内部等待状态不得直接序列化

**状态：已修合同并读回确认。**

211 的 `ReportCatalogCP4.CheckResult` 第 61 行仅接受 `EXECUTED / READ_ERROR / CANCELLED`。把新依赖容器的 UNAVAILABLE 或进度的 PAUSED/NOT_STARTED 直接塞入 `execution_state` 会触发 `CHECK_EXECUTION_STATE`。当前 catalog 外层通常会捕获并转为 READ_ERROR，不能据此断言一定无法生成 ZIP；但会制造伪读取错误、损害完成态和恢复语义。

`EXPORT_AND_STORAGE.md` 已明确：

- typed 状态只保存在内部依赖结果容器。
- 来源不可用映射为现有 READ_ERROR、`domain_outcome=SOURCE_UNAVAILABLE`、UNKNOWN 和稳定错误引用。
- PAUSED/NOT_STARTED 只在 job/check-progress 内部使用；对外仍按原 96 项和既有枚举装配。
- `checksComplete=true` 和 `allChecksPassed=true` 不被新增为 DIAGNOSTIC 交付/工作快照回收门槛。

**施工验收必须保留这条合同。** 不能为方便实现而扩大外部枚举、删慢检查或把未执行写成 PASS。取消仍独立传播，不能被当作可缓存的一般依赖失败。

### FR03：解除不相关原件恢复对现代开发 TRAIN 的整体阻断

**状态：开发 bootstrap 合同已修并读回；BATCHES 依赖调整已由主代理确认在处理，第二次读回的 JSON 尚未重生成。**

最初 B06 直接依赖 B05，B05 同时承担 R27 固定 pin、556 缺检查点、旧 ABI 冷子代和长期摘要等工作。即使现代 CP9 已有合法 `.scdev` 输入，不能因为另一路旧模型原件暂时缺失而禁止现代开发 TRAIN 开工。

应将 B06 的工程前置限定为身份、准入/恢复基础与本任务实际输入资格；B05 中只有被该候选真实引用的输入/父模型才能成为局部前置，不能把整个 B05 的现场自然恢复作为全局 gate。批次的工程可集成、目标设备、原件恢复、24/72h、自然质量状态须分别保留。

`CREATION_TRAINING_AND_QUALITY.md` 已补出正确启动路径：

`RuntimeCoordinatorR26.runModernContinuity → ResearchLearningContinuityR912 → Director IMPORT/TRAIN`。

即使 REAL=0、证书=0、旧结算全是 NO_PREDRAW，只要有足够可信历史和确认开奖，开发 TRAIN 仍应推进。RollingLearner 仍只消费合法 COMPLETED，不得伪造 completed/rolling_event 来启动。

**关闭条件：** 重生成后的 BATCHES/批次卡体现局部输入门槛；无原件的旧分支有明确 SOURCE_MISSING，而数据独立现代分支可以构建、运行和验收。新增依赖后再跑 DAG 检查。

### FR04：候选、复核脚本与原始结果需要真正进入仓库

**状态：主代理确认正在最终拷贝封包；第一次仓库检索时尚未存在，待最终路径检查。**

问题卡与合同多处写“随包候选补丁/脚本已验证”，但审阅时仓库中没有对应候选与脚本，原文件仍在 workspace 的 audit 下。应至少保存这些实际文件，并给出相对路径和可运行命令：

- `coverage_scope_join.patch`、`check_coverage_join.py`、`sql_equivalence_result.json`。
- `heartbeat_scope_identity.patch`、`check_scope_identity.py`、`scope_identity_result.json`。
- `check_settlement_evidence.py`、`settlement_evidence_result.json`。
- 其他代理提供的候选补丁、host fixtures 和真实验证日志。

scope 脚本的输入必须包括精确 source checkout、原报告、候选 patch；settlement 脚本输入原报告目录。原报告 ZIP 已纳入 evidence，可以提供解压后的复跑命令，不能依赖审计机器独有的绝对 scratch 路径。

验证标签须继续区分：SQL 主机等价、原件元数据联查、字符串/源码契约、Java 编译、Android 编译和目标设备运行。脚本生成的 `javaExecuted=false / androidCompiled=false / deviceMeasured=false` 应保留。

**关闭条件：** 仓库相对路径存在、正文链接可达、脚本命令可从包内证据复跑、精确底座 apply --check 与原日志一致；不重新包装为“完整软件修复通过”。

## 3. 重点合同复核结果

### 3.1 报告不退化

已保留独立 diagnostics owner，未迁入 ResearchWriteQueue；原 request→snapshot→catalog→archive→verify→READY→SAF 仍为基础。默认诊断和完整原件模式、单卷/分卷、清单 SHA、旧 job、取消、失败可导出均保留。

typed memo 键含快照/源码/定义/真实参数，只限本次 attempt；新 context 不复用旧错误、旧取消信号。一次依赖失败只影响真实依赖者，不把所有 40 项归为同一个 SQL。当前同步 runner 返回后再进入下一项；未来异步实现须等实际退出，Future.cancel 不是连接已空闲证明。

对同一不可变快照的续跑必须使用持久游标和已验证结果，不能每次 PAUSED 后重新从最慢第一个检查开始而无限自循环。失败结果可诚实出包，不要求业务全绿。

### 3.2 快照释放没有过度保留或错误删除

READY 的 DIAGNOSTIC 工作快照退役与 FULL_RAW 原库替代证明已分清。满足原请求并完成原 ZIP/manifest/semantic 验证、无额外恢复引用后，诊断作业沿既有 requireRawCoverageR31/cleanRaw 清理；不强迫装入5GB原库。完整原件任务则必须有精确同SHA同字节原库证明。

FAILED 唯一输入不能按年龄、大小、另一个不同快照成功直接删除；先真正恢复原请求或取得精确替代和引用核销。多个进程的 active reader/share 锁、job revision 和清理许可重新核验，清理失败不把 READY 降级。

因此无需再增加“所有失败快照无限保留”规则；现有合同已经要求列明阻断原因、恢复动作和可验证退役路径。

### 3.3 163 条 LIVE_RETRY 分类已经正确进入主问题

R344-023 完整保留 RT18 的精确联查和证据边界：163 全为 LIVE_RETRY，当前 shadow 行全缺，canonical hash 全相同，162 无热表实际开奖。已有 7,457 个无赛前终态和 3,308 个完成态不被批量重置。

恢复方案没有粗暴删掉 BACKLOG 谓词；要求核真实开奖、冻结/归档、正式 REAL 义务、活动 owner 和 lease。存在原冻结才做精确恢复结算；能证明无冻结且无待履行正式义务才审计排除；原件未知则显式待恢复。

同样保留延后证据的原 snapshot/hash、100/1200、core/action cursor、唯一租约与事务幂等。2,899 非终态不能当成2,899次相同异常；117 RUNNING不能直接当117个僵尸任务。核心结算不能被研究/延后侧车失败回滚。

### 3.4 writer 快照时钟边界正确

R344-008 明确旧报告无独立 pin 回执继续 UNKNOWN；RT12 的完整 scope 字符串不被当作 RT19 已解决。验证成功最多为 AS_OF_SNAPSHOT，不是当前 main 存活。不能从样本自身、observer 导出时刻或整个复制耗时推造锚。

唯一待补施工范围是 FR01 的实际 native 文件和回调链，不能只修 Java 数据结构。

## 4. 批次可运行性与非阻断建议

### 4.1 已确认不构成循环的安排

- B01 报告读取使用不可变快照和独立诊断 owner，不依赖 B03 研究恢复成功，可独立先实施。
- B02 的 writer fault 排空使用 `WriterHoldFaultsR31` 的 RESEARCH_CONTROL 与 `DatabaseCoordinator.runIdempotentAtomicWriteR31` 入口，应继续作为独立可前进控制路径；不等待被自身背压阻塞的普通研究队列。这避免“先研究完成才能排空、先排空才能研究”的运行死锁。
- B04 与 B05 可在共同 B03 工程基础上推进，分别保留主链和固定研究输入的边界。
- B08 的 EMPTY/WAITING 正确投影可以作为工程验收结果；不能要求必须先有自然 REAL，才允许 B09 开始自然观察。
- B09 的工程路径、自然窗口与质量结论分开；B10/B11 汇总不能要求强行把自然 NOT_PROVEN 改为 PASS。

### 4.2 建议显式增加现场主链门槛

最初 B07/B08/B09 的 DAG 祖先没有 B04。代码集成可以并行，但 B09 要签收“自然主链已消费”，必须要求 B04 的 PIT、唯一 owner、合法截止、精确结算等相关工程回归已通过。无需等待 B04 整个72h或所有历史原件完成；建议在批次卡中列出按能力的现场证据门槛。

同理，B04 若当前没有合法可加载模型，可用隔离的合法工程夹具验证调用/时序，真实自然覆盖保持待 B06/B07 产物；不能要求 B04 先证明长期自然高覆盖才允许训练批次施工。

### 4.3 建议把关键跨合同引用加在入口

在 PIPELINE 历史恢复段链接 R344-023，在 EXPORT 时钟/身份段链接 R344-008，使只读总合同的施工者不会漏掉这两个独立问题。问题卡已经足够详细，此项属于降低误用风险，不要求复制长文。

## 5. 复核快照与最终封板边界

以下为本次第一次完整结构检查的输入 SHA256，便于主代理判断后续重生成是否已经覆盖审阅反馈：

|文件|SHA256|
|---|---|
|ISSUES.json|5b3dcb920c7e64c1873f1ff4f69bd7aa5a075e7d3985e8c5dc7337d18770fd18|
|BATCHES.json|cde057bd0e406acbc4d7b24b75c7e1b8fc8853c15464a1b8cb5fb93ae338f0a2|
|PIPELINE_AND_PRESERVATION.md|97097394e913c2bdc61f566927bf4d7679dd3cedfdfd48987b4985e26930416e|

第二次读取确认的修订合同：

|文件|SHA256|
|---|---|
|EXPORT_AND_STORAGE.md|c265f1575f2564dd731e8cecf4893b29c24d43ff2aff97eb97d8c7023edde33e|
|CREATION_TRAINING_AND_QUALITY.md|784a6730efca7dbfcb11c94640864b960aee0ee69a11d1ce1362ee03afe2b982|

最终主代理应对重生成后的 source_files/anchors/矩阵、46主项/50原项、12批 DAG、候选脚本与链接做一次封板核验。本文已确认的设计边界不替代这一生成后检查，也不替代未来真实应用返修验收。
