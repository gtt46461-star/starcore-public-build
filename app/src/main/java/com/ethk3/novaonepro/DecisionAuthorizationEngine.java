package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.security.MessageDigest;
import java.util.Locale;

/**
 * V46 only signer used by DecisionGateEngine.
 *
 * <p>Authorization never recalculates probability, gate, lane, risk coverage or budget. It verifies
 * the already compiled immutable contract and signs the exact values.
 */
public final class DecisionAuthorizationEngine {
    public static final String MODULE_VERSION = V49BuildManifest.AUTH_SCHEMA;
    public static final String MODE_BET = "BET";
    public static final String MODE_SHADOW = "SHADOW";
    public static final String MODE_HOLD = "HOLD";
    public static final String POLICY_UNKNOWN = "UNKNOWN";
    public static final String POLICY_WATCH = "WATCH";
    public static final String POLICY_READY = "READY";
    public static final String POLICY_STRONG = "STRONG";
    public static final String POLICY_SOFT_RISK = "SOFT_RISK";
    public static final String POLICY_HARD_RISK = "HARD_RISK";

    /**
     * V50 signer. It verifies the frozen contract and signs the exact gate outcome; it never
     * promotes SHADOW/HOLD to BET.
     */
    public V50Models.AuthorizedDecision authorizeV50(
            V50CompiledActionContract compiled,
            V50Enums.DecisionAction gateAction,
            V50Enums.RejectCode rejectCode) {
        V50Models.SelectedActionEnvelope envelope = compiled == null ? null : compiled.envelope;
        V50PredictionContract.Result validation =
                compiled == null
                        ? new V50PredictionContract.Result(
                                false,
                                V50Enums.RejectCode.DATA_INCOMPLETE,
                                "V50_COMPILED_CONTRACT_NULL")
                        : compiled.validate();
        V50Enums.DecisionAction action =
                gateAction == null ? V50Enums.DecisionAction.HOLD : gateAction;
        V50Enums.RejectCode reject = rejectCode == null ? V50Enums.RejectCode.NONE : rejectCode;
        if (!validation.valid) {
            action =
                    envelope != null && envelope.action != null && envelope.action.validFixedK3()
                            ? V50Enums.DecisionAction.SHADOW
                            : V50Enums.DecisionAction.HOLD;
            reject = validation.rejectCode;
        }
        if (action == V50Enums.DecisionAction.BET
                && (!V50BuildManifest.ENGINEERING_COMPLETE
                        || !V50BuildManifest.MODEL_PROVEN
                        || !V50BuildManifest.REALTIME_PROVEN
                        || !V50SelectivePolicyArtifact.MODEL_GATE_PASSED)) {
            action = V50Enums.DecisionAction.SHADOW;
            reject = V50Enums.RejectCode.MODEL_ARTIFACT_UNPROVEN;
        }
        if(action==V50Enums.DecisionAction.BET){action=V50Enums.DecisionAction.SHADOW;reject=V50Enums.RejectCode.MODEL_ARTIFACT_UNPROVEN;}
        long authorizedAt = System.currentTimeMillis();
        String contractHash = envelope == null ? "" : envelope.contractHash;
        String decisionId =
                V50ChecksumEngine.sha256(
                        "V50_DECISION|"
                                + contractHash
                                + '|'
                                + (envelope == null || envelope.context == null
                                        ? ""
                                        : envelope.context.targetIssue));
        String signature =
                signV50(
                        decisionId,
                        contractHash,
                        action,
                        reject,
                        authorizedAt,
                        compiled == null ? "" : compiled.checksum);
        return new V50Models.AuthorizedDecision(
                decisionId,
                envelope,
                action,
                signature,
                V50BuildManifest.AUTHORIZATION_OWNER,
                authorizedAt,
                reject);
    }

    public static boolean verifyV50(
            V50Models.AuthorizedDecision decision, V50CompiledActionContract compiled) {
        if (decision == null
                || compiled == null
                || decision.envelope == null
                || !V50BuildManifest.AUTHORIZATION_OWNER.equals(decision.authorizationOwner))
            return false;
        String expected =
                signV50(
                        decision.decisionId,
                        decision.envelope.contractHash,
                        decision.action,
                        decision.rejectCode,
                        decision.authorizationTime,
                        compiled.checksum);
        return expected.equals(decision.authorizationSignature);
    }

