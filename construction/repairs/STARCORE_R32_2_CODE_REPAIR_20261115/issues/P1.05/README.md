# P1.05 冻结、结算、训练事件的因果关系

## 根因与现场证据

58个新增开奖中51个无及时赛前终态，仅7个补上影子结算。迟到未来标签不可追溯伪造。

## 本批修改与事务边界

以已确认 draw 的唯一 issue/来源摘要入账，按原冻结ID派发结算和独立合法学习事件；分别持久化数据可见时刻、freezeAt、openedAt、settledAt；都以事务提交为准。匹配不到冻结单列原因，旧已冻结单重试按唯一键重放。

用户停止/无网/抓取空/迟到/写失败分列；SHADOW、REAL、历史训练与未来观察口径分开。回调重复不能重复写收益或训练。

## 本批验收与故障反例

同期开两个回调、后台恢复、杀于结算后ACK前、旧历史覆盖；一次收益、每个合法训练事件至多一次且可补；主链资格接口沿用现有签名。

## 文件与完整源码

- [app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java](ddca867c7159.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java)
- [app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java](cf4b228e9f3e.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java)
- [app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java](161e91d563f4.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java)
- [app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java](b4bed32202f2.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java)
