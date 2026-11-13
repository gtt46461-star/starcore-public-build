#!/usr/bin/env python3
"""R31-B12-C05: the single delivery ZIP, built only from a real CI build output.

  verify_apk(apk, badging, signature, sums, identity)  the APK bytes equal the build stage's SHA256SUMS line, the aapt
      badging names the release identity (package/versionCode/versionName), and apksigner verified it; the signer
      certificate SHA-256 is taken from apksigner's output, never assumed. A renamed or older APK fails here.
  create_delivery(built, verification, out)  one APK (release when officially signed, otherwise debug labelled
      NOT_OFFICIAL), R31_DELIVERY.json with separate L2/L3/L4 and natural-quality columns, SHA256SUMS and the stage
      receipts, zipped as one uniquely named file. Missing evidence -> REFUSED.json, no ZIP.
  verify_zip(zip)  reopens the ZIP, tests every member CRC, recomputes every member digest against the manifest and
      re-verifies the APK identity recorded inside.
The delivery never lists a planned test as passed: layers that did not run are NOT_RUN; natural quality is NOT_PROVEN.
"""
import argparse, hashlib, io, json, re, sys, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
IDENTITY = ROOT / 'release/release_identity.properties'
# The phone's installed build (device report of the R31 plan: R30-REPAIR1) and its signer, the R27 CI key.
INSTALLED = {'package': 'com.starcore.research.r13', 'versionCode': 577311020, 'versionName': 'R31-CHECKPOINT-P01-2026.11.12-020',
             'signerCertSha256': 'd8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7'}


class Refused(Exception):
    pass


def need(ok, code):
    if not ok:
        raise Refused(code)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def identity(path=IDENTITY):
    out = {}
    for line in Path(path).read_text().splitlines():
        if '=' in line and not line.startswith('#'):
            k, v = line.split('=', 1); out[k.strip()] = v.strip()
    return out


def verify_apk(apk, badging, signature, sums, ident):
    """Returns the APK identity facts or raises Refused with the exact reason."""
    data = Path(apk).read_bytes(); digest = sha256(data)
    need(data[:2] == b'PK', 'R31_APK_NOT_A_ZIP')
    listed = {line.split()[1].split('/')[-1]: line.split()[0] for line in Path(sums).read_text().splitlines() if len(line.split()) == 2}
    need(Path(apk).name in listed, 'R31_APK_NOT_IN_BUILD_SHA256SUMS:' + Path(apk).name)
    need(listed[Path(apk).name] == digest, 'R31_APK_BYTES_DIFFER_FROM_BUILD_OUTPUT')
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        need(z.testzip() is None, 'R31_APK_CRC_FAILURE')
        need('AndroidManifest.xml' in z.namelist() and 'classes.dex' in z.namelist(), 'R31_APK_MISSING_MANIFEST_OR_DEX')
    b = Path(badging).read_text()
    m = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'", b)
    need(m is not None, 'R31_APK_BADGING_UNREADABLE')
    need(m.group(1) == ident['applicationId'], 'R31_APK_PACKAGE_MISMATCH:' + m.group(1))
    need(m.group(2) == ident['versionCode'], 'R31_APK_VERSION_CODE_MISMATCH:' + m.group(2))
    need(m.group(3) == ident['versionName'], 'R31_APK_VERSION_NAME_MISMATCH:' + m.group(3))
    s = Path(signature).read_text()
    need(re.search(r'^Verifies\s*$', s, re.M) is not None, 'R31_APK_SIGNATURE_NOT_VERIFIED')
    # New build-tools include the signer's SDK range and a colon in this label.
    # Parse only complete verified-signer lines, and reject different signers.
    certs = sorted(set(v.lower() for v in re.findall(
        r'^(?:V[0-9]+(?:\.[0-9]+)? )?Signer(?: #[0-9]+)?(?: \([^\r\n]*\))?:? certificate SHA-256 digest: ([0-9a-fA-F]{64})\s*$', s, re.M)))
    need(len(certs) == 1, 'R31_APK_SIGNER_CERT_UNKNOWN')
    schemes = sorted(set(re.findall(r'Verified using (v[0-9.]+) scheme[^:]*: true', s)))
    return {'file': Path(apk).name, 'sha256': digest, 'bytes': len(data), 'package': m.group(1), 'versionCode': int(m.group(2)),
            'versionName': m.group(3), 'signerCertSha256': certs[0], 'signatureSchemes': schemes}


def _layers(verification):
    stages = verification.get('stages', {})
    def st(name): return stages.get(name, {}).get('status', 'NOT_RUN')
    return {
        'L2_ENGINEERING_HOST_AND_UNIT': {'status': 'PASS' if st('contracts') == 'PASS' and st('baseline') == 'PASS' else st('contracts'),
                                         'evidence': 'r31-verification contracts + baseline stages; construction gate RECEIPT K01..K11'},
        'L3_ANDROID_EMULATOR': {'status': st('instrumentation') if st('instrumentation') != 'PASS' else 'PASS_EMULATOR_ONLY',
                                'evidence': 'instrumentation stage (API 29/35 disposable emulators); not a device'},
        'L4_PHYSICAL_DEVICE_24H_72H': {'status': 'NOT_RUN', 'evidence': 'R31-B12-C03 and R31-B12-C05#A1b are DEVICE items; tools/r31/collect_device_receipts.py + accept_device_receipts.py'},
        'NATURAL_QUALITY': {'status': 'NOT_PROVEN', 'evidence': 'engineering passes never imply a natural advantage; no certificate of natural superiority exists'},
    }


