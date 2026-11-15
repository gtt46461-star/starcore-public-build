package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Combined Batch-3/4 production evidence gate.
 * It never chooses an action; DecisionGateEngine remains the sole owner of BET creation.
 */
public final class Batch34RuntimeGateR929 {
    public static final String VERSION = "R929_BATCH34_RUNTIME_GATE_V2";
    public static final double MIN_SELECTED_COVERAGE = 0.15d;
    public static final double TARGET_HIT_RATE = 0.40d;
    public static final double MAX_ECE = 0.04d;
    public static final int MAX_MISS = 8;
    public static final int MIN_SEEDS = 5;
    public static final double MIN_TOP1_SEED_AGREEMENT = 0.80d;

    private Batch34RuntimeGateR929() {}

    public static final class PolicyEvidence {
        public final int systemAvailable;
        public final int eligible;
        public final int selected;
        public final int executionStarted;
        public final int frozen;
        public final int real;
        public final int shadow;
        public final int selectedHits;
        public final double selectedHitRate;
        public final double selectedWilsonLower;
        public final double brier;
        public final double ece;
        public final int longestMiss;
        public final String predictabilitySha256;
        public final String selectorSha256;
        public final String policySha256;
        public final String policyWindowSha256;
        public final boolean predictabilityIndependent;
        public final boolean selectorIndependent;
        public final boolean policyFrozenBeforeSealed;
        public final boolean sealedEvaluationPassed;
        public final boolean quotaEnforcementDisabled;
        public final boolean frequencyDebtDisabled;
        public final boolean denominatorsAudited;

        public PolicyEvidence(
                int systemAvailable,
                int eligible,
                int selected,
                int executionStarted,
                int frozen,
                int real,
                int shadow,
                int selectedHits,
                double selectedHitRate,
                double selectedWilsonLower,
                double brier,
                double ece,
                int longestMiss,
                String predictabilitySha256,
                String selectorSha256,
                String policySha256,
                String policyWindowSha256,
                boolean predictabilityIndependent,
                boolean selectorIndependent,
                boolean policyFrozenBeforeSealed,
                boolean sealedEvaluationPassed,
                boolean quotaEnforcementDisabled,
                boolean frequencyDebtDisabled,
                boolean denominatorsAudited) {
            this.systemAvailable = systemAvailable;
            this.eligible = eligible;
            this.selected = selected;
            this.executionStarted = executionStarted;
            this.frozen = frozen;
            this.real = real;
            this.shadow = shadow;
            this.selectedHits = selectedHits;
            this.selectedHitRate = selectedHitRate;
            this.selectedWilsonLower = selectedWilsonLower;
            this.brier = brier;
            this.ece = ece;
            this.longestMiss = longestMiss;
            this.predictabilitySha256 = safe(predictabilitySha256);
            this.selectorSha256 = safe(selectorSha256);
            this.policySha256 = safe(policySha256);
            this.policyWindowSha256 = safe(policyWindowSha256);
            this.predictabilityIndependent = predictabilityIndependent;
            this.selectorIndependent = selectorIndependent;
            this.policyFrozenBeforeSealed = policyFrozenBeforeSealed;
            this.sealedEvaluationPassed = sealedEvaluationPassed;
            this.quotaEnforcementDisabled = quotaEnforcementDisabled;
            this.frequencyDebtDisabled = frequencyDebtDisabled;
            this.denominatorsAudited = denominatorsAudited;
        }

        public double eligibleCoverage() {
            return systemAvailable <= 0 ? 0.0d : eligible / (double) systemAvailable;
        }

        public double selectedCoverage() {
            return systemAvailable <= 0 ? 0.0d : selected / (double) systemAvailable;
        }

        public double realCoverage() {
            return systemAvailable <= 0 ? 0.0d : real / (double) systemAvailable;
        }

