# R34 最终方案运行域交付 QA

审查目录：`repo/construction/plans/STARCORE_R34_20261123`。
审查基线：095 `273641e838e0b8dd842b42a67a674e184942a30a`。
范围：B00–B05、B10、B12运行域路径/文件卡、批次依赖、CODEX入口、B01 schema补件归属。未修改方案根文件，未扩展实现。此报告记录审查时的生成状态，生成器后续修改后需重核所列项。

## 一、已经核对通过

1. 运行域工单合计涉及 **75个唯一生产路径**；逐一核对 `FILE_CARDS.json` 卡片存在、EXISTING_095路径实际存在、SHA256与095相符、`source_before`原件与同摘要相符、issue反向关联存在，**未发现失败**。生产路径均限定实际 `app/src/main`，没有误选host shim。
2. 上轮指出的大部分owner遗漏已修正：`WriterHoldFaultsR31`在B01-01；`SchemaDomainCoordinatorR926`、`ResearchRetentionStoreR30`、`ResearchRetentionR30`在B01-03；canonical/analysis三owner已进入B03；`SettlementTaskStoreR36`在B04-04/05，`RuntimeSchemaR26`、`EssentialSettlementFeedbackR923`在B04-05。
3. 不存在的 `TaskContinuationR31` 已改成实际 `TaskContinuationR26.step/compute`；该文件本身在B03-05存在。另有B03-02关联遗漏，见下。
4. B01-01已明确三类是逻辑意图，映射既有九类WriteClass，不冒称已有三个新enum。
5. B01-06顺序已修为最小critical schema/故障spool先bootstrap，再关键抓取和故障耐久化，之后研究schema单元，不会要求业务在尚无最小表时先落库。
6. B04-05明确draw label不依赖冻结/BET，且将本批事件投递与B07数值消费分开，避免B04被未来训练实现反向卡住。
7. B00→B12工程依赖保持有向顺序；B03机械主链、B04影子/标签、B09工程资格夹具与自然观察分开，没有要求B01靠自然高胜率签收。
8. `payload/runtime/supplement` 与审计方最终补件逐文件相同，包括最新ticket/payload同cursor-fence校验；没有复制到旧版本。

## 二、封包前必须修正

### P0-01：APK验收包名混淆

`tools/r34/check_acceptance.py:55` 当前硬编码 `com.ethk3.novaonepro`，这是源码Java包命名。实际095 `release/release_identity.properties:1` 为：

```properties
applicationId=com.starcore.research.r13
```

`app/build.gradle:82` 用 `requiredIdentity('applicationId')` 生成APK身份。现有checker会拒绝真正同应用升级的APK，或诱导生成另一包名应用。必须使用封存release identity的实际applicationId，当前值为 `com.starcore.research.r13`；不得从Java package推导。签名兼容、递增versionCode等原检查保留。加入“正确applicationId能进入后续验收、Java package名APK被拒绝”的身份负例即可，不需要扩张新的测试体系。

### P0-02：B06冷启后像被错误标为B01/B03当前可应用代码

|文件卡|当前owner/材料|真实材料内容|需要的映射|
|---|---|---|---|
|F114 `RuntimeCoordinatorR26.java`|owner只有B01；`APPLICABLE_PATCH_WITH_FULL_POSTIMAGE` 指向learning后像|后像405行已经注册 `ColdRebirthR34.TASK`，属于B06集成|B01方法合同与B06集成材料分开；owner/issue加B06-01；后像注明仅B06上下文合并。|
|F125 `TaskContinuationR26.java`|owner只有B03；相同类型指向learning后像|后像87行调用 `ColdRebirthR34.step`，属于B06集成|B03公平调度合同与B06dispatch材料分开；owner/issue加B06-01。|

`batches/B01/issues/R34-B01-03.md`、B01-06等目前把B06 RuntimeCoordinator后像列为随卡可应用代码。照CODEX“每批应用本批代码”的指令执行，会提前引用B06新类，且该后像没有实现B01 schema切片。**文件通用的“累计合并”提醒不能替代材料所属批次。** 建议代码材料显式增加 `applicable_batches` 或按issue材料分项：B01/B03为方法合同，B06才是冷启小patch/审阅后像。

