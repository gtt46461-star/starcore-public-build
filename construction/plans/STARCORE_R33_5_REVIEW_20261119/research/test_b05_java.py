"""Compile and execute the corrected complete B05 owner on the selected JDK."""
import argparse
import hashlib
import json
import zipfile
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

from b05_pair_source import generate


def run(plan, support=None):
    archive = plan/'B05/verification_r335/c10_actual/final_run'
    vendor = plan/'B01/tests/host_runtime/vendor'
    jdbc = [vendor/'sqlite-jdbc-3.46.1.0.jar', vendor/'slf4j-api-2.0.13.jar',
            vendor/'slf4j-nop-2.0.13.jar']
    if support:
        production = Path(support)
        if not (production/'com/ethk3/novaonepro/strategyv2/HashingSU1.class').is_file():
            raise ValueError('MISSING_CURRENT_B05_CLASSES')
    else:
        production = Path(__file__).with_name('host-support17.jar')
        original = json.loads(Path(__file__).with_name('host-support17.json').read_bytes())
        if hashlib.sha256(production.read_bytes()).hexdigest() != original['jarSha256']:
            raise ValueError('B05_JDK17_HOST_SUPPORT_SHA_MISMATCH')
    if not all(p.is_file() for p in jdbc):
        raise ValueError('MISSING_ORIGINAL_JDBC')
    java = shutil.which('java')
    if not java:
        raise ValueError('JAVA_REQUIRED')
    compiler = [java, '-m', 'jdk.compiler/com.sun.tools.javac.Main']
    adapters = sorted((archive/'adapter/sources/android/database').rglob('*.java'))
    if not adapters:
        raise ValueError('MISSING_ORIGINAL_SQLITE_ADAPTER_SOURCES')
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        # The C10 read adapter declares an Android signature unused by paired().
        # A call to that overload must fail loudly in this host-only test.
        signal = root/'CancellationSignal.java'
        signal.write_text('package android.os; public final class CancellationSignal { '
                          'public void throwIfCanceled() { throw new UnsupportedOperationException(); } }')
        adapters.append(signal)
        owner = generate(plan, root/'candidate')
        classes = root/'classes'
        classes.mkdir()
        subprocess.run(compiler+['-encoding','UTF-8','-d',str(classes),*map(str,adapters)],check=True)
        cp = os.pathsep.join(map(str,[classes,production,*jdbc]))
        subprocess.run(compiler+['-encoding','UTF-8','-implicit:none','-sourcepath','',
                                  '-cp',cp,'-d',str(classes),str(owner),
                                  str(Path(__file__).with_name('PairWindowTest.java'))],check=True)
        subprocess.run([java,'-cp',cp,'com.ethk3.novaonepro.strategyv2.PairWindowTest'], check=True)


def rebuild_support(classes, source, output):
    manifest = json.loads(Path(__file__).with_name('host-support17.json').read_bytes())
    with zipfile.ZipFile(output, 'x') as jar:
        for name, record in sorted(manifest['classes'].items()):
            body = (classes/name).read_bytes()
            if (hashlib.sha256(body).hexdigest() != record['classSha256']
                    or hashlib.sha256((source/record['source']).read_bytes()).hexdigest() != record['sourceSha256']):
                raise ValueError('JDK17_B05_SOURCE_OR_CLASS_DRIFT: ' + name)
            entry = zipfile.ZipInfo(name, date_time=(1980,1,1,0,0,0))
            entry.compress_type = zipfile.ZIP_DEFLATED
            jar.writestr(entry, body)
    if hashlib.sha256(output.read_bytes()).hexdigest() != manifest['jarSha256']:
        raise ValueError('JDK17_B05_HOST_SUPPORT_DRIFT')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    parser.add_argument('--classes', type=Path, default=os.environ.get('R335_B05_CLASSES'),
                        help='Compiled B05 production classes from the current JDK stage')
    parser.add_argument('--rebuild-support', type=Path, help='Write the 12-class host JAR to a fresh path')
    parser.add_argument('--source', type=Path, help='Exact B05 source checkout used to compile --classes')
    args = parser.parse_args()
    if args.rebuild_support:
        if not args.source or not args.classes:
            parser.error('--rebuild-support requires --classes and --source')
        rebuild_support(args.classes, args.source, args.rebuild_support)
    else:
        run(args.plan.resolve(), args.classes)
