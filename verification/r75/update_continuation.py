#!/usr/bin/env python3
"""Emit exact reviewed DEV-only continuation; normal semantic build revalidates every byte.
Does not change statistical policy, migrate certificates, or grant Future compatibility.
"""
from pathlib import Path
import hashlib, importlib.util, json
r=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('semantic',r/'scripts/r967_semantic_identity.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
# Compute the proposed source dependency set before the proof exists. This is a
# review producer, not the build verifier; normal Gradle invokes unmodified build().
m.continuation=lambda *args,**kwargs:([], '')
now=m.build(r)
review={'ApiClient.java': 'Latest/history transport polling asks the alternate source when the actual next issue deadline has '
                   'elapsed; unchanged parsed evidence and durable settlement qualification.',
 'ApiTransportGovernanceEngine.java': 'A responsive old draw awaiting its next issue is classified as not-current, not '
                                      'a transport outage; actual source failure and circuit accounting remain.',
 'AutoFetchService.java': 'Fair persistent callback rotation provides continuity and archive/reclaim independent '
                          'opportunities inside the existing parent deadline; maintenance cannot consume every '
                          'training slice, and training completion does not gate maintenance.',
 'CalibrationRepairR75.java': 'Explicit calibration diagnostic repair creates a new candidate/action identity from '
                              'immutable failed-quality summary. Parent development cutoff remains earlier than '
                              'calibration; no held-out labels enter developer training, no old study reset, no '
                              'certificate or REAL permission. Research proposal definition lookup also includes '
                              'DISCOVERED nonbinding atoms; it does not promote or authorize an atom.',
 'DataRetentionGovernorR900.java': 'Revision cleanup is bounded by 128 indexed examined rows with durable keyset '
                                   'cursors and exact orphan recheck; cleanup cannot monopolize archive work. '
                                   'Preserved live/pinned evidence and transactional cursor rollback.',
 'DatabaseCoordinator.java': 'Add transaction begin/body/end timing to existing audit, with unchanged writer '
                             'ownership, deadlines, transaction success and rollback semantics.',
 'DevelopmentProductionCP12.java': 'Separate repair selection knowledge boundary from the older parent '
                                   'development-data cutoff. Ordinary action protocol bytes retain original source '
                                   'semantics; repair inputs exclude parent calibration/Future labels and commit '
                                   'rechecks the boundary.',
 'EvolutionControllerR952.java': 'Dispatch typed calibration-repair actions through the existing durable action owner, '
                                 'graph mutation, Director registration and TRAIN tasks. No synthetic formal lifetime '
                                 'or publication qualification. Research proposal definition lookup also includes '
                                 'DISCOVERED nonbinding atoms; it does not promote or authorize an atom.',
 'FrozenCandidateAdapterR958.java': 'Only the exact deterministic NEGATIVE_GRAPH_SCORE failure is isolated per '
                                    'reservation and persisted before healthy captures continue. Budget, SQL, identity '
                                    'and other errors remain visible; capture and sample eligibility unchanged.',
 'GlobalResearchScheduleR932.java': 'Already-full capture capacity skips non-admittable dependency scans and preserves '
                                    'scan cursor. Deterministically invalid negative graph scores retire only their '
                                    'original study/reservation with explicit failure; no synthetic captures, changed '
                                    'thresholds or REAL authority.',
 'MutationGraphCP12.java': 'Allow immutable registered DISCOVERED nonbinding definitions for research graph proposals, '
                           'matching existing family creation; preserve every registry state, binding assessment, OOS '
                           'evidence and production qualification. No existing frozen execution or certificate '
                           'migration.',
 'MatrixRegistrationR75.java': 'Add bounded durable discovery of matrix specifications from authoritative historical, '
                               'audit, frozen model, common Future and lineage identities. Registration grants no '
                               'numeric sample, atom admission, certificate or REAL permission; existing source '
                               'owners validate and project each observation.',
 'MatrixSnapshotBusR952.java': 'Schedule bounded persistent matrix specification discovery alongside existing '
                              'source capture and projection; preserve numerical projection, input proof and '
                              'checkpoint contracts with fair progress for both discovery and consumption.',
 'MatrixSourceRouterCP12.java': 'New versioned AtomTarget partitions identify their exact frozen source and '
                               'revalidate scope, feature, binding and normalizer instead of scanning an unrelated '
                               'first page. Teacher inputs require actual matching frozen AST and model expert '
                               'references in addition to ordinary common Future evidence. No invented labels '
                               'or grandfathered Teacher qualification. Persist only the last attempted source '
                               'partition so unread page entries are not skipped. Explicit new R75 history '
                               'defaults start from the exact retained numeric window with full-replay '
                               'equivalence, without rewriting old specifications, checkpoints or ACKs. Replace '
                               'repeated correlated prior-row counts with one indexed lag-boundary lookup; '
                               'preserve clean-row eligibility and exact consecutive predecessor/PIT proof.',
 'ProviderFreshnessGateR3611.java': 'After actual source draw deadline, stale previous-issue responses no longer '
                                    'suppress backup polling, including cold startup and normalized timestamps. No '
                                    'fabricated issue, draw or prediction deadline.',
 'ResearchDirectorR952.java': 'Do not consume a durable owner slot without sufficient remaining preparation budget. '
                              'Admitted owners still rotate before execution; numerical updates, leases and training '
                              'evidence semantics unchanged.',
 'ResearchFeedbackCP12.java': 'Route calibration-error diagnosis to the intended mutation operator and provide bounded '
                              'idempotent progression for unsuccessful calibration; never manufacture formal '
                              'observations or predictive qualifications. Research proposal definition lookup also '
                              'includes DISCOVERED nonbinding atoms; it does not promote or authorize an atom.',
 'ResearchTrainingHookRegistryR952.java': 'Preserve an unstarted candidate training/freezing slot when prior owner '
                                          'consumes the useful slice; advance before actual admission so failing '
                                          'owners cannot monopolize training.',
 'RuntimeResourceAuditR927.java': 'Record bounded DB transaction substage timing in existing stage samples without '
                                  'changing existing latency fields or training mathematics.',
 'TrainingContinuationR973.java': 'Finite exact reviewed predecessor capacity is ten solely to preserve R65-R74 '
                                  'development state; unknown hashes rejected, no transitive trust or '
                                  'certificate/Future migration.',
 'r967_semantic_identity.py': 'Explicit R65-R74 baseline allowlist and at most ten exact source-reviewed records for '
                              'modern DEV continuation; policy equality, per-file delta verification and legacy bound '
                              'of eight remain.'}
previous=json.loads((r/'release/r74_training_continuation.json').read_text())
old_records={v['baseline_file']:v for v in previous['predecessors']}
records=[]
for version in range(65,75):
 name=f'release/r{version}_semantic_baseline.json';raw=(r/name).read_bytes();before=json.loads(raw)
 old={v[0]:v[2] for v in before['dependencies']};new={v[0]:v[2] for v in now['dependencies']}
 prior={v['path']:v['rationale'] for v in old_records.get(name,{}).get('reviewed_numeric_changes',[])}
 reviews=[]
 for path in sorted(set(old)|set(new)):
  if old.get(path)==new.get(path):continue
  rationale=prior.get(path,'');delta=review.get(Path(path).name,'')
  if not (rationale or delta):raise RuntimeError('UNREVIEWED_CHANGE:'+path)
  reviews.append(dict(path=path,before_sha256=old.get(path,''),after_sha256=new.get(path,''),rationale=(rationale+' R75: '+delta).strip() if delta else rationale))
 records.append(dict(baseline_file=name,baseline_manifest_sha256=hashlib.sha256(raw).hexdigest(),predecessor_training_rule_sha256=before['training_rule_sha256'],reviewed_numeric_changes=reviews))
release=json.loads((r/'release/release_manifest.json').read_text())
runtime={'applicationId':release['applicationId'],'gameCode':release['gameCode'],'databaseName':release['databaseName'],'databaseUserVersion':release['schema']['userVersion'],'minSdk':release['minSdk'],'runtimeContract':release['runtimeContract']}
proof=dict(scope='EXACT_SOURCE_NUMERIC_CONTINUATION_ONLY_NO_CERTIFICATE_MIGRATION',reviewed_input_provenance_sha256=now['source_provenance_sha256'],runtime_contract_sha256=m.digest(m.canonical(runtime).encode()),predecessors=records)
(r/'release/r75_training_continuation.json').write_text(json.dumps(proof,indent=2)+'\n')
print('R75_REVIEWED_CONTINUATION_WRITTEN',len(records))
