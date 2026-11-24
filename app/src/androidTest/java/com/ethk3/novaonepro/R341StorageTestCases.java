package com.ethk3.novaonepro;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.HistoryContinuityR341;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.json.JSONObject;

public final class R341StorageTestCases {
    private static File fixture(Context app) throws IOException {
        return Files.createTempDirectory(app.getCacheDir().toPath(), "r341-b10-").toFile();
    }

    private static File bytes(File root, String path, byte[] body) throws IOException {
        File file = new File(root, path);
        if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) throw new IOException(path);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(body);
            output.getFD().sync();
        }
        return file;
    }

    public static void protectedDebtAndSnapshotAdmission(Context app) throws Exception {
        File root = fixture(app), backup = bytes(root,"databases/legacy.db.bak",new byte[]{9});
        try (RandomAccessFile file = new RandomAccessFile(backup,"rw")) { file.setLength(4_344_863_006L); }
        long[] space = {10L << 30};
        StorageBudgetR31 budget = new StorageBudgetR31(root,StorageBudgetR31.Config.production(), ignored -> space[0]);
        long original = ((Number)budget.snapshot().get("protectedOriginalRegularBytes")).longValue();
        assertEquals(4_344_863_006L,original);
        try (StorageBudgetR31.Reservation report = budget.reserve("report:first",1,StorageBudgetR31.Category.REPORT_WORKSPACE,
                "files/r981_reports/jobs/first",1_427_210_240L,true)) {
            assertTrue(report.maximumGrowthBytes()>StoragePolicyR341.ORDINARY_REQUEST_BYTES);
            try {
                budget.reserve("ordinary-too-large",1,StorageBudgetR31.Category.CACHE,"cache/oversize",129*StorageBudgetR31.MiB,false);
                fail("Oversized derived cache accepted");
            } catch (StorageBudgetR31.Rejected expected) { assertEquals("ORDINARY_WORKING_GROWTH_LIMIT",expected.reason); }
            try (StorageBudgetR31.Reservation critical = budget.reserve("critical",1,StorageBudgetR31.Category.DATABASE,
                    "databases/live.db",1L<<20,false)) { critical.beforeChunk(512); }
        }
        assertTrue(backup.isFile());
        assertEquals(original,backup.length());
    }

    public static void parallelPhysicalReservation(Context app) throws Exception {
        File root = fixture(app);
        long envelope = 1_000_000_000L;
        StorageBudgetR31 budget = new StorageBudgetR31(root,StorageBudgetR31.Config.production(),
                ignored -> StorageBudgetR31.FREE_FLOOR_BYTES + 10*StorageBudgetR31.MiB + envelope);
        try (StorageBudgetR31.Reservation first = budget.reserve("report-a",1,StorageBudgetR31.Category.REPORT_WORKSPACE,
                "files/job-a",envelope,true)) {
            first.beforeChunk(8192);
            File saved = bytes(root,"files/job-a/partial",new byte[8192]);
            first.reconcile();
            try {
                budget.reserve("report-b",2,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job-b",envelope,true);
                fail("Physical capacity promised twice");
            } catch (StorageBudgetR31.Rejected expected) { assertEquals("FREE_SPACE_RESERVE",expected.reason); }
            assertTrue(saved.isFile());
            assertEquals(0,budget.recoverReservations());
        }
        try (StorageBudgetR31.Reservation second = budget.reserve("report-b",2,StorageBudgetR31.Category.REPORT_WORKSPACE,
                "files/job-b",envelope,true)) { assertEquals(envelope,second.maximumGrowthBytes()); }
    }

    public static void uniqueReportsSurvive(Context app) throws Exception {
        File root = fixture(app);
        List<File> originals = new ArrayList<>();
        for (int n=1;n<=9;n++) {
            String id = String.format(java.util.Locale.ROOT,"%064x",n);
            File job = new File(root,"jobs/"+id);
            originals.add(bytes(job,"snapshot.db",new byte[]{(byte)n}));
            File pointer = bytes(job,"published.json",new byte[]{1});
            assertTrue(pointer.setLastModified(1));
        }
        ReportPublicationCP12.enforceQuota(root,new File(root,"none"),1);
        for (int i=0;i<originals.size();i++) assertArrayEquals(new byte[]{(byte)(i+1)},Files.readAllBytes(originals.get(i).toPath()));
    }

    public static void missingIndexProtectsSnapshot(Context app) throws Exception {
        File job = fixture(app);
        byte[] original = {3,1,4};
        File snapshot = bytes(job,"snapshot.db",original);
        try {
            ReportPublicationCP12.cleanRaw(job,"archive");
            fail("Deleted workspace without a verified replacement");
        } catch (IOException expected) { assertTrue(expected.getMessage().contains("REPLACEMENT_INDEX")); }
        assertArrayEquals(original,Files.readAllBytes(snapshot.toPath()));
    }

    public static void fetchTraceArchiveGuard(Context app) throws Exception {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.beginTransaction();
            try {
                db.execSQL("CREATE TABLE research_row_revision_v2(generation INTEGER PRIMARY KEY AUTOINCREMENT,source_table TEXT,source_rowid INTEGER,UNIQUE(source_table,source_rowid))");
                db.execSQL("CREATE TABLE research_retention_permit_v2(source_table,source_rowid,archive_id,row_hash,source_generation,created_at)");
                db.execSQL("CREATE TABLE research_evidence_archive_v2(archive_id,state)");
                db.execSQL("CREATE TABLE research_evidence_archive_member_v2(archive_id,source_table,source_rowid,row_hash,source_generation,source_key_hash,key_canonical)");
                db.execSQL("CREATE TABLE research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at)");
                FetchLedgerR925.ensureR31Timing(db);
                db.execSQL("INSERT INTO r31_fetch_timing(request_id,boot_id,payload_hash,issue,accepted,body_truncated,observation_contract,finished_wall_ms) VALUES('one','boot','payload','',0,0,'fixture',1)");
                db.execSQL("UPDATE r31_fetch_timing SET accepted=1 WHERE request_id='one'");
                try {
                    db.execSQL("DELETE FROM r31_fetch_timing WHERE request_id='one'");
                    fail("Direct deletion of an original was accepted");
                } catch (android.database.sqlite.SQLiteConstraintException expected) {
                    assertTrue(expected.getMessage().contains("R341_FETCH_TRACE_VERIFIED_ARCHIVE_REQUIRED"));
                }
                try (Cursor row=db.rawQuery("SELECT count(*) FROM r31_fetch_timing",null)) { assertTrue(row.moveToFirst());assertEquals(1,row.getInt(0)); }
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }
    }

    public static void checkpointAuditSeparatesMissingAndVerified(Context app) throws Exception {
        File root = fixture(app);
        byte[] content = "independent checkpoint bytes".getBytes(StandardCharsets.UTF_8);
        String hash = StorageBudgetR31.sha(content), missing = "0".repeat(64);
        bytes(root,hash+".bin",content);
        StringWriter observations = new StringWriter();
        JSONObject result = CheckpointReportAuditR32.observe(root,Set.of(hash,missing),observations,()->true);
        assertEquals(2,result.getLong("byteVerificationDenominator"));
        assertEquals(1,result.getLong("verifiedObjects"));
        assertEquals(1,result.getLong("missingObjects"));
        assertFalse(result.getBoolean("byteCoverageComplete"));
        assertTrue(observations.toString().contains(missing));
    }

    public static void checkpointMigrationDoesNotInventRecovery(Context app) throws Exception {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.beginTransaction();
            try {
                com.ethk3.novaonepro.strategyv2.CheckpointRecoveryR335.ensureSchema(db);
                assertTrue(com.ethk3.novaonepro.strategyv2.CheckpointRecoveryR335.ready(db));
                Map<String,Object> report=com.ethk3.novaonepro.strategyv2.CheckpointRecoveryR335.report(db);
                assertEquals(Map.of(),report.get("observedTransitions"));
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }
    }

    private static Models.Draw draw(String issue,long available) {
        Models.Draw draw=new Models.Draw();
        draw.issue=issue;draw.openTime=1;draw.fetchTime=available;
        for(int i=0;i<10;i++)draw.numbers[i]=i+1;
        return draw;
    }

    public static void historyGapsAndCrossDay(Context app) {
        List<Models.Draw> continuous=new ArrayList<>(),gap=new ArrayList<>();
        String issue="202611240001", expected="";
        for(int i=0;i<5010;i++){
            Models.Draw row=draw(issue,i+1);
            if(i<5000)continuous.add(row);
            if(i==1240)expected=issue;
            if(i<1240||i>=1250)gap.add(row);
            issue=TargetIssueResolver.next(issue).targetIssue;
            assertFalse(issue.isEmpty());
        }
        HistoryContinuityR341.Inspection good=HistoryContinuityR341.inspect(continuous);
        HistoryContinuityR341.Inspection bad=HistoryContinuityR341.inspect(gap);
        assertEquals(0,good.gaps.size());assertEquals(0,good.contiguousSuffixStart);
        assertEquals(5000,gap.size());assertEquals(1,bad.gaps.size());
        assertEquals(expected,bad.gaps.get(0).expected);
        assertEquals(1240,bad.contiguousSuffixStart);
        assertNotEquals(good.sourceHash,bad.sourceHash);
    }

    public static void lateHistoryChangesSourceIdentity(Context app) {
        Models.Draw first=draw("202611240001",120),second=draw("202611240002",180);
        String early=HistoryContinuityR341.inspect(List.of(first,second)).sourceHash;
        second.fetchTime=360;
        assertNotEquals(early,HistoryContinuityR341.inspect(List.of(first,second)).sourceHash);
    }

    private static void createOriginalDatabase(File file) {
        try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(file,null)){
            db.execSQL("CREATE TABLE draws(issue INTEGER PRIMARY KEY,number INTEGER NOT NULL)");
            db.execSQL("WITH RECURSIVE calendar(n) AS (VALUES(1) UNION ALL SELECT n+1 FROM calendar WHERE n<5000)"
                    +" INSERT INTO draws SELECT n,n%10+1 FROM calendar");
            db.execSQL("CREATE TABLE scratch(body BLOB)");
            db.execSQL("INSERT INTO scratch VALUES(zeroblob(524288))");
            db.execSQL("DROP TABLE scratch");
        }
    }

    public static void offlineSwapRecoveryR341(Context app) throws Exception {
        System.loadLibrary("starcore_report_cp4");
        for(boolean candidateInstalled:new boolean[]{false,true}){
            File root=fixture(app), live=new File(root,"live.db"), copy=new File(root,"live.db.r82-rebuilt");
            File backup=new File(root,"live.db.r82-original-backup"), journal=new File(root,"live.db.r82-maintenance-journal");
            File receiptFile=new File(root,"live.db.r82-backup-receipt");
            createOriginalDatabase(live);
            String original=StorageMaintenanceFileR82.sha256(live);
            StorageMaintenanceFileR82.Receipt rebuiltReceipt;
            try(SourceReadLeaseR82 lease=SourceReadLeaseR82.exclusive(live)){
                rebuiltReceipt=StorageMaintenanceFileR82.rebuildAndSwap(live);
            }
            String rebuilt=rebuiltReceipt.activeSha256;
            assertEquals(original,rebuiltReceipt.sourceSha256);
            assertNotEquals(original,rebuilt);
            assertTrue(rebuiltReceipt.activeBytes<rebuiltReceipt.originalBytes);
            assertEquals(original,StorageMaintenanceFileR82.sha256(backup));
            Properties state=StorageMaintenanceFileR82.read(receiptFile);
            state.setProperty("state",candidateInstalled?"INSTALLED":"ORIGINAL_RENAMED");
            Files.delete(receiptFile.toPath());
            if(!candidateInstalled)Files.move(live.toPath(),copy.toPath());
            StorageMaintenanceFileR82.write(journal,state);
            StorageMaintenanceFileR82.recoverIfNeeded(live);
            assertEquals(candidateInstalled?rebuilt:original,StorageMaintenanceFileR82.sha256(live));
            if(candidateInstalled){
                assertEquals(original,StorageMaintenanceFileR82.sha256(backup));
                assertEquals(rebuilt,StorageMaintenanceFileR82.read(receiptFile).getProperty("candidate_sha256"));
            }else{
                assertFalse(backup.exists());assertFalse(receiptFile.exists());
            }
            assertFalse(journal.exists());
            assertFalse(copy.exists());
            try(SQLiteDatabase db=SQLiteDatabase.openDatabase(live.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
                try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){assertTrue(c.moveToFirst());assertEquals("ok",c.getString(0));}
                try(Cursor c=db.rawQuery("SELECT count(*),sum(number) FROM draws",null)){
                    assertTrue(c.moveToFirst());assertEquals(5000,c.getInt(0));assertEquals(27500,c.getInt(1));
                }
            }
        }
    }
}
