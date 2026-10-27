#!/usr/bin/env python3
"""Bind the instrumented test denominator to the source compiled into this CI artifact."""
from pathlib import Path
import hashlib,json,re
ROOT=Path(__file__).resolve().parents[1]
def main():
    rows=[]
    for path in sorted((ROOT/'app/src/androidTest/java/com/ethk3/novaonepro/strategyv2').rglob('*.java'),key=lambda p:(p.stem!='ResearchUiDeviceSU1Test',p.name)):
        source=path.read_text()
        methods=re.findall(r'@(?:org\.junit\.)?Test(?:\([^)]*\))?\s+(?:public\s+)?void\s+(\w+)\s*\(',source)
        if not methods:continue
        assert len(methods)==len(re.findall(r'@(?:org\.junit\.)?Test\b',source)),f'Unparsed @Test annotation: {path}'
        assert len(methods)==len(set(methods)),f'Duplicate test method: {path}'
        package=re.search(r'^package\s+([\w.]+)\s*;',source,re.M)[1]
        rows.append(dict(className=package+'.'+path.stem,methods=methods,count=len(methods),sourcePath=str(path.relative_to(ROOT)),sourceSha256=hashlib.sha256(path.read_bytes()).hexdigest(),isolateEachMethod=path.stem=='R26PublicationDeviceTest'))
    assert rows,'Empty instrumentation manifest'
    out=ROOT/'package-receipt/instrumentation-tests.json';out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(dict(schema='R26_INSTRUMENTATION_MANIFEST_V1',classes=rows,total=sum(x['count'] for x in rows)),indent=2)+'\n')
    print(f'INSTRUMENTATION_MANIFEST|classes={len(rows)}|tests={sum(x["count"] for x in rows)}')
if __name__=='__main__':main()
