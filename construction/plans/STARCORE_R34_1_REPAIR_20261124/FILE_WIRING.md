# 全部施工文件的用途和签收入口

第 6 版在原文件责任内追加 36 个实施文件的具体改动；各文件原有调用与核验继续生效。[最新工单变更](FIELD6.md) · [逐工单方法](WORK_ORDERS.json)

按原工单复用正常调用与已有原件。表中带“补入”的文件是第 3 版只列有文件卡的路径。历史文件有明确替代入口的，读取替代实现。

| 文件（原路径） | 有效入口（不同时列出） | 工程用例 | 要核实的行为 |
|---|---|---|---|
| 补入 `CURRENT_FINAL_PLAN.md` | 同左 | `A-B12-03a` | 构建任务从根入口读取本次方案身份；错封印指向旧目录时拒收 |
| `app/build.gradle` | 同左 | `R341-B01-01-T01` | 保留 R31/R33.5 封印校验和原 assets；增加 r341 identity asset 的生成及最终输入一致性校验，禁止用 R34.1 目录冒充 R33.5 root。；REV-B01-01 把三个方案根分别绑定到实际构建输入和产物。实施入口与反例见 specs/review/build.md / R341-B01-01-T01。 |
| `app/src/main/AndroidManifest.xml` | 同左 | `R341-B05-03-T01` | 核对现有前台服务与receiver声明、stopWithTask=false；只有实机证实需要且符合目标API约束才修改，不以提升targetSdk替代本轮修复。 |
| `app/src/main/java/com/ethk3/novaonepro/AnalysisInputEnvelopeR341.java` | 同左 | `R341-B04-02-T01` | NEW：包含base/target、ordered history+draw revisions、historyHash、requestHash、generation/fence、input schema、固定deadline及source scope；只允许不可变值。 |
| `app/src/main/java/com/ethk3/novaonepro/AnalysisRequestStoreR36.java` | 同左 | `R341-B01-05-T01` | 只追加phase和reason诊断引用，现有PIT身份/固定deadline保持不变。 |
| `app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java` | 同左 | `R341-B01-05-T01` | 记录owner/fence和claim/release关联；真实方法签名以基线为准，不改变租约协议。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/AppConfig.java` | 同左 | `R341-B09-04-A01` | 合法单位置三个号码传入决策门；重复号码或非法位置被拒 |
| `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java` | 同左 | `R341-B01-03-T01` | 启动 owner 前后记录 ownerGeneration、thread state 和重试due；记录服务未创建/被取消路径。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/BackgroundRuntimeCapabilityR811.java` | 同左 | `R341-B05-02-T01` | 服务观察反映下一业务提交；仅通知或心跳存在不算恢复 |
| `app/src/main/java/com/ethk3/novaonepro/BootReceiver.java` | 同左 | `R341-B05-01-T01` | 外部恢复只递交幂等start intent并尊重userEnabled；不会作为正常每期分析唯一触发。 |
| `app/src/main/java/com/ethk3/novaonepro/BuildIdentity.java` | 同左 | `A-B11-02a` | 报告scope只接受已通过当前build identity核对的installed/source/plan；身份无法核对时具体为UNVERIFIED_IDENTITY，不默认当前源码等于安装。 |
| `app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java` | 同左 | `R341-B04-03-T01` | 原有事务内插入入口继续校验canonical identity；deadline/owner前检在V51Repository.commitCanonicalBundleVerifiedR368与DatabaseCoordinator.runCanonicalTransactionR36完成，不能越过durability边界。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java` | 同左 | `R341-B06-03-A01` | 读取有来源的候选预测并核对身份；缺或失效证明只读其原因，研发继续 |
| `app/src/main/java/com/ethk3/novaonepro/ChartPageReadR34.java` | 同左 | `A-B11-04a` | 保留逐图deadline与页面取消；timeout回值必须UNAVAILABLE/TIMED_OUT并附sourceScope，不画默认0系列。；REV-B11-03 47个图和16个表从真实页面逐ID回读对账。实施入口与反例见 specs/review/charts.md / A-B11-04a。 |
| `app/src/main/java/com/ethk3/novaonepro/ChartRegistryR31.java` | 同左 | `A-B11-02a` | REV-B11-02 统一探索影子、预测趋势与正式主链的图表角色。实施入口与反例见 specs/review/charts.md / A-B11-02b。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java` | 同左 | `A-B11-04a` | 仓储读取已发布图帧并在页面回读；缺源保留未知或错误 |
| `app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR83.java` | 同左 | `A-B11-01a` | REV-B11-01 修正图表缓存及报告索引的零值和归因。实施入口与反例见 specs/review/charts.md / A-B11-01b。 |
| `app/src/main/java/com/ethk3/novaonepro/CheckpointReportAuditR32.java` | 同左 | `A-B10-03a` | 输出byteBudget、visited/unvisited、missing/unverified独立计数与coverage；不把审计截断解读为不存在。 |
| `app/src/main/java/com/ethk3/novaonepro/DataRetentionGovernorR900.java` | 同左 | `A-B10-02a` | writer_hold等原始审计尾部先按既有归档协议封存/哈希/可读验证，再分页裁剪SQL热集；不得把聚合计数当原始事件替代。；REV-B10-01 满容量取证热表持续接纳新请求。实施入口与反例见 specs/review/runtime.md / A-B10-02c。 |
| `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java` | 同左 | `R341-B01-04-T01` | 导出 COMMITTED/ROLLED_BACK/UNKNOWN 与实际commit id；不可因诊断写失败把已提交业务重做。 |
| `app/src/main/java/com/ethk3/novaonepro/DbHelper.java` | 同左 | `R341-B04-01-T01` | 两张最小实时依赖表及idx_r919_legacy_post_status划归critical installer；research owner改为只读核验这些已安装对象，消除双owner。 |
| `app/src/main/java/com/ethk3/novaonepro/DbStore.java` | 同左 | `R341-B04-04-T01` | F4-B04-01 已存在交易的重放核查原始收据、概率和授权身份。见 specs/field4/gate.md |
| `app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java` | 同左 | `R341-B09-04-A01` | 主包：沿B04已有唯一owner验证该proof及时间；无证书走明确HOLD/既定fallback，任何备用不得冒称R27授证出号。；F4-B09-01 有资格后在首次和连续两个合法赛前目标进入真实决策。见 specs/field4/gate.md |
| `app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java` | 同左 | `R341-B04-05-T01` | 沿用已有lease/cursor/hash；slice按B02行/字节/时限预算动态截断，先最老可执行项，坏payload隔离为明确终态。；F3-B04-02 验证运行时新增量超过排水量的延后证据队列。按 specs/field3/runtime.md 和 R341-B04-05-T01 的正反例实施复验。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/DeferredWaitR34.java` | 同左 | `R341-B02-02-T01` | 可重试写者拥塞回到续排；永久错误不能冒充暂缓 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/DirectedResearchCreationR932.java` | 同左 | `R341-B06-01-A01` | 追到研究创建候选及其后续用途；只有建议或重复登记不计出生 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/DrawDependencyInputCP12.java` | 同左 | `R341-B01-05-T01` | 保存同一目标输入依赖；错水位或遗漏开奖保持缺口 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/EnabledWindowJournalR335.java` | 同左 | `R341-B09-02-A01` | 启用时段写入且参与机会分母；晚启用不能补记赛前 |
| `app/src/main/java/com/ethk3/novaonepro/EssentialSettlementFeedbackR923.java` | 同左 | `R341-B07-04-A01` | 主包：结算后仅投递事实，不在实时事务内做重训练；失败独立重试且不会拖延下一期冻结。；F3-B07-03 逐期交代训练机会，并绑定下次可用输入。按 specs/field3/chain.md 和 R341-B07-04-A01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/FetchLedgerR925.java` | 同左 | `R341-B04-06-T01` | 复验pending/raw/parsed绑定同request/hash；缺收据、未知请求、payload冲突各有状态，writer故障后仅凭原件恢复。；REV-B04-02 满表后的来源与事务返回证据。实施入口与反例见 specs/review/runtime.md / R341-B04-06-T02。 |
| `app/src/main/java/com/ethk3/novaonepro/LegacyPredrawContextBridgeR915.java` | 同左 | `R341-B04-01-T01` | 保留ensureSchema作为迁移安装器，移除freeze与settle两处调用；用无DDL ready guard，未就绪返回明确DEPENDENCY，不能在临近开奖自修。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/LegacyTeacherOwnerR26.java` | 同左 | `R341-B06-02-A01` | 提供有合法用途的实际教师或准确报告隔离；错来源不进入训练 |
| `app/src/main/java/com/ethk3/novaonepro/LifecycleHealthR963.java` | 同左 | `R341-B05-03-T01` | 记录前后台/息屏/移除任务/系统退出原因同一scope，区分主动退出与系统异常。 |
| `app/src/main/java/com/ethk3/novaonepro/LifecycleSchemaR31.java` | 同左 | `R341-B03-02-T01` | 列出每个需要的对象name/type/DDL hash/owner；单对象推进并验证sqlite_master及必需列/索引，不仅检查table exists。；REV-B03-01 在 B03 验证迁移边界和当前会话可见性。实施入口与反例见 specs/review/build.md / R341-B03-02-T02。 |
| `app/src/main/java/com/ethk3/novaonepro/MainActivity.java` | 同左 | `R341-B02-04-T01` | 每类最多一个在途read，页面销毁取消回调；旧缓存保留stale标识；DB忙不清成空图。 |
| `app/src/main/java/com/ethk3/novaonepro/MainActivityTruthPresenterR926.java` | 同左 | `A-B11-04a` | 只呈现同一只读snapshot的当前运行事实，不因打开页面补造计算/训练；后台通知与详情引用同scopeId。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/OosAfterDrawWriterR36.java` | 同左 | `R341-B04-04-T01` | 原赛前记录收到开奖后生成匹配标签；缺原件不补造预测 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/PreparedCanonicalCodecR335.java` | 同左 | `R341-B04-02-T01` | 同一规范输入编码解码后身份相符；篡改一字段失效 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/PreparedCanonicalStoreR335.java` | 同左 | `R341-B04-03-T01` | 对应准备结果由本目标实际消费；过时或别期结果不提交 |
| `app/src/main/java/com/ethk3/novaonepro/R36AnalysisHistoryPolicyR364.java` | 同左 | `R341-B04-02-T01` | 保留base边界、连续suffix、最小样本检查；明确canonical排序/编码和draw correction版本。 |
| `app/src/main/java/com/ethk3/novaonepro/RawHttpIdentityR34.java` | 同左 | `R341-B04-06-T01` | VERIFY_ONLY：原始body身份不可trim/归一化或覆盖；字符解码与原字节身份区分，不修改现有合法hash。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java` | 同左 | `R341-B01-01-T01` | 真实构建读取版本身份；旧清单与 APK 不一致时被发现 |
| `app/src/main/java/com/ethk3/novaonepro/ReportCheckRegistryCP12.java` | 同左 | `A-B11-01a` | 分别汇总检查是否执行、源是否可读、业务是否通过；96项已覆盖可为true，同时health FAIL/UNKNOWN不可被覆盖。 |
| `app/src/main/java/com/ethk3/novaonepro/ReportCleanupR31.java` | 同左 | `A-B10-02a` | 按原件保护分类和实际清理字节出回执；清理失败保持已发布状态独立，不把报告成功改为失败。；REV-B10-03 先证明交付件可恢复，再清理原始工作区。实施入口与反例见 specs/review/storage.md / A-B10-02b。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/ReportDiagnosticEvidenceR64.java` | 同左 | `A-B11-06a` | 导出读回当前业务证据的来源；缺原件不能标完整 |
| `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java` | 同左 | `A-B10-01a` | REV-B10-02 为大库一致快照独立预约实际峰值。实施入口与反例见 specs/review/storage.md / A-B10-01a。；F3-B10-01 把双快照磁盘增量按实际物理类别对账。按 specs/field3/runtime.md 和 A-B10-01a 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java` | 同左 | `A-B11-06a` | REV-B11-05 报告与赛前服务并行，并验手机端持久保存。实施入口与反例见 specs/review/storage.md / A-B11-06b。 |
| `app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java` | 同左 | `A-B10-01a` | REV-B10-02 为大库一致快照独立预约实际峰值。实施入口与反例见 specs/review/storage.md / A-B10-01a。 |
| `app/src/main/java/com/ethk3/novaonepro/ReportReadContextCP4.java` | 同左 | `A-B11-01a` | 维护既有严格null/type/shape校验，新增有界只读错误注册接口供适配器归并；不得改scalar返回默认0。 |
| `app/src/main/java/com/ethk3/novaonepro/ReportSemanticVerifierR31.java` | 同左 | `A-B11-01a` | 结构完整验收之外增加sourceHealth摘要一致性约束；内层source failure未计入总览即校验失败，不重定义历史96项编号。 |
| `app/src/main/java/com/ethk3/novaonepro/ReportSourceStateR341.java` | 同左 | `A-B11-01a` | 输入结构化子源结果；输出SOURCE_OK/EMPTY/UNAVAILABLE及reason枚举和原始细分state，UNKNOWN数值为null；FieldFollowup、图表适配、总览共用，无业务写入口。 |
| `app/src/main/java/com/ethk3/novaonepro/ResearchFeedbackCP12.java` | 同左 | `R341-B07-04-A01` | 主包：以权威 settled decisionId+target+model+feedback schema 为事件键；记录真实误差/残差/支持域，ACK 与下游 outbox/effect 引用同事务。；F3-B07-03 逐期交代训练机会，并绑定下次可用输入。按 specs/field3/chain.md 和 R341-B07-04-A01 的正反例实施复验。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/ResearchMainlineContributionAuditR922.java` | 同左 | `R341-B09-04-A01` | 读回研究贡献与真实决策身份；影子贡献不能计为真实应用 |
| `app/src/main/java/com/ethk3/novaonepro/ResearchVisualDataR82.java` | 同左 | `A-B11-04a` | 扩展不可变展示payload：displayState=HAS_DATA/EMPTY/UNAVAILABLE，reasonCode单独保存WAITING_SOURCE/WAITING_RUN/SOURCE_ERROR/TIMED_OUT/STALE/PARTIAL；UNKNOWN数值仍NaN/null。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/RetentionArchiveProtocolCP12.java` | 同左 | `A-B10-03a` | 从有效归档恢复受保护对象；错摘要或唯一副本保持保护 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/RetentionSnapshotCacheR73.java` | 同左 | `A-B10-02a` | 读取匹配快照并可重建缓存；失效缓存不充当唯一原件 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/RetentionTableRegistryCP12.java` | 同左 | `A-B10-01a` | 实际清理计划按注册表归类；保护对象不可按缓存删除 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/RetentionWorkingStorageQuotaCP12.java` | 同左 | `A-B10-01a` | 受限工作区可增量提交；过预算保留既有唯一依据 |
| `app/src/main/java/com/ethk3/novaonepro/RuntimeEventHooksR926.java` | 同左 | `R341-B01-04-T01` | 成功证据只从已提交事件或commit receipt解析；UNKNOWN不给成功。 |
| `app/src/main/java/com/ethk3/novaonepro/RuntimeEventLedgerR926.java` | 同左 | `R341-B01-02-T01` | 每条新事件带明确 scope；旧行缺字段投影为 LEGACY_UNSCOPED，不能默认加入当前窗口。 |
| `app/src/main/java/com/ethk3/novaonepro/RuntimeObservationScopeR341.java` | 同左 | `R341-B01-02-T01` | NEW：只读不可变 Scope 对象；采集 wallAt/monoAt/SQL snapshot watermark；连接观测只允许同 databaseIdentity 和明确兼容 scope，返回 mismatch reason。 |
| `app/src/main/java/com/ethk3/novaonepro/RuntimeReadSnapshotR341.java` | 同左 | `R341-B02-04-T01` | NEW：versioned immutable snapshot，含scope/sourceCommitAt/expiresAt/readStatus；singleflight刷新+有界队列，SQLite Cursor生命周期在工作线程内关闭。 |
| `app/src/main/java/com/ethk3/novaonepro/RuntimeRecoveryCoordinatorR926.java` | 同左 | `R341-B05-02-T01` | 将恢复控制记录关联recoveryId与progress receipt；保留原合法性保护和报告接口。 |
| `app/src/main/java/com/ethk3/novaonepro/RuntimeScenarioRecorderR341.java` | 同左 | `R341-B01-05-T01` | NEW：有界保存base/target、原开奖时间、history rows/hash、固定deadline、generation/fence、phase时间；仅供离线重放，不能导回生产预测表。 |
| `app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java` | 同左 | `R341-B04-01-T01` | 将两表/索引及定义指纹加入critical contract；version/contract revision变更由唯一schema owner提交并形成迁移receipt。 |
| `app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java` | 同左 | `R341-B01-02-T01` | 记录 releaseId、processEpoch、bootId、databaseIdentity；保留 lifetime 与 currentEpoch 两套字段，不覆盖历史。 |
| `app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java` | 同左 | `R341-B09-04-A01` | 主包：沿B04权威冻结/decisionId/target结算，R27资格账消费结果不能自建第二笔trade。；F4-B09-02 核验授权与撤销交错时唯一交易和唯一结算。见 specs/field4/gate.md |
| 补入 `app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java` | 同左 | `R341-B04-05-T01` | 入队开奖在重启后同键结算一次；重复及缺冻结保持各自终态 |
| `app/src/main/java/com/ethk3/novaonepro/SourceManifestR815.java` | 同左 | `R341-B01-02-T01` | 沿用生成源身份，禁止手填源码 commit；新增运行证据 envelope 消费该身份。 |
| `app/src/main/java/com/ethk3/novaonepro/StartupStageJournalR341.java` | 同左 | `R341-B01-03-T01` | NEW：应用私有目录有界 framed journal，CRC+序号+强制落盘；不引用 DbStore/SQLite/DatabaseCoordinator；首个失败与未闭合阶段受保护。 |
| `app/src/main/java/com/ethk3/novaonepro/StorageBudgetR31.java` | 同左 | `A-B10-01a` | 引入版本化分类增量策略；现有受保护字节记protectedDebtBytes，不能作为可删预算；以完整实际总量检查物理空闲，按分类工作集和未落盘reservation检查增量。；普通最低空闲保留256MiB；关键结算/事实写入的应急区为另行预留的64MiB有界空间，耗尽输出STORAGE_CRITICAL，禁止无限豁免。 |
| `app/src/main/java/com/ethk3/novaonepro/StorageCensusSU2.java` | 同左 | `A-B10-01a` | 分别公布logical/allocated、完整目录范围、dbstat截断和采样时间；分类不重叠；未知allocated不回填logical冒充。；F3-B10-01 把双快照磁盘增量按实际物理类别对账。按 specs/field3/runtime.md 和 A-B10-01a 的正反例实施复验。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/StorageCheckpointServiceR31.java` | 同左 | `A-B10-03a` | 服务响应对应实际保存或恢复；进程死亡后不把请求当成功 |
| `app/src/main/java/com/ethk3/novaonepro/StorageMaintenanceFileR82.java` | 同左 | `A-B10-05a` | 写前按源+candidate+WAL+已有backup峰值预约；不允许覆盖尚未处置的旧backup。；遵守已有journal→fsync→关闭句柄→原子swap→重开integrity/表计数/关键hash验证→结束回执；失败按journal恢复原库。 |
| `app/src/main/java/com/ethk3/novaonepro/StorageMaintenanceServiceR82.java` | 同左 | `A-B10-05a` | 进入显式维护窗口前检查无在途关键写/冻结读/报告pin，持有维护租约并发布原因与进度；临近目标期或空间预约失败则延后。；复用现有恢复证明与托管服务；当前旧备份保持保护，不因压缩完成自动删。 |
| `app/src/main/java/com/ethk3/novaonepro/StoragePolicyR341.java` | 同左 | `A-B10-01a` | 输入category、criticality、maxGrowth、protectionClass、censusHash；输出ALLOW/DEFER/REJECT、remainingGrowth、reason、policyHash；由StorageBudgetR31.reserveOnceR31及ensureAdditionalOnceR31调用，无独立写库入口。；初始受控工作集上限1.5GiB：可重算图表128MiB、临时报表256MiB、未发布模型512MiB、诊断索引256MiB、其他工作集384MiB；唯一原件单列，不强行纳入缓存驱逐。单次可选报表增量上限128MiB，超出改用流式/分卷/明确拒绝。调整须版本化记录。 |
| `app/src/main/java/com/ethk3/novaonepro/TargetIssueResolver.java` | 同左 | `R341-B05-03-T01` | REV-B05-03 每期影子和 1 秒/5 秒的实测分母。实施入口与反例见 specs/review/runtime.md / R341-B05-03-T01。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java` | 同左 | `R341-B08-04-A01` | 研究选择在截止前冻结并由合法通道消费；错代错目标不得串用 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java` | 同左 | `R341-B01-01-T01` | 签收已构建产物实际身份；旧清单单独不能赋予出手资格 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/V51DecisionGateCore.java` | 同左 | `R341-B09-04-A01` | 兼容门的真实调用者和输出可追溯；其正值不能独立生成第二份决定 |
| `app/src/main/java/com/ethk3/novaonepro/V51Repository.java` | 同左 | `R341-B04-04-T01` | 保留核心bundle原子性、PIT和授权门；只把可派生大对象准备移到锁外，不能拆出会造成半canonical的写。；F3-B04-01 同一期分开判断S0、PIT投影、正式冻结及结算。按 specs/field3/runtime.md 和 R341-B04-04-T02 的正反例实施复验。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java` | 同左 | `R341-B04-04-T01` | 同目标编排交付唯一规范结果；重试和迟到不得二次结算 |
| `app/src/main/java/com/ethk3/novaonepro/WriteBudgetR341.java` | 同左 | `R341-B02-01-T01` | NEW：输入lane/deadlineMono/rows/bytes/statements与等待开始；返回permit或RESOURCE_WAIT及nextDue，不能跨线程强行结束事务。；REV-B02-01 在 B10 存储策略交付前独立签收写入期限。实施入口与反例见 specs/review/build.md / R341-B02-01-T02。 |
| `app/src/main/java/com/ethk3/novaonepro/r952/HistoryContinuityR341.java` | 同左 | `A-B10-04a` | 只输出有序缺口与补取任务，无直接编造draw写入；调用LongScaleSummaryR80.prepare和既有归档/事实入口，由B04唯一事实owner接纳权威来源。记录来源hash、receivedAt、acceptedAt和修复epoch。 |
| `app/src/main/java/com/ethk3/novaonepro/r952/LongScaleSummaryR80.java` | 同左 | `A-B10-04a` | 将缺口诊断固定到epoch/generation/through/nextExpected/nextObserved，恢复后生成新epoch或证明连续补齐，CAS提交不污染旧冻结源。；所有新补取/重新构建的可用时间按真实返回/发布时间记录RECOVERED_AVAILABLE_NOW；原epoch、原始INVALID_SOURCE与修复关联均保留。 |
| `app/src/main/java/com/ethk3/novaonepro/r952/LongSummaryBindingsR80.java` | 同左 | `A-B10-04a` | 当前源使用新有效epoch；历史源继续检查available<drawAt及严格目标前驱，不因修复成功放宽PIT。；旧冻结spec依赖仍绑定原epoch；不静默重指向新数据，用新候选/新frozen身份重新研究。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java` | 同左 | `R341-B06-03-A01` | 从实际捕获追到候选和训练游标；固定旧候选的反复预测不计新生 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/AssetQualityVerdictR341.java` | 同左 | `R341-B09-03-A01` | NEW evaluate(V1Verdict,CalendarProof,PairedProof,Scope)->QualityVerdict；严格引用不可变原件，输出Q01–Q08状态，consumer为报告与质量验收；不增加V1选择门槛、不阻已合法证书首次进入既定选择，无独立REAL授权。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetPublicationR31.java` | 同左 | `R341-B07-02-A01` | 提交 guard 拒绝 stale old-state hash，stage/commit 的幂等键使用 optimizer epoch/step 和 owner fence。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java` | 同左 | `R341-B07-01-A01` | 用 B03 已安装 r34 缺标签/optimizer 表；按 issue+position+featureABI+source revision 幂等准备样本，标签只在权威结算可见后允许用于开发训练。；REV-B07-01 在线回放不能再抽中本模型的开发验证样本。实施入口与反例见 specs/review/algorithm.md / R341-B07-01-A03。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSampleArchiveR324.java` | 同左 | `R341-B07-05-A01` | 每对象重试、退避、失败隔离；原件仍热可读时不阻训练；容量已到硬限才回报明确 CAPACITY_WAIT，不能无限增长。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java` | 同左 | `R341-B07-01-A01` | 同 epoch 十位置一致；partialIssue 非空不可发布；缺位置保持部分状态，不写“完整轮”；恢复继续缺项。；REV-B07-01 在线回放不能再抽中本模型的开发验证样本。实施入口与反例见 specs/review/algorithm.md / R341-B07-01-A03。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetStoreR30.java` | 同左 | `R341-B07-02-A01` | 训练状态写入后原模型恢复并继续；错检查点不出替代模型 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetTrainerR27.java` | 同左 | `R341-B07-01-A01` | REV-B07-01 在线回放不能再抽中本模型的开发验证样本。实施入口与反例见 specs/review/algorithm.md / R341-B07-01-A03。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/BirthGateR27.java` | 同左 | `R341-B06-03-A01` | 规范 AST hash 精确去重，冻结 TRAIN 探针上的动作/概率指纹近重复筛选；不同机制保留单独候选，不能借未见评估选择探针。；F3-B06-02 21个思路族的可执行公式与报告处方逐一对照。按 specs/field3/learning.md 和 R341-B06-03-A01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/CalibrationRowsR27.java` | 同左 | `R341-B09-03-A01` | REV-B09-04 选择后校准与模型/策略独立取证。实施入口与反例见 specs/review/report.md / R341-B09-03-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/CapabilityNeedDiagnosticsR335.java` | 同左 | `A-B11-03a` | F3-B11-03 八项能力需求逐生产入口和实际消费者验真，保留缺能力提案分流。按 specs/field3/directions.md 和 A-B11-03b 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/CapabilityNeedRegistryR335.java` | 同左 | `A-B11-03a` | F3-B11-03 八项能力需求逐生产入口和实际消费者验真，保留缺能力提案分流。按 specs/field3/directions.md 和 A-B11-03b 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java` | 同左 | `R341-B09-02-A01` | REV-B09-02 同一期分批冻结、重试和超时后机会总数守恒。实施入口与反例见 specs/review/algorithm.md / R341-B09-02-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java` | 同左 | `R341-B09-04-A01` | 语义/模型/校准失效、证书到期、原件冲突按原合同撤销；撤销与阻断选择同事务。；F4-B09-02 核验授权与撤销交错时唯一交易和唯一结算。见 specs/field4/gate.md |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointClosureR324.java` | 同左 | `R341-B03-04-T01` | 活候选引用完整闭包；缺原件隔离该对象且独立对象继续 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointDependencyR341.java` | 同左 | `R341-B03-04-T01` | NEW：只读依赖映射，输入受保护source引用与task manifest；输出 READY/WAIT_SOURCE/UNVERIFIED/CONFLICT 和证据引用，不生成替代权重。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcReachabilityR324.java` | 同左 | `A-B10-03a` | 按局部闭合分量证明GC，不因别处缺失阻塞已闭合无根副本，也不因全局统计看似零引用跳过闭包。删除前复验引用、pin和文件身份，删除后独立回执。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcStoreR31.java` | 同左 | `A-B10-03a` | 恢复登记与引用状态CAS一致；未知原件不得被新候选继承为验证祖先。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointRecoveryR335.java` | 同左 | `A-B10-03a` | 逐对象记录MISSING/AVAILABLE_UNVERIFIED/VERIFIED/MISMATCH/UNRECOVERABLE，保存探测位置、hash、字节数、最后尝试、下一次退避、owner lineage。；恢复仅从已授权备份/intake读取，流式拷入temp→hash校验→fsync→atomic publish→短事务注册；同objectHash幂等，冲突原件隔离不覆盖。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdRebirthContractR34.java` | 同左 | `R341-B06-01-A01` | 沿已有 sourceKey/campaign 身份与 requireFreshCounts 零继承断言补齐字段核验；保留原身份版本，不新增并行身份规则。；REV-B06-01 旧父代遇到空 ABI 时可计数、可恢复。实施入口与反例见 specs/review/algorithm.md / R341-B06-01-A03。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdRebirthR34.java` | 同左 | `R341-B03-03-T01` | 只在实际schema-ready并拿到当前fence后执行；缺source、缺父对象返回准确dependency，不伪造candidate。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdRebirthSchemaR34.java` | 同左 | `R341-B03-02-T01` | DDL常量为现有权威定义，安装后逐对象验证；保留已存在行及schema版本。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/CreationBudgetR31.java` | 同左 | `R341-B08-07-A01` | F3-B08-04 用同一代的有无研究输入分配对照回查矩阵与研究成效。按 specs/field3/learning.md 和 R341-B08-07-A01 的正反例实施复验。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/DerivedAtomR27.java` | 同左 | `R341-B08-05-A01` | 实际被选择的派生原子进入子代；未来泄漏或没有行为差的分支留痕 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisTraceReaderR33.java` | 同左 | `A-B11-03a` | 诊断读回同范围原件；不同水位或缺源不能拼成成功 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/DirectedResearchPolicyR30R1.java` | 同左 | `R341-B08-05-A01` | F3-B08-06 让跨位或组件负贡献的失败假设到达兼容的真实实验。按 specs/field3/research.md 和 R341-B08-05-A01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java` | 同左 | `R341-B06-03-A01` | F3-B06-02 21个思路族的可执行公式与报告处方逐一对照。按 specs/field3/learning.md 和 R341-B06-03-A01 的正反例实施复验。；F4-B06-03 同一公式的历史拒绝与已成功出生分别处理。见 specs/field4/birth.md |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionPublicationSqlR31.java` | 同左 | `R341-B08-04-A01` | 真实发布批按期望绑定值读取；SQL参数或声明不匹配被拒 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionPublicationStoreR31.java` | 同左 | `R341-B08-04-A01` | 出生相关发布事务可回读且重试幂等；半提交对消费方不可见 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertContinuationR26.java` | 同左 | `R341-B06-04-A01` | 可执行专家续算消费自己的父态；审计专家或无效父态不冒领训练 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertExperimentR26.java` | 同左 | `R341-B06-04-A01` | Pair 状态可恢复；源角色/目标改变则新实验身份，保全原实验；缺组件只阻塞该专家。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertProposalsR27.java` | 同左 | `R341-B06-04-A01` | 处方包含具体 AST 操作、父策略、来源 measure/run、预期检验，非可执行文本标 AUDIT_ONLY。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertWorkshopR26.java` | 同左 | `R341-B06-04-A01` | 实验输出必须绑定 Pair 的两个合法组件、目标、角色、输入摘要、真实 child AST；审计输出 audit receipt 不冒充出生。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDirectedResearchR26.java` | 同左 | `R341-B06-01-A01` | F4-B06-01 把新工厂的基础种子与坏历史父代、失败研究输入隔离。见 specs/field4/birth.md |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDispositionR31.java` | 同左 | `R341-B02-01-T01` | 写入失败被分类并走正确的重试或上报；永久失败不循环占写者 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureProfileR30.java` | 同左 | `R341-B08-05-A01` | 同范围失败画像改变一个可观察的变异选择；错父代画像不能套用 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java` | 同左 | `R341-B06-02-A01` | F3-B06-01 24个模型家族逐一执行检测报告指定的机制对照。按 specs/field3/learning.md 和 R341-B06-02-A01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FieldFollowupR34.java` | 同左 | `A-B11-01a` | 每个source projection输出typed state、scope、reasonCode、errorRef、rowsReturned、totalRowsKnown、sourceQueryStatus；缺表显式SOURCE_UNAVAILABLE。；按所有子查询结构聚合sourceErrors，不只捕捉顶层异常；max32页显示不当作全量行数。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FormulaIdentityR341.java` | 同左 | `R341-B06-03-A01` | NEW 接口 canonical(Node,Abi)->bytes、fingerprint(Node,Probe)->Behavior、diff(parent,child)->NodeEdits；仅规范等价序列化和交换律安全节点，禁止浮点结合律重排。由 StructuralProposalR26.add 与 BirthGateR27.checkCandidate 调用。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java` | 同左 | `R341-B06-01-A01` | F4-B06-01 把新工厂的基础种子与坏历史父代、失败研究输入隔离。见 specs/field4/birth.md；F4-B06-02 稀疏历史仍用真实可训练尾段完成冷启动。见 specs/field4/birth.md |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationR30.java` | 同左 | `R341-B09-01-A01` | 冻结child/parent/control、AST/model/feature ABI、role边界、calendar、规则、seed、统计计划；评分不得再选阈值或窗口。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationStoreR30.java` | 同左 | `R341-B09-01-A01` | 确认原件和结果不可变；每次注册全局alpha index永久递增；重复注册幂等，拒绝原登记后换模型。；REV-B09-01 冻结资格比较对象及适用区间。实施入口与反例见 specs/review/algorithm.md / R341-B09-01-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionOpsR27.java` | 同左 | `R341-B08-06-A01` | 只合并同position/target语义、角色与pin相容的两亲；融合权重只在TRAIN拟合，SELECT选方案；新策略独立hash/lineage。；F3-B08-03 融合只给真正进入子代且获得实测贡献的父代记功。按 specs/field3/learning.md 和 R341-B08-06-A02 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FutureModelRegistrationR34.java` | 同左 | `R341-B07-03-A01` | 未来注册固定 publication/weights/feature ABI；注册时点之后模型更新不改变已注册实验；改变模型须新实验与独立证据。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/IssueTimelineWriterR30.java` | 同左 | `R341-B01-04-T01` | 进程内观测待刷与DB实际提交分开；重启缺进程缓存不能倒推失败或成功。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java` | 同左 | `R341-B09-01-A01` | REV-B09-01 冻结资格比较对象及适用区间。实施入口与反例见 specs/review/algorithm.md / R341-B09-01-A01。；F3-B09-01 将I13单独选择时机的价值与只挑高分时段的效果分开。按 specs/field3/research.md 和 R341-B09-01-A01 的正反例实施复验。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveStateR26.java` | 同左 | `R341-B06-01-A01` | 恢复出的状态能够预测并继续学习；损坏旧状态不计新版本出生 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterR31.java` | 同左 | `R341-B09-04-A01` | 直接适配原冻结position/K3；不得再次预测、挑新位置或补号。；F4-B09-02 核验授权与撤销交错时唯一交易和唯一结算。见 specs/field4/gate.md |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceSU1.java` | 同左 | `R341-B09-04-A01` | 查明当前版本选取或回退的权威消费者；旧选择不可成为独立出手门 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java` | 同左 | `R341-B09-04-A01` | 有效选择可以读回，撤销后下一次选择不复用；已冻结期保持原件 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineFreezeProofR31.java` | 同左 | `R341-B09-04-A01` | 核验证书gateVersion/有效期/撤销、policy/model/calibration/qualification不可变绑定与同目标；冻结时CAS source revision。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineModelBindingR31.java` | 同左 | `R341-B09-04-A01` | 冻结使用资格注册模型身份，拒绝当前latest替代已认证模型；记录实际model call来源。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixEvidenceR30R1.java` | 同左 | `R341-B08-03-A01` | 按实际各kind shape/axis/support生产；EVIDENCE_TEACHER可用同位置原归档策略作研究教师，明确不得替代F23原VM；RESOURCE_EFFICIENCY未测memory/queue列维持缺失。；REV-B08-02 时序矩阵按本次数据版本读取模型对照。实施入口与反例见 specs/review/algorithm.md / R341-B08-03-A02。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryR30.java` | 同左 | `R341-B08-03-A01` | 分别导出legacy algorithmConsumer与实际typedConsumers；前者PERSISTENCE_AND_DIAGNOSTICS不意味着后者缺失。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryStoreR30.java` | 同左 | `R341-B08-03-A01` | 每对象输出 role、target、knownAt、shape、axis labels、mask、support、artifactHash、消费引用与状态；20对象包含独立 SU2 teacher checkpoint，不能漏掉。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java` | 同左 | `R341-B08-03-A01` | 维持 source-only 优先和每kind隔离；19 R930 kinds按各自产物原件生产，COUNTERFACTUAL 原有专属来源单独接入，不绕过其精确组件移除合同。；REV-B08-02 时序矩阵按本次数据版本读取模型对照。实施入口与反例见 specs/review/algorithm.md / R341-B08-03-A02。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixSnapshotSU2.java` | 同左 | `R341-B08-03-A01` | 20项之一为SU2 factor/teacher bundle；每factor10位置权重与原stream codec校验，保留原owner可见时间。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricEvaluatorR29.java` | 同左 | `A-B11-02a` | 统一metric scopeId：installedVersion/sourceCommit/processEpoch/snapshotAt/role/targetFromThrough；当前与累计分开，不混读LIVE及旧合成数据。；延迟保留成功样本分布并并列全目标机会数、请求/成功/超时/未启动/被取代/缺失分子；无成功样本时P95=null。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelFoundryJobSU2.java` | 同左 | `R341-B06-02-A01` | 工厂任务切片产生可复现的状态推进；仅轮询running不计训练 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelReplaySU2.java` | 同左 | `R341-B06-04-A01` | 绑定批准的回放和合法角色或记录缺依赖；错模型样本不进入学习 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelReplayStudyR30R1.java` | 同左 | `R341-B07-04-A01` | F3-B07-02 按R15把没有回放、固定回放、候选回放分开比较。按 specs/field3/research.md 和 R341-B07-04-A03 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceProviderR30.java` | 同左 | `R341-B07-03-A01` | 校验 position、target、featureThrough、historyHash、prepared/frozen/deadline 与 published modelHash；不得内部取 latest 替换已绑定模型。；F3-B07-01 区分深度网络两个输出头及各自被哪种动作消费。按 specs/field3/learning.md 和 R341-B07-03-A02 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceStoreR30.java` | 同左 | `R341-B07-03-A01` | 保存概率面十维值/shape、modelCall 的 publication/model/feature/hash；源丢失为 UNAVAILABLE，不回填均匀面伪作模型输出。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/MutationOpsR27.java` | 同左 | `R341-B08-05-A01` | 选定算子实改可执行结构并评测；不可用变异有明确拒因 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/NullEvolutionR27.java` | 同左 | `R341-B09-05-A01` | 保留CONTROL_MUST_NEVER_PROMOTE与holdout保护；完整保存搜索池/算子/预算/RNG/alpha索引，崩溃恢复同一路径。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/PolicyAstR27.java` | 同左 | `R341-B06-03-A01` | 保持原 AST 语法、因果约束及资源上限；非法类型/未来访问/非有限值给出节点路径。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/PolicyContractR30.java` | 同左 | `R341-B06-03-A01` | 固定目标位置 1..10、单位置三个不同号码、≤64 节点≤8 深度；随机控制不能申请实盘。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/PolicySpecR27.java` | 同左 | `R341-B06-03-A01` | 规范公式可解析再执行，语义哈希可复算；改名不算新公式 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationGapRepairR30R1.java` | 同左 | `R341-B09-02-A01` | 仅用原事前hash、时间证据与权威开奖修复投影；缺原件保持缺口，不发明事前预测。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationInboxR29.java` | 同左 | `R341-B09-01-A01` | REV-B09-01 冻结资格比较对象及适用区间。实施入口与反例见 specs/review/algorithm.md / R341-B09-01-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/QualityExperimentR341.java` | 同左 | `R341-B09-03-A01` | F4-B09-05 从同机会独立原件重算父子增益下界。见 specs/field4/q06.md |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/RecoveryLedgerR341.java` | 同左 | `A-B10-03a` | 输入原件对象hash、既有恢复request、核验结果；输出状态/隔离原因，不授予资格。由CheckpointRecoveryR335.finish和报告适配层调用；按objectHash+attemptId去重。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/RepairDiagnosisR32.java` | 同左 | `A-B11-02a` | 时间/版本/角色不兼容时输出STALE_SCOPE或CROSS_SCOPE，不把旧crash、旧refit、影子命中转成当前主路成果。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/RepairEffectLedgerR341.java` | 同左 | `A-B11-05a` | 输入只接受验证build identity及生产事件ref；不允许报告渲染者自行把工单PASS。由B01安装身份owner调用recordInstall，现有任务owner提交observation，FieldFollowup只读。；幂等键issueId+sourceCommit+processEpoch+scenarioId+evidenceHash；失败追加不可变观察，不能覆盖失败历史；短事务、有限页返回。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayDatasetR27.java` | 同左 | `R341-B07-01-A01` | 样本记录原开奖可见时间、feature through、源提交及连续区间，拒绝把导入时间伪作历史赛前可见时间。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java` | 同左 | `A-B11-01a` | REV-B11-01 修正图表缓存及报告索引的零值和归因。实施入口与反例见 specs/review/charts.md / A-B11-01b。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR27.java` | 同左 | `A-B11-03a` | 当前问题、历史负债、未知证明和实际已完成工程分别输出；引用R341 issue ledger状态并给验证查询。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java` | 同左 | `A-B11-01a` | REV-B11-01 修正图表缓存及报告索引的零值和归因。实施入口与反例见 specs/review/charts.md / A-B11-01b。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchActionPolicyR33.java` | 同左 | `R341-B06-05-A01` | B06 仅用固定先验和开发负证据；B08 再接实际反馈。设正探索下限，负记忆按机制+dataset revision 精确作用，保护活跃资产。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchChartActionsR33.java` | 同左 | `R341-B08-02-A01` | 将既有动作说明与可执行 actionKind/source contract 对齐；说明文本不能替代真实动作与回执。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchConsumptionStoreR31.java` | 同左 | `R341-B08-04-A01` | BORN child、lineage、proposal consumed receipt 同一事务；非出生动作必须有实际 before/after状态及提交事务，不能借一个文字ACK。；REV-B08-05 把视图产品消费定位到同源实际动作。实施入口与反例见 specs/review/charts.md / R341-B08-04-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java` | 同左 | `R341-B08-03-A01` | 复用原typed消费者，校验input pin/axis/role/support；use/annotate/stage必须绑定实际child或预算回执；没有可用信号时不假记消费。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchGraphSpecsR30R1.java` | 同左 | `R341-B08-02-A01` | REV-B08-04 逐图以有值原件或正式链原件验收产出。实施入口与反例见 specs/review/charts.md / R341-B08-02-A01。；F3-B08-05 R04按实际提示窗口核算两个来源、交集及弃权代价。按 specs/field3/research.md 和 R341-B08-02-A01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputContextR33.java` | 同左 | `R341-B08-01-A01` | 事务外有界读；SOURCE_ONLY 不请求学生；释放资源必须 finally；pin 与计算预算同时获得后才开始。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java` | 同左 | `R341-B08-01-A01` | run pin 固定 datasetHash+revision+role+sourceCommit+可见水位；发布时 CAS，版本变化只作失效回执并重排，不覆写旧产物。；REV-B08-01 进化卡在老代时可安全替换待用数据缓存。实施入口与反例见 specs/review/algorithm.md / R341-B08-01-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchObservatoryR30R1.java` | 同左 | `R341-B08-02-A01` | 按 appendix 中 R01–R18 合同执行；一图一有界切片，错误/等待后轮转；frame 必含点值/support/单位/role/sourceHash。；R11/R14/R18 是正式链只读证据图，缺自然证据显示未观察；R08 固定 TRAIN 探针；其余研究测量用 SELECT/允许的 ONLINE_RESEARCH。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java` | 同左 | `R341-B08-03-A01` | RESOURCE_EFFICIENCY消费既有预算入口，记录实际发放与使用，不以声明budget冒充已分配。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchResultContractR31.java` | 同左 | `R341-B08-04-A01` | 研究结果通过正确类型消费检查；只读或错身份结果不准生子 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionR30.java` | 同左 | `R341-B06-05-A01` | 已退役且可恢复内容可回收；活孩子引用的父证据保留 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchRetentionStoreR30.java` | 同左 | `R341-B03-01-T01` | 迁移内仅提交已验证的引用；迁移后保留source与checkpoint证据，实际调用点RuntimeCoordinatorR26.prepareStartupSchemaR34。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java` | 同左 | `R341-B08-03-A01` | COUNTERFACTUAL复用paired的1×blockCount专属生产与exact measured child，不塞入通用轮转制造假对照。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAdaptersR30R1.java` | 同左 | `A-B11-04a` | 明确pageRows、pageOffset、filterHash、totalKnown/totalRows；无全量COUNT证明时totalRows=null；现有displayIsFullDenominator=false保持。；REV-B11-03 47个图和16个表从真实页面逐ID回读对账。实施入口与反例见 specs/review/charts.md / A-B11-04a。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAuditR32.java` | 同左 | `A-B11-04a` | 页内空/过期/混合窗口审计只对页范围结论；sourceVersion不可得继续UNKNOWN。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTaskRegistryR30R1.java` | 同左 | `R341-B06-01-A01` | 当前会话可定位生产任务及后续状态；旧注册表行不能代签运行 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java` | 同左 | `R341-B02-01-T01` | 队列的具体操作真实提交，提交未知能重试；入队计数不代签落库 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/RetentionMigrationR34.java` | 同左 | `R341-B01-03-T01` | 写阶段摘要与精确unit/fence/phase引用；输入hash、已提交cursor和UNKNOWN解析结果关联同一次attempt，禁止每条SQL泛滥日志。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RetentionR26.java` | 同左 | `R341-B06-01-A01` | 长算任务保留可解码的进度重启继续；唯一来源不误清除 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java` | 同左 | `R341-B01-03-T01` | 在真实调用边界写 begin/end；捕获 WAIT_DEPENDENCY、WAIT_RESOURCE、MISSING_SOURCE、BUG、FATAL；不得把所有 WAIT 抛成致命失败。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeDiagnosticLinksR31.java` | 同左 | `R341-B01-04-T01` | 诊断链接回到业务进度证据；不同进程的事实不得相互作证 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeFaultEvidenceR31.java` | 同左 | `R341-B02-02-T01` | 实际失败落到有界可读证据；上报通道被占用时业务能继续或恢复 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java` | 同左 | `R341-B03-03-T01` | 仅在R26初始化完成后启动当前R27 owner；退出和重启保持session代际一致。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeSchemaR26.java` | 同左 | `R341-B03-02-T01` | 启动后验证反馈结构可读写；部分表结构或同名错列停在迁移 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/SchemaR27.java` | 同左 | `R341-B03-02-T01` | 数据库就绪后观察和写入合同可用；原有不完整结构须被识别 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/SchemaUnitPlanR34.java` | 同左 | `R341-B03-02-T01` | 校验保留迁移的现有unit预算、fence与UNKNOWN解析契约；生命周期DDL仍用LifecycleSchemaR31.advance自己的单对象owner，不创建第二个DDL安装器。；REV-B03-01 在 B03 验证迁移边界和当前会话可见性。实施入口与反例见 specs/review/build.md / R341-B03-02-T02。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/SequentialQualificationR27.java` | 同左 | `R341-B09-02-A01` | 仅由按序、已settled、合法独立的账本输入；重复不得累计；缺口处理使用原合同，不把未知变miss或HOLD偷改统计。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/ShadowAdmissionR26.java` | 同左 | `R341-B09-04-A01` | 合规候选准入影子，其角色沿用到冻结；影子准入不算主链获证 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/StartupCheckpointIdentityR31.java` | 同左 | `R341-B03-01-T01` | 保留迁移前原件身份与准确失败原因；准备IO在写锁外。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/StatisticalTrainingDiagnosticsR26.java` | 同左 | `R341-B09-03-A01` | 由真实训练诊断消费该定义并产生可复算字段；未知样本禁止判合格 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/StatsR27.java` | 同左 | `R341-B09-03-A01` | 统计值能从同窗口输入复算；样本不足保留所定义的边界状态 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/StrategyFoundrySU1.java` | 同左 | `R341-B06-03-A01` | 从实际研究调用测生成或读取的候选及谱系；旧冠军重列不计创造 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/StructuralProposalR26.java` | 同左 | `R341-B06-03-A01` | 每次变异附操作、父节点路径、替换子树、seed、before/after canonical hash；NO_OP/PARAMETER_VARIANT/STRUCTURAL 分开计数。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/SymbolicFamilyR26.java` | 同左 | `R341-B06-02-A01` | 记录真实数值步与损失；合法零梯度标 ZERO_GRADIENT 而非伪造参数变化；F13/F14 的更新与 state codec 共用版本。 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/TaskContinuationR26.java` | 同左 | `R341-B02-03-T01` | 切片续算游标递增且能重启；反复释放再申请不计有效进展 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/TaskLivenessR30.java` | 同左 | `R341-B01-04-T01` | 停住的热线程报告无业务推进；真实提交与其水位匹配 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/TrainingCheckpointSU1.java` | 同左 | `R341-B07-02-A01` | 合规快照恢复后下一步数值等价；损坏或错形状不能继续当旧父代 |
| 补入 `app/src/main/java/com/ethk3/novaonepro/strategyv2/TrainingDiagnosticsR26.java` | 同左 | `R341-B09-03-A01` | 诊断字段跟随真实源或显式缺失；不能把字段存在或空计数当优势 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/UpgradeDiagnosisSU2.java` | 同左 | `R341-B08-04-A01` | REV-B08-03 将 S4 的连续形态处方闭合至 S5 影子回读。实施入口与反例见 specs/review/algorithm.md / R341-B08-04-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/WarmCandidateR26.java` | 同左 | `R341-B06-01-A01` | 区分数值 ABI 不兼容、缺源和已损坏；保存原件状态不得覆盖为 READY。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java` | 同左 | `R341-B02-02-T01` | 持久化失败分类、最老年龄、丢失计数和retry due显式；排除fault-drain诊断再次递归生产同类hold故障。 |
| 补入 `release/r33_plan_binding.json` | 同左 | `R341-B01-01-T01` | 历史方案绑定用于兼容验证；不得把当前方案识别为旧方案 |
| `release/r341_batch_ledger.schema.json` | 同左 | `A-B12-03a` | 每批batchId、baseCommit、commit、changedFiles、issues、localTests、hostIntegration、apk、field、natural、rollbackArtifact与remainingRisks；每张票工单ID和scenarioId一一可查。 |
| `release/r341_device_campaign.schema.json` | 同左 | `R341-B05-03-T01` | F4-B05-02 在 B05 自身交付后台取证器，B12 从最终 APK 重新取证。见 specs/field4/flow.md |
| `release/r341_natural_handoff.schema.json` | 同左 | `A-B12-05a` | 列candidate/artifact/model hash、protocolHash、freezeAt、independentFutureStart、最低样本/观察天数、当前有效样本、预算/停止规则、下一验收时间和资格状态；不包含保证盈利字段。 |
| `release/r341_plan_binding.json` | 同左 | `R341-B01-01-T01` | NEW：封存方案上传后由真实 planCommit/planSha256/sourceBaseline/sourceApkEvidenceRef 生成；不填写猜测的 SHA，不修改旧 r33/r34 binding。 |
| 补入 `release/r82_training_invalidation.json` | 同左 | `R341-B07-03-A01` | 复核发布与数值失效规则及影响范围；更改旧依赖不得静默复用权重 |
| 补入 `release/release_identity.properties` | 同左 | `R341-B01-01-T01` | 实际构建读取一致的版本身份；故意错码应被识别 |
| 补入 `release/release_manifest.json` | 同左 | `A-B12-01a` | 发布清单与构建物匹配；其他提交清单不可签收 |
| 补入 `release/research_dependencies_r967.json` | 同左 | `R341-B07-03-A01` | 核对模型和实际依赖图；替换一个生效依赖须使旧身份失效 |
| 补入 `scripts/r967_semantic_identity.py` | 同左 | `R341-B07-03-A01` | 运行真实语义身份生成器并与应用产物核对；错依赖须改变结果 |
| `tools/quality_experiment_r341` | 同左 | `R341-B09-05-A01` | 离线命令解析固定spec/checkpoint/output参数并调用QualityExperimentR341；强制ENGINEERING_ONLY store、不可连接生产库；非完成返回非零状态。 |
| 补入 `tools/r31/package_release.py` | 同左 | `A-B12-06a` | 打出的包可恢复与下载核对；缺必需载荷或错摘要失败 |
| 补入 `tools/r31/verify_baseline.py` | 同左 | `R341-B01-01-T01` | 对正确底座可复验；混入其他版本拒绝开工 |
| `tools/r33/advance_release_version.py` | 同左 | `R341-B01-01-T01` | 加入 R34.1 与 B01–B12，保留原 series 的 stage 约束；versionCode=max(577311122,现场读值,发布索引读值,manifest值)+1，沿用已有锁与四文件原子 journal。 |
| `tools/r33/package_ci_candidate.py` | 同左 | `R341-B01-01-T01` | 旧033祖先核验保留；增加 currentInstalledBaseline 的真实122 APK/设备拉取哈希和签名/包名/版本检查；嵌入及封包名分派 R341，缺现场APK则标 COVER_INSTALL_UNVERIFIED，不生成可覆盖声明。 |
| `tools/r33/run_release_build.py` | 同左 | `R341-B01-01-T01` | 显式传递并验证 R33、R34、R341 三个封存根；缺任何实际要求的封存根即返回确定的构建前置错误。；REV-B01-01 把三个方案根分别绑定到实际构建输入和产物。实施入口与反例见 specs/review/build.md / R341-B01-01-T01。 |
| `tools/r33/verify_plan_binding.py` | 同左 | `R341-B01-01-T01` | 按 release.versionName 严格分派 R33.5/R34/R34.1；旧 r34 binding 的存在仅触发旧封印验证，不能强制对新版本运行旧版本正则。；REV-B01-01 把三个方案根分别绑定到实际构建输入和产物。实施入口与反例见 specs/review/build.md / R341-B01-01-T01。 |
| `tools/r34/build_identity.py` | 同左 | `R341-B01-01-T01` | 提取只验证旧 binding/成员/祖先链的 verify_seal(repo,head)；identity 再额外校验 R34 版本，原 R34 行为保持一致。 |
| 补入 `tools/r34/check_acceptance.py` | `construction/plans/STARCORE_R34_1_REPAIR_20261124/tools/verify_receipt.py` | `A-B12-03a` | 现用回执校验入口查到测量；无真实证据拒收 |
| 补入 `tools/r34/test_legacy_r335.py` | 同左 | `R341-B01-01-T01` | 历史兼容夹具对正确身份通过，破坏既有封印时拒收 |
| 补入 `tools/r34/test_plan_guards.py` | `construction/plans/STARCORE_R34_1_REPAIR_20261124/tools/test_receipt_guards.py` | `A-B12-03a` | 替代的回执反例套件可运行并拒绝篡改输入 |
| 补入 `tools/r34/verify_period_trace.py` | `tools/r341/verify_integrated_chain.py` | `A-B12-03a` | 替代的联调工具经过服务路径取原件；事后补冻反例应失败 |
| 补入 `tools/r34/verify_plan.py` | `construction/plans/STARCORE_R34_1_REPAIR_20261124/tools/verify_plan.py` | `A-B12-03a` | 现用结构校验核对当前封印；篡改受保护成员时拒收 |
| `tools/r341/build_identity.py` | 同左 | `R341-B01-01-T01` | NEW：读取 release/r341_plan_binding.json 和 STARCORE_R341_PLAN_ROOT；校验精确文件集合、每成员哈希、应用底座祖先关系、新版本批次、plan commit 与新 plan seal。返回 STARCORE_R341_BUILD_IDENTITY_1。 |
| `tools/r341/device_campaign.py` | 同左 | `R341-B05-03-T01` | F4-B05-02 在 B05 自身交付后台取证器，B12 从最终 APK 重新取证。见 specs/field4/flow.md |
| `tools/r341/publish_delivery_manifest.py` | 同左 | `A-B12-06a` | 生成外置DELIVERY_R341.json，列APK、source.zip、source.bundle、plan、批次台账、原始日志、现场/自然状态和SHA256；仅在仓库/已授权发布端写入。；实际HTTP/平台下载最终公开文件后重新算hash、枚举ZIP、复验签名/身份；仅得到页面链接不足以PASS。 |
| `tools/r341/verify_batch_ledger.py` | 同左 | `A-B12-03a` | 输入全部R341工单、文件变更、场景结果与证据文件；检查12批/全部工单覆盖、依赖顺序、缺省状态、源码commit及artifact hash。；输出ENGINEERING_COMPLETE/INCOMPLETE、FIELD_PENDING条目、NATURAL_PENDING条目，分别计算，不将自然未合格作为工程未做。 |
| `tools/r341/verify_delivery_identity.py` | 同左 | `A-B12-01a` | 读取B01新增release/r341_plan_binding.json与其build identity输出，并核对R33/R34历史封印，输出只读校验回执。；sourceBaseline必须5562f9b19aa09f9f7d9e69ca085db56c537967fc；planCommit固定真实已提交的方案commit；从clean HEAD推导sourceCommit，输出回执不写回源树。 |
| `tools/r341/verify_integrated_chain.py` | 同左 | `A-B12-03b` | 输入旧库/故障夹具和封装生产入口，输出创建→训练真实数值变更→发布→PIT→canonical/等待→结算反馈→研究帧/矩阵→action→消费的事件hash链；仿真scope固定SYNTHETIC，不授予自然资格。；F3-B12-01 无自然证书时跑通两个连续出生与第三代实际消费。按 specs/field3/chain.md 和 A-B12-03b 的正反例实施复验。 |
| `tools/r341/verify_natural_delivery.py` | 同左 | `A-B12-05a` | 严格复用B09定义的资格协议/门限，不另造宽松规则；冻结时间、输入可用时间、未来样本、真实成本、对照、多重比较控制、置信下界、漂移/连败均须可回放。；输出NATURAL_PENDING/QUALIFIED/REJECTED/REVOKED与证据缺口；资格不由APK封包器赋予。 |
| `tools/r341/verify_upgrade_install.py` | 同左 | `A-B12-02b` | 通过adb读取指定设备包名/版本/签名，pm path并pull实际已安装base.apk计算sha；只对明确选定设备安装。；执行adb install -r，安装后再次读取包名/版本/签名及数据迁移/原件hash；不卸载、不clear data、不-d降级。输出deviceId哈希化标识、前后版本、apkSHA及失败原因。 |
| `app/src/main/java/com/ethk3/novaonepro/BusinessReconcilerR36.java` | 同左 | `R341-B05-01-T01` | REV-B05-01 服务单次预约与持久开奖重派。实施入口与反例见 specs/review/runtime.md / R341-B05-01-T01。 |
| `app/src/main/java/com/ethk3/novaonepro/RecoveryCoordinatorR815.java` | 同左 | `R341-B05-02-T01` | REV-B05-02 恢复真实所有者与投递失败重试。实施入口与反例见 specs/review/runtime.md / R341-B05-02-T02。 |
| `app/src/main/java/com/ethk3/novaonepro/InternalWatchdogReceiverR815.java` | 同左 | `R341-B05-02-T01` | REV-B05-02 恢复真实所有者与投递失败重试。实施入口与反例见 specs/review/runtime.md / R341-B05-02-T02。 |
| `app/src/main/java/com/ethk3/novaonepro/ServiceWatchdogJobService.java` | 同左 | `R341-B05-02-T01` | REV-B05-02 恢复真实所有者与投递失败重试。实施入口与反例见 specs/review/runtime.md / R341-B05-02-T02。 |
| `app/src/main/java/com/ethk3/novaonepro/BaselineShadowR34.java` | 同左 | `R341-B04-04-T01` | REV-B04-01 S0 双事务赛前证据与逐期失败归属。实施入口与反例见 specs/review/runtime.md / R341-B04-04-T02。；F3-B04-01 同一期分开判断S0、PIT投影、正式冻结及结算。按 specs/field3/runtime.md 和 R341-B04-04-T02 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FeatureCacheR27.java` | 同左 | `R341-B08-01-A01` | REV-B08-01 进化卡在老代时可安全替换待用数据缓存。实施入口与反例见 specs/review/algorithm.md / R341-B08-01-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ContMatSU2.java` | 同左 | `R341-B08-04-A01` | REV-B08-03 将 S4 的连续形态处方闭合至 S5 影子回读。实施入口与反例见 specs/review/algorithm.md / R341-B08-04-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java` | 同左 | `R341-B08-04-A01` | REV-B08-03 将 S4 的连续形态处方闭合至 S5 影子回读。实施入口与反例见 specs/review/algorithm.md / R341-B08-04-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisShadowReceiptR335.java` | 同左 | `R341-B08-04-A01` | REV-B08-03 将 S4 的连续形态处方闭合至 S5 影子回读。实施入口与反例见 specs/review/algorithm.md / R341-B08-04-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchRepositoryR30R1.java` | 同左 | `R341-B08-02-A01` | REV-B08-04 逐图以有值原件或正式链原件验收产出。实施入口与反例见 specs/review/charts.md / R341-B08-02-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java` | 同左 | `A-B11-01a` | REV-B11-01 修正图表缓存及报告索引的零值和归因。实施入口与反例见 specs/review/charts.md / A-B11-01b。 |
| `app/src/main/java/com/ethk3/novaonepro/ChartsActivityR83.java` | 同左 | `A-B11-04a` | REV-B11-03 47个图和16个表从真实页面逐ID回读对账。实施入口与反例见 specs/review/charts.md / A-B11-04a。 |
| `app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchActivityR30R1.java` | 同左 | `A-B11-04a` | REV-B11-03 47个图和16个表从真实页面逐ID回读对账。实施入口与反例见 specs/review/charts.md / A-B11-04a。 |
| `app/src/main/java/com/ethk3/novaonepro/ResearchTablesActivityR31.java` | 同左 | `A-B11-04a` | REV-B11-03 47个图和16个表从真实页面逐ID回读对账。实施入口与反例见 specs/review/charts.md / A-B11-04a。；REV-B11-04 将分页负例改为真实可达的数据库状态。实施入口与反例见 specs/review/charts.md / A-B11-04b。 |
| `app/src/main/java/com/ethk3/novaonepro/ReportPhaseBudgetR31.java` | 同左 | `A-B10-01a` | REV-B10-02 为大库一致快照独立预约实际峰值。实施入口与反例见 specs/review/storage.md / A-B10-01a。 |
| `app/src/main/java/com/ethk3/novaonepro/ReportArchiveVerifierCP12.java` | 同左 | `A-B10-02a` | REV-B10-03 先证明交付件可恢复，再清理原始工作区。实施入口与反例见 specs/review/storage.md / A-B10-02b。 |
| `app/src/main/java/com/ethk3/novaonepro/ReportBudgetSU2.java` | 同左 | `A-B10-02a` | REV-B10-03 先证明交付件可恢复，再清理原始工作区。实施入口与反例见 specs/review/storage.md / A-B10-02b。 |
| `app/src/main/java/com/ethk3/novaonepro/ReportSafCopyCP12.java` | 同左 | `A-B11-06a` | REV-B11-05 报告与赛前服务并行，并验手机端持久保存。实施入口与反例见 specs/review/storage.md / A-B11-06b。 |
| `app/src/main/java/com/ethk3/novaonepro/K3FailureDiagnosisEngine.java` | 同左 | `A-B11-03a` | REV-B11-06 RSEC-066/069/070/071 的三项编译占位与六项自由文本改为可重算来源。实施入口与反例见 specs/review/report.md / A-B11-03b。 |
| `app/src/main/java/com/ethk3/novaonepro/FormulaAlgorithmAuditEngine.java` | 同左 | `A-B11-03a` | REV-B11-06 RSEC-066/069/070/071 的三项编译占位与六项自由文本改为可重算来源。实施入口与反例见 specs/review/report.md / A-B11-03b。 |
| `app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java` | 同左 | `R341-B07-03-A01` | REV-B07-02 十位置概率面与同目标1200个动作排序建立可复算关系。实施入口与反例见 specs/review/report.md / R341-B07-03-A01。 |
| `app/src/main/java/com/ethk3/novaonepro/CodeOwnershipCompletenessAuditEngine.java` | 同左 | `A-B11-03a` | REV-B11-06 RSEC-066/069/070/071 的三项编译占位与六项自由文本改为可重算来源。实施入口与反例见 specs/review/report.md / A-B11-03b。 |
| `app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java` | 同左 | `A-B11-02a` | F3-B11-01 修复R923结算分子分母跨历史范围。按 specs/field3/runtime.md 和 A-B11-02b 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/SettlementCoverageTruthR906.java` | 同左 | `A-B11-02a` | F3-B11-01 修复R923结算分子分母跨历史范围。按 specs/field3/runtime.md 和 A-B11-02b 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportRepairCaseIndexR335.java` | 同左 | `A-B11-05a` | F3-B11-02 将每条升级方向接到施工回执、研究执行或只读复验，再由下一次诊断核对。按 specs/field3/directions.md 和 A-B11-05b 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaFamilyR27.java` | 同左 | `R341-B06-03-A01` | F3-B06-02 21个思路族的可执行公式与报告处方逐一对照。按 specs/field3/learning.md 和 R341-B06-03-A01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/AtomReplayR27.java` | 同左 | `R341-B07-03-A01` | F3-B07-01 区分深度网络两个输出头及各自被哪种动作消费。按 specs/field3/learning.md 和 R341-B07-03-A02 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetR27.java` | 同左 | `R341-B07-03-A01` | F3-B07-01 区分深度网络两个输出头及各自被哪种动作消费。按 specs/field3/learning.md 和 R341-B07-03-A02 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomWorldR27.java` | 同左 | `R341-B08-04-A01` | F3-B08-02 原子评分、派生原子及分子切换后，真实出生不沿用旧叶。按 specs/field3/learning.md 和 R341-B08-04-A02 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomPublicationStoreR31.java` | 同左 | `R341-B08-04-A01` | F3-B08-02 原子评分、派生原子及分子切换后，真实出生不沿用旧叶。按 specs/field3/learning.md 和 R341-B08-04-A02 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionStudyR30R1.java` | 同左 | `R341-B08-06-A01` | F3-B08-03 融合只给真正进入子代且获得实测贡献的父代记功。按 specs/field3/learning.md 和 R341-B08-06-A02 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/SetIntersectionPolicyR30.java` | 同左 | `R341-B08-02-A01` | F3-B08-05 R04按实际提示窗口核算两个来源、交集及弃权代价。按 specs/field3/research.md 和 R341-B08-02-A01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/HypothesisStoreR31.java` | 同左 | `R341-B08-07-A01` | F3-B08-07 把家族开发信用限制在当前可比实验，将分配追到下一轮实际投递。按 specs/field3/research.md 和 R341-B08-07-A03 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI13SelectorOnlyR27.java` | 同左 | `R341-B09-01-A01` | F3-B09-01 将I13单独选择时机的价值与只挑高分时段的效果分开。按 specs/field3/research.md 和 R341-B09-01-A01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/MultiHorizonProbeR80.java` | 同左 | `R341-B04-05-T01` | F3-B04-03 为多步长探针留下每个应到锚点的可核对终态。按 specs/field3/extended.md 和 R341-B04-05-T02 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/IssueTimelineR30.java` | 同左 | `R341-B05-03-T01` | F3-B05-03 同时验收开奖后五秒内出号与距离目标截止至少五秒。按 specs/field3/extended.md 和 R341-B05-03-T01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelAblationStoreSU1.java` | 同左 | `R341-B08-07-A01` | F3-B08-08 策略节点消融与模型核消融分别接入下一轮研究预算。按 specs/field3/extended.md 和 R341-B08-07-A01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR29.java` | 同左 | `R341-B08-07-A01` | F3-B08-08 策略节点消融与模型核消融分别接入下一轮研究预算。按 specs/field3/extended.md 和 R341-B08-07-A01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/GateV2R29.java` | 同左 | `R341-B09-04-A01` | F3-B09-02 把报告 R29F-GATEV2-NOT-RUN 的分支查到实际评估回执。按 specs/field3/extended.md 和 R341-B09-04-A01 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisRulesR30.java` | 同左 | `A-B11-05a` | F3-B11-04 让报告提出的根因、采集动作和反证回查到真实运行。按 specs/field3/extended.md 和 A-B11-05b 的正反例实施复验。 |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/StoreR27.java` | 同左 | `R341-B06-03-A01` | F4-B06-03 同一公式的历史拒绝与已成功出生分别处理。见 specs/field4/birth.md |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/FactoryCapabilityExperimentR31.java` | 同左 | `R341-B09-05-A01` | F4-B09-03 将三个零假设臂的名义60次与20个共同种子组分别复算。见 specs/field4/quality.md；F4-B09-04 分出序贯检出力与完整工厂从创建到通过证书数学门的检出能力。见 specs/field4/quality.md |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchCapabilityExperimentR31.java` | 同左 | `R341-B09-05-A01` | F4-B09-03 将三个零假设臂的名义60次与20个共同种子组分别复算。见 specs/field4/quality.md |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/NullPipelineSimulationR30.java` | 同左 | `R341-B09-05-A01` | F4-B09-04 分出序贯检出力与完整工厂从创建到通过证书数学门的检出能力。见 specs/field4/quality.md |
| `tools/r341/fixtures/continuity_b12.json` | 同左 | `A-B12-03b` | F4-B12-02 按固定输入和预登记周期验证跨重启持续生产。见 specs/field4/continuity.md |
| `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayKnowledgeStoreR31.java` | 同左 | `R341-B08-01-A01` | RuntimeR27.DatasetTask.step/publishRecoveredReplayKnowledgeR31、ResearchInputPinR30R1.loadCurrent/require、ReplayKnowledgeStoreR31.isPublishedR31 区分已匹配的投影、需重新物化的投影以及失效的原输入；保持 owner、pin、资格与实时截止的原条件。 |

## 测试入口自身的源码

以下 13 个测试入口由对应验收用例核对 runner 路径、方法和本提交的 Git blob。

- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B01ContractTest.java`
- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B02ContractTest.java`
- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B03ContractTest.java`
- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B04ContractTest.java`
- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B05ContractTest.java`
- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B06ContractTest.java`
- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B07ContractTest.java`
- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B08ContractTest.java`
- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B09ContractTest.java`
- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B10ContractTest.java`
- `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B11ContractTest.java`
- `tools/r341/test_b01_contracts.py`
- `tools/r341/test_b12_contracts.py`
