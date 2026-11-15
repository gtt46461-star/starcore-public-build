# P2.01 恢复回放知识并解除下游等待

## 根因与现场证据

r27-replay-dataset 两个时点都为3步/3597847，一次END回滚后一次ADMISSION回滚；evolution停4步/13。另有源码风险，恢复缓存绑定可能在 evoData 已发布后抛出。

## 本批修改与事务边界

P2.patch 在私有缓存绑定历史模型之后才提交知识、再发布已就绪缓存；失败释放私有缓存。逐层保留 retryable admission / END rollback / committed ACK lost 的事务证据。只用当前 pin+revision+ABI 读回恢复，同一已提交收据不制造新训练步。

prepare 在锁外，事务内短 CAS 核对 pin、owner/fence、撤销、generation。会话重启先还原同一pin，缺 knownAt 显式限制历史角色。

## 本批验收与故障反例

用持久提交的 pin 冷启动，依次注入准入、END、绑定、ACK故障；一旦writer恢复，下游能拿到同一缓存并真的计算。分别比对optimizer/游标/状态哈希。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java](5a1e670fd53b.md)：REPLACE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayKnowledgeStoreR31.java](762522b33ce0.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayKnowledgeStoreR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java](68ba38dec65d.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java](957327df7c80.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java](44cbc7497760.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java)
- [app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java](2ee925948844.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java](ca247b4e9e75.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java)
