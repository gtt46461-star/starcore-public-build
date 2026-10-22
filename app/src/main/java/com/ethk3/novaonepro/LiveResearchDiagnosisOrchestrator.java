package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * B14 read-only diagnosis owner. It snapshots the active runtime in a fixed order, preserves read
 * errors as first-class evidence, builds an acyclic finding graph and emits a deterministic bundle.
 * It never predicts, changes a threshold, authorizes BET, settles a draw, trains a model or writes
 * a production table.
 */
public final class LiveResearchDiagnosisOrchestrator {
    public static final String OWNER = "B14_B15_RESEARCH_DIAGNOSIS_ORCHESTRATOR_ONLY";
    public static final String VERSION = AppConfig.R36_RUNTIME_REPAIR_VERSION + "-B14-DIAGNOSIS-DAG-R908";
    public static final String GAME_CODE = "Binance1-PK10";
    public static final String MODE = "READ_ONLY_SHADOW_DIAGNOSIS";

    public enum Stage {
        DATA(10, "数据与期号"),
        RUNTIME(20, "Service与业务运行"),
        LEDGER(30, "账本与首写冻结"),
        NUMBER_SURFACE(40, "100号码概率面"),
        CROSS_POSITION(50, "跨位置结构"),
        K3_FUNNEL(60, "固定K3漏斗"),
        ACTION_SPACE(70, "1200正式动作空间"),
        RANKER(80, "动作Ranker"),
        CALIBRATION(90, "选择后校准与严格OOS"),
        FREQUENCY(100, "频率与REAL硬门"),
        FAILURE_TREE(110, "失败树与反证"),
        ABLATION(120, "消融与冲突"),
        CODE_OWNER(130, "代码Owner完整性"),
        IMPROVEMENT(140, "Challenger施工与验证");

        public final int order;
        public final String title;

        Stage(int order, String title) {
            this.order = order;
            this.title = title;
        }
    }

    public enum Status {
        PASS(0),
        OBSERVED(1),
        RETEST_REQUIRED(2),
        NOT_PROVEN(3),
        WARN(4),
        FAIL(5),
        READ_ERROR(6);

        public final int risk;

        Status(int risk) {
            this.risk = risk;
        }
    }

    public enum Severity {
        INFO(0),
        P3(1),
        P2(2),
        P1(3),
        P0(4);

        public final int priority;

        Severity(int priority) {
            this.priority = priority;
        }
    }

    public enum EvidenceGrade {
        CONTRACT,
        STATIC_BUILD,
        DATABASE_OBSERVATION,
        STRICT_OOS,
        PHYSICAL_DEVICE,
        READ_ERROR
    }

    public enum EdgeKind {
        REQUIRES,
        EXPLAINS,
        BLOCKS,
        VALIDATES
    }

    public static final class Metric {
        public final String key;
        public final String value;
        public final String unit;
        public final String source;
        public final boolean available;
        public final String checksum;

        public Metric(String key, String value, String unit, String source, boolean available) {
            this.key = safe(key);
            this.value = safe(value);
            this.unit = safe(unit);
            this.source = safe(source);
            this.available = available;
            this.checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return escape(key)
                    + '|'
                    + escape(value)
                    + '|'
                    + escape(unit)
                    + '|'
                    + escape(source)
                    + '|'
                    + (available ? 1 : 0);
        }
    }

    public static final class Evidence {
        public final String id;
        public final EvidenceGrade grade;
        public final String source;
        public final String observation;
        public final String counterEvidence;
        public final int sampleSize;
        public final double lowerBound;
        public final double upperBound;
        public final boolean readSuccessful;
        public final String checksum;

        public Evidence(
                String id,
                EvidenceGrade grade,
                String source,
                String observation,
                String counterEvidence,
                int sampleSize,
                double lowerBound,
                double upperBound,
                boolean readSuccessful) {
            this.id = safe(id);
            this.grade = grade == null ? EvidenceGrade.READ_ERROR : grade;
            this.source = safe(source);
            this.observation = safe(observation);
            this.counterEvidence = safe(counterEvidence);
            this.sampleSize = sampleSize;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.readSuccessful = readSuccessful;
            this.checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return escape(id)
                    + '|'
                    + grade.name()
                    + '|'
                    + escape(source)
                    + '|'
                    + escape(observation)
                    + '|'
                    + escape(counterEvidence)
                    + '|'
                    + sampleSize
                    + '|'
                    + doubleText(lowerBound)
                    + '|'
                    + doubleText(upperBound)
                    + '|'
                    + (readSuccessful ? 1 : 0);
        }
    }

    public static final class Finding {
        public final String id;
        public final Stage stage;
        public final Severity severity;
        public final Status status;
        public final String category;
        public final String conclusion;
        public final String cause;
        public final String impact;
        public final String owner;
        public final String codeLocation;
        public final String action;
        public final String validation;
        public final String expected;
        public final String rollback;
        public final double confidence;
        public final List<String> evidenceIds;
        public final String checksum;

        public Finding(
                String id,
                Stage stage,
                Severity severity,
                Status status,
                String category,
                String conclusion,
                String cause,
                String impact,
                String owner,
                String codeLocation,
                String action,
                String validation,
                String expected,
                String rollback,
                double confidence,
                List<String> evidenceIds) {
            this.id = safe(id);
            this.stage = stage == null ? Stage.DATA : stage;
            this.severity = severity == null ? Severity.INFO : severity;
            this.status = status == null ? Status.READ_ERROR : status;
            this.category = safe(category);
            this.conclusion = safe(conclusion);
            this.cause = safe(cause);
            this.impact = safe(impact);
            this.owner = safe(owner);
            this.codeLocation = safe(codeLocation);
            this.action = safe(action);
            this.validation = safe(validation);
            this.expected = safe(expected);
            this.rollback = safe(rollback);
            this.confidence = clamp01(confidence);
            this.evidenceIds = immutableStrings(evidenceIds);
            this.checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            StringBuilder out = new StringBuilder(1024);
            out.append(escape(id))
                    .append('|')
                    .append(stage.name())
                    .append('|')
                    .append(severity.name())
                    .append('|')
                    .append(status.name())
                    .append('|')
                    .append(escape(category))
                    .append('|')
                    .append(escape(conclusion))
                    .append('|')
                    .append(escape(cause))
                    .append('|')
                    .append(escape(impact))
                    .append('|')
                    .append(escape(owner))
                    .append('|')
                    .append(escape(codeLocation))
                    .append('|')
                    .append(escape(action))
                    .append('|')
                    .append(escape(validation))
                    .append('|')
                    .append(escape(expected))
                    .append('|')
                    .append(escape(rollback))
                    .append('|')
                    .append(doubleText(confidence));
            for (int i = 0; i < evidenceIds.size(); i++) {
                out.append('|').append(escape(evidenceIds.get(i)));
            }
            return out.toString();
        }
    }

    public static final class DependencyEdge {
        public final String fromFinding;
        public final String toFinding;
        public final EdgeKind kind;
        public final String rationale;
        public final String checksum;

        public DependencyEdge(String from, String to, EdgeKind kind, String rationale) {
            fromFinding = safe(from);
            toFinding = safe(to);
            this.kind = kind == null ? EdgeKind.REQUIRES : kind;
            this.rationale = safe(rationale);
            checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return escape(fromFinding)
                    + '|'
                    + escape(toFinding)
                    + '|'
                    + kind.name()
                    + '|'
                    + escape(rationale);
        }
    }

    public static final class StageArtifact {
        public final Stage stage;
        public final Status status;
        public final String owner;
        public final String source;
        public final String summary;
        public final String detail;
        public final String readError;
        public final long collectedAt;
        public final List<Metric> metrics;
        public final List<Evidence> evidence;
        public final String checksum;

        StageArtifact(
                Stage stage,
                Status status,
                String owner,
                String source,
                String summary,
                String detail,
                String readError,
                long collectedAt,
                List<Metric> metrics,
                List<Evidence> evidence) {
            this.stage = stage;
            this.status = status;
            this.owner = safe(owner);
            this.source = safe(source);
            this.summary = safe(summary);
            this.detail = safe(detail);
            this.readError = safe(readError);
            this.collectedAt = Math.max(0L, collectedAt);
            this.metrics = immutableMetrics(metrics);
            this.evidence = immutableEvidence(evidence);
            this.checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            StringBuilder out = new StringBuilder(detail.length() + 2048);
            out.append(stage.name())
                    .append('|')
                    .append(status.name())
                    .append('|')
                    .append(escape(owner))
                    .append('|')
                    .append(escape(source))
                    .append('|')
                    .append(escape(summary))
                    .append('|')
                    .append(escape(detail))
                    .append('|')
                    .append(escape(readError))
                    .append('|')
                    .append(collectedAt);
            for (int i = 0; i < metrics.size(); i++) {
                out.append("|M:").append(metrics.get(i).checksum);
            }
            for (int i = 0; i < evidence.size(); i++) {
                out.append("|E:").append(evidence.get(i).checksum);
            }
            return out.toString();
        }
    }

    public static final class BundleValidation {
        public final boolean valid;
        public final boolean fixedOrder;
        public final boolean uniqueFindingIds;
        public final boolean evidenceResolved;
        public final boolean edgesResolved;
        public final boolean acyclic;
        public final boolean hashStable;
        public final boolean readOnlyContract;
        public final List<String> violations;
        public final String checksum;

        BundleValidation(
                boolean fixedOrder,
                boolean uniqueFindingIds,
                boolean evidenceResolved,
                boolean edgesResolved,
                boolean acyclic,
                boolean hashStable,
                boolean readOnlyContract,
                List<String> violations) {
            this.fixedOrder = fixedOrder;
            this.uniqueFindingIds = uniqueFindingIds;
            this.evidenceResolved = evidenceResolved;
            this.edgesResolved = edgesResolved;
            this.acyclic = acyclic;
            this.hashStable = hashStable;
            this.readOnlyContract = readOnlyContract;
            this.violations = immutableStrings(violations);
            valid =
                    fixedOrder
                            && uniqueFindingIds
                            && evidenceResolved
                            && edgesResolved
                            && acyclic
                            && hashStable
                            && readOnlyContract
                            && this.violations.isEmpty();
            checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            StringBuilder out = new StringBuilder();
            out.append(fixedOrder ? 1 : 0)
                    .append('|')
                    .append(uniqueFindingIds ? 1 : 0)
                    .append('|')
                    .append(evidenceResolved ? 1 : 0)
                    .append('|')
                    .append(edgesResolved ? 1 : 0)
                    .append('|')
                    .append(acyclic ? 1 : 0)
                    .append('|')
                    .append(hashStable ? 1 : 0)
                    .append('|')
                    .append(readOnlyContract ? 1 : 0);
            for (int i = 0; i < violations.size(); i++) {
                out.append('|').append(escape(violations.get(i)));
            }
            return out.toString();
        }
    }

    public static final class DiagnosisBundle {
        public final long createdAt;
        public final String version;
        public final String gameCode;
        public final String mode;
        public final String targetIssue;
        public final List<StageArtifact> stages;
        public final List<Finding> findings;
        public final List<DependencyEdge> edges;
        public final Status overallStatus;
        public final String primaryFindingId;
        public final String canonicalPayload;
        public final String diagnosisHash;
        public final BundleValidation validation;

        DiagnosisBundle(
                long createdAt,
                String targetIssue,
                List<StageArtifact> stages,
                List<Finding> findings,
                List<DependencyEdge> edges,
                Status overallStatus,
                String primaryFindingId,
                String canonicalPayload,
                String diagnosisHash,
                BundleValidation validation) {
            this.createdAt = createdAt;
            version = VERSION;
            gameCode = GAME_CODE;
            mode = MODE;
            this.targetIssue = safe(targetIssue);
            this.stages = immutableStages(stages);
            this.findings = immutableFindings(findings);
            this.edges = immutableEdges(edges);
            this.overallStatus = overallStatus;
            this.primaryFindingId = safe(primaryFindingId);
            this.canonicalPayload = safe(canonicalPayload);
            this.diagnosisHash = safe(diagnosisHash);
            this.validation = validation;
        }

        public StageArtifact stage(Stage stage) {
            for (int i = 0; i < stages.size(); i++) {
                if (stages.get(i).stage == stage) return stages.get(i);
            }
            return null;
        }

        public Finding finding(String id) {
            String key = safe(id);
            for (int i = 0; i < findings.size(); i++) {
                if (findings.get(i).id.equals(key)) return findings.get(i);
            }
            return null;
        }

        public int count(Status status) {
            int count = 0;
            for (int i = 0; i < stages.size(); i++) {
                if (stages.get(i).status == status) count++;
            }
            return count;
        }

        public String renderSummary() {
            StringBuilder out = new StringBuilder(4096);
            out.append("Version=")
                    .append(version)
                    .append("\nMode=")
                    .append(mode)
                    .append("\nTargetIssue=")
                    .append(targetIssue)
                    .append("\nOverallStatus=")
                    .append(overallStatus.name())
                    .append("\nPrimaryFinding=")
                    .append(primaryFindingId)
                    .append("\nStages=")
                    .append(stages.size())
                    .append("|Findings=")
                    .append(findings.size())
                    .append("|Edges=")
                    .append(edges.size())
                    .append("\nREAD_ERROR=")
                    .append(count(Status.READ_ERROR))
                    .append("|FAIL=")
                    .append(count(Status.FAIL))
                    .append("|NOT_PROVEN=")
                    .append(count(Status.NOT_PROVEN))
                    .append("|RETEST_REQUIRED=")
                    .append(count(Status.RETEST_REQUIRED))
                    .append("\nDAG_VALID=")
                    .append(validation.valid)
                    .append("\nDiagnosisHash=")
                    .append(diagnosisHash);
            return out.toString();
        }
    }

    public static final class ReplayInput {
        public final long createdAt;
        public final String targetIssue;
        public final List<StageArtifact> stages;

        public ReplayInput(long createdAt, String targetIssue, List<StageArtifact> stages) {
            this.createdAt = Math.max(0L, createdAt);
            this.targetIssue = safe(targetIssue);
            this.stages = immutableStages(stages);
        }
    }

    public static final class ReplayResult {
        public final DiagnosisBundle first;
        public final DiagnosisBundle second;
        public final boolean deterministic;
        public final boolean dagValid;
        public final String checksum;

        ReplayResult(DiagnosisBundle first, DiagnosisBundle second) {
            this.first = first;
            this.second = second;
            deterministic = first.diagnosisHash.equals(second.diagnosisHash);
            dagValid = first.validation.valid && second.validation.valid;
            checksum =
                    V51ChecksumEngine.sha256(
                            first.diagnosisHash
                                    + '|'
                                    + second.diagnosisHash
                                    + '|'
                                    + deterministic
                                    + '|'
                                    + dagValid);
        }
    }

    private interface StageCollector {
        StageArtifact collect(DbStore store, DiagnosisContext context) throws Exception;
    }

    private static final class DiagnosisContext {
        final long createdAt;
        final String targetIssue;
        final ModelEffectivenessAnalyzerR35.Analysis model;
        final RuntimeHealthLedgerR36.BusinessTruth businessTruth;

        DiagnosisContext(
                long createdAt,
                String targetIssue,
                ModelEffectivenessAnalyzerR35.Analysis model,
                RuntimeHealthLedgerR36.BusinessTruth businessTruth) {
            this.createdAt = createdAt;
            this.targetIssue = safe(targetIssue);
            this.model = model;
            this.businessTruth = businessTruth;
        }
    }

    public DiagnosisBundle diagnose(DbStore store) {
        long now = System.currentTimeMillis();
        ModelEffectivenessAnalyzerR35.Analysis model =
                new ModelEffectivenessAnalyzerR35().analyze(store);
        RuntimeHealthLedgerR36.BusinessTruth truth =
                RuntimeHealthLedgerR36.businessTruth(store);
        String target =
                truth.latestDrawIssue.length() == 0
                        ? state(store, "target_issue", "")
                        : AppConfig.nextIssueText(truth.latestDrawIssue);
        DiagnosisContext context =
                new DiagnosisContext(now, target, model, truth);
        List<StageArtifact> stages = collectAll(store, context);
        return assemble(now, context.targetIssue, stages);
    }

    public DiagnosisBundle diagnoseAt(DbStore store, long now) {
        long at = Math.max(0L, now);
        ModelEffectivenessAnalyzerR35.Analysis model =
                new ModelEffectivenessAnalyzerR35().analyze(store);
        RuntimeHealthLedgerR36.BusinessTruth truth =
                RuntimeHealthLedgerR36.businessTruth(store);
        String target =
                truth.latestDrawIssue.length() == 0
                        ? state(store, "target_issue", "")
                        : AppConfig.nextIssueText(truth.latestDrawIssue);
        DiagnosisContext context =
                new DiagnosisContext(at, target, model, truth);
        return assemble(at, context.targetIssue, collectAll(store, context));
    }

    private List<StageArtifact> collectAll(DbStore store, DiagnosisContext context) {
        LinkedHashMap<Stage, StageCollector> collectors = collectors();
        ArrayList<StageArtifact> out = new ArrayList<StageArtifact>(collectors.size());
        for (Map.Entry<Stage, StageCollector> entry : collectors.entrySet()) {
            out.add(safeCollect(entry.getKey(), entry.getValue(), store, context));
        }
        return out;
    }

