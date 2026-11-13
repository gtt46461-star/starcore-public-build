#!/usr/bin/env python3
"""R31-B12-C05#A1a/#A2/#A3 self-test of tools/r31/package_release.py on a constructed build-output layout.

The fixture mimics exactly what r31-verification.yml android_build uploads (app/build/outputs/apk/<variant>/*.apk,
r31v/SHA256SUMS.txt, badging-*.txt, signature-*.txt, signing.txt, source-commit.txt). It is NOT an APK built here and
the test never claims one; the real ZIP is produced in CI from the real build output.
"""
import json, shutil, sys, tempfile, unittest, zipfile, hashlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import package_release as pr

ROOT = Path(__file__).resolve().parents[2]
IDENT = pr.identity()
COMMIT = 'a' * 40
CERT = 'b' * 64


def apk_bytes(tag=b'x'):
    buf = __import__('io').BytesIO()
    with zipfile.ZipFile(buf, 'w') as z:
        z.writestr('AndroidManifest.xml', b'\x03\x00\x08\x00' + tag); z.writestr('classes.dex', b'dex\n035\x00' + tag)
    return buf.getvalue()


def layout(base, signing='NOT_AVAILABLE: transport key not reachable', variant='debug', version=None, verified=True, extra_apk=False):
    built = Path(base) / 'built'; apkdir = built / 'app/build/outputs/apk' / variant; apkdir.mkdir(parents=True); r = built / 'r31v'; r.mkdir()
    data = apk_bytes(); name = 'app-%s' % variant
    (apkdir / (name + '.apk')).write_bytes(data)
    if extra_apk: (apkdir / 'app-debug-copy.apk').write_bytes(apk_bytes(b'y'))
    sums = '%s  app/build/outputs/apk/%s/%s.apk\n' % (hashlib.sha256(data).hexdigest(), variant, name)
    if extra_apk: sums += '%s  app/build/outputs/apk/debug/app-debug-copy.apk\n' % hashlib.sha256(apk_bytes(b'y')).hexdigest()
    (r / 'SHA256SUMS.txt').write_text(sums)
    (r / ('badging-%s.txt' % name)).write_text("package: name='%s' versionCode='%s' versionName='%s' compileSdkVersion='36'\n" % (IDENT['applicationId'], version or IDENT['versionCode'], IDENT['versionName']))
    (r / ('signature-%s.txt' % name)).write_text(('Verifies\n' if verified else 'DOES NOT VERIFY\n') + 'Verified using v1 scheme (JAR signing): true\nVerified using v2 scheme (APK Signature Scheme v2): true\nSigner #1 certificate SHA-256 digest: %s\n' % CERT)
    (r / 'signing.txt').write_text(signing + '\n'); (r / 'source-commit.txt').write_text(COMMIT + '\n')
    return built


def receipt(base, instrumentation='NOT_RUN'):
    p = Path(base) / 'verification.json'
    p.write_text(json.dumps({'format': 'R31_VERIFICATION_RECEIPT_V1', 'overall': 'NOT_PASS', 'stages': {
        'baseline': {'status': 'PASS'}, 'contracts': {'status': 'PASS'}, 'android_build': {'status': 'PASS'}, 'instrumentation': {'status': instrumentation}}}))
    return p


