package com.ethk3.novaonepro;

/**
 * B12 immutable V51 gate validator. It validates the frozen contract only. DecisionGateEngine
 * remains the only class allowed to choose BET/SHADOW/HOLD. This class never signs, persists or
 * settles.
 */
public final class V51DecisionGateCore {
    public static final String VERSION = "V57_B12_V51_GATE_CORE_20260808";
    public static final String BET_OWNER = "DecisionGateEngine";
    public static final String SETTLEMENT_OWNER = "SettlementEngine";

    public static final class Result {
        public final boolean contractValid;
        public final boolean fixedK3Valid;
        public final boolean pitValid;
        public final boolean championValid;
        public final boolean artifactValid;
        public final boolean policyValid;
        public final boolean rankValid;
        public final boolean calibrationValid;
        public final boolean domainValid;
        public final boolean stabilityValid;
        public final boolean duplicateFree;
        public final boolean openTradeFree;
        public final boolean releaseAllowsReal;
        public final boolean realEligible;
        public final V51Enums.RejectCode rejectCode;
        public final String detail;
        public final String checksum;

        Result(
                boolean contractValid,
                boolean fixedK3Valid,
                boolean pitValid,
                boolean championValid,
                boolean artifactValid,
                boolean policyValid,
                boolean rankValid,
                boolean calibrationValid,
                boolean domainValid,
                boolean stabilityValid,
                boolean duplicateFree,
                boolean openTradeFree,
                boolean releaseAllowsReal,
                V51Enums.RejectCode rejectCode,
                String detail) {
            this.contractValid = contractValid;
            this.fixedK3Valid = fixedK3Valid;
            this.pitValid = pitValid;
            this.championValid = championValid;
            this.artifactValid = artifactValid;
            this.policyValid = policyValid;
            this.rankValid = rankValid;
            this.calibrationValid = calibrationValid;
            this.domainValid = domainValid;
            this.stabilityValid = stabilityValid;
            this.duplicateFree = duplicateFree;
            this.openTradeFree = openTradeFree;
            this.releaseAllowsReal = releaseAllowsReal;
            this.realEligible =
                    contractValid
                            && fixedK3Valid
                            && pitValid
                            && championValid
                            && artifactValid
                            && policyValid
                            && rankValid
                            && calibrationValid
                            && domainValid
                            && stabilityValid
                            && duplicateFree
                            && openTradeFree
                            && releaseAllowsReal;
            this.rejectCode = rejectCode == null ? V51Enums.RejectCode.DATA_INCOMPLETE : rejectCode;
            this.detail = detail == null ? "" : detail;
            this.checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return String.valueOf(contractValid)
                    + '|'
                    + fixedK3Valid
                    + '|'
                    + pitValid
                    + '|'
                    + championValid
                    + '|'
                    + artifactValid
                    + '|'
                    + policyValid
                    + '|'
                    + rankValid
                    + '|'
                    + calibrationValid
                    + '|'
                    + domainValid
                    + '|'
                    + stabilityValid
                    + '|'
                    + duplicateFree
                    + '|'
                    + openTradeFree
                    + '|'
                    + releaseAllowsReal
                    + '|'
                    + realEligible
                    + '|'
                    + rejectCode
                    + '|'
                    + detail
                    + '|'
                    + VERSION;
        }
    }

    private V51DecisionGateCore() {}

