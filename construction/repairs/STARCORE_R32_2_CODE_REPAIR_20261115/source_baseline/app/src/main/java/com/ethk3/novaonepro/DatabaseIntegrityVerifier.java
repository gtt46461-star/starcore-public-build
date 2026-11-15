package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** 星核天枢B03只读数据库完整性、外键、不可变账本与回放一致性验证器。 */
public final class DatabaseIntegrityVerifier {
    public static final String VERSION = "V57.3.0_TIANSHU_B03_INTEGRITY";
    public static final String GAME_CODE = "Binance1-PK10";

    public static final class IntegrityReport {
        public final boolean valid;
        public final int tableCount, missingTableCount, missingColumnCount, missingIndexCount;
        public final String expectedChecksum, storedChecksum, detail;

        IntegrityReport(
                boolean valid,
                int tableCount,
                int missingTables,
                int missingColumns,
                int missingIndexes,
                String expected,
                String stored,
                String detail) {
            this.valid = valid;
            this.tableCount = tableCount;
            this.missingTableCount = missingTables;
            this.missingColumnCount = missingColumns;
            this.missingIndexCount = missingIndexes;
            this.expectedChecksum = safe(expected);
            this.storedChecksum = safe(stored);
            this.detail = safe(detail);
        }
    }

    public static final class B03IntegrityReport {
        public final boolean valid;
        public final int checkedObjects,
                missingTables,
                missingIndexes,
                missingTriggers,
                foreignKeyViolations,
                orphanRows,
                wrongGameRows,
                emptyHashRows,
                immutableViolations;
        public final String quickCheck, schemaHash, replayHash, detail;

        B03IntegrityReport(
                boolean valid,
                int checked,
                int mt,
                int mi,
                int mtr,
                int fk,
                int orphan,
                int wrong,
                int empty,
                int immutable,
                String quick,
                String schema,
                String replay,
                String detail) {
            this.valid = valid;
            checkedObjects = checked;
            missingTables = mt;
            missingIndexes = mi;
            missingTriggers = mtr;
            foreignKeyViolations = fk;
            orphanRows = orphan;
            wrongGameRows = wrong;
            emptyHashRows = empty;
            immutableViolations = immutable;
            quickCheck = safe(quick);
            schemaHash = safe(schema);
            replayHash = safe(replay);
            this.detail = safe(detail);
        }

        public String canonical() {
            return valid
                    + "|"
                    + checkedObjects
                    + "|"
                    + missingTables
                    + "|"
                    + missingIndexes
                    + "|"
                    + missingTriggers
                    + "|"
                    + foreignKeyViolations
                    + "|"
                    + orphanRows
                    + "|"
                    + wrongGameRows
                    + "|"
                    + emptyHashRows
                    + "|"
                    + immutableViolations
                    + "|"
                    + quickCheck
                    + "|"
                    + schemaHash
                    + "|"
                    + replayHash
                    + "|"
                    + detail;
        }
    }

    public static final class IntegrityRule {
        public final String id, table, check;
        public final int severity;
        public final boolean mandatory;

        IntegrityRule(String id, String table, String check, int severity, boolean mandatory) {
            this.id = id;
            this.table = table;
            this.check = check;
            this.severity = severity;
            this.mandatory = mandatory;
        }

        public String canonical() {
            return id + '|' + table + '|' + check + '|' + severity + '|' + (mandatory ? 1 : 0);
        }
    }

    private DatabaseIntegrityVerifier() {}
    private static final ThreadLocal<android.os.CancellationSignal> QUERY_CANCEL_R82=new ThreadLocal<android.os.CancellationSignal>();
    public static IntegrityReport verify(SQLiteDatabase db,android.os.CancellationSignal cancel) {
        android.os.CancellationSignal previous=QUERY_CANCEL_R82.get();QUERY_CANCEL_R82.set(cancel);
        try { if(cancel!=null)cancel.throwIfCanceled();return verify(db); }
        finally { if(previous==null)QUERY_CANCEL_R82.remove();else QUERY_CANCEL_R82.set(previous); }
    }
    private static Cursor queryR82(SQLiteDatabase db,String sql,String[] args) {
        android.os.CancellationSignal signal=QUERY_CANCEL_R82.get();if(signal!=null)signal.throwIfCanceled();
        return db.rawQuery(sql,args,signal);
    }

    public static IntegrityReport verify(SQLiteDatabase db) {
        if (db == null)
            return new IntegrityReport(
                    false, 0, 1, 0, 0, DbHelper.b03SchemaHash(), "", "B03_DATABASE_NULL");
        int mt = 0, mc = 0, mi = 0;
        StringBuilder detail = new StringBuilder();
        List<SchemaContractRegistry.Table> legacy = SchemaContractRegistry.tables();
        for (int i = 0; i < legacy.size(); i++) {
            SchemaContractRegistry.Table t = legacy.get(i);
            if (!exists(db, "table", t.name)) {
                mt++;
                append(detail, "MISSING_TABLE:" + t.name);
                continue;
            }
            HashSet<String> cols = readColumns(db, t.name);
            for (int c = 0; c < t.columns.size(); c++)
                if (!cols.contains(t.columns.get(c).name)) {
                    mc++;
                    append(detail, "MISSING_COLUMN:" + t.name + '.' + t.columns.get(c).name);
                }
            for (int x = 0; x < t.indexes.size(); x++)
                if (!exists(db, "index", t.indexes.get(x).name)) {
                    mi++;
                    append(detail, "MISSING_INDEX:" + t.indexes.get(x).name);
                }
        }
        B03IntegrityReport b03 = verifyB03(db);
        if (!b03.valid) append(detail, b03.detail);
        String expected = DbHelper.b03SchemaHash(), stored = metaB03(db, "schema_hash");
        boolean valid = mt == 0 && mc == 0 && mi == 0 && b03.valid && expected.equals(stored);
        if (detail.length() == 0) detail.append("B03_DATABASE_INTEGRITY_OK");
        return new IntegrityReport(
                valid,
                legacy.size() + DbHelper.b03TableNames().size(),
                mt + b03.missingTables,
                mc,
                mi + b03.missingIndexes,
                expected,
                stored,
                detail.toString());
    }

