package com.ethk3.novaonepro.strategyv2;

/** Android thermal status values: 0 none .. 3 severe .. 6 shutdown. Headroom approaching 1 is hotter, not cooler.
 * Pure governor, sampled by RuntimeCoordinatorR26; unknown/NaN headroom falls back to the system status. */
public final class ThermalPolicyR29 {
    public static final long RECOVERY_MS = 30_000;
    private boolean limited, paused; private long coolSince = -1;
    public int workers = 2; public String reason = "NORMAL";

    public void update(int status, double headroom, long monoMs) {
        boolean known = Double.isFinite(headroom) && headroom >= 0;
        boolean hot = status >= 2 || (known && headroom >= .85);
        boolean critical = status >= 4;
        if (hot || critical) { limited = true; paused = critical; coolSince = -1; }
        else if (limited) {
            boolean cool = status >= 0 && status <= 1 && (!known || headroom < .7);
            if (!cool) coolSince = -1;
            else {
                if (coolSince < 0) coolSince = monoMs;
                if (monoMs - coolSince >= RECOVERY_MS) { limited = false; paused = false; coolSince = -1; }
            }
        }
        // A critical status can cool to severe: resume bounded work without waiting for a full-temperature reset.
        if (status >= 0 && status < 4) paused = false;
        workers = limited ? 1 : 2;
        reason = paused ? "THERMAL_CRITICAL" : limited ? "THERMAL_LIMITED" : status < 0 && !known ? "THERMAL_UNKNOWN" : "NORMAL";
    }
    public boolean paused() { return paused; }
}
