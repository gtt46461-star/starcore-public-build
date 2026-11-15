# R32.4 保留、冷热读者与完整闭包的宿主回归

本目录只含测试源码和可移植入口，不携带 Android SDK、JDBC 或其他二进制工具链。测试必须以同一候选仓库的**全量生产编译结果**运行；脚本只编译 Android 边界适配器和测试，不编译、替换或伪造任何业务类。

## 运行

在已完成完整生产 Java 编译的工作区执行，路径按实际位置替换。`--classes` 可以是 classes 目录或整包 JAR；`--out` 必须是不存在或空的目录。

```bash
python3 verification/r324/retention/run_retention_host.py \
  --repo /path/to/candidate \
  --classes /path/to/full-production-classes \
  --android-jar /path/to/android.jar \
  --jdbc-jar /path/to/sqlite-jdbc-3.46.1.3.jar \
  --slf4j-jar /path/to/slf4j-api-2.0.13.jar \
  --out /path/to/new-retention-receipt

python3 verification/r324/retention/run_closure_host.py \
  --repo /path/to/candidate \
  --classes /path/to/full-production-classes \
  --android-jar /path/to/android.jar \
  --jdbc-jar /path/to/sqlite-jdbc-3.46.1.3.jar \
  --slf4j-jar /path/to/slf4j-api-2.0.13.jar \
  --json-jar /path/to/json-20240303.jar \
  --out /path/to/new-closure-receipt
```

需要 Python 3 和带 `jdk.compiler` 模块的 JDK。入口优先使用 `javac`，缺少独立可执行文件时使用 `java -m jdk.compiler/com.sun.tools.javac.Main`。Android 平台适配器从候选仓库既有 `tools/r31/run_atomnet_publication_host.py::platform_sources()` 取得，SQLite 实际执行由 sqlite-jdbc 的原生 SQLite 完成。

## 工具链来源与实物身份

| 文件 | 官方分发来源 | SHA-256 |
| --- | --- | --- |
| `android.jar`，Android 36 平台 | `https://dl.google.com/android/repository/platform-36_r02.zip`，取其中 Android 平台 JAR | `d9eb9da824d9e247a352f570f01e1169e725b2954bca9e283a71786c59b59f9a` |
| `sqlite-jdbc-3.46.1.3.jar` | `https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar` | `4a4832720a65eaf7f4d6fd7ede52087b994dc5633c076f9e994dc0c8b4b0b4fa` |
| `json-20240303.jar`（可选 compact 输出运行库） | `https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar` | `3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed` |
| `slf4j-api-2.0.13.jar` | `https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar` | `e7c2a48e8515ba1f49fa637d57b4e2f590b3f5bd97407ac699c3aa5efb1204a9` |

入口记录实际输入哈希。表中的 Android 哈希是解压得到的 `android.jar`，并非 ZIP 哈希。JDBC 无 SLF4J 日志提供器时的 NOP 日志提示不影响 SQLite 原生执行。`--json-jar` 仅在闭包入口中可选；提供后增加 compact 根前缀、缺失/损坏和取消的真实输出断言，不提供则明确记 `NOT_RUN_OPTIONAL_JSON_RUNTIME_NOT_SUPPLIED`，不伪装执行。Maven JSON-Java 是宿主边界运行库，不等同 Android 框架 org.json 的真机验证。

## 验证范围

- `ColdCheckpointR324HostTest`：真实压缩、SHA、文件同步、原子发布、退热、四个故障点、缺失/损坏/符号链接拒绝、实际释放字节、进程残留临时文件清理。
- `AtomNetArchiveR324NativeHostTest`：实际生产 DDL、已知旧触发器迁移、域签名附件、精确样本编码、V1/V2 身份、一次性退役许可、事务回滚、活动预算、真实冷热样本读者和内存租约。
- `CheckpointClosureR324NativeHostTest`：创建超过 17,000 个不同原始文件，实际执行生产磁盘索引、标准数据库根、去重、manifest 子图、冷热 SHA 校验、取消/损坏/磁盘准入失败和私有索引清理。
- `StorageRestoreAssetsR324HostTest`：实际复制超过 17,000 个文件、生成和复核流式清单、通过原生产恢复消费者读取未知旧表中的版本化 BLOB 引用、原件保护、损坏拒绝与全量私有工作区清理。

`RECEIPT.json` 保存测试源、生产源与生产类、依赖、平台适配器的 SHA-256；运行命令、编译日志和逐条断言输出也保留。只有进程成功结束、断言计数解析成功后才记录 `PASS_HOST_INVARIANTS_ONLY`。

这些回执证明 Linux 宿主上的生产 Java 算法、原生 SQLite、真实文件系统不变量。样本事务测试使用生产触发器及真实退役 SQL，但由宿主直接驱动 SQL，不宣称 Android Worker 调度已经运行。完整闭包测试执行实际生产遍历和恢复文件消费者，不等于 Android Binder 私有恢复进程、完整 APK 安装、报告 UI、大规模真机断电、24/72 小时持续运行或未来策略胜率已经通过；这些状态明确保留为 `NOT_RUN` / `NOT_EVALUATED`。
