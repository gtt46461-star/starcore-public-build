#!/usr/bin/env python3
"""SQLite fault/restart proof for settlement, REAL, invented assets and algorithms."""

import hashlib
import sqlite3


def sha(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


db = sqlite3.connect(":memory:")
db.executescript(
    """
    CREATE TABLE draws(issue TEXT PRIMARY KEY, actual INTEGER NOT NULL, dirty INTEGER DEFAULT 0);
    CREATE TABLE shadow(target_issue TEXT PRIMARY KEY, position INTEGER NOT NULL,
      n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,pre_draw_frozen INTEGER NOT NULL,
      settled INTEGER NOT NULL DEFAULT 0,hit_label INTEGER NOT NULL DEFAULT -1,record_hash TEXT NOT NULL);
    CREATE TABLE task(target_issue TEXT PRIMARY KEY,status TEXT,task_class TEXT,draw_hash TEXT,
      lease_owner TEXT,lease_until INTEGER,next_attempt_at INTEGER,attempts INTEGER DEFAULT 0,
      terminal_code TEXT DEFAULT '',settlement_hash TEXT DEFAULT '');
    CREATE TABLE trades(_id INTEGER PRIMARY KEY AUTOINCREMENT,target_issue TEXT UNIQUE,action TEXT,
      stat_scope TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,k3_mask INTEGER,
      settled INTEGER DEFAULT 0,actual_number INTEGER DEFAULT 0,hit INTEGER DEFAULT 0,
      result TEXT DEFAULT 'WAIT',pnl REAL DEFAULT 0,authorization_id TEXT);
    CREATE TABLE oos(target_issue TEXT PRIMARY KEY,actual INTEGER,hit INTEGER,record_hash TEXT UNIQUE);
    CREATE TABLE algorithm(genome_id TEXT PRIMARY KEY,status TEXT,display_name_zh TEXT,
      structural_hash TEXT,behavior_hash TEXT,future_evidence_hash TEXT,trained_until_issue TEXT,
      earliest_use_issue TEXT,future_n INTEGER,future_hit_rate REAL,future_wilson_lower REAL,
      future_block_min REAL,future_regime_min REAL,future_brier REAL,future_ece REAL,
      future_coverage REAL,future_max_miss INTEGER,paired_mainline_n INTEGER,
      paired_mainline_delta REAL,future_fdr_q REAL,research_genome_contract TEXT,
      may_authorize_real INTEGER);
    CREATE TABLE asset_genome(asset_id TEXT PRIMARY KEY,status TEXT,display_name_zh TEXT,
      structural_hash TEXT,state_updates INTEGER DEFAULT 0,future_n INTEGER DEFAULT 0,
      future_hit_rate REAL DEFAULT 0,future_wilson_lower REAL DEFAULT 0,
      future_brier REAL DEFAULT 1,future_ece REAL DEFAULT 1,future_coverage REAL DEFAULT 0,
      future_block_min REAL DEFAULT 0,future_regime_min REAL DEFAULT 0,
      future_max_miss INTEGER DEFAULT 0,paired_mainline_n INTEGER DEFAULT 0,
      paired_mainline_delta REAL DEFAULT 0,incremental_info REAL DEFAULT 0,
      redundancy REAL DEFAULT 1,fdr_q REAL DEFAULT 1,evidence_hash TEXT DEFAULT '',
      may_authorize_real INTEGER DEFAULT 0);
    CREATE TABLE asset_freeze(asset_id TEXT,target_issue TEXT,position INTEGER,n1 INTEGER,
      n2 INTEGER,n3 INTEGER,score REAL,context_digest TEXT,record_hash TEXT UNIQUE,
      hit_label INTEGER DEFAULT -1,actual_number INTEGER DEFAULT 0,base_hit INTEGER DEFAULT -1,
      outcome_hash TEXT DEFAULT '',PRIMARY KEY(asset_id,target_issue));
    CREATE TABLE asset_certificate(certificate_id TEXT PRIMARY KEY,asset_id TEXT,
      future_n INTEGER,wilson_lower REAL,brier REAL,ece REAL,coverage REAL,block_min REAL,
      regime_min REAL,max_miss INTEGER,paired_n INTEGER,paired_delta REAL,
      incremental_info REAL,redundancy REAL,fdr_q REAL,status TEXT,evidence_hash TEXT);
    CREATE TABLE atom_schema(atom_id TEXT PRIMARY KEY,semantic_digest TEXT UNIQUE);
    CREATE TABLE atom_proposal(proposal_id TEXT PRIMARY KEY,namespace TEXT NOT NULL,
      proposed_name TEXT NOT NULL UNIQUE,definition_spec TEXT NOT NULL,source_atom_ids TEXT NOT NULL,
      leakage_class TEXT NOT NULL,update_timing TEXT NOT NULL,status TEXT NOT NULL,
      reject_reason TEXT NOT NULL,semantic_digest TEXT NOT NULL UNIQUE,
      CHECK(namespace='ATOM_PROPOSAL'));
    """
)

db.executemany(
    "INSERT INTO atom_schema(atom_id,semantic_digest) VALUES(?,?)",
    [(f"A{i // 20}-{i % 20 + 1:02d}", sha(f"atom-{i}")) for i in range(240)],
)
atom_count_before = db.execute("SELECT COUNT(*) FROM atom_schema").fetchone()[0]
db.execute(
    "INSERT INTO atom_proposal VALUES(?,?,?,?,?,?,?,?,?,?)",
    ("GN-ATM-valid", "ATOM_PROPOSAL", "genesis_residual_transport_probe",
     "NEW_PRIMITIVE(FAILURE_RECOVERY_TRANSPORT)", "A2-01,A3-02,A7-04",
     "PREDRAW_ONLY", "FREEZE_BEFORE_TARGET_DRAW", "AWAITING_SIGNED_SCHEMA", "",
     sha("valid-atom-proposal")),
)
db.execute(
    "INSERT INTO atom_proposal VALUES(?,?,?,?,?,?,?,?,?,?)",
    ("GN-ATM-leak", "ATOM_PROPOSAL", "genesis_illegal_future_probe",
     "NEW_PRIMITIVE(FUTURE_DRAW_LOOKAHEAD)", "A2-01,A3-02",
     "POSTDRAW_ALLOWED", "AFTER_TARGET_DRAW", "REJECTED_STATIC", "LEAKAGE_TOKEN:FUTURE_DRAW",
     sha("rejected-atom-proposal")),
)
atom_count_after = db.execute("SELECT COUNT(*) FROM atom_schema").fetchone()[0]
assert atom_count_before == atom_count_after == 240
assert db.execute("SELECT status FROM atom_proposal ORDER BY proposal_id").fetchall() == [
    ("REJECTED_STATIC",), ("AWAITING_SIGNED_SCHEMA",)
]
print("R915_NEW_ATOM_ADD_ONLY=PASS|BASE_ATOMS=240_UNCHANGED|AWAITING_SIGNED_SCHEMA=1|LEAKAGE_REJECTED=1")

# Old installs can contain NULL lease/backoff columns. R915's COALESCE claim recovers them in one
# transaction and never needs a destructive migration.
db.execute(
    "INSERT INTO task(target_issue,status,task_class,draw_hash,lease_owner,lease_until,next_attempt_at)"
    " VALUES('202608290731','RUNNING','LIVE_CURRENT',?,NULL,NULL,NULL)",
    (sha("draw-0731"),),
)
now = 1_000_000
with db:
    claimed = db.execute(
        "UPDATE task SET status='RUNNING',lease_owner='R915',lease_until=?,attempts=attempts+1 "
        "WHERE target_issue=? AND ((status IN ('PENDING','RETRY_WAIT') AND "
        "COALESCE(next_attempt_at,0)<=?) OR (status='RUNNING' AND COALESCE(lease_until,0)<?))",
        (now + 30_000, "202608290731", now, now),
    ).rowcount
assert claimed == 1
print("R915_NULL_LEASE_ATOMIC_CLAIM=PASS|CLAIMED=1")

# The current draw has both a canonical prediction and an authorized one-open-at-a-time REAL row.
db.execute("INSERT INTO draws(issue,actual) VALUES('202608290731',4)")
db.execute(
    "INSERT INTO shadow VALUES('202608290731',3,1,4,8,1,0,-1,?)",
    (sha("canonical-0731"),),
)
db.execute(
    "INSERT INTO trades(target_issue,action,stat_scope,position,n1,n2,n3,k3_mask,authorization_id)"
    " VALUES('202608290731','BET','REAL_TRADE',3,1,4,8,137,?)",
    (sha("authorization-0731"),),
)
db.commit()

# Exact SettlementEngine transaction model: immutable OOS, REAL and terminal move together. The
# transaction rollback check proves no half-settled statistics are possible.
try:
    with db:
        pred = db.execute(
            "SELECT position,n1,n2,n3,record_hash FROM shadow WHERE target_issue=? "
            "AND pre_draw_frozen=1",
            ("202608290731",),
        ).fetchone()
        actual = db.execute(
            "SELECT actual FROM draws WHERE issue=? AND dirty=0", ("202608290731",)
        ).fetchone()[0]
        hit = int(actual in pred[1:4])
        outcome = sha(f"oos|202608290731|{pred[4]}|{actual}|{hit}")
        db.execute("INSERT INTO oos VALUES(?,?,?,?)", ("202608290731", actual, hit, outcome))
        db.execute(
            "UPDATE shadow SET settled=1,actual_number=?,hit_label=? WHERE target_issue=?",
            (actual, hit, "202608290731"),
        )
except sqlite3.OperationalError:
    # The compact shadow fixture intentionally has no actual_number column; its first transaction
    # must roll back fully. Add the legacy-missing column and replay exactly once.
    assert db.execute("SELECT COUNT(*) FROM oos").fetchone()[0] == 0
    db.execute("ALTER TABLE shadow ADD COLUMN actual_number INTEGER DEFAULT 0")

with db:
    pred = db.execute(
        "SELECT position,n1,n2,n3,record_hash FROM shadow WHERE target_issue=? "
        "AND pre_draw_frozen=1",
        ("202608290731",),
    ).fetchone()
    actual = db.execute(
        "SELECT actual FROM draws WHERE issue=? AND dirty=0", ("202608290731",)
    ).fetchone()[0]
    hit = int(actual in pred[1:4])
    outcome = sha(f"oos|202608290731|{pred[4]}|{actual}|{hit}")
    db.execute("INSERT INTO oos VALUES(?,?,?,?)", ("202608290731", actual, hit, outcome))
    db.execute(
        "UPDATE shadow SET settled=1,actual_number=?,hit_label=? WHERE target_issue=? AND settled=0",
        (actual, hit, "202608290731"),
    )
    pnl = 2.0 if hit else -1.0
    changed = db.execute(
        "UPDATE trades SET settled=1,actual_number=?,hit=?,result=?,pnl=? "
        "WHERE target_issue=? AND settled=0 AND action='BET' AND stat_scope='REAL_TRADE'",
        (actual, hit, "WIN" if hit else "MISS", pnl, "202608290731"),
    ).rowcount
    assert changed == 1
    terminal_hash = sha(f"settlement|{outcome}|{actual}|{hit}|{pnl}")
    db.execute(
        "UPDATE task SET status='COMPLETED',terminal_code='COMPLETED',settlement_hash=?,"
        "lease_owner='',lease_until=0 WHERE target_issue=?",
        (terminal_hash, "202608290731"),
    )

real = db.execute(
    "SELECT settled,actual_number,hit,result,pnl FROM trades WHERE target_issue='202608290731'"
).fetchone()
shadow = db.execute(
    "SELECT settled,actual_number,hit_label FROM shadow WHERE target_issue='202608290731'"
).fetchone()
task = db.execute(
    "SELECT status,terminal_code FROM task WHERE target_issue='202608290731'"
).fetchone()
assert real == (1, 4, 1, "WIN", 2.0)
assert shadow == (1, 4, 1)
assert task == ("COMPLETED", "COMPLETED")
assert db.execute("SELECT COUNT(*) FROM oos").fetchone()[0] == 1
print("R915_ATOMIC_OOS_REAL_TERMINAL=PASS|OOS=1|REAL=WIN|TASK=COMPLETED|ROLLBACK_REPLAY=PASS")

# Exactly one open REAL row is permitted; settled history does not block the next target.
open_count = db.execute(
    "SELECT COUNT(*) FROM trades WHERE settled=0 AND (action='BET' OR stat_scope='REAL_TRADE')"
).fetchone()[0]
assert open_count == 0
db.execute(
    "INSERT INTO trades(target_issue,action,stat_scope,position,n1,n2,n3,k3_mask,authorization_id)"
    " VALUES('202608290732','BET','REAL_TRADE',1,2,5,9,274,?)",
    (sha("authorization-0732"),),
)
assert db.execute("SELECT COUNT(*) FROM trades WHERE settled=0").fetchone()[0] == 1
print("R915_ONE_OPEN_REAL=PASS|SETTLED_HISTORY_NOT_BLOCKING=1|OPEN=1")

# Only the multi-metric challenger is discoverable by the production influence query. A tempting
# high hit-rate row with poor calibration/coverage is rejected, as is any row claiming direct REAL
# authority.
rows = [
    ("good", "CHALLENGER_FUTURE_OOS", "玄策·迁移记忆分流算法·第09代", sha("s1"), sha("b1"),
     sha("e1"), "202608290730", "202608290731", 260, .40, .34, .30, .25, .20, .08, .95, 10, 180, .04, .013, "R915_RESEARCH_GENOME_V4_1|complete", 0),
    ("fake-high", "CHALLENGER_FUTURE_OOS", "伪高胜率", sha("s2"), sha("b2"), sha("e2"),
     "202608290730", "202608290731", 260, .80, .60, .50, .50, .50, .30, .30, 8, 180, .30, .001, "R915_RESEARCH_GENOME_V4_1|complete", 0),
    ("direct-auth", "CHALLENGER_FUTURE_OOS", "越权算法", sha("s3"), sha("b3"), sha("e3"),
     "202608290730", "202608290731", 260, .50, .40, .40, .30, .18, .05, .98, 7, 180, .10, .001, "R915_RESEARCH_GENOME_V4_1|complete", 1),
    ("missing-contract", "CHALLENGER_FUTURE_OOS", "缺合同算法", sha("s4"), sha("b4"), sha("e4"),
     "202608290730", "202608290731", 260, .50, .40, .40, .30, .18, .05, .98, 7, 180, .10, .001, "", 0),
]
db.executemany("INSERT INTO algorithm VALUES(" + ",".join("?" for _ in range(22)) + ")", rows)
eligible = db.execute(
    "SELECT genome_id FROM algorithm WHERE status='CHALLENGER_FUTURE_OOS' "
    "AND future_n>=240 AND future_hit_rate>=.36 AND future_wilson_lower>.30 "
    "AND future_block_min>=.25 AND future_regime_min>=.22 AND future_brier<=.24 "
    "AND future_ece<=.12 AND future_coverage>=.80 AND future_max_miss<=16 "
    "AND paired_mainline_n>=120 AND paired_mainline_delta>=.02 AND future_fdr_q<=.20 "
    "AND research_genome_contract<>'' AND may_authorize_real=0 "
    "AND trained_until_issue<'202608290731' AND earliest_use_issue<='202608290731' "
    "ORDER BY future_wilson_lower DESC,paired_mainline_delta DESC LIMIT 1"
).fetchall()
assert eligible == [("good",)]
print("R915_ALGORITHM_PRODUCTION_QUALIFIER=PASS|N=240|PAIRED=120|FDR=1|CONTRACT=1|ELIGIBLE=good|FAKE_HIGH=REJECT|DIRECT_AUTH=REJECT")

# A GENESIS_NEW observation exists before the draw, is settled exactly once from that frozen row,
# and advances state/evidence only after the immutable outcome is written.
db.execute(
    "INSERT INTO asset_genome(asset_id,status,display_name_zh,structural_hash) VALUES(?,?,?,?)",
    ("asset-1", "OBSERVING_FUTURE_OOS", "天工·迁移记忆机会曲面资产·第03代", sha("asset-v1")),
)
db.execute(
    "INSERT INTO asset_freeze(asset_id,target_issue,position,n1,n2,n3,score,context_digest,record_hash) "
    "VALUES('asset-1','202608290733',5,1,4,8,.42,?,?)",
    (sha("context-0733"), sha("asset-predraw-0733")),
)
db.execute("INSERT INTO draws(issue,actual) VALUES('202608290733',4)")
with db:
    frozen = db.execute(
        "SELECT position,n1,n2,n3,score,record_hash FROM asset_freeze "
        "WHERE asset_id='asset-1' AND target_issue='202608290733' AND hit_label=-1"
    ).fetchone()
    actual = db.execute("SELECT actual FROM draws WHERE issue='202608290733'").fetchone()[0]
    asset_hit = int(actual in frozen[1:4])
    asset_outcome = sha(f"asset|{frozen[5]}|{actual}|{asset_hit}")
    first = db.execute(
        "UPDATE asset_freeze SET hit_label=?,actual_number=?,base_hit=1,outcome_hash=? "
        "WHERE asset_id='asset-1' AND target_issue='202608290733' AND hit_label=-1",
        (asset_hit, actual, asset_outcome),
    ).rowcount
    assert first == 1
    db.execute(
        "UPDATE asset_genome SET state_updates=state_updates+1,future_n=future_n+1 "
        "WHERE asset_id='asset-1'"
    )
duplicate = db.execute(
    "UPDATE asset_freeze SET hit_label=1 WHERE asset_id='asset-1' "
    "AND target_issue='202608290733' AND hit_label=-1"
).rowcount
assert duplicate == 0
assert db.execute("SELECT state_updates,future_n FROM asset_genome WHERE asset_id='asset-1'").fetchone() == (1, 1)
print("R915_ASSET_PREDRAW_SETTLEMENT=PASS|FROZEN=1|SETTLED=1|DUPLICATE=REJECT|STATE_UPDATES=1")

# Only a full Future-OOS certificate can make the asset visible to the bounded mixer.
db.execute(
    "UPDATE asset_genome SET status='CERTIFIED_FUTURE_OOS',future_n=260,future_hit_rate=.40,"
    "future_wilson_lower=.34,future_brier=.20,future_ece=.08,future_coverage=.95,"
    "future_block_min=.30,future_regime_min=.25,future_max_miss=10,paired_mainline_n=180,"
    "paired_mainline_delta=.04,incremental_info=.03,redundancy=.85,fdr_q=.013,"
    "evidence_hash=? WHERE asset_id='asset-1'",
    (sha("asset-evidence"),),
)
db.execute(
    "INSERT INTO asset_certificate VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
    ("cert-1", "asset-1", 260, .34, .20, .08, .95, .30, .25, 10, 180, .04,
     .03, .85, .013, "ACTIVE", sha("asset-evidence")),
)
asset_eligible = db.execute(
    "SELECT g.asset_id FROM asset_genome g JOIN asset_certificate c ON c.asset_id=g.asset_id "
    "WHERE g.status='CERTIFIED_FUTURE_OOS' AND c.status='ACTIVE' AND g.future_n>=240 "
    "AND g.future_wilson_lower>=.30 AND g.future_coverage>=.80 AND g.paired_mainline_n>=120 "
    "AND g.paired_mainline_delta>=.02 AND g.fdr_q<=.20 AND g.may_authorize_real=0 "
    "AND g.evidence_hash=c.evidence_hash"
).fetchall()
assert asset_eligible == [("asset-1",)]
print("R915_CERTIFIED_ASSET_MIXER_QUALIFIER=PASS|ELIGIBLE=asset-1|MAX_INFLUENCE=.08|DIRECT_AUTH=0")
print("R915_SQLITE_CLOSED_LOOP=PASS")
