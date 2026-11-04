# 星核天枢 R31 返修闭环方案

Nov 3, 2026 · @ttg

依据：检测报告 R30-REPAIR1-2026.11.02-009（作业 ba5bcb7f…，最新期 202611030010）+ 公共库源码提交 5237b265（分支 construction/r30-repair-20261101，即报告里的 sourceCommit）。

## 一页结论

**软件不再创造算法的直接原因已经定位：研究线程池启动约 7 分钟后，13 个研究任务里有 11 个因“领取任务的写事务超时”被连续计为错误，10 分钟内超过 5 次后被永久隔离（QUARANTINED），之后 12.5 小时再也没有运行。** 隔离只有手动重试或进程重启才会解除，而后台“重启”只重启服务、不重启研究协调器，所以等于永久停摆。数据集任务一停，进化、原子、校准、图表缓存、矩阵、研究观察全部跟着停。

其余四个问题的根因同样可以落到具体文件：

1. **图表空**：15 张图里有 6 张读的是已退役引擎的旧表（r908 / v41 / r943 / r974），这些表没有任何生产者，永远是 0 行；另 4 张读 R27 研究产物，被上面的隔离卡死；整页共用一个 15 秒读超时，一个慢查询会让整页“读取超时”。
2. **存储只增不减**：每出一份检测报告就完整复制一份数据库（本次 592 MB 的 snapshot.db），当前报告的副本永不清理；55 张研究表的任何删除都会被触发器原样复制进不可删除的 r30\_keep\_\* 表；热表删除前必须先“已验证归档”，归档不跑就一行都不删。清数据后 12.6 小时数据库已到 603 MB，约 0.8 MB/期。
3. **后台不运行**：设备是 vivo V2353A，退出记录里有 3 次被系统“一键清理（single-cleaner）”强杀，电池优化未设为“无限制”；只有 72 期在开奖前完成了预测，36 期实时期没有赛前预测，结算因此停在 202611020847，比抓取落后约 10 小时。
4. **卡顿**：研究写入在写事务里执行建表/建触发器语句（DDL），Android 每执行一次 DDL 就关闭全部读连接，实时分析看门狗线程被阻塞在取连接上；写锁硬超限 579 次；请求到落库 P95 = 37.4 秒（开奖间隔 60 秒）。

**方案共 9 个批次（B0–B8）。B0+B1+B2 必须同一版本发布**（它们共享“写失败 → 隔离”这一个不变量，分开发会让流水线继续死掉）；B3、B4 可并行；B5 依赖 B1；B6 依赖 B1+B2；B7、B8 收尾。每批都有可执行的验收线，见“验收总表”。

关于“高胜率”：这套方案能保证的是**创造—训练—结算—反馈—进化的闭环真实、持续、高速地跑起来，并且有能力在真实优势存在时把它找出来**（用植入信号的合成数据验收）。它不能保证在真实开奖上跑出高于基线的胜率，原因见“能做到什么、不能承诺什么”一节。

## 根因链

![根因链 · 从写锁争用到研究停摆](根因链.png)

上面两行是原因，下面一行是你看到的症状。B2 消除顶行的写锁压力，B1 改掉中间一行（不计错、会到期、能复活），所以二者必须同版发布。

## 证据：检测报告里的关键实测

下表每一行都能在报告 ZIP 里按“出处”找到原文。报告自身结论是 overallAcceptance = INCOMPLETE，44 个指标里 31 个为 INSUFFICIENT / SOURCE\_NOT\_YET\_MEASURED。

| 事实 | 实测值 | 出处（报告内文件） |
| --- | --- | --- |
| R27 研究任务状态 | 13 个里 11 个 QUARANTINED，错误 = F01\_WRITE\_FAILED\|operation=R30\_PREPARED\_TASK\_CLAIM；chart-cache = R27\_CANCELLED；matrices = MATRIX\_ORIGINAL\_STUDENT\_UNAVAILABLE | R30\_REPORT.json 第 4 章 taskLiveness |
| 研究任务最后一次提交 | updated\_at=1793619791…，进程启动后约 7.7 分钟；之后 747 期无提交 | 同上 committedFacts.periodsSinceCommit |
| incumbent-forecast | QUARANTINED，R26\_UNSLICED\_COMPUTATION\_CAPACITY | 同上 |
| 研究进度心跳 | business=RESEARCH\_STALE，progressAge=45,165,527 ms（≈ 12.5 h），learningProgress=UNKNOWN@0 | runtime-observation-heartbeatSummary.txt |
| 重启请求 vs 实际启动 | restartRequest=26 / actualStart=1，recovery=SKIP\_RECOVERY\_LEASE\_ACTIVE | 同上 |
| 结算停滞 | 最后结算 202611020847，抓取已到 202611030010；errorCode=FROZEN\_OBSERVATION\_MISSING | 同上 |
| 赛前预测覆盖 | 完成结算 72 期；实时期无赛前预测 36 期；历史种子期无预测 4,958 期 | REPORT\_DETAILS.txt 「R923运行闭环唯一真值」 |
| 请求到落库时延 | P95（最近 96 次）= 37,373 ms | 同上 |
| 后台权限 | SETUP\_REQUIRED\_BATTERY\_RESTRICTED，电池无限制=NO，权限 8/9 | 同上 |
| 进程被杀记录 | 3 次 FORCE STOP due to single-cleaner，1 次 clear data | runtime-observation-crashSummary.txt |
| 写锁硬超限 | writerHardViolations = 579（DETECTED） | R30\_REPORT.json diagnosis.findings\[0\] |
| 线程快照 | starcore-r26-continuous-research BLOCKED 在 ResearchRetentionStoreR30.ensureProtected → closeAvailableNonPrimaryConnections；starcore\_b07\_analysis\_watchdog BLOCKED 在 waitForConnection | runtime-observation-threadStacks.txt |
| 单次分析 | 耗时 10,712 ms，堆峰值 220 MB，HISTORY 阶段超预算（1224/1000 ms） | report-text-37e4939…txt B05\_RESOURCE\_RAW |
| 存储普查 | 合计 1.29 GB：数据库 603 MB；报告 673 MB（其中 snapshot.db 592 MB）；旧备份 3.35 MB | STORAGE\_CENSUS.txt |
| 表结构 | 713 张表，556 张为空 | DIAGNOSTIC\_SCOPE.json tables |
| 最大的表 | research\_row\_revision\_v2 316,363 行；v57\_runtime\_event\_r926 134,688；v57\_k3\_action\_space\_1200 85,859；v57\_research\_action\_label\_r811 84,000；r27\_chart\_cache 72,458 | 同上 |
| 进化产出 | r26\_evolution 50 行；r27\_policy / r27\_lineage / su2\_lineage / r974\_model\_lineage / research\_evolution\_event\_v41 均为 0 | 同上 |
| 升级诊断 | 一代比一代强、算子、家族、原子分级全部“暂无记录”；9 个探针期数均为 0；延续矩阵 7,190 次重拟合已证实 0 条 | UPGRADE\_DIAGNOSIS.md |
| 报告自带的检出力测试 | 真实命中概率 0.34 时，20 次合成实验检出 19 次，中位数 974 期，P95 3,232 期 | R30\_REPORT.json 第 2 章 syntheticControl.power |

## 问题总表：症状 → 根因 → 文件 → 批次

共 24 个问题，编号 P01–P24，后文各批次按编号引用。文件路径省略前缀 app/src/main/java/com/ethk3/novaonepro/。

