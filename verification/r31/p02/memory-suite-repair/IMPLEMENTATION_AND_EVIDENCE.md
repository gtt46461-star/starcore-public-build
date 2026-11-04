七个生产文件已冻结，继承源码提交 e96e11cc850672549863a6290b40350fa5248a08。精确 SHA256 见 FROZEN_SOURCES.json，最终定向验证依赖包括当前 PitGuardR30、EvolutionLoopR27、NullEvolutionR27、NullPipelineSimulationR30；验证前后源哈希一致。未 commit/push 或运行 Gradle，由 root 联合整合。

首次同 JVM 512m 诊断实际复现：M=536870912、H=284385472、FEATURE_CACHE 声明58830768；其中64个不同 raw 数组声明25608192、83块 shared声明33222576。原公式把已经真实分配的 primitive payload 同时算入H与未来reservation，发生提前拒绝。这个快照是在失败回调采集，不能当申请指令瞬间的原子快照；未证实应用全部强引用泄漏。去掉已证实分配的payload重复扣额后，后续仍真实抵达(.75-.15)M研究余量边界，原天然容量失败全部保留。

实际实现：

- MemoryBudget 仅对实际完成的 primitive allocation 记录 payload 证据，只有晚于分配的 heap sample 才覆盖它；未知scratch/metadata/working估计继续保留。owner声明持续到实际结束，不修改测量H，不宣称physical heap释放。
- fixed private scratch backing 配独立 ReferenceQueue；只有数组真实不可达/GC观察后可结束这个固定对象的声明，普通可grow工作lease不适用。旧fix5 orphan反例确实失败且残留3个lease，日志orphan-private-before.log；修正后孤立GC生命周期回归通过。生产未调用GC。
- optional admission 的真实最后拒绝、请求字节、kind、单调时间与episode/recheck分开报告。水线NORMAL不再被误解为有可选余量；RuntimeCoordinatorR26由task_runtime整合控制。
- FeatureCache内建private producer转移唯一backing，通用publish仍clone。scoped AST只复用从未公开的private中间数组，busy reader未退出不可复用；旧公开double[]仍独立且不回收复用。
- EvalContext公开memo用强引用byte-LRU（min(8MiB,M/64)，原96项上限仍保留），drop仅是引用移除；temperature、trainLogLoss、correlation实际读scope。BirthGate与ReplayEvaluator也实际消费并finally关闭scope。
- 实时critical只由明确predictNext入口传至冷shared leaf、fallback raw及同线程内置依赖，finally恢复；历史批量研究未升级权限。原AST/质量/K3/数据角色/算术顺序保留。
- BirthGate实测把RESOURCE_WAIT永久记成RANGE:EVAL:CapacityWait，现按共用FailureDisposition向调度传播资源/取消/旧owner/致命原因；真实无效策略仍走原拒绝门。

执行命令：PATH=/workspace/starcore-environment/tools/jdk-17.0.17+10/bin:$PATH python3 tools/r31/verify_memory.py。

最终定向结果：43项新/针对JUnit、原PIT/model6项、52个5237b基线golden fingerprint全部通过。43项含明确控制Probe与孤立GC的生命周期用例，不能称Android内存/自然质量验证。golden在独立真实默认processBudget、512m JVM上运行，属于数值ABI证明。

真实默认RuntimeProbe、512m独立JVM中，5000行/128候选完整九步成功；OS lowMemory observer明确注入→DEPENDENCY/RESOURCE_MEMORY→旧worker真实退出→原cursor恢复，全部events、score JSON、champion与最终RNG同healthy相等。healthy步边界观测峰H=177407632，resume=211082112。这只证明该完整一代与受控恢复，不能覆盖100轮null/持续多代原数学失败或72h。

实时预留真实堆压力实验：强持并登记343597383字节primitive压力owner，实际H=354154944；研究返回DECLARED_CAPACITY，正常水线下optional状态明确WAIT；冷实时预测的嵌套leaf与scratch都使用REALTIME，共1600656字节。概率和唯一K3正确，显式live范围退出后历史仍等待；无替换Probe、无加堆、无GC调用。

天然多代容量失败仍见fix5-failure-details.json（427项诊断中19失败，含临时overlay的1个provenance失配），以及root/build agent独立fresh原Evolution/null结果。旧日志和hprof摘要是各自源版本的证据，不冒充最后整编结果。hprof(all,false)仅证明异常退栈+trim之后的强图，不能外推申请瞬间。原日志保留，压缩副本用于交付，hprof本体仅/tmp。

施工卡状态仍BLOCKED。root还需运行无排除整Android编译/身份/全套tests；APK、设备、24/72h、native设备校准、跨PID总额度、自然质量均未在本组证明。公开score/Trace结果分配估计、mutable family/model state、leaf内部scratch、其他未登记owner和ReplayDataset公开可变数组仍列真实覆盖债务。
