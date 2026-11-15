# 五批施工：问题、文件与具体改法

本清单按R32.5.1-C1的实际代码manifest和问题卡生成。5批、18个问题、312个唯一受控文件、410张问题—文件卡；实际改动120件（新增64、替换56），另192件原字节保全。

| 批次 | 问题数 | 文件数 | 新增 | 替换 | 保全 |
|---|---:|---:|---:|---:|---:|
| P1 | 6 | 58 | 14 | 12 | 32 |
| P2 | 6 | 148 | 23 | 18 | 107 |
| P3 | 2 | 25 | 1 | 4 | 20 |
| P4 | 3 | 54 | 26 | 10 | 18 |
| P5 | 1 | 27 | 0 | 12 | 15 |

## P1：问题与改法

### P1.01 旧库三列与两个迁移入口

032 旧库快照的 link candidate 表只有部分列。helper 已加三列，store owner 在索引前漏了同一组三列。旧库累计尝试 394→589。

具体施工：按 P1.patch 在实际严格迁移 owner 先补 target_issue、candidate_rank、candidate_key，再创建唯一索引；检查 table_info/index_info、触发器声明及已存在重复键。在同一事务重算单元后置条件。

验收：重放仅含 snapshot_key 的旧表、三列分别已存在、错误索引、多条NULL、重复非空键；中断两次恢复，行数/原行摘要不变，有且只有一条完成收据。

### P1.02 铸造配额与生命周期迁移

foundry-su2 两次都在 408/4119，QUOTA_MISSING。表存在，单例无行。生命周期其他对象 66→130 时它仍未被初始化。

具体施工：在 LifecycleSchemaR31.advanceWithinMigration 内先核对假设表和全部触发器的精确定义；由 HypothesisStoreR31.recoverQuotaSliceR32 计算旧行原始计费，四表全空时同事务初始化。四表有数据时每事务最多64行，以原主键续页，最后一页把精确字节与完成状态一起提交。业务调用保持 requireSchemaR31 只读。

验收：空库首个可用工厂事务；有旧行删配额（多字节payload、三列复合键、恰好64及跨页）后重启；断在提交前后各一次，最后重算值等于原触发器账，重复插入不加费；原件被篡改或触发器缺失不得给出零配额。

### P1.03 实时尾段、下一期预冻结

5000行历史中该时点仅2期连续；门槛12。通用 R914_DRAW_HISTORY_BATCH 按研究写者准入；出现53条带积压原因的新延迟事件。

具体施工：在 saveApiResultDraws 的历史页入口提取独立修尾段任务；不依赖 quick 文本原因或最新开奖记录回调。先调度现有最新开奖，写者每片重读当前 head、截止与目标前12期，仅提升无冲突的已知连续前缀，沿用每片至多8行；普通历史仍走原回补。须补齐合并期间的持久目标、服务重启重投和不同来源冲突回执。

验收：输入乱序、跨日、缺中间期、冲突开奖、已过截止、相同页两次。检查 target 只用此前可见数据、发生真实 B04 冻结或留下准确错过原因。模拟后包覆盖变化并量P95和分母。

### P1.04 写者故障回执、服务调度与后台

写者积压使历史和研究被退让；报告进程pending=0不能说明主进程故障队列已排干。原runtime-observation的fetchCadenceR31记录本服务实例54957个1秒调度目标槽和141次唤醒，scope=CURRENT_SERVICE_INSTANCE_MONOTONIC_SCHEDULER_NOT_HTTP_OR_DEVICE_SLO；它不等于两报告1131期同窗HTTP或设备SLO，也不能单凭该值断言进程被杀。

具体施工：P1.patch 使主进程控制 tick 先排故障再触发旧库迁移；每次最多4笔、单笔独立提交，第二笔起检查40ms及关键工作，失败保留队头。补上持久化进程/boot/session、oldest事件、phase、到期时刻、下次尝试；从 provider 请求/响应→commit/冻结逐段计时。

验收：在END前/后杀进程，ACK未记、控制线程被阻、重复快照、时钟跳变、屏灭各跑一次；短切片不可抢占处按真实超预算计；对齐同进程的排队、落库和受阻变化。

### P1.05 冻结、结算、训练事件的因果关系

两报相隔1131个日历期，仅新增58个已采集开奖；其中51个无影子预冻结、7个已有影子预冻结并结算，7个中只有6个达到至少提前5秒，另1个仅4880ms。另有2个预冻结目标未采集到开奖。1073个未采集日历期与51个已采集但未预冻结必须分母分列。

具体施工：以已确认 draw 的唯一 issue/来源摘要入账，按原冻结ID派发结算和独立合法学习事件；分别持久化数据可见时刻、freezeAt、openedAt、settledAt；都以事务提交为准。匹配不到冻结单列原因，旧已冻结单重试按唯一键重放。

验收：同期开两个回调、后台恢复、杀于结算后ACK前、旧历史覆盖；一次收益、每个合法训练事件至多一次且可补；主链资格接口沿用现有签名。

