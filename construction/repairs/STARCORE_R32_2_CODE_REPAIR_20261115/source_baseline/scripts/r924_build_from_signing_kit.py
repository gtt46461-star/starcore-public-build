#!/usr/bin/env python3
"""Same-owner cover-compatible build; private credentials stay outside source and output."""
import argparse,os,re,subprocess,tempfile,zipfile
from pathlib import Path
def main():
    p=argparse.ArgumentParser()
    for n in ('base-apk','signing-kit','toolchain','out'):p.add_argument('--'+n,type=Path,required=True)
    p.add_argument('--certificate-base-apk',type=Path)
    a=p.parse_args();root=Path(__file__).resolve().parents[1]
    with zipfile.ZipFile(a.signing_kit) as z,tempfile.TemporaryDirectory(prefix='r924-key-') as td:
        names=[n for n in z.namelist() if n.endswith('.p12')];assert len(names)==1
        key=Path(td)/'owner.p12';key.write_bytes(z.read(names[0]));key.chmod(0o600)
        text=z.read('SIGNING-CREDENTIALS.txt').decode('utf-8-sig')
        def field(n):
            m=re.search(r'^\s*'+re.escape(n)+r'\s*[:=]\s*(.+?)\s*$',text,re.M|re.I)
            if not m:raise RuntimeError('SIGNING_FIELD_MISSING_'+n)
            return m.group(1)
        env=dict(os.environ,STARCORE_TOOLCHAIN_ROOT=str(a.toolchain.resolve()),STARCORE_SIGNING_P12=str(key),STARCORE_SIGNING_PASSWORD=field('Password'),STARCORE_SIGNING_ALIAS=field('Alias'),STARCORE_SIGNING_TYPE=field('Keystore type'))
        if a.certificate_base_apk:env['STARCORE_CERTIFICATE_BASE_APK']=str(a.certificate_base_apk.resolve())
        a.out.mkdir(parents=True,exist_ok=True)
        subprocess.run(['bash','scripts/r924_build_release.sh',str(a.base_apk.resolve()),str(a.out.resolve())],cwd=root,env=env,check=True)
        apk=a.out.resolve()/'Starcore-Tianshu-V57.21.0-R5.4.0-924-release.apk'
        subprocess.run(['bash','scripts/r924_apk_acceptance.sh',str(apk),str((a.certificate_base_apk or a.base_apk).resolve())],cwd=root,env=dict(env,R924_EVIDENCE_DIR=str(a.out.resolve()/'evidence')),check=True)
if __name__=='__main__':main()
