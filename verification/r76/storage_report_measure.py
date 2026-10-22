#!/usr/bin/env python3
"""Metadata-only report comparison. Does not infer byte ownership from row counts."""
from pathlib import Path
import json,re,argparse,hashlib
p=argparse.ArgumentParser();p.add_argument('--before',type=Path,required=True);p.add_argument('--after',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
def profile(path):
 inv=[json.loads(s) for s in (path/'FILE_STATE.jsonl').read_text().splitlines()];files=[x for x in inv if x.get('state')=='FILE_PRESENT']
 scope=json.loads((path/'DIAGNOSTIC_SCOPE.json').read_text());rows={x['table']:x['rows'] for x in scope['tables']}
 return {'observed_wall_ms':inv[0].get('startedWallMs'),'total_bytes':sum(x['bytes'] for x in files),'file_count':len(files),'files':sorted([{'path':x['path'],'bytes':x['bytes']} for x in files],key=lambda x:-x['bytes'])[:10],'report_cache_bytes':sum(x['bytes'] for x in files if x['path'].startswith('files/r981_reports/')),'top_rows':[{'table':k,'rows':v} for k,v in sorted(rows.items(),key=lambda x:-x[1])[:20]],'rows':rows,'metadata_sha256':hashlib.sha256((path/'FILE_STATE.jsonl').read_bytes()).hexdigest()}
before=profile(a.before);after=profile(a.after)
name='report-text-9d53f39df519e380c66b955a6a9e5f3db6dfc83f4fed4dbd6cca37246eae6273.txt';text=(a.after/name).read_text();tx=[]
for record in re.findall(r'DBTX75\|operation=[^;\n]+',text):tx.append(dict(re.findall(r'([^|=]+)=([^|]+)',record)))
agg={}
for key in ('begin_ns','body_ns','end_ns'):
 values=[int(x[key]) for x in tx if int(x[key])>=0];agg[key]={'count':len(values),'max_ms':max(values,default=0)/1e6,'sum_ms':sum(values)/1e6}
oldrows=before.pop('rows');newrows=after.pop('rows')
result={'scope':'FILE_INVENTORY_IS_LIVE_METADATA_NOT_ATOMIC_DB_SNAPSHOT;NO_RAW_DATABASE_OR_DBSTAT_BYTES_AVAILABLE','before':before,'after':after,'row_delta_top':[{'table':k,'before':oldrows.get(k),'after':newrows[k],'delta':newrows[k]-oldrows.get(k,0)} for k in sorted(newrows,key=lambda k:newrows[k]-oldrows.get(k,0),reverse=True)[:15]],'dbtx75':{'source':name,'records':len(tx),'stages':agg,'largest_body':max(tx,key=lambda x:int(x['body_ns'])),'largest_end':max(tx,key=lambda x:int(x['end_ns'])),'limit':'Only exported DBTX rows; bounded new telemetry may be omitted. Cannot infer that unmeasured time is BEGIN/COMMIT.'},'retention_timeout_age':{'state_updated_at':1791877720509,'r75_epoch':1791877997204,'predates_r75_ms':1791877997204-1791877720509,'meaning':'Inherited pre-R75 timeout, not evidence of a new R75 timeout.'},'backup_policy':'Old backup bytes retained. R76 version-only bump does not trigger migration when Critical revision/contract/DDL fingerprint is unchanged.'}
a.out.parent.mkdir(parents=True,exist_ok=True);a.out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'before':before['total_bytes'],'after':after['total_bytes'],'dbtx':agg},indent=2))
