# FR-B06-05 82能力与原教师F23恢复

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-16。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/CapabilityRegistryR30.java`

动作：**MODIFY**。施工范围：all。

问题：可执行/受阻/有效状态未分清。

修改方式：逐对象定义运行入口与数据依赖，统计有效性单独给证据。

原方法锚点：CapabilityRegistryR30.all() L19–34

基线SHA256：`70ef6e0bc66edd62cc3c9e43132951c851ed5e53ad329e467a4652371ed9dd67`。共享合并顺序：FR-B06-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/CapabilityRegistryStoreR30.java`

动作：**MODIFY**。施工范围：report。

问题：缺少完整生产/消费水位。

修改方式：对82项读取真实回执，显示运行时长、源水位、产物数、质量支持。

原方法锚点：CapabilityRegistryStoreR30.report(SQLiteDatabase db) L13–52

基线SHA256：`33816a509e490cd5b2e4f36bf753a3f329be62f9016bb4c1c2ac1db2e5870ae7`。共享合并顺序：FR-B06-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java`

动作：**MODIFY**。施工范围：bindLegacyTeacher/build。

问题：F23原教师未绑定。

修改方式：只接原教师合法输出和ABI，不用无关核/随机数占位；不可用保持具体BLOCKED。

原方法锚点：FamilyRegistryR26.bindLegacyTeacher(Map<String, String> binding,ModelReplaySU2.Adapter adapter) L9–9；FamilyRegistryR26.build(String id) L10–30

基线SHA256：`e9928ae9b578ff400439f9f98bde6b5f1b3f00cdbdb135ae486dbd50a877667a`。共享合并顺序：FR-B06-05。

## 实施步骤

1. F01–24、E01–16、I00–20、1模型、20矩阵全部登记；数量正确不自动有效。
2. 同类重复机制聚类，但保留原已验证能力与可恢复对象。

## 测试与反证

- 教师缺失/兼容/错ABI；82对象任何一个未知都不被汇总成有效0或100%。

## 验收

用户能逐个看到已实现、可执行、是否运行、是否消费、是否有效。

## 恢复与回滚

恢复原教师绑定或受阻状态，不删原有效功能。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
