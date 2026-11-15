package com.ethk3.novaonepro.strategyv2;

/** Android thermal status values: 0 none .. 3 severe .. 6 shutdown. Headroom approaching 1 is hotter, not cooler.
 * Pure governor, sampled by RuntimeCoordinatorR26.
 * R30-B01-06: headroom NaN/negative is unknown and carries no evidence (the system status still does; with both
 * unknown the current worker count is kept, never dropped to 0 or raised to the cap). Headroom >=0.95 -> 1 worker,
 * >=0.85 -> 2, otherwise the configured count. Moving to more workers needs 3 consecutive cooler readings spanning
 * at least RECOVERY_MS (hysteresis); moving to fewer applies at once. */
public final class ThermalPolicyR29 {
    public static final long RECOVERY_MS = 30_000;
    public static final int CONFIGURED_WORKERS = 2, RECOVERY_READINGS = 3;
    private boolean paused; private long coolSince = -1; private int coolReadings;
    public int workers = CONFIGURED_WORKERS; public String reason = "NORMAL";

    public void update(int status, double headroom, long monoMs) {
        boolean known = Double.isFinite(headroom) && headroom >= 0;
        int target;
        if (status >= 2 || (known && headroom >= .95)) target = 1;
        else if (known && headroom >= .85) target = 2;
        else if (status < 0 && !known) target = -1;
        else target = CONFIGURED_WORKERS;
        if (status >= 4) paused = true;
        // A critical status can cool to severe: resume bounded work without waiting for a full-temperature reset.
        else if (status >= 0) paused = false;
        if (target < 0 || target == workers) { coolReadings = 0; coolSince = -1; }
        else if (target < workers) { workers = target; coolReadings = 0; coolSince = -1; }
        else {
            if (coolReadings++ == 0) coolSince = monoMs;
            if (coolReadings >= RECOVERY_READINGS && monoMs - coolSince >= RECOVERY_MS) { workers = target; coolReadings = 0; coolSince = -1; }
        }
        reason = paused ? "THERMAL_CRITICAL" : workers < CONFIGURED_WORKERS ? "THERMAL_LIMITED" : status < 0 && !known ? "THERMAL_UNKNOWN" : "NORMAL";
    }
    public boolean paused() { return paused; }
}
