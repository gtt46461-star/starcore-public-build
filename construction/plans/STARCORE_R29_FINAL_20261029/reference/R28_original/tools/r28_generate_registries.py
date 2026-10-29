#!/usr/bin/env python3
"""Generates registries/r28_cards.json, registries/r28_acceptance_metrics.json, docs/08 and docs/09 tables."""
import json, sys, os

ROOT = sys.argv[1]

# ---------------------------------------------------------------- metrics
# id, kind(ENGINEERING must pass / QUALITY measured), name, current(10.29), target, report field
M = [
 ("M01","ENG","请求→提交 P95","23,523ms","≤5,000ms","R27_03/RSEC-033 requestToCommitP95"),
 ("M02","ENG","热路径 commit P95","8,676ms","≤200ms","PERFORMANCE_B05 WARM commit P95"),
 ("M03","ENG","冻结准时率（主链+观察）","93.0%","≥99%","R27_03 freezeOnTimeRate"),
 ("M04","ENG","24h 错过冻结截止","46","≤14","R27_03 deadlineMissed24h"),
 ("M05","ENG","实时链 F01_WRITE_FAILED / CP3_DB_BUSY_DEADLINE（24h）","≥8（30条样本中）","0","r28_issue_commit_failure 计数"),
 ("M06","ENG","抓取写入拒绝（24h）","BUSY_DEADLINE 出现","0","r27_fetch_reject / runtime-observation fetchWriteError"),
 ("M07","ENG","实时库写等待>200ms（每小时）","14,932（含研究）","≤5","R27_03 writeWaitsOver200msLastHour（R28 拆为 realtime/research 两项）"),
 ("M08","ENG","主线程卡顿>700ms（24h）","1 次 5,700ms（R27.3）","0","heartbeatSummary mainStallAt"),
 ("M09","ENG","开奖覆盖（最近30期）","30/30","30/30","RSEC-030 K1_Coverage"),
 ("M10","ENG","结算延迟","lag=0,0,0","lag=0","RSEC-052 lag"),
 ("M11","ENG","进程线程数","79","≤35","threadStacks THREAD 行数"),
 ("M12","ENG","同时运行的研究系统数","3（R908、R9xx/R26、R27）","1（R27）","R28-03 merge-status"),
 ("M13","ENG","从未运行任务","13","0","R27_03 neverRunTasks"),
 ("M14","ENG","隔离/卡住任务","1（incumbent-forecast）","0","R27_03 stuckTasks"),
 ("M15","ENG","实时库大小","1.72GB","≤1.0GB","STORAGE_CENSUS DATABASE（主库）"),
 ("M16","ENG","应用总存储","4.28GB","≤2.0GB","STORAGE_CENSUS total"),
 ("M17","ENG","实时库表数","603（全部同库）","≤80","DIAGNOSTIC_SCOPE tables（按库）"),
 ("M18","ENG","研究 CPU 份额（亮屏充电；口径=研究CPU时间÷墙钟）","0.095（相当于单核 9.5%，4 线程）","≥0.5×研究线程数（4 线程即 ≥2.0）","R27_03 researchCpuShare（R28 增加 Charging/Battery 两项）"),
 ("M19","ENG","进化代数 / 24h","553","≥500","R27_03 generationsLast24h"),
 ("M20","ENG","观察池跟踪数","24","≥128","R27_01 observers.tracking"),
 ("M21","ENG","同池负对照数","2","≥16","R27_01 sameperiodControls"),
 ("M22","ENG","观察冻结完成率","未单列","≥99%","R28-01 B observerFreezeRate"),
 ("M23","ENG","AtomNet 预训练样本 / 续训步数每小时","旧网络 93 步","≥50,000 / ≥55","r27_atomnet_state / r27_atomnet_epoch"),
 ("M24","ENG","每原子评分样本","94","≥1,000","r27_atom_score n"),
 ("M25","ENG","矩阵快照","0","每50代≥1","STRATEGY_MATRICES snapshots"),
 ("M26","ENG","校准模型（24h）","0","≥1","r27_calibration state=FITTED"),
 ("M27","ENG","消融结果行","0（NO_MEASURED_ABLATION）","每族/原子组/算子各≥1","R28-02 ablation"),
 ("M28","ENG","升级诊断书","空（需2000期）","有结论","UPGRADE_DIAGNOSIS verdict"),
 ("M29","ENG","报告新章 R28-01/02/03","无","3 章齐全，空数据只显示 INSUFFICIENT","R26_REPORT sections"),
 ("M30","ENG","报告导出","10MB ZIP，含 1.69GB 快照残留","ZIP≤50MB，快照导出后删除","STORAGE_CENSUS REPORTS"),
 ("M31","ENG","升级语义连续","4/4 EQUIVALENT","全部 EQUIVALENT 或给出首差","R27_04"),
 ("M32","ENG","主链红线","通过","每期 REAL≤1；无证书 0 REAL；门槛数值不变","RSEC-076 + H15"),
 ("M33","ENG","冷启动到首次出号","未单列","≤8s","PERFORMANCE_B05 COLD requestToDurable"),
 ("M34","ENG","覆盖安装数据不丢","模拟器通过","真机通过","R28-071 安装回执"),
 ("M35","ENG","24h FATAL/ANR/OOM","0（窗口不足24h）","0（连续24h）","crashSummary + 系统退出记录"),
 ("M36","ENG","图表首屏 P95","未测","≤300ms","r27_chart_cache 读耗时"),
 ("M37","ENG","施工卡真机证据","0/58（R27）","42/42","R28-03 卡片状态"),
 ("Q01","QUALITY","FUTURE 命中率（主链影子/观察池/对照池，Wilson 区间）","主链 29.7% [24.9%,…]；观察 30.7%","如实测量，不设通过线","R28-01 B"),
 ("Q02","QUALITY","已验证代际提升","0 / 279 代","如实测量","R27_02 verifiedImprovements"),
 ("Q03","QUALITY","观察榜首 vs 运气上限","43.75%（32期） vs 50.0%","如实测量","R28-01 C"),
 ("Q04","QUALITY","e 过程过线数（影子+观察）","0","如实测量","r27_eprocess state"),
 ("Q05","QUALITY","AtomNet 留出 BSS 与区间","无","如实测量","r27_brier_pair"),
 ("Q06","QUALITY","USEFUL 原子数 vs 纯随机期望","无","如实测量","r27_atom_score"),
]

