# app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java

问题归属：P4.01；唯一文件写入批次：P4

源码动作：PRESERVE_REFERENCE

基线SHA-256：90624e78b67571c73784281a80b8330f74761da0bc5d30522d74ec6e9f529150

后态SHA-256：90624e78b67571c73784281a80b8330f74761da0bc5d30522d74ec6e9f529150

[完整可查看后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java)

[本批精确补丁](../../patches/P4.patch)

## 当前问题
原96项已执行且44指标在册，但16条诊断规则8条证据不足、1条未知；旧UPGRADE文件存在空处方，用户无法知道该改谁。

## 本文件历史要求与修改合同

```json
{
  "old_card": "R31-B11-C02",
  "action": "修改",
  "before": "旧诊断可能引用旧sequence/探针，与新链实际执行不同",
  "after": "显示诊断年龄/source ABI/owner；同source兼容才合并，不把旧探针0解释为全软件无学习",
  "anchor": [
    {
      "old_request": "snapshot",
      "method": "snapshot",
      "line": "158",
      "descriptor": "(Landroid/database/sqlite/SQLiteDatabase;JLcom/ethk3/novaonepro/strategyv2/InformationProbeSU2;Lcom/ethk3/novaonepro/strategyv2/ModelEvolutionSU2$State;)Ljava/lang/String;"
    },
    {
      "old_request": "report",
      "method": "report",
      "line": "81",
      "descriptor": "(Landroid/database/sqlite/SQLiteDatabase;)[Ljava/lang/String;"
    }
  ]
}
```

## 方法锚点

[
  {
    "path": "app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java",
    "owner": "P4",
    "method": "report",
    "jvmOwner": "com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2",
    "descriptor": "(Landroid/database/sqlite/SQLiteDatabase;)[Ljava/lang/String;",
    "line": 81,
    "kind": "java"
  }
]

## 本批验证

用032两份快照恢复或授权脱敏fixture，检查每组ID、同一watermark与数值JSON/MD相同；F23、T06/10/15、AtomNet、无REAL均有具体处方，非空套话判失败。

## 代码状态

EXISTING_SOURCE_DELIVERED_NOT_NEW_IMPLEMENTATION

保全文件交付完整源码，不能因为文件随包提供便将尚未取得的业务证据签收。
