# 全部问题、详细原因与处理方法

按施工依赖排序。问题数量只统计独立问题卡，报告里的重复投影不重复凑数；有证据缺口的原因明确保持待证。

## 01. 当前唯一方案仍以122为底座，必须对152已完成改动作无损重绑

**编号 `R342-001`｜P0｜施工批次 B01｜证据等级 CONFIRMED_PLAN_IDENTITY_MISMATCH**

### 证据与源码定位

- `source-manifest.json` $.verifiedInstallationR341：{"installedVersionCode": 577311152, "sourceCommit": "eb8f82a8bda8db22f1f122095217132f07053bcb"}
- `construction/plans/STARCORE_R34_1_REPAIR_20261124/CODEX_EXECUTE.md` ：固定5562f9b1/577311122开工，主方案README也为122；主分支入口仍2b0177b7。
- `agent_file_card_delta.json` ：295文件卡对应294个不同实际路径，均存在；按卡计127与122相同、122修改、46新增；F238历史工具替代路径与F247实际工具共用verify_integrated_chain.py，不等于295个不同文件或全部运行验收通过。

### 已证实的原因或代码机制

- 方案入口的source_baseline没有更新到已安装152，且原文件卡before_hash仍为122。重复执行122整文件后态有覆盖有效152改动的风险；这是一项施工治理缺陷，不证明152的业务停滞由方案文件直接引起。

### 尚不能断言的部分

- 安装APK原字节未附，本审计只核报告安装身份与仓库精确源码，未独立拆包重算。

### 具体处理

1. 以eb8f82a8为本轮固定源码底座；记录旧方案commit与原件hash，不修改旧封印。
2. 按295原文件卡对应294个不同实际路径的当前blob重建增量文件卡；保留原卡身份及重复路径来历，同文件只由一个owner顺序编辑；保留已有用户未提交改动。
3. 62工单逐项承接，区分已实现需回归、现场未达标、未证实、真实源码缺口；152验收原断言全部保留。
4. 新R34.2方案单独封印，CURRENT_FINAL_PLAN与任务书只指一个当前入口；旧版本资料保留历史身份。

### 文件与方法

- `CURRENT_FINAL_PLAN.md`：UPDATE_ENTRY。

- `construction/plans/STARCORE_R34_1_REPAIR_20261124/MANIFEST_SHA256.json`：PRESERVE_SEALED_HISTORY。

- `construction/plans/STARCORE_R34_2_REPAIR_20261125/control/PLAN_IDENTITY.json`：NEW。

- `construction/plans/STARCORE_R34_2_REPAIR_20261125/FILE_CARDS.json`：NEW_FROM_152。

- `tools/r342/test_b01_contracts.py`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_001。

### 主链接入与保留

只改施工入口及验证基线，不再造生产引擎。

- 保留：295原文件卡及294个不同实际路径的既有实现
- 保留：原R33/R34/R341封印
- 保留：已验证签名和包名
- 保留：单位置K3/PIT/唯一真实入口

### 独立验收

- 新方案base==安装源码commit且所有文件卡当前blob可复核
- 原62工单/152入口都有唯一承接
- 保留清单前后哈希/行为有差分与必要回归
- 没有覆盖未授权的应用/签名文件

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 02. 新R34.2版本身份必须扩展构建链，不能直接改版本名

**编号 `R342-002`｜P0｜施工批次 B01｜证据等级 CONFIRMED_FORWARD_COMPATIBILITY_WORK_REQUIRED**

### 证据与源码定位

- `tools/r33/advance_release_version.py` ：允许series仅R33.5/R34/R34.1；R34.1使用既有原子版本journal。
- `tools/r33/verify_plan_binding.py` ：按R34.1前缀分派到r341 identity，已有binding必须对应recognized release。
- `app/build.gradle` ：构建封印series集合目前r34,r341。
- `.github/workflows/build-apk.yml` ：现有手动入口的默认job直接assembleDebug；其它发布分支绑定历史R26原件。152此文件尚未调用r33构建/封包工具，也未接R34.2身份。
- `.github/workflows/r31-publish-release.yml` ：只接受construction/r31-final-20261104与R31_PUBLISH_REQUEST；验证222/58旧验收和R31运行原件，是需保留的历史发行合同，不能改标成R34.2。

### 已证实的原因或代码机制

- 当前152已支持R34.1，但没有R34.2身份/series分派。新方案若直接让APK改名R34.2，会撞现有构建与封印约束。这是升级前置，不是152运行故障原因。

### 尚不能断言的部分

- 构建环境原签名/SDK与最高已发布版本需施工时实查；本审计未构建APK。

### 具体处理

1. 首批在原工具新增R34.2 series与单独r342方案绑定/identity，保留旧R33/R34/R341封印和验证器语义。
2. 在现有 .github/workflows/build-apk.yml 的 workflow_dispatch 增加显式R34.2分支：拟jobs.r342-candidate-build调用扩展后的tools/r33/run_release_build.py与package_ci_candidate.py，拟jobs.r342-publish仅取同source/plan/run/哈希原件发布并回下载校验。旧debug/R26分支以及r31-publish-release.yml历史合同保持；不能直接重标R31的222/58或旧版本覆盖回执。
3. 补充明确r342-plan参数及STARCORE_R342_PLAN_ROOT传递，app Gradle现有generateR334PlanIdentity/verifyFinalR334PlanIdentity纳入r342；tools/r342/build_identity.py提供拟verify_seal/identity，不改旧系列哈希。CI的未运行/跳过/取消/构建失败原件不得被旧绿灯替代。
4. versionCode从现场/manifest/发布索引最高值取max+1，四文件原子更新；不硬写153。
5. 保持当前applicationId com.starcore.research.r13与原签名，新增计划身份绑定实际152后代源码；没有APK构建则明确未执行。

### 文件与方法

- `tools/r33/advance_release_version.py`：EXTEND_R342_SERIES；方法：prepare, repo_version_lock, apply, apply_locked, main。

- `tools/r33/verify_plan_binding.py`：EXTEND_DISPATCH；方法：release_owner, identity, main。

- `tools/r33/run_release_build.py`：EXPLICIT_NEW_PLAN_ROOT；方法：build_environment, main。

- `tools/r33/package_ci_candidate.py`：VERIFY_NEW_SERIES_AND_CURRENT_INSTALLED_BASELINE；方法：validate_pair, embedded, main。

- `app/build.gradle`：GENERATE_VERIFY_NEW_IDENTITY；方法：generateR334PlanIdentity, verifyFinalR334PlanIdentity, preBuild, tasks.configureEach。

- `tools/r342/build_identity.py`：NEW_SERIES_OWNER；方法：verify_seal, identity。

- `release/r342_plan_binding.json`：NEW_BOUND_TO_REAL_PLAN_COMMIT；方法：$.series, $.planCommit, $.planSha256, $.sourceBaseline, $.sourceApkEvidenceRef。

- `.github/workflows/build-apk.yml`：EXTEND_EXPLICIT_R342_DISPATCH_BUILD_PACKAGE_AND_PUBLICATION_JOBS；方法：on.workflow_dispatch, jobs.debug-apk, jobs.r342-candidate-build, jobs.r342-publish。

- `.github/workflows/r31-publish-release.yml`：PRESERVE_HISTORICAL_R31_PUBLICATION_CONTRACT_NO_R342_RELABEL；方法：on.push, jobs.publish, jobs.publish.steps[id=req]。

- `tools/r341/publish_delivery_manifest.py`：GENERALIZE_EXPLICIT_SERIES_WITH_OLD_R341_OUTPUT_COMPATIBILITY；方法：collect, main。

- `tools/r342/test_b01_contracts.py`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_002。

### 主链接入与保留

构建身份和运行安装身份桥，不改变资格门或唯一业务入口。

- 保留：包名和原签名
- 保留：旧版本封印与工具行为
- 保留：152全部应用实现
- 保留：四文件原子版本journal

### 独立验收

- 旧系列fixtures仍通过，新R34.2有效绑定通过，错根/错祖先/错封印拒绝
- 干净source编译产物嵌入当前source/plan/APK一致
- 包名/证书相同，versionCode大于现场和发布记录
- 首批能独立构建验收，后批不反向充当前置

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 03. 启动错误分类与故障原件不足，可能把依赖或资源等待错误归为 BUG

**编号 `R342-003`｜P1｜施工批次 B02｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

启动日志后三次只记录 F01_WRITE_FAILED 与 DatabaseCoordinator.runWriteOrThrowF01 栈顶，完整 category/phase/inner cause 未直接保留。

### 证据与源码定位

- `report/runtime-observation-r341ResearchStartupJournal.txt` $.frames[34], [40], [46]：outcome=BUG; code=F01_WRITE_FAILED
- 源码 `app/src/main/java/com/ethk3/novaonepro/StartupStageJournalR341.java`，方法/成员 `classify, code, recordUncaught`，定位 135–177。
- 源码 `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`，方法/成员 `commitOutcomeName, failureCategory, operation, phase, runWriteOrThrowF01`，定位 141–152,921–933。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDispositionR31.java`，方法/成员 `classify, commitOutcomeName, consumesFaultBudget, declaredCategoryR31, failureCategory, firstTypedR31, operation, ownCategoryR31, phase, physicalCauseCategoryR31, rootCategoryR31`，定位 1–100。

### 已证实的原因或代码机制

- StartupStageJournalR341.classify 只检查每层异常 message 的首个大写 token，未优先消费 ClassifiedFailure。
- WriteRejectedR31 首 token 恒为 F01_WRITE_FAILED，typed category 在后续字段；若内层 token 不含识别关键字则默认 BUG。

### 尚不能断言的部分

- 代码存在分类失真风险已证实；当前后三次是否确属误分类，须取得真实内层 cause，不能直接认定。

### 具体处理

1. 将启动分类统一接入 FailureDispositionR31 的 typed contract，覆盖 cause 链与 FATAL/CANCELLED 优先级。
2. 日志同时保留 category、operation、phase、commitOutcome、原始原因摘要与完整原件引用；哈希用于聚合而不替代原件。
3. 将 WAIT_RESOURCE、WAIT_DEPENDENCY、MISSING_SOURCE 与 BUG 的重试/断路策略逐一绑定原 owner；不靠字符串猜测修复动作。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/StartupStageJournalR341.java`：MODIFY_OR_VERIFY；方法：classify, code, recordUncaught。

- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`：MODIFY_OR_VERIFY；方法：commitOutcomeName, failureCategory, operation, phase, runWriteOrThrowF01。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDispositionR31.java`：MODIFY_OR_VERIFY；方法：classify, commitOutcomeName, consumesFaultBudget, declaredCategoryR31, failureCategory, firstTypedR31, operation, ownCategoryR31, phase, physicalCauseCategoryR31, rootCategoryR31。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B02ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_003。

### 主链接入与保留

原启动日志和故障分类协议统一，所有 owner 共享语义。

- 保留：进程致命异常传播、取消语义、同事务不确定提交保护、三次同因 BUG 断路。

### 独立验收

- 同一 typed exception 经不同包装仍得到同一类别；schema pending 不消耗 BUG 连续计数。
- 实际 BUG 必须仍断路；报告能回溯每次保存失败的真实内部原因。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 04. writer 故障证据积压触发研究写入背压，同时挡住普通历史回补与前瞻冻结前置动作

**编号 `R342-004`｜P0｜施工批次 B02｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

当前记录出现 R914_DRAW_HISTORY_BATCH、CP12_RESERVE_PROSPECTIVE_CAPTURE、CP4_FORMAL_WINDOW_CLOSE 被 R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN 拒绝；前瞻登记 8 项而已启动计算/冻结完整清单为 0。

### 证据与源码定位

- `report/REPORT_DETAILS.txt` RSEC-012：CP12_RESERVE_PROSPECTIVE_CAPTURE
- `report/REPORT_DETAILS.txt` RSEC-078：CP4_FORMAL_WINDOW_CLOSE
- `report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[20] (runtime_event_outbox), 当前期 202611250346：R914_DRAW_HISTORY_BATCH / R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN
- 源码 `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`，方法/成员 `runWrite`，定位 88–119,994–1008。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`，方法/成员 `deferResearch, drainBatchR33, drainControlSliceR32, drainOne`，定位 20–40,147–234,292–294。
- 源码 `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`，方法/成员 `drainWriterFaultControlR334`，定位 2517–2534。

### 已证实的原因或代码机制

- 当 PENDING.size+admitting >=24，非受控服务时隙的 RESEARCH_DATA 在事务前被拒绝；这是明确的资源保护策略。
- 普通历史回补、前瞻捕获、固定窗口关闭等在现 registry 属 RESEARCH_DATA，会受同一背压条件影响；现有最小suffix使用R32_PREDRAW_SUFFIX/DRAW_INGEST，不能混同。
- 已有独立 fault drain Looper 和每次最多 4 条批量持久化/回读路径；“根本没有 drain”不成立。

### 尚不能断言的部分

- 本包不足以确认当前 drain 卡在 quota、schema、writer wait、回读或进程切换哪一步。
- 不能把全部研究动作提高为实时优先级，亦不能清空队列解除保护。

### 具体处理

1. 导出主 PID 队列 oldestAge、pending、admitting、drainStatus、lastFailure、fair-slot 等同一时点证据并确定真实堵点。
2. 保留单 writer、<=4 条/64KB 排空原件与 readback 后出队；为故障归档/配额恢复给可验证的独立受限服务时隙。
3. 逐操作审查 deadline：前瞻冻结前置使用有界服务保证；验证已有R32_PREDRAW_SUFFIX最小连续历史通道的调用与收据，普通研究维持背压。
4. pressure≥24 时暂停新增非必要研究/昂贵观测，保留最小故障原件；恢复后按原 task/cursor 重放，严禁补造历史前瞻。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`：MODIFY_OR_VERIFY；方法：runWrite。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`：MODIFY_OR_VERIFY；方法：deferResearch, drainBatchR33, drainControlSliceR32, drainOne。

- `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`：MODIFY_OR_VERIFY；方法：drainWriterFaultControlR334。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B02ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_004。

### 主链接入与保留

单 writer 的 admission/公平服务时隙/故障原件归档闭环；接回现有研究调度器。

- 保留：写锁唯一性、实时 freeze 优先、原始故障不丢、提交不确定先解析 receipt、重复提交幂等。

### 独立验收

- 压力注入令队列超过 24 后排空可恢复到阈值以下，原件计数=排队计数且无静默 capacityLoss。
- 连续开奖下主链冻结按期完成，后台研究仍能获得有界服务；无第二写连接绕过总控。
- 杀进程/COMMIT_UNKNOWN/磁盘满/迁移等待后能够识别原事件并继续，不能靠丢弃错误通过。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 05. 故障排空报告读取的是诊断进程静态队列，无法说明主运行进程已解除背压

**编号 `R342-005`｜P1｜施工批次 B02｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

R30 报告 writerHoldFaultReceipts 为 observerPid=22528、pending=0、drainStatus=IDLE、lastDrainCommitOutcome=NOT_RUN；实时 runtime 观察 PID=18685，且当前期错误明确曾被背压拒绝。

### 证据与源码定位

- `report/R30_REPORT.json` $.chapters[2].data.r31WriterProtocolObservation.writerHoldFaultReceipts：observerPid=22528; pending=0; lastDrainCommitOutcome=NOT_RUN
- `report/runtime-observation.json` $.pid / $.sampleWallMs：pid=18685; sampleWallMs=1795585570811
- 源码 `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`，方法/成员 `writeProtocolSnapshotR31`，定位 249。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`，方法/成员 `snapshot`，定位 261–288。
- 源码 `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`，方法/成员 `drainWriterFaultControlR334`，定位 2523–2531。

### 已证实的原因或代码机制

- WriterHoldFaultsR31.snapshot 读取当前 JVM 静态队列；diagnostics 进程与主进程不共享该队列。
- 源码已有 r33_main_writer_faults / pid / wall 投影，报告当前使用的结构不是主 PID 排空事实。

### 尚不能断言的部分

- 不能由诊断 PID 的零值推出主进程无故障、无队列、排空成功。
- 应检查主投影是否未产生、过期、未导出或报告未选用，不能凭报告零值确定是哪一种。

### 具体处理

1. 报告同时展示 observerPID 与 producerPID，并优先导出主进程已发布的带签名范围投影。
2. 绑定 release/boot/processEpoch/monotonic时间/validUntil；跨 PID 或过期标 UNKNOWN，绝不补零。
3. 故障排空每次持久化/回读/归档服务输出实际进度收据，后台 UI 直接消费这一事实。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`：MODIFY_OR_VERIFY；方法：writeProtocolSnapshotR31。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`：MODIFY_OR_VERIFY；方法：snapshot。

- `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`：MODIFY_OR_VERIFY；方法：drainWriterFaultControlR334。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B02ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_005。

### 主链接入与保留

主运行 owner → 带范围只读事实投影 → 诊断报告。

- 保留：诊断进程隔离、无业务副作用的报告、主进程不因报告阻塞。

### 独立验收

- 主 PID 人为积压时报告不能显示“已健康”；主进程未在线时明确 UNKNOWN。
- 同一主 PID 排空前后计数和 receipt 可对账，报告投影与原件 hash 一致。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 06. 故障冷原件已索引但只有部分做了hash和receipt核验

**编号 `R342-006`｜P1｜施工批次 B02｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

faultOriginalsR335 indexedOriginals7719、indexedOriginalBytes20151803、hashAndReceiptVerified3213、unverifiedByCompactBudget4506、unavailable0；rawBodiesIncluded=false。

### 证据与源码定位

- `report/source-manifest.json` $.faultOriginalsR335：hashAndReceiptVerified=3213; unverifiedByCompactBudget=4506
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`，方法/成员 `drainBatchR33`，定位 195–229。
- 源码 `app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java`，方法/成员 `capture`，定位 64–95。

### 已证实的原因或代码机制

- compact审计预算只验证部分；无unavailable记录不代表未验证4506项健康。

### 尚不能断言的部分

- 不能断言4506损坏或丢失；当前结论是未核验。
- 本包外部无法重算未附原件的哈希和codec。

### 具体处理

1. 原故障归档owner提供可续的hash+receipt校验游标和预算，覆盖全部所需原件。
2. 将indexed、verified、unavailable、pending明确分列，任何回收仅依赖已验证receipt与引用闭包。
3. 审计包导出需要定位的最小原件或完整RAW模式，保持诊断默认轻量。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`：MODIFY_OR_VERIFY；方法：drainBatchR33。

- `app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java`：MODIFY_OR_VERIFY；方法：capture。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B02ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_006。

### 主链接入与保留

故障原件owner→验证游标→诊断manifest。

- 保留：每个旧故障原件、热冷迁移幂等、未验证不可删。

### 独立验收

- 7719的四状态相加可对账；verified失败保留原件并显示原因。
- 完整审计重算能验证指定原件，不以索引存在代替字节验证。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 07. 研究 owner 在 checkpoint 保存阶段失败，并达到同因三次 BUG 停启条件

**编号 `R342-007`｜P0｜施工批次 B03｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

当前 sourceCommit 的研究启动 frames 25–48 包含 4 次 OWNER_BEGIN；4 次 CHECKPOINT_PREPARE 均 VERIFIED。第一次 CHECKPOINT_PRESERVE 为 WAIT_RESOURCE，后三次为 BUG，后三次 causeChainHash 均 175405ffe1660147efa56ccee5f35976d182f4a4a4470995694c4a6d046f86a6。没有后续 RUNTIME_BASE、SESSION_REGISTER 成功收据。

### 证据与源码定位

- `report/runtime-observation-r341ResearchStartupJournal.txt` $.frames[24:48]：CHECKPOINT_PRESERVE / F01_WRITE_FAILED
- `report/R30_REPORT.json` $.lifecycleEvidenceR31.startupCheckpointR31.value：NO_COMMITTED_CAS_AWARE_STARTUP_OBSERVED
- `audit/report/runtime-observation-r341ResearchStartupJournal.txt` $.frames[24:48]：[{"index": 24, "sequence": 25, "stage": "OWNER_BEGIN", "record": "BEGIN"}, {"index": 25, "sequence": 26, "stage": "CHECKPOINT_PREPARE", "record": "BEGIN"}, {"index": 26, "sequence": 27, "stage": "CHECKPOINT_PREPARE", "record": "END", "outcome": "VERIFIED"}, {"index": 27, "sequence": 28, "stage": "CHECKPOINT_PRESERVE", "record": "BEGIN"}, {"index": 28, "sequence": 29, "stage": "CHECKPOINT_PRESERVE", "record": "END", "outcome": "WAIT_RESOURCE", "code": "F01_WRITE_FAILED", "failureType": "com.ethk3.novaonepro.DatabaseCoordinator_WriteRejectedR31", "stackTop": "com.ethk3.novaonepro.WriteBudgetR341.beforeStatement", "causeChainHash": "8fa837fa9d6a1d6f16fd3e33b011a7e4ce108f94f655f6effcae6b3cbca865c0"}, {"index": 29, "sequence": 30, "stage": "OWNER_BEGIN", "record": "END", "outcome": "WAIT_RESOURCE", "code": "F01_WRITE_FAILED", "failureType": "com.ethk3.novaonepro.DatabaseCoordinator_WriteRejectedR31", "stackTop": "com.ethk3.novaonepro.WriteBudgetR341.beforeStatement", "causeChainHash": "8fa837fa9d6a1d6f16fd3e33b011a7e4ce108f94f655f6effcae6b3cbca865c0"}, {"index": 30, "sequence": 31, "stage": "OWNER_BEGIN", "record": "BEGIN"}, {"index": 31, "sequence": 32, "stage": "CHECKPOINT_PREPARE", "record": "BEGIN"}, {"index": 32, "sequence": 33, "stage": "CHECKPOINT_PREPARE", "record": "END", "outcome": "VERIFIED"}, {"index": 33, "sequence": 34, "stage": "CHECKPOINT_PRESERVE", "record": "BEGIN"}, {"index": 34, "sequence": 35, "stage": "CHECKPOINT_PRESERVE", "record": "END", "outcome": "BUG", "code": "F01_WRITE_FAILED", "failureType": "com.ethk3.novaonepro.DatabaseCoordinator_WriteRejectedR31", "stackTop": "com.ethk3.novaonepro.DatabaseCoordinator.runWriteOrThrowF01", "causeChainHash": "175405ffe1660147efa56ccee5f35976d182f4a4a4470995694c4a6d046f86a6"}, {"index": 35, "sequence": 36, "stage": "OWNER_BEGIN", "record": "END", "outcome": "BUG", "code": "F01_WRITE_FAILED", "failureType": "com.ethk3.novaonepro.DatabaseCoordinator_WriteRejectedR31", "stackTop": "com.ethk3.novaonepro.DatabaseCoordinator.runWriteOrThrowF01", "causeChainHash": "175405ffe1660147efa56ccee5f35976d182f4a4a4470995694c4a6d046f86a6"}, {"index": 36, "sequence": 37, "stage": "OWNER_BEGIN", "record": "BEGIN"}, {"index": 37, "sequence": 38, "stage": "CHECKPOINT_PREPARE", "record": "BEGIN"}, {"index": 38, "sequence": 39, "stage": "CHECKPOINT_PREPARE", "record": "END", "outcome": "VERIFIED"}, {"index": 39, "sequence": 40, "stage": "CHECKPOINT_PRESERVE", "record": "BEGIN"}, {"index": 40, "sequence": 41, "stage": "CHECKPOINT_PRESERVE", "record": "END", "outcome": "BUG", "code": "F01_WRITE_FAILED", "failureType": "com.ethk3.novaonepro.DatabaseCoordinator_WriteRejectedR31", "stackTop": "com.ethk3.novaonepro.DatabaseCoordinator.runWriteOrThrowF01", "causeChainHash": "175405ffe1660147efa56ccee5f35976d182f4a4a4470995694c4a6d046f86a6"}, {"index": 41, "sequence": 42, "stage": "OWNER_BEGIN", "record": "END", "outcome": "BUG", "code": "F01_WRITE_FAILED", "failureType": "com.ethk3.novaonepro.DatabaseCoordinator_WriteRejectedR31", "stackTop": "com.ethk3.novaonepro.DatabaseCoordinator.runWriteOrThrowF01", "causeChainHash": "175405ffe1660147efa56ccee5f35976d182f4a4a4470995694c4a6d046f86a6"}, {"index": 42, "sequence": 43, "stage": "OWNER_BEGIN", "record": "BEGIN"}, {"index": 43, "sequence": 44, "stage": "CHECKPOINT_PREPARE", "record": "BEGIN"}, {"index": 44, "sequence": 45, "stage": "CHECKPOINT_PREPARE", "record": "END", "outcome": "VERIFIED"}, {"index": 45, "sequence": 46, "stage": "CHECKPOINT_PRESERVE", "record": "BEGIN"}, {"index": 46, "sequence": 47, "stage": "CHECKPOINT_PRESERVE", "record": "END", "outcome": "BUG", "code": "F01_WRITE_FAILED", "failureType": "com.ethk3.novaonepro.DatabaseCoordinator_WriteRejectedR31", "stackTop": "com.ethk3.novaonepro.DatabaseCoordinator.runWriteOrThrowF01", "causeChainHash": "175405ffe1660147efa56ccee5f35976d182f4a4a4470995694c4a6d046f86a6"}, {"index": 47, "sequence": 48, "stage": "OWNER_BEGIN", "record": "END", "outcome": "BUG", "code": "F01_WRITE_FAILED", "failureType": "com.ethk3.novaonepro.DatabaseCoordinator_WriteRejectedR31", "stackTop": "com.ethk3.novaonepro.DatabaseCoordinator.runWriteOrThrowF01", "causeChainHash": "175405ffe1660147efa56ccee5f35976d182f4a4a4470995694c4a6d046f86a6"}]
- `audit/report/R26_REPORT.json` $.committedResearchService.sessions：{"retained_sessions": 15, "current_commit_sessions": 0}
- 源码 `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`，方法/成员 `ensureContinuousRuntimeR26`，定位 2507–2514。
- 源码 `app/src/main/java/com/ethk3/novaonepro/StartupStageJournalR341.java`，方法/成员 `hasDeterministicFailureLimit`，定位 239–252。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`，方法/成员 `initialize, prepareStartupSchemaR34`，定位 530–560,572–585。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`，方法/成员 `preservePreparedStartupCheckpoint, preserveStartupCheckpoint`，定位 225–264。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`，方法/成员 `initialize, prepareStartupSchemaR34`，定位 530。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`，方法/成员 `initialize, prepareStartupSchemaR34`，定位 469。

### 已证实的原因或代码机制

- 失败阶段确在初始化所需 checkpoint 保留事务，后续研究基座和会话登记位于它之后。
- AutoFetchService 明确在同提交连续三次同指纹 BUG 后停止新建研究 owner；报告记录满足此条件。

### 尚不能断言的部分

- 本包只有 F01_WRITE_FAILED、栈顶与原因链哈希，后三次真实内层异常全文不足；不能断言 SQL 哪一条、哪个 guard 触发。
- CAS 总体 missing 556 并不等于这次失败根因；本次 PREPARE 实际为 VERIFIED。

### 具体处理

1. 采集同版本同进程保存事务的 typed category、phase、commitOutcome、完整受限原因链及 checkpoint identity/guard 版本；在数据库副本复现。
2. 将保存动作保持于原 MIGRATION owner，读端准备、受限事务提交、提交收据回读；拆小只允许不破坏原子性/不可变原件。
3. 将资产级缺原件隔离到其消费链；未受影响基座/旧成熟模型可启动，受影响资产不参与预测/GC。
4. 保留三次确定性失败断路器，按依赖修复代次、schema 代次、源码提交变化重置可重试资格，避免永久静默和无效热循环。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`：MODIFY_OR_VERIFY；方法：ensureContinuousRuntimeR26。

- `app/src/main/java/com/ethk3/novaonepro/StartupStageJournalR341.java`：MODIFY_OR_VERIFY；方法：hasDeterministicFailureLimit。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`：MODIFY_OR_VERIFY；方法：initialize, prepareStartupSchemaR34。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`：MODIFY_OR_VERIFY；方法：preservePreparedStartupCheckpoint, preserveStartupCheckpoint。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B03ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_007。

### 主链接入与保留

启动控制 owner → MIGRATION checkpoint → 研究会话 → 原研究调度器；不另建第二研究总控。

- 保留：旧 checkpoint 全字节保存、typed equality、不可变守卫、同事务 boot/receipt、单 writer、错误断路保护。

### 独立验收

- 真实旧版本数据库升级/进程重启后出现 CHECKPOINT_PRESERVE COMMITTED 和 SESSION_REGISTER/研究 slice 实际进度。
- 同失败重复三次仍停车并显示精确原因；修复依赖后仅重启一次并推进。
- 被隔离资产保持旧身份/原件，其他研究任务与现有预测主链继续；提交不确定时不重复写。
- 同一安装 SHA 下每个启动阶段都取得 END/VERIFIED，当前会话在冷启动和进程恢复后各产生有内容的提交回执。
- 注入进程中断或 writer 忙时，游标可恢复且旧原件哈希不变；旧 owner 的 fence 不再可写。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 08. 服务心跳存在但研究健康长期陈旧，尝试时间不能替代实际进度

**编号 `R342-008`｜P1｜施工批次 B03｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

heartbeat enabled=1/alive=1/truth=RUNNING，business=RESEARCH_STALE，reason=RESEARCH_HEALTH_UNKNOWN_STALE_SAMPLE；研究样本年龄约943006ms，而attemptAge仅5ms。

### 证据与源码定位