    private LinkedHashMap<Stage, StageCollector> collectors() {
        LinkedHashMap<Stage, StageCollector> out = new LinkedHashMap<Stage, StageCollector>();
        out.put(
                Stage.DATA,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectData(store, context);
                    }
                });
        out.put(
                Stage.RUNTIME,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectRuntime(store, context);
                    }
                });
        out.put(
                Stage.LEDGER,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectLedger(store, context);
                    }
                });
        out.put(
                Stage.NUMBER_SURFACE,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectNumberSurface(store, context);
                    }
                });
        out.put(
                Stage.CROSS_POSITION,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectCrossPosition(store, context);
                    }
                });
        out.put(
                Stage.K3_FUNNEL,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectK3Funnel(store, context);
                    }
                });
        out.put(
                Stage.ACTION_SPACE,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectActionSpace(store, context);
                    }
                });
        out.put(
                Stage.RANKER,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectRanker(store, context);
                    }
                });
        out.put(
                Stage.CALIBRATION,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectCalibration(store, context);
                    }
                });
        out.put(
                Stage.FREQUENCY,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectFrequency(store, context);
                    }
                });
        out.put(
                Stage.FAILURE_TREE,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectFailureTree(store, context);
                    }
                });
        out.put(
                Stage.ABLATION,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectAblation(store, context);
                    }
                });
        out.put(
                Stage.CODE_OWNER,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectCodeOwner(store, context);
                    }
                });
        out.put(
                Stage.IMPROVEMENT,
                new StageCollector() {
                    public StageArtifact collect(DbStore store, DiagnosisContext context) {
                        return collectImprovement(store, context);
                    }
                });
        return out;
    }

    private StageArtifact safeCollect(
            Stage stage, StageCollector collector, DbStore store, DiagnosisContext context) {
        try {
            StageArtifact value = collector.collect(store, context);
            if (value == null) throw new IllegalStateException("NULL_STAGE_ARTIFACT");
            if (value.stage != stage) throw new IllegalStateException("STAGE_IDENTITY_MISMATCH");
            return value;
        } catch (Throwable error) {
            String message = errorText(error);
            ArrayList<Evidence> evidence = new ArrayList<Evidence>();
            evidence.add(
                    new Evidence(
                            evidenceId(stage, "READ_ERROR"),
                            EvidenceGrade.READ_ERROR,
                            collector.getClass().getName(),
                            "阶段读取失败，数值不得回退为0或PASS",
                            message,
                            -1,
                            -1.0d,
                            -1.0d,
                            false));
            return artifact(
                    stage,
                    Status.READ_ERROR,
                    stageOwner(stage),
                    collector.getClass().getName(),
                    "READ_ERROR",
                    "READ_ERROR=" + message,
                    message,
                    context.createdAt,
                    Collections.<Metric>emptyList(),
                    evidence);
        }
    }

    private StageArtifact collectData(DbStore store, DiagnosisContext context) {
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        if (store == null) return readError(Stage.DATA, context, "STORE_NULL", "DbStore");
        String base = state(store, "current_issue", "");
        String target = context.targetIssue;
        TargetIssueResolver.Resolution resolution = TargetIssueResolver.next(base);
        boolean targetValid = target.length() > 0 && TargetIssueResolver.isValid(target);
        boolean exactNext =
                resolution.valid && targetValid && resolution.targetIssue.equals(target);
        metrics.add(metric("baseIssue", base, "issue", "app_state", base.length() > 0));
        metrics.add(metric("targetIssue", target, "issue", "app_state", targetValid));
        metrics.add(
                metric(
                        "exactNextTarget",
                        String.valueOf(exactNext),
                        "boolean",
                        "TargetIssueResolver",
                        resolution.valid));
        String observation =
                "base="
                        + base
                        + "|target="
                        + target
                        + "|resolver="
                        + resolution.targetIssue
                        + "|exact="
                        + exactNext;
        evidence.add(
                new Evidence(
                        evidenceId(Stage.DATA, "ISSUE"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "TargetIssueResolver+app_state",
                        observation,
                        "空期号或非精确下一期会阻断正式分析",
                        targetValid ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        Status status = exactNext ? Status.PASS : Status.WARN;
        return artifact(
                Stage.DATA,
                status,
                "TargetIssueResolver_ONLY",
                "DbStore.app_state",
                exactNext ? "精确下一期已解析" : "期号需运行时复核",
                observation,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

    private StageArtifact collectRuntime(DbStore store, DiagnosisContext context) {
        if (store == null) return readError(Stage.RUNTIME, context, "STORE_NULL", "DbStore");
        AutoFetchService.B13RuntimeHealth health = AutoFetchService.b13Health(store);
        RuntimeHealthLedgerR36.BusinessTruth business = context.businessTruth;
        ModelEffectivenessAnalyzerR35.RuntimeSummary runtime = context.model.runtime;
        String durable = new AnalysisRequestStoreR36(store).diagnostic(context.targetIssue);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(latestAttemptMetricR76(runtime));
        metrics.add(
                metric(
                        "serviceAlive",
                        business.serviceAlive ? "1" : "0",
                        "boolean",
                        "RuntimeHealthLedgerR36.BusinessTruth",
                        true));
        metrics.add(
                metric(
                        "businessAlive",
                        business.businessAlive ? "1" : "0",
                        "boolean",
                        "RuntimeHealthLedgerR36.BusinessTruth+B13",
                        true));
        metrics.add(
                metric(
                        "rootCause",
                        business.rootCause,
                        "state",
                        "RuntimeHealthLedgerR36.BusinessTruth",
                        business.readError.length() == 0));
        metrics.add(
                metric(
                        "serviceAge",
                        String.valueOf(health.serviceAge),
                        "ms",
                        "service heartbeat",
                        health.serviceBeat > 0));
        metrics.add(
                metric(
                        "fetchAge",
                        String.valueOf(health.fetchAge),
                        "ms",
                        "fetch heartbeat",
                        health.fetchBeat > 0));
        metrics.add(
                metric(
                        "analysisAge",
                        String.valueOf(health.analysisAge),
                        "ms",
                        "analysis heartbeat",
                        health.analysisBeat > 0));
        metrics.add(
                metric(
                        "requestToCommitP95",
                        String.valueOf(runtime.analysisP95Ms),
                        "ms",
                        "RuntimeHealthLedgerR36",
                        "OK".equals(runtime.readStatus)));
        metrics.add(
                metric(
                        "durableRequest",
                        durable,
                        "state",
                        "AnalysisRequestStoreR36",
                        !"REQUEST_ROW_MISSING".equals(durable)));
        metrics.add(
                metric(
                        "maxConcurrency",
                        String.valueOf(runtime.maxConcurrency),
                        "count",
                        "RuntimeHealthLedgerR36",
                        "OK".equals(runtime.readStatus)));
        metrics.add(
                metric(
                        "oomCount",
                        String.valueOf(runtime.oomCount),
                        "count",
                        "RuntimeHealthLedgerR36",
                        "OK".equals(runtime.readStatus)));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.RUNTIME, "HEARTBEAT"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "AutoFetchService#b13Health",
                        AutoFetchService.b13Diagnostics(store),
                        "静态APK不能证明熄屏、杀进程或24小时运行",
                        health.businessAlive ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceId(Stage.RUNTIME, "LEDGER"),
                        "READ_ERROR".equals(runtime.readStatus)
                                ? EvidenceGrade.READ_ERROR
                                : EvidenceGrade.DATABASE_OBSERVATION,
                        "ModelEffectivenessAnalyzerR35.runtime",
                        runtime.summary(),
                        "READ_ERROR不得显示成零样本PASS",
                        runtime.eventCount,
                        -1.0d,
                        -1.0d,
                        !"READ_ERROR".equals(runtime.readStatus)));
        evidence.add(
                new Evidence(
                        evidenceId(Stage.RUNTIME, "DURABLE_REQUEST"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "AnalysisRequestStoreR36#diagnostic",
                        durable,
                        "活动外部Owner只能等待；失败线程不得改写其租约",
                        "REQUEST_ROW_MISSING".equals(durable) ? 0 : 1,
                        -1.0d,
                        -1.0d,
                        true));
        Status status;
        String readError = "";
        if ("READ_ERROR".equals(business.rootCause)
                || "READ_ERROR".equals(runtime.readStatus)) {
            status = Status.READ_ERROR;
            readError = business.readError.length() > 0 ? business.readError : runtime.error;
        } else if ("RUNTIME_CHAIN".equals(business.rootCause)
                || "SETTLEMENT_OOS".equals(business.rootCause)
                || "PREDRAW_COVERAGE_MISSING".equals(business.rootCause)
                || !business.businessAlive) {
            status = Status.FAIL;
        } else if ("ANALYSIS_ATTEMPT_FAILED".equals(runtime.lastAttemptType)
                && runtime.lastAttemptAt >= runtime.lastAt) {
            status = Status.FAIL;
        } else if (runtime.eventCount <= 0) {
            status = Status.RETEST_REQUIRED;
        } else if (runtime.maxConcurrency > 1
                || runtime.analysisP95Ms > 5000L
                || runtime.oomCount > 0) {
            status = Status.FAIL;
        } else {
            status = Status.PASS;
        }
        return artifact(
                Stage.RUNTIME,
                status,
                "AutoFetchService->BusinessReconcilerR36->V51RuntimeOrchestrator",
                "B13 heartbeat + RuntimeHealthLedgerR36",
                business.status + '|' + business.rootCause + '|' + runtime.readStatus,
                business.canonical()
                        + "\n"
                        + AutoFetchService.b13Diagnostics(store)
                        + "\n"
                        + runtime.summary()
                        + "\ndurableRequest="
                        + durable,
                readError,
                context.createdAt,
                metrics,
                evidence);
    }

    private StageArtifact collectLedger(DbStore store, DiagnosisContext context) {
        if (store == null) return readError(Stage.LEDGER, context, "STORE_NULL", "DbStore");
        String target = context.targetIssue;
        if (target.length() == 0) {
            return artifact(
                    Stage.LEDGER,
                    Status.RETEST_REQUIRED,
                    "V51Repository",
                    "canonical tables",
                    "NO_TARGET",
                    "尚无目标期，无法审计本期首写冻结与B12不变量",
                    "",
                    context.createdAt,
                    Collections.<Metric>emptyList(),
                    singletonEvidence(
                            evidenceId(Stage.LEDGER, "NO_TARGET"),
                            EvidenceGrade.DATABASE_OBSERVATION,
                            "app_state.target_issue",
                            "NO_TARGET",
                            "需要至少一次物理设备运行闭环",
                            0,
                            true));
        }
        V51Repository.B12InvariantAudit audit = V51Repository.auditB12(store, target);
        String requestStatus = new AnalysisRequestStoreR36(store).status(target);
        String deferredStatus = DeferredEvidenceWriterR36.statusForTarget(store, target);
        boolean deferredInFlight =
                AnalysisRequestStoreR36.CANONICAL_COMMITTED.equals(requestStatus)
                        && (DeferredEvidenceWriterR36.PENDING.equals(deferredStatus)
                                || DeferredEvidenceWriterR36.RUNNING.equals(deferredStatus)
                                || DeferredEvidenceWriterR36.RETRY_WAIT.equals(deferredStatus));
        boolean inFlight =
                AnalysisRequestStoreR36.REQUESTED.equals(requestStatus)
                        || AnalysisRequestStoreR36.RUNNING.equals(requestStatus)
                        || AnalysisRequestStoreR36.RETRY_WAIT.equals(requestStatus)
                        || "MISSING".equals(requestStatus)
                        || deferredInFlight;
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(metric("targetIssue", target, "issue", "V51Repository", true));
        metrics.add(
                metric(
                        "b12InvariantPass",
                        String.valueOf(audit.pass),
                        "boolean",
                        "V51Repository.auditB12",
                        true));
        metrics.add(metric("analysisRequestStatus", requestStatus, "state", "AnalysisRequestStoreR36", true));
        metrics.add(
                metric(
                        "deferredEvidenceStatus",
                        deferredStatus,
                        "state",
                        "DeferredEvidenceWriterR36",
                        !deferredStatus.startsWith("READ_ERROR:")));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.LEDGER, "B12"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "V51Repository#auditB12",
                        audit.detail + "|hash=" + audit.checksum,
                        "审计失败必须保持SHADOW并导出原始DB",
                        1,
                        -1.0d,
                        -1.0d,
                        true));
        return artifact(
                Stage.LEDGER,
                audit.pass ? Status.PASS : (inFlight ? Status.RETEST_REQUIRED : Status.FAIL),
                "V51Repository+CanonicalObservationWriterR36",
                "B12 invariant audit",
                audit.pass
                        ? "B12账本不变量通过"
                        : (inFlight
                                ? "账本等待 canonical 或延后证据完成"
                                : "B12账本不变量失败"),
                audit.detail
                        + "\nanalysisRequestStatus="
                        + requestStatus
                        + "\ndeferredEvidenceStatus="
                        + deferredStatus,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

    /** Bounded current persisted surface; legacy app_state summaries remain independent. */
    static String persistedNumberSurfaceR80(SQLiteDatabase db,String requestedTarget,ReportNativeCP4.Control control)throws Exception {
        org.json.JSONObject out=new org.json.JSONObject().put("scope","ONE_COMMITTED_TARGET_FULL_IDENTITY_MAX100_CELLS")
                .put("efficacy","NOT_PROVEN").put("rawDatabaseIncluded",false)
                .put("historicalCoverage","NOT_EXPORTED_HERE;FULL_RAW_EVIDENCE_REQUIRED_FOR_ALL_ORIGINALS");
        if(db==null)return out.put("status","READ_ERROR_STORE_NULL").toString();
        surfaceCheckpointR80(control);
        for(String table:new String[]{V51Repository.ISSUE,V51Repository.NUMBER,V51Repository.PERMUTATION})
            try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})) {
                if(!c.moveToFirst())return out.put("status","MISSING_SCHEMA").put("missingTable",table).toString();
            }
        String target=safe(requestedTarget),game,bundle,contract,record;
        if(target.length()==0){
            try(Cursor c=db.rawQuery("SELECT target_issue FROM "+V51Repository.ISSUE+" ORDER BY target_issue DESC LIMIT 1",null)){
                if(!c.moveToFirst())return out.put("status","NO_COMMITTED_TARGET").toString();target=c.getString(0);
            }
            out.put("targetSelection","LATEST_COMMITTED_TARGET_PRIMARY_KEY_ORDER");
        }else out.put("targetSelection","EXPLICIT_DIAGNOSIS_TARGET");
        out.put("targetIssue",target);
        try(Cursor c=db.rawQuery("SELECT game_code,bundle_hash,contract_hash,record_hash FROM "+V51Repository.ISSUE+" WHERE target_issue=? LIMIT 1",new String[]{target})){
            if(!c.moveToFirst())return out.put("status","NO_COMMITTED_TARGET").toString();
            game=c.getString(0);bundle=c.getString(1);contract=c.getString(2);record=c.getString(3);
        }
        if(!TargetIssueResolver.isValid(target)||!V51Repository.GAME_CODE.equals(game)||!surfaceHashR80(bundle)||!surfaceHashR80(contract)||!surfaceHashR80(record))
            return out.put("status","INVALID_COMMITTED_IDENTITY").toString();
        out.put("gameCode",game).put("bundleHash",bundle).put("contractHash",contract).put("recordHash",record);
        org.json.JSONArray cells=new org.json.JSONArray();int scanned=0,matched=0,mismatched=0,invalid=0;boolean[][] seen=new boolean[10][10];double[] sums=new double[10];int[] validCellsPerPosition=new int[10];
        try(Cursor c=db.rawQuery("SELECT position,number,raw_score,number_p,number_rank,game_code,bundle_hash,contract_hash,record_hash FROM "+V51Repository.NUMBER+" WHERE target_issue=? ORDER BY position,number LIMIT 101",new String[]{target})){
            while(c.moveToNext()){
                surfaceCheckpointR80(control);scanned++;if(scanned>100)break;
                if(c.getType(0)!=Cursor.FIELD_TYPE_INTEGER||c.getType(1)!=Cursor.FIELD_TYPE_INTEGER||c.getType(4)!=Cursor.FIELD_TYPE_INTEGER||c.getLong(0)<1||c.getLong(0)>10||c.getLong(1)<1||c.getLong(1)>10||c.getLong(4)<1||c.getLong(4)>10){invalid++;continue;}
                int p=c.getInt(0),number=c.getInt(1),rank=c.getInt(4);String rowHash=c.getString(8);
                if(!game.equals(c.getString(5))||!bundle.equals(c.getString(6))||!contract.equals(c.getString(7))||!(record.equals(rowHash)||V51ChecksumEngine.sha256(record+"|N|"+p+'|'+number).equals(rowHash))){mismatched++;continue;}
                matched++;if(!surfaceNumericCellR80(c,2)||!surfaceNumericCellR80(c,3)){invalid++;continue;}double raw=c.getDouble(2),probability=c.getDouble(3);
                if(p<1||p>10||number<1||number>10||rank<1||rank>10||!Double.isFinite(raw)||!Double.isFinite(probability)||probability<0||probability>1||seen[p-1][number-1]){invalid++;continue;}
                seen[p-1][number-1]=true;sums[p-1]+=probability;validCellsPerPosition[p-1]++;
                cells.put(new org.json.JSONObject().put("position",p).put("number",number).put("rawScore",raw).put("numberP",probability).put("rank",rank).put("recordHash",rowHash));
            }
        }
        boolean permutation=false;org.json.JSONObject projection=new org.json.JSONObject().put("status","MISSING");
        try(Cursor c=db.rawQuery("SELECT game_code,bundle_hash,contract_hash,record_hash,matrix_hash,row_error,column_error,iterations,converged,used_fallback FROM "+V51Repository.PERMUTATION+" WHERE target_issue=? LIMIT 1",new String[]{target})){
            if(c.moveToFirst()){
                if(!game.equals(c.getString(0))||!bundle.equals(c.getString(1))||!contract.equals(c.getString(2))||!record.equals(c.getString(3))){mismatched++;projection.put("status","IDENTITY_MISMATCH");}
                else if(!surfaceNumericCellR80(c,5)||!surfaceNumericCellR80(c,6)||c.getType(7)!=Cursor.FIELD_TYPE_INTEGER||c.getType(8)!=Cursor.FIELD_TYPE_INTEGER||c.getType(9)!=Cursor.FIELD_TYPE_INTEGER||c.getLong(7)<0||c.getLong(7)>Integer.MAX_VALUE||(c.getLong(8)!=0&&c.getLong(8)!=1)||(c.getLong(9)!=0&&c.getLong(9)!=1)){projection.put("status","INVALID_METADATA");invalid++;}
                else {double row=c.getDouble(5),col=c.getDouble(6);permutation=surfaceHashR80(c.getString(4))&&Double.isFinite(row)&&Double.isFinite(col)&&row>=0&&col>=0;
                    projection.put("status",permutation?"OBSERVED":"INVALID_METADATA").put("matrixHash",surfaceHashR80(c.getString(4))?c.getString(4):org.json.JSONObject.NULL).put("iterations",c.getInt(7)).put("converged",c.getInt(8)!=0).put("usedFallback",c.getInt(9)!=0);
                    if(permutation)projection.put("rowError",row).put("columnError",col);else invalid++;
                }
            }
        }
        org.json.JSONArray rowSums=new org.json.JSONArray(),rowCounts=new org.json.JSONArray();for(int i=0;i<10;i++){rowCounts.put(validCellsPerPosition[i]);rowSums.put(validCellsPerPosition[i]==10?sums[i]:org.json.JSONObject.NULL);}
        out.put("scannedNumberRowsUpTo101",scanned).put("matchingNumberRows",matched).put("identityMismatchRows",mismatched).put("invalidNumberOrPermutationRows",invalid)
                .put("cells",cells).put("validCellsPerPosition",rowCounts).put("completeRowProbabilitySums",rowSums).put("permutation",projection)
                .put("status",mismatched>0?"IDENTITY_MISMATCH":invalid>0?"INVALID_METADATA":scanned>100?"SURFACE_ROW_BOUND_EXCEEDED":matched==100&&permutation?"OBSERVED_COMPLETE_METADATA":"PARTIAL_METADATA");
        return out.toString();
    }
    private static boolean surfaceNumericCellR80(Cursor c,int column){int type=c.getType(column);return type==Cursor.FIELD_TYPE_INTEGER||type==Cursor.FIELD_TYPE_FLOAT;}
    private static boolean surfaceHashR80(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private static void surfaceCheckpointR80(ReportNativeCP4.Control control)throws java.io.InterruptedIOException {
        if(Thread.currentThread().isInterrupted()||!control.shouldContinue())throw new java.io.InterruptedIOException("REPORT_CANCELLED");
    }
    private static String surfaceEvidenceOrFailureR80(SQLiteDatabase db,String target,ReportNativeCP4.Control control){
        try{return persistedNumberSurfaceR80(db,target,control);}
        catch(java.io.InterruptedIOException cancelled){throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_CONTEXT_CANCELLED",cancelled);}
        catch(ReportReadContextCP4.ReportReadFailureCP4 cancelled){throw cancelled;}
        catch(Exception failure){return "{\"status\":\"READ_ERROR_CURRENT_SURFACE\",\"efficacy\":\"NOT_PROVEN\"}";}
    }

    private StageArtifact collectNumberSurface(DbStore store, DiagnosisContext context) {
        boolean proven = V51NumberModelArtifact.PROVEN;
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(metric("positions", "10", "count", "fixed contract", true));
        metrics.add(metric("numbersPerPosition", "10", "count", "fixed contract", true));
        metrics.add(metric("numberRows", "100", "count", "10x10 formula", true));
        metrics.add(
                metric(
                        "artifactProven",
                        String.valueOf(proven),
                        "boolean",
                        "V51NumberModelArtifact",
                        true));
        String distribution = state(store, "v51_number_distribution_summary", "NO_RUNTIME_SAMPLE");
        String sinkhorn = state(store, "v51_sinkhorn_summary", "NO_RUNTIME_SAMPLE");
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.NUMBER_SURFACE, "CONTRACT"),
                        EvidenceGrade.CONTRACT,
                        "V51PositionActionBuilder",
                        "10 positions * 10 numbers = 100 point-in-time probabilities",
                        "完整性不等于预测有效性",
                        100,
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceId(Stage.NUMBER_SURFACE, "MODEL"),
                        proven ? EvidenceGrade.STRICT_OOS : EvidenceGrade.STATIC_BUILD,
                        "V51NumberModelArtifact.PROVEN",
                        "artifactProof=" + proven,
                        "未冻结训练产物不得用工程实现替代模型证明",
                        proven ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        return artifact(
                Stage.NUMBER_SURFACE,
                proven ? Status.OBSERVED : Status.NOT_PROVEN,
                "V51NumberModelArtifact+V51SinkhornEngine",
                "frozen input and persisted summaries",
                proven ? "号码面产物已冻结，仍需证书复核" : "号码面工程存在但模型未证明",
                "distribution=" + distribution + "\nsinkhorn=" + sinkhorn
                        + "\ncurrentPersistedSurface="+surfaceEvidenceOrFailureR80(store==null?null:store.readable(),context.targetIssue,()->true),
                "",
                context.createdAt,
                metrics,
                evidence);
    }

    private StageArtifact collectCrossPosition(DbStore store, DiagnosisContext context) {
        String relation = state(store, "b06_relation_summary", "NO_RUNTIME_SAMPLE");
        String permutation = state(store, "v51_permutation_summary", "NO_RUNTIME_SAMPLE");
        String transition = state(store, "v51_transition_summary", "NO_RUNTIME_SAMPLE");
        boolean sample =
                !"NO_RUNTIME_SAMPLE".equals(relation)
                        || !"NO_RUNTIME_SAMPLE".equals(permutation)
                        || !"NO_RUNTIME_SAMPLE".equals(transition);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(metric("relationSummary", relation, "text", "app_state", sample));
        metrics.add(metric("permutationSummary", permutation, "text", "app_state", sample));
        metrics.add(metric("transitionSummary", transition, "text", "app_state", sample));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.CROSS_POSITION, "PIT"),
                        EvidenceGrade.CONTRACT,
                        "V51RelationGraphVerifier",
                        "跨位置特征只允许目标开奖前快照",
                        "同期开奖信息进入特征即构成泄漏",
                        sample ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceId(Stage.CROSS_POSITION, "RUNTIME"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "relation/permutation/transition summaries",
                        relation + '|' + permutation + '|' + transition,
                        "摘要存在不等于跨块证书通过",
                        sample ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        return artifact(
                Stage.CROSS_POSITION,
                sample ? Status.OBSERVED : Status.RETEST_REQUIRED,
                "V51RelationGraphVerifier+V51PermutationFeatureEngine",
                "pre-draw cross-position evidence",
                sample ? "跨位置运行证据已观察" : "跨位置等待运行样本",
                "relation="
                        + relation
                        + "\npermutation="
                        + permutation
                        + "\ntransition="
                        + transition,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

    private StageArtifact collectK3Funnel(DbStore store, DiagnosisContext context) {
        int[] sizes = K3FunnelOrchestrator.STAGE_SIZES;
        boolean exact =
                sizes != null
                        && sizes.length == 7
                        && sizes[0] == 9
                        && sizes[1] == 8
                        && sizes[2] == 7
                        && sizes[3] == 6
                        && sizes[4] == 5
                        && sizes[5] == 4
                        && sizes[6] == 3;
        String trace = state(store, "v51_funnel_summary", "NO_RUNTIME_SAMPLE");
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metric(
                        "stageCount",
                        String.valueOf(sizes.length),
                        "count",
                        "K3FunnelOrchestrator",
                        true));
        metrics.add(metric("stageSizes", joinInts(sizes), "count[]", "K3FunnelOrchestrator", true));
        metrics.add(
                metric(
                        "finalK",
                        sizes.length == 0 ? "-1" : String.valueOf(sizes[sizes.length - 1]),
                        "count",
                        "fixed contract",
                        true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.K3_FUNNEL, "STATIC"),
                        EvidenceGrade.STATIC_BUILD,
                        "K3FunnelOrchestrator.STAGE_SIZES",
                        "stages=" + joinInts(sizes) + "|exact=" + exact,
                        "漏斗只保留候选轨迹，不得删除1200正式动作证据",
                        sizes.length,
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceId(Stage.K3_FUNNEL, "RUNTIME"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "B10 proof ledger",
                        trace,
                        "静态阶段常量不能证明物理设备每期持久化成功",
                        "NO_RUNTIME_SAMPLE".equals(trace) ? 0 : 1,
                        -1.0d,
                        -1.0d,
                        true));
        Status status =
                !exact
                        ? Status.FAIL
                        : ("NO_RUNTIME_SAMPLE".equals(trace)
                                ? Status.RETEST_REQUIRED
                                : Status.PASS);
        return artifact(
                Stage.K3_FUNNEL,
                status,
                "K3FunnelOrchestrator_ONLY",
                "exact nested 9/8/7/6/5/4/3 audit",
                exact ? "固定K3漏斗合同成立" : "固定K3漏斗合同破坏",
                "static=" + joinInts(sizes) + "\nruntime=" + trace,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

    private StageArtifact collectActionSpace(DbStore store, DiagnosisContext context) {
        long combinations = K3ActionSpaceEngine.choose(10, 3);
        long formal = 10L * combinations;
        boolean exact = combinations == 120L && formal == K3ActionSpaceEngine.TOTAL_ACTIONS;
        QueryCount persisted =
                queryCount(
                        store,
                        "SELECT COUNT(1) FROM "
                                + V51Repository.ACTION_SPACE
                                + " WHERE target_issue=?",
                        context.targetIssue.length() == 0
                                ? null
                                : new String[] {context.targetIssue});
        String requestStatus = context.targetIssue.length() == 0 ? "MISSING" : new AnalysisRequestStoreR36(store).status(context.targetIssue);
        String deferredStatus = context.targetIssue.length() == 0 ? "MISSING" : DeferredEvidenceWriterR36.statusForTarget(store, context.targetIssue);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(metric("analysisRequestStatus", requestStatus, "state", "AnalysisRequestStoreR36", !requestStatus.startsWith("READ_ERROR")));
        metrics.add(metric("deferredEvidenceStatus", deferredStatus, "state", "DeferredEvidenceWriterR36", !deferredStatus.startsWith("READ_ERROR")));
        metrics.add(
                metric(
                        "combinationPerPosition",
                        String.valueOf(combinations),
                        "count",
                        "C(10,3)",
                        true));
        metrics.add(metric("formalActions", String.valueOf(formal), "count", "10*C(10,3)", exact));
        metrics.add(
                metric(
                        "persistedActions",
                        String.valueOf(persisted.value),
                        "rows",
                        "v51_action_space",
                        persisted.ok));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.ACTION_SPACE, "FORMULA"),
                        EvidenceGrade.CONTRACT,
                        "K3ActionSpaceEngine",
                        "10*C(10,3)=" + formal,
                        "排序、删除或剪枝不能改变正式宇宙基数",
                        (int) formal,
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceId(Stage.ACTION_SPACE, "PERSISTED"),
                        persisted.ok
                                ? EvidenceGrade.DATABASE_OBSERVATION
                                : EvidenceGrade.READ_ERROR,
                        V51Repository.ACTION_SPACE,
                        persisted.render(),
                        "表行数必须按目标期读取，不能拿全表数冒充覆盖",
                        persisted.value,
                        -1.0d,
                        -1.0d,
                        persisted.ok));
        Status status = actionSpaceStatusR77(exact, persisted.ok, persisted.value,
                context.targetIssue, requestStatus, deferredStatus);
        String readError = !persisted.ok ? persisted.error
                : (requestStatus.startsWith("READ_ERROR") ? requestStatus
                : (deferredStatus.startsWith("READ_ERROR") ? deferredStatus : ""));
        return artifact(
                Stage.ACTION_SPACE,
                status,
                "K3ActionSpaceEngine+V51Repository",
                "formal action universe",
                "formula=" + formal + "|persisted=" + persisted.render()
                        + "|deferredEvidenceStatus=" + deferredStatus,
                "ExactIdentityRequired=true|DeletionOrPruning=FORBIDDEN"
                        + "|analysisRequestStatus=" + requestStatus
                        + "|deferredEvidenceStatus=" + deferredStatus
                        + (status == Status.RETEST_REQUIRED ? "|未完成证据待续写，不代表1200动作已全部验收" : ""),
                readError,
                context.createdAt,
                metrics,
                evidence);
    }

    /** Partial deferred persistence is observable progress, never a completed action-space proof. */
    static Status actionSpaceStatusR77(boolean exact, boolean readable, int persisted,
            String target, String request, String deferred) {
        if (!exact) return Status.FAIL;
        if (!readable || request.startsWith("READ_ERROR") || deferred.startsWith("READ_ERROR"))
            return Status.READ_ERROR;
        if (target.length() == 0) return Status.RETEST_REQUIRED;
        if (persisted == K3ActionSpaceEngine.TOTAL_ACTIONS) return Status.PASS;
        if (persisted < 0 || persisted > K3ActionSpaceEngine.TOTAL_ACTIONS) return Status.FAIL;
        boolean deferredPending = AnalysisRequestStoreR36.CANONICAL_COMMITTED.equals(request)
                && (DeferredEvidenceWriterR36.PENDING.equals(deferred)
                    || DeferredEvidenceWriterR36.RUNNING.equals(deferred)
                    || DeferredEvidenceWriterR36.RETRY_WAIT.equals(deferred));
        if (deferredPending) return Status.RETEST_REQUIRED;
        if (persisted == 0 && (AnalysisRequestStoreR36.REQUESTED.equals(request)
                || AnalysisRequestStoreR36.RUNNING.equals(request)
                || AnalysisRequestStoreR36.RETRY_WAIT.equals(request))) return Status.RETEST_REQUIRED;
        return Status.FAIL;
    }

    private StageArtifact collectRanker(DbStore store, DiagnosisContext context) {
        boolean proven =
                V51ActionRankerArtifact.PROVEN && V51ActionRankerArtifact.RANK_ORDER_PASSED;
        String summary = state(store, "v51_ranker_summary", "NO_RUNTIME_SAMPLE");
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metric(
                        "artifactProven",
                        String.valueOf(V51ActionRankerArtifact.PROVEN),
                        "boolean",
                        "V51ActionRankerArtifact",
                        true));
        metrics.add(
                metric(
                        "rankOrderPassed",
                        String.valueOf(V51ActionRankerArtifact.RANK_ORDER_PASSED),
                        "boolean",
                        "V51ActionRankerArtifact",
                        true));
        metrics.add(
                metric(
                        "runtimeSummary",
                        summary,
                        "text",
                        "app_state",
                        !"NO_RUNTIME_SAMPLE".equals(summary)));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.RANKER, "ARTIFACT"),
                        proven ? EvidenceGrade.STRICT_OOS : EvidenceGrade.STATIC_BUILD,
                        "V51ActionRankerArtifact",
                        "PROVEN="
                                + V51ActionRankerArtifact.PROVEN
                                + "|RANK_ORDER="
                                + V51ActionRankerArtifact.RANK_ORDER_PASSED,
                        "工程可以枚举和排序，但不能伪造训练与跨块排名证书",
                        proven ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        return artifact(
                Stage.RANKER,
                proven ? Status.OBSERVED : Status.NOT_PROVEN,
                "V51ActionRankerEngine",
                "frozen rank artifact",
                proven ? "Ranker产物标记已证明，仍需证书哈希复核" : "Ranker未训练/未证明",
                summary,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

    private StageArtifact collectCalibration(DbStore store, DiagnosisContext context) {
        ModelEffectivenessAnalyzerR35.Metrics m = context.model.metrics;
        ModelEffectivenessAnalyzerR35.Coverage c = context.model.coverage;
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metric(
                        "strictOosN",
                        String.valueOf(m.strictOosSample),
                        "sample",
                        "strict OOS ledger",
                        "OK".equals(m.readStatus)));
        metrics.add(
                metric(
                        "hitCount",
                        String.valueOf(m.hitCount),
                        "count",
                        "strict OOS ledger",
                        "OK".equals(m.readStatus)));
        metrics.add(
                metric(
                        "wilsonLower95",
                        doubleText(m.wilsonLower95),
                        "ratio",
                        "Wilson score",
                        m.strictOosSample > 0));
        metrics.add(
                metric("ECE", doubleText(m.ece), "ratio", "selected calibration", m.ece >= 0.0d));
        metrics.add(
                metric(
                        "Brier",
                        doubleText(m.brier),
                        "score",
                        "selected calibration",
                        m.brier >= 0.0d));
        metrics.add(
                metric(
                        "coverage",
                        c.covered + "/" + c.eligible,
                        "targets",
                        "expected target enumeration",
                        "OK".equals(c.readStatus)));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.CALIBRATION, "OOS"),
                        "READ_ERROR".equals(m.readStatus)
                                ? EvidenceGrade.READ_ERROR
                                : EvidenceGrade.STRICT_OOS,
                        "ModelEffectivenessAnalyzerR35.metrics",
                        "read="
                                + m.readStatus
                                + "|N="
                                + m.strictOosSample
                                + "|hit="
                                + m.hitCount
                                + "|wilson="
                                + m.wilsonLower95
                                + "|ECE="
                                + m.ece
                                + "|Brier="
                                + m.brier,
                        "小样本或读取失败不得渲染为命中率证明",
                        m.strictOosSample,
                        m.wilsonLower95,
                        1.0d,
                        !"READ_ERROR".equals(m.readStatus)));
        evidence.add(
                new Evidence(
                        evidenceId(Stage.CALIBRATION, "COVERAGE"),
                        "READ_ERROR".equals(c.readStatus)
                                ? EvidenceGrade.READ_ERROR
                                : EvidenceGrade.DATABASE_OBSERVATION,
                        "ModelEffectivenessAnalyzerR35.coverage",
                        "read="
                                + c.readStatus
                                + "|covered="
                                + c.covered
                                + "|eligible="
                                + c.eligible
                                + "|missing="
                                + c.missingCsv(),
                        "覆盖率分母必须来自可枚举目标期，不能用已有表行数",
                        c.eligible,
                        -1.0d,
                        -1.0d,
                        !"READ_ERROR".equals(c.readStatus)));
        Status status;
        String readError = "";
        if ("READ_ERROR".equals(m.readStatus) || "READ_ERROR".equals(c.readStatus)) {
            status = Status.READ_ERROR;
            readError = safe(m.error) + (safe(c.error).length() > 0 ? "|" + safe(c.error) : "");
        } else if (!V51SelectedCalibrationArtifact.PROVEN) {
            status = Status.NOT_PROVEN;
        } else if (m.strictOosSample < AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE) {
            status = Status.RETEST_REQUIRED;
        } else {
            status = Status.OBSERVED;
        }
        return artifact(
                Stage.CALIBRATION,
                status,
                "OosAfterDrawWriterR36+ModelEffectivenessAnalyzerR35",
                "frozen canonical + exact draw",
                "read=" + m.readStatus + "|model=" + context.model.modelStatus,
                context.model.render(),
                readError,
                context.createdAt,
                metrics,
                evidence);
    }

    private StageArtifact collectFrequency(DbStore store, DiagnosisContext context) {
        boolean locked =
                !AppConfig.R36_REAL_BET_UNLOCK
                        && V51BuildManifest.SHADOW_ONLY
                        && V51BuildManifest.REAL_BET_HARD_DISABLED;
        ControlledRealContractAuditR922.Report realAudit=ControlledRealContractAuditR922.capture(store);
        Status safetyStatus=!"OK".equals(realAudit.readStatus)?Status.READ_ERROR:
                !realAudit.passed?Status.FAIL:
                realAudit.total==0?(locked?Status.PASS:Status.NOT_PROVEN):Status.OBSERVED;
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metric(
                        "realUnlock",
                        String.valueOf(AppConfig.R36_REAL_BET_UNLOCK),
                        "boolean",
                        "AppConfig",
                        true));
        metrics.add(
                metric(
                        "shadowOnly",
                        String.valueOf(V51BuildManifest.SHADOW_ONLY),
                        "boolean",
                        "V51BuildManifest",
                        true));
        metrics.add(metric("requiredRealBet", "0", "count", "unproven model policy", true));
        metrics.add(metric("frequencyDebt", "0", "count", "forbidden policy", true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.FREQUENCY, "REDLINE"),
                        EvidenceGrade.CONTRACT,
                        "DecisionGateEngine+V51BuildManifest",
                        "SHADOW_ONLY="
                                + V51BuildManifest.SHADOW_ONLY
                                + "|REAL_HARD_DISABLED="
                                + V51BuildManifest.REAL_BET_HARD_DISABLED
                                + "|UNLOCK="
                                + AppConfig.R36_REAL_BET_UNLOCK,
                        "频率、欠债、追单、倍投不得绕开模型与实时证明",
                        1,
                        -1.0d,
                        -1.0d,
                        true));
        return artifact(
                Stage.FREQUENCY,
                safetyStatus,
                "DecisionGateEngine_ONLY",
                "compile-time and runtime redlines",
                "实际REAL合同审核="+realAudit.status+"|claimed="+realAudit.total,
                realAudit.summary()+"|modelQuality=SEPARATE_FUTURE_EVALUATION|frequencyDebt=FORBIDDEN_POLICY",
                "",
                context.createdAt,
                metrics,
                evidence);
    }

    private StageArtifact collectFailureTree(DbStore store, DiagnosisContext context) {
        V57FailureDiagnosisEngine failureEngine = new V57FailureDiagnosisEngine();
        K3FailureDiagnosisEngine k3Engine = new K3FailureDiagnosisEngine();
        K3FailureDiagnosisEngine.Report k3 = k3Engine.diagnose(store);
        String detail = failureEngine.render(store) + "\n\n" + k3Engine.render(store);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metric(
                        "k3PrimaryLayer",
                        k3.primaryLayer,
                        "category",
                        "K3FailureDiagnosisEngine",
                        true));
        metrics.add(metric("k3Verdict", k3.verdict, "status", "K3FailureDiagnosisEngine", true));
        metrics.add(
                metric(
                        "k3LayerCount",
                        String.valueOf(k3.layers.length),
                        "count",
                        "K3FailureDiagnosisEngine",
                        true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.FAILURE_TREE, "B14"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "V57FailureDiagnosisEngine+K3FailureDiagnosisEngine",
                        "primary="
                                + k3.primaryLayer
                                + "|verdict="
                                + k3.verdict
                                + "|hash="
                                + k3.checksum,
                        "失败归因是诊断假设，不得直接改动生产参数",
                        k3.layers.length,
                        -1.0d,
                        -1.0d,
                        true));
        Status status =
                "BLOCKED".equals(k3.verdict)
                        ? Status.NOT_PROVEN
                        : ("HEALTHY".equals(k3.verdict) ? Status.OBSERVED : Status.WARN);
        return artifact(
                Stage.FAILURE_TREE,
                status,
                "V57FailureDiagnosisEngine+K3FailureDiagnosisEngine",
                "immutable evidence diagnosis",
                k3.primaryLayer + '|' + k3.verdict,
                detail,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

    private StageArtifact collectAblation(DbStore store, DiagnosisContext context) {
        String conflict = new StrategyConflictAblationEngine().render(store);
        String formula = new FormulaAlgorithmAuditEngine().render(store);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metric(
                        "conflictChars",
                        String.valueOf(conflict.length()),
                        "char",
                        "StrategyConflictAblationEngine",
                        true));
        metrics.add(
                metric(
                        "formulaChars",
                        String.valueOf(formula.length()),
                        "char",
                        "FormulaAlgorithmAuditEngine",
                        true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.ABLATION, "CONFLICT"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "StrategyConflictAblationEngine",
                        V51ChecksumEngine.sha256(conflict),
                        "消融建议必须离线验证，不能在报告线程改权重",
                        conflict.length(),
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceId(Stage.ABLATION, "FORMULA"),
                        EvidenceGrade.STATIC_BUILD,
                        "FormulaAlgorithmAuditEngine",
                        V51ChecksumEngine.sha256(formula),
                        "公式存在不等于效果已证明",
                        formula.length(),
                        -1.0d,
                        -1.0d,
                        true));
        return artifact(
                Stage.ABLATION,
                Status.OBSERVED,
                "StrategyConflictAblationEngine+FormulaAlgorithmAuditEngine",
                "read-only ablation evidence",
                "冲突、消融、公式审计已生成",
                conflict + "\n\n" + formula,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

    private StageArtifact collectCodeOwner(DbStore store, DiagnosisContext context) {
        CodeOwnershipCompletenessAuditEngine ownerEngine =
                new CodeOwnershipCompletenessAuditEngine();
        CodeOwnershipCompletenessAuditEngine.Report ownerReport = ownerEngine.audit(store);
        String audit = ownerEngine.render(store);
        boolean ownerConflict = false;
        if (ownerReport != null && ownerReport.findings != null) {
            for (int i = 0; i < ownerReport.findings.length; i++) {
                CodeOwnershipCompletenessAuditEngine.Finding finding =
                        ownerReport.findings[i];
                if (finding == null || finding.rule == null) continue;
                if ("OWNER_CONFLICT".equals(finding.rule.field)
                        && finding.level
                                == CodeOwnershipCompletenessAuditEngine.Level.HARD_BLOCK) {
                    ownerConflict = true;
                    break;
                }
            }
        }
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metric(
                        "auditChars",
                        String.valueOf(audit.length()),
                        "char",
                        "CodeOwnershipCompletenessAuditEngine",
                        true));
        metrics.add(
                metric(
                        "ownerConflict",
                        String.valueOf(ownerConflict),
                        "boolean",
                        "owner audit",
                        true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.CODE_OWNER, "OWNER"),
                        EvidenceGrade.STATIC_BUILD,
                        "CodeOwnershipCompletenessAuditEngine",
                        V51ChecksumEngine.sha256(audit),
                        "唯一Owner约束必须同时由静态门与运行账本验证",
                        audit.length(),
                        -1.0d,
                        -1.0d,
                        true));
        return artifact(
                Stage.CODE_OWNER,
                ownerReport==null?Status.READ_ERROR:
                    "HARD_BLOCK".equals(ownerReport.verdict)?Status.FAIL:
                    "PASS".equals(ownerReport.verdict)?Status.PASS:Status.WARN,
                "CodeOwnershipCompletenessAuditEngine",
                "owner matrix",
                "完整Owner审核="+(ownerReport==null?"READ_ERROR":ownerReport.verdict)+"|explicitConflict="+ownerConflict,
                audit,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

    private StageArtifact collectImprovement(DbStore store, DiagnosisContext context) {
        String plan = new ImprovementChallengerPlanEngine().render(store);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metric(
                        "planChars",
                        String.valueOf(plan.length()),
                        "char",
                        "ImprovementChallengerPlanEngine",
                        true));
        metrics.add(metric("productionMutation", "0", "count", MODE, true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(Stage.IMPROVEMENT, "PLAN"),
                        EvidenceGrade.CONTRACT,
                        "ImprovementChallengerPlanEngine",
                        V51ChecksumEngine.sha256(plan),
                        "Challenger只有冻结回放证书通过后才能晋级，不可自动解锁REAL",
                        plan.length(),
                        -1.0d,
                        -1.0d,
                        true));
        return artifact(
                Stage.IMPROVEMENT,
                Status.OBSERVED,
                "ImprovementChallengerPlanEngine",
                "read-only change plan",
                "可执行Challenger计划已生成，生产参数未修改",
                plan,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

    private DiagnosisBundle assemble(
            long createdAt, String targetIssue, List<StageArtifact> rawStages) {
        ArrayList<StageArtifact> stages = new ArrayList<StageArtifact>(rawStages);
        Collections.sort(
                stages,
                new Comparator<StageArtifact>() {
                    public int compare(StageArtifact left, StageArtifact right) {
                        return left.stage.order - right.stage.order;
                    }
                });
        ArrayList<Finding> findings = deriveFindings(stages);
        ArrayList<DependencyEdge> edges = deriveEdges(findings);
        sortFindings(findings);
        Status overall = overallStatus(stages);
        String primary = primaryFinding(findings);
        String canonical =
                canonicalBundle(createdAt, targetIssue, stages, findings, edges, overall, primary);
        String hash = V51ChecksumEngine.sha256(canonical);
        BundleValidation validation = validate(stages, findings, edges, canonical, hash);
        return new DiagnosisBundle(
                createdAt,
                targetIssue,
                stages,
                findings,
                edges,
                overall,
                primary,
                canonical,
                hash,
                validation);
    }

    private ArrayList<Finding> deriveFindings(List<StageArtifact> stages) {
        ArrayList<Finding> findings = new ArrayList<Finding>();
        for (int i = 0; i < stages.size(); i++) {
            StageArtifact stage = stages.get(i);
            String id = findingId(stage.stage);
            Severity severity = severity(stage.status);
            String conclusion = stage.summary;
            String cause = causeFor(stage);
            String impact = impactFor(stage);
            String action = actionFor(stage);
            String validation = validationFor(stage);
            String expected = expectedFor(stage);
            String rollback = rollbackFor(stage);
            ArrayList<String> evidenceIds = new ArrayList<String>();
            for (int j = 0; j < stage.evidence.size(); j++) {
                evidenceIds.add(stage.evidence.get(j).id);
            }
            findings.add(
                    new Finding(
                            id,
                            stage.stage,
                            severity,
                            stage.status,
                            stage.stage.name(),
                            conclusion,
                            cause,
                            impact,
                            stage.owner,
                            codeLocationFor(stage.stage),
                            action,
                            validation,
                            expected,
                            rollback,
                            confidenceFor(stage),
                            evidenceIds));
        }
        return findings;
    }

    private ArrayList<DependencyEdge> deriveEdges(List<Finding> findings) {
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < findings.size(); i++) ids.add(findings.get(i).id);
        ArrayList<DependencyEdge> edges = new ArrayList<DependencyEdge>();
        addEdge(edges, ids, Stage.DATA, Stage.RUNTIME, EdgeKind.REQUIRES, "可靠期号先于业务循环判定");
        addEdge(edges, ids, Stage.RUNTIME, Stage.LEDGER, EdgeKind.REQUIRES, "业务运行先产生可审计账本");
        addEdge(edges, ids, Stage.LEDGER, Stage.CALIBRATION, EdgeKind.REQUIRES, "严格OOS只读冻结首写账本");
        addEdge(
                edges,
                ids,
                Stage.NUMBER_SURFACE,
                Stage.CROSS_POSITION,
                EdgeKind.REQUIRES,
                "跨位置组合依赖号码面");
        addEdge(
                edges,
                ids,
                Stage.CROSS_POSITION,
                Stage.K3_FUNNEL,
                EdgeKind.EXPLAINS,
                "跨位置结构解释漏斗边界");
        addEdge(
                edges,
                ids,
                Stage.NUMBER_SURFACE,
                Stage.ACTION_SPACE,
                EdgeKind.REQUIRES,
                "1200动作概率来自100号码面");
        addEdge(
                edges,
                ids,
                Stage.ACTION_SPACE,
                Stage.RANKER,
                EdgeKind.REQUIRES,
                "Ranker不得在不完整宇宙上排序");
        addEdge(edges, ids, Stage.RANKER, Stage.CALIBRATION, EdgeKind.REQUIRES, "选择后校准依赖冻结选择机制");
        addEdge(edges, ids, Stage.CALIBRATION, Stage.FREQUENCY, EdgeKind.BLOCKS, "未证明校准阻断REAL频率");
        addEdge(
                edges,
                ids,
                Stage.FAILURE_TREE,
                Stage.IMPROVEMENT,
                EdgeKind.EXPLAINS,
                "改进计划必须对应证据化失败原因");
        addEdge(
                edges,
                ids,
                Stage.ABLATION,
                Stage.IMPROVEMENT,
                EdgeKind.VALIDATES,
                "消融提供Challenger必要性证据");
        addEdge(
                edges,
                ids,
                Stage.CODE_OWNER,
                Stage.IMPROVEMENT,
                EdgeKind.BLOCKS,
                "Owner冲突时禁止扩大生产链");
        addEdge(edges, ids, Stage.RUNTIME, Stage.FAILURE_TREE, EdgeKind.EXPLAINS, "先排运行读错再谈模型效果");
        addEdge(
                edges,
                ids,
                Stage.CALIBRATION,
                Stage.FAILURE_TREE,
                EdgeKind.EXPLAINS,
                "严格OOS证据进入失败归因");
        return edges;
    }

    private void addEdge(
            List<DependencyEdge> edges,
            Set<String> ids,
            Stage from,
            Stage to,
            EdgeKind kind,
            String rationale) {
        String a = findingId(from);
        String b = findingId(to);
        if (ids.contains(a) && ids.contains(b)) {
            edges.add(new DependencyEdge(a, b, kind, rationale));
        }
    }

    private BundleValidation validate(
            List<StageArtifact> stages,
            List<Finding> findings,
            List<DependencyEdge> edges,
            String canonical,
            String expectedHash) {
        ArrayList<String> violations = new ArrayList<String>();
        boolean fixedOrder = validateOrder(stages, violations);
        boolean unique = validateFindingIds(findings, violations);
        boolean evidence = validateEvidence(stages, findings, violations);
        boolean resolved = validateEdges(findings, edges, violations);
        boolean acyclic = validateAcyclic(findings, edges, violations);
        boolean stable = expectedHash.equals(V51ChecksumEngine.sha256(canonical));
        if (!stable) violations.add("DIAGNOSIS_HASH_UNSTABLE");
        boolean readOnly = MODE.indexOf("READ_ONLY") >= 0;
        if (!readOnly) violations.add("READ_ONLY_CONTRACT_MISSING");
        return new BundleValidation(
                fixedOrder, unique, evidence, resolved, acyclic, stable, readOnly, violations);
    }

    private boolean validateOrder(List<StageArtifact> stages, List<String> violations) {
        Stage[] expected = Stage.values();
        if (stages.size() != expected.length) {
            violations.add("STAGE_COUNT=" + stages.size() + "|expected=" + expected.length);
            return false;
        }
        boolean valid = true;
        for (int i = 0; i < expected.length; i++) {
            if (stages.get(i).stage != expected[i]) {
                violations.add(
                        "STAGE_ORDER_INDEX="
                                + i
                                + "|actual="
                                + stages.get(i).stage
                                + "|expected="
                                + expected[i]);
                valid = false;
            }
        }
        return valid;
    }

    private boolean validateFindingIds(List<Finding> findings, List<String> violations) {
        HashSet<String> ids = new HashSet<String>();
        boolean valid = true;
        for (int i = 0; i < findings.size(); i++) {
            Finding finding = findings.get(i);
            if (finding.id.length() == 0 || !ids.add(finding.id)) {
                violations.add("DUPLICATE_OR_EMPTY_FINDING=" + finding.id);
                valid = false;
            }
        }
        return valid;
    }

    private boolean validateEvidence(
            List<StageArtifact> stages, List<Finding> findings, List<String> violations) {
        HashSet<String> ids = new HashSet<String>();
        boolean valid = true;
        for (int i = 0; i < stages.size(); i++) {
            StageArtifact stage = stages.get(i);
            for (int j = 0; j < stage.evidence.size(); j++) {
                Evidence item = stage.evidence.get(j);
                if (item.id.length() == 0 || !ids.add(item.id)) {
                    violations.add("DUPLICATE_OR_EMPTY_EVIDENCE=" + item.id);
                    valid = false;
                }
            }
        }
        for (int i = 0; i < findings.size(); i++) {
            Finding finding = findings.get(i);
            for (int j = 0; j < finding.evidenceIds.size(); j++) {
                if (!ids.contains(finding.evidenceIds.get(j))) {
                    violations.add(
                            "UNRESOLVED_EVIDENCE="
                                    + finding.id
                                    + "->"
                                    + finding.evidenceIds.get(j));
                    valid = false;
                }
            }
        }
        return valid;
    }

    private boolean validateEdges(
            List<Finding> findings, List<DependencyEdge> edges, List<String> violations) {
        HashSet<String> ids = new HashSet<String>();
        for (int i = 0; i < findings.size(); i++) ids.add(findings.get(i).id);
        boolean valid = true;
        for (int i = 0; i < edges.size(); i++) {
            DependencyEdge edge = edges.get(i);
            if (!ids.contains(edge.fromFinding) || !ids.contains(edge.toFinding)) {
                violations.add("UNRESOLVED_EDGE=" + edge.fromFinding + "->" + edge.toFinding);
                valid = false;
            }
            if (edge.fromFinding.equals(edge.toFinding)) {
                violations.add("SELF_EDGE=" + edge.fromFinding);
                valid = false;
            }
        }
        return valid;
    }

    private boolean validateAcyclic(
            List<Finding> findings, List<DependencyEdge> edges, List<String> violations) {
        HashMap<String, List<String>> adjacency = new HashMap<String, List<String>>();
        for (int i = 0; i < findings.size(); i++) {
            adjacency.put(findings.get(i).id, new ArrayList<String>());
        }
        for (int i = 0; i < edges.size(); i++) {
            DependencyEdge edge = edges.get(i);
            List<String> next = adjacency.get(edge.fromFinding);
            if (next != null) next.add(edge.toFinding);
        }
        HashMap<String, Integer> color = new HashMap<String, Integer>();
        for (String id : adjacency.keySet()) color.put(id, Integer.valueOf(0));
        for (String id : adjacency.keySet()) {
            if (color.get(id).intValue() == 0 && hasCycle(id, adjacency, color)) {
                violations.add("FINDING_DAG_CYCLE_AT=" + id);
                return false;
            }
        }
        return true;
    }

    private boolean hasCycle(
            String id, Map<String, List<String>> adjacency, Map<String, Integer> color) {
        color.put(id, Integer.valueOf(1));
        List<String> next = adjacency.get(id);
        if (next != null) {
            for (int i = 0; i < next.size(); i++) {
                String target = next.get(i);
                Integer state = color.get(target);
                if (state == null) continue;
                if (state.intValue() == 1) return true;
                if (state.intValue() == 0 && hasCycle(target, adjacency, color)) return true;
            }
        }
        color.put(id, Integer.valueOf(2));
        return false;
    }

    private String canonicalBundle(
            long createdAt,
            String targetIssue,
            List<StageArtifact> stages,
            List<Finding> findings,
            List<DependencyEdge> edges,
            Status overall,
            String primary) {
        StringBuilder out = new StringBuilder(262144);
        out.append(VERSION)
                .append('|')
                .append(GAME_CODE)
                .append('|')
                .append(MODE)
                .append('|')
                .append(createdAt)
                .append('|')
                .append(escape(targetIssue))
                .append('|')
                .append(overall.name())
                .append('|')
                .append(escape(primary));
        for (int i = 0; i < stages.size(); i++) {
            out.append("\nS|").append(stages.get(i).canonical());
        }
        for (int i = 0; i < findings.size(); i++) {
            out.append("\nF|").append(findings.get(i).canonical());
        }
        for (int i = 0; i < edges.size(); i++) {
            out.append("\nD|").append(edges.get(i).canonical());
        }
        return out.toString();
    }

    public String render(DbStore store) {
        return render(diagnose(store));
    }

    public String render(DiagnosisBundle bundle) {
        StringBuilder out = new StringBuilder(262144);
        out.append("【B14统一研究诊断编排】\n").append(bundle.renderSummary()).append("\n\n【固定阶段证据】\n");
        for (int i = 0; i < bundle.stages.size(); i++) {
            StageArtifact stage = bundle.stages.get(i);
            out.append(i + 1)
                    .append('|')
                    .append(stage.stage.name())
                    .append('|')
                    .append(stage.stage.title)
                    .append("|status=")
                    .append(stage.status.name())
                    .append("|owner=")
                    .append(stage.owner)
                    .append("|hash=")
                    .append(stage.checksum)
                    .append('\n')
                    .append(stage.summary)
                    .append('\n')
                    .append(stage.detail)
                    .append("\n\n");
        }
        out.append("【Finding DAG】\n");
        for (int i = 0; i < bundle.findings.size(); i++) {
            Finding finding = bundle.findings.get(i);
            out.append(finding.id)
                    .append('|')
                    .append(finding.severity.name())
                    .append('|')
                    .append(finding.status.name())
                    .append("|confidence=")
                    .append(doubleText(finding.confidence))
                    .append("|owner=")
                    .append(finding.owner)
                    .append('\n')
                    .append("结论=")
                    .append(finding.conclusion)
                    .append('\n')
                    .append("原因=")
                    .append(finding.cause)
                    .append('\n')
                    .append("影响=")
                    .append(finding.impact)
                    .append('\n')
                    .append("施工=")
                    .append(finding.action)
                    .append('\n')
                    .append("验证=")
                    .append(finding.validation)
                    .append('\n')
                    .append("期望=")
                    .append(finding.expected)
                    .append('\n')
                    .append("回滚=")
                    .append(finding.rollback)
                    .append("\n\n");
        }
        out.append("【依赖边】\n");
        for (int i = 0; i < bundle.edges.size(); i++) {
            DependencyEdge edge = bundle.edges.get(i);
            out.append(edge.fromFinding)
                    .append(" -> ")
                    .append(edge.toFinding)
                    .append('|')
                    .append(edge.kind.name())
                    .append('|')
                    .append(edge.rationale)
                    .append('|')
                    .append(edge.checksum)
                    .append('\n');
        }
        out.append("\n【编排校验】\nvalid=")
                .append(bundle.validation.valid)
                .append("|fixedOrder=")
                .append(bundle.validation.fixedOrder)
                .append("|uniqueFindingIds=")
                .append(bundle.validation.uniqueFindingIds)
                .append("|evidenceResolved=")
                .append(bundle.validation.evidenceResolved)
                .append("|edgesResolved=")
                .append(bundle.validation.edgesResolved)
                .append("|acyclic=")
                .append(bundle.validation.acyclic)
                .append("|hashStable=")
                .append(bundle.validation.hashStable)
                .append("|readOnly=")
                .append(bundle.validation.readOnlyContract)
                .append("\nValidationHash=")
                .append(bundle.validation.checksum)
                .append("\nDiagnosisHash=")
                .append(bundle.diagnosisHash)
                .append('\n');
        return out.toString();
    }

    public String toJson(DiagnosisBundle bundle) {
        StringBuilder out = new StringBuilder(262144);
        out.append('{');
        jsonField(out, "version", bundle.version, true);
        jsonField(out, "gameCode", bundle.gameCode, true);
        jsonField(out, "mode", bundle.mode, true);
        jsonField(out, "targetIssue", bundle.targetIssue, true);
        jsonNumber(out, "createdAt", bundle.createdAt, true);
        jsonField(out, "overallStatus", bundle.overallStatus.name(), true);
        jsonField(out, "primaryFindingId", bundle.primaryFindingId, true);
        jsonField(out, "diagnosisHash", bundle.diagnosisHash, true);
        out.append("\"validation\":{");
        jsonBoolean(out, "valid", bundle.validation.valid, true);
        jsonBoolean(out, "fixedOrder", bundle.validation.fixedOrder, true);
        jsonBoolean(out, "uniqueFindingIds", bundle.validation.uniqueFindingIds, true);
        jsonBoolean(out, "evidenceResolved", bundle.validation.evidenceResolved, true);
        jsonBoolean(out, "edgesResolved", bundle.validation.edgesResolved, true);
        jsonBoolean(out, "acyclic", bundle.validation.acyclic, true);
        jsonBoolean(out, "hashStable", bundle.validation.hashStable, true);
        jsonBoolean(out, "readOnlyContract", bundle.validation.readOnlyContract, false);
        out.append("},\"stages\":[");
        for (int i = 0; i < bundle.stages.size(); i++) {
            if (i > 0) out.append(',');
            StageArtifact stage = bundle.stages.get(i);
            out.append('{');
            jsonField(out, "stage", stage.stage.name(), true);
            jsonNumber(out, "order", stage.stage.order, true);
            jsonField(out, "title", stage.stage.title, true);
            jsonField(out, "status", stage.status.name(), true);
            jsonField(out, "owner", stage.owner, true);
            jsonField(out, "source", stage.source, true);
            jsonField(out, "summary", stage.summary, true);
            jsonField(out, "detail", stage.detail, true);
            jsonField(out, "readError", stage.readError, true);
            jsonNumber(out, "collectedAt", stage.collectedAt, true);
            jsonField(out, "checksum", stage.checksum, true);
            out.append("\"metrics\":[");
            for (int j = 0; j < stage.metrics.size(); j++) {
                if (j > 0) out.append(',');
                Metric metric = stage.metrics.get(j);
                out.append('{');
                jsonField(out, "key", metric.key, true);
                jsonField(out, "value", metric.value, true);
                jsonField(out, "unit", metric.unit, true);
                jsonField(out, "source", metric.source, true);
                jsonBoolean(out, "available", metric.available, true);
                jsonField(out, "checksum", metric.checksum, false);
                out.append('}');
            }
            out.append("],\"evidence\":[");
            for (int j = 0; j < stage.evidence.size(); j++) {
                if (j > 0) out.append(',');
                Evidence evidence = stage.evidence.get(j);
                out.append('{');
                jsonField(out, "id", evidence.id, true);
                jsonField(out, "grade", evidence.grade.name(), true);
                jsonField(out, "source", evidence.source, true);
                jsonField(out, "observation", evidence.observation, true);
                jsonField(out, "counterEvidence", evidence.counterEvidence, true);
                jsonNumber(out, "sampleSize", evidence.sampleSize, true);
                jsonDouble(out, "lowerBound", evidence.lowerBound, true);
                jsonDouble(out, "upperBound", evidence.upperBound, true);
                jsonBoolean(out, "readSuccessful", evidence.readSuccessful, true);
                jsonField(out, "checksum", evidence.checksum, false);
                out.append('}');
            }
            out.append("]}");
        }
        out.append("],\"findings\":[");
        for (int i = 0; i < bundle.findings.size(); i++) {
            if (i > 0) out.append(',');
            Finding finding = bundle.findings.get(i);
            out.append('{');
            jsonField(out, "id", finding.id, true);
            jsonField(out, "stage", finding.stage.name(), true);
            jsonField(out, "severity", finding.severity.name(), true);
            jsonField(out, "status", finding.status.name(), true);
            jsonField(out, "category", finding.category, true);
            jsonField(out, "conclusion", finding.conclusion, true);
            jsonField(out, "cause", finding.cause, true);
            jsonField(out, "impact", finding.impact, true);
            jsonField(out, "owner", finding.owner, true);
            jsonField(out, "codeLocation", finding.codeLocation, true);
            jsonField(out, "action", finding.action, true);
            jsonField(out, "validation", finding.validation, true);
            jsonField(out, "expected", finding.expected, true);
            jsonField(out, "rollback", finding.rollback, true);
            jsonDouble(out, "confidence", finding.confidence, true);
            jsonField(out, "checksum", finding.checksum, true);
            out.append("\"evidenceIds\":[");
            for (int j = 0; j < finding.evidenceIds.size(); j++) {
                if (j > 0) out.append(',');
                jsonString(out, finding.evidenceIds.get(j));
            }
            out.append("]}");
        }
        out.append("],\"edges\":[");
        for (int i = 0; i < bundle.edges.size(); i++) {
            if (i > 0) out.append(',');
            DependencyEdge edge = bundle.edges.get(i);
            out.append('{');
            jsonField(out, "from", edge.fromFinding, true);
            jsonField(out, "to", edge.toFinding, true);
            jsonField(out, "kind", edge.kind.name(), true);
            jsonField(out, "rationale", edge.rationale, true);
            jsonField(out, "checksum", edge.checksum, false);
            out.append('}');
        }
        out.append("]}");
        return out.toString();
    }

    public ReplayResult replay(ReplayInput input) {
        DiagnosisBundle first = assemble(input.createdAt, input.targetIssue, input.stages);
        DiagnosisBundle second = assemble(input.createdAt, input.targetIssue, input.stages);
        return new ReplayResult(first, second);
    }

    public static ReplayInput deterministicFixture() {
        long at = 1786400000000L;
        ArrayList<StageArtifact> stages = new ArrayList<StageArtifact>();
        for (Stage stage : Stage.values()) {
            ArrayList<Metric> metrics = new ArrayList<Metric>();
            metrics.add(
                    new Metric(
                            "fixtureOrder",
                            String.valueOf(stage.order),
                            "index",
                            "HOST_REPLAY",
                            true));
            ArrayList<Evidence> evidence = new ArrayList<Evidence>();
            evidence.add(
                    new Evidence(
                            evidenceId(stage, "FIXTURE"),
                            EvidenceGrade.STATIC_BUILD,
                            "R36HostContractHarness",
                            "stage=" + stage.name() + "|order=" + stage.order,
                            "fixture only; no runtime effectiveness claim",
                            1,
                            -1.0d,
                            -1.0d,
                            true));
            stages.add(
                    artifact(
                            stage,
                            stage == Stage.CALIBRATION
                                            || stage == Stage.RANKER
                                            || stage == Stage.NUMBER_SURFACE
                                    ? Status.NOT_PROVEN
                                    : Status.PASS,
                            stageOwner(stage),
                            "HOST_REPLAY",
                            "deterministic fixture " + stage.name(),
                            "payload=" + stage.order,
                            "",
                            at,
                            metrics,
                            evidence));
        }
        return new ReplayInput(at, "202608100001", stages);
    }

    private static StageArtifact artifact(
            Stage stage,
            Status status,
            String owner,
            String source,
            String summary,
            String detail,
            String readError,
            long collectedAt,
            List<Metric> metrics,
            List<Evidence> evidence) {
        return new StageArtifact(
                stage,
                status,
                owner,
                source,
                summary,
                detail,
                readError,
                collectedAt,
                metrics,
                evidence);
    }

    private StageArtifact readError(
            Stage stage, DiagnosisContext context, String error, String source) {
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceId(stage, "READ_ERROR"),
                        EvidenceGrade.READ_ERROR,
                        source,
                        "READ_ERROR=" + safe(error),
                        "读取失败不得改写为0或PASS",
                        -1,
                        -1.0d,
                        -1.0d,
                        false));
        return artifact(
                stage,
                Status.READ_ERROR,
                stageOwner(stage),
                source,
                "READ_ERROR",
                "READ_ERROR=" + safe(error),
                error,
                context.createdAt,
                Collections.<Metric>emptyList(),
                evidence);
    }

    private static Metric metric(
            String key, String value, String unit, String source, boolean available) {
        return new Metric(key, value, unit, source, available);
    }

    private static List<Evidence> singletonEvidence(
            String id,
            EvidenceGrade grade,
            String source,
            String observation,
            String counterEvidence,
            int sample,
            boolean readSuccessful) {
        ArrayList<Evidence> out = new ArrayList<Evidence>();
        out.add(
                new Evidence(
                        id,
                        grade,
                        source,
                        observation,
                        counterEvidence,
                        sample,
                        -1.0d,
                        -1.0d,
                        readSuccessful));
        return out;
    }

    private static String findingId(Stage stage) {
        return String.format(Locale.US, "B14-F%03d-%s", Integer.valueOf(stage.order), stage.name());
    }

    private static String evidenceId(Stage stage, String suffix) {
        return "B14-E-" + stage.name() + '-' + safe(suffix).toUpperCase(Locale.US);
    }

    private static Severity severity(Status status) {
        if (status == Status.READ_ERROR || status == Status.FAIL) return Severity.P0;
        if (status == Status.NOT_PROVEN) return Severity.P1;
        if (status == Status.WARN || status == Status.RETEST_REQUIRED) return Severity.P2;
        if (status == Status.OBSERVED) return Severity.P3;
        return Severity.INFO;
    }

    private static double confidenceFor(StageArtifact stage) {
        if (stage.status == Status.READ_ERROR) return 1.0d;
        if (stage.status == Status.FAIL) return 0.95d;
        if (stage.status == Status.NOT_PROVEN) return 1.0d;
        if (stage.status == Status.RETEST_REQUIRED) return 0.8d;
        if (stage.status == Status.PASS) return 0.9d;
        return 0.65d;
    }

    private static String causeFor(StageArtifact stage) {
        if(stage.stage==Stage.RUNTIME&&stage.status==Status.FAIL) {
            String failures=runtimeFailureItemsR76(stage);
            StringBuilder observed=new StringBuilder();
            for(String key:new String[]{"serviceAlive","businessAlive","rootCause","latestAttemptFailed","requestToCommitP95","maxConcurrency","oomCount"}) {
                if(observed.length()>0)observed.append("，");
                observed.append(key).append('=').append(runtimeValueR76(stage,key));
            }
            return "运行失败项："+(failures.isEmpty()?"失败指标缺失或不可用，保留FAIL并等待原始证据":failures)
                    +"；观察值："+observed;
        }
        switch (stage.status) {
            case READ_ERROR:
                return "typed read failed: " + stage.readError;
            case FAIL:
                return "observed invariant or runtime gate failed";
            case NOT_PROVEN:
                return "frozen model artifact and strict OOS certificate are absent or unproven";
            case RETEST_REQUIRED:
                return "static integration exists but sufficient physical runtime evidence is"
                           + " absent";
            case WARN:
                return "evidence is incomplete or internally uncertain";
            case OBSERVED:
                return "evidence exists but does not independently prove promotion fitness";
            case PASS:
            default:
                return "contract and available evidence are consistent";
        }
    }

    private static String impactFor(StageArtifact stage) {
        switch (stage.stage) {
            case DATA:
                return "wrong target identity would contaminate every downstream label and"
                           + " settlement";
            case RUNTIME:
                return runtimeImpactR76(stage);
            case LEDGER:
                return "mutable or missing canonical rows invalidate OOS and first-write-wins"
                           + " proof";
            case NUMBER_SURFACE:
                return "unproven number probabilities cannot support REAL authorization";
            case CROSS_POSITION:
                return "leakage or spurious relation can inflate offline performance";
            case K3_FUNNEL:
                return "non-nested stages can silently replace fixed K3 semantics";
            case ACTION_SPACE:
                return "missing identities invalidate ranking, deferred proof and exact settlement";
            case RANKER:
                return "unproven ordering invalidates champion and selected-action claims";
            case CALIBRATION:
                return "unknown selected probability prevents safe coverage/threshold decisions";
            case FREQUENCY:
                return "frequency debt or auto-unlock could create unauthorized REAL actions";
            case FAILURE_TREE:
                return "wrong root cause wastes repairs and can weaken redlines";
            case ABLATION:
                return "correlated modules may be double-counted without counterfactual tests";
            case CODE_OWNER:
                return "duplicate owners create double writes and inconsistent decisions";
            case IMPROVEMENT:
            default:
                return "unvalidated changes can regress the frozen active chain";
        }
    }

    /** Read-only metric uses the exact existing latest-attempt predicate in both collectors. */
    private static Metric latestAttemptMetricR76(ModelEffectivenessAnalyzerR35.RuntimeSummary runtime) {
        boolean failed="ANALYSIS_ATTEMPT_FAILED".equals(runtime.lastAttemptType)&&runtime.lastAttemptAt>=runtime.lastAt;
        return metric("latestAttemptFailed",failed?"1":"0","boolean",
                "RuntimeHealthLedgerR36:lastAttemptType,lastAttemptAt,lastAt","OK".equals(runtime.readStatus));
    }

    private static String runtimeValueR76(StageArtifact stage,String key) {
        for(Metric metric:stage.metrics)if(key.equals(metric.key)&&metric.available&&!metric.value.isEmpty())return metric.value;
        return "未知";
    }

    private static boolean runtimeAboveR76(StageArtifact stage,String key,long boundary) {
        try{return Long.parseLong(runtimeValueR76(stage,key))>boundary;}
        catch(NumberFormatException unavailable){return false;}
    }

    private static boolean runtimeBusinessFailedR76(StageArtifact stage) {
        String root=runtimeValueR76(stage,"rootCause");
        return "0".equals(runtimeValueR76(stage,"businessAlive"))||"RUNTIME_CHAIN".equals(root)||"SETTLEMENT_OOS".equals(root)||"PREDRAW_COVERAGE_MISSING".equals(root);
    }

    private static void appendRuntimeItemR76(StringBuilder out,String item) {
        if(out.length()>0)out.append("；");out.append(item);
    }

    private static String runtimeFailureItemsR76(StageArtifact stage) {
        StringBuilder out=new StringBuilder();String root=runtimeValueR76(stage,"rootCause");
        if("0".equals(runtimeValueR76(stage,"businessAlive")))appendRuntimeItemR76(out,"businessAlive=0");
        if("RUNTIME_CHAIN".equals(root)||"SETTLEMENT_OOS".equals(root)||"PREDRAW_COVERAGE_MISSING".equals(root))appendRuntimeItemR76(out,"rootCause="+root);
        if("1".equals(runtimeValueR76(stage,"latestAttemptFailed")))appendRuntimeItemR76(out,"latestAttemptFailed=1");
        if(runtimeAboveR76(stage,"requestToCommitP95",5000L))appendRuntimeItemR76(out,"requestToCommitP95="+runtimeValueR76(stage,"requestToCommitP95")+"ms >5000ms");
        if(runtimeAboveR76(stage,"maxConcurrency",1L))appendRuntimeItemR76(out,"maxConcurrency="+runtimeValueR76(stage,"maxConcurrency")+" >1");
        if(runtimeAboveR76(stage,"oomCount",0L))appendRuntimeItemR76(out,"oomCount="+runtimeValueR76(stage,"oomCount")+" >0");
        return out.toString();
    }

    private static String runtimeImpactR76(StageArtifact stage) {
        if(stage.status==Status.READ_ERROR)return "运行数据读取失败，无法从本次诊断确认服务与业务状态。";
        if(stage.status==Status.PASS)return "本次可用指标满足既有运行检查；持续运行能力仍以设备记录为准。";
        if(stage.status!=Status.FAIL)return "运行证据尚未完成验收，尚不能据此定位服务或业务故障。";
        boolean businessFailed=runtimeBusinessFailedR76(stage);
        String prefix=businessFailed?"已观察到业务链异常；":
                "1".equals(runtimeValueR76(stage,"serviceAlive"))&&"1".equals(runtimeValueR76(stage,"businessAlive"))
                ?"当前服务与业务存活；":"服务与业务是否存活需结合可用指标核对；";
        StringBuilder impact=new StringBuilder();
        if(businessFailed)appendRuntimeItemR76(impact,"抓取、分析、结算或证据闭环可能未及时推进");
        if("1".equals(runtimeValueR76(stage,"latestAttemptFailed")))appendRuntimeItemR76(impact,"最近分析尝试失败，需核对本期正式产物");
        if(runtimeAboveR76(stage,"requestToCommitP95",5000L))appendRuntimeItemR76(impact,"请求到提交时延超出5000ms要求，压缩本期处理窗口");
        if(runtimeAboveR76(stage,"maxConcurrency",1L))appendRuntimeItemR76(impact,"分析并发数超过1，存在执行争用风险");
        if(runtimeAboveR76(stage,"oomCount",0L))appendRuntimeItemR76(impact,"记录到内存溢出，相关任务可能中断");
        if(impact.length()==0)impact.append("FAIL对应的失败指标缺失或不可用，尚不能定位具体影响");
        return prefix+impact+"。";
    }

    private static String actionFor(StageArtifact stage) {
        switch (stage.stage) {
            case DATA:
                return "use TargetIssueResolver only; reject invalid base/target pairs before"
                           + " enqueue";
            case RUNTIME:
                return "repair typed database/framework path first; then rerun screen-on/off, kill"
                           + " and soak matrix";
            case LEDGER:
                return "preserve atomic canonical write and export raw rows on invariant failure";
            case NUMBER_SURFACE:
                return "train offline, freeze artifact/hash and replay PIT number probabilities";
            case CROSS_POSITION:
                return "verify pre-draw timestamp boundary and cross-block certificate";
            case K3_FUNNEL:
                return "persist exact nested 9/8/7/6/5/4/3 trace without deleting 1200 actions";
            case ACTION_SPACE:
                return "enumerate all 10*C(10,3) identities and validate ordinals 0..1199";
            case RANKER:
                return "freeze groupwise rank artifact; validate MRR/NDCG/pairwise stability OOS";
            case CALIBRATION:
                return "write labels only after draw from frozen canonical rows; compute"
                           + " Wilson/ECE/Brier";
            case FREQUENCY:
                return "keep SHADOW_ONLY and zero frequency debt until every hard certificate"
                           + " passes";
            case FAILURE_TREE:
                return "test P0 read/runtime causes before model hypotheses";
            case ABLATION:
                return "run leave-one-module-out and conflict replay offline";
            case CODE_OWNER:
                return "retain one runtime, issue, gate, settlement, OOS and deferred owner";
            case IMPROVEMENT:
            default:
                return "implement challenger behind shadow gate; promote only with immutable"
                           + " certificate";
        }
    }

    private static String validationFor(StageArtifact stage) {
        switch (stage.stage) {
            case DATA:
                return "T05 rollover, invalid sequence and exact distance";
            case RUNTIME:
                return "T01-T04 single-flight plus T29-T34 physical runtime matrix";
            case LEDGER:
                return "B12 invariant audit, first-write replay and exact settlement idempotency";
            case NUMBER_SURFACE:
                return "PIT replay, row sums, Sinkhorn parity and artifact hash";
            case CROSS_POSITION:
                return "timestamp leakage audit and cross-block holdout";
            case K3_FUNNEL:
                return "seven exact stages, nested membership and final length three";
            case ACTION_SPACE:
                return "1200 unique identities, combination ordinal boundaries and payload hash";
            case RANKER:
                return "same-issue grouping, deterministic ties and frozen rank certificate";
            case CALIBRATION:
                return "T20/T21 coverage and T23 injected read error; min OOS sample 120";
            case FREQUENCY:
                return "static forbidden-token gate and zero observed REAL BET rows";
            case FAILURE_TREE:
                return "finding counter-evidence and root-cause priority replay";
            case ABLATION:
                return "blockwise ablation deltas with uncertainty intervals";
            case CODE_OWNER:
                return "owner matrix and active-path instantiation scan";
            case IMPROVEMENT:
            default:
                return "champion/challenger rollback rehearsal and deterministic bundle hash";
        }
    }

    private static String expectedFor(StageArtifact stage) {
        if (stage.status == Status.READ_ERROR)
            return "explicit READ_ERROR/P0; never numeric fallback";
        if (stage.status == Status.NOT_PROVEN) return "SHADOW_ONLY; REAL remains locked";
        if (stage.status == Status.RETEST_REQUIRED)
            return "engineering remains implemented; physical proof stays open";
        if (stage.status == Status.FAIL) return "gate fails closed and raw evidence is retained";
        return "stable deterministic evidence with no redline violation";
    }

    private static String rollbackFor(StageArtifact stage) {
        if (stage.stage == Stage.IMPROVEMENT)
            return "retain current champion and remove challenger from active registry";
        if (stage.stage == Stage.RUNTIME)
            return "keep SHADOW_ONLY and export DB/logcat before further changes";
        return "keep canonical rows immutable, disable new path and remain SHADOW_ONLY";
    }

    private static String codeLocationFor(Stage stage) {
        switch (stage) {
            case DATA:
                return "TargetIssueResolver.java";
            case RUNTIME:
                return "AutoFetchService.java|RuntimeHealthLedgerR36.java";
            case LEDGER:
                return "V51Repository.java|CanonicalObservationWriterR36.java";
            case NUMBER_SURFACE:
                return "V51NumberModelArtifact.java|V51SinkhornEngine.java";
            case CROSS_POSITION:
                return "V51RelationGraphVerifier.java|V51PermutationFeatureEngine.java";
            case K3_FUNNEL:
                return "K3FunnelOrchestrator.java";
            case ACTION_SPACE:
                return "K3ActionSpaceEngine.java|V51PositionActionBuilder.java";
            case RANKER:
                return "V51ActionRankerEngine.java|V51ActionRankerArtifact.java";
            case CALIBRATION:
                return "OosAfterDrawWriterR36.java|ModelEffectivenessAnalyzerR35.java";
            case FREQUENCY:
                return "DecisionGateEngine.java|V51BuildManifest.java";
            case FAILURE_TREE:
                return "V57FailureDiagnosisEngine.java|K3FailureDiagnosisEngine.java";
            case ABLATION:
                return "StrategyConflictAblationEngine.java|FormulaAlgorithmAuditEngine.java";
            case CODE_OWNER:
                return "CodeOwnershipCompletenessAuditEngine.java";
            case IMPROVEMENT:
            default:
                return "ImprovementChallengerPlanEngine.java";
        }
    }

    private static String stageOwner(Stage stage) {
        switch (stage) {
            case DATA:
                return "TargetIssueResolver_ONLY";
            case RUNTIME:
                return "AutoFetchService->BusinessReconcilerR36->V51RuntimeOrchestrator";
            case LEDGER:
                return "V51Repository";
            case NUMBER_SURFACE:
                return "V51NumberModelArtifact";
            case CROSS_POSITION:
                return "V51RelationGraphVerifier";
            case K3_FUNNEL:
                return "K3FunnelOrchestrator_ONLY";
            case ACTION_SPACE:
                return "K3ActionSpaceEngine";
            case RANKER:
                return "V51ActionRankerEngine";
            case CALIBRATION:
                return "OosAfterDrawWriterR36";
            case FREQUENCY:
                return "DecisionGateEngine_ONLY";
            case FAILURE_TREE:
                return "V57FailureDiagnosisEngine";
            case ABLATION:
                return "StrategyConflictAblationEngine";
            case CODE_OWNER:
                return "CodeOwnershipCompletenessAuditEngine";
            case IMPROVEMENT:
            default:
                return "ImprovementChallengerPlanEngine";
        }
    }

    private static Status overallStatus(List<StageArtifact> stages) {
        Status worst = Status.PASS;
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).status.risk > worst.risk) worst = stages.get(i).status;
        }
        return worst;
    }

    private static String primaryFinding(List<Finding> findings) {
        Finding best = null;
        for (int i = 0; i < findings.size(); i++) {
            Finding current = findings.get(i);
            if (best == null
                    || current.severity.priority > best.severity.priority
                    || (current.severity.priority == best.severity.priority
                            && current.confidence > best.confidence)
                    || (current.severity.priority == best.severity.priority
                            && current.confidence == best.confidence
                            && current.stage.order < best.stage.order)) {
                best = current;
            }
        }
        return best == null ? "NONE" : best.id;
    }

    private static void sortFindings(List<Finding> findings) {
        Collections.sort(
                findings,
                new Comparator<Finding>() {
                    public int compare(Finding left, Finding right) {
                        int severity = right.severity.priority - left.severity.priority;
                        if (severity != 0) return severity;
                        int confidence = Double.compare(right.confidence, left.confidence);
                        if (confidence != 0) return confidence;
                        return left.stage.order - right.stage.order;
                    }
                });
    }

    private static QueryCount queryCount(DbStore store, String sql, String[] args) {
        if (store == null) return QueryCount.error("STORE_NULL");
        if (args == null || args.length == 0 || safe(args[0]).length() == 0) {
            return new QueryCount(true, 0, "NO_TARGET");
        }
        Cursor cursor = null;
        try {
            SQLiteDatabase db = store.readable();
            cursor = db.rawQuery(sql, args);
            return new QueryCount(true, cursor.moveToFirst() ? cursor.getInt(0) : 0, "");
        } catch (Throwable error) {
            return QueryCount.error(errorText(error));
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static final class QueryCount {
        final boolean ok;
        final int value;
        final String error;

        QueryCount(boolean ok, int value, String error) {
            this.ok = ok;
            this.value = value;
            this.error = safe(error);
        }

        static QueryCount error(String error) {
            return new QueryCount(false, -1, error);
        }

        String render() {
            return ok ? String.valueOf(value) : "READ_ERROR:" + error;
        }
    }

    private static String state(DbStore store, String key, String fallback) {
        if (store == null) return safe(fallback);
        try {
            String value = store.getState(key, fallback);
            return value == null ? safe(fallback) : value;
        } catch (Throwable error) {
            return "READ_ERROR:" + errorText(error);
        }
    }

    private static boolean containsIgnoreCase(String value, String token) {
        return safe(value).toUpperCase(Locale.US).indexOf(safe(token).toUpperCase(Locale.US)) >= 0;
    }

    private static String joinInts(int[] values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) out.append(',');
                out.append(values[i]);
            }
        }
        return out.toString();
    }

    private static String errorText(Throwable error) {
        if (error == null) return "UNKNOWN";
        String message = error.getMessage();
        String out = error.getClass().getSimpleName();
        if (message != null && message.trim().length() > 0) out += ':' + message.trim();
        return limit(out, 1000);
    }

    private static String limit(String value, int maximum) {
        String text = safe(value);
        return text.length() <= maximum ? text : text.substring(0, maximum);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String escape(String value) {
        String text = value == null ? "" : value;
        return text.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0d;
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private static String doubleText(double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0.0d ? "Infinity" : "-Infinity";
        return Long.toHexString(Double.doubleToLongBits(value));
    }

    private static List<String> immutableStrings(List<String> values) {
        ArrayList<String> out = new ArrayList<String>();
        if (values != null) {
            for (int i = 0; i < values.size(); i++) out.add(safe(values.get(i)));
        }
        return Collections.unmodifiableList(out);
    }

    private static List<Metric> immutableMetrics(List<Metric> values) {
        return Collections.unmodifiableList(
                new ArrayList<Metric>(values == null ? Collections.<Metric>emptyList() : values));
    }

    private static List<Evidence> immutableEvidence(List<Evidence> values) {
        return Collections.unmodifiableList(
                new ArrayList<Evidence>(
                        values == null ? Collections.<Evidence>emptyList() : values));
    }

    private static List<StageArtifact> immutableStages(List<StageArtifact> values) {
        return Collections.unmodifiableList(
                new ArrayList<StageArtifact>(
                        values == null ? Collections.<StageArtifact>emptyList() : values));
    }

    private static List<Finding> immutableFindings(List<Finding> values) {
        return Collections.unmodifiableList(
                new ArrayList<Finding>(values == null ? Collections.<Finding>emptyList() : values));
    }

    private static List<DependencyEdge> immutableEdges(List<DependencyEdge> values) {
        return Collections.unmodifiableList(
                new ArrayList<DependencyEdge>(
                        values == null ? Collections.<DependencyEdge>emptyList() : values));
    }

    private static void jsonField(StringBuilder out, String key, String value, boolean comma) {
        jsonString(out, key);
        out.append(':');
        jsonString(out, value);
        if (comma) out.append(',');
    }

    private static void jsonNumber(StringBuilder out, String key, long value, boolean comma) {
        jsonString(out, key);
        out.append(':').append(value);
        if (comma) out.append(',');
    }

    private static void jsonDouble(StringBuilder out, String key, double value, boolean comma) {
        jsonString(out, key);
        out.append(':');
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            out.append("null");
        } else {
            out.append(Double.toString(value));
        }
        if (comma) out.append(',');
    }

    private static void jsonBoolean(StringBuilder out, String key, boolean value, boolean comma) {
        jsonString(out, key);
        out.append(':').append(value ? "true" : "false");
        if (comma) out.append(',');
    }

    private static void jsonString(StringBuilder out, String value) {
        out.append('"');
        String text = value == null ? "" : value;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c < 0x20) {
                String hex = Integer.toHexString(c);
                out.append("\\u");
                for (int pad = hex.length(); pad < 4; pad++) out.append('0');
                out.append(hex);
            } else {
                out.append(c);
            }
        }
        out.append('"');
    }

