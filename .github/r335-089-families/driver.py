from pathlib import Path
import hashlib,json,re,os,subprocess as sp,urllib.request
base=Path(os.environ['RUNNER_TEMP']);guest='/data/local/tmp/r335-families.dex.jar';root='https://github.com/gtt46461-star/starcore-public-build/releases/download/';sdk=Path(os.environ['ANDROID_HOME']);pkg='com.starcore.research.r13'
pairs=[(577311087,'1b442343c626e6f8054524fe6eaadc01ff6c782b','94fdaf5091804f1685850a19ba42845b9b86304adb57f60b17cc6d73670f2498'),(577311089,'46637fe3b5a6aea2ee6c4f06a600975c72985160',os.environ['REPAIRED_APK_SHA'])]
def run(*a):return sp.check_output(list(map(str,a)),stderr=sp.STDOUT,timeout=400).decode().strip()
def adb(*a):return run('adb',*a)
def sha(p):
 with p.open('rb')as f:return hashlib.file_digest(f,'sha256').hexdigest()
out=base/('R335-089-families-'+os.environ['GITHUB_RUN_ID']+'.json')
doc={'sourceCommit':pairs[1][1],'apkSha256':pairs[1][2],'scope':'DISPOSABLE_ANDROID_14_SIGNED_RELEASE_CODE_CONTROLLED_FORMULAS_AND_HOT_CALENDAR','testDexSha256':sha(base/'observer.jar'),'runs':[]}
def save():out.write_text(json.dumps(doc,indent=2)+'\n')
save();adb('push',base/'observer.jar',guest);adb('shell','chmod','444',guest)
for ver,commit,digest in pairs:
 name='STARCORE-R33-'+str(ver)+'-'+commit[:12]+'-release.apk';local=base/name;sp.run(['curl','-fLsS','--retry','4',root+'r335-B08-'+commit[:12]+'/'+name,'-o',local],check=True)
 assert sha(local)==digest;cert=run(sdk/'build-tools/36.0.0/apksigner','verify','--print-certs',local);assert 'Signer #1 certificate SHA-256 digest: d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7' in cert
 assert 'Success' in adb('install','-r',local);apk=adb('shell','pm','path',pkg).split(':',1)[1];assert adb('shell','sha256sum',apk).split()[0]==digest
 uid=re.search(r'(?:appId|userId)=(\d+)',adb('shell','dumpsys','package',pkg))[1]
 if ver==577311087:doc['originalUid']=uid
 else:assert doc['originalUid']==uid;doc['previousUidRetained']=True
 modes=[(1,'old-regime')] if ver==577311087 else [(1,'families')]+[(p,'variants') for p in range(1,11)]+[(0,'hot-window')]
 for position,mode in modes:
  argv=['adb','shell','env','CLASSPATH='+guest+':'+apk,'app_process','-Xmx192m','/system/bin']
  argv+=['com.ethk3.novaonepro.SignedTenThousandHotWindowCheck','/data/local/tmp/ten-thousand-r335.sqlite'] if mode=='hot-window' else ['com.ethk3.novaonepro.r952.SignedRegisteredFamilies',apk,str(position),mode]
  z=sp.run(argv,stdout=sp.PIPE,stderr=sp.STDOUT,timeout=1400)
  lines=z.stdout.decode(errors='replace').splitlines();family=[l.split() for l in lines if re.fullmatch(r'FAMILY [A-Z]+ [a-f0-9]{64} [a-f0-9]{64}',l)];result=[l for l in lines if l.startswith(('SIGNED_FAMILIES PASS ','HOT_WINDOW_SIGNED ','REGIME:NO_OP_32_ATTEMPTS:JSON_SIZE'))]
  doc['runs'].append({'source':commit,'apkSha256':digest,'position':position,'mode':mode,'exit':z.returncode,'result':result,'families':family,'logSha256':hashlib.sha256(z.stdout).hexdigest()});save()
  if mode=='old-regime':assert z.returncode!=0 and any('REGIME:NO_OP_32_ATTEMPTS:JSON_SIZE' in l for l in result)
  elif mode=='hot-window':assert z.returncode==0 and any('inserted=10000 hot=5000 retired=5000 pinnedBeforeConsumption=1 pinnedAfterConsumption=0 rollback=true reopens=2 calendarMissing=0 integrity=ok' in l for l in result)
  else:assert z.returncode==0 and len(result)==1 and (len(family)==15 if mode=='families' else 'regimeVariants=4' in result[0])
doc['completed']=True;save()
