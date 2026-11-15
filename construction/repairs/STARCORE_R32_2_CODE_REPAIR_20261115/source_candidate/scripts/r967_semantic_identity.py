#!/usr/bin/env python3
"""Explicit, closed-manifest DEV dependency identity and immutable policy build inputs.

This is deliberately conservative: every declared NUMERIC file is included. It
never grants historical-evidence compatibility. New/deleted/unclassified inputs
fail closed; reflected/dynamically loaded code requires a byte-pinned review.
No generated output is hashed back into its own inputs.
"""
from pathlib import Path
import argparse, hashlib, json, re

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = 'release/research_dependencies_r967.json'
POLICY = 'release/research_gate_plan_r967.json'
GENERATED = 'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java'
REPORT = 'app/build/generated/r967/semantic_manifest.json'
DYNAMIC = re.compile(r'\b(?:Class\s*\.\s*forName|loadClass|DexClassLoader|PathClassLoader|ServiceLoader\s*\.\s*load|System\s*\.\s*loadLibrary)\b')
ROLES = {'NUMERIC', 'PRESENTATION', 'PROVENANCE', 'POLICY', 'BUILD_CONTRACT'}
BUILD_INPUTS = ('scripts/cp4_compile_catalog.py', 'scripts/cp4_compile_molecule_seeds.py',
                'scripts/verify_catalog_assets_cp12.py', 'scripts/r817_generate_provenance.sh',
                'scripts/r815_generate_provenance.py', 'scripts/r815_generate_provenance.sh',
                'scripts/r926_release_manifest.py', 'scripts/r997_construction_assets.py',
                'scripts/opt299_stage_build.py', 'scripts/generate_strategy_su1_schema.py',
                'scripts/prepare_strategy_su1_identity.py', 'scripts/verify_strategy_su1.py',
                'scripts/generate_strategy_su2_schema.py', 'scripts/verify_plan1025.py',
                'scripts/test_plan1025_storage.py', 'tools/strategy_su2/schema/strategy_su2.sql',
                'release/strategy_su1_reviewed_inputs.json', 'schema/strategy_su1.sql',
                'schema/strategy_su1_foundry.sql', 'schema/strategy_su1_runtime.sql')

def canonical(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(',', ':'), allow_nan=False)

def digest(data):
    return hashlib.sha256(data).hexdigest()

def identity_json(raw):
    # Input classifications and runtime fields must have one interpretation.
    # A last-key-wins parser can silently discard a conflicting role or Owner.
    def unique_pairs(pairs):
        value = {}
        for key, item in pairs:
            if key in value:
                raise ValueError('R967_DUPLICATE_IDENTITY_KEY:' + key)
            value[key] = item
        return value
    return json.loads(raw, object_pairs_hook=unique_pairs)

def controlled_paths(root):
    result = set()
    for directory in ('app/src/main/java', 'app/src/main/assets', 'app/src/main/res', 'app/src/main/cpp'):
        base=root/directory
        if not base.is_dir() or base.is_symlink():
            raise ValueError('R967_MISSING_OR_SYMLINK_DIRECTORY:'+directory)
        for path in base.rglob('*'):
            if path.is_symlink():
                raise ValueError('R967_UNREVIEWED_SYMLINK:' + str(path))
            if path.is_file():
                result.add(path.relative_to(root).as_posix())
    result.update(('app/src/main/AndroidManifest.xml', 'app/build.gradle', 'scripts/r967_semantic_identity.py', POLICY,
                   'scripts/r982_source_call_asset.py', 'release/r982_source_calls.tsv.gz', 'release/r982_source_calls.manifest.json'))
    result.update(BUILD_INPUTS)
    if (root/'release/r82_training_invalidation.json').is_file():
        result.update(('release/r81_semantic_baseline.json', 'release/r81_controlled_source_baseline.json', 'release/r82_training_invalidation.json'))
        # The reviewed R82 offline drift exporter arrives atomically with its helper.
        # Earlier cumulative stages retain the old, unchanged offline exporter.
        if (root/'training/r932/drift_policy_r82.py').is_file():
            result.update(('training/r932/drift_policy_r82.py','training/r932/export_bundle.py'))
    # Standalone build inputs do not pass through the directory traversal above.
    # Their bytes and paths require the same reviewed, regular-file boundary.
    for name in sorted(result):
        path = root / name
        for component in (path, *path.parents):
            if component == root:
                break
            if component.is_symlink():
                raise ValueError('R967_UNREVIEWED_SYMLINK:' + name)
        if not path.is_file():
            raise ValueError('R967_REQUIRED_INPUT_MISSING:' + name)
    return result

