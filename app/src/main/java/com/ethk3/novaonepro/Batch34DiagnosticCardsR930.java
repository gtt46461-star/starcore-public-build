package com.ethk3.novaonepro;

import java.util.List;
import java.util.Locale;

/**
 * E05: four independent, copyable diagnostic cards.
 *
 * <p>Model accuracy, policy effect, issue-level execution and resource efficiency have different
 * denominators and must never be merged into one optimistic percentage. Missing future evidence is
 * rendered as N/A/UNPROVEN rather than zero or a fabricated pass.</p>
 */
public final class Batch34DiagnosticCardsR930 {
    public static final String VERSION = "R930_BATCH34_DIAGNOSTIC_CARDS_V1";

    private Batch34DiagnosticCardsR930() {}

    public static final class Cards {
        public final String modelAccuracy;
        public final String strategyEffect;
        public final String issueChain;
        public final String resourceEfficiency;
        public final String canonicalSha256;

        Cards(String modelAccuracy, String strategyEffect, String issueChain,
              String resourceEfficiency) {
            this.modelAccuracy = safe(modelAccuracy);
            this.strategyEffect = safe(strategyEffect);
            this.issueChain = safe(issueChain);
            this.resourceEfficiency = safe(resourceEfficiency);
            this.canonicalSha256 = V51ChecksumEngine.sha256(
                    VERSION + "\n" + this.modelAccuracy + "\n" + this.strategyEffect
                            + "\n" + this.issueChain + "\n" + this.resourceEfficiency);
        }

        public String render() {
            return "【E05-A.模型准确率卡】\n" + modelAccuracy
                    + "\n\n【E05-B.策略效果卡】\n" + strategyEffect
                    + "\n\n【E05-C.期级链路卡】\n" + issueChain
                    + "\n\n【E05-D.资源效率卡】\n" + resourceEfficiency
                    + "\n\nE05CardsSHA256=" + canonicalSha256
                    + "\nE05NoMetricConflation=true"
                    + "\nE05MissingEvidenceRenderedAs=N/A_OR_UNPROVEN";
        }
    }

    public static Cards capture(DbStore store) {
        Batch34RuntimeGateR929.ProductionEvidence evidence =
                Batch34ProductionEvidenceR929.current();
        Batch34RuntimeGateR929.GateResult gate =
                Batch34RuntimeGateR929.evaluate(evidence, true);
        return new Cards(
                modelAccuracy(evidence, gate),
                strategyEffect(evidence, gate),
                issueChain(store, evidence, gate),
                resourceEfficiency(store, evidence));
    }

    public static String render(DbStore store) { return capture(store).render(); }

    private static String modelAccuracy(
            Batch34RuntimeGateR929.ProductionEvidence evidence,
            Batch34RuntimeGateR929.GateResult gate) {
        StringBuilder out = new StringBuilder(1024);
        out.append("MetricScope=MODEL_AND_CALIBRATION_ONLY\n")
                .append("RawRankingScoreIsProbability=false\n")
                .append("BrierECERequireFrozenSelectedActionProbability=true\n")
                .append("GeneratedFromRealData=")
                .append(Batch34ProductionEvidenceR929.GENERATED_FROM_REAL_DATA).append('\n')
                .append("FutureOosProven=")
                .append(Batch34ProductionEvidenceR929.FUTURE_OOS_PROVEN).append('\n')
                .append("StructuralEvidenceReady=").append(gate.structuralReady).append('\n')
                .append("EffectEvidenceReady=").append(gate.effectReady).append('\n');
        Batch34RuntimeGateR929.PolicyEvidence policy = evidence == null ? null : evidence.policy;
        if (policy == null) {
            out.append("SelectedSupport=N/A\nSelectedHitRate=N/A\nSelectedWilsonLower=N/A\n")
                    .append("Brier=N/A\nECE=N/A\nLongestMiss=N/A\n")
                    .append("ModelAccuracyStatus=UNPROVEN_NO_FUTURE_POLICY_EVIDENCE");
        } else {
            out.append("SelectedSupport=").append(policy.selected).append('\n')
                    .append("SelectedHits=").append(policy.selectedHits).append('\n')
                    .append("SelectedHitRate=").append(metric(policy.selectedHitRate)).append('\n')
                    .append("SelectedWilsonLower=").append(metric(policy.selectedWilsonLower)).append('\n')
                    .append("Brier=").append(metric(policy.brier)).append('\n')
                    .append("ECE=").append(metric(policy.ece)).append('\n')
                    .append("LongestMiss=").append(policy.longestMiss).append('\n')
                    .append("ModelAccuracyStatus=")
                    .append(gate.effectReady ? "EVIDENCE_GATE_PASS" : "EVIDENCE_GATE_FAIL");
        }
        Batch34RuntimeGateR929.StabilityEvidence stability =
                evidence == null ? null : evidence.stability;
        if (stability == null) {
            out.append("\nSeedCount=N/A\nTop1Agreement=N/A\nKendallTau=N/A\n")
                    .append("ProbabilityIntervalWidthP95=N/A\nRankDisplacementP95=N/A");
        } else {
            out.append("\nSeedCount=").append(stability.deterministicSeeds)
                    .append("\nTop1Agreement=").append(metric(stability.top1Agreement))
                    .append("\nKendallTau=").append(metric(stability.meanKendallTau))
                    .append("\nProbabilityIntervalWidthP95=")
                    .append(metric(stability.probabilityIntervalWidthP95))
                    .append("\nRankDisplacementP95=")
                    .append(metric(stability.rankDisplacementP95));
        }
        return out.toString();
    }

