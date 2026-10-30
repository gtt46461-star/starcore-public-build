package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Actual production Critical schema/trigger regression on two isolated Android databases. */
@RunWith(AndroidJUnit4.class)
public final class R29CriticalInputTriggerDeviceTest {
    @Test public void productionRootFreezeSurvivesAndMirrorApplyRepairsOnlyErroneousReplicaTrigger()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();String name="r29-critical-trigger-"+UUID.randomUUID();File rootFile=new File(context.getCacheDir(),name+"-root.db"),researchFile=new File(context.getCacheDir(),name+"-research.db"),missingFile=new File(context.getCacheDir(),name+"-missing.db"),conflictFile=new File(context.getCacheDir(),name+"-conflict.db");Map<String,Object> result=new LinkedHashMap<>();result.put("scope","SYNTHETIC_ANDROID_NOT_PHYSICAL_NOT_NATURAL");
        try(SQLiteDatabase root=SQLiteDatabase.openOrCreateDatabase(rootFile,null);SQLiteDatabase research=SQLiteDatabase.openOrCreateDatabase(researchFile,null);SQLiteDatabase missing=SQLiteDatabase.openOrCreateDatabase(missingFile,null);SQLiteDatabase conflict=SQLiteDatabase.openOrCreateDatabase(conflictFile,null)){
            result=CriticalInputProductionTriggerFixtureR29.exercise(root,research);RetainedFormalProductionFixtureR29.seed(root,research);
            try(SQLiteDatabase reader=ResearchRouteR29.combinedReader(researchFile,rootFile.getPath())){
                boolean rootOnly=RetainedFormalProductionFixtureR29.n(reader,"SELECT count(*) FROM r974_exact_real_receipt")==0&&RetainedFormalProductionFixtureR29.n(reader,"SELECT count(*) FROM research_exact_probability_f05")==0&&RetainedFormalProductionFixtureR29.n(reader,"SELECT count(*) FROM main.r974_exact_real_receipt")==2;
                boolean writerRejected=false;try{RetainedFormalCopiesR29.isolate(reader);}catch(IllegalStateException expected){writerRejected=String.valueOf(expected.getMessage()).contains("PHYSICAL_RP_WRITER");}
                result.put("rootAuthorityViewShadowsNonemptyOldRpPhysicalCopy",rootOnly&&writerRejected);
            }
            boolean missingRejected=false;try(SQLiteDatabase reader=ResearchRouteR29.combinedReader(researchFile,missingFile.getPath())){RetainedFormalProductionFixtureR29.n(reader,"SELECT count(*) FROM r974_exact_real_receipt");}catch(android.database.sqlite.SQLiteException expected){missingRejected=String.valueOf(expected.getMessage()).contains("no such table");}result.put("missingRootAuthorityFailsClosedDespiteNonemptyOldRpCopy",missingRejected);
            result.putAll(RetainedFormalProductionFixtureR29.exercise(root,research));result.put("retainedNameConflictRollsBackBothFormalTablesAndAudit",RetainedFormalProductionFixtureR29.conflictRollsBack(root,conflict));
            try(SQLiteDatabase reader=ResearchRouteR29.combinedReader(researchFile,rootFile.getPath())){result.put("rootAuthorityViewStillWinsAfterArchival",RetainedFormalProductionFixtureR29.n(reader,"SELECT count(*) FROM r974_exact_real_receipt")==0&&RetainedFormalProductionFixtureR29.n(reader,"SELECT count(*) FROM main."+RetainedFormalCopiesR29.retainedName("r974_exact_real_receipt"))==2);}
            result.putAll(CanonicalInputProductionFixtureR29.exercise(root,research));
            for(Map.Entry<String,Object> entry:result.entrySet())if(entry.getValue() instanceof Boolean&&!Boolean.TRUE.equals(entry.getValue()))throw new AssertionError(entry.getKey());
        }
        catch(Throwable failure){result.put("result","FAIL");result.put("error",failure.toString());throw failure;}
        finally{SQLiteDatabase.deleteDatabase(rootFile);SQLiteDatabase.deleteDatabase(researchFile);SQLiteDatabase.deleteDatabase(missingFile);SQLiteDatabase.deleteDatabase(conflictFile);File receipt=new File(context.getFilesDir(),"r29-critical-trigger-device.json");try(FileOutputStream out=new FileOutputStream(receipt)){out.write(JsonR27.write(result).getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}}
    }
}
