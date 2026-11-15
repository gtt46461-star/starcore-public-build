# tools/r31/upgrade_matrix.py

问题归属：P5.01；唯一文件写入批次：P5

源码动作：PRESERVE_REFERENCE

基线SHA-256：4809a67602bc1a48dc7474db6c15b84ea4a9dadd443f45e9d4f47bf135715e50

后态SHA-256：4809a67602bc1a48dc7474db6c15b84ea4a9dadd443f45e9d4f47bf135715e50

[完整可查看后态代码](../../code/P5/files/tools/r31/upgrade_matrix.py)

[本批精确补丁](../../patches/P5.patch)

## 当前问题
032对应源工程已有653单测通过；并没有由此得到24/72小时合格、真机质量或闭环运行证据。

## 本文件历史要求与修改合同

```json
{
  "old_card": "R31-B12-C01",
  "action": "新增",
  "before": "缺本次升级矩阵回执。",
  "after": "对空库、当前约600MB库、压力库、部分迁移中断库逐一执行安装/恢复/验证并导出机器收据。",
  "anchor": [
    {
      "old_request": "run_upgrade_matrix",
      "binding": "旧方案表达；不作032已存在方法证明，使用当前文件方法索引及正常入口定位"
    },
    {
      "old_request": "verify_recovery",
      "binding": "旧方案表达；不作032已存在方法证明，使用当前文件方法索引及正常入口定位"
    }
  ]
}
```

## 方法锚点

[]

## 本批验证

验证当前SHA的Gradle、关键host/Android路径、apksigner、安装升级、ZIP读回。24h/72h实测与自然样本单列；条件未到不冒领。 加跑 verify/check_upgrade_rows.py：先固定032的32个开奖和32个已结算冻结，再对照候选导出与真机装前装后字节收据，另对模型/证书/CAS原件跨升级分别重放。

## 代码状态

EXISTING_SOURCE_DELIVERED_NOT_NEW_IMPLEMENTATION

保全文件交付完整源码，不能因为文件随包提供便将尚未取得的业务证据签收。