def read_policy_r980(raw):
    policy = json.loads(raw)
    required = {'revision','mode','statistics_revision','calibration_revision','data_roles','window_sizes',
                'missing_policy','restart_policy','allocation_revision','total_alpha','max_looks',
                'min_hit_rate','min_wilson_lower','max_brier','max_ece','min_paired_delta_exclusive',
                'min_block_lower_exclusive','consumer_cap','real_authority','null_assumption',
                'legacy_point_rate_alone_is_certificate'}
    if set(policy) != required:
        raise ValueError('R967_POLICY_FIELDS:' + str(sorted(set(policy) ^ required)))
    if policy['mode'] != 'STATIC_FROZEN' or policy['statistics_revision'] != 'R932_BOUNDED_PAIRED_AZUMA_V1':
        raise ValueError('R967_POLICY_INTERPRETER_UNSUPPORTED')
    if policy['calibration_revision'] != 'R932_CANDIDATE_CAL_LOGISTIC_V1':
        raise ValueError('R967_CALIBRATION_INTERPRETER_UNSUPPORTED')
    if policy['data_roles'] != {'development':'TRAINABLE_ONLY','calibration_fit':120,'embargo':1,'calibration_validation':120,'formal':'SEALED_NOT_TRAINABLE'}:
        raise ValueError('R967_ROLE_INTERPRETER_UNSUPPORTED')
    if policy['window_sizes'] != [120,240,240,240,360] or policy['max_looks'] != 5 or policy['total_alpha'] != .05:
        raise ValueError('R967_ALLOCATION_INTERPRETER_UNSUPPORTED')
    if policy['allocation_revision'] != 'R932_GLOBAL_FAMILY_HYPOTHESIS_SUMMABLE_FIVE_LOOKS_V1':
        raise ValueError('R967_ALLOCATION_REVISION_UNSUPPORTED')
    if policy['missing_policy'] != 'KEEP_ORIGINAL_ENDPOINT_PAUSED_CAPTURE_NO_IMPUTATION' or policy['restart_policy'] != 'AFTER_OLD_ENDPOINT_NEW_PAID_CLAIM_SAME_FAMILY_NO_REFUND':
        raise ValueError('R967_MISSING_RESTART_INTERPRETER_UNSUPPORTED')
    for name in ('min_hit_rate','min_wilson_lower','max_brier','max_ece','consumer_cap'):
        value=policy[name]
        if isinstance(value,bool) or not isinstance(value,(int,float)) or not 0 <= value <= 1:
            raise ValueError('R967_POLICY_RANGE:' + name)
    if policy['real_authority'] is not False or policy['legacy_point_rate_alone_is_certificate'] is not False:
        raise ValueError('R967_POLICY_AUTHORITY_ESCALATION')
    if not re.fullmatch(r'[A-Z0-9_]{1,80}',policy['revision']):
        raise ValueError('R967_POLICY_REVISION')
    if policy['consumer_cap'] != .05 or policy['null_assumption'] != 'BOUNDED_PAIRED_DELTA_IN_MINUS1_PLUS1_CONDITIONAL_MEAN_NONPOSITIVE':
        raise ValueError('R967_CONSUMER_OR_NULL_INTERPRETER_UNSUPPORTED')
    if policy['min_hit_rate'] < .34 or policy['min_wilson_lower'] < .30 or policy['max_brier'] > .24 or policy['max_ece'] > .04:
        raise ValueError('R967_POLICY_CANNOT_LOOSEN_EXISTING_GATES')
    for name in ('min_paired_delta_exclusive','min_block_lower_exclusive'):
        if isinstance(policy[name],bool) or not isinstance(policy[name],(float,int)) or not 0 <= policy[name] <= 1:
            raise ValueError('R967_POLICY_RANGE:' + name)
    canonical(policy) # also rejects NaN/infinity
    return policy

import math