    private static String strategyEffect(
            Batch34RuntimeGateR929.ProductionEvidence evidence,
            Batch34RuntimeGateR929.GateResult gate) {
        StringBuilder out = new StringBuilder(1024);
        out.append("MetricScope=FROZEN_POLICY_FUTURE_WINDOW_ONLY\n")
                .append("TargetSelectedCoverage=15.00%\n")
                .append("TargetSelectedHitRate=40.00%\n")
                .append("TargetECE<=4.00%\n")
                .append("TargetLongestMiss<=8\n")
                .append("QuotaEnforcementAllowed=false\n")
                .append("FrequencyDebtAllowed=false\n")
                .append("CertificateDirectBetAuthority=false\n");
        Batch34RuntimeGateR929.PolicyEvidence policy = evidence == null ? null : evidence.policy;
        if (policy == null) {
            out.append("SystemAvailable=N/A\nEligible=N/A\nSelected=N/A\n")
                    .append("ExecutionStarted=N/A\nFrozen=N/A\nREAL=N/A\nSHADOW=N/A\n")
                    .append("SelectedCoverage=N/A\nRealCoverage=N/A\n")
                    .append("StrategyEffectStatus=UNPROVEN_NO_UNCONSUMED_FUTURE_WINDOW");
        } else {
            out.append("SystemAvailable=").append(policy.systemAvailable)
                    .append("\nEligible=").append(policy.eligible)
                    .append("\nSelected=").append(policy.selected)
                    .append("\nExecutionStarted=").append(policy.executionStarted)
                    .append("\nFrozen=").append(policy.frozen)
                    .append("\nREAL=").append(policy.real)
                    .append("\nSHADOW=").append(policy.shadow)
                    .append("\nSelectedCoverage=").append(metric(policy.selectedCoverage()))
                    .append("\nRealCoverage=").append(metric(policy.realCoverage()))
                    .append("\nStrategyEffectStatus=")
                    .append(gate.effectReady ? "PASS" : "FAIL");
        }
        out.append("\nRealEligible=").append(gate.realEligible)
                .append("\nPolicyEffectProven=")
                .append(Batch34ProductionEvidenceR929.POLICY_EFFECT_PROVEN)
                .append("\nGateCode=").append(gate.code)
                .append("\nGateFailures=").append(join(gate.failures));
        return out.toString();
    }

