# 策略主链四处施工合同（安装版 211 精确底座）

基线：`5c7f1b36eaee6036df280e85835bb0fb4ce0b40d`，`versionCode=577311211`。所有 Java 路径以 `app/src/main/java/com/ethk3/novaonepro/` 为根。本文件为施工合同；候选 diff 未写入应用工作树，未构建 APK，未跑设备。

## C1：writer backlog 分类变更必须继续保留 prepared batch

**确定性：源码可证；是否为每条现场任务的本次失败原因仍需同 owner/fence 的故障回执。**

- 生产者：`DatabaseCoordinator.java:1023–1026`，`runWrite` 对 writer-hold backlog 抛出 `RESOURCE_QUEUE / ADMISSION / ROLLED_BACK`，直接 cause 文本为 `R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN`。
- 消费者：`strategyv2/ResearchWriteQueueR27.java:241–253`，`writerBusy` 只识别旧 `DEPENDENCY` 分类的这个 cause；当前 `RESOURCE_QUEUE` 落到返回 false。
- 后果：`submitAttemptR31:203` 抛出而非返回 false；prepared continuation 可以被标为失败，下一轮丢弃 draft 后重新准备。不能把这是“候选算法坏了”写进质量判定。

**实际候选**：`strategy_candidates/ST02_queue_category.patch`。只扩大这一精确已知 admission 条件的分类兼容，不接纳其它 RESOURCE_QUEUE；`COMMITTED`、`COMMITTED_DIAGNOSTIC_FAILED`、`UNKNOWN` 不自动重试。取消、旧 owner、哈希错误、磁盘满和任意含类似文本的普通异常保持原路径。

**必须守住的合同**：第一次 admission 回滚后，`ImmutableBatch` 对象、`payloadHash`、`commitKey`、`identityR31`、before/after guards、task cursor 不变；下一轮只重试该 batch，绝不重新采样 RNG、重新选择父代、重算选择集或递增进度。未知提交先查 durable receipt，不能套用本补丁。

**测试**：候选包含旧/新两种分类 × 全部 CommitOutcome；BODY/END/READBACK/SCHEMA_PENDING；无 cause、异 cause、后缀漂移；INTEGRITY/STALE_OWNER/CANCELLED/DISK_FULL/BUG；原 prepared 第一次回滚不推进 cursor、第二次准入保持身份。反射测试仅覆盖现有私有准入方法，不宣称真实数据库 exactly-once。还须运行现有 native SQLite 事务/receipt suite、取消和进程死亡测试。

## C2：让数据集的小型有界事务真正到达已有公平写入槽

**确定性：可构造永久饥饿的源码路径；现场 broad pressure=true 与 prepared waiting 相符，但缺少本次队列准入原因回执，不能断言唯一现场根因。**

锚点：`ResearchWriteQueueR27.submit(ImmutableBatch):153–173`、`submitAttemptR31:185–203`；`DatabaseCoordinator.researchDataDueR335:191–194`、`replayDatasetDueR341:195–203`、`runWrite:1009–1045`；`WriteBudgetR341.DatasetService:118–190`；`RuntimeR27.DatasetTask:236–340`。

当前序列为：队列先看 `critical` → 只有 `HISTORY_IMPORT_R31` 有过龄公平例外 → 进入 runnable 后才登记 `REPLAY_DATASET` prepared → writer 最后才检查已有 5 秒公平槽。若 broad critical 持续为真，普通 replay stage/recovery/final publication 永远到不了已有公平裁决。主线程心跳和 claim 仍可运行，这不等于算法有服务。

**实施接口建议，不直接放行补丁**：

```java
// 示意合同，尚未实现；必须由现有 DatasetTask/写入协调器持有。
final class ReplayAdmissionTicket {
    String taskId, ownerEpoch, inputSnapshot, payloadHash;
    long fence, preparedAtMonotonic;
    String exactOperation;  // 只允许 DatasetTask 的预登记有界阶段
}
```