def create_delivery(built, verification_path, out, ident=None, acceptance_dir=None):
    ident = ident or identity()
    built, out = Path(built), Path(out); out.mkdir(parents=True, exist_ok=True)
    try:
        need(Path(verification_path).is_file(), 'R31_DELIVERY_NO_VERIFICATION_RECEIPT')
        verification = json.loads(Path(verification_path).read_text())
        need(verification.get('format') == 'R31_VERIFICATION_RECEIPT_V1', 'R31_DELIVERY_VERIFICATION_FORMAT')
        r31v = built / 'r31v' if (built / 'r31v').is_dir() else built
        signing = (r31v / 'signing.txt').read_text().strip() if (r31v / 'signing.txt').is_file() else 'UNKNOWN'
        commit = (r31v / 'source-commit.txt').read_text().strip() if (r31v / 'source-commit.txt').is_file() else ''
        need(re.fullmatch(r'[0-9a-f]{40}', commit) is not None, 'R31_DELIVERY_SOURCE_COMMIT_UNKNOWN')
        official = signing.startswith('OFFICIAL_R13'); device = signing.startswith('EXISTING_DEVICE_IDENTITY')
        variant = 'release' if (official or device) and list((built / 'app/build/outputs/apk/release').glob('*.apk')) else 'debug'
        apks = sorted((built / 'app/build/outputs/apk' / variant).glob('*.apk'))
        need(len(apks) == 1, 'R31_DELIVERY_EXPECTED_ONE_%s_APK:%d' % (variant.upper(), len(apks)))
        apk = apks[0]; stem = apk.stem
        facts = verify_apk(apk, r31v / ('badging-%s.txt' % stem), r31v / ('signature-%s.txt' % stem), r31v / 'SHA256SUMS.txt', ident)
        label = 'OFFICIAL' if official else ('OVERLAY-INSTALL-R27-IDENTITY' if device else 'DEBUG-NOT-OFFICIAL')
        overlay = {'installed': INSTALLED, 'sameSigner': facts['signerCertSha256'] == INSTALLED['signerCertSha256'],
                   'samePackage': facts['package'] == INSTALLED['package'], 'versionCodeHigher': facts['versionCode'] > INSTALLED['versionCode']}
        overlay['overlayInstallKeepsData'] = overlay['sameSigner'] and overlay['samePackage'] and overlay['versionCodeHigher']
        need(not device or overlay['overlayInstallKeepsData'], 'R31_DELIVERY_DEVICE_IDENTITY_NOT_OVERLAY_COMPATIBLE')
        name = 'STARCORE-R31-%s-%s-%s' % (facts['versionName'], label, commit[:12])
        delivery = {
            'format': 'R31_DELIVERY_V1', 'deliverable': name + '.zip', 'sourceCommit': commit,
            'apk': dict(facts, deliveredAs=name + '.apk', variant=variant),
            'signing': 'OFFICIAL_R13' if official else (signing if device else 'NOT_OFFICIAL: ' + signing),
            'overlayInstall': overlay,
            'identity': {k: ident[k] for k in ('applicationId', 'versionName', 'versionCode', 'databaseName', 'releaseMode')},
            'layers': _layers(verification),
            'remainingLimits': ['natural predictive advantage NOT_PROVEN', '24h/72h device runs NOT_RUN', 'old-APK read of the upgraded database NOT_RUN (EMU/DEVICE)'],
            'verificationOverall': verification.get('overall'),
        }
        need(delivery['layers']['NATURAL_QUALITY']['status'] == 'NOT_PROVEN' and delivery['layers']['L4_PHYSICAL_DEVICE_24H_72H']['status'] == 'NOT_RUN', 'R31_DELIVERY_UNPROVEN_LAYER_CLAIMED')
        members = {name + '.apk': apk.read_bytes(), 'R31_DELIVERY.json': (json.dumps(delivery, indent=1, ensure_ascii=False) + '\n').encode(),
                   'R31_VERIFICATION_RECEIPT.json': Path(verification_path).read_bytes(),
                   'README.txt': ('安装 %s.apk（%s）。工程通过与自然质量分栏：自然优势 NOT_PROVEN；真机 24h/72h NOT_RUN。\n' % (name,
                       'official signing' if official else ('与手机已装 R31 577311020 同包名同签名（R27 CI 证书 d8d8cee7…），versionCode 更高，可直接覆盖安装并保留数据；不是正式 R13 签名' if overlay['overlayInstallKeepsData'] else 'debug key, not the official identity; cannot overlay the installed app'))).encode()}
        if acceptance_dir is not None:
            acceptance_dir=Path(acceptance_dir)
            need((acceptance_dir/'ACCEPTANCE_RESULTS.json').is_file() and (acceptance_dir/'ACCEPTANCE_RESULTS.csv').is_file(),'R31_DELIVERY_ITEMIZED_ACCEPTANCE_MISSING')
            itemized=json.loads((acceptance_dir/'ACCEPTANCE_RESULTS.json').read_text())
            need(itemized['identity']['source_head']==commit,'R31_DELIVERY_ITEMIZED_ACCEPTANCE_SOURCE_MISMATCH')
            need(len(itemized['items'])==222 and len(itemized['cards'])==58,'R31_DELIVERY_ITEMIZED_ACCEPTANCE_SET')
            for filename in ['ACCEPTANCE_RESULTS.json','ACCEPTANCE_RESULTS.csv']:members['acceptance/'+filename]=(acceptance_dir/filename).read_bytes()
            delivery['itemizedAcceptance']={'items':222,'cards':58,'bindingResults':itemized['itemCounts'],'wholeProductClosure':'NOT_INFERRED_FROM_ENGINEERING_TESTS'}
            members['R31_DELIVERY.json']=(json.dumps(delivery,indent=1,ensure_ascii=False)+'\n').encode()
        sums = ''.join('%s  %s\n' % (sha256(v), k) for k, v in sorted(members.items()))
        members['SHA256SUMS.txt'] = sums.encode()
        z = out / (name + '.zip')
        with zipfile.ZipFile(z, 'w', zipfile.ZIP_DEFLATED) as zf:
            for k in sorted(members): zf.writestr(k, members[k])
        verify_zip(z, ident)
        (out / 'R31_DELIVERY.json').write_text(json.dumps(dict(delivery, zipSha256=sha256(z.read_bytes()), zipBytes=z.stat().st_size), indent=1, ensure_ascii=False) + '\n')
        print('R31_DELIVERY_CREATED ' + z.name)
        return z
    except Refused as r:
        (out / 'REFUSED.json').write_text(json.dumps({'refused': str(r), 'zip': 'NOT_CREATED'}, indent=1) + '\n')
        print('R31_DELIVERY_REFUSED ' + str(r)); raise


