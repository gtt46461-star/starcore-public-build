R929 首请求前置与精确只读诊断

基线为实际 014 提交 `961a6b8a74b9624a2b195e9562fbdfc9283b29fc`，隔离分支 `construction/r31-platform-readiness-20261104`。原 API29 与 API35 的各14个故障单元都在故障注入前因 `R932_BUNDLE_CAPTURE_COMMIT_FAILED → SCHEMA_PENDING:R929_PLATFORM` 失败；原日志由主树 `verification/r31/p02/delivery_014/API29-FULL-ANDROID-FAILED.log` 和 `API35-FULL-ANDROID-FAILED.log` 保留。这些故障机制当时没有到达，不能把它们记为已验收。

实际复现：冻结最终014 Android类（jar SHA `15a524242feefe47f158c3cb4513b0ac4a5c7dc9038b4bf924525d11ec710b96`）加原生 SQLite，执行真实 DbHelper/Critical 前置建表，R929 的9表4索引全缺失。再按原 R30Fault.seed 的 RuntimeSchemaR26/SU1/SU2/SchemaR27 真实方法执行后，13个对象仍全部缺失；bundle registry capture 准确拒绝且业务 CREATE/ALTER/DROP 为0。调用原 ResearchSchemaCoordinator 的真实 R932_UNIFIED 回调后，完整列、PK、索引、global unique 均满足，capture 才成功。正常 DbStore 构造只准备 Critical，R929 原来属于异步 Research 迁移，首 ModelBundle 请求没有同步前置。这是生产顺序缺口，不能只在测试补 CREATE。

修复由原唯一 writer 完成：ResearchSchemaCoordinator 新增 `ensureRuntimeBundlePrerequisitesR31(DatabaseCoordinator)`，使用显式 MIGRATION / `R929_PLATFORM_SCHEMA` 调 Root 的 `SchemaDomainCoordinator.migrateR31PlatformPrerequisites`；只安装既有 R929 的13个对象并严格验证。Root 在实际 DbStore 构造的 Critical 完成后、RuntimeReleaseEpoch 前调用入口，使用独立 `RESEARCH_R31_PLATFORM_PREREQUISITES` rev1。它不将整个 Research 标为 READY，也不等待历史研究全图。Root 的 DbStore/Domain 变更由 Root 单独提交，本分支没有覆盖这些文件。

ResearchPlatformStore 的 `schemaReadinessReportR31` 强制读取实际元数据，区分缺表、缺列、主键不符、索引缺失/定义不符、global unique 未证明、元数据上界及连接不可用。只读检查不现场建表。报告最多64项原因；SchemaNotReadyR31 带 typed DEPENDENCY 及完整 report，保留旧 getMessage 合同并在异常串中附实际首因。原缓存仍以实际 schema_version 和真实提交作用域有效，诊断本身绕过 memo 重查；不写虚假 stamp。ModelBundle 的原 pointer CAS、registry 原子写入和 scope/模型逻辑不需要修改。

最终本模块原生证据：`runs/20261104T195444Z/RECEIPT.json`。122项实际断言通过：

- 014缺源复现、当前显式 Domain 迁移、迁移回滚和再次恢复；独立 Domain 不串成全 Research READY。
- bundle registry 真实写入/唯一 ACTIVE 约束，以及业务路径0 DDL的实际 SQL trace。
- 缺表/列、PK、索引定义、global unique、仅 partial unique 不能替代 global unique、元数据数量上界、已关闭连接 UNKNOWN。
- 已 warm 的 cache 后发生事务内 DDL，rollback 后恢复实际 committed contract；诊断既不创建对象，也不改变 schema_version。

工具冻结复制35源联合编译输出并逐类哈希，再 fresh 编译本模块 R929/RSC 与 Root 的 DbStore/Domain 四个源码，测试前后全部输入保持一致。固定类闭包和命令见 receipt；它不是 APK 或 ART 执行证明。

```sh
python3 tools/r31/verify_platform_readiness.py \
  --joint-manifest /workspace/r31-shared-migrations/verification/r31/p01/shared-startup-joint/20261104T194418Z/compile-01/RECEIPT.json
```

新增 `R31PlatformReadinessInstrumentedTest` 三项实际 Android 测试：正常首 DbStore 构造/数据库与 context/filesRoot 绑定/ModelBundle capture及重入；实际 SQLite 元数据漂移负控；Domain 回滚和原 owner 重试。原 R30FaultMatrix.seed 在其测试 DDL 前先断言生产构造路径已建立 strict contract，保留原14个故障单元全部工作量。三份 Android 测试源码 fresh javac 已通过，记录在 `androidtest-source-compile/RECEIPT.json`；实际 ART 运行仍为 NOT_RUN，由 Root 下一轮 API29/35 完整矩阵验证，不以编译替代执行。

保留原始试验失败：首 HOST 运行遇 SDK org.json 的 Stub，原错误仍在 baseline；加仓库已有 AOSP JSON 实现作为 HOST 平台后复验通过。首 metadata fixture 误将“完整原因列表的一项”要求为“异常首因”，修为同时验证完整列表及真正首因，生产源码没有为此放宽。原始失败在 `metadata-first-harness-failure/`。

仍未取得的证据：新的 Android 完整构建/身份、三项新 instrumentation 的 ART、两 API 原14故障矩阵、完整正常 DbStore 构造的联合 HOST 结果、物理设备、24/72小时及自然未来质量。没有清数据库、降低唯一性或统计门槛；本次没有 B10 主链权威选择切换或缓存回收。

源码与本轮原始证据提交：`6e7a9d1470fc0ef58acf09f443ec3c6a1751dc86`。该证据固定在测试时的 DatabaseCoordinator、Root Domain ec280eb3/DbStore f8a4 及35源 snapshot；之后 WriterHoldFaults 等新源必须由 Root 的新联合验证覆盖。
