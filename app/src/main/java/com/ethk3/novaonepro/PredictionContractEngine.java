package com.ethk3.novaonepro;

import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

/**
 * V40 immutable prediction identity contract. The contract is created before the target issue is
 * drawn and must survive unchanged through Candidate, Probability, Gate, Decision, Trade and
 * Settlement.
 */
public final class PredictionContractEngine {
    public static final String MODULE_VERSION = AppConfig.PREDICTION_CONTRACT_VERSION;
    public static final String STATUS_VALID = "CONTRACT_VALID";
    public static final String STATUS_MISMATCH = "CONTRACT_IDENTITY_MISMATCH";

    private PredictionContractEngine() {}

    public static void stampCandidate(Models.ComboCandidate c, List<Models.Draw> history) {
        if (c == null) return;
        normalizeK3(c);
        String snapshot = latestIssue(history);
        if (snapshot.length() == 0) snapshot = safe(c.baseIssue);
        String trained =
                firstNonEmpty(
                        c.evidenceAsOfIssue, c.snapshotAsOfIssue, c.teacherAsOfIssue, snapshot);

        HierarchicalPosteriorEngine.FamilyKeys keys = HierarchicalPosteriorEngine.deriveKeys(c);
        c.coarseFamilyKey = keys.coarse;
        c.midFamilyKey = keys.mid;
        c.fineFamilyKey = keys.fine;
        c.positionFamilyKey = keys.position;

        c.snapshotMaxIssue = snapshot;
        c.trainedUntilIssue = trained;
        c.buildId = AppConfig.BUILD_ID;
        c.cohortId = AppConfig.COHORT_ID;
        c.schemaVersion = AppConfig.SCHEMA_VERSION;
        c.featureSchemaVersion = AppConfig.FEATURE_SCHEMA_VERSION;
        c.rankingVersion = AppConfig.RANKING_VERSION;
        c.calibrationVersion = AppConfig.CALIBRATION_VERSION;
        c.gatePolicyVersion = AppConfig.GATE_POLICY_VERSION;
        c.pipelineVersion = AppConfig.PIPELINE_VERSION;
        c.candidateHash =
                candidateHash(
                        c.baseIssue,
                        c.targetIssue,
                        c.position,
                        c.n1,
                        c.n2,
                        c.n3,
                        c.snapshotMaxIssue,
                        c.buildId,
                        c.cohortId,
                        c.schemaVersion,
                        c.featureSchemaVersion,
                        c.rankingVersion);
        c.contractDecisionId =
                decisionId(
                        c.baseIssue,
                        c.targetIssue,
                        c.candidateHash,
                        c.buildId,
                        c.cohortId,
                        c.schemaVersion,
                        c.pipelineVersion,
                        c.calibrationVersion,
                        c.gatePolicyVersion);
        String status = verifyCandidate(c);
        c.contractStatus = status;
        c.contractValid = STATUS_VALID.equals(status) ? 1 : 0;
    }

    public static void stampDecision(Models.ComboCandidate c, Models.Decision d) {
        if (d == null) return;
        if (c != null) Models.copySharedPublicFieldsV20(c, d);
        normalizeK3(d);
        if (V50BuildManifest.COHORT_ID.equals(safe(d.cohortId))) {
            String status = verifyV50Decision(d);
            d.contractStatus = status;
            d.contractValid = STATUS_VALID.equals(status) ? 1 : 0;
            return;
        }
        if (V51BuildManifest.COHORT_ID.equals(safe(d.cohortId))) {
            String status = verifyV51Decision(d);
            d.contractStatus = status;
            d.contractValid = STATUS_VALID.equals(status) ? 1 : 0;
            return;
        }
        if (safe(d.buildId).length() == 0) d.buildId = AppConfig.BUILD_ID;
        if (safe(d.cohortId).length() == 0) d.cohortId = AppConfig.COHORT_ID;
        if (safe(d.schemaVersion).length() == 0) d.schemaVersion = AppConfig.SCHEMA_VERSION;
        if (d.candidateHash == null || d.candidateHash.length() == 0) {
            d.candidateHash =
                    candidateHash(
                            d.baseIssue,
                            d.targetIssue,
                            d.position,
                            d.n1,
                            d.n2,
                            d.n3,
                            d.snapshotMaxIssue,
                            d.buildId,
                            d.cohortId,
                            d.schemaVersion,
                            d.featureSchemaVersion,
                            d.rankingVersion);
        }
        if (d.contractDecisionId == null || d.contractDecisionId.length() == 0) {
            d.contractDecisionId =
                    decisionId(
                            d.baseIssue,
                            d.targetIssue,
                            d.candidateHash,
                            d.buildId,
                            d.cohortId,
                            d.schemaVersion,
                            d.pipelineVersion,
                            d.calibrationVersion,
                            d.gatePolicyVersion);
        }
        String status = verifyDecision(d);
        d.contractStatus = status;
        d.contractValid = STATUS_VALID.equals(status) ? 1 : 0;
    }

