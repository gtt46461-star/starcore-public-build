package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Disposable emulator real process/engine boundary. No synthetic predictions,
 * business DDL, source rows or device IO-bound/continuous-runtime claim. */
@RunWith(AndroidJUnit4.class)
public final class R31StorageCheckpointAndroidTest {
    static Context app(){assertEquals("true",InstrumentationRegistry.getArguments().getString("r31qRealService"));assertEquals("true",InstrumentationRegistry.getArguments().getString("r31WalDisposable"));return ApplicationProvider.getApplicationContext();}
    @Test public void passiveUsesRealBoundDiagnosticProcessAndPreservesPlatformDatabase()throws Exception{
        Context app=app();DbStore store=new DbStore(app);try{
            File file=new File(store.readable().getPath());String platform;
            try(Cursor c=store.readable().rawQuery("SELECT sqlite_version()",null)){assertTrue(c.moveToFirst());platform=c.getString(0);}
            Map<String,Object> actual=null;boolean executed=false;
            // A cold process can consume its original 40ms native slice loading
            // the library. Retry only after that actual owner has ended; every
            // new operation keeps the same service budget and admission gates.
            for(int attempt=0;attempt<3&&!executed;attempt++){
                StorageCheckpointClientR31.passive(store.contextForR26(),file,TimeSourceR811.monoNowMs()+5000);
                long end=TimeSourceR811.monoNowMs()+10000;
                do{actual=StorageCheckpointClientR31.snapshot();if(!Boolean.TRUE.equals(actual.get("inFlight")))break;Thread.sleep(20);}while(TimeSourceR811.monoNowMs()<end);
                assertFalse("timeout is not stopped; actual owner did not finish",Boolean.TRUE.equals(actual.get("inFlight")));
                executed="ACTUAL_PASSIVE_RESULT".equals(actual.get("state"))&&Boolean.TRUE.equals(actual.get("operationAttempted"));
            }
            assertTrue("NOT_RUN: no actual native PASSIVE after three bounded owners: "+actual,executed);assertEquals(true,actual.get("ownerCompleted"));
            assertEquals(3053004L,((Number)actual.get("nativeSqliteVersion")).longValue());long rc=((Number)actual.get("nativeCode")).longValue();assertTrue("Native checkpoint failed: "+rc,rc==0||rc==5||rc==6);
            try(Cursor c=store.readable().rawQuery("PRAGMA quick_check(1)",null)){assertTrue(c.moveToFirst());assertEquals("ok",c.getString(0));}
            System.out.println("R31_WAL_ANDROID|BOUND_DIAGNOSTIC_PASSIVE_ACTUAL|NATIVE_VERSION=3053004|PLATFORM_VERSION="+platform+"|NATIVE_CODE="+rc+"|NO_REAL_OR_DEVICE_IO_BOUND_CLAIM|PASS");
        }finally{store.close();}
    }
    @Test public void applicationCannotUseDiagnosticSqliteCapability()throws Exception{
        Context app=app();DbStore store=new DbStore(app);try(SourceReadLeaseR82 lease=SourceReadLeaseR82.trySharedCheckpoint(new File(store.readable().getPath()),()->true)){
            assertNotNull(lease);try{ReportNativeCP4.checkpointWalR31(app,new File(store.readable().getPath()),0,40,lease,()->true);fail("application process opened a second SQLite engine");}
            catch(IOException expected){assertEquals("WAL_DIAGNOSTIC_APPLICATION_SOURCE_REQUIRED",expected.getMessage());}
            assertFalse(StorageReclaimR943.ONLINE_TRUNCATE_VALIDATED_R31);System.out.println("R31_WAL_ANDROID|MAIN_SQLITE_CAPABILITY_REFUSED|UNVALIDATED_TRUNCATE_DISABLED|PASS");
        }finally{store.close();}
    }
}
