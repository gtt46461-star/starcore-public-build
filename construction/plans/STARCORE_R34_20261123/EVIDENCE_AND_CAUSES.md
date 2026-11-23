# 三报告对照、根因和施工优先级

完整22项原始发现见 [evidence/report_audit.md](evidence/report_audit.md)，结构化原值见 report_metrics.json，完整原字段片段见 evidence/check_evidence/。源码根因见 [evidence/runtime_audit.md](evidence/runtime_audit.md) 与 [evidence/learning_design.md](evidence/learning_design.md)。所有报告简称只映射到本次三个不可变原ZIP；不把不同采样时刻拼成同一原子快照。

## 1. 先修阻断链

|层|现场证据/源码事实|后果|责任批次|
|---|---|---|---|
|来源与写入|HTTP_REQUEST_PAYLOAD_IDENTITY_MISMATCH；PAYLOAD:WRITE_REJECTED:BUSY_DEADLINE|响应取得但合法来源绑定/开奖事务可能失败|B01、B02|
|启动与写者|CLEANUP后仍全transform；研究线程在保护表定义检查|迁移/保护扫描长事务可能挤占关键写入|B01|
|恢复|writer backlog进入DEPENDENCY但Deferred旧分类不识别；失败记账也走RESEARCH_DATA|正常等待被计失败、原件排债不收敛|B01|
|输入|最新报告HISTORY_BOOTSTRAP_PENDING、contiguous=1，而总draws=5000|历史条数够不等于当前模型连续窗口够|B02|
|主链|draw head=288，analysis/strict OOS=119；最终待结算162为较早运行采样|下一期事前冻结稀少，严格未来样本无法持续累积|B03、B04|
|候选|旧ABI被标BLOCKED_WARM_NUMERICAL_ABI_PRESERVED；两类旧owner没有重生处理器|有历史候选却没有当前可训练子代|B06|
|训练|AtomNet cursor3250、steps56三次不变；model artifact/call/surface=0|训练记录和出版计数不能证明可消费模型|B07|
|研究|Foundry/replay/evolution被共享writer积压阻断；generation/lineage=0|无法形成持续的策略更新链|B06—B08|
|评估|policyObjects/futurePeriods/independentFutureIssues/activeCertificates=0|没有自然优势证据，不能释放真实出手|B09、B10|
|呈现|18研究图当前帧/消费回执0；20矩阵inputAvailable=false|应修生产者和消费者，单改UI不能得到真实数据|B06—B11|
|存储|677对象中556缺失、18未核验、unknownChildSets574|旧依赖不可假恢复或擅删；总DB约1.4GB仍需治理|B05|

表中“可能挤占”的贡献需要同版本耗时trace定量；不将一个线程栈等同于全部P95归因。

## 2. 三时点事实

|指标|A 870cd99|B 89d1cac|C fabedd0|
|---|---:|---:|---:|
|运行采样（北京时间）|09:20:59|11:23:30|12:45:39|
|报告事实采集|09:25:50|11:59:29|12:53:23|
|latestIssue尾号|0080|0201|0288|
|核心预测/严格OOS尾号|0080|0119|0119|
|影子预测/结算|881/877|886/882|886/882|
|旧R26训练行|3453|3453|3453|
|旧R26反馈行|2958|3053|3114|
|新模型artifact/call/surface|0/0/0|0/0/0|0/0/0|
|自然机会/证书|0/0|0/0|0/0|
|图表缓存行|115831|116324|116463|

这些是保留行数，含淘汰/归档的表不能单靠差分判执行次数。我们同时核任务游标、真实产物与消费者。C区间(0080,0288]实存开奖156条、影子冻结5条，期号差208不是开奖条数，也不是自然资格覆盖分母。

一秒节拍指标C：targetSlots12362、wakes370、missed_tick11993、前工作超时/正常超时11892。该口径是服务实例节拍，不是漏开11993期，也不是网络完成统计。分析P95=36776ms只有6次样本，只能说明已记录的失败，不能当长期稳定分位数。

## 3. 报告目前能做到和欠缺什么

报告已经有能力索引、缺口、原件状态、建议方向和实现owner，但其覆盖率不是实际运行率。最新96检查是43 OBSERVED、16 N/A、20 NOT_PROVEN、7 WARN、4 PASS、6 FAIL；44工程/质量指标没有verifiedMetricIds。165对象覆盖和16规则actionCoverage=1只是声明/检查覆盖。

R34要求报告给出：原字段与原件键→首阻断→实际调用owner→证实等级→最小复现输入→对应施工问题/文件/方法→本次代码差异→同口径后续结果。观察到共享writer hold可列首阻断，不可据此宣称所有具体模型数值算法都已诊断。缺原件必须写待补证及取得路径。

不能混淆：caught工作失败/uncaught崩溃/OS退出；权限齐全/后台持续；checkpoint有hash/语义可恢复；方法存在/实际执行；publication行/模型发布；出生/选择/自然合格/真实结算。

## 4. 修复优先级

先固定095身份→迁移和writer恢复→HTTP绑定和连续历史→canonical与冻结结算→资源和原件→冷启候选→真实训练发布→研究进化→自然资格→主链应用→逐图及报告核验→真机耐久。

研究图/矩阵的机器生产和实际消费随B06—B08一起施工；B11负责呈现与全目录覆盖，不成为前批的未来依赖。所有前批用本批已可用的输入及隔离夹具独立验收，后批不能补签前批已失败项目。
