# V57.15.3-R3.6.16-819 总包说明

优先安装 `APK/Starcore-Tianshu-V57.15.3-R3.6.16-819-release.apk`。该 APK 与 R818 基线签名证书一致，可以覆盖升级；请勿卸载旧版。

总包包含：

- 已签名 APK；
- 完整修改后源代码与资源；
- Java 17 / Android 36 / D8 构建脚本；
- Host/性能/签名/ZipAlign/Manifest 验证证据；
- 工具链；
- SHA-256 清单；
- `R819_RUNTIME_CLOSURE_AUDIT_ZH.md` 根因、修改与设备复测边界。

安全边界：本包仍是 SHADOW_ONLY，REAL 不会因安装升级自动开启。强行停止应用后 Android 会暂停其后台组件，必须由用户再次打开；厂商后台与电池限制也需要在系统设置中放行。
