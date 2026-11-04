PRAGMA foreign_keys=ON;

CREATE TABLE IF NOT EXISTS task (
 task_id TEXT PRIMARY KEY, state TEXT NOT NULL DEFAULT 'READY',
 owner TEXT, fence INTEGER NOT NULL DEFAULT 0,
 boot_id TEXT, lease_until_ms INTEGER NOT NULL DEFAULT 0,
 cursor INTEGER NOT NULL DEFAULT 0, wait_kind TEXT,
 error_count INTEGER NOT NULL DEFAULT 0, error_start_ms INTEGER NOT NULL DEFAULT 0,
 quarantine_until_ms INTEGER NOT NULL DEFAULT 0,
 quarantine_rounds INTEGER NOT NULL DEFAULT 0,
 half_open INTEGER NOT NULL DEFAULT 0,
 CHECK(state IN ('READY','RUNNING','WAITING','QUARANTINED','NEEDS_FIX','STOPPED'))
);
CREATE TABLE IF NOT EXISTS task_commit (
 task_id TEXT NOT NULL, event_key TEXT NOT NULL, payload_hash TEXT NOT NULL,
 owner TEXT NOT NULL, fence INTEGER NOT NULL, cursor INTEGER NOT NULL,
 PRIMARY KEY(task_id,event_key), FOREIGN KEY(task_id) REFERENCES task(task_id)
);

CREATE TABLE IF NOT EXISTS forecast (
 forecast_id TEXT PRIMARY KEY, stream TEXT NOT NULL, issue TEXT NOT NULL,
 issue_ordinal INTEGER NOT NULL, model_hash TEXT NOT NULL,
 feature_max_ordinal INTEGER NOT NULL, position INTEGER NOT NULL,
 numbers_json TEXT NOT NULL, input_hash TEXT NOT NULL, prediction_hash TEXT NOT NULL,
 boot_id TEXT NOT NULL, deadline_elapsed_ms INTEGER NOT NULL,
 before_commit_elapsed_ms INTEGER NOT NULL,
 UNIQUE(stream,issue,model_hash),
 CHECK(feature_max_ordinal<issue_ordinal), CHECK(position BETWEEN 0 AND 9)
);
CREATE TRIGGER IF NOT EXISTS forecast_no_update BEFORE UPDATE ON forecast
 BEGIN SELECT RAISE(ABORT,'IMMUTABLE_FORECAST'); END;
CREATE TABLE IF NOT EXISTS forecast_commit_receipt (
 forecast_id TEXT PRIMARY KEY, after_commit_elapsed_ms INTEGER NOT NULL,
 boot_id TEXT NOT NULL, qualification TEXT NOT NULL,
 CHECK(qualification IN ('PREDRAW','LATE_OR_UNKNOWN')),
 FOREIGN KEY(forecast_id) REFERENCES forecast(forecast_id)
);
CREATE TRIGGER IF NOT EXISTS forecast_receipt_no_update BEFORE UPDATE ON forecast_commit_receipt
 BEGIN SELECT RAISE(ABORT,'IMMUTABLE_COMMIT_RECEIPT'); END;
CREATE TABLE IF NOT EXISTS reveal (
 stream TEXT NOT NULL, issue TEXT NOT NULL, issue_ordinal INTEGER NOT NULL,
 draw_hash TEXT NOT NULL, numbers_json TEXT NOT NULL,
 PRIMARY KEY(stream,issue)
);
CREATE TRIGGER IF NOT EXISTS reveal_no_update BEFORE UPDATE ON reveal
 BEGIN SELECT RAISE(ABORT,'REVEAL_CONFLICT_REQUIRES_NEW_REVISION'); END;
