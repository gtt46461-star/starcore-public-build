# P2.06 训练筛劣、淘汰与代际证据

## 根因与现场证据

50次尝试中行为新颖/接受/改进验证均0。现有最高历史胜率不足以判断优劣。

## 本批修改与事务边界

在 FoundryCampaignR26.finish 保留 DEV、SELECT 两段，用两段描述性 Wilson 下界的较小值排历史父代，平分看较小支持与不可变策略键；排除受控消融对象，至多保留三个待选排名对象。保留另外两份独立账记录长期同机会配对与未来资格。CreationBudgetR31 只控制同时活跃资源。对低效候选停止排程、撤出亲本池，证据保全后回收预算。

保全父子摘要、失败原因和冻结祖先；退役是关闭后续训练/选择，物理删除仅限已证实无引用的可再生负载。代号来自谱系增量；只有相同冻结测试上的可信改进可记强化。

## 本批验收与故障反例

1/1与40/100、跨段崩溃、平分、缺样本均可重放；负控不能当亲本。相同预算测亲本选中的净效应、宽度、计算成本、最差窗口、代际增量。已知弱者退休过程中杀进程，历史证据仍可回读；释放的可用槽能创建具有独立样本账的下一代。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/strategyv2/CreationBudgetR31.java](7cb3a2bc70d2.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/CreationBudgetR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/EliteArchiveR27.java](90ed3a1cb653.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/EliteArchiveR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java](e9148d72ecd6.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelEvolutionSU2.java](84c17484a60b.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelEvolutionSU2.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ImprovementStoreR31.java](b82001124aab.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ImprovementStoreR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/FactoryCapabilityExperimentR31.java](84e1c618eef2.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/FactoryCapabilityExperimentR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java](647bbbbf0a29.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcStoreR31.java](147625fe7247.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcStoreR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java](64624463364e.md)：REPLACE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java)
