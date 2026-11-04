只读结论：现行内存准入没有保证在有界时间内恢复。已经不可达但尚未被收集的数组仍进入 measured H；释放缓存引用后若 H 不降，真实需要新 backing 的请求可以持续被拒绝。尚未执行无限等待反例，也没有据此证明 Android 永久停跑。

`MemoryBudgetR31:153-157` 的 optional 可用量为 `floor(.75*M) - H - max(ceil(.15*M), realtimePeak) - uiPeak - pendingPromises`，各减法下限为0。512MiB 的 M=536870912，默认扣 realtime reserve 后是322122547 B，约307.2MiB。H约327MB时，即使 pending=0，新的 optional allocation 也没有准入余量。`softPressure` 在 `.75*M` 才置位，直到 `.60*M` 才恢复（169-175），所以此时可以显示 NORMAL 同时 optional 拒绝；两者不是矛盾。

`tryAcquire:87-111` 第一次失败会运行有界缓存回收 callback，再重新采样并尝试一次。它明确不从 H 中扣除被 drop 的引用，也不请求 GC。`RuntimeProbe:200-202` 的 H 是 `totalMemory-freeMemory`，不是 live-after-GC；`sample:178-179`、`ReferenceQueue:137-138` 都只是采样/轮询，不能自己完成收集。只有真实新采样下该请求可准入时，`update:176` 才清除 optionalWaitSince。

`RuntimeCoordinatorR26:127` 受限时每30秒 trim；129-130 降为一个 research worker；469/471 的零进展任务在一秒后重试。`MemoryControlStateR31:55,86,91-92` 的两分钟阈值只产生年龄、OVER_TWO_MINUTES 原因和报告，没有超过该时长后的另一条准入或回收动作，也没有有界恢复 deadline。控制器将 actionPolicy 明确写成 NO_FORCED_GC/NO_TASK_REVOKE。

能令 H 实际下降的来源是运行时自然收集、Android/VM 外部收集，或仍获准的实时/UI/其他线程继续真实分配后触发收集。Coordinator/proc 采样、遥测和重试本身也会产生小对象，可能最终触发自然收集，但没有字节/时间界限；不能把“可能最终发生”当作已证明的恢复承诺。已经持有、真正复用且无需新增 backing 的路径也可能继续工作，这不证明一个确需新 backing 的请求能恢复。

全源码确实有两处显式 GC，但不是普通 optional 等待链路：`AutoFetchService:1112` 属于 init failure 的 `releasePartialObjectGraphR3610`，`4657` 属于 `handleAnalysisOomV7`。`onTrimMemory/onLowMemory:17509-17550` 清缓存、异步记录并确保运行环，不调用这两处 GC。普通 resource wait 不保证触发初始化失败或 OOM，因此不能依赖它们作恢复证据。既有 H07 HOST 测试本身没有 Coordinator/Service 的Android生命周期，但真实生产恢复链也没有有界 GC 承诺。

因此需要把“容量真的不足”与“有大量暂未收集垃圾导致准入冻结”分别验收。root提供的同栈约193MB可收集 double 证据只能证明可达性，不等于此时已释放或会在时限内释放。本次没有重新获取该 dump、没有加 Xms、强制 GC、改 probe、调阈值或改断言。若继续完善恢复，必须保留真实年龄和观测、让失败请求最终重试并留下实际工作推进，不能仅把 wait 归零、下调 H 或填 PASS。
