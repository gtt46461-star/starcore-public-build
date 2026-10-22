"""Inspect the exact signed APK's DEX. This is wiring evidence, not a phone execution test."""
from pathlib import Path
import re,sys
r=Path(sys.argv[1])/'com/ethk3/novaonepro';n=0

def method(t,name):
 p=re.search(r'^\.method [^\n]*\b'+re.escape(name)+r'\([^\n]*\n.*?^\.end method',t,re.M|re.S)
 assert p,'METHOD_MISSING:'+name
 return p.group()
def text(name):return (r/(name+'.smali')).read_text()
def ok(v,reason):
 global n
 assert v,reason
 n+=1;print('PASS '+reason)
q=text('ResearchTaskQueueR967');ok('R967_PENDING_TASK_PROTECTED' in q and 'R967_PENDING_INPUT_PROTECTED' in q,'QUEUE_AND_INPUT_PROTECTION_IN_DEX')
ok('R967_TASK_BACKPRESSURE' in q and 'R967_TASK_GENERATION_EXHAUSTED' in text('ResearchTaskQueueR967$Lease'),'BOUNDED_QUEUE_AND_FENCE_OVERFLOW_GUARD')
registry=text('ResearchMultiplicityR932');enroll=method(registry,'enrollResearchIdentity');look=method(registry,'registerLook')
ok('allocateFormalHypothesis' not in enroll and 'allocateFormalHypothesis' in look,'RESEARCH_ENROLLMENT_NOT_FORMAL_SPENDING')
director=text('r952/ResearchDirectorR952');prepare=method(director,'prepareTrainingLabel');commit=method(director,'commitTrainingLabel')
ok('CandidateCompilerR952$Model;->update(' in prepare,'NUMERIC_UPDATE_ACTUAL_PREPARE_OWNER')
ok('CandidateCompilerR952$Model;->update(' not in commit and 'LearningCursorR952;->advance' in commit,'COMMIT_USES_PREPARED_NUMERIC_RESULT_AND_CURSOR')
ok('R967_TRAIN_PREPARED_SNAPSHOT_CHANGED' in commit,'STALE_INPUT_CAS_IN_PRODUCT')
ok('PostSettlementResearchLoopR952;->drainOne' in method(director,'pulse'),'FEEDBACK_DRAINED_INDEPENDENTLY_BY_DIRECTOR')
outbox=text('r952/ResearchOutboxR952');consume=method(outbox,'consume')
ok('ResearchTrainingHookRegistryR952;->commit' in consume and 'PostSettlementResearchLoopR952;->enqueueCommitted' in consume,'TRAINING_ACK_AND_POST_INTENT_SAME_CONSUMER')
ok('TERMINAL_FAILED' not in method(outbox,'release'),'RESOURCE_RETRY_NOT_QUALITY_TERMINATION')
post='\n'.join(p.read_text() for p in (r/'r952').glob('PostSettlementResearchLoopR952*.smali'))
ok('ResearchTaskQueueR967;->complete' in post,'POST_RESULT_ACK_RETAINS_EXISTING_QUEUE_OWNER')
ok('EvolutionControllerR952;->evolveOne' in director and 'EvolutionControllerR952;->evolveInTransaction' not in post,'GLOBAL_EVOLUTION_COALESCED_IN_DIRECTOR_NOT_PER_POST_TASK')
evolution=text('r952/EvolutionControllerR952');ok('MutationPolicyR952;->proposeNovel' in method(evolution,'evolveInTransaction'),'PROPOSAL_CALL_NOT_LOST_BY_EXTRACTION')
continuity=text('ResearchLearningContinuityR912')
# R971 retains the old overload as a delegate; inspect the implementation overload too.
claims='\n'.join(m.group() for m in re.finditer(r'^\.method [^\n]*\bclaimNext\([^\n]*\n.*?^\.end method',continuity,re.M|re.S))
ok('ResearchTaskQueueR967;->claimFor' in claims,'FAIR_LANES_USE_DURABLE_TASK_CLAIMS')
ok('ResearchLearningContinuityR912;->claimNext(Landroid/database/sqlite/SQLiteDatabase;JZ)' in claims,'LEGACY_CLAIM_OVERLOAD_DELEGATES_TO_WEIGHTED_IMPLEMENTATION')
ok('ResearchTaskQueueR967;->complete' in method(continuity,'reviewCommitted'),'REVIEW_RECEIPT_PERSISTED')
ok('numericEvidence=OWNER_RECEIPTS_ONLY' in continuity,'REVIEW_NOT_FALSE_NUMERIC_TRAINING')
service='\n'.join(p.read_text() for p in r.glob('AutoFetchService*.smali'));ok('ResearchLearningContinuityR912;->nextContinuationDelayMs' in service,'SERVICE_RESCHEDULE_USES_QUEUE_BACKOFF')
ok('ResearchTaskQueueR967;->schema' in text('r952/ResearchMigrationRegistryR952'),'TASK_SCHEMA_IN_REAL_MIGRATION_OWNER')
ok('v57_research_lane_cursor_r967' in method(text('SchemaDomainCoordinatorR926'),'researchContract'),'SCHEMA_READY_REQUIRES_LANE_TABLE')
funnel=text('ResearchFunnelR967$Snapshot');ok('NOT_IMPLEMENTED_IN_THIS_CHECKPOINT' in funnel and 'NOT_PROVEN' in funnel,'INCOMPLETE_FULL_PLAN_AND_MODEL_BOUNDARIES_VISIBLE')
for owner in ['ResearchDashboardR924','CompleteReportFileR927','r952/FullbloodDiagnosticsProviderR952']:
 ok('ResearchFunnelR967;->capture' in text(owner),'READ_SNAPSHOT_WIRED:'+owner)
ok('GenesisScanR967;->' in text('AssetGenesisEngineR915'),'RESUMABLE_GENESIS_IN_REAL_CAPTURE_OWNER')
print('R967_FINAL_DEX_WIRING=PASS|checks='+str(n)+'|SOURCE_FALLBACK=NO|ANDROID_EXECUTION=NOT_RUN|FULL_91_CARD_PLAN=NOT_COMPLETE')
