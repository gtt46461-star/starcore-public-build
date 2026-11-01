"""CI evidence correction only; never build, replace or change the published application APK."""
import argparse
import hashlib
import json
import os
import pathlib
import re
import subprocess
import zipfile

APP = 'a94ee9dae4e0165f8b2b340bbcb23f4be3a8743f'
APK = 'STARCORE-R30-REPAIR1-EXISTING_R27-a94ee9dae4e0.apk'
APK_SHA = 'cd46dd5b287d051e6968e96c57998608a1d9d50d69d64b1e61cecb98c12593ae'
CERT = 'd8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7'
TAG = 'r30-repair1-577311002-a94ee9dae4e0'
TESTS = ['app/src/androidTest/java/com/ethk3/novaonepro/' + f for f in (
    'R30ChartsAndroidTest.java', 'R30RepairChartsAndroidTest.java', 'R30GalleryCaptureR30.java')]
PRODUCT = ['app/src/main', 'app/build.gradle', 'build.gradle', 'settings.gradle',
           'gradle.properties', 'gradle', 'scripts', 'release',
           'construction/r997/REQUIREMENTS_LEDGER.csv.gz',
           'construction/r997/CARD_LEDGER.csv.gz', 'construction/r997/SUBITEMS.csv']
PREFIX = 'android/evidence/r30/android-regression/'

def command(*args):
    return subprocess.check_output(args).decode().strip()

def sha(data):
    return hashlib.sha256(data).hexdigest()

def load(path):
    return json.loads(pathlib.Path(path).read_text())

def write(path, obj):
    pathlib.Path(path).write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n')

def unique(root, name):
    rows = list(pathlib.Path(root).rglob(name))
    assert len(rows) == 1, (name, rows)
    return rows[0]

def prepare(a):
    assert a.app == APP and re.fullmatch('[a-f0-9]{40}', a.tests)
    assert command('git', 'rev-parse', 'HEAD') == APP
    assert not command('git', 'status', '--porcelain', '--untracked-files=no')
    assert not command('git', 'diff', APP, a.tests, '--', *PRODUCT), 'PRODUCT_SOURCE_CHANGED'
    hashes = {}
    for name in TESTS:
        data = subprocess.check_output(['git', 'show', a.tests + ':' + name])
        pathlib.Path(name).write_bytes(data)
        hashes[name] = sha(data)
    assert not command('git', 'diff', '--', *PRODUCT), 'PRODUCT_WORKTREE_CHANGED'
    changed = set(command('git', 'diff', '--name-only').splitlines())
    assert changed == set(TESTS[:2]), changed  # third file is new, intentionally untracked
    assert pathlib.Path(TESTS[2]).is_file()
    context = {'appSourceCommit': APP, 'instrumentationSourceCommit': a.tests,
               'testSourceSha256': hashes, 'applicationProductPathsUnchanged': True,
               'applicationApkRebuiltOrReplaced': False,
               'workflowRun': int(os.environ['GITHUB_RUN_ID']),
               'workflowAttempt': int(os.environ.get('GITHUB_RUN_ATTEMPT', '1')),
               'originalFullAcceptanceRun': 41357264729,
               'physicalDevice': 'NOT_RUN', 'naturalQuality': 'NOT_PROVEN',
               'correctionReason': 'Manual review found R01/R02/R03 identical R01 screenshots; '
                                   'replace gallery only after actual visible-target/frame checks.'}
    write(a.context, context)
    print('R30_GALLERY|EXACT_APP_SOURCE_WITH_ONLY_DECLARED_TEST_FIXES|PASS')

