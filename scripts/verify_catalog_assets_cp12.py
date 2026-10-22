#!/usr/bin/env python3
"""Verify the two immutable runtime catalogs, streaming bounded JSONL records.
Only --write generates the build manifest; default/--check fail on any drift.
"""
from pathlib import Path
import argparse, hashlib, json, re
from cp4_compile_catalog import ASSET_SHA256 as ATOM_SHA, ROWS as ATOM_ROWS, OUTPUT_TYPES, BINDING_CONTEXTS
from cp4_compile_molecule_seeds import ASSET_SHA256 as SEED_SHA, canonical, parse, validate_graph
ROOT=Path(__file__).resolve().parents[1]
def constant(source,name,kind):
    text=(ROOT/source).read_text(encoding='utf-8')
    pattern=r'\b'+re.escape(name)+(r'\s*=\s*"([^"\n]+)"' if kind=='str' else r'\s*=\s*(\d+)[Ll]?')
    found=re.findall(pattern,text)
    if len(found)!=1: raise ValueError('CATALOG_PIN_NOT_UNIQUE:'+source+':'+name)
    return found[0] if kind=='str' else int(found[0])
def verify(source,sha_name,row_name,fields,consumer,byte_name=None,atoms=None):
    asset=constant(source,'ASSET','str'); expected=constant(source,sha_name,'str'); count=constant(source,row_name,'int')
    approved = ('cp4_atom_registry.jsonl', ATOM_SHA, ATOM_ROWS) if atoms is None else ('research/cp4_molecule_seeds.jsonl', SEED_SHA, 4200)
    if (asset, expected, count) != approved:
        raise ValueError('CATALOG_APPROVED_PIN_DRIFT')
    path=ROOT/'app/src/main/assets'/asset
    digest=hashlib.sha256(); rows=0; size=0
    identities=set(); definitions={} if atoms is None else atoms
    with path.open('rb') as stream:
        while True:
            line=stream.readline(65537)
            if not line: break
            if len(line)>65536 or not line.endswith(b'\n'): raise ValueError('CATALOG_LINE_BOUND:'+asset)
            digest.update(line);size+=len(line);rows+=1
            obj=parse(line)
            if not isinstance(obj,dict) or set(obj)!=fields: raise ValueError('CATALOG_ROW_SCHEMA:'+asset+':'+str(rows))
            if atoms is None:
                spec=obj['spec']
                if not isinstance(spec,dict) or set(spec)!={'context','family','params','position','regime','transform','version'}:
                    raise ValueError('CATALOG_SPEC_SCHEMA')
                identity=hashlib.sha256(canonical(spec).encode()).hexdigest()
                if obj['specHash']!=identity or obj['atomId']!='ATOM2-'+identity[:24] or obj['atomId'] in identities:
                    raise ValueError('CATALOG_SPEC_HASH_OR_DUPLICATE')
                if type(spec['position']) is not int or not 1<=spec['position']<=10 or not isinstance(spec['params'],dict):
                    raise ValueError('CATALOG_POSITION_OR_PARAMS')
                state='REQUIRES_BINDING' if spec['context'] in BINDING_CONTEXTS else 'DISCOVERED'
                if obj['state']!=state or spec['context'] not in BINDING_CONTEXTS|{'PIT_DRAW'} or obj['outputType'] not in OUTPUT_TYPES:
                    raise ValueError('CATALOG_STATE_OR_TYPE')
                if type(obj['minimumHistory']) is not int or not 1<=obj['minimumHistory']<=5000 or not obj['missingPolicy']:
                    raise ValueError('CATALOG_HISTORY_OR_MISSING_POLICY')
                identities.add(obj['atomId']);definitions[obj['atomId']]=obj
            else:
                validate_graph(obj,atoms)
                identity=(obj['seedId'],obj['graph']['position'])
                if identity in identities:raise ValueError('CATALOG_SEED_DUPLICATE')
                identities.add(identity)
    if digest.hexdigest()!=expected or rows!=count: raise ValueError('CATALOG_PIN_DRIFT:'+asset)
    if byte_name and size!=constant(source,byte_name,'int'):raise ValueError('CATALOG_BYTE_DRIFT:'+asset)
    return dict(asset=asset,sha256=expected,rows=rows,bytes=size,pin_source=source,runtime_consumer=consumer),definitions
def main():
    p=argparse.ArgumentParser();g=p.add_mutually_exclusive_group();g.add_argument('--write',action='store_true');g.add_argument('--check',action='store_true');a=p.parse_args()
    base='app/src/main/java/com/ethk3/novaonepro/r952/'
    atom,definitions=verify(base+'AtomCatalogPinnedCP12.java','SHA256','ROWS',{'atomId','minimumHistory','missingPolicy','outputType','spec','specHash','state'},'AtomCatalogImportCP12','BYTES')
    seed,_=verify(base+'ModernSeedFactoryCP12.java','ASSET_SHA','TOTAL',{'seedId','graph','graphHash','sourceSha256'},'ModernSeedFactoryCP12',atoms=definitions)
    manifest={'schema':'CP12_CATALOG_MANIFEST_1','scope':'BUILD_IDENTITY_NOT_RUNTIME_QUALIFICATION','build_consumer':'verifyCatalogAssetsCP12','catalogs':[atom,seed]}
    dest=ROOT/'app/src/main/assets/cp4_catalog_manifest.json';data=json.dumps(manifest,ensure_ascii=False,sort_keys=True,indent=2)+'\n'
    if a.write:dest.write_text(data,encoding='utf-8')
    elif not dest.is_file() or dest.read_text(encoding='utf-8')!=data:raise ValueError('CATALOG_MANIFEST_DRIFT')
    print('CATALOG_IDENTITY_PASS|atoms=25350|seeds=4200|stream_line_cap=65536')
if __name__=='__main__':main()