    public static void stampTrade(Models.Decision d, Models.Trade t) {
        if (t == null) return;
        if (d != null) Models.copySharedPublicFieldsV20(d, t);
        normalizeK3(t);
        if (V50BuildManifest.COHORT_ID.equals(safe(t.cohortId))) {
            String status = verifyV50Trade(t, d);
            t.contractStatus = status;
            t.contractValid = STATUS_VALID.equals(status) ? 1 : 0;
            return;
        }
        if (V51BuildManifest.COHORT_ID.equals(safe(t.cohortId))) {
            String status = verifyV51Trade(t, d);
            t.contractStatus = status;
            t.contractValid = STATUS_VALID.equals(status) ? 1 : 0;
            return;
        }
        String status = verifyTrade(t, d);
        t.contractStatus = status;
        t.contractValid = STATUS_VALID.equals(status) ? 1 : 0;
    }

    public static String verifyCandidate(Models.ComboCandidate c) {
        if (c == null) return STATUS_MISMATCH + ":CANDIDATE_NULL";
        return verifyCore(
                c.baseIssue,
                c.targetIssue,
                c.position,
                c.n1,
                c.n2,
                c.n3,
                c.candidateHash,
                c.contractDecisionId,
                c.snapshotMaxIssue,
                c.trainedUntilIssue,
                c.buildId,
                c.cohortId,
                c.schemaVersion,
                c.featureSchemaVersion,
                c.rankingVersion,
                c.calibrationVersion,
                c.gatePolicyVersion,
                c.pipelineVersion);
    }

    public static String verifyDecision(Models.Decision d) {
        if (d == null) return STATUS_MISMATCH + ":DECISION_NULL";
        if (V50BuildManifest.COHORT_ID.equals(safe(d.cohortId))) {
            return verifyV50Decision(d);
        }
        if (V51BuildManifest.COHORT_ID.equals(safe(d.cohortId))) {
            return verifyV51Decision(d);
        }
        return verifyCore(
                d.baseIssue,
                d.targetIssue,
                d.position,
                d.n1,
                d.n2,
                d.n3,
                d.candidateHash,
                d.contractDecisionId,
                d.snapshotMaxIssue,
                d.trainedUntilIssue,
                d.buildId,
                d.cohortId,
                d.schemaVersion,
                d.featureSchemaVersion,
                d.rankingVersion,
                d.calibrationVersion,
                d.gatePolicyVersion,
                d.pipelineVersion);
    }

