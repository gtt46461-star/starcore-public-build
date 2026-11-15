#!/usr/bin/env python3
"""R27-054 build identity asset generator (runs at preBuild). Writes <out>/r27/build-identity.json with the exact
checkout commit, tree hash, dirty flag, version, toolchain pins and declared dependencies, and copies the R27 card
registry, the source audit and the R26→R27 merge status into the same asset folder for the report chapters
IDENTITY / BUILD_IDENTITY / SUPPLY_CHAIN / REQUIREMENTS / SOURCE / MERGE. The APK hash itself is computed at runtime
(an APK cannot contain its own hash). Usage: build_identity.py <assets-out-dir>"""
import json, re, subprocess, sys, shutil
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
out = Path(sys.argv[1]) / 'r27'; out.mkdir(parents=True, exist_ok=True)
def git(*a):
    try: return subprocess.check_output(['git', *a], cwd=ROOT, text=True).strip()
    except Exception as e: return 'UNAVAILABLE:' + type(e).__name__
manifest = json.loads((ROOT / 'release/release_manifest.json').read_text())
deps = []
for f in ('build.gradle', 'app/build.gradle'):
    for line in (ROOT / f).read_text().splitlines():
        m = re.search(r"(id|implementation|testImplementation|androidTestImplementation|coreLibraryDesugaring)\s*\(?\s*'([^']+)'\s*\)?(?:\s*version\s*'([^']+)')?", line)
        if m: deps.append({'file': f, 'kind': m.group(1), 'coordinate': m.group(2), 'version': m.group(3)})
wrapper = (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text()
identity = {
    'schema': 'R27_BUILD_IDENTITY_1', 'commit': git('rev-parse', 'HEAD'), 'tree': git('rev-parse', 'HEAD^{tree}'),
    'dirty': bool(git('status', '--porcelain', '--untracked-files=no')), 'applicationId': manifest['applicationId'],
    'versionName': manifest['versionName'], 'versionCode': manifest['versionCode'], 'baselineCommit': '1713d114ebbd07ebd6544a29acfbce1bd883215c',
    'toolchain': {'gradleDistribution': re.search(r'distributionUrl=(.*)', wrapper).group(1).replace('\\', ''), 'compileSdk': manifest['compileSdk'], 'minSdk': manifest['minSdk'], 'targetSdk': manifest['targetSdk'],
                  'buildTools': '36.0.0', 'ndk': '28.2.13676358', 'cmake': '3.22.1', 'java': 17},
    'dependencies': deps, 'apkSha256': 'COMPUTED_AT_RUNTIME_BY_REPORT_EXPORT',
}
(out / 'build-identity.json').write_text(json.dumps(identity, ensure_ascii=False, sort_keys=True, indent=1))
plan = ROOT / 'construction/plans/STARCORE_R27_FINAL_20261028'
for src, dst in ((plan / 'registries/r27_cards.json', 'r27_cards.json'), (ROOT / 'evidence/r27/source_audit.json', 'source-audit.json'), (ROOT / 'evidence/r27/merge_status.json', 'merge-status.json')):
    if src.is_file(): shutil.copyfile(src, out / dst)
    else: (out / dst).write_text(json.dumps({'status': 'INSUFFICIENT', 'reason': 'missing ' + str(src.relative_to(ROOT))}, ensure_ascii=False))
print('R27_BUILD_IDENTITY_WRITTEN', identity['commit'], identity['tree'])
