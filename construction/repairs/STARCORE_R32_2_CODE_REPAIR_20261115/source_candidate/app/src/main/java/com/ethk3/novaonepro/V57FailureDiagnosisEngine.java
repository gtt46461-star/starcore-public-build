package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * B14 dynamic failure diagnosis: confirmed facts are separated from hypotheses and every
 * recommendation has evidence.
 */
public final class V57FailureDiagnosisEngine {
    public static final String OWNER = "B14_FAILURE_DIAGNOSIS_OWNER";

    public enum EvidenceLevel {
        CONFIRMED,
        RUNTIME_OBSERVED,
        INFERRED,
        HYPOTHESIS,
        UNAVAILABLE
    }

    public enum Severity {
        INFO,
        WATCH,
        RISK,
        HARD_BLOCK,
        CRITICAL
    }

    public static final class Finding {
        public final String code,
                title,
                owner,
                positiveEvidence,
                negativeEvidence,
                why,
                repair,
                verification;
        public final EvidenceLevel level;
        public final Severity severity;
        public final double confidence;
        public final String checksum;

        Finding(
                String code,
                String title,
                String owner,
                EvidenceLevel level,
                Severity severity,
                double confidence,
                String positive,
                String negative,
                String why,
                String repair,
                String verification) {
            this.code = s(code);
            this.title = s(title);
            this.owner = s(owner);
            this.level = level;
            this.severity = severity;
            this.confidence = clamp01(confidence);
            positiveEvidence = s(positive);
            negativeEvidence = s(negative);
            this.why = s(why);
            this.repair = s(repair);
            this.verification = s(verification);
            checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return code
                    + '|'
                    + title
                    + '|'
                    + owner
                    + '|'
                    + level.name()
                    + '|'
                    + severity.name()
                    + '|'
                    + confidence
                    + '|'
                    + positiveEvidence
                    + '|'
                    + negativeEvidence
                    + '|'
                    + why
                    + '|'
                    + repair
                    + '|'
                    + verification;
        }
    }

    public static final class FaultNode {
        public final String id, parent, owner, state, evidence;
        public final double score;
        public final String checksum;

        FaultNode(
                String id,
                String parent,
                String owner,
                String state,
                double score,
                String evidence) {
            this.id = s(id);
            this.parent = s(parent);
            this.owner = s(owner);
            this.state = s(state);
            this.score = clamp01(score);
            this.evidence = s(evidence);
            checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return id + '|' + parent + '|' + owner + '|' + state + '|' + score + '|' + evidence;
        }
    }

    public static final class RepairAction {
        public final int priority;
        public final String owner, fileOrModule, method, change, reason, test, rollback;
        public final double expectedImpact, risk;
        public final String checksum;

        RepairAction(
                int p,
                String owner,
                String f,
                String m,
                String c,
                String reason,
                String test,
                String rollback,
                double impact,
                double risk) {
            priority = p;
            this.owner = s(owner);
            fileOrModule = s(f);
            method = s(m);
            change = s(c);
            this.reason = s(reason);
            this.test = s(test);
            this.rollback = s(rollback);
            expectedImpact = clamp01(impact);
            this.risk = clamp01(risk);
            checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return priority
                    + '|'
                    + owner
                    + '|'
                    + fileOrModule
                    + '|'
                    + method
                    + '|'
                    + change
                    + '|'
                    + reason
                    + '|'
                    + test
                    + '|'
                    + rollback
                    + '|'
                    + expectedImpact
                    + '|'
                    + risk;
        }
    }

    public static final class Report {
        public final long createdAt;
        public final Finding[] findings;
        public final FaultNode[] graph;
        public final RepairAction[] repairs;
        public final String verdict, rootCause, checksum;

