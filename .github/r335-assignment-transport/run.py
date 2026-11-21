import hashlib,json,os,re,subprocess,time
from pathlib import Path
from xml.etree import ElementTree as E
root=Path(os.environ['RUNNER_TEMP']);out=root/'numeric-diag';out.mkdir(exist_ok=True)
pkg='com.starcore.research.r13';app='com.ethk3.novaonepro';target='a8ce8f811af22cd29b14fe46774c32a12565b9a1e5b44b2c801c5412e71ae51f'
def cmd(*args,timeout=80):return subprocess.run(['adb',*map(str,args)],capture_output=True,timeout=timeout)
def check(*args):a=cmd(*args);a.check_returncode();return a.stdout.strip()
def classify(raw):
    try:
        doc=E.fromstring(raw)
        if doc.tag=='map':
            ev=doc.find("boolean[@name='user_enabled']")
            return 'READY_ENABLED' if ev is not None and ev.get('value')=='true' else 'READY_DISABLED' if ev is not None and ev.get('value')=='false' else 'PROJECTION_INCOMPLETE'
        return 'WRONG_ROOT'
    except E.ParseError:pass
    codes=['R335_ASSIGNMENT_NOT_STABLE','R335_ASSIGNMENT_UNREADABLE','R335_ASSIGNMENT_READ_ONLY_SHELL','R335_ASSIGNMENT_WRITE_IN_PROGRESS','SecurityException','Permission Denial','Unknown URI','FileNotFoundException','No content provider','DeadObjectException','NullPointerException','Unknown command','IllegalArgumentException']
    return next((c for c in codes if c.encode() in raw),'EMPTY_RESPONSE' if not raw else 'UNRECOGNIZED_PROVIDER_RESPONSE')
assert check('shell','getprop','ro.kernel.qemu')==b'1'
check('install',root/'B08-release.apk')
dest=check('shell','pm','path',pkg).decode().split('package:')[1];assert check('shell','sha256sum',dest).decode().split()[0]==target
check('shell','pm','grant',pkg,'android.permission.POST_NOTIFICATIONS')
check('shell','am','start','-W','-n',pkg+'/'+app+'.MainActivity')
cmd('unroot');check('wait-for-device');assert check('shell','id','-u')==b'2000'
sample={'role':'NEW_DISPOSABLE_EMULATOR','sourceCommit':'69eab914678bc988b71f2d7ce2287ccf89bd49eb','apkSha256':target,'api':int(check('shell','getprop','ro.build.version.sdk')),'shellUid':2000,'runId':int(os.environ['GITHUB_RUN_ID']),'observations':[]}
uri='content://'+pkg+'.r335.enabled/assignment'
for step in range(7):
    time.sleep(7)
    check('shell','uiautomator','dump','/sdcard/r335-control-display.xml')
    buf=check('shell','cat','/sdcard/r335-control-display.xml')
    ui=E.fromstring(buf);nodes={p.get('resource-id',''):p for p in ui.iter('node')}
    controls={key:{'present':pkg+':id/'+key in nodes,'enabled':nodes[pkg+':id/'+key].get('enabled')=='true' if pkg+':id/'+key in nodes else None} for key in ('btn_start','btn_stop')}
    action='OBSERVE'
    if step in (1,4) and controls['btn_start']['enabled']:
        xy=[int(k) for k in re.findall(r'\d+',nodes[pkg+':id/btn_start'].get('bounds',''))]
        if len(xy)==4 and xy[0]<xy[2] and xy[1]<xy[3]:
            check('shell','input','tap',int((xy[0]+xy[2])/2),int((xy[1]+xy[3])/2));action='TAP_VISIBLE_START'
    # Read only the production shell projection. Keep all response contents on this disposable guest.
    observation={'step':step,'atMs':int(time.time()*1000),'controls':controls,'action':action}
    for trans,argv in [('official',('exec-out','content','read','--uri',uri)),('shellUser0',('shell','content','read','--user','0','--uri',uri))]:
        v=cmd(*argv);observation[trans]={'exit':v.returncode,'category':classify(v.stdout+v.stderr)}
    service=check('shell','dumpsys','activity','services',pkg)
    observation['serviceListed']=('AutoFetchService' in service.decode(errors='replace'))
    sample['observations'].append(observation)
file=out/('R335-assignment-transport-'+str(sample['runId'])+'.json');file.write_text(json.dumps(sample,indent=2)+'\n')
assert file.stat().st_size<24000
