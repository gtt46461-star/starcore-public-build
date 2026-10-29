#!/usr/bin/env python3
"""Inspect actual signed APK bytes; never substitute a claimed hash or signing identity."""
import hashlib, json, os, re, shutil, subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[2]; os.chdir(root)
out=root/'r29-apk'; out.mkdir(exist_ok=True)
source=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
tree=subprocess.check_output(['git','rev-parse','HEAD^{tree}'],text=True).strip()
if subprocess.check_output(['git','status','--porcelain','--untracked-files=no'],text=True).strip(): raise SystemExit('DIRTY_SOURCE_NO_DELIVERY')
tool=Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0'
rows=[]
for variant in ['release','debug']:
    for apk in (root/f'app/build/outputs/apk/{variant}').glob('*.apk'):
        signature=subprocess.check_output([str(tool/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True)
        cert=re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})',signature).group(1)
        package=subprocess.check_output([str(tool/'aapt'),'dump','badging',str(apk)],text=True).splitlines()[0]
        if "name='com.starcore.research.r13'" not in package: raise SystemExit('WRONG_INSTALL_IDENTITY')
        if cert!=os.environ['R13_EXPECTED_CERT_SHA256']: raise SystemExit('WRONG_SIGNING_IDENTITY')
        name=f'STARCORE-R29.1-{source[:12]}-{variant}.apk'; shutil.copy2(apk,out/name)
        row={'apk':name,'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'package':'com.starcore.research.r13','versionCode':int(re.search(r"versionCode='(\d+)'",package).group(1)),'versionName':re.search(r"versionName='([^']+)'",package).group(1),'certificateSha256':cert,'sourceCommit':source,'sourceTree':tree,'sourceArchive':f'https://github.com/gtt46461-star/starcore-public-build/archive/{source}.zip','signingIdentity':os.environ['R29_SIGNING'],'officialR13Upgrade':'NOT_RUN' if os.environ['R29_SIGNING']=='OFFICIAL_R13' else 'BLOCKED_SIGNING_KEY','installation':'NOT_RUN','startup':'NOT_RUN','physicalDevice':'NOT_RUN','naturalQuality':'INSUFFICIENT','overall':'IMPLEMENTATION_IN_PROGRESS'}
        rows.append(row)
(out/'delivery.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
(out/'SHA256SUMS.txt').write_text(''.join(r['sha256']+'  '+r['apk']+'\n' for r in rows))
print(json.dumps(rows,ensure_ascii=False))
