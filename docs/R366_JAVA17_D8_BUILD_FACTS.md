# R3.6.6 Java 17、Android 36 API 与 D8 构建事实

生产源码由 JDK 17 编译为 classfile major 61，类路径使用官方 Android 36 `android.jar`，再由 D8 9.2.23
以 `--release --min-api 21` 生成 DEX。构建门禁止 Java 7、legacy dx、源码内 API key、签名密码和私钥。

最终 APK 必须通过：

- T01–T45 主机合同与全源码 Java 17 编译；
- T45 真实 100号码/1200动作快照编码、解码与哈希一致性；
- n4 单一合同、旧 `n4 != 0` 反向条件禁用和字段化错误静态门；
- AAPT2 包名/versionCode/versionName；
- DEX 类与方法存在性、Android ABI、zipalign、签名证书连续性和 ZIP 完整性；
- 114/114 源码合同审计、逐文件 SHA-256 和总包离线复建等价。

Java 17 是构建语言级别，不是手机系统版本要求。APK 不包含源码、JDK、D8 或方案原件；完整总包中的
SOURCE、Git bundle、REPORT、EVIDENCE、TOOLCHAIN_USED 与 PRIVATE_SIGNING 才构成可审计、可复建交付。
