# 星核天枢 V57.15.3-R3.6.2 先读说明

## 最终升级身份

- 修订：`V57.15.3-R3.6.2`
- 包名：`com.ethk3.novaonepro`
- versionCode：`577180814`
- versionName：`V57.15.3-tianshu-b01-b15-runtime-closure-hf2-20260811`
- 数据库：`binance_k3_nova_one_pro_v57.db`，`DB_VERSION=36`
- 构建：OpenJDK 17、Java 17 classfile major 61、官方 Android 36 API、D8 9.2.23
- minSdk/targetSdk：21/28
- 发布姿态：`SHADOW_ONLY`
- 模型状态：`NOT_PROVEN_UNTRAINED`
- REAL 自动解锁：关闭

R3.6.2 保留 R3.6.1 的全部业务修复，并替换最后一个旧兼容构建环节。生产源码不再使用 Java 7 classfile 或 legacy dx；Android 框架签名也不再由本地手写 stub 决定。

## 本次关闭的 P0 根因

R3.6 APK 的 `DbHelper` 源码本身调用了正确的四参数 `SQLiteOpenHelper` 构造器，但离线编译 stub 把第三参数错误声明为 `Object`。错误描述符进入 DEX 后，真机找不到该构造器，导致 Service 启动失败、`DbStore` 为空，并级联形成整份报告中的 `STORE_NULL/READ_ERROR`。

R3.6.2 的正式 APK 直接以官方 Android 36 `android.jar` 编译 387 个生产 Java 文件，再由 D8 9.2.23 把 Java 17 classfile 转为 minSdk 21 DEX。最终 APK 还会经过反编译 ABI 禁止签名门，因此同类错误会在交付前失败，而不会再留到手机上发现。

## 安装

安装 `APK/Starcore-Tianshu-V57.15.3-R3.6.2-release.apk`。先备份数据库，然后覆盖安装；如需保留应用私有目录中的旧数据库，不要先卸载。

```bash
adb install -r Starcore-Tianshu-V57.15.3-R3.6.2-release.apk
```

APK 使用与上一版相同的发布证书，且 versionCode 更高，满足签名升级条件。

## 安装后验收

首次启动后复制完整检测报告，至少核对：

1. 版本为 R3.6.2、versionCode 为 577180814。
2. 不再出现 `SQLiteOpenHelper(Context,String,Object,int)` 或 `No direct method <init>`。
3. `actualStart`、fetch、analysis、settlement 出现真实时间，而不是永久为 0。
4. 数据库读取不再统一为 `STORE_NULL`。
5. 小样本或无样本必须保持 `RETEST_REQUIRED/NO_DATA/NOT_PROVEN`，不得显示伪 0/PASS。
6. 报告末尾存在完整 SHA-256 与结束标记。

## 证明边界

已通过的是源码完整性、B01–B15 114/114 合同追踪、Java 17 全源码编译、主机行为回放、D8 构建、APK 包身份、签名连续性、zipalign、DEX Android ABI、固定 K3/1200 动作和完整报告门。

静态构建不能替代手机上的 T29–T34：亮屏/熄屏 120 分钟、任务划掉、进程杀死、主备网络和 24 小时浸泡仍须重测。模型也没有严格 OOS 证明，因此 REAL 继续硬锁并保持 SHADOW_ONLY。

`PRIVATE_SIGNING/` 内是应用所有者的升级签名材料，只能私下保管，不应上传到公开仓库或转发。
