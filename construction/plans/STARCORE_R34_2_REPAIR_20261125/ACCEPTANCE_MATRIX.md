# 新增57项验收合同

这些是本轮方案必须实现的验收合同，不是已经运行的测试。原152条用例逐条保留于inheritance；同名旧测试不自动覆盖本轮新增断言。DEVICE与NATURAL须额外取得真实设备/未来原件。

## R342-001-A｜R342-001｜ENGINEERING

- 正场景：152已变更文件、旧122同名文件卡、未提交用户修改同时存在。
- 反场景：旧整文件覆盖、错源码祖先或漏一条旧工单时拒绝签收。
- 拟施工测试入口：`tools/r342/test_b01_contracts.py#test_r342_001`；当前未实现/未执行。

断言：

- 新方案base==安装源码commit且所有文件卡当前blob可复核
- 原62工单/152入口都有唯一承接
- 保留清单前后哈希/行为有差分与必要回归
- 没有覆盖未授权的应用/签名文件

## R342-002-A｜R342-002｜ENGINEERING

- 正场景：使用同一真实152后代源码，构建R34.2候选并通过原子版本推进、身份计算和封包。
- 反场景：错计划hash、旧APK、错签名、半写版本四文件、错误series及旧工作流伪装新构建均被拒。
- 拟施工测试入口：`tools/r342/test_b01_contracts.py#test_r342_002`；当前未实现/未执行。

断言：

- 旧系列fixtures仍通过，新R34.2有效绑定通过，错根/错祖先/错封印拒绝
- 干净source编译产物嵌入当前source/plan/APK一致
- 包名/证书相同，versionCode大于现场和发布记录
- 首批能独立构建验收，后批不反向充当前置

## R342-003-A｜R342-003｜ENGINEERING

- 正场景：逐层包装ClassifiedFailure，分别注入资源、依赖、取消、确定BUG、提交未知。
- 反场景：首个字符串token相同而typed原因不同，不得全部计BUG；缺完整原件不得伪装已定位。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B02ContractTest.java#test_r342_003`；当前未实现/未执行。

断言：

- 同一 typed exception 经不同包装仍得到同一类别；schema pending 不消耗 BUG 连续计数。
- 实际 BUG 必须仍断路；报告能回溯每次保存失败的真实内部原因。

## R342-004-A｜R342-004｜ENGINEERING

- 正场景：在主服务进程制造pending+admitting达到24且有研究/history/deferred待写。
- 反场景：故障归档写失败或回读失败时不能出队；研究不得绕过保护，合法最小suffix不被误判同一路径。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B02ContractTest.java#test_r342_004`；当前未实现/未执行。

断言：

- 压力注入令队列超过 24 后排空可恢复到阈值以下，原件计数=排队计数且无静默 capacityLoss。
- 连续开奖下主链冻结按期完成，后台研究仍能获得有界服务；无第二写连接绕过总控。
- 杀进程/COMMIT_UNKNOWN/磁盘满/迁移等待后能够识别原事件并继续，不能靠丢弃错误通过。

## R342-005-A｜R342-005｜ENGINEERING

- 正场景：主PID有积压、诊断PID静态队列为空，两者同一版本但不同processEpoch。
- 反场景：诊断0、陈旧投影或跨boot单调时间不能验为主进程已恢复。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B02ContractTest.java#test_r342_005`；当前未实现/未执行。

断言：

- 主 PID 人为积压时报告不能显示“已健康”；主进程未在线时明确 UNKNOWN。
- 同一主 PID 排空前后计数和 receipt 可对账，报告投影与原件 hash 一致。

## R342-006-A｜R342-006｜ENGINEERING

- 正场景：索引、已核验、缺原件及等待核验故障分别存在，分片校验后进程重启。
- 反场景：只有索引或未readback的对象不能标verified/允许GC；不能等所有历史原件检完才恢复当期有界排空。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B02ContractTest.java#test_r342_006`；当前未实现/未执行。

断言：

- 7719的四状态相加可对账；verified失败保留原件并显示原因。
- 完整审计重算能验证指定原件，不以索引存在代替字节验证。

## R342-007-A｜R342-007｜ENGINEERING

- 正场景：CHECKPOINT_PREPARE通过后在PRESERVE各phase注入异常，再修依赖并启动原owner恢复。
- 反场景：三次同因确定BUG阻断热循环；依赖恢复须有界探测；缺CAS资产不可改为健康或全局拖死无关资产。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B03ContractTest.java#test_r342_007`；当前未实现/未执行。

