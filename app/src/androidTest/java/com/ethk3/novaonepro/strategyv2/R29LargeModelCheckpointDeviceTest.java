package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual Android CursorWindow regression: a real full replay state larger than 2MiB must restore without
 * trimming samples. Synthetic fixture only; never opens a product database or grants natural evidence. */
@RunWith(AndroidJUnit4.class)
public final class R29LargeModelCheckpointDeviceTest {
    static ModelArtifactR29 fixture(){
        AtomNetTrainerR27 trainer=new AtomNetTrainerR27(172,29008);trainer.onlineLr=.0001;
        for(int t=0;t<40;t++){double[] x=new double[172];for(int k=0;k<x.length;k++)x[k]=((t*31+k*17)%101)/100.0;trainer.online(new AtomNetTrainerR27.Sample(x,t%2,t%10,"WARM"+t));}trainer.checkpoint=trainer.net.encode();
        for(int t=0;t<AtomNetTrainerR27.REPLAY;t++){double[] x=new double[172];for(int k=0;k<x.length;k++)x[k]=((t*31+k*17)%101)/100.0;trainer.remember(new AtomNetTrainerR27.Sample(x,t%2,t%10,String.format(Locale.ROOT,"S%011d",t)));}
        FtrlR27 f=new FtrlR27(7,.05,1,.0001,.001);for(int i=0;i<32;i++)f.update(Arrays.copyOf(trainer.buffer.get(i).x,7),i%2);
        return new ModelArtifactR29(trainer,f,StatsR27.sha256("synthetic full replay fixture"),"202610300100");
    }
    @Test public void actualAndroidLargeCheckpointRestoresWithEveryReplaySampleAndNextUpdate()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();File file=new File(context.getCacheDir(),"r29-large-model-"+UUID.randomUUID()+".db");Map<String,Object> result=new LinkedHashMap<>();result.put("scope","SYNTHETIC_ANDROID_CURSORWINDOW_REGRESSION_NOT_NATURAL_QUALITY");
        try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(file,null)){
            db.execSQL(ModelArtifactR29.DDL);ModelArtifactR29 original=fixture();byte[] full=original.encode();assertTrue(full.length>(2<<20));
            db.execSQL("INSERT INTO r29_model_checkpoint VALUES('ATOMNET_FTRL',?,?,?,?,?,1)",new Object[]{ModelArtifactR29.ABI,original.datasetHash,original.trainedThrough,StatsR27.sha256(full),full});
            boolean wholeRowRejected=false;String originalFailure="";
            try(Cursor cursor=db.rawQuery("SELECT artifact FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL'",null)){if(cursor.moveToFirst())cursor.getBlob(0);}
            catch(SQLiteException expected){wholeRowRejected=String.valueOf(expected.getMessage()).contains("CursorWindow");originalFailure=expected.toString();}
            result.put("legacyWholeRowReadRejectedByActualAndroid",wholeRowRejected);result.put("legacyWholeRowFailure",originalFailure);assertTrue("negative control must reproduce the original CursorWindow failure on this API34 fixture",wholeRowRejected);
            ModelArtifactR29.Stored stored=ModelArtifactR29.restore(db,"ATOMNET_FTRL");assertNotNull(stored);assertEquals(full.length,stored.byteLength);assertEquals(2000,stored.artifact.trainer.buffer.size());assertArrayEquals(full,stored.artifact.encode());
            AtomNetTrainerR27.Sample next=original.trainer.buffer.get(0);original.trainer.online(next);stored.artifact.trainer.online(next);original.ftrl.update(Arrays.copyOf(next.x,7),next.y);stored.artifact.ftrl.update(Arrays.copyOf(next.x,7),next.y);assertArrayEquals(original.encode(),stored.artifact.encode());
            result.put("artifactBytes",full.length);result.put("replaySamplesRestored",2000);result.put("sqliteBlobChunkBytes",ModelArtifactR29.READ_CHUNK_BYTES);result.put("completeWeightsAdamRngFtrlReplayStateAndNextUpdateEqual",true);result.put("result","PASS_WITH_STATED_ANDROID_SCOPE");
        }catch(Throwable failure){result.put("result","FAIL");result.put("error",failure.toString());throw failure;}
        finally{SQLiteDatabase.deleteDatabase(file);File receipt=new File(context.getFilesDir(),"r29-model-cursorwindow-device.json");try(FileOutputStream out=new FileOutputStream(receipt)){out.write(JsonR27.write(result).getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}}
    }
}
