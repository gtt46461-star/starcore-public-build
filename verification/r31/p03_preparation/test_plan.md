# P03 可复用测试与新增验证矩阵

首轮准备仅阅读下表现有测试；表中的原测试及计划新增验收项仍未由本 agent 执行。root 后续另行授权并已执行隔离反例 harness，见 [repro/README.md](repro/README.md)：23 个预训练 work 边界和 9 个真实子 JVM halt/restart 数值恢复通过，9 个 partial 完整发布合同及 1 个重启发布 identity 合同 baseline FAIL。它们不等于下表端到端/DB/Android验收。没有 Gradle、安装、清数据、执行 GC、提交或推送。历史 PASS 文件不作为本次结果。

## 已有真实测试，可作为不退化基线

|路径（省略公共 Java 包前缀）|已存在的有意义验证|P03 使用方式 / 限制|
|---|---|---|
|`app/src/test/.../strategyv2/R29ModelArtifactTest.java`|全权重/Adam/RNG/FTRL/receipt 恢复后下一步精确一致；失败 draft 不污染 committed；校验和/尾字节拒绝；滚动 dataset 不重置模型；旧 CP04 receipt 恢复要求|复用零容差；增加 outer3→新 envelope/引用后下一步一致，禁止 weights-only 或 counter=0 迁移|
|`R30TenPositionTrainingTest.java`|10 位置 PIT；各训练/在线边界完整编码恢复；每片 <=64；development rollback 不倒退 cursor；legacy ABI 隔离|原 online 断言把 partial trainedThrough 当本期，需保留数值一致断言并新增五类水位；旧绿灯不能证明禁止 partial 发布|
|`RepairNeuralR30Test.java`|材料化不等于梯度/发布；TRAIN/SELECT 外的 formal confirm 标签不可读；旧 outer exact bytes；未知 validation origin；每 phase checkpoint；availableAt/through 参与 model identity；online window 先登记|扩展 initial inputBoundary/实际训练截止/online admission baseline 分离；不可降低 formal holdout 隔离|
|`RepairModelReplayR30Test.java`|真实两个模型只用过去输入；标签不进入 freeze；在线分配必须有合格证据；replay mode/evidence/RNG 无损编码|确保新 outer/CAS/pub receipt 不改变 uniform63 / old48+recent15 的数值与证据，不重置模型/alpha|
|`R27LeakageTest.java`、`R27ReplayTest.java`|原子/家族特征因果，未来标签扰动不影响历史；replay/live 一致、gap segment|扩大为 canonical 日期边界与缺期/补期/尾滚动，保留现默认 gate 与数学|
|`R30ModelSurfaceTest.java`|真实 number head；输入/输出不可变；late/target/训练截止拒绝；typed AST 实际消费 model surface；I08 实际提出 model leaf|增加必须持有完整 publish receipt、稳定重启 hash、十位置同一 publication，partial 不可进入 surface|
|`tools/r30-repair/qual_host/RepairNeuralR30HostTest.java`|SQLite JDBC 执行生产字节码；64 样本一次预读及数学/RNG一致；角色先于查询拒绝；NaN/shape/position 错误；旧 outer 原始 bytes 归档和事务 rollback/retry|加当前 outer3 精确保留；CAS 缺文件/损坏；bind budget；新旧读取兼容；不得仅 mock codec|
|`tools/r30/qual_host/TenPositionR30HostTest.java`|两个真实子 JVM 对 SQLite halt/restart；stops=4/5/9；失败提交不更新状态；恢复 cursor/hash 一致；旧 owner 原 bytes 不变|现 Unsafe runtime 没有 P01 currentClaim，需接入真实 claim/immutable writer；扩展 stops=1..9、位置10 commit 前/后；原最多120材料化行 fixture 不能作为64-op验收|
|`tools/r30-repair/qual_host/RepairInputPinR30HostTest.java`|固定 pin 重启恢复；追加400/删除热尾500不丢冻结副本；更正 invalidation 且 rollback不生效；before/after 同事务 guard；原 copy不可变；revert数值但 revision 仍换；trigger tamper failclosed|增加 pin admission 预算、canonical known_at/gap/role/reader envelope、受保护删除、新旧hash关联，不改变旧 pin有效语义|
|`tools/r30/qual_host/FrozenConfirmationR30HostTest.java`|实际子 JVM 冻结确认恢复；packet/result不可变；不反馈 loop；重试不重领 global index；orphan固定输入恢复、损坏拒绝|统一 pin 后 exact frozen bytes、reservation/receipt 仍一次；模拟低盘仅研究等待、实时 freeze/settlement 继续|
|`tools/r30/qual_host/ModelSurfaceR30HostTest.java`|valid/missing/target/race 四模式的实际模型、特征、消费引用测试|增 commit 后 publish 前 crash 和 restart，old frozen概率/hash不变；publication→surface→call→freeze receipt联接|
|`tools/strategy_su2/tests/R26RecoveryTest.java`|legacy全数值状态与下一预测、gap只重置 temporal不重置权重、calibration恢复、cooperative cancel真正终止、consensus streaming算术一致、私有bootstrap下一步一致|复用已有数值与 owner 终止合同；post-member 子阶段新增 golden 与重复恢复测试|
|`tools/strategy_su2/tests/R26ChunkedCheckpointTest.java`|4MiB流式还原、序列化调用边界不改变hash、ring移动块复用、legacy optimizer精确下一步|扩展十位置流式大状态，64MiB旧合法状态不被32MiB单块限制变成清空重训|
|`app/src/androidTest/.../strategyv2/R26CheckpointDeviceTest.java`|真实 Android fsync/rename/隔离 SQLite、stale parent、prepared/committed可达性、并发同内容与quota、report pin、current+parent+outputs备份闭包、streamed子块损坏|增 r29 checkpoint / R31 publication / pin根；真实 DB引用后现有maintenance不误删；metadata protection联测。现 crashBeforeAndDuring 方法是事务故障模拟，不能冒充真进程kill|
|`R26RuntimeDeviceTest.java`|实际生产 coordinator、样本和隔离 SQLite 的 stale owner/预算/WAL延迟、runtime对象重建|现 restart 为同进程对象替换，不是真 Android kill/restart；不能替代目标设备24h/72h|

