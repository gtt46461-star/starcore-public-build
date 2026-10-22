"""Retry unchanged canonical assertions with real host fsync and fresh isolated fixtures."""
from pathlib import Path
import json, shutil, subprocess, sqlite3
root=Path(__file__).resolve().parents[3]
prior=root/'evidence/replay-prerequisites'
out=root/'evidence/current-retry-with-settlement';out.mkdir(exist_ok=True)
entries=json.loads((prior/'receipt.json').read_text()); results=[]
for name in ('ModernFamilyContractCP12','ModernSeedRegistrationContractCP12','ColdCreatorPublicContractCP12'):
    old=next(r for r in entries if r['test']==name); d=out/name;d.mkdir(exist_ok=True)
    db=d/'fixture.db';shutil.copy2(prior/'seed-import.db',db)
    # Explicit synthetic PIT history from the original replay, never a production database.
    source=sqlite3.connect(prior/name/'fixture.db');dest=sqlite3.connect(db)
    for table in ('draws','v57_settlement_task_r36'):
        if not source.execute('SELECT 1 FROM sqlite_master WHERE name=?',(table,)).fetchone():continue
        rows=source.execute('SELECT * FROM '+table).fetchall()
        if rows:dest.executemany('INSERT OR REPLACE INTO '+table+' VALUES('+','.join('?' for _ in rows[0])+')',rows)
    dest.commit();source.close();dest.close()
    cmd=[s.replace(str(prior/name),str(d)) for s in old['command']]
    with (d/'execution.log').open('w') as log:
        r=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=300)
    results.append({'test':name,'exit_code':r.returncode,'status':'PASS' if r.returncode==0 else 'FAIL','command':cmd,'scope':'Synthetic host fixture; unchanged canonical assertions; real host fsync adapter'})
    (out/'receipt.json').write_text(json.dumps(results,indent=2));print(name,results[-1]['status'],flush=True)
