本模块只修 HOST 工具和必要 fixture，不修改任何生产、Android 测试、版本、身份、schema 或全局账本。分支从前模块 `bc6a8066cddf09fdbc799ac1fc26cad39ad5ddd0` 继续，工作区为 `/workspace/r31-owner-consumers`。没有运行 Gradle，没有修改主工作树或 root 的 storage_host 脚本。

先复制父任务保存的实际支持 jar（SHA256 `892e5be7feb3609755c6746ea080644fd19427a90f8d9ab8493c964af1f01fa8`）、实际生成 R.jar、SDK 和 JDBC。4404 个展开后的 class 逐字节对应原 jar；只把该复制品 class 的 mtime 设为较旧值，模拟 CI 新检出的源比编译支持新。没有改生产文件内容或 mtime。原始 CI 错误尾日志完整保存在 original-ci-log-tails，明确为尾日志而非原始完整 artifact。

原五种故障全部实际复现，见 BASELINE_RESULTS.json 和 baseline-*.log：

| 路径 | 原失败 | 修正 |
| --- | --- | --- |
| timeline / snapshot | ReportSnapshot 调用已编译 RuntimeCoordinator 的 per-PID 观测，SDK Process.myPid 在 HOST 抛 Stub! | 只为该平台 getter 返回真实 JVM PID，并检查 Android int 范围；其他 Android owner/设备事实仍保留 UNKNOWN/NOT_RUN |
| task / health / recovery | 显式 app 源目录允许 javac 因时间戳选择整个应用源，隐式编译 MainActivity 后缺 R | 明确当前 producer 列表，全部本模块 javac 使用空 sourcepath 和 implicit:none；依赖来自真实现有 Android class 和必要实际 R.jar，没有生成假 R |
| service | 公共 shim 已有 getColumnCount 和 columns，工具再次插入导致编译重复定义 | 扩展已支持当前和既有 legacy 形状，已有方法直接保留；两次转换幂等，公共 shim 本体 SHA 未改，Android 对 null selection argument 的拒绝保持原样 |
| matrix | SchemaR27 的 feedback 扩展先于基础 RuntimeSchema，缺 main.r26_feedback | 使用既有 R31OwnedRuntimeFixture 的实际 DatabaseCoordinator，在 MIGRATION 上下文和真实 native SQLite 事务内按 RuntimeSchema→SchemaR27 启动；保留两次 SchemaR27 幂等调用和所有旧验证 |

新 `host_tool_support.py` 支持 JAVA_HOME、ANDROID_HOME/ANDROID_SDK_ROOT、GRADLE_USER_HOME 默认用户缓存，以及 R31_HOST_CLASSES、R31_HOST_ANDROID_JAR、R31_HOST_RJAR、SQLITE_JDBC 路径覆盖。service 必须获得真实 R.jar；没有该依赖时明确失败，不以假资源代替。默认 CI 仍可沿原 app/build/intermediates 路径调用。

七条最终真实结果见 FINAL_RESULTS.json：

- timeline、snapshot 原 native SQLite 合同通过；报告未知值和缺源含义保留，不代表 R31 的44指标/96检查/47图已经集成验收。
- matrix 原 rollback/retry、13原表、5个实际 typed producer、14个显式依赖、原模型字节保护均通过；Calibration 原 native 合同通过。负 BSS、unsupported rows 等真实诊断结果保留，不是自然质量通过。
- taskRuntime 原211项断言通过；RuntimeHealth 原99项断言和7个故障分类 JUnit 通过。
- service 修复后先在默认权限进入实际 HTTP 时得到 SocketException: Operation not permitted。经平台正常 require_escalated 仅批准同一 HOST 脚本的本机临时回环服务器后，实际服务/native HTTP 合同及原6个 JUnit 通过。默认失败日志和批准后日志都保留；未修改网络策略、代理、TLS 或凭据。
- 新的5项 SQLite boundary 检查真实验证空结果仍有列元数据、null selection argument 被拒绝、SQL结果的 NULL 含义和实际 JVM PID；这不是 Android 调度或服务回调证据。
- Recovery 原23项断言通过，真实等待120202ms，clockSubstitution=false、probeSubstitution=false。实际生产控制器请求收集一次后重新准入；businessProgress=false、物理设备 NOT_RUN 保留。没有在数学工作负载插入 GC，没有用新 fake probe 或额外 GC 调用换绿。

原 matrix 的16处 check 调用 token 和全部数字 literal 序列不变；另外10个既有 HOST harness 文件 SHA 完全不变，见 HOST_CONTRACT_PRESERVATION.json。1247个生产 Java 文件前后 SHA 完全相同，见 SOURCE_SUPPORT_PROVENANCE.json 和 SNAPSHOT.json。各脚本原有最大堆参数保持不变。

memory 的5237、commit_failure 的b225、research_schema 的31fea历史对象缺失由 root 修复 CI source clone 的 Git 对象可用性。本模块未改这些 baseline 预期，未改 verify_memory/run_commit_failure/run_research_schema，也未把本七条结果当成十条完整发布门。需要在 root 合并最终生产依赖并重新构建后再验；本模块没有 Android、APK、真机、24/72小时或自然未来质量验收。
