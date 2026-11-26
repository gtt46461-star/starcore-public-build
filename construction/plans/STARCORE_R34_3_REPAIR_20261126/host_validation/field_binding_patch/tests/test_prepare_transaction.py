#!/usr/bin/env python3
"""Fault-inject the real original four-file advance API through field173 wrapper.
No production checkout is changed; all repositories/journals are disposable.
"""
from pathlib import Path
import contextlib,io,hashlib,importlib.util,json,shutil,subprocess,sys,tempfile
ROOT=Path(__file__).resolve().parents[3]
SOURCE=ROOT/'source-installed'
SCRIPT=ROOT/'audit/field_binding_patch/after/tools/r343/prepare_field_batch.py'
sys.path[:0]=[str(SCRIPT.parent),str(SOURCE/'tools/r33')]
spec=importlib.util.spec_from_file_location('field173_prepare_under_test',SCRIPT)
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
advance=module.advance
PIN={'schema':module.field.SCHEMA,'planCommit':'c'*40,'planSha256':'d'*64,'sourceBaseline':module.field.BASELINE,'reportSha256':module.field.REPORT_SHA256}
assertions=0

def check(value,why):
 global assertions
 assertions+=1
 assert value,why

def repo_at(parent,name):
 repo=parent/name;repo.mkdir()
 for rel in (*advance.FILES,'scripts/r926_release_manifest.py'):
  target=repo/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(SOURCE/rel,target)
 subprocess.run(['git','init','-q',str(repo)],check=True)
 return repo

def snapshot(repo): return {x:(repo/x).read_bytes() for x in advance.FILES}
def invoke(repo,journal,batch='B01',installed=577311173,highest=577311173,resume=True):
 return module.apply_field_batch(repo,journal,PIN,batch,installed,highest,'2026-11-26',resume)
def original_prepare(repo,journal,batch='B01',installed=577311173,highest=577311173,series='R34.2'):
 with advance.repo_version_lock(repo):return advance.prepare(repo,journal,installed,batch,'2026-11-26',series,highest)
def reject(fn,reason):
 try: fn()
 except (ValueError,FileNotFoundError) as e:check(reason in str(e),str(e))
 else:raise AssertionError('accepted '+reason)