| 编号 | 症状 | 根因 | 文件 : 方法 | 批次 |
| --- | --- | --- | --- | --- |
| P01 | 不创造算法、不进化 | 领取任务的后台写事务只有 250 ms 期限，关键业务写入排队时后台写必然超时；超时被包成 F01\_WRITE\_FAILED 计作任务错误 | strategyv2/ResearchPoolR27 : claimPrepared、loop；DatabaseCoordinator : runWrite | B1 |
| P02 | 任务一旦隔离永不恢复 | 10 分钟 >5 次错误即 QUARANTINED，无自动到期；只在协调器启动时解除 | strategyv2/WaitKindR27 : quarantine、recordError；RuntimeCoordinatorR26 第 79 行 | B1 |
| P03 | 领取失败后状态脏 | 异常时 prepared 表里的旧 PreparedTaskR30R1 不移除，下次拿旧 fence 领取可能抛 R30\_PREPARED\_OWNER\_CHANGED | ResearchPoolR27 : loop | B1 |
| P04 | 矩阵任务整体挂掉 | 三种需要“学生策略”的矩阵在没有策略时抛异常，连带其余矩阵一起失败并计入隔离 | strategyv2/MatrixResearchR30R1 第 43 行 | B1 |
| P05 | 现任预测任务被隔离 | 切片硬上限 millis+1000 ms，计算未在期限内到达检查点就 close+抛异常 | strategyv2/CooperativeContinuationR26 : slice | B1 |
| P06 | 图表缓存任务取消被计错 | InterruptedException(R27\_CANCELLED) 按错误记录 | strategyv2/RuntimeR27 第 71 行；WaitKindR27 : classify | B1 |
| P07 | 重启不解决研究停摆 | BootReceiver 只重发服务 Intent，且被恢复租约跳过；进程内研究池与库里的隔离标记不变 | BootReceiver 第 152–160 行；ServiceHeartbeatStore | B1/B3 |
| P08 | 卡顿、读连接被清空 | 每个保留步骤都在写事务里执行 CREATE … IF NOT EXISTS；Android 对 DDL 会关闭全部非主连接 | strategyv2/ResearchRetentionStoreR30 : ensureProtected；RuntimeCoordinatorR26 : retentionStepR30 | B2 |
| P09 | 写锁硬超限 579 次 | 一把全局写锁被 30+ 类操作争抢（R932/SU1/SU2/R941/R952/保留…），个别写事务持锁 ≈ 500 ms | DatabaseCoordinator；ResearchWriteQueueR27 : submit | B2 |
| P10 | 实时链读被研究拖慢 | 看门狗与研究共用连接池，getLatestDraw 等连接 | AutoFetchService : runAnalysisWatchdogR36；DbStore : getLatestDraw | B2 |
| P11 | 请求到落库 P95 37 s | HISTORY 阶段超预算 + 写锁等待 + 分析堆峰值 220 MB | AnalysisSingleFlightR36 链路；AutoFetchService | B2/B3 |
| P12 | 36 期实时期无赛前预测 | 分析晚于开奖就不冻结，没有“截止前快速通道” | OosAfterDrawWriterR35/R36；SettlementEngine | B3 |
| P13 | 进程被系统杀 | vivo 一键清理 + 电池限制；应用内只提示、无步骤引导与自动复核 | BackgroundCapability 相关 UI；BootReceiver | B3 |
| P14 | 每份报告复制整库 | 当前作业跳过清理（pruneTerminal 里 equals(current) → continue），snapshot.db 在发布后仍保留 | ReportPublicationCP12 : pruneTerminal、cleanRaw；ReportExportServiceCP4 | B4 |
| P15 | 删除变成搬家 | 55 张受保护表的 BEFORE DELETE 触发器把旧行复制进 r30\_keep\_\*，而 keep 表禁止 UPDATE/DELETE | strategyv2/ResearchRetentionStoreR30 : ensureProtected；ResearchRetentionR30.PROTECTED\_TABLES | B4 |
| P16 | 热表删不掉 | ARCHIVE\_RAW\_TAIL 规则要求“已验证 CAS 归档”才能删；归档未运行（archivedOriginalPages=0）则永不删 | RetentionTableRegistryCP12；DataRetentionGovernorR900 | B4 |
| P17 | 事件表膨胀 | v57\_runtime\_event\_r926 12.6 h 内 134,688 行（≈ 180 行/期） | RuntimeEventLedgerR926 | B4 |
| P18 | 修订索引翻倍行数 | research\_row\_revision\_v2 为每个被跟踪行写一行（316k） | RetentionRevisionSchemaCP12；MultiHorizonProbeR80 | B4 |
| P19 | 6 张图永远空 | 读已退役引擎表：v57\_algorithm\_genome\_r908、v57\_algorithm\_predraw\_r908、research\_evolution\_event\_v41、atomic\_tool\_context\_r943、r974\_model\_lineage、v57\_research\_mainline\_contribution\_r922 | ChartsRepositoryR83 | B5 |
| P20 | 整页图表超时 | 15 张图共用一个 15 s 取消信号和一次租约 | ChartsActivityR83 : load | B5 |
| P21 | 八套进化引擎并存 | EvolutionLoopR27、ModelEvolutionSU2、StrategyFoundrySU1、FoundryCampaignR26、LegacyGenerationR26、ExpertWorkshopR26、AlgorithmInventionLabR908、AssetGenesisEngineR915 各自写表、争写锁 | strategyv2/\* + 根目录 | B6/B7 |
| P22 | 进化只等实时开奖 | 进化依赖 r27-replay-dataset；未把 5,000 期历史当作可快速回放的训练场 | strategyv2/RuntimeR27 : EvolutionTask、DatasetTask | B6 |
| P23 | 结构膨胀 | 1,236 个 Java 文件 / 50.8 万行，713 表中 556 空；单文件最大 916 KB（DbStore） | 全局 | B7 |
| P24 | 报告答不出“缺什么、怎么做” | 31/44 指标没有接入采集源；报告每次内嵌 37 MB 源码调用索引 + 25 MB 诊断范围 | ReportR29、MetricDefinitionsR29、ReportSnapshotR30 | B8 |

## 能做到什么、不能承诺什么

**本方案把“创造能力”验收定义为：在植入了真实优势的合成数据上必须找出并验证它，在无优势数据上必须不误报。** 这是可以 100% 验收的；“在真实开奖上跑出高胜率”不是代码能保证的东西。

原因只有一条，但它决定了验收口径：

- Binance1-PK10 的号码由哈希推导。只要哈希正常，每个位置每期是 10 选 1 的独立均匀结果，固定 K3 的长期命中率是 3/10 = 30%，不论用什么算法选号。
- 已有证据与此一致：本次报告延续矩阵 7,190 次重拟合已证实 0 条；之前的影子命中 32.4%（N=1,104）、离线回测与随机对照无显著差异。
- 报告自带的检出力实验说明：如果真实命中概率是 34%，流水线中位数 974 期就能检出。**所以流水线跑通后，有没有优势会在几天内由数据给出答案。**

对应的设计原则（各批次都遵守）：

1. **让工厂全速运转**：创造、训练、结算、反馈、变异、融合、淘汰每一环都要真实产出记录，并用历史数据快速回放，不再只等实时开奖（B6）。
2. **不放松证据门**：MODEL\_PROVEN、多重比较校正、留出段保持；不加出手配额、不加追投/倍投。门是这套系统唯一能区分“真优势”和“运气”的地方。
3. **“一代比一代强”按同一前推段的父子配对衡量**，下界不为正就不计提升（与报告 P50-1 规则一致）。在无优势的数据上，正确的结果就是“未证实”，这不是缺陷。
4. **出手频率只报告、不强制**：报告会写出每个候选的校准后过阈频率，低于目标时状态为 DO\_NOT\_FORCE，与现有合同一致。

这意味着：本方案完成后，你会看到每天数百代的真实进化记录、完整的图表和平稳的存储。如果数据里存在可利用的规律，它会被找到并通过门；如果几千期后仍无候选通过，那就是对“这个游戏能否被预测”的一个可信回答。本方案不包含任何为了让数字好看而降低门槛的改动。

## 批次总览与合并决策

**分三个发布版本：R31-A（B0+B1+B2，必须同版）→ R31-B（B3+B4+B5，可并行开发）→ R31-C（B6+B7+B8）。** 每个版本内部仍按批次分开提交，便于回滚和对账。

| 批次 | 解决 | 依赖 | 发布版本 | 改动规模（估） | 是否可独立回滚 |
| --- | --- | --- | --- | --- | --- |
| B0 观测补齐 | 异常原因链、各表字节、任务隔离事件 | 无 | R31-A | 4 个文件，约 250 行 | 是 |
| B1 流水线复活 | P01–P07 | B0 | R31-A | 7 个文件，约 400 行 | 否（与 B2 同版） |
| B2 卡顿与写锁 | P08–P11 | B0 | R31-A | 6 个文件 + 静态检查脚本 | 否（与 B1 同版） |
| B3 后台与赛前预测 | P07、P11–P13 | B1 | R31-B | 5 个文件 + 1 个引导页 | 是 |
| B4 存储有增有减 | P14–P18 | B0（字节普查） | R31-B | 8 个文件 + 1 次迁移 | 部分（删除不可逆，需备份门） |
| B5 图表打通 | P19–P20 | B1 | R31-B | 3 个文件，重写 ChartsRepositoryR83 | 是 |
| B6 创造/进化收敛 | P21–P22 | B1、B2 | R31-C | 核心 6 个文件 + 6 个引擎改为插件 | 是（旧引擎先停用不删） |
| B7 精简 | P21、P23 | B6 稳定 72 h | R31-C | 删除为主 | 是（按表分步） |
| B8 报告改造 | P24 | B0–B6 的采集点 | R31-C | 4 个文件 | 是 |

