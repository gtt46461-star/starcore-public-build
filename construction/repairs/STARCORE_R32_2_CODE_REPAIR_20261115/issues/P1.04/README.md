# P1.04 写者故障回执、服务调度与后台

## 根因与现场证据

写者积压使历史和研究被退让。报告进程的 pending=0 无法说明主进程故障队列已排干。窗口内54957个应唤醒时槽中实际唤醒141次。

## 本批修改与事务边界

P1.patch 使主进程控制 tick 先排故障再触发旧库迁移；每次最多4笔、单笔独立提交，第二笔起检查40ms及关键工作，失败保留队头。补上持久化进程/boot/session、oldest事件、phase、到期时刻、下次尝试；从 provider 请求/响应→commit/冻结逐段计时。

一个 writer、一个有效 owner/fence；取消后先确认旧 continuation 结束；不能新增并发替身。手动停止、系统限制、屏灭、重启分列；开启的研究会话不以充电为条件。

## 本批验收与故障反例

在END前/后杀进程，ACK未记、控制线程被阻、重复快照、时钟跳变、屏灭各跑一次；短切片不可抢占处按真实超预算计；对齐同进程的排队、落库和受阻变化。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java](2ee925948844.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java](ca247b4e9e75.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java)
- [app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java](ddca867c7159.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java)
- [app/src/main/java/com/ethk3/novaonepro/RealtimePressureGateR811.java](aed18f22a24b.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/RealtimePressureGateR811.java)
- [app/src/main/java/com/ethk3/novaonepro/LifecycleHealthR963.java](7d34a6b74523.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/LifecycleHealthR963.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java](86a2cf806c71.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java)
