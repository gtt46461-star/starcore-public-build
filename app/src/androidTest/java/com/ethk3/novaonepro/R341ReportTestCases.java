package com.ethk3.novaonepro;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.RepairEffectLedgerR341;
import com.ethk3.novaonepro.strategyv2.ReportR29;
import com.ethk3.novaonepro.strategyv2.ResearchTableAdaptersR30R1;
import com.ethk3.novaonepro.strategyv2.SchemaR27;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public final class R341ReportTestCases {
    public static void missingSourcesReachOverview(Context app) {
        List<Map<String,Object>> rows=new ArrayList<>();
        for(int i=0;i<3;i++)rows.add(ReportSourceStateR341.observation("SOURCE"+i,"snapshot",
                "READ_ERROR","no such table: repair_fixture_"+i,null,null));
        rows.add(ReportSourceStateR341.observation("actual-zero","snapshot","READ_OK","",1,1L));
        Map<String,Object> merged=ReportSourceStateR341.merge(rows);
        assertEquals(3,merged.get("unavailable"));
        assertEquals(1,merged.get("ok"));
        assertEquals("UNKNOWN_SOURCE_UNAVAILABLE",merged.get("health"));
        assertNull(rows.get(0).get("totalRows"));
    }

    public static void sourceReasonsStayDistinct(Context app) {
        assertEquals("TIMED_OUT",ReportSourceStateR341.reason("read timeout"));
        assertEquals("PERMISSION_DENIED",ReportSourceStateR341.reason("permission denied"));
        assertEquals("PARTIAL",ReportSourceStateR341.reason("truncated"));
        assertEquals("OBSERVED_EMPTY",ReportSourceStateR341.reason("OBSERVED_EMPTY"));
        assertEquals("",ReportSourceStateR341.reason("READ_OK"));
    }

    public static void missingFaultSourceRemainsVisibleInReport(Context app) throws Exception {
        File snapshot=Files.createTempFile(app.getCacheDir().toPath(),"r341-fault-snapshot-",".db").toFile();
        try(SQLiteDatabase writer=SQLiteDatabase.openOrCreateDatabase(snapshot,null)){
            writer.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY)");
        }
        try(ReportInputsLeaseCP12 ignored=ReportInputsLeaseCP12.acquire(app.getFilesDir())){
            Map<String,Object> missing;
            try(SQLiteDatabase copy=SQLiteDatabase.openDatabase(snapshot.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
                missing=RuntimeFaultOriginalStoreR335.reportSample(copy,app.getFilesDir());
                assertNull(missing.get("rows"));
                assertNull(missing.get("totalOriginals"));
                assertTrue(((List<?>)missing.get("missingSourceTables")).contains("r31_runtime_fault"));
                try(Cursor c=copy.rawQuery("SELECT 1 FROM sqlite_master WHERE name='r31_runtime_fault'",null)){
                    assertFalse(c.moveToFirst());
                }
            }
            Map<String,Object> chapter=Map.of("snapshotWatermark",Map.of("sourceManifest",Map.of("runtimeFaultHotColdR335",missing)));
            Map<String,Object> health=ReportR29.sourceHealthR341(java.util.Collections.nCopies(12,chapter));
            assertEquals("UNKNOWN_SOURCE_UNAVAILABLE",health.get("health"));
            Map<?,?> source=(Map<?,?>)((List<?>)health.get("sources")).get(0);
            assertEquals("R335_RUNTIME_FAULT_ORIGINALS",source.get("sourceId"));
            assertEquals("WAITING_SOURCE",source.get("reasonCode"));
            assertTrue(String.valueOf(source.get("reason")).contains("r31_runtime_fault"));

            try(SQLiteDatabase writer=SQLiteDatabase.openDatabase(snapshot.getPath(),null,SQLiteDatabase.OPEN_READWRITE)){
                writer.beginTransaction();
                try{
                    SchemaR27.ensureR31Observations(writer);
                    writer.setTransactionSuccessful();
                }finally{writer.endTransaction();}
            }
            try(SQLiteDatabase copy=SQLiteDatabase.openDatabase(snapshot.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
                Map<String,Object> partial=RuntimeFaultOriginalStoreR335.reportSample(copy,app.getFilesDir());
                assertEquals(true,partial.get("hotSchemaReady"));
                assertEquals(List.of(),partial.get("rows"));
                assertEquals(0L,partial.get("observedOriginals"));
                assertNull(partial.get("totalOriginals"));
                assertEquals("PARTIAL",((Map<?,?>)partial.get("sourceObservationR341")).get("sourceQueryStatus"));
            }
        }
    }

    public static void coverageUsesSameSqlWindow(Context app) {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,dirty INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue TEXT PRIMARY KEY,pre_draw_frozen INTEGER,settled INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_OOS_LABEL+"(target_issue TEXT PRIMARY KEY,strict_oos INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT,status TEXT)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES('202611240002',0),('202611240003',0),('202611240004',1)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" VALUES('202611240001',1,1),('202611240002',1,1),('202611240003',1,0),('202611240004',1,1)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_OOS_LABEL+" VALUES('202611240001',1),('202611240002',1),('202611240004',1)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES('202611240001','COMPLETED'),('202611240002','COMPLETED'),('202611240003','PENDING')");
            SettlementCoverageTruthR906 truth=RuntimeTruthSnapshotR906.coverage(db);
            assertEquals("READ_OK",truth.sourceStatus);assertEquals(2,truth.predictedEligible);assertEquals(1,truth.settledPredictions);
            assertEquals(Integer.valueOf(2),truth.historicalSettledOutsideWindow);
            assertEquals("202611240002",truth.fromIssue);assertEquals("202611240003",truth.throughIssue);
            assertEquals(.5,truth.settlementCoverage,0);
            db.execSQL("DROP TABLE "+DbHelper.TABLE_R35_OOS_LABEL);
            truth=RuntimeTruthSnapshotR906.coverage(db);
            assertEquals("SOURCE_UNAVAILABLE",truth.sourceStatus);assertTrue(Double.isNaN(truth.settlementCoverage));
        }
    }

    public static void shadowChartsExcludeRealAndReportSourceFailures(Context app) {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)) {
            ResearchVisualDataR82.Panel missing=shadowPanel(db,"r83_action_candles");
            assertEquals("UNAVAILABLE",missing.displayState);
            assertEquals("WAITING_SOURCE",missing.reasonCode);
            assertTrue(missing.source.contains(ExplorationLaneR83.TABLE));
            DbHelper.ensureR35Schema(db);
            ExplorationLaneR83.ensure(db);
            ResearchVisualDataR82.Panel empty=shadowPanel(db,"r83_action_candles");
            assertEquals("EMPTY",empty.displayState);
            assertEquals("OBSERVED_EMPTY",empty.reasonCode);

            insertShadowSample(db,1,"EXPLORE",1,1);
            insertShadowSample(db,2,"REAL",1,0);
            ResearchVisualDataR82.Panel candle=shadowPanel(db,"r83_action_candles");
            assertEquals("READY",candle.status);
            assertEquals(1,candle.points.size());
            assertEquals(1L,candle.points.get(0).n);
            assertEquals(AppConfig.WIN_NET_PROFIT,candle.points.get(0).value,0.0);
            try(Cursor real=db.rawQuery("SELECT count(*) FROM "+ExplorationLaneR83.TABLE+" WHERE action='REAL'",null)){
                assertTrue(real.moveToFirst());assertEquals(1L,real.getLong(0));
            }
            for(int draw=3;draw<=21;draw++)insertShadowSample(db,draw,"EXPLORE",1,1);
            insertShadowSample(db,22,"EXPLORE",2,0);
            ResearchVisualDataR82.Panel subset=shadowPanel(db,"r83_act_hit");
            assertEquals("PARTIAL",subset.reasonCode);
            assertEquals("HAS_DATA",subset.displayState);
            assertEquals(1,subset.points.size());
            assertEquals(20L,subset.points.get(0).n);
            assertEquals(1.0,subset.points.get(0).value,0.0);
            assertTrue(subset.window.endsWith("不匹配的期数 1"));
            ResearchVisualDataR82.Panel all=shadowPanel(db,"r83_all_hit");
            assertEquals("READY",all.status);
            assertEquals(3,all.points.size());
            assertEquals(.95,all.points.get(2).value,1e-12);

            String shadow=DbHelper.TABLE_R35_SHADOW_OBSERVATION;
            db.execSQL("CREATE TABLE duplicate_originals AS SELECT * FROM "+shadow);
            db.execSQL("INSERT INTO duplicate_originals SELECT * FROM "+shadow+" WHERE target_issue='202611240022'");
            db.execSQL("DROP TABLE "+shadow);
            db.execSQL("ALTER TABLE duplicate_originals RENAME TO "+shadow);
            all=shadowPanel(db,"r83_all_hit");
            assertEquals("PARTIAL",all.reasonCode);
            assertEquals(2,all.points.size());
            assertTrue(all.window.endsWith("不匹配的期数 1"));
            db.execSQL("ALTER TABLE "+shadow+" RENAME COLUMN hit_label TO broken_label");
            ResearchVisualDataR82.Panel broken=shadowPanel(db,"r83_all_hit");
            assertEquals("UNAVAILABLE",broken.displayState);
            assertEquals("SOURCE_ERROR",broken.reasonCode);
            assertEquals(shadow,broken.source);
        }
    }

    private static ResearchVisualDataR82.Panel shadowPanel(SQLiteDatabase db,String id) {
        return new ChartsRepositoryR27.Page(db).read(id,null);
    }

    private static void insertShadowSample(SQLiteDatabase db,int draw,String action,int position,int hit) {
        String issue=String.format(java.util.Locale.ROOT,"20261124%04d",draw);
        db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_SHADOW_OBSERVATION
                +"(target_issue,game_code,base_issue,position,n1,n2,n3,lane,decision_action,reject_code,bundle_hash,"
                +"contract_hash,authorization_hash,record_hash,created_at,settled,actual_number,hit_label)"
                +" VALUES(?,'Binance1-PK10','202611230900',1,1,2,3,'SHADOW','HOLD','','','','',?,1,1,?,?)",
                new Object[]{issue,"ENGINEERING_"+issue,hit==1?1:4,hit});
        db.execSQL("INSERT INTO "+ExplorationLaneR83.TABLE+" VALUES(?,?,1,2,3,.4,.3,20,.3,?,'ENGINEERING',1)",
                new Object[]{issue,position,action});
    }

    private static ResearchVisualDataR82.Panel panel(String state,List<ResearchVisualDataR82.Point> points) {
        return new ResearchVisualDataR82.Panel("research","C01","LINE","actual_table","current","fixture",state,points,List.of());
    }

    public static void chartUnknownDoesNotPresentZero(Context app) {
        assertEquals("EMPTY",panel("OBSERVED_EMPTY",List.of()).displayState);
        List<ResearchVisualDataR82.Point> zero=List.of(new ResearchVisualDataR82.Point("valid",0,Double.NaN,Double.NaN,1,""));
        assertEquals("HAS_DATA",panel("READY",zero).displayState);
        assertEquals("UNAVAILABLE",panel("STALE_SOURCE_WATERMARK",zero).displayState);
        assertEquals("STALE",panel("STALE_SOURCE_WATERMARK",zero).reasonCode);
        assertEquals("UNAVAILABLE",panel("READ_TIMEOUT",List.of()).displayState);
        assertEquals("TIMED_OUT",panel("READ_TIMEOUT",List.of()).reasonCode);
        assertEquals("WAITING_SOURCE",panel("WAITING_SOURCE",List.of()).reasonCode);
        assertEquals("HAS_DATA",panel("READY",List.of(new ResearchVisualDataR82.Point("valid",3,Double.NaN,Double.NaN,1,""))).displayState);
    }

    public static void tablePageKeepsItsExactDenominator(Context app) {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            db.execSQL("CREATE TABLE r27_expert_proposal(generation INTEGER,expert TEXT,proposal_json TEXT,policy_hash TEXT,recorded_at INTEGER)");
            for(int i=0;i<29;i++)db.execSQL("INSERT INTO r27_expert_proposal VALUES(1,'test','{}',?,?)",new Object[]{"policy-"+i,i});
            Map<String,Object> first=ResearchTableAdaptersR30R1.readPageR31(db,"T08",Map.of(),0,8,null);
            assertEquals(8,first.get("shown"));assertEquals(29L,first.get("totalRows"));
            assertEquals(Boolean.TRUE,first.get("hasMore"));
            Map<String,Object> beyond=ResearchTableAdaptersR30R1.readPageR31(db,"T08",Map.of(),29,8,null);
            assertEquals(0,beyond.get("shown"));assertEquals(29L,beyond.get("totalRows"));
            assertEquals(Boolean.FALSE,beyond.get("hasMore"));
            db.execSQL("DROP TABLE r27_expert_proposal");
            Map<String,Object> unavailable=ResearchTableAdaptersR30R1.readPageR31(db,"T08",Map.of(),0,8,null);
            assertNull(unavailable.get("totalRows"));assertEquals(Boolean.FALSE,unavailable.get("totalKnown"));
            assertEquals("WAITING_SOURCE",((Map<?,?>)unavailable.get("sourceObservationR341")).get("reasonCode"));
        }
    }

    public static void effectMigrationDoesNotPrepopulatePass(Context app) {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            db.beginTransaction();
            try {
                RepairEffectLedgerR341.installWithin(db);
                RepairEffectLedgerR341.installWithin(db);
                try(Cursor rows=db.rawQuery("SELECT count(*) FROM "+RepairEffectLedgerR341.EFFECT,null)){
                    assertTrue(rows.moveToFirst());assertEquals(0,rows.getInt(0));
                }
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
    }

    public static void fullRawMustContainExactSnapshot(Context app) throws Exception {
        File job=Files.createTempDirectory(app.getCacheDir().toPath(),"r341-full-raw-").toFile();
        File delivery=new File(job,"delivery");assertTrue(delivery.mkdir());
        byte[] exact={1,2,3,4};
        File original=new File(job,"snapshot.db");Files.write(original.toPath(),exact);
        ReportJobJournalCP12.Job state=new ReportJobJournalCP12.Job("1".repeat(64),"1".repeat(64),
                ReleaseManifestR926.MANIFEST_SHA256,"FULL_REPORT",System.currentTimeMillis());
        state.snapshotSha=StorageBudgetR31.sha(exact);state.snapshotBytes=exact.length;
        byte[] document="{}".getBytes(StandardCharsets.UTF_8);
        byte[] receipt=new JSONObject().put("jobId",state.jobId).put("snapshotSha256",state.snapshotSha)
                .put("rawEvidence",true).put("reportSha256",StorageBudgetR31.sha(document))
                .put("catalogPages",0).put("catalogHashXor","0".repeat(64)).toString().getBytes(StandardCharsets.UTF_8);
        Files.write(new File(job,ReportSemanticVerifierR31.RECEIPT).toPath(),receipt);
        File zip=new File(delivery,"STARCORE_FULL_REPORT.zip");
        for(int sample=0;sample<3;sample++){
            try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))){
                for(String name:Arrays.asList("R30_REPORT.json","R30_REPORT.md",ReportSemanticVerifierR31.RECEIPT,
                        "REPORT_DETAILS.txt","R26_REPORT.json","R27_REPORT.md","R26_POLICY_IDENTITIES.jsonl",
                        "R26_FROZEN_SURFACE.json","STORAGE_CENSUS.txt")){
                    out.putNextEntry(new ZipEntry(name));out.write(name.equals(ReportSemanticVerifierR31.RECEIPT)?receipt:document);out.closeEntry();
                }
                if(sample!=1){out.putNextEntry(new ZipEntry("snapshot.db"));out.write(sample==0?exact:new byte[]{5,6,7,8});out.closeEntry();}
            }
            JSONObject member=new JSONObject().put("path","delivery/STARCORE_FULL_REPORT.zip")
                    .put("bytes",zip.length()).put("sha256",ReportArchiveCP4.hash(zip,()->true));
            JSONObject index=new JSONObject().put("jobId",state.jobId).put("format","SCREPORT_SINGLE_ZIP_R992")
                    .put("files",new JSONArray().put(member));
            Files.write(new File(delivery,"index.json").toPath(),index.toString().getBytes(StandardCharsets.UTF_8));
            try{
                ReportSemanticVerifierR31.verifyDelivery(job,state,()->true);
                assertEquals("A delivery with missing or changed raw input passed",0,sample);
            }catch(IOException invalid){
                if(sample==0)throw invalid;
                assertTrue(invalid.getMessage(),invalid.getMessage().contains(sample==1?"ZIP_MISSING_MEMBERS":"ZIP_SNAPSHOT_HASH"));
            }
            assertArrayEquals(exact,Files.readAllBytes(original.toPath()));
        }
    }
}
