#!/usr/bin/env python3
"""Verify production R14-R16/P49/P50 kernels against the signed-off plan fixtures."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'tools/strategy_su2'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--evolution', action='store_true', help='Also run the final R16 rolling forward comparison')
    args = parser.parse_args()
    results = []
    logs = ROOT / 'build/plan1025'; logs.mkdir(parents=True, exist_ok=True)

    def run(name, argv, marker=None):
        start = time.monotonic()
        p = subprocess.run(list(map(str, argv)), cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        ok = p.returncode == 0 and (marker is None or marker in p.stdout)
        (logs / (name + '.log')).write_text(p.stdout, encoding='utf-8')
        results.append({'name': name, 'ok': ok, 'returncode': p.returncode, 'seconds': round(time.monotonic() - start, 3)})
        print(name, 'PASS' if ok else 'FAIL', flush=True)
        if not ok: print(p.stdout[-5000:], flush=True)
        return ok

    ok = run('generated_sql', ['python3', 'scripts/generate_strategy_su2_schema.py', '--check'])
    ok = run('production_sql', ['python3', 'scripts/test_r13_production_ddl.py']) and ok
    prod = ROOT / 'app/src/main/java/com/ethk3/novaonepro'
    sources = [p for p in (prod / 'strategyv2').glob('*.java') if not any(s in p.read_text() for s in ['import android.', 'import com.ethk3.novaonepro.DbStore;', 'import com.ethk3.novaonepro.*;'])]
    sources += [ROOT / 'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java']
    sources += [prod / 'r952' / name for name in ['AtomEvaluatorCP4.java', 'AtomWindowCP4.java', 'MemoryBudgetCP4.java', 'MatrixMetricsCP11.java', 'StrictCanonicalJson.java']]
    sources += list((BASE / 'tests').glob('*.java'))
    classes = logs / 'classes'; classes.mkdir(exist_ok=True)
    compiled = run('production_compile', ['java', 'com.sun.tools.javac.Main', '-source', '17', '-target', '17', '-encoding', 'UTF-8', '-d', classes] + sources)
    ok = compiled and ok
    fixtures = BASE / 'fixtures'
    tests = [
        ('OnlineModelsSU2Test', ['r13_parity'], 'R13_PARITY_PASS'),
        ('ModelLiveSU2Test', ['r13_parity'], 'R13_LIVE_EQUALS_REPLAY_PASS'),
        ('XiIncumbentSU2Test', ['r13_parity'], 'XI_INCUMBENT_PARITY_PASS'),
        ('MainlinePolicySU2Test', ['r13_ipm'], 'R13_IPM_PARITY_PASS'),
        ('CalibrationSU2Test', ['r13_parity', 'r14_parity'], 'R14_CALIBRATION_PASS'),
        ('CommitteeSU2Test', ['r14_parity'], 'R14_COMMITTEE_PARITY_PASS'),
        ('CommitteeLiveSU2Test', ['r14_parity'], 'R14_LIVE_EQUALS_REPLAY_PASS'),
        ('AtomNetSU2Test', ['r14b_atomnet'], 'R14B_ATOMNET_PARITY_PASS'),
        ('AtomNetR15Test', ['r15_atomnet', 'r14b_atomnet'], 'R15_ATOMNET_PARITY_PASS'),
        ('LiveR15Test', ['r14b_atomnet', 'r15_atomnet'], 'R15_LIVE_EQUALS_REPLAY_PASS'),
        ('RegMemSU2Test', ['r16_regmem'], 'R16_REGMEM_PARITY_PASS'),
        ('ContMatSU2Test', ['r16_contmat'], 'R16_CONTMAT_PARITY_PASS'),
        ('StageLayerSU2Test', ['r16_stage'], 'R16_STAGE_PARITY_PASS'),
        ('UpgradeDiagnosisSU2Test', ['r16_diag'], 'R16_DIAG_PARITY_PASS'),
        ('InformationProbeSU2Test', ['r16_probe'], 'R16_PROBE_PARITY_PASS'),
        ('StageDeploymentSU2Test', ['r13_worlds'], 'P49_STAGE_DEPLOYMENT_PASS'),
        ('MatrixSnapshotSU2Test', ['r13_worlds'], 'P46_MATRIX_SNAPSHOT_PASS'),
    ]
    if args.evolution:
        tests.append(('ModelEvolutionR16Test', ['r13_worlds'], 'R16_JAVA_FOUNDRY_PASS'))
    if compiled:
        for name, paths, marker in tests:
            ok = run(name, ['java', '-Xmx1600m', '-cp', classes, 'com.ethk3.novaonepro.strategyv2.' + name] + [fixtures / p for p in paths], marker) and ok
    report = {'status': 'PASS' if ok else 'FAIL', 'scope': 'Current production Java and SQLite, independent plan fixtures; physical-device endurance and natural forward efficacy require separate evidence', 'evolution_executed': args.evolution, 'source_sha256': {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}, 'checks': results}
    (logs / 'verification.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    return 0 if ok else 1


if __name__ == '__main__':
    raise SystemExit(main())
