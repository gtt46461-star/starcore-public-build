package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.Arrays;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Isolated native SQLite revision-2 expansion, including an artifact larger than CursorWindow. */
@RunWith(AndroidJUnit4.class)
public final class R29LegacyDomainExtensionDeviceTest {
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){assertTrue(c.moveToFirst());return c.getLong(0);}}
    static LegacyDomainUpgradeR29.Check check(){return new LegacyDomainUpgradeR29.Check(System::nanoTime,System.nanoTime()+30_000_000_000L,()->false);}
    static void oldFull(SQLiteDatabase db){
        db.execSQL(ResearchRouteR29.OWNER_DDL);db.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY','schema','root',42,1)");
        db.execSQL(LegacyDomainUpgradeR29.DDL);db.execSQL("INSERT INTO r29_legacy_domain_owner VALUES(1,'FULL',17,?,1)",new Object[]{StatsR27.sha256("old-full-proof")});
        db.execSQL("CREATE TABLE r26_runtime(k TEXT PRIMARY KEY,steps INTEGER)");db.execSQL("INSERT INTO r26_runtime VALUES('already-advanced',9999)");
    }
    @Test public void oldFullKeepsAdvancedResearchAndImportsCompleteLargeArtifact()throws Exception{
        try(SQLiteDatabase root=SQLiteDatabase.create(null);SQLiteDatabase rp=SQLiteDatabase.create(null);SQLiteDatabase conflicting=SQLiteDatabase.create(null)){
            final String table="v57_research_artifact_r811",ddl="CREATE TABLE "+table+"(model TEXT PRIMARY KEY,state BLOB,steps INTEGER)";
            root.execSQL(ddl);byte[] original=new byte[(3<<20)+17];new Random(29).nextBytes(original);root.execSQL("INSERT INTO "+table+" VALUES('FTRL',?,739)",new Object[]{original});
            root.execSQL("CREATE TABLE r26_runtime(k TEXT PRIMARY KEY,steps INTEGER)");root.execSQL("INSERT INTO r26_runtime VALUES('already-advanced',1)");
            oldFull(rp);oldFull(conflicting);assertFalse(LegacyDomainExtensionR29.readyWithin(rp,17));
            LegacyDomainUpgradeR29.Check budget=check();LegacyDomainUpgradeR29.Snapshot captured=LegacyDomainUpgradeR29.capture(root,budget,64L<<20,LegacyResearchOwnershipR29::newlyOwnedResearch,()->{});
            assertEquals(1,captured.tables.size());rp.beginTransactionNonExclusive();try{LegacyDomainExtensionR29.mergeWithin(rp,captured,17,budget,name->{});rp.setTransactionSuccessful();}finally{rp.endTransaction();}
            assertTrue(LegacyDomainExtensionR29.readyWithin(rp,17));assertEquals(9999,n(rp,"SELECT steps FROM r26_runtime"));assertEquals(739,n(rp,"SELECT steps FROM "+table));
            byte[] restored=ResearchRowsR29.readBlob(rp,table,"state","model=?",new String[]{"FTRL"},original.length,()->{});assertTrue(Arrays.equals(original,restored));
            assertEquals(1,n(root,"SELECT steps FROM r26_runtime"));assertEquals(original.length,n(root,"SELECT length(state) FROM "+table));
            rp.execSQL("UPDATE "+table+" SET steps=800");rp.beginTransactionNonExclusive();try{LegacyDomainExtensionR29.mergeWithin(rp,captured,17,check(),name->{});rp.setTransactionSuccessful();}finally{rp.endTransaction();}assertEquals(800,n(rp,"SELECT steps FROM "+table));
            conflicting.execSQL(ddl);conflicting.execSQL("INSERT INTO "+table+" VALUES('FTRL',X'99',999)");boolean rejected=false;
            conflicting.beginTransactionNonExclusive();try{LegacyDomainExtensionR29.mergeWithin(conflicting,captured,17,check(),name->{});conflicting.setTransactionSuccessful();}catch(IllegalStateException conflict){rejected=conflict.getMessage().contains("EXISTING_STATE_CONFLICT");}finally{conflicting.endTransaction();}
            assertTrue(rejected);assertFalse(LegacyDomainExtensionR29.readyWithin(conflicting,17));assertEquals(999,n(conflicting,"SELECT steps FROM "+table));
            try(Cursor proof=rp.rawQuery("SELECT source_hash FROM r29_legacy_domain_owner WHERE singleton=1",null)){
                assertTrue(proof.moveToFirst());assertEquals(StatsR27.sha256("old-full-proof"),proof.getString(0));
            }
            java.util.Map<String,Object> receipt=new java.util.LinkedHashMap<>();
            receipt.put("scope","SYNTHETIC_ANDROID_SQLITE_NOT_PHYSICAL_NOT_NATURAL");receipt.put("artifactBytes",original.length);
            for(String key:new String[]{"oldFullAdvancedStateRetained","originalFullProofUnchanged","completeLargeArtifactRestored","rootOriginalUnchanged","acknowledgedExtensionNotReapplied","conflictingArtifactPreserved"})receipt.put(key,true);
            receipt.put("result","PASS_WITH_STATED_ANDROID_SCOPE");
            java.io.File file=new java.io.File(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"r29-domain-extension-device.json");
            try(java.io.FileOutputStream output=new java.io.FileOutputStream(file)){output.write(JsonR27.write(receipt).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            System.out.println("R29_DOMAIN_EXTENSION_DEVICE|PASS|scope=SYNTHETIC_ANDROID_SQLITE|artifactBytes="+original.length);
        }
    }
}
