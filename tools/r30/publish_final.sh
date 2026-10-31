#!/usr/bin/env bash
# Publish only after both existing signing lanes and their actual Android/upgrade gates complete.
set -euo pipefail
[[ "${R27_RUN_ID:-}" =~ ^[0-9]+$ ]] && [[ "${EXPECTED_SOURCE:-}" =~ ^[a-f0-9]{40}$ ]]
[ "$(git rev-parse HEAD)" = "$EXPECTED_SOURCE" ]
gh run watch "$R27_RUN_ID" --repo "$GITHUB_REPOSITORY" --exit-status --interval 15
gh run download "$R27_RUN_ID" --repo "$GITHUB_REPOSITORY" --name R30-R27-FINAL-CANDIDATE --dir final/r27
python3 - <<'PY'
import hashlib,json,os,pathlib,re,shutil,subprocess
root=pathlib.Path('final');out=root/'delivery';out.mkdir(parents=True,exist_ok=True)
source=os.environ['EXPECTED_SOURCE'];tools=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0';rows=[];versions=set()
for folder,lane,cert in [('official','OFFICIAL_R13','9e6d5dc79d29d4ba5b435a171e6535386f8a56ea6fad71a6cc77aeb10ade3eca'),('r27','EXISTING_R27','d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7')]:
    candidates=json.loads((root/folder/'delivery.json').read_text());assert len(candidates)==1;row=dict(candidates[0]);apk=root/folder/row['apk'];data=apk.read_bytes()
    assert row['sourceCommit']==source and row['lane']==lane and row['certificateSha256']==cert
    assert len(data)==row['bytes'] and hashlib.sha256(data).hexdigest()==row['sha256'];assert row['versionCode']>=577310101;versions.add(row['versionCode'])
    public=subprocess.check_output([str(tools/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True);assert re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})',public).group(1)==cert
    shutil.copy2(apk,out/row['apk']);row.update(artifactRole='FINAL_RELEASE_APK',installation='ANDROID_EMULATOR_PASS_BOTH_CP06_CP16',startup='COLD_AND_RESTART_HEARTBEAT_PASS',rollback='SAME_CERT_USERDEBUG_EMULATOR_PASS',androidRegression='16_TESTS_AND_48_SCREENSHOTS_PASS',faultMatrix='14_ANDROID_CELLS_PASS_CONFLICT_REMAINS_HARD_BLOCKED',physicalDevice='NOT_RUN',naturalQuality='INSUFFICIENT',overall='SOURCE_IMPLEMENTED_ANDROID_GATES_PASS_PHONE_AND_NATURAL_ACCEPTANCE_PENDING');rows.append(row)
assert len(rows)==2 and len(versions)==1 and rows[0]['sourceTree']==rows[1]['sourceTree'];version=versions.pop();tag=f'r30-final-{version}-{source[:12]}'
for row in rows:row['downloadUrl']=f"https://github.com/{os.environ['GITHUB_REPOSITORY']}/releases/download/{tag}/{row['apk']}"
(out/'delivery.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n');(out/'SHA256SUMS.txt').write_text(''.join(r['sha256']+'  '+r['apk']+'\n' for r in rows))
quality=json.load(open('construction/r30/QUALITY.json'));device=json.load(open('construction/r30/DEVICE_V2353A.json'))
ledger=json.load(open('construction/r30/FINAL.json'));ledger.update(sourceCommit=source,sourceTree=rows[0]['sourceTree'],versionCode=version,apks=rows,state='FINAL_APK_ANDROID_GATES_PASS_PHONE_QUALITY_PENDING')
receipt={'acceptanceLedger':ledger,'sourceCommit':source,'sourceTree':rows[0]['sourceTree'],'versionCode':version,'existingIdentityApkCount':2,'apks':rows,'officialRun':int(os.environ['GITHUB_RUN_ID']),'r27Run':int(os.environ['R27_RUN_ID']),'physical':device,'naturalQuality':quality,'signingKeysReadByCloudAgent':False,'signingKeysUploaded':False,'sourceArchive':rows[0]['sourceArchive'],'fullAcceptance100Percent':False,'reason':'physical duration/performance and natural quality require original V2353A evidence, not synthetic substitution'}
(out/'FINAL_BUILD.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
notes='STARCORE R30 最终源码构建，两份 APK 分别沿用既有 R13 官方证书和 R27 证书。\n\n'+f'源码：{source}\n\n'
notes+='两证书的 Android 回归、48 张实际界面截图、14 格故障矩阵及 CP06/CP16 覆盖/回退均通过；冲突开奖保持硬阻，系统强停后按显式重新打开恢复。\n\n'
notes+='V2353A 前台60分钟、息屏6小时、连续72小时尚未执行；Q01–Q07自然质量证据不足。源码实现与设备/质量验收分列，未宣称100%全指标达标。\n\n'
notes+='按当前安装应用的签名身份选择对应 APK。公开签名指纹、SHA-256、源码与完整状态见 delivery.json 和 FINAL_BUILD.json。\n'
(out/'RELEASE_NOTES.md').write_text(notes);(root/'release-tag.txt').write_text(tag+'\n')
PY
tag=$(cat final/release-tag.txt)
(cd final/delivery && sha256sum -c SHA256SUMS.txt)
if gh release view "$tag" --repo "$GITHUB_REPOSITORY" > /dev/null 2>&1; then
  echo FINAL_RELEASE_ALREADY_EXISTS
  exit 0
fi
gh release create "$tag" final/delivery/* --repo "$GITHUB_REPOSITORY" --target "$EXPECTED_SOURCE" --title "STARCORE R30 FINAL — two existing-certificate APKs" --notes-file final/delivery/RELEASE_NOTES.md --latest=false
