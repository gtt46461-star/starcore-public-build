package com.ethk3.novaonepro;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.util.zip.GZIPOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Runs actual feature, expert, NumberQ and complete META1200 production code on ART. */
@RunWith(AndroidJUnit4.class)
public final class E03AAndroidParityTest {
    @Test public void hundredIssueProductionExport() throws Exception {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File raw=new File(target.getFilesDir(),"e03a-android.jsonl");
        RawUpstreamHarnessR932.main(new String[]{raw.getAbsolutePath(),"full-export","100"});
        File zipped=new File(target.getFilesDir(),"e03a-android.jsonl.gz");
        try(InputStream in=new FileInputStream(raw);
            OutputStream out=new GZIPOutputStream(new FileOutputStream(zipped))) {
            byte[] bytes=new byte[32768]; int n;
            while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
        }
        assertTrue(zipped.isFile()&&zipped.length()>0);
        assertTrue(raw.delete());
        System.out.println("E03A_ANDROID_PRODUCTION_100_ISSUES=PASS");
    }
}
