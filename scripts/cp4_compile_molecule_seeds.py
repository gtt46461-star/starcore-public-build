#!/usr/bin/env python3
"""Compile frozen feature seeds; reject invalid graphs before atomic publication.

--source-seeds verifies legacy420 source AST identities before expansion.
--compiled-specs validates the canonical4200 position-bound specifications.
The latter does not claim recovery or independent verification of legacy ASTs.
"""
import argparse
import collections
import csv
import hashlib
import json
import math
import os
from pathlib import Path
import re
import tempfile

ROOT = Path(__file__).resolve().parents[1]
ASSET_SHA256 = 'b4ab9a61e2a36a4a426d0588ffb52350a25c09165273f802b9aa4c63e3c726ad'


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)


def digest(value):
    return hashlib.sha256(value.encode()).hexdigest()


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('SEED_DUPLICATE_JSON_KEY')
        result[key] = value
    return result


def parse(line):
    return json.loads(line, object_pairs_hook=unique_object,
                      parse_constant=lambda value: (_ for _ in ()).throw(ValueError('SEED_NONFINITE')))


def validate_graph(row, atoms):
    if not isinstance(row, dict) or set(row) != {'seedId', 'sourceSha256', 'graphHash', 'graph'}:
        raise ValueError('SEED_ROW_KEYS')
    source = row['sourceSha256']
    if not isinstance(source, str) or not re.fullmatch('[0-9a-f]{64}', source) or row['seedId'] != 'MOL-' + source[:20]:
        raise ValueError('SEED_SOURCE_IDENTITY')
    graph = row['graph']
    if not isinstance(graph, dict) or set(graph) != {'revision', 'position', 'nodes', 'outputId', 'role', 'missing', 'pit'}:
        raise ValueError('SEED_GRAPH_KEYS')
    position = graph['position']
    if type(position) is not int or not 1 <= position <= 10:
        raise ValueError('SEED_POSITION')
    if (graph['revision'], graph['role'], graph['missing'], graph['pit']) != ('CP4_SEED_BINDING_1', 'FEATURE_ONLY', 'PROPAGATE', 'STRICT_PAST'):
        raise ValueError('SEED_FEATURE_CONTRACT')
    nodes = graph['nodes']
    if not isinstance(nodes, list) or not 1 <= len(nodes) <= 128:
        raise ValueError('SEED_NODE_BOUND')
    types, depths = {}, {}
    for ordinal, node in enumerate(nodes):
        if not isinstance(node, dict):
            raise ValueError('SEED_NODE_OBJECT')
        op = node.get('op')
        if set(node) != {'id', 'op', 'type', 'parents', 'parameters'} | ({'atomId'} if op == 'ATOM' else set()):
            raise ValueError('SEED_NODE_KEYS')
        name, parents, parameters = node['id'], node['parents'], node['parameters']
        if name != 'n' + str(ordinal) or not isinstance(parents, list) or any(p not in types for p in parents):
            raise ValueError('SEED_PARENT_ORDER')
        if not isinstance(parameters, list) or len(parameters) > 64:
            raise ValueError('SEED_PARAMETER_BOUND')
        for parameter in parameters:
            if not isinstance(parameter, str):
                raise ValueError('SEED_PARAMETER_TYPE')
            value = float.fromhex(parameter)
            if not math.isfinite(value) or value.hex() != parameter:
                raise ValueError('SEED_PARAMETER_CANONICAL_OR_FINITE')
        if op == 'ATOM':
            atom = atoms.get(node['atomId'])
            valid = not parents and not parameters and atom is not None and atom['spec']['position'] == position
            output = 'SCALAR'  # Runtime importer explicitly casts the declared Atom value.
        elif op == 'CONSTANT':
            valid, output = not parents and len(parameters) == 1, 'SCALAR'
        elif op in {'THRESHOLD_GE', 'THRESHOLD_GT', 'THRESHOLD_LE'}:
            valid, output = len(parents) == 2 and not parameters and all(types[p] == 'SCALAR' for p in parents), 'BOOL'
        elif op == 'WEIGHTED_SUM':
            valid, output = len(parents) >= 1 and len(parameters) == len(parents) and all(types[p] == 'SCALAR' for p in parents), 'SCALAR'
        elif op == 'CONDITIONAL':
            valid = len(parents) == 3 and not parameters and [types[p] for p in parents] == ['BOOL', 'SCALAR', 'SCALAR']
            output = 'SCALAR'
        else:
            raise ValueError('SEED_OPERATOR_UNSUPPORTED')
        if not valid or node['type'] != output:
            raise ValueError('SEED_OPERATOR_ARITY_TYPE_OR_ATOM')
        depth = 1 + max((depths[p] for p in parents), default=0)
        if depth > 12:
            raise ValueError('SEED_DEPTH_BOUND')
        types[name], depths[name] = output, depth
    if graph['outputId'] not in types:
        raise ValueError('SEED_ROOT')
    if row['graphHash'] != digest(canonical(graph)):
        raise ValueError('SEED_GRAPH_HASH')
    return row


