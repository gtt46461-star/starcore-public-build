package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

public final class PairWindowTest {
    private static Map<?, ?> inspect(SQLiteDatabase db) {
        Object rows = DiagnosisEvidenceR335.paired(db, 104).get("rows");
        return (Map<?, ?>) ((List<?>) rows).get(0);
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }

    private static void invalid(SQLiteDatabase db, String sql, String reason) {
        db.beginTransaction();
        try {
            db.execSQL(sql);
            Map<?, ?> row = inspect(db);
            require("UNKNOWN_INVALID_PAIR_ORIGINAL".equals(row.get("status")), reason);
            require(row.get("pairedOpportunities") == null, "ineligible measurement: " + reason);
        } finally {
            db.endTransaction(); // roll back the fixture
        }
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SQLiteDatabase db = new SQLiteDatabase(conn);
            db.execSQL("CREATE TABLE su2_lineage(child_key TEXT,parent_key TEXT,campaign_id TEXT)");
            db.execSQL("CREATE TABLE su2_model_genome(genome_key TEXT,born_sequence INTEGER)");
            db.execSQL("CREATE TABLE su2_live_decision(genome_key TEXT,sequence_ordinal INTEGER,"
                    + "act INTEGER,hit INTEGER,position INTEGER,number_mask INTEGER,"
                    + "frozen_at INTEGER,target_open_time INTEGER,settled_at INTEGER)");
            db.execSQL("INSERT INTO su2_lineage VALUES('C','P','fixture')");
            db.execSQL("INSERT INTO su2_model_genome VALUES('C',100)");
            db.execSQL("INSERT INTO su2_live_decision VALUES"
                    + "('C',100,1,1,1,7,8,10,11),('P',100,1,0,1,7,9,10,12),"
                    + "('C',101,0,NULL,1,7,18,20,NULL),('P',101,1,1,1,7,19,20,20),"
                    + "('C',102,1,1,1,7,28,30,31)");
            Map<?, ?> row = inspect(db);
            require(Long.valueOf(4).equals(row.get("lineageWindowOrdinals")), "local ordinals");
            require(Long.valueOf(1).equals(row.get("pairedOpportunities")), "settled pairs");
            require(Long.valueOf(1).equals(row.get("ordinalsPendingSettlement")), "pending");
            require(Long.valueOf(2).equals(row.get("ordinalsMissingOneOrBothFreezes")), "missing");
            require(Double.valueOf(1.0).equals(row.get("pairedMeanOpportunityHitDifference")), "observed difference");
            require(row.get("independentEnabledDenominator") == null, "natural denominator");
            String original = "100\t1\t1\t1\t0\t1\t7\t1\t7\t8\t9\t10\t11\t12\n";
            require(HashingSU1.text(original).equals(row.get("pairedOriginalSha256")), "original hash semantics");
            invalid(db,"UPDATE su2_live_decision SET settled_at=9 WHERE sequence_ordinal=100","settlement before opening");
            invalid(db,"UPDATE su2_live_decision SET frozen_at=0 WHERE sequence_ordinal=102","zero on unmatched arm");
            invalid(db,"UPDATE su2_live_decision SET target_open_time=11 WHERE genome_key='P' AND sequence_ordinal=100","different opens");
            invalid(db,"INSERT INTO su2_live_decision SELECT * FROM su2_live_decision LIMIT 1","duplicate");
            invalid(db,"UPDATE su2_live_decision SET settled_at=22 WHERE hit IS NULL","missing label with settlement");
            db.execSQL("DELETE FROM su2_live_decision");
            row = inspect(db);
            require("INSUFFICIENT".equals(row.get("status")) && row.get("pairedMeanOpportunityHitDifference")==null,
                    "empty window");
            System.out.println("PASS: B05 complete owner compiled; original paired() executed using JDBC-backed SQLite");
        }
    }
}