# ---------------------------------------------------------------- cards
# id, batch, title, files/methods, change, test, metrics, deps
C = [
 ("R28-001","B0","固定施工基线","construction/r27-20261028@f9bdddb 全仓","从该提交开 construction/r28-*；记录将修改文件的 before SHA256 到 evidence/r28/baseline.json","脚本比对","M37","—"),
 ("R28-002","B0","验收仪表","新增 assets/r28/r28_acceptance_metrics.json、r28_cards.json；ReportR27","构建时把 registries/ 打包进 assets；运行时逐项求值","HOST：空库全部 INSUFFICIENT","M29,M37","R28-001"),
 ("R28-010","B1","研究库独立","新增 ResearchDatabaseR28（starcore_research_r28.db, WAL）；StoreR27 建表改到新库","r27_* 表迁移：ATTACH 只读复制→逐表行数+哈希比对→回执→主库 DROP；失败保留旧表","HOST：迁移、重复迁移、中断恢复","M05,M07,M15,M17","R28-001"),
 ("R28-011","B1","研究读写改道","ResearchWriteQueueR27、ResearchPoolR27.Worker(ro)、MainlineBridgeSU2、ReportDataR27、ChartCacheR27","写只进 research.db；开奖/结算经 realtime 只读连接读取；删除 p0Writing 让路","HOST：并发写压力（实时写 + 研究满负荷）","M05,M07","R28-010"),
 ("R28-012","B1","实时链事务合并","新增 IssueCommitR28；AnalysisRequestStoreR36.bindHistoryHash(第742行)、V51RuntimeOrchestrator.commitCanonicalBundleVerifiedR368、R932 bundle capture/domain route","一期一个 BEGIN IMMEDIATE；失败按步骤记 r28_issue_commit_failure；R932 domain route 若仅服务已退役链则删除","HOST：注入每一步失败整体回滚；真机 1440 期","M01,M02,M05","R28-011"),
 ("R28-013","B1","观察冻结准备—冻结分离","LiveShadowsR27、新表 r28_prepared","开奖后立即准备下一期全部观察/对照 K3；截止前一次批量 INSERT；迟到记 PREPARE_LATE 不补","真机 1440 期","M03,M04,M22","R28-011"),
 ("R28-014","B1","线程优先级","AutoFetchService 通道创建处；ResearchPoolR27","实时链 FOREGROUND；研究 BACKGROUND；研究线程数 cores−2（2..6）；温控降级","HOST：优先级断言；真机 CPU 采样","M01,M18","R28-001"),
 ("R28-015","B1","主线程渲染","MainActivity.buildDecisionV20Text/buildDecisionRenderKey/removePnlText","确认一次编译正则生效；文本拼接移到 eth_k3_ui_read_channel","真机 StrictMode + mainStall 采样","M08","R28-001"),
 ("R28-016","B1","抓取写入独立短事务","AutoFetchService 落库路径；r27_fetch_reject","只写 draw 行与回执；拒绝原因计数","HOST + 真机","M06,M09","R28-011"),
 ("R28-020","B2","旧系统种子与档案导出","LegacyPolicyExportR27；IdeaI12LegacyAssetR27","R908 15 算法、R26 F01–F24/E01–E16、CP12 学习器参数→I12 种子或 LEGACY_NOT_REPRODUCIBLE 档案","HOST：逐个复现或给原因","M12","R28-010"),
 ("R28-021","B2","停用 R908 自主研究","AutoFetchService：researchThreadR908 创建(1417行)、看门狗重建(16180–16186行)、researchSelfLoopR908(2245/2455行)","不创建、不重建、不 post；保留 settlement_recovery 线程","HOST：线程清单断言；真机","M11,M12","R28-020"),
 ("R28-022","B2","停用 R912/R9xx 研究调度","ResearchLearningContinuityR912（AutoFetchService 5274–5304行）；R932 校准/固定窗口；R967 队列","迁移置 r27_legacy_research_enabled=0 并移除默认值 1；调度入口删除","真机 24h 无旧 lane 写入","M12,M13","R28-020"),
 ("R28-023","B2","R26 协调器只留主链任务","RuntimeCoordinatorR26（第64–65行任务表、第118行启动 R27）；ExpertWorkshopR26；FoundryCampaignR26；LegacyGenerationR26；IncumbentLoopSU2；CooperativeContinuationR26","删除研究/铸造/专家任务与线程；RuntimeR27 改由 AutoFetchService.startWorkR3610OffMain 启动","HOST：任务表断言；真机线程清单","M11,M12,M13,M14","R28-020"),
 ("R28-024","B2","13 个从未运行任务处置","RuntimeCoordinatorR26 任务表；RuntimeR27 r27-calibration","逐项：并入 R27（列出对应任务）或删除；r27-calibration 由 R28-044 激活","报告 neverRunTasks","M13","R28-023"),
 ("R28-025","B2","守护线程合并","starcore_b07_*、starcore_r817_liveness、r927-*、CP4-recovery-*、starcore_ui_diagnostics、starcore-ui-funnel-deadline","合并为 HealthMonitorR28 单线程调度","真机线程清单","M11","R28-021"),
 ("R28-026","B2","表与存储瘦身","RetentionR26、DataRetentionGovernorR900、RetentionTableRegistryCP12；新增 starcore_archive_r28.db","退役表 VACUUM INTO 归档库后删除；旧 .bak 在新备份校验后删除；报告快照导出后删除，目录≤300MB","真机前后存储普查","M15,M16,M17,M30","R28-021,R28-022,R28-023"),
 ("R28-027","B2","施工总账替换","diagnostics/construction-current-*；CompleteReportFileR927","1832/299 冻结为历史档案；报告改读 R28 卡与指标","导出报告","M37","R28-002"),
 ("R28-030","B3","研究池满载","ResearchPoolR27；ServiceTotalsR26","分库后去掉写让路；充电/电池分开统计 CPU 份额","真机 60 分钟","M18,M19","R28-011,R28-023"),
 ("R28-031","B3","每期增量回放","ReplayDatasetR27、FeatureCacheR27、HoldoutLedgerR27；设置页导入历史 CSV","开奖后 ≤2s 完成追加/增量特征/补评；真机导入 1 万期","HOST：增量=全量逐位一致","M19","R28-030"),
 ("R28-032","B3","进化多样性与复杂度","EvolutionLoopR27、IslandModelR27、EliteArchiveR27、BirthGateR27","自适应复杂度 3–8；每族最低配额 4%；MAP-Elites 覆盖率与冠军相关度入报告；冠军不换原因入报告","HOST：植入信号 H06/无信号 H07 复跑","M19,Q02","R28-031"),
 ("R28-033","B3","观察池 128 + 对照 16","LiveShadowsR27、RuntimeR27","前 128 名（每族≤16）+ 8 个 I00 + 8 个打乱冠军；逐期冻结结算","真机 24h","M20,M21,M22","R28-013"),
 ("R28-034","B3","观察池晋级通道","SequentialQualificationR27、CertificateGateR27、MainlineBridgeSU2","首次进入领取永久全局 α 序号；只喂 FUTURE；过线→CHALLENGER→证书门（门槛不变）","HOST：1000 零优势观察×20000 期错误授证≤5%；植入 40% 信号可过线","M32,Q04","R28-033"),
 ("R28-035","B3","失败研究闭环","新增 ResearchFindingR28；IdeaI10FailureRepairR27","连挂≥2 触发；输出只改出生配额；子代 FUTURE N=0","HOST","Q02","R28-033"),
 ("R28-040","B4","AtomNet 正式接管","AtomNetR27、AtomNetTrainerR27、AtomWorldR27；AtomicToolWorldR943.java:139","≥5 万样本预训练；每期续训；回滚；删除旧逐期 SGD；BSS 图","HOST H16 复跑；真机 24h","M23,Q05","R28-031"),
 ("R28-041","B4","原子配对评分","AtomScoreR27、AtomReplayR27","回放样本≥1000；与打乱对照配对区间；USEFUL/NEUTRAL/HARMFUL/MASKED","HOST：纯噪声原子 HARMFUL 率≤2.5%","M24,Q06","R28-031"),
 ("R28-042","B4","派生原子与分子真机验证","DerivedAtomR27、AtomWorldR27（写 r27_molecule）","每50代派生、每代32分子；无产出给原因","真机 24h","M27","R28-041"),
 ("R28-043","B4","矩阵快照与矩阵清单","MatrixSnapshotSU2.capture、MatrixStoreSU2","由 R27 冠军每50代生成；Teacher 矩阵退役","HOST：1200/100 一致性","M25","R28-032"),
 ("R28-044","B4","校准出模型","LiveShadowsR27.calibrate（第193行只取 ACTIVE）、CalibrationRowsR27","对象扩展为影子+观察前32；拟合用回放、验证用 FUTURE","真机 24h ≥1 模型","M26,M13","R28-033"),
 ("R28-045","B4","九专家/NumberQ/Ranker 处置","V51PermutationNumberModelEngine、V51ExpertGateEngine、V51ActionRankerEngine 报告入口","可回放训练的进 I08；其余从报告/界面退役；生产链不改","HOST","M29","R28-031"),
 ("R28-050","B5","报告章 R28-01 胜率为什么低","新增 ReportR28；ReportDataR27","docs/07 7.1 A–G；结论三选一自动选择","HOST：三类数据各生成一次","M29,Q01,Q03","R28-033"),
 ("R28-051","B5","报告章 R28-02 功能效果诊断书","ReportR28.nextAction 规则表","24 功能逐行：运行量/状态/效果/结论/根因/下一步/卡号","HOST：规则表单测","M29","R28-050"),
 ("R28-052","B5","报告章 R28-03 施工与验收核对","ReportR28；assets/r28/*","43 项指标逐项判定；卡状态只认真机指标","HOST","M29,M37","R28-002"),
 ("R28-053","B5","升级诊断书用回放","UpgradeDiagnosisSU2、DiagnosisStoreSU2","输入改为 ReplayDatasetR27；实盘段单列","HOST","M28","R28-031"),
 ("R28-054","B5","消融汇总","StoreR27(r27_ablation)、ReportDataR27 ABLATION；生产链离线消融","每20代按族/原子组/算子汇总；R36 漏斗模块离线消融","HOST","M27","R28-032"),
 ("R28-055","B5","报告精简","CompleteReportFileR927、ReportExportServiceCP4","默认不含 R925 静态审计/历史需求原文；每章一句话结论；快照导出后删除","真机导出","M30","R28-052"),
 ("R28-060","B6","观察榜与运气上限","ChartsRepositoryR83（全部算法逐期实盘榜）、ChartCacheR27","每行 HR+区间；E_max(k,N) 线；对照灰色标出","截图","M36,Q03","R28-033"),
 ("R28-061","B6","功能效果页与真实阻断","MainActivity 功能卡；DiagnosticStateRendererR906","每个功能卡显示 R28-02 一行；每个 0 显示真实阻断原因（USR-28）","截图","M29","R28-051"),
 ("R28-062","B6","图表缓存首屏","ChartCacheR27（读 research.db）","预聚合；首屏 P95≤300ms","真机","M36","R28-011"),
 ("R28-070","B7","HOST 测试套件","app/src/test/…；tools/r27、tools/r28","R27 H01–H18 复跑 + R28 新增（迁移、事务合并、晋级通道错误率、配对评分、报告规则表）","CI","全部 ENG 中 HOST 可测项","全部"),
 ("R28-071","B7","真机 24h 矩阵","V2353A（API34）+ 另一品牌","亮屏/息屏/锁屏/充电/电池/断网恢复/覆盖安装/强杀/低内存/连续24h；导出报告","真机","M01–M37","R28-070"),
 ("R28-072","B7","正式签名（需用户决定）","CI 配置；construction/su1-20261022 的私钥","用户把正式 R13 私钥作为仓库 Secret 提供，或确认继续用 CI 临时密钥","签名指纹核对","M34","—"),
 ("R28-073","B7","发布与回滚","release/R28_ROLLBACK_PLAN.md","APK 哈希、签名指纹、versionCode、迁移回执；可回滚到 R27.4 且数据不丢（研究库回滚时可重新迁回）","真机覆盖安装/回滚","M34","R28-071"),
]

