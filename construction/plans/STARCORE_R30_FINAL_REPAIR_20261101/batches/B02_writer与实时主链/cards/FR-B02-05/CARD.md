# FR-B02-05 关键落库、结算和下一期冻结的幂等贯通

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-05/FIX-17。

前驱：B01。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java`

动作：**MODIFY**。施工范围：runWrite/runTransaction。

问题：多个旧写者与实时deadline竞争。

修改方式：关键写优先名单按实测操作复核，事务内禁等待/训练；失败必须回传可重试原因。

原方法锚点：DatabaseCoordinator.runWrite(String operation,WriteCallable<T> task,T fallback) L610–737；DatabaseCoordinator.runWrite(String operation,final WriteRunnable task) L862–874；DatabaseCoordinator.runTransaction(final String operation,final String targetIssue,final String idempotencyKey,final TransactionCallable<T> task,final T fallback) L885–910

基线SHA256：`a9ed86a1cba55d43ee77e44fb639209aabfe20bdcd44ce0813699dfecbd1990f`。共享合并顺序：FR-B02-05。

### `app/src/main/java/com/ethk3/novaonepro/BusinessReconcilerR36.java`

动作：**MODIFY**。施工范围：全部对账/投递。

问题：抓取成功不等结算和训练投递成功。

修改方式：主提交中唯一结算/持久outbox/时间线水位，提交后触发下一期。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`4ce6fef13c1df306b01aeee4757de1c423c71d87ba4ec3fc206b14d29cf7ada1`。共享合并顺序：FR-B02-05。

### `app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java`

动作：**MODIFY**。施工范围：全部正式结算入口。

问题：可能被解释/研究旁支拖累。

修改方式：保持唯一结算Owner和已冻结对象幂等；无原冻结记审计缺失，不补赛前票。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`38eb92ae71a0e09ee4bb7b3252a5c0ec2b51022e4dc5e5b18d8c0330ac85ecb0`。共享合并顺序：FR-B02-05 → FR-B05-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`

动作：**MODIFY**。施工范围：LiveTask。

问题：下一期冻结可能在研究重计算之后错过窗口。

修改方式：只消费已准备的不可变候选，源revision+截止+ABI在短提交复核。

原方法锚点：RuntimeR27$LiveTask.id() L250–250；RuntimeR27$LiveTask.step(ResearchPoolR27.Worker w) L251–262

基线SHA256：`ce5740d0d39e3e7f1ec6824614348713ee258535a92b8a46a600e4a423c71d2b`。共享合并顺序：FR-B02-05 → FR-B03-04 → FR-B04-05 → FR-B06-02 → FR-B07-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/FetchBackoffR30.java`

动作：**VERIFY_AND_KEEP**。施工范围：tryBegin/end/nextDelayMs。

问题：原单飞与退避已有实现；长延迟不能直接归因为抓取节奏。

修改方式：保留1/2/4/8/15s退避与4s超时合同，核对所有请求路径finally释放；仅发现违规才增量修补。

原方法锚点：FetchBackoffR30.tryBegin() L28–32；FetchBackoffR30.end(boolean success) L34–37；FetchBackoffR30.nextDelayMs(boolean haveCurrentDraw,long msUntilNextDeadline) L44–53

基线SHA256：`afeca954fe4a40bbcba05ffcd0a9624636cf7580c84eda771070850916b0cdfd`。共享合并顺序：FR-B02-05。

### `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`

动作：**MODIFY**。施工范围：fetchLoop/抓取finally。

问题：抓取、补抓、后台重启可能共享在途状态。

修改方式：逐入口核对FETCH_R30并只保留一个实际请求Owner；记录请求/成功/提交/outbox不同阶段，不改健康节奏。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`d00a7fed6e3a74ef8b6471893c3a4c508e27210277d959165b04a17a24f6a59c`。共享合并顺序：FR-B02-04 → FR-B02-05 → FR-B03-05。

## 实施步骤

1. 先保证核心draw/settlement/outbox，再逐个旁支；跨文件不增加第二真实结算Owner。
2. 每期有REAL/SHADOW/合法弃权/故障缺口明确终态，影子每期覆盖纳入全分母。

## 测试与反证

- 重复/乱序开奖、事务回滚、边界截止、服务重启；同issue REAL与结算最多一次。

## 验收

M03≤5s/M04≤1s，M05健康场景≥99.5%；小窗口不冒充24h验收。

## 恢复与回滚

撤新候选路由，保留既有合法主链与全部结算账。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
