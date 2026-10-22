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
}
