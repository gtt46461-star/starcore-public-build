STARCORE_TIANSHU V57.16.4-R4.0.4-904
RUNTIME IDENTITY + UI LOAD CLOSURE

首选安装包：
APK/Starcore-Tianshu-V57.16.4-R4.0.4-904-release.apk

结论：
- 可直接覆盖安装 R903，不清数据库。
- package=com.ethk3.novaonepro
- versionCode=577200904
- versionName=V57.16.4-R4.0.4-904
- DB_VERSION=36，schemaRevision=3
- 新旧 APK 签名证书 SHA-256 完全一致。
- SHADOW_ONLY=true，MODEL_PROVEN=false，REAL_BET_HARD_DISABLED=true。

本次修复的直接故障：
R903 构建脚本只把 APK 外层版本改成 R903，但 DEX 里编译进去的 BuildConfig
仍是 R902。运行时因此稳定抛出 R819_BUILD_IDENTITY_MISMATCH，每期分析在计算前
终止。R904 已将身份改为单一属性源，并在发布构建中强制反编译 DEX 内部身份；
任一字段不一致都会直接阻断出包。

完整说明：SOURCE_COMPLETE/R904_RELEASE_NOTES.md
完整自检：VERIFICATION/R904_VALIDATION_REPORT.md
全包哈希：MANIFEST/SHA256SUMS.txt

