#!/usr/bin/env python3
"""R27 evidence writer (R27-001 / docs/11 11.2). Usage: make_evidence.py [--ci-run URL]

Writes, from the working tree and git 1713d11:
  evidence/r27/baseline.json        before (1713d11) and after SHA-256 of every file this construction changed or added
  evidence/r27/source_audit.json    the SOURCE chapter input (file counts, per-file hashes)
  evidence/r27/merge_status.json    README §4 R26→R27 rulings, each with its implementing cards and status
  evidence/r27/<card>/card.json     the five docs/11 11.2 deliverables per card (files+hashes, DDL, tests, report field, gaps)
  registries/r27_cards.json         (plan copy on this branch) status per card

Status rule (docs/11): a card leaves 未施工 only when all five deliverables exist; HOST/CI evidence gives at most
"已修改待设备验证"; "已验证" needs real-device evidence, which this script never claims.
"""
import hashlib, json, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = "1713d114ebbd07ebd6544a29acfbce1bd883215c"
PLAN = ROOT / "construction/plans/STARCORE_R27_FINAL_20261028"
EV = ROOT / "evidence/r27"
S2 = "app/src/main/java/com/ethk3/novaonepro/strategyv2/"
M = "app/src/main/java/com/ethk3/novaonepro/"
T = "app/src/test/java/com/ethk3/novaonepro/strategyv2/"
DONE, TODO = "已修改待设备验证", "未施工"

def git(*a, binary=False):
    r = subprocess.run(["git", "-c", "core.quotepath=off", *a], cwd=ROOT, capture_output=True)
    return r.stdout if binary else r.stdout.decode()

def sha(b): return hashlib.sha256(b).hexdigest()

def before(path):
    r = subprocess.run(["git", "cat-file", "-e", BASE + ":" + path], cwd=ROOT, capture_output=True)
    return sha(git("show", BASE + ":" + path, binary=True)) if r.returncode == 0 else None

def after(path):
    p = ROOT / path
    return sha(p.read_bytes()) if p.is_file() else None

def fileinfo(path):
    return {"path": path, "before_sha256": before(path), "after_sha256": after(path),
            "change": "NEW" if before(path) is None else ("UNCHANGED" if before(path) == after(path) else "MODIFIED")}

JUNIT = "./gradlew :app:testDebugUnitTest (local: tools/r27 runner, JUnitCore)"
HOSTLOG = "evidence/r27/host/junit_r27_local.log"
MIG = "evidence/r27/host/migration.log"
H18 = "evidence/r27/host/h18_compare.log"

def t(name, cls=None, log=HOSTLOG, cmd=None, marker=None):
    return {"test": name, "class": cls, "command": cmd or (JUNIT + (" --tests " + cls if cls else "")), "log": log, "marker": marker or name}

def snippet(src, field):
    return {"source": src, "field": field}

