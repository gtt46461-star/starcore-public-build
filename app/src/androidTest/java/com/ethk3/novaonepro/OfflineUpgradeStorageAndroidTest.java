package com.ethk3.novaonepro;

import android.content.Context;
import android.content.pm.PackageInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Framework-only synthetic marker; can target original R78 without linking newer product classes. */
@RunWith(AndroidJUnit4.class)
public final class OfflineUpgradeStorageAndroidTest {
    private static final byte[] MARKER=("STARCORE_SYNTHETIC_OFFLINE_UPGRADE_STORAGE_V1\n"
            +"Disposable emulator fixture; no user data or credentials.\n").getBytes(StandardCharsets.UTF_8);
    private static File marker(Context context) {
        return new File(context.getFilesDir(),"synthetic_r80_upgrade_storage_marker.txt");
    }
    private static void receipt(Context context,String stage) throws Exception {
        PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
        StringBuilder digest=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(MARKER))digest.append(String.format(java.util.Locale.US,"%02x",b&255));
        System.out.println("UPGRADE_STORAGE_FIXTURE|stage="+stage+"|package="+context.getPackageName()
                +"|versionCode="+info.versionCode+"|versionName="+info.versionName
                +"|markerBytes="+MARKER.length+"|markerSha256="+digest
                +"|scope=SYNTHETIC_PRIVATE_FILE_NOT_USER_DATABASE_MIGRATION");
    }
    @Test public void writeSyntheticMarkerBeforeCoverInstall() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.ethk3.novaonepro",context.getPackageName());
        File file=marker(context);assertFalse("fresh synthetic marker required",file.exists());
        try(FileOutputStream out=new FileOutputStream(file)){out.write(MARKER);out.getFD().sync();}
        assertEquals(MARKER.length,file.length());receipt(context,"BEFORE_WRITE_COMPLETE");
    }
    @Test public void verifySyntheticMarkerAfterCoverInstall() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.ethk3.novaonepro",context.getPackageName());
        File file=marker(context);assertTrue("cover install must preserve marker",file.isFile());
        assertEquals(MARKER.length,file.length());ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(FileInputStream in=new FileInputStream(file)) {
            byte[] buffer=new byte[256];int n;
            while((n=in.read(buffer))!=-1){bytes.write(buffer,0,n);assertTrue(bytes.size()<=MARKER.length);}
        }
        assertArrayEquals(MARKER,bytes.toByteArray());receipt(context,"AFTER_READ_VERIFIED");
    }
}
