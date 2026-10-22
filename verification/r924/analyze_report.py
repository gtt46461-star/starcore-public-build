#!/usr/bin/env python3
"""Reads every supplied report line and extracts all raw asset rows without executing them."""
from pathlib import Path
import argparse,hashlib,json,re,collections
p=argparse.ArgumentParser();p.add_argument('report',type=Path);p.add_argument('out',type=Path);a=p.parse_args()
raw=a.report.read_bytes();s=raw.decode('utf-8-sig');lines=s.splitlines();a.out.mkdir(parents=True,exist_ok=True)
tables=[];i=0
while i<len(lines):
 m=re.fullmatch(r'\[TABLE ([\w]+)\]',lines[i])
 if not m:i+=1;continue
 name=m[1];start=i+1;cols=lines[start].split('\t');rows=[];i=start+1
 while not lines[i].startswith('[END_TABLE'):
  row=lines[i].split('\t');assert len(row)==len(cols),(i,len(row),len(cols));rows.append(dict(zip(cols,row)));i+=1
 end=re.fullmatch(r'\[END_TABLE rows=(\d+) status=(\w+)\]',lines[i]);assert end and int(end[1])==len(rows) and end[2]=='OK'
 (a.out/(name+'.tsv')).write_text('\n'.join(lines[start:i])+'\n')
 tables.append({'table':name,'rows':len(rows),'columns':len(cols),'sha256':hashlib.sha256(('\n'.join(lines[start:i])+'\n').encode()).hexdigest()})
 if name=='v57_algorithm_genome_r908':
  active=[x for x in rows if x['status'] in ['IDEA_WAIT_FUTURE_OOS','OBSERVING_FUTURE_OOS','CHALLENGER_FUTURE_OOS']]
  (a.out/'active_algorithm_evidence.json').write_text(json.dumps([{k:x.get(k) for k in ['genome_id','display_name_zh','status','future_n','future_hit_rate','future_wilson_lower','future_brier','future_ece','future_max_miss','future_coverage','lifecycle_reason']} for x in active],ensure_ascii=False,indent=2))
 i+=1
headings=[{'line':i+1,'heading':x} for i,x in enumerate(lines) if x.startswith('【') or x.startswith('[TABLE')]
errors=collections.Counter(re.findall(r'\b(?:[A-Z][A-Z0-9_]*(?:FAIL(?:ED)?|ERROR|MISSING|STALE|YIELD|NOT_PROVEN|NOT_AVAILABLE)[A-Z0-9_]*|NO_ANALYSIS_REQUEST)\b',s))
summary={'bytes':len(raw),'characters_python':len(s),'lines':len(lines),'sha256':hashlib.sha256(raw).hexdigest(),'tables':tables,'all_tables_count_checked':True,'headings':headings,'diagnostic_token_counts':errors.most_common(),'runtime_database_in_input':False,'scope':'All report bytes scanned. Asset TSV values remain escaped exactly as supplied. Different report sections have different observation times.'}
(a.out/'report_scan.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
print(json.dumps({k:v for k,v in summary.items() if k in ['bytes','lines','sha256','tables','all_tables_count_checked']},ensure_ascii=False,indent=2))
