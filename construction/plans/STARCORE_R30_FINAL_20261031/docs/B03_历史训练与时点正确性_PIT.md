# B03 历史训练与时点正确性（PIT）

<!-- CARDS -->

**目标**：历史回放与实时冻结共用一套PIT守卫；分割与曝光账防止确认段被重复使用；增量特征与可恢复训练游标。

**批次出口**：PIT违规=0（含故意注入的反例被拒）；训练续训结果与不中断一致（登记容差内）。

**versionCode**：577310031（批次内每次重新出包 +1，不得回退）

**本批次顺序共享文件**（同一文件被本批次两张卡改动：只能按卡序先后施工，改动方法互不重叠）：

- `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayEvaluatorR27.java`：R30-B03-01（回放样本生成） → R30-B03-06（评价入口（与 B03-01 不同方法））

### R30-B03-01 PIT守卫接入所有冻结写入

- 批次内顺序：第 1 张；前置：R30-B02-05；对应R29：R29-015, R29-016；指标：M18, M19

**独占文件**

|文件|动作|改动范围|bb607cf 行数|
|---|---|---|---|
|`app/src/test/java/com/ethk3/novaonepro/strategyv2/R30PitGuardTest.java`|ADD|—|—|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/PitGuardR30.java`|ADD|—|—|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java`|MODIFY|freeze(...)（bb607cf:80）写 Batch 之前|237|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayEvaluatorR27.java`|MODIFY|回放样本生成|147|

**施工步骤**

1. 复制 PitGuardR30.java。DDL：CREATE TABLE IF NOT EXISTS r30_pit_violation(policy_hash TEXT,target_ordinal INTEGER,code TEXT,source TEXT,recorded_at INTEGER,PRIMARY KEY(policy_hash,target_ordinal,code))（加进 SchemaR27.ensure）。
2. LiveShadowsR27 每条冻结、ReplayEvaluatorR27 每个回放样本：构造 Freeze → check → 非空则不写该冻结/不计该样本，写 r30_pit_violation。回放中 deadlineAt=该期计划截止，frozenAt=deadlineAt-5000（回放模拟口径），modelAvailableAt=模型训练截止期的计划开奖时间。

**本包代码**：`code/main/com/ethk3/novaonepro/strategyv2/PitGuardR30.java`

**测试**：pitGuard* HOST 测试；反例注入：故意让一个回放样本 trainedThrough=target，必须被拒并入账

**验收（全部满足才可标DONE）**

- 真机/回放 r30_pit_violation 只含注入反例

**禁止**：不得删除类、DROP表、清空模型或训练数据（保全规则）；不得放宽任何时间/数据/统计门槛来让测试通过；不得把模拟器/HOST结果写成真机结果；未测写NOT_RUN；不得在持有数据库写锁时做网络请求、sleep、等待其他线程或进入其他synchronized块；不得修改本卡未列出的文件；需要时新开卡并登记

**提交信息格式**：`R30-B03-01 <一句话>`；提交后运行 `python3 tools/r30/ownership_check.py --card R30-B03-01 --base <上一卡提交>`。

### R30-B03-02 真实历史数据治理

- 批次内顺序：第 2 张；前置：R30-B03-01；对应R29：R29-014；指标：M15, M18

**独占文件**

|文件|动作|改动范围|bb607cf 行数|
|---|---|---|---|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/HistoryImportR27.java`|MODIFY|导入校验|55|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayDatasetR27.java`|MODIFY|dataset hash|175|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/StoreR27.java`|MODIFY|draws 热库上限|131|

**施工步骤**

1. 导入：期号连续性、10个号码为1..10排列、重复期去重、冲突隔离到 r27_fetch_reject（已有表）。
2. dataset hash = sha256(首期|末期|行数|逐行规范化串)；数据集换代时旧 hash 的指纹/资格不跨代比较。
3. 热库 draws≤5000（M15），超出部分进压缩历史块（现有 ArchiveStoreSU1），回放从块读取。

**测试**：导入乱序/重复/缺期/跨日 fixture

**验收（全部满足才可标DONE）**

- fixture 全通过
- 热库行数≤5000

**禁止**：不得删除类、DROP表、清空模型或训练数据（保全规则）；不得放宽任何时间/数据/统计门槛来让测试通过；不得把模拟器/HOST结果写成真机结果；未测写NOT_RUN；不得在持有数据库写锁时做网络请求、sleep、等待其他线程或进入其他synchronized块；不得修改本卡未列出的文件；需要时新开卡并登记

**提交信息格式**：`R30-B03-02 <一句话>`；提交后运行 `python3 tools/r30/ownership_check.py --card R30-B03-02 --base <上一卡提交>`。

### R30-B03-03 分割角色与曝光账

- 批次内顺序：第 3 张；前置：R30-B03-02；对应R29：R29-015；指标：M18, M19

**独占文件**

