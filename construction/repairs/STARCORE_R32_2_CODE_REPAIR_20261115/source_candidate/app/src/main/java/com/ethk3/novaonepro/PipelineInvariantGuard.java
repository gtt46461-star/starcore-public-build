package com.ethk3.novaonepro;

import java.security.MessageDigest;

/**
 * V45 final prediction invariant guard.
 *
 * <p>The lock is created only after every probability-affecting module has finished and immediately
 * before LanePolicyCompiler compiles the single action contract.
 */
public final class PipelineInvariantGuard {
    public static final String MODULE_VERSION = "V46_EVENTP_ACTION_LCB_INVARIANT_20260721_46072130";
    public static final String STATUS_LOCKED = "OK";
    public static final String STATUS_PENDING = "WATCH:FINAL_LOCK_PENDING";

    private PipelineInvariantGuard() {}

    public static String auditCandidate(Models.ComboCandidate c, String expectedTarget) {
        if (c == null) return "BLOCK:CANDIDATE_NULL";
        if (!c.isValidK3()) return "BLOCK:INVALID_K3";
        if (c.position < 1 || c.position > 10) return "BLOCK:POSITION";
        if (expectedTarget != null
                && expectedTarget.length() > 0
                && !expectedTarget.equals(safe(c.targetIssue))) return "BLOCK:TARGET_MISMATCH";
        if (c.snapshotMaxIssue != null
                && c.targetIssue != null
                && c.snapshotMaxIssue.length() > 0
                && c.targetIssue.length() > 0
                && compareIssue(c.snapshotMaxIssue, c.targetIssue) >= 0) return "BLOCK:FUTURE_LEAK";
        PredictionEnvelope envelope = PredictionEnvelope.readOnly(c);
        double p = envelope == null ? 0.0d : envelope.canonicalProbability;
        if (Double.isNaN(p) || Double.isInfinite(p) || p <= 0.0d || p >= 1.0d)
            return "BLOCK:PROBABILITY_RANGE";
        if (c.contractValid != 1) return "BLOCK:CONTRACT_INVALID";
        if (!PredictionContractEngine.STATUS_VALID.equals(
                PredictionContractEngine.verifyCandidate(c))) return "BLOCK:CONTRACT_IDENTITY";
        if (c.v43EvidenceGroupCount < AppConfig.V43_MIN_EVIDENCE_GROUPS)
            return "WATCH:EVIDENCE_INCOMPLETE";
        if (c.v43HardRisk == 1 || c.hardBlockFlag == 1 || safe(c.hardBlockSource).length() > 0)
            return "BLOCK:HARD_RISK";
        return "OK";
    }

    /** Compatibility entry: now means final pre-lane canonical lock. */
    public static String lockProbability(Models.ComboCandidate c) {
        return lockFinalPrediction(c, "FINAL_PRE_LANE");
    }

    public static String lockFinalPrediction(Models.ComboCandidate c, String stage) {
        if (c == null) return "BLOCK:CANDIDATE_NULL";
        PredictionEnvelope envelope = PredictionEnvelope.finalizeAndApply(c);
        if (envelope == null) return "BLOCK:ENVELOPE_NULL";
        String audit = auditCandidate(c, c.targetIssue);
        if (audit.startsWith("BLOCK:")) {
            c.v43InvariantStatus = audit;
            c.v43PipelineStage = safe(stage) + "_BLOCKED";
            return audit;
        }
        c.v43ProbabilityLock = envelope.canonicalProbability;
        c.v43ProbabilityLockHash = hash(lockPayload(c, envelope));
        c.v46ContractHash = envelope.envelopeHash;
        c.v43InvariantStatus = STATUS_LOCKED;
        c.v43PipelineStage =
                safe(stage).length() == 0 ? "V46_FINAL_PREDICTION_LOCKED" : safe(stage);
        return STATUS_LOCKED;
    }