    static String signV50(
            String decisionId,
            String contractHash,
            V50Enums.DecisionAction action,
            V50Enums.RejectCode rejectCode,
            long authorizedAt,
            String compiledChecksum) {
        return V50ChecksumEngine.sha256(
                "V50_AUTHORIZATION_V1|"
                        + V50BuildManifest.fingerprint()
                        + '|'
                        + safe(decisionId)
                        + '|'
                        + safe(contractHash)
                        + '|'
                        + action
                        + '|'
                        + rejectCode
                        + '|'
                        + authorizedAt
                        + '|'
                        + safe(compiledChecksum)
                        + '|'
                        + V50BuildManifest.AUTHORIZATION_OWNER);
    }

    public Models.DecisionAuthorization authorize(
            DbStore store, Models.ComboCandidate c, Models.Decision d) {
        Models.DecisionAuthorization a=baseAuthorization();
        if(d!=null){a.decisionId=safe(d.contractDecisionId);a.calibratedProbability=d.finalP;a.gate=d.dynamicGate;}
        reject(a,"LEGACY_AUTHORIZATION_RETIRED",d!=null&&hasLegalK3(d)?MODE_SHADOW:MODE_HOLD);
        if(d!=null)apply(d,a);if(c!=null)apply(c,a);return a;
    }

    public Models.DecisionAuthorization refuse(DbStore store, Models.Decision d, String reason) {
        Models.DecisionAuthorization a = baseAuthorization();
        if (d != null) {
            a.decisionId = safe(d.contractDecisionId);
            a.calibratedProbability = d.finalP;
            a.gate = d.dynamicGate;
            a.edge = a.calibratedProbability - a.gate;
        }
        reject(
                a,
                safe(reason).length() == 0 ? "AUTH_NOT_SELECTED" : reason,
                d != null && hasLegalK3(d) ? MODE_SHADOW : MODE_HOLD);
        if (d != null) {
            apply(d, a);
            persist(store, a, d);
        }
        return a;
    }

    private Models.DecisionAuthorization baseAuthorization() {
        Models.DecisionAuthorization a = new Models.DecisionAuthorization();
        a.authorizedAt = System.currentTimeMillis();
        a.buildId = AppConfig.BUILD_ID;
        a.cohortId = AppConfig.COHORT_ID;
        a.schemaVersion = AppConfig.SCHEMA_VERSION;
        a.pipelineVersion = AppConfig.PIPELINE_VERSION;
        a.calibrationVersion = AppConfig.CALIBRATION_VERSION;
        return a;
    }

    private Models.DecisionAuthorization finish(
            DbStore store,
            Models.ComboCandidate c,
            Models.Decision d,
            Models.DecisionAuthorization a) {
        if (d != null) apply(d, a);
        if (c != null) apply(c, a);
        if (d != null) persist(store, a, d);
        if (c != null)
            PredictionLifecycleTraceEngine.record(
                    store,
                    c,
                    "AUTHORIZATION",
                    a == null ? "NULL" : (a.valid == 1 ? "AUTHORIZED" : safe(a.rejectCode)));
        return a;
    }

    private static void applyCompiledValues(
            Models.ComboCandidate c,
            Models.Decision d,
            CompiledActionContract contract,
            LanePolicyCompiler.Result policy) {
        double p = contract.probability;
        double gate = contract.gate;
        c.finalP = p;
        c.calibratedP = p;
        c.calibratedHitProbability = p;
        c.honestProbability = p;
        c.v43SafeProbability = contract.actionLcb;
        c.v46EventProbability = contract.probability;
        c.v46ActionLcb = contract.actionLcb;
        c.v46ProbabilityUncertainty = contract.uncertainty;
        c.v46ExpectedValue = AppConfig.ODDS * contract.probability - AppConfig.BET_COST;
        c.dynamicGate = gate;
        c.riskAdjustedGate = gate;
        c.gate = gate;
        c.edge = contract.edge;
        c.laneCandidate = contract.lane;

        d.finalP = p;
        d.calibratedP = p;
        d.calibratedHitProbability = p;
        d.honestProbability = p;
        d.v43SafeProbability = contract.actionLcb;
        d.v46EventProbability = contract.probability;
        d.v46ActionLcb = contract.actionLcb;
        d.v46ProbabilityUncertainty = contract.uncertainty;
        d.v46ExpectedValue = AppConfig.ODDS * contract.probability - AppConfig.BET_COST;
        d.v46FinalContractHash = contract.contractHash;
        d.dynamicGate = gate;
        d.riskAdjustedGate = gate;
        d.gate = gate;
        d.edge = contract.edge;
        d.laneCandidate = contract.lane;
        d.betDomain = contract.lane;
        d.finalDecisionBetDomain = contract.lane;
        d.v42AcceptScore = policy.acceptScore;
        d.v42FamilyLcb = policy.familyLcb;
        d.v42SupportVotes = policy.supportVotes;
        d.v42PrimaryReject = policy.primaryReject;
        d.v42SecondaryReject = policy.secondaryReject;
        d.v42TertiaryReject = policy.tertiaryReject;
        d.v42LanePolicyVersion = LanePolicyCompiler.MODULE_VERSION;
        d.v43InvariantStatus = PipelineInvariantGuard.STATUS_LOCKED;
        d.v43PipelineStage = "V49_AUTHORIZATION_VERIFY_EXACT_CONTRACT";
        d.reasonSummary =
                appendReason(
                        d.reasonSummary,
                        "V49授权只验证唯一NumberP→Top3 EventP与不可变合同=" + contract.summary());
    }

