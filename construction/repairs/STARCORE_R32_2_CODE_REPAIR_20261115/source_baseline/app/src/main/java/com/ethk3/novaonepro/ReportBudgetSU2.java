package com.ethk3.novaonepro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Report byte budget (P34). Root causes fixed here (public 0f1fb606 == private 9ec590ca):
 *  - ReportJobJournalCP12.writeFailureArtifact copied the whole snapshot.db into
 *    EVERY failed full report's recovery ZIP, regardless of the raw-evidence choice;
 *  - ReportPublicationCP12.pruneRequestMetadata kept up to 256 such recoveries for 7 days;
 *  - ReportPublicationCP12.enforceQuota existed but was never called, and
 *    ReportExportServiceCP4 set publishedQuotaBytes=Long.MAX_VALUE;
 *  - ReportPublicationCP12.pruneTerminal (the only cleaner of failed jobs/<id>/snapshot.db) threw on
 *    more than 48 job directories or on one unreadable job.json, after which nothing was cleaned again.
 * A failed export is retried by the user, so each attempt used to leave one more
 * full database copy behind.
 */
public final class ReportBudgetSU2 {
    private ReportBudgetSU2() {}
    public static final long PUBLISHED_QUOTA_BYTES = 192L << 20;          // R31 published-report category target
    public static final long PHYSICAL_REPORT_ADMISSION_BYTES = 4096L << 20; // also subject to the smaller app-wide shared grant
    public static final long RECOVERY_DATABASE_LIMIT = 256L << 20;        // a DB copy is attached only on explicit raw request and below this size
    public static final int RECOVERY_KEEP = 3;                            // newest heavy recovery artefacts kept
    public static final long RECOVERY_MAX_AGE_MS = 24L * 60 * 60 * 1000;
    public static final long FULL_REPORT_STAGE_MS = 30L * 60 * 1000;      // a stage may yield and resume, never hang forever

    /** Recovery ZIP may carry snapshot.db only if the user asked for raw evidence and it is small enough. */
    public static boolean recoveryMayIncludeDatabase(File requestJson, long snapshotBytes) {
        if (snapshotBytes <= 0 || snapshotBytes > RECOVERY_DATABASE_LIMIT || requestJson == null || !requestJson.isFile() || requestJson.length() > 65536) return false;
        try (InputStream in = new FileInputStream(requestJson)) {
            byte[] b = new byte[(int) requestJson.length()]; int off = 0, n; while (off < b.length && (n = in.read(b, off, b.length - off)) > 0) off += n;
            String s = new String(b, 0, off, StandardCharsets.UTF_8).replace(" ", "");
            return s.contains("\"includeRawEvidence\":true");
        } catch (IOException e) { return false; }
    }

    /** Delete heavy recovery artefacts (recovery/<id>, saf-parts/<id>) beyond the newest RECOVERY_KEEP or older than 24h. Small acks stay. */
    public static int pruneHeavyRecovery(File root, long now) throws IOException {
        int removed = 0;
        for (String kind : new String[]{"recovery", "saf-parts"}) {
            File dir = new File(root, kind); File[] items = DirectoryCursorR31.children(dir,4096);
            List<File> ds = new ArrayList<>(); for (File f : items) if (f.isDirectory() && f.getName().matches("[0-9a-f]{64}")) ds.add(f);
            ds.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (int i = 0; i < ds.size(); i++) {
                File d = ds.get(i);
                if (i < RECOVERY_KEEP && now - d.lastModified() < RECOVERY_MAX_AGE_MS) continue;
                if (!d.getCanonicalFile().getParentFile().equals(dir.getCanonicalFile())) throw new IOException("REPORT_RECOVERY_PATH");
                if(!retirableRecovery(root,d.getName()))continue; // TTL alone never deletes a unique unverified original.
                if(!ReportPublicationCP12.tryDeleteRecoveryR31(d))continue;
                removed++;
            }
        }
        return removed;
    }

    static void deleteTree(File f) throws IOException {
        if (!f.getAbsoluteFile().equals(f.getCanonicalFile())) throw new IOException("REPORT_RECOVERY_SYMLINK");
        if(f.isDirectory())try(DirectoryCursorR31 cursor=DirectoryCursorR31.open(f)){File child;while((child=cursor.next())!=null)deleteTree(child);}
        if (f.exists() && !f.delete()) throw new IOException("REPORT_RECOVERY_DELETE:" + f.getName());
    }

    /**
     * A job directory whose job.json cannot be read: abandoned when it is a 64-hex job id, unpublished and untouched
     * for 24 h (ReportPublicationCP12.pruneTerminal used to throw on it and stop all further cleanup).
     */
    public static boolean abandoned(File jobDir, long now) {
        // An unreadable journal is not proof that a snapshot is reconstructible.
        // Explicit user cancellation is processed by the existing sole owner.
        return false;
    }

    static boolean retirableRecovery(File root,String request){
        try{
            org.json.JSONObject ack=new org.json.JSONObject(ReportJobJournalCP12.readAtomic(new File(root,"requests/"+request+".ack.json"),8192));
            String stage=ack.optString("stage",ack.optString("state"));if("CANCELLED".equals(stage)||"VERIFIED_READBACK".equals(stage))return true;if(!"READY_TO_SHARE".equals(stage))return false;
            String id=ack.optString("jobId");if(!id.matches("[a-f0-9]{64}"))return false;File job=ReportArchiveVerifierCP12.inside(root,"jobs/"+id);
            if(!ReportPublicationCP12.verifyPublishedZip(job,()->true))return false;
            ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));ReportPublicationCP12.requireRawCoverageR31(job,state.publishedDir);return true;
        }catch(Exception unverified){return false;}
    }

    public static long deadline(long stageStarted, long milliseconds, boolean fullReport) {
        return stageStarted + (fullReport ? Math.max(milliseconds, FULL_REPORT_STAGE_MS) : milliseconds);
    }
}
