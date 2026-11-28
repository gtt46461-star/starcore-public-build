# 4组公开候选补丁

以精确211为前像，C01/C02/C03/C05共4组，改动4个生产文件和1个既有测试文件。均未应用到产品源码。前后hash见PATCHSET.json。

|候选|工单/批次|修复|已验证范围|
|---|---|---|---|
|C01|006 / B01|保留业务scope完整结构字符串|公开源码前像+5种纯合成字符串通过；未读用户报告|
|C02|003 / B01|scope作外层的等价SQL|5万事件、100组筛选等价，主机EXPLAIN|
|C03|015 / B03|精确已回滚ADMISSION故障背压的类别兼容|30例Java实际方法抽取，原版1个预期失败、候选0失败|
|C05|040 / B08|R18精确已结算/待结算/未知/缺证分开显示|12例纯Java投影验证|

C04的第五条RULE恢复需用户内部精确五元组，只在用户完整包提供，未公开其补丁、原件身份或相关测试后态。不能因此把白名单泛化。

```bash
python3 "$PLAN_DIR/tools/check_candidates.py" --source-root "$APP_DIR"
python3 "$PLAN_DIR/candidates/runtime/check_coverage_join.py"
python3 "$PLAN_DIR/candidates/runtime/check_scope_identity.py" --repo "$APP_DIR" --patch "$PLAN_DIR/candidates/runtime/heartbeat_scope_identity.patch"
java "$PLAN_DIR/candidates/strategy/WriterBusySourceContract.java"
java "$PLAN_DIR/candidates/charts/R18ProjectionContractHarness.java"
```

首条命令仅在临时副本顺序应用并核4组前后SHA，不改APP_DIR。Java仅抽取方法夹具，没有运行Android、生产SQLite、真实准入调度或设备；SQL主机等价不证明现场性能。完整Android/JUnit/真机验收均待施工。
