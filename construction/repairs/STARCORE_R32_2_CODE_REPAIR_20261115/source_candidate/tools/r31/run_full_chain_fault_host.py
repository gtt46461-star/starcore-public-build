#!/usr/bin/env python3
"""R31-B12-C02#A3a: full-chain fault injection on one native SQLite database (HOST, not ART/device/natural quality).

Compiles tools/r31/full_chain_host/R31FullChainFaultHost.java against the gradle classes and the owned-runtime
platform shim, runs it (it starts its own child JVMs for the process-death cases) and writes the receipt.
"""
import argparse, json, os, subprocess, sys, tempfile
from pathlib import Path
from host_runtime_fixture import compile_owned_platform
from host_tool_support import dependencies

ROOT = Path(__file__).resolve().parents[2]
HOST = ROOT / 'tools/r31/full_chain_host/R31FullChainFaultHost.java'
FIXTURE = ROOT / 'tools/r31/host/R31OwnedRuntimeFixture.java'
MANIFEST = ROOT / 'tools/r31/fault_injection_manifest.json'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default=str(ROOT / 'app/build/r312-host/run_full_chain_fault_host'))
    a = ap.parse_args()
    out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix='r31-full-chain-'))
    classes, sdk, rjar, jdbc = dependencies(ROOT)
    platform = compile_owned_platform(ROOT, work / 'owned-platform', classes, sdk)
    test = work / 'test'; test.mkdir()
    cp = os.pathsep.join(map(str, [platform, test, classes, jdbc, sdk]))
    subprocess.run(['javac', '-nowarn', '-encoding', 'UTF-8', '-sourcepath', '', '-cp', cp, '-d', str(test), str(FIXTURE), str(HOST)], check=True)
    r = subprocess.run(['java', '-Xmx768m', '-cp', cp, 'com.ethk3.novaonepro.strategyv2.R31FullChainFaultHost'], cwd=ROOT, capture_output=True, text=True, timeout=1800)
    log = r.stdout + r.stderr
    (out / 'full_chain.log').write_text(log)
    print(log[-12000:])
    line = next((x for x in log.splitlines() if x.startswith('R31_FULL_CHAIN_FAULT_HOST|')), None)
    if r.returncode != 0 or line is None:
        print('R31_FULL_CHAIN_FAULT_HOST_FAIL exit=%d' % r.returncode); sys.exit(1)
    receipt = json.loads(line.split('|', 1)[1])
    manifest = json.loads(MANIFEST.read_text())
    covered = sorted(s['id'] for s in manifest['scenarios'] if 'HOST:run_full_chain_fault_host' in s['executed_by'])
    text = json.dumps(receipt)
    kills = receipt.get('processDeath', [])
    # F02 is proven by the recorded real JVM halts (each resumed and re-checked by the host), not by a label
    f02 = bool(kills) and all(k.get('halted') for k in kills)
    missing = [i for i in covered if (i == 'F02_PROCESS_DEATH' and not f02) or (i != 'F02_PROCESS_DEATH' and i not in text)]
    if receipt.get('status') != 'PASS' or missing:
        print('R31_FULL_CHAIN_FAULT_HOST_FAIL missing manifest scenarios in receipt: %s' % missing); sys.exit(1)
    receipt['manifestScenariosExecuted'] = covered
    (out / 'RECEIPT.json').write_text(json.dumps(receipt, indent=1, ensure_ascii=False) + '\n')
    print('R31_FULL_CHAIN_FAULT_HOST_PASS ' + json.dumps({'checks': receipt['checks'], 'scenarios': covered, 'kills': len(receipt['processDeath'])}))


if __name__ == '__main__':
    main()
