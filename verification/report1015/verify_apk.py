"""Inspect the signed release artifact without treating host checks as device proof."""
from pathlib import Path
import argparse, hashlib, json, os, re, struct, subprocess
import xml.etree.ElementTree as ET
import zipfile

R = Path(__file__).resolve().parents[2]
W = R.parent
SDK = W / 'offline_tools/toolchain/android-sdk'
BT = SDK / 'build-tools/36.0.0'
env = os.environ.copy()
env['JAVA_HOME'] = str(W / 'offline_tools/toolchain/zulu17.62.17-ca-jdk17.0.17-linux_x64')


def sha(data):
    return hashlib.sha256(data).hexdigest()


def command(args):
    return subprocess.check_output(list(map(str, args)), env=env, stderr=subprocess.STDOUT, text=True)


def signature(path):
    output = command([BT / 'apksigner', 'verify', '--verbose', '--print-certs', path])
    cert = re.findall(r'certificate SHA-256 digest: ([0-9a-f]{64})', output)
    assert len(cert) == 1, 'APK_SIGNER_COUNT'
    assert 'Verified using v1 scheme (JAR signing): true' in output, 'APK_V1_SIGNATURE'
    assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in output, 'APK_V2_SIGNATURE'
    return cert[0]


def dex_index(data):
    assert data[:4] == b'dex\n', 'DEX_MAGIC'
    assert hashlib.sha1(data[32:]).digest() == data[12:32], 'DEX_SHA1'
    import zlib
    assert zlib.adler32(data[12:]) & 0xffffffff == struct.unpack_from('<I', data, 8)[0], 'DEX_CHECKSUM'
    def u32(at):
        return struct.unpack_from('<I', data, at)[0]
    strings = []
    for i in range(u32(56)):
        at = u32(u32(60) + i * 4)
        while data[at] & 0x80:
            at += 1
        at += 1
        end = data.index(b'\0', at)
        strings.append(data[at:end].decode('utf-8', errors='replace'))
    types = [strings[u32(u32(68) + i * 4)] for i in range(u32(64))]
    classes = {types[u32(u32(100) + i * 32)] for i in range(u32(96))}
    return classes, set(strings)


