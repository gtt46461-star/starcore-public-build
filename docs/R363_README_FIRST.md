# 星核天枢 V57.15.3-R3.6.3 先读说明

## 交付身份

- 包名：`com.ethk3.novaonepro`
- 修订：`V57.15.3-R3.6.3`
- versionCode：`577180815`
- versionName：`V57.15.3-tianshu-b01-b15-runtime-closure-hf3-20260811`
- 数据库：`binance_k3_nova_one_pro_v57.db`，`DB_VERSION=36`
- Java：源码/目标 17，classfile major 61
- DEX：D8 9.2.23，minSdk 21，legacy dx=0
- 当前发布姿态：`SHADOW_ONLY=true`、`MODEL_PROVEN=false`、REAL 硬锁

## R3.6.2 现场报告证明了什么

报告中的 `fg` 与 `cmd` 都有时间戳，证明 Android 已执行 Service 前台入口和
`onStartCommand`。`startAt=0`、`actualStart=0` 与全部 `STORE_NULL` 则证明旧实现把
“业务就绪”计数放在超重数据库/引擎初始化之后；它不是 APK 空壳，也不是 Service
根本没有被系统调用。与此同时，Receiver 又用 `OK:start_evaluated` 覆盖了真正的启动
结果，导致报告丢失精确失败阶段。

R3.6.3 对这条证据逐项修复：

1. Service `onCreate` 进入、前台通知、关键数据库、引擎图、业务 READY 分层记账。
2. DB35→DB36 走纯增量 R35/R36/V51/B03 Schema，不重复执行历史 V50 全量迁移/完整性扫描。
3. `DbStore` 构造只关闭关键 R36 Schema；历史学习/画像 Schema 按需单飞。
4. BuildIdentity 模块登记只确保自己的小表，不再暗中触发整套历史 Link Schema。
5. 新增列后只增量更新该表列缓存，不再每列清空全库缓存、反复 `PRAGMA table_info`。
6. 初始化失败保留具体 stage、因果链和重试次数；重试前清理半初始化 HandlerThread。
7. 完整报告在后台取得真实 `DbStore` 后生成，不再把“UI 尚未拿到 Store”直接复制为
   `READ_ERROR:STORE_NULL` 总报告。

## 安装

先备份应用数据，然后直接覆盖安装；要保留旧数据库，不要先卸载：

```bash
adb install -r Starcore-Tianshu-V57.15.3-R3.6.3-release.apk
```

首次覆盖安装可能执行一次增量 Schema。通知栏出现前台服务后，等待报告中的
`truth=RUNNING`、`init=READY/stage=BUSINESS_READY` 再判断业务闭环。初始化期间应显示
`truth=STARTING` 与精确阶段，不得再显示 `SERVICE_NEVER_STARTED`。

## 真实性边界

源码编译、主机合同、DEX ABI、签名和离线复建通过，只能证明这一个 APK 的工程事实。
屏幕开/关、划掉任务、进程杀死、网络切换与 24 小时运行必须在物理设备重新验证。
模型仍未训练/未证明，任何工程通过、文件数量或 APK 大小都不能解锁 REAL。

## APK 大小

APK 是压缩后的 DEX/资源/签名容器，不包含源码、Git 历史、Android SDK、D8、APKTool、
证据日志和旧方案包。数百个 Java 文件经 javac+D8 合并后得到数 MB APK 是正常结果。
完整总包包含全部源码、历史 bundle、实际工具链、基线 APK、方案和哈希证据，体积会远大于 APK。
本交付禁止用随机字符、无效资源或假库填充体积。