        public String canonical() {
            return systemAvailable + "|" + eligible + "|" + selected + "|" + executionStarted
                    + "|" + frozen + "|" + real + "|" + shadow + "|" + selectedHits + "|"
                    + finite(selectedHitRate) + "|" + finite(selectedWilsonLower) + "|"
                    + finite(brier) + "|" + finite(ece) + "|" + longestMiss + "|"
                    + predictabilitySha256 + "|" + selectorSha256 + "|" + policySha256 + "|"
                    + policyWindowSha256 + "|" + predictabilityIndependent + "|"
                    + selectorIndependent + "|" + policyFrozenBeforeSealed + "|"
                    + sealedEvaluationPassed + "|" + quotaEnforcementDisabled + "|"
                    + frequencyDebtDisabled + "|" + denominatorsAudited;
        }
    }

    public static final class StabilityEvidence {
        public final int deterministicSeeds;
        public final double top1Agreement;
        public final double meanKendallTau;
        public final double probabilityIntervalWidthP95;
        public final double rankDisplacementP95;
        public final String uncertaintySha256;
        public final String rankStabilitySha256;
        public final boolean sameInputReplayIdentical;
        public final boolean bootstrapUsesIssueBlocks;

        public StabilityEvidence(
                int deterministicSeeds,
                double top1Agreement,
                double meanKendallTau,
                double probabilityIntervalWidthP95,
                double rankDisplacementP95,
                String uncertaintySha256,
                String rankStabilitySha256,
                boolean sameInputReplayIdentical,
                boolean bootstrapUsesIssueBlocks) {
            this.deterministicSeeds = deterministicSeeds;
            this.top1Agreement = top1Agreement;
            this.meanKendallTau = meanKendallTau;
            this.probabilityIntervalWidthP95 = probabilityIntervalWidthP95;
            this.rankDisplacementP95 = rankDisplacementP95;
            this.uncertaintySha256 = safe(uncertaintySha256);
            this.rankStabilitySha256 = safe(rankStabilitySha256);
            this.sameInputReplayIdentical = sameInputReplayIdentical;
            this.bootstrapUsesIssueBlocks = bootstrapUsesIssueBlocks;
        }

        public String canonical() {
            return deterministicSeeds + "|" + finite(top1Agreement) + "|"
                    + finite(meanKendallTau) + "|" + finite(probabilityIntervalWidthP95) + "|"
                    + finite(rankDisplacementP95) + "|" + uncertaintySha256 + "|"
                    + rankStabilitySha256 + "|" + sameInputReplayIdentical + "|"
                    + bootstrapUsesIssueBlocks;
        }
    }

    public static final class ProductionEvidence {
        public final Batch34EvidenceContractR929.DatasetManifest dataset;
        public final Batch34EvidenceContractR929.BundleManifest bundle;
        public final Batch34StatisticalGovernanceR929.CertificateV2 certificate;
        public final Batch34StatisticalGovernanceR929.DomainCertificate domainCertificate;
        public final PolicyEvidence policy;
        public final StabilityEvidence stability;
        public final boolean engineeringContractsPassed;
        public final boolean relationGraphAblationPassed;
        public final boolean failureAblationPassed;
        public final boolean atomRegistryPassed;
        public final boolean inventionGenomeGatePassed;
        public final boolean sameCertificateUpgradePassed;
        public final String sourceCommitSha256;
        public final String evidenceIndexSha256;

        public ProductionEvidence(
                Batch34EvidenceContractR929.DatasetManifest dataset,
                Batch34EvidenceContractR929.BundleManifest bundle,
                Batch34StatisticalGovernanceR929.CertificateV2 certificate,
                Batch34StatisticalGovernanceR929.DomainCertificate domainCertificate,
                PolicyEvidence policy,
                StabilityEvidence stability,
                boolean engineeringContractsPassed,
                boolean relationGraphAblationPassed,
                boolean failureAblationPassed,
                boolean atomRegistryPassed,
                boolean inventionGenomeGatePassed,
                boolean sameCertificateUpgradePassed,
                String sourceCommitSha256,
                String evidenceIndexSha256) {
            this.dataset = dataset;
            this.bundle = bundle;
            this.certificate = certificate;
            this.domainCertificate = domainCertificate;
            this.policy = policy;
            this.stability = stability;
            this.engineeringContractsPassed = engineeringContractsPassed;
            this.relationGraphAblationPassed = relationGraphAblationPassed;
            this.failureAblationPassed = failureAblationPassed;
            this.atomRegistryPassed = atomRegistryPassed;
            this.inventionGenomeGatePassed = inventionGenomeGatePassed;
            this.sameCertificateUpgradePassed = sameCertificateUpgradePassed;
            this.sourceCommitSha256 = safe(sourceCommitSha256);
            this.evidenceIndexSha256 = safe(evidenceIndexSha256);
        }