**为什么 B1 和 B2 必须合并**：B1 让任务“容忍”写锁繁忙（不再隔离），B2 减少写锁繁忙本身。只发 B1，13 个任务会同时复活并涌入写锁，实时链会更卡；只发 B2，已隔离的任务仍然不动。两者共享同一组验收：“24 h 内 R27 任务零隔离 + 写锁硬超限为 0 + 实时 P95 < 5 s”。

**跨文件并发问题的处理规则**：

1. 一个“不变量”涉及的所有文件放同一批（例：“后台写繁忙不是错误”涉及 DatabaseCoordinator、ResearchPoolR27、WaitKindR27、RuntimeCoordinatorR26）。
2. 同一文件被两批修改时，后一批必须在前一批合并后的提交上开分支（RuntimeCoordinatorR26 在 B1 和 B2 都改 → B2 基于 B1）。
3. 每批的验收测试进 CI，后续批次不得让它变红。
4. 一切删除类改动（B4、B7）先以“停写、只读”形式运行 72 h，再真删。

## B0 观测补齐（R31-A）

**目标：修之前先让报告能看见真正的异常原因和每张表的字节数。** 现在报告只显示最外层 F01\_WRITE\_FAILED，看不到内层是 CP3\_DB\_BUSY\_DEADLINE 还是 R30\_PREPARED\_OWNER\_CHANGED；存储普查只有分类总数，报告自己写着 TABLE\_PAYLOAD\_DISTRIBUTION\_UNKNOWN\_WITHOUT\_ORIGINAL\_DBSTAT。

### B0-1 任务错误记录完整原因链

文件：strategyv2/ResearchPoolR27.java : record、strategyv2/WaitKindR27.java : classify、strategyv2/SchemaR27.java（迁移）。修改（不是重写）：

```java
// WaitKindR27.java 新增：取最内层原因，最多向下 8 层
public static Throwable root(Throwable t){Throwable r=t;for(int i=0;i<8&&r!=null&&r.getCause()!=null&&r.getCause()!=r;i++)r=r.getCause();return r==null?t:r;}
public static String rootTag(Throwable t){Throwable r=root(t);String m=String.valueOf(r.getMessage());return r.getClass().getSimpleName()+":"+(m.length()>160?m.substring(0,160):m);}
```

```java
// ResearchPoolR27.record(...)：INSERT 增加 root_cause 列
db.execSQL("INSERT INTO r27_task_error(task_id,error_class,message,root_cause,stack,recorded_at) VALUES(?,?,?,?,?,?)",
    new Object[]{id,failure.getClass().getName(),String.valueOf(failure.getMessage()),WaitKindR27.rootTag(failure),stack,now});
```

迁移：`ALTER TABLE r27_task_error ADD COLUMN root_cause TEXT`，放在 SchemaR27 的版本化迁移里（只执行一次），不要放在任何运行期路径。

### B0-2 隔离事件表

新增表 r31\_task\_health\_event(task\_id, kind, root\_cause, error\_count, at)，kind 取 QUARANTINE / RELEASE / BUSY\_WAIT。写入点：ResearchPoolR27.record 中 q==true 的分支；保留 2,000 行（与 r27\_task\_error 同策略）。报告第 4 章读这张表，而不是只读 r26\_task.status 的瞬时值。

### B0-3 每张表的字节数

文件：StorageCensusSU2.java（新增方法 tableBytes）。Android 自带 SQLite 多数未编译 DBSTAT，所以两段式：先试 dbstat，失败则用抽样估算，并在输出里标注方法。

```java
static Map<String,long[]> tableBytes(SQLiteDatabase ro){ // value = {bytes, rows, method(0=dbstat,1=sample)}
  Map<String,long[]> out=new LinkedHashMap<>();
  try(Cursor c=ro.rawQuery("SELECT name,SUM(pgsize) FROM dbstat GROUP BY name",null)){
    while(c.moveToNext())out.put(c.getString(0),new long[]{c.getLong(1),-1,0});
    if(!out.isEmpty())return out;
  }catch(RuntimeException noDbstat){/* fall through */}
  List<String> tables=new ArrayList<>();
  try(Cursor c=ro.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",null)){while(c.moveToNext())tables.add(c.getString(0));}
  for(String t:tables){
    long rows=0;try(Cursor c=ro.rawQuery("SELECT count(*) FROM \""+t.replace("\"","\"\"")+"\"",null)){if(c.moveToFirst())rows=c.getLong(0);}
    if(rows==0){out.put(t,new long[]{0,0,1});continue;}
    String cols=columnLengthSum(ro,t); // "length(CAST(a AS BLOB))+length(CAST(b AS BLOB))+..."
    double avg=0;try(Cursor c=ro.rawQuery("SELECT avg("+cols+") FROM (SELECT * FROM \""+t+"\" ORDER BY random() LIMIT 400)",null)){if(c.moveToFirst())avg=c.getDouble(0);}
    out.put(t,new long[]{(long)(rows*(avg+12)),rows,1}); // 12 字节为行头估计
  }
  return out;
}
```

运行时机：每 6 小时一次，在只读连接上，结果写 r31\_table\_bytes(table, bytes, rows, method, at)，只保留最近 8 次。报告第 11 章输出前 30 张表和两次采样之间的增长率（字节/期）。

### B0-4 写锁按操作汇总

文件：HoldLedgerFlushR30 / WriterHoldLedgerR30。报告已有 r30\_writer\_hold 1,122 行但只显示 15 行。新增一个聚合视图：按 operation 汇总 count、over\_hard、hold\_max、wait\_max，按 over\_hard 降序列出前 20 个。这是 B2 拆分写事务的依据。

### B0 验收

- 单元测试 R31RootCauseTest：构造 IllegalStateException(F01…, cause=IllegalStateException(CP3\_DB\_BUSY\_DEADLINE))，rootTag 必须返回 `IllegalStateException:CP3_DB_BUSY_DEADLINE`。
- 单元测试 R31TableBytesTest（Robolectric 或 host SQLite）：在已知大小的表上，抽样估算误差 ≤ 25%。
- 真机：新报告第 4 章每个 QUARANTINED 行都带 root\_cause；第 11 章出现逐表字节，各表合计与数据库文件大小差异 ≤ 15%。

## B1 研究流水线复活（R31-A，解决 P01–P07）

**一句话：把“写锁繁忙”从错误改成等待，把“隔离”从永久改成会到期，把“重启”从重发服务改成复活研究池。** 七个文件必须同时改，否则任一环节仍会把任务重新送进隔离。

故障重现路径（由源码推出，B0 上线后用 root\_cause 列确认）：关键业务写入（开奖保存、结算）排队 → DatabaseCoordinator.runWrite 里后台写在队外等待，250 ms 到期抛 CP3\_DB\_BUSY\_DEADLINE → runWriteOrThrowF01 包成 F01\_WRITE\_FAILED → ResearchPoolR27.loop 当作 failure → record 计数 → 第 6 次即 QUARANTINED → loop 每次读到 QUARANTINED 就跳过，永远不再领取。

### B1-1 DatabaseCoordinator：繁忙用独立异常类型

修改 runWriteOrThrowF01（第 612–631 行）。新类型继承 IllegalStateException，现有 catch 仍然兼容。

```java
public static final class WriterBusyException extends IllegalStateException {
    public final String operation;
    WriterBusyException(String op, Throwable cause){ super("F01_WRITER_BUSY|operation="+op, cause); operation=op; }
}
static boolean isWriterBusy(Throwable e){
    for(Throwable t=e;t!=null;t=t.getCause()){
        String m=String.valueOf(t.getMessage());
        if(m.startsWith("CP3_DB_BUSY_DEADLINE")||isLocked(t)) return true;
        if(t.getCause()==t) break;
    }
    return false;
}
// runWriteOrThrowF01 内原来的 throw 替换为：
if(result==null){
    if(isWriterBusy(own.error)) throw new WriterBusyException(safe(operation), own.error);
    throw new IllegalStateException("F01_WRITE_FAILED|operation="+safe(operation), own.error);
}
```

同步修改两个按消息前缀匹配的地方，接受 F01\_WRITER\_BUSY：CalibrationRepairR75.java 第 136 行、strategyv2/RuntimeCoordinatorR26.java 第 79 行。验收时 `grep -rn "F01_WRITE_FAILED" app/src/main/java` 的每个匹配都要人工对一遍。

### B1-2 ResearchPoolR27：领取用更长期限、繁忙即退避、失败即清脏状态

修改 claimPrepared 和 loop。领取是一条很短的 UPDATE，让它等 1.5 s 不会影响实时链（实时写优先级不变，后台仍在队外等）。