    /**
     * V44 copies sometimes lost lock fields. V45 does not silently accept that state: the final
     * pre-lane boundary reconstructs the canonical envelope once, records the recovery stage, then
     * all later checks are strict and may never re-lock.
     */
    public static String ensureFinalLock(Models.ComboCandidate c, String stage) {
        if (c == null) return "BLOCK:CANDIDATE_NULL";
        if (safe(c.v43ProbabilityLockHash).length() == 0) {
            String status = lockFinalPrediction(c, "V46_RECOVERED_" + safe(stage));
            if (STATUS_LOCKED.equals(status)) {
                c.reasonSummary =
                        append(
                                c.reasonSummary,
                                "V46在最终Lane边界重建兼容copy丢失的EventP/ActionLCB锁；后续禁止再次重锁");
            }
            return status;
        }
        return verifyProbabilityLock(c);
    }

    public static String verifyProbabilityLock(Models.ComboCandidate c) {
        if (c == null) return "BLOCK:CANDIDATE_NULL";
        if (safe(c.v43ProbabilityLockHash).length() == 0) return "BLOCK:LOCK_MISSING";
        PredictionEnvelope envelope = PredictionEnvelope.readOnly(c);
        if (envelope == null) return "BLOCK:ENVELOPE_NULL";
        double p = envelope.canonicalProbability;
        if (Math.abs(p - c.v43ProbabilityLock) > 0.000000001d) return "BLOCK:PROBABILITY_MUTATED";
        if (Math.abs(envelope.actionLcb - c.v46ActionLcb) > 0.000000001d)
            return "BLOCK:ACTION_LCB_MUTATED";
        if (Math.abs(envelope.uncertainty - c.v46ProbabilityUncertainty) > 0.000000001d)
            return "BLOCK:UNCERTAINTY_MUTATED";
        String now = hash(lockPayload(c, envelope));
        if (!now.equals(c.v43ProbabilityLockHash)) return "BLOCK:LOCK_HASH_MISMATCH";
        return STATUS_LOCKED;
    }

    public static String verifyBeforeAuthorization(
            Models.ComboCandidate c, CompiledActionContract contract) {
        String lock = verifyProbabilityLock(c);
        if (!STATUS_LOCKED.equals(lock)) return lock;
        if (contract == null) return "BLOCK:COMPILED_CONTRACT_MISSING";
        if (!contract.matches(c)) return "BLOCK:COMPILED_CONTRACT_MISMATCH";
        return STATUS_LOCKED;
    }

    public static Models.Decision enforceDecision(Models.Decision d) {
        if (d == null) return null;
        d.v43InvariantStatus =
                safe(d.v43InvariantStatus).length() == 0 ? STATUS_LOCKED : d.v43InvariantStatus;
        if (AppConfig.DECISION_BET.equals(d.decision)) {
            if (d.v43HardRisk == 1
                    || d.hardBlockFlag == 1
                    || safe(d.hardBlockSource).length() > 0) {
                DecisionAuthorizationEngine.downgradeUnauthorized(d, "V46_HARD_RISK");
                d.v43InvariantStatus = "BLOCK:HARD_RISK";
                d.reasonSummary = append(d.reasonSummary, "V46最终合同：硬风险候选不得BET，已降级SHADOW");
            } else if (d.v43EvidenceGroupCount < AppConfig.V43_MIN_EVIDENCE_GROUPS) {
                DecisionAuthorizationEngine.downgradeUnauthorized(d, "V46_EVIDENCE_INCOMPLETE");
                d.v43InvariantStatus = "BLOCK:EVIDENCE_INCOMPLETE";
                d.reasonSummary = append(d.reasonSummary, "V46证据组不足，已降级SHADOW");
            }
        }
        return d;
    }