    public static String verifyTrade(Models.Trade t, Models.Decision d) {
        if (t == null) return STATUS_MISMATCH + ":TRADE_NULL";
        if (V50BuildManifest.COHORT_ID.equals(safe(t.cohortId))) {
            return verifyV50Trade(t, d);
        }
        if (V51BuildManifest.COHORT_ID.equals(safe(t.cohortId))) {
            return verifyV51Trade(t, d);
        }
        String core =
                verifyCore(
                        t.baseIssue,
                        t.targetIssue,
                        t.position,
                        t.n1,
                        t.n2,
                        t.n3,
                        t.candidateHash,
                        t.contractDecisionId,
                        t.snapshotMaxIssue,
                        t.trainedUntilIssue,
                        t.buildId,
                        t.cohortId,
                        t.schemaVersion,
                        t.featureSchemaVersion,
                        t.rankingVersion,
                        t.calibrationVersion,
                        t.gatePolicyVersion,
                        t.pipelineVersion);
        if (!STATUS_VALID.equals(core)) return core;
        if (d != null) {
            if (!same(t.contractDecisionId, d.contractDecisionId))
                return STATUS_MISMATCH + ":DECISION_ID";
            if (!same(t.candidateHash, d.candidateHash)) return STATUS_MISMATCH + ":CANDIDATE_HASH";
            if (!same(t.targetIssue, d.targetIssue)
                    || t.position != d.position
                    || t.n1 != d.n1
                    || t.n2 != d.n2
                    || t.n3 != d.n3) {
                return STATUS_MISMATCH + ":DECISION_TRADE_CONTENT";
            }
            if (!same(t.authorizationId, d.authorizationId) || t.authorizationValid != 1) {
                return STATUS_MISMATCH + ":AUTHORIZATION";
            }
        }
        return STATUS_VALID;
    }

    private static String verifyV51Decision(Models.Decision d) {
        if (d == null) return STATUS_MISMATCH + ":V51_DECISION_NULL";
        return verifyV51Core(
                d.baseIssue,
                d.targetIssue,
                d.position,
                d.n1,
                d.n2,
                d.n3,
                d.candidateHash,
                d.contractDecisionId,
                d.snapshotMaxIssue,
                d.buildId,
                d.cohortId,
                d.schemaVersion,
                d.featureSchemaVersion,
                d.rankingVersion,
                d.calibrationVersion,
                d.gatePolicyVersion,
                d.pipelineVersion,
                d.preDrawReasonFingerprint);
    }

    private static String verifyV51Trade(Models.Trade t, Models.Decision d) {
        if (t == null) return STATUS_MISMATCH + ":V51_TRADE_NULL";
        String core =
                verifyV51Core(
                        t.baseIssue,
                        t.targetIssue,
                        t.position,
                        t.n1,
                        t.n2,
                        t.n3,
                        t.candidateHash,
                        t.contractDecisionId,
                        t.snapshotMaxIssue,
                        t.buildId,
                        t.cohortId,
                        t.schemaVersion,
                        t.featureSchemaVersion,
                        t.rankingVersion,
                        t.calibrationVersion,
                        t.gatePolicyVersion,
                        t.pipelineVersion,
                        t.preDrawReasonFingerprint);
        if (!STATUS_VALID.equals(core)) return core;
        if (d != null) {
            if (!same(t.contractDecisionId, d.contractDecisionId))
                return STATUS_MISMATCH + ":V51_DECISION_ID";
            if (!same(t.candidateHash, d.candidateHash))
                return STATUS_MISMATCH + ":V51_COMPILED_CHECKSUM";
            if (!same(t.targetIssue, d.targetIssue)
                    || t.position != d.position
                    || t.n1 != d.n1
                    || t.n2 != d.n2
                    || t.n3 != d.n3) return STATUS_MISMATCH + ":V51_DECISION_TRADE_CONTENT";
            if (!same(t.authorizationId, d.authorizationId) || t.authorizationValid != 1)
                return STATUS_MISMATCH + ":V51_AUTHORIZATION";
        }
        return STATUS_VALID;
    }