|文件|动作|改动范围|bb607cf 行数|
|---|---|---|---|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/SplitR27.java`|MODIFY|角色划分|52|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/HoldoutLedgerR27.java`|MODIFY|曝光记录|51|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomScoreR27.java`|MODIFY|评分只读 H_TRAIN/H_SELECT|63|

**施工步骤**

1. H_TRAIN/H_SELECT/H_CONFIRM 三段按 dataset hash 固定；H_CONFIRM 每个候选只允许被读取一次，读取即写 HoldoutLedgerR27（policy_hash, dataset_hash, range, read_at）；第二次读取抛 R30_CONFIRM_REUSED。
2. AtomScoreR27 只能读 H_TRAIN/H_SELECT（代码断言）。

**测试**：同一候选二次读确认段被拒

**验收（全部满足才可标DONE）**

- 训练分割污染=0

**禁止**：不得删除类、DROP表、清空模型或训练数据（保全规则）；不得放宽任何时间/数据/统计门槛来让测试通过；不得把模拟器/HOST结果写成真机结果；未测写NOT_RUN；不得在持有数据库写锁时做网络请求、sleep、等待其他线程或进入其他synchronized块；不得修改本卡未列出的文件；需要时新开卡并登记

**提交信息格式**：`R30-B03-03 <一句话>`；提交后运行 `python3 tools/r30/ownership_check.py --card R30-B03-03 --base <上一卡提交>`。

### R30-B03-04 增量特征与回放

- 批次内顺序：第 4 张；前置：R30-B03-03；对应R29：R29-016；指标：M03, M11, M18

**独占文件**

|文件|动作|改动范围|bb607cf 行数|
|---|---|---|---|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/FeatureCacheR27.java`|MODIFY|FamilyStream 增量喂入；rows [0,fed) 不变断言|310|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java`|MODIFY|每期增量特征调度|330|

**施工步骤**

1. FeatureCacheR27 现有注释要求 rows[0,fed) 不变：加断言（hash 前缀比对，每100期一次），失败则整缓存重建并记 r30_cache_rebuild。
2. RuntimeR27 每期只追加新一行特征，不全量重算；缓存键=(dataset hash, trainedThrough, feature ABI, position)。

**测试**：增量与全量结果逐位一致（HOST，用 ReplayDatasetR27 小样本）

**验收（全部满足才可标DONE）**

- 一致性测试通过

**禁止**：不得删除类、DROP表、清空模型或训练数据（保全规则）；不得放宽任何时间/数据/统计门槛来让测试通过；不得把模拟器/HOST结果写成真机结果；未测写NOT_RUN；不得在持有数据库写锁时做网络请求、sleep、等待其他线程或进入其他synchronized块；不得修改本卡未列出的文件；需要时新开卡并登记

**提交信息格式**：`R30-B03-04 <一句话>`；提交后运行 `python3 tools/r30/ownership_check.py --card R30-B03-04 --base <上一卡提交>`。

### R30-B03-05 训练游标与可恢复状态

- 批次内顺序：第 5 张；前置：R30-B03-04；对应R29：R29-017；指标：M10, M20

**独占文件**

|文件|动作|改动范围|bb607cf 行数|
|---|---|---|---|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetTrainerR27.java`|MODIFY|checkpoint 保存/恢复|84|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomWorldR27.java`|MODIFY|训练游标|243|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelArtifactR29.java`|MODIFY|artifact 发布|103|

**施工步骤**

1. checkpoint=权重+优化器状态+RNG状态+游标，一次原子发布（先写临时文件→fsync→rename→提交引用行）。
2. 恢复后继续训练 N 步的结果与不中断训练 N 步一致（浮点容差登记在 ModelArtifactR29.TOLERANCE）。

**测试**：中断/恢复一致性 HOST 测试

**验收（全部满足才可标DONE）**

- 一致性在容差内；不重复学习同一事件

**禁止**：不得删除类、DROP表、清空模型或训练数据（保全规则）；不得放宽任何时间/数据/统计门槛来让测试通过；不得把模拟器/HOST结果写成真机结果；未测写NOT_RUN；不得在持有数据库写锁时做网络请求、sleep、等待其他线程或进入其他synchronized块；不得修改本卡未列出的文件；需要时新开卡并登记

**提交信息格式**：`R30-B03-05 <一句话>`；提交后运行 `python3 tools/r30/ownership_check.py --card R30-B03-05 --base <上一卡提交>`。

### R30-B03-06 统一回放评价器

- 批次内顺序：第 6 张；前置：R30-B03-05；对应R29：R29-018；指标：M18, M19, Q05, Q06

**独占文件**

|文件|动作|改动范围|bb607cf 行数|
|---|---|---|---|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayScoreR27.java`|MODIFY|评分|67|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayEvaluatorR27.java`|MODIFY|评价入口（与 B03-01 不同方法）|147|
|`app/src/main/java/com/ethk3/novaonepro/strategyv2/EvalContextR27.java`|MODIFY|scores 缓存键含 dataset hash|58|

**施工步骤**

1. 注意：ReplayEvaluatorR27 在 B03-01 已改样本生成，本卡只改评价入口。
2. 所有回放评分统一走 ReplayEvaluatorR27.evaluate(policy, role)；scoreMemo 键加 dataset hash，避免换代后命中旧缓存。

**测试**：同策略同数据集两次评分完全一致；换数据集缓存失效

**验收（全部满足才可标DONE）**

- 测试通过

**禁止**：不得删除类、DROP表、清空模型或训练数据（保全规则）；不得放宽任何时间/数据/统计门槛来让测试通过；不得把模拟器/HOST结果写成真机结果；未测写NOT_RUN；不得在持有数据库写锁时做网络请求、sleep、等待其他线程或进入其他synchronized块；不得修改本卡未列出的文件；需要时新开卡并登记

**提交信息格式**：`R30-B03-06 <一句话>`；提交后运行 `python3 tools/r30/ownership_check.py --card R30-B03-06 --base <上一卡提交>`。
