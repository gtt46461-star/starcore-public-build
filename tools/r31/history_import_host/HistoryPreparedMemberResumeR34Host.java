package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashSet;
import java.util.Set;

/** Resume a genuinely interrupted member publication. Count repeat writer entries on the native SQLite adapter. */
public final class HistoryPreparedMemberResumeR34Host {
    private static final class Reader extends SQLiteDatabase {
        final Set<String> savedPages = new HashSet<>();
        int repeatWriters, individualLookups;
        Reader(Connection connection) { super(connection); }
        @Override public Cursor rawQuery(String sql, String[] args) {
            if (!inTransaction() && (sql.startsWith("SELECT n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE issue=?")
                    || sql.startsWith("SELECT numbers,known_at FROM "))) individualLookups++;
            if (args != null && args.length == 1 && savedPages.contains(args[0])
                    && sql.contains("FROM r31_commit_receipt") && inTransaction()) repeatWriters++;
            return super.rawQuery(sql, args);
        }
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        File database = new File(args[1]).getCanonicalFile();
        HistoryPosixStatR31Host.install(args[2]);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             ReaderScope scope = new ReaderScope(connection, database)) {
            Reader db = scope.db;
            String job = HistoryImportPublicationR31Host.text(db, "SELECT job_id FROM r31_history_import_run WHERE status='STAGING'");
            String members = HistoryImportPublicationR31Host.text(db, "SELECT DISTINCT member_set FROM r31_source_member");
            if (job == null || members == null) throw new AssertionError("expected real interrupted preparation");
            String first = StatsR27.sha256(HistoryImportR27.R31_IMPORT_ABI + '|' + job + "|MEMBERS:" + members + "|1");
            if (HistoryImportPublicationR31Host.scalar(db,"SELECT count(*) FROM r31_commit_receipt WHERE commit_key=?",first) != 1)
                throw new AssertionError("missing atomic receipt for first page");
            db.savedPages.add(first);
            if ("wrong-member".equals(args[0])) {
                db.execSQL("UPDATE r31_source_member SET operation='REPLACE' WHERE member_set=? AND ordinal=(SELECT min(ordinal) FROM r31_source_member WHERE member_set=?)",new Object[]{members,members});
            } else if ("missing-member".equals(args[0])) {
                db.execSQL("DELETE FROM r31_source_member WHERE member_set=? AND ordinal=(SELECT min(ordinal) FROM r31_source_member WHERE member_set=?)",new Object[]{members,members});
            } else if ("wrong-payload".equals(args[0])) {
                db.execSQL("UPDATE r31_commit_receipt SET payload_hash=? WHERE commit_key=?",new Object[]{StatsR27.sha256("different-payload"),first});
            }
            boolean damaged = !"resume".equals(args[0]);
            try {
                HistoryImportR27.resumeSavedR31(scope.store, job, () -> false);
                if (damaged) throw new AssertionError("damaged prepared page was published");
            } catch (RuntimeException failure) {
                if (!damaged || !String.valueOf(failure).contains("PREPARED_MEMBER")) throw failure;
            }
            long complete = HistoryImportPublicationR31Host.scalar(db,"SELECT count(*) FROM r31_history_import_run WHERE status='COMPLETE' AND job_id=?",job);
            if (complete != (damaged ? 0 : 1)) throw new AssertionError("publication count " + complete);
            if (!damaged && (HistoryImportPublicationR31Host.published(db) != 67 || HistoryImportPublicationR31Host.rejects(db) != 1))
                throw new AssertionError("missing or duplicated published evidence");
            System.out.println("resume=" + args[0] + " complete=" + complete + " repeatWrites=" + db.repeatWriters + " individualLookups=" + db.individualLookups);
            if (db.repeatWriters != 0) throw new AssertionError("committed member page re-entered writer");
            if (db.individualLookups > 0) throw new AssertionError("reopened a SQLite cursor for individual periods");
        }
    }

    private static final class ReaderScope implements AutoCloseable {
        final Reader db;
        final R31OwnedRuntimeFixture owner;
        final DbStore store;
        ReaderScope(Connection connection, File database) throws Exception {
            db = new Reader(connection);
            owner = new R31OwnedRuntimeFixture(db, "HOST_HISTORY_PUBLICATION");
            store = new DbStore(owner.coordinator, db, new android.app.Application(database));
        }
        @Override public void close() throws Exception { owner.close(); }
    }
}