断言：

- 真实旧版本数据库升级/进程重启后出现 CHECKPOINT_PRESERVE COMMITTED 和 SESSION_REGISTER/研究 slice 实际进度。
- 同失败重复三次仍停车并显示精确原因；修复依赖后仅重启一次并推进。
- 被隔离资产保持旧身份/原件，其他研究任务与现有预测主链继续；提交不确定时不重复写。
- 同一安装 SHA 下每个启动阶段都取得 END/VERIFIED，当前会话在冷启动和进程恢复后各产生有内容的提交回执。
- 注入进程中断或 writer 忙时，游标可恢复且旧原件哈希不变；旧 owner 的 fence 不再可写。

## R342-008-A｜R342-008｜ENGINEERING

- 正场景：服务attempt心跳持续但研究cursor/receipt不变，随后出现一次真实提交。
- 反场景：空维护、同cursor重复写、过期健康及仅租约更新不能增加模型训练进度。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B03ContractTest.java#test_r342_008`；当前未实现/未执行。

断言：

- 模拟owner停住而控制循环存活，界面必须显示研究阻断并给精确原因。
- 恢复成功必须有新cursor/receipt进度，不能仅更新计时戳。

## R342-009-A｜R342-009｜ENGINEERING

- 正场景：152数据库副本缺三个目标表、部分索引已存在、迁移中断再重入。
- 反场景：一个domain READY不允许其它缺表consumer运行；查询异常不得转0；已存在DDL不重复破坏数据。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B03ContractTest.java#test_r342_009`；当前未实现/未执行。

断言：

- 三表契约可验证且生产一次真实写/读回路；只建表不计能力验收。
- 中断升级/重启可续迁移，旧功能回归无丢失。
- 三源从 SOURCE_UNAVAILABLE 变为实际读成功；插入回滚、幂等迁移、旧库升级后 trigger/column 合约一致。
- 真实一次优化器更新能联查 optimizer_step→publication；缺表回归能阻断工作且不会丢旧模型。

## R342-010-A｜R342-010｜ENGINEERING

- 正场景：维护、IMPORT、训练、冻结、研究动作任务同时有eligible输入，注入writer退避。
- 反场景：空维护不能替代TRAIN/FREEZE完成；未提交cursor不得推进；维护不得永久挤占已有ready训练。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B03ContractTest.java#test_r342_010`；当前未实现/未执行。

断言：

- 注入真实 ready TRAIN、POST、RESEARCH、MATRIX、EVOLVE、IMPORT，按实际 device budget 都能取得提交，主链 deadline 不恶化。
- 一个任务中途杀进程恢复无重复数值训练/结算；故障产生可定位终态或下一次触发。

## R342-011-A｜R342-011｜ENGINEERING

- 正场景：旧版本研究service session存在、当前版本开始真实slice并有等待任务。
- 反场景：跨版本session、bookkeeping generation或无数值变化任务不算当前训练与策略代数。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B03ContractTest.java#test_r342_011`；当前未实现/未执行。

断言：

- 同机当前版本进行包含重启/屏幕熄灭/主链高压的连续观测，每个eligible家族都有可量化服务而非仅心跳。
- 空wait循环不增加数值训练/有效子代/质量提升指标；过去版本数据仍可读但不混入当前验收。

## R342-012-A｜R342-012｜ENGINEERING

- 正场景：逐期重放HISTORY_BOUND/PIT_FROZEN后无canonical、suffix返回0、跨日目标及请求过期。
- 反场景：PIT输入冻结不得当决策冻结；开奖后补数据只能服务未来，不补造旧期predraw；普通history与suffix分别定位。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B04ContractTest.java#test_r342_012`；当前未实现/未执行。

断言：

- 注入历史缺页、writer背压与重启后，下一可行期能够重新生成开奖前 canonical；旧已开奖期保持真实缺失。
- 每个 eligible target 必有完整成功链或可追溯 terminal，不能靠减少 denominator 提高覆盖率。

## R342-013-A｜R342-013｜ENGINEERING

- 正场景：30个合法机会仅1个predraw，另有5条过老WAIT_DRAW及真实新未开奖目标。
- 反场景：既有义务lag0不能把覆盖验绿；旧缺开奖不得标正常未来等待或补造标签。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B04ContractTest.java#test_r342_013`；当前未实现/未执行。

断言：

