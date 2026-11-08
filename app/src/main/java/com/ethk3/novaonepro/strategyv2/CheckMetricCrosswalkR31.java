package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B11-C04: explicit crosswalk between the 96 real check results (REPORT_DETAILS check_result rows, recorded in
 * construction/STARCORE_R31_FINAL_20261104/evidence/derived/checks_96.json) and the 44 product metrics
 * (ledgers/metric_coverage_44.json).
 *
 * <p>Execution state and domain outcome are kept apart: EXECUTED only proves a check ran. Each check declares the
 * scope of what it can support (STRUCTURE, RUNTIME, DEVICE, QUALITY or a pure composition INDEX) and the metrics it
 * supports. A metric never passes by mapping: a structure-only PASS supports only a structural subcondition, a
 * quality (Q*) or device metric needs its own registered natural/device evidence, and a metric without a supporting
 * check is listed as uncovered with what it still needs. Report coverage (assessed with a lawful result / applicable)
 * and pass rate are separate numbers; UNKNOWN stays UNKNOWN.</p>
 */
public final class CheckMetricCrosswalkR31 {
    private CheckMetricCrosswalkR31() {}
    public static final String VERSION = "R31_CHECK_METRIC_CROSSWALK_V1";
    public enum Scope { STRUCTURE, RUNTIME, DEVICE, QUALITY, INDEX }
    public static final class Check {
        public final String id, executionState, domainOutcome; public final Scope scope; public final List<String> metrics;
        Check(String id, String exec, String outcome, String scope, String metrics) { this.id = id; executionState = exec; domainOutcome = outcome; this.scope = Scope.valueOf(scope); this.metrics = metrics.isEmpty() ? Collections.<String>emptyList() : Collections.unmodifiableList(Arrays.asList(metrics.split(" "))); }
        public boolean domainPass() { return "PASS".equals(domainOutcome); }
    }
    public static final class Metric {
        public final String id, name, kind, recordedStatus, requiredEvidence, batch, source, target;
        Metric(String id, String name, String kind, String status, String required, String batch, String source, String target) { this.id = id; this.name = name; this.kind = kind; recordedStatus = status; requiredEvidence = required; this.batch = batch; this.source = source; this.target = target; }
        public boolean quality() { return "QUALITY".equals(kind); }
        public boolean deviceRequired() { return "TARGET_DEVICE_SCENARIO_RECEIPT".equals(requiredEvidence); }
    }
    private static Check c(String id, String exec, String outcome, String scope, String metrics) { return new Check(id, exec, outcome, scope, metrics); }
    private static Metric m(String id, String name, String kind, String status, String required, String batch, String source, String target) { return new Metric(id, name, kind, status, required, batch, source, target); }
    private static final List<Check> CHECKS = Collections.unmodifiableList(Arrays.asList(
        c("RSEC-001","EXECUTED","OBSERVED","RUNTIME","M10 M11 M24 M26 M27"),
        c("RSEC-002","EXECUTED","N/A","STRUCTURE","M31"),
        c("RSEC-003","EXECUTED","READ_OK","RUNTIME","M21 Q07"),
        c("RSEC-004","EXECUTED","OBSERVED","RUNTIME","M20"),
        c("RSEC-005","EXECUTED","OBSERVED","RUNTIME","M09"),
        c("RSEC-006","EXECUTED","OBSERVED","RUNTIME","M15"),
        c("RSEC-007","EXECUTED","OBSERVED","RUNTIME","M24 Q06"),
        c("RSEC-009","EXECUTED","DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY","STRUCTURE","M01"),
        c("RSEC-010","EXECUTED","OBSERVED","STRUCTURE","M17"),
        c("RSEC-011","EXECUTED","OBSERVED","STRUCTURE","M17"),
        c("RSEC-012","EXECUTED","OBSERVED","RUNTIME","M27 M28"),
        c("RSEC-013","EXECUTED","OBSERVED","QUALITY","Q07"),
        c("RSEC-014","EXECUTED","OBSERVED","QUALITY","Q07 M23"),
        c("RSEC-015","EXECUTED","OBSERVED","RUNTIME","M23"),
        c("RSEC-016","EXECUTED","OBSERVED","STRUCTURE","M17"),
        c("RSEC-017","EXECUTED","OBSERVED","RUNTIME","M23"),
        c("RSEC-018","EXECUTED","OBSERVED","STRUCTURE","M17"),
        c("RSEC-019","EXECUTED","ENGINEERING_IMPLEMENTED_REAL_DATA_AND_FUTURE_EVIDENCE_PENDING","QUALITY","Q05 M26"),
        c("RSEC-020","EXECUTED","ENGINEERING_IMPLEMENTED_REAL_DATA_AND_FUTURE_EVIDENCE_PENDING","QUALITY","Q01 Q02"),
        c("RSEC-021","EXECUTED","ENGINEERING_IMPLEMENTED_REAL_DATA_AND_FUTURE_EVIDENCE_PENDING","RUNTIME","M28 M03"),
        c("RSEC-022","EXECUTED","ENGINEERING_IMPLEMENTED_REAL_DATA_AND_FUTURE_EVIDENCE_PENDING","STRUCTURE","M17"),
        c("RSEC-023","EXECUTED","OBSERVED","RUNTIME","M27 M28"),
        c("RSEC-024","EXECUTED","OBSERVED","RUNTIME","M21 M22"),
        c("RSEC-025","EXECUTED","OBSERVED","STRUCTURE","M02"),
        c("RSEC-026","EXECUTED","OBSERVED","RUNTIME","M33"),
        c("RSEC-027","EXECUTED","N/A","STRUCTURE","M01"),
        c("RSEC-028","EXECUTED","N/A","STRUCTURE","M02"),
        c("RSEC-029","EXECUTED","N/A","STRUCTURE","M04 M06"),
        c("RSEC-030","EXECUTED","DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY","RUNTIME","M05 M03"),
        c("RSEC-031","EXECUTED","OBSERVED","RUNTIME","M03 M04"),
        c("RSEC-032","EXECUTED","OBSERVED","RUNTIME","M12 M32"),
        c("RSEC-033","EXECUTED","OK","RUNTIME","M07"),
        c("RSEC-034","EXECUTED","DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY","QUALITY","Q01 Q03"),
        c("RSEC-035","EXECUTED","DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY","RUNTIME","M28"),
        c("RSEC-036","EXECUTED","N/A","STRUCTURE","M12 M13"),
        c("RSEC-037","EXECUTED","DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY","STRUCTURE","M30"),
        c("RSEC-038","EXECUTED","OBSERVED","STRUCTURE","M06"),
        c("RSEC-039","EXECUTED","DEPLOYMENT_PROOF_INSUFFICIENT_RESEARCH_TRAINING_REPORTED_SEPARATELY","RUNTIME","M28 Q01"),
        c("RSEC-040","EXECUTED","OBSERVED","QUALITY","Q02"),
        c("RSEC-041","EXECUTED","OBSERVED","STRUCTURE","M13"),
        c("RSEC-042","EXECUTED","N/A","DEVICE","M13 M14"),
        c("RSEC-043","EXECUTED","OBSERVED","STRUCTURE","M02"),
        c("RSEC-044","EXECUTED","N/A","INDEX",""),
        c("RSEC-048","EXECUTED","PASS","RUNTIME","M03"),
        c("RSEC-052","EXECUTED","FAIL","RUNTIME","M12"),
        c("RSEC-053","EXECUTED","RETEST_REQUIRED","RUNTIME","M06 M28"),
        c("RSEC-054","EXECUTED","NOT_PROVEN","RUNTIME","M24"),
        c("RSEC-055","EXECUTED","RETEST_REQUIRED","RUNTIME","M24"),
        c("RSEC-056","EXECUTED","RETEST_REQUIRED","RUNTIME","M24"),
        c("RSEC-057","EXECUTED","RETEST_REQUIRED","STRUCTURE","M18 M19"),
        c("RSEC-058","EXECUTED","NOT_PROVEN","RUNTIME","M24"),
        c("RSEC-059","EXECUTED","WAIT_CALIBRATION_INPUT","RUNTIME","M28"),
        c("RSEC-060","EXECUTED","NOT_PROVEN","RUNTIME","M28"),
        c("RSEC-061","EXECUTED","NOT_PROVEN","RUNTIME","M31"),
        c("RSEC-062","EXECUTED","ENGINEERING_DIAGNOSIS_ACTIVE_REAL_REMAINS_GATED","RUNTIME","M31"),
        c("RSEC-063","EXECUTED","ENGINEERING_DIAGNOSIS_ACTIVE_REAL_REMAINS_GATED","RUNTIME","M31"),
        c("RSEC-064","EXECUTED","ENGINEERING_DIAGNOSIS_ACTIVE_REAL_REMAINS_GATED","RUNTIME","M31"),
        c("RSEC-065","EXECUTED","ENGINEERING_DIAGNOSIS_ACTIVE_REAL_REMAINS_GATED","RUNTIME","M31"),
        c("RSEC-066","EXECUTED","BLOCKED","QUALITY","Q01"),
        c("RSEC-067","EXECUTED","OBSERVED","RUNTIME","M36"),
        c("RSEC-068","EXECUTED","NO_MEASURED_ABLATION","RUNTIME","M36"),
        c("RSEC-069","EXECUTED","WATCH","STRUCTURE","M21"),
        c("RSEC-070","EXECUTED","WARN","STRUCTURE","M02"),
        c("RSEC-071","EXECUTED","WATCH","STRUCTURE","M02"),
        c("RSEC-072","EXECUTED","OBSERVED","RUNTIME","M35 M28"),
        c("RSEC-073","EXECUTED","REAL_LOCKED_SHADOW_ONLY","RUNTIME","M35 M28"),
        c("RSEC-074","EXECUTED","FAIL","RUNTIME","M31 M12"),
        c("RSEC-075","EXECUTED","PASS","STRUCTURE","M31"),
        c("RSEC-076","EXECUTED","PASS","STRUCTURE","M01"),
        c("RSEC-077","EXECUTED","PASS","STRUCTURE","M31"),
        c("RSEC-078","EXECUTED","RESEARCH_FOUNDATION_PENDING","RUNTIME","M09 M01"),
        c("RSEC-079","EXECUTED","OBSERVED","RUNTIME","M21 Q06"),
        c("RSEC-301","EXECUTED","N/A","STRUCTURE","M31"),
        c("RSEC-302","EXECUTED","N/A","STRUCTURE","M17"),
        c("RSEC-303","EXECUTED","N/A","STRUCTURE","M17"),
        c("RSEC-304","EXECUTED","N/A","INDEX",""),
        c("RSEC-305","EXECUTED","OBSERVED","RUNTIME","M32 M04"),
        c("RSEC-306","EXECUTED","OBSERVED","QUALITY","Q01 M04"),
        c("RSEC-307","EXECUTED","OBSERVED","STRUCTURE","M17"),
        c("RSEC-308","EXECUTED","OBSERVED","QUALITY","M28 Q07"),
        c("RSEC-309","EXECUTED","OBSERVED","STRUCTURE","M17"),
        c("RSEC-310","EXECUTED","OBSERVED","QUALITY","Q01 M28"),
        c("RSEC-311","EXECUTED","N/A","INDEX",""),
        c("RSEC-312","EXECUTED","AVAILABLE","RUNTIME","M03 M04"),
        c("RSEC-313","EXECUTED","AVAILABLE","RUNTIME","M03 M04"),
        c("RSEC-314","EXECUTED","AVAILABLE","RUNTIME","M28"),
        c("RSEC-315","EXECUTED","AVAILABLE","RUNTIME","M04"),
        c("RSEC-316","EXECUTED","OBSERVED","STRUCTURE","M30"),
        c("RSEC-317","EXECUTED","OBSERVED","RUNTIME","M23 M21"),
        c("RSEC-318","EXECUTED","N/A","STRUCTURE","M17"),
        c("RSEC-319","EXECUTED","N/A","STRUCTURE","M17"),
        c("RSEC-320","EXECUTED","N/A","STRUCTURE","M17"),
        c("RSEC-321","EXECUTED","OBSERVED","RUNTIME","M21 M23"),
        c("RSEC-322","EXECUTED","OBSERVED","RUNTIME","M21 M23"),
        c("RSEC-323","EXECUTED","OBSERVED","RUNTIME","M21 M23"),
        c("RSEC-324","EXECUTED","N/A","STRUCTURE","M17")
    ));
    private static final List<Metric> METRICS = Collections.unmodifiableList(Arrays.asList(
        m("M01","源码与安装身份","ENGINEERING","PARTIAL","SIGNED_HASHED_MEASUREMENT_RECEIPT","B00/B12","构建身份：release_manifest.json + APK 签名指纹 + 源码提交（tools/r29/package_receipt.py）","全部一致"),
        m("M02","功能保全覆盖","ENGINEERING","PARTIAL","SIGNED_HASHED_MEASUREMENT_RECEIPT","B00/B12","保全清单 1832/299（legacy_count.py）+ 生命周期回归 androidTest","100%有记录；无凭空VERIFIED"),
        m("M03","准时预测持久化","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B03","r30_issue_timeline：request_to_frozen 与 settled_to_frozen 段 P95（IssueTimelineR30.summarize）","P95≤5000ms"),
        m("M04","抓到后正式结算","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B03","r30_issue_timeline：observe_to_settled 段 P95","P95≤1000ms"),
        m("M05","赛前有效覆盖","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B03","r30_issue_timeline：NEXT_DEADLINE−NEXT_FROZEN ≥5000ms 的期数占比","目标100%；受支持健康场景≥99.5%"),
        m("M06","唯一正式结算","ENGINEERING","RECORDED_UNVALIDATED","SIGNED_HASHED_MEASUREMENT_RECEIPT","B03/B10","结算表唯一键冲突计数 + 同 issue REAL 意向计数（SQL 见 docs/90）","均为0"),
        m("M07","实时写事务","ENGINEERING","RECORDED_UNVALIDATED","SIGNED_HASHED_MEASUREMENT_RECEIPT","B02","r30_writer_hold critical=1 的 wait_p95 最大值；24h 失败数=sum(failures)","P95≤200ms；24h失败0"),
        m("M08","分库隔离","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B02","单库模式：SCHEDULER_OWNERS.json 中同表双写者数；B11 启用后：日志 R30_RESEARCH_TABLE_FROZEN 次数","0"),
        m("M09","迁移一致性","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B05/B12","r30_copy_log COUNT_MISMATCH 行数 + B11 ContentHash 不一致数","全部一致；丢失/重复0"),
        m("M10","每期训练投递","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B06","每期训练投递：r952_research_outbox 状态分布 + QualificationInboxR29 连续游标","100%；重复消费0"),
        m("M11","训练完成新鲜度","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B01/B06","TaskLivenessR30：最老积压期数（pending 最大任务）","正常负载最老积压≤5期；超限必须FAIL"),
        m("M12","任务活性","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B01","TaskLivenessR30.summarize.m12（null=有UNKNOWN）","0；等依赖/等数据单列"),
        m("M13","前后台连续性","ENGINEERING","INSUFFICIENT","TARGET_DEVICE_SCENARIO_RECEIPT","B04/B12","B10 场景：前后台切换前后已投递任务数差","受支持场景不丢已投递任务；缺口全记录"),
        m("M14","进程稳定性","ENGINEERING","INSUFFICIENT","TARGET_DEVICE_SCENARIO_RECEIPT","B04/B12","进程崩溃/ANR 计数（logcat + ApplicationExitInfo）","0"),
        m("M15","热库与总磁盘","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B05","draws 热库行数 + 应用数据目录总字节","draws≤5000；未超配置总预算且可恢复"),
        m("M16","报告取消","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B05/B11","ReportTaskControlR69 取消到释放耗时","≤2000ms"),
        m("M17","报告完整性","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B11","报告索引覆盖率 + ZIP 大小/分卷","全量索引100%；默认ZIP≤20MiB或明确分卷"),
        m("M18","PIT泄漏反证","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B06/B12","r30_pit_violation 非注入行数","0"),
        m("M19","训练分割污染","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B06/B07","HoldoutLedgerR27 R30_CONFIRM_REUSED 计数","0"),
        m("M20","全状态续训恢复","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B06","ModelArtifactR29 中断/恢复一致性测试结果","按已登记浮点容差一致；不重复学习"),
        m("M21","策略完整合同","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B07","BirthGateR27 R30_CONTRACT: 拒绝码分布；入池策略合同违规=0","100%；空壳拒绝"),
        m("M22","结构与行为去重","ENGINEERING","RECORDED_UNVALIDATED","SIGNED_HASHED_MEASUREMENT_RECEIPT","B07","r30_behavior_dup 行数与报告去重计数一致","计数与ledger一致"),
        m("M23","族专家覆盖","ENGINEERING","BLOCKED","SIGNED_HASHED_MEASUREMENT_RECEIPT","B07/B08","CapabilityRegistryR30 对象可执行率","全部注册与可执行；没有证据不标有效"),
        m("M24","模型实际消费","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B07/B10","r30_model_call 与冻结行匹配率","100%匹配或明确BLOCKED；不得伪造调用"),
        m("M25","矩阵接线","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B08","MatrixRegistryR30 20 项可追溯率","全部可追溯；未产出有明确原因"),
        m("M26","校准时间隔离","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B07/B10","校准样本时间隔离检查通过率","100%；零模型需可定位原因"),
        m("M27","资格预算连续","ENGINEERING","RECORDED_UNVALIDATED","SIGNED_HASHED_MEASUREMENT_RECEIPT","B10","SequentialQualificationR27 全局序号连续、无重置","全部通过"),
        m("M28","主链闭环","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B10/B12","androidTest 正对照贯通 + 非法/过期拦截","合法正对照贯通；非法/过期全部拦截"),
        m("M29","确认队列连续","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B07","确认队列被挤出次数","0"),
        m("M30","主链选择器身份","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B10","r30_decision_explain：selectorConfirmed=FAIL 时 REAL 次数（必须0）","未确认组合不能REAL"),
        m("M31","诊断可追溯","ENGINEERING","RECORDED_UNVALIDATED","SIGNED_HASHED_MEASUREMENT_RECEIPT","B11","DiagnosisRulesR30 规则行动覆盖率","100%或UNKNOWN并给采集动作"),
        m("M32","报告口径一致","ENGINEERING","RECORDED_UNVALIDATED","SIGNED_HASHED_MEASUREMENT_RECEIPT","B11","同快照两章同指标冲突数","0"),
        m("M33","图表回归","ENGINEERING","INSUFFICIENT","TARGET_DEVICE_SCENARIO_RECEIPT","B09/B12","图表页面 androidTest 截图通过数","全部场景可打开且口径一致"),
        m("M34","签名覆盖安装","ENGINEERING","INSUFFICIENT","TARGET_DEVICE_SCENARIO_RECEIPT","B12","覆盖安装首启/二启无崩溃（两证书）","成功；首启/二启无崩溃"),
        m("M35","失效模型撤销","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B10","证书撤销到 DecisionGate 拒绝的耗时","目标≤5000ms；不含APK回滚"),
        m("M36","同池对照","ENGINEERING","INSUFFICIENT","SIGNED_HASHED_MEASUREMENT_RECEIPT","B07/B12","NullEvolutionR27 同池对照输出可复算","有可复算输出；合成不计真实质量"),
        m("Q01","自然ACT命中","QUALITY","INSUFFICIENT","NATURAL_FUTURE_EVIDENCE_REGISTERED_BEFORE_OBSERVATION","B10/B12","真机 FUTURE ACT 命中率（事前登记策略）","≥34%，优先≥37%；未达不得宣称达标"),
        m("Q02","真实频率","QUALITY","NOT_DEPLOYED","NATURAL_FUTURE_EVIDENCE_REGISTERED_BEFORE_OBSERVATION","B10/B12","真机实际出手频率（不补配额）","目标≥15%；质量支持扩大；不得补配额"),
        m("Q03","真实统计优势","QUALITY","INSUFFICIENT","NATURAL_FUTURE_EVIDENCE_REGISTERED_BEFORE_OBSERVATION","B10/B12","SequentialQualificationR27 logEUp ≥ log(1/α_i)","e≥1/alpha，假设与预算成立"),
        m("Q04","跨块稳定","QUALITY","INSUFFICIENT","NATURAL_FUTURE_EVIDENCE_REGISTERED_BEFORE_OBSERVATION","B10/B12","事前冻结窗口规则下各块命中率","窗口规则事前冻结；不得事后选好块"),
        m("Q05","校准与BSS","QUALITY","INSUFFICIENT","NATURAL_FUTURE_EVIDENCE_REGISTERED_BEFORE_OBSERVATION","B10/B12","CalibrationR30：等质量ECE、BSS、支持覆盖（与等宽ECE并列）","ECE目标≤0.04；样本≥200且支持箱覆盖质量≥90%；不要求5个非空箱；BSS、区间、支持范围如实报告"),
        m("Q06","进化提升","QUALITY","INSUFFICIENT","NATURAL_FUTURE_EVIDENCE_REGISTERED_BEFORE_OBSERVATION","B07/B12","父子配对差单侧95%下界","有效区间下界>0才计验证改善"),
        m("Q07","有效资产产量","QUALITY","RECORDED_UNVALIDATED","NATURAL_FUTURE_EVIDENCE_REGISTERED_BEFORE_OBSERVATION","B10/B12","ACTIVE 证书资产数","目标≥1，继续寻找多个互补资产；0必须如实"),
        m("Q08","空信号与优势检出力","QUALITY","INSUFFICIENT","NATURAL_FUTURE_EVIDENCE_REGISTERED_BEFORE_OBSERVATION","B07/B12","R30SequentialNullSimTest 空信号/功效 + NullEvolutionR27","报告误授证率及CI、34/37/40%信号功效，不以一次模拟签收")
    ));
    public static List<Check> checks() { return CHECKS; }
    public static List<Metric> definitions() { return METRICS; }
    public static Metric metric(String id) { for (Metric x : METRICS) if (x.id.equals(id)) return x; throw new IllegalArgumentException("R31_METRIC_UNKNOWN:" + id); }
    public static List<Check> supportingChecks(String metricId) { metric(metricId); List<Check> out = new ArrayList<>(); for (Check x : CHECKS) if (x.metrics.contains(metricId)) out.add(x); return Collections.unmodifiableList(out); }
    /** What a metric still needs beyond its supporting checks; never empty for quality or device metrics. */
    public static List<String> uncoveredConditions(String metricId) {
        Metric x = metric(metricId); List<String> out = new ArrayList<>(); List<Check> s = supportingChecks(metricId);
        if (s.isEmpty()) out.add("NO_SUPPORTING_CHECK:" + x.requiredEvidence);
        boolean runtime = false; for (Check k : s) runtime |= k.scope != Scope.STRUCTURE && k.scope != Scope.INDEX;
        if (!runtime && !x.quality()) out.add("STRUCTURE_ONLY_NEEDS_RUNTIME_MEASUREMENT");
        if (x.quality()) out.add("NATURAL_QUALITY_NEEDS:" + x.requiredEvidence);
        if (x.deviceRequired()) out.add("DEVICE_NEEDS:" + x.requiredEvidence);
        return Collections.unmodifiableList(out);
    }
    /** Every check id is unique and mapped; every metric target exists and has its own evidence requirement. */
    public static void validateCoverage() {
        Set<String> ids = new HashSet<>(); for (Check k : CHECKS) { if (!ids.add(k.id)) throw new IllegalStateException("R31_DUPLICATE_CHECK:" + k.id); for (String id : k.metrics) metric(id); if (k.scope == Scope.INDEX && !k.metrics.isEmpty()) throw new IllegalStateException("R31_INDEX_CHECK_SUPPORTS_METRIC:" + k.id); }
        if (CHECKS.size() != 96) throw new IllegalStateException("R31_CHECK_COUNT:" + CHECKS.size());
        Set<String> mids = new HashSet<>(); for (Metric x : METRICS) { if (!mids.add(x.id)) throw new IllegalStateException("R31_DUPLICATE_METRIC:" + x.id); if (x.requiredEvidence.isEmpty()) throw new IllegalStateException("R31_METRIC_EVIDENCE:" + x.id); }
        if (METRICS.size() != 44) throw new IllegalStateException("R31_METRIC_COUNT:" + METRICS.size());
    }
    /**
     * Status a metric may claim from checks alone and its own evidence receipt: a structure/static PASS never makes
     * a quality or device metric PASS; without its own receipt the best a metric can be is SUPPORTED_NOT_PROVEN.
     */
    public static String metricStatusFromChecks(String metricId, boolean ownEvidenceReceiptPassed) {
        Metric x = metric(metricId); List<Check> s = supportingChecks(metricId);
        boolean anyFail = false, structurePass = false, runtimePass = false;
        for (Check k : s) { if ("FAIL".equals(k.domainOutcome) || "BLOCKED".equals(k.domainOutcome)) anyFail = true; if (k.domainPass()) { if (k.scope == Scope.STRUCTURE) structurePass = true; else if (k.scope != Scope.INDEX) runtimePass = true; } }
        if (anyFail) return "FAIL_OR_BLOCKED_BY_CHECK";
        if ((x.quality() || x.deviceRequired()) && !ownEvidenceReceiptPassed) return s.isEmpty() ? "UNKNOWN_NO_EVIDENCE" : "SUPPORTED_NOT_PROVEN";
        if (ownEvidenceReceiptPassed && (runtimePass || x.quality() || x.deviceRequired())) return "PASS";
        if (structurePass) return "STRUCTURE_SUBCONDITION_ONLY";
        return s.isEmpty() ? "UNKNOWN_NO_EVIDENCE" : "SUPPORTED_NOT_PROVEN";
    }
    /** Report coverage (lawful results / applicable) and result pass rate, never merged. */
    public static Map<String, Object> coverage(Map<String, String> metricResults) {
        int applicable = METRICS.size(), assessed = 0, passed = 0, unknown = 0;
        for (Metric x : METRICS) { String r = metricResults.get(x.id); if (r == null || r.startsWith("UNKNOWN")) { unknown++; continue; } assessed++; if (r.equals("PASS")) passed++; }
        Map<String, Object> m = new LinkedHashMap<>(); m.put("applicable", (long) applicable); m.put("assessed", (long) assessed); m.put("unknown", (long) unknown);
        m.put("reportCoverage", assessed / (double) applicable); m.put("passRate", assessed == 0 ? null : passed / (double) assessed);
        m.put("productAllPassed", passed == applicable); m.put("checksExecuted", (long) CHECKS.size()); return m;
    }
}
