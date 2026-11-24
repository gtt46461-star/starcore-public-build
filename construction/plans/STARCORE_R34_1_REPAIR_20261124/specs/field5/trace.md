# 在同一次服务运行中追到三代公式

第 4 版的三代校验逐条核对了单条谱系的训练、消费和出生。另一个校验复算了服务在八个工程目标、两个进程间持续提交的各阶段结果。两个索引只核对施工提交和 APK 的身份。原有两套测试一套用 `stream-1`、ordinal 1000–1007，另一套用 `reference-test`、ordinal 101–103，同一提交、同一 APK 且各自均通过。将这两套结果提交，仍缺少“连续服务中实际消费了这三代”的证据。

B12 的 `A-B12-03b` 在同一 Android 隔离安装启动生产服务，联合采集两套索引。旧的独立夹具可作为定位某一阶段的子测试。本次工程用例的预注册包含跨越进程重启的目标，曝光、训练角色、候选与基础预测的来源按原生产合同核查。实际业务写原存储，采集器读取原事务/源表、业务键和权重字节。无需为诊断建立第二套策略或交易存储。

## 数据接口

1. 在本来已由 Git 提交固定的 `tools/r341/fixtures/continuity_b12.json` 中增加 `lineage_target_ordinals`。至少三个严格递增的预定目标，全部在本次日历中，首尾跨过实际进程重启。三代索引中各轮使用这些目标，最后一个融合子代须在新进程冻结并实际结算。多轮输入仍接受原有时间和有限工作预算。
2. 两个索引及连续账本各写同一 `case_id=A-B12-03b`。索引的 scope、run_id、source_commit、apk_sha256、time_scope 完全匹配。连续 ledger 与三代索引共用 `database_instance`，取同一安装数据库实例身份（例如装载清单加持久数据库创建标识，不能使用随 PID 变化或仅取文件路径的值）。提交过程继续采用同一 boot 单调毫秒时钟，服务进程按外部观察的重启记录区分。
3. 每轮 train/publish/research 和受测的 mutation/fusion 从连续账本中已核验的 COMMIT 选取。其原件增加 `learning_original_sha256, database_instance, policy_hash, target_ordinal`。该摘要必须指向本次 learning index 所引用的具体阶段原件。其对应的学习记录增加 `service_origin={database_instance,process,source_table,durable_key,work_issue}`。时间分别对齐实际优化器提交、发布、研究完成或出生事务时刻。变异/融合的 policy_hash 取本次生成的 child_hash，target_ordinal 为触发它的父代研究目标；下次训练继续通过原 birth_ref 核对真正训练的子代。这些工作归属期可以与所预测的目标不同，两边需对上同一笔任务。
4. 实际冻结、结算、反馈取受测候选的原件。若连续账本中的该阶段是候选本身，可直接引用对应 COMMIT。若该处实际是另一路基础 R35 影子，保留原行，在执行该工作的服务时隙的 `ledger.rows[].lineage_candidate_rows` 中引用独立的候选事务原件。它沿用 COMMIT 原件的 scope、stage、issue、process、source_table、durable_key、committed_ms、scenario_sha256、effect 和上一步新增字段。每个引用使用连续索引目录相对路径与 SHA-256；其实际载荷也要登记于本次回执。核对候选和所属目标期开启、截止、开奖源和进程身份。基础影子的收益、结算及键均不能抄给受测子代。
5. 两边同名并不足以判断实际消费。验收器同时比较两边的持久键、同一进程/时钟的提交时刻和业务载荷。train/publish 的 effect 对应被发布和应用的实际数值权重字节；research 对应 artifact_hash 所指实际研究产物；mutation/fusion 对应子代规范 AST 的 program JSON；其摘要按 UTF-8、键排序、无空白、保留 Unicode 的 JSON 计算。冻结载荷取 policy_hash/target_ordinal/role/position/numbers/model_hash/weights_sha256，结算载荷取 policy_hash/target_ordinal/freeze_ref/draw/hit，反馈取 policy_hash/target_ordinal/settlement_ref/event_key/committed_count，三者各从当前候选的原件提取为同样的规范 JSON。保留原始事务及提取器以供复验。
6. 在当前版的正式 `A-B12-03b` 中，已有两个校验器均通过后执行 `check_joined_lineage(run,receipt,originals)`。三轮六阶段与两次出生共需找到 20 个不同的服务提交记录。缺一次训练、接错开奖、另一进程的记录、仅有基础影子或换了另一组公式时，报告首个不匹配的阶段和对象。这项工程签收验证机制可持续运行。自然样本中是否有收益和是否可发证仍依各自协议判定。

从方案目录执行 `python3 -B tools/test_joined_lineage5.py` 可复验。正例覆盖重启后对子代的真实消费关系；负例将源码和 APK 保持相同，分别替换运行、时钟、预登记目标、数据库、用例、数值权重、业务键、观测时间、进程或目标，或移除候选结算。参考工具测试使用合成原件；真实 Android 工程运行仍须采集生产原件。
