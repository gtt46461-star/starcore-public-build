# FR-B05-04 对齐报告旁路门禁与实际签证合同

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-11/FIX-17。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/GateV2R29.java`

动作：**MODIFY**。施工范围：ENABLED/evaluate/evidence/record。

问题：ENABLED=false且只写would_issue，不能直接代表真实授证。

修改方式：报告明确旁路状态；修Gate输入窗口hash/分母与影子摘要对账；不得简单把常量改true。

原方法锚点：GateV2R29.evaluate(Evidence e) L85–138；GateV2R29.evidence(SQLiteDatabase ro,String id,long latestOrdinal) L157–182；GateV2R29.record(ResearchWriteQueueR27.Batch b,Evidence e,Verdict v) L185–188

基线SHA256：`9003fa5f3a2a36b1152df06ba652bf5b196d464a5447473adbc6a748c2ee6269`。共享合并顺序：FR-B05-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java`

动作：**MODIFY**。施工范围：evaluate/choose。

问题：当前R27_GATE_V1是真实签证合同且有250 ACT/连败≤6等门槛。

修改方式：旧合同保持不可变；若采用新R30质量合同，新增版本化新实验证据绑定，不重写旧成绩。

原方法锚点：CertificateGateR27.evaluate(Evidence e) L31–45；CertificateGateR27.choose(List<Evidence> active) L52–61

基线SHA256：`067741d21f991118f9c659c38e2314fee21ce20d5c5323fa5f545150350cdb52`。共享合并顺序：FR-B05-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java`

动作：**MODIFY**。施工范围：certify。

问题：签证、主链选择与报告旁路可能不一致。

修改方式：以experiment绑定gateVersion选择唯一权威评估器，报告同时列inputHash/时刻/阈值。

原方法锚点：LiveShadowsR27.certify(RuntimeR27 rt,ResearchPoolR27.Worker w,long latestOrd) L206–251

基线SHA256：`0d150194fbb8d0758ef7501c7657eb5fdcaf02bbc3a7f96a39c1802451f04188`。共享合并顺序：FR-B05-03 → FR-B05-04 → FR-B06-06。

## 实施步骤

1. 21/61摘要与V2 failing列表hitRate差异必须先对齐输入再判断实现问题。
2. 新合同生效需正/负对照与撤销测试；所有门禁成立才走真实链，不以达到点命中率越线授权。

## 测试与反证

- 同证据两门禁并列，不混分母；旧证书在新合同下不自动有效；未注册新selector不得REAL。

## 验收

质量合同、报告和真实签证明确一致，没有旁路报告假装已启用。

## 恢复与回滚

停用新合同路由，保留原Gate及历史预算/证书。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