public StageArtifact collectImprovementSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        String plan = new ImprovementChallengerPlanEngine().renderSnapshotCP4(report, store);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metricSnapshotCP4(report, 
                        "planChars",
                        String.valueOf(plan.length()),
                        "char",
                        "ImprovementChallengerPlanEngine",
                        true));
        metrics.add(metricSnapshotCP4(report, "productionMutation", "0", "count", MODE, true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.IMPROVEMENT, "PLAN"),
                        EvidenceGrade.CONTRACT,
                        "ImprovementChallengerPlanEngine",
                        com.ethk3.novaonepro.ReportTextCP12.sha256(report, plan),
                        "Challenger只有冻结回放证书通过后才能晋级，不可自动解锁REAL",
                        plan.length(),
                        -1.0d,
                        -1.0d,
                        true));
        return artifactSnapshotCP4(report, 
                Stage.IMPROVEMENT,
                Status.OBSERVED,
                "ImprovementChallengerPlanEngine",
                "read-only change plan",
                "可执行Challenger计划已生成，生产参数未修改",
                plan,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

public static StageArtifact artifactSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            Stage stage,
            Status status,
            String owner,
            String source,
            String summary,
            String detail,
            String readError,
            long collectedAt,
            List<Metric> metrics,
            List<Evidence> evidence) {
        return new StageArtifact(
                stage,
                status,
                owner,
                source,
                summary,
                detail,
                readError,
                collectedAt,
                metrics,
                evidence);
    }