### P1.06 存储和缺失 checkpoint 可恢复

后包仍缺556个历史对象，另有未核验项；物理体积约2.96GB，含受保护备份。恢复读取不到文件会影响训练或导出。

具体施工：本批6份相关生产文件原字节保全，本问题没有新增写文件。复核既有保护根、缺件审计和恢复入口，逐件核对ABSENT/CORRUPT/UNVERIFIED及受影响资产；只有获得真实已验真副本时才按原恢复协议回读SHA/大小并发布。跨冷热完整根图、统一resolver、删除许可与安全回收的替换实现由P2.06同批交付；P1不提前依赖其新增接口，不把本卡保全源码称作新增闭环。

验收：准备→替换→确认任一阶段中断可恢复；旧URI无权限、磁盘满、空引用图、大对象均明确状态。出包实测磁盘和堆/PSS各自增长曲线。

本批全部唯一owner文件如下。ADD/REPLACE是实际写入；PRESERVE_REFERENCE只保全原字节，不能计为新增实现。

| 动作 | 完整路径 | 关联问题 |
|---|---|---|
| REPLACE | `.gitignore` | P1.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/AnalysisRequestStoreR36.java` | P1.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/AnalysisSingleFlightR36.java` | P1.03 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java` | P1.03, P1.04, P1.05, P3.02, P5.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/BootReceiver.java` | P1.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/CanonicalObservationWriterR36.java` | P1.05, P3.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/CountdownClock.java` | P1.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/CountdownProjectionR971.java` | P1.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/DataRetentionGovernorR900.java` | P1.06 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java` | P1.03, P1.04, P2.01, P2.02, P4.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/DbHelper.java` | P1.01 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/DbStore.java` | P1.01, P1.03, P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java` | P1.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java` | P1.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/HistoryIntegrityVerifierR815.java` | P1.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/LegacyDdlR31.java` | P1.01 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/LegacySchemaCoordinatorR926.java` | P1.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/LifecycleHealthR963.java` | P1.04 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/LifecycleSchemaR31.java` | P1.02, P2.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/OosAfterDrawWriterR36.java` | P1.05 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/PredrawTailRecoveryR32.java` | P1.03 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/R36AnalysisHistoryPolicyR364.java` | P1.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/RealtimePressureGateR811.java` | P1.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/RealtimeTaskScheduler.java` | P1.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ReportCleanupR31.java` | P1.06, P4.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/RuntimeMemoryGuardR811.java` | P1.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/RuntimeResourceAuditR927.java` | P1.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java` | P1.05, P3.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/SettlementTaskStoreR36.java` | P1.05, P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/StorageBudgetR31.java` | P1.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/StorageGcR31.java` | P1.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/TargetIssueResolver.java` | P1.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/V51Repository.java` | P1.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/V51RuntimeInputR36.java` | P1.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/V51RuntimeOrchestrator.java` | P1.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/KnowledgeLedgerR952.java` | P1.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/WorldFeatureComputationR974.java` | P1.05 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/HypothesisStoreR31.java` | P1.02, P2.03, P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/KeepMigrationStoreR31.java` | P1.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java` | P1.06, P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java` | P1.04 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/WriterHoldFaultsR31.java` | P1.04, P2.01 |
| REPLACE | `app/src/test/java/com/ethk3/novaonepro/R31RetirementManifestTest.java` | P1.01, P1.03 |
| REPLACE | `app/src/test/java/com/ethk3/novaonepro/strategyv2/R27QualityTest.java` | P1.01, P1.03 |
| REPLACE | `tools/r31/retirement_manifest.json` | P1.01, P1.03 |
| ADD | `tools/r32/p1_contracts/After.java` | P1.01, P1.03 |
| ADD | `tools/r32/p1_contracts/Assert.java` | P1.01, P1.03 |
| ADD | `tools/r32/p1_contracts/Before.java` | P1.01, P1.03 |
| ADD | `tools/r32/p1_contracts/HostSelectedTests.java` | P1.01, P1.03 |
| ADD | `tools/r32/p1_contracts/README.md` | P1.01, P1.03 |
| ADD | `tools/r32/p1_contracts/run_selected_contracts.py` | P1.01, P1.03 |
| ADD | `tools/r32/p1_current/README.md` | P1.01, P1.02, P1.03 |
| ADD | `tools/r32/p1_current/acceptance_bindings.json` | P1.01, P1.02, P1.03 |
| ADD | `tools/r32/p1_current/run_p1_current_host.py` | P1.01, P1.02, P1.03 |
| ADD | `tools/r32/p1_current/tests/CompletedDriftR32Host.java` | P1.01, P1.02, P1.03 |
| ADD | `tools/r32/p1_current/tests/HypothesisQuotaR32Host.java` | P1.01, P1.02, P1.03 |
| ADD | `tools/r32/p1_current/tests/TailRecoveryProbe.java` | P1.01, P1.02, P1.03 |
| ADD | `verification/r32/p1_dbstore_review.json` | P1.01, P1.03 |

## P2：问题与改法

### P2.01 恢复回放知识并解除下游等待

r27-replay-dataset 两个时点都为3步/3597847，一次END回滚后一次ADMISSION回滚；evolution停4步/13。另有源码风险，恢复缓存绑定可能在 evoData 已发布后抛出。

具体施工：P2.patch 在私有缓存绑定历史模型之后才提交知识、再发布已就绪缓存；失败释放私有缓存。逐层保留 retryable admission / END rollback / committed ACK lost 的事务证据。只用当前 pin+revision+ABI 读回恢复，同一已提交收据不制造新训练步。

验收：用持久提交的 pin 冷启动，依次注入准入、END、绑定、ACK故障；一旦writer恢复，下游能拿到同一缓存并真的计算。分别比对optimizer/游标/状态哈希。

### P2.02 事件驱动的历史训练与 AtomNet 十位置

有历史3453个梯度记录、714个统计记录；AtomNet材料2640→3150，完整训练/已发布权重/在线消费仍0。反馈增长也存在 NOT_MODEL_TRAINING。

具体施工：同一P2批次替换AtomNetPublicationR31、AtomNetStoreR30、AtomNetRuntimeR30、AtomNetSessionR30、RuntimeR27。优化器checkpoint与完整推理模型分开恢复；V2仅完整期创建推理权重，P1–P9与EVAL恢复上一完整模型；按owner/dataset/完整水位、原checkpoint与权重hash及发布时间核验，源头变化重试。物化只至TRAIN+SELECT，既有holdout字节保留；新增索引沿用唯一迁移owner。保持预测在先、合法标签在后和独立事件水位；具体完整代码、71断言范围及未测运行时项见audit/engine。

验收：固定样本重启续训数值等价，乱序、标签尚未知、缺位置7、zero-gradient、同模型旧缓存都不得冒领“已发布已消费”。再用真实事件检查滞后。

### P2.03 旧候选保全、下一轮出生与无总量上限

旧50个候选指纹、游标和验证数不变；均是 BLOCKED_WARM_NUMERICAL_ABI_PRESERVED。没有本区间新策略出生凭据。

具体施工：用原 warm 归档和明确的数值兼容声明保留旧对象；合法 warm fork 需要更新数据和独立新ID，不兼容保留原件、以当前ABI冷启合法种子。P2.patch 将父代长期固定的前几个供体改为同快照可重复、跨期轮换的异机制供体；新子代注册 sourceHash/parent/结构/算子/代号。

验收：跨多个campaign，使用足够新数据；验证有可执行、可重建、去重后的出生记录和后续首个真正训练步。用同一输入重放不增数，旧50不丢，垃圾释放槽后继续多轮。

### P2.04 思路族、策略族、失败假设和短期窗口

静态82能力登记不等于实际训练/产出。24家族报告last_progress为0，但50个持久候选有游标且被ABI保全门隔离，专家也有审计行。F23仅在当前报告进程binding不可用；已存在持久teacher/state/asset引用，不能推断为所有教师原件不存在。

具体施工：从连续失误按位置/状态分解校准、覆盖、窗口漂移；每项形成可证伪假设、对应 typed AST 编辑和禁用条件。保留多位置多号交集作对照，同期比较去窗口、去交集、延迟一个窗口。只把有效的因果短期状态送入下一期单位置固定K3生成。教师缺源仅挂本支，其他家族继续。

验收：输入常量、洗牌、含弱结构三组；固定预算测发现率与假阳性；打印每个族输入、操作、artifact及淘汰原因；拒绝读取当前标签或使用未来窗口。

### P2.05 原子、矩阵、实质变异与融合

20个矩阵inputAvailable=false；19项保留原件计数明确为0，1项计数未知，T06/T10有空点阻断，T15缺消费。结构尝试10、行为新颖0。

具体施工：原子登记输入列、lag、位置、量纲、可见时间、支持数及失效场景。矩阵按kind独立物化，带shape/axis/role/target/ABI/inputHash。通过已有typed消费者引用，记录不同输入实际改变的权重/掩码/候选。组合两个合法组件，同目标父代才可做融合。

验收：逐种矩阵实施原件读回，错轴/错期/错role/缺教师负例；移除原子、矩阵、一个父亲观察输出差与配对损失；不能把存储/可视化算成算法使用。

### P2.06 训练筛劣、淘汰与代际证据

R32.3活动计数已经修复，但热CAS256MiB/8192文件、全图16384节点和样本永不回收仍会形成长期停机；旧模型证据不能直接删除。本轮补完整冷热寻址、已验证样本归档、分页回收与故障恢复代码。；本轮复现旧坏归档对象阻断AtomNet训练恢复，局部错误与全局失败需分清。

具体施工：在 FoundryCampaignR26.finish 保留 DEV、SELECT 两段，用两段描述性 Wilson 下界的较小值排历史父代，平分看较小支持与不可变策略键；排除受控消融对象，至多保留三个待选排名对象。保留另外两份独立账记录长期同机会配对与未来资格。CreationBudgetR31 只控制同时活跃资源。对低效候选停止排程、撤出亲本池，证据保全后回收预算。 同批安装ColdCheckpointR324、AtomNetSampleArchiveR324和CheckpointGcReachabilityR324及所有读取/迁移/预算适配。原件原始SHA不变，冷包读回成功才退热；旧样本原始特征和标签按原codec/digest归档，再在短事务许可下删除SQLite重复行。完整旧热件census完成后才按哈希64件分页回收，重新核对当前根和未删除父节点。；将样本归档交独立r27-atomnet-archive任务，增加对象级持久attempt和公平到期重试，完整模型恢复不依赖归档成功。

验收：1/1与40/100、跨段崩溃、平分、缺样本均可重放；负控不能当亲本。相同预算测亲本选中的净效应、宽度、计算成本、最差窗口、代际增量。已知弱者退休过程中杀进程，历史证据仍可回读；释放的可用槽能创建具有独立样本账的下一代。 原字节冷热回读、损坏/截断/错误hash拒绝、归档步骤断电、样本许可回滚、未授许可删除拒绝、旧V1/V2回读、当前模型不归档、热目录未完整登记不得claim、超过16384历史节点仍分页、root/pin/父子保护、失败墓碑隔离、重启游标和物理字节只记一次；日志须明确宿主/设备范围。 无根组第二成员SQL失败整组回滚；部分unlink后保留组许可、拒绝中途同hash复活；65节点环与超过32MiB组明确保全且其他安全候选继续。；另运行本批archive_liveness当前生产/原生SQLite41断言，保持取消/不确定提交/全局错误边界。

本批全部唯一owner文件如下。ADD/REPLACE是实际写入；PRESERVE_REFERENCE只保全原字节，不能计为新增实现。

| 动作 | 完整路径 | 关联问题 |
|---|---|---|
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/AblationRepairCreationR930.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/AtomAdmissionAuditR932.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/AtomicToolWorldR943.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/CandidateCalibrationR932.java` | P2.02 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/CheckpointReportAuditR32.java` | P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java` | P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/MatrixSnapshotFeedbackR930.java` | P2.04, P2.05 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/ReportAttachmentClosureCP12.java` | P2.06, P4.03 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/ReportMemberPlanCP12.java` | P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ResearchEvaluationRunR967.java` | P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ResearchEvolutionStoreR930.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ResearchLogisticCalibrationR932.java` | P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ResearchTaskQueueR967.java` | P2.01 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java` | P2.06 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/StorageRestoreAssetsR324.java` | P2.06 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/StorageRestoreBusinessProbeR31.java` | P2.06 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/StorageRestoreProbeServiceR31.java` | P2.06 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/StorageRestoreProofR31.java` | P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/TrainingContinuationR973.java` | P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/TypedAtomMoleculeGraphR930.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/CandidateAstR952.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/CandidateCompilerR952.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/ComputationReceiptR974.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java` | P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentTrainerCP9.java` | P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/EvolutionControllerR952.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/EvolutionEvidenceR975.java` | P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/LearningCursorR952.java` | P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/MatrixBindingsCP12.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/MatrixSnapshotBusR952.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/PostSettlementResearchLoopR952.java` | P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java` | P2.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/ResearchMigrationRegistryR952.java` | P2.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/ResearchOutboxR952.java` | P2.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/r952/ResearchTrainingHookRegistryR952.java` | P2.02 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetPublicationR31.java` | P2.02, P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetR27.java` | P2.04 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetRuntimeR30.java` | P2.02, P2.04, P2.06, P4.01, P4.02 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSampleArchiveR324.java` | P2.06 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java` | P2.02 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetStoreR30.java` | P2.02, P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetTrainerR27.java` | P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomWorldR27.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/BirthGateR27.java` | P2.04, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CalibrationRowsR27.java` | P4.02 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointArtifactsR26.java` | P2.03, P2.06 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointClosureR324.java` | P2.06 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcReachabilityR324.java` | P2.06 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcStoreR31.java` | P2.06, P4.03 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointReferencesR26.java` | P2.06 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ColdCheckpointR324.java` | P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CreationBudgetR31.java` | P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/DerivedAtomR27.java` | P2.05, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/DistillationFamilyR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/EliteArchiveR27.java` | P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceBlockR26.java` | P3.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.java` | P2.04, P2.06, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ExpertProposalsR27.java` | P2.04, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FactoryCapabilityExperimentR31.java` | P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDirectedResearchR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureProfileR30.java` | P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF01KernelR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF02KernelR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF08KernelR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF09KernelR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF11KernelR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF12KernelR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF18KernelR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF19TensorR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF20KernelR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyF21KernelR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FamilyRegistryR26.java` | P2.04 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FoundryCampaignR26.java` | P2.03, P2.06, P3.01, P4.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FrozenConfirmationR30.java` | P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/FusionOpsR27.java` | P2.05, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/HoldoutLedgerR27.java` | P2.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaFamilyR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI00RandomBaselineR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI01FrequencyReversionR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI02GapCycleR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI03TransitionChainR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI04CrossPositionR27.java` | P2.04, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI05PermutationConsistencyR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI06RegimeSwitchR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI07FragmentRetrievalR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI08NeuralRepresentationR27.java` | P2.04, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI09SymbolicFormulaR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI10FailureRepairR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI11ComplementaryEnsembleR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI12LegacyAssetR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/IdeaI13SelectorOnlyR27.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ImprovementStoreR31.java` | P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixEvidenceR30R1.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryR30.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixResearchR30R1.java` | P2.05, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixSnapshotSU2.java` | P2.04, P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixStoreSU2.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelArtifactR29.java` | P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelEvolutionSU2.java` | P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelGenomeSU2.java` | P2.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelPublicationR31.java` | P2.03, P3.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelReplayStudyR30R1.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ModelSurfaceStoreR30.java` | P2.02, P3.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MutationOpsR27.java` | P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/NeuralFamilyR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/OnlineModelsSU2.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/PairedComponentStudyR30R1.java` | P2.05, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/PermutationModelR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/PitGuardR30.java` | P2.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/PortfolioFamilyR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/RegimeHypothesisR31.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayEvaluatorR27.java` | P2.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReplayKnowledgeStoreR31.java` | P2.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchContextR30R1.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchHypothesisR31.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchInputPinR30R1.java` | P2.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java` | P2.01, P2.05, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchStoreR30R1.java` | P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAdaptersR30R1.java` | P4.02 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTaskRegistryR30R1.java` | P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java` | P2.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResidualFamilyR26.java` | P2.04 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeR27.java` | P2.01, P2.02, P2.05, P2.06 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/SetFamilyR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/SetIntersectionPolicyR30.java` | P2.04, P4.02 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/StructuralProposalR26.java` | P2.03, P2.05 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/StructureGeneratorRegistryR30.java` | P2.04, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/SymbolicFamilyR26.java` | P2.04 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/TrainingStepStoreR26.java` | P2.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/WarmCandidateR26.java` | P2.03 |
| ADD | `app/src/test/java/com/ethk3/novaonepro/strategyv2/R32PublishedModelDuringTrainingTest.java` | P2.02 |
| ADD | `tools/r32/AtomNetRecoveryR32Host.java` | P2.02, P2.06 |
| ADD | `tools/r32/run_atomnet_recovery_host.py` | P2.02, P2.06 |
| ADD | `tools/r324/gc/GcClosureR324Host.java` | P2.06 |
| ADD | `tools/r324/gc/README.md` | P2.06 |
| ADD | `tools/r324/gc/run_gc_closure.py` | P2.06 |
| ADD | `tools/r324/retention/AtomNetArchiveR324NativeHostTest.java` | P2.06 |
| ADD | `tools/r324/retention/CheckpointClosureR324NativeHostTest.java` | P2.06 |
| ADD | `tools/r324/retention/ColdCheckpointR324HostTest.java` | P2.06 |
| ADD | `tools/r324/retention/README.md` | P2.06 |
| ADD | `tools/r324/retention/StorageRestoreAssetsR324HostTest.java` | P2.06 |
| ADD | `tools/r324/retention/run_closure_host.py` | P2.06 |
| ADD | `tools/r324/retention/run_retention_host.py` | P2.06 |
| ADD | `tools/r325/archive_liveness/ArchiveLivenessFixtureR325.java` | P2.06 |
| ADD | `tools/r325/archive_liveness/AtomNetArchiveR325HostTest.java` | P2.06 |
| ADD | `tools/r325/archive_liveness/LegacyArchiveStarvationR325Probe.java` | P2.06 |
| ADD | `tools/r325/archive_liveness/README.md` | P2.06 |
| ADD | `tools/r325/archive_liveness/run_archive_liveness.py` | P2.06 |

