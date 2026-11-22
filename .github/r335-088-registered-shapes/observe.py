"""Compile released SQL shapes against the same installed application on a disposable guest, read only."""
from pathlib import Path
import hashlib,re,sys
path=Path('.github/r335-088-research-import/observe.py').resolve()
sys.path.insert(0,str(path.parent))
body=path.read_text().split('try:main()')[0]
body=body.replace('R335-088-research-import-', 'R335-088-registered-shapes-')
body=body.replace('(30,180,600,1200,2400,3600)', '(30,180)').replace('if at in (600,2400,3600):read_panels()', 'if at==180:read_panels()')
ns={'__name__':'disposable_registered_schema'}
exec(compile(body,str(path),'exec'),ns)
f=ns['f']; original=ns['sample']; query=ns['query']
def read(elapsed):
    original(elapsed)
    row=ns['record']['observations'][-1]
    dex=Path(__file__).parent/'observer.dex.jar'
    fingerprint='5100f51dfc3f84057a743304e4a0eff2969ebf1f58b3fe005458c1137dc8f39c'
    assert f.fingerprint(dex)==fingerprint
    remote='/data/local/tmp/r335-shape-verifier.jar'
    f.adb('push',str(dex),remote)
    f.adb('shell','chmod','0444',remote)
    assert f.adb('shell','sha256sum',remote).decode().split()[0]==fingerprint
    apk=f.adb('shell','pm','path',f.PKG).decode().strip().split('package:')[-1]
    assert f.adb('shell','sha256sum',apk).decode().split()[0]==ns['record']['apkSha256']
    db='/data/user/0/'+f.PKG+'/databases/'+ns['observer'].DB
    raw=f.adb('shell','env','CLASSPATH='+remote+':'+apk,'app_process','/system/bin','com.ethk3.novaonepro.strategyv2.InstalledPublicationShapes',db)
    shapes=[s for s in raw.decode().splitlines() if re.fullmatch(r'SHAPE[|][0-9a-f]{64}[|][A-Za-z_0-9:]+',s)]
    assert len(shapes)==14
    row['compiledRegisteredShapes']=shapes
    # Only SQLite schema definitions attached to known released tables. No application records.
    targets=('r26_evolution','r27_policy','r27_lineage','r27_evolution_cursor','r27_expert_proposal','r27_replay_score','r27_generation','r30_confirm_dataset','r30_frozen_confirm')
    predicate=','.join(repr(t) for t in targets)
    ddl=query("SELECT hex(sql) FROM sqlite_master WHERE type='trigger' AND lower(tbl_name) IN ("+predicate+") ORDER BY name")
    row['registeredTriggerDdl']=[]
    for line in ddl.splitlines():
        assert re.fullmatch('[A-Fa-f0-9]+',line) and len(line)<85000
        statement=bytes.fromhex(line).decode()
        assert re.match(r'CREATE TRIGGER ',statement,re.I)
        row['registeredTriggerDdl'].append(statement)
    ns['save']()
ns['sample']=read
try:ns['main']()
except Exception as err:
    ns['record'].update(completed=False,failureType=type(err).__name__)
    ns['save']()
    raise
