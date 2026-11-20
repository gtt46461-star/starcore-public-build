import json
import sys
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

TOOLS=Path(sys.argv.pop(1)).resolve()
sys.path.insert(0,str(TOOLS/'B02/enabled_window_r335'))
import test_enabled_window as sample


class ReleaseCapture(unittest.TestCase):
    def capture(self, mode, changed=False):
        f=sample.EnabledCoverageTests()
        f.setUp()
        self.addCleanup(f.doCleanups)
        collector=sample.m
        apk=f.root/'test.apk'
        apk.write_bytes(b'apk-identity-fixture')
        digest=collector.trace.sha_file(apk)
        calendar=collector.trace.calendar.make_calendar(f.anchor,f.t0,100,f.t0+1000,sample.C,digest,
            collector.trace.sha_file(f.schedule),window_start_ms=f.t0+2000)
        sample.fixture.save(f.calendar,calendar)
        start=json.loads(f.start.read_bytes())
        prefs=(f.root/start['preferences']['path']).read_bytes()
        reads=[]
        def adb(serial,*argv):
            if argv[:3]==('shell','pm','path'):return b'package:/path/to/base.apk\n'
            if argv[:2]==('shell','sha256sum'):return (digest+'  /path/to/base.apk\n').encode()
            if argv==('get-serialno',):return b'test-serial\n'
            expected=(('exec-out','content','read','--uri','content://'+collector.PACKAGE+'.r335.enabled/assignment')
                      if mode=='APP_OWNED' else ('exec-out','run-as',collector.PACKAGE,'cat',collector.PREF))
            self.assertEqual(argv,expected)
            reads.append(argv)
            return prefs+b' ' if changed and len(reads)==2 else prefs
        out=f.root/'start-release.json'
        args=SimpleNamespace(calendar=f.calendar,apk=apk,source_commit=sample.C,serial='test-serial',
                             capture_source=mode,phase='START',out=out)
        with patch.object(collector,'adb',adb),patch.object(collector,'clock',return_value=start['after']):
            result=collector.capture(args)
        self.assertEqual(len(reads),2)
        self.assertEqual(result['captureSource'],mode)
        self.assertEqual(collector.read_capture(out,f.calendar,'START')[0],result)

    def test_release_reads_only_provider(self):
        self.capture('APP_OWNED')

    def test_private_route_still_reads_original(self):
        self.capture('ADB_PRIVATE')

    def test_change_between_reads_refused(self):
        with self.assertRaisesRegex(ValueError,'PREFERENCES_CHANGED_DURING_COPY'):
            self.capture('APP_OWNED',changed=True)


if __name__=='__main__':
    unittest.main(verbosity=2)
