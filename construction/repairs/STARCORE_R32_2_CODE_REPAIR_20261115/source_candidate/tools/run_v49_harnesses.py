from pathlib import Path
import subprocess,sys,json
scripts=['v49_static_selfcheck.py','v49_feature_parity.py','v49_deterministic_replay.py','v49_ablation_benchmark.py']
rows=[]
for s in scripts:
 p=subprocess.run([sys.executable,'tools/'+s],text=True,capture_output=True)
 rows.append({'script':s,'code':p.returncode,'stdout':p.stdout[-2000:],'stderr':p.stderr[-1000:]})
 print(s,'PASS' if p.returncode==0 else 'FAIL')
Path('reports/V49_HARNESSES_49072201.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),'utf-8')
sys.exit(0 if all(x['code']==0 for x in rows) else 1)
