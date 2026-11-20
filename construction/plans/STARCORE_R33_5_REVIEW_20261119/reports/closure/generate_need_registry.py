#!/usr/bin/env python3
"""Generate the production Java need registry from the reviewed finite JSON catalog."""
from pathlib import Path
import argparse,hashlib,json,sys

def generate(catalog,plan):
    needs=json.loads(catalog.read_text(encoding='utf-8'))
    if not isinstance(needs,list) or not 1<=len(needs)<=64:raise ValueError('NEED_COUNT')
    seen=set()
    for row in needs:
        key=row['needKey']
        if key in seen:raise ValueError('DUPLICATE_NEED:'+key)
        seen.add(key)
        relative=Path(row['sourceContract'])
        if relative.is_absolute() or '..' in relative.parts or not (plan/relative).is_file():raise ValueError('SOURCE_CONTRACT_MISSING:'+str(relative))
        if not 1<=len(row['requiredOwners'])<=8:raise ValueError('OWNER_COUNT:'+key)
        for owner in row['requiredOwners']:
            if not isinstance(owner['parameters'],list):raise ValueError('EXACT_PARAMETER_SIGNATURE_REQUIRED:'+key)
    raw=json.dumps(needs,ensure_ascii=False,sort_keys=True,separators=(',',':'))
    digest=hashlib.sha256(raw.encode()).hexdigest()
    java=('package com.ethk3.novaonepro.strategyv2;\nimport java.util.*;\n'
          '/** Generated from the reviewed finite CAPABILITY_NEEDS_R335.json, not a universal capability inventory. */\n'
          'public final class CapabilityNeedRegistryR335 {\n private CapabilityNeedRegistryR335(){}\n'
          ' private static final String JSON='+json.dumps(raw,ensure_ascii=False)+';\n'
          ' public static final String SHA256="'+digest+'";\n'
          ' public static List<?> needs(){if(!SHA256.equals(StatsR27.sha256(JSON)))throw new IllegalStateException("R335_NEED_REGISTRY_IDENTITY");return Collections.unmodifiableList((List<?>)JsonR27.parse(JSON));}\n}\n')
    return java

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--plan',type=Path,required=True)
    p.add_argument('--catalog',type=Path,default=Path(__file__).with_name('CAPABILITY_NEEDS_R335.json'))
    p.add_argument('--output',type=Path,default=Path(__file__).parent/'production/CapabilityNeedRegistryR335.java')
    p.add_argument('--check',action='store_true');a=p.parse_args();text=generate(a.catalog,a.plan.resolve())
    if a.check:
        if not a.output.is_file() or a.output.read_text(encoding='utf-8')!=text:raise ValueError('GENERATED_REGISTRY_DRIFT')
    else:a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(text,encoding='utf-8')
    print(json.dumps({'status':'PASS','needCount':len(json.loads(a.catalog.read_text())),'checkOnly':a.check}))

if __name__=='__main__':
    try:main()
    except (ValueError,KeyError,TypeError,OSError) as e:print('REGISTRY_REFUSED:'+str(e),file=sys.stderr);sys.exit(2)
