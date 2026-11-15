package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** First cause is immutable; an absent legacy cause is never reconstructed from today's scores. */
public final class LifecycleAuditR925 {
    private LifecycleAuditR925() {}
    public static void record(SQLiteDatabase db,String id,String status,String reason,
            String metrics,String issue,int n,double captureCoverage) {
        long now=System.currentTimeMillis();
        if(status.startsWith("PRUNED")) {
            boolean available=!reason.contains("UNAVAILABLE")&&!reason.equals("IRREVERSIBLE_EVIDENCE_PRUNE");
            db.execSQL("INSERT OR IGNORE INTO v57_lifecycle_audit_r925 VALUES (?,?,?,?,?,?,?,?,?,?)",
                new Object[]{id,reason,metrics+"|n="+n+"|system_capture_coverage="+captureCoverage,
                    issue,now,AlgorithmEvidencePolicyR912.EVIDENCE_VERSION,available?1:0,status,reason,now});
        }
        db.execSQL("UPDATE v57_lifecycle_audit_r925 SET latest_status=?,latest_reason=?,updated_at=? WHERE genome_id=?",
            new Object[]{status,reason,now,id});
    }
    public static String report(DbStore store) {
        StringBuilder b=new StringBuilder("PRUNE_REASON_LEDGER\n原始原因不可恢复的旧记录明确标记，不根据当前指标编造。\n");
        Cursor c=store.readable().rawQuery("SELECT genome_id,first_prune_reason,first_prune_issue,original_available,latest_status,latest_reason,first_prune_metrics FROM v57_lifecycle_audit_r925 ORDER BY first_prune_at DESC",null);
        try {while(c.moveToNext()) {for(int i=0;i<c.getColumnCount();i++)b.append(c.getColumnName(i)).append('=').append(c.getString(i)).append(' ');b.append('\n');}}
        finally {c.close();}return b.toString();
    }

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        com.ethk3.novaonepro.ReportTextCP12 b=new com.ethk3.novaonepro.ReportTextCP12(report, "PRUNE_REASON_LEDGER\n原始原因不可恢复的旧记录明确标记，不根据当前指标编造。\n");
        Cursor c=report.select("SELECT genome_id,first_prune_reason,first_prune_issue,original_available,latest_status,latest_reason,first_prune_metrics FROM v57_lifecycle_audit_r925 ORDER BY first_prune_at DESC",null);
        try {while(c.moveToNext()) {for(int i=0;i<c.getColumnCount();i++)b.append(c.getColumnName(i)).append('=').append(c.getString(i)).append(' ');b.append('\n');}}
        finally {c.close();}return b.toString();
    }
}