def verify_zip(path, ident=None):
    ident = ident or identity()
    with zipfile.ZipFile(path) as z:
        need(z.testzip() is None, 'R31_ZIP_CRC_FAILURE')
        names = z.namelist()
        need(len(set(names)) == len(names), 'R31_ZIP_DUPLICATE_MEMBER')
        apks = [n for n in names if n.endswith('.apk')]
        need(len(apks) == 1, 'R31_ZIP_EXPECTED_ONE_APK:%d' % len(apks))
        sums = dict((line.split('  ', 1)[1], line.split('  ', 1)[0]) for line in z.read('SHA256SUMS.txt').decode().splitlines())
        need(set(sums) == set(names) - {'SHA256SUMS.txt'}, 'R31_ZIP_MANIFEST_MEMBERS_DIFFER')
        for n, d in sums.items(): need(sha256(z.read(n)) == d, 'R31_ZIP_MEMBER_DIGEST:' + n)
        delivery = json.loads(z.read('R31_DELIVERY.json'))
        apk = z.read(apks[0])
        need(delivery['apk']['sha256'] == sha256(apk) and delivery['apk']['deliveredAs'] == apks[0], 'R31_ZIP_APK_NOT_THE_RECORDED_BUILD')
        need(delivery['apk']['package'] == ident['applicationId'] and str(delivery['apk']['versionCode']) == ident['versionCode'], 'R31_ZIP_APK_IDENTITY')
        need(re.fullmatch(r'[0-9a-f]{64}', delivery['apk']['signerCertSha256'] or '') is not None, 'R31_ZIP_SIGNER_UNKNOWN')
        need(Path(path).name == delivery['deliverable'], 'R31_ZIP_NAME_NOT_THE_DELIVERABLE')
        need(delivery['layers']['NATURAL_QUALITY']['status'] == 'NOT_PROVEN', 'R31_ZIP_NATURAL_QUALITY_CLAIMED')
        need(not (delivery['signing'].startswith('NOT_OFFICIAL') and 'OFFICIAL-' in Path(path).name.replace('NOT-OFFICIAL', '')), 'R31_ZIP_NAME_CLAIMS_OFFICIAL')
    return delivery


def main():
    ap = argparse.ArgumentParser(); sub = ap.add_subparsers(dest='cmd', required=True)
    c = sub.add_parser('create'); c.add_argument('--built', required=True); c.add_argument('--verification', required=True); c.add_argument('--out', required=True);c.add_argument('--acceptance',default=None)
    v = sub.add_parser('verify'); v.add_argument('zip')
    a = ap.parse_args()
    try:
        if a.cmd == 'create': create_delivery(a.built, a.verification, a.out,acceptance_dir=a.acceptance)
        else: print(json.dumps(verify_zip(a.zip), ensure_ascii=False)[:2000])
    except Refused:
        sys.exit(1)


if __name__ == '__main__':
    main()
