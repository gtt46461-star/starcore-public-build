# R31 已组装总包独立终审

审阅时点：2026-11-04。范围：docs/00–03、ledgers/acceptance_config.json、package_summary.json、user_requirements_40.json，以及cards.json中运行、存储、研究、图表、主链、设备验收关键卡。仅记录审阅结论，未改交付文件。

## 结论

核心事实和能力升级方向基本正确，明显优于直接照搬参考方案。13项R27全隔离、33/44不足、96执行不等于96通过、55张keep现场全空、当前结算已到202611030010、SHADOW与REAL隔离、48原成员校验边界等均已正确表达。总纲与验收说明没有承诺真实未来高胜率或“一代必胜一代”，也明确本轮仅交方案及参考合同，未冒充已修复APK。

**仍须修正机器依赖环、一个时延口径错误及静态索引证据强度表述。** 下列R1–R3建议交付前关闭；R4–R6完善方案的严格性和可执行性。

## R1 / 高：cards.dependencies含循环及非规范依赖，执行器无法得到明确施工拓扑

将批次ID展开为该批全部卡后，检出两个强连通分量：

- `R31-B01-C01 ↔ R31-B02-C01`。可作为明确的同版联合实施组，但需将组内联改关系与前置实施依赖分开。
- 21卡跨阶段循环：`R31-B06-C03, R31-B07-C05, R31-B08-C02, R31-B08-C03, R31-B08-C04, R31-B08-C05, R31-B09-C03, R31-B09-C04, R31-B09-C05, R31-B10-C01, R31-B10-C02, R31-B10-C03, R31-B11-C01, R31-B11-C02, R31-B11-C03, R31-B11-C04, R31-B12-C01, R31-B12-C02, R31-B12-C03, R31-B12-C04, R31-B12-C05`。

主要形成路径：B06-C03依赖B11，B11-C01又依赖B06；B10-C03依赖整个B12，而B12依赖B10；B10-C01依赖B09，B09多卡又依赖B10；B07-C05与B10-C02彼此依赖。总纲虽描述分阶段和同发，机器账本不能将这些全部表示为同一类前置依赖。

另14项dependency是自由文本，例如`B11报告profile契约`、`B10 typed selection migration gate`、`B12安装升级恢复`；若校验器只按精确ID检查，可能被忽略或误解。

建议改为：

1. `implementation_dependencies`：仅引用规范card/phase ID，必须DAG；共享接口先新增独立schema/contract阶段，不以整批完成作为先决条件。
2. `joint_acceptance_group`：B01/B02联改组与B05/B10迁移组。
3. `publication_dependencies`：B12真机与自然证据，阻断发布但不阻断前序实现。
4. `destructive_action_prerequisites`：只有typed选择正式切换后，B05缓存GC才能启用；只有恢复验证后备份才能删除。
5. 对规范ID存在性、组内允许关系、实施图无环、所有卡可达/可排序做自动自检；附真实拓扑表。

## R2 / 中：B03-C01仍混淆37.4秒的计量边界

`R31-B03-C01.issue`写“请求到入库P95约37.4秒”。原报告的`requestToCommitP95=37373ms`是分析请求至事务返回/commit口径，不能用来证明HTTP抓取响应至开奖落库耗时37.4秒。

改为：“报告存在抓取写拒绝；另记录分析请求→事务返回P95=37,373ms。抓取接收→校验→开奖提交需独立测量，不能混用两项指标。”同步检查所有生成的cards/*.md与HTML。

## R3 / 中：静态调用索引不能证明可编译

docs/00第3节末写“静态调用索引只证明可编译调用关系”。索引可以过期、来自生成器或只记录静态词法引用，无法单独证明可编译。

改为：“静态调用索引只提供特定版本的静态引用线索；编译与运行覆盖均需独立证据。”

## R4 / 中：内存验收仍缺可判定的阈值或校准程序

磁盘budget已经明确且具备保护债务边界。Java heap/native/PSS/RSS则只写“预热后72h峰值/趋势和gc后存活对象验收”，acceptance_config未提供PASS/FAIL边界。仅记录趋势不能阻止持续泄漏通过验收。

建议增加独立内存验收卡/配置：目标设备基线采样协议、预热长度、固定工作负载、按GC后存活堆的比较窗口、允许最大增长/斜率、任务/模型/图表缓存内存上限、系统low-memory/trim响应、OOM及不可解释单调增长的阻断条件。数值若须实测校准，应明确“B00锁定并签入配置后开始72h试验”，不能在看过失败曲线后调门限。

## R5 / 中：个别既有文件方法仍是占位描述

`R31-B12-C01`对SchemaDomainCoordinatorR926使用`schema migration coordination`，没有具体方法锚点。可直接使用已取源码的`ensureInfrastructure`(210)、`verifyAndStamp`(281)、`prepareRepairIfDrift`(312)、`runStep`(386)、`verifyObjects`(494)，并明确哪些新增R31迁移契约。

`R31-B12-C04`的DbStore方法是`affected methods only after coverage map`。若这是受控退役的条件性后续工作，应显式标`DEFERRED_TARGET_SELECTION_AFTER_COVERAGE`，不能被“全部精确到方法”的统计涵盖。

另两个可快速补足的空锚点：RetentionArchiveIoCP12.readReceipt在106行；SourceReadLeaseR82.exclusive在29行。其方法真实存在，不是源码缺失。

## R6 / 低：统一性能时延起点与阈值优先级

acceptance_config的`validated_response_to_draw_commit`应在术语中说明“有效响应接收时刻”还是“校验完成时刻”，避免把解析/校验耗时排除。建议同时保留first-response-observed、validated、draw-commit三个阶段，以总接收→提交为总SLO，校验阶段另报。

总纲称存储专题预算为准，acceptance_config则称自身和总纲优先。宜只保留一个明确优先链：`acceptance_config → 总纲解释 → 专题示例`。现数值无实质冲突，但机器验收不应依赖措辞推断。

## 能力升级与需求覆盖审查

不是只有bug修复。B06包含历史输入修订、持续回放、完整checkpoint和AtomNet十位置水位；B07包含家族/专家保全、有限typed AST、因果语义去重、研究预算、父子同机会比较及全工厂正负对照；B08包含真实矩阵产物、缺学生时分kind推进、typed消费与16逻辑研究表；B10包含权威主链选择迁移、门槛功效和撤销。均有具体文件/方法、产物和验收要求，能够对应用户能力升级诉求。

未见把新增类冒称既有类：拟新增文件用PROPOSED_NEW_FILE标注；自然质量保持NOT_PROVEN；历史100简单候选实验明确不代表生产R27/SU2工厂。40用户要求属于归纳后的设计映射，状态DESIGN_MAPPED_NOT_IMPLEMENTED与NOT_RUN适当，不能在最终答复改称40项已经完成软件实现。

## 建议交付前核查结果

修正R1–R3后重新生成JSON、逐卡Markdown及HTML；重跑包内完整性/覆盖自检，补无环依赖检查。R4–R5宜纳入同次修订。该审阅不是生产实现、设备验收或自然质量证明。
