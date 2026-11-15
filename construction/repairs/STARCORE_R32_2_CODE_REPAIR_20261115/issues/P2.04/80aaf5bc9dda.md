# app/src/main/java/com/ethk3/novaonepro/strategyv2/SetIntersectionPolicyR30.java

问题归属：P2.04；唯一文件写入批次：P2

源码动作：PRESERVE_REFERENCE

基线SHA-256：22123d4d15e6ebdcdc5f57f376e065a6ace9416b7345ee57aee73d44dfa6878d

后态SHA-256：22123d4d15e6ebdcdc5f57f376e065a6ace9416b7345ee57aee73d44dfa6878d

[完整可查看后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/SetIntersectionPolicyR30.java)

[本批精确补丁](../../patches/P2.patch)

## 当前问题
4个旧SU1策略、24个旧专家提案不能说明现版产生了新品。82能力中F23教师缺绑定。

## 本文件历史要求与修改合同

## 方法锚点

[
  {
    "path": "app/src/main/java/com/ethk3/novaonepro/strategyv2/SetIntersectionPolicyR30.java",
    "owner": "P2",
    "method": "support",
    "jvmOwner": "com/ethk3/novaonepro/strategyv2/SetIntersectionPolicyR30",
    "descriptor": "(Lcom/ethk3/novaonepro/strategyv2/FeatureCacheR27;IIII)Ljava/util/Map;",
    "line": 30,
    "kind": "java"
  }
]

## 本批验证

输入常量、洗牌、含弱结构三组；固定预算测发现率与假阳性；打印每个族输入、操作、artifact及淘汰原因；拒绝读取当前标签或使用未来窗口。

## 代码状态

EXISTING_SOURCE_DELIVERED_NOT_NEW_IMPLEMENTATION

保全文件交付完整源码，不能因为文件随包提供便将尚未取得的业务证据签收。
