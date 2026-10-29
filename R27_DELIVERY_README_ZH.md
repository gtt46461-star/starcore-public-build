# STARCORE R27 交付说明

- **分支**：`construction/r27-20261028`。从 `fix/r26-report-export-20261028@1713d114ebbd07ebd6544a29acfbce1bd883215c` 开出；未改 main，未删任何分支。
- **方案**：`construction/plans/STARCORE_R27_FINAL_20261028`，从 main@4b28696 原样复制。
- **版本**：R27-2026.10.28，versionCode **577271030**（基线为 577271029）。

## 1. 结论

### 四项签收（docs/12）

| 签收项 | 状态 |
|---|---|
| 工程完成（58卡全部“已修改”，HOST H01–H18 通过，构建身份一致） | **部分完成**。H01–H18 全部通过（本地 + CI）；构建身份一致。58 卡中 42 张“已修改待设备验证”，16 张仍为“未施工”，缺项见第4节。 |
| 能力完成（真机 24 小时） | **未执行**：本施工环境没有真机。 |
| 质量报告（R27-01..04 + 19 章） | 代码已接数据源，空数据只显示 INSUFFICIENT。真实报告须在设备上导出核对：**未执行**。 |
| 设备矩阵 + 可回滚到 1713d11 | 真机**未执行**。CI 模拟器（API 34）升级→回滚→再升级通过，见第3节。模拟器结果不计为设备验证。 |

### 主链规则未改

以下文件与 1713d11 **逐字节相同**：DecisionGateEngine、SettlementEngine、V51DecisionGateCore、CertificateBundleServiceR929、AppConfig、MainlineAdapterSU2、DbStore。H15 测试已核对 SHA256。规则不加严，也不放松：

- HR≥34% 且质量门通过才可 REAL；每期最多一次 REAL。
- 不设频率配额；禁止追期、马丁、加倍、补单、多位置真实、动态K。

R27 影子拿到 ACTIVE 证书后，只作为“意图”交给现有授权链，**没有新增任何 REAL 授权路径**。

## 1A. R27.4（第五份真机报告后的修改）

- 分析：`evidence/r27/device/R27_1029E_REPORT_ANALYSIS_ZH.md`。
- **全员逐期实盘观察**：每代种群前 24 个算法每期冻结、开奖结算；不建 e 过程、不授证、不进主链。
- 走势图表第一张改为“全部算法逐期实盘榜”，含每个算法的逐期成绩、历史回放训练期数、多重比较校正结论；旧 R908 榜改名并说明期数少的原因。
- R26 续算线程降为后台优先级，出号/抓取线程升为前台优先级（报告：出号 P95 25 秒，墙钟为 CPU 的 3～7 倍）。
- UI 主线程盈亏正则一次编译（报告：主线程卡 5.7 秒）。
- 主链规则、REAL 门、证书与留出门不变。
- APK：推送到本分支后由 R27 CI 用同一把缓存临时密钥签名，可直接覆盖 R27.2/R27.3 安装、不丢数据。下载链接以 CI 运行结果为准。

## 2. APK（R27.3）

| 项目 | 值 |
|---|---|
| 构建 | GitHub Actions run 40838143073，commit 7d3e0e2（R27.3 + R27.4，固定临时签名） |
| 下载 | https://nightly.link/gtt46461-star/starcore-public-build/actions/runs/40838143073/R27-APK.zip （免登录） |
| 备用（需登录 GitHub） | https://github.com/gtt46461-star/starcore-public-build/actions/runs/40838143073/artifacts/12882945646 |
| 安装文件 | `STARCORE-R27-2026.10.28-release.apk`（内置数据秘钥）；debug 包仅供排查 |
| release SHA256 | `f283b0f6aca4360c042bfc2d963b646bc69b93c52af933428dc8f971fbb02877` |
| debug SHA256 | `fae9fdde33bdffe4d007172513130f7b2eaed694e45801a8ec06b58b2f936bce` |
| 包名 / versionCode / versionName | com.starcore.research.r13 / 577271030 / R27-2026.10.28 |
| 签名证书 SHA-256 | `d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7` —— **CI 临时密钥，不是正式 R13 签名** |

**如实说明**

1. 从 R27.2 起，临时密钥只生成一次并保存在 Actions 缓存里。之后的 R27 新版签名相同，可以**直接覆盖安装、不丢数据**。从更早的包换过来时需要先卸载一次。
2. 正式签名私钥在 `construction/su1-20261022` 分支的 Actions 缓存里，本分支读不到。
3. 本包包含的修复：
   - R27.1：研究存储迁移加速；R27 写入降为后台级；抓取回执重试；续算切片 200 毫秒。
   - R27.2：写锁忙不再隔离任务；研究线程后台优先级；主链运行期间研究只留 1 个线程；迁移片内重试；走势图表防闪退；R27 工具弹窗防闪退。
4. 分析文档：`evidence/r27/device/R27_1029_REPORT_ANALYSIS_ZH.md`、`evidence/r27/device/R27_1029B_REPORT_ANALYSIS_ZH.md`。

## 3. 测试摘要

