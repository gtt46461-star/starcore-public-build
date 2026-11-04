package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.Connection;
import java.sql.DriverManager;

/** Actual JDBC boundary behavior, never an Android runtime or scheduling receipt. */
public final class R31SqliteHostBoundaryContract {
    private static int checks;
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); checks++; }
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SQLiteDatabase db = new SQLiteDatabase(connection);
            try (Cursor empty = db.rawQuery("SELECT 1 AS first,NULL AS second WHERE 0", null)) {
                require(empty.getColumnCount() == 2, "empty query lost column metadata");
                require(empty.getCount() == 0, "empty query invented rows");
            }
            boolean rejected = false;
            try (Cursor ignored = db.rawQuery("SELECT ? IS NULL", new String[] {null})) {
                throw new AssertionError("HOST accepted Android-forbidden null selection argument");
            } catch (IllegalArgumentException expected) { rejected = true; }
            require(rejected, "null selection argument was not rejected");
            try (Cursor row = db.rawQuery("SELECT NULL,?", new String[] {"actual"})) {
                require(row.moveToFirst() && row.isNull(0) && "actual".equals(row.getString(1)), "query result/null handling changed");
            }
            require(android.os.Process.myPid() == ProcessHandle.current().pid(), "PID adapter is not this JVM PID");
        }
        System.out.println("R31_SQLITE_HOST_BOUNDARY_PASS assertions=" + checks + " nullSelectionArgumentRejected=true emptyCursorMetadata=true actualJvmPid=true androidScheduling=NOT_RUN");
    }
}
