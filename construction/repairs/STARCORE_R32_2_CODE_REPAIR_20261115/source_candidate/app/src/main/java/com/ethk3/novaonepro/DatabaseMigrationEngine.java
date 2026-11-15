package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** 星核天枢B03增量迁移与旧库开奖隔离引擎。旧预测、校准、交易和盈亏绝不导入。 */
public final class DatabaseMigrationEngine {
    public static final int TARGET_DATABASE_VERSION = 57;
    public static final int B03_STORAGE_GENERATION = 3;
    public static final String GAME_CODE = "Binance1-PK10";
    public static final String DATABASE_NAME = "binance_k3_nova_one_pro_v57.db";
    public static final String POLICY_DRAW_ONLY_QUARANTINE = "LEGACY_DRAW_ONLY_QUARANTINE";

    public static final class MigrationReport {
        public final boolean success;
        public final int fromVersion,
                toVersion,
                tablesCreatedOrVerified,
                columnsAdded,
                indexesCreatedOrVerified;
        public final String schemaChecksum;
        public final V50Enums.FailureCode failureCode;
        public final String detail;

        MigrationReport(
                boolean success,
                int fromVersion,
                int toVersion,
                int tables,
                int columns,
                int indexes,
                String checksum,
                V50Enums.FailureCode code,
                String detail) {
            this.success = success;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.tablesCreatedOrVerified = tables;
            this.columnsAdded = columns;
            this.indexesCreatedOrVerified = indexes;
            this.schemaChecksum = safe(checksum);
            this.failureCode = code == null ? V50Enums.FailureCode.UNKNOWN : code;
            this.detail = safe(detail);
        }
    }

    public static final class MigrationRule {
        public final String id, phase, policy;
        public final int order;
        public final boolean mandatory;

        MigrationRule(String id, String phase, String policy, int order, boolean mandatory) {
            this.id = id;
            this.phase = phase;
            this.policy = policy;
            this.order = order;
            this.mandatory = mandatory;
        }

        public String canonical() {
            return id + '|' + phase + '|' + policy + '|' + order + '|' + (mandatory ? 1 : 0);
        }
    }

    public static final class LegacyDrawMigrationReport {
        public final boolean success;
        public final int scanned, accepted, quarantined, duplicates, rejected;
        public final String sourceDatabase, sourceGameCode, cursorIssue, replayHash, detail;

        LegacyDrawMigrationReport(
                boolean ok,
                int scanned,
                int accepted,
                int quarantined,
                int duplicates,
                int rejected,
                String db,
                String game,
                String cursor,
                String hash,
                String detail) {
            success = ok;
            this.scanned = scanned;
            this.accepted = accepted;
            this.quarantined = quarantined;
            this.duplicates = duplicates;
            this.rejected = rejected;
            sourceDatabase = safe(db);
            sourceGameCode = safe(game);
            cursorIssue = safe(cursor);
            replayHash = safe(hash);
            this.detail = safe(detail);
        }

        public String canonical() {
            return success
                    + "|"
                    + scanned
                    + "|"
                    + accepted
                    + "|"
                    + quarantined
                    + "|"
                    + duplicates
                    + "|"
                    + rejected
                    + "|"
                    + sourceDatabase
                    + "|"
                    + sourceGameCode
                    + "|"
                    + cursorIssue
                    + "|"
                    + replayHash
                    + "|"
                    + detail;
        }
    }

    public static final class LegacySourcePolicy {
        public final String table, disposition, reason;
        public final boolean allowed;

        LegacySourcePolicy(String table, String disposition, String reason, boolean allowed) {
            this.table = table;
            this.disposition = disposition;
            this.reason = reason;
            this.allowed = allowed;
        }

        public String canonical() {
            return table + '|' + disposition + '|' + reason + '|' + (allowed ? 1 : 0);
        }
    }

    private DatabaseMigrationEngine() {}

    public static MigrationReport migrate(SQLiteDatabase db) {
        if (db == null)
            return failed(0, "B03_DATABASE_NULL", V50Enums.FailureCode.MIGRATION_FAILED);
        int from = safeUserVersion(db), tables = 0, columns = 0, indexes = 0;
        String checksum =
                V51ChecksumEngine.sha256(
                        SchemaContractRegistry.checksum()
                                + '|'
                                + DbHelper.b03SchemaHash()
                                + '|'
                                + migrationRuleHash());
        boolean owns = !db.inTransaction();
        try {
            if (owns) db.beginTransaction();
            List<SchemaContractRegistry.Table> contracts = SchemaContractRegistry.tables();
            for (int i = 0; i < contracts.size(); i++) {
                SchemaContractRegistry.Table table = contracts.get(i);
                db.execSQL(table.createSql);
                tables++;
                HashSet<String> actual = readColumns(db, table.name);
                for (int c = 0; c < table.columns.size(); c++) {
                    SchemaContractRegistry.Column col = table.columns.get(c);
                    if (actual.contains(col.name)) continue;
                    String upper = col.definition.toUpperCase(java.util.Locale.ROOT);
                    if (upper.indexOf("PRIMARY KEY") >= 0 || upper.indexOf("AUTOINCREMENT") >= 0)
                        throw new IllegalStateException(
                                "NON_ADDABLE_KEY_COLUMN:" + table.name + '.' + col.name);
                    db.execSQL(
                            "ALTER TABLE "
                                    + table.name
                                    + " ADD COLUMN "
                                    + col.name
                                    + ' '
                                    + col.definition);
                    columns++;
                }
                for (int x = 0; x < table.indexes.size(); x++) {
                    db.execSQL(table.indexes.get(x).sql);
                    indexes++;
                }
            }
            DbHelper.ensureTianshuB03(db);
            tables += DbHelper.b03TableNames().size();
            indexes += DbHelper.b03IndexNames().size();
            putMeta(db, "schema_id", "STARCORE_TIANSHU_B03");
            putMeta(db, "schema_generation", String.valueOf(TARGET_DATABASE_VERSION));
            putMeta(db, "storage_generation", String.valueOf(B03_STORAGE_GENERATION));
            putMeta(db, "database_name", DATABASE_NAME);
            putMeta(db, "game_code", GAME_CODE);
            putMeta(db, "schema_checksum", checksum);
            putMeta(db, "migration_policy", POLICY_DRAW_ONLY_QUARANTINE);
            putMeta(db, "migration_rule_hash", migrationRuleHash());
            putMeta(db, "model_proven", "false");
            putMeta(db, "shadow_only", "true");
            insertLog(
                    db,
                    from,
                    TARGET_DATABASE_VERSION,
                    checksum,
                    "SUCCESS",
                    "",
                    "B03_ADDITIVE_BINANCE_ISOLATED");
            if (owns) db.setTransactionSuccessful();
            return new MigrationReport(
                    true,
                    from,
                    TARGET_DATABASE_VERSION,
                    tables,
                    columns,
                    indexes,
                    checksum,
                    V50Enums.FailureCode.NONE,
                    "B03_MIGRATION_OK");
        } catch (Throwable e) {
            return new MigrationReport(
                    false,
                    from,
                    TARGET_DATABASE_VERSION,
                    tables,
                    columns,
                    indexes,
                    checksum,
                    V50Enums.FailureCode.MIGRATION_FAILED,
                    e.getClass().getSimpleName() + ':' + safe(e.getMessage()));
        } finally {
            if (owns && db.inTransaction())
                try {
                    db.endTransaction();
                } catch (Throwable ignore) {
                }
        }
    }

