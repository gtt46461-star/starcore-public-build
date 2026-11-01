# FR-B02-04 实时事件身份和时间线校正

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-05。

前驱：B01。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/IssueTimelineR30.java`

动作：**MODIFY**。施工范围：Row/elapsed/summarize。

问题：80,042,119ms异常分段可能混历史/实时事件。

修改方式：添加事件模式/实例合同，LIVE/BACKFILL/REPLAY分开；跨boot、负差、重复实例单列。

原方法锚点：IssueTimelineR30$Row.<init>(String issue) L80–80；IssueTimelineR30$Row.set(Stage s,Long wallMs,Long monoMs,String bootId) L81–81；IssueTimelineR30$Row.wall(Stage s) L82–82；IssueTimelineR30$Row.elapsed(Stage from,Stage to) L84–89；IssueTimelineR30$Row.freezeMarginMs() L91–94；IssueTimelineR30.summarize(List<Row> rows) L98–135

基线SHA256：`5f14a140b4dc25f3d140898b8832f592fa1bfb55bd67b92fb00753c002083e57`。共享合并顺序：FR-B02-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/IssueTimelineWriterR30.java`

动作：**MODIFY**。施工范围：全部阶段写入。

问题：仅issue首次时间可能跨实例串接。

修改方式：绑定原始trace/boot实例，合法第一次到达且同事件求差；保留墙钟对账。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`749edbe39db2e191143622041cd32a53dfafe7b69bd6171efc27cab8202a807a`。共享合并顺序：FR-B02-04。

### `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`

动作：**MODIFY**。施工范围：全部抓取/倒计时触发路径。

问题：写拒绝与倒计时恢复不清晰。

修改方式：抓取单飞、1s计划节拍与失败退避分列；倒计时用单调钟校正原截止，不在UI驱动后台真相。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`d00a7fed6e3a74ef8b6471893c3a4c508e27210277d959165b04a17a24f6a59c`。共享合并顺序：FR-B02-04 → FR-B02-05 → FR-B03-05。

## 实施步骤

1. 每期串起response、validation、drawCommit、settlement、nextFreeze。
2. 抓取阶段错误不累加成丢期数，网络耗时与写锁等待分别报告。

## 测试与反证

- 历史回补/实时并存、改墙钟、跨进程/boot、重复响应、断网恢复；不剪裁异常来达标。

## 验收

时差语义正确，异常有来源；M03/M04按受支持真实LIVE样本重测。

## 恢复与回滚

恢复旧时间线原件，撤回新投影不改已冻结预测。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
