# FR-B03-03 任务提交与真实进度原子记录

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-03。

前驱：B02。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`

动作：**MODIFY**。施工范围：record/loop。

问题：事后另写任务receipt可能BUSY失败，不能视为主任务提交事实。

修改方式：主产物batch内写typed progress；record仅写调度/错误观测；失败不提前刷新完成水位。

原方法锚点：ResearchPoolR27.loop(Worker w) L86–118；ResearchPoolR27.record(String id,Outcome o,Throwable failure) L137–163

基线SHA256：`e80ba04df767f5130b537c3aa70fa5f50a9c9f4cc1b5bd309ca13815b0a85f05`。共享合并顺序：FR-B03-03 → FR-B03-04 → FR-B03-05 → FR-B07-04 → FR-B08-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`

动作：**MODIFY**。施工范围：execute/任务提交路径。

问题：不同任务游标与源时间混在一列。

修改方式：各任务返回明确ProgressFacts，只有本批产物提交才更新完成源/commitClock。

原方法锚点：RuntimeCoordinatorR26.execute(Claim c,BudgetTokenSU1 budget) L204–277

基线SHA256：`044b9b89c2e8bd62feeb9c1aad178f58bcac4572ec7f2469e48b95ac0a97e4f3`。共享合并顺序：FR-B02-03 → FR-B03-03 → FR-B03-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java`

动作：**ADD**。施工范围：拟增任务事实追加接口。

问题：新的研究任务同样需绑定真实提交。

修改方式：新增任务事实helper必须组合原before，不覆盖原lease/fence。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B03-03 → FR-B07-02 → FR-B07-04 → FR-B07-05 → FR-B08-05。

## 实施步骤

1. 每个任务先登记单位/输入语义，再接入产物同事务更新；禁止增加空心心跳来伪造进度。
2. B07将继续扩展同一个新Store文件；本批只添加任务事实接口，B07增量合并。

## 测试与反证

- 产物提交失败但调度receipt成功时不能显示训练完成；跨进程重启恢复pending。

## 验收

任务完成事实由相同提交产生，调度记录与产物水位可对账。

## 恢复与回滚

保留旧产物和typed事实版本，不双写两个权威游标。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