CP3_POLICY_V1 = {'revision': 'CP3_STATIC_FIXED_POLICY_V1', 'mode': 'STATIC_FROZEN', 'statistics_revision': 'CP3_PREQUENTIAL_EPROCESS_V1', 'calibration_revision': 'R932_CANDIDATE_CAL_LOGISTIC_V1', 'data_roles': {'development': 'TRAINABLE_ONLY', 'calibration_fit': 120, 'embargo': 1, 'calibration_validation': 120, 'formal': 'SEALED_NOT_TRAINABLE'}, 'window_sizes': [120, 240, 240, 240, 360], 'planned_endpoints': [120, 360, 600, 840, 1200], 'min_certificate_n': 360, 'max_looks': 5, 'missing_policy': 'KEEP_ORIGINAL_ENDPOINT_PAUSED_CAPTURE_NO_IMPUTATION', 'restart_policy': 'AFTER_OLD_ENDPOINT_NEW_PAID_CLAIM_SAME_FAMILY_NO_REFUND', 'allocation_revision': 'CP3_GLOBAL_LIFETIME_SUMMABLE_V1', 'total_alpha': 0.05, 'statistical_family': 'CP3_ALL_PRODUCTION', 'alpha_formula': '0.05/(j*(j+1));j=global_index+1', 'hypotheses_per_freeze_protocol': 1, 'terminal_look_id': 'CP3_PROTOCOL_FINAL', 'null_assumption': 'CONDITIONAL_HIT_PROBABILITY_LE_0_30', 'baseline_probability': 0.3, 'eprocess_alternatives': [0.31, 0.32, 0.34, 0.36, 0.4, 0.45, 0.5, 0.6, 0.7, 0.8, 0.9], 'eprocess_weights': 'UNIFORM_FIXED', 'crossing_rule': 'CURRENT_LOG_E_GTE_NEG_LOG_ALPHA', 'decision_p': 'MIN_1_EXP_NEG_MAX_LOG_E', 'min_hit_rate': 0.34, 'min_wilson_lower': 0.3, 'wilson_comparison': 'STRICT_GREATER', 'wilson_role': 'IID_DESCRIPTIVE_ADDITIONAL_GATE', 'min_fixed_window_hit_rate': 0.3, 'rolling_hit_rate_min': {'20': 0.15, '50': 0.22, '100': 0.27, '300': 0.34}, 'max_brier': 0.24, 'brier_relative_gate': 'LTE_SAME_WINDOW_CONSTANT_0_30_EMPIRICAL_BRIER', 'max_ece': 0.04, 'ece_bins': 10, 'min_coverage': 0.95, 'min_window_integrity': 1.0, 'max_loss_streak': 12, 'max_residual_drawdown': 8.0, 'max_js_drift': 0.1, 'min_regime_n': 50, 'min_regime_hit_rate': 0.27, 'unsupported_regime_policy': 'EXCLUDE_FROM_CERTIFICATE_DOMAIN_NO_SUPPORTED_GROUP_BLOCKS', 'latency_p95_ms_max': 700, 'latency_measurements_min': 50, 'heap_increment_bytes_max': 67108864, 'resource_unmeasured': 'BLOCK', 'duplicate_jaccard_min': 0.98, 'duplicate_paired_advantage_max': 0.0, 'comparison_reference_required': True, 'certificate_valid_issues': 120, 'renewal_new_future_n': 120, 'consumer_cap': 0.05, 'real_authority': False, 'legacy_point_rate_alone_is_certificate': False}

def _cp3_no_duplicate_pairs(pairs):
    out = {}
    for key, value in pairs:
        if key in out:
            raise ValueError('CP3_DUPLICATE_POLICY_KEY:' + key)
        out[key] = value
    return out

def _cp3_no_constant(value):
    raise ValueError('CP3_NONFINITE_POLICY_NUMBER:' + value)

def _cp3_check_finite(value):
    if isinstance(value, float) and not math.isfinite(value):
        raise ValueError('CP3_NONFINITE_POLICY_NUMBER')
    if isinstance(value, dict):
        for child in value.values(): _cp3_check_finite(child)
    elif isinstance(value, list):
        for child in value: _cp3_check_finite(child)

def read_policy_cp3(raw):
    policy = json.loads(raw, object_pairs_hook=_cp3_no_duplicate_pairs,
                        parse_constant=_cp3_no_constant)
    if not isinstance(policy, dict):
        raise ValueError('CP3_POLICY_OBJECT_REQUIRED')
    _cp3_check_finite(policy)
    if set(policy) != set(CP3_POLICY_V1):
        raise ValueError('CP3_POLICY_FIELDS:' + str(sorted(set(policy) ^ set(CP3_POLICY_V1))))
    # This is a STATIC_FROZEN revision: every field/value, including types as
    # serialized below, has exactly one supported interpretation. Changing a
    # threshold requires another reviewed revision and new evidence identity.
    if canonical(policy) != canonical(CP3_POLICY_V1):
        raise ValueError('CP3_POLICY_VALUE_OR_TYPE_UNSUPPORTED')
    return policy

