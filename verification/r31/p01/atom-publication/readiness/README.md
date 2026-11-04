# 旧源缺失／迁移事务缓存：独立最小返修

这是生产4bd和证据6e之后的独立修复。生产只改 `AtomPublicationStoreR31.requireReadyR31` 及其异常cause构造；29个DDL对象、rev1、科学计算、分片、fence／receipt协议及预算预留不变。

真实4bd原字节负对照表明：删除 `r27_atom_score`、`r27_molecule`、`r30_atom_contribution` 任一，view定义仍存在，旧catalog-only readiness会返回READY，随后真实view SELECT触发原生SQLite错误并被分类为BUG。另一个实际负控在迁移事务中看到健康schemaVersion+1后回滚，提交另一种损坏DDL重新得到同一个version，旧缓存再次错误命中。原失败行为没有被改写成新实现通过。

修复后的冷路径固定9个只读调用：前后两个schema版本、一个对象catalog、三个legacy列合同和三个实际view `SELECT * ... LIMIT 0`。还核对旧源必须为table。只有非事务的已提交版本能进入缓存；热路径仍只有一次版本查询。原业务事务中的schema guard保留原来一次版本查询；没有放宽32-op协议预留或64／动态字节上限。

三个实际 `AtomWorldR27.score` 缺表入口、rollback版本复用、缺列及错误type都返回具体typed DEPENDENCY。新业务事务、业务DML及业务DDL均为0；不会因原生缺表错误计入BUG隔离。查询未知异常保留cause；已知storageFATAL、busy和cancel保留原分类；嵌套LinkageError／OOM／ThreadDeath原对象上抛。后者是明确故障注入的分类边界测试，不是实际低磁盘／低内存／设备测试。

恢复测试走**真实DatabaseCoordinator.MIGRATION事务**调用实际 `SchemaR27.ensure`，没有为通过测试手写恢复表。随后老active指针和完整165结果仍能被实际view和state消费者读取。这里的Schema来源是本隔离分支的241a继承代码，收据有逐文件SHA；**root最新共享journal／History／Evolution同版完整启动仍待root联合验收**，不得把本地原版本启动范围冒充其通过。CI会从执行仓库编译实际Schema与完整app类，运行相同恢复断言。

隔离fixture的DROP只是故障注入，交付生产修复没有DROP／清空数据／运行期CREATE。坏type／缺列只报告依赖，不越权抹掉不兼容旧数据。

`runs/final-ci-same-source` 包含新增11组及原20组完整回归；实际退出73／74和64 MiB fresh-JVM恢复均重跑。`SUMMARY.json` 保留最终同源统计；全部之前试验、故障和SQL无损gzip仍在。支持jar固定为ba35完整真实构建，SHA见收据。31组驱动包含33次Java进程；其中4个baseline缺源负控是**已复现旧缺陷**，不是生产业务PASS。

旧原子／分子生产源和旧readiness helper从已提交真实字节冻结在 `tools/r31/atom_publication_host/baseline/`，有原commit及SHA核验，只编译为负对照。这样公开CI不依赖未推topic对象；当前生产始终从实际app源码编译。

本修复不关闭整张B02-C02、B05容量、Android构建、设备SLO、24／72小时或自然未来质量。HOST fixture只在实际connection关闭后删除它自己新建的临时数据库；其他代理的文件和hprof没有被触碰。

**最终同源HOST时延门FAILED。**7067次可测hold的p99为12ms、最大401ms，14次超过50ms、2次超过200ms；另1次事务中halt的完整hold未知。两次硬超时为 `interrupt-in-transaction` 的第14事务（322ms，原生事务区间318.963726ms）及 `process-aftercommit-resume-64m` 第120事务（401ms，原生事务区间399.917781ms），都是真实已提交的 `R27_ATOM_RESULT_STAGE`，21 API SQL、3个trigger保守上界、无DDL、wait为0。原始BEGIN单独延迟、SQL与END耗时分解、GC及调度当时未采，均为UNKNOWN；不能事后编造原因。父任务同时有其他验证工作，原始精确重叠窗口未知。这个性能开放项不改变源码正确性提交，也不允许宣布整卡或工程闭环。不能拿之前较低67／122ms替换本轮失败记录；后续正式Android设备P99单独验收。
