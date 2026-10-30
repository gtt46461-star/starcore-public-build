package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Native two-file Root-RO/RP retirement probe; Root's writer remains held during research retirement. */
@RunWith(AndroidJUnit4.class)
public final class R29ShadowRetirementAuthorityDeviceTest {
    static final String PROTECTED=StatsR27.sha256("root-choice"),OTHER=StatsR27.sha256("eligible-challenger"),PARENT=StatsR27.sha256("family-parent");
    static final long EXPIRES=QualificationWindowR26.LENGTH+1,NEXT=EXPIRES+10;
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){assertTrue(c.moveToFirst());return c.getLong(0);}}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){assertTrue(c.moveToFirst());return c.getString(0);}}
    static void research(SQLiteDatabase db){
        db.beginTransactionNonExclusive();try{
            RuntimeSchemaR26.ensure(db);for(String ddl:StrategySchemaSU2.DDL)if(!ddl.contains("su2_mainline_choice")&&!ddl.contains("su2_mainline_binding")&&!ddl.contains("su2_live_decision"))db.execSQL(ddl);
            for(String hash:new String[]{PROTECTED,OTHER,PARENT}){
                db.execSQL("INSERT INTO r26_live_model VALUES(?,?,X'0001FF','retained-state','retained-checkpoint','parent',100,1,101,'ACTIVE',1,'01')",new Object[]{hash,"SU2|SYNTHETIC_RETAINED_SHADOW_STATE"});
                db.execSQL("INSERT INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,state,created_at) VALUES(?,?,'synthetic','F15','SYNTHETIC','fixture',1,'LEAGUE',1)",new Object[]{hash,"SU2|SYNTHETIC_RETAINED_SHADOW_STATE"});
                db.execSQL("INSERT INTO su2_league(member_id,kind,name,genome_key,joined_sequence) VALUES(?,'SU2','fixture',?,1)",new Object[]{"SU2:"+hash,hash});
                db.execSQL("INSERT INTO r26_cohort(genome_key,from_sequence,to_sequence,incumbent,protocol,checked_sequence) VALUES(?,1,?,'fixture-benchmark',?,?)",new Object[]{hash,EXPIRES,QualificationWindowR26.PROTOCOL,EXPIRES});
                db.execSQL("INSERT INTO r26_shadow_admission VALUES(?,'01',?,1,?,'parent')",new Object[]{hash,hash.equals(PARENT)?"BASE":"CHALLENGER",hash.equals(PARENT)?0:EXPIRES});
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    @Test public void rootReadOnlyProtectsChoiceWhileRootWriterIsHeld()throws Exception{
        File dir=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"shadow-root-ro-"+UUID.randomUUID());assertTrue(dir.mkdirs());
        File rootFile=new File(dir,"root.db"),rpFile=new File(dir,"research.db");ExecutorService executor=Executors.newSingleThreadExecutor();
        try(SQLiteDatabase root=SQLiteDatabase.openOrCreateDatabase(rootFile,null);SQLiteDatabase rp=SQLiteDatabase.openOrCreateDatabase(rpFile,null)){
            root.enableWriteAheadLogging();rp.enableWriteAheadLogging();
            for(String ddl:StrategySchemaSU2.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS su2_mainline_choice("))root.execSQL(ddl);
            root.execSQL("INSERT INTO su2_mainline_choice VALUES(1,?,0,0,'SYNTHETIC_ROOT_AUTHORITY',0,1)",new Object[]{"SU2:"+PROTECTED});root.execSQL("CREATE TABLE held_probe(k INTEGER)");research(rp);
            try(SQLiteDatabase ro=SQLiteDatabase.openDatabase(rootFile.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY)){
                try(Cursor c=ro.rawQuery("PRAGMA query_only=1",null)){c.moveToFirst();}assertTrue(ro.isReadOnly());
                root.beginTransactionNonExclusive();try{
                    root.execSQL("INSERT INTO held_probe VALUES(1)");
                    Future<Long> work=executor.submit(()->{rp.beginTransactionNonExclusive();try{long retired=ShadowAdmissionR26.retireOne(rp,ro,NEXT,1000);rp.setTransactionSuccessful();return retired;}finally{rp.endTransaction();}});
                    assertEquals("Root primary held must not block the separate Root observation/RP writer pools",1L,(long)work.get(1500,TimeUnit.MILLISECONDS));
                    assertEquals("ACTIVE",s(rp,"SELECT status FROM r26_live_model WHERE policy_hash='"+PROTECTED+"'"));
                    assertEquals(0,n(rp,"SELECT count(*) FROM sqlite_master WHERE name='su2_mainline_choice'"));
                }finally{root.endTransaction();}
                assertEquals(0,n(root,"SELECT count(*) FROM held_probe"));assertEquals("SU2:"+PROTECTED,s(root,"SELECT member_id FROM su2_mainline_choice"));
                assertEquals(3,n(rp,"SELECT count(*) FROM r26_live_model WHERE state=X'0001FF' AND checkpoint_hash='retained-checkpoint'"));
                root.execSQL("DROP TABLE su2_mainline_choice");boolean failed=false;rp.beginTransactionNonExclusive();try{ShadowAdmissionR26.retireOne(rp,ro,NEXT,1000);rp.setTransactionSuccessful();}catch(android.database.sqlite.SQLiteException missing){failed=missing.getMessage().contains("no such table");}finally{rp.endTransaction();}assertTrue("missing true Root schema remains an error",failed);
                java.util.Map<String,Object> receipt=new java.util.LinkedHashMap<>();
                receipt.put("scope","SYNTHETIC_ANDROID_TWO_REAL_WAL_FILES_NOT_PHYSICAL_NOT_NATURAL");
                receipt.put("rootHeldDeadlineMs",1500);
                for(String key:new String[]{"rootChoiceMemberProtected","eligibleUnboundMemberRetired","allCheckpointWeightsRetained","rootRowsAndChoiceUnchanged","noFakeRootChoiceOnResearch","independentRootReaderWhileWriterHeld","realMissingRootSchemaRemainsError"})receipt.put(key,true);
                receipt.put("result","PASS_WITH_STATED_ANDROID_SCOPE");
                File outputFile=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"r29-shadow-retirement-device.json");
                try(java.io.FileOutputStream output=new java.io.FileOutputStream(outputFile)){output.write(JsonR27.write(receipt).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                System.out.println("R29_SHADOW_RETIREMENT_DEVICE|PASS|scope=SYNTHETIC_ANDROID_TWO_FILES|rootHeld=true|deadlineMs=1500");
            }
        }finally{executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));for(File f:dir.listFiles())f.delete();dir.delete();}
    }
}