    private static String issueChain(
            DbStore store,
            Batch34RuntimeGateR929.ProductionEvidence evidence,
            Batch34RuntimeGateR929.GateResult gate) {
        StringBuilder out = new StringBuilder(1536);
        out.append("MetricScope=ONE_TARGET_ISSUE_END_TO_END\n")
                .append("RequiredRoute=planned>system_available>eligible>selected>")
                .append("execution_started>frozen>REAL_OR_SHADOW_OR_AUDITED_REJECTION\n")
                .append("DecisionOwner=DecisionGateEngine_ONLY\n")
                .append("SettlementOwner=SettlementEngine_ONLY\n")
                .append("ShadowRequiredWhenNotReal=").append(gate.shadowRequiredWhenNotReal)
                .append("\nSystemOpportunityRoute=")
                .append(gate.realEligible ? "EXISTING_DECISION_GATE"
                        : (gate.shadowRequiredWhenNotReal ? "SHADOW_REQUIRED" : "AUDITED_REJECTION"))
                .append("\nCaptureDenominators=");
        try {
            out.append(store == null ? "N/A_STORE_NULL"
                    : ResearchCaptureStateMachineR930.denominatorSummary(store));
        } catch (Throwable failure) {
            out.append("N/A_READ_ERROR_").append(failure.getClass().getSimpleName());
        }
        out.append("\nCurrentEvidenceStatus=")
                .append(Batch34ProductionEvidenceR929.EVIDENCE_STATUS)
                .append("\nCurrentBundle=")
                .append(evidence == null || evidence.bundle == null
                        ? "N/A" : evidence.bundle.bundleSha256)
                .append("\nCurrentPolicy=")
                .append(evidence == null || evidence.policy == null
                        ? "N/A" : evidence.policy.policySha256)
                .append("\nIssueChainStatus=")
                .append(gate.realEligible ? "REAL_GATE_ELIGIBLE"
                        : "NO_REAL_PROOF_KEEP_SHADOW_AND_AUDIT");
        return out.toString();
    }

    private static String resourceEfficiency(
            DbStore store, Batch34RuntimeGateR929.ProductionEvidence evidence) {
        StringBuilder out = new StringBuilder(2048);
        out.append("MetricScope=COMPUTE_MEMORY_DB_AND_RESEARCH_BUDGET_ONLY\n")
                .append("ResearchTierCapacity=A8_FULL1200|B24_SHARED_FULL_SCORE|C64_PROXY_NURSERY\n")
                .append("PerSliceBudgetNs=")
                .append(Batch34ResearchLifecycleR929.PER_SLICE_BUDGET_NS).append('\n')
                .append("InitialTotalBudgetNs=")
                .append(Batch34ResearchLifecycleR929.INITIAL_TOTAL_BUDGET_NS).append('\n')
                .append("FrequencyDebtAllowed=false\n")
                .append("ResourceMetricsDoNotProveAccuracy=true\n")
                .append("RuntimeResourceAudit=\n");
        try {
            out.append(store == null ? "N/A_STORE_NULL" : RuntimeResourceAuditR927.report(store));
        } catch (Throwable failure) {
            out.append("N/A_READ_ERROR_").append(failure.getClass().getSimpleName())
                    .append('|').append(safe(failure.getMessage()));
        }
        out.append("\nResourceCertificate=")
                .append(evidence == null || evidence.certificate == null
                        ? "N/A" : evidence.certificate.resourcePassed)
                .append("\nResourceEfficiencyStatus=")
                .append(evidence == null || evidence.certificate == null
                        ? "UNPROVEN_NO_CERTIFICATE" :
                        (evidence.certificate.resourcePassed ? "PASS" : "FAIL"));
        return out.toString();
    }

    private static String metric(double value) {
        return Double.isFinite(value)
                ? String.format(Locale.US, "%.6f|%.2f%%", value, value * 100.0d)
                : "N/A";
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) return "NONE";
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append(',');
            out.append(safe(value));
        }
        return out.toString();
    }

    private static String safe(String value) { return value == null ? "" : value; }

public static String renderSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) { return captureSnapshotCP4(report, store).render(); }

public static Cards captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        Batch34RuntimeGateR929.ProductionEvidence evidence =
                Batch34ProductionEvidenceR929.current();
        Batch34RuntimeGateR929.GateResult gate =
                Batch34RuntimeGateR929.evaluate(evidence, true);
        return new Cards(
                modelAccuracySnapshotCP4(report, evidence, gate),
                strategyEffectSnapshotCP4(report, evidence, gate),
                issueChainSnapshotCP4(report, store, evidence, gate),
                resourceEfficiencySnapshotCP4(report, store, evidence));
    }

