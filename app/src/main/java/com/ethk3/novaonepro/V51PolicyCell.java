package com.ethk3.novaonepro;

/** Immutable selective-risk cell learned only from earlier calibrated OOS actions. */
public final class V51PolicyCell {
    public final String id, domainPrefix;
    public final V51Enums.Lifecycle lifecycle;
    public final int position, sample;
    public final double selectedPMin,
            predictabilityMin,
            rankMarginMin,
            thirdMarginMin,
            consensusMin,
            uncertaintyMax,
            falsifierMax,
            hitRate,
            credibleLow,
            ece,
            blockMinimum,
            maxDrift;
    public final boolean active;

    public V51PolicyCell(
            String id,
            String domainPrefix,
            V51Enums.Lifecycle lifecycle,
            int position,
            double selectedPMin,
            double predictabilityMin,
            double rankMarginMin,
            double thirdMarginMin,
            double consensusMin,
            double uncertaintyMax,
            double falsifierMax,
            int sample,
            double hitRate,
            double credibleLow,
            double ece,
            double blockMinimum,
            double maxDrift,
            boolean active) {
        this.id = id == null ? "" : id;
        this.domainPrefix = domainPrefix == null ? "" : domainPrefix;
        this.lifecycle = lifecycle == null ? V51Enums.Lifecycle.UNKNOWN : lifecycle;
        this.position = position;
        this.selectedPMin = selectedPMin;
        this.predictabilityMin = predictabilityMin;
        this.rankMarginMin = rankMarginMin;
        this.thirdMarginMin = thirdMarginMin;
        this.consensusMin = consensusMin;
        this.uncertaintyMax = uncertaintyMax;
        this.falsifierMax = falsifierMax;
        this.sample = sample;
        this.hitRate = hitRate;
        this.credibleLow = credibleLow;
        this.ece = ece;
        this.blockMinimum = blockMinimum;
        this.maxDrift = maxDrift;
        this.active = active;
    }

        /** R33.5 exact prepared-payload hydration; invoked only by the closed codec.
         * It restores frozen fields without consulting mutable current model state. */
        V51PolicyCell(PreparedCanonicalCodecR335.Decoded decodedR335) {
            this.id = (String) decodedR335.value(0);
            this.domainPrefix = (String) decodedR335.value(1);
            this.lifecycle = (V51Enums.Lifecycle) decodedR335.value(2);
            this.position = ((Integer) decodedR335.value(3)).intValue();
            this.sample = ((Integer) decodedR335.value(4)).intValue();
            this.selectedPMin = ((Double) decodedR335.value(5)).doubleValue();
            this.predictabilityMin = ((Double) decodedR335.value(6)).doubleValue();
            this.rankMarginMin = ((Double) decodedR335.value(7)).doubleValue();
            this.thirdMarginMin = ((Double) decodedR335.value(8)).doubleValue();
            this.consensusMin = ((Double) decodedR335.value(9)).doubleValue();
            this.uncertaintyMax = ((Double) decodedR335.value(10)).doubleValue();
            this.falsifierMax = ((Double) decodedR335.value(11)).doubleValue();
            this.hitRate = ((Double) decodedR335.value(12)).doubleValue();
            this.credibleLow = ((Double) decodedR335.value(13)).doubleValue();
            this.ece = ((Double) decodedR335.value(14)).doubleValue();
            this.blockMinimum = ((Double) decodedR335.value(15)).doubleValue();
            this.maxDrift = ((Double) decodedR335.value(16)).doubleValue();
            this.active = ((Boolean) decodedR335.value(17)).booleanValue();
        }
}
