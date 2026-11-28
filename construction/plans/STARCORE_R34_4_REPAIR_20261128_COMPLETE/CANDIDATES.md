# 5组精确候选补丁

这些补丁以精确211为前像，共改动5个生产Java文件和2个测试文件（其中1个测试新建）。本轮没有把它们应用到产品源码。前后SHA和所属工单见 [PATCHSET.json](PATCHSET.json)，只能在前像匹配或完成三方审阅后使用，不能整文件覆盖更新后的实现。

|候选|工单/批次|实际变化|本轮已验证|仍待验|
|---|---|---|---|---|
|C01|006 / B01|业务scope完整结构字符串，不使用300字符显示截断|报告实际截断、源码前像、5种字符串往返|Android运行与真实长身份写入/读取|
|C02|003 / B01|scope作外层的等价覆盖SQL|5万事件、100组筛选等价；主机EXPLAIN索引顺序|实际5GB数据库计划、耗时与取消|
|C03|015 / B03|精确故障背压的RESOURCE_QUEUE/DEPENDENCY已回滚准入分类|源码抽取Java30例：旧版1个预期失败，候选0失败|完整JUnit、SQLite提交/未知回执、调度公平与真机|
|C04|030 / B06|第五个RULE原task/candidate/protocol/input/action精确身份|前后hash、累计补丁可用、原报告身份来源审阅|新增JUnit尚未执行；原.scdev/分片/生命周期/幂等恢复|
|C05|040 / B08|R18精确已结算、待结算、未知身份、无证分别投影|实际投影方法抽取Java12例|Android图表及真实授权/结算原件联查|

## 重现已执行验证

```bash
python3 "$PLAN_DIR/tools/check_candidates.py" --source-root "$APP_DIR"
python3 "$PLAN_DIR/candidates/runtime/check_coverage_join.py"
python3 "$PLAN_DIR/candidates/runtime/check_scope_identity.py" --repo "$APP_DIR" --report "$REPORT_DIR" --patch "$PLAN_DIR/candidates/runtime/heartbeat_scope_identity.patch"
python3 "$PLAN_DIR/candidates/runtime/check_settlement_evidence.py" --report "$REPORT_DIR"
java "$PLAN_DIR/candidates/strategy/WriterBusySourceContract.java"
java "$PLAN_DIR/candidates/charts/R18ProjectionContractHarness.java"
```

Java使用支持源码启动的环境。本轮Java运行的是从真实候选方法抽出的主机夹具；writer分类依赖typed category adapter，没有执行生产fallback classifier、Android、真实SQLite或设备。R18夹具验证显示投影，不授予REAL资格，不代替上游exact SQL核验。所有运行记录保存在validation；原代理的更早结果保留在candidates供追溯。

本轮独立对163个活跃结算任务的JOIN是报告元数据复核，未修改任务/数据库，不能证明已完成恢复。
