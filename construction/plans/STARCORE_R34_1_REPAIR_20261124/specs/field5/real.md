# 已获证候选的真实出手、结算与质量计数

现场 577311122 的最新证据中，Q02 为 NOT_DEPLOYED，Q07 的计数为 0 且状态为 RECORDED_UNVALIDATED。第 4 版已规定怎样核验首次发证，以及如何在隔离安装中让合格样本走到真实交易和结算。本轮只在宿主机上运行核验工具，安装版仍待施工和实测。部署后的自然质量还有一个漏洞。实际执行第 4 版 `quality_projection` 中的 Q02 和 Q07 谓词，250 个任意互异的键填入 38 个 true，Q07 填 99 个资产和一项名单，两项局部校验均可通过，甚至不用提交交易表。这个对照只复现两处分项漏洞。第 5 版在同一候选的自然发证检查以后，从源库重新计算真实出手。

## 从候选到数字

1. 候选具备 V1 全部条件，签发后即可竞争合法 REAL。首次发证允许 `quality_projection_status=PENDING` 且没有部署资料。首个合法机会即可选中；先收集一份真实交易时可以单独核账并显示 1 笔。原有 250 个已部署机会和实际出手率 15% 用于综合质量 Q02。
2. B09 由 `DbStore.insertAuthorizedTradeWithinCanonicalR915`、`UnifiedResearchChoiceR932.freeze`、R974 精确预测及 F05、`DecisionGateEngine`、`SettlementEngine.settleV51ByDraw` 实际提交后取证。在同一一致性快照读取预登记的部署窗口及其中**全部候选**的 trades。用 `target_issue`、唯一交易、授权和位置 K3 核对实际胜者。其他候选赢得并执行这一期，不给当前候选计 ACT。已选中却没有交易、实盘交易未结算及尚待权威开奖都留在报告；实际成交与已结算的分子分别列示。
3. B11 报告与 B12 自然质量验收共用上述读取结果。同一预登记候选的 Q07 本次观测单位是一个候选，仅在它已完成自然资格且实际有对应的已结算主链交易时确认实际采用。全库原 Q07 统计保留原值及作用域；另按候选身份对通过正式原始自然回执的集合去重求数，可汇总任意数量的资产。不能把全库旧计数或其他候选的交易代入本次单候选判据。
4. 在初次合法 V1 发行时独立记录原证书字节及绑定；后续撤销须保存撤销的生效时刻。撤销前已提交的交易仍能结算；新授权不得越过撤销时限。尚无部署原件或样本不足时上报真实状态。现场 R35 基础影子和工程用例不进入部署分母。

## 新增回执字段

`natural_evaluator_artifact.deployment_evidence_artifact` 指向 `R341_DEPLOYMENT_BOOK_5` 索引。索引及 9 种原始投影都带 `source_commit, apk_sha256, device_identity, candidate_hash, protocol_hash, fixture=false`。索引登记 `snapshot_at, issuance_artifact_sha256, trading_database_artifact, trading_database_sha256, originals`。已通过资格验收的同一 `certificate_artifact` 在申请部署核验时还须投影 `certificate_hash, model_hash, gate_bytes_artifact`，从原签发代码回读证书 SHA、模型和 gate 字节，保持实际候选身份。122 的 `LiveShadowsR27.certify` 在追加 inputHash、evaluatedAt 之前以 JsonR27.write(g) 计算 evidence_digest；`gate_bytes_artifact` 提交这个输入原串的精确字节，不能把整份最终 gates_json 的字节摘要冒作证书。对照 `MainlineFreezeProofR31` 的规则，模型使用 modelStateBindingR31.modelHash，普通公式回退到 bindingSha256，精确校准摘要取 calibrationBindingR31.calibrationSha256。当前候选如已撤销，历史合法使用引用当时通过自然验收的原签发与应用回执，并查后续撤销时间，不能以撤销时点的待观察行重新发证。

`originals` 为 calendar、choices、decisions、trades、exact、probabilities、draws、settlements、revocations 九个不同路径。各原件字段 `source` 的精确取值见 `tools/real_application5.py:TABLES`。calendar 的 `registered_at` 不晚于窗口开始，`window_start,window_end,before_window,after_window,targets,draw_source_id` 一起固定完整日历，采用已有 PK10 期历的时区、水位和跨日编号。每个目标有 `target_key,opportunity_at,deadline_at`。其余原件带 `snapshot_at,calendar_sha256,trading_database_sha256,rows`。所有路径及摘要都进入回执的 artifacts。部署窗口与独立未来资格窗口分别登记，不要求两组机会一致。

`trading_database_artifact` 是对窗口末数据读一致的 SQLite 快照。可以本地对原库一致备份运行；交付最小片段时，应保留同一源快照身份、导出方法、原范围、总行数与可回读的业务证据。trade 原件不可按当前候选先行筛选。校验器用只读连接打开这份快照，直接查 **trades** 的 `_id,target_issue,position,n1,n2,n3,authorization_id,action,stat_scope,settled,hit`。从 WAL 中尚未合并的设备原库须通过一致备份或读事务导出，避免遗漏已提交行。设备端收集和原始数据真实性继续由真实安装及只读采集回执证明。

每期有一条选择结果。正常期由 `selected_policy,terminal,policy,certificate_hash,model_hash,proof_hash,action,user_enabled,frozen_at` 绑定资格和冻结的单位置 K3。其余退出保留具体终态。决策、交易、精确预测和概率收据分别绑定动作与权威授权，表的不同查询范围见样例。全局交易列出所有胜者；exact 与 probabilities 仅取当前候选的本期真实收据。实际开奖 `source_id,draw_id,draw_at,numbers` 与交易结算按目标连接，逐笔用原位置和原三号码从 PK10 排列重算命中。一笔先交易后撤销的合法历史可继续结算。

部署汇总包含全机会 `rows`、当前候选 `real_acts,settled,hits,missing_selected,missing_authorized,pending_draw,unsettled_after_draw,pending_settlement`。Q02 的 `deployed_opportunities`、`real_act_rate` 与该结果逐项对照。Q07 的单候选分项增加 `scope=ONE_REGISTERED_CANDIDATE`，数量和具体身份须一致。相关原件出现缺项时不得取得相应 PASS。样例与故障对照可运行 `python3 -B "$p/tools/test_real_application5.py"`。

每个已有 BET 决策都要回连同一候选的选择和授权，即使交易库尚无该笔。R932 声称弃权或未运行，但同一期已有 BET 时两份原件互相冲突，应回到 B09 查原决策。已被选中而未成交单列 missing_selected，其中已有有效授权的期列入 missing_authorized；没有进入授权的期可由两者差集定位。成交后，pending_draw 表示尚未取得该期权威开奖，unsettled_after_draw 表示已取得开奖却未见对应结算，pending_settlement 保留两者并集；三项都不计入已结算。
