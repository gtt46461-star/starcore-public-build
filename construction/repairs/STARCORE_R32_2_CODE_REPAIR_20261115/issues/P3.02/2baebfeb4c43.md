# app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java

问题归属：P3.02；唯一文件写入批次：P3

源码动作：PRESERVE_REFERENCE

基线SHA-256：5fa2632ba02b4008420fab98d2ea539ffa7cffbf2b2bd330f261c96b05555c2b

后态SHA-256：5fa2632ba02b4008420fab98d2ea539ffa7cffbf2b2bd330f261c96b05555c2b

[完整可查看后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java)

[本批精确补丁](../../patches/P3.patch)

## 当前问题
真实032的独立未来、当前证书及REAL均为0。源码另有并发候选遗漏风险：影子R27会先于已证实SU2返回，且旧理由解析会将SU2带到通用证书表。两份现场报告无法证明当时遇到双候选。

## 本文件历史要求与修改合同

## 方法锚点

[
  {
    "path": "app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java",
    "owner": "P3",
    "method": "prepare",
    "jvmOwner": "com/ethk3/novaonepro/CertifiedPredictionR974",
    "descriptor": "(Lcom/ethk3/novaonepro/DbStore;Lcom/ethk3/novaonepro/V51Models$PredictionContext;[Lcom/ethk3/novaonepro/V51Models$PositionAction;Lcom/ethk3/novaonepro/AnalysisSingleFlightR36$Request;)Lcom/ethk3/novaonepro/CertifiedPredictionR974$Prepared;",
    "line": 240,
    "kind": "java"
  },
  {
    "path": "app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java",
    "owner": "P3",
    "method": "beforeInsert",
    "jvmOwner": "com/ethk3/novaonepro/CertifiedPredictionR974",
    "descriptor": "(Lcom/ethk3/novaonepro/DbStore;Landroid/database/sqlite/SQLiteDatabase;Lcom/ethk3/novaonepro/V51CompiledActionContract;)V",
    "line": 360,
    "kind": "java"
  }
]

## 本批验证

同目标并发给出影子R27及有证SU2，在用户授权的测试库内经正常冻结/结算恰一次；交换顺序、R27已认证、候选ID错、证书撤销、未授权、模型缺失、重复回调、截止后再次选择均验证原始行和拒绝原因。

## 代码状态

EXISTING_SOURCE_DELIVERED_NOT_NEW_IMPLEMENTATION

保全文件交付完整源码，不能因为文件随包提供便将尚未取得的业务证据签收。