public static String evidenceIdSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Stage stage, String suffix) {
        return "B14-E-" + stage.name() + '-' + safeSnapshotCP4(report, suffix).toUpperCase(Locale.US);
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value) {
        return value == null ? "" : value.trim();
    }

public static Metric metricSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            String key, String value, String unit, String source, boolean available) {
        return new Metric(key, value, unit, source, available);
    }

public StageArtifact collectCodeOwnerSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        CodeOwnershipCompletenessAuditEngine ownerEngine =
                new CodeOwnershipCompletenessAuditEngine();
        CodeOwnershipCompletenessAuditEngine.Report ownerReport = ownerEngine.auditSnapshotCP4(report, store);
        String audit = ownerEngine.renderSnapshotCP4(report, store);
        boolean ownerConflict = false;
        if (ownerReport != null && ownerReport.findings != null) {
            for (int i = 0; i < ownerReport.findings.length; i++) {
                CodeOwnershipCompletenessAuditEngine.Finding finding =
                        ownerReport.findings[i];
                if (finding == null || finding.rule == null) continue;
                if ("OWNER_CONFLICT".equals(finding.rule.field)
                        && finding.level
                                == CodeOwnershipCompletenessAuditEngine.Level.HARD_BLOCK) {
                    ownerConflict = true;
                    break;
                }
            }
        }
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metricSnapshotCP4(report, 
                        "auditChars",
                        String.valueOf(audit.length()),
                        "char",
                        "CodeOwnershipCompletenessAuditEngine",
                        true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "ownerConflict",
                        String.valueOf(ownerConflict),
                        "boolean",
                        "owner audit",
                        true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.CODE_OWNER, "OWNER"),
                        EvidenceGrade.STATIC_BUILD,
                        "CodeOwnershipCompletenessAuditEngine",
                        com.ethk3.novaonepro.ReportTextCP12.sha256(report, audit),
                        "唯一Owner约束必须同时由静态门与运行账本验证",
                        audit.length(),
                        -1.0d,
                        -1.0d,
                        true));
        return artifactSnapshotCP4(report, 
                Stage.CODE_OWNER,
                ownerReport==null?Status.READ_ERROR:
                    "HARD_BLOCK".equals(ownerReport.verdict)?Status.FAIL:
                    "PASS".equals(ownerReport.verdict)?Status.PASS:Status.WARN,
                "CodeOwnershipCompletenessAuditEngine",
                "owner matrix",
                "完整Owner审核="+(ownerReport==null?"READ_ERROR":ownerReport.verdict)+"|explicitConflict="+ownerConflict,
                audit,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

public StageArtifact collectAblationSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        String conflict = new StrategyConflictAblationEngine().renderSnapshotCP4(report, store);
        String formula = new FormulaAlgorithmAuditEngine().renderSnapshotCP4(report, store);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metricSnapshotCP4(report, 
                        "conflictChars",
                        String.valueOf(conflict.length()),
                        "char",
                        "StrategyConflictAblationEngine",
                        true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "formulaChars",
                        String.valueOf(formula.length()),
                        "char",
                        "FormulaAlgorithmAuditEngine",
                        true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.ABLATION, "CONFLICT"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "StrategyConflictAblationEngine",
                        com.ethk3.novaonepro.ReportTextCP12.sha256(report, conflict),
                        "消融建议必须离线验证，不能在报告线程改权重",
                        conflict.length(),
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.ABLATION, "FORMULA"),
                        EvidenceGrade.STATIC_BUILD,
                        "FormulaAlgorithmAuditEngine",
                        com.ethk3.novaonepro.ReportTextCP12.sha256(report, formula),
                        "公式存在不等于效果已证明",
                        formula.length(),
                        -1.0d,
                        -1.0d,
                        true));
        return artifactSnapshotCP4(report, 
                Stage.ABLATION,
                Status.OBSERVED,
                "StrategyConflictAblationEngine+FormulaAlgorithmAuditEngine",
                "read-only ablation evidence",
                "冲突、消融、公式审计已生成",
                conflict + "\n\n" + formula,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

public StageArtifact collectFailureTreeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        V57FailureDiagnosisEngine failureEngine = new V57FailureDiagnosisEngine();
        K3FailureDiagnosisEngine k3Engine = new K3FailureDiagnosisEngine();
        K3FailureDiagnosisEngine.Report k3 = k3Engine.diagnoseSnapshotCP4(report, store);
        String detail = failureEngine.renderSnapshotCP4(report, store) + "\n\n" + k3Engine.renderSnapshotCP4(report, store);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metricSnapshotCP4(report, 
                        "k3PrimaryLayer",
                        k3.primaryLayer,
                        "category",
                        "K3FailureDiagnosisEngine",
                        true));
        metrics.add(metricSnapshotCP4(report, "k3Verdict", k3.verdict, "status", "K3FailureDiagnosisEngine", true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "k3LayerCount",
                        String.valueOf(k3.layers.length),
                        "count",
                        "K3FailureDiagnosisEngine",
                        true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.FAILURE_TREE, "B14"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "V57FailureDiagnosisEngine+K3FailureDiagnosisEngine",
                        "primary="
                                + k3.primaryLayer
                                + "|verdict="
                                + k3.verdict
                                + "|hash="
                                + k3.checksum,
                        "失败归因是诊断假设，不得直接改动生产参数",
                        k3.layers.length,
                        -1.0d,
                        -1.0d,
                        true));
        Status status =
                "BLOCKED".equals(k3.verdict)
                        ? Status.NOT_PROVEN
                        : ("HEALTHY".equals(k3.verdict) ? Status.OBSERVED : Status.WARN);
        return artifactSnapshotCP4(report, 
                Stage.FAILURE_TREE,
                status,
                "V57FailureDiagnosisEngine+K3FailureDiagnosisEngine",
                "immutable evidence diagnosis",
                k3.primaryLayer + '|' + k3.verdict,
                detail,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

public StageArtifact collectFrequencySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        boolean locked =
                !AppConfig.R36_REAL_BET_UNLOCK
                        && V51BuildManifest.SHADOW_ONLY
                        && V51BuildManifest.REAL_BET_HARD_DISABLED;
        ControlledRealContractAuditR922.Report realAudit=ControlledRealContractAuditR922.captureSnapshotCP4(report,store);
        Status safetyStatus=!"OK".equals(realAudit.readStatus)?Status.READ_ERROR:
                !realAudit.passed?Status.FAIL:
                realAudit.total==0?(locked?Status.PASS:Status.NOT_PROVEN):Status.OBSERVED;
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metricSnapshotCP4(report, 
                        "realUnlock",
                        String.valueOf(AppConfig.R36_REAL_BET_UNLOCK),
                        "boolean",
                        "AppConfig",
                        true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "shadowOnly",
                        String.valueOf(V51BuildManifest.SHADOW_ONLY),
                        "boolean",
                        "V51BuildManifest",
                        true));
        metrics.add(metricSnapshotCP4(report, "requiredRealBet", "0", "count", "unproven model policy", true));
        metrics.add(metricSnapshotCP4(report, "frequencyDebt", "0", "count", "forbidden policy", true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.FREQUENCY, "REDLINE"),
                        EvidenceGrade.CONTRACT,
                        "DecisionGateEngine+V51BuildManifest",
                        "SHADOW_ONLY="
                                + V51BuildManifest.SHADOW_ONLY
                                + "|REAL_HARD_DISABLED="
                                + V51BuildManifest.REAL_BET_HARD_DISABLED
                                + "|UNLOCK="
                                + AppConfig.R36_REAL_BET_UNLOCK,
                        "频率、欠债、追单、倍投不得绕开模型与实时证明",
                        1,
                        -1.0d,
                        -1.0d,
                        true));
        return artifactSnapshotCP4(report, 
                Stage.FREQUENCY,
                safetyStatus,
                "DecisionGateEngine_ONLY",
                "compile-time and runtime redlines",
                "实际REAL合同审核="+realAudit.status+"|claimed="+realAudit.total,
                realAudit.summary()+"|modelQuality=SEPARATE_FUTURE_EVALUATION|frequencyDebt=FORBIDDEN_POLICY",
                "",
                context.createdAt,
                metrics,
                evidence);
    }

