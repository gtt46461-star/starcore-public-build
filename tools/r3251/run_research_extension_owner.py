#!/usr/bin/env python3
"""Run the actual complete research coordinator after actual legacy owners on native SQLite."""
import argparse, datetime, hashlib, json, os, subprocess, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--classes', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    out = args.out.resolve(); out.mkdir(parents=True, exist_ok=False)
    sdk = Path(os.environ['ANDROID_HOME']) / 'platforms/android-36/android.jar'
    jdbc = Path(os.environ['SQLITE_JDBC'])
    slf = Path(os.environ['SLF4J_JAR'])
    production = ROOT / 'app/src/main/java/com/ethk3/novaonepro'
    names = ['LegacyDdlR31', 'LegacySchemaCoordinatorR926', 'SchemaDomainCoordinatorR926',
             'DbHelper', 'DbStore', 'LegacySettlementProofR79', 'ResearchSchemaCoordinatorR926']
    sources = [production / (name + '.java') for name in names]
    fixture = ROOT / 'tools/r31/unified_host/RuntimeSchemaR31Host.java'
    supplemental = ROOT / 'tools/r3251/ResearchExtensionOwnerHost.java'
    all_inputs = sources + [fixture, supplemental, Path(__file__).resolve()]
    def hashes():
        return {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in all_inputs}
    inputs = hashes(); commands = []
    def run(argv, name):
        result = subprocess.run(list(map(str, argv)), cwd=ROOT, capture_output=True, text=True, timeout=600)
        (out / name).write_text(result.stdout + result.stderr)
        commands.append(dict(argv=list(map(str, argv)), exitCode=result.returncode, log=name))
        if result.returncode:
            raise RuntimeError(name + '\n' + (result.stdout + result.stderr)[-6000:])
        return result.stdout
    with tempfile.TemporaryDirectory(prefix='r3251-research-owner-') as temporary:
        work = Path(temporary); platform = work / 'platform'; current = work / 'production'; test = work / 'test'
        for path in [platform, current, test]: path.mkdir()
        shims = list((ROOT / 'tools/r31/ddl_host').rglob('*.java'))
        shims += list((ROOT / 'tools/r29/qual_host/shim/android/database/sqlite').glob('SQLite*Exception.java'))
        log = work / 'Log.java'
        log.write_text('package android.util;public final class Log{public static int w(String a,String b){return 0;}public static int w(String a,String b,Throwable c){return 0;}}')
        clock = work / 'SystemClock.java'
        clock.write_text('package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return elapsedRealtime();}}')
        shims += [log, clock]
        run(['javac', '-cp', os.pathsep.join(map(str, [sdk, args.classes])), '-d', platform, *shims], 'platform.log')
        run(['javac', '-sourcepath', '', '-implicit:none', '-cp', os.pathsep.join(map(str, [sdk, platform, args.classes])), '-d', current, *sources], 'production.log')
        original = fixture.read_text()
        anchor = '            String first=SchemaDomainCoordinatorR926.fingerprint(db,critical);'
        assert original.count(anchor) == 1
        wrapped = work / 'RuntimeSchemaR31Host.java'
        wrapped.write_text(original.replace(anchor, '            ResearchExtensionOwnerHost.run(db);\n' + anchor))
        cp = os.pathsep.join(map(str, [platform, current, test, args.classes, jdbc, slf, sdk]))
        run(['javac', '-cp', cp, '-d', test, ROOT / 'tools/r31/unified_host/ColdSchemaR31Host.java', wrapped, supplemental], 'fixture.log')
        output = run(['java', '-Xmx768m', '-cp', cp, 'com.ethk3.novaonepro.RuntimeSchemaR31Host', work / 'runtime.db'], 'run.log')
        assert 'R3251_COMPLETE_RESEARCH_OWNER_NATIVE_PASS' in output
        print(output, end='')
    assert hashes() == inputs, 'SOURCE_CHANGED_DURING_TEST'
    (out / 'RECEIPT.json').write_text(json.dumps(dict(state='PASS_HOST_NATIVE_ONLY', capturedAt=datetime.datetime.now(datetime.timezone.utc).isoformat(), sourceHead=subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(), sources=inputs, commands=commands, Android='NOT_RUN', physicalDevice='NOT_RUN', naturalQuality='NOT_PROVEN'), indent=2) + '\n')

if __name__ == '__main__': main()