此外B06_FILE_MANIFEST有7个生产对象，但WORK_ORDERS的B06-01目前paths只列WarmCandidate/ExpertContinuation/ColdRebirthR34。部分额外对象已在FILE_CARDS关联B06-01，却没有进入该issue路径。应把3个ColdRebirth新类、LifecycleSchema、RuntimeCoordinator、TaskContinuation、ModelReplay这组实际集成文件在机器工单与文件卡中双向对齐。不要只修可视Markdown而留下机器索引不一致。

### P1-03：schema supplement 已有字节，但尚未进入工单和文件卡

`FILE_CARDS.json` 当前没有 `SchemaUnitPlanR34.java`；B01-03和B01-05随卡代码也没有schema补件。目录存在不等于施工入口可发现。下一节给精确纳入建议。

### P1-04：B03-02方法已改正，但paths未包含方法owner

B03-02写 `TaskContinuationR26.step/compute`，paths仍只有AutoFetchService、RuntimeR27、ResearchPool。把 `strategyv2/TaskContinuationR26.java` 加入该issue，并把F125反链加入B03-02；不用增加工单数量。

## 三、B01-03 / B01-05 纳入 schema supplement 的明确方式

实际工单ID为 `R34-B01-03`、`R34-B01-05`（不是三位003/005）。

### B01-03：单元迁移、定义版本与保护等价性owner

新增生产路径：

`app/src/main/java/com/ethk3/novaonepro/strategyv2/SchemaUnitPlanR34.java`

文件卡设 `before_state=NEW_PLANNED_PATH`、`before_sha256=null`，基线仍为095；代码材料指向：

`payload/runtime/supplement/after_full/app/src/main/java/com/ethk3/novaonepro/strategyv2/SchemaUnitPlanR34.java`

材料必须附状态 **PURE_CORE_NOT_ANDROID_INTEGRATED / REQUIRES_EXISTING_OWNER_INTEGRATION**，不能因它是完整Java源码就签B01实现。绑定 `payload/runtime/supplement/MANIFEST.json` 的实际SHA，随卡链接 `INTEGRATION.md`、`B01_schema_core_ADD.patch`、SQL账本及VERIFICATION。SQL文件是旧schema owner需要吸收的候选定义，不是另开SQLite写连接的执行脚本。

保留B01-03已有7个旧owner文件；额外标明旧owner必须接的方法：

- `RuntimeCoordinatorR26.startupUpgradeSpecR31/initialize`：恢复durable cursor，schema/会话/checkpoint阶段分开。
- `SchemaDomainCoordinatorR926.runUpgradeR31/migrateR31RetentionProtection`：原版本journal下面接单表账本和原件保护证明。
- `ResearchRetentionStoreR30.ensureProtectedObjectsR31`：提取一个受保护source的安装/验证，保留完整专用guard组合。
- `SchemaUnitPlanR34.Plan/prepare/beforeWrite/receiptPayload/restartVerification/certificateCurrent`：58表INSTALL+VERIFY、目标版本、预算、fence和证书失效协议。

新增明确验收点：58表注册表逐项一致；任何期望定义/DB身份变化不能复用旧cursor；相同schema cookie下逐表VERIFY完成才有retention DONE；新schema/restore使证书失效；全局原件指纹必须有等价保全实现；会话登记、checkpoint启动原件保护不能随schema缓存跳过。当前核心的 `originalContentRoot` 是输入合同，**没有实现095全量指纹的低成本替代**，必须留OPEN待旧owner接线，禁止填固定值。

### B01-05：同事务receipt、提交不明与ACK owner

把SchemaUnitPlanR34作为共享文件关联B01-05，链接相同补件，不复制第二份核心。方法锚点补：

`beforeWrite → receiptPayload → 原writer同事务DDL+receipt+cursor CAS → 独立ReadBack → resolve`

要求：

- 原件/DDL、receipt、cursor CAS同事务；CAS检查plan/run/revision/phase/index/owner/fence/auditRound。
- COMMITTED不是仅凭返回值；独立读回匹配receipt和cursor才ACK。
- UNKNOWN且缺receipt：WAIT_DURABLE_RECEIPT，不能盲重放；接管owner确认原事务结束后按原commitKey恢复。
- 同key异payload拒绝；旧owner不能用新owner的receipt推进自己的cursor。
- 提交前退出不推进；提交后ACK前退出凭同receipt恢复一次。

