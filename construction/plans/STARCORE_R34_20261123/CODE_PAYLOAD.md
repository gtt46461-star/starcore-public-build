# 随方案交付的真实代码与实施边界

本包共 **152条逐文件施工路径**。其中12份现有Java文件提供可应用delta及完整后态，6份Java提供完整新增源码，4份是完整方案/证据校验工具；其余130份提供精确前态与逐方法施工合同，仍需真实实现。完整文件卡是施工材料清单，不是应用完成清单。

## 1. 可应用的运行修复候选

[runtime/README](payload/runtime/README.md)、[精确清单](payload/runtime/PAYLOAD_MANIFEST.json) 和 [实测回执](payload/runtime/VERIFICATION.json)。

|批次|生产代码|已写出的行为|仍需完成|
|---|---|---|---|
|B01|DatabaseCoordinator、DeferredEvidenceWriterR36、DeferredWaitR34|恢复控制写类；窄范围资源等待分类；不误耗业务失败次数；保留cause/owner/CAS|实际故障spool、全部writer公平调度、生产线程和设备故障集成|
|B02|FetchLedgerR925、RawHttpIdentityR34|原body统一摘要；同原请求pending receipt在writer内恢复；无原件不伪造身份|真实HTTP失败原件复现、全抓取/历史补洞与设备时延|
|B06|DirectedResearchCreationR932|typed事务结果保真，不把拒绝或提交不明伪装成功|真实研究消费者、数据与生命周期接线|

## 2. schema单元迁移完整协议核

[SchemaUnitPlanR34完整源码](payload/runtime/supplement/after_full/app/src/main/java/com/ethk3/novaonepro/strategyv2/SchemaUnitPlanR34.java)、[旧owner接线合同](payload/runtime/supplement/INTEGRATION.md)、[SQL账本](payload/runtime/supplement/sql/schema_unit_journal.sql)。

以095的58张真实保护表建立INSTALL/VERIFY两阶段116个步骤，包含定义hash、原件root、cursor、fence、预算和提交UNKNOWN后耐久读回ACK。21个Java协议断言及6个SQLite故障场景实际通过。它仅覆盖保护对象迁移协议核，**不是整个794表数据库已完成迁移改造**；状态为PURE_CORE_NOT_ANDROID_INTEGRATED。真正的protectedTableFingerprints有界化、全部专用保护定义、checkpoint原件保全与Android adapter须按B01-03/B01-05实现。

## 3. 冷启重生与清理恢复

[B06说明](payload/learning/README_B06.md)、[11份源码对象清单](payload/learning/B06_FILE_MANIFEST.json)、[当前验证](payload/learning/verification/cold_rebirth_verification.json)。

3份新增类：ColdRebirthContractR34、ColdRebirthSchemaR34、ColdRebirthR34。8份旧owner delta：LifecycleSchemaR31、RuntimeCoordinatorR26、TaskContinuationR26、ModelReplaySU2、ResearchTaskRegistryR30R1、FoundryCampaignR26、WarmCandidateR26、RetentionR26。

新增处理器不复制旧权重/证书，按新身份将零训练子对象交给原Foundry/Workshop；候选与专家轮转，单片1个出生、最多16个可运行待训对象。已补实际清理SQL的active expert源引用保护、零样本元数据保护、原源过期/缺失后的新身份后继、全部16源过期仍可恢复，以及真正typed progress登记。原出生回执不可改写，出生仍不等于训练完成。

## 4. 合并规则

所有后态都以095为参考前态。先完成各批合同，再按delta合并累计代码；禁止把B06的RuntimeCoordinator/TaskContinuation整文件在B01/B03提前覆盖。FILE_CARDS.json的applies_in_batch与batch_materials明确了适用批次，其他批只有各自方法合同。

原始095上的5份累计补丁及3份ADD源码已在隔离目录组合应用，18份Java最终hash均与材料一致。这只证明所提供的局部补丁可组合，不能证明未写出的130份合同已经实现，也不证明Android调用者能编译。

## 5. 已执行验证

|范围|实际结果|不代表什么|
|---|---|---|
|运行helper/提取的精确策略|15个Java断言、3个SQLite事务合同|不代表完整Android类编译|
|schema协议核|21个Java断言、6个真实SQLite故障场景|不代表生产迁移owner接入|
|冷启合同|33个Java断言、44项SQLite/跨语言/进程恢复检查|不代表手机调度或训练数值完成|
|周期证据与交付检查|21个正反例|模拟SDK输出仅测解析，不是APK签名成功|
|累计代码材料|5份patch顺序应用、18份源码后态匹配|不代表66工单全部施工|

运行入口与只读验证顺序见 [TEST_RUNBOOK.md](TEST_RUNBOOK.md)。所有现有测试结果属于本包明确列出的输入/代码范围；未来施工修改之后要针对实际后态重验。
