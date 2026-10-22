#!/usr/bin/env python3
import sqlite3,tempfile,os
fd,path=tempfile.mkstemp(prefix='r911-sql-',suffix='.db'); os.close(fd)
try:
    db=sqlite3.connect(path)
    # Exact problem shape observed on R910: old table exists, CREATE IF NOT EXISTS cannot add canonical column.
    db.executescript('''
    create table v57_selected_evidence_r906(
      target_issue text primary key, action_ordinal integer not null, action_hash text not null,
      raw_action_p_value real, raw_action_p_status text not null, rank_score_value real,
      rank_score_status text not null, third_margin_value real, third_margin_status text not null,
      entropy_risk_value real, entropy_risk_status text not null, stability_value real,
      stability_status text not null, window_consensus_value real, window_consensus_status text not null,
      assignment_support_value real, assignment_support_status text not null, record_hash text not null);
    create table v57_oos_label_r35(target_issue text primary key,strict_oos int,prediction_created_at int,draw_open_time int,hit_label int,selected_hit_p real,actual_number int,record_hash text,settled_at int);
    create table v57_settlement_task_r36(target_issue text primary key,status text,draw_hash text);
    create table v57_shadow_observation_r35(target_issue text primary key,pre_draw_frozen int,settled int,record_hash text);
    create table v57_research_compact_label_r906(asset_id text,target_issue text,feature_blob blob,hit_label int);
    create table v57_research_artifact_r811(artifact_type text,artifact_blob blob,artifact_hash text,trained_until_issue text,status text);
    create table v57_algorithm_predraw_r908(genome_id text,target_issue text,hit_label int);
    create table v57_research_predraw_r817(candidate_id text,target_issue text,hit_label int);
    ''')
    db.execute("insert into v57_selected_evidence_r906 values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
               ('100',1,'h',.2,'AVAILABLE',.3,'AVAILABLE',.1,'AVAILABLE',.2,'AVAILABLE',.4,'AVAILABLE',.5,'AVAILABLE',.6,'AVAILABLE','r'))
    # R911 additive migration.
    cols={r[1] for r in db.execute('pragma table_info(v57_selected_evidence_r906)')}
    if 'rank_status' not in cols:
        db.execute("alter table v57_selected_evidence_r906 add column rank_status text not null default 'UNAVAILABLE'")
    cols={r[1] for r in db.execute('pragma table_info(v57_selected_evidence_r906)')}
    if 'rank_score_status' in cols:
        db.execute("update v57_selected_evidence_r906 set rank_status=rank_score_status where rank_score_status is not null and rank_score_status<>'' and (rank_status is null or rank_status='' or rank_status='UNAVAILABLE')")
    assert db.execute("select rank_status from v57_selected_evidence_r906 where target_issue='100'").fetchone()[0]=='AVAILABLE'
    print('R910_LEGACY_RANK_COLUMN_MIGRATION=PASS')

    # Prepare the same contract shapes used by Android R911 ResearchRuntimeSqlGate.
    queries=[
      "select e.target_issue,e.raw_action_p_value,e.rank_score_value,e.rank_status from v57_selected_evidence_r906 e limit 0",
      "select o.target_issue,e.rank_status,t.status,s.settled from v57_oos_label_r35 o join v57_selected_evidence_r906 e on e.target_issue=o.target_issue join v57_settlement_task_r36 t on t.target_issue=o.target_issue join v57_shadow_observation_r35 s on s.target_issue=o.target_issue where o.strict_oos=1 and t.status='COMPLETED' and s.pre_draw_frozen=1 and s.settled=1 limit 0",
      "select target_issue,feature_blob,hit_label from v57_research_compact_label_r906 where asset_id='PRODUCTION_SELECTED' limit 0",
      "select artifact_blob,artifact_hash,trained_until_issue from v57_research_artifact_r811 where artifact_type='FTRL_SHADOW' limit 0",
      "select genome_id,target_issue,hit_label from v57_algorithm_predraw_r908 limit 0",
      "select candidate_id,target_issue,hit_label from v57_research_predraw_r817 limit 0",
    ]
    for q in queries: db.execute(q).fetchall()
    print(f'R911_SQL_PREPARED={len(queries)}')
    print('COMPACT_FTRL_TINYDEEP_SQL_CONTRACT=PASS')
finally:
    try: db.close()
    except Exception: pass
    try: os.unlink(path)
    except OSError: pass
