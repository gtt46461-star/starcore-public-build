"""Formal complete C024 Android build, real call fixed point and scoped contracts."""
from pathlib import Path
import base64,gzip,hashlib,importlib.util,json,os,re,secrets,shutil,subprocess,tempfile,traceback,zipfile
ROOT=Path(__file__).resolve().parents[3];R=ROOT/'construction/c_chain_20261018/C021-C024';BRANCH='construction/c021-c024-20261021';PRIVATE=Path(tempfile.mkdtemp(prefix='c024-build-',dir=os.environ['RUNNER_TEMP']));OUT=PRIVATE/'output';OUT.mkdir();COMMANDS=[];ENV=dict(os.environ,OPENBLAS_NUM_THREADS='1',OMP_NUM_THREADS='1',PYTHONDONTWRITEBYTECODE='1');SOURCE=None
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def dump(p,x):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(x,sort_keys=True,ensure_ascii=False,indent=2)+'\n')
def run(name,args,cwd=ROOT,check=True,timeout=2700):
 args=list(map(str,args));log=OUT/(name+'.log')
 with log.open('w') as f:code=subprocess.run(args,cwd=cwd,env=ENV,stdout=f,stderr=subprocess.STDOUT,timeout=timeout).returncode
 row=dict(name=name,argv=args,cwd=str(cwd),exit_code=code,log_sha256=sha(log));COMMANDS.append(row);dump(OUT/'commands.json',COMMANDS);print('COMMAND|'+json.dumps(row),flush=True)
 if code and check:raise RuntimeError(name+': '+log.read_text(errors='replace')[-7000:])
 return code
def classes(ct):
 m={f.relative_to(ct).as_posix():sha(f) for f in sorted(ct.rglob('*.class'))};assert len(m)>3000
 return dict(class_files=len(m),tree_sha256=hashlib.sha256(json.dumps(m,sort_keys=True,separators=(',',':')).encode()).hexdigest(),sha256=m)
def freeze(ct):
 code="import importlib.util,pathlib,sys;r=pathlib.Path(sys.argv[1]);s=importlib.util.spec_from_file_location('idx',r/'scripts/r975_source_call_index.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m);m.OUT=r/'app/build/generated/assets/r982/research/r975_source_calls.tsv';sys.argv=['idx','--classes',sys.argv[2]];m.main()"
 run('freeze-current-complete-calls-'+str(len(COMMANDS)),['python3','-c',code,ROOT,ct]);raw=(ROOT/'app/build/generated/assets/r982/research/r975_source_calls.tsv').read_bytes();(ROOT/'release/r982_source_calls.tsv.gz').write_bytes(gzip.compress(raw,mtime=0));dump(ROOT/'release/r982_source_calls.manifest.json',dict(bytes=len(raw),scope='STATIC_COMPILED_CALLS_NOT_RUNTIME_COVERAGE',sha256=hashlib.sha256(raw).hexdigest()))
def checkpoint(label):
 changed=set(subprocess.check_output(['git','diff','--name-only','HEAD','--'],cwd=ROOT,text=True).splitlines());assert changed<=set(['release/r982_source_calls.tsv.gz','release/r982_source_calls.manifest.json']),changed
 if changed:
  run(label+'-stage',['git','add','-f','--',*sorted(changed)]);run(label+'-diff-check',['git','diff','--cached','--check']);run(label+'-commit',['git','commit','-m','build(C024): bind actual complete '+label+' invocation inventory'])
 return subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
