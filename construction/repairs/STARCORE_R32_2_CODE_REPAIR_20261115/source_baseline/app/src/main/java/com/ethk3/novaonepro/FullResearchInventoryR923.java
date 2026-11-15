package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

/** Explicit full-report inventory. Never called from the realtime prediction pipeline. */
public final class FullResearchInventoryR923 {
    public static final String VERSION = "R923_FULL_RESEARCH_INVENTORY_V1";
    private FullResearchInventoryR923() {}

    public static String capture(DbStore store) {
        StringBuilder out = new StringBuilder(32_768);
        try { writeTo(store, out); } catch (java.io.IOException error) {
            throw new IllegalStateException("INVENTORY_WRITE_FAILED", error);
        }
        return out.toString();
    }

    public static void writeTo(DbStore store, Appendable out) throws java.io.IOException { writeTo(store,out,false); }
    public static void writeToStrict(DbStore store, Appendable out) throws java.io.IOException { writeTo(store,out,true); }
    private static void writeTo(DbStore store, Appendable out, boolean strict) throws java.io.IOException {
        out.append(VERSION).append("\nScope=ALL_ROWS_ALL_COLUMNS_OF_LISTED_TABLES")
                .append("|ArchivedAssets=INCLUDED|RowLimit=NONE")
                .append("\nFormat=TSV|Escapes=\\\\,\\t,\\r,\\n|SQL_NULL=\\N|Blob=base64:")
                .append("\n每张表独立读取，后台可能继续迭代；本节是完整资产清单，不是整个历史数据库备份。\n");
        if (store == null) { if(strict)throw new java.io.IOException("INVENTORY_STORE_NULL"); out.append("STATUS=READ_ERROR|STORE_NULL\n"); return; }
        final SQLiteDatabase db;
        try { db = store.readable(); }
        catch (Throwable error) {
            if(strict)throw new java.io.IOException("INVENTORY_DATABASE",error);
            out.append("STATUS=READ_ERROR|").append(error.getClass().getSimpleName()); return;
        }
        appendTable(out, db, "v57_attempt_resource_r927", "execution_id", strict);
        appendTable(out, db, DbHelper.TABLE_R908_ALGORITHM_GENOME, "genome_id", strict);
        appendTable(out, db, AssetGenesisEngineR915.TABLE_ASSET_GENOME, "asset_id", strict);
        appendTable(out, db, DbHelper.TABLE_R906_RESEARCH_ASSET, "asset_id", strict);
        appendTable(out, db, DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE, "asset_id", strict);
        appendTable(out, db, "v57_lifecycle_audit_r925", "genome_id", strict);
        appendTable(out, db, "v57_candidate_coverage_r925", "genome_id", strict);
        appendTable(out, db, SchemaRevisionR925.TABLE_PROMOTION_WINDOW, "genome_id", strict);
        appendTable(out, db, SchemaRevisionR925.TABLE_PROMOTION_HISTORY, "genome_id,start_issue", strict);
        appendTable(out, db, SchemaRevisionR925.TABLE_HYPOTHESIS, "genome_id", strict);
        appendTable(out, db, "v57_research_candidate_r811", "candidate_id", strict);
        appendTable(out, db, "v57_research_candidate_score_r900", "candidate_id", strict);
        appendTable(out, db, "v57_research_prune_log_r900", "id", strict);
        appendTable(out, db, "v57_research_codegen_r811", "candidate_id", strict);
        appendTable(out, db, AssetGenesisEngineR915.TABLE_ASSET_CERTIFICATE, "certificate_id", strict);
        appendTable(out, db, AssetGenesisEngineR915.TABLE_ATOM_PROPOSAL, "proposal_id", strict);
        appendTable(out, db, AssetGenesisEngineR915.TABLE_ATOM_SCHEMA, "atom_id", strict);
        appendTable(out, db, AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY, "asset_key", strict);
        out.append("\n统一研究生命周期、校准、证书、冻结选择与贡献原始表（R929/R930/R932）\n");
        try (Cursor tables = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND "
                + "(name GLOB 'research_*_r929' OR name GLOB 'research_*_r932' OR name GLOB '*_r930' OR name GLOB '*_r943' OR name GLOB 'r952_*' OR name GLOB 'r958_*' OR name GLOB '*_r967' OR name GLOB 'r967_*' OR name GLOB 'r974_*' OR name GLOB 'r975_*' OR name GLOB 'r979_*') ORDER BY name", null)) {
            java.util.ArrayList<String> names = new java.util.ArrayList<String>();
            while (tables.moveToNext()) {
                String name = tables.getString(0);
                if (name == null || !name.matches("[A-Za-z0-9_]+")) throw new IllegalStateException("INVENTORY_TABLE_IDENTIFIER");
                names.add(name);
            }
            for (String name : names) appendTable(out, db, name, "1", strict);
            out.append("UnifiedResearchTables=").append(String.valueOf(names.size())).append('\n');
        } catch (RuntimeException failure) {
            if(strict)throw new java.io.IOException("INVENTORY_DISCOVERY",failure);
            out.append("UnifiedResearchDiscovery=READ_ERROR:").append(failure.getClass().getSimpleName()).append('\n');
        }
        appendTable(out, db, ResearchMainlineContributionAuditR922.TABLE, "target_issue", strict);
    }

    private static void appendTable(Appendable out, SQLiteDatabase db, String table, String key, boolean strict) throws java.io.IOException {
        out.append("\n[TABLE ").append(table).append(" readStartedAt=").append(String.valueOf(System.currentTimeMillis())).append("]\n");
        Cursor cursor = null;
        long rows = 0;
        try {
            // Table and order identifiers are fixed internal constants, never caller input.
            cursor = db.rawQuery("SELECT * FROM " + table + " ORDER BY " + key, null);
            String[] columns = cursor.getColumnNames();
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) out.append('\t');
                out.append(escape(columns[i]));
            }
            out.append('\n');
            while (cursor.moveToNext()) {
                for (int i = 0; i < columns.length; i++) {
                    if (i > 0) out.append('\t');
                    if (cursor.isNull(i)) out.append("\\N");
                    else if (cursor.getType(i) == Cursor.FIELD_TYPE_BLOB)
                        out.append("base64:").append(Base64.encodeToString(cursor.getBlob(i), Base64.NO_WRAP));
                    else out.append(escape(cursor.getString(i)));
                }
                out.append('\n');
                rows++;
            }
            out.append("[END_TABLE rows=").append(String.valueOf(rows)).append(" readFinishedAt=").append(String.valueOf(System.currentTimeMillis())).append(" status=OK]\n");
        } catch (java.io.IOException error) { throw error;
        } catch (Exception error) {
            if(strict)throw new java.io.IOException("INVENTORY_TABLE:"+table+"|rows="+rows,error);
            out.append("[END_TABLE rowsRead=").append(String.valueOf(rows)).append(" status=READ_ERROR reason=")
                    .append(error.getClass().getSimpleName()).append(':')
                    .append(escape(String.valueOf(error.getMessage()))).append("]\n");
        } finally { if (cursor != null) cursor.close(); }
    }

    static String escape(String value) {
        if (value == null) return "\\N";
        return value.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\r", "\\r").replace("\n", "\\n");
    }
}
