# R3.6.3 Java 17、官方 Android API 与 D8 构建事实

正式 APK 的全部生产 Java 源码使用 JDK 17 编译为 classfile major 61，编译类路径是官方
Android 36 `android.jar`。随后由 D8 9.2.23 以 `--min-api 21 --release` 转换为 DEX。
构建脚本禁止 Java 7 和 `com.android.dx`，并在最终 APK 上反编译检查 Android framework
方法描述符、关键类/方法、PendingIntent flags、包名、版本号、zipalign、签名证书连续性和 ZIP 完整性。

Java 17 是编译语言与工具链选择，不意味着 APK 只能运行在 Android 17。D8 会按 minSdk 21
生成 Android 可执行 DEX；设备是否通过仍由物理设备矩阵给出，不能由静态构建推断。

APK 不携带 Java 源文件、JDK、Android SDK 或 D8，所以 APK 大小不能用于判断源码是否完整。
总包中保留实际使用的 Android API、D8、APKTool、AAPT2、签名/对齐工具、源码 ZIP、Git bundle
和全部验证证据，可在无网络环境复建。
