#!/usr/bin/env python3
"""Read R34 batch receipts. Absent evidence remains OPEN; never grants REAL.
APK checks execute the actual SDK verifier supplied by the constructor.
This validates delivery evidence, not profitability or device endurance by assertion.
"""
import argparse, hashlib, json, pathlib, subprocess, zipfile, re

BASE='273641e838e0b8dd842b42a67a674e184942a30a'
APPLICATION_ID='com.starcore.research.r13'  # 095 release/release_identity.properties, not Java package
REQUIRED=['source_commit','plan_sha256','issue_receipts','apk','host_log','android_build_log','device_evidence','natural_evidence']

def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1<<20),b''):h.update(b)
 return h.hexdigest()

def artifact(root,item):
 if not isinstance(item,dict):raise ValueError('MISSING_ARTIFACT')
 p=(root/item['path']).resolve()
 if not p.is_relative_to(root) or p.is_symlink() or not p.is_file():raise ValueError('UNSAFE_OR_MISSING_ARTIFACT')
 if p.stat().st_size!=item['bytes'] or sha(p)!=item['sha256']:raise ValueError('ARTIFACT_BYTES_MISMATCH')
 return p

def run(args):
 p=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,timeout=90)
 if p.returncode:raise ValueError('VERIFIER_FAILED:'+pathlib.Path(args[0]).name)
 return p.stdout+p.stderr

def inspect(root, receipts, expected, apksigner=None,aapt2=None,expected_plan_sha256=None):
 results=[]
 for batch in expected['batches']:
  bid=batch['id']; row=receipts.get(bid); errors=[]
  result={'batch':bid,'delivery':'OPEN','device':'NOT_VERIFIED','natural':'NOT_PROVEN','errors':errors}
  results.append(result)
  if row is None:errors.append('BATCH_RECEIPT_MISSING');continue
  try:
   for field in REQUIRED:
    if field not in row:raise ValueError('REQUIRED_FIELD:'+field)
   if not re.fullmatch('[0-9a-f]{40}',row['source_commit']):raise ValueError('SOURCE_COMMIT_INVALID')
   if not expected_plan_sha256 or not re.fullmatch('[0-9a-f]{64}',expected_plan_sha256):raise ValueError('TRUSTED_PLAN_SEAL_REQUIRED')
   if not re.fullmatch('[0-9a-f]{64}',str(receipts.get('_plan_sha256',''))):raise ValueError('PLAN_SEAL_INVALID')
   if receipts['_plan_sha256']!=expected_plan_sha256:raise ValueError('UNTRUSTED_PLAN_SEAL')
   if row['plan_sha256']!=receipts.get('_plan_sha256'):raise ValueError('PLAN_HASH_MISMATCH')
   expected_ids=set(batch['issues']); issues=row['issue_receipts']
   if set(issues)!=expected_ids:raise ValueError('ISSUE_COVERAGE_MISMATCH')
   for key,item in issues.items():artifact(root,item)
   for field in ['host_log','android_build_log']:artifact(root,row[field])
   apk=artifact(root,row['apk'])
   with zipfile.ZipFile(apk) as z:
    if not {'AndroidManifest.xml','classes.dex'}.issubset(z.namelist()):raise ValueError('NOT_ANDROID_APK')
    if z.testzip():raise ValueError('APK_CRC')
   if not apksigner or not aapt2:raise ValueError('SDK_APK_VERIFIERS_REQUIRED')
   sign=run([str(apksigner),'verify','--verbose','--print-certs',str(apk)])
   fp=row['apk'].get('certificate_sha256','').lower().replace(':','')
   if len(fp)!=64 or fp not in sign.lower().replace(':',''):raise ValueError('SIGNATURE_IDENTITY')
   badging=run([str(aapt2),'dump','badging',str(apk)])
   match=re.search(r"package: name='([^']+)' versionCode='(\d+)'",badging)
   if not match or match.group(1)!=APPLICATION_ID or int(match.group(2))!=row['apk'].get('version_code'):raise ValueError('APK_PACKAGE_VERSION')
   if int(match.group(2))<=577311095:raise ValueError('APK_NOT_NEWER_THAN_095')
   # Device and natural observations are retained as references. Raw traces need
   # owner joins and protocol-specific verification; a boolean cannot upgrade them.
   for field in ['device_evidence','natural_evidence']:
    if row[field]:artifact(root,row[field])
   result['delivery']='VERIFIED_BYTES_AND_SDK_SIGNATURE'
   result['device']='EVIDENCE_PRESENT_REQUIRES_PROTOCOL_REVIEW' if row['device_evidence'] else 'NOT_RUN'
   result['natural']='EVIDENCE_PRESENT_REQUIRES_REGISTERED_ANALYSIS' if row['natural_evidence'] else 'NOT_PROVEN'
  except (ValueError,KeyError,TypeError,OSError,zipfile.BadZipFile,subprocess.TimeoutExpired) as e:errors.append(str(e))
 return {'schema':'R34_DELIVERY_CHECK_1','batches':results,'all_delivery_verified':bool(results) and all(x['delivery']=='VERIFIED_BYTES_AND_SDK_SIGNATURE' and not x['errors'] for x in results),'application_runtime_accepted':False,'natural_advantage_proven':False,'may_authorize_real':False,'boundary':'Artifact and SDK identity checks do not evaluate raw device period receipts or sequential quality tests. The supplied plan seal must independently match verify_plan.py and the reviewed Git commit. Never turn supplied booleans into runtime/quality approval.'}

def main():
 p=argparse.ArgumentParser();p.add_argument('--receipts',type=pathlib.Path,required=True);p.add_argument('--scope',type=pathlib.Path,required=True);p.add_argument('--plan-manifest',type=pathlib.Path,required=True,help='Locally verified plan seal from the reviewed Git commit, not supplied receipt JSON');p.add_argument('--root',type=pathlib.Path,required=True);p.add_argument('--apksigner',type=pathlib.Path);p.add_argument('--aapt2',type=pathlib.Path);p.add_argument('--out',type=pathlib.Path)
 a=p.parse_args();out=inspect(a.root.resolve(),json.loads(a.receipts.read_text()),json.loads(a.scope.read_text()),a.apksigner,a.aapt2,sha(a.plan_manifest));s=json.dumps(out,ensure_ascii=False,indent=2)+'\n'
 if a.out:a.out.parent.mkdir(parents=True,exist_ok=True);a.out.write_text(s)
 print(s);raise SystemExit(0 if out['all_delivery_verified'] else 2)
if __name__=='__main__':main()
