#!/usr/bin/env python3
"""静态扫描：每张表在哪里被创建、在哪些 文件:行 被写（INSERT/UPDATE/DELETE/REPLACE 与 SQLiteDatabase.insert/update/delete API）。
用法：python3 tools/r30/scan_table_writers.py <仓库根> <输出.json> [--prefix r27_] [--file AutoFetchService.java]
说明：字符串拼接出的表名扫不到（运行时以 sqlite_master 为准）；本工具用于定位写点，不用于证明“没有写点”。"""
import re,os,json,sys,collections
root=sys.argv[1]
create=re.compile(r'CREATE\s+(?:TEMP\s+)?TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?["`]?([A-Za-z_][A-Za-z0-9_]*)',re.I)
write=re.compile(r'\b(INSERT(?:\s+OR\s+(?:IGNORE|REPLACE|ABORT|FAIL|ROLLBACK))?\s+INTO|REPLACE\s+INTO|UPDATE(?:\s+OR\s+\w+)?|DELETE\s+FROM)\s+["`]?([A-Za-z_][A-Za-z0-9_]*)',re.I)
api=re.compile(r'\.(insert|insertOrThrow|insertWithOnConflict|replace|replaceOrThrow|update|updateWithOnConflict|delete)\(\s*"([A-Za-z_][A-Za-z0-9_]*)"')
tables=collections.defaultdict(list);writes=collections.defaultdict(list)
for d,_,fs in os.walk(os.path.join(root,'app/src/main')):
  for f in fs:
    if not f.endswith('.java'):continue
    p=os.path.join(d,f);rel=os.path.relpath(p,root)
    for i,line in enumerate(open(p,encoding='utf-8',errors='replace'),1):
      for m in create.finditer(line):tables[m.group(1).lower()].append(f"{rel}:{i}")
      for m in write.finditer(line):
        t=m.group(2).lower()
        if t in('or','set','into','from','where','select'):continue
        writes[t].append(f"{rel}:{i}:{m.group(1).split()[0].upper()}")
      for m in api.finditer(line):writes[m.group(2).lower()].append(f"{rel}:{i}:API_{m.group(1)}")
def domain(t):
  RT_EXPL={"r26_feedback","r26_frozen","r26_incumbent_frozen","su1_source_clock","su1_mainline_binding","su2_live_decision","su2_mainline_choice","su2_mainline_binding","r974_exact_real_receipt","research_exact_probability_f05"}
  RT29={"r29_event_outbox","r29_source_watermark","r29_registration_authority","r29_database_route","r29_fetch_observation","r29_scheduler_owner","r29_legacy_checkpoint_pin","r29_migration_publication","r29_migration_owner_fence","r29_projection_applied","r29_research_heartbeat","r29_legacy_live_projection","r29_critical_input_clock","r29_critical_input_change","r29_critical_input_schema","r29_legacy_authority_frame","r29_legacy_authority_projection","r27_fetch_reject","r27_chart_cache"}
  if t in RT_EXPL or t in RT29:return "REALTIME"
  if re.match(r'r(27|29)_',t) or re.match(r'(r26_|su1_|su2_|research_|v57_research_|v57_batch34_)',t) or re.match(r'r9(5[28]|6[789]|7[0-9])_',t):return "RESEARCH"
  return "REALTIME_OR_LEGACY"
out=[]
for t in sorted(set(tables)|set(writes)):
  if t not in tables:continue
  out.append({"table":t,"domain_by_prefix":domain(t),"created_at":tables[t][:5],"write_sites":writes.get(t,[])})
pre=sys.argv[sys.argv.index('--prefix')+1] if '--prefix' in sys.argv else None
onlyf=sys.argv[sys.argv.index('--file')+1] if '--file' in sys.argv else None
if pre: out=[o for o in out if o['table'].startswith(pre)]
if onlyf:
  for o in out: o['write_sites']=[w for w in o['write_sites'] if w.split(':')[0].endswith(onlyf)]
  out=[o for o in out if o['write_sites']]
json.dump(out,open(sys.argv[2],'w'),ensure_ascii=False,indent=1)
c=collections.Counter(o['domain_by_prefix'] for o in out)
print(len(out),dict(c),sum(len(o['write_sites']) for o in out))
