package com.ethk3.novaonepro;

/** One-pass mapper from immutable cross-position primitives to the typed 96-value vector. */
public final class ResearchFeatureMaterializerR815 {
    public static final class ActionIdentity {
        public final String targetIssue;
        public final int position;
        public final int n1;
        public final int n2;
        public final int n3;
        public final int rank;

        public ActionIdentity(String targetIssue, int position, int n1, int n2, int n3, int rank) {
            this.targetIssue = safe(targetIssue);
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.rank = rank;
            if (!TargetIssueResolver.isValid(this.targetIssue)
                    || !AppConfig.isValidPosition(position)
                    || !DecisionGateEngine.b12K3(n1, n2, n3)
                    || rank < 1 || rank > 1200)
                throw new IllegalArgumentException("R815_FEATURE_ACTION_INVALID");
        }
    }

    private ResearchFeatureMaterializerR815() {}

    public static ResearchFeatureVectorR815 materialize(
            ResearchFeatureVectorR815.Binding binding,
            CrossPositionPrimitiveCacheR815.Snapshot cache,
            ActionIdentity action) {
        if (binding == null || cache == null || action == null)
            throw new IllegalArgumentException("R815_FEATURE_ARGUMENT_NULL");
        if (!binding.targetIssue.equals(action.targetIssue)
                || !binding.targetIssue.equals(cache.binding.targetIssue)
                || !binding.historyHash.equals(cache.binding.historyHash)
                || !binding.schemaSha256.equals(cache.binding.schemaHash))
            throw new IllegalArgumentException("R815_FEATURE_BINDING_MISMATCH");
        ResearchFeatureSchemaR815.assertIntegrity();
        ResearchFeatureVectorR815.Builder out = ResearchFeatureVectorR815.builder(binding);
        // 0..19: explicitly project the first two position-number rows.
        int index = 0;
        for (int position = 1; position <= 2; position++) {
            for (int number = 1; number <= 10; number++) out.set(index++, cache.atom(position, number).q, true);
        }
        // 20..29 assignment identity, 30..39 immutable cache summaries.
        for (int position = 1; position <= 10; position++) out.set(index++, cache.assignedNumber(position) / 10.0d, true);
        double minAssignment = 1.0d, maxAssignment = 0.0d, sumAssignment = 0.0d;
        for (int position = 1; position <= 10; position++) {
            double value = cache.atom(position, cache.assignedNumber(position)).q;
            minAssignment = Math.min(minAssignment, value);
            maxAssignment = Math.max(maxAssignment, value);
            sumAssignment += value;
        }
        CrossPositionPrimitiveCacheR815.Subset pair = cache.subset(3);
        CrossPositionPrimitiveCacheR815.Subset triplet = cache.subset(7);
        double subsetMean = 0.0d, subsetMin = 1.0d, subsetMax = 0.0d;
        int subsets = 0;
        for (int mask = 0; mask < (1 << 10); mask++) {
            if (Integer.bitCount(mask) < 2) continue;
            CrossPositionPrimitiveCacheR815.Subset subset = cache.subset(mask);
            subsetMean += subset.qMean;
            subsetMin = Math.min(subsetMin, subset.qMean);
            subsetMax = Math.max(subsetMax, subset.qMean);
            subsets++;
        }
        out.set(index++, cache.assignmentScore / 10.0d, true);
        out.set(index++, maxAssignment - minAssignment, true);
        out.set(index++, sumAssignment / 10.0d, true);
        out.set(index++, entropy(cache, 1), true);
        out.set(index++, pair.qMean, true);
        out.set(index++, triplet.qMean, true);
        out.set(index++, subsetMean / subsets, true);
        out.set(index++, subsetMax, true);
        out.set(index++, subsetMin, true);
        out.set(index++, Math.min(1.0d, cache.binding.contiguousHistory / 720.0d), true);
        // 40..62 history readiness and distribution statistics.
        out.set(index++, cache.binding.contiguousHistory >= 24 ? 1.0d : 0.0d, true);
        out.set(index++, cache.binding.contiguousHistory >= 360 ? 1.0d : 0.0d, true);
        out.set(index++, cache.binding.contiguousHistory >= 720 ? 1.0d : 0.0d, true);
        for (int position = 1; position <= 10; position++) out.set(index++, entropy(cache, position), true);
        double sum = 0.0d, sumSq = 0.0d, globalMin = 1.0d, globalMax = 0.0d;
        for (int position = 1; position <= 10; position++) {
            for (int number = 1; number <= 10; number++) {
                double value = cache.atom(position, number).q;
                sum += value;
                sumSq += value * value;
                globalMin = Math.min(globalMin, value);
                globalMax = Math.max(globalMax, value);
            }
        }
        double globalMean = sum / 100.0d;
        out.set(index++, globalMean, true);
        out.set(index++, Math.sqrt(Math.max(0.0d, sumSq / 100.0d - globalMean * globalMean)), true);
        out.set(index++, globalMax, true);
        out.set(index++, globalMin, true);
        out.set(index++, sumAssignment / 10.0d, true);
        out.set(index++, minAssignment, true);
        out.set(index++, maxAssignment, true);
        // 63..72 selected action projection.
        out.set(index++, action.position / 10.0d, true);
        out.set(index++, action.n1 / 10.0d, true);
        out.set(index++, action.n2 / 10.0d, true);
        out.set(index++, action.n3 / 10.0d, true);
        double q1 = cache.atom(action.position, action.n1).q;
        double q2 = cache.atom(action.position, action.n2).q;
        double q3 = cache.atom(action.position, action.n3).q;
        out.set(index++, q1 + q2 + q3, true);
        out.set(index++, action.rank / 1200.0d, true);
        out.set(index++, (q1 + q2 + q3) / 3.0d, true);
        out.set(index++, Math.min(q1, Math.min(q2, q3)), true);
        out.set(index++, Math.max(q1, Math.max(q2, q3)), true);
        out.set(index++, cache.assignedNumber(action.position) == action.n1
                        || cache.assignedNumber(action.position) == action.n2
                        || cache.assignedNumber(action.position) == action.n3 ? 1.0d : 0.0d, true);
        // 73..82 collision/coverage and one location vote per position.
        int coverage = 0;
        boolean[] numbers = new boolean[11];
        for (int position = 1; position <= 10; position++) numbers[cache.assignedNumber(position)] = true;
        for (int number = 1; number <= 10; number++) if (numbers[number]) coverage++;
        out.set(index++, 10 - coverage, true);
        out.set(index++, coverage / 10.0d, true);
        for (int position = 1; position <= 10; position++)
            out.set(index++, cache.location(position, cache.assignedNumber(position)), true);
        // 82..95 identity, immutable-cache, and bounded issue-counter features.
        out.set(index++, binding.scope == DatasetIdentityGateR815.Scope.TRAIN ? 1.0d : 0.0d, true);
        out.set(index++, binding.scope == DatasetIdentityGateR815.Scope.VALIDATION ? 1.0d : 0.0d, true);
        out.set(index++, binding.scope == DatasetIdentityGateR815.Scope.SEALED_OOS ? 1.0d : 0.0d, true);
        out.set(index++, binding.scope == DatasetIdentityGateR815.Scope.FUTURE_LIVE ? 1.0d : 0.0d, true);
        out.set(index++, cache.dpCalls, true);
        out.set(index++, cache.voteCalls, true);
        out.set(index++, ResearchFeatureSchemaR815.VERSION, true);
        out.set(index++, hashFraction(binding.historyHash), true);
        out.set(index++, cache.subsetCount() / 1013.0d, true);
        out.set(index++, cache.assignmentScore / 10.0d, true);
        out.set(index++, hashFraction(cache.qHash), true);
        out.set(index++, hashFraction(binding.datasetSha256), true);
        out.set(index++, issueDayFraction(action.targetIssue), true);
        out.set(index++, issueSequenceFraction(action.targetIssue), true);
        if (index != ResearchFeatureSchemaR815.SIZE) throw new IllegalStateException("R815_FEATURE_INDEX_DRIFT_" + index);
        return out.build();
    }

    private static double entropy(CrossPositionPrimitiveCacheR815.Snapshot cache, int position) {
        double entropy = 0.0d;
        for (int number = 1; number <= 10; number++) {
            double value = cache.atom(position, number).q;
            if (value > 0.0d) entropy -= value * Math.log(value);
        }
        return entropy / Math.log(10.0d);
    }

    private static double hashFraction(String value) {
        try {
            long prefix = Long.parseLong(value.substring(0, 12), 16);
            return prefix / (double) 0xFFFFFFFFFFFFL;
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("R815_FEATURE_HASH_INVALID", failure);
        }
    }

    private static double issueDayFraction(String issue) {
        try {
            return Integer.parseInt(issue.substring(0, 8)) / 99999999.0d;
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("R815_FEATURE_ISSUE_DAY_INVALID", failure);
        }
    }

    private static double issueSequenceFraction(String issue) {
        try {
            return Integer.parseInt(issue.substring(8, 12)) / 1440.0d;
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("R815_FEATURE_ISSUE_SEQUENCE_INVALID", failure);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
