#!/usr/bin/env python3
"""Prepare compiled call evidence, then build through the unmodified Android gates.

prepare never emits a deliverable APK. Its temporary exclusions only allow actual
resource allocation before source-call evidence exists for the changed sources.
build runs all normal product generators/gates with no task exclusion. Commit the
refreshed tracked call archive and pin between these phases. No acceptance ledger
state, policy threshold, API key or signing input is altered by this program.
"""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess, sys, time, zipfile

ROOT = Path(__file__).resolve().parents[1]

def sha(path):
    h=hashlib.sha256()
    with Path(path).open('rb') as f:
        while b:=f.read(1048576):h.update(b)
    return h.hexdigest()

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--phase',choices=('prepare','build'),required=True)
    p.add_argument('--stage',required=True)
    p.add_argument('--gradle',type=Path,required=True)
    p.add_argument('--sdk',type=Path,required=True)
    p.add_argument('--jdk',type=Path,required=True)
    p.add_argument('--gradle-home',type=Path,required=True)
    p.add_argument('--evidence',type=Path,required=True)
    p.add_argument('--variant',choices=('debug','release'),default='debug')
    p.add_argument('--bootstrap-inputs',type=Path,help='packaged byte-pinned O01 provisional inputs, never an APK')
    p.add_argument('--construction-tool',type=Path)
    p.add_argument('--construction-manifest',type=Path)
    a=p.parse_args()
    if not __import__('re').fullmatch(r'O(?:0[1-9]|1[0-2])',a.stage):p.error('invalid stage')
    ev=a.evidence.resolve();ev.mkdir(parents=True,exist_ok=True)
    sdk=a.sdk.resolve();jdk=a.jdk.resolve();gradle=a.gradle.resolve()
    env=dict(os.environ,JAVA_HOME=str(jdk),ANDROID_HOME=str(sdk),ANDROID_SDK_ROOT=str(sdk),
             GRADLE_USER_HOME=str(a.gradle_home.resolve()))
    env['PATH']=str(jdk/'bin')+os.pathsep+env.get('PATH','')
    env.pop('STARCORE_SOURCE_RECEIPT',None) # ordinary Git build binds its own actual HEAD
    commands=[]
    def run(args,name):
        started=time.time();log=ev/(name+'.log')
        with log.open('wb') as out:
            r=subprocess.run([str(x) for x in args],cwd=ROOT,env=env,stdout=out,stderr=subprocess.STDOUT)
        commands.append({'argv':[str(x) for x in args],'log':log.name,'logSha256':sha(log),
                         'exitCode':r.returncode,'elapsedSeconds':round(time.time()-started,3)})
        (ev/(a.phase+'-commands.json')).write_text(json.dumps(commands,indent=2)+'\n')
        if r.returncode:
            print(log.read_text(errors='replace')[-5000:]);raise SystemExit(r.returncode)
    if bool(a.construction_tool)!=bool(a.construction_manifest):
        p.error('construction-tool and construction-manifest must be supplied together')
    construction_input=None
    if a.construction_tool:
        command=[sys.executable,str(a.construction_tool.resolve()),'--repo',str(ROOT),
                 '--manifest',str(a.construction_manifest.resolve()),'build-input',
                 '--expected-head',subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
                 '--stage',a.stage]
        construction_input=json.loads(subprocess.check_output(command,cwd=ROOT,env=env,text=True))
        if (construction_input.get('status')!='BUILD_INPUT_VERIFIED'
                or construction_input.get('producer_path')!='scripts/opt299_stage_build.py'
                or construction_input.get('producer_sha256')!=sha(__file__)):
            raise SystemExit('OPT299_CONSTRUCTION_BUILD_INPUT_REJECTED')
    def construction_receipt(scope,logname):
        if construction_input is None:return
        receipt_path=ROOT/'construction_state/optimized299/generated'/(a.stage+'.json')
        if construction_input.get('receipt_file')!=receipt_path.relative_to(ROOT).as_posix():
            raise SystemExit('OPT299_GENERATED_RECEIPT_PATH_DRIFT')
        artifacts=ROOT/'construction_state/optimized299/evidence'/a.stage
        artifacts.mkdir(parents=True,exist_ok=True)
        log=artifacts/(a.phase+'-'+logname)
        shutil.copy2(ev/logname,log)
        value={'stage':a.stage,'input_source_tree_hash':construction_input['input_source_tree_hash'],
               'producer_path':'scripts/opt299_stage_build.py','producer_sha256':sha(__file__),
               'exit_code':0,'scope':scope,
               'outputs':[{'path':name,'sha256':sha(ROOT/name)} for name in
                          ('release/r982_source_calls.tsv.gz','release/r982_source_calls.manifest.json',
                           'app/build/generated/r967/semantic_manifest.json',
                           'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java')],
               'artifacts':[{'path':log.relative_to(ROOT).as_posix(),'sha256':sha(log)}]}
        receipt_path.parent.mkdir(parents=True,exist_ok=True)
        receipt_path.write_text(json.dumps(value,indent=2)+'\n')
    g=[gradle,'--offline','--no-daemon','--max-workers=2','-Dorg.gradle.workers.max=2']
    if a.phase=='prepare':
        archive=ROOT/'release/r982_source_calls.tsv.gz'
        pin=ROOT/'release/r982_source_calls.manifest.json'
        if not archive.is_file():
            if a.stage!='O01' or not a.bootstrap_inputs:
                raise SystemExit('OPT299_MISSING_CURRENT_CALL_ARCHIVE_REQUIRES_PINNED_O01_BOOTSTRAP')
            expected={'r982_source_calls.tsv.gz':'82b4d55d019a030a63cea583cfaa1fd7a04835c86cd8194a7f4534a30c223138',
                      'r982_source_calls.manifest.json':'7c82acb8ac7f5718f68933136b6b6f0bc695cfe71f958dbd94467437bd2f9856'}
            for name,wanted in expected.items():
                src=a.bootstrap_inputs/name
                if not src.is_file() or sha(src)!=wanted:
                    raise SystemExit('OPT299_UNREVIEWED_BOOTSTRAP_INPUT:'+name)
            if pin.is_file():shutil.copy2(pin,ev/'unavailable-current-call-pin-before.json')
            for name in expected:shutil.copy2(a.bootstrap_inputs/name,ROOT/'release'/name)
            (ev/'bootstrap-input-boundary.json').write_text(json.dumps({
                'scope':'PROVISIONAL_OLD_CALL_ARCHIVE_ONLY_FOR_CURRENT_SOURCE_BOOTSTRAP',
                'currentMissingGitBlob':'e450f4830b24eb513935219286c7604b168b9d50',
                'sourceWasRecoveredAsSameBytes':False,'inputs':expected,
                'deliverableApkProducedBeforeCurrentFreeze':False},indent=2)+'\n')
        run([sys.executable,'scripts/r926_release_manifest.py','--check'],'release-identity')
        run([sys.executable,'scripts/verify_catalog_assets_cp12.py','--check'],'catalogs')
        # Neither excluded task owns resources, compilation or an authorization gate.
        # These commands produce only a bootstrap resource allocation/classes receipt.
        bootstrap_variant=a.variant.capitalize()
        run(g+[':app:compile'+bootstrap_variant+'JavaWithJavac','-x',':app:generateR982SourceCallAsset',
               '-x',':app:generateR997ConstructionAssets'],'bootstrap-actual-gradle-compile-NOT-APK')
        classes=ROOT/'app/build/intermediates/javac'/a.variant/('compile'+bootstrap_variant+'JavaWithJavac/classes')
        if not classes.is_dir():raise SystemExit('ACTUAL_BOOTSTRAP_COMPILED_CLASSES_MISSING')
        rjars=list((ROOT/'app/build/intermediates').glob('**/process'+bootstrap_variant+'Resources/R.jar'))
        if len(rjars)!=1:raise SystemExit('ACTUAL_VARIANT_R_JAR_NOT_UNIQUE:'+str(rjars))
        files=sorted((ROOT/'app/src/main/java').rglob('*.java'))
        run([sys.executable,'scripts/r982_source_call_asset.py','--freeze','--classes',classes],
            'freeze-actual-compiled-calls')
        run([sys.executable,'scripts/r967_semantic_identity.py'],'refresh-semantic-generated')
        run([sys.executable,'scripts/r967_semantic_identity.py','--check'],'verify-semantic-generated')
        run([sys.executable,'scripts/r967_semantic_identity.py','--self-test'],'semantic-mutations')
        receipt={'format':'OPT299_BUILD_PREPARE_V1','stage':a.stage,'scope':'COMPILED_BOOTSTRAP_NOT_APK_OR_RUNTIME_ACCEPTANCE',
                 'sourceCount':len(files),'runnerSha256':sha(__file__),
                 'rJarSha256':sha(rjars[0]),'callArchiveSha256':sha(ROOT/'release/r982_source_calls.tsv.gz'),
                 'callPinSha256':sha(ROOT/'release/r982_source_calls.manifest.json'),
                 'requiredNextStep':'COMMIT_TRACKED_CALL_ARCHIVE_AND_PIN_THEN_BUILD_PHASE'}
        (ev/'prepare-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
        construction_receipt('COMPILED_BOOTSTRAP_NOT_APK','freeze-actual-compiled-calls.log')
        print('OPT299_PREPARE_PASS|BOOTSTRAP_NOT_DELIVERABLE_APK|register-generated receipt, commit archive and pin, then build')
        return
    if subprocess.check_output(['git','status','--porcelain','--','app/src/main','app/build.gradle','build.gradle',
        'settings.gradle','gradle.properties','gradle','scripts','release','construction/r997'],cwd=ROOT).strip():
        raise SystemExit('OPT299_BUILD_INPUTS_UNCOMMITTED')
    variant=a.variant.capitalize()
    # Release retains the original Gradle checks for original key/certificate/API input.
    # Debug is an engineering APK and cannot cover-install an original release package.
    run(g+[':app:assemble'+variant,':app:testDebugUnitTest',':app:lintRelease'],'strict-android-'+a.variant)
    classes=ROOT/'app/build/intermediates/javac'/a.variant/('compile'+variant+'JavaWithJavac/classes')
    if not classes.is_dir():raise SystemExit('ACTUAL_FINAL_COMPILED_CLASSES_MISSING')
    archive=ROOT/'release/r982_source_calls.tsv.gz';pin=ROOT/'release/r982_source_calls.manifest.json'
    oldarchive,oldpin=archive.read_bytes(),pin.read_bytes()
    run([sys.executable,'scripts/r982_source_call_asset.py','--freeze','--classes',classes],
        'final-compiled-call-stability')
    if archive.read_bytes()!=oldarchive or pin.read_bytes()!=oldpin:
        raise SystemExit('FINAL_COMPILED_CALLS_CHANGED_REPREPARE_REQUIRED')
    # All final generated input identities must still match after compile/assembly.
    run(g+[':app:verifyFinalR967Semantics',':app:verifyFinalR815Provenance'],
        'final-generated-identities')
    apks=sorted((ROOT/'app/build/outputs/apk'/a.variant).glob('*.apk'))
    if len(apks)!=1:raise SystemExit('FINAL_APK_NOT_UNIQUE')
    apk=apks[0]
    apk_catalogs=[]
    with zipfile.ZipFile(apk) as z:
        if z.testzip() is not None:raise SystemExit('APK_ZIP_BAD')
        if not any(__import__('re').fullmatch(r'classes\d*\.dex',n) for n in z.namelist()):
            raise SystemExit('APK_PRODUCT_DEX_MISSING')
        abis={n.split('/')[1] for n in z.namelist() if n.startswith('lib/') and n.endswith('.so')}
        if abis!={'arm64-v8a','armeabi-v7a','x86_64'}:raise SystemExit('APK_NATIVE_ABI_SET_DRIFT:'+str(abis))
        for name in ('assets/cp4_atom_registry.jsonl','assets/research/cp4_molecule_seeds.jsonl'):
            info=z.getinfo(name)
            actual=hashlib.sha256(z.read(name)).hexdigest()
            if actual!=sha(ROOT/'app/src/main'/name):raise SystemExit('APK_CATALOG_BYTES_DRIFT:'+name)
            if info.compress_type!=zipfile.ZIP_STORED:
                raise SystemExit('APK_CATALOG_REQUIRES_OPENFD_ZIP_STORED:'+name)
            apk_catalogs.append({'path':name,'sha256':actual,'bytes':info.file_size,
                                 'compressedBytes':info.compress_size,'compression':'ZIP_STORED'})
    tools=sdk/'build-tools/36.0.0'
    run([tools/'aapt2','dump','badging',apk],'actual-apk-badging')
    run([tools/'apksigner','verify','--verbose','--print-certs',apk],'actual-apk-signature')
    release=json.loads((ROOT/'release/release_manifest.json').read_text())
    badging=(ev/'actual-apk-badging.log').read_text()
    expected=("package: name='"+release['applicationId']+"' versionCode='"+str(release['versionCode'])
              +"' versionName='"+release['versionName']+"'")
    if expected not in badging:raise SystemExit('APK_MANIFEST_RELEASE_IDENTITY_DRIFT')
    cert=__import__('re').findall(r'certificate SHA-256 digest: ([0-9a-f]{64})',
                                (ev/'actual-apk-signature.log').read_text())
    if len(cert)!=1:raise SystemExit('APK_SIGNER_CERTIFICATE_NOT_UNIQUE')
    destination=ev/apk.name;shutil.copy2(apk,destination)
    receipt={'format':'OPT299_ENGINEERING_APK_V1','stage':a.stage,'variant':a.variant,
             'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
             'sourceTree':subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=ROOT,text=True).strip(),
             'apkFile':destination.name,'apkSha256':sha(destination),'runnerSha256':sha(__file__),
             'callArchiveSha256':sha(archive),'nativeAbis':sorted(abis),'catalogAssets':apk_catalogs,
             'generatedOutputs':[{'path':name,'sha256':sha(ROOT/name)} for name in
                ('release/r982_source_calls.tsv.gz','release/r982_source_calls.manifest.json',
                 'app/build/generated/r967/semantic_manifest.json',
                 'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java')],
             'applicationId':release['applicationId'],'versionName':release['versionName'],
             'versionCode':release['versionCode'],'certificateSha256':cert[0],
             'deviceStatus':'NOT_RUN','naturalFutureStatus':'NOT_RUN',
             'overwriteInstallStatus':'NOT_RUN','complete299BusinessAcceptance':False}
    (ev/'build-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
    construction_receipt('APK_BUILD','strict-android-'+a.variant+'.log')
    print('OPT299_ANDROID_BUILD_PASS|variant='+a.variant+'|device=NOT_RUN|naturalFuture=NOT_RUN')

if __name__=='__main__':main()
