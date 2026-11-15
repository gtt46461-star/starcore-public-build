# P2.05 原子、矩阵、实质变异与融合

## 根因与现场证据

20种矩阵均无保留原件，T06/T10有空点阻断，T15缺消费。结构尝试10、行为新颖0。

## 本批修改与事务边界

原子登记输入列、lag、位置、量纲、可见时间、支持数及失效场景。矩阵按kind独立物化，带shape/axis/role/target/ABI/inputHash。通过已有typed消费者引用，记录不同输入实际改变的权重/掩码/候选。组合两个合法组件，同目标父代才可做融合。

子代须有真实变换、双亲（如适用）、子程序、行为签名和独立训练证据；变异/融合/参数微调分别记；缺学生矩阵不阻塞可用教师或其他kind。

## 本批验收与故障反例

逐种矩阵实施原件读回，错轴/错期/错role/缺教师负例；移除原子、矩阵、一个父亲观察输出差与配对损失；不能把存储/可视化算成算法使用。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomWorldR27.java](dfe1ad30ba69.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomWorldR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/DerivedAtomR27.java](f7cd1280af86.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/DerivedAtomR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryR30.java](006435bfc0b3.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryR30.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java](eab252bb49a7.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedComponentStudyR30R1.java](f2bd35c4f5b3.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedComponentStudyR30R1.java)
- [app/src/main/java/com/ethk3/novaonepro/MatrixSnapshotFeedbackR930.java](4b17daeee93c.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/MatrixSnapshotFeedbackR930.java)
- [app/src/main/java/com/ethk3/novaonepro/ResearchEvolutionStoreR930.java](b5784de29471.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/ResearchEvolutionStoreR930.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/StructuralProposalR26.java](8f1f9241e0bd.md)：REPLACE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/StructuralProposalR26.java)
- [app/src/main/java/com/ethk3/novaonepro/DbStore.java](e30e335e70ea.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/DbStore.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionOpsR27.java](c04618568214.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionOpsR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java](5a1e670fd53b.md)：REPLACE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java)
