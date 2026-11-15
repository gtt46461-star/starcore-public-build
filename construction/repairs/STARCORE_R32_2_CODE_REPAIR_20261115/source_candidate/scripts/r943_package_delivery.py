#!/usr/bin/env python3
"""Assemble the checked-out release, verify every ZIP member, and record detached hashes."""
from pathlib import Path
import argparse, hashlib, json, shutil, subprocess, zipfile

def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def copy(source, target):
    if source.is_symlink() or not source.is_file():
        raise ValueError('NOT_REGULAR_FILE: ' + str(source))
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)

def tree(source, target):
    for path in sorted(source.rglob('*')):
        if path.is_file():
            copy(path, target / path.relative_to(source))

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('source','apk','predecessor','input-report','output'):
        parser.add_argument('--'+name, type=Path, required=True)
    parser.add_argument('--commit', required=True)
    args=parser.parse_args(); root=args.source.resolve()
    if not all((args.predecessor/name).is_dir() for name in ('05_PLANS','06_DATA_AND_ASSETS','02_COMPLETE_SOURCE/app/src/main/java/com/ethk3/novaonepro')):
        raise ValueError('PREDECESSOR_DELIVERY_INCOMPLETE')
    if not args.input_report.is_file() or args.input_report.stat().st_size == 0:
        raise ValueError('INPUT_REPORT_MISSING')
    tracked_changes=subprocess.check_output(['git','status','--porcelain','--untracked-files=no'],cwd=root)
    if tracked_changes.strip():
        raise ValueError('COMMIT_TRACKED_SOURCE_BEFORE_PACKAGING')
    release=json.loads((root/'reports/r943/RELEASE_RESULT.json').read_text())
    if sha(args.apk)!=release['apk_sha256']:
        raise ValueError('APK_HASH_MISMATCH')
    for item in json.loads((root/'reports/r943/APK_INPUT_MANIFEST.json').read_text()):
        path=root/item['path']
        if sha(path)!=item['sha256'] or path.stat().st_size!=item['bytes']:
            raise ValueError('SOURCE_CHANGED_AFTER_BUILD: '+item['path'])
    name='STARCORE_5.4.1.0.12_R943_FULL'; output=args.output.resolve()
    folder=output/name; folder.mkdir(parents=True, exist_ok=False)
    paths=subprocess.check_output(['git','ls-files','-z'],cwd=root).decode().split('\0')
    for relative in filter(None,paths):
        copy(root/relative,folder/'02_COMPLETE_SOURCE'/relative)
    copy(args.apk,folder/'01_APK/STARCORE_5.4.1.0.12_R943.apk')
    tree(root/'reports/r943',folder/'03_VERIFICATION')
    copy(root/'docs/5.4.2/R943_ATOMIC_WORLD_AND_RUNTIME_REPAIR_ZH.md',folder/'03_VERIFICATION/R943_UPGRADE_PLAN_ZH.md')
    tree(args.predecessor/'05_PLANS',folder/'04_ORIGINAL_PLANS')
    tree(args.predecessor/'06_DATA_AND_ASSETS',folder/'05_ASSETS_AND_INPUT')
    copy(args.input_report,folder/'05_ASSETS_AND_INPUT/R942_USER_REPORT.txt')
    tree(args.predecessor/'02_COMPLETE_SOURCE/app/src/main/java/com/ethk3/novaonepro',folder/'06_BUILD/predecessor_java')
    handoff={'repository':'gtt46461-star/eth-k3-v43-nova','branch':'agent/r943-training-storage-atomic-world-20260913',
             'commit':args.commit,'release':release,'source_files':len(list(filter(None,paths))),
             'original_input_parts':74,'physical_device_verified':False,'future_effect_proven':False}
    (folder/'HANDOFF.json').write_text(json.dumps(handoff,ensure_ascii=False,indent=2)+'\n')
    (folder/'先读_README.md').write_text('''# STARCORE 5.4.1.0.12 / R943

安装文件：`01_APK/STARCORE_5.4.1.0.12_R943.apk`。包名和签名与前版一致，版本号 577250943，可直接覆盖安装。希望保留旧学习数据时不要先卸载或清除数据。

`03_VERIFICATION/R943_UPGRADE_PLAN_ZH.md` 说明本次修复、原子世界与储存策略；`BATCH34_CURRENT_AUDIT.md` 对照第三第四批及加强项。完整当前源码在 `02_COMPLETE_SOURCE`，原方案在 `04_ORIGINAL_PLANS`，资产和输入报告在 `05_ASSETS_AND_INPUT`。

逐期数据上限 5000；大型重复明细保留 120 期；历史模型和累计学习保留。旧大库由后台分批清理，空间不足或回收等待会在完整报告“手机储存”章节显示。候选达 35% 仍须其他证据门通过才参与主链，不会预置虚假证书。

当前包通过的工程测试见验证文件。未在用户手机验证冷启动、持续后台、旧 10 GB 数据库回收或真实未来效果，不能把“交付完成”解释为“百分百效果验收通过”。

复现构建需要桌面 JDK 17、Python 3、bash、rg、zip/unzip，以及所有者签名工具包。运行 `python 02_COMPLETE_SOURCE/scripts/r943_rebuild_from_total.py --package-root . --signing-kit /你的签名工具包.zip --out /构建输出目录`。脚本校验原始 74 片归档，从中提取已有工具链，并用包内源码及前版资源重新构建。签名私钥不另外导出到交付目录。

`FILE_SHA256.json` 校验包内文件；外层 ZIP 的摘要在同目录 `.sha256` 文件。
''',encoding='utf-8')
    inventory=[{'path':p.relative_to(folder).as_posix(),'bytes':p.stat().st_size,'sha256':sha(p)}
               for p in sorted(folder.rglob('*')) if p.is_file()]
    (folder/'FILE_SHA256.json').write_text(json.dumps(inventory,ensure_ascii=False,indent=2)+'\n')
    archive=output/(name+'.zip')
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
        for path in sorted(folder.rglob('*')):
            if path.is_file():z.write(path, name+'/'+path.relative_to(folder).as_posix())
    with zipfile.ZipFile(archive) as z:
        if z.testzip() is not None:raise ValueError('ZIP_CRC_FAILURE')
        for item in inventory:
            with z.open(name+'/'+item['path']) as stream:
                if hashlib.file_digest(stream,'sha256').hexdigest()!=item['sha256']:
                    raise ValueError('ZIP_CONTENT_HASH_FAILURE: '+item['path'])
    digest=sha(archive);(output/(name+'.zip.sha256')).write_text(digest+'  '+archive.name+'\n')
    print(json.dumps({'zip':str(archive),'bytes':archive.stat().st_size,'sha256':digest,
                      'verified_files':len(inventory),'zip_integrity':'PASS'},ensure_ascii=False),flush=True)

if __name__=='__main__':main()
