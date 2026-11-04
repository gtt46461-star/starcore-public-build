本次只增加独立 HOST 验证工具和证据，未修改任何生产源码，未重跑既有大数学套件。

使用 root 冻结的 013 实际 Android 构建 classes.jar：SHA256 8f863b5f6e40500640c1b65cea4c78594d5405ae7021853bafbf11525d71ff13。生产 PolicyAst/FeatureCache/ReplayEvaluator/MemoryBudget 均直接从该 jar 加载，没有重新编译或覆盖生产类。main PolicyAst 源码只读快照 SHA448de8c754fc22f10b6f4540f042ae43186e8040e6e25a0c09f7ad156442da56；javap 证实真实 family 分支传入当前 critical 到 familyIntoR31(II[DZ)V。

接口链实际运行：64 行合法排列输入 → ReplayEvaluator.predictNext → PolicyAst.evalRealtimeScopedR31 / evalScopedR31 → 四参数 familyIntoR31 → 冷模型/矩阵/行检查点准入 → 实际预测输出 → scope finally 恢复。输入是工程合成夹具，不写 SHADOW/FUTURE/REAL 账本，不作为自然质量证据。

在真实 512MiB HOST JVM 强持已登记的343597383字节数组，触及实际页面；未替换/包装 RuntimeProbe，未请求 GC，未人工改变水位。实测 Java heap346773912/536870912B，nativeAllocated=-1未知，PSS382148608B和RSS395001856B独立采样，不能相加作为堆。两块真实预先获得并已退出的数值 scratch 使历史请求越过 scratch，实际到达 MODEL 构造准入才被 DECLARED_CAPACITY 拒绝。

125 断言通过：

- 冷历史先拒绝，实际 public live 消费成功；model184320B、matrix51328B、row checkpoint90240B声明均为 REALTIME。
- 正常返回、真实 token 在模型 update 内抛 Yield、该异常后的真实 history、重试完成、并发 live 前后，历史均未继承 critical。
- 线程 A 实际 live 模型 token 内的 critical=true，并捕获真实调用栈；同时线程 B 的 history 没有 critical，且真实到 MODEL 准入被拒绝。
- 异常未发布部分矩阵，实际模型 body 已退出；实际 public 预测重试与健康输出逐位一致。
- 实际数值 scope 和 executor 退出后才关闭所属缓存，REALTIME 声明归零。关闭声明不声称实际堆已回收；后续堆值仍以测量为准。

首轮 harness 错将 ReplayEvaluator 原生0..9 K3索引当作UI1..10值，已按该 API 现有 top3/Trace 合同纠正，保留 initial-harness-k3-index.stderr.log。未更改生产K=3或用户显示口径。

复现：

    PATH=/path/to/jdk17/bin:$PATH python3 tools/r31/verify_ast_family_scope.py --support-jar /path/to/frozen013/classes.jar

结论仅为实际 Android 编译类在 HOST JVM 上的消费者机制验证。Android设备、24/72小时、原生内存校准、全链正式出手与自然未来质量均未在本测试运行。
