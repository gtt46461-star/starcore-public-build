# 星核天枢 V57.15.3-R3.6.1 先读说明

## 本次最新版本

- 产品修订：`V57.15.3-R3.6.1`
- Android 包名：`com.ethk3.novaonepro`
- versionCode：`577180813`
- versionName：`V57.15.3-tianshu-b01-b15-runtime-closure-hf1-20260811`
- 数据库：`binance_k3_nova_one_pro_v57.db`，DB_VERSION=36
- Java：Gradle 源码/目标兼容级别 17；JDK 17 全源码编译门通过
- 发布姿态：`SHADOW_ONLY`
- 模型状态：`NOT_PROVEN_UNTRAINED`
- REAL 自动解锁：关闭

这不是上一份 R3.6 APK 的重复打包。R3.6.1 提升了 versionCode，并修掉了上一包会让 SQLiteOpenHelper 在 Service 启动阶段直接崩溃的 Android 框架 ABI 错编译问题。

## 应安装哪个文件

安装 `APK/Starcore-Tianshu-V57.15.3-R3.6.1-release.apk`。

建议先备份数据库，然后直接覆盖安装旧版，不能先卸载；覆盖安装才能保留应用私有目录中的数据库。APK 与上一版证书 SHA-256 一致，且 versionCode 更高，满足原签名升级条件。

ADB 安装示例：

```bash
adb install -r Starcore-Tianshu-V57.15.3-R3.6.1-release.apk
```

如果系统仍显示旧版，先确认手机上包名确实是 `com.ethk3.novaonepro`，再用 `adb shell dumpsys package com.ethk3.novaonepro` 检查 versionCode 是否为 `577180813`。

## 安装后先看什么

首次启动后打开“一键复制完整检测报告”，确认：

1. 报告版本为 R3.6.1，versionCode 为 577180813。
2. 不再出现 `SQLiteOpenHelper(Context,String,Object,int)` 或 `No direct method <init>`。
3. Service 有实际启动时间，`actualStart` 不再长期为 0。
4. 数据库读取不再统一显示 `STORE_NULL`。
5. 运行样本不足时必须显示 `RETEST_REQUIRED/NO_DATA/NOT_PROVEN`，不得假装为 0/PASS。
6. 报告末尾包含完整 SHA-256 和结束标志。

## 已证明与未证明的边界

已证明的是源码集成、Java 17 全量编译、主机行为回放、APK 构建、包身份、签名连续性、DEX Android ABI、固定 K3 与 1200 动作合同、B14/B15 报告完整性。

尚未证明的是这台具体手机上的亮屏/熄屏 120 分钟、任务划掉、进程杀死、主备网络、24 小时浸泡，以及模型严格 OOS 效果。因此新包仍保持 SHADOW_ONLY。安装后的物理设备报告是关闭这些项目的唯一证据。

## 总包结构

- `APK/`：正式升级 APK 与 APK SHA-256。
- `SOURCE/`：完整源码 ZIP 和 Git history bundle。
- `EVIDENCE/`：Java 17、行为门、DEX ABI、签名、包身份、114 文件合同和逐文件 SHA-256。
- `PLANS/`：用户提供的 B01–B15、旧完整源码和 R3.6 返修方案原件。
- `TOOLCHAIN_USED/`：本次真实使用的离线构建、反编译、签名、格式化工具。
- `PRIVATE_SIGNING/`：延续旧版升级签名的私密材料，只能由应用所有者保管，禁止公开。
- `REPORT/`：根因、体积、Java 17、B01–B15、实机复测和交付清单。

总包体积来自完整源码、历史、三份原方案、旧版基准 APK、离线工具和证据；没有为了接近某个数字而填充垃圾文件。