with tempfile.TemporaryDirectory(prefix='r343-field-transaction-') as name:
 base=Path(name)
 # Exact previously unresumable cut: JOURNAL complete, wrapper plan record absent.
 repo=repo_at(base,'gap-repo');journal=base/'gap-journal';prepared=original_prepare(repo,journal)
 check(not (journal/'FIELD173_PLAN.json').exists(),'gap fixture')
 result=invoke(repo,journal);check(result['versionCode']==577311174,'gap recovered to monotonic version')
 check((journal/'FIELD173_PLAN.json').is_file(),'plan metadata recovered')
 check(module.read(repo/module.field.BINDING)==PIN,'binding exact')
 check(module.read(repo/advance.FILES[0])['versionName']=='R34.2-B01-2026-11-26-577311174','series preserved')
 with contextlib.redirect_stdout(io.StringIO()): advance.generator(repo).check()
 # Same plan pin from B01 must allow B02, including crash before its field record.
 journal2=base/'gap-b02';original_prepare(repo,journal2,'B02',577311174,577311174)
 result=invoke(repo,journal2,'B02',577311174,577311174)
 check(result['versionCode']==577311175,'same plan next batch')
 # Actual original atomic writer interrupted after two of the four version files.
 repo=repo_at(base,'partial-repo');journal=base/'partial-journal'
 actual_atomic=advance.atomic;writes=[]
 def interrupted(path,data):
  rel=str(Path(path).relative_to(repo)) if Path(path).is_relative_to(repo) else ''
  if rel in advance.FILES:
   if len(writes)==2:raise RuntimeError('TEST_PROCESS_CUT_AFTER_TWO_IDENTITIES')
   writes.append(rel)
  return actual_atomic(path,data)
 advance.atomic=interrupted
 try:
  invoke(repo,journal,resume=False)
 except RuntimeError as error:check(str(error)=='TEST_PROCESS_CUT_AFTER_TWO_IDENTITIES','injected real cut')
 finally:advance.atomic=actual_atomic
 check(len(writes)==2,'two identities changed before cut')
 result=invoke(repo,journal);check(result['versionCode']==577311174,'partial resumes')
 check((journal/'APPLIED.json').is_file(),'receipt after all four identities')
 with contextlib.redirect_stdout(io.StringIO()): advance.generator(repo).check()
 # Divergent user bytes are never overwritten and no new pin is written.
 repo=repo_at(base,'edit-repo');journal=base/'edit-journal';original_prepare(repo,journal)
 changed=repo/advance.FILES[2];changed.write_bytes(changed.read_bytes()+b'// USER EDIT\n');before=snapshot(repo)
 reject(lambda:invoke(repo,journal),'IDENTITY_DIVERGED_RETAIN_USER_EDIT')
 check(before==snapshot(repo),'divergent identity bytes preserved')
 check(not (repo/module.field.BINDING).exists(),'preflight before pin write')
 # A misleading JOURNAL header cannot advertise a different version from its bytes.
 repo=repo_at(base,'header-repo');journal=base/'header-journal';original_prepare(repo,journal)
 receipt=module.read(journal/'JOURNAL.json');receipt['versionCode']+=1;receipt['versionName']=receipt['versionName'].rsplit('-',1)[0]+'-'+str(receipt['versionCode'])
 (journal/'JOURNAL.json').write_text(json.dumps(receipt));before=snapshot(repo)
 reject(lambda:invoke(repo,journal),'FIELD173_JOURNAL_MANIFEST_IDENTITY')
 check(before==snapshot(repo),'false receipt identity writes nothing')
 # Correct four-file hashes but wrong historical series cannot attach field173 pin.
 repo=repo_at(base,'series-repo');journal=base/'series-journal';original_prepare(repo,journal,series='R33.5')
 before=snapshot(repo);reject(lambda:invoke(repo,journal),'FIELD173_JOURNAL_RELEASE_SERIES_MISMATCH')
 check(before==snapshot(repo),'wrong series writes nothing')
 # Journal-local after properties changed, including matching supplied SHA, still
 # must match original generator output from its manifest before any target write.
 repo=repo_at(base,'generator-repo');journal=base/'generator-journal';original_prepare(repo,journal)
 rel=advance.FILES[1];f=journal/'after'/rel;f.write_bytes(f.read_bytes()+b'fake=value\n')
 receipt=module.read(journal/'JOURNAL.json');receipt['files'][1]['afterSha256']=hashlib.sha256(f.read_bytes()).hexdigest();(journal/'JOURNAL.json').write_text(json.dumps(receipt))
 before=snapshot(repo);reject(lambda:invoke(repo,journal),'FIELD173_JOURNAL_GENERATED_IDENTITIES_MISMATCH')
 check(before==snapshot(repo),'generator conflict writes nothing')
 # Lock-protected pin conflict is checked even for a fresh batch.
 repo=repo_at(base,'pin-repo');journal=base/'pin-journal';location=repo/module.field.BINDING;location.write_text('{}');before=snapshot(repo)
 reject(lambda:invoke(repo,journal,resume=False),'FIELD173_EXISTING_PIN_CONFLICT')
 check(before==snapshot(repo) and not journal.exists(),'pin conflict before prepare')
 # Missing plan provenance after any controlled file changed remains fail-closed.
 repo=repo_at(base,'untrusted-partial');journal=base/'untrusted-journal';original_prepare(repo,journal)
 first=advance.FILES[0];(repo/first).write_bytes((journal/'after'/first).read_bytes());before=snapshot(repo)
 reject(lambda:invoke(repo,journal),'FIELD173_MISSING_PLAN_REQUIRES_UNAPPLIED_JOURNAL')
 check(before==snapshot(repo),'untrusted missing plan no overwrite')

print(json.dumps({'status':'PASS','assertions':assertions,'scope':'actual original advance_release_version four-file generator/lock/journal/atomic/apply API in disposable Git checkouts; field wrapper preflight and recovery; no APK or real plan authorization validation','source_sha256':hashlib.sha256(SCRIPT.read_bytes()).hexdigest(),'device_or_signing':'NOT_RUN'},ensure_ascii=False,indent=2))
