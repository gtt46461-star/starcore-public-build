# R27 旧验收合同与 R31 行为对照

真实失败来自 run `41812391330`，产品源码 `844a78488f396189c2db98591782b7bb214799d2`。首次无排除编译、SU1、plan1025 和 R26 均在远程通过；JUnit 实际执行 379 项，4 项失败，原始行号与错误保存在 `run41812391330-unit-failure.log`。该 run 没有通过构建门，没有 APK 或 Android PASS。

只对 `R27QualityTest.java` 改进合同，没有更改产品源码或删除保护目的：

| 原失败 | R31 合同与实际替代证据 | 仍未证明的事项 |
| --- | --- | --- |
| :273 把 `R914_DRAW_HISTORY_BATCH` 与当期开奖同列 critical | 方案 `R31-B02-C01.md:59` 明确禁止这样处理历史回补。保留 fetch / 当前原子入库 critical 断言，另验证历史为 RESEARCH_DATA；实际调用 typed API 核对所有 WriteClass 的 critical 分类。 | 设备当期入库延迟和历史吞吐量。 |
| :290 依赖带固定空格的旧 `if (!writerBusy...)`，随后还要求已被替换的批量恢复 SQL | 实际生产队列配受控写 sink：busy 返回 false、没有假提交、保留批次重试后只执行一次；真实错误原样抛出；typed disposition 验证 busy 不消耗 fault budget。保留共享恢复入口、CAS 速率限制、owner/fence 条件及迁移保留旧故障记录的明确源码接线检查。 | 该 JVM sink 不是 SQLite 事务；恢复 SQL 的行为需独立 native / Android 测试。 |
| :300 要求线程仍名为 `starcore-r27-compute` 且必须是旧 raw executor 声明 | 从实际 pool 取得 bounded executor 与真实工厂线程，实际执行回调并退出；检查实际返回工厂对应实现中请求后台优先级早于 body。通过真实 P0 压力、freeze deadline 和 thermal 限制验证 narrow；用一个真实 BORN 候选完成 SELECT replay，若尝试提交 helper 则立即失败，验证 P0 串行重放。 | JVM 不证明 Android OS priority，日志明确 `OS_PRIORITY_DEVICE_REQUIRED`。 |
| :319 依赖旧 writer 局部变量名和语句排版 | 实际队列在 critical pressure 下不进入写 sink，压力解除后同一批次只推进一次；校验当前 typed admission 的 deadline、critical/freeze gate 均位于唯一 writer lock 前，并保留有限研究服务槽与 freeze 优先接线。报告线程栈采集保护保持。 | 设备 writer 排队时延、OS 公平性和持续运行 SLO。 |

在独立 `/tmp/r31-ci-844a-frozen` 上仅修改这一测试文件，执行未排除任务的整个 R27QualityTest suite，实际 19 tests / 0 failures / 0 errors / 0 skipped，18.307 秒测试时间，Gradle 总耗时 48 秒。XML、命令、补丁 SHA256 和源码状态记录均已保存。当前工作区的测试由 root 单独审查并应用，root 提交为 `31fea28c5723ac70e47ae4cc7542344b16fb3f91`；子代理没有 stage、commit 或 push。

旧 native recovery receipt 记载 199 个断言，但其编译提交为 8fa 且 dirty，12 个生产源码 hash 中 DatabaseCoordinator 不等于 844a。因此没有拿旧回执冒充新源码通过；比较结果见 `existing-native-recovery-byte-comparison.json`。已在独立 31fea 完整源码上补跑两项真实 native HOST 工具并通过：任务协议 199 断言，写协议含 9 个诊断阶段、6 个 fatal 边界、真实主机 JVM halt73 / 新进程重放及 10 项 JUnit。源码 hash 全匹配当前正式提交，`dirtyAtRun=false`；新日志与回执单独放在 `exact31fea-native/`，旧回执没有覆盖。

本次修正是验收代码与 R31 合同对齐。目标仍由新 source 的完整 CI、实际签名 APK 身份核验及 Android 测试判断，19 项本地通过不代替这些门槛。物理设备、24/72 小时和自然未来质量分别保留其真实状态。
