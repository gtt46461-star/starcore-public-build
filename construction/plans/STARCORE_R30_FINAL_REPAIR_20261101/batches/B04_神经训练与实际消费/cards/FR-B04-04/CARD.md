# FR-B04-04 真实10×10概率面准备和发布

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-08。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java`

动作：**MODIFY**。施工范围：step/forecast。

问题：尚无已发布模型面，模型准备/冻结deadline容易混淆。

修改方式：只对ready模型发布不可变权重引用；trained/input/available时点和source revision统一。

原方法锚点：AtomNetRuntimeR30.step(RuntimeR27 rt,ResearchPoolR27.Worker w) L11–58；AtomNetRuntimeR30.forecast(ResearchPoolR27.Worker w,AtomNetSessionR30 state,ReplayDatasetR27 recent) L70–103

基线SHA256：`562ade6660d38a48a5dc831d550e8606c9befa6566168db404fabb86e7c054ba`。共享合并顺序：FR-B04-04 → FR-B08-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceProviderR30.java`

动作：**MODIFY**。施工范围：predict。

问题：排名分数不能称概率。

修改方式：只接真实网络10类输出，验证归一化、target与可用时间；不能用均匀面冒充训练结果。

原方法锚点：ModelSurfaceProviderR30.predict(Pit pit,FrozenModelRef model) L38–44

基线SHA256：`6695fd26064f74e576d59f5a5a6da59fc8a48ac85208632b8ad007b25c30c019`。共享合并顺序：FR-B04-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceStoreR30.java`

动作：**MODIFY**。施工范围：surface。

问题：artifact/surface必须同版本可追溯。

修改方式：原件及概率面原子提交，hash/ABI/源边界与发布水位一致。

原方法锚点：ModelSurfaceStoreR30.surface(ResearchWriteQueueR27.Batch b,ModelSurfaceProviderR30.Surface s,ModelSurfaceProviderR30.FrozenModelRef ref) L16–19

基线SHA256：`4a7605681eb69bfb4369bcfc1ede418de5fec9d13104dc54a3d25ced75c4938f`。共享合并顺序：FR-B04-04 → FR-B04-05 → FR-B08-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/NumberRankerAdapterR30.java`

动作：**VERIFY_AND_KEEP**。施工范围：probabilities。

问题：NumberQ原方案已统一到真实number head，不能另造一套排名权重。

修改方式：保留直接pClass、finite/归一化校验与ReplayEvaluator排序；新面接入后逐位一致性回归。

原方法锚点：NumberRankerAdapterR30.probabilities(AtomNetR27 net,double[] inputs) L7–10；NumberRankerAdapterR30.probabilities(AtomNetR27.Out output) L11–17

基线SHA256：`8b8496861082ff777b168c0c7e20078dd976479b33c1d0cfcfcf6470a65d34c8`。共享合并顺序：FR-B04-04。

## 实施步骤

1. 先完成历史预训练，再进入每期10位置在线阶段；输入迟到或模型过期明确BLOCKED。
2. 输出二元p_hit与number_class分布不同目标，不能互替。

## 测试与反证

- 真实权重前向、错ABI/错target/迟到model/不足历史、source修改；校验sum与finite。

## 验收

真实10位置面可复算，发布不领先成功训练提交。

## 恢复与回滚

停新模型引用并保留最后合法面，不伪造可用概率。

## 关键代码

本卡尚未给出完整生产实现；按本卡明确接口与测试施工。不能标“已完成”。

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
