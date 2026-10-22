package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Method;

/** Exercises the production bridge against SQLite; fixtures cannot authorize production. */
public final class R931BridgeRegressionHarness {
    private static void check(boolean condition, String code) {
        if (!condition) throw new AssertionError(code);
        System.out.println(code + "=PASS");
    }
    private static int count(SQLiteDatabase db, String table) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }
    public static void main(String[] args) throws Exception {
        SQLiteDatabase db = new SQLiteDatabase(args[0]);
        try {
            DbStore store = new DbStore(db);
            Batch34SchemaR930.ensure(db);
            db.execSQL("CREATE TABLE " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                    + "(genome_id TEXT PRIMARY KEY,display_name_zh TEXT,structural_hash TEXT,"
                    + "future_evidence_hash TEXT,future_n INTEGER,future_hit_rate REAL,"
                    + "future_wilson_lower REAL,future_block_min REAL,future_regime_min REAL,"
                    + "future_coverage REAL,future_max_miss INTEGER,future_brier REAL,future_ece REAL,"
                    + "paired_mainline_n INTEGER,paired_mainline_delta REAL,future_fdr_q REAL,"
                    + "trained_until_issue TEXT,status TEXT,may_authorize_real INTEGER,updated_at INTEGER)");
            db.execSQL("CREATE TABLE " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW
                    + "(genome_id TEXT,target_issue TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,"
                    + "selected_ordinal INTEGER,score REAL,feature_hash TEXT,record_hash TEXT,hit_label INTEGER)");
            db.execSQL("CREATE TABLE " + DbHelper.TABLE_R35_OOS_LABEL
                    + "(target_issue TEXT PRIMARY KEY,strict_oos INTEGER,hit_label INTEGER)");
            db.execSQL("CREATE TABLE " + DbHelper.TABLE_DRAWS + "(issue TEXT PRIMARY KEY)");
            String hash = V51ChecksumEngine.sha256("fixture");
            db.execSQL("INSERT INTO " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                    + " VALUES('g1','fixture',?,?,1200,.45,.40,.30,.25,.80,3,.2,.02,1200,.05,.01,"
                    + "'202609090100','OBSERVING_FUTURE_OOS',0,1)", new Object[]{hash,hash});
            // A large pruned population must not hide an admissible candidate behind LIMIT 16.
            for (int blocked = 0; blocked < 20; blocked++) db.execSQL("INSERT INTO " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                    + " VALUES(?, 'pruned',?,?,9000,.60,.55,.40,.40,.90,3,.2,.02,9000,.1,.01,"
                    + "'202609090100','PRUNED_EVIDENCE_R912',0,2)", new Object[]{"pruned"+blocked,hash,hash});
            Batch34ChallengerCertificateBridgeR930.Summary first =
                    Batch34ChallengerCertificateBridgeR930.sync(store,"202609100100");
            check(first.admitted == 1 && first.freshSettled == 0, "HISTORICAL_SUPPORT_NOT_INHERITED");
            int admissionRows = count(db,Batch34SchemaR930.TABLE_EVIDENCE_LINK);
            Batch34ChallengerCertificateBridgeR930.Summary repeat =
                    Batch34ChallengerCertificateBridgeR930.sync(store,"202609100101");
            check(repeat.admitted == 0 && count(db,Batch34SchemaR930.TABLE_EVIDENCE_LINK)==admissionRows,
                    "REPEAT_ADMISSION_PRESERVES_ANCHOR_AND_EVIDENCE");
            db.execSQL("INSERT INTO " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW
                    + " VALUES('g1','202609100101',1,1,2,3,0,.99,?,?,1)", new Object[]{hash,hash});
            CandidateExecutionAuditR932.schema(db);
            db.execSQL("INSERT INTO " + CandidateExecutionAuditR932.TABLE
                    + " VALUES('algorithm:g1','202609100101',?,?,?,?,?,?,?,?,?,1200,0.0,1,'fixture',?,1)",
                    new Object[]{hash,hash,"202609100100",hash,hash,hash,hash,hash,hash,hash});
            db.execSQL("INSERT INTO " + DbHelper.TABLE_DRAWS + " VALUES('202609100101')");
            Method metrics = Batch34ChallengerCertificateBridgeR930.class.getDeclaredMethod(
                    "freshMetrics",DbStore.class,String.class,String.class,String.class);
            metrics.setAccessible(true);
            Object observed = metrics.invoke(null,store,"g1","202609100100","202609100102");
            for(String name:new String[]{"brier","ece"}) {
                Method metric=observed.getClass().getDeclaredMethod(name);metric.setAccessible(true);
                check(Double.isNaN(((Double)metric.invoke(observed)).doubleValue()),
                        "RAW_SCORE_NOT_"+name.toUpperCase(java.util.Locale.US)+"_PROBABILITY");
            }
            Batch34ChallengerCertificateBridgeR930.Summary settled =
                    Batch34ChallengerCertificateBridgeR930.sync(store,"202609100102");
            check(settled.freshSettled==1 && settled.controlledInfluenceEligible==0,
                    "FRESH_LABELS_REMAIN_OBSERVATION_WITHOUT_CALIBRATION");
            Batch34ChallengerCertificateBridgeR930.sync(store,"202609100101");
            Cursor late=db.rawQuery("SELECT COUNT(*) FROM "+Batch34SchemaR930.TABLE_EVIDENCE_LINK
                    +" WHERE evidence_type='LEGACY_PREDRAW_OBSERVATION'",null);
            try {check(late.moveToFirst()&&late.getInt(0)==0,"POSTDRAW_MIRROR_CANNOT_CREATE_PREDRAW_EVIDENCE");}
            finally {late.close();}
            check(!Batch34ChallengerCertificateBridgeR930.MAY_AUTHORIZE_REAL,"BRIDGE_HAS_NO_REAL_AUTHORITY");
            System.out.println("R931_BRIDGE_REGRESSION_PASS");
        } finally { db.shutdown(); }
    }
}