## P3：问题与改法

### P3.01 冻结的未来观察和资格

新旧包未来策略/有效未来出手/证书均为0。高胜率、稳定性无法从历史数推定。

具体施工：分别保全 R27_GATE_V1 和 R26_PREREGISTERED_AGGREGATE_LCB_V3 的输入、机会日历、分母和证书表。R27 保留实际序贯登记，通过250笔未来出手等既有条件。SU2 从 QualificationWindowR26.completed 到 CertificateSU2.issueWithin 复核69120个机会的四段原始聚合、配对及后段校准。见修订指南的双路对照。分别核对数据完整性与同策略版本，二者不能互签。

验收：两套门各测刚低于/等于/刚超过边界，以及缺标签、错ABI、撤销与错策略。完成窗口、校准与正式签发全部使用同一不可变证据。保留真实032零证书作负例；工程正例仅用独立测试账。

### P3.02 真实主链消费、每期唯一和撤销

真实032的独立未来、当前证书及REAL均为0。源码另有并发候选遗漏风险：影子R27会先于已证实SU2返回，且旧理由解析会将SU2带到通用证书表。两份现场报告无法证明当时遇到双候选。

具体施工：同批修 MainlineBridgeSU2.intent 与 UnifiedResearchChoiceR932 的选择/原因读取；保留前置 R958、SU1 及正式 R27 的原优先级。已有不可变目标选择先复用；首次选择时同策略、同证书、有效冻结、模型存在、选择器确认且ACT的SU2才可越过影子R27。走现有 DecisionGateEngine 的最终授权，原账唯一结算。读取R27、SU1、SU2各自权威资格来源并记录用户真实开关状态。

