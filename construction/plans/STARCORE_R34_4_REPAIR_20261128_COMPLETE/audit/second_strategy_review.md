# 第二轮策略链施工方案审阅

审阅发布基线：7947177eb26a5508e8c34c1159cb179ccbe15a89。唯一应用底座：211 / versionCode 577311211，5c7f1b36eaee6036df280e85835bb0fb4ce0b40d；包名 com.starcore.research.r13。

## 结论

**确认3处计划合同缺口：2项P1（现代生产责任/验收漏项、租约恢复验收矛盾），1项P2（初次冻结顺序跨合同歧义）。没有据此证明新的应用运行缺陷，也未发现B06/B07/B08/B09依赖DAG的新循环。** 应局部补齐后继续原46卡、12批实施，无须重造既有owner。

审阅了三份合同、批次、执行提示、创造附约和当前工单，并核对精确211现有调用。11个需要加入当前施工锚的Java文件均实际读取，UTF-8字节数/SHA256重新计算，与1407文件继承基线一致。完整继承清单包含这些源码，不等于当前施工卡已指定生产与验收责任。

可直接合入的原值→新值在 second_strategy_amendments.json。必须核对原值后合并，再重生当前卡片、ACCEPTANCE、SOURCE_ANCHORS、指标责任与清单，原审计记录不改。

## 1. 旧两份合同仍把 rolling 写在首次冻结之前，与当前 CODEX/附约的独立初次冻结不一致（S2-ST01 / P2）

### 准确位置

- contracts/CREATION_TRAINING_AND_QUALITY.md:39–42
- contracts/PIPELINE_AND_PRESERVATION.md:53
- ISSUES.json: R344-034.repair[0]
- CODEX_EXECUTE.md:115（已正确）
- CREATION_EXECUTION_ADDENDUM.md:80–86（已正确）

### 可核证据

1. 旧步骤4→5→6仅列开发回执→RollingLearner→RollingFrozenPublisher，未列独立 FREEZE_CANDIDATE；R034首条施工又写开发模型/冻结后才 rolling。
2. 211 的 FrozenCandidateAdapterR958.prepare/commitDevelopmentFreezeCP8 读取开发 artifact/receipt/protocol，并直接调用 ResearchEvaluationRunR967.registerDevelopmentCP8；不要求 rolling event/version。RollingLearner 也不应等待首次冻结窗口完成。

### 影响与最小修正

按旧合同顺序施工可能遗漏初次冻结或误加跨 lane 屏障；最新 CODEX/附约已经正确，因此不能据此宣称当前源码存在新的必然死锁。

- 合同步骤拆成首次开发冻结与 rolling 两条分支；R034首条施工去掉首次冻结作为 rolling 前置；不改依赖 DAG、不另建 owner。

### 现有源码入口

- [FrozenCandidateAdapterR958.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java)
- [ResearchEvaluationRunR967.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/ResearchEvaluationRunR967.java)
- [RollingLearnerR343.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/r952/RollingLearnerR343.java)

## 2. 现代 AST3 初次冻结、CAL241、正式窗口和证书签发的既有 owner 未进入当前施工卡与指标责任链（S2-ST02 / P1）

### 准确位置

- ISSUES.json: R344-031.source_files/repair/acceptance（当前止于 FREEZE enqueue）
- ISSUES.json: R344-043.source_files/repair/acceptance（R27生产链与R974消费端）
- ISSUES.json: R344-044.repair/acceptance（Q05/M26仅R27）
- SOURCE_ANCHORS.json（现代生产链10个已有owner未列当前锚；共同lease是S2-ST03的第11个增量锚）
- ACCEPTANCE_MATRIX.json: R031/R043/R044
- BATCHES.json: B06/B09步骤与退出文字

### 可核证据

1. R031只要求receipt/artifact→FREEZE enqueue，没有实际R967 evaluation注册及目标期变化/同artifact重放验收。
2. R043当前源码责任列LiveShadows/CalibrationRows/CalibrationR30/CertificateGate/SequentialQualification/MainlineAdapter等R27 producer，加R974消费端；缺现代R958→R932→R929原生产链。R044同样只明确R27 Q05/M26。
3. R958 captureOneCP12在正式窗口freezePair之前调用CandidateCalibrationR932.captureOrPredict；GlobalResearchScheduleR932提供有界预约。合法raw CAL收集不要求自然证书。
4. CandidateCalibrationR932原合同为241观察（前120+间隔1+后120，前120内部FIT80/SELECT40/REFIT120），FITTED与CALIBRATION_QUALIFIED分开，质量原件不可变；不是R27 minActs250。
5. FixedWindowCertificatePublisherR932.prepareCertificateCP12验固定plan/lifetime/原矩阵/exact scope，CP4分支n<360返回AUDITED_NO_CERTIFICATE；R929 authority存gate/history/certificate；UnifiedResearchChoice和R974另有R958原消费分支。

