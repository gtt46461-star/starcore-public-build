package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.channels.*;
import org.json.JSONObject;

/** One disposable immutable page copy for the sole diagnostics owner, never a live DB writer.
 * A requested page must match the copy's exact schema and generations. Publication still
 * rechecks every generation and eligibility predicate in the existing writer transaction. */
public final class RetentionSnapshotCacheR73 {
    private static final long MAX_AGE_MS=10*60*1000L, SQLITE_BYTES=32L<<20, RESERVE=64L<<20;
    private static final int MAX_USES=256;
    private static RetentionSnapshotRowsCP7.VerifiedSnapshot.ReuseProof warmProof;
    private RetentionSnapshotCacheR73(){}

    public static final class Lease implements Closeable {
        public final File file;
        public final RetentionSnapshotRowsCP7.VerifiedSnapshot verified;
        public final long capturedWallMs;
        public final boolean reused;
        private Lease(File file,RetentionSnapshotRowsCP7.VerifiedSnapshot verified,long captured,boolean reused){
            this.file=file;this.verified=verified;capturedWallMs=captured;this.reused=reused;
        }
        public void close()throws IOException {verified.close();}
    }
    private static File directory(ReportJobJournalCP12 journal)throws IOException {
        journal.assertOwned();File root=ReportArchiveVerifierCP12.inside(journal.root(),"retention-snapshot-r73");
        if(!root.isDirectory()&&!root.mkdirs())throw new IOException("RETENTION_SNAPSHOT_DIRECTORY");
        return root;
    }
    private static void remove(File file)throws IOException {
        if(file.exists()&&!file.delete())throw new IOException("RETENTION_SNAPSHOT_CLEANUP");
    }
    /** Caller owns the diagnostics journal and has closed the previous cache lease. */
    public static synchronized void discard(ReportJobJournalCP12 journal)throws IOException {
        File root=directory(journal);
        try(RandomAccessFile lock=new RandomAccessFile(ReportArchiveVerifierCP12.inside(root,"snapshot.db.lock"),"rw")){
            FileLock lease;try{lease=lock.getChannel().tryLock();}catch(OverlappingFileLockException busy){throw new IOException("RETENTION_SNAPSHOT_BUSY",busy);}
            if(lease==null)throw new IOException("RETENTION_SNAPSHOT_BUSY");
            try{
                warmProof=null;
                // Never touch live SQLite/WAL files, CAS archives, or published report evidence.
                for(String name:new String[]{"snapshot.json","snapshot.json.bak","snapshot.json.new","snapshot.db","snapshot.db-journal","snapshot.db-wal","snapshot.db-shm"})
                    remove(ReportArchiveVerifierCP12.inside(root,name));
            }finally{lease.release();}
        }
    }
    private static JSONObject metadata(File path)throws IOException {
        if(!path.exists()&&!new File(path.getPath()+".bak").exists())return null;
        try{return new JSONObject(ReportJobJournalCP12.readAtomic(path,4096));}
        catch(org.json.JSONException invalid){return null;}
    }
    private static boolean matches(JSONObject m,File source,File snapshot,long now)throws IOException {
        if(m==null)return false;
        long created=m.optLong("capturedWallMs",-1),bytes=m.optLong("bytes",-1),uses=m.optLong("uses",-1);
        return "RETENTION_SNAPSHOT_R76_2".equals(m.optString("revision"))
            &&ReleaseManifestR926.MANIFEST_SHA256.equals(m.optString("releaseHash"))
            &&source.getCanonicalPath().equals(m.optString("source"))
            &&m.optString("sha256").matches("[0-9a-f]{64}")
            &&created>0&&now>=created&&now-created<MAX_AGE_MS&&uses>=0&&uses<MAX_USES
            &&bytes>0&&snapshot.isFile()&&snapshot.length()==bytes;
    }
    private static boolean contains(File snapshot,RetentionArchiveProtocolCP12.Page page,ReportNativeCP4.Control control)throws IOException {
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(snapshot.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){
            try(Cursor table=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{page.table})){if(!table.moveToFirst())return false;}
            if(!page.schemaHash.equals(DataRetentionGovernorR900.schemaHashCP12(db,page.table)))return false;
            for(RetentionArchiveProtocolCP12.RowVersion row:page.rows){
                if(!control.shouldContinue())throw new InterruptedIOException("ARCHIVE_CANCELLED");
                try(Cursor c=db.rawQuery("SELECT COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table=? AND r.source_rowid=t.rowid),0) FROM \""+page.table+"\" t WHERE t.rowid=?",new String[]{page.table,Long.toString(row.rowId)})){
                    if(!c.moveToFirst()||c.getLong(0)!=row.generation)return false;
                }
            }
            return true;
        }
    }
    /** Cancellation is never converted into a cache miss or a fresh expensive backup. */
    public static synchronized Lease acquire(ReportJobJournalCP12 journal,File source,RetentionArchiveProtocolCP12.Page page,
            ReportNativeCP4.Control control,long now,long availableBytes)throws Exception {
        if(source==null||page==null||control==null||now<=0||availableBytes<0)throw new IOException("RETENTION_SNAPSHOT_INPUT");
        if(!ReleaseManifestR926.MANIFEST_SHA256.equals(page.releaseHash)
                ||!RetentionTableRegistryCP12.require(page.table).archiveAndPrune()
                ||!RetentionTableRegistryCP12.hashForTable(page.table).equals(page.registryHash))throw new IOException("RETENTION_REQUEST_IDENTITY");
        File root=directory(journal),snapshot=ReportArchiveVerifierCP12.inside(root,"snapshot.db"),meta=ReportArchiveVerifierCP12.inside(root,"snapshot.json");
        JSONObject m=metadata(meta);RetentionSnapshotRowsCP7.VerifiedSnapshot verified=null;
        if(matches(m,source,snapshot,now)){
            try{verified=RetentionSnapshotRowsCP7.VerifiedSnapshot.verifyReusable(root,snapshot,m.optString("sha256"),control,warmProof);}
            catch(InterruptedIOException cancelled){throw cancelled;}
            catch(IOException invalid){
                if(!"ARCHIVE_SNAPSHOT_HASH".equals(invalid.getMessage()))throw invalid;
                warmProof=null;
            }
            if(verified!=null){
                boolean keep=false;
                try{
                    if(contains(snapshot,page,control)){
                        // Worst-case archive output is bounded by the copied DB plus framing.
                        if(availableBytes<Math.addExact(snapshot.length(),RESERVE))throw new IOException("REPORT_DISK_BUDGET");
                        warmProof=verified.reuseProof();m.put("uses",m.getLong("uses")+1);
                        ReportJobJournalCP12.writeAtomic(meta,m.toString());keep=true;
                        return new Lease(snapshot,verified,m.getLong("capturedWallMs"),true);
                    }
                }catch(org.json.JSONException invalid){throw new IOException("RETENTION_SNAPSHOT_METADATA",invalid);}
                finally{if(!keep)verified.close();}
            }
        }
        if(!control.shouldContinue())throw new InterruptedIOException("ARCHIVE_CANCELLED");
        long raw=Math.addExact(source.length(),new File(source.getPath()+"-wal").length());
        // Discard the old private copy before reserving its replacement, never accumulate copies.
        long reclaimed=snapshot.isFile()?snapshot.length():0;
        discard(journal);
        long available=Math.addExact(availableBytes,reclaimed);
        if(available<RESERVE+131072L)throw new IOException("REPORT_DISK_BUDGET");
        verified=null;
        try {
        boolean fullFallback=false;
        try {
            // Disk admission covers this page plus its independently verified SCROW copy,
            // not two copies of unrelated multi-GB history. Native max_page_count enforces it.
            ReportNativeCP4.retentionPage(source,snapshot,page.table,page.rowIds(),SQLITE_BYTES,(available-RESERVE)/2L,control);
        }catch(ReportNativeCP4.Failure failed){
            if(failed.nativeCode==13)throw new IOException("REPORT_DISK_BUDGET",failed); // SQLITE_FULL
            if(failed.nativeCode==12)throw new IOException("STALE_PAGE",failed); // SQLITE_NOTFOUND
            if(failed.nativeCode!=7)throw failed; // only SQLITE_NOMEM permits the old streaming route
            // A single very large TEXT/BLOB may exceed the bounded INSERT working set.
            // Preserve the pre-R76 incremental-reader contract, never truncate or skip it.
            if(!control.shouldContinue())throw new InterruptedIOException("ARCHIVE_CANCELLED");
            if(available<Math.addExact(Math.multiplyExact(raw,2L),RESERVE))throw new IOException("REPORT_DISK_BUDGET",failed);
            fullFallback=true;ReportNativeCP4.backup(source,snapshot,SQLITE_BYTES,control);
        }
        String hash=ReportArchiveCP4.hash(snapshot,control);
        verified=RetentionSnapshotRowsCP7.VerifiedSnapshot.verify(root,snapshot,hash,control);
            if(!contains(snapshot,page,control))throw new IOException("STALE_PAGE");
            m=new JSONObject().put("revision","RETENTION_SNAPSHOT_R76_2").put("fullFallback",fullFallback).put("releaseHash",ReleaseManifestR926.MANIFEST_SHA256)
                .put("source",source.getCanonicalPath()).put("sha256",hash).put("bytes",snapshot.length()).put("capturedWallMs",now).put("uses",1);
            ReportJobJournalCP12.writeAtomic(meta,m.toString());warmProof=verified.reuseProof();
            return new Lease(snapshot,verified,now,false);
        }catch(Exception|Error failed){
            // Hash/verification may itself be cancelled after native copy succeeded.
            // Such a file is unpublished and must not linger (especially a large fallback).
            if(verified!=null)try{verified.close();}catch(Exception cleanup){failed.addSuppressed(cleanup);}
            try{discard(journal);}catch(Exception cleanup){failed.addSuppressed(cleanup);}
            throw failed;
        }
    }
}