BATCH = {
 "B0":("基线与验收仪表","1—2"),
 "B1":("数据库拆分与实时链（P0）","5—8"),
 "B2":("统一单系统：退役旧研究链、瘦身","5—8"),
 "B3":("研究引擎满血","5—7"),
 "B4":("深度学习、原子、分子、矩阵、校准","4—6"),
 "B5":("检测报告三章与修复","4—6"),
 "B6":("图表与界面","2—3"),
 "B7":("测试、真机、发布","4—6"),
}

reg = os.path.join(ROOT, "registries"); os.makedirs(reg, exist_ok=True)
metrics = [dict(id=i, kind=("ENGINEERING_MUST_PASS" if k == "ENG" else "QUALITY_MEASURED_NO_PASS_LINE"), name=n, current_20261029=c, target=t, report_field=f) for i,k,n,c,t,f in M]
json.dump({"plan":"STARCORE_R28_FINAL_20261029","report_job":"fb32242320b1eedeb83e0c174594c1e20a8fa9dd0e02dd5aab70f62e5c20550f","report_source_commit":"7d3e0e2271ce2a734ca69f2fca8527c00222bba7",
           "rule":"An ENGINEERING metric passes only when measured on a physical device report; HOST results never mark it PASS. QUALITY metrics are reported as measured and have no pass line.",
           "metrics":metrics}, open(os.path.join(reg,"r28_acceptance_metrics.json"),"w",encoding="utf-8"), ensure_ascii=False, indent=1)