```java
private final Map<String,Integer> busyStreak=new HashMap<>();

private RuntimeCoordinatorR26.Claim claimPrepared(String id,PreparedTaskR30R1 current){
    long deadline=TimeSourceR811.monoNowMs()+1500L;
    try(DatabaseCoordinator.WriterDeadlineScopeCP3 s=DatabaseCoordinator.withWriterDeadlineCP3(deadline)){
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_PREPARED_TASK_CLAIM",db->{ /* 原逻辑不变 */ });
    }
}
```

loop 里包住领取与切片，按异常类型分流：

```java
try{ claim=claimPrepared(t.id(),current); }
catch(DatabaseCoordinator.WriterBusyException busy){
    int k; synchronized(this){ k=busyStreak.merge(t.id(),1,Integer::sum); }
    long backoff=Math.min(10_000L,250L<<Math.min(k,6))+ThreadLocalRandom.current().nextLong(100);
    o=Outcome.idle("WAITING_WRITER_BUSY",0,-1); nextDelay=backoff;   // 不赋值 failure
}
catch(IllegalStateException stale){
    if(current!=null){ current.close(); synchronized(this){ prepared.remove(t.id()); } }
    failure=stale; // 真实错误照常记录
}
// 领取成功时：synchronized(this){ busyStreak.remove(t.id()); }
// 末尾 notBefore 用 nextDelay（若已设置），否则沿用原 25/1000/5000 ms 规则
```

record() 里的 R27\_TASK\_RECEIPT 写也会遇到繁忙：现在异常被吞掉导致状态丢失。改为把未写成的回执放进内存队列（每个任务只保留最新一条），下次 flushCpu 时补写。

### B1-3 WaitKindR27 + RuntimeCoordinatorR26：隔离会到期

迁移（一次性）：`ALTER TABLE r26_task ADD COLUMN quarantine_until INTEGER NOT NULL DEFAULT 0` 与 `quarantine_count INTEGER NOT NULL DEFAULT 0`。

```java
// WaitKindR27
public static boolean transientFailure(Throwable f){
    if(f instanceof DatabaseCoordinator.WriterBusyException) return true;
    Throwable r=root(f); String m=String.valueOf(r.getMessage());
    return r instanceof InterruptedException || m.startsWith("CP3_DB_BUSY_DEADLINE") || m.equals("R27_CANCELLED");
}
public static long quarantineMs(long count){ return Math.min(60*60_000L, 5*60_000L<<Math.min(count,4)); } // 5,10,20,40,60 分钟
```

- classify：transientFailure 为真时返回 RESOURCE(WRITER\_BUSY) 或 NO\_NEW\_EVENT(CANCELLED)，不进入 ERROR，不计数。
- record：只有非瞬时错误才 recordError；进入隔离时写 quarantine\_until = now + quarantineMs(quarantine\_count)，quarantine\_count+1，并写 B0-2 的 QUARANTINE 事件。
- loop 的隔离判断改为 `status='QUARANTINED' AND quarantine_until>?`。
- RuntimeCoordinatorR26.run 主循环每 60 s 执行一次释放：`UPDATE r26_task SET status='READY',wait_kind='',error_count=0,error_window_start=0 WHERE status='QUARANTINED' AND quarantine_until<=?`，并写 RELEASE 事件。第 79 行启动时的特例释放保留。
- 任务连续 24 h 内隔离 ≥ 6 次时，不再释放，改为状态 NEEDS\_FIX，报告第 4 章列出它的 root\_cause —— 这是真正的代码缺陷，不应无限重试。

### B1-4 MatrixResearchR30R1：缺学生策略只跳过对应矩阵

第 43 行 `throw new IllegalArgumentException("MATRIX_ORIGINAL_STUDENT_UNAVAILABLE")` 改为返回一个状态为 WAITING\_STUDENT 的空矩阵（不写快照、不覆盖旧快照），调用方对该 kind 记 DEPENDENCY(STUDENT\_POLICY)，其余 kind（频率、跨位置流等）照常计算。学生策略来自 B6 的进化产出，所以这是一个正常的前后依赖，不是错误。

### B1-5 CooperativeContinuationR26：切片超时不杀任务

slice() 在 hard 到期时现在调用 close() 并抛 R26\_UNSLICED\_COMPUTATION\_CAPACITY。改为：设置 paused 请求并再等一个宽限（3 s）；仍未暂停则返回 Slice(overrun=true)，计算线程保留，调用方以 COOPERATIVE\_OVERRUN 让出。只有连续 3 次超时且累计 > 30 s 无任何 checkpoint，才 close 并报错（这时 B0 的 root\_cause 会带上计算线程的堆栈，用于定位漏掉 poll 的循环）。同时在 incumbent-forecast 的主循环里每 64 次迭代加一次 ExecutionBudgetR26.poll()。

### B1-6 RuntimeR27：取消不计错

第 71 行的 InterruptedException("R27\_CANCELLED") 在 ChartTask、以及所有 submit 循环里仍然抛出（保证关闭时能退出），但由 B1-3 的 transientFailure 归类为取消，不计数。如果不是池关闭导致的中断（closed==false），清除线程中断标志后继续。

### B1-7 复活研究池（与 B3 共用）

新增 RuntimeCoordinatorR26.reviveResearch(String reason)：释放所有已到期隔离 → 若 RuntimeR27 池已关闭或所有 worker 线程死亡则重建 → 写一条 RELEASE 事件。触发点：ServiceHeartbeatStore 判定 business=RESEARCH\_STALE 且 progressAge > 15 分钟时，由 AutoFetchService 在进程内直接调用，而不是走 BootReceiver 重发 Intent（那条路被 SKIP\_HEALTHY\_SERVICE / SKIP\_RECOVERY\_LEASE\_ACTIVE 挡住，且不会重建研究池）。

### B1 验收

| 测试 | 做法 | 通过线 |
| --- | --- | --- |
| R31ClaimBusyTest（host） | 一个线程持有写锁 3 s 并标记为关键业务，13 个任务同时领取 | 0 个隔离；释放后 10 s 内 13 个任务都拿到领取 |
| R31QuarantineExpiryTest | 连续注入 6 次非瞬时错误 | 进入隔离；5 分钟后（虚拟时钟）自动 READY；第 6 轮变 NEEDS\_FIX |
| R31TransientClassifyTest | WriterBusy / R27\_CANCELLED / 普通 IllegalState 各一次 | 前两者 error\_count 不变 |
| R31MatrixPartialTest | 无学生策略时跑全部矩阵 kind | 无异常；非学生 kind 产出快照 |
| R31ContinuationOverrunTest | 计算体 2 s 不 poll | 返回 overrun，任务未关闭 |
| 真机 24 h | 正常使用 + 每小时导一次小报告 | r27\_\* 任务 QUARANTINED 时长合计 < 30 分钟；r27-replay-dataset 游标与最新期差 ≤ 5；r26\_evolution、r27\_policy、r27\_lineage 行数持续增长 |

## B2 卡顿与写锁治理（R31-A，解决 P08–P11）

**一句话：运行期不再执行建表语句；每个写事务持锁 ≤ 50 ms；实时链有自己的读连接。** 验收线：24 h 写锁硬超限 = 0，请求到落库 P95 < 5 s。

### B2-1 运行期 DDL 清零

为什么会卡：Android 的 SQLiteDatabase.executeSql 发现语句类型是 DDL（CREATE/DROP/ALTER，包括 IF NOT EXISTS 的空操作）时，会关闭连接池里所有空闲的非主连接并清空语句缓存。线程快照里 starcore-r26-continuous-research 就停在这一步，而实时看门狗同时在等连接。

静态扫描结果：除名字含 schema/migration/DbHelper/registry 的类之外，还有 **57 个文件**在业务代码里直接 execSQL("CREATE|DROP|ALTER…")，例如 ResearchRetentionStoreR30、SettlementTaskStoreR36、V51Repository、GlobalResearchScheduleR932、ResearchInputPinR30R1、RuntimeResourceAuditR927。

改法（按顺序）：

1. **ResearchRetentionStoreR30.ensureProtected**：移出 RuntimeCoordinatorR26.retentionStepR30 的写事务。改为按 `PRAGMA schema_version` 缓存：进程内 static long ensuredAtSchema，只在 schema\_version 变化时才进入，并且在迁移协调器（SchemaDomainCoordinatorR926）里调用，不在保留步骤里调用。B4 会把 keep 触发器整体拆掉，到时这个方法只剩三条建表语句。
2. **其余 56 个文件**：把它们的 DDL 字符串移到各自的 `static List<String> ddl()`，由 SchemaDomainCoordinatorR926 在启动迁移阶段一次性执行；业务方法里改为断言表存在（不存在则返回 SCHEMA\_PENDING 依赖，而不是现场建表）。
3. **防回归脚本** tools/r31/check\_runtime\_ddl.py，进 CI：

