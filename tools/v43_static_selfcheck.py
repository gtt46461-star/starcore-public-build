#!/usr/bin/env python3
from pathlib import Path
import javalang, json, re
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java'
errors=[]
for p in JAVA.rglob('*.java'):
    try: javalang.parse.parse(p.read_text(encoding='utf-8'))
    except Exception as e: errors.append({'file':str(p.relative_to(ROOT)),'error':repr(e)})
print(json.dumps({'java_files':len(list(JAVA.rglob('*.java'))),'parse_errors':errors},ensure_ascii=False,indent=2))
raise SystemExit(1 if errors else 0)
