"""CI only: publish the exact R27-signed candidate after all dependent jobs succeeded."""
import argparse,hashlib,json,os,pathlib,re,subprocess,zipfile
p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--candidate',type=pathlib.Path,required=True);p.add_argument('--android',type=pathlib.Path,required=True);p.add_argument('--upgrades',type=pathlib.Path,required=True);p.add_argument('--host',type=pathlib.Path,required=True);a=p.parse_args()
def run(cmd):return subprocess.check_output(list(map(str,cmd)),text=True)
def unique(root,name):
 rows=list(root.rglob(name));assert len(rows)==1,(name,len(rows));return rows[0]
row=json.loads(unique(a.candidate,'delivery.json').read_text())[0];apk=unique(a.candidate,row['apk']);assert row['sourceCommit']==a.source and row['versionCode']>=577311003
assert hashlib.sha256(apk.read_bytes()).hexdigest()==row['sha256'] and apk.stat().st_size==row['bytes']
expected='d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7';sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0'
certificate=run([sdk/'apksigner','verify','--verbose','--print-certs',apk]);assert re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})',certificate).group(1)==expected==row['certificateSha256']
badging=run([sdk/'aapt','dump','badging',apk]).splitlines()[0];assert "name='com.starcore.research.r13'" in badging and "versionCode='"+str(row['versionCode'])+"'" in badging
unit_file=unique(a.host,'unit-summary.json');unit=json.loads(unit_file.read_text());assert unit['sourceCommit']==a.source and unit['tests']>0 and unit['failures']==unit['errors']==unit['skipped']==0
charts=json.loads(unique(a.android,'repair-chart-acceptance.json').read_text());assert charts['sourceCommit']==a.source and charts['screenshots']==188 and charts['registeredObjects']==47
fault=json.loads(unique(a.android,'matrix.json').read_text());assert fault['source']==a.source and len(fault['cells'])==14
research_fault=json.loads(unique(a.android,'research-fault-matrix.json').read_text());assert research_fault['sourceCommit']==a.source and research_fault['result']=='PASS' and len(research_fault['cells'])==12
assert {(c['stage'],c['fault']) for c in research_fault['cells'] if c['pass']}=={(s,f) for s in ('analysis','consume') for f in ('BUSY','FULL','CANCEL','KILL','CORRUPT','SOURCE_CHANGE')}
# The real renderer must have run successfully, not merely displayed its caught-error fallback.
states=json.loads(unique(a.android,'states.json').read_text());assert len(states)==72
for state in states:
 capture=state['capture'];assert capture['frameSync'] in ('API29_FRAME_COMMIT','CHOREOGRAPHER_TWO_FRAMES')
 if capture['nativeDrawing'] is not None:
  assert capture['nativeDrawing']['drawCount']>0 and capture['nativeDrawing']['lastDrawingFailure'] is None
critical=[x['capture'] for x in states if x['scenario']=='READY' and x['id'] in ('R01','R02','R03')]
assert len(critical)==3 and len({x['pixelSha256'] for x in critical})==3
assert all(x['nativeDrawing'] is not None for x in critical)
upgrade=[]
for file in a.upgrades.rglob('*.json'):
 if file.name in ('upgrade.json','rollback.json','reupgrade.json'):
  proof=json.loads(file.read_text());assert proof['pass'] and proof['originalDrawsLostOrChangedCount']==0;upgrade.append(proof)
