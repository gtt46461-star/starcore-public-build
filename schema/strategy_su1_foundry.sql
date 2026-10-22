-- SU1 FOUNDRY V1 (R12) — additive on top of schema/strategy_su1.sql (requires su1_blob,
-- su1_strategy_version). Research-track storage for the dual-track loop:
--   history replay campaigns + every pool program's live shadow freeze/settle each period.
-- Certificate-grade evidence stays in su1_prediction/su1_settlement/su1_evidence_look;
-- the live shadow ledger here is compact research data and may be compacted into daily
-- aggregates. Same rules as the main DDL: one writer, complete statements, no REPLACE.

CREATE TABLE IF NOT EXISTS su1_campaign (
 campaign_id TEXT NOT NULL PRIMARY KEY CHECK(length(campaign_id)=64),
 kind TEXT NOT NULL CHECK(kind IN ('HISTORY_REPLAY','LIVE_REFRESH')),
 data_through_sequence INTEGER NOT NULL CHECK(data_through_sequence>0),
 config_hash TEXT NOT NULL CHECK(length(config_hash)=64),
 config_json TEXT NOT NULL CHECK(length(CAST(config_json AS BLOB))<=65536),
 started_at INTEGER NOT NULL,
 finished_at INTEGER,
 status TEXT NOT NULL CHECK(status IN ('RUNNING','DONE','FAILED')),
 programs_evaluated INTEGER NOT NULL DEFAULT 0 CHECK(programs_evaluated>=0),
 CHECK((status='RUNNING' AND finished_at IS NULL) OR (status<>'RUNNING' AND finished_at>=started_at))
);
CREATE TABLE IF NOT EXISTS su1_era (
 era_id TEXT NOT NULL PRIMARY KEY CHECK(length(era_id)=64),
 campaign_id TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 kind TEXT NOT NULL CHECK(kind IN ('DEV','VAL','SEALED')),
 first_sequence INTEGER NOT NULL,
 last_sequence INTEGER NOT NULL,
 UNIQUE(campaign_id,kind),
 CHECK(first_sequence<=last_sequence)
);
-- A SEALED era is opened exactly once, for one pre-chosen champion.
CREATE TABLE IF NOT EXISTS su1_sealed_open (
 era_id TEXT NOT NULL PRIMARY KEY REFERENCES su1_era(era_id),
 campaign_id TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 opened_at INTEGER NOT NULL,
 result_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash)
);
CREATE TABLE IF NOT EXISTS su1_research_pool (
 entry_id TEXT NOT NULL PRIMARY KEY CHECK(length(entry_id)=64),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 campaign_id TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 arm TEXT NOT NULL,
 born_sequence INTEGER NOT NULL CHECK(born_sequence>0),
 prior_fitness REAL NOT NULL,
 state TEXT NOT NULL CHECK(state IN ('ACTIVE','RETIRED')),
 retired_sequence INTEGER,
 retire_reason TEXT,
 created_at INTEGER NOT NULL,
 CHECK((state='ACTIVE' AND retired_sequence IS NULL AND retire_reason IS NULL) OR
       (state='RETIRED' AND retired_sequence>=born_sequence AND retire_reason IS NOT NULL))
);
CREATE UNIQUE INDEX IF NOT EXISTS su1_pool_one_active ON su1_research_pool(strategy_id) WHERE state='ACTIVE';
-- Compact live shadow ledger: one row per (program, period) where the program decided to act.
CREATE TABLE IF NOT EXISTS su1_live_shadow (
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 sequence_ordinal INTEGER NOT NULL CHECK(sequence_ordinal>0),
 day_key INTEGER NOT NULL CHECK(day_key BETWEEN 20200101 AND 21001231),
 position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),
 number_mask INTEGER NOT NULL CHECK(number_mask BETWEEN 7 AND 896),
 captured_at INTEGER NOT NULL,
 target_open_time INTEGER NOT NULL,
 hit INTEGER CHECK(hit IN (0,1)),
 settled_at INTEGER,
 PRIMARY KEY(strategy_id,sequence_ordinal),
 CHECK(captured_at<target_open_time),
 CHECK(((number_mask>>0)&1)+((number_mask>>1)&1)+((number_mask>>2)&1)+((number_mask>>3)&1)+((number_mask>>4)&1)
      +((number_mask>>5)&1)+((number_mask>>6)&1)+((number_mask>>7)&1)+((number_mask>>8)&1)+((number_mask>>9)&1)=3),
 CHECK((hit IS NULL AND settled_at IS NULL) OR (hit IS NOT NULL AND settled_at>=target_open_time))
) WITHOUT ROWID;
CREATE INDEX IF NOT EXISTS su1_live_shadow_pending ON su1_live_shadow(sequence_ordinal) WHERE hit IS NULL;
CREATE TABLE IF NOT EXISTS su1_live_daily (
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 day_key INTEGER NOT NULL,
 actions INTEGER NOT NULL CHECK(actions>=0),
 hits INTEGER NOT NULL CHECK(hits>=0 AND hits<=actions),
 periods_observed INTEGER NOT NULL CHECK(periods_observed>=actions),
 closed_at INTEGER NOT NULL,
 PRIMARY KEY(strategy_id,day_key)
) WITHOUT ROWID;
CREATE TABLE IF NOT EXISTS su1_macro (
 macro_id TEXT NOT NULL PRIMARY KEY CHECK(macro_id LIKE 'M%' AND length(macro_id)=11),
 canonical_text TEXT NOT NULL UNIQUE CHECK(length(canonical_text) BETWEEN 3 AND 4096),
 support INTEGER NOT NULL CHECK(support>=2),
 best_fitness REAL NOT NULL,
 born_campaign TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 last_campaign TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 state TEXT NOT NULL CHECK(state IN ('ACTIVE','RETIRED'))
);
CREATE TABLE IF NOT EXISTS su1_emergent_family (
 family_id TEXT NOT NULL PRIMARY KEY CHECK(family_id='EMERGENT:'||macro_id),
 macro_id TEXT NOT NULL UNIQUE REFERENCES su1_macro(macro_id),
 created_campaign TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_director_state (
 campaign_id TEXT NOT NULL PRIMARY KEY REFERENCES su1_campaign(campaign_id),
 state_json TEXT NOT NULL CHECK(length(CAST(state_json AS BLOB))<=65536),
 state_hash TEXT NOT NULL CHECK(length(state_hash)=64)
);

-- Era ordering inside a campaign: DEV < VAL < SEALED, no overlap.
CREATE TRIGGER IF NOT EXISTS su1_era_order BEFORE INSERT ON su1_era
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_era e WHERE e.campaign_id=NEW.campaign_id AND (
   (NEW.kind='VAL' AND e.kind='DEV' AND e.last_sequence>=NEW.first_sequence) OR
   (NEW.kind='SEALED' AND e.kind IN ('DEV','VAL') AND e.last_sequence>=NEW.first_sequence) OR
   (NEW.kind='DEV' AND e.kind IN ('VAL','SEALED') AND NEW.last_sequence>=e.first_sequence) OR
   (NEW.kind='VAL' AND e.kind='SEALED' AND NEW.last_sequence>=e.first_sequence)))
 THEN RAISE(ABORT,'SU1_ERA_ORDER') END;
 SELECT CASE WHEN NEW.last_sequence>(SELECT data_through_sequence FROM su1_campaign WHERE campaign_id=NEW.campaign_id)
 THEN RAISE(ABORT,'SU1_ERA_BEYOND_DATA') END;