- 有 29/30 缺失的样本必须显示覆盖失败，即使已有预测全部结算。
- 统计可从同一范围期列表重算；跨版本、未来期、未开放期明确分组且有理由，不从完整机会清单抹掉。
- 5条旧WAIT_DRAW逐条有target、冻结原件、draw来源可用性、恢复或隔离结论；不能在没有开奖证据时置settled/hit，亦不能把缺源当普通下一期开奖等待。

## R342-014-A｜R342-014｜ENGINEERING

- 正场景：原始frozen snapshot持久后core已完成、deferred尚缺100/1200面，注入租约到期与重启。
- 反场景：不得从新模型重算旧预测；重复消费不重复行；未全量物化不得标full evidence完成。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B04ContractTest.java#test_r342_014`；当前未实现/未执行。

断言：

- 现有1632作业逐状态可解释，成功排空的每条有100/1200对账与最终receipt；新增速率下无长期无界堆积。
- 杀进程、重试和重复回读不重复动作行、不改变冻结原件；实时主链仍满足时限。

## R342-015-A｜R342-015｜DEVICE

- 正场景：自然单请求采集，分别注入HTTP慢、解析慢、后处理慢、网络断开恢复。
- 反场景：不能突发补444个漏时隙；失败与退避需计真实分母，单调deadline不因墙钟变化漂移。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B05ContractTest.java#test_r342_015`；当前未实现/未执行。

断言：

- 连续样本按同一serviceEpoch输出 targetSlots/wakes/missed与每段P95/P99；不存在不能解释的长阻塞。
- 网络失败时不中断本地下一期主链，恢复后请求不会叠加或瞬时追发。

## R342-016-A｜R342-016｜DEVICE

- 正场景：包含成功、失败、排队、无canonical的完整request序列；对比仅1有效计时样本窗口。
- 反场景：一个快样本不能签整体P95/P99达标；未测项不补0，不把lifetime与current差值称优化收益。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B05ContractTest.java#test_r342_016`；当前未实现/未执行。

断言：

- 报告不可因 sample=1 宣布整链验收通过；所有期可重算端到端/各段时延。
- 优化后证明没有把慢工作移出计时范围、删除失败样本或提前回报成功。

## R342-017-A｜R342-017｜DEVICE

- 正场景：同设备同交互脚本跑长列表/切页/后台解析并采frame与主线程调度证据。
- 反场景：不能用某一空闲stack否定掉帧，也不能未经关联就把所有后台DB读归因主线程卡顿。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B05ContractTest.java#test_r342_017`；当前未实现/未执行。

断言：

- 关键页面滚动/切换/更新与报告并行运行完成既有功能回归，并报告帧P95/P99/最大值与慢帧数量。
- 无主线程写库；缓存命中/失效不改变预测或解释内容。

## R342-018-A｜R342-018｜DEVICE

- 正场景：目标设备24h、72h锁屏/息屏/网络切换/更新重启连续运行，覆盖完整机会序列。
- 反场景：权限授予、通知存在或host通过不能代替真机持续结果；force-stop后不得绕过用户停止。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B05ContractTest.java#test_r342_018`；当前未实现/未执行。

断言：

- 真实设备连续证据与issue覆盖完整，不能用声明权限、线程存在或编译通过替代。
- 每种恢复均无重复owner、重复冻结/结算/学习；明确记录因系统或用户导致的停机。

## R342-019-A｜R342-019｜DEVICE

- 正场景：混合旧新版本crash/ANR/stall、进程重启、10m窗口与截断原件。
- 反场景：旧故障不得算当前版本；缺事件原件不得断言当前0故障；窗口边界与时间回拨可复算。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B05ContractTest.java#test_r342_019`；当前未实现/未执行。

断言：

- 跨版本汇总能一键分离；数据缺失显示unknown。
- 当前版新故障及历史回归可定位到具体运行身份。

## R342-020-A｜R342-020｜ENGINEERING

- 正场景：原子COMPLETE、分子900/4200，分别请求不依赖分子和确需某分子/教师的家族。
- 反场景：前者应合法开始，后者只等exact依赖；缺hash、伪complete或未验证prefix不得放行。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B06ContractTest.java#test_r342_020`；当前未实现/未执行。

断言：

- 故意让分子目录停在 900 行时，一个不依赖该目录的合法家族产生训练任务；需要分子的家族仍保持明确 WAIT。
- 恢复扫描后恰好物化 4200 个去重身份，SHA 不一致必须隔离；重启不丢 offset、不重复出生。

## R342-021-A｜R342-021｜ENGINEERING

