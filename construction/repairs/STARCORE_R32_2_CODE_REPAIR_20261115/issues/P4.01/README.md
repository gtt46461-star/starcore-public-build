# P4.01 逐对象的诊断、处方和可读出口

## 根因与现场证据

原96项已执行且44指标在册，但16条诊断规则8条证据不足、1条未知；旧UPGRADE文件存在空处方，用户无法知道该改谁。

## 本批修改与事务边界

ReportR29 第12章与完整导出的 UPGRADE_DIAGNOSIS.json / .md 共同使用 RepairDiagnosisR32 的对象行和 operatingSummaryR32。以本快照 R30_01、R30_04、R30_06 三章中的实际来源字段为准；列独立未来分母、工厂阻点、AtomNet材料/训练/发布和可核验的下一步。旧 DiagnosisStoreSU2 空S4处方另附人读解释和复验条件，保留原字段机器语义；后续填充每行的真实最近运行、输入产物消费链及同机会实验。

失败、缺测、无输入、未消费、无优势分别输出，0须有查得的分母。投影只取小字段；新增对象、缺ID、重ID须明确覆盖差异；来源冲突并列显示，禁止将read_ok解释成有效。

## 本批验收与故障反例

用032两份快照恢复或授权脱敏fixture，检查每组ID、同一watermark与数值JSON/MD相同；F23、T06/10/15、AtomNet、无REAL均有具体处方，非空套话判失败。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR29.java](057a34f0c204.md)：REPLACE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR29.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR27.java](c337a6e742ce.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR27.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java](ca003fdd1902.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java](ed967035d3e6.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java)
- [app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java](34e801bcb32d.md)：REPLACE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/RepairDiagnosisR32.java](9e20fc9934b7.md)：ADD；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/RepairDiagnosisR32.java)
- [app/src/main/java/com/ethk3/novaonepro/ReportSemanticVerifierR31.java](6b9e691931a2.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ReportSemanticVerifierR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java](64624463364e.md)：REPLACE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java)
- [app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java](2ee925948844.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java](42abddbfa149.md)：REPLACE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java](2019a2fb0dfe.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java](a87b8e9bfa8a.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAuditR32.java](3cc80cd992b8.md)：ADD；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAuditR32.java)