def main():
 global SOURCE
 assert os.environ['GITHUB_REF']=='refs/heads/'+BRANCH
 for k,v in [('user.name','github-actions[bot]'),('user.email','41898282+github-actions[bot]@users.noreply.github.com')]:run('git-'+k,['git','config',k,v])
 expected=json.loads((R/'local-final-source-index.json').read_text());assert {n:sha(ROOT/n) for n in expected}==expected,'PUSHED_SOURCE_DIFFERS_FROM_ACTUAL_LOCAL_APPLICATION'
 inp=json.loads((R/'test-inputs.json').read_text());raw=base64.b64decode(''.join((R/n).read_text().strip() for n in inp['parts']));assert hashlib.sha256(raw).hexdigest()==inp['sha256'];pack=PRIVATE/'inputs.zip';pack.write_bytes(raw)
 workspace=PRIVATE/'checks-work';workspace.mkdir()
 with zipfile.ZipFile(pack) as z:z.extractall(workspace)
 api=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';aapt=api.parents[2]/'build-tools/36.0.0/aapt2'
 compile_script=workspace/'plan/batching/tools/verify_stage_source.py'
 # Host and Android javac inventories differ. Preserve each rejection and bind
 # only the index measured from that complete compiler output.
 previous=None
 for attempt in range(1,4):
  dest=PRIVATE/('strict-'+str(attempt));args=['python3',compile_script,'--project-root',ROOT,'--android-jar',api,'--aapt2',aapt,'--out',dest]
  code=run('official-tools-strict-'+str(attempt),args,check=False)
  if code:
   failed=json.loads((dest/'failed-command.json').read_text());assert failed[-1]['name']=='source-call-bytecode-check' and (dest/'source-call-bytecode-check.log').read_text().strip()=='SOURCE_CALL_INDEX_STALE';freeze(dest/'classes');previous=None;continue
  rec=json.loads((dest/'receipt.json').read_text());shutil.copytree(dest,OUT/('strict-'+str(attempt)),ignore=shutil.ignore_patterns('classes','symbols','*.apk','resources.zip'))
  if previous==rec['class_tree_sha256']:break
  previous=rec['class_tree_sha256']
 else:raise RuntimeError('NO_TWO_REPEATABLE_STRICT_OFFICIAL_TOOL_COMPILES')
 # Build using a short-lived intermediate certificate; only public APK bytes
 # leave this runner. The authorized original private key remains local.
 run('fetch-authorized-api-key-source',['git','fetch','origin','80b09c5e5921f18c4ef05ceee7332ad0ee621944','--depth=1'])
 keysrc=subprocess.check_output(['git','show','80b09c5e5921f18c4ef05ceee7332ad0ee621944:app/src/main/java/com/ethk3/novaonepro/KeyProvider.java'],cwd=ROOT,text=True);key=''.join(re.findall(r"'([^'])'",keysrc)[:10]);assert hashlib.sha256(key.encode()).hexdigest()=='dc1aeda3c112739b900063c5d9921947c8291f6b8ba9c4a0556b378a3f97f96c';password=secrets.token_urlsafe(32);print('::add-mask::'+key);print('::add-mask::'+password)
 ENV.update(DUOQU_API_KEY=key,CI_INTERMEDIATE_PASS=password,STARCORE_SIGNING_PASSWORD=password,STARCORE_SIGNING_KEY_PASSWORD=password,STARCORE_SIGNING_ALIAS='ci-intermediate',STARCORE_SIGNING_TYPE='PKCS12',STARCORE_SIGNING_P12=str(PRIVATE/'intermediate.p12'))
 run('temporary-intermediate-signing-key',['keytool','-genkeypair','-alias','ci-intermediate','-keyalg','RSA','-keysize','2048','-validity','3','-dname','CN=STARCORE-C024-CI-INTERMEDIATE','-storetype','PKCS12','-keystore',PRIVATE/'intermediate.p12','-storepass:env','CI_INTERMEDIATE_PASS','-keypass:env','CI_INTERMEDIATE_PASS']);run('temporary-public-certificate',['keytool','-exportcert','-alias','ci-intermediate','-keystore',PRIVATE/'intermediate.p12','-storepass:env','CI_INTERMEDIATE_PASS','-file',PRIVATE/'intermediate.der']);ENV['STARCORE_EXPECTED_CERT_SHA256']=sha(PRIVATE/'intermediate.der')
 ct=ROOT/'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes'
 for attempt in range(1,4):
  checkpoint('official-call-fixed-point-'+str(attempt))
  run('normal-Android-release-'+str(attempt),['bash','./gradlew','--no-daemon','--no-build-cache','--stacktrace',':app:assembleRelease'])
  check="import importlib.util,pathlib,sys;r=pathlib.Path(sys.argv[1]);s=importlib.util.spec_from_file_location('idx',r/'scripts/r975_source_call_index.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m);m.OUT=r/'app/build/generated/assets/r982/research/r975_source_calls.tsv';sys.argv=['idx','--classes',sys.argv[2],'--check'];m.main()"
  if run('formal-real-call-fixed-point-'+str(attempt),['python3','-c',check,ROOT,ct],check=False)==0:break
  assert attempt<3;freeze(ct)
 SOURCE=checkpoint('formal-source');assert not subprocess.check_output(['git','diff','--name-only','HEAD','--'],cwd=ROOT,text=True).strip()
 run('normal-Android-unit-lint',['bash','./gradlew','--no-daemon','--no-build-cache','--stacktrace',':app:testDebugUnitTest',':app:lintRelease'])
 before=classes(ct);dump(OUT/'formal-release-class-tree.json',before)
 (workspace/'toolchain').mkdir(exist_ok=True);(workspace/'toolchain/android-platform-36').mkdir();(workspace/'toolchain/android-platform-36/android.jar').symlink_to(api);(workspace/'toolchain/jni').mkdir();shutil.copy2(Path(os.environ['JAVA_HOME'])/'include/jni.h',workspace/'toolchain/jni/jni.h');shutil.copy2(Path(os.environ['JAVA_HOME'])/'include/linux/jni_md.h',workspace/'toolchain/jni/jni_md.h')
 for bid in ['C021','C022','C023','C024']:
  dest=workspace/'build'/bid/'actual-applied';dest.mkdir(parents=True);(dest/'classes').symlink_to(ct,target_is_directory=True);snap=workspace/'stage-sources'/('final-'+bid);snap.parent.mkdir(exist_ok=True);snap.symlink_to(ROOT,target_is_directory=True)
  run('formal-cumulative-'+bid,['python3',workspace/'tools/check_stages.py',bid],cwd=workspace)
 shutil.copytree(workspace/'checks',OUT/'formal-contracts');assert before==classes(ct),'FORMAL_PRODUCT_CLASSES_MODIFIED_BY_TESTS'
 run('semantic-negative-mutations',['python3',ROOT/'scripts/r967_semantic_identity.py','--self-test']);run('release-identity-drift-tests',['python3',ROOT/'scripts/r926_release_manifest.py','--drift-test'])
 apk=next((ROOT/'app/build/outputs/apk/release').glob('*.apk'));shutil.copy2(apk,OUT/'STARCORE_C021_C024_5.4.2.95_intermediate.apk')
 final={n:sha(ROOT/n) for n in expected};dump(OUT/'final-source-index.json',final)
 dump(OUT/'receipt.json',dict(status='PASS_FORMAL_ANDROID_RELEASE_UNIT_LINT_AND_FOUR_CURRENT_COMPLETE_CLASS_CONTRACTS',source_commit=SOURCE,canonical_plan_sha256='50497444e626f1cc49b90add152ac042bb525c3fd775a54b54bceb8b8356c988',source_index_sha256=sha(OUT/'final-source-index.json'),formal_class_tree_sha256=before['tree_sha256'],versionCode=577251054,versionName='V57.21.2.0.95-R5.4.2.95-1021-C024',APK_sha256=sha(OUT/'STARCORE_C021_C024_5.4.2.95_intermediate.apk'),original_signature='PENDING_LOCAL_ORIGINAL_KEY',device_live_database_natural_data='WAIT_NOT_CLAIMED',next_batch='C025'))
 run('ordinary-source-push',['git','push','origin','HEAD:refs/heads/'+BRANCH])