        public String canonical() {
            return (dataset == null ? "NULL" : dataset.canonical()) + "\n"
                    + (bundle == null ? "NULL" : bundle.canonicalWithoutHash()) + "\n"
                    + (certificate == null ? "NULL" : certificate.canonicalWithoutHash()) + "\n"
                    + (domainCertificate == null ? "NULL" : domainCertificate.canonicalWithoutHash())
                    + "\n" + (policy == null ? "NULL" : policy.canonical()) + "\n"
                    + (stability == null ? "NULL" : stability.canonical()) + "\n"
                    + engineeringContractsPassed + "|" + relationGraphAblationPassed + "|"
                    + failureAblationPassed + "|" + atomRegistryPassed + "|"
                    + inventionGenomeGatePassed + "|" + sameCertificateUpgradePassed + "|"
                    + sourceCommitSha256 + "|" + evidenceIndexSha256;
        }
    }

    public static final class GateResult {
        public final boolean structuralReady;
        public final boolean effectReady;
        public final boolean realEligible;
        public final boolean shadowAllowed;
        public final boolean shadowRequiredWhenNotReal;
        public final String code;
        public final List<String> failures;
        public final String evidenceSha256;

        GateResult(
                boolean structuralReady,
                boolean effectReady,
                boolean realEligible,
                boolean shadowAllowed,
                boolean shadowRequiredWhenNotReal,
                List<String> failures,
                String canonical) {
            this.structuralReady = structuralReady;
            this.effectReady = effectReady;
            this.realEligible = realEligible;
            this.shadowAllowed = shadowAllowed;
            this.shadowRequiredWhenNotReal = shadowRequiredWhenNotReal;
            ArrayList<String> copy = new ArrayList<String>();
            if (failures != null) copy.addAll(failures);
            this.failures = Collections.unmodifiableList(copy);
            this.code = copy.isEmpty() ? "R929_BATCH34_PRODUCTION_EVIDENCE_PASS" : copy.get(0);
            this.evidenceSha256 = Batch34EvidenceContractR929.sha256(
                    VERSION + "|" + canonical + "|" + Batch34EvidenceContractR929.join(copy, ","));
        }

        public String canonical() {
            return structuralReady + "|" + effectReady + "|" + realEligible + "|"
                    + shadowAllowed + "|" + shadowRequiredWhenNotReal + "|" + code + "|"
                    + Batch34EvidenceContractR929.join(failures, ",") + "|" + evidenceSha256;
        }
    }