```python
import re, sys, pathlib
ALLOW = re.compile(r'(Schema|Migration|DbHelper|Registry)[A-Za-z0-9]*\.java$')
DDL = re.compile(r'execSQL\(\s*"\s*(CREATE|DROP|ALTER)\b', re.I)
bad = []
for p in pathlib.Path(sys.argv[1]).rglob('*.java'):
    if ALLOW.search(p.name):
        continue
    for i, line in enumerate(p.read_text(encoding='utf-8', errors='replace').splitlines(), 1):
        if DDL.search(line) and 'ddl()' not in line:
            bad.append(f'{p}:{i}')
print('\n'.join(bad)); sys.exit(1 if bad else 0)
```

第一步改完后脚本允许列表里只留 ddl() 方法；第二步改完后脚本必须输出为空。

### B2-2 长写事务拆分

依据 B0-4 的按操作汇总。已知样本里 R952\_STRICT\_FUTURE\_SETTLE 持锁 499 ms，RETENTION\_CP12\_PUBLISH\_PRUNE 等待 241 ms。统一规则：

- 计算一律移到事务外（先在只读连接上算好要写的行），事务里只做 INSERT/UPDATE/DELETE。
- 批量删除每批 ≤ 500 行，批间释放锁（ResearchRetentionStoreR30 已是 LIMIT 500，其它保留规则跟进）。
- DatabaseCoordinator 对后台写增加持锁预算告警：held > 50 ms 记 over\_budget，> 200 ms 记 over\_hard（已有字段，只是把阈值变严），报告输出前 20 名。
- 这部分对 ResearchWriteQueueR27.submit 的影响：报告已定位它为硬超限入口（修复卡 FR-B02-01～03），按卡片的幂等分片合同拆分，不改实时优先级。

### B2-3 实时链独立读连接

AutoFetchService.runAnalysisWatchdogR36 → postAnalysisForLatest → DbStore.getLatestDraw 现在走公共连接池。新增 DbStore.realtimeReader()：启动时打开一个独立的 OPEN\_READONLY 连接（与 ResearchPoolR27 的做法相同），只供抓取、分析、结算的读使用；同时把 getLatestDraw 的结果在内存里缓存到下一次开奖保存（此值只在 R914\_DRAW\_SAVE\_ATOMIC 成功后变化）。

### B2-4 分析时延与内存

- HISTORY 阶段超预算（1,224/1,000 ms）：历史窗口改为增量维护的环形缓冲（每期追加 1 行），不再每次从库里读 600–5,000 期。
- 堆峰值 220 MB：清单里 largeHeap=true 掩盖了问题。用 B0 的采样确认大对象来源（大概率是全量候选空间 1,200 行 × 特征矩阵），改为流式处理；目标单次分析堆峰值 < 96 MB。

### B2 验收

| 项 | 通过线 |
| --- | --- |
| check\_runtime\_ddl.py | 输出为空，CI 必须绿 |
| 写锁（24 h 真机） | writerHardViolations = 0；任一操作 hold\_max ≤ 200 ms |
| 实时链 | 请求到落库 P95 < 5 s，P99 < 15 s（最近 96 次） |
| 线程快照 | 不再出现 closeAvailableNonPrimaryConnections 或看门狗 waitForConnection |
| 内存 | 单次分析堆峰值 < 96 MB |

## B3 后台持续运行与每期必有赛前预测（R31-B，解决 P07、P11–P13）

**一句话：进程活着时每一期都必须在开奖前冻结一份预测；进程被系统杀掉时，用户一打开就看到原因和一键修复入口。**

要先说清楚一个硬边界：Android 上被“强行停止”（包括 vivo 一键清理）的应用，闹钟和任务会被系统清除，在用户再次打开前无法自启。代码能做的是：降低被杀概率、引导白名单、被杀后立刻告知。

### B3-1 开奖前快速通道（P12）

现状：完整分析 P95 37 s，晚于开奖就没有冻结观察，结算报 FROZEN\_OBSERVATION\_MISSING，这一期既不进反馈也不进统计。

改法：在预测的唯一 Owner（AnalysisSingleFlightR36）中加一个截止计时器：开奖时刻 drawOpenTime 前 8 秒若完整分析未落库，立即冻结一份 FAST\_PATH 预测（现任主链模型对上一期状态的输出，计算 < 200 ms，只用已提交的开奖），写入与正常预测同一张冻结表，带字段 path=FAST\_PATH。完整分析若在开奖前完成则覆盖它（同一期只保留开奖前最后一份），开奖后完成的只记录不覆盖。报告分开统计两条路径的命中，快速通道不影响任何证据门。

不做的事：开奖后不补预测（这是泄漏）。36 期无赛前预测的实时期继续按 EXCLUDED\_NO\_PREDRAW 处理，它们和 4,958 期历史期一起进入 B6 的回放训练。

### B3-2 后台权限引导与复核（P13）

- 清单增加 `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`；启动时用 PowerManager.isIgnoringBatteryOptimizations 复核，否则弹出 ACTION\_REQUEST\_IGNORE\_BATTERY\_OPTIMIZATIONS。
- 新增“后台保活设置”页（一个 Activity），对 vivo / iQOO 逐项打开：后台耗电管理 → 允许后台高耗电；i 管家 → 自启动；最近任务卡片锁定。厂商页面路径会变，每个 Intent 都要用 resolveActivity 判断，失败则退回应用详情页；每项由用户确认后打勾，状态存在 ServiceHeartbeatStore。
- 每次启动读 ApplicationExitInfo（现在已经在读），发现 “FORCE STOP … single-cleaner” 时在首页顶部显示红条：“上次后台被系统一键清理停止于 …，停止 N 分钟，点此修复”。

### B3-3 恢复路径修正（P07）

BootReceiver 第 152–160 行：isServiceHealthyForRecovery() 且 !isBusinessStale() 才跳过。问题是心跳显示 business=RESEARCH\_STALE 时，即便走到启动分支，重发服务 Intent 也复活不了进程内的研究池（26 次请求 / 1 次实际启动）。修正：

1. 心跳过期（服务死了）→ 维持现有启动服务路径。
2. 心跳正常但 RESEARCH\_STALE → 不发 Intent，由服务内的定时检查调用 B1-7 的 reviveResearch()。
3. 恢复租约加到期：租约超过 2 分钟仍未观察到实际启动（actualStart 未增加），视为租约失效，允许下一次请求。

### B3-4 关于 targetSdk 28

release\_manifest.json 里 targetSdk=28。它不是本次停摆的原因，但会让部分厂商把应用当作“旧应用”更积极地清理，且未来系统版本可能拒绝安装。升级会影响通知权限、精确闹钟权限和前台服务类型规则，放到 B7 单独评估，不在本批改。

### B3 验收

| 项 | 通过线 |
| --- | --- |
| 赛前预测覆盖（72 h，进程存活的期） | ≥ 99% 的实时期有冻结预测（FULL 或 FAST\_PATH） |
| 结算滞后 | 最新结算期与最新抓取期差 ≤ 2 期 |
| 研究停摆自愈 | 人为把所有 R27 任务置为 QUARANTINED，20 分钟内自动恢复进度 |
| 被杀告知 | 用 adb shell am force-stop 模拟，重新打开后首页显示停止时长与修复入口 |
| 白名单状态 | 报告里“后台系统能力”变为 9/9，电池无限制=YES |

## B4 存储有增有减（R31-B，解决 P14–P18）

**一句话：给每类数据设字节上限，超限就“先汇总、再删除”；报告发布后立刻删数据库副本；取消“删除即搬家”的 keep 表。** 稳态总占用目标 ≤ 2.5 GB，预热后第二个 24 h 增量 < 50 MB。

### 为什么会到 30 GB（推算）

清数据后 747 期数据库到 603 MB，约 0.8 MB/期。热表保留窗口是 5,000 期（MAX\_LOCAL\_HISTORY\_SAVE），而归档不跑就不删：两天约 2,880 期 → 数据库约 2.3 GB。每次导报告再复制一份同样大小的 snapshot.db，导出失败重试还会留下恢复副本；再加 WAL 和 .bak。几次导报告就会到十几到几十 GB。B0-3 的逐表字节上线后用实测替换这个推算。

### 预算表（写入 AppConfig，按分类强制）

| 分类 | 上限 | 超限动作 | 负责类 |
| --- | --- | --- | --- |
| 热数据库 | 1.2 GB | 按表优先级汇总后删除，最早的期先走 | DataRetentionGovernorR900 |
| WAL | 64 MB | wal\_checkpoint(TRUNCATE) | DatabaseCoordinator |
| 报告（全部已发布） | 512 MB（已有 PUBLISHED\_QUOTA\_BYTES） | 删最旧 | ReportPublicationCP12 |
| 报告数据库副本 | 0（发布后） | 发布校验通过即删 | ReportPublicationCP12 |
| 归档 CAS | 256 MB | 先进先出，只保留汇总 | ResearchRetentionStoreR30 |
| 备份 .bak | 1 个，24 h | 恢复证明通过后删 | 迁移协调器 |
| 缓存 | 64 MB | LRU | 各所有者 |

