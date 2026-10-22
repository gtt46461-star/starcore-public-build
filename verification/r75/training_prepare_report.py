from pathlib import Path
import argparse,json,sqlite3,shutil,hashlib,subprocess
p=argparse.ArgumentParser();p.add_argument('--report',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--schema',type=Path);a=p.parse_args();r=Path(__file__).resolve().parents[2];w=r.parent;schema=a.schema or w/'r72-final-schema';a.out.mkdir(parents=True,exist_ok=True);db=a.out/'family-report.db';shutil.copy2(schema/'schema-complete.db',db)
x=json.load(a.report.open());tables={t['table']:t for t in x['recentEvidence']};c=sqlite3.connect(db)
for name in ['draws','v57_draw_canonical_r811','research_atom_spec_v2','research_action_v2','research_family_creation_cp12']:
 t=tables[name];cols=t['columns'];sql='INSERT INTO "'+name+'"('+','.join('"'+v+'"' for v in cols)+') VALUES('+','.join('?' for _ in cols)+')'
 for row in t['rows']:c.execute(sql,[row.get(k) for k in cols])
for row in tables['research_family_creation_cp12']['rows']:
 action=next(q for q in tables['research_action_v2']['rows'] if q['action_id']==row['action_id']);issue=row['source_issue'];boundary=issue[:-4]+str(int(issue[-4:])+1).zfill(4);evidence=action['evidence_hash'];h=hashlib.sha256(('CP12_ACTION_BOUNDARY|'+row['action_id']+'|'+issue+'|'+boundary+'|'+evidence).encode()).hexdigest()
 c.execute('INSERT INTO research_action_input_boundary_cp12 VALUES(?,?,?,?,?,?)',(row['action_id'],issue,boundary,evidence,h,row['created_at']))
c.commit();c.close();print(db)