public StageArtifact collectCalibrationSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        ModelEffectivenessAnalyzerR35.Metrics m = context.model.metrics;
        ModelEffectivenessAnalyzerR35.Coverage c = context.model.coverage;
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metricSnapshotCP4(report, 
                        "strictOosN",
                        String.valueOf(m.strictOosSample),
                        "sample",
                        "strict OOS ledger",
                        "OK".equals(m.readStatus)));
        metrics.add(
                metricSnapshotCP4(report, 
                        "hitCount",
                        String.valueOf(m.hitCount),
                        "count",
                        "strict OOS ledger",
                        "OK".equals(m.readStatus)));
        metrics.add(
                metricSnapshotCP4(report, 
                        "wilsonLower95",
                        doubleTextSnapshotCP4(report, m.wilsonLower95),
                        "ratio",
                        "Wilson score",
                        m.strictOosSample > 0));
        metrics.add(
                metricSnapshotCP4(report, "ECE", doubleTextSnapshotCP4(report, m.ece), "ratio", "selected calibration", m.ece >= 0.0d));
        metrics.add(
                metricSnapshotCP4(report, 
                        "Brier",
                        doubleTextSnapshotCP4(report, m.brier),
                        "score",
                        "selected calibration",
                        m.brier >= 0.0d));
        metrics.add(
                metricSnapshotCP4(report, 
                        "coverage",
                        c.covered + "/" + c.eligible,
                        "targets",
                        "expected target enumeration",
                        "OK".equals(c.readStatus)));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.CALIBRATION, "OOS"),
                        "READ_ERROR".equals(m.readStatus)
                                ? EvidenceGrade.READ_ERROR
                                : EvidenceGrade.STRICT_OOS,
                        "ModelEffectivenessAnalyzerR35.metrics",
                        "read="
                                + m.readStatus
                                + "|N="
                                + m.strictOosSample
                                + "|hit="
                                + m.hitCount
                                + "|wilson="
                                + m.wilsonLower95
                                + "|ECE="
                                + m.ece
                                + "|Brier="
                                + m.brier,
                        "小样本或读取失败不得渲染为命中率证明",
                        m.strictOosSample,
                        m.wilsonLower95,
                        1.0d,
                        !"READ_ERROR".equals(m.readStatus)));
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.CALIBRATION, "COVERAGE"),
                        "READ_ERROR".equals(c.readStatus)
                                ? EvidenceGrade.READ_ERROR
                                : EvidenceGrade.DATABASE_OBSERVATION,
                        "ModelEffectivenessAnalyzerR35.coverage",
                        "read="
                                + c.readStatus
                                + "|covered="
                                + c.covered
                                + "|eligible="
                                + c.eligible
                                + "|missing="
                                + c.missingCsv(),
                        "覆盖率分母必须来自可枚举目标期，不能用已有表行数",
                        c.eligible,
                        -1.0d,
                        -1.0d,
                        !"READ_ERROR".equals(c.readStatus)));
        Status status;
        String readError = "";
        if ("READ_ERROR".equals(m.readStatus) || "READ_ERROR".equals(c.readStatus)) {
            status = Status.READ_ERROR;
            readError = safeSnapshotCP4(report, m.error) + (safeSnapshotCP4(report, c.error).length() > 0 ? "|" + safeSnapshotCP4(report, c.error) : "");
        } else if (!V51SelectedCalibrationArtifact.PROVEN) {
            status = Status.NOT_PROVEN;
        } else if (m.strictOosSample < AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE) {
            status = Status.RETEST_REQUIRED;
        } else {
            status = Status.OBSERVED;
        }
        return artifactSnapshotCP4(report, 
                Stage.CALIBRATION,
                status,
                "OosAfterDrawWriterR36+ModelEffectivenessAnalyzerR35",
                "frozen canonical + exact draw",
                "read=" + m.readStatus + "|model=" + context.model.modelStatus,
                context.model.render(),
                readError,
                context.createdAt,
                metrics,
                evidence);
    }