def legacy_rows(source, plan, atoms):
    csv.field_size_limit(1000000)
    rows = list(csv.DictReader((plan / '09_ATOM_MASTER_CATALOG/ATOM_MASTER_CATALOG.csv').open(encoding='utf-8-sig')))
    registry = {(r['source_atom'], int(r['position'])): r['atom_id'] for r in rows}
    with (plan / '09_ATOM_MASTER_CATALOG/ATOM_ALIAS_MAP.csv').open(encoding='utf-8-sig') as stream:
        for r in csv.DictReader(stream):
            key = r['old_atom_id'], int(r['position'])
            if key in registry and registry[key] != r['canonical_atom_id']:
                raise ValueError('SEED_ALIAS_CONFLICT')
            registry[key] = r['canonical_atom_id']
    drift = {int(r['position']): r['atom_id'] for r in rows
             if r['family'] == 'DRIFT_JS' and json.loads(r['params']) == {'recent': 20, 'reference': 120}}
    source_ids = set()
    with source.open(encoding='utf-8') as stream:
        for raw in stream:
            old = parse(raw)
            if digest(canonical(old['ast'])) != old['signature_sha256'] or old['molecule_id'] != 'MOL-' + old['signature_sha256'][:20]:
                raise ValueError('SEED_LEGACY_SOURCE_HASH')
            if old['molecule_id'] in source_ids:
                raise ValueError('SEED_LEGACY_DUPLICATE')
            source_ids.add(old['molecule_id'])
            kind = old['kind']
            legacy_atoms = [n['id'] for n in old['ast']['nodes'] if n['op'] == 'AtomNode']
            if kind not in {'THRESHOLD', 'WEIGHTED_SUM', 'CONDITIONAL', 'REGIME_GATE'} or len(legacy_atoms) != (1 if kind == 'THRESHOLD' else 2):
                raise ValueError('SEED_LEGACY_KIND_OR_ARITY')
            for position in range(1, 11):
                nodes = []
                def node(op, typ, parents=(), parameters=(), atom=None):
                    item = {'id': 'n' + str(len(nodes)), 'op': op, 'type': typ,
                            'parents': list(parents), 'parameters': [float(v).hex() for v in parameters]}
                    if atom:
                        item['atomId'] = atom
                    nodes.append(item)
                    return item['id']
                refs = [node('ATOM', 'SCALAR', atom=registry[(name, position)]) for name in legacy_atoms]
                if kind == 'THRESHOLD':
                    constant = node('CONSTANT', 'SCALAR', parameters=[.3])
                    output = node('THRESHOLD_GE', 'BOOL', [refs[0], constant])
                elif kind == 'WEIGHTED_SUM':
                    output = node('WEIGHTED_SUM', 'SCALAR', refs, [.65, .35])
                elif kind == 'CONDITIONAL':
                    zero = node('CONSTANT', 'SCALAR', parameters=[0])
                    guard = node('THRESHOLD_GT', 'BOOL', [refs[0], zero])
                    output = node('CONDITIONAL', 'SCALAR', [guard, refs[1], zero])
                else:
                    js = node('ATOM', 'SCALAR', atom=drift[position])
                    cut = node('CONSTANT', 'SCALAR', parameters=[.1])
                    guard = node('THRESHOLD_LE', 'BOOL', [js, cut])
                    output = node('CONDITIONAL', 'SCALAR', [guard, refs[0], refs[1]])
                graph = {'revision': 'CP4_SEED_BINDING_1', 'position': position, 'nodes': nodes,
                         'outputId': output, 'role': 'FEATURE_ONLY', 'missing': 'PROPAGATE', 'pit': 'STRICT_PAST'}
                yield validate_graph({'seedId': old['molecule_id'], 'sourceSha256': old['signature_sha256'],
                                      'graphHash': digest(canonical(graph)), 'graph': graph}, atoms)
    if len(source_ids) != 420:
        raise ValueError('SEED_LEGACY_SOURCE_COUNT')