# ------------------------------------------------------------------ card map (hand-written, reviewed against the code)
# files: the card's code; tables: DDL it adds/uses; tests; report: chapter/field; gaps: what is still missing.
C = {
 "R27-001": dict(files=["tools/r27/make_evidence.py"], tables=[], tests=[t("BASELINE_HASHES", cmd="python3 tools/r27/make_evidence.py", log="evidence/r27/baseline.json", marker="before_sha256")],
                 report=snippet("evidence/r27/source_audit.json → assets r27/source-audit.json", "SOURCE.files[].before_sha256/after_sha256"), gaps=[]),
 "R27-002": dict(files=[S2 + "RuntimeCoordinatorR26.java", S2 + "ResearchPoolR27.java", S2 + "SchemaR27.java", S2 + "WaitKindR27.java"], tables=["r27_thread_cpu", "r27_task_error", "r26_task.wait_kind/wait_detail/wait_since"],
                 tests=[t("M2_GUARDED_COLUMNS", log=MIG, cmd="tools/r27/run_host_tests.sh"), t("WAITKIND_PASS", "R27QualityTest")],
                 report=snippet("ReportR27.runtime", "R27_03_CONTINUOUS_RUNTIME.researchCpuShare / waitKindDistribution"),
                 gaps=["改造前研究CPU占比（预期≈4%）的对照值只能在真机上用旧版本测得（DEVICE）"]),
 "R27-003": dict(files=[S2 + "ResearchPoolR27.java", S2 + "RuntimeCoordinatorR26.java", S2 + "RuntimeR27.java"], tables=["r27_thread_cpu"], tests=[],
                 report=snippet("ReportR27.runtime", "researchCpuShare, researchThreads"),
                 gaps=["无多线程 HOST 测试（需要 Android SQLite）；研究CPU占比≥50%×线程数须真机60分钟测量"]),
 "R27-004": dict(files=[S2 + "ResearchWriteQueueR27.java"], tables=["r27_write_wait"], tests=[],
                 report=snippet("ReportR27.runtime", "writeWaitsOver200msLastHour, freezeOnTimeRate"),
                 gaps=["写队列让路行为无 HOST 测试（需要 Android SQLite）；冻结准时率/写等待须真机测量"]),
 "R27-005": dict(files=[S2 + "RuntimeCoordinatorR26.java"], tables=["r26_session"], tests=[t("R27-005_PASS", "R27QualityTest"), t("WAITKIND_PASS", "R27QualityTest")],
                 report=snippet("ReportR27.runtime", "waitKindDistribution（无 CRITICAL_DEADLINE）"),
                 gaps=["会话状态不再长期 BLOCKED:CRITICAL_DEADLINE 只能在运行中的 App 上观察（emulator/DEVICE）"]),
 "R27-006": dict(files=[S2 + "WaitKindR27.java", S2 + "RuntimeCoordinatorR26.java", S2 + "RuntimeR27.java"], tables=["r26_task.wait_kind"], tests=[t("WAITKIND_PASS", "R27QualityTest")],
                 report=snippet("ReportR27.runtime", "waitKindDistribution, stuckTasks, neverRunTasks"), gaps=[]),
 "R27-007": dict(files=[S2 + "WaitKindR27.java", S2 + "RuntimeCoordinatorR26.java", M + "ChartsActivityR83.java"], tables=["r27_task_error", "r26_task.error_window_start/error_count"],
                 tests=[t("WAITKIND_PASS", "R27QualityTest")], report=snippet("ReportR27.runtime", "waitKindDistribution.QUARANTINED"), gaps=[]),
 "R27-008": dict(files=[S2 + "LiveShadowsR27.java", S2 + "RuntimeR27.java", S2 + "RuntimeCoordinatorR26.java"], tables=["r27_live_frozen", "r27_opportunity"],
                 tests=[t("H15_SQL_FROZEN_REWRITE", log=MIG, cmd="tools/r27/run_host_tests.sh")], report=snippet("ReportR27.runtime", "freezeScheduled24h, deadlineMissed24h"),
                 gaps=["INCUMBENT_MISSING_AFTER_OPEN=0、DEADLINE_MISSED≤1% 须在运行中的 App 上测量（emulator/DEVICE）"]),
 "R27-009": dict(files=[S2 + "ResearchPoolR27.java", S2 + "RuntimeCoordinatorR26.java"], tables=[], tests=[], report=snippet("r27_task_error", "无 fence 失效写入"),
                 gaps=["启动/停止顺序与连接泄漏只能在 Android 进程上验证（emulator/DEVICE）"]),
 "R27-010": dict(files=[S2 + "OperatorGoldenR27.java", "app/src/main/assets/r27/golden_512.bin", "tools/r27/make_golden.py"], tables=["r27_semantic_fingerprint"],
                 tests=[t("H10_PASS", "R27QualityTest")], report=snippet("ReportR27.upgrade", "R27_04_UPGRADE_CONTINUITY.rows"), gaps=[]),
 "R27-011": dict(files=[S2 + "SemanticAbiR27.java", S2 + "LiveShadowsR27.java", S2 + "LegacyContinuityR27.java"], tables=["r27_semantic_fingerprint", "r27_abi_continuity"],
                 tests=[t("H11_PASS", "R27QualityTest"), t("H10_PASS", "R27QualityTest")], report=snippet("ReportR27.upgrade", "equivalent/changed/rows[].firstDiffIssue"), gaps=[]),
 "R27-012": dict(files=[S2 + "LegacyReplayR26.java", S2 + "LegacyContinuityR27.java"], tables=["r27_legacy_roster_alias"], tests=[],
                 report=snippet("ReportR27.upgrade", "legacyRuntimeBlockedByNumericalAbi, r26LiveModelsArchivedBySourceAbi"),
                 gaps=["BLOCKED_NUMERICAL_ABI_PRESERVED=0 须在已有 R26 数据的设备上升级后观察", "R26 live_model 身份仍含源码ABI（单列计数，未改）"]),
 "R27-013": dict(files=[S2 + "ReplayDatasetR27.java"], tables=["r27_replay_dataset"], tests=[t("R27-013_PASS", "R27ReplayTest"), t("H03_PASS", "R27ReplayTest")],
                 report=snippet("ReportDataR27/RuntimeReportR26", "REPLAY.r27_replay_dataset(rows,segments)"), gaps=[]),
 "R27-014": dict(files=[S2 + "FeatureCacheR27.java", M + "AtomReplayR27.java"], tables=[], tests=[t("H01_LEAVES_PASS", "R27LeakageTest"), t("H01_ATOMS_PASS", "R27LeakageTest")],
                 report=snippet("ReportR27.evolution", "rejectReasons.CAUSAL"), gaps=[]),
 "R27-015": dict(files=[S2 + "ReplayEvaluatorR27.java", S2 + "ReplayScoreR27.java"], tables=["r27_replay_score"], tests=[t("H02_PASS", "R27ReplayTest"), t("REPLAY_SPEED", "R27ReplayTest")],
                 report=snippet("ReportR27.runtime", "candidateReplayP95Ms"), gaps=["≤50ms 是手机目标；HOST 实测 4.5ms，真机须复测"]),
 "R27-016": dict(files=[S2 + "SplitR27.java", S2 + "HoldoutLedgerR27.java"], tables=["r27_holdout_exposure"], tests=[t("H07_PASS", "R27EvolutionTest"), t("H06_PASS", "R27EvolutionTest")],
                 report=snippet("r27_generation.detail_json", "holdout.blockExposures"), gaps=[]),
 "R27-017": dict(files=[S2 + "CampaignStateSU1.java", S2 + "HistoricalReplayEngineSU1.java"], tables=[], tests=[t("R27-017_PASS", "R27ReplayTest")],
                 report=snippet("r26_task", "foundry-su1 blocked_reason 不再是 SU1_HISTORY_NOT_CAUSAL_CONSECUTIVE"), gaps=[]),
 "R27-018": dict(files=[S2 + "HistoryImportR27.java", S2 + "StoreR27.java", M + "ChartsActivityR83.java"], tables=["r27_history_import"], tests=[],
                 report=snippet("RuntimeReportR26", "HISTORY_IMPORT 冲突期列表"), gaps=["CSV 导入走 Android 文件选择器，无 HOST 测试"]),
 "R27-019": dict(files=[S2 + "LegacyPolicyExportR27.java", S2 + "RuntimeR27.java"], tables=["r27_legacy_export"], tests=[],
                 report=snippet("RuntimeReportR26", "LEGACY_EXPORT 复现率与原因"), gaps=["R908 基因组全部是有状态程序，记为 LEGACY_NOT_REPRODUCIBLE；无 HOST 测试"]),
 "R27-020": dict(files=[S2 + "LegacyPolicyExportR27.java"], tables=["r27_legacy_export"], tests=[], report=snippet("RuntimeReportR26", "LEGACY_EXPORT"),
                 gaps=["旧学习器/R943 网络权重形状与 AtomNetR27 不同，未做热启动（如实记录原因）；验收“导入后立即有回放成绩”未达成"]),
 "R27-021": dict(files=[S2 + "PolicySpecR27.java", S2 + "PolicyAstR27.java", S2 + "JsonR27.java"], tables=["r27_policy"], tests=[t("R27-021_PASS", "R27BirthGateTest")],
                 report=snippet("r27_policy", "spec_json"), gaps=[]),
 "R27-022": dict(files=[S2 + "BirthGateR27.java"], tables=["r27_policy.state_reason"], tests=[t("H04_PASS", "R27BirthGateTest"), t("H05_PASS", "R27BirthGateTest")],
                 report=snippet("ReportR27.evolution", "rejectReasons"), gaps=[]),
 "R27-023": dict(files=[S2 + "IdeaFamilyR27.java"] + [S2 + "IdeaI%02d%s.java" % (i, n) for i, n in enumerate(["RandomBaselineR27", "FrequencyReversionR27", "GapCycleR27", "TransitionChainR27", "CrossPositionR27", "PermutationConsistencyR27", "RegimeSwitchR27", "FragmentRetrievalR27", "NeuralRepresentationR27", "SymbolicFormulaR27", "FailureRepairR27", "ComplementaryEnsembleR27", "LegacyAssetR27", "SelectorOnlyR27"])],
                 tables=["r27_policy.idea_family"], tests=[t("R27-023_PASS", "R27BirthGateTest")], report=snippet("ReportR27.evolution", "familySurvivors"), gaps=["文件平铺在 strategyv2/（方案写 idea/ 子目录），功能等价"]),
 "R27-024": dict(files=[S2 + "ExpertProposalsR27.java"], tables=["r27_expert_proposal"], tests=[t("R27-024_PASS", "R27ComponentTest")], report=snippet("r27_expert_proposal", "专家提案存活率"),
                 gaps=["长期存活率只在运行中的 App 上产生（HOST 3代：提案完整、可执行 10/10）"]),
 "R27-025": dict(files=[S2 + "MutationOpsR27.java", S2 + "CrossoverOpsR27.java", S2 + "FusionOpsR27.java"], tables=["r27_lineage"], tests=[t("H05_PASS", "R27BirthGateTest"), t("H12_PASS", "R27EvolutionTest")],
                 report=snippet("ChartCacheR27", "p05_lineage"), gaps=[]),
 "R27-026": dict(files=[S2 + "SuccessiveHalvingR27.java", S2 + "EliteArchiveR27.java"], tables=["r27_policy.state_reason"], tests=[t("H07_PASS", "R27EvolutionTest"), t("H12_PASS", "R27EvolutionTest")],
                 report=snippet("r27_policy", "state_reason R0..R3"), gaps=[]),
 "R27-027": dict(files=[S2 + "IslandModelR27.java", S2 + "EliteArchiveR27.java"], tables=["r27_generation.detail_json"], tests=[t("H12_PASS", "R27EvolutionTest")],
                 report=snippet("r27_generation.detail_json", "islandChampionSimilarity, archiveCells"), gaps=[]),
 "R27-028": dict(files=[S2 + "EvolutionLoopR27.java", S2 + "EvalContextR27.java", S2 + "StoreR27.java"], tables=["r27_evolution_cursor", "r27_generation", "r27_lineage"],
                 tests=[t("H12_PASS", "R27EvolutionTest")], report=snippet("ReportR27.evolution", "championLines"), gaps=[]),
 "R27-029": dict(files=[S2 + "EvolutionLoopR27.java"], tables=["r27_generation.verified"], tests=[t("H06_PASS", "R27EvolutionTest"), t("H07_PASS", "R27EvolutionTest")],
                 report=snippet("ReportR27.evolution", "verifiedGenerations, championLines[].verified"), gaps=[]),
 "R27-030": dict(files=[S2 + "NullEvolutionR27.java", S2 + "IdeaI00RandomBaselineR27.java"], tables=["r27_generation.null_champion_holdout_hr", "r27_generation.i00_champion_holdout_hr"],
                 tests=[t("H07_PASS", "R27EvolutionTest")], report=snippet("ReportR27.evolution", "championLines[].shuffledLabelControl / i00RandomBaseline"), gaps=[]),
 "R27-031": dict(files=[S2 + "StoreR27.java"], tables=["r26_evolution"], tests=[t("M6_R26_EVOLUTION_SEVEN_COUNTERS", log=MIG, cmd="tools/r27/run_host_tests.sh")],
                 report=snippet("RuntimeReportR26 EVOLUTION", "attempts…verifiedImprovements"), gaps=["旧 Foundry/Evolution 调度本身未停止（与 R27-046 退役开关一起）"]),
 "R27-032": dict(files=[S2 + "StoreR27.java", S2 + "LiveShadowsR27.java", S2 + "RuntimeR27.java"], tables=["r27_live_shadow", "r27_eprocess"], tests=[t("H15_SQL_CERTIFIED_ROW", log=MIG, cmd="tools/r27/run_host_tests.sh")],
                 report=snippet("ReportR27.truth", "futureShadows"), gaps=["影子池非空/轮换只能在运行中的 App 上观察"]),
 "R27-033": dict(files=[S2 + "AtomNetR27.java", S2 + "AtomNetTrainerR27.java", S2 + "AtomWorldR27.java"], tables=["r27_atomnet_state", "r27_atomnet_epoch", "r27_atomnet_pending"],
                 tests=[t("H16_PASS", "R27QualityTest")], report=snippet("ChartCacheR27", "p09 AtomNet 图A/B/C"),
                 gaps=["真机：60分钟续训步数≥55（DEVICE）", "AtomicToolWorldR943 未改为调用 AtomNetR27（旧网络并行保留，R27 训练在 RuntimeR27 atomnet 任务中）"]),
 "R27-034": dict(files=[M + "AtomReplayR27.java", S2 + "AtomWorldR27.java"], tables=["r27_brier_pair"], tests=[t("H01_ATOMS_PASS", "R27LeakageTest")],
                 report=snippet("ChartCacheR27", "AtomNet 样本数"), gaps=["训练不中断须真机观察"]),
 "R27-035": dict(files=[S2 + "BrierSkillR27.java", S2 + "ChartCacheR27.java"], tables=["r27_brier_pair"], tests=[t("H16_PASS", "R27QualityTest")],
                 report=snippet("BrierSkillR27.summary", "verdict=INSUFFICIENT when N<200"), gaps=[]),
 "R27-036": dict(files=[S2 + "FtrlR27.java"], tables=["r27_brier_pair.model='FTRL'"], tests=[t("R27-036_PASS", "R27ComponentTest")], report=snippet("ChartCacheR27", "FTRL 图A"), gaps=[]),
 "R27-037": dict(files=[S2 + "CalibrationRowsR27.java", S2 + "LiveShadowsR27.java"], tables=["r27_calibration"], tests=[t("H13_PASS", "R27QualityTest")],
                 report=snippet("r27_calibration", "excluded_late, excluded_gap, reason"), gaps=[]),
 "R27-038": dict(files=[S2 + "CalibrationRowsR27.java"], tables=["r27_calibration.fit_source"], tests=[t("H13_PASS", "R27QualityTest")],
                 report=snippet("r27_calibration", "fit_source=REPLAY+FUTURE validation"), gaps=["24小时内≥1个校准模型须在运行中的 App 上观察"]),
 "R27-039": dict(files=[S2 + "EvolutionLoopR27.java", S2 + "StoreR27.java"], tables=["r27_ablation"], tests=[], report=snippet("r27_ablation", "消融结果"),
                 gaps=["只实现了 R27 冠军消融；R26 反馈/审计任务未改，三个游标>0 未验证"]),
 "R27-040": dict(files=[S2 + "SequentialQualificationR27.java", S2 + "LiveShadowsR27.java"], tables=["r27_eprocess"], tests=[t("H08_PASS", "R27SequentialTest"), t("H09_PASS", "R27SequentialTest"), t("ALPHA", "R27SequentialTest")],
                 report=snippet("ReportR27.truth", "futureShadows[].logE/threshold/sequentialState"), gaps=[]),
 "R27-041": dict(files=[S2 + "CertificateGateR27.java", S2 + "LiveShadowsR27.java"], tables=["r27_certificate"], tests=[t("H15_JAVA_PASS", "R27QualityTest")],
                 report=snippet("r27_certificate", "gates_json"), gaps=[]),
 "R27-042": dict(files=[S2 + "MainlineBridgeSU2.java", M + "UnifiedResearchChoiceR932.java", S2 + "LiveShadowsR27.java"], tables=["r27_chart_cache(mainline)"],
                 tests=[t("H15_JAVA_PASS", "R27QualityTest"), t("H15_SQL_SECOND_DECISION_SAME_PERIOD", log=MIG, cmd="tools/r27/run_host_tests.sh")],
                 report=snippet("OWNER", "主链红线测试结果"), gaps=["R27 意图仍需经过现有 R929 授权链；未新增任何 REAL 授权路径（主链规则未改一个字）"]),
 "R27-043": dict(files=[S2 + "AtomScoreR27.java", M + "AtomReplayR27.java"], tables=["r27_atom_score"], tests=[t("H01_ATOMS_PASS", "R27LeakageTest"), t("R27-043_PASS", "R27ComponentTest")], report=snippet("ATOMS", "r27_atom_score"),
                 gaps=["165个原子样本≥1000 须运行中观察", "口径观察（未改口径）：按 docs/07 “上界<0 即 HARMFUL”，纯噪声原子因5分箱估计方差常被判 HARMFUL（HOST 实测），只影响生成器权重×0.25，不影响资格；是否改为与打乱对照配对比较需用户决定"]),
 "R27-044": dict(files=[S2 + "DerivedAtomR27.java"], tables=["r27_molecule"], tests=[t("R27-044_PASS", "R27ComponentTest")], report=snippet("r27_molecule", "派生原子"), gaps=["24小时后有记录须在运行中的 App 上观察"]),
 "R27-045": dict(files=[S2 + "MatrixConsistencyR27.java", "app/src/main/java/com/ethk3/novaonepro/r952/MatrixSourceRouterCP12.java"], tables=["r27_lineage"], tests=[t("H14_PASS", "R27QualityTest")],
                 report=snippet("RANKING、ACTION_SURFACE", "maxError"), gaps=[]),
 "R27-046": dict(files=[M + "ResearchLearningContinuityR912.java", S2 + "RuntimeR27.java", M + "ChartsActivityR83.java"], tables=[], tests=[], report=snippet("MERGE", "旧链退役开关"),
                 gaps=["退役开关已加，观察期对照表未运行"]),
 "R27-047": dict(files=[S2 + "RuntimeR27.java"], tables=[], tests=[], report=snippet("RuntimeReportR26", "dbSize"), gaps=["数据库归档压缩未实施：门控在旧链退役之后（只记录 DB 大小）"]),
 "R27-048": dict(files=[M + "FetchLedgerR925.java", S2 + "RuntimeR27.java"], tables=["r27_fetch_reject"], tests=[], report=snippet("FETCH", "WRITE_REJECTED 原因分布"), gaps=["无 HOST 测试"]),
 "R27-049": dict(files=[S2 + "ChartCacheR27.java", S2 + "RuntimeR27.java"], tables=["r27_chart_cache"], tests=[], report=snippet("UI", "图表加载耗时"), gaps=["首屏P95≤300ms 须真机测量"]),
 "R27-050": dict(files=[M + "ChartsRepositoryR27.java", M + "ChartsActivityR83.java", M + "ResearchVisualDataR82.java", "app/src/main/java/com/ethk3/novaonepro/r952/ResearchChartViewR82.java", S2 + "ChartCacheR27.java"],
                 tables=["r27_chart_cache"], tests=[], report=snippet("ChartCacheR27.PANELS", "14 panels"), gaps=["图表渲染无 HOST 测试；24小时 READY 须真机观察"]),
 "R27-051": dict(files=[S2 + "ReportR27.java", S2 + "ReportDataR27.java", S2 + "RuntimeReportR26.java", M + "ReportExportServiceCP4.java"], tables=[],
                 tests=[t("H17_PASS", "R27QualityTest")], report=snippet("ReportR27.truth", "R27_01_WIN_RATE_TRUTH.conclusionTemplate"), gaps=[]),
 "R27-052": dict(files=[S2 + "ReportR27.java", S2 + "ReportDataR27.java"], tables=[], tests=[t("H17_PASS", "R27QualityTest")],
                 report=snippet("ReportR27.evolution/runtime/upgrade", "R27_02/03/04"), gaps=[]),
 "R27-053": dict(files=[S2 + "RuntimeReportR26.java", S2 + "ReportDataR27.java"], tables=[], tests=[], report=snippet("RuntimeReportR26", "19 chapters"),
                 gaps=["19章数据源已接；导出的真实报告需在设备上生成后核对（仅 DEVICE 可 NOT_RUN）"]),
 "R27-054": dict(files=["tools/r27/build_identity.py", "app/build.gradle", S2 + "RuntimeR27.java"], tables=[], tests=[t("BUILD_IDENTITY", cmd="python3 tools/r27/build_identity.py <out>", log="evidence/r27/host/build_identity.log", marker="R27_BUILD_IDENTITY_WRITTEN")],
                 report=snippet("assets r27/build-identity.json", "IDENTITY.commit/tree/versionCode"), gaps=["报告 IDENTITY 章与运行时一致须在安装后的 App 上核对"]),
 "R27-055": dict(files=[T + "R27ComponentTest.java", T + "R27LeakageTest.java", T + "R27ReplayTest.java", T + "R27BirthGateTest.java", T + "R27EvolutionTest.java", T + "R27SequentialTest.java", T + "R27QualityTest.java", T + "R27NullBaselineTest.java", T + "R27TestSupport.java", "tools/r27/test_r27_migration.py", "tools/r27/run_host_tests.sh"],
                 tables=[], tests=[t("OK (31 tests)", log=HOSTLOG, marker="OK (31 tests)"), t("RUN_HOST_TESTS rc=0", log="evidence/r27/host/run_host_tests.log", cmd="tools/r27/run_host_tests.sh", marker="RUN_HOST_TESTS rc=0")],
                 report=snippet("evidence/r27/host", "H01–H18 原始日志"), gaps=[]),
 "R27-056": dict(files=[T + "R27NullBaselineTest.java", "tools/r27/compare_null_baseline.py"], tables=[], tests=[t("R27_056_JAVA_ISOMORPH_PASS", "R27NullBaselineTest"), t("H18|RESULT|PASS_WITHIN_1PP", log=H18, cmd="tools/r27/run_host_tests.sh", marker="PASS_WITHIN_1PP")],
                 report=snippet("ReportR27.truth", "luckCeiling"), gaps=[]),
 "R27-057": dict(files=["tools/r27/emulator_r27.sh", "tools/r27/emulator_receipt.py", ".github/workflows/r27.yml"], tables=[], tests=[], report=snippet("DEVICE", "第12章设备表"),
                 gaps=["真机设备矩阵（V2353A API34 + 另一品牌，亮屏/息屏/锁屏/插拔电/覆盖安装/杀进程/断网）未执行：本环境无真机。CI 只能提供模拟器回执（EMULATOR，不计为设备验证）"]),
 "R27-058": dict(files=["release/R27_ROLLBACK_PLAN.md", "release/release_manifest.json", ".github/workflows/r27.yml"], tables=[], tests=[], report=snippet("package-receipt", "APK SHA256 / 签名指纹 / versionCode"),
                 gaps=["正式签名需要 CI 缓存中的 R13 私钥；覆盖安装与回滚到 1713d11 须真机执行（模拟器回执见 CI）"]),
}