    public static Result validate(DbStore store, V51CompiledActionContract c) {
        if (c == null
                || c.selected == null
                || c.selected.context == null
                || c.selected.action == null) {
            return fail(V51Enums.RejectCode.DATA_INCOMPLETE, "CONTRACT_NULL");
        }
        V51Models.SelectedActionEnvelope e = c.selected;
        V51Models.PositionAction a = e.action;
        if (!validK3(a)) {
            return fail(V51Enums.RejectCode.FIXED_K3_CONTRACT_FAILED, "FIXED_K3_CONTRACT_FAILED");
        }
        if (!pitValid(e.context)) {
            return fail(
                    V51Enums.RejectCode.HISTORY_NOT_POINT_IN_TIME, "PIT_HISTORY_NOT_BEFORE_TARGET");
        }
        if (!V51BuildManifest.COHORT_ID.equals(e.context.cohortId)
                || !V51ArtifactBundleManifest.BUNDLE_HASH.equals(e.context.artifactBundleHash)) {
            return fail(
                    V51Enums.RejectCode.ARTIFACT_BUNDLE_MISMATCH, "IDENTITY_OR_BUNDLE_MISMATCH");
        }
        if (!nonEmpty(c.featureHash)
                || !nonEmpty(c.stateHash)
                || !nonEmpty(c.expertHash)
                || !nonEmpty(c.probabilityHash)
                || !nonEmpty(c.actionHash)
                || !nonEmpty(c.checksum)) {
            return fail(V51Enums.RejectCode.LEDGER_INCOMPLETE, "COMPILED_HASH_CHAIN_INCOMPLETE");
        }
        if (!championUnchanged(store, a)) {
            return fail(V51Enums.RejectCode.RANK_UNSTABLE, "CHAMPION_MUTATED_AFTER_FREEZE");
        }
        if (c.policy == null || !c.policy.realEligible) {
            return fail(
                    c.policy == null
                            ? V51Enums.RejectCode.POLICY_CELL_NOT_PROVEN
                            : c.policy.rejectCode,
                    "POLICY_NOT_REAL_ELIGIBLE");
        }
        if (!V51NumberModelArtifact.PROVEN || !V51ExpertGateArtifact.PROVEN) {
            return fail(V51Enums.RejectCode.MODEL_NOT_PROVEN, "NUMBER_OR_GATE_ARTIFACT_UNPROVEN");
        }
        if (!V51ActionRankerArtifact.PROVEN
                || !V51ActionRankerArtifact.RANK_ORDER_PASSED
                || V51ActionRankerArtifact.STABLE_FOLDS < 4) {
            return fail(V51Enums.RejectCode.RANK_ORDER_NOT_PROVEN, "RANKER_ARTIFACT_UNPROVEN");
        }
        if (!V51SelectedCalibrationArtifact.PROVEN
                || Double.isNaN(e.selectedHitP)
                || Double.isInfinite(e.selectedHitP)
                || e.selectedHitP <= 0.0d
                || e.selectedHitP >= 1.0d) {
            return fail(V51Enums.RejectCode.CALIBRATION_NOT_PROVEN, "SELECTED_HIT_P_UNPROVEN");
        }
        if (e.domain == null || e.domain.status != V51Enums.DomainStatus.PROVEN) {
            return fail(V51Enums.RejectCode.DOMAIN_NOT_PROVEN, "DOMAIN_NOT_PROVEN");
        }
        if (c.stability == null || !c.stability.stable) {
            return fail(V51Enums.RejectCode.RANK_UNSTABLE, "RANK_STABILITY_FAILED");
        }
        boolean duplicateFree = true;
        boolean openTradeFree = true;
        if (store != null) {
            Models.Trade old = store.getTradeByTargetIssue(e.context.targetIssue);
            duplicateFree = old == null;
            openTradeFree = !store.hasOpenRealTrade();
            if (!duplicateFree) {
                return fail(
                        V51Enums.RejectCode.DUPLICATE_TRADE,
                        "REAL_BET_COUNT_TARGET_ALREADY_NONZERO");
            }
            if (!openTradeFree) {
                return fail(V51Enums.RejectCode.UNSETTLED_TRADE_EXISTS, "OPEN_REAL_TRADE_EXISTS");
            }
        }
        boolean release = V51BuildManifest.mayAuthorizeRealBet();
        if (!release) {
            return fail(V51Enums.RejectCode.MODEL_NOT_PROVEN, "RELEASE_REAL_BET_LOCKED");
        }
        return new Result(
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                duplicateFree,
                openTradeFree,
                true,
                V51Enums.RejectCode.NONE,
                "ALL_B12_INVARIANTS_PASS");
    }

    private static Result fail(V51Enums.RejectCode code, String detail) {
        return new Result(
                false, false, false, false, false, false, false, false, false, false, false, false,
                false, code, detail);
    }

    private static boolean validK3(V51Models.PositionAction a) {
        if (a == null || a.position < 1 || a.position > 10) return false;
        if (a.n1 < 1 || a.n1 > 10 || a.n2 < 1 || a.n2 > 10 || a.n3 < 1 || a.n3 > 10) return false;
        return a.n1 != a.n2 && a.n1 != a.n3 && a.n2 != a.n3;
    }

    private static boolean pitValid(V51Models.PredictionContext c) {
        if (c == null || !nonEmpty(c.historyMaxIssue) || !nonEmpty(c.targetIssue)) {
            return false;
        }
        return compareIssue(c.historyMaxIssue, c.targetIssue) < 0;
    }

    private static boolean championUnchanged(DbStore store, V51Models.PositionAction a) {
        if (store == null || a == null) return true;
        String fp = store.getState("v57_b10_champion_fingerprint", "");
        String sum = store.getState("v57_b10_champion_checksum", "");
        if (fp.length() > 0 && !fp.equals(a.fingerprint)) return false;
        if (sum.length() > 0 && !sum.equals(a.checksum)) return false;
        return true;
    }

    private static int compareIssue(String a, String b) {
        try {
            String x = a == null ? "" : a.replaceAll("[^0-9]", "");
            String y = b == null ? "" : b.replaceAll("[^0-9]", "");
            return new java.math.BigInteger(x).compareTo(new java.math.BigInteger(y));
        } catch (Throwable error) {
            return safe(a).compareTo(safe(b));
        }
    }

    private static boolean nonEmpty(String value) {
        return value != null && value.length() > 0;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static boolean canCreateBet() {
        return false;
    }

    public static boolean canSettle() {
        return false;
    }
}
