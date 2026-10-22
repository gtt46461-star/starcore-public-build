"""Verify the built APK against current inputs, including its actual DEX constants."""
import gzip, hashlib, json, os, re, shutil, struct, subprocess, tempfile, zipfile
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
TOOLS = Path(os.environ['STARCORE_TOOLCHAIN_ROOT'])
BUILD_TOOLS = TOOLS / 'sdk/build-tools/36.0.0'
EVIDENCE = Path(os.environ['STARCORE_TEST_EVIDENCE_DIR']).parent
EXPECTED_CERT = 'ae7e1b4a5a71f1f299d4a0b2472c6a36513873514e78190d42d6cfba3340116f'
def sha(data): return hashlib.sha256(data).hexdigest()
def execute(command):
    print('COMMAND ' + json.dumps(list(map(str, command))), flush=True)
    result = subprocess.run(list(map(str, command)), cwd=ROOT, capture_output=True, text=True)
    print(result.stdout, end=''); print(result.stderr, end='')
    if result.returncode: raise RuntimeError('APK_TOOL_FAILURE:' + str(result.returncode))
    return result.stdout
def constants(data):
    def u32(offset): return struct.unpack_from('<I', data, offset)[0]
    def uleb(offset):
        value = shift = 0
        while True:
            byte = data[offset]; offset += 1; value |= (byte & 127) << shift
            if not byte & 128: return value, offset
            shift += 7
            if shift > 35: raise ValueError('DEX_ULEB_OVERFLOW')
    def string(index):
        offset = u32(u32(60) + 4 * index)
        _, offset = uleb(offset)
        return data[offset:data.index(b'\0', offset)].decode('utf-8')
    def type_name(index): return string(u32(u32(68) + 4 * index))
    for i in range(u32(96)):
        at = u32(100) + i * 32
        if type_name(u32(at)) != 'Lcom/ethk3/novaonepro/ResearchSemanticsGeneratedR967;': continue
        off = u32(at + 24); count, off = uleb(off)
        for _ in range(3): _, off = uleb(off)
        fields = []; index = 0
        for _ in range(count):
            delta, off = uleb(off); index += delta; _, off = uleb(off)
            fields.append(string(u32(u32(84) + index * 8 + 4)))
        off = u32(at + 28); values, off = uleb(off)
        if values != count: raise ValueError('DEX_STATIC_VALUE_COUNT')
        result = {}
        for name in fields:
            tag = data[off]; off += 1
            if tag & 31 != 0x17: raise ValueError('DEX_IDENTITY_FIELD_TYPE')
            width = (tag >> 5) + 1
            value = int.from_bytes(data[off:off + width], 'little'); off += width
            result[name] = string(value)
        return result
    return None
manifest = json.loads((ROOT / 'release/release_manifest.json').read_text())
apks = list((ROOT / 'app/build/outputs/apk/release').glob('*.apk'))
if len(apks) != 1: raise RuntimeError('APK_COUNT')
apk = apks[0]
badging = execute([BUILD_TOOLS / 'aapt', 'dump', 'badging', apk])
package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
if not package or package.groups() != (manifest['applicationId'], str(manifest['versionCode']), manifest['versionName']):
    raise RuntimeError('APK_PACKAGE_VERSION_DRIFT')
abis = re.search(r'native-code: (.+)', badging)
actual_abis = sorted(re.findall(r"'([^']+)'", abis.group(1)))
if actual_abis != ['arm64-v8a', 'armeabi-v7a', 'x86_64']: raise RuntimeError('APK_ABI_DRIFT')
signing = execute([BUILD_TOOLS / 'apksigner', 'verify', '--verbose', '--print-certs', apk])
if 'certificate SHA-256 digest: ' + EXPECTED_CERT not in signing: raise RuntimeError('APK_ORIGINAL_CERT_MISMATCH')
execute([BUILD_TOOLS / 'zipalign', '-c', '4', apk])
java = (ROOT / 'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java').read_text()
expected_constants = {name: json.loads(literal) for name, literal in re.findall(r'public static final String (\w+)=("(?:[^"\\]|\\.)*");', java)}
assets = []; dex_results = []
release_classes = ROOT / 'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes'
consumer_classpath = ':'.join(map(str, [release_classes, TOOLS / 'sdk/platforms/android-36/android.jar']))
asset_constants = execute([TOOLS / 'jdk17/bin/javap', '-classpath', release_classes,
                           '-constants', 'com.ethk3.novaonepro.HistoricalDrawImportEngine'])
