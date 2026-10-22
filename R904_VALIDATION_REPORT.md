# R904 严格自检报告

## 发布身份

- package: `com.ethk3.novaonepro`
- versionName: `V57.16.4-R4.0.4-904`
- versionCode: `577200904`
- DB: `36`
- APK SHA-256: `1ee0f1d60a65d497a60015d18240118e832c14b8397a226063ac67c384f406a4`
- 签名证书 SHA-256: `ae7e1b4a5a71f1f299d4a0b2472c6a36513873514e78190d42d6cfba3340116f`

## 自动验证

- 全量 Java 17 编译：PASS（475 个应用 Java 文件）。
- D8 release DEX：PASS。
- R904 专项行为门：PASS。
- R36 host contracts：PASS。
- R811 host contracts：PASS（666 assertions）。
- R815 host contracts：PASS（131 assertions）。
- R816 runtime closure：PASS（14 assertions）。
- R817 runtime/research closure：PASS（486 assertions）。
- 旧 R903 `FAILED_TERMINAL` 升级重置正向/防循环测试：PASS。
- 定时摘要/点击详情分离门：PASS。
- APK 外层身份：PASS。
- 签名 APK 反编译后 DEX `BuildConfig` 身份：PASS。
- DEX `ReleaseIdentityR817` 身份和 typed mismatch 路径：PASS。
- 旧 R902 DEX 身份残留扫描：PASS（0 命中）。
- zipalign：PASS。
- APK ZIP 完整性：PASS。
- APK v1/v2/v3 验签：PASS。
- R903 → R904 证书连续性：PASS，可覆盖安装。
- REAL 安全边界：PASS，仍为 SHADOW_ONLY。

## 安装后验收点

1. 新目标期不再出现 `R819_BUILD_IDENTITY_MISMATCH`。
2. durable request 从旧 repairVersion 终态一次性进入 `REQUESTED/RUNNING`，随后达到 `ANALYSIS_CANONICAL_COMMITTED`。
3. `latestCanonical` 跟随新开奖向前推进；新期产生固定 K3 SHADOW 号码。
4. 首页不再每 3 秒组装完整研究公式文本；点击卡片仍可打开完整真值。
5. 继续保持 `REAL=0`，直到独立模型与物理运行证书真实通过。

物理设备 T29–T34/24h soak 需安装后执行，本报告不把静态编译冒充真机证明。