    public static GateResult evaluate(ProductionEvidence evidence, boolean systemOpportunityAvailable) {
        ArrayList<String> structural = new ArrayList<String>();
        ArrayList<String> effect = new ArrayList<String>();
        if (evidence == null) {
            structural.add("R929_PRODUCTION_EVIDENCE_NULL");
            return result(structural, effect, "", systemOpportunityAvailable);
        }

        Batch34EvidenceContractR929.Check dataset =
                Batch34EvidenceContractR929.validateDataset(evidence.dataset, true);
        if (!dataset.passed) structural.addAll(dataset.codes);
        Batch34EvidenceContractR929.Check bundle =
                Batch34EvidenceContractR929.validateBundle(evidence.bundle, true);
        if (!bundle.passed) structural.addAll(bundle.codes);
        Batch34EvidenceContractR929.Check certificate =
                Batch34StatisticalGovernanceR929.validateCertificate(evidence.certificate, true);
        if (!certificate.passed) effect.addAll(certificate.codes);
        Batch34EvidenceContractR929.Check domain =
                Batch34StatisticalGovernanceR929.validateDomainCertificate(
                        evidence.domainCertificate, true);
        if (!domain.passed) effect.addAll(domain.codes);

        if (!evidence.engineeringContractsPassed) structural.add("R929_ENGINEERING_CONTRACTS_NOT_PASSED");
        if (!evidence.relationGraphAblationPassed) effect.add("D08_RELATION_GRAPH_ABLATION_NOT_PASSED");
        if (!evidence.failureAblationPassed) effect.add("C09_FAILURE_ABLATION_NOT_PASSED");
        if (!evidence.atomRegistryPassed) structural.add("C10_C11_ATOM_REGISTRY_NOT_PASSED");
        if (!evidence.inventionGenomeGatePassed) structural.add("C12_INVENTION_GENOME_GATE_NOT_PASSED");
        if (!evidence.sameCertificateUpgradePassed) structural.add("R929_SAME_CERTIFICATE_UPGRADE_NOT_PASSED");
        if (!Batch34EvidenceContractR929.isSha256(evidence.sourceCommitSha256)) {
            structural.add("R929_SOURCE_COMMIT_HASH_INVALID");
        }
        if (!Batch34EvidenceContractR929.isSha256(evidence.evidenceIndexSha256)) {
            structural.add("R929_EVIDENCE_INDEX_HASH_INVALID");
        }
        validatePolicy(evidence.policy, structural, effect);
        validateStability(evidence.stability, structural, effect);
        return result(structural, effect, evidence.canonical(), systemOpportunityAvailable);
    }

    public static GateResult evaluateCurrent(boolean systemOpportunityAvailable) {
        return evaluate(Batch34ProductionEvidenceR929.current(), systemOpportunityAvailable);
    }

    private static void validatePolicy(
            PolicyEvidence p, List<String> structural, List<String> effect) {
        if (p == null) {
            structural.add("D09_POLICY_EVIDENCE_NULL");
            return;
        }
        if (p.systemAvailable < 0
                || p.eligible < 0
                || p.selected < 0
                || p.executionStarted < 0
                || p.frozen < 0
                || p.real < 0
                || p.shadow < 0
                || p.selectedHits < 0) structural.add("D09_NEGATIVE_DENOMINATOR");
        if (!(p.systemAvailable >= p.eligible
                && p.eligible >= p.selected
                && p.selected >= p.executionStarted
                && p.executionStarted >= p.frozen
                && p.selected >= p.real
                && p.selectedHits <= p.selected)) structural.add("D09_DENOMINATOR_ORDER_INVALID");
        if (p.real + p.shadow > p.systemAvailable) structural.add("D09_REAL_SHADOW_DENOMINATOR_OVERFLOW");
        if (!p.denominatorsAudited) structural.add("D09_DENOMINATORS_NOT_AUDITED");
        if (!p.predictabilityIndependent) effect.add("D09_PREDICTABILITY_NOT_INDEPENDENT");
        if (!p.selectorIndependent) effect.add("D09_SELECTOR_NOT_INDEPENDENT");
        if (!p.policyFrozenBeforeSealed) effect.add("D09_POLICY_NOT_FROZEN_BEFORE_SEALED");
        if (!p.sealedEvaluationPassed) effect.add("D09_SEALED_EVALUATION_NOT_PASSED");
        if (!p.quotaEnforcementDisabled) structural.add("D09_QUOTA_ENFORCEMENT_PRESENT");
        if (!p.frequencyDebtDisabled) structural.add("D09_FREQUENCY_DEBT_PRESENT");
        if (!Batch34EvidenceContractR929.isSha256(p.predictabilitySha256)) {
            structural.add("D09_PREDICTABILITY_HASH_INVALID");
        }
        if (!Batch34EvidenceContractR929.isSha256(p.selectorSha256)) structural.add("D09_SELECTOR_HASH_INVALID");
        if (!Batch34EvidenceContractR929.isSha256(p.policySha256)) structural.add("D09_POLICY_HASH_INVALID");
        if (!Batch34EvidenceContractR929.isSha256(p.policyWindowSha256)) {
            structural.add("D09_POLICY_WINDOW_HASH_INVALID");
        }
        if (p.systemAvailable <= 0) {
            effect.add("D09_SYSTEM_AVAILABLE_ZERO");
        } else {
            if (p.selectedCoverage() < MIN_SELECTED_COVERAGE) {
                effect.add("D09_SELECTED_COVERAGE_LT_15_PERCENT");
            }
            if (p.selectedHitRate < TARGET_HIT_RATE) effect.add("D09_SELECTED_HIT_RATE_LT_40_PERCENT");
            if (p.selectedWilsonLower < 0.37d) effect.add("D09_SELECTED_WILSON_LOWER_LT_37_PERCENT");
            if (p.ece > MAX_ECE) effect.add("D09_ECE_GT_4_PERCENT");
            if (p.longestMiss > MAX_MISS) effect.add("D09_LONGEST_MISS_GT_8");
        }
        if (!probability(p.selectedHitRate)
                || !probability(p.selectedWilsonLower)
                || !nonNegativeFinite(p.brier)
                || !nonNegativeFinite(p.ece)) effect.add("D09_POLICY_METRIC_NONFINITE");
    }

