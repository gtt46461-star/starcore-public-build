package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * R926 deterministic backup manifest for same-user_version migrations.
 *
 * <p>Historical verification deliberately hashes only columns that existed before migration. This
 * allows additive columns/indexes while proving that pre-existing row values and row counts are
 * byte-for-byte stable. The backup itself is copied only after a successful WAL checkpoint and
 * helper close.</p>
 */
public final class DatabaseBackupManifestR926 {
    public static final String FORMAT = "R926_BACKUP_MANIFEST_V1";

    public static final class TableDigest {
        public final String table;
        public final String[] columns;
        public final long rows;
        public final String sha256;
        public final String status;

        TableDigest(String table, String[] columns, long rows, String sha256, String status) {
            this.table = safe(table);
            this.columns = columns == null ? new String[0] : columns.clone();
            this.rows = Math.max(0L, rows);
            this.sha256 = safe(sha256);
            this.status = safe(status);
        }

        public String canonical() {
            StringBuilder b = new StringBuilder(table).append('|').append(rows).append('|').append(sha256)
                    .append('|').append(status).append('|');
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) b.append(',');
                b.append(columns[i]);
            }
            return b.toString();
        }
    }

    public final int userVersion;
    public final String databaseSha256;
    public final String databasePath;
    public final long capturedAt;
    public final LinkedHashMap<String, TableDigest> tables;
    public final String snapshotHash;

    private DatabaseBackupManifestR926(
            int userVersion,
            String databaseSha256,
            String databasePath,
            long capturedAt,
            LinkedHashMap<String, TableDigest> tables) {
        this.userVersion = userVersion;
        this.databaseSha256 = safe(databaseSha256);
        this.databasePath = safe(databasePath);
        this.capturedAt = capturedAt;
        this.tables = tables == null ? new LinkedHashMap<String, TableDigest>() : tables;
        StringBuilder b = new StringBuilder(FORMAT).append('|').append(userVersion).append('|').append(this.databaseSha256);
        for (TableDigest d : this.tables.values()) b.append('\n').append(d.canonical());
        this.snapshotHash = V51ChecksumEngine.sha256(b.toString());
    }

    public static DatabaseBackupManifestR926 capture(SQLiteDatabase db, File databaseFile) throws Exception {
        return capture(db, databaseFile, true);
    }

    static DatabaseBackupManifestR926 capture(SQLiteDatabase db, File databaseFile, boolean hashFile) throws Exception {
        if (db == null || !db.isOpen()) throw new IllegalArgumentException("R926_BACKUP_CAPTURE_DB_NOT_OPEN");
        LinkedHashMap<String, TableDigest> digests = new LinkedHashMap<String, TableDigest>();
        String[] tables = historicalTables();
        for (String table : tables) digests.put(table, digestTable(db, table, null));
        return new DatabaseBackupManifestR926(
                userVersion(db), hashFile && databaseFile != null && databaseFile.isFile() ? sha256File(databaseFile) : "",
                databaseFile == null ? "" : databaseFile.getAbsolutePath(), System.currentTimeMillis(), digests);
    }

    DatabaseBackupManifestR926 withDatabaseSha256(String sha256) {
        return new DatabaseBackupManifestR926(userVersion, sha256, databasePath, capturedAt, tables);
    }

    /** Re-hash target using the exact pre-migration column set. */
    public Verification verifyHistoricalRows(SQLiteDatabase db) throws Exception {
        ArrayList<String> failures = new ArrayList<String>();
        for (Map.Entry<String, TableDigest> e : tables.entrySet()) {
            TableDigest before = e.getValue();
            if ("MISSING".equals(before.status)) continue;
            TableDigest after = digestTable(db, before.table, before.columns);
            if (!"READY".equals(after.status)) {
                failures.add(before.table + ":AFTER_" + after.status);
                continue;
            }
            if (before.rows != after.rows) failures.add(before.table + ":ROWS:" + before.rows + "->" + after.rows);
            if (!before.sha256.equals(after.sha256)) failures.add(before.table + ":HASH_MISMATCH");
        }
        return new Verification(failures.isEmpty(), failures);
    }

    public File writeNextTo(File backup) throws Exception {
        if (backup == null) throw new IllegalArgumentException("R926_BACKUP_FILE_NULL");
        File out = new File(backup.getParentFile(), backup.getName() + ".manifest.txt");
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(out, false);
            byte[] bytes = text().getBytes(StandardCharsets.UTF_8);
            stream.write(bytes);
            stream.getFD().sync();
        } finally { if (stream != null) stream.close(); }
        return out;
    }

    public String text() {
        StringBuilder b = new StringBuilder(8192);
        b.append("FORMAT=").append(FORMAT).append('\n');
        b.append("USER_VERSION=").append(userVersion).append('\n');
        b.append("DATABASE_SHA256=").append(databaseSha256).append('\n');
        b.append("DATABASE_PATH=").append(databasePath).append('\n');
        b.append("CAPTURED_AT=").append(capturedAt).append('\n');
        b.append("SNAPSHOT_HASH=").append(snapshotHash).append('\n');
        for (TableDigest d : tables.values()) b.append("TABLE=").append(d.canonical()).append('\n');
        return b.toString();
    }


    /** R33.5: read the original producer's manifest. Never regenerate missing
     * pre-migration evidence from a current database and call it an old proof. */
    public static DatabaseBackupManifestR926 readManifestR335(File file) throws Exception {
        byte[] bytes=LegacyBackupRecoveryR335.readBounded(file,131072);
        String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        java.util.LinkedHashMap<String,String> headers=new java.util.LinkedHashMap<>();
        LinkedHashMap<String,TableDigest> records=new LinkedHashMap<>();
        java.util.List<String> expected=java.util.Arrays.asList(historicalTables());
        for(String line:text.split("\\n",-1)){
            if(line.isEmpty())continue;if(line.length()>32768||line.indexOf('\r')>=0)throw new java.io.IOException("R335_LEGACY_MANIFEST_LINE");
            int equals=line.indexOf('=');if(equals<1)throw new java.io.IOException("R335_LEGACY_MANIFEST_SYNTAX");
            String key=line.substring(0,equals),value=line.substring(equals+1);
            if("TABLE".equals(key)){
                String[] p=value.split("\\|",-1);if(p.length!=5||!expected.contains(p[0])||records.containsKey(p[0]))throw new java.io.IOException("R335_LEGACY_MANIFEST_TABLE");
                long rows=Long.parseLong(p[1]);String[] cols=p[4].isEmpty()?new String[0]:p[4].split(",",-1);
                if(rows<0||cols.length>256)throw new java.io.IOException("R335_LEGACY_MANIFEST_BOUND");
                java.util.HashSet<String> unique=new java.util.HashSet<>();for(String col:cols)if(!col.matches("[A-Za-z_][A-Za-z0-9_]{0,127}")||!unique.add(col))throw new java.io.IOException("R335_LEGACY_MANIFEST_COLUMN");
                if("MISSING".equals(p[3])){if(rows!=0||!p[2].isEmpty()||cols.length!=0)throw new java.io.IOException("R335_LEGACY_MANIFEST_MISSING_CONFLICT");}
                else if(!"READY".equals(p[3])||!p[2].matches("[0-9a-f]{64}"))throw new java.io.IOException("R335_LEGACY_MANIFEST_DIGEST");
                records.put(p[0],new TableDigest(p[0],cols,rows,p[2],p[3]));
            }else if(!java.util.Arrays.asList("FORMAT","USER_VERSION","DATABASE_SHA256","DATABASE_PATH","CAPTURED_AT","SNAPSHOT_HASH").contains(key)||headers.put(key,value)!=null)throw new java.io.IOException("R335_LEGACY_MANIFEST_HEADER");
        }
        if(headers.size()!=6||!FORMAT.equals(headers.get("FORMAT"))||!headers.get("DATABASE_SHA256").matches("[0-9a-f]{64}")
                ||!headers.get("SNAPSHOT_HASH").matches("[0-9a-f]{64}")||!new java.util.ArrayList<>(records.keySet()).equals(expected))throw new java.io.IOException("R335_LEGACY_MANIFEST_INCOMPLETE");
        int version=Integer.parseInt(headers.get("USER_VERSION"));long captured=Long.parseLong(headers.get("CAPTURED_AT"));
        if(version<0||captured<0)throw new java.io.IOException("R335_LEGACY_MANIFEST_TIME_VERSION");
        DatabaseBackupManifestR926 original=new DatabaseBackupManifestR926(version,headers.get("DATABASE_SHA256"),headers.get("DATABASE_PATH"),captured,records);
        if(!original.snapshotHash.equals(headers.get("SNAPSHOT_HASH")))throw new java.io.IOException("R335_LEGACY_MANIFEST_SNAPSHOT_HASH");return original;
    }

    public static final class Verification {
        public final boolean passed;
        public final List<String> failures;
        Verification(boolean passed, List<String> failures) {
            this.passed = passed;
            this.failures = failures == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<String>(failures));
        }
        public String summary() {
            if (passed) return "PASS";
            StringBuilder b = new StringBuilder("FAIL");
            for (String x : failures) b.append('|').append(x);
            return b.toString();
        }
    }

    private static TableDigest digestTable(SQLiteDatabase db, String table, String[] requestedColumns) throws Exception {
        if (!tableExists(db, table)) return new TableDigest(table, new String[0], 0L, "", "MISSING");
        String[] columns = requestedColumns == null ? columns(db, table) : requestedColumns.clone();
        if (columns.length == 0) return new TableDigest(table, columns, 0L, V51ChecksumEngine.sha256(table + "|EMPTY_COLUMNS"), "READY");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        update(digest, table);
        for (String column : columns) update(digest, column);
        Cursor c = null;
        long rows = 0L;
        try {
            StringBuilder sql = new StringBuilder("SELECT ");
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) sql.append(',');
                sql.append(quoteIdentifier(columns[i]));
            }
            sql.append(" FROM ").append(quoteIdentifier(table)).append(" ORDER BY rowid");
            c = db.rawQuery(sql.toString(), null);
            while (c.moveToNext()) {
                rows++;
                update(digest, "ROW");
                for (int i = 0; i < columns.length; i++) updateCursorValue(digest, c, i);
            }
        } finally { if (c != null) c.close(); }
        return new TableDigest(table, columns, rows, hex(digest.digest()), "READY");
    }

    private static String[] columns(SQLiteDatabase db, String table) {
        ArrayList<String> out = new ArrayList<String>();
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + quoteIdentifier(table) + ")", null);
            while (c.moveToNext()) out.add(safe(c.getString(1)));
        } finally { if (c != null) c.close(); }
        return out.toArray(new String[out.size()]);
    }

    private static void updateCursorValue(MessageDigest digest, Cursor c, int index) {
        int type;
        try { type = c.getType(index); } catch (Throwable ignored) { type = Cursor.FIELD_TYPE_STRING; }
        update(digest, String.valueOf(type));
        if (type == Cursor.FIELD_TYPE_NULL) { update(digest, "NULL"); return; }
        if (type == Cursor.FIELD_TYPE_BLOB) {
            byte[] blob = c.getBlob(index);
            digest.update((byte) 'B');
            if (blob != null) digest.update(blob);
            digest.update((byte) 0);
            return;
        }
        update(digest, c.getString(index));
    }

    private static String[] historicalTables() {
        return new String[] {
                DbHelper.TABLE_DRAWS,
                DbHelper.TABLE_DECISIONS,
                DbHelper.TABLE_TRADES,
                DbHelper.TABLE_R35_SHADOW_OBSERVATION,
                DbHelper.TABLE_R35_OOS_LABEL,
                V51Repository.ISSUE,
                V51Repository.SELECTED,
                V51Repository.AUTH
        };
    }

    private static boolean tableExists(SQLiteDatabase db, String table) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1", new String[] {table});
            return c.moveToFirst();
        } finally { if (c != null) c.close(); }
    }

    private static int userVersion(SQLiteDatabase db) {
        Cursor c = null;
        try { c = db.rawQuery("PRAGMA user_version", null); return c.moveToFirst() ? c.getInt(0) : -1; }
        finally { if (c != null) c.close(); }
    }

    public static String sha256File(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            byte[] buffer = new byte[32768];
            int count;
            while ((count = input.read(buffer)) >= 0) if (count > 0) digest.update(buffer, 0, count);
        } finally { if (input != null) input.close(); }
        return hex(digest.digest());
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = safe(value).getBytes(StandardCharsets.UTF_8);
        digest.update((byte) ((bytes.length >>> 24) & 0xff));
        digest.update((byte) ((bytes.length >>> 16) & 0xff));
        digest.update((byte) ((bytes.length >>> 8) & 0xff));
        digest.update((byte) (bytes.length & 0xff));
        digest.update(bytes);
    }
    private static String quoteIdentifier(String value) { return '"' + safe(value).replace("\"", "\"\"") + '"'; }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format(Locale.US, "%02x", b & 0xff));
        return out.toString();
    }
}
