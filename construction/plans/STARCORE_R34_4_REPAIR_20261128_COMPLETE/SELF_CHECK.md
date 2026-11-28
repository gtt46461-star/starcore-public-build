> 2026-11-28完整授权发布：原始诊断、全部审计与5组候选均随原包分片保存在仓库，使用prepare_evidence.py自动还原诊断后核验。新增B00构建交付附约和完整施工指令；本轮是资料交付，不冒签产品已施工。旧原包与旧公开目录保持历史原样。

# 本轮实际自检、已关闭方案缺口与验收限制

## 原完整交付包已执行的结论（保留历史证据）

本包已完成报告原件完整性、46项问题/50条原发现/12批无环依赖、136源码锚、1407生产Java基线、96检查/44指标/165对象/826表/18研究图、旧44问题/1832需求/299卡、5候选累计应用与前后hash验证。应用源码保持未改。原封包的`PLAN_CHECK.json`在不可变ZIP中保留；本目录同名文件为本次重新封包验证结果；即使方案检查通过，也不表示产品返修已经完成。

|原交付时执行（历史）|结果|对应记录|
|---|---|---|
|原报告外/内ZIP、archive manifest、56载荷长度/SHA/CRC|一致；已用包内工具重新解包|evidence/REPORT_INTEGRITY.json、validation/unpack_report.json|
|结构/全量覆盖/继承/精确源字节/最终文件清单|历史源码去重1417文件核验见原ZIP内complete_plan/validation/PLAN_CHECK.json；当前清单见本目录PLAN_CHECK，当前源码未重跑|原完整ZIP中的历史PLAN_CHECK；本目录validation/PLAN_CHECK.json为重新封包结果|
|5候选顺序应用到临时副本与前后SHA|全部通过；5生产文件+2测试文件；未改产品|validation/candidate_cumulative.json|
|覆盖SQL合成夹具|50,000事件、100筛选组结果一致；主机索引顺序符合候选目的|validation/sql_equivalence.json|
|scope身份字段|原报告300字符JSON确已截断；5种字符串情况通过|validation/scope_identity.json|
|结算原元数据JOIN|163活跃均LIVE_RETRY/当前无shadow/canonical匹配；162热窗外、1热窗内|validation/settlement_evidence.json|
|writer分类实际方法抽取Java|30例，baseline1个预期失败，候选0失败|validation/writer_busy_java.txt|
|R18实际投影方法抽取Java|12例通过，含无证/错键/待结算/未知/赢输|validation/r18_java.txt|
|覆盖独立复核|68项检查通过，原字段与状态保留|audit/final_coverage_review.json|
|策略最终合入复核|16卡完整；相关60文件SHA正确；6项设计问题关闭|audit/final_strategy_merge_verification.json|
|运行/导出复核封板项|native锚、catalog兼容、局部门、候选脚本与原日志均闭合|validation/REVIEW_CLOSURE.json|
|应用保全|两个工作树相对各自HEAD的tracked app均无修改|validation/SOURCE_UNCHANGED.json|
|当前文档链接|跳过代码/数学内联后核当前包Markdown相对链接|validation/LINK_CHECK.json|

主机命令总表：[HOST_VALIDATION.json](validation/HOST_VALIDATION.json)。该表7条根代理重跑命令均exit0；元数据检查、字符串夹具和Java方法抽取有不同验证范围，不把它们混称为完整Android测试。

## 自检中发现并已经纠正的方案问题

1. **底座偏差**：main旧应用与211不同，改为211隔离工作树；本轮只发布计划。
2. **训练循环门**：解除B06对全部旧检查点恢复的整体依赖，加入REAL=0/证书=0/全NO_PREDRAW下独立DEV正例。
3. **ColdRebirth误用**：已推进旧cursor仍须旧state原件；缺失时隔离旧root，另走完全独立基础实验，不能删证明。
4. **结算与反馈混淆**：NO_PREDRAW不能冒充COMPLETED；现代rolling真实事件门保留，开发入口独立。
5. **源码因果夸大**：精确区分loadCurrent返回null的分支与仍抛出的异常；长期gap恢复使用新epoch，旧记录不改写，允许追加真实迁移journal。
6. **native实际pin遗漏**：补真实C文件、JNI/包装层、同域时钟和旧job无锚兼容，不只画Java数据结构。
7. **报告外部枚举破坏风险**：保留原catalog枚举；内部等待状态另记，检查不全PASS仍能导出。
8. **快照清理过度门**：DIAGNOSTIC重试成功的工作副本按原profile/语义/引用证明回收，不要求强装5GBraw；独有原件/FULL_RAW仍保护。
9. **图表—质量依赖循环**：B08工程与PENDING_B09自然回填分层；自然主链只要求对应B04工程前提，不等全部长测再开工。
10. **指标生产者遗漏**：补AtomNet、CP9/rolling恢复、校准、自然父子claim、证书和REAL消费；读取修复与生产责任分列。
11. **最低样本数误读**：保留role/window/unit/target与原定义，Q05/Q07/Q08均不以一个计数代替完整资格。
12. **旧修复被重复推翻风险**：173旧038等已有读层修复明确保留；49相同文件不再重套，15后续变化逐项审阅。
13. **生成结构瑕疵**：指标proof对象已拆平，别名归并后的工单ID去重，所有正文链接和包内文件再次校验。

