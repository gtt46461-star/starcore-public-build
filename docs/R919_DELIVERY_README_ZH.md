# STARCORE 天枢 R919 / R5.3.5 最终交付说明

## 交付身份

- applicationId：`com.ethk3.novaonepro`
- versionName：`V57.20.5-R5.3.5-919`
- versionCode：`577240919`
- 数据库：`binance_k3_nova_one_pro_v57.db`，`DB_VERSION=36`
- APK：`Starcore-Tianshu-V57.20.5-R5.3.5-919-release.apk`
- 覆盖安装：APK 与 R915 使用同一签名证书；不要卸载旧版，直接安装可保留应用数据库。

## 总包目录

1. `01_SIGNED_APK`：签名、zipalign、ZIP 完整性和关键 DEX 类验收通过的覆盖安装 APK。
2. `02_SOURCE_COMPLETE`：本次 Git 提交的完整可追溯源码快照。
3. `03_VERIFICATION_EVIDENCE`：编译、行为、SQLite、源码实质、APK 与签名连续性证据。
4. `04_REPORTS_AND_DOCS`：最新实机报告、算法创造原方案、R919 诊断、架构、验收合同和复测说明。
5. `05_GIT_REPOSITORY`：包含分支与标签的 Git bundle；可在没有原仓库时完整克隆。
6. `06_SIGNING_PRIVATE_USER_ONLY`：用户明确要求随覆盖安装版本保留的原签名私钥工具包。该目录只能私有保存，禁止公开上传。
7. `07_CHECKSUMS`：总包内部文件的 SHA-256 校验清单。

## 安装与验证

1. 先备份旧应用数据；不要卸载旧应用。
2. 安装 `01_SIGNED_APK` 中 APK。Android 若提示来源限制，给当前文件管理器临时授权。
3. 首次打开后进入后台可靠性设置，允许通知、精确闹钟、忽略电池优化与后台自启动。
4. 保持界面前台 5 期，确认开奖、预测、结算逐期前进。
5. 退出界面并息屏，按 `R919_PHYSICAL_DEVICE_RETEST_ZH.md` 连续测试至少 120 期。
6. 复制完整检测报告，对照：预测结算、Teacher 反哺、240 原子合同、算法/资产 Future-OOS、中文机制名、贡献门和 `PhysicalRuntime`。

## 结果边界

- R919 已实现失败向量驱动的结构创造、严格未来 OOS、退化淘汰/回滚、240 原子时间阶段与唯一主链受控贡献。
- 当前样本中没有通过高胜率生产证书的算法，因此研究贡献保持 0、原主链接管；这属于真实性保护，不是功能未接入。
- 代码不能保证随机开奖达到 70–80% 胜率，也不能承诺每一代样本胜率单调上升。能保证的是：不以回放冒充未来、不继承父代成绩、不允许退化候选进入主链。
- 主机与模拟数据库不能证明特定手机厂商在退界面/息屏后持续调度。物理机后台结果在完成 120 期复测前必须保持 `RETEST_REQUIRED`。

## Git 恢复

可用总包中的 bundle 克隆完整提交与标签：

```bash
git clone STARCORE_TIANSHU_V57.20.5_R5.3.5_919.bundle STARCORE_R919
cd STARCORE_R919
git checkout agent/starcore-r919-r5.3.5-truth-closure-20260831
```

逐文件行数、字符数、增删量、SHA-256 与空壳审计以 `03_VERIFICATION_EVIDENCE/source-audit` 中 CSV 为准。