### 影响与最小修正

执行者可以只验R27校准/证书和FREEZE入队便关闭相关卡，现代链的预约、校准、正式证据签发及消费仍未实际验收。只证明当前计划漏责任与验收，不能反推这些既有实现缺失，也不能单凭现场零计数确定具体运行阻断。

- R031加首次实际冻结/幂等/目标期变化验收；R043增已有现代producer→consumer责任和独立CAL/正式窗口/证书正负贯通；R044加指标family/path/contract/scope分账。
- 从211基线加入准确源码锚并保留owner；合同明确raw CAL采集不等待自身资格结果，避免新加循环；B06/B09仅补步骤/退出文字，不改DAG。
- Q05/M26/Q07/M28覆盖审计遇到相同缺口归本项单一根因，不另增问题数量。

### 现有源码入口

- [FrozenCandidateAdapterR958.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java)
- [ResearchEvaluationRunR967.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/ResearchEvaluationRunR967.java)
- [GlobalResearchScheduleR932.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/GlobalResearchScheduleR932.java)
- [CandidateCalibrationR932.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/CandidateCalibrationR932.java)
- [FixedResearchWindowR932.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java)
- [ResearchGatePlanR967.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/ResearchGatePlanR967.java)
- [FixedWindowCertificatePublisherR932.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/FixedWindowCertificatePublisherR932.java)
- [CertificateBundleServiceR929.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/CertificateBundleServiceR929.java)
- [ResearchPlatformStoreR929.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/ResearchPlatformStoreR929.java)
- [UnifiedResearchChoiceR932.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java)
- [CertifiedPredictionR974.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java)

## 3. R034把重开恢复和重复投递同lease混为一个正例，违反现有进程租约合同（S2-ST03 / P1）

### 准确位置

- ISSUES.json: R344-034.acceptance[1]
- issues/R344-034.md:50
- ACCEPTANCE_MATRIX.json: R344-034.acceptance[1]

### 可核证据

1. 在 FEATURES 与 TRAINING 阶段分别杀进程/重开并重复投递同 lease；cursor/prefix、checkpoint hash/generation CAS 保持一致，不多提交 optimizer step 或 outcome；开发模型与 rolling 模型各自独立验，不以一条历史 artifact 通过全链 M20。
2. ResearchTaskQueueR967.PROCESS为每进程UUID；Lease在claim时使用PROCESS并把task generation加1；owns同时验证PROCESS、单调到期、状态、generation、owner、lease_until、cursor；complete必须owns。

### 影响与最小修正

重启后的成功恢复不能复用旧进程lease。若把该条作为成功准则，会得到不可能通过的验收，或诱发删除正确stale-lease防线。现有源码防线应保留。

- 在 FEATURES 与 TRAINING 阶段分别于 prepare 后、提交前、已提交后杀进程并重开；按原租约到期/回收条件由新进程重新 claim 同一非终止 task/version/protocol/input，获得新 lease owner 与递增 task generation，再从已提交 cursor/prefix/checkpoint 恢复。与连续执行比较实际 optimizerSteps、验证 tape、终态 model hash 和 outcome；已提交步不重复，未提交批次不跳过。开发模型与 rolling 模型分别验，不用一条历史 artifact 代表全链 M20。
- 另设旧 lease 迟到/重放负例：新进程 claim 后，旧 owner/generation/cursor 的 prepare/commit/complete 不能更新任务、模型、checkpoint、optimizer 计数或新租约；允许记录拒绝原件。区分 task lease generation 与 rolling version/checkpoint generation：按各自 CAS 检验已提交状态，合法新提交可以递增，不要求跨重启维持旧 lease 或把 generation 永久固定。
- 补源锚ResearchTaskQueueR967；同步正式工单及ACCEPTANCE，保留original_audit_records不改写。

### 现有源码入口

- [ResearchTaskQueueR967.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/ResearchTaskQueueR967.java)
- [RollingLearnerR343.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/r952/RollingLearnerR343.java)

## 4. 现代链最小验收的实际边界

