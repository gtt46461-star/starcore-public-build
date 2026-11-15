#!/usr/bin/env python3
"""R31-B12-C02 stage receipts for .github/workflows/r31-verification.yml.

A stage is PASS only when it really executed tests and none failed:
  * JUnit XML is counted per testcase (tests/failures/errors/skipped); zero executed testcases is EMPTY, not PASS;
  * skipped testcases are reported by name and make the stage PASS_WITH_SKIPPED, never a silent PASS;
  * a job GitHub reports as skipped/cancelled (or that left no result) is NOT_RUN;
  * a test method that is empty or only checks that a class exists / has a name is refused (CLASS_NAME_ONLY);
  * every executed_by id of tools/r31/fault_injection_manifest.json must exist (HOST_SUITE id or a real @Test method).
"""
import argparse, hashlib, json, re, sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TRIVIAL = re.compile(r'^(?:\s*(?:assert(?:NotNull|True|Equals)\s*\((?:[^;]*\.class\b[^;]*|[^;]*getSimpleName\(\)[^;]*|[^;]*getName\(\)[^;]*)\)|Class\.forName\s*\([^;]*\)|[A-Za-z_][\w.]*\.class(?:\.getName\(\))?)\s*;\s*)*$')


def parse_junit(directory):
    """Counts every testcase in every TEST-*.xml under directory."""
    out = {'files': 0, 'tests': 0, 'failures': [], 'errors': [], 'skipped': [], 'passed': 0}
    for f in sorted(Path(directory).rglob('*.xml')) if directory and Path(directory).exists() else []:
        try:
            root = ET.parse(f).getroot()
        except ET.ParseError as e:
            out['errors'].append('%s:XML_PARSE:%s' % (f.name, e)); continue
        suites = [root] if root.tag == 'testsuite' else root.findall('testsuite')
        if not suites:
            continue
        out['files'] += 1
        for suite in suites:
            for case in suite.iter('testcase'):
                name = '%s#%s' % (case.get('classname', '?'), case.get('name', '?'))
                out['tests'] += 1
                if case.find('failure') is not None: out['failures'].append(name)
                elif case.find('error') is not None: out['errors'].append(name)
                elif case.find('skipped') is not None: out['skipped'].append(name)
                else: out['passed'] += 1
    return out


def stage_status(job_result, junit=None, expect_tests=True):
    """job_result is GitHub's needs.<job>.result (success/failure/cancelled/skipped) or '' when the job never ran."""
    if job_result in ('', None, 'skipped', 'cancelled'):
        return 'NOT_RUN'
    if junit is not None and expect_tests:
        if junit['failures'] or junit['errors']:
            return 'FAIL'
        if junit['tests'] == 0:
            return 'EMPTY'
    if job_result != 'success':
        return 'FAIL'
    if junit is not None and junit['skipped']:
        return 'PASS_WITH_SKIPPED:%d' % len(junit['skipped'])
    return 'PASS'


def _bodies(text):
    """(method, body) for every @Test method, brace-matched."""
    for m in re.finditer(r'@(?:org\.junit\.)?Test\b(?:\([^)]*\))?\s*(?:@[\w.]+(?:\([^)]*\))?\s*)*(?:public\s+)?(?:static\s+)?void\s+(\w+)\s*\([^)]*\)\s*(?:throws[^{]*)?\{', text):
        depth, i = 1, m.end()
        while i < len(text) and depth:
            depth += {'{': 1, '}': -1}.get(text[i], 0); i += 1
        yield m.group(1), text[m.end():i - 1]


def scan_tests(root):
    """Finds test methods that are empty or only check a class name; returns {'methods': n, 'refused': [...]}."""
    refused, methods = [], 0
    for f in sorted(Path(root).rglob('*.java')):
        text = f.read_text(encoding='utf-8', errors='replace')
        if '@Test' not in text:
            continue
        for name, body in _bodies(text):
            methods += 1
            code = re.sub(r'//[^\n]*|/\*.*?\*/', '', body, flags=re.S).strip()
            if not code:
                refused.append('%s#%s:EMPTY' % (f.stem, name))
            elif TRIVIAL.match(code):
                refused.append('%s#%s:CLASS_NAME_ONLY' % (f.stem, name))
    return {'methods': methods, 'refused': refused}


