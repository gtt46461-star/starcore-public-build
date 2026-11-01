# FR-B08-02 短期结构、跨位与集合交集研究

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-14。

前驱：B07。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/StructureGeneratorRegistryR30.java`

动作：**MODIFY**。施工范围：六类generate/mutate。

问题：现有结构生成器不等发现了可用优势窗口。

修改方式：接结构研究状态和支持域；窗口选择用过去，下一期结构目标事前定义。

原方法锚点：StructureGeneratorRegistryR30$Generator.generate(StatsR27.SplitMix r,Context ctx) L17–21；StructureGeneratorRegistryR30$Generator.mutate(PolicySpecR27 parent,StatsR27.SplitMix r,Context ctx) L22–24

基线SHA256：`23df335435c8395f3995d7942bee8ad64c91d9d95f216491b06c543418059754`。共享合并顺序：FR-B08-02。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/FeatureCacheR27.java`

动作：**MODIFY**。施工范围：regime/cross/crossCond/transition。

问题：历史描述标量不能当下一期结构概率。

修改方式：新增有语义的过去特征与可用时间，目标结构标签开奖后才可得。

原方法锚点：FeatureCacheR27.transition(int tp,int lag) L152–164；FeatureCacheR27.cross(int src,int lag) L167–172；FeatureCacheR27.crossCond(int src,int tp) L175–187；FeatureCacheR27.regime(int w) L225–234

基线SHA256：`b0c3072c8c03cedbf9130bb47a365b504c41c0b4d58f8b251248b31e82d45552`。共享合并顺序：FR-B04-05 → FR-B08-02。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/SetIntersectionPolicyR30.java`

动作：**MODIFY**。施工范围：完整集合策略实现。

问题：交集过少可能被随机补号或无证据缩减。

修改方式：保留每次集合来源/支持/缩减损失，最终合法单位置K3或明确弃权。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`cbc7b889bfa617ee8fce4c40e58266560174f29cba05d96c0944aa4fcc9bdc37`。共享合并顺序：FR-B08-02。

### `construction/r30-repair/STRUCTURE_STUDY_CONTRACT.json`

动作：**ADD**。施工范围：完整新增文件。

问题：准确无误抓窗口不能用后验选好段验证。

修改方式：固定coarse结构目标、8/32/128/512候选窗、训练/校准/前向验证、基线与失败条件。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B08-02。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI13SelectorOnlyR27.java`

动作：**MODIFY**。施工范围：generate。

问题：新的状态研究不能绕过原选时选位确认。

修改方式：将研究支持域形成的新selector仍转为完整新policy身份，经原出生/行为/独立确认门。

原方法锚点：IdeaI13SelectorOnlyR27.generate(StatsR27.SplitMix r,IdeaFamilyR27.Context ctx) L12–20

基线SHA256：`2d55131e2500c725e0951dbc4604c743debe8f440ca0a58d66d194105cb7c5ad`。共享合并顺序：FR-B08-02。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/SelectorPolicyR30.java`

动作：**VERIFY_AND_KEEP**。施工范围：combine/registeredRate。

问题：当前规则已有事前身份绑定，新研究接入不得借旧证书。

修改方式：保留H_SELECT估计率与QUANTILE/RANDOM执行语义，注册率与实际率分列；新组合重新确认。

原方法锚点：SelectorPolicyR30.combine(PolicySpecR27 base,int position,PolicySpecR27.Rule rule,double threshold,double rate,EvalContextR27 context,PolicySpecR27.Meta provenance) L10–36；SelectorPolicyR30.registeredRate(PolicySpecR27 spec) L39–42

基线SHA256：`8dc4e1af9778d6a0e7c840c89c30982ffa0c645a3570cce43700d841ede3c54b`。共享合并顺序：FR-B08-02。

## 实施步骤

1. 实现R01–R04计算：粗结构分类/条件概率、下一期误差、跨位兼容、集合支持链。
2. 先比较简单平滑条件模型与原结构核；只有时间隔离proper score/配对效用改善才增加复杂度。
3. 多位置研究不放宽真实单位置K3，交集大小不足3必须按原弃权合同。

## 测试与反证

- 改未来标签不改原窗口；对随机控制同预算跑；逐层缩减的覆盖与损失可复算。

## 验收

结构输入/预测/消费者/下一期结果成闭环，不把冷热/遗漏图当预测证明。

## 恢复与回滚

恢复原结构策略及弃权行为，保留研究原件。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
