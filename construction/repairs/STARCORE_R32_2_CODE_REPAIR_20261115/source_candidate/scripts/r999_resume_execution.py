#!/usr/bin/env python3
"""Resume exact-ID execution ledgers without converting historical receipts to PASS."""
import argparse, collections, csv, gzip, hashlib, json, shutil, sqlite3, subprocess, tarfile
from pathlib import Path
csv.field_size_limit(256 * 1024 * 1024)
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'construction/r999'
def sha(p):
    h = hashlib.sha256()
    with p.open('rb') as f:
        for b in iter(lambda: f.read(1048576), b''): h.update(b)
    return h.hexdigest()
def rows(p):
    with p.open(encoding='utf-8-sig', newline='') as f: return list(csv.DictReader(f))
def save(p, value): p.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')
def write_rows(p, data):
    fields = list(dict.fromkeys(k for row in data for k in row))
    with p.open('w', encoding='utf-8', newline='') as f:
        w = csv.DictWriter(f, fields); w.writeheader(); w.writerows(data)
def main():
    a = argparse.ArgumentParser(); a.add_argument('--plan', type=Path, required=True); a.add_argument('--report', type=Path, required=True)
    args = a.parse_args(); plan = args.plan.resolve(); report = args.report.resolve(); OUT.mkdir(parents=True, exist_ok=True)
    manifest_failures = []; entries = 0
    for line in (plan/'PACKAGE_MANIFEST.sha256').read_text().splitlines():
        expected, name = line.split('  ', 1); p = plan/name; actual = sha(p) if p.is_file() else 'MISSING'; entries += 1
        if expected != actual: manifest_failures.append({'path':name, 'expected':expected, 'actual':actual})
    inventory = []; errors = []
    for p in sorted(plan.rglob('*')):
        if not p.is_file(): continue
        rec = {'path':str(p.relative_to(plan)), 'bytes':p.stat().st_size, 'sha256':sha(p)}
        try:
            raw = gzip.decompress(p.read_bytes()) if p.suffix == '.gz' else p.read_bytes()
            ext = p.with_suffix('').suffix if p.suffix == '.gz' else p.suffix
            if p.suffix == '.gz': rec.update(decoded_bytes=len(raw), decoded_sha256=hashlib.sha256(raw).hexdigest())
            if ext == '.json': rec['json_type'] = type(json.loads(raw.decode('utf-8-sig'))).__name__
            elif ext == '.jsonl': rec['parsed_rows'] = sum(1 for l in raw.decode('utf-8-sig').splitlines() if l.strip() and json.loads(l) is not None)
            elif ext == '.csv': rec['parsed_rows'] = len(list(csv.DictReader(raw.decode('utf-8-sig').splitlines())))
            elif ext in {'.md','.txt','.java','.c','.h','.html','.sql','.tsv','.py','.xml','.gradle','.diff'}:
                rec['read_lines'] = len(raw.decode('utf-8-sig').splitlines())
        except (ValueError, UnicodeError, OSError, csv.Error) as e: errors.append({'path':rec['path'], 'error':str(e)})
        inventory.append(rec)
    with gzip.open(OUT/'PLAN_READ_INVENTORY.json.gz', 'wt', encoding='utf-8') as f:
        json.dump({'files':inventory, 'parse_errors':errors, 'scope':'Full byte read/machine parse. Semantic per-card review remains separate.'}, f, ensure_ascii=False)
    save(OUT/'PLAN_MANIFEST_VERIFICATION.json', {'entries':entries,'failures':manifest_failures,'parse_errors':errors,'sha256':sha(plan/'PACKAGE_MANIFEST.sha256')})
    if manifest_failures or errors: raise SystemExit('PLAN_INPUT_INVALID')
    names = ['REQUIREMENT_EXECUTION_LEDGER.csv','CARD_EXECUTION_LEDGER.csv','DETECTION_EXECUTION_LEDGER.csv','TEST_EXECUTION_LEDGER.csv']
    # The remote checkpoint keeps the complete large CSVs losslessly compressed.
    if not (ROOT/'construction/r998'/names[0]).exists():
        with tarfile.open(ROOT/'release/r998_execution_ledger.tar.gz') as archive:
            for member in archive.getmembers():
                if member.isfile() and Path(member.name).name in names:
                    source = archive.extractfile(member)
                    (ROOT/'construction/r998'/Path(member.name).name).write_bytes(source.read())
    for name in names:
        if not (OUT/name).exists(): shutil.copy2(ROOT/'construction/r998'/name, OUT/name)
    req = rows(OUT/names[0]); cards = rows(OUT/names[1]); tests = rows(OUT/names[3]); det = rows(OUT/names[2])
    assert len(req) == len({r['requirement_id'] for r in req}) == 1832
    assert len(cards) == len({r['card_id'] for r in cards}) == 299
    assert len(tests) == len({r['test_id'] for r in tests}) == 328
    assert sum(r.get('source_cohort') == 'ORIGINAL_R980' for r in det) == 166
    assert collections.Counter(r['semantic_disposition'] for r in req) == {'PLAN_ARTIFACT':530,'EXACT_SUPPORTED':860,'SUPERSEDED_EQUIVALENT':426,'HISTORICAL_REFERENCE':16}
    for r in req: assert hashlib.sha256(r['exact_requirement'].encode()).hexdigest() == r['exact_requirement_sha256']
    # Current host evidence binds only the recovery card. Other prior statuses keep their provenance.
    for card in cards:
        if card['card_id'] == 'FILE-CAN-113':
            card.update(current_status='HOST_PASS_DEVICE_PENDING', code_status='MODIFIED_HOST_VERIFIED',
                        current_before_sha256='8c5a6604f1d1402ef8991368699bc6d7c1ebd9dc582527d6d34d03b46979a7e9',
                        current_after_sha256=sha(ROOT/card['path']), current_contract_review='CURRENT_DEADLINE_GAP_REPAIRED',
                        current_test_id='NEW-TEST-001;R998_RECOVERY_MAILBOX_19', exit_code='0',
                        command='verification/r999/RecoveryDeadlineR999.java + RecoveryDispatchContractR998.java; exact javac/java commands in receipt',
                        log='construction/r999/evidence/SCC-022/fixed/CURRENT_EXECUTION.json',
                        current_evidence_scope='Production CAS mailbox/worker with host framework adapters. Not Android process or device.',
                        blocker='ANDROID_PROCESS_AND_DEVICE_PENDING',
                        acceptance_evidence='First failure retained; expired queued recovery IO suppressed; finish once; resumed worker; 19 overload/idempotency checks.',
                        diff_commit='LOCAL_MODIFICATION_PENDING_COMMIT')
    write_rows(OUT/names[1], cards)
    manifest = [json.loads(l) for l in (report/'ARCHIVE_MANIFEST.jsonl').read_text().splitlines()]
    report_failures = []
    for v in manifest:
        if v.get('kind') != 'member': continue
        p = report/v['name']
        if not p.is_file() or sha(p) != v['raw_sha256'] or p.stat().st_size != v['raw_bytes']: report_failures.append(v['name'])
    report_parse = []
    for p in sorted(report.iterdir()):
        if p.suffix in {'.txt','.tsv','.json','.jsonl'}:
            raw = p.read_text(encoding='utf-8-sig')
            n = None
            if p.suffix == '.json': json.loads(raw)
            elif p.suffix == '.jsonl': n = sum(1 for l in raw.splitlines() if l.strip() and json.loads(l) is not None)
            report_parse.append({'path':p.name,'bytes':p.stat().st_size,'sha256':sha(p),'lines':len(raw.splitlines()),'jsonl_rows':n})
    checks = []
    for l in (report/'REPORT_DETAILS.txt').read_text().splitlines():
        try: v = json.loads(l)
        except ValueError: continue
        if v.get('kind') == 'check_result': checks.append(v)
    lookup = {v['id']:v for v in checks}
    for row in det:
        if row.get('source_cohort') == 'PHONE_R996':
            rid = row.get('report_position','').split(':')[-1]
            row['report_evidence'] = json.dumps(lookup.get(rid,{}), ensure_ascii=False)
            row['original_source_file_sha256'] = sha(report/'REPORT_DETAILS.txt')
            row['source_attachment'] = '10.6号5.4.2.39检测报告.zip'
    if not any(r.get('issue_id') == 'NEW-DET-005' for r in det):
        det.append({'issue_id':'NEW-DET-005','source_cohort':'CURRENT_CODE_R998','record_kind':'REPRODUCED_CODE_DEFECT',
                    'report_position':'FILE-CAN-113 worker deadline','root_cause':'Worker checks done but does not recheck deadline when dequeued; a delayed pump permits expired recovery IO.',
                    'current_status':'HOST_FIXED_DEVICE_PENDING','current_test_id':'NEW-TEST-001',
                    'log':'construction/r999/evidence/SCC-022/first_failure/execution.log;construction/r999/evidence/SCC-022/fixed/deadline.log',
                    'current_evidence_scope':'Production worker on host; not device','acceptance':'Expired body not executed; completion once; worker resumes'})
    if not any(r.get('issue_id') == 'NEW-DET-006' for r in det):
        det.append({'issue_id':'NEW-DET-006','source_cohort':'CURRENT_DIRECTIVE_VS_PLAN','record_kind':'CONTRACT_CONFLICT',
                    'root_cause':'Current directive fixes Ethereum1-PK10; plan and latest release/data identity fix Binance1-PK10.',
                    'current_status':'BLOCKED_GAME_IDENTITY_MIGRATION','acceptance':'No Binance data relabeled as Ethereum; explicit compatible API/parser/migration/test evidence before release.'})
    write_rows(OUT/names[2], det)
    db = sqlite3.connect('file:'+str(report/'snapshot.db')+'?mode=ro', uri=True)
    integrity = db.execute('PRAGMA integrity_check').fetchone()[0]; fk = db.execute('PRAGMA foreign_key_check').fetchall()
    save(OUT/'REPORT_INPUT_VERIFICATION.json', {'member_count':sum(v.get('kind')=='member' for v in manifest),'hash_failures':report_failures,
         'parsed_files':report_parse,'checks':len(checks),'outcomes':dict(collections.Counter(v['domain_outcome'] for v in checks)),
         'sqlite_integrity':integrity,'foreign_key_failures':len(fk),'apk_sha256':sha(report/'installed-app.apk'),
         'source_manifest':json.loads((report/'source-manifest.json').read_text()),'privacy':'No raw DB, API or private runtime values copied'})
    previous = json.loads((ROOT/'construction/r998/EXECUTION_STATE.json').read_text())
    state = {**previous,'schema_version':2,'branch':'construction/r999-canonical-20261006',
             'baseline_commit':'71cd35da8fa0b76108c69f1b75049d14b79e9cd6','report_commit':'9ca5bc1ad0ce47cf85b6b435606c85629d4f88c2',
             'baseline_origin':'Remote R998 product source byte verified; prior work preserved',
             'current_group':'SCC-022','current_card':'FILE-CAN-113','next_action':'Current Android compile and recovery integration gates, then SCC-024',
             'new_detections':6,'new_host_tests_executed':2,'new_host_tests_passed':2,
             'final_closed_requirements':0,'final_closed_cards':0,'test_matrix_executed':0,
             'blockers':['DEVICE_NOT_VERIFIED','GAME_IDENTITY_CONFLICT_ETHEREUM_VS_BINANCE','NATURAL_FUTURE_NOT_VERIFIED'],
             'status':'IN_PROGRESS','progress_boundary':'Current host recovery receipts do not close Android/device conditions or the 328 matrix rows.'}
    save(OUT/'EXECUTION_STATE.json',state)
    save(OUT/'NEW_TEST_EXECUTION_LEDGER.json', {'NEW-TEST-001':{'status':'HOST_PASS','first_failure_exit_code':1,'fixed_exit_code':0,
         'sources':json.loads((OUT/'evidence/SCC-022/fixed/CURRENT_EXECUTION.json').read_text())['sources'],
         'scope':'Current production mailbox. Framework adapters on host, no device proof.'}})
    print(json.dumps({'requirements':len(req),'cards':len(cards),'original_detections':166,'new_detections':6,'matrix_tests':len(tests),
          'manifest_entries':entries,'manifest_failures':len(manifest_failures),'parse_errors':len(errors),'report_hash_failures':report_failures,'integrity':integrity},ensure_ascii=False))
if __name__ == '__main__': main()