- `report/runtime-observation-heartbeatSummary.txt` whole summary：RESEARCH_HEALTH_UNKNOWN_STALE_SAMPLE
- `report/runtime-observation.json` $.researchSchemaReady：true (schema flag only)
- 源码 `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`，方法/成员 `ensureContinuousRuntimeR26, postResearchControlR31, reconcileResearchOwner, run`，定位 2510–2512,2538–2580。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`，方法/成员 `activeHealthObservationR31, isRunning`，定位 168–170。
- 源码 `app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java`，方法/成员 `getBusinessStaleReason`，定位 838。

### 已证实的原因或代码机制

- 控制循环可继续写尝试心跳，即使研究owner未初始化完成；schemaReady和线程/服务alive不等于研究推进。
- 真实owner已因启动失败停住，后续陈旧健康样本无法证明当前可用。

### 尚不能断言的部分

- 不能用heartbeatAge很小否定研究卡住，也不能称整个服务完全未启动。

### 具体处理

1. 将进程、服务、owner就绪、task lease、cursor/receipt进度、等待依赖分栏展示。
2. 控制循环只记录attempt；实际研究slice提交才能更新progress时间与健康sample。
3. 陈旧状态触发有界恢复并带已知依赖原因；恢复条件接入RT01/RT03，不无限增加线程。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`：MODIFY_OR_VERIFY；方法：ensureContinuousRuntimeR26, postResearchControlR31, reconcileResearchOwner, run。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`：MODIFY_OR_VERIFY；方法：activeHealthObservationR31, isRunning。

- `app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java`：MODIFY_OR_VERIFY；方法：getBusinessStaleReason。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B03ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_008。

### 主链接入与保留

原 control owner观测并修复原研究owner。

- 保留：前台服务、既有watchdog、主动用户停止开关、真实心跳。

### 独立验收

- 模拟owner停住而控制循环存活，界面必须显示研究阻断并给精确原因。
- 恢复成功必须有新cursor/receipt进度，不能仅更新计时戳。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 09. 三项关键研究来源表在诊断快照缺失，部分“全局schema READY”不代表能力就绪

**编号 `R342-009`｜P1｜施工批次 B03｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

sourceHealthR341 27来源：11 OK、13 EMPTY、3 UNAVAILABLE。缺r34_cold_rebirth、r34_atomnet_optimizer_step、r34_atomnet_unavailable_label；lifecycle checkpointRecovery.schemaReady=false、startup dependencies MIGRATION_REQUIRED。

### 证据与源码定位

- `report/R30_REPORT.json` $.sourceHealthR341.sources[2:5]：no such table
- `report/R30_REPORT.json` $.lifecycleEvidenceR31.checkpointRecoveryR335.value：schemaReady=false
- `audit/report/R30_REPORT.json` $.sourceHealthR341.sources：[{"errorRef": "169e44f953597e39d1256d336728bc581549216a6020d42adc853bea5c1a2f25", "reason": "no such table: r34_cold_rebirth (code 1 SQLITE_ERROR): , while compiling: SELECT source_kind,source_id,child_id,child_campaign,target_abi,source_through FROM r34_cold_rebirth WHERE target_abi=? ORDER ", "reasonCode": "WAITING_SOURCE", "rowsReturned": null, "scope": "IMMUTABLE_REPORT_COPY", "scopeId": "9053ea179c9c8eb3319364973e99606873bf34c1deef604386fa6f06c113bbb0", "sourceId": "R34-B06-01", "sourceQueryStatus": "SOURCE_UNAVAILABLE", "state": "SOURCE_UNAVAILABLE", "totalRows": null, "totalRowsKnown": false}, {"errorRef": "c593d0968a63ccf852241b04f674947d39942b5728cfc6f2bcb5cef2757fb26c", "reason": "no such table: r34_atomnet_optimizer_step (code 1 SQLITE_ERROR): , while compiling: SELECT g.source_key,g.role,g.input_hash,g.gradient_norm,g.parameter_change,g.state_hash,p.model_hash,p.published_at ", "reasonCode": "WAITING_SOURCE", "rowsReturned": null, "scope": "IMMUTABLE_REPORT_COPY", "scopeId": "9053ea179c9c8eb3319364973e99606873bf34c1deef604386fa6f06c113bbb0", "sourceId": "R34-B07-02", "sourceQueryStatus": "SOURCE_UNAVAILABLE", "state": "SOURCE_UNAVAILABLE", "totalRows": null, "totalRowsKnown": false}, {"errorRef": "c6daf6777a24e8753b70d65cc9d44083c446ca34b5af3c6efd5421389e7de0ec", "reason": "no such table: r34_atomnet_unavailable_label (code 1 SQLITE_ERROR): , while compiling: SELECT issue,reason,first_seen_at,recorded_at FROM r34_atomnet_unavailable_label ORDER BY issue DESC LIMIT 32", "reasonCode": "WAITING_SOURCE", "rowsReturned": null, "scope": "IMMUTABLE_REPORT_COPY", "scopeId": "9053ea179c9c8eb3319364973e99606873bf34c1deef604386fa6f06c113bbb0", "sourceId": "R34-B07-01", "sourceQueryStatus": "SOURCE_UNAVAILABLE", "state": "SOURCE_UNAVAILABLE", "totalRows": null, "totalRowsKnown": false}]
- `audit/report/R30_REPORT.json` $.lifecycleEvidenceR31.atomNetPublicationCapacityR33：{"exceptionClass": "com.ethk3.novaonepro.DatabaseCoordinator$SchemaPendingR31", "fullOriginal": "verified snapshot.db / lifecycle owner tables and r31-risk-contract cache panels", "readFinishedAt": 1795587086684, "readOnly": true, "readStartedAt": 1795587086677, "sourceScope": "SEPARATE_LIFECYCLE_READS_AFTER_FACT_WATERMARK", "status": "UNKNOWN_LIFECYCLE_READ_FAILURE", "value": null}
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FieldFollowupR34.java`，方法/成员 `capture`，定位 42–44。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdRebirthSchemaR34.java`，方法/成员 `declares, require`，定位 11–28。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetPublicationR31.java`，方法/成员 `schemaObjectsR31`，定位 26–37,51。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`，方法/成员 `initialize, prepareStartupSchemaR34`，定位 530–585。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdRebirthSchemaR34.java`，方法/成员 `declares, require`，定位 11。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetPublicationR31.java`，方法/成员 `schemaObjectsR31`，定位 26。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java`，方法/成员 ``，定位 12。

### 已证实的原因或代码机制

- 诊断查询确实遇到无表，而非读取到了0行；新能力所需migration/contract未全部到位。
- 源码已有对应DDL和生产消费者，因此缺表是运行契约未完成，不是“功能没有写源码”的证明。

### 尚不能断言的部分

- 必须对照migration journal确定前置保存失败、迁移步骤未执行、回滚、版本契约不符各自原因。

### 具体处理

1. 以功能domain列出所需table/index/trigger/ABI并对照当前数据库实际对象，明确每个未完成migration步骤。
2. 依原SchemaDomainCoordinator/MIGRATION owner有界、可恢复、幂等推进，完成后读取exact schema contract。
3. 只唤醒依赖已就绪的consumer；无表/未迁移显示WAIT_DEPENDENCY，不能变0结果。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FieldFollowupR34.java`：MODIFY_OR_VERIFY；方法：capture。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdRebirthSchemaR34.java`：MODIFY_OR_VERIFY；方法：declares, require。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetPublicationR31.java`：MODIFY_OR_VERIFY；方法：schemaObjectsR31。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`：MODIFY_OR_VERIFY；方法：initialize, prepareStartupSchemaR34。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java`：MODIFY_OR_VERIFY。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B03ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_009。

### 主链接入与保留

原schema迁移owner→domain readiness→原consumer。

- 保留：旧表及原件、迁移日志、原成熟consumer；不得业务线程自行建新表绕总控。

### 独立验收

- 三表契约可验证且生产一次真实写/读回路；只建表不计能力验收。
- 中断升级/重启可续迁移，旧功能回归无丢失。
- 三源从 SOURCE_UNAVAILABLE 变为实际读成功；插入回滚、幂等迁移、旧库升级后 trigger/column 合约一致。
- 真实一次优化器更新能联查 optimizer_step→publication；缺表回归能阻断工作且不会丢旧模型。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 10. 8409 条研究任务大多只是维护，缺少真实训练和研究动作任务的可交付进度

**编号 `R342-010`｜P0｜施工批次 B03｜证据等级 CONFIRMED_TASK_KIND_GAP**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[28].rows：[{"kind": "MAINTENANCE", "status": "COMPLETED", "count": 8363}, {"kind": "MAINTENANCE", "status": "PENDING", "count": 9}, {"kind": "MAINTENANCE", "status": "YIELDED", "count": 12}, {"kind": "POST_TRAIN", "status": "COMPLETED", "count": 25}]
- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[28].rows：{"nonempty_last_error": 12, "no_train_or_freeze": true}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[5]：{"key": "r912_research_continuity_status", "value": "PERIOD_PENDING|REVIEW_ONLY|watermark=202611250346|taskWatermark=202611250331|reviewedLanes=1/22|lane=6|outcome=ERROR:WriteRejectedR31:F01_WRITE_FAILED|operation=CP4_FORMAL_WINDOW_CLOSE|category=DEPENDENCY|phase=ADMISSION|commitOutcome=ROLLED_BACK|numericEvidence=OWNER_RECEIPTS_ONLY|deviceTimingVerified=0", "updatedAt": 1795585596174, "status": "READ_OK"}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `onSettlementCompleted, pulse`，定位 356。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `onSettlementCompleted, pulse`，定位 580。

### 已证实的原因或代码机制

- 按完整任务集分组，8363 MAINTENANCE 完成、25 POST_TRAIN 完成，另有 12 YIELDED、9 PENDING，TRAIN/FREEZE/RESEARCH_ACTION 为0。调度层完成次数没有表达能力生产，reserve training 在没有 ready training 时不会生成工作。

### 尚不能断言的部分

- 目前无法仅用任务数断言 Director 从未执行；确有维护和旧在线 POST 成功。需区分没有生产者入队、任务反复被 writer 拒绝与已入队不被服务。

### 具体处理

1. 按 slot 与 kind 报告 eligible、enqueued、claimed、computed、committed、consumed、blocked 第一处缺口。
2. 修复出生/反馈→动作入队，保留六槽 Director；为真实可运行任务设有证据的最大等待时长和维护配额。
3. 计算、文件准备与原件校验在写锁外，落库用短事务与 CAS；被拒写仅延后本任务，不把空维护循环计作训练。
4. 每种 wait_reason 记录首次、末次进度和 nextTrigger，retry/backoff 不能吞没错误或无限空轮询。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`：MODIFY_OR_VERIFY；方法：onSettlementCompleted, pulse。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B03ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_010。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：既有六槽、deadline/live-pressure guard、POST 修复语义和 owner/fence。

### 独立验收

- 注入真实 ready TRAIN、POST、RESEARCH、MATRIX、EVOLVE、IMPORT，按实际 device budget 都能取得提交，主链 deadline 不恶化。
- 一个任务中途杀进程恢复无重复数值训练/结算；故障产生可定位终态或下一次触发。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 11. 研究公平服务和数值能力必须按当前版本真实提交衡量，旧会话与空推进不能验收

**编号 `R342-011`｜P1｜施工批次 B03｜证据等级 CONFIRMED_EVIDENCE_SCOPE_GAP**

### 证据与源码定位

