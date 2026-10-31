R30 基线 bb607cf（CP06，已验收）；R29 CP07–CP16 不并入；每卡状态表（TODO/DOING/DONE/NOT_RUN/BLOCKED）。

# STARCORE R30 施工账本

- 施工分支：`construction/r30-20261031`（父提交 bb607cf5ab69f7286f859e43f87ada85a5bc456d）
- 方案：`construction/plans/STARCORE_R30_FINAL_20261031/`（只读副本）
- 设备侧指标（真机时延、长测、自然质量）：未执行即 NOT_RUN，不写 PASS。
- 打包：按用户要求中间批次不再打包，全部批次完成后统一出两证书 APK。B00 已出包：R13 官方 r30-official-577310001-4bc5ee4e1d05（模拟器覆盖安装+新装冷启动+R30FoldBackInstrumentedTest 通过，run 41169766223）；R27 证书 r30-B00-R27-4bc5ee4e1d05（run 41169784335）。

|卡号|批次|标题|状态|提交|CI|备注|
|---|---|---|---|---|---|---|
|R30-B00-01|B00|从CP06建R30分支并锁定基线|DONE|7157660adec1|CI 自 B00-04 起|baseline_lock mismatches=0；rebind 产物 release/strategy_su1_reviewed_inputs.json 按 docs/02§2 属开放文件（ownership_check 原 OPEN 表漏列，B00-04 补登）|
|R30-B00-02|B00|封存R29 B01分支，登记可复用件|DONE|aa28bc2f60a4|—|标签本地已建；远端 tag 推送被会话代理 403 拒绝，待所有者补推（见 R29_ARCHIVE.md）|
|R30-B00-03|B00|回收R29后期包遗留的Root触发器与独立研究库|DONE|17dc6ef7bb35|待 r30.yml（B00-04 起）|本地全量编译通过；SQL 合同 19/19（含 T04/T05/T06）；R30FoldBackInstrumentedTest 需模拟器（CI connectedDebugAndroidTest）|
|R30-B00-04|B00|装施工护栏工具与R30 CI入口|DONE|f7044a7c2eba|—|工具按方案原样复制，3 处必要修正（见下方偏差记录 D1–D3）|
|R30-B00-05|B00|复核R29已签收的B00三卡并统一指标测量口径|DONE|e162b175f096|run 41163664048 绿|44 项均有 measurementSource；空源/零样本→INSUFFICIENT、值 null；legacy_count requirements=1832 fileCards=299；子卡 R30-B00-05-a 登记测试文件|
|R30-B01-01|B01|唯一写锁：等待/持有时长账本|DONE|a01e58ed15f4|—|HOST 6/6；DatabaseCoordinator 仅补丁所示改动；真机/模拟器持锁数据 NOT_RUN|
|R30-B01-02|B01|账本落库与报告出数|DONE|4c594e1122b5|—|SQL T02 4/4；报告第3章两表（writerHoldR30）；“运行10分钟≥9窗口”需设备 NOT_RUN|
|R30-B01-03|B01|切片写入工具与Android适配器|DONE|1fb9922d0946|—|HOST 6/6（含中途失败回滚续做不重复）；Android 杀进程续做 NOT_RUN|
|R30-B01-04|B01|按账本切片前五名后台长事务|DONE|a7d151c6af8c|—|HOST：SettleChunkR30 S1–S3×3 种子 OK；R29 qual_host Q1–Q16 OK；DataRetentionGovernorR900 已有界未改（理由见 B01_OFFENDERS.json）；2小时复测 over_hard NOT_RUN|
|R30-B01-05|B01|任务活性判定与报告|DONE|57b8239c294b|—|HOST livenessClassification OK；当前 r26_task 无 pending/last_committed_seq 列 → 全部 UNKNOWN 单列，M12=INSUFFICIENT（如实）|
|R30-B01-06|B01|调度公平与温控核对|DONE|56afcaa37dff|—|温控序列 HOST 6/6；STALLED 提升已接入 claim()，但 r26_task 尚无 pending/last_committed_seq 事实 → 无任务被判 STALLED（不猜测）；“下一轮被调度”的运行验证 NOT_RUN|
|R30-B01-07|B01|重复调度入口清单与收敛|DONE|dcef83a15e12|—|SCHEDULER_OWNERS.json 42 个循环，无“两个活跃写者写同一研究表”；旧研究链连续性/捕获在 R26 运行时让出（与 R27-046 并行观察冲突已记录）；生命周期回归 androidTest NOT_RUN|
|R30-B02-01|B02|每期8点时间线落库|DONE|56e7834b60c9|—|HOST timeline 3/3；SQL T01；VALIDATED/DRAW_COMMITTED 打点在同一事务内的 FetchLedgerR925.observe/canonicalDrawWithinR82（DbStore 为主链红线，保持不变）；30分钟覆盖 NOT_RUN|
|R30-B02-02|B02|抓取单飞与退避节奏|DONE|31ea80f32bae|—|HOST 2/2（16线程单飞、退避序列）；HTTP 超时现值 2500/3500ms 已≤4000 未改；断网恢复真机序列 NOT_RUN|
|R30-B02-03|B02|缺期补抓与幂等结算核验|DONE|b2d781d67d27|—|HOST PendingDrawR30 P1–P4 OK；SettlementEngine 未改（主链红线 R27QualityTest.h15 要求逐字节不变；CP06 重放已不写行并返回已存结果，仅重放哈希文本与首次不同）|
|R30-B02-04|B02|准备在事务外、冻结在短事务内|DONE|7efceca151e8|—|审查：两条冻结路径事务体内均无模型/特征调用（CP06 已符合，未改行为）；prepare_to_frozen 真机 P95 NOT_RUN|
|R30-B02-05|B02|UI与后台延迟|DONE|1ada7bd82310|—|图表已在后台线程读取；每页≤500点；debug StrictMode 已开；全页面违规清扫与首开<1s 需模拟器/真机 NOT_RUN|
|R30-B03-01|B03|PIT守卫接入所有冻结写入|DONE|461b96c50642|—|HOST PitGuard 测试 + 注入反例被拒入账；回放/研究冻结均经守卫；真机 r30_pit_violation NOT_RUN|
|R30-B03-02|B03|真实历史数据治理|DONE|1ec65983f4ae|—|fixture 乱序/重复/矛盾重复/缺期/跨日 通过；热库≤5000 沿用 CP06 保留尾（MAX_LOCAL_HISTORY_SAVE）；回放更早数据只读已核验导入历史，CP12 压缩归档块不回读（记录）|
|R30-B03-03|B03|分割角色与曝光账|DOING|8459eb55e1b7|—|已登记 B03-03-b：补确认账写前持久化及启动恢复；原子评分不读 H_CONFIRM；进化循环实际接线待修|
|R30-B03-04|B03|增量特征与回放|DONE|58b24f909afe|41175814154|实际 SQLite 缓存与源竞态回归通过；CI 通过；设备性能未测|
|R30-B03-05|B03|训练游标与可恢复状态|DONE|b4c944c|41175814154|HOST 恢复一致、防重复与 Q15 原子回滚通过；CI 通过；设备验证未测|
|R30-B03-06|B03|统一回放评价器|NOT_RUN|4c362dc66c8a|—|HOST：同策略同数据两次评分一致；换数据集不复用缓存；确认段只能经账本读一次|
|R30-B04-01|B04|完整策略合同进出生门|TODO|—|—|—|
|R30-B04-02|B04|行为指纹去重|TODO|—|—|—|
|R30-B04-03|B04|13个业务思路族增强|TODO|—|—|—|
|R30-B04-04|B04|短期结构窗口与集合缩减|TODO|—|—|—|
|R30-B04-05|B04|选时选位与出手条件|TODO|—|—|—|
|R30-B04-06|B04|六类结构生成器注册|TODO|—|—|—|
|R30-B04-07|B04|失败驱动结构修复|TODO|—|—|—|
|R30-B05-01|B05|进化与确认隔离|TODO|—|—|—|
|R30-B05-02|B05|全管线空信号对照与功效|TODO|—|—|—|
|R30-B05-03|B05|AtomNet 十位置真实训练输入|TODO|—|—|—|
|R30-B05-04|B05|I08 神经表示接入策略消费者|TODO|—|—|—|
|R30-B05-05|B05|NumberQ 排序器与学习模型统一|TODO|—|—|—|
|R30-B06-01|B06|校准时间隔离与等质量箱|TODO|—|—|—|
|R30-B06-02|B06|原子分子贡献评分|TODO|—|—|—|
|R30-B06-03|B06|20矩阵与教师能力迁移|TODO|—|—|—|
|R30-B06-04|B06|能力逐对象注册表|TODO|—|—|—|
|R30-B07-01|B07|确认队列不被换榜挤掉|TODO|—|—|—|
|R30-B07-02|B07|资格门版本化|TODO|—|—|—|
|R30-B07-03|B07|主链逐项解释与单笔REAL|TODO|—|—|—|
|R30-B07-04|B07|证书撤销≤5秒|TODO|—|—|—|
|R30-B08-01|B08|12章报告同一快照出数|TODO|—|—|—|
|R30-B08-02|B08|诊断规则给出具体代码行动|TODO|—|—|—|
|R30-B08-03|B08|升级诊断与消融空章修复|TODO|—|—|—|
|R30-B08-04|B08|导出取消、重建与分页|TODO|—|—|—|
|R30-B08-05|B08|总存储与证据保留|TODO|—|—|—|
|R30-B09-01|B09|图表与排行榜保全|TODO|—|—|—|
|R30-B09-02|B09|全链故障注入回归|TODO|—|—|—|
|R30-B09-03|B09|性能剖析与最后修复|TODO|—|—|—|
|R30-B10-01|B10|真机长时场景|TODO|—|—|—|
|R30-B10-02|B10|自然质量与主链闭环|TODO|—|—|—|
|R30-B10-03|B10|撤销与回滚演练|TODO|—|—|—|
|R30-B10-04|B10|最终账本与单一入口|TODO|—|—|—|
|R30-B11-01|B11|开工条件判定|TODO|—|—|—|
|R30-B11-02|B11|停机式离线切库与唯一路由点|TODO|—|—|—|

