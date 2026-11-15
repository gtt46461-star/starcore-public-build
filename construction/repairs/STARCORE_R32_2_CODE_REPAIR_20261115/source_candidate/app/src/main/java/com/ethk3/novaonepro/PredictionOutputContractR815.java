package com.ethk3.novaonepro;

import java.util.List;

/** Exact production-output contract: 100 number probabilities, 1200 unique K3 actions, one rank 1. */
public final class PredictionOutputContractR815 {
    public static final class Validation {
        public final boolean valid;
        public final PredictionAvailabilityR815 availability;
        public final String code;
        public final String targetIssue;
        public final String checksum;
        public final int numberRows;
        public final int actionRows;
        public final int rankOneCount;

        Validation(
                boolean valid,
                PredictionAvailabilityR815 availability,
                String code,
                String targetIssue,
                String checksum,
                int numberRows,
                int actionRows,
                int rankOneCount) {
            this.valid = valid;
            this.availability = availability == null ? PredictionAvailabilityR815.PRODUCER_FAILED : availability;
            this.code = safe(code);
            this.targetIssue = safe(targetIssue);
            this.checksum = safe(checksum);
            this.numberRows = Math.max(0, numberRows);
            this.actionRows = Math.max(0, actionRows);
            this.rankOneCount = Math.max(0, rankOneCount);
        }

        public String summary() {
            return code
                    + "|target="
                    + targetIssue
                    + "|numbers="
                    + numberRows
                    + "|actions="
                    + actionRows
                    + "|rank1="
                    + rankOneCount
                    + "|availability="
                    + availability.name()
                    + "|hash="
                    + checksum;
        }
    }

    private PredictionOutputContractR815() {}

