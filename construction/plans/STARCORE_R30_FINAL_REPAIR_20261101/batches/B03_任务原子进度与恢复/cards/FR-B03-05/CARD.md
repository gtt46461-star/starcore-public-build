# FR-B03-05 公平持续运行与息屏任务恢复

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-19/FIX-21。

前驱：B02。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java`

动作：**MODIFY**。施工范围：任务优先级/claim。

问题：不能用缺字段推断卡住并盲目提升。

修改方式：基于typed pending/依赖分派，实时优先且研究有最小服务份额。

原方法锚点：RuntimeCoordinatorR26.claim() L97–120

基线SHA256：`044b9b89c2e8bd62feeb9c1aad178f58bcac4572ec7f2469e48b95ac0a97e4f3`。共享合并顺序：FR-B02-03 → FR-B03-03 → FR-B03-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java`

动作：**MODIFY**。施工范围：narrow/next/start/close。

问题：有CPU占用不代表有效服务。

修改方式：统计有效提交/CPU成本，热/资源限制明示；不忙等、不仅充电闲时运行。

原方法锚点：ResearchPoolR27.start() L75–84；ResearchPoolR27.narrow(long now) L121–121；ResearchPoolR27.next() L123–134；ResearchPoolR27.close() L191–199

基线SHA256：`e80ba04df767f5130b537c3aa70fa5f50a9c9f4cc1b5bd309ca13815b0a85f05`。共享合并顺序：FR-B03-03 → FR-B03-04 → FR-B03-05 → FR-B07-04 → FR-B08-05。

### `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java`

动作：**MODIFY**。施工范围：生命周期/看门狗入口。

问题：重启请求多、实际恢复和缺口不透明。

修改方式：恢复持久任务与租约，退出/强停缺口审计；厂商强停不伪装能后台一直跑。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`d00a7fed6e3a74ef8b6471893c3a4c508e27210277d959165b04a17a24f6a59c`。共享合并顺序：FR-B02-04 → FR-B02-05 → FR-B03-05。

## 实施步骤

1. 调度研究优先级不依赖正式确认结果；预算只用研究角色证据。
2. 长时间WAIT_INPUT不误报；长期pending没提交按五期告警。

## 测试与反证

- 前台/息屏/切后台/进程杀死/低内存回收；已投递任务最终唯一完成。

## 验收

原活跃能力持续有真实提交，所有缺口可解释。

## 恢复与回滚

回退公平队列版本，保留所有outbox与租约证据。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
