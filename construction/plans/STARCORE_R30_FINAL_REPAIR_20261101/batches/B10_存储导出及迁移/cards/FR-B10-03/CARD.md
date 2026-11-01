# FR-B10-03 导出快照生命周期、取消与重建

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-20。

前驱：B00, B02, B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java`

动作：**MODIFY**。施工范围：全部导出任务阶段。

问题：小ZIP背后仍保留8GB快照；重导出可能重复全库复制。

修改方式：同一不可变job复建复用来源，临时snapshot成功确认后按引用释放；默认ZIP≤20MiB或明确分卷。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`74c6e1b4251812f600c5ab0041259cdb1d5685b8f23365238ada8a1319e51266`。共享合并顺序：FR-B10-03。

### `app/src/main/java/com/ethk3/novaonepro/ReportTaskControlR69.java`

动作：**MODIFY**。施工范围：取消与阶段控制。

问题：取消可能未传播到底层长读/文件遍历/压缩。

修改方式：取消信号贯穿SQLite/native/IO阶段，≤2s释放，无半成品被当完整。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`c5c9e787350f3ef2848311abc6b59209eb053f223ca1e6a44ae2d9d9df4db5c2`。共享合并顺序：FR-B10-03。

### `app/src/main/java/com/ethk3/novaonepro/SourceReadLeaseR82.java`

动作：**MODIFY**。施工范围：acquire/close。

问题：快照/切库与读者lease需一致。

修改方式：短读及时释放，取消中断等待；离线源交换仅在显式安全阶段。

原方法锚点：SourceReadLeaseR82.acquire(Context context,File database,CancellationSignal cancel) L22–26；SourceReadLeaseR82.acquire(File database) L27–27；SourceReadLeaseR82.acquire(File database,CancellationSignal cancel) L28–28；SourceReadLeaseR82.acquire(File database,java.util.function.BooleanSupplier continuing,long maxWaitMs) L30–33；SourceReadLeaseR82.acquire(File database,boolean exclusive,CancellationSignal cancel,java.util.function.BooleanSupplier continuing,long maxWaitMs) L38–65；SourceReadLeaseR82.close() L66–71

基线SHA256：`508642fd214a60a228c4c0f127d264a42000c3766bcf4d23b85f8da410bdbb34`。共享合并顺序：FR-B10-03。

## 实施步骤

1. 保留完整索引和机读字段，原始二进制按需另取；不因省体积隐瞒未导出的原件。
2. 原job状态未完成不重复发起第二全库copy；空间预检失败明确。

## 测试与反证

- 导出各阶段取消/ENOSPC/杀进程/重建；临时副本不无限叠加，原件不损坏。

## 验收

导出完整性/产品质量分别显示，默认体积及取消符合原指标。

## 恢复与回滚

保留可恢复job元数据和原快照引用，不删受保护源。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