def read_policy(raw):
    probe = json.loads(raw, object_pairs_hook=_cp3_no_duplicate_pairs,
                       parse_constant=_cp3_no_constant)
    if not isinstance(probe, dict):
        raise ValueError('CP3_POLICY_OBJECT_REQUIRED')
    revision = probe.get('statistics_revision')
    if revision == 'R932_BOUNDED_PAIRED_AZUMA_V1':
        return read_policy_r980(raw)
    if revision == 'CP3_PREQUENTIAL_EPROCESS_V1':
        return read_policy_cp3(raw)
    raise ValueError('CP3_POLICY_INTERPRETER_UNSUPPORTED:' + str(revision))


def continuation(root, provenance_hash, dependencies, runtime, modern=False):
    # R976: keep an explicit, byte-reviewed finite set of prior numeric rules.
    # One predecessor slot loses R974 learners when R975 is added; accepting every
    # historical rule instead would silently authorize unknown numerical changes.
    multi=root/('release/r81_training_continuation.json' if modern else 'release/r980_training_continuation.json')
    if modern and not multi.is_file():multi=root/'release/r80_training_continuation.json'
    if modern and not multi.is_file():multi=root/'release/r79_training_continuation.json'
    if modern and not multi.is_file():multi=root/'release/r78_training_continuation.json'
    if modern and not multi.is_file():multi=root/'release/r77_training_continuation.json'
    if modern and not multi.is_file():return [],''
    if not multi.is_file():multi=root/'release/r979_training_continuation.json'
    if not multi.is_file():multi=root/'release/r978_training_continuation.json'
    if not multi.is_file():multi=root/'release/r977_training_continuation.json'
    if not multi.is_file():multi=root/'release/r976_training_continuation.json'
    if multi.is_file():
        raw=multi.read_bytes(); proof=json.loads(raw)
        records=proof.get('predecessors',[])
        if not isinstance(records,list) or not 1 <= len(records) <= (16 if modern else 8):
            raise ValueError('R976_CONTINUATION_PREDECESSOR_COUNT')
        if set(proof) != {'scope','reviewed_input_provenance_sha256','runtime_contract_sha256','predecessors'}:
            raise ValueError('R976_CONTINUATION_FIELDS')
    else:
        current=(root/'release/r975_training_continuation.json').is_file()
        path=root/('release/r975_training_continuation.json' if current else 'release/r973_training_continuation.json')
        if not path.is_file(): return [], ''
        raw=path.read_bytes(); proof=json.loads(raw)
        record=dict(proof)
        record['baseline_file']='release/r974_semantic_baseline.json' if current else 'release/r972_semantic_baseline.json'
        records=[record]
    if proof.get('scope')!='EXACT_SOURCE_NUMERIC_CONTINUATION_ONLY_NO_CERTIFICATE_MIGRATION':
        raise ValueError('R973_CONTINUATION_SCOPE')
    if proof.get('reviewed_input_provenance_sha256')!=provenance_hash:
        raise ValueError('R973_CONTINUATION_SOURCE_NOT_REVIEWED')
    if proof.get('runtime_contract_sha256')!=digest(canonical(runtime).encode()):
        raise ValueError('R973_CONTINUATION_RUNTIME_NOT_REVIEWED')
    accepted=[]
    for record in records:
        filename=record.get('baseline_file','')
        if filename not in ('release/r972_semantic_baseline.json','release/r974_semantic_baseline.json','release/r975_semantic_baseline.json','release/r976_semantic_baseline.json','release/r977_semantic_baseline.json','release/r978_semantic_baseline.json','release/r979_semantic_baseline.json','release/r65_semantic_baseline.json','release/r66_semantic_baseline.json','release/r67_semantic_baseline.json','release/r68_semantic_baseline.json','release/r69_semantic_baseline.json','release/r70_semantic_baseline.json','release/r71_semantic_baseline.json','release/r72_semantic_baseline.json','release/r73_semantic_baseline.json','release/r74_semantic_baseline.json','release/r75_semantic_baseline.json','release/r76_semantic_baseline.json','release/r77_semantic_baseline.json','release/r78_semantic_baseline.json','release/r79_semantic_baseline.json','release/r80_semantic_baseline.json'):
            raise ValueError('R976_CONTINUATION_BASELINE_NOT_ALLOWLISTED')
        before=(root/filename).read_bytes()
        if digest(before)!=record.get('baseline_manifest_sha256'):
            raise ValueError('R973_CONTINUATION_BASELINE_DRIFT')
        baseline=json.loads(before)
        if modern and (filename not in ('release/r65_semantic_baseline.json','release/r66_semantic_baseline.json','release/r67_semantic_baseline.json','release/r68_semantic_baseline.json','release/r69_semantic_baseline.json','release/r70_semantic_baseline.json','release/r71_semantic_baseline.json','release/r72_semantic_baseline.json','release/r73_semantic_baseline.json','release/r74_semantic_baseline.json','release/r75_semantic_baseline.json','release/r76_semantic_baseline.json','release/r77_semantic_baseline.json','release/r78_semantic_baseline.json','release/r79_semantic_baseline.json','release/r80_semantic_baseline.json') or baseline['policy_hash']!=digest(canonical(read_policy((root/POLICY).read_bytes())).encode())):
            raise ValueError('R66_CONTINUATION_POLICY_DRIFT')
        prior=record.get('predecessor_training_rule_sha256','')
        if prior!=baseline['training_rule_sha256'] or not re.fullmatch('[0-9a-f]{64}',prior) or prior in accepted:
            raise ValueError('R973_CONTINUATION_RULE_DRIFT')
        old={x[0]:x[2] for x in baseline['dependencies']}
        new={x[0]:x[2] for x in dependencies}
        changes={p:(old.get(p,''),new.get(p,'')) for p in set(old)|set(new) if old.get(p)!=new.get(p)}
        reviews=record.get('reviewed_numeric_changes',[])
        reviewed={x['path']:(x['before_sha256'],x['after_sha256']) for x in reviews}
        if reviewed!=changes or len(reviewed)!=len(reviews) or any(not x.get('rationale','').strip() for x in reviews):
            raise ValueError('R973_CONTINUATION_DIFF_NOT_REVIEWED')
        accepted.append(prior)
    return accepted,digest(raw)


