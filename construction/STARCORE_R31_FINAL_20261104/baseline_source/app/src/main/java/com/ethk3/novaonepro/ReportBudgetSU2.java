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
    public static final long PUBLISHED_QUOTA_BYTES = 512L << 20;          // all published reports together
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
            File dir = new File(root, kind); File[] items = dir.listFiles(); if (items == null) continue;
            List<File> ds = new ArrayList<>(); for (File f : items) if (f.isDirectory() && f.getName().matches("[0-9a-f]{64}")) ds.add(f);
            ds.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (int i = 0; i < ds.size(); i++) {
                File d = ds.get(i);
                if (i < RECOVERY_KEEP && now - d.lastModified() < RECOVERY_MAX_AGE_MS) continue;
                if (!d.getCanonicalFile().getParentFile().equals(dir.getCanonicalFile())) throw new IOException("REPORT_RECOVERY_PATH");
                deleteTree(d); removed++;
            }
        }
        return removed;
    }

    static void deleteTree(File f) throws IOException {
        if (!f.getAbsoluteFile().equals(f.getCanonicalFile())) throw new IOException("REPORT_RECOVERY_SYMLINK");
        File[] kids = f.listFiles(); if (kids != null) for (File k : kids) deleteTree(k);
        if (f.exists() && !f.delete()) throw new IOException("REPORT_RECOVERY_DELETE:" + f.getName());
    }

    /**
     * A job directory whose job.json cannot be read: abandoned when it is a 64-hex job id, unpublished and untouched
     * for 24 h (ReportPublicationCP12.pruneTerminal used to throw on it and stop all further cleanup).
     */
    public static boolean abandoned(File jobDir, long now) {
        return jobDir.isDirectory() && jobDir.getName().matches("[0-9a-f]{64}") && !new File(jobDir, "published.json").exists()
            && now - jobDir.lastModified() > RECOVERY_MAX_AGE_MS;
    }

    public static long deadline(long stageStarted, long milliseconds, boolean fullReport) {
        return stageStarted + (fullReport ? Math.max(milliseconds, FULL_REPORT_STAGE_MS) : milliseconds);
    }
}