## 偏差记录（方案工具在施工中暴露的问题与处理）

- D1（B00-01/B00-04）`ownership_check.py`：git 默认转义中文路径导致方案目录误报；`rebind_identity.py` 的生成产物 `release/strategy_su1_reviewed_inputs.json`、`release/r82_training_invalidation.json`、`release/research_dependencies_r967.json` 按 docs/02 §2 属对所有卡开放的“生成身份文件”，但原 OPEN 表漏列。tools/r30 版本以 `-c core.quotepath=false` 调 git 并补列这三个文件，其余规则不变。
- D2（B00-04）`ci_build_r30.sh`：`SqlContractDumpR30` 引用 B01-02/B02-01/条件 B11-02 才加入的类，B00–B02 阶段无法编译。处理：已加入应用的类取应用编译产物，未加入的取方案 `code/main` 原文（与施工时原样复制的字节相同），来源逐类写入 `evidence/r30/ci/sql_contract_sources.txt`。
- D3（B00-04）CI 可能在卡提交之上追加 `R29: bind ...` 生成身份提交，原脚本只看 HEAD 提交信息会跳过所有权检查；改为检查最近一个以卡号开头的提交。
- D4（B00-04）签名工作流：按 docs/01 “只改分支与文件名”复制后，另外 (a) 把构建脚本指向 tools/r30/ci_build.sh；(b) 发布前把 `package_receipt.py` 产出的 `STARCORE-R29.1-*` 改名为 `STARCORE-R30-*`（R29 脚本不改）；(c) 官方通道增加 androidTest APK 与模拟器上 `R30FoldBackInstrumentedTest`（B00-03 第5步要求，需与 debug 包同签名，r30 分支自身没有签名身份）。
- D5（B00-02）会话 git 代理拒绝推送 tag 引用（HTTP 403）：`archive/r29-b01-cp16` 仅本地存在，需所有者补推。


