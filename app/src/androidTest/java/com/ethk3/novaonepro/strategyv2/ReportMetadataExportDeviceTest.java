package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.StrategySchemaSU2;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual diagnostic export with the production WITHOUT ROWID SU2 schema. */
@RunWith(AndroidJUnit4.class)
public final class ReportMetadataExportDeviceTest {
    public static String largeText(){StringBuilder s=new StringBuilder();for(int i=0;i<170000;i++)s.append("中😀\"\\\n\u0000");return s.toString();}
    public static void fixture(SQLiteDatabase db){
        db.beginTransaction();try{
            StrategySchemaSU2.ensure(db);
            for(int i=0;i<3;i++)db.execSQL("INSERT INTO su2_campaign(campaign_id,through_sequence,dev_start,dev_end,val_start,val_end,generations,population,status,started_at) VALUES(?,100,0,40,40,80,1,8,?,1000)",new Object[]{"SYNTHETIC_CAMPAIGN_"+i,i==0?"RUNNING":"DONE"});
            db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER) WITHOUT ROWID");
            db.execSQL("INSERT INTO app_state VALUES('r975_director_priority',?,1234)",new Object[]{largeText()});
            db.execSQL("CREATE TABLE composite_export(tenant TEXT,id BLOB,status TEXT,payload TEXT,empty_text TEXT,null_text TEXT,raw BLOB,PRIMARY KEY(tenant,id)) WITHOUT /* schema comment */ ROWID");
            db.execSQL("INSERT INTO composite_export VALUES('a',X'0001',?,?,'',NULL,X'0000')",new Object[]{largeText(),largeText()});
            db.execSQL("INSERT INTO composite_export VALUES('a',X'0002',NULL,'N','',NULL,X'')");
            db.execSQL("INSERT INTO composite_export VALUES('b',X'0001',?,'S','',NULL,X'01')",new Object[]{largeText()});
            db.execSQL("CREATE TABLE misleading(without ROWID,marker TEXT DEFAULT 'WITHOUT ROWID')");
            db.execSQL("INSERT INTO misleading VALUES(7,'ordinary')");
            db.execSQL("CREATE TABLE rowid_shadow(rowid INTEGER,status TEXT)");
            db.execSQL("INSERT INTO rowid_shadow VALUES(9,'A'),(9,'B'),(10,'A')");
            db.execSQL("CREATE TABLE nocase_group(id INTEGER PRIMARY KEY,status TEXT COLLATE NOCASE)");
            db.execSQL("INSERT INTO nocase_group VALUES(1,'a'),(2,'A')");
            db.execSQL("CREATE TABLE r952_candidate(id TEXT PRIMARY KEY,status TEXT,payload TEXT) WITHOUT ROWID");
            db.execSQL("WITH RECURSIVE n(x) AS(VALUES(0) UNION ALL SELECT x+1 FROM n WHERE x<20004) INSERT INTO r952_candidate SELECT printf('k%06d',x),'READY','small' FROM n");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    private static JSONArray rows(SQLiteDatabase db,String table,List<String> columns)throws Exception{StringWriter w=new StringWriter();w.write('[');long count=ReportMetadataStreamR81.writeRows(db,table,columns,w,()->true);w.write(']');JSONArray result=new JSONArray(w.toString());assertEquals(count,result.length());return result;}
    private static JSONArray groups(SQLiteDatabase db,String table,String column)throws Exception{StringWriter w=new StringWriter();w.write('[');long count=ReportMetadataStreamR81.writeGroups(db,table,column,w,()->true,false);w.write(']');JSONArray result=new JSONArray(w.toString());assertEquals(count,result.length());return result;}
    public static void verify(SQLiteDatabase db,File output)throws Exception{
        assertTrue(db.isReadOnly());JSONArray campaigns=groups(db,"su2_campaign","status");assertEquals(2,campaigns.length());assertEquals("DONE",campaigns.getJSONObject(0).getString("value"));assertEquals(2,campaigns.getJSONObject(0).getLong("rows"));assertEquals("RUNNING",campaigns.getJSONObject(1).getString("value"));
        JSONArray composite=groups(db,"composite_export","status");assertTrue(composite.getJSONObject(0).isNull("value"));assertEquals(largeText(),composite.getJSONObject(1).getString("value"));assertEquals(2,composite.getJSONObject(1).getLong("rows"));JSONArray detail=rows(db,"composite_export",Arrays.asList("tenant","id","status","payload","empty_text","null_text","raw"));assertEquals(3,detail.length());assertEquals(largeText(),detail.getJSONObject(0).getString("payload"));assertEquals("",detail.getJSONObject(0).getString("empty_text"));assertTrue(detail.getJSONObject(0).isNull("null_text"));assertEquals("[BINARY_PAYLOAD_NOT_INCLUDED bytes=2]",detail.getJSONObject(0).getString("id"));assertEquals("[BINARY_PAYLOAD_NOT_INCLUDED bytes=0]",detail.getJSONObject(1).getString("raw"));
        assertEquals("ordinary",rows(db,"misleading",Arrays.asList("marker")).getJSONObject(0).getString("marker"));assertEquals(2,groups(db,"rowid_shadow","status").getJSONObject(0).getLong("rows"));assertEquals("A",groups(db,"nocase_group","status").getJSONObject(0).getString("value"));assertEquals(2,groups(db,"nocase_group","status").getJSONObject(0).getLong("rows"));assertEquals(3,rows(db,"rowid_shadow",Arrays.asList("rowid","status")).length());
        StringWriter state=new StringWriter();ReportMetadataStreamR81.writeState(db,"r975_director_priority",state,()->true);assertEquals(largeText(),new JSONObject(state.toString()).getString("value"));
        StringWriter tail=new StringWriter();tail.write('[');assertEquals(5000,ReportMetadataStreamR81.writeLastRows(db,"r952_candidate",Arrays.asList("id","status"),tail,()->true,5000));tail.write(']');JSONArray last=new JSONArray(tail.toString());assertEquals("k015005",last.getJSONObject(0).getString("id"));assertEquals("k020004",last.getJSONObject(4999).getString("id"));
        File evidence=ReportDiagnosticEvidenceR64.write(db,output,"SYNTHETIC_SNAPSHOT_NOT_USER_DATA",()->true);JSONObject report=new JSONObject(new String(Files.readAllBytes(evidence.toPath()),StandardCharsets.UTF_8));boolean found=false;JSONArray recent=report.getJSONArray("recentEvidence");for(int i=0;i<recent.length();i++){JSONObject entry=recent.getJSONObject(i);if(entry.getString("table").equals("r952_candidate")){assertEquals(20005,entry.getLong("totalRows"));assertEquals(5000,entry.getLong("exportedRows"));assertFalse(entry.getBoolean("complete"));assertEquals("PRIMARY_KEY_ASCENDING",entry.getString("rowOrder"));assertEquals("LAST_5000_IN_STABLE_TABLE_ORDER",entry.getString("scope"));found=true;}}assertTrue(found);assertTrue(evidence.length()>0);
        try{ReportMetadataStreamR81.writeRows(db,"composite_export",Arrays.asList("payload"),new StringWriter(),()->false);fail("Uncancellable export");}catch(InterruptedIOException expected){}
    }
    @Test public void fullDiagnosticEvidenceExportsWithoutRowidAndLargeTextFromReadOnlySnapshot()throws Exception{
        File root=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"report-without-rowid-"+UUID.randomUUID());assertTrue(root.mkdirs());File database=new File(root,"snapshot.db");
        try{try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(database,null)){fixture(db);}try(SQLiteDatabase snapshot=SQLiteDatabase.openDatabase(database.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY)){verify(snapshot,root);}}
        finally{File[] files=root.listFiles();if(files!=null)for(File f:files)assertTrue(f.delete());assertTrue(root.delete());}
    }
    public static void verifyDirectoryAliases(File directory)throws Exception{
        java.nio.file.Path root=directory.toPath(),data=root.resolve("data");Files.createDirectories(data.resolve("app/files"));Files.createSymbolicLink(root.resolve("user0"),data);
        File aliased=root.resolve("user0/app/files").toFile();File canonical=ReportDirectoriesCP12.root(aliased);assertEquals(data.resolve("app/files/r981_reports").toFile().getCanonicalFile(),canonical);
        String id=String.join("",Collections.nCopies(64,"a"));
        try(ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(aliased)){
            assertNotNull(journal);ReportJobJournalCP12.Job job=new ReportJobJournalCP12.Job(id,id,id,"FULL_REPORT",1234);journal.create(job);journal.transition(job,ReportJobJournalCP12.Stage.BACKUP,1235);
            assertEquals("BACKUP",ReportJobJournalCP12.latestFullReport(canonical).stage);assertTrue(new File(canonical,"requests/"+id+".ack.json").isFile());
        }
        File outside=root.resolve("outside").toFile();assertTrue(outside.mkdir());java.nio.file.Path bad=root.resolve("bad/files");Files.createDirectories(bad);Files.createSymbolicLink(bad.resolve("r981_reports"),outside.toPath());
        try{ReportDirectoriesCP12.root(bad.toFile());fail("Report root symlink accepted");}catch(IOException expected){assertEquals("REPORT_ROOT_SYMLINK",expected.getMessage());}
        Files.createDirectories(root.resolve("files-link-app"));Files.createSymbolicLink(root.resolve("files-link-app/files"),outside.toPath());
        try{ReportDirectoriesCP12.root(root.resolve("files-link-app/files").toFile());fail("Files node symlink accepted");}catch(IOException expected){assertEquals("REPORT_FILES_SYMLINK",expected.getMessage());}
        assertEquals(0,Objects.requireNonNull(outside.list()).length);
    }
    @Test public void androidAppDirectoryAncestorAliasCanPersistReportJobsWithoutAllowingPrivateNodeLinks()throws Exception{
        android.content.Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();File root=new File(app.getCacheDir(),"report-app-alias-"+UUID.randomUUID());assertTrue(root.mkdirs());
        try{verifyDirectoryAliases(root);File actual=ReportDirectoriesCP12.root(app.getFilesDir());assertEquals(actual.getCanonicalFile(),actual);try(ReportJobJournalCP12 claim=ReportJobJournalCP12.tryClaim(app.getFilesDir())){if(claim!=null)assertEquals(actual,claim.root());}}
        finally{try(java.util.stream.Stream<java.nio.file.Path> paths=Files.walk(root.toPath())){for(java.nio.file.Path p:(Iterable<java.nio.file.Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.delete(p);}}
    }
}
