# FR-B10-01 真实空间归因与预算

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-20。

前驱：B00, B02, B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/StorageCensusSU2.java`

动作：**MODIFY**。施工范围：全部普查入口。

问题：当前总占用26.52GB，不能只看draws5000。

修改方式：按主库/旧备份/快照/归档/模型及采样时刻列字节，读取page/freelist与大表payload。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`ad8be39b6a339495b96eae5eebf6f2b10be98b2acc5ba31e4c8636ce052c9092`。共享合并顺序：FR-B10-01。

### `app/src/main/java/com/ethk3/novaonepro/DataRetentionGovernorR900.java`

动作：**MODIFY**。施工范围：保留预算策略。

问题：库大小与活跃数据/空闲页/临时副本混淆。

修改方式：热表行数、总磁盘、受保护引用与临时复制预算分别管理；预留恢复空间。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`53722821457cb5a510d5f565fbe149609db6920e6f357fcf5514a9776a5d8e4e`。共享合并顺序：FR-B10-01。

## 实施步骤

1. 先测根因再回收；主库约9GB、快照8.32GB、新备份8.23GB逐项核验时刻。
2. 不能靠清App数据或禁用训练让磁盘变小。

## 测试与反证

- 大库/多备份/低可用空间/空闲页多的情形；普查截断要报告。

## 验收

每个大项有责任Owner、保留/清理条件和恢复证据。

## 恢复与回滚

停止清理，保留已验证旧源，不重复复制扩大占用。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
