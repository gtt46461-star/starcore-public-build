本模块在隔离分支 construction/r31-family-admission-20261104 施工，基线为 3246ccf8eb6f7b2f7b9d0fe77fc18a70ab958056。只修改生产 FeatureCacheR27.java；未修改模型核、PolicyAst、MemoryBudget、阈值、应用身份或签名。

普通 family 初建、前缀纠偏重建和深复制现在均先申请模型构造额度。23 个内置构造上界结合实际对象图及构造分配核查，包含 NeuralFamily 未序列化的约 2.13 MiB AutoDiff 工作区。构造失败不进入缓存。F23 可替换适配器无法从本文件推断任意构造上界，必须由调用方注册明确额度；未获得该合同的路径保持显式未知容量，未伪造通用支持。

100 列私有预测矩阵在分配前单独申请额度。增长时旧、新矩阵分别持有声明；旧数组仍被持有时不能回收其许可，失去最后引用后由实际 GC 观察回收。当前模型、矩阵与行检查点仅在真实活跃使用结束后关闭。解除引用、关闭声明均不声称实际堆已经释放。

实际基线反例使用真实 F12 委托模型和 CooperativeContinuation 取消：模型已训练 6 行、已发布矩阵仍为空；重试丢失前段输出，共 60 个逐位差异。修复保留私有 pending 矩阵、完整行游标和行前模型 codec。模型调用中断时恢复该行前 codec，已完成行继续保留；有限 token 多次重试累积进展而非从头重训。检查点使用可复用直接 DataOutput，保持标准大端 primitive 和 modified UTF 字节合同。本实现是内存内恢复，不替代 P03 的持久训练检查点或杀进程恢复验收。

新增兼容重载：familyIntoR31(int tp, int index, double[] target, boolean critical)。只在显式 live 入口设置并恢复既有 ThreadLocal；历史不升级为 REALTIME。相同冷输入在历史准入被拒时，显式 live 的模型、矩阵和行检查点可使用实时预留。root 负责 PolicyAst 原 critical 参数的调用方透传及联合验证。

重放验证命令：

    PATH=/path/to/jdk17/bin:$PATH python3 tools/r31/verify_family_admission.py --support-jar /path/to/actual-android-runtime-classes.jar

本次支持 jar 为实际 Android 构建产物 /tmp/r31-p02-012-actual-android-support/classes.jar，SHA256 为 892e5be7feb3609755c6746ea080644fd19427a90f8d9ab8493c964af1f01fa8。工具记录生产源码、测试、模型依赖及支持 jar 身份并拒绝混版回执。HOST 证据不等于 Android 整体构建、可安装 APK 或设备验收。

first-codec/ 保留中间实现完整原 6 项测试的 2 项真实容量失败，耗时 288.564 秒，及匹配 SHA 的源码快照。direct-slow/ 保留同一中间实现独立原 6 项全部通过，耗时 549.839 秒。两者并存，不挑高分覆盖失败；自然内存回收时序仍影响准入。最新实现结果以顶层 RECEIPT.json 及对应日志为准。

内置初建和矩阵准入不代表所有模型内核临时分配、动态私有状态、native/SQLite/其他应用所有者已完整接入。未调整 .75/.60/.90/.15 规则，未通过普通数学路径强制 GC。Android 联合编译、全量测试、身份索引、合并提交、APK、真机、24/72 小时及自然未来质量均由主线另行记录；本模块不宣称相应验收通过。