asset_path_match = re.search(r'SEED_ASSET_PATH = "([^"]+)"', asset_constants)
if not asset_path_match: raise RuntimeError('APK_HISTORY_ASSET_CONSUMER_IDENTITY_MISSING')
seed_asset = 'assets/' + asset_path_match.group(1)
with zipfile.ZipFile(apk) as z:
    if z.testzip() is not None: raise RuntimeError('APK_ZIP_CRC_FAILURE')
    for source in [ROOT / 'app/src/main/assets', *sorted((ROOT / 'app/build/generated/assets').iterdir())]:
        for file in sorted(source.rglob('*')):
            if file.is_file():
                name = 'assets/' + file.relative_to(source).as_posix()
                want = file.read_bytes()
                transform = 'IDENTITY'
                if name not in z.namelist() and name.endswith('.gz'):
                    name = name[:-3]
                    want = gzip.decompress(want)
                    transform = 'AGP_MERGED_ASSET_WRITER_GZIP_EXPANSION'
                got = z.read(name)
                if got != want: raise RuntimeError('APK_ASSET_BYTE_DRIFT:' + name)
                assets.append({'path': name, 'bytes': len(got), 'sha256': sha(got),
                               'source':str(file.relative_to(ROOT)), 'packaging':transform,
                               'source_sha256':sha(file.read_bytes())})
    for name in z.namelist():
        if re.fullmatch(r'classes\d*\.dex', name):
            data = z.read(name); value = constants(data)
            dex_results.append({'path': name, 'sha256': sha(data), 'bytes': len(data)})
            if value is not None:
                if value != expected_constants: raise RuntimeError('APK_DEX_SEMANTIC_IDENTITY_DRIFT')
                semantic_dex = name
    if 'semantic_dex' not in locals(): raise RuntimeError('APK_GENERATED_CLASS_MISSING')
    construction = json.loads(z.read('assets/diagnostics/construction-current-IDENTITY.json'))
    provenance = json.loads(z.read('assets/diagnostics/source-provenance.json'))
    with tempfile.TemporaryDirectory(prefix='starcore-apk-seed-') as temporary:
        directory = Path(temporary)
        (directory / 'seed.csv').write_bytes(z.read(seed_asset))
        execute([TOOLS / 'jdk17/bin/javac', '-cp', consumer_classpath, '-d', directory,
                 ROOT / 'verification/batch01/HistoricalSeedConsumerProbe.java'])
        seed_result = execute([TOOLS / 'jdk17/bin/java', '-cp', str(directory) + ':' + consumer_classpath,
                               'com.ethk3.novaonepro.HistoricalSeedConsumerProbe', directory / 'seed.csv'])
commit = subprocess.check_output(['git','rev-parse','HEAD'], cwd=ROOT, text=True).strip()
tree = subprocess.check_output(['git','rev-parse','HEAD^{tree}'], cwd=ROOT, text=True).strip()
if (construction['sourceCommit'], construction['sourceTree']) != (commit, tree): raise RuntimeError('APK_COMMIT_TREE_DRIFT')
destination = ROOT.parent / 'deliverables' / ('STARCORE_B01_checkpoint_' + commit[:7] + '_original-signature.apk')
destination.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(apk, destination)
receipt = {'status':'PASS', 'source_commit':commit,'source_tree':tree, 'apk_path':str(destination),
           'apk_bytes':destination.stat().st_size,'apk_sha256':sha(destination.read_bytes()),
           'package':manifest['applicationId'],'version_code':manifest['versionCode'],'version_name':manifest['versionName'],
           'abis':actual_abis,'original_certificate_sha256':EXPECTED_CERT,'dex_semantic_class':semantic_dex,
           'dex_constants_equal_current_generator':True,'dex':dex_results,'assets':assets,
           'actual_packaged_history_consumer':seed_result.strip(),
           'provenance':provenance,'construction_identity':construction,
           'cover_install_conditions':'same package and original certificate; installed versionCode must be <=' + str(manifest['versionCode']) + '; actual installed identity must be checked on device',
           'actual_installation':'NOT_RUN_NO_DEVICE','batch_acceptance':'NOT_CLOSED'}
(EVIDENCE / 'CURRENT_APK_RECEIPT.json').write_text(json.dumps(receipt, indent=2) + '\n')
print(json.dumps({k:v for k,v in receipt.items() if k not in ['provenance','construction_identity','assets','dex']}, indent=2))
