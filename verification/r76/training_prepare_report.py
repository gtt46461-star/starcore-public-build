"""Controlled report reconstruction, never presented as the original phone database.

Only exported columns are imported. Blob-omitting tables are not silently repaired.
Feature cursors need real feature rows reconstructed by the Java owner fixture.
"""
from pathlib import Path
import argparse, hashlib, json, shutil, sqlite3

p = argparse.ArgumentParser()
for key in ('report', 'schema', 'out'):
    p.add_argument('--' + key, type=Path, required=True)
a = p.parse_args(); a.out.mkdir(parents=True, exist_ok=True)
db = a.out / 'report.db'; shutil.copy2(a.schema / 'schema-complete.db', db)
x = json.loads(a.report.read_text()); report = {t['table']: t for t in x['recentEvidence']}
c = sqlite3.connect(db)
triggers = list(c.execute("SELECT name,sql FROM sqlite_master WHERE type='trigger'"))
for name, _ in triggers: c.execute('DROP TRIGGER "' + name + '"')
copied, skipped = {}, {}
for name, table in report.items():
    fields = {row[1]: row for row in c.execute('PRAGMA table_info("' + name + '")')}
    cols = table['columns']
    missing = [k for k, v in fields.items() if v[3] and v[4] is None and k not in cols]
    blobs = [k for k, v in fields.items() if v[2].upper() == 'BLOB']
    if missing or not fields or blobs:
        skipped[name] = {'rows': len(table['rows']), 'missing_required_columns': missing, 'blob_columns_not_authentically_exported': blobs}
        continue
    if name == 'research_training_feature_cursor_cp12':
        skipped[name] = {'rows': len(table['rows']), 'reason': 'OMITTED_FEATURE_ROWS_REBUILD_REQUIRED'}
        continue
    c.execute('DELETE FROM "' + name + '"')
    sql = 'INSERT INTO "' + name + '" (' + ','.join('"' + k + '"' for k in cols) + ') VALUES(' + ','.join('?' for _ in cols) + ')'
    for row in table['rows']: c.execute(sql, [row.get(k) for k in cols])
    copied[name] = len(table['rows'])
# Snapshot intentionally starts modern tasks at their original zero feature prefix;
# Java verifies the real reconstructed prefix against the phone's reported hash.
c.execute("UPDATE v57_research_task_r967 SET cursor='' WHERE kind='TRAIN_CANDIDATE'")
for row in x['researchSchedulerState']:
    if row['status'] == 'READ_OK':
        c.execute('INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,?)', (row['key'], row['value'], row['updatedAt']))
for row in report['research_family_creation_cp12']['rows']:
    action = next(q for q in report['research_action_v2']['rows'] if q['action_id'] == row['action_id'])
    issue = row['source_issue']; boundary = issue[:-4] + str(int(issue[-4:]) + 1).zfill(4)
    evidence = action['evidence_hash']
    h = hashlib.sha256(('CP12_ACTION_BOUNDARY|' + row['action_id'] + '|' + issue + '|' + boundary + '|' + evidence).encode()).hexdigest()
    c.execute('INSERT INTO research_action_input_boundary_cp12 VALUES(?,?,?,?,?,?)', (row['action_id'], issue, boundary, evidence, h, row['created_at']))
for _, sql in triggers: c.execute(sql)
c.commit(); c.close()
(a.out / 'reported-feature-cursors.json').write_text(json.dumps(report['research_training_feature_cursor_cp12']['rows'], indent=2))
(a.out / 'reconstruction.json').write_text(json.dumps({'scope': 'CONTROLLED_REPORT_RECONSTRUCTION_NOT_ORIGINAL_PHONE_DB', 'report_sha256': hashlib.sha256(a.report.read_bytes()).hexdigest(), 'copied': copied, 'skipped': skipped}, indent=2) + '\n')
print(db)
