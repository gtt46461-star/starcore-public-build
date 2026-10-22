#!/usr/bin/env python3
from pathlib import Path
import re,sys
R=Path.cwd(); S=R/'app/src/main/java/com/ethk3/novaonepro'

def replace_once(p,old,new,tag):
    s=p.read_text(encoding='utf-8')
    n=s.count(old)
    if n!=1:
        raise SystemExit('%s count=%d'%(tag,n))
    p.write_text(s.replace(old,new,1),encoding='utf-8')

svc=S/'AutoFetchService.java'
s=svc.read_text(encoding='utf-8')
# Remove only the R2 methods inserted ahead of onBind; retain the mature B07 handlers near file end.
start=s.find('\t@Override\n    public void onLowMemory() {')
end=s.find('\t@Override\n\tpublic IBinder onBind(Intent intent) {',start)
if start<0 or end<0:
    raise SystemExit('R2_INSERTED_MEMORY_BLOCK_NOT_FOUND')
s=s[:start]+s[end:]
# Strengthen existing mature handlers instead of creating duplicate overrides.
old='''    @Override public void onLowMemory() {\n        try {\n            RollingWindowCache.shared().clear();\n            FullInformationFeatureGraphEngine.shared().clear();\n'''
new='''    @Override public void onLowMemory() {\n        try {\n            if (store != null) {\n                store.setState("b07r2_low_memory_at", String.valueOf(System.currentTimeMillis()));\n                store.setState("b07r2_low_memory_action", "CLEAR_HEAVY_CACHE_KEEP_SERVICE_ALIVE");\n            }\n            if (AppConfig.B07R2_LOW_MEMORY_SELF_HEAL) {\n                RollingWindowCache.shared().clear();\n                FullInformationFeatureGraphEngine.shared().clear();\n            }\n'''
if s.count(old)!=1: raise SystemExit('EXISTING_LOW_MEMORY_HANDLER_COUNT='+str(s.count(old)))
s=s.replace(old,new,1)
old='''    @Override public void onTrimMemory(int level) {\n        try {\n            RollingWindowCache.shared().clear();\n            FullInformationFeatureGraphEngine.shared().clear();\n'''
new='''    @Override public void onTrimMemory(int level) {\n        try {\n            if (store != null) {\n                store.setState("b07r2_trim_memory_at", String.valueOf(System.currentTimeMillis()));\n                store.setState("b07r2_trim_memory_level", String.valueOf(level));\n            }\n            if (AppConfig.B07R2_LOW_MEMORY_SELF_HEAL) {\n                RollingWindowCache.shared().clear();\n                FullInformationFeatureGraphEngine.shared().clear();\n            }\n'''
if s.count(old)!=1: raise SystemExit('EXISTING_TRIM_MEMORY_HANDLER_COUNT='+str(s.count(old)))
s=s.replace(old,new,1)
svc.write_text(s,encoding='utf-8')

# DecisionGate audit must be null-safe without relying on a helper that this class does not own.
dec=S/'DecisionGateEngine.java'
s=dec.read_text(encoding='utf-8')
old='store.setState("b07r2_real_action_target", safe(bestBet.candidate == null ? "" : bestBet.candidate.targetIssue));'
new='store.setState("b07r2_real_action_target", bestBet.candidate == null || bestBet.candidate.targetIssue == null ? "" : bestBet.candidate.targetIssue);'
if s.count(old)!=1: raise SystemExit('DECISION_SAFE_COUNT='+str(s.count(old)))
dec.write_text(s.replace(old,new,1),encoding='utf-8')

# Print the exact missing B01-B07 plan entry from the generated audit manifest.
audit=(S/'B07R2BuildAuditManifest.java').read_text(encoding='utf-8')
missing=[]
for line in audit.splitlines():
    if 'new Entry(' in line and ', false,' in line:
        m=re.search(r'new Entry\((\d+), "([^"]+)"',line)
        if m: missing.append('B0%s/%s'%(m.group(1),m.group(2)))
print('R2_REPAIR_OK')
print('MISSING_PLAN_FILES='+('|'.join(missing) if missing else 'NONE'))
