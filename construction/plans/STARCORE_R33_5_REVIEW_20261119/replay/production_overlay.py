"""Compose sealed review production replacements into the original installer contract.

This creates one temporary complete effective plan/ledger. It does not rewrite
historical package bytes or manufacture test/build/qualification acceptance.
"""
import csv
import hashlib
import importlib.util
import json
from pathlib import Path, PurePosixPath

SCHEMA = 'STARCORE_R335_PRODUCTION_REVIEW_20261120'
INVALIDATION = 'release/r82_training_invalidation.json'


def sha(body):
    return hashlib.sha256(body).hexdigest()


def safe(root, name):
    if not isinstance(name, str):
        raise ValueError('OVERLAY_MEMBER_PATH_REQUIRED')
    logical = PurePosixPath(name)
    if (not name or logical.is_absolute() or logical.as_posix() != name
            or any(p in ('', '.', '..', '.git') for p in logical.parts)
            or '\\' in name):
        raise ValueError('OVERLAY_UNSAFE_PATH:' + name)
    root = Path(root).resolve()
    path = root.joinpath(*logical.parts)
    if (path.is_symlink() or not path.resolve().is_relative_to(root)
            or any(p.is_symlink() for p in path.parents if p != root and p.is_relative_to(root))):
        raise ValueError('OVERLAY_SYMLINK_OR_ESCAPE:' + name)
    return path


def read_image(review, relative, digest):
    path = safe(review, relative)
    if not path.is_file():
        raise ValueError('OVERLAY_IMAGE_MISSING:' + relative)
    data = path.read_bytes()
    if sha(data) != digest:
        raise ValueError('OVERLAY_IMAGE_DIGEST:' + relative)
    return data


def prepare(tool, plan, pair, review, output):
    """Return effective manifest, preserving every original stage and old pair fix."""
    review = Path(review).resolve()
    descriptor = safe(review, 'production/OVERLAY.json')
    spec = json.loads(descriptor.read_text(encoding='utf-8'))
    if (spec.get('schema') != SCHEMA or spec.get('sourceBaseline') != tool.BASE
            or not isinstance(spec.get('stages'), dict) or not spec['stages']
            or set(spec['stages']) - set(tool.STAGES)):
        raise ValueError('OVERLAY_SCHEMA_OR_BASELINE')
    plan = Path(plan).resolve()
    manifest = json.loads((plan / 'control/APPLY_MANIFEST_R335.json').read_text(encoding='utf-8'))
    result = json.loads(json.dumps(manifest))
    # Resolve every original image before writing the effective tool contract.
    for batch in result['batches']:
        stage = batch['id']
        originals = tool.materialize(stage, tool.records(stage))
        rows = {row['path']: row for row in batch['files']}
        after = dict(originals)
        before = {}
        for name, row in rows.items():
            before[name] = None if row['action'] == 'ADD' else (plan / row['beforeFile']).read_bytes()
        if stage in ('B05', 'B07'):
            generated = output / 'pair-generation' / stage
            if stage == 'B05':
                after[pair.REL.as_posix()] = pair.generate(plan, generated).read_bytes()
            after[INVALIDATION] = pair.generate_identity(plan, stage, generated).read_bytes()
            if stage == 'B07':
                before[INVALIDATION] = pair.generate_identity(plan, 'B05', generated / 'prior').read_bytes()
        changes = spec['stages'].get(stage, {}).get('files', [])
        if not isinstance(changes, list) or len(changes) != len({r.get('path') for r in changes}):
            raise ValueError('OVERLAY_DUPLICATE_OR_INVALID_FILES:' + stage)
        for patch in changes:
            name = patch['path']
            safe(output, name)
            existing = after.get(name)
            original_after = None if existing is None else sha(existing)
            if patch.get('originalAfterSha256') != original_after:
                raise ValueError('OVERLAY_ORIGINAL_AFTER_DRIFT:' + stage + ':' + name)
            action = patch.get('action')
            mode = patch.get('mode')
            if mode not in ('100644', '100755') or action not in ('ADD', 'REPLACE'):
                raise ValueError('OVERLAY_ACTION_OR_MODE:' + stage + ':' + name)
            if action == 'ADD':
                if patch.get('beforeSha256') != 'NEW' or patch.get('beforeFile') is not None:
                    raise ValueError('OVERLAY_ADD_PREIMAGE:' + stage + ':' + name)
                before[name] = None
            else:
                before[name] = read_image(review, patch['beforeFile'], patch['beforeSha256'])
            after[name] = read_image(review, patch['payload'], patch['afterSha256'])
            rows[name] = dict(rows.get(name, {}), path=name, action=action, mode=mode)
        for name, row in rows.items():
            prior = before[name]
            row['beforeFile'] = None if prior is None else f'{stage}/before_full/{name}'
            row['beforeSha256'] = 'NEW' if prior is None else sha(prior)
            row['action'] = 'ADD' if prior is None else 'REPLACE'
            row['payload'] = f'{stage}/after_full/{name}'
            row['afterSha256'] = sha(after[name])
            if row.get('reviewedSource'):
                row['originalReviewedSource'] = row['reviewedSource']
            row['reviewedSource'] = row['payload']
            for relative, body in ((row['beforeFile'], prior), (row['payload'], after[name])):
                if relative is None:
                    continue
                path = safe(output, relative)
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(body)
        batch['files'] = list(rows.values())
        if 'fileCount' in batch:
            batch['fileCount'] = len(batch['files'])
    result['batchCount'] = len(result['batches'])
    result['fileChangeCount'] = sum(len(b['files']) for b in result['batches'])
    result['distinctPathCount'] = len({r['path'] for b in result['batches'] for r in b['files']})
    verifier_spec = importlib.util.spec_from_file_location('r335_overlay_manifest_verifier',
        plan / 'verification/verify_baseline_source_r335.py')
    verifier = importlib.util.module_from_spec(verifier_spec)
    verifier_spec.loader.exec_module(verifier)
    structural = verifier.validate_install_manifest(output, result)
    control = output / 'control'
    control.mkdir(parents=True, exist_ok=True)
    (control / 'APPLY_MANIFEST_R335.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    with tool.LEDGER.open(encoding='utf-8-sig', newline='') as source:
        old = csv.DictReader(source)
        fields = old.fieldnames
        old_rows = {(r['批次'], r['仓库路径']): r for r in old}
    with (control / tool.LEDGER.name).open('w', encoding='utf-8-sig', newline='') as dest:
        writer = csv.DictWriter(dest, fieldnames=fields)
        writer.writeheader()
        for batch in result['batches']:
            for row in batch['files']:
                record = dict(old_rows.get((batch['id'], row['path']), {field: '' for field in fields}))
                record.update({'批次': batch['id'], '仓库路径': row['path'],
                               '前SHA256': row['beforeSha256'], '后SHA256': row['afterSha256'],
                               '本批逐文件补丁或完整后态': row['payload']})
                writer.writerow(record)
    return {'overlaySha256': sha(descriptor.read_bytes()),
            'effectiveManifestSha256': sha((control / 'APPLY_MANIFEST_R335.json').read_bytes()),
            'manifestValidation': structural,
            'stageOperations': sum(len(b['files']) for b in result['batches']),
            'distinctProductionPaths': len({r['path'] for b in result['batches'] for r in b['files']})}
