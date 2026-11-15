package com.ethk3.novaonepro;

/** Enforces that proof artifacts are tied to the current Binance game and an immutable dataset. */
public final class DatasetIdentityGateR815 {
    public enum Scope {
        TRAIN,
        VALIDATION,
        SEALED_OOS,
        FUTURE_LIVE,
        LEGACY_CROSS_GAME_HYPOTHESIS_ONLY
    }

    public static final class Manifest {
        public final String gameCode;
        public final Scope scope;
        public final String firstIssue;
        public final String lastIssue;
        public final long rowCount;
        public final String datasetSha256;
        public final String schemaSha256;
        public final String producerCommit;

        public Manifest(
                String gameCode,
                Scope scope,
                String firstIssue,
                String lastIssue,
                long rowCount,
                String datasetSha256,
                String schemaSha256,
                String producerCommit) {
            this.gameCode = safe(gameCode);
            this.scope = scope;
            this.firstIssue = safe(firstIssue);
            this.lastIssue = safe(lastIssue);
            this.rowCount = rowCount;
            this.datasetSha256 = safe(datasetSha256);
            this.schemaSha256 = safe(schemaSha256);
            this.producerCommit = safe(producerCommit);
        }

        public String canonical() {
            return gameCode
                    + '|'
                    + (scope == null ? "" : scope.name())
                    + '|'
                    + firstIssue
                    + '|'
                    + lastIssue
                    + '|'
                    + rowCount
                    + '|'
                    + datasetSha256
                    + '|'
                    + schemaSha256
                    + '|'
                    + producerCommit;
        }
    }

    private DatasetIdentityGateR815() {}

    public static void requireForProof(Manifest manifest) {
        validate(manifest, false);
    }

    public static boolean allowHypothesis(Manifest manifest) {
        try {
            validate(manifest, true);
            return true;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public static String artifactBinding(Manifest manifest) {
        requireForProof(manifest);
        return V51ChecksumEngine.sha256("R815_DATASET_BINDING|" + manifest.canonical());
    }

    private static void validate(Manifest manifest, boolean hypothesisAllowed) {
        if (manifest == null || manifest.scope == null) fail("MANIFEST_MISSING");
        if (!ReleaseIdentityR815.GAME_CODE.equals(manifest.gameCode)) {
            if (hypothesisAllowed
                    && manifest.scope == Scope.LEGACY_CROSS_GAME_HYPOTHESIS_ONLY
                    && manifest.gameCode.length() > 0) {
                validateCommon(manifest);
                return;
            }
            fail("GAME_MISMATCH");
        }
        if (manifest.scope == Scope.LEGACY_CROSS_GAME_HYPOTHESIS_ONLY) fail("HYPOTHESIS_ONLY");
        validateCommon(manifest);
    }

    private static void validateCommon(Manifest manifest) {
        if (!TargetIssueResolver.isValid(manifest.firstIssue)
                || !TargetIssueResolver.isValid(manifest.lastIssue)
                || TargetIssueResolver.isAfter(manifest.firstIssue, manifest.lastIssue)) {
            fail("ISSUE_RANGE");
        }
        if (manifest.rowCount <= 0L) fail("ROW_COUNT");
        if (!sha256(manifest.datasetSha256) || !sha256(manifest.schemaSha256)) fail("HASH");
        if (manifest.producerCommit.length() < 7 || manifest.producerCommit.length() > 128)
            fail("PRODUCER_COMMIT");
    }

    private static boolean sha256(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static void fail(String code) {
        throw new IllegalStateException("R815_DATASET_" + code);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