cards = [dict(id=i, batch=b, title=t, files=f, change=c, test=te, metrics=[x.strip() for x in m.split(",")], depends=[x.strip() for x in d.split(",")] if d != "—" else [], status="未施工") for i,b,t,f,c,te,m,d in C]
json.dump({"plan":"STARCORE_R28_FINAL_20261029","baseline":"construction/r27-20261028@f9bdddb (report source 7d3e0e2)",
           "batches":[dict(id=k, title=v[0], person_days=v[1]) for k,v in BATCH.items()],
           "status_rule":"未施工 → 已修改待真机验证（代码合入 + HOST 通过）→ 已验证（绑定的全部 ENGINEERING 指标在真机报告 R28-03 章 PASS）",
           "cards":cards}, open(os.path.join(reg,"r28_cards.json"),"w",encoding="utf-8"), ensure_ascii=False, indent=1)

# ---------------------------------------------------------------- docs/08
L = []
L.append("# 08 施工卡与批次（42 卡、8 批）\n")
L.append("机器清单：`registries/r28_cards.json`（同内容，由同一份数据生成）。状态一律从\"未施工\"开始：代码合入且 HOST 通过 → \"已修改待真机验证\"；绑定的工程指标在真机报告 R28-03 章全部 PASS → \"已验证\"。没有第四种状态。\n")
L.append("## 8.1 批次顺序与依赖\n")
L.append("```\nB0 → B1 → B2 → B3 → B4\n          │     └→ B5 → B6\n          └──────────────→ B7（每批完成后各跑一次真机，最终 24h 矩阵）\n```\n")
L.append("| 批次 | 内容 | 初估人日 |\n|---|---|---:|")
for k,v in BATCH.items(): L.append(f"| {k} | {v[0]} | {v[1]} |")
L.append("\n合计约 30—46 人日（比 R27 的 46—72 人日少，因为 R28 以删除和接线为主，研究算法核心已在 R27 写好）。这是工程量估计，不含等待自然样本的时间，也不是胜率保证。\n")
L.append("**最小可见里程碑 = B1 + B2。** 完成后第一份真机报告应看到：M01 出号 P95 从 23.5 秒降到 5 秒内、M07 写等待从每小时 14,932 次降到个位数、M11 线程从 79 降到 35 以内、M12 研究系统从 3 套变 1 套、M13 从未运行任务归零、M18 研究 CPU 从 9.5% 上升。\n")
cur = None
for i,b,t,f,c,te,m,d in C:
    if b != cur:
        cur = b; L.append(f"\n## {b} {BATCH[b][0]}\n"); L.append("| 卡号 | 施工项 | 文件/方法 | 改动 | 测试 | 验收指标 | 依赖 |\n|---|---|---|---|---|---|---|")
    L.append(f"| {i} | {t} | {f} | {c} | {te} | {m} | {d} |")
