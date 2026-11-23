"""Source, version and numerical-ABI regression cases using the project owners."""
from contextlib import redirect_stdout
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import zipfile


def load(path):
    spec = importlib.util.spec_from_file_location(path.stem, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def version_case(source, out):
    owner = load(source / 'tools/r33/advance_release_version.py')
    copied = ('scripts/r926_release_manifest.py', *owner.FILES)
    checks = []
    with tempfile.TemporaryDirectory(prefix='r34-version-case-') as tmp:
        private = Path(tmp) / 'copy'
        for name in copied:
            dst = private / name; dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source / name, dst)
        generator = private / copied[0]
        base = json.loads((source / owner.FILES[0]).read_bytes())
        def check(label, expected_pass):
            p = subprocess.run([sys.executable, '-B', generator, '--check'], cwd=private, capture_output=True)
            (out / (label + '.log')).write_bytes(p.stdout + p.stderr)
            if (p.returncode == 0) != expected_pass:
                raise AssertionError(label + '_WRONG_VERDICT')
            checks.append({'input': label, 'exit': p.returncode})
        check('original-four', True)
        for index, name in enumerate(owner.FILES):
            path = private / name; initial = path.read_bytes()
            if index == 0:
                bad = dict(base, versionCode=base['versionCode'] + 1)
                path.write_bytes(json.dumps(bad).encode())
            else:
                current = str(base['versionCode']).encode()
                if current not in initial: raise AssertionError('VERSION_LITERAL_REQUIRED')
                path.write_bytes(initial.replace(current,str(base['versionCode'] - 1).encode(),1))
            check('tamper-' + str(index), False)
            path.write_bytes(initial)
        check('original-restored', True)
    return {'status':'PASS', 'checks': checks, 'scope':'ACTUAL_GENERATOR_ALL_FOUR_IDENTITIES_AND_FOUR_TAMPERED_INPUTS'}


def archive_case(source, out):
    # Use the true replacement and addition set since 095 and the packaging owner's
    # own Git mode, blob identity, CRC and entire declared-member verification.
    sys.path.insert(0, str(source / 'tools/r33'))
    pkg = load(source / 'tools/r33/package_ci_candidate.py')
    def git(*args): return subprocess.check_output(['git','--no-replace-objects','-C',source,*args])
    tip=git('rev-parse','HEAD').decode().strip()
    all_entries=pkg.source_tree_entries(source, tip)
    base=json.loads((source/'release/r34_plan_binding.json').read_bytes())['sourceBaseline']
    changed=git('diff','--name-only','--diff-filter=AM','-z',base,tip).decode().split('\0')
    files={n:all_entries[n] for n in changed if n and n.startswith('app/src/main/')}
    if len(files)<2: raise AssertionError('EXPECTED_PRODUCTION_PAYLOAD')
    content={n:git('cat-file','blob',oid) for n,(_,oid) in files.items()}
    target=next(iter(files)); checks=[]
    with tempfile.TemporaryDirectory(prefix='r34-archive-case-') as tmp:
        for label in ('original','omission','altered'):
            zpath=Path(tmp)/'current.zip'
            with zipfile.ZipFile(zpath,'w',zipfile.ZIP_DEFLATED) as z:
                for n,(mode,_) in files.items():
                    if n==target and label=='omission':continue
                    info=zipfile.ZipInfo(n);info.create_system=3;info.external_attr=pkg.source_zip_mode(mode)<<16
                    payload=content[n]+(b'\0' if n==target and label=='altered' else b'')
                    z.writestr(info,payload,compress_type=zipfile.ZIP_DEFLATED)
            try:
                pkg.verify_source_archive(zpath,files)
                issue=None
            except ValueError as e: issue=str(e)
            if (label=='original') != (issue is None): raise AssertionError('SOURCE_ARCHIVE_'+label)
            checks.append({'input':label, 'decision':issue or 'ACCEPTED'})
    return {'status':'PASS','checks':checks,'sourceCommit':tip,'baseline':base,
            'scope':'EVERY_ADDED_OR_REPLACED_PRODUCTION_APP_FILE_IN_THIS_RELEASE_DIFF','files':list(files)}


def semantic_case(source, output):
    script=source/'scripts/r967_semantic_identity.py';results=[]
    for switch in ('--check','--self-test'):
        name='r967-'+switch.removeprefix('--')+'.log'
        with (output/name).open('wb') as log:
            code=subprocess.run([sys.executable,'-B',script,switch],cwd=source,stdout=log,stderr=subprocess.STDOUT,timeout=480).returncode
        results.append({'command':switch,'exit':code,'log':name})
    return {'status':'PASS' if all(x['exit']==0 for x in results) else 'FAIL',
            'checks':results,'scope':'ORIGINAL_R967_GENERATOR_AND_ITS_EIGHT_DEPENDENCY_MUTATIONS'}