    private String validate(
            Models.ComboCandidate c,
            Models.Decision d,
            Models.DecisionAuthorization a,
            CompiledActionContract contract,
            LanePolicyCompiler.Result policy) {
        if (d.contractValid != 1
                || !PredictionContractEngine.STATUS_VALID.equals(
                        PredictionContractEngine.verifyDecision(d)))
            return "CONTRACT_IDENTITY_MISMATCH";
        if (!AppConfig.BUILD_ID.equals(d.buildId)
                || !AppConfig.COHORT_ID.equals(d.cohortId)
                || !AppConfig.SCHEMA_VERSION.equals(d.schemaVersion)
                || !AppConfig.PIPELINE_VERSION.equals(d.pipelineVersion)
                || !AppConfig.CALIBRATION_VERSION.equals(d.calibrationVersion))
            return "VERSION_IDENTITY_MISMATCH";
        if (!hasLegalK3(d)) return "ILLEGAL_SINGLE_POSITION_K3";
        if (!contract.eligible || !policy.eligible) return "COMPILED_CONTRACT_NOT_ELIGIBLE";
        String invariant = PipelineInvariantGuard.verifyBeforeAuthorization(c, contract);
        if (!PipelineInvariantGuard.STATUS_LOCKED.equals(invariant))
            return "V46_" + machineCode(invariant);
        if (c.hardBlockFlag == 1 || safe(c.hardBlockSource).length() > 0 || c.v43HardRisk == 1)
            return "STRUCTURED_HARD_BLOCK";
        if (POLICY_HARD_RISK.equals(a.teacherPolicy)) return "TEACHER_HARD_RISK";
        if (POLICY_HARD_RISK.equals(a.dynamicPolicy)) return "DYNAMIC_HARD_RISK";
        if (POLICY_HARD_RISK.equals(a.adaptivePolicy)) return "ADAPTIVE_HARD_RISK";
        if (a.calibratedProbability <= 0.0d || a.calibratedProbability >= 1.0d)
            return "PROBABILITY_INVALID";
        if (Math.abs(a.calibratedProbability - contract.probability)
                > AppConfig.CONTRACT_PROBABILITY_EPSILON) return "COMPILED_PROBABILITY_MISMATCH";
        if (Math.abs(a.gate - contract.gate) > AppConfig.CONTRACT_PROBABILITY_EPSILON)
            return "COMPILED_GATE_MISMATCH";
        if (!contract.matches(c)) return "COMPILED_CONTRACT_HASH_MISMATCH";
        double minEdge =
                (AppConfig.BET_DOMAIN_QUALIFIED_FLOOR.equals(contract.lane)
                                || AppConfig.BET_DOMAIN_REAL_BET_PROBE.equals(contract.lane))
                        ? -AppConfig.V46_MAX_CONTROLLED_NEGATIVE_EDGE
                        : -AppConfig.CONTRACT_PROBABILITY_EPSILON;
        return a.edge >= minEdge ? "" : "COMPILED_EDGE_LOW";
    }

    private static String laneOf(Models.Decision d) {
        if (d == null) return "";
        String lane = safe(d.betDomain);
        if (lane.length() == 0) lane = safe(d.finalDecisionBetDomain);
        if (lane.length() == 0) lane = safe(d.laneCandidate);
        return LanePolicyCompiler.normalizeLane(lane);
    }