public static String resourceEfficiencySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            com.ethk3.novaonepro.ReportReadContextCP4 store, Batch34RuntimeGateR929.ProductionEvidence evidence) {
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report, 2048);
        out.append("MetricScope=COMPUTE_MEMORY_DB_AND_RESEARCH_BUDGET_ONLY\n")
                .append("ResearchTierCapacity=A8_FULL1200|B24_SHARED_FULL_SCORE|C64_PROXY_NURSERY\n")
                .append("PerSliceBudgetNs=")
                .append(Batch34ResearchLifecycleR929.PER_SLICE_BUDGET_NS).append('\n')
                .append("InitialTotalBudgetNs=")
                .append(Batch34ResearchLifecycleR929.INITIAL_TOTAL_BUDGET_NS).append('\n')
                .append("FrequencyDebtAllowed=false\n")
                .append("ResourceMetricsDoNotProveAccuracy=true\n")
                .append("RuntimeResourceAudit=\n");
        try {
            out.append(store == null ? "N/A_STORE_NULL" : RuntimeResourceAuditR927.reportSnapshotCP4(report, store));
        } catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}
        out.append("\nResourceCertificate=")
                .append(evidence == null || evidence.certificate == null
                        ? "N/A" : evidence.certificate.resourcePassed)
                .append("\nResourceEfficiencyStatus=")
                .append(evidence == null || evidence.certificate == null
                        ? "UNPROVEN_NO_CERTIFICATE" :
                        (evidence.certificate.resourcePassed ? "PASS" : "FAIL"));
        return out.toString();
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value) { return value == null ? "" : value; }

public static String issueChainSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            com.ethk3.novaonepro.ReportReadContextCP4 store,
            Batch34RuntimeGateR929.ProductionEvidence evidence,
            Batch34RuntimeGateR929.GateResult gate) {
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report, 1536);
        out.append("MetricScope=ONE_TARGET_ISSUE_END_TO_END\n")
                .append("RequiredRoute=planned>system_available>eligible>selected>")
                .append("execution_started>frozen>REAL_OR_SHADOW_OR_AUDITED_REJECTION\n")
                .append("DecisionOwner=DecisionGateEngine_ONLY\n")
                .append("SettlementOwner=SettlementEngine_ONLY\n")
                .append("ShadowRequiredWhenNotReal=").append(gate.shadowRequiredWhenNotReal)
                .append("\nSystemOpportunityRoute=")
                .append(gate.realEligible ? "EXISTING_DECISION_GATE"
                        : (gate.shadowRequiredWhenNotReal ? "SHADOW_REQUIRED" : "AUDITED_REJECTION"))
                .append("\nCaptureDenominators=");
        try {
            out.append(store == null ? "N/A_STORE_NULL"
                    : ResearchCaptureStateMachineR930.denominatorSummarySnapshotCP4(report, store));
        } catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}
        out.append("\nCurrentEvidenceStatus=")
                .append(Batch34ProductionEvidenceR929.EVIDENCE_STATUS)
                .append("\nCurrentBundle=")
                .append(evidence == null || evidence.bundle == null
                        ? "N/A" : evidence.bundle.bundleSha256)
                .append("\nCurrentPolicy=")
                .append(evidence == null || evidence.policy == null
                        ? "N/A" : evidence.policy.policySha256)
                .append("\nIssueChainStatus=")
                .append(gate.realEligible ? "REAL_GATE_ELIGIBLE"
                        : "NO_REAL_PROOF_KEEP_SHADOW_AND_AUDIT");
        return out.toString();
    }

public static String strategyEffectSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            Batch34RuntimeGateR929.ProductionEvidence evidence,
            Batch34RuntimeGateR929.GateResult gate) {
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report, 1024);
        out.append("MetricScope=FROZEN_POLICY_FUTURE_WINDOW_ONLY\n")
                .append("TargetSelectedCoverage=15.00%\n")
                .append("TargetSelectedHitRate=40.00%\n")
                .append("TargetECE<=4.00%\n")
                .append("TargetLongestMiss<=8\n")
                .append("QuotaEnforcementAllowed=false\n")
                .append("FrequencyDebtAllowed=false\n")
                .append("CertificateDirectBetAuthority=false\n");
        Batch34RuntimeGateR929.PolicyEvidence policy = evidence == null ? null : evidence.policy;
        if (policy == null) {
            out.append("SystemAvailable=N/A\nEligible=N/A\nSelected=N/A\n")
                    .append("ExecutionStarted=N/A\nFrozen=N/A\nREAL=N/A\nSHADOW=N/A\n")
                    .append("SelectedCoverage=N/A\nRealCoverage=N/A\n")
                    .append("StrategyEffectStatus=UNPROVEN_NO_UNCONSUMED_FUTURE_WINDOW");
        } else {
            out.append("SystemAvailable=").append(policy.systemAvailable)
                    .append("\nEligible=").append(policy.eligible)
                    .append("\nSelected=").append(policy.selected)
                    .append("\nExecutionStarted=").append(policy.executionStarted)
                    .append("\nFrozen=").append(policy.frozen)
                    .append("\nREAL=").append(policy.real)
                    .append("\nSHADOW=").append(policy.shadow)
                    .append("\nSelectedCoverage=").append(metricSnapshotCP4(report, policy.selectedCoverage()))
                    .append("\nRealCoverage=").append(metricSnapshotCP4(report, policy.realCoverage()))
                    .append("\nStrategyEffectStatus=")
                    .append(gate.effectReady ? "PASS" : "FAIL");
        }
        out.append("\nRealEligible=").append(gate.realEligible)
                .append("\nPolicyEffectProven=")
                .append(Batch34ProductionEvidenceR929.POLICY_EFFECT_PROVEN)
                .append("\nGateCode=").append(gate.code)
                .append("\nGateFailures=").append(joinSnapshotCP4(report, gate.failures));
        return out.toString();
    }