L.append("\n## 8.2 每张卡的交付物（缺一项不得前进状态）\n")
L.append("1. 代码 diff 与 before/after SHA256；\n2. 新增/修改表的 DDL 与迁移回执；\n3. 测试命令、返回码、原始日志（evidence/r28/<卡号>/）；\n4. 真机报告中绑定指标的 JSON 片段（R28-03 章自动给出）；\n5. 未完成项与原因。\n")
L.append("## 8.3 红线（任何卡都不得触碰）\n")
L.append("- `DecisionGateEngine`、`SettlementEngine`、`V51DecisionGateCore`、`CertificateBundleServiceR929`、`AppConfig` 的门控/赔率逻辑不改（H15 SHA256 核对）；\n- 主链：HR≥34% 且质量门通过才可 REAL；每期最多 1 次 REAL；无频率配额；禁止追期、马丁、加倍、补单、多位置真实、动态 K；\n- PIT：任何预测必须在截止前冻结，不得赛后补；\n- 不得把 REPLAY/HOLDOUT 成绩写成 FUTURE；不得删除负对照；不得手写质量结论。\n")
open(os.path.join(ROOT,"docs","08_施工卡与批次.md"),"w",encoding="utf-8").write("\n".join(L)+"\n")

# ---------------------------------------------------------------- docs/09
L = []
L.append("# 09 验收闭环与真机测试\n")
L.append("机器清单：`registries/r28_acceptance_metrics.json`。**37 项工程指标（M01—M37）必须在真机报告里达到目标；6 项质量指标（Q01—Q06）只如实测量、不设通过线。** 新版报告 R28-03 章会自动逐项给出当前值与 PASS/FAIL/INSUFFICIENT。\n")
L.append("## 9.1 验收表（10.29 现值 → R28 目标）\n")
L.append("| 编号 | 类型 | 指标 | 10.29 现值 | 目标 | 报告字段 |\n|---|---|---|---|---|---|")
for i,k,n,c,t,f in M:
    L.append(f"| {i} | {'工程' if k=='ENG' else '质量'} | {n} | {c} | {t} | {f} |")