- 正场景：5000条内有37处gap与872期尾段，为不同窗口/稀疏输入家族登记输入。
- 反场景：不能拼接伪连续、倒填first_seen或扩大draws热表；相同来源重复回放不能增加独立样本。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B06ContractTest.java#test_r342_021`；当前未实现/未执行。

断言：

- 构造一个确切缺口，所有要求连续窗口的家族拒绝跨越；修复到该协议长度后自动恢复。
- 回填仅增加历史训练可用样本，不增加正式独立未来 n；数据时点审计可复现。

## R342-022-A｜R342-022｜ENGINEERING

- 正场景：候选出生一次事务登记不可变输入/协议/训练任务，分片数值更新、checkpoint恢复。
- 反场景：缺任一输入身份不能半注册成功；训练崩溃不得重复步，冻结模型不可被在线更新覆盖。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B06ContractTest.java#test_r342_022`；当前未实现/未执行。

断言：

- 每个测试家族有连续可联查的 input→protocol→TRAIN_CANDIDATE→数值 checkpoint→development receipt→artifact identity。
- 同份输入在进程中断后恰好继续，最终模型 hash/样本计数一致；训练不会读取未来/验证标签用于优化。

## R342-023-A｜R342-023｜ENGINEERING

- 正场景：合法dataset准备在锁外，writer退避后重新提交，exact消费者被唤醒。
- 反场景：旧ABI、缺pin、缺source/hash和仅dataset计数均不能说明当前可消费；不绕过backpressure。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B06ContractTest.java#test_r342_023`；当前未实现/未执行。

断言：

- 同一完整dataset publication下，回放score和evolution能够分别推进；真实writer压力时主链优先且研究最大等待可测。
- 回放样本、角色、gap与pin可复核；异常只有受影响dataset等待，其余可用任务继续。
- 每方向输入条件有真实owner、immutable输入hash和下一次触发；B11仅可读取这些已提交事实，不反向阻塞输入生产。

## R342-024-A｜R342-024｜ENGINEERING

- 正场景：保留50旧候选和12专家实验，按旧ABI回放；创建一个全新身份的冷重生候选。
- 反场景：不得删除旧原件、把旧model_id换名当出生，或继承旧证书/未来样本作为新资格。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java#test_r342_024`；当前未实现/未执行。

断言：

- 逐一列出 50 候选和 12 专家迁移终态、局部等待原因或合法子身份，不出现无解释沉默。
- 冷启子代实际权重/状态变化、训练 receipt 与下游消费可联查；断电恢复不重复建立同一子身份。

## R342-025-A｜R342-025｜ENGINEERING

- 正场景：跑完固定40变体后，用不同结构操作产生可执行候选并评价。
- 反场景：换名、等价AST或同参数排列不得计结构新颖；无预算的无限枚举不得挤占实时任务。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java#test_r342_025`；当前未实现/未执行。

断言：

- 基础游标达到 40 后，适用且有预算时探索仍能产生结构不同、可执行、去重且真实训练的子代；无新颖结构时诚实记 NO_NOVEL_CHILD。
- 变异/融合/新规则分别有独立证据和可复现实验；禁止把 bookkeeping generation、尝试次数当成改善代数。

## R342-026-A｜R342-026｜ENGINEERING

- 正场景：将真实重复失败归纳为版本化typed rule，原owner提交后经消费账生成后续新候选。
- 反场景：只登记规则名、只发图表、不消费、泄漏未来、循环依赖、重复hash或新owner旁路均拒。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java#test_r342_026`；当前未实现/未执行。

断言：

- 原两个发现策略各有训练/回放原件与贡献证据
- 新规则保留真实共同运算骨架，规则身份与ABI一致
- 迁移段2个产物结构不同且分别首评
- rule禁用对照改变实际派发/输出，非只改标签
- 重启后同发布原件继续消费且不重复出生
- 失败/等价/无输入不增加新思路族计数

## R342-027-A｜R342-027｜ENGINEERING

- 正场景：不同机制各取得合法输入并实际生成/训练，机制不可用时保留具体依赖。
- 反场景：一个RECENCY_BLEND的多个别名不能代表多个机制；不可为凑家族数复制相同公式。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java#test_r342_027`；当前未实现/未执行。

断言：

- 机制覆盖能由 AST/graph 和权重回执复核，不能只用不同名字/参数。
- 旧 RECENCY_BLEND 的25次连续数值更新与新机制状态并存且不被删除。

## R342-028-A｜R342-028｜ENGINEERING

