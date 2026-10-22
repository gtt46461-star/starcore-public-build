package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/** Non-destructive V47/V48/V49 tagging and pure V50 cohort activation. */
public final class BuildCohortMigrationEngine {
    public static final String MODULE_VERSION = "V50_BUILD_COHORT_MIGRATION_20260724_50072403";

    public static final class MigrationReport {
        public final boolean success;
        public final int legacyDecisionsTagged;
        public final int legacyTradesTagged;
        public final String detail;

        MigrationReport(
                boolean success, int legacyDecisionsTagged, int legacyTradesTagged, String detail) {
            this.success = success;
            this.legacyDecisionsTagged = legacyDecisionsTagged;
            this.legacyTradesTagged = legacyTradesTagged;
            this.detail = detail == null ? "" : detail;
        }
    }

    private BuildCohortMigrationEngine() {}

    public static void migrate(DbStore store) {
        migrateWithReport(store);
    }

    @Deprecated
    public static MigrationReport migrateWithReport(DbStore store) {
        return new MigrationReport(false, 0, 0,
                "CP5_LEGACY_COHORT_MUTATOR_RETIRED_READ_ONLY");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