        Report(long t, Finding[] f, FaultNode[] g, RepairAction[] r, String verdict, String root) {
            createdAt = t;
            findings = f;
            graph = g;
            repairs = r;
            this.verdict = s(verdict);
            rootCause = s(root);
            StringBuilder b = new StringBuilder(verdict).append('|').append(rootCause);
            for (int i = 0; i < f.length; i++) b.append('|').append(f[i].checksum);
            for (int i = 0; i < g.length; i++) b.append('|').append(g[i].checksum);
            for (int i = 0; i < r.length; i++) b.append('|').append(r[i].checksum);
            checksum = V51ChecksumEngine.sha256(b.toString());
        }
    }

    public Report diagnose(DbStore store) {
        long now = System.currentTimeMillis();
        ArrayList<Finding> f = new ArrayList<Finding>();
        ArrayList<FaultNode> g = new ArrayList<FaultNode>();
        ArrayList<RepairAction> r = new ArrayList<RepairAction>();
        AutoFetchService.B13RuntimeHealth health = AutoFetchService.b13Health(store);
        boolean b13 = health.businessAlive;
        f.add(
                new Finding(
                        "B14-RUNTIME",
                        b13 ? "业务循环活跃" : "业务循环停滞",
                        "AutoFetchService",
                        EvidenceLevel.RUNTIME_OBSERVED,
                        b13 ? Severity.INFO : Severity.CRITICAL,
                        b13 ? 0.95 : 0.90,
                        AutoFetchService.b13Diagnostics(store),
                        "仅Service存活不能证明业务存活",
                        "Process/Service/Fetch/Analysis/Settlement必须分层判断",
                        "检查自启动/电池优化/调度/异常并恢复最后业务阶段",
                        "息屏+HOME+划掉任务后连续观察业务beat"));

        boolean numberProven = V51NumberModelArtifact.PROVEN;
        boolean rankProven =
                V51ActionRankerArtifact.PROVEN && V51ActionRankerArtifact.RANK_ORDER_PASSED;
        boolean calProven = V51SelectedCalibrationArtifact.PROVEN;
        boolean policyProven = V51PolicySurfaceArtifact.MODEL_GATE_PASSED;
        boolean modelChain = numberProven && rankProven && calProven && policyProven;
        f.add(
                new Finding(
                        "B14-MODEL-PROOF",
                        modelChain ? "模型证明链完整" : "模型证明链未完成",
                        "B09-B11",
                        EvidenceLevel.CONFIRMED,
                        modelChain ? Severity.INFO : Severity.HARD_BLOCK,
                        1.0,
                        "Number="
                                + numberProven
                                + "|Rank="
                                + rankProven
                                + "|Calibration="
                                + calProven
                                + "|Policy="
                                + policyProven,
                        "不能用工程实现代替严格OOS证明",
                        "REAL必须依赖冻结Artifact、时间顺序OOS、校准和证书",
                        "继续SHADOW积累合法pre-draw样本；训练/冻结/Parity后再证明",
                        "冻结段独立复算+四块证书+ECE/Brier/Wilson"));
        V51Repository.DiagnosticStats stats =
                store == null ? null : V51Repository.diagnosticStats(store);
        String statText = stats == null ? "NO_DB_STATS" : reflectStats(stats);
        f.add(
                new Finding(
                        "B14-REAL-SHADOW",
                        "真实/影子统计隔离审计",
                        "V51Repository",
                        stats == null ? EvidenceLevel.UNAVAILABLE : EvidenceLevel.RUNTIME_OBSERVED,
                        Severity.WATCH,
                        stats == null ? 0.2 : 0.9,
                        statText,
                        "SHADOW绝不能写REAL盈亏/胜率",
                        "统计污染会直接使40/50/60结论失真",
                        "所有真实指标仅过滤REAL_TRADE+BET+SETTLED；SHADOW单独反事实",
                        "逐条账本scope/action/result交叉计数"));
        String target = store == null ? "" : store.getState("target_issue", "");
        if (target.length() == 0 && store != null)
            target = store.getState("b13_last_target_issue", "");
        V51Repository.B12InvariantAudit inv =
                target.length() == 0 || store == null
                        ? null
                        : V51Repository.auditB12(store, target);
        String durableStatus =
                target.length() == 0 || store == null
                        ? "MISSING"
                        : new AnalysisRequestStoreR36(store).status(target);
        String deferredStatus =
                target.length() == 0 || store == null
                        ? "MISSING"
                        : DeferredEvidenceWriterR36.statusForTarget(store, target);
        boolean deferredInFlight =
                AnalysisRequestStoreR36.CANONICAL_COMMITTED.equals(durableStatus)
                        && (DeferredEvidenceWriterR36.PENDING.equals(deferredStatus)
                                || DeferredEvidenceWriterR36.RUNNING.equals(deferredStatus)
                                || DeferredEvidenceWriterR36.RETRY_WAIT.equals(deferredStatus));
        boolean ledgerInFlight =
                AnalysisRequestStoreR36.REQUESTED.equals(durableStatus)
                        || AnalysisRequestStoreR36.RUNNING.equals(durableStatus)
                        || AnalysisRequestStoreR36.RETRY_WAIT.equals(durableStatus)
                        || "MISSING".equals(durableStatus)
                        || deferredInFlight;
        f.add(
                new Finding(
                        "B14-LEDGER",
                        inv != null && inv.pass
                                ? "当前期账本Invariant通过"
                                : (ledgerInFlight ? "当前期账本等待canonical" : "当前期账本Invariant未证明"),
                        "V51Repository",
                        inv == null ? EvidenceLevel.UNAVAILABLE : EvidenceLevel.RUNTIME_OBSERVED,
                        inv != null && inv.pass
                                ? Severity.INFO
                                : (ledgerInFlight ? Severity.WATCH : Severity.HARD_BLOCK),
                        inv == null ? 0.1 : 0.95,
                        (inv == null ? "NO_TARGET" : inv.detail)
                                + "|analysisRequestStatus="
                                + durableStatus
                                + "|deferredEvidenceStatus="
                                + deferredStatus,
                        "1200/Champion/Funnel/Selected/Auth任一重复或缺失都阻断REAL",
                        "唯一决策和唯一结算依赖不可变账本",
                        "修复缺失/重复写入源并回放同一期验证幂等",
                        "issue=1 number=100 compat=10 full=1200 champion=1 funnel=10 selected<=1"
                            + " auth<=1"));
        g.add(
                new FaultNode(
                        "ROOT",
                        "",
                        "V57",
                        "MODEL_OR_RUNTIME",
                        modelChain && b13 ? 0.05 : 0.9,
                        "modelChain=" + modelChain + "|businessAlive=" + b13));
        g.add(
                new FaultNode(
                        "RUNTIME",
                        "ROOT",
                        "AutoFetchService",
                        health.status,
                        b13 ? 0.05 : 0.95,
                        health.rootCause));
        g.add(
                new FaultNode(
                        "NUMBER_Q",
                        "ROOT",
                        "V51PermutationNumberModelEngine",
                        numberProven ? "PROVEN" : "UNTRAINED",
                        numberProven ? 0.05 : 0.75,
                        V51NumberModelArtifact.MODEL_ID));
        g.add(
                new FaultNode(
                        "RANK",
                        "ROOT",
                        "V51ActionRankerEngine",
                        rankProven ? "PROVEN" : "UNTRAINED",
                        rankProven ? 0.05 : 0.75,
                        V51ActionRankerArtifact.ARTIFACT_ID));
        g.add(
                new FaultNode(
                        "SELECTED_P",
                        "ROOT",
                        "V51SelectedActionCalibrationEngine",
                        calProven ? "PROVEN" : "UNTRAINED",
                        calProven ? 0.05 : 0.85,
                        V51SelectedCalibrationArtifact.ARTIFACT_ID));
        g.add(
                new FaultNode(
                        "POLICY",
                        "ROOT",
                        "V51SelectivePolicyCompiler",
                        policyProven ? "PROVEN" : "UNTRAINED",
                        policyProven ? 0.05 : 0.80,
                        V51PolicySurfaceArtifact.ARTIFACT_ID));
        if (!b13)
            r.add(
                    new RepairAction(
                            1,
                            "Runtime",
                            "AutoFetchService",
                            "businessLoop",
                            "恢复最后成功业务阶段而非仅重启Service",
                            health.rootCause,
                            "息屏/划掉任务/重启后beat连续",
                            "回退至R323后台稳定基线",
                            0.9,
                            0.2));
        if (!numberProven)
            r.add(
                    new RepairAction(
                            2,
                            "NumberQ",
                            "V51PermutationNumberModelEngine",
                            "training",
                            "时间顺序训练9专家Gate+NumberQ并冻结Artifact",
                            "当前Number Artifact未证明",
                            "严格train/val/test/frozen+Sinkhorn parity",
                            "保持UNTRAINED=SHADOW",
                            0.8,
                            0.3));
        if (!rankProven)
            r.add(
                    new RepairAction(
                            3,
                            "Rank",
                            "V51ActionRankerEngine",
                            "timeOrderedOos",
                            "训练1200同一期groupwise ranker并冻结Champion",
                            "当前Rank Artifact未证明",
                            "Rank1/MRR/NDCG/pairwise/四块稳定",
                            "回退research score仅SHADOW",
                            0.75,
                            0.25));
        if (!calProven)
            r.add(
                    new RepairAction(
                            4,
                            "SelectedHitP",
                            "V51SelectedActionCalibrationEngine",
                            "calibration",
                            "只用B11 pre-draw OOS ledger做selection-conditioned calibration",
                            "当前Calibration Artifact未证明",
                            "ECE<=4%+Brier+subgroup+parity",
                            "无Artifact则SelectedHitP无效",
                            0.9,
                            0.2));
        if (!policyProven)
            r.add(
                    new RepairAction(
                            5,
                            "Policy",
                            "V51SelectivePolicyCompiler",
                            "compile",
                            "四块Domain+Window+Conformal证书后才允许CORE/DYNAMIC",
                            "当前Policy Artifact未证明",
                            "coverage不靠债务填充，证书逐cell冻结",
                            "SHADOW",
                            0.85,
                            0.15));
        Collections.sort(
                r,
                new Comparator<RepairAction>() {
                    public int compare(RepairAction a, RepairAction b) {
                        return a.priority - b.priority;
                    }
                });
        String root =
                !b13
                        ? health.rootCause
                        : (!modelChain
                                ? "STRICT_OOS_MODEL_PROOF_NOT_COMPLETE"
                                : (inv != null && !inv.pass
                                        ? "LEDGER_INVARIANT_FAIL"
                                        : "NO_CONFIRMED_CRITICAL_FAULT"));
        String verdict =
                b13 && modelChain && (inv == null || inv.pass)
                        ? "ENGINEERING_AND_MODEL_CHAIN_HEALTHY"
                        : "ENGINEERING_DIAGNOSIS_ACTIVE_REAL_REMAINS_GATED";
        return new Report(
                now,
                f.toArray(new Finding[f.size()]),
                g.toArray(new FaultNode[g.size()]),
                r.toArray(new RepairAction[r.size()]),
                verdict,
                root);
    }