    public static boolean isDecisionAuthorized(Models.Decision d) {
        if (d == null
                || d.authorizationValid != 1
                || !MODE_BET.equals(d.authorizedMode)
                || safe(d.authorizationId).length() == 0) return false;
        if (V50BuildManifest.COHORT_ID.equals(safe(d.cohortId))) {
            return verifyPersistedV50Decision(d);
        }
        if (V51BuildManifest.COHORT_ID.equals(safe(d.cohortId))) {
            return d.contractValid == 1
                    && PredictionContractEngine.STATUS_VALID.equals(
                            PredictionContractEngine.verifyDecision(d))
                    && V51AuthorizationCore.verifyLegacyDecision(d);
        }
        if (d.contractValid != 1
                || !PredictionContractEngine.STATUS_VALID.equals(
                        PredictionContractEngine.verifyDecision(d))) return false;
        return AppConfig.BUILD_ID.equals(d.buildId)
                && AppConfig.COHORT_ID.equals(d.cohortId)
                && AppConfig.SCHEMA_VERSION.equals(d.schemaVersion)
                && AppConfig.PIPELINE_VERSION.equals(d.pipelineVersion)
                && AppConfig.CALIBRATION_VERSION.equals(d.calibrationVersion)
                && Math.abs(d.authorizationProbability - d.finalP)
                        <= AppConfig.CONTRACT_PROBABILITY_EPSILON
                && Math.abs(d.authorizationGate - d.dynamicGate)
                        <= AppConfig.CONTRACT_PROBABILITY_EPSILON
                && safe(d.authorizationId)
                        .equals(
                                sign(
                                        d.contractDecisionId,
                                        d.buildId,
                                        d.cohortId,
                                        d.schemaVersion,
                                        d.pipelineVersion,
                                        d.calibrationVersion,
                                        d.authorizedAt,
                                        d.authorizationProbability,
                                        d.authorizationGate));
    }

    public static boolean isTradeAuthorized(Models.Trade t) {
        if (t == null
                || t.authorizationValid != 1
                || !MODE_BET.equals(t.authorizedMode)
                || safe(t.authorizationId).length() == 0) return false;
        if (V50BuildManifest.COHORT_ID.equals(safe(t.cohortId))) {
            return verifyPersistedV50Trade(t);
        }
        if (V51BuildManifest.COHORT_ID.equals(safe(t.cohortId))) {
            return t.contractValid == 1
                    && PredictionContractEngine.STATUS_VALID.equals(
                            PredictionContractEngine.verifyTrade(t, null))
                    && V51AuthorizationCore.verifyLegacyTrade(t);
        }
        if (t.contractValid != 1
                || !PredictionContractEngine.STATUS_VALID.equals(
                        PredictionContractEngine.verifyTrade(t, null))) return false;
        return AppConfig.BUILD_ID.equals(t.buildId)
                && AppConfig.COHORT_ID.equals(t.cohortId)
                && AppConfig.SCHEMA_VERSION.equals(t.schemaVersion)
                && AppConfig.PIPELINE_VERSION.equals(t.pipelineVersion)
                && AppConfig.CALIBRATION_VERSION.equals(t.calibrationVersion)
                && Math.abs(t.authorizationProbability - t.finalP)
                        <= AppConfig.CONTRACT_PROBABILITY_EPSILON
                && Math.abs(t.authorizationGate - t.dynamicGate)
                        <= AppConfig.CONTRACT_PROBABILITY_EPSILON
                && safe(t.authorizationId)
                        .equals(
                                sign(
                                        t.contractDecisionId,
                                        t.buildId,
                                        t.cohortId,
                                        t.schemaVersion,
                                        t.pipelineVersion,
                                        t.calibrationVersion,
                                        t.authorizedAt,
                                        t.authorizationProbability,
                                        t.authorizationGate));
    }

