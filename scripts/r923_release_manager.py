#!/usr/bin/env python3
"""Stage, independently rebuild and verify a complete R923 delivery using actual source files."""
import argparse,copy,hashlib,io,json,os,re,shutil,subprocess,tarfile,tempfile,zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
NAME='STARCORE_TIANSHU_V57.20.9_R5.3.9_923_COMPLETE'
APK='Starcore-Tianshu-V57.20.9-R5.3.9-923-release.apk'
OLDAPK='Starcore-Tianshu-V57.20.8-R5.3.8-922-release.apk'
OLDZIP='STARCORE_TIANSHU_V57.20.8_R5.3.8_922_FINAL_FULL_SOURCE_TOOLCHAIN.zip'
R920ZIP='STARCORE_TIANSHU_V57.20.6_R5.3.6_920_FINAL_FULL_SOURCE_EXPANDED.zip'
R922SHA='4b5bea48196000ff41bed9a3b544df9b744d3ec8652dca9511d685ab4aae77cf'
R920SHA='4da4395b1144c015f4da1b566c95512071ac8270dbf739fc20b865f87eda46c8'

def sha(p):
    with Path(p).open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def git(*a):return subprocess.check_output(['git',*a],cwd=ROOT)
def cp(src,dest):
    dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dest)
def nested(handle):
    count=1
    with zipfile.ZipFile(handle) as z:
        assert z.testzip() is None
        for e in z.infolist():
            if not e.filename.lower().endswith(('.zip','.apk')):continue
            with tempfile.TemporaryFile() as f,z.open(e) as s:
                shutil.copyfileobj(s,f);f.seek(0)
                try:count+=nested(f)
                except Exception as err:raise RuntimeError('NESTED_ARCHIVE:'+e.filename) from err
    return count

