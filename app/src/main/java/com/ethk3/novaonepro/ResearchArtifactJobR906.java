package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Creates immutable offline-training jobs for non-executable method blueprints. */
public final class ResearchArtifactJobR906 {
    public static final boolean MAY_AUTHORIZE_REAL = false;
    private ResearchArtifactJobR906() {}

    public static int ensureWaitingJobs(final DbStore store, final int max) {
        if (store == null) return 0;
        final int bound = Math.max(1, Math.min(16, max));
        final int[] made = {0};
        store.coordinatorR36().runWrite("R906_ARTIFACT_JOB_CREATE", new DatabaseCoordinator.WriteRunnable() {
            @Override public void run(SQLiteDatabase db) {
                String dataUntil = scalar(db, "SELECT MAX(target_issue) FROM " + DbHelper.TABLE_R35_OOS_LABEL + " WHERE strict_oos=1");
                if (dataUntil.length() == 0) return;
                String datasetHash = datasetHash(db, dataUntil);
                Cursor c = null;
                try {
                    c = db.rawQuery(
                            "SELECT asset_id,canonical_spec,feature_schema_hash,method_signature FROM "
                                    + DbHelper.TABLE_R906_RESEARCH_ASSET
                                    + " a WHERE artifact_status='WAITING_OFFLINE_TRAINER'"
                                    + " AND NOT EXISTS(SELECT 1 FROM " + DbHelper.TABLE_R906_RESEARCH_ARTIFACT_JOB
                                    + " j WHERE j.asset_id=a.asset_id AND j.dataset_hash=?)"
                                    + " ORDER BY created_at ASC LIMIT ?",
                            new String[] {datasetHash, String.valueOf(bound)});
                    while (c.moveToNext()) {
                        String asset = safe(c.getString(0)), spec = safe(c.getString(1));
                        String schema = safe(c.getString(2)), sig = safe(c.getString(3));
                        long seed = positiveSeed(sig);
                        String protocol = V51ChecksumEngine.sha256(
                                "R906_OFFLINE_PROTOCOL_V1|PIT_EXPANDING_WINDOW|SEALED_FUTURE|HOLM|BLOCK_BOOTSTRAP|"
                                        + schema + '|' + spec);
                        String jobId = "R906-JOB-" + V51ChecksumEngine.sha256(
                                asset + '|' + datasetHash + '|' + protocol).substring(0, 28);
                        long now = System.currentTimeMillis();
                        ContentValues v = new ContentValues();
                        v.put("job_id", jobId); v.put("asset_id", asset); v.put("job_status", "READY_TO_EXPORT");
                        v.put("dataset_hash", datasetHash); v.put("data_until_issue", dataUntil);
                        v.put("feature_schema_hash", schema); v.put("canonical_method_spec", spec);
                        v.put("seed", seed); v.put("protocol_hash", protocol); v.put("artifact_hash", "");
                        v.put("manifest_hash", ""); v.put("parity_status", "NOT_RUN");
                        v.put("last_error_code", ""); v.put("created_at", now); v.put("updated_at", now);
                        long row = db.insertWithOnConflict(DbHelper.TABLE_R906_RESEARCH_ARTIFACT_JOB, null, v, SQLiteDatabase.CONFLICT_IGNORE);
                        if (row >= 0L) made[0]++;
                    }
                } finally { if (c != null) c.close(); }
            }
        });
        return made[0];
    }

    private static String datasetHash(SQLiteDatabase db, String until) {
        Cursor c = null; StringBuilder b = new StringBuilder("R906_PIT_DATASET|").append(until);
        try {
            c = db.rawQuery(
                    "SELECT target_issue,provenance_hash,record_hash FROM " + DbHelper.TABLE_R35_OOS_LABEL
                            + " WHERE strict_oos=1 AND target_issue<=? ORDER BY target_issue ASC",
                    new String[] {until});
            while (c.moveToNext()) b.append('|').append(safe(c.getString(0))).append(':')
                    .append(safe(c.getString(1))).append(':').append(safe(c.getString(2)));
        } finally { if (c != null) c.close(); }
        return V51ChecksumEngine.sha256(b.toString());
    }

    private static String scalar(SQLiteDatabase db, String sql) {
        Cursor c = null; try { c = db.rawQuery(sql, null); return c.moveToFirst() ? safe(c.getString(0)) : ""; }
        finally { if (c != null) c.close(); }
    }
    private static long positiveSeed(String s) {
        long h = 1469598103934665603L; for (int i=0;i<s.length();i++){h^=s.charAt(i);h*=1099511628211L;}
        return h == Long.MIN_VALUE ? 1L : Math.abs(h);
    }
    private static String safe(String x) { return x == null ? "" : x.trim(); }
}
