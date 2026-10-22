"""Actual compiled ready predicate, SQL work comparison, and lease Owner regression."""
from pathlib import Path
import argparse,json,subprocess,sys
p=argparse.ArgumentParser()
for key in ('schema','database','classes','jdk','out'):p.add_argument('--'+key,type=Path,required=True)
p.add_argument('--overlay',type=Path,action='append',default=[])
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True);cl=o/'classes';cl.mkdir()
cp=':'.join(map(str,[cl,a.schema.resolve()/'classes',*[x.resolve() for x in a.overlay],a.classes.resolve(),(a.schema/'host-classpath').read_text().strip().split(':')[-1]]));commands=[]
def run(command,log):
 command=list(map(str,command));commands.append(command)
 with (o/log).open('w') as f:subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,check=True)
run([a.jdk/'bin/javac','-cp',cp,'-d',cl,r/'verification/r76/training_ReadyQueueR76.java',r/'verification/r72/realtime/android/os/SystemClock.java'],'compile.log')
base=[a.jdk/'bin/java','-Djava.library.path='+str(r.parent/'host-native'),'-cp',cp,'com.ethk3.novaonepro.training_ReadyQueueR76']
run([*base,'sql',o/'current-ready.sql'],'export.log')
run([sys.executable,r/'verification/r76/training_ready_profile.py','--database',a.database,'--baseline',r/'verification/r76/evidence/training_r75_ready_predicate.sql','--current',o/'current-ready.sql','--out',o/'profile'],'profile.log')
run([*base,r/'verification/canonical/schema/sqlite_bridge.py',o/'profile/queue.db'],'owner.log')
summary=json.loads((o/'profile/summary.json').read_text());summary.update({'commands':commands,'product_classes':str(a.classes.resolve()),'explicit_product_overlays':list(map(str,a.overlay)),'actual_claim_yield_owner':'PASS'})
(o/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps({k:{f:v[f] for f in ('ready','baseline_steps','current_steps')} for k,group in summary['profiles'].items() for v in [group['MAINTENANCE']]},indent=2))
