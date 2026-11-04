# R31 历史 CSV 的生产协议验收

本目录调用真实 `HistoryImportR27`、`ResearchWriteQueueR27`、`DatabaseCoordinator`、
schema owner、source journal、原始 input pin 和 `StoreR27`。SQLite 由原生 sqlite-jdbc 执行。
所有输入均为明示的工程合成数据；不证明自然胜率、Android 调度或持续运行。

`run_history_import_host.py` 编译 241a9384 的原导入器作为负对照，保留其真实全量事务、
运行期 DDL、同 CSV 重复冲突计数，以及 COMMIT 后实际 halt/reentry 的结果。

`run_publication_host.py` 默认执行 15 个独立模式，包括真实进程 halt(73)、原 30 秒
owner lease 自然到期、保存源恢复、分片/最终发布/成员准备中断、rollback、提交后诊断失败、
实际阻塞 I/O 退出、容量债务拒绝、精确成员 pin 判定、真实关键 writer 竞态和流式格式治理。

准备实际 Android 编译产物与环境后运行：

```bash
source /workspace/starcore-environment/activate.sh
R31_HOST_CLASSES=/path/to/frozen-actual-android-classes.jar \
R31_HOST_RJAR=/path/to/actual-generated-R.jar \
SQLITE_JDBC=/path/to/sqlite-jdbc.jar \
python3 tools/r31/history_import_host/run_publication_host.py
```

联改尚未合并时，可用 `R31_SOURCE_JOURNAL_SOURCE` 指定另一 owner 的实际 Journal 源，
`R31_HISTORY_JOINT_SOURCES` 指定包含同版 Domain/Schema/Pin/其他生产协议源路径的 JSON 数组。
脚本先逐源复制和 SHA256 绑定，再显式编译；原始产物依赖和每条命令写入 `EXECUTION.json`。
这类 WIP 联合快照不声称为一个干净 Android 源提交或 APK。

HOST 默认 allocated 测量为 UNKNOWN 的拒绝保留为独立负控。其余模式只把 private
`STAT/BLOCKS` 测量边界接到本目录 JNI 的真实 POSIX `stat(2).st_blocks`，记录源/二进制 SHA，
不改变 VM 属性、生产 Config、全目录遍历、预算或 probe。物理 blocks 与文件逻辑字节分别报告；
2GiB 稀疏文件测试证明总容量债务，未声称耗尽真实可用磁盘。

Android 实际边界由 `R31HistoryImportInstrumentedTest` 覆盖。必须在新的 routed sandbox 内执行：

```bash
adb shell am instrument -w -e r30Sandbox r31-history-UNIQUE_RUN_ID \
  -e class com.ethk3.novaonepro.R31HistoryImportInstrumentedTest \
  com.starcore.research.r13.test/com.ethk3.novaonepro.R30FaultAndroidRunner
```

该 Android 测试未运行前维持 NOT_RUN。HOST 结果不替代 API29/35、真机、24h/72h、
真实低可用磁盘、外部文档 provider 卡住时的退出或 Android 实际持锁 SLO。

保护/回收边界：未引用失败快照及同源临时副本在实际流结束后删除，许可仅在实际退出后关闭。
STAGING 和 COMPLETE 原始 CSV 由 `run.source_path/hash/bytes` 引用；COMPLETE 行/manifest 禁止修改或删除。
本 P01 没有实现 P06 引用闭包退休；活动输入、证书/预测/训练依赖验证及旧 CSV/未发布 member
的回收仍需 P06。进程恰在快照 fsync 后、claim 前死亡留下的无引用文件也待 P06 普查。
默认存储预算会把保留文件和数据库真实占用计入容量，超额拒绝新增，不把换目录当释放。