    public String render(DbStore store) {
        Report x = diagnose(store);
        StringBuilder b = new StringBuilder();
        b.append("【B14动态故障诊断】\n结论=")
                .append(x.verdict)
                .append("\n根因=")
                .append(x.rootCause)
                .append("\n证据哈希=")
                .append(x.checksum)
                .append("\n\n【确认事实/假设分离】\n");
        for (int i = 0; i < x.findings.length; i++) {
            Finding f = x.findings[i];
            b.append(i + 1)
                    .append('.')
                    .append(f.code)
                    .append("｜")
                    .append(f.level.name())
                    .append("｜")
                    .append(f.severity.name())
                    .append("｜")
                    .append(f.title)
                    .append("\nOwner=")
                    .append(f.owner)
                    .append("\n正证据=")
                    .append(f.positiveEvidence)
                    .append("\n反证/限制=")
                    .append(f.negativeEvidence)
                    .append("\n为什么=")
                    .append(f.why)
                    .append("\n修复=")
                    .append(f.repair)
                    .append("\n验证=")
                    .append(f.verification)
                    .append("\n\n");
        }
        b.append("【因果故障图】\n");
        for (int i = 0; i < x.graph.length; i++) {
            FaultNode n = x.graph[i];
            b.append(n.parent)
                    .append(" -> ")
                    .append(n.id)
                    .append("｜Owner=")
                    .append(n.owner)
                    .append("｜State=")
                    .append(n.state)
                    .append("｜Score=")
                    .append(n.score)
                    .append("｜Evidence=")
                    .append(n.evidence)
                    .append('\n');
        }
        b.append("\n【修复优先级】\n");
        for (int i = 0; i < x.repairs.length; i++) {
            RepairAction a = x.repairs[i];
            b.append("P")
                    .append(a.priority)
                    .append("｜")
                    .append(a.fileOrModule)
                    .append('#')
                    .append(a.method)
                    .append("｜")
                    .append(a.change)
                    .append("｜原因=")
                    .append(a.reason)
                    .append("｜测试=")
                    .append(a.test)
                    .append("｜回滚=")
                    .append(a.rollback)
                    .append('\n');
        }
        return b.toString();
    }