- 正场景：在25350规格中选有明确输入/数值定义的atom，物化并做相同协议消融。
- 反场景：规格条数不等于可用特征，未绑定ABI/来源或没有边际贡献证据不能宣传有效。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java#test_r342_028`；当前未实现/未执行。

断言：

- 选定代表原子从spec→admission→binding→实际特征值→训练→消融receipt可以完整复放。
- 错误类型、未来依赖、重复行为原子被精确阻断；纯目录新增不增加有效能力计数。

## R342-029-A｜R342-029｜ENGINEERING

- 正场景：从已核验atom组合有类型的molecule并物化、发布、消费。
- 反场景：扫描到900/4200不能计900可用特征；无数值/单位兼容或缺依赖hash禁止发布。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java#test_r342_029`；当前未实现/未执行。

断言：

- 合法双组件产生实际特征/预测并进入候选训练，非法组件有确定reason；R07消费exact子代与两父件。
- 扫描中断后恢复hash/offset一致；不同分子不会共享一份不相容的训练state。

## R342-030-A｜R342-030｜ENGINEERING

- 正场景：教师具备合法state/ABI的F23和缺教师F23并行验证，其他81能力不受影响。
- 反场景：静态implemented不等于可调用；缺教师不能造teacher值或全局停止其他无关家族。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B07ContractTest.java#test_r342_030`；当前未实现/未执行。

断言：

- 错误教师、ABI或state hash被拒绝；正确原件能形成F23实际实训/消费回执。
- M23分别展示静态可调用、真实产出、消费与有效性，不能仅把分子改成82宣布全部完成。

## R342-031-A｜R342-031｜ENGINEERING

- 正场景：保留既有2150样本/790stage/19发布，建立新optimizer-step→checkpoint→forecast原件链。
- 反场景：stage数、发布数和重复同hash不得当梯度步；缺优化器状态或不恢复不得签训练。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_031`；当前未实现/未执行。

断言：

- 一个实际训练片可复验样本、optimizer step、数值变化及published完整模型；10位置surface由同一完成版本产生。
- 同 target 的exact consumer receipt指向该模型；杀进程不暴露部分更新模型，正式未来标签不进优化器。

## R342-032-A｜R342-032｜ENGINEERING

- 正场景：原R952更新链从旧游标恢复，逐期合法标签产生numeric_updates递增及新payload。
- 反场景：25历史更新不能证明当前持续；重复outbox不能重复训练；数值变化不可自动升格独立未来质量。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_032`；当前未实现/未执行。

断言：

- 新的合法实时时段中，eligible机会各有delivered/blocked-with-reason终态，训练水位在商定设备SLO内追随数据；不只看最终cursor。
- 重放25条已完成delivery不改变numeric_updates；一条新权威settlement只增加一次。

## R342-033-A｜R342-033｜MIXED_WITH_EXPLICIT_NATURAL

- 正场景：TRAIN_STREAM_V2在线任务与正式冻结未来的同目标样本各有独立role。
- 反场景：历史25predraw不得迁移角色抵充正式未来；同candidate更新后新模型不能沿用冻结样本。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_033`；当前未实现/未执行。

断言：

- 自然新发生的一段未来中exact freeze-before-open、settle-after-open可证，人工重放历史不会增加formal n。
- 改变冻结参数或混用训练label会被拒绝；无足够证据时资格始终未通过。

## R342-034-A｜R342-034｜MIXED_WITH_EXPLICIT_NATURAL

- 正场景：父子、空基线、预注册窗口同目标配对评价，缺一方样本/重复样本分别处理。
- 反场景：不对齐目标、不完整窗口、选后报告或以ACT行数替代独立期数不能通过。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_034`；当前未实现/未执行。

断言：

- 可从原始父子预测和结算重算pair结果，区间/分母与UI一致。
- 负向对照、随机或标签打乱模型不能靠选择偏差通过；没有改善时记录真实拒绝。

## R342-035-A｜R342-035｜ENGINEERING

- 正场景：20矩阵各按类型实现生产、原件校验、消费，尺寸/目标/单位有明确契约。
- 反场景：菜单注册、空矩阵和其它R952 snapshot不能替代这20矩阵的实际数值与消费证明。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_035`；当前未实现/未执行。

断言：

- 对每种有合法输入的矩阵，生产与至少一个合同指定consumer按hash联查；缺输入的kind显示specific WAIT。
- 错误维度、版本、ABI、role与未知支持域不会被消费；无改变的读图不计算法贡献。

## R342-036-A｜R342-036｜ENGINEERING

- 正场景：让projection处于退避期间，保留它实际依赖的17类输入并验证恢复。
- 反场景：已pin对象不能先GC；不能通过无限保留所有数据修TTL，超预算须明确局部退避。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_036`；当前未实现/未执行。

