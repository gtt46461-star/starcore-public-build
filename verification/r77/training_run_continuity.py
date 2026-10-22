"""Actual continuity owner over controlled report reconstruction, not Android timing proof.
Missing numeric run/input sidecars prevent replaying the report's online stream; recorded
online failures remain visible. Models, certificates and REAL rows are never invented.
"""
from pathlib import Path
import argparse,json,sqlite3,subprocess,collections
p=argparse.ArgumentParser()
for k in ('java','classpath','baseline','bridge','seed','assets','out'):
 p.add_argument('--'+k,required=True)
p.add_argument('--modes',default='fast,baseline,slow,write,p0')
a=p.parse_args();out=Path(a.out);out.mkdir(parents=True,exist_ok=True)
summary={}
for mode in a.modes.split(','):
 run=out/mode;run.mkdir(exist_ok=True);db=run/'report.db'
 if db.exists():raise RuntimeError('Fresh output directory required: '+str(run))
 source=sqlite3.connect(a.seed);target=sqlite3.connect(db);source.backup(target);target.close();source.close()
 cp=a.classpath if mode!='baseline' else a.baseline
 cmd=[a.java,'-Xmx512m','-cp',cp,'com.ethk3.novaonepro.training_ContinuityR77',a.bridge,str(db),str(run/'app'),a.assets,str(run/'run'),'77',mode]
 (run/'command.json').write_text(json.dumps(cmd,indent=2))
 with (run/'run.log').open('w') as f:subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,check=True)
 pulses=json.loads((run/'run-pulses.json').read_text());last=pulses[-1]
 lanes=collections.Counter()
 for x in pulses:
  text=x['outcome'];key='|lane='
  if key in text:lanes[text.split(key)[1].split('|')[0]]+=1
 c=sqlite3.connect(db)
 summary[mode]={'write_failures':last.get('write_failures',0),'slow_reads':last.get('slow_reads',0),'callbacks':len(pulses),'feature_rows':last['feature_rows'],'receipts':last['receipts'],'online_cursor':last['online_cursor'],'owners':last['owners'],'lanes':dict(lanes),'progress_callbacks':sum(x['feature_rows']>(pulses[i-1]['feature_rows'] if i else 0) for i,x in enumerate(pulses)),'feature_tasks':c.execute('SELECT count(*) FROM research_training_feature_cursor_cp12').fetchone()[0],'max_callback_ms':max(x['ms'] for x in pulses),'wall_scope':'HOST_TIMING_NOT_PHONE_LATENCY'}
 c.close();(out/'summary.json').write_text(json.dumps(summary,indent=2));print(mode,summary[mode],flush=True)
 # Separate process restart continues exactly the same DB and resets process cache.
 if mode=='fast':
  cmd[-3]=str(run/'restart');cmd[-2]='77'
  with (run/'restart.log').open('w') as f:subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,check=True)
  restart=json.loads((run/'restart-pulses.json').read_text());summary['restart']={'callbacks':len(restart),'before_features':last['feature_rows'],'feature_rows':restart[-1]['feature_rows'],'receipts':restart[-1]['receipts'],'owners':restart[-1]['owners']}
  assert restart[-1]['feature_rows']>last['feature_rows'],'Restart failed to advance durable prefixes'
  (out/'summary.json').write_text(json.dumps(summary,indent=2))
