# FR-B08-03 专家、家族、变异和融合的贡献反馈

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-14/FIX-16。

前驱：B07。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertProposalsR27.java`

动作：**MODIFY**。施工范围：propose。

问题：专家文字输出不等可执行策略。

修改方式：绑定失败簇→假设→AST→原子来源→原对子照；记录拒绝原因和后续贡献。

原方法锚点：ExpertProposalsR27.propose(String expert,IdeaFamilyR27.Context ctx,StatsR27.SplitMix r) L28–60

基线SHA256：`ac74623dbd5e33bb9e12c12beb7a5a772c8ef892096804eee738476e680d780d`。共享合并顺序：FR-B08-03。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/MutationOpsR27.java`

动作：**MODIFY**。施工范围：mutate/attempt。

问题：随机常数变动不能算新机制。

修改方式：研究贡献决定合法剪枝/窗口/输入/结构候选，保留随机探索份额。

原方法锚点：MutationOpsR27.mutate(PolicySpecR27 parent,String op,StatsR27.SplitMix r,IdeaFamilyR27.Context ctx,Contribution contribution) L22–29；MutationOpsR27.attempt(PolicySpecR27 parent,String op,StatsR27.SplitMix r,IdeaFamilyR27.Context ctx,Contribution contribution,FailureProfileR30 profile) L31–123

基线SHA256：`e0e52e3d32507e60923fd2f30dc31518bda5eced41806ad08788b03908be7408`。共享合并顺序：FR-B08-03。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionOpsR27.java`

动作：**MODIFY**。施工范围：fuse。

问题：单体高分不等融合有益。

修改方式：用同错区域/覆盖重叠/独立增益选互补父代，组合新身份重新确认。

原方法锚点：FusionOpsR27.fuse(List<PolicySpecR27> archive,EvalContextR27 ctx,StatsR27.SplitMix r,int gen) L15–61

基线SHA256：`db000ad09e04120535c953325c47a97808f6328896ca048c5129164480699075`。共享合并顺序：FR-B08-03。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java`

动作：**MODIFY**。施工范围：stepBirth/stepHalving/stepMigrateAblate。

问题：存活率和活动数不能代表变强。

修改方式：预算按研究贡献/多样性/成本，正式确认收益只用于晋升报告不回流共享选择。

原方法锚点：EvolutionLoopR27.stepBirth() L160–201；EvolutionLoopR27.stepHalving() L276–329；EvolutionLoopR27.stepMigrateAblate() L374–402

基线SHA256：`1c574ba07ac91efd900d29ed1ff02fbe659b9edf149e9d3de29ed36a4fd80580`。共享合并顺序：FR-B08-01 → FR-B08-03 → FR-B08-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaFamilyR27.java`

动作：**MODIFY**。施工范围：族接口/研究提案合同。

问题：13业务思路族增强需要机读贡献接口，不能只扩大名字数量。

修改方式：为实际族实现保留原生成ABI，增加可选研究产物引用；每族列输入/新机制/子代/成本/反证，缺研究输入不伪造能力。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`bccf3298cdfe271ffab928486a098d2e952dcc4297dc56ee8bad2fe243f1f175`。共享合并顺序：FR-B08-03。

## 实施步骤

1. 实现R05/R07–R12：失败处方、分子协同、行为簇、专家信用、算子成本、父子配对、融合互补。
2. 所有研究提案经原BirthGate，保证类型/PIT/时延/非重复；少数高质量冠军优于海量空壳。

## 测试与反证

- 同名改常数不新增机制数；负贡献不晋升；融合不继承证书；持久谱系与子代hash一致。

## 验收

每种创造动作都能追到研究依据和独立验证，未变强不能报verified。

## 恢复与回滚

停新提案通道，不删除原有效族/专家/创造能力。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
