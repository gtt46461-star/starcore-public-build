# 星核天枢 V57.15.3-R3.6.4 先读说明

## 交付身份

- 包名：`com.ethk3.novaonepro`
- 修订：`V57.15.3-R3.6.4`
- versionCode：`577180816`
- versionName：`V57.15.3-tianshu-b01-b15-runtime-closure-hf4-20260811`
- 数据库：`binance_k3_nova_one_pro_v57.db`，`DB_VERSION=36`
- Java：源码/目标 17，classfile major 61
- DEX：D8 9.2.23，minSdk 21，legacy dx=0
- 姿态：`SHADOW_ONLY=true`、`MODEL_PROVEN=false`、REAL 硬锁

## R3.6.3 报告的真实含义

R3.6.3 已经证明数据库启动修复生效：`databaseStartup=critical=READY`、Service
`truth=RUNNING`、typed reads 全部为 `OK`、完整报告成功持久化。剩余故障发生在
`FETCH_SUCCEEDED` 之后：分析尝试取得唯一 Owner，但 canonical、100 号码和 1200 动作都没有写入。

代码复现确认了两个上游阻断点：

1. `V51RuntimeInputR36` 用带前导零且无尾逗号的号码文本计算历史哈希，B04
   `V51PermutationFeatureEngine` 用无前导零且带尾逗号的排列计算哈希，同一批历史必然
   `HISTORY_HASH_MISMATCH`。
2. 最新接口默认仅取 5 期，而运行图最低需要 12 期。R36 唯一链绕过旧历史补齐后，新安装会持续
   `V51_HISTORY_LT_12`。240 从来不应是最低门槛，它只是冻结输入上限。

R3.6.4 将历史哈希统一交给一个 Owner；少于 12 期时在唯一分析 HandlerThread 上自动请求并保存
100 期历史，再冻结最多 240 期。目标期若在补齐期间已经开奖，立即终止旧请求，禁止开奖后重算。
覆盖安装时，旧版已写入的 `HISTORY_HASH_MISMATCH`、`V51_HISTORY_LT_12` 或
`R36_HISTORY_HASH_CONFLICT` 请求会在目标仍未开奖且无活跃 Owner 时仅重置一次；其他错误、已开奖目标
和已提交 canonical 均不会被重置。

## 安装

要保留数据库请直接覆盖安装，不要先卸载：

```bash
adb install -r Starcore-Tianshu-V57.15.3-R3.6.4-release.apk
```

从总包安装时，解压后安装 `APK/Starcore-Tianshu-V57.15.3-R3.6.4-release.apk`。单独 APK
下载链接只是方便，文件内容与总包内 APK 应具有相同 SHA-256。

## 安装后应观察到

首次新安装可短暂出现 `R3.6.4新安装历史补齐`。随后下一目标期应依次出现：

1. `analysisStage=RUNNING`；
2. 运行账本 `lastAttempt=ANALYSIS_CANONICAL_COMMITTED`；
3. canonical issue=1、number=100、full=1200、champion=1；
4. 下一期开奖后生成严格 OOS 标签。

失败时报告必须直接包含 `analysisDetail` 与 `attemptDetail`，不得再只显示
`NEVER_COMMITTED`。物理设备屏幕开关、划掉任务、杀进程、网络切换和 24 小时运行仍需实机复测。

## 真实性边界

工程修复不能伪造模型训练或命中率。模型 Artifact 仍未证明，严格 OOS 样本不足 120，REAL
保持永久显式锁定，禁止追期、马丁、倍投、频率债务、动态 K、多位置 REAL。

APK 大小是压缩 DEX/资源的大小，不等于源码数量。总包包含完整源码、Git 历史、实际工具链、基线、
方案与逐文件哈希，禁止用随机字符、占位库或无效资源填充体积。