    private static String lockPayload(Models.ComboCandidate c, PredictionEnvelope envelope) {
        double p = envelope == null ? 0.0d : envelope.canonicalProbability;
        double lcb = envelope == null ? 0.0d : envelope.actionLcb;
        return identity(c)
                + "|EventP="
                + number(p)
                + "|ActionLCB="
                + number(lcb)
                + "|U="
                + number(envelope == null ? 0.0d : envelope.uncertainty)
                + "|candidateHash="
                + safe(c.candidateHash)
                + "|snapshot="
                + safe(c.snapshotMaxIssue)
                + "|trained="
                + safe(c.trainedUntilIssue)
                + "|featureSchema="
                + safe(c.featureSchemaVersion)
                + "|ranking="
                + safe(c.rankingVersion)
                + "|calibration="
                + safe(c.calibrationVersion)
                + "|pipeline="
                + safe(c.pipelineVersion)
                + "|build="
                + safe(c.buildId)
                + "|cohort="
                + safe(c.cohortId)
                + "|schema="
                + safe(c.schemaVersion);
    }

    private static String identity(Models.ComboCandidate c) {
        return safe(c.targetIssue)
                + "|"
                + c.position
                + "|"
                + c.n1
                + "|"
                + c.n2
                + "|"
                + c.n3
                + "|"
                + c.k3Mask;
    }

    private static int compareIssue(String a, String b) {
        try {
            java.math.BigInteger x = new java.math.BigInteger(a.replaceAll("[^0-9]", ""));
            java.math.BigInteger y = new java.math.BigInteger(b.replaceAll("[^0-9]", ""));
            return x.compareTo(y);
        } catch (Throwable e) {
            return safe(a).compareTo(safe(b));
        }
    }

