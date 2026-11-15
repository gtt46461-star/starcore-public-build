# P2.03 旧候选保全、下一轮出生与无总量上限

## 根因与现场证据

旧50个候选指纹、游标和验证数不变；均是 BLOCKED_WARM_NUMERICAL_ABI_PRESERVED。没有本区间新策略出生凭据。

## 本批修改与事务边界

用原 warm 归档和明确的数值兼容声明保留旧对象；合法 warm fork 需要更新数据和独立新ID，不兼容保留原件、以当前ABI冷启合法种子。P2.patch 将父代长期固定的前几个供体改为同快照可重复、跨期轮换的异机制供体；新子代注册 sourceHash/parent/结构/算子/代号。

无限的是生命周期累计可创造的策略数；活跃槽、单代算力、单次写入和证据容量沿用预算；没有额度时进入可恢复队列，清退后继续出生。重跑同一输入幂等。模型模板及改名不计新策略。

## 本批验收与故障反例

跨多个campaign，使用足够新数据；验证有可执行、可重建、去重后的出生记录和后续首个真正训练步。用同一输入重放不增数，旧50不丢，垃圾释放槽后继续多轮。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java](64624463364e.md)：REPLACE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/WarmCandidateR26.java](48796e5cc64d.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/WarmCandidateR26.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/StructuralProposalR26.java](8f1f9241e0bd.md)：REPLACE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/StructuralProposalR26.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelGenomeSU2.java](9e330ae49f04.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelGenomeSU2.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelPublicationR31.java](fa0b72627637.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelPublicationR31.java)
- [app/src/main/java/com/ethk3/novaonepro/LifecycleSchemaR31.java](c5021400063c.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/LifecycleSchemaR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/HypothesisStoreR31.java](1efc16b2a724.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/HypothesisStoreR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointArtifactsR26.java](c7773900310d.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointArtifactsR26.java)
