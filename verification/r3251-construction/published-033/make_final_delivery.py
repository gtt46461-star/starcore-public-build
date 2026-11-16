#!/usr/bin/env python3
"""Build the frozen public ZIP with the exact already-verified signed APK."""
import hashlib,json,subprocess,zipfile
from pathlib import Path
REPO=Path('/workspace/starcore-r3251-attachment-final')
EVIDENCE=REPO/'verification/r3251-construction/final-delivery-033'
DELIVERY=Path('/workspace/starcore-r3251-final-apk-delivery')
BUILD='bb7b49bbcc9266bad548ec37e71403256fc06a4d'
EVIDENCE_COMMIT='85bdfc606dddb3bcb55e070e54c1c31379436ba4'
APK='STARCORE-R32.5.1-C1-033-release.apk'
ZIP='STARCORE-R32.5.1-C1-033-APK-AND-EVIDENCE.zip'
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def jbytes(d):return (json.dumps(d,ensure_ascii=False,indent=2)+'\n').encode()

def main():
 assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=REPO,text=True).strip()==EVIDENCE_COMMIT
 assert not subprocess.check_output(['git','status','--porcelain'],cwd=REPO,text=True).strip()
 assert not subprocess.check_output(['git','diff','--name-only',BUILD,'HEAD','--','app','tools','scripts','gradle','build.gradle','settings.gradle','gradle.properties'],cwd=REPO,text=True).strip()
 for x in json.loads((EVIDENCE/'ARTIFACT_SHA256.json').read_text()):
  p=EVIDENCE/x['path'];assert p.is_file() and p.stat().st_size==x['bytes'] and sha(p)==x['sha256'],p
 apk=DELIVERY/APK;assert apk.stat().st_size==28909143 and sha(apk)=='8f0cb6e6eadbce5787cffde310a6cbf204d43962e636fd9ebedf6e66556fb763'
 status=json.loads((EVIDENCE/'DELIVERY_STATUS.json').read_text());assert status['ciConclusion']=='success' and status['ciAttempt']==2
 internal=dict(status,frozenEvidenceCommit=EVIDENCE_COMMIT,apkFile=APK,sourceUrl='https://github.com/gtt46461-star/starcore-public-build/commit/'+BUILD,evidenceUrl='https://github.com/gtt46461-star/starcore-public-build/tree/'+EVIDENCE_COMMIT+'/verification/r3251-construction/final-delivery-033',zipContentHashScope='Every member except ZIP_CONTENTS_SHA256.json itself; outer SHA256SUMS covers complete ZIP.')
 readme='''R32.5.1-C1 033 正式签名成品\n\nAPK 为原032同包同签、versionCode577311033。系统覆盖安装并保留应用数据。实际API34原032→033覆盖及同版重装、旧draws行/UID/SQLite完整性保全通过；不冒充原用户大数据库验证。REAL未开启。\n\n五批源码工程检查通过：3291项单元测试零失败零跳过、55次分批HOST执行、编译/测试编译/Lint通过。Android29、34和35的原完整矩阵流程通过；35首轮一次未来冻结失败，原组及完整流程复验均通过，原失败保留且根因未被武断宣称消除。正式APK是Release，不是Debug。\n\n本ZIP内APK与独立下载APK完全同字节。DELIVERY.json记录精确APK源码bb7b49bbcc9266bad548ec37e71403256fc06a4d及冻结证据提交。ZIP_CONTENTS_SHA256.json逐个核对成员。\n\n222条逐ID账本不以组级PASS代签完整条款；未命中的F01-F10 Android联合格、未审阅完整语义、原用户032 DB/WAL/SHM与556checkpoint、真机24/72小时及独立自然质量均保留缺项。165对象的源码存在不等于已证实独立自然效果。不称全部业务验收完成。\n\n源代码已在GitHub独立施工分支推送；完整权威源码按上述bb7b49bb提交取得。source/BUILD_INPUTS_FROM_EXACT_BASELINE.patch在evidence目录内供审阅。另附独立源码bundle，以7424eeae6354eb6599dd8498e123c059bd410c90为前置，不内嵌本ZIP。签名私钥、口令、API值和原始应用数据库均不在本包。\n'''.encode()
 target=DELIVERY/ZIP;assert not target.exists(),target
 contents=[]
 with zipfile.ZipFile(target,'x',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
  z.write(apk,APK);contents.append({'path':APK,'bytes':apk.stat().st_size,'sha256':sha(apk)})
  for p in sorted(EVIDENCE.rglob('*')):
   if not p.is_file():continue
   name='evidence/'+p.relative_to(EVIDENCE).as_posix();z.write(p,name);contents.append({'path':name,'bytes':p.stat().st_size,'sha256':sha(p)})
  for name,raw in [('README.txt',readme),('DELIVERY.json',jbytes(internal))]:
   z.writestr(name,raw);contents.append({'path':name,'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()})
  z.writestr('ZIP_CONTENTS_SHA256.json',jbytes({'scope':'All members except this manifest itself','files':contents}))
 assert target.stat().st_size<100*1024*1024,'PUBLIC_GIT_FILE_LIMIT'
 with zipfile.ZipFile(target) as z:
  assert z.testzip() is None
  assert len(z.namelist())==len(set(z.namelist()))==len(contents)+1
  assert [n for n in z.namelist() if n.lower().endswith('.apk')]==[APK]
  for x in contents:
   raw=z.read(x['path']);assert len(raw)==x['bytes'] and hashlib.sha256(raw).hexdigest()==x['sha256'],x['path']
 internal.update(zipFile=ZIP,zipSha256=sha(target),zipBytes=target.stat().st_size,zipApkSameBytes=True,zipCrc='PASS',zipMembers=len(contents)+1,publicDownloadRequiresLogin=False,sourceBundle={'file':'STARCORE-R3251-C1-bb7b49bb-source.bundle','bytes':61967147,'sha256':'f773870a34d7a653d48c04b3ac301b2090f46cd639983b9bedb72fdd4d4b300d','fourEngineeringEvidenceRefsFrozenBeforeConsolidatedLedger':True,'baselinePrerequisite':'7424eeae6354eb6599dd8498e123c059bd410c90'})
 (DELIVERY/'DELIVERY.json').write_bytes(jbytes(internal))
 (DELIVERY/'README.md').write_text(readme.decode()+'\n[下载 APK](./'+APK+') · [下载同字节 APK 与证据 ZIP](./'+ZIP+') · [SHA-256](./SHA256SUMS.txt)\n')
 names=[APK,ZIP,'STARCORE-R3251-C1-bb7b49bb-source.bundle','DELIVERY.json']
 (DELIVERY/'SHA256SUMS.txt').write_text(''.join(sha(DELIVERY/n)+'  '+n+'\n' for n in names))
 print(json.dumps({k:internal[k] for k in ['buildSource','frozenEvidenceCommit','apkSha256','zipFile','zipSha256','zipBytes','zipApkSameBytes','zipCrc','zipMembers']},indent=2))

if __name__=='__main__':main()
