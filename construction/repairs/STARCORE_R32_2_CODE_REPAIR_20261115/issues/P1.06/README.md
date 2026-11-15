# P1.06 存储和缺失 checkpoint 可恢复

## 根因与现场证据

后包仍缺556个历史对象，另有未核验项；物理体积约2.96GB，含受保护备份。恢复读取不到文件会影响训练或导出。

## 本批修改与事务边界

根清单包含当前模型、祖先、候选、证书、待结算冻结、出报告的读租约；逐引用 verify SHA/大小、来源恢复，分离 ABSENT/CORRUPT/UNVERIFIED、标识受影响的策略。事务外枚举/复制/校验，事务内代数 CAS 后移除无引用副本，清单、WAL、缓存和外部备份各预算。

仅清除可再生数据或已证实同内容的无引用副本。关键原件缺且无备份时保留债务，不生成空确认。5000期热窗口与受保护档案不同。

## 本批验收与故障反例

准备→替换→确认任一阶段中断可恢复；旧URI无权限、磁盘满、空引用图、大对象均明确状态。出包实测磁盘和堆/PSS各自增长曲线。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/StorageBudgetR31.java](de3a973ee814.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/StorageBudgetR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcStoreR31.java](147625fe7247.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcStoreR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointArtifactsR26.java](c7773900310d.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointArtifactsR26.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/KeepMigrationStoreR31.java](4f034b28e98d.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/KeepMigrationStoreR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java](647bbbbf0a29.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java)
- [app/src/main/java/com/ethk3/novaonepro/ReportCleanupR31.java](64e512a4fcf0.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/ReportCleanupR31.java)
- [app/src/main/java/com/ethk3/novaonepro/StorageGcR31.java](5b4eeda68428.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/StorageGcR31.java)