- `audit/report/R26_REPORT.json` $.committedResearchService.scope：Committed research service receipts since telemetry installation; failed receipt commits are excluded; rows bind source/runtime/device; historical folds span sources; not P0 or a full opportunity SLO
- `audit/report/R26_REPORT.json` $.committedResearchService.sessions：{"sessions": 15, "current_source_sessions": 0}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[450]：{"table": "research_family_cursor_cp12", "rows": 15, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[111]：{"table": "r26_progress", "rows": 4097, "states": []}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java`，方法/成员 `prepare, maintain, guardComplete, register, holdCursor`，定位 163。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `pulse`，定位 580。

### 已证实的原因或代码机制

- 当前版本没有已提交研究service session；旧4097进度和cursor generation只能证明保留操作记录。ModernFamilyFactory在wait时也可更新generation，因此generation增量不是训练或新颖策略。

### 尚不能断言的部分

- 公平服务的真实上限需要同机CPU/内存/电量和主链deadline共同测量，本报告不能给出已经达到的毫秒SLO。单纯增加线程或取消预算不构成修复。

### 具体处理

1. 每个合法ready任务计eligible waiting、CPU sample处理、数值更新、事务commit与下游消费，按当前source/process分桶。
2. 采用已有scheduler公平仲裁，对大模型分片、目录导入和维护分别计预算，保证主链deadline与研究有限最大等待。
3. 新增语义明确的bookkeeping_generation、candidate_generation、optimizer_step、publication_version，不跨用途累计。
4. 把准入无效、缺输入、故障、长期饥饿与正常空闲分开，保留每种调度决定的理由及可审计分母。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java`：MODIFY_OR_VERIFY；方法：prepare, maintain, guardComplete, register, holdCursor。

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`：MODIFY_OR_VERIFY；方法：pulse。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B03ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_011。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：统一资源预算、旧性能历史、当前source作用域、主链优先级与公平调度现有基础。

### 独立验收

- 同机当前版本进行包含重启/屏幕熄灭/主链高压的连续观测，每个eligible家族都有可量化服务而非仅心跳。
- 空wait循环不增加数值训练/有效子代/质量提升指标；过去版本数据仍可读但不混入当前验收。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 12. 历史引导出现等待，多期在历史已绑定后仍未形成 canonical，逐期断点待闭合

**编号 `R342-012`｜P0｜施工批次 B04｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

RSEC-052 当前成功请求 3349 有 2 次尝试，firstError=R31_DEPENDENCY_HISTORY_BOOTSTRAP_PENDING；保留 cohort 中 202611250332–342 多期有 HISTORY_BOUND 却无 canonical，343–346 为 NO_REQUEST；347 才重新成功。

### 证据与源码定位

- `report/REPORT_DETAILS.txt` RSEC-052：R31_DEPENDENCY_HISTORY_BOOTSTRAP_PENDING
- `report/runtime-opportunity-cohort.json` $.rows, targets 202611250332..202611250347：12 requests / 2 canonical successes in declared scoped range
- 源码 `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`，方法/成员 `prepareAnalysisHistoryR364, reconcilePredrawTailRecoveryR32`，定位 4271–4293。
- 源码 `app/src/main/java/com/ethk3/novaonepro/DbStore.java`，方法/成员 `call, saveHistoryPageR32, savePredrawSuffixR32`，定位 2528–2614。
- 源码 `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`，方法/成员 `runWrite`，定位 88,96,1005–1008。
- 源码 `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`，方法/成员 `prepareAnalysisHistoryR364, reconcilePredrawTailRecoveryR32`，定位 2587–2598。
- 源码 `app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java`，方法/成员 `classifyBusinessR77, readProgressR34`，定位 260–291。

### 已证实的原因或代码机制

- 预测请求有历史连续性依赖；成功 canonical 的数量远低于已绑定/可观察期数。
- prepareAnalysisHistoryR364在连续suffix不足时安排现有quick history/backfill后抛出bootstrap等待；这是当前错误的直接源码条件。
- 普通历史批量写受writer研究背压影响，但已有R32_PREDRAW_SUFFIX注册DRAW_INGEST、不受同一研究背压判断；不得把两条路径混为一谈。

### 尚不能断言的部分

- 332–342 每一期失败是否全部同一 blocker 必须逐请求原件核验；不能把任何终态/SETTLED 事件当真正预测已结算。
- 最小suffix为何未及时满足，须核验savePredrawSuffixR32是否被调用、目标更替导致返回0、源数据冲突、网络返回不足或其他写失败；普通R914批次背压不是最小suffix失败的充分证据。
- 332–342已有HISTORY_BOUND/PIT_FROZEN证据，说明不能统一归因历史未就绪；其后计算/冻结提交/过期原因应从v57_analysis_request_r36逐期原件恢复。本DIAGNOSTIC只含该表结构、计数/状态汇总，未导出逐请求原始行。

### 具体处理

1. 以issue/target/sourceWatermark对账request→history_ready→PIT_FROZEN→compute→canonical，并导出v57_analysis_request_r36逐期attempts/first_error_code/error_code/last_stage/deadline/finished_at；PIT输入冻结不等于canonical决策冻结。
2. 沿已有savePredrawSuffixR32/DRAW_INGEST通道补齐最小连续suffix，加入入参target/base、执行/返回0原因、source conflict、写入receipt、完成后重试信号；完整历史仍由原backfill owner增量补齐，不新建第二通道。
3. 失去开奖前窗口立即标记 NO_PREDRAW，不补造历史预测；后续新期继续，不被旧缺口永久阻塞。
4. 在控制 owner 的恢复循环里检查任务实际完成/游标推进，限制相同输入的重复请求。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`：MODIFY_OR_VERIFY；方法：prepareAnalysisHistoryR364, reconcilePredrawTailRecoveryR32。

- `app/src/main/java/com/ethk3/novaonepro/DbStore.java`：MODIFY_OR_VERIFY；方法：call, saveHistoryPageR32, savePredrawSuffixR32。

- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`：MODIFY_OR_VERIFY；方法：runWrite。

- `app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java`：MODIFY_OR_VERIFY；方法：classifyBusinessR77, readProgressR34。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B04ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_012。

### 主链接入与保留

取数→最小历史连续性→唯一分析请求→开奖前 canonical；历史长任务留原后台链。

- 保留：PIT 时间边界、历史原始数据、幂等 request、不可变 canonical、单期唯一结算。

### 独立验收

- 注入历史缺页、writer背压与重启后，下一可行期能够重新生成开奖前 canonical；旧已开奖期保持真实缺失。
- 每个 eligible target 必有完整成功链或可追溯 terminal，不能靠减少 denominator 提高覆盖率。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 13. 近 30 期前瞻覆盖只有 1 期；“已有义务结算零滞后”掩盖未建义务

**编号 `R342-013`｜P0｜施工批次 B04｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

RSEC-059 eligible=30 covered=1；RSEC-052 latestDraw=202611250346/latestCanonical=347/latestSettled=331、rootCause=PREDRAW_COVERAGE_MISSING、latestDrawPredrawMissing=1，同时 settlementLagScope=EXISTING_PREDRAW_OBLIGATIONS 下 lag=0。另有5条shadow pending全部WAIT_DRAW，oldest=202611030728。

### 证据与源码定位

- `report/REPORT_DETAILS.txt` RSEC-059：covered=1
- `report/REPORT_DETAILS.txt` RSEC-052：PREDRAW_COVERAGE_MISSING
- `report/REPORT_DETAILS.txt` RSEC-306：1858
- `report/REPORT_DETAILS.txt` RSEC-078：Shadow backlog total/ready/waitDraw/lease/pitInvalid/corrupt=5/0/5/0/0/0
- 源码 `app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java`，方法/成员 `classifyBusinessR77, readProgressR34`，定位 260–291。
- 源码 `app/src/main/java/com/ethk3/novaonepro/SettlementBacklogAuditR3611.java`，方法/成员 `classify, isTerminal, scan`，定位 31–42,71–90。
- 源码 `app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java`，方法/成员 `pipelineDetail, pipelineDetailSnapshotCP4`，定位 687–719,1395–1428。
- 源码 `app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java`，方法/成员 `appendRuntimeItemR76, collectRuntime, runtimeBusinessFailedR76, runtimeFailureItemsR76`，定位 940,2614–2624。

### 已证实的原因或代码机制

- 未建立前瞻记录的目标没有形成可结算义务，因而“既有义务 lag=0”与覆盖失败同时成立。
- 1858/1863=99.73% 只描述已建 shadow 的结算完成度，不能替代全部 eligible targets 的预测覆盖率。
- SettlementBacklogAuditR3611只要关联draw_dependency_source_cp12查不到draw就分类WAIT_DRAW，没有区分尚未开奖与旧期原件缺失。

### 尚不能断言的部分

- 30期窗口与当前 release cohort 范围不同；不能把 retained history 的 119 已开奖全部当本版本运行时分母。
- 5条旧shadow为何缺draw来源及是否可从保留原件恢复须逐项核查；不能简单断言5000期裁剪就是它们缺失的原因。

### 具体处理

1. UI/报告并排列出 eligible、predrawCovered、canonicalCommitted、settled、noPredrawTerminal，独立分母与范围。
2. 主链健康门将缺预测覆盖直接判降级，并关联 RT05 的各期原因，不把 serviceAlive 当业务完成。
3. 用 issue ledger 记录每期状态转移与失败原因；恢复后仅对有真实冻结证据的义务补结算。
4. 对5条陈旧WAIT_DRAW逐项找精确开奖和原first_seen证据；可验证则沿原结算owner恢复，不可恢复则明确缺源/隔离状态，不造标签、不擦掉旧义务。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java`：MODIFY_OR_VERIFY；方法：classifyBusinessR77, readProgressR34。

- `app/src/main/java/com/ethk3/novaonepro/SettlementBacklogAuditR3611.java`：MODIFY_OR_VERIFY；方法：classify, isTerminal, scan。

- `app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java`：MODIFY_OR_VERIFY；方法：pipelineDetail, pipelineDetailSnapshotCP4。

- `app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java`：MODIFY_OR_VERIFY；方法：appendRuntimeItemR76, collectRuntime, runtimeBusinessFailedR76, runtimeFailureItemsR76。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B04ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_013。

### 主链接入与保留

唯一 issue ledger 同时驱动运行健康、结算义务和界面统计。

- 保留：已有 shadow/OOS 结算和真实交易门控，不为凑覆盖率制造预测或解锁交易。

### 独立验收

- 有 29/30 缺失的样本必须显示覆盖失败，即使已有预测全部结算。
- 统计可从同一范围期列表重算；跨版本、未来期、未开放期明确分组且有理由，不从完整机会清单抹掉。
- 5条旧WAIT_DRAW逐条有target、冻结原件、draw来源可用性、恢复或隔离结论；不能在没有开奖证据时置settled/hit，亦不能把缺源当普通下一期开奖等待。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 14. 延后证据积压 1632，当前号码/全动作曲面未落齐

**编号 `R342-014`｜P1｜施工批次 B04｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

RSEC-052 deferred=1632；当前 target347 的 RSEC-053 number=0/full=0/champion=1/selected=1；RSEC-054 10 个位置 validCellsPerPosition 全0、permutation=MISSING、PARTIAL_METADATA；RSEC-057 FULL PENDING。

### 证据与源码定位

- `report/REPORT_DETAILS.txt` RSEC-052：deferred=1632
- `report/REPORT_DETAILS.txt` RSEC-053：full
- `report/REPORT_DETAILS.txt` RSEC-054：PARTIAL_METADATA
- `report/REPORT_DETAILS.txt` RSEC-057：pending
- 源码 `app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java`，方法/成员 `call, claim, writeOneInternal`，定位 232–265。
- 源码 `app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java`，方法/成员 `call, claim, writeOneInternal`，定位 197–221。
- 源码 `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`，方法/成员 `runWrite`，定位 96,1005–1008。
- 源码 `app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java`，方法/成员 `persistedNumberSurfaceR80`，定位 1087–1114。

### 已证实的原因或代码机制

- immutable snapshot 已冻结不等于 100 个概率单元/1200 动作及关联投影已异步持久化。
- deferred claim 与材料化仍走 RESEARCH_DATA，受背压与服务时隙影响；claim 还需读 snapshot_blob。

### 尚不能断言的部分

- 不能断言 1632 每条均当前版本新增或都被同一原因挡住；需要 status/age/lastError/lease/cursor 分组。
- 线程快照中的 Cursor 读取只是当时位置，不能据此断言大 BLOB 读取独占全部耗时。

### 具体处理

1. 按 job 年龄/status/lease/原件hash 建立 backlog 列表，先处理当前期及必要证据，老任务按原游标有界推进。
2. 保持原 frozen snapshot，读端预解码/分块，事务仅写有界行与游标；使用原 writer 公平服务时隙。
3. 将 core complete 与 full evidence complete 区分展示；只有100单元/1200动作 identity/校验完整才置 full完成。
4. 对过期 lease、retry等待、失败分类和提交不确定统一恢复，不生成替代预测。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java`：MODIFY_OR_VERIFY；方法：call, claim, writeOneInternal。

- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`：MODIFY_OR_VERIFY；方法：runWrite。

- `app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java`：MODIFY_OR_VERIFY；方法：persistedNumberSurfaceR80。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B04ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_014。

### 主链接入与保留

canonical 后原 deferred owner 处理证据侧链，完成事实回投 issue ledger。

- 保留：immutable frozen bytes、core/full分层、分块游标、完整证据链与原错误。

### 独立验收

- 现有1632作业逐状态可解释，成功排空的每条有100/1200对账与最终receipt；新增速率下无长期无界堆积。
- 杀进程、重试和重复回读不重复动作行、不改变冻结原件；实时主链仍满足时限。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 15. 一秒采集目标漏时隙 444，当前调度稳定性未达证明

**编号 `R342-015`｜P1｜施工批次 B05｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

当前服务实例 targetSlots=943、wakes=510、missed_tick=444，其中434为 NORMAL_OR_PREVIOUS_WORK_OVERRUN、10为 HTTP_FAILURE_OR_429_BACKOFF，earlyExplicitWakes=11。

### 证据与源码定位

- `report/runtime-observation.json` $.fetchCadenceR31 (JSON string)：targetPeriodMs=1000; missed_tick=444; targetSlots=943
- `report/runtime-observation-threadStacks.txt` fetch thread：ApiClient.requestGet → fetchLatestRealtime → runFetchChannelOnce
- 源码 `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`，方法/成员 `beginSession, endSession, nextDelay, runFetchChannelOnce, scheduleNextFetchLoop, snapshot, wake`，定位 642–665。
- 源码 `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`，方法/成员 `beginSession, endSession, nextDelay, runFetchChannelOnce, scheduleNextFetchLoop, snapshot, wake`，定位 2944–3012。
- 源码 `app/src/main/java/com/ethk3/novaonepro/ApiClient.java`，方法/成员 `requestGet, requestGovernedLatest`，定位 556,1021。

### 已证实的原因或代码机制

- 计时器按单调时间统计跳过时隙；未补发突发请求是合理保护。
- 当前实现每轮 HTTP 与后续工作完毕才安排下一轮；现场 fetch thread 正在 socketRead。

### 尚不能断言的部分

- 434 的归因是合并标签，无法分开网络、历史导入、数据库读写、callback、调度迟到；一张栈不证明全由 HTTP 导致。
- missed/slots 不等于开奖覆盖率或设备24h SLO。

### 具体处理

1. 在原 single-flight fetch owner 增加 scheduleDelay、HTTP、decode、headRead、ingest、postcallback 的分段单调时间。
2. 最小取数回调先写必要 draw/发主链事件；历史导入/状态构建交回已有后台 owner，限制每轮预算。
3. 按服务接口协议配置总超时、取消和合理失败退避；保持单请求与不突发补轮。
4. 以真实设备连续运行/网络切换对照分段数据找最大瓶颈，优化后复测。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`：MODIFY_OR_VERIFY；方法：beginSession, endSession, nextDelay, runFetchChannelOnce, scheduleNextFetchLoop, snapshot, wake。

- `app/src/main/java/com/ethk3/novaonepro/ApiClient.java`：MODIFY_OR_VERIFY；方法：requestGet, requestGovernedLatest。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B05ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_015。

### 主链接入与保留

原 fetch loop 与原 backfill 分工，输入仍进入唯一 draw writer。

- 保留：single-flight、失败/429退避、primary/backup取数、断网本地分析、用户停止取消。

### 独立验收

- 连续样本按同一serviceEpoch输出 targetSlots/wakes/missed与每段P95/P99；不存在不能解释的长阻塞。
- 网络失败时不中断本地下一期主链，恢复后请求不会叠加或瞬时追发。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 16. 分析 PASS 只有 1 个有效计时样本，不能证明实时出号整体不卡

**编号 `R342-016`｜P1｜施工批次 B05｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

RSEC-052 analysisP95=2804ms、performanceState=PASS，但 scope=ANALYSIS_DURABLE_RETURNED_R923、performanceMeasured=1、window=2、unmeasured=1；lifetime P95=10616ms 为累计历史范围。

### 证据与源码定位

- `report/REPORT_DETAILS.txt` RSEC-052：performanceMeasured=1
- `report/runtime-opportunity-cohort.json` $.settlementTimingOrigin：authoritative timing source unavailable
- 源码 `app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java`，方法/成员 `applyPerformanceR80, measuredDurableR80`，定位 9,173–185。
- 源码 `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`，方法/成员 `dispatchResearchCaptureR946, run`，定位 4466。

### 已证实的原因或代码机制

- applyPerformanceR80仅要求sample>0且P95<=threshold即PASS，没有足量样本门；当前PASS仅支持一个已测量事件，不能当总体SLO验收，遗漏事件和排队/请求缺失不能自动当0ms。
- 报告已有明确 scope，若汇总忽略样本量会产生错误验收结论。

### 尚不能断言的部分

- 本包不能独立证明完整端到端“收到新开奖→出下一期结果→UI可见”或“结算后学习”时延。
- 10616ms不能直接归当前152版；2804ms也不能代表全体成功/失败。

### 具体处理

1. 统一 issue/request trace，记录 fetch received、historyReady、analysis queued/start、freeze durable、UI applied、settle/learn完成。
2. 分段统计并单列 timeout/failed/missing，不只统计成功快样本。
3. 建立足够自然期和受控异常场景的完整序列，对标方案约定P95/P99及最大值；报告展示有效样本和缺测原因。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java`：MODIFY_OR_VERIFY；方法：applyPerformanceR80, measuredDurableR80。

- `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`：MODIFY_OR_VERIFY；方法：dispatchResearchCaptureR946, run。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B05ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_016。

### 主链接入与保留

同一 issue trace 横跨原 owner，仅增加有界观测，不新增业务路径。

- 保留：真实 durable-return计时、版本/进程范围、失败样本和主链唯一标识。

### 独立验收

- 报告不可因 sample=1 宣布整链验收通过；所有期可重算端到端/各段时延。
- 优化后证明没有把慢工作移出计时范围、删除失败样本或提前回报成功。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 17. 界面存在可测掉帧，但目前缺少定位每次卡顿的完整因果链

**编号 `R342-017`｜P1｜施工批次 B05｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

frameSummary: frames1507、slow33=7、severe50=4、max176.004ms；scrollFrames763、slow33=2/severe50=2；truthBindMax32.661ms。

### 证据与源码定位

- `report/runtime-observation.json` $.frameSummary：R914_FRAME|frames=1507|slow33=7|severe50=4|maxMs=176.004
- `report/runtime-observation-threadStacks.txt` main thread：snapshot shows main looper polling
- 源码 `app/src/main/java/com/ethk3/novaonepro/FrameMetricsR906.java`，方法/成员 `buildSummary, doFrame`，定位 43–61,85–101。
- 源码 `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`，方法/成员 `runWrite`，定位 961–963。
- 源码 `app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceSnapshotR36.java`，方法/成员 `parseDoubles`，定位 984。
- 源码 `app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java`，方法/成员 `freezeChampion`，定位 43。

### 已证实的原因或代码机制

- Choreographer回调间隔统计已确认存在>50ms间隔，最大176.004ms；这是界面不顺畅的支持证据，不等同单次render函数耗时。
- 主线程单次快照空闲不能排除之前的卡顿；背景 parse/SQLite stack 不能直接归为 UI 卡顿原因。

### 尚不能断言的部分

- 需关联具体帧的线程 CPU/GC/IO/锁/布局/绑定时间，不能仅凭单次stack断言网络或大列表。

### 具体处理

1. 在可复现真机交互上关联 frame id 与主线程调度、GC、DB读/锁和 truth bind的分段观测。
2. 重用不可变结果快照并做差量绑定/分页，避免主线程读库、模型计算、全列表反复重建。
3. 背景解析与计算按真实资源预算运行，重复解码可缓存到同hash只读对象，资源压力时释放可重建缓存。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/FrameMetricsR906.java`：MODIFY_OR_VERIFY；方法：buildSummary, doFrame。

- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`：MODIFY_OR_VERIFY；方法：runWrite。

- `app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceSnapshotR36.java`：MODIFY_OR_VERIFY；方法：parseDoubles。

- `app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java`：MODIFY_OR_VERIFY；方法：freezeChampion。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B05ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_017。

### 主链接入与保留

主链只读投影→UI，优化消费和渲染，不让UI成为业务触发器。

- 保留：已有完整图表、历史筛选、中文解释卡片、实时状态内容；不通过删除功能缩短页面。

### 独立验收

- 关键页面滚动/切换/更新与报告并行运行完成既有功能回归，并报告帧P95/P99/最大值与慢帧数量。
- 无主线程写库；缓存命中/失效不改变预测或解释内容。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 18. 后台权限当前可用，24h/72h持续运行与设备恢复尚未验证

**编号 `R342-018`｜P1｜施工批次 B05｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

Android34/VivoV2353A观察: battery exemption YES、exact alarm YES、foregroundService YES、notifications YES、backgroundRestricted NO、powerSave NO、deviceIdle NO；状态CAPABILITIES_READY_DEVICE_SOAK_NOT_VERIFIED，continuous24h/72h均NOT_RUN。

### 证据与源码定位

- `report/runtime-observation-backgroundRuntimeCapabilityR31.txt` $.runtimeOperatingState：continuous24h=NOT_RUN; continuous72h=NOT_RUN
- `report/R30_REPORT.json` $.physicalDevice：NOT_RUN_OR_RECEIPT_UNAVAILABLE
- 源码 `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`，方法/成员 `runtimeOperatingStateR31`，定位 160。
- 源码 `app/src/main/java/com/ethk3/novaonepro/BackgroundRuntimeContractR922.java`，方法/成员 `evaluate`，定位 1–20。
- 源码 `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`，方法/成员 `startWork`，定位 2498–2504。

### 已证实的原因或代码机制

- 权限能力与长时间持续效果是不同证据；本机采样时能力具备，但设备级验收未提交。

### 尚不能断言的部分

- 没有证据证明卡顿或中断由缺通知/电池白名单造成。
- Android force-stop/OEM行为不能承诺强行绕过，API34的某些停止来源无法精确观察。

### 具体处理

1. 把现有启动/前台服务/网络恢复/电池状态纳入同一service generation，失败或用户停止分别处理。
2. 在目标真机执行24h/72h屏息、锁屏、网络切换、低电、更新重启等矩阵，逐期记录取数/冻结/结算/学习覆盖。
3. 用户force-stop后保留安全停止边界；用户重开时读取真实durable队列恢复并展示停机区间。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`：MODIFY_OR_VERIFY；方法：runtimeOperatingStateR31。

- `app/src/main/java/com/ethk3/novaonepro/BackgroundRuntimeContractR922.java`：MODIFY_OR_VERIFY；方法：evaluate。

- `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`：MODIFY_OR_VERIFY；方法：startWork。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B05ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_018。

### 主链接入与保留

现有前台服务+watchdog+单一generation恢复，不另建竞争保活入口。

- 保留：现有服务保活、设备权限引导、用户开关、后台本地学习与断网恢复。

### 独立验收

- 真实设备连续证据与issue覆盖完整，不能用声明权限、线程存在或编译通过替代。
- 每种恢复均无重复owner、重复冻结/结算/学习；明确记录因系统或用户导致的停机。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 19. 崩溃/长卡记录跨版本且滚动窗口不完备，不能直接算本版本故障或零故障

**编号 `R342-019`｜P2｜施工批次 B05｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

crashCount=2/oomCount=0；lastAt=1795460756142早于本次启动1795584625xxx；main stall6719ms标记旧R34-B12-577311122。windowCrashCount10m=0同时注明NO_ROLLING_TIMESTAMP_LOG。

### 证据与源码定位

- `report/runtime-observation-crashSummary.txt` lastAt / windowCrashCountScope / stall version：NO_ROLLING_TIMESTAMP_LOG;UNKNOWN_WHEN_RECENT_EVENT
- 源码 `app/src/main/java/com/ethk3/novaonepro/CrashRecorder.java`，方法/成员 `detailedSummary, persistOomLowAllocation`，定位 150–180。

### 已证实的原因或代码机制

- 持久化崩溃汇总包含升级前历史；当前10m窗口缺逐次时间原件。

### 尚不能断言的部分

- 没有当前152版新ANR/OOM复现证据；历史deferred异常应作为回归场景保留。
- windowCrashCount10m=0不能证明完整10m零崩溃，尤其数据源不具滚动明细。

### 具体处理

1. 每条crash/ANR/stall附release/sourceCommit/processEpoch/bootId/单调与墙钟时间及原件哈希。
2. 采用有界逐次索引与受保护原件，滚动窗口从真实事件重算；旧事件明确历史。
3. 用历史deferred故障重放并校验已修改逻辑；当前版只有实测才能提升通过状态。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/CrashRecorder.java`：MODIFY_OR_VERIFY；方法：detailedSummary, persistOomLowAllocation。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B05ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_019。

### 主链接入与保留

统一故障原件→同范围观测→报告，不参与业务状态的伪恢复。

- 保留：旧故障原件、捕获错误信息、OOS/主链数据，不因版本升级清空证据。

### 独立验收

- 跨版本汇总能一键分离；数据缺失显示unknown。
- 当前版新故障及历史回归可定位到具体运行身份。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 20. 全部 15 个现代策略家族被全量 4200 分子目录完成条件串联阻塞

**编号 `R342-020`｜P0｜施工批次 B06｜证据等级 CONFIRMED_GLOBAL_DEPENDENCY_BARRIER**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[32]：{"table": "research_atom_catalog_import_cp12", "exportedRows": 1, "totalRows": 1, "complete": true, "first_rows": [{"asset_sha256": "57673f6000cd5db5ae4be162430086753c879b8c63d5ef1d2bb521169cdcdefe", "asset_name": "cp4_atom_registry.jsonl", "expected_rows": "25350", "expected_bytes": "10416375", "next_row": "25350", "next_offset": "10416375", "prefix_sha256": "57673f6000cd5db5ae4be162430086753c879b8c63d5ef1d2bb521169cdcdefe", "sha_state": "[BINARY_PAYLOAD_NOT_INCLUDED bytes=111]", "generation": "267", "state": "COMPLETE", "updated_at": "1795579806859"}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[33]：{"table": "research_molecule_import_cp12", "exportedRows": 1, "totalRows": 1, "complete": true, "first_rows": [{"source_hash": "b4ab9a61e2a36a4a426d0588ffb52350a25c09165273f802b9aa4c63e3c726ad", "asset_name": "research/cp4_molecule_seeds.jsonl", "expected_rows": "4200", "next_row": "0", "state": "IMPORTING", "generation": "0", "updated_at": "1795579896518"}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[34]：{"table": "research_molecule_import_checkpoint_cp12", "exportedRows": 1, "totalRows": 1, "complete": true, "first_rows": [{"source_hash": "b4ab9a61e2a36a4a426d0588ffb52350a25c09165273f802b9aa4c63e3c726ad", "asset_name": "research/cp4_molecule_seeds.jsonl", "expected_rows": "4200", "expected_bytes": "3265020", "legacy_target_row": "0", "scan_row": "900", "scan_offset": "570690", "scan_prefix": "4984b3976e7190483ac4175f9a225e0003519d68b129801f371f2719fb5b2140", "scan_sha_state": "[BINARY_PAYLOAD_NOT_INCLUDED bytes=111]", "material_row": "0", "material_offset": "0", "material_prefix": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "material_sha_state": "[BINARY_PAYLOAD_NOT_INCLUDED bytes=111]", "generation": "36", "state": "SCANNING", "updated_at": "1795584636309"}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[35].rows：{"families": 15, "all_next_variant_zero": true}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[449]：{"table": "research_family_creation_cp12", "rows": 0, "states": []}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ModernSeedFactoryCP12.java`，方法/成员 `completeCP12, maintain`，定位 154。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java`，方法/成员 `prepare, maintain, guardComplete, register, holdCursor`，定位 173。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `runDirectorSlotR65, drainImportSliceCP12`，定位 326。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `runDirectorSlotR65, drainImportSliceCP12`，定位 367。

### 已证实的原因或代码机制

- 原子目录已经 COMPLETE 25350/25350；分子目录仍 SCANNING 900/4200、material_row=0。ModernSeedFactory 仅在全分子 complete 后调用家族创建，ModernFamilyFactory 再次全局 guardComplete，导致只需已验证原子/原始历史的家族也无法开始。

### 尚不能断言的部分

- 扫描缓慢的全部上游耗时须由每页耗时和 writer 排队解释；目录哈希验证不能取消。未验证的分子不得提前成为训练输入。

### 具体处理

1. 在同一 Director IMPORT 槽拆出按家族依赖判定的 readiness：原始历史/已验证原子、指定分子、合法教师分别满足。
2. 已具备输入的 FREQUENCY、MARKOV 等基础家族可以先 prepare/commit；依赖未完成分子或教师的家族只局部等待。
3. 目录继续使用完整 SHA、prefix、offset、generation CAS 的有界扫描和物化；完整校验后的对象才可发布引用。
4. 用 wait_dependency、first_wait_at、last_progress_at 和 ready-family 饥饿时间衡量进度，不用 import 调用次数代替出生。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`：MODIFY_OR_VERIFY；方法：runDirectorSlotR65, drainImportSliceCP12。

- `app/src/main/java/com/ethk3/novaonepro/r952/ModernSeedFactoryCP12.java`：MODIFY_OR_VERIFY；方法：completeCP12, maintain。

- `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java`：MODIFY_OR_VERIFY；方法：prepare, maintain, guardComplete, register, holdCursor。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B06ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_020。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：已完成原子目录及逐页完整性校验；全量种子原件；既有单 Director 和统一写入。

### 独立验收

- 故意让分子目录停在 900 行时，一个不依赖该目录的合法家族产生训练任务；需要分子的家族仍保持明确 WAIT。
- 恢复扫描后恰好物化 4200 个去重身份，SHA 不一致必须隔离；重启不丢 offset、不重复出生。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 21. 历史库有缺口，现代快训、回放和多周期研究的有效连续输入必须逐家族判断

**编号 `R342-021`｜P1｜施工批次 B06｜证据等级 CONFIRMED_DATA_GAPS_WITH_CONDITIONAL_IMPACT**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[0].rows：{"rows": 5000, "first_issue": "202611130022", "last_issue": "202611250346", "gap_intervals": 37, "missing_scheduled_issues": 12605, "consecutive_tail": 872, "last_gaps": [["202611230247", "202611230257", 9], ["202611230302", "202611230306", 3], ["202611230322", "202611231105", 782], ["202611231167", "202611231440", 272], ["202611240906", "202611240915", 8]]}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java`，方法/成员 `prepare, prepareBatch, commitFeaturePage`，定位 298。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java`，方法/成员 `prepare, prepareBatch, commitFeaturePage`，定位 175。

### 已证实的原因或代码机制

- 导出5000行并不等于5000连续期：本区间37处断档，按日1440机会计算缺12605期，末端连续872期。某家族需要的 history+训练+embargo+验证长度超出合法连续段时，只能等待/换合法输入；不能跨缺口拼接伪连续。

### 尚不能断言的部分

- 不是所有家族都被872限制：所需长度由具体协议决定；缺口是否源站漏发、抓取丢失或本地保留造成，须逐gap查 provenance/backfill原件。历史回填不能变成当年未知时刻的真实未来观察。

### 具体处理

1. 给每个输入协议计算精确所需连续长度、实际可用长度、首个断点和最早合法使用期。
2. 使用现有历史导入/回填路径补足所需连续段并验证 draw hash/来源；draws 热表继续最多5000期，较长研究仅保留原协议必需的已冻结依赖/证据；留存 first_seen 与 availability，不倒填未来冻结时刻。
3. 允许真正支持稀疏时间序列的算法显式带 gap mask；要求连续窗口的算法必须重新分段或等待。
4. 回放/长摘要遇gap采用已有新epoch边界并保存桥接原因，不把样本重复或重叠 horizon 计独立质量。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java`：MODIFY_OR_VERIFY；方法：prepare, prepareBatch, commitFeaturePage。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B06ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_021。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：已有5000开奖、原始来源/first_seen、long epoch gap记录、不可回溯伪造未来证据边界。

### 独立验收

- 构造一个确切缺口，所有要求连续窗口的家族拒绝跨越；修复到该协议长度后自动恢复。
- 回填仅增加历史训练可用样本，不增加正式独立未来 n；数据时点审计可复现。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 22. 历史快速训练在输入登记、协议和任务注册之前就中断，尚未到可归因的数值训练阶段

**编号 `R342-022`｜P0｜施工批次 B06｜证据等级 CONFIRMED_EMPTY_PRODUCTION_CHAIN**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[530]：{"table": "research_training_input_v2", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[527]：{"table": "research_training_attachment_cp12", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[532]：{"table": "research_training_protocol_v2", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[528]：{"table": "research_training_feature_cursor_cp12", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[524]：{"table": "research_task_checkpoint_v2", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[437]：{"table": "research_development_receipt_v2", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[392]：{"table": "research_artifact_identity_v2", "rows": 0, "states": []}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java`，方法/成员 `prepare, prepareBatch, commitFeaturePage`，定位 16。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `commitPreparedCandidateCP5, commitCandidateBatchCP11, pulse`，定位 527。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchTrainingHookRegistryR952.java`，方法/成员 `drainTrainingSlotCP11, drainTrainingKindR65, prepareCandidateBatchCP11`，定位 137。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchTrainingHookRegistryR952.java`，方法/成员 `drainTrainingSlotCP11, drainTrainingKindR65, prepareCandidateBatchCP11`，定位 150。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchTrainingHookRegistryR952.java`，方法/成员 `drainTrainingSlotCP11, drainTrainingKindR65, prepareCandidateBatchCP11`，定位 189。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java`，方法/成员 `step, checkpoint, restore, finish`，定位 106。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java`，方法/成员 `step, checkpoint, restore, finish`，定位 202。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java`，方法/成员 `step, checkpoint, restore, finish`，定位 213。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java`，方法/成员 `step, checkpoint, restore, finish`，定位 201。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `commitPreparedCandidateCP5, commitCandidateBatchCP11, pulse`，定位 482。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `commitPreparedCandidateCP5, commitCandidateBatchCP11, pulse`，定位 412。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `commitPreparedCandidateCP5, commitCandidateBatchCP11, pulse`，定位 306。

### 已证实的原因或代码机制

- 完整导出的现代 input/attachment/protocol/feature cursor/checkpoint/development receipt/artifact identity 均为 0；这证明该链没有形成已登记生产事实。源码已有训练适配器，第一处缺件先在 candidate→immutable input→protocol→durable train task，而非可直接断言“训练算法太慢”。

### 尚不能断言的部分

- R26 历史训练与 R952 LEGACY_ONLINE 的真实数值更新仍存在。实际 CPU/GPU 数值吞吐须在注册恢复后测量，当前无法用 0 任务测出性能。

### 具体处理

1. 将输入准备放在锁外，固化原始样本序列、来源可用时刻、特征定义、train/select 时间范围、协议和容量预算。
2. 由 commitPreparedCandidateCP5 一个原子事务登记 candidate/input/protocol/action/训练任务，不允许半个候选长期悬空。
3. 用现有 DevelopmentProductionCP12 分块读取、训练、checkpoint 与恢复；每片记录处理样本、优化器步和前后状态 hash。
4. 快训优先消耗合法历史，实时流进入独立 online 更新；正式未来冻结后参数变化创建新版本，不回写冻结对象。
5. 先量出端到端样本吞吐、p95 训练片耗时和主链尾延迟，再调整 batch、缓存和并发；不能仅放宽时间预算。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchTrainingHookRegistryR952.java`：MODIFY_OR_VERIFY；方法：drainTrainingSlotCP11, drainTrainingKindR65, prepareCandidateBatchCP11。

- `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java`：MODIFY_OR_VERIFY；方法：step, checkpoint, restore, finish。

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`：MODIFY_OR_VERIFY；方法：commitPreparedCandidateCP5, commitCandidateBatchCP11, pulse。

- `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java`：MODIFY_OR_VERIFY；方法：prepare, prepareBatch, commitFeaturePage。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B06ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_022。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：历史训练记录、train-only 统计变换、PIT、数值 checkpoint、现有资源预算与单任务 owner。

### 独立验收

- 每个测试家族有连续可联查的 input→protocol→TRAIN_CANDIDATE→数值 checkpoint→development receipt→artifact identity。
- 同份输入在进程中断后恰好继续，最终模型 hash/样本计数一致；训练不会读取未来/验证标签用于优化。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 23. 回放数据集没有给研究子任务持续提供可用输入，进化被依赖链挡住

**编号 `R342-023`｜P0｜施工批次 B06｜证据等级 CONFIRMED_PERSISTED_DEPENDENCY_WITH_CURRENT_STARTUP_BLOCK**

### 证据与源码定位

- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.operatingSummary.tasks：{"foundry-su2": {"blocked_reason": "PublicationDependencyR31:R31_MODEL_PUBLICATION_DEPENDENCY:R33_WAIT_WRITER_HOLD_FAULT_BACKLOG", "cursor": 4119, "status": "BLOCKED", "steps": 408, "task_id": "foundry-su2"}, "r27-evolution": {"blocked_reason": "DEPENDENCY:DEPENDENCY(DEPENDENCY(r27-replay-dataset))", "cursor": 13, "status": "BLOCKED", "steps": 4, "task_id": "r27-evolution"}, "r27-replay-dataset": {"blocked_reason": "DEPENDENCY:DEPENDENCY(DEPENDENCY(WRITER_HOLD_FAULT_BACKLOG))", "cursor": 3597847, "status": "BLOCKED", "steps": 3, "task_id": "r27-replay-dataset"}}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[150]：{"table": "r27_replay_dataset", "rows": 3, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[151]：{"table": "r27_replay_score", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[135]：{"table": "r27_evolution_cursor", "rows": 1, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[138]：{"table": "r27_generation", "rows": 0, "states": []}
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`，方法/成员 `DatasetTask.step`，定位 234。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`，方法/成员 `DatasetTask.step`，定位 241。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java`，方法/成员 ``，定位 106。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java`，方法/成员 `appendAdmitted, loadCurrent, require`，定位 59。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java`，方法/成员 `appendAdmitted, loadCurrent, require`，定位 41。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java`，方法/成员 `appendAdmitted, loadCurrent, require`，定位 52。

### 已证实的原因或代码机制

- 保留任务的replay-dataset因writer fault backlog阻断，evolution记录DEPENDENCY(r27-replay-dataset)，foundry也受同类门槛。已有3个dataset不等于当前ABI、当前数据和引用pin完整，旧游标不证明新version活跃。

### 尚不能断言的部分

- 共享owner reason是定位起点，不是每个下游算法根因。需修复当前启动后对确切数据集hash、内容和pin重放。

### 具体处理

1. 继续使用DatasetTask的不可变dataset与ReplayKnowledge发布；锁外准备、校验、pin，短事务原子提交后才对worker可见。
2. 在既有writer仲裁中给数据集生产可量测的公平服务机会；先正常drain持久fault backlog，不能简单移除deferResearch保护。
3. 把evolution等待映射到具体source hash与缺件，数据集发布后唤醒exact依赖，避免重复全量重建。
4. 保留数据/模型ABI绑定，旧dataset能只读回放则保留；不相容时建立新identity，不篡改旧source。
5. 在本输入生产批次承接R01–R18的dataset/student/parent/model就绪：按每方向原合同发布已pin且ABI一致的输入或specific WAIT；不能留到B11打开图表时才补数据集、训练学生或构造父件。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`：MODIFY_OR_VERIFY；方法：DatasetTask.step。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java`：MODIFY_OR_VERIFY；方法：appendAdmitted, loadCurrent, require。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java`：MODIFY_OR_VERIFY。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B06ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_023。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：现有不可变dataset、commit后发布、ReplayKnowledge、writer保护与历史dataset。

### 独立验收

- 同一完整dataset publication下，回放score和evolution能够分别推进；真实writer压力时主链优先且研究最大等待可测。
- 回放样本、角色、gap与pin可复核；异常只有受影响dataset等待，其余可用任务继续。
- 每方向输入条件有真实owner、immutable输入hash和下一次触发；B11仅可读取这些已提交事实，不反向阻塞输入生产。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 24. 50 个旧候选与 12 个专家实验被 ABI 保全后，没有完成独立新身份的冷启再训练

**编号 `R342-024`｜P0｜施工批次 B07｜证据等级 CONFIRMED_BLOCKED_RETAINED_OBJECTS**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[81]：{"table": "r26_candidate", "rows": 50, "states": [{"column": "status", "value": "BLOCKED_WARM_NUMERICAL_ABI_PRESERVED", "rows": 50}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[94]：{"table": "r26_expert_experiment", "rows": 12, "states": [{"column": "status", "value": "BLOCKED_EXPERT_ABI_PRESERVED", "rows": 12}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[121]：{"table": "r26_training_step", "rows": 3453, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[118]：{"table": "r26_statistic_training_step", "rows": 714, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[96]：{"table": "r26_feedback", "rows": 5030, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[111]：{"table": "r26_progress", "rows": 4097, "states": []}
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java`，方法/成员 `step`，定位 103。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdRebirthR34.java`，方法/成员 `step`，定位 208。

### 已证实的原因或代码机制

- 所有 50 个候选为 BLOCKED_WARM_NUMERICAL_ABI_PRESERVED，12 个专家为 BLOCKED_EXPERT_ABI_PRESERVED；当前冷启登记读源又缺表。为保护旧原件而阻断是正确安全边界，但新兼容子代没有形成可消费结果。

### 尚不能断言的部分

- 不能由旧候选 status 推出原算法本身无效；3453 训练步、714 统计训练步和 5030 反馈应保留。报告不含旧数值 payload，逐个可迁移性还需原件重放。

### 具体处理

1. 以旧资产 identity、state/checkpoint hash、source ABI 建立迁移清单，能字节级复现才允许兼容恢复。
2. 不相容对象保留父身份，走 ColdRebirthR34 创建当前 ABI 的独立子身份，清空新子代质量证据而非清空父历史。
3. 把新子代接回真实 TRAIN/SELECT、未来登记与资格路径；异常对象局部隔离，不全局挂死 foundry。
4. 为每个父子转换记录父原件、子初始化、训练输入、数值更新和独立质量起点，防止借用父的未来样本。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java`：MODIFY_OR_VERIFY；方法：step。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdRebirthR34.java`：MODIFY_OR_VERIFY；方法：step。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_024。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：原有 F01–F24、E01–E16 实现、旧 checkpoint 与既往训练/反馈；禁止批量删除重置。

### 独立验收

- 逐一列出 50 候选和 12 专家迁移终态、局部等待原因或合法子身份，不出现无解释沉默。
- 冷启子代实际权重/状态变化、训练 receipt 与下游消费可联查；断电恢复不重复建立同一子身份。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 25. 固定 40 个基础变体没有闭合为持续发现新结构的方法与算法生产

**编号 `R342-025`｜P1｜施工批次 B07｜证据等级 CONFIRMED_FOUNDATION_LIMIT_AND_UNPROVEN_CONTINUATION**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[449]：{"table": "research_family_creation_cp12", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[384]：{"table": "research_action_v2", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[444]：{"table": "research_evolution_event_r930", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[371]：{"table": "r975_evolution_review", "rows": 0, "states": []}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java`，方法/成员 `prepare, maintain, guardComplete, register, holdCursor`，定位 10。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java`，方法/成员 `prepare, maintain, guardComplete, register, holdCursor`，定位 161。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/EvolutionControllerR952.java`，方法/成员 `drainActionCP12, claimEvolution`，定位 15。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java`，方法/成员 ``，定位 130。

### 已证实的原因或代码机制

- ModernFamilyFactory 的基础空间是 10 位置×4 窗口，游标到 40 返回 FAMILY_FOUNDATION_COMPLETE。源码存在结构提案、现代 graph 变异和融合，不能说软件只会换参数；但当前 action/creation/evolution 生产与消费没有数据，有限基础库也不能当作无限持续创造的证明。

### 尚不能断言的部分

- 新的“生成规则归纳”不是静态注册更多家族；该要求由方案审计另外核对。本文不会把已有 graph mutation 误报为规则归纳完成。

### 具体处理

1. 把基础覆盖与连续探索分别记账：基础 40 只标 foundationComplete；探索使用现有 RESEARCH/EVOLVE 槽和 durable action 队列持续安排。
2. 每个子代必须给出正规 AST/图、操作器/特征/连接差异、结构 hash、行为指纹、父身份及 creation_reason；单改窗口或别名不能冒充新机制。
3. 接回既有机制 children、公式合成、失败定向变异与同目标融合；统一经 commitPreparedCandidateCP5 注册输入、协议和 TRAIN_CANDIDATE。
4. 新增生成规则须另走“共有骨架+类型槽→合法规则→不同结构两子代实训→重启消费→禁用对照”证据，不能用静态数组和消融处方代替。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/ModernFamilyFactoryCP12.java`：MODIFY_OR_VERIFY；方法：prepare, maintain, guardComplete, register, holdCursor。

- `app/src/main/java/com/ethk3/novaonepro/r952/EvolutionControllerR952.java`：MODIFY_OR_VERIFY；方法：drainActionCP12, claimEvolution。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java`：MODIFY_OR_VERIFY。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_025。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：现有 15 现代机制、F/I 家族、StructuralProposal/MutationGraph 的真实结构能力和完整可回滚父代。

### 独立验收

- 基础游标达到 40 后，适用且有预算时探索仍能产生结构不同、可执行、去重且真实训练的子代；无新颖结构时诚实记 NO_NOVEL_CHILD。
- 变异/融合/新规则分别有独立证据和可复现实验；禁止把 bookkeeping generation、尝试次数当成改善代数。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 26. 第6版要求的新思路族“生成规则归纳→发布→异段迁移→恢复消费”未找到完整实现

**编号 `R342-026`｜P1｜施工批次 B07｜证据等级 SOURCE_PATH_GAP_CONFIRMED_WITH_BOUNDED_SEARCH**

### 证据与源码定位

- `construction/plans/STARCORE_R34_1_REPAIR_20261124/control/REVIEW_CONTRACTS.json` $.contracts[id=F6-B08-02]：明确要求从两条以上公式提炼带类型槽的生成规则，并在未用开发段生成两棵结构不同实训树及重启消费。
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java` ：生产思路族数组静态登记既有生成器；这些生成器本身有价值，必须保留。
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java` ：proposeFamilyR31调用已有family.propose，记录移除source反事实，未读取/解码新发布生成规则。
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/DirectedResearchPolicyR30R1.java` ：materialize只处理PRUNE_COMPONENT，不能替代新生成规则归纳。
- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B08ContractTest.java` ：B08-04-A01调用旧failedLookCreatesFreshBoundedResearchAndAtomicCursorWithoutInheritedQuality，未实现新增规则迁移原件出口。
- `` ：未发现该合同指定的规则归纳/发布/迁移/重启原件链；这是有界静态检索，不声称证明所有可能命名均不存在。

### 已证实的原因或代码机制

- 第6版追加了新的能力和验收合同，但152现有入口仍是预注册思路族生成、参数/结构变异、现有源消融与组件裁剪。对应旧Android方法存在，新增合同的生产对象和消费者没有在所审链路落实，不能以方法存在或多种静态生成器替代新思路规则归纳。

### 尚不能断言的部分

- 即使补齐生成规则工程链，也不保证其在自然未来提高胜率；本报告r27_policy0意味着当前首先还需修前置启动/数据链。

### 具体处理

1. 拟新增 app/src/main/java/com/ethk3/novaonepro/strategyv2/TypedGenerationRuleR342.java：纯不可变类型及确定性decode/encodeCanonical/validate/induct/instantiate接口；这是待施工设计，不是152已实现代码。
2. 从至少两条已实训且有配对贡献的AST做结构归纳；去参数后相同窗口模板不能造新slot；整棵树通配规则拒绝。
3. 在原AtomPublicationStoreR31扩展规则专用prepare/publish/read方法和追加式r342规则表，由LifecycleSchemaR31显式声明并迁移；保留SCORES/MOLECULES现有CHECK与165/32行完整性合同，不把RULE硬塞旧表。
4. 沿RuntimeR27.AtomsTask→AtomWorldR27.atomsStep的有界子阶段归纳规则；ResearchContext在当前开发pin创建跨段派生上下文原件，保留discovery pin，不伪改旧dataset；用原EvolutionTask/stepBirth/stageResearchHypotheses/stepGate/commitResearchAdmissions消费，仍由ResearchStoreR30R1.consume和ResearchConsumptionStoreR31.completeWithin签原BORN事务。
5. 用未参与发现的开发段/种子产生两个去参数后不同候选，分别真实出生和首评；禁用规则同预算对照、重启后同rule版本恢复。
6. 补R341-B08-04-A01全部F6-B08-02原件与反例，之后B12同服务多根三代重验。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaFamilyR27.java`：EXTEND_EXISTING_CONSUMER；方法：propose, generate。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`：EXTEND_RULE_DECODE_AND_PROVENANCE；方法：read, proposeFamilyR31, stageBoundR31, readRuleSnapshotR342, stageRuleTransferR342, proposeRuleR342。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomWorldR27.java`：EXTRACT_SUPPORTED_REUSABLE_COMPONENTS；方法：atomsStep, derived, molecules, inductGenerationRulesR342。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomPublicationStoreR31.java`：PUBLISH_VERSIONED_RULE_ARTIFACT；方法：open, prepare, publish, requireReadyR31, ruleDdlR342, declaresRuleObjectR342, prepareRuleR342, publishRuleR342, readRulesR342。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchConsumptionStoreR31.java`：ATOMIC_RULE_CONSUMPTION_RECEIPT；方法：completeWithin, ablation, report, completeRuleUseWithinR342。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`：REUSE_BIRTH_AND_SELECTION_OWNER；方法：stepBirth, stageResearchHypotheses, stepGate, commitResearchAdmissions。

- `app/src/main/java/com/ethk3/novaonepro/LifecycleSchemaR31.java`：REGISTER_NEW_SCHEMA_IN_OWNER；方法：Definition, definitions, requireDefinitions, advanceWithinMigration。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/TypedGenerationRuleR342.java`：PLANNED_NEW_IMMUTABLE_TYPED_RULE_NOT_IMPLEMENTED；方法：decode, encodeCanonical, validate, induct, instantiate, ruleId, shapeHash。

- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B08ContractTest.java`：COMPLETE_ADDITIONAL_CONTRACT；方法：test_r341_b08_04_a01。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`：EXTEND_EXISTING_ATOMS_AND_EVOLUTION_TASKS_WITHOUT_NEW_OWNER；方法：AtomsTask.step, EvolutionTask.step。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`：PRESERVE_SAME_DATASET_SOURCE_CONSUMPTION_ADD_RULE_PROVENANCE；方法：ready, consume, currentSource。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_026。

### 主链接入与保留

现有研究owner产出规则，既有IdeaFamily/ResearchContext消费，所有策略继续走原BirthGate/训练/独立确认/唯一DecisionGate。

- 保留：已有21思路族与24模型族
- 保留：可执行AST及PIT检查
- 保留：FormulaIdentity去参数结构/行为区分
- 保留：BirthGate去重/资源限额
- 保留：原子分子已验证产物
- 保留：开发与正式未来角色隔离

### 独立验收

- 原两个发现策略各有训练/回放原件与贡献证据
- 新规则保留真实共同运算骨架，规则身份与ABI一致
- 迁移段2个产物结构不同且分别首评
- rule禁用对照改变实际派发/输出，非只改标签
- 重启后同发布原件继续消费且不重复出生
- 失败/等价/无输入不增加新思路族计数

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 27. 现代训练链当前只保留一个旧 RECENCY_BLEND 候选，不能代表多机制策略已经运行

**编号 `R342-027`｜P1｜施工批次 B07｜证据等级 CONFIRMED_SCOPE_LIMIT**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[37]：{"table": "r952_candidate", "exportedRows": 1, "totalRows": 1, "complete": true, "first_rows": [{"candidate_id": "R952-C-c41f6a28c10408b48b88", "ast": "AST1(RECENCY_BLEND,POSITION=1,LOOKBACK=120,K=3)", "parent_id": "", "state": "OBSERVING", "future_n": "25", "semantic_hash": ""}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[449]：{"table": "research_family_creation_cp12", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[345]：{"table": "r952_candidate", "rows": 1, "states": [{"column": "state", "value": "OBSERVING", "rows": 1}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[502]：{"table": "research_new_asset_genome_v41", "rows": 4, "states": [{"column": "status", "value": "IDEA_WAIT_FUTURE_OOS", "rows": 4}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[434]：{"table": "research_creation_attempt_r932", "rows": 11, "states": [{"column": "kind", "value": "HISTORICAL_RESEARCH_SEED", "rows": 1}, {"column": "kind", "value": "OPEN_EXPLORATION", "rows": 10}, {"column": "state", "value": "CREATED", "rows": 9}, {"column": "state", "value": "REJECTED", "rows": 2}]}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `bootstrap, commitPreparedCandidateCP5`，定位 382。

### 已证实的原因或代码机制

- bootstrap 默认只建立 position=1、lookback=120、K=3 的 AST1 RECENCY_BLEND；报告 R952 表只有此一候选。其他路径已有 9 次 CREATED、4 个 IDEA_WAIT_FUTURE_OOS，说明不是完全没有创造，但它们尚未变成现代实训/正式资格的多样化已消费模型。

### 尚不能断言的部分

- 缺少运行覆盖不等于静态支持其他家族的代码不存在，也不能据一条 bootstrap 推断所有策略都如此。

### 具体处理

1. 保留既有 recency learner 作兼容研究基线；在基础导入 readiness 后按机制覆盖创建可执行候选。
2. 建立跨 R26/SU/R952/新资产命名空间的只读身份映射：各自来源、真实训练、冻结、消费明确，不把同一候选多处计数。
3. 让每个新可用机制至少通过同一输入版本、训练协议和未来冻结入口；不凭均衡名额硬升格无效模型。
4. 在界面分别显示注册机制、已出生、数值训练过、正式观察、已被主链消费的数量。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`：MODIFY_OR_VERIFY；方法：bootstrap, commitPreparedCandidateCP5。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_027。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：bootstrap 基线、旧 R952 在线数值训练、已有 CREATED 与 IDEA_WAIT_FUTURE_OOS 原件。

### 独立验收

- 机制覆盖能由 AST/graph 和权重回执复核，不能只用不同名字/参数。
- 旧 RECENCY_BLEND 的25次连续数值更新与新机制状态并存且不被删除。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 28. 25350个原子规格没有转化为已准入、已绑定的训练特征和边际贡献证据

**编号 `R342-028`｜P1｜施工批次 B07｜证据等级 CONFIRMED_MATERIAL_TO_EXECUTION_GAP**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[404]：{"table": "research_atom_spec_v2", "rows": 25350, "states": [{"column": "state", "value": "DISCOVERED", "rows": 25220}, {"column": "state", "value": "REQUIRES_BINDING", "rows": 130}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[395]：{"table": "research_atom_binding_v2", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[393]：{"table": "research_atom_admission_cp12", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[126]：{"table": "r27_atom_score", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[152]：{"table": "r27_semantic_fingerprint", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[125]：{"table": "r27_ablation", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[32]：{"table": "research_atom_catalog_import_cp12", "exportedRows": 1, "totalRows": 1, "complete": true, "first_rows": [{"asset_sha256": "57673f6000cd5db5ae4be162430086753c879b8c63d5ef1d2bb521169cdcdefe", "asset_name": "cp4_atom_registry.jsonl", "expected_rows": "25350", "expected_bytes": "10416375", "next_row": "25350", "next_offset": "10416375", "prefix_sha256": "57673f6000cd5db5ae4be162430086753c879b8c63d5ef1d2bb521169cdcdefe", "sha_state": "[BINARY_PAYLOAD_NOT_INCLUDED bytes=111]", "generation": "267", "state": "COMPLETE", "updated_at": "1795579806859"}]}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `pulse, commitPreparedCandidateCP5`，定位 370。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `compute`，定位 50。

### 已证实的原因或代码机制

- 目录数量只证明规格导入完成；binding/admission为0，atom_score、semantic_fingerprint、ablation为0，尚无该批原子真实进入候选训练并形成边际贡献的证据。旧原子工具统计有活动，不能与现代绑定规格合并成同一指标。

### 尚不能断言的部分

- 没有绑定不等于25350种原子全部坏掉；需按原子定义的输入可用性、类型、PIT和机制去重逐类准入，不能以数量决定收益。

### 具体处理

1. 为已验证原子建立类型/算子/依赖/所需历史长度与ABI清单；已绑定状态必须指向具体原件和可执行表达式。
2. 由现有BoundAtomLifecycleR80选择有真实输入的少量代表原子，形成明确admission与binding，再进入同一training protocol。
3. 按同目标配对做加入/去除原子对照，保存行为指纹、边际变化、成本及支持域；零贡献也合法记录。
4. 新原子进入候选只改变研究版本，原封冻结版本不能热替换原子或继承旧证书。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`：MODIFY_OR_VERIFY；方法：pulse, commitPreparedCandidateCP5。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`：MODIFY_OR_VERIFY；方法：compute。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_028。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：25350合法原件、已经验证的完整目录hash、旧原子工具与安全类型检查。

### 独立验收

- 选定代表原子从spec→admission→binding→实际特征值→训练→消融receipt可以完整复放。
- 错误类型、未来依赖、重复行为原子被精确阻断；纯目录新增不增加有效能力计数。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 29. 分子仍处扫描阶段，合法双组件与分子协同研究没有可消费原件

**编号 `R342-029`｜P1｜施工批次 B07｜证据等级 CONFIRMED_MOLECULE_DEPENDENCY_GAP**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[34]：{"table": "research_molecule_import_checkpoint_cp12", "exportedRows": 1, "totalRows": 1, "complete": true, "first_rows": [{"source_hash": "b4ab9a61e2a36a4a426d0588ffb52350a25c09165273f802b9aa4c63e3c726ad", "asset_name": "research/cp4_molecule_seeds.jsonl", "expected_rows": "4200", "expected_bytes": "3265020", "legacy_target_row": "0", "scan_row": "900", "scan_offset": "570690", "scan_prefix": "4984b3976e7190483ac4175f9a225e0003519d68b129801f371f2719fb5b2140", "scan_sha_state": "[BINARY_PAYLOAD_NOT_INCLUDED bytes=111]", "material_row": "0", "material_offset": "0", "material_prefix": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", "material_sha_state": "[BINARY_PAYLOAD_NOT_INCLUDED bytes=111]", "generation": "36", "state": "SCANNING", "updated_at": "1795584636309"}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[147]：{"table": "r27_molecule", "rows": 64, "states": [{"column": "state", "value": "RETIRED", "rows": 64}]}
- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[154]：{"id": "T06", "group": "logicalTables", "implemented": null, "callable": null, "ran": null, "inputAvailable": null, "produced": null, "consumed": null, "effective": null, "firstKnownGap": "PAGE_HAS_RECORDED_BLOCKER", "currentSourceProducedFrames": null, "actualSourceConsumptionReceipts": null}
- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[108]：{"id": "R07", "group": "researchCharts", "implemented": true, "callable": true, "ran": "UNKNOWN", "inputAvailable": null, "produced": false, "consumed": false, "effective": null, "firstKnownGap": "NO_RECORDED_OUTPUT", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0, "notProducedReason": "等待本图算法的真实有界分析产物；登记合同或空面板不算完成"}
- `audit/report/R30_REPORT.json` $.researchTables[5].rows[0].payload_json：{"decoded_path": "$.frame.reason", "value": "LEGAL_TWO_COMPONENT_ADD_PARENT_REQUIRED", "run_id": "bd8a55a7fca4cf2e2e3a96aa033668e4658be98f6bbb104f9b326c9651116f45", "measure_key": "R07"}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ModernSeedFactoryCP12.java`，方法/成员 `maintain`，定位 147。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `compute`，定位 50。

### 已证实的原因或代码机制

- 分子目录material_row=0；旧64个molecule为RETIRED，T06记录LEGAL_TWO_COMPONENT_ADD_PARENT_REQUIRED。这是合法成分/父件与新生产输入尚未就绪，不是已经证实“分子算法无效”。

### 尚不能断言的部分

- 目录静态定义不提供协同提升；当前报告没有数值子代或去除成分对照原件，必须等待真正生产。

### 具体处理

1. 恢复有界全hash扫描和逐页物化，完整校验后发布可引用分子身份。
2. 每个分子graph验证至少合法组件、类型相容、同目标输入、规范化AST与来源，不把两个标签拼接成融合。
3. 建立父组件A、B、A+B在同训练/验证时间范围的实训对照，记录协同、冗余与成本。
4. 旧RETIRED分子只读保留；可迁移时产生独立现代身份，不能复活旧未来资格。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/ModernSeedFactoryCP12.java`：MODIFY_OR_VERIFY；方法：maintain。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`：MODIFY_OR_VERIFY；方法：compute。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_029。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：4200种子原件、64旧分子及退役原因、类型/支持域/PIT约束。

### 独立验收

- 合法双组件产生实际特征/预测并进入候选训练，非法组件有确定reason；R07消费exact子代与两父件。
- 扫描中断后恢复hash/offset一致；不同分子不会共享一份不相容的训练state。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 30. 81/82静态可调用中唯一缺件是F23教师未绑定，需局部修复而非重写全部家族

**编号 `R342-030`｜P1｜施工批次 B07｜证据等级 CONFIRMED_LOCAL_BINDING_GAP**

### 证据与源码定位

- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[22]：{"id": "F23", "group": "capabilities", "implemented": true, "callable": false, "ran": "UNKNOWN", "inputAvailable": null, "produced": null, "consumed": null, "effective": null, "firstKnownGap": "PERSISTED_OBJECT_BLOCKER_RECORDED", "currentSourceProducedFrames": null, "actualSourceConsumptionReceipts": null, "executionBlockedReason": "F23_ORIGINAL_TEACHER_NOT_BOUND", "notProducedReason": "F23_ORIGINAL_TEACHER_NOT_BOUND", "runtimeEvidenceR33": {"artifactBytesVerified": null, "attribution": "DELIMITED_R26Fxx_CANONICAL_MEMBER_REFERENCE; COMPOSITES_MAY_HAVE_MULTIPLE_MEMBERS; NOT_AST_OR_ARTIFACT_VALIDATION", "blockedStatusScope": "LEXICOGRAPHIC_FIRST_RETAINED_STATE; NOT_FIRST_IN_TIME_OR_COMPLETE_REASON_DISTRIBUTION", "counterReceiptSha256": "30dd957113719b443a2b94e1d663fe675ed5f58527703629e3bd47bd4dd88bde", "counterScope": "ALL_RETAINED_MATCHING_SOURCE_ROWS; NOT_CUMULATIVE_LIFETIME_AFTER_ARCHIVAL", "counters": {"blockedCandidateRows": 1, "candidateRows": 1, "checkpointReferenceRows": 1, "consumptionReceiptRows": 0, "developmentScoredRows": 0, "progressedCandidateRows": 1, "readyCandidateRows": 0}, "firstBlockedStatus": "BLOCKED_WARM_NUMERICAL_ABI_PRESERVED", "id": "F23", "numericalEffectValidated": null, "sourceStatus": "ALL_COUNTER_SOURCES_READ"}}
- `audit/report/R30_REPORT.json` $.metricResults[22]：{"id": "M23", "value": 81, "numerator": 81, "denominator": 82, "status": "BLOCKED", "missingReason": "可调用绑定与输入可用、真实产出、被消费、有效性分别记录，禁止由此推出质量", "observationScopeR341": {"installation": {"apkSha256": "2afc220e8942f7eda678b7ddd09ca16c48225c2f4f99167a9233a5e9bf1a8c38", "certificateSha256": "d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7", "installedVersionCode": 577311152, "releaseManifestSha256": "dea0831879133177189c9ae710d849e8bdd873293b02f9c2d3df055f51ce1e2b", "sourceCommit": "eb8f82a8bda8db22f1f122095217132f07053bcb", "state": "VERIFIED_INSTALLATION"}, "latestIssue": "202611250346", "population": "STATIC_DECLARED_HANDLERS_NOT_RUNTIME_EFFECT", "processEpoch": null, "role": "ENGINEERING", "runtimeObservedAt": 1795585570811, "scopeId": "262c3a4d8209790a37c2af775d84c88271e0f2ddaa7a0a3e8762739d2fadfda8", "snapshotAt": null, "snapshotHash": "31fc2f65c7137dd52e6c308eab53f35df0c19cd946adfaeb6b738430a49e5673"}}
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java`，方法/成员 `bindLegacyTeacher, build`，定位 10。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java`，方法/成员 `bindLegacyTeacher, build`，定位 24。

### 已证实的原因或代码机制

- F23 implemented=true、callable=false，executionBlockedReason=F23_ORIGINAL_TEACHER_NOT_BOUND；实现为DistillationFamilyR26。M23的81/82明确STATIC_DECLARED_HANDLERS_NOT_RUNTIME_EFFECT，其余81不是已验证有效。

### 尚不能断言的部分

- 该F23的保留候选还受旧ABI阻断；必须同时核对原教师原件与新子代身份，不能仅构造一个非空teacher对象即通过。

### 具体处理

1. 从原asset/teacher/state取可复现教师，验证canonical身份、payload hash、数值ABI及输入输出合同。
2. 通过FamilyRegistryR26.bindLegacyTeacher唯一入口绑定只读教师，再生成独立student身份和实训输入。
3. 在同输入/同预算比较学生与去教师学生，记录教师消费、student参数变化、预测差异和质量证据。
4. 教师缺失只挂起F23和依赖该教师的矩阵；其他具备输入的家族继续。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java`：MODIFY_OR_VERIFY；方法：bindLegacyTeacher, build。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_030。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：成熟原教师VM/资产、FamilyRegistry入口、已有81绑定、隔离缺件的局部等待。

### 独立验收

- 错误教师、ABI或state hash被拒绝；正确原件能形成F23实际实训/消费回执。
- M23分别展示静态可调用、真实产出、消费与有效性，不能仅把分子改成82宣布全部完成。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 31. AtomNet 已有物化和发布，但本次不能证明梯度更新→完整模型→预测消费闭环

**编号 `R342-031`｜P0｜施工批次 B08｜证据等级 CONFIRMED_OUTPUT_GAP_WITH_UNKNOWN_NUMERICS**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[169]：{"table": "r30_atomnet_sample", "rows": 2150, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[260]：{"table": "r31_atomnet_sample_stage", "rows": 790, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[259]：{"table": "r31_atomnet_publication", "rows": 19, "states": [{"column": "status", "value": "PUBLISHED", "rows": 19}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[168]：{"table": "r30_atomnet_forecast", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[239]：{"table": "r30_model_surface", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[330]：{"table": "r33_atomnet_progress_head", "rows": 0, "states": []}
- `audit/report/R30_REPORT.json` $.lifecycleEvidenceR31.atomNetPublicationCapacityR33.status：UNKNOWN_LIFECYCLE_READ_FAILURE
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java`，方法/成员 ``，定位 12。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java`，方法/成员 ``，定位 124。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `stored`，定位 88。

### 已证实的原因或代码机制

- 样本物化2150、stage790、19个PUBLISHED证明已有真实数据/发布活动；但forecast/model_surface为0。新optimizer表缺失且本生命周期读失败，所以不能把19次发布说成19梯度步，也不能反向说从来没有训练。当前缺少可复核的完整数值模型及消费者证据。

### 尚不能断言的部分

- 报告省略二进制权重，19次发布究竟哪些包含完整模型、哪些仅缓存/阶段状态，必须逐publication读取原件验证；归档任务失败不自动代表训练失败。

### 具体处理

1. 先完成 RES02 schema，再按publication_id核对模型artifact、trained_through、optimizer步、样本角色、前后权重hash、gradient norm与parameter change。
2. 已有19条原件不删不重写；缺证据者标 unknown，只有未来新实际优化步骤才生成新数值receipt。
3. 完成十位置模型发布后，由既有 forecast 只读最后完整不可变版本，校验 trained_through<target、deadline与canonical输入。
4. 将正确的预测surface接到实际策略消费与R14联查；归档独立维护并局部隔离毒对象，不阻塞已验证模型推理。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java`：MODIFY_OR_VERIFY。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`：MODIFY_OR_VERIFY；方法：stored。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_031。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：19次发布、2150物化样本、790stage、archive独立隔离、完整模型原子发布与前预测后训练。

### 独立验收

- 一个实际训练片可复验样本、optimizer step、数值变化及published完整模型；10位置surface由同一完成版本产生。
- 同 target 的exact consumer receipt指向该模型；杀进程不暴露部分更新模型，正式未来标签不进优化器。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 32. 已工作的在线数值训练没有追到最新数据，25次历史成功不能替代当前持续运行

**编号 `R342-032`｜P0｜施工批次 B08｜证据等级 CONFIRMED_TRAINING_WATERMARK_LAG**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[47]：{"table": "r952_learning_cursor", "exportedRows": 1, "totalRows": 1, "complete": true, "first_rows": [{"consumer_id": "R952:R952-C-c41f6a28c10408b48b88", "trained_until_issue": "202611250258", "artifact_hash": "b09ccfd2545891fe656c31767aac84cb5f391df5e68710e38515aa02dc0a908d", "last_delivery_hash": "973adbae7f93d7646551c4026394f3b4557dc3881a0ad23c4e8eb88591d68e5f", "updated_at": "1795580467421"}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[48].rows：{"artifacts": 25, "kinds": {"LEGACY_ONLINE": 25}, "unique_payload_hashes": 25, "numeric_updates": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[30]：{"table": "r952_research_outbox", "exportedRows": 25, "totalRows": 25, "complete": true, "first_rows": [{"state": "COMPLETED", "attempt_count": "10", "target_issue": "202611240133", "last_error": ""}, {"state": "COMPLETED", "attempt_count": "14", "target_issue": "202611240163", "last_error": ""}]}
- `audit/report/R30_REPORT.json` $.snapshotWatermark：{"captureFinishedAt": 1795587085486, "captureStartedAt": 1795587071012, "digestScope": "DETACHED_REPORT_FACTS_AND_WATERMARK_NOT_FULL_DATABASE_HASH", "factsReadAt": 1795587071381, "factsSha256": "8ae79f17508094366c1a9305e024482c4f9044a4e523a1c8836bbb0cddc05d0f", "inlineRowsetEncodedBytes": 220860, "latestIssue": "202611250346", "modelCensusEncodedBytes": 17, "readMode": "IMPLICIT_DEFERRED_SINGLE_SELECT", "readOnlyConnection": true, "reportHostModel": "V2353A", "reportJobId": "9fd87abfeeb7bd034dadb4e2113796ccdec63e1fb4738ad8e7a26d79e717df6d", "schemaVersion": 1, "sourceCommit": "eb8f82a8bda8db22f1f122095217132f07053bcb", "sourceManifest": {"apkSha256": "2afc220e8942f7eda678b7ddd09ca16c48225c2f4f99167a9233a5e9bf1a8c38", "assets": {"diagnostics/construction-current-CARDS_299.csv": {"bytes": 3261260, "member": "construction-current-09937fccc9a15f300e592fb41327e9da87f965ae80aeb4e6cfe78ac1f79efd9b.txt", "sha256": "09937fccc9a15f300e592fb41327e9da87f965ae80aeb4e6cfe78ac1f79efd9b"}, "diagnostics/construction-current-IDENTITY.json": {"bytes": 1083, "member": "construction-current-e26a155c888ecee99413caa33fb852c782734199f3b741f083746474135a8b00.txt", "sha256": "e26a155c888ecee99413caa33fb852c782734199f3b741f083746474135a8b00"}, "diagnostics/construction-current-REQUIREMENTS_1832.csv": {"bytes": 5732913, "member": "construction-current-952d92d89296cd08d1990b61eb8cbdde8f5a516a25a909ecfb65814ce5173765.txt", "sha256": "952d92d89296cd08d1990b61eb8cbdde8f5a516a25a909ecfb65814ce5173765"}, "diagnostics/construction-current-SUBITEMS.csv": {"bytes": 2132, "member": "construction-current-04aa7322bf15f369061741d55302a2afdc393ed688898001542e11b310e85e5e.txt", "sha256": "04aa7322bf15f369061741d55302a2afdc393ed688898001542e11b310e85e5e"}, "r925_software_audit.txt": {"bytes": 184935, "member": "software-audit-a1aa06540bafae346f21b96a3551a88d20d25ddeb336149ad909a7919c185f3c.txt", "sha256": "a1aa06540bafae346f21b96a3551a88d20d25ddeb336149ad909a7919c185f3c"}}, "attachmentRegistries": {"r335_fault_original": "PRESENT", "research_evidence_archive_v2": "PRESENT", "research_model_bundle_r929": "PRESENT", "research_training_attachment_cp12": "PRESENT", "research_training_input_v2": "PRESENT"}, "catalogSourceFailuresR341": [], "checkpointCasSource": {"checkpointObjectsVerified": null, "observedAt": 1795586333345, "scope": "EXPLICIT_LIVE_FILES_ROOT_UNDER_REPORT_INPUT_PIN; OUTSIDE_SQL_WATERMARK; OBJECT_VALIDATION_SEPARATE", "state": "SCOPED_TO_PINNED_CAS_ROOT_OBJECTS_UNVERIFIED"}, "checksR341": {"allPassed": false, "catalogComplete": true, "executionComplete": true, "queryErrors": 0, "recorded": 96}, "coverage": "All 96 checks evaluated on one immutable full snapshot; all schema objects and table counts; exact source call index; current construction ledgers; runtime observations. No omitted payload is claimed to be exported.", "faultOriginalsR335": {"hashAndReceiptVerified": 3213, "hotQuotaMeaning": "HOT_ROWS_ONLY; ALL_ARCHIVED_ORIGINALS_REMAIN_PROTECTED", "indexedOriginalBytes": 20151803, "indexedOriginals": 7719, "metadataMember": "r335-fault-originals.jsonl", "rawBodiesIncluded": false, "schemaState": "PRESENT", "unavailable": 0, "unverifiedByCompactBudget": 4506}, "generatedRuleSha256": "636370436c10f090ba640459729f77f8aace1e3063b3e074916c6bff635f5a5e", "installedApkBytes": 29752527, "installedApkIncluded": false, "installedSeedBundleSha256": "9959c46f778319a0d9ab6ff73fa0f99e8bc6e0888458a577d9cac0fa67bdcbb5", "keepOriginalsR31": {"bodiesIncluded": false, "bodyVerificationNotRun": 0, "codec": "R31_KEEP_SQLITE_CAS_1", "evidenceScope": "COMPLETE_ORIGINAL_SQLITE_ROW_CAS; COMPACT_OMISSION_IS_NOT_EXPORTED_PAYLOAD", "metadataMember": "r31-keep-originals.jsonl", "objects": 124, "readOnly": true, "registeredBytes": 1035286, "snapshotBound": true, "state": "MEASURED_SNAPSHOT_REGISTRY", "unavailableBodies": 0, "verifiedBodies": 124}, "omittedPayloads": "installed APK; raw SQLite; model/training/retention binaries; historical duplicate ledgers", "r26CheckpointAuditR32": {"boundary": "Diagnostic inventory only; missing objects and unknown children are not healthy, restored, exported, or a usable GC mark.", "byteCoverageComplete": false, "byteVerificationDenominator": 677, "byteVerificationNotRun": 18, "bytesRead": 8337781, "compactByteBudget": 8388608, "corruptObjects": 0, "format": "R32_CHECKPOINT_REFERENCE_AUDIT_V1", "gcOrModelUseAuthorized": false, "metadataMember": "r32-checkpoint-observations.jsonl", "missingObjects": 556, "observedBytes": 10056865, "observedObjects": 677, "queuedObjectsUnvisited": 0, "rawBodiesIncluded": false, "readOnly": true, "referenceRoots": 657, "retainedRootKeys": 657, "snapshotBound": true, "sourceClosureVerified": false, "sourceRootsComplete": true, "state": "CHECKPOINT_EVIDENCE_GAPS", "truncated": false, "unknownChildSets": 574, "unreadableObjects": 0, "verifiedObjects": 103}, "r26CheckpointBodiesIncluded": false, "r26CheckpointByteValidation": "BOUNDED_DIAGNOSTIC_AUDIT_SEE_MEMBER", "r26CheckpointBytes": 10056865, "r26CheckpointObjects": 677, "r26CheckpointScope": "Exact-snapshot references and observed availability; missing originals and unknown children are explicit evidence gaps, never repaired or exported bodies", "r31AdmissionBoundary": "MISSING_VERIFIED_SOURCE_BYTES_AND_FILESYSTEM_METADATA", "r31MissingAttachmentCopyBytes": 12542456, "r31PlannedAttachmentMembers": 18, "r31PlannedAttachmentPayloadBytes": 12584320, "r341RuntimeCohort": {"bytes": 41864, "member": "runtime-opportunity-cohort.json", "sha256": "177f3c7727e5061dfea7a49705103c05d9b764de75ef672fd2833ece74eafd03", "state": "OBSERVED_DECLARED_BOUNDED_RANGE"}, "r341SqlObservationScope": {"bootId": "KERNEL_BOOT:98d3b276-bde0-4d99-878b-18cc36f76a9e", "databaseIdentity": "35129b31-577b-41a8-95e6-76df10cd5a3d", "processEpoch": "f79523b237a299a19124c702", "releaseId": "dea0831879133177189c9ae710d849e8bdd873293b02f9c2d3df055f51ce1e2b", "sampleMonoAt": 126443319, "sampleWallAt": 1795586335233, "schema": "R341_RUNTIME_SCOPE_1", "scopeKind": "SQL_SNAPSHOT", "sourceCommit": "eb8f82a8bda8db22f1f122095217132f07053bcb", "sqlWatermark": "31fc2f65c7137dd52e6c308eab53f35df0c19cd946adfaeb6b738430a49e5673"}, "r80ArchiveRecovery": "FULL_RAW_EVIDENCE exports exact verified bodies; compact exports availability/hash/locator metadata and hot originals only", "r80ArchivedBodiesIncluded": false, "r80ArchivedOriginals": {"byteVerificationMode": "COMPACT_CUMULATIVE_BYTE_BUDGET", "byteVerificationNotRunPages": 0, "compactArchiveBytesRead": 0, "compactByteVerificationBudget": 8388608, "hotDatabaseBoundIsNotTotalDiskBound": true, "metadataMember": "r80-archive-observations.jsonl", "missingOrCorruptPages": 0, "observedTotalDiskBytes": 0, "pages": 0, "registeredRows": 0, "status": "VERIFIED_ARCHIVE_AVAILABILITY", "verifiedPages": 0}, "rawEvidenceIncluded": false, "registryEvidenceBoundary": "MISSING_SCHEMA is unavailable evidence, never an empty initialized registry or a passed business check", "releaseSha256": "dea0831879133177189c9ae710d849e8bdd873293b02f9c2d3df055f51ce1e2b", "reportProfile": "COMPLETE_DIAGNOSTIC_R68", "reportSourcePreparation": "REGISTERED_REPORT_SOURCES_AVAILABLE", "resourceObservationB01": {"state": "NO_REJECTION_OBSERVATION_IN_THIS_JOB"}, "runtimeFaultHotColdR335": {"byteOmittedAmongFirst20": 14, "coldSchemaReady": true, "hotSchemaReady": true, "missingSourceTables": [], "observedOriginals": 9481, "omitted": 9475, "omittedFromAvailableSources": 9475, "rows": [{"boot_id": "KERNEL_BOOT:98d3b276-bde0-4d99-878b-18cc36f76a9e", "byte_count": 3610, "category": "WRITER_HOLD", "cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":true,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"ROLLED_BACK\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R932_INDEPENDENT_ATOM_CAPTURE\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":11115,\"writerWaitMs\":18}", "commit_outcome": "ROLLED_BACK", "event_id": "c14c53fe9c927653632f3146cac84b8415d27fa3e4b66d2f142d41486fd58b38", "fence": null, "first_elapsed_ms": 124766606, "first_wall_ms": 1795584658520, "last_cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":true,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"ROLLED_BACK\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R932_INDEPENDENT_ATOM_CAPTURE\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":11115,\"writerWaitMs\":18}", "last_elapsed_ms": 124766606, "last_stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "last_stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "last_wall_ms": 1795584658520, "minute_bucket": 29926410, "occurrences": 1, "operation": "R932_INDEPENDENT_ATOM_CAPTURE", "owner_epoch": "08cd720f-8952-42bb-857f-f3063ea5c570", "phase": "WRITER_UNLOCKED", "root_fingerprint": "c14c53fe9c927653632f3146cac84b8415d27fa3e4b66d2f142d41486fd58b38", "stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "task_id": "database-writer"}, {"boot_id": "KERNEL_BOOT:98d3b276-bde0-4d99-878b-18cc36f76a9e", "byte_count": 3606, "category": "WRITER_HOLD", "cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":true,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"ROLLED_BACK\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R932_INDEPENDENT_ATOM_CAPTURE\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":566,\"writerWaitMs\":71}", "commit_outcome": "ROLLED_BACK", "event_id": "b1a7338f13236d6e593f7137c787f5e2dddb75d3645f4bfbc4cb28f0aaf20ac5", "fence": null, "first_elapsed_ms": 124754494, "first_wall_ms": 1795584646408, "last_cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":true,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"ROLLED_BACK\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R932_INDEPENDENT_ATOM_CAPTURE\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":566,\"writerWaitMs\":71}", "last_elapsed_ms": 124754494, "last_stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "last_stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "last_wall_ms": 1795584646408, "minute_bucket": 29926410, "occurrences": 1, "operation": "R932_INDEPENDENT_ATOM_CAPTURE", "owner_epoch": "08cd720f-8952-42bb-857f-f3063ea5c570", "phase": "WRITER_UNLOCKED", "root_fingerprint": "b1a7338f13236d6e593f7137c787f5e2dddb75d3645f4bfbc4cb28f0aaf20ac5", "stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "task_id": "database-writer"}, {"boot_id": "KERNEL_BOOT:98d3b276-bde0-4d99-878b-18cc36f76a9e", "byte_count": 3602, "category": "WRITER_HOLD", "cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":false,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"COMMITTED\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R811_ACTION_LABEL_1200_STREAM\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":629,\"writerWaitMs\":0}", "commit_outcome": "COMMITTED", "event_id": "1fb9a52ac8b09cece19ae5fd5e9127a28f76c96368b3c77efdaf4f19fe9c1b28", "fence": null, "first_elapsed_ms": 124745219, "first_wall_ms": 1795584637132, "last_cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":false,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"COMMITTED\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R811_ACTION_LABEL_1200_STREAM\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":629,\"writerWaitMs\":0}", "last_elapsed_ms": 124745219, "last_stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "last_stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "last_wall_ms": 1795584637132, "minute_bucket": 29926410, "occurrences": 1, "operation": "R811_ACTION_LABEL_1200_STREAM", "owner_epoch": "08cd720f-8952-42bb-857f-f3063ea5c570", "phase": "WRITER_UNLOCKED", "root_fingerprint": "1fb9a52ac8b09cece19ae5fd5e9127a28f76c96368b3c77efdaf4f19fe9c1b28", "stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "task_id": "database-writer"}, {"boot_id": "KERNEL_BOOT:98d3b276-bde0-4d99-878b-18cc36f76a9e", "byte_count": 3574, "category": "WRITER_HOLD", "cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":false,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"COMMITTED\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R967_LANE_CLAIM\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":328,\"writerWaitMs\":0}", "commit_outcome": "COMMITTED", "event_id": "5574aa78221413e918888789d74167bfa0be23a80edd1770fbc83f6a87bfc0fb", "fence": null, "first_elapsed_ms": 124740815, "first_wall_ms": 1795584632728, "last_cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":false,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"COMMITTED\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R967_LANE_CLAIM\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":328,\"writerWaitMs\":0}", "last_elapsed_ms": 124740815, "last_stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "last_stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "last_wall_ms": 1795584632728, "minute_bucket": 29926410, "occurrences": 1, "operation": "R967_LANE_CLAIM", "owner_epoch": "08cd720f-8952-42bb-857f-f3063ea5c570", "phase": "WRITER_UNLOCKED", "root_fingerprint": "5574aa78221413e918888789d74167bfa0be23a80edd1770fbc83f6a87bfc0fb", "stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "task_id": "database-writer"}, {"boot_id": "KERNEL_BOOT:98d3b276-bde0-4d99-878b-18cc36f76a9e", "byte_count": 3596, "category": "WRITER_HOLD", "cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":false,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"COMMITTED\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R31_LEGACY_SCHEMA_PREPARE\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":1423,\"writerWaitMs\":1}", "commit_outcome": "COMMITTED", "event_id": "6438257f0421633b8cf071b6d163de644beb9dcbfa5567b42886519becadbef8", "fence": null, "first_elapsed_ms": 124738279, "first_wall_ms": 1795584630193, "last_cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":false,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"COMMITTED\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R31_LEGACY_SCHEMA_PREPARE\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":1423,\"writerWaitMs\":1}", "last_elapsed_ms": 124738279, "last_stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "last_stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "last_wall_ms": 1795584630193, "minute_bucket": 29926410, "occurrences": 1, "operation": "R31_LEGACY_SCHEMA_PREPARE", "owner_epoch": "08cd720f-8952-42bb-857f-f3063ea5c570", "phase": "WRITER_UNLOCKED", "root_fingerprint": "6438257f0421633b8cf071b6d163de644beb9dcbfa5567b42886519becadbef8", "stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "task_id": "database-writer"}, {"boot_id": "KERNEL_BOOT:98d3b276-bde0-4d99-878b-18cc36f76a9e", "byte_count": 3594, "category": "WRITER_HOLD", "cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":false,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"COMMITTED\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R958_RESEARCH_SCHEMA_STEP\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":732,\"writerWaitMs\":0}", "commit_outcome": "COMMITTED", "event_id": "267a3030faebf0876a1897222c82f3d465f942e62a51f8bc8ee3d2bbac9d3d48", "fence": null, "first_elapsed_ms": 124736707, "first_wall_ms": 1795584628621, "last_cause_chain_json": "{\"beginSqlEndBreakdown\":\"NOT_MEASURED\",\"bodyReportedFailure\":false,\"captureSite\":\"DatabaseCoordinator.recordUnlockedWriterR30\",\"card\":\"R31-B02-C02\",\"commitOutcome\":\"COMMITTED\",\"commitOutcomeSource\":\"DATABASE_COORDINATOR_ATTEMPT_STATE\",\"critical\":false,\"gcOrSchedulingAttribution\":\"NOT_MEASURED\",\"next\":\"Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.\",\"observation\":\"ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS\",\"operation\":\"R958_RESEARCH_SCHEMA_STEP\",\"outcomeCapturedAt\":\"AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK\",\"rootCause\":\"UNKNOWN_NOT_INFERRED_FROM_DURATION\",\"sourceCommit\":\"eb8f82a8bda8db22f1f122095217132f07053bcb\",\"stackAvailability\":\"NOT_CAPTURED_NO_BUSINESS_THROWABLE\",\"writerHoldMs\":732,\"writerWaitMs\":0}", "last_elapsed_ms": 124736707, "last_stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "last_stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "last_wall_ms": 1795584628621, "minute_bucket": 29926410, "occurrences": 1, "operation": "R958_RESEARCH_SCHEMA_STEP", "owner_epoch": "08cd720f-8952-42bb-857f-f3063ea5c570", "phase": "WRITER_UNLOCKED", "root_fingerprint": "267a3030faebf0876a1897222c82f3d465f942e62a51f8bc8ee3d2bbac9d3d48", "stack": "NOT_CAPTURED_NO_BUSINESS_THROWABLE", "stack_hash": "e40150efdbe0c92e9efb132d3976b86615967cdc21456d569485caa42379582b", "task_id": "database-writer"}], "scope": "EXACT_READ_ONLY_REPORT_SNAPSHOT; MAX_20_ROWS_16_KIB", "shown": 6, "sourceObservationR341": {"errorRef": null, "reason": "", "reasonCode": "", "rowsReturned": 6, "scope": "IMMUTABLE_REPORT_COPY", "sourceId": "R335_RUNTIME_FAULT_ORIGINALS", "sourceQueryStatus": "READ_OK", "state": "SOURCE_OK", "totalRows": 9481, "totalRowsKnown": true}, "totalOriginals": 9481, "unavailableAmongFirst20": 0}, "runtimeObservedAt": 1795585570811, "seedIdentityMismatches": 0, "seedRegistryMember": "seed-bundle-observations.jsonl", "seedRegistryRows": 1, "signingCertificateSha256": "d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7", "snapshotBytes": 3176525824, "snapshotSha256": "31fc2f65c7137dd52e6c308eab53f35df0c19cd946adfaeb6b738430a49e5673", "sourceCommit": "eb8f82a8bda8db22f1f122095217132f07053bcb", "sourceIndexAsset": "research/r975_source_calls.tsv", "sourceIndexBytes": 43648638, "sourceIndexReference": {"apkSha256": "2afc220e8942f7eda678b7ddd09ca16c48225c2f4f99167a9233a5e9bf1a8c38", "assetPath": "research/r975_source_calls.tsv", "bytes": 43648638, "scope": "EXACT_INSTALLED_APK_ASSET", "sha256": "c7381d85ba1c8d6724522879855fb8f91adb924def28279b7591f989eb222388", "sourceCommit": "eb8f82a8bda8db22f1f122095217132f07053bcb"}, "sourceIndexSha256": "c7381d85ba1c8d6724522879855fb8f91adb924def28279b7591f989eb222388", "sourceTreeSha256": "37c9a8e7a7d285e705c65206d992dd32bc919275213fb02b86782a427d54972a", "verifiedInstallationR341": {"apkSha256": "2afc220e8942f7eda678b7ddd09ca16c48225c2f4f99167a9233a5e9bf1a8c38", "certificateSha256": "d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7", "installedVersionCode": 577311152, "releaseManifestSha256": "dea0831879133177189c9ae710d849e8bdd873293b02f9c2d3df055f51ce1e2b", "sourceCommit": "eb8f82a8bda8db22f1f122095217132f07053bcb", "state": "VERIFIED_INSTALLATION"}}, "tables": {"draws": {"available": true, "maxRowid": 11456, "objectType": "table", "rows": 5000, "withoutRowid": false}, "r26_candidate": {"available": true, "maxRowid": 50, "objectType": "table", "rows": 50, "withoutRowid": false}, "r26_capability": {"available": true, "maxRowid": 840, "objectType": "table", "rows": 40, "withoutRowid": false}, "r26_capability_source": {"available": true, "maxRowid": 1, "objectType": "table", "rows": 1, "withoutRowid": false}, "r26_certificate_revocation": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r26_cohort": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r26_consumption": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r26_evolution": {"available": true, "maxRowid": 50, "objectType": "table", "rows": 50, "withoutRowid": false}, "r26_expert_audit": {"available": true, "maxRowid": 286, "objectType": "table", "rows": 286, "withoutRowid": false}, "r26_expert_experiment": {"available": true, "maxRowid": 12, "objectType": "table", "rows": 12, "withoutRowid": false}, "r26_live_model": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r26_progress": {"available": true, "maxRowid": 179785, "objectType": "table", "rows": 4097, "withoutRowid": false}, "r26_task": {"available": true, "maxRowid": 62, "objectType": "table", "rows": 62, "withoutRowid": false}, "r26_work_total": {"available": true, "maxRowid": 1, "objectType": "table", "rows": 1, "withoutRowid": false}, "r27_abi_continuity": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_ablation": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_atom_score": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_calibration": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_certificate": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_chart_cache": {"available": true, "maxRowid": 284873, "objectType": "table", "rows": 210941, "withoutRowid": false}, "r27_eprocess": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_evolution_cursor": {"available": true, "maxRowid": 4, "objectType": "table", "rows": 1, "withoutRowid": false}, "r27_expert_proposal": {"available": true, "maxRowid": 24, "objectType": "table", "rows": 24, "withoutRowid": false}, "r27_generation": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_history_import": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_legacy_export": {"available": true, "maxRowid": 25, "objectType": "table", "rows": 3, "withoutRowid": false}, "r27_lineage": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_live_frozen": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_live_shadow": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_molecule": {"available": true, "maxRowid": 64, "objectType": "table", "rows": 64, "withoutRowid": false}, "r27_opportunity": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_policy": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_replay_dataset": {"available": true, "maxRowid": 3, "objectType": "table", "rows": 3, "withoutRowid": false}, "r27_replay_score": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r27_semantic_fingerprint": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r29_gate_v2": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r29_model_checkpoint": {"available": true, "maxRowid": 56, "objectType": "table", "rows": 1, "withoutRowid": false}, "r29_pending_draw": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r29_qual_calendar": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r29_qual_conflict": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r29_qual_cursor": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r29_qual_rebuild": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r29_qual_receipt": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_atom_contribution": {"available": true, "maxRowid": 64, "objectType": "table", "rows": 64, "withoutRowid": false}, "r30_atomnet_forecast": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_behavior_dup": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_certificate_revocation_event": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_confirm_read": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_decision_explain": {"available": true, "maxRowid": 1977, "objectType": "table", "rows": 1977, "withoutRowid": false}, "r30_decision_mismatch": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_frozen_confirm": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_issue_timeline": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_live_source_revision": {"available": true, "maxRowid": 1, "objectType": "table", "rows": 1, "withoutRowid": false}, "r30_model_artifact": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_model_call": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_model_surface": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_pit_violation": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_research_chart_frame": {"available": false, "maxRowid": null, "objectType": null, "rows": null, "withoutRowid": null}, "r30_retention_archive_index": {"available": true, "maxRowid": 55, "objectType": "table", "rows": 55, "withoutRowid": false}, "r30_retention_file_action": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_retention_run": {"available": true, "maxRowid": 15, "objectType": "table", "rows": 15, "withoutRowid": false}, "r30_revoked_certificate": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_revoked_policy": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30_writer_hold": {"available": true, "maxRowid": 83839, "objectType": "table", "rows": 75410, "withoutRowid": false}, "r30r1_analysis_run": {"available": true, "maxRowid": 29, "objectType": "table", "rows": 29, "withoutRowid": false}, "r30r1_chart_cache": {"available": true, "maxRowid": 28, "objectType": "table", "rows": 28, "withoutRowid": false}, "r30r1_consume": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30r1_proposal": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r30r1_research_measure": {"available": true, "maxRowid": 538, "objectType": "table", "rows": 538, "withoutRowid": false}, "r30r1_task_progress": {"available": true, "maxRowid": 695, "objectType": "table", "rows": 20, "withoutRowid": false}, "r31_atom_contribution_visible": {"available": true, "maxRowid": null, "objectType": "view", "rowidReason": "VIEW_HAS_NO_STABLE_ROWID", "rows": 64, "withoutRowid": false}, "r31_atom_result_active": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_atom_result_chunk": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_atom_result_run": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_atom_score_visible": {"available": true, "maxRowid": null, "objectType": "view", "rowidReason": "VIEW_HAS_NO_STABLE_ROWID", "rows": 0, "withoutRowid": false}, "r31_atomnet_publication": {"available": true, "maxRowid": 19, "objectType": "table", "rows": 19, "withoutRowid": false}, "r31_commit_receipt": {"available": true, "maxRowid": 16313, "objectType": "table", "rows": 16313, "withoutRowid": false}, "r31_feedback_event": {"available": true, "maxRowid": 176, "objectType": "table", "rows": 176, "withoutRowid": false}, "r31_feedback_receipt": {"available": true, "maxRowid": 88, "objectType": "table", "rows": 88, "withoutRowid": false}, "r31_fetch_timing": {"available": true, "maxRowid": 16512, "objectType": "table", "rows": 16384, "withoutRowid": false}, "r31_frozen_feedback_effect": {"available": true, "maxRowid": 88, "objectType": "table", "rows": 1, "withoutRowid": false}, "r31_gap_reference_batch": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_gap_reference_item": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_gap_reference_original": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_gap_reference_permit": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_gc_artifact": {"available": true, "maxRowid": 693, "objectType": "table", "rows": 693, "withoutRowid": false}, "r31_gc_checkpoint_scratch": {"available": true, "maxRowid": 1, "objectType": "table", "rows": 1, "withoutRowid": false}, "r31_gc_edge": {"available": true, "maxRowid": 40, "objectType": "table", "rows": 40, "withoutRowid": false}, "r31_gc_pin": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_gc_publication": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_gc_receipt": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_gc_status": {"available": true, "maxRowid": 1, "objectType": "table", "rows": 1, "withoutRowid": false}, "r31_history_import_row": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_history_import_run": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_hypothesis_binding": {"available": true, "maxPrimaryKeyByColumn": {"campaign_id": null, "child_policy": null}, "maxRowid": null, "objectType": "table", "rowidReason": "WITHOUT_ROWID_NOT_APPLICABLE", "rows": 0, "withoutRowid": true}, "r31_hypothesis_outcome": {"available": true, "maxPrimaryKeyByColumn": {"campaign_id": null, "child_policy": null}, "maxRowid": null, "objectType": "table", "rowidReason": "WITHOUT_ROWID_NOT_APPLICABLE", "rows": 0, "withoutRowid": true}, "r31_hypothesis_quota": {"available": true, "maxRowid": 1, "objectType": "table", "rows": 1, "withoutRowid": false}, "r31_hypothesis_registry": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_hypothesis_select": {"available": true, "maxPrimaryKeyByColumn": {"campaign_id": null, "policy_hash": null, "sample_index": null}, "maxRowid": null, "objectType": "table", "rowidReason": "WITHOUT_ROWID_NOT_APPLICABLE", "rows": 0, "withoutRowid": true}, "r31_keep_checkpoint": {"available": true, "maxRowid": 353, "objectType": "table", "rows": 55, "withoutRowid": false}, "r31_keep_cutover": {"available": true, "maxRowid": 54, "objectType": "table", "rows": 54, "withoutRowid": false}, "r31_keep_mismatch_original": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_keep_mismatch_permit": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_keep_original": {"available": true, "maxRowid": 124, "objectType": "table", "rows": 124, "withoutRowid": false}, "r31_keep_retire_permit": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_keep_row_retirement": {"available": true, "maxRowid": 11, "objectType": "table", "rows": 11, "withoutRowid": false}, "r31_mainline_freeze": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_molecule_visible": {"available": true, "maxRowid": null, "objectType": "view", "rowidReason": "VIEW_HAS_NO_STABLE_ROWID", "rows": 64, "withoutRowid": false}, "r31_recovery_clock": {"available": true, "maxRowid": 1, "objectType": "table", "rows": 1, "withoutRowid": false}, "r31_runtime_fault": {"available": true, "maxRowid": 9425, "objectType": "table", "rows": 1762, "withoutRowid": false}, "r31_startup_checkpoint_receipt": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r31_startup_checkpoint_source": {"available": true, "maxRowid": 1, "objectType": "table", "rows": 1, "withoutRowid": false}, "r31_task_control": {"available": true, "maxRowid": 2, "objectType": "table", "rows": 2, "withoutRowid": false}, "r31_task_lease": {"available": true, "maxRowid": 477387, "objectType": "table", "rows": 52, "withoutRowid": false}, "r31_task_observation": {"available": true, "maxRowid": 52, "objectType": "table", "rows": 52, "withoutRowid": false}, "r31_telemetry_counter": {"available": true, "maxRowid": 1569201, "objectType": "table", "rows": 48, "withoutRowid": false}, "r335_checkpoint_recovery": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "r335_checkpoint_recovery_cursor": {"available": true, "maxRowid": 1, "objectType": "table", "rows": 1, "withoutRowid": false}, "r335_fault_archive_work": {"available": true, "maxRowid": 1, "objectType": "table", "rows": 1, "withoutRowid": false}, "r335_fault_original": {"available": true, "maxRowid": 7719, "objectType": "table", "rows": 7719, "withoutRowid": false}, "r335_s5_refit_original": {"available": true, "maxPrimaryKeyByColumn": {"case_id": null}, "maxRowid": null, "objectType": "table", "rowidReason": "WITHOUT_ROWID_NOT_APPLICABLE", "rows": 0, "withoutRowid": true}, "r952_research_outbox": {"available": true, "maxRowid": 25, "objectType": "table", "rows": 25, "withoutRowid": false}, "research_matrix_snapshot_r930": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "su1_model_ablation": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}, "su2_certificate": {"available": true, "maxPrimaryKeyByColumn": {"certificate_id": null}, "maxRowid": null, "objectType": "table", "rowidReason": "WITHOUT_ROWID_NOT_APPLICABLE", "rows": 0, "withoutRowid": true}, "su2_diag_snapshot": {"available": true, "maxRowid": 3616141, "objectType": "table", "rows": 6, "withoutRowid": false}, "su2_diag_state": {"available": true, "maxRowid": 1, "objectType": "table", "rows": 1, "withoutRowid": false}, "su2_live_decision": {"available": true, "maxPrimaryKeyByColumn": {"genome_key": null, "sequence_ordinal": null}, "maxRowid": null, "objectType": "table", "rowidReason": "WITHOUT_ROWID_NOT_APPLICABLE", "rows": 0, "withoutRowid": true}, "su2_mainline_choice": {"available": true, "maxRowid": 3596500, "objectType": "table", "rows": 2, "withoutRowid": false}, "su2_matrix_snapshot": {"available": true, "maxPrimaryKeyByColumn": {"checkpoint_sequence": null, "genome_key": null}, "maxRowid": null, "objectType": "table", "rowidReason": "WITHOUT_ROWID_NOT_APPLICABLE", "rows": 0, "withoutRowid": true}, "su2_storage_budget": {"available": true, "maxPrimaryKeyByColumn": {"category": "WAL"}, "maxRowid": null, "objectType": "table", "rowidReason": "WITHOUT_ROWID_NOT_APPLICABLE", "rows": 10, "withoutRowid": true}, "su2_track_block": {"available": true, "maxPrimaryKeyByColumn": {"block_index": "7192", "member_id": "MAINLINE"}, "maxRowid": null, "objectType": "table", "rowidReason": "WITHOUT_ROWID_NOT_APPLICABLE", "rows": 2, "withoutRowid": true}, "trades": {"available": true, "maxRowid": null, "objectType": "table", "rows": 0, "withoutRowid": false}}}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `prepareTrainingLabel, commitTrainingLabel, pulse`，定位 198。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `prepareTrainingLabel, commitTrainingLabel, pulse`，定位 210。

### 已证实的原因或代码机制

- 25个 LEGACY_ONLINE artifact 的 numeric_updates 连续1..25且payload hash不同，证明在线训练确实运行过；training cursor只到202611250258，而快照数据到202611250346，相差88个当日期号，当前数据已超前。outbox已完成的25条不是仍在积压的25条。

### 尚不能断言的部分

- 未进入outbox的后88期不必然都应该训练：需按实际可冻结候选、canonical settlement、target eligibility和first_seen核对机会分母。不能把数值变化当质量提升。

### 具体处理

1. 逐target联查合法predraw→权威settlement COMPLETED→durable research outbox→训练prepare→CAS commit→learning cursor，定位首个应到未到事件。
2. 按exact delivery hash保证结算重复和POST repair不导致同标签二次训练；deadline错过明确NOT_RUN而非补造predraw。
3. 分开度量开奖到settlement、settlement到outbox、outbox到训练commit的延迟及缺口；保留已有prepare在锁外的成熟实现。
4. 每次新数值版本发布后触发合法后续target推理；冻结评估对象保持不可变。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`：MODIFY_OR_VERIFY；方法：prepareTrainingLabel, commitTrainingLabel, pulse。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_032。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：25份真实模型payload、prepare/commit分离、CAS、防迟到冻结、exact settlement authority。

### 独立验收

- 新的合法实时时段中，eligible机会各有delivered/blocked-with-reason终态，训练水位在商定设备SLO内追随数据；不只看最终cursor。
- 重放25条已完成delivery不改变numeric_updates；一条新权威settlement只增加一次。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 33. 训练流25次结算与正式独立未来资格是两套证据，正式冻结观察仍空

**编号 `R342-033`｜P0｜施工批次 B08｜证据等级 CONFIRMED_FORMAL_FUTURE_GAP**

### 证据与源码定位

- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.operatingSummary.independentFuture：{"activeCertificates": 0, "evolutionAttempts": 50, "futureActs": 0, "futureHitRate": null, "futureHits": 0, "independentFutureIssues": 0, "policyObjects": 0, "qualifiedExperiments": 0, "rateStatus": "NO_SETTLED_FUTURE_ACT"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[49].rows：{"rows": 25, "evidence_roles": {"TRAIN_STREAM_V2": 25}, "settled": {"1": 25}}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[361]：{"table": "r958_frozen_candidate", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[362]：{"table": "r958_frozen_predraw", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[363]：{"table": "r967_frozen_evaluation_v2", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[144]：{"table": "r27_live_frozen", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[179]：{"table": "r30_frozen_confirm", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[132]：{"table": "r27_certificate", "rows": 0, "states": []}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `capturePredraw, onSettlementCompleted, prepareTrainingLabel, commitTrainingLabel, pulse, drainImportSliceCP12, commitPreparedCandidateCP5`，定位 153。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `capturePredraw, onSettlementCompleted, prepareTrainingLabel, commitTrainingLabel, pulse, drainImportSliceCP12, commitPreparedCandidateCP5`，定位 32。
- 源码 `app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java`，方法/成员 `prepareDevelopmentFreezeCP8, commitDevelopmentFreezeCP8, capture, settle`，定位 68。
- 源码 `app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java`，方法/成员 `prepareDevelopmentFreezeCP8, commitDevelopmentFreezeCP8, capture, settle`，定位 197。
- 源码 `app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java`，方法/成员 `prepareDevelopmentFreezeCP8, commitDevelopmentFreezeCP8, capture, settle`，定位 271。
- 源码 `app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java`，方法/成员 `prepareDevelopmentFreezeCP8, commitDevelopmentFreezeCP8, capture, settle`，定位 284。

### 已证实的原因或代码机制

- 现有R952 candidate.future_n=25来自训练流settlement；正式冻结候选、frozen evaluation、R27 future/cert为0，operatingSummary独立future issues=0。训练数据可用于学习，但不构成冻结后不可改变策略的独立未来质量证明。

### 尚不能断言的部分

- 报告未证明真实质量或可开奖前授权REAL；不能把历史重放、历史回填、不同horizon、十位置预测累加成独立未来期数。

### 具体处理

1. 开发训练完成后一次性冻结模型、特征变换、规则、选择门槛、协议和hash，并记录first eligible target。
2. 用既有FrozenCandidateAdapter和冻结队列在开奖前登记预测，结算后只追加观察，不修改被冻结参数。
3. train/select/calibration/confirmation/live未来角色严格分离，未来反馈如要再训练必须生成下一模型身份。
4. 以唯一calendar issue和预声明试验定义统计样本；缺失、迟到、无资格和未出手分别保留机会分母。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java`：MODIFY_OR_VERIFY；方法：prepareDevelopmentFreezeCP8, commitDevelopmentFreezeCP8, capture, settle。

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`：MODIFY_OR_VERIFY；方法：capturePredraw, onSettlementCompleted, prepareTrainingLabel, commitTrainingLabel, pulse, drainImportSliceCP12, commitPreparedCandidateCP5。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_033。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：25次训练流真实结算、正式冻结/不泄漏合约、证书质量门槛。

### 独立验收

- 自然新发生的一段未来中exact freeze-before-open、settle-after-open可证，人工重放历史不会增加formal n。
- 改变冻结参数或混用训练label会被拒绝；无足够证据时资格始终未通过。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 34. 父子同期配对、固定窗口和独立确认没有运行，尚不能证明任何一代提升

**编号 `R342-034`｜P0｜施工批次 B08｜证据等级 CONFIRMED_NO_PAIRED_IMPROVEMENT_PROOF**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[458]：{"table": "research_fixed_window_r932", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[456]：{"table": "research_fixed_pair_r932", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[536]：{"table": "research_window_publication_r932", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[179]：{"table": "r30_frozen_confirm", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[591]：{"table": "su1_model_ablation", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[125]：{"table": "r27_ablation", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[61]：{"table": "research_lifecycle_r932", "exportedRows": 10, "totalRows": 10, "complete": true, "first_rows": [{"candidate_key": "r952:R952-C-c41f6a28c10408b48b88", "state": "OBSERVING", "reason": "WAIT_FIXED_WINDOW_AND_SHARED_CERTIFICATE", "future_n": "25", "paired_n": "0"}, {"candidate_key": "formula:R906-c38d56026557a8bfda7434f2", "state": "NURSERY", "reason": "FRESH_OBSERVATION_NO_INHERITED_SUPPORT", "future_n": "0", "paired_n": "0"}]}
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `stored`，定位 83。
- 源码 `app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java`，方法/成员 `freezeChampion, freezePair, maintainReviewed, closeOne`，定位 30。
- 源码 `app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java`，方法/成员 `freezeChampion, freezePair, maintainReviewed, closeOne`，定位 58。
- 源码 `app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java`，方法/成员 `freezeChampion, freezePair, maintainReviewed, closeOne`，定位 133。
- 源码 `app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java`，方法/成员 `freezeChampion, freezePair, maintainReviewed, closeOne`，定位 234。
- 源码 `app/src/main/java/com/ethk3/novaonepro/FixedWindowStatisticsR932.java`，方法/成员 `evaluate, evaluateCp4`，定位 17。
- 源码 `app/src/main/java/com/ethk3/novaonepro/FixedWindowStatisticsR932.java`，方法/成员 `evaluate, evaluateCp4`，定位 76。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationR30.java`，方法/成员 `freeze, evaluate`，定位 72。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationR30.java`，方法/成员 `freeze, evaluate`，定位 104。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationStoreR30.java`，方法/成员 `freeze, complete, register`，定位 65。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationStoreR30.java`，方法/成员 `freeze, complete, register`，定位 157。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationStoreR30.java`，方法/成员 `freeze, complete, register`，定位 176。

### 已证实的原因或代码机制

- 固定窗口/父子pair/publication全部0，生命周期中旧候选future25但paired_n=0；缺少在相同机会、目标和冻结规则上的父子比较。没有这些证据，增加预测次数或挑历史高分不能证明升级。

### 尚不能断言的部分

- 需要原资格协议的样本/时长/显著性门槛，不能为获得PASS临时降低。足够统计证据可能长期不能出现，工程畅通和质量达标必须分开验收。

### 具体处理

1. 将父代、子代、基线按同一机会集合和选择规则并行冻结，保存共同可评估样本与缺失原因。
2. 开发选择、校准与最终独立确认分离，固定窗口开启后不得根据成绩换区间或重置失败序列。
3. 报告增益点估计、区间、样本量、覆盖、成本与分块稳定性，并记录本轮搜索的试验族与多次选择。
4. 晋级是可回滚的精确版本切换；未满足条件不晋级，成熟冠军继续保留。
5. 分别核R27 FrozenConfirmation的封存HOLDOUT确认和CP4 FixedResearchWindow的自然未来配对；二者保留各自role/暴露预算/分母，不能把历史确认样本计入正式未来。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java`：MODIFY_OR_VERIFY；方法：freezeChampion, freezePair, maintainReviewed, closeOne。

- `app/src/main/java/com/ethk3/novaonepro/FixedWindowStatisticsR932.java`：MODIFY_OR_VERIFY；方法：evaluate, evaluateCp4。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationR30.java`：MODIFY_OR_VERIFY；方法：freeze, evaluate。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationStoreR30.java`：MODIFY_OR_VERIFY；方法：freeze, complete, register。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`：MODIFY_OR_VERIFY；方法：stored。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_034。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：固定窗口、独立确认、原冠军与预先声明协议；保留所有失败试验。

### 独立验收

- 可从原始父子预测和结算重算pair结果，区间/分母与UI一致。
- 负向对照、随机或标签打乱模型不能靠选择偏差通过；没有改善时记录真实拒绝。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 35. 20个注册矩阵没有保留的类型化生产/消费，不能用另一路25个R952快照抵充

**编号 `R342-035`｜P1｜施工批次 B08｜证据等级 CONFIRMED_TYPED_MATRIX_OUTPUT_GAP**

### 证据与源码定位

- `audit/report/STRATEGY_MATRICES.json` $.matrixInventoryR34：{"registeredObjects": 20, "objectsWithRetainedSnapshots": 0, "r930NativeKinds": 19, "qualityStatus": "UNKNOWN_NOT_VALIDATED"}
- `audit/report/STRATEGY_MATRICES.json` $.snapshots：[]
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[489]：{"table": "research_matrix_snapshot_r930", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[635]：{"table": "su2_matrix_snapshot", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[353]：{"table": "r952_matrix_snapshot", "rows": 25, "states": [{"column": "state", "value": "PREDRAW_FROZEN", "rows": 25}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[352]：{"table": "r952_matrix_feedback", "rows": 25, "states": []}
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`，方法/成员 `MatrixProductionTask.step`，定位 841。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`，方法/成员 `read, stageBoundR31`，定位 153。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java`，方法/成员 `step, produceR31, dependencyKeyR31`，定位 141。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java`，方法/成员 `step, produceR31, dependencyKeyR31`，定位 42。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java`，方法/成员 `step, produceR31, dependencyKeyR31`，定位 38。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixTensorR30R1.java`，方法/成员 `append`，定位 48。

### 已证实的原因或代码机制

- 19种R930+1种SU2 learned-factor矩阵静态绑定可追溯，但retained/typed snapshot都没有，exact child消费未证实；独立R952矩阵25份确有反馈，命名空间和contract不同。

### 尚不能断言的部分

- 不能要求每种矩阵在缺teacher/student/parent时硬生产，也不能认定20算法都实现错误。每种原始inputContract与waitRule见165对象附录及STRATEGY_MATRICES。

### 具体处理

1. 逐矩阵使用现有typed producer和真实依赖合同，缺输入只跳过该kind，在输入版本变化时重新触发。
2. 生产的tensor必须保留维度、支持数、role、dataset hash、revision、ABI、source commit与精确cell语义。
3. 实际consumer只读取匹配source和role的immutable snapshot，提交exact child/action和matrix hash消费回执。
4. 把既有R952成功路径保留独立统计；只在适配器能逐项证明语义相同后做互通，不用复制值伪造19种矩阵。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java`：MODIFY_OR_VERIFY；方法：step, produceR31, dependencyKeyR31。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixTensorR30R1.java`：MODIFY_OR_VERIFY；方法：append。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`：MODIFY_OR_VERIFY；方法：MatrixProductionTask.step。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`：MODIFY_OR_VERIFY；方法：read, stageBoundR31。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_035。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：全部20类型绑定、原始合同、R95225快照及反馈、已有typed消费者。

### 独立验收

- 对每种有合法输入的矩阵，生产与至少一个合同指定consumer按hash联查；缺输入的kind显示specific WAIT。
- 错误维度、版本、ABI、role与未知支持域不会被消费；无改变的读图不计算法贡献。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 36. 17个矩阵输入在投影前过期，输入保留期与真实消费服务没有协调

**编号 `R342-036`｜P1｜施工批次 B08｜证据等级 CONFIRMED_EXPIRED_UNPROJECTED_INPUTS**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[490]：{"table": "research_matrix_spec_cp12", "rows": 65, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[486]：{"table": "research_matrix_checkpoint_v2", "rows": 35, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[491]：{"table": "research_matrix_terminal_cp12", "rows": 17, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[42]：{"table": "research_matrix_terminal_cp12", "exportedRows": 17, "totalRows": 17, "complete": true, "first_rows": [{"matrix_id": "23102bafe44e75bcfb8b9a4268cdf751d166f5e24fab5c02b215f2e5ca26502b", "reason": "MATRIX_INPUT_EXPIRED_WITHOUT_PROJECTION", "source_issue": "202611020699", "created_at": "1795499080011"}, {"matrix_id": "97adb677ea767da612c5a17ff5af473e028bd84f38cc1e8a4bf3015fa7a86fc4", "reason": "MATRIX_INPUT_EXPIRED_WITHOUT_PROJECTION", "source_issue": "202611020699", "created_at": "1795504515833"}]}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `pulse`，定位 345。

### 已证实的原因或代码机制

- 65 matrix specs和35checkpoint已有生产准备，但17 terminal均为MATRIX_INPUT_EXPIRED_WITHOUT_PROJECTION。说明至少这17条在合法保留生命周期结束前没有完成投影，不能视为矩阵成功。

### 尚不能断言的部分

- 具体是排队饥饿、依赖缺件、输入已失效还是清理时序，须逐matrix_id、source_receipt_hash、input_hash与writer原件联查；不能从terminal字符串指认唯一GC缺陷。

### 具体处理

1. 为每份input建立ready/claimed/projected/consumed/expired状态以及deadline/pin归属，terminal保留前因。
2. 在现有MATRIX槽按合法输入剩余寿命和等待时间安排有界服务；受保护活跃input持有可回收引用，不能无限pin。
3. 依赖长期缺件时主动终止并记录原因，不让所有任务等到原件消失才发现。
4. 过期原件恢复只能创建新输入身份与新投影；不能复写旧receipt或用当前数据补造旧结果。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`：MODIFY_OR_VERIFY；方法：pulse。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_036。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：65规格、35checkpoint、17过期原件与hash、现有有限容量/清理保护。

### 独立验收

- 逐17条归因闭环；新测试中可完成投影的输入不因无界等待过期，确实过期者有精确terminal。
- GC/pin与投影提交的竞态可重放，无悬挂引用或永不释放pin。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 37. 长期摘要持续更新但没有进入新策略出生与冻结输入，1043不是进化代数

**编号 `R342-037`｜P1｜施工批次 B08｜证据等级 CONFIRMED_SUMMARY_CONSUMPTION_GAP**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[75]：{"table": "research_long_head_r80", "exportedRows": 1, "totalRows": 1, "complete": true, "first_rows": [{"through_issue": "202611250346", "generation": "1043", "status": "ACTIVE", "state_hash": "1f2e15e25b6aa4c0fd6e0aec60a54bdc49f5bdb7e5b36356a9b64b45338ce2aa"}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[477]：{"table": "research_long_epoch_r80", "rows": 3, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[482]：{"table": "research_long_snapshot_r80", "rows": 1041, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[481]：{"table": "research_long_publication_r80", "rows": 1041, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[476]：{"table": "research_long_creation_r81", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[484]：{"table": "research_long_spec_source_r81", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[478]：{"table": "research_long_frozen_source_r81", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[138]：{"table": "r27_generation", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[143]：{"table": "r27_lineage", "rows": 0, "states": []}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `onSettlementCompleted, pulse`，定位 346。

### 已证实的原因或代码机制

- long head ACTIVE并到最新250346，snapshot/publication各1041，说明摘要增量链已工作；creation/spec_source/frozen_source为0，摘要未形成有证据的子代输入消费。head generation1043是摘要版本计数，不是1043代策略变强。

### 尚不能断言的部分

- state_blob434428字节未导出，不能仅凭摘要更新断言内部所有统计正确或有预测价值；需要精确feature消费和对照。

### 具体处理

1. 保留现有增量摘要与gap新epoch逻辑，给可消费字段建立版本化feature contract、availability和source token。
2. 由现有候选创建路径选择某一immutable long snapshot，记录spec_source与input binding，不读取训练过程中变化的head。
3. 冻结子代时固化其long来源；按无long特征对照评估实际贡献，结果不显著也保持诚实。
4. 界面把摘要更新代数、策略出生代数、完成实训代数、独立确认代数分开。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`：MODIFY_OR_VERIFY；方法：onSettlementCompleted, pulse。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_037。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：已运行的1041快照/发布、最新long head、3epoch、gap journal。

### 独立验收

- 至少一个实际子代的训练输入和冻结原件精确联到long snapshot hash；重启与epoch切换不串用来源。
- 1041旧发布保持可追溯，任何新增“进化成功”必须另有真实generation/lineage与比较证据。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 38. 538条研究测量中517条是输入失效事件，不能当538次完成研究，需查明版本反复失效

**编号 `R342-038`｜P1｜施工批次 B08｜证据等级 CONFIRMED_INVALIDATION_DOMINANCE_CAUSE_TO_REPLAY**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[252]：{"table": "r30r1_research_measure", "rows": 538, "states": [{"column": "kind", "value": "BEHAVIOR_DIVERSITY", "rows": 1}, {"column": "kind", "value": "CALIBRATION_SUPPORT", "rows": 1}, {"column": "kind", "value": "DRIFT_REPLAY_VALUE", "rows": 1}, {"column": "kind", "value": "END_TO_END_RECEIPTS", "rows": 1}, {"column": "kind", "value": "EXPERT_EXECUTABLE_CREDIT", "rows": 1}, {"column": "kind", "value": "FAILED_MECHANISM_MEMORY", "rows": 1}, {"column": "kind", "value": "FAILURE_PRESCRIPTION", "rows": 1}, {"column": "kind", "value": "FUSION_COMPLEMENTARITY", "rows": 1}, {"column": "kind", "value": "INPUT_INVALIDATION", "rows": 517}, {"column": "kind", "value": "MODEL_CONSUMPTION_CHAIN", "rows": 1}, {"column": "kind", "value": "MOLECULE_SYNERGY", "rows": 1}, {"column": "kind", "value": "OPERATOR_GAIN_COST", "rows": 1}, {"column": "kind", "value": "PARENT_CHILD_PAIRED", "rows": 1}, {"column": "kind", "value": "POSITION_COMPATIBILITY", "rows": 2}, {"column": "kind", "value": "RESEARCH_BUDGET", "rows": 2}, {"column": "kind", "value": "SET_INTERSECTION_SUPPORT", "rows": 1}, {"column": "kind", "value": "STRUCTURE_FORECAST_ERROR", "rows": 2}, {"column": "kind", "value": "STRUCTURE_STATE", "rows": 2}]}
- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[150]：{"id": "T02", "group": "logicalTables", "implemented": null, "callable": null, "ran": null, "inputAvailable": null, "produced": null, "consumed": null, "effective": null, "firstKnownGap": "EMPTY_READ_PAGE", "currentSourceProducedFrames": null, "actualSourceConsumptionReceipts": null}
- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[162]：{"id": "T14", "group": "logicalTables", "implemented": null, "callable": null, "ran": null, "inputAvailable": null, "produced": null, "consumed": null, "effective": null, "firstKnownGap": "PAGE_HAS_RECORDED_BLOCKER", "currentSourceProducedFrames": null, "actualSourceConsumptionReceipts": null}
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `compute, stored, productR335, classifyFrameR335, step`，定位 228。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `compute, stored, productR335, classifyFrameR335, step`，定位 156。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStudyTaskR30R1.java`，方法/成员 `step`，定位 41。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `compute, stored, productR335, classifyFrameR335, step`，定位 209。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`，方法/成员 `ResearchStudyTask.step, ObservatoryTask.step`，定位 835。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`，方法/成员 `ResearchStudyTask.step, ObservatoryTask.step`，定位 834。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `compute, stored, productR335, classifyFrameR335, step`，定位 19。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `compute, stored, productR335, classifyFrameR335, step`，定位 72。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `compute, stored, productR335, classifyFrameR335, step`，定位 116。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `compute, stored, productR335, classifyFrameR335, step`，定位 182。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`，方法/成员 `paired, currentSource`，定位 62。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`，方法/成员 `paired, currentSource`，定位 96。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java`，方法/成员 `revision, runValid, require`，定位 51。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java`，方法/成员 `revision, runValid, require`，定位 32。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java`，方法/成员 `revision, runValid, require`，定位 52。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchSourceJournalR31.java`，方法/成员 `affectedSql, DatasetRead.capture`，定位 115。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchSourceJournalR31.java`，方法/成员 `affectedSql, DatasetRead.capture`，定位 80。

### 已证实的原因或代码机制

- 完整kind分布显示INPUT_INVALIDATION517/538，其他21也有BLOCKED/只读内容。失效记录是诊断证据而不是完成生产；持续输入变化与产物稳定性的关系尚未闭合。

### 尚不能断言的部分

- 只凭高失效率不能确定所有失效都是bug，数据/ABI真正变化时失效是必要的。需逐run追踪失效字段和精确耗时，区分合法替换与不必要全量hash波动。

### 具体处理

1. 将每次invalidation关联旧/新dataset、ABI、source revision、dependency hash及触发原因。
2. 研究片使用不可变输入快照；新输入产生新版本，不使正在合法计算的已pin片无谓中断。
3. 只有相应方向依赖改变才失效，不能因无关日志/读取时间戳改变全部产品；保留真实输入变化的严格失效。
4. 把诊断事件、有效数值点、研究产品、实际动作与质量效果分别计量，图表从同一快照读原件。
5. 在B08沿RuntimeR27.ResearchStudyTask/ObservatoryTask及ResearchStudyTaskR30R1.step、ResearchObservatoryR30R1.step恢复R01–R18的有界生产：可执行研究产生真实数值measure，R11/R14/R18保留正式只读联查角色，缺件返回specific WAIT。
6. 每个measure在当前source/ABI/dataset/revision下提交source_run、input/product hash、validPointCount与role；将其交RES20已有消费者，不能仅写chart cache或在B11补算研究。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`：MODIFY_OR_VERIFY；方法：ResearchStudyTask.step, ObservatoryTask.step。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStudyTaskR30R1.java`：MODIFY_OR_VERIFY；方法：step。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`：MODIFY_OR_VERIFY；方法：compute, stored, productR335, classifyFrameR335, step。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`：MODIFY_OR_VERIFY；方法：paired, currentSource。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java`：MODIFY_OR_VERIFY；方法：revision, runValid, require。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchSourceJournalR31.java`：MODIFY_OR_VERIFY；方法：affectedSql, DatasetRead.capture。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_038。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：517失效原件及其hash、必要的版本/ABI隔离、严格产品身份。

### 独立验收

- 注入无关状态变化不会使无关方向重算；改变真实输入则必须失效并生成新run。
- 至少一份稳定输入能从计算完成到consumer提交，不被循环invalidation永久饿死。
- 在B08生产入口已形成有数值支持的measure及typed source；图表未打开时也能持续生产。B11只验证/展示此原件。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 39. 失败诊断没有生成可验证的假设→行动→子代→结果回流，已安装不是已执行

**编号 `R342-039`｜P1｜施工批次 B08｜证据等级 CONFIRMED_FAILURE_RESEARCH_CLOSURE_GAP**

### 证据与源码定位

- `audit/report/R26_REPORT.json` $.strategyAssessment.failureDirectedResearch：{"scope": "FAILED_LOOK_GUIDED_FRESH_RESEARCH; REGISTRATION_AND_TRAINING_DO_NOT_GRANT_QUALITY", "installed": true, "totalBatches": 0, "batches": [], "omittedBatches": 0}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[287]：{"table": "r31_hypothesis_registry", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[284]：{"table": "r31_hypothesis_binding", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[285]：{"table": "r31_hypothesis_outcome", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[384]：{"table": "research_action_v2", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[381]：{"table": "research_action_result_binding_cp12", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[251]：{"table": "r30r1_proposal", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[250]：{"table": "r30r1_consume", "rows": 0, "states": []}
- `audit/report/REPORT_DETAILS.txt` line[72].evidence：【K3失败九类拆解】
Version=V57_K3_FAILURE_DIAGNOSIS_FULL_20260808
PrimaryLayer=NUMBER|Verdict=BLOCKED
1.NUMBER|BLOCKED|confidence=1.0
Positive=NumberQ artifact|PROVEN=false
Negative=Untrained NumberQ cannot establish top-number quality
Reason=Strict OOS artifact proof is missing
NextTest=Train/freeze NumberQ; compare top-k recall and Sinkhorn parity.
Hash=188a8c07b32305677574b8281e3fa23d00fb91928b496e47af2903fc7721746f

2.RANK|BLOCKED|confidence=1.0
Positive=1200-action rank artifact|PROVEN=false
Negative=Untrained ranker cannot establish Champion ordering
Reason=Strict OOS artifact proof is missing
NextTest=Run same-issue groupwise OOS Rank1/MRR/NDCG/pairwise stability.
Hash=04dd7351eac06dd82af4b9fffe376070b7f865e47b594e7a46fb63f9eb2fd84a

3.THIRD|UNAVAILABLE|confidence=0.0
Positive=N/A
Negative=No persisted runtime evidence
Reason=Third boundary decides whether final K3 loses its marginal candidate
NextTest=Audit rank3 vs rank4 margin, miss migration and after-draw counterfactual.
Hash=6b79dd3575f6e6884abccc9ba90ac2200ea6630bf2c2af228348ceff0a35e0c9

4.WINDOW|UNAVAILABLE|confidence=0.0
Positive=N/A
Negative=No persisted runtime evidence
Reason=Window choice can overfit local repetition or react too slowly
NextTest=Compare 12/24/48/96/360 survival and PIT-stable window selection.
Hash=fb3a48fa1bc2d5e28442c917c82f59400f6fb92e839b0127032d1fab2102cfed

5.REGIME|UNAVAILABLE|confidence=0.0
Positive=N/A
Negative=No persisted runtime evidence
Reason=Regime mismatch can invalidate an otherwise strong motif/expert
NextTest=Blockwise regime transition replay; inspect CUSUM/BOCPD/drift flags.
Hash=5c21f76c86346f97ba4c37d6cb76d5b36db86f656d68be3f9a833416efda04b0

6.MOTIF|UNAVAILABLE|confidence=0.0
Positive=N/A
Negative=No persisted runtime evidence
Reason=Motif may be sparse, stale or duplicated across templates
NextTest=Require support, recent validity and negative evidence before promotion.
Hash=32e23403c5b53c02ba73ab0509c51dd7bef16b695d1f80a76ba87818f1267b0d

7.RELATION|UNAVAILABLE|confidence=0.0
Positive=N/A
Negative=No persisted runtime evidence
Reason=Cross-position relation can be spurious or leak contemporaneous information
NextTest=Verify relation uses only pre-draw history and survives block ablation.
Hash=ed64e2c478c02f484a80404a6928a808cd31024f665f2a266dc1e88e6c34e1ac

8.CONSENSUS|UNAVAILABLE|confidence=0.0
Positive=N/A
Negative=No persisted runtime evidence
Reason=Consensus can become correlated-vote double counting
NextTest=Measure expert diversity, regret and leave-one-expert-out delta.
Hash=ee3932ea4105789fae103d999748483cf24d5b906f8af0c3e863266986fdc346

9.CALIBRATION|BLOCKED|confidence=1.0
Positive=SelectedHitP calibration artifact|PROVEN=false
Negative=Untrained calibration makes threshold/coverage decisions unreliable
Reason=Strict OOS artifact proof is missing
NextTest=Use selection-conditioned pre-draw OOS ECE/Brier/Wilson/subgroup certificates.
Hash=b56a0c0e94baee871985a8f23a4d725e5905ac9a1d3733cb744df123eea93f51

ReportHash=ab97478b03a0ebcbc22a03866cb796855156dc7dbee7965a7952526aef4a3fc1

- 源码 `app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java`，方法/成员 `diagnoseSnapshotCP4, stateLayerSnapshotCP4`，定位 13。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/EvolutionControllerR952.java`，方法/成员 `drainActionCP12, claimEvolution`，定位 15。
- 源码 `app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java`，方法/成员 `diagnoseSnapshotCP4, stateLayerSnapshotCP4`，定位 320。
- 源码 `app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java`，方法/成员 `diagnoseSnapshotCP4, stateLayerSnapshotCP4`，定位 196。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`，方法/成员 `read, appendBudgetReceipt, stageBoundR31`，定位 114。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`，方法/成员 `read, appendBudgetReceipt, stageBoundR31`，定位 26。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchActionPolicyR33.java`，方法/成员 `Source.requireAction, weights`，定位 19。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`，方法/成员 `EvolutionTask.step`，定位 416。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`，方法/成员 `read, appendBudgetReceipt, stageBoundR31`，定位 36。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`，方法/成员 `read, appendBudgetReceipt, stageBoundR31`，定位 26。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchActionPolicyR33.java`，方法/成员 `Source.requireAction, weights`，定位 49。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchActionStoreR33.java`，方法/成员 `append`，定位 44。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`，方法/成员 `ready, consume`，定位 100。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`，方法/成员 `ready, consume`，定位 112。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchConsumptionStoreR31.java`，方法/成员 `completeWithin`，定位 61。
- 源码 `app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java`，方法/成员 `diagnoseSnapshotCP4, stateLayerSnapshotCP4`，定位 320。
- 源码 `app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java`，方法/成员 `diagnoseSnapshotCP4, stateLayerSnapshotCP4`，定位 412。
- 源码 `app/src/main/java/com/ethk3/novaonepro/ResearchFeedbackCP12.java`，方法/成员 `drainOne`，定位 61。

### 已证实的原因或代码机制

- failureDirectedResearch installed=true但totalBatches=0，hypothesis/action/binding/outcome与proposal/consume均空。当前缺的是诊断原件转为可执行实验并归因结果；静态九层诊断条目不证明本机九类实际错误全发生。

### 尚不能断言的部分

- 具体失败层必须由真实settled row和当期frozen输入支持，无法从静态PROVEN标志或一条miss推断算法原因。九层逐项处理合同见附录，非九项已证故障。

### 具体处理

1. 以canonical settlement和原始predraw作唯一失败源，保留未命中、失准、执行缺口、随机残差等不同性质；上游链断先修链。
2. 修正RSEC-066诊断事实源：NUMBER/RANK/CALIBRATION由当前动态artifact/冻结原件绑定，六类summary按当前版本运行回执；不得用编译常量PROVEN=false或字符串关键词作为数值根因证明，也不得把常量改true伪造通过。
3. 对NUMBER/RANK/THIRD/WINDOW/REGIME/MOTIF/RELATION/CONSENSUS/CALIBRATION逐项提供证据和反证，不把每次没中自动解释成机制失败。
4. 将可执行假设写入registry与action，规定变更对象、必要输入、对照、预算、停止条件，交现有EVOLVE/TRAIN消费。
5. 结果只通过exact hypothesis/action/child绑定回流，失败知识用于避免重复消耗，不能仅增加诊断文字。
6. 在B08由ResearchContextR30R1和ResearchActionPolicyR33实际应用合法frame：预算、去重、回放和子代机制变更带before/after hash及exact action/child回执。Source.requireAction拒绝错误role、未来信息和陈旧来源；这些动作生产不留到B11。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`：MODIFY_OR_VERIFY；方法：EvolutionTask.step。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java`：MODIFY_OR_VERIFY；方法：read, appendBudgetReceipt, stageBoundR31。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchActionPolicyR33.java`：MODIFY_OR_VERIFY；方法：Source.requireAction, weights。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchActionStoreR33.java`：MODIFY_OR_VERIFY；方法：append。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`：MODIFY_OR_VERIFY；方法：ready, consume。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchConsumptionStoreR31.java`：MODIFY_OR_VERIFY；方法：completeWithin。

- `app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java`：MODIFY_OR_VERIFY；方法：diagnoseSnapshotCP4, stateLayerSnapshotCP4。

- `app/src/main/java/com/ethk3/novaonepro/ResearchFeedbackCP12.java`：MODIFY_OR_VERIFY；方法：drainOne。

- `app/src/main/java/com/ethk3/novaonepro/r952/EvolutionControllerR952.java`：MODIFY_OR_VERIFY；方法：drainActionCP12, claimEvolution。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_039。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：既有失败诊断九层、原始结算、失败原件、严格消费者行为回执。

### 独立验收

- 一条真实失败簇从原件到假设、独立子代实训、配对验证、action_result_binding可完整重放。
- 证据不足或随机残差有明确NO_ACTION，而非强制创造“提升”或伪造九层故障。
- 九层报告原状态分别为3个BLOCKED和6个UNAVAILABLE；修复后每层给出exact source identity及证据，缺件保留UNKNOWN，不把诊断confidence当算法因果置信。
- 不开页面也可由原研究owner消费measure并改变预算/子代/回放；禁用同source对照影响真实行为，before/after和结果回流完整。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 40. 已有出生和50次演化尝试没有完成真正代际记录与可复验的改善归因

**编号 `R342-040`｜P1｜施工批次 B08｜证据等级 CONFIRMED_EVOLUTION_EVIDENCE_GAP**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[92]：{"table": "r26_evolution", "rows": 50, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[138]：{"table": "r27_generation", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[143]：{"table": "r27_lineage", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[149]：{"table": "r27_policy", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[444]：{"table": "research_evolution_event_r930", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[371]：{"table": "r975_evolution_review", "rows": 0, "states": []}
- `audit/report/R26_REPORT.json` $.strategyAssessment.evolution：{"attempts": 50, "structural": 10, "researchAccepted": 0, "qualificationAssertions": 0, "improvementAssertions": 0, "scope": "PERSISTED_LEDGER_ASSERTIONS_ACROSS_RETAINED_SOURCES; NO_NEW_REAUDIT_OR_PER_GENERATION_GAIN_INFERRED"}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/EvolutionControllerR952.java`，方法/成员 `drainActionCP12, claimEvolution`，定位 15。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java`，方法/成员 ``，定位 130。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`，方法/成员 `stepBirth, stepGate, stageResearchHypotheses, commitResearchAdmissions`，定位 210。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`，方法/成员 `stepBirth, stepGate, stageResearchHypotheses, commitResearchAdmissions`，定位 268。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`，方法/成员 `stepBirth, stepGate, stageResearchHypotheses, commitResearchAdmissions`，定位 99。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`，方法/成员 `stepBirth, stepGate, stageResearchHypotheses, commitResearchAdmissions`，定位 100。

### 已证实的原因或代码机制

- 保留50次R26演化尝试/部分结构尝试不等于完成一代策略：R27 generation、lineage、policy及现代review均0。上游数据/训练/动作不通，进化结果也无法落库或被消费。

### 尚不能断言的部分

- “每一代必然更强”不能作为真实统计保证；后代可能退化。可实现的是候选持续产生、严谨比较、只有证据满足规则才替换冠军。

### 具体处理

1. 记录proposal→canonical graph→train→development→paired evaluation→promotion/rejection各自原子阶段与确切父代。
2. 结构变异、数值再训、参数搜索、组合融合和新规则分别计数，不用一次slot执行递增“强度代”。
3. 将同目标、同时间、同预算的父子结果交预先声明的准则；不合格子代留档/退役，冠军保持可用。
4. 新父代只有成功被下一代真实引用后才计链路连续性；重启和并发同身份只提交一次。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`：MODIFY_OR_VERIFY；方法：stepBirth, stepGate, stageResearchHypotheses, commitResearchAdmissions。

- `app/src/main/java/com/ethk3/novaonepro/r952/EvolutionControllerR952.java`：MODIFY_OR_VERIFY；方法：drainActionCP12, claimEvolution。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java`：MODIFY_OR_VERIFY。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_040。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：50历史尝试、结构生成器、已验证冠军、原始父代与不可变版本。

### 独立验收

- 出现至少一条完整合法父→子 lineage及真实数值/配对原件，成功和失败实验均正确记录。
- 故意制作差子代不能替代成熟冠军；没有质量证据时系统显示未证明而非自动更强。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 41. 互补融合缺同目标双父件与共同失败/增益验证，不能只平均概率称融合进化

**编号 `R342-041`｜P1｜施工批次 B08｜证据等级 CONFIRMED_REQUIRED_PARENT_GAP**

### 证据与源码定位

- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[158]：{"id": "T10", "group": "logicalTables", "implemented": null, "callable": null, "ran": null, "inputAvailable": null, "produced": null, "consumed": null, "effective": null, "firstKnownGap": "PAGE_HAS_RECORDED_BLOCKER", "currentSourceProducedFrames": null, "actualSourceConsumptionReceipts": null}
- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[113]：{"id": "R12", "group": "researchCharts", "implemented": true, "callable": true, "ran": "UNKNOWN", "inputAvailable": null, "produced": false, "consumed": false, "effective": null, "firstKnownGap": "NO_RECORDED_OUTPUT", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0, "notProducedReason": "等待本图算法的真实有界分析产物；登记合同或空面板不算完成"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[143]：{"table": "r27_lineage", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[456]：{"table": "research_fixed_pair_r932", "rows": 0, "states": []}
- `audit/report/R30_REPORT.json` $.researchTables[9].rows[0].payload_json：{"decoded_path": "$.frame.reason", "value": "TWO_SAME_TARGET_PARENTS_REQUIRED", "run_id": "d012bb1b81017807f6f09182d20c58b0af6e1d763058fc8046845433aa471df1", "measure_key": "R12"}
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/EvolutionControllerR952.java`，方法/成员 `drainActionCP12, claimEvolution`，定位 15。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `compute`，定位 60。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionOpsR27.java`，方法/成员 `fuse`，定位 15。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionStudyR30R1.java`，方法/成员 `pair, gain`，定位 47。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionStudyR30R1.java`，方法/成员 `pair, gain`，定位 119。

### 已证实的原因或代码机制

- T10原件的TWO_SAME_TARGET_PARENTS_REQUIRED直接指出缺少同目标双父件；R12本源码无可用产品/消费。静态存在FusionOps不等于已有可验证融合子代。

### 尚不能断言的部分

- 父代互补性不能靠模型名字不同判断；均值集成可以保留为一个基线，但必须有真实共同目标、行为差异和自身验证。

### 具体处理

1. 为A、B两父件绑定同目标/输入角色/时间窗口与完整模型版本，并核验输出空间、类型和ABI相容。
2. 用共同失败区域、相关性、互补信息和leave-one-parent-out对照判断是否值得产生融合假设。
3. 融合graph形成独立子身份，训练或确定组合权重只在开发数据完成；禁止复制父证书、未来样本或主链权限。
4. 通过同一训练与冻结评估路径，与A、B和预声明简单组合基线同时比较。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionOpsR27.java`：MODIFY_OR_VERIFY；方法：fuse。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionStudyR30R1.java`：MODIFY_OR_VERIFY；方法：pair, gain。

- `app/src/main/java/com/ethk3/novaonepro/r952/EvolutionControllerR952.java`：MODIFY_OR_VERIFY；方法：drainActionCP12, claimEvolution。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`：MODIFY_OR_VERIFY；方法：compute。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_041。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：既有FusionOps、molecule组合能力、父模型、简单组合基线与其历史表现。

### 独立验收

- 合法同目标双父件产生可执行融合child；时间/目标不匹配必须拒绝。
- exact lineage、两父hash、融合权重/graph与共同目标配对原件齐全；无提升则不晋级。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 42. 多周期探针与绑定研究没有持续观测，只有4条未运行记录和大量未记账机会

**编号 `R342-042`｜P1｜施工批次 B08｜证据等级 CONFIRMED_PROBE_OPPORTUNITY_GAP**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.multiHorizonProbeResearch：{"archivePolicy": "VERIFIED_CAS_BEYOND_5000;KEEP_PENDING_REFERENCES_AND_LAST48_SETTLED_PER_HORIZON;AUDIT_LOCATORS_RETAIN_ORIGINAL_BYTES", "archivedOriginalPages": 0, "comparisonPolicy": "FIXED_HORIZONS_THRESHOLD_AND_RESIDUAL_STRATA;NO_RETROSPECTIVE_SELECTION", "correlatedHorizonsAreIndependentSamples": false, "diagnosticWindowPerHorizon": 48, "efficacy": "NOT_PROVEN", "firstObservedAnchor": "202611240132", "gapMeaning": "EXPECTED_SCHEDULE_NO_CAPTURE_RECEIPT_NOT_BACKFILLED", "historyRetentionLimit": 5000, "latestConfirmedAnchor": "202611250346", "mayCreateReal": false, "metricScope": "RETAINED_HOT_ORIGINALS_AND_PENDING;NOT_LIFETIME_METRICS", "observedOpportunityTargets": 4, "probabilityEncoding": "JAVA_HEX_DOUBLE_EXACT_RAW_NOT_CALIBRATED", "probabilityKind": "RAW_NOT_CALIBRATED", "promotion": "NOT_GRANTED", "protocol": "R80_PROBE_V1|game=Binance1-PK10|windows=48,240,960|h=1,2,4,8|alpha=1|weights=equal|position=1|K=3|threshold=0.40|consensus=equal_same_target|residual=prior_settled_48_per_h|baseline=fixed123|NO_REAL_NO_CALIBRATION_NO_TRAINING", "protocolHash": "545bebdf82bd54e083bb1c3005c6ef32c99534f7e0b7a85934e26fb3f6c7f704", "role": "PROBE_RESEARCH_ONLY_NOT_FORMAL", "scheduledTargetsSinceFirstObservedAnchor": 1655, "separateTargetDenominators": {"fixedSameTargetConsensus": {"brierRawNotCalibrated": null, "fixed123BaselineBrierSameTargets": null, "fixed123BaselineHitsSameTargets": 0, "frozenTargets": 0, "hitRate": null, "hits": 0, "pendingTargets": 0, "predeclaredPriorResidualStrata": {"PRIOR_MEAN_RESIDUAL_NONPOSITIVE": {"hitRate": null, "hits": 0, "settledTargets": 0}, "PRIOR_MEAN_RESIDUAL_POSITIVE": {"hitRate": null, "hits": 0, "settledTargets": 0}, "UNAVAILABLE": {"hitRate": null, "hits": 0, "settledTargets": 0}}, "rejectedTargets": 0, "settledTargets": 0, "threshold040CoverageOfSettledTargets": null, "threshold040HitRate": null, "threshold040Hits": 0, "threshold040SettledTargets": 0}, "h1": {"brierRawNotCalibrated": null, "fixed123BaselineBrierSameTargets": null, "fixed123BaselineHitsSameTargets": 0, "frozenTargets": 0, "hitRate": null, "hits": 0, "pendingTargets": 0, "predeclaredPriorResidualStrata": {"PRIOR_MEAN_RESIDUAL_NONPOSITIVE": {"hitRate": null, "hits": 0, "settledTargets": 0}, "PRIOR_MEAN_RESIDUAL_POSITIVE": {"hitRate": null, "hits": 0, "settledTargets": 0}, "UNAVAILABLE": {"hitRate": null, "hits": 0, "settledTargets": 0}}, "rejectedTargets": 0, "settledTargets": 0, "threshold040CoverageOfSettledTargets": null, "threshold040HitRate": null, "threshold040Hits": 0, "threshold040SettledTargets": 0}, "h2": {"brierRawNotCalibrated": null, "fixed123BaselineBrierSameTargets": null, "fixed123BaselineHitsSameTargets": 0, "frozenTargets": 0, "hitRate": null, "hits": 0, "pendingTargets": 0, "predeclaredPriorResidualStrata": {"PRIOR_MEAN_RESIDUAL_NONPOSITIVE": {"hitRate": null, "hits": 0, "settledTargets": 0}, "PRIOR_MEAN_RESIDUAL_POSITIVE": {"hitRate": null, "hits": 0, "settledTargets": 0}, "UNAVAILABLE": {"hitRate": null, "hits": 0, "settledTargets": 0}}, "rejectedTargets": 0, "settledTargets": 0, "threshold040CoverageOfSettledTargets": null, "threshold040HitRate": null, "threshold040Hits": 0, "threshold040SettledTargets": 0}, "h4": {"brierRawNotCalibrated": null, "fixed123BaselineBrierSameTargets": null, "fixed123BaselineHitsSameTargets": 0, "frozenTargets": 0, "hitRate": null, "hits": 0, "pendingTargets": 0, "predeclaredPriorResidualStrata": {"PRIOR_MEAN_RESIDUAL_NONPOSITIVE": {"hitRate": null, "hits": 0, "settledTargets": 0}, "PRIOR_MEAN_RESIDUAL_POSITIVE": {"hitRate": null, "hits": 0, "settledTargets": 0}, "UNAVAILABLE": {"hitRate": null, "hits": 0, "settledTargets": 0}}, "rejectedTargets": 0, "settledTargets": 0, "threshold040CoverageOfSettledTargets": null, "threshold040HitRate": null, "threshold040Hits": 0, "threshold040SettledTargets": 0}, "h8": {"brierRawNotCalibrated": null, "fixed123BaselineBrierSameTargets": null, "fixed123BaselineHitsSameTargets": 0, "frozenTargets": 0, "hitRate": null, "hits": 0, "pendingTargets": 0, "predeclaredPriorResidualStrata": {"PRIOR_MEAN_RESIDUAL_NONPOSITIVE": {"hitRate": null, "hits": 0, "settledTargets": 0}, "PRIOR_MEAN_RESIDUAL_POSITIVE": {"hitRate": null, "hits": 0, "settledTargets": 0}, "UNAVAILABLE": {"hitRate": null, "hits": 0, "settledTargets": 0}}, "rejectedTargets": 0, "settledTargets": 0, "threshold040CoverageOfSettledTargets": null, "threshold040HitRate": null, "threshold040Hits": 0, "threshold040SettledTargets": 0}}, "sourceCodeHash": "636370436c10f090ba640459729f77f8aace1e3063b3e074916c6bff635f5a5e", "status": "READ_OK", "unrecordedGapTargets": 1651}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[341]：{"table": "r80_probe_input", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[340]：{"table": "r80_probe_forecast", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[342]：{"table": "r80_probe_label", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[343]：{"table": "r80_probe_opportunity", "rows": 4, "states": [{"column": "status", "value": "NOT_RUN", "rows": 4}]}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[405]：{"table": "research_bound_cohort_r80", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[407]：{"table": "research_bound_parent_r80", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[408]：{"table": "research_bound_source_r80", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[406]：{"table": "research_bound_input_r80", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[28]：{"key": "r80_probe_last_status", "value": "202611250331|NOT_RUN_OWNER_NOT_REACHED", "updatedAt": 1795584664628, "status": "READ_OK"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[29]：{"key": "r80_bound_source_last_status", "value": "202611250331|NOT_RUN_OWNER_NOT_REACHED", "updatedAt": 1795584664628, "status": "READ_OK"}
- 源码 `app/src/main/java/com/ethk3/novaonepro/MultiHorizonProbeR80.java`，方法/成员 `fit`，定位 127。
- 源码 `app/src/main/java/com/ethk3/novaonepro/MultiHorizonProbeR80.java`，方法/成员 `fit`，定位 160。
- 源码 `app/src/main/java/com/ethk3/novaonepro/ResearchPredrawCaptureR923.java`，方法/成员 `captureTarget, captureTargetWithinR927, publish`，定位 296。

### 已证实的原因或代码机制

- 探针机会记录4条均NOT_RUN/PROBE_HISTORY_GAP，forecast/input/label及bound cohort等0；报告按既定计划推导机会1655、未记录1651。fit合同固定末尾960条且连续，快照实际连续suffix仅872，当前仍无法满足此输入条件；调度状态另有OWNER_NOT_REACHED，属于还没到达该owner的另一个断点，不能混作同一故障。

### 尚不能断言的部分

- 探针按设计研究用途，不直接训练/校准/授权REAL；h1/h2/h4/h8重叠不能视为4倍独立样本。计划机会计数需保留其起点/日历算法，不能当实际运行失败逐条已证。

### 具体处理

1. 把probe触发接回已有predraw/研究owner的明确阶段，每个计划机会写executed或NOT_RUN具体原因的轻量回执。
2. 按既有固定960连续期与PIT合同创建immutable probe input；先补足合法连续历史或等待，不能悄悄缩短窗口仍冒用原protocolHash。未来标签到达后只匹配对应目标结算。
3. 用source与parent/cohort binding连接真实探针结果到研究动作；未到owner、历史不足、迟到分别统计。
4. 共享计算缓存但保留各horizon语义，不复制结果或重复计算独立未来n。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/MultiHorizonProbeR80.java`：MODIFY_OR_VERIFY；方法：fit。

- `app/src/main/java/com/ethk3/novaonepro/ResearchPredrawCaptureR923.java`：MODIFY_OR_VERIFY；方法：captureTarget, captureTargetWithinR927, publish。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_042。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：probe研究-only边界、日历机会账、gap拒绝与未来冻结时点。

### 独立验收

- 一个完整新时段所有计划机会均有观测终态，输入合法时产生开奖前forecast与之后label。
- 缺历史时不造输出；horizon重叠及研究-only状态不侵入正式资格和REAL。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 43. 候选概率校准及冻结漂移策略缺件，概率值尚不具备已验证的质量含义

**编号 `R342-043`｜P1｜施工批次 B09｜证据等级 CONFIRMED_CALIBRATION_AND_DRIFT_ARTIFACT_GAP**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[409]：{"table": "research_calibration_quality_cp12", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[412]：{"table": "research_candidate_cal_study_r932", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[410]：{"table": "research_candidate_cal_model_r932", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[131]：{"table": "r27_calibration", "rows": 0, "states": []}
- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[114]：{"id": "R13", "group": "researchCharts", "implemented": true, "callable": true, "ran": "UNKNOWN", "inputAvailable": null, "produced": false, "consumed": false, "effective": null, "firstKnownGap": "NO_RECORDED_OUTPUT", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0, "notProducedReason": "等待本图算法的真实有界分析产物；登记合同或空面板不算完成"}
- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[116]：{"id": "R15", "group": "researchCharts", "implemented": true, "callable": true, "ran": "UNKNOWN", "inputAvailable": null, "produced": false, "consumed": false, "effective": null, "firstKnownGap": "NO_RECORDED_OUTPUT", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0, "notProducedReason": "等待本图算法的真实有界分析产物；登记合同或空面板不算完成"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[47]：{"key": "r932_domain_position_1_drift_observation", "value": "{\"revision\":\"R82_FROZEN_DRIFT_TUNE_V1\",\"policy_hash\":\"\",\"target_issue\":\"202611250298\",\"base_issue\":\"202611250297\",\"position\":1,\"input_hash\":\"\",\"risk_js\":null,\"reason\":\"WAIT_FROZEN_DRIFT_TUNE_ARTIFACT\",\"triggered\":false,\"reference_window\":null,\"cooldown_issues\":null,\"threshold\":null,\"false_alarm_budget\":null,\"budget_scope\":\"EMPIRICAL_TUNE_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE\",\"may_authorize_real\":false}", "updatedAt": 1795582628161, "status": "READ_OK"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[55]：{"key": "r932_domain_position_2_drift_observation", "value": "{\"revision\":\"R82_FROZEN_DRIFT_TUNE_V1\",\"policy_hash\":\"\",\"target_issue\":\"202611250347\",\"base_issue\":\"202611250346\",\"position\":2,\"input_hash\":\"\",\"risk_js\":null,\"reason\":\"WAIT_FROZEN_DRIFT_TUNE_ARTIFACT\",\"triggered\":false,\"reference_window\":null,\"cooldown_issues\":null,\"threshold\":null,\"false_alarm_budget\":null,\"budget_scope\":\"EMPIRICAL_TUNE_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE\",\"may_authorize_real\":false}", "updatedAt": 1795585568958, "status": "READ_OK"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[63]：{"key": "r932_domain_position_3_drift_observation", "value": "{\"revision\":\"R82_FROZEN_DRIFT_TUNE_V1\",\"policy_hash\":\"\",\"target_issue\":\"202611250308\",\"base_issue\":\"202611250307\",\"position\":3,\"input_hash\":\"\",\"risk_js\":null,\"reason\":\"WAIT_FROZEN_DRIFT_TUNE_ARTIFACT\",\"triggered\":false,\"reference_window\":null,\"cooldown_issues\":null,\"threshold\":null,\"false_alarm_budget\":null,\"budget_scope\":\"EMPIRICAL_TUNE_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE\",\"may_authorize_real\":false}", "updatedAt": 1795583231055, "status": "READ_OK"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[71]：{"key": "r932_domain_position_4_drift_observation", "value": "{\"revision\":\"R82_FROZEN_DRIFT_TUNE_V1\",\"policy_hash\":\"\",\"target_issue\":\"202611250321\",\"base_issue\":\"202611250320\",\"position\":4,\"input_hash\":\"\",\"risk_js\":null,\"reason\":\"WAIT_FROZEN_DRIFT_TUNE_ARTIFACT\",\"triggered\":false,\"reference_window\":null,\"cooldown_issues\":null,\"threshold\":null,\"false_alarm_budget\":null,\"budget_scope\":\"EMPIRICAL_TUNE_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE\",\"may_authorize_real\":false}", "updatedAt": 1795584020595, "status": "READ_OK"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[79]：{"key": "r932_domain_position_5_drift_observation", "value": "{\"revision\":\"R82_FROZEN_DRIFT_TUNE_V1\",\"policy_hash\":\"\",\"target_issue\":\"202611250310\",\"base_issue\":\"202611250309\",\"position\":5,\"input_hash\":\"\",\"risk_js\":null,\"reason\":\"WAIT_FROZEN_DRIFT_TUNE_ARTIFACT\",\"triggered\":false,\"reference_window\":null,\"cooldown_issues\":null,\"threshold\":null,\"false_alarm_budget\":null,\"budget_scope\":\"EMPIRICAL_TUNE_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE\",\"may_authorize_real\":false}", "updatedAt": 1795583350080, "status": "READ_OK"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[87]：{"key": "r932_domain_position_6_drift_observation", "value": "{\"revision\":\"R82_FROZEN_DRIFT_TUNE_V1\",\"policy_hash\":\"\",\"target_issue\":\"202611250320\",\"base_issue\":\"202611250319\",\"position\":6,\"input_hash\":\"\",\"risk_js\":null,\"reason\":\"WAIT_FROZEN_DRIFT_TUNE_ARTIFACT\",\"triggered\":false,\"reference_window\":null,\"cooldown_issues\":null,\"threshold\":null,\"false_alarm_budget\":null,\"budget_scope\":\"EMPIRICAL_TUNE_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE\",\"may_authorize_real\":false}", "updatedAt": 1795583954882, "status": "READ_OK"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[95]：{"key": "r932_domain_position_7_drift_observation", "value": "{\"revision\":\"R82_FROZEN_DRIFT_TUNE_V1\",\"policy_hash\":\"\",\"target_issue\":\"202611250326\",\"base_issue\":\"202611250325\",\"position\":7,\"input_hash\":\"\",\"risk_js\":null,\"reason\":\"WAIT_FROZEN_DRIFT_TUNE_ARTIFACT\",\"triggered\":false,\"reference_window\":null,\"cooldown_issues\":null,\"threshold\":null,\"false_alarm_budget\":null,\"budget_scope\":\"EMPIRICAL_TUNE_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE\",\"may_authorize_real\":false}", "updatedAt": 1795584308188, "status": "READ_OK"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[103]：{"key": "r932_domain_position_8_drift_observation", "value": "{\"revision\":\"R82_FROZEN_DRIFT_TUNE_V1\",\"policy_hash\":\"\",\"target_issue\":\"202611250324\",\"base_issue\":\"202611250323\",\"position\":8,\"input_hash\":\"\",\"risk_js\":null,\"reason\":\"WAIT_FROZEN_DRIFT_TUNE_ARTIFACT\",\"triggered\":false,\"reference_window\":null,\"cooldown_issues\":null,\"threshold\":null,\"false_alarm_budget\":null,\"budget_scope\":\"EMPIRICAL_TUNE_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE\",\"may_authorize_real\":false}", "updatedAt": 1795584189942, "status": "READ_OK"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[111]：{"key": "r932_domain_position_9_drift_observation", "value": "{\"revision\":\"R82_FROZEN_DRIFT_TUNE_V1\",\"policy_hash\":\"\",\"target_issue\":\"202611250306\",\"base_issue\":\"202611250305\",\"position\":9,\"input_hash\":\"\",\"risk_js\":null,\"reason\":\"WAIT_FROZEN_DRIFT_TUNE_ARTIFACT\",\"triggered\":false,\"reference_window\":null,\"cooldown_issues\":null,\"threshold\":null,\"false_alarm_budget\":null,\"budget_scope\":\"EMPIRICAL_TUNE_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE\",\"may_authorize_real\":false}", "updatedAt": 1795583122597, "status": "READ_OK"}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.researchSchedulerState[119]：{"key": "r932_domain_position_10_drift_observation", "value": "{\"revision\":\"R82_FROZEN_DRIFT_TUNE_V1\",\"policy_hash\":\"\",\"target_issue\":\"202611250281\",\"base_issue\":\"202611250280\",\"position\":10,\"input_hash\":\"\",\"risk_js\":null,\"reason\":\"WAIT_FROZEN_DRIFT_TUNE_ARTIFACT\",\"triggered\":false,\"reference_window\":null,\"cooldown_issues\":null,\"threshold\":null,\"false_alarm_budget\":null,\"budget_scope\":\"EMPIRICAL_TUNE_EXCEEDANCE_NOT_FALSE_ALARM_GUARANTEE\",\"may_authorize_real\":false}", "updatedAt": 1795581609536, "status": "READ_OK"}
- 源码 `app/src/main/java/com/ethk3/novaonepro/FrozenDriftPolicyR82.java`，方法/成员 `parse, observe`，定位 35。
- 源码 `app/src/main/java/com/ethk3/novaonepro/DomainRouterR929.java`，方法/成员 `route, routeWithEvidenceR82`，定位 40。

### 已证实的原因或代码机制

- 校准study/model/quality均0；冻结漂移策略缺件时源码明确WAIT_FROZEN_DRIFT_TUNE_ARTIFACT，并使用fallback或namespace暂停。模型输出概率不能据此宣称可靠置信度、适应漂移已有效。

### 尚不能断言的部分

- fallback本身可能是成熟安全行为，不应为了显示进化而移除；某位置provider不新鲜属于运行健康依赖，不能全部归因校准算法。

### 具体处理

1. 保留原概率面并分开协议：FrozenDriftPolicyR82只用其既有earlier-TUNE；CandidateCalibrationR932按eligibleEvaluation、事前预约采集及FIT120/EMBARGO1/VALIDATION120隔离序列，拒绝训练角色与开奖后捕获。版本化模型仍绑定R969_STATIC_DISJOINT_CALIBRATION；其它证书域按各自完整合同，不能用TUNE或历史回放抵充正式概率校准/自然未来。
2. 按目标/位置/选择条件/版本生成immutable校准artifact，记录原始与校准后概率、Brier/ECE及分块证据。
3. 冻结drift policy后由原DomainRouter消费，绑定source hash；缺artifact保持可解释fallback或局部PAUSE。
4. 漂移触发新研究输入/回放计划，真实再训练产新版本，不能在线修改已冻结正式候选。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/FrozenDriftPolicyR82.java`：MODIFY_OR_VERIFY；方法：parse, observe。

- `app/src/main/java/com/ethk3/novaonepro/DomainRouterR929.java`：MODIFY_OR_VERIFY；方法：route, routeWithEvidenceR82。

- `app/src/main/java/com/ethk3/novaonepro/CandidateCalibrationR932.java`：MODIFY_OR_VERIFY；方法：inputStateCP12, qualificationForStudyCP12, captureOrPredict, settle, loadFit, fitModel, publishFit, fitOne。

- `app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java`：MODIFY_OR_VERIFY；方法：freezePair, freezePair, maintain, maintainReviewed。

- `app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java`：VERIFY_ONLY；方法：readExactCalibration, requireExactCalibrationObservations, requireExactCalibrationScopeGate, beforeInsert。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B09ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_043。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：已有GLOBAL_FALLBACK/局部暂停、原概率、PIT与冻结策略边界。

### 独立验收

- 校准artifact从输入到consumer可复核，改变版本或支持域不匹配不得使用；校准无改善可不采用。
- 无policy测试保持安全fallback；有合法policy后决策确由该hash产生，R13/R15可联查。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 44. 研究到证书、选择器、唯一主链冻结与结算的最后一段没有完成

**编号 `R342-044`｜P0｜施工批次 B09｜证据等级 CONFIRMED_QUALIFICATION_TO_MAINLINE_GAP**

### 证据与源码定位

- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[423]：{"table": "research_certificate_r929", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[485]：{"table": "research_mainline_contribution_r929", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[132]：{"table": "r27_certificate", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[623]：{"table": "su2_certificate", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[631]：{"table": "su2_live_decision", "rows": 0, "states": []}
- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[164]：{"id": "T16", "group": "logicalTables", "implemented": null, "callable": null, "ran": null, "inputAvailable": null, "produced": null, "consumed": null, "effective": null, "firstKnownGap": "READ_PROJECTION_ONLY", "currentSourceProducedFrames": null, "actualSourceConsumptionReceipts": null}
- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[119]：{"id": "R18", "group": "researchCharts", "implemented": true, "callable": true, "ran": "UNKNOWN", "inputAvailable": null, "produced": false, "consumed": false, "effective": null, "firstKnownGap": "NO_RECORDED_OUTPUT", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0, "notProducedReason": "等待本图算法的真实有界分析产物；登记合同或空面板不算完成"}
- `audit/report/R30_REPORT.json` $.researchTables[15].rows[0]：{"certificate_active": 0, "decision_key": "d61cc4cb79490e0350339035696ec1e1ba496911839587d8487917fcd44e6b1d", "entry_point": "V51_B12", "issue": "202611250347", "items_json": "{\"actRuleFired\":\"UNKNOWN\",\"certificateActive\":\"FAIL\",\"dataHealthy\":\"PASS\",\"freezeComplete\":\"FAIL\",\"modelAvailable\":\"UNKNOWN\",\"onePerIssue\":\"PASS\",\"selectorConfirmed\":\"FAIL\",\"stakeFixed\":\"PASS\",\"userRealEnabled\":\"FAIL\"}", "original_allow": 0, "original_reason": "MODEL_NOT_PROVEN", "recorded_at": 1795585569497, "role": "FORMAL_READ_ONLY", "source_json": "{\"baseIssue\":\"202611250346\",\"candidate\":\"SU2-F-35992c827c068919ee54eeb2da894fc5777c45bcced5e73bdb1aaaa08dfb863c\",\"certificate\":\"\",\"duplicateScope\":\"LEGACY_TRADE_AND_CANONICAL_REAL\",\"fixedUnit\":1.0,\"freezeReason\":\"MISSING_ORIGINAL_FREEZE\",\"historyHash\":\"d27dbff2f1d9bda6941943157ff48eaba962628dd7046b6ed794d5d0775069e9\",\"historyRows\":240,\"lane\":\"EXISTING_SU2_CERTIFIED_LANE\",\"mainlineAuthorityR31\":\"TYPED_READ:WAIT_NO_VALID_CERTIFICATE\",\"member\":\"XI_INCUMBENT:35992c827c068919ee54eeb2da894fc5777c45bcced5e73bdb1aaaa08dfb863c\",\"modelHash\":\"\",\"sourceGeneration\":12806,\"stakeSource\":\"AppConfig.UNIT_STAKE; typed contract carries no amount or loss/streak adjustment\"}"}
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `stored`，定位 101。
- 源码 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`，方法/成员 `capturePredraw`，定位 32。

### 已证实的原因或代码机制

- certificate/mainline_contribution等为空，T16原始decision解释中资格、冻结、选择器和用户REAL开关未满足。没有合法证书就没有可验证研究成果进入主链；这是闭环缺口，但保守不授权是正确功能。
- 静态生产链中ResearchPlatformStoreR929.recordContribution只有声明、没有调用；BusinessFunnelCP12读取该旧投影不能代表全部主链归因。现有V51Repository已包含与canonical同事务写R922精确贡献审计的路径，必须保留并按相同目标/角色/资格核对；这不证明当前已有合格新策略正贡献。

### 尚不能断言的部分

- 本任务不授权开启用户REAL开关或绕过任何质量门槛。无法从研究尝试或命中率图证明当前策略具备真实授权资格。

### 具体处理

1. 以原DecisionGate为唯一授权入口，证书必须绑定精确冻结模型、协议、时间范围和支持域。
2. 选择器只选在当前target合法且未过期的证书模型，记录拒绝其他候选的reason与保留旧冠军的依据。
3. 将exact certificate→selection→canonical predraw freeze→authoritative settlement→mainline contribution联成可读链。
4. 研究侧只提供证据和候选，不能写真实出手账、绕过用户设置或以测试样本充当已达标自然未来。
5. 按现有canonical、R974与R922精确审计建立不重复的读投影/派生回执，修正仅查看r929表的漏读；不把旧r929写入强行搬到热事务，不另造授权owner。exact recipe weight=1的构成语义与旧mix influence cap=.05分别保留。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`：MODIFY_OR_VERIFY；方法：stored。

- `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java`：MODIFY_OR_VERIFY；方法：capturePredraw。

- `app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java`：MODIFY_OR_VERIFY；方法：freeze, prepareChoiceCP12, selected。

- `app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java`：MODIFY_OR_VERIFY；方法：prepare, certificateContract, evaluate, verifyCertificate, beforeInsert。

- `app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java`：MODIFY_OR_VERIFY；方法：runR36, buildRankedActionStage, finishPreparedR335, resumePreparedR335。

- `app/src/main/java/com/ethk3/novaonepro/V51CandidateRealGateR915.java`：MODIFY_OR_VERIFY；方法：evaluate, evaluate, verifyAuthorized, releaseBoundary。

- `app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java`：MODIFY_OR_VERIFY；方法：decideV51B12, decideV51B12OriginalR30, b12Reject, toLegacyDecisionV51。

- `app/src/main/java/com/ethk3/novaonepro/V51Repository.java`：MODIFY_OR_VERIFY；方法：commitCanonicalBundleR36, commitCanonicalBundleR36, commitCanonicalBundleVerifiedR368, insertAuthorizationCoreR36。

- `app/src/main/java/com/ethk3/novaonepro/DbStore.java`：MODIFY_OR_VERIFY；方法：insertAuthorizedTradeWithinCanonicalR915。

- `app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java`：VERIFY_ONLY；方法：insertWithinTransaction, fromRuntime。

- `app/src/main/java/com/ethk3/novaonepro/ResearchMainlineContributionAuditR922.java`：MODIFY_OR_VERIFY；方法：validateExactWithinTransaction, insertWithinTransaction, exactRealAttributionR34。

- `app/src/main/java/com/ethk3/novaonepro/ResearchPlatformStoreR929.java`：MODIFY_OR_VERIFY；方法：authorizeCurrent, recordContribution。

- `app/src/main/java/com/ethk3/novaonepro/BusinessFunnelCP12.java`：MODIFY_OR_VERIFY；方法：capture, captureSnapshotCP12, readStatement, statementCP12。

- `app/src/main/java/com/ethk3/novaonepro/SettlementCoordinatorR815.java`：VERIFY_ONLY；方法：settleNow, drain, execute。

- `app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java`：VERIFY_ONLY；方法：executeFromCoordinatorR815, settleRealWithinTransactionR36。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterR31.java`：VERIFY_ONLY；方法：prepare, verifyFrozen。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineFreezeProofR31.java`：VERIFY_ONLY；方法：certificate, publishWithinFreeze, freeze, read, allows。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java`：VERIFY_ONLY；方法：evaluate, choose, contract。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterSU1.java`：VERIFY_ONLY；方法：authority, first, prepare, verifyFrozen, bind。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateSU1.java`：VERIFY_ONLY；方法：metrics, controlsComplete, read, maintain。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterSU2.java`：VERIFY_ONLY；方法：prepare。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateSU2.java`：VERIFY_ONLY；方法：read, maintain, preregister, maintainOne, maintainPolicy。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RealAuthoritySU2.java`：VERIFY_ONLY；方法：allows, completeFreezeAndNoRealR30。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/Su2CommitAuthorityR30.java`：VERIFY_ONLY；方法：verifyFrozen。

- `app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java`：VERIFY_ONLY；方法：load, recipeLaneCP12, scoreRecipeCP8。

- `app/src/main/java/com/ethk3/novaonepro/ResearchGatePlanR967.java`：VERIFY_ONLY；方法：installed, policyForGeneration, forWindow, bindCp4。

- `app/src/main/java/com/ethk3/novaonepro/CertificateBundleServiceR929.java`：VERIFY_ONLY；方法：evaluateCp4, validatedInfluenceCap, validatedCp4Cap, cp4ExternalGates。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B09ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_044。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：唯一DecisionGate、旧冠军、用户开关、canonical freeze、单次结算与资金账。

### 独立验收

- 在隔离/影子验收中完整重放合法证书的主链接入与结算；不合法、过期、ABI不符、用户未启用时保持拒绝。
- 真实运行只有预先协议达标后才可进入允许状态，报告可逐期解释每个gate。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 45. 总体占用约10.97GB，5000期开奖保留上限没有控制派生/报告/备份体积

**编号 `R342-045`｜P1｜施工批次 B10｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

STORAGE_CENSUS total10968312876bytes/12384entries；DATABASE4532251411、REPORTS6308292139、RESEARCH_CAS44108414。两份report snapshot共6183514112bytes，旧backup保护项1336456117bytes。draws实际5000。

### 证据与源码定位

- `report/STORAGE_CENSUS.txt` line 1：total=10968312876; detail.REPORT_OR_RETENTION_SNAPSHOT=6183514112
- `report/DIAGNOSTIC_SCOPE.json` $.recentEvidence[0]：draws totalRows=5000
- 源码 `app/src/main/java/com/ethk3/novaonepro/StorageCensusSU2.java`，方法/成员 `detail, protection`，定位 40–48。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionR30.java`，方法/成员 `clean, kind`，定位 50,93–102。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`，方法/成员 `afterDelete, beforeDelete, cleanFiles, retiredCopyReference`，定位 285–300。

### 已证实的原因或代码机制

- 主要量来自数据库、完整报告工作快照、受保护备份；不是当前WAL约1MB或单纯开奖条数。
- 分类 PROTECTED_ORIGINAL=10887307334、RECOMPUTABLE=0、UNKNOWN=81005542，不能把大文件直接视作可删缓存。

### 尚不能断言的部分

- 逻辑file length为区间采样而非原子盘面；allocatedBlocks另有11007679488，须分开。
- 当前包不能验证旧backup已具独立完整恢复链。

### 具体处理

1. 先建立owner-reference-job-deliveryPin-backupRecovery一一对应的文件寿命表；报告交付确认和重试状态进入引用闭包。
2. 完成报告交付/恢复原件校验后由原report owner退役无引用快照；旧备份须恢复演练证据后方可退役。
3. 为数据库派生表、图表/诊断索引、故障冷归档设独立增长预算和受保护保留策略；业务原件不删。
4. 清理前后记录逻辑bytes、allocatedbytes、真实已释放bytes和receipt；保留中断恢复。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/StorageCensusSU2.java`：MODIFY_OR_VERIFY；方法：detail, protection。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionR30.java`：MODIFY_OR_VERIFY；方法：clean, kind。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`：MODIFY_OR_VERIFY；方法：afterDelete, beforeDelete, cleanFiles, retiredCopyReference。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B10ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_045。

### 主链接入与保留

各原storage owner共用引用闭包与预算，不能增设盲扫删除器。

- 保留：5000期原始行情规则、旧版本备份、所有研究原件/CAS、报告交付恢复、精确引用保护。

### 独立验收

- 10.97GB逐owner可解释，重复报告无长期无引用快照增长。
- 可恢复性验证后才回收；回收receipt与文件/数据库实际bytes一致，用户功能历史仍完整。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 46. checkpoint引用闭包不完整：556缺原件、18未字节验证、574子集合未知

**编号 `R342-046`｜P1｜施工批次 B10｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

657referenceRoots、677observedObjects；verified103、missing556、byteVerificationNotRun18、unknownChildSets574、sourceClosureVerified=false。无corrupt并不代表完整。

### 证据与源码定位

- `report/source-manifest.json` $.r26CheckpointAuditR32：missingObjects=556; sourceClosureVerified=false; gcOrModelUseAuthorized=false
- `report/R30_REPORT.json` $.lifecycleEvidenceR31.checkpointRecoveryR335.value：PRESERVE_OLD_IDENTITY; ORIGINAL_CONSUMER_STAYS_DEPENDENCY_ISOLATED
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/StartupCheckpointIdentityR31.java`，方法/成员 `prepare`，定位 94–114。
- 源码 `app/src/main/java/com/ethk3/novaonepro/CheckpointReportAuditR32.java`，方法/成员 `observe`，定位 45–91。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`，方法/成员 `preservePreparedStartupCheckpoint, preserveStartupCheckpoint`，定位 225–264。

### 已证实的原因或代码机制

- 报告明确存在引用→文件缺失和验证预算未覆盖；不完整闭包不能授权GC或模型使用。

### 尚不能断言的部分

- 556并非全部已证不可恢复；需对原备份/CAS位置校验。
- 不可以该总体数字解释本次CHECKPOINT_PRESERVE失败；本次PREPARE VERIFIED。

### 具体处理

1. 以缺失content hash建立受影响asset/consumer索引，逐项找精确旧字节并复核hash/长度/codec。
2. 找不回的保留原身份和缺失事实，隔离相关消费链；独立重训必须新identity、新dataset/new evidence，不继承旧n或证书。
3. 把验证预算未覆盖18项续跑到明确结果，子闭包unknown不得判无引用；无关成熟主链继续。
4. 修复后的可恢复性以真实checkpoint reload/继续训练/同状态校验收据闭环。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/StartupCheckpointIdentityR31.java`：MODIFY_OR_VERIFY；方法：prepare。

- `app/src/main/java/com/ethk3/novaonepro/CheckpointReportAuditR32.java`：MODIFY_OR_VERIFY；方法：observe。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`：MODIFY_OR_VERIFY；方法：preservePreparedStartupCheckpoint, preserveStartupCheckpoint。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B10ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_046。

### 主链接入与保留

原checkpoint恢复owner→资产级依赖→研究consumer；与全局启动拆分。

- 保留：原身份不可伪造、历史证据n不可继承、受保护原件与可恢复副本、未受影响主链。

### 独立验收

- 677对象逐一有明确missing/verified/corrupt/unreadable状态及受影响asset，unknown不静默转0。
- 同源恢复给出字节等同与运行消费receipt；新训练显示新身份；隔离不阻断全局服务。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 47. 冷归档完成不等于物理空间已回收，当前没有可验证的GC释放闭环

**编号 `R342-047`｜P1｜施工批次 B10｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

fault archive stage=COMPLETED但physicalBytesReleased=null、releaseMeaning=HOT_QUOTA_ROWS_ONLY；checkpoint GC receipts=0/verifiedHistoricalReleasedBytes=0；最后实际collection为历史 BLOCKED_OR_INCOMPLETE_ORIGINALS_PRESERVED。DB freelist_count=0。

### 证据与源码定位

- `report/R30_REPORT.json` $.lifecycleEvidenceR31.faultOriginalsR335.value：physicalBytesReleased=null
- `report/R30_REPORT.json` $.lifecycleEvidenceR31.checkpointGcR31.value：receipts=0; verifiedHistoricalReleasedBytes=0
- `report/STORAGE_CENSUS.txt` line 2：freelist_count=0
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionR30.java`，方法/成员 `clean`，定位 93–102。
- 源码 `app/src/main/java/com/ethk3/novaonepro/StorageCensusSU2.java`，方法/成员 `sqliteSpace`，定位 126–127。

### 已证实的原因或代码机制

- 将hot quota行迁到保留原件只缓解逻辑配额；SQLite文件和CAS目录物理释放需要各自被证明的步骤。
- 现有保护会在闭包不完整时保留原件，这一保护是正确行为，缺的是安全完成条件。

### 尚不能断言的部分

- 历史GC失败不能当本次再次执行失败。
- freelist=0时不应承诺简单VACUUM必然大幅缩小；当前页多为实际存活数据。

### 具体处理

1. 分开hot rows退休、归档原件保留、DB pages释放、文件unlink、操作系统allocated bytes五种量。
2. 补齐RT16引用闭包后按原GC owner退役已证无引用数据，所有删除事务/文件操作可恢复。
3. 大规模DB重建须走已有离线维护窗口、足够空间、校验恢复与原件保护；不在实时链在线整库重建。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionR30.java`：MODIFY_OR_VERIFY；方法：clean。

- `app/src/main/java/com/ethk3/novaonepro/StorageCensusSU2.java`：MODIFY_OR_VERIFY；方法：sqliteSpace。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B10ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_047。

### 主链接入与保留

各原storage owner串联自己的真实状态，报告消费而非推测释放。

- 保留：安全保护、原件保留、实时写链、后台主动停止/恢复。

### 独立验收

- 任一“释放成功”必须附真实before/after bytes+receipt，未知用null。
- 断电/失败恢复后无孤儿删除或数据丢失，主链升级回归通过。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 48. 数据库对象体积只截到前1024个名称，当前无法准确定位全部大表/索引

**编号 `R342-048`｜P2｜施工批次 B10｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

STORAGE_CENSUS dbstatStatus=TRUNCATED_EXPLICIT_1024_OBJECT_LIMIT、dbstatObjectCoverageComplete=false。可见r27_chart_cache约219250688bytes/210941rows，r30_writer_hold约51617792bytes/75410rows，但不能保证是全库最大。

### 证据与源码定位

- `report/STORAGE_CENSUS.txt` line 2 / $.dbstatStatus：TRUNCATED_EXPLICIT_1024_OBJECT_LIMIT
- 源码 `app/src/main/java/com/ethk3/novaonepro/StorageCensusSU2.java`，方法/成员 `sqliteSpace`，定位 126–127。

### 已证实的原因或代码机制

- SQL以名称排序截取，既不覆盖全库，也不是按空间topK。

### 尚不能断言的部分

- 不能用这些局部大对象断言全库主要增长来源。

### 具体处理

1. 改为keyset分页完整对象普查，允许预算不足时续页并保留总覆盖分母。
2. 按table及其indexes合并bytes与row count/增长速率，标出统计时点和文件级差异。
3. 再制定图表缓存/telemetry表的退役或聚合策略，保留原件出处和用户历史查询能力。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/StorageCensusSU2.java`：MODIFY_OR_VERIFY；方法：sqliteSpace。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B10ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_048。

### 主链接入与保留

只读诊断分页→原表owner容量治理。

- 保留：完整表/索引结构、统计原件、成熟图表功能、存储保护。

### 独立验收

- 完整对象bytes与DB页数有可解释差异；不完整时明确unknown而非排名误导。
- 清理建议由完整证据支持，不误删索引/历史记录。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 49. 修复生效账仅覆盖2项工单且只保存首次事件，无法62项持续回查

**编号 `R342-049`｜P1｜施工批次 B11｜证据等级 CONFIRMED_SOURCE_REQUIREMENT_GAP**

### 证据与源码定位

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RepairEffectLedgerR341.java` ：EFFECT主键(install_id,issue_id)，每工单同安装仅一行。
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RepairEffectLedgerR341.java` ：issueFor仅R335_CHECKPOINT_RECOVERY_RECEIPT→B10-03、R31_ATOMNET_PUBLISH→B07-04，其余return null。
- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RepairEffectLedgerR341.java` ：appendObservation使用INSERT OR IGNORE，后续同工单变化无法追加。
- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java` ：正式提交后已有RepairEffect hook，保留这条接线。
- `app/src/main/java/com/ethk3/novaonepro/BuildIdentity.java` ：验证安装身份后已有recordInstall调用，保留。

### 已证实的原因或代码机制

- 映射表是手写两项白名单，数据模型被定义为“First committed observation”，而旧工单要求每项修复生效和后续回查。新增有效发布也不足以单独证明B07-04结算反馈链完成，操作与业务验收粒度不一致。

### 尚不能断言的部分

- 本次空账不能唯一归因为此问题，因为对应生产操作本身也未出现；此源码缺口会在链路修复后继续阻碍全覆盖/回归监测。

### 具体处理

1. 将工单与可验收产物/操作/断言定义成版本化注册表；工程、设备、自然分别对应原件，非所有工单强行映射DB操作。
2. 兼容保留原首生效表，新增追加式effect observation keyed by install/issue/evidence_scope/window/observation_id或receipt键；禁止覆盖历史。
3. 业务commit后写outbox轻量事件，不让报告记录阻塞关键提交；幂等消费补生效/退化/恢复/未知状态。
4. 分别证明发布、消费、结算反馈；R31_ATOMNET_PUBLISH归发布证据，不能直接替B07-04整个反馈链签通过。
5. 报告按工单显示首次生效、最近状态、未满足断言、回归原因/修复位置；旧事件缺scope保持未知。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/RepairEffectLedgerR341.java`：INCREMENTAL_SCHEMA_AND_REGISTRY；方法：installWithin, issueFor, appendObservation, read。

- `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`：PRESERVE_HOOK_ADD_BOUNDED_EVENT；方法：observeResearchCommitR341。

- `app/src/main/java/com/ethk3/novaonepro/BuildIdentity.java`：PRESERVE_INSTALL_BINDING；方法：verifyAndPersist。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/FieldFollowupR34.java`：READ_PER_ISSUE_CURRENT_AND_FIRST_EFFECT；方法：capture。

- `app/src/main/java/com/ethk3/novaonepro/LifecycleSchemaR31.java`：REGISTER_COMPATIBLE_TABLE_MIGRATION；方法：Definition, definitions, requireDefinitions, advanceWithinMigration。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_049。

### 主链接入与保留

沿现有BuildIdentity及DatabaseCoordinator提交钩子观测唯一主链，不在报告端代跑或修改算法。

- 保留：当前安装/source/APK/process/db身份
- 保留：原始commit receipt
- 保留：已有两项旧生效记录
- 保留：ENGINEERING/DEVICE/NATURAL分开

### 独立验收

- 62原工单和新增工单都有证据来源或明确NOT_OBSERVED，不能空白默认PASS
- 同工单首次成功→后续失败→恢复三条事件可追溯
- 重复receipt不重复计入，scope不同不串接
- 报告写失败不回滚已完成核心业务
- 发布成功但无反馈消费时B07-04仍未通过

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 50. 18个研究方向没有本次源码的可用产物与精确消费回执，读图不能替代算法闭环

**编号 `R342-050`｜P1｜施工批次 B11｜证据等级 CONFIRMED_CURRENT_SOURCE_RESEARCH_GAP**

### 证据与源码定位

- `audit/report/UPGRADE_DIAGNOSIS.json` $.capabilityRepairR32.rows[102:120]：[{"id": "R01", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R02", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R03", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R04", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R05", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R06", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R07", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R08", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R09", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R10", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R11", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R12", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R13", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R14", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R15", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R16", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R17", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}, {"id": "R18", "currentSourceProducedFrames": 0, "actualSourceConsumptionReceipts": 0}]
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[251]：{"table": "r30r1_proposal", "rows": 0, "states": []}
- `audit/report/DIAGNOSTIC_SCOPE.json` $.tables[250]：{"table": "r30r1_consume", "rows": 0, "states": []}
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `stored, classifyFrameR335`，定位 186。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `stored, classifyFrameR335`，定位 101。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `stored, classifyFrameR335`，定位 72。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `stored, classifyFrameR335`，定位 182。

### 已证实的原因或代码机制

- R01–R18 currentSourceProducedFrames=0、actualSourceConsumptionReceipts=0。已实现的观测器能产诊断frame，也明确区分usableResearchProduct；当前还未形成方向原件→合法研究产品→算法行为改变→结果的完整因果链。

### 尚不能断言的部分

- 有些R11/R18等正式只读审计面板不应强行成为训练输入；每方向consumer、role、允许动作以原合同为准。旧frame不等于当前故障或当前成功。

### 具体处理

1. B11按165附录逐R01–R18只读连接前批原件：RES12已提交的dataset/student/parent输入、RES19的真实measure、RES20的动作/消费/结果；缺哪段显示具体source和责任问题，不在页面补生产。
2. 展示并校验原source_run、ABI、commit、dataset/revision、product hash、validPointCount及role；当前版本与历史原件分别过滤，读取错误保持UNKNOWN而非0。
3. 使用前批已生成的before/after与exact action/child回执解释研究如何改变行为；B11只做关联与展示验证，不新建ResearchContext动作、训练任务或研究调度。
4. 正式R11/R14/R18只读联查其原冻结/数值/资格与主链身份；渲染、分页、刷新和取消不执行模型训练或改写正式未来证据。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`：MODIFY_OR_VERIFY；方法：stored, classifyFrameR335。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_050。

### 主链接入与保留

B11是已提交研究事实的只读汇合点。RES12→RES19→RES20在既有后台owner上生产，B11不新增或触发训练/动作，不成为前批生产依赖。

- 保留：18方向算法、formalReadOnly分流、validPointCount、before/after证据与既有图表。

### 独立验收

- 18方向各展示前批的PRODUCED+合法消费、FORMAL_READ_ONLY或specific WAIT/UNKNOWN原状态；不能把渲染成功等价为算法已完成。
- B11重放前批禁用对照的原件，核实所显示行为差异与真实source/action/child一致；不重新训练或为无数据方向填造曲线。
- 打开、刷新、分页、取消页面时，训练任务数、研究动作数和正式冻结/资格表均不因展示操作新增；后台原研究owner不依赖UI访问。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 51. 生命周期报告的晋级权重说明与实际证书协议不一致

**编号 `R342-051`｜P1｜施工批次 B11｜证据等级 SOURCE_AND_REPORT_CONFIRMED**

### 证据与源码定位

- `REPORT_DETAILS.txt` RSEC-317：静态说明声称累计600/840/1200后权重12%/20%/30%
- `app/src/main/java/com/ethk3/novaonepro/AlgorithmLifecycleReportR927.java` ：两处重复硬编码上述说明
- `app/src/main/java/com/ethk3/novaonepro/CertificateServiceV2R929.java` ：历史梯级实际为6%/7%/8%，并说明不能为当前CP4 REAL发证
- `app/src/main/java/com/ethk3/novaonepro/ResearchGatePlanR967.java` ：CP3/CP4 installed policy具有独立policy_hash和consumer_cap，不能用旧展示文案推断当前权重

### 已证实的原因或代码机制

- 报告生产和快照渲染复制了过期权重文案，未从当前消费者绑定的冻结policy生成说明。它会误导施工、验收和用户，但本证据不能证明发生了超权重REAL。

### 尚不能断言的部分

- 当前没有合格策略实际正权重贡献，因此不能以运行样本验证每条证书域的实际晋级效果。

### 具体处理

1. 由报告所引用的冻结实验/证书身份解析gateVersion、policy_hash、consumer及cap，生成同一份说明；不让两个render入口手写阈值。
2. 旧历史梯级、当前CP4、R27、SU2按身份分别展示；只用于历史展示的协议显式标注不可授权当前REAL。
3. 保留原质量阈值和消费者限制，不将展示文案修复变成提高权重或降低门槛。
4. 对渲染值与实际生产消费者读取值做一致性测试，覆盖旧协议、未知协议、缺字段、过期证书与无样本。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/AlgorithmLifecycleReportR927.java`：MODIFY；方法：capture, captureSnapshotCP4。

- `app/src/main/java/com/ethk3/novaonepro/CertificateServiceV2R929.java`：KEEP_CONTRACT_VERIFY；方法：evaluate, permitsRankingInfluence。

- `app/src/main/java/com/ethk3/novaonepro/ResearchGatePlanR967.java`：READ_CONTRACT_VERIFY；方法：installed, forWindow。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java`：KEEP_CONTRACT_VERIFY；方法：evaluate, contract。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateSU2.java`：KEEP_CONTRACT_VERIFY；方法：read, maintain。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_051。

### 主链接入与保留

只读报告从主链冻结证书合同取值；不新增决策或授证通路。

- 保留：所有既有证书协议及质量门槛
- 保留：唯一DecisionGateEngine
- 保留：用户真实开关

### 独立验收

- 报告显示cap等于所绑定消费者的精确合同值
- 旧协议不成为当前REAL授权
- 缺policy时显示UNKNOWN而不使用常量替补
- 相同快照两种导出说明一致

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 52. K3九层失败诊断仍依赖静态PROVEN和文本关键词，不能准确归因当前运行模型

**编号 `R342-052`｜P1｜施工批次 B11｜证据等级 SOURCE_AND_REPORT_CONFIRMED**

### 已观察的事实

RSEC-066将NUMBER/RANK/CALIBRATION列BLOCKED，其余六层列UNAVAILABLE，并给前三层confidence=1.0。该置信字段并非对本机数值失败根因的统计置信。

### 证据与源码定位

- `REPORT_DETAILS.txt` RSEC-066：NUMBER、RANK、CALIBRATION=BLOCKED；THIRD/WINDOW/REGIME/MOTIF/RELATION/CONSENSUS=UNAVAILABLE
- `app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java` ：diagnoseSnapshotCP4直接读V51*Artifact.PROVEN；stateLayerSnapshotCP4按summary关键词分类
- `app/src/main/java/com/ethk3/novaonepro/V51NumberModelArtifact.java` ：工程种子DATA_SHA256/PARAMETER_SHA256=UNTRAINED，PROVEN=false
- `app/src/main/java/com/ethk3/novaonepro/V51ActionRankerArtifact.java` ：工程排序种子DATA_SHA256=UNTRAINED，PROVEN/RANK_ORDER_PASSED=false

### 已证实的原因或代码机制

- 诊断器把静态工程种子及旧状态summary当作当前模型质量事实来源，不能区分该期实际读取了哪一种冻结模型、哪一个证书域和哪些真实消费收据。文本中出现RISK/DRIFT等词也不能单独证明故障因果。

### 尚不能断言的部分

- 不能据此宣称整个软件从未有任何数值训练；需要当前canonical模型身份与原artifact/训练/消费记录核对，确定当前是工程基线、旧模型还是新模型。

### 具体处理

1. 每个目标期从canonical/compiled合同固定实际模型、特征、排序、校准、选择器和证书域身份，再连接相应原件；静态种子状态单列。
2. 固定canonical原bundle hash/target；当前有效期检查使用ModelBundleRuntimeR932.forEvidence，历史只读回放使用现有forHistoricalEvidence(hash,target,frozenAt)按原冻结时刻校验。不得读取当下可变指针替代旧模型，也不能借历史回放资格恢复今天REAL。
3. 在B06/B08/B09既有训练、冻结和资格合同中按实际模型身份核验：NUMBER的同目标OOS/top-k/Brier/10×10与Sinkhorn约束；RANK的完整1200动作同issue分组及Rank1/MRR/NDCG/顺序一致性；CALIBRATION的该证书域原拟合/隔离/验证及自然未来分离。缺件在所属训练/资格批次补真实生产链；B11只消费这些原件，不新增训练owner，不把PROVEN常量改true。
4. THIRD/WINDOW/REGIME/MOTIF/RELATION/CONSENSUS分别取得冻结原件、同目标反事实或分块消融，输出事实/假设/拒绝/待取证四层；具体九层实验见coverage/NINE_FAILURE_LAYERS.json。
5. 删除将静态判定confidence=1.0展示成根因已确定的语义；保留旧字段兼容并新增confidence_kind/evidence_scope；报告live与snapshot入口共用相同类型化诊断。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java`：MODIFY；方法：diagnose, diagnoseSnapshotCP4, stateLayer, stateLayerSnapshotCP4。

- `app/src/main/java/com/ethk3/novaonepro/V51NumberModelArtifact.java`：KEEP_ENGINEERING_BASELINE_VERIFY；字段：PROVEN, DATA_SHA256, PARAMETER_SHA256。

- `app/src/main/java/com/ethk3/novaonepro/V51ActionRankerArtifact.java`：KEEP_ENGINEERING_BASELINE_VERIFY；字段：PROVEN, DATA_SHA256, RANK_ORDER_PASSED。

- `app/src/main/java/com/ethk3/novaonepro/V51PermutationNumberModelEngine.java`：VERIFY_MODEL_INPUT_AND_TRAINING_BINDING；方法：predict, productionModelProven。

- `app/src/main/java/com/ethk3/novaonepro/V51ActionRankerEngine.java`：VERIFY_MODEL_INPUT_AND_TRAINING_BINDING；方法：rankDetailed, rankOne。

- `app/src/main/java/com/ethk3/novaonepro/V51SelectedActionCalibrationEngine.java`：VERIFY_CURRENT_CALIBRATION_BINDING；方法：calibrateDetailed, probabilityEvidence。

- `app/src/main/java/com/ethk3/novaonepro/ModelBundleRuntimeR932.java`：READ_FROZEN_MODEL_IDENTITY_VERIFY；方法：forEvidence, forHistoricalEvidence。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_052。

### 主链接入与保留

该诊断只读主链已提交事实；模型训练由原训练owner完成，B08既有feedback owner独立于UI消费诊断事实并决定下一轮研究动作；B11只联查展示，不入队、不直接改当期预测。

- 保留：既有100号码/1200动作计算与合法工程SHADOW
- 保留：成熟号码约束与排序实现
- 保留：现有九类研究框架
- 保留：四类证书完整合同

### 独立验收

- 同一个目标/快照报告的模型身份与canonical一致，换模型不得沿用前模型的PROVEN/根因。
- 九层均有精确原件或明确缺件状态；关键词命中不能自动签数值故障。
- 通过/未通过/未知/工程种子各自正确显示；诊断不改决策不回写当期预测。
- NumberQ、Ranker和校准的训练/冻结/消费反例回归纳入B06/B08/B09既有模型合同，B11只消费已形成事实。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 53. 96项检查完成与语义封包成功不等于96项能力通过

**编号 `R342-053`｜P1｜施工批次 B11｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

96个check_result：OBSERVED44、NOT_APPLICABLE16、NOT_PROVEN21、PASS4、FAIL4、WARN7；R30 overallAcceptance=FAIL_RECORDED_METRICS，naturalQuality=NOT_PROVEN，physicalDevice=NOT_RUN_OR_RECEIPT_UNAVAILABLE。

### 证据与源码定位

- `report/REPORT_DETAILS.txt` all kind=check_result records：96 execution results; health与domain_outcome分别保留
- `report/REPORT_DETAILS.txt` RSEC-029：静态stateMachine流程摘要，原health=NOT_APPLICABLE
- `report/R30_REPORT.json` $.overallAcceptance / $.naturalQuality / $.physicalDevice：FAIL_RECORDED_METRICS
- `report/R31_SEMANTIC_RECEIPT.json` $.checks / $.rawEvidence：rawEvidence=false
- 源码 `app/src/main/java/com/ethk3/novaonepro/ReportCatalogCP4.java`，方法/成员 `hasCompleteResult, writeReadableSummary`，定位 64–80,145–146。
- 源码 `app/src/main/java/com/ethk3/novaonepro/ReportSemanticVerifierR31.java`，方法/成员 `verifyDelivery, writeReceipt`，定位 111,165。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportAssessmentR33.java`，方法/成员 `group`，定位 54。

### 已证实的原因或代码机制

- DIAGNOSTIC类别的完成合同是存在domain outcome及evidence；OBSERVED/NOT_PROVEN也可属于执行完成。
- 包内完整性/引用/hash校验属于报告完整性，不能推导业务工程/效果已经通过。

### 尚不能断言的部分

- 不能把报告全文正向条款或“completes_contract=true”当对应需求落实。
- RSEC-029为NOT_APPLICABLE静态流程摘要，写先Settlement再TargetRequest，不是生产时序收据；实际BusinessReconciler先尝试下期持久请求的时效设计必须保留，不能依旧摘要重排MAINLINE。
- 96项状态统计44 OBSERVED/16 NOT_APPLICABLE/21 NOT_PROVEN/4 PASS/4 FAIL/7 WARN对应health；domain_outcome另有细化结果，不应覆盖为相同字段。

### 具体处理

1. 报告显示三列：检查执行/证据结论/业务验收，字段含义固定。
2. 汇总所有FAIL/WARN/NOT_PROVEN/UNAVAILABLE/EMPTY并映射真实需求ID与施工项，保留N/A理由。
3. CI和发布门要求真实运行收据；只执行检查、源码出现类名、编译通过不能升格。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/ReportCatalogCP4.java`：MODIFY_OR_VERIFY；方法：hasCompleteResult, writeReadableSummary。

- `app/src/main/java/com/ethk3/novaonepro/ReportSemanticVerifierR31.java`：MODIFY_OR_VERIFY；方法：verifyDelivery, writeReceipt。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportAssessmentR33.java`：MODIFY_OR_VERIFY；方法：group。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_053。

### 主链接入与保留

同一报告事实模型→需求与工程验收门。

- 保留：全部96检查、不可变快照、附件hash和缺失显式标识，不删失败项凑通过。

### 独立验收

- 96逐项覆盖账能从原报告重算；PASS仅用于其明确范围。
- 更换健康结论不能改变原始检测数据与历史报告。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 54. 44项工程/质量指标没有已验证通过项，静态handler声明不能算运行效果

**编号 `R342-054`｜P1｜施工批次 B11｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

44 metrics：INSUFFICIENT35、RECORDED_UNVALIDATED5、BLOCKED1、UNKNOWN_SOURCE_MISSING1、NOT_DEPLOYED1、UNKNOWN_STALE_SYNTHETIC_SOURCE1；M23 BLOCKED(81/82，STATIC_DECLARED_HANDLERS_NOT_RUNTIME_EFFECT)。M06/M22/M27等0值样本数为0，不能证明唯一性/冲突清零。

### 证据与源码定位

- `report/R30_REPORT.json` $.metricResults：all 44 metrics
- `report/R30_REPORT.json` $.metricResults[metricId=M23]：STATIC_DECLARED_HANDLERS_NOT_RUNTIME_EFFECT
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportAssessmentR33.java`，方法/成员 `evaluate, failed, finite, group, map, verifiedPass`，定位 1–80。
- 源码 `app/src/main/java/com/ethk3/novaonepro/ReportCatalogCP4.java`，方法/成员 `hasCompleteResult`，定位 64–80。

### 已证实的原因或代码机制

- 工程收据不存在、来源不可用、角色/版本不符时，采集值和静态表数量无法验证运行要求。

### 尚不能断言的部分

- M01缺metric同角色收据并不否定manifest已有安装身份核验；应修证据桥接，不能重复说装错版本。
- M31规则覆盖1.0来自16条规则，不能等同全软件故障发现率100%。

### 具体处理

1. 逐metric固定definition、role、release、watermark、sample、denominator、required receipt与未通过原因。
2. 优先补M02成熟功能、M03–07实时主链/写等待、M10–14训练与设备、M20checkpoint/M24消费/M28链路/M34升级的真实证据。
3. M23每个handler需至少绑定真实原因→动作→提交→回读→效果/失败收据；声明存在只计覆盖准备。
4. 空样本唯一性和重复事件注入场景必须明确NOT_PROVEN，不能将零计数升格。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportAssessmentR33.java`：MODIFY_OR_VERIFY；方法：evaluate, failed, finite, group, map, verifiedPass。

- `app/src/main/java/com/ethk3/novaonepro/ReportCatalogCP4.java`：MODIFY_OR_VERIFY；方法：hasCompleteResult。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_054。

### 主链接入与保留

需求验收表消费唯一事实链，handler修复沿原owner执行。

- 保留：原验收门、旧成熟功能、证书与PIT/唯一性保护。

### 独立验收

- 44指标逐项能追到可复算原件；当前缺项不会被汇总PASS遮盖。
- 重复/重启/异常场景下主链一致性经实际事件验证。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 55. 多时点和多进程观测并存，需要范围对齐才能判断矛盾或改善

**编号 `R342-055`｜P1｜施工批次 B11｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

runtime sample1795585570811 PID18685；SQL scope sample1795586335233 epoch f79523b237a299a19124c702；lifecycle约1795587086xxx且标SEPARATE_LIFECYCLE_READS_AFTER_FACT_WATERMARK；cohort有retained history/current release分隔。

### 证据与源码定位

- `report/report-text-4ffdbb3f1c861fc0fccc4bbce1c7dba5413582b3d400034000d367ceeef7cfdb.txt` RSEC-010：status=HEALTHY; last30Draws=30; predictions=7
- `report/REPORT_DETAILS.txt` RSEC-052：rootCause=PREDRAW_COVERAGE_MISSING; 当前严格coverage另为1/30
- `report/runtime-observation.json` $.sampleWallMs / $.r341ProcessStartScope：runtime process scope
- `report/source-manifest.json` $.r341SqlObservationScope：scopeKind=SQL_SNAPSHOT
- `report/R30_REPORT.json` $.lifecycleEvidenceR31.*.sourceScope：SEPARATE_LIFECYCLE_READS_AFTER_FACT_WATERMARK
- `report/runtime-opportunity-cohort.json` $.retainedHistoryScope / $.releaseProgress：SEPARATE_FROM_CURRENT_RELEASE
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`，方法/成员 `add, capture, count, has, rows, sql`，定位 1–100。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`，方法/成员 `snapshot`，定位 270–272。

### 已证实的原因或代码机制

- 报告已在部分结构声明范围，但汇总解读必须保留；不同时间/进程的数值不能直接相减或互相覆盖。

### 尚不能断言的部分

- 不能把后续SQL新数据当runtime采样时已存在，也不能用后续诊断进程零队列否定先前主进程背压。
- RSEC-010统一总览HEALTHY且旧覆盖7/30，与RSEC-052及059严格当前覆盖1/30并存；先对齐生产者、角色、时间窗、过滤与watermark才能判是否实现矛盾，不能任选较好值作为验收。

### 具体处理

1. 统一ObservationEnvelope：sourceCommit/release/databaseIdentity/process/boot/role/window/watermark。
2. 同指标跨来源比较前先校验范围，不匹配明确NOT_COMPARABLE。
3. 需求验收聚合同范围receipt，历史保留/当前过程/诊断过程分别统计并显示时间。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java`：MODIFY_OR_VERIFY；方法：add, capture, count, has, rows, sql。

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java`：MODIFY_OR_VERIFY；方法：snapshot。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_055。

### 主链接入与保留

统一只读观测协议贯穿所有报告章节。

- 保留：历史全量原件、不可变快照、独立诊断读取，不强行把动态采样伪装原子。

### 独立验收

- 使用同issue异process/异release数据能自动识别不可比较；无无范围的0或累计P95。
- 审阅者能追溯每个结论对应哪个生产者与时间窗。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 56. 工程闭环和自然质量尚未验收，不能把升级目标写成高胜率或代代必强承诺

**编号 `R342-056`｜P0｜施工批次 B12｜证据等级 CONFIRMED_UNPROVEN_ACCEPTANCE**

### 证据与源码定位

- `audit/report/R30_REPORT.json` $.overallAcceptance：FAIL_RECORDED_METRICS
- `audit/report/R30_REPORT.json` $.naturalQuality：NOT_PROVEN
- `audit/report/R30_REPORT.json` $.metricResults[36:44]：[{"id": "Q01", "name": "自然ACT命中", "status": "INSUFFICIENT", "value": null, "missingReason": "缺少同版本、同角色的测量原件：真机 FUTURE ACT 命中率（事前登记策略）", "nextTrigger": "采集 真机 FUTURE ACT 命中率（事前登记策略）；按 [FR-B11-03] 验证，禁止用编译通过代替"}, {"id": "Q02", "name": "真实频率", "status": "NOT_DEPLOYED", "value": null, "missingReason": "当前没有原REAL行且没有有效ACTIVE证书；FUTURE_ACT_RATE与正式REAL频率分列", "nextTrigger": "采集 真机实际出手频率（不补配额）；按 [FR-B11-03] 验证，禁止用编译通过代替"}, {"id": "Q03", "name": "真实统计优势", "status": "INSUFFICIENT", "value": null, "missingReason": "缺少同版本、同角色的测量原件：SequentialQualificationR27 logEUp ≥ log(1/α_i)", "nextTrigger": "采集 SequentialQualificationR27 logEUp ≥ log(1/α_i)；按 [FR-B05-04, FR-B11-03] 验证，禁止用编译通过代替"}, {"id": "Q04", "name": "跨块稳定", "status": "INSUFFICIENT", "value": null, "missingReason": "需事前固定块规则及逐策略同角色日历块；不从本次展示子集事后挑好窗口", "nextTrigger": "采集 事前冻结窗口规则下各块命中率；按 [FR-B11-03] 验证，禁止用编译通过代替"}, {"id": "Q05", "name": "校准与BSS", "status": "INSUFFICIENT", "value": null, "missingReason": "缺少同版本、同角色的测量原件：CalibrationR30：等质量ECE、BSS、支持覆盖（与等宽ECE并列）", "nextTrigger": "采集 CalibrationR30：等质量ECE、BSS、支持覆盖（与等宽ECE并列）；按 [FR-B06-06, FR-B11-03] 验证，禁止用编译通过代替"}, {"id": "Q06", "name": "进化提升", "status": "INSUFFICIENT", "value": null, "missingReason": "缺少同版本、同角色的测量原件：父子配对差单侧95%下界", "nextTrigger": "采集 父子配对差单侧95%下界；按 [FR-B05-01, FR-B08-01, FR-B11-03] 验证，禁止用编译通过代替"}, {"id": "Q07", "name": "有效资产产量", "status": "RECORDED_UNVALIDATED", "value": 0, "missingReason": "当前有效证书为0；不隐去负结果、不补配额，Q01/03/04/05逐资产独立验收", "nextTrigger": "采集 ACTIVE 证书资产数；按 [FR-B11-03] 验证，禁止用编译通过代替"}, {"id": "Q08", "name": "空信号与优势检出力", "status": "UNKNOWN_STALE_SYNTHETIC_SOURCE", "value": null, "missingReason": "原注册回执不是当前源码；旧源实验不能签当前Q08。", "nextTrigger": "采集 R30SequentialNullSimTest 空信号/功效 + NullEvolutionR27；按 [FR-B07-05, FR-B11-03] 验证，禁止用编译通过代替"}]
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `stored`，定位 83。
- 源码 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`，方法/成员 `stored`，定位 101。

### 已证实的原因或代码机制

- 报告工程overallAcceptance=FAIL_RECORDED_METRICS、naturalQuality=NOT_PROVEN。自然质量指标未验证，历史计数、静态对象覆盖、工程编译通过不能替代独立未来收益证据。

### 尚不能断言的部分

- 本审计能确定已记录事实、代码条件和首处缺件，不能在没有设备复跑与完整原件时证明所有算法根因或承诺命中提升。最终闭环施工应区分工程完工、设备达标、自然质量观察三种状态。

### 具体处理

1. 逐指标使用原定义、时间范围、分母和原件；工程修复用明确输入/输出/恢复/延迟测试，不借未来高胜率替代。
2. 质量评价保留基线、冻结协议、同目标配对、置信区间、全搜索试验登记及负向/合成对照；过期旧验证不能当当前通过。
3. 修复后导出同schema的新报告，与本报告按源码、APK、snapshot和指标定义做可审计差异，逐项关闭证据缺口。
4. 推广规则只允许有证据的子代替换冠军；无改进时保留成熟模型和完整失败记录，继续研究。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java`：MODIFY_OR_VERIFY；方法：stored。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B12ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_056。

### 主链接入与保留

只在既有数据、任务、版本、结算和资格链中补齐此段；保留统一写入、owner/fence、PIT 与正式主链授权边界，不建立旁路成功计数。

- 保留：成熟功能、完整失败原件、固定评价协议、保守资格与可回滚冠军。

### 独立验收

- 所有本卡都有实现/单元与集成验证/设备原件/剩余观察四级状态，不以文档完成冒充软件完工。
- 不存在无资格REAL、未来泄漏、抽样挑选、重复n或为了PASS改低门槛；真实不达标保持NOT_PROVEN。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。

## 57. 本包为DIAGNOSTIC，没有数据库/APK/模型训练原字节，外部复现能力受限

**编号 `R342-057`｜P1｜施工批次 B12｜证据等级 OBSERVED_WITH_CAUSE_BOUNDARY**

### 已观察的事实

reportProfile=COMPLETE_DIAGNOSTIC_R68、rawEvidenceIncluded=false，omittedPayloads列出installed APK/raw SQLite/model/training/retention binaries/historical duplicate ledgers。snapshotSha256与bytes只是所用快照身份。

### 证据与源码定位

- `report/source-manifest.json` $.reportProfile / $.rawEvidenceIncluded / $.omittedPayloads：COMPLETE_DIAGNOSTIC_R68; rawEvidenceIncluded=false
- `report/R31_SEMANTIC_RECEIPT.json` $.rawEvidence：false
- 源码 `app/src/main/java/com/ethk3/novaonepro/ReportReadContextCP4.java`，方法/成员 `compactDiagnosticR64, evidenceMemberR64`，定位 42–71。
- 源码 `app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java`，方法/成员 `capture`，定位 64–95。
- 源码 `app/src/main/java/com/ethk3/novaonepro/ReportDiagnosticEvidenceR64.java`，方法/成员 `write`，定位 15–18。

### 已证实的原因或代码机制

- 诊断封包可以覆盖全部检测项和全表计数，仍不包含可独立SQLite重跑、APK重算、模型state重放的原字节。

### 尚不能断言的部分

- 无法在本包独立证明全部表数据一致性/触发器实际生效/二进制APK与源码可重建同一/原模型参数连续性/全部CAS闭包恢复/缺陷在真机复现。
- 这些限制不是“原件必然不存在”的证明；很多字节仍可能在原设备。

### 具体处理

1. 施工复现时收集明确授权和最小必要的原始副本：数据库一致备份、相关checkpoint/CAS、安装APK/构建receipt与运行trace。
2. 保留当前DIAGNOSTIC便捷导出模式，同时明确RAW evidence范围/容量与可复现步骤。
3. 在副本上执行触发器/迁移/故障重放并记录hash、版本和结果，不能修改用户原设备数据来证明问题。

### 文件与方法

- `app/src/main/java/com/ethk3/novaonepro/ReportReadContextCP4.java`：MODIFY_OR_VERIFY；方法：compactDiagnosticR64, evidenceMemberR64。

- `app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java`：MODIFY_OR_VERIFY；方法：capture。

- `app/src/main/java/com/ethk3/novaonepro/ReportDiagnosticEvidenceR64.java`：MODIFY_OR_VERIFY；方法：write。

- `app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B12ContractTest.java`：PLANNED_TEST_IMPLEMENTATION；方法：test_r342_057。

### 主链接入与保留

原导出owner补复现附件与范围标识，避免独立旁路报告。

- 保留：轻量完整诊断、原设备原件、快照隔离和既有备份。

### 独立验收

- 本报告能区分已证现象/已核源码/待复现运行因果，未做真机测试不写通过。
- RAW复现材料齐备后可由独立人按同脚本重现关键断点。

**回滚边界**：保留已提交canonical、结算、故障/模型原件和历史签收；优先停用新分支或回退模型指针。不得清空证据或降低质量门槛。

**本轮状态**：方案已列明，应用修改、设备效果与自然质量没有在本轮代签。
