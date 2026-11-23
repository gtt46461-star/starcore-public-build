# 方案交叉复核与修订闭环

这是方案和所附局部代码材料的修订记录。下表“已处理”只表示相应合同/材料已补正，不等于应用生产验收。初次审查保留原始NEEDS_REVISION结论。

|编号|实际修订|核对入口|
|---|---|---|
|IR01|REAL(game,target)唯一，position不可变载荷，新增多位置冲突拒例|PIPELINE_CONTRACT / B10-02 / test_plan_guards|
|IR02|机器帧/消费者随B06–B08实现，B11仅汇总展示与剩余覆盖|B06–B08 / B11-01/02|
|IR03|B04标签outbox投递与B07模型数值消费分开，各自同事务幂等|PIPELINE_CONTRACT / B04 / B07|
|IR04|机会日历事前固定，网络/进程/超时故障不得后验剔除|ACCEPTANCE_PROTOCOL / verify_period_trace|
|IR05|在线stateHash推进不清观察；静态frozen实例独立；旧精确Gate不放松|LEARNING_AND_QUALITY §3 / B09|
|IR06|nearest-rank分位数，completed与scheduled分母并列，unknown不作0|control/PERFORMANCE_AND_BUDGET / ACCEPTANCE_PROTOCOL|
|IR07|critical bootstrap→故障耐久化→research单元迁移，不用未准备schema排债|PIPELINE_CONTRACT / B01-06|
|IR08|66旧ID双向映射与具体测试合同；44空入口补成明确待实现case|R335_CROSSWALK / evidence/legacy_test_contracts.json|
|IR09|F23按LegacyTeacherOwner核原teacher，缺证WAIT_TEACHER|B06-02|
|IR10|S5 WAIT_PARENT/WAIT_CAPACITY保留公平恢复，不终态丢弃|B06-05|
|IR11|预登记合法排列负控误晋级率；PIT可学正控必须走实际owner|B07-05|
|IR12|同角色/同日历/同位置配对基线，明确app实际6.9/3净收益口径|B07-05 / B09-05|
|IR13|源码生成身份排除自引用，累计四文件版本与R967/R82逐批重验|B00-03/04|
|IR14|有限活动池/预算/归档，backlog满暂停新候选，保留原件闭包|B05 / B06-03 / B07|
|IR15|工程、APK、真机、自然四类状态，B09自然等待不阻断后续接线|BATCHES / ACCEPTANCE_PROTOCOL|
|QA-APP-ID|按release_identity使用真实applicationId，Java包名负例拒绝|check_acceptance / GUARD_VERIFICATION|
|QA-BATCH-MATERIAL|后态applies_in_batch及每批material限制，B06完整8修改owner登记|FILE_CARDS / WORK_ORDERS|
|QA-SCHEMA|纯核心入B01-03/05，schema保护协议接线明确待实施|CODE_PAYLOAD / supplement/INTEGRATION|
|QA-COLD-LIFECYCLE|原SQL复现源与child被清理后receipt残留；补引用保护/后继恢复/typed进度|payload/learning / evidence/b06_cold_source_prune_repro.json|
|QA-PAIR|ExpertExperimentR26.Pair/LiveState半构造与decode异常释放明确归B06-01，增加资源负例，未实现不启用birth|B06-01 / FILE_CARDS|
|QA-SCOPE|任务书用于未来明确执行指令，本轮不自增APK施工发布授权|CODEX_EXECUTE|

尚待实施：130条方法合同、既有调用者Android集成、旧66行为runner/JUnit、完整原32门/96断言/288场景的当前后态复核、APK与设备/自然观察。不得把本表写成“所有应用问题已解决”。