R82_BASELINE_SEMANTIC_SHA256 = 'dfcf44525ebff47f018c55dd0772c2b0c61ba335fc90f6fe4a25ad7dd2293eb7'
R82_BASELINE_CONTROLLED_SHA256 = 'a01f962a253d97879d7498d6e2c881719ac777bff64557a94310b84bdf7a0024'

def invalidation_r82(root, provenance_hash, dependencies, runtime, classified):
    """A new numerical rule explicitly revokes all old qualification inheritance.

    This path preserves old evidence bytes but grants no rule continuation. The
    immutable R81 inputs and every changed non-provenance source role are checked
    independently of the producer that prepares this proof.
    """
    path = root/'release/r82_training_invalidation.json'
    raw = path.read_bytes()
    proof = identity_json(raw)
    required = {'revision','scope','decision','baseline_file','baseline_manifest_sha256',
                'controlled_baseline_file','controlled_baseline_sha256','old_training_rule_sha256',
                'reviewed_input_provenance_sha256','runtime_contract_sha256',
                'current_controlled_roles_sha256','predecessors','controlled_changes','new_role_reviews'}
    if set(proof) != required:
        raise ValueError('R82_INVALIDATION_FIELDS')
    if (proof['revision'] != 'R82_NUMERIC_SEMANTICS_INVALIDATION_V1'
            or proof['scope'] != 'INVALIDATE_OLD_RULE_PRESERVE_DEV_MODELS_LABELS_NO_QUALIFICATION_MIGRATION'
            or proof['decision'] != 'INVALIDATE_OLD_RULE'
            or proof['predecessors'] != [] or not isinstance(proof['predecessors'],list)):
        raise ValueError('R82_INVALIDATION_SCOPE_OR_PREDECESSORS')
    if proof['baseline_file'] != 'release/r81_semantic_baseline.json' or proof['controlled_baseline_file'] != 'release/r81_controlled_source_baseline.json':
        raise ValueError('R82_INVALIDATION_BASELINE_PATH')
    baseline_raw = (root/proof['baseline_file']).read_bytes()
    controlled_raw = (root/proof['controlled_baseline_file']).read_bytes()
    if (digest(baseline_raw) != R82_BASELINE_SEMANTIC_SHA256
            or proof['baseline_manifest_sha256'] != R82_BASELINE_SEMANTIC_SHA256
            or digest(controlled_raw) != R82_BASELINE_CONTROLLED_SHA256
            or proof['controlled_baseline_sha256'] != R82_BASELINE_CONTROLLED_SHA256):
        raise ValueError('R82_IMMUTABLE_R81_BASELINE_DRIFT')
    baseline = identity_json(baseline_raw)
    before_doc = identity_json(controlled_raw)
    if (set(before_doc) != {'revision','semantic_manifest_sha256','files'}
            or before_doc['revision'] != 'R81_EXACT_SHIPPED_FINAL_CONTROLLED_SOURCE_V1'
            or before_doc['semantic_manifest_sha256'] != R82_BASELINE_SEMANTIC_SHA256
            or proof['old_training_rule_sha256'] != baseline['training_rule_sha256']):
        raise ValueError('R82_OLD_RULE_DRIFT')
    old = {}
    for record in before_doc['files']:
        if set(record) != {'path','role','sha256'} or record['path'] in old or record['role'] not in ROLES:
            raise ValueError('R82_INVALID_OLD_CONTROLLED_MAP')
        old[record['path']] = record
    if sorted([p,r['role'],r['sha256']] for p,r in old.items() if r['role'] in ('NUMERIC','BUILD_CONTRACT')) != baseline['dependencies']:
        raise ValueError('R82_BASELINE_DEPENDENCY_MAP_DRIFT')
    if proof['reviewed_input_provenance_sha256'] != provenance_hash:
        raise ValueError('R82_INVALIDATION_SOURCE_NOT_REVIEWED')
    if proof['runtime_contract_sha256'] != digest(canonical(runtime).encode()):
        raise ValueError('R82_INVALIDATION_RUNTIME_NOT_REVIEWED')
    roles = sorted([p,r['role']] for p,r in classified.items())
    if proof['current_controlled_roles_sha256'] != digest(canonical(roles).encode()):
        raise ValueError('R82_INVALIDATION_ROLE_MAP_DRIFT')
    # Proof/baseline/call-index bytes are provenance and excluded from the source
    # hash. Their roles, immutable baseline hashes and the proof schema are not.
    changes = {}
    for name in set(old)|set(classified):
        before = old.get(name,{})
        after = classified.get(name,{})
        before_role,after_role = before.get('role',''),after.get('role','')
        if before_role == 'PROVENANCE' and after_role == 'PROVENANCE':
            continue
        if not before_role and after_role == 'PROVENANCE':
            continue
        after_hash = digest((root/name).read_bytes()) if after else ''
        before_hash = before.get('sha256','')
        if (before_hash,before_role) != (after_hash,after_role):
            changes[name] = (before_hash,after_hash,before_role,after_role)
    reviews = proof['controlled_changes']
    if not isinstance(reviews,list):
        raise ValueError('R82_CONTROLLED_REVIEW_TYPE')
    reviewed = {}
    for review in reviews:
        if set(review) != {'path','before_sha256','after_sha256','before_role','after_role','reviewer','rationale'}:
            raise ValueError('R82_CONTROLLED_REVIEW_FIELDS')
        if (review['path'] in reviewed or not isinstance(review['rationale'],str)
                or len(review['rationale'].strip()) < 32
                or not isinstance(review['reviewer'],str) or not review['reviewer'].strip()):
            raise ValueError('R82_CONTROLLED_REVIEW_NOT_EXPLICIT')
        reviewed[review['path']] = tuple(review[k] for k in ('before_sha256','after_sha256','before_role','after_role'))
    if reviewed != changes:
        raise ValueError('R82_INVALIDATION_DIFF_NOT_REVIEWED')
    added = {p:r['role'] for p,r in classified.items() if p not in old}
    role_reviews = proof['new_role_reviews']
    if not isinstance(role_reviews,list):
        raise ValueError('R82_NEW_ROLE_REVIEW_TYPE')
    actual_roles = {}
    for review in role_reviews:
        if (set(review) != {'path','role','reviewer','rationale'} or review['path'] in actual_roles
                or not isinstance(review['rationale'],str) or len(review['rationale'].strip()) < 32
                or not isinstance(review['reviewer'],str) or not review['reviewer'].strip()):
            raise ValueError('R82_NEW_ROLE_REVIEW_NOT_EXPLICIT')
        actual_roles[review['path']] = review['role']
    if actual_roles != added:
        raise ValueError('R82_NEW_CONTROLLED_ROLES_NOT_REVIEWED')
    return [],digest(raw)

