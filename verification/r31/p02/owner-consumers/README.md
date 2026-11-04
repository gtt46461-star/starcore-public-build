本模块在隔离 worktree `/workspace/r31-owner-consumers`、分支 `construction/r31-owner-consumers-20261104` 施工，基线为 `3246ccf8eb6f7b2f7b9d0fe77fc18a70ab958056`。只改一个生产文件和五个测试文件，没有修改主工作树、其他代理源码、版本、身份、schema、账本或构建配置，没有运行 Gradle。

`OperatorGoldenR27.fingerprints` 原来把内部只读表达式交给公开 `PolicyAst.eval`，产生可外逃完整 double 矩阵，再建立完整摘要字节缓冲。现在在实际 `evalScopedR31` 生命周期内读取数值，每个 IEEE-754 long 仍用 big endian 编码并依次送入 SHA256；只保留一个8字节摘要工作区，返回值仍为普通指纹字符串 Map。调用方的 cache 保持打开，scope 在成功和异常路径都关闭。

源码确认的测试所有权缺口及修正：

| 文件 | 原未关闭资源 | 实际生命周期修正 |
| --- | --- | --- |
| R27QualityTest | H10/H11 的 before/after cache；H10 直接传入 readGolden 的文件流；两个 helper 创建的私有 evolution/cache | try-with-resources；helper loop 在其 cache 之前关闭；原 executor/pool 的实际终止等待保持不变 |
| R27ReplayTest | full cache、last200 中每个已不再使用的前缀 cache、gap/speed 测试 cache | deep fork 同步返回后关闭旧前缀，最终和失败路径关闭当前 cache，其他自有 cache 在最后调用后关闭 |
| R30IncrementalFeatureTest | full cache、121次增量循环中的旧 cache、prefix-rebuild 的父子/独立 cache | 与 last200 相同的父子所有权，fork 失败关闭未发布子 cache，最终保留的公开数组仍完成原逐位比较 |
| R30ModelSurfaceTest | 自建 surface cache 和 ready.evaluation | context 在共享 cache 之前关闭，没有关闭不属于该 context 的输入资源 |
| R30FailureRepairTest | helper 的临时原始 context、返回 context 的私有 cache、三个共享 cache 的局部 context | 测试内 AutoCloseable 只拥有 helper 自建 cache/context；临时 context 在转交前关闭，转交失败关闭 cache；共享 cache 的 context 不负责关闭该 cache |

基线 EvalContext 只有原三参构造，close 不拥有 cache，因此没有虚构 ownsCache API，也没有越权修改该生产类。没有把取消回执当作实际退出，没有加入重试、forkEvery、Xms、额外堆、假 Probe、强制 GC、skip 或数学门槛调整。

运行依赖先复制为固定快照，见 SNAPSHOT.json。实际 Android HOST app jar 的 BuildConfig 为 `2ab54e2b3ad1772b560a15404f0421084d10c5c1`、dirty=true；它是父任务保存的012实际编译支持，不是本分支 clean APK。五类测试从本隔离基线/修改版源码 fresh javac，未引用主树会变化的 testclasses；生产覆盖只包含本模块 OperatorGolden。所有命令和退出码保存于 BASELINE_EXECUTION.json、MODIFIED_EXECUTION.json、FINAL_EXECUTION.json。

实际结果：

- 修改前五个原类，同一默认512MiB HOST JVM：33/33通过，17.755秒。未复现父任务全套472项中的容量失败；这份子集结果不替换原始失败。
- 原/新 OperatorGolden 的52指纹逐项一致，并均等于既有保护52指纹。保护 golden 二进制和两份指纹文件前后 SHA256 不变。
- 最终六个文件冻结后，原五类同一默认512MiB HOST JVM：33/33通过，24.467秒。H02实际比较200期逐位一致；原 H10/H11 等价/受影响策略划分保留；FailureRepair 的5000次尝试、3700个合法产物、47次目标窗口修改和100个家族子代保持原结果。
- 原245处 assert/fail 调用 token 序列、619个数字 literal 序列、33个测试方法序列完全不变。可从同一基线运行 `python3 verification/r31/p02/owner-consumers/verify_preservation.py` 复核。
- 独立实际所有权检查通过：私有失败 fixture 的 cache/context 均关闭；golden 返回后 readerLeases、workingLoans、activeProductions、busyScopes 全为0；调用方 cache 仍可使用；非 owning context 关闭后共享 cache 仍可使用。

没有将 drop 引用、scope 归还或较少 GC 次数写成真实堆释放。最终测得 H 为177917440字节，与修改前的167201936不能作为内存或性能改善证据。父任务全套472/8原始失败仍须在合并全部必要修复后重新执行；本模块没有取得全套、Android安装、24/72小时设备或自然质量验收。
