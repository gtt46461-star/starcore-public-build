# 拟新增类型化生成规则及原研究链接入合同

## 1. 状态、文件与所有权

**本文件是待施工接口设计，不是已实现代码，也不证明现场已经创造新规则。**拟新增文件唯一指定为：

`app/src/main/java/com/ethk3/novaonepro/strategyv2/TypedGenerationRuleR342.java`

它保存不可变规则并执行有界、确定性的类型检查、结构归纳和实例化，不创建线程、服务、数据库连接、调度循环或REAL入口。调度仍归 `RuntimeCoordinatorR26`；规则发现使用 `RuntimeR27.AtomsTask.step → AtomWorldR27.atomsStep` 的已有 `r27-atoms` claim；候选仍由 `RuntimeR27.EvolutionTask.step → EvolutionLoopR27` 出生；写入仍走 `ResearchWriteQueueR27/DatabaseCoordinator`；最终应用仍受 `MAINLINE.md` 和 `CERTIFICATE_DOMAINS.md`约束。

## 2. 拟定类型接口

以下签名及嵌套类型全部为 **PLANNED_NOT_IMPLEMENTED**。施工时以这些语义冻结接口和测试，再写Java实现；不得通过只创建同名空壳签收。

```java
final class TypedGenerationRuleR342 {
    static TypedGenerationRuleR342 decode(byte[] canonicalBytes, Limits limits);
    byte[] encodeCanonical();
    Validation validate(ConsumptionPin pin, Limits limits);
    static InductionResult induct(List<DiscoveryEvidence> originals, Limits limits);
    Instantiation instantiate(ConsumptionPin pin, SlotBindings bindings,
                              long seed, Limits limits);
    String ruleId();
    String shapeHash();
}
```

| 嵌套类型/字段 | 必需语义 |
|---|---|
| `RuleTemplate` | 保留实际共同运算骨架；节点是已有AST算子或有明确类型的slot，不能出现可执行脚本、反射或未知运行时算子 |
| `Slot` | 唯一slot id、`PolicyAstR27.Type`中的PROB/FLAG/RISK/SCORE/CONST、允许结构/源角色/目标约束、深度节点成本和PIT约束；禁止整树通配和任意字符串替换 |
| `DiscoveryEvidence` | 至少两条真实已训练公式的policy/shape/model哈希、原数据pin、训练及配对消融原件哈希、区间、角色、source revision、数值/特征ABI、归纳实现版本；来源不同名字但同一等价树不计两项机制 |
| `ConsumptionPin` | 当前开发dataset hash、区间与角色、源revision、ABI、cutoff、预算、目标位置、当前generation及所捕获规则publication；与发现pin分开保存 |
| `SlotBindings` | 每个slot的原子/子树原件、类型、来源哈希和因果范围；同一slot重复出现时一致绑定；所有输入只来自允许的开发上下文 |
| `Instantiation` | 完整`PolicySpecR27`、原rule id/manifest hash、slot绑定哈希、seed、当前消费pin、生成前后结构hash与解释；不能直接包含出生成功或证书PASS |
| `Limits` | 编码大小、slot数、遍历节点、深度、尝试次数和计算成本的显式上限；生成结果仍满足原BirthGate更严格的出生限制，不能沿解析器较大import预算放宽出生门 |
| `Validation/InductionResult` | 区分可用、无支持、等待依赖、类型/ABI不符、证据无效和预算yield；无候选不是成功创造，依赖未到不是永久BUG |

`ruleId`由格式、规则语义、ABI、canonical模板和slot约束确定；发现原件与发布身份另入不可变manifest hash。`shapeHash`用于去参数的机制等价判断，沿 `FormulaIdentityR341` 归一规则保留交换律规范和来源角色。增加一次发现记录、换名字、窗口参数或常数，不增加“新思路机制”计数。相同语义可以有不同来源manifest，但不得借此绕过去重。

实例化结果先展开为现有 `PolicyAstR27.Node`，再形成原`PolicySpecR27`。运行预测时不存在slot解释器，也不向实时推理器增加新的动态语言。校验必须在分配大数组或递归前扣预算；相同输入、版本、pin、slot绑定和seed应得到相同canonical AST与哈希。

## 3. 结构归纳及数据角色