### B4-1 报告副本（P14）

ReportPublicationCP12.pruneTerminal 对当前作业 `continue`，cleanRaw 只在验证通过时清理，否则写 cleanup-pending 并永久保留。修改：

```java
// ReportExportServiceCP4：发布成功（READY_TO_SHARE 且 verifyPublishedZip==true）后立即调用
static void dropSnapshotAfterPublish(File job) throws IOException {
    for (String n : new String[]{"snapshot.db","snapshot.db-wal","snapshot.db-shm","snapshot.db-journal"}) {
        File f = ReportArchiveVerifierCP12.inside(job, n);
        if (f.exists() && !f.delete()) throw new IOException("REPORT_SNAPSHOT_DROP:" + n);
    }
}
```

- 当前作业也适用：ZIP 已封存校验，快照不再有用。
- cleanup-pending 状态超过 24 h：重新校验一次，仍失败则把整个作业标记 FAILED 并删除（用户可重新导出）。
- 快照改用 `VACUUM INTO` 生成（不含空闲页，通常比原库小），并且不再把 37 MB 源码调用索引每次内嵌（见 B8）。

### B4-2 取消 r30\_keep\_\* 搬家式保留（P15）

55 张受保护表的 BEFORE DELETE 触发器把行复制到 r30\_keep\_\*，而 keep 表自己禁止删除。这意味着研究跑得越多，数据库只会越大。替换为“终身充分统计量 + 限量原件”：

1. 迁移中 DROP 所有 `r30_keep_*_delete`、`*_clear`、`*_immutable_update`、`*_immutable_delete` 触发器。
2. 新表 r31\_lifetime\_stat(owner, key, n, hits, sum\_logloss, sum\_brier, first\_issue, last\_issue)，删除前把要删的行折叠进来（计数和均值可以无损累加），这就是报告里“终身累计”的来源。
3. 确实需要原件的对象（名人堂、冠军、证书、委员会基因组），按各自数量上限保留（例如冠军只留每个频率层前 32 个），与 UPGRADE\_DIAGNOSIS.md 第 8 节“永久完整保留（体积固定）”的承诺一致。
4. 已有 keep 表中的数据先折叠进 r31\_lifetime\_stat，然后 DROP TABLE。

### B4-3 热表删除不再依赖归档成功（P16）

RetentionTableRegistryCP12 的 ARCHIVE\_RAW\_TAIL 规则要求“已验证归档”才能删。保留正常路径，增加预算路径：热库 > 1.2 GB 且归档任务连续 30 分钟无进展时，进入 BUDGET\_FOLD：按期从最早开始，先把行折叠进 r31\_lifetime\_stat，再删除，每批 500 行，写一条回执（表、期号范围、行数、原因）。正在被未完成任务引用的行（v57\_research\_input\_pin\_r967 中状态未终结的）仍不删。各表优先级（先删谁）：事件日志 → 候选空间 → 标签 → 图表缓存 → 研究中间产物 → 开奖原始数据（最后，且至少保留 5,000 期）。

### B4-4 单表上限（P17、P18）

| 表 | 现状（12.6 h） | 新规则 |
| --- | --- | --- |
| v57\_runtime\_event\_r926 | 134,688 行 | 最近 24 h 或 50,000 行，取小者；之前只保留按小时汇总 |
| v57\_k3\_action\_space\_1200 | 85,859 行 | 最近 96 期（结算与诊断只用到最近几十期） |
| v57\_research\_action\_label\_r811 | 84,000 行 | 最近 96 期 + 终身统计 |
| r27\_chart\_cache | 72,458 行 | 每个 panel 只保留最新 1 份（时间线 panel 保留 2,000 行） |
| research\_row\_revision\_v2 | 316,363 行 | 只跟踪真正需要 generation 校验的表；定期删除孤儿行（source 不存在的） |
| r27\_task\_error、r30\_writer\_hold | 170 / 1,122 行 | 已有上限，保持 |

### B4-5 删除后真正释放空间

SQLite 删行后文件不会变小。一次性迁移：`PRAGMA auto_vacuum=INCREMENTAL` + 一次 VACUUM（在充电且屏幕关闭时由 StorageMaintenanceServiceR82 执行，需要等于数据库大小的空闲空间）。之后每次大批删除后执行 `PRAGMA incremental_vacuum(2048)` 和 `PRAGMA wal_checkpoint(TRUNCATE)`。

### B4 验收

| 项 | 通过线 |
| --- | --- |
| 72 h 浸泡 | 总占用 ≤ 2.5 GB；第 2–3 天日增量 < 50 MB |
| 导 5 次报告 | 每次导出完成 10 分钟内，总占用回到导出前 + 该 ZIP 大小以内；不存在 snapshot.db |
| keep 表 | 不存在 r30\_keep\_\* 表和对应触发器；r31\_lifetime\_stat 行数只随对象数增长，不随期数增长 |
| 预算路径 | 人为停掉归档任务 2 h，热库仍稳定在 1.2 GB 以内，并有 BUDGET\_FOLD 回执 |
| 单元测试 R31LifetimeFoldTest | 先折叠再删除后，终身命中率与全量重算结果完全一致 |

## B5 15 张图表全部打通（R31-B，解决 P19–P20）

**一句话：每张图只读有活生产者的表，每张图独立加载、独立超时，空图必须说出是哪个任务没产出、为什么。** 开奖类 4 张图安装后立刻有数据，研究类图在 B1 后 1 小时内有数据，进化类图在 B6 后有数据。

### 逐图核对（ChartsRepositoryR83.load 的 15 张）

| 图 | 现在读的表（快照行数） | 为什么空 | 改为读 | 何时有数据 |
| --- | --- | --- | --- | --- |
| 全部算法逐期实盘榜 | r27\_live\_shadow + r27\_live\_frozen（0 / 0） | LiveTask 被隔离 | 不变 | B1 后 |
| 算法实盘排行榜 | v57\_algorithm\_genome\_r908 + predraw\_r908（0） | R908 引擎已退役，无生产者 | r27\_policy + r27\_live\_frozen（按策略汇总） | B1 后 |
| 最佳算法累计命中率 | predraw\_r908（0） | 同上 | r27\_live\_frozen，取留出段第一名的累计曲线 | B1 后 |
| 出手资金 K 线 | v57\_exploration\_r83 + 影子观察（73 / 73） | 有数据，但出手行可能为 0 | 不变，空时显示“已结算 73 期，出手 0 期” | 立即 |
| 全部预测命中率走势 | 影子观察（73） | 需 ≥ 20 期已结算，已满足 | 不变 | 立即 |
| 出手命中率走势 | v57\_exploration\_r83 | 同出手 K 线 | 不变 | 立即 |
| 第 N 位号码 K 线 | draws（5,000） | 只可能因整页超时而空 | 不变 | 立即 |
| 第 N 位号码轨迹 | draws | 同上 | 不变 | 立即 |
| 号码遗漏热力图 | draws | 同上 | 不变 | 立即 |
| 第 N 位冷热号 | draws | 同上 | 不变 | 立即 |
| 预测概率校准 | 影子观察 selected\_hit\_p | 样本少 | 不变；分 10 档，每档标样本数 | 立即（样本少时带提示） |
| 主链算法参与权重 | v57\_research\_mainline\_contribution\_r922 | 旧链路 | su2\_mainline\_choice / su2\_mainline\_binding | 立即（为 0 时写明“无通过门的算法”） |
| 算法迭代进化事件 | research\_evolution\_event\_v41（0） | v41 无生产者 | B6 新表 r31\_generation | B6 后 |
| 深度学习训练误差 | atomic\_tool\_context\_r943（0） | R943 无生产者 | r26\_training\_step（3,047）+ r30\_atomnet\_sample（2,080） | 立即 |
| 算法父子谱系 | r974\_model\_lineage（0） | 旧表 | r27\_lineage | B1 后 |

另加 4 张，对应升级诊断书里一直“暂无”的四节（B6 产出）：

1. 一代比一代强：每次进化活动第 0 代冠军 vs 最终冠军，同一前推段的 log-loss 增益（mnat），点 + 90% 区间须条。
2. 算子成效：变异/交叉/融合/剪枝/新生各自的尝试数与子代胜父代比例。
3. 家族贡献：9 个探针家族 + F01–F20 在冠军里的占比与正增益块数。
4. 原子分级：原子按前推贡献分级的数量分布。

### 加载方式重写（ChartsActivityR83 + ChartsRepositoryR83）

