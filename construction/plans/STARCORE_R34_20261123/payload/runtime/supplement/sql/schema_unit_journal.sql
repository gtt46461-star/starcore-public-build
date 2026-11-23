-- Candidate R34 journal. Install through the existing versioned SchemaDomain owner.
-- Never execute this file as an unregistered alternate migration connection.
CREATE TABLE IF NOT EXISTS r34_schema_unit_run (
  run_id TEXT PRIMARY KEY,
  database_id TEXT NOT NULL,
  migration_id TEXT NOT NULL,
  plan_hash TEXT NOT NULL CHECK(length(plan_hash)=64),
  schema_revision INTEGER NOT NULL CHECK(schema_revision>0),
  definition_revision TEXT NOT NULL,
  table_count INTEGER NOT NULL CHECK(table_count>0),
  phase TEXT NOT NULL CHECK(phase IN ('INSTALL','VERIFY','DONE')),
  next_index INTEGER NOT NULL CHECK(next_index>=0 AND next_index<=table_count),
  revision INTEGER NOT NULL CHECK(revision>=0),
  owner_epoch TEXT NOT NULL,
  owner_fence INTEGER NOT NULL CHECK(owner_fence>0),
  audit_round INTEGER NOT NULL CHECK(audit_round>=0),
  audit_schema_cookie INTEGER NOT NULL DEFAULT -1 CHECK(audit_schema_cookie>=-1),
  last_commit_key TEXT NOT NULL DEFAULT '',
  updated_at INTEGER NOT NULL,
  CHECK((phase='DONE' AND next_index=table_count AND audit_schema_cookie>=0)
    OR (phase<>'DONE' AND next_index<table_count))
);
CREATE TABLE IF NOT EXISTS r34_schema_unit_receipt (
  commit_key TEXT PRIMARY KEY CHECK(length(commit_key)=64),
  run_id TEXT NOT NULL,
  plan_hash TEXT NOT NULL CHECK(length(plan_hash)=64),
  phase TEXT NOT NULL CHECK(phase IN ('INSTALL','VERIFY')),
  unit_index INTEGER NOT NULL CHECK(unit_index>=0),
  source_table TEXT NOT NULL,
  audit_round INTEGER NOT NULL CHECK(audit_round>=0),
  cursor_before INTEGER NOT NULL CHECK(cursor_before>=0),
  cursor_after INTEGER NOT NULL CHECK(cursor_after=cursor_before+1),
  owner_epoch TEXT NOT NULL,
  owner_fence INTEGER NOT NULL CHECK(owner_fence>0),
  payload_hash TEXT NOT NULL CHECK(length(payload_hash)=64),
  definition_hash TEXT NOT NULL CHECK(length(definition_hash)=64),
  original_root TEXT NOT NULL CHECK(length(original_root)=64),
  schema_cookie_before INTEGER NOT NULL,
  schema_cookie_after INTEGER NOT NULL,
  committed_at INTEGER NOT NULL,
  UNIQUE(run_id,phase,unit_index,audit_round),
  FOREIGN KEY(run_id) REFERENCES r34_schema_unit_run(run_id)
);
CREATE TRIGGER IF NOT EXISTS r34_schema_unit_receipt_no_update
BEFORE UPDATE ON r34_schema_unit_receipt BEGIN
  SELECT RAISE(ABORT,'R34_SCHEMA_RECEIPT_IMMUTABLE');
END;
CREATE TRIGGER IF NOT EXISTS r34_schema_unit_receipt_no_delete
BEFORE DELETE ON r34_schema_unit_receipt BEGIN
  SELECT RAISE(ABORT,'R34_SCHEMA_RECEIPT_RETENTION_REQUIRES_OWNER_PROTOCOL');
END;
CREATE TRIGGER IF NOT EXISTS r34_schema_unit_receipt_no_replace
BEFORE INSERT ON r34_schema_unit_receipt
WHEN EXISTS(SELECT 1 FROM r34_schema_unit_receipt WHERE commit_key=NEW.commit_key
 OR (run_id=NEW.run_id AND phase=NEW.phase AND unit_index=NEW.unit_index AND audit_round=NEW.audit_round))
BEGIN SELECT RAISE(ABORT,'R34_SCHEMA_RECEIPT_DUPLICATE_REQUIRE_READBACK'); END;