发现只读已提交的开发训练与开发选择原件。选取至少两个有实际数值训练及支持消融的公式，按已有类型系统做有界树结构归纳：保留共同运算节点，差异子树只能在类型、来源和因果兼容时抽成slot。没有保留实际共同机制、只有全树通配、仅常数/窗口替换，或删去共同骨架后结果不变的规则，返回明确拒绝。

规则发现、规则迁移、候选训练、独立确认和自然未来各有不同原件及分母。规则迁移只能使用未参与该规则发现的开发区间；新计划要求两个未使用的开发区间完成消费，每个产物独立出生、训练和首评。原始发现证据不可改标成新的dataset，也不能读取封存确认或自然未来结果来选slot、调模板或换端点。

开放规则归纳是一种能力扩展，不移除既有21思路族、已有变异/交叉/融合算子或成熟模型。既有静态族最低预算保持；规则尝试使用事前在原 `CreationBudgetR31` 内分配的剩余创造预算。规则无效、预算不足或无输入时保留理由，余量按既有规则处理；禁止另开不限额候选池，禁止把静态家族生成结果改个rule标签冒充规则产物。

## 4. 发布存储必须兼容现有表

152源码的 `AtomPublicationStoreR31`现有表 `kind` 只允许`SCORES/MOLECULES`，并有165个原子、32个分子的完整性CHECK和发布trigger。**不得把RULE直接塞进这些表，也不得为了新类型去掉原CHECK。**

在原类中拟新增 `ruleDdlR342/declaresRuleObjectR342/prepareRuleR342/publishRuleR342/readRulesR342`；规则使用追加式专用表，拟定为 `r342_generation_rule`（不可变规则原件）、`r342_generation_rule_publication`（PREPARING/PUBLISHED清单）、`r342_generation_rule_head`（作用域内原子切换的已发布指针）、`r342_generation_rule_event`（禁用、恢复、失效原因事件）、`r342_generation_rule_use`（实际消费回执）。最终列、索引、trigger和DDL hash在本批文件卡冻结。

这些新增对象只由 `LifecycleSchemaR31.definitions/Definition/advanceWithinMigration` 的原MIGRATION任务安装：显式登记准确名字、定义哈希和预期附件，核对总对象预算；业务读者只调用ready检查，不能执行DDL。旧原子/分子表的schema owner及旧DDL不变；新规则表归单一Lifecycle owner，不能同时让旧atom schema安装器再次创建它们。

`prepareRuleR342`在写锁外完成canonical、manifest及原件读取；原worker的claim/fence、数据pin和ABI进入提交前guard；按原写入行数/字节/事务预算提交完整原件，再原子切换head。未完整PREPARING不可消费，取消/崩溃不能留下已生效空头指针。hash相同的重复提交幂等，身份相同内容不同必须冲突隔离。禁用仅追加事件并撤销后续可用资格，已存在原件和消费证据保留。

## 5. 跨区间消费不能放宽原同dataset合同

当前 `ResearchContextR30R1.read/stageBoundR31` 和 `ResearchStoreR30R1.consume`校验当前dataset、source revision、source commit、ABI和SELECT角色；`ResearchConsumptionStoreR31.completeWithin`在原BORN事务里验证实际子代。这些正确约束不能为“迁移”而删除。

拟新增 `ResearchContextR30R1.readRuleSnapshotR342/stageRuleTransferR342/proposeRuleR342`。读取规则时先验证其原发现manifest，再在当前消费pin形成**新的派生上下文run**：输入显式包含规则manifest及原discovery pins、当前dataset/区间/角色/源版本、绑定库与ABI；输出是当前区间真实生成的完整proposal。该run通过已有研究原件提交路径成为当前SELECT上下文的已提交产物；旧发现run仍保留其原dataset。当前run只是可迁移规则消费事实，不重复宣称旧公式在当前数据上已训练。

`RuntimeR27.EvolutionTask.step`目前在step 2前捕获 `ResearchContextR30R1.read`，在step 2后 `stageResearchHypotheses`，在step 3后 `commitResearchAdmissions`，并把演化游标与publication一起提交。新规则必须在这一时序内捕获generation固定的规则指针和seed；中途新发布规则从下一次合法捕获开始可见，不能改变已准备proposal。

