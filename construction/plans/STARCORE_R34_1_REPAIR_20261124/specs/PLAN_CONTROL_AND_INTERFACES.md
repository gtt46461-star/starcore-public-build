# R34.1 施工控制、共享接口和验收口径

## 1. 唯一身份与施工范围

本方案绑定实际安装 R34-B12-2026-11-24-577311122，应用基线是 construction/r34-20261123 的 5562f9b19aa09f9f7d9e69ca085db56c537967fc。main 是方案交付位置，不能把 main 的旧应用目录复制覆盖此基线。新的施工分支以此提交或已经逐项核验的后继为起点。

本轮产品约束继承：单位置 K=3；不引入 K4、动态 K、多位置真实出手、追期、马丁或倍增；唯一真实决策入口和唯一结算入口保持可核验。研究可比较多个位置/家族，但真实动作仍遵守原合同。历史训练、开发挑选、确认、自然未来和真实出手分别统计。

累计可探索的策略结构不以某个固定总数封顶；活跃内存对象、并发工作、每轮预算、缓存和新增磁盘占用必须有界。淘汰停止计算与丢弃唯一原始证据分离。

## 2. 两类前置条件，禁止循环等候

工程前置只允许来自当前批次或已经通过的前序批次。自然未来资格不可以成为创建候选、训练、影子预测和反馈处理的前置条件。未有合格资产时，真实入口应保留 NO_BET/HOLD 与原因；基础影子和研究链仍须运行。

批次局部工程验收使用隔离 fixture、重启恢复、故障注入和实际 Android SQLite 行为；不要求后批尚未建立的自然证书。隔离测试身份不得进入生产资格库。真实设备与自然样本另列状态，不能用工程通过代签。

B01 必须完成当前方案的构建身份桥和版本分派。B02 建立可验证的写入/控制服务机制。B03 才安装和核验研究结构、注册任务。B04 去除赛前和结算热路径的 DDL 时，必须同时完成相应 critical schema 迁移，不能交付孤立删除。

B10 是总存储治理，但 B03 的启动迁移不能等 B10：B03 应能在既有原件保留和受限迁移预算下完成结构安装，不能先强制释放历史债务或取得自然资格。

## 3. 跨批共享身份字段

每个事件和产物按用途携带下列字段；不适用字段显式 null，不用空字符串冒充已验证：

| 对象 | 必须绑定 |
|---|---|
| 运行事件 | source_commit、release_id、process_epoch、boot_id、wall_time、elapsed_time、foreground_state、task_id、owner、fence、phase、outcome |
| 目标机会 | target_issue、source_id、observed_at、decision_deadline、history_identity、request_id、terminal_reason |
| 赛前冻结 | decision_id、target_issue、policy_hash、model_hash、trained_through、input_hash、freeze_commit_time、role |
| 数值训练 | candidate_id、parent_id、training_role、input_epoch、input_hash、cursor_before/after、optimizer_before/after、state_hash_before/after、commit_receipt |
| 研究动作 | run_id、frame_id、proposal_id、source_artifact_hash、consumer_id、operation、child_policy_hash、receipt_hash |
| 资格 | experiment_id、pre_registered_policy_hash、calendar_window、alpha_contract、eligible_opportunities、ACT/HOLD/missing、certificate_hash |
| 发布/安装 | source_commit、product_tree_hash、plan_manifest_hash、version_code、application_id、apk_sha256、signing_cert_sha256、installed_identity |

时间比较使用同一 boot 内的单调时钟；跨重启转换需要明确墙钟锚点和不确定性。未知截止不允许写成准时。输入被固定后不能以稍后“最新历史”偷偷替换。

## 4. 提交、恢复和失败模型

计算在写事务外准备不可变输入和结果；事务内复核 owner/fence、当前版本、目标期仍可提交、依赖哈希一致，然后提交最小必要记录。提交成功必须同时产生可追溯收据。事务失败保持原游标，不推进为完成。业务提交成功而观测收据待刷新时，区分 COMMITTED_RECEIPT_PENDING，恢复先查业务主键，不能重复训练或重复结算。

