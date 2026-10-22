# 星核·天枢 V57.15.3-R3.6.9-812 检测报告后修复版交付说明

本交付是 R3.6.7 基线上的 R811 FINAL LOCKED 运行返修与 Research 隔离升级。

## 固定身份

- applicationId：`com.ethk3.novaonepro`
- versionCode：`577190821`
- versionName：`V57.15.3-R3.6.9-812`
- compileSdk：36；targetSdk：28；minSdk：21
- 数据库：DB36，R811 same-version schema revision 1
- Production 策略 cohort：保持 R3.6.7 冻结值，防止策略哈希漂移
- 模型状态：`MODEL_PROVEN=false`
- 运行车道：`SHADOW_ONLY=true`
- REAL：硬锁，Research 无 Production/REAL/Settlement/OOS owner 权限

## 已验证

- 420 个 Java 源文件、374,904 行，官方 Android 36 API 全量编译通过
- 1,815 个 class，Java 17 classfile major 61
- 旧 R36 主机回归通过；R811 主机契约 170 条断言通过
- R3.6.7 与 R811 的 100 组、每组 11 字段策略黄金样本逐字节一致
- 165 行文件处置清单审计通过：53 MODIFY、29 ADD、60 FREEZE、2 P2 optional
- T01–T210 验收矩阵完整：166 项具备工程 PASS 证据、7 项按锁定方案为 P2 不适用、37 项等待真机/SQLite 故障注入/未来 OOS 或 24h 证据
- APK 的 D8、zipalign、ZIP、aapt2 identity、min/target SDK、ABI 和签名验证通过
- 签名证书与 R3.6.7 连续，APK 支持 v1/v2/v3 签名验证
- APK SHA-256：`1312d9f1c28848acfa45c546afd36b27d2228934ad8594c94eec03166616a253`
- 源码树未包含 API 密钥、签名口令或私钥文件

## 诚实状态边界

当前环境没有 Android 真机，也没有用户的真实 R3.6.7 运行数据库。因此 T29–T34
物理矩阵、T46 真实旧库覆盖安装、24 小时 soak、实时 P95/DB lock P95、连续 30/120
期 Coverage 与 OOS 证据不能被伪造为 PASS，均在验收矩阵中明确标注
`NOT_RUN_NO_DEVICE` 或 `PARTIAL_CODE_PROVEN_DEVICE_PENDING`。

这不影响本轮 P0/P1 工程施工完成，但意味着：本包只能标记为 `Engineering Candidate`；
工程完成不等于模型统计证明完成，也不等于真机物理运行证明完成。APK 会继续保持
SHADOW/REAL 硬锁，不能标记为 `MODEL_PROVEN`。

## 离线重建

```sh
R811_EVIDENCE_DIR="$PWD/build/r811-verification/evidence" ./scripts/r811_behavior_gate.sh
./scripts/r811_disposition_audit.sh /path/to/04_FULL_FILE_DISPOSITION_MANIFEST_FINAL.csv
./scripts/r811_acceptance_matrix_generate.py --plan-root /path/to/locked-plan --output docs/R811_ACCEPTANCE_MATRIX.csv
./scripts/r811_build_release.sh /path/to/R3.6.7-release.apk /path/to/output.apk /path/to/signing-kit.zip /path/to/reference.apk
```

构建需要 JDK 17、官方 API-36 `android.jar`、R8/D8 9.2.23、apktool、aapt2、zipalign、
apksigner 和原签名凭据。交付总包只包含可公开的工具链与签名证书摘要，不包含私钥或口令。