def log_has(path, marker):
    p = ROOT / path
    return p.is_file() and marker in p.read_text(encoding="utf-8", errors="replace")

def main():
    ci = sys.argv[sys.argv.index("--ci-run") + 1] if "--ci-run" in sys.argv else None
    changed = sorted(set(git("diff", "--name-only", BASE).split()) | set(git("ls-files", "--others", "--exclude-standard").split()))
    changed = [p for p in changed if not p.startswith("evidence/r27/") and not p.startswith("app/build/") and not p.startswith(str(PLAN.relative_to(ROOT)))]
    files = [fileinfo(p) for p in changed]
    EV.mkdir(parents=True, exist_ok=True)
    (EV / "baseline.json").write_text(json.dumps({"schema": "R27_BASELINE_HASHES_1", "baselineCommit": BASE, "files": files}, ensure_ascii=False, indent=1) + "\n")
    tracked = git("ls-files", "app/src").split()
    audit = {"schema": "R27_SOURCE_AUDIT_1", "baselineCommit": BASE, "sourceFiles": len(set(tracked) | {f["path"] for f in files if f["path"].startswith("app/src")}),
             "changed": sum(f["change"] == "MODIFIED" for f in files), "new": sum(f["change"] == "NEW" for f in files), "files": files}
    (EV / "source_audit.json").write_text(json.dumps(audit, ensure_ascii=False, indent=1) + "\n")
    reg = json.loads((PLAN / "registries/r27_cards.json").read_text())
    rows = []
    for card in reg["cards"]:
        cid = card["id"]; m = C[cid]
        tests = []
        for x in m["tests"]:
            ok = log_has(x["log"], x["marker"])
            tests.append(dict(x, logPresent=(ROOT / x["log"]).is_file(), passedInLog=ok))
        ddl = []
        for tb in m["tables"]:
            name = tb.split(".")[0].split("(")[0]
            src = (ROOT / (S2 + "SchemaR27.java")).read_text() + (ROOT / (S2 + "RuntimeSchemaR26.java")).read_text() + (ROOT / (S2 + "HistoryImportR27.java")).read_text()
            i = src.find("CREATE TABLE IF NOT EXISTS " + name + "(")
            ddl.append({"table": tb, "ddl": src[i:src.find('"', i)] if i >= 0 else None, "migration": "evidence/r27/host/migration.log (M1–M6)"})
        deliver = {"diffAndHashes": bool(m["files"]) and all(after(f) for f in m["files"]), "ddlAndMigration": all(d["ddl"] for d in ddl) and log_has(MIG, "RESULT|ALL_OK") if ddl else True,
                   "testCommandRcLog": bool(tests) and all(x["passedInLog"] for x in tests), "reportField": bool(m["report"]), "gapsRecorded": True}
        status = DONE if all(deliver.values()) and not any(g.startswith("无") or "未实施" in g or "未达成" in g or "未执行" in g for g in m["gaps"]) else TODO
        out = {"card": cid, "title": card["title"], "batch": card["batch"], "acceptance": card["acceptance"], "status": status,
               "deliverables": deliver, "files": [fileinfo(f) for f in m["files"]], "ddl": ddl, "tests": tests, "reportField": m["report"],
               "openItems": m["gaps"], "ciRun": ci, "statusRule": "HOST/CI → 已修改待设备验证 at most; 已验证 only with real-device evidence"}
        d = EV / cid; d.mkdir(exist_ok=True)
        (d / "card.json").write_text(json.dumps(out, ensure_ascii=False, indent=1) + "\n")
        card["status"] = status
        card["evidence"] = "evidence/r27/%s/card.json" % cid
        if status == TODO and (m["files"] or m["tests"]):
            card["construction_note"] = "代码已提交；缺：" + "；".join(m["gaps"] or ["测试/报告字段"])
        elif "construction_note" in card:
            del card["construction_note"]
        rows.append((cid, card["batch"], card["title"], status, len(tests), "；".join(m["gaps"])))
    (PLAN / "registries/r27_cards.json").write_text(json.dumps(reg, ensure_ascii=False, indent=1) + "\n")
    merge = [
        (1, "研究线程池＋只读连接＋写队列", ["R27-003", "R27-004", "R27-005"]), (2, "语义ABI＋黄金回放", ["R27-010", "R27-011", "R27-012"]),
        (3, "历史回放为主训练/筛选通道", ["R27-013", "R27-015", "R27-016"]), (4, "双向 e-过程序贯检验", ["R27-040"]), (5, "分片段回放 SKIPPED_GAP", ["R27-017"]),
        (6, "校准逐行剔除", ["R27-037", "R27-038"]), (7, "七计数＋代际三线＋谱系上图", ["R27-028", "R27-029", "R27-031", "R27-050"]),
        (8, "I00＋打乱标签进化负对照", ["R27-030"]), (9, "历史预训练＋经验回放续训", ["R27-033", "R27-034"]), (10, "R27-01—04 章＋19章接数据源", ["R27-051", "R27-052", "R27-053"]),
        (11, "旧链作为种子导入，观察期后退役", ["R27-019", "R27-020", "R27-046"]), (12, "主链规则不变", ["R27-041", "R27-042"])]
    st = {r[0]: r[3] for r in rows}
    ms = {"schema": "R27_MERGE_STATUS_1", "rulings": [{"ruling": n, "r27": txt, "cards": cs, "cardStatus": {c: st[c] for c in cs},
          "status": "IMPLEMENTED_PENDING_DEVICE" if all(st[c] == DONE for c in cs) else "PARTIAL"} for n, txt, cs in merge]}
    (EV / "merge_status.json").write_text(json.dumps(ms, ensure_ascii=False, indent=1) + "\n")
    with open(EV / "cards_table.md", "w", encoding="utf-8") as f:
        f.write("| 卡 | 批次 | 标题 | 状态 | 测试数 | 未完成项 |\n|---|---|---|---|---|---|\n")
        for r in rows: f.write("| %s | %s | %s | %s | %d | %s |\n" % (r[0], r[1], r[2], r[3], r[4], r[5].replace("|", "/") or "—"))
    print("R27_EVIDENCE_WRITTEN cards=%d done=%d todo=%d files=%d" % (len(rows), sum(r[3] == DONE for r in rows), sum(r[3] == TODO for r in rows), len(files)))

if __name__ == "__main__":
    main()
