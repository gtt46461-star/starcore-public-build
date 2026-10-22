#!/usr/bin/env python3
"""Measure the product prior-count predicate and its exact LIMIT/OFFSET equivalent on SQLite.
Large old full scans are explicitly interrupted; no interrupted run is reported as a result.
"""
import argparse,sqlite3,time,json
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--schema',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args();a.out.mkdir(parents=True,exist_ok=True)
schema=sqlite3.connect(a.schema);reports=[]
for size in [5000,100000]:
 db=sqlite3.connect(a.out/('lag-'+str(size)+'.db'))
 for table in ['draws','research_matrix_regime5_cp12']:
  for (sql,) in schema.execute("SELECT sql FROM sqlite_master WHERE tbl_name=? AND type IN('table','index') AND sql IS NOT NULL ORDER BY CASE type WHEN 'table' THEN 0 ELSE 1 END",[table]):db.execute(sql)
 db.executemany('INSERT INTO draws(issue,dirty,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,0,?,1,2,3,4,5,6,7,8,9,10)',((f'{i:012}',i) for i in range(1,size+1)));db.commit()
 def predicate(old,lag):
  return '(SELECT COUNT(*) FROM draws prior WHERE prior.dirty=0 AND prior.issue<d.issue)>='+str(lag) if old else 'd.issue>(SELECT prior.issue FROM draws prior WHERE prior.dirty=0 ORDER BY prior.issue LIMIT 1 OFFSET '+str(lag-1)+')'
 def run(sql,limit=None):
  steps=0
  def progress():
   nonlocal steps
   steps+=1000
   return limit is not None and steps>limit
  db.set_progress_handler(progress,1000);start=time.perf_counter();rows=None;interrupted=False
  try:rows=db.execute(sql).fetchall()
  except sqlite3.OperationalError as e:
   if str(e)!='interrupted':raise
   interrupted=True
  finally:elapsed=(time.perf_counter()-start)*1000;db.set_progress_handler(None,0)
  return {'wall_ms':elapsed,'vm_steps_lower_bound':steps,'rows':rows,'interrupted':interrupted,'sql':sql,'plan':db.execute('EXPLAIN QUERY PLAN '+sql).fetchall()}
 # Exact production no-regime source shape with the candidate checkpoint/input absent.
 suffix=" AND EXISTS(SELECT 1 FROM research_matrix_regime5_cp12 r WHERE r.target_issue=d.issue AND r.position=1 AND r.policy_hash='"+'0'*64+"' AND r.regime_id=0 AND r.captured_at<d.open_time) ORDER BY d.issue LIMIT 1"
 old=run('SELECT d.issue FROM draws d WHERE d.dirty=0 AND '+predicate(True,1)+suffix,None if size==5000 else 5000000)
 new=run('SELECT d.issue FROM draws d WHERE d.dirty=0 AND '+predicate(False,1)+suffix)
 assert new['rows']==[] and not new['interrupted']
 if not old['interrupted']:assert old['rows']==new['rows']
 # On the full 100000-row table both predicates complete for sparse selected targets, including lag>1/dirty rows.
 db.execute("UPDATE draws SET dirty=1 WHERE CAST(issue AS INTEGER)%11=0")
 parity=0
 for lag in [1,2,3,10,50,1000,4999,6000,9999]:
  for target in [1,2,4,11,12,100,size]:
   where=" FROM draws d WHERE d.issue='"+f'{target:012}'+"'"
   x=db.execute('SELECT '+predicate(True,lag)+where).fetchone();y=db.execute('SELECT COALESCE('+predicate(False,lag)+',0)'+where).fetchone();assert x==y,(size,lag,target,x,y);parity+=1
 large_lag=[run('SELECT d.issue FROM draws d WHERE d.dirty=0 AND '+predicate(False,lag)+suffix) for lag in [10,4999,6000,9999]]
 for row in large_lag:assert row['rows']==[] and not row['interrupted']
 reports.append({'large_lag_new':large_lag,'rows':size,'empty_regime_old':old,'empty_regime_new':new,'exact_predicate_parity_cases':parity,'note':'100000-row old all-source scan is capped explicitly; sparse full-table predicates complete and match.'})
 db.close()
(a.out/'lag_sql.json').write_text(json.dumps(reports,indent=2)+'\n')
for r in reports:print('R75_MATRIX_LAG_SQL_PASS rows=%d parity=%d old_ms=%.3f old_steps=%d old_interrupted=%s new_ms=%.3f new_steps=%d'%(r['rows'],r['exact_predicate_parity_cases'],r['empty_regime_old']['wall_ms'],r['empty_regime_old']['vm_steps_lower_bound'],r['empty_regime_old']['interrupted'],r['empty_regime_new']['wall_ms'],r['empty_regime_new']['vm_steps_lower_bound']))
