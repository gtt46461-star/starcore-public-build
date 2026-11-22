"""Test the 065-to-066 upgrade and the original production owners in a disposable Android guest."""
import hashlib,json,os,re,subprocess,time,urllib.request
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
    started=time.monotonic()
    cmd=['adb','shell','am','instrument','-w','-r','-e','r30Sandbox','vivo-'+phase+'-'+method[:40],'-e','class',qualified,runner]
    try:
        result=subprocess.run(cmd,capture_output=True,timeout=480);data=result.stdout+result.stderr;exit_code=result.returncode
    except subprocess.TimeoutExpired as error:
        data=(error.stdout or b'')+(error.stderr or b'');exit_code=124
    name=phase+'-'+method+'.txt';(private/name).write_bytes(data)
    text=data.decode(errors='replace')
    failures=[x.strip()[:320] for x in text.splitlines() if ('AssertionError' in x or 'expected:<' in x or 'ComparisonFailure' in x or 'Exception:' in x or 'Error in ' in x or 'INSTRUMENTATION_FAILED' in x)][:5]
    return {'phase':phase,'test':qualified,'passed':exit_code==0 and bool(re.search(r'OK\s*\(1 test\)',text)) and 'FAILURES!!!' not in text,
             'exit':exit_code,'elapsedMs':int((time.monotonic()-started)*1000),'diagnosisCount':len(failures),
            'numericalComparisons':re.findall(r'expected:<(\d{1,12})> but was:<(\d{1,12})>',text)[:4], 'fixtureStages':[v for v in ('PROBE_SOURCE_BUDGET','PROBE_CURRENT_PARENT','R26_STALE_FENCE_OR_LEASE_OR_SESSION','SOURCE_NOT_PRESENT') if v in text],
            'errorTypes':list(dict.fromkeys(re.findall(r'(?:[A-Za-z$]+Error|[A-Za-z$]+Exception|ComparisonFailure)',text)))[:15],
            'frames':list(dict.fromkeys(re.findall(r'[(]([A-Za-z0-9_$]+[.]java:[0-9]+)[)]',text)))[:24],'originalLogSha256':hashlib.sha256(data).hexdigest()}

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
    before=[]
    assert 'Success' in adb('install','-r',target)
    new_hash,new_uid=installed();assert (new_hash,new_uid)==(target_sha,uid)
    cases={'R335SnapshotPageReadTest':['authenticatedOlderSnapshotRetainsPointsAndDoesNotSupplyCurrentQualification','unrelatedWalCommitDoesNotEraseDecisionRowsOrTheirTotal'], 'strategyv2.R335TerminalTrainingInputTest':['pendingFailedConflictingAndRolledBackTasksCannotLabelALiveModel'], 'R335HotHistoryAndChartsTest':['rollingCoverageIgnoresHistoricalRowsBeyondEitherEdgeAndDirtyRows','legacyChartDistinguishesMissingTableEmptyTableAndInvalidSchema'], 'ShadowCycleAndroidTest':['eachPredrawSettlesOnceAndFeedsTheNextCycle'],
           'WarmShadowCycleTest':['installedSchemaThenOriginalThreePredraws'],
           'R30RepairChartsAndroidTest':['eighteenViewsRenderCommittedAnalysisOrExplicitMissingStatesAndCancelCleanly'],
           'R30ChartsAndroidTest':['allFourteenLegacyAndTwoSnapshotChartsOpenInThreeStatesAndSaveScreenshots'],
           'R335VivoFieldRepairTest':[large,'pendingRetentionArchivesBlockOnlyActualRetirement'],
           'R335RegistryMigrationTest':['signedRegistriesResumeAndPublishTogether'],
           'strategyv2.R335AppliedBudgetTableTest':['originalOldAndCurrentBudgetRowsReopenBesideTheR16Frame'],
           'strategyv2.R26RuntimeDeviceTest':['liveAndDelayedLeasesCannotHideEligibleTasksInEitherLane']}
    extra_cases = {'R335VivoFieldRepairTest': ['historicalReplayAndMissingFreezeDoNotReplaceLatestSettlement'], 'FeedbackBootstrapAndroidTest': ['missingOptionalTableDoesNotBlockEligibleFeedback', 'sidecarStillFindsMissingClosuresAfterFeedbackTableAppears'], 'R30ResearchRetentionAndroidTest': ['withoutRowidEvidenceArchivesExactlyAndRollbackKeepsOriginal'], 'R30ReportSnapshotAndroidTest': ['twelveChaptersKeepOneViewWhileTheWriterAdvances', 'cancelledReadSignalStopsThePrimarySnapshotWithoutHoldingATransaction'], 'R31StorageProtocolInstrumentedTest': ['kernelAllocatedBlocksAndLegacyEnumerationAreHonest', 'productionBlockRequirementAndInjectedSpaceDenialKeepOriginal'], 'R31OwnerFenceProtocolInstrumentedTest': ['staleOwnerReceiptsCannotReplaceNewClaimAndCommitIsIdempotent', 'independentSqliteWriterContentionReleasesBeforeSameInputRetry'], 'R31RuntimeProtocolInstrumentedTest': ['committedDiagnosticFailuresNeverRepeatBusiness', 'rollbackCanRetryAndPayloadConflictCannotMutate', 'feedbackReceiptRequiresTheActualCheckpointTransaction'], 'R30DecisionChainAndroidTest': ['correctionRevokesAndNextAuthorityReadRejectsWithinFiveSeconds', 'originalOwnerPositiveAndIndependentNegativeAuthorities'], 'strategyv2.R26PublicationDeviceTest': ['failedLookCreatesFreshBoundedResearchAndAtomicCursorWithoutInheritedQuality', 'forgedCompletionCannotAllocateFutureQualification', 'waitingFuturePolicyDoesNotStarveReadyPrebirthCatchup', 'mixedStatisticalAndGradientExpertRowsRollbackAsOnePair', 'symbolicF13TrainingCommitsWithCandidateAndTerminalLiveCheckpoint', 'badWarmParentIsPreservedAndDoesNotBlockAValidParent'], 'strategyv2.R31FamilyContinuationInstrumentedTest': ['realOperationYieldsCancelAndJoinOldBodiesThenResumeTheSameStream', 'partialParentsDeepForkAcrossFamiliesAndAllTenPositionsMatchFresh'], 'strategyv2.R26RuntimeDeviceTest': ['allMechanismsAdvanceWithRealWriterPressureAndRestartWithoutStaleCommit']}
    cases={'strategyv2.R26PublicationDeviceTest':[
        'failedLookCreatesFreshBoundedResearchAndAtomicCursorWithoutInheritedQuality',
        'mixedStatisticalAndGradientExpertRowsRollbackAsOnePair','badWarmParentIsPreservedAndDoesNotBlockAValidParent']}
    cases['strategyv2.R335PreparedWriterRetryTest']=['deferredPreparedWriterCanResumeOnce']
    doc={'sourceCommit':source,'apkSha256':target_sha,'priorApkSha256':old_hash,'uidRetained':uid==new_uid,
         'device':adb('shell','getprop','ro.product.model'),'api':adb('shell','getprop','ro.build.version.sdk'),
         'scope':'SUPPLEMENTARY_CURRENT_TEST_SETUP_ORIGINAL_ASSERTIONS_UNALTERED_SIGNED_PRODUCTION_APK','usedTestSources':json.loads(Path('.github/r335-093-publication-probe/test-sources.json').read_bytes()),'baseline':before,'candidate':[]}
    out=temp/('R335-093-publication-probe-'+os.environ['GITHUB_RUN_ID']+'.json')
    for cls,tests in cases.items():
        for method in tests:
            result=run(cls,method,'093');doc['candidate'].append(result)
            out.write_text(json.dumps(doc,indent=2)+'\n');print(result,flush=True)
    rootdir='/sdcard/Android/data/'+pkg+'/files/r30-repair-chart-regression'
    try:
        files=adb('shell','ls','-1',rootdir).splitlines()
        images=sorted(n for n in files if re.fullmatch(r'(READY|EMPTY|INSUFFICIENT|READ_ERROR)-R(0[1-9]|1[0-8])[.]png',n))
        doc['galleryImages']=[{'file':n,'sha256':adb('shell','sha256sum',rootdir+'/'+n).split()[0]} for n in images]
    except subprocess.SubprocessError:
        doc['galleryImages']=[]
    out.write_text(json.dumps(doc,indent=2)+'\n')
    assert installed()==(target_sha,uid)
    if not all(x['passed'] for x in doc['candidate']):raise SystemExit('UNRESOLVED_RUNTIME_TESTS')
if __name__=='__main__':main()
