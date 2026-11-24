package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.LifecycleSchemaR31;

/** Installed only by LifecycleSchemaR31's existing MIGRATION owner. */
public final class ColdRebirthSchemaR34 {
    private ColdRebirthSchemaR34() {}
    public static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS r34_cold_rebirth(source_kind TEXT NOT NULL CHECK(source_kind IN ('CANDIDATE','EXPERT')),source_campaign TEXT NOT NULL,source_id TEXT NOT NULL,source_key TEXT NOT NULL CHECK(length(source_key)=64),source_state_hash TEXT NOT NULL,source_checkpoint TEXT NOT NULL,source_cursor INTEGER NOT NULL CHECK(source_cursor>=0),source_abi TEXT NOT NULL,target_abi TEXT NOT NULL CHECK(length(target_abi)=64),child_campaign TEXT NOT NULL UNIQUE,child_id TEXT NOT NULL,snapshot_hash TEXT NOT NULL CHECK(length(snapshot_hash)=64),source_first INTEGER NOT NULL,source_through INTEGER NOT NULL CHECK(source_through>=source_first),birth_kind TEXT NOT NULL CHECK(birth_kind='R34_COLD_RETRAIN_ONLY_1'),created_at INTEGER NOT NULL,PRIMARY KEY(source_kind,source_campaign,source_id,target_abi)) WITHOUT ROWID",
        "CREATE TABLE IF NOT EXISTS r34_cold_rebirth_retry(source_kind TEXT NOT NULL,source_campaign TEXT NOT NULL,source_id TEXT NOT NULL,target_abi TEXT NOT NULL,attempts INTEGER NOT NULL CHECK(attempts>=1),next_at INTEGER NOT NULL,reason TEXT NOT NULL CHECK(length(reason)<=512),updated_at INTEGER NOT NULL,PRIMARY KEY(source_kind,source_campaign,source_id,target_abi)) WITHOUT ROWID",
        "CREATE TRIGGER IF NOT EXISTS r34_cold_rebirth_immutable_insert BEFORE INSERT ON r34_cold_rebirth WHEN EXISTS(SELECT 1 FROM r34_cold_rebirth b WHERE (b.source_kind=NEW.source_kind AND b.source_campaign=NEW.source_campaign AND b.source_id=NEW.source_id AND b.target_abi=NEW.target_abi) OR b.child_campaign=NEW.child_campaign) BEGIN SELECT RAISE(ABORT,'R34_REBIRTH_RECEIPT_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r34_cold_rebirth_immutable_update BEFORE UPDATE ON r34_cold_rebirth BEGIN SELECT RAISE(ABORT,'R34_REBIRTH_RECEIPT_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r34_cold_rebirth_immutable_delete BEFORE DELETE ON r34_cold_rebirth BEGIN SELECT RAISE(ABORT,'R34_REBIRTH_RECEIPT_IMMUTABLE'); END",
        "CREATE INDEX IF NOT EXISTS r34_cold_candidate_lookup ON r26_candidate(status,campaign_id,policy_hash) WHERE status='BLOCKED_WARM_NUMERICAL_ABI_PRESERVED'",
        "CREATE INDEX IF NOT EXISTS r34_cold_expert_lookup ON r26_expert_experiment(status,created_at,experiment_id) WHERE status='BLOCKED_EXPERT_ABI_PRESERVED'"
    };
    public static boolean declares(String name) {
        return name.equals("r34_cold_rebirth") || name.equals("r34_cold_rebirth_retry")
                || name.equals("r34_cold_rebirth_immutable_insert") || name.equals("r34_cold_rebirth_immutable_update") || name.equals("r34_cold_rebirth_immutable_delete")
                || name.equals("r34_cold_candidate_lookup") || name.equals("r34_cold_expert_lookup");
    }
    public static void require(SQLiteDatabase db) {
        DatabaseCoordinator.requireSchemaObjectsR31(db,"R34_COLD_REBIRTH",new String[][]{
            {"r34_cold_rebirth","source_kind,source_campaign,source_id,source_key,target_abi,child_campaign,child_id"},
            {"r34_cold_rebirth_retry","source_kind,source_campaign,source_id,target_abi,attempts,next_at,reason,updated_at"}
        },new String[][]{{"trigger","r34_cold_rebirth_immutable_insert"},{"trigger","r34_cold_rebirth_immutable_update"},{"trigger","r34_cold_rebirth_immutable_delete"},
            {"index","r34_cold_candidate_lookup"},{"index","r34_cold_expert_lookup"}});
        LifecycleSchemaR31.requireDefinitions(db, DDL, "R34_COLD_REBIRTH");
    }
}
