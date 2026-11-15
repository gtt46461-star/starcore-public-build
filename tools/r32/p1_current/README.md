# P1 当前生产代码的可执行主机验收

本入口针对 P1.01、P1.02、P1.03 的明确生产方法，直接编译当前待验工作树中的生产源码，使用仓库 `tools/r31/ddl_host` 的 Android API 传输适配器连接原生 SQLite。没有替换 `HypothesisStoreR31`，也没有使用旧 lifecycle fixture 的 store stub。测试脚本可以在 P1 应用完成后独立执行，不依赖 P2—P5 的文件或回执。

## 调用

依赖为 JDK 17、Python 3、Android SDK API 36 的 `android.jar`、SQLite JDBC、SLF4J API，以及同一 P1 工作树全量主机类型编译得到的支持 class 目录。所有路径由参数提供；交付代码没有硬编码本次审计机路径。

```bash
python3 "$P1_TOOLS/run_p1_current_host.py" \
  --repo "$P1_CHECKOUT" \
  --classes "$P1_HOST_SUPPORT_CLASSES" \
  --sdk "$ANDROID_JAR" \
  --jdbc "$SQLITE_JDBC_JAR" \
  --slf4j "$SLF4J_API_JAR" \
  --out "$NEW_P1_RESULT_DIRECTORY"
```

`--out` 必须是尚不存在的新目录。支持 classes 应来自此阶段源码的主机类型编译，不把后续批次 classes 作为 P1 的业务依赖。此入口重新编译实际受验生产类并放在支持 classes 前面；同时记录支持 classes 的完整 SHA 清单，防止不透明的环境输入。SDK 是编译 API，执行时 Android 数据库 API 由已记录 SHA 的 JDBC 传输适配器承接。

## 验收绑定

| 问题 | 执行对象 | 期望结果 | 证据边界 |
|---|---|---|---|
| P1.01 严格迁移 | 当前 `LegacyDdlR31`、`LegacySchemaCoordinatorR926`、`SchemaDomainCoordinatorR926`；原 `StrictMigrationR31Host` 与附加 `CompletedDriftR32Host` | 61 + 12 = 73 项断言；真实进程中断恢复；已完成定义漂移重开正确 owner；原行保全 | 合成旧库上的主机迁移合同，不替代用户设备数据库回放 |
| P1.02 假设账本配额恢复 | 当前 `HypothesisStoreR31.recoverQuotaSliceR32`、`requireSchemaR31`、`canAdmitResearchR31` 和实际 DDL/trigger | 119 项断言；四表 326 行；每事务最多 64 行；逐页回滚/恢复；真实子 JVM kill；UTF-8 计费；异常 journal 拒绝；幂等 seed/准入恢复 | 直接生产存储方法，不单独证明服务调度或完整策略出生链 |
| P1.03 尾部恢复意图 | 当前 `PredrawTailRecoveryR32.reconstruct`、生产 issue resolver/history policy | 9 例真实生产 reducer/resolver 回归 | 不测网络、Android 服务或回调线程时序 |

## 配额 fixture 的准确场景

建立真实生产表、索引与保护 trigger，向四表写入 66、65、131、64 行。删除 quota singleton 模拟需要补齐账本的旧库，验证新写入被拒绝、原件重复不重复计费、原件更新/删除被保护。通过中文和补充平面 Unicode 字符验证按 UTF-8 字节计费而非 Java 字符数。

首片在独立 JVM 中事务写入后 `Runtime.halt(73)`，重开数据库必须没有未提交 cursor/quota 且原行摘要不变。随后每个片先 rollback 并关闭数据库，重开后提交同一片。最终合计应精确等于独立逐行字节计算；quota 与 journal 删除同事务公布，准入只在完成后打开。

journal 检查覆盖：修改累计值但保留原摘要、错误列表结构、越界 stage、负累计值、超上限累计值、错误复合键长度。这里的 SHA 是内部恢复日志的一致性校验，不是对能任意改库并重新计算摘要的攻击者的认证机制。

## 输出和判定

每项命令必须退出 0，预期成功标记必须出现，所有直接生产/适配器/测试/依赖与支持 classes 字节在运行期间不得改变。`RECEIPT.json` 绑定生产源码 SHA、测试 SHA、依赖 SHA、执行命令、日志和范围。`SUPPORT_CLASSES.json` 记录支持 classes 清单。

这是该三项方法合同的 `PASS_HOST_CURRENT_PRODUCTION`，不能把这个回执写成 P1 全批业务签收，也不签收 P1.04—P1.06、Android Gradle 构建、设备表现或自然样本策略品质。主批次的其他本批用例仍应独立执行，缺证据明确保留为未运行；不能要求未施工后批代签。
