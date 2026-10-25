# 星核创研 R13 · 公有独立版

公有仓库的独立 Android 应用。包名 `com.starcore.research.r13`，显示名称“星核创研 R13”，版本 `R13-5.4.3.04-2026.10.25`，versionCode `577251063`。使用独立固定签名，可与私有原版 `com.ethk3.novaonepro` 及旧公有 SU1 同时安装；数据分别保存在各自应用目录。

本次 5.4.3.04 按 10.25 的 50 项方案升级至 R16 引擎，补齐阶段窗口、因子/原因/教师矩阵和 P50 升级诊断书。使用原 R13 固定独立签名，可覆盖更新既有 R13 并保留数据。交付为 Release APK。[本次施工与验收记录](construction/plan1025/README.md) · [最终交付回执](construction/plan1025/delivery.json)。已有的进程启动恢复修复继续保留。

## 本轮来源与升级

- 私有成功发布的 5.4.3.00 源码：`f29ab95633b966e3a27906f99abba404d134f479`。
- 私有最新产品分支 `ccr-18baf6f2-no1yow`：`aae63bc692a1ca602fe4db7aacf87958d5f3fbd7`，源码版本已到 5.4.3.01。
- 保留公有 SU1 升级基线 `0f1fb606a2e49f5065ff46c02c46917216eea02e`，补齐最新图表、研究可视化和训练相关源码，再接入用户提供的 R13 35项施工方案及10个补丁。
- 对私有最新 `app/src` 的1063个文件逐项核对，零缺失。相同文件保持原字节；合并文件保留 SU1/R13 接线和公有独立身份。

[源码核对清单](construction/r13/source-reconciliation.json) · [施工说明与修复记录](construction/r13/README.md)。根目录 `SOURCE_COPY_MANIFEST.json` 记录的是此前 C024 首次导入，不能代替本轮最新核对清单。

## 已接入的功能

走势图表入口及14类图表；六类在线模型与持续训练；策略进化、频率分层、联盟主链选择；开奖前冻结和开奖后结算；证书与共享检验预算；存储统计、报告维护和研究控制页面。

图表读取已提交数据，缺少样本时说明原因。REAL 授权默认关闭，有效证书、当期冻结和唯一主链校验仍逐期生效。编译、模拟与自动测试不构成真实预测有效性或未来收益证明。

## 构建与验证

当前构建流程为 `.github/workflows/r13.yml`。在 Actions 的 **R13 restored latest APK** 中选择 `construction/su1-20261022` 分支，使用经认证加密的固定 R13 签名包及该分支的配置传输密钥缓存；不使用私有原版签名。

工具链：JDK17、Android SDK36、Build Tools36.0.0、NDK28.2.13676358、CMake3.22.1、Python3与NumPy2.3.5。

本地构建需要配置 Android SDK 路径，并从私有签名备份设置 `R13_SIGNING_P12` 与 `R13_SIGNING_PASSWORD`；有效接口配置通过本地环境变量 `DUOQU_API_KEY` 提供，凭据不得提交到源码。

```sh
python3 scripts/prepare_strategy_su1_identity.py
python3 scripts/test_r13_production_ddl.py
python3 scripts/generate_strategy_su2_schema.py --check
python3 scripts/test_plan1025_storage.py
python3 scripts/verify_plan1025.py
python3 scripts/verify_strategy_su1.py
./gradlew --no-daemon :app:verifyCanonicalPlanBuildCP3 :app:assembleRelease :app:assembleDebug :app:assembleDebugAndroidTest
```

自动检查覆盖生产 Java/Python/SQLite、Android单测与lint、APK签名和包名、模拟器安装、真实接口请求及图表页面。此前 5.4.3.03 的72项单测、21组生产主机检查、26项Android模拟器检查（Android 10、双核2 GB、测试类分别运行）及lint通过；另在 Android 10、15、16 上分别验证默认启用服务时的冷启动和强制停止后重启，每阶段检查新进程初始化及持续60秒的心跳。[构建与运行记录](https://github.com/gtt46461-star/starcore-public-build/actions/runs/40080151500)。[Android 10 分组测试](https://github.com/gtt46461-star/starcore-public-build/actions/runs/40082044526)。单核2 GB模拟器的高强度计算测试仍有系统低内存终止记录，详见启动修复说明；该限制不计为测试通过。此前版本的交付记录见 [delivery.json](construction/r13/delivery.json)；10.25 的最终状态见本次施工目录；用户真实设备持续运行和自然前推仍须单独验证。

私有 Git 历史、私钥和明文凭据未迁入本仓库。
