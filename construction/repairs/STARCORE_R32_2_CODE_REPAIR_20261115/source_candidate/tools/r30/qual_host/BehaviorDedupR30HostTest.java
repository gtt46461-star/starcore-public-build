package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

/** Actual production sink + report on SQLite: retry receipts are idempotent and report counts are committed rows. */
public final class BehaviorDedupR30HostTest {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SQLiteDatabase db = new SQLiteDatabase(c);
            Map<String,Object> missing = ReportR29.behaviorDuplicates(db);
            if (!"INSUFFICIENT".equals(missing.get("status")) || missing.get("rows") != null) throw new AssertionError("MISSING_TABLE_FAKED_ZERO");
            try (Statement s = c.createStatement()) { s.execute(BehaviorSignatureR30.DDL); }
            BehaviorSignatureR30.Probes probes = new BehaviorSignatureR30.Probes("dataset", "probes", new int[0]);
            BehaviorSignatureR30.Duplicate archive = new BehaviorSignatureR30.Duplicate(probes, "signature", "kept", "dropped", "ARCHIVE");
            StoreR27.EvolutionSink sink = new StoreR27.EvolutionSink("MAIN", "fixture");
            sink.behaviorDuplicate(archive); sink.behaviorDuplicate(archive);
            sink.behaviorDuplicate(new BehaviorSignatureR30.Duplicate(probes, "signature", "kept", "dropped", "MIGRATION"));
            c.setAutoCommit(false); ResearchWriteQueueR27.execute(db, sink.batch, "BEHAVIOR_TEST"); c.commit(); c.setAutoCommit(true);
            long actual;
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT count(*) FROM r30_behavior_dup")) { r.next(); actual = r.getLong(1); }
            Map<String,Object> report = ReportR29.behaviorDuplicates(db);
            if (actual != 2 || ((Number)report.get("rows")).longValue() != actual) throw new AssertionError("REPORT_NOT_COMMITTED_ROWS:" + report);
            @SuppressWarnings("unchecked") Map<String,Object> stages = (Map<String,Object>) report.get("byStage");
            if (((Number)stages.get("ARCHIVE")).longValue() != 1 || ((Number)stages.get("MIGRATION")).longValue() != 1) throw new AssertionError("STAGES_DOUBLE_COUNTED");
            System.out.println("R30_BEHAVIOR_SQLITE|RETRY_IDEMPOTENT|REPORT_ROWS_MATCH|ARCHIVE_1|MIGRATION_1|PASS");
        }
    }
}
