#!/usr/bin/env python3
from pathlib import Path
import json
import re
import sys

try:
    import javalang
except Exception as exc:
    print(json.dumps({'ok': False, 'fatal': 'javalang_missing', 'error': repr(exc)}, ensure_ascii=False, indent=2))
    raise SystemExit(2)

ROOT = Path(__file__).resolve().parents[1]
PKG = ROOT / 'app/src/main/java/com/ethk3/novaonepro'
VERSION = 'V45_PIPELINE_CONTRACT_RANKING_20260720'
checks = []

def add(name, ok, detail=''):
    checks.append({'name': name, 'ok': bool(ok), 'detail': str(detail)})

def text(name):
    return (PKG / name).read_text(encoding='utf-8')

java_files = list((ROOT / 'app/src/main/java').rglob('*.java'))
parse_errors = []
lambdas = []
bad_imports = []
for p in java_files:
    rel = str(p.relative_to(ROOT))
    src = p.read_text(encoding='utf-8')
    try:
        tree = javalang.parse.parse(src)
        for _, node in tree:
            if isinstance(node, javalang.tree.LambdaExpression):
                lambdas.append(rel)
        for imp in tree.imports:
            q = imp.path
            if not (q.startswith('java.') or q.startswith('android.') or q.startswith('org.json') or q.startswith('com.ethk3.novaonepro')):
                bad_imports.append(rel + ':' + q)
    except Exception as exc:
        parse_errors.append({'file': rel, 'error': repr(exc)})

add('java_parse_all', not parse_errors, json.dumps(parse_errors, ensure_ascii=False))
add('java7_no_lambda', not lambdas, json.dumps(lambdas, ensure_ascii=False))
add('no_third_party_runtime_import', not bad_imports, json.dumps(bad_imports, ensure_ascii=False))

required = [
    'PredictionEnvelope.java',
    'CompiledActionContract.java',
    'PredictionLifecycleTraceEngine.java',
    'EvidenceGroupAggregatorEngine.java',
    'RankingAuditEngine.java',
    'CoverageFrontierEngine.java',
]
for name in required:
    add('required_' + name.replace('.', '_'), (PKG / name).exists(), name)

pipeline = text('PipelineInvariantGuard.java')
unified = text('UnifiedStrategyEvidenceEngine.java')
lane = text('LanePolicyCompiler.java')
auth = text('DecisionAuthorizationEngine.java')
oos = text('OosEligibilityEngine.java')
prob = text('RealtimeCausalProbabilityEngine.java')
envelope = text('PredictionEnvelope.java')
contract = text('CompiledActionContract.java')
groups = text('EvidenceGroupAggregatorEngine.java')
models = text('Models.java')

add('lock_final_pre_lane', 'ensureFinalLock' in pipeline and 'lockFinalPrediction' in pipeline and 'FINAL_PRE_LANE' in pipeline)
add('lock_payload_identity_version_snapshot', all(x in pipeline for x in ['candidateHash', 'snapshotMaxIssue', 'calibrationVersion', 'pipelineVersion', 'k3Mask']))
add('no_mid_pipeline_lock', 'PipelineInvariantGuard.lockProbability(c)' not in unified and 'LOCK_PENDING' in unified)
add('canonical_probability_sync', all(x in envelope for x in ['calibratedHitProbability = out.canonicalProbability', 'finalP = out.canonicalProbability', 'v43SafeProbability = out.canonicalProbability']))
add('lane_single_compile_cache', all(x in lane for x in ['RESULT_CACHE', 'getCompiledResult', 'cacheOnly', 'CompiledActionContract.publish']))
add('lane_evidence_group_quorum', all(x in lane for x in ['EvidenceGroupAggregatorEngine.evaluate', 'positiveGroups >= 3', 'evidenceGroups < 4']))
add('authorization_verify_only', all(x in auth for x in ['LanePolicyCompiler.getCompiledResult', 'CompiledActionContract.getEligible', 'verifyBeforeAuthorization']))
add('authorization_no_recompile', 'LanePolicyCompiler.compileForLane' not in auth and 'LanePolicyCompiler.compile(' not in auth)
add('authorization_duplicate_attempt_guard', 'markAuthorizationAttempt' in auth)
add('oos_consumes_policy_probability', 'out.probability = policy.probability' in oos and 'applyEffectiveGate(c, out, policy.probability, policy.gate)' in oos)
add('probability_first_sort', 'Double.compare(rankingProbability(b), rankingProbability(a))' in prob)
add('ranking_three_surfaces', all(x in text('RankingAuditEngine.java') for x in ['probabilityRank', 'expectedValueRank', 'actionRank']))
add('coverage_frontier_targets', all(x in text('CoverageFrontierEngine.java') for x in ['0.05d', '0.10d', '0.15d', '0.20d', '0.25d']))
add('machine_codes_not_translated_by_substring', 'machineCode(' in lane and 'replace("MISS"' not in lane and 'replace("MISS"' not in auth)

# Verify critical fields used directly by new classes exist on Models.  Gradle remains the final type check.
critical_fields = [
    'v43ProbabilityLockHash', 'v43ProbabilityLock', 'v43SafeProbability',
    'calibratedHitProbability', 'honestProbability', 'probabilityPipelineVersion',
    'candidateHash', 'snapshotMaxIssue', 'featureSchemaVersion', 'rankingVersion',
    'calibrationVersion', 'pipelineVersion', 'contractDecisionId', 'contractValid',
    'hardBlockFlag', 'hardBlockSource', 'v43EvidenceGroupCount', 'v43HardRisk',
    'coverageQuality', 'thirdQuality', 'counterRisk', 'posteriorSample',
    'familyWilsonLower', 'promotionWilsonLowerBound', 'actionUncertaintyPenalty'
]
missing_fields = []
for field in critical_fields:
    if not re.search(r'\b(?:public\s+)?(?:int|long|double|boolean|String)\s+' + re.escape(field) + r'\b', models):
        missing_fields.append(field)
add('critical_model_fields_exist', not missing_fields, ','.join(missing_fields))

# Preserve fixed single-position K3 invariants.
app = text('AppConfig.java')
add('fixed_k3', 'FIXED_K = 3' in app)
add('multi_position_disabled', 'ALLOW_MULTI_POSITION_BET = false' in app)
add('decision_gate_only_trade_owner', 'DecisionGateEngine' in text('OosEligibilityEngine.java') or 'never creates BET' in text('OosEligibilityEngine.java'))

ok = all(c['ok'] for c in checks)
out = {
    'version': VERSION,
    'ok': ok,
    'java_files': len(java_files),
    'checks': checks,
}
print(json.dumps(out, ensure_ascii=False, indent=2))
raise SystemExit(0 if ok else 1)