验收：同目标并发给出影子R27及有证SU2，在用户授权的测试库内经正常冻结/结算恰一次；交换顺序、R27已认证、候选ID错、证书撤销、未授权、模型缺失、重复回调、截止后再次选择均验证原始行和拒绝原因。

本批全部唯一owner文件如下。ADD/REPLACE是实际写入；PRESERVE_REFERENCE只保全原字节，不能计为新增实现。

| 动作 | 完整路径 | 关联问题 |
|---|---|---|
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/CertificateBundleServiceR929.java` | P3.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/CertifiedPredictionR974.java` | P3.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/DecisionAuthorizationEngine.java` | P3.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java` | P3.02, P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/FixedWindowCertificatePublisherR932.java` | P3.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ResearchMainlineContributionAuditR922.java` | P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ResearchMainlineGovernorR903.java` | P3.02 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java` | P3.02 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java` | P3.01, P5.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateRevocationR30.java` | P3.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateSU2.java` | P3.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/EvidenceContractR31.java` | P3.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/GateV2R29.java` | P3.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/LiveShadowsR27.java` | P3.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineAdapterR31.java` | P3.02 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineBridgeSU2.java` | P3.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineChoiceStoreR31.java` | P3.02, P4.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineFreezeProofR31.java` | P3.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlineModelBindingR31.java` | P3.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationGapRepairR30R1.java` | P3.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationInboxR29.java` | P3.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationWindowR26.java` | P3.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/SequentialQualificationR27.java` | P3.01 |
| REPLACE | `tools/r31/run_typed_mainline_host.py` | P3.02 |
| ADD | `tools/r32/run_mainline_host.py` | P3.02 |

## P4：问题与改法

### P4.01 逐对象的诊断、处方和可读出口

原96项已执行且44指标在册，但16条诊断规则8条证据不足、1条未知；旧UPGRADE文件存在空处方，用户无法知道该改谁。 R32.4复核发现Q08合成角色误写为自然未来、总评漏Q08及设备组漏M33/M34、UNKNOWN遮蔽FAIL及顶层/回执值不一致未拒绝。；Q08真实原件被展示缓存截断且消费者未读；Q05/Q07支持单位混淆；29旧图表缺文件方法定位。

具体施工：同P4替换报告生产、快照、对象索引、指标证据投影、图表、语义验证和导出消费者；新增CapabilityRuntimeEvidenceR33同一SELECT368个字段，分对象区分登记/实际运行/产出/消费/效果，缺源为UNKNOWN；ReportAssessmentR33按原指标证据聚合状态，不写死、不改变计算阈值。RepairDiagnosisR32与ResearchTableAuditR32给出具体源文件/方法/原因/下一步/反例。JSON、Markdown同源，离线旧报告不能虚构旧快照未保存的统计。 本轮将Q08独立列为syntheticValidation并纳入overall，按既有合成试验完整性与原统计门分别核验；Q01–Q07自然数据角色、逐资产原资格及DecisionGate权限不变；Q05/Q07仅修报告支持单位。校验id/version/value/samples/minimum一致，已知FAIL优先。；同批加入Q08专用有界只读采集和原注册/源/协议/原件集合投影，Q05报告200校准观察、Q07报告1资产记录，逐资产自然原门不变；按真实登记组填165对象生产文件方法，独立UPGRADE_DIAGNOSIS同快照输出。

验收：用032两份快照恢复或授权脱敏fixture，检查每组ID、同一watermark与数值JSON/MD相同；F23、T06/10/15、AtomNet、无REAL均有具体处方，非空套话判失败。；运行新增60断言（42生产报告消费/单位/定位及出口，18原生SQLite读取），原51诊断和两原报告37断言保留；均不能冒称120次真实实验或自然质量。

### P4.02 16张研究表、47个面板和消费者

18张研究图缺当前源帧/消费，29旧图设备有效性未知。16表只有8表出行，部分行为阻断占位。 R32.4复核发现READ_OK缺rows/非行列表/超过页上限、settled_at=0及非整数布尔值可造成误判。

具体施工：按 ledger 的源SQL、生产者、消费者及逐项情形恢复生产帧和版本关联。研究页面通过业务只读投影展示同目标父子收益、命中区间、校准、机会频率、漂移和失败原因。查明每个表是否纯展示或被算法读取；真实消费者在P2既有调用点留收据。 页审计对缺失/非法/截断输入分别返回明确未通过原因，仅检查真实已审范围；保留原数值消费者与冻结时间规则。

验收：每个ID单独用已知行/空/错版本/分页/取消测试；8张有行表逐表回读，T06/T10空点不给成功。15张可行动研究图验证同源消费ID、输入变化与消融；R11/R14/R18三张FORMAL_READ_ONLY图只核对原账、角色和引用一致，并验证禁止正式证据反馈训练，不要求优化消费者。

### P4.03 快照、完整导出、恢复与附件

本轮两份导出成功；历史曾在BACKUP读缺失checkpoint而失败。主JSON和MD展开体积很大。

具体施工：在当前报告链保留全部原始facts、水位、44指标值/状态/分母及非索引源。根部只保留一份完整对象索引，章节/图表的重复证据改为精确path+SHA引用；ReportSemanticVerifierR31先校验原结构，再逐项校验引用、缺失hash和篡改负例。修订产物与严格RAW恢复包分别标识，556个缺失原件不冒充恢复。现有原子导出/ACK/附件合同继续按其原生产代码和单独验收执行。

验收：准备中/发布后ACK前进程死亡、原件消失、文件满、取消、重复点击、URI回读失败。成品解压、逐件hash与根引用闭包匹配，ZIP和原机事件可追。

本批全部唯一owner文件如下。ADD/REPLACE是实际写入；PRESERVE_REFERENCE只保全原字节，不能计为新增实现。

| 动作 | 完整路径 | 关联问题 |
|---|---|---|
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ChartRegistryR31.java` | P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java` | P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR83.java` | P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/CompleteReportFileR927.java` | P4.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/DiagnosisActivity.java` | P4.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/DiagnosticStateRendererR906.java` | P4.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/MainActivity.java` | P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ReportArchiveVerifierCP12.java` | P4.03 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java` | P4.01, P4.03, P5.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java` | P4.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ReportJsonWriterR31.java` | P4.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java` | P4.03 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/ReportSemanticVerifierR31.java` | P4.01, P4.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/ResearchTablesActivityR31.java` | P4.02 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/RuntimeChainDiagnosisR975.java` | P4.01 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CapabilityRuntimeEvidenceR33.java` | P4.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ChartCacheR27.java` | P4.02 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckMetricCrosswalkR31.java` | P4.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisRulesR30.java` | P4.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisStoreSU2.java` | P4.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryStoreR30.java` | P4.02 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricDefinitionsR29.java` | P4.01 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricEvaluatorR29.java` | P4.01, P4.03 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/strategyv2/Q08EvidenceReaderR325.java` | P4.01 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/strategyv2/Q08ReportProjectionR325.java` | P4.01 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/strategyv2/RepairDiagnosisR32.java` | P4.01, P4.02 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportAssessmentR33.java` | P4.01 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportChartsR30.java` | P4.02, P4.03 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportEvidenceProjectionR33.java` | P4.03 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportObjectIndexR30.java` | P4.01, P4.03 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR27.java` | P4.01 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportR29.java` | P4.01, P4.02, P4.03 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportSnapshotR30.java` | P4.01 |
| ADD | `app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchTableAuditR32.java` | P4.01, P4.02 |
| REPLACE | `app/src/test/java/com/ethk3/novaonepro/strategyv2/RepairReportFactsR30Test.java` | P4.01, P4.02, P4.03 |
| ADD | `tools/r324/diagnostic/README.md` | P4.01 |
| ADD | `tools/r324/diagnostic/run_diagnostic.py` | P4.01 |
| ADD | `tools/r324/diagnostic/stubs/CertificateGateR27.java` | P4.01 |
| ADD | `tools/r324/diagnostic/stubs/DiagnosisRulesR30.java` | P4.01 |
| ADD | `tools/r324/diagnostic/stubs/IssueTimelineR30.java` | P4.01 |
| ADD | `tools/r324/diagnostic/stubs/MetricCardMapR29.java` | P4.01 |
| ADD | `tools/r324/diagnostic/stubs/ReportEvidenceProjectionR33.java` | P4.01 |
| ADD | `tools/r324/diagnostic/stubs/ReportR27.java` | P4.01 |
| ADD | `tools/r324/diagnostic/stubs/ReportR29.java` | P4.01 |
| ADD | `tools/r324/diagnostic/tests/AssessmentBoundaryProbe.java` | P4.01 |
| ADD | `tools/r324/diagnostic/tests/PageAuditBoundaryProbe.java` | P4.01 |
| ADD | `tools/r325/diagnostic/Q08OriginalReadProbe.java` | P4.01 |
| ADD | `tools/r325/diagnostic/Q08ReportBoundaryProbe.java` | P4.01 |
| ADD | `tools/r325/diagnostic/host/android/database/Cursor.java` | P4.01 |
| ADD | `tools/r325/diagnostic/host/android/database/sqlite/SQLiteDatabase.java` | P4.01 |
| ADD | `tools/r325/diagnostic/run_q08_diagnostic.py` | P4.01 |
| ADD | `tools/r33/report_audit/ReportReRenderR33.java` | P4.01, P4.02, P4.03 |
| ADD | `tools/r33/report_audit/ReportReauditR33HostTest.java` | P4.01, P4.02, P4.03 |
| ADD | `tools/r33/report_audit/run_report_reaudit.sh` | P4.01, P4.02, P4.03 |