assert len(upgrade)==9 and all(sum(cp in x['label'] for x in upgrade)==3 for cp in ('CP06','CP16','CURRENT_R30_107'))
out=a.candidate;row.update(artifactRole='REPAIR_CANDIDATE_SAME_R27_SIGNER_ANDROID_AND_UPGRADE_VERIFIED',installation='EMULATOR_COVER_INSTALL_ROLLBACK_REUPGRADE_PASS',startup='EMULATOR_FIRST_AND_SECOND_START_PASS',physicalDevice='NOT_RUN',naturalQuality='NOT_PROVEN',overall='ENGINEERING_VERIFIED_WITH_EXPLICIT_DEVICE_AND_QUALITY_LIMITS')
(out/'delivery.json').write_text(json.dumps([row],ensure_ascii=False,indent=2)+'\n')
receipt={'sourceCommit':a.source,'package':'com.starcore.research.r13','versionCode':row['versionCode'],'certificateSha256':expected,'apkSha256':row['sha256'],'registeredCharts':47,'displayScenarios':4,'actualActivityScreenshots':188,'chartStateScope':'Real production synthetic analysis or explicit missing original sources; no fake effective result','faultMatrixCells':14,'repairResearchFaultCells':12,'upgradeDrills':['CP06','CP16','CURRENT_R30_107'],'noUninstall':True,'signerLoader':'ORIGINAL_R27_UNCHANGED','signingKeyReadOrUploadedByAgent':False,'physicalV2353A72h':'NOT_RUN','naturalQuality':'NOT_PROVEN','knownLintBaseline':'51 original errors filtered by unchanged baseline; zero new lint errors required'}
(out/'ACCEPTANCE.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
receipt['actualJUnitXmlSummary']=unit
tag='r30-repair1-'+str(row['versionCode'])+'-'+a.source[:12]
notes=out/'RELEASE_NOTES.md';notes.write_text('R30返修候选，原R27证书，同包覆盖升级。\n\n源码：'+a.source+'\n\n47项四显示场景188截图、原14故障格和新研究链12故障格，以及CP06/CP16/当前R30-107覆盖→回滚→再升级的数据保全已在Android模拟器通过。缺少原始分析或收益证据的图仍显示不足。\n\nV2353A/72小时、原用户大库重测与自然质量尚未证明，不能视为百分百质量闭环。原51项lint基线保留，新lint错误必须为0。\n')
repo=os.environ.get('GITHUB_REPOSITORY','gtt46461-star/starcore-public-build')
files=[apk,out/'delivery.json',out/'SHA256SUMS.txt',out/'public-certificate.txt',out/'ACCEPTANCE.json']
# Public delivery evidence contains only synthetic Activity PNGs and validated receipt JSONs.
# No logs, databases, credentials or signing material are included.
gallery=sorted(f for f in a.android.rglob('*.png') if re.fullmatch(r'(READY|EMPTY|INSUFFICIENT|READ_ERROR)-(R\d{2}|\d{2})\.png',f.name));assert len(gallery)==188
expected_names={f'{s}-{i:02d}.png' for s in ('READY','EMPTY','INSUFFICIENT','READ_ERROR') for i in range(29)}|{f'{s}-R{i:02d}.png' for s in ('READY','EMPTY','INSUFFICIENT','READ_ERROR') for i in range(1,19)}
assert {f.name for f in gallery}==expected_names
original_chart_receipt=json.loads(unique(a.android,'regression.json').read_text());assert original_chart_receipt['source']==a.source and original_chart_receipt['junitCases']==17
image_proofs=charts['artifacts']+original_chart_receipt['screenshots'];assert len(image_proofs)==188
hashes={pathlib.Path(x['file']).name:x['sha256'] for x in image_proofs};assert set(hashes)==expected_names
assert all(hashlib.sha256(f.read_bytes()).hexdigest()==hashes[f.name] and f.read_bytes().startswith(b'\x89PNG\r\n\x1a\n') for f in gallery)
proof_zip=out/'ANDROID_ACCEPTANCE_EVIDENCE.zip'
with zipfile.ZipFile(proof_zip,'w',compression=zipfile.ZIP_DEFLATED) as bundle:
 bundle.write(unit_file,'host/unit-summary.json')
 for file in gallery:bundle.write(file,'android/'+str(file.relative_to(a.android)))
 for name in ('repair-chart-acceptance.json','regression.json','matrix.json','research-fault-matrix.json','states.json'):
  for file in a.android.rglob(name):bundle.write(file,'android/'+str(file.relative_to(a.android)))
 for file in a.upgrades.rglob('*.json'):
  if file.name in ('upgrade.json','rollback.json','reupgrade.json'):bundle.write(file,'upgrade/'+str(file.relative_to(a.upgrades)))
files.append(proof_zip)
proof_hash=hashlib.sha256(proof_zip.read_bytes()).hexdigest();receipt['evidenceArchive']={'file':proof_zip.name,'sha256':proof_hash,'actualGalleryPngs':188}
(out/'ACCEPTANCE.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
with (out/'SHA256SUMS.txt').open('a') as checksum:checksum.write(proof_hash+'  '+proof_zip.name+'\n')
subprocess.run(['gh','release','create',tag,*map(str,files),'--repo',repo,'--target',a.source,'--title','STARCORE R30 返修 '+str(row['versionCode']),'--notes-file',str(notes)],check=True)
print(json.dumps({'release':'https://github.com/'+repo+'/releases/tag/'+tag,'apk':'https://github.com/'+repo+'/releases/download/'+tag+'/'+apk.name,'source':a.source},ensure_ascii=False))
