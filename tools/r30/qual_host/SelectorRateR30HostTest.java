package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** Actual live registration binds the pre-registered selector rate and preserves existing qualification state. */
public final class SelectorRateR30HostTest {
    static PolicySpecR27 spec(int position, double rate) {
        return new PolicySpecR27(PolicyAstR27.leaf("freq", position, 32), position, PolicySpecR27.Rule.MARGIN,
                0.02, rate, 1, false, 0.01, 0.001, IdeaFamilyR27.meta("I13", "M_SELECTOR", 1, 1, "H_SELECT fixture"));
    }
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER NOT NULL)");
                s.execute("INSERT INTO draws VALUES('202610310001',0)");
                for (String table : new String[] {"r27_live_shadow", "r27_eprocess", "r27_policy"})
                    for (String ddl : SchemaR27.DDL) if (ddl.startsWith("CREATE TABLE IF NOT EXISTS " + table + "(")) s.execute(ddl);
                s.execute(QualificationInboxR29.DDL[5]);
            }
            SQLiteDatabase db = new SQLiteDatabase(c); PolicySpecR27 fitted = spec(2, 0.23), legacy = spec(3, 0);
            c.setAutoCommit(false);
            StoreR27.insertShadow(db, fitted, 1, "fixture", "abi", fitted.policyHash(), fitted.policyHash());
            StoreR27.insertShadow(db, legacy, 1, "fixture", "abi", legacy.policyHash(), legacy.policyHash());
            c.commit(); c.setAutoCommit(true);
            SequentialQualificationR27.State state = StoreR27.readEprocess(db, fitted.policyHash());
            if (Double.doubleToLongBits(state.delta) != Double.doubleToLongBits(SequentialQualificationR27.deltaMin(0.23))) throw new AssertionError("REGISTERED_DELTA_CHANGED");
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT target_rate FROM r27_eprocess WHERE policy_hash='" + legacy.policyHash() + "'")) {
                r.next(); if (r.getDouble(1) != 0.15) throw new AssertionError("LEGACY_MARGIN_FALLBACK_CHANGED");
            }
            c.setAutoCommit(false); StoreR27.insertShadow(db, fitted, 1, "retry", "abi", fitted.policyHash(), fitted.policyHash()); c.commit(); c.setAutoCommit(true);
            SequentialQualificationR27.State retried = StoreR27.readEprocess(db, fitted.policyHash());
            if (retried.globalIndex != state.globalIndex || retried.delta != state.delta) throw new AssertionError("RETRY_RESET_QUALIFICATION");
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT count(*) FROM r27_eprocess")) { r.next(); if (r.getLong(1) != 2) throw new AssertionError("DUPLICATE_REGISTRATION"); }
            System.out.println("R30_SELECTOR_SQLITE|REGISTERED_RATE_TO_DELTA|LEGACY_FALLBACK_PRESERVED|RETRY_NO_RESET|PASS");
        }
    }
}
