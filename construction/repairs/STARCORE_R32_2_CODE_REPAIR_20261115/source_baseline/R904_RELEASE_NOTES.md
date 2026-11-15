# R904 深度修复说明

## 根因闭环

R903 的 APK 外层身份为 `V57.16.3-R4.0.3-903 / 577200903`，但发布脚本编译 DEX 时使用了仍写着 R902 的验证 stub：

- DEX `BuildConfig.VERSION_NAME=V57.16.2-R4.0.2-902`
- DEX `BuildConfig.VERSION_CODE=577200902`
- APK PackageManager 身份却是 R903

`ProductionPredictionDispatcherR815` 在每期分析入口执行 `ReleaseIdentityR817.assertBuildConfig()`，所以分析在模型计算前必然抛出 `R819_BUILD_IDENTITY_MISMATCH`。这与报告中的 `FAILED_TERMINAL`、`NEVER_COMMITTED`、预测期号停滞和覆盖率缺口完全一致。

## R904 实施

1. 发布身份单一真值
   - 版本升级为 `V57.16.4-R4.0.4-904 / 577200904`。
   - `release_identity.properties` 同时驱动 BuildConfig 生成、APK 外层版本和发布门。
   - 构建中对 D8 产物执行 baksmali，硬检查 DEX 内的 applicationId/versionName/versionCode。
   - 运行时异常带精确字段名，不再只输出一个不透明错误码。

2. 覆盖安装后自动恢复
   - R903 留下的 `FAILED_TERMINAL` 请求在 repairVersion 变更后可一次性 rearm。
   - 只允许目标未开奖、不存在 canonical、没有活动 owner 时重放。
   - 已开奖或已有 canonical 的冻结记录不被改写，保持 PIT/OOS 真实性。

3. UI 卡顿修复
   - 之前首页每 3 秒构造完整诊断快照，包含结算分布、候选榜、60 条公式目录和多次 SQLite 聚合。
   - R904 定时读取只生成四张摘要卡，完整详情只在用户点击后异步生成。
   - 摘要间隔调整为 15 秒，并在轻/重 UI 读取正在执行时让行。

## 不变红线

- `Package=com.ethk3.novaonepro`
- `DB_VERSION=36`, `schemaRevision=3`
- `Game=Binance1-PK10`
- `single-position | fixed-K3 | max-one-REAL-per-issue`
- `SHADOW_ONLY=true`
- `MODEL_PROVEN=false`
- `REAL_BET_HARD_DISABLED=true`
- 未引入 K4、dynamic K、multi-position REAL、chase、martingale、doubling 或 frequency debt。

## 真实性边界

Java 17、D8、宿主回归、APK 身份、DEX 内部身份、zipalign、v1/v2/v3 验签和新旧证书连续性已通过。静态构建不伪装成物理设备 24h 运行证明；安装后仍应执行 T29–T34 真机矩阵。

