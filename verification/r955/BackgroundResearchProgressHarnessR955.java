package com.ethk3.novaonepro;

public final class BackgroundResearchProgressHarnessR955 {
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println(name + "=PASS");
    }

    public static void main(String[] args) {
        long now = 1_000_000L;
        long stale = 180_000L;
        long grace = 45_000L;

        BackgroundResearchProgressContractR955.Result startup =
                BackgroundResearchProgressContractR955.evaluate(
                        now, now - 20_000L, now - 20_000L, 0L, now - 1_000L,
                        "POSTED_R955", stale, grace);
        check(!startup.stale() && "WAITING".equals(startup.state), "START_GRACE_NOT_FALSE_STALE");

        BackgroundResearchProgressContractR955.Result firstWindow =
                BackgroundResearchProgressContractR955.evaluate(
                        now, now - 100_000L, now - 100_000L, 0L, now - 1_000L,
                        "V51_OWNER_YIELD", stale, grace);
        check(!firstWindow.stale(), "FIRST_COMPLETION_GETS_NORMAL_STALE_WINDOW");

        BackgroundResearchProgressContractR955.Result maskedYield =
                BackgroundResearchProgressContractR955.evaluate(
                        now, now - 300_000L, now - 300_000L, 0L, now - 1_000L,
                        "V51_OWNER_YIELD", stale, grace);
        check(maskedYield.stale(), "FRESH_YIELD_CANNOT_MASK_MISSING_PROGRESS");
        check("RESEARCH_PROGRESS_STALE_AFTER_YIELD".equals(maskedYield.reason),
                "YIELD_STALE_REASON_EXPLICIT");

        BackgroundResearchProgressContractR955.Result freshProgress =
                BackgroundResearchProgressContractR955.evaluate(
                        now, now - 300_000L, now - 300_000L, now - 10_000L,
                        now - 1_000L, "POSTED_R955", stale, grace);
        check(!freshProgress.stale() && "OK".equals(freshProgress.state),
                "COMPLETED_PROGRESS_IS_HEALTHY");

        BackgroundResearchProgressContractR955.Result oldProgressFreshAttempts =
                BackgroundResearchProgressContractR955.evaluate(
                        now, now - 600_000L, now - 600_000L, now - 300_000L,
                        now - 500L, "POSTED_R955", stale, grace);
        check(oldProgressFreshAttempts.stale(), "FRESH_POSTS_CANNOT_MASK_OLD_PROGRESS");

        BackgroundResearchProgressContractR955.Result error =
                BackgroundResearchProgressContractR955.evaluate(
                        now, now - 600_000L, now - 600_000L, now - 300_000L,
                        now - 500L, "V51_OWNER_ERROR", stale, grace);
        check(error.stale() && "RESEARCH_PROGRESS_STALE_AFTER_ERROR".equals(error.reason),
                "ERROR_STALE_REASON_EXPLICIT");

        System.out.println("R955_BACKGROUND_RESEARCH_PROGRESS_CONTRACT_PASS|attempt_ne_progress=TRUE");
    }
}
