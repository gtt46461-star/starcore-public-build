package com.ethk3.novaonepro;

import android.database.Cursor;

/** Walk-forward gate. It can only make a candidate eligible for SHADOW promotion, never REAL. */
public final class ResearchPromotionVerifierR815 {
    public enum VerdictCode {
        NOT_PROVEN,
        ELIGIBLE_FOR_SHADOW_PROMOTION,
        DATA_IDENTITY_INVALID,
        SAMPLE_INSUFFICIENT,
        METRIC_FAILED,
        FUTURE_LIVE_MISSING
    }

    public static final class Verdict {
        public final String candidateId;
        public final VerdictCode code;
        public final int issues;
        public final int hits;
        public final double hitRate;
        public final double meanBrier;
        public final double meanLogLoss;
        public final String certificateHash;

        Verdict(
                String candidateId,
                VerdictCode code,
                int issues,
                int hits,
                double meanBrier,
                double meanLogLoss) {
            this.candidateId = safe(candidateId);
            this.code = code == null ? VerdictCode.NOT_PROVEN : code;
            this.issues = Math.max(0, issues);
            this.hits = Math.max(0, hits);
            this.hitRate = issues == 0 ? 0.0d : hits / (double) issues;
            this.meanBrier = meanBrier;
            this.meanLogLoss = meanLogLoss;
            certificateHash =
                    V51ChecksumEngine.sha256(
                            this.candidateId
                                    + '|'
                                    + this.code.name()
                                    + '|'
                                    + this.issues
                                    + '|'
                                    + this.hits
                                    + '|'
                                    + this.meanBrier
                                    + '|'
                                    + this.meanLogLoss
                                    + "|SHADOW_ONLY");
        }
    }

    private final ResearchRepositoryR815 repository;
    private final DbStore store;

    public ResearchPromotionVerifierR815(DbStore store, ResearchRepositoryR815 repository) {
        if (store == null || repository == null) throw new IllegalArgumentException("R815_PROMOTION_ARGUMENT_NULL");
        this.store = store;
        this.repository = repository;
    }

    public Verdict evaluate(String candidateId) {
        String id = safe(candidateId);
        ResearchRepositoryR815.Candidate candidate = find(id);
        if (candidate == null) return new Verdict(id, VerdictCode.NOT_PROVEN, 0, 0, 0.0d, 0.0d);
        if (!ReleaseIdentityR815.GAME_CODE.equals(AppConfig.LOTTERY_CODE)
                || !ResearchFeatureSchemaR815.schemaSha256().equals(candidate.schemaSha256)
                || candidate.datasetSha256.length() != 64)
            return new Verdict(id, VerdictCode.DATA_IDENTITY_INVALID, 0, 0, 0.0d, 0.0d);
        Cursor cursor = null;
        int issues = 0, hits = 0;
        double brier = 0.0d, logLoss = 0.0d;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT hit,brier,log_loss FROM "
                                            + DbHelper.TABLE_R815_RESEARCH_EVAL
                                            + " WHERE candidate_id=? ORDER BY target_issue ASC",
                                    new String[] {id});
            while (cursor.moveToNext()) {
                issues++;
                hits += cursor.getInt(0) == 1 ? 1 : 0;
                brier += cursor.getDouble(1);
                logLoss += cursor.getDouble(2);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        if (issues < 120)
            return new Verdict(id, VerdictCode.SAMPLE_INSUFFICIENT, issues, hits, mean(brier, issues), mean(logLoss, issues));
        // Future-live evidence is intentionally unavailable to an offline build; therefore a
        // candidate cannot accidentally become proven merely by historical action rows.
        if (!hasFutureLiveCertificate(candidate))
            return new Verdict(id, VerdictCode.FUTURE_LIVE_MISSING, issues, hits, mean(brier, issues), mean(logLoss, issues));
        double hitRate = hits / (double) issues;
        if (hitRate < 0.30d || mean(brier, issues) > 0.25d || mean(logLoss, issues) > 1.15d)
            return new Verdict(id, VerdictCode.METRIC_FAILED, issues, hits, mean(brier, issues), mean(logLoss, issues));
        return new Verdict(
                id,
                VerdictCode.ELIGIBLE_FOR_SHADOW_PROMOTION,
                issues,
                hits,
                mean(brier, issues),
                mean(logLoss, issues));
    }

    public boolean transitionIfEligible(Verdict verdict) {
        if (verdict == null || verdict.code != VerdictCode.ELIGIBLE_FOR_SHADOW_PROMOTION) return false;
        // Research authority is an intrinsic class contract, independent of Production release
        // posture.  The highest verdict can only enter SHADOW promotion.
        if (!AlgorithmInventionLabR908.MAY_AUTHORIZE_REAL
                && !ResearchShadowChallengerRuntimeR817.MAY_AUTHORIZE_REAL) {
            return repository.transition(
                    verdict.candidateId,
                    ResearchRepositoryR815.State.ELIGIBLE_FOR_SHADOW_PROMOTION,
                    verdict.certificateHash);
        }
        return false;
    }

    private ResearchRepositoryR815.Candidate find(String candidateId) {
        ResearchRepositoryR815.Snapshot snapshot = repository.snapshot(64);
        for (int i = 0; i < snapshot.candidates.size(); i++) {
            ResearchRepositoryR815.Candidate candidate = snapshot.candidates.get(i);
            if (candidate.candidateId.equals(candidateId)) return candidate;
        }
        return null;
    }

    private static boolean hasFutureLiveCertificate(ResearchRepositoryR815.Candidate candidate) {
        // Future-live attestation is deliberately external and absent until independently imported.
        return candidate.state == ResearchRepositoryR815.State.ELIGIBLE_FOR_SHADOW_PROMOTION
                && !AppConfig.R815_MODEL_PROVEN;
    }

    private static double mean(double sum, int count) {
        return count <= 0 ? 0.0d : sum / count;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
