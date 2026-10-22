#!/usr/bin/env python3
"""Exercise production model import/capture with Android's JSON implementation and real SQLite."""
import os, subprocess
from pathlib import Path
R=Path(__file__).resolve().parents[1]
os.chdir(R)
O=R/'build/r932-model-bundle'; O.mkdir(parents=True,exist_ok=True)
H=R/'build/r925-host/compiled-classes'; A=R/'build/r930-batch34-sql'
platform=Path(os.environ.get('ANDROID_PLATFORM_JAR',R.parent/'08_TOOLCHAIN/android-platform-36/android.jar'))
cp=os.pathsep.join(map(str,[A,H,platform]))
production=['ObservationGenerationR932','AdaptiveShadowLearnerEngine','ResearchMethodSignatureR906','DirectedResearchCreationR932','TypedExpressionR932','ResearchMethodPlanR906','ResearchCompiledGraphRuntimeR906','PairedResearchChallengeR932','ResearchLifecycleR932','ExecutableIdentityR932','ResearchCohortR932','CandidateExecutionAuditR932','FixedWindowCertificatePublisherR932','ModuleCalibrationR932','ResearchLogisticCalibrationR932','FrozenConsumerRecipeR932','AlgorithmProductionGovernorR915','CertifiedResearchMixerR915','ResearchMainlineGovernorR903','V51RuntimeResult','ModuleAblationR932','ModuleReplayR932','FrozenModuleInputR932','V51PermutationNumberModelEngine','V51ExpertGateEngine','V51EffectiveConsensusEngine','FixedWindowStatisticsR932','CandidateCalibrationR932','ResearchMultiplicityR932','AtomAdmissionAuditR932','UnifiedResearchChoiceR932','CertificateDependenciesR932','V51PredictabilityArtifact','V51PredictabilityEngine','GlobalResearchScheduleR932','AtomCaptureR932','GenesisAtomicWorldR920','GenesisAssetVmR915','UpstreamModelMathR932','SlowStateR932','FixedResearchWindowR932','ResearchCaptureLedgerR932','TrainedModelMathR932','V51SelectedActionCalibrationEngine','ModelBundleManagerR929','ModelBundleParametersR932','ModelBundleRuntimeR932','V51ActionRankerEngine','DomainRouterR929','CertificateBundleServiceR929','ResearchPlatformStoreR929','PromotionWindowR925','RawTrainingInputR932','SelectionEvidenceContractR929']
files=list((R/'verification/r932/android-json').rglob('*.java'))+[R/'verification/r932/ModelBundleProductionHarnessR932.java',R/'verification/r932/DomainRouteHarnessR932.java',R/'verification/r932/UnifiedCertificateHarnessR932.java',R/'verification/r932/RawUpstreamHarnessR932.java']
files += [R/'verification/r932/MillionAtomIndexHarnessR932.java',R/'verification/r932/MultiplicityHarnessR932.java',R/'verification/r932/DualMemoryHarnessR932.java',R/'verification/r925/stubs/android/content/ContentValues.java',R/'verification/r925/stubs/android/database/sqlite/SQLiteDatabase.java',R/'verification/r925/stubs/com/ethk3/novaonepro/DatabaseCoordinator.java',R/'verification/r932/ObservationRecoveryHarnessR932.java',R/'verification/r932/LifecycleIntegrityHarnessR932.java',R/'verification/r932/DirectedCreationHarnessR932.java',R/'verification/r932/PairedChallengeHarnessR932.java',R/'verification/r932/CohortLifecycleHarnessR932.java',R/'verification/r932/WindowPublicationHarnessR932.java',R/'verification/r932/ModuleCalibrationHarnessR932.java',R/'verification/r932/ModuleAblationHarnessR932.java',R/'verification/r932/CalibratedWindowHarnessR932.java',R/'verification/r932/UnifiedChoiceHarnessR932.java',R/'verification/r932/GlobalScheduleHarnessR932.java',R/'verification/r932/SlowStateHarnessR932.java',R/'verification/r932/AtomCaptureHarnessR932.java',R/'verification/r932/CaptureTransactionHarnessR932.java',R/'verification/r932/FixedWindowHarnessR932.java',R/'verification/r925/stubs/com/ethk3/novaonepro/DbStore.java']
files += [R/'app/src/main/java/com/ethk3/novaonepro'/f'{n}.java' for n in production]
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-cp',cp,'-d',str(O),*map(str,files)],check=True)
cp=os.pathsep.join(map(str,[O,A,H,platform]))
p=subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.ModelBundleProductionHarnessR932',str(R/'verification/r925/sqlite_bridge.py')],capture_output=True,text=True)
report=R/'reports/r932/MODEL_BUNDLE_PRODUCTION.txt';report.parent.mkdir(parents=True,exist_ok=True);report.write_text(p.stdout+p.stderr)
print(p.stdout+p.stderr,end='')
if p.returncode or 'R932_MODEL_BUNDLE_PRODUCTION_PASS' not in p.stdout:raise SystemExit(1)