public static String doubleTextSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, double value) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0.0d ? "Infinity" : "-Infinity";
        return Long.toHexString(Double.doubleToLongBits(value));
    }

public StageArtifact collectRankerSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        boolean proven =
                V51ActionRankerArtifact.PROVEN && V51ActionRankerArtifact.RANK_ORDER_PASSED;
        String summary = stateSnapshotCP4(report, store, "v51_ranker_summary", "NO_RUNTIME_SAMPLE");
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metricSnapshotCP4(report, 
                        "artifactProven",
                        String.valueOf(V51ActionRankerArtifact.PROVEN),
                        "boolean",
                        "V51ActionRankerArtifact",
                        true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "rankOrderPassed",
                        String.valueOf(V51ActionRankerArtifact.RANK_ORDER_PASSED),
                        "boolean",
                        "V51ActionRankerArtifact",
                        true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "runtimeSummary",
                        summary,
                        "text",
                        "app_state",
                        !"NO_RUNTIME_SAMPLE".equals(summary)));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.RANKER, "ARTIFACT"),
                        proven ? EvidenceGrade.STRICT_OOS : EvidenceGrade.STATIC_BUILD,
                        "V51ActionRankerArtifact",
                        "PROVEN="
                                + V51ActionRankerArtifact.PROVEN
                                + "|RANK_ORDER="
                                + V51ActionRankerArtifact.RANK_ORDER_PASSED,
                        "工程可以枚举和排序，但不能伪造训练与跨块排名证书",
                        proven ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        return artifactSnapshotCP4(report, 
                Stage.RANKER,
                proven ? Status.OBSERVED : Status.NOT_PROVEN,
                "V51ActionRankerEngine",
                "frozen rank artifact",
                proven ? "Ranker产物标记已证明，仍需证书哈希复核" : "Ranker未训练/未证明",
                summary,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

public static String stateSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String key, String fallback) {
        if (store == null) return safeSnapshotCP4(report, fallback);
        try {
            String value = store.getState(key, fallback);
            return value == null ? safeSnapshotCP4(report, fallback) : value;
        } catch (Throwable error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);}
    }

