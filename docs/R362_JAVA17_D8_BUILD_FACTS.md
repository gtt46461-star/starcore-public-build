# R3.6.2 Java 17、官方 Android API 与 D8 构建事实

## 可复核事实

- `app/build.gradle` 的 source/target compatibility 均为 Java 17，Java toolchain 为 17。
- 正式 APK 编译命令使用 `-source 17 -target 17`。
- 构建脚本读取一个实际生产类的 classfile 头，并硬门要求 major version 为 61（Java 17）。
- 编译 classpath 是官方 Android 36 `android.jar`，不是 `verification/r36-stubs`。
- DEX 后端是 Google R8/D8 9.2.23，min API 为 21。
- 正式构建脚本禁止 legacy `com.android.dx`，也禁止 `source 7`。
- `verification/r36-stubs` 只保留给可在普通 JVM 执行的主机合同回放，不进入生产 APK 编译 classpath，也不会打入 APK。

## 为什么 APK 仍可能只有几 MB

Java 源文件的注释、空白、测试、文档、Git 历史和构建工具不会进入 APK。Java classfile 转为 DEX 后按符号和指令存储，再由 APK ZIP 压缩；因此 APK 大小不能用来推断源码是否完整。

交付审计以可验证指标判断，而不是用体积猜测：387 个生产 Java 文件、约 36.5 万物理行、B01–B15 合同文件 114/114、逐文件 SHA-256、禁止 TODO/FIXME/XXX/placeholder、核心类在最终 DEX 中存在，并且行为门实际执行。

总包体积较大，是因为包含完整源码 ZIP、Git history bundle、原始方案、基准 APK、官方 Android API、D8/R8、反编译/签名工具与验收证据；没有为追求某个数字添加填充文件。

## 工具校验

总包的 `EVIDENCE/REPRODUCIBILITY/` 保存 Android API、R8/D8 与其他实际工具的版本和 SHA-256。`SHA256SUMS.txt` 覆盖总包目录内每个文件，外层 master ZIP 和 APK各有独立 SHA-256。
