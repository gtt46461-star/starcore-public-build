#!/usr/bin/env python3
"""Build unchanged product JNI for host replay; this is not an Android ABI build."""
import argparse
import hashlib
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def source_inputs():
    cpp = ROOT / 'app/src/main/cpp'
    sources = [cpp / 'report_native_cp4.c', cpp / 'vendor/sqlite3.c']
    for component in ['common', 'compress', 'decompress']:
        sources.extend(sorted((cpp / 'vendor/zstd/lib' / component).glob('*.c')))
    # A header-only change changes the compiled engine too. The builder fixes
    # compile flags and symbol checks, so its own bytes are part of this input.
    inputs = sources + sorted(cpp.glob('*.inc')) + sorted(cpp.rglob('*.h')) + [Path(__file__).resolve()]
    return sources, inputs


def input_rows(inputs):
    return [{'path': str(p.relative_to(ROOT)),
             'sha256': hashlib.sha256(p.read_bytes()).hexdigest()} for p in inputs]


def verify_reuse(directory):
    directory = directory.resolve()
    receipt = json.loads((directory / 'receipt.json').read_text())
    _, inputs = source_inputs()
    current = input_rows(inputs)
    recorded = receipt.get('source_files', [])
    # Equality also refuses legacy receipts which omitted vendor headers;
    # matching the subset recorded by an old build cannot prove current input.
    if receipt.get('native_input_manifest_version') != 2 or recorded != current:
        raise RuntimeError('NATIVE_COMPLETE_INPUT_IDENTITY_MISSING_OR_CHANGED_REBUILD_REQUIRED')
    library = directory / 'libstarcore_report_cp4.so'
    if receipt.get('status') != 'PASS' or hashlib.sha256(library.read_bytes()).hexdigest() != receipt.get('library_sha256'):
        raise RuntimeError('NATIVE_BINARY_CHANGED_OR_BUILD_NOT_PASSED')
    print('PASS|NATIVE_REUSE_INPUT_IDENTITY_ONLY|NO_TEST_RESULT_REUSED', flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk', type=Path)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--output-dir', type=Path)
    mode.add_argument('--verify-reuse', type=Path)
    args = parser.parse_args()
    if args.verify_reuse is not None:
        verify_reuse(args.verify_reuse)
        return
    if args.jdk is None:
        parser.error('--jdk is required when building --output-dir')
    output = args.output_dir.resolve()
    output.mkdir(parents=True, exist_ok=False)
    cpp = ROOT / 'app/src/main/cpp'
    sources, inputs = source_inputs()
    before = input_rows(inputs)
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
    if input_rows(inputs) != before:
        raise RuntimeError('NATIVE_INPUT_CHANGED_DURING_COMPILATION_NO_PASS')
    receipt = {'status': 'PASS', 'scope': 'ACTUAL_PRODUCT_JNI_HOST_BUILD_NOT_ANDROID',
               'native_input_manifest_version': 2,
               'command': command, 'library_path': str(library),
               'library_sha256': hashlib.sha256(library.read_bytes()).hexdigest(),
               'source_files': before,
               'guard_policy': 'Original diagnostics Owner and CAS admission remain unchanged.'}
    (output / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print('PASS|ACTUAL_PRODUCT_JNI_HOST_BUILD|' + receipt['library_sha256'], flush=True)


if __name__ == '__main__':
    main()