def compile_seeds(source, atoms, output, plan=None, legacy=False, check=False):
    source, output = Path(source), Path(output)
    if source.resolve() == output.resolve():
        raise ValueError('SEED_INPUT_OUTPUT_COLLISION')
    output.parent.mkdir(parents=True, exist_ok=True)
    seen, seeds, positions, graphs = set(), set(), collections.Counter(), set()
    h = hashlib.sha256()
    descriptor, name = tempfile.mkstemp(dir=output.parent, suffix='.partial')
    count = 0
    try:
        with os.fdopen(descriptor, 'wb') as destination, source.open(encoding='utf-8') as stream:
            rows = legacy_rows(source, plan, atoms) if legacy else (parse(line) for line in stream)
            for row in rows:
                validate_graph(row, atoms)
                key = row['seedId'], row['graph']['position']
                if key in seen:
                    raise ValueError('SEED_DUPLICATE_POSITION_ID')
                seen.add(key)
                seeds.add(row['seedId'])
                positions[key[1]] += 1
                graphs.add(row['graphHash'])
                line = (canonical(row) + '\n').encode()
                if len(line) > 32768:
                    raise ValueError('SEED_LINE_BOUND')
                destination.write(line)
                h.update(line)
                count += 1
            if count != 4200 or len(seeds) != 420 or positions != {p: 420 for p in range(1, 11)}:
                raise ValueError('SEED_FROZEN_CARDINALITY')
            if h.hexdigest() != ASSET_SHA256:
                raise ValueError('SEED_FROZEN_ASSET_DRIFT')
            destination.flush()
            os.fsync(destination.fileno())
        if check:
            if not output.is_file() or Path(name).read_bytes() != output.read_bytes():
                raise ValueError('SEED_OUTPUT_DRIFT')
        else:
            os.replace(name, output)
        return {'rows': count, 'sourceSeeds': len(seeds), 'uniqueGraphs': len(graphs),
                'assetSha256': h.hexdigest(), 'sourceSha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                'provenanceBasis': 'LEGACY420_SOURCE_AST_VERIFIED' if legacy else 'CANONICAL4200_GRAPH_SPECS; LEGACY420_SOURCE_AST_NOT_REVERIFIED',
                'scope': 'FEATURE_DEFINITIONS_ONLY_NOT_RUNTIME_QUALIFICATION'}
    finally:
        if os.path.exists(name):
            os.unlink(name)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    g = p.add_mutually_exclusive_group(required=True)
    g.add_argument('--source-seeds', type=Path)
    g.add_argument('--compiled-specs', type=Path)
    p.add_argument('--plan-root', type=Path)
    p.add_argument('--atom-asset', type=Path, default=ROOT / 'app/src/main/assets/cp4_atom_registry.jsonl')
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--check', action='store_true')
    a = p.parse_args()
    if a.source_seeds and a.plan_root is None:
        p.error('--source-seeds requires --plan-root')
    with a.atom_asset.open(encoding='utf-8') as stream:
        atoms = {}
        for raw in stream:
            atom = parse(raw)
            if atom['atomId'] in atoms:
                raise ValueError('SEED_DUPLICATE_ATOM_ID')
            atoms[atom['atomId']] = atom
    print(json.dumps(compile_seeds(a.source_seeds or a.compiled_specs, atoms, a.output,
                                   a.plan_root, bool(a.source_seeds), a.check)))


if __name__ == '__main__':
    try:
        main()
    except (ValueError, KeyError, OSError, TypeError) as error:
        raise SystemExit(str(error)) from error
