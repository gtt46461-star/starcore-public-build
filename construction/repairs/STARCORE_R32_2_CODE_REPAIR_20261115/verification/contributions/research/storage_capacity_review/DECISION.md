# AtomNet 4096终身发布计数阻断：定向源码审查结论

结论：当前源码不存在可直接复用、覆盖AtomNet发布原件的安全归档退役入口。本轮没有删掉容量门，也没有提交未经引用闭包验证的删除代码。这是**尚未修复的P2/P1联合阻断**，不是已完成项。它需要一个独立的同批跨文件生命周期实现，完成后才能声称累计发布不封顶、活跃资源有界。

## 已核对的实际行为

|源码与方法|实际行为|为什么不能直接复用|
|---|---|---|
|`AtomNetPublicationR31.Prepared.stagingBatches`|按整张`r31_atomnet_publication`的`count(*)`、`sum(sample_count)`、`sum(sample_bytes)`准入，4096行后拒绝新manifest；没有区分当前、历史、被消费或已归档。|即使所有新增在线manifest的sample_count=0，仍然累计到4096；不存在终身计数复位或经验证的退役路线。|
|`AtomNetPublicationR31.parentMaterializedR31`|按当前checkpoint的state_hash查发布记录以校验dataset和已物化样本数；缺发布时回读当前checkpoint。|当前父状态及对应样本不能按年龄清理；并发发布还要保留CAS可重试身份。|
|`AtomNetStoreR30.samples`|从legacy sample表和`PUBLISHED`的sample_stage联合取样，支持原训练/SELECT样本。|删旧manifest会让JOIN隐藏样本；删stage也会破坏重放与模型的后续开发验证。|
|`MainlineModelBindingR31.capture/stillCommitted`|按model_hash/weights_hash查原manifest，已冻结绑定存publicationId+stateHash；writer再按原表验证。|只有行复制到别处而不迁移读者，就会让真实合格主链授权失败；不能通过降门绕过。|
|`CheckpointReferencesR26.hashes`|publication的state_ref、weights_ref、dataset_ref均为根；所有r30_model_artifact.weights也是根。未退役manifest承诺原字节。|删除文件而保留原manifest将制造ENOENT；仅移除4096计数则根继续增长。|
|`CheckpointGcStoreR31`|引用保护触发器、pin、图边、CAS状态机制可重用，但roots/artifacts/edges一次集合有16384上限。|它管理文件可达性，不决定哪个业务发布原件已经允许退役；所有旧manifest仍是根，GC不会释放它们。|
|`ResearchRetentionR30.PROTECTED_TABLES/Policy`|没有注册r31_atomnet_publication/sample_stage；未注册表仍默认不可按年龄删除。已注册模型/证书的默认原件策略也不允许FIFO清理。|不能把“未注册”解释为可删；需要明确对象owner的发布、回滚、验证和原件期契约。|
|`KeepMigrationR31/KeepMigrationStoreR31`|迁移对象是r30_keep_*旧归档副本；按页写完整原件CAS，stage到SWITCH/DONE；切换读者有专门合同。|没有AtomNet新发布表的源对象注册、reader adapters或switch记录。|
|`KeepArchiveRetirementR31.retireOne`|有经验证完整CAS原件、精确许可、reader合同才删除SQLite重复行；physical_bytes_released固定0。|是副本回收，不会删除CAS原件，不能解决模型/状态原件持续增长。|
|`CheckpointScratchGcR31`|仅清理弃置.tmp-UUID，明确不处理canonical hash.bin。|不是历史已提交模型原件的回收入口。|
|`ModelReplayStudyR30R1.previous/ModelSurfaceStoreR30.bindHistorical`|直接读取r30_model_artifact，按冻结时间/目标做旧模型对照和历史模型绑定。|归档读者必须保留PIT与原始模型可用时间，不能只留下收益摘要。|

## 不可替代而当前缺失的契约

这些缺项需要同批实现为真实数据与状态转换；不是让用户凭空补一个布尔开关。

1. **发布owner的退役许可。** 必须明确哪些原件仍由checkpoint恢复、部分issue继续训练、开发验证、rollback、开放确认、未来资格、真实冻结/结算、导出snapshot/分享租约所持有。没有owner持久化的“关闭/不再回滚”记录时，不能由GC推断过期。
2. **AtomNet精确引用反向索引及迁移读者。** 当前mainline binding含JSON publicationId，多个模型表直接以modelHash取原件。需要冻结绑定、forecast、model surface/call、model replay study、sample reader、checkpoint恢复、报告附件共用的active+archive读合同；每个消费者逐一迁移和反例验证。
3. **完整原件归档与可回读证明。** 归档必须涵盖manifest全部字段、sample rows、state/model/dataset CAS及关联hash，写临时文件、hash/大小验证、fsync/原子发布后提交逐对象收据。先归档后退役，失败停在原对象，不把摘要当原件；PREPARED/CAS结果不明的对象不得清理。
4. **有界根扫描与业务退役闭包。** 活跃root要分页持久化，不能继续用全历史根集合16384上限。根快照和新引用写入须同版本/fence校验，GC的DELETING阶段必须与新引用互斥。
5. **终身累计与活跃占用分开。** 累计计数只作审计不作为新生/训练门；准入只对active PREPARED、必要热样本、活跃模型内存、真实磁盘字节/保留租约计费。被归档完整原件仍占物理预算，不能说逻辑行少了就释放了字节；如果受保护原件占满物理预算，必须如实输出具体持有者与解除条件。

## 同一个批次必须一起施工的修改面

`AtomNetPublicationR31`、`AtomNetStoreR30`、`MainlineModelBindingR31`、`MainlineFreezeProofR31`、`ModelReplayStudyR30R1`、`ModelSurfaceStoreR30`、`CheckpointReferencesR26`、`CheckpointGcStoreR31`、`ResearchRetentionR30`、`ResearchRetentionStoreR30`、`SchemaDomainCoordinatorR926/迁移owner`、`ReportSnapshotR30`、`ReportAttachmentClosureCP12`以及对应设备导出/恢复测试。不得把退役生产者放前批、archive reader或GC新引用防护留后批才验收。

最小验收必须覆盖：超过4096次真实发布仍继续；当前checkpoint和训练样本不丢；已冻结mainline binding跨归档有效；新旧model replay按时序同结果；归档ACK丢失/进程死亡可恢复；导出租约持有时不回收；陈旧root快照不能删除新引用；文件满时原件仍可读且精确报容量；归档回读与成品导出逐件hash一致。自然胜率与资格门保持原协议。

## 为什么本轮不给“直接替换”的容量修复代码

现有安全入口只能搬运旧keep副本或删除无引用scratch；没有发布原件退役许可、archive reader和分页root owner。以两三个文件的替换代码去掉4096门、把它改成仅PREPARED、或清最老manifest，都会分别导致16384根溢出/物理增长、训练样本JOIN缺失、主链资格不可验证或再次ENOENT。它们都不是用户要求的闭环。未补齐上述合同并执行跨文件反例前，把这部分标成“代码已完成”不真实。
