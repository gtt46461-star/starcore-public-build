#!/usr/bin/env python3
"""Verify source provenance, final APK identity/signature and freshly built DEX bytes.
This is artifact verification, NOT device installation, Android UI or accuracy testing.
Run before cleaning the private build scratch; no credentials are read or emitted.
"""
from pathlib import Path
import argparse, hashlib, json, re, subprocess, sys, zipfile
ROOT = Path(__file__).resolve().parents[2]

def sha(path):
    h=hashlib.sha256()
    with Path(path).open('rb') as f:
        for block in iter(lambda:f.read(1024*1024), b''): h.update(block)
    return h.hexdigest()

def tree(files):
    h=hashlib.sha256()
    # Same bytewise path order and record format as r815_generate_provenance.sh.
    for p in sorted(files,key=lambda f:str(f.relative_to(ROOT))):
        h.update(('PATH='+str(p.relative_to(ROOT))+'\nSHA256='+sha(p)+'\n').encode())
    return h.hexdigest()

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--apk',type=Path,required=True)
    p.add_argument('--base-apk',type=Path,required=True)
    p.add_argument('--dex-dir',type=Path,required=True)
    p.add_argument('--toolchain',type=Path,required=True)
    p.add_argument('--out',type=Path,required=True)
    a=p.parse_args(); results=[]
    def check(label,ok,detail):
        results.append(dict(name=label,status='PASS' if ok else 'FAIL',detail=detail))
        print(results[-1]['status'],label)
    def command(args):
        run=subprocess.run(list(map(str,args)),capture_output=True,text=True,check=False)
        if run.returncode:raise RuntimeError('Command failed: '+str(args)+'\n'+run.stderr)
        return run.stdout
    manifest=json.loads((ROOT/'release/release_manifest.json').read_text())
    source=(ROOT/'app/src/main/java/com/ethk3/novaonepro/SourceManifestR815.java').read_text()
    fields=dict(re.findall(r'public static final String (\w+) = "([^"]+)";',source))
    java=[f for f in (ROOT/'app/src/main/java').rglob('*.java') if f.name!='SourceManifestR815.java']
    resource=[ROOT/'app/src/main/AndroidManifest.xml']+[f for f in (ROOT/'app/src/main/res').rglob('*') if f.is_file()]
    check('FINAL_SOURCE_MANIFEST_JAVA',tree(java)==fields['SOURCE_TREE_SHA256'],dict(count=len(java),sha256=tree(java)))
    check('FINAL_SOURCE_MANIFEST_RESOURCES',tree(resource)==fields['RESOURCE_MANIFEST_SHA256'],dict(count=len(resource),sha256=tree(resource)))
    aapt=a.toolchain/'aapt/bin/x64/linux/aapt2';signer=a.toolchain/'android-signing/apksigner'
    def package(apk):
        text=command([aapt,'dump','badging',apk]);header=re.search(r'^package: (.+)$',text,re.M)
        if not header:raise ValueError('Missing APK package header')
        return dict(re.findall(r"(\w+)='([^']*)'",header[1]))
    old,new=package(a.base_apk),package(a.apk)
    check('SAME_APPLICATION_ID',old['name']==new['name']==manifest['applicationId'],dict(base=old,final=new))
    check('INCREASING_VERSION_EXACT_RELEASE_IDENTITY',int(new['versionCode'])>int(old['versionCode']) and int(new['versionCode'])==manifest['versionCode'] and new['versionName']==manifest['versionName'],new)
    def certificate(apk):
        text=command([signer,'verify','--verbose','--print-certs',apk])
        digest=sorted(set(re.findall(r'^(?:Signer #\d+|V[\d.]+ Signer):? certificate SHA-256 digest: ([0-9a-fA-F]{64})$',text,re.M)))
        if not digest:raise ValueError('Verified signer certificate missing')
        return sorted(digest),text
    oldcert,oldtext=certificate(a.base_apk);newcert,newtext=certificate(a.apk)
    check('VERIFIED_SIGNATURE_SAME_OWNER_CERTIFICATE',newcert==oldcert,dict(base=oldcert,final=newcert))
    with zipfile.ZipFile(a.apk) as z:
        entries=sorted(n for n in z.namelist() if re.fullmatch(r'classes(?:\d+)?\.dex',n))
        fresh=sorted(p.name for p in a.dex_dir.glob('classes*.dex'))
        check('NO_PREDECESSOR_OR_EXTRA_DEX',bool(entries) and entries==fresh,dict(apk=entries,fresh=fresh))
        dex=[]
        for name in entries:
            inside=hashlib.sha256(z.read(name)).hexdigest();path=a.dex_dir/name
            d=dict(entry=name,apk_sha256=inside,fresh_d8_sha256=sha(path) if path.is_file() else None)
            check('FRESH_D8_BYTES_'+name,d['apk_sha256']==d['fresh_d8_sha256'],d);dex.append(d)
        check('ZIP_INTEGRITY',z.testzip() is None,dict(entries=len(z.namelist())))
    git=lambda *args:command(['git','-C',ROOT,*args]).strip()
    report=dict(scope='ARTIFACT_ONLY_NOT_DEVICE_RUNTIME',checks=results,apk_sha256=sha(a.apk),
                base_apk_sha256=sha(a.base_apk),git_commit=git('rev-parse','HEAD'),branch=git('branch','--show-current'),
                worktree_status=git('status','--porcelain'),source_manifest=fields,release=manifest,
                signature_verifier_stdout=newtext,physical_overwrite_install='NOT RUN',live_accuracy='NOT RUN')
    a.out.parent.mkdir(parents=True,exist_ok=True);a.out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    if any(x['status']!='PASS' for x in results):raise SystemExit(1)
if __name__=='__main__':main()