断言：

- 逐17条归因闭环；新测试中可完成投影的输入不因无界等待过期，确实过期者有精确terminal。
- GC/pin与投影提交的竞态可重放，无悬挂引用或永不释放pin。

## R342-037-A｜R342-037｜ENGINEERING

- 正场景：保留1041长期发布和717冠军概率，使用exact发布快照并能恢复同一预测。
- 反场景：generation=1043不能叫1043代；过期/未消费长摘要不得伪装当前主链贡献。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_037`；当前未实现/未执行。

断言：

- 至少一个实际子代的训练输入和冻结原件精确联到long snapshot hash；重启与epoch切换不串用来源。
- 1041旧发布保持可追溯，任何新增“进化成功”必须另有真实generation/lineage与比较证据。

## R342-038-A｜R342-038｜ENGINEERING

- 正场景：测量对象由旧版本切到新版本，记录invalidate、recompute、publish、consume各一次。
- 反场景：517失效事件不等于517个新结论；旧key测量不能给新模型背书。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_038`；当前未实现/未执行。

断言：

- 注入无关状态变化不会使无关方向重算；改变真实输入则必须失效并生成新run。
- 至少一份稳定输入能从计算完成到consumer提交，不被循环invalidation永久饿死。
- 在B08生产入口已形成有数值支持的measure及typed source；图表未打开时也能持续生产。B11只验证/展示此原件。

## R342-039-A｜R342-039｜ENGINEERING

- 正场景：同canonical结算失败形成可证伪假设，原Director登记动作并生产孩子与复验结果。
- 反场景：只写诊断词/研究待办不算研究完成；错误归因不得直接改当期预测或训练标签。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_039`；当前未实现/未执行。

断言：

- 一条真实失败簇从原件到假设、独立子代实训、配对验证、action_result_binding可完整重放。
- 证据不足或随机残差有明确NO_ACTION，而非强制创造“提升”或伪造九层故障。
- 九层报告原状态分别为3个BLOCKED和6个UNAVAILABLE；修复后每层给出exact source identity及证据，缺件保留UNKNOWN，不把诊断confidence当算法因果置信。
- 不开页面也可由原研究owner消费measure并改变预算/子代/回放；禁用同source对照影响真实行为，before/after和结果回流完整。

## R342-040-A｜R342-040｜ENGINEERING

- 正场景：父代保留，合法变异子代完成独立配对后按原合同选择，负改进样本也保存。
- 反场景：50birth记录、generation增长、没有配对结果不得称代代更强；差子代不强行替换父代。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_040`；当前未实现/未执行。

断言：

- 出现至少一条完整合法父→子 lineage及真实数值/配对原件，成功和失败实验均正确记录。
- 故意制作差子代不能替代成熟冠军；没有质量证据时系统显示未证明而非自动更强。

## R342-041-A｜R342-041｜ENGINEERING

- 正场景：两个父模型在同未来窗口冻结，融合后比较贡献、相关失败与资源成本。
- 反场景：简单平均或不同期拼接不能等同进化通过；弱父、证书不匹配与零贡献融合不能准入。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_041`；当前未实现/未执行。

断言：

- 合法同目标双父件产生可执行融合child；时间/目标不匹配必须拒绝。
- exact lineage、两父hash、融合权重/graph与共同目标配对原件齐全；无提升则不晋级。

## R342-042-A｜R342-042｜ENGINEERING

- 正场景：MultiHorizon固定960连续协议，在尾段872时记录等待，满足960后按新可用时刻运行。
- 反场景：不得改成872沿用旧协议验收；四条NOT_RUN不能覆盖所有机会，遗漏机会需可见。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B08ContractTest.java#test_r342_042`；当前未实现/未执行。

断言：

- 一个完整新时段所有计划机会均有观测终态，输入合法时产生开奖前forecast与之后label。
- 缺历史时不造输出；horizon重叠及研究-only状态不侵入正式资格和REAL。

## R342-043-A｜R342-043｜MIXED_WITH_EXPLICIT_NATURAL

- 正场景：按四资格域分别拟合/验证校准，冻结漂移调优产物并做可恢复正反例。
- 反场景：共用120门槛、训练/验证重叠、旧模型校准给新模型使用、缺冻结漂移artifact均拒。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B09ContractTest.java#test_r342_043`；当前未实现/未执行。