    public static LegacyDrawMigrationReport migrateLegacyDraws(
            SQLiteDatabase target,
            SQLiteDatabase legacy,
            String sourceDatabase,
            String sourceGameCode,
            int limit) {
        if (target == null || legacy == null) return legacyFail("DATABASE_NULL");
        DbHelper.ensureTianshuB03(target);
        int cap = Math.max(1, Math.min(5000, limit));
        int scanned = 0, accepted = 0, quarantined = 0, duplicates = 0, rejected = 0;
        String cursorIssue = "";
        StringBuilder replay = new StringBuilder();
        boolean owns = !target.inTransaction();
        Cursor c = null;
        try {
            if (owns) target.beginTransaction();
            String table = findLegacyDrawTable(legacy);
            if (table.length() == 0) throw new IllegalStateException("LEGACY_DRAW_TABLE_NOT_FOUND");
            HashSet<String> cols = readColumns(legacy, table);
            String issueCol =
                    firstColumn(
                            cols,
                            new String[] {"issue", "bet_no", "betNo", "draw_issue", "period"});
            String numbersCol =
                    firstColumn(
                            cols,
                            new String[] {
                                "numbers", "open_number", "openNumber", "number_text", "result"
                            });
            String timeCol =
                    firstColumn(
                            cols,
                            new String[] {
                                "draw_time", "open_time", "openTime", "gmt_bet_no", "created_at"
                            });
            if (issueCol.length() == 0 || numbersCol.length() == 0)
                throw new IllegalStateException("LEGACY_DRAW_COLUMNS_MISSING");
            c =
                    legacy.query(
                            table,
                            null,
                            null,
                            null,
                            null,
                            null,
                            issueCol + " ASC",
                            String.valueOf(cap));
            while (c.moveToNext()) {
                scanned++;
                String issue = cursorString(c, issueCol);
                String numbers = cursorString(c, numbersCol);
                long drawTime = timeCol.length() == 0 ? 0L : cursorLong(c, timeCol);
                cursorIssue = issue;
                String normalized = normalizePermutation(numbers);
                String reason =
                        normalized.length() == 0
                                ? "INVALID_PERMUTATION"
                                : "LEGACY_DRAW_REVIEW_REQUIRED";
                if (issue.length() == 0) {
                    rejected++;
                    continue;
                }
                String rowHash =
                        V51ChecksumEngine.sha256(
                                safe(sourceDatabase)
                                        + '|'
                                        + safe(sourceGameCode)
                                        + '|'
                                        + issue
                                        + '|'
                                        + numbers
                                        + '|'
                                        + drawTime);
                ContentValues cv = new ContentValues();
                cv.put("source_database", safe(sourceDatabase));
                cv.put("source_game_code", safe(sourceGameCode));
                cv.put("source_table", table);
                cv.put("source_issue", issue);
                cv.put("source_numbers", numbers);
                cv.put("normalized_numbers", normalized);
                cv.put("draw_time", drawTime);
                cv.put("quarantine_reason", reason);
                cv.put("review_status", "PENDING");
                cv.put("row_hash", rowHash);
                cv.put("created_at", System.currentTimeMillis());
                long row =
                        target.insertWithOnConflict(
                                DbHelper.TABLE_B03_DRAW_QUARANTINE,
                                null,
                                cv,
                                SQLiteDatabase.CONFLICT_IGNORE);
                if (row < 0) duplicates++;
                else {
                    quarantined++;
                    if (normalized.length() > 0) accepted++;
                }
                replay.append(rowHash);
            }
            String hash = V51ChecksumEngine.sha256(replay.toString());
            writeLegacyJournal(
                    target,
                    sourceDatabase,
                    sourceGameCode,
                    cursorIssue,
                    scanned,
                    quarantined,
                    hash,
                    "SUCCESS");
            if (owns) target.setTransactionSuccessful();
            return new LegacyDrawMigrationReport(
                    true,
                    scanned,
                    accepted,
                    quarantined,
                    duplicates,
                    rejected,
                    sourceDatabase,
                    sourceGameCode,
                    cursorIssue,
                    hash,
                    "DRAW_ONLY_QUARANTINE_OK");
        } catch (Throwable e) {
            String hash = V51ChecksumEngine.sha256(replay.toString());
            try {
                writeLegacyJournal(
                        target,
                        sourceDatabase,
                        sourceGameCode,
                        cursorIssue,
                        scanned,
                        quarantined,
                        hash,
                        "FAILED:" + safe(e.getMessage()));
            } catch (Throwable ignore) {
            }
            return new LegacyDrawMigrationReport(
                    false,
                    scanned,
                    accepted,
                    quarantined,
                    duplicates,
                    rejected,
                    sourceDatabase,
                    sourceGameCode,
                    cursorIssue,
                    hash,
                    "FAILED:" + safe(e.getMessage()));
        } finally {
            if (c != null) c.close();
            if (owns && target.inTransaction())
                try {
                    target.endTransaction();
                } catch (Throwable ignore) {
                }
        }
    }

    public static List<LegacySourcePolicy> sourcePolicies() {
        ArrayList<LegacySourcePolicy> x = new ArrayList<LegacySourcePolicy>();
        x.add(new LegacySourcePolicy("draws", "QUARANTINE", "DRAW_ONLY_REVIEW", true));
        x.add(new LegacySourcePolicy("draw_history", "QUARANTINE", "DRAW_ONLY_REVIEW", true));
        x.add(new LegacySourcePolicy("predictions", "REJECT", "NO_LEGACY_PREDICTION", false));
        x.add(new LegacySourcePolicy("decisions", "REJECT", "NO_LEGACY_DECISION", false));
        x.add(new LegacySourcePolicy("trades", "REJECT", "NO_LEGACY_TRADE", false));
        x.add(new LegacySourcePolicy("calibration", "REJECT", "NO_LEGACY_CALIBRATION", false));
        x.add(new LegacySourcePolicy("profit_loss", "REJECT", "NO_LEGACY_PNL", false));
        x.add(new LegacySourcePolicy("authorization", "REJECT", "NO_LEGACY_AUTHORIZATION", false));
        return Collections.unmodifiableList(x);
    }

    public static boolean mayImportTable(String table) {
        String t = safe(table).toLowerCase(java.util.Locale.ROOT);
        List<LegacySourcePolicy> x = sourcePolicies();
        for (int i = 0; i < x.size(); i++) if (x.get(i).table.equals(t)) return x.get(i).allowed;
        return false;
    }

    public static boolean containsForbiddenLegacyCategory(String table) {
        String t = safe(table).toLowerCase(java.util.Locale.ROOT);
        return t.indexOf("predict") >= 0
                || t.indexOf("decision") >= 0
                || t.indexOf("trade") >= 0
                || t.indexOf("calibr") >= 0
                || t.indexOf("profit") >= 0
                || t.indexOf("pnl") >= 0
                || t.indexOf("author") >= 0;
    }

    private static void writeLegacyJournal(
            SQLiteDatabase db,
            String sourceDb,
            String sourceGame,
            String cursor,
            int scanned,
            int imported,
            String hash,
            String status) {
        ContentValues cv = new ContentValues();
        cv.put("migration_id", V51ChecksumEngine.sha256(safe(sourceDb) + '|' + safe(sourceGame)));
        cv.put("source_database", safe(sourceDb));
        cv.put("source_game_code", safe(sourceGame));
        cv.put("policy", POLICY_DRAW_ONLY_QUARANTINE);
        cv.put("cursor_issue", safe(cursor));
        cv.put("scanned_count", scanned);
        cv.put("imported_count", imported);
        cv.put("replay_hash", safe(hash));
        cv.put("status", safe(status));
        cv.put("updated_at", System.currentTimeMillis());
        db.insertWithOnConflict(
                DbHelper.TABLE_B03_MIGRATION_JOURNAL, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static String findLegacyDrawTable(SQLiteDatabase db) {
        String[] names = {"draws", "draw_history", "lottery_draws", "history_draws"};
        for (int i = 0; i < names.length; i++)
            if (objectExists(db, "table", names[i])) return names[i];
        return "";
    }

    private static boolean objectExists(SQLiteDatabase db, String type, String name) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT 1 FROM sqlite_master WHERE type=? AND name=? LIMIT 1",
                            new String[] {type, name});
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    private static HashSet<String> readColumns(SQLiteDatabase db, String table) {
        HashSet<String> x = new HashSet<String>();
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            int n = c.getColumnIndex("name");
            while (c.moveToNext()) if (n >= 0) x.add(c.getString(n));
        } finally {
            if (c != null) c.close();
        }
        return x;
    }

    private static String firstColumn(HashSet<String> cols, String[] candidates) {
        for (int i = 0; i < candidates.length; i++)
            if (cols.contains(candidates[i])) return candidates[i];
        return "";
    }

    private static String cursorString(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        return i >= 0 && !c.isNull(i) ? safe(c.getString(i)) : "";
    }