|阶段|原owner与必须验证的产物|不可替代条件|
|---|---|---|
|首次freeze|R958 prepare/commit→R967 registerDevelopment；实际EVAL、同artifact幂等、目标期变化|仅FREEZE入队不算冻结完成；不等rolling/证书|
|预约与raw CAL|GlobalResearchSchedule有界预约→R958事前capture→CandidateCalibration原始观察|合法PIT/截止、champion/selector、容量保留；不等自身质量结果|
|校准质量|CAL241及80/40/120内部流程；immutable quality和全部identity回读|FITTED不等于CALIBRATION_QUALIFIED；R27门槛不能移植|
|正式Future与签发|FixedResearchWindow/GatePlan→原publisher/BundleService→R929 gate/history/certificate|保留原plan/lifetime、机会、matrix、scope、alpha；CP4 n<360仍无证书|
|唯一消费|UnifiedResearchChoice→R974现代分支→合法下一边界|工程夹具只证实路径；自然消费须真实证据和B04工程条件|

R031新增首次实际freeze的正负例；R043新增整条现代producer→consumer的实际工程正负贯通；R044把Q05/M26/Q07/M28按路径与合同分账。卡、ACCEPTANCE与源码锚必须同步；只把这段写进说明而不落卡不能消除遗漏。

R034恢复验收必须保留同task/version/protocol/input，重新claim新lease；旧lease是负例。不同generation对应不同CAS域，不要求跨重启仍是旧进程身份。FAILED_TERMINAL原任务的专门继任恢复仍独立保留，不能拿正常checkpoint续算规则替换它。

## 5. 本轮未发现的新阻断

- 46卡/B00–B11范围及5候选非全部任务已明确；六ModelKind最终覆盖已明确。
- B06不等所有旧root；B07缺donor/teacher仅等待对应路径；无REAL/无predraw的独立DEV正例已明确。
- B08工程READY与PENDING_B09分开；B09现场消费只以B04工程主链条件为前提，不要求72h全部自然窗口；未发现B08/B09循环。
- typed mutation/crossover/fusion与NO_OP、参数变化已分账；反馈必须实际消费而非只写图表或计数。
- 三合同的导出原格式、失败/UNKNOWN仍能导出、原件保全与自然质量不得伪造约束继续保留。

补入现代来源映射不能修改报告现有格式、删除检查或增加“自然全通过才给用户导出”的门。原报告允许失败/UNKNOWN、96检查/44指标及保全条款继续有效。

## 6. 新增当前源码锚与取证完整性

|已有211源码|字节数|SHA256|
|---|---:|---|
|FrozenCandidateAdapterR958.java|70764|3dcd69caa13afa46b71e473fd15a60674017bc45b2f439ce10aa7b4568fb6ea7|
|ResearchEvaluationRunR967.java|29332|4855ca104ecdbd53139188589400d17643a4cc64d8ef56c08baa69140d261081|
|ResearchTaskQueueR967.java|23938|864ccc38ef314c2f28177239d6ad7070825aa3ddf3f19bf42d3d2746ef1995ca|
|GlobalResearchScheduleR932.java|44393|97b9a0eccb9fca25fc0da433b667c87a4575b07d2972fa228984748c51969dbc|
|CandidateCalibrationR932.java|41841|72fd66e9a434333a7e23fe4ee5bea0f2a3bb527109ef39f1560bdbf43d29d624|
|FixedResearchWindowR932.java|31999|dfb9645ec5c90c75ea5e910fa9f7e8a5fb0e9b48189262f51a1cf8afd8c0329d|
|ResearchGatePlanR967.java|42069|6ba5d77bdc2eb3424988020f4b2adba538665b1c6bf34a6d5204c5efc2cd9bff|
|FixedWindowCertificatePublisherR932.java|52861|290db4473dce5da2ab70b7224d1f5e37704c534f694be9222787ae0db6a7f872|
|CertificateBundleServiceR929.java|51799|7e07152d224e4342e4a7f95e200f52fd6b12c5bde2022c19393437c440b436e2|
|ResearchPlatformStoreR929.java|66351|ecdca4ba0956a7accd5749d89400d16b37d681e70fba63041c1ee53a907be2ec|
|UnifiedResearchChoiceR932.java|33123|d8df77be1b214d8f6d3af9d5615bd82a6a44f228a8f3cd3806a1619eeb7da75d|

11个文件都是211已有owner；10个对应现代生产链，ResearchTaskQueueR967为共同正确租约owner。方法锚和逐卡归属详见JSON，不新增重复实现。

## 7. 未执行事项与交接

- 未取得原用户完整SQLite/APK/全部.scdev与模型原字节；未执行Android或自然未来验收。
- 精确211源码只读核验用于约束施工方案，不把原件缺失、零计数或门禁拒绝自动判为运行根因。

只读审阅不等于应用已经返修。本次不承诺自然校准、证书或REAL必达，不修改任何自然资格门。

交接文件：second_strategy_review.json列3个发现；second_strategy_amendments.json含16个逐卡字段变更、3个批次文字变更、3个合同段落替换、11个精确源锚。父任务统一合入共享计划；本agent没有修改publication、应用或远端。
