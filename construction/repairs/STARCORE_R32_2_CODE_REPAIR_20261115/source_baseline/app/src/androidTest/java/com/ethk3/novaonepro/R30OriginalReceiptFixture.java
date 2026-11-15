package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952;

/** Test setup only: install original transaction receipt units, without claiming the full research domain is ready. */
public final class R30OriginalReceiptFixture {
    private static final String UNIT = "F05_EXACT_CALIBRATED_REAL_RECEIPT_V1";
    private R30OriginalReceiptFixture() {}
    public static void schema(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R30_RECEIPT_FIXTURE_REQUIRES_ORIGINAL_WRITER_TX");
        ResearchPlatformStoreR929.createSchema(db);
        FixedResearchWindowR932.schema(db);
        SchemaDomainCoordinatorR926.Contract contract = SchemaDomainCoordinatorR926.researchContract();
        for (int unit = 0; unit < 64; unit++) {
            if (SchemaDomainCoordinatorR926.stepComplete(db, contract, UNIT)) {
                try (Cursor rows = db.rawQuery("SELECT count(*) FROM sqlite_master WHERE name IN ('r974_exact_real_receipt','r974_real_no_update','research_exact_probability_f05','f05_exact_probability_no_update','f05_exact_probability_no_replace','f05_cal_model_no_update','f05_cal_frozen_obs_no_update')", null)) {
                    if (!rows.moveToFirst() || rows.getInt(0) != 7) throw new IllegalStateException("R30_ORIGINAL_RECEIPT_OBJECTS_MISSING");
                }
                return;
            }
            ResearchMigrationRegistryR952.advanceR64(db);
        }
        throw new IllegalStateException("R30_ORIGINAL_RECEIPT_UNIT_NOT_COMPLETED");
    }
}