def inspect(apk, receipt_path):
    release = json.loads((R / 'release/release_manifest.json').read_text())
    receipt = json.loads(receipt_path.read_text())
    baseline = W / 'build_inputs/STARCORE-R77-20261013.apk'
    certificate = signature(apk)
    assert certificate == signature(baseline), 'APK_ORIGINAL_SIGNER_MISMATCH'
    badging = command([BT / 'aapt', 'dump', 'badging', apk])
    assert "name='" + release['applicationId'] + "'" in badging, 'APK_APPLICATION_ID'
    for key in ['versionName', 'versionCode']:
        assert key + "='" + str(release[key]) + "'" in badging, 'APK_' + key
    command([BT / 'zipalign', '-c', '-P', '16', '4', apk])
    main = R / 'app/build/intermediates/javac'
    debug, product = main / 'debug/compileDebugJavaWithJavac/classes', main / 'release/compileReleaseJavaWithJavac/classes'
    classes = {str(p.relative_to(product)): sha(p.read_bytes()) for p in product.rglob('*.class')}
    assert len(classes) > 3000, 'RELEASE_BYTECODE_MISSING'
    differing = [name for name, digest in classes.items() if not (debug / name).is_file() or sha((debug / name).read_bytes()) != digest]
    assert differing == ['com/ethk3/novaonepro/BuildConfig.class'], 'UNTESTED_RELEASE_BYTECODE'
    native = {}
    with zipfile.ZipFile(apk) as z:
        assert z.testzip() is None, 'APK_ZIP_CRC'
        identity = json.loads(z.read('assets/diagnostics/construction-current-IDENTITY.json'))
        assert identity['sourceCommit'] == receipt['commit'] and identity['sourceTree'] == receipt['tree'], 'APK_SOURCE_RECEIPT'
        canonical = json.dumps(receipt['files'], sort_keys=True, separators=(',', ':')).encode()
        assert identity['buildInputsSha256'] == sha(canonical), 'APK_SOURCE_INPUT_SET'
        for name, digest in receipt['files'].items():
            assert sha((R / name).read_bytes()) == digest, 'POST_BUILD_SOURCE_DRIFT:' + name
        provenance = json.loads(z.read('assets/diagnostics/source-provenance.json'))
        index = json.loads((R / 'release/r982_source_calls.manifest.json').read_text())
        calls = z.read('assets/research/r975_source_calls.tsv')
        assert len(calls) == index['bytes'] and sha(calls) == index['sha256'], 'APK_SOURCE_CALL_INDEX'
        dex_classes, dex_strings = set(), set()
        dex_files = [n for n in z.namelist() if re.fullmatch(r'classes(?:[0-9]+)?\.dex', n)]
        for name in dex_files:
            types, strings = dex_index(z.read(name))
            assert not (types & dex_classes), 'APK_DUPLICATE_CLASS'
            dex_classes.update(types)
            dex_strings.update(strings)
        missing = ['L' + name[:-6] + ';' for name in classes if 'L' + name[:-6] + ';' not in dex_classes]
        assert not missing, 'APK_MISSING_RELEASE_CLASSES:' + str(missing[:8])
        assert provenance['sourceTreeSha256'] in dex_strings, 'APK_RUNTIME_SOURCE_IDENTITY'
        semantic = R / 'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java'
        rule = re.search(r'TRAINING_RULE_SHA256\s*=\s*runtimeValue\("([0-9a-f]{64})"\)', semantic.read_text())
        assert rule and rule[1] in dex_strings, 'APK_RUNTIME_RULE_IDENTITY'
        expected_native = {'arm64-v8a': 183, 'armeabi-v7a': 40, 'x86_64': 62}
        entries = [n for n in z.namelist() if n.startswith('lib/')]
        assert set(entries) == {'lib/' + abi + '/libstarcore_report_cp4.so' for abi in expected_native}, 'APK_NATIVE_SET'
        for abi, machine in expected_native.items():
            blob = z.read('lib/' + abi + '/libstarcore_report_cp4.so')
            assert blob[:4] == b'\x7fELF' and struct.unpack_from('<H', blob, 18)[0] == machine, 'APK_NATIVE_ARCH'
            native[abi] = {'sha256': sha(blob), 'bytes': len(blob), 'elfMachine': machine}
    unit = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    for p in (R / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
        root = ET.parse(p).getroot()
        for key in unit:
            unit[key] += int(root.get(key, '0'))
    assert unit['tests'] >= 54 and unit['failures'] == unit['errors'] == 0, 'APK_UNIT_REGRESSION'
    lint = (R / 'app/build/reports/lint-results-release.txt').read_text()
    assert 'No errors or warnings (and 55 errors filtered by baseline' in lint, 'APK_LINT_RESULT'
    build = (W / 'audit_work/gradle-r78-release.log').read_text()
    assert 'BUILD SUCCESSFUL' in build, 'APK_NORMAL_BUILD_NOT_COMPLETE'
    return {'scope': 'SIGNED_APK_STATIC_AND_HOST_VERIFIED_NOT_DEVICE_OR_NATURAL_FUTURE',
            'apk': {'filename': apk.name, 'bytes': apk.stat().st_size, 'sha256': sha(apk.read_bytes()),
                    'applicationId': release['applicationId'], 'versionName': release['versionName'], 'versionCode': release['versionCode']},
            'signature': {'certificateSha256': certificate, 'sameAsR77': True, 'v1': True, 'v2': True},
            'zipIntegrity': 'PASS', 'zipAlignment16KiB': 'PASS', 'native': native,
            'sourceCommit': receipt['commit'], 'sourceTree': receipt['tree'], 'verifiedSourceInputs': len(receipt['files']),
            'javaSourceIdentity': provenance['sourceTreeSha256'], 'ruleIdentity': rule[1],
            'sourceCalls': {'sha256': index['sha256'], 'bytes': index['bytes'], 'scope': index['scope']},
            'releaseClasses': len(classes), 'releaseClassesInDex': len(classes), 'dexFiles': len(dex_files),
            'releaseVsTestedBytecodeDifferences': differing, 'unitTests': unit,
            'normalGradleBuild': 'PASS', 'lint': {'newIssues': 0, 'inheritedBaselineFindingsFiltered': 55, 'zeroTotalIssuesClaimed': False},
            'deviceRuntime': 'NOT_VERIFIED', 'naturalFutureQualification': 'NOT_VERIFIED', 'all22WorkItemsClosed': False}


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('apk', type=Path)
    parser.add_argument('receipt', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    result = inspect(args.apk, args.receipt)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))