## 2026-10-31 恢复施工前核对

- B03-03 / B03-06 的原接口单测不等于运行链路完成：实际 EvolutionLoopR27 仍使用旧 look/run，未调用 ReplayEvaluatorR27.evaluate；复核时 confirmReads 的持久保存和启动恢复也没有生产调用。已启动下述 B03-03-b 修复；运行链接线仍待完成，不把局部 HOST 通过写成完整闭环。

- B03-03-b 已新增单库确认预约表并在进化运行器启动时恢复/绑定研究写者；确认读取的预约须先提交，未知旧曝光和重复预约保守拒绝，对照组保留独立预算。以磁盘 SQLite 真正结束写入进程、启动新进程验证预约仍在且重复被拒，另测旧曝光与过期写者；追加反证发现旧曝光未计入预算，已修正并复测，失败及通过日志均在 evidence/r30/b03-03-b。修正前源码 1011d71 全量单测 180 项及 lint 通过（无新问题，保留 51 条基线及 7 条未匹配），最终源码仍需重新验证和 CI。EvolutionLoop 原 look/run 调用尚未接入此闸门，不能宣称生产确认链已闭环。

- 修正后的源码 ff52929：编译通过；确认账及统一评价器相关定向单测 8/8 通过；实际 SQLite 新进程恢复和重复建表回归通过；R30 SQL 19/19、所有权检查无越界，lint 任务成功（沿用上述基线）；完整新 CI 待执行。此前源码 56fb281 的 [CI 41176676100](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41176676100) 已成功，含重复 schema 安装检查，不能替代 ff52929 的 CI。

