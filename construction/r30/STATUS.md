R30 基线 bb607cf（CP06，已验收）；R29 CP07–CP16 不并入；每卡状态表（TODO/DOING/DONE/NOT_RUN/BLOCKED）。

# STARCORE R30 施工账本

- 施工分支：`construction/r30-20261031`（父提交 bb607cf5ab69f7286f859e43f87ada85a5bc456d）
- 方案：`construction/plans/STARCORE_R30_FINAL_20261031/`（只读副本）
- 设备侧指标（真机时延、长测、自然质量）：未执行即 NOT_RUN，不写 PASS。

|卡号|批次|标题|状态|提交|CI|备注|
|---|---|---|---|---|---|---|
|R30-B00-01|B00|从CP06建R30分支并锁定基线|DONE|7157660adec1|CI 自 B00-04 起|baseline_lock mismatches=0；rebind 产物 release/strategy_su1_reviewed_inputs.json 按 docs/02§2 属开放文件（ownership_check 原 OPEN 表漏列，B00-04 补登）|
|R30-B00-02|B00|封存R29 B01分支，登记可复用件|DONE|aa28bc2f60a4|—|标签本地已建；远端 tag 推送被会话代理 403 拒绝，待所有者补推（见 R29_ARCHIVE.md）|
|R30-B00-03|B00|回收R29后期包遗留的Root触发器与独立研究库|DONE|17dc6ef7bb35|待 r30.yml（B00-04 起）|本地全量编译通过；SQL 合同 19/19（含 T04/T05/T06）；R30FoldBackInstrumentedTest 需模拟器（CI connectedDebugAndroidTest）|
|R30-B00-04|B00|装施工护栏工具与R30 CI入口|DONE|f7044a7c2eba|—|工具按方案原样复制，3 处必要修正（见下方偏差记录 D1–D3）|
|R30-B00-05|B00|复核R29已签收的B00三卡并统一指标测量口径|DONE|—|—|44 项均有 measurementSource；空源/零样本→INSUFFICIENT、值 null；legacy_count requirements=1832 fileCards=299；子卡 R30-B00-05-a 登记测试文件|
|R30-B01-01|B01|唯一写锁：等待/持有时长账本|DONE|0bdc4468dde1|—|HOST 6/6；DatabaseCoordinator 仅补丁所示改动；真机/模拟器持锁数据 NOT_RUN|
|R30-B01-02|B01|账本落库与报告出数|DONE|01bbf50ee5e1|—|SQL T02 4/4；报告第3章两表（writerHoldR30）；“运行10分钟≥9窗口”需设备 NOT_RUN|
|R30-B01-03|B01|切片写入工具与Android适配器|DONE|32d371df2ec2|—|HOST 6/6（含中途失败回滚续做不重复）；Android 杀进程续做 NOT_RUN|
|R30-B01-04|B01|按账本切片前五名后台长事务|DONE|7f393f83943e|—|HOST：SettleChunkR30 S1–S3×3 种子 OK；R29 qual_host Q1–Q16 OK；DataRetentionGovernorR900 已有界未改（理由见 B01_OFFENDERS.json）；2小时复测 over_hard NOT_RUN|
|R30-B01-05|B01|任务活性判定与报告|DONE|873daf77aef7|—|HOST livenessClassification OK；当前 r26_task 无 pending/last_committed_seq 列 → 全部 UNKNOWN 单列，M12=INSUFFICIENT（如实）|
|R30-B01-06|B01|调度公平与温控核对|DONE|27b2e16296be|—|温控序列 HOST 6/6；STALLED 提升已接入 claim()，但 r26_task 尚无 pending/last_committed_seq 事实 → 无任务被判 STALLED（不猜测）；“下一轮被调度”的运行验证 NOT_RUN|
|R30-B01-07|B01|重复调度入口清单与收敛|DONE|65337ff1cb9e|—|SCHEDULER_OWNERS.json 42 个循环，无“两个活跃写者写同一研究表”；旧研究链连续性/捕获在 R26 运行时让出（与 R27-046 并行观察冲突已记录）；生命周期回归 androidTest NOT_RUN|
|R30-B02-01|B02|每期8点时间线落库|DONE|5f3c7030fa60|—|HOST timeline 3/3；SQL T01 通过；FIRST_OBSERVED 落在 payload 回调（complete 回调无解析期号）；30分钟≥95%三点覆盖 NOT_RUN|
|R30-B02-02|B02|抓取单飞与退避节奏|DONE|—|—|HOST 2/2（16线程单飞、退避序列）；HTTP 超时现值 2500/3500ms 已≤4000 未改；断网恢复真机序列 NOT_RUN|
|R30-B02-03|B02|缺期补抓与幂等结算核验|TODO|—|—|—|
|R30-B02-04|B02|准备在事务外、冻结在短事务内|TODO|—|—|—|
|R30-B02-05|B02|UI与后台延迟|TODO|—|—|—|
|R30-B03-01|B03|PIT守卫接入所有冻结写入|TODO|—|—|—|
|R30-B03-02|B03|真实历史数据治理|TODO|—|—|—|
|R30-B03-03|B03|分割角色与曝光账|TODO|—|—|—|
|R30-B03-04|B03|增量特征与回放|TODO|—|—|—|
|R30-B03-05|B03|训练游标与可恢复状态|TODO|—|—|—|
|R30-B03-06|B03|统一回放评价器|TODO|—|—|—|
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
