# FR-B11-02 V2353A前台、息屏与72小时实测

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-21。

前驱：B05, B09, B10。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `construction/r30-repair/DEVICE_RUNS.json`

动作：**ADD**。施工范围：完整新增文件。

问题：现有physicalDevice=NOT_RUN。

修改方式：记录型号/Android/进程/温度/电量/磁盘/前后台、每期机会和提交水位。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B11-02。

### `app/src/main/java/com/ethk3/novaonepro/RuntimeHealthLedgerR36.java`

动作：**MODIFY**。施工范围：运行窗口汇总。

问题：瞬时HEALTHY不代表窗口SLO通过。

修改方式：活性、窗口时延/覆盖、系统退出、研究完成和质量分别显示。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`ecbd68d1b389f773bdae6c71a2029226b7b4c455dc77ed905030ec169a79123a`。共享合并顺序：FR-B11-02。

## 实施步骤

1. 前台60min、息屏6h、连续24h与72h，包含旧库覆盖和新安装场景。
2. 厂商强停/低内存/应用崩溃分列；已投递恢复，未冻结缺口不补票。

## 测试与反证

- M03≤5s/M04≤1s/M05健康≥99.5%/M07≤200ms，任务无无故饿死，磁盘预算受控。

## 验收

目标设备工程指标有真实窗口分母与原日志，不用模拟器替代。

## 恢复与回滚

恢复上一稳定版和可恢复数据，不伪造长测。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
