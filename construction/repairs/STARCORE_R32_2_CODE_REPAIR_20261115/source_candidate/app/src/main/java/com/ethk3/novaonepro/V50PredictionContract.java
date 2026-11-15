package com.ethk3.novaonepro;

/** Hard validation contract before a V50 action can reach DecisionGateEngine. */
public final class V50PredictionContract {
    public static final class Result {
        public final boolean valid;
        public final V50Enums.RejectCode rejectCode;
        public final String detail;

        Result(boolean valid, V50Enums.RejectCode rejectCode, String detail) {
            this.valid = valid;
            this.rejectCode = rejectCode;
            this.detail = detail == null ? "" : detail;
        }
    }

    private V50PredictionContract() {}

    public static Result validate(V50Models.SelectedActionEnvelope envelope) {
        V50FieldOwnerRegistry.verifyUniqueOwners();
        if (envelope == null || envelope.context == null || envelope.action == null) {
            return fail(
                    V50Enums.RejectCode.DATA_INCOMPLETE, "V50_ENVELOPE_CONTEXT_ACTION_REQUIRED");
        }
        if (!V50BuildManifest.COHORT_ID.equals(envelope.context.cohortId)) {
            return fail(V50Enums.RejectCode.BUILD_IDENTITY_MISMATCH, "V50_COHORT_MISMATCH");
        }
        if (envelope.context.targetIssue.length() == 0
                || !envelope.context.targetIssue.equals(envelope.action.targetIssue)) {
            return fail(V50Enums.RejectCode.TARGET_ISSUE_INVALID, "V50_TARGET_ISSUE_MISMATCH");
        }
        if (!envelope.action.validFixedK3()) {
            return fail(
                    V50Enums.RejectCode.CONTRACT_MISMATCH, "V50_SINGLE_POSITION_FIXED_K3_REQUIRED");
        }
        if (!probability(envelope.action.rawActionP)
                || !probability(envelope.action.rankAdjustedP)
                || !probability(envelope.selectedHitP)
                || !probability(envelope.uncertainty)) {
            return fail(V50Enums.RejectCode.CONTRACT_MISMATCH, "V50_PROBABILITY_RANGE_INVALID");
        }
        if (envelope.action.rank < 1 || envelope.action.rank > 10) {
            return fail(V50Enums.RejectCode.CONTRACT_MISMATCH, "V50_ACTION_RANK_INVALID");
        }
        if (envelope.contractHash.length() != 64) {
            return fail(V50Enums.RejectCode.CONTRACT_MISMATCH, "V50_CONTRACT_HASH_INVALID");
        }
        String expected = compileHash(envelope);
        if (!expected.equals(envelope.contractHash)) {
            return fail(V50Enums.RejectCode.CONTRACT_MISMATCH, "V50_CONTRACT_HASH_MISMATCH");
        }
        return new Result(true, V50Enums.RejectCode.NONE, "V50_CONTRACT_OK");
    }

    public static String compileHash(V50Models.SelectedActionEnvelope envelope) {
        if (envelope == null || envelope.context == null || envelope.action == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(V50BuildManifest.fingerprint())
                .append('|')
                .append(envelope.context.baseIssue)
                .append('|')
                .append(envelope.context.targetIssue)
                .append('|')
                .append(envelope.context.historyHash)
                .append('|')
                .append(envelope.action.position)
                .append('|');
        for (int i = 0; i < envelope.action.top3.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(envelope.action.top3[i]);
        }
        sb.append('|')
                .append(Double.toString(envelope.action.rawActionP))
                .append('|')
                .append(Double.toString(envelope.action.rankAdjustedP))
                .append('|')
                .append(Double.toString(envelope.selectedHitP))
                .append('|')
                .append(Double.toString(envelope.uncertainty))
                .append('|')
                .append(envelope.domainStatus.name())
                .append('|')
                .append(envelope.lane.name())
                .append('|')
                .append(envelope.primaryCause)
                .append('|')
                .append(envelope.secondaryCause)
                .append('|')
                .append(envelope.thirdCause)
                .append('|')
                .append(envelope.counterEvidence)
                .append('|')
                .append(envelope.necessaryCondition)
                .append('|')
                .append(envelope.falsifierCondition)
                .append('|')
                .append(envelope.action.checksum);
        return V50ChecksumEngine.sha256(sb.toString());
    }

    public static boolean normalized10(double[] p, double tolerance) {
        if (p == null || p.length != 10) return false;
        double sum = 0.0d;
        for (int i = 0; i < p.length; i++) {
            if (!probability(p[i])) return false;
            sum += p[i];
        }
        return Math.abs(1.0d - sum) <= Math.max(1.0e-12d, tolerance);
    }

    private static boolean probability(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0d && value <= 1.0d;
    }

    private static Result fail(V50Enums.RejectCode code, String detail) {
        return new Result(false, code, detail);
    }
}