- 每张图一个任务，串行提交到后台线程，各自一个 CancellationSignal，单图超时 3 s；读完一张立即渲染一张，不再等 15 张全部完成。
- 开奖类图先加载；开奖数据只读一次（现在就是共用 draws 列表）。
- 空图文案由生产者状态生成：从 r26\_task 读对应任务的 status / wait\_kind / root\_cause，例如“生产任务 r27-live 已隔离：WRITER\_BUSY，3 分钟后自动恢复”。
- 为慢查询加索引：r27\_live\_frozen(policy\_hash,status,hit)、r27\_lineage(created\_issue)，写在 SchemaR27 迁移里。
- 删除对 r908 / v41 / r943 / r974 / r922 表的所有图表读取（表本身在 B7 删）。

### B5 验收

| 项 | 通过线 |
| --- | --- |
| 全新安装 + 种子历史 | 打开图表页 3 s 内至少 8 张图为 READY |
| B1 上线 1 h 后 | 实盘榜、排行榜、谱系 3 张图为 READY |
| B6 上线 24 h 后 | 19 张图全部 READY，或为空时显示生产任务名与原因 |
| 单元测试 R31ChartSourceTest | 遍历每张图的 SQL，表名必须在“有生产者”白名单内；白名单由 grep INSERT 自动生成 |
| 单图超时 | 人为让一张图的查询卡 10 s，其余图正常显示 |

## B6 创造/进化引擎收敛 + 历史快速回放（R31-C，解决 P21–P22）

**一句话：只保留一个进化引擎（EvolutionLoopR27），其它 7 套改成它的“候选来源插件”；用已存的 5,000 期历史做滚动前推回放，不等实时开奖就能连续一代一代地跑。** 实时结算负责最后一道独立验证（FUTURE），不再是进化的唯一数据源。

### B6-1 为什么选 EvolutionLoopR27 做唯一引擎

它已经具备：策略语法树 PolicyAstR27、特征缓存 FeatureCacheR27、训练/挑选/留出三段切分（H\_TRAIN / H\_SELECT / H\_HOLDOUT）、曝光台账、冻结确认（FrozenConfirmationR30）、并行回放执行器，而且它的输出表（r27\_policy / r27\_lineage / r27\_live\_\*）就是 B5 图表要读的表。其它引擎各自独占写锁、各自有证书体系，这是 P09 写锁争抢和 P23 结构膨胀的主要来源。

### B6-2 插件化（改写，不重写）

```java
// strategyv2/CandidateSourceR31.java（新增）
public interface CandidateSourceR31 {
    String family();                                  // 如 "SU2_MODEL_GENOME"、"SU1_PROGRAM_TREE"、"PROBE_HMM"
    List<PolicyAstR27.Node> propose(EvolutionContextR31 ctx, int n, SplittableRandom rng); // 只生成候选，不写库
}
```

| 原引擎 | 处理 |
| --- | --- |
| ModelEvolutionSU2 | 保留模型基因组编码，包成 CandidateSourceR31("SU2\_MODEL\_GENOME")；删除它自己的选择与证书写入 |
| StrategyFoundrySU1 | 程序树生成器包成插件；停用其竞赛调度 |
| AlgorithmInventionLabR908、AssetGenesisEngineR915 | 一次性把已有基因模板导入为种子插件（现库为 0 行，只导出代码里的模板）；从调度移除 |
| LegacyGenerationR26、FoundryCampaignR26、ExpertWorkshopR26 | 专家 E01–E16 作为种子插件；停用各自的代际循环 |
| 9 个探针家族（MSDIR、BOCPD、HMM、LOGLIN、RELBAYES、KALMAN、REGMEM、RELMEM、CONTMAT）+ F01–F20 | 各自一个插件，生成对应族的叶子节点 |

停用方式：RuntimeCoordinatorR26 / RuntimeR27 不再注册这些任务（legacy-generation、foundry-su1、foundry-su2、expert-E01…E16 共 19 个任务），类文件先保留，B7 再删。研究线程数从“13 + 30 个任务抢 2 个 worker”变为“约 10 个任务抢 2 个 worker”。

### B6-3 一代的完整闭环

1. **创造**：算子调度器按上一轮成效分配名额（Thompson 采样，Beta(成功+1, 失败+1)），在 5 种算子间分配 64 个子代：变异（参数抖动、节点替换）、交叉（子树交换）、融合（两个父代概率的加权平均，权重在 TRAIN 上拟合）、剪枝、新生（从插件抽取）。算子每种至少 4 个名额，避免死锁。
2. **训练**：可学参数只在 H\_TRAIN 上拟合。
3. **结算**：在 H\_SELECT 上逐期回放，只用该期之前的开奖（沿用 PitGuardR30 的时点检查）。指标：10 类对数损失相对均匀分布的增益（mnat）、K3 命中率、Brier。
4. **反馈**：子代与父代在同一段同一组期上配对，配对差的 90% 下界 > 0 才计为“子胜父”；结果回写算子成效和家族成效。
5. **淘汰**：保留前 32 名为下一代父代（含复杂度惩罚：每个节点 − 0.05 mnat）；结构与行为去重（预测序列哈希相同视为同一个）。
6. **晋级申请**：每 20 代，把当前第一名送到 H\_HOLDOUT 一次；留出段对一个候选只看一次，防止反复试探。门的显著性水平按累计送检次数做 α 支出（沿用 su1\_alpha\_ledger）。通过的进入实时影子（r27\_live\_shadow），由实时开奖做 FUTURE 确认。
7. **滚动**：每新增 500 期已结算开奖，三段窗口整体前移一次；旧 HOLDOUT 的结论保留在台账中。

### B6-4 新增记录表（均有上限）

| 表 | 一行是什么 | 上限 |
| --- | --- | --- |
| r31\_generation | 一代：代号、数据集哈希、第 0 代冠军与当前冠军在同一 SELECT 段的增益、配对差与 90% 区间 | 512 点，抽稀保留首/末/最好 |
| r31\_operator\_stat | 算子 × 家族的尝试数、子胜父数 | 固定行数（5 × 家族数） |
| r31\_family\_stat | 家族在冠军中的占比、正增益块数 | 固定行数 |
| r27\_lineage（已有） | 父 → 子 | 只保留仍在种群或名人堂中的祖先链 |

### B6-5 计算预算（需实测确认）

评估一个候选 = 在 SELECT 段（约 1,000 期）上回放。特征由 FeatureCacheR27 预算，单候选估计 5–20 ms；一代 64 个约 0.3–1.3 s CPU。在每分钟给研究 15 s CPU 的预算下，估计每天 1,000 代以上。B6 上线后第一件事是用 r27\_thread\_cpu 实测这个数字并写进报告。

### B6 验收（这是创造能力的严格闭环）

| 测试 | 做法 | 通过线 |
| --- | --- | --- |
| R31NullDatasetTest | 20 份独立均匀随机序列（5,000 期），全流程跑 200 代 | 误晋级 ≤ 1/20 |
| R31PlantedMarginalTest | 植入“第 3 位号码 1/4/7 合计概率 0.34” | 20 份里 ≥ 18 份晋级；中位数 ≤ 1,500 期 |
| R31PlantedLagTest | 植入“第 5 位下一期以 +0.08 概率重复本期” | 结构家族（CONTMAT/跨位置流）在冠军中出现；晋级 |
| R31GenerationGainTest | 在植入数据上 | r31\_generation 的配对下界为正的代数 ≥ 1；在零数据上不得大于 5% |
| R31OperatorTableTest | 200 代后 | 5 种算子均有尝试，配比随成效变化 |
| 真机 24 h | 正常运行 | r31\_generation ≥ 200 行；算子表、家族表非空；升级诊断书第 1.1–1.4 节不再“暂无” |

说明：前两行的合成数据实验是报告第 2 章 syntheticControl 已有机制的扩展，用它证明“工厂会造、会验、会升级”。真实开奖上的结果，由同一套门如实写进报告。

## B7 精简（R31-C，解决 P21、P23）

**一句话：在 B6 稳定运行 72 h 后，删除已经没有生产者、也没有读者的表和类。** 当前 713 张表里 556 张为空；1,236 个 Java 文件、约 50.8 万行，其中按版本后缀分布在 R811、R906、R915、R932、CP12、SU1、SU2、R26–R30 等十几代。

做法（每一步单独提交、单独可回滚）：