END;
-- A sealed span that was already opened can never be reused as SEALED, VAL or DEV-free test data.
CREATE TRIGGER IF NOT EXISTS su1_sealed_reuse_guard BEFORE INSERT ON su1_era
WHEN NEW.kind='SEALED'
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_sealed_open o JOIN su1_era e ON e.era_id=o.era_id
   WHERE NEW.first_sequence<=e.last_sequence AND NEW.last_sequence>=e.first_sequence)
 THEN RAISE(ABORT,'SU1_SEALED_SPAN_ALREADY_OPENED') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_sealed_open_binding BEFORE INSERT ON su1_sealed_open
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_era WHERE era_id=NEW.era_id AND kind='SEALED' AND campaign_id=NEW.campaign_id)
 THEN RAISE(ABORT,'SU1_SEALED_OPEN_BINDING') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_sealed_open WHERE campaign_id=NEW.campaign_id)
 THEN RAISE(ABORT,'SU1_ONE_SEALED_OPEN_PER_CAMPAIGN') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_sealed_open_immutable BEFORE UPDATE ON su1_sealed_open
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_sealed_open_no_delete BEFORE DELETE ON su1_sealed_open
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_era_immutable BEFORE UPDATE ON su1_era
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_era_no_delete BEFORE DELETE ON su1_era
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- Pool: ACTIVE -> RETIRED only.
CREATE TRIGGER IF NOT EXISTS su1_pool_transition BEFORE UPDATE ON su1_research_pool
BEGIN
 SELECT CASE WHEN NOT (OLD.state='ACTIVE' AND NEW.state='RETIRED' AND NEW.entry_id=OLD.entry_id
   AND NEW.strategy_id=OLD.strategy_id AND NEW.campaign_id=OLD.campaign_id AND NEW.arm=OLD.arm
   AND NEW.born_sequence=OLD.born_sequence AND NEW.prior_fitness=OLD.prior_fitness AND NEW.created_at=OLD.created_at)
 THEN RAISE(ABORT,'SU1_POOL_TRANSITION') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_pool_no_delete BEFORE DELETE ON su1_research_pool
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

