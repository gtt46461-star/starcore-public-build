# FR-B09-04 旧29图语义与真实资金纠偏

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-18。

前驱：B08。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR83.java`

动作：**MODIFY**。施工范围：actionCandles/algorithmBoard/bestAlgorithmCurve/calibration。

问题：资金K线源为探索+影子，事后最佳曲线有选择偏差。

修改方式：影子模拟资金明确改名；REAL只读真实唯一结算；榜单分角色，事后榜与事前冠军曲线区分。

原方法锚点：ChartsRepositoryR83.actionCandles(SQLiteDatabase db,CancellationSignal cancel) L86–119；ChartsRepositoryR83.calibration(SQLiteDatabase db,CancellationSignal cancel) L222–245；ChartsRepositoryR83.algorithmBoard(SQLiteDatabase db,CancellationSignal cancel) L420–443；ChartsRepositoryR83.bestAlgorithmCurve(SQLiteDatabase db,CancellationSignal cancel) L445–479

基线SHA256：`3097ec677cb207e81e4655471b789d0cef286bd6481dd3e9d7de2eaaafa501aa`。共享合并顺序：FR-B09-04。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java`

动作：**MODIFY**。施工范围：bss/rolling/trainHoldout/mainline/atoms。

问题：旧轮换模型图不能冒充新十位置模型效果。

修改方式：按ABI/model_hash/position/角色分层；计数标窗口，已验证改善0不画成变强。

原方法锚点：ChartCacheR27.bss(SQLiteDatabase db) L168–174；ChartCacheR27.rolling(SQLiteDatabase db) L175–182；ChartCacheR27.trainHoldout(SQLiteDatabase db) L183–193；ChartCacheR27.mainline(SQLiteDatabase db) L202–209；ChartCacheR27.atoms(SQLiteDatabase db) L230–236

基线SHA256：`cf209389817cfad6f88662be7b85e11bd2e2202665f4b20a6ea7da56f9055fde`。共享合并顺序：FR-B09-04。

### `app/src/main/java/com/ethk3/novaonepro/MainActivity.java`

动作：**VERIFY_THEN_CONDITIONAL_MODIFY**。施工范围：onCreate/onResume/主线程数据库路径。

问题：需核对当前UI延迟，不能预判主线程就是已测慢锁根因。

修改方式：先以Trace/StrictMode定位；已异步路径保留，仅证实阻塞时移到取消可恢复后台任务并保留生命周期epoch。

原方法锚点：MainActivity.onCreate(Bundle savedInstanceState) L468–535；MainActivity.onResume() L668–686

基线SHA256：`9ddac6537742e602708ea464c716cf7395f52cf49348220e41751f40cadb52be`。共享合并顺序：FR-B09-04。

## 实施步骤

1. 套用标签纠正hunk，再完成完整源/分母/角色解释。
2. 冷热/遗漏/号码K线保留作数据描述，参与研究时必须走独立特征/验证合同。

## 测试与反证

- 无REAL时资金图不得显示真实盈利；事后挑最佳有明确标识；新旧模型不混线。

## 验收

29旧图逐一裁决与报告一致，不丢已有有效显示。

## 恢复与回滚

只回退显示层，不改历史真实结算。

## 关键代码

顺序补丁：`patches/B09_research_graph_contracts.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR83.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
