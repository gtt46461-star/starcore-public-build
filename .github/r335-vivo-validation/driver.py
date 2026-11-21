"""Test the 065-to-066 upgrade and the original production owners in a disposable Android guest."""
import hashlib,json,os,re,subprocess,urllib.request
from pathlib import Path

temp=Path(os.environ['RUNNER_TEMP'])
candidate=temp/'protocol-candidate'
private=temp/'report-originals'
pkg='com.starcore.research.r13'
runner=pkg+'.test/com.ethk3.novaonepro.R30FaultAndroidRunner'
source=os.environ['SOURCE']
old='a724adbfb31dd96f1459aa2e5cbd94582dd6e82d'
prior_sha='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'
base='https://github.com/gtt46461-star/starcore-public-build/releases/download'
def adb(*args): return subprocess.check_output(['adb',*map(str,args)],timeout=180).decode().strip()
def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()
def installed():
    path=next(x[8:] for x in adb('shell','pm','path',pkg).splitlines() if x.endswith('base.apk'))
    uid=re.search(r'(?:userId|appId)=(\d+)',adb('shell','dumpsys','package',pkg)).group(1)
    return adb('shell','sha256sum',path).split()[0],uid

def run(cls,method,phase):
    qualified='com.ethk3.novaonepro.'+cls+'#'+method
    cmd=['adb','shell','am','instrument','-w','-r','-e','r30Sandbox','vivo-'+phase+'-'+method[:40],'-e','class',qualified,runner]
    try:
        result=subprocess.run(cmd,capture_output=True,timeout=480);data=result.stdout+result.stderr;exit_code=result.returncode
    except subprocess.TimeoutExpired as error:
        data=(error.stdout or b'')+(error.stderr or b'');exit_code=124
    name=phase+'-'+method+'.txt';(private/name).write_bytes(data)
    text=data.decode(errors='replace')
    failures=[x.strip()[:320] for x in text.splitlines() if ('AssertionError' in x or 'Exception:' in x or 'Error in ' in x or 'INSTRUMENTATION_FAILED' in x)][:5]
    return {'phase':phase,'test':qualified,'passed':exit_code==0 and bool(re.search(r'OK\s*\(1 test\)',text)) and 'FAILURES!!!' not in text,
            'exit':exit_code,'diagnosis':failures,'originalLogSha256':hashlib.sha256(data).hexdigest()}

def main():
    assert adb('shell','getprop','ro.kernel.qemu')=='1'
    target=next(candidate.glob('*-release.apk'));target_sha=sha(target)
    sums=(temp/'SHA256SUMS.txt').read_text()
    assert re.search('^'+target_sha+r'\s+\*?'+re.escape(target.name)+'$',sums,re.M)
    assert source==subprocess.check_output(['git','-C','app-source','rev-parse','HEAD'],text=True).strip()
    prior=temp/'vivo-prior.apk'
    urllib.request.urlretrieve(base+'/r335-B08-'+old[:12]+'/STARCORE-R33-577311065-'+old[:12]+'-release.apk',prior)
    assert sha(prior)==prior_sha and 'Success' in adb('install','-r',prior)
    old_hash,uid=installed();assert old_hash==prior_sha
    assert 'Success' in adb('install','-r','-t',candidate/'instrument-androidTest.apk')
    field='R335VivoFieldRepairTest';large='largeOriginalAndCorruptSiblingRemainIndependentForEveryGroup'
    heart='historicalReplayAndMissingFreezeDoNotReplaceLatestSettlement'
    before=[run(field,method,'065') for method in (large,heart)]
    assert 'Success' in adb('install','-r',target)
    new_hash,new_uid=installed();assert (new_hash,new_uid)==(target_sha,uid)
    cases={field:[large,heart],
      'FeedbackBootstrapAndroidTest':['missingOptionalTableDoesNotBlockEligibleFeedback','sidecarStillFindsMissingClosuresAfterFeedbackTableAppears'],
      'strategyv2.R26PublicationDeviceTest':['failedLookCreatesFreshBoundedResearchAndAtomicCursorWithoutInheritedQuality',
         'completedCandidatePublishesBeforePeersAndFutureLabelRequiresTerminalFeedback','forgedCompletionCannotAllocateFutureQualification',
         'expertPairTrainingReceiptsCommitWithBothModelsAndRollbackWithCheckpoint','mixedStatisticalAndGradientExpertRowsRollbackAsOnePair'],
      'R30DecisionChainAndroidTest':['originalOwnerPositiveAndIndependentNegativeAuthorities'],
      'R30RepairChartsAndroidTest':['eighteenViewsRenderCommittedAnalysisOrExplicitMissingStatesAndCancelCleanly']}
    doc={'sourceCommit':source,'apkSha256':target_sha,'priorApkSha256':old_hash,'uidRetained':uid==new_uid,
         'device':adb('shell','getprop','ro.product.model'),'api':adb('shell','getprop','ro.build.version.sdk'),
         'scope':'CONTROLLED_EMULATOR_UPGRADE_AND_ORIGINAL_PRODUCTION_METHODS','baseline':before,'candidate':[]}
    out=temp/('R335-vivo-validation-'+os.environ['GITHUB_RUN_ID']+'.json')
    for cls,tests in cases.items():
        for method in tests:
            result=run(cls,method,'066');doc['candidate'].append(result)
            out.write_text(json.dumps(doc,indent=2)+'\n');print(result,flush=True)
    assert installed()==(target_sha,uid)
    if not all(x['passed'] for x in doc['candidate']):raise SystemExit('UNRESOLVED_RUNTIME_TESTS')
if __name__=='__main__':main()
