package com.ethk3.novaonepro;

/** Exact V51 contract signer. Authorization never recalculates model values. */
public final class V51AuthorizationCore {
    private V51AuthorizationCore() {}

    public static V51Models.AuthorizedDecision authorize(
            V51CompiledActionContract compiled,
            V51Enums.DecisionAction gateAction,
            V51Enums.RejectCode gateReject) {
        V51Enums.DecisionAction action =
                gateAction == null ? V51Enums.DecisionAction.HOLD : gateAction;
        V51Enums.RejectCode reject =
                gateReject == null ? V51Enums.RejectCode.RUNTIME_EXCEPTION : gateReject;
        V51Models.SelectedActionEnvelope selected = compiled == null ? null : compiled.selected;
        if (compiled == null
                || selected == null
                || selected.context == null
                || selected.action == null) {
            action = V51Enums.DecisionAction.HOLD;
            reject = V51Enums.RejectCode.DATA_INCOMPLETE;
        }
        if (action == V51Enums.DecisionAction.BET && !V51BuildManifest.mayAuthorizeRealBet()) {
            action = V51Enums.DecisionAction.SHADOW;
            reject = V51Enums.RejectCode.MODEL_NOT_PROVEN;
        }
        long at = System.currentTimeMillis();
        String target =
                selected == null || selected.context == null ? "" : selected.context.targetIssue;
        String contractHash = selected == null ? "" : selected.contractHash;
        String decisionId =
                V51ChecksumEngine.sha256(
                        "V51_DECISION|"
                                + target
                                + '|'
                                + contractHash
                                + '|'
                                + V51ArtifactBundleManifest.BUNDLE_HASH);
        String auth =
                sign(
                        decisionId,
                        contractHash,
                        compiled == null ? "" : compiled.checksum,
                        action,
                        reject,
                        at);
        return new V51Models.AuthorizedDecision(
                action, reject, selected, decisionId, auth, V51ArtifactBundleManifest.BUNDLE_HASH);
    }

    public static boolean verify(
            V51Models.AuthorizedDecision decision, V51CompiledActionContract compiled) {
        if (decision == null || compiled == null || decision.selected == null) return false;
        String expected =
                sign(
                        decision.decisionId,
                        decision.selected.contractHash,
                        compiled.checksum,
                        decision.action,
                        decision.rejectCode,
                        extractTime(decision.authorizationHash));
        return expected.equals(decision.authorizationHash);
    }

    public static boolean verifyLegacyDecision(Models.Decision d) {
        if (d == null || d.authorizationValid != 1 || !"BET".equals(d.authorizedMode)) return false;
        if (!V51BuildManifest.COHORT_ID.equals(s(d.cohortId))) return false;
        long at = extractTime(d.authorizationId);
        if (at <= 0L) return false;
        String expected =
                sign(
                        s(d.contractDecisionId),
                        s(d.preDrawReasonFingerprint),
                        s(d.candidateHash),
                        V51Enums.DecisionAction.BET,
                        V51Enums.RejectCode.NONE,
                        at);
        return expected.equals(s(d.authorizationId));
    }

    public static boolean verifyLegacyTrade(Models.Trade t) {
        if (t == null || t.authorizationValid != 1 || !"BET".equals(t.authorizedMode)) return false;
        if (!V51BuildManifest.COHORT_ID.equals(s(t.cohortId))) return false;
        long at = extractTime(t.authorizationId);
        if (at <= 0L) return false;
        String expected =
                sign(
                        s(t.contractDecisionId),
                        s(t.preDrawReasonFingerprint),
                        s(t.candidateHash),
                        V51Enums.DecisionAction.BET,
                        V51Enums.RejectCode.NONE,
                        at);
        return expected.equals(s(t.authorizationId));
    }

    private static String sign(
            String id,
            String contract,
            String compiled,
            V51Enums.DecisionAction action,
            V51Enums.RejectCode reject,
            long at) {
        return prefix(id, contract, compiled, action, reject)
                + at
                + '|'
                + V51ChecksumEngine.sha256(
                        V51BuildManifest.fingerprint()
                                + '|'
                                + V51FieldOwnerRegistry.fingerprint()
                                + '|'
                                + at);
    }

    private static String prefix(
            String id,
            String contract,
            String compiled,
            V51Enums.DecisionAction action,
            V51Enums.RejectCode reject) {
        return "V51AUTH|"
                + safe(id)
                + '|'
                + safe(contract)
                + '|'
                + safe(compiled)
                + '|'
                + action.name()
                + '|'
                + reject.name()
                + '|';
    }

    private static long extractTime(String signature) {
        try {
            String[] x = signature.split("\\|");
            return x.length > 6 ? Long.parseLong(x[6]) : 0L;
        } catch (Throwable e) {
            return 0L;
        }
    }

    /** Exact signed authorization time for the immutable REAL ledger writer. */
    public static long authorizationTime(String signature) {
        return extractTime(signature);
    }

    /** Syntax only. Historical audits must also match the immutable authorization records;
     * this method never grants permission or substitutes for verify(). */
    public static boolean isBetEnvelope(String signature) {
        if (signature == null) return false;
        String[] fields = signature.split("\\|", -1);
        return fields.length == 8 && "V51AUTH".equals(fields[0])
                && fields[1].matches("[0-9a-f]{64}") && fields[2].matches("[0-9a-f]{64}")
                && fields[3].matches("[0-9a-f]{64}") && "BET".equals(fields[4])
                && "NONE".equals(fields[5]) && fields[6].matches("[1-9][0-9]{0,18}")
                && extractTime(signature) > 0 && fields[7].matches("[0-9a-f]{64}");
    }

    private static String safe(String x) {
        return x == null ? "" : x;
    }

    private static String s(String x) {
        return x == null ? "" : x;
    }
}