def check_manifest(manifest_path, suite_path, test_root):
    manifest = json.loads(Path(manifest_path).read_text())
    hosts = {h['id'] for h in json.loads(Path(suite_path).read_text())['host'] if not h.get('retired')}
    tests = {}
    for f in Path(test_root).rglob('*.java'):
        text = f.read_text(encoding='utf-8', errors='replace')
        pkg = re.search(r'^package\s+([\w.]+);', text, re.M)
        for name, _ in _bodies(text):
            tests.setdefault('%s.%s#%s' % (pkg.group(1) if pkg else '', f.stem, name), True)
    problems = []
    for s in manifest['scenarios']:
        if not s.get('executed_by'): problems.append(s['id'] + ':NO_EXECUTOR')
        if not s.get('invariants'): problems.append(s['id'] + ':NO_INVARIANT')
        for ref in s.get('executed_by', []):
            if ref.startswith('HOST:'):
                if ref[5:] not in hosts: problems.append('%s:UNKNOWN_HOST:%s' % (s['id'], ref))
            elif ref not in tests:
                problems.append('%s:UNKNOWN_TEST:%s' % (s['id'], ref))
        if s.get('android') not in ('NOT_RUN',) and not str(s.get('android', '')).startswith('EMU_RECEIPT:'):
            problems.append('%s:ANDROID_CLAIM_WITHOUT_RECEIPT' % s['id'])
    for k in manifest['invariants']:
        if not any(k in s['invariants'] for s in manifest['scenarios']): problems.append('INVARIANT_UNUSED:' + k)
    return problems


def aggregate(stage_dir):
    """Combines stage receipts; any stage that is not PASS keeps the overall result from being PASS."""
    stages = {}
    for f in sorted(Path(stage_dir).glob('stage-*.json')):
        r = json.loads(f.read_text()); stages[r['stage']] = r
    overall = 'PASS' if stages and all(r['status'] == 'PASS' for r in stages.values()) else 'NOT_PASS'
    digests = {name: r.get('artifacts', {}) for name, r in stages.items()}
    return {'format': 'R31_VERIFICATION_RECEIPT_V1', 'overall': overall, 'stages': stages, 'artifactDigests': digests,
            'device': 'NOT_RUN', 'naturalQuality': 'NOT_PROVEN'}


def sha256(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--stage'); ap.add_argument('--result', default='')
    ap.add_argument('--junit'); ap.add_argument('--no-tests-expected', action='store_true')
    ap.add_argument('--artifact', action='append', default=[])
    ap.add_argument('--out'); ap.add_argument('--aggregate'); ap.add_argument('--check-manifest', action='store_true')
    a = ap.parse_args()
    if a.check_manifest:
        p = check_manifest(ROOT / 'tools/r31/fault_injection_manifest.json', ROOT / 'construction/r31/HOST_SUITE.json', ROOT / 'app/src/test/java')
        scan = scan_tests(ROOT / 'app/src/test/java')
        print(json.dumps({'manifestProblems': p, 'testMethods': scan['methods'], 'refusedTests': scan['refused']}))
        sys.exit(1 if p or scan['refused'] else 0)
    if a.aggregate:
        r = aggregate(a.aggregate); Path(a.out).write_text(json.dumps(r, indent=1, sort_keys=True) + '\n'); print(json.dumps({'overall': r['overall'], 'stages': {k: v['status'] for k, v in r['stages'].items()}}))
        return
    junit = parse_junit(a.junit) if a.junit else None
    status = stage_status(a.result, junit, not a.no_tests_expected)
    arts = {p: sha256(p) for p in a.artifact if Path(p).is_file()}
    r = {'stage': a.stage, 'jobResult': a.result or 'NOT_RUN', 'status': status, 'junit': junit, 'artifacts': arts}
    Path(a.out).parent.mkdir(parents=True, exist_ok=True); Path(a.out).write_text(json.dumps(r, indent=1, sort_keys=True) + '\n')
    print('R31_STAGE|%s|%s' % (a.stage, status))


if __name__ == '__main__':
    main()
