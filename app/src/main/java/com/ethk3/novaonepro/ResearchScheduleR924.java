package com.ethk3.novaonepro;

/** Persistent round-robin policy. A yielded lane cannot repeatedly monopolize the first slot. */
public final class ResearchScheduleR924 {
    private ResearchScheduleR924() {}

    public static int phase(String stored, int lanes) {
        if (lanes < 1) throw new IllegalArgumentException("LANES_MUST_BE_POSITIVE");
        try { return Math.floorMod(Integer.parseInt(stored), lanes); }
        catch (RuntimeException malformed) { return 0; }
    }

    public static int next(int phase, int lanes) {
        return Math.floorMod(phase + 1, lanes);
    }

    public static long slice(long remainingMs, int remainingLanes, long maximumMs) {
        if (remainingMs <= 0 || remainingLanes <= 0) return 0;
        return Math.min(remainingMs, Math.min(maximumMs, Math.max(80L, remainingMs / remainingLanes)));
    }

    /** A retention/repair count decrease is a new baseline, not an infinite negative interval. */
    public static boolean generationDue(int samples, int last, int active, int interval) {
        return active == 0 || last < 0 || samples < last || samples - last >= interval;
    }

    /** Fresh heartbeats alone do not prove that the latest prediction was committed. */
    public static String predictionState(String latestDraw, String committed, long drawAgeMs) {
        TargetIssueResolver.Resolution next = TargetIssueResolver.next(latestDraw);
        if (!next.valid) return "DRAW_ISSUE_UNAVAILABLE";
        if (!TargetIssueResolver.isValid(committed)) return "NO_COMMITTED_PREDICTION";
        int distance = TargetIssueResolver.distance(committed, next.targetIssue);
        if (distance < 0) return "PREDICTION_AHEAD_OF_SOURCE";
        if (distance == 0) return "CURRENT_COMMITTED";
        if (distance == 1 && drawAgeMs >= 0L && drawAgeMs <= 5000L) return "NEXT_PREDICTION_DUE";
        return "PREDICTION_STALE_" + distance + "_ISSUES";
    }
}
