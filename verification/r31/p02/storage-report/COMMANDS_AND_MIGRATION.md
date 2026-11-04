# 命令与兼容性

已执行（完整原始log、input/output hash保存在对应目录）：

```sh
source /workspace/starcore-environment/activate.sh
CLASSES=/tmp/r31-p02-compile:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes \
R31_STORAGE_OUT=/tmp/r31-p02-storage-report-all-sinks-a \
REPORT_NATIVE_DIR=/tmp/r31-p02-native-20261104c \
tools/r31/storage_host/run_storage_report_host.sh
```

输出目录必须全新；不传REPORT_NATIVE_DIR时脚本从真实生产C/SQLite/Zstd建立HOST JNI，传入则校验receipt中每份C和.so hash。P02 API classpath只用于子集编译，不是最终source gate。source-before/after比较防止测试中源码漂移。

最后3源直接javac与UI取消目标命令保存在 final-freeze/target-command.txt。该目标只跑必要变更，未把16套无关重复计数。

数据库：NO_NEW_SQL_SCHEMA；不打开业务库writer、不提升user_version、不清空历史。文件ledger首次创建，固定bounded格式；损坏/hash不符不擦除重置，保留并显式拒写。正常finish记录已存在文件；OS lifetime lock证明旧进程结束后才回收旧承诺，文件保留。ledger metadata写失败留下conservative recovery/debt，不能伪称回收完成。

旧ReportNative.Control、Archive构造、Journal恢复API、Catalog/renderer/外排消费者入口保留；新增hooks默认兼容，实际Service通过明确scope连接。原native4/32MiB、backup source READONLY、输入pin、archive CRC/hash、128MiB CAS及checkpoint/backup原边界均保留。

API26+：流式DirectoryStream路径。API21–25：类隔离后返回UNSUPPORTED/UNKNOWN并拒绝依赖完整普查的新准入；没有listFiles无界回退。合法min21启动及各Android版本实际行为尚未在设备运行，不能据静态编译写DEVICE_PASS。

APK/applicationId/签名：本agent没有改版本/身份/签名、没有产APK。root沿既有构建签名流程统一生成。source快照不含签名私钥或令牌。