public static String errorTextSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Throwable error) {
        if (error == null) return "UNKNOWN";
        String message = error.getMessage();
        String out = error.getClass().getSimpleName();
        if (message != null && message.trim().length() > 0) out += ':' + message.trim();
        return limitSnapshotCP4(report, out, 1000);
    }

public static String limitSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value, int maximum) {
        String text = safeSnapshotCP4(report, value);
        return text.length() <= maximum ? text : text.substring(0, maximum);
    }

public StageArtifact collectActionSpaceSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        long combinations = K3ActionSpaceEngine.choose(10, 3);
        long formal = 10L * combinations;
        boolean exact = combinations == 120L && formal == K3ActionSpaceEngine.TOTAL_ACTIONS;
        QueryCount persisted =
                queryCountSnapshotCP4(report, 
                        store,
                        "SELECT COUNT(1) FROM "
                                + V51Repository.ACTION_SPACE
                                + " WHERE target_issue=?",
                        context.targetIssue.length() == 0
                                ? null
                                : new String[] {context.targetIssue});
        String requestStatus = context.targetIssue.length() == 0 ? "MISSING" : AnalysisRequestStoreR36.statusSnapshotCP4(report, context.targetIssue);
        String deferredStatus = context.targetIssue.length() == 0 ? "MISSING" : DeferredEvidenceWriterR36.statusForTargetSnapshotCP4(report, store, context.targetIssue);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(metricSnapshotCP4(report, "analysisRequestStatus", requestStatus, "state", "AnalysisRequestStoreR36", !requestStatus.startsWith("READ_ERROR")));
        metrics.add(metricSnapshotCP4(report, "deferredEvidenceStatus", deferredStatus, "state", "DeferredEvidenceWriterR36", !deferredStatus.startsWith("READ_ERROR")));
        metrics.add(
                metricSnapshotCP4(report, 
                        "combinationPerPosition",
                        String.valueOf(combinations),
                        "count",
                        "C(10,3)",
                        true));
        metrics.add(metricSnapshotCP4(report, "formalActions", String.valueOf(formal), "count", "10*C(10,3)", exact));
        metrics.add(
                metricSnapshotCP4(report, 
                        "persistedActions",
                        String.valueOf(persisted.value),
                        "rows",
                        "v51_action_space",
                        persisted.ok));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.ACTION_SPACE, "FORMULA"),
                        EvidenceGrade.CONTRACT,
                        "K3ActionSpaceEngine",
                        "10*C(10,3)=" + formal,
                        "排序、删除或剪枝不能改变正式宇宙基数",
                        (int) formal,
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.ACTION_SPACE, "PERSISTED"),
                        persisted.ok
                                ? EvidenceGrade.DATABASE_OBSERVATION
                                : EvidenceGrade.READ_ERROR,
                        V51Repository.ACTION_SPACE,
                        persisted.render(),
                        "表行数必须按目标期读取，不能拿全表数冒充覆盖",
                        persisted.value,
                        -1.0d,
                        -1.0d,
                        persisted.ok));
        Status status = actionSpaceStatusR77(exact, persisted.ok, persisted.value,
                context.targetIssue, requestStatus, deferredStatus);
        String readError = !persisted.ok ? persisted.error
                : (requestStatus.startsWith("READ_ERROR") ? requestStatus
                : (deferredStatus.startsWith("READ_ERROR") ? deferredStatus : ""));
        return artifactSnapshotCP4(report, 
                Stage.ACTION_SPACE,
                status,
                "K3ActionSpaceEngine+V51Repository",
                "formal action universe",
                "formula=" + formal + "|persisted=" + persisted.render()
                        + "|deferredEvidenceStatus=" + deferredStatus,
                "ExactIdentityRequired=true|DeletionOrPruning=FORBIDDEN"
                        + "|analysisRequestStatus=" + requestStatus
                        + "|deferredEvidenceStatus=" + deferredStatus
                        + (status == Status.RETEST_REQUIRED ? "|未完成证据待续写，不代表1200动作已全部验收" : ""),
                readError,
                context.createdAt,
                metrics,
                evidence);
    }

public static QueryCount queryCountSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String sql, String[] args) {
        if (store == null) return QueryCount.error("STORE_NULL");
        if (args == null || args.length == 0 || safeSnapshotCP4(report, args[0]).length() == 0) {
            return new QueryCount(true, 0, "NO_TARGET");
        }
        Cursor cursor = null;
        try {
            SQLiteDatabase db = store.readable();
            cursor = report.select(sql, args);
            return new QueryCount(true, cursor.moveToFirst() ? cursor.getInt(0) : 0, "");
        } catch (Throwable error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} finally {
            if (cursor != null) cursor.close();
        }
    }

public StageArtifact collectK3FunnelSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        int[] sizes = K3FunnelOrchestrator.STAGE_SIZES;
        boolean exact =
                sizes != null
                        && sizes.length == 7
                        && sizes[0] == 9
                        && sizes[1] == 8
                        && sizes[2] == 7
                        && sizes[3] == 6
                        && sizes[4] == 5
                        && sizes[5] == 4
                        && sizes[6] == 3;
        String trace = stateSnapshotCP4(report, store, "v51_funnel_summary", "NO_RUNTIME_SAMPLE");
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(
                metricSnapshotCP4(report, 
                        "stageCount",
                        String.valueOf(sizes.length),
                        "count",
                        "K3FunnelOrchestrator",
                        true));
        metrics.add(metricSnapshotCP4(report, "stageSizes", joinIntsSnapshotCP4(report, sizes), "count[]", "K3FunnelOrchestrator", true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "finalK",
                        sizes.length == 0 ? "-1" : String.valueOf(sizes[sizes.length - 1]),
                        "count",
                        "fixed contract",
                        true));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.K3_FUNNEL, "STATIC"),
                        EvidenceGrade.STATIC_BUILD,
                        "K3FunnelOrchestrator.STAGE_SIZES",
                        "stages=" + joinIntsSnapshotCP4(report, sizes) + "|exact=" + exact,
                        "漏斗只保留候选轨迹，不得删除1200正式动作证据",
                        sizes.length,
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.K3_FUNNEL, "RUNTIME"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "B10 proof ledger",
                        trace,
                        "静态阶段常量不能证明物理设备每期持久化成功",
                        "NO_RUNTIME_SAMPLE".equals(trace) ? 0 : 1,
                        -1.0d,
                        -1.0d,
                        true));
        Status status =
                !exact
                        ? Status.FAIL
                        : ("NO_RUNTIME_SAMPLE".equals(trace)
                                ? Status.RETEST_REQUIRED
                                : Status.PASS);
        return artifactSnapshotCP4(report, 
                Stage.K3_FUNNEL,
                status,
                "K3FunnelOrchestrator_ONLY",
                "exact nested 9/8/7/6/5/4/3 audit",
                exact ? "固定K3漏斗合同成立" : "固定K3漏斗合同破坏",
                "static=" + joinIntsSnapshotCP4(report, sizes) + "\nruntime=" + trace,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

public static String joinIntsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, int[] values) {
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) out.append(',');
                out.append(values[i]);
            }
        }
        return out.toString();
    }