| 测试 | 结果 | 证据 |
|---|---|---|
| H01 泄漏（23 叶子 × 138 检查，240 原子） | 通过 | evidence/r27/host/junit_r27_local.log |
| H02/H03 回放逐位一致、缺口切片 | 通过 | 同上 |
| H04/H05 出生门枚举、无效变异 | 通过 | 同上 |
| H06 植入信号 → VERIFIED（留出 HR 63.9%） | 通过 | 同上 |
| H07 无信号 100 次 → VERIFIED 率 0% | 通过 | 同上 |
| H08 1000 个零策略 × 20000 期，错误授证 0 | 通过 | 同上 |
| H09 HR35% 中位停时 7676 期（≤10000） | 通过 | 同上 |
| H10/H11 语义 ABI：只改界面全部 EQUIVALENT；改 fdecay 只有 2 个相关策略 CHANGED，首差正确 | 通过 | 同上 |
| H12 进化 18 步逐步杀进程恢复，事件完全一致 | 通过 | 同上 |
| H13 校准注入 5% 迟到行，逐行剔除不整批作废 | 通过 | 同上 |
| H14 100 面 / 1200 动作，最大误差 2.8e-14 | 通过 | 同上 |
| H15 主链红线（Java + SQL：无证书 / 过期 / 吊销 / 同期第二次 / 冻结改写 / 结算后追单 全部拒绝） | 通过 | 同上 + evidence/r27/host/migration.log |
| H16 AtomNet：植入信号带下界>0；无信号带含0（3 种子） | 通过 | junit 日志 |
| H17 空数据报告全部 INSUFFICIENT，无 PASS | 通过 | 同上 |
| H18 零假设脚本复现发表结果（431 项，最大差 0） | 通过 | evidence/r27/host/h18_compare.log |
| R27-056 Java 同构（A ±0.47pp，D 完全一致） | 通过 | junit 日志 |
| 迁移 M1–M6（1713d11 形态数据库增量升级、无数据丢失、可重复、七计数） | 通过 | migration.log |
| 仓库原有套件 verify_strategy_su1 / verify_plan1025 / verify_r26、全部 testDebugUnitTest、lintRelease | 通过（CI） | CI run 40758444513 |
| 模拟器 API 34：1713d11 → R27 → 回滚 1713d11 → R27 | 通过 U1–U4（不删表、行数不减、迁移记录、回滚后表保留且 integrity ok、证据保留），全程无 FATAL/ANR。模拟器无 API 凭据，开奖数据为 0，研究表为空（INSUFFICIENT）。 | CI 工件 `R27-emulator-api34`（artifact 12848859569） |

本地共 31 个 R27 JUnit 测试 `OK (31 tests)`，用时 428 秒。

## 4. 未完成项与原因（如实）

16 张卡仍为“未施工”，每张的缺项写在 `evidence/r27/<卡>/card.json` 和注册表的 `construction_note` 中。

| 卡 | 状态与原因 |
|---|---|
| R27-003 / 004 / 009 | 代码已提交（线程池、写队列、启动/停止顺序）。行为需要 Android SQLite 与真机测量；HOST 无法测试。 |
| R27-012 | 语义 ABI 已接入 LegacyReplayR26；BLOCKED_NUMERICAL_ABI_PRESERVED=0 须在有 R26 数据的设备上升级后观察。R26 live_model 身份仍含源码ABI，未改。 |
| R27-018 / 019 / 048 | 代码已提交，无 HOST 测试。R908 基因组是有状态程序，全部记为 LEGACY_NOT_REPRODUCIBLE。 |
| R27-020 | 旧学习器/R943 网络权重形状与 AtomNetR27 不同，**没有做热启动**。验收“导入后立即有回放成绩”未达成。 |
| R27-039 | 只实现了 R27 冠军消融；R26 反馈/审计任务未改。 |
| R27-046 / 047 | 退役开关已加，观察期未运行；数据库归档压缩**未实施**（门控在旧链退役之后）。 |
| R27-049 / 050 / 053 | 图表与 19 章数据源已接；首屏耗时、24 小时 READY、真实报告须真机核对。 |
| R27-057 | 真机设备矩阵**未执行**（本环境没有真机）。 |
| R27-058 | 没有正式签名包（见第2节）；真机覆盖安装与回滚未执行。回滚方案见 `release/R27_ROLLBACK_PLAN.md`。 |

### 其他偏差与需要您决定的事项

- **思路族文件位置**：14 个思路族文件平铺在 `strategyv2/`，方案写的是 `idea/` 子目录。功能等价。
- **AtomNet 训练入口**：`AtomicToolWorldR943` 未改为调用 AtomNetR27。旧网络并行保留；R27 训练在 `RuntimeR27` 的 atomnet 任务中。
- **原子评分口径（R27-043，需您决定）**：
  - 按 docs/07“上界<0 即 HARMFUL”，纯噪声原子因 5 分箱估计方差常被判 HARMFUL，而不是 NEUTRAL。
  - 这只影响生成器权重（×0.25），不影响资格。
  - 未改口径。是否改为与打乱对照配对比较，需要您决定。
- **AtomNet 样本量**：HOST 实测，只用 3000 条训练样本时，无信号数据上有 4/12 次显著差于基线（过拟合）。按方案的样本量（约 5 万）为 12/12 含 0。这说明历史预训练的样本量很重要。
- **提交粒度**：提交按 4 组完成（方案 / 纯计算核心 / Android 接入 / 身份与证据），未按 8 个批次逐一提交，因为 Android 接入文件被多个批次共用。每个提交都单独做了 javac 编译检查；完整 Gradle 构建只在最终提交上执行。

## 5. 复现命令

```
./gradlew :app:testDebugUnitTest                      # H01–H17、R27-056 及全部原有单测
CLASSES=<javac classes> ANDROID_JAR=<android.jar> tools/r27/run_host_tests.sh   # H18、迁移、H15 SQL
python3 tools/r27/make_evidence.py                     # 重新生成 evidence/r27 与卡片状态
```

**质量结论**由安装后的报告 R27-01 章按数据自动选择模板。“未发现在 FUTURE 上显著优于随机的策略”是合法结论。本交付不预设、不手写任何质量结论。
