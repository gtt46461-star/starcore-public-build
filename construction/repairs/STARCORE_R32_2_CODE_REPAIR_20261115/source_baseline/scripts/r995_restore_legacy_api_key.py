#!/usr/bin/env python3
"""Recover authorized release transport defaults without printing credential values.

Use the user's earlier APK or this delivery's APK as --apk. The output is a private
build environment JSON outside the source repository; never commit or publish it.
"""
import argparse, hashlib, json, os, re, struct, zipfile
from pathlib import Path

def release_key(apk):
    def dex_keys(data):
        def u32(pos): return struct.unpack_from('<I', data, pos)[0]
        def uleb(pos):
            value=0;shift=0
            while True:
                b=data[pos];pos+=1;value|=(b&127)<<shift;shift+=7
                if not b&128:return value,pos
        count,off=u32(0x38),u32(0x3c);strings=[]
        for i in range(count):
            pos=u32(off+4*i);_,pos=uleb(pos);end=data.index(0,pos)
            strings.append(data[pos:end].decode('utf-8',errors='replace'))
        types=[strings[u32(u32(0x44)+4*i)] for i in range(u32(0x40))]
        def value(pos):
            first=data[pos];pos+=1;kind=first&31;n=(first>>5)+1
            if kind in (0x1e,0x1f):return None,pos
            raw=int.from_bytes(data[pos:pos+n],'little');pos+=n
            return (strings[raw] if kind==0x17 else raw),pos
        for i in range(u32(0x60)):
            c=u32(0x64)+32*i
            if types[u32(c)]!='Lcom/ethk3/novaonepro/BuildConfig;':continue
            pos=u32(c+24);counts=[]
            for _ in range(4):n,pos=uleb(pos);counts.append(n)
            fields=[];idx=0
            for _ in range(counts[0]):
                diff,pos=uleb(pos);_,pos=uleb(pos);idx+=diff
                fields.append(strings[u32(u32(0x54)+8*idx+4)])
            pos=u32(c+28)
            if not pos:continue
            n,pos=uleb(pos);values=[]
            for _ in range(n):v,pos=value(pos);values.append(v)
            pairs=dict(zip(fields,values))
            if isinstance(pairs.get('DUOQU_API_KEY_A'),str) and isinstance(pairs.get('DUOQU_API_KEY_B'),str):
                key=pairs['DUOQU_API_KEY_A']+pairs['DUOQU_API_KEY_B']
                if re.fullmatch(r'[A-Za-z0-9]{8,128}',key):yield key
    keys=set()
    with zipfile.ZipFile(apk) as archive:
        for name in archive.namelist():
            if re.fullmatch(r'classes(?:\d+)?\.dex',name):keys.update(dex_keys(archive.read(name)))
    if len(keys)!=1:raise SystemExit('LEGACY_RELEASE_DEFAULT_NOT_UNIQUE_OR_MISSING')
    return keys.pop()

def main():
    p=argparse.ArgumentParser();p.add_argument('--apk',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
    root=Path(__file__).resolve().parents[1]
    if a.out.resolve().is_relative_to(root):raise SystemExit('PRIVATE_BUILD_INPUT_MUST_BE_OUTSIDE_REPOSITORY')
    key=release_key(a.apk);a.out.parent.mkdir(parents=True,exist_ok=True)
    fd=os.open(a.out,os.O_CREAT|os.O_TRUNC|os.O_WRONLY,0o600)
    with os.fdopen(fd,'w') as f:json.dump({'DUOQU_API_KEY':key},f)
    print(json.dumps({'status':'RECOVERED','apkSha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'credentialCharacters':len(key),'credentialValuePrinted':False}))
if __name__=='__main__':main()