    private static String verifyV51Core(
            String baseIssue,
            String targetIssue,
            int position,
            int n1,
            int n2,
            int n3,
            String compiledChecksum,
            String contractDecisionId,
            String snapshotMaxIssue,
            String buildId,
            String cohortId,
            String schemaVersion,
            String featureSchemaVersion,
            String rankingVersion,
            String calibrationVersion,
            String gatePolicyVersion,
            String pipelineVersion,
            String envelopeContractHash) {
        if (safe(baseIssue).length() == 0 || safe(targetIssue).length() == 0)
            return STATUS_MISMATCH + ":V51_ISSUE_EMPTY";
        if (compareIssue(baseIssue, targetIssue) >= 0)
            return STATUS_MISMATCH + ":V51_TARGET_NOT_AFTER_BASE";
        if (!AppConfig.isPlayRuleLegal(position, AppConfig.FIXED_K, n1, n2, n3))
            return STATUS_MISMATCH + ":V51_ILLEGAL_K3";
        if (safe(snapshotMaxIssue).length() == 0 || compareIssue(snapshotMaxIssue, baseIssue) > 0)
            return STATUS_MISMATCH + ":V51_SNAPSHOT_FUTURE";
        if (!V51BuildManifest.BUILD_ID.equals(safe(buildId)))
            return STATUS_MISMATCH + ":V51_BUILD_ID";
        if (!V51BuildManifest.COHORT_ID.equals(safe(cohortId)))
            return STATUS_MISMATCH + ":V51_COHORT_ID";
        if (!V51BuildManifest.DATABASE_SCHEMA.equals(safe(schemaVersion)))
            return STATUS_MISMATCH + ":V51_SCHEMA";
        if (!V51BuildManifest.FEATURE_SCHEMA.equals(safe(featureSchemaVersion)))
            return STATUS_MISMATCH + ":V51_FEATURE_SCHEMA";
        if (!V51ActionRankerArtifact.ARTIFACT_ID.equals(safe(rankingVersion)))
            return STATUS_MISMATCH + ":V51_RANK_ARTIFACT";
        if (!V51SelectedCalibrationArtifact.ARTIFACT_ID.equals(safe(calibrationVersion)))
            return STATUS_MISMATCH + ":V51_CALIBRATION_ARTIFACT";
        if (!V51PolicySurfaceArtifact.ARTIFACT_ID.equals(safe(gatePolicyVersion)))
            return STATUS_MISMATCH + ":V51_POLICY_ARTIFACT";
        if (!V51BuildManifest.VERSION_NAME.equals(safe(pipelineVersion)))
            return STATUS_MISMATCH + ":V51_PIPELINE_VERSION";
        if (safe(compiledChecksum).length() == 0 || safe(envelopeContractHash).length() == 0)
            return STATUS_MISMATCH + ":V51_HASH_EMPTY";
        String expected =
                V51ChecksumEngine.sha256(
                        "V51_DECISION|"
                                + targetIssue
                                + '|'
                                + envelopeContractHash
                                + '|'
                                + V51ArtifactBundleManifest.BUNDLE_HASH);
        if (!same(expected, contractDecisionId)) return STATUS_MISMATCH + ":V51_DECISION_ID";
        return STATUS_VALID;
    }

    private static String verifyV50Decision(Models.Decision d) {
        if (d == null) return STATUS_MISMATCH + ":V50_DECISION_NULL";
        return verifyV50Core(
                d.baseIssue,
                d.targetIssue,
                d.position,
                d.n1,
                d.n2,
                d.n3,
                d.candidateHash,
                d.contractDecisionId,
                d.snapshotMaxIssue,
                d.trainedUntilIssue,
                d.buildId,
                d.cohortId,
                d.schemaVersion,
                d.featureSchemaVersion,
                d.rankingVersion,
                d.calibrationVersion,
                d.gatePolicyVersion,
                d.pipelineVersion,
                d.v46ContractHash);
    }

