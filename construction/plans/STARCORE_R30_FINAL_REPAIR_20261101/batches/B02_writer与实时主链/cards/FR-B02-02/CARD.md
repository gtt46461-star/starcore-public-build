# FR-B02-02 定位并优化FTRL事务内慢点

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-04。

前驱：B01。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/ResearchAdaptiveLearnerStoreR811.java`

动作：**MODIFY**。施工范围：updateFromStrictOos/tailMatchesPreState/targetCompatible/matches。

问题：61.581秒长锁；编码已在锁外，不能重复提出无效搬移。

修改方式：给事务内读尾/查重/BLOB插入/事件钩子分段计时；用真实EXPLAIN检查索引；原artifact+learned原子性保留。

原方法锚点：ResearchAdaptiveLearnerStoreR811.updateFromStrictOos(DbStore store,String targetIssue) L29–190；ResearchAdaptiveLearnerStoreR811.tailMatchesPreState(SQLiteDatabase db,String targetIssue,String expectedPreStateHash) L351–370；ResearchAdaptiveLearnerStoreR811.targetCompatible(SQLiteDatabase db,String artifactType,String issue,String expectedHash) L373–388；ResearchAdaptiveLearnerStoreR811.matches(SQLiteDatabase db,ResearchArtifactCodecR811.Artifact artifact,String hash) L390–411

基线SHA256：`5269face2938d4a6d9bc38b9cc76eb34d33838fb487b8bc7955604162d00f0e1`。共享合并顺序：FR-B02-02。

### `app/src/main/java/com/ethk3/novaonepro/RuntimeEventHooksR926.java`

动作：**MODIFY**。施工范围：learnedWithinTransaction。

问题：事务内串接事件与Batch34归档可能放大写量。

修改方式：检查事件写和触发器级联，核心学习回执在本事务；可重算展示外移outbox，不把核心证据拆成两提交。

原方法锚点：RuntimeEventHooksR926.learnedWithinTransaction(SQLiteDatabase db,String issue,String source,String learningIdentity,String preStateHash,String postStateHash,String trainedUntil,String detail) L157–204

基线SHA256：`80f44f57d23d3363c9c1c78ad829bc152efad4cbce4727d63b288f81c5b55b63`。共享合并顺序：FR-B02-02。

### `app/src/main/java/com/ethk3/novaonepro/Batch34RuntimeRepositoryR930.java`

动作：**MODIFY**。施工范围：全部learned写入链。

问题：可能存在重复归档和大payload修订。

修改方式：按learningIdentity去重并记录字节/影响行；payload引用化需保存原字节及哈希。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`b3f3fd52aa087bee98b5a934861504f0ae045202f87a76f3a2d2a09fc910453b`。共享合并顺序：FR-B02-02。

## 实施步骤

1. 必须用实际大库与模型测出最慢子段，不能凭operation名字归因。
2. 先加/复用匹配查询索引并测读写代价；若大blob确为瓶颈，再设计stage/CAS发布且原读者兼容。
3. 不拆掉B09 atomic linear学习合同。

## 测试与反证

- 同输入、同模型、同并发下长锁复测；原件checksum与唯一学习回执一致；回滚/磁盘满不产生半个学习状态。

## 验收

后台hold目标50ms硬200ms，关键wait P95≤200ms；若仍失败，保留准确原因。

## 恢复与回滚

恢复原发布指针与artifact加载路径，不回滚已结算账。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