    public static B03IntegrityReport verifyB03(SQLiteDatabase db) {
        if (db == null)
            return new B03IntegrityReport(
                    false, 0, 1, 0, 0, 0, 0, 0, 0, 0, "", "", "", "DATABASE_NULL");
        int mt = 0, mi = 0, mtr = 0;
        StringBuilder detail = new StringBuilder();
        List<String> tables = DbHelper.b03TableNames(),
                indexes = DbHelper.b03IndexNames(),
                triggers = DbHelper.b03TriggerNames();
        for (int i = 0; i < tables.size(); i++)
            if (!exists(db, "table", tables.get(i))) {
                mt++;
                append(detail, "MISSING_TABLE:" + tables.get(i));
            }
        for (int i = 0; i < indexes.size(); i++)
            if (!exists(db, "index", indexes.get(i))) {
                mi++;
                append(detail, "MISSING_INDEX:" + indexes.get(i));
            }
        for (int i = 0; i < triggers.size(); i++)
            if (!exists(db, "trigger", triggers.get(i))) {
                mtr++;
                append(detail, "MISSING_TRIGGER:" + triggers.get(i));
            }
        String quick = pragmaFirst(db, "PRAGMA quick_check");
        int fk = countRows(db, "PRAGMA foreign_key_check", null);
        int orphan = orphanCount(db);
        int wrong = wrongGameCount(db);
        int empty = emptyHashCount(db);
        int immutable = immutableAuditViolationCount(db);
        String schema = DbHelper.b03SchemaHash();
        String replay = replayHash(db);
        if (!"ok".equalsIgnoreCase(quick)) append(detail, "QUICK_CHECK:" + quick);
        if (fk > 0) append(detail, "FOREIGN_KEY:" + fk);
        if (orphan > 0) append(detail, "ORPHAN:" + orphan);
        if (wrong > 0) append(detail, "WRONG_GAME:" + wrong);
        if (empty > 0) append(detail, "EMPTY_HASH:" + empty);
        if (immutable > 0) append(detail, "IMMUTABLE_AUDIT:" + immutable);
        boolean valid =
                mt == 0
                        && mi == 0
                        && mtr == 0
                        && "ok".equalsIgnoreCase(quick)
                        && fk == 0
                        && orphan == 0
                        && wrong == 0
                        && empty == 0
                        && immutable == 0;
        if (detail.length() == 0) detail.append("B03_INTEGRITY_OK");
        return new B03IntegrityReport(
                valid,
                tables.size() + indexes.size() + triggers.size(),
                mt,
                mi,
                mtr,
                fk,
                orphan,
                wrong,
                empty,
                immutable,
                quick,
                schema,
                replay,
                detail.toString());
    }

