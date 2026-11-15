package com.ethk3.novaonepro;

/** Looks up a proven OOS policy cell without frequency-debt rescue. */
public final class V51PolicySurfaceEngine {
    public static final double MIN_OOS_HIT_RATE = 0.40d;
    public static final double MIN_OOS_COVERAGE = 0.15d;
    public static final double MAX_OOS_ECE = 0.04d;

    public V51PolicyCell match(
            V51Models.SelectedActionEnvelope e, V51EffectiveConsensusEngine.Consensus c) {
        ModelBundleParametersR932.Policy policy = ModelBundleRuntimeR932.policy();
        // E04F: the point-value policy is a JOINT contract. A good-looking cell cannot bypass
        // an unproven/under-covered/miscalibrated policy generation, and low frequency is never
        // repaid by lowering quality or creating a later debt action.
        if (!jointQualityPassed(
                policy.MODEL_GATE_PASSED,
                policy.OOS_COVERAGE,
                policy.OOS_HIT_RATE,
                policy.OOS_ECE)) return null;
        for (int i = 0; i < policy.CELLS.length; i++) {
            V51PolicyCell cell = policy.CELLS[i];
            if (matches(cell, e, c)) return cell;
        }
        return null;
    }

    /** Exact admission boundary shared by production lookup and deterministic verification. */
    public static boolean jointQualityPassed(
            boolean modelGatePassed, double coverage, double hitRate, double ece) {
        return modelGatePassed
                && finite01(coverage)
                && finite01(hitRate)
                && finite01(ece)
                && coverage >= MIN_OOS_COVERAGE
                && hitRate >= MIN_OOS_HIT_RATE
                && ece <= MAX_OOS_ECE;
    }

    private boolean matches(
            V51PolicyCell cell,
            V51Models.SelectedActionEnvelope e,
            V51EffectiveConsensusEngine.Consensus c) {
        if (!cell.active
                || e == null
                || e.domain == null
                || e.action == null
                || e.predictability == null) return false;
        return e.domain.domainId.startsWith(cell.domainPrefix)
                && (cell.position == 0 || cell.position == e.action.position)
                && e.selectedHitP >= cell.selectedPMin
                && e.predictability.probability >= cell.predictabilityMin
                && e.action.rankMargin >= cell.rankMarginMin
                && e.action.thirdMargin >= cell.thirdMarginMin
                && c.effective >= cell.consensusMin
                && e.uncertainty <= cell.uncertaintyMax
                && e.falsifierScore <= cell.falsifierMax
                && cell.sample >= 30
                && cell.hitRate >= MIN_OOS_HIT_RATE
                && cell.credibleLow >= 0.30d
                && cell.ece <= MAX_OOS_ECE
                && cell.blockMinimum >= 0.37d;
    }

    private static boolean finite01(double value) {
        return Double.isFinite(value) && value >= 0.0d && value <= 1.0d;
    }
}