def publish():
 dump(OUT/'commands.json',COMMANDS);dest=R/'runs'/os.environ['GITHUB_RUN_ID'];dest.mkdir(parents=True,exist_ok=True);components=[]
 for name in ['receipt.json','commands.json','final-source-index.json']:
  if(OUT/name).exists():shutil.copy2(OUT/name,dest/name)
 archive=PRIVATE/'STARCORE_C021_C024_formal_evidence.zip'
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
  for p in OUT.rglob('*'):
   if p.is_file() and p.suffix not in ['.apk','.class','.db','.sqlite','.p12','.der']:z.write(p,p.relative_to(OUT).as_posix())
 for f in [archive,OUT/'STARCORE_C021_C024_5.4.2.95_intermediate.apk']:
  if not f.exists():continue
  parts=[]
  with f.open('rb') as stream:
   i=0
   while raw:=stream.read(64*1024):
    name=f.stem+'-'+f'{i:04d}'+'.b64';q=dest/'transport'/name;q.parent.mkdir(exist_ok=True);q.write_text(base64.b64encode(raw).decode()+'\n');parts.append(dict(name=name,bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest()));i+=1
  components.append(dict(name=f.name,bytes=f.stat().st_size,sha256=sha(f),parts=parts))
 dump(dest/'transport/manifest.json',dict(components=components,run_id=os.environ['GITHUB_RUN_ID']));run('stage-evidence',['git','add','-f','--',dest.relative_to(ROOT)]);run('evidence-diff-check',['git','diff','--cached','--check']);run('commit-evidence',['git','commit','-m','test(C021-C024): preserve complete source build contracts and public APK']);run('ordinary-evidence-push',['git','push','origin','HEAD:refs/heads/'+BRANCH])
try:main()
except BaseException as e:dump(OUT/'receipt.json',dict(status='FAIL_C024_FORMAL_BUILD_OR_CONTRACTS',error=str(e),traceback=traceback.format_exc(),source_commit=SOURCE));traceback.print_exc()
finally:publish()
if not json.loads((OUT/'receipt.json').read_text())['status'].startswith('PASS_'):raise SystemExit(1)
