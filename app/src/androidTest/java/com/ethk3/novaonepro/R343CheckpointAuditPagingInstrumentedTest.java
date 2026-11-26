package com.ethk3.novaonepro;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.Field173NativeEvidence.Checks;
import com.ethk3.novaonepro.strategyv2.CheckpointArtifactsR26;
import com.ethk3.novaonepro.strategyv2.CheckpointReferencesR26;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.json.JSONObject;
import org.junit.Test;

/** Actual Android checkpoint auditing of two pinned read-only snapshots of one fixture database. */
public final class R343CheckpointAuditPagingInstrumentedTest {
    private static final long MIB=1L<<20;
    private static final String MISSING="f".repeat(64);

    private static void snapshot(File file, List<String> roots) {
        try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(file,null)){
            db.beginTransaction();
            try{
                RuntimeObservationScopeR341.installWithinTransaction(db);
                db.execSQL("CREATE TABLE r31_gc_pin (artifact_hash TEXT PRIMARY KEY NOT NULL)");
                for(String hash:roots)db.execSQL("INSERT INTO r31_gc_pin VALUES(?)",new String[]{hash});
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
    }

    private static void copyWithDifferentSnapshot(File prior,File next) throws Exception {
        try(FileInputStream in=new FileInputStream(prior);FileOutputStream out=new FileOutputStream(next)){
            byte[] buf=new byte[8192];
            int size;
            while((size=in.read(buf))!=-1)out.write(buf,0,size);
            out.getFD().sync();
        }
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(next.getPath(),null,SQLiteDatabase.OPEN_READWRITE)){
            db.setVersion(2);
        }
    }

    private static JSONObject observe(Checks checks, Context app, File checkpointRoot, File snapshot,
            File cursor, StringWriter out) throws Exception {
        String sha=ReportArchiveCP4.hash(snapshot);
        try(ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(app.getFilesDir());
            SQLiteDatabase db=SQLiteDatabase.openDatabase(snapshot.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
            checks.require(ReportInputsLeaseCP12.currentThreadHoldsInput(app.getFilesDir()),"Real report input is pinned");
            checks.require(db.isReadOnly(),"Audit source SQLite stays read-only");
            return CheckpointReportAuditR32.observePage(checkpointRoot,
                    accept->CheckpointReferencesR26.forEachHashR324(db,accept),out,()->true,
                    cursor,sha,RuntimeObservationScopeR341.databaseIdentity(db),BuildConfig.SOURCE_COMMIT_R26);
        }
    }

    @Test public void secondPinnedSnapshotVerifiesDeferredBodiesWithoutPromotingHistoricalBytes() throws Exception {
        Field173NativeEvidence.run((app,checks)->{
            File path=new File(app.getCacheDir(),"checkpoint-paging-fixture");
            File bodyRoot=new File(path,"r26-checkpoints");
            checks.require(bodyRoot.mkdirs(),"Only the isolated test storage is used");
            List<String> roots=new ArrayList<>();
            Random random=new Random(343017);
            for(int i=0;i<10;i++){
                byte[] body=new byte[(int)MIB];random.nextBytes(body);
                String hash=CheckpointArtifactsR26.sha(body);
                try(FileOutputStream out=new FileOutputStream(new File(bodyRoot,hash+".bin"))){
                    out.write(body);out.getFD().sync();
                }
                roots.add(hash);
            }
            roots.add(MISSING);
            File first=new File(path,"first.sqlite"),second=new File(path,"second.sqlite");
            snapshot(first,roots);copyWithDifferentSnapshot(first,second);
            String firstHash=ReportArchiveCP4.hash(first),secondHash=ReportArchiveCP4.hash(second);
            checks.require(!firstHash.equals(secondHash),"Actual SQLite snapshot bytes changed with roots held fixed");
            File cursor=new File(path,CheckpointAuditContinuationR343.FILE);
            StringWriter firstOriginal=new StringWriter(),secondOriginal=new StringWriter();
            JSONObject before=observe(checks,app,bodyRoot,first,cursor,firstOriginal);
            JSONObject firstPage=before.getJSONObject("continuationR343");
            checks.equal(8L*MIB,before.getLong("bytesRead"),"Current page remains within 8 MiB");
            checks.equal(2L,firstPage.getLong("deferredObjectsThisPage"),"Two entries remain unverified");
            checks.equal("DURABLE",firstPage.getString("progressWriteState"),"Framework AtomicFile committed the cursor");
            JSONObject after=observe(checks,app,bodyRoot,second,cursor,secondOriginal);
            JSONObject secondPage=after.getJSONObject("continuationR343");
            checks.equal(2L*MIB,after.getLong("bytesRead"),"Second page reads the originally deferred bodies");
            checks.equal(2L,after.getLong("verifiedObjects"),"Two actual current bytes were independently verified");
            checks.equal(8L,secondPage.getLong("historicalPreviouslyVerifiedObjects"),"Original checks retain historical scope");
            checks.equal(firstHash,secondPage.getString("previousPageSnapshotSha256"),"Continuation cites the actual source");
            checks.equal(secondHash,secondPage.getString("snapshotSha256"),"Current output cites the actual snapshot");
            checks.equal(firstPage.getString("rootSetSha256"),secondPage.getString("rootSetSha256"),"Reference membership remains exact");
            checks.equal(0L,secondPage.getLong("deferredObjectsThisPage"),"Budget-deferred cohort was processed");
            checks.equal(1L,after.getLong("missingObjects"),"Paging does not recover bytes that were absent");
            checks.require(!after.getBoolean("sourceClosureVerified"),"Historical facts do not prove present closure");
            checks.require(!after.getBoolean("gcOrModelUseAuthorized"),"The diagnostic does not authorize models or GC");
            try(SQLiteDatabase db=SQLiteDatabase.openDatabase(second.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
                checks.databaseWithoutTarget("ANDROID_ENGINEERING_CHECKPOINT_AUDIT_FIXTURE",
                        RuntimeObservationScopeR341.databaseIdentity(db),"Checkpoint audit has no prediction target");
            }
            checks.record("firstAuditOriginal",firstOriginal.toString());
            checks.record("secondAuditOriginal",secondOriginal.toString());
        });
    }
}