    private static String verifyV50Trade(Models.Trade t, Models.Decision d) {
        if (t == null) return STATUS_MISMATCH + ":V50_TRADE_NULL";
        String core =
                verifyV50Core(
                        t.baseIssue,
                        t.targetIssue,
                        t.position,
                        t.n1,
                        t.n2,
                        t.n3,
                        t.candidateHash,
                        t.contractDecisionId,
                        t.snapshotMaxIssue,
                        t.trainedUntilIssue,
                        t.buildId,
                        t.cohortId,
                        t.schemaVersion,
                        t.featureSchemaVersion,
                        t.rankingVersion,
                        t.calibrationVersion,
                        t.gatePolicyVersion,
                        t.pipelineVersion,
                        t.v46ContractHash);
        if (!STATUS_VALID.equals(core)) return core;
        if (d != null) {
            if (!same(t.contractDecisionId, d.contractDecisionId))
                return STATUS_MISMATCH + ":V50_DECISION_ID";
            if (!same(t.candidateHash, d.candidateHash))
                return STATUS_MISMATCH + ":V50_COMPILED_CHECKSUM";
            if (!same(t.v46ContractHash, d.v46ContractHash))
                return STATUS_MISMATCH + ":V50_CONTRACT_HASH";
            if (!same(t.targetIssue, d.targetIssue)
                    || t.position != d.position
                    || t.n1 != d.n1
                    || t.n2 != d.n2
                    || t.n3 != d.n3) return STATUS_MISMATCH + ":V50_DECISION_TRADE_CONTENT";
            if (!same(t.authorizationId, d.authorizationId) || t.authorizationValid != 1)
                return STATUS_MISMATCH + ":V50_AUTHORIZATION";
        }
        return STATUS_VALID;
    }

    private static String verifyV50Core(
            String baseIssue,
            String targetIssue,
            int position,
            int n1,
            int n2,
            int n3,
            String compiledChecksum,
            String contractDecisionId,
            String snapshotMaxIssue,
            String trainedUntilIssue,
            String buildId,
            String cohortId,
            String schemaVersion,
            String featureSchemaVersion,
            String rankingVersion,
            String calibrationVersion,
            String gatePolicyVersion,
            String pipelineVersion,
            String contractHash) {
        if (safe(baseIssue).length() == 0 || safe(targetIssue).length() == 0)
            return STATUS_MISMATCH + ":V50_ISSUE_EMPTY";
        if (compareIssue(baseIssue, targetIssue) >= 0)
            return STATUS_MISMATCH + ":V50_TARGET_NOT_AFTER_BASE";
        if (!AppConfig.isPlayRuleLegal(position, AppConfig.FIXED_K, n1, n2, n3))
            return STATUS_MISMATCH + ":V50_ILLEGAL_K3";
        if (safe(snapshotMaxIssue).length() == 0 || compareIssue(snapshotMaxIssue, baseIssue) > 0)
            return STATUS_MISMATCH + ":V50_SNAPSHOT_FUTURE";
        if (safe(trainedUntilIssue).length() == 0 || compareIssue(trainedUntilIssue, baseIssue) > 0)
            return STATUS_MISMATCH + ":V50_TRAINED_FUTURE";
        if (!V50BuildManifest.BUILD_ID.equals(safe(buildId)))
            return STATUS_MISMATCH + ":V50_BUILD_ID";
        if (!V50BuildManifest.COHORT_ID.equals(safe(cohortId)))
            return STATUS_MISMATCH + ":V50_COHORT_ID";
        if (!V50BuildManifest.SCHEMA_ID.equals(safe(schemaVersion)))
            return STATUS_MISMATCH + ":V50_SCHEMA_VERSION";
        if (!V50BuildManifest.FEATURE_SCHEMA.equals(safe(featureSchemaVersion)))
            return STATUS_MISMATCH + ":V50_FEATURE_SCHEMA";
        if (!V50ActionRankerArtifact.ARTIFACT_ID.equals(safe(rankingVersion)))
            return STATUS_MISMATCH + ":V50_RANK_ARTIFACT";
        if (!V50SelectedCalibrationArtifact.ARTIFACT_ID.equals(safe(calibrationVersion)))
            return STATUS_MISMATCH + ":V50_CALIBRATION_ARTIFACT";
        if (!V50SelectivePolicyArtifact.ARTIFACT_ID.equals(safe(gatePolicyVersion)))
            return STATUS_MISMATCH + ":V50_POLICY_ARTIFACT";
        if (!V50BuildManifest.STRATEGY_VERSION.equals(safe(pipelineVersion)))
            return STATUS_MISMATCH + ":V50_PIPELINE_VERSION";
        if (safe(compiledChecksum).length() == 0)
            return STATUS_MISMATCH + ":V50_COMPILED_CHECKSUM_EMPTY";
        if (safe(contractHash).length() == 0) return STATUS_MISMATCH + ":V50_CONTRACT_HASH_EMPTY";
        String expectedDecisionId =
                V50ChecksumEngine.sha256("V50_DECISION|" + contractHash + '|' + targetIssue);
        if (!same(expectedDecisionId, contractDecisionId))
            return STATUS_MISMATCH + ":V50_DECISION_ID";
        return STATUS_VALID;
    }

