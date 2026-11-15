# P1.02 铸造配额与生命周期迁移

## 根因与现场证据

foundry-su2 两次都在 408/4119，QUOTA_MISSING。表存在，单例无行。生命周期其他对象 66→130 时它仍未被初始化。

## 本批修改与事务边界

在 LifecycleSchemaR31.advanceWithinMigration 内先核对假设表和全部触发器的精确定义；由 HypothesisStoreR31.recoverQuotaSliceR32 计算旧行原始计费，四表全空时同事务初始化。四表有数据时每事务最多64行，以原主键续页，最后一页把精确字节与完成状态一起提交。业务调用保持 requireSchemaR31 只读。

重算期间原准入触发器拒绝新增，更新/删除触发器保护原件。游标、累计字节及摘要写同事务的 app_state；错触发器、坏游标或超过保护上限记录精确错误，保留原行；其它可运行任务独立推进。

## 本批验收与故障反例

空库首个可用工厂事务；有旧行删配额（多字节payload、三列复合键、恰好64及跨页）后重启；断在提交前后各一次，最后重算值等于原触发器账，重复插入不加费；原件被篡改或触发器缺失不得给出零配额。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/LifecycleSchemaR31.java](c5021400063c.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/LifecycleSchemaR31.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/HypothesisStoreR31.java](1efc16b2a724.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/HypothesisStoreR31.java)