    public static Validation validate(
            String targetIssue, V51RuntimeResult result, List<Models.Draw> historySnapshot) {
        String target = safe(targetIssue);
        HistoryIntegrityVerifierR815.Result history = HistoryIntegrityVerifierR815.verifyAscending(historySnapshot);
        if (!TargetIssueResolver.isValid(target))
            return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "TARGET_INVALID", 0, 0, 0);
        if (history.hasHardConflict())
            return fail(target, PredictionAvailabilityR815.HISTORY_CONFLICT, "HISTORY_CONFLICT", 0, 0, 0);
        if (history.contiguousSuffix < AppConfig.R815_MIN_CANONICAL_HISTORY)
            return fail(target, PredictionAvailabilityR815.NO_HISTORY_LT_12, "HISTORY_LT_12", 0, 0, 0);
        if (history.gapCount > 0)
            return fail(target, PredictionAvailabilityR815.HISTORY_GAPPED, "HISTORY_GAPPED", 0, 0, 0);
        if (result == null || !result.success || result.context == null || result.numberModel == null)
            return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "RUNTIME_RESULT_INVALID", 0, 0, 0);
        if (!target.equals(result.context.targetIssue)
                || !history.maxIssue.equals(result.context.baseIssue)
                || !TargetIssueResolver.exact(result.context.baseIssue, target).valid)
            return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "CONTEXT_BOUNDARY_INVALID", 0, 0, 0);
        double[][] probabilities =
                result.numberModel.matrix == null ? null : result.numberModel.matrix.probability;
        if (probabilities == null || probabilities.length != 10)
            return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "NUMBER_MATRIX_ROWS", 0, 0, 0);
        int numberRows = 0;
        for (int position = 0; position < probabilities.length; position++) {
            if (probabilities[position] == null || probabilities[position].length != 10)
                return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "NUMBER_MATRIX_COLUMNS", numberRows, 0, 0);
            double sum = 0.0d;
            for (int number = 0; number < probabilities[position].length; number++) {
                double value = probabilities[position][number];
                if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0d || value > 1.0d)
                    return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "NUMBER_PROBABILITY_INVALID", numberRows, 0, 0);
                sum += value;
                numberRows++;
            }
            if (Math.abs(1.0d - sum) > 0.0005d)
                return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "NUMBER_PROBABILITY_NOT_NORMALIZED", numberRows, 0, 0);
        }
        V51Models.PositionAction[] actions = result.actions;
        if (actions == null || actions.length != 1200)
            return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "ACTION_COUNT_INVALID", numberRows, actions == null ? 0 : actions.length, 0);
        boolean[] ranks = new boolean[1201];
        boolean[] identities = new boolean[1200];
        int rankOne = 0;
        V51Models.PositionAction rankOneAction = null;
        for (int i = 0; i < actions.length; i++) {
            V51Models.PositionAction action = actions[i];
            if (action == null
                    || !AppConfig.isValidPosition(action.position)
                    || !DecisionGateEngine.b12K3(action.n1, action.n2, action.n3)
                    || action.rank < 1
                    || action.rank > 1200
                    || action.checksum == null
                    || action.checksum.length() != 64)
                return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "ACTION_FIELD_INVALID", numberRows, i, rankOne);
            int ordinal;
            try {
                ordinal =
                        K3ActionSpaceEngine.globalOrdinal(
                                action.position, action.n1, action.n2, action.n3);
            } catch (RuntimeException invalidIdentity) {
                return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "ACTION_IDENTITY_INVALID", numberRows, i, rankOne);
            }
            if (ordinal < 0
                    || ordinal >= identities.length
                    || identities[ordinal]
                    || ranks[action.rank])
                return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "ACTION_DUPLICATE", numberRows, i, rankOne);
            identities[ordinal] = true;
            ranks[action.rank] = true;
            if (action.rank == 1) {
                rankOne++;
                rankOneAction = action;
            }
        }
        if (rankOne != 1 || rankOneAction == null)
            return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "RANK_ONE_INVALID", numberRows, actions.length, rankOne);
        boolean controlledBet = result.authorized != null
                && result.authorized.action == V51Enums.DecisionAction.BET
                && V51CandidateRealGateR915.verifyAuthorized(
                        null, result.contract, result.authorized);
        if (result.selected == null
                || result.selected.action == null
                || result.selected.action.rank != 1
                || result.authorized == null
                || (result.authorized.action == V51Enums.DecisionAction.BET && !controlledBet))
            return fail(target, PredictionAvailabilityR815.PRODUCER_FAILED, "CANONICAL_SELECTION_INVALID", numberRows, actions.length, rankOne);
        String checksum =
                V51ChecksumEngine.sha256(
                        "R815_OUTPUT|"
                                + target
                                + '|'
                                + history.historyHash
                                + '|'
                                + result.checksum
                                + '|'
                                + rankOneAction.checksum);
        return new Validation(
                true,
                AppConfig.R815_MODEL_PROVEN
                        ? PredictionAvailabilityR815.CANONICAL_READY_PROVEN
                        : controlledBet
                                ? PredictionAvailabilityR815.CANONICAL_READY_CONTROLLED_UNPROVEN
                                : PredictionAvailabilityR815.CANONICAL_READY_SHADOW,
                "READY",
                target,
                checksum,
                numberRows,
                actions.length,
                rankOne);
    }

    public static void requireCanonicalReady(Validation validation) {
        if (validation == null || !validation.valid || !validation.availability.hasNumbers())
            throw new IllegalStateException(
                    "R815_OUTPUT_CONTRACT_FAILED|" + (validation == null ? "NULL" : validation.summary()));
    }

    private static Validation fail(
            String target,
            PredictionAvailabilityR815 availability,
            String code,
            int numberRows,
            int actionRows,
            int rankOne) {
        return new Validation(
                false,
                availability,
                code,
                target,
                V51ChecksumEngine.sha256("R815_OUTPUT_FAIL|" + safe(target) + '|' + code),
                numberRows,
                actionRows,
                rankOne);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
