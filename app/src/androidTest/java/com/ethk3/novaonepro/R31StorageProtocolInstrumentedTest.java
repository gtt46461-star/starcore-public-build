package com.ethk3.novaonepro;

import android.content.Context;
import android.os.Build;
import android.system.Os;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Test definitions for the real Android filesystem. Their presence/compilation is not a device PASS. */
@RunWith(AndroidJUnit4.class)
public final class R31StorageProtocolInstrumentedTest {
    private static File fixture()throws IOException{
        Context app=ApplicationProvider.getApplicationContext();File root=new File(app.getCacheDir(),"r31-storage-test-"+UUID.randomUUID());
        if(!root.mkdir())throw new IOException("R31_TEST_ROOT");return root;
    }
    @Test public void kernelAllocatedBlocksAndLegacyEnumerationAreHonest()throws Exception{
        File root=fixture(),payload=new File(root,"payload");try(FileOutputStream out=new FileOutputStream(payload)){out.write(new byte[65536]);out.getFD().sync();}
        StorageCensusSU2.Census census=StorageCensusSU2.measure(root);
        if(Build.VERSION.SDK_INT<26){assertTrue(census.truncated);assertTrue(census.incompleteReasons.toString().contains("UNSUPPORTED_API_"));return;}
        assertFalse(census.line(),census.truncated);assertTrue(census.allocatedBytesKnown);assertEquals(65536,census.total);
        long actual=Os.stat(root.getPath()).st_blocks*512L+Os.stat(payload.getPath()).st_blocks*512L;
        assertEquals(actual,census.allocatedFileAndDirectoryBytes);
        System.out.println("R31_ANDROID_STORAGE|REAL_KERNEL_ST_BLOCKS_AND_DIRECTORY_BYTES|INJECTED_FILES_NOT_NATURAL|PASS");
    }
    @Test public void productionBlockRequirementAndInjectedSpaceDenialKeepOriginal()throws Exception{
        File root=fixture(),directory=new File(root,"files/job");assertTrue(directory.mkdirs());File original=new File(directory,"original");try(FileOutputStream out=new FileOutputStream(original)){out.write(new byte[4096]);out.getFD().sync();}
        final long[] available={64L<<20};StorageBudgetR31 budget=new StorageBudgetR31(root,new StorageBudgetR31.Config(8L<<20,8L<<20,1024,65536,StorageBudgetR31.VERSION),f->available[0]);
        if(Build.VERSION.SDK_INT<26){try{budget.reserve("android-test",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",65536,true);fail("Pre26 enumeration must be explicit unknown");}catch(StorageBudgetR31.Rejected expected){assertEquals("CENSUS_INCOMPLETE",expected.reason);}return;}
        try(StorageBudgetR31.Reservation reservation=budget.reserve("android-test",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",65536,true)){
            available[0]=1024;try{reservation.beforeChunk(1);fail("Injected free-floor shortage accepted");}catch(StorageBudgetR31.Rejected expected){assertEquals("FREE_SPACE_RESERVE",expected.reason);}
            assertEquals(0,budget.recoverReservations());assertEquals(4096,original.length());
        }assertTrue(((List<?>)budget.snapshot().get("owners")).isEmpty());assertEquals(4096,original.length());
        System.out.println("R31_ANDROID_STORAGE|REAL_KERNEL_CENSUS_WITH_INJECTED_FREE_SPACE_REJECTION|ORIGINAL_PROTECTED|NOT_REAL_ENOSPC|PASS");
    }
}
