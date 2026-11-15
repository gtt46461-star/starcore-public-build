# P3.01 冻结的未来观察和资格

## 根因与现场证据

新旧包未来策略/有效未来出手/证书均为0。高胜率、稳定性无法从历史数推定。

## 本批修改与事务边界

分别保全 R27_GATE_V1 和 R26_PREREGISTERED_AGGREGATE_LCB_V3 的输入、机会日历、分母和证书表。R27 保留实际序贯登记，通过250笔未来出手等既有条件。SU2 从 QualificationWindowR26.completed 到 CertificateSU2.issueWithin 复核69120个机会的四段原始聚合、配对及后段校准。见修订指南的双路对照。分别核对数据完整性与同策略版本，二者不能互签。

120可用于旧观察或校准子段的既定口径；R27以未来有效出手计、SU2以机会日历及其内实际出手分别计。热库5000期外保留可验证资格聚合、证书及引用根；不借父代、重复窗口或旧ABI扩大独立样本。

## 本批验收与故障反例

两套门各测刚低于/等于/刚超过边界，以及缺标签、错ABI、撤销与错策略。完成窗口、校准与正式签发全部使用同一不可变证据。保留真实032零证书作负例；工程正例仅用独立测试账。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java](8ac156fb01f8.md)：REPLACE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/SequentialQualificationR27.java](611996d6b236.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/SequentialQualificationR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationInboxR29.java](bd986581bff5.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationInboxR29.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationGapRepairR30R1.java](b232c5d83eb2.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationGapRepairR30R1.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceContractR31.java](841595c1f5da.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceContractR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/GateV2R29.java](7ef79d0298a0.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/GateV2R29.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java](64624463364e.md)：REPLACE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelPublicationR31.java](fa0b72627637.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelPublicationR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateSU2.java](9348ff515f9e.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateSU2.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationWindowR26.java](0a31e67ce53d.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationWindowR26.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceBlockR26.java](3446c7e65060.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceBlockR26.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java](5c63e8b92f87.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java)
