# 从自然资格到第一次和下一次真实出手

第二份报告 `R30_REPORT.json` 第 8 章的证书、决策、撤销列表均为空。`UPGRADE_DIAGNOSIS.json#/capabilityRepairR32/operatingSummary/independentFuture` 记录 independentFutureIssues=0、futureActs=0、qualifiedExperiments=0、activeCertificates=0。附件 `report-text-1d74ec58c6c9aa3077c6a8f71b4e768c666bac724a5866455890e762169f1bc7.txt` 的节点 14 按期列出 202611240598 至 202611240605 的 NO_CANDIDATE；节点 15 对真实收据表的查询返回 NO_MATCHING_RECEIPT。该窗口尚无自然超越对照的合格资产。以下是对 122 源码控制流的核验和可触发的故障场景，现场是否曾经错过合格资产须以后续原件判断。

## 首次和连续两期应用须走实际业务入口（B09-04-A01）

122 的 `RuntimeR27.LiveTask.step` 先运行 certify、freeze，再 settle、qualify。`V51RuntimeOrchestrator` 的动作选择阶段独立调用 `UnifiedResearchChoiceR932.freeze` 和 `CertifiedPredictionR974.prepare`。原选择器在已有相同 target/bundle 行时直接返回；首次选择没有候选时也可插入空 consumer 行。若有效证书已在截止前存在、当期 ACT 证明还在另一工作者上准备，先到的选择器可能永久冻结一个空选择。之后证书、证明、授权甚至仍在合法赛前时间内，都不能更换已提交的选择。两份报告尚未给出这种竞争发生的正例。

施工先按目标、bundle、源修订和法定截止记录认证、ACT 准备、统一选择的请求、提交及返回时刻。在可能产生本期可用证明且来源健康的分支，分清“尚待生产者”与“已完成并确定无可执行候选”，由既有服务按同一请求有界续排。用生产者提交后通知或受期限约束的重新读取安排最终选取；预留现有实时预算，耗尽立即记录具体终态并维持基础预测。选择器保留现有跨引擎优先顺序，R27 入围后沿同 gate 既有评分排序。已合法选中其他资产、有效策略本期弃权、用户关闭已有授权开关、无自然证书，各自留实际原因。冻结以后保持首写不可变。因超时错过的目标下一期重新参选。

验收从同一次后台开奖处理进入。无更高优先合格候选、合法隔离资格及原 ACT 全部就绪时，分别强制“选择器先到，证明在预算内完成”和“证明先到”两种顺序，再覆盖连续两期合格 ACT。每期须读回同一个动作的 R932 选择、R974 精确预测、真实政策通道 CONTROLLED_PROBE、DecisionGateEngine 产生的 BET、同目标唯一交易及真实结算；逐笔比较 position、三个号码、model、certificate、proof 与 authorization hash。没有部署后记录的第一期同样可以进入；第二期在准入条件均满足时不得因上一期已出手被任意限频。把用户原有明确开关设为关闭和提交中撤销另做反例。开关默认值与全局编译锁原意保持；`b12Reject` 的 controlled 分支位于旧全局锁判断之前，禁止通过放开所有候选来构造成功。赛前已冻结空结果、事后才产生证明或首次到达时已过截止，一律保留真实缺口。

## 重试看授权原件是否齐全（B04-04-T01、B09-04-A02）

122 的 `V51Repository.commitCanonicalBundleVerifiedR368` 在一个事务内写核心授权、交易和 canonical 观察。`DbStore.insertAuthorizedTradeWithinCanonicalR915` 遇到已有交易会比对 position、K3、authorization_id 后直接返回既有 id；该分支自身不回读同时应当存在的 `r974_exact_real_receipt` 与 `research_exact_probability_f05`。B04 增加按当前合同识别的历史/部分恢复样本，检查完整条件是同一游戏、目标、交易、决策、授权、候选、模型、证书、概率和证明原件一致。支持旧版本数据按当时的合同显示；把本文的两个原件要求用于本应写入它们的版本。完整者只读重放；有缺口的交易保留原始数据，标记具体缺失或冲突，并进入既有的恢复流程。可以从完整在先原件重建允许的投影。缺少授权或冻结事实的记录保留未证，禁止生成第二笔交易或开奖后补造 REAL；已合法交易依原依据进入结算，诊断异常单列。

B09 在隔离安装复用同样的事务场景。分别在授权至提交前撤销、交易提交后开奖前撤销，以及提交成功丢失回调后重放。前者须零新增真实交易，后者原交易仍按原赛前号码结算且后续新授权受阻。并发重复调度和重复开奖沿唯一结算 owner，完整回调只产生一套交易、收据与标签；原件缺失或摘要交换必须精确报错。验收观察的是应用内的 REAL 决策和交易账，研发排序的正权重、R35 普通影子、资格影子及实验夹具各自保留原身份。

## 上线后按“本可真实出手”的机会对账

B09-04-A03 现有的无证书真机用例继续验证合理拒绝。B11 报告同时从完整期历列出各期当时可用的自然证书、目标 ACT/弃权、原用户开关、选择时点、更高优先候选、PIT/撤销/到期、授权、真交易与结算。记录每个已取得合法授权、当期本应交付的目标，第一段缺原件的地方和每种排除的原始理由。未获证则部署样本数为 0，部署成功率待观察；若有合规实盘机会却没有真实交易，列出所有漏用的期。分开报告候选合格次数、入选次数、合法主链 ACT 机会、已入账真实交易及已结算真实交易。多资产同一目标最多一笔；跨引擎优先级导致某候选未当选时，仍可核实实际胜者及其唯一交易。用完整日历算此前协议的覆盖率。首个自然资产一旦出现，登记它首次合规目标的生产证据；工程正例保持工程身份。

