# P2.02 事件驱动的历史训练与 AtomNet 十位置

## 根因与现场证据

有历史3453个梯度记录、714个统计记录；AtomNet材料2640→3150，完整训练/已发布权重/在线消费仍0。反馈增长也存在 NOT_MODEL_TRAINING。

## 本批修改与事务边界

合法开奖每期入唯一事件，分模型、位置保存可恢复水位；先用旧参数预测，标签到达后再更新，记录 loss、样本数、optimizer、weightBefore/After；十位置一致的 epoch 发布一次不可变模型。让真实预测消费者记录已读权重ID与输出摘要。

共享上游的样本ID只算一次；同标签不能训练两次；父进程关闭或保护预算等待均保留水位。残差与数值梯度分账，热历史回放不得记自然未来。

## 本批验收与故障反例

固定样本重启续训数值等价，乱序、标签尚未知、缺位置7、zero-gradient、同模型旧缓存都不得冒领“已发布已消费”。再用真实事件检查滞后。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java](5a1e670fd53b.md)：REPLACE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java](42abddbfa149.md)：REPLACE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetPublicationR31.java](ecd0bd89d4d6.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetPublicationR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java](6b36757d2e0c.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetTrainerR27.java](ba89347343a9.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetTrainerR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/TrainingStepStoreR26.java](f497bdb3e4ff.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/TrainingStepStoreR26.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceStoreR30.java](0b13925ed482.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceStoreR30.java)
- [app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java](161e91d563f4.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java)
- [app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java](2ee925948844.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java)
- [app/src/test/java/com/ethk3/novaonepro/strategyv2/R32PublishedModelDuringTrainingTest.java](901f34450d9c.md)：ADD；[完整后态代码](../../code/P2/files/app/src/test/java/com/ethk3/novaonepro/strategyv2/R32PublishedModelDuringTrainingTest.java)
