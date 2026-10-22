#!/usr/bin/env python3
"""Generate report attachments from the complete construction ledgers; never infer acceptance."""
from pathlib import Path
import csv,gzip,hashlib,json,shutil,os,subprocess,re
ROOT=Path(__file__).resolve().parents[1]
source=ROOT/'construction/r997';out=ROOT/'app/build/generated/assets/r997/diagnostics'
out.mkdir(parents=True,exist_ok=True)
ledger=source/'REQUIREMENTS_LEDGER.csv'
if ledger.is_file():raw=ledger.read_bytes()
else:raw=gzip.decompress((source/'REQUIREMENTS_LEDGER.csv.gz').read_bytes())
import io
rows=list(csv.DictReader(io.StringIO(raw.decode('utf-8-sig'))))
allowed={'未检查','未实现','实现错误','已修改待验证','验证失败','已验证','外部验收待完成'}
if len(rows)!=1832 or len({r['requirement_id'] for r in rows})!=1832:
    raise SystemExit('R997_COMPLETE_PLAN_DENOMINATOR_INVALID')
for row in rows:
    if row['current_status'] not in allowed:raise SystemExit('R997_REQUIREMENT_STATUS_INVALID')
    if hashlib.sha256(row['exact_requirement'].encode()).hexdigest()!=row['exact_requirement_sha256']:
        raise SystemExit('R997_ORIGINAL_REQUIREMENT_CHANGED:'+row['requirement_id'])
(out/'construction-current-REQUIREMENTS_1832.csv').write_bytes(raw)
shutil.copyfile(source/'SUBITEMS.csv',out/'construction-current-SUBITEMS.csv')
card_raw=gzip.decompress((source/'CARD_LEDGER.csv.gz').read_bytes())
cards=list(csv.DictReader(io.StringIO(card_raw.decode('utf-8-sig'))))
if len(cards)!=299 or len({c['card_id'] for c in cards})!=299:
    raise SystemExit('R997_COMPLETE_CARD_DENOMINATOR_INVALID')
for card in cards:
    if card['current_status'] not in allowed:raise SystemExit('R997_CARD_STATUS_INVALID')
    if hashlib.sha256(card['original_card'].encode()).hexdigest()!=card['original_card_sha256']:
        raise SystemExit('R997_ORIGINAL_CARD_CHANGED:'+card['card_id'])
(out/'construction-current-CARDS_299.csv').write_bytes(card_raw)
# The source commit is recorded in generated assets, avoiding a commit/self-hash cycle.
# A recovered ZIP checkout uses a remotely verified receipt; an ordinary clean Git
# checkout uses its own HEAD. No user credential is read or written here.
paths=['app/src/main','app/build.gradle','build.gradle','settings.gradle','gradle.properties',
       'gradle','scripts','release','construction/r997/REQUIREMENTS_LEDGER.csv.gz',
       'construction/r997/CARD_LEDGER.csv.gz','construction/r997/SUBITEMS.csv']
listed=subprocess.check_output(['git','ls-files','-z','--',*paths],cwd=ROOT).decode().split('\0')
files={name:hashlib.sha256((ROOT/name).read_bytes()).hexdigest() for name in sorted(listed) if name and (ROOT/name).is_file()}
receipt=os.environ.get('STARCORE_SOURCE_RECEIPT','')
if receipt:
    remote=json.loads(Path(receipt).read_text())
    if remote.get('files')!=files:raise SystemExit('R997_REMOTE_BUILD_INPUT_DRIFT')
    commit,tree=remote['commit'],remote['tree']
else:
    if subprocess.check_output(['git','status','--porcelain','--',*paths],cwd=ROOT).strip():
        raise SystemExit('R997_UNCOMMITTED_BUILD_INPUTS_RELEASE_BLOCKED')
    commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    tree=subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=ROOT,text=True).strip()
if not re.fullmatch('[0-9a-f]{40}',commit) or not re.fullmatch('[0-9a-f]{40}',tree):
    raise SystemExit('R997_SOURCE_GIT_IDENTITY_INVALID')
identity={'format':'R997_CONSTRUCTION_LEDGER_NOT_FULL_ACCEPTANCE','requirements':1832,
          'states':{s:sum(r['current_status']==s for r in rows)for s in sorted(allowed)},
          'requirementLedgerSha256':hashlib.sha256(raw).hexdigest(),
          'subitemsSha256':hashlib.sha256((source/'SUBITEMS.csv').read_bytes()).hexdigest(),
          'cards':299,'cardStates':{s:sum(c['current_status']==s for c in cards)for s in sorted(allowed)},
          'cardLedgerSha256':hashlib.sha256(card_raw).hexdigest(),
          'sourceCommit':commit,'sourceTree':tree,
          'buildInputsSha256':hashlib.sha256(json.dumps(files,sort_keys=True,separators=(',',':')).encode()).hexdigest(),
          'acceptanceBoundary':'HOST_REPLAY_IS_NOT_DEVICE_OR_NATURAL_FUTURE; PARTIAL_PARENT_STATUS_IS_NOT_COMPLETION'}
(out/'construction-current-IDENTITY.json').write_text(json.dumps(identity,ensure_ascii=False,indent=2)+'\n')
print('R997_CONSTRUCTION_ASSETS_GENERATED requirements=1832 statuses_preserved=TRUE')