    private static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(safe(raw).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < d.length; i++) {
                String h = Integer.toHexString(d[i] & 0xff);
                if (h.length() == 1) sb.append('0');
                sb.append(h);
            }
            return sb.toString();
        } catch (Throwable e) {
            return Integer.toHexString(safe(raw).hashCode());
        }
    }

    private static String number(double v) {
        return String.valueOf(Math.round(v * 1000000000.0d));
    }

    private static String append(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0 || a.indexOf(b) >= 0) return safe(a);
        return a + "；" + b;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public static final String B12_TYPED_INVARIANT_VERSION = "V57_B12_TYPED_INVARIANTS_20260808";
    public static final String BET_OWNER = "DecisionGateEngine";
    public static final String SETTLEMENT_OWNER = "SettlementEngine";

    public enum CoreCanonicalStatus {
        PASS,
        FAIL
    }

    public enum FullEvidenceStatus {
        PENDING,
        PASS,
        FAIL
    }

    /** Two-level R811 evidence truth: deferred work can never turn a valid core into a failure. */
    public static final class CanonicalEvidenceStatus {
        public final CoreCanonicalStatus core;
        public final FullEvidenceStatus full;
        public final String targetIssue, detail;

        CanonicalEvidenceStatus(
                String target,
                CoreCanonicalStatus core,
                FullEvidenceStatus full,
                String detail) {
            targetIssue = safe(target);
            this.core = core;
            this.full = full;
            this.detail = safe(detail);
        }
    }

    public static CanonicalEvidenceStatus auditCanonicalEvidence(DbStore store, String target) {
        V51Repository.B12InvariantAudit audit = V51Repository.auditB12(store, target);
        boolean corePass =
                audit.issue == 1
                        && audit.champion == 1
                        && audit.selected == 1
                        && audit.auth == 1
                        && !audit.duplicateTrade
                        && !audit.duplicateSettlement
                        && audit.realShadowSeparated;
        CoreCanonicalStatus core =
                corePass ? CoreCanonicalStatus.PASS : CoreCanonicalStatus.FAIL;
        String deferred = DeferredEvidenceWriterR36.statusForTarget(store, target);
        FullEvidenceStatus full;
        if (!corePass) full = FullEvidenceStatus.FAIL;
        else if (audit.number == 100
                && audit.compatAction == 10
                && audit.fullAction == 1200
                && audit.funnel == 10) full = FullEvidenceStatus.PASS;
        else if (DeferredEvidenceWriterR36.FAILED_TERMINAL.equals(deferred)
                || deferred.startsWith("READ_ERROR")) full = FullEvidenceStatus.FAIL;
        else full = FullEvidenceStatus.PENDING;
        return new CanonicalEvidenceStatus(
                target,
                core,
                full,
                audit.detail + "|deferred=" + deferred + "|CORE=" + core + "|FULL=" + full);
    }

    public static String auditV51(DbStore store, V51RuntimeResult result) {
        if (result == null || !result.success) return "FAIL:RUNTIME";
        if (result.actions == null || result.actions.length != 1200) {
            return "FAIL:ACTION_SPACE:" + (result.actions == null ? -1 : result.actions.length);
        }
        if (result.selected == null || result.authorized == null || result.contract == null) {
            return "FAIL:CONTRACT_INCOMPLETE";
        }
        if (result.selected.action == null
                || result.selected.action.position < 1
                || result.selected.action.position > 10) {
            return "FAIL:FIXED_POSITION";
        }
        V51Models.PositionAction a = result.selected.action;
        if (a.n1 == a.n2 || a.n1 == a.n3 || a.n2 == a.n3) {
            return "FAIL:K3_UNIQUE";
        }
        if (result.authorized.action == V51Enums.DecisionAction.BET) {
            V51DecisionGateCore.Result gate = V51DecisionGateCore.validate(store, result.contract);
            if (!gate.realEligible) {
                return "FAIL:UNAUTHORIZED_BET:" + gate.detail;
            }
        }
        if (store != null) {
            String fp = store.getState("v57_b10_champion_fingerprint", "");
            String sum = store.getState("v57_b10_champion_checksum", "");
            if (fp.length() > 0 && !fp.equals(a.fingerprint)) {
                return "FAIL:CHAMPION_FINGERPRINT";
            }
            if (sum.length() > 0 && !sum.equals(a.checksum)) {
                return "FAIL:CHAMPION_CHECKSUM";
            }
            ShadowObservationSettlementEngine.V57ShadowReport shadow =
                    new ShadowObservationSettlementEngine.V57ShadowReport(
                            result.context == null ? "" : result.context.targetIssue,
                            0,
                            0,
                            0,
                            0,
                            0);
            if (!shadow.valid()) return "FAIL:SHADOW_REAL_PNL";
        }
        return "PASS";
    }

public static CanonicalEvidenceStatus auditCanonicalEvidenceSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String target) {
        V51Repository.B12InvariantAudit audit = V51Repository.auditB12SnapshotCP4(report, store, target);
        boolean corePass =
                audit.issue == 1
                        && audit.champion == 1
                        && audit.selected == 1
                        && audit.auth == 1
                        && !audit.duplicateTrade
                        && !audit.duplicateSettlement
                        && audit.realShadowSeparated;
        CoreCanonicalStatus core =
                corePass ? CoreCanonicalStatus.PASS : CoreCanonicalStatus.FAIL;
        String deferred = DeferredEvidenceWriterR36.statusForTargetSnapshotCP4(report, store, target);
        FullEvidenceStatus full;
        if (!corePass) full = FullEvidenceStatus.FAIL;
        else if (audit.number == 100
                && audit.compatAction == 10
                && audit.fullAction == 1200
                && audit.funnel == 10) full = FullEvidenceStatus.PASS;
        else if (DeferredEvidenceWriterR36.FAILED_TERMINAL.equals(deferred)
                || deferred.startsWith("READ_ERROR")) full = FullEvidenceStatus.FAIL;
        else full = FullEvidenceStatus.PENDING;
        return new CanonicalEvidenceStatus(
                target,
                core,
                full,
                audit.detail + "|deferred=" + deferred + "|CORE=" + core + "|FULL=" + full);
    }
}