    private static void validateStability(
            StabilityEvidence s, List<String> structural, List<String> effect) {
        if (s == null) {
            structural.add("D07_STABILITY_EVIDENCE_NULL");
            return;
        }
        if (s.deterministicSeeds < MIN_SEEDS) effect.add("D07_SEED_COUNT_LT_5");
        if (!probability(s.top1Agreement) || s.top1Agreement < MIN_TOP1_SEED_AGREEMENT) {
            effect.add("D07_TOP1_SEED_AGREEMENT_LT_80_PERCENT");
        }
        if (!Double.isFinite(s.meanKendallTau)
                || s.meanKendallTau < 0.60d
                || s.meanKendallTau > 1.0d) effect.add("D07_KENDALL_STABILITY_FAILED");
        if (!nonNegativeFinite(s.probabilityIntervalWidthP95)
                || s.probabilityIntervalWidthP95 > 0.25d) {
            effect.add("D07_PROBABILITY_INTERVAL_TOO_WIDE");
        }
        if (!nonNegativeFinite(s.rankDisplacementP95) || s.rankDisplacementP95 > 5.0d) {
            effect.add("D07_RANK_DISPLACEMENT_TOO_LARGE");
        }
        if (!Batch34EvidenceContractR929.isSha256(s.uncertaintySha256)) {
            structural.add("D07_UNCERTAINTY_HASH_INVALID");
        }
        if (!Batch34EvidenceContractR929.isSha256(s.rankStabilitySha256)) {
            structural.add("D07_RANK_STABILITY_HASH_INVALID");
        }
        if (!s.sameInputReplayIdentical) structural.add("D07_SAME_INPUT_REPLAY_NOT_IDENTICAL");
        if (!s.bootstrapUsesIssueBlocks) effect.add("D07_BOOTSTRAP_NOT_ISSUE_BLOCKED");
    }

    private static GateResult result(
            List<String> structural,
            List<String> effect,
            String canonical,
            boolean opportunityAvailable) {
        ArrayList<String> all = new ArrayList<String>();
        all.addAll(structural);
        all.addAll(effect);
        boolean structuralReady = structural.isEmpty();
        boolean effectReady = effect.isEmpty();
        boolean real = structuralReady && effectReady;
        return new GateResult(
                structuralReady,
                effectReady,
                real,
                opportunityAvailable,
                opportunityAvailable && !real,
                all,
                canonical);
    }

    private static boolean probability(double value) {
        return Double.isFinite(value) && value >= 0.0d && value <= 1.0d;
    }

    private static boolean nonNegativeFinite(double value) {
        return Double.isFinite(value) && value >= 0.0d;
    }

    private static String finite(double value) {
        return Double.isFinite(value) ? String.format(Locale.US, "%.12g", value) : "NONFINITE";
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
