# P4.03 快照、完整导出、恢复与附件

## 根因与现场证据

本轮两份导出成功；历史曾在BACKUP读缺失checkpoint而失败。主JSON和MD展开体积很大。

## 本批修改与事务边界

沿正常导出入口实现 requestId 幂等、ACK outbox、固定snapshot、附件租约、逐文件hash/大小、同目录临时写和原子发布、用户保存后回读。大型行集走可分页附件，正文保留准确分母及定位；导出可取消、断点恢复。

可选历史缺件标示缺失/受影响模型；必需附件缺失不能签 COMPLETE，按对象恢复重跑，保留首个原因/阶段和用户可复制错误。导出进程不得把自身遥测当主进程。

## 本批验收与故障反例

准备中/发布后ACK前进程死亡、原件消失、文件满、取消、重复点击、URI回读失败。成品解压、逐件hash与根引用闭包匹配，ZIP和原机事件可追。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java](34e801bcb32d.md)：REPLACE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java)
- [app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java](7fed5cb3d645.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java)
- [app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java](d89a0cdb3508.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java)
- [app/src/main/java/com/ethk3/novaonepro/ReportArchiveVerifierCP12.java](467193aeedf7.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ReportArchiveVerifierCP12.java)
- [app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java](c9f9a5cceea5.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java)
- [app/src/main/java/com/ethk3/novaonepro/ReportJsonWriterR31.java](ef58be36dd7f.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ReportJsonWriterR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcStoreR31.java](147625fe7247.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcStoreR31.java)
- [app/src/main/java/com/ethk3/novaonepro/ReportCleanupR31.java](64e512a4fcf0.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/ReportCleanupR31.java)
