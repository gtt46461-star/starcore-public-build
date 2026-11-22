package com.ethk3.novaonepro;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class WarmShadowCycleTest {
    @Test public void installedSchemaThenOriginalThreePredraws() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        BootReceiver.persistServiceUserEnabled(context, false);
        BootReceiver.cancelAllRecovery(context, "ISOLATED_SCHEMA_PREPARATION");
        context.stopService(new Intent(context, AutoFetchService.class));
        DbStore store = new DbStore(context);
        try {
            long deadline = SystemClock.elapsedRealtime() + 300000;
            while (!store.ensureResearchSidecarR811()) {
                assertTrue(ResearchSchemaCoordinatorR926.summary(), SystemClock.elapsedRealtime() < deadline);
                SystemClock.sleep(8);
            }
        } finally {
            store.close();
        }
        new ShadowCycleAndroidTest().eachPredrawSettlesOnceAndFeedsTheNextCycle();
    }
}
