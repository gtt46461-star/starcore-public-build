package com.ethk3.novaonepro.strategyv2;

/** Pure clock policy. A probe permits one initialization attempt, never READY or REAL. */
public final class StartupProbePolicyR343 {
    private StartupProbePolicyR343() {}
    public static final long RETRY_MS = 300_000L;
    public static final int MAX_PROBES_PER_DEPENDENCY = 3;

    public static boolean hasProbeBudget(int previousAttempts) {
        return previousAttempts >= 0 && previousAttempts < MAX_PROBES_PER_DEPENDENCY;
    }

    public static boolean due(String boot, long now, String previousBoot, long previousMono) {
        if (boot == null || boot.isEmpty() || "UNRESOLVED".equals(boot) || now < 0) return false;
        // A fresh boot can make one durable probe. It cannot inherit elapsed time.
        if (previousBoot == null || !boot.equals(previousBoot)) return true;
        return previousMono >= 0 && now >= previousMono && now - previousMono >= RETRY_MS;
    }
}