    private static String reflectStats(Object o) {
        StringBuilder b = new StringBuilder();
        try {
            java.lang.reflect.Field[] f = o.getClass().getFields();
            for (int i = 0; i < f.length; i++) {
                if (i > 0) b.append('|');
                b.append(f[i].getName()).append('=').append(String.valueOf(f[i].get(o)));
            }
        } catch (Throwable e) {
            return "STATS_REFLECTION_FAIL:" + e.getClass().getSimpleName();
        }
        return b.toString();
    }

    private static String s(String x) {
        return x == null ? "" : x;
    }

    private static double clamp01(double x) {
        return Math.max(0, Math.min(1, Double.isNaN(x) || Double.isInfinite(x) ? 0 : x));
    }

public String renderSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        Report x = diagnoseSnapshotCP4(report, store);
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report);
        b.append("【B14动态故障诊断】\n结论=")
                .append(x.verdict)
                .append("\n根因=")
                .append(x.rootCause)
                .append("\n证据哈希=")
                .append(x.checksum)
                .append("\n\n【确认事实/假设分离】\n");
        for (int i = 0; i < x.findings.length; i++) {
            Finding f = x.findings[i];
            b.append(i + 1)
                    .append('.')
                    .append(f.code)
                    .append("｜")
                    .append(f.level.name())
                    .append("｜")
                    .append(f.severity.name())
                    .append("｜")
                    .append(f.title)
                    .append("\nOwner=")
                    .append(f.owner)
                    .append("\n正证据=")
                    .append(f.positiveEvidence)
                    .append("\n反证/限制=")
                    .append(f.negativeEvidence)
                    .append("\n为什么=")
                    .append(f.why)
                    .append("\n修复=")
                    .append(f.repair)
                    .append("\n验证=")
                    .append(f.verification)
                    .append("\n\n");
        }
        b.append("【因果故障图】\n");
        for (int i = 0; i < x.graph.length; i++) {
            FaultNode n = x.graph[i];
            b.append(n.parent)
                    .append(" -> ")
                    .append(n.id)
                    .append("｜Owner=")
                    .append(n.owner)
                    .append("｜State=")
                    .append(n.state)
                    .append("｜Score=")
                    .append(n.score)
                    .append("｜Evidence=")
                    .append(n.evidence)
                    .append('\n');
        }
        b.append("\n【修复优先级】\n");
        for (int i = 0; i < x.repairs.length; i++) {
            RepairAction a = x.repairs[i];
            b.append("P")
                    .append(a.priority)
                    .append("｜")
                    .append(a.fileOrModule)
                    .append('#')
                    .append(a.method)
                    .append("｜")
                    .append(a.change)
                    .append("｜原因=")
                    .append(a.reason)
                    .append("｜测试=")
                    .append(a.test)
                    .append("｜回滚=")
                    .append(a.rollback)
                    .append('\n');
        }
        return b.toString();
    }