def build(root=ROOT, overrides=None, release_override=None, manifest_override=None):
    root=Path(root).resolve(); overrides=overrides or {}
    # Historical callers use paths relative to the Java package.
    overrides={k if k.startswith(('app/','release/','scripts/')) else 'app/src/main/java/com/ethk3/novaonepro/'+k:v for k,v in overrides.items()}
    manifest=manifest_override if manifest_override is not None else identity_json((root/MANIFEST).read_bytes())
    if manifest.get('revision') != 'R967_EXPLICIT_DEPENDENCIES_V1':
        raise ValueError('R967_DEPENDENCY_REVISION')
    records=manifest.get('files',[]); classified={}
    for record in records:
        path=record['path'];role=record['role']
        if path in classified or role not in ROLES or Path(path).is_absolute() or '..' in Path(path).parts:
            raise ValueError('R967_INVALID_CLASSIFICATION:'+path)
        classified[path]=record
    actual=controlled_paths(root)
    if actual != set(classified):
        raise ValueError('R967_UNCLASSIFIED_OR_MISSING_PATHS:'+canonical({'new':sorted(actual-set(classified)),'missing':sorted(set(classified)-actual)}))
    if set(overrides) - actual:
        raise ValueError('R967_OVERRIDE_UNKNOWN_PATH')
    numeric=[];provenance=[]
    for path,record in sorted(classified.items()):
        raw=overrides.get(path,(root/path).read_bytes());h=digest(raw)
        if record['role'] in ('NUMERIC','BUILD_CONTRACT'):
            if path.endswith('.java') and DYNAMIC.search(raw.decode('utf-8')) and record.get('dynamic_review_sha256')!=h:
                raise ValueError('R967_UNKNOWN_DYNAMIC_DEPENDENCY:'+path)
            numeric.append([path,record['role'],h])
        if record['role']!='PROVENANCE':
            provenance.append([path,h])
    release=release_override if release_override is not None else identity_json((root/'release/release_manifest.json').read_bytes())
    runtime={'applicationId':release['applicationId'],'gameCode':release['gameCode'],'databaseName':release['databaseName'],
             'databaseUserVersion':release['schema']['userVersion'],'minSdk':release['minSdk'],'runtimeContract':release['runtimeContract']}
    policy=read_policy(overrides.get(POLICY,(root/POLICY).read_bytes()))
    policy_json=canonical(policy);manifest_hash=digest(canonical(manifest).encode())
    source_provenance=digest(canonical(provenance).encode())
    if (root/'release/r82_training_invalidation.json').is_file():
        predecessors,proof_hash=invalidation_r82(root,source_provenance,numeric,runtime,classified)
    elif policy['statistics_revision']=='CP3_PREQUENTIAL_EPROCESS_V1':
        predecessors,proof_hash=continuation(root,source_provenance,numeric,runtime,modern=True)
    else:
        predecessors,proof_hash=continuation(root,source_provenance,numeric,runtime)
    rule_payload={'revision':'R967_CONSERVATIVE_NUMERIC_DEV_RULE_V1','classification_hash':manifest_hash,'dependencies':numeric,'runtime':runtime}
    if proof_hash:rule_payload['continuation_proof_sha256']=proof_hash
    rule=digest(canonical(rule_payload).encode())
    return {'revision':'R967_SEMANTIC_BUILD_V1','training_rule_sha256':rule,'policy_hash':digest(policy_json.encode()),
            'policy_json':policy_json,'manifest_sha256':manifest_hash,'controlled_file_count':len(actual),'numeric_dependency_count':len(numeric),
            'source_provenance_sha256':source_provenance,'dependencies':numeric,
            'compatible_predecessor_rule_sha256':predecessors[0] if predecessors else '',
            'compatible_predecessor_rules_sha256':predecessors,'continuation_proof_sha256':proof_hash,
            'compatibility_scope':'DEV_ONLY; historical_evidence_and_certificate_compatibility_NOT_GRANTED'}

