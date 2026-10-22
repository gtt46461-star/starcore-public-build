# R3.6.4 Java 17、官方 Android API 与 D8 构建事实

全部生产 Java 源码由 JDK 17 编译为 classfile major 61，编译类路径为官方 Android 36
`android.jar`，随后由 D8 9.2.23 使用 `--release --min-api 21` 转换为 DEX。构建门禁止 Java 7、
legacy dx、伪 Android stub ABI 和错误 framework 方法描述符。

最终 APK 需同时通过：Java 17 全源码编译、T01–T42 主机合同、secret gate、AAPT2 包/版本核验、
DEX ABI 反编译门、zipalign、APK 签名证书连续性与 ZIP 完整性。总包中的离线复建脚本只使用随包
保存的实际工具链、R3.6.3 签名连续基线与完整源码重新生成 APK。

打包门会实际执行一次离线复建，并强制比较 `classes.dex`、`resources.arsc`、二进制 Manifest、
签名证书及包/版本身份。完整 APK 容器若因 ZIP/签名元数据而非逐字节相同，会如实分别记录两个
SHA-256，不把“载荷等价”伪装成“整包字节一致”。

Java 17 是构建语言版本，不代表仅支持 Android 17。APK 不携带 Java 源码、JDK、Android SDK 或
D8，因此其体积不能证明或否定源码完整性；源码、Git bundle、工具链与证据位于总包中。
