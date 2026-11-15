#!/usr/bin/env python3
"""Verify and package an actual signed candidate; never promote device/quality status."""
import argparse,hashlib,json,os,re,shutil,subprocess,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
CERT='d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7'
def digest(path):
 with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()
def run(args,output):
 with output.open('xb') as log:subprocess.run(list(map(str,args)),cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
def only_apk(directory):
 paths=sorted(directory.glob('*.apk'))
 if len(paths)!=1:raise SystemExit('EXPECTED_EXACTLY_ONE_ACTUAL_APK:'+str(directory)+':'+str(len(paths)))
 return paths[0]
def certificate(text):
 match=re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})\b',text)
 if not match or match.group(1)!=CERT:raise SystemExit('INSTALLED_SIGNER_MISMATCH')
 return match.group(1)
def package_identity(text):
 match=re.search(r"^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'",text,re.M)
 if not match:raise SystemExit('ACTUAL_APK_BADGING_MISSING')
 return dict(applicationId=match.group(1),versionCode=int(match.group(2)),versionName=match.group(3))
def main():
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--out',required=True,type=Path);parser.add_argument('--baseline-apk',type=Path);args=parser.parse_args();out=args.out.resolve();out.mkdir(parents=True,exist_ok=True)
 sdk=Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0';release=json.loads((ROOT/'release/release_manifest.json').read_text());commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip();tree=subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=ROOT,text=True).strip()
 assert release['applicationId']=='com.starcore.research.r13' and release['versionCode']==577311033 and release['versionName']=='R32.5-REPAIR-2026.11.15-033'
 original=only_apk(ROOT/'app/build/outputs/apk/release')
 run([sdk/'apksigner','verify','--verbose','--print-certs',original],out/'public-certificate.txt')
 cert=(out/'public-certificate.txt').read_text();certificate(cert)
 run([sdk/'aapt','dump','badging',original],out/'badging.txt');badging=(out/'badging.txt').read_text();assert "package: name='com.starcore.research.r13' versionCode='577311033' versionName='R32.5-REPAIR-2026.11.15-033'" in badging
 name='STARCORE-R32.5.1-C1-033-'+commit[:12]+'-release.apk';apk=out/name;shutil.copy2(original,apk);assert digest(apk)==digest(original)
 for variant in ['debug','androidTest/debug']:
  source=only_apk(ROOT/'app/build/outputs/apk'/variant);dest=out/'android-build'/('app-debug.apk' if variant=='debug' else 'app-debug-androidTest.apk');dest.parent.mkdir(exist_ok=True);shutil.copy2(source,dest)
  public_name=variant.replace('/','-')+'-certificate.txt';run([sdk/'apksigner','verify','--verbose','--print-certs',source],out/public_name);certificate((out/public_name).read_text())
 receipt=dict(kind='ACTUAL_SIGNED_RELEASE_CANDIDATE',sourceCommit=commit,sourceTree=tree,baselineCommit='7424eeae6354eb6599dd8498e123c059bd410c90',planCommit='0e932cc14fa0d72dd67546488915fa038f201501',originalPackageSha256='ceb5e3516452524de2519781bff04ba9f844bb95f95052bd4c2778928b856966',applicationId=release['applicationId'],versionCode=release['versionCode'],versionName=release['versionName'],certificateSha256=CERT,apk=name,apkSha256=digest(apk),apkBytes=apk.stat().st_size,ciRun=os.environ.get('GITHUB_RUN_ID'),whole222BusinessAcceptance=False,physical032Overlay='NOT_RUN_NO_ORIGINAL_DEVICE_DATABASE',physical24h='NOT_RUN',physical72h='NOT_RUN',naturalQuality='NOT_PROVEN',realActionSwitch='NOT_ENABLED_BY_THIS_WORKFLOW',sourceDownload='https://github.com/gtt46461-star/starcore-public-build/archive/'+commit+'.zip')
 if args.baseline_apk:
  baseline=args.baseline_apk.resolve();assert digest(baseline)=='876f26e81114acfa3d2fa2d023b2e36309bec6dde3ef32f0e87c562b48d165ac','ORIGINAL_032_APK_SHA_MISMATCH'
  run([sdk/'apksigner','verify','--verbose','--print-certs',baseline],out/'original032-certificate.txt');certificate((out/'original032-certificate.txt').read_text())
  run([sdk/'aapt','dump','badging',baseline],out/'original032-badging.txt');old=package_identity((out/'original032-badging.txt').read_text());new=package_identity(badging)
  assert old['applicationId']==new['applicationId'] and old['versionCode']==577311032<new['versionCode'],'OVERLAY_IDENTITY_OR_VERSION_MISMATCH'
  receipt['overlayCompatibility']=dict(sameApplicationId=True,sameCertificate=True,higherVersionCode=True,original032ApkSha256=digest(baseline),oldVersionCode=old['versionCode'],newVersionCode=new['versionCode'],actualInstall='REQUIRES_SEPARATE_ANDROID_EXECUTION',originalUserDatabaseIncluded=False)
 (out/'source-commit.txt').write_text(commit+'\n')
 (out/'DELIVERY.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');(out/'APK_SHA256SUMS.txt').write_text(receipt['apkSha256']+'  '+name+'\n')
 bundle=out/('STARCORE_R3251_C1_033_'+commit[:12]+'_DELIVERY.zip')
 with zipfile.ZipFile(bundle,'x',zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
  for path in [apk,out/'DELIVERY.json',out/'public-certificate.txt',out/'badging.txt',out/'APK_SHA256SUMS.txt']:archive.write(path,path.name)
  for path in sorted((ROOT/'verification/r3251-construction').rglob('*')):
   if path.is_file():archive.write(path,'evidence/'+path.relative_to(ROOT/'verification/r3251-construction').as_posix())
  for path in sorted((ROOT/'app/src/main/java').rglob('*.java')):archive.write(path,'source/'+path.relative_to(ROOT).as_posix())
 with zipfile.ZipFile(bundle) as archive,archive.open(name) as inside,apk.open('rb') as outside:
  while True:
   left,right=inside.read(1<<20),outside.read(1<<20)
   assert left==right,'ZIP_APK_BYTES_DIFFER'
   if not left:break
 (out/'SHA256SUMS.txt').write_text(''.join(digest(path)+'  '+path.name+'\n' for path in [apk,bundle,out/'DELIVERY.json']))
 print(json.dumps(dict(sourceCommit=commit,apk=apk.name,apkSha256=digest(apk),zip=bundle.name,zipSha256=digest(bundle),zipApkSameBytes=True,scope='SIGNED_CANDIDATE_NOT_DEVICE_OR_NATURAL_SIGNOFF')))
if __name__=='__main__':main()