def repack(old,out,replace_name,replacement,note):
    rows=[]
    with zipfile.ZipFile(old) as src,zipfile.ZipFile(out,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as dst:
        prefix=src.namelist()[0].split('/')[0]+'/'
        metadata={'PACKAGE_MANIFEST.md','PACKAGE_FILE_INVENTORY.tsv','SHA256SUMS.txt'}
        def write(info,stream):
            h=hashlib.sha256();size=0
            with dst.open(info,'w',force_zip64=True) as target:
                while True:
                    b=stream.read(1048576)
                    if not b:break
                    target.write(b);h.update(b);size+=len(b)
            if not info.is_dir():rows.append((info.filename[len(prefix):],size,h.hexdigest()))
        def text(name,value):
            info=zipfile.ZipInfo(prefix+name);info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16
            write(info,io.BytesIO(value.encode()))
        for e in src.infolist():
            info=copy.copy(e)
            if e.filename[len(prefix):] in metadata:info.filename=prefix+'00_README_AND_MANIFEST/R923_ORIGINAL_'+e.filename[len(prefix):]
            if e.filename==replace_name:
                with replacement.open('rb') as f:write(info,f)
            else:
                with src.open(e) as f:write(info,f)
        text('PACKAGE_MANIFEST.md','# 历史归档恢复\n'+note+'\n原元数据另存为 R923_ORIGINAL_*。所有 APK、源码、Git、工具载荷保持原字节；只有损坏嵌套 ZIP 和校验元数据更新。\n')
        text('PACKAGE_FILE_INVENTORY.tsv','path\tbytes\tsha256\n'+''.join(f'{n}\t{s}\t{h}\n' for n,s,h in sorted(rows)))
        text('SHA256SUMS.txt',''.join(f'{h}  {n}\n' for n,s,h in sorted(rows)))
    with zipfile.ZipFile(out) as dst,zipfile.ZipFile(old) as src:
        assert dst.testzip() is None
        for e in src.infolist():
            if e.is_dir() or e.filename==replace_name or e.filename[len(prefix):] in metadata:continue
            with src.open(e) as a,dst.open(e.filename) as b:
                assert hashlib.file_digest(a,'sha256').digest()==hashlib.file_digest(b,'sha256').digest(),e.filename
        for line in dst.read(prefix+'SHA256SUMS.txt').decode().splitlines():
            h,n=line.split('  ',1)
            with dst.open(prefix+n) as f:assert hashlib.file_digest(f,'sha256').hexdigest()==h,n
    return {'bytes':out.stat().st_size,'sha256':sha(out),'files':len(rows)}

def restore(inputs,dest):
    old=inputs/OLDZIP;complete=inputs/R920ZIP
    assert old.stat().st_size==313343916 and sha(old)==R922SHA
    assert complete.stat().st_size==43149586 and sha(complete)==R920SHA
    assert nested(complete)==5
    with tempfile.TemporaryDirectory(prefix='r923-restore-') as tmp:
        tmp=Path(tmp);r921=tmp/'r921.zip';fixed=tmp/'r921_fixed.zip'
        with zipfile.ZipFile(old) as z:
            assert z.testzip() is None
            n921=next(n for n in z.namelist() if n.endswith('921_FINAL_FULL_SOURCE_TOOLCHAIN.zip'))
            with z.open(n921) as a,r921.open('wb') as b:shutil.copyfileobj(a,b)
        with zipfile.ZipFile(r921) as z:
            n920=next(n for n in z.namelist() if n.endswith('920_FINAL_FULL_SOURCE_EXPANDED.zip'));partial=z.read(n920)
            with complete.open('rb') as f:assert partial==f.read(len(partial))
        note=f'R920 ZIP 从 {len(partial)} 字节恢复到 43149586 字节，精确前缀一致。原 R922 SHA256={R922SHA}；完整 R920 SHA256={R920SHA}。'
        result={'input_r922_sha256':R922SHA,'r920_sha256':R920SHA,'partial_bytes':len(partial),'full_bytes':43149586,'exact_prefix_match':True}
        result['r921']=repack(r921,fixed,n920,complete,note)
        output=dest/'STARCORE_R922_ROLLBACK_CHAIN_RESTORED_R923.zip'
        result['r922']=repack(old,output,n921,fixed,note)
        result['nested_zip_apk_crc_pass']=nested(output)
        result['unchanged_payloads']='SHA256_VERIFIED'
        (dest/'ARCHIVE_REPAIR_PROVENANCE_R923.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
        print('R923_ROLLBACK_CHAIN_RESTORE=PASS|NESTED='+str(result['nested_zip_apk_crc_pass']),flush=True)

def metrics():
    base='997457c';diff=git('diff','--unified=0',base).decode();lines=diff.splitlines()
    added=[s[1:] for s in lines if s.startswith('+') and not s.startswith('+++')]
    deleted=[s[1:] for s in lines if s.startswith('-') and not s.startswith('---')]
    files=git('diff','--name-only',base).decode().splitlines();java=list((ROOT/'app/src/main/java').rglob('*.java'))
    return {'head':git('rev-parse','HEAD').decode().strip(),'base':git('rev-parse',base).decode().strip(),
            'java_files':len(java),'java_lines':sum(len(p.read_text().splitlines()) for p in java),
            'java_unicode_characters':sum(len(p.read_text()) for p in java),'changed_files':len(files),
            'changed_java_files':sum(p.endswith('.java') for p in files),'added_lines':len(added),'removed_lines':len(deleted),
            'added_line_characters':sum(map(len,added)),'removed_line_characters':sum(map(len,deleted)),
            'scope':'Static file/change counts are not proof of universal correctness or win rate.'}

def stage(a):
    assert not git('status','--porcelain').strip(),'SOURCE_NOT_CLEAN'
    d=a.package;assert not d.exists(),'PACKAGE_ALREADY_EXISTS'
    for n in ('00_README_AND_MANIFEST','01_SIGNED_OVERWRITE_APK','02_COMPLETE_SOURCE_EXPANDED','03_SOURCE_AND_GIT_BUNDLE','04_VERIFICATION','05_REPORTS_AND_SPEC','06_SIGNING_PRIVATE_USER_ONLY','07_ROLLBACK_R922','08_BUILD_SCRIPTS','09_TOOLCHAIN_USED','10_INPUT_EVIDENCE','11_DEVICE_DATABASE_HANDOFF'):(d/n).mkdir(parents=True)
    with tarfile.open(fileobj=io.BytesIO(git('archive','--format=tar','HEAD'))) as t:t.extractall(d/'02_COMPLETE_SOURCE_EXPANDED',filter='data')
    subprocess.run(['git','archive','--format=zip','--output='+str(d/'03_SOURCE_AND_GIT_BUNDLE/R923_COMPLETE_SOURCE.zip'),'HEAD'],cwd=ROOT,check=True)
    subprocess.run(['git','bundle','create',str(d/'03_SOURCE_AND_GIT_BUNDLE/R923_ALL_HISTORY.bundle'),'--all'],cwd=ROOT,check=True)
    (d/'03_SOURCE_AND_GIT_BUNDLE/SOURCE_COMMIT.txt').write_text(git('rev-parse','HEAD').decode())
    cp(ROOT/'build/r923-release'/APK,d/'01_SIGNED_OVERWRITE_APK'/APK)
    for p in (ROOT/'build/r923-release').glob('*.idsig'):cp(p,d/'01_SIGNED_OVERWRITE_APK'/p.name)
    shutil.copytree(ROOT/'build/r923-verification',d/'04_VERIFICATION/host',dirs_exist_ok=True)
    shutil.copytree(ROOT/'build/r923-release/evidence',d/'04_VERIFICATION/apk',dirs_exist_ok=True)
    cp(ROOT/'release/R923_RELEASE_AND_ACCEPTANCE_ZH.md',d/'00_README_AND_MANIFEST/R923_RELEASE_AND_ACCEPTANCE_ZH.md')
    (d/'00_README_AND_MANIFEST/R923_CHANGE_METRICS.json').write_text(json.dumps(metrics(),ensure_ascii=False,indent=2)+'\n')
    (d/'04_VERIFICATION/R923_FULL_SOURCE_CHANGE.patch').write_bytes(git('diff','997457c'))
    shutil.copytree(a.baseline/'05_REPORTS_AND_SPEC',d/'05_REPORTS_AND_SPEC/R922_original',dirs_exist_ok=True)
    shutil.copytree(a.baseline/'06_SIGNING_PRIVATE_USER_ONLY',d/'06_SIGNING_PRIVATE_USER_ONLY',dirs_exist_ok=True)
    cp(a.baseline/'01_SIGNED_OVERWRITE_APK'/OLDAPK,d/'07_ROLLBACK_R922'/OLDAPK)
    restore(a.inputs,d/'07_ROLLBACK_R922')
    for p in (ROOT/'scripts').glob('r923_*'):
        if p.is_file():cp(p,d/'08_BUILD_SCRIPTS'/p.name)
    shutil.copytree(a.baseline/'09_TOOLCHAIN_USED',d/'09_TOOLCHAIN_USED',dirs_exist_ok=True)
    shutil.copytree(Path('/usr/lib/jvm/java-17-openjdk-amd64'),d/'09_TOOLCHAIN_USED/jdk17',symlinks=False)
    for p in (a.workspace/'upload').iterdir():
        if p.is_file():cp(p,d/'10_INPUT_EVIDENCE'/p.name)
    p=a.workspace/'docx_render/report.txt';cp(p,d/'10_INPUT_EVIDENCE/USER_REPORT_FULL_TEXT.txt')
    report=p.read_text();pat=re.compile(r'FAIL|READ_ERROR|NO_PREDRAW|SOURCE_NOT_REALTIME|PRUNED|YIELD_P0|P95')
    hits=[f'{i}\t{s}' for i,s in enumerate(report.splitlines(),1) if pat.search(s)]
    (d/'10_INPUT_EVIDENCE/REPORT_REVIEW_INDEX.tsv').write_text('line\ttext\n'+'\n'.join(hits)+'\n')
    (d/'10_INPUT_EVIDENCE/REPORT_REVIEW_SCOPE.txt').write_text(f'全文字符={len(report)}；全文行={len(report.splitlines())}；索引行={len(hits)}。索引仅定位，规则描述和合法零值不自动判为缺陷。\n')
    (d/'11_DEVICE_DATABASE_HANDOFF/README.txt').write_text('本次没有收到手机运行数据库、WAL或运行模型快照，不能伪造这些数据。请覆盖安装，勿卸载或清除应用数据。完整诊断资产清单不是全量历史 SQLite 备份。\n')
    (d/'README_R923_FIRST.txt').write_text('R923 完整工程总包；先读 00_README_AND_MANIFEST/R923_RELEASE_AND_ACCEPTANCE_ZH.md。\n01签名APK，02完整源码，03完整Git历史，04检查证据与完整diff，05规范，06本人签名材料，07修复后的完整回退链，08构建脚本，09工具链和JDK17，10原始报告，11数据库说明。\n反向重建：python3 08_BUILD_SCRIPTS/r923_release_manager.py rebuild --package .\n主机需 Linux x86_64、Python3.12+、bash、git、tar、zip/unzip、rg 和系统动态库。JDK与Android工具已包含。\n不凑体积，不虚构未来高胜率或真实频率15%已达成；手机连续72小时后台尚未实测。\n')
    print('R923_STAGE=PASS|PATH='+str(d),flush=True)

def rebuild(a):
    d=a.package;out=d/'04_VERIFICATION/reverse_rebuild';out.mkdir(parents=True,exist_ok=True)
    tc=d/'09_TOOLCHAIN_USED';env=dict(os.environ,JAVA_HOME=str(tc/'jdk17'),STARCORE_TOOLCHAIN_ROOT=str(tc),PATH=str(tc/'jdk17/bin')+':'+os.environ['PATH'])
    with tempfile.TemporaryDirectory(prefix='r923-rebuild-') as td:
        r=Path(td)/'repo'
        with (out/'clone.log').open('w') as log:subprocess.run(['git','clone',str(d/'03_SOURCE_AND_GIT_BUNDLE/R923_ALL_HISTORY.bundle'),str(r)],stdout=log,stderr=subprocess.STDOUT,check=True)
        with (out/'behavior.log').open('w') as log:subprocess.run(['bash','scripts/r923_behavior_gate.sh'],cwd=r,env=dict(env,R923_EVIDENCE_DIR=str(out/'host/evidence'),R923_SOURCE_AUDIT_DIR=str(out/'host/source-audit')),stdout=log,stderr=subprocess.STDOUT,check=True)
        with (out/'build.log').open('w') as log:subprocess.run(['python3','scripts/r923_build_from_signing_kit.py','--base-apk',str(d/'07_ROLLBACK_R922'/OLDAPK),'--signing-kit',str(d/'06_SIGNING_PRIVATE_USER_ONLY/STARCORE-V57-release-signing-kit.zip'),'--toolchain',str(tc),'--out',str(out/'rebuilt')],cwd=r,env=env,stdout=log,stderr=subprocess.STDOUT,check=True)
    lines=[]
    with zipfile.ZipFile(d/'01_SIGNED_OVERWRITE_APK'/APK) as x,zipfile.ZipFile(out/'rebuilt'/APK) as y:
        dex=lambda z:sorted(n for n in z.namelist() if re.fullmatch(r'classes\d*\.dex',n))
        assert dex(x)==dex(y)
        for n in ['AndroidManifest.xml','resources.arsc']+dex(x):
            assert x.read(n)==y.read(n),n;lines.append(n+' SHA256='+hashlib.sha256(x.read(n)).hexdigest()+' MATCH=TRUE')
    lines.append('R923_REVERSE_REBUILD=PASS|SOURCE=PACKAGED_BUNDLE|JDK=PACKAGED_JDK17|PHYSICAL_DEVICE=NOT_TESTED')
    (out/'R923_REVERSE_REBUILD.txt').write_text('\n'.join(lines)+'\n');print('\n'.join(lines),flush=True)

def finish(a):
    d=a.package;assert 'R923_REVERSE_REBUILD=PASS' in (d/'04_VERIFICATION/reverse_rebuild/R923_REVERSE_REBUILD.txt').read_text()
    source=d/'02_COMPLETE_SOURCE_EXPANDED';count=0
    with zipfile.ZipFile(d/'03_SOURCE_AND_GIT_BUNDLE/R923_COMPLETE_SOURCE.zip') as z:
        expected={e.filename for e in z.infolist() if not e.is_dir()}
        assert {p.relative_to(source).as_posix() for p in source.rglob('*') if p.is_file()}==expected
        for n in expected:
            with z.open(n) as f:assert hashlib.file_digest(f,'sha256').hexdigest()==sha(source/n),n
            count+=1
    java=list((source/'app/src/main/java').rglob('*.java'));manifest=next(p for p in java if p.name=='SourceManifestR815.java').read_text()
    def digest(paths):return hashlib.sha256(''.join('PATH='+p.relative_to(source).as_posix()+'\nSHA256='+sha(p)+'\n' for p in sorted(paths,key=lambda p:p.as_posix())).encode()).hexdigest()
    assert digest([p for p in java if p.name!='SourceManifestR815.java']) in manifest
    assert digest([source/'app/src/main/AndroidManifest.xml']+[p for p in (source/'app/src/main/res').rglob('*') if p.is_file()]) in manifest
    rollback=d/'07_ROLLBACK_R922/STARCORE_R922_ROLLBACK_CHAIN_RESTORED_R923.zip'
    proof=json.loads((d/'07_ROLLBACK_R922/ARCHIVE_REPAIR_PROVENANCE_R923.json').read_text())
    assert sha(rollback)==proof['r922']['sha256'] and proof['exact_prefix_match'];archives=nested(rollback)
    (d/'04_VERIFICATION/R923_FINAL_PRECHECK.json').write_text(json.dumps({'source_files':count,'java_files':len(java),'source_parity':'PASS','rollback_nested_zip_apk_crc':archives,'reverse_build':'PASS','physical_device':'NOT_TESTED'},indent=2)+'\n')
    rows=[];exclude={'PACKAGE_FILE_INVENTORY.tsv','SHA256SUMS.txt'}
    for p in sorted(d.rglob('*')):
        if p.is_file() and p.relative_to(d).as_posix() not in exclude:
            assert not p.is_symlink();rows.append((p.relative_to(d).as_posix(),p.stat().st_size,sha(p)))
    inv=d/'PACKAGE_FILE_INVENTORY.tsv';inv.write_text('path\tbytes\tsha256\n'+''.join(f'{n}\t{s}\t{h}\n' for n,s,h in rows));rows.append((inv.name,inv.stat().st_size,sha(inv)))
    sums=d/'SHA256SUMS.txt';sums.write_text(''.join(f'{h}  {n}\n' for n,s,h in rows));rows.append((sums.name,sums.stat().st_size,sha(sums)))
    out=d.parent/(d.name+'.zip');tmp=out.with_name(out.name+'.partial')
    with zipfile.ZipFile(tmp,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
        for n,s,h in rows:z.write(d/n,d.name+'/'+n)
    with zipfile.ZipFile(tmp) as z:
        assert z.testzip() is None and len(z.infolist())==len(rows)
        for n,s,h in rows:
            assert z.getinfo(d.name+'/'+n).file_size==s
            with z.open(d.name+'/'+n) as f:assert hashlib.file_digest(f,'sha256').hexdigest()==h,n
    tmp.replace(out);checksum=out.with_name(out.name+'.sha256');checksum.write_text(sha(out)+'  '+out.name+'\n')
    result={'path':str(out),'bytes':out.stat().st_size,'MB':round(out.stat().st_size/1e6,2),'sha256':sha(out),'files':len(rows),'checksum_file':str(checksum),'crc':'PASS','all_entry_hashes':'PASS'}
    (d.parent/'R923_DELIVERY_INFO.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result),flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['stage','rebuild','finish']);p.add_argument('--package',type=Path,required=True)
    for n in ('baseline','inputs','workspace'):p.add_argument('--'+n,type=Path)
    a=p.parse_args();a.package=a.package.resolve()
    for n in ('baseline','inputs','workspace'):
        if getattr(a,n) is not None:setattr(a,n,getattr(a,n).resolve())
    {'stage':stage,'rebuild':rebuild,'finish':finish}[a.action](a)
