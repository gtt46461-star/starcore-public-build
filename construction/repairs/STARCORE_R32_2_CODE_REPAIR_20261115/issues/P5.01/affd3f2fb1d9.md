# .github/workflows/r31-current-live.yml

问题归属：P5.01；唯一文件写入批次：P5

源码动作：PRESERVE_REFERENCE

基线SHA-256：e2ce39f36fdd844454f4e19f6c7227427e496dd3a6e4b1d37115fba83119ecf1

后态SHA-256：e2ce39f36fdd844454f4e19f6c7227427e496dd3a6e4b1d37115fba83119ecf1

[完整可查看后态代码](../../code/P5/files/.github/workflows/r31-current-live.yml)

[本批精确补丁](../../patches/P5.patch)

## 当前问题
032对应源工程已有653单测通过；并没有由此得到24/72小时合格、真机质量或闭环运行证据。

## 本文件历史要求与修改合同

```json
{
  "repair_card": "P5.01",
  "implementation": "将固定025输入改为经SHA-256核验的032旧APK，将实际工件传给同一个运行器并保存装前后收据。"
}
```

## 方法锚点

[]

## 本批验证

验证当前SHA的Gradle、关键host/Android路径、apksigner、安装升级、ZIP读回。24h/72h实测与自然样本单列；条件未到不冒领。 加跑 verify/check_upgrade_rows.py：先固定032的32个开奖和32个已结算冻结，再对照候选导出与真机装前装后字节收据，另对模型/证书/CAS原件跨升级分别重放。

## 代码状态

EXISTING_SOURCE_DELIVERED_NOT_NEW_IMPLEMENTATION

保全文件交付完整源码，不能因为文件随包提供便将尚未取得的业务证据签收。