1. prepared payload 完全冻结后，在进入队列等待前，由原 owner 宣布真实准备就绪；不能只凭 operation 字符串伪装成 dataset。票据绑定 task、fence、input、payload；取消和实际 worker 退出必须释放。
2. 对 `REPLAY_DATASET_STAGE_PAGE`、`REPLAY_KNOWLEDGE_RECOVERY`、最终 `REPLAY_DATASET` 等明确的既有阶段，统一经 coordinator 的服务资格计算；同一个 oldest-ready 年龄不能因为每期新 head 而不断重置。具体白名单应以调用图审查后的单一 enum 登记，reset/retire 需要各自明确身份。
3. active settlement、即将过期的 freeze、有 freeze waiter 时继续优先；仅 broad pressure/backlog 且无正在执行的关键事务、真实准备完成且过龄时，允许尝试原有单 writer 公平槽。协调器锁内重检、deadline/workload cap、回滚结果、fence 都保留。
4. 一个小事务完成后释放服务槽，让结算/freeze 插入；不能独占整个解压、历史读取、特征准备、全世代训练。解压和模型计算仍在 writer 外。
5. 登记 `admissionDeniedReason`、prepared年龄、服务槽请求/授予/提交、operation/input/payload/owner/fence。最少的控制回执应复用现有遥测 owner，不能在拒绝路径同步递归写更多故障。

**验收**：在冻结窗口外制造 broad pressure 持续 30 秒但 settlement 不活跃，准备好的真实 dataset 小事务必须获得有界服务；随后插入 freeze waiter，freeze 优先且研究停止推进；取消/换 owner/源修改不能获得提交；重复片段同 identity 只产生一次 artifact；若真实 freeze/结算连续占满，应输出 `SATURATED_CURRENT_FREEZE` 或相应压力状态，不能伪报健康，也不能以“保证30秒成功”越过真实资源限制。

## C3：固定旧 pin 恢复必须分清原件、源有效性和派生投影

锚点：`ResearchInputPinR30R1.loadCurrent:41–47`、`invalid:34–40`、`load/decode:93–101`；`RuntimeR27.DatasetTask:245–263`、`publishRecoveredReplayKnowledgeR31:344–369`、`EvolutionTask:418–455`；`ReplayKnowledgeStoreR31.appendProjectionR31/isPublishedR31:20–35`。

**确定的诊断缺口**：`loadCurrent` 的 null 同时覆盖 malformed hash、已失效、找不到 pin、require 时源变更；报告只有 expectedPin，无法据此判定是哪一种。应新增伴随的只读结构化结果（兼容原 `loadCurrent` 调用）：

```java
// 示意合同，不能用 UNKNOWN 代替一个成功的 pin。
enum PinLoadStatus {
    READY, INVALID_HASH, NOT_FOUND, INVALIDATED_SOURCE,
    ORIGINAL_MISSING_OR_INVALID, SOURCE_CHANGED_DURING_VERIFY, SCHEMA_PENDING
}
final class PinLoadOutcome {
    PinLoadStatus status;
    ResearchInputPinR30R1.Pin pin; // 仅 READY 非空
    String requestedHash, runId, originalContractHash, reasonCode;
    long sourceRevision;
}
```

1. 原 pin 解压、raw hash、dataset hash、canonical issue、原行数、H_TRAIN/H_SELECT/H_CONFIRM/OPEN边界必须全部重验。旧 pin 可大于现在5000条热尾；热尾删除与原始样本被修改必须区分。不得把期号缺口填造为训练样本，不得将原 holdout 转为开发训练。
2. 原 pin 已有效但派生 replay-knowledge 投影缺失时，保留恢复 metadata 的有界 writer 事务，之后再发布 evoCache；C2 解决其服务问题。
3. **可选窄优化**：当 `appendProjectionR31` 生成的精确 payload、mode、sourceCommit/worktree、pin.recordedAt 已与数据库完全一致，可在再次 `requireFence + pin.require + isPublished` 成功后跳过纯重复写入。现有代码即使投影已一致仍总是 submit。新源码/新 mode 的 payload 不一致时不得走快路；投影命中不等于源有效。此优化不是现有报告“投影UNAVAILABLE”的完整修复。
4. generation进行中恢复原输入、原 RNG、原父代、原 exposure ledger。只有现有 `restartInvalidDevelopmentInput`/generation boundary允许切换输入，且保留审计。严禁清空 `r27_evolution_cursor`、ledger或原pin让“进度重新动起来”。

