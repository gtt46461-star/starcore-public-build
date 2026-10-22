#!/usr/bin/env python3
"""Executable SQLite contracts for denominator truth and algorithm-only settlement discovery."""
import sqlite3

db = sqlite3.connect(":memory:")
db.executescript(
    """
    create table draws(issue text primary key, dirty integer not null);
    create table shadow(target_issue text primary key, pre_draw_frozen integer not null, settled integer not null);
    create table oos(target_issue text primary key, strict_oos integer not null);
    create table task(target_issue text primary key, status text not null);
    create table research_predraw(candidate_id text, target_issue text, hit_label integer);
    create table algorithm_predraw(genome_id text, target_issue text, hit_label integer);
    create table selected(target_issue text primary key, shape_lifecycle text, shape_prototype text,
      shape_run_length integer, shape_change_risk real, shape_confidence real);
    create table selected_evidence(target_issue text primary key, rank_score_value real,
      rank_status text not null, raw_action_p_value real, raw_action_p_status text not null);
    """
)

for issue in range(1, 101):
    key = f"20260828{issue:04d}"
    db.execute("insert into draws values(?,0)", (key,))
    db.execute("insert into shadow values(?,1,?)", (key, 1 if issue <= 4 else 0))
    db.execute("insert into task values(?,?)", (key, "COMPLETED" if issue <= 4 else "RETRY_WAIT"))
    if issue <= 4:
        db.execute("insert into oos values(?,1)", (key,))

eligible = db.execute(
    "select count(*) from shadow s where pre_draw_frozen=1 and exists"
    "(select 1 from draws d where d.issue=s.target_issue and d.dirty=0)"
).fetchone()[0]
settled = db.execute(
    "select count(*) from shadow s where pre_draw_frozen=1 and s.settled=1 and exists"
    "(select 1 from oos o where o.target_issue=s.target_issue and o.strict_oos=1)"
).fetchone()[0]
assert (eligible, settled, eligible - settled) == (100, 4, 96)

# Only an algorithm row exists for the first issue; the UNION must still discover it.
db.execute("insert into algorithm_predraw values('ALG-A','202608280101',-1)")
db.execute("insert into research_predraw values('DSL-A','202608280102',-1)")
pending = [r[0] for r in db.execute(
    "select target_issue from (select target_issue from research_predraw where hit_label=-1 union "
    "select target_issue from algorithm_predraw where hit_label=-1) group by target_issue order by target_issue"
)]
assert pending == ["202608280101", "202608280102"]

db.execute("insert into selected values('202608280101','STABLE','PROTO-7',6,0.12,0.81)")
shape = db.execute("select shape_lifecycle,shape_prototype,shape_run_length from selected").fetchone()
assert shape == ("STABLE", "PROTO-7", 6)
db.execute("insert into selected_evidence(target_issue,rank_score_value,rank_status,raw_action_p_value,raw_action_p_status) values(?,?,?,?,?)",
           ("202608280101", 0.77, "AVAILABLE", 0.41, "AVAILABLE"))
assert db.execute("select rank_status from selected_evidence").fetchone()[0] == "AVAILABLE"

print("R912_SETTLEMENT_DENOMINATOR=PASS|eligible=100|settled=4|open=96")
print("R912_ALGORITHM_ONLY_PENDING_DISCOVERY=PASS")
print("R912_SHAPE_TRUTH_SCHEMA=PASS")
print("R912_CANONICAL_RANK_STATUS_WRITE=PASS")
