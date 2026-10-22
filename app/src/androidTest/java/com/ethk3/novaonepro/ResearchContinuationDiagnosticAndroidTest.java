package com.ethk3.novaonepro;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.r952.AtomCatalogImportCP12;
import com.ethk3.novaonepro.r952.ModernSeedFactoryCP12;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Retained-fixture diagnostic only; never substitutes for the cold600-second acceptance test. */
@RunWith(AndroidJUnit4.class)
public final class ResearchContinuationDiagnosticAndroidTest {
    private static long count(DbStore store,String table) {
        try(Cursor c=store.readable().rawQuery("SELECT COUNT(*) FROM "+table,null)) {
            assertTrue(c.moveToFirst());return c.getLong(0);
        }
    }
    private static void progress(DbStore store,int pulses,String status) {
        System.out.println("CONTINUATION_DIAGNOSTIC|pulses="+pulses
                +"|atoms="+count(store,"research_atom_spec_v2")
                +"|seeds="+count(store,"research_molecule_seed_v2")
                +"|draws="+count(store,"draws")+"|trades="+count(store,"trades")
                +"|status="+status+"|scope=RETAINED_FIXTURE_NOT_COLD_ACCEPTANCE");
    }
    @Test public void retainedFoundationResumesThroughPublicMaintenanceOwner() {
        Context context=ApplicationProvider.getApplicationContext();
        BootReceiver.persistServiceUserEnabled(context,false);
        BootReceiver.cancelAllRecovery(context,"SYNTHETIC_CONTINUATION_DIAGNOSTIC");
        context.stopService(new Intent(context,AutoFetchService.class));
        long stopBy=SystemClock.elapsedRealtime()+10000L;
        while(AnalysisSingleFlightR36.shared().currentLease()!=null
                &&SystemClock.elapsedRealtime()<stopBy)SystemClock.sleep(20L);
        assertNull(AnalysisSingleFlightR36.shared().currentLease());
        DbStore store=new DbStore(context);int pulses=0;String last="START";
        try {
            long deadline=SystemClock.elapsedRealtime()+600000L;
            while(!store.ensureResearchSidecarR811()) {
                assertTrue(ResearchSchemaCoordinatorR926.summary(),SystemClock.elapsedRealtime()<deadline);
                SystemClock.sleep(20L);
            }
            assertEquals("retained synthetic history required",600,count(store,"draws"));
            progress(store,pulses,last);
            while(!AtomCatalogImportCP12.completeCP12(store.readable())
                    ||!ModernSeedFactoryCP12.completeCP12(store.readable())) {
                assertTrue("unfinished foundation lost: "+last,ResearchLearningContinuityR912.needsContinuation(store));
                last=ResearchLearningContinuityR912.pulse(store,1000L).status;pulses++;
                if(pulses%25==0)progress(store,pulses,last);
                assertTrue("diagnostic600-second slice incomplete: "+last,SystemClock.elapsedRealtime()<deadline);
                SystemClock.sleep(ResearchLearningContinuityR912.nextContinuationDelayMs(store));
            }
            assertEquals(25350,count(store,"research_atom_spec_v2"));
            assertEquals(4200,count(store,"research_molecule_seed_v2"));
            assertEquals(0,count(store,"trades"));
            try(Cursor c=store.readable().rawQuery("PRAGMA integrity_check",null)) {
                assertTrue(c.moveToFirst());assertEquals("ok",c.getString(0));
            }
            last="RETAINED_FOUNDATION_COMPLETE";
        } finally {progress(store,pulses,last);store.close();}
    }
}