def admission_case(source, out):
    owner=load(source/'tools/r33/advance_release_version.py')
    current=json.loads((source/owner.FILES[0]).read_bytes())
    floor=current['versionCode']+13
    with tempfile.TemporaryDirectory(prefix='r34-version-floor-') as t:
        journal=Path(t)/'journal'
        owner.prepare(source,journal,floor,'B12','2026-11-23','R34')
        updated=json.loads((journal/'after'/owner.FILES[0]).read_bytes())
        if updated['versionCode']!=floor+1 or not updated['versionName'].endswith('-'+str(floor+1)):
            raise AssertionError('HIGHER_INSTALLED_VERSION_LOST')
        try:
            owner.prepare(source,journal,577311095,'B12','2026-11-23','R34')
        except ValueError as e:
            if 'FRESH_EXTERNAL_VERSION_JOURNAL_REQUIRED' not in str(e): raise
        else: raise AssertionError('OVERWROTE_EXISTING_TRANSACTION')
    return {'status':'PASS', 'scope':'ORIGINAL_VERSION_TRANSACTION_HIGHER_INSTALLED_VERSION_AND_REUSED_JOURNAL',
            'sourceVersion':current['versionCode'],'installedFixture':floor,'newVersion':updated['versionCode']}


CASES={'version-four':version_case,'archive-inputs':archive_case,'r967':semantic_case,'version-floor':admission_case}
BY_ID={'D01':'archive-inputs','D03':'version-four','D04':'version-floor','D07':'version-floor','D13':'r967',
       **{'ID-'+s:'r967' for s in ('P00','B01','B02','B03','B04','B05','B07')}}


def build_environment_case(source, out):
    settings = ['ANDROID_HOME','STARCORE_QA_BASELINE_APK','STARCORE_QA_SIGNED_APK']
    missing = [key for key in settings if not os.environ.get(key)]
    if missing: return {'status':'OPEN','reason':'NEED_ORIGINAL_BUILD_INPUTS','inputs':missing}
    sdk=Path(os.environ['ANDROID_HOME']);baseline=Path(os.environ['STARCORE_QA_BASELINE_APK']);candidate=Path(os.environ['STARCORE_QA_SIGNED_APK'])
    sys.path.insert(0,str(source/'tools/r33'))
    preflight=load(source/'tools/r33/preflight_build.py');pkg=load(source/'tools/r33/package_ci_candidate.py')
    positive=preflight.collect(source,sdk)
    with tempfile.TemporaryDirectory(prefix='r34-missing-sdk-') as temporary:
        no_sdk=preflight.collect(source,Path(temporary))
        old_java=os.environ.get('JAVA_HOME')
        try:
            os.environ['JAVA_HOME']=temporary
            no_jdk=preflight.collect(source,sdk)
        finally:
            if old_java is None: os.environ.pop('JAVA_HOME',None)
            else: os.environ['JAVA_HOME']=old_java
    if positive['status']!='READY_TO_ATTEMPT_BUILD' or any(x['status']=='READY_TO_ATTEMPT_BUILD' for x in (no_sdk,no_jdk)):
        return {'status':'FAIL','validPreflight':positive,'missingSdk':no_sdk,'missingJdk':no_jdk}
    tools=sdk/'build-tools/36.0.0';manifest=json.loads((source/'release/release_manifest.json').read_bytes())
    old,_,_=pkg.inspect(baseline,tools/'aapt2',tools/'apksigner');new,_,_=pkg.inspect(candidate,tools/'aapt2',tools/'apksigner')
    pkg.validate_pair(manifest,old,new)
    if new['versionCode']!=manifest['versionCode']:raise AssertionError('WRONG_TARGET')
    try:pkg.validate_pair(manifest,old,dict(new,certificateSha256='0'*64))
    except ValueError as err:
        if str(err)!='SIGNER_CHANGED':raise
    else:raise AssertionError('WRONG_SIGNER_ACCEPTED')
    return {'status':'PASS','scope':'ORIGINAL_PREFLIGHT_AND_ACTUAL_SIGNED_APK_PAIR', 'apkSha256':new['sha256'],
            'certificateSha256':new['certificateSha256'],'checks':positive['checks'],
            'negative':['ABSENT_SDK_BLOCKED','ABSENT_JDK_BLOCKED','WRONG_CERTIFICATE_REJECTED']}


CASES['build-environment']=build_environment_case
BY_ID['D02']='build-environment'
