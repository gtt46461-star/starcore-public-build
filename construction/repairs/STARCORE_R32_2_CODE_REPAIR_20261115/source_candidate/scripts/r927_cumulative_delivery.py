#!/usr/bin/env python3
"""Cumulative delivery: exact tracked source, retained archives, verified signed APK, all evidence.

Physical-device and future-effect evidence remains explicit and outside the user-defined construction percentage.
"""
import hashlib, json, os, shutil, subprocess, zipfile
from pathlib import Path
from r927_verify_release import verify, sha

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/r927-cumulative'
ZIP=ROOT/'build/STARCORE_5.4.1.0.2_R927_BATCH2_CUMULATIVE_TOTAL.zip'

def main():
    release=verify()
    required=['reports/r927/AUDIT_SQL_RESULT.json','reports/r927/CHANGE_LEDGER.json',
              'reports/r927/CHANGE_LEDGER.csv','reports/r927/STATUS.json','reports/r927/BATCH2_CONSTRUCTION_MATRIX.json',
              'reports/r927/ENGINEERING_CLOSURE_RESULT.json','reports/r927/BACKUP_REPLAY_TOOL_RESULT.json',
              'reports/r927/FINAL_HOST_SUMMARY.txt','reports/r927/BATCH2_GATE.txt',
              'reports/r927/BEHAVIOR_GATE.txt','reports/r927/SECRET_GATE.txt',
              'delivery/r927-batch2/HANDOFF_ZH.md']
    for name in required:
        if not (ROOT/name).is_file():raise RuntimeError('REQUIRED_DELIVERY_FILE_MISSING:'+name)
    if not json.loads((ROOT/required[0]).read_text())['passed']:raise RuntimeError('AUDIT_NOT_PASSED')
    matrix=json.loads((ROOT/'reports/r927/BATCH2_CONSTRUCTION_MATRIX.json').read_text())
    if matrix['construction_total']!=47 or matrix['construction_complete']!=47 or matrix['construction_completion_percent']!=100.0:raise RuntimeError('BATCH2_CONSTRUCTION_NOT_COMPLETE')
    for name,marker in [('FINAL_HOST_SUMMARY.txt','R925_COMPILE_HOST=PASS'),
                        ('BATCH2_GATE.txt','R926_BATCH2_GATE=PASS'),
                        ('BEHAVIOR_GATE.txt','R36_GATE=PASS'),('SECRET_GATE.txt','R366_SECRET_GATE=PASS')]:
        if marker not in (ROOT/'reports/r927'/name).read_text():raise RuntimeError('GATE_NOT_PASSED:'+name)
    if OUT.exists():shutil.rmtree(OUT)
    for name in ['01_APK','02_COMPLETE_SOURCE','03_CHANGE_LEDGER','04_VERIFICATION',
                 '05_PROGRESS','06_ROLLBACK','07_BUILD_REPRO','08_PLAN_SNAPSHOT','09_DATA_AND_ASSETS']:
        (OUT/name).mkdir(parents=True)
    tracked=subprocess.check_output(['git','ls-files','-z'],cwd=ROOT).decode().split('\0')
    for name in tracked:
        if not name:continue
        p=ROOT/name
        if not p.is_file():raise RuntimeError('TRACKED_SOURCE_MISSING:'+name)
        dest=OUT/'02_COMPLETE_SOURCE'/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,dest)
    shutil.copy2(ROOT/release['apk_path'],OUT/'01_APK'/Path(release['apk_path']).name)
    for p in (ROOT/'reports/r927').iterdir():
        if p.is_file():shutil.copy2(p,OUT/('03_CHANGE_LEDGER' if 'CHANGE_LEDGER' in p.name else '04_VERIFICATION')/p.name)
    shutil.copytree(ROOT/'reports/r927/release-evidence',OUT/'04_VERIFICATION/release-evidence')
    shutil.copytree(ROOT/'reports/r926/batch2',OUT/'04_VERIFICATION/batch2',dirs_exist_ok=True)
    shutil.copytree(ROOT/'docs/5.4.2',OUT/'08_PLAN_SNAPSHOT/5.4.2')
    shutil.copy2(ROOT/'reports/r927/STATUS.json',OUT/'05_PROGRESS/STATUS.json')
    shutil.copy2(ROOT/'delivery/r927-batch2/HANDOFF_ZH.md',OUT/'07_BUILD_REPRO/HANDOFF_ZH.md')
    (OUT/'06_ROLLBACK/README.txt').write_text('回退基线 b2439b2078434690c355890cf04083997e254fbf。新增时延字段为兼容迁移，数据库user_version仍为36。安装前备份用户数据库；不通过降低versionCode或覆盖用户数据库回退。\n')
    (OUT/'09_DATA_AND_ASSETS/README.txt').write_text('完整历史输入以74个分片保留在02_COMPLETE_SOURCE/delivery/r926-batch1/input-archive-parts，含原有工具链和资产。按07_BUILD_REPRO交接文档重组，逐片及整包SHA256核验。当前源码/新APK以01和02目录为准，历史归档不可覆盖当前代码。原材料未提供当前手机业务数据库，不生成冒充用户数据的空库；覆盖安装保留手机原库，现有history_seed资产完整保留。\n')
    parts=ROOT/'delivery/r926-batch1/input-archive-parts'
    manifest=json.loads((parts/'parts_manifest.json').read_text());whole=hashlib.sha256();size=0
    for e in manifest['parts']:
        p=parts/e['filename']
        if p.stat().st_size!=e['bytes'] or sha(p)!=e['sha256']:raise RuntimeError('ARCHIVE_PART_DRIFT')
        with p.open('rb') as f:
            for b in iter(lambda:f.read(1024*1024),b''):whole.update(b);size+=len(b)
    if size!=manifest['bytes'] or whole.hexdigest()!=manifest['sha256']:raise RuntimeError('ARCHIVE_REASSEMBLY_DRIFT')
    info={'versionName':release['versionName'],'versionCode':release['versionCode'],
          'source_commit':os.environ.get('DELIVERY_SOURCE_COMMIT',os.environ.get('GITHUB_SHA','LOCAL_VERIFIED_WORKTREE')),
          'apk_sha256':release['apk_sha256'],'archive_bytes':size,'archive_sha256':whole.hexdigest(),
          'batch2_construction':'47/47_100_PERCENT','physical_device':'EXCLUDED_BY_USER_FROM_CONSTRUCTION_COMPLETION','future_oos':'NOT_PROVEN','external_runtime_evidence':'SEPARATELY_REPORTED'}
    (OUT/'PACKAGE_INFO.json').write_text(json.dumps(info,ensure_ascii=False,indent=2)+'\n')
    (OUT/'MANIFEST_SHA256.txt').write_text(''.join(sha(p)+'  '+p.relative_to(OUT).as_posix()+'\n' for p in sorted(OUT.rglob('*')) if p.is_file()))
    with zipfile.ZipFile(ZIP,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(OUT.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(OUT).as_posix())
    with zipfile.ZipFile(ZIP) as z:
        bad=z.testzip()
        if bad:raise RuntimeError('ZIP_CRC_FAIL:'+bad)
    digest=sha(ZIP);ZIP.with_suffix('.zip.sha256').write_text(digest+'  '+ZIP.name+'\n')
    print(json.dumps({'status':'PASS','zip':str(ZIP),'bytes':ZIP.stat().st_size,'sha256':digest},indent=2))
if __name__=='__main__':main()
