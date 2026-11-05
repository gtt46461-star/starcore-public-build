import sys,re,os,glob
from xml.etree import ElementTree as ET
res=sys.argv[1];out=sys.argv[2]
types={}
def add(t,n):types.setdefault(t,set()).add(n.replace('.','_'))
for d in glob.glob(res+'/*'):
    base=os.path.basename(d).split('-')[0]
    for f in glob.glob(d+'/*'):
        name=os.path.splitext(os.path.basename(f))[0]
        if base!='values':add(base,name)
        if f.endswith('.xml'):
            txt=open(f,encoding='utf-8').read()
            for m in re.findall(r'@\+id/([A-Za-z0-9_.]+)',txt):add('id',m)
            if base=='values':
                for el in ET.parse(f).getroot():
                    tag=el.tag;n=el.get('name')
                    if not n:continue
                    t={'string-array':'array','integer-array':'array','array':'array','item':el.get('type'),'declare-styleable':'styleable'}.get(tag,tag)
                    if t and t!='styleable':add(t,n)
src=['package com.ethk3.novaonepro;','public final class R {'];base=0x7f010000
for t in sorted(types):
    src.append(' public static final class %s {'%t)
    for i,n in enumerate(sorted(types[t])):src.append('  public static final int %s=0x%08x;'%(n,base+i))
    src.append(' }');base+=0x10000
src.append('}')
open(out,'w').write('\n'.join(src)+'\n')
print({t:len(v) for t,v in types.items()})
