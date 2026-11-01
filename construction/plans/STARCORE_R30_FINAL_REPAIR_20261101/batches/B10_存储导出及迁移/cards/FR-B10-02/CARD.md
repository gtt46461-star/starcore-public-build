# FR-B10-02 5000热期与引用安全归档

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-20。

前驱：B00, B02, B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java`

动作：**MODIFY**。施工范围：ensureProtected/step/归档与删除。

问题：仅开奖热表受限但研究修订/标签/模型可能无限积累。

修改方式：保护所有未完成任务、确认、证书、父代引用；无引用可重算派生页有界归档/删除。

原方法锚点：ResearchRetentionStoreR30.ensureProtected(SQLiteDatabase db) L23–50；ResearchRetentionStoreR30.step(DbStore store,RuntimeCoordinatorR26.Claim claim) L119–128

基线SHA256：`5d3b1a97012b91001b57dde9aba14da807669ffa74db68ffad9d5f1b16c5fa27`。共享合并顺序：FR-B07-03 → FR-B10-02。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/RetentionR26.java`

动作：**MODIFY**。施工范围：prune。

问题：删除源行前需证明充分统计和原件可恢复。

修改方式：逐页checksum/hash归档并复验，再原子切引用；删除不在长事务整库进行。

原方法锚点：RetentionR26.prune(SQLiteDatabase db,long seq) L14–62

基线SHA256：`4c9d4b08cfb4da8c5740f2d88fced081a0810b18cb638c869b277938a044457f`。共享合并顺序：FR-B02-03 → FR-B10-02。

### `app/src/main/java/com/ethk3/novaonepro/RetentionTableRegistryCP12.java`

动作：**MODIFY**。施工范围：各表保留合同。

问题：新增6表与16逻辑视图需纳入生命周期。

修改方式：按对象角色与依赖保留，非按同一个created_at粗暴清理全部。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`f1dd234a3c059b24d53edab8158c229a38fc21e4ee5315db23fe4d7ef29ad261`。共享合并顺序：FR-B10-02。

## 实施步骤

1. draws≤5000，受保护证据可超热窗但总目录需预算与可追溯归档。
2. 明确归档对象总盘仍计入预算，不能只把文件搬目录冒充压缩。

## 测试与反证

- 引用未归零不得删、坏归档拒删、任意页中断恢复、充分统计不双计。

## 验收

空间稳定受控且历史训练/确认/证书原件可恢复。

## 恢复与回滚

回到原源和引用指针，未验证归档不接管。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
