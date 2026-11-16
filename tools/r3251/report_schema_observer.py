"""Build an Android read-only observer; never initialize or repair an application schema."""
import datetime, hashlib, json, os, subprocess, time, uuid, zipfile
from pathlib import Path

def prepare(out):
    source=Path(__file__).with_name('ReadOnlySchemaProbe.java')
    sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')))
    android=sdk/'platforms/android-36/android.jar';d8=sdk/'build-tools/36.0.0/d8'
    assert android.is_file() and d8.is_file(),'READONLY_OBSERVER_ANDROID36_TOOLCHAIN_MISSING'
    build=out/'read-only-schema-observer';build.mkdir(exist_ok=False)
    classes=build/'classes';classes.mkdir();dex=build/'dex';dex.mkdir()
    commands=[['javac','-source','8','-target','8','-cp',str(android),'-d',str(classes),str(source)],
              [str(d8),'--lib',str(android),'--output',str(dex),str(classes/'com/starcore/r3251probe/ReadOnlySchemaProbe.class')]]
    for i,argv in enumerate(commands):
        r=subprocess.run(argv,capture_output=True,text=True,timeout=120)
        (build/f'compile-{i}.log').write_text(r.stdout+r.stderr)
        assert r.returncode==0,'READONLY_OBSERVER_COMPILATION_FAILED'
    archive=build/'probe.jar'
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_STORED) as z:z.write(dex/'classes.dex','classes.dex')
    remote='/data/local/tmp/r3251-schema-observer-'+uuid.uuid4().hex+'.jar'
    subprocess.run(['adb','push',str(archive),remote],check=True,capture_output=True,timeout=30)
    return remote

def sample(remote):
    command='CLASSPATH='+remote+' app_process /system/bin com.starcore.r3251probe.ReadOnlySchemaProbe /data/user/0/com.starcore.research.r13/databases/binance_k3_nova_one_pro_v57.db'
    r=subprocess.run(['adb','shell',command],capture_output=True,text=True,timeout=20)
    rows=[line for line in r.stdout.splitlines() if line.startswith('{')]
    assert len(rows)==1,'READONLY_SCHEMA_OBSERVER_OUTPUT_INVALID'
    value=json.loads(rows[0]);assert r.returncode in (0,2,3),'READONLY_SCHEMA_OBSERVER_EXECUTION_FAILED'
    assert value.get('realAuthorized') is not True,'REAL_AUTHORIZATION_UNEXPECTED_IN_DISPOSABLE_REPORT_FIXTURE'
    return value

def wait(out,remote,*,require_ready,seconds):
    start=time.monotonic();deadline=start+seconds;observations=[];last=None
    while True:
        value=sample(remote);value['observedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat()
        observations.append(value)
        current=(value.get('criticalState'),value.get('researchState'),value.get('extensionRootCompleted'),value.get('researchCompletedUnits'),value.get('pendingExceptionClass'))
        if current!=last:print('R3251_READONLY_SCHEMA_PROGRESS|'+json.dumps({k:v for k,v in value.items() if k!='observedAt'},sort_keys=True),flush=True);last=current
        done=value.get('ready') is True if require_ready else value.get('realAuthorized') is False
        if done or time.monotonic()>=deadline:
            label='ready' if require_ready else 'real-switch-precondition'
            (out/('read-only-schema-'+label+'.json')).write_text(json.dumps(dict(scope='DISPOSABLE_EMULATOR_ORIGINAL_PRODUCTION_SQLITE_READONLY_OBSERVATIONS',elapsedSeconds=round(time.monotonic()-start,3),applicationDataWritten=False,observations=observations,passed=done),indent=2)+'\n')
            assert done,'PRODUCTION_RESEARCH_SCHEMA_NOT_READY' if require_ready else 'REAL_SWITCH_STATE_NOT_OBSERVED_BEFORE_SERVICE_START'
            return
        time.sleep(2)