**验收**：原pin有效/已热尾删除但原件完好/原件缺失/单字节篡改/源更新/源修正/owner改变/metadata缺失/metadata旧sourceCommit/提交后进程死亡分别执行。恢复后的 cursor、RNG、ledger与安装版原值逐字段比对；不是仅检查 task=RUNNING。

## C4：恢复第5个已证误终止的现代训练任务，并完成现有TRAIN闭环

锚点：`r952/TrainingRecoveryR343.KNOWN:21–26`、`known:39–44`、`read:46–52`、`drainOne:54–76`、`verifyAndResume:116–154`、`commit:156–180`。

报告全量任务显示5条旧失败：FREQUENCY、MARKOV、TRANSITION、STATISTICAL、RULE。211原白名单只有前4条；FREQUENCY已经恢复，产生1个不可变恢复receipt及1个attempt_count=0的PENDING successor，另外4个候选仍QUARANTINED。**不能重复宣称4条全未修、也不能把已恢复状态当作训练完成。**

第5个 RULE 的固定身份如下：

| 字段 | 原值 |
|---|---|
| task | `678d059b7207c6f2ec712c0cdd906dbc201357a61632236b8019af0c67ce740f` |
| candidate | `CP12-FAMILY-ebfcc38e32ef9155cf4cbae7` |
| protocol | `ab1473934a4326bf05eeef4ed8e5dd015a0b4dddff7aeed7791e52e6dc5594da` |
| input | `129c749b58ccfe9884070607d83b7ec51e2f742481495c0d24d2050c7b012f01` |
| action | `1b75dcb6745bbff2bbc0d669159523e34fae306e1f2ce6b1304cfa8c9eb1370d` |

**实际候选**：`strategy_candidates/ST06_fifth_training_recovery.patch`。只增此精确五元组，不开放按错误文本任意恢复。报告 AST3/协议证明该 RULE 是 FIXED_GRAPH，64 nodes、requiredHistory50、无专家/绑定/long-summary、boundFiles空，符合现有恢复类型；但`.scdev`原件不在报告中，必须在运行时照原方法验证，未通过则保持 hold。

当前 `ResearchTrainingHookRegistryR952.infrastructureWaitReasonCP12/retryInfrastructureCP12` 与 `TrainingInfrastructurePolicyR343` 已区分资源等待和真实性错误，应保留。`drainTrainingSlotCP11` 的5子槽、Director的6槽与训练预留已存在，也应保留。修复服务后必须实际观察 successor 的 `FEATURES → NORMALIZER/TRAIN/VALIDATE → development_receipt → frozen evaluation`，不得新建旁路训练器以掩盖旧队列停滞。

本次新增的JUnit仅验证五元组白名单正例/身份与生命周期负例，未运行。后续 native SQLite 验收必须覆盖：重复恢复只建1个successor；旧失败task/cursor不变；原件不存在/byte_count不符/hash错/协议错/已有artifact或future_n>0拒绝；提交后进程死亡可读receipt；运行后新模型、验证tape、artifact等真实产物闭合。训练更新、冻结与授权各自仍走现有owner。

## 候选自检边界

两份diff均已在精确211工作树执行 `git apply --check` 成功，工作树tracked文件保持干净。通过 `java strategy_candidates/WriterBusySourceContract.java` 源码启动，实际运行从源码逐字抽取的两版`writerBusy`方法：30例，原版预期失败1例（当前RESOURCE_QUEUE/ADMISSION/ROLLED_BACK），候选0失败。适配器仅提供测试用typed-category，生产fallback分类器、完整prepared事务未执行；scope为`SOURCE_EXTRACTED_CLASSIFIER_WITH_TYPED_CATEGORY_ADAPTER`。

当前环境未提供javac命令、Android SDK和Gradle编译产物，所以完整JUnit/Android/设备验收均为未执行；这不是可直接发布APK的证书。候选仅使用附加审计目录保存，最终应用施工必须从211或经验证的后继底座进行，禁止把main旧应用整文件覆盖进211。
