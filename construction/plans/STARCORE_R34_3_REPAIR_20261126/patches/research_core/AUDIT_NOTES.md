# B05 核心修补：施工与证据合同

本组严格以已安装源码 `4d1de9d83692635b5c5ce39144fece1c8a31c8ac` 为前态。完整7文件在 `after/app/...`，精确差异为 `research_core.patch`，逐文件前后SHA见 `research_core_manifest.json`。后续 `research_rolling.patch` 是在本组后态上继续修改，不能从173直接覆盖同名文件。

## 1. Typed writer 拒绝不能变成候选质量失败

生产报告中四条相同错误为 `WriteRejectedR31`，`category=DEPENDENCY`、`phase=ADMISSION`、`commitOutcome=ROLLED_BACK`，底层原因是writer故障原件backlog待排空。它们不是数学输入错误。

`TrainingInfrastructurePolicyR343.reason` 检查最多32层完整无环cause链。明确遇到Error、SQLite corruption/constraint、ClassifiedFailure的INTEGRITY/DB_CORRUPT/BUG时不进入基础设施重试；不靠字符串中出现“DEPENDENCY”判定。只对可信typed接口及已知outcome分类。ROLLED_BACK/NOT_SUBMITTED的DEPENDENCY、LOCK_BUSY、TEMP_TIMEOUT、CANCELLED、STALE_OWNER、RESOURCE_MEMORY、RESOURCE_QUEUE、MISSING_SOURCE、DISK_FULL保留原cursor等待；UNKNOWN/COMMITTED只进入协调器确认等待，不在分类器重放数值提交。

`ResearchTrainingHookRegistryR952.infrastructureWaitReasonCP12` 保留原Android精确异常分型并先调用上述完整typed规则。wait/yield使用原队列、原lease/fence和writer；等待不会产生模型、资格或质量证据。完整性/未知BUG路径仍保留原隔离守门，不能宣称本次为所有任意RuntimeException都建立可恢复证明。

## 2. 四条旧误终态精确复训

`TrainingRecoveryR343.KNOWN` 是报告四个实际 task/candidate/protocol/input/action 的完整组合，`REASON`是完整错误原文。不得放宽成“所有QUARANTINED”或“所有含DEPENDENCY”。

恢复依次验证：原task FAILED_TERMINAL、原candidate QUARANTINED、原action REJECTED且错误完全一致、future_n=0、may_create_real=0、无artifact/数值cursor/其它TRAIN或FREEZE；重解析AST和注册atom，核协议/manifest/hash，确认这四条是无外部teacher/binding/summary的固定图；以现有CAS共享锁读取原文件，核尺寸、SHA、SCDEV解码、训练/验证split和cutoff。

同一writer事务重新CAS全部原字段、检查队列容量及新target，创建新TRAIN任务，写不可变恢复receipt，然后恢复candidate NURSERY和原action WAIT_FUTURE。原FAILED_TERMINAL task及其cursor、原feature、原task专属错误finding保留。原实验finding恢复EVALUATING并由恢复receipt记录旧拒绝。新任务从feature0开始重新执行，不能拿旧cursor与新input拼接。

队列的terminal predecessor豁免只接受这条不可变receipt精确连接的old→successor关系；另一条质量/完整性失败仍阻挡。任何一步失败整笔回滚。原CAS不存在、哈希错误或合同不满足时写HOLD遥测并至少60秒退避，不伪造旧输入，也不阻塞其它可恢复任务。

迁移由 `ResearchMigrationRegistryR952` 的 `R343_TRAIN_INFRA_RECOVERY_V1` 完成。原TRAIN槽每个quantum最多尝试一次恢复。`DatabaseCoordinator` 的 `R343_TRAIN_INFRA_RECOVERY` 注册由配套runtime累积补丁负责，类别为实际枚举 `RESEARCH_DATA`。

## 3. 分族已验证种子可以先开工

`ModernSeedFactoryCP12.verifiedMaterializedCountR343` 仅当整个原始目录SHA验证完成、状态为MATERIALIZING或COMPLETE、material.row=baseRow>0、当前compiler revision的实际VALIDATED条数恰等于baseRow才返回可用前缀。不把1203行伪装成4200全目录COMPLETE。

`ModernFamilyFactoryCP12.prepare` 的5个learned族改用这项独立就绪证明。选取当前variant对应position=`variant%10+1`，同位置按seed_hash排序，offset=`variant/10`。没有对应位置已验证seed时WAIT_FEATURE_SEED。已持久化PROPOSED AST/sourceHash继续使用原身份，不随目录新物化重新选择旧提案。

7个固定图和3个teacher族继续使用它们自身输入资格门；教师不存在不伪造teacher。现有最多40基础变体仍是有限基础工厂，不表示永久创造新数学机制。后续结构变异、规则归纳、数值版本与质量须按各自真实回执分别计数。

## 4. 已执行与必须继续执行的验证

已执行：typed分类纯Java25断言；原queue predicate和恢复DDL在真实SQLite上的11断言；7文件Java parse；在173受影响文件副本上git apply --check、实际apply及7个后态SHA核对。以上都只签各自范围PASS。

没有执行：Android完整编译、原用户SQLite/CAS恢复、故障注入、设备迁移、四个真实继任任务完成、后续冻结与自然质量。施工方必须以原件复核，逐任务交付成功或明确HOLD原因，不能把本组源码与host合同测试当作手机已修复。
