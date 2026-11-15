package com.ethk3.novaonepro;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.r952.AtomCatalogImportCP12;
import com.ethk3.novaonepro.r952.ModernSeedFactoryCP12;
import java.util.ArrayList;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Cold catalogs must progress through the production scheduler, not a direct import loop. */
@RunWith(AndroidJUnit4.class)
public final class ResearchContinuationAndroidTest {
    private static long count(DbStore store, String table) {
        try (Cursor c = store.readable().rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(c.moveToFirst());
            return c.getLong(0);
        }
    }

    @Test public void coldFoundationResumesThroughMaintenanceOwner() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        BootReceiver.persistServiceUserEnabled(context, false);
        BootReceiver.cancelAllRecovery(context, "SYNTHETIC_CONTINUATION_FIXTURE");
        context.stopService(new Intent(context, AutoFetchService.class));
        long stopBy = SystemClock.elapsedRealtime() + 10000L;
        while (AnalysisSingleFlightR36.shared().currentLease() != null
                && SystemClock.elapsedRealtime() < stopBy) SystemClock.sleep(20L);
        assertNull(AnalysisSingleFlightR36.shared().currentLease());
        DbStore store = new DbStore(context);
        try {
            long deadline = SystemClock.elapsedRealtime() + 600000L;
            while (!store.ensureResearchSidecarR811()) {
                assertTrue(ResearchSchemaCoordinatorR926.summary(),
                        SystemClock.elapsedRealtime() < deadline);
                SystemClock.sleep(20L);
            }
            assertEquals(0, count(store, "research_atom_spec_v2"));
            assertEquals(0, count(store, "research_molecule_seed_v2"));
            ArrayList<Models.Draw> history = new ArrayList<>();
            long latest = System.currentTimeMillis();
            for (int row = 1; row <= 600; row++) {
                Models.Draw draw = new Models.Draw();
                draw.issue = String.format(Locale.US, "20260910%04d", row);
                draw.openTime = latest - (600 - row) * 60000L;
                for (int p = 0; p < 10; p++) draw.numbers[p] = ((p + row - 1) % 10) + 1;
                assertTrue(draw.isValid());
                history.add(draw);
            }
            assertEquals(600, store.saveDraws(history));
            int pulses = 0;
            String last = "NOT_STARTED";
            while (!AtomCatalogImportCP12.completeCP12(store.readable())
                    || !ModernSeedFactoryCP12.completeCP12(store.readable())) {
                assertTrue("scheduler lost unfinished foundation: " + last,
                        ResearchLearningContinuityR912.needsContinuation(store));
                last = ResearchLearningContinuityR912.pulse(store, 1000L).status;
                pulses++;
                if (pulses % 25 == 0) System.out.println("CONTINUATION_PROGRESS|pulses=" + pulses
                        + "|atoms=" + count(store, "research_atom_spec_v2")
                        + "|seeds=" + count(store, "research_molecule_seed_v2") + "|" + last);
                assertTrue("scheduler did not finish catalogs: " + last,
                        SystemClock.elapsedRealtime() < deadline);
                SystemClock.sleep(ResearchLearningContinuityR912.nextContinuationDelayMs(store));
            }
            assertEquals(25350, count(store, "research_atom_spec_v2"));
            assertEquals(4200, count(store, "research_molecule_seed_v2"));
            assertEquals(0, count(store, DbHelper.TABLE_TRADES));
            try (Cursor c = store.readable().rawQuery("PRAGMA integrity_check", null)) {
                assertTrue(c.moveToFirst()); assertEquals("ok", c.getString(0));
            }
            System.out.println("CONTINUATION_ANDROID_PASS|atoms=25350|seeds=4200|pulses="
                    + pulses + "|scope=SYNTHETIC_SCHEDULER_NOT_NATURAL_NOT_PHONE");
        } finally { store.close(); }
    }
}
