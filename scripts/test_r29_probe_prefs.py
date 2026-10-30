import unittest
from r29_probe_prefs import read_preferences


class SnapshotTest(unittest.TestCase):
    def test_transient_file_replacement_is_retried(self):
        reads = iter(['', '<map><string', '<map><string name="init_status">READY</string><long name="heartbeat_at" value="123"/></map>'])
        self.assertEqual({'init_status': 'READY', 'heartbeat_at': '123'}, read_preferences(lambda: next(reads), sleep=lambda _: None))

    def test_permanent_failure_never_becomes_ready(self):
        calls = []
        self.assertEqual({}, read_preferences(lambda: calls.append(1) or '', sleep=lambda _: None))
        self.assertEqual(3, len(calls))

    def test_actual_failed_state_is_preserved(self):
        result = read_preferences(lambda: '<map><string name="init_status">FAILED</string><string name="init_last_error">IO_ERROR</string></map>')
        self.assertEqual('FAILED', result['init_status']); self.assertEqual('IO_ERROR', result['init_last_error'])


if __name__ == '__main__': unittest.main()
