"""Exercise the actual Gradle task graph and its independent output fence.

Run serially with compilation/assembly: output corruption is restored in finally.
The generator and Gradle Exec tasks are real; no policy or identity is mocked.
"""
import os
import json
from pathlib import Path
import subprocess
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / 'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java'
MANIFEST = ROOT / 'app/build/generated/r967/semantic_manifest.json'


class GradleSemanticGate(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.gradle = os.environ['STARCORE_GRADLE_BIN']

    def gradle_run(self, *tasks):
        launcher = [sys.executable, self.gradle] if self.gradle.endswith('.py') else [self.gradle]
        command = [*launcher, '--offline', '--no-daemon', '--console=plain', *tasks]
        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, timeout=180)
        evidence = os.environ.get('STARCORE_TEST_EVIDENCE_DIR')
        if evidence:
            directory = Path(evidence)
            directory.mkdir(parents=True, exist_ok=True)
            number = len(list(directory.glob('gradle-case-*.json'))) + 1
            stem = directory / f'gradle-case-{number:02d}'
            stem.with_suffix('.stdout.log').write_text(result.stdout, encoding='utf-8')
            stem.with_suffix('.stderr.log').write_text(result.stderr, encoding='utf-8')
            stem.with_suffix('.json').write_text(json.dumps({
                'test': self.id(), 'command': command, 'cwd': str(ROOT),
                'exit_code': result.returncode,
            }, indent=2) + '\n', encoding='utf-8')
        return result

    def generate(self):
        subprocess.run(['python3', 'scripts/r967_semantic_identity.py'],
                       cwd=ROOT, check=True, capture_output=True)

    def test_compile_uses_independent_check_before_java_consumer(self):
        r = self.gradle_run(':app:compileDebugJavaWithJavac', '--dry-run')
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        tasks = [line.split()[0] for line in r.stdout.splitlines() if line.startswith(':app:')]
        self.assertLess(tasks.index(':app:generateR967Semantics'), tasks.index(':app:verifyR967Semantics'))
        self.assertLess(tasks.index(':app:verifyR967Semantics'), tasks.index(':app:compileDebugJavaWithJavac'))

    def test_acceptance_executes_original_mutation_self_test(self):
        r = self.gradle_run(':app:verifyCanonicalPlanBuildCP3', '--dry-run')
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        tasks = [line.split()[0] for line in r.stdout.splitlines() if line.startswith(':app:')]
        self.assertLess(tasks.index(':app:verifyR967Semantics'), tasks.index(':app:testR967Semantics'))
        self.assertLess(tasks.index(':app:testR967Semantics'), tasks.index(':app:verifyCanonicalPlanBuildCP3'))

    def test_assembly_has_post_assembly_check_without_regeneration(self):
        r = self.gradle_run(':app:assembleDebug', '--dry-run')
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        tasks = [line.split()[0] for line in r.stdout.splitlines() if line.startswith(':app:')]
        self.assertLess(tasks.index(':app:assembleDebug'), tasks.index(':app:verifyFinalR967Semantics'))
        r = self.gradle_run(':app:verifyFinalR967Semantics', '--dry-run')
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertNotIn(':app:generateR967Semantics', r.stdout)

    def corruption(self, target, corrupt, reason):
        self.generate()
        original = target.read_bytes()
        try:
            damaged = corrupt(original)
            self.assertNotEqual(damaged, original)
            target.write_bytes(damaged)
            r = self.gradle_run(':app:verifyFinalR967Semantics')
            self.assertNotEqual(r.returncode, 0, r.stdout + r.stderr)
            self.assertIn(reason, r.stdout + r.stderr)
            self.assertEqual(target.read_bytes(), damaged, 'A check must not repair its own evidence')
        finally:
            self.generate()
        r = self.gradle_run(':app:verifyFinalR967Semantics')
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertEqual(target.read_bytes(), original)

    def test_corrupt_java_fails_gradle_and_regeneration_recovers(self):
        self.corruption(JAVA, lambda b: b + b'// corrupt identity output\n', 'R967_GENERATED_IDENTITY_STALE')

    def test_corrupt_manifest_fails_gradle_and_regeneration_recovers(self):
        self.corruption(MANIFEST, lambda b: b + b'\n', 'R967_GENERATED_MANIFEST_STALE')


if __name__ == '__main__':
    unittest.main(verbosity=2)
