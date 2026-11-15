"""Validate plan structure, original source and the five patch preimages. Never modifies a product checkout."""
import argparse, collections, copy, csv, gzip, hashlib, json, pathlib, re, subprocess, tempfile
P=pathlib.Path(__file__).resolve().parents[1]
def load(p):return json.loads((P/p).read_text())
def csvrows(p):
 with (P/p).open(encoding='utf-8-sig') as f:return list(csv.DictReader(f))
def sha(b):return hashlib.sha256(b).hexdigest()
def check(b,msg):
 if not b:raise ValueError(msg)
def unique(rows,key):
 vals=[r[key] for r in rows];check(len(vals)==len(set(vals)),f'duplicate {key}')
def graph(batches,files,cards,items):
 unique(batches,'id');unique(files,'path');unique(cards,'id');unique(items,'id')
 order=[r['id'] for r in batches];owners={r['path']:r['owner'] for r in files};before={}
 for i,r in enumerate(batches):
  n=r['id'];d=r['prerequisites'];check(all(x in order[:i] for x in d),f'cycle/future dependency {n}');before[n]=set(d)
  for x in d:before[n].update(before[x])
 for r in cards:
  n=r['batch'];check(set(r['writes'])|set(r['reads'])==set(r['files']),r['id']+' file coverage')
  check(not set(r['writes']).intersection(r['reads']),r['id']+' duplicate access')
  check(all(owners[f]==n for f in r['writes']),r['id']+' duplicate write owner')
  check(all(owners[f] in before[n] for f in r['reads']),r['id']+' needs later producer')
  anchors=r['sourceAnchors']
  check(bool(anchors) and all(owners.get(a['path'])==a['owner'] and a['owner'] in before[n]|{n} for a in anchors),r['id']+' bad source anchor or later producer')
  check(set(r['traceOnly'])=={a['path'] for a in anchors}-set(r['files']),r['id']+' trace-only inventory')
 for r in items:
  check(set(r['requires'])<=before[r['run_at']],r['id']+' forward signoff')
 return order
