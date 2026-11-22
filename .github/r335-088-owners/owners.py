"""Reopen controlled producer and UI results on the intact, already released Android package."""
import hashlib
import json
import os
import re
import subprocess
from pathlib import Path

pkg='com.starcore.research.r13'
temp=Path(os.environ['RUNNER_TEMP'])
candidate=temp/'protocol-candidate'
source='59ab4da16977891b093820747b6b39ead8072fbb'
expected=next(line.split()[0] for line in (temp/'SHA256SUMS.txt').read_text().splitlines() if line.endswith('59ab4da16977-release.apk'))
document={'scope':'CONTROLLED_ORIGINAL_OWNER_INPUT_AND_ASSERTION_REPLAY','sourceCommit':source,'apkSha256':expected,'cases':[]}
out=temp/('R335-088-owner-replay-'+os.environ['GITHUB_RUN_ID']+'.json')
def adb(*args):return subprocess.check_output(['adb',*map(str,args)],timeout=240).decode().strip()
def record():out.write_text(json.dumps(document,indent=2)+'\n')
def identity():
    target=next(x[8:] for x in adb('shell','pm','path',pkg).splitlines() if x.endswith('base.apk'))
    return adb('shell','sha256sum',target).split()[0]
def cases():
    assert subprocess.check_output(['git','-C','app-source','rev-parse','HEAD'],text=True).strip()==source
    assert adb('shell','getprop','ro.kernel.qemu')=='1'
    apk=next(candidate.glob('*-release.apk'))
    with apk.open('rb') as stream:assert hashlib.file_digest(stream,'sha256').hexdigest()==expected
    assert 'Success' in adb('install','-r',apk) and identity()==expected
    assert 'Success' in adb('install','-r','-t',candidate/'instrument-androidTest.apk')
    document['api']=adb('shell','getprop','ro.build.version.sdk')
    runner=pkg+'.test/com.ethk3.novaonepro.R30InstrumentedRunner'
    runner=next(i.split('instrumentation:')[1].split(' ')[0] for i in adb('shell','pm','list','instrumentation').splitlines() if i.startswith('instrumentation:'+pkg+'.test/'))
    tests={
      'R335FeedbackCursorTest':['missingOptionalTableDoesNotBlockEligibleFeedback','sidecarStillFindsMissingClosuresAfterFeedbackTableAppears'],
      'strategyv2.R335DuplicateFeedbackTest':['confirmedDrawAdvancesOneTrainingCursorAndGrantsNoNaturalAuthority','missingPredrawUsesTheSoleTerminalAndKeepsTheOriginalDrawLabel'],
      'strategyv2.R335WorkbenchRenderTest':['realRenderedPanelsDoNotWriteBirthsOrResearchActions'],
      'strategyv2.R335MigratedPublicationTest':['observedCoverageFailurePersistsTwoBirthsAndTrainsTheDistinctChild','failedLookCreatesFreshBoundedResearchAndAtomicCursorWithoutInheritedQuality','completedCandidatePublishesBeforePeersAndFutureLabelRequiresTerminalFeedback','forgedCompletionCannotAllocateFutureQualification','expertPairTrainingReceiptsCommitWithBothModelsAndRollbackWithCheckpoint','mixedStatisticalAndGradientExpertRowsRollbackAsOnePair'],
      'R30ChartsAndroidTest':['allFourteenLegacyAndTwoSnapshotChartsOpenInThreeStatesAndSaveScreenshots']}
    tests['strategyv2.R335MigratedPublicationTest'].extend([
        'symbolicF13TrainingCommitsWithCandidateAndTerminalLiveCheckpoint',
        'symbolicF14TrainingCommitsWithCandidateAndTerminalLiveCheckpoint',
        'f06NeuralReceiptsFollowCandidateAndTerminalLiveCheckpointCas',
        'f07NeuralReceiptsFollowCandidateAndTerminalLiveCheckpointCas',
        'chainTrainingReceiptsFollowCandidateAndTerminalLiveCheckpointCas',
        'autoregressiveTrainingReceiptsFollowCandidateAndTerminalLiveCheckpointCas',
        'sparseTrainingReceiptsFollowCandidateAndTerminalLiveCheckpointCas',
        'durationTrainingReceiptsFollowCandidateAndTerminalLiveCheckpointCas',
        'suffixStatisticsFollowCandidateAndTerminalLiveCheckpoint',
        'expiredPrivateWeightsForkFromVerifiedKernelsAndOnlyLaterLabels',
        'badWarmParentIsPreservedAndDoesNotBlockAValidParent',
        'missingWarmCheckpointCannotSilentlyBecomeColdTraining',
        'completedFirstServiceDoesNotStarveAblationFromCpuQueue',
    ])
    for cls,methods in tests.items():
        for method in methods:
            target='com.ethk3.novaonepro.'+cls+'#'+method
            args=['adb','shell','am','instrument','-w','-r','-e','r30Sandbox','repair-owners-'+str(len(document['cases'])),'-e','class',target,runner]
            try:
                p=subprocess.run(args,capture_output=True,timeout=1080 if cls=='R30ChartsAndroidTest' else 480); raw=p.stdout+p.stderr; code=p.returncode
            except subprocess.TimeoutExpired as ex:
                raw=(ex.stdout or b'')+(ex.stderr or b'');code=124
            message=raw.decode(errors='replace')
            passed=bool(code==0 and re.search(r'OK\s*\(1 test\)',message) and 'FAILURES!!!' not in message)
            (temp/'report-originals'/('controlled-'+method+'.log')).write_bytes(raw)
            entry={'class':cls,'method':method,'passed':passed,'exit':code,'originalLogSha256':hashlib.sha256(raw).hexdigest(),
                'sourceFrames':list(dict.fromkeys(re.findall(r'\(([A-Za-z][A-Za-z0-9_]+\.java:\d+)\)',message)))[:15],
                'reasonTypes':list(dict.fromkeys(re.findall(r'(?:[A-Za-z_$]+Exception|AssertionError|ComparisonFailure|IllegalAccessError|NoSuchMethodError|UnsatisfiedLinkError)',message)))[:9]}
            entry['numericComparisons']=re.findall(r'expected:<(-?[0-9]+)> but was:<(-?[0-9]+)>',message)[:10]
            print(cls,method,passed,entry['sourceFrames'][:4],flush=True);document['cases'].append(entry);record()
    assert identity()==expected
    if not any('eighteenViews' in z.get('method','') for z in document['cases']):
        case='com.ethk3.novaonepro.R30RepairChartsAndroidTest#eighteenViewsRenderCommittedAnalysisOrExplicitMissingStatesAndCancelCleanly'
        v=subprocess.run(['adb','shell','am','instrument','-w','-r','-e','r30Sandbox','088-gallery','-e','class',case,runner],capture_output=True,timeout=530)
        txt=v.stdout+v.stderr;document['cases'].append(dict(method=case,passed=v.returncode==0 and bool(re.search(rb'OK\s*\(1 test\)',txt)) and b'FAILURES!!!' not in txt,originalLogSha256=hashlib.sha256(txt).hexdigest()))
    document['closed']=all(x['passed'] for x in document['cases']);record()
try:
    cases()
finally:
    record()