    private static String verifyCore(
            String baseIssue,
            String targetIssue,
            int position,
            int n1,
            int n2,
            int n3,
            String candidateHash,
            String decisionId,
            String snapshotMaxIssue,
            String trainedUntilIssue,
            String buildId,
            String cohortId,
            String schemaVersion,
            String featureSchemaVersion,
            String rankingVersion,
            String calibrationVersion,
            String gatePolicyVersion,
            String pipelineVersion) {
        if (safe(baseIssue).length() == 0 || safe(targetIssue).length() == 0)
            return STATUS_MISMATCH + ":ISSUE_EMPTY";
        if (compareIssue(baseIssue, targetIssue) >= 0)
            return STATUS_MISMATCH + ":TARGET_NOT_AFTER_BASE";
        if (!AppConfig.isPlayRuleLegal(position, AppConfig.FIXED_K, n1, n2, n3))
            return STATUS_MISMATCH + ":ILLEGAL_K3";
        if (safe(snapshotMaxIssue).length() == 0 || compareIssue(snapshotMaxIssue, baseIssue) > 0)
            return STATUS_MISMATCH + ":SNAPSHOT_FUTURE";
        if (safe(trainedUntilIssue).length() == 0 || compareIssue(trainedUntilIssue, baseIssue) > 0)
            return STATUS_MISMATCH + ":TRAINED_FUTURE";
        if (!AppConfig.BUILD_ID.equals(buildId)) return STATUS_MISMATCH + ":BUILD_ID";
        if (!AppConfig.COHORT_ID.equals(cohortId)) return STATUS_MISMATCH + ":COHORT_ID";
        if (!AppConfig.SCHEMA_VERSION.equals(schemaVersion))
            return STATUS_MISMATCH + ":SCHEMA_VERSION";
        if (!AppConfig.FEATURE_SCHEMA_VERSION.equals(featureSchemaVersion))
            return STATUS_MISMATCH + ":FEATURE_SCHEMA";
        if (!AppConfig.RANKING_VERSION.equals(rankingVersion))
            return STATUS_MISMATCH + ":RANKING_VERSION";
        if (!AppConfig.CALIBRATION_VERSION.equals(calibrationVersion))
            return STATUS_MISMATCH + ":CALIBRATION_VERSION";
        if (!AppConfig.GATE_POLICY_VERSION.equals(gatePolicyVersion))
            return STATUS_MISMATCH + ":GATE_POLICY_VERSION";
        if (!AppConfig.PIPELINE_VERSION.equals(pipelineVersion))
            return STATUS_MISMATCH + ":PIPELINE_VERSION";
        String expectedHash =
                candidateHash(
                        baseIssue,
                        targetIssue,
                        position,
                        n1,
                        n2,
                        n3,
                        snapshotMaxIssue,
                        buildId,
                        cohortId,
                        schemaVersion,
                        featureSchemaVersion,
                        rankingVersion);
        if (!same(expectedHash, candidateHash)) return STATUS_MISMATCH + ":CANDIDATE_HASH";
        String expectedId =
                decisionId(
                        baseIssue,
                        targetIssue,
                        candidateHash,
                        buildId,
                        cohortId,
                        schemaVersion,
                        pipelineVersion,
                        calibrationVersion,
                        gatePolicyVersion);
        if (!same(expectedId, decisionId)) return STATUS_MISMATCH + ":DECISION_ID";
        return STATUS_VALID;
    }