def verify_apk(a):
    root = pathlib.Path(a.previous)
    apk = root / APK
    assert apk.stat().st_size == 28059055 and sha(apk.read_bytes()) == APK_SHA
    row = load(root / 'delivery.json')[0]
    assert row['sourceCommit'] == APP and row['sha256'] == APK_SHA
    sdk = pathlib.Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
    certificate = command(str(sdk / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk))
    assert re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})', certificate).group(1) == CERT
    badging = command(str(sdk / 'aapt'), 'dump', 'badging', str(apk)).splitlines()[0]
    assert "name='com.starcore.research.r13'" in badging and "versionCode='577311002'" in badging
    manifest = load(root / 'ACCEPTANCE.json')
    assert sha((root / 'ANDROID_ACCEPTANCE_EVIDENCE.zip').read_bytes()) == manifest['evidenceArchive']['sha256']
    context = load(a.context)
    context.update(apk=APK, apkSha256=APK_SHA, apkBytes=apk.stat().st_size,
                   certificateSha256=CERT, versionCode=577311002, releaseTag=TAG)
    write(a.context, context)
    print('R30_GALLERY|UNCHANGED_PUBLISHED_APK_HASH_SOURCE_VERSION_CERTIFICATE|PASS')

def merge(a):
    context = load(a.context)
    assert context['appSourceCommit'] == APP and context['apkSha256'] == APK_SHA
    assert command('git', 'rev-parse', 'HEAD') == APP
    assert not command('git', 'diff', '--', *PRODUCT)
    root = pathlib.Path(a.fresh)
    original = load(unique(root, 'regression.json'))
    repair = load(unique(root, 'repair-chart-acceptance.json'))
    states = load(unique(root, 'states.json'))
    assert original['source'] == repair['sourceCommit'] == APP
    assert original['junitCases'] == 17 and original['processes'] == 11
    assert repair['screenshots'] == 188 and repair['registeredObjects'] == 47
    expected = {f'{s}-{i:02d}.png' for s in ('READY', 'EMPTY', 'INSUFFICIENT', 'READ_ERROR') for i in range(29)}
    expected |= {f'{s}-R{i:02d}.png' for s in ('READY', 'EMPTY', 'INSUFFICIENT', 'READ_ERROR') for i in range(1, 19)}
    images = list(root.rglob('*.png'))
    assert len(images) == 188 and {p.name for p in images} == expected
    proofs = original['screenshots'] + repair['artifacts']
    assert len(proofs) == 188
    hashes = {pathlib.Path(p['file']).name: p['sha256'] for p in proofs}
    for file in images:
        assert file.read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
        assert sha(file.read_bytes()) == hashes[file.name]
    assert len(states) == 72
    assert {(x['scenario'], x['id']) for x in states} == {(s, f'R{i:02d}') for s in ('READY', 'EMPTY', 'INSUFFICIENT', 'READ_ERROR') for i in range(1, 19)}
    critical = []
    for row in states:
        capture = row['capture']
        assert capture['title'].startswith(row['id']), row
        assert capture['frameSync'] in ('API29_FRAME_COMMIT', 'CHOREOGRAPHER_TWO_FRAMES')
        assert capture['screenshotWidth'] >= 900 and capture['screenshotHeight'] >= 1600
        assert len(capture['visibleTitle']) == 4 and capture['scrollY'] >= 0
        if row['scenario'] == 'READY' and row['id'] in ('R01', 'R02', 'R03'):
            critical.append(capture['pixelSha256'])
    assert len(critical) == len(set(critical)) == 3, 'CRITICAL_REAL_FRAMES_DUPLICATED'
    assert len({hashes[f'READY-{n:02d}.png'] for n in range(29)}) == 29
    context.update(result='PASS', actualActivityScreenshots=188, actualRegisteredObjects=47,
                   originalAndroidTests=17, additionalWorkbenchTests=1,
                   visibleTargetAndFrameChecks=True, criticalR01R02R03FramesDistinct=True,
                   frameSyncModes=sorted({x['capture']['frameSync'] for x in states}),
                   emulatorProfile='pixel_4', originalFaultCells=14, repairFaultCells=12,
                   faultEvidenceScope='Original C7 exact same APK; not reexecuted by gallery correction',
                   originalEvidenceArchiveSha256=sha((pathlib.Path(a.previous) / 'ANDROID_ACCEPTANCE_EVIDENCE.zip').read_bytes()))
    write(a.context, context)
    replacements = {PREFIX + str(p.relative_to(root)): p.read_bytes() for p in images}
    for name in ('regression.json', 'repair-chart-acceptance.json', 'states.json'):
        file = unique(root, name)
        replacements[PREFIX + str(file.relative_to(root))] = file.read_bytes()
    previous = pathlib.Path(a.previous)
    output = pathlib.Path(a.output)
    output.mkdir(parents=True, exist_ok=True)
    preserved = {}
    with zipfile.ZipFile(previous / 'ANDROID_ACCEPTANCE_EVIDENCE.zip') as src:
        names = src.namelist()
        assert len(names) == len(set(names)) and set(replacements) <= set(names)
        with zipfile.ZipFile(output / 'ANDROID_ACCEPTANCE_EVIDENCE.zip', 'w', compression=zipfile.ZIP_DEFLATED) as dst:
            for name in names:
                data = replacements.get(name, src.read(name))
                dst.writestr(name, data)
                if name not in replacements:
                    preserved[name] = sha(data)
            dst.writestr('android/gallery-verification-context.json', json.dumps(context, ensure_ascii=False, indent=2) + '\n')
    assert len([x for x in preserved if x.endswith(('matrix.json', 'research-fault-matrix.json'))]) == 2
    assert len([x for x in preserved if '/upgrade-' in x and x.endswith('.json')]) == 6
    assert load(previous / 'ACCEPTANCE.json')['apkSha256'] == APK_SHA
    with zipfile.ZipFile(output / 'ANDROID_ACCEPTANCE_EVIDENCE.zip') as check:
        assert all(sha(check.read(name)) == value for name, value in preserved.items())
        assert len([x for x in check.namelist() if x.endswith('.png')]) == 188
        assert json.loads(check.read('host/unit-summary.json'))['tests'] == 344
    acceptance = load(previous / 'ACCEPTANCE.json')
    acceptance['galleryReverification'] = context
    acceptance['evidenceArchive']['sha256'] = sha((output / 'ANDROID_ACCEPTANCE_EVIDENCE.zip').read_bytes())
    acceptance['evidenceArchive']['actualGalleryPngs'] = 188
    write(output / 'ACCEPTANCE.json', acceptance)
    (output / 'SHA256SUMS.txt').write_text(APK_SHA + '  ' + APK + '\n' + acceptance['evidenceArchive']['sha256'] + '  ANDROID_ACCEPTANCE_EVIDENCE.zip\n')
    print('R30_GALLERY|188_FRESH_REAL_IMAGES_VISIBLE_TARGETS_FRAME_SYNC_DISTINCT_R01_R02_R03|PASS')
    print('R30_GALLERY|APK_UNCHANGED_ORIGINAL26_FAULTS_6_UPGRADE_RECEIPTS_344_UNIT_PRESERVED|PASS')
    print(json.dumps({'newEvidenceSha256': acceptance['evidenceArchive']['sha256'], 'appSource': APP,
                      'testSource': context['instrumentationSourceCommit'], 'apkSha256': APK_SHA}))

parser = argparse.ArgumentParser(__doc__)
parser.add_argument('mode', choices=['prepare', 'verify-apk', 'merge'])
parser.add_argument('--app', default=APP)
parser.add_argument('--tests')
parser.add_argument('--context', default='gallery-verification-context.json')
parser.add_argument('--previous', default='previous')
parser.add_argument('--fresh', default='evidence/gallery/android-regression')
parser.add_argument('--output', default='gallery-corrected')
args = parser.parse_args()
{'prepare': prepare, 'verify-apk': verify_apk, 'merge': merge}[args.mode](args)
