# P1.01 旧库三列与两个迁移入口

## 根因与现场证据

032 旧库快照的 link candidate 表只有部分列。helper 已加三列，store owner 在索引前漏了同一组三列。旧库累计尝试 394→589。

## 本批修改与事务边界

按 P1.patch 在实际严格迁移 owner 先补 target_issue、candidate_rank、candidate_key，再创建唯一索引；检查 table_info/index_info、触发器声明及已存在重复键。在同一事务重算单元后置条件。

禁止删旧行或虚构 key；合法 NULL 按旧语义保留；重复非空键先列出不可合并的冲突并停在本单元；业务读者不得顺手建表。

## 本批验收与故障反例

重放仅含 snapshot_key 的旧表、三列分别已存在、错误索引、多条NULL、重复非空键；中断两次恢复，行数/原行摘要不变，有且只有一条完成收据。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/DbStore.java](e30e335e70ea.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/DbStore.java)
- [app/src/main/java/com/ethk3/novaonepro/DbHelper.java](e5f3283cf8ab.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/DbHelper.java)
- [app/src/main/java/com/ethk3/novaonepro/LegacyDdlR31.java](5110c2f7e035.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/LegacyDdlR31.java)
- [app/src/main/java/com/ethk3/novaonepro/LegacySchemaCoordinatorR926.java](f10f7fb356c0.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/LegacySchemaCoordinatorR926.java)
- [app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java](e287047750cf.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java)
- [app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java](2ee925948844.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java)
