package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.IOException;
import java.util.*;

/** Compatibility API. Continuous sample-level research belongs to FoundryCampaignR26.
 * Legacy campaign rows and evolution state remain readable for audit/parent protection. */
public final class ModelFoundryJobSU2 {
    private ModelFoundryJobSU2() {}
    public static final int GENERATIONS = 8, POPULATION = 24, HALL_OF_FAME = 12, VAL_LEN = 1200, REFRESH_EVERY = 300;
    public static final long MODEL_BYTES = 96L << 20;

    /** Compatibility wake-up; the complete legacy engine is owned by LegacyGenerationR26. */
    @Deprecated public static String slice(DbStore store, String target) {
        RuntimeCoordinatorR26.wake();
        return "SU2_FOUNDRY_OWNED_BY_R26_CONTINUOUS_LEGACY_GENERATION";
    }

    static long seed(String campaign, int generation) { return Long.parseLong(campaign.substring(0, 15), 16) ^ (0x9E3779B97F4A7C15L * (generation + 1)); }
    static String running(SQLiteDatabase db) { try (Cursor c = db.rawQuery("SELECT campaign_id FROM su2_campaign WHERE status='RUNNING' ORDER BY started_at DESC LIMIT 1", null)) { return c.moveToFirst() ? c.getString(0) : null; } }
    static long lastThrough(SQLiteDatabase db) { try (Cursor c = db.rawQuery("SELECT max(through_sequence) FROM su2_campaign WHERE status='DONE'", null)) { return c.moveToFirst() && !c.isNull(0) ? c.getLong(0) : -1; } }
    static int generationsDone(SQLiteDatabase db, String campaign) {
        try (Cursor c = db.rawQuery("SELECT evaluated,population FROM su2_campaign WHERE campaign_id=?", new String[]{campaign})) { return c.moveToFirst() ? c.getInt(0) / Math.max(1, c.getInt(1)) : 0; }
    }
}
