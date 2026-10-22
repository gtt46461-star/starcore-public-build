#!/usr/bin/env python3
"""Execute actual concatenated production SELECT SQL against gap/leakage fixtures."""
import json,re,sqlite3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];J=ROOT/'app/src/main/java/com/ethk3/novaonepro'
constants={}
for file in ['DbHelper','AssetGenesisEngineR915']:
 for name,val in re.findall(r'(?:public |private |protected )?static final String\s+(\w+)\s*=\s*"([^"]*)"', (J/(file+'.java')).read_text()):constants[file+'.'+name]=val

def sql(file,method):
 s=(J/(file+'.java')).read_text();s=s[s.index(method):];s=s[s.index('.rawQuery(')+10:];s=s[:s.index('new String[]')].rstrip(' ,\n')
 tokens=re.findall(r'"(?:\\.|[^"\\])*"|(?:DbHelper|AssetGenesisEngineR915)\.\w+',s)
 return ''.join(json.loads(t) if t.startswith('"') else constants[t] for t in tokens)

def check(b,msg):
 if not b:raise AssertionError(msg)

q=sql('ResearchPredrawSettlementPumpR908','private static List<String> pendingIssues')
db=sqlite3.connect(':memory:')
for t in [constants['DbHelper.TABLE_R817_RESEARCH_PREDRAW'],constants['DbHelper.TABLE_R908_ALGORITHM_PREDRAW'],constants['AssetGenesisEngineR915.TABLE_ASSET_FREEZE']]:
 db.execute(f'CREATE TABLE {t}(target_issue TEXT,hit_label INTEGER)')
 db.executemany(f'INSERT INTO {t} VALUES (?, -1)',[(f'20260906{i:04d}',) for i in range(1,110)])
draws=constants['DbHelper.TABLE_DRAWS'];db.execute(f'CREATE TABLE {draws}(issue TEXT,dirty INTEGER)');db.executemany(f'INSERT INTO {draws} VALUES (?,?)',[('202609060100',0),('202609060101',1),('202609060102',0)])
check(db.execute(q,('2',)).fetchall()==[('202609060100',),('202609060102',)],'OLD_GAPS_MUST_NOT_BLOCK_READY_OR_INCLUDE_DIRTY')
print('PRODUCTION_SQL_GAP_DIRTY_DEDUP=PASS')
q2=sql('ResearchAdaptiveLearnerStoreR811','public static String nextTrainableIssue')
# This query ends its argument on the same line.
# sql extraction accepts only string literals and table constants before argument data.
for key,cols in [
 ('TABLE_R35_OOS_LABEL','target_issue TEXT,strict_oos INT,hit_label INT,prediction_created_at INT,draw_open_time INT'),
 ('TABLE_R906_SELECTED_EVIDENCE','target_issue TEXT,raw_action_p_status TEXT,rank_status TEXT,third_margin_status TEXT,entropy_risk_status TEXT,stability_status TEXT,window_consensus_status TEXT'),
 ('TABLE_R36_SETTLEMENT_TASK','target_issue TEXT,status TEXT'),
 ('TABLE_R35_SHADOW_OBSERVATION','target_issue TEXT,pre_draw_frozen INT,settled INT')]:
 db.execute(f'CREATE TABLE {constants["DbHelper."+key]}({cols})')
for n in range(1,9):
 issue=f'20260906{n:04d}'
 db.execute(f'INSERT INTO {constants["DbHelper.TABLE_R35_OOS_LABEL"]} VALUES (?,?,?,?,?)',(issue,0 if n==3 else 1,1,100,90 if n==4 else 110))
 db.execute(f'INSERT INTO {constants["DbHelper.TABLE_R906_SELECTED_EVIDENCE"]} VALUES (?,?,?,?,?,?,?)',(issue,'UNAVAILABLE' if n==1 else 'AVAILABLE',*(['AVAILABLE']*5)))
 db.execute(f'INSERT INTO {constants["DbHelper.TABLE_R36_SETTLEMENT_TASK"]} VALUES (?,?)',(issue,'PENDING' if n==2 else 'COMPLETED'))
 db.execute(f'INSERT INTO {constants["DbHelper.TABLE_R35_SHADOW_OBSERVATION"]} VALUES (?,?,?)',(issue,0 if n==5 else 1,0 if n==6 else 1))
check(db.execute(q2,('',)).fetchone()[0]=='202609060007','CHRONOLOGICAL_VALID_ONLY')
check(db.execute(q2,('202609060007',)).fetchone()[0]=='202609060008','CURSOR')
print('PRODUCTION_SQL_LEARNING_GAPS_TEMPORAL_LEAKAGE=PASS')
print('R924_SQLITE_HOST=PASS|NOT_DEVICE_DATABASE')
