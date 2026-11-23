#!/usr/bin/env python3
"""Verify sealed R34 plan bytes and internal mappings, not application behavior.

The seal is an integrity inventory, not a signature. Compare its printed SHA256
with the independently reviewed Git commit or bundle receipt. Reports/source
are optional local read-only inputs; their absence is never reported as tested.
"""
import argparse, collections, hashlib, json, pathlib, re, subprocess

BASE = '273641e838e0b8dd842b42a67a674e184942a30a'
SEAL = 'MANIFEST_SHA256.json'

def sha(p):
    h = hashlib.sha256()
    with p.open('rb') as f:
        for block in iter(lambda: f.read(1 << 20), b''):
            h.update(block)
    return h.hexdigest()

def inventory(root):
    return sorted(p for p in root.rglob('*') if p.is_file()
                  and '__pycache__' not in p.parts and p.suffix != '.pyc'
                  and p.relative_to(root).as_posix() != SEAL)

def check(root, source=None, reports=None):
    root = root.resolve()
    errors, counts = [], {}
    def need(ok, msg):
        if not ok:
            errors.append(msg)
    def path(name):
        rel = pathlib.PurePosixPath(name)
        if rel.is_absolute() or '..' in rel.parts:
            raise ValueError('UNSAFE_PLAN_PATH:' + str(name))
        p = root.joinpath(*rel.parts)
        if p.is_symlink() or not p.resolve().is_relative_to(root):
            raise ValueError('SYMLINK_OR_ESCAPE:' + str(name))
        return p
    def read(name):
        return json.loads(path(name).read_text(encoding='utf-8'))
    seal = read(SEAL)
    records = seal['files']
    declared = [r['path'] for r in records]
    need(len(declared) == len(set(declared)), 'DUPLICATE_SEAL_PATH')
    actual = {p.relative_to(root).as_posix() for p in inventory(root)}
    need(set(declared) == actual, 'SEAL_INVENTORY:' + repr(sorted(set(declared) ^ actual)))
    for item in records:
        p = path(item['path'])
        need(p.is_file(), 'MISSING:' + item['path'])
        if p.is_file():
            need(p.stat().st_size == item['bytes'] and sha(p) == item['sha256'],
                 'HASH_OR_SIZE:' + item['path'])
    counts['sealed_files'] = len(records)
    scope, work, cards, cross = (read(x) for x in
        ['FINAL_SCOPE.json', 'WORK_ORDERS.json', 'FILE_CARDS.json', 'R335_CROSSWALK.json'])
    batches = work['batches']
    issues = {i['id']: i for i in work['issues']}
    files = {f['production_path']: f for f in cards['files']}
    need(work['source_baseline'] == scope['source_baseline'] == BASE, 'SOURCE_BASELINE')
    need(len(batches) == scope['batch_count'] == 13, 'BATCH_COUNT')
    need(len(issues) == len(work['issues']) == scope['issue_count'] == 66, 'ISSUE_COUNT')
    need([b['id'] for b in batches] == [f'B{i:02d}' for i in range(13)], 'BATCH_ORDER')
    for ix, b in enumerate(batches):
        need(b['predecessor'] == (f'B{ix-1:02d}' if ix else None), 'PREDECESSOR:' + b['id'])
        need(b['issue_count'] == len(b['issues']), 'BATCH_ISSUES:' + b['id'])
        for ident in b['issues']:
            need(ident in issues and issues[ident]['batch'] == b['id'], 'ISSUE_OWNER:' + ident)
            need(path(f"batches/{b['id']}/issues/{ident}.md").is_file(), 'ISSUE_CARD:' + ident)
    for ident, issue in issues.items():
        need(bool(issue['implementation']) and bool(issue['verification']), 'EMPTY_CONTRACT:' + ident)
        for name in issue['paths']:
            need(name in files and ident in files[name]['issue_ids'], 'FILE_BACKLINK:' + ident + ':' + name)
    for name, item in files.items():
        need(path('files/' + item['file_id'] + '.md').is_file(), 'FILE_CARD:' + name)
        for ident in item['issue_ids']:
            need(ident in issues and name in issues[ident]['paths'], 'ISSUE_BACKLINK:' + name + ':' + ident)
        if item['before_state'] == 'EXISTING_095':
            p = path('source_before/' + name)
            need(p.is_file() and sha(p) == item['before_sha256'], 'BEFORE_HASH:' + name)
            if source:
                actual_p = source / name
                need(actual_p.is_file() and sha(actual_p) == item['before_sha256'], 'SOURCE_HASH:' + name)
        else:
            need(item['before_sha256'] is None, 'NEW_PATH_HAS_BEFORE:' + name)
        mat = item['code_material']
        if mat.get('path'):
            p = path(mat['path'])
            need(p.is_file() and sha(p) == mat['sha256'], 'PAYLOAD_HASH:' + name)
            need(mat['applies_in_batch'] in item['owner_batches'], 'PAYLOAD_OWNER:' + name)
            for b, bm in item['batch_materials'].items():
                need(b == mat['applies_in_batch'] or not bm.get('path'), 'FUTURE_BATCH_PAYLOAD:' + name + ':' + b)
    legacy = cross['requirements']
    oldids = [r['legacy_id'] for r in legacy]
    need(len(set(oldids)) == len(oldids) == cross['legacy_count'] == 66, 'LEGACY_COVERAGE')
    for old in legacy:
        need(bool(old['new_issue_ids']), 'LEGACY_NO_OWNER:' + old['legacy_id'])
        for ident in old['new_issue_ids']:
            need(ident in issues and any(o['id'] == old['legacy_id'] for o in issues[ident].get('legacy_requirements', [])),
                 'LEGACY_BACKLINK:' + old['legacy_id'] + ':' + ident)
        need(bool(old.get('test_contracts')), 'LEGACY_NO_TEST_CONTRACT:' + old['legacy_id'])
        for test in old.get('test_contracts', []):
            need(test.get('state') in ['EXISTING_ENTRY_REQUIRES_REVALIDATION', 'NEW_TEST_REQUIRED'],
                 'LEGACY_TEST_STATE:' + old['legacy_id'])
            need(bool(test.get('path')) and bool(test.get('assertions')), 'LEGACY_TEST_INCOMPLETE:' + old['legacy_id'])
            if source and test.get('state') == 'EXISTING_ENTRY_REQUIRES_REVALIDATION' and test.get('root') == 'SOURCE_095':
                t = source/test['path']
                need(t.is_file(), 'LEGACY_TEST_MISSING:' + old['legacy_id'] + ':' + test['path'])
                if t.is_file():
                    need(sha(t) == test.get('observed_095_sha256'), 'LEGACY_TEST_HASH:' + old['legacy_id'] + ':' + test['path'])
                    need(re.search(r'\b'+re.escape(test.get('method',''))+r'\s*\(',t.read_text(encoding='utf-8')) is not None,
                         'LEGACY_TEST_METHOD:' + old['legacy_id'])
    for ident, issue in issues.items():
        inherited = {o['id'] for o in issue.get('legacy_requirements', [])}
        for assertion in issue['mandatory_assertions']:
            name = assertion['legacy_id']
            # Short legacy names in detailed assertions map to the full R335 prefix.
            full = name if name.startswith('R335-') else 'R335-' + name
            need(full in inherited, 'MANDATORY_NOT_IN_CROSSWALK:' + ident + ':' + full)
    observed = dict(collections.Counter(f['code_material']['material_type'] for f in files.values()))
    need(observed == scope['code_material_counts'], 'MATERIAL_COUNTS')
    need(len(files) == cards['file_count'] == scope['source_file_count'], 'FILE_COUNT')
    catalog = read('CHART_MATRIX_CATALOG.json')
    need(len(catalog['research_charts']) == catalog['chart_count'] == 18, 'CHART_COUNT')
    need(len(catalog['r930_matrices']) + 1 == catalog['matrix_count'] == 20, 'MATRIX_COUNT')
    # Check links in the constructed navigation/cards. Historical audit quotations
    # retain their original workspace context and are described in SOURCE_MAP.md.
    docs = list(root.glob('*.md')) + list((root/'files').glob('*.md')) + list((root/'batches').rglob('*.md'))
    for doc in docs:
        for ref in re.findall(r'\]\(([^\s)]+)\)', doc.read_text(encoding='utf-8')):
            if ':' in ref or ref.startswith('#'):
                continue
            ref = ref.split('#', 1)[0]
            if ref:
                dest = (doc.parent/ref).resolve()
                need(dest.is_relative_to(root) and dest.exists(), 'LOCAL_LINK:' + doc.relative_to(root).as_posix() + ':' + ref)
    report_state = 'NOT_CHECKED_NO_REPORT_PATH'
    if reports:
        inputs = read('evidence/ORIGINAL_INPUT_MANIFEST.json')['rawFiles']
        for item in inputs:
            p = reports/pathlib.Path(item['file']).name
            need(p.is_file() and p.stat().st_size == item['bytes'] and sha(p) == item['sha256'], 'RAW_REPORT:' + p.name)
        counts['raw_reports_checked'] = len(inputs)
        report_state = 'CHECKED'
    source_state = 'NOT_CHECKED_NO_SOURCE_PATH'
    if source:
        out = subprocess.run(['git','-C',str(source),'rev-parse','HEAD'],capture_output=True,text=True)
        need(out.returncode == 0 and out.stdout.strip() == BASE, 'SOURCE_GIT_HEAD')
        source_state = 'CHECKED'
    counts.update({'batches':len(batches),'issues':len(issues),'legacy_requirements':len(legacy),
                   'file_cards':len(files),'code_materials':observed})
    return {'schema':'STARCORE_R34_PLAN_CHECK_1','status':'PASS' if not errors else 'FAIL',
            'errors':errors,'counts':counts,'plan_manifest_sha256':sha(path(SEAL)),
            'source_readback':source_state,'original_report_readback':report_state,
            'android_build':'NOT_RUN','apk':'NOT_BUILT','device':'NOT_RUN',
            'natural_quality':'NOT_PROVEN','scope':'PACKAGE_BYTES_MAPPINGS_AND_CONTRACTS_ONLY'}

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--plan',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[2])
    parser.add_argument('--source',type=pathlib.Path)
    parser.add_argument('--reports',type=pathlib.Path)
    args=parser.parse_args()
    try:
        result=check(args.plan,args.source.resolve() if args.source else None,args.reports.resolve() if args.reports else None)
    except (OSError,ValueError,KeyError,TypeError) as error:
        result={'status':'FAIL','errors':[str(error)],'scope':'PACKAGE_VALIDATION_FAILED'}
    print(json.dumps(result,ensure_ascii=False,indent=2))
    raise SystemExit(result['status']!='PASS')

if __name__=='__main__':main()