## P5：问题与改法

### P5.01 集成故障、升级和签名交付

032对应源工程已有653单测通过；并没有由此得到24/72小时合格、真机质量或闭环运行证据。；设备分母遗漏未冻结机会、全窗被2000条截断且DB/WAL副本未证明稳定，可能虚假PASS。

具体施工：P5同时交付versionCode577311033/versionName R32.5-REPAIR-2026.11.15-033及原R926生成器产生的三份一致文件；除两版本字段外manifest语义不变。执行当前原生成器、Gradle、APK签名/包名核对以及032覆盖实测；签名私钥不入包。工程回执不替代设备保留原数据或自然质量证明。若施工时实际已安装版本高于033，停止该发布步骤并重新审阅更高版本，不能自动重封训练身份。；同批完整替换采集/判定/30项回归：预登记日历、全窗分页、原策略期号、稳定DB/WAL、原件冲突与真实COMMIT后时序；设备证据不作前批工程等待条件。

验收：验证当前SHA的Gradle、关键host/Android路径、apksigner、安装升级及ZIP读回；24h/72h与自然样本独立记录。设备原件齐备后运行 python3 "$PACKAGE/reference/R32.1/verify/check_upgrade_rows.py" "$BEFORE_EXPORT" "$AFTER_EXPORT" --overlay "$OVERLAY_RECEIPT"。它只抽样检查32个开奖及32个已结算冻结，不能代签全部模型/证书/CAS原件；其同目录verify_overlay_032.py已保留。必须本轮重新执行，不能借历史PASS。；python3 tools/r31/test_device_receipts.py为P5本批工程门；24h/72h及自然资格逐原件另签，缺证为BLOCKED/UNKNOWN。