**这不是替换canonical/deferred回执。** B01-05原 `DatabaseCoordinator`、`PreparedCanonicalStoreR335`、`DeferredEvidenceWriterR36` 合同继续保留；schema与业务对象共享提交判定原则，各自保存原身份和原表owner。

验证回执准确列：21个Java协议断言、58表覆盖、116fixture单元、6真实SQLite故障场景；编译是Java21 compiler/source-target17；Android、APK、真机均NOT_RUN。不得把这些数字算成生产58表迁移已经在真机通过。

## 四、CODEX入口与封包检查

### 已正确

- 独立检出实际095提交，避免main旧app；保留完整Git祖先和原PIN。
- 计划与应用检出分开；后批不能整文件覆盖前批累计修改。
- 每批APK/host/Android/设备/自然状态分开；代码材料不等于整批完成。
- B06–B08同步自身生产者/消费者契约，不等待B11才第一次接线。

### 当前未闭合

1. 审查时 `CODEX_EXECUTE.md:7` 要求先运行 `tools/r34/verify_plan.py`，文件尚不存在；README链接的 `CODE_PAYLOAD.md` 也尚不存在。可能属于正在生成的文件，但封包时必须实际存在、链接正确、`--help`/执行入口可用。计划放工程外后，指令应指明从计划目录执行或使用绝对脚本路径，避免在095工程误找尚未添加的工具。
2. supplement在复制后的计划目录中，原INTEGRATION示例仍是开发workspace路径 `analyses/runtime_payload/supplement/...`。CODE_PAYLOAD/CODEX应给真正可执行的计划路径，例如从计划目录运行：

   ```bash
   python payload/runtime/supplement/tests/verify_schema_unit_plan.py --baseline /absolute/path/to/actual095
   ```

   这验证纯核和SQLite合同，不执行Android接线。
3. `CODEX_EXECUTE.md:3` 写“用户已经授权按方案修复、编译并交付”。本次用户明确请求的是深入分析、完整施工方案及原报告上传。任务书可以作为下一次实际施工输入，但不宜替当前会话扩写授权事实。建议改成“当用户将本任务书交给Codex执行时，按以下范围完成真实Android施工”，并保留原自主持续执行要求；不增加确认流程。
4. `PIPELINE_CONTRACT` 的文字已正确描述draw label独立产生，但图中仍只有结算F→反馈G的路径。可补B（开奖与标签）→G（反馈与训练输入），避免视觉上把训练标签重新画成必须经过冻结/结算。

## 五、交付结论

**源路径/前态/旧owner修正基本通过；当前不能签“最终交付索引无误”。** 需先修包名、B06材料跨批错配、schema补件的工单/文件卡接线、B03-02反链，并完成入口文件生成。以上为有限方案QA，没有新增应用实现，也没有重跑可替代Android的虚假验收。

主整合owner已确认将修复：包名改从实际release identity绑定并加入正/反例；材料增加 `applies_in_batch` 与每批合同；B06实际修改文件自动登记B06-01；schema core关联B01-03/05；`verify_plan.py`、`CODE_PAYLOAD.md`继续生成。这些项在本审查收尾时记为 **FIX_ACCEPTED_PENDING_REGENERATION**，不把收到修复承诺写成修复后已复测。由主整合owner完成重生成和封包核验闭环。

## 本次审查快照

2026-11-23T06:28:43.379690+00:00

```json
{
  "WORK_ORDERS.json": "7b12440e93488fdc63499325548c44fb9ab299db371830554a6c02d30732e36b",
  "FILE_CARDS.json": "ceefe18c677ad34c517ad839b4c5ca774aa7b7496c83d4052d9ea979ce223ebc",
  "CODEX_EXECUTE.md": "01f85509d1714de3592a9faa2fd2fc5133073012e6fccbbde515834974e6f894",
  "tools/r34/check_acceptance.py": "bdcef2b90af431dab708f4e47201aa56fe5e1d8c9648e526218cfc4c11ee6c73"
}
```
