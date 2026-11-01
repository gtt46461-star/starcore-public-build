# FR-B02-03 移出旧结算解释与保留重计算

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-04/FIX-20。

前驱：B01。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/LegacyPredrawContextBridgeR915.java`

动作：**MODIFY**。施工范围：settleDraw/settlementInput。

问题：事务内读取、格式化Teacher大文本和计算解释hash。

修改方式：锁外准备不可变解释输入，短事务复核原predraw/draw/hash后唯一插入；原结算不等展示。

原方法锚点：LegacyPredrawContextBridgeR915.settleDraw(final DbStore store,final Models.Draw draw) L176–232；LegacyPredrawContextBridgeR915.settlementInput(SQLiteDatabase db,Models.Draw draw) L265–281

基线SHA256：`0f321dfaee7243dce171cf948099c6d2c84762023b8a4cde541092b998a54eee`。共享合并顺序：FR-B02-03。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`

动作：**MODIFY**。施工范围：execute。

问题：R26_RETENTION把多个fold/prune放同一长事务。

修改方式：改成可恢复维护阶段，每片核心游标+结果原子；保留实时优先。

原方法锚点：RuntimeCoordinatorR26.execute(Claim c,BudgetTokenSU1 budget) L204–277

基线SHA256：`044b9b89c2e8bd62feeb9c1aad178f58bcac4572ec7f2469e48b95ac0a97e4f3`。共享合并顺序：FR-B02-03 → FR-B03-03 → FR-B03-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/RetentionR26.java`

动作：**MODIFY**。施工范围：prune。

问题：全批多表DELETE/聚合不可界定。

修改方式：逐表有限键范围与阶段cursor，不按固定行数假设耗时；受保护引用不删。

原方法锚点：RetentionR26.prune(SQLiteDatabase db,long seq) L14–62

基线SHA256：`4c9d4b08cfb4da8c5740f2d88fced081a0810b18cb638c869b277938a044457f`。共享合并顺序：FR-B02-03 → FR-B10-02。

### `app/src/main/java/com/ethk3/novaonepro/RetentionRulesSU2.java`

动作：**MODIFY**。施工范围：foldDecisions/pruneResearchGenomes。

问题：折叠与删除跨阶段可能双计或漏计。

修改方式：折叠幂等键+高水位与源删除在安全同事务；必须恢复原充分统计。

原方法锚点：RetentionRulesSU2.foldDecisions(SQLiteDatabase db,long currentSequence,long now) L27–49；RetentionRulesSU2.pruneResearchGenomes(SQLiteDatabase db) L51–56

基线SHA256：`5103bf93069ef8780753562c303df0af674fb192dc385f123d4dcc1207514211`。共享合并顺序：FR-B02-03。

## 实施步骤

1. 使用准备/复核/提交协议，解释展示与核心结算Owner分开。
2. 维护每次最多一个有界片；取消后保留阶段，不整段重跑产生重复增量。

## 测试与反证

- 相同issue解释首次写哈希一致；维护中断任意片后汇总总量与不中断一致。

## 验收

R919/R26头部超限消除；保护资产、历史统计不丢失。

## 恢复与回滚

停止新维护片，原统计/源行引用仍可恢复。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