本批全部唯一owner文件如下。ADD/REPLACE是实际写入；PRESERVE_REFERENCE只保全原字节，不能计为新增实现。

| 动作 | 完整路径 | 关联问题 |
|---|---|---|
| PRESERVE_REFERENCE | `.github/workflows/r31-current-live.yml` | P5.01 |
| PRESERVE_REFERENCE | `.github/workflows/r31-publish-release.yml` | P5.01 |
| PRESERVE_REFERENCE | `.github/workflows/r31-report-publish.yml` | P5.01 |
| PRESERVE_REFERENCE | `.github/workflows/r31-verification.yml` | P5.01 |
| REPLACE | `CURRENT_FINAL_PLAN.md` | P5.01 |
| REPLACE | `STARCORE_最终施工入口.md` | P5.01 |
| PRESERVE_REFERENCE | `app/build.gradle` | P5.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/BuildIdentity.java` | P5.01 |
| PRESERVE_REFERENCE | `app/src/main/java/com/ethk3/novaonepro/BuildProvenanceR815.java` | P5.01 |
| REPLACE | `app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java` | P5.01 |
| PRESERVE_REFERENCE | `release/R31_REPORT_PUBLISH_REQUEST.json` | P5.01 |
| REPLACE | `release/r982_source_calls.manifest.json` | P5.01 |
| REPLACE | `release/r982_source_calls.tsv.gz` | P5.01 |
| REPLACE | `release/release_identity.properties` | P5.01 |
| REPLACE | `release/release_manifest.json` | P5.01 |
| REPLACE | `tools/r31/accept_device_receipts.py` | P5.01 |
| REPLACE | `tools/r31/collect_device_receipts.py` | P5.01 |
| REPLACE | `tools/r31/package_release.py` | P5.01 |
| PRESERVE_REFERENCE | `tools/r31/publish_report_package.py` | P5.01 |
| PRESERVE_REFERENCE | `tools/r31/run_current_acceptance.py` | P5.01 |
| PRESERVE_REFERENCE | `tools/r31/run_emulator_upgrade_matrix.py` | P5.01 |
| PRESERVE_REFERENCE | `tools/r31/run_final_runtime.sh` | P5.01 |
| PRESERVE_REFERENCE | `tools/r31/run_phone_report_android.sh` | P5.01 |
| REPLACE | `tools/r31/test_device_receipts.py` | P5.01 |
| PRESERVE_REFERENCE | `tools/r31/test_package_release.py` | P5.01 |
| PRESERVE_REFERENCE | `tools/r31/upgrade_matrix.py` | P5.01 |
| REPLACE | `verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java` | P5.01 |

每张issues/Px.xx文件卡包含完整后态、前后SHA、方法定位与具体改法。完整矩阵为contracts/FILE_CHANGE_MATRIX.csv。原受控路径按唯一owner写入，跨问题引用不重复施工。