所有生成候选进入 `EvolutionLoopR27.stepBirth → stepGate`。`stageResearchHypotheses`将规则来历加入原proposal；`commitResearchAdmissions`仍调用 `ResearchStoreR30R1.consume → ResearchConsumptionStoreR31.completeWithin`。拟 `completeRuleUseWithinR342`仅由此原事务调用，核对rule manifest、当前pin、slot绑定、原proposal与真正BORN child，并原子写入唯一use回执。必须保留原最多4个sourceRuns的有界校验；完整发现来源嵌入规则manifest，不无限扩展主事务来源列表。

去重键至少绑定规则manifest、当前消费pin、generation、seed/slot绑定与proposal/child身份；游标、RNG和消费回执必须在原提交确认后推进。重启重放相同输入不能二次出生或二次训练。类型本身、报告页面、UI和规则发布器都不能直接插入`r27_policy`或修改证书。

## 6. 文件与方法施工顺序

| 顺序 | 具体文件 | 原方法与拟新增入口 |
|---|---|---|
| 1 | `strategyv2/TypedGenerationRuleR342.java` | 新增第2节纯接口及嵌套类型；全部PLANNED |
| 2 | `strategyv2/AtomPublicationStoreR31.java`、根包`LifecycleSchemaR31.java` | 保留`open/prepare/publish/requireReadyR31`；新增规则方法和DDL；接`definitions/Definition/requireDefinitions/advanceWithinMigration` |
| 3 | `strategyv2/AtomWorldR27.java`、`strategyv2/RuntimeR27.java` | 扩展`atomsStep`的有界规则阶段，拟`inductGenerationRulesR342`；保留`AtomsTask.step`原owner，不改`derived/molecules`旧行为 |
| 4 | `strategyv2/ResearchContextR30R1.java`、`strategyv2/IdeaFamilyR27.java` | 扩展`read/proposeFamilyR31/stageBoundR31`；新增规则快照/跨段派生/实例化入口，保留`propose/generate`的原合同 |
| 5 | `strategyv2/EvolutionLoopR27.java`、`strategyv2/ResearchStoreR30R1.java`、`strategyv2/ResearchConsumptionStoreR31.java` | 原`stepBirth/stageResearchHypotheses/stepGate/commitResearchAdmissions/consume/completeWithin`；拟`completeRuleUseWithinR342`只装饰原实际消费事务 |
| 6 | `app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B08ContractTest.java` | 扩充现有`test_r341_b08_04_a01`真正执行F6-B08-02，不只转调旧失败look测试；新增类型合同测试以本批明确文件卡登记 |

前五行的Java相对路径除测试外均以 `app/src/main/java/com/ethk3/novaonepro/` 为根。只有第1行是拟新增应用文件；其余是基线真实文件的增量修改。每个新接口均须在文件卡标注PLANNED，直到对应source_after和实际调用证据完成。

## 7. 可签收的实际证据

- 两条以上发现公式有原AST、训练参数变化/产物、配对消融和发现pin；规则编码能精确回读；源码ABI或payload改变会被拒绝。
- 两个未参与发现的开发区间分别产生去参数后不同的可执行结构，经过原BirthGate、真实训练和首评；规则发布、proposal、BORN、训练、首评分别有原件，不互相代签。
- 同原输入、同seed和同分配预算的规则禁用对照，实际派发或生成结果发生可解释变化；只改展示标签、参数克隆、等价交换树、whole-tree wildcard、无效父边均为拒例。
- 发布中断、消费中断、不同source revision、跨ABI、禁用后重试、重复delivery、错误角色、被封存数据和超预算均通过真实生产入口验证；恢复后无重复出生/训练，无空head或半份规则。
- 规则使用不会减少既有家族最低预算、改变旧SCORES/MOLECULES数值语义、写入正式未来样本或降低四类证书门。B12再通过同一服务的多根、至少三代集成链确认连续运行。

这些工程证据证明能力及接线已实现，不证明自然命中优势；候选晋升继续使用其原域独立证据。完整主链及证书语义由已审计的 `MAINLINE.md`、`PRESERVATION.md`、`ACCEPTANCE_AND_STATUS.md`、`CERTIFICATE_DOMAINS.md`共同约束，本规格不覆盖或改写它们。
