# app/src/main/java/com/ethk3/novaonepro/strategyv2/TrainingStepStoreR26.java

问题归属：P2.02；唯一文件写入批次：P2

源码动作：PRESERVE_REFERENCE

基线SHA-256：d24c0da1a68bd37b075679951dd4b9feb07b1217733c1e86e9abefbec6c3dfe1

后态SHA-256：d24c0da1a68bd37b075679951dd4b9feb07b1217733c1e86e9abefbec6c3dfe1

[完整可查看后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/TrainingStepStoreR26.java)

[本批精确补丁](../../patches/P2.patch)

## 当前问题
有历史3453个梯度记录、714个统计记录；AtomNet材料2640→3150，完整训练/已发布权重/在线消费仍0。反馈增长也存在 NOT_MODEL_TRAINING。

## 本文件历史要求与修改合同

## 方法锚点

[
  {
    "path": "app/src/main/java/com/ethk3/novaonepro/strategyv2/TrainingStepStoreR26.java",
    "owner": "P2",
    "method": "capture",
    "jvmOwner": "com/ethk3/novaonepro/strategyv2/TrainingStepStoreR26",
    "descriptor": "(Ljava/lang/String;Lcom/ethk3/novaonepro/strategyv2/ModelLiveSU2$Hub;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JJZLjava/lang/String;Ljava/lang/String;Ljava/util/Map;)Ljava/util/List;",
    "line": 23,
    "kind": "java"
  },
  {
    "path": "app/src/main/java/com/ethk3/novaonepro/strategyv2/TrainingStepStoreR26.java",
    "owner": "P2",
    "method": "record",
    "jvmOwner": "com/ethk3/novaonepro/strategyv2/TrainingStepStoreR26",
    "descriptor": "(Landroid/database/sqlite/SQLiteDatabase;Ljava/util/List;Ljava/lang/String;)V",
    "line": 30,
    "kind": "java"
  }
]

## 本批验证

固定样本重启续训数值等价，乱序、标签尚未知、缺位置7、zero-gradient、同模型旧缓存都不得冒领“已发布已消费”。再用真实事件检查滞后。

## 代码状态

EXISTING_SOURCE_DELIVERED_NOT_NEW_IMPLEMENTATION

保全文件交付完整源码，不能因为文件随包提供便将尚未取得的业务证据签收。
