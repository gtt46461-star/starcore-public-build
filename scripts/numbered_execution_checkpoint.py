#!/usr/bin/env python3
"""Preserve prior ledgers and attach current numbered navigation without closing untested clauses."""
import argparse, collections, csv, gzip, hashlib, json, subprocess, tarfile
from pathlib import Path

csv.field_size_limit(256 * 1024 * 1024)
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'construction/numbered'
NAMES = ['REQUIREMENT_EXECUTION_LEDGER.csv', 'CARD_EXECUTION_LEDGER.csv',
         'DETECTION_EXECUTION_LEDGER.csv', 'TEST_EXECUTION_LEDGER.csv']

def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda: f.read(1048576), b''): h.update(block)
    return h.hexdigest()

def rows(path):
    with path.open(encoding='utf-8-sig', newline='') as f: return list(csv.DictReader(f))

def write_rows(path, data):
    fields = list(dict.fromkeys(k for row in data for k in row))
    with path.open('w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fields); writer.writeheader(); writer.writerows(data)

def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--numbered-plan', type=Path, required=True)
    p.add_argument('--current-commit', required=True)
    a = p.parse_args(); plan = a.numbered_plan.resolve(); numbered = plan/'NUMBERED_EXECUTION'
    original = plan/'ORIGINAL_PLAN'; OUT.mkdir(parents=True, exist_ok=True)
    source = ROOT/'construction/r998'
    # Existing working ledgers always take precedence over either archive or empty templates.
    with tarfile.open(ROOT/'release/r998_execution_ledger.tar.gz') as archive:
        saved = {Path(m.name).name: m for m in archive.getmembers() if m.isfile()}
        for name in NAMES:
            dst = OUT/name
            if not dst.exists():
                old = source/name
                dst.write_bytes(old.read_bytes() if old.exists() else archive.extractfile(saved[name]).read())
    req, cards, det, tests = [rows(OUT/name) for name in NAMES]
    assert len(req) == len({r['requirement_id'] for r in req}) == 1832
    assert len(cards) == len({r['card_id'] for r in cards}) == 299
    assert len(tests) == len({r['test_id'] for r in tests}) == 328
    assert sum(r.get('source_cohort') == 'ORIGINAL_R980' for r in det) == 166
    assert collections.Counter(r['semantic_disposition'] for r in req) == {
        'PLAN_ARTIFACT':530, 'EXACT_SUPPORTED':860, 'SUPERSEDED_EQUIVALENT':426, 'HISTORICAL_REFERENCE':16}
    for r in req:
        assert hashlib.sha256(r['exact_requirement'].encode()).hexdigest() == r['exact_requirement_sha256']
    maps = [(req, 'REQUIREMENTS_NUMBERED_1832.csv', 'requirement_id'),
            (cards, 'MASTER_CARD_SEQUENCE.csv', 'card_id'),
            (det, 'DETECTIONS_NUMBERED_166.csv', 'issue_id'),
            (tests, 'TESTS_NUMBERED_328.csv', 'test_id')]
    for data, filename, key in maps:
        lookup = {r[key]:r for r in rows(numbered/filename)}
        for r in data:
            for k, v in lookup.get(r[key], {}).items():
                # Original ledger fields are immutable; new navigation is additive.
                if k not in r: r[k] = v
            if 'final_closure_status' not in r: r['final_closure_status'] = 'PENDING'
    subfile = OUT/'SUBCONDITION_EXECUTION_LEDGER.csv'
    sub = rows(subfile) if subfile.exists() else []
    existing = {r['subcondition_id'] for r in sub}
    for r in cards:
        cid = r['card_id']; cardpath = original/'31_FILE_CONSTRUCTION_CARDS/card_data'/f'{cid}.json'
        card = json.loads(cardpath.read_text()); fields = card['fixed_contract_fields']
        for field, values in fields.items():
            if not isinstance(values, list): values = [values]
            for i, value in enumerate(values, 1):
                sid = f'{cid}:{field}:{i}'
                if sid in existing: continue
                sub.append({'subcondition_id':sid, 'parent_kind':'CARD', 'parent_id':cid,
                    'execution_id':r.get('execution_id',''), 'criterion_kind':field,
                    'exact_criterion':value if isinstance(value,str) else json.dumps(value,ensure_ascii=False),
                    'source_path':str(cardpath.relative_to(plan)), 'source_sha256':sha(cardpath),
                    'source_field':'fixed_contract_fields.'+field, 'decomposition_status':'REVIEW_COMPOUND_CRITERION',
                    'actual_method':'', 'caller_callee':'', 'current_source_sha256':'',
                    'evidence_index':'', 'evidence_scope':'', 'test_id':'', 'assertion':'',
                    'status':'NOT_STARTED', 'blocker':'SEMANTIC_AND_CURRENT_EXECUTION_REVIEW_PENDING'})
                existing.add(sid)
    # Requirement criteria are preserved verbatim. Compound clauses stay open until reviewed/split.
    for r in req:
        sid = r['requirement_id']+':acceptance:1'
        if sid not in existing:
            sub.append({'subcondition_id':sid, 'parent_kind':'REQUIREMENT', 'parent_id':r['requirement_id'],
                'execution_id':r.get('execution_card_refs',''), 'criterion_kind':'canonical_acceptance',
                'exact_criterion':r['canonical_acceptance'], 'source_path':'ORIGINAL_PLAN/38_REQUIREMENTS_TRACEABILITY/REQUIREMENTS_1832_SEMANTIC_REVIEW_CP12.csv',
                'source_sha256':r['original_source_file_sha256'], 'source_field':'canonical_acceptance',
                'decomposition_status':'REVIEW_COMPOUND_CRITERION', 'actual_method':'', 'caller_callee':'',
                'current_source_sha256':'', 'evidence_index':'', 'evidence_scope':'', 'test_id':'',
                'assertion':'', 'status':'NOT_STARTED', 'blocker':'SEMANTIC_AND_CURRENT_EXECUTION_REVIEW_PENDING'})
    write_rows(subfile, sub)
    for name, data in zip(NAMES, [req,cards,det,tests]): write_rows(OUT/name,data)
    summary = {
        'requirements': {'denominator':1832,'states':dict(collections.Counter(r.get('execution_status',r.get('current_status')) for r in req)),
                         'semantic_categories':dict(collections.Counter(r['semantic_disposition'] for r in req)),
                         'final_closed':sum(r['final_closure_status']=='CLOSED' for r in req)},
        'cards': {'denominator':299,'actions':dict(collections.Counter(r['disposition'] for r in cards)),
                  'code_states':dict(collections.Counter(r['code_status'] for r in cards)),
                  'final_closed':sum(r['final_closure_status']=='CLOSED' for r in cards)},
        'detections':{'original':166,'additional':len(det)-166},
        'tests':{'denominator':328,'states':dict(collections.Counter(r['execution_status'] for r in tests))},
        'subconditions':{'rows':len(sub),'decomposition_pending':sum(r['decomposition_status']=='REVIEW_COMPOUND_CRITERION' for r in sub)}
    }
    save(OUT/'PROGRESS_COUNTS.json', summary)
    if not (OUT/'EXECUTION_STATE.json').exists():
        save(OUT/'EXECUTION_STATE.json', {
            'schema_version':3, 'branch':'construction/numbered-resume-20261006-1121',
            'baseline_commit':'4fab11e104beb314a8689c5c41d30097c0ce23b9',
            'predecessor_baseline':'71cd35da8fa0b76108c69f1b75049d14b79e9cd6',
            'report_commit':'9ca5bc1ad0ce47cf85b6b435606c85629d4f88c2',
            'current_commit':a.current_commit, 'current_execution':'EXEC-0001',
            'current_card':'FILE-CAN-003','current_group':'SCC-001',
            'status':'IN_PROGRESS','counts':summary,
            'next_action':'Complete current Android compilation and regenerate bytecode-bound source-call archive; resume each original dependency group.',
            'blockers':['DEVICE_NOT_VERIFIED','SIGNING_KEY_NOT_AVAILABLE_IN_CURRENT_INPUTS',
                        'ETHEREUM_API_DATA_IDENTITY_MIGRATION_PENDING','NATURAL_FUTURE_NOT_VERIFIED'],
            'evidence_boundary':'Prior receipts preserved. No historical PASS is promoted to current PASS; compound subconditions remain open.'})
    else:
        state = json.loads((OUT/'EXECUTION_STATE.json').read_text())
        state['current_commit'] = a.current_commit
        state['counts'] = summary
        save(OUT/'EXECUTION_STATE.json', state)
    print(json.dumps(summary,ensure_ascii=False))

if __name__ == '__main__': main()
