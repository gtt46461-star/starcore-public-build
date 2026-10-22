# R920 逐文件修改台账

精确机器可读字段（基线/当前行数、Git 增删、UTF-8 字符、字符差、字节、SHA-256）见总包
`R920_PATCH_ALL_CHANGED_FILE_METRICS.csv`。下表说明每个 R920 文件为什么存在、改了什么以及由哪道门验收。
短合同按职责而不是行数审计；空文件、无类型、TODO/FIXME 和无法解释的短具体 Owner 均为发布失败。

| 文件 | 当前规模 | 问题与实际修改 | 验收 |
| --- | ---: | --- | --- |
| `app/build.gradle` | 54 行 / 2,855 字符 | versionCode/versionName 升到 R920 | 身份门、APK badging |
| `AppConfig.java` | 6,216 / 319,060 | 统一运行修复版本身份 | 全量编译、静态身份门 |
| `BuildIdentity.java` | 449 / 28,405 | 统一构建身份 | 全量编译 |
| `GenesisAssetVmR915.java` | 424 / 20,618 | 删除 104 行通用原子轮换；生产路径改为具名语义原子 Owner | 语义原子门、VM 合同 |
| `GenesisAtomicWorldR920.java` | 797 / 42,046 | 新建 240 具名 PIT 原子物化：多窗频率、事件、转移/迁移、图路径、信息、记忆、风险、动作、运行和元研究；A9 赛前零值 | 240/240、1200 动作面、确定性/A9/多样性门 |
| `HistoricalStrategyInventionR908.java` | 369 / 23,092 | Profile 保存不可变、限窗的赛前历史供原子世界使用，不暴露未来数据 | PIT Profile、全量编译 |
| `MainActivity.java` | 9,376 / 382,271 | 修复启动绑定崩溃的第二道保护；R920 安全页；统一 R920 真值卡并保持 16 行创造卡 | 启动绑定 45/45、最终 Dex 安全页 |
| `ModelLearningActivity.java` | 173 / 7,124 | 深度学习/动态迭代/创造页统一读取 R920 快照 | 静态 UI/同 Owner 门 |
| `ReleaseIdentityR817.java` | 89 / 3,778 | R920 身份与时间戳 | 身份门 |
| `RuntimeTruthSnapshotR906.java` | 701 / 60,244 | R920 唯一快照；界面显示“具名语义物化=240/240、旧轮换已删除”；保留 16 状态框和完整创造详情 | 运行真值合同、最终 Dex 可见门 |
| `V51BuildManifest.java` | 359 / 23,717 | R920 版本与 code；性能证明仍 fail-closed | 身份/无虚假证明门 |
| `V57CompleteDiagnosticReportEngine.java` | 568 / 24,701 | 报告标题与稳定真值统一 R920 | 报告静态门 |
| `WhyTruthSectionRendererR906.java` | 54 / 2,303 | 为什么中/不中/不出手的版本与来源统一 | Renderer 6/6 绑定 |
| `strings.xml` | 72 / 4,069 | 可见产品名、R920 徽标、240 原子及唯一门文案 | 最终 APK 资源/可见身份门 |
| `release_identity.properties` | 10 / 229 | 唯一发布身份升级到 577240920 | 构建入口硬门 |
| `verification/r915/r915_source_substance_audit.py` | 314 / 12,549 | 审计输出支持版本化前缀，保持旧调用兼容 | Patch/Full 两份逐文件审计 |
| `scripts/r920_generate_r_java.py` | 146 / 5,426 | 从编译后 `public.xml` 原子生成唯一 R.java 和资源清单 | 275/275 资源 ID 门 |
| `scripts/r920_build_release.sh` | 233 / 12,171 | 资源先行→R 生成→523 Java→D8→组装→签名→反编译；禁止手工 R 发布 | 发布构建 PASS、R919 失败哨兵 |
| `scripts/r920_compile_host.sh` | 62 / 3,313 | Java 17 全编译与 9 组 JVM 合同入口 | 523 Java / 2,223 class |
| `scripts/r920_behavior_gate.sh` | 120 / 6,237 | 聚合 SQLite、结算、研究、语义原子、REAL 边界和源码实质性门 | `R920_BEHAVIOR_GATE=PASS` |
| `scripts/r920_apk_acceptance.sh` | 148 / 7,347 | 独立验收签名成品的包身份、资源、控件类型、Dex 接线、语义原子可见状态和安全页 | `R920_APK_ACCEPTANCE=PASS` |
| `verification/r920/R920RuntimeContractHarness.java` | 231 / 12,008 | 版本、240 原子、1200 动作面、A9、资产、七类结构变异、中文名、Future N=0、16 状态框 | JVM 运行合同 |
| `verification/r920/r920_apk_resource_contract.py` | 245 / 9,237 | ARSC↔Dex 全量资源等值、源码引用、45 首页绑定、6 行绑定和 launcher 三类 | 275/275、偏差 0 |
| `verification/r920/r920_static_contract.py` | 218 / 7,665 | 构建顺序、后台/结算接线、具名原子替换旧轮换、UI 和源码完整性 | 静态合同 |
| `R920_DELIVERY_README_ZH.md` | 51 / 1,827 | 总包、安装身份、边界与使用说明 | 文档人工复核 |
| `R920_ROOT_CAUSE_AND_LAUNCH_FIX_ZH.md` | 60 / 1,826 | R919 11 项资源漂移根因与两阶段构建图 | 失败证明、资源门 |
| `R920_R5.3.6_UPGRADE_PLAN_AND_IMPLEMENTATION_ZH.md` | 101 / 4,368 | V4.1 方案逐阶段映射、主链、原子/算法/证书边界 | 功能矩阵复核 |
| `R920_FEATURE_ASSET_INVENTORY_ZH.md` | 62 / 2,297 | 523 文件分类、关键 Owner、Legacy/Genesis/算法/APK 资产 | Source audit/APK evidence |
| `R920_PHYSICAL_DEVICE_ACCEPTANCE_ZH.md` | 59 / 1,583 | 覆盖安装、冷启动、200 期、后台五场景与证书复测 | 实体机待填写 |
| `R920_CHANGED_FILE_LEDGER_ZH.md` | 46 / 见最终 CSV | 把机器 CSV 转成逐文件人类说明 | 与最终 CSV/SHA 对照 |

## 全版本范围

相对 R914 基线 `fd1a850`，R915→R919→R920 共变更 109 个文件，其中 Java 66 个；523 个 Java
总计 402,989 行、16,361,413 个 UTF-8 字符。133 个锁定 Owner 中 131 个与基线逐字节相同，
`DecisionGateEngine` 与 `SettlementEngine` 是两项明示修复并重锁的唯一控制 Owner。完整逐文件
哈希和字符差保存在 Full 审计 CSV 中。
