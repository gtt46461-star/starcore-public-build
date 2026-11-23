# 图表、矩阵、逻辑表与检测报告施工合同

## 1. 当前已有目录不等于产物

095源码已有47个图表稳定ID（29个旧页面面板+18个研究视图）、19种R930矩阵+1种SU2矩阵、16个逻辑表。R34不以再增加大量空图作为完成。目录逐项核验，见 CHART_MATRIX_CATALOG.json；现有ID和语义继续保留，历史数据独立标层。

每个对象交付字段：objectId、producerClass/method、inputRole、datasetHash、model/policyHash、ABI、producerReceipt、产物hash、consumerOwner、actualConsumptionReceipt、后续子代/配置或正式只读作用、sourceIssue、sourceCommit、当前状态、首阻断owner和下一动作。

状态区分：WAIT_INPUT、BLOCKED_DEPENDENCY、RUNNING_NO_PRODUCT、PRODUCED_NOT_CONSUMED、CURRENT_MEASURED、HISTORICAL、READ_ERROR、INVALID_SOURCE。任何一个状态都不自动说明高品质。

## 2. 18研究图的实际用途

|ID|机器产物与研究用途|责任先落地批次|
|---|---|---|
|R01|多尺度状态与固定窗口条件收益，进入结构生成器|B06/B08|
|R02|事前结构概率与下一期落地误差，进入失败分析|B07/B08|
|R03|号码位置兼容/跨位结构，进入跨位置研究|B06/B08|
|R04|多位置候选交集支持与缩减，最终单位置K3|B06/B08|
|R05|失败簇→假设→实际AST子代|B08|
|R06|同日历原子边际贡献和移除对照|B08|
|R07|组合协同、冗余与复杂度代价|B08|
|R08|固定TRAIN probe上的行为多样性与配额|B06/B08|
|R09|专家建议→执行处方→子代归因|B08|
|R10|算子开发增益/CPU成本→预算实际回执|B08|
|R11|父子独立确认同期配对，formal read-only|B09|
|R12|父代共同失败/互补→实际融合新身份|B08|
|R13|prior-only校准、支持域、原ACT/父绑定|B07/B09|
|R14|梯度→权重→完整概率面→实际消费者|B07|
|R15|漂移/遗忘/回放价值，新旧模型同期预登记|B07/B09|
|R16|研究实测CPU/内存/预算与有效产物|B06/B08|
|R17|失败机制记忆→重复消耗减少与合法再测|B08|
|R18|出生→资格→唯一REAL→结算原键归因|B10|

B11汇总18项的正反场景和显示，不让B06—B10等待尚未施工的B11生产者。每个“有数据”必须可点击到真实原行/原件键；正式只读视图不要求伪造策略出生。

## 3. 20矩阵与16逻辑表

矩阵19种名称从MatrixRegistryR30.NATIVE读取，SU2保持LEARNED_FACTOR_TEACHER_CHECKPOINT原身份。依赖分SOURCE_ONLY/STUDENT/EVIDENCE；缺学生不阻塞来源独立矩阵。每类按同输入revision只产一次当前帧，更新才计算新版本，避免缓存无限增长。

16逻辑表的分页用绑定snapshot/filter/role的keyset，使用pageSize+1判hasMore；不能“满页=还有下一页”。取消需透传SQLite CancellationSignal，旧页面epoch回包不能覆盖新页面。显示n=0、未知n、读取失败为三种状态；有限制的最近窗口不冒充全生命周期。

## 4. 报告六级能力证据

1. DECLARED：类/方法存在。
2. CALLABLE：正确owner和输入前提具备。
3. INPUT_AVAILABLE：数据/模型/role/ABI/原件闭包满足。
4. PRODUCED：当前版本真实产物及durable receipt。
5. CONSUMED：指定消费者实际读取同hash，并有ACK/实际效果。
6. EFFECTIVE：在预登记独立自然证据上满足该能力的效果条件。

每级分别展示，不能只给一个“已实现”百分比。165索引覆盖不代表165能力有效，16诊断动作覆盖不代表16修复已执行。

## 5. 可施工诊断记录

每条诊断结构：caseId、R34IssueId、原sourceCommit、reportSnapshot、runtimeSampleTime、原件键/hash、首阻断、ownerClass/method、证据等级OBSERVED/SOURCE_CONFIRMED/REPRODUCED/HYPOTHESIS、最小复现材料、修复文件、实际patchHash、验收输入/预期拒因、后续观察窗、当前状态、缺证及取得方法。

缺原件/未数值复演不得把原因写成ROOT_CAUSE_PROVEN。源码里只反射到方法存在，应输出DECLARED_RUNTIME_UNVERIFIED。报告建议引用本R34问题卡而非继续指向R31旧卡；旧卡保留历史关联。

## 6. 报告体积与一致性

原本每份重复42.8MB源码调用索引，应按sourceCommit去重保存/引用，紧凑报告保留精确定位和索引摘要。完整原件导出另列，不把rawEvidenceIncluded=false的紧凑报告说成原SQLite已导出。原库快照需一致、只读、预留峰值空间，诊断和主链互不抢无界事务。

新报告逐R34问题列修复前值、修复后同口径值、真实分母、增量、证据状态与未完成条件。某图新增一条样例或旧缓存更新，不能签“研究恢复”。
