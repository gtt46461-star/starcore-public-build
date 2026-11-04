-- R31 storage protocol executable design model, not an Android migration script.
-- Existing production schema requires versioned adapters and owner/fence integration.
PRAGMA foreign_keys=ON;
CREATE TABLE budget(singleton INTEGER PRIMARY KEY CHECK(singleton=1),base_bytes INTEGER NOT NULL CHECK(base_bytes>=0),cap_bytes INTEGER NOT NULL CHECK(cap_bytes>=0));
CREATE TABLE reservation(id TEXT PRIMARY KEY,owner TEXT NOT NULL,bytes INTEGER NOT NULL CHECK(bytes>=0),used INTEGER NOT NULL DEFAULT 0 CHECK(used>=0 AND used<=bytes));
CREATE TRIGGER reservation_capacity BEFORE INSERT ON reservation
WHEN NEW.bytes > (SELECT cap_bytes-base_bytes-COALESCE((SELECT sum(bytes) FROM reservation),0) FROM budget WHERE singleton=1)
BEGIN SELECT RAISE(ABORT,'STORAGE_BUDGET'); END;
CREATE TRIGGER reservation_identity_immutable BEFORE UPDATE OF id,owner,bytes ON reservation BEGIN SELECT RAISE(ABORT,'RESERVATION_IDENTITY_IMMUTABLE'); END;
CREATE TABLE artifact(hash TEXT PRIMARY KEY,bytes INTEGER NOT NULL CHECK(bytes>=0),state TEXT NOT NULL CHECK(state IN('STAGED','LIVE','DELETING','DELETED')),generation INTEGER NOT NULL DEFAULT 1);
CREATE TABLE root(id TEXT PRIMARY KEY,hash TEXT NOT NULL REFERENCES artifact(hash),state TEXT NOT NULL CHECK(state IN('ACTIVE','CLOSED')));
CREATE TABLE edge(parent TEXT NOT NULL REFERENCES artifact(hash),child TEXT NOT NULL REFERENCES artifact(hash),PRIMARY KEY(parent,child));
CREATE TRIGGER root_live BEFORE INSERT ON root WHEN NEW.state='ACTIVE' AND NOT EXISTS(SELECT 1 FROM artifact WHERE hash=NEW.hash AND state='LIVE') BEGIN SELECT RAISE(ABORT,'ROOT_TARGET_NOT_LIVE'); END;
CREATE TRIGGER root_live_update BEFORE UPDATE ON root WHEN NEW.state='ACTIVE' AND NOT EXISTS(SELECT 1 FROM artifact WHERE hash=NEW.hash AND state='LIVE') BEGIN SELECT RAISE(ABORT,'ROOT_TARGET_NOT_LIVE'); END;
CREATE TRIGGER edge_live BEFORE INSERT ON edge WHEN NOT EXISTS(SELECT 1 FROM artifact WHERE hash=NEW.parent AND state='LIVE') OR NOT EXISTS(SELECT 1 FROM artifact WHERE hash=NEW.child AND state='LIVE') BEGIN SELECT RAISE(ABORT,'EDGE_TARGET_NOT_LIVE'); END;
CREATE TRIGGER edge_immutable BEFORE UPDATE ON edge BEGIN SELECT RAISE(ABORT,'EDGE_IMMUTABLE'); END;
CREATE TABLE gc_action(id TEXT PRIMARY KEY,hash TEXT NOT NULL REFERENCES artifact(hash),generation INTEGER NOT NULL,phase TEXT NOT NULL CHECK(phase IN('CLAIMED','UNLINKED','COMMITTED')),UNIQUE(hash,generation));
