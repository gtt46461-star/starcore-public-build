# 第一个可训练模型与第一条可执行公式

第二份报告的 R26“发布恢复”章导出 50 条旧候选，全部属于同一 campaign，状态均为 BLOCKED_WARM_NUMERICAL_ABI_PRESERVED，原 cursor 在 23 至 158 之间。EVOLUTION 章有 50 个尝试、10 个结构候选、0 个行为新颖。R27_02_EVOLUTION_TRUTH 另报已完成代数为 0，r27_generation 为空，七项计数为 null。前一组属于 R26 汇总，不能据此认定 R27 已跑完出生门。施工先从服务生产入口同时观察 R26 的新工厂/训练事务、R27 的 dataset→birth→gate→replay→record 提交，保留两条独立身份。R26 冷重生会给旧结构生成新状态，能够训练的冷子代与具有不同可执行行为的新公式须各报一次。以下三个分支在 122 源码可定位。报告没有对应逐调用异常，设备上的第一处实际断点仍须由开工后的回执确定。

还有一项可以直接用两份 DIAGNOSTIC_SCOPE.json 对账：两次各有 1 条 r27_evolution_cursor，但 r27_policy、r27_lineage、r27_replay_score、r27_generation、r27_live_shadow 均是 0 行。R26_REPORT.json 的 CONTINUOUS_RUNTIME 原行同时记录，r27-evolution 为 BLOCKED，等待 r27-replay-dataset；r27-replay-dataset 为 BLOCKED，原因是 WRITER_HOLD_FAULT_BACKLOG。foundry-su2 的快照原因也为 R33_WAIT_WRITER_HOLD_FAULT_BACKLOG。首轮修复顺序据此从生产写入准入、可恢复的 replay dataset pin、进化游标、BORN 与首个重放逐段取证。单有游标、服务心跳、旧训练游标都无法填这五张空表。这是当时持久化行的报告值，实际时效以同进程新回执核对。特别是下面第 3 项依赖旧拒绝行，当前 r27_policy=0，故它是打通生产后必须防的再堵塞，不能解释这次已有的空表。

## 1. 坏旧父代或旧研究对象不得拖死整个新工厂

`FoundryCampaignR26.step` 在没有 RUNNING 工厂时先形成注册种子，再调用 `savedParents` 对按适应度排序的前三份历史文本逐一 `ModelGenomeSU2.parse`；随后 `FailureDirectedResearchR26.select` 读取旧研究记录，处理先前的 proposal_json 等数据。两部分都在创建/续用 FOUNDRY_BIRTH 发布计划之前。一个符合 SU2 文本筛选但不可解析的旧父代可中断整轮；符合失败研究选择条件的损坏 JSON 也可在此处引发 IOException。独立种子一次也无法到达发布。冷重生自己的单父重试不能覆盖这条入口。

在 R341-B06-01-A03 同时覆盖冷重生和“没有运行工厂”的真实生产入口。保全原行及原摘要、限定异常对象与可恢复原因，按有界稳定分页继续选其他合法父代；选取预算或页到边界时持久化续扫位置。未来准入和选择结果不继承。无合法旧父代时，已有注册家族仍可就有效冻结窗口发布基础候选并完成首个实训事务。候选、失败研究游标、假设、父子关系分别按同一已提交项目写入，隔离或推迟的对象保留待处理身份。注入可识别的父代格式/研究元数据损坏，并验证另一合法父代与基础候选获得可核对的首次训练提交。对切片取消、失去 writer fence、DB 整体不可用、源版本改变仍按原语义退出和幂等重试；这些异常不得被归咎于某个父代。

## 2. 避免刚出生的工厂在首个训练切片立刻过期

`FoundryCampaignR26.capture` 最多取最近 4096 条观察，并以至少 64 条为准；按行数取满不保证其期号跨度小于 5000。`ColdRebirthContractR34.requireSnapshot` 拒绝 through−first≥5000；普通工厂新建时没有同一个前置判定，下一切片 `FoundryCampaignR26.step` 才会以 latestOrdinal−first≥5000 清空快照并置 EXPIRED_RETENTION。在“旧段+断档+近期连续段”条件下，即使近期已有足够有效标签，可能反复重试同一不可用快照，或留下出生后未训练的新身份。

在 R341-B06-01-A01 共用一次可复演的输入选取。冻结最新已提交且当时可见的原始数据，预先按两处相同的保留界限约束首尾，再依模型的断档规则取足量合规尾段；记录首尾、全体入选期及实际缺口、父代已曝光截止、可用样本、源摘要和输入修订。输入不够时只等具体资料；不拼接断档相邻性，不占用一个已知无法训练的工厂名额。候选与专家使用各自现有训练者；旧父状态和旧证据保全。正例固定最新序号 10000，旧记录含序号 ≤5000，最近有 256 个连续且合法的已结算样本，父代曝光早于窗口截止；验证新源、出生及随后的首个可恢复训练状态，再在相同源重启复算。边界检测最新减首期分别 4999、5000、少于64条、父代曝光晚于新源和新的修订；旧范围不得直接作为已通过数据集。不同历史模型有更长因果预热要求时，须另验各自要求。

## 3. 让暂时缺输入的公式可以重测，同时保留全局防重复出生

`EvolutionLoopR27.revalidateResearchAdmissions` 调用 `StoreR27.recordedPolicies`，后者按 policy_hash 读取 r27_policy，不查状态或原拒绝原因。`EvolutionLoopR27.stepGate` 对已在集合中的 hash 直接记 POLICY_HASH_ALREADY_RECORDED。生产 `StoreR27.EvolutionSink.policy` 对 REJECTED 与 BORN 都 INSERT OR IGNORE；后续提交也检查已存在 hash。若开发探针 A 输入不足导致 OUTPUT:AVAILABLE_ROWS 拒绝并落行，即使下一合法探针 B 已有可用数据，同一有效程序可在运行前被全局拦截。单改读查询仍会被写前重复保护和 INSERT OR IGNORE 拦下。

在 R341-B06-03-A02 同时改读回、写前 guard 与出生提交语义。以全局规范策略身份标识程序，按开发输入/版本/原因另存不可变的历次评估。只有能由原件确认的输入依赖变化或经版本化修复的机制允许重试。缺原拒绝依据的旧行先回查、保全；不得把所有旧拒绝批量改成 READY。重复提交无新输入时读取原结论。一次成功出生以同一身份唯一提交，保留先前拒绝、不重复写谱系、不重复增加尝试/行为创新或自然机会；已出生、已退休、固定语法错误和同源行为重复仍走原约束。正例先在缺合法可用行的 A 上产生可解析拒绝，再在 B 上使用同一个 policy hash，从真实 gate 到首份 replay 取证。反例以同 A 重试、复用 B、并发工人争同一出生、崩溃后重试、同一结构换名、偷用未来标签分别验证。开发重测只使用事前固定的开发数据及预算，质量对照、正式未来机会和真实资格保持各自原账。