    static boolean verifyPersistedV50Decision(Models.Decision d) {
        if (d == null
                || !V50BuildManifest.mayLabelFullRelease()
                || d.contractValid != 1
                || !AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(d.decision))
                || !hasLegalK3(d)
                || !V50BuildManifest.BUILD_ID.equals(safe(d.buildId))
                || !V50BuildManifest.COHORT_ID.equals(safe(d.cohortId))
                || !V50BuildManifest.SCHEMA_ID.equals(safe(d.schemaVersion))
                || !V50BuildManifest.STRATEGY_VERSION.equals(safe(d.pipelineVersion))
                || !V50SelectedCalibrationArtifact.ARTIFACT_ID.equals(safe(d.calibrationVersion))
                || safe(d.contractDecisionId).length() == 0
                || safe(d.candidateHash).length() == 0
                || safe(d.v46ContractHash).length() == 0
                || !"NONE".equals(safe(d.authorizationRejectCode))
                || Math.abs(d.authorizationProbability - d.finalP)
                        > AppConfig.CONTRACT_PROBABILITY_EPSILON
                || Math.abs(d.authorizationGate - d.dynamicGate)
                        > AppConfig.CONTRACT_PROBABILITY_EPSILON) return false;
        String expectedDecisionId =
                V50ChecksumEngine.sha256("V50_DECISION|" + d.v46ContractHash + '|' + d.targetIssue);
        if (!expectedDecisionId.equals(safe(d.contractDecisionId))) return false;
        String expectedSignature =
                signV50(
                        d.contractDecisionId,
                        d.v46ContractHash,
                        V50Enums.DecisionAction.BET,
                        V50Enums.RejectCode.NONE,
                        d.authorizedAt,
                        d.candidateHash);
        return expectedSignature.equals(safe(d.authorizationId));
    }

    static boolean verifyPersistedV50Trade(Models.Trade t) {
        if (t == null
                || !V50BuildManifest.mayLabelFullRelease()
                || t.contractValid != 1
                || !AppConfig.ACTION_BET.equals(AppConfig.normalizeDecision(t.action))
                || !AppConfig.isPlayRuleLegal(t.position, AppConfig.FIXED_K, t.n1, t.n2, t.n3)
                || !V50BuildManifest.BUILD_ID.equals(safe(t.buildId))
                || !V50BuildManifest.COHORT_ID.equals(safe(t.cohortId))
                || !V50BuildManifest.SCHEMA_ID.equals(safe(t.schemaVersion))
                || !V50BuildManifest.STRATEGY_VERSION.equals(safe(t.pipelineVersion))
                || !V50SelectedCalibrationArtifact.ARTIFACT_ID.equals(safe(t.calibrationVersion))
                || safe(t.contractDecisionId).length() == 0
                || safe(t.candidateHash).length() == 0
                || safe(t.v46ContractHash).length() == 0
                || !"NONE".equals(safe(t.authorizationRejectCode))
                || Math.abs(t.authorizationProbability - t.finalP)
                        > AppConfig.CONTRACT_PROBABILITY_EPSILON
                || Math.abs(t.authorizationGate - t.dynamicGate)
                        > AppConfig.CONTRACT_PROBABILITY_EPSILON) return false;
        String expectedDecisionId =
                V50ChecksumEngine.sha256("V50_DECISION|" + t.v46ContractHash + '|' + t.targetIssue);
        if (!expectedDecisionId.equals(safe(t.contractDecisionId))) return false;
        String expectedSignature =
                signV50(
                        t.contractDecisionId,
                        t.v46ContractHash,
                        V50Enums.DecisionAction.BET,
                        V50Enums.RejectCode.NONE,
                        t.authorizedAt,
                        t.candidateHash);
        return expectedSignature.equals(safe(t.authorizationId));
    }

    public static void downgradeUnauthorized(Models.Decision d, String reason) {
        if (d == null) return;
        d.decision = AppConfig.DECISION_SHADOW;
        d.action = AppConfig.DECISION_SHADOW;
        d.statScope = AppConfig.STAT_SCOPE_SHADOW;
        d.isRealtimeReal = 0;
        d.authorizationValid = 0;
        d.authorizedMode = MODE_SHADOW;
        d.authorizationRejectCode = safe(reason);
        d.blockCode = safe(reason);
        d.finalDecisionRejectCode = safe(reason);
        d.result = AppConfig.RESULT_WAIT;
        d.settled = 0;
    }

    private static void apply(Models.Decision d, Models.DecisionAuthorization a) {
        d.authorizationId = a.authorizationId;
        d.authorizationValid = a.valid;
        d.authorizedMode = a.authorizedMode;
        d.authorizationRejectCode = a.rejectCode;
        d.authorizedAt = a.authorizedAt;
        d.authorizationProbability = a.calibratedProbability;
        d.authorizationGate = a.gate;
        d.authorizationEdge = a.edge;
        d.teacherPolicy = a.teacherPolicy;
        d.dynamicPolicy = a.dynamicPolicy;
        d.adaptivePolicy = a.adaptivePolicy;
    }

    private static void apply(Models.ComboCandidate c, Models.DecisionAuthorization a) {
        c.authorizationId = a.authorizationId;
        c.authorizationValid = a.valid;
        c.authorizedMode = a.authorizedMode;
        c.authorizationRejectCode = a.rejectCode;
        c.authorizedAt = a.authorizedAt;
        c.authorizationProbability = a.calibratedProbability;
        c.authorizationGate = a.gate;
        c.authorizationEdge = a.edge;
        c.teacherPolicy = a.teacherPolicy;
        c.dynamicPolicy = a.dynamicPolicy;
        c.adaptivePolicy = a.adaptivePolicy;
    }

    private static Models.DecisionAuthorization reject(
            Models.DecisionAuthorization a, String code, String mode) {
        a.valid = 0;
        a.authorizedMode = mode;
        a.rejectCode = safe(code);
        a.authorizationId = "";
        return a;
    }

    private void persist(DbStore store, Models.DecisionAuthorization a, Models.Decision d) {
        if (store == null || a == null) return;
        try {
            ContentValues v = new ContentValues();
            v.put("authorization_id", safe(a.authorizationId));
            v.put("contract_decision_id", safe(a.decisionId));
            v.put("build_id", safe(a.buildId));
            v.put("cohort_id", safe(a.cohortId));
            v.put("schema_version", safe(a.schemaVersion));
            v.put("pipeline_version", safe(a.pipelineVersion));
            v.put("calibration_version", safe(a.calibrationVersion));
            v.put("authorized_mode", safe(a.authorizedMode));
            v.put("authorized_at", a.authorizedAt);
            v.put("valid", a.valid);
            v.put("calibrated_probability", a.calibratedProbability);
            v.put("gate", a.gate);
            v.put("edge", a.edge);
            v.put("family_lcb", a.familyLCB);
            v.put("coverage", a.coverage);
            v.put("third_quality", a.thirdQuality);
            v.put("action_utility", a.actionUtility);
            v.put("uncertainty_penalty", a.uncertaintyPenalty);
            v.put("structure_score", a.structureScore);
            v.put("lane_candidate", safe(a.laneCandidate));
            v.put("teacher_policy", safe(a.teacherPolicy));
            v.put("dynamic_policy", safe(a.dynamicPolicy));
            v.put("adaptive_policy", safe(a.adaptivePolicy));
            v.put("reject_code", safe(a.rejectCode));
            v.put("version", MODULE_VERSION);
            store.writable()
                    .insertWithOnConflict(
                            DbHelper.TABLE_DECISION_AUTHORIZATION,
                            null,
                            v,
                            SQLiteDatabase.CONFLICT_REPLACE);
            store.setState("v48_authorization_version", MODULE_VERSION);
            store.setState("v48_authorization_last_mode", safe(a.authorizedMode));
            store.setState("v48_authorization_last_reject", safe(a.rejectCode));
            store.setState("v48_authorization_last_decision_id", safe(a.decisionId));
            store.setState(
                    "v48_authorization_last_probability", String.valueOf(a.calibratedProbability));
            store.setState("v48_authorization_last_gate", String.valueOf(a.gate));
            store.setState("v48_authorization_last_edge", String.valueOf(a.edge));
            store.setState(
                    "v48_authorization_last_summary",
                    "mode="
                            + safe(a.authorizedMode)
                            + "|reject="
                            + safe(a.rejectCode)
                            + "|P="
                            + a.calibratedProbability
                            + "|Gate="
                            + a.gate
                            + "|Edge="
                            + a.edge
                            + "|Lane="
                            + safe(a.laneCandidate)
                            + "|policy=VERIFY_COMPILED_CONTRACT_ONLY");
        } catch (Throwable ignore) {
        }
    }

    private static String teacherPolicy(Models.Decision d) {
        return combinePolicies(
                new String[] {
                    d.teacherOrdinaryBetPolicy,
                    d.teacherMatrixOrdinaryBetPolicy,
                    d.teacherMatrixStatus,
                    d.teacherStatus,
                    d.teacherNumberStatus
                });
    }

    private static String dynamicPolicy(Models.Decision d) {
        return combinePolicies(
                new String[] {
                    d.dynamicClusterStatus,
                    d.v20Status,
                    d.dynamicStatus,
                    d.dynamicCauseStatus,
                    d.finalDecisionV20Policy
                });
    }

    private static String adaptivePolicy(Models.Decision d) {
        return combinePolicies(
                new String[] {
                    d.adaptiveJudgeAction,
                    d.adaptiveStatus,
                    d.adaptiveJudgeStatus,
                    d.ruleHealthStatus,
                    d.shadowPromotionStatus
                });
    }

    private static String combinePolicies(String[] values) {
        boolean any = false, ready = false, strong = false, soft = false;
        if (values != null)
            for (int i = 0; i < values.length; i++) {
                if (safe(values[i]).trim().length() == 0) continue;
                any = true;
                String p = resolvePolicy(values[i]);
                if (POLICY_HARD_RISK.equals(p)) return POLICY_HARD_RISK;
                if (POLICY_SOFT_RISK.equals(p)) soft = true;
                if (POLICY_STRONG.equals(p)) strong = true;
                if (POLICY_READY.equals(p)) ready = true;
            }
        if (strong) return POLICY_STRONG;
        if (ready) return POLICY_READY;
        if (soft) return POLICY_SOFT_RISK;
        return any ? POLICY_WATCH : POLICY_UNKNOWN;
    }

    static String resolvePolicy(String s) {
        String x = safe(s).trim().toUpperCase(Locale.US);
        if (x.length() == 0) return POLICY_UNKNOWN;
        if (oneOf(
                x,
                new String[] {
                    "HARD_RISK",
                    "DYNAMIC_HARD_RISK",
                    "V20_HARD_RISK",
                    "V20_DYNAMIC_HARD_RISK",
                    "DYNAMIC_CAUSAL_HARD_RISK",
                    "RULE_HEALTH_HARD_RISK",
                    "ADAPTIVE_BLOCK",
                    "ADAPTIVE_FREEZE",
                    "FREEZE",
                    "REJECT",
                    "BLOCK",
                    "BLOCKED",
                    "HARD_BLOCK",
                    "HARD_FREEZE",
                    "FAKE",
                    "FALSE_STRONG_FREEZE",
                    "TEACHER_FREEZE",
                    "DYNAMIC_FREEZE",
                    "SHADOW_HEALTH_HARD_BLOCK",
                    "DWC_MATURE_HARD_BLOCK"
                })) return POLICY_HARD_RISK;
        if (oneOf(
                x,
                new String[] {
                    "SOFT_RISK",
                    "RISK",
                    "DYNAMIC_RISK",
                    "V20_RISK",
                    "TEACHER_RISK",
                    "TEACHER_MATRIX_RISK",
                    "TM_RISK",
                    "RULE_RISK",
                    "HEALTH_RISK",
                    "DWC_SOFT_RISK",
                    "CLUSTER_RISK",
                    "DYNAMIC_CLUSTER_RISK",
                    "DOWNGRADE"
                })) return POLICY_SOFT_RISK;
        if (oneOf(
                x,
                new String[] {
                    "STRONG",
                    "HIERARCHICAL_HISTORY_STRONG",
                    "TEACHER_STRONG",
                    "TEACHER_STRONG_READY",
                    "TM_STRONG",
                    "TM_ELITE",
                    "CLUSTER_STRONG",
                    "DWC_STRONG",
                    "DYNAMIC_STRONG",
                    "V20_STRONG",
                    "ADAPTIVE_STRONG",
                    "RULE_STRONG"
                })) return POLICY_STRONG;
        if (oneOf(
                x,
                new String[] {
                    "READY",
                    "HIERARCHICAL_HISTORY_READY",
                    "HIERARCHICAL_OOS_READY",
                    "OOS_MATURITY_READY",
                    "ALLOW",
                    "ALLOW_REVIEW",
                    "ALLOW_DECISION_GATE",
                    "ALLOW_TO_GATE",
                    "BET_READY",
                    "PASS",
                    "CLEAN_PASS",
                    "HEALTHY",
                    "SAFE",
                    "STABLE",
                    "NORMAL",
                    "TM_READY",
                    "TM_APPLIED",
                    "TM_OK",
                    "TEACHER_READY",
                    "TEACHER_MATRIX_READY",
                    "CLUSTER_READY",
                    "DWC_READY",
                    "DYNAMIC_READY",
                    "DYNAMIC_CAUSAL_READY",
                    "DYNAMIC_PASS",
                    "V20_READY",
                    "V20_CAUSE_PASS",
                    "ADAPTIVE_READY",
                    "RULE_PASS",
                    "RULE_READY",
                    "SHADOW_PASS",
                    "SHADOW_PROMOTE",
                    "HISTORICAL_PRIOR_READY"
                })) return POLICY_READY;
        return POLICY_WATCH;
    }

    private static boolean oneOf(String x, String[] values) {
        if (x == null || values == null) return false;
        for (int i = 0; i < values.length; i++) if (x.equals(values[i])) return true;
        return false;
    }

    private static boolean hasLegalK3(Models.Decision d) {
        return d != null
                && AppConfig.isPlayRuleLegal(d.position, AppConfig.FIXED_K, d.n1, d.n2, d.n3);
    }

    private static String machineCode(String status) {
        String s = safe(status).toUpperCase(Locale.US);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) out.append(ch);
            else out.append('_');
        }
        return out.toString();
    }

    static String sign(
            String decisionId,
            String buildId,
            String cohortId,
            String schemaVersion,
            String pipeline,
            String calibration,
            long at,
            double p,
            double gate) {
        String x =
                safe(decisionId)
                        + "|"
                        + safe(buildId)
                        + "|"
                        + safe(cohortId)
                        + "|"
                        + safe(schemaVersion)
                        + "|"
                        + safe(pipeline)
                        + "|"
                        + safe(calibration)
                        + "|"
                        + at
                        + "|"
                        + p
                        + "|"
                        + gate;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(x.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < 12; i++) out.append(String.format(Locale.US, "%02x", b[i] & 255));
            return out.toString();
        } catch (Throwable e) {
            return Integer.toHexString(x.hashCode());
        }
    }

    private static String appendReason(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0 || a.indexOf(b) >= 0) return safe(a);
        return a + "｜" + b;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // V51_APEX_AUTH_ENTRY
    public V51Models.AuthorizedDecision authorizeV51(
            V51CompiledActionContract compiled,
            V51Enums.DecisionAction action,
            V51Enums.RejectCode reject) {
        throw new UnsupportedOperationException("LEGACY_AUTHORIZATION_RETIRED");
    }

    public static boolean verifyV51(
            V51Models.AuthorizedDecision decision, V51CompiledActionContract compiled) {
        return V51AuthorizationCore.verify(decision, compiled);
    }

    // V57_B12_V51_FROZEN_SIGNER
    V51Models.AuthorizedDecision authorizeV51FrozenCP12(
            DecisionGateEngine.AuthorizationScopeCP12 scope,
            V51CompiledActionContract compiled,
            V51Enums.DecisionAction gateAction,
            V51Enums.RejectCode gateReject) {
        if(scope==null||!scope.matches(compiled))throw new IllegalStateException("AUTHORIZATION_SCOPE_MISMATCH");
        V51Enums.DecisionAction action =
                gateAction == null ? V51Enums.DecisionAction.HOLD : gateAction;
        V51Enums.RejectCode reject =
                gateReject == null ? V51Enums.RejectCode.DATA_INCOMPLETE : gateReject;
        if (compiled == null
                || compiled.selected == null
                || compiled.selected.context == null
                || compiled.selected.action == null) {
            action = V51Enums.DecisionAction.HOLD;
            reject = V51Enums.RejectCode.DATA_INCOMPLETE;
        }
        if (action == V51Enums.DecisionAction.BET) {
            V51DecisionGateCore.Result check = V51DecisionGateCore.validate(null, compiled);
            if (check == null || !check.realEligible) {
                action = V51Enums.DecisionAction.SHADOW;
                reject =
                        check == null ? V51Enums.RejectCode.AUTHORIZATION_FAILED : check.rejectCode;
            }
        }
        return V51AuthorizationCore.authorize(compiled, action, reject);
    }

    public static boolean v57V51SignerOwnsBetChoice() {
        return false;
    }
    /** Retired compatibility entry; final signer requires an unforgeable Gate scope. */
    @Deprecated public V51Models.AuthorizedDecision authorizeV51Frozen(V51CompiledActionContract compiled,V51Enums.DecisionAction action,V51Enums.RejectCode reject){
        throw new UnsupportedOperationException("LEGACY_AUTHORIZATION_RETIRED");
    }
}
