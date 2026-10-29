#!/usr/bin/env python3
"""Deliver only the independently hashed repair whose new export regression actually ran."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import shutil

SOURCE='b639fa4405f1e8c8bd6a34dded424ef1511f149a'
RUN=40653752513
CERT='9e6d5dc79d29d4ba5b435a171e6535386f8a56ea6fad71a6cc77aeb10ade3eca'
NAME='STARCORE-R26-20261028-reportfix1.apk'
TEST='com.ethk3.novaonepro.ReportMetadataExportDeviceTest'


def read(path):return json.loads(path.read_text())
def write(path,value):path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')


def main():
    parser=argparse.ArgumentParser()
    for name in ['build','runtime','workflow','jobs','out','ui-probe']:parser.add_argument('--'+name,required=True,type=Path)
    parser.add_argument('--sha256',required=True);a=parser.parse_args()
    assert re.fullmatch('[0-9a-f]{64}',a.sha256)
    workflow=read(a.workflow);jobs=read(a.jobs)['jobs'];names={j['name']:j for j in jobs}
    assert workflow['id']==RUN and workflow['head_sha']==SOURCE and workflow['path']=='.github/workflows/r13.yml'
    assert workflow['head_branch']=='construction/su1-20261022'
    for name in ['build','android-runtime (29)','android-runtime (35)','android-runtime (36)']:
        assert names[name]['status']=='completed' and names[name]['conclusion']=='success',name
    receipt=a.build/'package-receipt';apks=list(a.build.glob('app/build/outputs/apk/release/*.apk'));assert len(apks)==1
    apk=apks[0];assert hashlib.sha256(apk.read_bytes()).hexdigest()==a.sha256
    assert a.sha256 in (receipt/'SHA256SUMS.txt').read_text()
    package=(receipt/'package.txt').read_text();signature=(receipt/'signature.txt').read_text()
    assert all(x in package for x in ["name='com.starcore.research.r13'","versionCode='577271028'","versionName='R26-2026.10.28-reportfix1'",'arm64-v8a','armeabi-v7a','x86_64'])
    assert CERT in signature and 'Verified using v1 scheme (JAR signing): true' in signature and 'Verified using v2 scheme (APK Signature Scheme v2): true' in signature
    compiled=(receipt/'source-commit.txt').read_text().strip();assert re.fullmatch('[0-9a-f]{40}',compiled)
    native=read(a.runtime/'r26-instrumentation-results.json')
    assert (native['declaredTests'],native['executedTests'],native['failedSelectors'],native['notExecutedTests'],native['status'])==(119,119,0,0,'PASS')
    assert native['manifestSha256']==hashlib.sha256((receipt/'instrumentation-tests.json').read_bytes()).hexdigest()
    assert sum(x['executed'] for x in native['selectors'])==119
    assert all(x['status']=='PASS' and x['executed']==x['expected'] and x['returncode']==0 for x in native['selectors'])
    export=[x for x in native['selectors'] if x['selector']==TEST];assert len(export)==1 and export[0]['executed']==1
    runtime=read(a.runtime/'r26-runtime-synthetic.json');assert runtime['result']=='PASS_WITH_STATED_ENGINEERING_SCOPE'
    service=runtime['committedResearchService'];assert service['sessions'] and all(x['sourceCommit']==compiled for x in service['sessions'])
    assert service['committedCapacityFailuresSinceInstallation']==0
    ui=read(a.ui_probe);assert ui['status']=='PASS_UI_TO_FINAL_ZIP' and ui['sourceCommit']==compiled and ui['packageVersion']==577271028
    assert ui['canonicalCheckContracts']==ui['canonicalCheckResults']==96 and ui['r26ReportSections']==40 and ui['diagnosticScopeIncludesActualWithoutRowidCampaign']
    a.out.mkdir(parents=True,exist_ok=True);shutil.copy2(apk,a.out/NAME)
    (a.out/'source-commit.txt').write_text(compiled+'\n');(a.out/'SHA256SUMS.txt').write_text(a.sha256+'  '+NAME+'\n')
    metadata={'schema':'R26_REPORT_EXPORT_REPAIR_DELIVERY_V1','repair':'WITHOUT_ROWID_GROUPS_AND_STREAMED_METADATA','repairStatus':'PASS_HOST_AND_ANDROID29_MATCHING_SOURCE_REGRESSION','apk':NAME,'apkSha256':a.sha256,'bytes':apk.stat().st_size,'sourceCommit':SOURCE,'compiledCommit':compiled,'workflowRun':RUN,'applicationId':'com.starcore.research.r13','versionCode':577271028,'versionName':'R26-2026.10.28-reportfix1','certificateSha256':CERT,'nativeTests':{k:native[k] for k in ['declaredTests','executedTests','failedSelectors','notExecutedTests','status']},'exportRegression':export[0],'api35and36Scope':'STARTUP_RESTART_RELEASE_SURVIVAL_ONLY','durableCapacityFailures':0,'userDatabaseReplayed':False,'inputEvidence':'Recovery ZIP identified the original error; private user observations are not republished','constructionAcceptance':'INCOMPLETE','naturalStrategyQuality':'INSUFFICIENT','enduranceScope':'SEPARATE_NOT_PROMOTED_TO_PASSED_BY_THIS_DELIVERY','workflowJobs':[{k:j.get(k) for k in ['id','name','status','conclusion']} for j in jobs]}
    write(a.out/'R26-report-repair-delivery.json',metadata);write(a.out/'R26-native-results.json',native);write(a.out/'R26-runtime-results.json',runtime)
    metadata['uiToFinalZipWorkflow']=40655824356;metadata['uiToFinalZip']=ui;write(a.out/'R26-report-repair-delivery.json',metadata);write(a.out/'R26-report-ui-to-zip.json',ui)
    shutil.copy2(Path(__file__).resolve().parents[1]/'construction/r26/continuation/report-export-without-rowid-host.json',a.out/'R26-report-repair-host.json')
    (a.out/'release-notes.md').write_text('修复完整检测报告在 WITHOUT ROWID 表（包括 su2_campaign）上导出失败的问题。分组统计不再依赖隐藏 rowid；主键表按稳定主键顺序流式导出，保留长中文内容、NULL、空文本和二进制描述。大表截取记录真实顺序和分母。\n\n已复现原错误；实际只读 SQLite 回归及 Android29 新导出用例通过，全套119/119项原生测试通过。Android35/36完成启动、重启和release存活检查。\n\n版本577271028，使用既有包名和签名，可覆盖安装。安装后进入完整检测报告，选择“清理旧报告并生成当前完整报告”。本次是导出故障修复；完整施工和自然策略质量仍按各自证据验收，不宣称全方案100%完成。\n')
    print('R26_REPORT_EXPORT_REPAIR_DELIVERY_STAGED_WITH_119_EXECUTED_NATIVE_TESTS')


if __name__=='__main__':main()
