#!/usr/bin/env python3
"""Build unchanged product JNI for host replay; this is not an Android ABI build."""
import argparse
import hashlib
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk', type=Path, required=True)
    parser.add_argument('--output-dir', type=Path, required=True)
    args = parser.parse_args()
    output = args.output_dir.resolve()
    output.mkdir(parents=True, exist_ok=False)
    cpp = ROOT / 'app/src/main/cpp'
    sources = [cpp / 'report_native_cp4.c', cpp / 'vendor/sqlite3.c']
    for component in ['common', 'compress', 'decompress']:
        sources.extend(sorted((cpp / 'vendor/zstd/lib' / component).glob('*.c')))
    headers = [args.jdk.resolve() / 'include', args.jdk.resolve() / 'include/linux',
               cpp / 'vendor', cpp / 'vendor/zstd/lib']
    library = output / 'libstarcore_report_cp4.so'
    command = ['gcc', '-shared', '-fPIC', '-std=c11', '-O2', '-fvisibility=hidden',
               '-DSQLITE_THREADSAFE=1', '-DSQLITE_TEMP_STORE=1',
               '-DSQLITE_DEFAULT_MEMSTATUS=1', '-DSQLITE_OMIT_LOAD_EXTENSION=1',
               '-DZSTD_DISABLE_ASM=1', *['-I' + str(x) for x in headers],
               *map(str, sources), '-o', str(library), '-ldl', '-lm', '-pthread']
    print('COMMAND ' + json.dumps(command), flush=True)
    subprocess.run(command, cwd=ROOT, check=True)
    symbols = subprocess.check_output(['nm', '-D', str(library)], text=True)
    if 'Java_com_ethk3_novaonepro_ReportNativeCP4_trainingCasBytes0' not in symbols:
        raise RuntimeError('ACTUAL_PRODUCT_CAS_JNI_SYMBOL_MISSING')
    receipt = {'status': 'PASS', 'scope': 'ACTUAL_PRODUCT_JNI_HOST_BUILD_NOT_ANDROID',
               'command': command, 'library_path': str(library),
               'library_sha256': hashlib.sha256(library.read_bytes()).hexdigest(),
               'source_files': [{'path': str(p.relative_to(ROOT)),
                                 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()}
                                for p in sources],
               'guard_policy': 'Original diagnostics Owner and CAS admission remain unchanged.'}
    (output / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print('PASS|ACTUAL_PRODUCT_JNI_HOST_BUILD|' + receipt['library_sha256'], flush=True)


if __name__ == '__main__':
    main()