各专项初次复核文件是带当时SHA的历史发现，不应把其中“当时待生成”的文字当当前未关闭项。最终关闭以validation/REVIEW_CLOSURE、最终策略/覆盖复核及当前PLAN_CHECK为准，原发现没有删除。

## 尚未执行，不能签收的层级

- 未做完整Android构建、完整JUnit/instrumentation、真实SQLite迁移或APK覆盖安装。
- C04新增JUnit只写入候选，未运行；原.scdev和模型/训练原字节未包含在本诊断包。
- Java夹具没有执行Android、生产SQLite、原writer准入调度或真实5GB数据库查询。主机SQL等价不保证目标设备性能。
- 未在用户设备真实排空故障、恢复163待办、完成CP9 TRAIN或图表生产，没有修改任何运行台账。
- 24/72小时后台连续性、重复/取消/低空间导出和独立自然未来质量均待施工后取得原件。
- 原报告DIAGNOSTIC不含原SQLite、安装APK、完整模型与训练原件；缺件限制已进入每项验收。不能保证策略长期高胜率或每一代必然改善。

## 自包含封包规则

MANIFEST.json列出本计划全部应封存文件的路径、字节和SHA256；仅排除清单自身、其摘要及最终验证结果，避免自引用。候选日志、原诊断ZIP、审计、问题卡和合同均列入清单。PLAN_CHECK记录清单实际验证；签收者可重新运行只读工具复核。

## 本次完整授权发布与重新自检

2026-11-28用户已授权所有材料。原20,814,251字节交付ZIP及263成员保持原字节；仓库新增27片与严格还原脚本，C04与原始报告一并完整提供。当前_COMPLETE目录更新分发口径、B00实际执行合同、创造训练附约及详细施工指令；原完整ZIP和旧公开目录不改。

本次工作区重新从原ZIP恢复资料，没有211应用工作树。`validation/PLAN_CHECK.json`对当前完整计划/原报告/候选清单/文件SHA重新核验，源码层为NOT_RUN、候选执行为NOT_RUN，不冒充历史源锚/主机测试重新运行。原历史日志按原范围保留。当前新增还原/物化脚本结果见`validation/PUBLICATION_CHECK.json`；它只验证资料交付，不验证Android产品。

完整MANIFEST包含自动还原的原诊断；必须先运行`tools/prepare_evidence.py`。执行输出与版本journal放在冻结计划外。APK构建、设备安装、5GB库实跑、自然未来与实际盈利均未在本次资料上传中验证。

## 修订2：已复现缺口与真实关闭范围

第二轮确认14项资料工具/执行合同缺口，修订说明见[REVISION_2.md](REVISION_2.md)。当前152个源码锚为原136个加16个已存在的211 owner；新增源字节均按精确211读取并与原1407清单核对。原1407文件清单未改，当前没有重新声称完整应用源码/Android通过。

- `validation/SECOND_PLAN_TOOL_REGRESSION.json`：19项实际工具I/O回归通过，含禁止覆盖源码/计划/已有回执、symlink输出拒绝、普通及优化模式的损坏hash拒绝、缺必需清单/空候选/错清单摘要拒绝、真实原报告56载荷解包读回及无bytecode污染。回执记录实际测试工具SHA和临时清单SHA；最终清单另行封存核验。
- `validation/SECOND_CANDIDATE_CUMULATIVE.json`：重新下载并核SHA的6个211候选前态输入，5候选在临时副本累计应用通过，7个声明后态文件正确，输入未改。这只证明局部补丁适用性，不等于完整1407源码复核、编译或设备运行。
- `audit/second_strategy_merge_check.json`与`audit/second_coverage_merge_check.json`：分别13组及16组定向合入检查通过；46卡当前条款及ACCEPTANCE同步，原报告/原发现未改，既有native桥接保全。
- 开工Bash的计划pin、独立Git blob字节、失败即停及原包物化正反例记录在`validation/SECOND_BOOTSTRAP_REGRESSION.json`；仅资料前置，真实211完整checkout/构建部分不冒签。

本版验证器要求MANIFEST、其SHA封印和完整5候选目录；所有验证输出只能写到冻结计划/明确source之外的新位置。历史ZIP中的旧脚本保留审计原字节，施工使用本修订2目录的现行工具。由封包步骤把外部验证回执放入预留的PLAN_CHECK位置，不由只读验证器覆写冻结输入。Android、正式APK、用户设备、5GB库实跑与自然未来仍为待验。
