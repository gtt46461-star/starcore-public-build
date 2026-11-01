# FR-B04-03 完整优化器续训及标签隔离

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-09。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java`

动作：**MODIFY**。施工范围：encode/decode/online。

问题：不能只凭checkpoint存在判全状态恢复。

修改方式：原Adam/RNG/dropout/shuffle/replay/position/partialIssue一体保存并验证。

原方法锚点：AtomNetSessionR30.online(AtomNetTrainerR27.Sample sample,String issue,long ordinal,int position) L96–105；AtomNetSessionR30.encode() L121–133；AtomNetSessionR30.decode(byte[] bytes,String hash) L134–150

基线SHA256：`bea4b2c349b9f2342dbfdcbf3890c25896204c663378536b22efa32d67c5f35a`。共享合并顺序：FR-B04-01 → FR-B04-02 → FR-B04-03。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelArtifactR29.java`

动作：**MODIFY**。施工范围：encode/decode。

问题：旧部分状态不可当完整续训成功。

修改方式：兼容原字节；缺字段保留旧对象并明确需重训，不静默初始化优化器。

原方法锚点：ModelArtifactR29.encode() L35–54；ModelArtifactR29.decode(byte[] bytes,String expectedHash) L56–87

基线SHA256：`6b3e6e79a23284a458eb218478aa3348b5c38466fad4a07aa00fc2eac6c5646b`。共享合并顺序：FR-B04-03。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetTrainerR27.java`

动作：**MODIFY**。施工范围：online/持有验证状态。

问题：EVAL_ONLINE_HOLD影响学习率/回滚，须明确是开发验证还是正式确认。

修改方式：改名标注DEVELOPMENT_VALIDATION；正式CONFIRM/FUTURE受保护标签不回流共享优化器。

原方法锚点：AtomNetTrainerR27.online(Sample s) L67–77

基线SHA256：`d55800da020e21ab3a514f8a23475c1c7130914203113d89c2a9579feb918d14`。共享合并顺序：FR-B04-03。

## 实施步骤

1. 保持原codec ABI或明确新outer ABI并提供逐字节备份；角色改变不能只改字符串绕过守卫。
2. 中断P04/P09、shuffle中间、验证前后逐一复验。

## 测试与反证

- 真实类连续运行vs每64样本decode恢复，后续更新一致；改确认标签不改研究输出。

## 验收

注册容差内全状态一致、重复消费0、正式角色污染0。

## 恢复与回滚

回到最近兼容完整artifact，旧原件保持可恢复。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
