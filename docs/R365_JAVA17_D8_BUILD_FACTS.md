# R3.6.5 Java 17、官方 Android API 与 D8 构建事实

全部生产 Java 源码由 JDK 17 编译为 classfile major 61，编译类路径为官方 Android 36 `android.jar`，
随后由 D8 9.2.23 使用 `--release --min-api 21` 转换为 DEX。构建门禁止 Java 7、legacy dx、伪 Android
stub ABI、错误 framework 方法描述符、源码内 API key、签名密码和私钥文件。

最终 APK 必须同时通过：

- T01–T44 主机合同与 Java 17 全源码编译；
- R3.6.5 durable claim、Owner 围栏、重试退避与只读诊断静态门；
- 114/114 B01–B15 合同逐文件审计及占位/TODO/空实现扫描；
- AAPT2 包名、versionCode、versionName 核验；
- DEX 反编译类/方法存在性与 Android ABI 描述符门；
- zipalign、APK 签名证书连续性与 ZIP 完整性；
- 总包内实际工具链执行一次完全离线复建。

离线复建强制比较 `classes.dex`、`resources.arsc`、二进制 Manifest、签名证书及包/版本身份。APK 容器
可能因 ZIP 排列或签名元数据而非逐字节相同；报告会分别记录原 APK 与复建 APK 的 SHA-256，只在上述
可执行载荷全部相同时声明 payload equivalent。

Java 17 是源码和 classfile 构建版本，不是 Android 系统版本要求。APK 只携带压缩 DEX 与资源，不包含
Java 源码、JDK、Android SDK、D8 或完整方案，因此 APK 体积不能用于判断源码是否“满血”。总包中的
源码 ZIP、Git history bundle、工具链、方案原件、逐文件哈希与复建日志才是完整性证据。
