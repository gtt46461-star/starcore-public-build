# app/src/main/java/com/ethk3/novaonepro/strategyv2/BirthGateR27.java

问题归属：P2.04；唯一文件写入批次：P2

源码动作：PRESERVE_REFERENCE

基线SHA-256：c334cb9fbb19fcc89dc8d6ade1ee00060b89c0503f3af1fc93b77a37d7f081ca

后态SHA-256：c334cb9fbb19fcc89dc8d6ade1ee00060b89c0503f3af1fc93b77a37d7f081ca

[完整可查看后态代码](../../code/P2/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/BirthGateR27.java)

[本批精确补丁](../../patches/P2.patch)

## 当前问题
4个旧SU1策略、24个旧专家提案不能说明现版产生了新品。82能力中F23教师缺绑定。

## 本文件历史要求与修改合同

```json
{
  "old_card": "R31-B07-C02",
  "action": "PRESERVE_AND_MODIFY",
  "before": "depth<=6、nodes<=40、constants<=12 等出生门及行为签名已有实现。",
  "after": "导入硬上限建议 64KiB/64层/4096节点；正式出生继续现有 6/40/12，不用导入上限放宽出生；行为签名键加 probe 输入、角色、位置与 selector/update ABI。",
  "anchor": [
    {
      "old_request": "candidateContract",
      "method": "candidateContract",
      "line": "47",
      "descriptor": "(Lcom/ethk3/novaonepro/strategyv2/PolicySpecR27;)Lcom/ethk3/novaonepro/strategyv2/BirthGateR27$Verdict;"
    },
    {
      "old_request": "check",
      "method": "check",
      "line": "64",
      "descriptor": "(Lcom/ethk3/novaonepro/strategyv2/PolicySpecR27;)Lcom/ethk3/novaonepro/strategyv2/BirthGateR27$Verdict;"
    },
    {
      "old_request": "duplicateOf",
      "method": "duplicateOf",
      "line": "109",
      "descriptor": "([I)Ljava/lang/String;"
    },
    {
      "old_request": "duplicateOf",
      "method": "duplicateOf",
      "line": "112",
      "descriptor": "([II)Ljava/lang/String;"
    },
    {
      "old_request": "register",
      "method": "register",
      "line": "118",
      "descriptor": "(Ljava/lang/String;[I)V"
    },
    {
      "old_request": "register",
      "method": "register",
      "line": "119",
      "descriptor": "(Ljava/lang/String;[II)V"
    }
  ]
}
```

```json
{
  "old_card": "UF14",
  "diagnosis": [
    "当前candidates固定4程序，PRODUCTION_STAGES没有驱动实际family/expert/proposal/进化编排，不能证明完整工厂能力。"
  ],
  "implementation": [
    "保留四程序测试为内核测试；另建驱动真实工厂的可复现adapter，使用隔离SYNTHETIC命名空间、预算、seed与真实搜索路径。",
    "记录所有候选/淘汰/失败、搜索暴露、训练—选择—确认—资格门及成本；失败试验不能标COMPLETE。",
    "保留所有有效家族、typed AST、变异/融合/淘汰和父子比较；历史快速训练筛劣继续可用。",
    "家族kernel、Foundry遍历、AST出生门、CreationBudget和FrozenConfirmation已有生产实现，先核对新接口与这些实现的等价合同；FamilyRegistry新接口或VerifiedImprovement helper未调用既不能算新合同完成，也不能误判原创造能力全不存在；以同机会父子、冻结确认、未来证据的生产回执关门。"
  ]
}
```

## 方法锚点

[
  {
    "path": "app/src/main/java/com/ethk3/novaonepro/strategyv2/BirthGateR27.java",
    "owner": "P2",
    "method": "check",
    "jvmOwner": "com/ethk3/novaonepro/strategyv2/BirthGateR27",
    "descriptor": "(Lcom/ethk3/novaonepro/strategyv2/PolicySpecR27;)Lcom/ethk3/novaonepro/strategyv2/BirthGateR27$Verdict;",
    "line": 64,
    "kind": "java"
  }
]

## 本批验证

输入常量、洗牌、含弱结构三组；固定预算测发现率与假阳性；打印每个族输入、操作、artifact及淘汰原因；拒绝读取当前标签或使用未来窗口。

## 代码状态

EXISTING_SOURCE_DELIVERED_NOT_NEW_IMPLEMENTATION

保全文件交付完整源码，不能因为文件随包提供便将尚未取得的业务证据签收。
