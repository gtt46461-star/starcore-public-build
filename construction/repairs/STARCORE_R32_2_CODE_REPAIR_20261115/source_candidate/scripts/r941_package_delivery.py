#!/usr/bin/env python3
"""Build and verify a complete R941 delivery from an observed GitHub tree."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import sqlite3
import zipfile


def sha256(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for data in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(data)
    return h.hexdigest()


def git_blob(path):
    h = hashlib.sha1(('blob %d\0' % path.stat().st_size).encode())
    with path.open('rb') as stream:
        for data in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(data)
    return h.hexdigest()


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def copy_file(source, target):
    if not source.is_file() or source.is_symlink():
        raise ValueError('SOURCE_NOT_REGULAR_FILE:' + str(source))
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)


def copy_tree(source, target):
    if not source.is_dir():
        raise ValueError('SOURCE_DIRECTORY_MISSING:' + str(source))
    for path in sorted(source.rglob('*')):
        if path.is_symlink():
            raise ValueError('UNEXPECTED_SYMLINK:' + str(path))
        if path.is_file():
            copy_file(path, target / path.relative_to(source))


def verify_original_parts(source):
    directory = source / 'delivery/r926-batch1/input-archive-parts'
    manifest = json.loads((directory / 'parts_manifest.json').read_text(encoding='utf-8'))
    whole, total = hashlib.sha256(), 0
    seen = set()
    for i, entry in enumerate(manifest['parts']):
        name = entry['filename']
        if Path(name).name != name or name in seen or entry['index'] != i:
            raise ValueError('INVALID_ORIGINAL_PART_ORDER')
        seen.add(name)
        path = directory / name
        if path.stat().st_size != entry['bytes'] or sha256(path) != entry['sha256']:
            raise ValueError('ORIGINAL_PART_MISMATCH:' + name)
        with path.open('rb') as stream:
            for data in iter(lambda: stream.read(1024 * 1024), b''):
                whole.update(data)
                total += len(data)
    if total != manifest['bytes'] or whole.hexdigest() != manifest['sha256']:
        raise ValueError('ORIGINAL_ARCHIVE_MISMATCH')
    return dict(parts=len(seen), bytes=total, sha256=whole.hexdigest(), status='PASS')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for option in ('source', 'baseline', 'apk', 'apk_evidence', 'input_report',
                   'evidence_dir', 'remote_tree', 'handoff', 'output_dir'):
        p.add_argument('--' + option.replace('_', '-'), type=Path, required=True)
    args = p.parse_args()
    source = args.source.resolve()
    tree = json.loads(args.remote_tree.read_text(encoding='utf-8'))
    handoff = json.loads(args.handoff.read_text(encoding='utf-8'))
    if tree.get('truncated') or tree['sha'] != handoff['source_tree']:
        raise ValueError('REMOTE_TREE_NOT_COMPLETE_OR_NOT_BOUND')
    result = json.loads((source / 'reports/r941/RELEASE_RESULT.json').read_text(encoding='utf-8'))
    if sha256(args.apk) != result['apk_sha256'] or args.apk.stat().st_size != result['apk_bytes']:
        raise ValueError('SIGNED_APK_MISMATCH')
    inputs = json.loads((source / 'reports/r941/APK_INPUT_MANIFEST.json').read_text(encoding='utf-8'))
    for entry in inputs:
        path = source / entry['path']
        if path.stat().st_size != entry['bytes'] or sha256(path) != entry['sha256']:
            raise ValueError('APK_INPUT_MISMATCH:' + entry['path'])
    audit = json.loads((source / 'reports/r941/INPUT_REPORT_AUDIT.json').read_text(encoding='utf-8'))
    if sha256(args.input_report) != audit['input_sha256']:
        raise ValueError('INPUT_REPORT_MISMATCH')
    with sqlite3.connect((args.evidence_dir / 'report_evidence.sqlite').resolve().as_uri() + '?mode=ro', uri=True) as db:
        if db.execute('PRAGMA integrity_check').fetchall() != [('ok',)]:
            raise ValueError('EVIDENCE_SQLITE_INTEGRITY')
        for name, expected in audit['tables'].items():
            quoted = '"' + name.replace('"', '""') + '"'
            if db.execute('SELECT COUNT(*) FROM ' + quoted).fetchone()[0] != expected:
                raise ValueError('EVIDENCE_SQLITE_ROWS:' + name)
    original = verify_original_parts(source)
    output = args.output_dir.resolve()
    output.mkdir(parents=True, exist_ok=True)
    folder = output / 'STARCORE_5.4.1.0.10_R941_FULL'
    folder.mkdir(exist_ok=False)
    blobs = [entry for entry in tree['tree'] if entry['type'] == 'blob']
    for entry in blobs:
        relative = PurePosixPath(entry['path'])
        if relative.is_absolute() or '..' in relative.parts or entry['mode'] not in ('100644', '100755'):
            raise ValueError('UNSAFE_OR_UNSUPPORTED_GIT_PATH:' + entry['path'])
        src = source / entry['path']
        if entry['path'] == 'delivery/r941/' + args.apk.name:
            src = args.apk
        if git_blob(src) != entry['sha']:
            raise ValueError('REMOTE_SOURCE_MISMATCH:' + entry['path'])
        dst = folder / '02_COMPLETE_SOURCE' / entry['path']
        copy_file(src, dst)
        dst.chmod(0o755 if entry['mode'] == '100755' else 0o644)
    copy_file(args.apk, folder / '01_APK' / args.apk.name)
    for name in ('FILE_CHANGES.json', 'FILE_CHANGES.tsv', 'CHANGE_SUMMARY.json'):
        copy_file(source / 'reports/r941' / name, folder / '03_CHANGE_LEDGER' / name)
    copy_tree(source / 'reports/r941', folder / '04_VERIFICATION')
    copy_tree(source / 'docs/5.4.2', folder / '05_PLANS')
    copy_tree(source / 'app/src/main/assets', folder / '06_DATA_AND_ASSETS/app_assets')
    copy_tree(args.evidence_dir, folder / '06_DATA_AND_ASSETS/report_evidence')
    copy_file(args.input_report, folder / '06_DATA_AND_ASSETS' / args.input_report.name)
    copy_file(args.remote_tree, folder / '07_HANDOFF/REMOTE_GIT_TREE.json')
    copy_file(args.handoff, folder / '07_HANDOFF/DELIVERY_RECEIPT.json')
    copy_file(source / 'reports/r941/DELIVERY_HANDOFF_ZH.md', folder / '07_HANDOFF/DELIVERY_HANDOFF_ZH.md')
    copy_tree(args.apk_evidence, folder / '08_BUILD_AND_APK_EVIDENCE')
    for old in sorted(args.baseline.iterdir()):
        if old.name == '02_COMPLETE_SOURCE':
            continue
        target = folder / '09_HISTORICAL_R939_DELIVERY' / old.name
        if old.is_dir():
            copy_tree(old, target)
        else:
            copy_file(old, target)
    readme = '''# 星核天枢 5.4.1.0.10 / R941 完整交付包

安装入口：01_APK 内的 release.apk。版本号 577250941，包名 com.ethk3.novaonepro；使用原证书签名，可覆盖同签名旧版。直接安装，不要先卸载或清除应用数据。

本包包含当前完整源码、655 个生产 Java 文件、资产、原施工方案与加强方案、逐文件行/字符统计、编译和回归日志、签名 APK、检测报告原文与重建证据 SQLite，以及原始74片资产归档。完整当前源码在 02_COMPLETE_SOURCE，历史报告在 09_HISTORICAL_R939_DELIVERY。当前源码与 APK 可直接使用，无需先重组历史归档。

主要修复：滚动与页面挂载、长详情和完整导出、研究事务重复DDL、13项维护任务轮转、漏请求与积压恢复、挑战者/证书/主链统一计数及授权审计。细节见 04_VERIFICATION/REPAIR_AND_VALIDATION_ZH.md。

已核验生产源码、构建输入、远端文件和签名APK的一致性，21项专项回归重跑通过。GitHub Android任务未分配执行器、执行步骤为0；本版本手机滑动/闪退、息屏后台、每期出号与耗时仍待实测。15%实际出手和40%未来命中是目标，没有在本次证据中证明。证书与主链应用仍按真实样本及证书门槛执行，不伪造非零计数。

06_DATA_AND_ASSETS/report_evidence/report_evidence.sqlite 仅重建检测报告导出的18张表（46,158行），不是手机全库备份，不要把它覆盖到手机工作数据库。本次升级保持手机原数据库名称与版本36。

最终远端提交与PR见07_HANDOFF/DELIVERY_RECEIPT.json。逐文件SHA-256清单见MANIFEST_SHA256.txt；清单覆盖包内其他所有文件，清单本身不自引用。打包时已流式读取ZIP内每个文件并核对内容哈希与CRC。

历史原始资产恢复入口：在02_COMPLETE_SOURCE目录运行 python3 scripts/r926_reassemble_total.py --manifest delivery/r926-batch1/input-archive-parts/parts_manifest.json --out-dir build/original-input-restore。该归档是历史输入；继续开发以当前源码为准。
'''
    (folder / 'README_FIRST_ZH.md').write_text(readme, encoding='utf-8')
    info = dict(result, source_commit=handoff['source_commit'], source_tree=tree['sha'],
                source_file_count=len(blobs), apk_input_file_count=len(inputs),
                source_materialization='ALL_BLOBS_MATCH_GIT_TREE',
                production_java_files=655, original_input=original,
                report_evidence_rows=sum(audit['tables'].values()),
                report_evidence_tables=len(audit['tables']),
                report_database_is_phone_backup=False,
                android_runtime_status='NOT_EXECUTED',
                delivery_scope='COMPLETE_FILES_WITH_EXPLICIT_RUNTIME_EVIDENCE_LIMITS')
    write_json(folder / 'PACKAGE_INFO.json', info)
    files = sorted(path for path in folder.rglob('*') if path.is_file())
    hashes = {path.relative_to(folder).as_posix(): sha256(path) for path in files}
    manifest = folder / 'MANIFEST_SHA256.txt'
    manifest.write_text(''.join(value + '  ' + name + '\n' for name, value in hashes.items()), encoding='utf-8')
    hashes[manifest.name] = sha256(manifest)
    archive = output / (folder.name + '.zip')
    temporary = output / (folder.name + '.zip.partial')
    print(json.dumps(dict(stage='VERIFIED_INPUTS_AND_STAGED', files=len(hashes), source_files=len(blobs))), flush=True)
    with zipfile.ZipFile(temporary, 'x', compression=zipfile.ZIP_DEFLATED, compresslevel=6, allowZip64=True) as z:
        for name in sorted(hashes):
            path = folder / name
            compressed = (path.suffix.lower() in ('.zip', '.gz', '.apk', '.jar', '.png', '.jpg', '.pdf')
                          or '.zip.part' in path.name)
            z.write(path, folder.name + '/' + name,
                    compress_type=zipfile.ZIP_STORED if compressed else zipfile.ZIP_DEFLATED)
    print(json.dumps(dict(stage='VERIFYING_ZIP', bytes=temporary.stat().st_size)), flush=True)
    with zipfile.ZipFile(temporary) as z:
        names = z.namelist()
        if len(names) != len(set(names)) or len(names) != len(hashes):
            raise ValueError('ZIP_ENTRY_COUNT_OR_DUPLICATES')
        for name, expected in hashes.items():
            h = hashlib.sha256()
            with z.open(folder.name + '/' + name) as stream:
                for data in iter(lambda: stream.read(1024 * 1024), b''):
                    h.update(data)
            if h.hexdigest() != expected:
                raise ValueError('ZIP_CONTENT_HASH_MISMATCH:' + name)
    os.replace(temporary, archive)
    receipt = dict(status='PASS', path=str(archive), bytes=archive.stat().st_size,
                   sha256=sha256(archive), files=len(hashes), source_files=len(blobs),
                   all_archive_entries_verified=True, original_parts_verified=original['parts'])
    write_json(output / 'PACKAGING_RESULT.json', receipt)
    print(json.dumps(receipt, ensure_ascii=False), flush=True)


if __name__ == '__main__':
    main()
