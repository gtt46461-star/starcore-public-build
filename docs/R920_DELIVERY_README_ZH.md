# STARCORE 天枢 V57.20.6 / R5.3.6 / R920 交付说明

## 先读结论

R919 发布 APK 被撤回：它的 Java/Dex 资源常量与最终 `resources.arsc` 不一致，覆盖安装后进入
`MainActivity.bindViews()` 时会把 `diagnostic_state_host` 的 `LinearLayout` 当成后台可靠性 `Button`，
从而发生启动期 `ClassCastException`。R920 没有继续修补错误常量，而是重建了发布流水线：先编译
资源、从编译后的 `public.xml` 生成唯一 `R.java`，再编译 523 个 Java，最后对成品 APK 的
Dex/ARSC 做全量等值验收。

R920 成品主机级验收结果：275/275 资源 ID 一致、45 个首页绑定和 6 个动态行绑定通过、28 个
关键运行类存在、签名证书连续、可覆盖安装、ZIP 对齐与完整性通过。实体 Android 设备冷启动、
厂商后台策略和自然未来期运行仍必须按随包清单复测，不能用主机验收冒充实体机证明。

## 总包目录

| 目录 | 内容 |
| --- | --- |
| `01_SIGNED_OVERWRITE_APK` | 同包名、同签名、递增 versionCode 的覆盖安装 APK |
| `02_SOURCE_COMPLETE` | R920 提交对应的完整 Git 源码快照 |
| `03_VERIFICATION_EVIDENCE` | 行为门、SQLite、Java 17、APK 反编译、资源一致性及逐文件审计证据 |
| `04_REPORTS_PLANS_AND_DOCS` | R920 说明、用户检测报告、原始《算法创造方案》及实现映射 |
| `05_GIT_REPOSITORY` | 可独立克隆的 Git bundle、提交/分支/标签说明 |
| `06_SIGNING_PRIVATE_USER_ONLY` | 用户原有发布签名材料，仅用于后续覆盖安装构建 |
| `07_ASSET_INVENTORY` | 编译资源、源码实质性、算法/原子/资产能力清单 |
| `08_CHECKSUMS` | 总包内部文件 SHA-256 与校验说明 |

## 安装身份

- applicationId：`com.ethk3.novaonepro`
- versionName：`V57.20.6-R5.3.6-920`
- versionCode：`577240920`
- 数据库：沿用原应用数据库与加法迁移，不以卸载清数据冒充修复
- 发布证书 SHA-256：`ae7e1b4a5a71f1f299d4a0b2472c6a36513873514e78190d42d6cfba3340116f`

## 安装与首轮复测

1. 不卸载旧版本，直接覆盖安装 `01_SIGNED_OVERWRITE_APK` 中的 R920 APK。
2. 首次打开应看到“星核天枢 R920”及“资源 ID 同源编译”字样，不应再立即退出。
3. 点开“R920 唯一运行真值总览”，四张卡应能展开；创造卡最多显示 16 行且不省略，详情页列出
   240 原子、算法中文机制名、Future OOS、证书和主链贡献。
4. 返回桌面、息屏、划走任务后，依照 `R920_PHYSICAL_DEVICE_ACCEPTANCE_ZH.md` 观察业务游标和逐期
   结算。若厂商明确禁止后台或用户关闭后台开关，界面必须显示具体原因，不得伪装为运行正常。
5. 当前没有有效研究证书时，创造层主链贡献应诚实显示 0；这不是接线失效，而是证据门在工作。

## 胜率与真实出手边界

代码已经实现“发现—冻结—未来 OOS—结算—失败向量—结构变异—淘汰/制证—有界贡献”，但任何
程序都不能事先保证随机或弱信号序列必然达到 70%–80%。R920 只允许被独立未来样本证明、同时
通过 Wilson、FDR、ECE、Brier、分块/Regime、覆盖、最大连挂和配对主链指标的候选进入证书层；
研究层没有直接创建 REAL 的权限，最终真实出手仍由唯一 `DecisionGateEngine_ONLY` 决定。