L.append("""
## 9.2 问题 → 修改 → 指标 闭环

| 用户看到的问题 | 根因（docs/00） | 卡 | 指标 |
|---|---|---|---|
| 运行不流畅、出号慢 | 单库写锁争用；R26 线程抢 CPU | R28-010/011/012/014/023 | M01、M02、M05、M07 |
| 模块冲突 | 三套研究系统并行 | R28-020—R28-025 | M11、M12、M13、M14 |
| 算法不每期训练/反馈/变异/融合 | 研究 CPU 仅相当于单核 9.5%；观察池只 24 个 | R28-030/031/033 | M18、M19、M20、M22 |
| 创造的算法从不进主链 | 影子池只认留出通过者；观察池无晋级路 | R28-034 | Q04（由数据决定） |
| 深度学习不学 | 旧网络每期 1 次 SGD；新网络未接管 | R28-040 | M23、Q05 |
| 原子、矩阵、校准、消融都是空的 | 依赖链断（只认 ACTIVE 影子、只在旧铸造完成时生成） | R28-041/043/044/054 | M24、M25、M26、M27 |
| 报告看不出为什么胜率低、怎么改 | 报告只罗列原始记录 | R28-050/051/052/053 | M28、M29 |
| 施工方案永远完成不了 | 1832 条需求与真机指标不对应 | R28-002/027 | M37 |
| 胜率低 | 数据无可利用规律（docs/01） | 无法用施工改变；R28-050 如实报告 | Q01、Q02、Q03 |

## 9.3 真机矩阵（R28-071）

设备：V2353A（vivo，Android 14 / API 34）+ 至少 1 台其他品牌。

| 场景 | 时长 | 必看指标 |
|---|---|---|
| 覆盖安装 R27.4 → R28（保留数据） | — | M31、M34；迁移回执全部一致 |
| 亮屏 + 充电 | 2h | M01—M07、M18 |
| 息屏 + 电池 | 2h | M03、M04、M09、M10 |
| 锁屏 / 划掉任务 / 强杀后自启 | 各 1 次 | M35、M10 |
| 断网 10 分钟后恢复 | 1 次 | M09、M06 |
| 低内存（开启其他大应用） | 30 分钟 | M35 |
| **连续 24 小时** | 24h | 全部 M01—M37；导出报告 |
| 回滚到 R27.4 再升回 R28 | — | M34 |

## 9.4 四项签收

| 签收 | 条件 |
|---|---|
| 工程完成 | 42 卡全部"已修改待真机验证"；R28-070 全部通过；构建身份一致 |
| 能力完成 | 真机 24h 报告中 M01—M37 全部 PASS |
| 质量报告 | R28-01 章给出三选一结论并可回溯到数据行；Q01—Q06 有数值 |
| 发布 | 正式签名（或用户书面确认临时密钥）；可回滚 |

**签收中没有"胜率达到 X%"这一项。** 胜率是测出来的，不是施工出来的；R28-01 章给出什么结论，就是什么结论。
""")
open(os.path.join(ROOT,"docs","09_验收闭环与真机测试.md"),"w",encoding="utf-8").write("\n".join(L)+"\n")
print(len(M), len(C))