public static String joinSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, List<String> values) {
        if (values == null || values.isEmpty()) return "NONE";
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report);
        for (String value : values) {
            if (out.length() > 0) out.append(',');
            out.append(safeSnapshotCP4(report, value));
        }
        return out.toString();
    }

public static String metricSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, double value) {
        return Double.isFinite(value)
                ? String.format(Locale.US, "%.6f|%.2f%%", value, value * 100.0d)
                : "N/A";
    }

public static String modelAccuracySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            Batch34RuntimeGateR929.ProductionEvidence evidence,
            Batch34RuntimeGateR929.GateResult gate) {
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report, 1024);
        out.append("MetricScope=MODEL_AND_CALIBRATION_ONLY\n")
                .append("RawRankingScoreIsProbability=false\n")
                .append("BrierECERequireFrozenSelectedActionProbability=true\n")
                .append("GeneratedFromRealData=")
                .append(Batch34ProductionEvidenceR929.GENERATED_FROM_REAL_DATA).append('\n')
                .append("FutureOosProven=")
                .append(Batch34ProductionEvidenceR929.FUTURE_OOS_PROVEN).append('\n')
                .append("StructuralEvidenceReady=").append(gate.structuralReady).append('\n')
                .append("EffectEvidenceReady=").append(gate.effectReady).append('\n');
        Batch34RuntimeGateR929.PolicyEvidence policy = evidence == null ? null : evidence.policy;
        if (policy == null) {
            out.append("SelectedSupport=N/A\nSelectedHitRate=N/A\nSelectedWilsonLower=N/A\n")
                    .append("Brier=N/A\nECE=N/A\nLongestMiss=N/A\n")
                    .append("ModelAccuracyStatus=UNPROVEN_NO_FUTURE_POLICY_EVIDENCE");
        } else {
            out.append("SelectedSupport=").append(policy.selected).append('\n')
                    .append("SelectedHits=").append(policy.selectedHits).append('\n')
                    .append("SelectedHitRate=").append(metricSnapshotCP4(report, policy.selectedHitRate)).append('\n')
                    .append("SelectedWilsonLower=").append(metricSnapshotCP4(report, policy.selectedWilsonLower)).append('\n')
                    .append("Brier=").append(metricSnapshotCP4(report, policy.brier)).append('\n')
                    .append("ECE=").append(metricSnapshotCP4(report, policy.ece)).append('\n')
                    .append("LongestMiss=").append(policy.longestMiss).append('\n')
                    .append("ModelAccuracyStatus=")
                    .append(gate.effectReady ? "EVIDENCE_GATE_PASS" : "EVIDENCE_GATE_FAIL");
        }
        Batch34RuntimeGateR929.StabilityEvidence stability =
                evidence == null ? null : evidence.stability;
        if (stability == null) {
            out.append("\nSeedCount=N/A\nTop1Agreement=N/A\nKendallTau=N/A\n")
                    .append("ProbabilityIntervalWidthP95=N/A\nRankDisplacementP95=N/A");
        } else {
            out.append("\nSeedCount=").append(stability.deterministicSeeds)
                    .append("\nTop1Agreement=").append(metricSnapshotCP4(report, stability.top1Agreement))
                    .append("\nKendallTau=").append(metricSnapshotCP4(report, stability.meanKendallTau))
                    .append("\nProbabilityIntervalWidthP95=")
                    .append(metricSnapshotCP4(report, stability.probabilityIntervalWidthP95))
                    .append("\nRankDisplacementP95=")
                    .append(metricSnapshotCP4(report, stability.rankDisplacementP95));
        }
        return out.toString();
    }
}
