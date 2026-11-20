"""Reproduce exact additional stage images with the unchanged R967/R82 identity algorithms.

This is an authoring tool. Contractors consume OVERLAY.json via stage_checked.py.
The input baseline and original sealed plan are read only; output must be new.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import sys

BASE = 'bb7b49bbcc9266bad548ec37e71403256fc06a4d'
SCHEMA = 'STARCORE_R335_PRODUCTION_REVIEW_20261120'
SEMANTIC = ('release/research_dependencies_r967.json', 'release/r82_training_invalidation.json')
GENERATED = ('app/build/generated/r967/semantic_manifest.json',
             'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java')
R967 = '60e4a4f535dd79126a4ab4ba1580aa8afba3cda3b53d8347dd3ee59e8379d85e'
R815 = '7c3488045682b4df49c42140a5befe9eb83e386122e6f4232b685a272b94a169'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def need(ok, message):
    if not ok:
        raise ValueError(message)


def load(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def json_file(path):
    return json.loads(path.read_bytes())


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def safe_relative(value):
    path = PurePosixPath(value)
    need(isinstance(value, str) and path.as_posix() == value and not path.is_absolute()
         and all(p not in ('.', '..', '.git') for p in path.parts)
         and '\\' not in value and ':' not in value, 'INVALID_SOURCE_PATH:' + str(value))
    return value


def copy_input(source, destination):
    need(source.is_file() and not source.is_symlink(), 'NONREGULAR_INPUT:' + str(source))
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, destination)


def produce(plan, baseline, review, out):
    for path in (plan, baseline, review):
        need(path.is_dir(), 'INPUT_DIRECTORY_REQUIRED:' + str(path))
        need(not out.is_relative_to(path) and not path.is_relative_to(out), 'SEPARATE_OUTPUT_REQUIRED')
    need(not out.exists(), 'NEW_OUTPUT_REQUIRED')
    need(sha((baseline / 'scripts/r967_semantic_identity.py').read_bytes()) == R967, 'ORIGINAL_R967_REQUIRED')
    need(sha((baseline / 'scripts/r815_generate_provenance.py').read_bytes()) == R815, 'ORIGINAL_R815_REQUIRED')
    module = load(baseline / 'scripts/r967_semantic_identity.py', 'unchanged_r967_review')
    identity = load(plan / 'verification/build_semantic_inputs_r335.py', 'unchanged_r335_identity')
    pair = load(review / 'research/b05_pair_source.py', 'reviewed_b05_pair')
    installed = json_file(plan / 'control/APPLY_MANIFEST_R335.json')
    need(installed['sourceBaseline'] == BASE, 'WRONG_BASELINE')
    provenance = load(baseline / 'scripts/r815_generate_provenance.py', 'baseline_provenance')
    sources = provenance.records(baseline, [p for p in provenance.files(baseline, 'app/src/main/java')
        if p.suffix == '.java' and p.relative_to(baseline).as_posix() != provenance.FACADE])
    need(provenance.record_hash(sources) == identity.BASE_TREE, 'EXACT_033_JAVA_REQUIRED')
    inputs = json_file(review / 'production/REVIEW_INPUTS.json')
    need(inputs['sourceBaseline'] == BASE, 'INPUT_BASELINE_MISMATCH')
    allowed_stages = [b['id'] for b in installed['batches']]
    need(allowed_stages == ['P00', *('B%02d' % i for i in range(1, 9))], 'EXACT_STAGE_SET_REQUIRED')
    additions = {}
    for item in inputs['files']:
        need(item['stage'] in allowed_stages, 'UNKNOWN_REVIEW_STAGE:' + str(item['stage']))
        key = (item['stage'], safe_relative(item['path']))
        need(key not in additions, 'DUPLICATE_REVIEW_SOURCE')
        path = review / safe_relative(item['payload'])
        need(path.resolve().is_relative_to(review) and sha(path.read_bytes()) == item['sha256'], 'REVIEW_SOURCE_DRIFT')
        additions[key] = item
    source_reviews = json_file(plan / 'control/SEMANTIC_SOURCE_REVIEWS_R335.json')['reviews']
    original_reviews = {(r['stage'], r['path']): r for r in source_reviews}
    need(len(original_reviews) == len(source_reviews), 'DUPLICATE_ORIGINAL_SOURCE_REVIEW')
    initial = module.build(baseline)
    need(initial['compatible_predecessor_rules_sha256'] == [], 'BASELINE_QUALIFICATION_BOUNDARY')
    current_manifest = json_file(baseline / SEMANTIC[0])
    original_proof = json_file(baseline / SEMANTIC[1])
    names = set(module.controlled_paths(baseline)) | set(provenance.BUILD_CONTRACTS) | set(SEMANTIC) | set(GENERATED)
    names |= {'release/release_manifest.json', 'scripts/r975_source_call_index.py'}
    names |= {r['path'] for b in installed['batches'] for r in b['files']}
    names |= {p for _, p in additions}
    work = out / 'candidate'
    work.mkdir(parents=True)
    for name in sorted(names):
        if (baseline / name).is_file():
            copy_input(baseline / name, work / name)
    production = out / 'production'
    stages = {}
    records = []
    reviewed = {}
    custom_paths = set()
    effective_counts = []
    consumed = set()
    for batch in installed['batches']:
        sid = batch['id']
        rows = {r['path']: r for r in batch['files']}
        need(len(rows) == len(batch['files']), 'DUPLICATE_ORIGINAL_STAGE_PATH:' + sid)
        originals = {name: (plan / row['payload']).read_bytes() for name, row in rows.items()}
        for name, data in originals.items():
            need(sha(data) == rows[name]['afterSha256'], 'ORIGINAL_AFTER_DRIFT:' + sid + ':' + name)
        old_effective = dict(originals)
        temp = out / 'old-pair' / sid
        if sid == 'B05':
            old_effective[pair.REL.as_posix()] = pair.generate(plan, temp).read_bytes()
        if sid in ('B05', 'B07'):
            old_effective[SEMANTIC[1]] = pair.generate_identity(plan, sid, temp).read_bytes()
        possible = set(rows) | set(SEMANTIC) | set(GENERATED) | {p for s, p in additions if s == sid}
        before = {name: (work / name).read_bytes() if (work / name).exists() else None for name in possible}
        for name, data in old_effective.items():
            if name in SEMANTIC:
                continue
            row = rows[name]
            if name in custom_paths and (sid, name) not in additions:
                need(before[name] is not None and sha(before[name]) == row['beforeSha256'],
                     'LATER_STAGE_WOULD_REVERT_REVIEW:' + sid + ':' + name)
            path = work / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
            if (sid, name) in original_reviews:
                reviewed[name] = dict(original_reviews[(sid, name)])
        if sid == 'B05':
            reviewed[pair.REL.as_posix()].update(
                reviewer='R33.5 2026-11-19 source review',
                rationale='B05 RE24 exact paired-window timing, missing and pending ordinal accounting; preserve NUMERIC role and invalidate old qualification.')
            custom_paths.add(pair.REL.as_posix())
        for (stage, name), item in additions.items():
            if stage != sid:
                continue
            current = work / name
            actual = sha(current.read_bytes()) if current.exists() else 'NEW'
            need(actual == item['inputSha256'], 'REVIEW_INPUT_DRIFT:' + sid + ':' + name)
            copy_input(review / item['payload'], current)
            reviewed[name] = {k: item[k] for k in ('reviewer', 'rationale', 'dynamicReview')}
            custom_paths.add(name)
            consumed.add((stage, name))
        value, current_manifest = identity.identity(module, work, current_manifest, original_proof, reviewed)
        need(value['policy_hash'] == initial['policy_hash'] and value['compatible_predecessor_rules_sha256'] == [],
             'POLICY_OR_CERTIFICATE_CONTINUATION_CHANGED')
        module.write_outputs(work)
        module.verify_outputs(work)
        entries = []
        for name in sorted(possible):
            data = (work / name).read_bytes() if (work / name).is_file() else None
            need(data is not None, 'MISSING_STAGE_AFTER:' + sid + ':' + name)
            original = old_effective.get(name)
            changed_image = original is not None and data != original
            changed_preimage = (name in rows and
                ('NEW' if before[name] is None else sha(before[name])) != rows[name]['beforeSha256'])
            extra = original is None and before[name] != data
            # The old B07 adapter already carries the reviewed B05 R82 predecessor.
            if sid == 'B07' and name == SEMANTIC[1]:
                expected = pair.generate_identity(plan, 'B05', out / 'old-b07-before').read_bytes()
                changed_preimage = before[name] != expected
            if not (changed_image or changed_preimage or extra):
                continue
            payload = 'production/images/' + sid + '/' + name
            target = out / payload
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            old_path = None
            if before[name] is not None:
                old_path = 'production/before/' + sid + '/' + name
                path = out / old_path
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(before[name])
            entries.append({'path': name, 'originalAfterSha256': sha(original) if original is not None else None,
                'beforeSha256': sha(before[name]) if before[name] is not None else 'NEW',
                'afterSha256': sha(data), 'beforeFile': old_path, 'payload': payload,
                'mode': '100644', 'action': 'REPLACE' if before[name] is not None else 'ADD'})
        stages[sid] = {'files': entries}
        effective = set(rows) | {r['path'] for r in entries}
        effective_counts.append({'stage': sid, 'originalOperations': len(rows), 'effectiveOperations': len(effective),
                                 'additionalOperations': len(effective - set(rows))})
        records.append({'stage': sid, 'trainingRuleSha256': value['training_rule_sha256'],
            'policyHash': value['policy_hash'], 'compatiblePredecessorRules': [],
            'controlledFiles': value['controlled_file_count'], 'numericDependencies': value['numeric_dependency_count'],
            'semanticFiles': {name: sha((work / name).read_bytes()) for name in (*SEMANTIC, *GENERATED)},
            'scope': 'ORIGINAL_R967_BUILD_AND_VERIFY; NOT_ANDROID_APK_OR_DEVICE'})
        print(sid, len(entries), value['training_rule_sha256'], flush=True)
    need(consumed == set(additions), 'UNCONSUMED_REVIEW_SOURCE')
    overlay = {'schema': SCHEMA, 'sourceBaseline': BASE, 'stages': stages}
    write_json(production / 'OVERLAY.json', overlay)
    write_json(production / 'SEMANTIC_RESULTS.json', {'sourceBaseline': BASE,
        'sourceReviewsSha256': sha((review / 'production/REVIEW_INPUTS.json').read_bytes()),
        'policyUnchanged': True, 'oldQualificationCompatibilityGranted': False,
        'generatorSha256': {'r967': R967, 'r815Baseline': R815}, 'stages': records,
        'fileOperations': effective_counts})
    return overlay


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for field in ('plan', 'baseline', 'review', 'out'):
        parser.add_argument('--' + field, required=True, type=Path)
    args = parser.parse_args()
    produce(*(getattr(args, field).resolve() for field in ('plan', 'baseline', 'review', 'out')))