def output_text(value):
    fields={'TRAINING_RULE_SHA256':value['training_rule_sha256'],'POLICY_HASH':value['policy_hash'],
            'POLICY_JSON':value['policy_json'],'DEPENDENCY_MANIFEST_SHA256':value['manifest_sha256'],
            'COMPATIBLE_PREDECESSOR_RULE_SHA256':value.get('compatible_predecessor_rule_sha256',''),
            'COMPATIBLE_PREDECESSOR_RULES_SHA256':','.join(value.get('compatible_predecessor_rules_sha256',[])),
            'CONTINUATION_PROOF_SHA256':value.get('continuation_proof_sha256','')}
    return ('package com.ethk3.novaonepro;\n/** Build-generated. Never edit; no authorization is implied. */\n'
            'public final class ResearchSemanticsGeneratedR967 {\n'+
            ''.join('    public static final String '+k+'=runtimeValue('+json.dumps(v,ensure_ascii=True)+');\n' for k,v in fields.items())+
            '    private static String runtimeValue(String value){return value;}\n'+
            '    private ResearchSemanticsGeneratedR967(){}\n}\n')

def write_outputs(root=ROOT):
    value=build(root);dest=root/GENERATED;report=root/REPORT
    dest.parent.mkdir(parents=True,exist_ok=True);report.parent.mkdir(parents=True,exist_ok=True)
    # The contract binds exact generated bytes, including UTF-8 encoding and LF.
    # Text-mode newline translation must not vary the identity between hosts.
    dest.write_bytes(output_text(value).encode('utf-8'))
    report.write_bytes((json.dumps(value,indent=2)+'\n').encode('utf-8'))
    print('R967_SEMANTIC_IDENTITY_GENERATED|'+value['training_rule_sha256']);return value