### B09 工程回执的可执行格式

施工者在隔离 Android 测试库中运行正式生产入口，再由测试驱动导出已提交行、交易前后状态和生产者事件。执行原件中的 `mainline_campaign={path,sha256}` 引用索引。沿用回执的 `apk_artifact`、`apk_identity_member`、`source_after` 与 `plan_manifest_sha256`。将 APK 和每个被引用的文件列入 `artifacts`；所有路径相对该回执的证据根。索引使用 `scope=ENGINEERING_FIXTURE, source_commit, apk_sha256, run_id, time_scope, timing_registration, cases`。时间投影到同一设备、boot、单调单位的可比较时基，归档原 wall/mono 对照锚点。每个阶段原件带索引身份、scenario、db_lineage、target、input_sha256 和 producer。保留原生产日志/测试触发器及只读导出 SQL，字段从原件提取；计数同时导出该目标全部匹配行。

五组场景依次为 `PROOF_FIRST`、`PENDING_THEN_PROOF`、`LATE_TERMINAL`、`CONCURRENT_RETRY`、`NEXT_ELIGIBLE`。前四组从同一份生产兼容输入开出各自隔离库，最后一组在第一组的库里实际推进到下一期。每组含 `input={path,sha256}`、`db_lineage` 和下列七个已登记原件的引用。input 记录 policy、target（连续期序）、事先固定的 deadline、action（位置及三个号码）和原用户开关。gate_bytes 是发行方实算证书摘要的准确字节，同时保存原 gates_json 供重新导出。迟到例保留 R27 已赛前冻结、证明可见过晚且统一选择已经封存的原件；它的交易和结算 REAL 行数组均为空。

| 阶段键 | 生产来源 | 必须从原件得到的内容 |
|---|---|---|
| qualification | r27_certificate | policy、ACTIVE、gate_version、gate_bytes、certificate、issued_at、expires_at |
| act | r31_mainline_freeze | policy、certificate、model、proof、target、action、act、selected_hit_p、calibration_hash、frozen_at、deadline |
| choice | research_consumer_choice_r932 | 选择行按原始键值规范计算 row_hash、bundle、consumer、candidate、policy、certificate、committed_at；空选择保留原空值 |
| decision | DecisionGateEngine.decideV51B12 | choice_row、decision_id、authorization、action_type、lane、证书与动作摘要、action_hash、selected_hit_p、authorized_at |
| ledger | V51Repository.commitCanonicalBundleVerifiedR368 | committed_at、trade_rows、exact_rows、probability_rows。三种投影逐行带 target、trade_id、authorization；trade 另含原 decision_id、role、action，两个 exact 投影含 candidate、certificate、model、proof、action_hash，probability 另有 calibration_hash 与 selected_hit_p |
| settlement | SettlementEngine.settleV51ByDraw 对应的权威结算 | draw_at、settled_at、当期完整 10 位排列、real_rows 中的目标、交易、授权、角色及 hit |
| schedule | V51RuntimeOrchestrator | first_read、first_read_outcome、final_rows_at_first_read、proof_visible_at、choice_rows、returned_choice；并发场景记录两次不同 attempt 的 id/start/end/result/choice_row/trade_id，两个区间都跨过同一提交，分别为 COMMITTED 与 REPLAY_EXISTING |

`tools/mainline4.py` 的 `check_mainline_receipt(run,receipt,originals,repo)` 由正式 B09 回执驱动。用 `python3 -B tools/test_mainline4.py` 在方案目录运行校验器的正反例；该命令只测试校验器。实装签收使用项目既有 `verify_receipt.py` 命令和 Android 原件。这里的 REAL 是软件内部的唯一正式决策、交易及结算账。

**预先登记选取边界。** 在 B09 现有 Android 测试源 `R341B09ContractTest.java` 中放一条唯一的单行 `// R341_MAINLINE_TIMING_V1` 标记，空格后接 JSON。字段为 `unit="ms"`、`reserve_ms`（该隔离场景实际生产调度保留给授权、冻结提交的毫秒预算）、`first_read_before_cutoff_ms`。此配置随被测源码提交固定。预算数值取该构建和场景的真实生产时限；两个对照复用它。执行前将相同 rule、runner 的仓库相对路径、registered_at 与运行身份写入 timing_registration 原件。schedule 上报其摘要、生产入口读取的 observed_execution_reserve_ms、decision_cutoff、selected_at。校验器从 receipt.source_after 的 Git 对象回读规则，与原件和实际调度值核对。

以 `cutoff=deadline−reserve_ms` 为选取截止。两个竞争用例从同一份输入、相同首次空读时刻出发，首次必须为 PENDING、最终选择行数为 0。用可控时钟/投递屏障驱动生产入口：在 cutoff−1 ms 证明可见，截止前选中并在总 deadline 前提交 REAL；在 cutoff+1 ms 证明才可见，至 cutoff 前最终选择行数仍为 0，在 cutoff 结束等待，迟到投递后的实际回读保留原终态。记录 final_rows_before_cutoff、after_late_delivery_choice。提前永久 HOLD、测后修改预算、两条路径的权威截止不同、或在迟到回调中换行均拒绝。真机继续保存实际单调时刻、耗时与窗口，受控时刻用于确定性工程用例。