    public static long persistSnapshot(SQLiteDatabase db, B03IntegrityReport report) {
        if (db == null || report == null) return -1L;
        ContentValues cv = new ContentValues();
        cv.put("snapshot_hash", V51ChecksumEngine.sha256(report.canonical()));
        cv.put("schema_hash", report.schemaHash);
        cv.put("replay_hash", report.replayHash);
        cv.put("valid", report.valid ? 1 : 0);
        cv.put("detail", report.detail);
        cv.put("created_at", System.currentTimeMillis());
        return db.insertWithOnConflict(
                DbHelper.TABLE_B03_INTEGRITY_SNAPSHOT, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static boolean hasUniqueTargetRows(SQLiteDatabase db, String table, String targetIssue) {
        if (db == null || !safeIdentifier(table) || safe(targetIssue).length() == 0) return false;
        return scalarInt(
                        db,
                        "SELECT COUNT(*) FROM " + table + " WHERE target_issue=?",
                        new String[] {targetIssue})
                <= 1;
    }

    public static boolean isGameIsolated(SQLiteDatabase db) {
        return wrongGameCount(db) == 0;
    }

    public static boolean hasImmutableTriggers(SQLiteDatabase db) {
        List<String> x = DbHelper.b03TriggerNames();
        for (int i = 0; i < x.size(); i++) if (!exists(db, "trigger", x.get(i))) return false;
        return true;
    }

    private static int orphanCount(SQLiteDatabase db) {
        int n = 0;
        n +=
                scalarInt(
                        db,
                        "SELECT COUNT(*) FROM "
                                + DbHelper.TABLE_B03_NUMBER_EVENT
                                + " n LEFT JOIN "
                                + DbHelper.TABLE_B03_PREDICTION_BUNDLE
                                + " p ON p.bundle_id=n.bundle_id WHERE p.bundle_id IS NULL",
                        null);
        n +=
                scalarInt(
                        db,
                        "SELECT COUNT(*) FROM "
                                + DbHelper.TABLE_B03_ACTION_EVENT
                                + " a LEFT JOIN "
                                + DbHelper.TABLE_B03_PREDICTION_BUNDLE
                                + " p ON p.bundle_id=a.bundle_id WHERE p.bundle_id IS NULL",
                        null);
        n +=
                scalarInt(
                        db,
                        "SELECT COUNT(*) FROM "
                                + DbHelper.TABLE_B03_SELECTED_EVENT
                                + " s LEFT JOIN "
                                + DbHelper.TABLE_B03_PREDICTION_BUNDLE
                                + " p ON p.bundle_id=s.bundle_id WHERE p.bundle_id IS NULL",
                        null);
        return n;
    }

    private static int wrongGameCount(SQLiteDatabase db) {
        String[] tables = {
            DbHelper.TABLE_B03_PREDICTION_BUNDLE,
            DbHelper.TABLE_B03_NUMBER_EVENT,
            DbHelper.TABLE_B03_ACTION_EVENT,
            DbHelper.TABLE_B03_PERMUTATION_EVENT,
            DbHelper.TABLE_B03_SELECTED_EVENT,
            DbHelper.TABLE_B03_AUTHORIZATION_EVENT,
            DbHelper.TABLE_B03_SETTLEMENT_EVENT,
            DbHelper.TABLE_B03_TRANSACTION_AUDIT
        };
        int n = 0;
        for (int i = 0; i < tables.length; i++)
            n +=
                    scalarInt(
                            db,
                            "SELECT COUNT(*) FROM " + tables[i] + " WHERE game_code<>?",
                            new String[] {DatabaseCoordinator.GAME_CODE});
        return n;
    }

    private static int emptyHashCount(SQLiteDatabase db) {
        int n = 0;
        n +=
                scalarInt(
                        db,
                        "SELECT COUNT(*) FROM "
                                + DbHelper.TABLE_B03_PREDICTION_BUNDLE
                                + " WHERE bundle_hash='' OR contract_hash='' OR record_hash=''",
                        null);
        n +=
                scalarInt(
                        db,
                        "SELECT COUNT(*) FROM "
                                + DbHelper.TABLE_B03_SETTLEMENT_EVENT
                                + " WHERE settlement_hash='' OR record_hash=''",
                        null);
        n +=
                scalarInt(
                        db,
                        "SELECT COUNT(*) FROM "
                                + DbHelper.TABLE_B03_TRANSACTION_AUDIT
                                + " WHERE transaction_hash='' OR idempotency_key=''",
                        null);
        return n;
    }

    private static int immutableAuditViolationCount(SQLiteDatabase db) {
        return scalarInt(
                db,
                "SELECT COUNT(*) FROM "
                        + DbHelper.TABLE_B03_TRANSACTION_AUDIT
                        + " WHERE status NOT IN ('COMMITTED','ROLLED_BACK')",
                null);
    }

    private static String replayHash(SQLiteDatabase db) {
        StringBuilder b = new StringBuilder();
        appendRows(
                db,
                b,
                "SELECT record_hash FROM "
                        + DbHelper.TABLE_B03_PREDICTION_BUNDLE
                        + " ORDER BY game_code,target_issue");
        appendRows(
                db,
                b,
                "SELECT record_hash FROM "
                        + DbHelper.TABLE_B03_SETTLEMENT_EVENT
                        + " ORDER BY game_code,target_issue,created_at");
        return V51ChecksumEngine.sha256(b.toString());
    }

    private static void appendRows(SQLiteDatabase db, StringBuilder b, String sql) {
        Cursor c = null;
        try {
            c = queryR82(db,sql, null);
            while (c.moveToNext()) b.append(c.isNull(0) ? "" : c.getString(0));
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean exists(SQLiteDatabase db, String type, String name) {
        Cursor c = null;
        try {
            c =
                    queryR82(db,
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
            c = queryR82(db,"PRAGMA table_info(" + table + ")", null);
            int n = c.getColumnIndex("name");
            while (c.moveToNext()) if (n >= 0) x.add(c.getString(n));
        } finally {
            if (c != null) c.close();
        }
        return x;
    }

    private static String metaB03(SQLiteDatabase db, String key) {
        Cursor c = null;
        try {
            c=queryR82(db,"SELECT meta_value FROM "+DbHelper.TABLE_B03_SCHEMA_META+" WHERE meta_key=? LIMIT 1",new String[]{key});
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } catch (Throwable e) {
            if(e instanceof android.os.OperationCanceledException)throw (android.os.OperationCanceledException)e;
            return "";
        } finally {
            if (c != null) c.close();
        }
    }

    private static String pragmaFirst(SQLiteDatabase db, String sql) {
        Cursor c = null;
        try {
            c = queryR82(db,sql, null);
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } catch (Throwable e) {
            if(e instanceof android.os.OperationCanceledException)throw (android.os.OperationCanceledException)e;
            return "ERROR:" + e.getClass().getSimpleName();
        } finally {
            if (c != null) c.close();
        }
    }

    private static int countRows(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        int n = 0;
        try {
            c = queryR82(db,sql, args);
            while (c.moveToNext()) n++;
        } finally {
            if (c != null) c.close();
        }
        return n;
    }

    private static int scalarInt(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = queryR82(db,sql, args);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Throwable e) {
            if(e instanceof android.os.OperationCanceledException)throw (android.os.OperationCanceledException)e;
            return Integer.MAX_VALUE / 4;
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean safeIdentifier(String value) {
        return safe(value).matches("[A-Za-z0-9_]+");
    }

    private static void append(StringBuilder sb, String value) {
        if (sb.length() > 0) sb.append('|');
        sb.append(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static List<IntegrityRule> integrityRules() {
        ArrayList<IntegrityRule> items = new ArrayList<IntegrityRule>();
        items.add(new IntegrityRule("B03-INT-001", "b03_schema_meta", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-002", "b03_transaction_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-003", "b03_migration_journal", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-004", "b03_draw_quarantine", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-005", "b03_prediction_bundle", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-006", "b03_number_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-007", "b03_action_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-008", "b03_permutation_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-009", "b03_selected_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-010", "b03_authorization_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-011", "b03_settlement_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-012", "b03_integrity_snapshot", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-013", "b03_replay_audit", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-014", "b03_schema_meta", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-015", "b03_transaction_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-016", "b03_migration_journal", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-017", "b03_draw_quarantine", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-018", "b03_prediction_bundle", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-019", "b03_number_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-020", "b03_action_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-021", "b03_permutation_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-022", "b03_selected_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-023", "b03_authorization_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-024", "b03_settlement_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-025", "b03_integrity_snapshot", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-026", "b03_replay_audit", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-027", "b03_schema_meta", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-028", "b03_transaction_audit", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-029", "b03_migration_journal", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-030", "b03_draw_quarantine", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-031", "b03_prediction_bundle", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-032", "b03_number_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-033", "b03_action_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-034", "b03_permutation_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-035", "b03_selected_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-036", "b03_authorization_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-037", "b03_settlement_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-038", "b03_integrity_snapshot", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-039", "b03_replay_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-040", "b03_schema_meta", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-041", "b03_transaction_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-042", "b03_migration_journal", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-043", "b03_draw_quarantine", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-044", "b03_prediction_bundle", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-045", "b03_number_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-046", "b03_action_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-047", "b03_permutation_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-048", "b03_selected_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-049", "b03_authorization_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-050", "b03_settlement_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-051", "b03_integrity_snapshot", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-052", "b03_replay_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-053", "b03_schema_meta", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-054", "b03_transaction_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-055", "b03_migration_journal", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-056", "b03_draw_quarantine", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-057", "b03_prediction_bundle", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-058", "b03_number_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-059", "b03_action_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-060", "b03_permutation_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-061", "b03_selected_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-062", "b03_authorization_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-063", "b03_settlement_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-064", "b03_integrity_snapshot", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-065", "b03_replay_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-066", "b03_schema_meta", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-067", "b03_transaction_audit", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-068", "b03_migration_journal", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-069", "b03_draw_quarantine", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-070", "b03_prediction_bundle", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-071", "b03_number_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-072", "b03_action_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-073", "b03_permutation_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-074", "b03_selected_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-075", "b03_authorization_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-076", "b03_settlement_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-077", "b03_integrity_snapshot", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-078", "b03_replay_audit", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-079", "b03_schema_meta", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-080", "b03_transaction_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-081", "b03_migration_journal", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-082", "b03_draw_quarantine", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-083", "b03_prediction_bundle", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-084", "b03_number_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-085", "b03_action_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-086", "b03_permutation_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-087", "b03_selected_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-088", "b03_authorization_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-089", "b03_settlement_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-090", "b03_integrity_snapshot", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-091", "b03_replay_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-092", "b03_schema_meta", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-093", "b03_transaction_audit", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-094", "b03_migration_journal", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-095", "b03_draw_quarantine", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-096", "b03_prediction_bundle", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-097", "b03_number_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-098", "b03_action_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-099", "b03_permutation_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-100", "b03_selected_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-101", "b03_authorization_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-102", "b03_settlement_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-103", "b03_integrity_snapshot", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-104", "b03_replay_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-105", "b03_schema_meta", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-106", "b03_transaction_audit", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-107", "b03_migration_journal", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-108", "b03_draw_quarantine", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-109", "b03_prediction_bundle", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-110", "b03_number_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-111", "b03_action_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-112", "b03_permutation_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-113", "b03_selected_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-114", "b03_authorization_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-115", "b03_settlement_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-116", "b03_integrity_snapshot", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-117", "b03_replay_audit", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-118", "b03_schema_meta", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-119", "b03_transaction_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-120", "b03_migration_journal", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-121", "b03_draw_quarantine", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-122", "b03_prediction_bundle", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-123", "b03_number_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-124", "b03_action_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-125", "b03_permutation_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-126", "b03_selected_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-127", "b03_authorization_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-128", "b03_settlement_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-129", "b03_integrity_snapshot", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-130", "b03_replay_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-131", "b03_schema_meta", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-132", "b03_transaction_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-133", "b03_migration_journal", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-134", "b03_draw_quarantine", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-135", "b03_prediction_bundle", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-136", "b03_number_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-137", "b03_action_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-138", "b03_permutation_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-139", "b03_selected_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-140", "b03_authorization_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-141", "b03_settlement_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-142", "b03_integrity_snapshot", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-143", "b03_replay_audit", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-144", "b03_schema_meta", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-145", "b03_transaction_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-146", "b03_migration_journal", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-147", "b03_draw_quarantine", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-148", "b03_prediction_bundle", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-149", "b03_number_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-150", "b03_action_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-151", "b03_permutation_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-152", "b03_selected_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-153", "b03_authorization_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-154", "b03_settlement_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-155", "b03_integrity_snapshot", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-156", "b03_replay_audit", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-157", "b03_schema_meta", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-158", "b03_transaction_audit", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-159", "b03_migration_journal", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-160", "b03_draw_quarantine", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-161", "b03_prediction_bundle", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-162", "b03_number_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-163", "b03_action_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-164", "b03_permutation_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-165", "b03_selected_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-166", "b03_authorization_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-167", "b03_settlement_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-168", "b03_integrity_snapshot", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-169", "b03_replay_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-170", "b03_schema_meta", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-171", "b03_transaction_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-172", "b03_migration_journal", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-173", "b03_draw_quarantine", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-174", "b03_prediction_bundle", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-175", "b03_number_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-176", "b03_action_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-177", "b03_permutation_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-178", "b03_selected_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-179", "b03_authorization_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-180", "b03_settlement_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-181", "b03_integrity_snapshot", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-182", "b03_replay_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-183", "b03_schema_meta", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-184", "b03_transaction_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-185", "b03_migration_journal", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-186", "b03_draw_quarantine", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-187", "b03_prediction_bundle", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-188", "b03_number_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-189", "b03_action_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-190", "b03_permutation_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-191", "b03_selected_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-192", "b03_authorization_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-193", "b03_settlement_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-194", "b03_integrity_snapshot", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-195", "b03_replay_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-196", "b03_schema_meta", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-197", "b03_transaction_audit", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-198", "b03_migration_journal", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-199", "b03_draw_quarantine", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-200", "b03_prediction_bundle", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-201", "b03_number_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-202", "b03_action_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-203", "b03_permutation_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-204", "b03_selected_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-205", "b03_authorization_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-206", "b03_settlement_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-207", "b03_integrity_snapshot", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-208", "b03_replay_audit", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-209", "b03_schema_meta", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-210", "b03_transaction_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-211", "b03_migration_journal", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-212", "b03_draw_quarantine", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-213", "b03_prediction_bundle", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-214", "b03_number_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-215", "b03_action_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-216", "b03_permutation_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-217", "b03_selected_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-218", "b03_authorization_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-219", "b03_settlement_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-220", "b03_integrity_snapshot", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-221", "b03_replay_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-222", "b03_schema_meta", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-223", "b03_transaction_audit", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-224", "b03_migration_journal", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-225", "b03_draw_quarantine", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-226", "b03_prediction_bundle", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-227", "b03_number_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-228", "b03_action_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-229", "b03_permutation_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-230", "b03_selected_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-231", "b03_authorization_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-232", "b03_settlement_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-233", "b03_integrity_snapshot", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-234", "b03_replay_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-235", "b03_schema_meta", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-236", "b03_transaction_audit", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-237", "b03_migration_journal", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-238", "b03_draw_quarantine", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-239", "b03_prediction_bundle", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-240", "b03_number_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-241", "b03_action_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-242", "b03_permutation_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-243", "b03_selected_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-244", "b03_authorization_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-245", "b03_settlement_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-246", "b03_integrity_snapshot", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-247", "b03_replay_audit", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-248", "b03_schema_meta", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-249", "b03_transaction_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-250", "b03_migration_journal", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-251", "b03_draw_quarantine", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-252", "b03_prediction_bundle", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-253", "b03_number_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-254", "b03_action_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-255", "b03_permutation_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-256", "b03_selected_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-257", "b03_authorization_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-258", "b03_settlement_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-259", "b03_integrity_snapshot", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-260", "b03_replay_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-261", "b03_schema_meta", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-262", "b03_transaction_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-263", "b03_migration_journal", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-264", "b03_draw_quarantine", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-265", "b03_prediction_bundle", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-266", "b03_number_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-267", "b03_action_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-268", "b03_permutation_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-269", "b03_selected_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-270", "b03_authorization_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-271", "b03_settlement_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-272", "b03_integrity_snapshot", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-273", "b03_replay_audit", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-274", "b03_schema_meta", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-275", "b03_transaction_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-276", "b03_migration_journal", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-277", "b03_draw_quarantine", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-278", "b03_prediction_bundle", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-279", "b03_number_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-280", "b03_action_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-281", "b03_permutation_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-282", "b03_selected_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-283", "b03_authorization_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-284", "b03_settlement_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-285", "b03_integrity_snapshot", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-286", "b03_replay_audit", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-287", "b03_schema_meta", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-288", "b03_transaction_audit", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-289", "b03_migration_journal", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-290", "b03_draw_quarantine", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-291", "b03_prediction_bundle", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-292", "b03_number_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-293", "b03_action_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-294", "b03_permutation_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-295", "b03_selected_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-296", "b03_authorization_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-297", "b03_settlement_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-298", "b03_integrity_snapshot", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-299", "b03_replay_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-300", "b03_schema_meta", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-301", "b03_transaction_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-302", "b03_migration_journal", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-303", "b03_draw_quarantine", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-304", "b03_prediction_bundle", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-305", "b03_number_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-306", "b03_action_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-307", "b03_permutation_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-308", "b03_selected_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-309", "b03_authorization_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-310", "b03_settlement_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-311", "b03_integrity_snapshot", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-312", "b03_replay_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-313", "b03_schema_meta", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-314", "b03_transaction_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-315", "b03_migration_journal", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-316", "b03_draw_quarantine", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-317", "b03_prediction_bundle", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-318", "b03_number_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-319", "b03_action_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-320", "b03_permutation_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-321", "b03_selected_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-322", "b03_authorization_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-323", "b03_settlement_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-324", "b03_integrity_snapshot", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-325", "b03_replay_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-326", "b03_schema_meta", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-327", "b03_transaction_audit", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-328", "b03_migration_journal", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-329", "b03_draw_quarantine", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-330", "b03_prediction_bundle", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-331", "b03_number_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-332", "b03_action_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-333", "b03_permutation_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-334", "b03_selected_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-335", "b03_authorization_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-336", "b03_settlement_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-337", "b03_integrity_snapshot", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-338", "b03_replay_audit", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-339", "b03_schema_meta", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-340", "b03_transaction_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-341", "b03_migration_journal", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-342", "b03_draw_quarantine", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-343", "b03_prediction_bundle", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-344", "b03_number_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-345", "b03_action_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-346", "b03_permutation_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-347", "b03_selected_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-348", "b03_authorization_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-349", "b03_settlement_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-350", "b03_integrity_snapshot", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-351", "b03_replay_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-352", "b03_schema_meta", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-353", "b03_transaction_audit", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-354", "b03_migration_journal", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-355", "b03_draw_quarantine", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-356", "b03_prediction_bundle", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-357", "b03_number_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-358", "b03_action_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-359", "b03_permutation_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-360", "b03_selected_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-361", "b03_authorization_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-362", "b03_settlement_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-363", "b03_integrity_snapshot", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-364", "b03_replay_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-365", "b03_schema_meta", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-366", "b03_transaction_audit", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-367", "b03_migration_journal", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-368", "b03_draw_quarantine", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-369", "b03_prediction_bundle", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-370", "b03_number_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-371", "b03_action_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-372", "b03_permutation_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-373", "b03_selected_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-374", "b03_authorization_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-375", "b03_settlement_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-376", "b03_integrity_snapshot", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-377", "b03_replay_audit", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-378", "b03_schema_meta", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-379", "b03_transaction_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-380", "b03_migration_journal", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-381", "b03_draw_quarantine", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-382", "b03_prediction_bundle", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-383", "b03_number_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-384", "b03_action_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-385", "b03_permutation_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-386", "b03_selected_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-387", "b03_authorization_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-388", "b03_settlement_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-389", "b03_integrity_snapshot", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-390", "b03_replay_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-391", "b03_schema_meta", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-392", "b03_transaction_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-393", "b03_migration_journal", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-394", "b03_draw_quarantine", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-395", "b03_prediction_bundle", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-396", "b03_number_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-397", "b03_action_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-398", "b03_permutation_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-399", "b03_selected_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-400", "b03_authorization_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-401", "b03_settlement_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-402", "b03_integrity_snapshot", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-403", "b03_replay_audit", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-404", "b03_schema_meta", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-405", "b03_transaction_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-406", "b03_migration_journal", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-407", "b03_draw_quarantine", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-408", "b03_prediction_bundle", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-409", "b03_number_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-410", "b03_action_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-411", "b03_permutation_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-412", "b03_selected_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-413", "b03_authorization_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-414", "b03_settlement_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-415", "b03_integrity_snapshot", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-416", "b03_replay_audit", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-417", "b03_schema_meta", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-418", "b03_transaction_audit", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-419", "b03_migration_journal", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-420", "b03_draw_quarantine", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-421", "b03_prediction_bundle", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-422", "b03_number_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-423", "b03_action_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-424", "b03_permutation_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-425", "b03_selected_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-426", "b03_authorization_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-427", "b03_settlement_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-428", "b03_integrity_snapshot", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-429", "b03_replay_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-430", "b03_schema_meta", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-431", "b03_transaction_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-432", "b03_migration_journal", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-433", "b03_draw_quarantine", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-434", "b03_prediction_bundle", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-435", "b03_number_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-436", "b03_action_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-437", "b03_permutation_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-438", "b03_selected_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-439", "b03_authorization_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-440", "b03_settlement_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-441", "b03_integrity_snapshot", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-442", "b03_replay_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-443", "b03_schema_meta", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-444", "b03_transaction_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-445", "b03_migration_journal", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-446", "b03_draw_quarantine", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-447", "b03_prediction_bundle", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-448", "b03_number_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-449", "b03_action_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-450", "b03_permutation_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-451", "b03_selected_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-452", "b03_authorization_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-453", "b03_settlement_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-454", "b03_integrity_snapshot", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-455", "b03_replay_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-456", "b03_schema_meta", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-457", "b03_transaction_audit", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-458", "b03_migration_journal", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-459", "b03_draw_quarantine", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-460", "b03_prediction_bundle", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-461", "b03_number_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-462", "b03_action_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-463", "b03_permutation_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-464", "b03_selected_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-465", "b03_authorization_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-466", "b03_settlement_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-467", "b03_integrity_snapshot", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-468", "b03_replay_audit", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-469", "b03_schema_meta", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-470", "b03_transaction_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-471", "b03_migration_journal", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-472", "b03_draw_quarantine", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-473", "b03_prediction_bundle", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-474", "b03_number_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-475", "b03_action_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-476", "b03_permutation_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-477", "b03_selected_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-478", "b03_authorization_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-479", "b03_settlement_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-480", "b03_integrity_snapshot", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-481", "b03_replay_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-482", "b03_schema_meta", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-483", "b03_transaction_audit", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-484", "b03_migration_journal", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-485", "b03_draw_quarantine", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-486", "b03_prediction_bundle", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-487", "b03_number_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-488", "b03_action_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-489", "b03_permutation_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-490", "b03_selected_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-491", "b03_authorization_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-492", "b03_settlement_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-493", "b03_integrity_snapshot", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-494", "b03_replay_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-495", "b03_schema_meta", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-496", "b03_transaction_audit", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-497", "b03_migration_journal", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-498", "b03_draw_quarantine", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-499", "b03_prediction_bundle", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-500", "b03_number_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-501", "b03_action_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-502", "b03_permutation_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-503", "b03_selected_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-504", "b03_authorization_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-505", "b03_settlement_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-506", "b03_integrity_snapshot", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-507", "b03_replay_audit", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-508", "b03_schema_meta", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-509", "b03_transaction_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-510", "b03_migration_journal", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-511", "b03_draw_quarantine", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-512", "b03_prediction_bundle", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-513", "b03_number_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-514", "b03_action_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-515", "b03_permutation_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-516", "b03_selected_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-517", "b03_authorization_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-518", "b03_settlement_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-519", "b03_integrity_snapshot", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-520", "b03_replay_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-521", "b03_schema_meta", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-522", "b03_transaction_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-523", "b03_migration_journal", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-524", "b03_draw_quarantine", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-525", "b03_prediction_bundle", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-526", "b03_number_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-527", "b03_action_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-528", "b03_permutation_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-529", "b03_selected_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-530", "b03_authorization_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-531", "b03_settlement_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-532", "b03_integrity_snapshot", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-533", "b03_replay_audit", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-534", "b03_schema_meta", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-535", "b03_transaction_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-536", "b03_migration_journal", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-537", "b03_draw_quarantine", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-538", "b03_prediction_bundle", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-539", "b03_number_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-540", "b03_action_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-541", "b03_permutation_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-542", "b03_selected_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-543", "b03_authorization_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-544", "b03_settlement_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-545", "b03_integrity_snapshot", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-546", "b03_replay_audit", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-547", "b03_schema_meta", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-548", "b03_transaction_audit", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-549", "b03_migration_journal", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-550", "b03_draw_quarantine", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-551", "b03_prediction_bundle", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-552", "b03_number_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-553", "b03_action_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-554", "b03_permutation_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-555", "b03_selected_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-556", "b03_authorization_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-557", "b03_settlement_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-558", "b03_integrity_snapshot", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-559", "b03_replay_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-560", "b03_schema_meta", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-561", "b03_transaction_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-562", "b03_migration_journal", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-563", "b03_draw_quarantine", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-564", "b03_prediction_bundle", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-565", "b03_number_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-566", "b03_action_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-567", "b03_permutation_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-568", "b03_selected_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-569", "b03_authorization_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-570", "b03_settlement_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-571", "b03_integrity_snapshot", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-572", "b03_replay_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-573", "b03_schema_meta", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-574", "b03_transaction_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-575", "b03_migration_journal", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-576", "b03_draw_quarantine", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-577", "b03_prediction_bundle", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-578", "b03_number_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-579", "b03_action_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-580", "b03_permutation_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-581", "b03_selected_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-582", "b03_authorization_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-583", "b03_settlement_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-584", "b03_integrity_snapshot", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-585", "b03_replay_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-586", "b03_schema_meta", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-587", "b03_transaction_audit", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-588", "b03_migration_journal", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-589", "b03_draw_quarantine", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-590", "b03_prediction_bundle", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-591", "b03_number_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-592", "b03_action_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-593", "b03_permutation_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-594", "b03_selected_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-595", "b03_authorization_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-596", "b03_settlement_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-597", "b03_integrity_snapshot", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-598", "b03_replay_audit", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-599", "b03_schema_meta", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-600", "b03_transaction_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-601", "b03_migration_journal", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-602", "b03_draw_quarantine", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-603", "b03_prediction_bundle", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-604", "b03_number_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-605", "b03_action_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-606", "b03_permutation_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-607", "b03_selected_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-608", "b03_authorization_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-609", "b03_settlement_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-610", "b03_integrity_snapshot", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-611", "b03_replay_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-612", "b03_schema_meta", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-613", "b03_transaction_audit", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-614", "b03_migration_journal", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-615", "b03_draw_quarantine", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-616", "b03_prediction_bundle", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-617", "b03_number_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-618", "b03_action_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-619", "b03_permutation_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-620", "b03_selected_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-621", "b03_authorization_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-622", "b03_settlement_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-623", "b03_integrity_snapshot", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-624", "b03_replay_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-625", "b03_schema_meta", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-626", "b03_transaction_audit", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-627", "b03_migration_journal", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-628", "b03_draw_quarantine", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-629", "b03_prediction_bundle", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-630", "b03_number_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-631", "b03_action_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-632", "b03_permutation_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-633", "b03_selected_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-634", "b03_authorization_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-635", "b03_settlement_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-636", "b03_integrity_snapshot", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-637", "b03_replay_audit", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-638", "b03_schema_meta", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-639", "b03_transaction_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-640", "b03_migration_journal", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-641", "b03_draw_quarantine", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-642", "b03_prediction_bundle", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-643", "b03_number_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-644", "b03_action_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-645", "b03_permutation_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-646", "b03_selected_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-647", "b03_authorization_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-648", "b03_settlement_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-649", "b03_integrity_snapshot", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-650", "b03_replay_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-651", "b03_schema_meta", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-652", "b03_transaction_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-653", "b03_migration_journal", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-654", "b03_draw_quarantine", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-655", "b03_prediction_bundle", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-656", "b03_number_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-657", "b03_action_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-658", "b03_permutation_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-659", "b03_selected_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-660", "b03_authorization_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-661", "b03_settlement_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-662", "b03_integrity_snapshot", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-663", "b03_replay_audit", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-664", "b03_schema_meta", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-665", "b03_transaction_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-666", "b03_migration_journal", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-667", "b03_draw_quarantine", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-668", "b03_prediction_bundle", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-669", "b03_number_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-670", "b03_action_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-671", "b03_permutation_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-672", "b03_selected_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-673", "b03_authorization_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-674", "b03_settlement_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-675", "b03_integrity_snapshot", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-676", "b03_replay_audit", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-677", "b03_schema_meta", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-678", "b03_transaction_audit", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-679", "b03_migration_journal", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-680", "b03_draw_quarantine", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-681", "b03_prediction_bundle", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-682", "b03_number_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-683", "b03_action_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-684", "b03_permutation_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-685", "b03_selected_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-686", "b03_authorization_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-687", "b03_settlement_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-688", "b03_integrity_snapshot", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-689", "b03_replay_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-690", "b03_schema_meta", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-691", "b03_transaction_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-692", "b03_migration_journal", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-693", "b03_draw_quarantine", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-694", "b03_prediction_bundle", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-695", "b03_number_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-696", "b03_action_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-697", "b03_permutation_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-698", "b03_selected_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-699", "b03_authorization_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-700", "b03_settlement_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-701", "b03_integrity_snapshot", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-702", "b03_replay_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-703", "b03_schema_meta", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-704", "b03_transaction_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-705", "b03_migration_journal", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-706", "b03_draw_quarantine", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-707", "b03_prediction_bundle", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-708", "b03_number_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-709", "b03_action_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-710", "b03_permutation_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-711", "b03_selected_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-712", "b03_authorization_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-713", "b03_settlement_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-714", "b03_integrity_snapshot", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-715", "b03_replay_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-716", "b03_schema_meta", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-717", "b03_transaction_audit", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-718", "b03_migration_journal", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-719", "b03_draw_quarantine", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-720", "b03_prediction_bundle", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-721", "b03_number_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-722", "b03_action_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-723", "b03_permutation_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-724", "b03_selected_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-725", "b03_authorization_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-726", "b03_settlement_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-727", "b03_integrity_snapshot", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-728", "b03_replay_audit", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-729", "b03_schema_meta", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-730", "b03_transaction_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-731", "b03_migration_journal", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-732", "b03_draw_quarantine", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-733", "b03_prediction_bundle", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-734", "b03_number_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-735", "b03_action_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-736", "b03_permutation_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-737", "b03_selected_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-738", "b03_authorization_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-739", "b03_settlement_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-740", "b03_integrity_snapshot", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-741", "b03_replay_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-742", "b03_schema_meta", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-743", "b03_transaction_audit", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-744", "b03_migration_journal", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-745", "b03_draw_quarantine", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-746", "b03_prediction_bundle", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-747", "b03_number_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-748", "b03_action_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-749", "b03_permutation_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-750", "b03_selected_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-751", "b03_authorization_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-752", "b03_settlement_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-753", "b03_integrity_snapshot", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-754", "b03_replay_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-755", "b03_schema_meta", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-756", "b03_transaction_audit", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-757", "b03_migration_journal", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-758", "b03_draw_quarantine", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-759", "b03_prediction_bundle", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-760", "b03_number_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-761", "b03_action_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-762", "b03_permutation_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-763", "b03_selected_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-764", "b03_authorization_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-765", "b03_settlement_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-766", "b03_integrity_snapshot", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-767", "b03_replay_audit", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-768", "b03_schema_meta", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-769", "b03_transaction_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-770", "b03_migration_journal", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-771", "b03_draw_quarantine", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-772", "b03_prediction_bundle", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-773", "b03_number_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-774", "b03_action_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-775", "b03_permutation_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-776", "b03_selected_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-777", "b03_authorization_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-778", "b03_settlement_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-779", "b03_integrity_snapshot", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-780", "b03_replay_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-781", "b03_schema_meta", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-782", "b03_transaction_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-783", "b03_migration_journal", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-784", "b03_draw_quarantine", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-785", "b03_prediction_bundle", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-786", "b03_number_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-787", "b03_action_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-788", "b03_permutation_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-789", "b03_selected_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-790", "b03_authorization_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-791", "b03_settlement_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-792", "b03_integrity_snapshot", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-793", "b03_replay_audit", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-794", "b03_schema_meta", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-795", "b03_transaction_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-796", "b03_migration_journal", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-797", "b03_draw_quarantine", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-798", "b03_prediction_bundle", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-799", "b03_number_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-800", "b03_action_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-801", "b03_permutation_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-802", "b03_selected_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-803", "b03_authorization_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-804", "b03_settlement_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-805", "b03_integrity_snapshot", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-806", "b03_replay_audit", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-807", "b03_schema_meta", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-808", "b03_transaction_audit", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-809", "b03_migration_journal", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-810", "b03_draw_quarantine", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-811", "b03_prediction_bundle", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-812", "b03_number_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-813", "b03_action_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-814", "b03_permutation_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-815", "b03_selected_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-816", "b03_authorization_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-817", "b03_settlement_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-818", "b03_integrity_snapshot", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-819", "b03_replay_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-820", "b03_schema_meta", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-821", "b03_transaction_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-822", "b03_migration_journal", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-823", "b03_draw_quarantine", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-824", "b03_prediction_bundle", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-825", "b03_number_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-826", "b03_action_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-827", "b03_permutation_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-828", "b03_selected_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-829", "b03_authorization_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-830", "b03_settlement_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-831", "b03_integrity_snapshot", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-832", "b03_replay_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-833", "b03_schema_meta", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-834", "b03_transaction_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-835", "b03_migration_journal", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-836", "b03_draw_quarantine", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-837", "b03_prediction_bundle", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-838", "b03_number_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-839", "b03_action_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-840", "b03_permutation_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-841", "b03_selected_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-842", "b03_authorization_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-843", "b03_settlement_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-844", "b03_integrity_snapshot", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-845", "b03_replay_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-846", "b03_schema_meta", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-847", "b03_transaction_audit", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-848", "b03_migration_journal", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-849", "b03_draw_quarantine", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-850", "b03_prediction_bundle", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-851", "b03_number_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-852", "b03_action_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-853", "b03_permutation_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-854", "b03_selected_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-855", "b03_authorization_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-856", "b03_settlement_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-857", "b03_integrity_snapshot", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-858", "b03_replay_audit", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-859", "b03_schema_meta", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-860", "b03_transaction_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-861", "b03_migration_journal", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-862", "b03_draw_quarantine", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-863", "b03_prediction_bundle", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-864", "b03_number_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-865", "b03_action_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-866", "b03_permutation_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-867", "b03_selected_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-868", "b03_authorization_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule("B03-INT-869", "b03_settlement_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-870", "b03_integrity_snapshot", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-871", "b03_replay_audit", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-872", "b03_schema_meta", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-873", "b03_transaction_audit", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-874", "b03_migration_journal", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-875", "b03_draw_quarantine", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-876", "b03_prediction_bundle", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-877", "b03_number_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-878", "b03_action_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-879", "b03_permutation_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-880", "b03_selected_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-881", "b03_authorization_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-882", "b03_settlement_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-883", "b03_integrity_snapshot", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-884", "b03_replay_audit", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule("B03-INT-885", "b03_schema_meta", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-886", "b03_transaction_audit", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-887", "b03_migration_journal", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-888", "b03_draw_quarantine", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-889", "b03_prediction_bundle", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-890", "b03_number_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-891", "b03_action_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-892", "b03_permutation_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-893", "b03_selected_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-894", "b03_authorization_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-895", "b03_settlement_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-896", "b03_integrity_snapshot", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-897", "b03_replay_audit", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-898", "b03_schema_meta", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-899", "b03_transaction_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-900", "b03_migration_journal", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-901", "b03_draw_quarantine", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-902", "b03_prediction_bundle", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-903", "b03_number_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-904", "b03_action_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-905", "b03_permutation_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule("B03-INT-906", "b03_selected_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-907", "b03_authorization_event", "HASH_PRESENT", 2, true));
        items.add(new IntegrityRule("B03-INT-908", "b03_settlement_event", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-909", "b03_integrity_snapshot", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-910", "b03_replay_audit", "ACTION_LEDGER_POLICY", 5, true));
        items.add(new IntegrityRule("B03-INT-911", "b03_schema_meta", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-912", "b03_transaction_audit", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-913", "b03_migration_journal", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-914", "b03_draw_quarantine", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-915", "b03_prediction_bundle", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-916", "b03_number_event", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-917", "b03_action_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-918", "b03_permutation_event", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-919", "b03_selected_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-920", "b03_authorization_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-921", "b03_settlement_event", "TABLE_EXISTS", 1, true));
        items.add(
                new IntegrityRule("B03-INT-922", "b03_integrity_snapshot", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-923", "b03_replay_audit", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-924", "b03_schema_meta", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-925", "b03_transaction_audit", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-926", "b03_migration_journal", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-927", "b03_draw_quarantine", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-928", "b03_prediction_bundle", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-929", "b03_number_event", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-930", "b03_action_event", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-931", "b03_permutation_event", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-932", "b03_selected_event", "COLUMN_SET", 2, true));
        items.add(
                new IntegrityRule("B03-INT-933", "b03_authorization_event", "UNIQUE_KEY", 3, true));
        items.add(new IntegrityRule("B03-INT-934", "b03_settlement_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-935", "b03_integrity_snapshot", "IMMUTABLE_TRIGGER", 5, true));
        items.add(new IntegrityRule("B03-INT-936", "b03_replay_audit", "GAME_ISOLATION", 1, true));
        items.add(new IntegrityRule("B03-INT-937", "b03_schema_meta", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-938", "b03_transaction_audit", "ORPHAN_FREE", 3, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-939", "b03_migration_journal", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-940", "b03_draw_quarantine", "ACTION_LEDGER_POLICY", 5, true));
        items.add(
                new IntegrityRule("B03-INT-941", "b03_prediction_bundle", "TABLE_EXISTS", 1, true));
        items.add(new IntegrityRule("B03-INT-942", "b03_number_event", "COLUMN_SET", 2, true));
        items.add(new IntegrityRule("B03-INT-943", "b03_action_event", "UNIQUE_KEY", 3, true));
        items.add(
                new IntegrityRule("B03-INT-944", "b03_permutation_event", "FOREIGN_KEY", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-945", "b03_selected_event", "IMMUTABLE_TRIGGER", 5, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-946", "b03_authorization_event", "GAME_ISOLATION", 1, true));
        items.add(
                new IntegrityRule("B03-INT-947", "b03_settlement_event", "HASH_PRESENT", 2, true));
        items.add(
                new IntegrityRule("B03-INT-948", "b03_integrity_snapshot", "ORPHAN_FREE", 3, true));
        items.add(new IntegrityRule("B03-INT-949", "b03_replay_audit", "REPLAY_STABLE", 4, true));
        items.add(
                new IntegrityRule(
                        "B03-INT-950", "b03_schema_meta", "ACTION_LEDGER_POLICY", 5, true));
        return Collections.unmodifiableList(items);
    }

    public static String integrityRuleHash() {
        StringBuilder b = new StringBuilder();
        List<IntegrityRule> x = integrityRules();
        for (int i = 0; i < x.size(); i++) {
            if (i > 0) b.append('|');
            b.append(x.get(i).canonical());
        }
        return V51ChecksumEngine.sha256(b.toString());
    }

    public static boolean selfTest() {
        return integrityRules().size() == 950
                && integrityRuleHash().length() == 64
                && DbHelper.b03TableNames().size() >= 13
                && DbHelper.b03TriggerNames().size() >= 16;
    }
}