    public static String candidateHash(
            String baseIssue,
            String targetIssue,
            int position,
            int n1,
            int n2,
            int n3,
            String snapshotMaxIssue,
            String buildId,
            String cohortId,
            String schemaVersion,
            String featureSchemaVersion,
            String rankingVersion) {
        int[] a = sort3(n1, n2, n3);
        return digest(
                "C|"
                        + safe(baseIssue)
                        + "|"
                        + safe(targetIssue)
                        + "|"
                        + position
                        + "|"
                        + a[0]
                        + "|"
                        + a[1]
                        + "|"
                        + a[2]
                        + "|"
                        + safe(snapshotMaxIssue)
                        + "|"
                        + safe(buildId)
                        + "|"
                        + safe(cohortId)
                        + "|"
                        + safe(schemaVersion)
                        + "|"
                        + safe(featureSchemaVersion)
                        + "|"
                        + safe(rankingVersion));
    }

    public static String decisionId(
            String baseIssue,
            String targetIssue,
            String candidateHash,
            String buildId,
            String cohortId,
            String schemaVersion,
            String pipelineVersion,
            String calibrationVersion,
            String gatePolicyVersion) {
        return digest(
                "D|"
                        + safe(baseIssue)
                        + "|"
                        + safe(targetIssue)
                        + "|"
                        + safe(candidateHash)
                        + "|"
                        + safe(buildId)
                        + "|"
                        + safe(cohortId)
                        + "|"
                        + safe(schemaVersion)
                        + "|"
                        + safe(pipelineVersion)
                        + "|"
                        + safe(calibrationVersion)
                        + "|"
                        + safe(gatePolicyVersion));
    }

    private static String digest(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16 && i < b.length; i++)
                sb.append(String.format(Locale.US, "%02x", b[i] & 255));
            return sb.toString();
        } catch (Throwable e) {
            return Integer.toHexString(text == null ? 0 : text.hashCode());
        }
    }

    private static String latestIssue(List<Models.Draw> history) {
        if (history == null) return "";
        String best = "";
        for (int i = 0; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d != null && d.isValid() && compareIssue(d.issue, best) > 0) best = safe(d.issue);
        }
        return best;
    }

    public static int compareIssue(String a, String b) {
        String x = digits(a), y = digits(b);
        if (x.length() != y.length()) return x.length() < y.length() ? -1 : 1;
        return x.compareTo(y);
    }

    private static String digits(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++)
            if (Character.isDigit(s.charAt(i))) b.append(s.charAt(i));
        return b.toString();
    }

    private static int[] sort3(int a, int b, int c) {
        int[] x = new int[] {a, b, c};
        if (x[0] > x[1]) {
            int t = x[0];
            x[0] = x[1];
            x[1] = t;
        }
        if (x[1] > x[2]) {
            int t = x[1];
            x[1] = x[2];
            x[2] = t;
        }
        if (x[0] > x[1]) {
            int t = x[0];
            x[0] = x[1];
            x[1] = t;
        }
        return x;
    }

    private static void normalizeK3(Models.ComboCandidate c) {
        int[] a = sort3(c.n1, c.n2, c.n3);
        c.setNumbers(a[0], a[1], a[2]);
    }

    private static void normalizeK3(Models.Decision d) {
        int[] a = sort3(d.n1, d.n2, d.n3);
        d.setNumbers(a[0], a[1], a[2]);
    }

    private static void normalizeK3(Models.Trade t) {
        int[] a = sort3(t.n1, t.n2, t.n3);
        t.setNumbers(a[0], a[1], a[2]);
    }

    private static boolean same(String a, String b) {
        return safe(a).equals(safe(b));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String firstNonEmpty(String a, String b, String c, String d) {
        if (safe(a).length() > 0) return a;
        if (safe(b).length() > 0) return b;
        if (safe(c).length() > 0) return c;
        return safe(d);
    }
}