1. **生成清单**：脚本 tools/r31/dead\_tables.py 对每张表统计三个数：源码里 INSERT/UPDATE/insertWithOnConflict 指向它的位置数（生产者）、SELECT 指向它的位置数（读者）、真机快照行数。生产者 = 0 且行数 = 0 的表进入候选。报告和图表里的只读引用需先在 B5/B8 里移除。
2. **停写 72 h**：候选表加 BEFORE INSERT 触发器 RAISE(ABORT,'R31\_RETIRED\_TABLE')，并在 r31\_task\_health\_event 里记录任何触发。有触发就说明还有隐藏生产者，该表移出候选。
3. **删表**：72 h 无触发 → 迁移里 DROP TABLE + 从 SchemaContractRegistry / RetentionTableRegistryCP12 / ResearchMigrationRegistryR952 的契约列表中删除。注意这三个注册表有“必须存在”校验，漏改会导致启动失败。
4. **删类**：B6 中已停用的 7 套引擎的调度与写库代码删除，只保留已包成 CandidateSourceR31 的生成部分。以编译通过 + 全部 host 测试通过 + 24 h 真机无新增错误为准。
5. **targetSdk 评估**：在单独分支把 targetSdk 从 28 升到 34，逐项处理通知权限、精确闹钟、前台服务类型；只有真机 72 h 对比存活率不下降才合并。
6. **大文件拆分不在本轮**：DbStore（916 KB）、Models（840 KB）、AutoFetchService（795 KB）拆分风险高、收益是可维护性而非功能，排到 R32。

### B7 验收

| 项 | 通过线 |
| --- | --- |
| 表数量 | 空表数从 556 降到 < 100，启动与迁移无报错 |
| 停写期 | 72 h 内 R31\_RETIRED\_TABLE 触发次数 = 0（有则逐一解释） |
| 代码 | 全部 host 测试绿；check\_runtime\_ddl.py 绿；R31ChartSourceTest 绿 |
| 研究任务数 | 注册的研究任务 ≤ 15 个 |

## B8 检测报告改造（R31-C，解决 P24）

**一句话：报告第一页就回答“每项能力现在能不能用、缺什么、为什么、改哪个文件”，并把报告体积从 11 MB ZIP / 93 MB 解压降到几 MB。**

### B8-1 能力状态页（报告第 0 章）

每一项能力一行，列固定：能力 · 生产任务 · 任务状态（含 root\_cause）· 最近产出时间 · 24 h 产出量 · 判定（运行中 / 有产出未验证 / 已通过门 / 停摆）· 停摆原因 · 对应修复（文件:方法 + 本方案问题编号）。能力清单：抓取、分析、赛前冻结、结算、在线校准、回放数据集、创造（各算子）、训练、父子反馈、淘汰、晋级、实时影子、原子世界、各矩阵、图表缓存、存储保留、报告。

### B8-2 31 个“未测量”指标接入采集源

M03–M05、M08–M21、M24–M26、M28–M30、M33–M36、Q01–Q08 现在是 SOURCE\_NOT\_YET\_MEASURED。在 MetricDefinitionsR29 里给每个指标加 `sourceSql` 和 `producerTask` 两个字段，并由 R31MetricSourceTest 检查：每个指标的 sourceSql 能在空库上执行、producerTask 存在于已注册任务。对应关系举例：M03/M05 ← 冻结表（B3 的 path 字段）；M12 ← r31\_task\_health\_event；M15 ← r31\_table\_bytes；Q06 ← r31\_generation；M33 ← B5 的图表状态。

### B8-3 体积

- source-index TSV（37 MB）每次内嵌 → 只写 SHA-256 和提交号；需要时从仓库按提交号重建。
- DIAGNOSTIC\_SCOPE.json（25 MB，其中 recentEvidence 25.6 MB）→ 每张表最多 200 行样本 + 总数，全量只在用户勾选“原始证据”时导出。
- R30\_REPORT.md 与 R30\_REPORT.json 内容重复（6 MB + 7.6 MB）→ Markdown 只写结论和表格，细节在 JSON。
- 目标：默认报告 ZIP ≤ 3 MB，解压 ≤ 20 MB。

### B8 验收

| 项 | 通过线 |
| --- | --- |
| 未测量指标 | SOURCE\_NOT\_YET\_MEASURED 的数量从 31 降到 0（样本不足可以是 INSUFFICIENT，但必须有数据源） |
| 第 0 章 | 每个停摆能力都有 root\_cause 和文件:方法 |
| 体积 | ZIP ≤ 3 MB；导出耗时 < 2 分钟 |

## 验收总表与自检清单

**闭环的定义：下面每一项都由新版报告里的一个字段或一个测试结果证明，编译通过不算验收。** 任一项不达标，对应版本不发布。

### R31-A 发布前（host / CI）

- [ ] R31RootCauseTest、R31TableBytesTest 通过
- [ ] R31ClaimBusyTest：写锁被占 3 s，13 个任务 0 隔离
- [ ] R31QuarantineExpiryTest、R31TransientClassifyTest、R31MatrixPartialTest、R31ContinuationOverrunTest 通过
- [ ] check\_runtime\_ddl.py 输出为空
- [ ] 现有 98 个 host 测试全部仍绿（尤其 R27LeakageTest、R27NullBaselineTest）
- [ ] `grep -rn F01_WRITE_FAILED` 的每个匹配已核对

### R31-A 真机 24 h

- [ ] 报告第 4 章：R27 任务 QUARANTINED 累计 < 30 分钟，每条隔离都有 root\_cause
- [ ] r27-replay-dataset 游标与最新期差 ≤ 5
- [ ] r26\_evolution、r27\_policy、r27\_lineage 持续增长
- [ ] writerHardViolations = 0；请求到落库 P95 < 5 s
- [ ] 线程快照中无 closeAvailableNonPrimaryConnections / waitForConnection 阻塞

### R31-B 真机 72 h

- [ ] 实时期赛前预测覆盖 ≥ 99%；结算滞后 ≤ 2 期
- [ ] 研究停摆 20 分钟内自愈（人为注入）
- [ ] 总占用 ≤ 2.5 GB，第 2–3 天日增量 < 50 MB
- [ ] 导 5 次报告后无 snapshot.db 残留
- [ ] 无 r30\_keep\_\* 表；R31LifetimeFoldTest 通过
- [ ] 图表页 3 s 内 ≥ 8 张 READY；R31ChartSourceTest 通过

### R31-C 真机 72 h + 合成数据

- [ ] R31NullDatasetTest：误晋级 ≤ 1/20
- [ ] R31PlantedMarginalTest：≥ 18/20 晋级，中位数 ≤ 1,500 期
- [ ] R31PlantedLagTest、R31GenerationGainTest、R31OperatorTableTest 通过
- [ ] r31\_generation 每天 ≥ 200 行；升级诊断书 1.1–1.4 节有数据
- [ ] 19 张图全部 READY 或显示生产任务与原因
- [ ] 空表 < 100；研究任务 ≤ 15 个
- [ ] 报告 ZIP ≤ 3 MB；SOURCE\_NOT\_YET\_MEASURED = 0

### 自检方法

每个版本由不参与编码的一方按本清单逐项对照报告原文勾选，并把报告 ZIP 的 SHA-256 写在发布说明里。任一项靠“代码已写”而非“报告已测”来勾选，视为未完成。

## 发布、回滚与风险

**每个版本都先覆盖安装（不清数据），并保留上一版 APK；涉及删除的迁移在执行前自动做一次数据库备份，备份只留 24 h。**

| 风险 | 影响 | 对策 |
| --- | --- | --- |
| B1 后 13 个任务同时复活，写锁压力骤增 | 实时链变慢 | 与 B2 同版；复活后前 30 分钟 worker 限 1 个 |
| B4 删除 keep 表和热表是不可逆的 | 丢失历史原件 | 先折叠为终身统计并由测试证明一致；迁移前备份；先只读 72 h |
| 首次 VACUUM 需要等于数据库大小的空闲空间 | 空间不足时失败 | 只在空闲 > 2× 库大小、充电、息屏时执行；失败不影响运行 |
| B6 停用 7 套引擎 | 旧页面引用的表不再更新 | B5 先重绑图表；旧类保留到 B7 |
| 厂商一键清理仍会杀进程 | 断续运行 | 无法在代码里绕过；依赖白名单引导和被杀告知 |
| 真实数据上没有任何候选通过门 | “高胜率”目标未达成 | 这是对数据的可信回答，不是缺陷；不通过降低门槛来“解决” |

回滚：R31-A 整体回滚（B1/B2 不拆）；R31-B 的 B3、B5 可单独回滚，B4 只能回滚代码、已删数据靠 24 h 备份；R31-C 的 B6 通过重新注册旧任务即可回到旧引擎。

说明：本方案里的行号、表名和数字来自提交 5237b265 与报告作业 ba5bcb7f。如果开发分支已经前移（main 当前为 e1413de9），开工前先按问题编号重新核对行号。标为“推算”的部分（隔离的内层原因、存储增长率、进化计算耗时）由 B0 实测确认。
