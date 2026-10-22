# Java 17 与 Android 构建事实

## Java 17 已实现的部分

- `app/build.gradle` 的 sourceCompatibility/targetCompatibility 为 Java 17。
- Java toolchain 为 17。
- 使用 OpenJDK 17.0.19 对 387 个生产 Java 文件与 Android API 声明执行 `-source 17 -target 17` 全量编译。
- 同一批源码随后编译主机回放并实际执行 T01–T05、T27、T28、T35、T36。

## APK DEX 兼容路径

当前离线环境没有可在 Linux JVM 直接运行的现代 Android D8/R8 与完整 `android.jar`。总包中的 `d8-termux` 是 Dalvik/Termux 专用 dex 容器，不能冒充桌面 D8。因此正式 APK 的 DEX 步骤由 JDK 17 编译器输出 Java 7 classfile，再交给已验证的 legacy dx；这是 Android minSdk 21 的字节码兼容路径，不改变生产 Java 源码内容。

为了避免上一包再次因本地 API 声明产生错误 DEX，新增了最终 APK 反编译 ABI 门：所有 Android 调用描述符以真实 SDK 构建的 V51 APK为基准，R3.6 新增调用按 API 28 声明单独复核，且禁止把本地 Android 声明打进 APK。

所以准确表述是：

- Java 17 开发配置：是。
- Java 17 全源码编译验证：是。
- 最终 APK 由 JDK 17 进程构建：是。
- 最终 DEX 直接消费 classfile 61：否；为兼容现有 dx，DEX 分支使用 Java 7 classfile。
- 最终 DEX Android ABI 反编译验证：是。

如果以后具备官方 Android SDK build-tools/D8，可把 `r36_build_release.sh` 的 dx 步骤替换为 D8，并保留现有行为门、ABI 门、签名与包身份门作为回归验证。