-- Live shadow: only ACTIVE pool members, only from their birth, only before the draw opens.
CREATE TRIGGER IF NOT EXISTS su1_live_shadow_freeze BEFORE INSERT ON su1_live_shadow
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_research_pool WHERE strategy_id=NEW.strategy_id
   AND state='ACTIVE' AND born_sequence<=NEW.sequence_ordinal)
 THEN RAISE(ABORT,'SU1_LIVE_NOT_IN_ACTIVE_POOL') END;
 SELECT CASE WHEN NEW.hit IS NOT NULL OR NEW.settled_at IS NOT NULL
 THEN RAISE(ABORT,'SU1_LIVE_FREEZE_MUST_BE_UNSETTLED') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_live_shadow WHERE strategy_id=NEW.strategy_id AND sequence_ordinal=NEW.sequence_ordinal)
 THEN RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT') END;
END;
-- Settlement: hit/settled_at set once; everything else immutable.
CREATE TRIGGER IF NOT EXISTS su1_live_shadow_settle BEFORE UPDATE ON su1_live_shadow
BEGIN
 SELECT CASE WHEN NOT (OLD.hit IS NULL AND NEW.hit IS NOT NULL AND NEW.strategy_id=OLD.strategy_id
   AND NEW.sequence_ordinal=OLD.sequence_ordinal AND NEW.day_key=OLD.day_key AND NEW.position=OLD.position
   AND NEW.number_mask=OLD.number_mask AND NEW.captured_at=OLD.captured_at AND NEW.target_open_time=OLD.target_open_time)
 THEN RAISE(ABORT,'SU1_LIVE_SETTLE_ONCE') END;
END;
-- Compaction: a day may be deleted only after its aggregate exists and matches the rows.
CREATE TRIGGER IF NOT EXISTS su1_live_shadow_compact_guard BEFORE DELETE ON su1_live_shadow
BEGIN
 SELECT CASE WHEN OLD.hit IS NULL OR NOT EXISTS(SELECT 1 FROM su1_live_daily d WHERE d.strategy_id=OLD.strategy_id AND d.day_key=OLD.day_key)
 THEN RAISE(ABORT,'SU1_LIVE_COMPACT_REQUIRES_DAILY') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_live_daily_close BEFORE INSERT ON su1_live_daily
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_live_shadow WHERE strategy_id=NEW.strategy_id AND day_key=NEW.day_key AND hit IS NULL)
 THEN RAISE(ABORT,'SU1_LIVE_DAY_HAS_UNSETTLED') END;
 SELECT CASE WHEN NEW.actions<>(SELECT count(*) FROM su1_live_shadow WHERE strategy_id=NEW.strategy_id AND day_key=NEW.day_key)
   OR NEW.hits<>(SELECT coalesce(sum(hit),0) FROM su1_live_shadow WHERE strategy_id=NEW.strategy_id AND day_key=NEW.day_key)
 THEN RAISE(ABORT,'SU1_LIVE_DAILY_MISMATCH') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_live_daily_immutable BEFORE UPDATE ON su1_live_daily
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_live_daily_no_delete BEFORE DELETE ON su1_live_daily
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

CREATE TRIGGER IF NOT EXISTS su1_macro_update BEFORE UPDATE ON su1_macro
BEGIN
 SELECT CASE WHEN NEW.macro_id<>OLD.macro_id OR NEW.canonical_text<>OLD.canonical_text OR NEW.born_campaign<>OLD.born_campaign
   OR NEW.support<OLD.support OR (OLD.state='RETIRED' AND NEW.state='ACTIVE')
 THEN RAISE(ABORT,'SU1_MACRO_IDENTITY_OR_MONOTONE') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_macro_no_delete BEFORE DELETE ON su1_macro
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_emergent_family_immutable BEFORE UPDATE ON su1_emergent_family
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_director_state_immutable BEFORE UPDATE ON su1_director_state
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_campaign_transition BEFORE UPDATE ON su1_campaign
BEGIN
 SELECT CASE WHEN NOT (OLD.status='RUNNING' AND NEW.status IN ('DONE','FAILED') AND NEW.campaign_id=OLD.campaign_id
   AND NEW.kind=OLD.kind AND NEW.data_through_sequence=OLD.data_through_sequence AND NEW.config_hash=OLD.config_hash
   AND NEW.started_at=OLD.started_at AND NEW.programs_evaluated>=OLD.programs_evaluated)
 THEN RAISE(ABORT,'SU1_CAMPAIGN_TRANSITION') END;
END;