public StageArtifact collectCrossPositionSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        String relation = stateSnapshotCP4(report, store, "b06_relation_summary", "NO_RUNTIME_SAMPLE");
        String permutation = stateSnapshotCP4(report, store, "v51_permutation_summary", "NO_RUNTIME_SAMPLE");
        String transition = stateSnapshotCP4(report, store, "v51_transition_summary", "NO_RUNTIME_SAMPLE");
        boolean sample =
                !"NO_RUNTIME_SAMPLE".equals(relation)
                        || !"NO_RUNTIME_SAMPLE".equals(permutation)
                        || !"NO_RUNTIME_SAMPLE".equals(transition);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(metricSnapshotCP4(report, "relationSummary", relation, "text", "app_state", sample));
        metrics.add(metricSnapshotCP4(report, "permutationSummary", permutation, "text", "app_state", sample));
        metrics.add(metricSnapshotCP4(report, "transitionSummary", transition, "text", "app_state", sample));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.CROSS_POSITION, "PIT"),
                        EvidenceGrade.CONTRACT,
                        "V51RelationGraphVerifier",
                        "跨位置特征只允许目标开奖前快照",
                        "同期开奖信息进入特征即构成泄漏",
                        sample ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.CROSS_POSITION, "RUNTIME"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "relation/permutation/transition summaries",
                        relation + '|' + permutation + '|' + transition,
                        "摘要存在不等于跨块证书通过",
                        sample ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        return artifactSnapshotCP4(report, 
                Stage.CROSS_POSITION,
                sample ? Status.OBSERVED : Status.RETEST_REQUIRED,
                "V51RelationGraphVerifier+V51PermutationFeatureEngine",
                "pre-draw cross-position evidence",
                sample ? "跨位置运行证据已观察" : "跨位置等待运行样本",
                "relation="
                        + relation
                        + "\npermutation="
                        + permutation
                        + "\ntransition="
                        + transition,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

public StageArtifact collectNumberSurfaceSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        boolean proven = V51NumberModelArtifact.PROVEN;
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(metricSnapshotCP4(report, "positions", "10", "count", "fixed contract", true));
        metrics.add(metricSnapshotCP4(report, "numbersPerPosition", "10", "count", "fixed contract", true));
        metrics.add(metricSnapshotCP4(report, "numberRows", "100", "count", "10x10 formula", true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "artifactProven",
                        String.valueOf(proven),
                        "boolean",
                        "V51NumberModelArtifact",
                        true));
        String distribution = stateSnapshotCP4(report, store, "v51_number_distribution_summary", "NO_RUNTIME_SAMPLE");
        String sinkhorn = stateSnapshotCP4(report, store, "v51_sinkhorn_summary", "NO_RUNTIME_SAMPLE");
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.NUMBER_SURFACE, "CONTRACT"),
                        EvidenceGrade.CONTRACT,
                        "V51PositionActionBuilder",
                        "10 positions * 10 numbers = 100 point-in-time probabilities",
                        "完整性不等于预测有效性",
                        100,
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.NUMBER_SURFACE, "MODEL"),
                        proven ? EvidenceGrade.STRICT_OOS : EvidenceGrade.STATIC_BUILD,
                        "V51NumberModelArtifact.PROVEN",
                        "artifactProof=" + proven,
                        "未冻结训练产物不得用工程实现替代模型证明",
                        proven ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        return artifactSnapshotCP4(report, 
                Stage.NUMBER_SURFACE,
                proven ? Status.OBSERVED : Status.NOT_PROVEN,
                "V51NumberModelArtifact+V51SinkhornEngine",
                "frozen input and persisted summaries",
                proven ? "号码面产物已冻结，仍需证书复核" : "号码面工程存在但模型未证明",
                "distribution=" + distribution + "\nsinkhorn=" + sinkhorn
                        + "\ncurrentPersistedSurface="+surfaceEvidenceOrFailureR80(report.readable(),context.targetIssue,()->{report.checkpoint();return true;}),
                "",
                context.createdAt,
                metrics,
                evidence);
    }

public StageArtifact collectLedgerSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        if (store == null) return readErrorSnapshotCP4(report, Stage.LEDGER, context, "STORE_NULL", "DbStore");
        String target = context.targetIssue;
        if (target.length() == 0) {
            return artifactSnapshotCP4(report, 
                    Stage.LEDGER,
                    Status.RETEST_REQUIRED,
                    "V51Repository",
                    "canonical tables",
                    "NO_TARGET",
                    "尚无目标期，无法审计本期首写冻结与B12不变量",
                    "",
                    context.createdAt,
                    Collections.<Metric>emptyList(),
                    singletonEvidenceSnapshotCP4(report, 
                            evidenceIdSnapshotCP4(report, Stage.LEDGER, "NO_TARGET"),
                            EvidenceGrade.DATABASE_OBSERVATION,
                            "app_state.target_issue",
                            "NO_TARGET",
                            "需要至少一次物理设备运行闭环",
                            0,
                            true));
        }
        V51Repository.B12InvariantAudit audit = V51Repository.auditB12SnapshotCP4(report, store, target);
        String requestStatus = AnalysisRequestStoreR36.statusSnapshotCP4(report, target);
        String deferredStatus = DeferredEvidenceWriterR36.statusForTargetSnapshotCP4(report, store, target);
        boolean deferredInFlight =
                AnalysisRequestStoreR36.CANONICAL_COMMITTED.equals(requestStatus)
                        && (DeferredEvidenceWriterR36.PENDING.equals(deferredStatus)
                                || DeferredEvidenceWriterR36.RUNNING.equals(deferredStatus)
                                || DeferredEvidenceWriterR36.RETRY_WAIT.equals(deferredStatus));
        boolean inFlight =
                AnalysisRequestStoreR36.REQUESTED.equals(requestStatus)
                        || AnalysisRequestStoreR36.RUNNING.equals(requestStatus)
                        || AnalysisRequestStoreR36.RETRY_WAIT.equals(requestStatus)
                        || "MISSING".equals(requestStatus)
                        || deferredInFlight;
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(metricSnapshotCP4(report, "targetIssue", target, "issue", "V51Repository", true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "b12InvariantPass",
                        String.valueOf(audit.pass),
                        "boolean",
                        "V51Repository.auditB12",
                        true));
        metrics.add(metricSnapshotCP4(report, "analysisRequestStatus", requestStatus, "state", "AnalysisRequestStoreR36", true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "deferredEvidenceStatus",
                        deferredStatus,
                        "state",
                        "DeferredEvidenceWriterR36",
                        !deferredStatus.startsWith("READ_ERROR:")));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.LEDGER, "B12"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "V51Repository#auditB12",
                        audit.detail + "|hash=" + audit.checksum,
                        "审计失败必须保持SHADOW并导出原始DB",
                        1,
                        -1.0d,
                        -1.0d,
                        true));
        return artifactSnapshotCP4(report, 
                Stage.LEDGER,
                audit.pass ? Status.PASS : (inFlight ? Status.RETEST_REQUIRED : Status.FAIL),
                "V51Repository+CanonicalObservationWriterR36",
                "B12 invariant audit",
                audit.pass
                        ? "B12账本不变量通过"
                        : (inFlight
                                ? "账本等待 canonical 或延后证据完成"
                                : "B12账本不变量失败"),
                audit.detail
                        + "\nanalysisRequestStatus="
                        + requestStatus
                        + "\ndeferredEvidenceStatus="
                        + deferredStatus,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

public static List<Evidence> singletonEvidenceSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            String id,
            EvidenceGrade grade,
            String source,
            String observation,
            String counterEvidence,
            int sample,
            boolean readSuccessful) {
        ArrayList<Evidence> out = new ArrayList<Evidence>();
        out.add(
                new Evidence(
                        id,
                        grade,
                        source,
                        observation,
                        counterEvidence,
                        sample,
                        -1.0d,
                        -1.0d,
                        readSuccessful));
        return out;
    }

public StageArtifact readErrorSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            Stage stage, DiagnosisContext context, String error, String source) {
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, stage, "READ_ERROR"),
                        EvidenceGrade.READ_ERROR,
                        source,
                        "READ_ERROR=" + safeSnapshotCP4(report, error),
                        "读取失败不得改写为0或PASS",
                        -1,
                        -1.0d,
                        -1.0d,
                        false));
        return artifactSnapshotCP4(report, 
                stage,
                Status.READ_ERROR,
                stageOwnerSnapshotCP4(report, stage),
                source,
                "READ_ERROR",
                "READ_ERROR=" + safeSnapshotCP4(report, error),
                error,
                context.createdAt,
                Collections.<Metric>emptyList(),
                evidence);
    }

public static String stageOwnerSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Stage stage) {
        switch (stage) {
            case DATA:
                return "TargetIssueResolver_ONLY";
            case RUNTIME:
                return "AutoFetchService->BusinessReconcilerR36->V51RuntimeOrchestrator";
            case LEDGER:
                return "V51Repository";
            case NUMBER_SURFACE:
                return "V51NumberModelArtifact";
            case CROSS_POSITION:
                return "V51RelationGraphVerifier";
            case K3_FUNNEL:
                return "K3FunnelOrchestrator_ONLY";
            case ACTION_SPACE:
                return "K3ActionSpaceEngine";
            case RANKER:
                return "V51ActionRankerEngine";
            case CALIBRATION:
                return "OosAfterDrawWriterR36";
            case FREQUENCY:
                return "DecisionGateEngine_ONLY";
            case FAILURE_TREE:
                return "V57FailureDiagnosisEngine";
            case ABLATION:
                return "StrategyConflictAblationEngine";
            case CODE_OWNER:
                return "CodeOwnershipCompletenessAuditEngine";
            case IMPROVEMENT:
            default:
                return "ImprovementChallengerPlanEngine";
        }
    }

public StageArtifact collectRuntimeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        if (store == null) return readErrorSnapshotCP4(report, Stage.RUNTIME, context, "STORE_NULL", "DbStore");
        AutoFetchService.B13RuntimeHealth health = AutoFetchService.b13HealthSnapshotCP4(report, store);
        RuntimeHealthLedgerR36.BusinessTruth business = context.businessTruth;
        ModelEffectivenessAnalyzerR35.RuntimeSummary runtime = context.model.runtime;
        String durable = AnalysisRequestStoreR36.diagnosticSnapshotCP4(report, context.targetIssue);
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        metrics.add(latestAttemptMetricR76(runtime));
        metrics.add(
                metricSnapshotCP4(report, 
                        "serviceAlive",
                        business.serviceAlive ? "1" : "0",
                        "boolean",
                        "RuntimeHealthLedgerR36.BusinessTruth",
                        true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "businessAlive",
                        business.businessAlive ? "1" : "0",
                        "boolean",
                        "RuntimeHealthLedgerR36.BusinessTruth+B13",
                        true));
        metrics.add(
                metricSnapshotCP4(report, 
                        "rootCause",
                        business.rootCause,
                        "state",
                        "RuntimeHealthLedgerR36.BusinessTruth",
                        business.readError.length() == 0));
        metrics.add(
                metricSnapshotCP4(report, 
                        "serviceAge",
                        String.valueOf(health.serviceAge),
                        "ms",
                        "service heartbeat",
                        health.serviceBeat > 0));
        metrics.add(
                metricSnapshotCP4(report, 
                        "fetchAge",
                        String.valueOf(health.fetchAge),
                        "ms",
                        "fetch heartbeat",
                        health.fetchBeat > 0));
        metrics.add(
                metricSnapshotCP4(report, 
                        "analysisAge",
                        String.valueOf(health.analysisAge),
                        "ms",
                        "analysis heartbeat",
                        health.analysisBeat > 0));
        metrics.add(
                metricSnapshotCP4(report, 
                        "requestToCommitP95",
                        String.valueOf(runtime.analysisP95Ms),
                        "ms",
                        "RuntimeHealthLedgerR36",
                        "OK".equals(runtime.readStatus)));
        metrics.add(
                metricSnapshotCP4(report, 
                        "durableRequest",
                        durable,
                        "state",
                        "AnalysisRequestStoreR36",
                        !"REQUEST_ROW_MISSING".equals(durable)));
        metrics.add(
                metricSnapshotCP4(report, 
                        "maxConcurrency",
                        String.valueOf(runtime.maxConcurrency),
                        "count",
                        "RuntimeHealthLedgerR36",
                        "OK".equals(runtime.readStatus)));
        metrics.add(
                metricSnapshotCP4(report, 
                        "oomCount",
                        String.valueOf(runtime.oomCount),
                        "count",
                        "RuntimeHealthLedgerR36",
                        "OK".equals(runtime.readStatus)));
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.RUNTIME, "HEARTBEAT"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "AutoFetchService#b13Health",
                        AutoFetchService.b13DiagnosticsSnapshotCP4(report, store),
                        "静态APK不能证明熄屏、杀进程或24小时运行",
                        health.businessAlive ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.RUNTIME, "LEDGER"),
                        "READ_ERROR".equals(runtime.readStatus)
                                ? EvidenceGrade.READ_ERROR
                                : EvidenceGrade.DATABASE_OBSERVATION,
                        "ModelEffectivenessAnalyzerR35.runtime",
                        runtime.summary(),
                        "READ_ERROR不得显示成零样本PASS",
                        runtime.eventCount,
                        -1.0d,
                        -1.0d,
                        !"READ_ERROR".equals(runtime.readStatus)));
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.RUNTIME, "DURABLE_REQUEST"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "AnalysisRequestStoreR36#diagnostic",
                        durable,
                        "活动外部Owner只能等待；失败线程不得改写其租约",
                        "REQUEST_ROW_MISSING".equals(durable) ? 0 : 1,
                        -1.0d,
                        -1.0d,
                        true));
        Status status;
        String readError = "";
        if ("READ_ERROR".equals(business.rootCause)
                || "READ_ERROR".equals(runtime.readStatus)) {
            status = Status.READ_ERROR;
            readError = business.readError.length() > 0 ? business.readError : runtime.error;
        } else if ("RUNTIME_CHAIN".equals(business.rootCause)
                || "SETTLEMENT_OOS".equals(business.rootCause)
                || "PREDRAW_COVERAGE_MISSING".equals(business.rootCause)
                || !business.businessAlive) {
            status = Status.FAIL;
        } else if ("ANALYSIS_ATTEMPT_FAILED".equals(runtime.lastAttemptType)
                && runtime.lastAttemptAt >= runtime.lastAt) {
            status = Status.FAIL;
        } else if (runtime.eventCount <= 0) {
            status = Status.RETEST_REQUIRED;
        } else if (runtime.maxConcurrency > 1
                || runtime.analysisP95Ms > 5000L
                || runtime.oomCount > 0) {
            status = Status.FAIL;
        } else {
            status = Status.PASS;
        }
        return artifactSnapshotCP4(report, 
                Stage.RUNTIME,
                status,
                "AutoFetchService->BusinessReconcilerR36->V51RuntimeOrchestrator",
                "B13 heartbeat + RuntimeHealthLedgerR36",
                business.status + '|' + business.rootCause + '|' + runtime.readStatus,
                business.canonical()
                        + "\n"
                        + AutoFetchService.b13DiagnosticsSnapshotCP4(report, store)
                        + "\n"
                        + runtime.summary()
                        + "\ndurableRequest="
                        + durable,
                readError,
                context.createdAt,
                metrics,
                evidence);
    }

public StageArtifact collectDataSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosisContext context) {
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        ArrayList<Evidence> evidence = new ArrayList<Evidence>();
        if (store == null) return readErrorSnapshotCP4(report, Stage.DATA, context, "STORE_NULL", "DbStore");
        String base = stateSnapshotCP4(report, store, "current_issue", "");
        String target = context.targetIssue;
        TargetIssueResolver.Resolution resolution = TargetIssueResolver.next(base);
        boolean targetValid = target.length() > 0 && TargetIssueResolver.isValid(target);
        boolean exactNext =
                resolution.valid && targetValid && resolution.targetIssue.equals(target);
        metrics.add(metricSnapshotCP4(report, "baseIssue", base, "issue", "app_state", base.length() > 0));
        metrics.add(metricSnapshotCP4(report, "targetIssue", target, "issue", "app_state", targetValid));
        metrics.add(
                metricSnapshotCP4(report, 
                        "exactNextTarget",
                        String.valueOf(exactNext),
                        "boolean",
                        "TargetIssueResolver",
                        resolution.valid));
        String observation =
                "base="
                        + base
                        + "|target="
                        + target
                        + "|resolver="
                        + resolution.targetIssue
                        + "|exact="
                        + exactNext;
        evidence.add(
                new Evidence(
                        evidenceIdSnapshotCP4(report, Stage.DATA, "ISSUE"),
                        EvidenceGrade.DATABASE_OBSERVATION,
                        "TargetIssueResolver+app_state",
                        observation,
                        "空期号或非精确下一期会阻断正式分析",
                        targetValid ? 1 : 0,
                        -1.0d,
                        -1.0d,
                        true));
        Status status = exactNext ? Status.PASS : Status.WARN;
        return artifactSnapshotCP4(report, 
                Stage.DATA,
                status,
                "TargetIssueResolver_ONLY",
                "DbStore.app_state",
                exactNext ? "精确下一期已解析" : "期号需运行时复核",
                observation,
                "",
                context.createdAt,
                metrics,
                evidence);
    }

public StageArtifact stageSnapshotCP12(com.ethk3.novaonepro.ReportReadContextCP4 report,Stage stage){
      ModelEffectivenessAnalyzerR35.Analysis model=report.memo("model-analysis",()->new ModelEffectivenessAnalyzerR35().analyzeSnapshotCP4(report,report));
      RuntimeHealthLedgerR36.BusinessTruth truth=report.memo("business-truth",()->RuntimeHealthLedgerR36.businessTruthSnapshotCP4(report,report));
      String target=truth.latestDrawIssue.length()==0?report.state("target_issue",""):AppConfig.nextIssueText(truth.latestDrawIssue);
      DiagnosisContext context=new DiagnosisContext(report.nowMs(),target,model,truth);
      switch(stage){
case DATA:return collectDataSnapshotCP4(report,report,context);
case RUNTIME:return collectRuntimeSnapshotCP4(report,report,context);
case LEDGER:return collectLedgerSnapshotCP4(report,report,context);
case NUMBER_SURFACE:return collectNumberSurfaceSnapshotCP4(report,report,context);
case CROSS_POSITION:return collectCrossPositionSnapshotCP4(report,report,context);
case K3_FUNNEL:return collectK3FunnelSnapshotCP4(report,report,context);
case ACTION_SPACE:return collectActionSpaceSnapshotCP4(report,report,context);
case RANKER:return collectRankerSnapshotCP4(report,report,context);
case CALIBRATION:return collectCalibrationSnapshotCP4(report,report,context);
case FREQUENCY:return collectFrequencySnapshotCP4(report,report,context);
case FAILURE_TREE:return collectFailureTreeSnapshotCP4(report,report,context);
case ABLATION:return collectAblationSnapshotCP4(report,report,context);
case CODE_OWNER:return collectCodeOwnerSnapshotCP4(report,report,context);
case IMPROVEMENT:return collectImprovementSnapshotCP4(report,report,context);
default:throw new IllegalArgumentException("REPORT_STAGE_UNKNOWN");}
    }

public DiagnosisBundle diagnosisSnapshotCP12(com.ethk3.novaonepro.ReportReadContextCP4 report){
      java.util.List<StageArtifact> stages=new java.util.ArrayList<>();
      for(Stage stage:Stage.values())stages.add(report.memo("diagnosis-stage-"+stage.name(),()->stageSnapshotCP12(report,stage)));
      RuntimeHealthLedgerR36.BusinessTruth truth=report.memo("business-truth",()->RuntimeHealthLedgerR36.businessTruthSnapshotCP4(report,report));
      String target=truth.latestDrawIssue.length()==0?report.state("target_issue",""):AppConfig.nextIssueText(truth.latestDrawIssue);
      return assemble(report.nowMs(),target,stages);
    }
}
