#!/usr/bin/env python3
"""Package the cumulative original plan, new source audit and code repair contract."""
from pathlib import Path
from html.parser import HTMLParser
import csv,hashlib,json,shutil,zipfile

ROOT=Path(__file__).resolve().parents[2]
NAME='STARCORE_R31_UNIFIED_FINAL_20261112'
OUT=ROOT/'construction/repairs'/NAME
DEST=ROOT.parent/'deliverables'
DEST.mkdir(exist_ok=True)
def sha(data):return hashlib.sha256(data).hexdigest()
def read(p):return json.loads(p.read_text())
status=read(OUT/'CURRENT_SOURCE_STATUS_58.json')
assert len(status['rows'])==58 and status['currently_accepted_complete_cards']==0
proof=read(OUT/'PLAN_PRESERVATION_PROOF.json')
assert proof['original_cards']==56 and proof['current_items']==222 and proof['original_acceptance_texts']==194
for p in OUT.glob('*.json'):read(p)
class Parser(HTMLParser):pass
Parser().feed((OUT/(NAME+'.html')).read_text())
original=ROOT/'construction/STARCORE_R31_FINAL_20261104'
assert sha((original/'ledgers/acceptance_config.json').read_bytes())==proof['acceptance_config_sha256']
assert (OUT/'ORIGINAL_acceptance_config.json').read_bytes()==(original/'ledgers/acceptance_config.json').read_bytes()
assert sum(1 for _ in csv.DictReader((OUT/'ORIGINAL_TO_CURRENT_ACCEPTANCE.csv').open(encoding='utf-8-sig')))==222
members=[]
for p in sorted(OUT.rglob('*')):
    if p.is_file():members.append((p,Path(NAME)/p.relative_to(OUT)))
for p in sorted(original.rglob('*')):
    if p.is_file():members.append((p,Path(NAME)/'original_R31_4'/p.relative_to(original)))
archives=ROOT/'construction/archives'
for revision in ['STARCORE_R31_1_EXECUTION_20261105','STARCORE_R31_2_SEQUENTIAL_20261105','STARCORE_R31_3_BATCHES_20261105']:
    top=archives/revision
    for p in sorted(top.rglob('*')):
        if p.is_file() and (p.relative_to(top).parts[0] in ['docs','ledgers','batches'] or p.name=='README.md'):
            members.append((p,Path(NAME)/'revision_history'/revision/p.relative_to(top)))
for name in ['build_unified_final_plan.py','build_unified_source_status.py','package_unified_final_plan.py']:
    members.append((ROOT/'tools/r31'/name,Path(NAME)/'reproducible_packaging'/name))
for name in ['LOCK_REGRESSION.json','lock-regression.log']:
    members.append((ROOT/'verification/r31q/021'/name,Path(NAME)/'evidence_host_only'/name))
manifest={'format':'R31_UNIFIED_PLAN_PACKAGE_V1','artifact_kind':'PLAN_SOURCE_REVIEW_AND_IMPLEMENTATION_CONTRACT_NOT_APK',
    'source_checkpoint':status['source_checkpoint'],'original_input_files':sum(1 for _,p in members if 'original_R31_4' in p.parts),
    'preservation':proof,'members':[{'path':str(a),'bytes':p.stat().st_size,'sha256':sha(p.read_bytes())} for p,a in members]}
manifest_bytes=(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode()
archive=DEST/(NAME+'.zip')
with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for p,a in members:z.write(p,str(a))
    z.writestr(NAME+'/PACKAGE_MANIFEST.json',manifest_bytes)
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    assert len(z.namelist())==len(members)+1
    assert len(set(z.namelist()))==len(z.namelist())
    for p,a in members:
        assert sha(z.read(str(a)))==sha(p.read_bytes()),str(a)
shutil.copyfile(OUT/(NAME+'.html'),DEST/(NAME+'.html'))
shutil.copyfile(OUT/'CODEX_CONSTRUCTION_PROMPT.txt',DEST/'CODEX_STARCORE_R31_UNIFIED_20261112.txt')
shutil.copyfile(OUT/'CURRENT_SOURCE_STATUS_58.md',DEST/'STARCORE_R31_CURRENT_SOURCE_STATUS_58.md')
shutil.copyfile(OUT/'FINAL_CONSTRUCTION_PLAN.md',DEST/'STARCORE_R31_FINAL_CONSTRUCTION_PLAN.md')
qa={'scope':'DELIVERABLE_PLAN_ONLY_NO_NEW_PRODUCT_PASS','zip_crc':'PASS','all_member_hashes':'PASS',
    'duplicate_members':0,'original_input_files':manifest['original_input_files'],
    'cards':58,'original_acceptance_texts':194,'current_acceptance_items':222,
    'threshold_sha256':proof['acceptance_config_sha256'],
    'source_checkpoint':status['source_checkpoint'],
    'source_cards_with_findings':status['cards_with_specific_source_findings'],
    'source_cards_additional_review':status['cards_requiring_additional_source_review'],
    'files':[{'path':str(p),'bytes':p.stat().st_size,'sha256':sha(p.read_bytes())} for p in sorted(DEST.iterdir()) if p.is_file()]}
(DEST/'PLAN_PACKAGE_QA.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(qa,ensure_ascii=False))
