# P4.02 16张研究表、47个面板和消费者

## 根因与现场证据

18张研究图缺当前源帧/消费，29旧图设备有效性未知。16表只有8表出行，部分行为阻断占位。

## 本批修改与事务边界

按 ledger 的源SQL、生产者、消费者及逐项情形恢复生产帧和版本关联。研究页面通过业务只读投影展示同目标父子收益、命中区间、校准、机会频率、漂移和失败原因。查明每个表是否纯展示或被算法读取；真实消费者在P2既有调用点留收据。

图表缓存不能决定主链或成为算法真值；缺点位与零值分开。慢查询限时分页，用户离开取消；新旧版本不得混画。

## 本批验收与故障反例

每个ID单独用已知行/空/错版本/分页/取消测试；8张有行逐表回读，T06/10空点不给成功。对18研究图检查同源消费ID和移除输入效果。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java](a24077c1911d.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java)
- [app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR83.java](4f38078b5467.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR83.java)
- [app/src/main/java/com/ethk3/novaonepro/ChartRegistryR31.java](fd3c65892a86.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ChartRegistryR31.java)
- [app/src/main/java/com/ethk3/novaonepro/ResearchTablesActivityR31.java](3bec19fc1a98.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ResearchTablesActivityR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryStoreR30.java](1a3c9add0428.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryStoreR30.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java](eab252bb49a7.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedComponentStudyR30R1.java](f2bd35c4f5b3.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedComponentStudyR30R1.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java](e9148d72ecd6.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAdaptersR30R1.java](2127cef4b473.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAdaptersR30R1.java)
