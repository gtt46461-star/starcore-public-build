package com.ethk3.novaonepro;

/** Deterministic, side-effect-free database upgrade routing for the R3.6.3 startup path. */
public final class DatabaseUpgradePlanR363 {
    public enum Path {
        R35_TO_R36_ADDITIVE_ONLY,
        MODERN_RECONCILE,
        LEGACY_RECONCILE,
        NO_UPGRADE,
        REJECT_DOWNGRADE
    }

    public static final class Plan {
        public final int oldVersion, newVersion;
        public final Path path;
        public final boolean runV50Migration;
        public final boolean createR35;
        public final boolean createR36;
        public final boolean destructive;

        Plan(
                int oldVersion,
                int newVersion,
                Path path,
                boolean runV50Migration,
                boolean createR35,
                boolean createR36) {
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
            this.path = path;
            this.runV50Migration = runV50Migration;
            this.createR35 = createR35;
            this.createR36 = createR36;
            this.destructive = false;
        }

        public String canonical() {
            return oldVersion
                    + "->"
                    + newVersion
                    + '|'
                    + path
                    + "|v50="
                    + runV50Migration
                    + "|r35="
                    + createR35
                    + "|r36="
                    + createR36
                    + "|destructive="
                    + destructive;
        }
    }

    private DatabaseUpgradePlanR363() {}

    public static Plan select(int oldVersion, int newVersion) {
        if (newVersion < oldVersion)
            return new Plan(
                    oldVersion, newVersion, Path.REJECT_DOWNGRADE, false, false, false);
        if (newVersion == oldVersion)
            return new Plan(oldVersion, newVersion, Path.NO_UPGRADE, false, false, false);
        /* DB35 already passed the V50 migration/integrity gate.  Re-running the historical full
         * scan while moving to DB36 only delays foreground readiness and cannot add R36 evidence.
         * This exact route is additive-only and preserves every historical row. */
        if (oldVersion == 35 && newVersion == 36)
            return new Plan(
                    oldVersion,
                    newVersion,
                    Path.R35_TO_R36_ADDITIVE_ONLY,
                    false,
                    true,
                    true);
        if (oldVersion >= 34)
            return new Plan(
                    oldVersion,
                    newVersion,
                    Path.MODERN_RECONCILE,
                    true,
                    newVersion >= 35,
                    newVersion >= 36);
        return new Plan(
                oldVersion,
                newVersion,
                Path.LEGACY_RECONCILE,
                true,
                newVersion >= 35,
                newVersion >= 36);
    }
}