故障类别必须拆开：

- WAIT_RESOURCE：有界资源等待，保留 next_attempt_at，不消耗算法质量失败额度。
- WAIT_DEPENDENCY：明确等待哪个已注册生产者和最旧等待年龄。
- SOURCE_UNAVAILABLE：缺表、缺原件、输入断档或读取失败，不能等价为健康空集。
- DETERMINISTIC_BUG：同输入可复现的逻辑或数值错误，隔离该对象并保全输入。
- MISSED_DEADLINE：本目标终态不可逆；不得事后补造赛前预测。
- FAILED_EFFECT：真实新数据未改善或不达资格，保留负结果，淘汰或继续研究，不冒充工程异常。

控制收据、故障排债与初始化首错记录不能反向依赖其正在修复的同一堵塞写入路径。仍须有持久化、空间上限和原子性，禁止仅写内存标记自称恢复。

## 5. 文件所有权和合并顺序

FILE_CARDS.json 为每个文件定义首个主负责批次；同文件后续编辑必须在卡内列出批次、方法和原因。共享文件只允许按 B01→B12 顺序修改，每批基于前一批已提交内容，记录实际 source_before/source_after。不同代理不得同时写同一共享文件。

同一个方法有跨批扩展时，后批只能保持或扩大前批接口合同，不得撤销时点、签名、原件保留和幂等约束。发现未登记的跨文件更改，先补对应工单和影响测试，再提交；不得用全文件旧后态覆盖前批新改动。

基线 blob SHA 用于定位最初源码，不要求每批继续等于原始 SHA。续工依据前批提交、已变更文件和执行回执核对。新提交与已封存产物不匹配时只停相应写入；不得 reset、清数据库或覆写用户改动来获得表面一致。

## 6. 完成状态机

| 层级 | 状态 | 意义 |
|---|---|---|
| 方案 | DESIGNED / PLAN_CHECKED | 内容和引用通过设计自检，不代表代码已经实现 |
| 工单工程 | TODO / IMPLEMENTING / ENGINEERING_PASS / ENGINEERING_BLOCKED | 必须有源码差异、场景断言和日志原件 |
| 批次交付 | BUILD_PENDING / APK_VERIFIED / INSTALL_VERIFIED | 版本、签名和产物哈希分别核验 |
| 真机 | NOT_RUN / RUNNING / PASS / FAIL / INSUFFICIENT | 绑定同一 APK、设备、窗口和场景 |
| 自然质量 | NOT_OBSERVED / OBSERVING / QUALIFIED / NOT_QUALIFIED | 由真实事前登记的新样本决定 |

不得把 PLAN_CHECKED 写成 ENGINEERING_PASS；不得把 APK_VERIFIED 写成 DEVICE_PASS；不得把 DEVICE_PASS 写成 NATURAL_QUALIFIED。源可读、检查已执行、方法可调用、记录数增加，都不是相应业务通过的充分条件。

B12 汇总全批，不追认缺少回执的前批。若真机暂不可连接，继续完成可做的全批工程和 APK 交付，设备与自然状态保留未完成；施工总状态不得称全部实机通过。当前用户任务是交付施工方案，执行台账初始全部 TODO。

## 7. 证据保留与公共仓库

公共仓库只存方案、源码、无密钥构建信息、报告及验证产物。不存私钥、签名口令、授权令牌或 API 凭据值。需要同签名覆盖安装时复用原合法签名环境，不能生成另一把密钥假装可覆盖。

输入报告原包按 SHA256 封存；本方案证据摘要只是定位辅助。原包没有原始 SQLite/模型二进制，不宣称可以在此重演全部数值。实验 fixture 的输入与结果标识为 ENGINEERING_FIXTURE，禁止进入自然成绩。