`tools/r30-repair/run_neural_host.py` 可复用编排，但它依赖 `app/build` 中编译好的生产 classes、Android 36 和 sqlite-jdbc；必须由主任务在 P03 集成后 fresh build，再执行已更新 harness，记录 source/build/hash。不能用旧 classes 得出新源码 PASS。未发现 TrainingCasCP12 的直接命名测试引用，不据此断言整个仓库完全无输入 CAS 测试。

## 必需新增验证

|ID|故障/输入|必须核对的事实|执行层 / 当前状态|
|---|---|---|---|
|P03-T01|outer1/2/3 原 fixture，真实设备外层3原 bytes|归档 checksum/bytes精确且不可改；新 reader恢复完整NN/Adam/RNG/FTRL/replay/counters；下一更新和黄金一致；旧 owner 原数据仍可恢复|单测 +真实SQLite；`NOT_RUN`|
|P03-T02|位置1..9逐一停止，位置10提交前失败|partial/nextPosition前进但 complete/published/最后消费仍旧期；restart数值精确；已提交event不重复训练|真实子JVM SQLite + Android进程kill；`NOT_RUN`|
|P03-T03|CAS文件完成但DB未提交、DB提交但进程未publish、重复同event不同owner/fence|引用/游标/receipt原子；restart采用已提交state；同一issue+position+modelVersion仅一次；availableAt/modelHash恢复稳定|真实SQLite+生产writer；`NOT_RUN`|
|P03-T04|initial FILL完成，TRAIN/SELECT/CONFIRM人工可辨标签|inputBoundary可以是holdout尾；训练实际影响截止不能报成holdout尾；online admission基线保留原输入尾；formal标签不因修水位被补学|纯数值+DB读审计；`NOT_RUN`|
|P03-T05|无梯度/学习率0控制 vs受控非零梯度|控制权重不变合法；非零梯度确实改变weight/Adam；消费引用恰为本次完整发布hash，不声称质量改善|单测+端到端host；`NOT_RUN`|
|P03-T06|尾窗滚动、规范跨日、缺期、补期、更正/回退数值|固定输入身份稳定；新revision新envelope；old experiment标invalid且证据留存；known_at缺失显式unknown；冻结历史不会被未来补写|输入host+单测；`NOT_RUN`|
|P03-T07|pin/CAS/cache/盘配额不足，现有合法大checkpoint|明确RESOURCE_WAIT/BLOCKED_PROTECTED_DEBT；原bytes不删；实时冻结结算继续；resume后同样数值；拒绝不被包装PASS|生产admission集成+设备；`NOT_RUN`|
|P03-T08|120样本materialize、128 births、Foundry proposals、generation最终多行结果|每个真实writer批 op<=64且bind cap；callback隐藏SQL有归属；chunk无漏重；final未提交不可见新generation；全候选/父系保留|生产writer+SQLite计数+设备hold；`NOT_RUN`|
|P03-T09|RAW/CAL共享state+teacher states，多候选retire|逐owner字节账与实际唯一数组/引用核对；stateSeries计费；无负账/双扣；清候选引用后live堆/native/cache下降；预测完全相同|单测+设备heap/PSS；`NOT_RUN`|
|P03-T10|新checkpoint/pub/pin引用，prepared失效及report snapshot期间现有maintenance|已提交根、旧可恢复根、子块闭包均保护；备份闭包完整；损坏failclosed；P03只新增保护、不跑新GC|Android隔离DB/CAS；`NOT_RUN`|
|P03-T11|legacy generation成员结束/策略回放/committee/mutation各子阶段重启|黄金RNG/weights/calibration/results/act/K3/nextfreezehash一致；计算恢复不重新学习标签，且有持久子阶段进度|纯数值+真实SQLite+进程重启；`NOT_RUN`|
|P03-T12|source或fence在prepare与commit间更换、提交成功后旧owner晚返回|旧owner不能提交/发布；新owner从durable receipt恢复；helper真实终止后才替换；不改P01owner协议绕过测试|P01联合接口+host；`NOT_RUN`|

## 证据与发布门

所有结果绑定 planning baseline、integrated source commit、worktree hash、package/build identity、设备/API/数据身份和执行命令。数值容差沿用现有 exact contract，不事后放宽。writer hold/GC/pin恢复/吞吐、首屏和实时延迟只能报实测覆盖；初始 24h、最终 72h、R27_GATE_V1 及独立未来质量门由主任务负责。缺原 artifact、目标设备、真实进程切点或持续时长时分别写 BLOCKED / NOT_RUN，不能以 host 或合成数据替代。