p=subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.DomainRouteHarnessR932',str(R/'verification/r925/sqlite_bridge.py')],capture_output=True,text=True)
(R/'reports/r932/DOMAIN_ROUTE.txt').write_text(p.stdout+p.stderr);print(p.stdout+p.stderr,end='')
if p.returncode or 'R932_DOMAIN_ROUTE_PASS' not in p.stdout:raise SystemExit(1)

p=subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.UnifiedCertificateHarnessR932',str(R/'verification/r925/sqlite_bridge.py')],capture_output=True,text=True)
(R/'reports/r932/UNIFIED_CERTIFICATE.txt').write_text(p.stdout+p.stderr);print(p.stdout+p.stderr,end='')
if p.returncode or 'R932_UNIFIED_CERTIFICATE_PASS' not in p.stdout:raise SystemExit(1)

p=subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.RawUpstreamHarnessR932',str(O/'raw_upstream.jsonl')],capture_output=True,text=True)
(R/'reports/r932/RAW_UPSTREAM.txt').write_text(p.stdout+p.stderr);print(p.stdout+p.stderr,end='')
if p.returncode or 'R932_RAW_UPSTREAM_PASS' not in p.stdout:raise SystemExit(1)

p=subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.CaptureTransactionHarnessR932',str(R/'verification/r925/sqlite_bridge.py')],capture_output=True,text=True)
(R/'reports/r932/CAPTURE_TRANSACTION.txt').write_text(p.stdout+p.stderr);print(p.stdout+p.stderr,end='')
if p.returncode or 'R932_CAPTURE_TRANSACTION_PASS' not in p.stdout:raise SystemExit(1)

p=subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.FixedWindowHarnessR932',str(R/'verification/r925/sqlite_bridge.py')],capture_output=True,text=True)
(R/'reports/r932/FIXED_WINDOW.txt').write_text(p.stdout+p.stderr);print(p.stdout+p.stderr,end='')
if p.returncode or 'R932_FIXED_WINDOW_PASS' not in p.stdout:raise SystemExit(1)

for name,marker,report in [('ObservationRecoveryHarnessR932','R932_OBSERVATION_RECOVERY_PASS','OBSERVATION_RECOVERY.txt'),('LifecycleIntegrityHarnessR932','R932_LIFECYCLE_INTEGRITY_PASS','LIFECYCLE_INTEGRITY.txt'),('MillionAtomIndexHarnessR932','R932_MILLION_ATOM_INDEX_PASS','MILLION_ATOM_INDEX.txt'),('MultiplicityHarnessR932','R932_MULTIPLICITY_PASS','MULTIPLICITY.txt'),('DualMemoryHarnessR932','R932_DUAL_MEMORY_PASS','DUAL_MEMORY.txt'),('DirectedCreationHarnessR932','R932_DIRECTED_CREATION_PASS','DIRECTED_CREATION.txt'),('PairedChallengeHarnessR932','R932_PAIRED_CHALLENGE_PASS','PAIRED_CHALLENGE.txt'),('CohortLifecycleHarnessR932','R932_COHORT_LIFECYCLE_PASS','COHORT_LIFECYCLE.txt'),('WindowPublicationHarnessR932','R932_WINDOW_PUBLICATION_PASS','WINDOW_PUBLICATION.txt'),('ModuleCalibrationHarnessR932','R932_MODULE_CALIBRATION_PASS','MODULE_CALIBRATION.txt'),('ModuleAblationHarnessR932','R932_MODULE_ABLATION_PASS','MODULE_ABLATION.txt'),('CalibratedWindowHarnessR932','R932_CALIBRATED_WINDOW_PASS','CALIBRATED_WINDOW.txt'),('UnifiedChoiceHarnessR932','R932_UNIFIED_CHOICE_PASS','UNIFIED_CHOICE.txt'),('GlobalScheduleHarnessR932','R932_GLOBAL_SCHEDULE_PASS','GLOBAL_SCHEDULE.txt'),('SlowStateHarnessR932','R932_SLOW_STATE_PASS','SLOW_STATE.txt'),('AtomCaptureHarnessR932','R932_ATOM_CAPTURE_PASS','ATOM_CAPTURE.txt')]:
    p=subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.'+name,str(R/'verification/r925/sqlite_bridge.py')],capture_output=True,text=True)
    (R/'reports/r932'/report).write_text(p.stdout+p.stderr);print(p.stdout+p.stderr,end='')
    if p.returncode or marker not in p.stdout:raise SystemExit(1)
