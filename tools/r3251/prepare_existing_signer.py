#!/usr/bin/env python3
"""Bind the existing installed signer and authorized API without exporting secrets."""
import hashlib,importlib.util,json,os,re,subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
CERT='d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7'
OLD_SHA='876f26e81114acfa3d2fa2d023b2e36309bec6dde3ef32f0e87c562b48d165ac'
def main():
 output=Path(os.environ['GITHUB_ENV']);temporary=Path(os.environ['RUNNER_TEMP'])
 key_root=Path.home()/'.starcore/r27-ci-temp-key';key=key_root/'r27-ci.p12';password_file=key_root/'pw'
 if not key.is_file() or not password_file.is_file():raise SystemExit('EXISTING_INSTALLED_SIGNER_NOT_RESTORED')
 password=password_file.read_text().strip()
 if not password or '\n' in password or '\r' in password:raise SystemExit('EXISTING_SIGNER_PASSWORD_FORMAT_INVALID')
 print('::add-mask::'+password,flush=True)
 environment=dict(os.environ,R3251_PRIVATE_SIGNER_PASS=password)
 exported=subprocess.run(['keytool','-exportcert','-keystore',str(key),'-storetype','PKCS12','-alias','starcore-r13','-storepass:env','R3251_PRIVATE_SIGNER_PASS'],env=environment,capture_output=True)
 if exported.returncode:raise SystemExit('EXISTING_SIGNER_PUBLIC_CERT_EXPORT_FAILED_EXIT_'+str(exported.returncode))
 actual=hashlib.sha256(exported.stdout).hexdigest()
 if actual!=CERT:raise SystemExit('INSTALLED_CERTIFICATE_MISMATCH:'+actual)
 old_apks=list((temporary/'original032').rglob('*.apk'))
 if len(old_apks)!=1:raise SystemExit('EXACT_ORIGINAL_032_APK_REQUIRED')
 baseline=old_apks[0]
 if hashlib.sha256(baseline.read_bytes()).hexdigest()!=OLD_SHA:raise SystemExit('ORIGINAL_032_APK_SHA_MISMATCH')
 api=os.environ.get('DUOQU_API_KEY','').strip();origin='EXISTING_DUOQU_API_KEY_REPOSITORY_BINDING'
 if not api:
  spec=importlib.util.spec_from_file_location('original_authorized_legacy_api_recovery',ROOT/'scripts/r995_restore_legacy_api_key.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
  api=module.release_key(baseline);origin='AUTHORIZED_DEFAULT_FROM_EXACT_ORIGINAL_032_APK'
 if not re.fullmatch(r'[A-Za-z0-9]{8,128}',api):raise SystemExit('EXISTING_API_BINDING_INVALID')
 print('::add-mask::'+api,flush=True)
 with output.open('a') as stream:
  for name,value in dict(R13_SIGNING_P12=str(key),R13_SIGNING_PASSWORD=password,R13_EXPECTED_CERT_SHA256=CERT,DUOQU_API_KEY=api,R3251_BASELINE_APK=str(baseline)).items():stream.write(name+'='+value+'\n')
 public=temporary/'r3251-ci';public.mkdir(exist_ok=True)
 receipt=dict(certificateSha256=actual,existingInstalledSignerRestored=True,replacementKeyGenerated=False,apiOrigin=origin,original032ApkSha256=OLD_SHA,privateKeyOrPasswordExported=False)
 (public/'signing-public.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt),flush=True)
if __name__=='__main__':main()
