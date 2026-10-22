-- Runtime bindings and durable burst cursor. All writes use the existing research owner.
CREATE TABLE IF NOT EXISTS su1_source_clock (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 generation INTEGER NOT NULL CHECK(generation>=0)
);
INSERT OR IGNORE INTO su1_source_clock VALUES(1,0);
CREATE TRIGGER IF NOT EXISTS su1_draw_insert_clock AFTER INSERT ON draws
BEGIN UPDATE su1_source_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_draw_update_clock AFTER UPDATE ON draws
BEGIN UPDATE su1_source_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_draw_delete_clock AFTER DELETE ON draws
BEGIN UPDATE su1_source_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TABLE IF NOT EXISTS su1_runtime_program (
 strategy_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_strategy_version(strategy_id),
 canonical_text TEXT NOT NULL CHECK(length(canonical_text) BETWEEN 3 AND 4096),
 coverage REAL NOT NULL CHECK(coverage>0 AND coverage<=1),
 margin_gate INTEGER NOT NULL CHECK(margin_gate IN (0,1)),
 parent_id TEXT REFERENCES su1_strategy_version(strategy_id),
 created_at INTEGER NOT NULL
);
CREATE TRIGGER IF NOT EXISTS su1_runtime_program_immutable BEFORE UPDATE ON su1_runtime_program
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_runtime_program_no_delete BEFORE DELETE ON su1_runtime_program
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TABLE IF NOT EXISTS su1_live_target (
 sequence_ordinal INTEGER PRIMARY KEY NOT NULL CHECK(sequence_ordinal>0),
 target_issue TEXT UNIQUE NOT NULL,
 target_open_time INTEGER NOT NULL,
 snapshot_hash TEXT NOT NULL REFERENCES su1_snapshot(snapshot_hash),
 legacy_settlement_hash TEXT,
 completed_at INTEGER,
 CHECK((legacy_settlement_hash IS NULL AND completed_at IS NULL) OR (length(legacy_settlement_hash)=64 AND completed_at>=target_open_time))
);
CREATE TRIGGER IF NOT EXISTS su1_live_target_update BEFORE UPDATE ON su1_live_target
BEGIN
 SELECT CASE WHEN OLD.completed_at IS NOT NULL OR NEW.completed_at IS NULL OR NEW.sequence_ordinal<>OLD.sequence_ordinal
 OR NEW.target_issue<>OLD.target_issue OR NEW.target_open_time<>OLD.target_open_time OR NEW.snapshot_hash<>OLD.snapshot_hash
 THEN RAISE(ABORT,'SU1_TARGET_IMMUTABLE') END;
END;
CREATE TABLE IF NOT EXISTS su1_live_attempt (
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 sequence_ordinal INTEGER NOT NULL REFERENCES su1_live_target(sequence_ordinal),
 outcome TEXT NOT NULL CHECK(outcome IN ('FROZEN','GATE_SKIP','BUDGET_SKIP','COMPUTE_FAILED')),
 detail TEXT NOT NULL,
 created_at INTEGER NOT NULL,
 PRIMARY KEY(strategy_id,sequence_ordinal)
) WITHOUT ROWID;
CREATE TRIGGER IF NOT EXISTS su1_live_attempt_immutable BEFORE UPDATE ON su1_live_attempt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TABLE IF NOT EXISTS su1_search_attempt (
 attempt_id TEXT PRIMARY KEY NOT NULL CHECK(length(attempt_id)=64),
 genome_key TEXT NOT NULL CHECK(length(genome_key)=64),
 campaign_id TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 program_text TEXT NOT NULL,
 arm TEXT NOT NULL,
 operator TEXT NOT NULL,
 parent_id TEXT,
 dev_fitness REAL,
 val_fitness REAL,
 result TEXT NOT NULL CHECK(result IN ('EVALUATED','REJECTED','YIELD')),
 detail TEXT NOT NULL,
 created_at INTEGER NOT NULL
);
CREATE TRIGGER IF NOT EXISTS su1_search_attempt_immutable BEFORE UPDATE ON su1_search_attempt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TABLE IF NOT EXISTS su1_burst_checkpoint (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 generation INTEGER NOT NULL CHECK(generation>=0),
 dataset_hash TEXT NOT NULL CHECK(length(dataset_hash)=64),
 cursor_json TEXT NOT NULL CHECK(length(CAST(cursor_json AS BLOB))<=65536),
 state_hash TEXT NOT NULL CHECK(length(state_hash)=64),
 updated_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_health_event (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 phase TEXT NOT NULL,
 detail TEXT NOT NULL,
 elapsed_micros INTEGER NOT NULL CHECK(elapsed_micros>=0),
 created_at INTEGER NOT NULL
);
