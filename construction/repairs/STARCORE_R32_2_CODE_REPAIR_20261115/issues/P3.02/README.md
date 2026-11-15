# P3.02 真实主链消费、每期唯一和撤销

## 根因与现场证据

真实032的独立未来、当前证书及REAL均为0。源码另有并发候选遗漏风险：影子R27会先于已证实SU2返回，且旧理由解析会将SU2带到通用证书表。两份现场报告无法证明当时遇到双候选。

## 本批修改与事务边界

同批修 MainlineBridgeSU2.intent 与 UnifiedResearchChoiceR932 的选择/原因读取；保留前置 R958、SU1 及正式 R27 的原优先级。已有不可变目标选择先复用；首次选择时同策略、同证书、有效冻结、模型存在、选择器确认且ACT的SU2才可越过影子R27。走现有 DecisionGateEngine 的最终授权，原账唯一结算。读取R27、SU1、SU2各自权威资格来源并记录用户真实开关状态。

单位置固定K=3，每期最多一条REAL；证书未达时写明HOLD并保留影子。约15%频率是目标，不能补单。撤销竞态不得改写已冻结历史，SHADOW不能计真实胜率。

## 本批验收与故障反例

同目标并发给出影子R27及有证SU2，在用户授权的测试库内经正常冻结/结算恰一次；交换顺序、R27已认证、候选ID错、证书撤销、未授权、模型缺失、重复回调、截止后再次选择均验证原始行和拒绝原因。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterR31.java](ad1f7a48015e.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java](2019a2fb0dfe.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineFreezeProofR31.java](b1175200be6a.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineFreezeProofR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineModelBindingR31.java](0e1c70d9982e.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineModelBindingR31.java)
- [app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java](039e1b580a46.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java](ce581b79d84f.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java)
- [app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java](ddca867c7159.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java)
- [app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java](cf4b228e9f3e.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java)
- [app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java](b4bed32202f2.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceStoreR30.java](0b13925ed482.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceStoreR30.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineBridgeSU2.java](d9485d127956.md)：REPLACE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineBridgeSU2.java)
- [app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java](b5c8f2507c27.md)：REPLACE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java)
- [app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java](2baebfeb4c43.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java)
- [tools/r31/run_typed_mainline_host.py](92426b4a87bb.md)：REPLACE；[完整后态代码](../../code/P3/files/tools/r31/run_typed_mainline_host.py)
- [tools/r32/run_mainline_host.py](f5e6464083ec.md)：ADD；[完整后态代码](../../code/P3/files/tools/r32/run_mainline_host.py)
- [app/src/main/java/com/ethk3/novaonepro/DbStore.java](e30e335e70ea.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/DbStore.java)
- [app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java](cf4a0da2b9eb.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java)
