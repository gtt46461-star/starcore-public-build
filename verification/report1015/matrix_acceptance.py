"""Execute existing matrix source/projection/consumption contracts on current product classes.
Explicit synthetic fixtures prove code paths, not natural predictive effectiveness.
"""
from pathlib import Path
import json,shutil,subprocess,os
R=Path(__file__).resolve().parents[2];W=R.parent;c=json.loads((W/'audit_work/host/config.json').read_text());O=W/'audit_work'/os.environ.get('STARCORE_MATRIX_OUT','matrix-current');O.mkdir(exist_ok=False)
sources=[R/'verification/canonical/extended'/name for name in ['ModernModelContractTest.java','MatrixBindingsCP12Test.java','MatrixHistoricalDrawCP12Test.java']]+[R/'verification/report1015/MatrixSourcesR78.java']
with (O/'compile.log').open('w')as f:subprocess.run([c['javac'],'-sourcepath',str(O/'empty'),'-cp',c['classpath'],'-d',c['tests'],*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
results=[]
for name in ['MatrixSourcesR78','MatrixBindingsCP12Test','MatrixHistoricalDrawCP12Test']:
 db=O/(name+'.db');shutil.copy2(W/'audit_work/host-schema.db',db)
 cmd=[c['java'],'-Xmx512m','-Dstarcore.test.ast='+str(W/'audit_work/acceptance-latest/fixture.ast'),'-cp',c['classpath'],'com.ethk3.novaonepro.r952.'+name,c['bridge'],str(db),str(R/'app/src/main/assets/cp4_atom_registry.jsonl')]
 with (O/(name+'.log')).open('w')as f:z=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=180)
 results.append({'test':name,'exit_code':z.returncode,'scope':'ACTUAL_PRODUCT_SYNTHETIC_SOURCES_NOT_PHONE_OR_QUALITY'});(O/'summary.json').write_text(json.dumps(results,indent=2));print(name,z.returncode,flush=True)

assert all(x['exit_code']==0 for x in results),'SEE_MATRIX_LOGS'
