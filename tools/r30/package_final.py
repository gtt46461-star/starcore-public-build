#!/usr/bin/env python3
"""Select one actual release APK for one existing identity; inspect only public APK metadata."""
import argparse,hashlib,json,os,re,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--lane',choices=['OFFICIAL_R13','EXISTING_R27'],required=True);p.add_argument('--input',type=Path,default=Path('r29-apk'));p.add_argument('--output',type=Path,default=Path('r30-final'));p.add_argument('--expected-source',required=True);a=p.parse_args()
source=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip();assert source==a.expected_source,'SOURCE_CHANGED_DURING_BUILD'
expected={'OFFICIAL_R13':'9e6d5dc79d29d4ba5b435a171e6535386f8a56ea6fad71a6cc77aeb10ade3eca','EXISTING_R27':'d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7'}[a.lane]
rows=json.loads((a.input/'delivery.json').read_text());chosen=[r for r in rows if r['apk'].endswith('-release.apk')];assert len(chosen)==1,'EXACTLY_ONE_RELEASE_APK_REQUIRED';row=dict(chosen[0]);apk=a.input/row['apk'];raw=apk.read_bytes()
assert row['sourceCommit']==source and row['certificateSha256']==expected and row['versionCode']>=577310101
assert hashlib.sha256(raw).hexdigest()==row['sha256'] and len(raw)==row['bytes']
tools=Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0'
cert=subprocess.check_output([str(tools/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True);assert re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})',cert).group(1)==expected
badging=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(apk)],text=True).splitlines()[0];assert "name='com.starcore.research.r13'" in badging and f"versionCode='{row['versionCode']}'" in badging
assert not subprocess.check_output(['git','status','--porcelain','--untracked-files=no'],text=True).strip(),'DIRTY_DELIVERY_SOURCE'
a.output.mkdir(parents=True,exist_ok=True);name=f"STARCORE-R30-FINAL-{a.lane}-{source[:12]}.apk";shutil.copy2(apk,a.output/name)
row.update(apk=name,lane=a.lane,artifactRole='FINAL_CANDIDATE_PENDING_ANDROID_GATES',installation='PENDING_ANDROID_GATES',startup='PENDING_ANDROID_GATES',physicalDevice='NOT_RUN',naturalQuality='INSUFFICIENT',overall='SOURCE_IMPLEMENTED_ACCEPTANCE_EVIDENCE_SEPARATE')
(a.output/'delivery.json').write_text(json.dumps([row],ensure_ascii=False,indent=2)+'\n');(a.output/'SHA256SUMS.txt').write_text(row['sha256']+'  '+name+'\n');(a.output/'public-certificate.txt').write_text(cert)
print(json.dumps({k:row[k] for k in ('apk','lane','sourceCommit','versionCode','certificateSha256','sha256','bytes')},ensure_ascii=False))
