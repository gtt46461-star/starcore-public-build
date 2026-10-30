package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.ResearchRouteR29;

/** Formal REAL/probability receipts belong to Root alongside the original trade transaction.
 * These are the original R974/F05 DDL bytes, moved out of the research initialization dependency. */
public final class FormalReceiptSchemaR29 {
    private FormalReceiptSchemaR29(){}
    private static final String[] EXACT_REAL={
        "CREATE TABLE IF NOT EXISTS r974_exact_real_receipt(target_issue TEXT PRIMARY KEY,trade_id INTEGER NOT NULL,candidate_id TEXT NOT NULL,model_hash TEXT NOT NULL,certificate_hash TEXT NOT NULL,proof_hash TEXT NOT NULL,recipe_hash TEXT NOT NULL,action_hash TEXT NOT NULL,authorization_hash TEXT NOT NULL,committed_at INTEGER NOT NULL)",
        "CREATE TRIGGER IF NOT EXISTS r974_real_no_update BEFORE UPDATE ON r974_exact_real_receipt BEGIN SELECT RAISE(ABORT,'R974_REAL_RECEIPT_IMMUTABLE'); END"
    };
    private static final String[] EXACT_PROBABILITY={
        "CREATE TABLE IF NOT EXISTS research_exact_probability_f05(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,trade_id INTEGER NOT NULL UNIQUE REFERENCES trades(_id),candidate_id TEXT NOT NULL,model_hash TEXT NOT NULL CHECK(length(model_hash)=64),certificate_hash TEXT NOT NULL CHECK(length(certificate_hash)=64),calibration_hash TEXT NOT NULL CHECK(length(calibration_hash)=64),selection_hash TEXT NOT NULL CHECK(length(selection_hash)=64),scope_policy_hash TEXT NOT NULL CHECK(length(scope_policy_hash)=64),selected_hit_p REAL NOT NULL CHECK(selected_hit_p>0 AND selected_hit_p<1),proof_hash TEXT NOT NULL CHECK(length(proof_hash)=64),action_hash TEXT NOT NULL CHECK(length(action_hash)=64),authorization_hash TEXT NOT NULL,captured_at INTEGER NOT NULL,committed_at INTEGER NOT NULL CHECK(committed_at>=captured_at),PRIMARY KEY(game_code,target_issue))",
        "CREATE TRIGGER IF NOT EXISTS f05_exact_probability_no_update BEFORE UPDATE ON research_exact_probability_f05 BEGIN SELECT RAISE(ABORT,'F05_EXACT_PROBABILITY_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS f05_exact_probability_no_replace BEFORE INSERT ON research_exact_probability_f05 WHEN EXISTS(SELECT 1 FROM research_exact_probability_f05 e WHERE e.game_code=NEW.game_code AND e.target_issue=NEW.target_issue) BEGIN SELECT RAISE(ABORT,'F05_EXACT_PROBABILITY_FIRST_WRITE'); END"
    };
    public static void ensureRoot(SQLiteDatabase db){
        if(ResearchRouteR29.isResearchDatabase(db))throw new IllegalStateException("R29_FORMAL_RECEIPT_REQUIRES_ROOT");
        for(String sql:EXACT_REAL)db.execSQL(sql);
        for(String sql:EXACT_PROBABILITY)db.execSQL(sql);
    }
    public static void ensureExactRealIfRoot(SQLiteDatabase db){
        if(!ResearchRouteR29.isResearchDatabase(db))for(String sql:EXACT_REAL)db.execSQL(sql);
    }
    public static void ensureExactProbabilityIfRoot(SQLiteDatabase db){
        if(!ResearchRouteR29.isResearchDatabase(db))for(String sql:EXACT_PROBABILITY)db.execSQL(sql);
    }
}
