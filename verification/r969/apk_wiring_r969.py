"""Final DEX call-site checks for the R952 disjoint multi-version protocol, not device evidence."""
from pathlib import Path
import re,sys,json
root=Path(__file__).resolve().parents[2];smali=Path(sys.argv[1])/'com/ethk3/novaonepro';checks=0

def text(name):return (smali/(name+'.smali')).read_text()
def group(name):return '\n'.join(p.read_text() for p in smali.glob(name+'*.smali'))
def method(name,member):
    m=re.search(r'^\.method [^\n]*\b'+re.escape(member)+r'\([^\n]*\n.*?^\.end method',text(name),re.M|re.S)
    assert m,'METHOD_MISSING:'+name+'.'+member
    return m.group()
def check(ok,name):
    global checks
    assert ok,name
    checks+=1;print('PASS '+name)
check((smali/'ResearchEvaluationRunR967.smali').is_file(),'STREAM_OWNER_PRESENT')
check((smali/'ResearchEvaluationRunR967$Run.smali').is_file(),'IMMUTABLE_RUN_IDENTITY_PRESENT')
run=group('ResearchEvaluationRunR967');frozen=group('FrozenCandidateAdapterR958');director=group('r952/ResearchDirectorR952')
for call in ['enroll','bindInput','verifyInput','trainingIssue']:
    check('ResearchEvaluationRunR967;->'+call+'(' in director,'ACTUAL_TRAIN_OWNER_CALLS:'+call)
for table in ['r967_numeric_training_run','r967_numeric_training_input','r967_frozen_evaluation_v2','r967_frozen_candidate_all']:
    check(table in run and table in method('SchemaDomainCoordinatorR926','researchContract'),'STREAM_SCHEMA_MIGRATION_WIRED:'+table)
check('R952_NUMERIC_RUN_ORIGIN_V1' in method('ResearchEvaluationRunR967','originHash'),'RUN_ORIGIN_USES_REAL_TYPED_PROVENANCE')
check('AlgorithmProofRecoveryR948;->epoch(' not in run,'R952_RUN_NOT_WRONGLY_BOUND_TO_R948_NONALGORITHM_SENTINEL')
check('r969_run_no_replace' in run and 'r969_input_no_replace' in run and 'r969_eval_no_replace' in run,'REPLACE_GUARDS_IN_PRODUCT_DEX')
check('ResearchEvaluationRunR967;->current(' in frozen and 'ResearchEvaluationRunR967;->evaluationHash(' in frozen,'FREEZE_CURRENT_RUN_AND_IMMUTABLE_MODEL')
check('ResearchEvaluationRunR967;->mayCapture(' in frozen,'FULL1200_CAPTURE_REVALIDATES_STREAM_AND_RUN')
check('ResearchEvaluationRunR967;->verifyRecordedInput(' in group('r952/PostSettlementResearchLoopR952'),'ACTUAL_POST_TRAIN_FEEDBACK_VALIDATES_ORIGINAL_INPUT')
check('ResearchEvaluationRunR967;->verifyRecordedInput(' in group('r952/EvolutionControllerR952'),'ACTUAL_EVOLUTION_EXCLUDES_UNBOUND_SEALED_LABELS')
check('ResearchEvaluationRunR967;->consumerCompatible(' in group('UnifiedResearchChoiceR932'),'CHOICE_CANNOT_SELECT_OUTSIDE_CERTIFIED_STATIC_DOMAIN')
check('ResearchEvaluationRunR967;->mayCapture(' in method('AlgorithmProductionGovernorR915','governFrozen'),'MAINLINE_RECHECKS_DOMAIN_AND_CURRENT_RUN')
check('ResearchEvaluationRunR967;->eligibleEvaluation(' in text('FrozenConsumerRecipeR932$Recipe'),'REPLAY_REJECTS_OUTSIDE_STATIC_DOMAIN')
check('ResearchEvaluationRunR967;->endpoint(' in group('FixedResearchWindowR932'),'WINDOW_ENDPOINT_USES_PREDECLARED_EVALUATION_TARGETS')
check('ResearchEvaluationRunR967;->nextEvaluation(' in group('FixedWindowCertificatePublisherR932'),'CERTIFICATE_AUDIT_FOLLOWS_DECLARED_TARGET_SCHEDULE')
check('ResearchEvaluationRunR967;->nextEvaluation(' in group('FixedWindowStatisticsR932'),'STATISTICS_REJECT_MISSING_DECLARED_TARGETS')
check('R969_FORMAL_PLAN_DISJOINT_V2' in group('ResearchGatePlanR967'),'FORMAL_PLAN_HAS_NEW_SCHEDULE_REVISION')
check('DEPENDENCY_RETIRED' in group('r952/ResearchOutboxR952') and 'DEPENDENCY_RETIRED' in run,'RULE_UPGRADE_CAN_RETIRE_WITHOUT_TRAINING_OR_FAKE_ACK')
for metric in ['training_runs_v2','frozen_versions_v2','training_dependency_retired']:
    check(metric in group('ResearchFunnelR967'),'VISIBLE_READ_ONLY_METRIC:'+metric)
check('r967_numeric_training_run' in group('DataRetentionGovernorR900'),'RUN_SOURCE_ARTIFACTS_PROTECTED_FROM_RETENTION')
check('ResearchEvaluationRunR967;->consumerCompatible(' in group('StagedResearchRosterR943'),'STAGED_ROSTER_FILTERS_INCOMPATIBLE_RUNS')
print('R969_FINAL_DEX_WIRING=PASS|checks='+str(checks)+'|ANDROID_EXECUTION=NOT_RUN|FULL_91_PLAN=NOT_COMPLETE')
