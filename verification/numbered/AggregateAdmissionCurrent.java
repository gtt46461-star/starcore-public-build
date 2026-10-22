package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/** Actual DAO preflight and unique writer on real SQLite, with explicit host Android adapters. */
public final class AggregateAdmissionCurrent {
    private static int checks;
    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }
    private static long count(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM \""+table+"\"", null)) {
            c.moveToFirst(); return c.getLong(0);
        }
    }
    private static String state(SQLiteDatabase db, String key) {
        try (Cursor c = db.rawQuery("SELECT state_value FROM app_state WHERE state_key=?", new String[]{key})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }
    private static long insert(Method method, DbStore store, SQLiteDatabase db, String table, ContentValues row) {
        try {
            return (Long)method.invoke(store, db, table, row, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) throw (RuntimeException)e.getCause();
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    public static void main(String[] args) throws Exception {
        SQLiteDatabase db = new SQLiteDatabase(args[0], args[1]);
        try {
            DbStore store = HostProductR974.store(db);
            java.lang.reflect.Field field = DbStore.class.getDeclaredField("coordinator");
            field.setAccessible(true);
            DatabaseCoordinator owner = (DatabaseCoordinator)field.get(store);
            Method method = DbStore.class.getDeclaredMethod("insertFiltered", SQLiteDatabase.class, String.class, ContentValues.class, int.class);
            method.setAccessible(true);
            int tables = 0;
            for (String line : Files.readAllLines(Path.of(args[2]))) {
                String[] spec = line.split(","); String table=spec[0], first=spec[1];
                ContentValues original = new ContentValues();
                try (Cursor c = db.rawQuery("SELECT * FROM \""+table+"\" ORDER BY rowid DESC LIMIT 1", null)) {
                    check(c.moveToFirst(), table+" fixture missing");
                    for (int i=0; i<c.getColumnCount(); i++) {
                        if (c.isNull(i)) original.putNull(c.getColumnName(i));
                        else original.put(c.getColumnName(i), c.getString(i));
                    }
                }
                check(count(db,table)==4096, table+" fixed cap fixture");
                RetentionAggregateCapacityCP12.checkAdmission(db,table,original);
                ContentValues proposed = new ContentValues(original);
                proposed.put(first, spec[2].equals("numeric") ? "4097" : "fixture-4097");
                try {
                    RetentionAggregateCapacityCP12.checkAdmission(db,table,proposed);
                    throw new AssertionError(table+" accepted new domain");
                } catch (RetentionAggregateCapacityCP12.Capacity expected) {
                    check(expected.table.equals(table) && expected.reason.equals("RETENTION_AGGREGATE_CAPACITY"),table+" typed cap");
                }
                ContentValues invalid = new ContentValues(original); invalid.putNull(first);
                try {
                    RetentionAggregateCapacityCP12.checkAdmission(db,table,invalid);
                    throw new AssertionError(table+" accepted null natural key");
                } catch (RetentionAggregateCapacityCP12.Capacity expected) {
                    check(expected.reason.equals("RETENTION_AGGREGATE_INVALID_KEY"),table+" typed invalid key");
                }
                boolean accepted = owner.runAtomicWrite("NUMBERED_AGGREGATE_BOUNDARY", x -> {
                    x.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('numbered_provisional','must rollback',1)");
                    insert(method,store,x,table,proposed);
                });
                check(!accepted,table+" writer fallback");
                check(state(db,"numbered_provisional")==null,table+" prior write rolled back");
                check(count(db,table)==4096,table+" failed DAO unchanged count");
                String blocker=state(db,"cp12_aggregate_blocker");
                check(blocker!=null && blocker.contains("RETENTION_AGGREGATE_CAPACITY") && blocker.contains(table),table+" durable typed blocker");
                check("1".equals(state(db,"cp4_retention_requested")),table+" retention requested");
                check("0".equals(state(db,"r979_retention_due_at")),table+" scheduler due");
                check(owner.runAtomicWrite("NUMBERED_AGGREGATE_EXISTING_KEY", x -> {
                    check(insert(method,store,x,table,original)>=0,table+" existing natural key update admitted");
                }),table+" existing key transaction committed");
                check(count(db,table)==4096,table+" update preserves denominator");
                tables++;
            }
            check(tables==37,"all 37 original aggregate tables");
            try (Cursor c=db.rawQuery("PRAGMA integrity_check",null)) {
                check(c.moveToFirst() && "ok".equals(c.getString(0)),"SQLite integrity");
            }
            System.out.println("PASS|tables="+tables+"|assertions="+checks+"|actual DAO, writer rollback, durable typed blocker and scheduler consumption");
        } finally { db.shutdown(); }
    }
}
