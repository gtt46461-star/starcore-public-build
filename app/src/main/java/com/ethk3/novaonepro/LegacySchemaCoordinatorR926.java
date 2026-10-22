package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

/** R926 lazy Legacy schema owner. Legacy failure is observable and never marks Critical as failed. */
public final class LegacySchemaCoordinatorR926 {
    private LegacySchemaCoordinatorR926() {}

    public static boolean ensure(DatabaseCoordinator owner, final DatabaseCoordinator.SchemaInitializer initializer) {
        if (owner == null || initializer == null) return false;
        SQLiteDatabase db = owner.writable();
        final SchemaDomainCoordinatorR926.Contract c = SchemaDomainCoordinatorR926.legacyContract();
        if (SchemaDomainCoordinatorR926.matches(db, c)) return true;
        try {
            SchemaDomainCoordinatorR926.prepareRepairIfDrift(db, c);
            SchemaDomainCoordinatorR926.markState(db, c, "INITIALIZING", "LEGACY_RUNTIME", "");
            SchemaDomainCoordinatorR926.runStep(db, c, "LEGACY_RUNTIME",
                    new SchemaDomainCoordinatorR926.Step() { public void run(SQLiteDatabase x) throws Exception { initializer.run(x); }});
            SchemaDomainCoordinatorR926.verifyAndStamp(db, c);
            return true;
        } catch (Throwable failure) {
            SchemaDomainCoordinatorR926.markState(db, c, "FAILED", "LEGACY_RUNTIME",
                    failure.getClass().getSimpleName() + ':' + String.valueOf(failure.getMessage()));
            return false;
        }
    }
}