断言：

- 校准artifact从输入到consumer可复核，改变版本或支持域不匹配不得使用；校准无改善可不采用。
- 无policy测试保持安全fallback；有合法policy后决策确由该hash产生，R13/R15可联查。

## R342-044-A｜R342-044｜MIXED_WITH_EXPLICIT_NATURAL

- 正场景：构造精确合格与逐字段无效的四域证书fixture，驱动统一选择器到beforeInsert并记使用/拒绝原因。
- 反场景：不得global PROVEN翻真、旧证书继承、weight越权、用户开关关闭后REAL；正fixture不得注入生产资格。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B09ContractTest.java#test_r342_044`；当前未实现/未执行。

断言：

- 在隔离/影子验收中完整重放合法证书的主链接入与结算；不合法、过期、ABI不符、用户未启用时保持拒绝。
- 真实运行只有预先协议达标后才可进入允许状态，报告可逐期解释每个gate。

## R342-045-A｜R342-045｜ENGINEERING

- 正场景：按类测出数据库/WAL/快照/备份/CAS等物理字节，按预算有界回收可重建对象。
- 反场景：5000条热历史不能代指总磁盘有界；禁止删除未验证依赖或把磁盘数字当内存。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B10ContractTest.java#test_r342_045`；当前未实现/未执行。

断言：

- 10.97GB逐owner可解释，重复报告无长期无引用快照增长。
- 可恢复性验证后才回收；回收receipt与文件/数据库实际bytes一致，用户功能历史仍完整。

## R342-046-A｜R342-046｜ENGINEERING

- 正场景：有活跃pin、缺556原件、18未验证、574child集合未知的图进行引用闭包遍历。
- 反场景：未知child关系不能假设无子项；受影响资产保持隔离，不能破坏未受影响旧模型。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B10ContractTest.java#test_r342_046`；当前未实现/未执行。

断言：

- 677对象逐一有明确missing/verified/corrupt/unreadable状态及受影响asset，unknown不静默转0。
- 同源恢复给出字节等同与运行消费receipt；新训练显示新身份；隔离不阻断全局服务。

## R342-047-A｜R342-047｜ENGINEERING

- 正场景：归档、持久化、hash/readback、引用释放、物理reclaim按不同phase中断恢复。
- 反场景：archived不等于freed；WAL checkpoint不等于VACUUM；前台长事务VACUUM不得进入实时通道。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B10ContractTest.java#test_r342_047`；当前未实现/未执行。

断言：

- 任一“释放成功”必须附真实before/after bytes+receipt，未知用null。
- 断电/失败恢复后无孤儿删除或数据丢失，主链升级回归通过。

## R342-048-A｜R342-048｜ENGINEERING

- 正场景：对象数超过1024、同一对象跨页计费，分片重启后继续完整巡查。
- 反场景：截断1024名不得说全量；逻辑row_count不能当物理字节，重复页不可重复收费。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B10ContractTest.java#test_r342_048`；当前未实现/未执行。

断言：

- 完整对象bytes与DB页数有可解释差异；不完整时明确unknown而非排名误导。
- 清理建议由完整证据支持，不误删索引/历史记录。

## R342-049-A｜R342-049｜ENGINEERING

- 正场景：同一install/issue在多个版本先失败后修复再回归失败，追加多条观察。
- 反场景：INSERT OR IGNORE首条不能吞后续退化；两个绑定工单不能代表全部62/57问题已验收。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java#test_r342_049`；当前未实现/未执行。

断言：

- 62原工单和新增工单都有证据来源或明确NOT_OBSERVED，不能空白默认PASS
- 同工单首次成功→后续失败→恢复三条事件可追溯
- 重复receipt不重复计入，scope不同不串接
- 报告写失败不回滚已完成核心业务
- 发布成功但无反馈消费时B07-04仍未通过

## R342-050-A｜R342-050｜ENGINEERING

- 正场景：18方向分别消费有来源数值研究产物，打开图表只读并能定位所用模型/窗口。
- 反场景：图表可渲染或目录存在不算算法运行；无原件显示未知，不能造好看曲线代替训练。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java#test_r342_050`；当前未实现/未执行。

断言：

