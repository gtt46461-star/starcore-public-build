#!/usr/bin/env python3
"""Apply only the supplied R34 partial patches to an isolated 095 subset.
Does not implement the method-only work orders or compile Android callers.
"""
import argparse, hashlib, json, pathlib, re, shutil, subprocess, tempfile

def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
 p=argparse.ArgumentParser();p.add_argument('--plan',type=pathlib.Path,required=True);p.add_argument('--source',type=pathlib.Path,required=True);a=p.parse_args()
 plan=a.plan.resolve();source=a.source.resolve()
 head=subprocess.check_output(['git','-C',str(source),'rev-parse','HEAD'],text=True).strip()
 assert head=='273641e838e0b8dd842b42a67a674e184942a30a','WRONG_BASE'
 patches=[plan/'payload'/s for s in ['runtime/patches/B01_runtime.patch','runtime/supplement/B01_schema_core_ADD.patch','runtime/patches/B02_runtime.patch','runtime/patches/B06_runtime.patch','learning/patches/B06_cold_rebirth_integration.patch']]
 cards=json.loads((plan/'FILE_CARDS.json').read_text())['files']
 supplied=[f for f in cards if f['code_material']['material_type'] in ['FULL_NEW_SOURCE','APPLICABLE_PATCH_WITH_FULL_POSTIMAGE']]
 with tempfile.TemporaryDirectory(prefix='r34-patch-check-') as tmp:
  root=pathlib.Path(tmp)
  subprocess.run(['git','-C',str(root),'init','-q'],check=True,capture_output=True)
  for f in supplied:
   if f['before_state']=='EXISTING_095':
    src=source/f['production_path'];assert digest(src)==f['before_sha256']
    dest=root/f['production_path'];dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dest)
  receipts=[]
  for patch in patches:
   subprocess.run(['git','-C',str(root),'apply','--check',str(patch)],check=True,capture_output=True)
   subprocess.run(['git','-C',str(root),'apply',str(patch)],check=True,capture_output=True)
   receipts.append({'patch':str(patch.relative_to(plan)),'sha256':digest(patch),'status':'APPLIED_IN_CUMULATIVE_ISOLATED_095_SUBSET'})
  # The B06 integration patch contains deltas to existing owners. Its three new
  # classes are explicitly supplied as full ADD files, not silently omitted.
  for src in (plan/'payload/learning/app').rglob('*.java'):
   dest=root/'app'/src.relative_to(plan/'payload/learning/app')
   assert not dest.exists(),'NEW_FILE_ALREADY_EXISTS'
   dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dest)
  checked=[]
  for f in supplied:
   name=f['production_path'];mat=f['code_material'];dest=root/name
   assert dest.is_file() and digest(dest)==mat['sha256'],('POSTIMAGE_MISMATCH',name)
   checked.append(name)
  print(json.dumps({'schema':'R34_SUPPLIED_PATCH_SET_CHECK_1','status':'PASS','baseline':head,
   'patches':receipts,'postimages_checked':len(checked),'paths':checked,
   'source_checkout_mutated':False,'all_66_issues_implemented':False,'android_build':'NOT_RUN','apk':'NOT_BUILT','device':'NOT_RUN','natural_quality':'NOT_PROVEN'},ensure_ascii=False,indent=2))

if __name__=='__main__':main()
