#!/usr/bin/env python3
"""Compile the complete frozen CP12 CSV without publishing partial output."""
import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import tempfile

ROWS = 25350
FAMILIES = 28
BINDINGS = 130
ASSET_SHA256 = '57673f6000cd5db5ae4be162430086753c879b8c63d5ef1d2bb521169cdcdefe'
BINDING_CONTEXTS = {'strategy_version', 'candidate_set'}
OUTPUT_TYPES = {'BOOLEAN', 'PROBABILITY', 'SCALAR', 'SCALAR_REGIME_SCORE', 'ORDINAL'}


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'), allow_nan=False)


def compile_catalog(catalog, output):
    catalog, output = Path(catalog), Path(output)
    if catalog.resolve() == output.resolve():
        raise ValueError('CATALOG_INPUT_OUTPUT_COLLISION')
    output.parent.mkdir(parents=True, exist_ok=True)
    descriptor, name = tempfile.mkstemp(dir=output.parent, suffix='.partial')
    ids, hashes, families = set(), set(), set()
    count = requires = 0
    digest = hashlib.sha256()
    try:
        with os.fdopen(descriptor, 'wb') as dst, catalog.open(encoding='utf-8-sig', newline='') as src:
            for r in csv.DictReader(src):
                spec = {k: r[k] for k in ('family', 'version', 'transform', 'regime', 'context')}
                spec['params'] = json.loads(r['params'])
                spec['position'] = int(r['position'])
                if not isinstance(spec['params'], dict) or not 1 <= spec['position'] <= 10:
                    raise ValueError('CATALOG_INVALID_SPEC')
                h = hashlib.sha256(canonical(spec).encode()).hexdigest()
                if h != r['spec_hash'] or r['atom_id'] != 'ATOM2-' + h[:24]:
                    raise ValueError('CATALOG_SPEC_IDENTITY')
                if h in hashes or r['atom_id'] in ids:
                    raise ValueError('CATALOG_DUPLICATE')
                hashes.add(h)
                ids.add(r['atom_id'])
                minimum = int(r['minimum_history'])
                if not 1 <= minimum <= 5000 or r['output'] not in OUTPUT_TYPES or not r['missing_policy']:
                    raise ValueError('CATALOG_INVALID_TYPE_HISTORY_OR_MISSING_POLICY')
                if spec['context'] not in BINDING_CONTEXTS | {'PIT_DRAW'}:
                    raise ValueError('CATALOG_INVALID_CONTEXT')
                # The CSV lifecycle column is the full contract text, not an enum.
                # Definition discovery cannot grant ACTIVE, qualification or REAL.
                state = 'REQUIRES_BINDING' if spec['context'] in BINDING_CONTEXTS else 'DISCOVERED'
                row = {'atomId': r['atom_id'], 'specHash': h, 'spec': spec,
                       'outputType': r['output'], 'minimumHistory': minimum,
                       'state': state, 'missingPolicy': r['missing_policy']}
                b = (canonical(row) + '\n').encode()
                if len(b) > 8192:
                    raise ValueError('CATALOG_LINE_BOUND')
                dst.write(b)
                digest.update(b)
                count += 1
                families.add(r['family'])
                requires += state == 'REQUIRES_BINDING'
            if (count, len(families), requires) != (ROWS, FAMILIES, BINDINGS):
                raise ValueError('CATALOG_CARDINALITY:' + str((count, len(families), requires)))
            if digest.hexdigest() != ASSET_SHA256:
                raise ValueError('CATALOG_FROZEN_ASSET_DRIFT')
            dst.flush()
            os.fsync(dst.fileno())
        manifest = {'revision': 'CP12_CATALOG_ASSET_1', 'atoms': count,
                    'families': len(families), 'requiresBinding': requires,
                    'assetSha256': digest.hexdigest(), 'schema': 'research_atom_spec_v2',
                    'runtimePolicy': 'activeClosure<=256; no full eager evaluation',
                    'scope': 'FROZEN_DEFINITIONS_ONLY_NOT_RUNTIME_QUALIFICATION'}
        os.replace(name, output)
        manifest_path = output.with_suffix('.manifest.json')
        descriptor, meta_name = tempfile.mkstemp(dir=output.parent, suffix='.partial')
        try:
            with os.fdopen(descriptor, 'w') as stream:
                stream.write(json.dumps(manifest, indent=2) + '\n')
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(meta_name, manifest_path)
        finally:
            if os.path.exists(meta_name):
                os.unlink(meta_name)
        return manifest
    finally:
        if os.path.exists(name):
            os.unlink(name)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--catalog', required=True)
    p.add_argument('--output', required=True)
    a = p.parse_args()
    print(json.dumps(compile_catalog(a.catalog, a.output)))


if __name__ == '__main__':
    try:
        main()
    except (ValueError, KeyError, OSError) as error:
        raise SystemExit(str(error)) from error