public Report diagnoseSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        long now = report.nowMs();
        ArrayList<Finding> f = new ArrayList<Finding>();
        ArrayList<FaultNode> g = new ArrayList<FaultNode>();
        ArrayList<RepairAction> r = new ArrayList<RepairAction>();
        AutoFetchService.B13RuntimeHealth health = AutoFetchService.b13HealthSnapshotCP4(report, store);
        boolean b13 = health.businessAlive;
        f.add(
                new Finding(
                        "B14-RUNTIME",
                        b13 ? "业务循环活跃" : "业务循环停滞",
                        "AutoFetchService",
                        EvidenceLevel.RUNTIME_OBSERVED,
                        b13 ? Severity.INFO : Severity.CRITICAL,
                        b13 ? 0.95 : 0.90,
                        AutoFetchService.b13DiagnosticsSnapshotCP4(report, store),
                        "仅Service存活不能证明业务存活",
                        "Process/Service/Fetch/Analysis/Settlement必须分层判断",
                        "检查自启动/电池优化/调度/异常并恢复最后业务阶段",
                        "息屏+HOME+划掉任务后连续观察业务beat"));

        boolean numberProven = V51NumberModelArtifact.PROVEN;
        boolean rankProven =
                V51ActionRankerArtifact.PROVEN && V51ActionRankerArtifact.RANK_ORDER_PASSED;
        boolean calProven = V51SelectedCalibrationArtifact.PROVEN;
        boolean policyProven = V51PolicySurfaceArtifact.MODEL_GATE_PASSED;
        boolean modelChain = numberProven && rankProven && calProven && policyProven;
        f.add(
                new Finding(
                        "B14-MODEL-PROOF",
                        modelChain ? "模型证明链完整" : "模型证明链未完成",
                        "B09-B11",
                        EvidenceLevel.CONFIRMED,
                        modelChain ? Severity.INFO : Severity.HARD_BLOCK,
                        1.0,
                        "Number="
                                + numberProven
                                + "|Rank="
                                + rankProven
                                + "|Calibration="
                                + calProven
                                + "|Policy="
                                + policyProven,
                        "不能用工程实现代替严格OOS证明",
                        "REAL必须依赖冻结Artifact、时间顺序OOS、校准和证书",
                        "继续SHADOW积累合法pre-draw样本；训练/冻结/Parity后再证明",
                        "冻结段独立复算+四块证书+ECE/Brier/Wilson"));
        V51Repository.DiagnosticStats stats =
                store == null ? null : V51Repository.diagnosticStatsSnapshotCP4(report, store);
        String statText = stats == null ? "NO_DB_STATS" : reflectStatsSnapshotCP4(report, stats);
        f.add(
                new Finding(
                        "B14-REAL-SHADOW",
                        "真实/影子统计隔离审计",
                        "V51Repository",
                        stats == null ? EvidenceLevel.UNAVAILABLE : EvidenceLevel.RUNTIME_OBSERVED,
                        Severity.WATCH,
                        stats == null ? 0.2 : 0.9,
                        statText,
                        "SHADOW绝不能写REAL盈亏/胜率",
                        "统计污染会直接使40/50/60结论失真",
                        "所有真实指标仅过滤REAL_TRADE+BET+SETTLED；SHADOW单独反事实",
                        "逐条账本scope/action/result交叉计数"));
        String target = store == null ? "" : store.getState("target_issue", "");
        if (target.length() == 0 && store != null)
            target = store.getState("b13_last_target_issue", "");
        V51Repository.B12InvariantAudit inv =
                target.length() == 0 || store == null
                        ? null
                        : V51Repository.auditB12SnapshotCP4(report, store, target);
        String durableStatus =
                target.length() == 0 || store == null
                        ? "MISSING"
                        : AnalysisRequestStoreR36.statusSnapshotCP4(report, target);
        String deferredStatus =
                target.length() == 0 || store == null
                        ? "MISSING"
                        : DeferredEvidenceWriterR36.statusForTargetSnapshotCP4(report, store, target);
        boolean deferredInFlight =
                AnalysisRequestStoreR36.CANONICAL_COMMITTED.equals(durableStatus)
                        && (DeferredEvidenceWriterR36.PENDING.equals(deferredStatus)
                                || DeferredEvidenceWriterR36.RUNNING.equals(deferredStatus)
                                || DeferredEvidenceWriterR36.RETRY_WAIT.equals(deferredStatus));
        boolean ledgerInFlight =
                AnalysisRequestStoreR36.REQUESTED.equals(durableStatus)
                        || AnalysisRequestStoreR36.RUNNING.equals(durableStatus)
                        || AnalysisRequestStoreR36.RETRY_WAIT.equals(durableStatus)
                        || "MISSING".equals(durableStatus)
                        || deferredInFlight;
        f.add(
                new Finding(
                        "B14-LEDGER",
                        inv != null && inv.pass
                                ? "当前期账本Invariant通过"
                                : (ledgerInFlight ? "当前期账本等待canonical" : "当前期账本Invariant未证明"),
                        "V51Repository",
                        inv == null ? EvidenceLevel.UNAVAILABLE : EvidenceLevel.RUNTIME_OBSERVED,
                        inv != null && inv.pass
                                ? Severity.INFO
                                : (ledgerInFlight ? Severity.WATCH : Severity.HARD_BLOCK),
                        inv == null ? 0.1 : 0.95,
                        (inv == null ? "NO_TARGET" : inv.detail)
                                + "|analysisRequestStatus="
                                + durableStatus
                                + "|deferredEvidenceStatus="
                                + deferredStatus,
                        "1200/Champion/Funnel/Selected/Auth任一重复或缺失都阻断REAL",
                        "唯一决策和唯一结算依赖不可变账本",
                        "修复缺失/重复写入源并回放同一期验证幂等",
                        "issue=1 number=100 compat=10 full=1200 champion=1 funnel=10 selected<=1"
                            + " auth<=1"));
        g.add(
                new FaultNode(
                        "ROOT",
                        "",
                        "V57",
                        "MODEL_OR_RUNTIME",
                        modelChain && b13 ? 0.05 : 0.9,
                        "modelChain=" + modelChain + "|businessAlive=" + b13));
        g.add(
                new FaultNode(
                        "RUNTIME",
                        "ROOT",
                        "AutoFetchService",
                        health.status,
                        b13 ? 0.05 : 0.95,
                        health.rootCause));
        g.add(
                new FaultNode(
                        "NUMBER_Q",
                        "ROOT",
                        "V51PermutationNumberModelEngine",
                        numberProven ? "PROVEN" : "UNTRAINED",
                        numberProven ? 0.05 : 0.75,
                        V51NumberModelArtifact.MODEL_ID));
        g.add(
                new FaultNode(
                        "RANK",
                        "ROOT",
                        "V51ActionRankerEngine",
                        rankProven ? "PROVEN" : "UNTRAINED",
                        rankProven ? 0.05 : 0.75,
                        V51ActionRankerArtifact.ARTIFACT_ID));
        g.add(
                new FaultNode(
                        "SELECTED_P",
                        "ROOT",
                        "V51SelectedActionCalibrationEngine",
                        calProven ? "PROVEN" : "UNTRAINED",
                        calProven ? 0.05 : 0.85,
                        V51SelectedCalibrationArtifact.ARTIFACT_ID));
        g.add(
                new FaultNode(
                        "POLICY",
                        "ROOT",
                        "V51SelectivePolicyCompiler",
                        policyProven ? "PROVEN" : "UNTRAINED",
                        policyProven ? 0.05 : 0.80,
                        V51PolicySurfaceArtifact.ARTIFACT_ID));
        if (!b13)
            r.add(
                    new RepairAction(
                            1,
                            "Runtime",
                            "AutoFetchService",
                            "businessLoop",
                            "恢复最后成功业务阶段而非仅重启Service",
                            health.rootCause,
                            "息屏/划掉任务/重启后beat连续",
                            "回退至R323后台稳定基线",
                            0.9,
                            0.2));
        if (!numberProven)
            r.add(
                    new RepairAction(
                            2,
                            "NumberQ",
                            "V51PermutationNumberModelEngine",
                            "training",
                            "时间顺序训练9专家Gate+NumberQ并冻结Artifact",
                            "当前Number Artifact未证明",
                            "严格train/val/test/frozen+Sinkhorn parity",
                            "保持UNTRAINED=SHADOW",
                            0.8,
                            0.3));
        if (!rankProven)
            r.add(
                    new RepairAction(
                            3,
                            "Rank",
                            "V51ActionRankerEngine",
                            "timeOrderedOos",
                            "训练1200同一期groupwise ranker并冻结Champion",
                            "当前Rank Artifact未证明",
                            "Rank1/MRR/NDCG/pairwise/四块稳定",
                            "回退research score仅SHADOW",
                            0.75,
                            0.25));
        if (!calProven)
            r.add(
                    new RepairAction(
                            4,
                            "SelectedHitP",
                            "V51SelectedActionCalibrationEngine",
                            "calibration",
                            "只用B11 pre-draw OOS ledger做selection-conditioned calibration",
                            "当前Calibration Artifact未证明",
                            "ECE<=4%+Brier+subgroup+parity",
                            "无Artifact则SelectedHitP无效",
                            0.9,
                            0.2));
        if (!policyProven)
            r.add(
                    new RepairAction(
                            5,
                            "Policy",
                            "V51SelectivePolicyCompiler",
                            "compile",
                            "四块Domain+Window+Conformal证书后才允许CORE/DYNAMIC",
                            "当前Policy Artifact未证明",
                            "coverage不靠债务填充，证书逐cell冻结",
                            "SHADOW",
                            0.85,
                            0.15));
        Collections.sort(
                r,
                new Comparator<RepairAction>() {
                    public int compare(RepairAction a, RepairAction b) {
                        return a.priority - b.priority;
                    }
                });
        String root =
                !b13
                        ? health.rootCause
                        : (!modelChain
                                ? "STRICT_OOS_MODEL_PROOF_NOT_COMPLETE"
                                : (inv != null && !inv.pass
                                        ? "LEDGER_INVARIANT_FAIL"
                                        : "NO_CONFIRMED_CRITICAL_FAULT"));
        String verdict =
                b13 && modelChain && (inv == null || inv.pass)
                        ? "ENGINEERING_AND_MODEL_CHAIN_HEALTHY"
                        : "ENGINEERING_DIAGNOSIS_ACTIVE_REAL_REMAINS_GATED";
        return new Report(
                now,
                f.toArray(new Finding[f.size()]),
                g.toArray(new FaultNode[g.size()]),
                r.toArray(new RepairAction[r.size()]),
                verdict,
                root);
    }

public static String reflectStatsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Object o) {
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report);
        try {
            java.lang.reflect.Field[] f = o.getClass().getFields();
            for (int i = 0; i < f.length; i++) {
                if (i > 0) b.append('|');
                b.append(f[i].getName()).append('=').append(String.valueOf(f[i].get(o)));
            }
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
        return b.toString();
    }
}