CREATE TABLE IF NOT EXISTS settlement (
 forecast_id TEXT PRIMARY KEY, draw_hash TEXT NOT NULL, hit INTEGER NOT NULL,
 FOREIGN KEY(forecast_id) REFERENCES forecast(forecast_id), CHECK(hit IN(0,1))
);
CREATE TABLE IF NOT EXISTS feedback_outbox (
 event_id TEXT PRIMARY KEY, forecast_id TEXT NOT NULL UNIQUE,
 draw_hash TEXT NOT NULL, hit INTEGER NOT NULL,
 FOREIGN KEY(forecast_id) REFERENCES forecast(forecast_id)
);
CREATE TABLE IF NOT EXISTS learning_applied (
 consumer_id TEXT NOT NULL, event_id TEXT NOT NULL, payload_hash TEXT NOT NULL,
 PRIMARY KEY(consumer_id,event_id), FOREIGN KEY(event_id) REFERENCES feedback_outbox(event_id)
);

CREATE TABLE IF NOT EXISTS budget_pool (
 pool TEXT PRIMARY KEY, cap_bytes INTEGER NOT NULL CHECK(cap_bytes>=0)
);
CREATE TABLE IF NOT EXISTS object_catalog (
 object_id TEXT PRIMARY KEY, pool TEXT NOT NULL,
 bytes INTEGER NOT NULL CHECK(bytes>=0), state TEXT NOT NULL DEFAULT 'LIVE',
 CHECK(state IN ('LIVE','DELETE_PENDING')),
 FOREIGN KEY(pool) REFERENCES budget_pool(pool)
);
CREATE TABLE IF NOT EXISTS object_edge (
 parent TEXT NOT NULL, child TEXT NOT NULL,
 PRIMARY KEY(parent,child),
 FOREIGN KEY(parent) REFERENCES object_catalog(object_id),
 FOREIGN KEY(child) REFERENCES object_catalog(object_id)
);
CREATE TABLE IF NOT EXISTS object_root (
 root_id TEXT PRIMARY KEY, object_id TEXT NOT NULL, kind TEXT NOT NULL,
 FOREIGN KEY(object_id) REFERENCES object_catalog(object_id)
);
CREATE TRIGGER IF NOT EXISTS root_live_insert BEFORE INSERT ON object_root
 WHEN (SELECT state FROM object_catalog WHERE object_id=NEW.object_id)<>'LIVE'
 BEGIN SELECT RAISE(ABORT,'ROOT_TO_PENDING_DELETE'); END;
CREATE TRIGGER IF NOT EXISTS root_live_update BEFORE UPDATE ON object_root
 WHEN (SELECT state FROM object_catalog WHERE object_id=NEW.object_id)<>'LIVE'
 BEGIN SELECT RAISE(ABORT,'ROOT_TO_PENDING_DELETE'); END;
CREATE TRIGGER IF NOT EXISTS edge_live_insert BEFORE INSERT ON object_edge
 WHEN (SELECT state FROM object_catalog WHERE object_id=NEW.parent)<>'LIVE'
 OR (SELECT state FROM object_catalog WHERE object_id=NEW.child)<>'LIVE'
 BEGIN SELECT RAISE(ABORT,'EDGE_TO_PENDING_DELETE'); END;
CREATE TRIGGER IF NOT EXISTS edge_live_update BEFORE UPDATE ON object_edge
 WHEN (SELECT state FROM object_catalog WHERE object_id=NEW.parent)<>'LIVE'
 OR (SELECT state FROM object_catalog WHERE object_id=NEW.child)<>'LIVE'
 BEGIN SELECT RAISE(ABORT,'EDGE_TO_PENDING_DELETE'); END;
CREATE TABLE IF NOT EXISTS budget_reservation (
 token TEXT PRIMARY KEY, pool TEXT NOT NULL, bytes INTEGER NOT NULL CHECK(bytes>=0),
 purpose TEXT NOT NULL,
 FOREIGN KEY(pool) REFERENCES budget_pool(pool)
);
CREATE TABLE IF NOT EXISTS gc_receipt (
 object_id TEXT PRIMARY KEY, bytes INTEGER NOT NULL, status TEXT NOT NULL,
 CHECK(status IN ('PLANNED','UNLINK_FAILED','UNLINKED'))
);
-- This connection-local work table is installed at owner/connection startup.
-- No CREATE/DROP/ALTER belongs in a business-loop write transaction.
CREATE TEMP TABLE IF NOT EXISTS gc_mark(object_id TEXT PRIMARY KEY,bytes INTEGER NOT NULL);