def verify_outputs(root=ROOT):
    value=build(root)
    if not (root/GENERATED).is_file() or (root/GENERATED).read_bytes()!=output_text(value).encode('utf-8'):
        raise ValueError('R967_GENERATED_IDENTITY_STALE')
    # Parsing alone accepts duplicate keys and conflates JSON numeric types.
    # Compare the canonical generator output before it can be used as evidence.
    if not (root/REPORT).is_file() or (root/REPORT).read_bytes()!=(json.dumps(value,indent=2)+'\n').encode('utf-8'):
        raise ValueError('R967_GENERATED_MANIFEST_STALE')
    print('R967_SEMANTIC_IDENTITY_VERIFIED|'+value['training_rule_sha256']);return value

def self_test():
    value=build();base=value['training_rule_sha256'];java=ROOT/'app/src/main/java/com/ethk3/novaonepro'
    reviewed=bool(value.get('continuation_proof_sha256'))
    for name in ('MainActivity.java','CompleteReportFileR927.java','r952/DeepLearningPresenterR952.java',
                 'AlgorithmGenomeVmR908.java','V51TrajectoryFeatureEngine.java','ResearchLogisticCalibrationR932.java'):
        try: changed=build(overrides={name:(java/name).read_bytes()+b'\n'})
        except ValueError as error:
            assert reviewed and any(x in str(error) for x in ('R973_CONTINUATION_SOURCE_NOT_REVIEWED','R82_INVALIDATION_SOURCE_NOT_REVIEWED')),str(error)
        else:
            assert not reviewed,'An unreviewed source change retained a continuation proof'
            presentation=name in ('MainActivity.java','CompleteReportFileR927.java','r952/DeepLearningPresenterR952.java')
            assert (changed['training_rule_sha256']==base)==presentation,name
    m=identity_json((ROOT/'release/release_manifest.json').read_bytes());m['versionCode']+=1
    assert build(release_override=m)['training_rule_sha256']==base
    m['schema']['userVersion']+=1
    try: changed=build(release_override=m)
    except ValueError as error:
        assert reviewed and any(x in str(error) for x in ('R973_CONTINUATION_RUNTIME_NOT_REVIEWED','R82_INVALIDATION_RUNTIME_NOT_REVIEWED')),str(error)
    else: assert not reviewed and changed['training_rule_sha256']!=base
    print('R973_SEMANTIC_IDENTITY_SELF_TEST_PASS|8 mutation checks|DEV_ONLY|no_certificate_migration')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--check',action='store_true');p.add_argument('--self-test',action='store_true');a=p.parse_args()
    if a.self_test:self_test()
    elif a.check:verify_outputs()
    else:write_outputs()
