# FR-B04-05 I08与策略冻结真实消费闭环

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-08。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI08NeuralRepresentationR27.java`

动作：**MODIFY**。施工范围：generate/mutate。

问题：model_surface叶子可能因模型未就绪全部阻断。

修改方式：保留family旧能力，只有合法模型面可用时提案；缺依赖原因可查。

原方法锚点：IdeaI08NeuralRepresentationR27.generate(StatsR27.SplitMix r,IdeaFamilyR27.Context ctx) L12–17；IdeaI08NeuralRepresentationR27.mutate(PolicySpecR27 p,StatsR27.SplitMix r,IdeaFamilyR27.Context ctx) L18–20

基线SHA256：`5886c23344596855be4d8d6e39bd89b1a59e9e7a147fc02a8c61deeb9bdaaa53`。共享合并顺序：FR-B04-05。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceStoreR30.java`

动作：**MODIFY**。施工范围：消费记录与report。

问题：0调用不能显示100%匹配。

修改方式：调用必须匹配真实policy/target/model/probability/frozen_at；统计0/0为null。

原方法锚点：ModelSurfaceStoreR30.report(SQLiteDatabase db) L77–87

基线SHA256：`4a7605681eb69bfb4369bcfc1ede418de5fec9d13104dc54a3d25ced75c4938f`。共享合并顺序：FR-B04-04 → FR-B04-05 → FR-B08-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/FeatureCacheR27.java`

动作：**MODIFY**。施工范围：bindModelSurfaces/modelSurface。

问题：缓存可能把过期面混到新目标。

修改方式：键含模型、ABI、目标与源版本；历史重放只用时点合法冻结面。

原方法锚点：FeatureCacheR27.bindModelSurfaces(ModelSurfaceProviderR30.Source source) L19–19；FeatureCacheR27.modelSurface(int row,int position) L20–20

基线SHA256：`b0c3072c8c03cedbf9130bb47a365b504c41c0b4d58f8b251248b31e82d45552`。共享合并顺序：FR-B04-05 → FR-B08-02。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`

动作：**MODIFY**。施工范围：LiveTask。

问题：真实模型消费必须与同一期冻结提交一致。

修改方式：消费回执和freeze同事务，缺面不自动补均匀值。

原方法锚点：RuntimeR27$LiveTask.id() L250–250；RuntimeR27$LiveTask.step(ResearchPoolR27.Worker w) L251–262

基线SHA256：`ce5740d0d39e3e7f1ec6824614348713ee258535a92b8a46a600e4a423c71d2b`。共享合并顺序：FR-B02-05 → FR-B03-04 → FR-B04-05 → FR-B06-02 → FR-B07-04。

## 实施步骤

1. 按model_surface实际叶子位置计required_count，不用有一条调用就标全匹配。
2. 从真实消费到下一期结算/校准的来源链可追。

## 测试与反证

- 一个/多个模型叶子、跨位置、重复冻结、过期面、缺artifact、相同概率错hash。

## 验收

适用消费100%匹配或明确BLOCKED；自然得分另行验证。

## 恢复与回滚

回退新叶子候选，原family预测/历史训练继续。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