class Package(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())

    def test_sdk_range_signer_label_and_conflicting_signers(self):
        built = layout(self.tmp)
        signature = next((built / 'r31v').glob('signature-*.txt'))
        original = signature.read_text()
        for label in ['Signer:', 'Signer (minSdkVersion=21, maxSdkVersion=2147483647):', 'Signer #1']:
            signature.write_text(original.replace('Signer #1', label))
            apk = built / 'app/build/outputs/apk/debug/app-debug.apk'
            result = pr.verify_apk(apk, built / 'r31v/badging-app-debug.txt', signature, built / 'r31v/SHA256SUMS.txt', IDENT)
            self.assertEqual(result['signerCertSha256'], CERT)
        signature.write_text(original + 'Signer #2 certificate SHA-256 digest: ' + 'c' * 64 + '\n')
        with self.assertRaisesRegex(pr.Refused, 'R31_APK_SIGNER_CERT_UNKNOWN'):
            pr.verify_apk(apk, built / 'r31v/badging-app-debug.txt', signature, built / 'r31v/SHA256SUMS.txt', IDENT)

    def test_zip_integrity_member_digests_and_apk_signature_agree_with_the_receipt(self):
        z = pr.create_delivery(layout(self.tmp), receipt(self.tmp), self.tmp / 'out', IDENT)
        d = pr.verify_zip(z, IDENT)
        with zipfile.ZipFile(z) as f:
            apk = f.read(d['apk']['deliveredAs'])
        self.assertEqual(d['apk']['sha256'], hashlib.sha256(apk).hexdigest()); self.assertEqual(d['apk']['signerCertSha256'], CERT)
        self.assertEqual(d['apk']['signatureSchemes'], ['v1', 'v2']); self.assertEqual(d['sourceCommit'], COMMIT)
        # a changed member, an unverified signature, other bytes under the build's name, or another version are refused
        bad = self.tmp / 'bad.zip'
        with zipfile.ZipFile(z) as src, zipfile.ZipFile(bad, 'w') as dst:
            for n in src.namelist(): dst.writestr(n, src.read(n) + (b' ' if n == 'README.txt' else b''))
        with self.assertRaises(pr.Refused) as e: pr.verify_zip(bad, IDENT)
        self.assertEqual(str(e.exception), 'R31_ZIP_MEMBER_DIGEST:README.txt')
        for kwargs, code in (({'verified': False}, 'R31_APK_SIGNATURE_NOT_VERIFIED'), ({'version': '1'}, 'R31_APK_VERSION_CODE_MISMATCH:1')):
            t = Path(tempfile.mkdtemp())
            with self.assertRaises(pr.Refused) as e: pr.create_delivery(layout(t, **kwargs), receipt(t), t / 'out', IDENT)
            self.assertEqual(str(e.exception), code); self.assertFalse(list((t / 'out').glob('*.zip')))
            self.assertIn(code, (t / 'out/REFUSED.json').read_text())
        t = Path(tempfile.mkdtemp()); built = layout(t); (built / 'app/build/outputs/apk/debug/app-debug.apk').write_bytes(apk_bytes(b'old'))
        with self.assertRaises(pr.Refused) as e: pr.create_delivery(built, receipt(t), t / 'out', IDENT)
        self.assertEqual(str(e.exception), 'R31_APK_BYTES_DIFFER_FROM_BUILD_OUTPUT')

    def test_the_download_is_one_openable_uniquely_named_file(self):
        z = pr.create_delivery(layout(self.tmp), receipt(self.tmp), self.tmp / 'out', IDENT)
        self.assertTrue(z.is_file()); self.assertEqual(list((self.tmp / 'out').glob('*.zip')), [z])
        self.assertIn(COMMIT[:12], z.name); self.assertIn('DEBUG-NOT-OFFICIAL', z.name); self.assertIn(IDENT['versionName'], z.name)
        x = self.tmp / 'x'; zipfile.ZipFile(z).extractall(x)
        self.assertEqual(sorted(p.name for p in x.iterdir()), sorted([z.stem + '.apk', 'R31_DELIVERY.json', 'R31_VERIFICATION_RECEIPT.json', 'README.txt', 'SHA256SUMS.txt']))
        self.assertTrue(zipfile.is_zipfile(x / (z.stem + '.apk')))
        t = Path(tempfile.mkdtemp())
        with self.assertRaises(pr.Refused) as e: pr.create_delivery(layout(t, extra_apk=True), receipt(t), t / 'out', IDENT)
        self.assertEqual(str(e.exception), 'R31_DELIVERY_EXPECTED_ONE_DEBUG_APK:2')
        t = Path(tempfile.mkdtemp())
        with self.assertRaises(pr.Refused) as e: pr.create_delivery(layout(t), t / 'missing.json', t / 'out', IDENT)
        self.assertEqual(str(e.exception), 'R31_DELIVERY_NO_VERIFICATION_RECEIPT')

    def test_engineering_pass_and_natural_advantage_are_separate_columns(self):
        z = pr.create_delivery(layout(self.tmp), receipt(self.tmp), self.tmp / 'out', IDENT)
        d = pr.verify_zip(z, IDENT); L = d['layers']
        self.assertEqual(L['L2_ENGINEERING_HOST_AND_UNIT']['status'], 'PASS')
        self.assertEqual(L['L3_ANDROID_EMULATOR']['status'], 'NOT_RUN'); self.assertEqual(L['L4_PHYSICAL_DEVICE_24H_72H']['status'], 'NOT_RUN')
        self.assertEqual(L['NATURAL_QUALITY']['status'], 'NOT_PROVEN'); self.assertTrue(d['signing'].startswith('NOT_OFFICIAL'))
        t = Path(tempfile.mkdtemp()); z2 = pr.create_delivery(layout(t), receipt(t, 'PASS'), t / 'out', IDENT)
        self.assertEqual(pr.verify_zip(z2, IDENT)['layers']['L3_ANDROID_EMULATOR']['status'], 'PASS_EMULATOR_ONLY')
        forged = self.tmp / 'forged.zip'
        with zipfile.ZipFile(z) as src:
            members = {n: src.read(n) for n in src.namelist()}
        dl = json.loads(members['R31_DELIVERY.json']); dl['layers']['NATURAL_QUALITY']['status'] = 'PASS'; members['R31_DELIVERY.json'] = json.dumps(dl).encode()
        members['SHA256SUMS.txt'] = ''.join('%s  %s\n' % (hashlib.sha256(v).hexdigest(), k) for k, v in sorted(members.items()) if k != 'SHA256SUMS.txt').encode()
        with zipfile.ZipFile(forged, 'w') as f:
            for n, v in members.items(): f.writestr(n, v)
        shutil.copy(forged, self.tmp / dl['deliverable'])
        with self.assertRaises(pr.Refused) as e: pr.verify_zip(self.tmp / dl['deliverable'], IDENT)
        self.assertEqual(str(e.exception), 'R31_ZIP_NATURAL_QUALITY_CLAIMED')

    def test_the_installed_device_identity_gives_a_release_overlay_install_never_called_official(self):
        dev = pr.INSTALLED['signerCertSha256']
        t = Path(tempfile.mkdtemp()); built = layout(t, signing='EXISTING_DEVICE_IDENTITY: R27 CI key %s' % dev, variant='release')
        for f in (built / 'r31v').glob('signature-*.txt'): f.write_text(f.read_text().replace(CERT, dev))
        d = pr.verify_zip(pr.create_delivery(built, receipt(t), t / 'out', IDENT), IDENT)
        self.assertEqual(d['apk']['variant'], 'release'); self.assertIn('OVERLAY-INSTALL-R27-IDENTITY', d['deliverable']); self.assertNotIn('OFFICIAL', d['deliverable'])
        self.assertTrue(d['overlayInstall']['overlayInstallKeepsData']); self.assertTrue(d['signing'].startswith('EXISTING_DEVICE_IDENTITY'))
        t = Path(tempfile.mkdtemp()); built = layout(t, signing='EXISTING_DEVICE_IDENTITY: changed key', variant='release')
        with self.assertRaises(pr.Refused) as e: pr.create_delivery(built, receipt(t), t / 'out', IDENT)
        self.assertEqual(str(e.exception), 'R31_DELIVERY_DEVICE_IDENTITY_NOT_OVERLAY_COMPATIBLE')

    def test_the_committed_delivery_declaration_keeps_the_columns_apart(self):
        d = json.loads((ROOT / 'release/R31_DELIVERY.json').read_text())
        self.assertEqual(d['layers']['NATURAL_QUALITY']['status'], 'NOT_PROVEN')
        self.assertEqual(d['layers']['L4_PHYSICAL_DEVICE_24H_72H']['status'], 'NOT_RUN')
        self.assertNotIn(d['signing'], ('OFFICIAL_R13',)) if not d['apk'].get('signerCertSha256') else None
        for k, v in d['layers'].items():
            self.assertTrue(v['status'] in ('PASS', 'PASS_EMULATOR_ONLY', 'NOT_RUN', 'NOT_PROVEN', 'FAIL', 'PENDING_CI') or v['status'].startswith('PASS_WITH_SKIPPED'), k)
        self.assertTrue(d['deliverable'].endswith('.zip') or d['deliverable'].startswith('PENDING_CI'))


if __name__ == '__main__':
    unittest.main(verbosity=2)
