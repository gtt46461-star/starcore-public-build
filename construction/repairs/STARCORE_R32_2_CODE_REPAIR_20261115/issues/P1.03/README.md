# P1.03 实时尾段、下一期预冻结

## 根因与现场证据

5000行历史中该时点仅2期连续；门槛12。通用 R914_DRAW_HISTORY_BATCH 按研究写者准入；出现53条带积压原因的新延迟事件。

## 本批修改与事务边界

在 saveApiResultDraws 的历史页入口提取独立修尾段任务；不依赖 quick 文本原因或最新开奖记录回调。先调度现有最新开奖，写者每片重读当前 head、截止与目标前12期，仅提升无冲突的已知连续前缀，沿用每片至多8行；普通历史仍走原回补。须补齐合并期间的持久目标、服务重启重投和不同来源冲突回执。

取、写、冻结分别校验目标与截止时间；补档行始终走 BACKLOG 结算；不能给已开奖补写赛前决策。任何写失败在唯一 owner 下重读；不扩大批量历史的优先级。

## 本批验收与故障反例

输入乱序、跨日、缺中间期、冲突开奖、已过截止、相同页两次。检查 target 只用此前可见数据、发生真实 B04 冻结或留下准确错过原因。模拟后包覆盖变化并量P95和分母。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java](ddca867c7159.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java)
- [app/src/main/java/com/ethk3/novaonepro/DbStore.java](e30e335e70ea.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/DbStore.java)
- [app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java](2ee925948844.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java)
- [app/src/main/java/com/ethk3/novaonepro/R36AnalysisHistoryPolicyR364.java](90d0c0a02ad2.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/R36AnalysisHistoryPolicyR364.java)
- [app/src/main/java/com/ethk3/novaonepro/HistoryIntegrityVerifierR815.java](b85706658b90.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/HistoryIntegrityVerifierR815.java)
- [app/src/main/java/com/ethk3/novaonepro/TargetIssueResolver.java](509fb41d352b.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/TargetIssueResolver.java)
- [app/src/main/java/com/ethk3/novaonepro/PredrawTailRecoveryR32.java](85960464425d.md)：ADD；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/PredrawTailRecoveryR32.java)
