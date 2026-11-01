# 验证范围与复跑

保留本轮实际日志与测试源代码。PASS只作用于日志所述范围；没有Android编译/真机/自然质量通过结论。

- ResearchCoreTest：76项角色、统计、合同、单飞提交及图规格检查。
- WriterRotateTest：真实WriterHoldLedger并发160000事件，未丢计数。
- HoldFlushRetryTest：真实Flush逻辑+明确的假DB适配器；首次失败后12旧+3新=15。不是Android SQLite集成证明。
- DiagnosisRenderRegression：空探针崩溃修复；16旧MD/JSON字节一致。adapters只用于未涉路径链接，相关Committee方法抛UnsupportedOperationException。
- schema_test/sql_binding_test：真实DDL在SQLite幂等/约束/回滚，6条实际writer SQL的参数与列数检查。
- patch_apply_check/parse：指定基线补丁可顺序应用、1214 Java语法0错误。

复跑须先准备指定git基线及原tools/strategy_su2/fixtures/r16_diag，不从网页摘要重建fixture。JDK17可用`java com.sun.tools.javac.Main`编译纯Java核心与测试；测试包名见源码。其他测试使用各自适配器，不把它们复制进app。Android构建仍须原Gradle/SDK36/NDK/签名/设备环境。
