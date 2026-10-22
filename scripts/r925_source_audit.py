#!/usr/bin/env python3
"""Reproducible static inventory, exact line/character deltas and XML control parity.
Static flags are review aids; this script does not claim full semantic or device execution proof.
"""
from pathlib import Path
import argparse,csv,hashlib,json,re,difflib,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];J=ROOT/'app/src/main/java';R=ROOT/'app/src/main/res';O=ROOT/'reports/r925';O.mkdir(parents=True,exist_ok=True)
H=lambda b:hashlib.sha256(b).hexdigest()
def main(base):
 entries=[];changes=[];patch=[]
 files=sorted(p for p in ROOT.rglob('*') if p.is_file() and not any(x in p.relative_to(ROOT).parts for x in ['build','__pycache__','reports']) and p.suffix!='.pyc' and p.name!='r925_software_audit.txt')
 for p in files:
  rel=p.relative_to(ROOT);data=p.read_bytes();old=base/rel;before=old.read_bytes() if old.is_file() else b''
  try:after_text=data.decode('utf8');before_text=before.decode('utf8')
  except UnicodeDecodeError:after_text=before_text=''
  flags=[]
  if p.suffix=='.java':
   if re.search(r'catch\s*\([^)]*\)\s*\{\s*\}',after_text):flags.append('EMPTY_CATCH_REVIEW')
   if len(after_text.splitlines())>3000:flags.append('LARGE_FILE_REVIEW')
   if len(after_text.splitlines())<15:flags.append('SMALL_FILE_NOT_AUTOMATIC_DEFECT')
  entries.append([str(rel),len(data),len(after_text.splitlines()),len(after_text),H(data),'|'.join(flags) or 'NO_LEXICAL_FLAG'])
  if data!=before:
   added=deleted=addchars=delchars=0
   for tag,a,b,c,d in difflib.SequenceMatcher(a=before_text.splitlines(True),b=after_text.splitlines(True),autojunk=False).get_opcodes():
    if tag!='equal':deleted+=b-a;added+=d-c;delchars+=sum(map(len,before_text.splitlines(True)[a:b]));addchars+=sum(map(len,after_text.splitlines(True)[c:d]))
   changes.append([str(rel),'MODIFIED' if old.exists() else 'NEW',len(before),len(data),len(before_text.splitlines()),len(after_text.splitlines()),len(before_text),len(after_text),added,deleted,addchars,delchars,H(before) if old.exists() else '',H(data)])
   if after_text:patch.extend(difflib.unified_diff(before_text.splitlines(True),after_text.splitlines(True),fromfile='before/'+str(rel),tofile='after/'+str(rel)))
 for name,headers,rows in [('all_files.csv',['file','bytes','lines','characters','sha256','lexical_review_flags'],entries),('changed_files.csv',['file','status','before_bytes','after_bytes','before_lines','after_lines','before_characters','after_characters','added_lines','deleted_lines','added_line_characters','deleted_line_characters','before_sha256','after_sha256'],changes)]:
  with (O/name).open('w',encoding='utf-8-sig',newline='') as f:w=csv.writer(f);w.writerow(headers);w.writerows(rows)
 (O/'R924_TO_R925.patch').write_text(''.join(patch))
 NS='{http://schemas.android.com/apk/res/android}'
 layouts={};buttons=[]
 for p in R.rglob('*.xml'):
  root=ET.parse(p).getroot()
  if p.parent.name.startswith('layout'):
   ids=sorted({e.get(NS+'id') for e in root.iter() if e.get(NS+'id')});layouts[str(p.relative_to(R))]=ids
   for e in root.iter():
    if e.tag.endswith('Button'):buttons.append({'layout':str(p.relative_to(R)),'id':e.get(NS+'id'),'text':e.get(NS+'text')})
 phone=layouts['layout/activity_main.xml'];tablet=layouts['layout-sw600dp/activity_main.xml'];assert phone==tablet,'LAYOUT_ID_PARITY'
 manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
 components=[dict(type=e.tag,**{k.replace(NS,''):v for k,v in e.attrib.items()}) for e in manifest.iter() if e.tag in ['activity','service','receiver','provider','uses-permission']]
 resources=[{'path':str(p.relative_to(R)),'bytes':p.stat().st_size,'sha256':H(p.read_bytes())} for p in sorted(R.rglob('*')) if p.is_file()]
 java=[r for r in entries if r[0].startswith('app/src/main/java') and r[0].endswith('.java')]
 dynamic={'实时流水（500期）':'requestR919TruthDetail(6)','抓取记录（100条）':'requestR919TruthDetail(7)','影子统计':'requestR919TruthDetail(8)','研究证书漏斗':'requestR919TruthDetail(9)','淘汰原因明细':'requestR919TruthDetail(10)','导出500期流水 CSV':'requestR919TruthDetail(11) -> shareCsv','导出完整1200动作训练数据':'TrainingSnapshotExportR925.start','四大能力卡':'requestR919TruthDetail(1..4)','一键导出完整检测报告 TXT':'b15CopyDiagnosticReportR922 -> shareFile'}
 audit={'APP_IDENTITY_AND_BUILD':(ROOT/'release/release_identity.properties').read_text(),'ANDROID_MANIFEST_AUDIT':components,'RESOURCE_INVENTORY':resources,'LAYOUT_AND_UI_CONTROL_AUDIT':layouts,'BUTTON_WIRING_AUDIT':{'xml_buttons':buttons,'dynamic_routes':dynamic,'runtime_click_proof':'NOT_RUN_ON_DEVICE'},'UI_RUNTIME_VISIBILITY_AUDIT':{'source_views':['background','shadow','pipeline','deep_learning','evolution','creation','atoms'],'device_proof':'NOT_RUN'},'SOURCE_FILE_AUDIT':{'count':len(java),'files':java,'review_type':'LEXICAL_AND_TARGETED_SEMANTIC_REVIEW_NOT_545_FILE_EXECUTION_PROOF'},'ARTIFACT_ASSET_MANIFEST':[r for r in entries if r[0].startswith(('training/','app/src/main/assets/','app/src/main/java/com/ethk3/novaonepro/V51'))],'DATABASE_SCHEMA_MIGRATION_AUDIT':'DB36 additive tables; schema fingerprint validated before startup shortcut; mismatch reruns migration','PACKAGE_INTEGRITY':'EXTERNAL_RELEASE_EVIDENCE_REQUIRED_APK_SIGNATURE_CANNOT_SELF_PROVE','PHYSICAL_DEVICE':'NOT_RUN','CURRENT_MODEL':'NOT_PROVEN','LAYOUT_PARITY':'PASS'}
 write=json.dumps(audit,ensure_ascii=False,indent=2)
 assets=ROOT/'app/src/main/assets';assets.mkdir(exist_ok=True);(assets/'r925_software_audit.txt').write_text(write)
 (O/'static_audit.json').write_text(write)
 summary={'java_files':len(java),'resources':len(resources),'changed_files':len(changes),'added_lines':sum(r[8] for r in changes),'deleted_lines':sum(r[9] for r in changes),'phone_tablet_ids':len(phone),'layout_parity':'PASS','physical_device':'NOT_RUN'}
 (O/'change_summary.json').write_text(json.dumps(summary,indent=2));print(json.dumps(summary))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--baseline',required=True,type=Path);main(p.parse_args().baseline)
 import subprocess,sys
 subprocess.check_call([sys.executable,str(ROOT/'scripts/r926_source_roles.py')])