def main(repo,query_dump):
 batches=load('contracts/batches.json');files=load('ledger/file_owners.json');cards=load('contracts/repair_cards.json');items=load('ledger/acceptance_222.json')
 order=graph(batches,files,cards,items);check(order==['P1','P2','P3','P4','P5'],'batch list')
 def rejected(change):
  data=copy.deepcopy((batches,files,cards,items));change(*data)
  try:graph(*data)
  except ValueError:return
  raise ValueError('bad plan accepted')
 rejected(lambda b,f,c,i:b[0]['prerequisites'].append('P5'))
 rejected(lambda b,f,c,i:f.append(dict(f[0],owner='P5')))
 rejected(lambda b,f,c,i:i[0].update(run_at='P1',requires=['P4']))
 rejected(lambda b,f,c,i:c[0]['reads'].append(next(x['path'] for x in f if x['owner']=='P4')))
 rejected(lambda b,f,c,i:c[0]['sourceAnchors'][0].update(owner='P5'))
 with gzip.open(P/'ledger/current_methods.tsv.gz','rt',encoding='utf-8') as stream:
  methods={(m['path'],m['jvm_owner'],m['method'],m['descriptor'],int(m['start_line'])) for m in csv.DictReader(stream,delimiter='\t')}
 for c in cards:
  for a in c['sourceAnchors']:
   if a['kind']=='java':
    check((a['path'],a['jvmOwner'],a['method'],a['descriptor'],a['line']) in methods,'mismatched compiled production anchor '+c['id'])
   else:
    check(a['kind']=='python','unknown anchor language')
 prior=load('prior/acceptance_items.json');lookup={x['item']:x for x in prior}
 check(len(items)==222 and len({i['original'] for i in items if lookup[i['id']]['kind']=='ORIGINAL_CARD'})==194 and len({i['original'] for i in items if lookup[i['id']]['kind']=='REPAIR_CARD'})==6 and set(lookup)=={r['id'] for r in items},'original acceptance lost')
 for i in items:
  o=lookup[i['id']];check((i['text'],i['original_text_sha256'],i['level'])==(o['text'],o['original_text_sha256'],o['level']),'original text or evidence tier changed')
 check(collections.Counter(i['level'] for i in items)=={'LOCAL':189,'EMU':8,'DEVICE':24,'NATURAL':1},'evidence tiers')
 ids={o['id'] for o in load('prior/cards.json')+load('prior/repair_cards.json')+load('prior/REPAIR_CARDS_20.json')};mapped=load('ledger/cards_58_plus_20.json');check(len(mapped)==78 and {r['id'] for r in mapped}==ids,'card preservation')
 groups={r['id']:set(r['paths_by_owner']) for r in mapped}
 for r in items:check(groups[r['card']]<=set(r['requires']+[r['run_at']]),'missing actual file owner for original item')
 legacy=csvrows('prior/legacy_requirements_1832.csv');mapped_legacy=csvrows('ledger/requirements_1832.csv');unique(mapped_legacy,'id')
 check(len(legacy)==len(mapped_legacy)==1832 and {x['requirement_id']:sha(x['exact_requirement'].encode()) for x in legacy}=={x['id']:x['exact_hash'] for x in mapped_legacy},'legacy text missing')
 check(len(csvrows('prior/legacy_file_cards_299.csv'))==299 and len(load('ledger/user_40.json'))==40,'legacy counts')
 objects=load('contracts/objects_165.json');check(len(set((x['category'],x['id']) for x in objects))==len(objects)==165,'object identity')
 check(collections.Counter(x['category'] for x in objects)=={'capabilities':82,'matrices':20,'researchCharts':18,'legacyCharts':29,'logicalTables':16},'object groups')
 check(len(load('ledger/checks_96.json'))==96 and len(load('ledger/metrics_44.json'))==44,'metric count')
 check(all(x.get('action') and x.get('negative') and x.get('source') for x in objects),'missing per-object contract')
 experiments=[o['action'] for o in objects if o['category']=='capabilities' and re.fullmatch('[FIE][0-9]{2}',o['id'])]
 check(len(experiments)==len(set(experiments))==61,'missing per-family actions')
 tables={o['id']:o['contract'] for o in objects if o['category']=='logicalTables'}
 check({k for k,v in tables.items() if v['physicalSql']!=v['historicalPlanSql']}=={'T02','T09'},'032 table SQL revision lost')
 if query_dump:
  actual=json.loads(pathlib.Path(query_dump).read_text())
  check(len(actual)==16 and set(tables)=={a['id'] for a in actual},'query IDs')
  for a in actual:
   check(tables[a['id']]['physicalSql']==a['sourceSql'] and tables[a['id']]['allowedFilters']==a['filters'],'production SQL/filter mismatch '+a['id'])
 patch_paths={}
 for path in sorted((P/'patches').glob('*.patch')):
  names=re.findall(r'^diff --git a/.+? b/(.+)$',path.read_text(),re.M)
  for name in names:check(name not in patch_paths,'patch owner conflict');patch_paths[name]=path.stem
 check(patch_paths=={r['path']:r['patch'] for r in files if r['patch']},'patch/file map')
 inventory=csvrows('ledger/all_production_java.csv');unique(inventory,'path');check(len(inventory)==1308,'source inventory')
 if repo:
  repo=pathlib.Path(repo).resolve()
  def git(*args):return subprocess.check_output(['git','-C',str(repo),*args])
  source='51667a1236742b9bee8e1a8903c26358727d7c81'
  tree={}
  for rec in git('ls-tree','-r','-z',source).split(b'\0'):
   if rec:
    header,name=rec.split(b'\t',1)
    tree[name.decode()]=header.split()[2].decode()
  snap={n:h for n,h in tree.items() if n.startswith('app/src/main/java/') and n.endswith('.java')}
  for c in cards:
   for a in c['sourceAnchors']:
    if a['kind']=='python':
     lines=git('cat-file','blob',tree[a['path']]).decode().splitlines()
     check(lines[a['line']-1].startswith('def '+a['method']+'('),'wrong Python entry '+a['path'])
  check(snap=={r['path']:r['git_blob'] for r in inventory},'wrong baseline')
  # Check ordered apply and reverse in a temporary directory using actual immutable Git blob content.
  with tempfile.TemporaryDirectory() as t:
   tmp=pathlib.Path(t)
   for n in patch_paths:
    if n in tree:
     p=tmp/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(git('cat-file','blob',tree[n]))
   def apply(path,*args):subprocess.run(['git','apply',*args,str(path.resolve())],cwd=tmp,check=True)
   patches=sorted((P/'patches').glob('P?.patch'))
   for path in patches:apply(path,'--check');apply(path)
   for path in reversed(patches):apply(path,'--reverse','--check');apply(path,'--reverse')
   for n in patch_paths:check((tmp/n).read_bytes()==git('cat-file','blob',tree[n]) if n in tree else not (tmp/n).exists(),'round-trip mismatch')
 for p in [P/'README.md',*(P/'guide').glob('*.md')]:
  for target in re.findall(r'\[[^\]\n]+\]\(([^)\n]+)\)',p.read_text()):
   if target.startswith(('http','#','<')):continue
   check((p.parent/target).is_file(),p.name+' bad link '+target)
 if (P/'MANIFEST.json').is_file():
  hashes=load('MANIFEST.json')['sha256_by_file']
  actual_files={f.relative_to(P).as_posix() for f in P.rglob('*') if f.is_file() and f.name!='MANIFEST.json' and '__pycache__' not in f.parts}
  check(actual_files==set(hashes),'manifest coverage')
  for n,h in hashes.items():check(sha((P/n).read_bytes())==h,'file hash '+n)
 print(json.dumps({'structural_checks':'PASS','negative_plan_mutations_rejected':5,'production_anchor_bindings':sum(len(c['sourceAnchors']) for c in cards),'unique_owned_paths':len(files),'registered_sql_compared':16 if query_dump else 0,'batches':len(batches),'repair_cards':len(cards),'original_and_unified_cards':len(mapped),'items':len(items),'source_files':len(inventory),'patch_paths':len(patch_paths),'patch_round_trip':bool(repo)},ensure_ascii=False))
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--repo');parser.add_argument('--query-dump',help='JSON emitted by FieldContractProbe directly from production query definitions');args=parser.parse_args();main(args.repo,args.query_dump)
