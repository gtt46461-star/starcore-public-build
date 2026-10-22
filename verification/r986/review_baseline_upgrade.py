#!/usr/bin/env python3
"""Adjudicate exact observed R980 migration deltas; retain the all-row-byte guard failure."""
from pathlib import Path
import argparse,json,hashlib
p=argparse.ArgumentParser();p.add_argument('directory',type=Path);a=p.parse_args();d=a.directory
before=json.loads((d/'BEFORE_ORIGINAL_R980_FACTS.json').read_text());after=json.loads((d/'AFTER_ORIGINAL_COLUMN_FACTS.json').read_text());receipt=json.loads((d/'receipt.json').read_text());checks=[]
def require(value,label):
 checks.append({'assertion':label,'status':'PASS' if value else 'FAIL'});assert value,label
def rows(data):return [dict(zip(data['columns'],json.loads(raw))) for raw in data['rows']]
metadata={'app_state','v57_schema_domain_r926','v57_schema_migration_journal_r926','research_legacy_fixed_registry_v41','v57_batch34_schema_meta_r930'}
for table,old in before.items():
 require(table in after,'original table retained: '+table)
 if table not in metadata:require(old['sha256']==after[table]['sha256'],'exact original columns and rows retained: '+table)
old={x['state_key']:x for x in rows(before['app_state'])};new={x['state_key']:x for x in rows(after['app_state'])}
refreshed={'r811_schema_revision','r906_schema_revision','r906_schema_research_error','r906_schema_critical_ready','r906_schema_research_ready','r915_genesis_schema_ready','r915_genesis_schema_fingerprint','r915_legacy_registry_fingerprint'}
for key,row in old.items():
 require(key in new and row['state_value']==new[key]['state_value'],'original state value retained: '+key)
 require(row==new[key] if key not in refreshed else new[key]['updated_at']>=row['updated_at'],'only named schema checkpoint timestamps may advance: '+key)
require(set(new)-set(old)=={'cp12_matrix_storage_blocker','cp12_model_storage_blocker','cp12_training_working_storage_blocker','cp12_training_history_generation'},'only required CP12 initial state keys added')
for table,ignored,key in [('research_legacy_fixed_registry_v41',{'registered_ms'},'asset_key'),('v57_batch34_schema_meta_r930',{'updated_at'},'singleton')]:
 oldrows=rows(before[table]);newrows=rows(after[table]);key=key if key in before[table]['columns'] else before[table]['columns'][0];bykey={x[key]:x for x in newrows}
 require(len(oldrows)==len(newrows),'derived metadata roster cardinality preserved: '+table)
 for row in oldrows:
  require(row[key] in bykey,'metadata identity preserved: '+table+':'+str(row[key]));actual=bykey[row[key]]
  require(all(v==actual[k] for k,v in row.items() if k not in ignored),'all metadata semantics/digests/locks retained: '+table+':'+str(row[key]))
  require(all(actual[k]>=row[k] for k in ignored),'only explicit reseeding/checkpoint times advance: '+table+':'+str(row[key]))
require(set(before['v57_schema_migration_journal_r926']['rows'])<=set(after['v57_schema_migration_journal_r926']['rows']),'all original composite-key migration journal rows preserved')
added=set(after['v57_schema_migration_journal_r926']['rows'])-set(before['v57_schema_migration_journal_r926']['rows']);require(len(added)==12,'exact current twelve migration steps added')
for raw in added:
 row=dict(zip(before['v57_schema_migration_journal_r926']['columns'],json.loads(raw)));require(row['domain']=='RESEARCH' and row['revision']==15 and row['status']=='COMPLETED','new journal is actual completed research revision15: '+row['step_id'])
old=rows(before['v57_schema_domain_r926']);new=rows(after['v57_schema_domain_r926']);require(len(old)==len(new)==1,'domain roster preserved');require(old[0]['domain']==new[0]['domain']=='RESEARCH' and old[0]['revision']==14 and new[0]['revision']==15,'actual research domain14 to15');require(new[0]['state']=='READY' and new[0]['last_error']=='','actual new domain ready without error')
require(receipt['integrity']==[['ok']] and not receipt['foreign_keys'] and receipt['sentinel_preserved'],'integrity/FK and original owner sentinel intact')
result={'status':'PASS_SCOPED_ORIGINAL_BUSINESS_FACTS_AND_SPECIFIED_METADATA_DELTAS','assertions':len(checks),'baseline_commit':receipt['baseline_commit'],'full_row_byte_guard':'FAIL_RETAINED_EXPECTED_SCHEMA_METADATA_DIFF_REQUIRES_REVIEW','original_table_count':len(before),'exact_unchanged_original_tables':len(before)-len(metadata),'scope':'Real R980 source-built schema and original product owner facts; current migration owners twice. Synthetic DB, not customer DB or missing original CP11 training fixture.','metadata_sources':['DbHelper.ensureR811CriticalSchema/ensureR906CriticalSchema checkpoint timestamps','AssetGenesisEngineR915.seedImmutableRegistries explicitly regenerates manifest metadata, all133 locks/digests preserved','SchemaDomainCoordinatorR926/ResearchSchemaCoordinatorR926 actual revision14→15 twelve-step journal','Batch34SchemaR930 schema checkpoint timestamp'],'checks':checks};(d/'SCOPED_BUSINESS_PRESERVATION_RECEIPT.json').write_text(json.dumps(result,indent=2));print(result['status'],'assertions',len(checks))
