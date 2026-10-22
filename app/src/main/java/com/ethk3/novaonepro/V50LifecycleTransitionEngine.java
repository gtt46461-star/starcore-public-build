package com.ethk3.novaonepro;

/** Explicit-duration lifecycle mapping from real historical state runs and hazard. */
public final class V50LifecycleTransitionEngine {
    public V50Enums.Lifecycle resolve(
            int runLength,
            double strength,
            double strengthDelta,
            double hazard,
            boolean previousBreak) {
        if (previousBreak && hazard < 0.48d) return V50Enums.Lifecycle.RECOVER;
        if (hazard >= 0.72d) return V50Enums.Lifecycle.BREAK;
        if (strengthDelta < -0.10d && runLength >= 4) return V50Enums.Lifecycle.EXHAUST;
        if (runLength <= 2) return V50Enums.Lifecycle.ENTRY;
        if (runLength <= 5 && strengthDelta >= -0.02d) return V50Enums.Lifecycle.GROW;
        if (runLength >= 5 && strength >= 0.42d) return V50Enums.Lifecycle.MATURE;
        return strengthDelta < 0.0d ? V50Enums.Lifecycle.EXHAUST : V50Enums.Lifecycle.GROW;
    }
}