- 18方向各展示前批的PRODUCED+合法消费、FORMAL_READ_ONLY或specific WAIT/UNKNOWN原状态；不能把渲染成功等价为算法已完成。
- B11重放前批禁用对照的原件，核实所显示行为差异与真实source/action/child一致；不重新训练或为无数据方向填造曲线。
- 打开、刷新、分页、取消页面时，训练任务数、研究动作数和正式冻结/资格表均不因展示操作新增；后台原研究owner不依赖UI访问。

## R342-051-A｜R342-051｜ENGINEERING

- 正场景：用旧报道600/840/1200阶段和当前四域policy identity生成展示。
- 反场景：不得把静态12/20/30%当当前授权上限；不能为迎合展示而提高真实权重。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java#test_r342_051`；当前未实现/未执行。

断言：

- 报告显示cap等于所绑定消费者的精确合同值
- 旧协议不成为当前REAL授权
- 缺policy时显示UNKNOWN而不使用常量替补
- 相同快照两种导出说明一致

## R342-052-A｜R342-052｜ENGINEERING

- 正场景：分别对九层输入正确/缺失/陈旧/关键词命中，绑定该canonical冻结bundle运行诊断。
- 反场景：静态PROVEN、关键词FAIL和confidence1不得当模型因果结论；旧期禁止读取当前可变bundle。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java#test_r342_052`；当前未实现/未执行。

断言：

- 同一个目标/快照报告的模型身份与canonical一致，换模型不得沿用前模型的PROVEN/根因。
- 九层均有精确原件或明确缺件状态；关键词命中不能自动签数值故障。
- 通过/未通过/未知/工程种子各自正确显示；诊断不改决策不回写当期预测。
- NumberQ、Ranker和校准的训练/冻结/消费反例回归纳入B06/B08/B09既有模型合同，B11只消费已形成事实。

## R342-053-A｜R342-053｜ENGINEERING

- 正场景：96检查原PASS/FAIL/WARN/NOT_PROVEN/OBSERVED/NOT_APPLICABLE及14子Finding逐项输出。
- 反场景：96 executed不等于96 passed；NOT_APPLICABLE流程描述不得覆盖真实源码时序。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java#test_r342_053`；当前未实现/未执行。

断言：

- 96逐项覆盖账能从原报告重算；PASS仅用于其明确范围。
- 更换健康结论不能改变原始检测数据与历史报告。

## R342-054-A｜R342-054｜ENGINEERING

- 正场景：44指标保留定义/范围/分母/角色，82handler分别有或没有生产收据。
- 反场景：有样本未验证、81静态callable、空样本唯一性不得提升为已运行有效或自然通过。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java#test_r342_054`；当前未实现/未执行。

断言：

- 44指标逐项能追到可复算原件；当前缺项不会被汇总PASS遮盖。
- 重复/重启/异常场景下主链一致性经实际事件验证。

## R342-055-A｜R342-055｜ENGINEERING

- 正场景：混合13:46运行/快照、13:58诊断、14:10磁盘和7/30对1/30不同过滤合同。
- 反场景：不同scope不可直接相减/并表；无法对齐标NOT_COMPARABLE，不选择较好值验收。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B11ContractTest.java#test_r342_055`；当前未实现/未执行。

断言：

- 使用同issue异process/异release数据能自动识别不可比较；无无范围的0或累计P95。
- 审阅者能追溯每个结论对应哪个生产者与时间窗。

## R342-056-A｜R342-056｜MIXED_WITH_EXPLICIT_NATURAL

- 正场景：真实工程测试原件、同域未来样本与未满足质量域并存，冠军子代一优一劣。
- 反场景：工程PASS不签自然质量；不保证随机预测高胜或每代提升；负结果可见且保留成熟冠军。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B12ContractTest.java#test_r342_056`；当前未实现/未执行。

断言：

- 所有本卡都有实现/单元与集成验证/设备原件/剩余观察四级状态，不以文档完成冒充软件完工。
- 不存在无资格REAL、未来泄漏、抽样挑选、重复n或为了PASS改低门槛；真实不达标保持NOT_PROVEN。

## R342-057-A｜R342-057｜ENGINEERING

- 正场景：对DIAGNOSTIC及明确范围RAW包分别验证hash、数据库一致副本、APK和相关模型可重放性。
- 反场景：没有SQLite/APK/模型原字节的包不能声称已独立数值重算或APK运行验收。
- 拟施工测试入口：`app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B12ContractTest.java#test_r342_057`；当前未实现/未执行。

断言：

- 本报告能区分已证现象/已核源码/待复现运行因果，未做真机测试不写通过。
- RAW复现材料齐备后可由独立人按同脚本重现关键断点。