- B03-03-b 继续补 EvolutionLoop 确认入口：现有 look 直接调用 run 会绕过已持久化的账本。先用实际进化入口建立反证，再修接线及重读处理；B03-06 仍负责统一评价入口，B05-01 负责确认后进化隔离。

- 源码 58b24f9 本地 41 份新 XML 共 177 项单测、失败/错误/跳过均为 0；本轮 lint 新问题 0，原有 51 条基线、7 条未匹配保留。R30 SQL 合同 19/19，资格与模型回滚 Q1–Q16 通过；实时缓存两种回归及冻结源 8 种 SQLite 场景通过。日志见 evidence/r30/b03-04-b/source-58b24f9。远端 [CI run 41175814154](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41175814154) 成功；本地及 CI 结果不替代设备验收。
- B00 已有 R13 run 41169766223 和 R27 run 41169784335 构建通过；R13 原覆盖升级使用 R26 包，另有 4 项模拟器构造数据测试。实际 CP06/CP16 → 已发布 B00 版的首次 run 41176346773 安装及文件标记保留成功，但 release 的 run-as 探针无法读取偏好，READY 超时，未到数据库比较；失败证据保留。独立验证分支使用模拟器 root 读取相同偏好并保持原门槛，重跑 [41176921680](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41176921680) 两路均通过：覆盖前 4714 条开奖数据无丢失或更改、数据库 integrity_check 为 ok、原有表无缺失，覆盖及全新安装的冷启/重启均通过服务心跳探针。证据在 evidence/r30/b00-actual-upgrade；该测试只证明所发布 B00 R13 包在 API34 模拟器的结果，未证明 CP16 独立库 ACTIVE 状态回收、最新 R30 源码出包升级或真机长测。

- R30 CI run 41174517618 失败：Q15 恢复/回滚夹具重复学习 ISSUE_1，触发 R30_DUPLICATE_LEARNING，原回滚断言未运行。已登记 B03-05-b 修正夹具使用 ISSUE_2，保留防重复规则和回滚断言；本地及后续成功 CI 的原资格 HOST 16 项通过（含 Q15 原字节和游标回滚）。

- B03-04 重开：实际 SQLite 调用原生产入口，90 期积压时只读取64期（落后26期）；更正既有开奖后缓存仍用旧值。旧单测只检查直接构造的数据集，未覆盖实时读库链路。已登记子卡 B03-04-b；真实 SQLite 两条回归、完整单测和 CI 已通过，缓存对原始数据逐行校验并只在完整前缀一致时复用数值流。
- B03 同一实时冻结链另复现源竞态：预测准备后、写事务前目标期开奖已落库，旧代码仍写入 FROZEN；保留真实 SQLite 失败日志。现已加同库源版本与写事务检查；目标已出、历史更正/删除、历史导入变化、触发器变化或版本不可用时均拒绝冻结并记录可重试原因；正常源和仅抓取元数据变化仍可冻结，8项 SQLite 场景及 Android 编译、CI 已通过；设备验证未测。

- B00–B03 的 23 个原 DONE 卡提交均为当前 HEAD 的祖先；原账本仅 B00-05 列出来源明确的 CI 成功。B02/B03 部分历史 CI 曾失败或被取消，B03 修正源码现有本地全量测试与远端 CI 成功；不能把 DONE 一律理解为已完成设备验收。
- 对 23 个原卡提交按当前所有权工具复核，22 通过；B02-01 原提交 56e7834 对 ReportR29.java 的时间线报告修改未登记，且该文件后由 B02-02 使用。已事后登记 B02-01-b 保留事实；原提交当时未通过检查，不倒写为历史通过。
- 初次直接运行 Gradle 单测在生成调用资产阶段失败：SOURCE_CALL_INDEX_STALE:AnalysisRequestStoreR36.java。已按编译产物重新生成调用索引并登记 B03-06-c；全量单测及 CI 已复验，设备测试未运行。

## 施工续接记录

- 当时账本将 B00–B03 的 23 张卡记为 DONE 并推送；本轮复核后修复 B03-04/05 并取得 CI 通过，B03-03/06 运行链接入及设备项目仍待验证。用户已要求继续 R30 全部剩余施工。
- 卡提交后另有修正提交：R30-B02-01-b / R30-B02-03-b（主链红线 DbStore、SettlementEngine 保持与 bb607cf 逐字节一致）、R30-B03-06-b（身份重绑）。
- 打包：按用户要求全部批次完成后统一出包；目前只有 B00 包。