    private static long cursorLong(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i < 0 || c.isNull(i)) return 0L;
        try {
            return c.getLong(i);
        } catch (Throwable e) {
            return 0L;
        }
    }

    private static String normalizePermutation(String text) {
        String s = safe(text).replace('-', ',').replace(' ', ',');
        String[] p = s.split(",");
        boolean[] seen = new boolean[11];
        StringBuilder b = new StringBuilder();
        int count = 0;
        for (int i = 0; i < p.length; i++) {
            String q = p[i].trim();
            if (q.length() == 0) continue;
            int n;
            try {
                n = Integer.parseInt(q);
            } catch (Throwable e) {
                return "";
            }
            if (n < 1 || n > 10 || seen[n]) return "";
            seen[n] = true;
            if (count > 0) b.append(',');
            b.append(n);
            count++;
        }
        return count == 10 ? b.toString() : "";
    }

    private static void putMeta(SQLiteDatabase db, String key, String value) {
        ContentValues cv = new ContentValues();
        cv.put("meta_key", key);
        cv.put("meta_value", safe(value));
        cv.put("updated_at", System.currentTimeMillis());
        long row =
                db.insertWithOnConflict(
                        SchemaContractRegistry.TABLE_SCHEMA_META,
                        null,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE);
        if (row < 0) throw new IllegalStateException("META_WRITE_FAILED:" + key);
    }

    private static void insertLog(
            SQLiteDatabase db,
            int from,
            int to,
            String checksum,
            String status,
            String code,
            String detail) {
        ContentValues cv = new ContentValues();
        cv.put("from_version", from);
        cv.put("to_version", to);
        cv.put("schema_checksum", safe(checksum));
        cv.put("status", safe(status));
        cv.put("error_code", safe(code));
        cv.put("error_detail", safe(detail));
        cv.put("created_at", System.currentTimeMillis());
        db.insert(SchemaContractRegistry.TABLE_MIGRATION_LOG, null, cv);
    }

    private static int safeUserVersion(SQLiteDatabase db) {
        try {
            return db.getVersion();
        } catch (Throwable e) {
            return 0;
        }
    }

    private static MigrationReport failed(int from, String detail, V50Enums.FailureCode code) {
        return new MigrationReport(
                false,
                from,
                TARGET_DATABASE_VERSION,
                0,
                0,
                0,
                DbHelper.b03SchemaHash(),
                code,
                detail);
    }

    private static LegacyDrawMigrationReport legacyFail(String detail) {
        return new LegacyDrawMigrationReport(false, 0, 0, 0, 0, 0, "", "", "", "", detail);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static List<MigrationRule> migrationRules() {
        ArrayList<MigrationRule> rules = new ArrayList<MigrationRule>();
        rules.add(new MigrationRule("B03-MIG-001", "IDENTITY", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-002", "SCHEMA", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-003", "INDEX", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-004", "TRIGGER", "NO_PNL", 4, true));
        rules.add(
                new MigrationRule("B03-MIG-005", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-006", "DRAW_QUARANTINE", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-007", "CHECKPOINT", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-008", "REPLAY", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-009", "CLEANUP", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-010", "VERIFY", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-011", "IDENTITY", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-012", "SCHEMA", "NO_PNL", 3, true));
        rules.add(new MigrationRule("B03-MIG-013", "INDEX", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-014", "TRIGGER", "NO_TRADE", 5, true));
        rules.add(
                new MigrationRule("B03-MIG-015", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-016", "DRAW_QUARANTINE", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-017", "CHECKPOINT", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-018", "REPLAY", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-019", "CLEANUP", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-020", "VERIFY", "NO_PNL", 2, true));
        rules.add(new MigrationRule("B03-MIG-021", "IDENTITY", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-022", "SCHEMA", "NO_TRADE", 4, true));
        rules.add(new MigrationRule("B03-MIG-023", "INDEX", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-024", "TRIGGER", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-025", "LEGACY_DISCOVERY", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-026", "DRAW_QUARANTINE", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-027", "CHECKPOINT", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-028", "REPLAY", "NO_PNL", 1, true));
        rules.add(new MigrationRule("B03-MIG-029", "CLEANUP", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-030", "VERIFY", "NO_TRADE", 3, true));
        rules.add(new MigrationRule("B03-MIG-031", "IDENTITY", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-032", "SCHEMA", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-033", "INDEX", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-034", "TRIGGER", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-035", "LEGACY_DISCOVERY", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-036", "DRAW_QUARANTINE", "NO_PNL", 9, true));
        rules.add(new MigrationRule("B03-MIG-037", "CHECKPOINT", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-038", "REPLAY", "NO_TRADE", 2, true));
        rules.add(new MigrationRule("B03-MIG-039", "CLEANUP", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-040", "VERIFY", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-041", "IDENTITY", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-042", "SCHEMA", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-043", "INDEX", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-044", "TRIGGER", "NO_PNL", 8, true));
        rules.add(
                new MigrationRule("B03-MIG-045", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-046", "DRAW_QUARANTINE", "NO_TRADE", 1, true));
        rules.add(new MigrationRule("B03-MIG-047", "CHECKPOINT", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-048", "REPLAY", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-049", "CLEANUP", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-050", "VERIFY", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-051", "IDENTITY", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-052", "SCHEMA", "NO_PNL", 7, true));
        rules.add(new MigrationRule("B03-MIG-053", "INDEX", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-054", "TRIGGER", "NO_TRADE", 9, true));
        rules.add(
                new MigrationRule("B03-MIG-055", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-056", "DRAW_QUARANTINE", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-057", "CHECKPOINT", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-058", "REPLAY", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-059", "CLEANUP", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-060", "VERIFY", "NO_PNL", 6, true));
        rules.add(new MigrationRule("B03-MIG-061", "IDENTITY", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-062", "SCHEMA", "NO_TRADE", 8, true));
        rules.add(new MigrationRule("B03-MIG-063", "INDEX", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-064", "TRIGGER", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-065", "LEGACY_DISCOVERY", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-066", "DRAW_QUARANTINE", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-067", "CHECKPOINT", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-068", "REPLAY", "NO_PNL", 5, true));
        rules.add(new MigrationRule("B03-MIG-069", "CLEANUP", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-070", "VERIFY", "NO_TRADE", 7, true));
        rules.add(new MigrationRule("B03-MIG-071", "IDENTITY", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-072", "SCHEMA", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-073", "INDEX", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-074", "TRIGGER", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-075", "LEGACY_DISCOVERY", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-076", "DRAW_QUARANTINE", "NO_PNL", 4, true));
        rules.add(new MigrationRule("B03-MIG-077", "CHECKPOINT", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-078", "REPLAY", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-079", "CLEANUP", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-080", "VERIFY", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-081", "IDENTITY", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-082", "SCHEMA", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-083", "INDEX", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-084", "TRIGGER", "NO_PNL", 3, true));
        rules.add(
                new MigrationRule("B03-MIG-085", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-086", "DRAW_QUARANTINE", "NO_TRADE", 5, true));
        rules.add(new MigrationRule("B03-MIG-087", "CHECKPOINT", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-088", "REPLAY", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-089", "CLEANUP", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-090", "VERIFY", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-091", "IDENTITY", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-092", "SCHEMA", "NO_PNL", 2, true));
        rules.add(new MigrationRule("B03-MIG-093", "INDEX", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-094", "TRIGGER", "NO_TRADE", 4, true));
        rules.add(
                new MigrationRule("B03-MIG-095", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-096", "DRAW_QUARANTINE", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-097", "CHECKPOINT", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-098", "REPLAY", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-099", "CLEANUP", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-100", "VERIFY", "NO_PNL", 1, true));
        rules.add(new MigrationRule("B03-MIG-101", "IDENTITY", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-102", "SCHEMA", "NO_TRADE", 3, true));
        rules.add(new MigrationRule("B03-MIG-103", "INDEX", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-104", "TRIGGER", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-105", "LEGACY_DISCOVERY", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-106", "DRAW_QUARANTINE", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-107", "CHECKPOINT", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-108", "REPLAY", "NO_PNL", 9, true));
        rules.add(new MigrationRule("B03-MIG-109", "CLEANUP", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-110", "VERIFY", "NO_TRADE", 2, true));
        rules.add(new MigrationRule("B03-MIG-111", "IDENTITY", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-112", "SCHEMA", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-113", "INDEX", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-114", "TRIGGER", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-115", "LEGACY_DISCOVERY", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-116", "DRAW_QUARANTINE", "NO_PNL", 8, true));
        rules.add(new MigrationRule("B03-MIG-117", "CHECKPOINT", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-118", "REPLAY", "NO_TRADE", 1, true));
        rules.add(new MigrationRule("B03-MIG-119", "CLEANUP", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-120", "VERIFY", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-121", "IDENTITY", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-122", "SCHEMA", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-123", "INDEX", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-124", "TRIGGER", "NO_PNL", 7, true));
        rules.add(
                new MigrationRule("B03-MIG-125", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-126", "DRAW_QUARANTINE", "NO_TRADE", 9, true));
        rules.add(new MigrationRule("B03-MIG-127", "CHECKPOINT", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-128", "REPLAY", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-129", "CLEANUP", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-130", "VERIFY", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-131", "IDENTITY", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-132", "SCHEMA", "NO_PNL", 6, true));
        rules.add(new MigrationRule("B03-MIG-133", "INDEX", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-134", "TRIGGER", "NO_TRADE", 8, true));
        rules.add(
                new MigrationRule("B03-MIG-135", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-136", "DRAW_QUARANTINE", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-137", "CHECKPOINT", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-138", "REPLAY", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-139", "CLEANUP", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-140", "VERIFY", "NO_PNL", 5, true));
        rules.add(new MigrationRule("B03-MIG-141", "IDENTITY", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-142", "SCHEMA", "NO_TRADE", 7, true));
        rules.add(new MigrationRule("B03-MIG-143", "INDEX", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-144", "TRIGGER", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-145", "LEGACY_DISCOVERY", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-146", "DRAW_QUARANTINE", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-147", "CHECKPOINT", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-148", "REPLAY", "NO_PNL", 4, true));
        rules.add(new MigrationRule("B03-MIG-149", "CLEANUP", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-150", "VERIFY", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-151", "IDENTITY", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-152", "SCHEMA", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-153", "INDEX", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-154", "TRIGGER", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-155", "LEGACY_DISCOVERY", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-156", "DRAW_QUARANTINE", "NO_PNL", 3, true));
        rules.add(new MigrationRule("B03-MIG-157", "CHECKPOINT", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-158", "REPLAY", "NO_TRADE", 5, true));
        rules.add(new MigrationRule("B03-MIG-159", "CLEANUP", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-160", "VERIFY", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-161", "IDENTITY", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-162", "SCHEMA", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-163", "INDEX", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-164", "TRIGGER", "NO_PNL", 2, true));
        rules.add(
                new MigrationRule("B03-MIG-165", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-166", "DRAW_QUARANTINE", "NO_TRADE", 4, true));
        rules.add(new MigrationRule("B03-MIG-167", "CHECKPOINT", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-168", "REPLAY", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-169", "CLEANUP", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-170", "VERIFY", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-171", "IDENTITY", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-172", "SCHEMA", "NO_PNL", 1, true));
        rules.add(new MigrationRule("B03-MIG-173", "INDEX", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-174", "TRIGGER", "NO_TRADE", 3, true));
        rules.add(
                new MigrationRule("B03-MIG-175", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-176", "DRAW_QUARANTINE", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-177", "CHECKPOINT", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-178", "REPLAY", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-179", "CLEANUP", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-180", "VERIFY", "NO_PNL", 9, true));
        rules.add(new MigrationRule("B03-MIG-181", "IDENTITY", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-182", "SCHEMA", "NO_TRADE", 2, true));
        rules.add(new MigrationRule("B03-MIG-183", "INDEX", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-184", "TRIGGER", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-185", "LEGACY_DISCOVERY", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-186", "DRAW_QUARANTINE", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-187", "CHECKPOINT", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-188", "REPLAY", "NO_PNL", 8, true));
        rules.add(new MigrationRule("B03-MIG-189", "CLEANUP", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-190", "VERIFY", "NO_TRADE", 1, true));
        rules.add(new MigrationRule("B03-MIG-191", "IDENTITY", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-192", "SCHEMA", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-193", "INDEX", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-194", "TRIGGER", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-195", "LEGACY_DISCOVERY", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-196", "DRAW_QUARANTINE", "NO_PNL", 7, true));
        rules.add(new MigrationRule("B03-MIG-197", "CHECKPOINT", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-198", "REPLAY", "NO_TRADE", 9, true));
        rules.add(new MigrationRule("B03-MIG-199", "CLEANUP", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-200", "VERIFY", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-201", "IDENTITY", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-202", "SCHEMA", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-203", "INDEX", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-204", "TRIGGER", "NO_PNL", 6, true));
        rules.add(
                new MigrationRule("B03-MIG-205", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-206", "DRAW_QUARANTINE", "NO_TRADE", 8, true));
        rules.add(new MigrationRule("B03-MIG-207", "CHECKPOINT", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-208", "REPLAY", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-209", "CLEANUP", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-210", "VERIFY", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-211", "IDENTITY", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-212", "SCHEMA", "NO_PNL", 5, true));
        rules.add(new MigrationRule("B03-MIG-213", "INDEX", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-214", "TRIGGER", "NO_TRADE", 7, true));
        rules.add(
                new MigrationRule("B03-MIG-215", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-216", "DRAW_QUARANTINE", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-217", "CHECKPOINT", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-218", "REPLAY", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-219", "CLEANUP", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-220", "VERIFY", "NO_PNL", 4, true));
        rules.add(new MigrationRule("B03-MIG-221", "IDENTITY", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-222", "SCHEMA", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-223", "INDEX", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-224", "TRIGGER", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-225", "LEGACY_DISCOVERY", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-226", "DRAW_QUARANTINE", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-227", "CHECKPOINT", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-228", "REPLAY", "NO_PNL", 3, true));
        rules.add(new MigrationRule("B03-MIG-229", "CLEANUP", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-230", "VERIFY", "NO_TRADE", 5, true));
        rules.add(new MigrationRule("B03-MIG-231", "IDENTITY", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-232", "SCHEMA", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-233", "INDEX", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-234", "TRIGGER", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-235", "LEGACY_DISCOVERY", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-236", "DRAW_QUARANTINE", "NO_PNL", 2, true));
        rules.add(new MigrationRule("B03-MIG-237", "CHECKPOINT", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-238", "REPLAY", "NO_TRADE", 4, true));
        rules.add(new MigrationRule("B03-MIG-239", "CLEANUP", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-240", "VERIFY", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-241", "IDENTITY", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-242", "SCHEMA", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-243", "INDEX", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-244", "TRIGGER", "NO_PNL", 1, true));
        rules.add(
                new MigrationRule("B03-MIG-245", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-246", "DRAW_QUARANTINE", "NO_TRADE", 3, true));
        rules.add(new MigrationRule("B03-MIG-247", "CHECKPOINT", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-248", "REPLAY", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-249", "CLEANUP", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-250", "VERIFY", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-251", "IDENTITY", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-252", "SCHEMA", "NO_PNL", 9, true));
        rules.add(new MigrationRule("B03-MIG-253", "INDEX", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-254", "TRIGGER", "NO_TRADE", 2, true));
        rules.add(
                new MigrationRule("B03-MIG-255", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-256", "DRAW_QUARANTINE", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-257", "CHECKPOINT", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-258", "REPLAY", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-259", "CLEANUP", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-260", "VERIFY", "NO_PNL", 8, true));
        rules.add(new MigrationRule("B03-MIG-261", "IDENTITY", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-262", "SCHEMA", "NO_TRADE", 1, true));
        rules.add(new MigrationRule("B03-MIG-263", "INDEX", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-264", "TRIGGER", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-265", "LEGACY_DISCOVERY", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-266", "DRAW_QUARANTINE", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-267", "CHECKPOINT", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-268", "REPLAY", "NO_PNL", 7, true));
        rules.add(new MigrationRule("B03-MIG-269", "CLEANUP", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-270", "VERIFY", "NO_TRADE", 9, true));
        rules.add(new MigrationRule("B03-MIG-271", "IDENTITY", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-272", "SCHEMA", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-273", "INDEX", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-274", "TRIGGER", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-275", "LEGACY_DISCOVERY", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-276", "DRAW_QUARANTINE", "NO_PNL", 6, true));
        rules.add(new MigrationRule("B03-MIG-277", "CHECKPOINT", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-278", "REPLAY", "NO_TRADE", 8, true));
        rules.add(new MigrationRule("B03-MIG-279", "CLEANUP", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-280", "VERIFY", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-281", "IDENTITY", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-282", "SCHEMA", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-283", "INDEX", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-284", "TRIGGER", "NO_PNL", 5, true));
        rules.add(
                new MigrationRule("B03-MIG-285", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-286", "DRAW_QUARANTINE", "NO_TRADE", 7, true));
        rules.add(new MigrationRule("B03-MIG-287", "CHECKPOINT", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-288", "REPLAY", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-289", "CLEANUP", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-290", "VERIFY", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-291", "IDENTITY", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-292", "SCHEMA", "NO_PNL", 4, true));
        rules.add(new MigrationRule("B03-MIG-293", "INDEX", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-294", "TRIGGER", "NO_TRADE", 6, true));
        rules.add(
                new MigrationRule("B03-MIG-295", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-296", "DRAW_QUARANTINE", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-297", "CHECKPOINT", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-298", "REPLAY", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-299", "CLEANUP", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-300", "VERIFY", "NO_PNL", 3, true));
        rules.add(new MigrationRule("B03-MIG-301", "IDENTITY", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-302", "SCHEMA", "NO_TRADE", 5, true));
        rules.add(new MigrationRule("B03-MIG-303", "INDEX", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-304", "TRIGGER", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-305", "LEGACY_DISCOVERY", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-306", "DRAW_QUARANTINE", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-307", "CHECKPOINT", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-308", "REPLAY", "NO_PNL", 2, true));
        rules.add(new MigrationRule("B03-MIG-309", "CLEANUP", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-310", "VERIFY", "NO_TRADE", 4, true));
        rules.add(new MigrationRule("B03-MIG-311", "IDENTITY", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-312", "SCHEMA", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-313", "INDEX", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-314", "TRIGGER", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-315", "LEGACY_DISCOVERY", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-316", "DRAW_QUARANTINE", "NO_PNL", 1, true));
        rules.add(new MigrationRule("B03-MIG-317", "CHECKPOINT", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-318", "REPLAY", "NO_TRADE", 3, true));
        rules.add(new MigrationRule("B03-MIG-319", "CLEANUP", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-320", "VERIFY", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-321", "IDENTITY", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-322", "SCHEMA", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-323", "INDEX", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-324", "TRIGGER", "NO_PNL", 9, true));
        rules.add(
                new MigrationRule("B03-MIG-325", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-326", "DRAW_QUARANTINE", "NO_TRADE", 2, true));
        rules.add(new MigrationRule("B03-MIG-327", "CHECKPOINT", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-328", "REPLAY", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-329", "CLEANUP", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-330", "VERIFY", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-331", "IDENTITY", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-332", "SCHEMA", "NO_PNL", 8, true));
        rules.add(new MigrationRule("B03-MIG-333", "INDEX", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-334", "TRIGGER", "NO_TRADE", 1, true));
        rules.add(
                new MigrationRule("B03-MIG-335", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-336", "DRAW_QUARANTINE", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-337", "CHECKPOINT", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-338", "REPLAY", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-339", "CLEANUP", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-340", "VERIFY", "NO_PNL", 7, true));
        rules.add(new MigrationRule("B03-MIG-341", "IDENTITY", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-342", "SCHEMA", "NO_TRADE", 9, true));
        rules.add(new MigrationRule("B03-MIG-343", "INDEX", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-344", "TRIGGER", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-345", "LEGACY_DISCOVERY", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-346", "DRAW_QUARANTINE", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-347", "CHECKPOINT", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-348", "REPLAY", "NO_PNL", 6, true));
        rules.add(new MigrationRule("B03-MIG-349", "CLEANUP", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-350", "VERIFY", "NO_TRADE", 8, true));
        rules.add(new MigrationRule("B03-MIG-351", "IDENTITY", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-352", "SCHEMA", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-353", "INDEX", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-354", "TRIGGER", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-355", "LEGACY_DISCOVERY", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-356", "DRAW_QUARANTINE", "NO_PNL", 5, true));
        rules.add(new MigrationRule("B03-MIG-357", "CHECKPOINT", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-358", "REPLAY", "NO_TRADE", 7, true));
        rules.add(new MigrationRule("B03-MIG-359", "CLEANUP", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-360", "VERIFY", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-361", "IDENTITY", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-362", "SCHEMA", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-363", "INDEX", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-364", "TRIGGER", "NO_PNL", 4, true));
        rules.add(
                new MigrationRule("B03-MIG-365", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-366", "DRAW_QUARANTINE", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-367", "CHECKPOINT", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-368", "REPLAY", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-369", "CLEANUP", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-370", "VERIFY", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-371", "IDENTITY", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-372", "SCHEMA", "NO_PNL", 3, true));
        rules.add(new MigrationRule("B03-MIG-373", "INDEX", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-374", "TRIGGER", "NO_TRADE", 5, true));
        rules.add(
                new MigrationRule("B03-MIG-375", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-376", "DRAW_QUARANTINE", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-377", "CHECKPOINT", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-378", "REPLAY", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-379", "CLEANUP", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-380", "VERIFY", "NO_PNL", 2, true));
        rules.add(new MigrationRule("B03-MIG-381", "IDENTITY", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-382", "SCHEMA", "NO_TRADE", 4, true));
        rules.add(new MigrationRule("B03-MIG-383", "INDEX", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-384", "TRIGGER", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-385", "LEGACY_DISCOVERY", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-386", "DRAW_QUARANTINE", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-387", "CHECKPOINT", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-388", "REPLAY", "NO_PNL", 1, true));
        rules.add(new MigrationRule("B03-MIG-389", "CLEANUP", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-390", "VERIFY", "NO_TRADE", 3, true));
        rules.add(new MigrationRule("B03-MIG-391", "IDENTITY", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-392", "SCHEMA", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-393", "INDEX", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-394", "TRIGGER", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-395", "LEGACY_DISCOVERY", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-396", "DRAW_QUARANTINE", "NO_PNL", 9, true));
        rules.add(new MigrationRule("B03-MIG-397", "CHECKPOINT", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-398", "REPLAY", "NO_TRADE", 2, true));
        rules.add(new MigrationRule("B03-MIG-399", "CLEANUP", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-400", "VERIFY", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-401", "IDENTITY", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-402", "SCHEMA", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-403", "INDEX", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-404", "TRIGGER", "NO_PNL", 8, true));
        rules.add(
                new MigrationRule("B03-MIG-405", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-406", "DRAW_QUARANTINE", "NO_TRADE", 1, true));
        rules.add(new MigrationRule("B03-MIG-407", "CHECKPOINT", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-408", "REPLAY", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-409", "CLEANUP", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-410", "VERIFY", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-411", "IDENTITY", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-412", "SCHEMA", "NO_PNL", 7, true));
        rules.add(new MigrationRule("B03-MIG-413", "INDEX", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-414", "TRIGGER", "NO_TRADE", 9, true));
        rules.add(
                new MigrationRule("B03-MIG-415", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-416", "DRAW_QUARANTINE", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-417", "CHECKPOINT", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-418", "REPLAY", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-419", "CLEANUP", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-420", "VERIFY", "NO_PNL", 6, true));
        rules.add(new MigrationRule("B03-MIG-421", "IDENTITY", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-422", "SCHEMA", "NO_TRADE", 8, true));
        rules.add(new MigrationRule("B03-MIG-423", "INDEX", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-424", "TRIGGER", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-425", "LEGACY_DISCOVERY", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-426", "DRAW_QUARANTINE", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-427", "CHECKPOINT", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-428", "REPLAY", "NO_PNL", 5, true));
        rules.add(new MigrationRule("B03-MIG-429", "CLEANUP", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-430", "VERIFY", "NO_TRADE", 7, true));
        rules.add(new MigrationRule("B03-MIG-431", "IDENTITY", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-432", "SCHEMA", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-433", "INDEX", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-434", "TRIGGER", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-435", "LEGACY_DISCOVERY", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-436", "DRAW_QUARANTINE", "NO_PNL", 4, true));
        rules.add(new MigrationRule("B03-MIG-437", "CHECKPOINT", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-438", "REPLAY", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-439", "CLEANUP", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-440", "VERIFY", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-441", "IDENTITY", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-442", "SCHEMA", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-443", "INDEX", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-444", "TRIGGER", "NO_PNL", 3, true));
        rules.add(
                new MigrationRule("B03-MIG-445", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-446", "DRAW_QUARANTINE", "NO_TRADE", 5, true));
        rules.add(new MigrationRule("B03-MIG-447", "CHECKPOINT", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-448", "REPLAY", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-449", "CLEANUP", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-450", "VERIFY", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-451", "IDENTITY", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-452", "SCHEMA", "NO_PNL", 2, true));
        rules.add(new MigrationRule("B03-MIG-453", "INDEX", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-454", "TRIGGER", "NO_TRADE", 4, true));
        rules.add(
                new MigrationRule("B03-MIG-455", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-456", "DRAW_QUARANTINE", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-457", "CHECKPOINT", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-458", "REPLAY", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-459", "CLEANUP", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-460", "VERIFY", "NO_PNL", 1, true));
        rules.add(new MigrationRule("B03-MIG-461", "IDENTITY", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-462", "SCHEMA", "NO_TRADE", 3, true));
        rules.add(new MigrationRule("B03-MIG-463", "INDEX", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-464", "TRIGGER", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-465", "LEGACY_DISCOVERY", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-466", "DRAW_QUARANTINE", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-467", "CHECKPOINT", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-468", "REPLAY", "NO_PNL", 9, true));
        rules.add(new MigrationRule("B03-MIG-469", "CLEANUP", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-470", "VERIFY", "NO_TRADE", 2, true));
        rules.add(new MigrationRule("B03-MIG-471", "IDENTITY", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-472", "SCHEMA", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-473", "INDEX", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-474", "TRIGGER", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-475", "LEGACY_DISCOVERY", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-476", "DRAW_QUARANTINE", "NO_PNL", 8, true));
        rules.add(new MigrationRule("B03-MIG-477", "CHECKPOINT", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-478", "REPLAY", "NO_TRADE", 1, true));
        rules.add(new MigrationRule("B03-MIG-479", "CLEANUP", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-480", "VERIFY", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-481", "IDENTITY", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-482", "SCHEMA", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-483", "INDEX", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-484", "TRIGGER", "NO_PNL", 7, true));
        rules.add(
                new MigrationRule("B03-MIG-485", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-486", "DRAW_QUARANTINE", "NO_TRADE", 9, true));
        rules.add(new MigrationRule("B03-MIG-487", "CHECKPOINT", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-488", "REPLAY", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-489", "CLEANUP", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-490", "VERIFY", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-491", "IDENTITY", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-492", "SCHEMA", "NO_PNL", 6, true));
        rules.add(new MigrationRule("B03-MIG-493", "INDEX", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-494", "TRIGGER", "NO_TRADE", 8, true));
        rules.add(
                new MigrationRule("B03-MIG-495", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-496", "DRAW_QUARANTINE", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-497", "CHECKPOINT", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-498", "REPLAY", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-499", "CLEANUP", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-500", "VERIFY", "NO_PNL", 5, true));
        rules.add(new MigrationRule("B03-MIG-501", "IDENTITY", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-502", "SCHEMA", "NO_TRADE", 7, true));
        rules.add(new MigrationRule("B03-MIG-503", "INDEX", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-504", "TRIGGER", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-505", "LEGACY_DISCOVERY", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-506", "DRAW_QUARANTINE", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-507", "CHECKPOINT", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-508", "REPLAY", "NO_PNL", 4, true));
        rules.add(new MigrationRule("B03-MIG-509", "CLEANUP", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-510", "VERIFY", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-511", "IDENTITY", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-512", "SCHEMA", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-513", "INDEX", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-514", "TRIGGER", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-515", "LEGACY_DISCOVERY", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-516", "DRAW_QUARANTINE", "NO_PNL", 3, true));
        rules.add(new MigrationRule("B03-MIG-517", "CHECKPOINT", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-518", "REPLAY", "NO_TRADE", 5, true));
        rules.add(new MigrationRule("B03-MIG-519", "CLEANUP", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-520", "VERIFY", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-521", "IDENTITY", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-522", "SCHEMA", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-523", "INDEX", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-524", "TRIGGER", "NO_PNL", 2, true));
        rules.add(
                new MigrationRule("B03-MIG-525", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-526", "DRAW_QUARANTINE", "NO_TRADE", 4, true));
        rules.add(new MigrationRule("B03-MIG-527", "CHECKPOINT", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-528", "REPLAY", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-529", "CLEANUP", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-530", "VERIFY", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-531", "IDENTITY", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-532", "SCHEMA", "NO_PNL", 1, true));
        rules.add(new MigrationRule("B03-MIG-533", "INDEX", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-534", "TRIGGER", "NO_TRADE", 3, true));
        rules.add(
                new MigrationRule("B03-MIG-535", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-536", "DRAW_QUARANTINE", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-537", "CHECKPOINT", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-538", "REPLAY", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-539", "CLEANUP", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-540", "VERIFY", "NO_PNL", 9, true));
        rules.add(new MigrationRule("B03-MIG-541", "IDENTITY", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-542", "SCHEMA", "NO_TRADE", 2, true));
        rules.add(new MigrationRule("B03-MIG-543", "INDEX", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-544", "TRIGGER", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-545", "LEGACY_DISCOVERY", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-546", "DRAW_QUARANTINE", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-547", "CHECKPOINT", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-548", "REPLAY", "NO_PNL", 8, true));
        rules.add(new MigrationRule("B03-MIG-549", "CLEANUP", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-550", "VERIFY", "NO_TRADE", 1, true));
        rules.add(new MigrationRule("B03-MIG-551", "IDENTITY", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-552", "SCHEMA", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-553", "INDEX", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-554", "TRIGGER", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-555", "LEGACY_DISCOVERY", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-556", "DRAW_QUARANTINE", "NO_PNL", 7, true));
        rules.add(new MigrationRule("B03-MIG-557", "CHECKPOINT", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-558", "REPLAY", "NO_TRADE", 9, true));
        rules.add(new MigrationRule("B03-MIG-559", "CLEANUP", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-560", "VERIFY", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-561", "IDENTITY", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-562", "SCHEMA", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-563", "INDEX", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-564", "TRIGGER", "NO_PNL", 6, true));
        rules.add(
                new MigrationRule("B03-MIG-565", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-566", "DRAW_QUARANTINE", "NO_TRADE", 8, true));
        rules.add(new MigrationRule("B03-MIG-567", "CHECKPOINT", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-568", "REPLAY", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-569", "CLEANUP", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-570", "VERIFY", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-571", "IDENTITY", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-572", "SCHEMA", "NO_PNL", 5, true));
        rules.add(new MigrationRule("B03-MIG-573", "INDEX", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-574", "TRIGGER", "NO_TRADE", 7, true));
        rules.add(
                new MigrationRule("B03-MIG-575", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-576", "DRAW_QUARANTINE", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-577", "CHECKPOINT", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-578", "REPLAY", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-579", "CLEANUP", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-580", "VERIFY", "NO_PNL", 4, true));
        rules.add(new MigrationRule("B03-MIG-581", "IDENTITY", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-582", "SCHEMA", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-583", "INDEX", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-584", "TRIGGER", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-585", "LEGACY_DISCOVERY", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-586", "DRAW_QUARANTINE", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-587", "CHECKPOINT", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-588", "REPLAY", "NO_PNL", 3, true));
        rules.add(new MigrationRule("B03-MIG-589", "CLEANUP", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-590", "VERIFY", "NO_TRADE", 5, true));
        rules.add(new MigrationRule("B03-MIG-591", "IDENTITY", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-592", "SCHEMA", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-593", "INDEX", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-594", "TRIGGER", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-595", "LEGACY_DISCOVERY", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-596", "DRAW_QUARANTINE", "NO_PNL", 2, true));
        rules.add(new MigrationRule("B03-MIG-597", "CHECKPOINT", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-598", "REPLAY", "NO_TRADE", 4, true));
        rules.add(new MigrationRule("B03-MIG-599", "CLEANUP", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-600", "VERIFY", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-601", "IDENTITY", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-602", "SCHEMA", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-603", "INDEX", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-604", "TRIGGER", "NO_PNL", 1, true));
        rules.add(
                new MigrationRule("B03-MIG-605", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-606", "DRAW_QUARANTINE", "NO_TRADE", 3, true));
        rules.add(new MigrationRule("B03-MIG-607", "CHECKPOINT", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-608", "REPLAY", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-609", "CLEANUP", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-610", "VERIFY", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-611", "IDENTITY", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-612", "SCHEMA", "NO_PNL", 9, true));
        rules.add(new MigrationRule("B03-MIG-613", "INDEX", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-614", "TRIGGER", "NO_TRADE", 2, true));
        rules.add(
                new MigrationRule("B03-MIG-615", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-616", "DRAW_QUARANTINE", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-617", "CHECKPOINT", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-618", "REPLAY", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-619", "CLEANUP", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-620", "VERIFY", "NO_PNL", 8, true));
        rules.add(new MigrationRule("B03-MIG-621", "IDENTITY", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-622", "SCHEMA", "NO_TRADE", 1, true));
        rules.add(new MigrationRule("B03-MIG-623", "INDEX", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-624", "TRIGGER", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-625", "LEGACY_DISCOVERY", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-626", "DRAW_QUARANTINE", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-627", "CHECKPOINT", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-628", "REPLAY", "NO_PNL", 7, true));
        rules.add(new MigrationRule("B03-MIG-629", "CLEANUP", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-630", "VERIFY", "NO_TRADE", 9, true));
        rules.add(new MigrationRule("B03-MIG-631", "IDENTITY", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-632", "SCHEMA", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-633", "INDEX", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-634", "TRIGGER", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-635", "LEGACY_DISCOVERY", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-636", "DRAW_QUARANTINE", "NO_PNL", 6, true));
        rules.add(new MigrationRule("B03-MIG-637", "CHECKPOINT", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-638", "REPLAY", "NO_TRADE", 8, true));
        rules.add(new MigrationRule("B03-MIG-639", "CLEANUP", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-640", "VERIFY", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-641", "IDENTITY", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-642", "SCHEMA", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-643", "INDEX", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-644", "TRIGGER", "NO_PNL", 5, true));
        rules.add(
                new MigrationRule("B03-MIG-645", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-646", "DRAW_QUARANTINE", "NO_TRADE", 7, true));
        rules.add(new MigrationRule("B03-MIG-647", "CHECKPOINT", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-648", "REPLAY", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-649", "CLEANUP", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-650", "VERIFY", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-651", "IDENTITY", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-652", "SCHEMA", "NO_PNL", 4, true));
        rules.add(new MigrationRule("B03-MIG-653", "INDEX", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-654", "TRIGGER", "NO_TRADE", 6, true));
        rules.add(
                new MigrationRule("B03-MIG-655", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-656", "DRAW_QUARANTINE", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-657", "CHECKPOINT", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-658", "REPLAY", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-659", "CLEANUP", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-660", "VERIFY", "NO_PNL", 3, true));
        rules.add(new MigrationRule("B03-MIG-661", "IDENTITY", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-662", "SCHEMA", "NO_TRADE", 5, true));
        rules.add(new MigrationRule("B03-MIG-663", "INDEX", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-664", "TRIGGER", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-665", "LEGACY_DISCOVERY", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-666", "DRAW_QUARANTINE", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-667", "CHECKPOINT", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-668", "REPLAY", "NO_PNL", 2, true));
        rules.add(new MigrationRule("B03-MIG-669", "CLEANUP", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-670", "VERIFY", "NO_TRADE", 4, true));
        rules.add(new MigrationRule("B03-MIG-671", "IDENTITY", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-672", "SCHEMA", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-673", "INDEX", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-674", "TRIGGER", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-675", "LEGACY_DISCOVERY", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-676", "DRAW_QUARANTINE", "NO_PNL", 1, true));
        rules.add(new MigrationRule("B03-MIG-677", "CHECKPOINT", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-678", "REPLAY", "NO_TRADE", 3, true));
        rules.add(new MigrationRule("B03-MIG-679", "CLEANUP", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-680", "VERIFY", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-681", "IDENTITY", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-682", "SCHEMA", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-683", "INDEX", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-684", "TRIGGER", "NO_PNL", 9, true));
        rules.add(
                new MigrationRule("B03-MIG-685", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-686", "DRAW_QUARANTINE", "NO_TRADE", 2, true));
        rules.add(new MigrationRule("B03-MIG-687", "CHECKPOINT", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-688", "REPLAY", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-689", "CLEANUP", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-690", "VERIFY", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-691", "IDENTITY", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-692", "SCHEMA", "NO_PNL", 8, true));
        rules.add(new MigrationRule("B03-MIG-693", "INDEX", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-694", "TRIGGER", "NO_TRADE", 1, true));
        rules.add(
                new MigrationRule("B03-MIG-695", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-696", "DRAW_QUARANTINE", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-697", "CHECKPOINT", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-698", "REPLAY", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-699", "CLEANUP", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-700", "VERIFY", "NO_PNL", 7, true));
        rules.add(new MigrationRule("B03-MIG-701", "IDENTITY", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-702", "SCHEMA", "NO_TRADE", 9, true));
        rules.add(new MigrationRule("B03-MIG-703", "INDEX", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-704", "TRIGGER", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-705", "LEGACY_DISCOVERY", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-706", "DRAW_QUARANTINE", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-707", "CHECKPOINT", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-708", "REPLAY", "NO_PNL", 6, true));
        rules.add(new MigrationRule("B03-MIG-709", "CLEANUP", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-710", "VERIFY", "NO_TRADE", 8, true));
        rules.add(new MigrationRule("B03-MIG-711", "IDENTITY", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-712", "SCHEMA", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-713", "INDEX", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-714", "TRIGGER", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-715", "LEGACY_DISCOVERY", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-716", "DRAW_QUARANTINE", "NO_PNL", 5, true));
        rules.add(new MigrationRule("B03-MIG-717", "CHECKPOINT", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-718", "REPLAY", "NO_TRADE", 7, true));
        rules.add(new MigrationRule("B03-MIG-719", "CLEANUP", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-720", "VERIFY", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-721", "IDENTITY", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-722", "SCHEMA", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-723", "INDEX", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-724", "TRIGGER", "NO_PNL", 4, true));
        rules.add(
                new MigrationRule("B03-MIG-725", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-726", "DRAW_QUARANTINE", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-727", "CHECKPOINT", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-728", "REPLAY", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-729", "CLEANUP", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-730", "VERIFY", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-731", "IDENTITY", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-732", "SCHEMA", "NO_PNL", 3, true));
        rules.add(new MigrationRule("B03-MIG-733", "INDEX", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-734", "TRIGGER", "NO_TRADE", 5, true));
        rules.add(
                new MigrationRule("B03-MIG-735", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-736", "DRAW_QUARANTINE", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-737", "CHECKPOINT", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-738", "REPLAY", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-739", "CLEANUP", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-740", "VERIFY", "NO_PNL", 2, true));
        rules.add(new MigrationRule("B03-MIG-741", "IDENTITY", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-742", "SCHEMA", "NO_TRADE", 4, true));
        rules.add(new MigrationRule("B03-MIG-743", "INDEX", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-744", "TRIGGER", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-745", "LEGACY_DISCOVERY", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-746", "DRAW_QUARANTINE", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-747", "CHECKPOINT", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-748", "REPLAY", "NO_PNL", 1, true));
        rules.add(new MigrationRule("B03-MIG-749", "CLEANUP", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-750", "VERIFY", "NO_TRADE", 3, true));
        rules.add(new MigrationRule("B03-MIG-751", "IDENTITY", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-752", "SCHEMA", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-753", "INDEX", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-754", "TRIGGER", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-755", "LEGACY_DISCOVERY", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-756", "DRAW_QUARANTINE", "NO_PNL", 9, true));
        rules.add(new MigrationRule("B03-MIG-757", "CHECKPOINT", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-758", "REPLAY", "NO_TRADE", 2, true));
        rules.add(new MigrationRule("B03-MIG-759", "CLEANUP", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-760", "VERIFY", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-761", "IDENTITY", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-762", "SCHEMA", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-763", "INDEX", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-764", "TRIGGER", "NO_PNL", 8, true));
        rules.add(
                new MigrationRule("B03-MIG-765", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-766", "DRAW_QUARANTINE", "NO_TRADE", 1, true));
        rules.add(new MigrationRule("B03-MIG-767", "CHECKPOINT", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-768", "REPLAY", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-769", "CLEANUP", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-770", "VERIFY", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-771", "IDENTITY", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-772", "SCHEMA", "NO_PNL", 7, true));
        rules.add(new MigrationRule("B03-MIG-773", "INDEX", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-774", "TRIGGER", "NO_TRADE", 9, true));
        rules.add(
                new MigrationRule("B03-MIG-775", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-776", "DRAW_QUARANTINE", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-777", "CHECKPOINT", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-778", "REPLAY", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-779", "CLEANUP", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-780", "VERIFY", "NO_PNL", 6, true));
        rules.add(new MigrationRule("B03-MIG-781", "IDENTITY", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-782", "SCHEMA", "NO_TRADE", 8, true));
        rules.add(new MigrationRule("B03-MIG-783", "INDEX", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-784", "TRIGGER", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-785", "LEGACY_DISCOVERY", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-786", "DRAW_QUARANTINE", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-787", "CHECKPOINT", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-788", "REPLAY", "NO_PNL", 5, true));
        rules.add(new MigrationRule("B03-MIG-789", "CLEANUP", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-790", "VERIFY", "NO_TRADE", 7, true));
        rules.add(new MigrationRule("B03-MIG-791", "IDENTITY", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-792", "SCHEMA", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-793", "INDEX", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-794", "TRIGGER", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-795", "LEGACY_DISCOVERY", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-796", "DRAW_QUARANTINE", "NO_PNL", 4, true));
        rules.add(new MigrationRule("B03-MIG-797", "CHECKPOINT", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-798", "REPLAY", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-799", "CLEANUP", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-800", "VERIFY", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-801", "IDENTITY", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-802", "SCHEMA", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-803", "INDEX", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-804", "TRIGGER", "NO_PNL", 3, true));
        rules.add(
                new MigrationRule("B03-MIG-805", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-806", "DRAW_QUARANTINE", "NO_TRADE", 5, true));
        rules.add(new MigrationRule("B03-MIG-807", "CHECKPOINT", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-808", "REPLAY", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-809", "CLEANUP", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-810", "VERIFY", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-811", "IDENTITY", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-812", "SCHEMA", "NO_PNL", 2, true));
        rules.add(new MigrationRule("B03-MIG-813", "INDEX", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-814", "TRIGGER", "NO_TRADE", 4, true));
        rules.add(
                new MigrationRule("B03-MIG-815", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-816", "DRAW_QUARANTINE", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-817", "CHECKPOINT", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-818", "REPLAY", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-819", "CLEANUP", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-820", "VERIFY", "NO_PNL", 1, true));
        rules.add(new MigrationRule("B03-MIG-821", "IDENTITY", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-822", "SCHEMA", "NO_TRADE", 3, true));
        rules.add(new MigrationRule("B03-MIG-823", "INDEX", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-824", "TRIGGER", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-825", "LEGACY_DISCOVERY", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-826", "DRAW_QUARANTINE", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-827", "CHECKPOINT", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-828", "REPLAY", "NO_PNL", 9, true));
        rules.add(new MigrationRule("B03-MIG-829", "CLEANUP", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-830", "VERIFY", "NO_TRADE", 2, true));
        rules.add(new MigrationRule("B03-MIG-831", "IDENTITY", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-832", "SCHEMA", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-833", "INDEX", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-834", "TRIGGER", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-835", "LEGACY_DISCOVERY", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-836", "DRAW_QUARANTINE", "NO_PNL", 8, true));
        rules.add(new MigrationRule("B03-MIG-837", "CHECKPOINT", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-838", "REPLAY", "NO_TRADE", 1, true));
        rules.add(new MigrationRule("B03-MIG-839", "CLEANUP", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-840", "VERIFY", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-841", "IDENTITY", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-842", "SCHEMA", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-843", "INDEX", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-844", "TRIGGER", "NO_PNL", 7, true));
        rules.add(
                new MigrationRule("B03-MIG-845", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-846", "DRAW_QUARANTINE", "NO_TRADE", 9, true));
        rules.add(new MigrationRule("B03-MIG-847", "CHECKPOINT", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-848", "REPLAY", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-849", "CLEANUP", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-850", "VERIFY", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-851", "IDENTITY", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-852", "SCHEMA", "NO_PNL", 6, true));
        rules.add(new MigrationRule("B03-MIG-853", "INDEX", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-854", "TRIGGER", "NO_TRADE", 8, true));
        rules.add(
                new MigrationRule("B03-MIG-855", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-856", "DRAW_QUARANTINE", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-857", "CHECKPOINT", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-858", "REPLAY", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-859", "CLEANUP", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-860", "VERIFY", "NO_PNL", 5, true));
        rules.add(new MigrationRule("B03-MIG-861", "IDENTITY", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-862", "SCHEMA", "NO_TRADE", 7, true));
        rules.add(new MigrationRule("B03-MIG-863", "INDEX", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-864", "TRIGGER", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-865", "LEGACY_DISCOVERY", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-866", "DRAW_QUARANTINE", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-867", "CHECKPOINT", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-868", "REPLAY", "NO_PNL", 4, true));
        rules.add(new MigrationRule("B03-MIG-869", "CLEANUP", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-870", "VERIFY", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-871", "IDENTITY", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-872", "SCHEMA", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-873", "INDEX", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-874", "TRIGGER", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-875", "LEGACY_DISCOVERY", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-876", "DRAW_QUARANTINE", "NO_PNL", 3, true));
        rules.add(new MigrationRule("B03-MIG-877", "CHECKPOINT", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-878", "REPLAY", "NO_TRADE", 5, true));
        rules.add(new MigrationRule("B03-MIG-879", "CLEANUP", "BINANCE_ISOLATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-880", "VERIFY", "HASH_REQUIRED", 7, true));
        rules.add(new MigrationRule("B03-MIG-881", "IDENTITY", "DRAW_ONLY", 8, true));
        rules.add(new MigrationRule("B03-MIG-882", "SCHEMA", "NO_PREDICTION", 9, true));
        rules.add(new MigrationRule("B03-MIG-883", "INDEX", "NO_CALIBRATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-884", "TRIGGER", "NO_PNL", 2, true));
        rules.add(
                new MigrationRule("B03-MIG-885", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-886", "DRAW_QUARANTINE", "NO_TRADE", 4, true));
        rules.add(new MigrationRule("B03-MIG-887", "CHECKPOINT", "BINANCE_ISOLATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-888", "REPLAY", "HASH_REQUIRED", 6, true));
        rules.add(new MigrationRule("B03-MIG-889", "CLEANUP", "DRAW_ONLY", 7, true));
        rules.add(new MigrationRule("B03-MIG-890", "VERIFY", "NO_PREDICTION", 8, true));
        rules.add(new MigrationRule("B03-MIG-891", "IDENTITY", "NO_CALIBRATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-892", "SCHEMA", "NO_PNL", 1, true));
        rules.add(new MigrationRule("B03-MIG-893", "INDEX", "NO_AUTHORIZATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-894", "TRIGGER", "NO_TRADE", 3, true));
        rules.add(
                new MigrationRule("B03-MIG-895", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-896", "DRAW_QUARANTINE", "HASH_REQUIRED", 5, true));
        rules.add(new MigrationRule("B03-MIG-897", "CHECKPOINT", "DRAW_ONLY", 6, true));
        rules.add(new MigrationRule("B03-MIG-898", "REPLAY", "NO_PREDICTION", 7, true));
        rules.add(new MigrationRule("B03-MIG-899", "CLEANUP", "NO_CALIBRATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-900", "VERIFY", "NO_PNL", 9, true));
        rules.add(new MigrationRule("B03-MIG-901", "IDENTITY", "NO_AUTHORIZATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-902", "SCHEMA", "NO_TRADE", 2, true));
        rules.add(new MigrationRule("B03-MIG-903", "INDEX", "BINANCE_ISOLATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-904", "TRIGGER", "HASH_REQUIRED", 4, true));
        rules.add(new MigrationRule("B03-MIG-905", "LEGACY_DISCOVERY", "DRAW_ONLY", 5, true));
        rules.add(new MigrationRule("B03-MIG-906", "DRAW_QUARANTINE", "NO_PREDICTION", 6, true));
        rules.add(new MigrationRule("B03-MIG-907", "CHECKPOINT", "NO_CALIBRATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-908", "REPLAY", "NO_PNL", 8, true));
        rules.add(new MigrationRule("B03-MIG-909", "CLEANUP", "NO_AUTHORIZATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-910", "VERIFY", "NO_TRADE", 1, true));
        rules.add(new MigrationRule("B03-MIG-911", "IDENTITY", "BINANCE_ISOLATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-912", "SCHEMA", "HASH_REQUIRED", 3, true));
        rules.add(new MigrationRule("B03-MIG-913", "INDEX", "DRAW_ONLY", 4, true));
        rules.add(new MigrationRule("B03-MIG-914", "TRIGGER", "NO_PREDICTION", 5, true));
        rules.add(new MigrationRule("B03-MIG-915", "LEGACY_DISCOVERY", "NO_CALIBRATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-916", "DRAW_QUARANTINE", "NO_PNL", 7, true));
        rules.add(new MigrationRule("B03-MIG-917", "CHECKPOINT", "NO_AUTHORIZATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-918", "REPLAY", "NO_TRADE", 9, true));
        rules.add(new MigrationRule("B03-MIG-919", "CLEANUP", "BINANCE_ISOLATION", 1, true));
        rules.add(new MigrationRule("B03-MIG-920", "VERIFY", "HASH_REQUIRED", 2, true));
        rules.add(new MigrationRule("B03-MIG-921", "IDENTITY", "DRAW_ONLY", 3, true));
        rules.add(new MigrationRule("B03-MIG-922", "SCHEMA", "NO_PREDICTION", 4, true));
        rules.add(new MigrationRule("B03-MIG-923", "INDEX", "NO_CALIBRATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-924", "TRIGGER", "NO_PNL", 6, true));
        rules.add(
                new MigrationRule("B03-MIG-925", "LEGACY_DISCOVERY", "NO_AUTHORIZATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-926", "DRAW_QUARANTINE", "NO_TRADE", 8, true));
        rules.add(new MigrationRule("B03-MIG-927", "CHECKPOINT", "BINANCE_ISOLATION", 9, true));
        rules.add(new MigrationRule("B03-MIG-928", "REPLAY", "HASH_REQUIRED", 1, true));
        rules.add(new MigrationRule("B03-MIG-929", "CLEANUP", "DRAW_ONLY", 2, true));
        rules.add(new MigrationRule("B03-MIG-930", "VERIFY", "NO_PREDICTION", 3, true));
        rules.add(new MigrationRule("B03-MIG-931", "IDENTITY", "NO_CALIBRATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-932", "SCHEMA", "NO_PNL", 5, true));
        rules.add(new MigrationRule("B03-MIG-933", "INDEX", "NO_AUTHORIZATION", 6, true));
        rules.add(new MigrationRule("B03-MIG-934", "TRIGGER", "NO_TRADE", 7, true));
        rules.add(
                new MigrationRule("B03-MIG-935", "LEGACY_DISCOVERY", "BINANCE_ISOLATION", 8, true));
        rules.add(new MigrationRule("B03-MIG-936", "DRAW_QUARANTINE", "HASH_REQUIRED", 9, true));
        rules.add(new MigrationRule("B03-MIG-937", "CHECKPOINT", "DRAW_ONLY", 1, true));
        rules.add(new MigrationRule("B03-MIG-938", "REPLAY", "NO_PREDICTION", 2, true));
        rules.add(new MigrationRule("B03-MIG-939", "CLEANUP", "NO_CALIBRATION", 3, true));
        rules.add(new MigrationRule("B03-MIG-940", "VERIFY", "NO_PNL", 4, true));
        rules.add(new MigrationRule("B03-MIG-941", "IDENTITY", "NO_AUTHORIZATION", 5, true));
        rules.add(new MigrationRule("B03-MIG-942", "SCHEMA", "NO_TRADE", 6, true));
        rules.add(new MigrationRule("B03-MIG-943", "INDEX", "BINANCE_ISOLATION", 7, true));
        rules.add(new MigrationRule("B03-MIG-944", "TRIGGER", "HASH_REQUIRED", 8, true));
        rules.add(new MigrationRule("B03-MIG-945", "LEGACY_DISCOVERY", "DRAW_ONLY", 9, true));
        rules.add(new MigrationRule("B03-MIG-946", "DRAW_QUARANTINE", "NO_PREDICTION", 1, true));
        rules.add(new MigrationRule("B03-MIG-947", "CHECKPOINT", "NO_CALIBRATION", 2, true));
        rules.add(new MigrationRule("B03-MIG-948", "REPLAY", "NO_PNL", 3, true));
        rules.add(new MigrationRule("B03-MIG-949", "CLEANUP", "NO_AUTHORIZATION", 4, true));
        rules.add(new MigrationRule("B03-MIG-950", "VERIFY", "NO_TRADE", 5, true));
        return Collections.unmodifiableList(rules);
    }

    public static String migrationRuleHash() {
        StringBuilder b = new StringBuilder();
        List<MigrationRule> x = migrationRules();
        for (int i = 0; i < x.size(); i++) {
            if (i > 0) b.append('|');
            b.append(x.get(i).canonical());
        }
        return V51ChecksumEngine.sha256(b.toString());
    }

    public static boolean selfTest() {
        return TARGET_DATABASE_VERSION == 57
                && GAME_CODE.equals(AppConfig.LOTTERY_CODE)
                && DATABASE_NAME.equals(AppConfig.DB_NAME)
                && migrationRules().size() == 950
                && !mayImportTable("predictions")
                && mayImportTable("draws")
                && migrationRuleHash().length() == 64;
    }
}
